/*******************************************************************************
 * Copyright (C) 2005, 2015  Wolfgang Schramm and Contributors
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

import net.tourbook.common.UI;

import org.eclipse.swt.graphics.RGB;

public abstract class ChartDataSerie {

	public static final int			AXIS_UNIT_NUMBER						= 10;
	public static final int			AXIS_UNIT_HOUR_MINUTE					= 20;
	public static final int			AXIS_UNIT_HOUR_MINUTE_24H				= 21;
	public static final int			AXIS_UNIT_HOUR_MINUTE_SECOND			= 22;
	public static final int			AXIS_UNIT_MINUTE_SECOND					= 23;
	public static final int			AXIS_UNIT_HOUR_MINUTE_OPTIONAL_SECOND	= 24;
	public static final int			AXIS_UNIT_HISTORY						= 25;

	public static final int			X_AXIS_UNIT_MONTH						= 30;
	public static final int			X_AXIS_UNIT_DAY							= 40;
	public static final int			X_AXIS_UNIT_YEAR						= 50;
	public static final int			X_AXIS_UNIT_WEEK						= 100;
	public static final int			X_AXIS_UNIT_NUMBER_CENTER				= 101;
	public static final int			X_AXIS_UNIT_HISTORY						= 102;

	/**
	 * Default color, when default color is not set
	 */
	private static RGB				DEFAULT_DEFAULT_RGB						= new RGB(0xFF, 0xA5, 0xCB);

	/**
	 * divisor for highValues
	 */
	private int						_valueDivisor							= 1;

	/**
	 * Number of digits which are displayed.
	 */
	private int						_displayedFractionalDigits				= 0;

	/**
	 * min value which is used to draw the chart
	 */
	protected double				_visibleMinValue;

	/**
	 * max value which is used to draw the chart
	 */
	protected double				_visibleMaxValue;

	/**
	 * minimum value found in the provided values
	 */
	double							_originalMinValue;

	/**
	 * maximum value found in the provided values
	 */
	double							_originalMaxValue;

	/**
	 * Average for all positive values.
	 */
	double							_avgPositiveValue;

	/**
	 * when <code>true</code> the minimum value is forced when the dataserie is displayed
	 */
	private boolean					_isForceMinValue						= false;
	private boolean					_isForceMaxValue						= false;

	private double					_visibleMaxValueForced;
	private double					_visibleMinValueForced;

	/**
	 * Unit which is drawn on the x or y axis
	 */
	private int						_axisUnit								= AXIS_UNIT_NUMBER;

	/**
	 * Text label for the unit
	 */
	private String					_unitLabel								= UI.EMPTY_STRING;

	/**
	 * Text label for the chart data, e.g. distance, altitude, speed...
	 */
	private String					_label									= UI.EMPTY_STRING;

	private HashMap<String, Object>	_customData								= new HashMap<String, Object>();

	private RGB						_rgbBright[]							= new RGB[] { new RGB(255, 0, 0) };
	private RGB						_rgbDark[]								= new RGB[] { new RGB(0, 0, 255) };
	private RGB						_rgbLine[]								= new RGB[] { new RGB(0, 255, 0) };
	private RGB						_rgbText[]								= new RGB[] { new RGB(0, 0, 0) };

	private RGB						_defaultRGB;

	public double getAvgPositiveValue() {
		return _avgPositiveValue;
	}

	public int getAxisUnit() {
		return _axisUnit;
	}

	/**
	 * Returns the application defined property of the receiver with the specified name, or null if
	 * it was not been set.
	 */
	public Object getCustomData(final String key) {
		if (_customData.containsKey(key)) {
			return _customData.get(key);
		} else {
			return null;
		}
	}

	/**
	 * @return Returns the default color for this data serie
	 */
	public RGB getDefaultRGB() {

		/*
		 * when default color is not set, return an ugly color to see in the ui that something is
		 * wrong
		 */
		if (_defaultRGB == null) {
			return DEFAULT_DEFAULT_RGB;
		}

		return _defaultRGB;
	}

	public int getDisplayedFractionalDigits() {
		return _displayedFractionalDigits;
	}

	public String getLabel() {
		return _label;
	}

	public double getOriginalMaxValue() {
		return _originalMaxValue;
	}

	public double getOriginalMinValue() {
		return _originalMinValue;
	}

	public RGB[] getRgbBright() {
		return _rgbBright;
	}

	public RGB[] getRgbDark() {
		return _rgbDark;
	}

	public RGB[] getRgbLine() {
		return _rgbLine;
	}

	public RGB[] getRgbText() {
		return _rgbText;
	}

	/**
	 * @return Returns the unit label for the data, e.g. m km/h sec h:m
	 */
	public String getUnitLabel() {
		return _unitLabel;
	}

	public int getValueDivisor() {
		return _valueDivisor;
	}

	/**
	 * @return returns the maximum value in the data serie
	 */
	public double getVisibleMaxValue() {
		return _visibleMaxValue;
	}

	public double getVisibleMaxValueForced() {
		return _visibleMaxValueForced;
	}

	/**
	 * @return returns the minimum value in the data serie
	 */
	public double getVisibleMinValue() {
		return _visibleMinValue;
	}

	public double getVisibleMinValueForced() {
		return _visibleMinValueForced;
	}

	public boolean isForceMaxValue() {
		return _isForceMaxValue;
	}

	public boolean isForceMinValue() {
		return _isForceMinValue;
	}

	public void setAxisUnit(final int axisUnit) {
		_axisUnit = axisUnit;
	}

	/**
	 * Sets the application defined property of the receiver with the specified name to the given
	 * value.
	 */
	public void setCustomData(final String key, final Object value) {
		_customData.put(key, value);
	}

	/**
	 * This value is also used to draw the axis text for the data serie
	 * 
	 * @param color
	 */
	public void setDefaultRGB(final RGB color) {
		_defaultRGB = color;
	}

	public void setDisplayedFractionalDigits(final int _displayedFractionalDigits) {
		this._displayedFractionalDigits = _displayedFractionalDigits;
	}

	public void setLabel(final String label) {
		_label = label;
	}

	public void setRgbBright(final RGB[] rgbBright) {
		_rgbBright = rgbBright;
	}

	public void setRgbDark(final RGB[] rgbDark) {
		_rgbDark = rgbDark;
	}

	public void setRgbLine(final RGB[] rgbLine) {
		_rgbLine = rgbLine;
	}

	public void setRgbText(final RGB _rgbText[]) {
		this._rgbText = _rgbText;
	}

	/**
	 * @param unit
	 *            The measurement to set.
	 */
	public void setUnitLabel(final String unit) {
		_unitLabel = unit == null ? UI.EMPTY_STRING : unit;
	}

	public void setValueDivisor(final int valueDivisor) {
		_valueDivisor = valueDivisor;
	}

	public void setVisibleMaxValue(final double maxValue) {
		_visibleMaxValue = maxValue;
	}

	public void setVisibleMaxValueForced(final double maxValue) {

		_isForceMaxValue = true;

		// ensure max is larger than min
		_visibleMaxValue = maxValue > _visibleMinValue //
				? maxValue
				: _visibleMinValue * _valueDivisor;

		_visibleMaxValueForced = _visibleMaxValue;
	}

	public void setVisibleMinValue(final double minValue) {

// debug: check Nan
//		if (minValue != minValue) {
//			int a = 0;
//			a++;
//		}

		_visibleMinValue = minValue;
	}

	public void setVisibleMinValueForced(final double minValue) {

		_isForceMinValue = true;

		_visibleMinValue = minValue;
		_visibleMinValueForced = minValue;
	}

}
