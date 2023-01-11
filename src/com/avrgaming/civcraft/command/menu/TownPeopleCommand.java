package com.avrgaming.civcraft.command.menu;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.command.Commander;
import com.avrgaming.civcraft.command.CustomCommand;
import com.avrgaming.civcraft.command.MenuAbstractCommand;
import com.avrgaming.civcraft.command.taber.AbstractCashedTaber;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.object.TownPeoplesManager.Prof;

public class TownPeopleCommand extends MenuAbstractCommand {

	public TownPeopleCommand(String perentCommand) {
		super(perentCommand);

		add(new CustomCommand("addprof").withDescription("Показать таблицу дохода от каждого професионала").withTabCompleter(new AbstractCashedTaber() {
			@Override
			protected List<String> newTabList(String arg) {
				List<String> l = new ArrayList<>();
				for (Prof prof : Prof.values()) {
					if (prof.name().toLowerCase().startsWith(arg)) l.add(prof.name());
				}
				return l;
			}
		}).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				try {
					Prof prof = Prof.valueOf(args[0]);
					town.PM.hirePeoples(prof, Integer.parseInt(args[1]));
				} catch (Exception e) {
					throw new CivException(e.getMessage());
				}
			}
		}));
		add(new CustomCommand("removeprof").withDescription("Показать таблицу дохода от каждого професионала").withTabCompleter(new AbstractCashedTaber() {
			@Override
			protected List<String> newTabList(String arg) {
				List<String> l = new ArrayList<>();
				for (Prof prof : Prof.values()) {
					if (prof.name().toLowerCase().startsWith(arg)) l.add(prof.name());
				}
				return l;
			}
		}).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				try {
					Prof prof = Prof.valueOf(args[0]);
					town.PM.dismissPeoples(prof, Integer.parseInt(args[1]));
				} catch (Exception e) {
					throw new CivException(e.getMessage());
				}
			}
		}));
		
		add(new CustomCommand("showintake").withDescription("Показать таблицу дохода от каждого професионала").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				town.PM.intakeTable.showIntakeTable(sender);
			}
		}));

		add(new CustomCommand("whowork").withDescription("Показать таблицу дохода от каждого професионала").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				for (Prof prof : Prof.values()) {
					CivMessage.send(sender, "" + prof.toString() + " : " + town.PM.getPeoplesWorker(prof));
				}
			}
		}));
	}

}
