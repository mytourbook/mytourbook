package net.tourbook.ext.srtm;

import java.io.File;

public final class ElevationEtopo extends ElevationBase {

   static private EtopoI fEtopoi = null;

   public ElevationEtopo()  {
      
      bGrid.setGradMinutenSekundenRichtung(0, 5, 0, 'N');
      lGrid.setGradMinutenSekundenRichtung(0, 5, 0, 'E');
      
   }

   public short getElevation(GeoLat lat, GeoLon lon) {
      
      if (lat.getTertia() != 0) return getElevationGrid(lat, lon);
      if (lon.getTertia() != 0) return getElevationGrid(lat, lon);
      if (lat.getSekunden() != 0) return getElevationGrid(lat, lon);
      if (lon.getSekunden() != 0) return getElevationGrid(lat, lon);
      if (lat.getMinuten() % 5 != 0) return getElevationGrid(lat, lon);
      if (lon.getMinuten() % 5 != 0) return getElevationGrid(lat, lon);
      
      
      if (fEtopoi == null) 
         fEtopoi = new EtopoI(); // nur beim ersten Mal !!
        

      return fEtopoi.getElevation(lat, lon);
      
   }
   
   public double getElevationDouble(GeoLat lat, GeoLon lon) {

      if (lat.getDezimal() == 0 && lon.getDezimal() == 0) return 0.;
      if (lat.getTertia() != 0) return getElevationGridDouble(lat, lon);
      if (lon.getTertia() != 0) return getElevationGridDouble(lat, lon);
      if (lat.getSekunden() != 0) return getElevationGridDouble(lat, lon);
      if (lon.getSekunden() != 0) return getElevationGridDouble(lat, lon);
      if (lat.getMinuten() % 5 != 0) return getElevationGridDouble(lat, lon);
      if (lon.getMinuten() % 5 != 0) return getElevationGridDouble(lat, lon);
      return (double) getElevation(lat, lon);
   }

   public short getSekDiff() {
   	// Anzahl Gradsekunden zwischen zwei Datenpunkten
   	return 300;
   }
   
   public String getName() {
   	return "ETOPO";
   }

   private final class EtopoI {
      
      private GeoLat minLat = new GeoLat();
      private GeoLon minLon = new GeoLon();
      ElevationFile elevationFile;
      GeoLat bo = new GeoLat();
      GeoLon lo = new GeoLon();
      
      
      private EtopoI() {
         
         final String elevationDataPath = getElevationDataPath(); 
         final String etopoDir = elevationDataPath + File.separator + "etopo"; // Lokale Lage der ETOPO-Files!!!
         final String etopoFilename = "ETOPO5.DAT";

         String fileName = new String();
         fileName =etopoDir
         + File.separator
         + etopoFilename;
         
         
         try {
            elevationFile = new ElevationFile(fileName, Constants.ELEVATION_TYPE_ETOPO);
         } catch (Exception e) {
            System.out.println("EtopoI: Fehler: " + e.getMessage()); // NICHT File not found
            // Exception nicht weitergeben
         }         
         
         minLon.setGradMinutenSekundenRichtung(360, 0, 0, 'W');
         minLat.setGradMinutenSekundenRichtung(89, 55, 0, 'N');
      }

      public short getElevation(GeoLat lat, GeoLon lon) {
         
          return elevationFile.get(offset(lat, lon));
      } 
      
      //    Offset im Etopo-File
      public int offset(GeoLat lat, GeoLon lon) {
         
         bo.sub(minLat, lat);
         lo.sub(lon, minLon);
         return bo.getGrad() * 51840   // 360*12*12       
              + bo.getMinuten() * 864  // 360*12/5      
              + lo.getGrad() * 12 
              + lo.getMinuten() / 5;
      }           
   }

   public static void main(String[] args) {
   }
}
