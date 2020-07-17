package com.avrgaming.civcraft.loreguiinventory;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.lorestorage.GuiInventory;
import com.avrgaming.civcraft.lorestorage.GuiItems;
import com.avrgaming.civcraft.util.CivColor;

public class RelationPage extends GuiInventory {

	public RelationPage(Player player, String arg) {
		super(player, arg);
		this.setRow(9);
		this.setCiv(getResident().getCiv());
		this.setTitle(CivSettings.localize.localizedString("resident_relationsGuiHeading"));
		this.addGuiItem(GuiItems.newGuiItem()//
				.setTitle(CivColor.LightGreenBold + CivSettings.localize.localizedString("resident_relationsGui_ally"))//
				.setMaterial(Material.EMERALD_BLOCK)//
				.setLore(ChatColor.RESET + CivSettings.localize.localizedString("resident_relationsGui_allyInfo"), "§6" + CivSettings.localize.localizedString("bookReborn_clickToView"))//
				.setAction("RelationGuiAllies")//
				.setActionData("civilization", getCiv().getName()));
		this.addGuiItem(GuiItems.newGuiItem()//
				.setTitle(CivColor.LightGreenBold + CivSettings.localize.localizedString("resident_relationsGui_peace"))//
				.setMaterial(Material.LAPIS_BLOCK)//
				.setLore(ChatColor.RESET + CivSettings.localize.localizedString("resident_relationsGui_peaceInfo"), "§6" + CivSettings.localize.localizedString("bookReborn_clickToView"))//
				.setAction("RelationGuiPeaces")//
				.setActionData("civilization", getCiv().getName()));
		this.addGuiItem(GuiItems.newGuiItem()//
				.setTitle(CivColor.LightGreenBold + CivSettings.localize.localizedString("resident_relationsGui_hostile"))//
				.setMaterial(Material.GOLD_BLOCK)//
				.setLore(ChatColor.RESET + CivSettings.localize.localizedString("resident_relationsGui_hostileInfo"), "§6" + CivSettings.localize.localizedString("bookReborn_clickToView"))//
				.setAction("RelationGuiHostiles")//
				.setActionData("civilization", getCiv().getName()));
		this.addGuiItem(GuiItems.newGuiItem()//
				.setTitle(CivColor.LightGreenBold + CivSettings.localize.localizedString("resident_relationsGui_war"))//
				.setMaterial(Material.REDSTONE_BLOCK)//
				.setLore(ChatColor.RESET + CivSettings.localize.localizedString("resident_relationsGui_warInfo"), "§6" + CivSettings.localize.localizedString("bookReborn_clickToView"))//
				.setAction("RelationGuiWars")//
				.setActionData("civilization", getCiv().getName()));
		saveStaticGuiInventory();
	}

}
