package com.avrgaming.civcraft.construct.constructs;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.components.ProjectileArrowComponent;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.Construct;
import com.avrgaming.civcraft.construct.ConstructDamageBlock;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.exception.InvalidObjectException;
import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.SimpleBlock;
import com.avrgaming.civcraft.war.War;

public class Arrowpost extends Construct {

	public static HashMap<BlockCoord, Construct> arrowposts = new HashMap<BlockCoord, Construct>();

	ProjectileArrowComponent arrowComponent;
	Set<BlockCoord> turretLocation = new HashSet<>();

	public static void cleanupAll() {
		for (Construct arrowpost : arrowposts.values()) {
			arrowpost.deleteWithUndo();
		}
	}

	public Arrowpost(Player player) throws CivException {
		super("c_arrowpost", CivGlobal.getResident(player).getCiv());
	}

	public static void newArrowpost(Player player) throws CivException {
		Resident resident = CivGlobal.getResident(player);
		if (player.isOp())
			CivMessage.send(player, "Поскольку вы оп, то можете строить пушки в мирное время");
		else {
			if (!War.isWarTime()) throw new CivException(CivSettings.localize.localizedString("buildCannon_NotWar"));
			if (!resident.getCiv().getDiplomacyManager().isAtWar()) throw new CivException("Ваша цивилизация не участвует в войне");
		}
		if (resident.getTown() == null) throw new CivException("У вас нет города");
		Arrowpost arrowpost = new Arrowpost(player);
		arrowpost.initDefaultTemplate(player.getLocation());
		arrowpost.checkBlockPermissionsAndRestrictions(player);

		CivMessage.sendCiv(resident.getCiv(), CivSettings.localize.localizedString("var_buildCannon_Success", arrowpost.getCorner().toStringNotWorld()));
		arrowpost.build(player);
	}

	private void destroy() {
		for (BlockCoord b : this.constructBlocks.keySet()) {
			if (b.getBlock().getType() == Material.AIR) continue;
			double rand = CivCraft.civRandom.nextDouble();
			if (rand < 0.2)
				ItemManager.setTypeIdAndData(b.getBlock(), CivData.GRAVEL, 0, false);
			else
				ItemManager.setTypeIdAndData(b.getBlock(), CivData.AIR, 0, false);
		}
		this.delete();
	}

	@Override
	public void commandBlockRelatives(BlockCoord absCoord, SimpleBlock sb) {
		switch (sb.command) {
		case "/towerfire":
			turretLocation.add(absCoord);
			break;
		}
	}

	@Override
	public void onPostBuild() {
		arrowComponent = new ProjectileArrowComponent(this);
		arrowComponent.createComponent(this);
		arrowComponent.setTurretLocation(turretLocation);
		arrowComponent.setDamage(arrowComponent.getDamage() / 2);
	}

	@Override
	public void build(Player player) throws CivException {
		this.checkBlockPermissionsAndRestrictions(player);
		try {
			this.getTemplate().saveUndoTemplate(this.getCorner().toString(), this.getCorner());
		} catch (IOException var8) {
			var8.printStackTrace();
		}
		this.getTemplate().buildTemplate(corner);
		this.postBuild();
		Arrowpost.arrowposts.put(this.getCorner(), this);
	}

	@Override
	public void processUndo() throws CivException {
		// TODO Автоматически созданная заглушка метода

	}

	@Override
	public String getDynmapDescription() {
		// TODO Автоматически созданная заглушка метода
		return null;
	}

	@Override
	public String getMarkerIconName() {
		// TODO Автоматически созданная заглушка метода
		return null;
	}

	@Override
	public void onLoad() throws CivException {
		// TODO Автоматически созданная заглушка метода

	}

	@Override
	public void onUnload() {
		// TODO Автоматически созданная заглушка метода

	}

	@Override
	public void onDamage(int amount, Player player, ConstructDamageBlock hit) {
		this.setHitpoints(this.getHitpoints() - amount);

		if (getHitpoints() <= 0) {
			destroy();
			return;
		}
	}

	@Override
	public void onDamageNotification(Player player, ConstructDamageBlock hit) {
		// TODO Автоматически созданная заглушка метода

	}

	@Override
	public void load(ResultSet rs) throws SQLException, InvalidNameException, InvalidObjectException, CivException {
		// TODO Автоматически созданная заглушка метода

	}

	@Override
	public void saveNow() throws SQLException {
		// TODO Автоматически созданная заглушка метода

	}

	@Override
	public Location repositionCenter(Location center, Template tpl) throws CivException {
		Location loc = center.clone();
		String dir = tpl.getDirection();
		double x_size = tpl.getSize_x();
		double z_size = tpl.getSize_z();
		if (dir.equalsIgnoreCase("east")) {
			loc.setZ(loc.getZ() - (z_size / 2));
			loc.setX(loc.getX());
		} else
			if (dir.equalsIgnoreCase("west")) {
				loc.setZ(loc.getZ() - (z_size / 2));
				loc.setX(loc.getX() - (x_size));

			} else
				if (dir.equalsIgnoreCase("north")) {
					loc.setX(loc.getX() - (x_size / 2));
					loc.setZ(loc.getZ() - (z_size));
				} else
					if (dir.equalsIgnoreCase("south")) {
						loc.setX(loc.getX() - (x_size / 2));
						loc.setZ(loc.getZ());

					}

		return loc;
	}
}
