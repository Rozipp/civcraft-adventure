package ua.rozipp.gui.guiinventory;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigTech;
import com.avrgaming.civcraft.exception.CivException;
import ua.rozipp.gui.GuiInventory;
import ua.rozipp.gui.GuiItem;

public class TechPage extends GuiInventory {

	public TechPage(Player player, String arg) throws CivException {
		super(player, player, arg);
		boolean isTutorial = Boolean.parseBoolean(arg);
		this.setCiv(getResident().getCiv());

		if (isTutorial) this.setPlayer(null);
		this.setTitle(CivSettings.localize.localizedString("resident_techsGuiHeading"));

		if (!getCiv().GM.isLeaderOrAdviser(getResident())) isTutorial = true;

		for (ConfigTech tech : ConfigTech.getAvailableTechs(getCiv())) {
			GuiItem gi = GuiItem.newGuiItem()//
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
		GuiInventory.closeInventory(getPlayer());
		if (getCiv().getResearchTech() == null)
			Bukkit.dispatchCommand(getPlayer(), "civ research on " + strings[0]);
		else
			Bukkit.dispatchCommand(getPlayer(), "civ research queueadd " + strings[0]);
	}

}
