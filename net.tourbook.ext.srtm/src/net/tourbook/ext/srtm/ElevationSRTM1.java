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

import java.io.File;
import java.util.HashMap;

public class ElevationSRTM1 extends ElevationBase {
   
   ;
   static private SRTM1I fSRTMi;
   static private HashMap<Integer,SRTM1I> hm;

   private class SRTM1I {

      ElevationFile elevationFile;

      private SRTM1I(final GeoLat lat, final GeoLon lon) {

			final String srtm1DataPath = getElevationDataPath("srtm1"); //$NON-NLS-1$
			final String srtm1Suffix = ".hgt"; //$NON-NLS-1$

			final String fileName = new String(srtm1DataPath
					+ File.separator
					+ lat.direction
					+ NumberForm.n2((lat.direction == GeoLat.DIRECTION_NORTH) ? lat.degrees : lat.degrees + 1)
					+ lon.direction
					+ NumberForm.n3((lon.direction == GeoLon.DIRECTION_EAST) ? lon.degrees : lon.degrees + 1)
					+ srtm1Suffix);

         try {
            elevationFile = new ElevationFile(fileName,  Constants.ELEVATION_TYPE_SRTM1);
         } catch (final Exception e) {
            System.out.println("SRTM1I: Error: " + e.getMessage()); // NOT File not found //$NON-NLS-1$
            // dont return exception
         }
       }
      
      public short getElevation(final GeoLat lat, final GeoLon lon) {
      	return elevationFile.get(offset(lat, lon));
      }

      //    Offset in the SRTM1-File
      public int offset(final GeoLat lat, final GeoLon lon) {

      	if (lat.direction == GeoLat.DIRECTION_SOUTH) {
      		if (lon.direction == GeoLon.DIRECTION_EAST)
      			return 3601 * (lat.minutes * 60 + lat.seconds)
            		+ lon.minutes * 60 + lon.seconds;
      		else
      			return 3601 * (lat.minutes * 60 + lat.seconds)
					+ 3599 - lon.minutes * 60 - lon.seconds;
      	}
      	else {
      		if (lon.direction == GeoLon.DIRECTION_EAST)
      			return 3601 * (3599 - lat.minutes * 60 - lat.seconds)
            		+ lon.minutes * 60 + lon.seconds;
      		else
      			return 3601 * (3599 - lat.minutes * 60 - lat.seconds)
					+ 3599 - lon.minutes * 60 - lon.seconds;
      	}
      }

   }

   public static void main(final String[] args) {
   }

   public ElevationSRTM1() {
      // create map with used Files
      // to find file, calculate and remember key value
      hm = new HashMap<Integer,SRTM1I>(); // default initial 16 Files
      gridLat.setDegreesMinutesSecondsDirection(0, 0, 1, 'N');
      gridLon.setDegreesMinutesSecondsDirection(0, 0, 1, 'E');
   }
   
   @Override
public short getElevation(final GeoLat lat, final GeoLon lon) {

      if (lat.tertias != 0) return getElevationGrid(lat, lon);
      if (lon.tertias != 0) return getElevationGrid(lat, lon);
      
      int i = lon.degrees;
      if (lon.direction == GeoLon.DIRECTION_WEST)
         i += 256;
      i *= 1024;
      i += lat.degrees;
      if (lat.direction == GeoLat.DIRECTION_SOUTH)
         i += 256;
      final Integer ii = new Integer(i);
      fSRTMi = hm.get(ii);

      if (fSRTMi == null) {
         // first time only
         fSRTMi = new SRTM1I(lat, lon);
         hm.put(ii, fSRTMi);
      }

      return fSRTMi.getElevation(lat, lon);

   }
   
   @Override
public double getElevationDouble(final GeoLat lat, final GeoLon lon) {

      if (lat.decimal == 0 && lon.decimal == 0) return 0.;
      if (lat.tertias != 0) return getElevationGridDouble(lat, lon);
      if (lon.tertias != 0) return getElevationGridDouble(lat, lon);
      return getElevation(lat, lon);
   }

   @Override
public String getName() {
   	return "SRTM1"; //$NON-NLS-1$
   }

   @Override
public short getSecDiff() {
	   // number of degrees seconds between two data points
	   return 1;
   }
}
