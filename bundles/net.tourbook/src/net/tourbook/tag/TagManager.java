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
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import net.tourbook.Messages;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TourManager;

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

   private static void deleteTag(final TourTag tourTag) {

      if (deleteTag_10_FromTourData(tourTag)) {
         deleteTag_20_FromDb(tourTag);
      }
   }

   private static boolean deleteTag_10_FromTourData(final TourTag tourTag) {

      boolean returnResult = false;

      final EntityManager em = TourDatabase.getInstance().getEntityManager();

      if (em != null) {

         final Query query = em.createQuery("" //$NON-NLS-1$

               + "SELECT         \n" //$NON-NLS-1$
               + " TourData      \n" //                                          //$NON-NLS-1$
               + " FROM TourData \n" //                                          //$NON-NLS-1$
               + " WHERE tourData.tourType.typeId=" + tourTag.getTagId()); //    //$NON-NLS-1$

         final List<?> allTourData = query.getResultList();
         if (allTourData.size() > 0) {

            final EntityTransaction ts = em.getTransaction();

            try {

               ts.begin();

               // remove tour tag from all tour data
               for (final Object listItem : allTourData) {

                  if (listItem instanceof TourData) {

                     final TourData tourData = (TourData) listItem;

                     final Set<TourTag> allTourTags = tourData.getTourTags();
                     allTourTags.remove(tourTag);
                     tourData.setTourTags(allTourTags);

                     em.merge(tourData);
                  }
               }

               ts.commit();

            } catch (final Exception e) {
               StatusUtil.showStatus(e);
            } finally {
               if (ts.isActive()) {
                  ts.rollback();
               }
            }
         }

         returnResult = true;
         em.close();
      }

      return returnResult;
   }

   private static boolean deleteTag_20_FromDb(final TourTag tourTag) {

      boolean returnResult = false;

      final EntityManager em = TourDatabase.getInstance().getEntityManager();
      final EntityTransaction ts = em.getTransaction();

      try {
         final TourType tourTypeEntity = em.find(TourType.class, tourTag.getTagId());

         if (tourTypeEntity != null) {

            ts.begin();

            em.remove(tourTypeEntity);

            ts.commit();
         }

      } catch (final Exception e) {
         StatusUtil.showStatus(e);
      } finally {
         if (ts.isActive()) {
            ts.rollback();
         } else {
            returnResult = true;
         }
         em.close();
      }

      return returnResult;
   }

   public static boolean deleteTourTag(final TourTag tourTag) {

      if (TourManager.isTourEditorModified(false)) {
         return false;
      }

      final ArrayList<Long> allTagIds = getTaggedTours(tourTag);

      // confirm deletion, show tag name and number of tours which contain a tag
      final MessageDialog dialog = new MessageDialog(
            Display.getDefault().getActiveShell(),
            Messages.Pref_TourTag_Dialog_DeleteTag_Title,
            null,
            NLS.bind(Messages.Pref_TourTag_Dialog_DeleteTag_Message, tourTag.getTagName(), allTagIds.size()),
            MessageDialog.QUESTION,
            new String[] {
                  Messages.Pref_TourTag_Action_DeleteTag,
                  IDialogConstants.CANCEL_LABEL },
            1);

      if (dialog.open() == Window.OK) {

         deleteTag(tourTag);

      }

      return false;
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
