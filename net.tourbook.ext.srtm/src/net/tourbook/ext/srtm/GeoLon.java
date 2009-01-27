package net.tourbook.ext.srtm;

public class GeoLon extends GeoCoord {

   public char richtungPlus() {return 'E';}
   public char richtungMinus() {return 'W';}

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
   
   public boolean isOsten() {
      return (richtung == richtungPlus());
   }

   public boolean isWesten() {
      return (!this.isOsten());
   }

   public void add(GeoLon lon) {

      dezimal += lon.getDezimal();
      if (dezimal > 180*faktg) dezimal -= 360*faktg;

      updateGrad();
   }

   public void add(GeoLon lon, GeoLon a) {
      dezimal = lon.getDezimal();
      this.add(a);
   }
   
   public void sub(GeoLon lon) {

      dezimal -= lon.getDezimal();
      if (dezimal < -180*faktg) dezimal += 360*faktg;

      updateGrad();
   }

   public void sub(GeoLon lon, GeoLon s) {
      dezimal = lon.getDezimal();
      this.sub(s);
   }
      
   public void set(GeoLon lon) {
      super.set((GeoCoord)lon);
   }

   public static void main(String[] args) {
   }
}
