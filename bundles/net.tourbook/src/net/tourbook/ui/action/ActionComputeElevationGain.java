/*******************************************************************************
 * Copyright (C) 2014, 2025 Wolfgang Schramm and Contributors
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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.data.TourData;
import net.tourbook.database.IComputeNoDataserieValues;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProviderByID;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;

public class ActionComputeElevationGain extends Action {

   private final ITourProviderByID _tourProvider;

   private NumberFormat            _nf0 = NumberFormat.getNumberInstance();
   private NumberFormat            _nf1 = NumberFormat.getNumberInstance();
   {
      _nf0.setMinimumFractionDigits(0);
      _nf0.setMaximumFractionDigits(0);
      _nf1.setMinimumFractionDigits(1);
      _nf1.setMaximumFractionDigits(1);
   }

   public ActionComputeElevationGain(final ITourProviderByID tourProvider) {

      super(null, AS_PUSH_BUTTON);

      _tourProvider = tourProvider;

      setText(Messages.Action_Compute_ElevationGain);
   }

   @Override
   public void run() {

      final ArrayList<Long> tourIds = new ArrayList<>(_tourProvider.getSelectedTourIDs());

      final float prefDPTolerance = TourbookPlugin.getPrefStore().getFloat(ITourbookPreferences.COMPUTED_ALTITUDE_DP_TOLERANCE);

      final String dpToleranceWithUnit = _nf1.format(prefDPTolerance / UI.UNIT_VALUE_ELEVATION) + UI.SPACE1 + UI.UNIT_LABEL_ELEVATION;

      final String message = NLS.bind(
            Messages.Compute_TourValue_ElevationGain_Message,
            tourIds.size(),
            dpToleranceWithUnit);

      if (MessageDialog.openConfirm(
            Display.getCurrent().getActiveShell(),
            Messages.Compute_TourValue_ElevationGain_Title,
            message) == false) {

         return;
      }

      final int[] elevationUp_Old = new int[] { 0 };
      final int[] elevationUp_New = new int[] { 0 };
      final int[] elevationDown_Old = new int[] { 0 };
      final int[] elevationDown_New = new int[] { 0 };

      final IComputeNoDataserieValues configComputeTourValue = new IComputeNoDataserieValues() {

         @Override
         public boolean computeTourValues(final TourData tourData,
                                          final PreparedStatement sqlUpdateStatement) throws SQLException {

            // keep old value
            elevationUp_Old[0] += tourData.getTourAltUp();
            elevationDown_Old[0] += tourData.getTourAltDown();

            // compute new values
            if (tourData.computeAltitudeUpDown() == false) {

               // altitude up/down values could not be computed
               return false;
            }

            final int newAltitudeUp = tourData.getTourAltUp();
            final int newAltitudeDown = tourData.getTourAltDown();

            elevationUp_New[0] += newAltitudeUp;
            elevationDown_New[0] += newAltitudeDown;

            sqlUpdateStatement.setShort(1, tourData.getDpTolerance());
            sqlUpdateStatement.setInt(2, newAltitudeUp);
            sqlUpdateStatement.setInt(3, newAltitudeDown);
            sqlUpdateStatement.setInt(4, tourData.getAvgAltitudeChange());
            sqlUpdateStatement.setLong(5, tourData.getTourId());

            return true;
         }

         @Override
         public String getResultText() {

            final int elevationDiff_Up = Math.abs(elevationUp_New[0] - elevationUp_Old[0]);
            final int elevationDiff_Down = Math.abs(elevationDown_New[0] - elevationDown_Old[0]);

            final String diffText = "+%5.1f %s    -%5.1f %s".formatted( //$NON-NLS-1$

                  elevationDiff_Up / UI.UNIT_VALUE_ELEVATION,
                  UI.UNIT_LABEL_ELEVATION,

                  elevationDiff_Down / UI.UNIT_VALUE_ELEVATION,
                  UI.UNIT_LABEL_ELEVATION);

            return NLS.bind(Messages.Compute_TourValue_ElevationGain_ResultText,
                  new Object[] {
                        dpToleranceWithUnit,
                        diffText,
                        UI.EMPTY_STRING
                  });
         }

         @Override
         public String getSQLUpdateStatement() {

            final String sql = UI.EMPTY_STRING

                  + "UPDATE " + TourDatabase.TABLE_TOUR_DATA //   //$NON-NLS-1$

                  + " SET" //                                     //$NON-NLS-1$

                  + " dpTolerance         = ?, " //   1           //$NON-NLS-1$
                  + " tourAltUp           = ?, " //   2           //$NON-NLS-1$
                  + " tourAltDown         = ?, " //   3           //$NON-NLS-1$
                  + " avgAltitudeChange   = ? " //    4           //$NON-NLS-1$

                  + " WHERE tourId        = ?"; //    5           //$NON-NLS-1$

            return sql;
         }
      };

      TourDatabase.computeNoDataserieValues_ForAllTours(configComputeTourValue, tourIds);

      /*
       * Fire event
       */
      TourManager.getInstance().removeAllToursFromCache();
      TourManager.fireEvent(TourEventId.CLEAR_DISPLAYED_TOUR);

      // fire unique event for all changes
      TourManager.fireEvent(TourEventId.ALL_TOURS_ARE_MODIFIED);
   }
}
