package net.tourbook.ext.srtm;
import java.io.File;
import java.util.*;

public final class ElevationSRTM3 extends Elevation {
   
   
   static private SRTM3I srtm3I;
   static final private HashMap<Integer,SRTM3I> hm = new HashMap<Integer,SRTM3I>(); // default initial 16 Files

   public ElevationSRTM3() {
      // FileLog.println(this, "ElevationSRTM3 Konstructor!");
      
      // Map mit benutzten Files anlegen
      // um den File zu finden, wird eine Schluesselzahl berechnet und gemerkt
      // hm = new HashMap<Integer,SRTM3I>(); // default initial 16 Files
      bGrid.setGradMinutenSekundenRichtung(0, 0, 3, 'N');
      lGrid.setGradMinutenSekundenRichtung(0, 0, 3, 'E');
   }

   public short getElevation(GeoLat b, GeoLon l) {

      if (b.getTertia() != 0) return getElevationGrid(b, l);
      if (l.getTertia() != 0) return getElevationGrid(b, l);
      if (b.getSekunden() % 3 != 0) return getElevationGrid(b, l);
      if (l.getSekunden() % 3 != 0) return getElevationGrid(b, l);
      
      int i = l.getGrad();
      if (l.isWesten())
         i += 256;
      i *= 1024;
      i += b.getGrad();
      if (b.isSueden())
         i += 256;
      Integer ii = new Integer(i);
      srtm3I = (SRTM3I)hm.get(ii);

      if (srtm3I == null) {
         // nur beim jeweils ersten Mal
         // FileLog.println(this, "Index ElevationSRTM3 " + ii);
         srtm3I = new SRTM3I(b, l);
         hm.put(ii, srtm3I);
      }

      return srtm3I.getElevation(b, l);

   }

   public double getElevationDouble(GeoLat b, GeoLon l) {

      if (b.getDezimal() == 0 && l.getDezimal() == 0) return 0.;
      if (b.getTertia() != 0) return getElevationGridDouble(b, l);
      if (l.getTertia() != 0) return getElevationGridDouble(b, l);
      if (b.getSekunden() % 3 != 0) return getElevationGridDouble(b, l);
      if (l.getSekunden() % 3 != 0) return getElevationGridDouble(b, l);
      return (double) getElevation(b, l);
   }
   
   public short getSekDiff() {
   	// Anzahl Gradsekunden zwischen zwei Datenpunkten
   	return 3;
   }
   
   public String getName() {
   	return "ElevationSRTM3";
   }

   private class SRTM3I {

      FileShort fileShort;

      private SRTM3I(GeoLat b, GeoLon l) {
         
        String  homeDir = (String)System.getProperties().get("user.home");
        String  srtm3Dir = homeDir + File.separator + "srtm3"; // Lokale Lage der SRTM3-Files!!!
        String  srtm3Suffix = ".hgt";

         String fileName = new String();
         fileName =
               srtm3Dir + File.separator
               + b.getRichtung()
               + NumberForm.n2(b.isNorden() ? b.getGrad() : b.getGrad() + 1)
               + l.getRichtung()
               + NumberForm.n3(l.isOsten() ? l.getGrad() : l.getGrad() + 1)
               + srtm3Suffix;

         try {
            fileShort = new FileShort(fileName, true); // ggf. unzip und/oder FTP
         } catch (Exception e) {
            e.printStackTrace();
         }
       }
      
      public short getElevation(GeoLat b, GeoLon l) {
      	return fileShort.get(offset(b, l));
      }

      //    Offset im SRTM3-File
      public int offset(GeoLat b, GeoLon l) {

      	if (b.isSueden()) {
      		if (l.isOsten())
      			return 1201 * (b.getMinuten() * 20 + b.getSekunden() / 3)
            		+ l.getMinuten() * 20 + l.getSekunden() / 3;
      		else
      			return 1201 * (b.getMinuten() * 20 + b.getSekunden() / 3)
					+ 1199 - l.getMinuten() * 20 - l.getSekunden() / 3;
      	}
      	else {
      		if (l.isOsten())
      			return 1201 * (1199 - b.getMinuten() * 20 - b.getSekunden() / 3)
            		+ l.getMinuten() * 20 + l.getSekunden() / 3;
      		else
      			return 1201 * (1199 - b.getMinuten() * 20 - b.getSekunden() / 3)
					+ 1199 - l.getMinuten() * 20 - l.getSekunden() / 3;
      	}
      }

   }

   public static void main(String[] args) {
   }
}
