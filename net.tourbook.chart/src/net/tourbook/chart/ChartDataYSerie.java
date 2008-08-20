/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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
	public static final int			BAR_LAYOUT_SINGLE_SERIE	= 1;

	/**
	 * the bars are displayed one of the other
	 */
	public static final int			BAR_LAYOUT_STACKED		= 2;

	/**
	 * the bars are displayed beside each other
	 */
	public static final int			BAR_LAYOUT_BESIDE		= 3;

	public static final String		YDATA_INFO				= "yDataInfo";								//$NON-NLS-1$

	public static final int			FILL_METHOD_NOFILL		= 0;
	public static final int			FILL_METHOD_FILL_BOTTOM	= 1;
	public static final int			FILL_METHOD_FILL_ZERO	= 2;

	private int						fChartLayout			= BAR_LAYOUT_SINGLE_SERIE;
	private String					fYTitle;

	/**
	 * contains the color index for each value
	 */
	private int[][]					fColorIndex;

	private int						graphFillMethod			= FILL_METHOD_NOFILL;

	private boolean					showYSlider				= false;

	/**
	 * <p>
	 * true: the direction is from bottom to top by increasing number <br>
	 * false: the direction is from top to bottom by increasing number
	 */
	private boolean					yAxisDirection			= true;

	/**
	 * Contains a list with all layers which are drawn on top of the graph
	 */
	private ArrayList<IChartLayer>	fCustomLayers			= new ArrayList<IChartLayer>();

	private ChartYSlider			ySliderTop;
	private ChartYSlider			ySliderBottom;

	private final int				fChartType;

	public ChartDataYSerie(final int chartType, final int chartLayout, final int[][] lowValueSeries, final int[][] highValueSeries) {

		fChartType = chartType;
		fChartLayout = chartLayout;

		setMinMaxValues(lowValueSeries, highValueSeries);
	}

	public ChartDataYSerie(final int chartType, final int[] valueSerie) {
		fChartType = chartType;
		setMinMaxValues(new int[][] { valueSerie });
	}

	public ChartDataYSerie(final int chartType, final int[] lowValueSerie, final int[] highValueSerie) {
		fChartType = chartType;
		setMinMaxValues(new int[][] { lowValueSerie }, new int[][] { highValueSerie });
	}

//	public ChartDataYSerie(int chartType, int[][] valueSerie) {
//		fChartType = chartType;
//		setMinMaxValues(valueSerie);
//	}

	public ChartDataYSerie(final int chartType, final int[][] lowValueSeries, final int[][] highValueSeries) {
		fChartType = chartType;
		setMinMaxValues(lowValueSeries, highValueSeries);
	}

//	int getAdjustedMaxValue() {
//		// return yAxisDirection
//		// ? fSavedMaxValue + (fSavedMaxValue / 10)
//		// : (fSavedMaxValue + (fSavedMaxValue / 100));
//		return fOriginalMaxValue;
//	}

//	int getAdjustedMinValue() {
//		// return yAxisDirection
//		// ? fSavedMinValue - (fSavedMinValue / 10)
//		// : (fSavedMinValue - (fSavedMinValue / 100));
//		return fOriginalMinValue;
//	}

	/**
	 * @return Returns the chartLayout.
	 */
	protected int getChartLayout() {
		return fChartLayout;
	}

	public int getChartType() {
		return fChartType;
	}

	/**
	 * @return Returns the valueColors.
	 */
	public int[][] getColorsIndex() {
		if (fColorIndex == null || fColorIndex.length == 0 || fColorIndex[0] == null || fColorIndex[0].length == 0) {
			setAllValueColors(0);
		}
		return fColorIndex;
	}

	public ArrayList<IChartLayer> getCustomLayers() {
		return fCustomLayers;
	}

	/**
	 * @return returns true if the graph is filled
	 */
	public int getGraphFillMethod() {
		return graphFillMethod;
	}

	public String getXTitle() {
		return null;
	}

	/**
	 * @return Returns the ySliderBottom.
	 */
	public ChartYSlider getYSliderBottom() {
		return ySliderBottom;
	}

	/**
	 * @return Returns the ySliderTop.
	 */
	public ChartYSlider getYSliderTop() {
		return ySliderTop;
	}

	/**
	 * @return Returns the title.
	 */
	public String getYTitle() {
		return fYTitle;
	}

	/**
	 * @return Returns the showYSlider.
	 */
	public boolean isShowYSlider() {
		return showYSlider;
	}

	/**
	 * <p>
	 * true: the direction is from bottom to top by increasing number <br>
	 * false: the direction is from top to bottom by increasing number
	 */
	public boolean isYAxisDirection() {
		return yAxisDirection;
	}

	/**
	 * set the color index of all values
	 * 
	 * @param colorIndexValue
	 */
	public void setAllValueColors(final int colorIndexValue) {

		if (fHighValues == null || fHighValues.length == 0 || fHighValues[0] == null || fHighValues[0].length == 0) {
			return;
		}

		fColorIndex = new int[1][fHighValues[0].length];

		for (int colorIndex = 0; colorIndex < fColorIndex[0].length; colorIndex++) {
			fColorIndex[0][colorIndex] = colorIndexValue;
		}
	}

	/**
	 * @param colorIndex
	 *            set's the color index for each value
	 */
	public void setColorIndex(final int[] colorIndex) {
		fColorIndex = new int[][] { colorIndex };
	}

	public void setColorIndex(final int[][] colorIndex) {
		fColorIndex = colorIndex;
	}

	public void setCustomLayers(final ArrayList<IChartLayer> customLayers) {
		fCustomLayers = customLayers;
	}

	/**
	 * @param fillMethod
	 *            when set to <tt>true</tt> graph is filled, default is <tt>false</tt>
	 */
	public void setGraphFillMethod(final int fillMethod) {
		graphFillMethod = fillMethod;
	}

	@Override
	void setMinMaxValues(final int[][] valueSeries) {

		if (valueSeries == null || valueSeries.length == 0 || valueSeries[0] == null || valueSeries[0].length == 0) {
			fHighValues = new int[0][0];
			fVisibleMaxValue = fVisibleMinValue = 0;
			fOriginalMaxValue = fOriginalMinValue = 0;

		} else {

			fHighValues = valueSeries;

			// set initial min/max value
			fVisibleMaxValue = fVisibleMinValue = valueSeries[0][0];

			if (fChartType == ChartDataModel.CHART_TYPE_LINE) {

				super.setMinMaxValues(valueSeries);

			} else if (fChartType == ChartDataModel.CHART_TYPE_BAR) {

				switch (fChartLayout) {
				case ChartDataYSerie.BAR_LAYOUT_SINGLE_SERIE:
				case ChartDataYSerie.BAR_LAYOUT_BESIDE:

					// get the min/max highValues for all data
					for (final int[] valuesOuter : valueSeries) {
						for (final int valuesInner : valuesOuter) {
							fVisibleMaxValue = (fVisibleMaxValue >= valuesInner) ? fVisibleMaxValue : valuesInner;
							fVisibleMinValue = (fVisibleMinValue <= valuesInner) ? fVisibleMinValue : valuesInner;
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

							serieMax[valueIndex] = (fVisibleMaxValue >= outerValueWithMax)
									? fVisibleMaxValue
									: outerValueWithMax;

							fVisibleMinValue = (fVisibleMinValue <= outerValue) ? fVisibleMinValue : outerValue;
						}
					}

					// get max for all series
					fVisibleMaxValue = 0;
					for (final int serieValue : serieMax) {
						fVisibleMaxValue = (fVisibleMaxValue >= serieValue) ? fVisibleMaxValue : serieValue;
					}

					break;
				}

			} else if (fChartType == ChartDataModel.CHART_TYPE_LINE_WITH_BARS) {

				super.setMinMaxValues(valueSeries);
			}

			fOriginalMinValue = fVisibleMinValue;
			fOriginalMaxValue = fVisibleMaxValue;
		}
	}

	@Override
	void setMinMaxValues(final int[][] lowValues, final int[][] highValues) {

		if (lowValues == null || lowValues.length == 0 || lowValues[0] == null || lowValues[0].length == 0

		|| highValues == null || highValues.length == 0 || highValues[0] == null || highValues[0].length == 0) {

			fVisibleMaxValue = fVisibleMinValue = 0;
			fOriginalMaxValue = fOriginalMinValue = 0;

			fLowValues = new int[1][2];
			fHighValues = new int[1][2];

		} else {

			fLowValues = lowValues;
			fHighValues = highValues;
			fColorIndex = new int[fHighValues.length][fHighValues[0].length];

			// set initial min/max value
			fVisibleMinValue = lowValues[0][0];
			fVisibleMaxValue = highValues[0][0];

			if (fChartType == ChartDataModel.CHART_TYPE_LINE
					|| (fChartType == ChartDataModel.CHART_TYPE_BAR && fChartLayout == ChartDataYSerie.BAR_LAYOUT_SINGLE_SERIE)
					|| (fChartType == ChartDataModel.CHART_TYPE_BAR && fChartLayout == ChartDataYSerie.BAR_LAYOUT_BESIDE)) {

				// get the min/max values for all data
				for (final int[] valueSerie : highValues) {
					for (final int value : valueSerie) {
						fVisibleMaxValue = (fVisibleMaxValue >= value) ? fVisibleMaxValue : value;
					}
				}

				for (final int[] valueSerie : lowValues) {
					for (final int value : valueSerie) {
						fVisibleMinValue = (fVisibleMinValue <= value) ? fVisibleMinValue : value;
					}
				}

			} else if (fChartType == ChartDataModel.CHART_TYPE_BAR
					&& fChartLayout == ChartDataYSerie.BAR_LAYOUT_STACKED) {

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
					fVisibleMaxValue = (fVisibleMaxValue >= value) ? fVisibleMaxValue : value;
				}

				/*
				 * calculate the min value
				 */
				for (final int[] serieData : lowValues) {
					for (final int value : serieData) {
						fVisibleMinValue = (fVisibleMinValue <= value) ? fVisibleMinValue : value;
					}
				}
			}

			fOriginalMinValue = fVisibleMinValue;
			fOriginalMaxValue = fVisibleMaxValue;
		}
	}

	/**
	 * show the y-sliders for the chart
	 * 
	 * @param showYSlider
	 *            The showYSlider to set.
	 */
	public void setShowYSlider(final boolean showYSlider) {

		this.showYSlider = showYSlider;

		ySliderTop = new ChartYSlider(this);
		ySliderBottom = new ChartYSlider(this);
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
		yAxisDirection = axisDirection;
	}

	/**
	 * @param title
	 *            set the title for the y-axis
	 */
	public void setYTitle(final String title) {
		fYTitle = title;
	}
}
