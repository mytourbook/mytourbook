/*******************************************************************************
 * Copyright (C) 2005, 2025 Wolfgang Schramm and Contributors
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
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StringUtils;
import net.tourbook.common.util.Util;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

/**
 * Chart widget which represents the chart UI.
 * <p>
 * The chart widget has the following heights:
 *
 * <pre>
 *  {@link #_devMarginTop}
 *  {@link #_devChartTitleHeight}
 *
 *  {@link #_devGraphLabelHeight}
 *  |#graph#
 *
 *  {@link #_devGraphVerticalDistance}
 *
 *  |{@link #_devGraphLabelHeight}
 *  |#graph#
 *
 *  {@link #_devGraphVerticalDistance}
 *
 *     ...
 *
 *  {@link #_devGraphLabelHeight}
 *  |#graph#
 *
 *  {@link #_devMarginBottom}
 * </pre>
 */
public class ChartComponents extends Composite {

   public static final int          BAR_SELECTION_DELAY_TIME    = 100;

   /**
    * min/max pixel widthDev/heightDev of the chart
    */
   static final int                 CHART_MIN_WIDTH             = 5;
   static final int                 CHART_MIN_HEIGHT            = 5;

   static final long                CHART_MAX_WIDTH             = 1_000_000_000_000L;  // seconds which is about 31'700 years
   static final int                 CHART_MAX_HEIGHT            = 10_000;

   static final int                 UNIT_TICK_SIZE              = 5;

   /**
    * Number of seconds in one day
    */
   private static final int         DAY_IN_SECONDS              = 24 * 60 * 60;
   private static final int         MONTH_IN_SECONDS            = 31 * DAY_IN_SECONDS;
   private static final int         YEAR_IN_SECONDS             = 366 * DAY_IN_SECONDS;

   private static final String[]    _monthLabels                = {

         Messages.Month_jan,
         Messages.Month_feb,
         Messages.Month_mar,
         Messages.Month_apr,
         Messages.Month_may,
         Messages.Month_jun,
         Messages.Month_jul,
         Messages.Month_aug,
         Messages.Month_sep,
         Messages.Month_oct,
         Messages.Month_nov,
         Messages.Month_dec
   };

   private static final String[]    _monthShortLabels           = {

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
         Integer.toString(12)
   };

   private int                      _gcFontHeight;

   private final Chart              _chart;

   /**
    * Top margin of the chart (and all it's components)
    */
   private int                      _devMarginTop;

   /**
    * Height of the horizontal axis
    */
   private int                      _devMarginBottom;

   /**
    * Height of the title bar, 0 indicates that the title is not visible
    */
   private int                      _devChartTitleHeight;

   /**
    * Height of the graph label
    */
   private int                      _devGraphLabelHeight;

   /**
    * Vertical distance between two graphs
    */
   private int                      _devGraphVerticalDistance;

   /**
    * Contains the {@link SynchConfiguration} for the current chart and will be used from the chart
    * which is synchronized
    */
   SynchConfiguration               synchConfigOut;

   /**
    * When a {@link SynchConfiguration} is set, this chart will be synchronized with the chart
    * which set's the synch config
    */
   SynchConfiguration               synchConfigSrc;

   /**
    * Visible chart rectangle for all graphs but without y-axis
    */
   private Rectangle                _visibleAllGraphRect;

   final ChartComponentGraph        componentGraph;
   private final ChartComponentAxis _componentAxisLeft;
   private final ChartComponentAxis _componentAxisRight;

   private ChartDataModel           _chartDataModel;

   private ChartDrawingData         _chartDrawingData;

   /**
    * Width in pixel for all months in one year
    */
   private int                      _devAllMonthLabelWidth      = -1;
   private int                      _devAllMonthShortLabelWidth = -1;
   private int                      _devYearLabelWidth;

   private final int[]              _keyDownCounter             = new int[1];
   private final int[]              _lastKeyDownCounter         = new int[1];

   /**
    * this error message is displayed instead of the chart when it's not <code>null</code>
    */
   String                           errorMessage;

   private long                     _historyUnitStart_EpochMilli;
   private long                     _historyUnitDurationOrTime;

   private int[]                    _historyUnit_Years;

   /**
    * Contains number of days for each month
    */
   private int[][]                  _historyUnit_Months;

   /**
    * Contains days of year (DOY) for all years
    */
   private int[]                    _historyUnit_DOY;

   /**
    * Create and layout the components of the chart
    *
    * @param parent
    * @param style
    */
   ChartComponents(final Chart parent, final int style) {

      super(parent, style);

      _chart = parent;

      // set layout for this chart
      GridDataFactory.fillDefaults().grab(true, true).applyTo(this);
      GridLayoutFactory.fillDefaults().spacing(0, 0).numColumns(3).applyTo(this);

      {
         /*
          * Left axis
          */
         _componentAxisLeft = new ChartComponentAxis(parent, this);
         GridDataFactory.fillDefaults().grab(false, true)
               .hint(_chart.yAxisWidth, SWT.DEFAULT)
               .applyTo(_componentAxisLeft);
      }
      {
         /*
          * Graph canvas
          */
         componentGraph = new ChartComponentGraph(parent, this);
         GridDataFactory.fillDefaults().grab(true, true)
               .applyTo(componentGraph);
      }
      {
         /*
          * Right axis
          */
         _componentAxisRight = new ChartComponentAxis(parent, this);
         GridDataFactory.fillDefaults().grab(false, true)
               .hint(_chart.yAxisWidth, SWT.DEFAULT)
               .applyTo(_componentAxisRight);
      }

      _componentAxisLeft.setComponentGraph(componentGraph);
      _componentAxisRight.setComponentGraph(componentGraph);

      addListener();

      updateFontScaling();
   }

   private void addListener() {

      // this is the only resize listener for the whole chart
      addControlListener(ControlListener.controlResizedAdapter(controlEvent -> onResize()));
   }

   /**
    * Computes all the data for the chart
    *
    * @return chart drawing data
    */
   private ChartDrawingData createDrawingData() {

      // compute the graphs and axis
      final ArrayList<GraphDrawingData> allGraphDrawingData = new ArrayList<>();

      final ChartDrawingData chartDrawingData = new ChartDrawingData(allGraphDrawingData);

      chartDrawingData.chartDataModel = _chartDataModel;

      final ArrayList<ChartDataYSerie> allYData = _chartDataModel.getYData();
      final ChartDataXSerie xData = _chartDataModel.getXData();
      final ChartDataXSerie xData2nd = _chartDataModel.getXData2nd();

      final int numGraphs = allYData.size();
      int graphNumber = 1;

      // loop all graphs
      for (final ChartDataYSerie yData : allYData) {

         final GraphDrawingData graphDrawingData = new GraphDrawingData(chartDrawingData, yData.getChartType());

         allGraphDrawingData.add(graphDrawingData);

         // set chart title above the first graph
         if (graphNumber == 1) {

            final String chartTitle = _chartDataModel.getTitle();

            graphDrawingData.setXTitle(chartTitle);

            // set the chart title height
            final int xAxisUnit = xData.getAxisUnit();
            final ChartStatisticSegments chartSegments = xData.getChartSegments();

            final boolean isHistoryTitle = xAxisUnit == ChartDataSerie.X_AXIS_UNIT_HISTORY;

            final boolean isShowTitle = componentGraph.chartTitleSegmentConfig.isShowSegmentTitle;
            final boolean isChartTitleAvailable = chartTitle != null && chartTitle.trim().length() > 0;
            final boolean isSegmentTitleAvailable = chartSegments != null && chartSegments.segmentTitle != null;

            if (isHistoryTitle
                  || isShowTitle && (isChartTitleAvailable || isSegmentTitleAvailable)) {

               _devChartTitleHeight = _gcFontHeight;

            } else {

               _devChartTitleHeight = 0;
            }
         }

// SET_FORMATTING_OFF

         _devMarginTop              = 3;
         _devMarginBottom           = _gcFontHeight + UNIT_TICK_SIZE + 3;

         _devGraphLabelHeight       = _gcFontHeight;
         _devGraphVerticalDistance  = 5;

// SET_FORMATTING_ON

         // transfer symbol size
         graphDrawingData.setSymbolSize(yData.getSymbolSize());

         // set x/y data
         graphDrawingData.setXData(xData);
         graphDrawingData.setXData2nd(xData2nd);
         graphDrawingData.setYData(yData);

         // compute x/y values
         createDrawingData_X(graphDrawingData);
         createDrawingData_Y(graphDrawingData, numGraphs, graphNumber);

         // reset adjusted y-slider value
         yData.adjustedYValue = Float.MIN_VALUE;

         graphNumber++;
      }

      // set values after they have been computed
      chartDrawingData.devMarginTop = _devMarginTop;
      chartDrawingData.devVisibleChartWidth = getDevVisibleChartWidth();

      return chartDrawingData;
   }

   /**
    * Compute units for the x-axis and keep it in the drawingData object
    */
   private void createDrawingData_X(final GraphDrawingData drawingData) {

      final ChartDataXSerie xData = drawingData.getXData();

      final double graphMinValue = xData.getXAxisMinValueForced();

      final double xAxisEndValue = xData.getXAxisMaxValueForced();
      final double graphMaxValue = xAxisEndValue == Double.MIN_VALUE ? xData.getOriginalMaxValue() : xAxisEndValue;

      final double graphRange = graphMaxValue - graphMinValue;

      final long devVirtualGraphWidth = componentGraph.getXXDevGraphWidth();
      final double scaleX = devVirtualGraphWidth / graphRange;

      drawingData.devVirtualGraphWidth = devVirtualGraphWidth;
      drawingData.setScaleX(scaleX);

      final double[] variableX_Values = _chartDataModel.getVariableX_Values();
      if (variableX_Values != null && variableX_Values.length > 0) {

         final double lastVariableXValue = variableX_Values[variableX_Values.length - 1];
         final double scaleXVariable = devVirtualGraphWidth / lastVariableXValue;

         drawingData.setScaleX_Variable(scaleXVariable);
      }

      /*
       * Calculate the number of units which will be visible by dividing the visible length by the
       * minimum size which one unit should have in pixels
       */
      final long numDefaultUnits = devVirtualGraphWidth / _chart.gridHorizontalDistance;

      // unit raw value (not yet rounded) is the number in data values for one unit
      final double graphDefaultUnit = graphRange / Math.max(1, numDefaultUnits);

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

         createDrawingData_X__Default(drawingData, xData, graphDefaultUnit, unitType);
         break;
      }

      /*
       * Configure bars in the bar charts
       */
      if (_chartDataModel.getChartType() == ChartType.BAR) {

         final int numValues = xData.getHighValuesDouble()[0].length;

         if (unitType == ChartDataSerie.AXIS_UNIT_NUMBER || unitType == ChartDataSerie.AXIS_UNIT_HOUR_MINUTE) {

            final int barWidth = (int) ((devVirtualGraphWidth / numValues) * 0.8);

            drawingData.setBarRectangleWidth(Math.max(0, barWidth));
            drawingData.setBarPosition(GraphDrawingData.BAR_POS_CENTER);

         } else if (unitType == ChartDataSerie.X_AXIS_UNIT_NUMBER_CENTER) {

            /*
             * Set bar width that it is wide enough to overlap the next right bar, the
             * overlapped part will be removed in ChartComponentGraph.draw210BarGraph()
             */
            final float barWidth = ((float) devVirtualGraphWidth / (numValues - 1));
            final int barWidth2 = (int) (Math.max(1, barWidth) * 1.10);

            drawingData.setBarRectangleWidth(barWidth2);
            drawingData.setBarPosition(GraphDrawingData.BAR_POS_CENTER);
         }
      }
   }

   private void createDrawingData_X__Default(final GraphDrawingData drawingData,
                                             final ChartDataXSerie xData,
                                             final double graphDefaultUnit,
                                             final int unitType) {

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
      final double xStartValue = xData.getUnitStartValue();
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
       * cases this didn't occurred
       */
      double graphMinVisibleValue = xData.getVisibleMinValue();
      double graphMaxVisibleValue = xData.getVisibleMaxValue() + graphUnit;

      if (xData.isForceMinValue()) {
         graphMinVisibleValue = xData.getXAxisMinValueForced();
      }
      if (xData.isForceMaxValue()) {
         graphMaxVisibleValue = xData.getXAxisMaxValueForced();
      }

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

         if ((unitType == ChartDataSerie.AXIS_UNIT_HOUR_MINUTE_SECOND
               || unitType == ChartDataSerie.AXIS_UNIT_HOUR_MINUTE_OPTIONAL_SECOND) && xStartValue > 0) {

            /*
             * x-axis shows day time, start with 0:00 at midnight
             */

            unitLabelValue = unitLabelValue % DAY_IN_SECONDS;
         }

         final int valueDecimals = 3;
         final String unitLabel = net.tourbook.chart.Util.formatNumber(//
               unitLabelValue,
               unitType,
               valueDivisor,
               valueDecimals);

         final boolean isMajorValue = unitLabelValue % majorValue == 0;

         xUnits.add(new ChartUnit(unitPos, unitLabel, isMajorValue));

         // check for an infinity loop
         if (/* graphValue > graphMaxVisibleValue || */loopCounter++ > 10000) {
            break;
         }

         graphValue += graphUnit;
         graphValue = Util.roundValueToUnit(graphValue, graphUnit, false);
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

   private void createDrawingData_X_History(final GraphDrawingData graphDrawingData, final double graphDefaultUnit_Double) {

      final ChartDataXSerie xData = graphDrawingData.getXData();
      final double scaleX = graphDrawingData.getScaleX();

      final long graphMaxValue_Seconds = (long) xData.getOriginalMaxValue();
      final long graphDefaultUnit = (long) graphDefaultUnit_Double;

      // get start time with mills truncated
      final ZonedDateTime tourStartTime = xData.getHistoryStartDateTime().withNano(0);
      final ZonedDateTime tourEndTime = tourStartTime.plusSeconds(graphMaxValue_Seconds);

      final long tourStartTimeMilli = tourStartTime.toInstant().toEpochMilli();

      long unitStart_EpochMilli = tourStartTimeMilli;
      long unitDurationOrTime = graphMaxValue_Seconds;
      long firstUnit_Year = tourStartTime.getYear();
      long lastUnit_Year = tourEndTime.getYear();

      long roundedYearUnit = 0;
      long majorRoundedYearUnit = 0;
      final double dev1Year = scaleX * YEAR_IN_SECONDS;
      final double dev1Month = scaleX * MONTH_IN_SECONDS;

//      System.out.println(UI.timeStampNano() + " \t");
//      System.out.println(UI.timeStampNano() + " createDrawingData_X_History\t" + " start: " + tourStartTime);

      final double devTitleVisibleUnit = _devAllMonthLabelWidth * 1.2;

      final boolean isYearRounded = dev1Year < _devYearLabelWidth * 4;
      if (isYearRounded) {

         /*
          * Adjust years to the rounded values
          */

         final double unitYears = (double) graphDefaultUnit / YEAR_IN_SECONDS;

         roundedYearUnit = Util.roundSimpleNumberUnits((long) unitYears);
         majorRoundedYearUnit = Util.getMajorSimpleNumberValue(roundedYearUnit);

         final long firstHistoryYear = tourStartTime.getYear();

         // decrease min value when it does not fit to unit borders
         final long yearMinRemainder = firstHistoryYear % roundedYearUnit;
         final long yearMinValue = firstHistoryYear - yearMinRemainder;

         final long yearMaxValue = lastUnit_Year - (lastUnit_Year % roundedYearUnit) + roundedYearUnit;

         unitStart_EpochMilli = ZonedDateTime
               .of((int) yearMinValue, 1, 1, 0, 0, 0, 0, TimeTools.getDefaultTimeZone())
               .toInstant()
               .toEpochMilli();

         unitDurationOrTime = ZonedDateTime
               .of((int) yearMaxValue, 12, 31, 23, 59, 59, 999, TimeTools.getDefaultTimeZone())
               .toInstant()
               .toEpochMilli();

         firstUnit_Year = yearMinValue;
         lastUnit_Year = yearMaxValue;
      }

      /*
       * check if history units must be created, this is done only once for a tour to optimize it
       */
      if (unitStart_EpochMilli != _historyUnitStart_EpochMilli
            || unitDurationOrTime != _historyUnitDurationOrTime) {

         _historyUnitStart_EpochMilli = unitStart_EpochMilli;
         _historyUnitDurationOrTime = unitDurationOrTime;

         createHistoryUnits((int) firstUnit_Year, (int) lastUnit_Year);
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
      final ArrayList<ChartUnit> xUnitTitles = new ArrayList<>();

      final ArrayList<Long> titleValueStart = historyTitle.graphStart = new ArrayList<>();
      final ArrayList<Long> titleValueEnd = historyTitle.graphEnd = new ArrayList<>();
      final ArrayList<String> titleText = historyTitle.titleText = new ArrayList<>();

      final boolean isTimeSerieWithTimeZoneAdjustment = xData.isTimeSerieWithTimeZoneAdjustment();

//      DateTime graphTime = tourStartTime.plus(graphLeftBorder * 1000);
//      if (isTimeSerieWithTimeZoneAdjustment) {
//         if (graphTime.getMillis() > UI.beforeCET) {
//            graphTime = graphTime.minus(UI.BERLIN_HISTORY_ADJUSTMENT * 1000);
//         }
//      }
//
////      final int graphSecondsOfDay = graphTime.getSecondOfDay();
////      final DateTime graphNextDay = graphTime.plus((DAY_IN_SECONDS - graphSecondsOfDay) * 1000);
//
//      System.out.println(UI.timeStampNano());
//      System.out.println(UI.timeStampNano() + " tourStartTime " + tourStartTime);
//      System.out.println(UI.timeStampNano() + " graphTime     " + graphTime);
////      System.out.println(UI.timeStampNano() + " graphNextDay  " + graphNextDay);
//      System.out.println(UI.timeStampNano());

      if (isYearRounded) {

         /*
          * create units for rounded years
          */

//         System.out.println(UI.timeStampNano() + "\trounded years\t");

         graphDrawingData.setXUnitTextPos(GraphDrawingData.X_UNIT_TEXT_POS_LEFT);

         int historyYearIndex = 0;

         /*
          * start unit at the first day of the first year at 0:00:00, this is necessary that the
          * unit is positioned exactly
          */
         final int startDOY = tourStartTime.getDayOfYear();
         final int startDaySeconds = tourStartTime.get(ChronoField.SECOND_OF_DAY);

         final int startYear = tourStartTime.getYear();

         int yearIndex = 0;
         long graphYearOffset = 0;
         while (startYear > _historyUnit_Years[yearIndex]) {
            graphYearOffset += _historyUnit_DOY[yearIndex++] * DAY_IN_SECONDS;
         }

         long graphValue = -startDOY * DAY_IN_SECONDS - startDaySeconds - graphYearOffset;

         // loop: years
         while (graphValue <= graphMaxValue_Seconds) {

            long graphUnit = 0;

            for (int unitIndex = 0; unitIndex < roundedYearUnit; unitIndex++) {

               final int unitYearIndex = historyYearIndex + unitIndex;

               // graph unit = rounded years
               graphUnit += _historyUnit_DOY[unitYearIndex] * DAY_IN_SECONDS;
            }

            if (graphValue < graphLeftBorder
                  - graphUnit //
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
            final int yearValue = _historyUnit_Years[historyYearIndex];

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

//         System.out.println(UI.timeStampNano() + "\tyear/month\t");

         graphDrawingData.setTitleTextPos(GraphDrawingData.X_UNIT_TEXT_POS_CENTER);
         graphDrawingData.setXUnitTextPos(GraphDrawingData.X_UNIT_TEXT_POS_CENTER);

         int historyYearIndex = 0;

         // start unit at the first day of the first year at 0:00:00
         final int startDOY = tourStartTime.getDayOfYear();
         final int startSeconds = tourStartTime.get(ChronoField.SECOND_OF_DAY);
         long graphValue = -startDOY * DAY_IN_SECONDS - startSeconds;

         // loop: years
         while (graphValue <= graphMaxValue_Seconds) {

            // graph unit = 1 year
            final long graphUnit = _historyUnit_DOY[historyYearIndex] * DAY_IN_SECONDS;

            if (graphValue < graphLeftBorder
                  - graphUnit //
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
            final int[] historyMonthDays = _historyUnit_Months[historyYearIndex];

            /*
             * draw year tick
             */
            xUnits.add(new ChartUnit(graphValue + DAY_IN_SECONDS, UI.EMPTY_STRING, true));

            /*
             * draw year title
             */
            {
               final String yearLabel = Integer.toString(_historyUnit_Years[historyYearIndex]);

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

//         System.out.println(UI.timeStampNano() + "\tmonth/day");

         graphDrawingData.setTitleTextPos(GraphDrawingData.X_UNIT_TEXT_POS_CENTER);

         int historyYearIndex = 0;

         // start unit at the first day of the first year at 0:00:00
         final int startDOY = tourStartTime.getDayOfYear();
         final int startSeconds = tourStartTime.get(ChronoField.SECOND_OF_DAY);
         long graphValue = -startDOY * DAY_IN_SECONDS - startSeconds;

         monthLoop:

         // loop: months
         while (graphValue <= graphMaxValue_Seconds) {

            final int[] yearMonths = _historyUnit_Months[historyYearIndex];

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
                        + Integer.toString(_historyUnit_Years[historyYearIndex]);

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

//         System.out.println(UI.timeStampNano() + " day/seconds");

         graphDrawingData.setTitleTextPos(GraphDrawingData.X_UNIT_TEXT_POS_CENTER);
         graphDrawingData.setXUnitTextPos(GraphDrawingData.X_UNIT_TEXT_POS_LEFT);

         final long graphUnit = Util.roundTime24h(graphDefaultUnit);
         final long majorUnit = Util.getMajorTimeValue24(graphUnit);

         final int startSeconds = tourStartTime.get(ChronoField.SECOND_OF_DAY);
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
          * Create title units
          */
         long prevGraphUnitValue = Long.MIN_VALUE;

         for (final ChartUnit chartUnit : xUnitTitles) {

            if (chartUnit.isMajorValue) {

               final long currentGraphUnitValue = (long) chartUnit.value;

               if (prevGraphUnitValue != Long.MIN_VALUE) {

                  titleValueStart.add(prevGraphUnitValue);
                  titleValueEnd.add(currentGraphUnitValue - 1);

                  long graphDay = tourStartTimeMilli + prevGraphUnitValue * 1000;

                  if (isTimeSerieWithTimeZoneAdjustment) {

                     if (graphDay > UI.beforeCET) {
                        graphDay -= UI.BERLIN_HISTORY_ADJUSTMENT * 1000;
                     }
                  }

//                  private final DateTimeFormatter   _dtFormatter            = DateTimeFormat.forStyle("M-");   //$NON-NLS-1$

                  final String dayTitle = TimeTools.getZonedDateTime(graphDay).format(TimeTools.Formatter_Date_M);

                  titleText.add(dayTitle);
               }

               prevGraphUnitValue = currentGraphUnitValue;
            }
         }
      }

//      System.out.println(UI.timeStampNano() + " \t");
//
//      for (final ChartUnit xUnit : xUnits) {
//         System.out.println(UI.timeStampNano() + " \t" + xUnit);
//      }
//
//
//      for (final ChartUnit xUnit : xUnitTitles) {
//         System.out.println(UI.timeStampNano() + " \t" + xUnit);
//      }
//
//      for (int unitIndex = 0; unitIndex < titleText.size(); unitIndex++) {
//
//         System.out.println(UI.timeStampNano()
//               + ("\t" + titleText.get(unitIndex))
//               + ("\t" + (long) ((long) (titleValueStart.get(unitIndex) * scaleX) - devGraphXOffset))
//               + ("\t" + (long) ((long) (titleValueEnd.get(unitIndex) * scaleX) - devGraphXOffset))
//               + ("\t" + titleValueStart.get(unitIndex))
//               + ("\t" + titleValueEnd.get(unitIndex))
//         //
//               );
//      }
   }

   private void createDrawingData_X_Month(final GraphDrawingData drawingData) {

      final ChartDataXSerie xData = drawingData.getXData();
      final ChartDataYSerie yData = drawingData.getYData();

      final int allUnitsSize = xData._highValuesDouble[0].length;
      final long devVirtualGraphWidth = componentGraph.getXXDevGraphWidth();
      final double scaleX = (double) devVirtualGraphWidth / allUnitsSize;
      drawingData.setScaleX(scaleX);

      drawingData.setXUnitTextPos(GraphDrawingData.X_UNIT_TEXT_POS_CENTER);

      final int numberOfYears = xData.getChartSegments().segmentTitle.length;

      createMonthEqualUnits(drawingData, devVirtualGraphWidth, allUnitsSize, numberOfYears);

      // compute the width and position of the rectangles
      final int monthWidth = (int) Math.max(0, (scaleX) - 1);
      final int barWidth = (int) Math.max(0, (monthWidth * 0.90f));

      drawingData.setBarRectangleWidth(barWidth);
      drawingData.setDevBarRectangleXPos(Math.max(0, (monthWidth - barWidth) / 2) + 1);

      switch (yData.getChartLayout()) {
      case ChartDataYSerie.BAR_LAYOUT_SINGLE_SERIE:
      case ChartDataYSerie.BAR_LAYOUT_STACKED:

         drawingData.setBarRectangleWidth(barWidth);
         drawingData.setDevBarRectangleXPos((Math.max(0, (monthWidth - barWidth) / 2) + 1));

         break;

      case ChartDataYSerie.BAR_LAYOUT_BESIDE:

         final int serieCount = yData.getHighValuesFloat().length;

         final int singleBarWidth = Math.max(1, barWidth / (serieCount - 0));
         final int barPosition = (monthWidth - (singleBarWidth * (serieCount - 0))) / 2;

         drawingData.setBarRectangleWidth(singleBarWidth);
         drawingData.setDevBarRectangleXPos((Math.max(0, barPosition) + 0));

         break;

      default:
         break;
      }
   }

   private void createDrawingData_X_Week(final GraphDrawingData drawingData) {

      final ChartDataXSerie xData = drawingData.getXData();
      final ChartDataYSerie yData = drawingData.getYData();

      final long devVirtualGraphWidth = componentGraph.getXXDevGraphWidth();

      final double[] xValues = xData.getHighValuesDouble()[0];
      final int numWeeks = xValues.length;

      final double scaleX = (double) devVirtualGraphWidth / numWeeks;

      final ChartStatisticSegments chartSegments = drawingData.getXData().getChartSegments();

      final int[] yearDays = chartSegments.yearDays;

      int allDaysInAllYears = 0;
      for (final int days : yearDays) {
         allDaysInAllYears += days;
      }

      createMonthUnequalUnits(drawingData, devVirtualGraphWidth, chartSegments.years, yearDays);

      final int weekWidth = (int) Math.max(0, scaleX - 1);
      final int barWidth = (int) Math.max(0, weekWidth * 0.9f);

      drawingData.setBarPosition(GraphDrawingData.BAR_POS_CENTER);
      drawingData.setXUnitTextPos(GraphDrawingData.X_UNIT_TEXT_POS_CENTER);

      drawingData.setScaleX(scaleX);
      drawingData.setScaleUnitX((double) devVirtualGraphWidth / allDaysInAllYears);

      switch (yData.getChartLayout()) {
      case ChartDataYSerie.BAR_LAYOUT_SINGLE_SERIE:
      case ChartDataYSerie.BAR_LAYOUT_STACKED:

         drawingData.setBarRectangleWidth(barWidth);
         drawingData.setDevBarRectangleXPos((Math.max(0, (weekWidth - barWidth) / 2) + 1));

         break;

      case ChartDataYSerie.BAR_LAYOUT_BESIDE:

         final int serieCount = yData.getHighValuesFloat().length;

         final int singleBarWidth = Math.max(1, barWidth / (serieCount - 0));
         final int barPosition = (weekWidth - (singleBarWidth * (serieCount - 0))) / 2;

         drawingData.setBarRectangleWidth(singleBarWidth);
         drawingData.setDevBarRectangleXPos((Math.max(0, barPosition) + 0));

         break;

      default:
         break;
      }
   }

   private void createDrawingData_X_Year(final GraphDrawingData drawingData) {

      final ChartDataXSerie xData = drawingData.getXData();
      final ChartDataYSerie yData = drawingData.getYData();

      final ChartStatisticSegments chartSegments = drawingData.getXData().getChartSegments();
      final int[] yearValues = chartSegments.years;
      final long devVirtualGraphWidth = componentGraph.getXXDevGraphWidth();

      final int numYears = xData._highValuesDouble[0].length;
      final double scaleX = (double) devVirtualGraphWidth / numYears;
      drawingData.setScaleX(scaleX);

      drawingData.setXUnitTextPos(GraphDrawingData.X_UNIT_TEXT_POS_CENTER);

      // create year units
      final ArrayList<ChartUnit> xUnits = drawingData.getXUnits();
      for (int yearIndex = 0; yearIndex < yearValues.length; yearIndex++) {
         xUnits.add(new ChartUnit(yearIndex, Integer.toString(yearValues[yearIndex])));
      }

      // compute the width and position of the rectangles

      final int yearWidth = (int) Math.max(0, scaleX - 1);
      final int barWidth = (int) Math.max(0, yearWidth * 0.9f);

      switch (yData.getChartLayout()) {
      case ChartDataYSerie.BAR_LAYOUT_SINGLE_SERIE:
      case ChartDataYSerie.BAR_LAYOUT_STACKED:

         drawingData.setBarRectangleWidth(barWidth);
         drawingData.setDevBarRectangleXPos((Math.max(0, (yearWidth - barWidth) / 2) + 1));

         break;

      case ChartDataYSerie.BAR_LAYOUT_BESIDE:

         final int serieCount = yData.getHighValuesFloat().length;

         final int singleBarWidth = Math.max(1, barWidth / (serieCount - 0));
         final int barPosition = (yearWidth - (singleBarWidth * (serieCount - 0))) / 2;

         drawingData.setBarRectangleWidth(singleBarWidth);
         drawingData.setDevBarRectangleXPos((Math.max(0, barPosition) + 0));

         break;

      default:
         break;
      }
   }

   /**
    * computes data for the y axis
    *
    * @param drawingData
    * @param graphCount
    * @param graphNumber
    */
   private void createDrawingData_Y(final GraphDrawingData drawingData,
                                    final int graphCount,
                                    final int graphNumber) {

      final int unitType = drawingData.getYData().getAxisUnit();

      if (unitType == ChartDataSerie.AXIS_UNIT_NUMBER) {

         createDrawingData_Y_Numbers(drawingData, graphCount, graphNumber);

      } else if (unitType == ChartDataSerie.AXIS_UNIT_HOUR_MINUTE
            || unitType == ChartDataSerie.AXIS_UNIT_HOUR_MINUTE_24H
            || unitType == ChartDataSerie.AXIS_UNIT_HOUR_MINUTE_SECOND
            || unitType == ChartDataSerie.AXIS_UNIT_MINUTE_SECOND) {

         createDrawingData_Y_Time(drawingData, graphCount, graphNumber);

      } else if (unitType == ChartDataSerie.AXIS_UNIT_HISTORY) {

         createDrawingData_Y_History(drawingData, graphCount, graphNumber);
      }
   }

   private void createDrawingData_Y_History(final GraphDrawingData graphDrawingData,
                                            final int numGraphs,
                                            final int graphNumber) {

      // height of one chart graph including the slider bar
      final int devChartHeight = getDevChartHeightWithoutTrim();

      int devGraphHeight = devChartHeight;
      final boolean isChartStacked = _chartDataModel.isGraphOverlapped() == false;

      /*
       * adjust graph device height for stacked graphs, a gap is between two graphs
       */
      if (isChartStacked && numGraphs > 1) {
         final int devGraphHeightSpace = devGraphHeight - (_devGraphVerticalDistance * (numGraphs - 1));
         devGraphHeight = (devGraphHeightSpace / numGraphs);
      }

      // enforce minimum chart height
      devGraphHeight = Math.max(devGraphHeight, CHART_MIN_HEIGHT);

      // remove slider bar from graph height
      devGraphHeight -= _devGraphLabelHeight;

      // calculate the vertical device offset
      int devYTop = _devMarginTop + _devChartTitleHeight;

      if (isChartStacked) {

         // each chart has its own drawing rectangle which are stacked on top of each other

         devYTop += (graphNumber * (devGraphHeight + _devGraphLabelHeight))
               + ((graphNumber - 1) * _devGraphVerticalDistance);

      } else {
         // all charts are drawn on the same rectangle
         devYTop += devGraphHeight;
      }

      graphDrawingData.setDevYBottom(devYTop);
      graphDrawingData.setDevYTop(devYTop - devGraphHeight);

      graphDrawingData.devGraphHeight = devGraphHeight;
   }

   /**
    * @param graphDrawingData
    * @param numGraphs
    * @param graphNumber
    *           This is starting from 1
    */
   private void createDrawingData_Y_Numbers(final GraphDrawingData graphDrawingData,
                                            final int numGraphs,
                                            final int graphNumber) {

      final ChartDataYSerie yData = graphDrawingData.getYData();
      final int unitType = yData.getAxisUnit();

      // height of one graph including the unit label
      final int devAllGraphHeight = getDevChartHeightWithoutTrim();

      int devOneGraphHeight = devAllGraphHeight;

      final boolean isChartStacked = _chartDataModel.isGraphOverlapped() == false;

      /*
       * Adjust graph device height for stacked graphs, a gap is between two graphs
       */
      if (isChartStacked && numGraphs > 1) {

         final int devGraphHeightSpace = devOneGraphHeight - (_devGraphVerticalDistance * (numGraphs - 1));

         devOneGraphHeight = (devGraphHeightSpace / numGraphs);
      }

      // enforce minimum chart height
      devOneGraphHeight = Math.max(devOneGraphHeight, CHART_MIN_HEIGHT);

      // remove unit label height from graph height
      devOneGraphHeight -= _devGraphLabelHeight;

      /*
       * All variables starting with graph... are containing data values from the graph which are
       * not scaled to the device
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
      final int numDefaultUnits = devOneGraphHeight / _chart.gridVerticalDistance;

      // defaultUnitValue is the number in data values for one unit
      final double defaultUnitValue = defaultValueRange / Math.max(1, numDefaultUnits);

      // round the unit
      final double graphUnit = Util.roundDecimalValue(defaultUnitValue);

      /*
       * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
       * The scaled unit with long min/max values is used because arithmetic with floating point
       * values fails. BigDecimal is necessary otherwise the scaledUnit can be wrong !!!
       * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
       */
      final long valueScaling = Util.getValueScaling(graphUnit);

      final BigDecimal bigGraphUnit = BigDecimal.valueOf(graphUnit);
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
      final double graphScaleY = devOneGraphHeight / value1;

      // calculate the vertical device offset
      int devYBottom = _devMarginTop + _devChartTitleHeight + 0;

      if (_chartDataModel.isGraphOverlapped()) {

         // all charts are drawn at the same rectangle
         devYBottom += devOneGraphHeight + _devGraphLabelHeight;

      } else {

         // each chart has its own drawing rectangle which are stacked on
         // top of each other
         devYBottom += (graphNumber * (devOneGraphHeight + _devGraphLabelHeight))
               + ((graphNumber - 1) * _devGraphVerticalDistance);
      }

      graphDrawingData.setScaleY(graphScaleY);

      graphDrawingData.setDevYBottom(devYBottom);
      graphDrawingData.setDevYTop(devYBottom - devOneGraphHeight);

      graphDrawingData.setGraphYBottom((float) scaledMinValue / valueScaling);
      graphDrawingData.setGraphYTop((float) scaledMaxValue / valueScaling);

      graphDrawingData.devGraphHeight = devOneGraphHeight;

      final ArrayList<ChartUnit> allYUnits = graphDrawingData.getYUnits();
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
               false,
               graphUnit);

         final boolean isMajorValue = descaledValue % majorValue == 0;

         allYUnits.add(new ChartUnit((float) descaledValue, unitLabel, isMajorValue));

         // prevent endless loops when the unit is 0
         if (scaledValue == scaledMaxValue || loopCounter++ > 1000) {
            break;
         }

         scaledValue += scaledUnit;
      }

      if (unitType == ChartDataSerie.AXIS_UNIT_HOUR_MINUTE_24H && scaledValue > scaledMaxValue) {
         allYUnits.add(new ChartUnit(scaledMaxValue / valueScaling, UI.EMPTY_STRING));
      }
   }

   private void createDrawingData_Y_Time(final GraphDrawingData graphDrawingData,
                                         final int numGraphs,
                                         final int graphNumber) {

      final ChartDataYSerie yData = graphDrawingData.getYData();

      // height of one chart graph including the slider bar
      final int devChartHeight = getDevChartHeightWithoutTrim();

      int devGraphHeight = devChartHeight;
      final boolean isChartStacked = _chartDataModel.isGraphOverlapped() == false;

      // adjust graph device height for stacked graphs, a gap is between two
      // graphs
      if (isChartStacked && numGraphs > 1) {
         final int devGraphHeightSpace = (devGraphHeight - (_devGraphVerticalDistance * (numGraphs - 1)));
         devGraphHeight = (devGraphHeightSpace / numGraphs);
      }

      // enforce minimum chart height
      devGraphHeight = Math.max(devGraphHeight, CHART_MIN_HEIGHT);

      // remove slider bar from graph height
      devGraphHeight -= _devGraphLabelHeight;

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
      long graphUnit = (long) Util.roundTimeValue(//
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

         // max value exceeds 24h

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
         graphMinValue = Math.max(0, (((int) yData.getVisibleMinValue() / 3600) * 3600));

         graphUnit = (graphMaxValue - graphMinValue) / unitCounter;
         graphUnit = (long) Util.roundTimeValue(graphUnit, unitType == ChartDataSerie.AXIS_UNIT_HOUR_MINUTE_24H);
      }

      graphValueRange = graphMaxValue > 0
            ? (graphMaxValue - graphMinValue) //
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
      int devYBottom = _devMarginTop + _devChartTitleHeight;

      if (_chartDataModel.isGraphOverlapped()) {

         // all charts are drawn at the same rectangle
         devYBottom += devGraphHeight + _devGraphLabelHeight;

      } else {
         // each chart has its own drawing rectangle which are stacked on
         // top of each other
         devYBottom += (graphNumber * (devGraphHeight + _devGraphLabelHeight))
               + ((graphNumber - 1) * _devGraphVerticalDistance);
      }

      graphDrawingData.setScaleY(graphScaleY);

      graphDrawingData.setDevYBottom(devYBottom);
      graphDrawingData.setDevYTop(devYBottom - devGraphHeight);

      graphDrawingData.setGraphYBottom(graphMinValue);
      graphDrawingData.setGraphYTop(graphMaxValue);

      graphDrawingData.devGraphHeight = devGraphHeight;

      final ArrayList<ChartUnit> allYUnits = graphDrawingData.getYUnits();
      int graphValue = graphMinValue;
      int maxUnits = 0;
      final int valueDivisor = yData.getValueDivisor();

      // loop: create unit label for all units
      while (graphValue <= graphMaxValue) {

         final String unitLabel = net.tourbook.chart.Util.formatValue(graphValue, unitType, valueDivisor, false, -1);
         final boolean isMajorValue = graphValue % majorValue == 0;

         allYUnits.add(new ChartUnit(graphValue, unitLabel, isMajorValue));

         // prevent endless loops when the unit is 0
         if (graphValue == graphMaxValue || maxUnits++ > 1000) {
            break;
         }

         graphValue += graphUnit;
      }

      if (unitType == ChartDataSerie.AXIS_UNIT_HOUR_MINUTE_24H && graphValue > graphMaxValue) {
         allYUnits.add(new ChartUnit(graphMaxValue, UI.EMPTY_STRING));
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

         xUnits.add(new ChartUnit(
               graphValue + monthValue + DAY_IN_SECONDS - 1,
               valueLabel,
               isMajorValue)
         //
         );
      }
   }

   /**
    * Create year, month and day units for the history graph
    *
    * @param firstYear
    * @param lastYear
    */
   private void createHistoryUnits(final int firstYear, int lastYear) {

      /*
       * add an additional year because at the year end, a history chart displays also the next
       * year which caused an outOfBound exception when testing this app at 28.12.2012
       */
      lastYear += 1;

      final int numYears = lastYear - firstYear + 1;

      _historyUnit_Years = new int[numYears];
      _historyUnit_Months = new int[numYears][12];
      _historyUnit_DOY = new int[numYears];

      int yearIndex = 0;

      ZonedDateTime currentYear = ZonedDateTime.of(firstYear - 1, 1, 1, 0, 0, 0, 0, TimeTools.getDefaultTimeZone());

      for (int currentYearNo = firstYear; currentYearNo <= lastYear; currentYearNo++) {

         currentYear = currentYear.plusYears(1);

         _historyUnit_Years[yearIndex] = currentYearNo;

         int yearDOY = 0;

         // get number of days for each month
         for (int monthIndex = 0; monthIndex < 12; monthIndex++) {

            final int monthDays = (int) currentYear
                  .withMonth(monthIndex + 1)
                  .range(ChronoField.DAY_OF_MONTH)
                  .getMaximum();

            _historyUnit_Months[yearIndex][monthIndex] = monthDays;

            yearDOY += monthDays;
         }

         _historyUnit_DOY[yearIndex] = yearDOY;

         yearIndex++;
      }
   }

   private void createMonthEqualUnits(final GraphDrawingData drawingData,
                                      final long devGraphWidth,
                                      final int allUnitsSize,
                                      final int numberOfYears) {

      final ArrayList<ChartUnit> xUnits = drawingData.getXUnits();
      final boolean[] isDrawUnits = new boolean[allUnitsSize];

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
    *           Number of days in one year
    */
   private void createMonthUnequalUnits(final GraphDrawingData drawingData,
                                        final long devGraphWidth,
                                        final int[] years,
                                        final int[] yearDays) {

      /*
       * Multiple years can have different number of days but we assume that each year has the
       * same number of days to make it simpler
       */

      final int numberOfYears = years.length;
      final int numberOfMonths = numberOfYears * 12; // number of units

      final boolean[] isDrawUnits = new boolean[numberOfMonths];

      /*
       * Create a list with the day number for all years and months
       */
      final int[] daysForAllUnits = new int[numberOfMonths];
      int allDays = 0;
      for (int yearIndex = 0; yearIndex < numberOfYears; yearIndex++) {

         // create month units
         for (int monthIndex = 0; monthIndex < 12; monthIndex++) {

            final int firstDayInMonth = LocalDate//
                  .of(years[yearIndex], monthIndex + 1, 1)
                  .get(ChronoField.DAY_OF_YEAR) - 1;

            final int unitIndex = yearIndex * 12 + monthIndex;
            daysForAllUnits[unitIndex] = allDays + firstDayInMonth;
         }

         allDays += yearDays[yearIndex];
      }

      final ArrayList<ChartUnit> allXUnits = drawingData.getXUnits();

      /*
       * Create month labels depending on the available width for a unit
       */
      final int devYearWidth = (int) (devGraphWidth / numberOfYears);
      if (devYearWidth >= _devAllMonthLabelWidth) {

         // all month labels can be displayed

         for (int monthIndex = 0; monthIndex < numberOfMonths; monthIndex++) {

            final int unitValue = daysForAllUnits[monthIndex];

            allXUnits.add(new ChartUnit(unitValue, _monthLabels[monthIndex % 12]));

            isDrawUnits[monthIndex] = true;
         }

      } else if (devYearWidth >= _devAllMonthLabelWidth / 3) {

         // every second month label can be displayed

         for (int monthIndex = 0; monthIndex < numberOfMonths; monthIndex++) {

            final String monthLabel = monthIndex % 3 == 0 //
                  ? _monthLabels[monthIndex % 12]
                  : UI.EMPTY_STRING;

            final int unitValue = daysForAllUnits[monthIndex];

            allXUnits.add(new ChartUnit(unitValue, monthLabel));

//            isDrawUnits[monthIndex] = monthIndex % 3 == 0;
            isDrawUnits[monthIndex] = true;
         }

      } else if (devYearWidth >= _devAllMonthShortLabelWidth / 3) {

         // every second month short label can be displayed

         for (int monthIndex = 0; monthIndex < numberOfMonths; monthIndex++) {

            final String monthLabel = monthIndex % 3 == 0 //
                  ? _monthShortLabels[monthIndex % 12]
                  : UI.EMPTY_STRING;

            allXUnits.add(new ChartUnit(daysForAllUnits[monthIndex], monthLabel));
            isDrawUnits[monthIndex] = monthIndex % 3 == 0;
         }

      } else if (devYearWidth >= _devAllMonthShortLabelWidth / 6) {

         // every second month short label can be displayed

         for (int monthIndex = 0; monthIndex < numberOfMonths; monthIndex++) {

            final String monthLabel = monthIndex % 6 == 0 //
                  ? _monthShortLabels[monthIndex % 12]
                  : UI.EMPTY_STRING;

            allXUnits.add(new ChartUnit(daysForAllUnits[monthIndex], monthLabel));
            isDrawUnits[monthIndex] = monthIndex % 3 == 0;
         }

      } else {

         // width is too small to display month labels, display nothing

         for (int monthIndex = 0; monthIndex < numberOfMonths; monthIndex++) {
            allXUnits.add(new ChartUnit(daysForAllUnits[monthIndex], UI.EMPTY_STRING));
         }
      }

      // set state that the overlap is not checked again
      drawingData.setIsXUnitOverlapChecked(true);

      drawingData.setIsDrawUnit(isDrawUnits);

//      // shorten the unit when there is not enough space to draw the full unit name
//      final GC gc = new GC(this);
//      final int monthLength = gc.stringExtent(_monthLabels[0]).x;
//      final boolean useShortUnitLabel = monthLength > (devGraphWidth / allUnitsSize) * 0.9;
//      gc.dispose();
//
//      /*
//       * create month units for all years
//       */
//      for (int yearIndex = 0; yearIndex < years.length; yearIndex++) {
//
//         final int year = years[yearIndex];
//
//         // create month units
//         for (int month = 0; month < 12; month++) {
//
//            _calendar.set(year, month, 1);
//            final int firstDayInMonth = _calendar.get(Calendar.DAY_OF_YEAR) - 1;
//
//            String monthLabel = _monthLabels[month];
//            if (useShortUnitLabel) {
//               monthLabel = monthLabel.substring(0, 1);
//            }
//
//            units.add(new ChartUnit(allDays + firstDayInMonth, monthLabel));
//         }
//
//         allDays += yearDays[yearIndex];
//      }
   }

   /**
    * set the {@link SynchConfiguration} when this chart is the source for the synched chart
    */
   private SynchConfiguration createSynchConfig() {

      final ChartDataXSerie xData = _chartDataModel.getXData();

      final int xValueMarker_StartIndex = xData.getXValueMarker_StartIndex();
      final int xValueMarker_EndIndex = xData.getXValueMarker_EndIndex();

      if (xValueMarker_StartIndex == -1) {

         // disable chart synchronization
         synchConfigOut = null;
         return null;
      }

      /*
       * Create synch configuration data
       */

      final double[] xValues = xData.getHighValuesDouble()[0];
      final double markerStartValue = xValues[Math.min(xValueMarker_StartIndex, xValues.length - 1)];
      final double markerEndValue = xValues[Math.min(xValueMarker_EndIndex, xValues.length - 1)];

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
      return _componentAxisLeft;
   }

   public ChartComponentAxis getAxisRight() {
      return _componentAxisRight;
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

      return _visibleAllGraphRect.height

            - _devMarginTop
            - _devChartTitleHeight
            - _devMarginBottom;
   }

   int getDevChartMargin_Bottom() {

      return _devMarginBottom;
   }

   int getDevChartMargin_Top() {

      return _devMarginTop
            + _devChartTitleHeight
            + _devGraphLabelHeight;
   }

   /**
    * @return Returns the visible chart graph height
    */
   int getDevVisibleChartHeight() {

      if (_visibleAllGraphRect == null) {

         return 100;
      }

      return _visibleAllGraphRect.height;
   }

   /**
    * @return Returns the visible chart width
    */
   int getDevVisibleChartWidth() {

      if (_visibleAllGraphRect == null) {
         return 100;
      }

      return _visibleAllGraphRect.width;
   }

   int getMarginBottomStartingFromTop() {

      return _visibleAllGraphRect.height
            - _devMarginBottom;
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

      // compute the visual size of all graphs
      setVisibleAllGraphRect();

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
      _componentAxisLeft.setDrawingData(_chartDrawingData, true);
      _componentAxisRight.setDrawingData(_chartDrawingData, false);

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
         selectedIndex[0] = componentGraph.selectBarItem_Next();
         break;

      case SWT.ARROW_LEFT:
         selectedIndex[0] = componentGraph.selectBarItem_Previous();
         break;

      default:
         selectedIndex[0] = componentGraph.selectBarItem(event.keyCode);
         break;
      }

      // fire the event when the selection has changed
      if (selectedIndex[0] != Chart.NO_BAR_SELECTION) {

         /*
          * delay the change event when the key down was pressed several times
          */
         getDisplay().asyncExec(() -> getDisplay().timerExec(BAR_SELECTION_DELAY_TIME, new Runnable() {

            final int __runnableKeyDownCounter = _keyDownCounter[0];

            @Override
            public void run() {
               if (__runnableKeyDownCounter == _keyDownCounter[0]
                     && __runnableKeyDownCounter != _lastKeyDownCounter[0]) {

                  /*
                   * prevent redoing it, this happened when the selectNext/Previous
                   * Method took a long time when the chart was drawn
                   */
                  _lastKeyDownCounter[0] = __runnableKeyDownCounter;

                  _chart.fireEvent_BarSelection(0, selectedIndex[0]);
               }
            }
         }));
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
       * When data model has changed, update the visible y-values to use the full visible area for
       * drawing the chart
       */
      final ChartType chartType = _chartDataModel.getChartType();
      if (isShowAllData
            && (chartType == ChartType.LINE
                  || chartType == ChartType.LINE_WITH_BARS
                  || chartType == ChartType.LINE_WITH_GAPS
                  || chartType == ChartType.HISTORY)) {

         componentGraph.updateVisibleMinMaxValues();
      }

      /*
       * resetting the sliders require that the drawing data are created, this is done in the
       * onResize method
       */
      if (onResize()) {
         if (_devGraphLabelHeight > 0) {
            componentGraph.resetSliders();
         }
      }
   }

   /**
    * @param isSliderVisible
    */
   void setSliderVisible(final boolean isSliderVisible) {

      componentGraph.setXSliderVisible(isSliderVisible);
   }

   /**
    * Set's a {@link SynchConfiguration}, this chart will then be synchronized with the chart which
    * sets the synch config
    *
    * @param synchConfigIn
    *           the xMarkerPosition to set
    */
   void setSynchConfig(final SynchConfiguration synchConfigIn) {

      synchConfigSrc = synchConfigIn;

      onResize();
   }

   /**
    * Set {@link #_visibleAllGraphRect}
    */
   private void setVisibleAllGraphRect() {

      final ArrayList<ChartDataYSerie> allYData = _chartDataModel.getYData();

      boolean isYUnitLabel = false;

      // loop all graphs - find labels for the y-axis
      for (final ChartDataYSerie yData : allYData) {

         if (StringUtils.hasContent(yData.getUnitLabel())) {

            isYUnitLabel = true;
            break;
         }
      }

      int yAxisWidthLeft = _chart.yAxisWidth;

      if (isYUnitLabel) {

         // add font "width"
         yAxisWidthLeft += _gcFontHeight;
      }

      final GridData gdLeft = (GridData) _componentAxisLeft.getLayoutData();
      final GridData gdRight = (GridData) _componentAxisRight.getLayoutData();

      gdLeft.widthHint = yAxisWidthLeft;
      gdRight.widthHint = _chart.yAxisWidth;

      // relayout after the layout size has been changed
      layout();

      /*
       * Set the visible graph size
       */
      final Rectangle clientRect = getClientArea();

      final int chartWidth = clientRect.width;
      final int chartHeight = clientRect.height;

      final int devGraphWidth = chartWidth - yAxisWidthLeft - _chart.yAxisWidth;

      _visibleAllGraphRect = new Rectangle(

            yAxisWidthLeft,
            0,

            devGraphWidth,
            chartHeight);
   }

   /**
    * adjust the graph width to the synched chart
    *
    * @return Returns <code>true</code> when the graph width was set
    */
   private boolean setWidthToSynchedChart() {

      final ChartDataXSerie xData = _chartDataModel.getXData();
      int markerStartIndex = xData.getXValueMarker_StartIndex();
      int markerEndIndex = xData.getXValueMarker_EndIndex();

      // check if synchronization is disabled
      if (synchConfigSrc == null || markerStartIndex == -1) {
         return false;
      }

      // set min/max values from the source synched chart into this chart
      synchConfigSrc.getYDataMinMaxKeeper().setMinMaxValues(_chartDataModel);

      final double[] xValues = xData.getHighValuesDouble()[0];
      final int numXValues = xValues.length;

      // check bounds to fix exception
      if (markerStartIndex >= numXValues) {
         markerStartIndex = numXValues - 1;
      }
      if (markerEndIndex >= numXValues) {
         markerEndIndex = numXValues - 1;
      }

      final double markerValueStart = xValues[markerStartIndex];

      final double valueDiff = xValues[markerEndIndex] - markerValueStart;
      final double valueLast = xValues[numXValues - 1];

      final int devVisibleChartWidth = getDevVisibleChartWidth();

      final double xxDevGraphWidth;
      final int xxDevViewPortOffset;
      final double graphZoomRatio;

      switch (_chart.chartSynchMode) {
      case BY_SCALE:

         // get marker data from the synch source
         final float markerWidthRatio = synchConfigSrc.getMarkerWidthRatio();
         final float markerOffsetRatio = synchConfigSrc.getMarkerOffsetRatio();

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

      case BY_SIZE:

         // get marker data from the synch source
         final float synchSrcDevMarkerWidth = synchConfigSrc.getDevMarkerWidth();
         final float synchSrcDevMarkerOffset = synchConfigSrc.getDevMarkerOffset();

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
      final boolean isCenterZoomPosition = sliderPosition.isCenterZoomPosition();
      final boolean isMoveChartToShowSlider = sliderPosition.isMoveChartToShowSlider();

      /*
       * Move left slider
       */
      if (slider1ValueIndex == SelectionChartXSliderPosition.SLIDER_POSITION_AT_CHART_BORDER) {

         componentGraph.moveXSlider(
               leftSlider,
               0,
               isCenterSliderPosition,
               isMoveChartToShowSlider,
               isFireEvent,
               isCenterZoomPosition);

      } else if (slider1ValueIndex != SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION) {

         componentGraph.moveXSlider(
               leftSlider,
               slider1ValueIndex,
               isCenterSliderPosition,
               isMoveChartToShowSlider,
               isFireEvent,
               isCenterZoomPosition);
      }

      /*
       * Move right slider
       */
      if (slider0ValueIndex != SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION) {

         // move second slider before the first slider

         componentGraph.moveXSlider(
               rightSlider,
               slider0ValueIndex,
               isCenterSliderPosition,
               isMoveChartToShowSlider,
               isFireEvent,
               isCenterZoomPosition);

      } else {

         // move right slider
         if (slider2ValueIndex == SelectionChartXSliderPosition.SLIDER_POSITION_AT_CHART_BORDER) {

            componentGraph.moveXSlider(
                  rightSlider,
                  xValues.length - 1,
                  isCenterSliderPosition,
                  isMoveChartToShowSlider,
                  isFireEvent,
                  isCenterZoomPosition);

         } else if (slider2ValueIndex != SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION) {

            componentGraph.moveXSlider(
                  rightSlider,
                  slider2ValueIndex,
                  isCenterSliderPosition,
                  isMoveChartToShowSlider,
                  isFireEvent,
                  isCenterZoomPosition);
         }
      }

      componentGraph.redraw();
   }

   private void synchronizeChart(final SynchConfiguration newSynchConfigOut) {

      boolean fireEvent = false;

      if (synchConfigOut == null) {

         // synch new config

         fireEvent = true;

      } else if (synchConfigOut.isEqual(newSynchConfigOut) == false) {

         // synch when config changed

         fireEvent = true;
      }

      if (fireEvent) {

         // set new synch config
         synchConfigOut = newSynchConfigOut;

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

   /**
    * This will set the chart default font which is used to calculate different margin values
    */
   public void updateFontScaling() {

      // the font size is differently scaled on an image than on "this" (the current canvas)
      final Image gcImage = new Image(this.getDisplay(), 50, 50);
      final GC gc = new GC(gcImage);
      {
         gc.setFont(UI.getUIDrawingFont());
         final Point stringExtent = gc.stringExtent("0123456789"); //$NON-NLS-1$

         _gcFontHeight = stringExtent.y;

         updateLabelWidth(gc);
      }
      gc.dispose();
      gcImage.dispose();
   }

   /**
    * Get size in pixel for the month labels and shortcuts
    */
   private void updateLabelWidth(final GC gc) {

      _devAllMonthLabelWidth = 0;
      _devAllMonthShortLabelWidth = 0;

      for (int monthIndex = 0; monthIndex < _monthLabels.length; monthIndex++) {

         _devAllMonthLabelWidth += gc.stringExtent(_monthLabels[monthIndex]).x + 6;
         _devAllMonthShortLabelWidth += gc.stringExtent(_monthShortLabels[monthIndex]).x + 12;
      }

      _devYearLabelWidth += gc.stringExtent("2222").x + 0;//$NON-NLS-1$
   }
}
