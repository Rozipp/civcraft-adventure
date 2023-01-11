
package com.avrgaming.civcraft.construct.wonders;

import com.avrgaming.civcraft.components.ProjectileLightningComponent;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CivColor;

public class StatueOfZeus
extends Wonder {
    ProjectileLightningComponent teslaComponent;

    public StatueOfZeus(String id, Town town) {
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
        if (this.getTownOwner().getBuffManager().hasBuff("buff_powerstation")) rate = 1.4;
        return (int)((double)this.teslaComponent.getDamage() * rate);
    }

    public void setTurretLocation(BlockCoord absCoord) {
        this.teslaComponent.setTurretLocation(absCoord);
    }

    @Override
    public int getMaxHitPoints() {
        double rate = 1.0;
        if (this.getTownOwner().getBuffManager().hasBuff("buff_chichen_itza_tower_hp")) {
            rate += this.getTownOwner().getBuffManager().getEffectiveDouble("buff_chichen_itza_tower_hp");
        }
        if (this.getTownOwner().getBuffManager().hasBuff("buff_barricade")) {
            rate += this.getTownOwner().getBuffManager().getEffectiveDouble("buff_barricade");
        }
        return (int)((double)this.getInfo().max_hitpoints * rate);
    }

    @Override
    protected void removeBuffs() {
        this.removeBuffFromTown(this.getTownOwner(), "buff_statue_of_zeus_tower_range");
        this.removeBuffFromTown(this.getTownOwner(), "buff_statue_of_zeus_struct_regen");
    }

    @Override
    protected void addBuffs() {
        this.addBuffToTown(this.getTownOwner(), "buff_statue_of_zeus_tower_range");
        this.addBuffToTown(this.getTownOwner(), "buff_statue_of_zeus_struct_regen");
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
            this.getTownOwner().getTreasury().deposit(totalCoins);
            this.getTownOwner().SM.addCulture(totalCulture);
            int captured = totalCulture / culture;
            CivMessage.sendCiv(this.getCivOwner(), CivSettings.localize.localizedString("var_statue_of_zeus_addedCoinsAndCulture",
            		CivColor.LightGreen + totalCulture + CivColor.RESET, CivColor.Gold + totalCoins + " " + CivSettings.CURRENCY_NAME + CivColor.RESET, CivColor.Rose + captured + CivColor.RESET,
            		CivColor.Yellow + this.getTownOwner().getName() + CivColor.RESET));
        }
    }
}

