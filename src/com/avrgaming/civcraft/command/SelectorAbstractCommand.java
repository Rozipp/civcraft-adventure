package com.avrgaming.civcraft.command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.command.taber.AbstractCashedTaber;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivMessage;

/** Клас псевдо меню. Отличаеться от меню тем, что у него не подкоманды, а набор статических аргументов
 * @author rozipp */
public abstract class SelectorAbstractCommand extends CustomCommand {
	private List<String> selectorCommands = new ArrayList<>();
	private List<String> selectorDescriptions = new ArrayList<>();

	public SelectorAbstractCommand(String string_cmd, CustonExecutor executor) {
		super(string_cmd);
		initSubCommands();
		this.withTabCompleter(new SelectorCustomTaber());
		this.withExecutor(new SelectorCustomExecutor(executor));
	}

	public abstract void initSubCommands();

	public void add(String string_cmd, String description) {
		selectorCommands.add(string_cmd);
		selectorDescriptions.add(description);
	}

	private class SelectorCustomExecutor implements CustonExecutor {
		private CustonExecutor executor;

		public SelectorCustomExecutor(CustonExecutor executor) {
			this.executor = executor;
		}

		@Override
		public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
			if (args.length < 1) {
				for (int i = 0; i < selectorCommands.size(); i++) {
					CivMessage.send(sender, "(" + i + ") " + selectorCommands.get(i) + "   " + selectorDescriptions.get(i));
				}
				return;
			}
			executor.run(sender, cmd, label, args);
		}
	}

	public class SelectorCustomTaber extends AbstractCashedTaber {

		@Override
		protected List<String> newTabList(String arg) {
			List<String> l = new ArrayList<>();
			for (String s : selectorCommands) {
				if (s.startsWith(arg)) l.add(s);
			}
			return l;
		}
	}
}