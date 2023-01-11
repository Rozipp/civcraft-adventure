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
package ua.rozipp.abstractplugin.exception;

import ua.rozipp.abstractplugin.APlugin;

public class InvalidConfiguration extends Exception {

	private static final long serialVersionUID = 6603010451357647626L;
	
	public InvalidConfiguration(String fileName, String path) {
		super("Failed to get configuration string \"" + path + "\" from file \"" + fileName + "\"");
	}

	@Override
	public String getLocalizedMessage() {
		return APlugin.getInstance().getLocalizer().getString("", "failed_to_get_configuration_string");
	}
}
