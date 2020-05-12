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
package com.avrgaming.civcraft.object;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.enchantment.CustomEnchantment;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.structure.Library;

public class LibraryEnchantment {
	public CustomEnchantment enchant;
	public int level;
	public double price;
	public String name;
	public String displayName;

	public LibraryEnchantment(String name, int lvl, double p) throws CivException {
		enchant = Library.getEnchantFromString(name);
		if (enchant == null) {
			throw new CivException(CivSettings.localize.localizedString("libraryEnchantError1", name));
		}
		level = lvl;
		price = p;

		this.name = name;
		displayName = enchant.getDisplayName(level);

	}
}
