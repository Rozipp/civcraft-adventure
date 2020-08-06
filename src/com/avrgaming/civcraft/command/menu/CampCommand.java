package com.avrgaming.civcraft.command.menu;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.command.Commander;
import com.avrgaming.civcraft.command.CustomCommand;
import com.avrgaming.civcraft.command.MenuAbstractCommand;
import com.avrgaming.civcraft.command.Validators;
import com.avrgaming.civcraft.command.taber.AbstractTaber;
import com.avrgaming.civcraft.command.taber.ResidentInWorldTaber;
import com.avrgaming.civcraft.command.taber.ResidentInCampTaber;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigCampUpgrade;
import com.avrgaming.civcraft.construct.constructs.Camp;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.questions.JoinCampResponse;
import com.avrgaming.civcraft.questions.Question;
import com.avrgaming.civcraft.threading.sync.TeleportPlayerTaskCamp;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;

public class CampCommand extends MenuAbstractCommand {
	public static final long INVITE_TIMEOUT = 30000; // 30 seconds

	public CampCommand(String perentComman) {
		super(perentComman);
		this.setDescription("description " + CivSettings.localize.localizedString("Camp"));
		this.displayName = CivSettings.localize.localizedString("Camp");
		this.addValidator(Validators.validHasCamp);

		add(new CustomCommand("undo").withDescription(CivSettings.localize.localizedString("cmd_camp_undoDesc")).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Player player = Commander.getPlayer(sender);
				Resident resident = Commander.getResident(sender);
				if (!resident.hasCamp()) throw new CivException(CivSettings.localize.localizedString("cmd_campBase_NotIncamp"));
				Camp camp = resident.getCamp();
				if (camp.getSQLOwner() != resident) throw new CivException(CivSettings.localize.localizedString("cmd_camp_undoNotOwner"));
				if (!camp.isUndoable()) throw new CivException(CivSettings.localize.localizedString("cmd_camp_undoTooLate"));
				HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(ItemManager.createItemStack("mat_found_camp", 1));
				for (ItemStack stack : leftovers.values()) {
					player.getWorld().dropItem(player.getLocation(), stack);
					CivMessage.send(sender, CivColor.LightGray + CivSettings.localize.localizedString("cmd_camp_undoFullInven"));
				}
				camp.undo();
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_camp_undoSuccess"));
			}
		}));
		add(new CustomCommand("add").withAliases("a").withDescription(CivSettings.localize.localizedString("cmd_camp_addDesc")).withValidator(Validators.validCampOwner).withTabCompleter(new ResidentInWorldTaber())
				.withExecutor(new CustonExecutor() {
					@Override
					public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
						Camp camp = Commander.getCurrentCamp(sender);
						Resident resident = Commander.getNamedResident(args, 0);
						Player player = Commander.getPlayer(sender);
						if (resident.hasCamp()) throw new CivException(CivSettings.localize.localizedString("cmd_camp_addIncamp"));
						if (resident.hasTown()) throw new CivException(CivSettings.localize.localizedString("cmd_camp_addInTown"));

						JoinCampResponse join = new JoinCampResponse();
						join.camp = camp;
						join.resident = resident;
						join.sender = player;
						Question.questionPlayer(player, CivGlobal.getPlayer(resident), CivSettings.localize.localizedString("var_cmd_camp_addInvite", player.getName(), camp.getName()), INVITE_TIMEOUT, join);
						CivMessage.sendSuccess(player, CivSettings.localize.localizedString("var_cmd_camp_addSuccess", resident.getName()));
					}
				}));
		add(new CustomCommand("remove").withAliases("r").withDescription(CivSettings.localize.localizedString("cmd_camp_removeDesc")).withValidator(Validators.validCampOwner).withTabCompleter(new ResidentInCampTaber())
				.withExecutor(new CustonExecutor() {
					@Override
					public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
						Camp camp = Commander.getCurrentCamp(sender);
						Resident resident = Commander.getNamedResident(args, 0);
						if (!resident.hasCamp() || resident.getCamp() != camp) throw new CivException(CivSettings.localize.localizedString("var_cmd_camp_removeNotIncamp", resident.getName()));
						if (resident.getCamp().getSQLOwner() == resident) throw new CivException(CivSettings.localize.localizedString("cmd_camp_removeErrorOwner"));
						camp.removeMember(resident);
						CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_camp_removeSuccess", resident.getName()));
					}
				}));
		add(new CustomCommand("leave").withDescription(CivSettings.localize.localizedString("cmd_camp_leaveDesc")).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Resident resident = Commander.getResident(sender);
				if (!resident.hasCamp()) throw new CivException(CivSettings.localize.localizedString("cmd_campBase_NotIncamp"));
				Camp camp = resident.getCamp();
				if (camp.getSQLOwner() == resident) throw new CivException(CivSettings.localize.localizedString("cmd_camp_leaveOwner"));
				camp.removeMember(resident);
				camp.save();
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_camp_leaveSuccess", camp.getName()));
			}
		}));
		add(new CustomCommand("setowner").withDescription(CivSettings.localize.localizedString("cmd_camp_setownerDesc")).withTabCompleter(new ResidentInCampTaber()).withValidator(Validators.validCampOwner)
				.withExecutor(new CustonExecutor() {
					@Override
					public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
						Camp camp = Commander.getCurrentCamp(sender);
						Resident newLeader = Commander.getNamedResident(args, 0);
						if (!camp.hasMember(newLeader.getName())) throw new CivException(CivSettings.localize.localizedString("var_cmd_camp_removeNotIncamp", newLeader.getName()));
						camp.setSQLOwner(newLeader);
						camp.save();
						CivMessage.sendSuccess(newLeader, CivSettings.localize.localizedString("var_cmd_camp_setownerMsg", camp.getName()));
						CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_camp_setownerSuccess", newLeader.getName()));
					}
				}));
		add(new CustomCommand("info").withAliases("i").withDescription(CivSettings.localize.localizedString("cmd_camp_infoDesc")).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Camp camp = Commander.getCurrentCamp(sender);
				SimpleDateFormat sdf = CivGlobal.dateFormat;

				CivMessage.sendHeading(sender, CivSettings.localize.localizedString("var_camp_infoHeading", camp.getName()));
				HashMap<String, String> info = new HashMap<String, String>();
				info.put(CivSettings.localize.localizedString("Owner"), camp.getOwnerName());
				info.put(CivSettings.localize.localizedString("Members"), "" + camp.getMembers().size());
				info.put(CivSettings.localize.localizedString("NextRaid"), "" + sdf.format(camp.getNextRaidDate()));
				CivMessage.send(sender, Commander.makeInfoString(info, CivColor.Green, CivColor.LightGreen));

				info.clear();
				info.put(CivSettings.localize.localizedString("cmd_camp_infoFireLeft"), "" + camp.getFirepoints());
				info.put(CivSettings.localize.localizedString("cmd_camp_infoLonghouseLevel"), "" + camp.getLonghouseLevel() + "" + camp.getLonghouseCountString());
				CivMessage.send(sender, Commander.makeInfoString(info, CivColor.Green, CivColor.LightGreen));

				info.clear();
				info.put(CivSettings.localize.localizedString("Members"), camp.getMembersString());
				CivMessage.send(sender, Commander.makeInfoString(info, CivColor.Green, CivColor.LightGreen));
			}
		}));
		add(new CustomCommand("disband").withDescription(CivSettings.localize.localizedString("cmd_camp_disbandDesc")).withValidator(Validators.validCampOwner).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Camp camp = Commander.getCurrentCamp(sender);
				camp.disband();
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_camp_disbandSuccess"));
			}
		}));

		add(new upgradeCampMenu("upgrade").withDescription(CivSettings.localize.localizedString("cmd_camp_upgradeDesc")).withValidator(Validators.validCampOwner));
		add(new CustomCommand("refresh").withDescription(CivSettings.localize.localizedString("cmd_camp_refreshDesc")).withValidator(Validators.validCampOwner).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Resident resident = Commander.getResident(sender);
				Camp camp = resident.getCamp();
				Date now = new Date();
				int buildable_refresh_cooldown;
				try {
					buildable_refresh_cooldown = CivSettings.getInteger(CivSettings.townConfig, "town.buildable_refresh_cooldown");
				} catch (InvalidConfiguration e) {
					e.printStackTrace();
					throw new CivException(CivSettings.localize.localizedString("internalCommandException"));
				}
				if (camp.isDestroyed()) throw new CivException(CivSettings.localize.localizedString("cmd_camp_refreshDestroyed"));
				if (camp.lastBuildableRefresh != null && now.getTime() < camp.lastBuildableRefresh.getTime() + (buildable_refresh_cooldown * 60 * 1000)) {
					throw new CivException(CivSettings.localize.localizedString("var_town_refresh_wait1", buildable_refresh_cooldown));
				}
				camp.lastBuildableRefresh = now;
				camp.repairFromTemplate();
				CivMessage.send(sender, CivSettings.localize.localizedString("cmd_camp_refreshSuccess"));
				resident.setNextRefresh(now.getTime() + (buildable_refresh_cooldown * 60 * 1000));
			}
		}));
		add(new CustomCommand("location").withDescription(CivSettings.localize.localizedString("cmd_camp_locationDesc")).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Resident resident = Commander.getResident(sender);
				if (!resident.hasCamp()) throw new CivException(CivSettings.localize.localizedString("cmd_campBase_NotIncamp"));
				Camp camp = resident.getCamp();
				if (camp != null) {
					CivMessage.send(sender, "");
					CivMessage.send(sender, CivColor.LightGreen + CivColor.BOLD + CivSettings.localize.localizedString("cmd_camp_locationSuccess") + " " + CivColor.LightPurple + camp.getCorner());
					CivMessage.send(sender, "");
				}
			}
		}));
		add(new CustomCommand("teleport").withAliases("tp").withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Resident resident = Commander.getResident(sender);
				CivGlobal.dateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));
				if (!resident.hasCamp()) throw new CivException("§c" + CivSettings.localize.localizedString("cmd_campBase_NotIncamp"));
				Camp camp = resident.getCamp();
				long nextTeleport = resident.getNextTeleport();
				long timeNow = Calendar.getInstance().getTimeInMillis();
				if (nextTeleport > timeNow) throw new CivException("§c" + CivSettings.localize.localizedString("cmd_camp_teleport_cooldown", CivGlobal.dateFormat.format(nextTeleport)));
				final Location toTeleport = camp.getCenterLocation();
				final TeleportPlayerTaskCamp teleportPlayerTaskCamp = new TeleportPlayerTaskCamp(resident, Commander.getPlayer(sender), toTeleport, resident.getCamp());
				teleportPlayerTaskCamp.run(true);
			}
		}));
		add(new CustomCommand("chat").withAliases("c").withDescription(CivSettings.localize.localizedString("cmd_camp_chatDesc")).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Bukkit.dispatchCommand(sender, "vcc");
			}
		}));
	}

	private static void list_upgrades(CommandSender sender) throws CivException {
		Camp camp = Commander.getCurrentCamp(sender);
		for (ConfigCampUpgrade upgrade : CivSettings.campUpgrades.values()) {
			if (upgrade.isAvailable(camp)) {
				CivMessage.send(sender, upgrade.name + " " + CivColor.LightGray + CivSettings.localize.localizedString("Cost") + " " + CivColor.Yellow + upgrade.cost);
			}
		}
	}

	private class upgradeCampMenu extends MenuAbstractCommand {

		public upgradeCampMenu(String perentComman) {
			super(perentComman);
			add(new CustomCommand("list").withDescription(CivSettings.localize.localizedString("cmd_camp_upgrade_listDesc")).withExecutor(new CustonExecutor() {
				@Override
				public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
					CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_camp_upgrade_list"));
					list_upgrades(sender);
				}
			}));
			add(new CustomCommand("purchased").withDescription(CivSettings.localize.localizedString("cmd_camp_upgrade_purchasedDesc")).withExecutor(new CustonExecutor() {
				@Override
				public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
					Camp camp = Commander.getCurrentCamp(sender);
					CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_camp_upgrade_purchasedSuccess"));
					String out = "";
					for (ConfigCampUpgrade upgrade : camp.getUpgrades()) {
						out += upgrade.name + ", ";
					}
					CivMessage.send(sender, out);
				}
			}));
			add(new CustomCommand("buy").withDescription(CivSettings.localize.localizedString("cmd_camp_upgrade_buyDesc")).withTabCompleter(new AbstractTaber() {
				@Override
				public List<String> getTabList(CommandSender sender, String arg) throws CivException {
					List<String> l = new ArrayList<>();
					Camp camp = Commander.getCurrentCamp(sender);
					for (ConfigCampUpgrade upgrade : CivSettings.campUpgrades.values()) {
						String s = upgrade.name.toLowerCase().replace(" ", "_");
						if (upgrade.isAvailable(camp) && s.startsWith(arg)) l.add(s);
					}
					return l;
				}
			}).withExecutor(new CustonExecutor() {
				@Override
				public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
					Camp camp = Commander.getCurrentCamp(sender);
					if (args.length < 1) {
						CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_camp_upgrade_list"));
						list_upgrades(sender);
						CivMessage.send(sender, CivSettings.localize.localizedString("cmd_camp_upgrade_buyHeading"));
						return;
					}
					String combinedArgs = Commander.combineArgs(args);
					combinedArgs = combinedArgs.trim().replace("_", " ");
					ConfigCampUpgrade upgrade = CivSettings.getCampUpgradeByNameRegex(camp, combinedArgs);
					if (upgrade == null) throw new CivException(CivSettings.localize.localizedString("var_cmd_camp_upgrade_buyInvalid", combinedArgs));
					if (camp.hasUpgrade(upgrade.id)) throw new CivException(CivSettings.localize.localizedString("cmd_camp_upgrade_buyOwned"));
					camp.purchaseUpgrade(upgrade);
					CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_camp_upgrade_buySuccess", upgrade.name));
				}
			}));
		}
	}
}
