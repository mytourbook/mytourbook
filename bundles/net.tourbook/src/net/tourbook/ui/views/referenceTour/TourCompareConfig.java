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

import net.tourbook.common.UI;
import net.tourbook.data.TourData;
import net.tourbook.data.TourReference;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.tourChart.TourChartConfiguration;

/**
 * Contains data and configuration for an elevation or geo compared tour
 */
public class TourCompareConfig {

   private static final char      NL = UI.NEW_LINE;

   private TourReference          _refTour;

   /**
    * Tour ID of the compared reference tour
    */
   private Long                   _refTour_TourId;

   /**
    * Type of the {@link #_refTour_TourId}
    */
   private TourCompareType        _tourCompareType;

   private TourChartConfiguration _chartConfig_RefTour;
   private TourChartConfiguration _chartConfig_ComparedTour;

   TourCompareConfig(final TourReference refTour,
                     final Long refTour_TourId,
                     final TourChartConfiguration chartConfig_RefTour,
                     final TourChartConfiguration chartConfig_ComparedTour) {

      _refTour = refTour;
      _refTour_TourId = refTour_TourId;

      _chartConfig_RefTour = chartConfig_RefTour;
      _chartConfig_ComparedTour = chartConfig_ComparedTour;
   }

   TourChartConfiguration getCompareTourChartConfig() {
      return _chartConfig_ComparedTour;
   }

   public TourReference getRefTour() {
      return _refTour;
   }

   public long getRefTour_RefId() {

      if (_refTour != null) {
         return _refTour.getRefId();
      }

      return -1;
   }

   public Long getRefTour_TourId() {
      return _refTour_TourId;
   }

   TourChartConfiguration getRefTourChartConfig() {
      return _chartConfig_RefTour;
   }

   public TourData getRefTourData() {

      /*
       * Ensure to have the correct tour data, load tour data because tour data in the ref tour
       * could be changed, this is a wrong concept which could be changed but requires additonal
       * work
       */
      return TourManager.getInstance().getTourData(_refTour_TourId);
   }

   public TourCompareType getTourCompareType() {
      return _tourCompareType;
   }

   public void setTourCompareType(final TourCompareType tourCompareType) {
      _tourCompareType = tourCompareType;
   }

   @Override
   public String toString() {

      return "CompareConfig" + NL //                                                   //$NON-NLS-1$

            + "[" + NL //                                                              //$NON-NLS-1$

            + "  _tourCompareType          = " + _tourCompareType + NL //              //$NON-NLS-1$
            + "  _refTour_TourId           = " + _refTour_TourId + NL //                //$NON-NLS-1$

            + "  _chartConfig_RefTour      = " + _chartConfig_RefTour + NL //          //$NON-NLS-1$
            + "  _chartConfig_ComparedTour = " + _chartConfig_ComparedTour + NL //     //$NON-NLS-1$

            + "  _refTour                  = " + _refTour + NL //                      //$NON-NLS-1$

            + "]" + NL //                                                              //$NON-NLS-1$
      ;
   }

}
