package com.avrgaming.civcraft.command.menu;

import java.text.SimpleDateFormat;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.command.Commander;
import com.avrgaming.civcraft.command.CustomCommand;
import com.avrgaming.civcraft.command.MenuAbstractCommand;
import com.avrgaming.civcraft.command.Validators;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.randomevents.RandomEvent;
import com.avrgaming.civcraft.util.CivColor;

public class TownEventCommand extends MenuAbstractCommand {

	public TownEventCommand(String perentComman) {
		super(perentComman);
		displayName = CivSettings.localize.localizedString("cmd_town_event_name");
		add(new CustomCommand("show").withDescription(CivSettings.localize.localizedString("cmd_town_event_showDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				RandomEvent event = town.getActiveEvent();
				if (event == null) {
					CivMessage.sendError(sender, CivSettings.localize.localizedString("cmd_town_event_activateNone"));
				} else {
					SimpleDateFormat sdf = CivGlobal.dateFormat;
					CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_town_event_showCurrent") + " " + event.configRandomEvent.name);
					CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("cmd_town_event_showStarted") + " " + CivColor.LightGreen + sdf.format(event.getStartDate()));
					CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("cmd_town_event_showEnd") + " " + CivColor.LightGreen + sdf.format(event.getEndDate()));
					if (event.isActive()) {
						CivMessage.send(sender, CivColor.LightGray + CivSettings.localize.localizedString("cmd_town_event_showActive"));
					} else {
						CivMessage.send(sender, CivColor.Yellow + CivSettings.localize.localizedString("cmd_town_event_showInactive"));
					}
					CivMessage.send(sender, CivColor.Green + "-- " + CivSettings.localize.localizedString("cmd_town_event_showMessageHeading") + " ---");
					CivMessage.send(sender, CivColor.LightGray + event.getMessages());
				}
			}
		}));
		add(new CustomCommand("activate").withDescription(CivSettings.localize.localizedString("cmd_town_event_activateDesc")).withValidator(Validators.validMayorAssistant).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				RandomEvent event = town.getActiveEvent();
				if (event == null) {
					CivMessage.sendError(sender, CivSettings.localize.localizedString("cmd_town_event_activateNone"));
				} else {
					event.activate();
					CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_town_event_activateSuccess"));
				}
			}
		}));
	}
}
