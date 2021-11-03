/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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

   public static final int         AXIS_UNIT_NUMBER                      = 10;
   public static final int         AXIS_UNIT_HOUR_MINUTE                 = 20;
   public static final int         AXIS_UNIT_HOUR_MINUTE_24H             = 21;
   public static final int         AXIS_UNIT_HOUR_MINUTE_SECOND          = 22;
   public static final int         AXIS_UNIT_MINUTE_SECOND               = 23;
   public static final int         AXIS_UNIT_HOUR_MINUTE_OPTIONAL_SECOND = 24;
   public static final int         AXIS_UNIT_HISTORY                     = 25;

   public static final int         X_AXIS_UNIT_MONTH                     = 30;
   public static final int         X_AXIS_UNIT_DAY                       = 40;
   public static final int         X_AXIS_UNIT_YEAR                      = 50;
   public static final int         X_AXIS_UNIT_WEEK                      = 100;
   public static final int         X_AXIS_UNIT_NUMBER_CENTER             = 200;
   public static final int         X_AXIS_UNIT_HOUR_MINUTE               = 202;
   public static final int         X_AXIS_UNIT_HISTORY                   = 500;

   public static final String      CHART_TYPE_BAR_ADJACENT               = "CHART_TYPE_BAR_ADJACENT"; //$NON-NLS-1$
   public static final String      CHART_TYPE_BAR_STACKED                = "CHART_TYPE_BAR_STACKED";  //$NON-NLS-1$

   /**
    * Default color, when default color is not set
    */
   private static RGB              DEFAULT_GRAPH_RGB                     = new RGB(0xFF, 0xA5, 0xCB);

   /**
    * divisor for highValues
    */
   protected int                   _valueDivisor                         = 1;

   /**
    * Number of digits which are displayed.
    */
   private int                     _displayedFractionalDigits            = 0;

   /**
    * min value which is used to draw the chart
    */
   protected double                _visibleMinValue;

   /**
    * max value which is used to draw the chart
    */
   protected double                _visibleMaxValue;

   /**
    * minimum value found in the provided values
    */
   double                          _originalMinValue;

   /**
    * maximum value found in the provided values
    */
   double                          _originalMaxValue;

   /**
    * Average for all positive values.
    */
   double                          _avgPositiveValue;

   /**
    * When <code>true</code> the minimum value is forced when the dataserie is displayed.
    */
   protected boolean               _isForceMinValue                      = false;
   protected boolean               _isForceMaxValue                      = false;

   protected double                _visibleMinValueForced;
   protected double                _visibleMaxValueForced;

   /**
    * Unit which is drawn on the x or y axis
    */
   private int                     _axisUnit                             = AXIS_UNIT_NUMBER;

   /**
    * Text label for the unit
    */
   private String                  _unitLabel                            = UI.EMPTY_STRING;

   /**
    * Text label for the chart data, e.g. distance, altitude, speed...
    */
   private String                  _label                                = UI.EMPTY_STRING;

   private HashMap<String, Object> _customData                           = new HashMap<>();

   /*
    * Bar colors are mainly used for bar graphs
    */
   private RGB[] _rgbBar_Gradient_Bright = new RGB[] { DEFAULT_GRAPH_RGB };
   private RGB[] _rgbBar_Gradient_Dark   = new RGB[] { DEFAULT_GRAPH_RGB };
   private RGB[] _rgbBar_Line            = new RGB[] { DEFAULT_GRAPH_RGB };

   /*
    * Graph colors are mainly used for line graphs, graph title or units
    */
   private RGB _rgbGraph_Gradient_Bright = DEFAULT_GRAPH_RGB;
   private RGB _rgbGraph_Gradient_Dark   = DEFAULT_GRAPH_RGB;
   private RGB _rgbGraph_Line            = DEFAULT_GRAPH_RGB;
   private RGB _rgbGraph_Text            = DEFAULT_GRAPH_RGB;

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

   public RGB[] getRgbBar_Gradient_Bright() {
      return _rgbBar_Gradient_Bright;
   }

   public RGB[] getRgbBar_Gradient_Dark() {
      return _rgbBar_Gradient_Dark;
   }

   public RGB[] getRgbBar_Line() {
      return _rgbBar_Line;
   }

   public RGB getRgbGraph_Gradient_Bright() {
      return _rgbGraph_Gradient_Bright;
   }

   public RGB getRgbGraph_Gradient_Dark() {
      return _rgbGraph_Gradient_Dark;
   }

   public RGB getRgbGraph_Line() {
      return _rgbGraph_Line;
   }

   /**
    * This color is mainly used to draw the graph tile, graph value or unit label
    *
    * @return
    */
   public RGB getRgbGraph_Text() {
      return _rgbGraph_Text;
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

   /**
    * Sets the unit for the x-axis, default is {@link #AXIS_UNIT_NUMBER}
    *
    * @param axisUnit
    */
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

   public void setDisplayedFractionalDigits(final int displayedFractionalDigits) {
      _displayedFractionalDigits = displayedFractionalDigits;
   }

   public void setLabel(final String label) {
      _label = label;
   }

   public void setRgbBar_Gradient_Bright(final RGB[] rgbGradient_Bright) {
      _rgbBar_Gradient_Bright = rgbGradient_Bright;
   }

   public void setRgbBar_Gradient_Dark(final RGB[] rgbGradient_Dark) {
      _rgbBar_Gradient_Dark = rgbGradient_Dark;
   }

   public void setRgbBar_Line(final RGB[] rgbLine) {
      _rgbBar_Line = rgbLine;
   }

   public void setRgbGraph_Gradient_Bright(final RGB rgbDefault_Gradient_Bright) {
      _rgbGraph_Gradient_Bright = rgbDefault_Gradient_Bright;
   }

   public void setRgbGraph_Gradient_Dark(final RGB rgbDefault_Gradient_Dark) {
      _rgbGraph_Gradient_Dark = rgbDefault_Gradient_Dark;
   }

   public void setRgbGraph_Line(final RGB rgbDefault_Line) {
      _rgbGraph_Line = rgbDefault_Line;
   }

   /**
    * This color is used to draw the graph tile, graph value or unit label
    *
    * @param rgbGraph_Text
    */
   public void setRgbGraph_Text(final RGB rgbGraph_Text) {
      _rgbGraph_Text = rgbGraph_Text;
   }

   /**
    * @param unit
    *           The measurement to set.
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

   public void setVisibleMinValue(final double minValue) {

// debug: check Nan
//    if (minValue != minValue) {
//       int a = 0;
//       a++;
//    }

      _visibleMinValue = minValue;
   }

}
