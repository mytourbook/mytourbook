/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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
import net.tourbook.common.util.StatusUtil;

/**
 * Configuration for the units in a map color provider.
 */
public class MapUnits implements Cloneable {

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

	@Override
	public MapUnits clone() {

		MapUnits clonedObject = null;

		// !!! fields are not cloned because they are overwritten when color provider is set !!!

		try {

			clonedObject = (MapUnits) super.clone();

		} catch (final CloneNotSupportedException e) {
			StatusUtil.log(e);
		}

		return clonedObject;
	}

	@Override
	public String toString() {
		final int maxLen = 10;
		return String.format("\n" //$NON-NLS-1$
				+ "MapUnits\n" //$NON-NLS-1$
				+ "   legendMinValue		=%s\n" //$NON-NLS-1$
				+ "   legendMaxValue		=%s\n" //$NON-NLS-1$
				+ "   units					=%s\n" //$NON-NLS-1$
				+ "   unitLabels			=%s\n" //$NON-NLS-1$
				+ "   unitFormat			=%s\n" //$NON-NLS-1$
				+ "   unitText				=%s\n" //$NON-NLS-1$
				+ "   numberFormatDigits	=%s\n", //$NON-NLS-1$
				legendMinValue,
				legendMaxValue,
				units != null ? units.subList(0, Math.min(units.size(), maxLen)) : null,
				unitLabels != null ? unitLabels.subList(0, Math.min(unitLabels.size(), maxLen)) : null,
				unitFormat,
				unitText,
				numberFormatDigits);
	}

}
