/*******************************************************************************
 * Copyright (C) 2005, 2020 Wolfgang Schramm and Contributors
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
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourTag;
import net.tourbook.database.TourDatabase;
import net.tourbook.tag.tour.filter.TourTagFilterManager;
import net.tourbook.tag.tour.filter.TourTagFilterProfile;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourLogManager;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;

/**
 */
public class TagManager {

   private static final String  NL                  = UI.NEW_LINE;

   public static final String[] EXPAND_TYPE_NAMES   = {
         Messages.app_action_expand_type_flat,
         Messages.app_action_expand_type_year_day,
         Messages.app_action_expand_type_year_month_day };

   public static final int[]    EXPAND_TYPES        = {
         TourTag.EXPAND_TYPE_FLAT,
         TourTag.EXPAND_TYPE_YEAR_DAY,
         TourTag.EXPAND_TYPE_YEAR_MONTH_DAY };

   private static final String  PARAMETER_FIRST     = "?";        //$NON-NLS-1$
   private static final String  PARAMETER_FOLLOWING = ", ?";      //$NON-NLS-1$

   private static boolean canDeleteTourTagCategory(final long categoryId, final String categoryName) {

      // Category -> Tag
      final String sql_Category_Tags = UI.EMPTY_STRING

            + "SELECT" + NL //                                                               //$NON-NLS-1$
            + " COUNT(TOURTAGCATEGORY_TAGCATEGORYID)" + NL //                                //$NON-NLS-1$
            + " FROM " + TourDatabase.JOINTABLE__TOURTAGCATEGORY_TOURTAG + NL //             //$NON-NLS-1$
            + " WHERE TOURTAGCATEGORY_TAGCATEGORYID = " + categoryId + NL //                 //$NON-NLS-1$
      ;

      // Category -> Category
      final String sql_Category_Categories = UI.EMPTY_STRING

            + "SELECT" + NL //                                                               //$NON-NLS-1$
            + " COUNT(TOURTAGCATEGORY_TAGCATEGORYID1)" + NL //                               //$NON-NLS-1$
            + " FROM " + TourDatabase.JOINTABLE__TOURTAGCATEGORY_TOURTAGCATEGORY + NL //     //$NON-NLS-1$
            + " WHERE TOURTAGCATEGORY_TAGCATEGORYID1 = " + categoryId + NL //                //$NON-NLS-1$
      ;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final long numCategory_Tags = getNumberOfItems(conn, sql_Category_Tags);
         if (numCategory_Tags > 0) {

            MessageDialog.openError(Display.getDefault().getActiveShell(),
                  Messages.Tag_Manager_Dialog_DeleteCategory_Title,
                  NLS.bind(Messages.Tag_Manager_Dialog_DeleteCategory_Tags_Message,
                        new Object[] { categoryName, numCategory_Tags }));

            return false;
         }

         final long numCategory_Categories = getNumberOfItems(conn, sql_Category_Categories);
         if (numCategory_Categories > 0) {

            MessageDialog.openError(Display.getDefault().getActiveShell(),
                  Messages.Tag_Manager_Dialog_DeleteCategory_Title,
                  NLS.bind(Messages.Tag_Manager_Dialog_DeleteCategory_Categories_Message,
                        new Object[] { categoryName, numCategory_Categories }));

            return false;
         }

         return true;

      } catch (final SQLException e) {
         UI.showSQLException(e);
      }

      return false;
   }

   /**
    * Deletes a tour tag from all contained tours and in the tag structure. This event
    * {@link TourEventId#TAG_STRUCTURE_CHANGED} is fired when done.
    *
    * @param allTags
    * @return Returns <code>true</code> when deletion was successful
    */
   public static boolean deleteTourTag(final ArrayList<TourTag> allTags) {

      // ensure that a tour is NOT modified in the tour editor
      if (TourManager.isTourEditorModified(false)) {
         return false;
      }

      String dialogMessage;
      String actionDeleteTags;

      final ArrayList<Long> allTourIds = getTaggedTours(allTags);

      if (allTags.size() == 1) {

         // remove one tag

         dialogMessage = NLS.bind(Messages.Tag_Manager_Dialog_DeleteTag_Message, allTags.get(0).getTagName(), allTourIds.size());
         actionDeleteTags = Messages.Tag_Manager_Action_DeleteTag;

      } else {

         // remove multiple tags

         dialogMessage = NLS.bind(Messages.Tag_Manager_Dialog_DeleteTag_Multiple_Message, allTags.size(), allTourIds.size());
         actionDeleteTags = Messages.Tag_Manager_Action_DeleteTags;
      }

      final Display display = Display.getDefault();

      // confirm deletion, show tag name and number of tours which contain a tag
      final MessageDialog dialog = new MessageDialog(
            display.getActiveShell(),
            Messages.Tag_Manager_Dialog_DeleteTag_Title,
            null,
            dialogMessage,
            MessageDialog.QUESTION,
            new String[] {
                  actionDeleteTags,
                  IDialogConstants.CANCEL_LABEL },
            1);

      final boolean returnValue[] = { false };

      if (dialog.open() == Window.OK) {

         BusyIndicator.showWhile(display, () -> {

            if (deleteTourTag_10(allTags)) {

               fireChangeEvent();

               updateTourTagFilterProfiles(allTags);

               returnValue[0] = true;
            }
         });
      }

      return returnValue[0];
   }

   private static boolean deleteTourTag_10(final ArrayList<TourTag> allTags) {

      boolean returnResult = false;

      String sql;

      PreparedStatement prepStmt_TagCategory = null;
      PreparedStatement prepStmt_TourData = null;
      PreparedStatement prepStmt_TourTag = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         // remove tag from TOURDATA_TOURTAG
         sql = "DELETE" //                                                        //$NON-NLS-1$
               + " FROM " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG //          //$NON-NLS-1$
               + " WHERE " + TourDatabase.KEY_TAG + "=?"; //                      //$NON-NLS-1$ //$NON-NLS-2$
         prepStmt_TourData = conn.prepareStatement(sql);

         // remove tag from TOURTAGCATEGORY_TOURTAG
         sql = "DELETE" //                                                       //$NON-NLS-1$
               + " FROM " + TourDatabase.JOINTABLE__TOURTAGCATEGORY_TOURTAG //   //$NON-NLS-1$
               + " WHERE " + TourDatabase.KEY_TAG + "=?"; //                     //$NON-NLS-1$ //$NON-NLS-2$
         prepStmt_TagCategory = conn.prepareStatement(sql);

         // remove tag from TOURTAG
         sql = "DELETE" //                                                       //$NON-NLS-1$
               + " FROM " + TourDatabase.TABLE_TOUR_TAG //                       //$NON-NLS-1$
               + " WHERE " + TourDatabase.ENTITY_ID_TAG + "=?"; //               //$NON-NLS-1$ //$NON-NLS-2$
         prepStmt_TourTag = conn.prepareStatement(sql);

         int[] returnValue_TourData;
         int[] returnValue_TagCategory;
         int[] returnValue_TourTag;

         conn.setAutoCommit(false);
         {
            for (final TourTag tourTag : allTags) {

               final long tagId = tourTag.getTagId();

               prepStmt_TourData.setLong(1, tagId);
               prepStmt_TourData.addBatch();

               prepStmt_TagCategory.setLong(1, tagId);
               prepStmt_TagCategory.addBatch();

               prepStmt_TourTag.setLong(1, tagId);
               prepStmt_TourTag.addBatch();
            }

            returnValue_TourData = prepStmt_TourData.executeBatch();
            returnValue_TagCategory = prepStmt_TagCategory.executeBatch();
            returnValue_TourTag = prepStmt_TourTag.executeBatch();
         }
         conn.commit();

         // log result
         TourLogManager.showLogView();

         for (int tagIndex = 0; tagIndex < allTags.size(); tagIndex++) {

            TourLogManager.log_INFO(String.format(Messages.Tag_Manager_LogInfo_DeletedTags,
                  returnValue_TourData[tagIndex],
                  returnValue_TagCategory[tagIndex],
                  returnValue_TourTag[tagIndex],
                  allTags.get(tagIndex).getTagName()));
         }

         returnResult = true;

      } catch (final SQLException e) {

         UI.showSQLException(e);

      } finally {

         Util.closeSql(prepStmt_TourData);
         Util.closeSql(prepStmt_TagCategory);
         Util.closeSql(prepStmt_TourTag);
      }

      return returnResult;
   }

   /**
    * @param categoryId
    * @param categoryName
    * @return Returns <code>true</code> when tag category is deleted.
    */
   public static boolean deleteTourTagCategory(final long categoryId, final String categoryName) {

      // ensure that a tour is NOT modified in the tour editor
      if (TourManager.isTourEditorModified(false)) {
         return false;
      }

      if (canDeleteTourTagCategory(categoryId, categoryName) == false) {
         return false;
      }

      final Display display = Display.getDefault();

      // confirm deletion, show tag name and number of tours which contain a tag
      final MessageDialog dialog = new MessageDialog(
            display.getActiveShell(),
            Messages.Tag_Manager_Dialog_DeleteCategory_Title,
            null,
            NLS.bind(Messages.Tag_Manager_Dialog_DeleteCategory_Message, categoryName),
            MessageDialog.QUESTION,
            new String[] {
                  Messages.Tag_Manager_Action_DeleteCategory,
                  IDialogConstants.CANCEL_LABEL },
            1);

      final boolean returnValue[] = { false };

      if (dialog.open() == Window.OK) {

         BusyIndicator.showWhile(display, () -> {

            if (deleteTourTagCategory_10(categoryId, categoryName)) {

               fireChangeEvent();

               returnValue[0] = true;
            }
         });
      }

      return returnValue[0];
   }

   private static boolean deleteTourTagCategory_10(final long categoryId, final String categoryName) {

      boolean returnResult = false;

      String sql;

      PreparedStatement prepStmt_CategoryCategory = null;
      PreparedStatement prepStmt_TagCategory = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         // remove category from: TOURTAGCATEGORY_TOURTAGCATEGORY
         sql = "DELETE" + NL //                                                                 //$NON-NLS-1$
               + " FROM " + TourDatabase.JOINTABLE__TOURTAGCATEGORY_TOURTAGCATEGORY + NL //     //$NON-NLS-1$
               + " WHERE TOURTAGCATEGORY_TAGCATEGORYID2 =?"; //                                 //$NON-NLS-1$
         prepStmt_CategoryCategory = conn.prepareStatement(sql);

         // remove category from TOURTAGCATEGORY
         sql = "DELETE" //                                                                      //$NON-NLS-1$
               + " FROM " + TourDatabase.TABLE_TOUR_TAG_CATEGORY //                       //$NON-NLS-1$
               + " WHERE " + TourDatabase.ENTITY_ID_TAG_CATEGORY + "=?"; //               //$NON-NLS-1$ //$NON-NLS-2$
         prepStmt_TagCategory = conn.prepareStatement(sql);

         int[] returnValue_CategoryCategory;
         int[] returnValue_TagCategory;

         conn.setAutoCommit(false);
         {
            prepStmt_CategoryCategory.setLong(1, categoryId);
            prepStmt_CategoryCategory.addBatch();

            prepStmt_TagCategory.setLong(1, categoryId);
            prepStmt_TagCategory.addBatch();

            returnValue_CategoryCategory = prepStmt_CategoryCategory.executeBatch();
            returnValue_TagCategory = prepStmt_TagCategory.executeBatch();
         }
         conn.commit();

         // log result
         TourLogManager.showLogView();
         TourLogManager.log_INFO(String.format(Messages.Tag_Manager_LogInfo_DeletedTagCategory,
               returnValue_CategoryCategory[0],
               returnValue_TagCategory[0],
               categoryName));

         returnResult = true;

      } catch (final SQLException e) {

         UI.showSQLException(e);

      } finally {

         Util.closeSql(prepStmt_CategoryCategory);
         Util.closeSql(prepStmt_TagCategory);
      }

      return returnResult;
   }

   private static void fireChangeEvent() {

      // remove old tags from cached tours
      TourDatabase.clearTourTags();

      TagMenuManager.clearRecentTags();

      TourManager.getInstance().clearTourDataCache();

      // fire modify event
      TourManager.fireEvent(TourEventId.TAG_STRUCTURE_CHANGED);
   }

   private static long getNumberOfItems(final Connection conn, final String sql) {

      long numItems = 0;

      try (PreparedStatement stmt = conn.prepareStatement(sql)) {

         final ResultSet result = stmt.executeQuery();
         while (result.next()) {
            numItems = result.getLong(1);
            break;
         }

      } catch (final SQLException e) {
         StatusUtil.log(sql);
         UI.showSQLException(e);
      }

      return numItems;
   }

   /**
    * Get all tours for a tag id.
    */
   /**
    * @param allTags
    * @return Returns a list with all tour id's which contain the tour tag.
    */
   private static ArrayList<Long> getTaggedTours(final ArrayList<TourTag> allTags) {

      final ArrayList<Long> allTourIds = new ArrayList<>();

      final ArrayList<Long> sqlParameters = new ArrayList<>();
      final StringBuilder sqlParameterPlaceholder = new StringBuilder();

      boolean isFirst = true;

      for (final TourTag tagTag : allTags) {

         if (isFirst) {
            isFirst = false;
            sqlParameterPlaceholder.append(PARAMETER_FIRST);
         } else {
            sqlParameterPlaceholder.append(PARAMETER_FOLLOWING);
         }

         sqlParameters.add(tagTag.getTagId());
      }

      final String sql = UI.EMPTY_STRING

            + "SELECT\n" //                                                                           //$NON-NLS-1$

            + " DISTINCT TourData.tourId\n" //                                                        //$NON-NLS-1$

            + " FROM " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " jTdataTtag \n" //              //$NON-NLS-1$ //$NON-NLS-2$

            // get all tours for current tag
            + " LEFT OUTER JOIN " + TourDatabase.TABLE_TOUR_DATA + " TourData" //                     //$NON-NLS-1$ //$NON-NLS-2$
            + " ON jTdataTtag.TourData_tourId = TourData.tourId \n" //                                //$NON-NLS-1$

            + " WHERE jTdataTtag.TourTag_TagId IN (" + sqlParameterPlaceholder.toString() + ")\n" //  //$NON-NLS-1$ //$NON-NLS-2$

            + " ORDER BY tourId\n"; //                                                 //$NON-NLS-1$

      PreparedStatement statement = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         statement = conn.prepareStatement(sql);

         // fillup parameter
         for (int parameterIndex = 0; parameterIndex < sqlParameters.size(); parameterIndex++) {
            statement.setLong(parameterIndex + 1, sqlParameters.get(parameterIndex));
         }

         final ResultSet result = statement.executeQuery();
         while (result.next()) {
            allTourIds.add(result.getLong(1));
         }

      } catch (final SQLException e) {
         StatusUtil.log(sql);
         UI.showSQLException(e);
      } finally {
         Util.closeSql(statement);
      }

      return allTourIds;
   }

   /**
    * Updates the tag list in each tour tag filter profile as one or several tags were just deleted.
    *
    * @param deletedTags
    *           An array containing the Tag Id's of the tour tags just deleted
    */
   private static void updateTourTagFilterProfiles(final ArrayList<TourTag> deletedTags) {

      final ArrayList<TourTagFilterProfile> profiles = TourTagFilterManager.getProfiles();
      for (final TourTagFilterProfile profile : profiles) {
         for (final TourTag tourTag : deletedTags) {
            if (profile.tagFilterIds.contains(tourTag.getTagId())) {
               profile.tagFilterIds.remove(tourTag.getTagId());
            }
         }
      }
   }
}
