package com.avrgaming.civcraft.command.admin;

import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.items.BaseCustomMaterial;
import com.avrgaming.civcraft.items.CustomMaterial;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.units.UnitCustomMaterial;
import com.avrgaming.civcraft.units.UnitObject;
import com.avrgaming.civcraft.units.UnitStatic;
import com.avrgaming.civcraft.util.ItemManager;

import net.minecraft.server.v1_12_R1.NBTTagCompound;

public class AdminUnitCommand extends CommandBase {
	@Override
	public void init() {
		command = "/ad mob";
		displayName = CivSettings.localize.localizedString("cmd_mob_managament");

		cs.add("setComponent", "[Equipments] [value] изменить уровень амуниции");
		cs.add("spawn", "[unitId] создать нового юнита");
		cs.add("respawn", "[Plaeyr][id] Дать игроку последнего его юнита.");
		cs.add("addUnitComponent", "Добавить книгу улучшения");
		cs.add("hashcode", "Вернуть hashcod интема в руке");
		cs.add("createUnitCustomMaterial", "Создать предмет юнита без метки юнита");
	}

	public void createUnitCustomMaterial_cmd() throws CivException {
		Player player = getPlayer();
		String ucmId;
		UnitCustomMaterial ucm;
		try {
			ucmId = this.getNamedString(1, "Введите Айди предмета");
			ucm = CustomMaterial.getUnitCustomMaterial(ucmId);
			if (ucm == null) throw new CivException("Не найден материал " + ucmId);
		} catch (CivException e) {
			CivMessage.sendSuccess(player, "список предметов юнита");
			String ss = "";
			for (BaseCustomMaterial bcm : CustomMaterial.getAllUnitCustomMaterial())
				ss = ss + bcm.getId() + ", ";
			CivMessage.sendSuccess(player, ss);
			throw new CivException(e.getMessage());
		}

		ItemStack is = CustomMaterial.spawn(ucm, 1);

		player.getInventory().addItem(is);
		CivMessage.send(player, "Предмет создан");
	}

	public void hashcode_cmd() throws CivException {
		Player player = getPlayer();
		ItemStack is = player.getEquipment().getItemInMainHand();
		 CivMessage.send(player, "В вашер руке " + is.hashCode());

		net.minecraft.server.v1_12_R1.ItemStack nmsStack = CraftItemStack.asNMSCopy(is);
		if (nmsStack != null && nmsStack.getTag() != null) {
			NBTTagCompound compound = nmsStack.getTag();
			if (compound != null) {
				CivMessage.send(player, compound.toString());
			}
		}

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
		UnitStatic.spawn(town, configUnitId);
	}

	public void respawn_cmd() throws CivException {
		String name = this.getNamedString(1, "Введите имя игрока");
		Player player = CivGlobal.getPlayer(name);
		if (player == null) throw new CivException("Ирок не найден");
		String idstring = getNamedString(2, "Введите айди юнита");
		int id = Integer.parseInt(idstring);
		if (player.getInventory().firstEmpty() == -1) throw new CivException(CivSettings.localize.localizedString("var_settler_errorBarracksFull", " id = " + id));
		player.getInventory().addItem(UnitStatic.respawn(id));
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

		uo.addComponent(key);
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