
package com.avrgaming.civcraft.construct.structures;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigAlchLevel;
import com.avrgaming.civcraft.construct.ConstructSign;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.CivColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

public class Alch extends Structure {
    private int level = 1;

    public Alch(String id, Town town) {
        super(id, town);
    }

    @Override
    public String getDynmapDescription() {
        StringBuilder out = new StringBuilder("<u><b>" + this.getDisplayName() + "</u></b><br/>");
        for (int i = 0; i < this.level; ++i) {
            ConfigAlchLevel alchlevel = CivSettings.alchLevels.get(i + 1);
            out.append("<b>").append(alchlevel.itemName).append("</b> ").append(CivSettings.localize.localizedString("Amount")).append(" ").append(alchlevel.amount).append(" ").append(CivSettings.localize.localizedString("Price")).append(" ").append(alchlevel.price).append(" ").append(CivSettings.CURRENCY_NAME).append(".<br/>");
        }
        return out.toString();
    }

    @Override
    public String getMarkerIconName() {
        return "drink";
    }

    public int getLevel() {
        return this.level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public double getNonResidentFee() {
        return this.getNonMemberFeeComponent().getFeeRate();
    }

    public void setNonResidentFee(double nonResidentFee) {
        this.getNonMemberFeeComponent().setFeeRate(nonResidentFee);
    }

    private String getNonResidentFeeString() {
        return "Fee: " + ((int) (this.getNonResidentFee() * 100.0) + "%");
    }

    private ConstructSign getSignFromSpecialId(int special_id) {
        for (ConstructSign sign : this.getSigns()) {
            int id = Integer.parseInt(sign.getAction());
            if (id != special_id) continue;
            return sign;
        }
        return null;
    }

    public void sign_buy_material(Player player, String itemName, int id, byte data, int amount, double price) {
        int payToTown = (int)Math.round(price * this.getNonResidentFee());
        try {
            Resident resident = CivGlobal.getResident(player.getName());
            Civilization c = resident.getCiv();
            if (c == this.getCivOwner()) {
                resident.buyItem(itemName, id, data, price, amount);
                CivMessage.send(player, CivColor.Green + CivSettings.localize.localizedString("var_alch_msgBought", amount, itemName, price + " " + CivSettings.CURRENCY_NAME));
                return;
            }
            resident.buyItem(itemName, id, data, price + (double) payToTown, amount);
            this.getTownOwner().depositDirect(payToTown);
            CivMessage.send(player, CivColor.Green + CivSettings.localize.localizedString("var_alch_msgBought", amount, itemName, price, CivSettings.CURRENCY_NAME));
            CivMessage.send(player, CivColor.Yellow + CivSettings.localize.localizedString("var_taxes_paid", this.getTownOwner().getName(), payToTown + " " + CivSettings.CURRENCY_NAME));
        }
        catch (CivException e) {
            CivMessage.send(player, CivColor.Red + e.getMessage());
        }
    }

    @Override
    public void updateSignText() {
        ConstructSign sign;
        int count;
        for (count = 0; count < this.level; ++count) {
            sign = this.getSignFromSpecialId(count);
            if (sign == null) {
                CivLog.error("sign from special id was null, id:" + count);
                return;
            }
            ConfigAlchLevel alchlevel = CivSettings.alchLevels.get(count + 1);
            sign.setText(CivSettings.localize.localizedString("alch_sign_buy") + "\n" + alchlevel.itemName + "\n" + CivSettings.localize.localizedString("alch_sign_for") + " " + alchlevel.price + " " + CivSettings.CURRENCY_NAME + "\n" + this.getNonResidentFeeString());
            sign.update();
        }
        while (count < this.getSigns().size()) {
            sign = this.getSignFromSpecialId(count);
            if (sign == null) {
                CivLog.error("sign from special id was null, id:" + count);
                return;
            }
            sign.setText(CivSettings.localize.localizedString("alch_sign_empty"));
            sign.update();
            ++count;
        }
    }

    @Override
    public void processSignAction(Player player, ConstructSign sign, PlayerInteractEvent event) {
        int special_id = Integer.parseInt(sign.getAction());
        if (special_id < this.level) {
            ConfigAlchLevel alchlevel = CivSettings.alchLevels.get(special_id + 1);
            this.sign_buy_material(player, alchlevel.itemName, alchlevel.itemId, (byte) alchlevel.itemData, alchlevel.amount, alchlevel.price);
        } else {
            CivMessage.send(player, CivColor.Red + CivSettings.localize.localizedString("alch_sign_needUpgrade"));
        }
    }

    @Override
    public void onPostBuild() {
        this.setLevel(getTownOwner().BM.saved_alch_levels);
    }
}

