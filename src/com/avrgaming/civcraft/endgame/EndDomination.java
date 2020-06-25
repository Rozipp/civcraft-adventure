
package com.avrgaming.civcraft.endgame;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TreeMap;
import com.avrgaming.civcraft.endgame.EndGameCondition;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.wonders.Wonder;
import com.avrgaming.civcraft.war.War;

public class EndDomination
extends EndGameCondition {
    public static boolean check = false;
    int daysAfterStart;
    int timesMore;
    Date startDate = null;

    @Override
    public void onLoad() {
        this.daysAfterStart = Integer.valueOf(this.getString("days_after_start"));
        this.timesMore = Integer.valueOf(this.getString("timesMore"));
        this.getStartDate();
    }

    private void getStartDate() {
        String key = "endcondition:domination:startdate";
        ArrayList<SessionEntry> entries = CivGlobal.getSessionDatabase().lookup(key);
        if (entries.isEmpty()) {
            this.startDate = new Date();
            CivGlobal.getSessionDatabase().add(key, "" + this.startDate.getTime(), 0, 0, 0);
        } else {
            long time = Long.valueOf(entries.get((int)0).value);
            this.startDate = new Date(time);
        }
    }

    private boolean isAfterStartupTime() {
        Calendar startCal = Calendar.getInstance();
        startCal.setTime(this.startDate);
        Calendar now = Calendar.getInstance();
        startCal.add(5, this.daysAfterStart);
        return now.after(startCal);
    }

    @Override
    public String getSessionKey() {
        return "endgame:domination";
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public boolean check(Civilization civ) {
        Civilization top1;
        Civilization top2;
        if (!this.isAfterStartupTime()) {
            return false;
        }
        TreeMap<Integer, Civilization> treeMap = CivGlobal.civilizationScores;
        synchronized (treeMap) {
            top1 = (Civilization)CivGlobal.civilizationScores.values().toArray()[0];
            top2 = (Civilization)CivGlobal.civilizationScores.values().toArray()[1];
        }
        int diff = top1.getScore() / top2.getScore();
        if (diff < 9) {
            return false;
        }
        int wonderCount = 0;
        for (Town town : top1.getTowns()) {
            for (Wonder wonder : town.SM.getWonders()) {
                if (wonder.getConfigId().equalsIgnoreCase("w_space_shuttle") || wonder.getConfigId().equalsIgnoreCase("w_colosseum") || wonder.getConfigId().equalsIgnoreCase("")) continue;
                ++wonderCount;
            }
        }
        if (wonderCount < 6) {
            return false;
        }
        check = true;
        War.time_declare_days = 1;
        return true;
    }

    @Override
    protected void onWarDefeat(Civilization civ) {
        this.onFailure(civ);
    }
}

