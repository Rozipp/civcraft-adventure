package com.avrgaming.civcraft.comm.Resident;

import java.util.HashMap;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.comm.AbstractSubCommand;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.items.CraftableCustomMaterial;
import com.avrgaming.civcraft.main.CivMessage;

public class Resident_book extends AbstractSubCommand {

	public Resident_book() {
		super("resident book");
	}

	@Override
	public List<String> onTabComplete(CommandSender arg0, Command arg1, String arg2, String[] arg3) {
		return null;
	}

	@Override
	public void onComm(CommandSender arg0, Command arg1, String arg2, String[] arg3) throws CivException {
		Player player = getPlayer();

		/* Determine if he already has the book. */
		for (ItemStack stack : player.getInventory().getContents()) {
			if (stack == null) continue;

			CraftableCustomMaterial craftMat = CraftableCustomMaterial.getCraftableCustomMaterial(stack);
			if (craftMat == null) continue;

			if (craftMat.getConfigId().equals("mat_tutorial_book")) {
				throw new CivException(CivSettings.localize.localizedString("cmd_res_bookHaveOne"));
			}
		}

		CraftableCustomMaterial craftMat = CraftableCustomMaterial.getCraftableCustomMaterial("mat_tutorial_book");
		ItemStack helpBook = CraftableCustomMaterial.spawn(craftMat);

		HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(helpBook);
		if (leftovers != null && leftovers.size() >= 1) {
			throw new CivException(CivSettings.localize.localizedString("cmd_res_bookInvenFull"));
		}
		CivMessage.sendSuccess(player, CivSettings.localize.localizedString("cmd_res_bookSuccess"));
	}

	@Override
	public void doDefaultAction() throws CivException {
		// TODO Автоматически созданная заглушка метода

	}

	@Override
	public void showHelp() {
		// TODO Автоматически созданная заглушка метода

	}

	@Override
	public void permissionCheck() throws CivException {
		// TODO Автоматически созданная заглушка метода

	}

}
