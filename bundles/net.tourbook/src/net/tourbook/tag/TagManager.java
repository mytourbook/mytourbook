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
package net.tourbook.tag;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourTag;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourLogManager;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;

/**
 */
public class TagManager {

   public static final String[] EXPAND_TYPE_NAMES = {
         Messages.app_action_expand_type_flat,
         Messages.app_action_expand_type_year_day,
         Messages.app_action_expand_type_year_month_day };

   public static final int[]    EXPAND_TYPES      = {
         TourTag.EXPAND_TYPE_FLAT,
         TourTag.EXPAND_TYPE_YEAR_DAY,
         TourTag.EXPAND_TYPE_YEAR_MONTH_DAY };

   /**
    * Deletes a tour tag from all contained tours and in the tag structure. This event
    * {@link TourEventId#TAG_STRUCTURE_CHANGED} is fired when done.
    *
    * @param tourTag
    * @return Returns <code>true</code> when deletion was sucessfull
    */
   public static boolean deleteTourTag(final TourTag tourTag) {

      // ensure that a tour is NOT modified
      if (TourManager.isTourEditorModified(false)) {
         return false;
      }

      final ArrayList<Long> allTagIds = getTaggedTours(tourTag);

      // confirm deletion, show tag name and number of tours which contain a tag
      final MessageDialog dialog = new MessageDialog(
            Display.getDefault().getActiveShell(),
            Messages.Tag_Manager_Dialog_DeleteTag_Title,
            null,
            NLS.bind(Messages.Tag_Manager_Dialog_DeleteTag_Message, tourTag.getTagName(), allTagIds.size()),
            MessageDialog.QUESTION,
            new String[] {
                  Messages.Tag_Manager_Action_DeleteTag,
                  IDialogConstants.CANCEL_LABEL },
            1);

      if (dialog.open() == Window.OK) {

         TourLogManager.logInfo(NLS.bind("Removing tag \"{0}\" from {1} tours", tourTag.getTagName(), allTagIds.size()));

         if (deleteTourTag_10(tourTag)) {

            // remove old tags from cached tours
            TourDatabase.clearTourTags();

            TagMenuManager.updateRecentTagNames();

            TourManager.getInstance().clearTourDataCache();

            // fire modify event
            TourManager.fireEvent(TourEventId.TAG_STRUCTURE_CHANGED);

            return true;
         }
      }

      return false;
   }

   private static boolean deleteTourTag_10(final TourTag tourTag) {

      final long tagId = tourTag.getTagId();

      boolean returnResult = false;

      String sql;
      Connection conn = null;
      PreparedStatement prepStmt = null;

      try {

         conn = TourDatabase.getInstance().getConnection();

         /*
          * Remove tag from TOURDATA_TOURTAG
          */
         sql = "DELETE" //                                                 //$NON-NLS-1$
               + " FROM " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG //   //$NON-NLS-1$
               + " WHERE " + TourDatabase.KEY_TAG + "=?";
         prepStmt = conn.prepareStatement(sql);
         {
            prepStmt.setLong(1, tagId);
            prepStmt.execute();
         }
         prepStmt.close();
//         TourLogManager.logSubInfo(String.format("Removed tag from %s", TourDatabase.JOINTABLE__TOURDATA__TOURTAG)); //$NON-NLS-1$
         TourLogManager.logSubInfo(sql);

         /*
          * Remove tag from TOURTAGCATEGORY_TOURTAG
          */
         sql = "DELETE" //                                                       //$NON-NLS-1$
               + " FROM " + TourDatabase.JOINTABLE__TOURTAGCATEGORY_TOURTAG //   //$NON-NLS-1$
               + " WHERE " + TourDatabase.KEY_TAG + "=?";
         prepStmt = conn.prepareStatement(sql);
         {
            prepStmt.setLong(1, tagId);
            prepStmt.execute();
         }
         prepStmt.close();
//         TourLogManager.logSubInfo(String.format("Removed tag from %s", TourDatabase.JOINTABLE__TOURTAGCATEGORY_TOURTAG)); //$NON-NLS-1$
         TourLogManager.logSubInfo(sql);

         /*
          * Remove tag from TOURTAG
          */
         sql = "DELETE" //                                           //$NON-NLS-1$
               + " FROM " + TourDatabase.TABLE_TOUR_TAG //           //$NON-NLS-1$
               + " WHERE " + TourDatabase.ENTITY_ID_TAG + "=?";
         prepStmt = conn.prepareStatement(sql);
         {
            prepStmt.setLong(1, tagId);
            prepStmt.execute();
         }
         prepStmt.close();
//         TourLogManager.logSubInfo(String.format("Removed tag from %s", TourDatabase.TABLE_TOUR_TAG)); //$NON-NLS-1$
         TourLogManager.logSubInfo(sql);

         returnResult = true;

      } catch (final SQLException e) {

         UI.showSQLException(e);

      } finally {
         Util.closeSql(conn);
         Util.closeSql(prepStmt);
      }

      return returnResult;
   }

   /**
    * Get all tours for a tag id.
    */
   /**
    * @param tourTag
    * @return Returns a list with all tour id's which contain the tour tag.
    */
   private static ArrayList<Long> getTaggedTours(final TourTag tourTag) {

      final ArrayList<Long> allTourIds = new ArrayList<>();

      try {

         final String sql = "" //$NON-NLS-1$

               + "SELECT\n" //                                                           //$NON-NLS-1$

               + " TourData.tourId\n" //                                              1  //$NON-NLS-1$

               + " FROM " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " jTdataTtag \n" //$NON-NLS-1$ //$NON-NLS-2$

               // get all tours for current tag
               + " LEFT OUTER JOIN " + TourDatabase.TABLE_TOUR_DATA + " TourData" //      //$NON-NLS-1$ //$NON-NLS-2$
               + " ON jTdataTtag.TourData_tourId = TourData.tourId \n" //                 //$NON-NLS-1$

               + " WHERE jTdataTtag.TourTag_TagId = ?\n" //                               //$NON-NLS-1$

               + " ORDER BY tourId\n"; //                                                 //$NON-NLS-1$

         final Connection conn = TourDatabase.getInstance().getConnection();

         final PreparedStatement statement = conn.prepareStatement(sql);
         statement.setLong(1, tourTag.getTagId());

         final ResultSet result = statement.executeQuery();
         while (result.next()) {
            allTourIds.add(result.getLong(1));
         }

         conn.close();

      } catch (final SQLException e) {
         net.tourbook.ui.UI.showSQLException(e);
      }

      return allTourIds;
   }
}
