package com.avrgaming.civcraft.object;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.HashMap;

import com.avrgaming.civcraft.components.AttributeStatic;
import com.avrgaming.civcraft.components.AttributeWarUnhappiness;
import com.avrgaming.civcraft.components.Component;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigCultureLevel;
import com.avrgaming.civcraft.construct.structures.Mine;
import com.avrgaming.civcraft.construct.structures.Structure;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.randomevents.RandomEvent;

public class TownStorageManager {

	public enum StorageType {
		GROWTH, FOODS, HAMMERS, SUPPLIES, CULTURE, COINS, BEAKERS, HAPPY, UNHAPPY,
	}

	private final Town town;

	private int foodBasket = 0;
	private int supplies = 600;
	private double cultureTotal = 0;

	public double baseHammers = 1.0;
	public double baseGrowth = 0.0;
	public double baseHappy = 0.0;
	public double baseUnhappy = 0.0;

	private EnumMap<StorageType, AttrSource> lastAttrCache = new EnumMap<>(StorageType.class);

	public TownStorageManager(Town town) {
		this.town = town;
		this.setBaseHammers(1.0);
	}

	public TownStorageManager(Town town, ResultSet rs) throws SQLException {
		this.town = town;
		this.foodBasket = rs.getInt("foodBasket");
		this.supplies = rs.getInt("hammers");
		this.cultureTotal = rs.getDouble("culture");
	}

	public void saveNow(HashMap<String, Object> hashmap) {
		hashmap.put("foodBasket", this.getFoodBasket());
		hashmap.put("hammers", this.getSupplies());
		hashmap.put("culture", this.getCulture());
	}

	public void onHourlyUpdate() {
	}

	/** В конце цивтика подсчитываем сколько максимально можно было принести еды и материала. Подсчитываем сколько принесли професионалы и
	 * бесдельники. Распределяем по складам города и цивилизации добытое. Даем кушать всем жителям */
	public void onCivtickUpdate() {
		calcAttrGrowth();
		calcAttrFood();
		calcAttrHammer();
		calcAttrSupplies();
		calcAttrCulture();
		calcAttrCoins();
		calcAttrBeakers();

		changeFoods((int) getAttr(StorageType.FOODS).total);
		depositSupplies((int) getAttr(StorageType.SUPPLIES).total);
		addCulture(getAttr(StorageType.CULTURE).total);
		town.getTreasury().deposit(getAttr(StorageType.COINS).total);
	}

	public AttrSource getAttr(StorageType type) {
		if (!lastAttrCache.containsKey(type)) {
			switch (type) {
			case GROWTH:
				calcAttrGrowth();
				break;
			case FOODS:
				calcAttrFood();
				break;
			case HAMMERS:
				calcAttrHammer();
				break;
			case SUPPLIES:
				calcAttrSupplies();
				break;
			case CULTURE:
				calcAttrCulture();
				break;
			case COINS:
				calcAttrCoins();
				break;
			case BEAKERS:
				calcAttrBeakers();
				break;
			case HAPPY:
				calcAttrHappiness();
				break;
			case UNHAPPY:
				calcAttrUnhappiness();
				break;
			}
		}
		return this.lastAttrCache.get(type);
	}

	// -------------- growth

	public void setBaseGrowth(double baseGrowth) {
		this.baseGrowth = baseGrowth;
	}
	
	public AttrRate calcAttrGrowthRate() {
		double rate = 1.0;
		HashMap<String, Double> rates = new HashMap<>();

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

	public void calcAttrGrowth() {
		double total = 0;
		HashMap<String, Double> sources = new HashMap<>();

		/* Grab any growth from culture. */
		double cultureSource = 0;
		for (CultureChunk cc : town.getCultureChunks()) {
			cultureSource += cc.getGrowth();
		}
		sources.put("Culture Biomes", cultureSource);
		total += cultureSource;

		/* Grab any growth from structures. */
		double structures = 0;
		for (Structure struct : town.BM.getStructures()) {
			if (struct.isWork()) {
				AttributeStatic as = (AttributeStatic) struct.getComponent("AttributeStatic");
				if (as != null) structures += as.getGenerated(StorageType.GROWTH);
			}
		}
		total += structures;
		sources.put("Structures", structures);

		// double fromBurj = 0;
		// for (final Town town : town.getCiv().getTowns()) {
		// if (town.BM.hasWonder("w_burj")) {
		// fromBurj = 1000.0;
		// break;
		// }
		// }
		// if (fromBurj != 0) {
		// sources.put("Wonders", fromBurj);
		// total += fromBurj;
		// }

		sources.put("Base Growth", baseGrowth);
		total += baseGrowth;

		AttrRate rate = this.calcAttrGrowthRate();
		total *= rate.total;

		if (total < 0) total = 0;
		this.lastAttrCache.put(StorageType.GROWTH, new AttrSource(sources, total, rate));
	}

	// ------------ food

	private void changeFoods(int food) {
		if (food == 0) return;
		foodBasket += food;
		while (foodBasket >= getFoodBasketSize()) {
			foodBasket -= getFoodBasketSize();
			town.PM.bornPeoples(1);
		}
		while (foodBasket < 0) {
			foodBasket += getFoodBasketSize() * 0.2;// TODO перенести в константу
			town.PM.deadPeoples(1);
		}
	}

	public int getFoodBasketSize() {
		return 2000 + town.PM.getPeoplesTotal() * 1000; // TODO перенести в константу
	}

	public int getFoodBasket() {
		return foodBasket;
	}

	public AttrRate calcAttrFoodRate() {
		double rate = 1.0;
		HashMap<String, Double> rates = new HashMap<>();

		/* Government */
		double newRate = rate;// * town.getGovernment().hammer_rate;
		rates.put("Government", newRate - rate);
		rate = newRate;

		return new AttrRate(rates, rate);
	}

	public void calcAttrFood() {
		double total = 0;
		HashMap<String, Double> sources = new HashMap<>();

		int formPeoples = (int) Math.min(getAttr(StorageType.GROWTH).total, town.PM.getIntake(StorageType.FOODS));
		total += formPeoples;
		sources.put("Peoples", (double) formPeoples);

		/* Grab hammers generated from structures with components. */
		double structures = 0;
		for (Structure struct : town.BM.getStructures()) {
			if (struct.getProfesionalComponent() == null || struct.getProfesionalComponent().isWork) {
				AttributeStatic as = (AttributeStatic) struct.getComponent("AttributeStatic");
				if (as != null) structures += as.getGenerated(StorageType.FOODS);
			}
		}
		total += structures;
		sources.put("Structures", structures);

		AttrRate rate = calcAttrFoodRate();
		total *= rate.total;

		int eat = town.PM.getFoodsOuttake();
		total -= eat;
		sources.put("Peoples eat", (double) eat);

		lastAttrCache.put(StorageType.FOODS, new AttrSource(sources, total, rate));
	}

	// -------------- Hammers

	public void setBaseHammers(double baseHammers) {
		this.baseHammers = baseHammers;
	}
	
	public AttrRate calcAttrHammerRate() {
		double rate = 1.0;
		HashMap<String, Double> rates = new HashMap<>();

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
		rate += newRate;

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
		HashMap<String, Double> sources = new HashMap<>();

		double cultureHammers = 0;
		for (CultureChunk cc : town.getCultureChunks()) {
			cultureHammers += cc.getHammers();
		}
		sources.put("Culture Biomes", cultureHammers);
		total += cultureHammers;

		/* Grab hammers generated from structures with components. */
		double structures = 0;
		for (Structure struct : town.BM.getStructures()) {
			if (struct.getProfesionalComponent() == null || struct.getProfesionalComponent().isWork) {
				AttributeStatic as = (AttributeStatic) struct.getComponent("AttributeStatic");
				if (as != null) structures += as.getGenerated(StorageType.HAMMERS);
			}
		}

		total += structures;
		sources.put("Structures", structures);
		total += this.baseHammers;
		sources.put("Base Hammers", this.baseHammers);

		AttrRate rate = calcAttrHammerRate();
		total *= rate.total;

		if (total < this.baseHammers) total = this.baseHammers;
		lastAttrCache.put(StorageType.HAMMERS, new AttrSource(sources, total, rate));
	}

	// --------------- Supplies

	public int getSupplies() {
		return supplies;
	}

	public void depositSupplies(int supplies) {
		this.supplies += supplies;
	}

	public int withdrawSupplies(int neadSupplies) {
		int withraw = Math.min(neadSupplies, this.supplies);
		this.supplies -= withraw;
		return withraw;
	}

	public AttrRate calcAttrSuppliesRate() {
		double rate = 1.0;
		HashMap<String, Double> rates = new HashMap<>();

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

	public void calcAttrSupplies() {
		double total = 0;
		HashMap<String, Double> sources = new HashMap<>();

		int fromPeople = (int) Math.min(getAttr(StorageType.HAMMERS).total, town.PM.getIntake(StorageType.SUPPLIES));
		total += fromPeople;
		sources.put("Peoples", (double) fromPeople);

		double structures = 0.0;
		for (Structure struct : town.BM.getStructures()) {
			if (struct instanceof Mine) {
				if (struct.getProfesionalComponent().isWork) {
					Mine mine = (Mine) struct;
					structures = structures + mine.getBonusHammers();
				}
			}
		}
		sources.put("Structures", structures);
		total += structures;

		AttrRate rate = calcAttrSuppliesRate();
		total *= rate.total;

		lastAttrCache.put(StorageType.SUPPLIES, new AttrSource(sources, total, rate));
	}

	// ------------- culture

	public void addCulture(double generated) {
		this.cultureTotal += generated;
		town.save();

		ConfigCultureLevel clc = CivSettings.cultureLevels.get(this.getLevel());
		if (this.getLevel() != CivSettings.getMaxCultureLevel()) {
			if (this.cultureTotal >= clc.amount) {
				CivGlobal.processCulture();
				CivMessage.sendCiv(town.getCiv(), CivSettings.localize.localizedString("var_town_bordersExpanded", town.getName()));
			}
		}
	}

	public int getCulture() {
		return (int) cultureTotal;
	}

	public int getLevel() {
		/* Get the first level */
		int bestLevel = 0;
		ConfigCultureLevel configLevel = CivSettings.cultureLevels.get(0);

		while (this.cultureTotal >= configLevel.amount) {
			configLevel = CivSettings.cultureLevels.get(bestLevel + 1);
			if (configLevel == null) {
				configLevel = CivSettings.cultureLevels.get(bestLevel);
				break;
			}
			bestLevel++;
		}
		return configLevel.level;
	}

	public AttrRate calcAttrCultureRate() {
		double rate = 1.0;
		HashMap<String, Double> rates = new HashMap<>();

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

	public void calcAttrCulture() {
		double total = 0;
		HashMap<String, Double> sources = new HashMap<>();
		/* Grab beakers generated from structures with components. */
		double fromStructures = 0;
		for (Structure struct : town.BM.getStructures()) {
			if (struct.isWork()) {
				AttributeStatic as = (AttributeStatic) struct.getComponent("AttributeStatic");
				if (as != null) fromStructures += as.getGenerated(StorageType.CULTURE);
			}
		}
		total += fromStructures;
		sources.put("Structures", fromStructures);

		double fromPeople = town.PM.getIntake(StorageType.CULTURE);
		total += fromPeople;
		sources.put("Populate", fromPeople);

		AttrRate rate = this.calcAttrCultureRate();
		total *= rate.total;

		if (total < 0) total = 0;
		this.lastAttrCache.put(StorageType.CULTURE, new AttrSource(sources, total, rate));
	}

	// --------------- econ
	
	public AttrRate calcAttrCoinsRate() {
		double rate = 1.0;
		HashMap<String, Double> rates = new HashMap<>();

		double newRate = town.getGovernment().trade_rate;
		rates.put("Government", newRate - rate);
		rate = newRate;

//		double structures = 0;
//		if (town.getBuffManager().hasBuff("buff_art_appreciation")) structures += town.getBuffManager().getEffectiveDouble("buff_art_appreciation");
//		rates.put("Great Works", structures);
//		rate += structures;
//
//		double additional = 0;
//		if (town.getBuffManager().hasBuff("buff_fine_art")) additional += town.getBuffManager().getEffectiveDouble(Buff.FINE_ART);
//		if (town.getBuffManager().hasBuff("buff_pyramid_culture")) additional += town.getBuffManager().getEffectiveDouble("buff_pyramid_culture");
//		if (town.getBuffManager().hasBuff("buff_neuschwanstein_culture")) additional += town.getBuffManager().getEffectiveDouble("buff_neuschwanstein_culture");
//
//		rates.put("Wonders/Goodies", additional);
//		rate += additional;

		return new AttrRate(rates, rate);
	}

	public void calcAttrCoins() {
		double total = 0;
		HashMap<String, Double> sources = new HashMap<>();
		/* Grab beakers generated from structures with components. */
		double fromStructures = 0;
		for (Structure struct : town.BM.getStructures()) {
			if (struct.isWork()) {
				AttributeStatic as = (AttributeStatic) struct.getComponent("AttributeStatic");
				if (as != null) fromStructures += as.getGenerated(StorageType.COINS);
			}
		}
		total += fromStructures;
		sources.put("Structures", fromStructures);

		double fromPeople = town.PM.getIntake(StorageType.COINS);
		total += fromPeople;
		sources.put("Populate", fromPeople);

		AttrRate rate = this.calcAttrCultureRate();
		total *= rate.total;

		if (total < 0) total = 0;
		this.lastAttrCache.put(StorageType.COINS, new AttrSource(sources, total, rate));
	}
	
	// --------------- beaker

	public AttrRate calcAttrBeakerRate() {
		double rate = 1.0;
		HashMap<String, Double> rates = new HashMap<>();

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

	public double calcAttrBeakers() {
		double total = 0;
		HashMap<String, Double> sources = new HashMap<>();

		/* Grab beakers generated from structures with components. */
		double fromStructures = 0;
		for (Structure struct : town.BM.getStructures()) {
			if (struct.isWork()) {
				AttributeStatic as = (AttributeStatic) struct.getComponent("AttributeStatic");
				if (as != null) fromStructures += as.getGenerated(StorageType.BEAKERS);
			}
		}
		total += fromStructures;
		sources.put("Structures", fromStructures);

		// /* Grab any extra beakers from buffs. */
		// double wondersTrade = 0;
		// // No more flat bonuses here, leaving it in case of new buffs
		// if (town.getBuffManager().hasBuff("buff_advanced_mixing")) wondersTrade += 150.0;
		// total += wondersTrade;
		// sources.put("Goodies/Wonders", wondersTrade);

		double fromPeople = town.PM.getIntake(StorageType.BEAKERS);
		total += fromPeople;
		sources.put("Populate", fromPeople);

		AttrRate rate = calcAttrBeakerRate();
		total = total * rate.total;

		if (total < 0) total = 0;
		this.lastAttrCache.put(StorageType.BEAKERS, new AttrSource(sources, total, rate));
		return total;
	}

	// ----------------- happiness

	/* Gets the basic amount of happiness for a town. */

	public void calcAttrHappiness() {
		HashMap<String, Double> sources = new HashMap<>();
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
			AttributeStatic as = (AttributeStatic) struct.getComponent("AttributeStatic");
			if (as != null) structures += as.getGenerated(StorageType.HAPPY);
		}
		total += structures;
		sources.put("Structures", structures);

		double randomEvent = RandomEvent.getHappiness(town);
		total += randomEvent;
		sources.put("Random Events", randomEvent);

		if (total < 0) total = 0;

		// TODO Governments
		this.lastAttrCache.put(StorageType.HAPPY, new AttrSource(sources, total, null));
	}

	/* Gets the basic amount of unhappiness for a town. */

	public void calcAttrUnhappiness() {
		HashMap<String, Double> sources = new HashMap<>();

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
			AttributeStatic as = (AttributeStatic) struct.getComponent("AttributeStatic");
			if (as != null) structures += as.getGenerated(StorageType.UNHAPPY);
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
		this.lastAttrCache.put(StorageType.UNHAPPY, new AttrSource(sources, total, null));
	}

	public double getHappinessPercentage() {
		double total_happiness = getAttr(StorageType.HAPPY).total;
		double total_unhappiness = getAttr(StorageType.UNHAPPY).total;

		double total = total_happiness + total_unhappiness;
		return total_happiness / total;
	}

}