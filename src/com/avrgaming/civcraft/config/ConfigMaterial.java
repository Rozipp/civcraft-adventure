package com.avrgaming.civcraft.config;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public abstract class ConfigMaterial {

	/* Required */
	public String id;
	public int item_id;
	public int item_data;
	public String name;

	/* Optional */
	public String[] lore = null;
//	public String required_tech = null;
	public boolean shiny = false;
	public boolean tradeable = false;
	public List<HashMap<String, String>> components = new LinkedList<>();
	public double tradeValue = 0;
}
