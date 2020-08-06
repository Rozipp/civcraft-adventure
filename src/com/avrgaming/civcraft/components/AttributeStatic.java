package com.avrgaming.civcraft.components;

public class AttributeStatic extends Component {
	public double getGenerated(AttributeTypeKeys atk) {
		if (!this.getString("attribute").equalsIgnoreCase(atk.name())) return 0.0;
		if (this.getConstruct().isActive())
			return super.getDouble("value");
		else
			return 0.0;
	}

	public enum AttributeTypeKeys {
		COINS, HAPPINESS, UNHAPPINESS, HAMMERS, GROWTH, BEAKERS, CULTURE, 
	}
}
