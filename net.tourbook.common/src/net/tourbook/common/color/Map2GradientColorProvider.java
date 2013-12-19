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

/**
 * Provides colors to draw a tour or legend.
 */
public class Map2GradientColorProvider extends MapGradientColorProvider implements IGradientColors {

	private MapColorId				_mapColorId;

	private Map2ColorProfile		_map2ColorProfile;

	private MapUnitsConfiguration	_mapUnitsConfig	= new MapUnitsConfiguration();

	/**
	 * Contructor for a 2D color profile {@link Map2ColorProfile}.
	 * 
	 * @param mapColorId
	 *            Unique id for this color, it can be e.g. altitude, speed, ...
	 * @param map2ColorProfile
	 */
	public Map2GradientColorProvider(final MapColorId mapColorId) {

		_mapColorId = mapColorId;
		_map2ColorProfile = new Map2ColorProfile();
	}

	/**
	 * Set legend values from a dataserie
	 * 
	 * @param legendHeight
	 * @param dataSerie
	 * @param legendProvider
	 * @param unitText
	 */
	@Override
	public void configureColorProvider(	final int legendHeight,
										float minValue,
										float maxValue,
										final String unitText,
										final LegendUnitFormat unitFormat) {

		/*
		 * enforce min/max values, another option is necessary in the pref dialog to not enforce
		 * min/max values
		 */
		if (_map2ColorProfile.isMinValueOverwrite()) {
			minValue = _map2ColorProfile.getOverwriteMinValue();

			if (unitFormat == LegendUnitFormat.Pace) {
				// adjust value from minutes->seconds
				minValue *= 60;
			}
		}
		if (_map2ColorProfile.isMaxValueOverwrite()) {
			maxValue = _map2ColorProfile.getOverwriteMaxValue();

			if (unitFormat == LegendUnitFormat.Pace) {
				// adjust value from minutes->seconds
				maxValue *= 60;
			}
		}

		// ensure max is larger than min
		if (maxValue <= minValue) {
			maxValue = minValue + 1;
		}

		final List<Float> legendUnits = getLegendUnits(legendHeight, minValue, maxValue, unitFormat);

		if (legendUnits.size() > 0) {

			final Float legendMinValue = legendUnits.get(0);
			final Float legendMaxValue = legendUnits.get(legendUnits.size() - 1);

			_mapUnitsConfig.units = legendUnits;
			_mapUnitsConfig.unitText = unitText;
			_mapUnitsConfig.legendMinValue = legendMinValue;
			_mapUnitsConfig.legendMaxValue = legendMaxValue;

			/*
			 * set color configuration, each tour has a different altitude config
			 */

			final float diffMinMax = legendMaxValue - legendMinValue;
			final float diffMinMax2 = diffMinMax / 2;
			final float diffMinMax10 = diffMinMax / 10;
			final float midValueAbsolute = legendMinValue + diffMinMax2;

			final ColorValue[] valueColors = _map2ColorProfile.getColorValues();

			valueColors[0].value = legendMinValue + diffMinMax10;
			valueColors[1].value = legendMinValue + diffMinMax2 / 2;
			valueColors[2].value = midValueAbsolute;
			valueColors[3].value = legendMaxValue - diffMinMax2 / 2;
			valueColors[4].value = legendMaxValue - diffMinMax10;
		}
	}

	@Override
	public int getColorValue(final float graphValue) {

		int red = 0;
		int green = 0;
		int blue = 0;

		final ColorValue[] valueColors = _map2ColorProfile.getColorValues();
		final float minBrightnessFactor = _map2ColorProfile.getMinBrightnessFactor() / 100.0f;
		final float maxBrightnessFactor = _map2ColorProfile.getMaxBrightnessFactor() / 100.0f;

		/*
		 * find the ColorValue for the current value
		 */
		ColorValue valueColor;
		ColorValue minValueColor = null;
		ColorValue maxValueColor = null;

		for (final ColorValue valueColor2 : valueColors) {

			valueColor = valueColor2;
			if (graphValue > valueColor.value) {
				minValueColor = valueColor;
			}
			if (graphValue <= valueColor.value) {
				maxValueColor = valueColor;
			}

			if (minValueColor != null && maxValueColor != null) {
				break;
			}
		}

		if (minValueColor == null) {

			// legend value is smaller than minimum value

			valueColor = valueColors[0];
			red = valueColor.red;
			green = valueColor.green;
			blue = valueColor.blue;

			final float minValue = valueColor.value;
			final float minDiff = _mapUnitsConfig.legendMinValue - minValue;

			final float ratio = minDiff == 0 ? 1 : (graphValue - minValue) / minDiff;
			final float dimmRatio = minBrightnessFactor * ratio;

			if (_map2ColorProfile.getMinBrightness() == MapColorProfile.BRIGHTNESS_DIMMING) {

				red = red - (int) (dimmRatio * red);
				green = green - (int) (dimmRatio * green);
				blue = blue - (int) (dimmRatio * blue);

			} else if (_map2ColorProfile.getMinBrightness() == MapColorProfile.BRIGHTNESS_LIGHTNING) {

				red = red + (int) (dimmRatio * (255 - red));
				green = green + (int) (dimmRatio * (255 - green));
				blue = blue + (int) (dimmRatio * (255 - blue));
			}

		} else if (maxValueColor == null) {

			// legend value is larger than maximum value

			valueColor = valueColors[valueColors.length - 1];
			red = valueColor.red;
			green = valueColor.green;
			blue = valueColor.blue;

			final float maxValue = valueColor.value;
			final float maxDiff = _mapUnitsConfig.legendMaxValue - maxValue;

			final float ratio = maxDiff == 0 ? 1 : (graphValue - maxValue) / maxDiff;
			final float dimmRatio = maxBrightnessFactor * ratio;

			if (_map2ColorProfile.getMaxBrightness() == MapColorProfile.BRIGHTNESS_DIMMING) {

				red = red - (int) (dimmRatio * red);
				green = green - (int) (dimmRatio * green);
				blue = blue - (int) (dimmRatio * blue);

			} else if (_map2ColorProfile.getMaxBrightness() == MapColorProfile.BRIGHTNESS_LIGHTNING) {

				red = red + (int) (dimmRatio * (255 - red));
				green = green + (int) (dimmRatio * (255 - green));
				blue = blue + (int) (dimmRatio * (255 - blue));
			}

		} else {

			// legend value is in the min/max range

			final float minValue = minValueColor.value;
			final float maxValue = maxValueColor.value;

			final float minRed = minValueColor.red;
			final float minGreen = minValueColor.green;
			final float minBlue = minValueColor.blue;

			final float redDiff = maxValueColor.red - minRed;
			final float greenDiff = maxValueColor.green - minGreen;
			final float blueDiff = maxValueColor.blue - minBlue;

			final float ratioDiff = maxValue - minValue;
			final float ratio = ratioDiff == 0 ? 1 : (graphValue - minValue) / (ratioDiff);

			red = (int) (minRed + redDiff * ratio);
			green = (int) (minGreen + greenDiff * ratio);
			blue = (int) (minBlue + blueDiff * ratio);
		}

		// adjust color values to 0...255, this is optimized
		final int maxRed = (0 >= red) ? 0 : red;
		final int maxGreen = (0 >= green) ? 0 : green;
		final int maxBlue = (0 >= blue) ? 0 : blue;
		red = (255 <= maxRed) ? 255 : maxRed;
		green = (255 <= maxGreen) ? 255 : maxGreen;
		blue = (255 <= maxBlue) ? 255 : maxBlue;

		final int graphColor = ((red & 0xFF) << 0) | ((green & 0xFF) << 8) | ((blue & 0xFF) << 16);

		return graphColor;
	}

	public MapColorId getMapColorId() {
		return _mapColorId;
	}

	@Override
	public Map2ColorProfile getMapColorProfile() {
		return _map2ColorProfile;
	}

	public MapUnitsConfiguration getMapUnitsConfiguration() {
		return _mapUnitsConfig;
	}

	@Override
	public void setColorProfile(final MapColorProfile mapColorProfile) {

		if (mapColorProfile instanceof Map2ColorProfile) {

			final Map2ColorProfile map2ColorProfile = (Map2ColorProfile) mapColorProfile;

			final ColorValue[] oldValueColors = _map2ColorProfile.getColorValues();
			final ColorValue[] newValueColors = map2ColorProfile.getColorValues();

			// copy new colors into current legend colors
			for (int valueIndex = 0; valueIndex < oldValueColors.length; valueIndex++) {

				final ColorValue oldValueColor = oldValueColors[valueIndex];
				final ColorValue newValueColor = newValueColors[valueIndex];

				oldValueColor.red = newValueColor.red;
				oldValueColor.green = newValueColor.green;
				oldValueColor.blue = newValueColor.blue;
			}

			_map2ColorProfile.setMinBrightness(map2ColorProfile.getMinBrightness());
			_map2ColorProfile.setMinBrightnessFactor(map2ColorProfile.getMinBrightnessFactor());

			_map2ColorProfile.setMaxBrightness(map2ColorProfile.getMaxBrightness());
			_map2ColorProfile.setMaxBrightnessFactor(map2ColorProfile.getMaxBrightnessFactor());

			_map2ColorProfile.setMinValueOverwrite(map2ColorProfile.isMinValueOverwrite());
			_map2ColorProfile.setMaxValueOverwrite(map2ColorProfile.isMaxValueOverwrite());

			_map2ColorProfile.setOverwriteMinValue(map2ColorProfile.getOverwriteMinValue());
			_map2ColorProfile.setOverwriteMaxValue(map2ColorProfile.getOverwriteMaxValue());
		}
	}

}
