/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm
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
package net.tourbook.chart;

import java.util.HashMap;

import org.eclipse.swt.graphics.RGB;

public abstract class ChartDataSerie {

	public static final int			AXIS_UNIT_NUMBER				= 10;
	public static final int			AXIS_UNIT_HOUR_MINUTE			= 20;
	public static final int			AXIS_UNIT_HOUR_MINUTE_24H		= 21;
	public static final int			AXIS_UNIT_HOUR_MINUTE_SECOND	= 22;
	public static final int			AXIS_UNIT_MONTH					= 30;
	public static final int			AXIS_UNIT_YEAR					= 40;

	/**
	 * contains the values for the chart, highValues contains the upper value,
	 * lowValues the lower value. When lowValues is null then the low values is
	 * set to 0
	 */
	int[][]							fLowValues;

	int[][]							fHighValues;

	/**
	 * divisor for highValues
	 */
	private int						valueDivisor					= 1;

	/**
	 * unit which is drawn on the x-axis
	 */
	private int						axisUnit						= AXIS_UNIT_NUMBER;

	/**
	 * Text label for the unit
	 */
	private String					unitLabel						= new String();

	/**
	 * Text label for the chart data, e.g. distance, altitude, speed...
	 */
	private String					label							= new String();

	private HashMap<String, Object>	fCustomData						= new HashMap<String, Object>();

	/**
	 * max value which is used to draw the chart
	 */
	protected int					fMaxValue;

	/**
	 * min value which is used to draw the chart
	 */
	protected int					fMinValue;

	private RGB						fRgbBright[]					= new RGB[] { new RGB(
																			255,
																			0,
																			0) };
	private RGB						fRgbDark[]						= new RGB[] { new RGB(
																			0,
																			0,
																			255) };
	private RGB						fRgbLine[]						= new RGB[] { new RGB(
																			0,
																			255,
																			0) };
	/**
	 * minimum value found in the provided values
	 */
	int								fSavedMinValue;
	private int						fOriginalMinValue;

	/**
	 * maximum value found in the provided values
	 */
	int								fSavedMaxValue;

	public int getAxisUnit() {
		return axisUnit;
	}

	/**
	 * Returns the application defined property of the receiver with the
	 * specified name, or null if it was not been set.
	 */
	public Object getCustomData(String key) {
		if (fCustomData.containsKey(key)) {
			return fCustomData.get(key);
		} else {
			return null;
		}
	}

	/**
	 * @return returns the value array
	 */
	public int[][] getHighValues() {
		return fHighValues;
	}

	public String getLabel() {
		return label;
	}

	/**
	 * @return Returns the lowValues.
	 */
	public int[][] getLowValues() {
		return fLowValues;
	}

	/**
	 * @return returns the maximum value in the data serie
	 */
	public int getMaxValue() {
		return fMaxValue;
	}

	/**
	 * @return returns the minimum value in the data serie
	 */
	public int getMinValue() {
		return fMinValue;
	}

	public int getOriginalMinValue() {
		return fOriginalMinValue;
	}

	public RGB[] getRgbBright() {
		return fRgbBright;
	}

	public RGB[] getRgbDark() {
		return fRgbDark;
	}

	public RGB[] getRgbLine() {
		return fRgbLine;
	}
	/**
	 * @return Returns the unit label for the data, e.g. m km/h sec h:m
	 */
	public String getUnitLabel() {
		return unitLabel;
	}
	public int getValueDivisor() {
		return valueDivisor;
	}

	public void setAxisUnit(int axisUnit) {
		this.axisUnit = axisUnit;
	}

	/**
	 * Sets the application defined property of the receiver with the specified
	 * name to the given value.
	 */
	public void setCustomData(String key, Object value) {
		fCustomData.put(key, value);
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public void setMaxValue(int maxValue) {
		fMaxValue = maxValue;
	}
	
	void setMinMax(int valueSeries[][]) {

		if (valueSeries == null || valueSeries.length == 0 || valueSeries[0].length == 0) {
			fMaxValue = fMinValue = 0;
			fSavedMaxValue = fOriginalMinValue = fSavedMinValue = 0;
			fHighValues = new int[1][2];
			fLowValues = new int[1][2];
		} else {

			fHighValues = valueSeries;

			// set initial min/max value
			fMaxValue = fMinValue = valueSeries[0][0];

			// calculate min/max highValues
			for (int[] valueSerie : valueSeries) {
				for (int value : valueSerie) {
					fMaxValue = Math.max(fMaxValue, value);
					fMinValue = Math.min(fMinValue, value);
				}
			}

			fOriginalMinValue = fSavedMinValue = fMinValue;
			fSavedMaxValue = fMaxValue;
		}
	}
	
	abstract void setMinMax(int[][] lowValues, int[][] highValues);

	public void setMinValue(int minValue) {
		fMinValue = minValue;
	}

	public void setRgbBright(RGB[] rgbBright) {
		fRgbBright = rgbBright;
	}

	public void setRgbDark(RGB[] rgbDark) {
		fRgbDark = rgbDark;
	}

	public void setRgbLine(RGB[] rgbLine) {
		fRgbLine = rgbLine;
	}

	public void setSavedMinValue(int savedMinValue) {
		fMinValue = savedMinValue;
		fSavedMinValue = savedMinValue;
	}

	/**
	 * @param unit
	 *        The measurement to set.
	 */
	public void setUnitLabel(String unit) {
		this.unitLabel = unit == null ? "" : unit; //$NON-NLS-1$
	}

	public void setValueDivisor(int valueDivisor) {
		this.valueDivisor = valueDivisor;
	}

}
