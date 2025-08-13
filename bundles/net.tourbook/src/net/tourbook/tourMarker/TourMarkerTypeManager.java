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

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import net.tourbook.common.UI;
import net.tourbook.common.util.CustomScalingImageDataProvider;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourMarkerType;
import net.tourbook.database.TourDatabase;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

/**
 * Manage tour marker types
 */
public class TourMarkerTypeManager {

   private static final char                       NL                    = UI.NEW_LINE;

   private final static Map<Long, Image>           _imageCache           = new HashMap<>();
   private final static Map<Long, ImageDescriptor> _imageCacheDescriptor = new HashMap<>();

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

   private static Image createMarkerTypeImage_SWT(final long typeId) {

      final Image swtTourTypeImage = createMarkerTypeImage_SWT_Create(typeId);

      // keep image in cache
      _imageCache.put(typeId, swtTourTypeImage);

      return swtTourTypeImage;
   }

   private static Image createMarkerTypeImage_SWT_Create(final long typeId) {

      final int imageSize = (int) (TourMarkerType.MARKER_TYPE_IMAGE_SIZE * UI.HIDPI_SCALING);

      final BufferedImage awtImage = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_4BYTE_ABGR);

      final Graphics2D g2d = awtImage.createGraphics();

// SET_FORMATTING_OFF

      g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,     RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,          RenderingHints.VALUE_ANTIALIAS_ON);

// SET_FORMATTING_ON

      try {

         drawMarkerTypeImage(typeId, g2d, imageSize);

         return new Image(Display.getCurrent(), new CustomScalingImageDataProvider(awtImage));

      } finally {

         g2d.dispose();
      }
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
               + " FROM " + TourMarker.class.getSimpleName() + " AS tourMarker" //$NON-NLS-1$ //$NON-NLS-2$
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

   /**
    * Dispose images
    */
   public static void dispose() {

      for (final Image image : _imageCache.values()) {
         image.dispose();
      }

      _imageCache.clear();
      _imageCacheDescriptor.clear();
   }

   /**
    * Dispose the image for a marker type, the next time the image will be recreated.
    *
    * @param markerTypeID
    */
   public static void dispose(final long markerTypeID) {

      UI.disposeResource(_imageCache.get(markerTypeID));

      _imageCache.remove(markerTypeID);
      _imageCacheDescriptor.remove(markerTypeID);
   }

   private static void drawMarkerTypeImage(final long markerId, final Graphics2D g2d, final int imageSize) {

      final Map<Long, TourMarkerType> markerTypes_ById = TourDatabase.getAllTourMarkerTypes_ById();
      final TourMarkerType markerType = markerTypes_ById.get(markerId);

      if (markerType == null) {
         return;
      }

//    // draw debug border
//    g2d.setStroke(new BasicStroke(1));
//    g2d.setColor(Color.GRAY);
//    g2d.drawRect(0, 0, imageSize - 1, imageSize - 1);

      final String typeName = markerType.getTypeName();
      final String typeText = typeName.length() > 0 ? typeName.substring(0, 1) : UI.SYMBOL_QUESTION_MARK;

      final Font scaled4kFont = UI.getAWT4kScaledDefaultFont();
      final FontMetrics fontMetrics = g2d.getFontMetrics();

      final int textWidth = fontMetrics.stringWidth(typeText);
      final int textAscent = fontMetrics.getAscent();

      final int imageSize2 = imageSize / 2;
      final int textAscent2 = textAscent / 2;

      // center text
      final int devX = imageSize2 - textWidth; // strange, textWidth2 do not center it horizontally
      final int devY = imageSize2 + textAscent2;

      g2d.setColor(markerType.getBackgroundColorAWT());
      g2d.fillRect(0, 0, imageSize, imageSize);

      g2d.setColor(markerType.getForegroundColorAWT());
      g2d.setFont(scaled4kFont);
      g2d.drawString(typeText, devX + 1, devY + 1);
   }

   public static ImageDescriptor getMarkerTypeImageDescriptor(final long markerTypeID) {

      final ImageDescriptor existingDescriptor = _imageCacheDescriptor.get(markerTypeID);

      if (existingDescriptor != null) {
         return existingDescriptor;
      }

      final Image markerTypeImage = createMarkerTypeImage_SWT(markerTypeID);
      final ImageDescriptor newImageDesc = ImageDescriptor.createFromImage(markerTypeImage);

      _imageCacheDescriptor.put(markerTypeID, newImageDesc);

      return newImageDesc;
   }

}
