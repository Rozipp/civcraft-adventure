package com.avrgaming.civcraft.command.debug;

import java.io.IOException;

import org.bukkit.configuration.InvalidConfigurationException;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.command.admin.AdminCommand;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigCraftableMaterial;
import com.avrgaming.civcraft.config.ConfigUnitMaterial;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.items.CraftableCustomMaterial;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.units.ConfigUnit;
import com.avrgaming.civcraft.units.UnitCustomMaterial;
import com.avrgaming.civcraft.units.UnitStatic;

import ua.rozipp.sound.SoundManager;

public class DebugReloadConfCommand extends CommandBase {

	@Override
	public void init() {
		command = "/dbg reloadconf";
		displayName = "Reload config Commands";

		cs.add("sound", "Reload sound");
		cs.add("craftmaterial", "Reload crafmaterial");
		cs.add("unitmaterial", "Reload unitmaterial");
		cs.add("unitseting", "Reload unit setings");
	}

	public void unitseting_cmd() throws CivException {
		try {
			CivSettings.unitConfig = CivSettings.loadCivConfig("units.yml");
			ConfigUnit.loadConfig(CivSettings.unitConfig, UnitStatic.configUnits);
		} catch (IOException | InvalidConfigurationException e) {
			e.printStackTrace();
		}
		CivMessage.send(getPlayer(), "Unit config reloaded");
	}
	
	public void craftmaterial_cmd() throws CivException {
		try {
			CivSettings.craftableMaterialsConfig = CivSettings.loadCivConfig("materials.yml");
			ConfigCraftableMaterial.loadConfigCraftable(CivSettings.craftableMaterialsConfig, CivSettings.craftableMaterials);
			CraftableCustomMaterial.buildStaticMaterials();
			CraftableCustomMaterial.buildRecipes();
			AdminCommand.spawnInventory = null;
		} catch (IOException | InvalidConfigurationException e) {
			e.printStackTrace();
		}
		CivMessage.send(getPlayer(), "CraftableMaterial reloaded");
	}
	
	public void unitmaterial_cmd() throws CivException {
		try {
			CivSettings.unitMaterialsConfig = CivSettings.loadCivConfig("unitmaterials.yml");
			ConfigUnitMaterial.loadConfigUnit(CivSettings.unitMaterialsConfig, CivSettings.unitMaterials);
			UnitCustomMaterial.buildStaticMaterials();
		} catch (IOException | InvalidConfigurationException e) {
			e.printStackTrace();
		}
		CivMessage.send(getPlayer(), "UnitMaterial reloaded");
	}
	
	public void sound_cmd() throws CivException {
		try {
			CivSettings.soundConfig = CivSettings.loadCivConfig("sound.yml");
			SoundManager.loadConfig(CivSettings.soundConfig);
		} catch (IOException | InvalidConfigurationException e) {
			e.printStackTrace();
		}
		CivMessage.send(getPlayer(), "Sound config reloaded");
	}

	@Override
	public void doDefaultAction() throws CivException {
		showBasicHelp();
	}

	@Override
	public void showHelp() {
		showHelp();
	}

	@Override
	public void permissionCheck() throws CivException {
	}

}
