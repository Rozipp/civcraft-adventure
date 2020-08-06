package com.avrgaming.civcraft.object;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivMessage;

public class AttrSource {

	/* Contains a list of sources and the total. */
	public HashMap<String, Double> sources;
	public double total;
	AttrRate rate;
	public Date lastUpdate;

	public AttrSource(HashMap<String, Double> sources, double total, AttrRate rate) {
		this.sources = sources;
		this.total = total;
		this.rate = rate;
		this.lastUpdate = new Date();
	}

	public void modifyAttrSource(HashMap<String, Double> sources, double total, AttrRate rate) {
		for (String name : sources.keySet()) {
			this.sources.put(name, sources.get(name));
		}
		this.total = total;
		this.rate = rate;
		this.lastUpdate = new Date();
	}

	public AttrRate getRate() {
		return rate;
	}

	public ArrayList<String> getSourceDisplayString(String sourceColor, String valueColor) {
		ArrayList<String> out = new ArrayList<String>();
		DecimalFormat df = new DecimalFormat();

		out.add(CivMessage.buildSmallTitle(CivSettings.localize.localizedString("town_info_sources")));

		for (String source : sources.keySet()) {
			out.add(sourceColor + source + ": " + valueColor + df.format(sources.get(source)));
		}

		return out;
	}

	public ArrayList<String> getRateDisplayString(String sourceColor, String valueColor) {
		ArrayList<String> out = new ArrayList<String>();
		DecimalFormat df = new DecimalFormat();

		if (rate != null) {
			out.add(CivMessage.buildSmallTitle(CivSettings.localize.localizedString("town_info_rates")));

			for (String source : rate.sources.keySet()) {
				out.add(sourceColor + source + ": " + valueColor + df.format(rate.sources.get(source) * 100) + "%");
			}
		}
		return out;
	}

	public ArrayList<String> getTotalDisplayString(String sourceColor, String valueColor) {
		ArrayList<String> out = new ArrayList<String>();
		DecimalFormat df = new DecimalFormat();

		out.add(CivMessage.buildSmallTitle(CivSettings.localize.localizedString("town_info_totals")));
		out.add(sourceColor + "Total: " + valueColor + df.format(this.total) + sourceColor);
		return out;
	}

}
