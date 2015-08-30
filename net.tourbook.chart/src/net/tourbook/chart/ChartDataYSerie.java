/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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

import java.util.ArrayList;

/**
 * Contains the highValues and display attributes for one data serie
 */
public class ChartDataYSerie extends ChartDataSerie {

	static final double				FLOAT_ZERO							= 0.0001;

	/**
	 * contains the values for the chart, highValues contains the upper value, lowValues the lower
	 * value. When lowValues is null then the low values is set to 0
	 */
	float[][]						_lowValuesFloat;

	float[][]						_highValuesFloat;

	private double[][]				_highValuesDouble;

	/**
	 * the bars has only a low and high value
	 */
	public static final int			BAR_LAYOUT_SINGLE_SERIE				= 1;

	/**
	 * the bars are displayed one of the other
	 */
	public static final int			BAR_LAYOUT_STACKED					= 2;

	/**
	 * the bars are displayed beside each other
	 */
	public static final int			BAR_LAYOUT_BESIDE					= 3;

	public static final String		YDATA_INFO							= "yDataInfo";					//$NON-NLS-1$

	public static final int			FILL_METHOD_NO						= 0;
	public static final int			FILL_METHOD_FILL_BOTTOM				= 1;
	public static final int			FILL_METHOD_FILL_ZERO				= 2;
	public static final int			FILL_METHOD_FILL_BOTTOM_NO_BORDER	= 3;
	public static final int			FILL_METHOD_CUSTOM					= 100;
	public static final int			BAR_DRAW_METHOD_BOTTOM				= 200;

	/**
	 * Slider label format: n.1
	 */
	public static final int			SLIDER_LABEL_FORMAT_DEFAULT			= 0;
	/**
	 * Slider label format: mm:ss
	 */
	public static final int			SLIDER_LABEL_FORMAT_MM_SS			= 1;

	private int						_sliderLabelFormat					= SLIDER_LABEL_FORMAT_DEFAULT;
	private int						_chartLayout						= BAR_LAYOUT_SINGLE_SERIE;
	private String					_yTitle;

	/**
	 * contains the color index for each value
	 */
	private int[][]					_colorIndex;

	private int						_graphFillMethod					= FILL_METHOD_FILL_BOTTOM;

	private boolean					_isShowYSlider						= false;

	/**
	 * This value is set when a y-slider is dragged
	 */
	float							adjustedYValue						= Float.MIN_VALUE;

	/**
	 * <p>
	 * true: the direction is from bottom to top by increasing number <br>
	 * false: the direction is from top to bottom by increasing number
	 */
	private boolean					_yAxisDirection						= true;

	/**
	 * Contains all layers which are drawn on top of the graph before the slider
	 */
	private ArrayList<IChartLayer>	_customFgLayers						= new ArrayList<IChartLayer>();

	/**
	 * Contains all layers which are drawn in the background of the graph
	 */
	private ArrayList<IChartLayer>	_customBgLayers						= new ArrayList<IChartLayer>();

	/**
	 * Contains a painter which fills the graph
	 */
	private IFillPainter			_customFillPainter;

	private ChartYSlider			_ySlider1;

	private ChartYSlider			_ySlider2;

	private final ChartType			_chartType;

	private boolean[]				_lineGaps;

	private ISliderLabelProvider	_sliderLabelProvider;

	/**
	 * When <code>true</code> then 0 is ignored when min/max values of the data serie are computed.
	 */
	private boolean					_isIgnoreMinMaxZero;

	/**
	 * When this value is > 0 a line chart will not draw a line to the next value point when the
	 * difference in the x-data values is greater than this value.
	 * <p>
	 * Lines are not drawn to the next value with this feature because these lines (and filling)
	 * looks ugly (a triangle is painted) when a tour is paused.
	 */
//	private int						_disabledLineToNext;

	/**
	 * @param chartType
	 * @param valueSerie
	 * @param isIgnoreZero
	 *            When <code>true</code> then 0 values will be ignored when computing min/max
	 *            values.
	 */
	public ChartDataYSerie(final ChartType chartType, final float[] valueSerie, final boolean isIgnoreZero) {

		_chartType = chartType;
		_isIgnoreMinMaxZero = isIgnoreZero;

		setMinMaxValues(new float[][] { valueSerie });
	}

	public ChartDataYSerie(final ChartType chartType, final float[] lowValueSerie, final float[] highValueSerie) {

		_chartType = chartType;

		setMinMaxValues(new float[][] { lowValueSerie }, new float[][] { highValueSerie });
	}

	public ChartDataYSerie(final ChartType chartType, final float[][] valueSeries) {

		_chartType = chartType;

		setMinMaxValues(valueSeries);
	}

	public ChartDataYSerie(final ChartType chartType, final float[][] valueSerie, final boolean isIgnoreZero) {

		_chartType = chartType;
		_isIgnoreMinMaxZero = isIgnoreZero;

		setMinMaxValues(valueSerie);
	}

	public ChartDataYSerie(final ChartType chartType, final float[][] lowValueSeries, final float[][] highValueSeries) {

		_chartType = chartType;

		setMinMaxValues(lowValueSeries, highValueSeries);
	}

	public ChartDataYSerie(	final ChartType chartType,
							final int chartLayout,
							final float[][] lowValueSeries,
							final float[][] highValueSeries) {

		_chartType = chartType;
		_chartLayout = chartLayout;

		setMinMaxValues(lowValueSeries, highValueSeries);
	}

	/**
	 * @return Returns the chartLayout.
	 */
	protected int getChartLayout() {
		return _chartLayout;
	}

	public ChartType getChartType() {
		return _chartType;
	}

	/**
	 * @return Returns the valueColors.
	 */
	public int[][] getColorsIndex() {
		if (_colorIndex == null || _colorIndex.length == 0 || _colorIndex[0] == null || _colorIndex[0].length == 0) {
			setAllValueColors(0);
		}
		return _colorIndex;
	}

	public ArrayList<IChartLayer> getCustomBackgroundLayers() {
		return _customBgLayers;
	}

	public IFillPainter getCustomFillPainter() {
		return _customFillPainter;
	}

	public ArrayList<IChartLayer> getCustomForegroundLayers() {
		return _customFgLayers;
	}

	/**
	 * @param valueSeries
	 * @return Returns first value of a dataseries.
	 */
	private float getFirstMinMax(final float[][] valueSeries) {

		if (_isIgnoreMinMaxZero) {

			// get first value which is not 0

			for (final float[] outerValues : valueSeries) {

				if (outerValues == null) {
					continue;
				}

				for (final float value : outerValues) {

					if (value != value) {
						// ignore NaN
						continue;
					}

					if (value == Float.POSITIVE_INFINITY) {
						// ignore infinity
						continue;
					}

					if (value < -FLOAT_ZERO || value > FLOAT_ZERO) {
						return value;
					}
				}
			}

		} else {

			// get first not NaN value

			for (final float[] outerValues : valueSeries) {

				if (outerValues == null) {
					continue;
				}

				for (final float value : outerValues) {

					if (value != value) {
						// ignore NaN
						continue;
					}

					if (value == Float.POSITIVE_INFINITY) {
						// ignore infinity
						continue;
					}

					return value;
				}
			}
		}

		return 0;
	}

	/**
	 * @return returns true if the graph is filled
	 */
	public int getGraphFillMethod() {
		return _graphFillMethod;
	}

	public double[][] getHighValuesDouble() {

		if (_highValuesDouble != null) {
			return _highValuesDouble;
		}

		if (_highValuesFloat == null || _highValuesFloat.length == 0) {
			return null;
		}

		/*
		 * convert float[][] -> double[][]
		 */
		_highValuesDouble = new double[_highValuesFloat.length][];

		for (int index1 = 0; index1 < _highValuesFloat.length; index1++) {

			final float[] values1 = _highValuesFloat[index1];

			if (values1 == null || values1.length == 0) {
				continue;
			}

			final double[] values2 = _highValuesDouble[index1] = new double[values1.length];

			for (int index2 = 0; index2 < values2.length; index2++) {
				values2[index2] = values1[index2];
			}
		}

		return _highValuesDouble;
	}

	/**
	 * @return returns the value array
	 */
	public float[][] getHighValuesFloat() {
		return _highValuesFloat;
	}

	public boolean[] getLineGaps() {
		return _lineGaps;
	}

	/**
	 * @return Returns the lowValues.
	 */
	public float[][] getLowValuesFloat() {
		return _lowValuesFloat;
	}

	/**
	 * @return Returns the format how the slider label will be formatted, which can be <br>
	 *         {@link #SLIDER_LABEL_FORMAT_DEFAULT}<br>
	 *         {@link #SLIDER_LABEL_FORMAT_MM_SS}
	 */
	public int getSliderLabelFormat() {
		return _sliderLabelFormat;
	}

	public ISliderLabelProvider getSliderLabelProvider() {
		return _sliderLabelProvider;
	}

	public String getXTitle() {
		return null;
	}

	/**
	 * @return Returns the ySliderTop.
	 */
	public ChartYSlider getYSlider1() {
		return _ySlider1;
	}

	/**
	 * @return Returns the ySliderBottom.
	 */
	public ChartYSlider getYSlider2() {
		return _ySlider2;
	}

	/**
	 * @return Returns the title.
	 */
	public String getYTitle() {
		return _yTitle;
	}

	/**
	 * @return Returns <code>true</code> when 0 is ignored when min/max values of the data serie are
	 *         computed.
	 */
	public boolean isIgnoreMinMaxZero() {
		return _isIgnoreMinMaxZero;
	}

	/**
	 * @return Returns the showYSlider.
	 */
	public boolean isShowYSlider() {
		return _isShowYSlider;
	}

	/**
	 * <p>
	 * true: the direction is from bottom to top by increasing number <br>
	 * false: the direction is from top to bottom by increasing number
	 */
	public boolean isYAxisDirection() {
		return _yAxisDirection;
	}

	/**
	 * set the color index of all values
	 * 
	 * @param colorIndexValue
	 */
	public void setAllValueColors(final int colorIndexValue) {

		if (_highValuesFloat == null
				|| _highValuesFloat.length == 0
				|| _highValuesFloat[0] == null
				|| _highValuesFloat[0].length == 0) {
			return;
		}

		_colorIndex = new int[1][_highValuesFloat[0].length];

		final int[] colorIndex0 = _colorIndex[0];

		for (int colorIndex = 0; colorIndex < colorIndex0.length; colorIndex++) {
			colorIndex0[colorIndex] = colorIndexValue;
		}
	}

	/**
	 * @param colorIndex
	 *            set's the color index for each value
	 */
	public void setColorIndex(final int[][] colorIndex) {
		_colorIndex = colorIndex;
	}

	public void setCustomBackgroundLayers(final ArrayList<IChartLayer> customBackgroundLayers) {
		_customBgLayers = customBackgroundLayers;
	}

	public void setCustomFillPainter(final IFillPainter fillPainter) {
		_customFillPainter = fillPainter;
	}

	public void setCustomForegroundLayers(final ArrayList<IChartLayer> customLayers) {
		_customFgLayers = customLayers;
	}

	/**
	 * @param fillMethod
	 *            when set to <tt>true</tt> graph is filled, default is <tt>false</tt>
	 */
	public void setGraphFillMethod(final int fillMethod) {
		_graphFillMethod = fillMethod;
	}

	public void setLineGaps(final boolean[] lineGaps) {
		_lineGaps = lineGaps;
	}

	/**
	 * @param valueSeries
	 * @param _isIgnoreMinMaxZero
	 *            When <code>true</code> then zero values will be ignored.
	 *            <p>
	 *            <b>Is not yet implemented for all cases !</b>
	 */
	private void setMinMaxValues(final float[][] valueSeries) {

		if (valueSeries == null || valueSeries.length == 0 || valueSeries[0] == null || valueSeries[0].length == 0) {

			_highValuesFloat = new float[0][0];

			_visibleMaxValue = _visibleMinValue = 0;
			_originalMaxValue = _originalMinValue = 0;

		} else {

			_highValuesFloat = valueSeries;

			if (_chartType == ChartType.LINE
					|| _chartType == ChartType.LINE_WITH_BARS
					|| _chartType == ChartType.HORIZONTAL_BAR
					|| _chartType == ChartType.XY_SCATTER
					|| _chartType == ChartType.HISTORY) {

				setMinMaxValuesLine(valueSeries);

			} else {

				/*
				 * Set initial min/max value
				 */
				final float firstValue = getFirstMinMax(valueSeries);
				_visibleMaxValue = _visibleMinValue = firstValue;

				if (_chartType == ChartType.BAR) {

					switch (_chartLayout) {
					case ChartDataYSerie.BAR_LAYOUT_SINGLE_SERIE:
					case ChartDataYSerie.BAR_LAYOUT_BESIDE:

						// get the min/max highValues for all data
						for (final float[] valuesOuter : valueSeries) {
							for (final float value : valuesOuter) {

								if (_isIgnoreMinMaxZero && (value > -FLOAT_ZERO && value < FLOAT_ZERO)) {
									continue;
								}

								_visibleMinValue = (_visibleMinValue <= value) ? _visibleMinValue : value;
								_visibleMaxValue = (_visibleMaxValue >= value) ? _visibleMaxValue : value;
							}
						}
						break;

					case ChartDataYSerie.BAR_LAYOUT_STACKED:

						final float serieMax[] = new float[valueSeries[0].length];

						// get the max value for the data which are stacked on each
						// other
						for (final float[] valuesOuter : valueSeries) {
							for (int valueIndex = 0; valueIndex < valuesOuter.length; valueIndex++) {

								final float outerValue = valuesOuter[valueIndex];
								final float outerValueWithMax = serieMax[valueIndex] + outerValue;

								serieMax[valueIndex] = (float) ((_visibleMaxValue >= outerValueWithMax)
										? _visibleMaxValue
										: outerValueWithMax);

								_visibleMinValue = (_visibleMinValue <= outerValue) ? _visibleMinValue : outerValue;
							}
						}

						// get max for all series
						_visibleMaxValue = 0;
						for (final float serieValue : serieMax) {
							_visibleMaxValue = (_visibleMaxValue >= serieValue) ? _visibleMaxValue : serieValue;
						}

						break;
					}
				}

				_originalMinValue = _visibleMinValue;
				_originalMaxValue = _visibleMaxValue;
			}
		}
	}

	void setMinMaxValues(final float[][] lowValues, final float[][] highValues) {

		if (lowValues == null || lowValues.length == 0 || lowValues[0] == null || lowValues[0].length == 0

		|| highValues == null || highValues.length == 0 || highValues[0] == null || highValues[0].length == 0) {

			_visibleMaxValue = _visibleMinValue = 0;
			_originalMaxValue = _originalMinValue = 0;

			_lowValuesFloat = new float[1][2];
			_highValuesFloat = new float[1][2];

		} else {

			_lowValuesFloat = lowValues;
			_highValuesFloat = highValues;
			_colorIndex = new int[_highValuesFloat.length][_highValuesFloat[0].length];

			// set initial min/max value
			_visibleMinValue = lowValues[0][0];
			_visibleMaxValue = highValues[0][0];

			if (_chartType == ChartType.LINE
					|| (_chartType == ChartType.BAR && _chartLayout == ChartDataYSerie.BAR_LAYOUT_SINGLE_SERIE)
					|| (_chartType == ChartType.BAR && _chartLayout == ChartDataYSerie.BAR_LAYOUT_BESIDE)) {

				// get the min/max values for all data
				for (final float[] valueSerie : highValues) {
					for (final float value : valueSerie) {
						_visibleMaxValue = (_visibleMaxValue >= value) ? _visibleMaxValue : value;
					}
				}

				for (final float[] valueSerie : lowValues) {
					for (final float value : valueSerie) {
						_visibleMinValue = (_visibleMinValue <= value) ? _visibleMinValue : value;
					}
				}

			} else if (_chartType == ChartType.BAR && _chartLayout == ChartDataYSerie.BAR_LAYOUT_STACKED) {

				/*
				 * calculate the max value
				 */

				// summarize the data
				final float[] summarizedMaxValues = new float[highValues[0].length];
				for (final float[] valueSerie : highValues) {
					for (int valueIndex = 0; valueIndex < valueSerie.length; valueIndex++) {
						summarizedMaxValues[valueIndex] += valueSerie[valueIndex];
					}
				}

				// get max value for the summarized values
				for (final float value : summarizedMaxValues) {
					_visibleMaxValue = (_visibleMaxValue >= value) ? _visibleMaxValue : value;
				}

				/*
				 * calculate the min value
				 */
				for (final float[] serieData : lowValues) {
					for (final float value : serieData) {
						_visibleMinValue = (_visibleMinValue <= value) ? _visibleMinValue : value;
					}
				}
			}

			_originalMinValue = _visibleMinValue;
			_originalMaxValue = _visibleMaxValue;
		}
	}

	private void setMinMaxValuesLine(final float[][] valueSeries) {

		if (valueSeries == null || valueSeries.length == 0 || valueSeries[0] == null || valueSeries[0].length == 0) {

			_highValuesFloat = new float[1][2];
			_lowValuesFloat = new float[1][2];

			_visibleMaxValue = _visibleMinValue = 0;
			_originalMaxValue = _originalMinValue = 0;

		} else {

			/*
			 * Set initial min/max value
			 */
			final float firstValue = getFirstMinMax(valueSeries);
			_visibleMaxValue = _visibleMinValue = firstValue;

			_highValuesFloat = valueSeries;

			for (final float[] valueSerie : valueSeries) {

				if (valueSerie == null) {
					continue;
				}

				for (final float value : valueSerie) {

					if (value != value) {
						// ignore Nan
						continue;
					}

					if (value == Float.POSITIVE_INFINITY) {
						// ignore infinity
						continue;
					}

					if (_isIgnoreMinMaxZero && (value > -FLOAT_ZERO && value < FLOAT_ZERO)) {
						// ignore zero
						continue;
					}

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

	/**
	 * show the y-sliders for the chart
	 * 
	 * @param showYSlider
	 *            The showYSlider to set.
	 */
	public void setShowYSlider(final boolean showYSlider) {

		_isShowYSlider = showYSlider;

		_ySlider1 = new ChartYSlider(this);
		_ySlider2 = new ChartYSlider(this);
	}

	/**
	 * @param sliderLabelFormat
	 */
	public void setSliderLabelFormat(final int sliderLabelFormat) {
		_sliderLabelFormat = sliderLabelFormat;
	}

	public void setSliderLabelProvider(final ISliderLabelProvider sliderLabelProvider) {
		_sliderLabelProvider = sliderLabelProvider;
	}

	/**
	 * set the direction for the y axis <code>
	 * true: the direction is from bottom to top by increasing number
	 * false: the direction is from top to bottom by increasing number
	 * </code>
	 * 
	 * @param axisDirection
	 */
	public void setYAxisDirection(final boolean axisDirection) {
		_yAxisDirection = axisDirection;
	}

	/**
	 * @param title
	 *            set the title for the y-axis
	 */
	public void setYTitle(final String title) {
		_yTitle = title;
	}

	@Override
	public String toString() {
		return "[ChartDataYSerie]";//$NON-NLS-1$
	}

}
