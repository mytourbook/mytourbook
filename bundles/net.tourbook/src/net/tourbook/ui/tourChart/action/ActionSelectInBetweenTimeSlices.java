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

import net.tourbook.chart.IChartContextProvider;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.common.util.Util;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.views.tourDataEditor.TourDataEditorView;

import org.eclipse.jface.action.Action;

public class ActionSelectInBetweenTimeSlices extends Action {

   private final IChartContextProvider _chartContextProvider;

   public ActionSelectInBetweenTimeSlices(final IChartContextProvider chartContextProvider, final String text) {

      super(text);

      _chartContextProvider = chartContextProvider;
   }

   @Override
   public void run() {

      // make sure the tour editor does not contain a modified tour
      if (TourManager.isTourEditorModified()) {
         return;
      }

      //Opens the TourDataEditorView
      Util.showView(TourDataEditorView.ID, true);

      final TourDataEditorView tourDataEditorView = TourManager.getTourDataEditor();
      final SelectionChartInfo selectionChartInfo = _chartContextProvider.getChart().getChartInfo();

      if (selectionChartInfo == null || tourDataEditorView == null) {
         return;
      }

      //Opens the TimeSlice tab
      tourDataEditorView.selectTimeSlicesTab();

      tourDataEditorView.setRowEditModeEnabled(true);

      tourDataEditorView.selectTimeSlice_InViewer(selectionChartInfo.leftSliderValuesIndex, selectionChartInfo.rightSliderValuesIndex);

      //We recreate the viewer to take into account the new selection
      tourDataEditorView.recreateViewer();
   }
}
