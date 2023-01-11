package com.avrgaming.civcraft.construct;

import org.bukkit.World;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.BlockCoord;

public interface ConstructDamageBlock {
	public Construct getOwner();
	public void setOwner(Construct owner);
	public Town getTown();
	public Civilization getCiv();
	public BlockCoord getCoord();
	public void setCoord(BlockCoord coord);
	public World getWorld();
	public boolean isDamageable();
	public void setDamageable(boolean damageable);
	public boolean canDestroyOnlyDuringWar();
	public boolean allowDamageNow(Player player);
}