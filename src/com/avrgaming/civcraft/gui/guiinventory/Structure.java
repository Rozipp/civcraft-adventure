package com.avrgaming.civcraft.gui.guiinventory;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.config.ConfigTech;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.gui.GuiInventory;
import com.avrgaming.civcraft.gui.GuiItem;
import com.avrgaming.civcraft.gui.GuiItems;
import com.avrgaming.civcraft.interactive.BuildCallback;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;

public class Structure extends GuiInventory {

	public Structure(Player player, String arg) throws CivException {
		super(player, arg);
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
		this.setTitle(CivSettings.localize.localizedString("resident_structuresGuiHeading") + (getTown() != null ? " " + getTown().getName() : " Tutorial"));

		double rate = 1.0;
		if (!isTutorial) {
			rate -= getTown().getBuffManager().getEffectiveDouble("buff_rush");
			// rate -= getTown().getBuffManager().getEffectiveDouble("buff_grandcanyon_rush");
			// rate -= getTown().getBuffManager().getEffectiveDouble("buff_mother_tree_tile_improvement_cost");
		}

		for (ConfigBuildableInfo info : CivSettings.structures.values()) {
			double hammerCost = Math.round(info.hammer_cost * rate);
			int count = isTutorial ? 1 : (info.limit == 0 ? 64 : info.limit) - getTown().BM.getBuildableByIdCount(info.id);
			GuiItem gi = (count > 0) ? GuiItems.newGuiItem(ItemManager.createItemStack(info.gui_item_id, (short) 0, count)) : GuiItems.newGuiItem().setMaterial(Material.BEDROCK);
			gi.setTitle(info.displayName)//
					.setLore("§b" + CivSettings.localize.localizedString("money_requ", Double.parseDouble(String.valueOf(info.cost))), //
							"§a" + CivSettings.localize.localizedString("hammers_requ", hammerCost), //
							"§d" + CivSettings.localize.localizedString("upkeep_day", info.upkeep)); //
			if (info.require_tech != null) {
				for (String t : info.require_tech.split(",")) {
					ConfigTech tech = CivSettings.techs.get(t);
					if (!isTutorial) {
						if (!getTown().getCiv().hasTechnologys(t)) {
							gi.setMaterial(Material.BEDROCK);
							gi.addLore(CivColor.Red + CivSettings.localize.localizedString("req") + tech.name, //
									"§3" + CivSettings.localize.localizedString("clicktoresearch"));
							gi.setOpenInventory("TechPage", isTutorial.toString());
						}
					} else
						gi.addLore(CivColor.LightBlue + CivSettings.localize.localizedString("req") + tech.name);
					gi.addLore("§d" + CivSettings.localize.localizedString("era_this", tech.era));
				}
			}
			ConfigBuildableInfo str = CivSettings.structures.get(info.require_structure);
			if (str != null) {
				gi.setMaterial(Material.BEDROCK).setLore(CivColor.Red + CivSettings.localize.localizedString("requ") + str.displayName);
			}
			if (info.isAvailable(getTown())) {
				if (!isTutorial) {
					gi.addLore("§6" + CivSettings.localize.localizedString("clicktobuild")); //
					gi.setCallbackGui(info.id);
				}
			}
			this.addGuiItem(info.gui_slot, gi);
		}
		this.addGuiItem(52, GuiItems.newGuiItem()//
				.setTitle(CivSettings.localize.localizedString("4udesa"))//
				.setMaterial(Material.DIAMOND_BLOCK)//
				.setLore("§6" + CivSettings.localize.localizedString("click_to_view"))//
				.setOpenInventory("Wonders", isTutorial.toString()));
	}

	@Override
	public void execute(String... strings) {
		try {
			GuiInventory.closeInventory(getPlayer());
			try {
				String buildId = strings[0];
				ConfigBuildableInfo sinfo = CivSettings.structures.get(buildId);
				if (sinfo == null) throw new CivException(CivSettings.localize.localizedString("cmd_build_defaultUnknownStruct") + " " + buildId);
				getResident().setPendingCallback(new BuildCallback(getPlayer(), sinfo, getTown()));
			} catch (CivException e) {
				CivMessage.sendError(getPlayer(), e.getMessage());
			}
		} catch (CivException e1) {
			// TODO Автоматически созданный блок catch
			e1.printStackTrace();
		}
	}

}
