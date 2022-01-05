/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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
/**
 *
 */
package net.tourbook.ui.views.tourCatalog;

import java.util.HashMap;

import javax.persistence.EntityManager;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.data.TourData;
import net.tourbook.data.TourReference;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.tourChart.TourChartConfiguration;

import org.eclipse.jface.dialogs.IDialogSettings;

/**
 *
 */
public class ReferenceTourManager {

   private static final IDialogSettings _state = TourbookPlugin.getState("net.tourbook.ui.views.tourCatalog.ReferenceTourManager");//$NON-NLS-1$

   //
   private static final HashMap<Long, TourCompareConfig> _compareConfigCache = new HashMap<>();

   /**
    * When {@link #_geoCompare_RefId} == 0 then {@link #getTourCompareConfig(long)} will return
    * <code>null</code>, this is wrong when refId == 0
    */
   private static long                                   _geoCompare_RefId   = Long.MIN_VALUE;
   private static TourReference                          _geoCompare_RefTour;
   private static TourCompareConfig                      _geoCompare_RefConfig;

   private ReferenceTourManager() {}

   /**
    * Creates a special {@link TourReference} which is NOT saved in the db
    *
    * @param tourData
    * @param startIndex
    * @param endIndex
    * @return Returns the special ref id
    */
   public static long createGeoCompareRefTour(final TourData tourData, final int startIndex, final int endIndex) {

      final String refTourLabel = Messages.Geo_Compare_Label_ReferenceTour;

      _geoCompare_RefId = System.nanoTime();

      _geoCompare_RefTour = new TourReference(
            refTourLabel,
            tourData,
            startIndex,
            endIndex);

      _geoCompare_RefConfig = createTourCompareConfig(_geoCompare_RefTour);
      _geoCompare_RefConfig.isGeoCompareRefTour = true;

      return _geoCompare_RefId;
   }

   /**
    * Create a new reference tour configuration
    */
   private static TourCompareConfig createTourCompareConfig(final TourReference refTour) {

      final TourData refTourData = refTour.getTourData();

      final TourChartConfiguration refTourChartConfig = TourManager.createDefaultTourChartConfig(_state);
      final TourChartConfiguration compTourchartConfig = TourManager.createDefaultTourChartConfig(_state);

      final ChartDataModel chartDataModel = TourManager.getInstance().createChartDataModel(
            refTourData,
            refTourChartConfig);

      final TourCompareConfig compareConfig = new TourCompareConfig(
            refTour,
            chartDataModel,
            refTourData.getTourId(),
            refTourChartConfig,
            compTourchartConfig);

      return compareConfig;
   }

   public static TourReference getGeoCompareReferenceTour() {
      return _geoCompare_RefTour;
   }

   /**
    * @return Returns tour id of the geo compare ref tour or -1 when not available
    */
   public static long getGeoCompareReferenceTourId() {

      if (_geoCompare_RefTour == null) {
         return -1;
      }

      return _geoCompare_RefTour.getTourData().getTourId();
   }

   /**
    * Returns a {@link TourCompareConfig} or <code>null</code> when the reference tour cannot be
    * loaded from the database
    *
    * @param refId
    *           Reference Id
    * @return
    */
   public static TourCompareConfig getTourCompareConfig(final long refId) {

      if (refId == _geoCompare_RefId) {
         return _geoCompare_RefConfig;
      }

      final TourCompareConfig compareConfig = _compareConfigCache.get(refId);

      if (compareConfig != null) {
         return compareConfig;
      }

      // load the reference tour from the database
      final EntityManager em = TourDatabase.getInstance().getEntityManager();
      final TourReference refTour = em.find(TourReference.class, refId);
      em.close();

      if (refTour == null) {
         return null;
      }

      final TourCompareConfig newCompareConfig = createTourCompareConfig(refTour);

      // keep ref config in the cache
      _compareConfigCache.put(refId, newCompareConfig);

      return newCompareConfig;
   }

}
