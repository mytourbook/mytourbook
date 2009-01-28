package net.tourbook.ext.srtm;
import java.util.regex.*;

public class GeoCoord {
   
   public static final int faktg = 60*60*60;
   public static final int faktm = 60*60;
   public static final int fakts = 60;
   
   // dummies; siehe GeoLon / GeoLat
   public char directionPlus() {return '!';}
   public char directionMinus() {return '?';}
   
   final static private int PATTERN_ANZ = 10;
   final static private String  patternString[] = new String[PATTERN_ANZ];
   final static private Pattern pattern[] = new Pattern[PATTERN_ANZ];
   static private Matcher matcher = null;
   
   public char direction;
   private int grade;                            // 1 Grade in NS-Direction = 110.946 km
   private int minutes;                         // 1 Min. in NS-Direction = 1.852 km
   private int seconds;                        // 1 Sek. in NS-Direction = 30.68 m
   private int tertia; // sechzigstel Seconds  // 1 Trz. in NS-Direction =  0.51 m
   protected int decimal; // nur Subklassen (GeoLat, GeoLon) kennen die Variable
   
   // Variable doubleValue wird ausschliesslich für GPS-Dateifiles verwendet
   // (dort, damit keine Rundungsfehler beim Splitten eines großen HST-Files in
   // viele kleine entstehen)
   // Variable muss mit set(double) gesetzt und mit toStringDouble*() gelesen werden
   // _kein_ Update in updateDecimal und updateGrade,
   // d.h. add etc. fkt. nicht!
   private double doubleValue = 0.; 
   

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
      grade = 0;
      minutes = 0;
      seconds = 0;
      decimal = 0;
      tertia = 0;
      
      direction = directionPlus();
      
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
           .replace('\u00B0', ':') // degree sign
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
         grade = minutes = seconds = tertia = 0;
         updateDecimal();
         return;
      }
      
      switch (pat) {
      
      case 0: case 1: // -ggg:mm oder -gggMM   
         grade =     new Integer(matcher.group(2)).intValue();
         minutes =  new Integer(matcher.group(3)).intValue();
         seconds = 0;
         tertia = 0;
         break;
         
      case 2: case 3: // -ggg:mm:ss (z.B. von toString) oder -gggMMSS
         
         grade =     new Integer(matcher.group(2)).intValue();
         minutes =  new Integer(matcher.group(3)).intValue();
         seconds = new Integer(matcher.group(4)).intValue();
         tertia = 0;
         break;
         
      case 4: // -ggg:mm:ss:tt z.B. von toStringFine (mit Tertia, d. h. um Faktor 60 genauer)      
         
         grade =     new Integer(matcher.group(2)).intValue();
         minutes =  new Integer(matcher.group(3)).intValue();
         seconds = new Integer(matcher.group(4)).intValue();
         tertia =   new Integer(matcher.group(5)).intValue();
         break;
         
      case 5: // -ggg.ggggg        
         
         double dg = new Double(matcher.group(2)).doubleValue();
         grade = (int)dg;
         double dgg = Math.abs(dg - grade);
         minutes =  (int)(dgg*fakts); 
         seconds = (int)(dgg*faktm - minutes*fakts); 
         tertia =   (int)(dgg*faktg - minutes*faktm - seconds*fakts + 0.5);
         break;
         
      case 6: case 7: // -ggg:mm.mmmmm oder -gggMM.mmmmm
         
         grade =      new Integer(matcher.group(2)).intValue();
         double dm = new Double(matcher.group(3)).doubleValue();
         minutes = (int)dm;
         double dmm = Math.abs(dm - minutes);
         seconds = (int)(dmm*fakts); 
         tertia =   (int)(dmm*faktm - seconds*fakts + 0.5); 
         break;
         
      case 8: case 9: // -ggg:mm:ss.sssss oder -gggMMSS.sssss
         
         grade =      new Integer(matcher.group(2)).intValue();
         minutes =   new Integer(matcher.group(3)).intValue();
         double ds = new Double(matcher.group(4)).doubleValue();
         seconds = (int)ds;
         double dss = Math.abs(ds - seconds);
         tertia =   (int)(dss*fakts + 0.5);
         break;
         
      default:
         break;
      }
            
      if (matcher.group(1).equals("-"))
         direction = directionMinus();
      else
         direction = directionPlus();
      updateDecimal();
   }


   public int getDecimal() {
      return decimal;
   }
   
//   public int getHashkey() {
//       // tertia-genau
//      if (decimal >= 0)
//         return decimal;
//      return decimal + 134217728; // = 2^27  > 77760000 = 360 * 60 * 60 * 60
//      }

   public int getHashkeyDist() {
      // Minutes-genau; Wert < 21600; 21600^2 < 2^30
      // absichtlich grob, damit "benachbarte" Punkte in gleiche "Toepfe" fallen
      if (direction == 'N') return 60*( 89-grade) + minutes;
      if (direction == 'S') return 60*( 90+grade) + minutes;
      if (direction == 'W') return 60*(179-grade) + minutes;
      return 60*(180+grade) + minutes;
      
      }

   public int getGrade() {
      return grade;
   }

   public int getMinutes() {
      return minutes;
   }

   public int getSeconds() {
      return seconds;
   }
   
   public int getTertia() {
      return tertia;
   }
   
   public char getDirection() {
      return direction;
   }
   
   public double toRadians() {
      return Math.toRadians((double)decimal/faktg);
   }
   
   public double toDegrees() {
      return ((double)decimal/faktg);
   }
   public void setDecimal(int d) {
      decimal = d;
      updateGrade();
   }

   public void setGrade(int g) {
      grade = g;
      updateDecimal();
   }

   public void setMinutes(int m) {
      minutes = m;
      updateDecimal();
   }

   public void setSeconds(int s) {
      seconds = s;
      updateDecimal();
   }

   public void setTertia(int t) {
       tertia = t;
       updateDecimal();
    }

   public void setGradeMinutesSecondsDirection(int g, int m, int s, char r) {
      grade = g;
      minutes = m;
      seconds = s;
      direction = r;
      updateDecimal();
   }

   public void set(GeoCoord lb) {
      
      decimal = lb.getDecimal();
      doubleValue = lb.getDoubleValue();  

      updateGrade();
   }

   public void setDirection(char r) {
      direction = r;
      if (direction == 'O') direction = 'E';
      updateDecimal();
   }

   public void add(GeoCoord a) {
      decimal += a.getDecimal();

      updateGrade();
   }
   
   public void add(GeoCoord lb, GeoCoord a) {
      decimal = lb.getDecimal();
      this.add(a);
   }
      
   public void add(double d) {
      decimal += d;

      updateGrade();
   }

   public void addSecond(int n) {
      this.add(n*fakts);
   }

   public void sub(GeoCoord gc) {
      decimal -= gc.getDecimal();

      updateGrade();
   }
   
   public void sub(GeoCoord lb, GeoCoord s) {
      decimal = lb.getDecimal();
      this.sub(s);
   }
   
   public void sub(double d) {
      decimal -= d;

      updateGrade();
   }
   
   public void subSecond(int n) {
      this.sub(n*fakts);
   }
   
   public void mult(double faktor) {
      decimal *= faktor;

      updateGrade();
   }

   public void div(double faktor) {
      decimal /= faktor;

      updateGrade();
   }

   public void toLeft(GeoCoord r) {
      // auf Rasterrand zur Linken shiften
      int raster = r.getDecimal();
      if (decimal < 0) decimal -= raster;
      decimal /= raster;
      decimal *= raster;

      updateGrade();
   }
   public void toLeft(GeoCoord lb, GeoCoord r) {
      // lb auf Rasterrand zur Linken shiften
      decimal = lb.getDecimal();
      this.toLeft(r);
   }
   
   public void toRight(GeoCoord r) {
      // auf Rasterrand zur Rechten shiften
      int raster = r.getDecimal();
      decimal /= raster;
      decimal *= raster;
      if (decimal >= 0) decimal += raster;
      updateGrade();
   }
   
   public void toRight(GeoCoord lb, GeoCoord r) {
      // lb auf Rasterrand zur Rechten shiften
      decimal = lb.getDecimal();
      this.toRight(r);
   }
   
   public boolean lessThen(GeoCoord lb) {

      return (decimal < lb.getDecimal());
   }

   public boolean lessOrEqual(GeoCoord lb) {

      return (decimal <= lb.getDecimal());
   }

   public boolean greaterThen(GeoCoord lb) {

      return (decimal > lb.getDecimal());
   }

   public boolean greaterOrEqual(GeoCoord lb) {

      return (decimal >= lb.getDecimal());
   }

   public boolean equalTo(GeoCoord lb) {

      return (decimal == lb.getDecimal());
   }

   public boolean notEqualTo(GeoCoord lb) {

      return (decimal != lb.getDecimal());
   }

   public String toString() {  // = toStringGradeMinutesSecondsDirection()

      return ""
         + NumberForm.n2(grade)
         + ":"
         + NumberForm.n2(minutes)
         + ":"
         + NumberForm.n2(seconds)
         + " "
         + direction;
   }

   public String toStringFine() { // = toStringGradeMinutesSecondsTertiaDirection()

      return ""
         + NumberForm.n2(grade)
         + ":"
         + NumberForm.n2(minutes)
         + ":"
         + NumberForm.n2(seconds)
         + ":"
         + NumberForm.n2(tertia)
         + " "
         + direction;
   }

   public String toStringDouble() {  // nur für GPS-Datenfiles
      
      return NumberForm.f6(doubleValue);
   }

   public String toStringGrade() {
      
      double g = decimal;
      g /= faktg;
      
      return NumberForm.f6(g);
   }

   public String toStringGradeDirection() {
      
      double g = decimal;
      g /= faktg;
      if (g < 0) g = -g;
      
      return "" 
         + NumberForm.f5(g)
         + " "
         + direction;
      
   }

   public String toStringGradeMinutesDirection() {
      
      double m = decimal;
      if (m < 0) m = -m;
      m -= grade * faktg;
      m /= faktm;
      return ""
         + NumberForm.n2(grade)
         + ":"
         + NumberForm.n2f3(m)
         + " "
         + direction;
   }
   
   public void updateDecimal() {
      decimal = grade * faktg;
      decimal += minutes * faktm;
      decimal += seconds * fakts;
      decimal += tertia;

      doubleValue = decimal;
      doubleValue /= faktg;
      
      if (direction == directionMinus()) {
         decimal = -decimal;
         doubleValue = -doubleValue;
      }
   }
   
   
   public void updateGrade() {

      int dec = Math.abs(decimal);

      grade = dec / faktg;
      dec -= grade * faktg;
      minutes = dec / faktm;
      dec -= minutes * faktm;
      seconds = dec / fakts;
      dec -= seconds * fakts;
      tertia = dec;
      direction = decimal < 0 ? directionMinus() : directionPlus();
      
      doubleValue = decimal; 
      doubleValue /= faktg;
   }
   
   public void set(double d) {
      doubleValue = d;
      decimal = (int)(d * faktg);
      updateGrade();
   }
   public double getDoubleValue() {
      return doubleValue;
   }
   public void setDoubleValue(double doppel) {
      this.doubleValue = doppel;
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
