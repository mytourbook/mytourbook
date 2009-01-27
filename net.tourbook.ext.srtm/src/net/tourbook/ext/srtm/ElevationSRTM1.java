package net.tourbook.ext.srtm;

import java.io.File;
import java.util.*;

public class ElevationSRTM1 extends ElevationBase {
   
   ;
   static private SRTM1I fSRTMi;
   static private HashMap<Integer,SRTM1I> hm;

   public ElevationSRTM1() {
      // FileLog.println(this, "ElevationSRTM1 Konstructor!");
      
      // Map mit benutzten Files anlegen
      // um den File zu finden, wird eine Schluesselzahl berechnet und gemerkt
      hm = new HashMap<Integer,SRTM1I>(); // default initial 16 Files
      bGrid.setGradMinutenSekundenRichtung(0, 0, 1, 'N');
      lGrid.setGradMinutenSekundenRichtung(0, 0, 1, 'E');
   }

   public short getElevation(GeoLat lat, GeoLon lon) {

      if (lat.getTertia() != 0) return getElevationGrid(lat, lon);
      if (lon.getTertia() != 0) return getElevationGrid(lat, lon);
      
      int i = lon.getGrad();
      if (lon.isWesten())
         i += 256;
      i *= 1024;
      i += lat.getGrad();
      if (lat.isSueden())
         i += 256;
      Integer ii = new Integer(i);
      fSRTMi = (SRTM1I)hm.get(ii);

      if (fSRTMi == null) {
         // nur beim jeweils ersten Mal
         // FileLog.println(this, "Index ElevationSRTM1 " + ii);
         fSRTMi = new SRTM1I(lat, lon);
         hm.put(ii, fSRTMi);
      }

      return fSRTMi.getElevation(lat, lon);

   }

   public double getElevationDouble(GeoLat lat, GeoLon lon) {

      if (lat.getDezimal() == 0 && lon.getDezimal() == 0) return 0.;
      if (lat.getTertia() != 0) return getElevationGridDouble(lat, lon);
      if (lon.getTertia() != 0) return getElevationGridDouble(lat, lon);
      return (double) getElevation(lat, lon);
   }
   
   public short getSekDiff() {
   	// Anzahl Gradsekunden zwischen zwei Datenpunkten
   	return 1;
   }
   
   public String getName() {
   	return "SRTM1";
   }

   private class SRTM1I {

      ElevationFile elevationFile;

      private SRTM1I(GeoLat lat, GeoLon lon) {

    	  final String elevationDataPath = getElevationDataPath();
    	  final String srtm1Dir = elevationDataPath + File.separator + "srtm1"; // Lokale Lage der SRTM1-Files!!!
    	  final String srtm1Suffix = ".hgt";

    	  String fileName = new String();
    	  fileName =srtm1Dir
    	       + File.separator
               + lat.getRichtung()
               + NumberForm.n2(lat.isNorden() ? lat.getGrad() : lat.getGrad() + 1)
               + lon.getRichtung()
               + NumberForm.n3(lon.isOsten() ? lon.getGrad() : lon.getGrad() + 1)
  			   + srtm1Suffix;

         try {
            elevationFile = new ElevationFile(fileName,  Constants.ELEVATION_TYPE_SRTM1);
         } catch (Exception e) {
            System.out.println("SRTM1I: Fehler: " + e.getMessage()); // NICHT File not found
            // Exception nicht weitergeben
         }
       }
      
      public short getElevation(GeoLat lat, GeoLon lon) {
      	return elevationFile.get(offset(lat, lon));
      }

      //    Offset im SRTM1-File
      public int offset(GeoLat lat, GeoLon lon) {

      	if (lat.isSueden()) {
      		if (lon.isOsten())
      			return 3601 * (lat.getMinuten() * 60 + lat.getSekunden())
            		+ lon.getMinuten() * 60 + lon.getSekunden();
      		else
      			return 3601 * (lat.getMinuten() * 60 + lat.getSekunden())
					+ 3599 - lon.getMinuten() * 60 - lon.getSekunden();
      	}
      	else {
      		if (lon.isOsten())
      			return 3601 * (3599 - lat.getMinuten() * 60 - lat.getSekunden())
            		+ lon.getMinuten() * 60 + lon.getSekunden();
      		else
      			return 3601 * (3599 - lat.getMinuten() * 60 - lat.getSekunden())
					+ 3599 - lon.getMinuten() * 60 - lon.getSekunden();
      	}
      }

   }

   public static void main(String[] args) {
   }
}
