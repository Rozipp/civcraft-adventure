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

import java.util.ArrayList;

import com.avrgaming.civcraft.construct.Construct;

public class RegisterComponentAsync implements Runnable {

	public Construct construct;
	public Component component;
	public String name;
	boolean register;
	
	public RegisterComponentAsync(Construct construct, Component component, String name, boolean register) {
		this.construct = construct;
		this.component = component;
		this.name = name;
		this.register = register;
	}
	
	
	@Override
	public void run() {
		
		if (register) {
		Component.componentsLock.lock();
			try {
				ArrayList<Component> components = Component.componentsByType.get(name);
				
				if (components == null) {
					components = new ArrayList<Component>();
				}
			
				components.add(component);
				Component.componentsByType.put(name, components);
				if (construct != null) {
					construct.attachedComponents.add(component);
				}
			} finally {
				Component.componentsLock.unlock();
			}		
		} else {
			Component.componentsLock.lock();
			try {
				ArrayList<Component> components = Component.componentsByType.get(name);
				
				if (components == null) {
					return;
				}
			
				components.remove(component);
				Component.componentsByType.put(name, components);
				if (construct != null) {
					construct.attachedComponents.remove(component);
				}
			} finally {
				Component.componentsLock.unlock();
			}
		}
		
	}

	
}
