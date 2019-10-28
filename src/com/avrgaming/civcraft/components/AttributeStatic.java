package com.avrgaming.civcraft.components;

public class AttributeStatic extends AttributeBase {

	@Override
	public double getGenerated() {
		if (this.getConstruct().isActive()) {
			return super.getDouble("value");
		} else {
			return 0.0;
		}
	}
	
}
