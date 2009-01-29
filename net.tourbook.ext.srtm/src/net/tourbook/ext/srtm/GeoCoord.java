package net.tourbook.ext.srtm;

import java.util.regex.*;

public class GeoCoord {

	public static final int	faktg	= 60 * 60 * 60;
	public static final int	faktm	= 60 * 60;
	public static final int	fakts	= 60;

	// dummies; siehe GeoLon / GeoLat
	public char directionPlus() {
		return '!';
	}

	public char directionMinus() {
		return '?';
	}

	final static private int		PATTERN_ANZ		= 10;
	final static private String		patternString[]	= new String[PATTERN_ANZ];
	final static private Pattern	pattern[]		= new Pattern[PATTERN_ANZ];
	static private Matcher			matcher			= null;

	public char						direction;
	private int						degrees;									// 1 Degrees in NS-Direction = 110.946 km
	private int						minutes;									// 1 Min. in NS-Direction = 1.852 km
	private int						seconds;									// 1 Sek. in NS-Direction = 30.68 m
	private int						tertias;									// sechzigstel Seconds  // 1 Trz. in NS-Direction =  0.51 m
	protected int					decimal;									// nur Subklassen (GeoLat, GeoLon) kennen die Variable

	// Variable doubleValue wird ausschliesslich für GPS-Dateifiles verwendet
	// (dort, damit keine Rundungsfehler beim Splitten eines großen HST-Files in
	// viele kleine entstehen)
	// Variable muss mit set(double) gesetzt und mit toStringDouble*() gelesen werden
	// _kein_ Update in updateDecimal und updateDegrees,
	// d.h. add etc. fkt. nicht!
	private double					doubleValue		= 0.;

	/***********************************************************************************
	 * Zur Erkennung von Strings: zunächst werden Ersetzungen vorgenommen: blank -> (nichts) " ->
	 * (nichts) [SW] vorne -> - vorne [SW] hinten -> - vorne [NEO] -> (nichts) , -> . ° -> : ' -> :
	 * : hinten -> (nichts) danach bleiben folgende Fälle übrig: Symbolisch RegEx -ggg:mm
	 * ([-+]?)([0-9]{1,3}):([0-9]{1,2}) -gggMM ([-+]?)([0-9]{1,3})([0-9]{2}) -ggg:mm:ss
	 * ([-+]?)([0-9]{1,3}):([0-9]{1,2}):([0-9]{1,2}) -gggMMSS
	 * ([-+]?)([0-9]{1,3})([0-9]{2})([0-9]{2}) -ggg:mm:ss:tt
	 * ([-+]?)([0-9]{1,3}):([0-9]{1,2}):([0-9]{1,2}):([0-9]{1,2}) -ggg.ggggg
	 * ([-+]?)([0-9]{1,3}\\.[0-9]+) -ggg:mm.mmmmm ([-+]?)([0-9]{1,3}):([0-9]{1,2}\\.[0-9]+)
	 * -gggMM.mmmmm ([-+]?)([0-9]{1,3})([0-9]{2}\\.[0-9]+) -ggg:mm:ss.sssss
	 * ([-+]?)([0-9]{1,3}):([0-9]{1,2}):([0-9]{1,2}\\.[0-9]+) -gggMMSS.sssss
	 * ([-+]?)([0-9]{1,3})([0-9]{2})([0-9]{2}\\.[0-9]+) dabei bedeutet: - ^= plus, minus oder nichts
	 * ggg ^= ein bis drei Stellen mm,ss,tt ^= ein oder zwei Stellen MM,SS ^= exakt zwei Stellen .*
	 * ^= ein oder mehrere Nachkommastellen
	 ***********************************************************************************/

	public GeoCoord() {
		degrees = 0;
		minutes = 0;
		seconds = 0;
		decimal = 0;
		tertias = 0;

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
		s = s.replace(" ", "").replace("\"", "").replace("N", "").replace("E", "").replace("O", "");

		//  [SW] vorne  -> - vorne
		//  [SW] hinten -> - vorne
		if (s.startsWith("S"))
			s = "-" + s.substring(1);
		else if (s.startsWith("W"))
			s = "-" + s.substring(1);
		else if (s.endsWith("S"))
			s = "-" + s.substring(0, s.length() - 1);
		else if (s.endsWith("W"))
			s = "-" + s.substring(0, s.length() - 1);

		//  ,           -> .  
		//  °           -> :  
		//  '           -> :  
		s = s.replace(',', '.').replace('\u00B0', ':') // degree sign
				.replace('\'', ':');

		//  : hinten    -> (nichts)
		if (s.endsWith(":"))
			s = s.substring(0, s.length() - 1);

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
			degrees = minutes = seconds = tertias = 0;
			updateDecimal();
			return;
		}

		switch (pat) {

		case 0:
		case 1: // -ggg:mm oder -gggMM   
			degrees = new Integer(matcher.group(2)).intValue();
			minutes = new Integer(matcher.group(3)).intValue();
			seconds = 0;
			tertias = 0;
			break;

		case 2:
		case 3: // -ggg:mm:ss (z.B. von toString) oder -gggMMSS

			degrees = new Integer(matcher.group(2)).intValue();
			minutes = new Integer(matcher.group(3)).intValue();
			seconds = new Integer(matcher.group(4)).intValue();
			tertias = 0;
			break;

		case 4: // -ggg:mm:ss:tt z.B. von toStringFine (mit Tertias, d. h. um Faktor 60 genauer)      

			degrees = new Integer(matcher.group(2)).intValue();
			minutes = new Integer(matcher.group(3)).intValue();
			seconds = new Integer(matcher.group(4)).intValue();
			tertias = new Integer(matcher.group(5)).intValue();
			break;

		case 5: // -ggg.ggggg        

			double dg = new Double(matcher.group(2)).doubleValue();
			degrees = (int) dg;
			double dgg = Math.abs(dg - degrees);
			minutes = (int) (dgg * fakts);
			seconds = (int) (dgg * faktm - minutes * fakts);
			tertias = (int) (dgg * faktg - minutes * faktm - seconds * fakts + 0.5);
			break;

		case 6:
		case 7: // -ggg:mm.mmmmm oder -gggMM.mmmmm

			degrees = new Integer(matcher.group(2)).intValue();
			double dm = new Double(matcher.group(3)).doubleValue();
			minutes = (int) dm;
			double dmm = Math.abs(dm - minutes);
			seconds = (int) (dmm * fakts);
			tertias = (int) (dmm * faktm - seconds * fakts + 0.5);
			break;

		case 8:
		case 9: // -ggg:mm:ss.sssss oder -gggMMSS.sssss

			degrees = new Integer(matcher.group(2)).intValue();
			minutes = new Integer(matcher.group(3)).intValue();
			double ds = new Double(matcher.group(4)).doubleValue();
			seconds = (int) ds;
			double dss = Math.abs(ds - seconds);
			tertias = (int) (dss * fakts + 0.5);
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
//       // tertias-genau
//      if (decimal >= 0)
//         return decimal;
//      return decimal + 134217728; // = 2^27  > 77760000 = 360 * 60 * 60 * 60
//      }

	public int getHashkeyDist() {
		// Minutes-genau; Wert < 21600; 21600^2 < 2^30
		// absichtlich grob, damit "benachbarte" Punkte in gleiche "Toepfe" fallen
		if (direction == 'N')
			return 60 * (89 - degrees) + minutes;
		if (direction == 'S')
			return 60 * (90 + degrees) + minutes;
		if (direction == 'W')
			return 60 * (179 - degrees) + minutes;
		return 60 * (180 + degrees) + minutes;

	}

	public int getDegrees() {
		return degrees;
	}

	public int getMinutes() {
		return minutes;
	}

	public int getSeconds() {
		return seconds;
	}

	public int getTertias() {
		return tertias;
	}

	public char getDirection() {
		return direction;
	}

	public double toRadians() {
		return Math.toRadians((double) decimal / faktg);
	}

	public double toDegrees() {
		return ((double) decimal / faktg);
	}

	public void setDecimal(int d) {
		decimal = d;
		updateDegrees();
	}

	public void setDegrees(int d) {
		degrees = d;
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

	public void setTertias(int t) {
		tertias = t;
		updateDecimal();
	}

	public void setDegreesMinutesSecondsDirection(int d, int m, int s, char r) {
		degrees = d;
		minutes = m;
		seconds = s;
		direction = r;
		updateDecimal();
	}

	public void set(GeoCoord c) {

		decimal = c.getDecimal();
		doubleValue = c.getDoubleValue();

		updateDegrees();
	}

	public void setDirection(char r) {
		direction = r;
		if (direction == 'O')
			direction = 'E';
		updateDecimal();
	}

	public void add(GeoCoord a) {
		decimal += a.getDecimal();

		updateDegrees();
	}

	public void add(GeoCoord c, GeoCoord a) {
		decimal = c.getDecimal();
		this.add(a);
	}

	public void add(double d) {
		decimal += d;

		updateDegrees();
	}

	public void addSecond(int n) {
		this.add(n * fakts);
	}

	public void sub(GeoCoord c) {
		decimal -= c.getDecimal();

		updateDegrees();
	}

	public void sub(GeoCoord c, GeoCoord s) {
		decimal = c.getDecimal();
		this.sub(s);
	}

	public void sub(double d) {
		decimal -= d;

		updateDegrees();
	}

	public void subSecond(int n) {
		this.sub(n * fakts);
	}

	public void mult(double faktor) {
		decimal *= faktor;

		updateDegrees();
	}

	public void div(double faktor) {
		decimal /= faktor;

		updateDegrees();
	}

	public void toLeft(GeoCoord r) {
		// auf Rasterrand zur Linken shiften
		int raster = r.getDecimal();
		if (decimal < 0)
			decimal -= raster;
		decimal /= raster;
		decimal *= raster;

		updateDegrees();
	}

	public void toLeft(GeoCoord c, GeoCoord r) {
		// c auf Rasterrand zur Linken shiften
		decimal = c.getDecimal();
		this.toLeft(r);
	}

	public void toRight(GeoCoord r) {
		// auf Rasterrand zur Rechten shiften
		int raster = r.getDecimal();
		decimal /= raster;
		decimal *= raster;
		if (decimal >= 0)
			decimal += raster;
		updateDegrees();
	}

	public void toRight(GeoCoord c, GeoCoord r) {
		// c auf Rasterrand zur Rechten shiften
		decimal = c.getDecimal();
		this.toRight(r);
	}

	public boolean lessThen(GeoCoord c) {

		return (decimal < c.getDecimal());
	}

	public boolean lessOrEqual(GeoCoord c) {

		return (decimal <= c.getDecimal());
	}

	public boolean greaterThen(GeoCoord c) {

		return (decimal > c.getDecimal());
	}

	public boolean greaterOrEqual(GeoCoord c) {

		return (decimal >= c.getDecimal());
	}

	public boolean equalTo(GeoCoord c) {

		return (decimal == c.getDecimal());
	}

	public boolean notEqualTo(GeoCoord c) {

		return (decimal != c.getDecimal());
	}

	public String toString() { // = toStringDegreesMinutesSecondsDirection()

		return ""
				+ NumberForm.n2(degrees)
				+ ":"
				+ NumberForm.n2(minutes)
				+ ":"
				+ NumberForm.n2(seconds)
				+ " "
				+ direction;
	}

	public String toStringFine() { // = toStringDegreesMinutesSecondsTertiasDirection()

		return ""
				+ NumberForm.n2(degrees)
				+ ":"
				+ NumberForm.n2(minutes)
				+ ":"
				+ NumberForm.n2(seconds)
				+ ":"
				+ NumberForm.n2(tertias)
				+ " "
				+ direction;
	}

	public String toStringDouble() { // nur für GPS-Datenfiles

		return NumberForm.f6(doubleValue);
	}

	public String toStringDegrees() {

		double d = decimal;
		d /= faktg;

		return NumberForm.f6(d);
	}

	public String toStringDegreesDirection() {

		double d = decimal;
		d /= faktg;
		if (d < 0)
			d = -d;

		return "" + NumberForm.f5(d) + " " + direction;

	}

	public String toStringDegreesMinutesDirection() {

		double m = decimal;
		if (m < 0)
			m = -m;
		m -= degrees * faktg;
		m /= faktm;
		return "" + NumberForm.n2(degrees) + ":" + NumberForm.n2f3(m) + " " + direction;
	}

	public void updateDecimal() {
		decimal = degrees * faktg;
		decimal += minutes * faktm;
		decimal += seconds * fakts;
		decimal += tertias;

		doubleValue = decimal;
		doubleValue /= faktg;

		if (direction == directionMinus()) {
			decimal = -decimal;
			doubleValue = -doubleValue;
		}
	}

	public void updateDegrees() {

		int dec = Math.abs(decimal);

		degrees = dec / faktg;
		dec -= degrees * faktg;
		minutes = dec / faktm;
		dec -= minutes * faktm;
		seconds = dec / fakts;
		dec -= seconds * fakts;
		tertias = dec;
		direction = decimal < 0 ? directionMinus() : directionPlus();

		doubleValue = decimal;
		doubleValue /= faktg;
	}

	public void set(double d) {
		doubleValue = d;
		decimal = (int) (d * faktg);
		updateDegrees();
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

	public void setMean(GeoCoord k1, GeoCoord k2) {
		// auf Mittelwert von k1 und k2 setzen (1-dimensional!) 
		set(k1);
		div(2.);
		GeoCoord kHelp = new GeoCoord();
		kHelp.set(k2);
		kHelp.div(2.);
		add(kHelp);
	}

}
