/*******************************************************************************
 * Copyright (C) 2005, 2019 Wolfgang Schramm and Contributors
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
/**
 * @author Wolfgang Schramm created 11.07.2005
 */
package net.tourbook.chart;

import net.tourbook.common.RectangleLong;

public class ChartYSlider {

   public static final int     SLIDER_TYPE_TOP    = 1;
   public static final int     SLIDER_TYPE_BOTTOM = 2;

   private final int           _sliderHitLine_Above_GraphHorizontalAxis;
   
   /**
    * Height of the slider line which can be hit by the mouse
    */
   private final int           _sliderHitLineHeight;

   private ChartDataYSerie     _yData;

   /**
    * rectangle where the slider can be hit
    */
   private final RectangleLong _hitRectangle;

   /**
    * y position for the slider line
    */
   private int                 _devYSliderLine    = 0;

   /**
    * offset between the top of the slider hit rectangle and the mouse hit within it
    */
   int                         devYClickOffset;

   private GraphDrawingData    _graphDrawingData;

   /**
    * Constructor
    */
   ChartYSlider(final ChartDataYSerie yData) {

      _yData = yData;

      if (yData.getChartType() == ChartType.BAR) {

         /*
          * The top hitline for the bar chart is below the graph horizontal axis otherwise the
          * tooltip for the bottom bars are NOT displayed.
          */

         _sliderHitLine_Above_GraphHorizontalAxis = 0;
         _sliderHitLineHeight = 5;

      } else {

         _sliderHitLine_Above_GraphHorizontalAxis = 10;
         _sliderHitLineHeight = 10 + _sliderHitLine_Above_GraphHorizontalAxis;
      }

      _hitRectangle = new RectangleLong(0, 0, 0, _sliderHitLineHeight);
   }

   /**
    * @return Returns the devYSliderLine.
    */
   public int getDevYSliderLine() {
      return _devYSliderLine;
   }

   /**
    * @return Returns the drawingData.
    */
   public GraphDrawingData getDrawingData() {
      return _graphDrawingData;
   }

   /**
    * @return Returns the hitRectangle.
    */
   public RectangleLong getHitRectangle() {
      return _hitRectangle;
   }

   /**
    * @return the _sliderHitLine_Above_GraphHorizontalAxis
    */
   public int getSliderHitLine_Above_GraphHorizontalAxis() {
      return _sliderHitLine_Above_GraphHorizontalAxis;
   }

   /**
    * @return Returns the yData.
    */
   public ChartDataYSerie getYData() {
      return _yData;
   }

   /**
    * Resize the slider after the chart was resizes
    *
    * @param drawingData
    */
   void handleChartResize(final GraphDrawingData drawingData, final int sliderType) {

      _graphDrawingData = drawingData;

      final int devGraphHeight = drawingData.devGraphHeight;
      final int devYBottom = drawingData.getDevYBottom();

      if (sliderType == SLIDER_TYPE_BOTTOM) {
         // set the slider and hit rectangle at the bottom of the chart
         _devYSliderLine = devYBottom;
      } else {
         // set the slider and hit rectangle at the top of the chart
         _devYSliderLine = devYBottom - devGraphHeight;
      }

      _hitRectangle.y = _devYSliderLine - _sliderHitLine_Above_GraphHorizontalAxis;
      _hitRectangle.width = drawingData.devVirtualGraphWidth - 1;
   }

   /**
    * set y value for the slider line, this is done when the mouse was moved
    *
    * @param devYSliderLine
    *           The devYSliderLine to set.
    */
   public void setDevYSliderLine(final int devYSliderLine) {

      _devYSliderLine = devYSliderLine;

      _hitRectangle.y = devYSliderLine - _sliderHitLine_Above_GraphHorizontalAxis;
   }

   @Override
   public String toString() {

      return "ChartYSlider " //$NON-NLS-1$
            + "[" //$NON-NLS-1$
            + "devYSliderLine=" + _devYSliderLine + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "devYClickOffset=" + devYClickOffset + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "hitRectangle=" + _hitRectangle //$NON-NLS-1$
            + "]"; //$NON-NLS-1$
   }
}
