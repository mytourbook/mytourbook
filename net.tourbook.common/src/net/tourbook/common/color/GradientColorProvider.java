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

import java.util.ArrayList;
import java.util.List;

import net.tourbook.common.util.Util;

/**
 * Contains all data to draw the legend image.
 */
public class GradientColorProvider implements IGradientColors {

	private MapColorId				_mapColorId;

	private MapColor				_mapColor;
	private MapLegendImageConfig	_mapLegendImageConfig;

	/**
	 * @param mapColorId
	 *            Unique id for this color, it can be e.g. altitude, speed, ...
	 * @param mapLegendImageConfig
	 * @param mapColor
	 */
	public GradientColorProvider(	final MapColorId mapColorId,
									final MapLegendImageConfig mapLegendImageConfig,
									final MapColor mapColor) {

		_mapColorId = mapColorId;

		_mapLegendImageConfig = mapLegendImageConfig;
		_mapColor = mapColor;
	}

	private static List<Float> getLegendUnits(	final int legendHeight,
												final float graphMinValue,
												final float graphMaxValue,
												final LegendUnitFormat unitFormat) {

		if (unitFormat == LegendUnitFormat.Pace) {
			return getLegendUnits20Pace(legendHeight, (int) graphMinValue, (int) graphMaxValue, unitFormat);
		} else {
			return getLegendUnits10Number(legendHeight, graphMinValue, graphMaxValue, unitFormat);
		}
	}

	private static List<Float> getLegendUnits10Number(	final int legendHeight,
														final float graphMinValue,
														final float graphMaxValue,
														final LegendUnitFormat unitFormat) {

		/*
		 * !!! value range does currently NOT provide negative altitudes
		 */
		final float graphRange = graphMaxValue - graphMinValue;

		final int defaultUnitCount = legendHeight / IMapColorProvider.LEGEND_UNIT_DISTANCE;

		// get range for one unit
		final float graphUnitValue = graphRange / Math.max(1, defaultUnitCount);

		// round the unit
		final double graphUnit = Util.roundDecimalValue(graphUnitValue);

		/*
		 * the scaled unit with long min/max values is used because arithmetic with floating point
		 * values fails, BigDecimal could propably solve this problem but I don't have it used yet
		 */
		final long valueScaling = Util.getValueScaling(graphUnit);
		final long scaledUnit = (long) (graphUnit * valueScaling);

//		System.out.println();
//		System.out.println(valueScaling + "\t" + graphUnit + "\t" + scaledUnit);
//		// TODO remove SYSTEM.OUT.PRINTLN

		long scaledMinValue = (long) (graphMinValue * valueScaling);
		long scaledMaxValue = (long) ((graphMaxValue * valueScaling));

		/*
		 * adjust min value, decrease min value when it does not fit to unit borders
		 */
		float adjustMinValue = 0;
		final long minRemainder = scaledMinValue % scaledUnit;
		if (minRemainder != 0 && scaledMinValue < 0) {
			adjustMinValue = scaledUnit;
		}
		scaledMinValue = (long) ((scaledMinValue - adjustMinValue) / scaledUnit) * scaledUnit;

		/*
		 * adjust max value, increase the max value when it does not fit to unit borders
		 */
		float adjustMaxValue = 0;
		final long maxRemainder = scaledMaxValue % scaledUnit;
		if (maxRemainder != 0) {
			adjustMaxValue = scaledUnit;
		}
		scaledMaxValue = ((long) ((scaledMaxValue + adjustMaxValue) / scaledUnit) * scaledUnit);

		/*
		 * check that max is larger than min
		 */
		if (scaledMinValue >= scaledMaxValue) {
			/*
			 * this case can happen when the min value is set in the pref dialog, this is more a
			 * hack than a good solution
			 */
			scaledMinValue = scaledMaxValue - (3 * scaledUnit);
		}

		final List<Float> unitList = new ArrayList<Float>();
		int loopCounter = 0;
		float scaledValue = scaledMinValue;

		// loop: create unit label for all units
		while (scaledValue <= scaledMaxValue) {

			final float descaledValue = scaledValue / valueScaling;

			unitList.add(descaledValue);

			// prevent endless loops when the unit is 0
			if (scaledValue == scaledMaxValue || loopCounter++ > 1000) {
				break;
			}

			scaledValue += scaledUnit;
		}

		return unitList;
	}

	private static List<Float> getLegendUnits20Pace(final int legendHeight,
													int graphMinValue,
													int graphMaxValue,
													final LegendUnitFormat unitFormat) {

		int graphValueRange = graphMaxValue > 0 ? graphMaxValue - graphMinValue : -(graphMinValue - graphMaxValue);

		/*
		 * calculate the number of units which will be visible by dividing the available height by
		 * the minimum size which one unit should have in pixels
		 */
		final float defaultUnitCount = legendHeight / IMapColorProvider.LEGEND_UNIT_DISTANCE;

		// unitValue is the number in data values for one unit
		final float defaultUnitValue = graphValueRange / Math.max(1, defaultUnitCount);

		// round the unit
		long graphUnit = (long) Util.roundTimeValue(defaultUnitValue, false);

		long adjustMinValue = 0;
		if ((graphMinValue % graphUnit) != 0 && graphMinValue < 0) {
			adjustMinValue = graphUnit;
		}
		graphMinValue = (int) ((int) ((float) (graphMinValue - adjustMinValue) / graphUnit) * graphUnit);

		// increase the max value when it does not fit to unit borders
		float adjustMaxValue = 0;
		if ((graphMaxValue % graphUnit) != 0) {
			adjustMaxValue = graphUnit;
		}
		graphMaxValue = (int) ((int) ((graphMaxValue + adjustMaxValue) / graphUnit) * graphUnit);

		graphValueRange = (graphMaxValue > 0 ? //
				(graphMaxValue - graphMinValue)
				: -(graphMinValue - graphMaxValue));

		// ensure the chart is drawn correctly with pseudo data
		if (graphValueRange == 0) {
			graphValueRange = 3600;
			graphMaxValue = 3600;
			graphUnit = 1800;
		}

		final List<Float> unitList = new ArrayList<Float>();
		int graphValue = graphMinValue;
		int maxUnits = 0;

		// loop: create unit label for all units
		while (graphValue <= graphMaxValue) {

			unitList.add((float) graphValue);

			// prevent endless loops when the unit is 0
			if (graphValue == graphMaxValue || maxUnits++ > 1000) {
				break;
			}

			graphValue += graphUnit;
		}

		return unitList;
	}

	@Override
	public int getColorValue(final float graphValue) {

		int red = 0;
		int green = 0;
		int blue = 0;

		final ColorValue[] valueColors = _mapColor.colorValues;
		final float minBrightnessFactor = _mapColor.minBrightnessFactor / 100.0f;
		final float maxBrightnessFactor = _mapColor.maxBrightnessFactor / 100.0f;

		/*
		 * find the valueColor for the current value
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
			final float minDiff = _mapLegendImageConfig.legendMinValue - minValue;

			final float ratio = minDiff == 0 ? 1 : (graphValue - minValue) / minDiff;
			final float dimmRatio = minBrightnessFactor * ratio;

			if (_mapColor.minBrightness == MapColor.BRIGHTNESS_DIMMING) {

				red = red - (int) (dimmRatio * red);
				green = green - (int) (dimmRatio * green);
				blue = blue - (int) (dimmRatio * blue);

			} else if (_mapColor.minBrightness == MapColor.BRIGHTNESS_LIGHTNING) {

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
			final float maxDiff = _mapLegendImageConfig.legendMaxValue - maxValue;

			final float ratio = maxDiff == 0 ? 1 : (graphValue - maxValue) / maxDiff;
			final float dimmRatio = maxBrightnessFactor * ratio;

			if (_mapColor.maxBrightness == MapColor.BRIGHTNESS_DIMMING) {

				red = red - (int) (dimmRatio * red);
				green = green - (int) (dimmRatio * green);
				blue = blue - (int) (dimmRatio * blue);

			} else if (_mapColor.maxBrightness == MapColor.BRIGHTNESS_LIGHTNING) {

				red = red + (int) (dimmRatio * (255 - red));
				green = green + (int) (dimmRatio * (255 - green));
				blue = blue + (int) (dimmRatio * (255 - blue));
			}

		} else {

			// legend value is in the min/max range

			final float maxValue = maxValueColor.value;
			final float minValue = minValueColor.value;
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

	public MapColor getMapColor() {
		return _mapColor;
	}

	public MapColorId getMapColorId() {
		return _mapColorId;
	}

	public MapLegendImageConfig getMapLegendImageConfig() {
		return _mapLegendImageConfig;
	}

	public void setMapColorColors(final MapColor mapColor) {

		final ColorValue[] valueColors = _mapColor.colorValues;
		final ColorValue[] newValueColors = mapColor.colorValues;

		// copy new colors into current legend colors
		for (int valueIndex = 0; valueIndex < valueColors.length; valueIndex++) {

			final ColorValue valueColor = valueColors[valueIndex];
			final ColorValue newValueColor = newValueColors[valueIndex];

			valueColor.red = newValueColor.red;
			valueColor.green = newValueColor.green;
			valueColor.blue = newValueColor.blue;
		}

		_mapColor.minBrightness = mapColor.minBrightness;
		_mapColor.minBrightnessFactor = mapColor.minBrightnessFactor;
		_mapColor.maxBrightness = mapColor.maxBrightness;
		_mapColor.maxBrightnessFactor = mapColor.maxBrightnessFactor;

		_mapColor.isMinValueOverwrite = mapColor.isMinValueOverwrite;
		_mapColor.overwriteMinValue = mapColor.overwriteMinValue;
		_mapColor.isMaxValueOverwrite = mapColor.isMaxValueOverwrite;
		_mapColor.overwriteMaxValue = mapColor.overwriteMaxValue;
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
	public void setMapConfigValues(	final int legendHeight,
									float minValue,
									float maxValue,
									final String unitText,
									final LegendUnitFormat unitFormat) {

		/*
		 * enforce min/max values, another option is necessary in the pref dialog to not enforce
		 * min/max values
		 */
		if (_mapColor.isMinValueOverwrite /*
										 * && minValue < _legendColor.overwriteMinValue * unitFactor
										 */) {
			minValue = _mapColor.overwriteMinValue;

			if (unitFormat == LegendUnitFormat.Pace) {
				// adjust value from minutes->seconds
				minValue *= 60;
			}
		}
		if (_mapColor.isMaxValueOverwrite /*
										 * && maxValue > _legendColor.overwriteMaxValue * unitFactor
										 */) {
			maxValue = _mapColor.overwriteMaxValue;

			if (unitFormat == LegendUnitFormat.Pace) {
				// adjust value from minutes->seconds
				maxValue *= 60;
			}
		}

		if (maxValue < minValue) {
			maxValue = minValue + 1;
		}

		final List<Float> legendUnits = getLegendUnits(legendHeight, minValue, maxValue, unitFormat);

		if (legendUnits.size() > 0) {

			final Float legendMinValue = legendUnits.get(0);
			final Float legendMaxValue = legendUnits.get(legendUnits.size() - 1);

			_mapLegendImageConfig.units = legendUnits;
			_mapLegendImageConfig.unitText = unitText;
			_mapLegendImageConfig.legendMinValue = legendMinValue;
			_mapLegendImageConfig.legendMaxValue = legendMaxValue;

			/*
			 * set color configuration, each tour has a different altitude config
			 */

			final float diffMinMax = legendMaxValue - legendMinValue;
			final float diffMinMax2 = diffMinMax / 2;
			final float diffMinMax10 = diffMinMax / 10;
			final float midValueAbsolute = legendMinValue + diffMinMax2;

			final ColorValue[] valueColors = _mapColor.colorValues;

			valueColors[0].value = legendMinValue + diffMinMax10;
			valueColors[1].value = legendMinValue + diffMinMax2 / 2;
			valueColors[2].value = midValueAbsolute;
			valueColors[3].value = legendMaxValue - diffMinMax2 / 2;
			valueColors[4].value = legendMaxValue - diffMinMax10;
		}
	}

}
