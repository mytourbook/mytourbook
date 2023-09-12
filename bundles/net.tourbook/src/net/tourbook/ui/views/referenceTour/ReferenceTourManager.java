/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.referenceTour;

import java.util.HashMap;

import javax.persistence.EntityManager;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.data.TourData;
import net.tourbook.data.TourReference;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.tourChart.TourChartConfiguration;

import org.eclipse.jface.dialogs.IDialogSettings;

public class ReferenceTourManager {

   private static final IDialogSettings _state = TourbookPlugin.getState("net.tourbook.ui.views.tourCatalog.ReferenceTourManager"); //$NON-NLS-1$
   //
   //
   /**
    * Key is the reference ID
    */
   private static final HashMap<Long, TourCompareConfig> _compareConfig_Cache = new HashMap<>();

   /**
    * When {@link #_geoCompare_RefId} == 0 then {@link #getTourCompareConfig(long)} will return
    * <code>null</code>, this is wrong when refId == 0
    */
   private static long                                   _geoCompare_RefId    = Long.MIN_VALUE;

   private static TourReference                          _geoCompare_RefTour;
   private static TourCompareConfig                      _geoCompare_RefTour_Config;

   private ReferenceTourManager() {}

   /**
    * Create a new reference tour configuration
    *
    * @param tourCompareType
    */
   private static TourCompareConfig createTourCompareConfig(final TourReference refTour,
                                                            final TourCompareType tourCompareType) {

      final TourData refTourData = refTour.getTourData();

      final TourChartConfiguration refTourChartConfig = TourManager.createDefaultTourChartConfig(_state);
      final TourChartConfiguration compTourchartConfig = TourManager.createDefaultTourChartConfig(_state);

      final TourCompareConfig compareConfig = new TourCompareConfig(
            refTour,
            refTourData.getTourId(),
            refTourChartConfig,
            compTourchartConfig);

      compareConfig.setTourCompareType(tourCompareType);

      return compareConfig;
   }

   public static long getGeoCompare_RefId() {
      return _geoCompare_RefId;
   }

   public static TourReference getGeoCompare_RefTour() {

      return _geoCompare_RefTour;
   }

   /**
    * @return Returns tour id of the geo compare ref tour or -1 when not available
    */
   public static long getGeoCompare_RefTour_TourId() {

      if (_geoCompare_RefTour == null) {
         return -1;
      }

      return _geoCompare_RefTour.getTourData().getTourId();
   }

   /**
    * Load {@link TourReference} from the database
    *
    * @param refId
    * @return Returns {@link TourReference} for the <code>refId</code>
    */
   public static TourReference getReferenceTour(final long refId) {

      TourReference refTour;

      final EntityManager em = TourDatabase.getInstance().getEntityManager();
      {
         refTour = em.find(TourReference.class, refId);
      }
      em.close();

      return refTour;
   }

   /**
    * Returns a {@link TourCompareConfig} or <code>null</code> when the reference tour cannot
    * be loaded from the database
    *
    * @param refTour_RefId
    *           Reference Id
    * @return
    */
   public static TourCompareConfig getTourCompareConfig(final long refTour_RefId) {

      if (refTour_RefId == _geoCompare_RefId) {

         return _geoCompare_RefTour_Config;
      }

      final TourCompareConfig compareConfig = _compareConfig_Cache.get(refTour_RefId);

      if (compareConfig != null) {

         return compareConfig;
      }

      // load reference tour from the database
      TourReference refTour;
      final EntityManager em = TourDatabase.getInstance().getEntityManager();
      {
         refTour = em.find(TourReference.class, refTour_RefId);
      }
      em.close();

      if (refTour == null) {
         return null;
      }

      final TourCompareConfig newCompareConfig = createTourCompareConfig(refTour, TourCompareType.ANY_COMPARE_REFERENCE_TOUR);

      // keep ref config in the cache
      _compareConfig_Cache.put(refTour_RefId, newCompareConfig);

      return newCompareConfig;
   }

   /**
    * @param refTour
    * @return
    */ 
   public static long setupGeoCompareRefTour_FromNative(final TourReference refTour) {

      _geoCompare_RefTour = refTour;

      _geoCompare_RefId = refTour.getRefId();
      _geoCompare_RefTour_Config = createTourCompareConfig(_geoCompare_RefTour, TourCompareType.GEO_COMPARE_REFERENCE_TOUR);

      return _geoCompare_RefId;
   }

   /**
    * Creates a special {@link TourReference} which is NOT saved in the db
    *
    * @param tourData
    * @param startIndex
    * @param endIndex
    * @return Returns the special ref id
    */
   public static long setupGeoCompareRefTour_Virtual(final TourData tourData, final int startIndex, final int endIndex) {

      // create a virtual reference tour
      _geoCompare_RefTour = new TourReference(
            UI.EMPTY_STRING,
            tourData,
            startIndex,
            endIndex);

      _geoCompare_RefTour.setIsVirtualRefTour();

      _geoCompare_RefId = System.nanoTime();
      _geoCompare_RefTour_Config = createTourCompareConfig(_geoCompare_RefTour, TourCompareType.GEO_COMPARE_ANY_TOUR);

      return _geoCompare_RefId;
   }
}
