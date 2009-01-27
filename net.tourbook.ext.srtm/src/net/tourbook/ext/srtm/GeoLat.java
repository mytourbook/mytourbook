package net.tourbook.ext.srtm;

public class GeoLat extends GeoCoord {

   public char richtungPlus() {return 'N';}
   public char richtungMinus() {return 'S';}

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

   public boolean isNorden() {
      return (richtung == richtungPlus());
   }

   public boolean isSueden() {
      return (!this.isNorden());
   }

   public void add(GeoLat lat) {

      dezimal += lat.getDezimal();
       if (dezimal > 90 *faktg) dezimal = 180*faktg - dezimal;

      updateGrad();
   }

   public void add(GeoLat lat, GeoLat a) {
      dezimal = lat.getDezimal();
      this.add(a);
   }
   
   public void sub(GeoLat lat) {

      dezimal -= lat.getDezimal();
      if (dezimal < -90 *faktg) dezimal = -180*faktg - dezimal;

      updateGrad();
   }

   public void sub(GeoLat lat, GeoLat s) {
      dezimal = lat.getDezimal();
      this.sub(s);
   }

   public void set(GeoLat lat) {
      super.set((GeoCoord)lat);
   }

   public static void main(String[] args) {

   }
   

}
