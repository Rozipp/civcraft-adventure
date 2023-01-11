package com.avrgaming.civcraft.components;

import java.util.EnumMap;
import java.util.Map;

import com.avrgaming.civcraft.construct.Construct;
import com.avrgaming.civcraft.object.TownPeoplesManager.Prof;
import com.avrgaming.civcraft.object.TownStorageManager.StorageType;

public class ProfesionalComponent extends Component {

	public Prof prof;
	public Integer countMax;
	public Integer count;
	public boolean isWork = false;

	private Map<StorageType, Double> storageIntake = new EnumMap<>(StorageType.class);

	@Override
	public void createComponent(Construct constr, boolean async) {
		super.createComponent(constr, async);
		prof = Prof.valueOf(getString("prof"));
		try {
			countMax = Integer.parseInt(getString("max_count"));
		} catch (Exception e) {
			countMax = 1;
		}
		count = countMax;

		for (StorageType type : StorageType.values()) {
			String atr = getString(type.toString());
			if (atr == null)
				storageIntake.put(type, 0.0);
			else
				storageIntake.put(type, Double.parseDouble(atr));
		}
	}

	public int setCount(int count) {
		if (count > countMax)
			this.count = countMax;
		else
			this.count = count;
		return this.count;
	}

	public Double getStorage(StorageType type) {
		return storageIntake.get(type);
	}
	
}
