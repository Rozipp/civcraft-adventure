package com.avrgaming.civcraft.gui.guiinventory;

import java.text.SimpleDateFormat;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.gui.GuiInventory;
import com.avrgaming.civcraft.gui.GuiItems;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Relation;
import com.avrgaming.civcraft.util.CivColor;

public class RelationPage extends GuiInventory {

	public RelationPage(Player player, String arg) throws CivException {
		super(player, arg);
		if (getResident().getTown() == null) {
			CivMessage.send((Object) player, "§c" + CivSettings.localize.localizedString("res_gui_noTown"));
			return;
		}
		this.setCiv(getResident().getCiv());
		if (arg == null) createGuiPerent();
		if (arg == "Allies") createGuiAllies();
		if (arg == "Peaces") createGuiPeaces();
		if (arg == "Hostiles") createGuiHostiles();
		if (arg == "Wars") createGuiWars();
	}

	private void createGuiPerent() {
		this.setRow(1);
		this.setTitle(CivSettings.localize.localizedString("resident_relationsGuiHeading"));
		this.addGuiItem(GuiItems.newGuiItem()//
				.setTitle(CivColor.LightGreenBold + CivSettings.localize.localizedString("resident_relationsGui_ally"))//
				.setMaterial(Material.EMERALD_BLOCK)//
				.setLore(ChatColor.RESET + CivSettings.localize.localizedString("resident_relationsGui_allyInfo"), //
						"§6" + CivSettings.localize.localizedString("bookReborn_clickToView"))//
				.setOpenInventory("RelationPage", "Allies"));
		this.addGuiItem(GuiItems.newGuiItem()//
				.setTitle(CivColor.LightGreenBold + CivSettings.localize.localizedString("resident_relationsGui_peace"))//
				.setMaterial(Material.LAPIS_BLOCK)//
				.setLore(ChatColor.RESET + CivSettings.localize.localizedString("resident_relationsGui_peaceInfo"), //
						"§6" + CivSettings.localize.localizedString("bookReborn_clickToView"))//
				.setOpenInventory("RelationPage", "Peaces"));
		this.addGuiItem(GuiItems.newGuiItem()//
				.setTitle(CivColor.LightGreenBold + CivSettings.localize.localizedString("resident_relationsGui_hostile"))//
				.setMaterial(Material.GOLD_BLOCK)//
				.setLore(ChatColor.RESET + CivSettings.localize.localizedString("resident_relationsGui_hostileInfo"), //
						"§6" + CivSettings.localize.localizedString("bookReborn_clickToView"))//
				.setOpenInventory("RelationPage", "Hostiles"));
		this.addGuiItem(GuiItems.newGuiItem()//
				.setTitle(CivColor.LightGreenBold + CivSettings.localize.localizedString("resident_relationsGui_war"))//
				.setMaterial(Material.REDSTONE_BLOCK)//
				.setLore(ChatColor.RESET + CivSettings.localize.localizedString("resident_relationsGui_warInfo"), //
						"§6" + CivSettings.localize.localizedString("bookReborn_clickToView"))//
				.setOpenInventory("RelationPage", "Wars"));
	}

	public void createGuiAllies() {
		SimpleDateFormat sdf = CivGlobal.dateFormat;
		this.setTitle(CivSettings.localize.localizedString("resident_relationsGui_ally"));
		for (Relation relation : getCiv().getDiplomacyManager().getRelations()) {
			if (relation.getStatus() == Relation.Status.ALLY) {
				this.addGuiItem(GuiItems.newGuiItem()//
						.setMaterial(Material.EMERALD_BLOCK)//
						.setLore(ChatColor.RESET + CivSettings.localize.localizedString("resident_relationsGui_relationToString", relation.toString()), //
								"§6" + CivSettings.localize.localizedString("relation_creationDate", sdf.format(relation.getCreatedDate()))));
			}
		}
	}

	public void createGuiPeaces() {
		SimpleDateFormat sdf = CivGlobal.dateFormat;
		this.setTitle(CivSettings.localize.localizedString("resident_relationsGui_peace"));
		for (Relation relation : getCiv().getDiplomacyManager().getRelations()) {
			if (relation.getStatus() == Relation.Status.PEACE) {
				this.addGuiItem(GuiItems.newGuiItem()//
						.setMaterial(Material.LAPIS_BLOCK)//
						.setLore(ChatColor.RESET + CivSettings.localize.localizedString("resident_relationsGui_relationToString", relation.toString()), //
								"§6" + CivSettings.localize.localizedString("relation_creationDate", sdf.format(relation.getCreatedDate()))));
			}
		}
	}

	public void createGuiHostiles() {
		SimpleDateFormat sdf = CivGlobal.dateFormat;
		this.setTitle(CivSettings.localize.localizedString("resident_relationsGui_hostile"));
		for (Relation relation : getCiv().getDiplomacyManager().getRelations()) {
			if (relation.getStatus() == Relation.Status.HOSTILE) {
				this.addGuiItem(GuiItems.newGuiItem()//
						.setMaterial(Material.GOLD_BLOCK)//
						.setLore(ChatColor.RESET + CivSettings.localize.localizedString("resident_relationsGui_relationToString", relation.toString()), //
								"§6" + CivSettings.localize.localizedString("relation_creationDate", sdf.format(relation.getCreatedDate()))));
			}
		}
	}

	public void createGuiWars() {
		SimpleDateFormat sdf = CivGlobal.dateFormat;
		this.setTitle(CivSettings.localize.localizedString("resident_relationsGui_war"));
		for (Relation relation : getCiv().getDiplomacyManager().getRelations()) {
			if (relation.getStatus() == Relation.Status.WAR) {
				this.addGuiItem(GuiItems.newGuiItem()//
						.setMaterial(Material.REDSTONE_BLOCK)//
						.setLore(ChatColor.RESET + CivSettings.localize.localizedString("resident_relationsGui_relationToString", relation.toString()), //
								"§6" + CivSettings.localize.localizedString("relation_creationDate", sdf.format(relation.getCreatedDate()))));
			}
		}
	}
}
