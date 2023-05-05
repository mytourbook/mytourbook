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
 * @author Alfred Barten
 */
package net.tourbook.srtm;

public class ElevationLayer {

   private static ElevationBase[] _allElevations  = new ElevationBase[4];

   private static ElevationEtopo  _elevationEtopo = new ElevationEtopo();
   private static ElevationGlobe  _elevationGlobe = new ElevationGlobe();
   private static ElevationSRTM3  _elevationSrtm3 = new ElevationSRTM3();
//   private static ElevationSRTM1  _elevationSrtm1 = new ElevationSRTM1();

   private static int             zoom;
   private static int             fileTypIndexStart;

   public ElevationLayer() {

      _allElevations[0] = _elevationEtopo;
      _allElevations[1] = _elevationGlobe;
      _allElevations[2] = _elevationSrtm3;
//      _allElevations[3] = _elevationSrtm1;
//      System.out.println("******************* ElevationLayer *********************");
      zoom = 0;
   }

   public float getElevation(final GeoLat lat, final GeoLon lon) {

      int layerIndex = fileTypIndexStart;

      while (layerIndex >= 0) {
         try {
            final float hoehe = _allElevations[layerIndex].getElevation(lat, lon);

            if (_allElevations[layerIndex].isValid(hoehe)) {
               return hoehe;
            } else {
               layerIndex--;
            }
         } catch (final Exception e) {
            layerIndex--;
         }
      }

      layerIndex = 0;

      return -500;
   }

   private int getElevationType() {

      if (zoom <= 4) {
         return ElevationType.ETOPO;
      }

      if (zoom <= 8) {
         return ElevationType.GLOBE;
      }

//      if (zoom <= 14) {
      return ElevationType.SRTM3;
//      }
//
//      return Constants.ELEVATION_TYPE_SRTM1;
   }

   public String getName() {

      // ETOPO, GLOBE, SRTM3, SRTM1
      return _allElevations[getElevationType()].getName();
   }

   public short getSekDiff() {

      // Anzahl Degreesseconds zwischen zwei Datenpunkten
      return _allElevations[getElevationType()].getSecDiff();
   }

   private void setFileTypIndexStart() {

      fileTypIndexStart = getElevationType();
   }

   public void setZoom(final int z) {

      zoom = z;
      setFileTypIndexStart();
   }

}
