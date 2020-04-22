package com.avrgaming.civcraft.structure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import org.bukkit.Location;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.TransmuterAsyncTask;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.SimpleBlock;

public class Quarry extends Structure {
	public static final int MAX_CHANCE = CivSettings.getIntegerStructure("quarry.max");
	public int skippedCounter = 0;
	public ReentrantLock lock = new ReentrantLock();

	public enum Mineral {
		RARE, TUNGSTEN, GOLD, REDSTONE, IRON, COAL, OTHER, COBBLESTONE
	}

	public Quarry(Location center, String id, Town town) throws CivException {
		super(center, id, town);
		setLevel(town.saved_quarry_level);
	}

	public Quarry(ResultSet rs) throws SQLException, CivException {
		super(rs);
		this.rebiuldTransmuterRecipe();
	}

	@Override
	public void onSecondUpdate() {
		if (!CivGlobal.quarriesEnabled) return;
		for (String ctrId : this.transmuterLocks.keySet()) {
			if (!this.transmuterLocks.get(ctrId).isLocked())
				TaskMaster.asyncTask("quarry-" + this.getCorner() + ";tr-" + ctrId, new TransmuterAsyncTask(this, CivSettings.transmuterRecipes.get(ctrId)), 0);
		}
	}

	@Override
	public String getDynmapDescription() {
		String out = "<u><b>" + this.getDisplayName() + "</u></b><br/>";
		out += CivSettings.localize.localizedString("Level") + " " + this.getLevel();
		return out;
	}

	@Override
	public String getMarkerIconName() {
		return "minecart";
	}

	@Override
	public double modifyTransmuterChance(Double chance) {
		double increase = chance * (this.getTown().getBuffManager().getEffectiveDouble(Buff.EXTRACTION)
				+ this.getTown().getBuffManager().getEffectiveDouble("buff_grandcanyon_quarry_and_trommel"));
		chance += increase;

		try {
			if (this.getTown().getGovernment().id.equals("gov_despotism")) {
				chance *= CivSettings.getDouble(CivSettings.structureConfig, "quarry.despotism_rate");
			} else
				if (this.getTown().getGovernment().id.equals("gov_theocracy") || this.getTown().getGovernment().id.equals("gov_monarchy")) {
					chance *= CivSettings.getDouble(CivSettings.structureConfig, "quarry.penalty_rate");
				}

		} catch (InvalidConfiguration e) {
			e.printStackTrace();
		}
		return chance;
	}

	@Override
	public ArrayList<String> getTransmuterRecipe() {
		ArrayList<String> result = new ArrayList<>();
		switch (getLevel()) {
			case 1 :
				result.add("quarry1");
				break;
			case 2 :
				result.add("quarry2");
				break;
			case 3 :
				result.add("quarry3");
				break;
			case 4 :
				result.add("quarry4");
				break;
			default :
				break;
		}
		return result;
	}

	@Override
	public void onPostBuild(BlockCoord absCoord, SimpleBlock commandBlock) {
		this.setLevel(getTown().saved_quarry_level);
		this.rebiuldTransmuterRecipe();
	}
}
