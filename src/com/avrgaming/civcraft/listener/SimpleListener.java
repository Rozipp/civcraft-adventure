/*
 * The MIT License
 *
 * Copyright 2014 Goblom.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.avrgaming.civcraft.listener;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.fishing.FishingListener;
import com.avrgaming.civcraft.gui.GuiInventoryListener;
import com.avrgaming.civcraft.items.CraftableCustomMaterialListener;
import com.avrgaming.civcraft.listener.armor.ArmorListener;
import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.mythicmob.MobListener;
import com.avrgaming.civcraft.trade.TradeInventoryListener;
import com.avrgaming.civcraft.units.UnitListener;
import com.avrgaming.civcraft.war.WarListener;
import com.google.common.collect.Lists;

import pvptimer.PvPListener;

import java.util.Collections;
import java.util.List;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

/**
 * Make a listener extend this class then all you need to do is "new ExampleListener(myPlugin);"
 * @author Goblom
 */
public abstract class SimpleListener implements Listener {
    
    public static final List<SimpleListener> listeners = Lists.newArrayList();
    
    public static final void unregisterAll() {
        for (SimpleListener listener : listeners) {
            listener.unregister();
        }
    }
    
    public static final void registerAll() {
    	new BlockListener();
		new ChatListener();
		new MarkerPlacementManager();
		new CustomItemListener();
		new UnitInventoryListener();
		new PlayerListener();
		new DebugListener();
		new CraftableCustomMaterialListener();
		new GuiInventoryListener();

		Boolean useEXPAsCurrency = true;
		try {
			useEXPAsCurrency = CivSettings.getBoolean(CivSettings.civConfig, "global.use_exp_as_currency");
		} catch (InvalidConfiguration e) {
			CivLog.error("Unable to check if EXP should be enabled. Disabling.");
			e.printStackTrace();
		}
		if (useEXPAsCurrency) {
			new DisableXPListener();
		}
		new TradeInventoryListener();
		new WarListener();
		new FishingListener();
		new PvPListener();
		new UnitListener();
		new MobListener();
		
		ArmorListener.blockedMaterials = CivCraft.getPlugin().getConfig().getStringList("blocked");
		new ArmorListener();
		CivLog.info("Registred " +  SimpleListener.listeners.size() + " listeners");
    }
    
    public static final List<SimpleListener> getRegistered() {
        return Collections.unmodifiableList(listeners);
    }
            
    protected static Plugin plugin = CivCraft.getPlugin();
    
    public SimpleListener() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        SimpleListener.listeners.add(this);
    }
    
    public final void unregister() {
        HandlerList.unregisterAll(this);
    }
}
