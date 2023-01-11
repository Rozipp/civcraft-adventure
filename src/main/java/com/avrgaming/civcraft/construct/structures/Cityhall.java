package com.avrgaming.civcraft.construct.structures;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.TreeMap;

import com.avrgaming.civcraft.construct.*;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.components.ProjectileArrowComponent;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigCultureLevel;
import com.avrgaming.civcraft.config.ConfigTech;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.ControlPoint;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.object.TownPeoplesManager.Prof;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.sync.SyncBuildUpdateTask;
import com.avrgaming.civcraft.units.ConfigUnit;
import com.avrgaming.civcraft.units.UnitStatic;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.FireworkEffectPlayer;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.SimpleBlock;
import com.avrgaming.civcraft.util.SimpleBlock.SimpleType;
import com.avrgaming.civcraft.war.War;
import com.avrgaming.civcraft.war.WarStats;

public class Cityhall extends Structure implements RespawnLocationHolder {

	private final BlockCoord[] granarybar = new BlockCoord[10];
	private ConstructSign technameSign;
	private ConstructSign techdataSign;

	private final ArrayList<BlockCoord> respawnPoints = new ArrayList<>();
	private final ArrayList<BlockCoord> revivePoints = new ArrayList<>();

	private ConstructSign respawnSign;
	private int indexTown = 0;

	protected HashMap<BlockCoord, ControlPoint> controlPoints = new HashMap<>();

	private final HashMap<BlockCoord, ProjectileArrowComponent> arrowTowers = new HashMap<>();

	public Cityhall(String id, Town town) {
		super(id, town);
	}

	// ------------ build
	@Override
	public void commandBlockRelatives(BlockCoord absCoord, SimpleBlock sb) {
		ConstructSign structSign;
		switch (sb.command) {
		case "/prevunit":
			ItemManager.setTypeId(absCoord.getBlock(), sb.getType());
			ItemManager.setData(absCoord.getBlock(), sb.getData());
			structSign = new ConstructSign(absCoord, this);
			structSign.setText("\n" + ChatColor.BOLD + ChatColor.UNDERLINE + CivSettings.localize.localizedString("barracks_sign_previousUnit"));
			structSign.setDirection(sb.getData());
			structSign.setAction("prevunit");
			structSign.update();
			this.addConstructSign(structSign);
			break;
		case "/unitname":
			ItemManager.setTypeId(absCoord.getBlock(), sb.getType());
			ItemManager.setData(absCoord.getBlock(), sb.getData());
			structSign = new ConstructSign(absCoord, this);
			structSign.setText(getUnitSignText(0));
			structSign.setDirection(sb.getData());
			structSign.setAction("info");
			structSign.update();
			this.unitNameSign = structSign;
			this.addConstructSign(structSign);
			break;
		case "/nextunit":
			ItemManager.setTypeId(absCoord.getBlock(), sb.getType());
			ItemManager.setData(absCoord.getBlock(), sb.getData());
			structSign = new ConstructSign(absCoord, this);
			structSign.setText("\n" + ChatColor.BOLD + ChatColor.UNDERLINE + CivSettings.localize.localizedString("barracks_sign_nextUnit"));
			structSign.setDirection(sb.getData());
			structSign.setAction("nextunit");
			structSign.update();
			this.addConstructSign(structSign);
			break;
		case "/train":
			ItemManager.setTypeId(absCoord.getBlock(), sb.getType());
			ItemManager.setData(absCoord.getBlock(), sb.getData());
			structSign = new ConstructSign(absCoord, this);
			structSign.setText("\n" + ChatColor.BOLD + ChatColor.UNDERLINE + CivSettings.localize.localizedString("barracks_sign_train"));
			structSign.setDirection(sb.getData());
			structSign.setAction("train");
			structSign.update();
			this.addConstructSign(structSign);
			break;
		case "/progress":
			ItemManager.setTypeId(absCoord.getBlock(), sb.getType());
			ItemManager.setData(absCoord.getBlock(), sb.getData());
			structSign = new ConstructSign(absCoord, this);
			structSign.setText("");
			structSign.setDirection(sb.getData());
			structSign.setAction("");
			structSign.update();
			this.addConstructSign(structSign);
			this.progresBar.put(Integer.valueOf(sb.keyvalues.get("id")), structSign);
			break;
		case "/techbar":
			String strvalue = sb.keyvalues.get("id");
			if (strvalue != null) granarybar[Integer.parseInt(strvalue)] = absCoord;
			break;
		case "/techname":
			this.technameSign = new ConstructSign(absCoord, this);
			break;
		case "/techdata":
			this.techdataSign = new ConstructSign(absCoord, this);
			break;
		// case "/itemframe":
		// strvalue = sb.keyvalues.get("id");
		// if (strvalue != null) {
		// this.createGoodieItemFrame(absCoord, Integer.valueOf(strvalue), sb.getData());
		// this.addConstructBlock(absCoord, false);
		// }
		// break;
		case "/respawn":
			this.respawnPoints.add(absCoord);
			break;
		case "/revive":
			this.revivePoints.add(absCoord);
			break;
		case "/control":
			this.createControlPoint(absCoord, "");
			break;
		case "/towerfire":
			if (!arrowTowers.containsKey(absCoord)) {
				ProjectileArrowComponent arrowTower = new ProjectileArrowComponent(this);
				arrowTower.createComponent(this);
				arrowTower.setTurretLocation(absCoord);
				arrowTowers.put(absCoord, arrowTower);
			}
			break;
		case "/next":
			ItemManager.setTypeId(absCoord.getBlock(), sb.getType());
			ItemManager.setData(absCoord.getBlock(), sb.getData());
			structSign = new ConstructSign(absCoord, this);
			structSign.setText("\n" + ChatColor.BOLD + ChatColor.UNDERLINE + CivSettings.localize.localizedString("capitol_sign_nextLocation"));
			structSign.setDirection(sb.getData());
			structSign.setAction("next");
			structSign.update();
			this.addConstructSign(structSign);
			break;
		case "/prev":
			ItemManager.setTypeId(absCoord.getBlock(), sb.getType());
			ItemManager.setData(absCoord.getBlock(), sb.getData());
			structSign = new ConstructSign(absCoord, this);
			structSign.setText("\n" + ChatColor.BOLD + ChatColor.UNDERLINE + CivSettings.localize.localizedString("capitol_sign_previousLocation"));
			structSign.setDirection(sb.getData());
			structSign.setAction("prev");
			structSign.update();
			this.addConstructSign(structSign);
			break;
		case "/respawndata":
			ItemManager.setTypeId(absCoord.getBlock(), sb.getType());
			ItemManager.setData(absCoord.getBlock(), sb.getData());
			structSign = new ConstructSign(absCoord, this);
			structSign.setText(CivSettings.localize.localizedString("capitol_sign_Capitol"));
			structSign.setDirection(sb.getData());
			structSign.setAction("respawn");
			structSign.update();
			this.addConstructSign(structSign);
			this.respawnSign = structSign;
			changeIndex(indexTown);
			break;
		case "/tohome":
			ItemManager.setTypeId(absCoord.getBlock(), sb.getType());
			ItemManager.setData(absCoord.getBlock(), sb.getData());
			structSign = new ConstructSign(absCoord, this);
			structSign.setText("ДОМОЙ");
			structSign.setDirection(sb.getData());
			structSign.setAction("tohome");
			structSign.update();
			this.addConstructSign(structSign);
			break;
		}
	}

	@Override
	public void onPostBuild() {
		// (new UpdateTechBar(this.getCiv())).run();
	}

	@Override
	public void runOnBuild(ChunkCoord cCoord) throws CivException {
		// Cityhall oldTownHall = this.getTown().getTownHall();
		// if (oldTownHall != null) {
		// TownChunk tc = CivGlobal.getTownChunk(cCoord);
		// if (tc == null || tc.getTown() != this.getTown()) throw new
		// CivException(CivSettings.localize.localizedString("townHall_preBuild_outsideBorder"));
		//
		// if (War.isWarTime()) throw new CivException(CivSettings.localize.localizedString("townHall_preBuild_duringWar"));
		//
		// this.getTown().clearBonusGoods();
		//
		// try {
		// this.getTown().demolish(oldTownHall, true);
		// } catch (CivException e) {
		// e.printStackTrace();
		// }
		// CivMessage.sendTown(this.getTown(), CivSettings.localize.localizedString("var_townHall_preBuild_Success", this.getDisplayName()));
		// }
	}

	@Override
	public void processSignAction(Player player, ConstructSign sign, PlayerInteractEvent event) {
		// int special_id = Integer.valueOf(sign.getAction());
		Resident resident = CivGlobal.getResident(player);
		if (resident == null) return;

		boolean hasPermission = false;
		if ((resident.getTown().GM.isMayorOrAssistant(resident)) || (resident.getCiv().GM.isLeaderOrAdviser(resident))) hasPermission = true;

		switch (sign.getAction()) {
		case "info":
			ConfigUnit cUnit = getTownOwner().getAvailableUnits().get(indexUnit);
			CivMessage.send(player, "Выбран юнит " + cUnit.name);
			for (String ss : cUnit.lore)
				CivMessage.send(player, "" + ss);
			break;
		case "prevunit":
			changeIndexUnit((indexUnit - 1));
			break;
		case "nextunit":
			changeIndexUnit((indexUnit + 1));
			break;
		case "train":
			if (resident.hasTown()) {
				try {
					train(resident);
				} catch (CivException e) {
					CivMessage.send(player, CivColor.Rose + e.getMessage());
				}
			}
			break;
		case "prev":
			if (!War.isWarTime()) return;
			if (hasPermission)
				changeIndex((indexTown - 1));
			else
				CivMessage.sendError(resident, CivSettings.localize.localizedString("capitol_Sign_noPermission"));
			break;
		case "next":
			if (!War.isWarTime()) return;
			if (hasPermission)
				changeIndex((indexTown + 1));
			else
				CivMessage.sendError(resident, CivSettings.localize.localizedString("capitol_Sign_noPermission"));
			break;
		case "respawn":
			if (!War.isWarTime()) return;
			ArrayList<RespawnLocationHolder> respawnables = this.getTownOwner().getCiv().getAvailableRespawnables();
			if (indexTown >= respawnables.size()) {
				indexTown = 0;
				changeIndex(indexTown);
				CivMessage.sendError(resident, CivSettings.localize.localizedString("capitol_cannotRespawn"));
				return;
			}
			RespawnLocationHolder holder = getSelectedHolder();
			int respawnTimeSeconds = this.getRespawnTime();
			Date now = new Date();
			if (resident.getLastKilledTime() != null) {
				long secondsLeft = (resident.getLastKilledTime().getTime() + (respawnTimeSeconds * 1000)) - now.getTime();
				if (secondsLeft > 0) {
					secondsLeft /= 1000;
					CivMessage.sendError(resident, CivColor.Rose + CivSettings.localize.localizedString("var_capitol_secondsLeftTillRespawn", secondsLeft));
					return;
				}
			}
			BlockCoord revive = holder.getRandomRevivePoint();
			Location loc = (revive == null) ? player.getBedSpawnLocation() : revive.getLocation();
			CivMessage.send(player, CivColor.LightGreen + CivSettings.localize.localizedString("capitol_respawningAlert"));
			player.teleport(loc);
			break;
		case "tohome":
			if (!War.isWarTime()) return;
			holder = resident.getTown().getCityhall();
			respawnTimeSeconds = this.getRespawnTime();
			now = new Date();

			if (resident.getLastKilledTime() != null) {
				long secondsLeft = (resident.getLastKilledTime().getTime() + (respawnTimeSeconds * 1000)) - now.getTime();
				if (secondsLeft > 0) {
					secondsLeft /= 1000;
					CivMessage.sendError(resident, CivColor.Rose + CivSettings.localize.localizedString("var_capitol_secondsLeftTillRespawn", secondsLeft));
					return;
				}
			}
			revive = holder.getRandomRevivePoint();
			loc = (revive == null) ? player.getBedSpawnLocation() : revive.getLocation();
			CivMessage.send(player, CivColor.LightGreen + CivSettings.localize.localizedString("capitol_respawningAlert"));
			player.teleport(loc);
			break;
		}
	}

	// --------------- RespawnPoint and RevivePoint

	public BlockCoord getRandomRespawnPoint() {
		if (this.respawnPoints.size() == 0) return null;
		Random rand = new Random();
		return this.respawnPoints.get(rand.nextInt(this.respawnPoints.size()));
	}

	@Override
	public BlockCoord getRandomRevivePoint() {
		if (this.revivePoints.size() == 0 || !this.isComplete()) return this.getCorner().clone();
		Random rand = new Random();
		int index = rand.nextInt(this.revivePoints.size());
		return this.revivePoints.get(index);
	}

	@Override
	public List<BlockCoord> getRespawnPoints() {
		return this.revivePoints;
	}

	@Override
	public String getRespawnName() {
		return this.getDisplayName() + "\n" + this.getTownOwner().getName();
	}

	public void updateRespawnSigns() {
		// TODO Auto-generated method stub
	}

	private RespawnLocationHolder getSelectedHolder() {
		ArrayList<RespawnLocationHolder> respawnables = this.getTownOwner().getCiv().getAvailableRespawnables();
		return respawnables.get(indexTown);
	}

	private void changeIndex(int newIndex) {
		ArrayList<RespawnLocationHolder> respawnables = this.getTownOwner().getCiv().getAvailableRespawnables();

		if (this.respawnSign != null) {
			try {
				this.respawnSign.setText(CivSettings.localize.localizedString("capitol_sign_respawnAt") + "\n" + CivColor.Green + CivColor.BOLD + respawnables.get(newIndex).getRespawnName());
				indexTown = newIndex;
			} catch (IndexOutOfBoundsException e) {
				if (respawnables.size() > 0) {
					this.respawnSign.setText(CivSettings.localize.localizedString("capitol_sign_respawnAt") + "\n" + CivColor.Green + CivColor.BOLD + respawnables.get(0).getRespawnName());
					indexTown = 0;
				}
			}
			this.respawnSign.update();
		} else
			CivLog.warning("Could not find civ spawn sign:" + this.getId() + " at " + this.getCorner());
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
				if (cp.isDestroyed()) totalRespawn += controlRespawn;
			}

			if (this.validated && !this.isValid()) totalRespawn += invalidRespawnPenalty * 60;

			// Search for any town in our civ with the medicine goodie.
			for (Town t : this.getCivOwner().getTowns()) {
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

	// ----------- ControlPoint

	public void createControlPoint(BlockCoord absCoord, String info) {
		/* Build the fence block. */
		// for (int i = 0; i < 1; i++) {
		Block b = absCoord.getBlock();
		ItemManager.setTypeId(b, CivData.FENCE);
		ItemManager.setData(b, 0);

		ConstructBlock sb = new ConstructBlock(new BlockCoord(b), this);
		this.addConstructBlock(sb.getCoord(), true);
		// }

		/* Build the control block. */
		b = absCoord.getBlock().getRelative(0, 1, 0);
		ItemManager.setTypeId(b, CivData.OBSIDIAN);
		sb = new ConstructBlock(new BlockCoord(b), this);
		this.addConstructBlock(sb.getCoord(), true);

		int townhallControlHitpoints = 50;

		if (this.getTownOwner().getBuffManager().hasBuff("buff_oracle_extra_hp")) townhallControlHitpoints += 20;
		if (this.getTownOwner().getBuffManager().hasBuff("buff_chichen_itza_tower_hp")) townhallControlHitpoints += 20;
		if (this.getTownOwner().BM.hasStructure("s_castle")) townhallControlHitpoints += 5;

		BlockCoord coord = new BlockCoord(b);
		this.controlPoints.put(coord, new ControlPoint(coord, this, townhallControlHitpoints, info));
	}

	public HashMap<BlockCoord, ControlPoint> getControlPoints() {
		return this.controlPoints;
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
			if (!c.isDestroyed()) {
				allDestroyed = false;
				break;
			}
		}
		CivMessage.sendTownSound(cp.getTown(), Sound.AMBIENT_CAVE, 1.0f, 0.5f);

		if (allDestroyed)
			onAllControlBlockDestroy(player);
		else {
			CivMessage.sendTown(cp.getTown(), CivColor.Rose + CivSettings.localize.localizedString("townHall_controlBlockDestroyed"));
			if (cp.getTown().BM.hasWonder("w_neuschwanstein")) {
				CivMessage.sendCiv(attacker.getCiv(), CivSettings.localize.localizedString("var_townHall_didDestroyNeus", cp.getTown().getName()));
			}
			CivMessage.sendCiv(attacker.getTown().getCiv(), CivColor.LightGreen + CivSettings.localize.localizedString("var_townHall_didDestroyCB", cp.getTown().getName()));
			CivMessage.sendCiv(cp.getTown().getCiv(), CivColor.Rose + CivSettings.localize.localizedString("var_townHall_civMsg_controlBlockDestroyed", cp.getTown().getName()));
		}
	}

	public void onAllControlBlockDestroy(Player player) {
		Resident attacker = CivGlobal.getResident(player);
		Civilization civ = getTownOwner().getCiv();

		if (civ.getCapitolId() == this.getTownOwner().getId()) {
			CivMessage.globalTitle(CivColor.LightBlue + CivSettings.localize.localizedString("var_townHall_destroyed_isCap", civ.getName()),
					CivSettings.localize.localizedString("var_townHall_destroyed_isCap2", attacker.getCiv().getName()));
			for (Town town : civ.getTowns()) {
				town.defeated = true;
			}

			civ.updateReviveSigns();
			if (civ.hasTechnologys("tech_enlightenment")) {
				civ.removeTech("tech_enlightenment");
				final ConfigTech tech = CivSettings.techs.get("tech_enlightenment");
				attacker.getCiv().addTech(tech);
				CivMessage.global(CivSettings.localize.localizedString("war_defeat_loseEnlightenment", this.getTownOwner().getCiv().getName(), attacker.getCiv().getName()));
			}
			if (civ.getCurrentMission() >= 2) {
				try {
					civ.setCurrentMission(this.getCivOwner().getCurrentMission() - 1);
					civ.setMissionActive(false);
					civ.updateMissionProgress(0.0, 0.0);
					civ.saveNow();
				} catch (SQLException e2) {
					e2.printStackTrace();
				}
				CivMessage.global(CivSettings.localize.localizedString("war_defeat_loseMission", civ.getName(), civ.getCurrentMission()));
			}

			War.transferDefeated(this.getTownOwner().getCiv(), attacker.getTown().getCiv());
			WarStats.logCapturedCiv(attacker.getTown().getCiv(), this.getTownOwner().getCiv());
			War.saveDefeatedCiv(this.getCivOwner(), attacker.getTown().getCiv());
			if (CivGlobal.isCasualMode()) {
				HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(this.getCivOwner().getRandomLeaderSkull(CivSettings.localize.localizedString("var_townHall_victoryOverItem", this.getCivOwner().getName())));
				for (ItemStack stack : leftovers.values()) {
					player.getWorld().dropItem(player.getLocation(), stack);
				}
			}
		} else {
			CivMessage.global(CivColor.Yellow + ChatColor.BOLD + CivSettings.localize.localizedString("var_townHall_destroyed", getTownOwner().getName(), this.getCivOwner().getName(), attacker.getCiv().getName()));
			// this.getTown().onDefeat(attacker.getTown().getCiv());
			this.getTownOwner().defeated = true;
			// War.defeatedTowns.put(this.getTown().getName(), attacker.getTown().getCiv());
			WarStats.logCapturedTown(attacker.getTown().getCiv(), this.getTownOwner());
			War.saveDefeatedTown(this.getTownOwner(), attacker.getTown().getCiv());
		}
	}

	public void onControlBlockCannonDestroy(ControlPoint cp, Player player, ConstructBlock hit) {
		// // Should always have a resident and a town at this point.
		// Resident attacker = CivGlobal.getResident(player);
		//
		// ItemManager.setTypeId(hit.getCoord().getLocation().getBlock(), CivData.AIR);
		//
		// boolean allDestroyed = true;
		// for (ControlPoint c : this.controlPoints.values()) {
		// if (c.isDestroyed() == false) {
		// allDestroyed = false;
		// break;
		// }
		// }
		// CivMessage.sendTownSound(hit.getTown(), Sound.AMBIENT_CAVE, 1.0f, 0.5f);
		//
		// if (allDestroyed) {
		// onAllControlBlockDestroy(player);
		// } else {
		// CivMessage.sendTown(hit.getTown(), CivColor.Rose + CivSettings.localize.localizedString("townHall_controlBlockDestroyed"));
		// CivMessage.sendCiv(attacker.getTown().getCiv(), CivColor.LightGreen + CivSettings.localize.localizedString("var_townHall_didDestroyCB",
		// hit.getTown().getName()));
		// CivMessage.sendCiv(hit.getTown().getCiv(), CivColor.Rose +
		// CivSettings.localize.localizedString("var_townHall_civMsg_controlBlockDestroyed", hit.getTown().getName()));
		// }
	}

	public void onControlBlockHit(Player player, ControlPoint cp) {
		cp.getWorld().playSound(cp.getCoord().getLocation(), Sound.BLOCK_ANVIL_USE, 0.2f, 1);
		cp.getWorld().playEffect(cp.getCoord().getLocation(), Effect.MOBSPAWNER_FLAMES, 0);
		CivMessage.send(player, CivColor.LightGray + CivSettings.localize.localizedString("var_townHall_damagedControlBlock", ("(" + cp.getHitpoints() + " / " + cp.getMaxHitpoints() + ")")));
		CivMessage.sendTown(cp.getTown(), CivColor.Yellow + CivSettings.localize.localizedString("townHall_cbUnderAttack"));
	}

	// --------------- Damage regen

	@Override
	public void onDamage(int amount, Player player, ConstructDamageBlock hit) {
		ControlPoint cp = this.controlPoints.get(hit.getCoord());
		Resident resident = CivGlobal.getResident(player);
		if (cp != null) {
			if (!resident.canDamageControlBlock()) {
				CivMessage.send(player, CivColor.Rose + CivSettings.localize.localizedString("townHall_damageCB_invalid"));
				return;
			}
			if (!cp.isDestroyed()) {
				cp.damage(resident.isSBPermOverride() ? cp.getHitpoints() : amount);
				if (cp.isDestroyed())
					onControlBlockDestroy(player, cp);
				else
					onControlBlockHit(player, cp);
			} else
				CivMessage.send(player, CivColor.Rose + CivSettings.localize.localizedString("townHall_damageCB_destroyed"));
		} else
			CivMessage.send(player, CivColor.Rose + CivSettings.localize.localizedString("var_townHall_damage_notCB", this.getDisplayName()));
	}

	public void regenControlBlocks() {
		for (BlockCoord coord : this.controlPoints.keySet()) {
			ItemManager.setTypeId(coord.getBlock(), CivData.OBSIDIAN);
			ControlPoint cp = this.controlPoints.get(coord);
			cp.setHitpoints(cp.getMaxHitpoints());
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
		CivMessage.sendTown(this.getTownOwner(), CivColor.Rose + CivColor.BOLD + CivSettings.localize.localizedString("var_townHall_invalidPunish", invalid_respawn_penalty));
	}

	public void onCannonDamage(int damage, CannonProjectile projectile) {
		if (!this.getCivOwner().getDiplomacyManager().isAtWar()) return;
		this.setHitpoints(getHitpoints() - damage);

		// Resident resident = projectile.whoFired;
		if (getHitpoints() <= 0) {
			for (BlockCoord coord : this.controlPoints.keySet()) {
				ControlPoint cp = this.controlPoints.get(coord);
				if (cp != null) {
					if (cp.getHitpoints() > CannonProjectile.controlBlockHP) {
						cp.damage(cp.getHitpoints() - 1);
						this.setHitpoints(this.getMaxHitPoints() / 2);
						// StructureBlock hit = CivGlobal.getStructureBlock(coord);
						// onControlBlockCannonDestroy(cp, CivGlobal.getPlayer(resident), hit);
						CivMessage.sendCiv(getCivOwner(), CivSettings.localize.localizedString("var_townHall_cannonHit_destroyCB", this.getDisplayName(), CannonProjectile.controlBlockHP));
						CivMessage.sendCiv(getCivOwner(), CivSettings.localize.localizedString("var_townHall_cannonHit_regen", this.getDisplayName(), this.getMaxHitPoints() / 2));
						return;
					}
				}
			}
			CivMessage.sendCiv(getCivOwner(), CivSettings.localize.localizedString("var_townHall_cannonHit_destroyed", this.getDisplayName()));
			setHitpoints(0);
		}
		CivMessage.sendCiv(getCivOwner(), CivSettings.localize.localizedString("var_townHall_cannonHit", this.getDisplayName(), ("(" + this.getHitpoints() + "/" + this.getMaxHitPoints() + ")")));
	}

	public void onTNTDamage(int damage) {
		if (!this.getCivOwner().getDiplomacyManager().isAtWar()) return;
		if (getHitpoints() >= damage + 1) {
			this.setHitpoints(getHitpoints() - damage);
			CivMessage.sendCiv(getCivOwner(), CivSettings.localize.localizedString("var_townHall_tntHit", this.getDisplayName(), ("(" + this.getHitpoints() + "/" + this.getMaxHitPoints() + ")")));
		}
	}

	// ------------- unit

	private static final long SAVE_INTERVAL = 60 * 1000;

	private int indexUnit = 0;
	private ConstructSign unitNameSign;
	private ConfigUnit trainingUnit = null;
	private double currentHammers = 0.0;
	private final TreeMap<Integer, ConstructSign> progresBar = new TreeMap<>();
	private Date lastSave = null;

	private String getUnitSignText(int index) throws IndexOutOfBoundsException {
		ArrayList<ConfigUnit> unitList = getTownOwner().getAvailableUnits();
		if (unitList.size() == 0) return "\n" + CivColor.LightGray + CivSettings.localize.localizedString("Nothing") + "\n" + CivColor.LightGray + CivSettings.localize.localizedString("Available");
		ConfigUnit unit = unitList.get(index);
		String out = "\n";
		double coinCost = unit.cost;
		out += CivColor.LightPurple + unit.name + "\n";
		out += CivColor.Yellow + coinCost + "\n";
		out += CivColor.Yellow + CivSettings.CURRENCY_NAME;
		return out;
	}

	private void changeIndexUnit(int newIndex) {
		if (this.unitNameSign != null) {
			try {
				this.unitNameSign.setText(getUnitSignText(newIndex));
				indexUnit = newIndex;
			} catch (IndexOutOfBoundsException e) {
				indexUnit = 0;
				this.unitNameSign.setText(getUnitSignText(indexUnit));
			}
			this.unitNameSign.update();
		} else {
			CivLog.warning("Could not find unit name sign for barracks:" + this.getId() + " at " + this.getCorner());
		}
	}

	private void train(Resident whoClicked) throws CivException {
		if (!getTownOwner().GM.isMayorOrAssistant(whoClicked)) throw new CivException(CivSettings.localize.localizedString("barracks_actionNoPerms"));
		ArrayList<ConfigUnit> unitList = getTownOwner().getAvailableUnits();
		ConfigUnit unit = unitList.get(indexUnit);
		if (unit == null) throw new CivException(CivSettings.localize.localizedString("barracks_unknownUnit"));
		// TODO Добавить проверку на количество юнитов if (unit.limit != 0 && unit.limit < getTown().getUnitTypeCount(unit.id)) throw new
		// CivException(CivSettings.localize.localizedString("var_barracks_atLimit", unit.name));
		if (!unit.isAvailable(getTownOwner())) throw new CivException(CivSettings.localize.localizedString("barracks_unavailable"));
		if (this.trainingUnit != null) throw new CivException(CivSettings.localize.localizedString("var_barracks_inProgress", this.trainingUnit.name));
		double coinCost = unit.cost;
		if (!getTownOwner().getTreasury().hasEnough(coinCost)) throw new CivException(CivSettings.localize.localizedString("var_barracks_tooPoor", unit.name, coinCost, CivSettings.CURRENCY_NAME));
		getTownOwner().getTreasury().withdraw(coinCost);
		this.currentHammers = 0.0;
		this.trainingUnit = unit;
		CivMessage.sendTown(getTownOwner(), CivSettings.localize.localizedString("var_barracks_begin", unit.name));
		this.onCivtickUpdate(null);
		this.onTechUpdate();
	}

	@Override
	public void onTechUpdate() {
		TaskMaster.syncTask(() -> {
			unitNameSign.setText(getUnitSignText(indexUnit));
			unitNameSign.update();
		});
	}

	public void createUnit(ConfigUnit unit) {
		// Find the chest inventory
		// ArrayList<ConstructChest> chests = this.getAllChestsById("0");
		// if (chests.size() == 0) return;
		try {
			UnitStatic.spawn(this.getTownOwner(), unit.id);
			CivMessage.sendTown(this.getTownOwner(), CivSettings.localize.localizedString("var_barracks_completedTraining", unit.name));
			this.trainingUnit = null;
			this.currentHammers = 0.0;
			CivGlobal.getSessionDatabase().delete_all(getSessionKey());
			getTownOwner().PM.hirePeoples(Prof.UNIT, 1);
		} catch (CivException e) {
			this.trainingUnit = null;
			this.currentHammers = 0.0;
			e.getCause().getMessage();
			e.printStackTrace();
			CivMessage.sendTown(getTownOwner(), CivColor.Rose + e.getMessage());
		}
	}

	public void updateProgressBar() {
		if (this.trainingUnit == null) return;
		TaskMaster.syncTask(() -> {
			double percentageDone;
			if (this.trainingUnit == null) return;
			percentageDone = this.currentHammers / this.trainingUnit.hammer_cost;
			int size = this.progresBar.size();
			int textCount = (int) (size * 16 * percentageDone);
			int textIndex = 0;
			for (int i = 0; i < size; i++) {
				ConstructSign structSign = this.progresBar.get(i);
				String[] text = new String[4];
				text[0] = "";
				text[1] = "";
				text[2] = "";
				text[3] = "";
				for (int j = 0; j < 16; j++) {
					text[2] += (textIndex == 0) ? "[" : (textIndex == ((size * 15) + 3)) ? "]" : (textIndex < textCount) ? "=" : "_";
					textIndex++;
				}
				if (i == (size / 2)) text[1] = CivColor.LightGreen + this.trainingUnit.name;
				structSign.setText(text);
				structSign.update();
			}
		});
	}

	public String getSessionKey() {
		return this.getTownOwner().getName() + ":" + "barracks" + ":" + this.getId();
	}

	public void saveProgress() {
		TaskMaster.asyncTask(() -> {
			if (this.trainingUnit != null) {
				String key = getSessionKey();
				String value = this.trainingUnit.id + ":" + this.currentHammers;
				ArrayList<SessionEntry> entries = CivGlobal.getSessionDatabase().lookup(key);
				if (entries.size() > 0) {
					SessionEntry entry = entries.get(0);
					CivGlobal.getSessionDatabase().update(entry.request_id, key, value);

					/* delete any bad extra entries. */
					for (int i = 1; i < entries.size(); i++) {
						SessionEntry bad_entry = entries.get(i);
						CivGlobal.getSessionDatabase().delete(bad_entry.request_id, key);
					}
				} else {
					this.sessionAdd(key, value);
				}
				lastSave = new Date();
			}
		}, 0);
	}

	@Override
	public void onLoad() {
		String key = getSessionKey();
		ArrayList<SessionEntry> entries = CivGlobal.getSessionDatabase().lookup(key);
		if (entries.size() > 0) {
			SessionEntry entry = entries.get(0);
			String[] values = entry.value.split(":");
			this.trainingUnit = UnitStatic.configUnits.get(values[0]);
			if (trainingUnit == null) {
				CivLog.error("Couldn't find in-progress unit id:" + values[0] + " for town " + this.getTownOwner().getName());
				return;
			}
			this.currentHammers = Double.parseDouble(values[1]);
			/* delete any bad extra entries. */
			for (int i = 1; i < entries.size(); i++) {
				SessionEntry bad_entry = entries.get(i);
				CivGlobal.getSessionDatabase().delete(bad_entry.request_id, key);
			}
		}
	}

	public void onUnitCivtickUpdate() {
		if (this.trainingUnit != null) {
			// Hammers are per hour, this runs per min. We need to adjust the hammers we add.
			double addedHammers = getTownOwner().PM.progressBuildGetSupplies((int) this.trainingUnit.hammer_cost);
			this.currentHammers += addedHammers;
			this.updateProgressBar();
			Date now = new Date();
			if (lastSave == null || ((lastSave.getTime() + SAVE_INTERVAL) < now.getTime())) this.saveProgress();
			if (this.currentHammers >= this.trainingUnit.hammer_cost) {
				this.currentHammers = this.trainingUnit.hammer_cost;
				this.createUnit(this.trainingUnit);
			}
		}
	}

	// ------------- other

	private int lastFoodBasketCount = -1;
	private int lastPeopleTotal = -1;
	private int lastPercentageDone = -1;
	private ConfigTech lastResearchTech = new ConfigTech();

	public void updateFoodBasket() {
		Queue<SimpleBlock> sbs = new LinkedList<>();

		if (!this.isActive()) return;
		Town town = getTownOwner();
		SimpleBlock sb;
		/* Get the number of blocks to light up. */
		int size = this.granarybar.length;
		int blockCount = (int) (1.0 * size * town.SM.getFoodBasket() / town.SM.getFoodBasketSize());
		if (lastFoodBasketCount != blockCount) {
			for (int i = 0; i < size; i++) {
				BlockCoord bcoord = this.granarybar[i];
				if (bcoord == null) continue;/* tech bar DNE, might not be finished yet. */
				sb = new SimpleBlock(bcoord, null);
				if (i <= blockCount)
					sb.setTypeAndData(CivData.WOOL, CivData.DATA_WOOL_GREEN);
				else
					sb.setTypeAndData(CivData.WOOL, CivData.DATA_WOOL_BLACK);
				sbs.add(sb);
				this.addConstructBlock(this.granarybar[i], false);
			}
			lastFoodBasketCount = blockCount;
		}

		if (lastPeopleTotal != town.PM.getPeoplesTotal()) {
			if (this.technameSign != null) {
				ConstructSign sign = this.technameSign;
				sb = new SimpleBlock(CivData.WALL_SIGN, sign.getDirection());
				sb.setBlockCoord(sign.getCoord());
				sb.specialType = SimpleType.LITERAL;
				sb.message[0] = "Население города";
				sb.message[1] = "";
				sb.message[2] = "" + town.PM.getPeoplesTotal();
				sb.message[3] = "";
				sbs.add(sb);
				this.addConstructBlock(sign.getCoord(), false);
			}
			lastPeopleTotal = town.PM.getPeoplesTotal();
		}
		SyncBuildUpdateTask.queueSimpleBlock(sbs);
	}

	public void updateResearchSign() {
		Queue<SimpleBlock> sbs = new LinkedList<>();

		if (!this.isActive()) return;
		ConstructSign sign = this.techdataSign;
		if (sign != null) {
			SimpleBlock sb = new SimpleBlock(CivData.WALL_SIGN, sign.getDirection());
			Civilization civ = getCivOwner();
			if (civ.getResearchTech() == null) {
				if (lastResearchTech != civ.getResearchTech()) {
					sb.setBlockCoord(sign.getCoord());
					sb.specialType = SimpleType.LITERAL;
					sb.message[0] = CivSettings.localize.localizedString("UpdateTechBar_sign_Use");
					sb.message[1] = "/civ research";
					sb.message[2] = CivSettings.localize.localizedString("UpdateTechBar_sign_toStart");
					sb.message[3] = CivSettings.localize.localizedString("UpdateTechBar_sign_Researching");
					sbs.add(sb);
					this.addConstructBlock(sign.getCoord(), false);
					lastResearchTech = civ.getResearchTech();
				}
			} else {
				int percentageDone = (int) Math.round(civ.getResearchProgress() / civ.getResearchTech().getAdjustedBeakerCost(civ) * 100);
				if (lastPercentageDone != percentageDone) {
					sb.setBlockCoord(sign.getCoord());
					sb.specialType = SimpleType.LITERAL;
					sb.message[0] = CivSettings.localize.localizedString("Researching");
					sb.message[1] = civ.getResearchTech().name;
					sb.message[2] = CivSettings.localize.localizedString("UpdateTechBar_sign_Percent") + " " + CivSettings.localize.localizedString("UpdateTechBar_sign_Complete");
					sb.message[3] = "" + percentageDone + "%";
					sbs.add(sb);
					this.addConstructBlock(sign.getCoord(), false);
				}
				lastPercentageDone = percentageDone;
				lastResearchTech = civ.getResearchTech();
			}
		}
		SyncBuildUpdateTask.queueSimpleBlock(sbs);
	}

	@Override
	public String getDynmapDescription() {
		String out = "";
		out += "<b>" + CivSettings.localize.localizedString("var_townHall_dynmap_heading", this.getTownOwner().getName()) + "</b>";
		ConfigCultureLevel culturelevel = CivSettings.cultureLevels.get(this.getTownOwner().SM.getLevel());
		out += "<br/>" + CivSettings.localize.localizedString("townHall_dynmap_cultureLevel") + " " + culturelevel.level + " (" + this.getTownOwner().SM.getCulture() + "/" + culturelevel.amount + ")";
		return out;
	}
}
