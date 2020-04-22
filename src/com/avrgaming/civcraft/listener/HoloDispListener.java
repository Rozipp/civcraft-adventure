package com.avrgaming.civcraft.listener;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.gmail.filoghost.holographicdisplays.api.line.ItemLine;
import com.gmail.filoghost.holographicdisplays.api.line.TextLine;
import java.sql.SQLException;
import java.util.Iterator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuff;
import com.avrgaming.civcraft.construct.ConstructSign;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.object.TradeGood;
import com.avrgaming.civcraft.structure.Bank;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CivColor;

public class HoloDispListener {
   @SuppressWarnings("deprecation")
   public static void tradeGoodHolo() {
      if (!CivSettings.hasHoloDisp) {
         CivLog.warning("Человек попытался сгенерировать голограмму без плагина HoloDisp.");
         return;
      }

      Plugin CivCraftGoods = CivCraft.getPlugin();
      Iterator<TradeGood> var1 = CivGlobal.getTradeGoods().iterator();

      while(var1.hasNext()) {
         TradeGood tradeGood = (TradeGood)var1.next();
         BlockCoord coord = tradeGood.getCoord();
         Location loc = new Location(coord.getBlock().getWorld(), (double)coord.getX() + 0.5D, (double)(coord.getY() + 5), (double)coord.getZ() + 0.5D);
         Hologram holoTradeGood = HologramsAPI.createHologram(CivCraftGoods, loc);
         ItemLine tradeGoodItem;
         TextLine tradeGoodName;
         TextLine tradeGoodValue;
         String color;
         if (tradeGood.getInfo().water) {
            tradeGoodItem = holoTradeGood.appendItemLine(new ItemStack(tradeGood.getInfo().material, 1, (short)tradeGood.getInfo().material_data));
            tradeGoodName = holoTradeGood.appendTextLine(CivColor.GoldBold + "Торговый ресурс " + CivColor.LightBlueBold + CivColor.ITALIC + tradeGood.getInfo().name);
            tradeGoodValue = holoTradeGood.appendTextLine(CivColor.LightPurpleBold + "Дает: ");
            color = "§7";
         } else {
            tradeGoodItem = holoTradeGood.appendItemLine(new ItemStack(tradeGood.getInfo().material, 1, (short)tradeGood.getInfo().material_data));
            tradeGoodName = holoTradeGood.appendTextLine(CivColor.GoldBold + "Торговый ресурс " + CivColor.LightGreenBold + CivColor.ITALIC + tradeGood.getInfo().name);
            tradeGoodValue = holoTradeGood.appendTextLine(CivColor.LightPurpleBold + "Дает: ");
            color = "§c";
         }

         TextLine tradeGoodBuffs = holoTradeGood.appendTextLine(color + getHumanBuffList(tradeGood));
         setTradeGoodInfo(tradeGoodItem, tradeGoodName, tradeGoodValue, tradeGoodBuffs);
      }

      CivLog.info(CivGlobal.getTradeGoods().size() + " было создано голограмм для торговых ресурсов.");
   }

   public static String getHumanBuffList(TradeGood tradeGood) {
      StringBuilder buffs = new StringBuilder();
      Iterator<ConfigBuff> var2 = tradeGood.getInfo().buffs.values().iterator();

      while(var2.hasNext()) {
         ConfigBuff configBuff = (ConfigBuff)var2.next();
         buffs.append(configBuff.name).append(", ");
      }

      return buffs.toString();
   }

   public static void setTradeGoodInfo(ItemLine item, TextLine name, TextLine value, TextLine buffs) {
      item.setTouchHandler((whoClicked) -> {
         CivMessage.send((Object)whoClicked, (String)(CivColor.LightBlueItalic + CivSettings.localize.localizedString("tradeHoloMoreInfoHere")));
      });
      name.setTouchHandler((whoClicked) -> {
         CivMessage.send((Object)whoClicked, (String)(CivColor.LightBlueItalic + CivSettings.localize.localizedString("tradeHoloMoreInfoHere")));
      });
      value.setTouchHandler((whoClicked) -> {
         CivMessage.send((Object)whoClicked, (String)(CivColor.LightBlueItalic + CivSettings.localize.localizedString("tradeHoloMoreInfoHere")));
      });
      buffs.setTouchHandler((whoClicked) -> {
         CivMessage.send((Object)whoClicked, (String)(CivColor.LightBlueItalic + CivSettings.localize.localizedString("tradeHoloMoreInfoHere")));
      });
   }
   
   public static void setMobSpawner(TextLine name, TextLine value) {
	      name.setTouchHandler((whoClicked) -> {
	         CivMessage.send((Object)whoClicked, (String)(CivColor.LightBlueItalic + CivSettings.localize.localizedString("tradeSpawnerMoreInfoHere")));
	      });
	      value.setTouchHandler((whoClicked) -> {
	         CivMessage.send((Object)whoClicked, (String)(CivColor.LightBlueItalic + CivSettings.localize.localizedString("tradeSpawnerMoreInfoHere")));
	      });
	   }

   public static void clearHolos() {
       CivLog.info(CivGlobal.getTradeGoods().size() + CivGlobal.getTotalCamps() + CivGlobal.getTownHalls() + CivGlobal.getTotalBanks() + " \u0413\u043e\u043b\u043e\u0433\u0440\u0430\u043c\u043c \u0441 CivCraft'\u043e\u043c \u0431\u044b\u043b\u043e \u0443\u0434\u0430\u043b\u0435\u043d\u043e.");
       for (final Hologram hologram : HologramsAPI.getHolograms((Plugin)CivCraft.getPlugin())) {
           hologram.delete();
       }
       
       for (final Hologram hologram : HologramsAPI.getHolograms((Plugin)CivCraft.getPlugin())) {
           hologram.delete();
       }
   }

@SuppressWarnings("deprecation")
public static void bankHolo() {
      if (!CivSettings.hasHoloDisp) {
         CivLog.warning("Человек попытался сгенерировать голограмму без плагина HoloDisp.");
      }

      Plugin CivCraftBanks = CivCraft.getPlugin();
      Iterator<Structure> var1 = CivGlobal.getStructures().iterator();

      while(true) {
         Structure structure;
         do {
            if (!var1.hasNext()) {
               CivLog.info("Для " + CivGlobal.getTotalBanks() + " банков было создано голограмм");
               return;
            }

            structure = (Structure)var1.next();
         } while(!(structure instanceof Bank));

         Bank bank = (Bank)structure;
         Iterator<ConstructSign> var4 = bank.getSigns().iterator();

         while(var4.hasNext()) {
            ConstructSign strucutreSign = (ConstructSign)var4.next();
            /*boolean IRON_SIGN = false;
			boolean GOLD_SIGN = true;
            boolean DIAMOND_SIGN = true;
            boolean EMERALD_SIGN = true;*/
            String var15 = strucutreSign.getAction().toLowerCase();
            byte var16 = -1;
            switch(var15.hashCode()) {
            case -1634062812:
               if (var15.equals("emerald")) {
                  var16 = 3;
               }
               break;
            case 3178592:
               if (var15.equals("gold")) {
                  var16 = 1;
               }
               break;
            case 3241160:
               if (var15.equals("iron")) {
                  var16 = 0;
               }
               break;
            case 1655054676:
               if (var15.equals("diamond")) {
                  var16 = 2;
               }
            }

            ItemLine bankItem;
            TextLine bankPercentage;
            TextLine bankPrice;
            TextLine bankFee;
            Location structureSignLocation;
            switch(var16) {
            case 0:
               BlockCoord coordIron = strucutreSign.getCoord();
               Location strucutreSignLocationIron = new Location(coordIron.getBlock().getWorld(), (double)coordIron.getX() + 0.5D, (double)(coordIron.getBlock().getY() + 2), (double)coordIron.getZ() + 0.5D);
               Hologram holoIron = HologramsAPI.createHologram(CivCraftBanks, strucutreSignLocationIron);
               bankItem = holoIron.appendItemLine(new ItemStack(Material.IRON_INGOT, 1));
               bankPercentage = holoIron.appendTextLine(CivColor.GoldBold + "Уровень: " + bank.getLevel());
               bankPrice = holoIron.appendTextLine(CivColor.GoldBold + "За " + bank.getHoloItemPrice(0));
               bankFee = holoIron.appendTextLine(CivColor.GoldBold + "Налог: " + CivColor.LightGreenBold + bank.getNonResidentFee() * 100.0D + "%");
               setBankAction(bankItem, bankPercentage, bankPrice, bankFee, 265, CivSettings.iron_rate);

               try {
                  structureSignLocation = coordIron.getLocation();
                  structureSignLocation.getBlock().setTypeId(0);
                  strucutreSign.delete();
               } catch (SQLException var33) {
                  var33.printStackTrace();
               }
               break;
            case 1:
               BlockCoord coordGold = strucutreSign.getCoord();
               Location strucutreSignLocationGold = new Location(coordGold.getBlock().getWorld(), (double)coordGold.getX() + 0.5D, (double)(coordGold.getBlock().getY() + 2), (double)coordGold.getZ() + 0.5D);
               Hologram holoGold = HologramsAPI.createHologram(CivCraftBanks, strucutreSignLocationGold);
               bankItem = holoGold.appendItemLine(new ItemStack(Material.GOLD_INGOT, 1));
               bankPercentage = holoGold.appendTextLine(CivColor.GoldBold + "Уровень: " + bank.getLevel());
               bankPrice = holoGold.appendTextLine(CivColor.GoldBold + "За " + bank.getHoloItemPrice(1));
               bankFee = holoGold.appendTextLine(CivColor.GoldBold + "Налог: " + CivColor.LightGreenBold + bank.getNonResidentFee() * 100.0D + "%");
               setBankAction(bankItem, bankPercentage, bankPrice, bankFee, 266, CivSettings.gold_rate);

               try {
                  structureSignLocation = coordGold.getLocation();
                  structureSignLocation.getBlock().setTypeId(0);
                  strucutreSign.delete();
               } catch (SQLException var32) {
                  var32.printStackTrace();
               }
               break;
            case 2:
               BlockCoord coordDiamond = strucutreSign.getCoord();
               Location strucutreSignLocationDiamond = new Location(coordDiamond.getBlock().getWorld(), (double)coordDiamond.getX() + 0.5D, (double)(coordDiamond.getBlock().getY() + 2), (double)coordDiamond.getZ() + 0.5D);
               Hologram holoDiamond = HologramsAPI.createHologram(CivCraftBanks, strucutreSignLocationDiamond);
               bankItem = holoDiamond.appendItemLine(new ItemStack(Material.DIAMOND, 1));
               bankPercentage = holoDiamond.appendTextLine(CivColor.GoldBold + "Уровень: " + bank.getLevel());
               bankPrice = holoDiamond.appendTextLine(CivColor.GoldBold + "За " + bank.getHoloItemPrice(2));
               bankFee = holoDiamond.appendTextLine(CivColor.GoldBold + "Налог: " + CivColor.LightGreenBold + bank.getNonResidentFee() * 100.0D + "%");
               setBankAction(bankItem, bankPercentage, bankPrice, bankFee, 264, CivSettings.diamond_rate);

               try {
                  structureSignLocation = coordDiamond.getLocation();
                  structureSignLocation.getBlock().setTypeId(0);
                  strucutreSign.delete();
               } catch (SQLException var31) {
                  var31.printStackTrace();
               }
               break;
            case 3:
               BlockCoord coordEmerald = strucutreSign.getCoord();
               Location strucutreSignLocationEmerald = new Location(coordEmerald.getBlock().getWorld(), (double)coordEmerald.getX() + 0.5D, (double)(coordEmerald.getBlock().getY() + 2), (double)coordEmerald.getZ() + 0.5D);
               Hologram holoEmerald = HologramsAPI.createHologram(CivCraftBanks, strucutreSignLocationEmerald);
               bankItem = holoEmerald.appendItemLine(new ItemStack(Material.EMERALD, 1));
               bankPercentage = holoEmerald.appendTextLine(CivColor.GoldBold + "Уровень: " + bank.getLevel());
               bankPrice = holoEmerald.appendTextLine(CivColor.GoldBold + "За " + bank.getHoloItemPrice(3));
               bankFee = holoEmerald.appendTextLine(CivColor.GoldBold + "Налог: " + CivColor.LightGreenBold + bank.getNonResidentFee() * 100.0D + "%");
               setBankAction(bankItem, bankPercentage, bankPrice, bankFee, 388, CivSettings.emerald_rate);

               try {
                  structureSignLocation = coordEmerald.getLocation();
                  structureSignLocation.getBlock().setTypeId(0);
                  strucutreSign.delete();
               } catch (SQLException var30) {
                  var30.printStackTrace();
               }
            }
         }
      }
   }

   public static Structure getNearestStrucutre(Location location) {
      Structure nearest = null;
      double lowest_distance = Double.MAX_VALUE;
      Iterator<Structure> var4 = CivGlobal.getStructures().iterator();

      while(var4.hasNext()) {
         Structure struct = (Structure)var4.next();
         double distance = struct.getCenterLocation().distanceSquared(location);
         if (distance < lowest_distance) {
            lowest_distance = distance;
            nearest = struct;
         }
      }

      return nearest;
   }

   public static void setBankAction(ItemLine item, TextLine percent, TextLine price, TextLine fee, int itemToSeel, double priceAdding) {
      item.setTouchHandler((whoClicked) -> {
         Resident clicker = CivGlobal.getResident(whoClicked);
         Structure nearestBank = getNearestStrucutre(whoClicked.getLocation());
         if (nearestBank instanceof Bank) {
            Bank bank = (Bank)nearestBank;

            try {
               bank.exchange_for_coins(clicker, itemToSeel, priceAdding);
            } catch (CivException var8) {
               CivMessage.send((Object)clicker, (String)("§c" + CivSettings.localize.localizedString("advancedGuiNoRes")));
            }
         } else {
            CivMessage.sendError(clicker, "Извините, но произошел баг. Попробуйте перезайти.");
         }

      });
      percent.setTouchHandler((whoClicked) -> {
         Resident clicker = CivGlobal.getResident(whoClicked);
         Structure nearestBank = getNearestStrucutre(whoClicked.getLocation());
         if (nearestBank instanceof Bank) {
            Bank bank = (Bank)nearestBank;

            try {
               bank.exchange_for_coins(clicker, itemToSeel, priceAdding);
            } catch (CivException var8) {
               CivMessage.send((Object)clicker, (String)("§c" + CivSettings.localize.localizedString("advancedGuiNoRes")));
            }
         } else {
            CivMessage.sendError(clicker, "Извините, но произошел баг. Попробуйте перезайти.");
         }

      });
      price.setTouchHandler((whoClicked) -> {
         Resident clicker = CivGlobal.getResident(whoClicked);
         Structure nearestBank = getNearestStrucutre(whoClicked.getLocation());
         if (nearestBank instanceof Bank) {
            Bank bank = (Bank)nearestBank;

            try {
               bank.exchange_for_coins(clicker, itemToSeel, priceAdding);
            } catch (CivException var8) {
               CivMessage.send((Object)clicker, (String)("§c" + CivSettings.localize.localizedString("advancedGuiNoRes")));
            }
         } else {
            CivMessage.sendError(clicker, "Извините, но произошел баг. Попробуйте перезайти.");
         }

      });
      fee.setTouchHandler((whoClicked) -> {
         Resident clicker = CivGlobal.getResident(whoClicked);
         Structure nearestBank = getNearestStrucutre(whoClicked.getLocation());
         if (nearestBank instanceof Bank) {
            Bank bank = (Bank)nearestBank;

            try {
               bank.exchange_for_coins(clicker, itemToSeel, priceAdding);
            } catch (CivException var8) {
               CivMessage.send((Object)clicker, (String)("§c" + CivSettings.localize.localizedString("advancedGuiNoRes")));
            }
         } else {
            CivMessage.sendError(clicker, "Извините, но произошел баг. Попробуйте перезайти.");
         }

      });
   }


   public static void startWar() {
      Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(CivCraft.getPlugin(), () -> {
         Iterator<Civilization> var0 = CivGlobal.getCivs().iterator();

         label37:
         while(true) {
            Civilization civ;
            do {
               if (!var0.hasNext()) {
                  return;
               }

               civ = (Civilization)var0.next();
            } while(!civ.getDiplomacyManager().isAtWar());

            Iterator<Town> var2 = civ.getTowns().iterator();

            while(true) {
               Town town;
               do {
                  if (!var2.hasNext()) {
                     continue label37;
                  }

                  town = (Town)var2.next();
               } while(town.getTownHall() == null);
            }
         }
      });
   }
   /*
   public static void generateTops() {
      Tops tops = new Tops();
      CivGlobal.setTops(tops);
   }*/

   public static void runAllHolos() {
      clearHolos();
      tradeGoodHolo();
      //worldBossHolo();
      //generateTops();
   }
}
    