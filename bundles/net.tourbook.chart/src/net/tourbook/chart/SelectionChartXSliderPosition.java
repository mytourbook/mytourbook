/*******************************************************************************
 * Copyright (C) 2005, 2022 Wolfgang Schramm and Contributors
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

import net.tourbook.common.UI;

import org.eclipse.jface.viewers.ISelection;

/**
 * Contains the value index for the sliders.
 */
public class SelectionChartXSliderPosition implements ISelection {

   private static final char NL                              = UI.NEW_LINE;

   public static final int   IGNORE_SLIDER_POSITION          = -1;
   public static final int   SLIDER_POSITION_AT_CHART_BORDER = -2;

   private int               _beforeLeftSliderIndex          = IGNORE_SLIDER_POSITION;
   private int               _leftSliderValueIndex           = IGNORE_SLIDER_POSITION;
   private int               _rightSliderValueIndex          = IGNORE_SLIDER_POSITION;

   /**
    * When <code>true</code> the slider will be positioned in the center of the chart.
    */
   private boolean           _isCenterSliderPosition         = false;

   /**
    * When <code>true</code> then the slider will be set visible in the chart by repositioning the
    * graph (this is the old default behavior), otherwise <code>false</code>.
    */
   private boolean           _isMoveChartToShowSlider        = true;

   private Chart             _chart;

   /**
    * When <code>true</code> the zoom position is set to the center of the chart that the next zoom
    * starts from the center of the sliders.
    */
   private boolean           _isCenterZoomPosition;

   private Object            _customData;

   public SelectionChartXSliderPosition(final Chart chart,
                                        final int leftValueIndex,
                                        final int rightValueIndex) {

      _chart = chart;

      _leftSliderValueIndex = leftValueIndex;
      _rightSliderValueIndex = rightValueIndex;
   }

   public SelectionChartXSliderPosition(final Chart chart,
                                        final int serieIndex0,
                                        final int serieIndex1,
                                        final int serieIndex2) {

      this(chart, serieIndex1, serieIndex2);

      _beforeLeftSliderIndex = serieIndex0;
   }

   public int getBeforeLeftSliderIndex() {
      return _beforeLeftSliderIndex;
   }

   public Chart getChart() {
      return _chart;
   }

   public Object getCustomData() {
      return _customData;
   }

   /**
    * @return Returns the value index for the left slider or {@link #IGNORE_SLIDER_POSITION} when
    *         this value index should not be used.
    */
   public int getLeftSliderValueIndex() {
      return _leftSliderValueIndex;
   }

   public int getRightSliderValueIndex() {
      return _rightSliderValueIndex;
   }

   public boolean isCenterSliderPosition() {
      return _isCenterSliderPosition;
   }

   public boolean isCenterZoomPosition() {
      return _isCenterZoomPosition;
   }

   @Override
   public boolean isEmpty() {
      return false;
   }

   /**
    * @return
    */
   public boolean isMoveChartToShowSlider() {
      return _isMoveChartToShowSlider;
   }

   public void setCenterSliderPosition(final boolean isCenterSliderPosition) {
      _isCenterSliderPosition = isCenterSliderPosition;
   }

   public void setCenterZoomPosition(final boolean isCenterZoomPosition) {
      _isCenterZoomPosition = isCenterZoomPosition;
   }

   public void setChart(final Chart chart) {
      _chart = chart;
   }

   public void setCustomData(final Object customData) {
      _customData = customData;
   }

   public void setMoveChartToShowSlider(final boolean isMoveChartToShowSlider) {
      _isMoveChartToShowSlider = isMoveChartToShowSlider;
   }

   @Override
   public String toString() {
      return UI.EMPTY_STRING

            + "SelectionChartXSliderPosition [" + NL //                       //$NON-NLS-1$

            + "_beforeLeftSliderIndex  =" + _beforeLeftSliderIndex + NL //    //$NON-NLS-1$
            + "_leftSliderValueIndex   =" + _leftSliderValueIndex + NL //     //$NON-NLS-1$
            + "_rightSliderValueIndex  =" + _rightSliderValueIndex + NL //    //$NON-NLS-1$
            + "_isCenterSliderPosition =" + _isCenterSliderPosition + NL //   //$NON-NLS-1$

//				+ "_chart=" + _chart + NL //
//				+ "_customData=" + _customData+ NL //

            + "]"; //$NON-NLS-1$
   }

}
