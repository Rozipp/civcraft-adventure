
package com.avrgaming.civcraft.loregui.book;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.tutorial.Book;
import com.avrgaming.civcraft.lorestorage.GuiInventory;
import com.avrgaming.civcraft.lorestorage.GuiItemAction;
import com.avrgaming.civcraft.lorestorage.GuiItems;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;

import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.CivColor;

public class BookRelationsGui implements GuiItemAction {
	public static GuiInventory inventory = null;

	@Override
	public void performAction(InventoryClickEvent event, ItemStack stack) {
		Player player = (Player) event.getWhoClicked();
		Resident resident = CivGlobal.getResident(player);
		if (resident.getTown() == null) {
			Book.spawnGuiBook(player);
			CivMessage.send((Object) player, "§c" + CivSettings.localize.localizedString("res_gui_noTown"));
			return;
		}
		inventory = new GuiInventory(player, 1, CivSettings.localize.localizedString("resident_relationsGuiHeading"), event.getInventory().getName());
		inventory.addGuiItem(GuiItems.newGuiItem()//
				.setTitle(CivColor.LightGreenBold + CivSettings.localize.localizedString("resident_relationsGui_ally"))//
				.setMaterial(Material.EMERALD_BLOCK)//
				.setLore(ChatColor.RESET + CivSettings.localize.localizedString("resident_relationsGui_allyInfo"), //
						"§6§6" + CivSettings.localize.localizedString("bookReborn_clickToView"))//
				.setAction("RelationGuiAllies")//
				.setActionData("civilization", resident.getCiv().getName()));
		inventory.addGuiItem(GuiItems.newGuiItem()//
				.setTitle(CivColor.LightGreenBold + CivSettings.localize.localizedString("resident_relationsGui_peace"))//
				.setMaterial(Material.LAPIS_BLOCK)//
				.setLore(ChatColor.RESET + CivSettings.localize.localizedString("resident_relationsGui_peaceInfo"), //
						"§6§6" + CivSettings.localize.localizedString("bookReborn_clickToView"))//
				.setAction("RelationGuiPeaces")//
				.setActionData("civilization", resident.getCiv().getName()));
		inventory.addGuiItem(GuiItems.newGuiItem()//
				.setTitle(CivColor.LightGreenBold + CivSettings.localize.localizedString("resident_relationsGui_hostile"))//
				.setMaterial(Material.GOLD_BLOCK)//
				.setLore(ChatColor.RESET + CivSettings.localize.localizedString("resident_relationsGui_hostileInfo"), //
						"§6§6" + CivSettings.localize.localizedString("bookReborn_clickToView"))//
				.setAction("RelationGuiHostiles")//
				.setActionData("civilization", resident.getCiv().getName()));
		inventory.addGuiItem(GuiItems.newGuiItem()//
				.setTitle(CivColor.LightGreenBold + CivSettings.localize.localizedString("resident_relationsGui_war"))//
				.setMaterial(Material.REDSTONE_BLOCK)//
				.setLore(ChatColor.RESET + CivSettings.localize.localizedString("resident_relationsGui_warInfo"), //
						"§6§6" + CivSettings.localize.localizedString("bookReborn_clickToView"))//
				.setAction("RelationGuiWars")//
				.setActionData("civilization", resident.getCiv().getName()));

		inventory.addBackItem(CivSettings.localize.localizedString("bookReborn_backToDashBoard"), CivSettings.localize.localizedString("bookReborn_backToDashBoard"));
		inventory.openInventory(player);
	}
}
