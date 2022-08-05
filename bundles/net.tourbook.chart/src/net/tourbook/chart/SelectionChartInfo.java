/*******************************************************************************
 * Copyright (C) 2005, 2020 Wolfgang Schramm and Contributors
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

public class SelectionChartInfo implements ISelection {

   private static final char NL = UI.NEW_LINE;

   private Chart             _chart;

   public ChartDataModel     chartDataModel;
   public ChartDrawingData   chartDrawingData;

   public int                leftSliderValuesIndex;
   public int                rightSliderValuesIndex;

   /**
    * contains the value index for the slider which is selected
    */
   public int                selectedSliderValuesIndex;

   @SuppressWarnings("unused")
   private SelectionChartInfo() {}

   public SelectionChartInfo(final Chart chart) {
      _chart = chart;
   }

   public Chart getChart() {
      return _chart;
   }

   @Override
   public boolean isEmpty() {
      return false;
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "SelectionChartInfo" + NL //$NON-NLS-1$

            + "[" + NL //$NON-NLS-1$

//            + "   _chart                     =" + _chart + "" + NL //$NON-NLS-1$ //$NON-NLS-2$
//            + "   chartDataModel             =" + chartDataModel + "" + NL //$NON-NLS-1$ //$NON-NLS-2$
//            + "   chartDrawingData           =" + chartDrawingData + "" + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   leftSliderValuesIndex      =" + leftSliderValuesIndex + "" + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   rightSliderValuesIndex     =" + rightSliderValuesIndex + "" + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   selectedSliderValuesIndex  =" + selectedSliderValuesIndex + NL //$NON-NLS-1$

            + "]"; //$NON-NLS-1$
   }

}
