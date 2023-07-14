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

import net.tourbook.chart.ChartDataModel;
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
    * ID of a compared tour according to the {@link #_tourCompareType}
    */
   private Long                   _tourCompareId;

   /**
    * Type of the {@link #_tourCompareId}
    */
   private TourCompareType        _tourCompareType;

   private TourChartConfiguration _refTour_ChartConfig;
   private TourChartConfiguration _compareTour_ChartConfig;

   TourCompareConfig(final TourReference refTour,
                 final Long tourCompareId,
                 final ChartDataModel refChartDataModel,
                 final TourChartConfiguration refTour_ChartConfig,
                 final TourChartConfiguration compTour_ChartConfig) {

      _refTour = refTour;
      _tourCompareId = tourCompareId;

      _refTour_ChartConfig = refTour_ChartConfig;
      _compareTour_ChartConfig = compTour_ChartConfig;
   }

   TourChartConfiguration getCompareTourChartConfig() {
      return _compareTour_ChartConfig;
   }

   public TourReference getRefTour() {
      return _refTour;
   }

   TourChartConfiguration getRefTourChartConfig() {
      return _refTour_ChartConfig;
   }

   public TourData getRefTourData() {

      /*
       * Ensure to have the correct tour data, load tour data because tour data in the ref tour
       * could be changed, this is a wrong concept which could be changed but requires additonal
       * work
       */
      return TourManager.getInstance().getTourData(_tourCompareId);
   }

   public TourCompareType getTourCompareType() {
      return _tourCompareType;
   }

   public void setTourCompareType(final TourCompareType tourCompareType) {
      _tourCompareType = tourCompareType;
   }

   @Override
   public String toString() {

      return "CompareConfig" + NL //                                                //$NON-NLS-1$

            + "[" + NL //                                                           //$NON-NLS-1$

            + "  _tourCompareType         = " + _tourCompareType + NL //            //$NON-NLS-1$
            + "  _tourCompareId           = " + _tourCompareId + NL //              //$NON-NLS-1$

            + "  _refTour_ChartConfig     = " + _refTour_ChartConfig + NL //        //$NON-NLS-1$
            + "  _compareTour_ChartConfig = " + _compareTour_ChartConfig + NL //    //$NON-NLS-1$

            + "  _refTour                 = " + _refTour + NL //                    //$NON-NLS-1$

            + "]" + NL //                                                           //$NON-NLS-1$
      ;
   }

}
