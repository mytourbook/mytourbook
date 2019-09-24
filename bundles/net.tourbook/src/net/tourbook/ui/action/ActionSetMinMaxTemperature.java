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
package net.tourbook.ui.action;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.SQL;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourLogManager;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;

public class ActionSetMinMaxTemperature extends Action {

   private final ITourProvider _tourProvider;

   public ActionSetMinMaxTemperature(final ITourProvider tourProvider) {

      super(Messages.Tour_Action_SetMinMaxTemperature, AS_PUSH_BUTTON);

      _tourProvider = tourProvider;
   }

   @Override
   public void run() {

      // check if the tour editor contains a modified tour
      if (TourManager.isTourEditorModified()) {
         return;
      }

      final ArrayList<TourData> selectedTours = _tourProvider.getSelectedTours();

      if (selectedTours == null || selectedTours.size() < 1) {

         // a tour is not selected -> this should not happen, action should be disabled

         return;
      }

      final MessageDialog messageDialog = new MessageDialog(
            Display.getDefault().getActiveShell(),
            Messages.Tour_Action_SetMinMaxTemperature_Title,
            null,
            NLS.bind(Messages.Tour_Action_SetMinMaxTemperature_Message, selectedTours.size()),
            MessageDialog.QUESTION,
            new String[] {
                  Messages.Tour_Action_SetMinMaxTemperature_Apply,
                  IDialogConstants.CANCEL_LABEL },
            1);

      if (messageDialog.open() != 0) {

         // canceled

         return;
      }

      // compute min/max temperature

      TourLogManager.showLogView();

      try {

         final IRunnableWithProgress runnable = new IRunnableWithProgress() {
            @Override
            public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

               boolean isTaskDone = false;

               Connection conn = null;
               try {

                  conn = TourDatabase.getInstance().getConnection();

                  isTaskDone = run_SetMinMaxTemperature(conn, monitor, selectedTours);

               } catch (final SQLException e) {

                  SQL.showException(e);

               } finally {

                  Util.closeSql(conn);

                  if (isTaskDone) {

                     TourManager.getInstance().clearTourDataCache();

                     Display.getDefault().asyncExec(new Runnable() {
                        @Override
                        public void run() {

                           TourManager.fireEvent(TourEventId.CLEAR_DISPLAYED_TOUR);
                           TourManager.fireEvent(TourEventId.ALL_TOURS_ARE_MODIFIED);
                        }
                     });

                  }
               }
            }
         };

         new ProgressMonitorDialog(Display.getCurrent().getActiveShell()).run(true, false, runnable);

      } catch (final Exception e) {
         StatusUtil.log(e);
      }
   }

   /**
    * Update calendar week for all tours with the app week settings from
    * {@link TimeTools#calendarWeek}
    *
    * @param conn
    * @param monitor
    * @param selectedTours
    * @return Returns <code>true</code> when the week is computed
    * @throws SQLException
    */
   private boolean run_SetMinMaxTemperature(final Connection conn,
                                            final IProgressMonitor monitor,
                                            final ArrayList<TourData> selectedTours) throws SQLException {

      boolean isUpdated = false;

      final PreparedStatement stmtUpdate = conn.prepareStatement(

            "UPDATE " + TourDatabase.TABLE_TOUR_DATA //  //$NON-NLS-1$
                  + " SET" //                            //$NON-NLS-1$
                  + " weather_Temperature_Min=?, " //    //$NON-NLS-1$
                  + " weather_Temperature_Max=? " //     //$NON-NLS-1$
                  + " WHERE tourId=?"); //               //$NON-NLS-1$

      int tourIdx = 1;

      // loop over all tours and calculate and set new columns
      for (final TourData tourData : selectedTours) {

         if (monitor != null) {
            final String msg = NLS.bind(
                  Messages.Tour_Database_Update_MinMaxTemperature,
                  new Object[] { tourIdx++, selectedTours.size() });

            monitor.subTask(msg);
         }

         if (tourData.temperatureSerie == null || tourData.temperatureSerie.length == 0) {

         } else {

            // compute min/max temperature
            tourData.computeAvg_Temperature();

            // update min/max temperature in the database
            stmtUpdate.setFloat(1, tourData.getWeather_Temperature_Min());
            stmtUpdate.setFloat(2, tourData.getWeather_Temperature_Max());
            stmtUpdate.setLong(3, tourData.getTourId());

            stmtUpdate.executeUpdate();

            isUpdated = true;
         }
      }

      return isUpdated;
   }
}
