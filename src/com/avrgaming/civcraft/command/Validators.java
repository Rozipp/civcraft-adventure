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

/**
 * <p>
 * Ххранит в себе статические Validator-ы
 * </p>
 * @author rozipp */
public class Validators {

	/** Разрешает выполнение админских команд связынных.*/
	public static Validator validAdmin = new ValidAdmin();
	/** Разрешает выполнение команд связынных с созданием или удалением валюты. Обычно это модераторы и админы */
	public static Validator validEcon = new ValidEcon();
	/** Разрешает команду всем, кто находиться в городе */
	public static Validator validHasTown = new ValidHasTown();
	/** Разрешает команду мерам города */
	public static Validator validMayor = new ValidMayor();
	/** Разрешаеть команду мерам и асистентам города */
	public static Validator validMayorAssistant = new ValidMayorAssistant();
	/** Разрешаеть команду мерам асистентам города а так же лидерам цивилизации */
	public static Validator validMayorAssistantLeader = new ValidMayorAssistantLeader();
	/** Разрешаеть команду лидерам цивилизации */
	public static Validator validLeader = new ValidLeader();
	/** Разрешаеть команду лидерам цивилизации и их помощникам */
	public static Validator validLeaderAdvisor = new ValidLeaderAdvisor();
	/** Разрешаеть команду хозяивам чанка. Если у чанка нет хозяина, то хозяином считаеться мер или ассистент города */
	public static Validator validPlotOwner = new ValidPlotOwner();
	/** Разрешаеть команду игрокам, чей город был окупирован */
	public static Validator ValidMotherCiv = new ValidMotherCiv();
	/** Разрешаеть команду игрокам, которые состоят в кемпе */
	public static Validator validHasCamp = new ValidHasCamp();
	/** Разрешаеть команду хозяивам кемпов */
	public static Validator validCampOwner = new ValidCampOwner();

	private static class ValidAdmin implements Validator {
		@Override
		public void isValide(CommandSender sender) throws CivException {
			if (!(sender instanceof ConsoleCommandSender)) {
				Player player = Commander.getPlayer(sender);
				if (!player.isOp() || !player.hasPermission(CivSettings.MINI_ADMIN) || !player.hasPermission(CivSettings.MODERATOR)) {
					throw new CivException(CivSettings.localize.localizedString("cmd_MustBeOP"));
				}
			}
		}
	}
	
	private static class ValidEcon implements Validator {
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

	private static class ValidHasTown implements Validator {
		@Override
		public void isValide(CommandSender sender) throws CivException {
			Resident resident = Commander.getResident(sender);
			if (resident.getTown() == null) throw new CivException(CivSettings.localize.localizedString("cmd_notPartOfTown"));
		}
	}

	private static class ValidMayor implements Validator {
		@Override
		public void isValide(CommandSender sender) throws CivException {
			Resident resident = Commander.getResident(sender);
			Town town = Commander.getSelectedTown(sender);
			if (!town.GM.isMayor(resident)) throw new CivException(CivSettings.localize.localizedString("cmd_MustBeMayor"));
		}
	}

	private static class ValidMayorAssistant implements Validator {
		@Override
		public void isValide(CommandSender sender) throws CivException {
			Resident resident = Commander.getResident(sender);
			Town town = Commander.getSelectedTown(sender);
			if (!town.GM.isMayorOrAssistant(resident)) throw new CivException(CivSettings.localize.localizedString("cmd_NeedHigherTownOrCivRank"));
		}
	}

	private static class ValidMayorAssistantLeader implements Validator {
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

	private static class ValidLeaderAdvisor implements Validator {
		@Override
		public void isValide(CommandSender sender) throws CivException {
			Resident res = Commander.getResident(sender);
			Civilization civ = Commander.getSenderCiv(sender);
			if (!civ.GM.isLeaderOrAdviser(res)) throw new CivException(CivSettings.localize.localizedString("cmd_NeedHigherCivRank"));
		}
	}

	private static class ValidLeader implements Validator {
		@Override
		public void isValide(CommandSender sender) throws CivException {
			Resident res = Commander.getResident(sender);
			Civilization civ = Commander.getSenderCiv(sender);
			if (!civ.GM.isLeader(res)) throw new CivException(CivSettings.localize.localizedString("cmd_NeedHigherCivRank2"));
		}
	}

	private static class ValidMotherCiv implements Validator {
		@Override
		public void isValide(CommandSender sender) throws CivException {
			Resident res = Commander.getResident(sender);
			if (res.getTown() == null) throw new CivException(CivSettings.localize.localizedString("cmd_notPartOfTown"));
			;
			if (res.getTown().getMotherCiv() == null) throw new CivException("cmd_civ_notHaveMotherCiv");
		}
	}

	private static class ValidPlotOwner implements Validator {
		@Override
		public void isValide(CommandSender sender) throws CivException {
			Resident resident = Commander.getResident(sender);
			TownChunk tc = Commander.getStandingTownChunk(sender);

			if (tc.perms.getOwner() == null) {
				Validators.validMayorAssistant.isValide(sender);
			} else {
				if (resident != tc.perms.getOwner()) throw new CivException(CivSettings.localize.localizedString("cmd_validPlotOwnerFalse2"));
			}
		}
	}

	private static class ValidHasCamp implements Validator {
		@Override
		public void isValide(CommandSender sender) throws CivException {
			Resident resident = Commander.getResident(sender);
			if (!resident.hasCamp()) throw new CivException(CivSettings.localize.localizedString("cmd_campBase_NotIncamp"));
		}
	}

	private static class ValidCampOwner implements Validator {
		@Override
		public void isValide(CommandSender sender) throws CivException {
			Resident resident = Commander.getResident(sender);
			if (!resident.hasCamp()) throw new CivException(CivSettings.localize.localizedString("cmd_campBase_NotIncamp"));
			if (resident.getCamp().getSQLOwner() != resident) throw new CivException(CivSettings.localize.localizedString("cmd_campBase_NotOwner") + " (" + resident.getCamp().getOwnerName() + ")");
		}
	}

	public interface Validator {
		public abstract void isValide(CommandSender sender) throws CivException;
	}
}
