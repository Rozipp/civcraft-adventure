
package com.avrgaming.civcraft.structure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.ConstructSign;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.SimpleBlock;
import com.avrgaming.civcraft.war.War;

public class Stable2
extends Structure {
    private ConstructSign respawnSign;
    private int index = 0;

    public Stable2(ResultSet rs) throws SQLException, CivException {
        super(rs);
    }

    public Stable2(Location center, String id, Town town) throws CivException {
        super(center, id, town);
    }

    @Override
    public String getMarkerIconName() {
        return "pin";
    }

    private RespawnLocationHolder getSelectedHolder() {
        ArrayList<RespawnLocationHolder> respawnables = this.getTown().getCiv().getAvailableRespawnables();
        return respawnables.get(this.index);
    }

    private void changeIndex(int newIndex) {
        ArrayList<RespawnLocationHolder> respawnables = this.getTown().getCiv().getAvailableRespawnables();
        if (this.respawnSign != null) {
            block4 : {
                try {
                    this.respawnSign.setText(CivSettings.localize.localizedString("stable_sign_respawnAt") + "\n" + CivColor.GreenBold + respawnables.get(newIndex).getRespawnName());
                    this.index = newIndex;
                }
                catch (IndexOutOfBoundsException e) {
                    if (respawnables.size() <= 0) break block4;
                    this.respawnSign.setText(CivSettings.localize.localizedString("stable_sign_respawnAt") + "\n" + CivColor.GreenBold + respawnables.get(0).getRespawnName());
                    this.index = 0;
                }
            }
            this.respawnSign.update();
        } else {
            CivLog.warning("Could not find civ spawn sign:" + this.getId() + " at " + this.getCorner());
        }
    }

    @Override
    public void processSignAction(Player player, ConstructSign sign, PlayerInteractEvent event) {
        Resident resident = CivGlobal.getResident(player);
        if (resident == null) {
            return;
        }
        if (War.isWarTime()) {
            CivMessage.sendError(resident, CivSettings.localize.localizedString("stable_wartime_returned"));
            return;
        }
        Boolean hasPermission = false;
        Civilization civ = this.getTown().getCiv();
        if (civ.hasResident(resident)) {
            hasPermission = true;
        }
        switch (sign.getAction()) {
            case "prev": {
                if (hasPermission.booleanValue()) {
                    this.changeIndex(this.index - 1);
                    break;
                }
                CivMessage.sendError(resident, CivSettings.localize.localizedString("stable_Sign_noPermission"));
                break;
            }
            case "next": {
                if (hasPermission.booleanValue()) {
                    this.changeIndex(this.index + 1);
                    break;
                }
                CivMessage.sendError(resident, CivSettings.localize.localizedString("stable_Sign_noPermission"));
                break;
            }
            case "respawn": {
                long timeNow;
                ArrayList<RespawnLocationHolder> respawnables = this.getTown().getCiv().getAvailableRespawnables();
                if (this.index >= respawnables.size()) {
                    this.index = 0;
                    this.changeIndex(this.index);
                    CivMessage.sendError(resident, CivSettings.localize.localizedString("stable_cannotRespawn"));
                    return;
                }
                respawnables.get(this.index).getRandomRevivePoint();
                if (!hasPermission.booleanValue()) {
                    CivMessage.sendError(resident, CivSettings.localize.localizedString("stable_Sign_noPermission"));
                    return;
                }
                long nextTeleport = resident.getNextTeleport();
				SimpleDateFormat sdf = CivGlobal.dateFormat;
                if (nextTeleport > (timeNow = Calendar.getInstance().getTimeInMillis())) {
                    CivMessage.sendError(resident, CivSettings.localize.localizedString("var_stable2_teleportNotAva", sdf.format(nextTeleport)));
                    return;
                }
                RespawnLocationHolder holder = this.getSelectedHolder();
                Town toTeleport = CivGlobal.getCultureChunk(holder.getRandomRevivePoint().getLocation()).getTown();
                boolean hasStable = false;
                Location placeToTeleport = null;
                for (Structure structure : toTeleport.getStructures()) {
                    if (!(structure instanceof Stable2)) continue;
                    hasStable = true;
                    placeToTeleport = ((Stable2)structure).respawnSign.getCoord().getLocation();
                    break;
                }
                if (!hasStable) {
                    CivMessage.sendError(resident, CivSettings.localize.localizedString("stable_Sign_teleport_noStable", toTeleport.getName()));
                    return;
                }
                if (!resident.getTreasury().hasEnough(2000.0)) {
                    CivMessage.sendError(resident, CivSettings.localize.localizedString("shipyard_Sign_noMoney", CivColor.Gold + (2000.0 - resident.getTreasury().getBalance()), CivColor.Green + CivSettings.CURRENCY_NAME + CivColor.Red));
                    return;
                }
                nextTeleport = timeNow + 120000L;
                CivMessage.send((Object)player, CivColor.Green + CivSettings.localize.localizedString("stable_respawningAlert"));
                player.teleport(placeToTeleport);
                resident.getTreasury().withdraw(2000.0);
                resident.setNextTeleport(nextTeleport);
                resident.save();
            }
        }
    }

    @Override
    public void commandBlockRelatives(BlockCoord absCoord, SimpleBlock commandBlock) {
        switch (commandBlock.command) {
            case "/next": {
                ItemManager.setTypeId(absCoord.getBlock(), commandBlock.getType());
                ItemManager.setData(absCoord.getBlock(), commandBlock.getData());
                ConstructSign structSign = new ConstructSign(absCoord, this);
                structSign.setText("\n" + (Object)ChatColor.BOLD + (Object)ChatColor.UNDERLINE + CivSettings.localize.localizedString("stable_sign_nextLocation"));
                structSign.setDirection(commandBlock.getData());
                structSign.setAction("next");
                structSign.update();
                this.addConstructSign(structSign);
                CivGlobal.addConstructSign(structSign);
                break;
            }
            case "/prev": {
                ItemManager.setTypeId(absCoord.getBlock(), commandBlock.getType());
                ItemManager.setData(absCoord.getBlock(), commandBlock.getData());
                ConstructSign structSign = new ConstructSign(absCoord, this);
                structSign.setText("\n" + (Object)ChatColor.BOLD + (Object)ChatColor.UNDERLINE + CivSettings.localize.localizedString("stable_sign_previousLocation"));
                structSign.setDirection(commandBlock.getData());
                structSign.setAction("prev");
                structSign.update();
                this.addConstructSign(structSign);
                CivGlobal.addConstructSign(structSign);
                break;
            }
            case "/respawn": {
                ItemManager.setTypeId(absCoord.getBlock(), commandBlock.getType());
                ItemManager.setData(absCoord.getBlock(), commandBlock.getData());
                ConstructSign structSign = new ConstructSign(absCoord, this);
                structSign.setText(CivSettings.localize.localizedString("stable_sign_Stable"));
                structSign.setDirection(commandBlock.getData());
                structSign.setAction("respawn");
                structSign.update();
                this.addConstructSign(structSign);
                CivGlobal.addConstructSign(structSign);
                this.respawnSign = structSign;
                this.changeIndex(this.index);
            }
        }
    }

    @Override
    public void onInvalidPunish() {
        int invalid_respawn_penalty;
        try {
            invalid_respawn_penalty = CivSettings.getInteger(CivSettings.warConfig, "war.invalid_respawn_penalty");
        }
        catch (InvalidConfiguration e) {
            e.printStackTrace();
            return;
        }
        CivMessage.sendTown(this.getTown(), CivColor.RoseBold + CivSettings.localize.localizedString("stable_cannotSupport1") + " " + CivSettings.localize.localizedString("var_stable_cannotSupport2", invalid_respawn_penalty));
    }

    @Override
    public boolean isValid() {
        if (this.getCiv().isAdminCiv()) {
            return true;
        }
        for (Town town : this.getCiv().getTowns()) {
            Townhall townhall = town.getTownHall();
            if (townhall != null) continue;
            return false;
        }
        return super.isValid();
    }
}

