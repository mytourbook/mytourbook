package net.tourbook.ext.srtm;



public class Elevation {

   final public GeoLat bGrid;
   final public GeoLon lGrid;
   final public GeoLat bFirst;
   final public GeoLat bLast;
   final public GeoLon lFirst;
   final public GeoLon lLast;    

   public Elevation() {
      bGrid = new GeoLat();
      lGrid = new GeoLon();
      bFirst = new GeoLat();
      bLast = new GeoLat();
      lFirst = new GeoLon();
      lLast = new GeoLon();
   }

   // dummy
   public short getElevation(GeoLat b, GeoLon l)  {
       return 0;
   }
   
   // dummy
   public double getElevationDouble(GeoLat b, GeoLon l) {
      return 0.;
   }

   // dummy
   public short getSekDiff() {
   	// Anzahl Gradsekunden zwischen zwei Datenpunkten
   	return 42;
   }
   
   // dummy
   public String getName() {
   	return "FILETYP-DUMMY";
   }
   
   public double getElevationGridDouble(GeoLat b, GeoLon l)  {

      short h1, h2, h3, h4;
      double p, q;
      short ok = 0;
      double hm;
      
      bFirst.toLeft(b, bGrid);
      bLast.toRight(b, bGrid);
      lFirst.toLeft(l, lGrid);
      lLast.toRight(l, lGrid);

      h1 = this.getElevation(bLast, lFirst);
      h2 = this.getElevation(bLast, lLast);
      h3 = this.getElevation(bFirst, lFirst);
      h4 = this.getElevation(bFirst, lLast);

      // ungueltige Werte ausgleichen
      boolean is_valid_h1 = is_valid(h1);
      boolean is_valid_h2 = is_valid(h2);
      boolean is_valid_h3 = is_valid(h3);
      boolean is_valid_h4 = is_valid(h4);

      ok = 0;
      if (is_valid_h1) ok++;
      if (is_valid_h2) ok++;
      if (is_valid_h3) ok++;
      if (is_valid_h4) ok++;
      if (ok != 4){

         //FileLog.println(this, "Elevation: " + ok + " " + h1 + " " + h2 + " " + h3 + " " + h4 + " " + ec++);

         if (ok == 0){
            hm = (h1 + h2 + h3 + h4)/4;
            return hm; 
         }
         hm = 0;
         if (is_valid_h1) hm += h1;
         if (is_valid_h2) hm += h2;
         if (is_valid_h3) hm += h3;
         if (is_valid_h4) hm += h4;
         hm /= ok;
         if (!is_valid_h1) h1 = (short)hm;
         if (!is_valid_h2) h2 = (short)hm;
         if (!is_valid_h3) h3 = (short)hm;
         if (!is_valid_h4) h4 = (short)hm;
      }      

      p = b.getDezimal() - bFirst.getDezimal();
      p /= bLast.getDezimal() - bFirst.getDezimal();
      q = l.getDezimal() - lFirst.getDezimal();
      q /= lLast.getDezimal() - lFirst.getDezimal();

      return ((1 - q) * p * h1 + q * p * h2 + (1 - q) * (1 - p) * h3 + q * (1 - p) * h4 + 0.5);
   }
   
   public short getElevationGrid(GeoLat b, GeoLon l) {
      return (short) getElevationGridDouble(b, l);
   }

   public boolean is_valid(short elev){
      if (elev >= -11000 && elev < 8850)
         return true;
      return false;
   }

   public boolean is_valid(double elev){
      return is_valid((short)elev);
   }
   
   public static void main(String[] args) {     
   }
}
