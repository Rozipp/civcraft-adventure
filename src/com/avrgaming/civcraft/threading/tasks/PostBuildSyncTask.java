/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.threading.tasks;

import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.template.Template;

public class PostBuildSyncTask implements Runnable {

	/* Search the template for special command blocks and handle them *after* the structure has finished building. */

	Template tpl;
	Buildable buildable;

	public PostBuildSyncTask(Template tpl, Buildable buildable) {
		this.tpl = tpl;
		this.buildable = buildable;
	}

	@Override
	public void run() {
		buildable.processCommandSigns(tpl);
	}

}
