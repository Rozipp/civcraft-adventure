/*************************************************************************
 * 
 * AVRGAMING LLC
 * __________________
 * 
 *  [2013] AVRGAMING LLC
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of AVRGAMING LLC and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to AVRGAMING LLC
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from AVRGAMING LLC.
 */
package com.avrgaming.civcraft.command.menu;

import java.sql.SQLException;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.command.Commander;
import com.avrgaming.civcraft.command.CustomCommand;
import com.avrgaming.civcraft.command.MenuAbstractCommand;
import com.avrgaming.civcraft.command.Validators;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;

public class EconCommand extends MenuAbstractCommand {

	public EconCommand(String perentComman) {
		super(perentComman);
		this.setAliases("money");
		displayName = CivSettings.localize.localizedString("cmd_econ_Name");

		add(new CustomCommand("help").withDescription(CivSettings.localize.localizedString("cmd_econ_addDesc")).withValidator(Validators.validEcon).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				showBasicHelp(sender);
			}
		}));
		add(new CustomCommand("add").withDescription(CivSettings.localize.localizedString("cmd_econ_addDesc")).withValidator(Validators.validEcon).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				if (args.length < 2) throw new CivException(CivSettings.localize.localizedString("cmd_econ_ProvideNameAndNumberPrompt"));
				Resident resident = Commander.getNamedResident(args, 0);
				try {
					Double amount = Double.valueOf(args[1]);
					resident.getTreasury().deposit(amount);
					CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_econ_added", args[1], CivSettings.CURRENCY_NAME, resident.getName()));
				} catch (NumberFormatException e) {
					throw new CivException(args[1] + " " + CivSettings.localize.localizedString("cmd_enterNumerError"));
				}
			}
		}));
		add(new CustomCommand("give").withDescription(CivSettings.localize.localizedString("cmd_econ_giveDesc")).withValidator(Validators.validEcon).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				if (args.length < 2) throw new CivException(CivSettings.localize.localizedString("cmd_econ_ProvideNameAndNumberPrompt"));
				Resident resident = Commander.getNamedResident(args, 0);
				try {
					Double amount = Double.valueOf(args[1]);
					resident.getTreasury().deposit(amount);
					CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_econ_give", args[1], CivSettings.CURRENCY_NAME, args[0]));
				} catch (NumberFormatException e) {
					throw new CivException(args[1] + " " + CivSettings.localize.localizedString("cmd_enterNumerError"));
				}
			}
		}));
		add(new CustomCommand("set").withDescription(CivSettings.localize.localizedString("cmd_econ_setDesc")).withValidator(Validators.validEcon).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				if (args.length < 2) throw new CivException(CivSettings.localize.localizedString("cmd_econ_ProvideNameAndNumberPrompt"));
				Resident resident = Commander.getNamedResident(args, 0);
				try {
					Double amount = Double.valueOf(args[1]);
					resident.getTreasury().setBalance(amount);
					CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_econ_set", args[0], args[1], CivSettings.CURRENCY_NAME));
				} catch (NumberFormatException e) {
					throw new CivException(args[1] + " " + CivSettings.localize.localizedString("cmd_enterNumerError"));
				}
			}
		}));
		add(new CustomCommand("sub").withDescription(CivSettings.localize.localizedString("cmd_econ_subDesc")).withValidator(Validators.validEcon).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				if (args.length < 2) throw new CivException(CivSettings.localize.localizedString("cmd_econ_ProvideNameAndNumberPrompt"));
				Resident resident = Commander.getNamedResident(args, 0);
				try {
					Double amount = Double.valueOf(args[1]);
					resident.getTreasury().withdraw(amount);
					CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_econ_Subtracted", args[1], CivSettings.CURRENCY_NAME, args[0]));
				} catch (NumberFormatException e) {
					throw new CivException(args[1] + " " + CivSettings.localize.localizedString("cmd_enterNumerError"));
				}
			}
		}));

		add(new CustomCommand("addtown").withDescription(CivSettings.localize.localizedString("cmd_econ_addtownDesc")).withValidator(Validators.validEcon).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				if (args.length < 2) throw new CivException(CivSettings.localize.localizedString("cmd_econ_ProvideNameAndNumberPrompt"));
				Town town = Commander.getNamedTown(args, 0);
				try {
					Double amount = Double.valueOf(args[1]);
					town.getTreasury().deposit(amount);
					CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_econ_added", args[1], CivSettings.CURRENCY_NAME, args[0]));
				} catch (NumberFormatException e) {
					throw new CivException(args[1] + " " + CivSettings.localize.localizedString("cmd_enterNumerError"));
				}
			}
		}));
		add(new CustomCommand("settown").withDescription(CivSettings.localize.localizedString("cmd_econ_settownDesc")).withValidator(Validators.validEcon).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				if (args.length < 2) throw new CivException(CivSettings.localize.localizedString("cmd_econ_ProvideNameAndNumberPrompt"));
				Town town = Commander.getNamedTown(args, 0);
				try {
					Double amount = Double.valueOf(args[1]);
					town.getTreasury().setBalance(amount);
					CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_econ_set", args[0], args[1], CivSettings.CURRENCY_NAME));
				} catch (NumberFormatException e) {
					throw new CivException(args[1] + " " + CivSettings.localize.localizedString("cmd_enterNumerError"));
				}
			}
		}));
		add(new CustomCommand("subtown").withDescription(CivSettings.localize.localizedString("cmd_econ_subtownDesc")).withValidator(Validators.validEcon).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				if (args.length < 2) throw new CivException(CivSettings.localize.localizedString("cmd_econ_ProvideNameAndNumberPrompt"));
				Town town = Commander.getNamedTown(args, 0);
				try {
					Double amount = Double.valueOf(args[1]);
					town.getTreasury().withdraw(amount);
					CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_econ_Subtracted", args[1], CivSettings.CURRENCY_NAME, args[0]));
				} catch (NumberFormatException e) {
					throw new CivException(args[1] + " " + CivSettings.localize.localizedString("cmd_enterNumerError"));
				}
			}
		}));

		add(new CustomCommand("addciv").withDescription(CivSettings.localize.localizedString("cmd_econ_addcivDesc")).withValidator(Validators.validEcon).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				if (args.length < 2) throw new CivException(CivSettings.localize.localizedString("cmd_econ_ProvideNameAndNumberPrompt"));
				Civilization civ = Commander.getNamedCiv(args, 0);
				try {
					Double amount = Double.valueOf(args[1]);
					civ.getTreasury().deposit(amount);
					CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_econ_added", args[1], CivSettings.CURRENCY_NAME, args[0]));
				} catch (NumberFormatException e) {
					throw new CivException(args[1] + " " + CivSettings.localize.localizedString("cmd_enterNumerError"));
				}
			}
		}));
		add(new CustomCommand("setciv").withDescription(CivSettings.localize.localizedString("cmd_econ_setcivDesc")).withValidator(Validators.validEcon).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				if (args.length < 2) throw new CivException(CivSettings.localize.localizedString("cmd_econ_ProvideNameAndNumberPrompt"));
				Civilization civ = Commander.getNamedCiv(args, 0);
				try {
					Double amount = Double.valueOf(args[1]);
					civ.getTreasury().setBalance(amount);
					CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_econ_set", args[0], args[1], CivSettings.CURRENCY_NAME));
				} catch (NumberFormatException e) {
					throw new CivException(args[1] + " " + CivSettings.localize.localizedString("cmd_enterNumerError"));
				}
			}
		}));
		add(new CustomCommand("subciv").withDescription(CivSettings.localize.localizedString("cmd_econ_subcivDesc")).withValidator(Validators.validEcon).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				if (args.length < 2) 
					throw new CivException(CivSettings.localize.localizedString("cmd_econ_ProvideNameAndNumberPrompt"));
				Civilization civ = Commander.getNamedCiv(args,0);
				try {
					Double amount = Double.valueOf(args[1]);
					civ.getTreasury().withdraw(amount);
					CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_econ_Subtracted", args[1], CivSettings.CURRENCY_NAME, args[0]));
				} catch (NumberFormatException e) {
					throw new CivException(args[1] + " " + CivSettings.localize.localizedString("cmd_enterNumerError"));
				}
			}
		}));

		add(new CustomCommand("setdebt").withDescription(CivSettings.localize.localizedString("cmd_econ_setdebtDesc")).withValidator(Validators.validEcon).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Resident resident = Commander.getNamedResident(args,0);
				Double amount = Commander.getNamedDouble(args,1);
				resident.getTreasury().setDebt(amount);
				resident.save();

				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("SetSuccess"));
			}
		}));
		add(new CustomCommand("setdebttown").withDescription(CivSettings.localize.localizedString("cmd_econ_setdebttownDesc")).withValidator(Validators.validEcon).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getNamedTown(args,0);
				Double amount = Commander.getNamedDouble(args,1);
				town.getTreasury().setDebt(amount);
				town.save();

				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("SetSuccess"));
			}
		}));
		add(new CustomCommand("setdebtciv").withDescription(CivSettings.localize.localizedString("cmd_econ_setdebtcivDesc")).withValidator(Validators.validEcon).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Civilization civ = Commander.getNamedCiv(args,0);
				Double amount = Commander.getNamedDouble(args,1);
				civ.getTreasury().setDebt(amount);
				civ.save();

				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("SetSuccess"));
			}
		}));

		add(new CustomCommand("clearalldebt").withDescription(CivSettings.localize.localizedString("cmd_econ_clearAllDebtDesc")).withValidator(Validators.validEcon).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				for (Civilization civ : CivGlobal.getCivs()) {
					civ.getTreasury().setDebt(0);
					try {
						civ.saveNow();
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
				for (Town town : CivGlobal.getTowns()) {
					town.getTreasury().setDebt(0);
					try {
						town.saveNow();
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
				for (Resident res : CivGlobal.getResidents()) {
					res.getTreasury().setDebt(0);
					try {
						res.saveNow();
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
				CivMessage.send(sender, CivSettings.localize.localizedString("cmd_econ_clearedAllDebtSuccess"));
			}
		}));
	}

	@Override
	public void doDefaultAction(CommandSender sender) throws CivException {
		Player player = Commander.getPlayer(sender);
		Resident resident = CivGlobal.getResident(player);
		if (resident == null) return;
		CivMessage.sendSuccess(player, resident.getTreasury().getBalance() + " " + CivSettings.CURRENCY_NAME);
	}

}
