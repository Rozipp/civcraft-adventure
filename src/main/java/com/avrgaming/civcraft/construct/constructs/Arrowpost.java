package com.avrgaming.civcraft.construct.constructs;

import com.avrgaming.civcraft.components.ProjectileArrowComponent;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.Construct;
import com.avrgaming.civcraft.construct.ConstructDamageBlock;
import com.avrgaming.civcraft.construct.Direction;
import com.avrgaming.civcraft.construct.Template;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.SimpleBlock;
import com.avrgaming.civcraft.war.War;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Arrowpost extends Construct {

	public static HashMap<BlockCoord, Construct> arrowposts = new HashMap<>();

	ProjectileArrowComponent arrowComponent;
	Set<BlockCoord> turretLocation = new HashSet<>();

	public static void cleanupAll() {
		for (Construct arrowpost : arrowposts.values()) {
			arrowpost.deleteWithUndo();
		}
	}

	public Arrowpost(Player player) {
		super("c_arrowpost", CivGlobal.getResident(player).getCiv());
	}

	public static void newArrowpost(Player player) throws CivException {
		Resident resident = CivGlobal.getResident(player);
		if (player.isOp())
			CivMessage.send(player, "Поскольку вы оп, то можете строить пушки в мирное время");
		else {
			if (!War.isWarTime()) throw new CivException(CivSettings.localize.localizedString("buildCannon_NotWar"));
			if (!resident.getCiv().getDiplomacyManager().isAtWar()) throw new CivException("Ваша цивилизация не участвует в войне");
		}
		if (resident.getTown() == null) throw new CivException("У вас нет города");
		Arrowpost arrowpost = new Arrowpost(player);
		arrowpost.initDefaultTemplate(player.getLocation());
		arrowpost.checkBlockPermissionsAndRestrictions(player);

		CivMessage.sendCiv(resident.getCiv(), CivSettings.localize.localizedString("var_buildCannon_Success", arrowpost.getCorner().toStringNotWorld()));
		arrowpost.build(player);
	}

	private void destroy() {
		for (BlockCoord b : this.constructBlocks.keySet()) {
			if (b.getBlock().getType() == Material.AIR) continue;
			double rand = CivCraft.civRandom.nextDouble();
			if (rand < 0.2)
				ItemManager.setTypeIdAndData(b.getBlock(), CivData.GRAVEL, 0, false);
			else
				ItemManager.setTypeIdAndData(b.getBlock(), CivData.AIR, 0, false);
		}
		this.delete();
	}

	@Override
	public void commandBlockRelatives(BlockCoord absCoord, SimpleBlock sb) {
		if (sb.command.equals("/towerfire")) {
			turretLocation.add(absCoord);
		}
	}

	@Override
	public void onPostBuild() {
		arrowComponent = new ProjectileArrowComponent(this);
		arrowComponent.createComponent(this);
		arrowComponent.setTurretLocation(turretLocation);
		arrowComponent.setDamage(arrowComponent.getDamage() / 2);
	}

	@Override
	public void build(Player player) throws CivException {
		this.checkBlockPermissionsAndRestrictions(player);
		try {
			this.getTemplate().saveUndoTemplate(this.getCorner().toString(), this.getCorner());
		} catch (IOException var8) {
			var8.printStackTrace();
		}
		this.getTemplate().buildTemplate(corner);
		this.postBuild();
		Arrowpost.arrowposts.put(this.getCorner(), this);
	}

	@Override
	public String getMarkerIconName() {
		return null;
	}

	@Override
	public void onDamage(int amount, Player player, ConstructDamageBlock hit) {
		this.setHitpoints(this.getHitpoints() - amount);

		if (getHitpoints() <= 0) {
			destroy();
		}
	}

	@Override
	public void onDamageNotification(Player player, ConstructDamageBlock hit) {
	}

	@Override
	public void load(ResultSet rs) {
	}

	@Override
	public void saveNow() {
	}

	@Override
	public Location repositionCenter(Location center, Template tpl) {
		Location loc = center.clone();
		Direction dir = tpl.getDirection();
		double x_size = tpl.getSize_x();
		double z_size = tpl.getSize_z();
		switch (dir) {
			case east:
				loc.setZ(loc.getZ() - (z_size / 2));
				loc.setX(loc.getX());
				break;
			case south:
				loc.setX(loc.getX() - (x_size / 2));
				loc.setZ(loc.getZ());
				break;
			case west:
				loc.setZ(loc.getZ() - (z_size / 2));
				loc.setX(loc.getX() - (x_size));
				break;
			case north:
				loc.setX(loc.getX() - (x_size / 2));
				loc.setZ(loc.getZ() - (z_size));
				break;
		}

		return loc;
	}
}
