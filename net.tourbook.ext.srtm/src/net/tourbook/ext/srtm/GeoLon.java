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

public class GeoLon extends GeoCoord {

   public char directionPlus() {return 'E';}
   public char directionMinus() {return 'W';}

   public GeoLon() {
      super();
   }

   public GeoLon(GeoLon lon) {
      super();
      set(lon);
   }

   public GeoLon(String s) {
      super();
      set(s);
   }
   
   public GeoLon(double d) {
      super();
      set(d);
   }
   
   public boolean isEast() {
      return (direction == directionPlus());
   }

   public boolean isWest() {
      return (!this.isEast());
   }

   public void add(GeoLon lon) {

      decimal += lon.getDecimal();
      if (decimal > 180*faktg) decimal -= 360*faktg;

      updateDegrees();
   }

   public void add(GeoLon lon, GeoLon a) {
      decimal = lon.getDecimal();
      this.add(a);
   }
   
   public void sub(GeoLon lon) {

      decimal -= lon.getDecimal();
      if (decimal < -180*faktg) decimal += 360*faktg;

      updateDegrees();
   }

   public void sub(GeoLon lon, GeoLon s) {
      decimal = lon.getDecimal();
      this.sub(s);
   }
      
   public void set(GeoLon lon) {
      super.set((GeoCoord)lon);
   }

   public static void main(String[] args) {
   }
}
