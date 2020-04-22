/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import com.avrgaming.civcraft.construct.Construct;
import com.avrgaming.civcraft.threading.TaskMaster;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Component {

	public static ConcurrentHashMap<String, ArrayList<Component>> componentsByType = new ConcurrentHashMap<String, ArrayList<Component>>();

	public static ReentrantLock componentsLock = new ReentrantLock();

	// Allow components to be specified in YAMLs. To do this each component must be given a name and some attributes.
	// Examples:
	// components:
	// - name: 'AttributeStatic'
	// type: 'direct'
	// attribute: 'beakers'
	// value: '50.0'
	//
	// We use the power of YAML to find the key-values other than name and populate them here.
	// We then register that component to the structure automatically on construction.
	private String name;
	private Construct construct;
	private HashMap<String, String> attributes = new HashMap<String, String>();
	protected String typeName = null;

	public void createComponent(Construct constr) {
		this.createComponent(constr, false);
	}

	public void createComponent(Construct constr, boolean async) {
		String typeName = this.typeName == null ? this.getClass().getName() : this.typeName;
		startRegisterComponentTask(constr, typeName, true, async);
		this.construct = constr;
	}

	public void destroyComponent() {
		startRegisterComponentTask(null, this.getClass().getName(), false, true);
	}

	public void onLoad() {
	}

	public void onSave() {
	}

	public String getString(String key) {
		return attributes.get(key);
	}

	public double getDouble(String key) {
		return Double.valueOf(attributes.get(key));
	}

	public void setAttribute(String key, String value) {
		attributes.put(key, value);
	}

	public boolean isActive() {
		if (construct != null) {
			return construct.isActive();
		} else {
			return false;
		}
	}

	public void startRegisterComponentTask(Construct constr, String name, boolean register, boolean async) {
		Component component = this;
		class RegisterComponentAsync implements Runnable {
			@Override
			public void run() {
				Component.componentsLock.lock();
				try {
					ArrayList<Component> components = Component.componentsByType.get(name);
					if (register) {
						if (components == null) components = new ArrayList<Component>();
						components.add(component);
						if (constr != null) { constr.attachedComponents.add(component); }
					} else {
						if (components == null) return;
						components.remove(component);
						if (constr != null) { constr.attachedComponents.remove(component); }
					}
					Component.componentsByType.put(name, components);
				} finally {
					Component.componentsLock.unlock();
				}
			}
		}

		if (async) TaskMaster.asyncTask(new RegisterComponentAsync(), 0);
		else TaskMaster.syncTask(new RegisterComponentAsync());
	}

}
