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
package net.tourbook.chart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ChartYDataMinMaxKeeper {

   /**
    * Min/max values for the y-axis data
    */
   private HashMap<Integer, Double> _minValues = null;
   private HashMap<Integer, Double> _maxValues = null;

   private class MinMaxCloneValues {

      public Integer         fromGraphID;

      public ChartDataYSerie intoYData;
   }

   public ChartYDataMinMaxKeeper() {}

   HashMap<Integer, Double> getMaxValues() {
      return _maxValues;
   }

   HashMap<Integer, Double> getMinValues() {
      return _minValues;
   }

   private ChartDataYSerie getYDataFromGraphID(final List<ChartDataSerie> xyData, final Integer requestedGraphID) {

      for (final ChartDataSerie dataSerie : xyData) {

         if (dataSerie instanceof ChartDataYSerie) {

            final ChartDataYSerie yData = (ChartDataYSerie) dataSerie;
            final Integer yGraphId = (Integer) yData.getCustomData(ChartDataYSerie.YDATA_GRAPH_ID);

            if (requestedGraphID.equals(yGraphId)) {

               return yData;
            }
         }
      }

      return null;
   }

   public boolean hasMinMaxChanged() {
      final boolean isEqual = _minValues.equals(_maxValues);
      return !isEqual;
   }

   /**
    * keep the min/max values for all data series from the data model
    *
    * @param chartDataModel
    */
   public void saveMinMaxValues(final ChartDataModel chartDataModel) {

      if (chartDataModel == null) {
         return;
      }

      final ArrayList<ChartDataSerie> xyData = chartDataModel.getXyData();

      _minValues = new HashMap<>();
      _maxValues = new HashMap<>();

      // loop: save min/max values for all data series
      for (final ChartDataSerie chartData : xyData) {

         if (chartData instanceof ChartDataYSerie) {

            final ChartDataYSerie yData = (ChartDataYSerie) chartData;

            final Integer yDataInfo = (Integer) yData.getCustomData(ChartDataYSerie.YDATA_GRAPH_ID);

            if (yDataInfo != null) {

               final double visibleMinValue = yData.getVisibleMinValue();
               double visibleMaxValue = yData.getVisibleMaxValue();

               // prevent setting to the same value,
               if (visibleMinValue == visibleMaxValue) {
                  visibleMaxValue++;
               }

               _minValues.put(yDataInfo, visibleMinValue);
               _maxValues.put(yDataInfo, visibleMaxValue);
            }
         }
      }
   }

   /**
    * Set min/max values from this min/max keeper into a data model
    *
    * @param chartDataModelOut
    *           data model which min/max data will be set from this min/max keeper
    */
   public void setMinMaxValues(final ChartDataModel chartDataModelOut) {

      if (_minValues == null) {

         // min/max values have not yet been saved, so nothing can be restored

         return;
      }

      final List<MinMaxCloneValues> allClonedMinMaxValues = new ArrayList<>();
      final List<ChartDataSerie> xyData = chartDataModelOut.getXyData();

      // loop: restore min/max values for all data series
      for (final ChartDataSerie dataSerie : xyData) {

         if (dataSerie instanceof ChartDataYSerie) {

            final ChartDataYSerie yData = (ChartDataYSerie) dataSerie;

            final Integer yGraphId = (Integer) yData.getCustomData(ChartDataYSerie.YDATA_GRAPH_ID);

            if (yGraphId != null) {

               final Double minValue = _minValues.get(yGraphId);
               if (minValue != null) {
                  yData.setVisibleMinValue(minValue);
               }

               final Double maxValue = _maxValues.get(yGraphId);
               if (maxValue != null) {
                  yData.setVisibleMaxValue(maxValue);
               }

               /*
                * Create clone values
                */
               final Integer cloneFromGraphID = (Integer) yData.getCustomData(ChartDataYSerie.CLONE_MIN_MAX_VALUES_GRAPH_ID);
               if (cloneFromGraphID != null) {

                  final MinMaxCloneValues minMaxClone = new MinMaxCloneValues();

                  minMaxClone.fromGraphID = cloneFromGraphID;
                  minMaxClone.intoYData = yData;

                  allClonedMinMaxValues.add(minMaxClone);
               }
            }
         }
      }

      /*
       * Clone min/max values
       */
      for (final MinMaxCloneValues minMaxCloneValues : allClonedMinMaxValues) {

         final Integer cloneFromID = minMaxCloneValues.fromGraphID;
         final ChartDataYSerie yDataFrom = getYDataFromGraphID(xyData, cloneFromID);

         if (yDataFrom != null) {

            minMaxCloneValues.intoYData.setVisibleMinValue(yDataFrom.getVisibleMinValue());
            minMaxCloneValues.intoYData.setVisibleMaxValue(yDataFrom.getVisibleMaxValue());
         }
      }
   }

}
