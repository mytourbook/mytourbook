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
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * Chart widget which represents the chart ui The chart consists of these components
 * <p>
 * The chart widget consists has the following heights: <code>
 *  devMarginTop
 *  devTitleBarHeight
 *  devMarkerBarHeight
 *                                                                                                      
 *  |devSliderBarHeight
 *  |#graph#                                                 
 *  |verticalDistance
 *                                                          
 *  |devSliderBarHeight
 *  |#graph# 
 *  |verticalDistance
 *                                                          
 *     ...
 *                                                                                                          
 *   |devSliderBarHeight
 *   |#graph#                                                 
 *                                                                                                          
 *   xAxisHeight
 * </code>
 */
public class ChartComponents extends Composite {

	private static final int			DELAY_TIME					= 100;

	/**
	 * min/max pixel widthDev/heightDev of the chart
	 */
	static final int					CHART_MIN_WIDTH				= 1;
	static final int					CHART_MIN_HEIGHT			= 1;
	static final int					CHART_MAX_WIDTH				= 0x7fff;
	static final int					CHART_MAX_HEIGHT			= 5000;

	static final int					SLIDER_BAR_HEIGHT			= 10;
	static final int					MARKER_BAR_HEIGHT			= 50;
	static final int					TITLE_BAR_HEIGHT			= 15;
	static final int					MARGIN_TOP_WITH_TITLE		= 5;
	static final int					MARGIN_TOP_WITHOUT_TITLE	= 10;

	private final Chart					fChart;

	/**
	 * top margin of the chart (and all it's components)
	 */
	private int							devMarginTop				= MARGIN_TOP_WITHOUT_TITLE;

	/**
	 * height of the marker bar, 0 indicates that the marker bar is not visible
	 */
	private int							devMarkerBarHeight			= 0;

	/**
	 * height of the slider bar, 0 indicates that the slider is not visible
	 */
	int									devSliderBarHeight			= 0;

	/**
	 * height of the title bar, 0 indicates that the title is not visible
	 */
	private int							fDevXTitleBarHeight			= 0;

	/**
	 * height of the horizontal axis
	 */
	private final int					xAxisHeight					= 25;

	/**
	 * width of the vertical axis
	 */
	private final int					yAxisWidthLeft				= 50;
	private int							yAxisWidthLeftWithTitle		= yAxisWidthLeft;
	private final int					yAxisWidthRight				= 50;

	/**
	 * vertical distance between two graphs
	 */
	private final int					fChartsVerticalDistance		= 15;

	/**
	 * minimum width in pixel for one unit, this is only an approximate value because the pixel is
	 * rounded up or down to fit a rounded unit
	 */
	private final int					fDevMinXUnit				= 100;

	private final int					fDevMinYUnit				= 50;

	/**
	 * contains the {@link SynchConfiguration} for the current chart and will be used from the chart
	 * which is synchronized
	 */
	SynchConfiguration					fSynchConfigOut				= null;

	/**
	 * when a {@link SynchConfiguration} is set, this chart will be synchronized with the chart
	 * which set's the synch config
	 */
	SynchConfiguration					fSynchConfigSrc				= null;

	/**
	 * visible chart rectangle
	 */
	private Rectangle					fVisibleGraphRect;

	private final ChartComponentGraph	fComponentGraph;
	private final ChartComponentAxis	fComponentAxisLeft;
	private final ChartComponentAxis	fComponentAxisRight;

	private ChartDataModel				fChartDataModel				= null;

	private ArrayList<ChartDrawingData>	fChartDrawingData;

	public boolean						useAdvancedGraphics			= true;

	private final String				monthLabels[]				= {
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
			Messages.Month_dec										};

	private final int[]					fKeyDownCounter				= new int[1];
	private final int[]					fLastKeyDownCounter			= new int[1];

	/**
	 * Create and layout the components of the chart
	 * 
	 * @param parent
	 * @param style
	 */
	public ChartComponents(final Chart parent, final int style) {

		super(parent, style);

		GridData gd;
		fChart = parent;

		// set the layout for the components
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
//		gd.widthHint = CHART_MIN_WIDTH;
//		gd.heightHint = CHART_MIN_HEIGHT;
		setLayoutData(gd);

		// set the layout for this chart
		final GridLayout gl = new GridLayout(3, false);
		gl.horizontalSpacing = 0;
		gl.verticalSpacing = 0;
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		setLayout(gl);

		// left: create left axis canvas
		fComponentAxisLeft = new ChartComponentAxis(parent, this, SWT.NONE);
		gd = new GridData(SWT.NONE, SWT.FILL, false, true);
		gd.widthHint = yAxisWidthLeft;
		fComponentAxisLeft.setLayoutData(gd);

		// center: create chart canvas
		fComponentGraph = new ChartComponentGraph(parent, this, SWT.NONE);
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
//		gd.widthHint = CHART_MIN_WIDTH;
		fComponentGraph.setLayoutData(gd);

		// right: create right axis canvas
		fComponentAxisRight = new ChartComponentAxis(parent, this, SWT.NONE);
		gd = new GridData(SWT.NONE, SWT.FILL, false, true);
		gd.widthHint = yAxisWidthRight;
		fComponentAxisRight.setLayoutData(gd);

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

		// addListener(SWT.MouseDown, new Listener() {
		// public void handleEvent(final Event event) {
		// // set the focus to the chart
		// fComponentGraph.setFocus();
		// }
		// });

		fComponentGraph.addListener(SWT.Traverse, new Listener() {
			public void handleEvent(final Event event) {

				switch (event.detail) {
				case SWT.TRAVERSE_RETURN:
				case SWT.TRAVERSE_ESCAPE:
				case SWT.TRAVERSE_TAB_NEXT:
				case SWT.TRAVERSE_TAB_PREVIOUS:
				case SWT.TRAVERSE_PAGE_NEXT:
				case SWT.TRAVERSE_PAGE_PREVIOUS:
					event.doit = true;
					break;
				}
			}
		});

		fComponentGraph.addListener(SWT.KeyDown, new Listener() {
			public void handleEvent(final Event event) {
				handleLeftRightEvent(event);
			}
		});
	}

	/**
	 * Compute the units for the x-axis and save it in the drawingData object
	 */
	private void computeXValues(final ChartDrawingData drawingData) {

		final ChartDataXSerie xData = drawingData.getXData();

		final int xMaxValue = xData.getVisibleMaxValue();
		final int xAxisUnit = xData.getAxisUnit();
		final int xStartValue = xData.getStartValue();

		final int devGraphWidth = fComponentGraph.getDevVirtualGraphImageWidth();

		// enforce minimum chart width
//		devGraphWidth = Math.max(devGraphWidth, CHART_MIN_WIDTH);

		drawingData.setDevGraphWidth(devGraphWidth);
		drawingData.setScaleX((float) devGraphWidth / xMaxValue);

		/*
		 * calculate the number of units which will be visible by dividing the visible length by the
		 * minimum size which one unit should have in pixels
		 */
		final int unitRawNumbers = devGraphWidth / fDevMinXUnit;

		// unitRawValue is the number in data values for one unit
		final int unitRawValue = xMaxValue / Math.max(1, unitRawNumbers);

		// axis unit
		float unitValue = 0;

		// get the unit list from the configuration
		final ArrayList<ChartUnit> units = drawingData.getXUnits();

		switch (xAxisUnit) {
		case ChartDataSerie.AXIS_UNIT_HOUR_MINUTE_SECOND:
		case ChartDataSerie.AXIS_UNIT_HOUR_MINUTE:
			unitValue = ChartUtil.roundTimeValue(unitRawValue);
			break;

		case ChartDataSerie.AXIS_UNIT_NUMBER:
			unitValue = ChartUtil.roundDecimalValue(unitRawValue);
			break;

		case ChartDataSerie.X_AXIS_UNIT_WEEK:
			createXValuesWeek(drawingData, units, devGraphWidth);
			break;

		case ChartDataSerie.AXIS_UNIT_MONTH:
			createXValuesMonth(drawingData, units, devGraphWidth);
			break;

		case ChartDataSerie.AXIS_UNIT_YEAR:
			createXValuesYear(drawingData, units, devGraphWidth);
			break;

		case ChartDataSerie.AXIS_UNIT_DAY:
			createXValuesDay(drawingData, units, devGraphWidth);
			break;

		default:
			break;
		}

		// create the units for the x-axis
		if (xAxisUnit != ChartDataSerie.AXIS_UNIT_DAY
				&& xAxisUnit != ChartDataSerie.AXIS_UNIT_MONTH
				&& xAxisUnit != ChartDataSerie.AXIS_UNIT_YEAR
				&& xAxisUnit != ChartDataSerie.X_AXIS_UNIT_WEEK) {

			// get the unitOffset when a startValue is set
			int unitOffset = 0;
			if (xStartValue != 0) {
				unitOffset = (int) (xStartValue % unitValue);
			}

			final int valueDivisor = xData.getValueDivisor();
			int graphValue = 0;
			int infinityLoopIndex = 0;

			while (graphValue <= xMaxValue) {

				// create unit value/label
				final int unitPos = graphValue - unitOffset;
				units.add(new ChartUnit(unitPos, ChartUtil.formatValue(unitPos + xStartValue,
						xAxisUnit,
						valueDivisor,
						false)));

				graphValue += unitValue;

				// check for an infinity loop
				if (infinityLoopIndex++ > 1000) {
					break;
				}
			}

		}

		// configure the bar in the bar charts
		if (fChartDataModel.getChartType() == ChartDataModel.CHART_TYPE_BAR && // 
				(xAxisUnit == ChartDataSerie.AXIS_UNIT_NUMBER //
				|| xAxisUnit == ChartDataSerie.AXIS_UNIT_HOUR_MINUTE)) {

			final int barWidth = (devGraphWidth / xData.getHighValues()[0].length) / 2;

			drawingData.setBarRectangleWidth(Math.max(0, barWidth));
			drawingData.setBarPosition(ChartDrawingData.BAR_POS_CENTER);
		}
	}

	/**
	 * computes data for the y axis
	 * 
	 * @param drawingData
	 * @param graphCount
	 * @param currentGraph
	 */
	private void computeYValues(final ChartDrawingData drawingData, final int graphCount, final int currentGraph) {

		final ChartDataSerie yData = drawingData.getYData();

		final Point graphSize = fComponentGraph.getVisibleSizeWithHBar(fVisibleGraphRect.width,
				fVisibleGraphRect.height);

		// height of one chart graph including the slider bar
		int devGraphHeight = graphSize.y - devMarginTop - devMarkerBarHeight - fDevXTitleBarHeight - xAxisHeight;

		// adjust graph device height for stacked graphs, a gap is between two
		// graphs
		if (fChartDataModel.isStackedChart() && graphCount > 1) {
			final int devGraphHeightSpace = (devGraphHeight - (fChartsVerticalDistance * (graphCount - 1)));
			devGraphHeight = (devGraphHeightSpace / graphCount);
		}

		// enforce minimum chart height
		devGraphHeight = Math.max(devGraphHeight, CHART_MIN_HEIGHT);

		// remove slider bar from graph height
		devGraphHeight -= devSliderBarHeight;

		/*
		 * all variables starting with graph... contain data values from the graph which are not
		 * scaled to the device
		 */

		final int axisUnit = yData.getAxisUnit();
		int graphMinValue = yData.getVisibleMinValue();
		int graphMaxValue = yData.getVisibleMaxValue();

		boolean adjustGraphUnit = false;
		// clip max value
		if (axisUnit == ChartDataSerie.AXIS_UNIT_HOUR_MINUTE_24H && (graphMaxValue / 3600 > 24)) {
			graphMaxValue = 24 * 3600;
			adjustGraphUnit = true;
		}

		int graphValueRange = graphMaxValue > 0 ? (graphMaxValue - graphMinValue) : -(graphMinValue - graphMaxValue);

		/*
		 * calculate the number of units which will be visible by dividing the available height by
		 * the minimum size which one unit should have in pixels
		 */
		final int unitCount = devGraphHeight / (fDevMinYUnit + 15);

		// unitValue is the number in data values for one unit
		final int graphUnitValue = graphValueRange / Math.max(1, unitCount);

		// round the unit
		float graphUnit = 0;
		switch (axisUnit) {
		case ChartDataSerie.AXIS_UNIT_HOUR_MINUTE:
		case ChartDataSerie.AXIS_UNIT_HOUR_MINUTE_24H:
		case ChartDataSerie.AXIS_UNIT_HOUR_MINUTE_SECOND:
			graphUnit = ChartUtil.roundTimeValue(graphUnitValue);
			break;

		case ChartDataSerie.AXIS_UNIT_NUMBER:
			// unit is a decimal number
			graphUnit = ChartUtil.roundDecimalValue(graphUnitValue);
			break;
		}

		float adjustMinValue = 0;
		if (((float) graphMinValue % graphUnit) != 0 && graphMinValue < 0) {
			adjustMinValue = graphUnit;
		}
		graphMinValue = (int) ((int) ((graphMinValue - adjustMinValue) / graphUnit) * graphUnit);

		// adjust the min value so that bar graphs start at the bottom of the chart
		if (fChartDataModel.getChartType() == ChartDataModel.CHART_TYPE_BAR && fChart.getStartAtChartBottom()) {
			yData.setVisibleMinValue(graphMinValue);
		}

		// increase the max value when it does not fit to unit borders
		float adjustMaxValue = 0;
		if (((float) graphMaxValue % graphUnit) != 0) {
			adjustMaxValue = graphUnit;
		}
		graphMaxValue = (int) ((int) ((graphMaxValue + adjustMaxValue) / graphUnit) * graphUnit);

		if (adjustGraphUnit || axisUnit == ChartDataSerie.AXIS_UNIT_HOUR_MINUTE_24H && (graphMaxValue / 3600 > 24)) {

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
			graphMaxValue = 24 * 3600;
			graphMaxValue = Math.min(24 * 3600, (((yData.getVisibleMaxValue()) / 3600) * 3600) + 3600);

			// adjust to the whole hour
			graphMinValue = Math.max(0, (((yData.getVisibleMinValue() / 3600) * 3600)));

			graphUnit = (graphMaxValue - graphMinValue) / unitCounter;
			graphUnit = ChartUtil.roundTimeValue((int) graphUnit);
		}

		graphValueRange = graphMaxValue > 0 ? (graphMaxValue - graphMinValue) : -(graphMinValue - graphMaxValue);

		// calculate the vertical scaling between graph and device
		final float graphScaleY = (float) (devGraphHeight) / graphValueRange;

		// calculate the vertical device offset
		int devYTop = devMarginTop + devMarkerBarHeight + fDevXTitleBarHeight;

		if (fChartDataModel.isStackedChart()) {
			// each chart has its own drawing rectangle which are stacked on
			// top of each other
			devYTop += (currentGraph * (devGraphHeight + devSliderBarHeight))
					+ ((currentGraph - 1) * fChartsVerticalDistance);

		} else {
			// all charts are drawn on the same rectangle
			devYTop += devGraphHeight;
		}

		drawingData.setScaleY(graphScaleY);

		drawingData.setDevYBottom(devYTop);
		drawingData.setDevYTop(devYTop - devGraphHeight);

		drawingData.setGraphYBottom(graphMinValue);
		drawingData.setGraphYTop(graphMaxValue);

		drawingData.setDevGraphHeight(devGraphHeight);
		drawingData.setDevSliderHeight(devSliderBarHeight);

		final ArrayList<ChartUnit> unitList = drawingData.getYUnits();

		int graphValue = graphMinValue;
		int maxUnits = 0;

		// loop: create unit label for all units
		while (graphValue <= graphMaxValue) {
			unitList.add(new ChartUnit(graphValue, ChartUtil.formatValue(graphValue,
					axisUnit,
					yData.getValueDivisor(),
					false)));

			// prevent endless loops when the unit is 0
			if (graphValue == graphMaxValue || maxUnits > 1000) {
				break;
			}

			graphValue += graphUnit;
			maxUnits++;
		}

		if (axisUnit == ChartDataSerie.AXIS_UNIT_HOUR_MINUTE_24H && graphValue > graphMaxValue) {

			unitList.add(new ChartUnit(graphMaxValue, ""));
		}
	}

	/**
	 * Computes all the data for the chart
	 * 
	 * @return chart drawing data
	 */
	private ArrayList<ChartDrawingData> createChartDrawingData() {

		// compute the graphs and axis
		final ArrayList<ChartDrawingData> chartDrawingData = new ArrayList<ChartDrawingData>();

		final ArrayList<ChartDataYSerie> yDataList = fChartDataModel.getYData();
		final ChartDataXSerie xData = fChartDataModel.getXData();
		final ChartDataXSerie xData2nd = fChartDataModel.getXData2nd();

		final int graphCount = yDataList.size();
		int graphIndex = 1;

		// loop all graphs
		for (final ChartDataYSerie yData : yDataList) {

			final ChartDrawingData drawingData = new ChartDrawingData(yData.getChartType());

			chartDrawingData.add(drawingData);

			// set chart title
			if (graphIndex == 1) {
				drawingData.setXTitle(fChartDataModel.getTitle());

				// set the chart title height and margin
				final String title = drawingData.getXTitle();
				final ChartSegments chartSegments = xData.getChartSegments();

				if (title != null && title.length() > 0 || //
						(chartSegments != null && chartSegments.segmentTitle != null)) {

					fDevXTitleBarHeight = TITLE_BAR_HEIGHT;
					devMarginTop = MARGIN_TOP_WITH_TITLE;
				}
			}

			// set x/y data
			drawingData.setXData(xData);
			drawingData.setXData2nd(xData2nd);
			drawingData.setYData(yData);

			// compute x/y values
			computeXValues(drawingData);
			computeYValues(drawingData, graphCount, graphIndex);

			// set values after they have been computed
			drawingData.setDevMarginTop(devMarginTop);
			drawingData.setDevXTitelBarHeight(fDevXTitleBarHeight);
			drawingData.setDevSliderBarHeight(devSliderBarHeight);
			drawingData.setDevMarkerBarHeight(devMarkerBarHeight);

			graphIndex++;
		}

		return chartDrawingData;
	}

	/**
	 * set the {@link SynchConfiguration} when this chart is the source for the synched chart
	 */
	private SynchConfiguration createSynchConfig() {

		final ChartDataXSerie xData = fChartDataModel.getXData();

		final int markerValueIndexStart = xData.getSynchMarkerStartIndex();
		final int markerValueIndexEnd = xData.getSynchMarkerEndIndex();

		if (markerValueIndexStart == -1) {

			// disable chart synchronization
			fSynchConfigOut = null;
			return null;
		}

		/*
		 * create synch configuration data
		 */

		final int[] xValues = xData.getHighValues()[0];
		final float markerStartValue = xValues[Math.min(markerValueIndexStart, xValues.length - 1)];
		final float markerEndValue = xValues[Math.min(markerValueIndexEnd, xValues.length - 1)];

		final float valueDiff = markerEndValue - markerStartValue;
		final float lastValue = xValues[xValues.length - 1];

		final float devVirtualGraphImageWidth = fComponentGraph.getDevVirtualGraphImageWidth();
		final float devGraphImageXOffset = fComponentGraph.getDevGraphImageXOffset();

		final float devOneValueSlice = devVirtualGraphImageWidth / lastValue;

		final float devMarkerWidth = (int) (valueDiff * devOneValueSlice);
		final float devMarkerStartPos = (int) (markerStartValue * devOneValueSlice);
		final float devMarkerOffset = (int) (devMarkerStartPos - devGraphImageXOffset);

		final int devVisibleChartWidth = getDevVisibleChartWidth();

		final float markerWidthRatio = devMarkerWidth / devVisibleChartWidth;
		final float markerOffsetRatio = devMarkerOffset / devVisibleChartWidth;

		// ---------------------------------------------------------------------------------------

		final SynchConfiguration synchConfig = new SynchConfiguration(fChartDataModel,
				devMarkerWidth,
				devMarkerOffset,
				markerWidthRatio,
				markerOffsetRatio);

		return synchConfig;
	}

	private void createXValuesDay(	final ChartDrawingData drawingData,
									final ArrayList<ChartUnit> units,
									final int devGraphWidth) {

		final Calendar calendar = GregorianCalendar.getInstance();
		final ChartSegments chartSegments = drawingData.getXData().getChartSegments();

		// get number of days for all years
		final int allValues = chartSegments.allValues;
		final int[] years = chartSegments.years;
		final int[] yearDays = chartSegments.yearDays;

		// shorten the unit when there is not enough space to draw the full unit name
		final GC gc = new GC(this);
		final int monthLength = gc.stringExtent(monthLabels[0]).x;
		final boolean useShortUnitLabel = monthLength > (devGraphWidth / (years.length * 12)) * 0.9;
		gc.dispose();

		int yearIndex = 0;
		int allDays = 0;

		/*
		 * create month units for all years
		 */
		for (final int year : years) {

			// create month units
			for (int month = 0; month < 12; month++) {

				calendar.set(year, month, 1);
				final int firstMonthDay = calendar.get(Calendar.DAY_OF_YEAR) - 1;

				String monthLabel = monthLabels[month];
				if (useShortUnitLabel) {
					monthLabel = monthLabel.substring(0, 1);
				}

				units.add(new ChartUnit(allDays + firstMonthDay, monthLabel));
			}

			allDays += yearDays[yearIndex++];
		}

		// compute the width of the rectangles
		drawingData.setBarRectangleWidth(Math.max(0, (devGraphWidth / allValues)));
		drawingData.setXUnitTextPos(ChartDrawingData.XUNIT_TEXT_POS_CENTER);

		drawingData.setScaleX((float) devGraphWidth / allValues);
	}

	private void createXValuesMonth(final ChartDrawingData drawingData,
									final ArrayList<ChartUnit> units,
									final int devGraphWidth) {

		final ChartDataYSerie yData = drawingData.getYData();
		final ChartDataXSerie xData = drawingData.getXData();

		final int months = xData.fHighValues[0].length;
		final float scaleX = (float) devGraphWidth / months;
		drawingData.setScaleX(scaleX);

		// shorten the unit when there is not enough space to draw the full unit name
		final GC gc = new GC(this);
		final int monthLength = gc.stringExtent(monthLabels[0]).x + 2;
		final boolean isShortUnitLabel = monthLength > (devGraphWidth / months);
		gc.dispose();

		// create the month labels
		for (int month = 0; month < months; month++) {

			String monthLabel = monthLabels[month % 12];
			if (isShortUnitLabel) {
				monthLabel = monthLabel.substring(0, 1);
			}

			final ChartUnit chartUnit = new ChartUnit(month, monthLabel);
			units.add(chartUnit);
		}

		// compute the width and position of the rectangles
		int barWidth;
		final int monthWidth = (int) Math.max(0, (scaleX) - 1);

		switch (yData.getChartLayout()) {
		case ChartDataYSerie.BAR_LAYOUT_SINGLE_SERIE:
		case ChartDataYSerie.BAR_LAYOUT_STACKED:
			// the bar's width is 50% of the width for a month
			barWidth = (int) Math.max(0, (monthWidth * 0.90f));
			drawingData.setBarRectangleWidth(barWidth);
			drawingData.setDevBarRectangleXPos((int) (Math.max(0, (monthWidth - barWidth) / 2) + 1));
			break;

		case ChartDataYSerie.BAR_LAYOUT_BESIDE:
			final int serieCount = yData.getHighValues()[0].length;

			// the bar's width is 75% of the width for a month
			barWidth = (int) Math.max(0, monthWidth / 4 * 3);
			drawingData.setBarRectangleWidth(Math.max(1, barWidth / serieCount));
			drawingData.setDevBarRectangleXPos((int) (Math.max(0, (monthWidth - barWidth) / 2) + 2));
		default:
			break;
		}

		drawingData.setXUnitTextPos(ChartDrawingData.XUNIT_TEXT_POS_CENTER);
	}

	private void createXValuesWeek(	final ChartDrawingData drawingData,
									final ArrayList<ChartUnit> units,
									final int devGraphWidth) {

		final ChartDataXSerie xData = drawingData.getXData();
		final int[] xValues = xData.getHighValues()[0];

		final int allWeeks = xValues.length;
		final int monthsInChart = allWeeks / 53 * 12;

		// shorten the unit when there is not enough space to draw the full unit name
		final GC gc = new GC(this);
		final int monthLabelLength = gc.stringExtent(monthLabels[0]).x + 2;
		final boolean isShortUnitLabel = monthLabelLength > (devGraphWidth / monthsInChart);
		gc.dispose();

		// create the month labels
		for (int month = 0; month < monthsInChart; month++) {

			String monthLabel = monthLabels[month % 12];
			if (isShortUnitLabel) {
				monthLabel = monthLabel.substring(0, 1);
			}

			final ChartUnit chartUnit = new ChartUnit(month, monthLabel);
			units.add(chartUnit);
		}

		// compute the width and position of the rectangles
		final int barWidth = (devGraphWidth / xValues.length) / 2;
		drawingData.setBarRectangleWidth(Math.max(0, barWidth));
		drawingData.setBarPosition(ChartDrawingData.BAR_POS_CENTER);

		drawingData.setScaleX((float) devGraphWidth / allWeeks);
		drawingData.setScaleUnitX((float) devGraphWidth / allWeeks * 53 / 12);
		drawingData.setXUnitTextPos(ChartDrawingData.XUNIT_TEXT_POS_CENTER);
	}

	private void createXValuesYear(	final ChartDrawingData drawingData,
									final ArrayList<ChartUnit> units,
									final int devGraphWidth) {

		final ChartDataYSerie yData = drawingData.getYData();
		final ChartDataXSerie xData = drawingData.getXData();

		final ChartSegments chartSegments = drawingData.getXData().getChartSegments();
		final int[] yearValues = chartSegments.years;

		final int yearCounter = xData.fHighValues[0].length;
		final float scaleX = (float) devGraphWidth / yearCounter;
		drawingData.setScaleX(scaleX);

		// create year units
		for (int yearIndex = 0; yearIndex < yearValues.length; yearIndex++) {
			units.add(new ChartUnit(yearIndex, Integer.toString(yearValues[yearIndex])));
		}

		// compute the width and position of the rectangles
		int barWidth;
		final int yearWidth = (int) Math.max(0, (scaleX) - 1);

		switch (yData.getChartLayout()) {
		case ChartDataYSerie.BAR_LAYOUT_SINGLE_SERIE:
		case ChartDataYSerie.BAR_LAYOUT_STACKED:

			// the bar's width is 50% of the width for a month
			barWidth = (int) Math.max(0, (yearWidth * 0.9f));
			drawingData.setBarRectangleWidth(barWidth);
			drawingData.setDevBarRectangleXPos((int) (Math.max(0, (yearWidth - barWidth) / 2) + 1));
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

			drawingData.setDevBarRectangleXPos((int) (Math.max(0, barPosition) + 0));
//				drawingData.setDevBarRectangleXPos(0);
//			}

		default:
			break;
		}

		drawingData.setXUnitTextPos(ChartDrawingData.XUNIT_TEXT_POS_CENTER);
	}

	ChartComponentAxis getAxisLeft() {
		return fComponentAxisLeft;
	}

	ChartComponentAxis getAxisRight() {
		return fComponentAxisRight;
	}

	ChartComponentGraph getChartComponentGraph() {
		return fComponentGraph;
	}

	ArrayList<ChartDrawingData> getChartDrawingData() {
		return fChartDrawingData;
	}

	ChartProperties getChartProperties() {
		return new ChartProperties();
	}

	/**
	 * @return Returns the visible chart graph height
	 */
	int getDevVisibleChartHeight() {
		if (fVisibleGraphRect == null) {
			return 100;
		}
		return fVisibleGraphRect.height;
	}

	/**
	 * @return Returns the visible chart graph width
	 */
	int getDevVisibleChartWidth() {
		if (fVisibleGraphRect == null) {
			return 100;
		}
		return fVisibleGraphRect.width;
	}

	void handleLeftRightEvent(final Event event) {

		switch (fChartDataModel.getChartType()) {
		case ChartDataModel.CHART_TYPE_BAR:
			selectBarItem(event);
			break;

		case ChartDataModel.CHART_TYPE_LINE:
			fComponentGraph.moveXSlider(event);
			break;

		default:
			break;
		}
	}

	/**
	 * Resize handler for all components, computes the chart when the chart data, client area has
	 * changed or the chart was zoomed
	 */
	boolean onResize() {

		if (fChartDataModel == null || getClientArea().width == 0) {
			return false;
		}

		// compute the visual size of the graph
		setVisibleGraphRect();

		if (setWidthToSynchedChart() == false) {

			// chart is not synchronized, compute the 'normal' graph width
			fComponentGraph.updateImageWidthAndOffset();
		}

		// compute the chart data
		fChartDrawingData = createChartDrawingData();

		// notify components about the new configuration
		fComponentGraph.setDrawingData(fChartDrawingData);

		// resize the sliders after the drawing data have changed and the new
		// chart size is saved
		fComponentGraph.updateSlidersOnResize();

		// resize the axis
		fComponentAxisLeft.setDrawingData(fChartDrawingData, true);
		fComponentAxisRight.setDrawingData(fChartDrawingData, false);

		// synchronize chart
		final SynchConfiguration synchConfig = createSynchConfig();
		if (synchConfig != null) {
			synchronizeChart(synchConfig);
		}

		return true;
	}

	private void selectBarItem(final Event event) {

		fKeyDownCounter[0]++;
		final int[] selectedIndex = new int[] { Chart.NO_BAR_SELECTION };

		switch (event.keyCode) {
		case SWT.ARROW_RIGHT:
			selectedIndex[0] = fComponentGraph.selectNextBarItem();
			break;
		case SWT.ARROW_LEFT:
			selectedIndex[0] = fComponentGraph.selectPreviousBarItem();
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
					display.timerExec(DELAY_TIME, new Runnable() {

						final int	fRunnableKeyDownCounter	= fKeyDownCounter[0];

						public void run() {
							if (fRunnableKeyDownCounter == fKeyDownCounter[0]
									&& fRunnableKeyDownCounter != fLastKeyDownCounter[0]) {

								/*
								 * prevent redoing it, this happened when the selectNext/Previous
								 * Method took a long time when the chart was drawn
								 */
								fLastKeyDownCounter[0] = fRunnableKeyDownCounter;

								fChart.fireBarSelectionEvent(0, selectedIndex[0]);
							}
						}
					});
				}
			});
		}
	}

	/**
	 * @param isMarkerVisible
	 */
	void setMarkerVisible(final boolean isMarkerVisible) {
		devMarkerBarHeight = isMarkerVisible ? MARKER_BAR_HEIGHT : 0;
	}

	/**
	 * updates the chart data fDataModel and redraw the chart
	 * 
	 * @param fChartDataModel
	 * @throws ChartIsEmptyException
	 */
	void setModel(final ChartDataModel chartModel) {

		fChartDataModel = chartModel;

		if (onResize()) {
			/*
			 * resetting the sliders require that the drawing data are created, this is done in the
			 * onResize method
			 */
			if (devSliderBarHeight > 0) {
				fComponentGraph.resetSliders();
			}
		}
	}

	/**
	 * @param isSliderVisible
	 */
	void setSliderVisible(final boolean isSliderVisible) {

		devSliderBarHeight = isSliderVisible ? SLIDER_BAR_HEIGHT : 0;

		fComponentGraph.setXSliderVisible(isSliderVisible);
	}

	/**
	 * Set's a {@link SynchConfiguration}, this chart will then be sychronized with the chart which
	 * sets the synch config
	 * 
	 * @param fSynchConfigSrc
	 *        the xMarkerPosition to set
	 */
	void setSynchConfig(final SynchConfiguration synchConfigIn) {

		fSynchConfigSrc = synchConfigIn;

		onResize();
	}

	private void setVisibleGraphRect() {

		final ArrayList<ChartDataYSerie> yDataList = fChartDataModel.getYData();
		boolean isYTitle = false;

		// loop all graphs - find the title for the y-axis
		for (final ChartDataYSerie yData : yDataList) {
			if (yData.getYTitle() != null || yData.getUnitLabel() != null) {
				isYTitle = true;
				break;
			}
		}

		if (isYTitle) {

			yAxisWidthLeftWithTitle = yAxisWidthLeft + TITLE_BAR_HEIGHT;

			final GridData gl = (GridData) fComponentAxisLeft.getLayoutData();
			gl.widthHint = yAxisWidthLeftWithTitle;

			// relayout after the size was changed
			layout();
		}

		final Rectangle clientRect = getClientArea();

		// set the visible graph size
		fVisibleGraphRect = new Rectangle(yAxisWidthLeftWithTitle, 0, clientRect.width
				- (yAxisWidthLeftWithTitle + yAxisWidthRight), clientRect.height);
	}

	/**
	 * adjust the graph width to the synched chart
	 * 
	 * @return Returns <code>true</code> when the graph width was set
	 */
	private boolean setWidthToSynchedChart() {

		final ChartDataXSerie xData = fChartDataModel.getXData();
		final int markerStartIndex = xData.getSynchMarkerStartIndex();
		final int markerEndIndex = xData.getSynchMarkerEndIndex();

		// check if synchronization is disabled
		if (fSynchConfigSrc == null || markerStartIndex == -1) {
			return false;
		}

		// set min/max values from the source synched chart into this chart
		fSynchConfigSrc.getYDataMinMaxKeeper().setMinMaxValues(fChartDataModel);

		final int[] xValues = xData.getHighValues()[0];
		final float markerValueStart = xValues[markerStartIndex];

		final float valueDiff = xValues[markerEndIndex] - markerValueStart;
		final float valueLast = xValues[xValues.length - 1];

		final int devVisibleChartWidth = getDevVisibleChartWidth();

		final float devVirtualGraphImageWidth;
		final float graphZoomRatio;
		final int devGraphOffset;

		switch (fChart.fSynchMode) {
		case Chart.SYNCH_MODE_BY_SCALE:

			// get marker data from the synch source
			final float markerWidthRatio = fSynchConfigSrc.getMarkerWidthRatio();
			final float markerOffsetRatio = fSynchConfigSrc.getMarkerOffsetRatio();

			// virtual graph width
			final float devMarkerWidth = devVisibleChartWidth * markerWidthRatio;
			final float devOneValueSlice = devMarkerWidth / valueDiff;
			devVirtualGraphImageWidth = devOneValueSlice * valueLast;

			// graph offset
			final float devMarkerOffset = devVisibleChartWidth * markerOffsetRatio;
			final float devMarkerStart = devOneValueSlice * markerValueStart;
			devGraphOffset = (int) (devMarkerStart - devMarkerOffset);

			// zoom ratio
			graphZoomRatio = devVirtualGraphImageWidth / devVisibleChartWidth;

			fComponentGraph.setGraphImageWidth((int) devVirtualGraphImageWidth, devGraphOffset, graphZoomRatio);

			return true;

		case Chart.SYNCH_MODE_BY_SIZE:

			// get marker data from the synch source
			final float synchSrcDevMarkerWidth = fSynchConfigSrc.getDevMarkerWidth();
			final float synchSrcDevMarkerOffset = fSynchConfigSrc.getDevMarkerOffset();

			// virtual graph width
			devVirtualGraphImageWidth = valueLast / valueDiff * synchSrcDevMarkerWidth;

			// graph offset
			final int devLeftSynchMarkerPos = (int) (markerValueStart / valueLast * devVirtualGraphImageWidth);
			devGraphOffset = (int) (devLeftSynchMarkerPos - synchSrcDevMarkerOffset);

			// zoom ratio
			graphZoomRatio = devVirtualGraphImageWidth / devVisibleChartWidth;

			fComponentGraph.setGraphImageWidth((int) devVirtualGraphImageWidth, devGraphOffset, graphZoomRatio);

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

		if (fChartDataModel == null) {
			return;
		}

		final ChartXSlider leftSlider = fComponentGraph.getLeftSlider();
		final ChartXSlider rightSlider = fComponentGraph.getRightSlider();

		final int slider1ValueIndex = sliderPosition.getSlider1ValueIndex();
		final int slider2ValueIndex = sliderPosition.getSlider2ValueIndex();

		final int[] xValues = fChartDataModel.getXData().fHighValues[0];

		if (slider1ValueIndex == SelectionChartXSliderPosition.SLIDER_POSITION_AT_CHART_BORDER) {
			fComponentGraph.setXSliderValueIndex(leftSlider, 0);
		} else if (slider1ValueIndex != SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION) {
			fComponentGraph.setXSliderValueIndex(leftSlider, slider1ValueIndex);
		}

		if (slider2ValueIndex == SelectionChartXSliderPosition.SLIDER_POSITION_AT_CHART_BORDER) {
			fComponentGraph.setXSliderValueIndex(rightSlider, xValues.length - 1);
		} else if (slider2ValueIndex != SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION) {
			fComponentGraph.setXSliderValueIndex(rightSlider, slider2ValueIndex);
		}

		fComponentGraph.redraw();
	}

//	/**
//	 * set the {@link SynchConfiguration} when this chart is the source for the synched chart
//	 */
//	@SuppressWarnings("unused")
//	private void synchronizeChart_AdjustToSameSize() {
//
////		final ChartDataXSerie xData = fChartDataModel.getXData();
////
////		final int markerValueIndexStart = xData.getSynchMarkerStartIndex();
////		final int markerValueIndexEnd = xData.getSynchMarkerEndIndex();
////
////		if (markerValueIndexStart == -1) {
////
////			// disable chart synch
////			fSynchConfigOut = null;
////			return;
////		}
////
////		final int[] xValues = xData.getHighValues()[0];
////		final float markerStartValue = xValues[markerValueIndexStart];
////		final float markerEndValue = xValues[markerValueIndexEnd];
////
////		final float valueDiff = markerEndValue - markerStartValue;
////		final float lastValue = xValues[xValues.length - 1];
////
////		final float devVirtualGraphImageWidth = fComponentGraph.getDevVirtualGraphImageWidth();
////		final float devGraphImageXOffset = fComponentGraph.getDevGraphImageXOffset();
////
////		final float devOneValueSlice = devVirtualGraphImageWidth / lastValue;
////
////		final int devMarkerWidth = (int) (valueDiff * devOneValueSlice);
////		final int devMarkerStartPos = (int) (markerStartValue * devOneValueSlice);
////		final int devMarkerOffset = (int) (devMarkerStartPos - devGraphImageXOffset);
////
////		final SynchConfiguration newSynchConfigOut = new SynchConfiguration(devMarkerWidth,
////				devMarkerOffset,
////				fChartDataModel);
////
////		synchronizeChart(newSynchConfigOut);
//	}

	private void synchronizeChart(final SynchConfiguration newSynchConfigOut) {

		boolean fireEvent = false;

		if (fSynchConfigOut == null) {
			// synch new config
			fireEvent = true;
		} else if (fSynchConfigOut.isEqual(newSynchConfigOut) == false) {
			// synch when config changed
			fireEvent = true;
		}

		if (fireEvent) {

			// set new synch config
			fSynchConfigOut = newSynchConfigOut;

			fChart.synchronizeChart();
		}
	}

	void updateChartLayers() {

		if (fChartDrawingData == null) {
			return;
		}

		final ArrayList<ChartDataYSerie> yDataList = fChartDataModel.getYData();

		int iGraph = 0;

		// loop all graphs
		for (final ChartDataYSerie yData : yDataList) {
			fChartDrawingData.get(iGraph++).getYData().setCustomLayers(yData.getCustomLayers());
		}

		fComponentGraph.updateChartLayers();
	}

	// void zoomToXSlider(final SelectionChartXSliderPosition sliderPosition) {
	// fComponentGraph.zoomToXSliderPosition(sliderPosition);
	// onResize();
	// }

	/**
	 */
	void zoomIn() {

		if (devSliderBarHeight == 0) {
			fComponentGraph.zoomInWithoutSlider();
		} else {
			fComponentGraph.zoomInWithSlider();
		}
		onResize();
	}

	/**
	 */
	void zoomOut(final boolean updateChart) {
		fComponentGraph.zoomOut();
		if (updateChart) {
			onResize();
		}
	}

	void zoomWithParts(final int parts, final int position, final boolean scrollSmoothly) {
		fComponentGraph.zoomWithParts(parts, position, scrollSmoothly);
		onResize();
	}
}
