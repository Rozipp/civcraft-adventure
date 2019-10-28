package com.avrgaming.civcraft.components;

public class AttributeRate extends AttributeBaseRate {

	@Override
	public double getGenerated() {
		if (this.getConstruct().isActive()) {
			return super.getDouble("value");
		} else {
			return 0.0;
		}	
	}

}
