package ua.rozipp.gui;

import com.avrgaming.civcraft.main.CivLog;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class GuiAction {

	public static HashMap<String, GuiAction> guiActions = new HashMap<>();

	public GuiAction(){
		String name = this.getClass().getSimpleName();
		CivLog.debug("CREATED GuiAction " + name);
		guiActions.put(name, this);
	}

	public void performAction(Player player, ItemStack stack){}
}
