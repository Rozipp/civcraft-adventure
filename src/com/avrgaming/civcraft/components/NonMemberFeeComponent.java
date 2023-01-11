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
package com.avrgaming.civcraft.components;

import java.text.DecimalFormat;
import java.util.ArrayList;

import com.avrgaming.civcraft.construct.Buildable;
import com.avrgaming.civcraft.construct.Construct;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.sessiondb.SessionEntry;

import lombok.Getter;

@Getter
public class NonMemberFeeComponent extends Component {

	private double feeRate = 0.05;
	
	private String getKey() {
		return getConstruct().getDisplayName()+":"+getConstruct().getId()+":"+"fee";
	}
	
	@Override
	public void onLoad() {
		CivLog.debug("onLoad component " + this.typeName);
		ArrayList<SessionEntry> entries = CivGlobal.getSessionDatabase().lookup(getKey());
		
		if (entries.size() == 0) {
			getConstruct().sessionAdd(getKey(), ""+feeRate);
			return;
		}
		
		feeRate = Double.valueOf(entries.get(0).value);
	}

	@Override
	public void onSave() {
		CivLog.debug("onSave component " + this.typeName);
		ArrayList<SessionEntry> entries = CivGlobal.getSessionDatabase().lookup(getKey());
		
		if (entries.size() == 0) {
			getConstruct().sessionAdd(getKey(), ""+feeRate);
			return;
		}
		CivGlobal.getSessionDatabase().update(entries.get(0).request_id, getKey(), ""+feeRate);
	}


	public void setFeeRate(double feeRate) {
		this.feeRate = feeRate;
		onSave();
	}

	
	public String getFeeString() {
		DecimalFormat df = new DecimalFormat();
		return ""+df.format(this.getFeeRate()*100)+"%";
	}
	
}