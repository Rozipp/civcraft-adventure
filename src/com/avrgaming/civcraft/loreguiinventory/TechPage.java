package com.avrgaming.civcraft.loreguiinventory;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigTech;
import com.avrgaming.civcraft.lorestorage.GuiInventory;
import com.avrgaming.civcraft.lorestorage.GuiItem;
import com.avrgaming.civcraft.lorestorage.GuiItems;

public class TechPage extends GuiInventory {

	public TechPage(Player player, String arg) {
		super(player, arg);
		Boolean isTutorial = Boolean.parseBoolean(arg);
		this.setCiv(getResident().getCiv());
		this.setTitle(CivSettings.localize.localizedString("resident_techsGuiHeading"));

		if (!getCiv().GM.isLeaderOrAdviser(getResident())) isTutorial = true;

		for (ConfigTech tech : ConfigTech.getAvailableTechs(getCiv())) {
			GuiItem gi = GuiItems.newGuiItem()//
					.setTitle(tech.name)//
					.setMaterial(Material.EMERALD_BLOCK)//
					.setLore("§6" + CivSettings.localize.localizedString("clicktoresearch"), //
							"§b" + CivSettings.localize.localizedString("money_req", tech.getAdjustedTechCost(getCiv())), //
							"§a" + CivSettings.localize.localizedString("bealers_req", tech.getAdjustedBeakerCost(getCiv())), //
							"§d" + CivSettings.localize.localizedString("era_this", tech.era));//
			if (!isTutorial) gi.setCallbackGui(tech.name);
			this.addGuiItem(gi);
		}
	}

	@Override
	public void execute(String... strings) {
		if (getCiv().getResearchTech() == null)
			Bukkit.dispatchCommand((CommandSender) getPlayer(), "civ research on " + strings[0]);
		else
			Bukkit.dispatchCommand((CommandSender) getPlayer(), "civ research queueadd " + strings[0]);
	}

}
