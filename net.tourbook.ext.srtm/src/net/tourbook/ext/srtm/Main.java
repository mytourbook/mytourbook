package net.tourbook.ext.srtm;

public class Main {

   public static void main(final String[] args) {
      
      final NumberForm numberForm = new NumberForm();
      
      final GeoLat latVaals = new GeoLat("50:46:28N");
      final GeoLon lonVaals = new GeoLon("06:00:35E");
      
      final ElevationSRTM3 elevationSRTM3 = new ElevationSRTM3();
      
      final short elevVaals = elevationSRTM3.getElevation(latVaals, lonVaals);
         
      System.out.println("Hoehe Vaals = " + elevVaals);
      

   }

}
