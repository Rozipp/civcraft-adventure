package ua.rozipp.gui.guiinventory;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigMaterial;
import com.avrgaming.civcraft.config.ConfigMaterialCategory;
import com.avrgaming.civcraft.exception.CivException;
import ua.rozipp.gui.GuiInventory;
import ua.rozipp.gui.GuiItem;
import com.avrgaming.civcraft.util.CivColor;

public class CraftingHelp extends GuiInventory {

	public CraftingHelp(Player player, String arg) throws CivException {
		super(player, null, arg);
		if (arg == null)
			createPerent();
		else
			createCategory();
	}

	private void createPerent() {
		this.setTitle(CivSettings.localize.localizedString("tutorial_customRecipesHeading"));

		/* Build the Category Inventory. */
		for (ConfigMaterialCategory cat : ConfigMaterialCategory.getCategories()) {
			if (cat.craftableCount == 0) continue;
			Material identifier = cat.title.contains("Fish") ? Material.RAW_FISH //
					: cat.title.contains("Gear") ? Material.IRON_SWORD //
							: cat.title.contains("Materials") ? Material.WOOD_STEP //
									: cat.title.contains("Tools") ? Material.IRON_SPADE : //
											Material.WRITTEN_BOOK;
			this.addGuiItem(GuiItem.newGuiItem()//
					.setTitle(cat.title)//
					.setMaterial(identifier)//
					.addLore(CivColor.LightBlue + cat.materials.size() + " " + CivSettings.localize.localizedString("tutorial_lore_items")) //
					.addLore(CivColor.Gold + CivSettings.localize.localizedString("tutorial_lore_clickToOpen"))//
					.setOpenInventory("CraftingHelp", cat.id));
		}
	}

	private void createCategory() {
		/* Build a new GUI Inventory. */
		ConfigMaterialCategory cat = ConfigMaterialCategory.getCategory(this.getArg());
		this.setTitle(CivSettings.localize.localizedString("tutorial_lore_recipes", cat.title));
		for (ConfigMaterial mat : cat.materials.values()) {
			ItemStack stack = CraftingHelpRecipe.getInfoBookForItem(mat.id);
			if (stack == null) continue;
			this.addGuiItem(GuiItem.newGuiItem(stack).setOpenInventory("CraftingHelpRecipe", mat.id));
		}
	}

}
