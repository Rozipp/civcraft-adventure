/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.object;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.HashMap;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.Camp;
import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.exception.AlreadyRegisteredException;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.permission.PermissionGroup;
import com.avrgaming.civcraft.permission.PlotPermissions;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;

@Getter
@Setter
public class TownChunk extends SQLObject {

	private ChunkCoord chunkLocation;
	private Town town;
	private boolean forSale;
	/* Price vs value, price is what the owner is currently selling it for, value is the amount that it was last purchased at, used for
	 * taxes. */
	private double value;
	private double price;

	public PlotPermissions perms = new PlotPermissions();

	public static final String TABLE_NAME = "TOWNCHUNKS";

	public TownChunk(ResultSet rs) throws SQLException, CivException {
		try {
			this.load(rs);
		} catch (CivException e) {
			this.delete();
			throw new CivException(e.getMessage());
		}
	}

	public TownChunk(Town newTown, Location location) {
		ChunkCoord coord = new ChunkCoord(location);
		setTown(newTown);
		setChunkCord(coord);
		perms.addGroup(newTown.getDefaultGroup());
	}

	public TownChunk(Town newTown, ChunkCoord chunkLocation) {
		setTown(newTown);
		setChunkCord(chunkLocation);
		perms.addGroup(newTown.getDefaultGroup());
	}

	public static void init() throws SQLException {
		if (!SQL.hasTable(TABLE_NAME)) {
			String table_create = "CREATE TABLE " + SQL.tb_prefix + TABLE_NAME + " (" //
					+ "`id` int(11) unsigned NOT NULL auto_increment,"//
					+ "`town_id` int(11) unsigned NOT NULL,"//
					+ "`world` VARCHAR(32) NOT NULL,"//
					+ "`x` bigint(20) NOT NULL,"//
					+ "`z` bigint(20) NOT NULL,"//
					+ "`owner_id` int(11) unsigned DEFAULT NULL,"//
					+ "`groups` mediumtext DEFAULT NULL,"//
					+ "`permissions` mediumtext NOT NULL,"//
					+ "`for_sale` bool NOT NULL DEFAULT '0',"//
					+ "`value` float NOT NULL DEFAULT '0',"//
					+ "`price` float NOT NULL DEFAULT '0',"//
					// "FOREIGN KEY (owner_id) REFERENCES "+SQL.tb_prefix+Resident.TABLE_NAME+"(id),"+
					// "FOREIGN KEY (town_id) REFERENCES "+SQL.tb_prefix+Town.TABLE_NAME+"(id),"+
					+ "PRIMARY KEY (`id`)" + ")";

			SQL.makeTable(table_create);
			CivLog.info("Created " + TABLE_NAME + " table");
		} else {
			CivLog.info(TABLE_NAME + " table OK!");
		}
	}

	@Override
	public void load(ResultSet rs) throws SQLException, CivException {
		this.setId(rs.getInt("id"));
		this.town = CivGlobal.getTownFromId(rs.getInt("town_id"));
		if (this.getTown() == null) {
			CivLog.warning("TownChunk tried to load without a town...");
			if (CivGlobal.isHaveTestFlag("cleanupDatabase")) {
				CivLog.info("CLEANING");
				this.delete();
			}
			throw new CivException("Not found town ID (" + rs.getInt("town_id") + ") to load this town chunk(" + this.getId());
		}
		ChunkCoord cord = new ChunkCoord(rs.getString("world"), rs.getInt("x"), rs.getInt("z"));
		this.setChunkCord(cord);

		try {
			this.perms.loadFromSaveString(town, rs.getString("permissions"));
		} catch (CivException e) {
			e.printStackTrace();
		}

		this.perms.setOwner(CivGlobal.getResidentFromId(rs.getInt("owner_id")));
		// this.perms.setGroup(CivGlobal.getPermissionGroup(this.getTown(), rs.getInt("groups")));
		String grpString = rs.getString("groups");
		if (grpString != null) {
			String[] groups = grpString.split(":");
			for (String grp : groups) {
				this.perms.addGroup(CivGlobal.getPermissionGroup(this.getTown(), Integer.valueOf(grp)));
			}
		}

		this.forSale = rs.getBoolean("for_sale");
		this.value = rs.getDouble("value");
		this.price = rs.getDouble("price");

		try {
			this.getTown().addTownChunk(this);
		} catch (AlreadyRegisteredException e1) {
			e1.printStackTrace();
		}

	}

	@Override
	public void saveNow() throws SQLException {
		HashMap<String, Object> hashmap = new HashMap<String, Object>();

		hashmap.put("id", this.getId());
		hashmap.put("town_id", this.getTown().getId());
		hashmap.put("world", this.getChunkCoord().getWorldname());
		hashmap.put("x", this.getChunkCoord().getX());
		hashmap.put("z", this.getChunkCoord().getZ());
		hashmap.put("permissions", perms.getSaveString());
		hashmap.put("for_sale", this.isForSale());
		hashmap.put("value", this.getValue());
		hashmap.put("price", this.getPrice());

		if (this.perms.getOwner() != null) {
			hashmap.put("owner_id", this.perms.getOwner().getId());
		} else {
			hashmap.put("owner_id", null);
		}

		if (this.perms.getGroups().size() != 0) {
			String out = "";
			for (PermissionGroup grp : this.perms.getGroups()) {
				out += grp.getId() + ":";
			}
			hashmap.put("groups", out);
		} else {
			hashmap.put("groups", null);
		}

		SQL.updateNamedObject(this, hashmap, TABLE_NAME);
	}

	public ChunkCoord getChunkCoord() {
		return chunkLocation;
	}

	public void setChunkCord(ChunkCoord chunkLocation) {
		this.chunkLocation = chunkLocation;
	}

	public static TownChunk claim(Town town, ChunkCoord coord) throws CivException {
		if (CivGlobal.getTownChunk(coord) != null) throw new CivException(CivSettings.localize.localizedString("town_chunk_errorClaimed"));

		CultureChunk cultureChunk = CivGlobal.getCultureChunk(coord);
		if (cultureChunk == null || cultureChunk.getCiv() != town.getCiv()) throw new CivException(CivSettings.localize.localizedString("town_chunk_claimOutsideCulture"));
		if (cultureChunk.getTown() != town) throw new CivException(CivSettings.localize.localizedString("town_chunk_notOwnCultureChunk"));

		TownChunk tc = new TownChunk(town, coord);

		if (!tc.isOnEdgeOfOwnership()) throw new CivException(CivSettings.localize.localizedString("town_chunk_claimTooFar"));
		if (town.getMaxPlots() <= town.getTownChunks().size()) throw new CivException(CivSettings.localize.localizedString("town_chunk_claimTooMany"));

		// Test that we are not too close to another civ
		try {
			int min_distance = CivSettings.getInteger(CivSettings.civConfig, "civ.min_distance");
			double min_distanceSqr = Math.pow(min_distance, 2);

			for (TownChunk cc : CivGlobal.getTownChunks()) {
				if (cc.getCiv() != town.getCiv()) {
					double distSqr = coord.distanceSqr(cc.getChunkCoord());
					if (distSqr <= min_distanceSqr) {
						DecimalFormat df = new DecimalFormat();
						throw new CivException(CivSettings.localize.localizedString("var_town_chunk_claimTooClose", cc.getCiv().getName(), df.format(Math.sqrt(distSqr)), min_distance));
					}
				}
			}
		} catch (InvalidConfiguration e1) {
			e1.printStackTrace();
			throw new CivException(CivSettings.localize.localizedString("internalException"));
		}

		try {
			town.addTownChunk(tc);
		} catch (AlreadyRegisteredException e1) {
			e1.printStackTrace();
			throw new CivException(CivSettings.localize.localizedString("internalCommandException"));
		}

		Camp camp = CivGlobal.getCampAt(coord);
		if (camp != null) {
			CivMessage.sendCamp(camp, CivColor.Yellow + ChatColor.BOLD + CivSettings.localize.localizedString("var_town_chunk_dibandcamp", town.getName()));
			camp.disband();
		}

		tc.save();
		CivGlobal.addTownChunk(tc);
		CivGlobal.processCulture();
		return tc;
	}

	public static TownChunk claim(Town town, Player player) throws CivException {
		TownChunk tc = claim(town, new ChunkCoord(player.getLocation()));
		CivMessage.sendSuccess(player, CivSettings.localize.localizedString("var_town_chunk_success", tc.getChunkCoord(), town.getTownChunks().size(), town.getMaxPlots()));
		return tc;
	}

	private Civilization getCiv() {
		return this.getTown().getCiv();
	}

	/* XXX This claim is only called when a town hall is building and needs to be claimed. We do not save here since its going to be saved
	 * in-order using the SQL save in order task. Also certain types of validation and cost cacluation are skipped. */
	public static TownChunk autoClaim(Town town, ChunkCoord coord) throws CivException {
		// This is only called when the town hall is built and needs to be claimed.

		if (CivGlobal.getTownChunk(coord) != null) throw new CivException(CivSettings.localize.localizedString("town_chunk_errorClaimed"));

		TownChunk tc = new TownChunk(town, coord);

		try {
			town.addTownChunk(tc);
		} catch (AlreadyRegisteredException e1) {
			e1.printStackTrace();
			throw new CivException(CivSettings.localize.localizedString("internalCommandException"));
		}

		Camp camp = CivGlobal.getCampAt(coord);
		if (camp != null) {
			CivMessage.sendCamp(camp, CivColor.Yellow + ChatColor.BOLD + CivSettings.localize.localizedString("var_town_chunk_dibandcamp", town.getName()));
			camp.disband();
		}

		CivGlobal.addTownChunk(tc);
		tc.save();
		return tc;
	}

	private boolean isOnEdgeOfOwnership() {

		int[][] offset = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
		for (int i = 0; i < 4; i++) {
			TownChunk tc = CivGlobal.getTownChunk(new ChunkCoord(this.getChunkCoord().getWorldname(), this.getChunkCoord().getX() + offset[i][0], this.getChunkCoord().getZ() + offset[i][1]));
			if (tc != null && tc.getTown() == this.getTown()) return true;
		}
		return false;
	}

	@Override
	public void delete() throws SQLException {
		SQL.deleteNamedObject(this, TABLE_NAME);
		CivGlobal.removeTownChunk(this);
	}

	/* Called when a player enters this plot. */
	public String getOnEnterString(Player player, TownChunk fromTc) {
		String out = "";

		if (this.perms.getOwner() != null) {
			out += CivColor.LightGray + "[" + CivSettings.localize.localizedString("town_chunk_status_owned") + " " + CivColor.LightGreen + this.perms.getOwner().getName() + CivColor.LightGray + "]";
		}

		if (this.perms.getOwner() == null && fromTc != null && fromTc.perms.getOwner() != null) {
			out += CivColor.LightGray + "[" + CivSettings.localize.localizedString("town_chunk_status_unowned") + "]";
		}

		if (this.isForSale()) {
			out += CivColor.Yellow + "[" + CivSettings.localize.localizedString("town_chunk_status_forSale") + " " + this.price + " " + CivSettings.CURRENCY_NAME + "]";
		}

		return out;
	}

	public void purchase(Resident resident) throws CivException {

		if (!resident.getTreasury().hasEnough(this.price)) {
			throw new CivException(CivSettings.localize.localizedString("var_town_chunk_purchase_tooPoor", this.price, CivSettings.CURRENCY_NAME));
		}

		if (this.perms.getOwner() == null) {
			resident.getTreasury().payTo(this.getTown().getTreasury(), this.price);
		} else {
			resident.getTreasury().payTo(this.perms.getOwner().getTreasury(), this.price);
		}

		this.value = this.price;
		this.price = 0;
		this.forSale = false;
		this.perms.setOwner(resident);
		this.perms.clearGroups();

		this.save();
	}

	public String getCenterString() {
		return this.chunkLocation.toString();
	}

	public static void unclaim(TownChunk tc) throws CivException {
		tc.getTown().removeTownChunk(tc);
		try {
			tc.delete();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new CivException(CivSettings.localize.localizedString("internalDatabaseException"));
		}

	}
}
