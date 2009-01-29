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
