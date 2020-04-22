package com.avrgaming.civcraft.command.admin;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.units.UnitObject;
import com.avrgaming.civcraft.units.UnitStatic;
import com.avrgaming.civcraft.util.ItemManager;

public class AdminUnitCommand extends CommandBase {
	@Override
	public void init() {
		command = "/ad mob";
		displayName = CivSettings.localize.localizedString("cmd_mob_managament");

		cs.add("setComponent", "[Equipments] [value] изменить уровень амуниции");
		cs.add("getComponents", "показать все компоненты которые есть у юнита");
		cs.add("spawn", "[unitId] создать нового юнита");
		cs.add("respawn", "[Plaeyr] Дать игроку последнего его юнита.");
		cs.add("addUnitComponent", "Добавить книгу улучшения");
	}
	
	public void addUnitComponent_cmd() throws CivException {

		Player player = getPlayer();
		ItemStack stack = ItemManager.createItemStack("u_choiceunitcomponent", 1);
		player.getInventory().addItem(stack);
		CivMessage.send(player, "предмет добавлен");
	}

	public void spawn_cmd() throws CivException {

		Player player = getPlayer();
		Town town = CivGlobal.getResident(player).getTown();
		if (town == null) throw new CivException("У вас нет города");
		String configUnitId = this.getNamedString(1, "Введите configUnitId юнита");
		UnitStatic.spawn(player.getInventory(), town, configUnitId);
	}
	
	public void respawn_cmd() throws CivException {
		String name = this.getNamedString(1, "Введите имя игрока");
		Player player = CivGlobal.getPlayer(name);
		if (player == null) throw new CivException("Ирок не найден");
		UnitStatic.respawn(player);
	}

	public void getComponents_cmd() throws CivException {
		Player player = getPlayer();
		UnitObject uo = CivGlobal.getUnitObject(UnitStatic.getUnitIdNBTTag(UnitStatic.findUnit(player)));
		uo.printAllComponents(player);
	}
	public void setComponent_cmd() throws CivException {
		Player player = getPlayer();
		UnitObject uo = CivGlobal.getUnitObject(UnitStatic.getUnitIdNBTTag(UnitStatic.findUnit(player)));
		if (uo == null) return;

		String key;
		try {
			key = getNamedString(1, "Введите имя компонента");
		} catch (CivException e) {
			CivMessage.sendSuccess(player, "Компоненты екиперовки (0-4): helmet, chestplate, leggings, boots, sword, two");
			CivMessage.sendSuccess(player, "Компоненты характеристики(0-50): speed, maxheal, protection, swordattack, bowattack");
			CivMessage.sendSuccess(player, "Компоненты зачарования(значение = уровень зачарования): ___???___???___???___???___");
			throw new CivException(e.getMessage());
		}
		Integer value = getNamedInteger(2);

		uo.setComponent(key, value);
		UnitStatic.updateUnitForPlaeyr(player);

		CivMessage.sendSuccess(player, "установлено успешно ");
	}

	@Override
	public void doDefaultAction() throws CivException {
		showHelp();
	}

	@Override
	public void showHelp() {
		showBasicHelp();
	}

	@Override
	public void permissionCheck() throws CivException {
	}
}