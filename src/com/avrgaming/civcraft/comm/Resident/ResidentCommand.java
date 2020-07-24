/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.comm.Resident;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.comm.CustonCommand;
import com.avrgaming.civcraft.comm.MenuCustomCommand;
import com.avrgaming.civcraft.comm.CustonExecutor;
import com.avrgaming.civcraft.comm.Commander;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.items.CraftableCustomMaterial;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.CivColor;

public class ResidentCommand extends MenuCustomCommand {

	public ResidentCommand() {
		super("resident");
		this.setAliases("res");
		this.setDescription("description " + CivSettings.localize.localizedString("cmd_res_Name"));
		displayName = CivSettings.localize.localizedString("cmd_res_Name");

		// add("info", new Resident_info(), new String[] { "i" }, new ArgType[] {}, CivSettings.localize.localizedString("cmd_res_infoDesc"));
		// add("toggle", new Resident_toggle(), new String[] {}, new ArgType[] {}, CivSettings.localize.localizedString("cmd_res_toggleDesc"));
		// add("show", new Resident_show(), new String[] {}, new ArgType[] { ArgType.Resident },
		// CivSettings.localize.localizedString("cmd_res_showDesc"));
		// add("resetspawn", new Resident_resetspawn(), new String[] {}, new ArgType[] {},
		// CivSettings.localize.localizedString("cmd_res_resetspawnDesc"));
		add(new CustonCommand("book")//
				.withAliases("b")//
				.withDescription(CivSettings.localize.localizedString("cmd_res_bookDesc"))//
				.withExecutor(new CustonExecutor() {
					@Override
					public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
						Player player = Commander.getPlayer(sender);
						
						/* Determine if he already has the book. */
						for (ItemStack stack : player.getInventory().getContents()) {
							if (stack == null) continue;
							CraftableCustomMaterial craftMat = CraftableCustomMaterial.getCraftableCustomMaterial(stack);
							if (craftMat == null) continue;
							if (craftMat.getConfigId().equals("mat_tutorial_book")) {
								throw new CivException(CivSettings.localize.localizedString("cmd_res_bookHaveOne"));
							}
						}

						CraftableCustomMaterial craftMat = CraftableCustomMaterial.getCraftableCustomMaterial("mat_tutorial_book");
						ItemStack helpBook = CraftableCustomMaterial.spawn(craftMat);

						HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(helpBook);
						if (leftovers != null && leftovers.size() >= 1) {
							throw new CivException(CivSettings.localize.localizedString("cmd_res_bookInvenFull"));
						}
						CivMessage.sendSuccess(player, CivSettings.localize.localizedString("cmd_res_bookSuccess"));
						return false;
					}
				}));

		// add("timezone", new Resident_timezone(), new String[] {}, new ArgType[] {},
		// CivSettings.localize.localizedString("cmd_res_timezoneDesc"));
		// add("pvptimer", new Resident_pvptimer(), new String[] {}, new ArgType[] {},
		// CivSettings.localize.localizedString("cmd_res_pvptimerDesc"));
		// add("outlawed", new Resident_outlawed(), new String[] {}, new ArgType[] {},
		// CivSettings.localize.localizedString("cmd_res_outlawedDesc"));
	}

	public static void show(CommandSender sender, Resident resident) {
		CivMessage.sendHeading(sender, CivSettings.localize.localizedString("var_Resident", resident.getName()));
		Date lastOnline = new Date(resident.getLastOnline());
		SimpleDateFormat sdf = CivGlobal.dateFormat;
		CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("cmd_res_showLastOnline") + " " + CivColor.LightGreen + sdf.format(lastOnline));
		CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("Town") + " " + CivColor.LightGreen + (resident.getTown() == null ? "none" : resident.getTown().getName()));
		CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("Camp") + " " + CivColor.LightGreen + (resident.getCamp() == null ? "none" : resident.getCamp().getName()));

		if (sender.getName().equalsIgnoreCase(resident.getName()) || sender.isOp()) {
			CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("cmd_res_showTreasure") + " " + CivColor.LightGreen + resident.getTreasury().getBalance() + " " + CivColor.Green);
			if (resident.hasTown()) {
				if (resident.getSelectedTown() != null) {
					CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("cmd_res_showSelected") + " " + CivColor.LightGreen + resident.getSelectedTown().getName());
				} else {
					CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("cmd_res_showSelected") + " " + CivColor.LightGreen + resident.getTown().getName());
				}
			}
		}

		CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("Groups") + " " + resident.getGroupsString());
	}

	@Override
	public void doDefaultAction(CommandSender sender) throws CivException {
		showHelp(sender);
	}

	@Override
	public void permissionCheck(CommandSender sender) throws CivException {

	}

}
