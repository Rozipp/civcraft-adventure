package com.avrgaming.civcraft.util;

import java.util.HashMap;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.material.MaterialData;

import com.avrgaming.civcraft.items.CustomMaterial;
import com.avrgaming.civcraft.main.CivLog;

import net.minecraft.server.v1_12_R1.NBTTagCompound;
import net.minecraft.server.v1_12_R1.NBTTagString;

/* The ItemManager class is going to be used to wrap itemstack operations that have now been deprecated by Bukkit. If bukkit ever actually takes these methods
 * away from us, we'll just have to use NMS or be a little creative. Doing it on spot (here) will be better than having fragile code scattered everywhere.
 * 
 * Additionally it gives us an opportunity to unit test certain item operations that we want to use with our new custom item stacks. */

public class ItemManager {

	public static ItemStack createItemStack(int typeId, int amount) {
		return createItemStack(typeId, (short) 0, amount);
	}

	@SuppressWarnings("deprecation")
	public static ItemStack createItemStack(int typeId, short data, int amount) {
		if (data < 0) return new ItemStack(typeId, amount, (short) 0);
		return new ItemStack(typeId, amount, data);
	}

	public static ItemStack createItemStack(String umid, int amount) {
		CustomMaterial lcm = CustomMaterial.getCustomMaterial(umid.toLowerCase());
		if (lcm != null) return CustomMaterial.spawn(lcm, amount);
		if (umid.contains(":")) {
			String[] spl = umid.split(":");
			try {
				return createItemStack(Integer.parseInt(spl[0]), Short.parseShort(spl[1]), amount);
			} catch (NumberFormatException e) {
				CivLog.warning("createItemStack() \"" + umid + "\" is not found material");
				return null;
			}
		}
		try {
			return createItemStack(Integer.parseInt(umid), amount);
		} catch (NumberFormatException e) {
			try {
				return new ItemStack(Material.valueOf(umid.toUpperCase()));
			} catch (IllegalArgumentException e1) {
				CivLog.warning("createItemStack() \"" + umid + "\" is not found material");
				return null;
			}
		}
	}

	@SuppressWarnings("deprecation")
	public static MaterialData getMaterialData(int type_id, int data) {
		return new MaterialData(type_id, (byte) data);
	}

	@SuppressWarnings("deprecation")
	public static Enchantment getEnchantById(int id) {
		return Enchantment.getById(id);
	}

	@SuppressWarnings("deprecation")
	public static int getMaterialId(Material material) {
		return material.getId();
	}
	@SuppressWarnings("deprecation")
	public static int getEnchantmentId(Enchantment e) {
		return e.getId();
	}

	@SuppressWarnings("deprecation")
	public static int getTypeId(ItemStack stack) {
		return stack.getTypeId();
	}
	@SuppressWarnings("deprecation")
	public static int getTypeId(Block block) {
		return block.getTypeId();
	}
	@SuppressWarnings("deprecation")
	public static void setTypeId(Block block, int typeId) {
		block.setTypeId(typeId);
	}
	@SuppressWarnings("deprecation")
	public static void setTypeId(BlockState block, int typeId) {
		block.setTypeId(typeId);
	}
	@SuppressWarnings("deprecation")
	public static byte getData(Block block) {
		return block.getData();
	}
	@SuppressWarnings("deprecation")
	public static short getData(ItemStack stack) {
		return stack.getData().getData();
	}
	@SuppressWarnings("deprecation")
	public static byte getData(MaterialData data) {
		return data.getData();
	}
	@SuppressWarnings("deprecation")
	public static byte getData(BlockState state) {
		return state.getRawData();
	}
	@SuppressWarnings("deprecation")
	public static void setData(Block block, int data) {
		block.setData((byte) data);
	}
	@SuppressWarnings("deprecation")
	public static void setData(Block block, int data, boolean update) {
		block.setData((byte) data, update);
	}

	public static void setDurability(ItemStack stack, int demage) {
		stack.setDurability((short) demage);
	}
	public static void setDurability(ItemStack stack, double percent) {
		short demage = (short) ((1 - percent) * stack.getType().getMaxDurability());
		stack.setDurability(demage);
	}
	public static short getDurability(ItemStack stack) {
		return stack.getDurability();
	}

	@SuppressWarnings("deprecation")
	public static Material getMaterial(int material) {
		return Material.getMaterial(material);
	}

	@SuppressWarnings("deprecation")
	public static int getBlockTypeId(ChunkSnapshot snapshot, int x, int y, int z) {
		return snapshot.getBlockTypeId(x, y, z);
	}
	@SuppressWarnings("deprecation")
	public static int getBlockData(ChunkSnapshot snapshot, int x, int y, int z) {
		return snapshot.getBlockData(x, y, z);
	}
	@SuppressWarnings("deprecation")
	public static void sendBlockChange(Player player, Location loc, int type, int data) {
		player.sendBlockChange(loc, type, (byte) data);
	}
	@SuppressWarnings("deprecation")
	public static int getBlockTypeIdAt(World world, int x, int y, int z) {
		return world.getBlockTypeIdAt(x, y, z);
	}
	@SuppressWarnings("deprecation")
	public static int getTypeId(BlockState newState) {
		return newState.getTypeId();
	}
	@SuppressWarnings("deprecation")
	public static void setTypeIdAndData(Block block, int type, int data, boolean update) {
		block.setTypeIdAndData(type, (byte) data, update);
	}

	@SuppressWarnings("deprecation")
	public static short getEntityTypeId(EntityType entity) {
		return entity.getTypeId();
	}

	@SuppressWarnings("deprecation")
	public static ItemStack spawnPlayerHead(String playerName, String itemDisplayName) {
		ItemStack skull = ItemManager.createItemStack(ItemManager.getMaterialId(Material.SKULL_ITEM), (short) 3, 1);
		SkullMeta meta = (SkullMeta) skull.getItemMeta();
		meta.setOwner(playerName);
		meta.setDisplayName(itemDisplayName);
		skull.setItemMeta(meta);
		return skull;
	}

	public static boolean removeItemFromPlayer(Player player, Material mat, int amount) {
		ItemStack m = new ItemStack(mat, amount);
		if (player.getInventory().contains(mat)) {
			player.getInventory().removeItem(m);
			player.updateInventory();
			return true;
		}
		return false;
	}

	/** @Возвращает информацию из NBTTag'а под ключем key */
	public static String getProperty(ItemStack stack, String key) {
		net.minecraft.server.v1_12_R1.ItemStack nmsStack = CraftItemStack.asNMSCopy(stack);
		if (nmsStack != null && nmsStack.getTag() != null) {
			NBTTagCompound civcraftCompound = nmsStack.getTag().getCompound("civcraft");
			if (civcraftCompound != null) {
				NBTTagString strTag = (NBTTagString) civcraftCompound.get(key);
				if (strTag != null) return strTag.toString().replace("\"", "");
			}
		}
		return null;
	}
	/** Возвращает все NBTTag'ы предмета */
	public static String getAllProperty(ItemStack stack) {
		net.minecraft.server.v1_12_R1.ItemStack nmsStack = CraftItemStack.asNMSCopy(stack);
		if (nmsStack != null && nmsStack.getTag() != null) {
			return nmsStack.getTag().toString();
		}
		return null;
	}
	/** Добавляет в NBTTag информацию value под ключом key */
	public static ItemStack setProperty(ItemStack stack, String key, String value) {
		net.minecraft.server.v1_12_R1.ItemStack nmsStack = CraftItemStack.asNMSCopy(stack);
		if (nmsStack == null) return stack;
		if (nmsStack.getTag() == null) nmsStack.setTag(new NBTTagCompound());
		NBTTagCompound civcraftCompound = nmsStack.getTag().getCompound("civcraft");
		if (civcraftCompound == null) civcraftCompound = new NBTTagCompound();
		civcraftCompound.set(key, new NBTTagString(value));
		nmsStack.getTag().set("civcraft", civcraftCompound);
		return CraftItemStack.asCraftMirror(nmsStack);
	}

	/** Сравнивает ItemStack с umid предмета umid может быть: 1) число_тип:число_дата предмета (пример 15:0) 2) mid CustomMaterial 3) Число тип 4) название
	 * материала (например Material.AIR) */
	public static boolean isCorrectItemStack(ItemStack stack, String umid) {
		if (umid.contains(":")) {
			String[] spl = umid.split(":");
			try {
				return isCorrectItemStack(stack, null, Integer.parseInt(spl[0]), Short.parseShort(spl[1]));
			} catch (NumberFormatException e) {
				CivLog.warning("isCorrectItemStack() \"" + umid + "\" is not material");
				return false;
			}
		}
		try {
			Integer typeId = Integer.parseInt(umid);
			return isCorrectItemStack(stack, null, typeId, (short) 0);
		} catch (NumberFormatException e) {
			try {
				if (CustomMaterial.isCustomMaterial(stack)) return isCorrectItemStack(stack, umid, 0, (short) 0);
				return stack.getType().equals(Material.valueOf(umid.toUpperCase()));
			} catch (IllegalArgumentException e1) {
				return false;
			}
		}
	}
	public static boolean isCorrectItemStack(ItemStack stack, String mid, int type, short data) {
		if (stack == null) return false;
		if (mid != null) {
			/* Looking for a custom id. */
			String gMID = CustomMaterial.getMID(stack);
			if (gMID.isEmpty()) return false;
			return gMID.equals(mid.toLowerCase());
		} else {
			if (ItemManager.getTypeId(stack) != type) return false;
			/* Only check item data when max dura == 0. Otherwise item doesnt use data and it's the durability. */
			return (ItemManager.getMaterial(type).getMaxDurability() == 0) && (ItemManager.getData(stack) == data); /* data didn't match, wrong item. */
		}
	}

	public static void removeCustomItemFromInventory(Inventory inv, String mat, Integer amount) {
		for (int i = inv.getSize() - 1; i >= 0; i--) {
			ItemStack stack = inv.getItem(i);
			if (stack == null) continue;
			if (isCorrectItemStack(stack, mat)) {
				Integer count = stack.getAmount();

				stack.setAmount(count - amount);
				inv.setItem(i, stack);

			}
		}
	}

	public static String getUMid(ItemStack stack) {
		if (stack == null) return "";
		String ss = CustomMaterial.getMID(stack);
		if (!ss.isEmpty()) return ss;
		return getTypeId(stack) + ":" + getData(stack);
	}

	/** Ложит предмет в заданный слот. Если тот не пустой выбрасывает содержимое на пол. Moves an item stack off of this slot by trying to re-add it to the
	 * inventory, if it fails, then we drop it on the ground. */
	public static void putItemToSlot(Player player, Inventory inv, int slot, ItemStack newItem) {
		ItemStack stack = inv.getItem(slot);
		inv.setItem(slot, newItem);

		if (stack != null) {
			if (stack.equals(newItem)) return;
			HashMap<Integer, ItemStack> leftovers = inv.addItem(stack);
			for (ItemStack s : leftovers.values()) {
				player.getWorld().dropItem(player.getLocation(), s);
			}
		}
	}
}
