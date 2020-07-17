package com.avrgaming.civcraft.loreguiinventory;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.config.ConfigTownUpgrade;
import com.avrgaming.civcraft.lorestorage.GuiInventory;
import com.avrgaming.civcraft.lorestorage.GuiItem;
import com.avrgaming.civcraft.lorestorage.GuiItems;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.CivColor;

public class UpgradeBuy extends GuiInventory {

	public UpgradeBuy(Player player, String arg) {
		super(player, arg);
		Boolean isTutorial = Boolean.parseBoolean(arg);
		if (!isTutorial) {
			Town town = getResident().getSelectedTown();
			if (town == null) town = getResident().getTown();
			if (town == null) {
				CivMessage.send((Object) player, "§c" + CivSettings.localize.localizedString("res_gui_noTown"));
				return;
			}
			this.setTown(town);

			if (!(town.GM.isMayorOrAssistant(getResident()) || getTown().getCiv().GM.isLeader(getResident()))) isTutorial = true;
		}
		this.setTitle(CivSettings.localize.localizedString("resident_upgradesGuiHeading"));

		for (ConfigTownUpgrade upgrade : (isTutorial) ? CivSettings.townUpgrades.values() : ConfigTownUpgrade.getAllUpgrades(getTown())) {
			double cost = upgrade.cost;
			if (!isTutorial && getTown().getCiv().getGovernment().id.equalsIgnoreCase("gov_theocracy")) cost *= 0.9;
			if (!isTutorial && !getTown().BM.hasStructure(upgrade.require_structure)) {
				ConfigBuildableInfo structure = CivSettings.structures.get(upgrade.require_structure);
				this.addGuiItem(GuiItems.newGuiItem()//
						.setTitle(upgrade.name)//
						.setMaterial(Material.EMERALD)//
						.setLore("§b" + CivSettings.localize.localizedString("money_requ", Math.round(cost)), //
								CivColor.Red + CivSettings.localize.localizedString("requ") + structure.displayName, //
								"§3" + CivSettings.localize.localizedString("clicktobuild")));//
				continue;
			}
			if (!isTutorial && !getTown().hasUpgrade(upgrade.require_upgrade)) {
				ConfigTownUpgrade upgrade1 = CivSettings.getUpgradeById(upgrade.require_upgrade);
				this.addGuiItem(GuiItems.newGuiItem()//
						.setTitle(upgrade.name)//
						.setMaterial(Material.GLOWSTONE_DUST)//
						.setLore("§b" + CivSettings.localize.localizedString("money_requ", Math.round(cost)), //
								CivColor.Red + CivSettings.localize.localizedString("requ") + upgrade1.name, //
								"§3" + CivSettings.localize.localizedString("tutorial_lore_clicktoView"))//
						.setCallbackGui(upgrade1.name));
				continue;
			}
			GuiItem gi = GuiItems.newGuiItem()//
					.setTitle(upgrade.name)//
					.setMaterial(Material.EMERALD_BLOCK)//
					.setLore("§b" + CivSettings.localize.localizedString("money_requ", Math.round(cost)));//
			if (!isTutorial && upgrade.isAvailable(getTown())) {
				gi.addLore("§6" + CivSettings.localize.localizedString("tutorial_lore_clicktoView"))//
						.setCallbackGui(upgrade.name);
			}
			this.addGuiItem(gi);
		}
		this.addLastItem(CivSettings.localize.localizedString("bookReborn_backToDashBoard"));
	}

	@Override
	public void execute(String... strings) {
		String toUpgrade = "town upgrade buy " + strings[0];
		Bukkit.dispatchCommand((CommandSender) getPlayer(), (String) toUpgrade);
	}

}
