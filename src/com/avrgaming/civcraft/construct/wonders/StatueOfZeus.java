
package com.avrgaming.civcraft.construct.wonders;

import java.sql.ResultSet;
import java.sql.SQLException;
import com.avrgaming.civcraft.components.ProjectileLightningComponent;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.wonders.Wonder;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CivColor;

public class StatueOfZeus
extends Wonder {
    ProjectileLightningComponent teslaComponent;

    public StatueOfZeus(ResultSet rs) throws SQLException, CivException {
        super(rs);
    }

    public StatueOfZeus(String id, Town town) throws CivException {
        super(id, town);
    }

    @Override
    public void loadSettings() {
        super.loadSettings();
        this.teslaComponent = new ProjectileLightningComponent(this);
        this.teslaComponent.createComponent(this);
        this.teslaComponent.setDamage(this.getDamage());
    }

    public int getDamage() {
        double rate = 1.0;
        if (this.getTown().getBuffManager().hasBuff("buff_powerstation")) {
            rate = 1.4;
        }
        return (int)((double)this.teslaComponent.getDamage() * rate);
    }

    public void setTurretLocation(BlockCoord absCoord) {
        this.teslaComponent.setTurretLocation(absCoord);
    }

    @Override
    public int getMaxHitPoints() {
        double rate = 1.0;
        if (this.getTown().getBuffManager().hasBuff("buff_chichen_itza_tower_hp")) {
            rate += this.getTown().getBuffManager().getEffectiveDouble("buff_chichen_itza_tower_hp");
        }
        if (this.getTown().getBuffManager().hasBuff("buff_barricade")) {
            rate += this.getTown().getBuffManager().getEffectiveDouble("buff_barricade");
        }
        return (int)((double)this.getInfo().max_hitpoints * rate);
    }

    @Override
    public void onComplete() {
        this.addBuffs();
    }

    @Override
    protected void removeBuffs() {
        this.removeBuffFromTown(this.getTown(), "buff_statue_of_zeus_tower_range");
        this.removeBuffFromTown(this.getTown(), "buff_statue_of_zeus_struct_regen");
    }

    @Override
    protected void addBuffs() {
        this.addBuffToTown(this.getTown(), "buff_statue_of_zeus_tower_range");
        this.addBuffToTown(this.getTown(), "buff_statue_of_zeus_struct_regen");
    }

    public void processBonuses() {
        int culture = 500;
        int coins = 10000;
        int totalCulture = 0;
        int totalCoins = 0;
        for (Town town : CivGlobal.getTowns()) {
            if (town.getMotherCiv() == null) continue;
            totalCulture += culture;
            totalCoins += coins;
        }
        if (totalCoins != 0) {
            this.getTown().getTreasury().deposit(totalCoins);
            this.getTown().SM.addCulture(totalCulture);
            int captured = totalCulture / culture;
            CivMessage.sendCiv(this.getCiv(), CivSettings.localize.localizedString("var_statue_of_zeus_addedCoinsAndCulture",
            		CivColor.LightGreen + totalCulture + CivColor.RESET, CivColor.Gold + totalCoins + " " + CivSettings.CURRENCY_NAME + CivColor.RESET, CivColor.Rose + captured + CivColor.RESET,
            		CivColor.Yellow + this.getTown().getName() + CivColor.RESET));
        }
    }
}

