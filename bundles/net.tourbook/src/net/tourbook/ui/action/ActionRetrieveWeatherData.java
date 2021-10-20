/*******************************************************************************
 * Copyright (C) 2019, 2021 Frédéric Bard
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
package net.tourbook.ui.action;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider2;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class ActionRetrieveWeatherData extends Action {
   private final ITourProvider2 _tourProvider;

   public ActionRetrieveWeatherData(final ITourProvider2 tourProvider) {

      super(null, AS_PUSH_BUTTON);

      _tourProvider = tourProvider;

      setText(Messages.Tour_Action_RetrieveWeatherData);
   }

   @Override
   public void run() {

      // check if the tour editor contains a modified tour
      if (TourManager.isTourEditorModified()) {
         return;
      }

      final Shell shell = Display.getCurrent().getActiveShell();
      final ArrayList<TourData> selectedTours = _tourProvider.getSelectedTours();
      if (selectedTours == null || selectedTours.isEmpty()) {

         // a tour is not selected
         MessageDialog.openInformation(
               shell,
               Messages.Dialog_RetrieveWeather_Dialog_Title,
               Messages.UI_Label_TourIsNotSelected);

         return;
      }

      BusyIndicator.showWhile(Display.getCurrent(), () -> {

         final ArrayList<TourData> modifiedTours = new ArrayList<>();

         for (final TourData tour : selectedTours) {

            final boolean isDataRetrieved = TourManager.retrieveWeatherData(tour);

            if (isDataRetrieved) {
               modifiedTours.add(tour);
            }
         }

         if (modifiedTours.size() > 0) {
            TourManager.saveModifiedTours(modifiedTours);
         } else {
            MessageDialog.openInformation(
                  shell,
                  Messages.Dialog_RetrieveWeather_Dialog_Title,
                  Messages.Dialog_RetrieveWeather_Label_WeatherDataNotRetrieved);
         }
      });
   }
}
