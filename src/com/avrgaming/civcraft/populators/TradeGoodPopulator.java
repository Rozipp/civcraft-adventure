/*************************************************************************
 * 
 * AVRGAMING LLC
 * __________________
 * 
 *  [2013] AVRGAMING LLC
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of AVRGAMING LLC and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to AVRGAMING LLC
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from AVRGAMING LLC.
 */
package com.avrgaming.civcraft.populators;

import java.sql.SQLException;
import java.util.Random;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.plugin.Plugin;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuff;
import com.avrgaming.civcraft.config.ConfigTradeGood;
import com.avrgaming.civcraft.construct.ConstructSign;
import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.ProtectedBlock;
import com.avrgaming.civcraft.object.TradeGood;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;
import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.gmail.filoghost.holographicdisplays.api.line.ItemLine;
import com.gmail.filoghost.holographicdisplays.api.line.TextLine;

public class TradeGoodPopulator extends BlockPopulator {
	
	//private static final int RESOURCE_CHANCE = 400; 
    private static final int FLAG_HEIGHT = 3;
//    private static final double MIN_DISTANCE = 400.0;
    

    public static void buildTradeGoodie(ConfigTradeGood good, BlockCoord coord, World world, boolean sync) {
    	TradeGood new_good = new TradeGood(good, coord);            
    	CivGlobal.addTradeGood(new_good);
        final Plugin CivCraftGoods = (Plugin)CivCraft.getPlugin();

    	BlockFace direction = null;
    	Block top = null;
    	Random random = new Random();
    	int dir = random.nextInt(4);
    	if (dir == 0) {
    		direction = BlockFace.NORTH;
    	} else if (dir == 1) {
    		direction = BlockFace.EAST;
    	} else if (dir == 2) {
    		direction = BlockFace.SOUTH;
    	} else {
    		direction = BlockFace.WEST;
    	}

    	//clear any stack goodies
    	for (int y = coord.getY(); y < 256; y++) {
    		top = world.getBlockAt(coord.getX(), y, coord.getZ());
    		if (ItemManager.getTypeId(top) == CivData.BEDROCK) {
    			ItemManager.setTypeId(top, CivData.AIR);
    		}
    	}
    	
    	for (int y = coord.getY(); y < coord.getY() + FLAG_HEIGHT; y++) {
    		top = world.getBlockAt(coord.getX(), y, coord.getZ());
    		top.setType(Material.BEDROCK);

    		ProtectedBlock pb = new ProtectedBlock(new BlockCoord(top), ProtectedBlock.Type.TRADE_MARKER);
    		CivGlobal.addProtectedBlock(pb);
    		if (sync) {
    		try {
				pb.saveNow();
			} catch (SQLException e) {
				CivLog.warning("Unable to Protect Goodie Sign");
				e.printStackTrace();
			}    
    		} else {
    			pb.save();
    		}
    	}

        final Location loc = new Location(coord.getBlock().getWorld(), coord.getBlock().getX() + 0.5, (double)(coord.getBlock().getY() + 5), coord.getBlock().getZ() + 0.5);
        final Hologram holoTradeGood = HologramsAPI.createHologram(CivCraftGoods, loc);

        ItemLine tradeGoodItem;
		TextLine tradeGoodName;
		TextLine tradeGoodValue;
        String color;
        if (good.water) {
            tradeGoodItem = holoTradeGood.appendItemLine(ItemManager.createItemStack(good.material, (short)good.material_data, 1));
            tradeGoodName = holoTradeGood.appendTextLine(CivColor.GoldBold + "\u0422\u043e\u0440\u0433\u043e\u0432\u044b\u0439 \u0440\u0435\u0441\u0443\u0440\u0441 " + CivColor.LightBlueBold + CivColor.ITALIC + good.name);
            tradeGoodValue = holoTradeGood.appendTextLine(CivColor.LightPurpleBold + "\u0414\u0430\u0435\u0442: ");
            color = "§7";
        }
        else {
            tradeGoodItem = holoTradeGood.appendItemLine(ItemManager.createItemStack(good.material, (short)good.material_data, 1));
            tradeGoodName = holoTradeGood.appendTextLine(CivColor.GoldBold + "\u0422\u043e\u0440\u0433\u043e\u0432\u044b\u0439 \u0440\u0435\u0441\u0443\u0440\u0441 " + CivColor.LightGreenBold + CivColor.ITALIC + good.name);
            tradeGoodValue = holoTradeGood.appendTextLine(CivColor.LightPurpleBold + "\u0414\u0430\u0435\u0442: ");
            color = "§c";
        }
		final TextLine tradeGoodBuffs = holoTradeGood.appendTextLine(color + getHumanBuffList(good));
    	Block signBlock = top.getRelative(direction);
    	signBlock.setType(Material.WALL_SIGN);
    	//TODO make sign a structure sign?
    			//          Civ.protectedBlockTable.put(Civ.locationHash(signBlock.getLocation()), 
    	//          		new ProtectedBlock(signBlock, null, null, null, ProtectedBlock.Type.TRADE_MARKER));

    	BlockState state = signBlock.getState();

    	if (state instanceof Sign) {
    		Sign sign = (Sign)state;
    		org.bukkit.material.Sign data = (org.bukkit.material.Sign)state.getData();

    		data.setFacingDirection(direction);
    		sign.setLine(0, CivSettings.localize.localizedString("TradeGoodSign_Heading"));
    		sign.setLine(1, "----");
    		sign.setLine(2, good.name);
    		sign.setLine(3, "");
    		sign.update(true);

    		ConstructSign structSign = new ConstructSign(new BlockCoord(signBlock), null);
    		structSign.setAction("");
    		structSign.setType("");
    		structSign.setText(sign.getLines());
    		structSign.setDirection(ItemManager.getData(sign.getData()));
    		CivGlobal.addConstructSign(structSign);
            ProtectedBlock pbsign = new ProtectedBlock(new BlockCoord(signBlock), ProtectedBlock.Type.TRADE_MARKER);
            CivGlobal.addProtectedBlock(pbsign);
            if (sync) {
                try {
                	pbsign.saveNow();
                    structSign.saveNow();
                } catch (SQLException e) {
                	e.printStackTrace();
                }
            } else {
            	pbsign.save();
                structSign.save();
            }
    	}
        
    	if (sync) {
	    	try {
				new_good.saveNow();
			} catch (SQLException e) {
				e.printStackTrace();
			}
    	} else {
    		new_good.save();
    	}
    }
    
    public static String getHumanBuffList(final ConfigTradeGood tradeGood) {
        final StringBuilder buffs = new StringBuilder();
        for (final ConfigBuff configBuff : tradeGood.buffs.values()) {
            buffs.append(configBuff.name).append(", ");
        }
        return buffs.toString();
    }

    public boolean checkForDuplicateTradeGood(String worldName, int centerX, int centerY, int centerZ) {
    	/* 
    	 * Search downward to bedrock for any trade goodies here. If we find one, don't generate. 
    	 */
    	
    	BlockCoord coord = new BlockCoord(worldName, centerX, centerY, centerZ);
    	for (int y = centerY; y > 0; y--) {
    		coord.setY(y);    		
    		
    		if (CivGlobal.getTradeGood(coord) != null) {
				/* Already a trade goodie here. DONT Generate it. */
				return true;
    		}		
    	}
    	return false;
    }
    
    @Override
	public void populate(World world, Random random, Chunk source) {
    	
    	ChunkCoord cCoord = new ChunkCoord(source);
    	TradeGoodPick pick = CivGlobal.tradeGoodPreGenerator.goodPicks.get(cCoord);
    	if (pick != null) {
			int centerX = (source.getX() << 4) + 8;
			int centerZ = (source.getZ() << 4) + 8;
			int centerY = world.getHighestBlockYAt(centerX, centerZ);
			BlockCoord coord = new BlockCoord(world.getName(), centerX, centerY, centerZ);

			if (checkForDuplicateTradeGood(world.getName(), centerX, centerY, centerZ)) {
				return;
			}
			
			// Determine if we should be a water good.
			ConfigTradeGood good;
			if (ItemManager.getBlockTypeIdAt(world, centerX, centerY-1, centerZ) == CivData.WATER || 
				ItemManager.getBlockTypeIdAt(world, centerX, centerY-1, centerZ) == CivData.WATER_RUNNING) {
				good = pick.waterPick;
			}  else {
				good = pick.landPick;
			}
			
			// Randomly choose a land or water good.
			if (good == null) {
				System.out.println("Could not find suitable good type during populate! aborting.");
				return;
			}
			
			// Create a copy and save it in the global hash table.
			buildTradeGoodie(good, coord, world, false);
    	}
 	
    }

    public static void setTradeGoodInfo(final ItemLine item, final TextLine name, final TextLine value, final TextLine buffs) {
        item.setTouchHandler(whoClicked -> CivMessage.send(whoClicked, CivColor.LightBlueItalic + CivSettings.localize.localizedString("tradeHoloMoreInfoHere")));
        name.setTouchHandler(whoClicked -> CivMessage.send(whoClicked, CivColor.LightBlueItalic + CivSettings.localize.localizedString("tradeHoloMoreInfoHere")));
        value.setTouchHandler(whoClicked -> CivMessage.send(whoClicked, CivColor.LightBlueItalic + CivSettings.localize.localizedString("tradeHoloMoreInfoHere")));
        buffs.setTouchHandler(whoClicked -> CivMessage.send(whoClicked, CivColor.LightBlueItalic + CivSettings.localize.localizedString("tradeHoloMoreInfoHere")));
    }
}