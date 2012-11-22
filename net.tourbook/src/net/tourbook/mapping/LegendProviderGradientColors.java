/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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
package net.tourbook.mapping;

import java.util.ArrayList;
import java.util.List;

import net.tourbook.chart.Util;

import org.eclipse.swt.graphics.Rectangle;

/**
 * Contains all data to draw the legend image.
 */
public class LegendProviderGradientColors implements ILegendProviderGradientColors {

	private LegendConfig	_legendConfig;
	private LegendColor		_legendColor;

	private int				_colorId;

	public LegendProviderGradientColors(final LegendConfig legendConfig,
										final LegendColor legendColor,
										final int colorId) {
		_legendConfig = legendConfig;
		_legendColor = legendColor;
		_colorId = colorId;
	}

	private static List<Float> getLegendUnits(	final Rectangle legendBounds,
												final float graphMinValue,
												final float graphMaxValue,
												final LegendUnitFormat unitFormat) {

		if (unitFormat == LegendUnitFormat.Pace) {
			return getLegendUnits20Pace(legendBounds, (int) graphMinValue, (int) graphMaxValue, unitFormat);
		} else {
			return getLegendUnits10Number(legendBounds, graphMinValue, graphMaxValue, unitFormat);
		}
	}

	private static List<Float> getLegendUnits10Number(	final Rectangle legendBounds,
														final float graphMinValue,
														final float graphMaxValue,
														final LegendUnitFormat unitFormat) {

		final int legendHeight = legendBounds.height - 2 * TourMapView.LEGEND_MARGIN_TOP_BOTTOM;

		/*
		 * !!! value range does currently NOT provide negative altitudes
		 */
		final float graphRange = graphMaxValue - graphMinValue;

		final int defaultUnitCount = legendHeight / TourMapView.LEGEND_UNIT_DISTANCE;

		// get altitude range for one unit
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

	private static List<Float> getLegendUnits20Pace(final Rectangle legendBounds,
													int graphMinValue,
													int graphMaxValue,
													final LegendUnitFormat unitFormat) {

		final int legendHeight = legendBounds.height - 2 * TourMapView.LEGEND_MARGIN_TOP_BOTTOM;

		int graphValueRange = graphMaxValue > 0 ? graphMaxValue - graphMinValue : -(graphMinValue - graphMaxValue);

		/*
		 * calculate the number of units which will be visible by dividing the available height by
		 * the minimum size which one unit should have in pixels
		 */
		final float defaultUnitCount = legendHeight / TourMapView.LEGEND_UNIT_DISTANCE;

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
		return TourPainter.getLegendColor(_legendConfig, _legendColor, legendValue);
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
	 * @param legendBounds
	 * @param dataSerie
	 * @param legendProvider
	 * @param unitText
	 */
	@Override
	public void setLegendColorValues(	final Rectangle legendBounds,
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

		final List<Float> legendUnits = getLegendUnits(legendBounds, minValue, maxValue, unitFormat);

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

//	/**
//	 * Set legend values from a dataserie
//	 *
//	 * @param legendBounds
//	 * @param dataSerie
//	 * @param legendProvider
//	 * @param unitText
//	 */
//	public void setLegendColorValues(	final Rectangle legendBounds,
//										final float[] dataSerie,
//										final String unitText,
//										final LegendUnitFormat unitFormat) {
//
//		/*
//		 * get min/max value
//		 */
//		float minValue = 0;
//		float maxValue = 0;
//		for (int valueIndex = 0; valueIndex < dataSerie.length; valueIndex++) {
//			if (valueIndex == 0) {
//				minValue = dataSerie[0];
//				maxValue = dataSerie[0];
//			} else {
//				final float dataValue = dataSerie[valueIndex];
//				minValue = (minValue <= dataValue) ? minValue : dataValue;
//				maxValue = (maxValue >= dataValue) ? maxValue : dataValue;
//			}
//		}
//
//		if (_legendColor.isMinValueOverwrite && minValue < _legendColor.overwriteMinValue) {
//			minValue = _legendColor.overwriteMinValue;
//		}
//		if (_legendColor.isMaxValueOverwrite && maxValue > _legendColor.overwriteMaxValue) {
//			maxValue = _legendColor.overwriteMaxValue;
//		}
//
//		final List<Float> legendUnits = getLegendUnits(legendBounds, minValue, maxValue, unitFormat);
//		if (legendUnits.size() > 0) {
//
//			final Float legendMinValue = legendUnits.get(0);
//			final Float legendMaxValue = legendUnits.get(legendUnits.size() - 1);
//
//			_legendConfig.units = legendUnits;
//			_legendConfig.unitText = unitText;
//			_legendConfig.legendMinValue = legendMinValue;
//			_legendConfig.legendMaxValue = legendMaxValue;
//
//			/*
//			 * set color configuration, each tour has a different altitude config
//			 */
//
//			final float diffMinMax = legendMaxValue - legendMinValue;
//			final float diffMinMax2 = diffMinMax / 2;
//			final float diffMinMax10 = diffMinMax / 10;
//			final float midValueAbsolute = legendMinValue + diffMinMax2;
//
//			final ValueColor[] valueColors = _legendColor.valueColors;
//
//			valueColors[0].value = legendMinValue + diffMinMax10;
//			valueColors[1].value = legendMinValue + diffMinMax2 / 2;
//			valueColors[2].value = midValueAbsolute;
//			valueColors[3].value = legendMaxValue - diffMinMax2 / 2;
//			valueColors[4].value = legendMaxValue - diffMinMax10;
//		}
//	}

}
