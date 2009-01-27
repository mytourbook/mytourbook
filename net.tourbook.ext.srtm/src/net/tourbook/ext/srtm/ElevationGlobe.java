package net.tourbook.ext.srtm;

import java.io.File;

// Minimal-/Maximalwerte
// 
//           | W179:59:30 | W089:59:30 | E000:00:00 | E090:00:00 |
//           | W090:00:00 | W000:00:00 | E089:59:30 | E179:59:30 |
// ----------+------------+------------+------------+------------+
// N89:59:30 | a          | lat          | c          | d          |
// N50:00:00 |            |            |            |            |
// ----------+------------+------------+------------+------------+
// N49:59:30 | e          | f          | g          | h          |
// N00:00:00 |            |            |            |            |
// ----------+------------+------------+------------+------------+
// S00:00:00 | i          | j          | k          | lon          |
// S49:59:30 |            |            |            |            |
// ----------+------------+------------+------------+------------+
// S50:00:00 | m          | n          | o          | p          |
// S89:59:30 |            |            |            |            |
// ----------+------------+------------+------------+------------+

public final class ElevationGlobe extends ElevationBase { 

   final static private GlobeI fGlobei[] = new GlobeI[16];
   final static private boolean initialized[] = new boolean[16]; ///[fGlobei.length];
   
   ;
   public ElevationGlobe() {
      // FileLog.println(this, "ElevationGlobe Konstructor!");
      
      
      for (int i = 0; i < 16; i++) {initialized[i] = false;} 
      bGrid.setGradMinutenSekundenRichtung(0, 0, 30, 'N');
      lGrid.setGradMinutenSekundenRichtung(0, 0, 30, 'E');
   }

   public short getElevation(GeoLat lat, GeoLon lon) {
      int i = 0;
      
      if (lat.getTertia() != 0) return getElevationGrid(lat, lon);
      if (lon.getTertia() != 0) return getElevationGrid(lat, lon);
      if (lat.getSekunden() % 30 != 0) return getElevationGrid(lat, lon);
      if (lon.getSekunden() % 30 != 0) return getElevationGrid(lat, lon);
      
      // globe-Fileindex bestimmen (a-p ~ 0-15)
      if (lat.isSueden()) {
         i += 8;
         if (lat.getGrad() >= 50)
            i += 4;
      } else if (lat.getGrad() < 50)
         i += 4;
      if (lon.isOsten()) {
         i += 2;
         if (lon.getGrad() >= 90)
            i++;
      } else if (lon.getGrad() < 90)
         i++;

      if (initialized[i] == false) {
         initialized[i] = true;
         // FileLog.println(this, "Index ElevationGlobe " + i);
         fGlobei[i] = new GlobeI(i); // nur beim jeweils ersten Mal !!
      }

      return fGlobei[i].getElevation(lat, lon);
   }

   public double getElevationDouble(GeoLat lat, GeoLon lon) {

      if (lat.getDezimal() == 0 && lon.getDezimal() == 0) return 0.;
      if (lat.getTertia() != 0) return getElevationGridDouble(lat, lon);
      if (lon.getTertia() != 0) return getElevationGridDouble(lat, lon);
      if (lat.getSekunden() % 30 != 0) return getElevationGridDouble(lat, lon);
      if (lon.getSekunden() % 30 != 0) return getElevationGridDouble(lat, lon);
      return (double) getElevation(lat, lon);
   }

   public short getSekDiff() {
   	// Anzahl Gradsekunden zwischen zwei Datenpunkten
   	return 30;
   }
   
   public String getName() {
   	return "GLOBE";
   }

   private class GlobeI {
      private GeoLat minLat = new GeoLat();
      private GeoLon minLon = new GeoLon();
      ElevationFile elevationFile;
      GeoLat bo = new GeoLat();
      GeoLon lo = new GeoLon();
      

      private GlobeI(int i) {

         final String elevationDataPath = getElevationDataPath();
         final String globeDir = elevationDataPath + File.separator + "globe"; // Lokale Lage der GLOBE-Files!!!
         final String globeSuffix = "10g";

         char c = (char) ('a' + i);
         String fileName = new String();
         fileName =globeDir
         + File.separator
         + c 
         + globeSuffix;
         
         try {
            elevationFile = new ElevationFile(fileName, Constants.ELEVATION_TYPE_GLOBE);
         } catch (Exception e) {
            System.out.println("GlobeI: Fehler: " + e.getMessage()); // NICHT File not found
            // Exception nicht weitergeben
         }
         
         switch (i) {
            case 0 :
            case 4 :
            case 8 :
            case 12 :
               minLon.setGradMinutenSekundenRichtung(179, 59, 30, 'W');
               break;
            case 1 :
            case 5 :
            case 9 :
            case 13 :
               minLon.setGradMinutenSekundenRichtung(89, 59, 30, 'W');
               break;
            case 2 :
            case 6 :
            case 10 :
            case 14 :
               minLon.setGradMinutenSekundenRichtung(0, 0, 0, 'E');
               break;
            case 3 :
            case 7 :
            case 11 :
            case 15 :
               minLon.setGradMinutenSekundenRichtung(90, 0, 0, 'E');
               break;
            default :
               break;
         }
         switch (i) {
            case 0 :
            case 1 :
            case 2 :
            case 3 :
               minLat.setGradMinutenSekundenRichtung(89, 59, 30, 'N');
               break;
            case 4 :
            case 5 :
            case 6 :
            case 7 :
               minLat.setGradMinutenSekundenRichtung(49, 59, 30, 'N');
               break;
            case 8 :
            case 9 :
            case 10 :
            case 11 :
               minLat.setGradMinutenSekundenRichtung(0, 0, 0, 'S');
               break;
            case 12 :
            case 13 :
            case 14 :
            case 15 :
               minLat.setGradMinutenSekundenRichtung(50, 0, 0, 'S');
               break;
            default :
               break;
         }
      }
      
      /**
       * Byte swap a single short value.
       * 
       * @param value  Value to byte swap.
       * @return       Byte swapped representation.
       */
      private short swap(short value)
      {
        int lat1 = value & 0xff;
        int lat2 = (value >> 8) & 0xff;

        return (short) (lat1 << 8 | lat2 << 0);
      }

      public short getElevation(GeoLat lat, GeoLon lon) {
         
         short elev = elevationFile.get(offset(lat, lon));
         return swap(elev);
         // return elev;
      }

      //    Offset im Globe-File
      public int offset(GeoLat lat, GeoLon lon) {

         bo.sub(minLat, lat);
         lo.sub(minLon, lon);
         return bo.getGrad() * 1296000  // 360*60*60
            + bo.getMinuten() * 21600   // 360*60
            + bo.getSekunden() * 360
            + lo.getGrad() * 120
            + lo.getMinuten() * 2
            + lo.getSekunden() / 30;
      }

   }

   public static void main(String[] args) {
   }
}
