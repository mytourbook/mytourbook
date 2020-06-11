/*******************************************************************************
 * Copyright (C) 2020 Frédéric Bard
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

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.IChartContextProvider;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.common.util.Util;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.tourChart.TourChartConfiguration;
import net.tourbook.ui.views.tourDataEditor.TourDataEditorView;

import org.eclipse.jface.action.Action;

public class ActionSelectInBetweenTimeSlices extends Action {

   /**
    *
    */
   private final IChartContextProvider _chartContextProvider;
   private final TourChart             _tourChart;

   public ActionSelectInBetweenTimeSlices(final IChartContextProvider chartContextProvider,
                                          final TourChart tourChart) {

      //TODO FB
      setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__edit_tour_marker_new));
      setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__edit_tour_marker_new_disabled));

      _chartContextProvider = chartContextProvider;
      _tourChart = tourChart;
   }

   @Override
   public void run() {

      //Opens the TourDataEditorView
      Util.showView(TourDataEditorView.ID, true);

      final TourDataEditorView tourDataEditorView = TourManager.getTourDataEditor();
      final SelectionChartInfo selectionChartInfo = _chartContextProvider.getChart().getChartInfo();
      final TourChartConfiguration tcc= _tourChart.getTourChartConfig();

      for (final int actionId : tcc.getVisibleGraphs()) {

         switch (actionId) {
         case TourManager.GRAPH_SWIM_STROKES:
         case TourManager.GRAPH_SWIM_SWOLF:
            System.out.println("SWIM GRAPH");
            break;
            default:
            System.out.println("NOT SWIM GRAPH");
            break;
         }
      }
      if (selectionChartInfo == null || tourDataEditorView == null) {
         return;
      }

      //TODO fb detect if it swim graph and opens the swim slice ?
      //Opens the TimeSlice tab
      tourDataEditorView.selectTimeSlicesTab();

      tourDataEditorView.setRowEditModeEnabled(true);

      tourDataEditorView.selectTimeSlices(selectionChartInfo);
   }
}
