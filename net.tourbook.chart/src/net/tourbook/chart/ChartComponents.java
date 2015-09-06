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
package net.tourbook.chart;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import net.tourbook.common.UI;
import net.tourbook.common.util.Util;

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
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Chart widget which represents the chart UI.
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

	public static final int			BAR_SELECTION_DELAY_TIME	= 100;

	/**
	 * min/max pixel widthDev/heightDev of the chart
	 */
	static final int				CHART_MIN_WIDTH				= 5;
	static final int				CHART_MIN_HEIGHT			= 5;

//	static final int				CHART_MAX_WIDTH				= Integer.MAX_VALUE;				// 2'147'483'647
//	static final int				CHART_MAX_WIDTH				= 1000000000;						// 1'000'000'000
	static final long				CHART_MAX_WIDTH				= 1000000000000L;					// 1'000'000'000'000
//																									//   308'333'095
	static final int				CHART_MAX_HEIGHT			= 10000;

	static final int				SLIDER_BAR_HEIGHT			= 10;
	static final int				TITLE_BAR_HEIGHT			= 18;								//15;
	static final int				MARGIN_TOP_WITH_TITLE		= 5;
	static final int				MARGIN_TOP_WITHOUT_TITLE	= 10;

	/**
	 * Number of seconds in one day.
	 */
	private static final int		DAY_IN_SECONDS				= 24 * 60 * 60;

	private static final int		YEAR_IN_SECONDS				= 366 * DAY_IN_SECONDS;

	private static final int		MONTH_IN_SECONDS			= 31 * DAY_IN_SECONDS;

	private final Chart				_chart;

	/**
	 * top margin of the chart (and all it's components)
	 */
	private int						_devMarginTop				= MARGIN_TOP_WITHOUT_TITLE;

	/**
	 * height of the slider bar, 0 indicates that the slider is not visible
	 */
	int								_devSliderBarHeight			= 0;

	/**
	 * height of the title bar, 0 indicates that the title is not visible
	 */
	private int						_devXTitleBarHeight			= 0;

	/**
	 * height of the horizontal axis
	 */
	private final int				_devXAxisHeight				= 25;

	/**
	 * width of the vertical axis
	 */
	private final int				_yAxisWidthLeft				= 50;
	private int						_yAxisWidthLeftWithTitle	= _yAxisWidthLeft;
	private final int				_yAxisWidthRight			= 50;

	/**
	 * vertical distance between two graphs
	 */
	private final int				_chartsVerticalDistance		= 15;

	/**
	 * contains the {@link SynchConfiguration} for the current chart and will be used from the chart
	 * which is synchronized
	 */
	SynchConfiguration				_synchConfigOut				= null;

	/**
	 * when a {@link SynchConfiguration} is set, this chart will be synchronized with the chart
	 * which set's the synch config
	 */
	SynchConfiguration				_synchConfigSrc				= null;

	/**
	 * visible chart rectangle
	 */
	private Rectangle				_visibleGraphRect;

	final ChartComponentGraph		componentGraph;
	final ChartComponentAxis		componentAxisLeft;
	final ChartComponentAxis		componentAxisRight;

	private ChartDataModel			_chartDataModel				= null;

	private ChartDrawingData		_chartDrawingData;

	private static final String		_monthLabels[]				= {
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

	private static final String		_monthShortLabels[]			= {
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
	private int						_devAllMonthLabelWidth		= -1;
	private int						_devAllMonthShortLabelWidth	= -1;
	private int						_devYearLabelWidth;

	private final int[]				_keyDownCounter				= new int[1];
	private final int[]				_lastKeyDownCounter			= new int[1];

	private final Calendar			_calendar					= GregorianCalendar.getInstance();

	/**
	 * this error message is displayed instead of the chart when it's not <code>null</code>
	 */
	String							errorMessage;

	private final DateTimeFormatter	_dtFormatter				= DateTimeFormat.forStyle("M-");	//$NON-NLS-1$

	private long					_historyUnitStart;
	private long					_historyUnitDuration;

	private int[]					_historyYears;

	/**
	 * Contains number of days for each month
	 */
	private int[][]					_historyMonths;
	private int[]					_historyDOY;

	/**
	 * Create and layout the components of the chart
	 * 
	 * @param parent
	 * @param style
	 */
	ChartComponents(final Chart parent, final int style) {

		super(parent, style);

		_chart = parent;

		GridData gd;

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

		getMonthLabelWidth();
	}

	private void addListener() {

		// this is the only resize listener for the whole chart
		addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent event) {
				onResize();
			}
		});
	}

	/**
	 * Computes all the data for the chart
	 * 
	 * @return chart drawing data
	 */
	private ChartDrawingData createDrawingData() {

		// compute the graphs and axis
		final ArrayList<GraphDrawingData> allGraphDrawingData = new ArrayList<GraphDrawingData>();

		final ChartDrawingData chartDrawingData = new ChartDrawingData(allGraphDrawingData);

		chartDrawingData.chartDataModel = _chartDataModel;

		final ArrayList<ChartDataYSerie> yDataList = _chartDataModel.getYData();
		final ChartDataXSerie xData = _chartDataModel.getXData();
		final ChartDataXSerie xData2nd = _chartDataModel.getXData2nd();

		final int graphCount = yDataList.size();
		int graphIndex = 1;

		// loop all graphs
		for (final ChartDataYSerie yData : yDataList) {

			final GraphDrawingData graphDrawingData = new GraphDrawingData(chartDrawingData, yData.getChartType());

			allGraphDrawingData.add(graphDrawingData);

			// set chart title above the first graph
			if (graphIndex == 1) {

				graphDrawingData.setXTitle(_chartDataModel.getTitle());

				// set the chart title height and margin
				final String title = graphDrawingData.getXTitle();
				final ChartStatisticSegments chartSegments = xData.getChartSegments();

				if (title != null && title.length() > 0 || //
						(chartSegments != null && chartSegments.segmentTitle != null)) {

					_devXTitleBarHeight = TITLE_BAR_HEIGHT;
					_devMarginTop = MARGIN_TOP_WITH_TITLE;
				}
			}

			// set x/y data
			graphDrawingData.setXData(xData);
			graphDrawingData.setXData2nd(xData2nd);
			graphDrawingData.setYData(yData);

			// compute x/y values
			createDrawingData_X(graphDrawingData);
			createDrawingData_Y(graphDrawingData, graphCount, graphIndex);

			// reset adjusted y-slider value
			yData.adjustedYValue = Float.MIN_VALUE;

			graphIndex++;
		}

		// set values after they have been computed
		chartDrawingData.devMarginTop = _devMarginTop;
		chartDrawingData.devXTitelBarHeight = _devXTitleBarHeight;
		chartDrawingData.devSliderBarHeight = _devSliderBarHeight;
		chartDrawingData.devXAxisHeight = _devXAxisHeight;
		chartDrawingData.devVisibleChartWidth = getDevVisibleChartWidth();

		return chartDrawingData;
	}

	/**
	 * Compute units for the x-axis and keep it in the drawingData object
	 */
	private void createDrawingData_X(final GraphDrawingData drawingData) {

		final ChartDataXSerie xData = drawingData.getXData();

		final double graphMaxValue = xData.getOriginalMaxValue();
		final long devVirtualGraphWidth = componentGraph.getXXDevGraphWidth();

		drawingData.devVirtualGraphWidth = devVirtualGraphWidth;

//		final double scaleX = ((double) devVirtualGraphWidth - 1) / graphMaxValue;
		final double scaleX = (devVirtualGraphWidth) / graphMaxValue;
		drawingData.setScaleX(scaleX);

		/*
		 * calculate the number of units which will be visible by dividing the visible length by the
		 * minimum size which one unit should have in pixels
		 */
		final long defaultUnitCount = devVirtualGraphWidth / _chart.gridHorizontalDistance;

		// unit raw value (not yet rounded) is the number in data values for one unit
		final double graphDefaultUnit = graphMaxValue / Math.max(1, defaultUnitCount);

		final int unitType = xData.getAxisUnit();
		switch (unitType) {

		case ChartDataSerie.X_AXIS_UNIT_DAY:

			createDrawingData_X_Day(drawingData);
			break;

		case ChartDataSerie.X_AXIS_UNIT_WEEK:

			createDrawingData_X_Week(drawingData);
			break;

		case ChartDataSerie.X_AXIS_UNIT_MONTH:

			createDrawingData_X_Month(drawingData);
			break;

		case ChartDataSerie.X_AXIS_UNIT_YEAR:

			createDrawingData_X_Year(drawingData);
			break;

		case ChartDataSerie.X_AXIS_UNIT_HISTORY:

			createDrawingData_X_History(drawingData, graphDefaultUnit);
			break;

		default:

			// get the unit list from the configuration
			final ArrayList<ChartUnit> xUnits = drawingData.getXUnits();

			// axis unit
			double graphUnit = 1; // this default value should be overwritten
			double majorValue = 0;

			switch (unitType) {
			case ChartDataSerie.AXIS_UNIT_HOUR_MINUTE_SECOND:
			case ChartDataSerie.AXIS_UNIT_HOUR_MINUTE_OPTIONAL_SECOND:
			case ChartDataSerie.AXIS_UNIT_HOUR_MINUTE:
				graphUnit = Util.roundTimeValue((long) graphDefaultUnit, false);
				majorValue = Util.getMajorTimeValue((long) graphUnit, false);
				break;

			case ChartDataSerie.AXIS_UNIT_NUMBER:
			case ChartDataSerie.X_AXIS_UNIT_NUMBER_CENTER:
				// unit is a decimal number
				graphUnit = Util.roundDecimalValue(graphDefaultUnit);
				majorValue = Util.getMajorDecimalValue(graphUnit);
				break;

			default:
				break;
			}

			/*
			 * create units for the x-axis
			 */

			// get the unitOffset when a startValue is set
			final double xStartValue = xData.getStartValue();
			double unitOffset = 0;
			if (xStartValue != 0) {
				unitOffset = xStartValue % graphUnit;
			}

			final int valueDivisor = xData.getValueDivisor();

			/**
			 * This implementation do NOT support extended scaling (logarithmic scaling)
			 */

			/*
			 * increase by one unit that the right side of the chart is drawing a unit, in some
			 * cases this didn't occured
			 */
			double graphMinVisibleValue = xData.getVisibleMinValue();
			double graphMaxVisibleValue = xData.getVisibleMaxValue() + graphUnit;

			// decrease min value when it does not fit to unit borders
			final double graphMinRemainder = graphMinVisibleValue % graphUnit;
			graphMinVisibleValue = graphMinVisibleValue - graphMinRemainder;

			graphMinVisibleValue = Util.roundValueToUnit(graphMinVisibleValue, graphUnit, true);
			graphMaxVisibleValue = Util.roundValueToUnit(graphMaxVisibleValue, graphUnit, false);

			int loopCounter = 0;
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

//				String unitLabel = net.tourbook.chart.Util.formatValue(
//						(float) unitLabelValue,
//						unitType,
//						valueDivisor,
//						true);

				final int valueDecimals = 3;
				final String unitLabel = net.tourbook.chart.Util.formatNumber(
						unitLabelValue,
						unitType,
						valueDivisor,
						valueDecimals);

				final boolean isMajorValue = unitLabelValue % majorValue == 0;

				xUnits.add(new ChartUnit(unitPos, unitLabel, isMajorValue));

				// check for an infinity loop
				if (graphValue == graphMaxValue || loopCounter++ > 10000) {
					break;
				}

				graphValue += graphUnit;
				graphValue = Util.roundValueToUnit(graphValue, graphUnit, false);
			}

			break;
		}

		/*
		 * configure bars in the bar charts
		 */
		if (_chartDataModel.getChartType() == ChartType.BAR) {

			final double[] highValues = xData.getHighValuesDouble()[0];

			if (unitType == ChartDataSerie.AXIS_UNIT_NUMBER || unitType == ChartDataSerie.AXIS_UNIT_HOUR_MINUTE) {

				final int barWidth = (int) ((devVirtualGraphWidth / highValues.length) / 2);

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

	private void createDrawingData_X_Day(final GraphDrawingData drawingData) {

		final ChartStatisticSegments chartSegments = drawingData.getXData().getChartSegments();
		final long devVirtualGraphWidth = componentGraph.getXXDevGraphWidth();

		createMonthUnequalUnits(drawingData, devVirtualGraphWidth, chartSegments.years, chartSegments.yearDays);

		// compute the width of the rectangles
		final int allDaysInAllYears = chartSegments.allValues;
		drawingData.setBarRectangleWidth((int) Math.max(0, (devVirtualGraphWidth / allDaysInAllYears)));
		drawingData.setXUnitTextPos(GraphDrawingData.X_UNIT_TEXT_POS_CENTER);

		drawingData.setScaleX((double) devVirtualGraphWidth / allDaysInAllYears);
	}

	private void createDrawingData_X_History(final GraphDrawingData graphDrawingData, final double graphDefaultUnitD) {

		final ChartDataXSerie xData = graphDrawingData.getXData();
		final double scaleX = graphDrawingData.getScaleX();

		final long graphMaxValue = (long) xData.getOriginalMaxValue();
		final long graphDefaultUnit = (long) graphDefaultUnitD;

		// get start time without mills
		final DateTime tourStartTime = xData.getStartDateTime().minus(xData.getStartDateTime().getMillisOfSecond());
		final DateTime tourEndTime = tourStartTime.plus(graphMaxValue * 1000);

		long unitStart = tourStartTime.getMillis();
		long unitEnd = graphMaxValue;
		long firstUnitYear = tourStartTime.getYear();
		long lastUnitYear = tourEndTime.getYear();

		long roundedYearUnit = 0;
		long majorRoundedYearUnit = 0;
		final double dev1Year = scaleX * YEAR_IN_SECONDS;
		final double dev1Month = scaleX * MONTH_IN_SECONDS;

//		System.out.println(UI.timeStampNano() + " \t");
//		System.out.println(UI.timeStampNano() + " createDrawingData_X_History\t" + " start: " + tourStartTime);
//		// TODO remove SYSTEM.OUT.PRINTLN

		final double devTitleVisibleUnit = _devAllMonthLabelWidth * 1.2;

		final boolean isYearRounded = dev1Year < _devYearLabelWidth * 4;
		if (isYearRounded) {

			/*
			 * adjust years to the rounded values
			 */

			final double unitYears = (double) graphDefaultUnit / YEAR_IN_SECONDS;

			roundedYearUnit = Util.roundSimpleNumberUnits((long) unitYears);
			majorRoundedYearUnit = Util.getMajorSimpleNumberValue(roundedYearUnit);

			final long firstHistoryYear = tourStartTime.getYear();

			// decrease min value when it does not fit to unit borders
			final long yearMinRemainder = firstHistoryYear % roundedYearUnit;
			final long yearMinValue = firstHistoryYear - yearMinRemainder;

			final long yearMaxValue = lastUnitYear - (lastUnitYear % roundedYearUnit) + roundedYearUnit;

			unitStart = new DateTime((int) yearMinValue, 1, 1, 0, 0, 0, 0).getMillis();
			unitEnd = new DateTime((int) yearMaxValue, 12, 31, 23, 59, 59, 999).getMillis();
			firstUnitYear = yearMinValue;
			lastUnitYear = yearMaxValue;
		}

		/*
		 * check if history units must be created, this is done only once for a tour to optimize it
		 */
		if (unitStart != _historyUnitStart || unitEnd != _historyUnitDuration) {

			_historyUnitStart = unitStart;
			_historyUnitDuration = unitEnd;

			createHistoryUnits((int) firstUnitYear, (int) lastUnitYear);
		}

		graphDrawingData.setXUnitTextPos(GraphDrawingData.X_UNIT_TEXT_POS_CENTER);
		graphDrawingData.setIsXUnitOverlapChecked(true);
		graphDrawingData.setIsCheckUnitBorderOverlap(false);

		final HistoryTitle historyTitle = new HistoryTitle();
		xData.setHistoryTitle(historyTitle);

		// hide default unit
		xData.setUnitLabel(UI.EMPTY_STRING);

		final double devGraphXOffset = componentGraph.getXXDevViewPortLeftBorder();
		final int devVisibleWidth = getDevVisibleChartWidth();

		final long graphLeftBorder = (long) (devGraphXOffset / scaleX);
		final long graphRightBorder = (long) ((devGraphXOffset + devVisibleWidth) / scaleX);

		final ArrayList<ChartUnit> xUnits = graphDrawingData.getXUnits();
		final ArrayList<ChartUnit> xUnitTitles = new ArrayList<ChartUnit>();

		final ArrayList<Long> titleValueStart = historyTitle.graphStart = new ArrayList<Long>();
		final ArrayList<Long> titleValueEnd = historyTitle.graphEnd = new ArrayList<Long>();
		final ArrayList<String> titleText = historyTitle.titleText = new ArrayList<String>();

		final boolean isTimeSerieWithTimeZoneAdjustment = xData.isTimeSerieWithTimeZoneAdjustment();

//		DateTime graphTime = tourStartTime.plus(graphLeftBorder * 1000);
//		if (isTimeSerieWithTimeZoneAdjustment) {
//			if (graphTime.getMillis() > UI.beforeCET) {
//				graphTime = graphTime.minus(UI.BERLIN_HISTORY_ADJUSTMENT * 1000);
//			}
//		}
//
////		final int graphSecondsOfDay = graphTime.getSecondOfDay();
////		final DateTime graphNextDay = graphTime.plus((DAY_IN_SECONDS - graphSecondsOfDay) * 1000);
//
//		System.out.println(UI.timeStampNano());
//		System.out.println(UI.timeStampNano() + " tourStartTime " + tourStartTime);
//		System.out.println(UI.timeStampNano() + " graphTime     " + graphTime);
////		System.out.println(UI.timeStampNano() + " graphNextDay  " + graphNextDay);
//		System.out.println(UI.timeStampNano());
//		// TODO remove SYSTEM.OUT.PRINTLN

		if (isYearRounded) {

			/*
			 * create units for rounded years
			 */

//			System.out.println(UI.timeStampNano() + "\trounded years\t");
//			// TODO remove SYSTEM.OUT.PRINTLN

			graphDrawingData.setXUnitTextPos(GraphDrawingData.X_UNIT_TEXT_POS_LEFT);

			int historyYearIndex = 0;

			/*
			 * start unit at the first day of the first year at 0:00:00, this is necessary that the
			 * unit is positioned exactly
			 */
			final int startDOY = tourStartTime.getDayOfYear();
			final int startDaySeconds = tourStartTime.secondOfDay().get();

			final int startYear = tourStartTime.getYear();

			int yearIndex = 0;
			long graphYearOffset = 0;
			while (startYear > _historyYears[yearIndex]) {
				graphYearOffset += _historyDOY[yearIndex++] * DAY_IN_SECONDS;
			}

			long graphValue = -startDOY * DAY_IN_SECONDS - startDaySeconds - graphYearOffset;

			// loop: years
			while (graphValue <= graphMaxValue) {

				long graphUnit = 0;

				for (int unitIndex = 0; unitIndex < roundedYearUnit; unitIndex++) {

					final int unitYearIndex = historyYearIndex + unitIndex;

					// graph unit = rounded years
					graphUnit += _historyDOY[unitYearIndex] * DAY_IN_SECONDS;
				}

				if (graphValue < graphLeftBorder - graphUnit //
						//
						// ensure it's 366 days
						- DAY_IN_SECONDS) {

					// advance to the next unit
					graphValue += graphUnit;
					historyYearIndex += roundedYearUnit;

					continue;
				}

				if (graphValue > graphRightBorder) {
					break;
				}

				/*
				 * draw year tick
				 */
				final int yearValue = _historyYears[historyYearIndex];

				final boolean isMajorValue = yearValue % majorRoundedYearUnit == 0;

				xUnits.add(new ChartUnit(graphValue + DAY_IN_SECONDS, UI.EMPTY_STRING, isMajorValue));

				/*
				 * draw title
				 */
				titleValueStart.add(graphValue);
				titleValueEnd.add(graphValue + graphUnit - 1);
				titleText.add(Integer.toString(yearValue));

				// advance to the next rounded unit
				graphValue += graphUnit;
				historyYearIndex += roundedYearUnit;
			}

		} else if (dev1Year < _devAllMonthLabelWidth * 12) {

			/*
			 * create units for year/month
			 */

//			System.out.println(UI.timeStampNano() + "\tyear/month\t");
//			// TODO remove SYSTEM.OUT.PRINTLN

			graphDrawingData.setTitleTextPos(GraphDrawingData.X_UNIT_TEXT_POS_CENTER);
			graphDrawingData.setXUnitTextPos(GraphDrawingData.X_UNIT_TEXT_POS_CENTER);

			int historyYearIndex = 0;

			// start unit at the first day of the first year at 0:00:00
			final int startDOY = tourStartTime.getDayOfYear();
			final int startSeconds = tourStartTime.secondOfDay().get();
			long graphValue = -startDOY * DAY_IN_SECONDS - startSeconds;

			// loop: years
			while (graphValue <= graphMaxValue) {

				// graph unit = 1 year
				final long graphUnit = _historyDOY[historyYearIndex] * DAY_IN_SECONDS;

				if (graphValue < graphLeftBorder - graphUnit //
						//
						// ensure it's 366 days
						- DAY_IN_SECONDS) {

					// advance to the next unit
					graphValue += graphUnit;
					historyYearIndex++;

					continue;
				}

				if (graphValue > graphRightBorder) {
					break;
				}

				final int devUnitWidth = (int) (scaleX * graphUnit);
				final int[] historyMonthDays = _historyMonths[historyYearIndex];

				/*
				 * draw year tick
				 */
				xUnits.add(new ChartUnit(graphValue + DAY_IN_SECONDS, UI.EMPTY_STRING, true));

				/*
				 * draw year title
				 */
				{
					final String yearLabel = Integer.toString(_historyYears[historyYearIndex]);

					/*
					 * get number of repeated year labels within a year unit
					 */
					int repeatedMonths = 1;

					while (true) {
						if (devUnitWidth / repeatedMonths < devTitleVisibleUnit) {
							break;
						}
						repeatedMonths++;
					}

					// ensure array size is big enough (*2)
					final int[] monthStarts = new int[repeatedMonths * 2];
					final int[] monthEnds = new int[repeatedMonths * 2];
					final int monthRepeats = 12 / repeatedMonths;

					int yearMonthDOY = 0;
					int repeatIndex = 0;

					for (int monthIndex = 0; monthIndex < 12; monthIndex++) {

						final int monthDays = historyMonthDays[monthIndex];

						if (monthIndex % monthRepeats == 0) {

							if (repeatIndex > 0) {
								monthEnds[repeatIndex - 1] = yearMonthDOY;
							}

							monthStarts[repeatIndex] = yearMonthDOY;

							repeatIndex++;
						}

						yearMonthDOY += monthDays;
					}
					monthEnds[repeatIndex - 1] = yearMonthDOY;

					for (int repeatIndex2 = 0; repeatIndex2 < monthStarts.length; repeatIndex2++) {

						final int monthStart = monthStarts[repeatIndex2];
						final int monthEnd = monthEnds[repeatIndex2];

						// skip invalid entries
						if (monthStart == 0 && monthEnd == 0) {
							break;
						}

						titleValueStart.add(graphValue + monthStart * DAY_IN_SECONDS + DAY_IN_SECONDS);
						titleValueEnd.add(graphValue + monthEnd * DAY_IN_SECONDS + DAY_IN_SECONDS);

						titleText.add(yearLabel);
					}
				}

				/*
				 * draw x-axis units
				 */

				if (devUnitWidth >= _devAllMonthLabelWidth * 1.2) {

					createHistoryMonthUnits_Months(xUnits, historyMonthDays, graphValue, 1, 12, true);

				} else if (devUnitWidth >= _devAllMonthLabelWidth * 1) {

					createHistoryMonthUnits_Months(xUnits, historyMonthDays, graphValue, 3, 0, false);

				} else if (devUnitWidth >= _devAllMonthLabelWidth * 0.7) {

					createHistoryMonthUnits_Months(xUnits, historyMonthDays, graphValue, 6, 0, false);
				}

				// advance to the next unit
				graphValue += graphUnit;
				historyYearIndex++;
			}

		} else if (dev1Month < _devAllMonthLabelWidth * 30) {

			/*
			 * create units for month/day
			 */

//			System.out.println(UI.timeStampNano() + "\tmonth/day");
//			// TODO remove SYSTEM.OUT.PRINTLN

			graphDrawingData.setTitleTextPos(GraphDrawingData.X_UNIT_TEXT_POS_CENTER);

			int historyYearIndex = 0;

			// start unit at the first day of the first year at 0:00:00
			final int startDOY = tourStartTime.getDayOfYear();
			final int startSeconds = tourStartTime.secondOfDay().get();
			long graphValue = -startDOY * DAY_IN_SECONDS - startSeconds;

			monthLoop:

			// loop: months
			while (graphValue <= graphMaxValue) {

				final int[] yearMonths = _historyMonths[historyYearIndex];

				for (int monthIndex = 0; monthIndex < yearMonths.length; monthIndex++) {

					final int monthDays = yearMonths[monthIndex];

					// graph unit = 1 month
					final long graphUnit = monthDays * DAY_IN_SECONDS;

					if (graphValue < graphLeftBorder - graphUnit) {

						// advance to the next month unit
						graphValue += graphUnit;

						continue;
					}

					if (graphValue > graphRightBorder) {
						break monthLoop;
					}

					/*
					 * draw month tick
					 */
					xUnits.add(new ChartUnit(graphValue + DAY_IN_SECONDS, UI.EMPTY_STRING, true));

					/*
					 * create title units
					 */
					{
						final String monthTitle = _monthLabels[monthIndex]
								+ UI.SPACE2
								+ Integer.toString(_historyYears[historyYearIndex]);

						// get number of repeated labels within one graph unit
						int repeatedDays = 1;
						final int devUnitWidth = (int) (scaleX * graphUnit);

						while (true) {
							if (devUnitWidth / repeatedDays < devTitleVisibleUnit) {
								break;
							}
							repeatedDays++;
						}

						// ensure array size is big enough (*2)
						final int[] dayStarts = new int[repeatedDays * 2];
						final int[] dayEnds = new int[repeatedDays * 2];
						final int repeatedDayUnit = monthDays / repeatedDays;

						int dayStartEnd = 0;
						int repeatIndex = 0;

						for (int dayIndex = 0; dayIndex < monthDays; dayIndex++) {

							if (dayIndex % repeatedDayUnit == 0) {

								if (repeatIndex > 0) {
									dayEnds[repeatIndex - 1] = dayStartEnd;
								}

								dayStarts[repeatIndex] = dayStartEnd;

								repeatIndex++;
							}

							dayStartEnd += 1;
						}
						dayEnds[repeatIndex - 1] = dayStartEnd;

						for (int repeatIndex2 = 0; repeatIndex2 < dayStarts.length; repeatIndex2++) {

							final int dayStart = dayStarts[repeatIndex2];
							final int dayEnd = dayEnds[repeatIndex2];

							// skip invalid entries
							if (dayStart == 0 && dayEnd == 0) {
								break;
							}

							titleValueStart.add(graphValue + dayStart * DAY_IN_SECONDS + DAY_IN_SECONDS);
							titleValueEnd.add(graphValue + dayEnd * DAY_IN_SECONDS + DAY_IN_SECONDS);
							titleText.add(monthTitle);
						}
					}

					/*
					 * draw x-axis units: day number in month
					 */
					final double unitDays = (double) graphDefaultUnit / DAY_IN_SECONDS;

					final int roundedDayUnit = Util.roundSimpleNumberUnits((long) unitDays);

					if (roundedDayUnit == 1) {
						graphDrawingData.setXUnitTextPos(GraphDrawingData.X_UNIT_TEXT_POS_CENTER);
					} else {
						graphDrawingData.setXUnitTextPos(GraphDrawingData.X_UNIT_TEXT_POS_LEFT);
					}

					int dayNo = roundedDayUnit;
					while (dayNo <= monthDays) {

						xUnits.add(new ChartUnit(//
								graphValue + (dayNo * DAY_IN_SECONDS),
								Integer.toString(dayNo),
								false));

						dayNo += roundedDayUnit;
					}

					// advance to the next month unit
					graphValue += graphUnit;
				}

				historyYearIndex++;
			}

		} else {

			/*
			 * create units for day/seconds
			 */

//			System.out.println(UI.timeStampNano() + " day/seconds");
//			// TODO remove SYSTEM.OUT.PRINTLN

			graphDrawingData.setTitleTextPos(GraphDrawingData.X_UNIT_TEXT_POS_CENTER);
			graphDrawingData.setXUnitTextPos(GraphDrawingData.X_UNIT_TEXT_POS_LEFT);

			final long graphUnit = Util.roundTime24h(graphDefaultUnit);
			final long majorUnit = Util.getMajorTimeValue24(graphUnit);

			final int startSeconds = tourStartTime.secondOfDay().get();
			final long startUnitOffset = startSeconds % graphUnit;

			// decrease min value when it does not fit to unit borders, !!! VERY IMPORTANT !!!
			final long graphValueStart = graphLeftBorder - graphLeftBorder % graphUnit;

			final long graphMaxVisibleValue = graphRightBorder + graphUnit;
			long graphValue = graphValueStart;

			/*
			 * create x-axis units
			 */
			while (graphValue <= graphMaxVisibleValue) {

				// create unit value/label
				final long unitValueAdjusted = graphValue - startUnitOffset;
				final long unitValueStart = unitValueAdjusted + startSeconds;

				final long unitValue = unitValueStart % DAY_IN_SECONDS;

				final String unitLabel = net.tourbook.chart.Util.format_hh_mm_ss_Optional(unitValue);

				final boolean isMajorValue = unitValue % majorUnit == 0;

				xUnits.add(new ChartUnit(unitValueAdjusted, unitLabel, isMajorValue));

				graphValue += graphUnit;
			}

			/*
			 * create dummy units before and after the real units that the title is displayed also
			 * at the border, title is displayed between major units
			 */
			final int numberOfSmallUnits = (int) (majorUnit / graphUnit);
			long titleUnitStart = (long) xUnits.get(0).value;

			for (int unitIndex = numberOfSmallUnits; unitIndex > 0; unitIndex--) {

				final long unitValueAdjusted = titleUnitStart - (graphUnit * unitIndex);

				final long unitValue = (unitValueAdjusted + startSeconds) % DAY_IN_SECONDS;
				final boolean isMajorValue = unitValue % majorUnit == 0;

				final String unitLabel = net.tourbook.chart.Util.format_hh_mm_ss_Optional(unitValue);

				xUnitTitles.add(new ChartUnit(unitValueAdjusted, unitLabel, isMajorValue));
			}

			xUnitTitles.addAll(xUnits);

			titleUnitStart = (long) xUnitTitles.get(xUnitTitles.size() - 1).value;

			for (int unitIndex = 1; unitIndex < numberOfSmallUnits * 1; unitIndex++) {

				final long unitValueAdjusted = titleUnitStart + (graphUnit * unitIndex);

				final long unitValue = (unitValueAdjusted + startSeconds) % DAY_IN_SECONDS;
				final boolean isMajorValue = unitValue % majorUnit == 0;

				final String unitLabel = net.tourbook.chart.Util.format_hh_mm_ss_Optional(unitValue);

				xUnitTitles.add(new ChartUnit(unitValueAdjusted, unitLabel, isMajorValue));
			}

			/*
			 * create title units
			 */
			long prevGraphUnitValue = Long.MIN_VALUE;

			for (int unitIndex = 0; unitIndex < xUnitTitles.size(); unitIndex++) {

				final ChartUnit chartUnit = xUnitTitles.get(unitIndex);
				if (chartUnit.isMajorValue) {

					final long currentGraphUnitValue = (long) chartUnit.value;

					if (prevGraphUnitValue != Long.MIN_VALUE) {

						titleValueStart.add(prevGraphUnitValue);
						titleValueEnd.add(currentGraphUnitValue - 1);

						long graphDay = tourStartTime.getMillis() + prevGraphUnitValue * 1000;

						if (isTimeSerieWithTimeZoneAdjustment) {

							if (graphDay > UI.beforeCET) {
								graphDay -= UI.BERLIN_HISTORY_ADJUSTMENT * 1000;
							}
						}

						final String dayTitle = _dtFormatter.print(graphDay);

						titleText.add(dayTitle);
					}

					prevGraphUnitValue = currentGraphUnitValue;
				}
			}
		}

//		System.out.println(UI.timeStampNano() + " \t");
//
//		for (final ChartUnit xUnit : xUnits) {
//			System.out.println(UI.timeStampNano() + " \t" + xUnit);
//			// TODO remove SYSTEM.OUT.PRINTLN
//		}
//
//
//		for (final ChartUnit xUnit : xUnitTitles) {
//			System.out.println(UI.timeStampNano() + " \t" + xUnit);
//			// TODO remove SYSTEM.OUT.PRINTLN
//		}
//
//		for (int unitIndex = 0; unitIndex < titleText.size(); unitIndex++) {
//
//			System.out.println(UI.timeStampNano()
//					+ ("\t" + titleText.get(unitIndex))
//					+ ("\t" + (long) ((long) (titleValueStart.get(unitIndex) * scaleX) - devGraphXOffset))
//					+ ("\t" + (long) ((long) (titleValueEnd.get(unitIndex) * scaleX) - devGraphXOffset))
//					+ ("\t" + titleValueStart.get(unitIndex))
//					+ ("\t" + titleValueEnd.get(unitIndex))
//			//
//					);
//			// TODO remove SYSTEM.OUT.PRINTLN
//		}
	}

	private void createDrawingData_X_Month(final GraphDrawingData drawingData) {

		final ChartDataXSerie xData = drawingData.getXData();

		final int allUnitsSize = xData._highValuesDouble[0].length;
		final long devVirtualGraphWidth = componentGraph.getXXDevGraphWidth();
		final double scaleX = (double) devVirtualGraphWidth / allUnitsSize;
		drawingData.setScaleX(scaleX);

		final int numberOfYears = xData.getChartSegments().segmentTitle.length;

		createMonthEqualUnits(drawingData, devVirtualGraphWidth, allUnitsSize, numberOfYears);

		// compute the width and position of the rectangles
		final float monthWidth = (float) Math.max(0, (scaleX) - 1);
		final float barWidth = Math.max(0, (monthWidth * 0.90f));

		drawingData.setBarRectangleWidth((int) barWidth);
		drawingData.setDevBarRectangleXPos((int) (Math.max(0, (monthWidth - barWidth) / 2) + 1));

		drawingData.setXUnitTextPos(GraphDrawingData.X_UNIT_TEXT_POS_CENTER);
	}

	private void createDrawingData_X_Week(final GraphDrawingData drawingData) {

		final ChartDataXSerie xData = drawingData.getXData();
		final long devVirtualGraphWidth = componentGraph.getXXDevGraphWidth();

		final double[] xValues = xData.getHighValuesDouble()[0];
		final int allWeeks = xValues.length;
		final float devWeekWidth = devVirtualGraphWidth / allWeeks;

		final ChartStatisticSegments chartSegments = drawingData.getXData().getChartSegments();

		final int[] yearDays = chartSegments.yearDays;

		int allDaysInAllYears = 0;
		for (final int days : yearDays) {
			allDaysInAllYears += days;
		}

		createMonthUnequalUnits(drawingData, devVirtualGraphWidth, chartSegments.years, yearDays);

		final float barWidth = devWeekWidth * 0.7f;

		drawingData.setBarRectangleWidth((int) barWidth);
		drawingData.setDevBarRectangleXPos((int) (Math.max(0, (devWeekWidth - 2) / 2) + 1));

		drawingData.setBarPosition(GraphDrawingData.BAR_POS_CENTER);
		drawingData.setXUnitTextPos(GraphDrawingData.X_UNIT_TEXT_POS_CENTER);

		drawingData.setScaleX((double) devVirtualGraphWidth / allWeeks);
		drawingData.setScaleUnitX((double) devVirtualGraphWidth / allDaysInAllYears);

	}

	private void createDrawingData_X_Year(final GraphDrawingData drawingData) {

		final ChartDataYSerie yData = drawingData.getYData();
		final ChartDataXSerie xData = drawingData.getXData();

		final ChartStatisticSegments chartSegments = drawingData.getXData().getChartSegments();
		final int[] yearValues = chartSegments.years;
		final long devVirtualGraphWidth = componentGraph.getXXDevGraphWidth();

		final int yearCounter = xData._highValuesDouble[0].length;
		final double scaleX = (double) devVirtualGraphWidth / yearCounter;
		drawingData.setScaleX(scaleX);

		// create year units
		final ArrayList<ChartUnit> xUnits = drawingData.getXUnits();
		for (int yearIndex = 0; yearIndex < yearValues.length; yearIndex++) {
			xUnits.add(new ChartUnit(yearIndex, Integer.toString(yearValues[yearIndex])));
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

			final int serieCount = yData.getHighValuesFloat().length;

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

	/**
	 * computes data for the y axis
	 * 
	 * @param drawingData
	 * @param graphCount
	 * @param currentGraph
	 */
	private void createDrawingData_Y(final GraphDrawingData drawingData, final int graphCount, final int currentGraph) {

		final int unitType = drawingData.getYData().getAxisUnit();

		if (unitType == ChartDataSerie.AXIS_UNIT_NUMBER) {

			createDrawingData_Y_Numbers(drawingData, graphCount, currentGraph);

		} else if (unitType == ChartDataSerie.AXIS_UNIT_HOUR_MINUTE
				|| unitType == ChartDataSerie.AXIS_UNIT_HOUR_MINUTE_24H
				|| unitType == ChartDataSerie.AXIS_UNIT_HOUR_MINUTE_SECOND
				|| unitType == ChartDataSerie.AXIS_UNIT_MINUTE_SECOND) {

			createDrawingData_Y_Time(drawingData, graphCount, currentGraph);

		} else if (unitType == ChartDataSerie.AXIS_UNIT_HISTORY) {

			createDrawingData_Y_History(drawingData, graphCount, currentGraph);
		}
	}

	private void createDrawingData_Y_History(	final GraphDrawingData drawingData,
												final int graphCount,
												final int currentGraph) {

		// height of one chart graph including the slider bar
		final int devChartHeight = getDevChartHeightWithoutTrim();

		int devGraphHeight = devChartHeight;
		final boolean isChartStacked = _chartDataModel.isGraphOverlapped() == false;

		/*
		 * adjust graph device height for stacked graphs, a gap is between two graphs
		 */
		if (isChartStacked && graphCount > 1) {
			final int devGraphHeightSpace = devGraphHeight - (_chartsVerticalDistance * (graphCount - 1));
			devGraphHeight = (devGraphHeightSpace / graphCount);
		}

		// enforce minimum chart height
		devGraphHeight = Math.max(devGraphHeight, CHART_MIN_HEIGHT);

		// remove slider bar from graph height
		devGraphHeight -= _devSliderBarHeight;

		// calculate the vertical device offset
		int devYTop = _devMarginTop + _devXTitleBarHeight;

		if (isChartStacked) {
			// each chart has its own drawing rectangle which are stacked on
			// top of each other
			devYTop += (currentGraph * (devGraphHeight + _devSliderBarHeight))
					+ ((currentGraph - 1) * _chartsVerticalDistance);

		} else {
			// all charts are drawn on the same rectangle
			devYTop += devGraphHeight;
		}

		drawingData.setDevYBottom(devYTop);
		drawingData.setDevYTop(devYTop - devGraphHeight);

		drawingData.devGraphHeight = devGraphHeight;
		drawingData.setDevSliderHeight(0);
	}

	private void createDrawingData_Y_Numbers(	final GraphDrawingData drawingData,
												final int graphCount,
												final int currentGraph) {

		final ChartDataYSerie yData = drawingData.getYData();
		final int unitType = yData.getAxisUnit();

		// height of one chart graph including the slider bar
		final int devChartHeight = getDevChartHeightWithoutTrim();

		int devGraphHeight = devChartHeight;
		final boolean isChartStacked = _chartDataModel.isGraphOverlapped() == false;

		/*
		 * adjust graph device height for stacked graphs, a gap is between two graphs
		 */
		if (isChartStacked && graphCount > 1) {
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

		final double graphMinValue = yData.getVisibleMinValue();
		final double graphMaxValue = yData.getVisibleMaxValue();

		final double defaultValueRange = graphMaxValue > 0
				? (graphMaxValue - graphMinValue)
				: -(graphMinValue - graphMaxValue);

		/*
		 * calculate the number of units which will be visible by dividing the available height by
		 * the minimum size which one unit should have in pixels
		 */
		final int defaultUnitCount = devGraphHeight / _chart.gridVerticalDistance;

		// defaultUnitValue is the number in data values for one unit
		final double defaultUnitValue = defaultValueRange / Math.max(1, defaultUnitCount);

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
		final double value1 = (double) scaledValueRange / valueScaling;
		final double graphScaleY = devGraphHeight / value1;

		// calculate the vertical device offset
		int devYBottom = _devMarginTop + _devXTitleBarHeight;

		if (_chartDataModel.isGraphOverlapped()) {

			// all charts are drawn at the same rectangle
			devYBottom += devGraphHeight + _devSliderBarHeight;

		} else {
			// each chart has its own drawing rectangle which are stacked on
			// top of each other
			devYBottom += (currentGraph * (devGraphHeight + _devSliderBarHeight))
					+ ((currentGraph - 1) * _chartsVerticalDistance);
		}

		drawingData.setScaleY(graphScaleY);

		drawingData.setDevYBottom(devYBottom);
		drawingData.setDevYTop(devYBottom - devGraphHeight);

		drawingData.setGraphYBottom((float) scaledMinValue / valueScaling);
		drawingData.setGraphYTop((float) scaledMaxValue / valueScaling);

		drawingData.devGraphHeight = devGraphHeight;
		drawingData.setDevSliderHeight(_devSliderBarHeight);

		final ArrayList<ChartUnit> units = drawingData.getYUnits();
		final int valueDivisor = yData.getValueDivisor();
		int loopCounter = 0;

		long scaledValue = scaledMinValue;

		// loop: create unit label for all units
		while (scaledValue <= scaledMaxValue) {

			final double descaledValue = (double) scaledValue / valueScaling;

			final String unitLabel = net.tourbook.chart.Util.formatValue(
					(float) descaledValue,
					unitType,
					valueDivisor,
					false);
			final boolean isMajorValue = descaledValue % majorValue == 0;

			units.add(new ChartUnit((float) descaledValue, unitLabel, isMajorValue));

			// prevent endless loops when the unit is 0
			if (scaledValue == scaledMaxValue || loopCounter++ > 1000) {
				break;
			}

			scaledValue += scaledUnit;
		}

		if (unitType == ChartDataSerie.AXIS_UNIT_HOUR_MINUTE_24H && scaledValue > scaledMaxValue) {
			units.add(new ChartUnit(scaledMaxValue / valueScaling, UI.EMPTY_STRING));
		}
	}

	private void createDrawingData_Y_Time(	final GraphDrawingData drawingData,
											final int graphCount,
											final int currentGraph) {

		final ChartDataYSerie yData = drawingData.getYData();

		// height of one chart graph including the slider bar
		final int devChartHeight = getDevChartHeightWithoutTrim();

		int devGraphHeight = devChartHeight;
		final boolean isChartStacked = _chartDataModel.isGraphOverlapped() == false;

		// adjust graph device height for stacked graphs, a gap is between two
		// graphs
		if (isChartStacked && graphCount > 1) {
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
		if (_chartDataModel.getChartType() == ChartType.BAR && _chart.getStartAtChartBottom()) {
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
		int devYBottom = _devMarginTop + _devXTitleBarHeight;

		if (_chartDataModel.isGraphOverlapped()) {

			// all charts are drawn at the same rectangle
			devYBottom += devGraphHeight + _devSliderBarHeight;

		} else {
			// each chart has its own drawing rectangle which are stacked on
			// top of each other
			devYBottom += (currentGraph * (devGraphHeight + _devSliderBarHeight))
					+ ((currentGraph - 1) * _chartsVerticalDistance);
		}

		drawingData.setScaleY(graphScaleY);

		drawingData.setDevYBottom(devYBottom);
		drawingData.setDevYTop(devYBottom - devGraphHeight);

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

			final String unitLabel = net.tourbook.chart.Util.formatValue(graphValue, unitType, valueDivisor, false);
			final boolean isMajorValue = graphValue % majorValue == 0;

			unitList.add(new ChartUnit(graphValue, unitLabel, isMajorValue));

			// prevent endless loops when the unit is 0
			if (graphValue == graphMaxValue || maxUnits++ > 1000) {
				break;
			}

			graphValue += graphUnit;
		}

		if (unitType == ChartDataSerie.AXIS_UNIT_HOUR_MINUTE_24H && graphValue > graphMaxValue) {
			unitList.add(new ChartUnit(graphMaxValue, UI.EMPTY_STRING));
		}

	}

	private void createHistoryMonthUnits_Months(final ArrayList<ChartUnit> xUnits,
												final int[] historyMonths,
												final long graphValue,
												final int skippedMonths,
												final int majorMonth,
												final boolean isShowLabel) {

		int allMonthDays = 0;

		for (int monthIndex = 0; monthIndex < historyMonths.length; monthIndex++) {

			final int monthDays = historyMonths[monthIndex];

			allMonthDays += monthDays;

			if (monthIndex % skippedMonths != 0) {
				// skip months which are not displayed
				continue;
			}

			final int monthValueDays = allMonthDays - monthDays;
			final int monthValue = monthValueDays * DAY_IN_SECONDS;

			final String valueLabel = isShowLabel ? _monthLabels[monthIndex] : UI.EMPTY_STRING;
			final boolean isMajorValue = majorMonth == 0 ? false : monthIndex % majorMonth == 0;

			xUnits.add(new ChartUnit(//
					graphValue + monthValue + DAY_IN_SECONDS - 1, //
					valueLabel,
					isMajorValue)
			//
					);
		}
	}

	private void createHistoryUnits(final int firstYear, int lastYear) {

		/*
		 * add an additional year because at the year end, a history chart displays also the next
		 * year which caused an outOfBound exception when testing this app at 28.12.2012
		 */
		lastYear += 1;

		final int numberOfYears = lastYear - firstYear + 1;

		_historyYears = new int[numberOfYears];
		_historyMonths = new int[numberOfYears][12];
		_historyDOY = new int[numberOfYears];

		int yearIndex = 0;

		DateTime currentYear = new DateTime().withDate(firstYear - 1, 1, 1);

		for (int currentYearNo = firstYear; currentYearNo <= lastYear; currentYearNo++) {

			currentYear = currentYear.plusYears(1);

			_historyYears[yearIndex] = currentYearNo;

			int yearDOY = 0;

			// get number of days for each month
			for (int monthIndex = 0; monthIndex < 12; monthIndex++) {

				final int monthDays = currentYear.withMonthOfYear(monthIndex + 1).dayOfMonth().getMaximumValue();

				_historyMonths[yearIndex][monthIndex] = monthDays;

				yearDOY += monthDays;
			}

			_historyDOY[yearIndex] = yearDOY;

			yearIndex++;
		}
	}

	private void createMonthEqualUnits(	final GraphDrawingData drawingData,
										final long devGraphWidth,
										final int allUnitsSize,
										final int numberOfYears) {

		final ArrayList<ChartUnit> xUnits = drawingData.getXUnits();
		final boolean isDrawUnits[] = new boolean[allUnitsSize];

		/*
		 * create month labels depending on the available width for a unit
		 */
		final int devYearWidth = (int) (devGraphWidth / numberOfYears);
		if (devYearWidth >= _devAllMonthLabelWidth) {

			// all month labels can be displayed

			for (int monthIndex = 0; monthIndex < allUnitsSize; monthIndex++) {
				xUnits.add(new ChartUnit(monthIndex, _monthLabels[monthIndex % 12]));
				isDrawUnits[monthIndex] = true;
			}

		} else if (devYearWidth >= _devAllMonthLabelWidth / 3) {

			// every second month label can be displayed

			for (int monthIndex = 0; monthIndex < allUnitsSize; monthIndex++) {

				final String monthLabel = monthIndex % 3 == 0 //
						? _monthLabels[monthIndex % 12]
						: UI.EMPTY_STRING;

				xUnits.add(new ChartUnit(monthIndex, monthLabel));
				isDrawUnits[monthIndex] = monthIndex % 3 == 0;
			}

		} else if (devYearWidth >= _devAllMonthShortLabelWidth / 3) {

			// every second month short label can be displayed

			for (int monthIndex = 0; monthIndex < allUnitsSize; monthIndex++) {

				final String monthLabel = monthIndex % 3 == 0 //
						? _monthShortLabels[monthIndex % 12]
						: UI.EMPTY_STRING;

				xUnits.add(new ChartUnit(monthIndex, monthLabel));
				isDrawUnits[monthIndex] = monthIndex % 3 == 0;
			}

		} else if (devYearWidth >= _devAllMonthShortLabelWidth / 6) {

			// every second month short label can be displayed

			for (int monthIndex = 0; monthIndex < allUnitsSize; monthIndex++) {

				final String monthLabel = monthIndex % 6 == 0 //
						? _monthShortLabels[monthIndex % 12]
						: UI.EMPTY_STRING;

				xUnits.add(new ChartUnit(monthIndex, monthLabel));
				isDrawUnits[monthIndex] = monthIndex % 3 == 0;
			}

		} else {

			// width is too small to display month labels, display nothing

			for (int monthIndex = 0; monthIndex < allUnitsSize; monthIndex++) {
				xUnits.add(new ChartUnit(monthIndex, UI.EMPTY_STRING));
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
	 * @param devGraphWidth
	 * @param years
	 * @param yearDays
	 *            Number of days in one year
	 */
	private void createMonthUnequalUnits(	final GraphDrawingData drawingData,
											final long devGraphWidth,
											final int[] years,
											final int[] yearDays) {

		/*
		 * multiple years can have different number of days but we assume that each year has the
		 * same number of days to make it simpler
		 */

		final int numberOfYears = years.length;
		final int numberOfMonths = numberOfYears * 12; // number of units

		final boolean isDrawUnits[] = new boolean[numberOfMonths];

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

		final ArrayList<ChartUnit> xUnits = drawingData.getXUnits();

		/*
		 * create month labels depending on the available width for a unit
		 */
		final int devYearWidth = (int) (devGraphWidth / numberOfYears);
		if (devYearWidth >= _devAllMonthLabelWidth) {

			// all month labels can be displayed

			for (int monthIndex = 0; monthIndex < numberOfMonths; monthIndex++) {

				final int unitValue = daysForAllUnits[monthIndex];

				xUnits.add(new ChartUnit(unitValue, _monthLabels[monthIndex % 12]));

				isDrawUnits[monthIndex] = true;
			}

		} else if (devYearWidth >= _devAllMonthLabelWidth / 3) {

			// every second month label can be displayed

			for (int monthIndex = 0; monthIndex < numberOfMonths; monthIndex++) {

				final String monthLabel = monthIndex % 3 == 0 //
						? _monthLabels[monthIndex % 12]
						: UI.EMPTY_STRING;

				final int unitValue = daysForAllUnits[monthIndex];

				xUnits.add(new ChartUnit(unitValue, monthLabel));

//				isDrawUnits[monthIndex] = monthIndex % 3 == 0;
				isDrawUnits[monthIndex] = true;
			}

		} else if (devYearWidth >= _devAllMonthShortLabelWidth / 3) {

			// every second month short label can be displayed

			for (int monthIndex = 0; monthIndex < numberOfMonths; monthIndex++) {

				final String monthLabel = monthIndex % 3 == 0 //
						? _monthShortLabels[monthIndex % 12]
						: UI.EMPTY_STRING;

				xUnits.add(new ChartUnit(daysForAllUnits[monthIndex], monthLabel));
				isDrawUnits[monthIndex] = monthIndex % 3 == 0;
			}

		} else if (devYearWidth >= _devAllMonthShortLabelWidth / 6) {

			// every second month short label can be displayed

			for (int monthIndex = 0; monthIndex < numberOfMonths; monthIndex++) {

				final String monthLabel = monthIndex % 6 == 0 //
						? _monthShortLabels[monthIndex % 12]
						: UI.EMPTY_STRING;

				xUnits.add(new ChartUnit(daysForAllUnits[monthIndex], monthLabel));
				isDrawUnits[monthIndex] = monthIndex % 3 == 0;
			}

		} else {

			// width is too small to display month labels, display nothing

			for (int monthIndex = 0; monthIndex < numberOfMonths; monthIndex++) {
				xUnits.add(new ChartUnit(daysForAllUnits[monthIndex], UI.EMPTY_STRING));
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

		final double[] xValues = xData.getHighValuesDouble()[0];
		final double markerStartValue = xValues[Math.min(markerValueIndexStart, xValues.length - 1)];
		final double markerEndValue = xValues[Math.min(markerValueIndexEnd, xValues.length - 1)];

		final double valueDiff = markerEndValue - markerStartValue;
		final double lastValue = xValues[xValues.length - 1];

		final float devVirtualGraphImageWidth = componentGraph.getXXDevGraphWidth();
		final float devGraphImageXOffset = componentGraph.getXXDevViewPortLeftBorder();

		final double devOneValueSlice = devVirtualGraphImageWidth / lastValue;

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

	public ChartComponentAxis getAxisLeft() {
		return componentAxisLeft;
	}

	public ChartComponentAxis getAxisRight() {
		return componentAxisRight;
	}

	public ChartComponentGraph getChartComponentGraph() {
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
		return _devMarginTop //
				+ _devXTitleBarHeight
				+ _devSliderBarHeight;
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

	int getMarginBottomStartingFromTop() {

		return _visibleGraphRect.height //
				- _devXAxisHeight;
	}

	/**
	 * Get size in pixel for the month labels and shortcuts
	 */
	private void getMonthLabelWidth() {

		final GC gc = new GC(this);
		{
			_devAllMonthLabelWidth = 0;
			_devAllMonthShortLabelWidth = 0;

			for (int monthIndex = 0; monthIndex < _monthLabels.length; monthIndex++) {
				_devAllMonthLabelWidth += gc.stringExtent(_monthLabels[monthIndex]).x + 6;
				_devAllMonthShortLabelWidth += gc.stringExtent(_monthShortLabels[monthIndex]).x + 12;
			}

			_devYearLabelWidth += gc.stringExtent("2222").x + 0;//$NON-NLS-1$
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

		// resize the sliders after the drawing data have changed and the new chart size is saved
		componentGraph.updateSlidersOnResize();

		// resize the axis
		componentAxisLeft.setDrawingData(_chartDrawingData, true);
		componentAxisRight.setDrawingData(_chartDrawingData, false);

		componentGraph.updateChartSize();

		_chart.onExternalChartResize();

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
				@Override
				public void run() {
					display.timerExec(BAR_SELECTION_DELAY_TIME, new Runnable() {

						final int	__runnableKeyDownCounter	= _keyDownCounter[0];

						@Override
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
		final ChartType chartType = _chartDataModel.getChartType();
		if (isShowAllData
				&& (chartType == ChartType.LINE || chartType == ChartType.LINE_WITH_BARS || chartType == ChartType.HISTORY)) {

			componentGraph.updateVisibleMinMaxValues();
		}

		/*
		 * resetting the sliders require that the drawing data are created, this is done in the
		 * onResize method
		 */
		if (onResize()) {
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
			if (yData.getYTitle() != null || yData.getUnitLabel().length() > 0) {
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

		final double[] xValues = xData.getHighValuesDouble()[0];
		final double markerValueStart = xValues[markerStartIndex];

		final double valueDiff = xValues[markerEndIndex] - markerValueStart;
		final double valueLast = xValues[xValues.length - 1];

		final int devVisibleChartWidth = getDevVisibleChartWidth();

		final double xxDevGraphWidth;
		final int xxDevViewPortOffset;
		final double graphZoomRatio;

		switch (_chart._synchMode) {
		case Chart.SYNCH_MODE_BY_SCALE:

			// get marker data from the synch source
			final float markerWidthRatio = _synchConfigSrc.getMarkerWidthRatio();
			final float markerOffsetRatio = _synchConfigSrc.getMarkerOffsetRatio();

			// virtual graph width
			final float devMarkerWidth = devVisibleChartWidth * markerWidthRatio;
			final double devOneValueSlice = devMarkerWidth / valueDiff;
			xxDevGraphWidth = devOneValueSlice * valueLast;

			// graph offset
			final float devMarkerOffset = devVisibleChartWidth * markerOffsetRatio;
			final double devMarkerStart = devOneValueSlice * markerValueStart;
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
	void setXSliderPosition(final SelectionChartXSliderPosition sliderPosition, final boolean isFireEvent) {

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

		final double[] xValues = _chartDataModel.getXData()._highValuesDouble[0];
		final boolean isCenterSliderPosition = sliderPosition.isCenterSliderPosition();

		// move left slider
		if (slider1ValueIndex == SelectionChartXSliderPosition.SLIDER_POSITION_AT_CHART_BORDER) {

			componentGraph.setXSliderValueIndex(//
					leftSlider,
					0,
					isCenterSliderPosition,
					isFireEvent);

		} else if (slider1ValueIndex != SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION) {

			componentGraph.setXSliderValueIndex(//
					leftSlider,
					slider1ValueIndex,
					isCenterSliderPosition,
					isFireEvent);
		}

		if (slider0ValueIndex != SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION) {

			// move second slider before the first slider

			componentGraph.setXSliderValueIndex(rightSlider, slider0ValueIndex, isCenterSliderPosition, isFireEvent);

		} else {

			// move right slider
			if (slider2ValueIndex == SelectionChartXSliderPosition.SLIDER_POSITION_AT_CHART_BORDER) {

				componentGraph.setXSliderValueIndex(//
						rightSlider,
						xValues.length - 1,
						isCenterSliderPosition,
						isFireEvent);

			} else if (slider2ValueIndex != SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION) {

				componentGraph.setXSliderValueIndex(//
						rightSlider,
						slider2ValueIndex,
						isCenterSliderPosition,
						isFireEvent);
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
