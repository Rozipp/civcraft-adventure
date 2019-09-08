package com.avrgaming.civcraft.command.admin;

import org.bukkit.entity.Player;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivMessage;

import io.lumine.xikage.mythicmobs.MythicMobs;

public class AdminMobCommand extends CommandBase {
    
    @Override
    public void init() {
        command = "/ad mob";
        displayName = CivSettings.localize.localizedString("cmd_mob_managament");
        
        cs.add("count", "Shows mob totals globally");
        cs.add("disable", "[name] Disables this mob from spawning");
        cs.add("enable", "[name] Enables this mob to spawn.");
        cs.add("killall", "[name] Removes all of these mobs from the game instantly.");
    }
    
    public void killall_cmd() throws CivException {
        Player player = getPlayer();
//        String name = getNamedString(1, "Enter a mob name");
        
        int count = MythicMobs.inst().getMobManager().removeAllMobs();
        CivMessage.sendSuccess(player, "Removed " + count + " mobs of type ");// + name);
    }
    
    public void count_cmd() throws CivException {
//        Player player = getPlayer();
//        
//        HashMap<String, Integer> amounts = new HashMap<String, Integer>();
//        int total = CommonCustomMob.customMobs.size();
//        CommonCustomMob.customMobs.values().forEach((mob) -> {
//            Integer count = amounts.get(mob.getClass().getSimpleName());
//            if (count == null) {
//                count = 0;
//            }
//            
//            amounts.put(mob.getClass().getSimpleName(), count + 1);
//        });
//        
//        CivMessage.sendHeading(player, "Custom Mob Counts");
//        CivMessage.send(player, CivColor.LightGray + "Red mobs are over their count limit for this area and should no longer spawn.");
//        for (Iterator<String> it = amounts.keySet().iterator(); it.hasNext(); ) {
//            String mob = it.next();
//            int count = amounts.get(mob);
//            LinkedList<Entity> entities = EntityProximity.getNearbyEntities(null, player.getLocation(), MobSpawnerTimer.MOB_AREA, EntityCreature.class);
//            if (entities.size() > MobSpawnerTimer.MOB_AREA_LIMIT) {
//                CivMessage.send(player, CivColor.Red + mob + ": " + CivColor.Rose + count);
//            } else {
//                CivMessage.send(player, CivColor.Green + mob + ": " + CivColor.LightGreen + count);
//            }
//        }
//        CivMessage.send(player, CivColor.Green + "Total Mobs:" + CivColor.LightGreen + total);
    }
    
    public void disable_cmd() throws CivException {
        Player player = getPlayer();
        String name = getNamedString(1, "Enter a mob name");
//        TODo
//        switch (name.toLowerCase()) {
//            case "behemoth":
//                CommonCustomMob.disabledMobs.add(MobType.BEHEMOTH.toString());
//                break;
//            case "ruffian":
//                CommonCustomMob.disabledMobs.add(MobType.RUFFIAN.toString());
//                break;
//            case "yobo":
//                CommonCustomMob.disabledMobs.add(MobType.YOBO.toString());
//                break;
//            case "savagae":
//                CommonCustomMob.disabledMobs.add(MobType.SAVAGE.toString());
//                break;
//            default:
//                CivMessage.send(player, CivColor.Red + CivSettings.localize.localizedString("error_mobname"));
//        }
        
        CivMessage.sendSuccess(player, "Disabled " + name);
    }
    
    public void enable_cmd() throws CivException {
        Player player = getPlayer();
        String name = getNamedString(1, "Enter a mob name");
//        TODO
//        switch (name.toLowerCase()) {
//            case "behemoth":
//                CommonCustomMob.disabledMobs.add(MobType.BEHEMOTH.toString());
//                break;
//            case "ruffian":
//                CommonCustomMob.disabledMobs.add(MobType.RUFFIAN.toString());
//                break;
//            case "yobo":
//                CommonCustomMob.disabledMobs.add(MobType.YOBO.toString());
//                break;
//            case "savagae":
//                CommonCustomMob.disabledMobs.add(MobType.SAVAGE.toString());
//                break;
//           
//            default:
//                CivMessage.send(player, CivColor.Red + CivSettings.localize.localizedString("error_mobname"));
//        }
        
        CivMessage.sendSuccess(player, "Enabled " + name);
    }
    
    @Override
    public void doDefaultAction() throws CivException {
        showHelp();
    }
    
    @Override
    public void showHelp() {
        showBasicHelp();
    }
    
    @Override
    public void permissionCheck() throws CivException {
    }
}