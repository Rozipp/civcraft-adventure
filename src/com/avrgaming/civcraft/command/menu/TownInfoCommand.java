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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.command.Commander;
import com.avrgaming.civcraft.command.CustomCommand;
import com.avrgaming.civcraft.command.MenuAbstractCommand;
import com.avrgaming.civcraft.command.Validators;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigCultureLevel;
import com.avrgaming.civcraft.construct.Buildable;
import com.avrgaming.civcraft.construct.structures.Bank;
import com.avrgaming.civcraft.construct.structures.Cottage;
import com.avrgaming.civcraft.construct.structures.Mine;
import com.avrgaming.civcraft.construct.structures.Structure;
import com.avrgaming.civcraft.construct.structures.Temple;
import com.avrgaming.civcraft.construct.structures.TradeShip;
import com.avrgaming.civcraft.construct.wonders.Wonder;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.AttrSource;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.CultureChunk;
import com.avrgaming.civcraft.object.Relation;
import com.avrgaming.civcraft.object.Relation.Status;
import com.avrgaming.civcraft.object.TownPeoplesManager.Prof;
import com.avrgaming.civcraft.object.TownStorageManager.StorageType;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CivColor;

public class TownInfoCommand extends MenuAbstractCommand {

	public TownInfoCommand(String perentComman) {
		super(perentComman);
		displayName = CivSettings.localize.localizedString("cmd_town_info_name");
		withValidator(Validators.validHasTown);

		add(new CustomCommand("help").withDescription("Вивести все возможные подкоманды").withExecutor(new CustomExecutor() {

			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				showBasicHelp(sender);
			}
		}));
		add(new CustomCommand("upkeep").withDescription(CivSettings.localize.localizedString("cmd_town_info_upkeepDesc")).withValidator(Validators.validMayorAssistantLeader).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				CivMessage.sendHeading(sender, town.getName() + " " + CivSettings.localize.localizedString("cmd_town_info_upkeepHeading"));
				CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("baseUpkeep") + " " + CivColor.LightGreen + town.getBaseUpkeep());
				CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("StructureUpkeep") + " " + CivColor.LightGreen + town.getStructureUpkeep());
				try {
					CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("Subtotal") + " " + CivColor.LightGreen + town.getTotalUpkeep() + CivColor.Green + " "
							+ CivSettings.localize.localizedString("cmd_civ_gov_infoUpkeep") + " " + CivColor.LightGreen + town.getGovernment().upkeep_rate);
				} catch (InvalidConfiguration e) {
					e.printStackTrace();
					throw new CivException(CivSettings.localize.localizedString("internalException"));
				}
				CivMessage.send(sender, CivColor.LightGray + "---------------------------------");
				try {
					CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("Total") + " " + CivColor.LightGreen + town.getTotalUpkeep() * town.getCiv().getGovernment().upkeep_rate);
				} catch (InvalidConfiguration e) {
					e.printStackTrace();
					throw new CivException(CivSettings.localize.localizedString("internalException"));
				}
			}
		}));
		add(new CustomCommand("cottage").withDescription(CivSettings.localize.localizedString("cmd_town_info_cottageDesc")).withValidator(Validators.validMayorAssistantLeader).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				ArrayList<String> out = new ArrayList<>();
				DecimalFormat df = new DecimalFormat("#,###.#");
				CivMessage.sendHeading(sender, town.getName() + " " + CivSettings.localize.localizedString("cmd_town_info_cottageHeading"));
				double total = 0;
				for (Structure struct : town.BM.getStructures()) {
					if (!struct.getConfigId().equals("ti_cottage")) continue;
					Cottage cottage = (Cottage) struct;
					String color;
					color = (struct.isActive()) ? CivColor.LightGreen : CivColor.Rose;
					double coins = cottage.getCoinsGenerated();
					if (town.getCiv().hasTechnologys("tech_taxation")) {
						double taxation_bonus;
						try {
							taxation_bonus = CivSettings.getDouble(CivSettings.techsConfig, "taxation_cottage_buff");
							coins *= taxation_bonus;
						} catch (InvalidConfiguration e) {
							e.printStackTrace();
						}
					}

					if (!struct.isDestroyed()) {
						out.add(color + "Cottage (" + struct.getCorner() + ")");
						out.add(CivColor.Green + "    " + CivSettings.localize.localizedString("Level") + " " + CivColor.Yellow + cottage.getLevel() + CivColor.Green + " " + CivSettings.localize.localizedString("count") + " "
								+ CivColor.Yellow + "(" + cottage.getCount() + "/" + cottage.getMaxCount() + ")");
						out.add(CivColor.Green + "   " + CivSettings.localize.localizedString("base") + " " + CivSettings.CURRENCY_NAME + ": " + CivColor.Yellow + coins + CivColor.Green + " "
								+ CivSettings.localize.localizedString("LastResult") + " " + CivColor.Yellow + cottage.getLastResult().name());
					} else {
						out.add(color + "Cottage" + " (" + struct.getCorner() + ")");
						out.add(CivColor.Rose + "    " + CivSettings.localize.localizedString("DESTROYED"));
					}
					total += coins;
				}
				out.add(CivColor.Green + "----------------------------");
				out.add(CivColor.Green + CivSettings.localize.localizedString("SubTotal") + " " + CivColor.Yellow + total);
				out.add(CivColor.Green + CivSettings.localize.localizedString("cmd_civ_gov_infoCottage") + " " + CivColor.Yellow + df.format(town.getCottageRate() * 100) + "%");
				if (town.getBuffManager().hasBuff("buff_pyramid_cottage_bonus")) {
					out.add("§2" + CivSettings.localize.localizedString("cmd_town_bonusCottage_pyramid",
							new StringBuilder().append("§a").append(Math.round((town.getBuffManager().getEffectiveDouble("buff_pyramid_cottage_bonus") - 1.0) * 100.0)).toString()));
				}
				if (town.getBuffManager().hasBuff("buff_hotel"))
					out.add("§2" + CivSettings.localize.localizedString("cmd_town_bonusCottage_hotel", new StringBuilder().append("§a").append(Math.round((town.getBuffManager().getEffectiveDouble("buff_hotel") - 1.0) * 100.0)).toString()));
				if (town.getCiv().getStockExchangeLevel() >= 1) out.add("§2" + CivSettings.localize.localizedString("cmd_town_bonusCottage_stockExchange", "§a30%", String.valueOf(town.getCiv().getStockExchangeLevel())));
				total *= town.getCottageRate();
				out.add(CivColor.Green + CivSettings.localize.localizedString("Total") + " " + CivColor.Yellow + df.format(total) + " " + CivSettings.CURRENCY_NAME);
				CivMessage.send(sender, out);
			}
		}));
		add(new CustomCommand("temple").withDescription(CivSettings.localize.localizedString("cmd_town_info_templeDesc")).withValidator(Validators.validMayorAssistantLeader).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				ArrayList<String> out = new ArrayList<String>();
				DecimalFormat df = new DecimalFormat("#,###.#");
				CivMessage.sendHeading(sender, town.getName() + " " + CivSettings.localize.localizedString("cmd_town_info_templeHeading"));
				double total = 0;
				for (Structure struct : town.BM.getStructures()) {
					if (!struct.getConfigId().equals("s_temple")) continue;
					Temple temple = (Temple) struct;
					String color = struct.isActive() ? CivColor.LightGreen : CivColor.Rose;
					double culture = temple.getCultureGenerated();
					if (!struct.isDestroyed()) {
						out.add(color + CivSettings.localize.localizedString("cmd_town_info_templeName") + " (" + struct.getCorner() + ")");
						out.add(CivColor.Green + "    " + CivSettings.localize.localizedString("Level") + " " + CivColor.Yellow + temple.getLevel() + CivColor.Green + " " + CivSettings.localize.localizedString("count") + " "
								+ CivColor.Yellow + "(" + temple.getCount() + "/" + temple.getMaxCount() + ")");
						out.add(CivColor.Green + "    " + CivSettings.localize.localizedString("baseCulture") + " " + CivColor.Yellow + culture + CivColor.Green + " " + CivSettings.localize.localizedString("LastResult") + " "
								+ CivColor.Yellow + temple.getLastResult().name());
					} else {
						out.add(color + CivSettings.localize.localizedString("cmd_town_info_templeName") + " " + "(" + struct.getCorner() + ")");
						out.add(CivColor.Rose + "    " + CivSettings.localize.localizedString("DESTROYED"));
					}
					total += culture;
				}
				out.add(CivColor.Green + "----------------------------");
				out.add(CivColor.Green + CivSettings.localize.localizedString("SubTotal") + " " + CivColor.Yellow + total);
				out.add(CivColor.Green + CivSettings.localize.localizedString("Temple") + " " + CivColor.Yellow + df.format(town.getTempleRate() * 100) + "%");
				total *= town.getTempleRate();
				out.add(CivColor.Green + CivSettings.localize.localizedString("Total") + " " + CivColor.Yellow + df.format(total) + " " + CivSettings.localize.localizedString("Culture"));
				CivMessage.send(sender, out);
			}
		}));
		add(new CustomCommand("structures").withDescription(CivSettings.localize.localizedString("cmd_town_info_structuresDesc")).withValidator(Validators.validMayorAssistantLeader).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				if (args.length > 1) {
					if (args[1].equalsIgnoreCase("debug")) {
						showDebugStructureInfo(sender, town);
						return;
					}
				}
				HashMap<String, Double> structsByName = new HashMap<String, Double>();
				for (Structure struct : town.BM.getStructures()) {
					Double upkeep = structsByName.get(struct.getConfigId());
					if (upkeep == null) {
						structsByName.put(struct.getDisplayName(), struct.getUpkeepCost());
					} else {
						upkeep += struct.getUpkeepCost();
						structsByName.put(struct.getDisplayName(), upkeep);
					}
				}
				CivMessage.sendHeading(sender, town.getName() + " " + CivSettings.localize.localizedString("cmd_town_info_structuresInfo"));
				for (String structName : structsByName.keySet()) {
					Double upkeep = structsByName.get(structName);
					CivMessage.send(sender, CivColor.Green + structName + " " + CivSettings.localize.localizedString("cmd_town_info_structuresUpkeep") + " " + CivColor.LightGreen + upkeep);
				}
				CivMessage.sendHeading(sender, town.getName() + " " + CivSettings.localize.localizedString("cmd_town_info_stucturesWonders"));
				for (Wonder wonder : town.BM.getWonders()) {
					CivMessage.send(sender, CivColor.Green + wonder.getDisplayName() + " " + CivSettings.localize.localizedString("cmd_town_info_structuresUpkeep") + " " + CivColor.LightGreen + wonder.getUpkeepCost());
				}
			}
		}));
		add(new CustomCommand("culture").withDescription(CivSettings.localize.localizedString("cmd_town_info_cultureDesc")).withValidator(Validators.validMayorAssistantLeader).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				AttrSource cultureSources = town.SM.getAttr(StorageType.CULTURE);
				CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_town_info_cultureHeading"));
				CivMessage.send(sender, cultureSources.getSourceDisplayString(CivColor.Green, CivColor.LightGreen));
				CivMessage.send(sender, cultureSources.getRateDisplayString(CivColor.Green, CivColor.LightGreen));
				CivMessage.send(sender, cultureSources.getTotalDisplayString(CivColor.Green, CivColor.LightGreen));
			}
		}));
		add(new CustomCommand("foods").withDescription(CivSettings.localize.localizedString("cmd_town_info_cultureDesc")).withValidator(Validators.validMayorAssistantLeader).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				AttrSource foodsSources = town.SM.getAttr(StorageType.FOODS);
				CivMessage.sendHeading(sender, "FIXME " + CivSettings.localize.localizedString("cmd_town_info_cultureHeading"));
				CivMessage.send(sender, foodsSources.getSourceDisplayString(CivColor.Green, CivColor.LightGreen));
				CivMessage.send(sender, foodsSources.getRateDisplayString(CivColor.Green, CivColor.LightGreen));
				CivMessage.send(sender, foodsSources.getTotalDisplayString(CivColor.Green, CivColor.LightGreen));
			}
		}));
		add(new CustomCommand("supplies").withDescription(CivSettings.localize.localizedString("cmd_town_info_cultureDesc")).withValidator(Validators.validMayorAssistantLeader).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				AttrSource suppliesSources = town.SM.getAttr(StorageType.SUPPLIES);
				CivMessage.sendHeading(sender, "FIXME " + CivSettings.localize.localizedString("cmd_town_info_cultureHeading"));
				CivMessage.send(sender, suppliesSources.getSourceDisplayString(CivColor.Green, CivColor.LightGreen));
				CivMessage.send(sender, suppliesSources.getRateDisplayString(CivColor.Green, CivColor.LightGreen));
				CivMessage.send(sender, suppliesSources.getTotalDisplayString(CivColor.Green, CivColor.LightGreen));
			}
		}));
		add(new CustomCommand("mine").withDescription(CivSettings.localize.localizedString("cmd_town_info_mineDesc")).withValidator(Validators.validMayorAssistantLeader).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				DecimalFormat df = new DecimalFormat("#,###.#");
				ArrayList<String> out = new ArrayList<String>();
				CivMessage.sendHeading(sender, town.getName() + " " + CivSettings.localize.localizedString("cmd_town_info_mineHeading"));
				double total = 0;
				for (Structure struct : town.BM.getStructures()) {
					if (!struct.getConfigId().equals("ti_mine")) continue;
					Mine mine = (Mine) struct;
					String color = (struct.isActive()) ? CivColor.LightGreen : CivColor.Rose;
					out.add(color + CivSettings.localize.localizedString("cmd_town_info_mineName") + " (" + struct.getCorner() + ")");
					out.add(CivColor.Green + "    " + CivSettings.localize.localizedString("Level") + " " + CivColor.Yellow + mine.getLevel() + CivColor.Green + " " + CivSettings.localize.localizedString("count") + " " + CivColor.Yellow
							+ "(" + mine.getCount() + "/" + mine.getMaxCount() + ")");
					out.add(CivColor.Green + "    " + CivSettings.localize.localizedString("hammersPerTile") + " " + CivColor.Yellow + mine.getBonusHammers());
					out.add(CivColor.Green + "    " + CivSettings.localize.localizedString("LastResult") + " " + CivColor.Yellow + mine.getLastResult().name());
					total += mine.getBonusHammers(); // XXX estimate based on tile radius of 1.
				}
				out.add(CivColor.Green + "----------------------------");
				out.add(CivColor.Green + CivSettings.localize.localizedString("SubTotal") + " " + CivColor.Yellow + total);
				out.add(CivColor.Green + CivSettings.localize.localizedString("Total") + " " + CivColor.Yellow + df.format(total) + " " + CivSettings.localize.localizedString("cmd_town_info_mineHammersInfo"));
				CivMessage.send(sender, out);
			}
		}));
		add(new CustomCommand("hammers").withDescription(CivSettings.localize.localizedString("cmd_town_info_hammersDesc")).withValidator(Validators.validMayorAssistantLeader).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_town_info_hammersHeading"));
				AttrSource hammerSources = town.SM.getAttr(StorageType.HAMMERS);
				CivMessage.send(sender, hammerSources.getSourceDisplayString(CivColor.Green, CivColor.LightGreen));
				CivMessage.send(sender, hammerSources.getRateDisplayString(CivColor.Green, CivColor.LightGreen));
				CivMessage.send(sender, hammerSources.getTotalDisplayString(CivColor.Green, CivColor.LightGreen));
			}
		}));
		add(new CustomCommand("rates").withDescription(CivSettings.localize.localizedString("cmd_town_info_ratesDesc")).withValidator(Validators.validMayorAssistantLeader).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				CivMessage.sendHeading(sender, town.getName() + " " + CivSettings.localize.localizedString("cmd_town_info_ratesHeading"));
				DecimalFormat df = new DecimalFormat("#,###.#");
				CivMessage.send(sender,
						CivColor.Green + " " + CivSettings.localize.localizedString("cmd_civ_gov_infoGrowth") + " " + CivColor.LightGreen + df.format(town.SM.getAttr(StorageType.GROWTH).getRate().total * 100) + CivColor.Green + " "
								+ CivSettings.localize.localizedString("cmd_civ_gov_infoCulture") + " " + CivColor.LightGreen + df.format(town.SM.calcAttrCultureRate().total * 100) + CivColor.Green + " "
								+ CivSettings.localize.localizedString("cmd_civ_gov_infoCottage") + " " + CivColor.LightGreen + df.format(town.getCottageRate() * 100) + CivColor.Green + " " + CivSettings.localize.localizedString("Temple")
								+ " " + CivColor.Green + " " + CivSettings.localize.localizedString("cmd_civ_gov_infoBeaker") + " " + CivColor.LightGreen + df.format(town.SM.getAttr(StorageType.BEAKERS).getRate().total * 100));
			}
		}));
		add(new CustomCommand("growth").withDescription(CivSettings.localize.localizedString("cmd_town_info_growthDesc")).withValidator(Validators.validMayorAssistantLeader).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				AttrSource growthSources = town.SM.getAttr(StorageType.GROWTH);
				CivMessage.sendHeading(sender, town.getName() + " " + CivSettings.localize.localizedString("cmd_town_info_growthHeading"));
				CivMessage.send(sender, growthSources.getSourceDisplayString(CivColor.Green, CivColor.LightGreen));
				CivMessage.send(sender, growthSources.getRateDisplayString(CivColor.Green, CivColor.LightGreen));
				CivMessage.send(sender, growthSources.getTotalDisplayString(CivColor.Green, CivColor.LightGreen));
			}
		}));
		add(new CustomCommand("buffs").withDescription(CivSettings.localize.localizedString("cmd_town_info_buffsDesc")).withValidator(Validators.validMayorAssistantLeader).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				CivMessage.sendHeading(sender, town.getName() + " " + CivSettings.localize.localizedString("cmd_town_info_buffsHeading"));
				ArrayList<String> out = new ArrayList<String>();
				for (Buff buff : town.getBuffManager().getAllBuffs()) {
					out.add(CivColor.Green + CivSettings.localize.localizedString("var_BuffsFrom", (CivColor.LightGreen + buff.getDisplayName() + CivColor.Green), CivColor.LightGreen + buff.getSource()));
				}
				CivMessage.send(sender, out);
			}
		}));
		add(new CustomCommand("online").withDescription(CivSettings.localize.localizedString("cmd_town_info_onlineDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				CivMessage.sendHeading(sender, CivSettings.localize.localizedString("var_cmd_town_info_onlineHeading", town.getName()));
				String out = "";
				for (Resident resident : town.getOnlineResidents()) {
					out += resident.getName() + " ";
				}
				CivMessage.send(sender, out);
			}
		}));
		add(new CustomCommand("happiness").withDescription(CivSettings.localize.localizedString("cmd_town_info_happinessDesc")).withValidator(Validators.validMayorAssistantLeader).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_town_info_happinessHeading"));
				ArrayList<String> out = new ArrayList<String>();
				out.add(CivMessage.buildSmallTitle(CivSettings.localize.localizedString("cmd_town_info_happinessSources")));
				AttrSource happySources = town.SM.getAttr(StorageType.HAPPY);
				DecimalFormat df = new DecimalFormat();
				df.applyPattern("###,###");
				for (String source : happySources.sources.keySet()) {
					Double value = happySources.sources.get(source);
					out.add(CivColor.Green + source + ": " + CivColor.LightGreen + df.format(value));
				}
				out.add(CivColor.LightPurple + CivSettings.localize.localizedString("Total") + " " + CivColor.LightGreen + df.format(happySources.total));
				out.add(CivMessage.buildSmallTitle(CivSettings.localize.localizedString("cmd_town_info_happinessUnhappy")));
				AttrSource unhappySources = town.SM.getAttr(StorageType.UNHAPPY);
				for (String source : unhappySources.sources.keySet()) {
					Double value = unhappySources.sources.get(source);
					out.add(CivColor.Green + source + ": " + CivColor.LightGreen + value);
				}
				out.add(CivColor.LightPurple + CivSettings.localize.localizedString("Total") + " " + CivColor.LightGreen + df.format(unhappySources.total));
				out.add(CivMessage.buildSmallTitle(CivSettings.localize.localizedString("Total")));
				CivMessage.send(sender, out);
			}
		}));
		add(new CustomCommand("beakers").withDescription(CivSettings.localize.localizedString("cmd_town_info_beakersDesc")).withValidator(Validators.validMayorAssistantLeader).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_town_info_beakersHeading"));
				AttrSource beakerSources = town.SM.getAttr(StorageType.BEAKERS);
				CivMessage.send(sender, beakerSources.getSourceDisplayString(CivColor.Green, CivColor.LightGreen));
				CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("cmd_civ_gov_infoBeaker") + " " + CivColor.LightGreen + (beakerSources.getRate().total * 100 + "%"));
				CivMessage.send(sender, beakerSources.getTotalDisplayString(CivColor.Green, CivColor.LightGreen));
			}
		}));
		add(new CustomCommand("area").withDescription(CivSettings.localize.localizedString("cmd_town_info_areaDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_town_info_areaHeading"));
				HashMap<String, Integer> biomes = new HashMap<String, Integer>();
				double hammers = 0.0;
				double growth = 0.0;
				double happiness = 0.0;
				double beakers = 0.0;
				DecimalFormat df = new DecimalFormat();
				for (CultureChunk cc : town.getCultureChunks()) {
					/* Increment biome counts. */
					if (!biomes.containsKey(cc.getBiome().name())) {
						biomes.put(cc.getBiome().name(), 1);
					} else {
						Integer value = biomes.get(cc.getBiome().name());
						biomes.put(cc.getBiome().name(), value + 1);
					}
					hammers += cc.getHammers();
					growth += cc.getGrowth();
					happiness += cc.getHappiness();
				}
				CivMessage.send(sender, CivColor.LightBlue + CivSettings.localize.localizedString("cmd_town_biomeList"));
				String out = "";
				// int totalBiomes = 0;
				for (String biome : biomes.keySet()) {
					Integer count = biomes.get(biome);
					out += CivColor.Green + biome + ": " + CivColor.LightGreen + count + CivColor.Green + ", ";
					// totalBiomes += count;
				}
				CivMessage.send(sender, out);
				CivMessage.send(sender, CivColor.LightBlue + "Totals");
				CivMessage.send(sender,
						CivColor.Green + " " + CivSettings.localize.localizedString("cmd_town_happiness") + " " + CivColor.LightGreen + df.format(happiness) + CivColor.Green + " " + CivSettings.localize.localizedString("Hammers") + " "
								+ CivColor.LightGreen + df.format(hammers) + CivColor.Green + " " + CivSettings.localize.localizedString("cmd_town_growth") + " " + CivColor.LightGreen + df.format(growth) + CivColor.Green + " "
								+ CivSettings.localize.localizedString("Beakers") + " " + CivColor.LightGreen + df.format(beakers));
			}
		}));
		add(new CustomCommand("disabled").withDescription(CivSettings.localize.localizedString("cmd_town_info_disabledDesc")).withValidator(Validators.validMayorAssistantLeader).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_town_info_disabledHeading"));
				ArrayList<String> out = new ArrayList<>();
				boolean showhelp = false;
				for (Buildable buildable : town.BM.getDisabledBuildables()) {
					showhelp = true;
					out.add(CivColor.Green + buildable.getDisplayName() + CivColor.LightGreen + " " + CivSettings.localize.localizedString("Coord") + buildable.getCorner().toString());
				}
				if (showhelp) {
					out.add(CivColor.LightGray + CivSettings.localize.localizedString("cmd_town_info_disabledHelp1"));
					out.add(CivColor.LightGray + CivSettings.localize.localizedString("cmd_town_info_disabledHelp2"));
					out.add(CivColor.LightGray + CivSettings.localize.localizedString("cmd_town_info_disabledHelp3"));
					out.add(CivColor.LightGray + CivSettings.localize.localizedString("cmd_town_info_disabledHelp4"));
					out.add(CivColor.LightGray + CivSettings.localize.localizedString("cmd_town_info_disabledHelp5"));
				}
				CivMessage.send(sender, out);
			}
		}));
		add(new CustomCommand("tradeship").withDescription(CivSettings.localize.localizedString("cmd_town_info_tradeship_desc")).withValidator(Validators.validMayorAssistantLeader).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_town_info_tradeshipHeading", town.getName()));
				if (!town.BM.hasStructure("ti_trade_ship")) throw new CivException(CivSettings.localize.localizedString("cmd_town_info_tradeship_noShip"));
				TradeShip tradeShip = (TradeShip) town.BM.getFirstStructureById("ti_trade_ship");
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_town_info_tradeship_level", "§b" + tradeShip.getLevel()));
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_town_info_tradeship_level", "§b" + tradeShip.getLevel()));
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_town_info_tradeship_progress", CivColor.Red + tradeShip.getTradeLevelComponent().getCountString()));
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_town_info_tradeship_stagnateDebuff", "§6" + tradeShip.getLastResult() + "§a", "§2" + "Progress" + "§a"));
				CivMessage.sendHeading(sender, "");
			}
		}));
	}

	public void showDebugStructureInfo(CommandSender sender, Town town) {
		CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_town_info_showDebug"));
		for (Structure struct : town.BM.getStructures()) {
			CivMessage.send(sender, struct.getDisplayName() + ": " + CivSettings.localize.localizedString("cmd_town_info_showdebugCorner") + " " + struct.getCorner() + " "
					+ CivSettings.localize.localizedString("cmd_town_info_showdebugCenter") + " " + struct.getCenterLocation().toVector());
		}
	}

	public static void show(CommandSender sender, Resident resident, Town town, Civilization civ) throws CivException {
		DecimalFormat df = new DecimalFormat();
		boolean isAdmin = false;

		if (resident != null) {
			Player player = CivGlobal.getPlayer(resident);
			isAdmin = player.hasPermission(CivSettings.MINI_ADMIN);
		} else {
			/* We're the console! */
			isAdmin = true;
		}

		CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_town_info_showHeading", town.getName()));
		ConfigCultureLevel clc = CivSettings.cultureLevels.get(town.SM.getLevel());
		CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("Civilization") + " " + CivColor.LightGreen + town.getCiv().getName());
		CivMessage.send(sender,
				CivColor.Green + CivSettings.localize.localizedString("TownLevel") + " " + CivColor.LightGreen + town.SM.getLevel() + " (" + town.getLevelTitle() + ") " + CivColor.Green + CivSettings.localize.localizedString("Culture")
						+ " " + CivColor.LightGreen + town.SM.getCulture() + "/" + clc.amount + "  " + CivColor.Green + CivSettings.localize.localizedString("Score") + " " + CivColor.LightGreen + town.getScore());

		if (town.GM.getMayorGroup() == null)
			CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("Mayors") + " " + CivColor.Rose + CivSettings.localize.localizedString("none"));
		else
			CivMessage.send(sender, CivColor.Green + town.GM.mayorGroupName + ": " + CivColor.LightGreen + town.GM.getMayorGroup().getMembersString());

		if (town.GM.getAssistantGroup() == null)
			CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("Assitants") + " " + CivColor.Rose + CivSettings.localize.localizedString("none"));
		else
			CivMessage.send(sender, CivColor.Green + town.GM.assistantGroupName + ": " + CivColor.LightGreen + town.GM.getAssistantGroup().getMembersString());

		int lastFood = (int) town.SM.getAttr(StorageType.FOODS).total;
		CivMessage.send(sender, CivColor.Green + "Население города: " + CivColor.LightGreen + town.PM.getPeoplesTotal()//
				+ CivColor.Green + "  Стакан еды: " + CivColor.LightGreen + town.SM.getFoodBasket() + "(" + (lastFood < 0 ? CivColor.Rose : "+") + lastFood + (lastFood < 0 ? CivColor.LightGreen : "") + ")/"
				+ town.SM.getFoodBasketSize()//
				+ CivColor.Green + "  Склад материалов: " + CivColor.LightGreen + town.SM.getSupplies() + "(+" + town.SM.getAttr(StorageType.SUPPLIES).total + ")");

		if (resident == null || civ.hasResident(resident) || isAdmin) {
			String color = CivColor.LightGreen;
			Integer maxTileImprovements = town.getMaxTileImprovements();
			if (town.getTileImprovementCount() > maxTileImprovements) color = CivColor.Rose;

			CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("Plots") + " " + CivColor.LightGreen + "(" + town.getTownChunks().size() + "/" + town.getMaxPlots() + ") " + CivColor.Green + " "
					+ CivSettings.localize.localizedString("TileImprovements") + " " + CivColor.LightGreen + "(" + color + town.getTileImprovementCount() + CivColor.LightGreen + "/" + maxTileImprovements + ")");

			CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("Growth") + " " + CivColor.LightGreen + df.format(town.SM.getAttr(StorageType.GROWTH).total)//
					+ " " + CivColor.Green + CivSettings.localize.localizedString("Hammers") + " " + CivColor.LightGreen + df.format(town.SM.getAttr(StorageType.HAMMERS).total));

			CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("Members") + " " + CivColor.LightGreen + town.getResidentCount() + CivColor.Green + "  " + CivSettings.localize.localizedString("Online") + " "
					+ CivColor.LightGreen + town.getOnlineResidents().size());

			// HashMap<String, String> info = new HashMap<String, String>();
			// info.put("Happiness", CivColor.White+"("+CivColor.LightGreen+"H"+CivColor.Yellow+town.getHappinessTotal()
			// +CivColor.White+"/"+CivColor.Rose+"U"+CivColor.Yellow+town.getUnhappinessTotal()+CivColor.White+") = "+
			// CivColor.LightGreen+df.format(town.getHappinessPercentage()*100)+"%");
			// info.put(CivSettings.localize.localizedString("Happiness"), CivColor.LightGreen + df.format(Math.floor(town.SM.getHappinessPercentage() *
			// 100)) + "%");
			// CivMessage.send(sender, Commander.makeInfoString(info, CivColor.Green, CivColor.LightGreen));
		}

		if (resident == null || town.GM.isMayorOrAssistant(resident) || civ.GM.isLeaderOrAdviser(resident) || isAdmin) {
			try {
				String s = "";
				for (Prof prof : town.PM.getPeoplesPriority()) {
					s += CivColor.LightGreen + prof.toString() + ": " + CivColor.LightBlue + town.PM.getPeoplesProfCount(prof) + CivColor.LightGray + "(" + town.PM.getPeoplesWorker(prof) + CivColor.LightGreen + "), ";
				}
				CivMessage.send(sender, CivColor.Green + "Население города: " + CivColor.LightGreen + s);
				CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("Treasury") + " " + CivColor.LightGreen + town.getBalance() + " " + CivSettings.CURRENCY_NAME + "   " + CivColor.Green
						+ CivSettings.localize.localizedString("cmd_town_info_structuresUpkeep") + " " + CivColor.LightGreen + town.getTotalUpkeep() * town.getGovernment().upkeep_rate + " " + CivSettings.CURRENCY_NAME);
				Structure bank = town.BM.getFirstStructureById("s_bank");
				if (bank != null) {
					CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("cmd_town_info_showBankInterest") + " " + CivColor.LightGreen + df.format(((Bank) bank).getInterestRate() * 100) + "%" + CivColor.Green + " "
							+ CivSettings.localize.localizedString("cmd_town_info_showBankPrinciple") + " " + CivColor.LightGreen + town.getTreasury().getPrincipalAmount());
				} else {
					CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("cmd_town_info_showBankInterest") + " " + CivColor.LightGreen + CivSettings.localize.localizedString("cmd_town_info_showBankNoBank") + " "
							+ CivColor.Green + CivSettings.localize.localizedString("cmd_town_info_showBankPrinciple") + " " + CivColor.LightGreen + CivSettings.localize.localizedString("cmd_town_info_showBankNoBank"));
				}
			} catch (InvalidConfiguration e) {
				e.printStackTrace();
				throw new CivException(CivSettings.localize.localizedString("internalException"));
			}
		}

		if (town.inDebt()) {
			CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("Debt") + " " + CivColor.Yellow + town.getDebt() + " " + CivSettings.CURRENCY_NAME);
			CivMessage.send(sender, CivColor.Yellow + CivSettings.localize.localizedString("cmd_town_info_showInDebt"));
		}

		if (town.getMotherCiv() != null) CivMessage.send(sender, CivColor.Yellow + CivSettings.localize.localizedString("var_cmd_town_info_showYearn", CivColor.LightPurple + town.getMotherCiv().getName() + CivColor.Yellow));
		if (town.BM.getDisabledBuildables().size() > 0) CivMessage.send(sender, CivColor.Rose + CivSettings.localize.localizedString("cmd_town_info_showDisabled"));

		if (isAdmin) {
			if (!town.isValid())
				CivMessage.send(sender, CivColor.LightPurple + CivSettings.localize.localizedString("cmd_town_info_showNoTownHall"));
			else
				CivMessage.send(sender, CivColor.LightPurple + CivSettings.localize.localizedString("Location") + " " + (new BlockCoord(town.getLocation())).toStringNotWorld());

			String wars = "";
			for (Relation relation : town.getCiv().getDiplomacyManager().getRelations()) {
				if (relation.getStatus() == Status.WAR) wars += relation.getOtherCiv().getName() + ", ";
			}
			CivMessage.send(sender, CivColor.LightPurple + CivSettings.localize.localizedString("cmd_town_info_showWars") + " " + wars);
		}
	}

	private void show_info(CommandSender sender) throws CivException {
		Civilization civ = Commander.getSenderCiv(sender);
		Town town = Commander.getSelectedTown(sender);
		Resident resident = Commander.getResident(sender);

		show(sender, resident, town, civ);
	}

	@Override
	public void doDefaultAction(CommandSender sender) throws CivException {
		show_info(sender);
		CivMessage.send(sender, CivColor.LightGray + CivSettings.localize.localizedString("cmd_town_info_showHelp"));
	}

}
