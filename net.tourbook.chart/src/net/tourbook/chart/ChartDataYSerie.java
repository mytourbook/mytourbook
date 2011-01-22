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

import java.util.ArrayList;

/**
 * Contains the highValues and display attributes for one data serie
 */
public class ChartDataYSerie extends ChartDataSerie {

	/**
	 * the bars has only a low and high value
	 */
	public static final int			BAR_LAYOUT_SINGLE_SERIE		= 1;

	/**
	 * the bars are displayed one of the other
	 */
	public static final int			BAR_LAYOUT_STACKED			= 2;

	/**
	 * the bars are displayed beside each other
	 */
	public static final int			BAR_LAYOUT_BESIDE			= 3;

	public static final String		YDATA_INFO					= "yDataInfo";					//$NON-NLS-1$

	public static final int			FILL_METHOD_NOFILL			= 0;
	public static final int			FILL_METHOD_FILL_BOTTOM		= 1;
	public static final int			FILL_METHOD_FILL_ZERO		= 2;

	/**
	 * Slider label format: n.1
	 */
	public static final int			SLIDER_LABEL_FORMAT_DEFAULT	= 0;
	/**
	 * Slider label format: mm:ss
	 */
	public static final int			SLIDER_LABEL_FORMAT_MM_SS	= 1;

	private int						_sliderLabelFormat			= SLIDER_LABEL_FORMAT_DEFAULT;
	private int						_chartLayout				= BAR_LAYOUT_SINGLE_SERIE;
	private String					_yTitle;

	/**
	 * contains the color index for each value
	 */
	private int[][]					_colorIndex;

	private int						_graphFillMethod			= FILL_METHOD_NOFILL;

	private boolean					_showYSlider				= false;

	/**
	 * <p>
	 * true: the direction is from bottom to top by increasing number <br>
	 * false: the direction is from top to bottom by increasing number
	 */
	private boolean					_yAxisDirection				= true;

	/**
	 * Contains a list with all layers which are drawn on top of the graph
	 */
	private ArrayList<IChartLayer>	_customLayers				= new ArrayList<IChartLayer>();

	private ChartYSlider			_ySliderTop;

	private ChartYSlider			_ySliderBottom;
	private final int				_chartType;

	/**
	 * 2nd y-data serie is currently used to display the slider label and the pace y-units
	 */
//	private int[]					fYData2ndSerie;

	public ChartDataYSerie(	final int chartType,
							final int chartLayout,
							final int[][] lowValueSeries,
							final int[][] highValueSeries) {

		_chartType = chartType;
		_chartLayout = chartLayout;

		setMinMaxValues(lowValueSeries, highValueSeries);
	}

	public ChartDataYSerie(final int chartType, final int[] valueSerie) {
		_chartType = chartType;
		setMinMaxValues(new int[][] { valueSerie });
	}

	public ChartDataYSerie(final int chartType, final int[] lowValueSerie, final int[] highValueSerie) {
		_chartType = chartType;
		setMinMaxValues(new int[][] { lowValueSerie }, new int[][] { highValueSerie });
	}

	public ChartDataYSerie(final int chartType, final int[][] valueSeries) {
		_chartType = chartType;
		setMinMaxValues(valueSeries);
	}

	public ChartDataYSerie(final int chartType, final int[][] lowValueSeries, final int[][] highValueSeries) {
		_chartType = chartType;
		setMinMaxValues(lowValueSeries, highValueSeries);
	}

	/**
	 * @return Returns the chartLayout.
	 */
	protected int getChartLayout() {
		return _chartLayout;
	}

	public int getChartType() {
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

	public ArrayList<IChartLayer> getCustomLayers() {
		return _customLayers;
	}

	/**
	 * @return returns true if the graph is filled
	 */
	public int getGraphFillMethod() {
		return _graphFillMethod;
	}

	/**
	 * @return Returns the format how the slider label will be formatted, which can be <br>
	 *         {@link #SLIDER_LABEL_FORMAT_DEFAULT}<br> {@link #SLIDER_LABEL_FORMAT_MM_SS}
	 */
	public int getSliderLabelFormat() {
		return _sliderLabelFormat;
	}

	public String getXTitle() {
		return null;
	}

	/**
	 * @return Returns the ySliderBottom.
	 */
	public ChartYSlider getYSliderBottom() {
		return _ySliderBottom;
	}

	/**
	 * @return Returns the ySliderTop.
	 */
	public ChartYSlider getYSliderTop() {
		return _ySliderTop;
	}

	/**
	 * @return Returns the title.
	 */
	public String getYTitle() {
		return _yTitle;
	}

	/**
	 * @return Returns the showYSlider.
	 */
	public boolean isShowYSlider() {
		return _showYSlider;
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

		if (_highValues == null || _highValues.length == 0 || _highValues[0] == null || _highValues[0].length == 0) {
			return;
		}

		_colorIndex = new int[1][_highValues[0].length];

		for (int colorIndex = 0; colorIndex < _colorIndex[0].length; colorIndex++) {
			_colorIndex[0][colorIndex] = colorIndexValue;
		}
	}

	/**
	 * @param colorIndex
	 *            set's the color index for each value
	 */
	public void setColorIndex(final int[] colorIndex) {
		_colorIndex = new int[][] { colorIndex };
	}

	public void setColorIndex(final int[][] colorIndex) {
		_colorIndex = colorIndex;
	}

	public void setCustomLayers(final ArrayList<IChartLayer> customLayers) {
		_customLayers = customLayers;
	}

	/**
	 * @param fillMethod
	 *            when set to <tt>true</tt> graph is filled, default is <tt>false</tt>
	 */
	public void setGraphFillMethod(final int fillMethod) {
		_graphFillMethod = fillMethod;
	}

	@Override
	void setMinMaxValues(final int[][] valueSeries) {

		if (valueSeries == null || valueSeries.length == 0 || valueSeries[0] == null || valueSeries[0].length == 0) {
			_highValues = new int[0][0];
			_visibleMaxValue = _visibleMinValue = 0;
			_originalMaxValue = _originalMinValue = 0;

		} else {

			_highValues = valueSeries;

			// set initial min/max value
			_visibleMaxValue = _visibleMinValue = valueSeries[0][0];

			if (_chartType == ChartDataModel.CHART_TYPE_LINE
					|| _chartType == ChartDataModel.CHART_TYPE_LINE_WITH_BARS
					|| _chartType == ChartDataModel.CHART_TYPE_XY_SCATTER) {

				super.setMinMaxValues(valueSeries);

			} else if (_chartType == ChartDataModel.CHART_TYPE_BAR) {

				switch (_chartLayout) {
				case ChartDataYSerie.BAR_LAYOUT_SINGLE_SERIE:
				case ChartDataYSerie.BAR_LAYOUT_BESIDE:

					// get the min/max highValues for all data
					for (final int[] valuesOuter : valueSeries) {
						for (final int valuesInner : valuesOuter) {
							_visibleMaxValue = (_visibleMaxValue >= valuesInner) ? _visibleMaxValue : valuesInner;
							_visibleMinValue = (_visibleMinValue <= valuesInner) ? _visibleMinValue : valuesInner;
						}
					}
					break;

				case ChartDataYSerie.BAR_LAYOUT_STACKED:

					final int serieMax[] = new int[valueSeries[0].length];

					// get the max value for the data which are stacked on each
					// other
					for (final int[] valuesOuter : valueSeries) {
						for (int valueIndex = 0; valueIndex < valuesOuter.length; valueIndex++) {

							final int outerValue = valuesOuter[valueIndex];
							final int outerValueWithMax = serieMax[valueIndex] + outerValue;

							serieMax[valueIndex] = (_visibleMaxValue >= outerValueWithMax)
									? _visibleMaxValue
									: outerValueWithMax;

							_visibleMinValue = (_visibleMinValue <= outerValue) ? _visibleMinValue : outerValue;
						}
					}

					// get max for all series
					_visibleMaxValue = 0;
					for (final int serieValue : serieMax) {
						_visibleMaxValue = (_visibleMaxValue >= serieValue) ? _visibleMaxValue : serieValue;
					}

					break;
				}
			}

			_originalMinValue = _visibleMinValue;
			_originalMaxValue = _visibleMaxValue;
		}
	}

	@Override
	void setMinMaxValues(final int[][] lowValues, final int[][] highValues) {

		if (lowValues == null || lowValues.length == 0 || lowValues[0] == null || lowValues[0].length == 0

		|| highValues == null || highValues.length == 0 || highValues[0] == null || highValues[0].length == 0) {

			_visibleMaxValue = _visibleMinValue = 0;
			_originalMaxValue = _originalMinValue = 0;

			_lowValues = new int[1][2];
			_highValues = new int[1][2];

		} else {

			_lowValues = lowValues;
			_highValues = highValues;
			_colorIndex = new int[_highValues.length][_highValues[0].length];

			// set initial min/max value
			_visibleMinValue = lowValues[0][0];
			_visibleMaxValue = highValues[0][0];

			if (_chartType == ChartDataModel.CHART_TYPE_LINE
					|| (_chartType == ChartDataModel.CHART_TYPE_BAR && _chartLayout == ChartDataYSerie.BAR_LAYOUT_SINGLE_SERIE)
					|| (_chartType == ChartDataModel.CHART_TYPE_BAR && _chartLayout == ChartDataYSerie.BAR_LAYOUT_BESIDE)) {

				// get the min/max values for all data
				for (final int[] valueSerie : highValues) {
					for (final int value : valueSerie) {
						_visibleMaxValue = (_visibleMaxValue >= value) ? _visibleMaxValue : value;
					}
				}

				for (final int[] valueSerie : lowValues) {
					for (final int value : valueSerie) {
						_visibleMinValue = (_visibleMinValue <= value) ? _visibleMinValue : value;
					}
				}

			} else if (_chartType == ChartDataModel.CHART_TYPE_BAR
					&& _chartLayout == ChartDataYSerie.BAR_LAYOUT_STACKED) {

				/*
				 * calculate the max value
				 */

				// summarize the data
				final int[] summarizedMaxValues = new int[highValues[0].length];
				for (final int[] valueSerie : highValues) {
					for (int valueIndex = 0; valueIndex < valueSerie.length; valueIndex++) {
						summarizedMaxValues[valueIndex] += valueSerie[valueIndex];
					}
				}

				// get max value for the summarized values
				for (final int value : summarizedMaxValues) {
					_visibleMaxValue = (_visibleMaxValue >= value) ? _visibleMaxValue : value;
				}

				/*
				 * calculate the min value
				 */
				for (final int[] serieData : lowValues) {
					for (final int value : serieData) {
						_visibleMinValue = (_visibleMinValue <= value) ? _visibleMinValue : value;
					}
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

		this._showYSlider = showYSlider;

		_ySliderTop = new ChartYSlider(this);
		_ySliderBottom = new ChartYSlider(this);
	}

	/**
	 * @param sliderLabelFormat
	 */
	public void setSliderLabelFormat(final int sliderLabelFormat) {
		_sliderLabelFormat = sliderLabelFormat;
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
