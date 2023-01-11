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
package com.avrgaming.civcraft.command.admin;

import java.sql.SQLException;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.command.CustomCommand;
import com.avrgaming.civcraft.command.MenuAbstractCommand;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.sync.RecoverStructuresAsyncTask;

public class AdminRecoverCommand extends MenuAbstractCommand {

	public AdminRecoverCommand(String perentCommand) {
		super(perentCommand);
		displayName = CivSettings.localize.localizedString("adcmd_recover_Name");

		add(new CustomCommand("structures").withDescription(CivSettings.localize.localizedString("adcmd_recover_structuresDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				CivMessage.send(sender, CivSettings.localize.localizedString("adcmd_recover_structuresStart"));
				TaskMaster.syncTask(new RecoverStructuresAsyncTask(sender, false), 0);
			}
		}));
		add(new CustomCommand("listbroken").withDescription(CivSettings.localize.localizedString("adcmd_recover_listBrokenDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				CivMessage.send(sender, CivSettings.localize.localizedString("adcmd_recover_listbrokenStart"));
				TaskMaster.syncTask(new RecoverStructuresAsyncTask(sender, true), 0);
			}
		}));
		add(new CustomCommand("listorphantowns").withDescription(CivSettings.localize.localizedString("adcmd_recover_listOrphanTownDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				CivMessage.sendHeading(sender, CivSettings.localize.localizedString("adcmd_recover_listOrphanTownsHeading"));

				for (Town town : CivGlobal.orphanTowns) {
					CivMessage.send(sender, town.getName());
				}
			}
		}));
		add(new CustomCommand("listorphancivs").withDescription(CivSettings.localize.localizedString("adcmd_recover_listOrphanCivsDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				CivMessage.sendHeading(sender, CivSettings.localize.localizedString("adcmd_recover_listOrphanCivsHeading"));

				for (Civilization civ : CivGlobal.orphanCivs) {
					CivMessage.send(sender, civ.getName() + " capitol:" + civ.getCapitol().getName());
				}
			}
		}));
		add(new CustomCommand("listorphanleaders").withDescription(CivSettings.localize.localizedString("adcmd_recover_listOrphanLeadersDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				CivMessage.sendHeading(sender, CivSettings.localize.localizedString("adcmd_recover_listOrphanLeadersHeading"));

				for (Civilization civ : CivGlobal.getCivs()) {
					Resident res = civ.GM.getLeader();
					if (res == null) continue;

					if (!res.hasTown()) {
						if (civ.getCapitol() == null) {
							CivMessage.send(sender, CivSettings.localize.localizedString("adcmd_recover_fixLeadersNoCap") + " " + civ.getName());
							continue;
						}
						CivMessage.send(sender, CivSettings.localize.localizedString("adcmd_recover_listOrphanLeadersBroken") + civ.getName() + " " + CivSettings.localize.localizedString("Leader") + " " + res.getName());
					}
				}
			}
		}));
		add(new CustomCommand("fixleaders").withDescription(CivSettings.localize.localizedString("adcmd_recover_fixLeadersDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				for (Civilization civ : CivGlobal.getCivs()) {
					Resident res = civ.GM.getLeader();
					if (res == null) continue;

					if (!res.hasTown()) {
						Town capitol = civ.getCapitol();
						if (capitol == null) {
							CivMessage.send(sender, CivSettings.localize.localizedString("adcmd_recover_fixLeadersNoCap") + " " + civ.getName());
							continue;
						}
						res.setTown(capitol);
						try {
							res.saveNow();
						} catch (SQLException e) {
							e.printStackTrace();
						}
						CivMessage.send(sender, CivSettings.localize.localizedString("adcmd_recover_FixLeaders1") + " " + civ.getName() + " " + CivSettings.localize.localizedString("Leader") + " " + res.getName());
					}

					if (!civ.GM.isLeader(res)) {
						try {
							civ.GM.addLeader(res);
						} catch (CivException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}));
		add(new CustomCommand("listorphanmayors").withDescription(CivSettings.localize.localizedString("adcmd_recover_listOrphanMayorsDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				for (Civilization civ : CivGlobal.getCivs()) {
					Town capitol = civ.getCapitol();
					if (capitol == null) continue;

					Resident leader = civ.GM.getLeader();
					if (leader == null) continue;

					CivMessage.send(sender, CivSettings.localize.localizedString("Broken") + " " + leader.getName() + " " + CivSettings.localize.localizedString("inCiv") + " " + civ.getName() + " "
							+ CivSettings.localize.localizedString("inCapitol") + " " + capitol.getName());
				}
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("Finished"));
			}
		}));
		add(new CustomCommand("fixmayors").withDescription(CivSettings.localize.localizedString("admcd_recover_fixmayorsDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				for (Civilization civ : CivGlobal.getCivs()) {
					Town capitol = civ.getCapitol();
					if (capitol == null) continue;

					Resident leader = civ.GM.getLeader();
					if (leader == null) continue;

					if (capitol.GM.getMayorGroup() == null) {
						CivMessage.send(sender, CivSettings.localize.localizedString("var_adcmd_recover_fixMayorsError", capitol.getName()));
						continue;
					}

					try {
						capitol.GM.addMayor(leader);
					} catch (CivException e) {
						e.printStackTrace();
					}
					CivMessage.send(sender, CivSettings.localize.localizedString("Fixed") + " " + leader.getName() + " " + CivSettings.localize.localizedString("inCiv") + " " + civ.getName() + " "
							+ CivSettings.localize.localizedString("inCapitol") + " " + capitol.getName());

				}

				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("Finished"));
			}
		}));
		add(new CustomCommand("forcesaveresidents").withDescription(CivSettings.localize.localizedString("adcmd_recover_forceSaveResDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				for (Resident resident : CivGlobal.getResidents()) {
					try {
						resident.saveNow();
					} catch (SQLException e) {
						CivLog.error("Can not save resident " + resident.getName() + ": " + e.getMessage());
					}
				}
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_recover_forcesaveResSuccss", CivGlobal.getResidents().size()));
			}
		}));
		add(new CustomCommand("forcesavetowns").withDescription(CivSettings.localize.localizedString("adcmd_recover_forceSaveTownsDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				for (Town town : CivGlobal.getTowns()) {
					try {
						town.saveNow();
					} catch (SQLException e) {
						CivLog.error("Can not save town " + town.getName() + ": " + e.getMessage());
					}
				}
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_recover_forceSaveTownsSuccess", CivGlobal.getTowns().size()));
			}
		}));
		add(new CustomCommand("forcesavecivs").withDescription(CivSettings.localize.localizedString("adcmd_recover_forceSaveCivsDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				for (Civilization civ : CivGlobal.getCivs()) {
					try {
						civ.saveNow();
					} catch (SQLException e) {
						CivLog.error("Can not save civ " + civ.getName() + ": " + e.getMessage());
					}
				}
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_recover_forceSaveCivsSuccess", CivGlobal.getCivs().size()));
			}
		}));
		add(new CustomCommand("listdefunctcivs").withDescription(CivSettings.localize.localizedString("adcmd_recover_listDefunctCivsDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				CivMessage.sendHeading(sender, CivSettings.localize.localizedString("adcmd_recover_ListNoCapitolHeading"));
				for (Civilization civ : CivGlobal.getCivs()) {
					if (civ.GM.getLeaderGroup() == null) CivMessage.send(sender, civ.getName());
				}
			}
		}));
		add(new CustomCommand("killdefunctcivs").withDescription(CivSettings.localize.localizedString("admcd_recover_killDefunctCivsDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				for (Civilization civ : CivGlobal.getCivs()) {
					if (civ.GM.getLeaderGroup() == null) {
						CivMessage.send(sender, CivSettings.localize.localizedString("Deleting") + " " + civ.getName());
						civ.delete();
					}
				}
			}
		}));
		add(new CustomCommand("listdefuncttowns").withDescription(CivSettings.localize.localizedString("adcmd_recover_listDefunctTownsDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				CivMessage.sendHeading(sender, CivSettings.localize.localizedString("adcmd_recover_listDefunctTownsHeading"));
				for (Town town : CivGlobal.getTowns()) {
					if (town.GM.getMayorGroup() == null) CivMessage.send(sender, town.getName());
				}
			}
		}));
		add(new CustomCommand("killdefuncttowns").withDescription(CivSettings.localize.localizedString("adcmd_recover_killdefunctTownsDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				for (Town town : CivGlobal.getTowns()) {
					if (town.GM.getMayorGroup() == null) {
						CivMessage.send(sender, CivSettings.localize.localizedString("Deleting") + " " + town.getName());
						town.delete();
					}
				}
			}
		}));
		add(new CustomCommand("listnocaptials").withDescription(CivSettings.localize.localizedString("adcmd_recover_listNoCapitolsDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				CivMessage.sendHeading(sender, CivSettings.localize.localizedString("adcmd_recover_ListNoCapitolHeading"));
				for (Civilization civ : CivGlobal.getCivs()) {
					if (civ.getCapitol() == null) CivMessage.send(sender, civ.getName());
				}
			}
		}));
		add(new CustomCommand("cleannocapitols").withDescription(CivSettings.localize.localizedString("adcmd_recover_cleanNoCapitolsDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				for (Civilization civ : CivGlobal.getCivs()) {
					if (civ.getCapitol() == null) {
						CivMessage.send(sender, CivSettings.localize.localizedString("Deleting") + " " + civ.getName());
						civ.delete();
					}
				}
			}
		}));
	}

}
