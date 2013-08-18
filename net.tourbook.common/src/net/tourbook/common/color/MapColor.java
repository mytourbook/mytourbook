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

import java.util.Arrays;

import net.tourbook.common.Messages;

/**
 * Contains all colors to paint a tour in a map.
 */
public class MapColor {

	public static final String[]	BRIGHTNESS_LABELS		= new String[] //
															{
			Messages.legend_color_keep_color,
			Messages.legend_color_dim_color,
			Messages.legend_color_lighten_color			};

	public static final int			BRIGHTNESS_DEFAULT		= 0;
	public static final int			BRIGHTNESS_DIMMING		= 1;
	public static final int			BRIGHTNESS_LIGHTNING	= 2;

	public ColorValue[]				colorValues				= new ColorValue[] {
			new ColorValue(10, 255, 0, 0),
			new ColorValue(50, 100, 100, 0),
			new ColorValue(100, 0, 255, 0),
			new ColorValue(150, 0, 100, 100),
			new ColorValue(190, 0, 0, 255)					};

	/**
	 * min and max value is painted black when {@link #minBrightnessFactor}==100, a value below 100
	 * will dim the color
	 */
	public int						minBrightness			= BRIGHTNESS_DEFAULT;
	public int						minBrightnessFactor		= 100;

	public int						maxBrightness			= BRIGHTNESS_DEFAULT;
	public int						maxBrightnessFactor		= 100;

	public boolean					isMaxValueOverwrite		= false;
	public int						overwriteMaxValue;

	public boolean					isMinValueOverwrite		= false;
	public int						overwriteMinValue;

	public MapColor() {}

	/**
	 * @param valueColors
	 * @param minBrightness
	 * @param minBrightnessFactor
	 * @param maxBrightness
	 * @param maxBrightnessFactor
	 */
	public MapColor(final ColorValue[] valueColors,
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
	public MapColor(final ColorValue[] valueColors,
					final int minBrightness,
					final int minBrightnessFactor,
					final int maxBrightness,
					final int maxBrightnessFactor,
					final boolean isMinOverwrite,
					final int minOverwrite,
					final boolean isMaxOverwrite,
					final int maxOverwrite) {

		this.colorValues = valueColors;

		this.minBrightness = minBrightness;
		this.minBrightnessFactor = minBrightnessFactor;
		this.maxBrightness = maxBrightness;
		this.maxBrightnessFactor = maxBrightnessFactor;

		this.isMinValueOverwrite = isMinOverwrite;
		this.overwriteMinValue = minOverwrite;
		this.isMaxValueOverwrite = isMaxOverwrite;
		this.overwriteMaxValue = maxOverwrite;
	}

	/**
	 * Creates a copy for this {@link MapColor}
	 * 
	 * @return
	 */
	public MapColor getCopy() {

		final MapColor copy = new MapColor();

		copy.colorValues = new ColorValue[colorValues.length];

		for (int colorIndex = 0; colorIndex < colorValues.length; colorIndex++) {
			copy.colorValues[colorIndex] = new ColorValue(colorValues[colorIndex]);
		}

		copy.minBrightness = minBrightness;
		copy.minBrightnessFactor = minBrightnessFactor;

		copy.maxBrightness = maxBrightness;
		copy.maxBrightnessFactor = maxBrightnessFactor;

		copy.isMinValueOverwrite = isMinValueOverwrite;
		copy.overwriteMinValue = overwriteMinValue;

		copy.isMaxValueOverwrite = isMaxValueOverwrite;
		copy.overwriteMaxValue = overwriteMaxValue;

		return copy;
	}

	@Override
	public String toString() {

		final int maxLen = 10;

		return String.format(
				"\n"
						+ "MapColor\n"
						+ "   colorValues=%s\n"
						+ "   minBrightness=%s\n"
						+ "   minBrightnessFactor=%s\n"
						+ "   maxBrightness=%s\n"
						+ "   maxBrightnessFactor=%s\n"
						+ "   isMaxValueOverwrite=%s\n"
						+ "   overwriteMaxValue=%s\n"
						+ "   isMinValueOverwrite=%s\n"
						+ "   overwriteMinValue=%s\n",
				colorValues != null
						? Arrays.asList(colorValues).subList(0, Math.min(colorValues.length, maxLen))
						: null,
				minBrightness,
				minBrightnessFactor,
				maxBrightness,
				maxBrightnessFactor,
				isMaxValueOverwrite,
				overwriteMaxValue,
				isMinValueOverwrite,
				overwriteMinValue);
	}

}
