package com.avrgaming.civcraft.fishing;

import org.bukkit.entity.*;
import org.bukkit.*;

public class AntiAutofish implements Runnable
{
    public final Player player;
    
    public AntiAutofish(final Player player) {
        this.player = player;
    }
    
    @Override
    public void run() {
    	World world = this.player.getLocation().getWorld();
        world.playSound(this.player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 10.0f, 1.0f);
        world.playSound(this.player.getLocation(), Sound.ENTITY_ARROW_HIT, 10.0f, 1.0f);
        world.playSound(this.player.getLocation(), Sound.ENTITY_WOLF_HOWL, 10.0f, 1.0f);
        world.playSound(this.player.getLocation(), Sound.ENTITY_COW_DEATH, 10.0f, 1.0f);
        world.playSound(this.player.getLocation(), Sound.ENTITY_ENDERDRAGON_FIREBALL_EXPLODE, 0.1f, 1.0f);
        world.playSound(this.player.getLocation(), Sound.ENTITY_ENDERDRAGON_DEATH, 8.1f, 1.0f);
        world.playSound(this.player.getLocation(), Sound.ENTITY_ENDERMEN_DEATH, 6.1f, 1.0f);
        world.playSound(this.player.getLocation(), Sound.ENTITY_ENDERMEN_AMBIENT, 5.1f, 1.0f);
    }
}
