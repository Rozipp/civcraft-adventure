package com.avrgaming.civcraft.command.admin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.command.Commander;
import com.avrgaming.civcraft.command.CustomCommand;
import com.avrgaming.civcraft.command.MenuAbstractCommand;
import com.avrgaming.civcraft.command.taber.TownInCivTaber;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.items.BaseCustomMaterial;
import com.avrgaming.civcraft.items.CustomMaterial;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.units.ConfigUnit;
import com.avrgaming.civcraft.units.UnitCustomMaterial;
import com.avrgaming.civcraft.units.UnitObject;
import com.avrgaming.civcraft.units.UnitStatic;
import com.avrgaming.civcraft.util.ItemManager;

public class AdminUnitCommand extends MenuAbstractCommand {
	public AdminUnitCommand(String perentCommand) {
		super(perentCommand);
		displayName = CivSettings.localize.localizedString("cmd_mob_managament");

		add(new CustomCommand("spawnunit").withDescription(CivSettings.localize.localizedString("adcmd_spawnUnitDesc")).withTabCompleter(new TownInCivTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Player player = Commander.getPlayer(sender);
				Town town = Commander.getNamedTown(args, 0);
				if (args.length < 2) throw new CivException(CivSettings.localize.localizedString("adcmd_spawnUnitPrompt"));
				ConfigUnit unit = UnitStatic.configUnits.get(args[1]);
				if (unit == null) throw new CivException(CivSettings.localize.localizedString("var_adcmd_spawnUnitInvalid", args[1]));
				
				// if (args.length > 2) {
				// try {
				// player = CivGlobal.getPlayer(args[2]);
				// } catch (CivException e) {
				// throw new CivException("Player "+args[2]+" is not online.");
				// }
				// } else {
				// player = getPlayer();
				// }

				Class<?> c;
				try {
					c = Class.forName(unit.class_name);
					Method m = c.getMethod("spawn", Inventory.class, Town.class);
					m.invoke(null, player.getInventory(), town);
				} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					throw new CivException(e.getMessage());
				}
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_spawnUnitSuccess", unit.name));
			}
		}));
		add(new CustomCommand("setComponent").withDescription("[Equipments] [value] изменить уровень амуниции").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Player player = Commander.getPlayer(sender);
				UnitObject uo = CivGlobal.getUnitObject(UnitStatic.getUnitIdNBTTag(UnitStatic.findUnit(player)));
				if (uo == null) return;

				String key;
				try {
					key = Commander.getNamedString(args, 0, "Введите имя компонента");
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
		}));
		add(new CustomCommand("spawn").withDescription("[unitId] создать нового юнита").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Player player = Commander.getPlayer(sender);
				Town town = CivGlobal.getResident(player).getTown();
				if (town == null) throw new CivException("У вас нет города");
				String configUnitId = Commander.getNamedString(args, 0, "Введите configUnitId юнита");
				UnitStatic.spawn(town, configUnitId);
			}
		}));
		add(new CustomCommand("respawn").withDescription("[Plaeyr][id] Дать игроку последнего его юнита.").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				String name = Commander.getNamedString(args, 0, "Введите имя игрока");
				Player player = CivGlobal.getPlayer(name);
				if (player == null) throw new CivException("Ирок не найден");
				String idstring = Commander.getNamedString(args, 1, "Введите айди юнита");
				int id = Integer.parseInt(idstring);
				if (player.getInventory().firstEmpty() == -1) throw new CivException(CivSettings.localize.localizedString("var_settler_errorBarracksFull", " id = " + id));
				player.getInventory().addItem(UnitStatic.respawn(CivGlobal.getUnitObject(id)));
			}
		}));
		add(new CustomCommand("addUnitComponent").withDescription("Добавить книгу улучшения").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Player player = Commander.getPlayer(sender);
				ItemStack stack = ItemManager.createItemStack("u_choiceunitcomponent", 1);
				player.getInventory().addItem(stack);
				CivMessage.send(player, "предмет добавлен");
			}
		}));
		add(new CustomCommand("createUnitCustomMaterial").withDescription("Создать предмет юнита без метки юнита").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Player player = Commander.getPlayer(sender);
				String ucmId;
				UnitCustomMaterial ucm;
				try {
					ucmId = Commander.getNamedString(args, 0, "Введите Айди предмета");
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
		}));
	}
}