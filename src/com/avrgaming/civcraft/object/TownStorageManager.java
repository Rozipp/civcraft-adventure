package com.avrgaming.civcraft.object;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.HashMap;

import com.avrgaming.civcraft.components.AttributeBase;
import com.avrgaming.civcraft.components.AttributeWarUnhappiness;
import com.avrgaming.civcraft.components.Component;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigCultureLevel;
import com.avrgaming.civcraft.construct.structures.Mine;
import com.avrgaming.civcraft.construct.structures.Structure;
import com.avrgaming.civcraft.construct.structures.Temple;
import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.randomevents.RandomEvent;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.TimeTools;

public class TownStorageManager {

	public static enum StorageType {
		Food, Hammer, Culture, Econ, Beakers, Happy, Unhappy,
	}

	private Town town;

	private double foodBasket = 0;
	private double hammers = 600;
	private double culture = 0;
	private double econsCash = 0;
	private double beakersCivtick;

	public double baseHammers = 1.0;
	public double baseGrowth = 0.0;
	public double baseHappy = 0.0;
	public double baseUnhappy = 0.0;

	public EnumMap<StorageType, AttrSource> attributeCache = new EnumMap<>(StorageType.class);

	public TownStorageManager(Town town) {
		this.town = town;
		this.setBaseHammers(1.0);
	}

	public TownStorageManager(Town town, ResultSet rs) throws SQLException {
		this.town = town;
		this.foodBasket = rs.getDouble("foodBasket");
		this.hammers = rs.getDouble("hammers");
		this.culture = rs.getDouble("culture");
		this.econsCash = rs.getDouble("storageCash");
	}

	public void saveNow(HashMap<String, Object> hashmap) {
		hashmap.put("foodBasket", this.getFoodBasket());
		hashmap.put("hammers", this.getHammers());
		hashmap.put("culture", this.getCulture());
		hashmap.put("storageCash", econsCash);
	}

	public void saveNowCash() {
		if (town.getId() != 0) {
			HashMap<String, Object> hashmap = new HashMap<String, Object>();
			hashmap.put("id", town.getId());
			saveNow(hashmap);
			SQL.updateNamedObjectAsync(town, hashmap, Town.TABLE_NAME);
		}
	}

	public void onHourlyUpdate() {
		int depositEcons = (int) econsCash;
		econsCash = 0;
		town.getTreasury().deposit(depositEcons);
		CivMessage.sendTown(town, CivColor.LightGreen + "Жители принесли в казну города " + depositEcons + " монет.");

		for (StorageType type : StorageType.values()) {
			if (attributeCache.containsKey(type)) attributeCache.get(type).clearHourAttrSources();
		}
	}

	public void onCivtickUpdate() {
		calcAttrGrowth();
		calcAttrHammer();

		calcAttrHappiness();
		calcAttrUnhappiness();

		addCulture(calcAttrCulture() * 0.01);
		econsCash += town.PM.getIntake(StorageType.Econ);
		setBeakersCivtick(town.PM.getIntake(StorageType.Beakers) + (getAttrBeakers().total / 100)); // TODO отправить в циву

		processHammers();
		processFoods();
		saveNowCash();
	}

	// ------------ food

	private void changeFoods(double food) {
		if (food == 0) return;
		foodBasket += food;
		while (foodBasket >= getFoodBasketSize()) {
			foodBasket -= getFoodBasketSize();
			town.PM.bornPeoples(1);
		}
		while (foodBasket < 0) {
			foodBasket += getFoodBasketSize() / 2;
			town.PM.deadPeoples(1);
		}
	}

	public int getFoodBasketSize() {
		return 2000 + town.PM.getPeoplesTotal() * 1000; // TODO перенести в константу
	}

	public int getFoodBasket() {
		return (int) foodBasket;
	}

	public void processFoods() {
		double foods = Math.min(getAttrGrowth().total * 0.01, town.PM.getIntake(StorageType.Food));
		changeFoods(foods - town.PM.getFoodsOuttake());
	}

	// -------------- Hammers

	public int getHammers() {
		return (int) hammers;
	}

	public void depositHammers(double hammers) {
		this.hammers += hammers;
	}

	public double withdrawHammers(double hammers) {
		double withraw = Math.min(hammers, this.hammers);
		this.hammers -= withraw;
		return withraw;
	}

	public void processHammers() {
		depositHammers(Math.min(getAttrHammer().total * 0.01, town.PM.getIntake(StorageType.Hammer)));
	}

	public AttrRate calcAttrHammerRate() {
		double rate = 1.0;
		HashMap<String, Double> rates = new HashMap<String, Double>();

		/* Government */
		double newRate = rate * town.getGovernment().hammer_rate;
		rates.put("Government", newRate - rate);
		rate = newRate;

		double randomRate = RandomEvent.getHammerRate(town);
		newRate = rate * randomRate;
		rates.put("Random Events", newRate - rate);
		rate = newRate;

		for (final Town town : town.getCiv().getTowns()) {
			if (town.getBuffManager().hasBuff("buff_spoil")) newRate *= 1.1;
		}
		rate = newRate;

		/* Captured Town Penalty */
		if (town.getMotherCiv() != null) {
			try {
				newRate = rate * CivSettings.getDouble(CivSettings.warConfig, "war.captured_penalty");
				rates.put("Captured Penalty", newRate - rate);
				rate = newRate;
			} catch (InvalidConfiguration e) {
				e.printStackTrace();
			}
		}
		return new AttrRate(rates, rate);
	}

	public void calcAttrHammer() {
		double total = 0;
		HashMap<String, Double> sources = new HashMap<String, Double>();

		/* Wonders and Goodies. */
		double wonderGoodies = town.getBuffManager().getEffectiveInt(Buff.CONSTRUCTION);
		wonderGoodies += town.getBuffManager().getEffectiveDouble("buff_grandcanyon_hammers");
		sources.put("Wonders/Goodies", wonderGoodies);
		total += wonderGoodies;

		if (town.hasScroll()) {
			total += 500.0;
			sources.put("Scroll of Hammers (Until " + town.getScrollTill() + ")", 500.0);
		}

		double cultureHammers = this.getHammersFromCulture();
		sources.put("Culture Biomes", cultureHammers);
		total += cultureHammers;

		/* Grab hammers generated from structures with components. */
		double structures = 0;
		double mines = 0;
		for (Structure struct : town.BM.getStructures()) {
			if (struct instanceof Mine) {
				Mine mine = (Mine) struct;
				mines += mine.getBonusHammers();
			}
			for (Component comp : struct.attachedComponents) {
				if (comp instanceof AttributeBase) {
					AttributeBase ab = (AttributeBase) comp;
					if (ab.getString("attribute").equalsIgnoreCase("HAMMERS")) structures += ab.getGenerated();
				}
			}
		}

		total += mines;
		sources.put("Mines", mines);

		total += structures;
		sources.put("Structures", structures);

		sources.put("Base Hammers", this.baseHammers);
		total += this.baseHammers;

		AttrRate rate = calcAttrHammerRate();
		total *= rate.total;

		if (total < this.baseHammers) total = this.baseHammers;

		AttrSource cache = this.attributeCache.get(StorageType.Hammer);
		if (cache == null)
			cache = new AttrSource(sources, total, rate);
		else {
			cache.modifyAttrSource(sources, total, rate);
		}
		attributeCache.put(StorageType.Hammer, cache);
		return;
	}

	public AttrSource getAttrHammer() {
		if (!attributeCache.containsKey(StorageType.Hammer)) calcAttrHammer();
		return this.attributeCache.get(StorageType.Hammer);
	}

	public void setBaseHammers(double baseHammers) {
		this.baseHammers = baseHammers;
	}

	public Double getHammersFromCulture() {
		double hammers = 0;
		for (CultureChunk cc : town.getCultureChunks()) {
			hammers += cc.getHammers();
		}
		return hammers;
	}

	// ------------- culture

	public void addCulture(double generated) {
		this.culture += generated;
		town.save();

		ConfigCultureLevel clc = CivSettings.cultureLevels.get(this.getLevel());
		if (this.getLevel() != CivSettings.getMaxCultureLevel()) {
			if (this.culture >= clc.amount) {
				CivGlobal.processCulture();
				CivMessage.sendCiv(town.getCiv(), CivSettings.localize.localizedString("var_town_bordersExpanded", town.getName()));
			}
		}
		return;
	}

	public int getCulture() {
		return (int) culture;
	}

	public AttrRate getAttrCultureRate() {
		double rate = 1.0;
		HashMap<String, Double> rates = new HashMap<String, Double>();

		double newRate = town.getGovernment().culture_rate;
		rates.put("Government", newRate - rate);
		rate = newRate;

		double structures = 0;
		if (town.getBuffManager().hasBuff("buff_art_appreciation")) structures += town.getBuffManager().getEffectiveDouble("buff_art_appreciation");
		rates.put("Great Works", structures);
		rate += structures;

		double additional = 0;
		if (town.getBuffManager().hasBuff("buff_fine_art")) additional += town.getBuffManager().getEffectiveDouble(Buff.FINE_ART);
		if (town.getBuffManager().hasBuff("buff_pyramid_culture")) additional += town.getBuffManager().getEffectiveDouble("buff_pyramid_culture");
		if (town.getBuffManager().hasBuff("buff_neuschwanstein_culture")) additional += town.getBuffManager().getEffectiveDouble("buff_neuschwanstein_culture");

		rates.put("Wonders/Goodies", additional);
		rate += additional;

		return new AttrRate(rates, rate);
	}

	public AttrSource getAttrCulture() {
		if (!attributeCache.containsKey(StorageType.Culture)) calcAttrCulture();
		return attributeCache.get(StorageType.Culture);
	}

	public double calcAttrCulture() {
		double total = 0;
		HashMap<String, Double> sources = new HashMap<String, Double>();
		double goodieCulture = 0.0;
		if (town.getBuffManager().hasBuff("buff_advanced_touring")) goodieCulture += 200.0;
		sources.put("Goodies", goodieCulture);
		total += goodieCulture;
		/* Grab beakers generated from structures with components. */
		double fromStructures = 0;
		for (Structure struct : town.BM.getStructures()) {
			for (Component comp : struct.attachedComponents) {
				if (comp instanceof AttributeBase) {
					AttributeBase as = (AttributeBase) comp;
					if (as.getString("attribute").equalsIgnoreCase("CULTURE")) fromStructures += as.getGenerated();
				}
			}
			if (struct instanceof Temple) {
				Temple temple = (Temple) struct;
				fromStructures += temple.getCultureGenerated();
			}
		}

		total += fromStructures / TimeTools.countCivtickInHour;
		sources.put("Structures", fromStructures / TimeTools.countCivtickInHour);

		double globe_theatre = 0;
		if (town.getBuffManager().hasBuff("buff_globe_theatre_culture_from_towns")) {
			int townCount = CivGlobal.getTowns().size();
			double culturePerTown = Double.valueOf(CivSettings.buffs.get("buff_globe_theatre_culture_from_towns").value);
			globe_theatre = culturePerTown * townCount;
			CivMessage.sendTown(town, CivColor.LightGreen + CivSettings.localize.localizedString("var_town_GlobeTheatreCulture", CivColor.Yellow + globe_theatre + CivColor.LightGreen, townCount));
		}
		total += globe_theatre;
		sources.put("Globe theatre", globe_theatre);

		double fromPeople = town.PM.getIntake(StorageType.Culture);
		total += fromPeople;
		sources.put("Populate", fromPeople);

		AttrRate rate = this.calcAttrHammerRate();
		total *= rate.total;

		if (total < 0) total = 0;

		AttrSource cache = this.attributeCache.get(StorageType.Culture);
		if (cache == null)
			cache = new AttrSource(sources, total, rate);
		else
			cache.modifyAttrSource(sources, total, rate);
		this.attributeCache.put(StorageType.Culture, cache);
		return cache.total;
	}

	public int getLevel() {
		/* Get the first level */
		int bestLevel = 0;
		ConfigCultureLevel configLevel = CivSettings.cultureLevels.get(0);

		while (this.culture >= configLevel.amount) {
			configLevel = CivSettings.cultureLevels.get(bestLevel + 1);
			if (configLevel == null) {
				configLevel = CivSettings.cultureLevels.get(bestLevel);
				break;
			}
			bestLevel++;
		}

		return configLevel.level;
	}

	// -------------- get growth

	public AttrRate calcAttrGrowthRate() {
		double rate = 1.0;
		HashMap<String, Double> rates = new HashMap<String, Double>();

		double newRate = rate * town.getGovernment().growth_rate;
		rates.put("Government", newRate - rate);
		rate = newRate;

		if (town.getCiv().hasTechnologys("tech_fertilizer")) {
			double techRate = 0.3;
			rates.put("Technology", techRate);
			rate += techRate;
		}

		/* Wonders and Goodies. */
		double additional = town.getBuffManager().getEffectiveDouble(Buff.GROWTH_RATE);
		additional += town.getBuffManager().getEffectiveDouble("buff_hanging_gardens_growth");
		additional += town.getBuffManager().getEffectiveDouble("buff_mother_tree_growth");

		rates.put("Wonders", additional);
		rate += additional;

		return new AttrRate(rates, rate);
	}

	public AttrSource getAttrGrowth() {
		if (!attributeCache.containsKey(StorageType.Food)) calcAttrGrowth();
		return attributeCache.get(StorageType.Food);
	}

	public void calcAttrGrowth() {
		double total = 0;
		HashMap<String, Double> sources = new HashMap<String, Double>();

		/* Grab any growth from culture. */
		double cultureSource = 0;
		for (CultureChunk cc : town.getCultureChunks()) {
			try {
				cultureSource += cc.getGrowth();
			} catch (NullPointerException e) {
				CivLog.error(town.getName() + " - Culture Chunks: " + cc);
				e.printStackTrace();
			}
		}

		sources.put("Culture Biomes", cultureSource);
		total += cultureSource;

		/* Grab any growth from structures. */
		double structures = 0;
		for (Structure struct : town.BM.getStructures()) {
			for (Component comp : struct.attachedComponents) {
				if (comp instanceof AttributeBase) {
					AttributeBase as = (AttributeBase) comp;
					if (as.getString("attribute").equalsIgnoreCase("GROWTH")) {
						double h = as.getGenerated();
						structures += h;
					}
				}
			}
		}

		total += structures;
		sources.put("Structures", structures);

		double fromBurj = 0;
		for (final Town town : town.getCiv().getTowns()) {
			if (town.BM.hasWonder("w_burj")) {
				fromBurj = 1000.0;
				break;
			}
		}
		if (fromBurj != 0) {
			sources.put("Wonders", fromBurj);
			total += fromBurj;
		}

		sources.put("Base Growth", baseGrowth);
		total += baseGrowth;

		AttrRate rate = this.calcAttrGrowthRate();
		total *= rate.total;

		if (total < 0) total = 0;

		AttrSource cache = this.attributeCache.get(StorageType.Food);
		if (cache == null)
			cache = new AttrSource(sources, total, rate);
		else
			cache.modifyAttrSource(sources, total, rate);
		this.attributeCache.put(StorageType.Food, cache);
		return;
	}

	// --------------- beaker

	public AttrRate calcAttrBeakerRate() {
		double rate = 1.0;
		HashMap<String, Double> rates = new HashMap<String, Double>();

		double newRate;

		newRate = rate * town.getGovernment().beaker_rate;
		rates.put("Government", newRate - rate);
		rate = newRate;

		/* Additional rate increases from buffs. */
		/* Great Library buff is made to not stack with Science_Rate */
		double additional = rate * town.getBuffManager().getEffectiveDouble(Buff.SCIENCE_RATE);
		additional += rate * town.getBuffManager().getEffectiveDouble("buff_greatlibrary_extra_beakers");
		rate += additional;
		rates.put("Goodies/Wonders", additional);

		return new AttrRate(rates, rate);
	}

	public AttrSource getAttrBeakers() {
		if (!attributeCache.containsKey(StorageType.Beakers)) calcAttrBeakers();
		return attributeCache.get(StorageType.Beakers);
	}

	public double calcAttrBeakers() {
		double total = 0;
		HashMap<String, Double> sources = new HashMap<String, Double>();

		/* Grab beakers generated from structures with components. */
		double fromStructures = town.BM.getBeakersFromStructure();
		total += fromStructures;
		sources.put("Structures", fromStructures);

		/* Grab any extra beakers from buffs. */
		double wondersTrade = 0;
		// No more flat bonuses here, leaving it in case of new buffs
		if (town.getBuffManager().hasBuff("buff_advanced_mixing")) wondersTrade += 150.0;
		total += wondersTrade;
		sources.put("Goodies/Wonders", wondersTrade);

		double fromPeople = town.PM.getIntake(StorageType.Beakers);
		total += fromPeople;
		sources.put("Populate", fromPeople);

		AttrRate rate = calcAttrBeakerRate();
		total = total * rate.total;

		if (total < 0) total = 0;

		AttrSource cache = this.attributeCache.get(StorageType.Beakers);
		if (cache == null)
			cache = new AttrSource(sources, total, rate);
		else
			cache.modifyAttrSource(sources, total, rate);
		this.attributeCache.put(StorageType.Beakers, cache);
		return cache.total;
	}

	public double getBeakersCivtick() {
		return beakersCivtick;
	}

	public void setBeakersCivtick(double beakersCivtick) {
		this.beakersCivtick = beakersCivtick;
	}

	// ----------------- happiness

	/* Gets the basic amount of happiness for a town. */
	public AttrSource getAttrHappiness() {
		if (!attributeCache.containsKey(StorageType.Happy)) calcAttrHappiness();
		return attributeCache.get(StorageType.Happy);
	}

	public void calcAttrHappiness() {
		HashMap<String, Double> sources = new HashMap<String, Double>();
		double total = 0;

		/* Add happiness from town level. */
		double townlevel = CivSettings.townHappinessLevels.get(getLevel()).happiness;
		total += townlevel;
		sources.put("Base Happiness", townlevel);

		/* Grab any sources from buffs. */
		double goodiesWonders = town.getBuffManager().getEffectiveDouble("buff_hedonism");
		goodiesWonders += town.getBuffManager().getEffectiveDouble("buff_globe_theatre_happiness_to_towns");
		goodiesWonders += town.getBuffManager().getEffectiveDouble("buff_colosseum_happiness_to_towns");
		goodiesWonders += town.getBuffManager().getEffectiveDouble("buff_colosseum_happiness_for_town");
		sources.put("Goodies/Wonders", goodiesWonders);
		total += goodiesWonders;

		/* Add in base happiness if it exists. */
		if (this.baseHappy != 0) {
			sources.put("Base Happiness", this.baseHappy);
			total += baseHappy;
		}

		/* Grab happiness generated from culture. */
		double fromCulture = 0;
		for (CultureChunk cc : town.getCultureChunks()) {
			fromCulture += cc.getHappiness();
		}
		sources.put("Culture Biomes", fromCulture);
		total += fromCulture;

		/* Grab happiness generated from structures with components. */
		double structures = 0;
		for (Structure struct : town.BM.getStructures()) {
			for (Component comp : struct.attachedComponents) {
				if (comp instanceof AttributeBase) {
					AttributeBase as = (AttributeBase) comp;
					if (as.getString("attribute").equalsIgnoreCase("HAPPINESS")) {
						structures += as.getGenerated();
					}
				}
			}
		}
		total += structures;
		sources.put("Structures", structures);

		double randomEvent = RandomEvent.getHappiness(town);
		total += randomEvent;
		sources.put("Random Events", randomEvent);

		if (total < 0) total = 0;

		// TODO Governments

		AttrSource cache = this.attributeCache.get(StorageType.Happy);
		if (cache == null)
			cache = new AttrSource(sources, total, null);
		else
			cache.modifyAttrSource(sources, total, null);
		this.attributeCache.put(StorageType.Happy, cache);
		return;
	}

	/* Gets the basic amount of unhappiness for a town. */
	public AttrSource getAttrUnhappiness() {
		if (!attributeCache.containsKey(StorageType.Unhappy)) calcAttrUnhappiness();
		return attributeCache.get(StorageType.Unhappy);
	}

	public void calcAttrUnhappiness() {
		HashMap<String, Double> sources = new HashMap<String, Double>();

		/* Get the unhappiness from the civ. */
		double total = town.getCiv().getCivWideUnhappiness(sources);

		/* Get unhappiness from residents. */
		double per_resident = 0;
		try {
			per_resident = CivSettings.getDouble(CivSettings.happinessConfig, "happiness.per_resident");
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
		}

		double happy_resident = per_resident * town.getResidents().size();
		sources.put("Residents", (happy_resident));
		total += happy_resident;

		/* Try to reduce war unhappiness via the component. */
		if (sources.containsKey("War")) {
			for (Structure struct : town.BM.getStructures()) {
				for (Component comp : struct.attachedComponents) {
					if (!comp.isActive()) continue;

					if (comp instanceof AttributeWarUnhappiness) {
						AttributeWarUnhappiness warunhappyComp = (AttributeWarUnhappiness) comp;
						double value = sources.get("War"); // Negative if a reduction
						value += warunhappyComp.value;

						if (value < 0) value = 0;

						sources.put("War", value);
					}
				}
			}
		}

		/* Add in base unhappiness if it exists. */
		if (this.baseUnhappy != 0) {
			sources.put("Base Unhappiness", this.baseUnhappy);
			total += this.baseUnhappy;
		}

		/* Grab unhappiness generated from structures with components. */
		double structures = 0;
		for (Structure struct : town.BM.getStructures()) {
			for (Component comp : struct.attachedComponents) {
				if (comp instanceof AttributeBase) {
					AttributeBase as = (AttributeBase) comp;
					if (as.getString("attribute").equalsIgnoreCase("UNHAPPINESS")) {
						structures += as.getGenerated();
					}
				}
			}
		}
		total += structures;
		sources.put("Structures", structures);

		/* Grab unhappiness from Random events. */
		double randomEvent = RandomEvent.getUnhappiness(town);
		total += randomEvent;
		if (randomEvent > 0) {
			sources.put("Random Events", randomEvent);
		}

		// TODO Spy Missions
		// TODO Governments

		if (total < 0) total = 0;

		AttrSource cache = this.attributeCache.get(StorageType.Unhappy);
		if (cache == null)
			cache = new AttrSource(sources, total, null);
		else
			cache.modifyAttrSource(sources, total, null);
		this.attributeCache.put(StorageType.Unhappy, cache);
		return;
	}

	public double getHappinessPercentage() {
		double total_happiness = getAttrHappiness().total;
		double total_unhappiness = getAttrUnhappiness().total;

		double total = total_happiness + total_unhappiness;
		return total_happiness / total;
	}

}