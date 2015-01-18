/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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

import java.io.Serializable;

public class GearData implements Serializable {

	private static final long	serialVersionUID	= 1L;

	public long					absoluteTime;

	/**
	 * Gears are in the HEX format (left to right)
	 * <p>
	 * Front teeth<br>
	 * Front gear number<br>
	 * Back teeth<br>
	 * Back gear number<br>
	 */
	public long					gears;

	public int getFrontGearNum() {
		return (int) (gears >> 16 & 0xff);
	}

	public int getFrontGearTeeth() {
		return (int) (gears >> 24 & 0xff);
	}

	public int getRearGearNum() {
		return (int) (gears & 0xff);
	}

	public int getRearGearTeeth() {
		return (int) (gears >> 8 & 0xff);
	}

	@Override
	public String toString() {
		return "GearData ["
				+ ("absoluteTime=" + absoluteTime + ", ")
				+ ("FrontGearNum=" + getFrontGearNum() + ", ")
				+ ("FrontGearTeeth=" + getFrontGearTeeth() + ", ")
				+ ("RearGearNum=" + getRearGearNum() + ", ")
				+ ("RearGearTeeth=" + getRearGearTeeth())
				+ "]\n";
	}

}
