/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
package net.tourbook.ext.srtm;

public class GeoLat extends GeoCoord {

   public char directionPlus() {return 'N';}
   public char directionMinus() {return 'S';}

   public GeoLat() {
      super();
   }

   public GeoLat(GeoLat lat) {
      super();
      set(lat);
   }

   public GeoLat(String s) {
      super();
      set(s);
   }

   public GeoLat(double d) {
      super();
      set(d);
   }

   public boolean isNorth() {
      return (direction == directionPlus());
   }

   public boolean isSouth() {
      return (!this.isNorth());
   }

   public void add(GeoLat lat) {

      decimal += lat.getDecimal();
       if (decimal > 90 *faktg) decimal = 180*faktg - decimal;

      updateDegrees();
   }

   public void add(GeoLat lat, GeoLat a) {
      decimal = lat.getDecimal();
      this.add(a);
   }
   
   public void sub(GeoLat lat) {

      decimal -= lat.getDecimal();
      if (decimal < -90 *faktg) decimal = -180*faktg - decimal;

      updateDegrees();
   }

   public void sub(GeoLat lat, GeoLat s) {
      decimal = lat.getDecimal();
      this.sub(s);
   }

   public void set(GeoLat lat) {
      super.set((GeoCoord)lat);
   }

   public static void main(String[] args) {

   }
   

}
