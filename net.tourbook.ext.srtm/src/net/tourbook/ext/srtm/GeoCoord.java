package net.tourbook.ext.srtm;
import java.util.regex.*;

public class GeoCoord {
   
   public static final int faktg = 60*60*60;
   public static final int faktm = 60*60;
   public static final int fakts = 60;
   
   // dummies; siehe GeoLon / GeoLat
   public char richtungPlus() {return '!';}
   public char richtungMinus() {return '?';}
   
   final static private int PATTERN_ANZ = 10;
   final static private String  patternString[] = new String[PATTERN_ANZ];
   final static private Pattern pattern[] = new Pattern[PATTERN_ANZ];
   static private Matcher matcher = null;
   
   public char richtung;
   private int grad;                            // 1 Grad in NS-Richtung = 110.946 km
   private int minuten;                         // 1 Min. in NS-Richtung = 1.852 km
   private int sekunden;                        // 1 Sek. in NS-Richtung = 30.68 m
   private int tertia; // sechzigstel Sekunden  // 1 Trz. in NS-Richtung =  0.51 m
   protected int dezimal; // nur Subklassen (GeoLat, GeoLon) kennen die Variable
   
   // Variable doppel wird ausschliesslich für GPS-Dateifiles verwendet
   // (dort, damit keine Rundungsfehler beim Splitten eines großen HST-Files in
   // viele kleine entstehen)
   // Variable muss mit set(double) gesetzt und mit toStringDouble*() gelesen werden
   // _kein_ Update in updateDezimal und updateGrad,
   // d.h. add etc. fkt. nicht!
   private double doppel = 0.; 
   

   /***********************************************************************************
    
   Zur Erkennung von Strings:
    
   zunächst werden Ersetzungen vorgenommen:
   
      blank       -> (nichts) 
      "           -> (nichts) 
      [SW] vorne  -> - vorne
      [SW] hinten -> - vorne
      [NEO]       -> (nichts) 
      ,           -> .  
      °           -> :  
      '           -> :  
      : hinten    -> (nichts) 

      danach bleiben folgende Fälle übrig:

      Symbolisch           RegEx

      -ggg:mm            ([-+]?)([0-9]{1,3}):([0-9]{1,2})
      -gggMM             ([-+]?)([0-9]{1,3})([0-9]{2})
      -ggg:mm:ss         ([-+]?)([0-9]{1,3}):([0-9]{1,2}):([0-9]{1,2})
      -gggMMSS           ([-+]?)([0-9]{1,3})([0-9]{2})([0-9]{2})
      -ggg:mm:ss:tt      ([-+]?)([0-9]{1,3}):([0-9]{1,2}):([0-9]{1,2}):([0-9]{1,2})
      -ggg.ggggg         ([-+]?)([0-9]{1,3}\\.[0-9]+)
      -ggg:mm.mmmmm      ([-+]?)([0-9]{1,3}):([0-9]{1,2}\\.[0-9]+)
      -gggMM.mmmmm       ([-+]?)([0-9]{1,3})([0-9]{2}\\.[0-9]+)
      -ggg:mm:ss.sssss   ([-+]?)([0-9]{1,3}):([0-9]{1,2}):([0-9]{1,2}\\.[0-9]+)                      
      -gggMMSS.sssss     ([-+]?)([0-9]{1,3})([0-9]{2})([0-9]{2}\\.[0-9]+)

      dabei bedeutet:
         -        ^= plus, minus oder nichts
         ggg      ^= ein bis drei Stellen
         mm,ss,tt ^= ein oder zwei Stellen
         MM,SS    ^= exakt zwei Stellen
         .*       ^= ein oder mehrere Nachkommastellen

      ***********************************************************************************/
   
   
   public GeoCoord() {
      grad = 0;
      minuten = 0;
      sekunden = 0;
      dezimal = 0;
      tertia = 0;
      
      richtung = richtungPlus();
      
      // Kommentar s. o.
      patternString[0] = new String("([-+]?)([0-9]{1,3}):([0-9]{1,2})");
      patternString[1] = new String("([-+]?)([0-9]{1,3})([0-9]{2})");
      patternString[2] = new String("([-+]?)([0-9]{1,3}):([0-9]{1,2}):([0-9]{1,2})");
      patternString[3] = new String("([-+]?)([0-9]{1,3})([0-9]{2})([0-9]{2})");
      patternString[4] = new String("([-+]?)([0-9]{1,3}):([0-9]{1,2}):([0-9]{1,2}):([0-9]{1,2})");
      patternString[5] = new String("([-+]?)([0-9]{1,3}\\.[0-9]+)");
      patternString[6] = new String("([-+]?)([0-9]{1,3}):([0-9]{1,2}\\.[0-9]+)");
      patternString[7] = new String("([-+]?)([0-9]{1,3})([0-9]{2}\\.[0-9]+)");
      patternString[8] = new String("([-+]?)([0-9]{1,3}):([0-9]{1,2}):([0-9]{1,2}\\.[0-9]+)");
      patternString[9] = new String("([-+]?)([0-9]{1,3})([0-9]{2})([0-9]{2}\\.[0-9]+)");

      for (int i = 0; i < PATTERN_ANZ; i++)
         pattern[i] = Pattern.compile(patternString[i]);
      
   }
   
   private String normalize(String s) {
      
      // Kommentar s. o.
      
      //    blank       -> (nichts) 
      //    "           -> (nichts) 
      //    [NEO]       -> (nichts) 
      s = s.replace(" ", "")
           .replace("\"", "")
           .replace("N", "")
           .replace("E", "")
           .replace("O", "");
       
      //  [SW] vorne  -> - vorne
      //  [SW] hinten -> - vorne
      if (s.startsWith("S")) s = "-"+s.substring(1);
      else if (s.startsWith("W")) s = "-"+s.substring(1);
      else if (s.endsWith("S")) s = "-"+s.substring(0, s.length()-1);
      else if (s.endsWith("W")) s = "-"+s.substring(0, s.length()-1);
      
      //  ,           -> .  
      //  °           -> :  
      //  '           -> :  
      s = s.replace(',', '.')
//            .replace('°', ':')
           .replace('\'', ':');
      
      //  : hinten    -> (nichts)
      if (s.endsWith(":")) s = s.substring(0, s.length()-1);
    
      return s;
   }
   
   public void set(String s) {
      
      int pat;
      s = normalize(s);
      
      for (pat = 0; pat < PATTERN_ANZ; pat++) {
         matcher = pattern[pat].matcher(s);
         if (matcher.matches()) 
            break;
      }
      
      if (pat == PATTERN_ANZ) {
         grad = minuten = sekunden = tertia = 0;
         updateDezimal();
         return;
      }
      
      switch (pat) {
      
      case 0: case 1: // -ggg:mm oder -gggMM   
         grad =     new Integer(matcher.group(2)).intValue();
         minuten =  new Integer(matcher.group(3)).intValue();
         sekunden = 0;
         tertia = 0;
         break;
         
      case 2: case 3: // -ggg:mm:ss (z.B. von toString) oder -gggMMSS
         
         grad =     new Integer(matcher.group(2)).intValue();
         minuten =  new Integer(matcher.group(3)).intValue();
         sekunden = new Integer(matcher.group(4)).intValue();
         tertia = 0;
         break;
         
      case 4: // -ggg:mm:ss:tt z.B. von toStringFine (mit Tertia, d. h. um Faktor 60 genauer)      
         
         grad =     new Integer(matcher.group(2)).intValue();
         minuten =  new Integer(matcher.group(3)).intValue();
         sekunden = new Integer(matcher.group(4)).intValue();
         tertia =   new Integer(matcher.group(5)).intValue();
         break;
         
      case 5: // -ggg.ggggg        
         
         double dg = new Double(matcher.group(2)).doubleValue();
         grad = (int)dg;
         double dgg = Math.abs(dg - grad);
         minuten =  (int)(dgg*fakts); 
         sekunden = (int)(dgg*faktm - minuten*fakts); 
         tertia =   (int)(dgg*faktg - minuten*faktm - sekunden*fakts + 0.5);
         break;
         
      case 6: case 7: // -ggg:mm.mmmmm oder -gggMM.mmmmm
         
         grad =      new Integer(matcher.group(2)).intValue();
         double dm = new Double(matcher.group(3)).doubleValue();
         minuten = (int)dm;
         double dmm = Math.abs(dm - minuten);
         sekunden = (int)(dmm*fakts); 
         tertia =   (int)(dmm*faktm - sekunden*fakts + 0.5); 
         break;
         
      case 8: case 9: // -ggg:mm:ss.sssss oder -gggMMSS.sssss
         
         grad =      new Integer(matcher.group(2)).intValue();
         minuten =   new Integer(matcher.group(3)).intValue();
         double ds = new Double(matcher.group(4)).doubleValue();
         sekunden = (int)ds;
         double dss = Math.abs(ds - sekunden);
         tertia =   (int)(dss*fakts + 0.5);
         break;
         
      default:
         break;
      }
            
      if (matcher.group(1).equals("-"))
         richtung = richtungMinus();
      else
         richtung = richtungPlus();
      updateDezimal();
   }


   public int getDezimal() {
      return dezimal;
   }
   
//   public int getHashkey() {
//       // tertia-genau
//      if (dezimal >= 0)
//         return dezimal;
//      return dezimal + 134217728; // = 2^27  > 77760000 = 360 * 60 * 60 * 60
//      }

   public int getHashkeyDist() {
      // Minuten-genau; Wert < 21600; 21600^2 < 2^30
      // absichtlich grob, damit "benachbarte" Punkte in gleiche "Toepfe" fallen
      if (richtung == 'N') return 60*( 89-grad) + minuten;
      if (richtung == 'S') return 60*( 90+grad) + minuten;
      if (richtung == 'W') return 60*(179-grad) + minuten;
      return 60*(180+grad) + minuten;
      
      }

   public int getGrad() {
      return grad;
   }

   public int getMinuten() {
      return minuten;
   }

   public int getSekunden() {
      return sekunden;
   }
   
   public int getTertia() {
      return tertia;
   }
   
   public char getRichtung() {
      return richtung;
   }
   
   public double toRadians() {
      return Math.toRadians((double)dezimal/faktg);
   }
   
   public double toDegrees() {
      return ((double)dezimal/faktg);
   }
   public void setDezimal(int d) {
      dezimal = d;
      updateGrad();
   }

   public void setGrad(int g) {
      grad = g;
      updateDezimal();
   }

   public void setMinuten(int m) {
      minuten = m;
      updateDezimal();
   }

   public void setSekunden(int s) {
      sekunden = s;
      updateDezimal();
   }

   public void setTertia(int t) {
       tertia = t;
       updateDezimal();
    }

   public void setGradMinutenSekundenRichtung(int g, int m, int s, char r) {
      grad = g;
      minuten = m;
      sekunden = s;
      richtung = r;
      updateDezimal();
   }

   public void set(GeoCoord lb) {
      
      dezimal = lb.getDezimal();
      doppel = lb.getDoppel();  

      updateGrad();
   }

   public void setRichtung(char r) {
      richtung = r;
      if (richtung == 'O') richtung = 'E';
      updateDezimal();
   }

   public void add(GeoCoord a) {
      dezimal += a.getDezimal();

      updateGrad();
   }
   
   public void add(GeoCoord lb, GeoCoord a) {
      dezimal = lb.getDezimal();
      this.add(a);
   }
      
   public void add(double d) {
      dezimal += d;

      updateGrad();
   }

   public void addSekunde(int n) {
      this.add(n*fakts);
   }

   public void sub(GeoCoord gc) {
      dezimal -= gc.getDezimal();

      updateGrad();
   }
   
   public void sub(GeoCoord lb, GeoCoord s) {
      dezimal = lb.getDezimal();
      this.sub(s);
   }
   
   public void sub(double d) {
      dezimal -= d;

      updateGrad();
   }
   
   public void subSekunde(int n) {
      this.sub(n*fakts);
   }
   
   public void mult(double faktor) {
      dezimal *= faktor;

      updateGrad();
   }

   public void div(double faktor) {
      dezimal /= faktor;

      updateGrad();
   }

   public void toLeft(GeoCoord r) {
      // auf Rasterrand zur Linken shiften
      int raster = r.getDezimal();
      if (dezimal < 0) dezimal -= raster;
      dezimal /= raster;
      dezimal *= raster;

      updateGrad();
   }
   public void toLeft(GeoCoord lb, GeoCoord r) {
      // lb auf Rasterrand zur Linken shiften
      dezimal = lb.getDezimal();
      this.toLeft(r);
   }
   
   public void toRight(GeoCoord r) {
      // auf Rasterrand zur Rechten shiften
      int raster = r.getDezimal();
      dezimal /= raster;
      dezimal *= raster;
      if (dezimal >= 0) dezimal += raster;
      updateGrad();
   }
   
   public void toRight(GeoCoord lb, GeoCoord r) {
      // lb auf Rasterrand zur Rechten shiften
      dezimal = lb.getDezimal();
      this.toRight(r);
   }
   
   public boolean kleiner(GeoCoord lb) {

      return (dezimal < lb.getDezimal());
   }

   public boolean kleinergleich(GeoCoord lb) {

      return (dezimal <= lb.getDezimal());
   }

   public boolean groesser(GeoCoord lb) {

      return (dezimal > lb.getDezimal());
   }

   public boolean groessergleich(GeoCoord lb) {

      return (dezimal >= lb.getDezimal());
   }

   public boolean gleich(GeoCoord lb) {

      return (dezimal == lb.getDezimal());
   }

   public boolean ungleich(GeoCoord lb) {

      return (dezimal != lb.getDezimal());
   }

   public String toString() {  // = toStringGradMinutenSekundenRichtung()

      return ""
         + NumberForm.n2(grad)
         + ":"
         + NumberForm.n2(minuten)
         + ":"
         + NumberForm.n2(sekunden)
         + " "
         + richtung;
   }

   public String toStringFine() { // = toStringGradMinutenSekundenTertiaRichtung()

      return ""
         + NumberForm.n2(grad)
         + ":"
         + NumberForm.n2(minuten)
         + ":"
         + NumberForm.n2(sekunden)
         + ":"
         + NumberForm.n2(tertia)
         + " "
         + richtung;
   }

   public String toStringDouble() {  // nur für GPS-Datenfiles
      
      return NumberForm.f6(doppel);
   }

   public String toStringGrad() {
      
      double g = dezimal;
      g /= faktg;
      
      return NumberForm.f6(g);
   }

   public String toStringGradRichtung() {
      
      double g = dezimal;
      g /= faktg;
      if (g < 0) g = -g;
      
      return "" 
         + NumberForm.f5(g)
         + " "
         + richtung;
      
   }

   public String toStringGradMinutenRichtung() {
      
      double m = dezimal;
      if (m < 0) m = -m;
      m -= grad * faktg;
      m /= faktm;
      return ""
         + NumberForm.n2(grad)
         + ":"
         + NumberForm.n2f3(m)
         + " "
         + richtung;
   }
   
   public void updateDezimal() {
      dezimal = grad * faktg;
      dezimal += minuten * faktm;
      dezimal += sekunden * fakts;
      dezimal += tertia;

      doppel = dezimal;
      doppel /= faktg;
      
      if (richtung == richtungMinus()) {
         dezimal = -dezimal;
         doppel = -doppel;
      }
   }
   
   
   public void updateGrad() {

      int dez = Math.abs(dezimal);

      grad = dez / faktg;
      dez -= grad * faktg;
      minuten = dez / faktm;
      dez -= minuten * faktm;
      sekunden = dez / fakts;
      dez -= sekunden * fakts;
      tertia = dez;
      richtung = dezimal < 0 ? richtungMinus() : richtungPlus();
      
      doppel = dezimal; 
      doppel /= faktg;
   }
   
   public void set(double d) {
      doppel = d;
      dezimal = (int)(d * faktg);
      updateGrad();
   }
   public double getDoppel() {
      return doppel;
   }
   public void setDoppel(double doppel) {
      this.doppel = doppel;
   }

   public double sin() {
      return Math.sin(this.toRadians());
   }
   
   public double cos() {
      return Math.cos(this.toRadians());
   }
   
   public double tan() {
      return Math.tan(this.toRadians());
   }
   
   public double asin() {
      return Math.asin(this.toRadians());
   }
   
   public double acos() {
      return Math.acos(this.toRadians());
   }
   
   public double atan() {
      return Math.atan(this.toRadians());
   }

   public void setMitte(GeoCoord k1, GeoCoord k2) {
      // auf Mittelwert von k1 und k2 setzen (1-dimensional!) 
      set(k1);
      div(2.);
      GeoCoord kHelp = new GeoCoord();
      kHelp.set(k2);
      kHelp.div(2.);
      add(kHelp);
   }
   
}
