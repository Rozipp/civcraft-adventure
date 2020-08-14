package com.avrgaming.civcraft.command.old;

import java.io.IOException;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;

import com.avrgaming.civcraft.command.CustomCommand;
import com.avrgaming.civcraft.command.MenuAbstractCommand;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigCraftableMaterial;
import com.avrgaming.civcraft.config.ConfigGovernment;
import com.avrgaming.civcraft.config.ConfigUnitMaterial;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.items.CraftableCustomMaterial;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.units.ConfigUnit;
import com.avrgaming.civcraft.units.UnitCustomMaterial;
import com.avrgaming.civcraft.units.UnitStatic;
import com.avrgaming.civcraft.util.CivColor;

import ua.rozipp.sound.SoundManager;

public class DebugReloadConfCommand extends MenuAbstractCommand {

	public DebugReloadConfCommand(String perentCommand) {
		super(perentCommand);
		displayName = "Reload config Commands";

		add(new CustomCommand("sound").withDescription("Reload sound").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				try {
					CivSettings.soundConfig = CivSettings.loadCivConfig("sound.yml");
					SoundManager.loadConfig(CivSettings.soundConfig);
				} catch (IOException | InvalidConfigurationException e) {
					e.printStackTrace();
				}
				CivMessage.send(sender, "Sound config reloaded");
			}
		}));
		add(new CustomCommand("craftmaterial").withDescription("Reload crafmaterial").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				try {
					CivSettings.craftableMaterialsConfig = CivSettings.loadCivConfig("materials.yml");
					ConfigCraftableMaterial.loadConfigCraftable(CivSettings.craftableMaterialsConfig, CivSettings.craftableMaterials);
					CraftableCustomMaterial.buildStaticMaterials();
					CraftableCustomMaterial.buildRecipes();
				} catch (IOException | InvalidConfigurationException e) {
					e.printStackTrace();
				}
				CivMessage.send(sender, "CraftableMaterial reloaded");
			}
		}));
		add(new CustomCommand("unitmaterial").withDescription("Reload unitmaterial").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				try {
					CivSettings.unitMaterialsConfig = CivSettings.loadCivConfig("unitmaterials.yml");
					ConfigUnitMaterial.loadConfigUnit(CivSettings.unitMaterialsConfig, CivSettings.unitMaterials);
					UnitCustomMaterial.buildStaticMaterials();
				} catch (IOException | InvalidConfigurationException e) {
					e.printStackTrace();
				}
				CivMessage.send(sender, "UnitMaterial reloaded");
			}
		}));
		add(new CustomCommand("unitseting").withDescription("Reload unit setings").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				try {
					CivSettings.unitConfig = CivSettings.loadCivConfig("units.yml");
					ConfigUnit.loadConfig(CivSettings.unitConfig, UnitStatic.configUnits);
				} catch (IOException | InvalidConfigurationException e) {
					e.printStackTrace();
				}
				CivMessage.send(sender, "Unit config reloaded");
			}
		}));
		add(new CustomCommand("reloadgov").withDescription(CivSettings.localize.localizedString("adcmd_reloadgovDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				CivSettings.governments.clear();
				try {
					CivSettings.governmentConfig = CivSettings.loadCivConfig("governments.yml");
				} catch (IOException | InvalidConfigurationException e) {
					throw new CivException(e.getMessage());
				}
				ConfigGovernment.loadConfig(CivSettings.governmentConfig, CivSettings.governments);
				for (Civilization civ : CivGlobal.getCivs()) {
					ConfigGovernment gov = civ.getGovernment();
					civ.setGovernment(gov.id);
				}
				CivMessage.send(sender, CivColor.Gold + CivSettings.localize.localizedString("adcmd_reloadgovSuccess"));
			}
		}));
		add(new CustomCommand("newspaper").withDescription(CivSettings.localize.localizedString("adcmd_newspaper")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				try {
					CivSettings.reloadNewspaperConfigFiles();
				} catch (IOException | InvalidConfigurationException e) {
					throw new CivException(e.getMessage());
				}
				CivMessage.send(sender, CivColor.Gold + CivSettings.localize.localizedString("adcmd_newspaper_done"));
				CivMessage.global(CivSettings.localize.localizedString("adcmd_newspaper_broadcast"));
			}
		}));
	}
}
