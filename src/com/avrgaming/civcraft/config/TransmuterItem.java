package com.avrgaming.civcraft.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.bukkit.configuration.file.FileConfiguration;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivLog;

public class TransmuterItem {

	public ReentrantLock lock = new ReentrantLock();
	public String id;
	public String resultChest;
	public LinkedList<SourceItem> sourceItems;
	public LinkedList<ResultItem> resultItems;

	public TransmuterItem() {
		sourceItems = new LinkedList<SourceItem>();
		resultItems = new LinkedList<ResultItem>();
	}

	public TransmuterItem(String id, String resultChest) {
		this.id = id;
		this.resultChest = resultChest;
		sourceItems = new LinkedList<SourceItem>();
		resultItems = new LinkedList<ResultItem>();
	}

	public void addSourceItem(String item, int amount, String chest) {
		SourceItem si = new SourceItem();
		si.item = item;
		si.count = amount;
		si.chest = chest;
		sourceItems.add(si);
	}

	public void addResultItem(String resultItem, int resultAmount, int resultRate) {
		ResultItem ri = new ResultItem();
		ri.item = resultItem;
		ri.count = resultAmount;
		ri.rate = resultRate;
		resultItems.add(ri);
	}

	public Collection<SourceItem> getSourceItems() {
		return sourceItems;
	}

	public Collection<ResultItem> getResultItems() {
		return resultItems;
	}

	public int getAllRate() {
		int sum = 0;
		for (ResultItem ri : resultItems)
			sum = sum + ri.rate;
		return sum;
	}

	public static TransmuterItem getTransmuterItemForId(ArrayList<TransmuterItem> transmuterItems, String id) {
		for (TransmuterItem tI : transmuterItems)
			if (tI.id.equalsIgnoreCase(id)) return tI;
		return null;
	};

	@SuppressWarnings("unchecked")
	public static void loadConfig(FileConfiguration cfg, ArrayList<TransmuterItem> transmuterItems) {
		transmuterItems.clear();
		List<Map<?, ?>> configTransmuter = cfg.getMapList("transmuter");
		for (Map<?, ?> b : configTransmuter) {
			TransmuterItem ct = new TransmuterItem();
			try {
				/* Mandatory Settings */
				if ((ct.id = (String) b.get("id")) == null) throw new CivException("null елемент в id");
				if ((ct.resultChest = (String) b.get("result_chest")) == null) throw new CivException("null елемент в result_chest");
				List<Map<?, ?>> configSourceItems = (List<Map<?, ?>>) b.get("source_item");
				if (configSourceItems != null) {
					for (Map<?, ?> ingred : configSourceItems) {
						SourceItem sourceItem = new SourceItem();
						if ((sourceItem.item = (String) ingred.get("item")) == null) throw new CivException("null елемент в item");
						if ((sourceItem.count = (Integer) ingred.get("count")) == 0) throw new CivException("0 елемент в count");
						if ((sourceItem.chest = (String) ingred.get("chest")) == null) throw new CivException("null елемент в chest");
						ct.sourceItems.add(sourceItem);
					}
				}
				List<Map<?, ?>> configResultItems = (List<Map<?, ?>>) b.get("result_item");
				if (configResultItems != null) {
					for (Map<?, ?> ingred : configResultItems) {
						ResultItem resultItem = new ResultItem();
						if ((resultItem.item = (String) ingred.get("item")) == null) throw new CivException("null елемент в item");
						if ((resultItem.count = (Integer) ingred.get("count")) == 0) throw new CivException("0 елемент в count");
						if ((resultItem.rate = (Integer) ingred.get("rate")) == 0) throw new CivException("0 елемент в rate");
						ct.resultItems.add(resultItem);
					}
				}
				transmuterItems.add(ct);
			} catch (CivException e) {
				CivLog.error("----ConfigTransmuterItem---" + e.getMessage() + " при чтении transmuter.id = " + ct.id);
			}
		}

		CivLog.info("Loaded " + transmuterItems.size() + " TransmuterItems");
	}
}
