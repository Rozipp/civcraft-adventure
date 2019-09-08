/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.units;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigMission;
import com.avrgaming.civcraft.items.UnitCustomMaterial;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.MissionLogger;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.BookUtil;

import gpl.AttributeUtil;

public class Spy extends UnitMaterial {

	public final int BOOK_ID = 403;
	
	public HashSet<Integer> allowedSubslots = new HashSet<Integer>();
	public HashMap<String, UnitCustomMaterial> missionBooks = new HashMap<>();

	
	
	public Spy(String id, ConfigUnit configUnit) {
		super(id, configUnit);
		//TODO отправить в клас шпиона
		for (final ConfigMission mission : CivSettings.missions.values()) {
			if (mission.slot > 0) {
				CivLog.debug("mission.id " + mission.id);
				final MissionBook book = new MissionBook(mission.id, BOOK_ID, (short) 0);
				CivLog.debug("book.getId() " + book.getId());
				book.setName(mission.name);
				book.setupLore(book.getId());
				book.setParent(this);
				book.setSocketSlot(mission.slot);
				addMissionBook(book);
				//SPY_MISSIONS.add(book); 
			}
		}
	}
	
	@Override
	public void initUnitObject(UnitObject uo) {
		uo.setComponent("sword", 0);
	}
	
	@Override
	public void initLore(AttributeUtil attrs, UnitObject uo) {
//		String h = CivColor.RoseBold + uo.getComponent(Equipments.HELMET);
//		String c = CivColor.RoseBold + uo.getComponent(Equipments.CHESTPLATE);
//		String l = CivColor.RoseBold + uo.getComponent(Equipments.LEGGINGS);
//		String b = CivColor.RoseBold + uo.getComponent(Equipments.BOOTS);
//		String s = CivColor.RoseBold + uo.getComponent(Equipments.SWORD);
//		String t = CivColor.RoseBold + uo.getComponent(Equipments.TWOHAND);
//		attrs.addLore(CivColor.RoseBold + "               Амуниция:");
//		attrs.addLore(CivColor.Rose     + " правая   Голова  : T" + h + CivColor.Rose + "   левая");
//		attrs.addLore(CivColor.Rose     + "  рука    Грудь   : T" + c + CivColor.Rose + "   рука ");
//		attrs.addLore(CivColor.Rose     + "   Т" + s + CivColor.Rose + "     Ноги    : T" + l + CivColor.Rose + "    Т" + t);
//		attrs.addLore(CivColor.Rose     + "          Ступни  : T" + b);
	}
	@Override
	public void initAmmunitions() {
//		SlotElement e = null;
//		//helmet
//		e = new SlotElement(39);
//		e.addComponent("WaterWorker");
//		e.addMatTir("");
//		e.addMatTir("mat_leather_helmet");
//		e.addMatTir("mat_refined_leather_helmet");
//		e.addMatTir("mat_hardened_leather_helmet");
//		e.addMatTir("mat_composite_leather_helmet");
//		ammunitions.put(Equipments.HELMET, e);
//
//		//chestplate
//		e = new SlotElement(38);
//		e.addComponent("MaxHeal");
//		e.addMatTir("");
//		e.addMatTir("mat_leather_chestplate");
//		e.addMatTir("mat_refined_leather_chestplate");
//		e.addMatTir("mat_hardened_leather_chestplate");
//		e.addMatTir("mat_composite_leather_chestplate");
//		ammunitions.put(Equipments.CHESTPLATE, e);
//
//		//leggings
//		e = new SlotElement(37);
//		e.addComponent("speed");
//		e.addMatTir("");
//		e.addMatTir("mat_leather_leggings");
//		e.addMatTir("mat_refined_leather_leggings");
//		e.addMatTir("mat_hardened_leather_leggings");
//		e.addMatTir("mat_composite_leather_leggings");
//		ammunitions.put(Equipments.LEGGINGS, e);
//
//		//boots
//		e = new SlotElement(36);
//		e.addComponent("againstfall");
//		e.addMatTir("");
//		e.addMatTir("mat_leather_boots");
//		e.addMatTir("mat_refined_leather_boots");
//		e.addMatTir("mat_hardened_leather_boots");
//		e.addMatTir("mat_composite_leather_boots");
//		ammunitions.put(Equipments.BOOTS, e);
//
//		//sword
//		e = new SlotElement(0);
//		e.addComponent("attack");
//		e.addComponent("fireaspect");
//		e.addMatTir("");
//		e.addMatTir("mat_iron_sword");
//		e.addMatTir("mat_steel_sword");
//		e.addMatTir("mat_carbide_steel_sword");
//		e.addMatTir("mat_tungsten_sword");
//		ammunitions.put(Equipments.SWORD, e);
//
//		//two
//		e = new SlotElement(40);
//		e.addComponent("infinite");
//		e.addComponent("arrowattack");
//		e.addMatTir("");
//		e.addMatTir("mat_hunting_bow");
//		e.addMatTir("mat_recurve_bow");
//		e.addMatTir("mat_longbow");
//		e.addMatTir("mat_marksmen_bow");
//		ammunitions.put(Equipments.TWOHAND, e);
	}
	public void addMissionBook(UnitCustomMaterial umat) {
		this.missionBooks.put(umat.getId(), umat);
		this.allowedSubslots.add(umat.getSocketSlot());
	}

//	@Override
//	public void onInteract(PlayerInteractEvent event) {
//		event.setCancelled(true);
//		Player player = event.getPlayer();
//		Resident resident = CivGlobal.getResident(player);
//		PlayerInventory inv = player.getInventory();
//
//		if (resident.isUnitActive()) {
//			//Деактивация юнита
//			removeChildren(inv);
//			CivMessage.send(player, CivColor.LightGreen + CivColor.BOLD + "Юнит деактивирован");
//			player.setLevel(0);
//			player.setExp(0);
//			resident.setUnitId(null);
//		} else {
//			//Активация юнита
//			ItemStack unitItemStack = event.getItem();
//			ArrayList<ItemStack> removes = new ArrayList<>(); //список предметов которые занимают нужные слоты
//			for (ConfigUnitsMaterial i : this.getConfigUnit().unitsMaterials.values()) {
//				ItemStack stack = inv.getItem(i.socketSlot);
//				if (stack != null) {
//					removes.add(stack);
//					inv.setItem(i.socketSlot, null);
//				}
//			}
//			int unitLevel = UnitStatic.getLevel(unitItemStack);
//			for (ConfigUnitsMaterial i : this.getConfigUnit().unitsMaterials.values()) {
//				if (i.isRequiredLevelValid(unitLevel)) {//Проверка уровня
//
//					CustomMaterial loreMat;
//					if (missionBooks.containsKey(i.lcm_id))
//						loreMat = missionBooks.get(i.lcm_id);
//					else
//						loreMat = CustomMaterial.getCustomMaterial(i.lcm_id);
//					ItemStack is = CustomMaterial.spawn(loreMat);
//					AttributeUtil attrs = new AttributeUtil(is);
//					attrs.setLore(loreMat.getLore());
//					is = attrs.getStack();
//					if (!inv.contains(is)) inv.setItem(i.socketSlot, is);
//				}
//			}
//			resident.setUnitId(this.getConfigUnit().id);
//			int level = UnitStatic.getLevel(unitItemStack);
//			player.setLevel(level);
//			//TODO
////			player.setExp((float) (UnitStatic.getExp(unitItemStack) / UnitStatic.totalExpLevel.get(level)));
//			CivMessage.send(player, CivColor.LightGreen + CivColor.BOLD + "Юнит активирован ");
//
//			// Try to re-add any removed items.
//			for (ItemStack is : removes) {
//				HashMap<Integer, ItemStack> leftovers = inv.addItem(is);
//				for (ItemStack s : leftovers.values())
//					player.getWorld().dropItem(player.getLocation(), s);
//			}
//		}
//	}

	@Override
	public void onItemToPlayer(Player player, ItemStack stack) {
	}

	@Override
	public void onPlayerDeath(EntityDeathEvent event, ItemStack stack) {

		Player player = (Player) event.getEntity();
		Resident resident = CivGlobal.getResident(player);
		if (resident == null || !resident.hasTown()) return;

		ArrayList<String> bookout = MissionLogger.getMissionLogs(resident.getTown());

		ItemStack book = new ItemStack(Material.WRITTEN_BOOK, 1);
		BookMeta meta = (BookMeta) book.getItemMeta();
		ArrayList<String> lore = new ArrayList<String>();
		lore.add("Mission Report");
		meta.setAuthor("Mission Reports");
		meta.setTitle("Missions From" + " " + resident.getTown().getName());

		String out = "";
		for (String str : bookout) {
			out += str + "\n";
		}
		BookUtil.paginate(meta, out);

		meta.setLore(lore);
		book.setItemMeta(meta);
		player.getWorld().dropItem(player.getLocation(), book);

	}

}
