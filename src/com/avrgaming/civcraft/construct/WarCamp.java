package com.avrgaming.civcraft.construct;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.ControlPoint;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.structure.RespawnLocationHolder;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.FireworkEffectPlayer;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.SimpleBlock;
import com.avrgaming.civcraft.war.War;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class WarCamp extends Construct implements RespawnLocationHolder {

	public static final String RESTORE_NAME = "special:WarCamps";
	private ArrayList<BlockCoord> respawnPoints = new ArrayList<BlockCoord>();
	protected HashMap<BlockCoord, ControlPoint> controlPoints = new HashMap<BlockCoord, ControlPoint>();

	public static ConfigBuildableInfo info = new ConfigBuildableInfo("warcamp", "War Camp", false, "warcamp", false, 100, -1);
	public static int warCampMax = 5;
	static {
		try {
			warCampMax = CivSettings.getInteger(CivSettings.warConfig, "warcamp.max");
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
		}
	}

	public WarCamp(Resident resident) {
		this.setSQLOwner(resident.getCiv());
		this.setInfo(WarCamp.info);
	}

	public static WarCamp newWarCamp(Player player, Location location) throws CivException {
		Resident resident = CivGlobal.getResident(player);
		if (player.isOp())
			CivMessage.send(player, "Вы оп, потому можно строить в мирное время");
		else {
			if (!resident.hasTown()) throw new CivException(CivSettings.localize.localizedString("buildWarCamp_errorNotInCiv"));
			if (!resident.getCiv().getLeaderGroup().hasMember(resident) && !resident.getCiv().getAdviserGroup().hasMember(resident)) throw new CivException(CivSettings.localize.localizedString("buildWarCamp_errorNotPerms"));
			if (!War.isWarTime()) throw new CivException(CivSettings.localize.localizedString("buildWarCamp_errorNotWarTime"));
			if (resident.getCiv().getWarCamps().size() >= warCampMax) throw new CivException(CivSettings.localize.localizedString("var_warcamp_maxReached", warCampMax));
		}
		WarCamp warCamp = new WarCamp(resident);
		warCamp.initDefaultTemplate(location);
		warCamp.checkBlockPermissionsAndRestrictions(player);
		return warCamp;
	}

	// ----------- build
	public void createWarCamp(Player player) throws CivException {
		this.build(player);
		this.getCiv().addWarCamp(this);
		this.getCiv().setLastWarCampCreated(System.currentTimeMillis());

		CivMessage.sendSuccess(player, CivSettings.localize.localizedString("warcamp_createSuccess"));
		ItemStack newStack = new ItemStack(Material.AIR);
		player.getInventory().setItemInMainHand(newStack);
	}

	@Override
	public void checkBlockPermissionsAndRestrictions(Player player) throws CivException {
		int regionY = this.getTemplate().getSize_y();

		if (getCorner().getY() >= 200) throw new CivException(CivSettings.localize.localizedString("camp_checkTooHigh"));
		if ((regionY + getCorner().getY()) >= 255) throw new CivException(CivSettings.localize.localizedString("camp_checkWayTooHigh"));

		int minsLeft = this.isWarCampCooldownLeft();
		if (minsLeft > 0) throw new CivException(CivSettings.localize.localizedString("var_warcamp_oncooldown", minsLeft));

		super.checkBlockPermissionsAndRestrictions(player);
	}

	@Override
	public void commandBlockRelatives(BlockCoord absCoord, SimpleBlock sb) {
		switch (sb.command) {
		case "/respawn":
			this.respawnPoints.add(absCoord);
			BlockCoord coord = new BlockCoord(absCoord);
			ItemManager.setTypeId(coord.getBlock(), CivData.AIR);
			this.addConstructBlock(new BlockCoord(absCoord), false);

			coord = new BlockCoord(absCoord);
			coord.setY(absCoord.getY() + 1);
			ItemManager.setTypeId(coord.getBlock(), CivData.AIR);
			this.addConstructBlock(coord, false);

			break;
		case "/control":
			this.createControlPoint(absCoord, "");
			break;
		}
	}

	@Override
	public void processUndo() throws CivException {
	}

	@Override
	public String getDynmapDescription() {
		return null;
	}

	@Override
	public String getMarkerIconName() {
		return null;
	}

	@Override
	public void onLoad() throws CivException {
	}

	@Override
	public void onUnload() {
	}

	@Override
	public void load(ResultSet rs) throws SQLException, CivException {
	}

	@Override
	public void save() {
	}

	@Override
	public void saveNow() throws SQLException {
	}

	public void createControlPoint(BlockCoord absCoord, String info) {
		/* Build the bedrock tower. */
		Block b = absCoord.getBlock();
		ItemManager.setTypeIdAndData(b, CivData.FENCE, 0, true);
		this.addConstructBlock(new BlockCoord(b), true);

		/* Build the control block. */
		b = absCoord.getBlockRelative(0, 1, 0);
		ItemManager.setTypeId(b, CivData.OBSIDIAN);
		this.addConstructBlock(new BlockCoord(b), true);

		int controlBlockhitPoints;
		try {
			controlBlockhitPoints = CivSettings.getInteger(CivSettings.warConfig, "warcamp.control_block_hitpoints");
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			return;
		}

		BlockCoord coord = new BlockCoord(b);
		this.controlPoints.put(coord, new ControlPoint(coord, this, controlBlockhitPoints, info));
	}

	@Override
	public void onDamage(int amount, World world, Player player, BlockCoord coord, ConstructDamageBlock hit) {
		ControlPoint cp = this.controlPoints.get(coord);
		Resident resident = CivGlobal.getResident(player);

		if (cp != null)
			if (!cp.isDestroyed()) {
				if (resident.isControlBlockInstantBreak())
					cp.damage(cp.getHitpoints());
				else
					cp.damage(amount);
				if (cp.isDestroyed())
					onControlBlockDestroy(cp, world, player, (ConstructBlock) hit);
				else {
					world.playSound(hit.getCoord().getLocation(), Sound.BLOCK_ANVIL_USE, 0.2f, 1);
					world.playEffect(hit.getCoord().getLocation(), Effect.MOBSPAWNER_FLAMES, 0);
					CivMessage.send(player, CivColor.LightGray + CivSettings.localize.localizedString("warcamp_hitControlBlock") + " (" + cp.getHitpoints() + " / " + cp.getMaxHitpoints() + ")");
					CivMessage.sendCiv(getCiv(), CivColor.Yellow + CivSettings.localize.localizedString("warcamp_controlBlockUnderAttack"));
				}
			} else
				CivMessage.send(player, CivColor.Rose + CivSettings.localize.localizedString("camp_controlBlockAlreadyDestroyed"));
		else
			CivMessage.send(player, CivColor.Rose + CivSettings.localize.localizedString("structure_cannotDamage") + " " + this.getDisplayName() + ", " + CivSettings.localize.localizedString("structure_targetControlBlocks"));
	}

	public void onControlBlockDestroy(ControlPoint cp, World world, Player player, ConstructBlock hit) {
		// Should always have a resident and a town at this point.
		Resident attacker = CivGlobal.getResident(player);

		ItemManager.setTypeId(hit.getCoord().getLocation().getBlock(), CivData.AIR);
		world.playSound(hit.getCoord().getLocation(), Sound.BLOCK_ANVIL_BREAK, 1.0f, -1.0f);
		world.playSound(hit.getCoord().getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);

		FireworkEffect effect = FireworkEffect.builder().with(org.bukkit.FireworkEffect.Type.BURST).withColor(Color.OLIVE).withColor(Color.RED).withTrail().withFlicker().build();
		FireworkEffectPlayer fePlayer = new FireworkEffectPlayer();
		for (int i = 0; i < 3; i++) {
			try {
				fePlayer.playFirework(world, hit.getCoord().getLocation(), effect);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		boolean allDestroyed = true;
		for (ControlPoint c : this.controlPoints.values()) {
			if (c.isDestroyed() == false) {
				allDestroyed = false;
				break;
			}
		}

		if (allDestroyed) {
			CivMessage.sendCiv(this.getCiv(), CivColor.Rose + CivSettings.localize.localizedString("warcamp_ownDestroyed"));
			this.delete();
		} else {
			CivMessage.sendCiv(attacker.getTown().getCiv(), CivColor.LightGreen + CivSettings.localize.localizedString("warcamp_enemyControlBlockDestroyed") + " " + getCiv().getName() + CivSettings.localize.localizedString("warcamp_name"));
			CivMessage.sendCiv(getCiv(), CivColor.Rose + CivSettings.localize.localizedString("warcamp_ownControlBlockDestroyed"));
		}

	}

	@Override
	public void delete() {
		this.getCiv().getWarCamps().remove(this);
		try {
			this.undoFromTemplate();
		} catch (IOException | CivException e) {
			this.fancyDestroyConstructBlocks();
		}
		super.delete();
	}

	@Override
	public String getRespawnName() {
		return "WarCamp\n(" + this.getCorner().getX() + "," + this.getCorner().getY() + "," + this.getCorner().getZ() + ")";
	}

	@Override
	public List<BlockCoord> getRespawnPoints() {
		return this.getRespawnPoints();
	}

	@Override
	public boolean isTeleportReal() {
		if (this.getTown().isCapitol()) {
			return true;
		}
		for (final ControlPoint c : this.controlPoints.values()) {
			if (c.isDestroyed()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int getRegenRate() {
		if (this.getCiv().getCapitol().getBuffManager().hasBuff("level6_wcHPTown")) return 1;
		if (WarCamp.info.regenRate == null) return 0;
		return info.regenRate;
	}

	@Override
	public BlockCoord getRandomRevivePoint() {
		if (this.respawnPoints.size() == 0) {
			return new BlockCoord(this.getCorner());
		}
		Random rand = new Random();
		int index = rand.nextInt(this.respawnPoints.size());
		return this.respawnPoints.get(index);
	}

	@Override
	public void onDamageNotification(Player player, ConstructDamageBlock hit) {
		CivMessage.send(player, "TODO Нужно чтото написать");
	}

	@Override
	protected List<HashMap<String, String>> getComponentInfoList() {
		return null;
	}

	public int isWarCampCooldownLeft() {
		int rebuild_timeout;
		try {
			rebuild_timeout = CivSettings.getInteger(CivSettings.warConfig, "warcamp.rebuild_timeout");
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			return 0;
		}
		long milisecLeft = (rebuild_timeout * 60000) - (System.currentTimeMillis() - this.getCiv().getLastWarCampCreated());
		if (milisecLeft <= 0) return 0;
		return (int) (milisecLeft / 60000);
	}

	@Override
	public void onPostBuild() {
		// TODO Автоматически созданная заглушка метода
		
	}
}
