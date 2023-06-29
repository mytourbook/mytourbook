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
package net.tourbook.ui.tourChart.action;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.PerspectiveFactoryReferenceTimeline;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourReference;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.views.referenceTour.ReferenceTourView;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;

/**
 * add a new reference tour to all reference tours
 */
public class ActionCreateRefTour extends Action {

   private TourChart _tourChart;

   public ActionCreateRefTour(final TourChart tourChart) {

      setText(Messages.RefTour_Action_CreateReferenceTour);
      setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.RefTour_New));

      _tourChart = tourChart;
   }

   @Override
   public void run() {

      if (TourManager.isTourEditorModified()) {
         return;
      }

      final SelectionChartInfo chartInfo = _tourChart.getChartInfo();
      final TourData tourData = _tourChart.getTourData();

      // get the reference tour name
      final InputDialog dialog = new InputDialog(
            Display.getCurrent().getActiveShell(),
            Messages.RefTour_Dialog_AddReferenceTour_Title,
            Messages.RefTour_Dialog_AddReferenceTour_Message,
            UI.EMPTY_STRING,
            null);

      if (dialog.open() != Window.OK) {
         return;
      }

      // create new tour reference
      final TourReference newTourReference = new TourReference(
            dialog.getValue(),
            tourData,
            chartInfo.leftSliderValuesIndex,
            chartInfo.rightSliderValuesIndex);

      // add the tour reference into the tour data
      tourData.getTourReferences().add(newTourReference);

      // save tour
      final TourData savedTourData = TourManager.saveModifiedTour(tourData, false);

      // show reference tour perspective and select the ref tour
      Display.getDefault().asyncExec(() -> {

         try {

            final IWorkbench workbench = PlatformUI.getWorkbench();
            final IWorkbenchWindow wbWindow = workbench.getActiveWorkbenchWindow();

            // show compare result perspective
            workbench.showPerspective(PerspectiveFactoryReferenceTimeline.PERSPECTIVE_ID, wbWindow);

            // show ref tour view
            Util.showView(ReferenceTourView.ID, true);

            // update reference tour view
            TourManager.fireEvent(TourEventId.REFERENCE_TOUR_IS_CREATED, new TourEvent(savedTourData));

         } catch (final WorkbenchException e) {
            StatusUtil.log(e);
         }
      });
   }
}
