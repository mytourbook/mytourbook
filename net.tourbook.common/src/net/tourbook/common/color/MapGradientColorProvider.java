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

import java.util.ArrayList;
import java.util.List;

import net.tourbook.common.util.Util;

public class MapGradientColorProvider {

	public MapGradientColorProvider() {
		super();
	}

	/**
	 * Converts graph min/max values into legend values which adjusted min/max values.
	 * 
	 * @param legendSize
	 * @param graphMinValue
	 * @param graphMaxValue
	 * @param unitFormat
	 * @return
	 */
	static List<Float> getLegendUnits(	final int legendSize,
										final float graphMinValue,
										final float graphMaxValue,
										final LegendUnitFormat unitFormat) {

		if (unitFormat == LegendUnitFormat.Pace) {

			return getLegendUnits_20_Pace(legendSize, (int) graphMinValue, (int) graphMaxValue, unitFormat);

		} else {

			return getLegendUnits_10_Number(legendSize, graphMinValue, graphMaxValue, unitFormat);
		}
	}

	private static List<Float> getLegendUnits_10_Number(final int legendSize,
														final float graphMinValue,
														final float graphMaxValue,
														final LegendUnitFormat unitFormat) {

		/**
		 * ????????????????????????????<br>
		 * !!! value range does currently NOT provide negative altitudes
		 * ????????????????????????????
		 */
		final float graphRange = graphMaxValue - graphMinValue;

		final int defaultUnitCount = legendSize / IMapColorProvider.LEGEND_UNIT_DISTANCE;

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

	private static List<Float> getLegendUnits_20_Pace(	final int legendHeight,
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

}
