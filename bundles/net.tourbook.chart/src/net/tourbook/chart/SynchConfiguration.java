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

/**
 * {@link SynchConfiguration} is used to synchronize 2 charts. Chart A sets the
 * zoomMarkerPositionOut which can be read (and set to zoomMarkerPositionIn) from the chart B. Chart
 * B will then be synched to chart A
 */
public class SynchConfiguration {

   /**
    * width for the marker, this can be smaller or wider than the visible part of the chart
    */
   private float                  _devMarkerWidth;

   /**
    * offset for the marker start position, this value starts at the left position of the visible
    * graph, this can also be a negative value
    */
   private float                  _devMarkerOffset;

   private float                  _markerWidthRatio;
   private float                  _markerOffsetRatio;

   private ChartYDataMinMaxKeeper _yDataMinMaxKeeper = new ChartYDataMinMaxKeeper();

   /**
    * The ZoomMarkerPosition describes the position and width for the x-marker in the graph
    *
    * @param chartDataModel
    * @param markerWidthRatio
    * @param markerOffsetRatio
    */
   public SynchConfiguration(final ChartDataModel chartDataModel,
                             final float devMarkerWidth,
                             final float devMarkerOffset,
                             final float markerWidthRatio,
                             final float markerOffsetRatio) {

      _devMarkerWidth = devMarkerWidth;
      _devMarkerOffset = devMarkerOffset;

      _markerWidthRatio = markerWidthRatio;
      _markerOffsetRatio = markerOffsetRatio;

      _yDataMinMaxKeeper.saveMinMaxValues(chartDataModel);
   }

   float getDevMarkerOffset() {
      return _devMarkerOffset;
   }

   float getDevMarkerWidth() {
      return _devMarkerWidth;
   }

   float getMarkerOffsetRatio() {
      return _markerOffsetRatio;
   }

   float getMarkerWidthRatio() {
      return _markerWidthRatio;
   }

   ChartYDataMinMaxKeeper getYDataMinMaxKeeper() {
      return _yDataMinMaxKeeper;
   }

   /**
    * @param newSynchConfig
    * @return Returns <code>true</code> when the newXMarkerPosition has the same values as the
    *         current object
    */
   public boolean isEqual(final SynchConfiguration newSynchConfig) {

      if (_devMarkerWidth == newSynchConfig.getDevMarkerWidth()
            && _devMarkerOffset == newSynchConfig.getDevMarkerOffset()
            && _yDataMinMaxKeeper.hasMinMaxChanged() == false) {

         return true;
      }

      return false;
   }

}
