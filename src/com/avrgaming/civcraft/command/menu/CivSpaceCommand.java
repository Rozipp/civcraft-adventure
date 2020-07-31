
package com.avrgaming.civcraft.command.menu;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.Inventory;

import com.avrgaming.civcraft.command.Commander;
import com.avrgaming.civcraft.command.CustomCommand;
import com.avrgaming.civcraft.command.MenuAbstractCommand;
import com.avrgaming.civcraft.command.Validators;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigSpaceMissions;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.util.CivColor;

public class CivSpaceCommand extends MenuAbstractCommand {
	public static Inventory guiInventory;

	public CivSpaceCommand() {
		super("space");
		this.displayName = CivSettings.localize.localizedString("cmd_civ_space_name");
		this.setValidator(Validators.validLeader);
		add(new CustomCommand("gui").withDescription(CivSettings.localize.localizedString("cmd_civ_space_guiDesc")).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {

			}
		}));
		add(new CustomCommand("complete").withDescription(CivSettings.localize.localizedString("cmd_civ_space_succusessDesc")).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Civilization civ = Commander.getSenderCiv(sender);
				int ended = civ.getCurrentMission();
				CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_space_completed"));
				StringBuilder endedMissions = new StringBuilder();
				for (int i = 1; i < ended; ++i) {
					ConfigSpaceMissions configSpaceMissions = CivSettings.spacemissions_levels.get(i);
					endedMissions.append(configSpaceMissions.name).append("\n");
				}
				CivMessage.sendSuccess(sender, endedMissions.toString());
			}
		}));
		add(new CustomCommand("future").withDescription(CivSettings.localize.localizedString("cmd_civ_space_futureDesc")).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Civilization civ = Commander.getSenderCiv(sender);
				if (civ.getCurrentMission() >= 8) throw new CivException(CivSettings.localize.localizedString("var_spaceshuttle_end", CivSettings.spacemissions_levels.get((Object) Integer.valueOf((int) 7)).name));
				int current = civ.getCurrentMission();
				StringBuilder futureMissions = new StringBuilder(CivSettings.localize.localizedString("cmd_space_future") + ": \n" + CivColor.LightPurple);
				if (current == 7 && civ.getMissionActive()) throw new CivException(CivSettings.localize.localizedString("var_spaceshuttle_end", CivSettings.spacemissions_levels.get((Object) Integer.valueOf((int) 7)).name));
				if (civ.getMissionActive()) ++current;
				for (int i = current; i <= 7; ++i) {
					ConfigSpaceMissions configSpaceMissions = CivSettings.spacemissions_levels.get(i);
					futureMissions.append(configSpaceMissions.name).append("\n");
				}
				CivMessage.sendSuccess(sender, futureMissions.toString());
			}
		}));
		add(new CustomCommand("progress").withAliases("calc").withDescription(CivSettings.localize.localizedString("cmd_civ_space_progressDesc")).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Civilization civ = Commander.getSenderCiv(sender);
				if (!civ.getMissionActive()) throw new CivException(CivSettings.localize.localizedString("var_spaceshuttle_noProgress"));
				String[] split = civ.getMissionProgress().split(":");
				String missionName = CivSettings.spacemissions_levels.get((Object) Integer.valueOf((int) civ.getCurrentMission())).name;
				double beakers = Math.round(Double.parseDouble(split[0]));
				double hammers = Math.round(Double.parseDouble(split[1]));
				int percentageCompleteBeakers = (int) ((double) Math.round(Double.parseDouble(split[0])) / Double.parseDouble(CivSettings.spacemissions_levels.get((Object) Integer.valueOf((int) civ.getCurrentMission())).require_beakers)
						* 100.0);
				int percentageCompleteHammers = (int) ((double) Math.round(Double.parseDouble(split[1])) / Double.parseDouble(CivSettings.spacemissions_levels.get((Object) Integer.valueOf((int) civ.getCurrentMission())).require_hammers)
						* 100.0);
				String message = CivColor.LightBlue + missionName + ":" + CivColor.RESET + "\n" + CivColor.Gold + "Beakers: " + beakers + CivColor.Red + " (" + percentageCompleteBeakers + "%)" + CivColor.LightPurple + "Hammers: " + hammers
						+ CivColor.Red + " (" + percentageCompleteHammers + "%)";
				CivMessage.sendSuccess(sender, message);
			}
		}));
	}

	@Override
	public void doDefaultAction(CommandSender sender) throws CivException {
		this.showBasicHelp(sender);
	}
}
