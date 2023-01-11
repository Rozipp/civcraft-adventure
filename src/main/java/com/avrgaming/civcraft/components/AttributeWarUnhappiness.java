package com.avrgaming.civcraft.components;

import com.avrgaming.civcraft.construct.Construct;

public class AttributeWarUnhappiness extends Component {

	public double value;
	/*
	 * This is another special case. We only want to use this to reduce war unhappiness. 
	 * We dont want to generate any actual happiness, just reduce the unhappiness caused from war.
	 */
	
	@Override
	public void createComponent(Construct constr, boolean async) {
		super.createComponent(constr, async);
		value = this.getDouble("value");
	}
	
}
