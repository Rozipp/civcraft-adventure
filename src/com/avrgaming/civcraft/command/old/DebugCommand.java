/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.command.old;

import gpl.AttributeUtil;
import ua.rozipp.sound.ConfigSound;
import ua.rozipp.sound.SoundManager;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import com.avrgaming.civcraft.command.Commander;
import com.avrgaming.civcraft.command.CustomCommand;
import com.avrgaming.civcraft.command.MenuAbstractCommand;
import com.avrgaming.civcraft.command.Validators;
import com.avrgaming.civcraft.command.taber.AbstractCashedTaber;
import com.avrgaming.civcraft.command.taber.CivInWorldTaber;
import com.avrgaming.civcraft.command.taber.ResidentInWorldTaber;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigConstructInfo;
import com.avrgaming.civcraft.construct.ConstructSign;
import com.avrgaming.civcraft.construct.constructs.Template;
import com.avrgaming.civcraft.construct.structures.ArrowTower;
import com.avrgaming.civcraft.construct.structures.BuildableStatic;
import com.avrgaming.civcraft.construct.structures.Cityhall;
import com.avrgaming.civcraft.construct.structures.Structure;
import com.avrgaming.civcraft.construct.wonders.GrandShipIngermanland;
import com.avrgaming.civcraft.construct.wonders.Wonder;
import com.avrgaming.civcraft.exception.AlreadyRegisteredException;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.gui.GuiInventory;
import com.avrgaming.civcraft.interactive.BuildCallbackDbg;
import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.SimpleBlock;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.regions.CuboidRegion;

public class DebugCommand extends MenuAbstractCommand {

	public DebugCommand(String perentCommand) {
		super(perentCommand);
		displayName = "Debug";
		this.addValidator(Validators.validAdmin);
		
		add(new DebugInfoCommand("info").withDescription("Show data base info."));
		add(new DebugReloadConfCommand("reloadconf").withDescription("перезагрузка настроек из файла"));
		// add(new DebugCaveCommand("cave").withDescription("Дебаг пещер"));

		add(new CustomCommand("dupe").withDescription("duplicates the item in your hand.").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Player player = Commander.getPlayer(sender);
				if (player.getInventory().getItemInMainHand() == null || ItemManager.getTypeId(player.getInventory().getItemInMainHand()) == 0) throw new CivException("No item in hand.");
				player.getInventory().addItem(player.getInventory().getItemInMainHand());
				CivMessage.sendSuccess(player, player.getInventory().getItemInMainHand().getType().name() + "duplicated.");
			}
		}));
		add(new DebugTestCommand("test").withDescription("Run test suite commands."));
		add(new CustomCommand("sound").withDescription("[name] [pitch]").withTabCompleter(new AbstractCashedTaber() {
			@Override
			protected List<String> newTabList(String arg) {
				List<String> l = new ArrayList<>();
				for (ConfigSound configSound : SoundManager.sounds.values()) {
					String name = configSound.key;
					if (name.toLowerCase().startsWith(arg)) l.add(name);
				}
				return l;
			}
		}).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				final Player player = Commander.getPlayer(sender);
				if (args.length < 1) throw new CivException("Enter sound key from suond.yml");
				SoundManager.playSound(args[0], player.getLocation());
			}
		}));
		add(new CustomCommand("arrow").withDescription("[power] change arrow's power.").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				if (args.length < 2) {
					throw new CivException("/arrow [power]");
				}
				for (Town town : CivGlobal.getTowns()) {
					for (Structure struct : town.BM.getStructures()) {
						if (struct instanceof ArrowTower) {
							((ArrowTower) struct).setPower(Float.valueOf(args[1]));
						}
					}
					for (Wonder wonder : town.BM.getWonders()) {
						if (wonder instanceof GrandShipIngermanland) {
							((GrandShipIngermanland) wonder).setArrorPower(Float.valueOf(args[1]));
						}
					}
				}
			}
		}));
		add(new CustomCommand("unloadchunk").withDescription("[x] [z] - unloads this chunk.").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				if (args.length < 2) throw new CivException("Enter an x and z");
				Commander.getPlayer(sender).getWorld().unloadChunk(Integer.valueOf(args[0]), Integer.valueOf(args[1]));
				CivMessage.sendSuccess(sender, "unloaded.");
			}
		}));
		add(new CustomCommand("restoresigns").withDescription("restores all structure signs").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				CivMessage.send(sender, "restoring....");
				for (ConstructSign sign : CivGlobal.getConstructSigns()) {
					BlockCoord bcoord = sign.getCoord();
					Block block = bcoord.getBlock();
					ItemManager.setTypeId(block, CivData.WALL_SIGN);
					ItemManager.setData(block, sign.getDirection());

					Sign s = (Sign) block.getState();
					String[] lines = sign.getText().split("\n");
					if (lines.length > 0) s.setLine(0, lines[0]);
					if (lines.length > 1) s.setLine(1, lines[1]);
					if (lines.length > 2) s.setLine(2, lines[2]);
					if (lines.length > 3) s.setLine(3, lines[3]);
					s.update();
				}
			}
		}));
		
		add(new DebugFarmCommand("farm").withDescription("show debug commands for farms"));

		add(new CustomCommand("refreshchunk").withDescription("refreshes the chunk you're standing in.. for science.").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Player you = Commander.getPlayer(sender);
				ChunkCoord coord = new ChunkCoord(you.getLocation());
				for (Player player : Bukkit.getOnlinePlayers()) {
					player.getWorld().unloadChunk(coord.getX(), coord.getZ());
					player.getWorld().loadChunk(coord.getX(), coord.getZ());
				}
			}
		}));
		add(new CustomCommand("listconquered").withDescription("shows a list of conquered civilizations.").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				CivMessage.sendHeading(sender, "Conquered Civs");
				String out = "";
				for (Civilization civ : CivGlobal.getConqueredCivs()) {
					out += civ.getName() + ", ";
				}
				CivMessage.send(sender, out);
			}
		}));
		add(new CustomCommand("scout").withDescription("[civ] - enables debugging for scout towers in this civ.").withTabCompleter(new CivInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Civilization civ = Commander.getNamedCiv(args, 0);
				if (!civ.scoutDebug) {
					civ.scoutDebug = true;
					civ.scoutDebugPlayer = Commander.getPlayer(sender).getName();
					CivMessage.sendSuccess(sender, "Enabled scout tower debugging in " + civ.getName());
				} else {
					civ.scoutDebug = false;
					civ.scoutDebugPlayer = null;
					CivMessage.sendSuccess(sender, "Disabled scout tower debugging in " + civ.getName());
				}
			}
		}));
		add(new CustomCommand("showinv").withDescription("shows you an inventory").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				GuiInventory.openGuiInventory(Commander.getPlayer(sender), "GuiBook", null);
			}
		}));
		add(new CustomCommand("showcraftinv").withDescription("shows you crafting inventory").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				GuiInventory.openGuiInventory(Commander.getPlayer(sender), "CraftingHelp", null);
			}
		}));

		add(new CustomCommand("togglebookcheck").withDescription("Toggles checking for enchanted books on and off.").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				CivGlobal.checkForBooks = !CivGlobal.checkForBooks;
				CivMessage.sendSuccess(sender, "Check for books is:" + CivGlobal.checkForBooks);
			}
		}));
		add(new CustomCommand("colorme").withDescription("[hex] adds nbt color value to item held.").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Player player = Commander.getPlayer(sender);
				String hex = Commander.getNamedString(args, 0, "color code");
				long value = Long.decode(hex);
				ItemStack inHand = player.getInventory().getItemInMainHand();
				if (inHand == null || ItemManager.getTypeId(inHand) == CivData.AIR) throw new CivException("please have an item in your hand.");
				AttributeUtil attrs = new AttributeUtil(inHand);
				attrs.setColor(value);
				player.getInventory().setItemInMainHand(attrs.getStack());
				CivMessage.sendSuccess(player, "Set color.");
			}
		}));
		add(new CustomCommand("loadtemplate").withDescription("[name] [theme] tests out some new template stream code.").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Player player = Commander.getPlayer(sender);
				if (args.length < 1) {
					Commander.getResident(sender).setPendingCallback(new BuildCallbackDbg(player));
					return;
				}
				String name = Commander.getNamedString(args, 0, "Enter a buildName");
				String theme = "default";
				if (args.length == 2) theme = args[1];
				ConfigConstructInfo sinfo;

				sinfo = CivSettings.getConstructInfoByName(name);

				if (sinfo == null) throw new CivException(CivSettings.localize.localizedString("cmd_build_defaultUnknownStruct") + " " + name);
				String templatePath = Template.getTemplateFilePath(player.getLocation(), sinfo, theme);
				Template tpl = Template.getTemplate(templatePath);
				CivMessage.send(sender, "Loaded Template " + templatePath);
				if (tpl != null) tpl.buildTemplateDbg(new BlockCoord(player.getLocation()));
			}
		}));
		add(new CustomCommand("savetemplate").withDescription("[name] [theme] seve select region.").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Player player = Commander.getPlayer(sender);
				String name = Commander.getNamedString(args, 0, "Enter a filename");
				String build_name = name.replace("_", " ");
				String theme = (args.length == 3) ? args[2] : null;
				ConfigConstructInfo sinfo = CivSettings.getConstructInfoByName(build_name);

				String filepath = null;
				if (sinfo != null)
					filepath = Template.getTemplateFilePath(player.getLocation(), sinfo, theme);
				else
					filepath = "templates/" + name + ".def";

				LocalSession session = CivCraft.getWorldEdit().getSession(player);
				CuboidRegion selection;
				try {
					selection = (CuboidRegion) session.getSelection(session.getSelectionWorld());
				} catch (IncompleteRegionException e) {
					player.sendMessage(ChatColor.RED + "Регион не выделен");
					return;
				}
				Vector pos1 = selection.getPos1();
				Vector pos2 = selection.getPos2();
				Location loc = player.getLocation().clone();
				loc.setX(Math.min(pos1.getBlockX(), pos2.getBlockX()));
				loc.setY(Math.min(pos1.getBlockY(), pos2.getBlockY()));
				loc.setZ(Math.min(pos1.getBlockZ(), pos2.getBlockZ()));

				Template tpl = new Template();
				tpl.getBlocksWithWorld(loc, selection.getWidth(), selection.getHeight(), selection.getLength(), filepath);

				File templateFile = new File(tpl.filepath);
				if (templateFile.exists()) {
					File newFile = templateFile;
					int i = 1;
					while (newFile.exists()) {
						newFile = new File(tpl.filepath + ".bk" + (++i));
					}
					CivMessage.sendError(sender, "File " + filepath + " существует. Переименовываю файл в " + tpl.filepath + ".bk" + i);
					templateFile.renameTo(newFile);
				}

				try {
					tpl.saveTemplate();
				} catch (CivException | IOException e) {
					CivMessage.sendError(player, e.getMessage());
					e.printStackTrace();
				}
				CivMessage.send(player, "Template \"" + tpl.getFilepath() + " \" is saved");
			}
		}));
		add(new CustomCommand("buildspawn").withDescription("[civname] [capitolname] Builds spawn from spawn template.").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				/* First create a new Civilization and spawn capitol */
				String civName = Commander.getNamedString(args, 0, "Enter a Civ name/");
				String capitolName = Commander.getNamedString(args, 1, "Enter a capitol name.");
				Resident resident = Commander.getResident(sender);

				try {
					/* Build a spawn civ. */
					Civilization civ = new Civilization();
					civ.setName(civName);
					civ.setTag(civName);
					civ.saveNow();

					/* Build a spawn capitol */
					Town capitol = new Town(civ);
					capitol.setName(capitolName);
					capitol.saveNow();

					// Create permission groups for civs.
					civ.GM.init();
					civ.GM.addLeader(resident);

					capitol.GM.init();
					try {
						capitol.addResident(resident);
					} catch (AlreadyRegisteredException e) {}
					capitol.GM.addMayor(resident);

					civ.addTown(capitol);
					civ.setCapitolId(capitol.getId());
					civ.setAdminCiv(true);
					civ.save();

					capitol.setCiv(civ);
					capitol.save();
					resident.save();

					CivGlobal.addTown(capitol);
					CivGlobal.addCiv(civ);

					class BuildSpawnTask implements Runnable {
						CommandSender sender;
						int start_x;
						int start_y;
						int start_z;
						Town spawnCapitol;

						public BuildSpawnTask(CommandSender sender, int x, int y, int z, Town capitol) {
							this.sender = sender;
							this.start_x = x;
							this.start_y = y;
							this.start_z = z;
							this.spawnCapitol = capitol;
						}

						@Override
						public void run() {
							try {
								/* Initialize the spawn template */
								Template tpl = Template.getTemplate("templates/spawn.def");
								if (tpl == null) throw new CivException("IO Error.");

								Player player = (Player) sender;
								Location center = BuildableStatic.repositionCenterStatic(player.getLocation(), 0, tpl);
								BlockCoord corner = new BlockCoord(center);

								CivMessage.send(sender, "Building from " + start_x + "," + start_y + "," + start_z);
								for (int y = start_y; y < tpl.size_y; y++) {
									for (SimpleBlock sb : tpl.blocks.get(y)) {
										BlockCoord next = corner.getRelative(sb.getX(), y, sb.getZ());

										if (sb.specialType.equals(SimpleBlock.SimpleType.COMMAND)) {
											String buildableName = sb.command.replace("/", "");

											ConfigConstructInfo info = CivSettings.getConstructInfoByName(buildableName);
											if (info == null) {
												try {
													Block block = next.getBlock();
													ItemManager.setTypeIdAndData(block, CivData.AIR, 0, false);
													continue;
												} catch (Exception e) {
													e.printStackTrace();
													continue;
												}
											}

											CivMessage.send(sender, "Setting up " + buildableName);
											int yShift = 0;
											String lines[] = sb.getKeyValueString().split(",");
											String split[] = lines[0].split(":");
											String dir = split[0];
											yShift = Integer.valueOf(split[1]);

											Location loc = next.getLocation();
											loc.setY(loc.getY() + yShift);

											Structure struct = Structure.newStructure(player, loc, info.id, spawnCapitol, false);
											if (struct instanceof Cityhall) {
												AdminTownCommand.claimradius(spawnCapitol, center, 15);
											}
											struct.setTemplate(Template.getTemplate(Template.getTemplateFilePath(info.template_name, dir, null)));
											struct.setComplete(true);
											struct.setHitpoints(info.max_hitpoints);
											CivGlobal.addStructure(struct);
											spawnCapitol.BM.addStructure(struct);
											struct.postBuild();
											struct.save();
											spawnCapitol.save();
										} else
											if (sb.specialType.equals(SimpleBlock.SimpleType.LITERAL)) {
												try {
													Block block = next.getBlock();
													ItemManager.setTypeIdAndData(block, sb.getType(), sb.getData(), false);

													Sign s = (Sign) block.getState();
													for (int j = 0; j < 4; j++) {
														s.setLine(j, sb.message[j]);
													}

													s.update();
												} catch (Exception e) {
													e.printStackTrace();
												}
											} else {
												try {
													Block block = next.getBlock();
													ItemManager.setTypeIdAndData(block, sb.getType(), sb.getData(), false);
												} catch (Exception e) {
													e.printStackTrace();
												}
											}
									}
								}

								CivMessage.send(sender, "Finished building.");

								spawnCapitol.SM.addCulture(60000000);
								spawnCapitol.save();

							} catch (CivException e) {
								e.printStackTrace();
								CivMessage.send(sender, e.getMessage());
							}
						}
					}

					TaskMaster.syncTask(new BuildSpawnTask(sender, 0, 0, 0, capitol));
				} catch (InvalidNameException e) {
					throw new CivException(e.getMessage());
				} catch (SQLException e) {
					e.printStackTrace();
					throw new CivException("Internal DB Error.");
				}
			}
		}));
		add(new CustomCommand("datebypass").withDescription("Bypasses certain date restrictions").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				CivGlobal.debugDateBypass = !CivGlobal.debugDateBypass;
				CivMessage.send(sender, "Date bypass is now:" + CivGlobal.debugDateBypass);
			}
		}));
		add(new CustomCommand("skull").withDescription("[player] [title]").withTabCompleter(new ResidentInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Player player = Commander.getPlayer(sender);
				Resident resident = Commander.getNamedResident(args, 0);
				String message = Commander.getNamedString(args, 1, "Enter a title.");
				ItemStack skull = ItemManager.spawnPlayerHead(resident.getName(), message);
				player.getInventory().addItem(skull);
				CivMessage.sendSuccess(player, "Added skull item.");
			}
		}));
		add(new CustomCommand("packet").withDescription("sends custom auth packet.").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Player player = Commander.getPlayer(sender);
				player.sendPluginMessage(CivCraft.getPlugin(), "CAC", "Test Message".getBytes());
				CivMessage.sendSuccess(player, "Sent test message");
			}
		}));
		add(new CustomCommand("disablemap").withDescription("disables zan's minimap").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Player player = Commander.getPlayer(sender);
				player.sendMessage("§3§6§3§6§3§6§e");
				player.sendMessage("§3§6§3§6§3§6§d");
				CivMessage.sendSuccess(player, "Disabled.");
			}
		}));
		add(new DebugWorldCommand("world").withDescription("Show world debug options"));
		add(new CustomCommand("saveinv").withDescription("save an inventory").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Resident resident = Commander.getResident(sender);
				resident.saveInventory();
				CivMessage.sendSuccess(resident, "saved inventory.");
			}
		}));
		add(new CustomCommand("restoreinv").withDescription("restore your inventory.").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Resident resident = Commander.getResident(sender);
				resident.restoreInventory();
				CivMessage.sendSuccess(resident, "restore inventory.");
			}
		}));
	}

	// public void culturechunk_cmd() {
	// Player player = Commander.getPlayer(sender);
	// CultureChunk cc = CivGlobal.getCultureChunk(player.getLocation());
	// if (cc == null) {
	// CivMessage.send(sender, "No culture chunk found here.");
	// return;
	// }
	// CivMessage.send(sender, "loc:" + cc.getChunkCoord() + " town:" + cc.getTown().getName() + " civ:" + cc.getCiv().getName() + "
	// distanceToNearest:" + cc.getDistanceToNearestEdge(cc.getTown().savedEdgeBlocks));
	// }

}
