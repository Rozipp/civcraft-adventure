package com.avrgaming.civcraft.components;

import com.avrgaming.civcraft.object.TownStorageManager.StorageType;

public class AttributeStatic extends Component {
	public double getGenerated(StorageType atk) {
		if (!this.getString("attribute").equalsIgnoreCase(atk.name())) return 0.0;
		if (this.getConstruct().isActive())
			return super.getDouble("value");
		else
			return 0.0;
	}

}
