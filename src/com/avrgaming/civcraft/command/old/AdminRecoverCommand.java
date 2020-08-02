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

import java.sql.SQLException;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.sync.RecoverStructuresAsyncTask;

public class AdminRecoverCommand extends CommandBase {

	@Override
	public void init() {
		command = "/ad recover";
		displayName = CivSettings.localize.localizedString("adcmd_recover_Name");

		cs.add("structures", CivSettings.localize.localizedString("adcmd_recover_structuresDesc"));
		cs.add("listbroken", CivSettings.localize.localizedString("adcmd_recover_listBrokenDesc"));

		cs.add("listorphantowns", CivSettings.localize.localizedString("adcmd_recover_listOrphanTownDesc"));
		cs.add("listorphancivs", CivSettings.localize.localizedString("adcmd_recover_listOrphanCivsDesc"));

		cs.add("listorphanleaders", CivSettings.localize.localizedString("adcmd_recover_listOrphanLeadersDesc"));
		cs.add("fixleaders", CivSettings.localize.localizedString("adcmd_recover_fixLeadersDesc"));

		cs.add("listorphanmayors", CivSettings.localize.localizedString("adcmd_recover_listOrphanMayorsDesc"));
		cs.add("fixmayors", CivSettings.localize.localizedString("admcd_recover_fixmayorsDesc"));

		cs.add("forcesaveresidents", CivSettings.localize.localizedString("adcmd_recover_forceSaveResDesc"));
		cs.add("forcesavetowns", CivSettings.localize.localizedString("adcmd_recover_forceSaveTownsDesc"));
		cs.add("forcesavecivs", CivSettings.localize.localizedString("adcmd_recover_forceSaveCivsDesc"));

		cs.add("listdefunctcivs", CivSettings.localize.localizedString("adcmd_recover_listDefunctCivsDesc"));
		cs.add("killdefunctcivs", CivSettings.localize.localizedString("admcd_recover_killDefunctCivsDesc"));

		cs.add("listdefuncttowns", CivSettings.localize.localizedString("adcmd_recover_listDefunctTownsDesc"));
		cs.add("killdefuncttowns", CivSettings.localize.localizedString("adcmd_recover_killdefunctTownsDesc"));

		cs.add("listnocaptials", CivSettings.localize.localizedString("adcmd_recover_listNoCapitolsDesc"));
		cs.add("cleannocapitols", CivSettings.localize.localizedString("adcmd_recover_cleanNoCapitolsDesc"));

	}

	public void listnocapitols_cmd() {
		CivMessage.sendHeading(sender, CivSettings.localize.localizedString("adcmd_recover_ListNoCapitolHeading"));
		for (Civilization civ : CivGlobal.getCivs()) {
			if (civ.getCapitol() == null) CivMessage.send(sender, civ.getName());
		}
	}

	public void cleannocapitols_cmd() {
		for (Civilization civ : CivGlobal.getCivs()) {
			if (civ.getCapitol() == null) {
				CivMessage.send(sender, CivSettings.localize.localizedString("Deleting") + " " + civ.getName());
				civ.delete();
			}
		}
	}

	public void listdefunctcivs_cmd() {
		CivMessage.sendHeading(sender, CivSettings.localize.localizedString("adcmd_recover_ListNoCapitolHeading"));
		for (Civilization civ : CivGlobal.getCivs()) {
			if (civ.GM.getLeaderGroup() == null) CivMessage.send(sender, civ.getName());
		}
	}

	public void killdefunctcivs_cmd() {
		for (Civilization civ : CivGlobal.getCivs()) {
			if (civ.GM.getLeaderGroup() == null) {
				CivMessage.send(sender, CivSettings.localize.localizedString("Deleting") + " " + civ.getName());
				civ.delete();
			}
		}
	}

	public void listdefuncttowns_cmd() {
		CivMessage.sendHeading(sender, CivSettings.localize.localizedString("adcmd_recover_listDefunctTownsHeading"));
		for (Town town : CivGlobal.getTowns()) {
			if (town.GM.getMayorGroup() == null) CivMessage.send(sender, town.getName());
		}
	}

	public void killdefuncttowns_cmd() {
		for (Town town : CivGlobal.getTowns()) {
			if (town.GM.getMayorGroup() == null) {
				CivMessage.send(sender, CivSettings.localize.localizedString("Deleting") + " " + town.getName());
				town.delete();
			}
		}
	}

	public void forcesaveresidents_cmd() throws SQLException {
		for (Resident resident : CivGlobal.getResidents()) {
			resident.saveNow();
		}
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_recover_forcesaveResSuccss", CivGlobal.getResidents().size()));
	}

	public void forcesavetowns_cmd() throws SQLException {
		for (Town town : CivGlobal.getTowns()) {
			town.saveNow();
		}
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_recover_forceSaveTownsSuccess", CivGlobal.getTowns().size()));
	}

	public void forcesavecivs_cmd() throws SQLException {
		for (Civilization civ : CivGlobal.getCivs()) {
			civ.saveNow();
		}
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_recover_forceSaveCivsSuccess", CivGlobal.getCivs().size()));
	}

	public void listorphanmayors_cmd() {
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

	public void fixmayors_cmd() {

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
			CivMessage.send(sender, CivSettings.localize.localizedString("Fixed") + " " + leader.getName() + " " + CivSettings.localize.localizedString("inCiv") + " " + civ.getName() + " " + CivSettings.localize.localizedString("inCapitol")
					+ " " + capitol.getName());

		}

		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("Finished"));
	}

	public void fixleaders_cmd() {
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

	public void listorphanleaders_cmd() {
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

	public void listorphantowns_cmd() {
		CivMessage.sendHeading(sender, CivSettings.localize.localizedString("adcmd_recover_listOrphanTownsHeading"));

		for (Town town : CivGlobal.orphanTowns) {
			CivMessage.send(sender, town.getName());
		}
	}

	public void listorphancivs_cmd() {
		CivMessage.sendHeading(sender, CivSettings.localize.localizedString("adcmd_recover_listOrphanCivsHeading"));

		for (Civilization civ : CivGlobal.orphanCivs) {
			CivMessage.send(sender, civ.getName() + " capitol:" + civ.getCapitol().getName());
		}

	}

	public void listbroken_cmd() {
		CivMessage.send(sender, CivSettings.localize.localizedString("adcmd_recover_listbrokenStart"));
		TaskMaster.syncTask(new RecoverStructuresAsyncTask(sender, true), 0);
	}

	public void structures_cmd() {
		CivMessage.send(sender, CivSettings.localize.localizedString("adcmd_recover_structuresStart"));
		TaskMaster.syncTask(new RecoverStructuresAsyncTask(sender, false), 0);

	}

	@Override
	public void doDefaultAction() throws CivException {
		showHelp();
	}

	@Override
	public void showHelp() {
		showBasicHelp();
	}

	@Override
	public void permissionCheck() throws CivException {
		// Permissions checked in /ad command above.
	}

}
