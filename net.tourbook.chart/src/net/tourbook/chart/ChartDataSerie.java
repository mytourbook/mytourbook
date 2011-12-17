/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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

	public static final int			AXIS_UNIT_NUMBER						= 10;
	public static final int			AXIS_UNIT_HOUR_MINUTE					= 20;
	public static final int			AXIS_UNIT_HOUR_MINUTE_24H				= 21;
	public static final int			AXIS_UNIT_HOUR_MINUTE_SECOND			= 22;
	public static final int			AXIS_UNIT_MINUTE_SECOND					= 23;
	public static final int			AXIS_UNIT_HOUR_MINUTE_OPTIONAL_SECOND	= 24;

	public static final int			X_AXIS_UNIT_MONTH						= 30;
	public static final int			X_AXIS_UNIT_DAY							= 40;
	public static final int			X_AXIS_UNIT_YEAR						= 50;
	public static final int			X_AXIS_UNIT_WEEK						= 100;
	public static final int			X_AXIS_UNIT_NUMBER_CENTER				= 101;

	/**
	 * Default color, when default color is not set
	 */
	private static RGB				DEFAULT_DEFAULT_RGB						= new RGB(0xFF, 0xA5, 0xCB);

	/**
	 * contains the values for the chart, highValues contains the upper value, lowValues the lower
	 * value. When lowValues is null then the low values is set to 0
	 */
	float[][]						_lowValues;

	float[][]						_highValues;

	/**
	 * divisor for highValues
	 */
	private int						_valueDivisor							= 1;

	/**
	 * max value which is used to draw the chart
	 */
	protected float					_visibleMaxValue;

	/**
	 * min value which is used to draw the chart
	 */
	protected float					_visibleMinValue;

	/**
	 * unit which is drawn on the x-axis
	 */
	private int						_axisUnit								= AXIS_UNIT_NUMBER;

	/**
	 * Text label for the unit
	 */
	private String					_unitLabel								= new String();

	/**
	 * Text label for the chart data, e.g. distance, altitude, speed...
	 */
	private String					_label									= new String();

	private HashMap<String, Object>	_customData								= new HashMap<String, Object>();

	private RGB						_rgbBright[]							= new RGB[] { new RGB(255, 0, 0) };
	private RGB						_rgbDark[]								= new RGB[] { new RGB(0, 0, 255) };
	private RGB						_rgbLine[]								= new RGB[] { new RGB(0, 255, 0) };
	private RGB						_rgbText[]								= new RGB[] { new RGB(0, 0, 0) };

	/**
	 * minimum value found in the provided values
	 */
	float							_originalMinValue;

	/**
	 * maximum value found in the provided values
	 */
	float							_originalMaxValue;

	private RGB						_defaultRGB;

	/**
	 * when <code>true</code> the minimum value is forced when the dataserie is displayed
	 */
	private boolean					_isForceMinValue						= false;
	private boolean					_isForceMaxValue						= false;

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

	/**
	 * @return returns the value array
	 */
	public float[][] getHighValues() {
		return _highValues;
	}

	public String getLabel() {
		return _label;
	}

	/**
	 * @return Returns the lowValues.
	 */
	public float[][] getLowValues() {
		return _lowValues;
	}

	public float getOriginalMaxValue() {
		return _originalMaxValue;
	}

	public float getOriginalMinValue() {
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
	public float getVisibleMaxValue() {
		return _visibleMaxValue;
	}

	/**
	 * @return returns the minimum value in the data serie
	 */
	public float getVisibleMinValue() {
		return _visibleMinValue;
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

	public void setLabel(final String label) {
		_label = label;
	}

	void setMinMaxValues(final float[][] valueSeries) {

		if (valueSeries == null || valueSeries.length == 0 || valueSeries[0] == null || valueSeries[0].length == 0) {
			_visibleMaxValue = _visibleMinValue = 0;
			_originalMaxValue = _originalMinValue = 0;
			_highValues = new float[1][2];
			_lowValues = new float[1][2];
		} else {

			_highValues = valueSeries;

			// set initial min/max value
			_visibleMaxValue = _visibleMinValue = valueSeries[0][0];

			// calculate min/max highValues
			for (final float[] valueSerie : valueSeries) {

				if (valueSerie == null) {
					continue;
				}

				for (final float value : valueSerie) {
					_visibleMaxValue = (_visibleMaxValue >= value) ? _visibleMaxValue : value;
					_visibleMinValue = (_visibleMinValue <= value) ? _visibleMinValue : value;
				}
			}

			/*
			 * force the min/max values to have not the same value this is necessary to display a
			 * visible line in the chart
			 */
			if (_visibleMinValue == _visibleMaxValue) {

				_visibleMaxValue++;

				if (_visibleMinValue > 0) {
					_visibleMinValue--;
				}
			}

			_originalMinValue = _visibleMinValue;
			_originalMaxValue = _visibleMaxValue;
		}
	}

	abstract void setMinMaxValues(float[][] lowValues, float[][] highValues);

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
		_unitLabel = unit == null ? Util.EMPTY_STRING : unit;
	}

	public void setValueDivisor(final int valueDivisor) {
		_valueDivisor = valueDivisor;
	}

	public void setVisibleMaxValue(final float maxValue) {
		_visibleMaxValue = maxValue;
	}

	public void setVisibleMaxValue(final float maxValue, final boolean forceValue) {
		_visibleMaxValue = maxValue > _visibleMinValue ? maxValue : _visibleMinValue * _valueDivisor;
		_isForceMaxValue = forceValue;
	}

	public void setVisibleMinValue(final float minValue) {
		_visibleMinValue = minValue;
	}

	public void setVisibleMinValue(final float minValue, final boolean forceValue) {
		_visibleMinValue = minValue;
		_isForceMinValue = forceValue;
	}

}
