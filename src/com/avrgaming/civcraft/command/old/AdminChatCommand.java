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
package com.avrgaming.civcraft.command.old;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.command.Commander;
import com.avrgaming.civcraft.command.CustomCommand;
import com.avrgaming.civcraft.command.MenuAbstractCommand;
import com.avrgaming.civcraft.command.taber.CivInWorldTaber;
import com.avrgaming.civcraft.command.taber.TownInWorldTaber;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;

public class AdminChatCommand extends MenuAbstractCommand {

	public AdminChatCommand(String perentCommand) {
		super(perentCommand);
		// command = "/ad chat";
		displayName = CivSettings.localize.localizedString("adcmd_chat_name");
		add(new CustomCommand("tc").withDescription(CivSettings.localize.localizedString("adcmd_chat_tcDesc")).withTabCompleter(new TownInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Resident resident = Commander.getResident(sender);
				if (args.length < 1) {
					resident.setTownChat(false);
					resident.setTownChatOverride(null);
					CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_chat_noLongerChattingInTown"));
					return;
				}
				Town town = Commander.getNamedTown(args, 0);
				resident.setTownChat(true);
				resident.setTownChatOverride(town);
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_chat_nowChattingInTown") + " " + town.getName());
			}
		}));
		add(new CustomCommand("cc").withDescription(CivSettings.localize.localizedString("adcmd_chat_ccDesc")).withTabCompleter(new CivInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Resident resident = Commander.getResident(sender);
				if (args.length < 1) {
					resident.setCivChat(false);
					resident.setCivChatOverride(null);
					CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_chat_noLongerChattingInCiv"));
					return;
				}
				Civilization civ = Commander.getNamedCiv(args, 0);
				resident.setCivChat(true);
				resident.setCivChatOverride(civ);
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_chat_nowChattingInCiv") + " " + civ.getName());
			}
		}));
		add(new CustomCommand("cclisten").withDescription(CivSettings.localize.localizedString("adcmd_chat_cclisten")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Resident resident = Commander.getResident(sender);
				for (Civilization civ : CivGlobal.getCivs()) {
					CivMessage.addExtraCivChatListener(civ, resident);
				}
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_chat_cclistenAllSuccess"));
			}
		}));
		add(new CustomCommand("tclisten").withDescription(CivSettings.localize.localizedString("adcmd_chat_tclisten")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Resident resident = Commander.getResident(sender);
				for (Town t : CivGlobal.getTowns()) {
					CivMessage.addExtraTownChatListener(t, resident);
				}
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_chat_tclistenAllSuccess"));
			}
		}));
		add(new CustomCommand("listenoff").withDescription(CivSettings.localize.localizedString("adcmd_chat_listenOffDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Resident resident = Commander.getResident(sender);
				for (Town t : CivGlobal.getTowns()) {
					CivMessage.removeExtraTownChatListener(t, resident);
				}
				for (Civilization civ : CivGlobal.getCivs()) {
					CivMessage.removeExtraCivChatListener(civ, resident);
				}
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_chat_listenOffSuccess"));
			}
		}));
		add(new CustomCommand("cclistenall").withDescription(CivSettings.localize.localizedString("adcmd_chat_listenAllDesc")).withTabCompleter(new CivInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Resident resident = Commander.getResident(sender);
				Civilization civ = Commander.getNamedCiv(args, 0);
				for (Resident res : CivMessage.getExtraCivChatListeners(civ)) {
					if (res.equals(resident)) {
						CivMessage.removeExtraCivChatListener(civ, res);
						CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_chat_noLongerListenCiv") + " " + civ.getName());
						return;
					}
				}
				CivMessage.addExtraCivChatListener(civ, resident);
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_chat_listenCivSuccess") + " " + civ.getName());
			}
		}));
		add(new CustomCommand("tclistenall").withDescription(CivSettings.localize.localizedString("adcmd_chat_tclistenAllDesc")).withTabCompleter(new TownInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Resident resident = Commander.getResident(sender);
				Town town = Commander.getNamedTown(args, 0);
				for (Resident res : CivMessage.getExtraTownChatListeners(town)) {
					if (res.equals(resident)) {
						CivMessage.removeExtraTownChatListener(town, res);
						CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_chat_noLongerListenTown") + " " + town.getName());
						return;
					}
				}
				CivMessage.addExtraTownChatListener(town, resident);
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_chat_listenTownSuccess") + " " + town.getName());
			}
		}));
		add(new CustomCommand("banwordon").withDescription(CivSettings.localize.localizedString("adcmd_chat_banWordOnDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				CivGlobal.banWordsActive = true;
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_chat_banwordsActivated"));
			}
		}));
		add(new CustomCommand("banwordoff").withDescription(CivSettings.localize.localizedString("adcmd_chat_banWordOffDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				CivGlobal.banWordsActive = false;
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_chat_banwordsDeactivated"));
			}
		}));
		add(new CustomCommand("banwordadd").withDescription(CivSettings.localize.localizedString("admcd_chat_banwordaddDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				if (args.length < 1) throw new CivException(CivSettings.localize.localizedString("adcmd_chat_addBanwordPrompt"));
				CivGlobal.banWords.add(args[0]);
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_chat_banwordadded", args[1]));
			}
		}));
		add(new CustomCommand("banwordremove").withDescription(CivSettings.localize.localizedString("adcmd_chat_banwordremoveDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				if (args.length < 1) throw new CivException(CivSettings.localize.localizedString("adcmd_chat_removeBanwordPrompt"));
				CivGlobal.banWords.remove(args[0]);
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_chat_banwordremoved", args[1]));
			}
		}));
		add(new CustomCommand("banwordtoggle").withDescription(CivSettings.localize.localizedString("adcmd_chat_banwordToggleDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				CivGlobal.banWordsAlways = !CivGlobal.banWordsAlways;
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("admcd_chat_banwordAlways") + " " + CivGlobal.banWordsAlways);
			}
		}));
	}

}
