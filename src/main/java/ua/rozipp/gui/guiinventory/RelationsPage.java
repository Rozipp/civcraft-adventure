package ua.rozipp.gui.guiinventory;

import java.text.SimpleDateFormat;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import ua.rozipp.gui.GuiInventory;
import ua.rozipp.gui.GuiItem;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Relation;
import com.avrgaming.civcraft.util.CivColor;

public class RelationsPage extends GuiInventory {

	public RelationsPage(Player player, String arg) throws CivException {
		super(player, player, arg);
		if (getResident().getTown() == null) throw new CivException("§c" + CivSettings.localize.localizedString("res_gui_noTown"));
		this.setCiv(getResident().getCiv());
		if (arg == null) createGuiPerent();
		if (arg.equals("Allies")) createGuiAllies();
		if (arg.equals("Peaces")) createGuiPeaces();
		if (arg.equals("Hostiles")) createGuiHostiles();
		if (arg.equals("Wars")) createGuiWars();
	}

	private void createGuiPerent() {
		this.setRow(1);
		this.setTitle(CivSettings.localize.localizedString("resident_relationsGuiHeading"));
		this.addGuiItem(GuiItem.newGuiItem()//
				.setTitle(CivColor.LightGreenBold + CivSettings.localize.localizedString("resident_relationsGui_ally"))//
				.setMaterial(Material.EMERALD_BLOCK)//
				.setLore(ChatColor.RESET + CivSettings.localize.localizedString("resident_relationsGui_allyInfo"), //
						"§6" + CivSettings.localize.localizedString("bookReborn_clickToView"))//
				.setOpenInventory("RelationPage", "Allies"));
		this.addGuiItem(GuiItem.newGuiItem()//
				.setTitle(CivColor.LightGreenBold + CivSettings.localize.localizedString("resident_relationsGui_peace"))//
				.setMaterial(Material.LAPIS_BLOCK)//
				.setLore(ChatColor.RESET + CivSettings.localize.localizedString("resident_relationsGui_peaceInfo"), //
						"§6" + CivSettings.localize.localizedString("bookReborn_clickToView"))//
				.setOpenInventory("RelationPage", "Peaces"));
		this.addGuiItem(GuiItem.newGuiItem()//
				.setTitle(CivColor.LightGreenBold + CivSettings.localize.localizedString("resident_relationsGui_hostile"))//
				.setMaterial(Material.GOLD_BLOCK)//
				.setLore(ChatColor.RESET + CivSettings.localize.localizedString("resident_relationsGui_hostileInfo"), //
						"§6" + CivSettings.localize.localizedString("bookReborn_clickToView"))//
				.setOpenInventory("RelationPage", "Hostiles"));
		this.addGuiItem(GuiItem.newGuiItem()//
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
				this.addGuiItem(GuiItem.newGuiItem()//
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
				this.addGuiItem(GuiItem.newGuiItem()//
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
				this.addGuiItem(GuiItem.newGuiItem()//
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
				this.addGuiItem(GuiItem.newGuiItem()//
						.setMaterial(Material.REDSTONE_BLOCK)//
						.setLore(ChatColor.RESET + CivSettings.localize.localizedString("resident_relationsGui_relationToString", relation.toString()), //
								"§6" + CivSettings.localize.localizedString("relation_creationDate", sdf.format(relation.getCreatedDate()))));
			}
		}
	}
}
