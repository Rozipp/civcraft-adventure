package com.avrgaming.global.scores;

import com.avrgaming.civcraft.threading.*;
import java.text.*;
import com.avrgaming.civcraft.main.*;
import java.sql.*;
import com.avrgaming.civcraft.object.*;
import java.util.*;

public class GlobalTickEvent extends CivAsyncTask
{
    protected DecimalFormat df;
    
    public GlobalTickEvent() {
        this.df = new DecimalFormat();
    }
    
    @Override
    public void run() {
        final TreeMap<Integer, Civilization> civScores = new TreeMap<Integer, Civilization>();
        for (final Civilization civ : CivGlobal.getCivs()) {
            if (civ.isAdminCiv()) {
                continue;
            }
            civScores.put(civ.getScore(), civ);
            try {
                ScoreManager.UpdateScore(civ, civ.getScore());
            }
            catch (SQLException e) {
                e.printStackTrace();
            }
        }
        final TreeMap<Integer, Town> townScores = new TreeMap<Integer, Town>();
        for (final Town town : CivGlobal.getTowns()) {
            if (town.getCiv().isAdminCiv()) {
                continue;
            }
            try {
                townScores.put(town.getScore(), town);
            }
            catch (Exception e2) {
                e2.printStackTrace();
            }
            try {
                ScoreManager.UpdateScore(town, town.getScore());
            }
            catch (SQLException e3) {
                e3.printStackTrace();
            }
        }
        synchronized (CivGlobal.civilizationScores) {
            CivGlobal.civilizationScores = civScores;
        }
        synchronized (CivGlobal.townScores) {
            CivGlobal.townScores = townScores;
        }
    }
}
