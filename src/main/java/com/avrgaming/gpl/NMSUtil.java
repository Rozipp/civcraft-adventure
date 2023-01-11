package com.avrgaming.gpl;

import net.minecraft.server.v1_12_R1.*;
import org.bukkit.craftbukkit.v1_12_R1.util.*;
import java.lang.reflect.*;
public class NMSUtil
{
    @SuppressWarnings({ "null", "rawtypes" })
	public static void clearPathfinderGoals(final PathfinderGoalSelector selector) {
        try {
            Field gsa = PathfinderGoalSelector.class.getDeclaredField("b");
            gsa.setAccessible(true);
            gsa.set(selector, new UnsafeList());
            gsa = PathfinderGoalSelector.class.getDeclaredField("c");
            gsa.setAccessible(true);
            gsa.set(selector, new UnsafeList());
        }
        catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex2) {
            final Exception ex = null;
            final Exception e = ex;
            e.printStackTrace();
        }
    }
}
