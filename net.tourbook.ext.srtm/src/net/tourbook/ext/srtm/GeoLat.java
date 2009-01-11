package net.tourbook.ext.srtm;

public class GeoLat extends GeoCoord {

   public char richtungPlus() {return 'N';}
   public char richtungMinus() {return 'S';}

   public GeoLat() {
      super();
   }

   public GeoLat(GeoLat b) {
      super();
      set(b);
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

   public void add(GeoLat b) {

      dezimal += b.getDezimal();
       if (dezimal > 90 *faktg) dezimal = 180*faktg - dezimal;

      updateGrad();
   }

   public void add(GeoLat b, GeoLat a) {
      dezimal = b.getDezimal();
      this.add(a);
   }
   
   public void sub(GeoLat b) {

      dezimal -= b.getDezimal();
      if (dezimal < -90 *faktg) dezimal = -180*faktg - dezimal;

      updateGrad();
   }

   public void sub(GeoLat b, GeoLat s) {
      dezimal = b.getDezimal();
      this.sub(s);
   }

   public void set(GeoLat b) {
      super.set((GeoCoord)b);
   }

   public static void main(String[] args) {

   }
   

}
