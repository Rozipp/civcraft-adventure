package com.avrgaming.civcraft.gui.guiinventory;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigConstructInfo;
import com.avrgaming.civcraft.config.ConfigTownUpgrade;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.gui.GuiInventory;
import com.avrgaming.civcraft.gui.GuiItem;
import com.avrgaming.civcraft.gui.GuiItems;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.CivColor;

public class UpgradeBuy extends GuiInventory {

	public UpgradeBuy(Player player, String arg) throws CivException {
		super(player, player, arg);
		Boolean isTutorial = Boolean.parseBoolean(arg);
		if (!isTutorial) {
			if (getResident().getTown() == null)
				isTutorial = true;
			else {
				Town town = getResident().getTown();
				if (getResident().getSelectedTown() != null) {
					try {
						getResident().getSelectedTown().validateResidentSelect(getResident());
					} catch (CivException e) {
						CivMessage.send(player, CivColor.Yellow + CivSettings.localize.localizedString("var_cmd_townDeselectedInvalid", getResident().getSelectedTown().getName(), getResident().getTown().getName()));
						getResident().setSelectedTown(getResident().getTown());
						town = getResident().getTown();
					}
					town = getResident().getSelectedTown();
					this.setTown(town);
				}
				if (!town.GM.isMayorOrAssistant(getResident()) && !town.getCiv().GM.isLeader(getResident())) {
					isTutorial = true;
				}
			}
		}
		if (isTutorial) this.setPlayer(null);
		this.setTitle(CivSettings.localize.localizedString("resident_upgradesGuiHeading") + (getTown() != null ? " " + getTown().getName() : " Tutorial"));

		for (ConfigTownUpgrade upgrade : (isTutorial) ? CivSettings.townUpgrades.values() : ConfigTownUpgrade.getAllUpgrades(getTown())) {
			double cost = upgrade.cost;
			if (!isTutorial && getTown().getCiv().getGovernment().id.equalsIgnoreCase("gov_theocracy")) cost *= 0.9;
			GuiItem gi = GuiItems.newGuiItem().setTitle(upgrade.name)//
					.setLore("§b" + CivSettings.localize.localizedString("money_requ", Math.round(cost)));
			if (!isTutorial && !getTown().BM.hasStructure(upgrade.require_structure)) {
				ConfigConstructInfo structure = CivSettings.constructs.get(upgrade.require_structure);
				gi.setMaterial(Material.EMERALD)//
						.addLore(CivColor.Red + CivSettings.localize.localizedString("requ") + structure.displayName, //
								"§3" + CivSettings.localize.localizedString("clicktobuild"));
				this.addGuiItem(gi);
				continue;
			}
			if (!isTutorial && !getTown().hasUpgrade(upgrade.require_upgrade)) {
				ConfigTownUpgrade upgrade1 = CivSettings.getUpgradeById(upgrade.require_upgrade);
				gi.setMaterial(Material.GLOWSTONE_DUST)//
						.addLore(CivColor.Red + CivSettings.localize.localizedString("requ") + upgrade1.name, //
								"§3" + CivSettings.localize.localizedString("tutorial_lore_clicktoView"))//
						.setCallbackGui(upgrade1.name);
				this.addGuiItem(gi);
				continue;
			}
			gi.setMaterial(Material.EMERALD_BLOCK);
			if (!isTutorial && upgrade.isAvailable(getTown())) {
				gi.addLore("§6" + CivSettings.localize.localizedString("tutorial_lore_clicktoView"))//
						.setCallbackGui(upgrade.name);
			}
			this.addGuiItem(gi);
		}
	}

	@Override
	public void execute(String... strings) {
		GuiInventory.closeInventory(getPlayer());
		String toUpgrade = "town upgrade buy " + strings[0];
		Bukkit.dispatchCommand((CommandSender) getPlayer(), (String) toUpgrade);
	}

}
