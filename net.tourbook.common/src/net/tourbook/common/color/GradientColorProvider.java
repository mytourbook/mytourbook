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
public class GradientColorProvider implements ILegendProviderGradientColors {

	protected LegendConfig	_legendConfig;
	protected LegendColor	_legendColor;

	private int				_colorId;

	/**
	 * @param colorId
	 *            Unique id for this color, it can be e.g. altitude, speed, ...
	 * @param legendConfig
	 * @param legendColor
	 */
	public GradientColorProvider(final int colorId, final LegendConfig legendConfig, final LegendColor legendColor) {

		_colorId = colorId;

		_legendConfig = legendConfig;
		_legendColor = legendColor;
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

		final int defaultUnitCount = legendHeight / ILegendProvider.LEGEND_UNIT_DISTANCE;

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
		final float defaultUnitCount = legendHeight / ILegendProvider.LEGEND_UNIT_DISTANCE;

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
	public int getColorValue(final float legendValue) {
//		return getLegendColor(_legendConfig, _legendColor, legendValue);
//	}
//
//	/**
//	 * @param legendConfig
//	 * @param legendColor
//	 * @param legendValue
//	 * @param device
//	 * @return Returns a {@link Color} which corresponst to the legend value
//	 */
//	private int getLegendColor(final LegendConfig legendConfig, final LegendColor legendColor, final float legendValue) {

		int red = 0;
		int green = 0;
		int blue = 0;

		final ValueColor[] valueColors = _legendColor.valueColors;
		final float minBrightnessFactor = _legendColor.minBrightnessFactor / 100.0f;
		final float maxBrightnessFactor = _legendColor.maxBrightnessFactor / 100.0f;

		/*
		 * find the valueColor for the current value
		 */
		ValueColor valueColor;
		ValueColor minValueColor = null;
		ValueColor maxValueColor = null;

		for (final ValueColor valueColor2 : valueColors) {

			valueColor = valueColor2;
			if (legendValue > valueColor.value) {
				minValueColor = valueColor;
			}
			if (legendValue <= valueColor.value) {
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
			final float minDiff = _legendConfig.legendMinValue - minValue;

			final float ratio = minDiff == 0 ? 1 : (legendValue - minValue) / minDiff;
			final float dimmRatio = minBrightnessFactor * ratio;

			if (_legendColor.minBrightness == LegendColor.BRIGHTNESS_DIMMING) {

				red = red - (int) (dimmRatio * red);
				green = green - (int) (dimmRatio * green);
				blue = blue - (int) (dimmRatio * blue);

			} else if (_legendColor.minBrightness == LegendColor.BRIGHTNESS_LIGHTNING) {

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
			final float maxDiff = _legendConfig.legendMaxValue - maxValue;

			final float ratio = maxDiff == 0 ? 1 : (legendValue - maxValue) / maxDiff;
			final float dimmRatio = maxBrightnessFactor * ratio;

			if (_legendColor.maxBrightness == LegendColor.BRIGHTNESS_DIMMING) {

				red = red - (int) (dimmRatio * red);
				green = green - (int) (dimmRatio * green);
				blue = blue - (int) (dimmRatio * blue);

			} else if (_legendColor.maxBrightness == LegendColor.BRIGHTNESS_LIGHTNING) {

				red = red + (int) (dimmRatio * (255 - red));
				green = green + (int) (dimmRatio * (255 - green));
				blue = blue + (int) (dimmRatio * (255 - blue));
			}

		} else {

			// legend value is in the min/max range

			final float maxValue = maxValueColor.value;
			final float minValue = minValueColor.value;
			final int minRed = minValueColor.red;
			final int minGreen = minValueColor.green;
			final int minBlue = minValueColor.blue;

			final int redDiff = maxValueColor.red - minRed;
			final int greenDiff = maxValueColor.green - minGreen;
			final int blueDiff = maxValueColor.blue - minBlue;

			final float ratioDiff = maxValue - minValue;
			final float ratio = ratioDiff == 0 ? 1 : (legendValue - minValue) / (ratioDiff);

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

		final int colorValue = ((red & 0xFF) << 0) | ((green & 0xFF) << 8) | ((blue & 0xFF) << 16);

		return colorValue;
	}

	public LegendColor getLegendColor() {
		return _legendColor;
	}

	public LegendConfig getLegendConfig() {
		return _legendConfig;
	}

	public int getTourColorId() {
		return _colorId;
	}

	public void setLegendColorColors(final LegendColor legendColor) {

		final ValueColor[] valueColors = _legendColor.valueColors;
		final ValueColor[] newValueColors = legendColor.valueColors;

		// copy new colors into current legend colors
		for (int valueIndex = 0; valueIndex < valueColors.length; valueIndex++) {

			final ValueColor valueColor = valueColors[valueIndex];
			final ValueColor newValueColor = newValueColors[valueIndex];

			valueColor.red = newValueColor.red;
			valueColor.green = newValueColor.green;
			valueColor.blue = newValueColor.blue;
		}

		_legendColor.minBrightness = legendColor.minBrightness;
		_legendColor.minBrightnessFactor = legendColor.minBrightnessFactor;
		_legendColor.maxBrightness = legendColor.maxBrightness;
		_legendColor.maxBrightnessFactor = legendColor.maxBrightnessFactor;

		_legendColor.isMinValueOverwrite = legendColor.isMinValueOverwrite;
		_legendColor.overwriteMinValue = legendColor.overwriteMinValue;
		_legendColor.isMaxValueOverwrite = legendColor.isMaxValueOverwrite;
		_legendColor.overwriteMaxValue = legendColor.overwriteMaxValue;
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
	public void setLegendColorValues(	final int legendHeight,
										float minValue,
										float maxValue,
										final String unitText,
										final LegendUnitFormat unitFormat) {

		/*
		 * enforce min/max values, another option is necessary in the pref dialog to not enforce
		 * min/max values
		 */
		if (_legendColor.isMinValueOverwrite /*
											 * && minValue < _legendColor.overwriteMinValue *
											 * unitFactor
											 */) {
			minValue = _legendColor.overwriteMinValue;

			if (unitFormat == LegendUnitFormat.Pace) {
				// adjust value from minutes->seconds
				minValue *= 60;
			}
		}
		if (_legendColor.isMaxValueOverwrite /*
											 * && maxValue > _legendColor.overwriteMaxValue *
											 * unitFactor
											 */) {
			maxValue = _legendColor.overwriteMaxValue;

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

			_legendConfig.units = legendUnits;
			_legendConfig.unitText = unitText;
			_legendConfig.legendMinValue = legendMinValue;
			_legendConfig.legendMaxValue = legendMaxValue;

			/*
			 * set color configuration, each tour has a different altitude config
			 */

			final float diffMinMax = legendMaxValue - legendMinValue;
			final float diffMinMax2 = diffMinMax / 2;
			final float diffMinMax10 = diffMinMax / 10;
			final float midValueAbsolute = legendMinValue + diffMinMax2;

			final ValueColor[] valueColors = _legendColor.valueColors;

			valueColors[0].value = legendMinValue + diffMinMax10;
			valueColors[1].value = legendMinValue + diffMinMax2 / 2;
			valueColors[2].value = midValueAbsolute;
			valueColors[3].value = legendMaxValue - diffMinMax2 / 2;
			valueColors[4].value = legendMaxValue - diffMinMax10;
		}
	}

}
