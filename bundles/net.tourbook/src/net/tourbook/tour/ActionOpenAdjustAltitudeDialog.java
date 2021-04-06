/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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
package net.tourbook.tour;

import java.util.ArrayList;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourData;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.views.tourDataEditor.TourDataEditorView;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;

public class ActionOpenAdjustAltitudeDialog extends Action {

   private ITourProvider _tourProvider;
   private boolean       _isFromEditor;

   public ActionOpenAdjustAltitudeDialog(final ITourProvider tourProvider) {

      _tourProvider = tourProvider;

      setText(Messages.app_action_edit_adjust_altitude);
      setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.AdjustElevation));
      setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Images.AdjustElevation_Disabled));

      setEnabled(false);
   }

   public ActionOpenAdjustAltitudeDialog(final ITourProvider tourProvider, final boolean isFromEditor) {
      this(tourProvider);
      _isFromEditor = isFromEditor;
   }

   public static void doAction(final ITourProvider tourProvider, final boolean isFromEditor) {

      final ArrayList<TourData> selectedTours = tourProvider.getSelectedTours();

      // check if one tour is selected
      if (selectedTours == null || selectedTours.size() != 1 || selectedTours.get(0) == null) {
         return;
      }

      final TourData tourData = selectedTours.get(0);

      if (tourData.isManualTour()) {
         // a manually created tour do not have time slices -> no altitude
         return;
      }

      boolean isCreateDummyAltitude = false;
      final float[] altitudeSerie = tourData.altitudeSerie;

      // check if altitude values are available
      if (altitudeSerie == null || altitudeSerie.length == 0) {
         if (MessageDialog.openQuestion(
               Display.getCurrent().getActiveShell(),
               Messages.Adjust_Altitude_CreateDummyAltitudeData_Title,
               Messages.Adjust_Altitude_CreateDummyAltitudeData_Message) == false) {
            return;
         }

         isCreateDummyAltitude = true;
      }

      /*
       * don't save when the tour is opened in the editor, just update the tour, saving must be
       * done ALWAYS in the editor
       */
      boolean isSave = true;
      final TourDataEditorView tourDataEditor = TourManager.getTourDataEditor();
      if (isFromEditor
            || (tourDataEditor != null && tourDataEditor.isDirty() && tourDataEditor.getTourData() == tourData)) {
         isSave = false;
      }

      if (new DialogAdjustAltitude(Display.getCurrent().getActiveShell(), tourData, isSave, isCreateDummyAltitude)
            .open() == Window.OK) {

         if (isSave) {
            TourManager.saveModifiedTours(selectedTours);
         } else {

            /*
             * don't save the tour, just update the tour data editor
             */
            if (tourDataEditor != null) {

               tourDataEditor.updateUI(tourData, true);

               TourManager.fireEvent(TourEventId.TOUR_CHANGED, new TourEvent(tourData));
            }
         }
      }
   }

   @Override
   public void run() {
      doAction(_tourProvider, _isFromEditor);
   }
}
