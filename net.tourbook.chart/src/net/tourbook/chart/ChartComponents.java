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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
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
	static final int					CHART_MIN_WIDTH				= 100;
	static final int					CHART_MIN_HEIGHT			= 100;
	static final int					CHART_MAX_WIDTH				= 0x7fff;
	static final int					CHART_MAX_HEIGHT			= 5000;

	static final int					SLIDER_BAR_HEIGHT			= 10;
	static final int					MARKER_BAR_HEIGHT			= 50;
	static final int					TITLE_BAR_HEIGHT			= 15;
	static final int					MARGIN_TOP_WITH_TITLE		= 5;
	static final int					MARGIN_TOP_WITHOUT_TITLE	= 10;

	private Chart						fChart;

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
	 * this contains an zoom marker position which can be read from outside of the current chart
	 */
	public ZoomMarkerPosition			zoomMarkerPositionOut		= null;

	/**
	 * the zoom marker position which is set from outside of the current chart. When the zoom marker
	 * is set, the graph will be sized to the width, which is stored in the zoom marker position
	 */
	ZoomMarkerPosition					zoomMarkerPositionIn		= null;

	/**
	 * visible chart rectangle
	 */
	private Rectangle					fVisibleGraphRect;

	private ChartComponentGraph			fComponentGraph;
	private ChartComponentAxis			fComponentAxisLeft;
	private ChartComponentAxis			fComponentAxisRight;

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
		gd=new GridData(SWT.FILL, SWT.FILL, true, true);
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
//		gd.widthHint = yAxisWidthLeft;
		fComponentAxisLeft.setLayoutData(gd);

		// center: create chart canvas
		fComponentGraph = new ChartComponentGraph(parent, this, SWT.NONE);
		gd=new GridData(SWT.FILL, SWT.FILL, true, true);
//		gd.widthHint = CHART_MIN_WIDTH;
		fComponentGraph.setLayoutData(gd);

		// right: create right axis canvas
		fComponentAxisRight = new ChartComponentAxis(parent, this, SWT.NONE);
		gd = new GridData(SWT.NONE, SWT.FILL, false, true);
//		gd.widthHint = yAxisWidthRight;
		fComponentAxisRight.setLayoutData(gd);

		addListener();
	}

	private void addListener() {

		// this is the only resize listener for the whole chart
		addControlListener(new ControlAdapter() {
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
			public void handleEvent(final Event e) {
				switch (e.detail) {
				case SWT.TRAVERSE_ESCAPE:
				case SWT.TRAVERSE_RETURN:
				case SWT.TRAVERSE_TAB_NEXT:
				case SWT.TRAVERSE_TAB_PREVIOUS:
				case SWT.TRAVERSE_PAGE_NEXT:
				case SWT.TRAVERSE_PAGE_PREVIOUS:
					e.doit = true;
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
		final ChartDataYSerie yData = drawingData.getYData();

		final int xMaxValue = xData.getVisibleMaxValue();
		final int xAxisUnit = xData.getAxisUnit();
		final int xStartValue = xData.getStartValue();

		int devGraphWidth = fComponentGraph.getDevVirtualGraphImageWidth();
		
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

		final int monthLength = monthLabels.length;

		switch (xAxisUnit) {
		case ChartDataYSerie.AXIS_UNIT_HOUR_MINUTE_SECOND:
		case ChartDataYSerie.AXIS_UNIT_HOUR_MINUTE:
			unitValue = ChartUtil.roundTimeValue(unitRawValue);
			break;

		case ChartDataYSerie.AXIS_UNIT_NUMBER:
			unitValue = ChartUtil.roundDecimalValue(unitRawValue);
			break;

		case ChartDataYSerie.AXIS_UNIT_MONTH:
			createXValuesMonth(drawingData, monthLength, units, devGraphWidth, yData);
			break;

		case ChartDataYSerie.AXIS_UNIT_YEAR:
			// the maxValue contains the year which is displayed
			createXValuesYear(drawingData, monthLength, units, devGraphWidth, xMaxValue);
			break;

		default:
			break;
		}

		// create the units for the x-axis
		if (xAxisUnit != ChartDataYSerie.AXIS_UNIT_YEAR
				&& xAxisUnit != ChartDataYSerie.AXIS_UNIT_MONTH) {

			// get the unitOffset when a startValue is set
			int unitOffset = 0;
			if (xStartValue != 0) {
				unitOffset = (int) (xStartValue % unitValue);
			}

			final int valueDivisor = xData.getValueDivisor();
			int graphValue = 0;

			while (graphValue <= xMaxValue) {

				// create unit value/label
				final int unitPos = graphValue - unitOffset;
				units.add(new ChartUnit(unitPos, ChartUtil.formatValue(unitPos + xStartValue,
						xAxisUnit,
						valueDivisor,
						false)));

				graphValue += unitValue;
			}

		}

		// configure the bar in bar charts
		if (fChartDataModel.getChartType() == ChartDataModel.CHART_TYPE_BAR
				&& (xAxisUnit == ChartDataYSerie.AXIS_UNIT_NUMBER || xAxisUnit == ChartDataYSerie.AXIS_UNIT_HOUR_MINUTE)) {

			// if (axisUnit))) {
			//				
			// }
			// drawingData.setBarRectangleWidth(Math.max(0, (devGraphWidth /
			// xData
			// .getHighValues()[0].length) / 2));

			int barWidth = (devGraphWidth / xData.getHighValues()[0].length) / 2;

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
	private void computeYValues(final ChartDrawingData drawingData,
								final int graphCount,
								final int currentGraph) {

		final ChartDataYSerie yData = drawingData.getYData();

		final Point graphSize = fComponentGraph.getVisibleSizeWithHBar(fVisibleGraphRect.width,
				fVisibleGraphRect.height);

		// height of one chart graph including the slider bar
		int devGraphHeight = graphSize.y
				- devMarginTop
				- devMarkerBarHeight
				- fDevXTitleBarHeight
				- xAxisHeight;

		// adjust graph device height for stacked graphs, a gap is between two
		// graphs
		if (fChartDataModel.isStackedChart() && graphCount > 1) {
			final int devGraphHeightSpace = (devGraphHeight - (fChartsVerticalDistance * (graphCount - 1)));
			devGraphHeight = (int) (devGraphHeightSpace / graphCount);
		}

		// enforce minimum chart height
//		devGraphHeight = Math.max(devGraphHeight, CHART_MIN_HEIGHT);
		devGraphHeight = Math.max(devGraphHeight, 1);

		// remove slider bar from graph height
		devGraphHeight -= devSliderBarHeight;

		/*
		 * all variables starting with graph... contain data values from the graph which are not
		 * scaled to the device
		 */

		int graphMinValue = yData.getVisibleMinValue();
		// int graphMinValue = yData.getOriginalMinValue();
		int graphMaxValue = yData.getVisibleMaxValue();

		int graphValueRange = graphMaxValue > 0
				? (graphMaxValue - graphMinValue)
				: -(graphMinValue - graphMaxValue);

		/*
		 * calculate the number of units which will be visible by dividing the available height by
		 * the minimum size which one unit should have in pixels
		 */
		final int unitCount = devGraphHeight / fDevMinYUnit;

		// unitValue is the number in data values for one unit
		final int graphUnitValue = graphValueRange / Math.max(1, unitCount);

		// round the unit
		float unit = 0;
		final int axisUnit = yData.getAxisUnit();
		switch (axisUnit) {
		case ChartDataYSerie.AXIS_UNIT_HOUR_MINUTE:
		case ChartDataYSerie.AXIS_UNIT_HOUR_MINUTE_24H:
		case ChartDataYSerie.AXIS_UNIT_HOUR_MINUTE_SECOND:
			unit = ChartUtil.roundTimeValue(graphUnitValue);
			break;

		case ChartDataYSerie.AXIS_UNIT_NUMBER:
			// unit is a decimal number
			unit = ChartUtil.roundDecimalValue(graphUnitValue);
			break;
		}

		float adjustMinValue = 0;
		if (((float) graphMinValue % unit) != 0 && graphMinValue < 0) {
			adjustMinValue = unit;
		}
		graphMinValue = (int) ((int) ((graphMinValue - adjustMinValue) / unit) * unit);

		// adjust the min value so that bar graphs start at the bottom of the chart
		if (fChartDataModel.getChartType() == ChartDataModel.CHART_TYPE_BAR) {
			yData.setVisibleMinValue(graphMinValue);
		}

		// increase the max value when it does not fit to unit borders
		float adjustMaxValue = 0;
		if (((float) graphMaxValue % unit) != 0) {
			adjustMaxValue = unit;
		}
		graphMaxValue = (int) ((int) ((graphMaxValue + adjustMaxValue) / unit) * unit);

		// System.out.println(graphMinValue +" "+graphMaxValue);

		if (axisUnit == ChartDataYSerie.AXIS_UNIT_HOUR_MINUTE_24H && (graphMaxValue / 3600 > 24)) {

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
				graphValue += unit;
			}

			// adjust to 24h
			graphMaxValue = 24 * 3600;
			graphMaxValue = Math.min(24 * 3600,
					(((yData.getVisibleMaxValue()) / 3600) * 3600) + 3600);

			// adjust to the whole hour
			graphMinValue = Math.max(0, (((yData.getVisibleMinValue() / 3600) * 3600)));

			unit = (graphMaxValue - graphMinValue) / unitCounter;
			unit = ChartUtil.roundTimeValue((int) unit);
		}

		graphValueRange = graphMaxValue > 0
				? (graphMaxValue - graphMinValue)
				: -(graphMinValue - graphMaxValue);

		// calculate the device vertical scaling
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

		// loop: create unit label for all units
		while (graphValue <= graphMaxValue) {
			unitList.add(new ChartUnit(graphValue, ChartUtil.formatValue(graphValue,
					axisUnit,
					yData.getValueDivisor(),
					false)));

			// prevent endless loops when the unit is 0
			if (graphValue == graphMaxValue) {
				break;
			}

			graphValue += unit;
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

			final ChartDrawingData drawingData = new ChartDrawingData();
			chartDrawingData.add(drawingData);

			// set chart title
			if (graphIndex == 1) {
				drawingData.setXTitle(fChartDataModel.getTitle());

				// set the chart title height and margin
				final String title = drawingData.getXTitle();
				if (title != null && title.length() > 0) {
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

	private void createXValuesMonth(final ChartDrawingData drawingData,
									final int months,
									final ArrayList<ChartUnit> units,
									final int graphWidth,
									final ChartDataYSerie yData) {

		drawingData.setScaleX((float) graphWidth / months);

		// create the month units
		for (int month = 0; month < months; month++) {
			units.add(new ChartUnit(month, monthLabels[month]));
		}

		// compute the width and position of the rectangles
		int rectangleWidth;
		final int monthWidth = Math.max(0, (graphWidth / months) - 1);

		switch (yData.getChartLayout()) {
		case ChartDataYSerie.BAR_LAYOUT_SINGLE_SERIE:
		case ChartDataYSerie.BAR_LAYOUT_STACKED:
			// the bar's width is 50% of the width for a month
			rectangleWidth = Math.max(0, monthWidth / 2);
			drawingData.setBarRectangleWidth(rectangleWidth);
			drawingData.setBarRectanglePos(Math.max(0, rectangleWidth / 2) + 2);
			break;

		case ChartDataYSerie.BAR_LAYOUT_BESIDE:
			final int serieCount = yData.getHighValues()[0].length;

			// the bar's width is 75% of the width for a month
			rectangleWidth = Math.max(0, monthWidth / 4 * 3);
			drawingData.setBarRectangleWidth(Math.max(1, rectangleWidth / serieCount));
			drawingData.setBarRectanglePos(Math.max(0, (monthWidth - rectangleWidth) / 2) + 2);
		default:
			break;
		}

		drawingData.setXUnitTextPos(ChartDrawingData.XUNIT_TEXT_POS_CENTER);
	}

	private void createXValuesYear(	final ChartDrawingData drawingData,
									final int months,
									final ArrayList<ChartUnit> units,
									final int graphWidth,
									final int year) {

		final Calendar calendar = GregorianCalendar.getInstance();

		// get number of days for the year, start with 0
		calendar.set(year, 11, 31);
		final int yearDays = calendar.get(Calendar.DAY_OF_YEAR) - 1;

		drawingData.setScaleX((float) graphWidth / yearDays);

		// create the month units
		for (int month = 0; month < months; month++) {
			calendar.set(year, month, 1);
			final int firstMonthDay = calendar.get(Calendar.DAY_OF_YEAR) - 1;

			units.add(new ChartUnit(firstMonthDay, monthLabels[month]));
		}

		// compute the width of the rectangles
		drawingData.setBarRectangleWidth(Math.max(0, (graphWidth / yearDays)));
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

		if (setGraphWidthToZoomMarkerPosition() == false) {

			// compute the graph width when a zoom marker was not set
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

		// set the zoom marker position and fire the event to the listener
		setAndFireZoomMarkerPositionOut();

		return true;
	}

	/**
	 * set the zoom marker position which can be read from outside this chart
	 */
	private void setAndFireZoomMarkerPositionOut() {

		final ChartDataXSerie xData = fChartDataModel.getXData();

		final int markerValueIndexStart = xData.getXMarkerStartIndex();
		final int markerValueIndexEnd = xData.getXMarkerEndIndex();

		if (markerValueIndexStart == -1) {

			// no zoom marker's is set
			zoomMarkerPositionOut = null;
			return;
		}

		// zoom marker is set

		final int[] xValues = xData.getHighValues()[0];
		final float markerStartValue = xValues[markerValueIndexStart];
		final float markerEndValue = xValues[markerValueIndexEnd];

		final float valueDiff = markerEndValue - markerStartValue;
		final float lastValue = xValues[xValues.length - 1];

		final float devVirtualGraphImageWidth = fComponentGraph.getDevVirtualGraphImageWidth();

		final float devVirtualGraphOffset = fComponentGraph.getDevGraphImageXOffset();

		final int devXMarkerWidth = (int) (devVirtualGraphImageWidth / lastValue * valueDiff);

		final int devMarkerStartPos = (int) (markerStartValue / lastValue * devVirtualGraphImageWidth);
		final int devXMarkerOffset = (int) (devMarkerStartPos - devVirtualGraphOffset);

		final ZoomMarkerPosition newXMarkerPositionOut = new ZoomMarkerPosition(
				devXMarkerWidth,
				devXMarkerOffset,
				fChartDataModel);

		boolean fireEvent = false;
		if (zoomMarkerPositionOut == null) {
			// fire when the position is new
			fireEvent = true;
		} else if (!zoomMarkerPositionOut.isEqual(newXMarkerPositionOut)) {
			// fire when the position has changed
			fireEvent = true;
		}

		if (fireEvent) {

			// set a new zoom marker position
			zoomMarkerPositionOut = newXMarkerPositionOut;

			fChart.fireZoomMarkerPositionListener();
		}
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
	 * adjust the graph width and position to the zoom marker position
	 * 
	 * @return Returns <code>true</code> when the graph width was set
	 */
	private boolean setGraphWidthToZoomMarkerPosition() {

		final ChartDataXSerie xData = fChartDataModel.getXData();
		final int markerStartIndex = xData.getXMarkerStartIndex();
		final int markerEndIndex = xData.getXMarkerEndIndex();

		if (zoomMarkerPositionIn == null || markerStartIndex == -1) {
			// no zoom marker is set
			return false;
		}

		zoomMarkerPositionIn.yDataMinMaxKeeper.restoreMinMaxValues(fChartDataModel);

		final float xmpinDevMarkerWidth = zoomMarkerPositionIn.devMarkerWidth;
		final int xmpinDevMarkerOffset = zoomMarkerPositionIn.devMarkerOffset;

		final int[] xValues = xData.getHighValues()[0];
		final float valueMarkerStart = xValues[markerStartIndex];

		final float valueDiff = xValues[markerEndIndex] - valueMarkerStart;
		final float valueLast = xValues[xValues.length - 1];

		final float devVirtualGraphImageWidth = valueLast / valueDiff * xmpinDevMarkerWidth;
		final float graphZoomRatio = devVirtualGraphImageWidth / getDevVisibleChartWidth();

		final int devLeftXMarkerPos = (int) (valueMarkerStart / valueLast * devVirtualGraphImageWidth);
		final int devGraphOffset = devLeftXMarkerPos - xmpinDevMarkerOffset;

		fComponentGraph.setGraphImageWidth((int) devVirtualGraphImageWidth,
				devGraphOffset,
				graphZoomRatio);

		return true;
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
	 * set the x-sliders to a new position, this is done from a selection provider
	 * 
	 * @param position
	 */
	void setXSliderPosition(final SelectionChartXSliderPosition position) {

		if (position == null) {
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

		int slider1ValueIndex = position.slider1ValueIndex;
		int slider2ValueIndex = position.slider2ValueIndex;

		int[] xValues = fChartDataModel.getXData().fHighValues[0];

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

	/**
	 * Set the zoom-marker position, this is used when the chart is drawn/resized
	 * 
	 * @param zoomMarkerPositionIn
	 *        the xMarkerPosition to set
	 */
	void setZoomMarkerPositionIn(final ZoomMarkerPosition xMarkerPositionIn) {

		this.zoomMarkerPositionIn = xMarkerPositionIn;

		onResize();
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

	// void zoomToXSlider(final SelectionChartXSliderPosition sliderPosition) {
	// fComponentGraph.zoomToXSliderPosition(sliderPosition);
	// onResize();
	// }

	void zoomWithParts(final int parts, final int position) {
		fComponentGraph.zoomWithParts(parts, position);
		onResize();
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
}
