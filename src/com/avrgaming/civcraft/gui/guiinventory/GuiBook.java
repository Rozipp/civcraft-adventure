package com.avrgaming.civcraft.gui.guiinventory;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.gui.GuiInventory;
import com.avrgaming.civcraft.gui.GuiItems;
import com.avrgaming.civcraft.util.ItemManager;

public class GuiBook extends GuiInventory {

	public GuiBook(Player player, String arg) throws CivException {
		super(player, null, arg);
		this.setRow(3);
		this.setTitle("§a" + CivSettings.localize.localizedString("bookReborn_heading"));

		this.addGuiItem(0, GuiItems.newGuiItem()//
				.setTitle(CivSettings.localize.localizedString("bookReborn_infoMenu"))//
				.setStack(ItemManager.createItemStack(ItemManager.getMaterialId(Material.SKULL_ITEM), (short) 3, 1))//
				.setLore("§6" + CivSettings.localize.localizedString("bookReborn_clickToView"))//
				.setOpenInventory("ResidentPage", null));
		this.addGuiItem(1, GuiItems.newGuiItem()//
				.setTitle(CivSettings.localize.localizedString("bookReborn_diplomaticMenu"))//
				.setMaterial(Material.NAME_TAG)//
				.setLore("§6" + CivSettings.localize.localizedString("bookReborn_clickToView"))//
				.setOpenInventory("Relations", null));
		this.addGuiItem(2, GuiItems.newGuiItem()//
				.setTitle(CivSettings.localize.localizedString("bookReborn_civSpaceMenu"))//
				.setMaterial(Material.BLAZE_POWDER)//
				.setLore("§6" + CivSettings.localize.localizedString("bookReborn_clickToView"))//
				.setAction("BookCivSpace"));

		this.addGuiItem(4, GuiItems.newGuiItem()//
				.setTitle(CivSettings.localize.localizedString("bookReborn_craftMenu"))//
				.setMaterial(Material.WORKBENCH)//
				.setLore("§6" + CivSettings.localize.localizedString("bookReborn_clickToView"))//
				.setOpenInventory("CraftingHelp", null));
		this.addGuiItem(5, GuiItems.newGuiItem()//
				.setTitle(CivSettings.localize.localizedString("bookReborn_newsMenu"))//
				.setMaterial(Material.PAPER)//
				.setLore("§6" + CivSettings.localize.localizedString("bookReborn_clickToView"))//
				.setOpenInventory("NewsPaper", null));

		this.addGuiItem(7, GuiItems.newGuiItem()//
				.setTitle(CivSettings.localize.localizedString("bookReborn_dynmapMenu"))//
				.setMaterial(Material.LADDER)//
				.setLore("§6" + CivSettings.localize.localizedString("bookReborn_clickToView"))//
				.setAction("BookLinksAction"));
		this.addGuiItem(8, GuiItems.newGuiItem()//
				.setTitle(CivSettings.localize.localizedString("bookReborn_gameInfoMenu"))//
				.setMaterial(Material.WRITTEN_BOOK)//
				.setLore("§6" + CivSettings.localize.localizedString("bookReborn_clickToView"))//
				.setOpenInventory("Tutorial", null));
		this.addGuiItem(9, GuiItems.newGuiItem()//
				.setTitle(CivSettings.localize.localizedString("bookReborn_buildMenu"))//
				.setMaterial(Material.SLIME_BLOCK)//
				.setLore("§6" + CivSettings.localize.localizedString("bookReborn_clickToView"))//
				.setOpenInventory("Structure", "false"));

		this.addGuiItem(17, GuiItems.newGuiItem()//
				.setTitle(CivSettings.localize.localizedString("bookReborn_donateMenu"))//
				.setMaterial(Material.GOLD_INGOT)//
				.setLore("§6" + CivSettings.localize.localizedString("bookReborn_clickToView"))//
				.setAction("BookShowDonateMenu"));
		this.addGuiItem(18, GuiItems.newGuiItem()//
				.setTitle(CivSettings.localize.localizedString("bookReborn_techMenu"))//
				.setStack(ItemManager.createItemStack(ItemManager.getMaterialId(Material.POTION), (short) 8267, 1))//
				.setLore("§6" + CivSettings.localize.localizedString("bookReborn_clickToView"))//
				.setAction("BookTechsGui"));
		this.addGuiItem(19, GuiItems.newGuiItem()//
				.setTitle(CivSettings.localize.localizedString("bookReborn_upgradeMenu"))//
				.setMaterial(Material.ANVIL)//
				.setLore("§6" + CivSettings.localize.localizedString("bookReborn_clickToView"))//
				.setOpenInventory("UpgradeBuy", "true"));

		this.addGuiItem(26, GuiItems.newGuiItem()//
				.setTitle(CivSettings.localize.localizedString("bookReborn_perkMenu"))//
				.setMaterial(Material.BOOK_AND_QUILL)//
				.setLore("§6" + CivSettings.localize.localizedString("bookReborn_clickToView"))//
				.setOpenInventory("PerkPage", null));
	}

}
