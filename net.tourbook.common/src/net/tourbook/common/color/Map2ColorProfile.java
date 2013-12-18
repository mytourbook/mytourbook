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

import net.tourbook.common.util.StatusUtil;

/**
 * Contains all colors to paint a tour in a map.
 */
public class Map2ColorProfile extends MapColorProfile implements Cloneable {

	private ColorValue[]	colorValues	= new ColorValue[] {
			new ColorValue(10, 255, 0, 0),
			new ColorValue(50, 100, 100, 0),
			new ColorValue(100, 0, 255, 0),
			new ColorValue(150, 0, 100, 100),
			new ColorValue(190, 0, 0, 255) };

	public Map2ColorProfile() {}

	/**
	 * @param valueColors
	 * @param minBrightness
	 * @param minBrightnessFactor
	 * @param maxBrightness
	 * @param maxBrightnessFactor
	 */
	public Map2ColorProfile(final ColorValue[] valueColors,
							final int minBrightness,
							final int minBrightnessFactor,
							final int maxBrightness,
							final int maxBrightnessFactor) {

		this.colorValues = valueColors;

		this.minBrightness = minBrightness;
		this.minBrightnessFactor = minBrightnessFactor;
		this.maxBrightness = maxBrightness;
		this.maxBrightnessFactor = maxBrightnessFactor;
	}

	/**
	 * @param valueColors
	 * @param minBrightness
	 * @param minBrightnessFactor
	 * @param maxBrightness
	 * @param maxBrightnessFactor
	 * @param isMinOverwrite
	 * @param minOverwrite
	 * @param isMaxOverwrite
	 * @param maxOverwrite
	 */
	public Map2ColorProfile(final ColorValue[] valueColors,
							final int minBrightness,
							final int minBrightnessFactor,
							final int maxBrightness,
							final int maxBrightnessFactor,
							final boolean isMinOverwrite,
							final int minOverwrite,
							final boolean isMaxOverwrite,
							final int maxOverwrite) {

		this(valueColors, minBrightness, minBrightnessFactor, maxBrightness, maxBrightnessFactor);

		this.isMinValueOverwrite = isMinOverwrite;
		this.overwriteMinValue = minOverwrite;
		this.isMaxValueOverwrite = isMaxOverwrite;
		this.overwriteMaxValue = maxOverwrite;
	}

	@Override
	public Map2ColorProfile clone() {

		Map2ColorProfile clonedObject = null;

		try {

			clonedObject = (Map2ColorProfile) super.clone();

			clonedObject.colorValues = new ColorValue[colorValues.length];

			for (int colorIndex = 0; colorIndex < colorValues.length; colorIndex++) {
				clonedObject.colorValues[colorIndex] = colorValues[colorIndex].clone();
			}

		} catch (final CloneNotSupportedException e) {
			StatusUtil.log(e);
		}

		return clonedObject;
	}

	public ColorValue[] getColorValues() {
		return colorValues;
	}

	public void setColorValues(ColorValue[] colorValues) {
		this.colorValues = colorValues;
	}

}
