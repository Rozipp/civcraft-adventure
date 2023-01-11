package ua.rozipp.gui.guiinventory;

import com.avrgaming.civcraft.exception.CivException;
import ua.rozipp.gui.GuiInventory;
import ua.rozipp.gui.GuiItem;
import com.avrgaming.civcraft.items.CustomMaterial;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.threading.tasks.DelayMoveInventoryItem;
import com.avrgaming.civcraft.units.UnitMaterial;
import com.avrgaming.civcraft.units.UnitObject;
import com.avrgaming.civcraft.units.UnitStatic;
import com.avrgaming.gpl.AttributeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.sql.Date;

public class UnitInventory extends GuiInventory {

    private final Town town;

    public UnitInventory(Player player, Town town) throws CivException {
        super(player, null, null);
        this.town = town;
        this.setRow(3);
        this.setTitle("Юниты города  " + town.getName());
    }

    public static ItemStack unitToNotActive(ItemStack stack) {
        stack.setType(Material.BEDROCK);
        int uId = UnitStatic.getUnitIdNBTTag(stack);
        UnitObject uo = CivGlobal.getUnitObject(uId);
        if (uo == null) return stack;
        AttributeUtil attrs = new AttributeUtil(stack);
        attrs.removeCivCraftProperty("GUI");
        attrs.removeCivCraftProperty("GUI_ACTION");
        attrs.setLore("");

        if (uo.getLastResident() != null) {
            attrs.addLore("Взял игрок " + uo.getLastResident().getName());
            attrs.addLore("Последний раз использован ");
            attrs.addLore(CivGlobal.dateFormat.format(new Date(uo.getLastActivate())));
        } else attrs.addLore("Юнит безследно изчез");
        return attrs.getStack();
    }

    @Override
    public Inventory getInventory(Player player) {
        if (inventory == null) {
            try {
                inventory = Bukkit.createInventory(getHolder(), size() + 1, getTitle());
                addLastItem(player.getUniqueId());
                for (Integer uId : town.unitInventory.getUnits()) {
                    inventory.addItem(UnitStatic.respawn(CivGlobal.getUnitObject(uId)));
                }
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack == null || stack.getType().equals(Material.AIR)) continue;
            int uId = UnitStatic.getUnitIdNBTTag(stack);
            UnitObject uo = CivGlobal.getUnitObject(uId);
            if (uo == null) continue;
            try {
                uo.validLastActivate();
            } catch (CivException e) {
                if (stack.getType() == Material.BEDROCK) {
                    inventory.setItem(slot, UnitStatic.respawn(uo));
                }
                continue;
            }
            inventory.setItem(slot, unitToNotActive(stack));
        }
        return inventory;
    }

    @Override
    public boolean onItemToInventory(Cancellable event, Player player, Inventory inv, ItemStack stack) {
        CustomMaterial cMat = CustomMaterial.getCustomMaterial(stack);
        if (cMat instanceof UnitMaterial) {
            int uId = UnitStatic.getUnitIdNBTTag(stack);
            int slot = -1;
            for (int i = 0; i < inv.getSize(); i++) {
                ItemStack is = inv.getItem(i);
                if (is == null || is.getType().equals(Material.AIR)) continue;
                if (UnitStatic.getUnitIdNBTTag(is) == uId) {
                    slot = i;
                    break;
                }
            }
            if (slot == -1) return true;
            DelayMoveInventoryItem.beginTaskRespawn(event, player, inv, stack, slot);
            stack.setAmount(0);
            UnitObject uo = CivGlobal.getUnitObject(uId);
            uo.setLastActivate(0);
            uo.setLastResident(null);
            player.updateInventory();
            return false;
        }
        return true;
    }

    @Override
    public boolean onItemFromInventory(Cancellable event, Player player, Inventory inv, ItemStack stack) {
        if (!GuiItem.isGUIItem(stack)) {
            int uId= UnitStatic.getUnitIdNBTTag(stack);
            UnitObject uo = CivGlobal.getUnitObject(uId);
            uo.used(CivGlobal.getResident(player));
            DelayMoveInventoryItem.beginTaskRespawn(event, player, inv, unitToNotActive(stack), inv.first(stack));
            ItemStack newStack = UnitStatic.respawn(uo);
            stack.setType(newStack.getType());
            stack.setItemMeta(newStack.getItemMeta());
            return false;
        } else {
            String action = GuiItem.getAction(stack);
            if (action != null) {
                GuiItem.processAction(action, stack, player);
                return true;
            }
        }
        return true;
    }
}
