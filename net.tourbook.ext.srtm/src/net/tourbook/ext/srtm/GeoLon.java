package net.tourbook.ext.srtm;

public class GeoLon extends GeoCoord {

   public char richtungPlus() {return 'E';}
   public char richtungMinus() {return 'W';}

   public GeoLon() {
      super();
   }

   public GeoLon(GeoLon l) {
      super();
      set(l);
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

   public void add(GeoLon l) {

      dezimal += l.getDezimal();
      if (dezimal > 180*faktg) dezimal -= 360*faktg;

      updateGrad();
   }

   public void add(GeoLon l, GeoLon a) {
      dezimal = l.getDezimal();
      this.add(a);
   }
   
   public void sub(GeoLon l) {

      dezimal -= l.getDezimal();
      if (dezimal < -180*faktg) dezimal += 360*faktg;

      updateGrad();
   }

   public void sub(GeoLon l, GeoLon s) {
      dezimal = l.getDezimal();
      this.sub(s);
   }
      
   public void set(GeoLon l) {
      super.set((GeoCoord)l);
   }

   public static void main(String[] args) {
   }
}
