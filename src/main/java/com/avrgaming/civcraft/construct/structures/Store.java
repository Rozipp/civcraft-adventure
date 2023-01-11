package com.avrgaming.civcraft.construct.structures;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.ConstructSign;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.StoreMaterial;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.CivColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;

public class Store extends Structure {
	
	private int level = 1;
	
	ArrayList<StoreMaterial> materials = new ArrayList<>();
	
	public Store(String id, Town town) {
		super(id, town);
		setLevel(town.BM.saved_store_level);
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
		getNonMemberFeeComponent().setFeeRate(nonResidentFee);
	}
	
	private String getNonResidentFeeString() {
		return "Fee: "+ ((int)(getNonMemberFeeComponent().getFeeRate()*100) + "%");
	}
	
	public void addStoreMaterial(StoreMaterial mat) throws CivException {
		if (materials.size() >= 4) {
			throw new CivException(CivSettings.localize.localizedString("store_isFull"));
		}
		materials.add(mat);
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
	
	@Override
	public void updateSignText() {
		int count = 0;

		
		// iterate through materials, set signs using array...
		
		for (StoreMaterial mat : this.materials) {
			ConstructSign sign = getSignFromSpecialId(count);
			if (sign == null) {
				CivLog.error("sign from special id was null, id:"+count);
				return;
			}
			
			sign.setText(CivSettings.localize.localizedString("var_store_sign_buy",mat.name,((int)mat.price+" "+CivSettings.CURRENCY_NAME),getNonResidentFeeString()));
			sign.update();
			count++;
		}
		
		// We've finished with all of the materials, update the empty signs to show correct text.
		for (; count < getSigns().size(); count++) {
			ConstructSign sign = getSignFromSpecialId(count);
			sign.setText(CivSettings.localize.localizedString("store_sign_empty"));
			sign.update();
		}
	}
	
	@Override
	public void processSignAction(Player player, ConstructSign sign, PlayerInteractEvent event) {
		int special_id = Integer.parseInt(sign.getAction());
		if (special_id < this.materials.size()) {
			StoreMaterial mat = this.materials.get(special_id);
			sign_buy_material(player, mat.name, mat.type, mat.data, 64, mat.price);
		} else {
			CivMessage.send(player, CivColor.Rose+CivSettings.localize.localizedString("store_buy_empty"));
		}
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
					CivMessage.send(player, CivColor.LightGreen + CivSettings.localize.localizedString("var_market_buy",amount,itemName,price,CivSettings.CURRENCY_NAME));
					return;
				} else {
					// Pay non-resident taxes
					resident.buyItem(itemName, id, data, price + payToTown, amount);
					getTownOwner().depositDirect(payToTown);
					CivMessage.send(player, CivColor.Yellow + CivSettings.localize.localizedString("var_taxes_paid",payToTown,CivSettings.CURRENCY_NAME));
				}
			
			}
			catch (CivException e) {
				CivMessage.send(player, CivColor.Rose + e.getMessage());
			}
	}

	@Override
	public String getDynmapDescription() {
		String out = "<u><b>"+this.getDisplayName()+"</u></b><br/>";
		if (this.materials.size() == 0) {
			out += CivSettings.localize.localizedString("store_dynmap_nothingStocked");
		} 
		else {
			for (StoreMaterial mat : this.materials) {
				out += CivSettings.localize.localizedString("var_store_dynmap_item",mat.name,mat.price)+"<br/>";
			}
		}
		return out;
	}
	
	@Override
	public String getMarkerIconName() {
		return "bricks";
	}

	public void reset() {
		this.materials.clear();
		this.updateSignText();
	}
	
}
