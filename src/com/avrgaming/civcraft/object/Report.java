
package com.avrgaming.civcraft.object;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.HashMap;

import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.main.CivLog;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Report
extends SQLObject {
    public static final String TABLE_NAME = "REPORTS";
    private String cause;
    private String proof;
    private String reportedBy;
    private long time;
    private boolean bug;
    private boolean closed;
    private String whoClosed;
    private String result;
    private long closeTime;

    public Report(String cause, String proof, String reportedBy, boolean bug) {
        this.cause = cause;
        this.proof = proof;
        this.reportedBy = reportedBy;
        this.time = Calendar.getInstance().getTimeInMillis();
        this.bug = bug;
        this.closed = false;
    }

    public Report(ResultSet rs) throws SQLException {
        this.load(rs);
    }

    public static void init() throws SQLException {
        if (!SQL.hasTable(TABLE_NAME)) {
            String table_create = "CREATE TABLE " + SQL.tb_prefix + TABLE_NAME + " (`id` int(11) unsigned NOT NULL auto_increment,`cause` mediumtext,`proof` mediumtext,`reportedBy` mediumtext,`time` long,`bug` boolean DEFAULT false,`closed` boolean DEFAULT false,`whoClosed` mediumtext,`result` mediumtext,`closeTime` long,PRIMARY KEY (`id`))";
            SQL.makeTable(table_create);
            CivLog.info("Created REPORTS table");
        } else {
            CivLog.info("REPORTS table OK!");
        }
    }

    @Override
    public void load(ResultSet rs) throws SQLException {
        this.setId(rs.getInt("id"));
        this.setCause(rs.getString("cause"));
        this.setProof(rs.getString("proof"));
        this.setReportedBy(rs.getString("reportedBy"));
        this.setTime(rs.getLong("time"));
        this.setBug(rs.getBoolean("bug"));
        this.setClosed(rs.getBoolean("closed"));
        this.setWhoClosed(rs.getString("whoClosed"));
        this.setResult(rs.getString("result"));
        this.setCloseTime(rs.getLong("closeTime"));
    }

    @Override
    public void saveNow() throws SQLException {
        HashMap<String, Object> hashmap = new HashMap<String, Object>();
        hashmap.put("cause", this.cause);
        hashmap.put("proof", this.proof);
        hashmap.put("reportedBy", this.reportedBy);
        hashmap.put("time", this.time);
        hashmap.put("bug", this.bug);
        hashmap.put("closed", this.closed);
        hashmap.put("whoClosed", this.whoClosed);
        hashmap.put("result", this.result);
        hashmap.put("closeTime", this.closeTime);
        SQL.updateNamedObject(this, hashmap, TABLE_NAME);
    }

    @Override
    public void delete() throws SQLException {
    }

    public void close(String whoClosed, String result) {
        this.setWhoClosed(whoClosed);
        this.setResult(result);
        this.setCloseTime(Calendar.getInstance().getTimeInMillis());
        this.setClosed(true);
        this.save();
    }
}

