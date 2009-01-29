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
