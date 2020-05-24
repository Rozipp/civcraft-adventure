package com.avrgaming.civcraft.construct;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.SimpleBlock;

public class CaveEntrance extends Construct {

	private Cave cave;

	public CaveEntrance(Cave cave) {
		this.cave = cave;
	}

	@Override
	public void commandBlockRelatives(BlockCoord absCoord, SimpleBlock sb) {
		ConstructSign structSign;
		switch (sb.command) {
		case "/entrance":
			structSign = CivGlobal.getConstructSign(absCoord);
			if (structSign == null)
				structSign = new ConstructSign(absCoord, this);
			ItemManager.setTypeIdAndData(absCoord.getBlock(), sb.getType(), sb.getData(), true);

			structSign.setDirection(ItemManager.getData(absCoord.getBlock().getState()));
			structSign.setOwner(this);
			structSign.setText(new String[] { "", "Нажми", "что бы", "спустится" });
			structSign.setAction("entrance");
			structSign.update();
			this.addConstructSign(structSign);
			CivGlobal.addConstructSign(structSign);
			break;
		}

	}

	@Override
	public void processSignAction(Player player, ConstructSign sign, PlayerInteractEvent event) {
		cave.processSignAction(player, sign, event);
	}
	
	@Override
	public void load(ResultSet rs) {
	}

	@Override
	public void saveNow() throws SQLException {
	}

	@Override
	public String getDisplayName() {
		return cave.getDisplayName();
	}

	@Override
	public void processUndo() throws CivException {
	}

	@Override
	public void build(Player player) {
		this.getTemplate().buildTemplate(getCorner());
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
	public void onDamage(int amount, World world, Player player, BlockCoord coord, ConstructDamageBlock hit) {
	}

	@Override
	public void onDamageNotification(Player player, ConstructDamageBlock hit) {
	}

	@Override
	protected List<HashMap<String, String>> getComponentInfoList() {
		return null;
	}

	@Override
	public void onPostBuild() {
		// TODO Автоматически созданная заглушка метода
		
	}

}
