package com.avrgaming.civcraft.construct.structures;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigGrocerLevel;
import com.avrgaming.civcraft.construct.ConstructSign;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.CivColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

public class Grocer extends Structure {

	private int level = 1;

	public Grocer( String id, Town town) {
		super(id, town);
		setLevel(town.BM.saved_grocer_levels);
	}

	@Override
	public String getDynmapDescription() {
		String out = "<u><b>"+this.getDisplayName()+"</u></b><br/>";

		for (int i = 0; i < level; i++) {
			ConfigGrocerLevel grocerlevel = CivSettings.grocerLevels.get(i+1);
			out += "<b>"+grocerlevel.itemName+"</b> "+CivSettings.localize.localizedString("Amount")+" "+grocerlevel.amount+ " "+CivSettings.localize.localizedString("Price")+" "+grocerlevel.price+" "+CivSettings.CURRENCY_NAME+".<br/>";
		}
		
		return out;
	}
	
	@Override
	public String getMarkerIconName() {
		return "cutlery";
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public double getNonResidentFee() {
		return getNonMemberFeeComponent().getFeeRate();
	}

	public void setNonResidentFee(double nonResidentFee) {
		this.getNonMemberFeeComponent().setFeeRate(nonResidentFee);
	}
	
	private String getNonResidentFeeString() {
		return "Fee: "+ ((int)(getNonResidentFee()*100) + "%");
	}
	
	private ConstructSign getSignFromSpecialId(int special_id) {
		for (ConstructSign sign : getSigns()) {
			int id = Integer.parseInt(sign.getAction());
			if (id == special_id) {
				return sign;
			}
		}
		return null;
	}
	
	public void sign_buy_material(Player player, String itemName, int id, byte data, int amount, double price) {
		Resident resident;
		int payToTown = (int) Math.round(price*this.getNonResidentFee());
		try {
				
				resident = CivGlobal.getResident(player.getName());
				Town t = resident.getTown();
			
				if (t == this.getTownOwner()) {
					// Pay no taxes! You're a member.
					resident.buyItem(itemName, id, data, price, amount);
					CivMessage.send(player, CivColor.LightGreen + CivSettings.localize.localizedString("var_grocer_msgBought",amount,itemName,price+" "+CivSettings.CURRENCY_NAME));
				} else {
					// Pay non-resident taxes
					resident.buyItem(itemName, id, data, price + payToTown, amount);
					getTownOwner().depositDirect(payToTown);
					CivMessage.send(player, CivColor.LightGreen + CivSettings.localize.localizedString("var_grocer_msgBought",amount,itemName,price,CivSettings.CURRENCY_NAME));
					CivMessage.send(player, CivColor.Yellow + CivSettings.localize.localizedString("var_grocer_msgPaidTaxes",this.getTownOwner().getName(),payToTown+" "+CivSettings.CURRENCY_NAME));
				}
			
			}
			catch (CivException e) {
				CivMessage.send(player, CivColor.Rose + e.getMessage());
			}
	}

	
	@Override
	public void updateSignText() {
		int count;
	
		for (count = 0; count < level; count++) {
			ConstructSign sign = getSignFromSpecialId(count);
			if (sign == null) {
				CivLog.error("sign from special id was null, id:"+count);
				return;
			}
			ConfigGrocerLevel grocerlevel = CivSettings.grocerLevels.get(count+1);
			
			sign.setText(CivSettings.localize.localizedString("grocer_sign_buy")+"\n"+grocerlevel.itemName+"\n"+
						 CivSettings.localize.localizedString("grocer_sign_for")+" "+grocerlevel.price+" "+CivSettings.CURRENCY_NAME+"\n"+
					     getNonResidentFeeString());
			
			sign.update();
		}
		
		for (; count < getSigns().size(); count++) {
			ConstructSign sign = getSignFromSpecialId(count);
			if (sign == null) {
				CivLog.error("sign from special id was null, id:"+count);
				return;
			}
			sign.setText(CivSettings.localize.localizedString("grocer_sign_empty"));
			sign.update();
		}
		
	}
	
	@Override
	public void processSignAction(Player player, ConstructSign sign, PlayerInteractEvent event) {
		int special_id = Integer.parseInt(sign.getAction());
		if (special_id < this.level) {
			ConfigGrocerLevel grocerlevel = CivSettings.grocerLevels.get(special_id+1);
			sign_buy_material(player, grocerlevel.itemName, grocerlevel.itemId, 
					(byte)grocerlevel.itemData, grocerlevel.amount, grocerlevel.price);
		} else {
			CivMessage.send(player, CivColor.Rose+CivSettings.localize.localizedString("grocer_sign_needUpgrade"));
		}
	}
	
	
}
