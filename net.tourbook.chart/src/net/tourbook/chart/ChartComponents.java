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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import net.tourbook.common.UI;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;

/**
 * Chart widget which represents the chart ui The chart consists of these components
 * <p>
 * The chart widget consists has the following heights:
 * 
 * <pre>
 *  {@link #_devMarginTop}
 *  {@link #_devXTitleBarHeight}
 * 
 *  |devSliderBarHeight
 *  |#graph#
 * 
 *   verticalDistance
 * 
 *  |devSliderBarHeight
 *  |#graph#
 * 
 *   verticalDistance
 * 
 *     ...
 * 
 *   |devSliderBarHeight
 *   |#graph#
 * 
 *   {@link #_devXAxisHeight}
 * </pre>
 */
public class ChartComponents extends Composite {

	public static final int		BAR_SELECTION_DELAY_TIME		= 100;

	/**
	 * min/max pixel widthDev/heightDev of the chart
	 */
	static final int			CHART_MIN_WIDTH					= 5;
	static final int			CHART_MIN_HEIGHT				= 5;
	static final int			CHART_MAX_WIDTH					= 10000000;						// 10'000'000
	static final int			CHART_MAX_HEIGHT				= 10000;

	static final int			SLIDER_BAR_HEIGHT				= 10;
	static final int			TITLE_BAR_HEIGHT				= 18;								//15;
	static final int			MARGIN_TOP_WITH_TITLE			= 5;
	static final int			MARGIN_TOP_WITHOUT_TITLE		= 10;

	private static final int	DAY_IN_SECONDS					= 24 * 60 * 60;

	private final Chart			_chart;

	/**
	 * top margin of the chart (and all it's components)
	 */
	private int					_devMarginTop					= MARGIN_TOP_WITHOUT_TITLE;

	/**
	 * height of the slider bar, 0 indicates that the slider is not visible
	 */
	int							_devSliderBarHeight				= 0;

	/**
	 * height of the title bar, 0 indicates that the title is not visible
	 */
	private int					_devXTitleBarHeight				= 0;

	/**
	 * height of the horizontal axis
	 */
	private final int			_devXAxisHeight					= 25;

	/**
	 * width of the vertical axis
	 */
	private final int			_yAxisWidthLeft					= 50;
	private int					_yAxisWidthLeftWithTitle		= _yAxisWidthLeft;
	private final int			_yAxisWidthRight				= 50;

	/**
	 * vertical distance between two graphs
	 */
	private final int			_chartsVerticalDistance			= 15;

	/**
	 * contains the {@link SynchConfiguration} for the current chart and will be used from the chart
	 * which is synchronized
	 */
	SynchConfiguration			_synchConfigOut					= null;

	/**
	 * when a {@link SynchConfiguration} is set, this chart will be synchronized with the chart
	 * which set's the synch config
	 */
	SynchConfiguration			_synchConfigSrc					= null;

	/**
	 * visible chart rectangle
	 */
	private Rectangle			_visibleGraphRect;

	final ChartComponentGraph	componentGraph;
	final ChartComponentAxis	componentAxisLeft;
	final ChartComponentAxis	componentAxisRight;

	private ChartDataModel		_chartDataModel					= null;

	private ChartDrawingData	_chartDrawingData;

	public boolean				_useAdvancedGraphics			= true;

	private static final String	_monthLabels[]					= {
			Messages.Month_jan,
			Messages.Month_feb,
			Messages.Month_mar,
			Messages.Month_apr,
			Messages.Month_mai,
			Messages.Month_jun,
			Messages.Month_jul,
			Messages.Month_aug,
			Messages.Month_sep,
			Messages.Month_oct,
			Messages.Month_nov,
			Messages.Month_dec									};

	private static final String	_monthShortLabels[]				= {
			Integer.toString(1),
			Integer.toString(2),
			Integer.toString(3),
			Integer.toString(4),
			Integer.toString(5),
			Integer.toString(6),
			Integer.toString(7),
			Integer.toString(8),
			Integer.toString(9),
			Integer.toString(10),
			Integer.toString(11),
			Integer.toString(12)								};

	/**
	 * Width in pixel for all months in one year
	 */
	private int					_devYearEqualMonthsWidth		= -1;
	private int					_devYearEqualMonthsShortWidth	= -1;

	private final int[]			_keyDownCounter					= new int[1];
	private final int[]			_lastKeyDownCounter				= new int[1];

	private final Calendar		_calendar						= GregorianCalendar.getInstance();

	/**
	 * this error message is displayed instead of the chart when it's not <code>null</code>
	 */
	String						errorMessage;

	/**
	 * Create and layout the components of the chart
	 * 
	 * @param parent
	 * @param style
	 */
	ChartComponents(final Chart parent, final int style) {

		super(parent, style);

		GridData gd;
		_chart = parent;

		// set layout for the components
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		setLayoutData(gd);

		// set layout for this chart
		final GridLayout gl = new GridLayout(3, false);
		gl.horizontalSpacing = 0;
		gl.verticalSpacing = 0;
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		setLayout(gl);

		// left: create left axis canvas
		componentAxisLeft = new ChartComponentAxis(parent, this, SWT.NONE);
		gd = new GridData(SWT.NONE, SWT.FILL, false, true);
		gd.widthHint = _yAxisWidthLeft;
		componentAxisLeft.setLayoutData(gd);

		// center: create chart canvas
		componentGraph = new ChartComponentGraph(parent, this, SWT.NONE);
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		componentGraph.setLayoutData(gd);

		// right: create right axis canvas
		componentAxisRight = new ChartComponentAxis(parent, this, SWT.NONE);
		gd = new GridData(SWT.NONE, SWT.FILL, false, true);
		gd.widthHint = _yAxisWidthRight;
		componentAxisRight.setLayoutData(gd);

		componentAxisLeft.setComponentGraph(componentGraph);
		componentAxisRight.setComponentGraph(componentGraph);

		addListener();
	}

	private void addListener() {

		// this is the only resize listener for the whole chart
		addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent event) {
				onResize();
			}
		});

//		fComponentGraph.addListener(SWT.Traverse, new Listener() {
//			public void handleEvent(final Event event) {
//
//				switch (event.detail) {
//				case SWT.TRAVERSE_RETURN:
//				case SWT.TRAVERSE_ESCAPE:
//				case SWT.TRAVERSE_TAB_NEXT:
//				case SWT.TRAVERSE_TAB_PREVIOUS:
//				case SWT.TRAVERSE_PAGE_NEXT:
//				case SWT.TRAVERSE_PAGE_PREVIOUS:
//					event.doit = true;
//					break;
//				}
//			}
//		});
//
//		fComponentGraph.addListener(SWT.KeyDown, new Listener() {
//			public void handleEvent(final Event event) {
//				handleLeftRightEvent(event);
//			}
//		});

	}

	/**
	 * Computes all the data for the chart
	 * 
	 * @return chart drawing data
	 */
	private ChartDrawingData createDrawingData() {

		// compute the graphs and axis
		final ArrayList<GraphDrawingData> graphDrawingData = new ArrayList<GraphDrawingData>();

		final ChartDrawingData chartDrawingData = new ChartDrawingData(graphDrawingData);

		chartDrawingData.chartDataModel = _chartDataModel;

		final ArrayList<ChartDataYSerie> yDataList = _chartDataModel.getYData();
		final ChartDataXSerie xData = _chartDataModel.getXData();
		final ChartDataXSerie xData2nd = _chartDataModel.getXData2nd();

		final int graphCount = yDataList.size();
		int graphIndex = 1;

		// loop all graphs
		for (final ChartDataYSerie yData : yDataList) {

			final GraphDrawingData drawingData = new GraphDrawingData(chartDrawingData, yData.getChartType());

			graphDrawingData.add(drawingData);

			// set chart title above the first graph
			if (graphIndex == 1) {

				drawingData.setXTitle(_chartDataModel.getTitle());

				// set the chart title height and margin
				final String title = drawingData.getXTitle();
				final ChartSegments chartSegments = xData.getChartSegments();

				if (title != null && title.length() > 0 || //
						(chartSegments != null && chartSegments.segmentTitle != null)) {

					_devXTitleBarHeight = TITLE_BAR_HEIGHT;
					_devMarginTop = MARGIN_TOP_WITH_TITLE;
				}
			}

			// set x/y data
			drawingData.setXData(xData);
			drawingData.setXData2nd(xData2nd);
			drawingData.setYData(yData);

			// compute x/y values
			createDrawingDataXValues(drawingData);
			createDrawingDataYValues(drawingData, graphCount, graphIndex);

			// reset adjusted y-slider value
			yData.adjustedYValue = Float.MIN_VALUE;

			graphIndex++;
		}

		// set values after they have been computed
		chartDrawingData.devMarginTop = _devMarginTop;
		chartDrawingData.devXTitelBarHeight = _devXTitleBarHeight;
		chartDrawingData.devSliderBarHeight = _devSliderBarHeight;
		chartDrawingData.devXAxisHeight = _devXAxisHeight;

		return chartDrawingData;
	}

	/**
	 * Compute units for the x-axis and keep it in the drawingData object
	 */
	private void createDrawingDataXValues(final GraphDrawingData drawingData) {

		final ChartDataXSerie xData = drawingData.getXData();

		final float graphMaxValue = xData.getOriginalMaxValue();
		final int unitType = xData.getAxisUnit();
		final float xStartValue = xData.getStartValue();

		final int devVirtualGraphWidth = componentGraph.getXXDevGraphWidth();
		final float scaleX = ((float) devVirtualGraphWidth - 1) / graphMaxValue;

		drawingData.devVirtualGraphWidth = devVirtualGraphWidth;
		drawingData.setScaleX(scaleX);

		/*
		 * calculate the number of units which will be visible by dividing the visible length by the
		 * minimum size which one unit should have in pixels
		 */
		final int defaultUnitCount = devVirtualGraphWidth / _chart.gridHorizontalDistance;

		// unitRawValue is the number in data values for one unit
		final float defaultUnitValue = graphMaxValue / Math.max(1, defaultUnitCount);

		// get the unit list from the configuration
		final ArrayList<ChartUnit> unitList = drawingData.getXUnits();

		switch (unitType) {

		case ChartDataSerie.X_AXIS_UNIT_DAY:

			createXValuesDay(drawingData, unitList, devVirtualGraphWidth);
			break;

		case ChartDataSerie.X_AXIS_UNIT_WEEK:

			createXValuesWeek(drawingData, unitList, devVirtualGraphWidth);
			break;

		case ChartDataSerie.X_AXIS_UNIT_MONTH:

			createXValuesMonth(drawingData, unitList, devVirtualGraphWidth);
			break;

		case ChartDataSerie.X_AXIS_UNIT_YEAR:

			createXValuesYear(drawingData, unitList, devVirtualGraphWidth);
			break;

		default:

			// axis unit
			double graphUnit = 1; // this default value should be overwritten
			double majorValue = 0;

			switch (unitType) {
			case ChartDataSerie.AXIS_UNIT_HOUR_MINUTE_SECOND:
			case ChartDataSerie.AXIS_UNIT_HOUR_MINUTE_OPTIONAL_SECOND:
			case ChartDataSerie.AXIS_UNIT_HOUR_MINUTE:
				graphUnit = Util.roundTimeValue((long) defaultUnitValue, false);
				majorValue = Util.getMajorTimeValue((long) graphUnit, false);
				break;

			case ChartDataSerie.AXIS_UNIT_NUMBER:
			case ChartDataSerie.X_AXIS_UNIT_NUMBER_CENTER:
				// unit is a decimal number
				graphUnit = Util.roundDecimalValue(defaultUnitValue);
				majorValue = Util.getMajorDecimalValue(graphUnit);
				break;

			default:
				break;
			}

			/*
			 * create units for the x-axis
			 */

			// get the unitOffset when a startValue is set
			double unitOffset = 0;
			if (xStartValue != 0) {
				unitOffset = xStartValue % graphUnit;
			}

			final int valueDivisor = xData.getValueDivisor();
			int loopCounter = 0;

			/*
			 * increase by one unit that the right side of the chart is drawing a unit, in some
			 * cases this didn't occured
			 */
			double graphMinVisibleValue = xData.getVisibleMinValue();
			double graphMaxVisibleValue = xData.getVisibleMaxValue();

			// decrease min value when it does not fit to unit borders
			final double graphMinRemainder = graphMinVisibleValue % graphUnit;
			graphMinVisibleValue = graphMinVisibleValue - graphMinRemainder;

			graphMinVisibleValue = Util.roundFloatToUnit(graphMinVisibleValue, graphUnit, true);
			graphMaxVisibleValue = Util.roundFloatToUnit(graphMaxVisibleValue, graphUnit, false);

			double graphValue = graphMinVisibleValue;

			while (graphValue <= graphMaxVisibleValue) {

				// create unit value/label
				final double unitPos = graphValue - unitOffset;
				double unitLabelValue = unitPos + xStartValue;

				if ((unitType == ChartDataSerie.AXIS_UNIT_HOUR_MINUTE_SECOND //
						|| unitType == ChartDataSerie.AXIS_UNIT_HOUR_MINUTE_OPTIONAL_SECOND)
						&& xStartValue > 0) {

					/*
					 * x-axis shows day time, start with 0:00 at midnight
					 */

					unitLabelValue = unitLabelValue % DAY_IN_SECONDS;
				}

				final String unitLabel = Util.formatValue((float) unitLabelValue, unitType, valueDivisor, true);

				final boolean isMajorValue = graphValue % majorValue == 0;

				unitList.add(new ChartUnit((float) graphValue, unitLabel, isMajorValue));

				// check for an infinity loop
				if (graphValue == graphMaxValue || loopCounter++ > 10000) {
					break;
				}

				graphValue += graphUnit;
				graphValue = Util.roundFloatToUnit(graphValue, graphUnit, false);
			}

			break;
		}

		/*
		 * configure bars in the bar charts
		 */
		if (_chartDataModel.getChartType() == ChartDataModel.CHART_TYPE_BAR) {

			final float[] highValues = xData.getHighValues()[0];

			if (unitType == ChartDataSerie.AXIS_UNIT_NUMBER || unitType == ChartDataSerie.AXIS_UNIT_HOUR_MINUTE) {

				final int barWidth = (devVirtualGraphWidth / highValues.length) / 2;

				drawingData.setBarRectangleWidth(Math.max(0, barWidth));
				drawingData.setBarPosition(GraphDrawingData.BAR_POS_CENTER);

			} else if (unitType == ChartDataSerie.X_AXIS_UNIT_NUMBER_CENTER) {

				/*
				 * set bar width that it is wide enouth to overlap the next right bar, the
				 * overlapped part will be removed in ChartComponentGraph.draw210BarGraph()
				 */
				final float barWidth = ((float) devVirtualGraphWidth / (highValues.length - 1));
				final int barWidth2 = (int) (Math.max(1, barWidth) * 1.10);

				drawingData.setBarRectangleWidth(barWidth2);
				drawingData.setBarPosition(GraphDrawingData.BAR_POS_CENTER);
			}
		}
	}

	/**
	 * computes data for the y axis
	 * 
	 * @param drawingData
	 * @param graphCount
	 * @param currentGraph
	 */
	private void createDrawingDataYValues(	final GraphDrawingData drawingData,
											final int graphCount,
											final int currentGraph) {

		final int unitType = drawingData.getYData().getAxisUnit();

		if (unitType == ChartDataSerie.AXIS_UNIT_NUMBER) {

			createDrawingDataYValues10Numbers(drawingData, graphCount, currentGraph);

		} else if (unitType == ChartDataSerie.AXIS_UNIT_HOUR_MINUTE
				|| unitType == ChartDataSerie.AXIS_UNIT_HOUR_MINUTE_24H
				|| unitType == ChartDataSerie.AXIS_UNIT_HOUR_MINUTE_SECOND
				|| unitType == ChartDataSerie.AXIS_UNIT_MINUTE_SECOND) {

			createDrawingDataYValues20Time(drawingData, graphCount, currentGraph);
		}
	}

	private void createDrawingDataYValues10Numbers(	final GraphDrawingData drawingData,
													final int graphCount,
													final int currentGraph) {

		final ChartDataYSerie yData = drawingData.getYData();
		final int unitType = yData.getAxisUnit();

		// height of one chart graph including the slider bar
		final int devChartHeight = getDevChartHeightWithoutTrim();

		int devGraphHeight = devChartHeight;

		/*
		 * adjust graph device height for stacked graphs, a gap is between two graphs
		 */
		if (_chartDataModel.isStackedChart() && graphCount > 1) {
			final int devGraphHeightSpace = devGraphHeight - (_chartsVerticalDistance * (graphCount - 1));
			devGraphHeight = (devGraphHeightSpace / graphCount);
		}

		// enforce minimum chart height
		devGraphHeight = Math.max(devGraphHeight, CHART_MIN_HEIGHT);

		// remove slider bar from graph height
		devGraphHeight -= _devSliderBarHeight;

		/*
		 * all variables starting with graph... contain data values from the graph which are not
		 * scaled to the device
		 */

		final float graphMinValue = yData.getVisibleMinValue();
		final float graphMaxValue = yData.getVisibleMaxValue();

		final float defaultValueRange = graphMaxValue > 0
				? (graphMaxValue - graphMinValue)
				: -(graphMinValue - graphMaxValue);

		/*
		 * calculate the number of units which will be visible by dividing the available height by
		 * the minimum size which one unit should have in pixels
		 */
		final int defaultUnitCount = devGraphHeight / _chart.gridVerticalDistance;

		// defaultUnitValue is the number in data values for one unit
		final float defaultUnitValue = defaultValueRange / Math.max(1, defaultUnitCount);

		// round the unit
		final double graphUnit = Util.roundDecimalValue(defaultUnitValue);

		/*
		 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		 * The scaled unit with long min/max values is used because arithmetic with floating point
		 * values fails. BigDecimal is necessary otherwise the scaledUnit can be wrong !!!
		 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		 */
		final long valueScaling = Util.getValueScaling(graphUnit);

		final BigDecimal bigGraphUnit = new BigDecimal(Double.valueOf(graphUnit));
		final BigDecimal bigValueScaling = new BigDecimal(valueScaling);
		final BigDecimal bigScaledUnit = bigGraphUnit.multiply(bigValueScaling);

		final long scaledUnit = bigScaledUnit.longValue();

		long scaledMinValue = (long) (graphMinValue * valueScaling);
		long scaledMaxValue = (long) (graphMaxValue * valueScaling);

		/*
		 * adjustedYValue is set when the y-slider has been moved
		 */
		boolean isMinAdjusted = false;
		boolean isMaxAdjusted = false;
		final float adjustedYValue = yData.adjustedYValue;
		final boolean isMinMaxAdjusted = adjustedYValue != Float.MIN_VALUE;

		if (isMinMaxAdjusted) {

			final long scaledAdjustedYValue = (long) (adjustedYValue * valueScaling);

			isMinAdjusted = scaledAdjustedYValue == scaledMinValue;
			isMaxAdjusted = scaledAdjustedYValue == scaledMaxValue;
		}

		/*
		 * adjust min value, decrease min value when it does not fit to unit borders
		 */
		float adjustMinValue = 0;
		final long minRemainder = scaledMinValue % scaledUnit;
		if (minRemainder != 0 && scaledMinValue < 0) {
			adjustMinValue = scaledUnit;
		}
		final long adjustedScaledMinValue = (long) ((scaledMinValue - adjustMinValue) / scaledUnit) * scaledUnit;

		/*
		 * ensure that min value is not at the bottom of the graph, except values which start at 0
		 */
		if (isMinMaxAdjusted == false && scaledMinValue == adjustedScaledMinValue && scaledMinValue != 0) {
			scaledMinValue = adjustedScaledMinValue - scaledUnit;
		} else if (isMinMaxAdjusted && isMinAdjusted) {
			scaledMinValue = adjustedScaledMinValue;// - scaledUnit;
		} else {
			scaledMinValue = adjustedScaledMinValue;
		}

		/*
		 * adjust max value, increase the max value when it does not fit to unit borders
		 */
		float adjustMaxValue = 0;
		final long maxRemainder = scaledMaxValue % scaledUnit;
		if (maxRemainder != 0) {
			adjustMaxValue = scaledUnit;
		}
		final long adjustedScaledMaxValue = ((long) ((scaledMaxValue + adjustMaxValue) / scaledUnit) * scaledUnit);

		// ensure that max value is not at the top of the graph
		if (isMinMaxAdjusted == false && scaledMaxValue == adjustedScaledMaxValue) {
			scaledMaxValue = adjustedScaledMaxValue + scaledUnit;
		} else if (isMinMaxAdjusted && isMaxAdjusted) {
			scaledMaxValue = adjustedScaledMaxValue;// + scaledUnit;
		} else {
			scaledMaxValue = adjustedScaledMaxValue;
		}

		/*
		 * check that max is larger than min
		 */
		if (scaledMinValue == scaledMaxValue) {

			scaledMinValue = scaledMinValue - scaledUnit;
			scaledMaxValue = scaledMaxValue + scaledUnit;

		} else if (scaledMinValue > scaledMaxValue) {
			/*
			 * this case can happen when the min value is set in the pref dialog, this is more a
			 * hack than a good solution
			 */
			scaledMinValue = scaledMaxValue - (2 * scaledUnit);
		}

		final long scaledValueRange = scaledMaxValue > 0
				? (scaledMaxValue - scaledMinValue)
				: -(scaledMinValue - scaledMaxValue);

		// get major values according to the unit
		final double majorValue = Util.getMajorDecimalValue(graphUnit);

		// calculate the vertical scaling between graph and device
		final float value1 = (float) scaledValueRange / valueScaling;
		final float graphScaleY = devGraphHeight / value1;

		// calculate the vertical device offset
		int devYTop = _devMarginTop + _devXTitleBarHeight;

		if (_chartDataModel.isStackedChart()) {
			// each chart has its own drawing rectangle which are stacked on
			// top of each other
			devYTop += (currentGraph * (devGraphHeight + _devSliderBarHeight))
					+ ((currentGraph - 1) * _chartsVerticalDistance);

		} else {
			// all charts are drawn on the same rectangle
			devYTop += devGraphHeight;
		}

		drawingData.setScaleY(graphScaleY);

		drawingData.setDevYBottom(devYTop);
		drawingData.setDevYTop(devYTop - devGraphHeight);

		drawingData.setGraphYBottom((float) scaledMinValue / valueScaling);
		drawingData.setGraphYTop((float) scaledMaxValue / valueScaling);

		drawingData.devGraphHeight = devGraphHeight;
		drawingData.setDevSliderHeight(_devSliderBarHeight);

		final ArrayList<ChartUnit> unitList = drawingData.getYUnits();
		final int valueDivisor = yData.getValueDivisor();
		int loopCounter = 0;

		long scaledValue = scaledMinValue;

		// loop: create unit label for all units
		while (scaledValue <= scaledMaxValue) {

			final double descaledValue = (double) scaledValue / valueScaling;

			final String unitLabel = Util.formatValue((float) descaledValue, unitType, valueDivisor, false);
			final boolean isMajorValue = descaledValue % majorValue == 0;

			unitList.add(new ChartUnit((float) descaledValue, unitLabel, isMajorValue));

			// prevent endless loops when the unit is 0
			if (scaledValue == scaledMaxValue || loopCounter++ > 1000) {
				break;
			}

			scaledValue += scaledUnit;
		}

		if (unitType == ChartDataSerie.AXIS_UNIT_HOUR_MINUTE_24H && scaledValue > scaledMaxValue) {
			unitList.add(new ChartUnit(scaledMaxValue / valueScaling, Util.EMPTY_STRING));
		}
	}

	private void createDrawingDataYValues20Time(final GraphDrawingData drawingData,
												final int graphCount,
												final int currentGraph) {

		final ChartDataYSerie yData = drawingData.getYData();

		// height of one chart graph including the slider bar
		final int devChartHeight = getDevChartHeightWithoutTrim();

		int devGraphHeight = devChartHeight;

		// adjust graph device height for stacked graphs, a gap is between two
		// graphs
		if (_chartDataModel.isStackedChart() && graphCount > 1) {
			final int devGraphHeightSpace = (devGraphHeight - (_chartsVerticalDistance * (graphCount - 1)));
			devGraphHeight = (devGraphHeightSpace / graphCount);
		}

		// enforce minimum chart height
		devGraphHeight = Math.max(devGraphHeight, CHART_MIN_HEIGHT);

		// remove slider bar from graph height
		devGraphHeight -= _devSliderBarHeight;

		/*
		 * all variables starting with graph... contain data values from the graph which are not
		 * scaled to the device
		 */

		final int unitType = yData.getAxisUnit();
		int graphMinValue = (int) yData.getVisibleMinValue();
		int graphMaxValue = (int) yData.getVisibleMaxValue();

		// clip max value
		boolean isAdjustGraphUnit = false;
		if (unitType == ChartDataSerie.AXIS_UNIT_HOUR_MINUTE_24H && (graphMaxValue / 3600 > 24)) {
			graphMaxValue = 24 * 3600;
			isAdjustGraphUnit = true;
		}

		int graphValueRange = graphMaxValue > 0 ? (graphMaxValue - graphMinValue) : -(graphMinValue - graphMaxValue);

		/*
		 * calculate the number of units which will be visible by dividing the available height by
		 * the minimum size which one unit should have in pixels
		 */
		final float defaultUnitCount = devGraphHeight / _chart.gridVerticalDistance;

		// unitValue is the number in data values for one unit
		final float defaultUnitValue = graphValueRange / Math.max(1, defaultUnitCount);

		// round the unit
		long graphUnit = (long) Util.roundTimeValue(
				defaultUnitValue,
				unitType == ChartDataSerie.AXIS_UNIT_HOUR_MINUTE_24H);

		long adjustMinValue = 0;
		if ((graphMinValue % graphUnit) != 0 && graphMinValue < 0) {
			adjustMinValue = graphUnit;
		}
		graphMinValue = (int) ((int) ((graphMinValue - adjustMinValue) / graphUnit) * graphUnit);

		// adjust the min value so that bar graphs start at the bottom of the chart
		if (_chartDataModel.getChartType() == ChartDataModel.CHART_TYPE_BAR && _chart.getStartAtChartBottom()) {
			yData.setVisibleMinValue(graphMinValue);
		}

		// increase the max value when it does not fit to unit borders
		float adjustMaxValue = 0;
		if ((graphMaxValue % graphUnit) != 0) {
			adjustMaxValue = graphUnit;
		}
		graphMaxValue = (int) ((int) ((graphMaxValue + adjustMaxValue) / graphUnit) * graphUnit);

		if (isAdjustGraphUnit || unitType == ChartDataSerie.AXIS_UNIT_HOUR_MINUTE_24H && (graphMaxValue / 3600 > 24)) {

			// max value exeeds 24h

			// count number of units
			int unitCounter = 0;
			int graphValue = graphMinValue;
			while (graphValue <= graphMaxValue) {

				// prevent endless loops when the unit is 0
				if (graphValue == graphMaxValue) {
					break;
				}
				unitCounter++;
				graphValue += graphUnit;
			}

			// adjust to 24h
			graphMaxValue = Math.min(24 * 3600, ((((int) yData.getVisibleMaxValue()) / 3600) * 3600) + 3600);

			// adjust to the whole hour
			graphMinValue = Math.max(0, ((((int) yData.getVisibleMinValue() / 3600) * 3600)));

			graphUnit = (graphMaxValue - graphMinValue) / unitCounter;
			graphUnit = (long) Util.roundTimeValue(graphUnit, unitType == ChartDataSerie.AXIS_UNIT_HOUR_MINUTE_24H);
		}

		graphValueRange = graphMaxValue > 0 ? //
				(graphMaxValue - graphMinValue)
				: -(graphMinValue - graphMaxValue);

		// ensure the chart is drawn correctly with pseudo data
		if (graphValueRange == 0) {
			graphValueRange = 3600;
			graphMaxValue = 3600;
			graphUnit = 1800;
		}

		final float majorValue = Util.getMajorTimeValue(//
				graphUnit,
				unitType == ChartDataSerie.AXIS_UNIT_HOUR_MINUTE_24H);

		// calculate the vertical scaling between graph and device
		final float graphScaleY = (float) (devGraphHeight) / graphValueRange;

		// calculate the vertical device offset
		int devYTop = _devMarginTop + _devXTitleBarHeight;

		if (_chartDataModel.isStackedChart()) {
			// each chart has its own drawing rectangle which are stacked on
			// top of each other
			devYTop += (currentGraph * (devGraphHeight + _devSliderBarHeight))
					+ ((currentGraph - 1) * _chartsVerticalDistance);

		} else {
			// all charts are drawn on the same rectangle
			devYTop += devGraphHeight;
		}

		drawingData.setScaleY(graphScaleY);

		drawingData.setDevYBottom(devYTop);
		drawingData.setDevYTop(devYTop - devGraphHeight);

		drawingData.setGraphYBottom(graphMinValue);
		drawingData.setGraphYTop(graphMaxValue);

		drawingData.devGraphHeight = devGraphHeight;
		drawingData.setDevSliderHeight(_devSliderBarHeight);

		final ArrayList<ChartUnit> unitList = drawingData.getYUnits();
		int graphValue = graphMinValue;
		int maxUnits = 0;
		final int valueDivisor = yData.getValueDivisor();

		// loop: create unit label for all units
		while (graphValue <= graphMaxValue) {

			final String unitLabel = Util.formatValue(graphValue, unitType, valueDivisor, false);
			final boolean isMajorValue = graphValue % majorValue == 0;

			unitList.add(new ChartUnit(graphValue, unitLabel, isMajorValue));

			// prevent endless loops when the unit is 0
			if (graphValue == graphMaxValue || maxUnits++ > 1000) {
				break;
			}

			graphValue += graphUnit;
		}

		if (unitType == ChartDataSerie.AXIS_UNIT_HOUR_MINUTE_24H && graphValue > graphMaxValue) {
			unitList.add(new ChartUnit(graphMaxValue, Util.EMPTY_STRING));
		}

	}

	private void createMonthEqualUnits(	final GraphDrawingData drawingData,
										final ArrayList<ChartUnit> units,
										final int devGraphWidth,
										final int allUnitsSize,
										final int numberOfYears) {

		final boolean isDrawUnits[] = new boolean[allUnitsSize];

		// check if month label width is set
		if (_devYearEqualMonthsWidth == -1) {
			getMonthLabelWidth();
		}

		/*
		 * create month labels depending on the available width for a unit
		 */
		final int devYearWidth = devGraphWidth / numberOfYears;
		if (devYearWidth >= _devYearEqualMonthsWidth) {

			// all month labels can be displayed

			for (int monthIndex = 0; monthIndex < allUnitsSize; monthIndex++) {
				units.add(new ChartUnit(monthIndex, _monthLabels[monthIndex % 12]));
				isDrawUnits[monthIndex] = true;
			}

		} else if (devYearWidth >= _devYearEqualMonthsWidth / 3) {

			// every second month label can be displayed

			for (int monthIndex = 0; monthIndex < allUnitsSize; monthIndex++) {

				final String monthLabel = monthIndex % 3 == 0 //
						? _monthLabels[monthIndex % 12]
						: UI.EMPTY_STRING;

				units.add(new ChartUnit(monthIndex, monthLabel));
				isDrawUnits[monthIndex] = monthIndex % 3 == 0;
			}

		} else if (devYearWidth >= _devYearEqualMonthsShortWidth / 3) {

			// every second month short label can be displayed

			for (int monthIndex = 0; monthIndex < allUnitsSize; monthIndex++) {

				final String monthLabel = monthIndex % 3 == 0 //
						? _monthShortLabels[monthIndex % 12]
						: UI.EMPTY_STRING;

				units.add(new ChartUnit(monthIndex, monthLabel));
				isDrawUnits[monthIndex] = monthIndex % 3 == 0;
			}

		} else if (devYearWidth >= _devYearEqualMonthsShortWidth / 6) {

			// every second month short label can be displayed

			for (int monthIndex = 0; monthIndex < allUnitsSize; monthIndex++) {

				final String monthLabel = monthIndex % 6 == 0 //
						? _monthShortLabels[monthIndex % 12]
						: UI.EMPTY_STRING;

				units.add(new ChartUnit(monthIndex, monthLabel));
				isDrawUnits[monthIndex] = monthIndex % 3 == 0;
			}

		} else {

			// width is too small to display month labels, display nothing

			for (int monthIndex = 0; monthIndex < allUnitsSize; monthIndex++) {
				units.add(new ChartUnit(monthIndex, UI.EMPTY_STRING));
			}
		}

		// set state that the overlap is not checked again
		drawingData.setIsXUnitOverlapChecked(true);

		drawingData.setIsDrawUnit(isDrawUnits);
	}

	/**
	 * Create the labels for the months by using the days to scale the x-axis
	 * 
	 * @param drawingData
	 * @param units
	 * @param devGraphWidth
	 * @param years
	 * @param yearDays
	 *            Number of days in one year
	 */
	private void createMonthUnequalUnits(	final GraphDrawingData drawingData,
											final ArrayList<ChartUnit> units,
											final int devGraphWidth,
											final int[] years,
											final int[] yearDays) {

		/*
		 * multiple years can have different number of days but we assume that each year has the
		 * same number of days to make it simpler
		 */

		final int numberOfYears = years.length;
		final int numberOfMonths = numberOfYears * 12; // number of units

		final boolean isDrawUnits[] = new boolean[numberOfMonths];

		// check if month label width is set
		if (_devYearEqualMonthsWidth == -1) {
			getMonthLabelWidth();
		}

		/*
		 * create list with the day number for all years and months
		 */
		final int[] daysForAllUnits = new int[numberOfMonths];
		int allDays = 0;
		for (int yearIndex = 0; yearIndex < numberOfYears; yearIndex++) {

			// create month units
			for (int monthIndex = 0; monthIndex < 12; monthIndex++) {

				_calendar.set(years[yearIndex], monthIndex, 1);
				final int firstDayInMonth = _calendar.get(Calendar.DAY_OF_YEAR) - 1;

				final int unitIndex = yearIndex * 12 + monthIndex;
				daysForAllUnits[unitIndex] = allDays + firstDayInMonth;
			}

			allDays += yearDays[yearIndex];
		}

		/*
		 * create month labels depending on the available width for a unit
		 */
		final int devYearWidth = devGraphWidth / numberOfYears;
		if (devYearWidth >= _devYearEqualMonthsWidth) {

			// all month labels can be displayed

			for (int monthIndex = 0; monthIndex < numberOfMonths; monthIndex++) {

				final int unitValue = daysForAllUnits[monthIndex];

				units.add(new ChartUnit(unitValue, _monthLabels[monthIndex % 12]));

				isDrawUnits[monthIndex] = true;
			}

		} else if (devYearWidth >= _devYearEqualMonthsWidth / 3) {

			// every second month label can be displayed

			for (int monthIndex = 0; monthIndex < numberOfMonths; monthIndex++) {

				final String monthLabel = monthIndex % 3 == 0 //
						? _monthLabels[monthIndex % 12]
						: UI.EMPTY_STRING;

				final int unitValue = daysForAllUnits[monthIndex];

				units.add(new ChartUnit(unitValue, monthLabel));

//				isDrawUnits[monthIndex] = monthIndex % 3 == 0;
				isDrawUnits[monthIndex] = true;
			}

		} else if (devYearWidth >= _devYearEqualMonthsShortWidth / 3) {

			// every second month short label can be displayed

			for (int monthIndex = 0; monthIndex < numberOfMonths; monthIndex++) {

				final String monthLabel = monthIndex % 3 == 0 //
						? _monthShortLabels[monthIndex % 12]
						: UI.EMPTY_STRING;

				units.add(new ChartUnit(daysForAllUnits[monthIndex], monthLabel));
				isDrawUnits[monthIndex] = monthIndex % 3 == 0;
			}

		} else if (devYearWidth >= _devYearEqualMonthsShortWidth / 6) {

			// every second month short label can be displayed

			for (int monthIndex = 0; monthIndex < numberOfMonths; monthIndex++) {

				final String monthLabel = monthIndex % 6 == 0 //
						? _monthShortLabels[monthIndex % 12]
						: UI.EMPTY_STRING;

				units.add(new ChartUnit(daysForAllUnits[monthIndex], monthLabel));
				isDrawUnits[monthIndex] = monthIndex % 3 == 0;
			}

		} else {

			// width is too small to display month labels, display nothing

			for (int monthIndex = 0; monthIndex < numberOfMonths; monthIndex++) {
				units.add(new ChartUnit(daysForAllUnits[monthIndex], UI.EMPTY_STRING));
			}
		}

		// set state that the overlap is not checked again
		drawingData.setIsXUnitOverlapChecked(true);

		drawingData.setIsDrawUnit(isDrawUnits);

//		// shorten the unit when there is not enough space to draw the full unit name
//		final GC gc = new GC(this);
//		final int monthLength = gc.stringExtent(_monthLabels[0]).x;
//		final boolean useShortUnitLabel = monthLength > (devGraphWidth / allUnitsSize) * 0.9;
//		gc.dispose();
//
//		/*
//		 * create month units for all years
//		 */
//		for (int yearIndex = 0; yearIndex < years.length; yearIndex++) {
//
//			final int year = years[yearIndex];
//
//			// create month units
//			for (int month = 0; month < 12; month++) {
//
//				_calendar.set(year, month, 1);
//				final int firstDayInMonth = _calendar.get(Calendar.DAY_OF_YEAR) - 1;
//
//				String monthLabel = _monthLabels[month];
//				if (useShortUnitLabel) {
//					monthLabel = monthLabel.substring(0, 1);
//				}
//
//				units.add(new ChartUnit(allDays + firstDayInMonth, monthLabel));
//			}
//
//			allDays += yearDays[yearIndex];
//		}
	}

	/**
	 * set the {@link SynchConfiguration} when this chart is the source for the synched chart
	 */
	private SynchConfiguration createSynchConfig() {

		final ChartDataXSerie xData = _chartDataModel.getXData();

		final int markerValueIndexStart = xData.getSynchMarkerStartIndex();
		final int markerValueIndexEnd = xData.getSynchMarkerEndIndex();

		if (markerValueIndexStart == -1) {

			// disable chart synchronization
			_synchConfigOut = null;
			return null;
		}

		/*
		 * create synch configuration data
		 */

		final float[] xValues = xData.getHighValues()[0];
		final float markerStartValue = xValues[Math.min(markerValueIndexStart, xValues.length - 1)];
		final float markerEndValue = xValues[Math.min(markerValueIndexEnd, xValues.length - 1)];

		final float valueDiff = markerEndValue - markerStartValue;
		final float lastValue = xValues[xValues.length - 1];

		final float devVirtualGraphImageWidth = componentGraph.getXXDevGraphWidth();
		final float devGraphImageXOffset = componentGraph.getXXDevViewPortLeftBorder();

		final float devOneValueSlice = devVirtualGraphImageWidth / lastValue;

		final float devMarkerWidth = (int) (valueDiff * devOneValueSlice);
		final float devMarkerStartPos = (int) (markerStartValue * devOneValueSlice);
		final float devMarkerOffset = (int) (devMarkerStartPos - devGraphImageXOffset);

		final int devVisibleChartWidth = getDevVisibleChartWidth();

		final float markerWidthRatio = devMarkerWidth / devVisibleChartWidth;
		final float markerOffsetRatio = devMarkerOffset / devVisibleChartWidth;

		// ---------------------------------------------------------------------------------------

		final SynchConfiguration synchConfig = new SynchConfiguration(
				_chartDataModel,
				devMarkerWidth,
				devMarkerOffset,
				markerWidthRatio,
				markerOffsetRatio);

		return synchConfig;
	}

	private void createXValuesDay(	final GraphDrawingData drawingData,
									final ArrayList<ChartUnit> units,
									final int devGraphWidth) {

		final ChartSegments chartSegments = drawingData.getXData().getChartSegments();

		createMonthUnequalUnits(drawingData, units, devGraphWidth, chartSegments.years, chartSegments.yearDays);

		// compute the width of the rectangles
		final int allDaysInAllYears = chartSegments.allValues;
		drawingData.setBarRectangleWidth(Math.max(0, (devGraphWidth / allDaysInAllYears)));
		drawingData.setXUnitTextPos(GraphDrawingData.X_UNIT_TEXT_POS_CENTER);

		drawingData.setScaleX((float) devGraphWidth / allDaysInAllYears);
	}

	private void createXValuesMonth(final GraphDrawingData drawingData,
									final ArrayList<ChartUnit> units,
									final int devGraphWidth) {

		final ChartDataXSerie xData = drawingData.getXData();

		final int allUnitsSize = xData._highValues[0].length;
		final float scaleX = (float) devGraphWidth / allUnitsSize;
		drawingData.setScaleX(scaleX);

		final int numberOfYears = xData.getChartSegments().segmentTitle.length;

		createMonthEqualUnits(drawingData, units, devGraphWidth, allUnitsSize, numberOfYears);

		// compute the width and position of the rectangles
		final float monthWidth = Math.max(0, (scaleX) - 1);
		final float barWidth = Math.max(0, (monthWidth * 0.90f));

		drawingData.setBarRectangleWidth((int) barWidth);
		drawingData.setDevBarRectangleXPos((int) (Math.max(0, (monthWidth - barWidth) / 2) + 1));

		drawingData.setXUnitTextPos(GraphDrawingData.X_UNIT_TEXT_POS_CENTER);
	}

	private void createXValuesWeek(	final GraphDrawingData drawingData,
									final ArrayList<ChartUnit> units,
									final int devGraphWidth) {

		final ChartDataXSerie xData = drawingData.getXData();

		final float[] xValues = xData.getHighValues()[0];
		final int allWeeks = xValues.length;
		final float devWeekWidth = devGraphWidth / allWeeks;

		final ChartSegments chartSegments = drawingData.getXData().getChartSegments();

		final int[] yearDays = chartSegments.yearDays;

		int allDaysInAllYears = 0;
		for (final int days : yearDays) {
			allDaysInAllYears += days;
		}

		createMonthUnequalUnits(drawingData, units, devGraphWidth, chartSegments.years, yearDays);

		final float barWidth = devWeekWidth * 0.7f;

		drawingData.setBarRectangleWidth((int) barWidth);
		drawingData.setDevBarRectangleXPos((int) (Math.max(0, (devWeekWidth - 2) / 2) + 1));

		drawingData.setBarPosition(GraphDrawingData.BAR_POS_CENTER);
		drawingData.setXUnitTextPos(GraphDrawingData.X_UNIT_TEXT_POS_CENTER);

		drawingData.setScaleX((float) devGraphWidth / allWeeks);
		drawingData.setScaleUnitX((float) devGraphWidth / allDaysInAllYears);

	}

	private void createXValuesYear(	final GraphDrawingData drawingData,
									final ArrayList<ChartUnit> units,
									final int devGraphWidth) {

		final ChartDataYSerie yData = drawingData.getYData();
		final ChartDataXSerie xData = drawingData.getXData();

		final ChartSegments chartSegments = drawingData.getXData().getChartSegments();
		final int[] yearValues = chartSegments.years;

		final int yearCounter = xData._highValues[0].length;
		final float scaleX = (float) devGraphWidth / yearCounter;
		drawingData.setScaleX(scaleX);

		// create year units
		for (int yearIndex = 0; yearIndex < yearValues.length; yearIndex++) {
			units.add(new ChartUnit(yearIndex, Integer.toString(yearValues[yearIndex])));
		}

		// compute the width and position of the rectangles
		int barWidth;
		final int yearWidth = (int) Math.max(0, scaleX - 1);

		switch (yData.getChartLayout()) {
		case ChartDataYSerie.BAR_LAYOUT_SINGLE_SERIE:
		case ChartDataYSerie.BAR_LAYOUT_STACKED:

			// the bar's width is 50% of the width for a month
			barWidth = (int) Math.max(0, (yearWidth * 0.9f));
			drawingData.setBarRectangleWidth(barWidth);
			drawingData.setDevBarRectangleXPos((Math.max(0, (yearWidth - barWidth) / 2) + 1));
			break;

		case ChartDataYSerie.BAR_LAYOUT_BESIDE:

			final int serieCount = yData.getHighValues().length;

			// the bar's width is 75% of the width for a month
			barWidth = (int) Math.max(0, yearWidth * 0.9f);
//			if (serieCount == 1) {
//
//				drawingData.setBarRectangleWidth(Math.max(1, barWidth));
////			drawingData.setDevBarRectangleXPos((int) (Math.max(0, (yearWidth - barWidth) / 2) + 2));
////			drawingData.setDevBarRectangleXPos(0);
//
//			} else {
			final int singleBarWidth = Math.max(1, barWidth / (serieCount - 0));
			drawingData.setBarRectangleWidth(singleBarWidth);
			final int barPosition = (yearWidth - (singleBarWidth * (serieCount - 0))) / 2;

			drawingData.setDevBarRectangleXPos((Math.max(0, barPosition) + 0));
//				drawingData.setDevBarRectangleXPos(0);
//			}

		default:
			break;
		}

		drawingData.setXUnitTextPos(GraphDrawingData.X_UNIT_TEXT_POS_CENTER);
	}

	ChartComponentAxis getAxisLeft() {
		return componentAxisLeft;
	}

	ChartComponentAxis getAxisRight() {
		return componentAxisRight;
	}

	ChartComponentGraph getChartComponentGraph() {
		return componentGraph;
	}

	ChartDataModel getChartDataModel() {
		return _chartDataModel;
	}

	ChartDrawingData getChartDrawingData() {
		return _chartDrawingData;
	}

	private int getDevChartHeightWithoutTrim() {

		return _visibleGraphRect.height //
				- _devMarginTop
				- _devXTitleBarHeight
				- _devXAxisHeight;
	}

	int getDevChartMarginBottom() {
		return _devXAxisHeight;
	}

	int getDevChartMarginTop() {
		return _devMarginTop + _devXTitleBarHeight + _devSliderBarHeight;
	}

	/**
	 * @return Returns the visible chart graph height
	 */
	int getDevVisibleChartHeight() {

		if (_visibleGraphRect == null) {
			return 100;
		}

		return _visibleGraphRect.height;
	}

	/**
	 * @return Returns the visible chart width
	 */
	int getDevVisibleChartWidth() {

		if (_visibleGraphRect == null) {
			return 100;
		}

		return _visibleGraphRect.width;
	}

	/**
	 * Get size in pixel for the month labels and shortcuts
	 */
	private void getMonthLabelWidth() {

		final GC gc = new GC(this);
		{
			_devYearEqualMonthsWidth = 0;
			_devYearEqualMonthsShortWidth = 0;

			for (int monthIndex = 0; monthIndex < _monthLabels.length; monthIndex++) {
				_devYearEqualMonthsWidth += gc.stringExtent(_monthLabels[monthIndex]).x + 6;
				_devYearEqualMonthsShortWidth += gc.stringExtent(_monthShortLabels[monthIndex]).x + 12;
			}
		}
		gc.dispose();
	}

	int getYAxisWidthLeft() {
		return getAxisLeft().getSize().x;
	}

	/**
	 * Resize handler for all components, computes the chart when the chart data or the client area
	 * has changed or when the chart was zoomed
	 */
	boolean onResize() {

		if (isDisposed() || _chartDataModel == null || getClientArea().width == 0) {
			return false;
		}

		// compute the visual size of the graph
		setVisibleGraphRect();

		if (setWidthToSynchedChart() == false) {

			// chart is not synchronized, compute the 'normal' graph width
			componentGraph.updateGraphSize();
		}

		// compute the chart data
		_chartDrawingData = createDrawingData();

		// notify components about the new configuration
		componentGraph.setDrawingData(_chartDrawingData);

		// resize the sliders after the drawing data have changed and the new
		// chart size is saved
		componentGraph.updateSlidersOnResize();

		// resize the axis
		componentAxisLeft.setDrawingData(_chartDrawingData, true);
		componentAxisRight.setDrawingData(_chartDrawingData, false);

		componentGraph.updateChartSize();

		// synchronize chart
		final SynchConfiguration synchConfig = createSynchConfig();
		if (synchConfig != null) {
			synchronizeChart(synchConfig);
		}

		return true;
	}

	void selectBarItem(final Event event) {

		_keyDownCounter[0]++;

		final int[] selectedIndex = new int[] { Chart.NO_BAR_SELECTION };

		switch (event.keyCode) {
		case SWT.ARROW_RIGHT:
			selectedIndex[0] = componentGraph.selectBarItemNext();
			break;
		case SWT.ARROW_LEFT:
			selectedIndex[0] = componentGraph.selectBarItemPrevious();
			break;
		}

		// fire the event when the selection has changed
		if (selectedIndex[0] != Chart.NO_BAR_SELECTION) {

			/*
			 * delay the change event when the key down was pressed several times
			 */
			final Display display = Display.getCurrent();
			display.asyncExec(new Runnable() {
				public void run() {
					display.timerExec(BAR_SELECTION_DELAY_TIME, new Runnable() {

						final int	__runnableKeyDownCounter	= _keyDownCounter[0];

						public void run() {
							if (__runnableKeyDownCounter == _keyDownCounter[0]
									&& __runnableKeyDownCounter != _lastKeyDownCounter[0]) {

								/*
								 * prevent redoing it, this happened when the selectNext/Previous
								 * Method took a long time when the chart was drawn
								 */
								_lastKeyDownCounter[0] = __runnableKeyDownCounter;

								_chart.fireBarSelectionEvent(0, selectedIndex[0]);
							}
						}
					});
				}
			});
		}
	}

	void setErrorMessage(final String errorMessage) {
		this.errorMessage = errorMessage;
	}

	/**
	 * set the chart data model and redraw the chart
	 * 
	 * @param chartModel
	 * @param isShowAllData
	 */
	void setModel(final ChartDataModel chartModel, final boolean isShowAllData) {

		_chartDataModel = chartModel;

		/*
		 * when data model has changed, update the visible y-values to use the full visible area for
		 * drawing the chart
		 */
		final int chartType = _chartDataModel.getChartType();
		if ((chartType == ChartDataModel.CHART_TYPE_LINE || chartType == ChartDataModel.CHART_TYPE_LINE_WITH_BARS)
				&& isShowAllData) {

			componentGraph.updateVisibleMinMaxValues();
		}

		if (onResize()) {
			/*
			 * resetting the sliders require that the drawing data are created, this is done in the
			 * onResize method
			 */
			if (_devSliderBarHeight > 0) {
				componentGraph.resetSliders();
			}
		}
	}

	/**
	 * @param isSliderVisible
	 */
	void setSliderVisible(final boolean isSliderVisible) {

		_devSliderBarHeight = isSliderVisible ? SLIDER_BAR_HEIGHT : 0;

		componentGraph.setXSliderVisible(isSliderVisible);
	}

	/**
	 * Set's a {@link SynchConfiguration}, this chart will then be sychronized with the chart which
	 * sets the synch config
	 * 
	 * @param fSynchConfigSrc
	 *            the xMarkerPosition to set
	 */
	void setSynchConfig(final SynchConfiguration synchConfigIn) {

		_synchConfigSrc = synchConfigIn;

		onResize();
	}

	private void setVisibleGraphRect() {

		final ArrayList<ChartDataYSerie> yDataList = _chartDataModel.getYData();
		boolean isYTitle = false;

		// loop all graphs - find the title for the y-axis
		for (final ChartDataYSerie yData : yDataList) {
			if (yData.getYTitle() != null || yData.getUnitLabel() != null) {
				isYTitle = true;
				break;
			}
		}

		if (isYTitle) {

			_yAxisWidthLeftWithTitle = _yAxisWidthLeft + TITLE_BAR_HEIGHT;

			final GridData gl = (GridData) componentAxisLeft.getLayoutData();
			gl.widthHint = _yAxisWidthLeftWithTitle;

			// relayout after the layout size has been changed
			layout();
		}

		// set the visible graph size
		final Rectangle clientRect = getClientArea();
		final int devGraphWidth = clientRect.width - (_yAxisWidthLeftWithTitle + _yAxisWidthRight) - 0;

		_visibleGraphRect = new Rectangle(_yAxisWidthLeftWithTitle, 0, devGraphWidth, clientRect.height);
	}

	/**
	 * adjust the graph width to the synched chart
	 * 
	 * @return Returns <code>true</code> when the graph width was set
	 */
	private boolean setWidthToSynchedChart() {

		final ChartDataXSerie xData = _chartDataModel.getXData();
		final int markerStartIndex = xData.getSynchMarkerStartIndex();
		final int markerEndIndex = xData.getSynchMarkerEndIndex();

		// check if synchronization is disabled
		if (_synchConfigSrc == null || markerStartIndex == -1) {
			return false;
		}

		// set min/max values from the source synched chart into this chart
		_synchConfigSrc.getYDataMinMaxKeeper().setMinMaxValues(_chartDataModel);

		final float[] xValues = xData.getHighValues()[0];
		final float markerValueStart = xValues[markerStartIndex];

		final float valueDiff = xValues[markerEndIndex] - markerValueStart;
		final float valueLast = xValues[xValues.length - 1];

		final int devVisibleChartWidth = getDevVisibleChartWidth();

		final float xxDevGraphWidth;
		final int xxDevViewPortOffset;
		final float graphZoomRatio;

		switch (_chart._synchMode) {
		case Chart.SYNCH_MODE_BY_SCALE:

			// get marker data from the synch source
			final float markerWidthRatio = _synchConfigSrc.getMarkerWidthRatio();
			final float markerOffsetRatio = _synchConfigSrc.getMarkerOffsetRatio();

			// virtual graph width
			final float devMarkerWidth = devVisibleChartWidth * markerWidthRatio;
			final float devOneValueSlice = devMarkerWidth / valueDiff;
			xxDevGraphWidth = devOneValueSlice * valueLast;

			// graph offset
			final float devMarkerOffset = devVisibleChartWidth * markerOffsetRatio;
			final float devMarkerStart = devOneValueSlice * markerValueStart;
			xxDevViewPortOffset = (int) (devMarkerStart - devMarkerOffset);

			// zoom ratio
			graphZoomRatio = xxDevGraphWidth / devVisibleChartWidth;

			componentGraph.setGraphSize((int) xxDevGraphWidth, xxDevViewPortOffset, graphZoomRatio);

			return true;

		case Chart.SYNCH_MODE_BY_SIZE:

			// get marker data from the synch source
			final float synchSrcDevMarkerWidth = _synchConfigSrc.getDevMarkerWidth();
			final float synchSrcDevMarkerOffset = _synchConfigSrc.getDevMarkerOffset();

			// virtual graph width
			xxDevGraphWidth = valueLast / valueDiff * synchSrcDevMarkerWidth;

			// graph offset
			final int devLeftSynchMarkerPos = (int) (markerValueStart / valueLast * xxDevGraphWidth);
			xxDevViewPortOffset = (int) (devLeftSynchMarkerPos - synchSrcDevMarkerOffset);

			// zoom ratio
			graphZoomRatio = xxDevGraphWidth / devVisibleChartWidth;

			componentGraph.setGraphSize((int) xxDevGraphWidth, xxDevViewPortOffset, graphZoomRatio);

			return true;

		default:
			break;
		}

		return false;
	}

	/**
	 * set the x-sliders to a new position, this is done from a selection provider
	 * 
	 * @param sliderPosition
	 */
	void setXSliderPosition(final SelectionChartXSliderPosition sliderPosition) {

		if (sliderPosition == null) {
			/*
			 * nothing to do when the position was not set, this can happen when the chart was not
			 * yet created
			 */
			return;
		}

		if (_chartDataModel == null) {
			return;
		}

		final ChartXSlider leftSlider = componentGraph.getLeftSlider();
		final ChartXSlider rightSlider = componentGraph.getRightSlider();

		final int slider0ValueIndex = sliderPosition.getBeforeLeftSliderIndex();
		final int slider1ValueIndex = sliderPosition.getLeftSliderValueIndex();
		final int slider2ValueIndex = sliderPosition.getRightSliderValueIndex();

		final float[] xValues = _chartDataModel.getXData()._highValues[0];
		final boolean isCenterSliderPosition = sliderPosition.isCenterSliderPosition();

		// move left slider
		if (slider1ValueIndex == SelectionChartXSliderPosition.SLIDER_POSITION_AT_CHART_BORDER) {

			componentGraph.setXSliderValueIndex(//
					leftSlider,
					0,
					isCenterSliderPosition);

		} else if (slider1ValueIndex != SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION) {

			componentGraph.setXSliderValueIndex(//
					leftSlider,
					slider1ValueIndex,
					isCenterSliderPosition);
		}

		if (slider0ValueIndex != SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION) {

			// move second slider before the first slider

			componentGraph.setXSliderValueIndex(rightSlider, slider0ValueIndex, isCenterSliderPosition);

		} else {

			// move right slider
			if (slider2ValueIndex == SelectionChartXSliderPosition.SLIDER_POSITION_AT_CHART_BORDER) {

				componentGraph.setXSliderValueIndex(//
						rightSlider,
						xValues.length - 1,
						isCenterSliderPosition);

			} else if (slider2ValueIndex != SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION) {

				componentGraph.setXSliderValueIndex(//
						rightSlider,
						slider2ValueIndex,
						isCenterSliderPosition);
			}
		}

		componentGraph.redraw();
	}

	private void synchronizeChart(final SynchConfiguration newSynchConfigOut) {

		boolean fireEvent = false;

		if (_synchConfigOut == null) {
			// synch new config
			fireEvent = true;
		} else if (_synchConfigOut.isEqual(newSynchConfigOut) == false) {
			// synch when config changed
			fireEvent = true;
		}

		if (fireEvent) {

			// set new synch config
			_synchConfigOut = newSynchConfigOut;

			_chart.synchronizeChart();
		}
	}

	void updateCustomLayers() {

		if (_chartDrawingData == null) {
			return;
		}

		int graphIndex = 0;
		final ArrayList<GraphDrawingData> graphDrawingData = _chartDrawingData.graphDrawingData;

		/*
		 * set custom layers from the data model into the drawing data
		 */

		for (final ChartDataYSerie yData : _chartDataModel.getYData()) {
			graphDrawingData.get(graphIndex++).getYData().setCustomForegroundLayers(yData.getCustomForegroundLayers());
		}

		componentGraph.updateCustomLayers();
	}

}
