package ua.rozipp.gui.guiinventory;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import ua.rozipp.gui.GuiInventory;
import ua.rozipp.gui.GuiItem;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.util.CivColor;

public class Tutorial extends GuiInventory {

	public Tutorial(Player player, String arg) throws CivException {
		super(player, null, arg);
		this.setRow(3);
		this.setTitle(CivSettings.localize.localizedString("tutorial_gui_heading"));

		this.addGuiItem(0, GuiItem.newGuiItem()//
				.setMaterial(Material.WORKBENCH)//
				.setTitle(CivColor.LightBlue + ChatColor.BOLD + CivSettings.localize.localizedString("tutorial_workbench_heading"))//
				.setLore(ChatColor.RESET + CivSettings.localize.localizedString("tutorial_workbench_Line1"), //
						ChatColor.RESET + CivSettings.localize.localizedString("tutorial_workbench_Line2"), //
						ChatColor.RESET + CivSettings.localize.localizedString("tutorial_workbench_Line3"), //
						ChatColor.RESET + CivSettings.localize.localizedString("tutorial_workbench_Line4"), //
						ChatColor.RESET + CivSettings.localize.localizedString("tutorial_workbench_Line5"), //
						ChatColor.RESET + CivColor.LightGreen + CivSettings.localize.localizedString("tutorial_workbench_Line6")));

		this.addGuiItem(0, GuiItem.newGuiItem()//
				.setMaterial(Material.COMPASS)//
				.setTitle(CivColor.LightBlue + ChatColor.BOLD + CivSettings.localize.localizedString("tutorial_compass_heading"))//
				.setLore(ChatColor.RESET + CivSettings.localize.localizedString("tutorial_compass_Line1"), //
						ChatColor.RESET + CivSettings.localize.localizedString("tutorial_compass_Line2"), //
						ChatColor.RESET + CivSettings.localize.localizedString("tutorial_compass_Line3"), //
						ChatColor.RESET + CivSettings.localize.localizedString("tutorial_compass_Line4"), //
						ChatColor.RESET + CivSettings.localize.localizedString("tutorial_compass_Line5")));

		this.addGuiItem(0, GuiItem.newGuiItem()//
				.setMaterial(Material.DIAMOND_ORE)//
				.setTitle(CivColor.LightBlue + ChatColor.BOLD + CivSettings.localize.localizedString("tutorial_diamondOre_heading"))//
				.setLore(ChatColor.RESET + CivSettings.localize.localizedString("tutorial_diamondOre_Line1"), //
						ChatColor.RESET + CivSettings.localize.localizedString("tutorial_diamondOre_Line2"), //
						ChatColor.RESET + CivSettings.localize.localizedString("tutorial_diamondOre_Line3"), //
						ChatColor.RESET + CivSettings.localize.localizedString("tutorial_diamondOre_Line4"), //
						ChatColor.RESET + CivSettings.localize.localizedString("var_tutorial_diamondOre_Line5", CivSettings.CURRENCY_NAME), //
						ChatColor.RESET + CivSettings.localize.localizedString("var_tutorial_diamondOre_Line6", CivSettings.CURRENCY_NAME)));

		this.addGuiItem(0, GuiItem.newGuiItem()//
				.setMaterial(Material.FENCE)//
				.setTitle(CivColor.LightBlue + ChatColor.BOLD + CivSettings.localize.localizedString("tutorial_Fence_heading"))//
				.setLore(ChatColor.RESET + CivSettings.localize.localizedString("tutorial_Fence_Line1"), //
						ChatColor.RESET + CivSettings.localize.localizedString("tutorial_Fence_Line2"), //
						ChatColor.RESET + CivSettings.localize.localizedString("tutorial_Fence_Line3"), //
						ChatColor.RESET + CivSettings.localize.localizedString("var_tutorial_Fence_Line4", CivSettings.CURRENCY_NAME), //
						ChatColor.RESET + CivSettings.localize.localizedString("tutorial_Fence_Line5"), //
						ChatColor.RESET + CivSettings.localize.localizedString("tutorial_Fence_Line6"), //
						ChatColor.RESET + CivSettings.localize.localizedString("tutorial_Fence_Line7")));

		this.addGuiItem(0, GuiItem.newGuiItem()//
				.setMaterial(Material.GOLD_HELMET)//
				.setTitle(CivColor.LightBlue + ChatColor.BOLD + CivSettings.localize.localizedString("tutorial_goldHelmet_heading"))//
				.setLore(ChatColor.RESET + CivSettings.localize.localizedString("tutorial_goldHelmet_Line1"), //
						ChatColor.RESET + CivSettings.localize.localizedString("tutorial_goldHelmet_Line2"), //
						ChatColor.RESET + CivSettings.localize.localizedString("tutorial_goldHelmet_Line3"), //
						ChatColor.RESET + CivSettings.localize.localizedString("tutorial_goldHelmet_Line4"), //
						ChatColor.RESET + CivSettings.localize.localizedString("tutorial_goldHelmet_Line5"), //
						ChatColor.RESET + CivSettings.localize.localizedString("tutorial_goldHelmet_Line6"), //
						ChatColor.RESET + CivSettings.localize.localizedString("tutorial_goldHelmet_Line7")));

		if (CivGlobal.isCasualMode()) {
			this.addGuiItem(0, GuiItem.newGuiItem()//
					.setMaterial(Material.FIREWORK)//
					.setTitle(CivColor.LightBlue + ChatColor.BOLD + CivSettings.localize.localizedString("tutorial_firework_heading"))//
					.setLore(ChatColor.RESET + CivSettings.localize.localizedString("tutorial_firework_Line1"), //
							ChatColor.RESET + CivSettings.localize.localizedString("tutorial_firework_Line2"), //
							ChatColor.RESET + CivSettings.localize.localizedString("tutorial_firework_Line3"), //
							ChatColor.RESET + CivSettings.localize.localizedString("tutorial_firework_Line4"), //
							ChatColor.RESET + CivSettings.localize.localizedString("tutorial_firework_Line5")));
		} else {
			this.addGuiItem(0, GuiItem.newGuiItem()//
					.setMaterial(Material.IRON_SWORD)//
					.setTitle(CivColor.LightBlue + ChatColor.BOLD + CivSettings.localize.localizedString("tutorial_ironSword_heading"))//
					.setLore(ChatColor.RESET + CivSettings.localize.localizedString("tutorial_ironSword_Line1"), //
							ChatColor.RESET + CivSettings.localize.localizedString("tutorial_ironSword_Line2"), //
							ChatColor.RESET + CivSettings.localize.localizedString("tutorial_ironSword_Line3"), //
							ChatColor.RESET + CivSettings.localize.localizedString("tutorial_ironSword_Line4"), //
							ChatColor.RESET + CivSettings.localize.localizedString("tutorial_ironSword_Line5"), //
							ChatColor.RESET + CivSettings.localize.localizedString("tutorial_ironSword_Line6")));
		}

		this.addGuiItem(8, GuiItem.newGuiItem()//
				.setMaterial(Material.BOOK_AND_QUILL)//
				.setTitle(CivColor.LightBlue + ChatColor.BOLD + CivSettings.localize.localizedString("tutorial_bookAndQuill_heading"))//
				.setLore(ChatColor.RESET + CivSettings.localize.localizedString("tutorial_bookAndQuill_Line1"), //
						ChatColor.RESET + CivSettings.localize.localizedString("tutorial_bookAndQuill_Line2"), //
						ChatColor.RESET + CivColor.LightGreen + ChatColor.BOLD + CivSettings.localize.localizedString("tutorial_bookAndQuill_Line3"), //
						ChatColor.RESET + CivSettings.localize.localizedString("tutorial_bookAndQuill_Line4")));

		this.addGuiItem(9, GuiItem.newGuiItem()//
				.setMaterial(Material.BOOK_AND_QUILL)//
				.setTitle(CivColor.LightBlue + ChatColor.BOLD + CivSettings.localize.localizedString("tutorial_campQuest_heading"))//
				.setLore(ChatColor.RESET + CivSettings.localize.localizedString("tutorial_campQuest_Line1"), //
						ChatColor.RESET + CivSettings.localize.localizedString("tutorial_campQuest_Line2"), //
						ChatColor.RESET + CivSettings.localize.localizedString("tutorial_campQuest_Line3"), //
						ChatColor.RESET + CivSettings.localize.localizedString("tutorial_campQuest_Line4")));

		this.addGuiItem(10, GuiItem.newGuiItem()//
				.setMaterial(Material.BOOK_AND_QUILL)//
				.setTitle(CivColor.LightBlue + ChatColor.BOLD + CivSettings.localize.localizedString("tutorial_civQuest_heading"))//
				.setLore(ChatColor.RESET + CivSettings.localize.localizedString("tutorial_civQuest_Line1"), //
						ChatColor.RESET + CivSettings.localize.localizedString("tutorial_civQuest_Line2"), //
						ChatColor.RESET + CivSettings.localize.localizedString("tutorial_civQuest_Line3"), //
						ChatColor.RESET + CivSettings.localize.localizedString("tutorial_civQuest_Line4"), //
						ChatColor.RESET + CivSettings.localize.localizedString("tutorial_civQuest_Line5")));

		this.addGuiItem(11, GuiItem.newGuiItem()//
				.setMaterial(Material.WORKBENCH)//
				.setTitle(CivColor.LightBlue + ChatColor.BOLD + CivSettings.localize.localizedString("tutorial_needRecipe_heading"))//
				.setLore(ChatColor.RESET + CivSettings.localize.localizedString("tutorial_needRecipe_Line1"), //
						ChatColor.RESET + CivSettings.localize.localizedString("tutorial_needRecipe_Line2"), //
						ChatColor.RESET + CivSettings.localize.localizedString("tutorial_needRecipe_Line3"), //
						ChatColor.RESET + CivSettings.localize.localizedString("tutorial_needRecipe_Line4"), //
						ChatColor.RESET + CivSettings.localize.localizedString("tutorial_needRecipe_Line5")));

		this.addGuiItem(19, GuiItem.newGuiItem(CraftingHelpRecipe.getInfoBookForItem("mat_found_civ"))//
				.setOpenInventory("CraftingHelpRecipe", "mat_found_civ"));
		this.addGuiItem(18, GuiItem.newGuiItem(CraftingHelpRecipe.getInfoBookForItem("mat_found_camp"))//
				.setOpenInventory("CraftingHelpRecipe", "mat_found_camp"));
		
	}

}
