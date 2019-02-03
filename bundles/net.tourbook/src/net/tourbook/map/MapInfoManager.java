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
package net.tourbook.map;

import java.text.NumberFormat;

import org.eclipse.osgi.util.NLS;

import net.tourbook.map2.Messages;

public class MapInfoManager {

   private static MapInfoManager _instance;

   private double                _latitude     = Double.MIN_VALUE;
   private double                _longitude;

   private int                   _mapZoomLevel = -1;

   private final NumberFormat    _nf0          = NumberFormat.getNumberInstance();
   private final NumberFormat    _nf1          = NumberFormat.getNumberInstance();
   private final NumberFormat    _nf2          = NumberFormat.getNumberInstance();
   private final NumberFormat    _nf3          = NumberFormat.getNumberInstance();
   private final NumberFormat    _nf4          = NumberFormat.getNumberInstance();
   private final NumberFormat    _nf5          = NumberFormat.getNumberInstance();
   private final NumberFormat    _nf6          = NumberFormat.getNumberInstance();

   {
      _nf0.setMinimumFractionDigits(0);
      _nf0.setMaximumFractionDigits(0);

      _nf1.setMinimumFractionDigits(1);
      _nf1.setMaximumFractionDigits(1);

      _nf2.setMinimumFractionDigits(2);
      _nf2.setMaximumFractionDigits(2);

      _nf3.setMinimumFractionDigits(3);
      _nf3.setMaximumFractionDigits(3);

      _nf4.setMinimumFractionDigits(4);
      _nf4.setMaximumFractionDigits(4);

      _nf5.setMinimumFractionDigits(5);
      _nf5.setMaximumFractionDigits(5);

      _nf6.setMinimumFractionDigits(6);
      _nf6.setMaximumFractionDigits(6);
   }

   private MapInfoControl _infoWidget;

   public static MapInfoManager getInstance() {

      if (_instance == null) {
         _instance = new MapInfoManager();
      }

      return _instance;
   }

   public void resetInfo() {

      _latitude = Double.MIN_VALUE;

      updateUI();
   }

   void setInfoWidget(final MapInfoControl infoWidget) {

      _infoWidget = infoWidget;

      updateUI();
   }

   public void setMapPosition(final double latitude, final double longitude, final int zoomLevel) {

      _latitude = latitude;
      _longitude = longitude;

      _mapZoomLevel = zoomLevel;

      updateUI();
   }

   private void updateUI() {

      // check widget
      if ((_infoWidget == null) || _infoWidget.isDisposed()) {
         return;
      }

      // check data
      if ((_latitude == Double.MIN_VALUE) || (_mapZoomLevel == -1)) {

         _infoWidget.setText(Messages.statusLine_mapInfo_defaultText);

      } else {

         // reduce digits when not necessary

         double lon = _longitude % 360;
         lon = lon > 180 ? //
               lon - 360
               : lon < -180 ? //
                     lon + 360
                     : lon;

         final int minZoomLevel_Digit_1 = 1;
         final int minZoomLevel_Digit_2 = minZoomLevel_Digit_1 + 3;
         final int minZoomLevel_Digit_3 = minZoomLevel_Digit_2 + 3;
         final int minZoomLevel_Digit_4 = minZoomLevel_Digit_3 + 3;
         final int minZoomLevel_Digit_5 = minZoomLevel_Digit_4 + 3;
         final int minZoomLevel_Digit_6 = minZoomLevel_Digit_5 + 3;

         final String latText =

               _mapZoomLevel > minZoomLevel_Digit_6 ? _nf6.format(_latitude) : //

                     _mapZoomLevel > minZoomLevel_Digit_5 ? _nf5.format(_latitude) : //

                           _mapZoomLevel > minZoomLevel_Digit_4 ? _nf4.format(_latitude) : //

                                 _mapZoomLevel > minZoomLevel_Digit_3 ? _nf3.format(_latitude) : //

                                       _mapZoomLevel > minZoomLevel_Digit_2 ? _nf2.format(_latitude) : //

                                             _mapZoomLevel > minZoomLevel_Digit_1 ? _nf1.format(_latitude) : //

                                                   _nf0.format(_latitude);

         final String lonText =

               _mapZoomLevel > minZoomLevel_Digit_6 ? _nf6.format(lon) : //

                     _mapZoomLevel > minZoomLevel_Digit_5 ? _nf5.format(lon) : //

                           _mapZoomLevel > minZoomLevel_Digit_4 ? _nf4.format(lon) : //

                                 _mapZoomLevel > minZoomLevel_Digit_3 ? _nf3.format(lon) : //

                                       _mapZoomLevel > minZoomLevel_Digit_2 ? _nf2.format(lon) : //

                                             _mapZoomLevel > minZoomLevel_Digit_1 ? _nf1.format(lon) : //

                                                   _nf0.format(lon);

         _infoWidget.setText(
               NLS.bind(
                     Messages.statusLine_mapInfo_data,
                     new Object[] {
                           latText,
                           lonText,
                           Integer.toString(_mapZoomLevel + 1) }));
      }
   }

}
