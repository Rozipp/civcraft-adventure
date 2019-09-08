// 
// Decompiled by Procyon v0.5.30
// 

package com.avrgaming.civcraft.config;

import java.util.List;
import com.avrgaming.civcraft.main.CivLog;
import java.util.Map;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigAlchLevel
{
    public int level;
    public String itemName;
    public int itemId;
    public int itemData;
    public int amount;
    public double price;
    
    public static void loadConfig(final FileConfiguration cfg, final Map<Integer, ConfigAlchLevel> levels) {
        levels.clear();
        List<Map<?, ?>> culture_levels = cfg.getMapList("alch_levels");
        for (final Map<?, ?> level : culture_levels) {
            ConfigAlchLevel alch_level = new ConfigAlchLevel();
            alch_level.level = (int)level.get("level");
            alch_level.itemName = (String)level.get("itemName");
            alch_level.itemId = (int)level.get("itemId");
            alch_level.itemData = (int)level.get("itemData");
            alch_level.amount = (int)level.get("amount");
            alch_level.price = (double)level.get("price");
            levels.put(alch_level.level, alch_level);
        }
        CivLog.info("Loaded " + levels.size() + " alch levels.");
    }
}
