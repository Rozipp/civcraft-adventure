/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.command.menu;

import java.util.ArrayList;

import org.apache.commons.lang.WordUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.command.Commander;
import com.avrgaming.civcraft.command.CustomCommand;
import com.avrgaming.civcraft.command.MenuAbstractCommand;
import com.avrgaming.civcraft.command.Validators;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigTownUpgrade;
import com.avrgaming.civcraft.construct.structures.Library;
import com.avrgaming.civcraft.construct.structures.Store;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.CivColor;

public class TownUpgradeCommand extends MenuAbstractCommand {

	public TownUpgradeCommand(String perentComman) {
		super(perentComman);
		displayName = CivSettings.localize.localizedString("cmd_town_upgrade_name");
		this.setValidator(Validators.validMayorAssistantLeader);

		add(new CustomCommand("list").withDescription(CivSettings.localize.localizedString("cmd_town_upgrade_listDesc")).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_town_upgrade_listHeading"));
				if (args.length < 1) {
					CivMessage.send(sender, "- " + CivColor.Gold + CivSettings.localize.localizedString("cmd_town_upgrade_listAllHeading") + " " + CivColor.LightBlue + "(" + ConfigTownUpgrade.getAvailableCategoryCount("all", town) + ")");
					for (String category : ConfigTownUpgrade.categories.keySet()) {
						CivMessage.send(sender, "- " + CivColor.Gold + WordUtils.capitalize(category) + CivColor.LightBlue + " (" + ConfigTownUpgrade.getAvailableCategoryCount(category, town) + ")");
					}
					return;
				}
				list_upgrades(sender, args[0], town);
			}
		}));
		add(new CustomCommand("purchased").withDescription(CivSettings.localize.localizedString("cmd_town_upgrade_purchasedDesc")).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_town_upgrade_purchasedHeading"));
				String out = "";
				for (ConfigTownUpgrade upgrade : town.getUpgrades().values()) {
					out += upgrade.name + ", ";
				}
				CivMessage.send(sender, out);
			}
		}));
		add(new CustomCommand("buy").withDescription(CivSettings.localize.localizedString("cmd_town_upgrade_buyDesc")).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				if (args.length < 2) {
					list_upgrades(sender, "all", town);
					CivMessage.send(sender, CivSettings.localize.localizedString("cmd_town_upgrade_buyHeading"));
					return;
				}
				String combinedArgs = Commander.combineArgs(args);
				ConfigTownUpgrade upgrade = CivSettings.getUpgradeByNameRegex(town, combinedArgs);
				if (upgrade == null) throw new CivException(CivSettings.localize.localizedString("cmd_town_upgrade_buyInvalid") + " " + combinedArgs);
				if (town.hasUpgrade(upgrade.id)) throw new CivException(CivSettings.localize.localizedString("cmd_town_upgrade_buyOwned"));

				// TODO make upgrades take time by using hammers.
				town.purchaseUpgrade(upgrade);
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_upgrade_buySuccess", upgrade.name));
			}
		}));
		add(new CustomCommand("resetlibrary").withDescription(CivSettings.localize.localizedString("cmd_town_reset_libraryDesc")).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				Library library = (Library) town.BM.getFirstStructureById("s_library");
				if (library == null) throw new CivException(CivSettings.localize.localizedString("cmd_town_reset_libraryNone"));
				ArrayList<ConfigTownUpgrade> removeUs = new ArrayList<ConfigTownUpgrade>();
				for (ConfigTownUpgrade upgrade : town.getUpgrades().values()) {
					if (upgrade.action.contains("enable_library_enchantment")) removeUs.add(upgrade);
				}
				for (ConfigTownUpgrade upgrade : removeUs) {
					town.removeUpgrade(upgrade);
				}
				library.reset();
				town.save();
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_town_reset_librarySuccess"));
			}
		}));
		add(new CustomCommand("resetstore").withDescription(CivSettings.localize.localizedString("cmd_town_reset_storeDesc")).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				Store store = (Store) town.BM.getFirstStructureById("s_store");
				if (store == null) throw new CivException(CivSettings.localize.localizedString("cmd_town_reset_storeNone"));
				ArrayList<ConfigTownUpgrade> removeUs = new ArrayList<ConfigTownUpgrade>();
				for (ConfigTownUpgrade upgrade : town.getUpgrades().values()) {
					if (upgrade.action.contains("set_store_material")) removeUs.add(upgrade);
				}
				for (ConfigTownUpgrade upgrade : removeUs) {
					town.removeUpgrade(upgrade);
				}
				store.reset();
				town.save();
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_town_reset_storeSuccess"));
			}
		}));
	}

	private void list_upgrades(CommandSender sender, String category, Town town) throws CivException {
		if (!ConfigTownUpgrade.categories.containsKey(category.toLowerCase()) && !category.equalsIgnoreCase("all")) throw new CivException(CivSettings.localize.localizedString("var_cmd_town_upgrade_listnoCat", category));
		for (ConfigTownUpgrade upgrade : CivSettings.townUpgrades.values()) {
			if (category.equalsIgnoreCase("all") || upgrade.category.equalsIgnoreCase(category)) {
				if (upgrade.isAvailable(town)) CivMessage.send(sender, upgrade.name + " " + CivColor.LightGray + CivSettings.localize.localizedString("Cost") + " " + CivColor.Yellow + upgrade.cost);
			}
		}
	}
}
