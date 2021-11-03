/*******************************************************************************
 * Copyright (C) 2019, 2020 Frédéric Bard
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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.SQL;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourLogManager;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;

public class ActionComputeCadenceZonesTimes extends Action {

   private static IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

   private final ITourProvider     _tourProvider;

   public ActionComputeCadenceZonesTimes(final ITourProvider tourDataEditor) {

      super(null, AS_PUSH_BUTTON);

      _tourProvider = tourDataEditor;

      setText(Messages.Tour_Action_ComputeCadenceZonesTimes);
   }

   @Override
   public void run() {

      // do NOT run when tour editor contains a modified tour
      if (TourManager.isTourEditorModified()) {
         return;
      }

      final List<TourData> selectedTours = _tourProvider.getSelectedTours();
      if (selectedTours == null || selectedTours.isEmpty()) {
         // tours are not selected -> this can occur when loading tour data is canceled
         return;
      }

      if (!MessageDialog.openConfirm(
            Display.getCurrent().getActiveShell(),
            Messages.Tour_Action_ComputeCadenceZonesTimes_Title,
            NLS.bind(Messages.Tour_Action_ComputeCadenceZonesTimes_Message,
                  selectedTours.size(),
                  _prefStore.getInt(ITourbookPreferences.CADENCE_ZONES_DELIMITER)))) {
         return;
      }

      final long start = System.currentTimeMillis();

      TourLogManager.showLogView();
      TourLogManager.log_TITLE(NLS.bind(Messages.Log_ComputeCadenceZonesTimes_001_Start, selectedTours.size()));

      boolean isTaskDone = false;
      try (Connection sqlConnection = TourDatabase.getInstance().getConnection()) {

         isTaskDone = TourManager.computeCadenceZonesTimes(sqlConnection, selectedTours);

      } catch (final SQLException e) {
         SQL.showException(e);
      } finally {

         TourLogManager.log_TITLE(String.format(Messages.Log_ComputeCadenceZonesTimes_002_End, (System.currentTimeMillis() - start) / 1000.0));

         if (isTaskDone) {

            TourManager.getInstance().clearTourDataCache();

            Display.getDefault().asyncExec(new Runnable() {
               @Override
               public void run() {

                  TourManager.fireEvent(TourEventId.CLEAR_DISPLAYED_TOUR);
                  // prevent re-importing in the import view
                  RawDataManager.setIsReimportingActive(true);
                  {
                     // fire unique event for all changes
                     TourManager.fireEvent(TourEventId.ALL_TOURS_ARE_MODIFIED);
                  }
                  RawDataManager.setIsReimportingActive(false);
               }
            });
         }
      }
   }
}
