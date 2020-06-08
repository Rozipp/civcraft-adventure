package com.avrgaming.civcraft.interactive;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.construct.template.ChoiseTemplate;
import com.avrgaming.civcraft.construct.template.Template;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.lorestorage.LoreGuiItem;
import com.avrgaming.civcraft.lorestorage.LoreGuiItemListener;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CallbackInterface;
import com.avrgaming.civcraft.util.ItemManager;

public class BuildCallbackDbg implements CallbackInterface {

	private Player player;
	private Resident resident;

	public BuildCallbackDbg(Player player) throws CivException {
		this.player = player;
		resident = CivGlobal.getResident(player);
		Inventory inv = Bukkit.getServer().createInventory((InventoryHolder) player, 54, CivSettings.localize.localizedString("resident_structuresGuiHeading"));
		for (final ConfigBuildableInfo info : CivSettings.structures.values()) {
			final int type = ItemManager.getMaterialId(Material.EMERALD_BLOCK);
			ItemStack itemStack;
			itemStack = LoreGuiItem.build(info.displayName, type, 0, "§6" + CivSettings.localize.localizedString("clicktobuild"), "", "", "");
			itemStack = LoreGuiItem.setAction(itemStack, "BuildTemplateDbg");
			itemStack = LoreGuiItem.setActionData(itemStack, "info", info.id);
			inv.addItem(itemStack);
			continue;
		}
		ItemStack is = LoreGuiItem.build("§e" + CivSettings.localize.localizedString("4udesa"), ItemManager.getMaterialId(Material.DIAMOND_BLOCK), 0, "§6" + CivSettings.localize.localizedString("click_to_view"));
		is = LoreGuiItem.setAction(is, "WondersGui");
		inv.setItem(53, is);
		LoreGuiItemListener.guiInventories.put(inv.getName(), inv);
		resident.setPendingCallback(this);
		player.openInventory(inv);
	}

	private ConfigBuildableInfo sinfo = null;
	private String templateTheme = null;

	@Override
	public void execute(String... strings) {
		if (sinfo == null) {
			String buildName = strings[0];
			try {
				sinfo = CivSettings.structures.get(buildName);
				if (sinfo == null) throw new CivException(CivSettings.localize.localizedString("cmd_build_defaultUnknownStruct") + " " + buildName);
				player.closeInventory();

				new ChoiseTemplate(player, sinfo, this);
			} catch (CivException e) {
				e.printStackTrace();
				sinfo = null;
				resident.clearInteractiveMode();
			}
			return;
		}

		if (templateTheme == null) {
			templateTheme = strings[0];
			
			Template tpl;
			try {
				String tplPath = Template.getTemplateFilePath(player.getLocation(), sinfo , templateTheme);
				tpl = Template.getTemplate(tplPath);
				if (tpl == null) throw new CivException("Не найден шаблон " + tplPath);
				
				tpl.buildTemplateDbg(new BlockCoord(player.getLocation()));
			} catch (CivException e) {
				CivMessage.sendError(player, e.getMessage());
			}
			resident.clearInteractiveMode();
			return;
		}

	}

}
