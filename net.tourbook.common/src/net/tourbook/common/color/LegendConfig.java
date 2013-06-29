/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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
package net.tourbook.common.color;

import java.util.List;

import net.tourbook.common.UI;

/**
 * Configuration for the map legend to visualize one unit in a tour
 */
public class LegendConfig {

	public float			legendMinValue;
	public float			legendMaxValue;

	public List<Float>		units;
	public List<String>		unitLabels;

	public LegendUnitFormat	unitFormat	= LegendUnitFormat.Number;

	public String			unitText	= UI.EMPTY_STRING;

	/**
	 * Number of digits when label is formatted, default is 0.
	 */
	public int				numberFormatDigits;
}
