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

import java.util.Arrays;

import net.tourbook.common.Messages;

/**
 * Contains all colors for one graph to paint a tour in the 3D map.
 */
public class Map3ColorProfile {

	private static final int	BRIGHTNESS_DEFAULT		= 0;
	private static final int	BRIGHTNESS_DIMMING		= 1;
	private static final int	BRIGHTNESS_LIGHTNING	= 2;

	/**
	 * Name which is visible in the UI.
	 */
	private String				_name					= Messages.Map3_Color_DefaultProfileName;

	/**
	 * Unique id to identify a color profile.
	 */
	private int					_profileId;

	private ColorValue[]		colorValues				= new ColorValue[] {
			new ColorValue(10, 255, 0, 0),
			new ColorValue(50, 100, 100, 0),
			new ColorValue(100, 0, 255, 0),
			new ColorValue(150, 0, 100, 100),
			new ColorValue(190, 0, 0, 255)				};

	/**
	 * min and max value is painted black when {@link #minBrightnessFactor}==100, a value below 100
	 * will dim the color
	 */
	private int					minBrightness			= BRIGHTNESS_DEFAULT;
	private int					minBrightnessFactor		= 100;

	private int					maxBrightness			= BRIGHTNESS_DEFAULT;
	private int					maxBrightnessFactor		= 100;

	private boolean				isMaxValueOverwrite		= false;
	private int					overwriteMaxValue;

	private boolean				isMinValueOverwrite		= false;
	private int					overwriteMinValue;

	public Map3ColorProfile() {}

	/**
	 * @param valueColors
	 * @param minBrightness
	 * @param minBrightnessFactor
	 * @param maxBrightness
	 * @param maxBrightnessFactor
	 */
	public Map3ColorProfile(final ColorValue[] valueColors,
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
	public Map3ColorProfile(final ColorValue[] valueColors,
	//
							final int minBrightness,
							final int minBrightnessFactor,
							final int maxBrightness,
							final int maxBrightnessFactor,
							//
							final boolean isMinOverwrite,
							final int minOverwrite,
							final boolean isMaxOverwrite,
							final int maxOverwrite
	//
	) {

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
	 * Creates a copy for this {@link Map3ColorProfile}
	 * 
	 * @return
	 */
	public Map3ColorProfile getCopy() {

		final Map3ColorProfile copy = new Map3ColorProfile();

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

	public String getName() {
		return _name;
	}

	public void setName(final String name) {
		_name = name;
	}

	@Override
	public String toString() {

		final int maxLen = 10;

		return String.format("\n" //$NON-NLS-1$
				+ "MapColor\n" //$NON-NLS-1$
				+ "   colorValues=%s\n" //$NON-NLS-1$
				+ "   minBrightness=%s\n" //$NON-NLS-1$
				+ "   minBrightnessFactor=%s\n" //$NON-NLS-1$
				+ "   maxBrightness=%s\n" //$NON-NLS-1$
				+ "   maxBrightnessFactor=%s\n" //$NON-NLS-1$
				+ "   isMaxValueOverwrite=%s\n" //$NON-NLS-1$
				+ "   overwriteMaxValue=%s\n" //$NON-NLS-1$
				+ "   isMinValueOverwrite=%s\n" //$NON-NLS-1$
				+ "   overwriteMinValue=%s\n", //$NON-NLS-1$
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
