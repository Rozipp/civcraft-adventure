package com.avrgaming.civcraft.object;

import java.util.HashMap;

public class AttrRate {
	/* Contains a list of sources and the total. */
	public HashMap<String, Double> sources;
	public double total;

	public AttrRate(HashMap<String, Double> sources, double total) {
		this.sources = sources;
		this.total = total;
	}
}
