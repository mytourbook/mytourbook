/*******************************************************************************
 * Copyright (C) 2026 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.tourChart.action;

import net.tourbook.chart.Chart;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.tourChart.TourChartContextProvider;

import org.eclipse.jface.action.Action;

public class ActionSetGeoPositionFromHereToStart extends Action {

   private TourChartContextProvider _tourChartContextProvider;

   public ActionSetGeoPositionFromHereToStart(final TourChartContextProvider tourChartContextProvider) {

      super("Set &geo position from here into all previous time slices");

      _tourChartContextProvider = tourChartContextProvider;

      setToolTipText(
            "It takes the geo position from the selected time slice\nand sets this position into all previous time slices\nThe distance will be set to 0");
   }

   @Override
   public void run() {

      final Chart chart = _tourChartContextProvider.getChart();

      if (chart instanceof final TourChart tourChart) {

         tourChart.actionSetGeoPositionFromSelectedToAllPrevious(
               _tourChartContextProvider.getLeftSlider(),
               _tourChartContextProvider.getRightSlider());
      }

   }
}
