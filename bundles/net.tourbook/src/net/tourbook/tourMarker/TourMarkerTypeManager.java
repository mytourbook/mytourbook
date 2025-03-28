/*******************************************************************************
 * Copyright (C) 2025 Wolfgang Schramm and Contributors
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
package net.tourbook.tourMarker;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourMarkerType;
import net.tourbook.database.TourDatabase;

/**
 * Manage tour marker types
 */
public class TourMarkerTypeManager {

   private static final char NL = UI.NEW_LINE;

   /**
    * @param requestedMarkerTypeID
    *
    * @return Returns the number of {@link TourMarker}s which are containing the
    *         {@link TourMarkerType}
    */
   public static int countTourMarkers(final long requestedMarkerTypeID) {

      final String sql = UI.EMPTY_STRING

            + " SELECT COUNT(*)" + NL //                                   //$NON-NLS-1$
            + " FROM TourMarker" + NL //                                   //$NON-NLS-1$
            + " WHERE " + TourDatabase.KEY_MARKER_TYPE + " = ?" + NL //    //$NON-NLS-1$ //$NON-NLS-2$
      ;

      int numMarkers = 0;

      try (Connection conn = TourDatabase.getInstance().getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

         // fillup sql parameters
         stmt.setLong(1, requestedMarkerTypeID);

         final ResultSet result = stmt.executeQuery();

         // get first result
         result.next();

         // get first value
         numMarkers = result.getInt(1);

      } catch (final SQLException e) {

         StatusUtil.logError(sql);
         UI.showSQLException(e);
      }

      return numMarkers;
   }

   public static boolean deleteTourMarkerType(final TourMarkerType selectedMarkerType) {

      if (deleteTourMarkerType_10_FromAllTourMarkers(selectedMarkerType)) {

         if (deleteTourMarkerType_20_FromDB(selectedMarkerType)) {
            return true;
         }
      }

      return false;
   }

   private static boolean deleteTourMarkerType_10_FromAllTourMarkers(final TourMarkerType selectedMarkerType) {

      boolean returnResult = false;

      try {

         final long markerTypeID = selectedMarkerType.getId();

         final EntityManager em = TourDatabase.getInstance().getEntityManager();

         final Query query = em.createQuery(UI.EMPTY_STRING

               + "SELECT TourMarker" //$NON-NLS-1$
               + " FROM " + TourMarker.class.getSimpleName() + " AS tourMarker" //$NON-NLS-1$
               + " WHERE tourMarker.tourMarkerType IS NOT NULL AND tourMarker.tourMarkerType.markerTypeID = ?"); //$NON-NLS-1$

         query.setParameter(1, markerTypeID);

         final List<?> allTourMarker = query.getResultList();
         if (allTourMarker.size() > 0) {

            final EntityTransaction ts = em.getTransaction();

            try {

               ts.begin();

               // remove tour marker type from all tour markers
               for (final Object listItem : allTourMarker) {

                  if (listItem instanceof final TourMarker tourMarker) {

                     tourMarker.setTourMarkerType(null);

                     em.merge(tourMarker);
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

      } catch (final Exception e) {

         StatusUtil.log(e);
      }

      return returnResult;
   }

   private static boolean deleteTourMarkerType_20_FromDB(final TourMarkerType markerType) {

      boolean returnResult = false;

      try {

         final EntityManager em = TourDatabase.getInstance().getEntityManager();
         final EntityTransaction ts = em.getTransaction();

         try {

            final TourMarkerType tourTypeEntity = em.find(TourMarkerType.class, markerType.getId());

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

      } catch (final Exception e) {

         StatusUtil.log(e);
      }

      return returnResult;
   }

}
