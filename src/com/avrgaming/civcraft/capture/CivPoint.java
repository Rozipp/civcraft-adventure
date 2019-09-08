package com.avrgaming.civcraft.capture;

import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.google.common.base.Preconditions;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldguard.bukkit.BukkitUtil;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CivPoint {
    private static WorldGuardPlugin worldGuard = WorldGuardPlugin.inst();
    @SuppressWarnings({ "unchecked", "rawtypes" })
	public static Map<Civilization, Integer> civsCaptured = new HashMap();
    String name, location, locationXZ;
    ProtectedRegion region;
    int interval, playersValue;
    Map<Civilization, Integer> civs;
    Civilization owner, oldOwner;
    World world;
    boolean captureMessage;
    List<Resident> players;
    @SuppressWarnings({ "unchecked", "rawtypes" })
	public static Map<String, CivPoint> pointsRegion = new HashMap();
    @SuppressWarnings({ "unchecked", "rawtypes" })
	public static List<CivPoint> points = new ArrayList();

    public Civilization getOwner() {
        return this.owner;
    }
    
    public Map<Civilization, Integer> getCivs() {
        return this.civs;
    }
    public static void init() {
        CivGlobal.getCivs().forEach((civilization ->  {
            civsCaptured.put(civilization, 0);
        }));
    }

    public static void addCiv(Civilization civilization) {
        points.forEach(civPoint -> {
            civPoint.getCivs().put(civilization, 0);
        });
        civsCaptured.put(civilization, 0);
    }
    
    public void resetPoint() {
        this.civs.forEach((civilization, percent) -> {
            this.civs.put(civilization, 0);
        });
        this.oldOwner = null;
        this.owner = null;
        this.captureMessage = false;

        Bukkit.broadcastMessage(" ");
        CivMessage.global("§eЗагробный мир был освобождён от захватчиков!");
        //this.name - имя мира
        Bukkit.broadcastMessage(" ");
    }

    public CivPoint(String name, String location, String region, int interval, World world) {
        this(name, location, region, interval, world, 0);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public CivPoint(String name, String location, String regionName, int interval, World world, int players) {
        this.name = ChatColor.translateAlternateColorCodes('&', name);
        this.location = location;
        this.interval = interval;
        this.playersValue = players;
        this.world = world;
        try {
            ProtectedRegion region1 = worldGuard.getRegionManager(world).getRegion(regionName);
            Vector middle = region1.getMinimumPoint().add(region1.getMaximumPoint()).divide(2);
            this.locationXZ = ChatColor.translateAlternateColorCodes('&', "&5X: " + middle.getBlockX() + ", Y: " + middle.getBlockY() + ", Z: " + middle.getBlockZ());
            if(region1 != null) {
                this.region = region1;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.civs = new HashMap();
        CivGlobal.getCivs().forEach((civilization ->  {
            civs.put(civilization, 0);
        }));
        this.players = new ArrayList();
        Bukkit.getScheduler().runTaskTimerAsynchronously(CivCraft.getPlugin(), () -> update(), 1, interval);
        pointsRegion.put(regionName, this);
        points.add(this);
        Preconditions.checkNotNull(this.name, "Name nulled!");
        Preconditions.checkNotNull(this.world, "World nulled!");
        Bukkit.getConsoleSender().sendMessage("Point info: " + this.name + " | " + this.world.getName() + " | ");
    }


    public void initPlayers() {
        if(region == null) {
            Bukkit.getConsoleSender().sendMessage("Region not found!");
            return;
        }
        players = getAllPlayersInRegion(region.getId());
        //Bukkit.getConsoleSender().sendMessage("Players in region: " + players.size());
        //Bukkit.getConsoleSender().sendMessage("Civs amount: " + civs.size());
    }

    public static CivPoint getByName(String name) {
        return pointsRegion.get(name);
    }

    public int getPercent(Civilization civilization) {
        return this.civs.get(civilization);
    }

    public void updateFaction(Civilization civilization, int result) {
        this.civs.put(civilization, result);
    }

    public void addPercent(Civilization civilization, int percent) {
        int result = getPercent(civilization) + percent;
        if(result > 0 && !captureMessage) {
            CivMessage.global(ChatColor.translateAlternateColorCodes('&', "&eЦивилизация " + "&f&l" +civilization.getName() + " &eначала захват " + "&c&l" + "Загробного мира"));
            this.captureMessage = true;
        }
        if(result == 25) {
            CivMessage.global(ChatColor.translateAlternateColorCodes('&', "&eЦивилизация " + "&f&l" +civilization.getName() + " &eзахватила загробный мир на " + "&f&l" + "25%"));
        }
        if(result == 50) {
            CivMessage.global(ChatColor.translateAlternateColorCodes('&', "&eЦивилизация " + "&f&l" +civilization.getName() + " &eзахватила загробный мир на " + "&f&l" + "50%"));
        }
        if(result == 75) {
            CivMessage.global(ChatColor.translateAlternateColorCodes('&', "&eЦивилизация " + "&f&l" +civilization.getName() + " &eзахватила загробный мир на " + "&f&l" + "75%"));
        }
        if(result > 100) result = 100;
        updateFaction(civilization, result);
        sendPlayersInPoint("&0&l⚔ &2&lЗагробный мир захвачен на " + getPercent(civilization) + "% &0&l⚔");
        if(result == 100) {
            if(this.oldOwner == null) {
                capture(civilization, civilization);
                return;
            }
            else {
                civsCaptured.put(this.owner, civsCaptured.get(this.owner) - 1);
                capture(this.owner, civilization);
            }
        }
    }

    public void capture(Civilization oldCiv, Civilization civ) {
    	if(!civsCaptured.containsKey(civ)) {
            civsCaptured.put(civ, 1);
        } else {
            civsCaptured.put(civ, civsCaptured.get(civ) + 1);
        }
        this.owner = civ;
        this.oldOwner = oldCiv;
        this.captureBroadcast(civ);
        this.captureMessage = false;
    }

    private void captureBroadcast(Civilization civilization) {
        Bukkit.broadcastMessage(" ");
        CivMessage.global("&eЦивилизация " + "&f&l" + civilization.getName() + " §eзахватила " + "&c&l" + "Загробный мир!");
        Bukkit.broadcastMessage(" ");
        sendPlayersInPoint("&a&lВы успешно захватили &c&lЗагробный мир&a&l!");

        CivGlobal.getResidents().forEach(resident -> {
        	resident.getTown();
            Player player = Bukkit.getPlayerExact(resident.getName());
            if(player == null) return;
            World world = player.getWorld();
            if(world.getName().equalsIgnoreCase("world_the_end") && !resident.getCiv().equals(civilization)) {
            	Bukkit.getScheduler().runTask(CivCraft.getPlugin(), () -> {
            		for(Town town : resident.getCiv().getTowns()) {
                    	Location spawnLoc = town.getTownHall().getRandomRevivePoint().getLocation().add(0.0, 4.5, 0.0);
                        player.teleport(spawnLoc);
                    }
                });
            }
        });
        TaskMaster.asyncTask(() -> resetPoint(), TimeUnit.HOURS.toSeconds(1) * 20);
    }

    public void removePercent(Civilization civilization, int percent) {
        int result = getPercent(civilization) - percent;
        if(result < 0) result = 0;
        this.updateFaction(civilization, result);
    }

    public void update() {
        initPlayers();
        if(players.isEmpty()) {
            //Bukkit.getConsoleSender().sendMessage("Players empty!");
            return;
        }
        
        if(isEnemyInRegion()) {
            //Bukkit.getConsoleSender().sendMessage("Enemy in region!");
            sendPlayersInPoint("&4&l✖ &4Враг мешает захватывать точку &4&l✖");
            return;
        }
        if(playersValue != 0 && players.size() < playersValue) {
            //Bukkit.getConsoleSender().sendMessage("Can't capturing!");
            sendPlayersInPoint("&4&l✖️ &4Данную точку могут захватывать только " + playersValue + " и более игроков &4&l✖");
            return;
        }
        int capturing = players.size();
        Civilization civilization = players.get(0).getCiv();
        if(owner != null && owner.equals(civilization)) {
            //Bukkit.getConsoleSender().sendMessage("Already captured!!");
            sendPlayersInPoint("&f&lЗагробный мир был захвачен!");
            return;
        }
        if(!civs.containsKey(civilization)) {
            civs.put(civilization, 0);
        }
        civs.forEach((civ, integer) -> {
            if(!civilization.equals(civ)) {
                removePercent(civ, capturing);
                //Bukkit.getConsoleSender().sendMessage("removing percents...");
                return;
            }
            addPercent(civilization, capturing);
        });
    }

    private void sendPlayersInPoint(String message) {
        players.forEach(resident -> resident.sendActionBar(ChatColor.translateAlternateColorCodes('&', message)));
    }

    public boolean isEnemyInRegion() {
        Civilization civilization = null;
        for(Resident resident : players) {
            if(resident == null) {
                continue;
            }
            if(civilization == null) {
                civilization = resident.getCiv();
            }
            if(!civilization.equals(resident.getCiv())) {
                return true;
            }
        }
        return false;
    }
   
    private ArrayList<Resident> getAllPlayersInRegion(String regionName) {
        ArrayList<Resident> array = new ArrayList<>();
        for(Player cPlayer : Bukkit.getOnlinePlayers()) {
            if(cPlayer != null && cPlayer.isOnline() && !cPlayer.isDead() && isInRegion(cPlayer.getLocation(), regionName)) {
                Resident resident = CivGlobal.getResident(cPlayer);
                if(resident != null && resident.getCiv() != null) {
                    array.add(resident);
                }
            }
        }
        return array;
    }

    public boolean isInRegion(Location playerLocation, String regionName) {
        if(regionName == null) {
            return true;
        } else {
            ApplicableRegionSet set = getWGSet(playerLocation);
            if(set == null) {
                return false;
            } else {
                for(ProtectedRegion r : set) {
                    if(r.getId().equalsIgnoreCase(regionName)) {
                        return true;
                    }
                }

                return false;
            }
        }
    }

    public ApplicableRegionSet getWGSet(Location loc) {
        WorldGuardPlugin wg = worldGuard;
        if(wg == null) {
            return null;
        } else {
            RegionManager rm = wg.getRegionManager(loc.getWorld());
            return rm == null ? null : rm.getApplicableRegions(BukkitUtil.toVector(loc));
        }
    }
}
