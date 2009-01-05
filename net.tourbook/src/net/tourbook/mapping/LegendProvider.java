/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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

import net.tourbook.chart.ChartUtil;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;

public class LegendProvider implements ILegendProvider {

	private LegendConfig	fLegendConfig;
	private LegendColor		fLegendColor;
	private int				fColorId;

	private static List<Integer> getLegendUnits(final Rectangle legendBounds, int graphMinValue, int graphMaxValue) {

		final int legendHeight = legendBounds.height - 2 * TourMapView.LEGEND_MARGIN_TOP_BOTTOM;

		/*
		 * !!! value range does currently NOT provide negative altitudes
		 */
		final int graphRange = graphMaxValue - graphMinValue;

		final int unitCount = legendHeight / TourMapView.LEGEND_UNIT_DISTANCE;

		// get altitude range for one unit
		final int graphUnitValue = graphRange / Math.max(1, unitCount);

		// round the unit
		final float graphUnit = ChartUtil.roundDecimalValue(graphUnitValue);

		/*
		 * adjust min value
		 */
		float adjustMinValue = 0;
		if ((graphMinValue % graphUnit) != 0 && graphMinValue < 0) {
			adjustMinValue = graphUnit;
		}
		graphMinValue = (int) ((int) ((graphMinValue - adjustMinValue) / graphUnit) * graphUnit);

		/*
		 * adjust max value
		 */
		// increase the max value when it does not fit to unit borders
		float adjustMaxValue = 0;
		if ((graphMaxValue % graphUnit) != 0) {
			adjustMaxValue = graphUnit;
		}
		graphMaxValue = (int) ((int) ((graphMaxValue + adjustMaxValue) / graphUnit) * graphUnit);

		/*
		 * create a list with all units
		 */
		final ArrayList<Integer> unitList = new ArrayList<Integer>();

		int graphValue = graphMinValue;
		int unitCounter = 0;

		// loop: create unit label for all units
		while (graphValue <= graphMaxValue) {

			unitList.add(graphValue);

			// prevent endless loops 
			if (graphValue >= graphMaxValue || unitCounter > 100) {
				break;
			}

			graphValue += graphUnit;
			unitCounter++;
		}

		return unitList;
	}

	public LegendProvider(final LegendConfig legendConfig, final LegendColor legendColor, final int colorId) {
		fLegendConfig = legendConfig;
		fLegendColor = legendColor;
		fColorId = colorId;
	}

	public LegendColor getLegendColor() {
		return fLegendColor;
	}

	public LegendConfig getLegendConfig() {
		return fLegendConfig;
	}

	public int getTourColorId() {
		return fColorId;
	}

	public Color getValueColor(final int legendValue) {
		return TourPainter.getLegendColor(fLegendConfig, fLegendColor, legendValue);
	}

	public void setLegendColorColors(final LegendColor legendColor) {

		final ValueColor[] valueColors = fLegendColor.valueColors;
		final ValueColor[] newValueColors = legendColor.valueColors;

		// copy new colors into current legend colors
		for (int valueIndex = 0; valueIndex < valueColors.length; valueIndex++) {

			final ValueColor valueColor = valueColors[valueIndex];
			final ValueColor newValueColor = newValueColors[valueIndex];

			valueColor.red = newValueColor.red;
			valueColor.green = newValueColor.green;
			valueColor.blue = newValueColor.blue;
		}

		fLegendColor.minBrightness = legendColor.minBrightness;
		fLegendColor.minBrightnessFactor = legendColor.minBrightnessFactor;
		fLegendColor.maxBrightness = legendColor.maxBrightness;
		fLegendColor.maxBrightnessFactor = legendColor.maxBrightnessFactor;

		fLegendColor.isMinValueOverwrite = legendColor.isMinValueOverwrite;
		fLegendColor.overwriteMinValue = legendColor.overwriteMinValue;
		fLegendColor.isMaxValueOverwrite = legendColor.isMaxValueOverwrite;
		fLegendColor.overwriteMaxValue = legendColor.overwriteMaxValue;
	}

	/**
	 * Set legend values from a dataserie
	 * 
	 * @param legendBounds
	 * @param dataSerie
	 * @param legendProvider
	 * @param unitText
	 */
	public void setLegendColorValues(final Rectangle legendBounds, int minValue, int maxValue, final String unitText) {

		final int unitFactor = fLegendConfig.unitFactor;

		if (fLegendColor.isMinValueOverwrite && minValue < fLegendColor.overwriteMinValue * unitFactor) {
			minValue = fLegendColor.overwriteMinValue * unitFactor;
		}
		if (fLegendColor.isMaxValueOverwrite && maxValue > fLegendColor.overwriteMaxValue * unitFactor) {
			maxValue = fLegendColor.overwriteMaxValue * unitFactor;
		}

		if (maxValue < minValue) {
			maxValue = minValue + 1;
		}
		
		final List<Integer> legendUnits = getLegendUnits(legendBounds, minValue, maxValue);
		if (legendUnits.size() > 0) {

			final Integer legendMinValue = legendUnits.get(0);
			final Integer legendMaxValue = legendUnits.get(legendUnits.size() - 1);

			fLegendConfig.units = legendUnits;
			fLegendConfig.unitText = unitText;
			fLegendConfig.legendMinValue = legendMinValue;
			fLegendConfig.legendMaxValue = legendMaxValue;

			/*
			 * set color configuration, each tour has a different altitude config
			 */

			final int diffMinMax = legendMaxValue - legendMinValue;
			final int diffMinMax2 = diffMinMax / 2;
			final int diffMinMax10 = diffMinMax / 10;
			final int midValueAbsolute = legendMinValue + diffMinMax2;

			final ValueColor[] valueColors = fLegendColor.valueColors;
			valueColors[0].value = legendMinValue + diffMinMax10;
			valueColors[1].value = legendMinValue + diffMinMax2 / 2;
			valueColors[2].value = midValueAbsolute;
			valueColors[3].value = legendMaxValue - diffMinMax2 / 2;
			valueColors[4].value = legendMaxValue - diffMinMax10;
		}
	}

	/**
	 * Set legend values from a dataserie
	 * 
	 * @param legendBounds
	 * @param dataSerie
	 * @param legendProvider
	 * @param unitText
	 */
	public void setLegendColorValues(final Rectangle legendBounds, final int[] dataSerie, final String unitText) {

		/*
		 * get min/max value
		 */
		int minValue = 0;
		int maxValue = 0;
		for (int valueIndex = 0; valueIndex < dataSerie.length; valueIndex++) {
			if (valueIndex == 0) {
				minValue = dataSerie[0];
				maxValue = dataSerie[0];
			} else {
				final int dataValue = dataSerie[valueIndex];
				minValue = (minValue <= dataValue) ? minValue : dataValue;
				maxValue = (maxValue >= dataValue) ? maxValue : dataValue;
			}
		}

		final int unitFactor = fLegendConfig.unitFactor;

		if (fLegendColor.isMinValueOverwrite && minValue < fLegendColor.overwriteMinValue * unitFactor) {
			minValue = fLegendColor.overwriteMinValue * unitFactor;
		}
		if (fLegendColor.isMaxValueOverwrite && maxValue > fLegendColor.overwriteMaxValue * unitFactor) {
			maxValue = fLegendColor.overwriteMaxValue * unitFactor;
		}

		final List<Integer> legendUnits = getLegendUnits(legendBounds, minValue, maxValue);
		if (legendUnits.size() > 0) {

			final Integer legendMinValue = legendUnits.get(0);
			final Integer legendMaxValue = legendUnits.get(legendUnits.size() - 1);

			fLegendConfig.units = legendUnits;
			fLegendConfig.unitText = unitText;
			fLegendConfig.legendMinValue = legendMinValue;
			fLegendConfig.legendMaxValue = legendMaxValue;

			/*
			 * set color configuration, each tour has a different altitude config
			 */

			final int diffMinMax = legendMaxValue - legendMinValue;
			final int diffMinMax2 = diffMinMax / 2;
			final int diffMinMax10 = diffMinMax / 10;
			final int midValueAbsolute = legendMinValue + diffMinMax2;

			final ValueColor[] valueColors = fLegendColor.valueColors;
			valueColors[0].value = legendMinValue + diffMinMax10;
			valueColors[1].value = legendMinValue + diffMinMax2 / 2;
			valueColors[2].value = midValueAbsolute;
			valueColors[3].value = legendMaxValue - diffMinMax2 / 2;
			valueColors[4].value = legendMaxValue - diffMinMax10;
		}
	}
}
