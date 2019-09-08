/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.threading.tasks;

import java.io.IOException;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.structure.wonders.Wonder;
import com.avrgaming.civcraft.template.Template;

public class onLoadTask implements Runnable {
	/* After all the trade outposts, town halls, and bonus goodies are loaded we'll call an event on all structures to load them. */

	@Override
	public void run() {

		/* Run all post-build sync tasks first. */
		for (Structure struct : CivGlobal.getStructures()) {
			try {
				Template tpl;
				try {
					if (struct.getSavedTemplatePath() == null && struct.hasTemplate()) {
						CivLog.warning("structure:" + struct.getDisplayName() + " did not have a template name set but says it needs one!");
						continue;
					}
					if (!struct.hasTemplate()) continue;
					tpl = Template.getTemplate(struct.getSavedTemplatePath(), null);
				} catch (CivException | IOException e) {
					e.printStackTrace();
					return;
				}
				/* Re-run the post build on the command blocks we found. */
				if (struct.isPartOfAdminCiv())
					struct.processValidateCommandBlockRelative(tpl);
				else
					if (struct.isActive()) struct.processCommandSigns(tpl);
			} catch (Exception e) {
				CivLog.error("-----ON LOAD EXCEPTION-----");
				if (struct != null) {
					CivLog.error("Structure:" + struct.getDisplayName());
					if (struct.getTown() != null) CivLog.error("Town:" + struct.getTown().getName());
				}
				CivLog.error(e.getMessage());
				e.printStackTrace();
			}
		}

		for (Wonder wonder : CivGlobal.getWonders()) {
			Template tpl;
			try {
				try {
					tpl = Template.getTemplate(wonder.getSavedTemplatePath(), null);
				} catch (CivException | IOException e) {
					e.printStackTrace();
					return;
				}
				/* Re-run the post build on the command blocks we found. */
				if (wonder.isActive()) wonder.processCommandSigns(tpl);
			} catch (Exception e) {
				CivLog.error("-----ON LOAD EXCEPTION-----");
				if (wonder != null) {
					CivLog.error("Structure:" + wonder.getDisplayName());
					if (wonder.getTown() != null) {
						CivLog.error("Town:" + wonder.getTown().getName());
					}
				}
				CivLog.error(e.getMessage());
				e.printStackTrace();

			}
		}

		/* Now everything should be loaded and ready to go. */
		for (Structure struct : CivGlobal.getStructures()) {
			try {
				struct.onLoad();
			} catch (Exception e) {
				CivLog.error("-----ON LOAD EXCEPTION-----");
				if (struct != null) {
					CivLog.error("Structure:" + struct.getDisplayName());
					if (struct.getTown() != null) CivLog.error("Town:" + struct.getTown().getName());
				}
				CivLog.error(e.getMessage());
				e.printStackTrace();
			}
		}
		for (Wonder wonder : CivGlobal.getWonders()) {
			try {
				wonder.onLoad();
			} catch (Exception e) {
				CivLog.error("-----ON LOAD EXCEPTION-----");
				if (wonder != null) {
					CivLog.error("Structure:" + wonder.getDisplayName());
					if (wonder.getTown() != null) CivLog.error("Town:" + wonder.getTown().getName());
				}
				CivLog.error(e.getMessage());
				e.printStackTrace();

			}
		}
	}
}
