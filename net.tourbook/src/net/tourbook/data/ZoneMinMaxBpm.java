/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *******************************************************************************/
package net.tourbook.data;

public class ZoneMinMaxBpm {

	/**
	 * Age in years
	 */
	public int		age;
	public int		hrMax;

	public int[]	zoneMinBmp;
	public int[]	zoneMaxBmp;

	/**
	 * Set HR zones, age and max HR
	 * 
	 * @param zoneMinBmp
	 * @param zoneMaxBmp
	 * @param age
	 * @param hrMax
	 */
	public ZoneMinMaxBpm(final int[] zoneMinBmp, final int[] zoneMaxBmp, final int age, final int hrMax) {

		this.zoneMinBmp = zoneMinBmp;
		this.zoneMaxBmp = zoneMaxBmp;

		this.age = age;
		this.hrMax = hrMax;
	}

}
