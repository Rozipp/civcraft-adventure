package com.avrgaming.civcraft.command;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.object.TownChunk;

public class Validators {

	public static Validator validEcon = new ValidEcon();
	public static Validator validHasTown = new ValidHasTown();
	public static Validator validMayor = new ValidMayor();
	public static Validator validMayorAssistant = new ValidMayorAssistant();
	public static Validator validMayorAssistantLeader = new ValidMayorAssistantLeader();
	public static Validator validLeader = new ValidLeader();
	public static Validator validLeaderAdvisor = new ValidLeaderAdvisor();
	public static Validator validPlotOwner = new ValidPlotOwner();
	public static Validator ValidMotherCiv = new ValidMotherCiv();
	public static Validator validHasCamp = new ValidHasCamp();
	public static Validator validCampOwner = new ValidCampOwner();

	private static class ValidEcon extends Validator {
		@Override
		public void isValide(CommandSender sender) throws CivException {
			if (!(sender instanceof ConsoleCommandSender)) {
				Player player = Commander.getPlayer(sender);
				if (!player.isOp() || !player.hasPermission(CivSettings.ECON)) {
					throw new CivException(CivSettings.localize.localizedString("cmd_MustBeOP"));
				}
			}
		}
	}

	private static class ValidHasTown extends Validator {
		@Override
		public void isValide(CommandSender sender) throws CivException {
			Resident resident = Commander.getResident(sender);
			if (resident.getTown() == null) throw new CivException(CivSettings.localize.localizedString("cmd_notPartOfTown"));
		}
	}
	
	private static class ValidMayor extends Validator {
		@Override
		public void isValide(CommandSender sender) throws CivException {
			Resident resident = Commander.getResident(sender);
			Town town = Commander.getSelectedTown(sender);
			if (!town.GM.isMayor(resident)) throw new CivException(CivSettings.localize.localizedString("cmd_MustBeMayor"));
		}
	}
	
	private static class ValidMayorAssistant extends Validator {
		@Override
		public void isValide(CommandSender sender) throws CivException {
			Resident resident = Commander.getResident(sender);
			Town town = Commander.getSelectedTown(sender);
			if (!town.GM.isMayorOrAssistant(resident)) throw new CivException(CivSettings.localize.localizedString("cmd_NeedHigherTownOrCivRank"));
		}
	}
	
	private static class ValidMayorAssistantLeader extends Validator {
		@Override
		public void isValide(CommandSender sender) throws CivException {
			Resident resident = Commander.getResident(sender);
			Town town = Commander.getSelectedTown(sender);
			Civilization civ;

			/* If we're using a selected town that isn't ours validate based on the mother civ. */
			if (town.getMotherCiv() != null)
				civ = town.getMotherCiv();
			else
				civ = Commander.getSenderCiv(sender);

			if (!town.GM.isMayorOrAssistant(resident) && !civ.GM.isLeader(resident)) throw new CivException(CivSettings.localize.localizedString("cmd_NeedHigherTownOrCivRank"));
		}
	}

	private static class ValidLeaderAdvisor extends Validator {
		@Override
		public void isValide(CommandSender sender) throws CivException {
			Resident res = Commander.getResident(sender);
			Civilization civ = Commander.getSenderCiv(sender);
			if (!civ.GM.isLeaderOrAdviser(res)) throw new CivException(CivSettings.localize.localizedString("cmd_NeedHigherCivRank"));
		}
	}

	private static class ValidLeader extends Validator {
		@Override
		public void isValide(CommandSender sender) throws CivException {
			Resident res = Commander.getResident(sender);
			Civilization civ = Commander.getSenderCiv(sender);
			if (!civ.GM.isLeader(res)) throw new CivException(CivSettings.localize.localizedString("cmd_NeedHigherCivRank2"));
		}
	}
	
	private static class ValidMotherCiv extends Validator {
		@Override
		public void isValide(CommandSender sender) throws CivException {
			Resident res = Commander.getResident(sender);
			if (res.getTown() == null) throw new CivException(CivSettings.localize.localizedString("cmd_notPartOfTown"));;
			if (res.getTown().getMotherCiv() == null) throw new CivException("cmd_civ_notHaveMotherCiv");
		}
	}

	private static class ValidPlotOwner extends Validator {
		@Override
		public void isValide(CommandSender sender) throws CivException {
			Resident resident = Commander.getResident(sender);
			TownChunk tc = Commander.getStandingTownChunk(sender);

			if (tc.perms.getOwner() == null) {
				Validators.validMayorAssistantLeader.isValide(sender);
				if (tc.getTown() != resident.getTown()) throw new CivException(CivSettings.localize.localizedString("cmd_validPlotOwnerFalse"));
			} else {
				if (resident != tc.perms.getOwner()) throw new CivException(CivSettings.localize.localizedString("cmd_validPlotOwnerFalse2"));
			}
		}
	}

	private static class ValidHasCamp extends Validator {
		@Override
		public void isValide(CommandSender sender) throws CivException {
			Resident resident = Commander.getResident(sender);
			if (!resident.hasCamp()) throw new CivException(CivSettings.localize.localizedString("cmd_campBase_NotIncamp"));
		}
	}
	
	private static class ValidCampOwner extends Validator {
		@Override
		public void isValide(CommandSender sender) throws CivException {
			Resident resident = Commander.getResident(sender);
			if (!resident.hasCamp()) throw new CivException(CivSettings.localize.localizedString("cmd_campBase_NotIncamp"));
			if (resident.getCamp().getSQLOwner() != resident) throw new CivException(CivSettings.localize.localizedString("cmd_campBase_NotOwner") + " (" + resident.getCamp().getOwnerName() + ")");
		}
	}
	
}
