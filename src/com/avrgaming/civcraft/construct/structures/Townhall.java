/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.construct.structures;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.components.ProjectileArrowComponent;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigCultureLevel;
import com.avrgaming.civcraft.config.ConfigTech;
import com.avrgaming.civcraft.construct.CannonProjectile;
import com.avrgaming.civcraft.construct.ConstructBlock;
import com.avrgaming.civcraft.construct.ConstructDamageBlock;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.items.BonusGoodie;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.ControlPoint;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.object.TownChunk;
import com.avrgaming.civcraft.threading.tasks.UpdateTechBar;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.FireworkEffectPlayer;
import com.avrgaming.civcraft.util.ItemFrameStorage;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.SimpleBlock;
import com.avrgaming.civcraft.war.War;
import com.avrgaming.civcraft.war.WarStats;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Townhall extends Structure implements RespawnLocationHolder {

	// TODO make this configurable.
	public static int MAX_GOODIE_FRAMES = 8;

	private BlockCoord[] techbar = new BlockCoord[10];

	private BlockCoord technameSign;
	private byte technameSignData; // Hold the sign's orientation

	private BlockCoord techdataSign;
	private byte techdataSignData; // Hold the sign's orientation

	private ArrayList<ItemFrameStorage> goodieFrames = new ArrayList<ItemFrameStorage>();
	private ArrayList<BlockCoord> respawnPoints = new ArrayList<BlockCoord>();
	private ArrayList<BlockCoord> revivePoints = new ArrayList<BlockCoord>();
	protected HashMap<BlockCoord, ControlPoint> controlPoints = new HashMap<BlockCoord, ControlPoint>();

	private HashMap<Integer, ProjectileArrowComponent> arrowTowers = new HashMap<Integer, ProjectileArrowComponent>();

	public ArrayList<BlockCoord> nextGoodieFramePoint = new ArrayList<BlockCoord>();
	public ArrayList<Integer> nextGoodieFrameDirection = new ArrayList<Integer>();

	public Townhall(String id, Town town) throws CivException {
		super(id, town);
	}

	public Townhall(ResultSet rs) throws SQLException, CivException {
		super(rs);
	}

	@Override
	public void delete() {
		if (this.getTown() != null) {
			/* Remove any protected item frames. */
			for (ItemFrameStorage framestore : goodieFrames) {
				BonusGoodie goodie = CivGlobal.getBonusGoodie(framestore.getItem());
				if (goodie != null) goodie.replenish();

				CivGlobal.removeProtectedItemFrame(framestore.getFrameID());
			}
		}
		super.delete();
	}

	// ------------ build
	@Override
	public void commandBlockRelatives(BlockCoord absCoord, SimpleBlock sb) {
		switch (sb.command) {
		case "/techbar":
			String strvalue = sb.keyvalues.get("id");
			if (strvalue != null) this.addTechBarBlock(absCoord, Integer.valueOf(strvalue));
			break;
		case "/techname":
			this.setTechnameSign(absCoord);
			this.setTechnameSignData((byte) sb.getData());
			break;
		case "/techdata":
			this.setTechdataSign(absCoord);
			this.setTechdataSignData((byte) sb.getData());
			break;
		// case "/itemframe":
		// strvalue = sb.keyvalues.get("id");
		// if (strvalue != null) {
		// this.createGoodieItemFrame(absCoord, Integer.valueOf(strvalue), sb.getData());
		// this.addConstructBlock(absCoord, false);
		// }
		// break;
		case "/respawn":
			this.addRespawnPoint(absCoord);
			break;
		case "/revive":
			this.addRevivePoint(absCoord);
			break;
		case "/control":
			this.createControlPoint(absCoord, "");
			break;
		case "/towerfire":
			String id = sb.keyvalues.get("id");
			Integer towerID = Integer.valueOf(id);

			if (!arrowTowers.containsKey(towerID)) {
				ProjectileArrowComponent arrowTower = new ProjectileArrowComponent(this);
				arrowTower.createComponent(this);
				arrowTower.setTurretLocation(absCoord);
				arrowTowers.put(towerID, arrowTower);
			}
			break;
		}
	}

	@Override
	public void onPostBuild() {
		(new UpdateTechBar(this.getCiv())).run();
	}

	// ------------- TechBar
	public void addTechBarBlock(BlockCoord coord, int index) {
		techbar[index] = coord;
	}

	public BlockCoord getTechBarBlock(int i) {
		return techbar[i];
	}

	public int getTechBarSize() {
		return techbar.length;
	}
	// ------------------ ItemFrame

	public void createGoodieItemFrame(BlockCoord absCoord, int slotId, int direction) {
		if (slotId >= MAX_GOODIE_FRAMES) return;
		/* Make sure there isn't another frame here. We have the position of the sign, but the entity's position is the block it's attached to.
		 * We'll use the direction from the sign data to determine which direction to look for the entity. */
		Block attachedBlock = absCoord.getBlock();
		BlockFace facingDirection;
		switch (direction) {
		case CivData.DATA_SIGN_EAST:
			facingDirection = BlockFace.EAST;
			break;
		case CivData.DATA_SIGN_WEST:
			facingDirection = BlockFace.WEST;
			break;
		case CivData.DATA_SIGN_NORTH:
			facingDirection = BlockFace.NORTH;
			break;
		case CivData.DATA_SIGN_SOUTH:
			facingDirection = BlockFace.SOUTH;
			break;
		default:
			CivLog.error("Bad sign data for /itemframe sign in town hall.");
			return;
		}

		Block itemFrameBlock = absCoord.getBlock();
		if (ItemManager.getTypeId(itemFrameBlock) != CivData.AIR) ItemManager.setTypeId(itemFrameBlock, CivData.AIR);

		ItemFrameStorage itemStore;
		Entity entity = CivGlobal.getEntityAtLocation(absCoord.getBlock().getLocation());
		if (entity == null || (!(entity instanceof ItemFrame)))
			itemStore = new ItemFrameStorage(attachedBlock.getLocation(), facingDirection);
		else {
			try {
				itemStore = new ItemFrameStorage((ItemFrame) entity, attachedBlock.getLocation());
			} catch (CivException e) {
				e.printStackTrace();
				return;
			}
		}
		itemStore.setBuildable(this);
		goodieFrames.add(itemStore);
	}

	// --------------- RespawnPoint and RevivePoint

	public void addRespawnPoint(BlockCoord absCoord) {
		this.respawnPoints.add(absCoord);
	}

	public BlockCoord getRandomRespawnPoint() {
		if (this.respawnPoints.size() == 0) return null;
		Random rand = new Random();
		return this.respawnPoints.get(rand.nextInt(this.respawnPoints.size()));
	}

	@Override
	public boolean isTeleportReal() {
		for (final ControlPoint c : this.controlPoints.values()) {
			if (c.isDestroyed()) return false;
		}
		return true;
	}

	public int getRespawnTime() {
		try {
			int baseRespawn = CivSettings.getInteger(CivSettings.warConfig, "war.respawn_time");
			int controlRespawn = CivSettings.getInteger(CivSettings.warConfig, "war.control_block_respawn_time");
			int invalidRespawnPenalty = CivSettings.getInteger(CivSettings.warConfig, "war.invalid_respawn_penalty");

			int totalRespawn = baseRespawn;
			for (ControlPoint cp : this.controlPoints.values()) {
				if (cp.isDestroyed()) {
					totalRespawn += controlRespawn;
				}
			}

			if (this.validated && !this.isValid()) {
				totalRespawn += invalidRespawnPenalty * 60;
			}

			// Search for any town in our civ with the medicine goodie.
			for (Town t : this.getCiv().getTowns()) {
				if (t.getBuffManager().hasBuff(Buff.MEDICINE)) {
					int respawnTimeBonus = t.getBuffManager().getEffectiveInt(Buff.MEDICINE);
					totalRespawn = Math.max(1, (totalRespawn - respawnTimeBonus));
					break;
				}
			}

			return totalRespawn;
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
		}
		return 60;
	}

	public void addRevivePoint(BlockCoord absCoord) {
		this.revivePoints.add(absCoord);
	}

	public BlockCoord getRandomRevivePoint() {
		if (this.revivePoints.size() == 0 || !this.isComplete()) {
			return new BlockCoord(this.getCorner());
		}
		Random rand = new Random();
		int index = rand.nextInt(this.revivePoints.size());
		return this.revivePoints.get(index);

	}

	public void createControlPoint(BlockCoord absCoord, String info) {

		Location centerLoc = absCoord.getLocation();

		/* Build the fence block. */
		// for (int i = 0; i < 1; i++) {
		Block b = centerLoc.getBlock();
		ItemManager.setTypeId(b, CivData.FENCE);
		ItemManager.setData(b, 0);

		ConstructBlock sb = new ConstructBlock(new BlockCoord(b), this);
		this.addConstructBlock(sb.getCoord(), true);
		// }

		/* Build the control block. */
		b = centerLoc.getBlock().getRelative(0, 1, 0);
		ItemManager.setTypeId(b, CivData.OBSIDIAN);
		sb = new ConstructBlock(new BlockCoord(b), this);
		this.addConstructBlock(sb.getCoord(), true);

		int townhallControlHitpoints;

		if (this.getTown().getBuffManager().hasBuff("buff_oracle_extra_hp")) {
			townhallControlHitpoints = 30;
		} else
			if (this.getTown().getBuffManager().hasBuff("buff_chichen_itza_tower_hp") && this.getTown().getBuffManager().hasBuff("buff_greatlibrary_extra_beakers")) {
				townhallControlHitpoints = 30;
			} else {
				townhallControlHitpoints = 20;
			}
		if (this.getTown().hasStructure("s_castle")) {
			townhallControlHitpoints += 3;
		}
		BlockCoord coord = new BlockCoord(b);
		this.controlPoints.put(coord, new ControlPoint(coord, this, townhallControlHitpoints, info));
	}

	public void onControlBlockDestroy(Player player, ControlPoint cp) {
		// Should always have a resident and a town at this point.
		World world = cp.getWorld();
		Resident attacker = CivGlobal.getResident(player);

		ItemManager.setTypeId(cp.getCoord().getLocation().getBlock(), CivData.AIR);
		world.playSound(cp.getCoord().getLocation(), Sound.BLOCK_ANVIL_BREAK, 1.0f, -1.0f);
		world.playSound(cp.getCoord().getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);

		FireworkEffect effect = FireworkEffect.builder().with(Type.BURST).withColor(Color.YELLOW).withColor(Color.RED).withTrail().withFlicker().build();
		FireworkEffectPlayer fePlayer = new FireworkEffectPlayer();
		for (int i = 0; i < 3; i++) {
			try {
				fePlayer.playFirework(world, cp.getCoord().getLocation(), effect);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		boolean allDestroyed = true;
		for (ControlPoint c : this.controlPoints.values()) {
			if (c.isDestroyed() == false) {
				allDestroyed = false;
				break;
			}
		}
		CivMessage.sendTownSound(cp.getTown(), Sound.AMBIENT_CAVE, 1.0f, 0.5f);

		if (allDestroyed) {
			Civilization civ = getTown().getCiv();

			if (civ.getCapitolId() == this.getTown().getId()) {
				CivMessage.globalTitle(CivColor.LightBlue + CivSettings.localize.localizedString("var_townHall_destroyed_isCap", civ.getName()),
						CivSettings.localize.localizedString("var_townHall_destroyed_isCap2", attacker.getCiv().getName()));
				for (Town town : civ.getTowns()) {
					town.defeated = true;
				}

				if (this instanceof Capitol) {
					civ.updateReviveSigns();
				}
				if (civ.hasTechnology("tech_enlightenment")) {
					civ.removeTech("tech_enlightenment");
					final ConfigTech tech = CivSettings.techs.get("tech_enlightenment");
					attacker.getCiv().addTech(tech);
					CivMessage.global(CivSettings.localize.localizedString("war_defeat_loseEnlightenment", this.getTown().getCiv().getName(), attacker.getCiv().getName()));
				}
				if (civ.getCurrentMission() >= 2) {
					try {
						civ.setCurrentMission(this.getCiv().getCurrentMission() - 1);
						civ.setMissionActive(false);
						civ.updateMissionProgress(0.0, 0.0);
						civ.saveNow();
					} catch (SQLException e2) {
						e2.printStackTrace();
					}
					CivMessage.global(CivSettings.localize.localizedString("war_defeat_loseMission", civ.getName(), civ.getCurrentMission()));
				}

				War.transferDefeated(this.getTown().getCiv(), attacker.getTown().getCiv());
				WarStats.logCapturedCiv(attacker.getTown().getCiv(), this.getTown().getCiv());
				War.saveDefeatedCiv(this.getCiv(), attacker.getTown().getCiv());

				if (CivGlobal.isCasualMode()) {
					HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(this.getCiv().getRandomLeaderSkull(CivSettings.localize.localizedString("var_townHall_victoryOverItem", this.getCiv().getName())));
					for (ItemStack stack : leftovers.values()) {
						player.getWorld().dropItem(player.getLocation(), stack);
					}
				}

			} else {
				CivMessage.global(CivColor.Yellow + ChatColor.BOLD + CivSettings.localize.localizedString("var_townHall_destroyed", getTown().getName(), this.getCiv().getName(), attacker.getCiv().getName()));
				// this.getTown().onDefeat(attacker.getTown().getCiv());
				this.getTown().defeated = true;
				// War.defeatedTowns.put(this.getTown().getName(), attacker.getTown().getCiv());
				WarStats.logCapturedTown(attacker.getTown().getCiv(), this.getTown());
				War.saveDefeatedTown(this.getTown().getName(), attacker.getTown().getCiv());
			}

		} else {
			CivMessage.sendTown(cp.getTown(), CivColor.Rose + CivSettings.localize.localizedString("townHall_controlBlockDestroyed"));
			if (cp.getTown().hasWonder("w_neuschwanstein")) {
				CivMessage.sendCiv(attacker.getCiv(), CivSettings.localize.localizedString("var_townHall_didDestroyNeus", cp.getTown().getName()));
			}
			CivMessage.sendCiv(attacker.getTown().getCiv(), CivColor.LightGreen + CivSettings.localize.localizedString("var_townHall_didDestroyCB", cp.getTown().getName()));
			CivMessage.sendCiv(cp.getTown().getCiv(), CivColor.Rose + CivSettings.localize.localizedString("var_townHall_civMsg_controlBlockDestroyed", cp.getTown().getName()));
		}
	}

	public void onControlBlockCannonDestroy(ControlPoint cp, Player player, ConstructBlock hit) {
		// Should always have a resident and a town at this point.
		Resident attacker = CivGlobal.getResident(player);

		ItemManager.setTypeId(hit.getCoord().getLocation().getBlock(), CivData.AIR);

		boolean allDestroyed = true;
		for (ControlPoint c : this.controlPoints.values()) {
			if (c.isDestroyed() == false) {
				allDestroyed = false;
				break;
			}
		}
		CivMessage.sendTownSound(hit.getTown(), Sound.AMBIENT_CAVE, 1.0f, 0.5f);

		if (allDestroyed) {

			if (this.getTown().getCiv().getCapitolId() == this.getTown().getId()) {
				CivMessage.globalTitle(CivColor.LightBlue + CivSettings.localize.localizedString("var_townHall_destroyed_isCap", this.getTown().getCiv().getName()),
						CivSettings.localize.localizedString("var_townHall_destroyed_isCap2", attacker.getCiv().getName()));
				for (Town town : this.getTown().getCiv().getTowns()) {
					town.defeated = true;
				}

				War.transferDefeated(this.getTown().getCiv(), attacker.getTown().getCiv());
				WarStats.logCapturedCiv(attacker.getTown().getCiv(), this.getTown().getCiv());
				War.saveDefeatedCiv(this.getCiv(), attacker.getTown().getCiv());

				if (CivGlobal.isCasualMode()) {
					HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(this.getCiv().getRandomLeaderSkull(CivSettings.localize.localizedString("var_townHall_victoryOverItem_withCannon", this.getCiv().getName())));
					for (ItemStack stack : leftovers.values()) {
						player.getWorld().dropItem(player.getLocation(), stack);
					}
				}

			} else {
				CivMessage.global(CivColor.Yellow + ChatColor.BOLD + CivSettings.localize.localizedString("var_townHall_destroyed", getTown().getName(), this.getCiv().getName(), attacker.getCiv().getName()));
				// this.getTown().onDefeat(attacker.getTown().getCiv());
				this.getTown().defeated = true;
				// War.defeatedTowns.put(this.getTown().getName(), attacker.getTown().getCiv());
				WarStats.logCapturedTown(attacker.getTown().getCiv(), this.getTown());
				War.saveDefeatedTown(this.getTown().getName(), attacker.getTown().getCiv());
			}

		} else {
			CivMessage.sendTown(hit.getTown(), CivColor.Rose + CivSettings.localize.localizedString("townHall_controlBlockDestroyed"));
			CivMessage.sendCiv(attacker.getTown().getCiv(), CivColor.LightGreen + CivSettings.localize.localizedString("var_townHall_didDestroyCB", hit.getTown().getName()));
			CivMessage.sendCiv(hit.getTown().getCiv(), CivColor.Rose + CivSettings.localize.localizedString("var_townHall_civMsg_controlBlockDestroyed", hit.getTown().getName()));
		}
	}

	public void onControlBlockHit(Player player, ControlPoint cp) {
		cp.getWorld().playSound(cp.getCoord().getLocation(), Sound.BLOCK_ANVIL_USE, 0.2f, 1);
		cp.getWorld().playEffect(cp.getCoord().getLocation(), Effect.MOBSPAWNER_FLAMES, 0);

		CivMessage.send(player, CivColor.LightGray + CivSettings.localize.localizedString("var_townHall_damagedControlBlock", ("(" + cp.getHitpoints() + " / " + cp.getMaxHitpoints() + ")")));
		CivMessage.sendTown(cp.getTown(), CivColor.Yellow + CivSettings.localize.localizedString("townHall_cbUnderAttack"));
	}

	@Override
	public void onDamage(int amount, Player player, ConstructDamageBlock hit) {

		ControlPoint cp = this.controlPoints.get(hit.getCoord());
		Resident resident = CivGlobal.getResident(player);

		if (!resident.canDamageControlBlock()) {
			CivMessage.send(player, CivColor.Rose + CivSettings.localize.localizedString("townHall_damageCB_invalid"));
			return;
		}

		if (cp != null) {
			if (!cp.isDestroyed()) {
				if (resident.isSBPermOverride())
					cp.damage(cp.getHitpoints());
				else
					cp.damage(amount);

				if (cp.isDestroyed())
					onControlBlockDestroy(player, cp);
				else
					onControlBlockHit(player, cp);

			} else {
				CivMessage.send(player, CivColor.Rose + CivSettings.localize.localizedString("townHall_damageCB_destroyed"));
			}

		} else {
			CivMessage.send(player, CivColor.Rose + CivSettings.localize.localizedString("var_townHall_damage_notCB", this.getDisplayName()));
		}
	}

	public void regenControlBlocks() {
		for (BlockCoord coord : this.controlPoints.keySet()) {
			ItemManager.setTypeId(coord.getBlock(), CivData.OBSIDIAN);

			ControlPoint cp = this.controlPoints.get(coord);
			cp.setHitpoints(cp.getMaxHitpoints());
		}
	}

	@Override
	public void onLoad() {
		// We must load goodies into the frame as we find them from the trade outpost's
		// onLoad() function, otherwise we run into timing issues over which loads first.
	}

	@Override
	public void runOnBuild(ChunkCoord cCoord) throws CivException {
		Townhall oldTownHall = this.getTown().getTownHall();
		if (oldTownHall != null) {
			TownChunk tc = CivGlobal.getTownChunk(cCoord);
			if (tc == null || tc.getTown() != this.getTown()) throw new CivException(CivSettings.localize.localizedString("townHall_preBuild_outsideBorder"));

			if (War.isWarTime()) throw new CivException(CivSettings.localize.localizedString("townHall_preBuild_duringWar"));

			this.getTown().clearBonusGoods();

			try {
				this.getTown().demolish(oldTownHall, true);
			} catch (CivException e) {
				e.printStackTrace();
			}
			CivMessage.sendTown(this.getTown(), CivSettings.localize.localizedString("var_townHall_preBuild_Success", this.getDisplayName()));
		}
	}

	@Override
	public void onInvalidPunish() {
		int invalid_respawn_penalty;
		try {
			invalid_respawn_penalty = CivSettings.getInteger(CivSettings.warConfig, "war.invalid_respawn_penalty");
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			return;
		}

		CivMessage.sendTown(this.getTown(), CivColor.Rose + CivColor.BOLD + CivSettings.localize.localizedString("var_townHall_invalidPunish", invalid_respawn_penalty));
	}

	@Override
	public List<BlockCoord> getRespawnPoints() {
		return this.revivePoints;
	}

	@Override
	public String getRespawnName() {
		return this.getDisplayName() + "\n" + this.getTown().getName();
	}

	public HashMap<BlockCoord, ControlPoint> getControlPoints() {
		return this.controlPoints;
	}

	public void onCannonDamage(int damage, CannonProjectile projectile) {
		if (!this.getCiv().getDiplomacyManager().isAtWar()) {
			return;
		}
		this.setHitpoints(getHitpoints() - damage);

		// Resident resident = projectile.whoFired;
		if (getHitpoints() <= 0) {
			for (ControlPoint cp : this.controlPoints.values()) {
				if (cp != null) {
					if (cp.getHitpoints() > CannonProjectile.controlBlockHP) {
						cp.damage(cp.getHitpoints() - CannonProjectile.controlBlockHP);
						this.setHitpoints(this.getMaxHitPoints() / 2);
						// StructureBlock hit = CivGlobal.getStructureBlock(coord);
						// onControlBlockCannonDestroy(cp, CivGlobal.getPlayer(resident), hit);
						CivMessage.sendCiv(getCiv(), CivSettings.localize.localizedString("var_townHall_cannonHit_destroyCB", this.getDisplayName(), CannonProjectile.controlBlockHP));
						CivMessage.sendCiv(getCiv(), CivSettings.localize.localizedString("var_townHall_cannonHit_regen", this.getDisplayName(), this.getMaxHitPoints() / 2));
						return;

					}

				}
			}

			CivMessage.sendCiv(getCiv(), CivSettings.localize.localizedString("var_townHall_cannonHit_destroyed", this.getDisplayName()));
			setHitpoints(0);
		}

		CivMessage.sendCiv(getCiv(), CivSettings.localize.localizedString("var_townHall_cannonHit", this.getDisplayName(), ("(" + this.getHitpoints() + "/" + this.getMaxHitPoints() + ")")));
	}

	public void onTNTDamage(int damage) {
		if (!this.getCiv().getDiplomacyManager().isAtWar()) {
			return;
		}
		if (getHitpoints() >= damage + 1) {
			this.setHitpoints(getHitpoints() - damage);
			CivMessage.sendCiv(getCiv(), CivSettings.localize.localizedString("var_townHall_tntHit", this.getDisplayName(), ("(" + this.getHitpoints() + "/" + this.getMaxHitPoints() + ")")));
		}

	}

	@Override
	public String getDynmapDescription() {
		String out = "";
		out += "<b>" + CivSettings.localize.localizedString("var_townHall_dynmap_heading", this.getTown().getName()) + "</b>";
		ConfigCultureLevel culturelevel = CivSettings.cultureLevels.get(this.getTown().getCultureLevel());
		out += "<br/>" + CivSettings.localize.localizedString("townHall_dynmap_cultureLevel") + " " + culturelevel.level + " (" + this.getTown().getAccumulatedCulture() + "/" + culturelevel.amount + ")";
		return out;
	}
}