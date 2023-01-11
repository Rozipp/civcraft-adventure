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
package com.avrgaming.civcraft.cache;

import java.util.Calendar;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;

import com.avrgaming.civcraft.components.ProjectileArrowComponent;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ArrowFiredCache {
	private ProjectileArrowComponent fromTower;
	private Location target;
	private Entity targetEntity;
	private Arrow arrow;
	private UUID uuid;
	private Calendar expired;
	private boolean hit = false;

	public ArrowFiredCache(ProjectileArrowComponent tower, Entity targetEntity, Arrow arrow) {
		this.setFromTower(tower);
		this.target = targetEntity.getLocation();
		this.targetEntity = targetEntity;
		this.setArrow(arrow);
		this.uuid = arrow.getUniqueId();
		expired = Calendar.getInstance();
		expired.add(Calendar.SECOND, 5);
	}

	public Object getUUID() {
		return uuid;
	}

	public void destroy(Arrow arrow) {
		arrow.remove();
		this.arrow = null;
		CivCache.arrowsFired.remove(this.getUUID());
		this.uuid = null;
	}

	public void destroy(Entity damager) {
		if (damager instanceof Arrow) this.destroy((Arrow) damager);
	}

}
