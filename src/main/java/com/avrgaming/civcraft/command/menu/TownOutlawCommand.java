/*************************************************************************
 * 
 * AVRGAMING LLC
 * __________________
 * 
 *  [2013] AVRGAMING LLC
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of AVRGAMING LLC and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to AVRGAMING LLC
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from AVRGAMING LLC.
 */
package com.avrgaming.civcraft.command.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.command.Commander;
import com.avrgaming.civcraft.command.CustomCommand;
import com.avrgaming.civcraft.command.MenuAbstractCommand;
import com.avrgaming.civcraft.command.Validators;
import com.avrgaming.civcraft.command.taber.AbstractTaber;
import com.avrgaming.civcraft.command.taber.CivInWorldTaber;
import com.avrgaming.civcraft.command.taber.ResidentInWorldTaber;
import com.avrgaming.civcraft.command.taber.TownInWorldTaber;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.TownAddOutlawTask;
import com.avrgaming.civcraft.util.CivColor;

public class TownOutlawCommand extends MenuAbstractCommand {

	public TownOutlawCommand(String perentComman) {
		super(perentComman);
		displayName = CivSettings.localize.localizedString("cmd_town_outlaw_name");
		this.addValidator(Validators.validMayorAssistantLeader);

		add(new CustomCommand("add").withDescription(CivSettings.localize.localizedString("cmd_town_outlaw_addDesc")).withTabCompleter(new ResidentInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				if (args.length < 2) throw new CivException(CivSettings.localize.localizedString("cmd_town_outlaw_addPrompt"));
				Resident resident = Commander.getNamedResident(args, 0);
				if (resident.getTown() == town) throw new CivException(CivSettings.localize.localizedString("cmd_town_outlaw_addError"));
				try {
					Player player = CivGlobal.getPlayer(args[1]);
					CivMessage.send(player, CivColor.Yellow + ChatColor.BOLD + CivSettings.localize.localizedString("var_cmd_town_outlaw_addAllAlert1", town.getName()));
				} catch (CivException e) {}
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_outlaw_addallalert3", args[1]));
				TaskMaster.asyncTask(new TownAddOutlawTask(args[1], town), 1000);
			}
		}));
		add(new CustomCommand("remove").withDescription(CivSettings.localize.localizedString("cmd_town_outlaw_removeDesc"))//
				.withTabCompleter(new AbstractTaber() {
					@Override
					public List<String> getTabList(CommandSender sender, String arg) throws CivException {
						List<String> l = new ArrayList<>();
						Town town = Commander.getSelectedTown(sender);
						for (String s : town.outlaws)
							if (s.toLowerCase().startsWith(arg)) l.add(s);
						return l;
					}
				}).withExecutor(new CustomExecutor() {
					@Override
					public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
						Town town = Commander.getSelectedTown(sender);
						if (args.length < 2) throw new CivException(CivSettings.localize.localizedString("cmd_town_outlaw_removePrompt"));
						town.removeOutlaw(args[1]);
						town.save();
						CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_outlaw_removeSuccess", args[1]));
					}
				}));
		add(new CustomCommand("list").withDescription(CivSettings.localize.localizedString("cmd_town_outlaw_listDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_town_outlaw_listHeading"));
				String out = "";
				for (String outlaw : town.outlaws) {
					Resident res = CivGlobal.getResidentViaUUID(UUID.fromString(outlaw));
					out += res.getName() + ",";
				}
				CivMessage.send(sender, out);
			}
		}));
		add(new CustomCommand("addall").withDescription(CivSettings.localize.localizedString("cmd_town_outlaw_addallDesc")).withTabCompleter(new TownInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				Town targetTown = Commander.getNamedTown(args, 0);
				for (Resident resident : targetTown.getResidents()) {
					try {
						Player player = CivGlobal.getPlayer(args[1]);
						CivMessage.send(player, CivColor.Yellow + ChatColor.BOLD + CivSettings.localize.localizedString("var_cmd_town_outlaw_addAllAlert1", town.getName()));
					} catch (CivException e) {}
					TaskMaster.asyncTask(new TownAddOutlawTask(resident.getName(), town), 1000);
				}
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_outlaw_addallalert3", args[1]));
			}
		}));
		add(new CustomCommand("removeall").withDescription(CivSettings.localize.localizedString("cmd_town_outlaw_removeallDesc")).withTabCompleter(new TownInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				Town targetTown = Commander.getNamedTown(args, 0);
				for (Resident resident : targetTown.getResidents()) {
					town.removeOutlaw(resident.getName());
				}
				town.save();
			}
		}));
		add(new CustomCommand("addallciv").withDescription(CivSettings.localize.localizedString("cmd_town_outlaw_addallcivDesc")).withTabCompleter(new CivInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				Civilization targetCiv = Commander.getNamedCiv(args, 0);
				for (Town targetTown : targetCiv.getTowns()) {
					for (Resident resident : targetTown.getResidents()) {
						try {
							Player player = CivGlobal.getPlayer(args[1]);
							CivMessage.send(player, CivColor.Yellow + ChatColor.BOLD + CivSettings.localize.localizedString("var_cmd_town_outlaw_addAllAlert1", town.getName()));
						} catch (CivException e) {}
						TaskMaster.asyncTask(new TownAddOutlawTask(resident.getName(), town), 1000);
					}
				}
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_outlaw_addallalert3", args[1]));
			}
		}));
		add(new CustomCommand("removeallciv").withDescription(CivSettings.localize.localizedString("cmd_town_outlaw_removeallcivDesc")).withTabCompleter(new CivInWorldTaber()).withExecutor(new CustomExecutor() {

			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				Civilization targetCiv = Commander.getNamedCiv(args, 0);
				for (Town targetTown : targetCiv.getTowns()) {
					for (Resident resident : targetTown.getResidents()) {
						town.removeOutlaw(resident.getName());
					}
				}
				town.save();
			}
		}));
	}
}
