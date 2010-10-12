// ----------------------------------------------------------------------------
// Copyright 2006-2009, GeoTelematic Solutions, Inc.
// All rights reserved
// ----------------------------------------------------------------------------
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// 
// http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// ----------------------------------------------------------------------------
// Description:
//  Parse NMEA-0183 records, currently the following types:
//      - $GPRMC: Recommended Minimum Specific GPS/TRANSIT Data
//      - $GPGGA: Global Positioning System Fix Data
//      - $GPVTG: Track Made Good and Ground Speed
//      - $GPZDA: UTC Date/Time and Local Time Zone Offset
// References:
//  http://www.scientificcomponent.com/nmea0183.htm
//  http://home.mira.net/~gnb/gps/nmea.html
// ----------------------------------------------------------------------------
// Change History:
//  2007/07/27  Martin D. Flynn
//     -Initial release
//  2007/09/16  Martin D. Flynn
//     -Added 'getExtraData' method to return data following checksum.
//  2008/02/10  Martin D. Flynn
//     -Added handling of $GPVTG and $GPZDA record types
//     -Support parsing and combining multiple record types
//  2008/08/07  Martin D. Flynn
//     -Changed private '_calcChecksum' to public static 'calcXORChecksum'
// ----------------------------------------------------------------------------
package org.opengts.util;

public class Nmea0183 {

	// ------------------------------------------------------------------------

	// Note: these values can change between releases!
	public static final long	TYPE_NONE			= 0x0000000000000000L;
	public static final long	TYPE_GPRMC			= 0x0000000000000001L;
	public static final long	TYPE_GPGGA			= 0x0000000000000002L;
	public static final long	TYPE_GPVTG			= 0x0000000000000004L;
	public static final long	TYPE_GPZDA			= 0x0000000000000008L;

	public static final long	FIELD_RECORD_TYPE	= 0x0000000000000001L;

	// ------------------------------------------------------------------------

	public static final long	FIELD_VALID_FIX		= 0x0000000000000002L;
	public static final long	FIELD_DDMMYY		= 0x0000000000000004L;
	public static final long	FIELD_HHMMSS		= 0x0000000000000008L;
	public static final long	FIELD_LATITUDE		= 0x0000000000000010L;
	public static final long	FIELD_LONGITUDE		= 0x0000000000000020L;
	public static final long	FIELD_SPEED			= 0x0000000000000040L;
	public static final long	FIELD_HEADING		= 0x0000000000000080L;
	public static final long	FIELD_HDOP			= 0x0000000000000100L;
	public static final long	FIELD_NUMBER_SATS	= 0x0000000000000200L;
	public static final long	FIELD_ALTITUDE		= 0x0000000000000400L;
	public static final long	FIELD_FIX_TYPE		= 0x0000000000000800L;
	public static final double	KILOMETERS_PER_KNOT	= 1.85200000;

	// ------------------------------------------------------------------------

	public static final double	KNOTS_PER_KILOMETER	= 1.0 / KILOMETERS_PER_KNOT;
	private boolean				validChecksum		= false;

	// ------------------------------------------------------------------------

	private long				parsedRcdTypes		= TYPE_NONE;
	private String				lastRcdType			= "";
	private long				fieldMask			= 0L;
	private long				ddmmyy				= 0L;

	private long				hhmmss				= 0L;
	private long				fixtime				= 0L;
	private boolean				validGPS			= false;

	private double				latitude			= 0.0;
	private double				longitude			= 0.0;
	private GeoPoint			geoPoint			= null;
	private double				speedKnots			= 0.0;

	private double				heading				= 0.0;
	private double				hdop				= 0.0;

	private int					numSats				= 0;
	private double				altitudeM			= 0.0;
	private int					fixType				= 0;
	private String				extraData			= null;

	/**
	 * Calculates/Returns the checksum for a NMEA-0183 formatted String
	 * 
	 * @param str
	 *            NMEA-0183 formatted String to be checksummed.
	 * @return Checksum computed from input.
	 */
	public static int calcXORChecksum(final String str, final boolean includeAll) {
		final byte b[] = StringTools.getBytes(str);
		if (b == null) {

			/* no bytes */
			return -1;

		} else {

			int cksum = 0, s = 0;

			/* skip leading '$' */
			if (!includeAll && (b.length > 0) && (b[0] == '$')) {
				s++;
			}

			/* calc checksum */
			for (; s < b.length; s++) {
				if (!includeAll && (b[s] == '*')) {
					break;
				}
				if ((b[s] == '\r') || (b[s] == '\n')) {
					break;
				}
				cksum = (cksum ^ b[s]) & 0xFF;
			}

			/* return checksum */
			return cksum;
		}
	}

	// ------------------------------------------------------------------------

	public static String GetTypeNames(final long type) {
		final String sep = ",";
		final StringBuffer sb = new StringBuffer();
		if ((type & TYPE_GPRMC) != 0) {
			if (sb.length() > 0) {
				sb.append(sep);
			}
			sb.append("GPRMC");
		}
		if ((type & TYPE_GPGGA) != 0) {
			if (sb.length() > 0) {
				sb.append(sep);
			}
			sb.append("GPGGA");
		}
		if ((type & TYPE_GPVTG) != 0) {
			if (sb.length() > 0) {
				sb.append(sep);
			}
			sb.append("GPVTG");
		}
		if ((type & TYPE_GPZDA) != 0) {
			if (sb.length() > 0) {
				sb.append(sep);
			}
			sb.append("GPZDA");
		}
		return (sb.length() > 0) ? sb.toString() : "NONE";
	}

	public static void main(final String argv[]) {
//        RTConfig.setCommandLineArgs(argv);
//        
//        if (RTConfig.hasProperty("xor")) {
//            String cksumStr = RTConfig.getString("xor","");
//            int cksum = Nmea0183.calcXORChecksum(cksumStr,true);
//            Print.sysPrintln("Checksum: " + StringTools.toHexString(cksum,8));
//            System.exit(0);
//        }
//        
//        if (RTConfig.hasProperty("test")) {
		Nmea0183 n;
		n = new Nmea0183("$GPRMC,080701.00,A,3128.7540,N,14257.6714,W,000.0,000.0,180707,13.1,E,A*1C");
		Print.sysPrintln("NMEA-0183: \n" + n);
		n = new Nmea0183("$GPGGA,025425.494,3509.0743,N,14207.6314,W,1,04,2.3,530.3,M,-21.9,M,0.0,0000*45");
		Print.sysPrintln("NMEA-0183: \n" + n);
		n = new Nmea0183("$GPGGA,125653.00,3845.165,N,14228.961,W,1,05,,102.1331,M,,M,,*75");
		n.parse("$GPVTG,229.86,T,,M,0.00,N,0.0046,K*55"); // speed/heading
		n.parse("$GPZDA,125653.00,13,09,2007,00,00*6E"); // date/time
		Print.sysPrintln("NMEA-0183: \n" + n);
//        }

	}

	/* instantiate NMEA-0183 record */
	public Nmea0183() {
		super();
	}

	// ------------------------------------------------------------------------

	/* instantiate NMEA-0183 record */
	public Nmea0183(final String rcd) {
		this();
		this._parse(rcd, false);
	}

	/* instantiate NMEA-0183 record */
	public Nmea0183(final String rcd, final boolean ignoreChecksum) {
		this();
		this._parse(rcd, ignoreChecksum);
	}

	// ------------------------------------------------------------------------

	/**
	 * Computes seconds in UTC time given values from GPS device.
	 * 
	 * @param dmy
	 *            Date received from GPS in DDMMYY format, where DD is day, MM is month, YY is year.
	 * @param hms
	 *            Time received from GPS in HHMMSS format, where HH is hour, MM is minute, and SS is
	 *            second.
	 * @return Time in UTC seconds.
	 */
	private long _getUTCSeconds(final long dmy, final long hms) {

		/* time of day [TOD] */
		final int HH = (int) ((hms / 10000L) % 100L);
		final int MM = (int) ((hms / 100L) % 100L);
		final int SS = (int) (hms % 100L);
		final long TOD = (HH * 3600L) + (MM * 60L) + SS;

		/* current UTC day */
		long DAY;
		if (dmy > 0L) {
			final int yy = (int) (dmy % 100L) + 2000;
			final int mm = (int) ((dmy / 100L) % 100L);
			final int dd = (int) ((dmy / 10000L) % 100L);
			final long yr = (yy * 1000L) + (((mm - 3) * 1000) / 12);
			DAY = ((367L * yr + 625L) / 1000L)
					- (2L * (yr / 1000L))
					+ (yr / 4000L)
					- (yr / 100000L)
					+ (yr / 400000L)
					+ dd
					- 719469L;
		} else {
			// we don't have the day, so we need to figure out as close as we can what it should be.
			final long utc = DateTime.getCurrentTimeSec();
			final long tod = utc % DateTime.DaySeconds(1);
			DAY = utc / DateTime.DaySeconds(1);
			final long dif = (tod >= TOD) ? (tod - TOD) : (TOD - tod); // difference should be small (ie. < 1 hour)
			if (dif > DateTime.HourSeconds(12)) { // 12 to 18 hours
				// > 12 hour difference, assume we've crossed a day boundary
				if (tod > TOD) {
					// tod > TOD likely represents the next day
					DAY++;
				} else {
					// tod < TOD likely represents the previous day
					DAY--;
				}
			}
		}

		/* return UTC seconds */
		final long sec = DateTime.DaySeconds(DAY) + TOD;
		return sec;

	}

	/**
	 * Checks if NMEA-0183 formatted String has valid checksum by calculating the checksum of the
	 * payload and comparing that to the received checksum.
	 * 
	 * @param str
	 *            NMEA-0183 formatted String to be checked.
	 * @return true if checksum is valid, false otherwise.
	 */
	private boolean _hasValidChecksum(final String str) {
		final int c = str.indexOf("*");
		if (c < 0) {
			// does not contain a checksum char
			return false;
		}
		final String chkSum = str.substring(c + 1);
		final byte cs[] = StringTools.parseHex(chkSum, null);
		if ((cs == null) || (cs.length != 1)) {
			// invalid checksum hex length
			return false;
		}
		final int calcSum = Nmea0183.calcXORChecksum(str, false);
		final boolean isValid = (calcSum == (cs[0] & 0xFF));
		if (!isValid) {
			Print.logWarn("Expected checksum: 0x" + StringTools.toHexString(calcSum, 8));
		}
		return isValid;
	}

	// ------------------------------------------------------------------------

	/* parse record */
	private boolean _parse(final String rcd, final boolean ignoreChecksum) {

		/* pre-validate */
		if (rcd == null) {
			Print.logError("Null record specified");
			return false;
		} else if (!rcd.startsWith("$")) {
			Print.logError("Invalid record (must begin with '$'): " + rcd);
			return false;
		}

		/* valid checksum? */
		if (ignoreChecksum) {
			this.validChecksum = true;
		} else {
			this.validChecksum = this._hasValidChecksum(rcd);
			if (!this.validChecksum) {
				Print.logError("Invalid Checksum: " + rcd);
				return false;
			}
		}

		/* parse into fields */
		final String fld[] = StringTools.parseString(rcd, ',');
		if ((fld == null) || (fld.length < 1)) {
			Print.logError("Insufficient fields: " + rcd);
			return false;
		}

		/* parse record type */
		this.fieldMask = 0L;
		if (fld[0].equals("$GPRMC")) {
			this.parsedRcdTypes |= TYPE_GPRMC;
			this.lastRcdType = fld[0];
			this.fieldMask |= FIELD_RECORD_TYPE;
			return this._parse_GPRMC(fld);
		} else if (fld[0].equals("$GPGGA")) {
			this.parsedRcdTypes |= TYPE_GPGGA;
			this.lastRcdType = fld[0];
			this.fieldMask |= FIELD_RECORD_TYPE;
			return this._parse_GPGGA(fld);
		} else if (fld[0].equals("$GPVTG")) {
			this.parsedRcdTypes |= TYPE_GPVTG;
			this.lastRcdType = fld[0];
			this.fieldMask |= FIELD_RECORD_TYPE;
			return this._parse_GPVTG(fld); // speed/heading
		} else if (fld[0].equals("$GPZDA")) {
			this.parsedRcdTypes |= TYPE_GPZDA;
			this.lastRcdType = fld[0];
			this.fieldMask |= FIELD_RECORD_TYPE;
			return this._parse_GPZDA(fld);
		} else {
			Print.logError("Record not supported: " + rcd);
			return false;
		}

	}

	// ------------------------------------------------------------------------

	/* parse "$GPGGA" */
	private boolean _parse_GPGGA(final String fld[]) {
		// $GPGGA - Global Positioning System Fix Data
		// $GPGGA,015402.240,0000.0000,N,00000.0000,E,0,00,50.0,0.0,M,18.0,M,0.0,0000*4B
		// $GPGGA,025425.494,3509.0743,N,14207.6314,W,1,04,2.3,530.3,M,-21.9,M,0.0,0000*4D,
		// $GPGGA,    1     ,    2    ,3,     4    ,5,6,7 , 8 ,  9  ,A,  B  ,C, D , E  *MM,F
		//      1   UTC time of position HHMMSS
		//      2   current latitude in ddmm.mm format
		//      3   latitude hemisphere ("N" = northern, "S" = southern)
		//      4   current longitude in dddmm.mm format
		//      5   longitude hemisphere ("E" = eastern, "W" = western)
		//      6   (0=no fix, 1=GPS, 2=DGPS, 3=PPS?, 6=dead-reckoning)
		//      7   number of satellites (00-12)
		//      8   Horizontal Dilution of Precision
		//      9   Height above/below mean geoid (above mean sea level, not WGS-84 ellipsoid height)
		//      A   Unit of height, always 'M' meters
		//      B   Geoidal separation (add to #9 to get WGS-84 ellipsoid height)
		//      C   Unit of Geoidal separation (meters)
		//      D   Age of differential GPS
		//      E   Differential reference station ID (always '0000')
		//      F   Extra data (may not be present)

		/* valid number of fields? */
		if (fld.length < 14) {
			return false;
		}

		/* valid GPS? */
		this.validGPS = !fld[6].equals("0");
		this.fieldMask |= FIELD_VALID_FIX;

		/* fixtime */
		this.hhmmss = StringTools.parseLong(fld[1], 0L);
		this.ddmmyy = 0L; // we don't know the day
		this.fieldMask |= FIELD_HHMMSS;
		this.fixtime = 0L; // calculated later

		/* latitude, longitude, altitude */
		if (this.validGPS) {
			this.latitude = this._parseLatitude(fld[2], fld[3]);
			this.longitude = this._parseLongitude(fld[4], fld[5]);
			if ((this.latitude >= 90.0)
					|| (this.latitude <= -90.0)
					|| (this.longitude >= 180.0)
					|| (this.longitude <= -180.0)) {
				this.validGPS = false;
				this.latitude = 0.0;
				this.longitude = 0.0;
			} else {
				this.fieldMask |= FIELD_LATITUDE | FIELD_LONGITUDE;
				this.fixType = StringTools.parseInt(fld[6], 1); // 1=GPS, 2=DGPS, 3=PPS?, ...
				this.numSats = StringTools.parseInt(fld[7], 0);
				this.hdop = StringTools.parseDouble(fld[8], 0.0);
				this.altitudeM = StringTools.parseDouble(fld[9], 0.0); // meters
				this.fieldMask |= FIELD_FIX_TYPE | FIELD_NUMBER_SATS | FIELD_HDOP | FIELD_ALTITUDE;
			}
		} else {
			this.latitude = 0.0;
			this.longitude = 0.0;
			this.fixType = 0;
			this.numSats = 0;
			this.hdop = 0.0;
			this.altitudeM = 0.0;
		}

		/* extra data? */
		this.extraData = (fld.length >= 16) ? fld[15] : null;

		/* return valid GPS state */
		return this.validGPS;

	}

	/* parse "$GPRMC" */
	private boolean _parse_GPRMC(final String fld[]) {
		// $GPRMC - Recommended Minimum Specific GPS/TRANSIT Data
		// $GPRMC,025423.494,A,3709.0642,N,11907.8315,W,7.094,108.52,200505, , *12,
		// $GPRMC,    1     ,2,    3    ,4,     5    ,6,  7  ,  8   ,  9   ,A,B*MM,D
		//      1   UTC time of position HHMMSS
		//      2   validity of the fix ("A" = valid, "V" = invalid)
		//      3   current latitude in ddmm.mm format
		//      4   latitude hemisphere ("N" = northern, "S" = southern)
		//      5   current longitude in dddmm.mm format
		//      6   longitude hemisphere ("E" = eastern, "W" = western)
		//      7   speed in knots
		//      8   true course in degrees
		//      9   date in DDMMYY format
		//      A   magnetic variation in degrees
		//      B   direction of magnetic variation ("E" = east, "W" = west)
		//      MM  checksum
		//      C   extra data (may not be present)

		/* valid number of fields? */
		if (fld.length < 10) {
			return false;
		}

		/* valid GPS? */
		this.validGPS = fld[2].equals("A");
		this.fieldMask |= FIELD_VALID_FIX;

		/* fixtime */
		this.hhmmss = StringTools.parseLong(fld[1], 0L);
		this.ddmmyy = StringTools.parseLong(fld[9], 0L);
		this.fieldMask |= FIELD_HHMMSS | FIELD_DDMMYY;
		this.fixtime = 0L; // calculated later

		/* latitude, longitude, speed, heading */
		if (this.validGPS) {
			this.latitude = this._parseLatitude(fld[3], fld[4]);
			this.longitude = this._parseLongitude(fld[5], fld[6]);
			if ((this.latitude >= 90.0)
					|| (this.latitude <= -90.0)
					|| (this.longitude >= 180.0)
					|| (this.longitude <= -180.0)) {
				this.validGPS = false;
				this.latitude = 0.0;
				this.longitude = 0.0;
			} else {
				this.fieldMask |= FIELD_LATITUDE | FIELD_LONGITUDE;
				this.speedKnots = StringTools.parseDouble(fld[7], -1.0);
				this.heading = StringTools.parseDouble(fld[8], -1.0);
				this.fieldMask |= FIELD_SPEED | FIELD_HEADING;
			}
		} else {
			this.latitude = 0.0;
			this.longitude = 0.0;
			this.speedKnots = 0.0;
			this.heading = 0.0;
		}

		/* extra data? */
		this.extraData = (fld.length >= 13) ? fld[12] : null;

		/* return valid GPS state */
		return this.validGPS;

	}

	/* parse "$GPVTG" (speed/heading) */
	private boolean _parse_GPVTG(final String fld[]) {
		// $GPVTG - Track Made Good and Ground Speed
		// $GPVTG,229.86,T, ,M,0.00,N,0.0046,K*55
		// $GPVTG,   1  ,2,3,4, 5  ,6,  7   ,8*MM
		//      1   True course over ground, degrees
		//      2   "T" ("True" course)
		//      3   Magnetic course over ground, degrees
		//      4   "M" ("Magnetic" course)
		//      5   Speed over ground in Knots
		//      6   "N" ("Knots")
		//      7   Speed over ground in KM/H
		//      8   "K" ("KM/H")

		/* valid number of fields? */
		if (fld.length < 3) {
			return false;
		}

		/* loop through values */
		for (int i = 1; (i + 1) < fld.length; i += 2) {
			if (fld[i + 1].equals("T")) { // True course
				this.heading = StringTools.parseDouble(fld[i], -1.0);
				this.fieldMask |= FIELD_HEADING;
			} else if (fld[i + 1].equals("N")) { // Knots
				this.speedKnots = StringTools.parseDouble(fld[i], -1.0);
				this.fieldMask |= FIELD_SPEED;
			} else if (fld[i + 1].equals("K")) { // KPH
				final double kph = StringTools.parseDouble(fld[i], -1.0);
				this.speedKnots = (kph >= 0.0) ? (kph * KNOTS_PER_KILOMETER) : -1.0;
				this.fieldMask |= FIELD_SPEED;
			}
		}

		/* success */
		return true;

	}

	/* parse "$GPZDA" */
	private boolean _parse_GPZDA(final String fld[]) {
		// $GPZDA - UTC Date/Time and Local Time Zone Offset
		// $GPZDA,125653.00,13,09,2007,00,00*6E 
		// $GPZDA,   1     , 2, 3,  4 , 5, 6*MM
		//      1   UTC hhmmss.ss
		//      2   Day: 01..31
		//      3   Month: 01..12
		//      4   Year
		//      5   Local zone hours description: -13..00..+13 hours
		//      6   Local zone minutes description (same sign as hours)

		/* valid number of fields? */
		if (fld.length < 5) {
			return false;
		}

		/* parse time */
		this.hhmmss = StringTools.parseLong(fld[1], 0L);

		/* parse date */
		final long day = StringTools.parseLong(fld[2], 0L) % 100L;
		final long month = StringTools.parseLong(fld[3], 0L) % 100L;
		final long year = StringTools.parseLong(fld[4], 0L) % 10000L;
		this.ddmmyy = (day * 10000L) + (month * 100L) + (year % 100L);
		this.fieldMask |= FIELD_HHMMSS | FIELD_DDMMYY;
		this.fixtime = 0L; // calculated later

		/* success */
		return true;

	}

	// ------------------------------------------------------------------------

	/**
	 * Parses latitude given values from GPS device.
	 * 
	 * @param s
	 *            Latitude String from GPS device in ddmm.mm format.
	 * @param d
	 *            Latitude hemisphere, "N" for northern, "S" for southern.
	 * @return Latitude parsed from GPS data, with appropriate sign based on hemisphere or 90.0 if
	 *         invalid latitude provided.
	 */
	private double _parseLatitude(final String s, final String d) {
		final double _lat = StringTools.parseDouble(s, 99999.0);
		if (_lat < 99999.0) {
			double lat = ((long) _lat / 100L); // _lat is always positive here
			lat += (_lat - (lat * 100.0)) / 60.0;
			return d.equals("S") ? -lat : lat;
		} else {
			return 90.0; // invalid latitude
		}
	}

	/**
	 * Parses longitude given values from GPS device.
	 * 
	 * @param s
	 *            Longitude String from GPS device in ddmm.mm format.
	 * @param d
	 *            Longitude hemisphere, "E" for eastern, "W" for western.
	 * @return Longitude parsed from GPS data, with appropriate sign based on hemisphere or 180.0 if
	 *         invalid longitude provided.
	 */
	private double _parseLongitude(final String s, final String d) {
		final double _lon = StringTools.parseDouble(s, 99999.0);
		if (_lon < 99999.0) {
			double lon = ((long) _lon / 100L); // _lon is always positive here
			lon += (_lon - (lon * 100.0)) / 60.0;
			return d.equals("W") ? -lon : lon;
		} else {
			return 180.0; // invalid longitude
		}
	}

	/* return the altitude in meters */
	public double getAltitudeMeters() {
		return this.altitudeM;
	}

	/* return the day/month/year of the fix */
	public long getDDMMYY() {
		return this.ddmmyy;
	}

	// ------------------------------------------------------------------------

	/* return any data that may follow the checksum */
	public String getExtraData() {
		return this.extraData;
	}

	/* return mask of available fields */
	public long getFieldMask() {
		return this.fieldMask;
	}

	/* return the epoch fix time */
	public long getFixtime() {
		if (this.fixtime <= 0L) {
			this.fixtime = this._getUTCSeconds(this.ddmmyy, this.hhmmss);
		}
		return this.fixtime;
	}

	// ------------------------------------------------------------------------

	/* return the "$GPGGA" fix type */
	// (0=no fix, 1=GPS, 2=DGPS, 3=PPS?, 6=dead-reckoning)
	public int getFixType() {
		return this.fixType;
	}

	// ------------------------------------------------------------------------

	/* return lat/lon point */
	public GeoPoint getGeoPoint() {
		if (this.geoPoint == null) {
			this.geoPoint = new GeoPoint(this.getLatitude(), this.getLongitude());
		}
		return this.geoPoint;
	}

	// ------------------------------------------------------------------------

	/* return the horizontal-dilution-of-precision */
	public double getHDOP() {
		return this.hdop;
	}

	// ------------------------------------------------------------------------

	/* return the heading/course in degrees */
	public double getHeading() {
		return this.heading;
	}

	// ------------------------------------------------------------------------

	/* return the hour/minute/seconds of the fix */
	public long getHHMMSS() {
		return this.hhmmss;
	}

	// ------------------------------------------------------------------------

	/* return record type */
	public String getLastRecordType() {
		return this.lastRcdType;
	}

	// ------------------------------------------------------------------------
	// ------------------------------------------------------------------------

	/* return the latitude */
	public double getLatitude() {
		return this.latitude;
	}

	/* return the longitude */
	public double getLongitude() {
		return this.longitude;
	}

	// ------------------------------------------------------------------------

	/* return the number of satellites used in fix */
	public int getNumberOfSatellites() {
		return this.numSats;
	}

	// ----------------------------------------------------------------------------

	/* return all parsed record types (mask) */
	public long getParsedRecordTypes() {
		return this.parsedRcdTypes;
	}

	// ------------------------------------------------------------------------

	/* return the speed in knots */
	public double getSpeedKnots() {
		return this.speedKnots;
	}

	// ------------------------------------------------------------------------

	/* return the speed in KPH */
	public double getSpeedKPH() {
		return this.speedKnots * KILOMETERS_PER_KNOT;
	}

	// ----------------------------------------------------------------------------
	// ----------------------------------------------------------------------------
	// ----------------------------------------------------------------------------

	/* return true if specified field is available */
	public boolean hasField(final long fld) {
		return ((this.fieldMask & fld) != 0);
	}

	/* return true if checksum is valid */
	public boolean isValidChecksum() {
		return this.validChecksum;
	}

	/* return true if the GPS fix is valid */
	public boolean isValidGPS() {
		return this.validGPS;
	}

	// ------------------------------------------------------------------------

	/* parse record */
	public boolean parse(final String rcd) {
		return this._parse(rcd, false);
	}

	/* set the day/month/year (for "$GPGGA" records) */
	public void setDDMMYY(final long ddmmyy) {
		if ((ddmmyy >= 10100L) && (ddmmyy <= 311299L)) { // day/month must be specified
			this.ddmmyy = ddmmyy;
			this.fieldMask |= FIELD_DDMMYY;
		} else {
			this.ddmmyy = 0L;
			this.fieldMask &= ~FIELD_DDMMYY;
		}
	}

	// ------------------------------------------------------------------------
	// ------------------------------------------------------------------------
	// ------------------------------------------------------------------------

	/* return string representation */
	@Override
	public String toString() {
		final long types = this.getParsedRecordTypes();
		final StringBuffer sb = new StringBuffer();
		sb.append("RcdType  : ")
				.append(GetTypeNames(types))
				.append(" [0x")
				.append(StringTools.toHexString(types, 16))
				.append("]\n");
		sb.append("Checksum : ").append(this.isValidChecksum() ? "ok" : "failed").append("\n");
		sb.append("Fixtime  : ")
				.append(this.getFixtime())
				.append(" [")
				.append(new DateTime(this.getFixtime()).toString())
				.append("]\n");
		sb.append("GPS      : ")
				.append(this.isValidGPS() ? "valid " : "invalid ")
				.append(this.getGeoPoint().toString())
				.append("\n");
		sb.append("SpeedKPH : ")
				.append(this.getSpeedKPH())
				.append(" kph, heading ")
				.append(this.getHeading())
				.append("\n");
		sb.append("Altitude : ").append(this.getAltitudeMeters()).append(" meters\n");
		return sb.toString();
	}

}
