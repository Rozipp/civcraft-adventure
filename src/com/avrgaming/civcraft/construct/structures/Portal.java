
package com.avrgaming.civcraft.construct.structures;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.ConstructSign;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.SimpleBlock;
import com.avrgaming.civcraft.war.War;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

public class Portal extends Structure {
    public Location spawnLocation;

    public Portal(String id, Town town) {
        super(id, town);
    }

    @Override
    public String getDynmapDescription() {
        return null;
    }

    @Override
    public String getMarkerIconName() {
        return "pin";
    }

    @Override
    public void processSignAction(Player player, ConstructSign sign, PlayerInteractEvent event) throws CivException {
        Resident resident = CivGlobal.getResident(player);
        if (resident == null) {
            return;
        }
        if ("teleport".equals(sign.getAction())) {
            if (War.isWarTime()) {
                throw new CivException(CivSettings.localize.localizedString("var_portal_wartime", this.getCivOwner().getName()));
            }
            if (resident.isProtected()) {
                throw new CivException(CivSettings.localize.localizedString("var_portal_pvptimer"));
            }
//                if (!UnitStatic.isWearingFullHell(player)) {
            throw new CivException(CivSettings.localize.localizedString("var_portal_notFullSet"));
//                }
//                boolean right = CivCraft.civRandom.nextBoolean();
//                Location bossLocation = right ? new Location(Bukkit.getWorld((String)"world_nether"), 143.0, 147.0, -613.0) : new Location(Bukkit.getWorld((String)"world_nether"), 1.0, 148.0, -610.0);
//                CivMessage.sendSuccess((CommandSender)player, CivSettings.localize.localizedString("var_portal_teleporting", CivColor.Red));
//                player.teleport(bossLocation);
        }
    }

    @Override
    public void commandBlockRelatives(BlockCoord absCoord, SimpleBlock commandBlock) {
        if (commandBlock.command.equals("/teleport")) {
            ItemManager.setTypeId(absCoord.getBlock(), commandBlock.getType());
            ItemManager.setData(absCoord.getBlock(), commandBlock.getData());
            ConstructSign structSign = new ConstructSign(absCoord, this);
            structSign.setText(CivSettings.localize.localizedString("structure_portal_sign"));
            structSign.setDirection(commandBlock.getData());
            structSign.setAction("teleport");
            structSign.update();
            this.addConstructSign(structSign);
            if (this.spawnLocation == null) {
                this.spawnLocation = structSign.getCoord().getLocation();
            }
        }
    }
}

