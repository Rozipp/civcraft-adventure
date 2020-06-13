//
// Decompiled by Procyon v0.5.30
//

package com.avrgaming.civcraft.structure.wonders;

import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.SimpleBlock;
import com.avrgaming.civcraft.util.BlockCoord;
import org.bukkit.command.CommandSender;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.threading.TaskMaster;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Inventory;
import java.util.Collection;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.threading.sync.ValidateShuttleSync;
import com.avrgaming.civcraft.threading.sync.request.UpdateInventoryRequest;
import com.avrgaming.civcraft.object.Resident;

import java.util.HashMap;

import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.util.MultiInventory;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigSpaceMissions;
import com.avrgaming.civcraft.config.ConfigSpaceRocket;
import com.avrgaming.civcraft.construct.ConstructChest;
import com.avrgaming.civcraft.construct.ConstructSign;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.CivTaskAbortException;
import com.avrgaming.civcraft.items.CraftableCustomMaterial;
import com.avrgaming.civcraft.items.CustomMaterial;

import org.bukkit.entity.Player;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import java.sql.SQLException;
import java.sql.ResultSet;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.Buildable;

public class SpaceShuttle extends Wonder {
	protected SpaceShuttle(String id, Town town) throws CivException {
		super(id, town);
	}

	public SpaceShuttle(ResultSet rs) throws SQLException, CivException {
		super(rs);
	}

	@Override
	public void loadSettings() {
		super.loadSettings();
	}

	@Override
	public String getDynmapDescription() {
		return null;
	}

	@Override
	protected void removeBuffs() {
	}

	@Override
	protected void addBuffs() {
	}

	@Override
	public String getMarkerIconName() {
		return "";
	}

	public void startMission(final CivAsyncTask task, final Player player) throws InterruptedException {
		if (this.getChests().size() == 0) {
			return;
		}
		CivMessage.send(player, "§5" + CivSettings.localize.localizedString("var_science_w8"));
		final MultiInventory source = new MultiInventory();
		final Collection<ConstructChest> chests = this.getChests();
		for (final ConstructChest c : chests) {
			task.syncLoadChunk(c.getCoord().getChunkCoord());
			Inventory tmp;
			try {
				tmp = task.getChestInventory(c.getCoord(), false);
			} catch (CivTaskAbortException e) {
				e.printStackTrace();
				return;
			}
			source.addInventory(tmp);
		}
		final ConfigSpaceRocket configSpaceRocket = CivSettings.spaceRocket_name.get(super.getCiv().getCurrentMission());
		final ConfigSpaceMissions configSpaceMissions = CivSettings.spacemissions_levels.get(super.getCiv().getCurrentMission());
		final HashMap<String, Integer> components = new HashMap<String, Integer>();
		final String[] split2;
		final String[] split = split2 = configSpaceRocket.components.split(":");
		for (final String component : split2) {
			final int count = Integer.parseInt(component.replaceAll("[^\\d]", ""));
			final String craftMatID = component.replace(String.valueOf(count), "");
			components.put(craftMatID, count);
		}
		boolean allMatch = true;
		final HashMap<String, Integer> multiInvContents = new HashMap<String, Integer>();
		for (final ItemStack itemStack : source.getContents()) {
			if (itemStack != null) {
				final CustomMaterial loreMaterial = CustomMaterial.getCustomMaterial(itemStack);
				if (loreMaterial != null) {
					if (multiInvContents.get(loreMaterial.getId()) == null) {
						multiInvContents.put(loreMaterial.getId(), itemStack.getAmount());
					} else {
						final int oldAmount = multiInvContents.get(loreMaterial.getId());
						multiInvContents.put(loreMaterial.getId(), itemStack.getAmount() + oldAmount);
					}
				}
			}
		}
		final StringBuilder notMatchComponents = new StringBuilder("§cCould not start the mission '" + configSpaceMissions.name + "' with Rocket "
				+ configSpaceRocket.name + ". Not enough components. Need more: " + "§a" + "\n");
		for (final String component2 : split) {
			final int count2 = Integer.parseInt(component2.replaceAll("[^\\d]", ""));
			final String craftMatID2 = component2.replace(String.valueOf(count2), "");
			final CraftableCustomMaterial itemToGetName = CraftableCustomMaterial.getCraftableCustomMaterial(craftMatID2);
			if (multiInvContents.get(craftMatID2) == null) {
				allMatch = false;
				notMatchComponents.append(itemToGetName.getName()).append(" ").append(count2).append(" ").append("piece(s)/n");
			} else
				if (multiInvContents.get(craftMatID2) < count2) {
					allMatch = false;
					final int reaming = count2 - multiInvContents.get(craftMatID2);
					notMatchComponents.append(itemToGetName.getName()).append(" ").append(reaming).append(" ").append("piece(s)/n");
				}
		}
		if (allMatch) {
			for (final ItemStack itemStack2 : source.getContents()) {
				if (itemStack2 != null) {
					task.updateInventory(UpdateInventoryRequest.Action.REMOVE, source, itemStack2);
				}
			}
			final String fullName = player.getDisplayName();
			CivMessage.sendCiv(super.getCiv(),
					"§a" + CivSettings.localize.localizedString("var_spaceshuttle_succusess", configSpaceMissions.name, configSpaceRocket.name, fullName));
			super.getCiv().setMissionActive(true);
			return;
		}
		CivMessage.send(player, notMatchComponents.toString());
	}

	public boolean lastKeySet(final int current, final HashMap<String, Integer> array) {
		int lenght = 0;
		for (final String string : array.keySet()) {
			if (string != null && string.equalsIgnoreCase("")) {
				++lenght;
			}
		}
		return current == lenght;
	}

	@Override
	public void processSignAction(final Player player, final ConstructSign sign, final PlayerInteractEvent event) throws CivException {
		final Resident resident = CivGlobal.getResident(player);
		boolean access = true;
		if (resident == null) access = false;
		if (!(sign.getOwner() instanceof Buildable)) return;

		Civilization civ = ((Buildable) sign.getOwner()).getCiv();
		Town town = ((Buildable) sign.getOwner()).getTown();
		if (!civ.getLeaderGroup().hasMember(resident)) access = false;
		if (access) {
			final String action = sign.getAction();
			switch (action) {
				case "runMission" : {
					if (civ.getMissionActive()) {
						throw new CivException(CivSettings.localize.localizedString("var_spaceshuttle_already",
								CivSettings.spacemissions_levels.get(civ.getCurrentMission()).name));
					}
					if (civ.getCurrentMission() < 8) {
						TaskMaster.asyncTask("ValidateShuttleSync", new ValidateShuttleSync(town, player), 0L);
						break;
					}
					throw new CivException("§a" + CivSettings.localize.localizedString("var_spaceshuttle_end", CivSettings.spacemissions_levels.get(7).name));
				}
				case "missionProgress" : {
					if (!civ.getMissionActive()) {
						CivMessage.sendError(player, CivSettings.localize.localizedString("var_spaceshuttle_noProgress"));
						break;
					}
					final Integer currentMission = super.getCiv().getCurrentMission();
					final String missionName = CivSettings.spacemissions_levels.get(currentMission).name;
					final String[] split = civ.getMissionProgress().split(":");
					final double completedBeakers = Math.round(Double.valueOf(split[0]));
					final double completedHammers = Math.round(Double.valueOf(split[1]));
					final int percentageCompleteBeakers = (int) (Math.round(Double.parseDouble(split[0]))
							/ Double.parseDouble(CivSettings.spacemissions_levels.get(civ.getCurrentMission()).require_beakers) * 100.0);
					final int percentageCompleteHammers = (int) (Math.round(Double.parseDouble(split[1]))
							/ Double.parseDouble(CivSettings.spacemissions_levels.get(civ.getCurrentMission()).require_hammers) * 100.0);
					CivMessage.sendSuccess((CommandSender) player,
							CivSettings.localize.localizedString("var_spaceshuttle_progress", "§c" + missionName + CivColor.RESET,
									"§b" + completedBeakers + "§c" + "(" + percentageCompleteBeakers + "%)" + CivColor.RESET,
									"§7" + completedHammers + "§c" + "(" + percentageCompleteHammers + "%)" + CivColor.RESET));
					break;
				}
			}
		} else {
			CivMessage.sendError(player, CivSettings.localize.localizedString("var_spaceshuttle_noPerm", civ.getName()));
		}
	}

	@Override
	public void commandBlockRelatives(final BlockCoord absCoord, final SimpleBlock commandBlock) {
		if (commandBlock.command.equals("/runMission")) {
			ItemManager.setTypeId(absCoord.getBlock(), commandBlock.getType());
			ItemManager.setData(absCoord.getBlock(), commandBlock.getData());
			final ConstructSign structSign = new ConstructSign(absCoord, this);
			structSign.setText(CivColor.UNDERLINE + "Run Mission");
			structSign.setDirection(commandBlock.getData());
			structSign.setAction("runMission");
			structSign.update();
			this.addConstructSign(structSign);
			CivGlobal.addConstructSign(structSign);
		} else
			if (commandBlock.command.equals("/missionProgress")) {
				ItemManager.setTypeId(absCoord.getBlock(), commandBlock.getType());
				ItemManager.setData(absCoord.getBlock(), commandBlock.getData());
				final ConstructSign structSign = new ConstructSign(absCoord, this);
				structSign.setText(CivColor.UNDERLINE + "Progress");
				structSign.setDirection(commandBlock.getData());
				structSign.setAction("missionProgress");
				structSign.update();
				this.addConstructSign(structSign);
				CivGlobal.addConstructSign(structSign);
			}
	}
}
