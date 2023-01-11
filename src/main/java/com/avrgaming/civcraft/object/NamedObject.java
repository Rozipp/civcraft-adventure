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

import com.avrgaming.civcraft.exception.InvalidNameException;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NamedObject {

	/* Unique Id of named object. */
	private int id; 
	
	/* Display name of the object. */
	private String name;
		
	public void setName(String newname) throws InvalidNameException {
		validateName(newname);
		this.name = newname;
	}
	
	private static void validateName(String name) throws InvalidNameException {
		if (name == null) {
			throw new InvalidNameException();
		}
				
		switch (name.toLowerCase()) {
			case "":
			case "null":
			case "none":
			case "town":
			case "group":
			case "civ":
			case "resident":
				throw new InvalidNameException(name);
		}
	}	
}
