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
//  GPS latitude/longitude and algorithms to operate on such.
// ----------------------------------------------------------------------------
// Change History:
//  2006/03/26  Martin D. Flynn
//     -Initial release
//  2006/04/02  Martin D. Flynn
//     -Changed format of lat/lon to include 5 decimal places
//  2006/06/30  Martin D. Flynn
//     -Repackaged
//  2007/02/18  Martin D. Flynn
//     -Added static 'isValid' method
//  2007/02/25  Martin D. Flynn
//     -Added 'String' constructor
//  2007/05/06  Martin D. Flynn
//     -Added 'GeoBounds' class to calculate map bounding box and scale
//  2008/01/10  Martin D. Flynn
//     -Modified 'decodeGeoPoint' to add 0.5 be raw lat/lon before decoding to
//      reduce rounding error (special thanks for B. Jansen for his input on this).
//  2008/04/11  Martin D. Flynn
//     -Updated nautical-mile conversions and abbreviations
//  2008/05/14  Martin D. Flynn
//     -Cleaned up, removed obsolete code
//  2008/05/20  Martin D. Flynn
//     -Addes support for immutability.
//  2008/08/15  Martin D. Flynn
//     -Added 'getHeadingPoint'
//     -Moved 'GeoBounds' to a separate class file.
//  2009/01/01  Martin D. Flynn
//     -Added additional Latitude/Longitude formatting support.
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.util.*;

/**
*** A container for a single latitude/longitude value pair
**/

public class GeoPoint
    implements Cloneable, GeoPointProvider
{

    // ------------------------------------------------------------------------

    private static boolean UseHaversineDistanceFormula = true;

    // ------------------------------------------------------------------------
    
    public  static final double EPSILON             = 1.0E-7; 

    public  static final double MAX_LATITUDE        = 90.0;
    public  static final double MIN_LATITUDE        = -90.0;
    
    public  static final double MAX_LONGITUDE       = 180.0;
    public  static final double MIN_LONGITUDE       = -180.0;
    
    public  static final String PointSeparator      = "/";
    public  static final char   PointSeparatorChar  = '/';

    // ------------------------------------------------------------------------

    private static final int    FORMAT_DEC_MASK         = 0x000F; // format decimal mask
    private static final int    FORMAT_DEC_0            = 0x0001; // "#0.0"
    private static final int    FORMAT_DEC_1            = 0x0001; // "#0.0"
    private static final int    FORMAT_DEC_2            = 0x0002; // "#0.00"
    private static final int    FORMAT_DEC_3            = 0x0003; // "#0.000"
    private static final int    FORMAT_DEC_4            = 0x0004; // "#0.0000"
    private static final int    FORMAT_DEC_5            = 0x0005; // "#0.00000"
    private static final int    FORMAT_DEC_6            = 0x0006; // "#0.000000"
    private static final int    FORMAT_DEC_7            = 0x0007; // "#0.0000000"

    private static final int    FORMAT_TYPE_MASK        = 0x00F0; // format type mask
    public  static final int    FORMAT_DEC              = 0x0010; // decimal format
    public  static final int    FORMAT_DMS              = 0x0020; // DMS format
    public  static final int    FORMAT_DM               = 0x0030; // DM  format

    private static final int    FORMAT_AXIS_MASK        = 0x0F00; // axis mask
    private static final int    FORMAT_LATITUDE         = 0x0100; // latitude
    private static final int    FORMAT_LONGITUDE        = 0x0200; // longitude

    public  static final String DMS_HTML_SEPARATORS[]   = new String[] { "&deg;", "'", "&quot;" };
    public  static final String DMS_TEXT_SEPARATORS[]   = new String[] { "\260", "'", "\"" }; // octal 260 == decimal 176

    public  static final String DECIMAL_FORMAT_0        = "#0";
    public  static final String DECIMAL_FORMAT_1        = "#0.0";
    public  static final String DECIMAL_FORMAT_2        = "#0.00";
    public  static final String DECIMAL_FORMAT_3        = "#0.000";
    public  static final String DECIMAL_FORMAT_4        = "#0.0000";
    public  static final String DECIMAL_FORMAT_5        = "#0.00000";
    public  static final String DECIMAL_FORMAT_6        = "#0.000000";
    public  static final String DECIMAL_FORMAT_7        = "#0.0000000";

    public  static int GetFormatMask(String fmt, boolean isLat)
    {
        int f = isLat? FORMAT_LATITUDE : FORMAT_LONGITUDE;
        if (StringTools.isBlank(fmt)) {
            return f | FORMAT_DEC | FORMAT_DEC_5;
        } else
        if (fmt.equalsIgnoreCase("DMS")) {
            return f | FORMAT_DMS;
        } else
        if (fmt.equalsIgnoreCase("DM")) {
            return f | FORMAT_DM;
        } else
        if (Character.isDigit(fmt.charAt(0))) {
            int decFmt;
            switch (fmt.charAt(0)) {
                case '0': decFmt = FORMAT_DEC_5; break; // 0 == default
                case '1': decFmt = FORMAT_DEC_1; break;
                case '2': decFmt = FORMAT_DEC_2; break;
                case '3': decFmt = FORMAT_DEC_3; break;
                case '4': decFmt = FORMAT_DEC_4; break;
                case '5': decFmt = FORMAT_DEC_5; break;
                case '6': decFmt = FORMAT_DEC_6; break;
                case '7': decFmt = FORMAT_DEC_7; break;
                default : decFmt = FORMAT_DEC_7; break;
            }
            return f | FORMAT_DEC | decFmt;
        } else {
            return f | FORMAT_DEC | FORMAT_DEC_5;
        }
    }
    
    public  static String GetDecimalFormat(int fmt)
    {
        if ((fmt & FORMAT_TYPE_MASK) == FORMAT_DEC) {
            switch (fmt & FORMAT_DEC_MASK) {
                case   0: return DECIMAL_FORMAT_5; // 0 == default
                case   1: return DECIMAL_FORMAT_1;
                case   2: return DECIMAL_FORMAT_2;
                case   3: return DECIMAL_FORMAT_3;
                case   4: return DECIMAL_FORMAT_4;
                case   5: return DECIMAL_FORMAT_5;
                case   6: return DECIMAL_FORMAT_6;
                case   7: return DECIMAL_FORMAT_7;
                default : return DECIMAL_FORMAT_7;
            }
        } else {
            return "";
        }
    }

    // ------------------------------------------------------------------------
    
    /**
    *** An immutable invalid GeoPoint
    **/
    public  static final GeoPoint INVALID_GEOPOINT  = new GeoPoint(0.0,0.0).setImmutable();

    // ------------------------------------------------------------------------

    /**
    *** A private function performing the 'square' of the argument
    *** @param X  The argument to 'square'
    *** @return The square of X (ie. 'X' raised to the 2nd power)
    **/
    private static double SQ(double X) { return X * X; }
    
    // ------------------------------------------------------------------------
    // References:
    //   http://www.jqjacobs.net/astro/geodesy.html
    //   http://www.boeing-727.com/Data/fly%20odds/distance.html
    //   http://mathforum.org/library/drmath/view/51785.html
    //   http://mathforum.org/library/drmath/view/52070.html
    //   http://en.wikipedia.org/wiki/Nautical_mile
    //   http://en.wikipedia.org/wiki/Conversion_of_units
    // GPS Error Analysis:
    //   http://edu-observatory.org/gps/gps_accuracy.html
    //   http://users.erols.com/dlwilson/gps.htm
    //   http://www.gisdevelopment.net/technology/gps/ma04123pf.htm
    
    public  static final double PI                           = Math.PI;
    public  static final double RADIANS                      = PI / 180.0;
    public  static final double EARTH_EQUATORIAL_RADIUS_KM   = 6378.1370;   // Km: a
    public  static final double EARTH_POLOR_RADIUS_KM        = 6356.752314; // Km: b
    public  static final double EARTH_MEAN_RADIUS_KM         = 6371.0088;   // Km: (2a + b)/3 
    public  static final double EARTH_MEAN_RADIUS_METERS     = EARTH_MEAN_RADIUS_KM * 1000.0;
    public  static final double EARTH_CIRCUMFERENCE_KM       = 2.0 * PI * EARTH_MEAN_RADIUS_KM;     // 40030 km
    public  static final double EARTH_ANTIPODAL_KM           = PI * EARTH_MEAN_RADIUS_KM;           // 20015 km

    public  static final double FEET_PER_MILE                = 5280.0;                              // (exact)
    public  static final double KILOMETERS_PER_MILE          = 1.609344;                            // (exact)
    public  static final double MILES_PER_KILOMETER          = 1.0 / KILOMETERS_PER_MILE;           // 0.621371192;
    public  static final double METERS_PER_MILE              = KILOMETERS_PER_MILE * 1000.0;        // 1609.344
    public  static final double METERS_PER_FOOT              = METERS_PER_MILE / FEET_PER_MILE;     // 0.30480
    public  static final double FEET_PER_METER               = 1.0 / METERS_PER_FOOT;               // 3.280839895
    public  static final double FEET_PER_KILOMETER           = FEET_PER_METER * 1000.0;             // 3280.84
    public  static final double KILOMETERS_PER_NAUTICAL_MILE = 1.852;                               // (exact)
    public  static final double NAUTICAL_MILES_PER_KILOMETER = 1.0 / KILOMETERS_PER_NAUTICAL_MILE;  // 0.539956803
    public  static final double MILES_PER_NAUTICAL_MILE      = MILES_PER_KILOMETER * KILOMETERS_PER_NAUTICAL_MILE; // 1.150779
    public  static final double NAUTICAL_MILES_PER_MILE      = 1.0 / MILES_PER_NAUTICAL_MILE;       // 0.868976

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Return the "Heading" title
    *** @return The "Heading" title
    **/
    public static String GetHeadingTitle(Locale locale)
    {
        I18N i18n = I18N.getI18N(GeoPoint.class, locale);
        return i18n.getString("GeoPoint.heading", "Heading"); // "Bearing", "Course"
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** DistanceUnits enumerated type
    **/
    public enum DistanceUnits implements EnumTools.StringLocale, EnumTools.IntValue {
        KILOMETERS     (0,I18N.getString(GeoPoint.class,"GeoPoint.distance.km"    ,"km"    ),I18N.getString(GeoPoint.class,"GeoPoint.speed.kph"  ,"km/h" ), 1.0                           ),
        METERS         (1,I18N.getString(GeoPoint.class,"GeoPoint.distance.meters","meters"),null                                                         , 1000.0                        ),
        MILES          (2,I18N.getString(GeoPoint.class,"GeoPoint.distance.miles" ,"miles" ),I18N.getString(GeoPoint.class,"GeoPoint.speed.mph"  ,"mph"  ), MILES_PER_KILOMETER           ),
        FEET           (3,I18N.getString(GeoPoint.class,"GeoPoint.distance.feet"  ,"feet"  ),null                                                         , FEET_PER_KILOMETER            ),
        NAUTICAL_MILES (4,I18N.getString(GeoPoint.class,"GeoPoint.distance.knots" ,"knots "),I18N.getString(GeoPoint.class,"GeoPoint.speed.knots","knots"), NAUTICAL_MILES_PER_KILOMETER  );
        private int       vv = -1;
        private I18N.Text nn = null;
        private I18N.Text ss = null;
        private double    mm = 1.0;
        DistanceUnits(int v, I18N.Text n, I18N.Text s, double m) { vv=v; nn=n; ss=s; mm=m; }
        public int    getIntValue()              { return vv;                  }
        public String toDistanceAbbr()           { return nn.toString();       }
        public String toDistanceAbbr(Locale loc) { return nn.toString(loc);    }
        public String toSpeedAbbr()              { return (ss != null)? ss.toString()    : ""; }
        public String toSpeedAbbr(Locale loc)    { return (ss != null)? ss.toString(loc) : ""; }
        public String toString()                 { return toDistanceAbbr();    }
        public String toString(Locale loc)       { return toDistanceAbbr(loc); }
        public double convertFromKM(double v)    { return v * mm;              }   // MILES: km * mi/km = mi
        public double convertToKM(double v)      { return v / mm;              }
    };

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** CompassHeading enumerated type
    **/
    public enum CompassHeading implements EnumTools.StringLocale, EnumTools.IntValue {
        N   (0,I18N.getString(GeoPoint.class,"GeoPoint.compass.N" ,"N" ),I18N.getString(GeoPoint.class,"GeoPoint.compass.north"    ,"North"    )),
        NE  (1,I18N.getString(GeoPoint.class,"GeoPoint.compass.NE","NE"),I18N.getString(GeoPoint.class,"GeoPoint.compass.northeast","NorthEast")),
        E   (2,I18N.getString(GeoPoint.class,"GeoPoint.compass.E" ,"E" ),I18N.getString(GeoPoint.class,"GeoPoint.compass.east"     ,"East"     )),
        SE  (3,I18N.getString(GeoPoint.class,"GeoPoint.compass.SE","SE"),I18N.getString(GeoPoint.class,"GeoPoint.compass.southeast","SouthEast")),
        S   (4,I18N.getString(GeoPoint.class,"GeoPoint.compass.S" ,"S" ),I18N.getString(GeoPoint.class,"GeoPoint.compass.south"    ,"South"    )),
        SW  (5,I18N.getString(GeoPoint.class,"GeoPoint.compass.SW","SW"),I18N.getString(GeoPoint.class,"GeoPoint.compass.southwest","SouthWest")),
        W   (6,I18N.getString(GeoPoint.class,"GeoPoint.compass.W" ,"W" ),I18N.getString(GeoPoint.class,"GeoPoint.compass.west"     ,"West"     )),
        NW  (7,I18N.getString(GeoPoint.class,"GeoPoint.compass.NW","NW"),I18N.getString(GeoPoint.class,"GeoPoint.compass.northwest","NorthWest"));
        private int         vv = 0;
        private I18N.Text   aa = null;
        private I18N.Text   dd = null;
        CompassHeading(int v, I18N.Text a, I18N.Text d) { vv=v; aa=a; dd=d; }
        public int     getIntValue()            { return vv; }
        public String  toString()               { return aa.toString();    }
        public String  toString(Locale loc)     { return aa.toString(loc); }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified latitude/longitude are at the origin 0.0,0.0
    *** @param lat  The latitude
    *** @param lon  The longitude
    *** @return True if the specified latitude/longitude are at the origin 0.0,0.0
    **/
    public static boolean isOrigin(double lat, double lon)
    {
        double latAbs = Math.abs(lat);
        double lonAbs = Math.abs(lon);
        if ((latAbs <= 0.0001) && (lonAbs <= 0.0001)) {
            // small square off the coast of Africa (Ghana)
            return true;
        } else {
            return false;
        }
    }
    
    /**
    *** Returns true if the specified latitude/longitude are valid, false otherwise
    *** @param lat  The latitude
    *** @param lon  The longitude
    *** @return True if the specified latitude/longitude are valid, false otherwise
    **/
    public static boolean isValid(double lat, double lon)
    {
        double latAbs = Math.abs(lat);
        double lonAbs = Math.abs(lon);
        if (latAbs >= MAX_LATITUDE) {
            // valid latitude
            return false;
        } else
        if (lonAbs >= MAX_LONGITUDE) {
            // invalid longitude
            return false;
        } else
        if ((latAbs <= 0.0001) && (lonAbs <= 0.0001)) {
            // small square off the coast of Africa (Ghana)
            return false;
        } else {
            return true;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private boolean immutable = false;
    private double  latitude  = 0.0;
    private double  longitude = 0.0;

    /**
    *** Constructor.
    *** This creates a GeoPoint with latitude=0.0, and longitude=0.0
    **/
    public GeoPoint()
    {
        super();
        this.latitude  = 0.0;
        this.longitude = 0.0;
    }

    /**
    *** Copy Constructor.
    *** This copies the specified argument GeoPoint to this constructed GeoPoint
    *** @param gp  The GeoPoint to copy to this constructed GeoPoint
    **/
    public GeoPoint(GeoPoint gp)
    {
        this();
        this.setLatitude(gp.getLatitude());
        this.setLongitude(gp.getLongitude());
        // Note: does not clone "immutability"
    }

    /**
    *** Constructor.
    *** This creates a new GeoPoint with the specified latitude/longitude.
    *** @param latitude  The latitude
    *** @param longitude The longitude
    **/
    public GeoPoint(double latitude, double longitude)
    {
        this();
        this.setLatitude(latitude);
        this.setLongitude(longitude);
    }

    /**
    *** Constructor.
    *** This creates a new GeoPoint with the specified latitude/longitude.
    *** @param latDeg    The latitude degrees
    *** @param latMin    The latitude minutes
    *** @param latSec    The latitude seconds
    *** @param lonDeg    The longitude degrees
    *** @param lonMin    The longitude minutes
    *** @param lonSec    The longitude seconds
    **/
    public GeoPoint(
        double latDeg, double latMin, double latSec, 
        double lonDeg, double lonMin, double lonSec)
    {
        this();
        this.setLatitude( latDeg, latMin, latSec);
        this.setLongitude(lonDeg, lonMin, lonSec);
    }

    /**
    *** Constructor.
    *** This creates a new GeoPoint with the latitude/longitude parsed from the specified String
    *** @param gp  The String containing the GeoPoint to parse ("latitude/longitude")
    **/
    public GeoPoint(String gp)
    {
        this(gp, PointSeparatorChar);
    }
    
    /**
    *** Constructor.
    *** This creates a new GeoPoint with the latitude/longitude parsed from the specified String
    *** @param gp  The String containing the GeoPoint to parse ("latitude/longitude")
    *** @param sep The character which separates the latitude from longitude
    **/
    public GeoPoint(String gp, char sep)
    {
        // Parse "21.1234/-141.1234"
        this();
        if (gp != null) {
            int p = gp.indexOf(sep);
            if (p >= 0) {
                this.setLatitude( StringTools.parseDouble(gp.substring(0,p),0.0));
                this.setLongitude(StringTools.parseDouble(gp.substring(p+1),0.0));
            }
        }
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Retruns a clone of this GeoPoint instance
    *** @return A clone of this GeoPoint
    **/
    public Object clone()
    {
        return new GeoPoint(this); // does NOT clone immutability
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Set this GeoPoint as Immutable
    *** @return This GeoPoint
    **/
    public GeoPoint setImmutable()
    {
        this.immutable = true;
        return this; // to allow chaining
    }
    
    /**
    *** Returns true if this GeoPoint is immutable
    *** @return True if this GeoPoint is immutable, false otherwise.
    **/
    public boolean isImmutable()
    {
        return this.immutable;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** GeoPointProvider interface inplementation.<br>
    *** Returns this GeoPoint
    *** @return This GeoPoint
    **/
    public GeoPoint getGeoPoint()
    {
        return this;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the latitude/longitude contained by the GeoPoint is valid
    *** @return True if the latitude/longitude contained by the GeoPoint is valid
    **/
    public boolean isValid()
    {
        return GeoPoint.isValid(this.getLatitude(), this.getLongitude());
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Sets the Latitude in degrees/minutes/seconds
    *** @param deg  The degrees
    *** @param min  The minutes
    *** @param sec  The seconds
    **/
    public void setLatitude(double deg, double min, double sec)
    {
        this.setLatitude(GeoPoint.convertDmsToDec(deg, min, sec));
    }

    /**
    *** Sets the Latitude in degrees
    *** @param lat  The Latitude
    **/
    public void setLatitude(double lat)
    {
        // immutable?
        if (this.isImmutable()) {
            Print.logError("This GeoPoint is immutable, changing Latitude denied!");
        } else {
            this.latitude = lat;
        }
    }

    /**
    *** Gets the Latitude in degrees
    *** @return The Latitude in degrees
    **/
    public double getLatitude()
    {
        return this.latitude;
    }

    /**
    *** Gets the 'Y' coordinate (same as Latitude)
    *** @return The 'Y' coordinate
    **/
    public double getY()
    {
        return this.latitude;
    }

    /**
    *** Gets the Latitude in radians
    *** @return The Latitude in radians
    **/
    public double getLatitudeRadians()
    {
        return this.getLatitude() * RADIANS;
    }

    /**
    *** Gets the String representation of the Latitude
    *** @param type  The format type
    *** @param locale  The locale (only used for DMS)
    *** @return The String representation of the Latitude
    **/
    public String getLatitudeString(String type, Locale locale)
    {
        return _formatCoord(this.getLatitude(), GetFormatMask(type,true), locale);
    }

    /**
    *** Formats and returns a String representation of the specified Latitude
    *** @param lat  The Latitude to format
    *** @param type  The format type
    *** @param locale  The locale (only used for FORMAT_DMS)
    *** @return The String representation of the Latitude
    **/
    public static String formatLatitude(double lat, String type, Locale locale)
    {
        return _formatCoord(lat, GetFormatMask(type,true), locale);
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the Longitude in degrees/minutes/seconds
    *** @param deg  The degrees
    *** @param min  The minutes
    *** @param sec  The seconds
    **/
    public void setLongitude(double deg, double min, double sec)
    {
        this.setLongitude(GeoPoint.convertDmsToDec(deg, min, sec));
    }

    /**
    *** Sets the Longitude in degrees
    *** @param lon  The Longitude
    **/
    public void setLongitude(double lon)
    {
        if (this.isImmutable()) {
            Print.logError("This GeoPoint is immutable, changing Longitude denied!");
        } else {
            this.longitude = lon;
        }
    }

    /**
    *** Gets the Longitude in degrees
    *** @return The Longitude in degrees
    **/
    public double getLongitude()
    {
        return this.longitude;
    }

    /**
    *** Gets the 'X' coordinate (same as Longitude)
    *** @return The 'X' coordinate
    **/
    public double getX()
    {
        return this.longitude;
    }

     /**
    *** Gets the Longitude in radians
    *** @return The Longitude in radians
    **/
   public double getLongitudeRadians()
    {
        return this.getLongitude() * RADIANS;
    }

    /**
    *** Gets the String representation of the Longitude
    *** @param type  The format type
    *** @param locale  The locale (only used for DMS)
    *** @return The String representation of the Longitude
    **/
    public String getLongitudeString(String type,Locale locale)
    {
        return _formatCoord(this.getLongitude(), GetFormatMask(type,false), locale);
    }
    
    /**
    *** Formats and returns a String representation of the specified Longitude
    *** @param lon  The Longitude to format
    *** @param type  The format type
    *** @param locale  The locale (only used for FORMAT_DMS)
    *** @return The String representation of the Longitude
    **/
    public static String formatLongitude(double lon, String type, Locale locale)
    {
        return _formatCoord(lon, GetFormatMask(type,false), locale);
    }
    
    // ------------------------------------------------------------------------

    public  static final int    ENCODE_HIRES_LEN    = 8;
    public  static final int    ENCODE_LORES_LEN    = 6;

    private static final double POW_24              =    16777216.0; // 2^24
    private static final double POW_28              =   268435456.0; // 2^28
    private static final double POW_32              =  4294967296.0; // 2^32

    /**
    *** Encodes the specified GeoPoint into a byte array
    *** @param gp   The GeoPoint to encode
    *** @param enc  The byte array into which the GeoPoint will be encoded
    *** @param ofs  The offset into the byte array where the encoded GeoPoint will be placed
    *** @param len  Either '6', for 6-byte encoding, or '8', for 8-byte encoding
    *** @return The byte array into which the GeoPoint was encoded
    **/
    public static byte[] encodeGeoPoint(GeoPoint gp, byte enc[], int ofs, int len)
    {
        /* null/empty bytes */
        if (enc == null) {
            return null;
        }
        
        /* offset/length out-of-range */
        if (len < 0) { len = enc.length; }
        if ((ofs + len) > enc.length) {
            return null;
        }
        
        /* not enough bytes to encode */
        if (len < ENCODE_LORES_LEN) {
            return null;
        }

        /* lat/lon */
        double lat = gp.getLatitude();
        double lon = gp.getLongitude();
        
        /* standard resolution */
        if ((len >= ENCODE_LORES_LEN) && (len < ENCODE_HIRES_LEN)) {
            // LL-LL-LL LL-LL-LL
            long rawLat24 = (lat != 0.0)? Math.round((lat -  90.0) * (POW_24 / -180.0)) : 0L;
            long rawLon24 = (lon != 0.0)? Math.round((lon + 180.0) * (POW_24 /  360.0)) : 0L;
            long rawAccum = ((rawLat24 << 24) & 0xFFFFFF000000L) | (rawLon24 & 0xFFFFFFL);
            enc[ofs + 0] = (byte)((rawAccum >> 40) & 0xFF);
            enc[ofs + 1] = (byte)((rawAccum >> 32) & 0xFF);
            enc[ofs + 2] = (byte)((rawAccum >> 24) & 0xFF);
            enc[ofs + 3] = (byte)((rawAccum >> 16) & 0xFF);
            enc[ofs + 4] = (byte)((rawAccum >>  8) & 0xFF);
            enc[ofs + 5] = (byte)((rawAccum      ) & 0xFF);
            return enc;
        } 
        
        /* high resolution */
        if (len >= ENCODE_HIRES_LEN) {
            // LL-LL-LL-LL LL-LL-LL-LL
            long rawLat32 = (lat != 0.0)? Math.round((lat -  90.0) * (POW_32 / -180.0)) : 0L;
            long rawLon32 = (lon != 0.0)? Math.round((lon + 180.0) * (POW_32 /  360.0)) : 0L;
            long rawAccum = ((rawLat32 << 32) & 0xFFFFFFFF00000000L) | (rawLon32 & 0xFFFFFFFFL);
            enc[ofs + 0] = (byte)((rawAccum >> 56) & 0xFF);
            enc[ofs + 1] = (byte)((rawAccum >> 48) & 0xFF);
            enc[ofs + 2] = (byte)((rawAccum >> 40) & 0xFF);
            enc[ofs + 3] = (byte)((rawAccum >> 32) & 0xFF);
            enc[ofs + 4] = (byte)((rawAccum >> 24) & 0xFF);
            enc[ofs + 5] = (byte)((rawAccum >> 16) & 0xFF);
            enc[ofs + 6] = (byte)((rawAccum >>  8) & 0xFF);
            enc[ofs + 7] = (byte)((rawAccum      ) & 0xFF);
            return enc;
        }
       
        /* will never reach here */
        return null;

    }

    /**
    *** Decodes a GeoPoint from the specified byte array
    *** @param enc  The byte array from which the GeoPoint will be decoded
    *** @param ofs  The offset into the byte array where the GeoPoint will be decoded
    *** @param len  Either '6', for 6-byte decoding, or '8', for 8-byte decoding
    *** @return The decoded GeoPoint
    **/
    public static GeoPoint decodeGeoPoint(byte enc[], int ofs, int len)
    {
        
        /* null/empty bytes */
        if (enc == null) {
            return null;
        }
        
        /* offset/length out-of-range */
        if (len < 0) { len = enc.length; }
        if ((ofs + len) > enc.length) {
            return null;
        }
        
        /* not enough bytes to decode */
        if (len < 6) {
            return null;
        }
        
        /* 6-byte standard resolution */
        if ((len >= 6) && (len < 8)) {
            // LL-LL-LL LL-LL-LL
            long rawLat24 = (((long)enc[ofs+0] & 0xFF) << 16) | (((long)enc[ofs+1] & 0xFF) << 8) | ((long)enc[ofs+2] & 0xFF);
            long rawLon24 = (((long)enc[ofs+3] & 0xFF) << 16) | (((long)enc[ofs+4] & 0xFF) << 8) | ((long)enc[ofs+5] & 0xFF);
            double lat = (rawLat24 != 0L)? ((((double)rawLat24 + 0.5) * (-180.0 / POW_24)) +  90.0) : 0.0;
            double lon = (rawLon24 != 0L)? ((((double)rawLon24 + 0.5) * ( 360.0 / POW_24)) - 180.0) : 0.0; // was: - 360.0) : 0.0;
            // TODO: handle +/- 90 latitude, and +/- 180 longitude.
            return new GeoPoint(lat, lon);
        }
        
        /* 8-byte high resolution */
        if (len >= 8) {
            // LL-LL-LL-LL LL-LL-LL-LL
            long rawLat32 = (((long)enc[ofs+0] & 0xFF) << 24) | (((long)enc[ofs+1] & 0xFF) << 16) | (((long)enc[ofs+2] & 0xFF) << 8) | ((long)enc[ofs+3] & 0xFF);
            long rawLon32 = (((long)enc[ofs+4] & 0xFF) << 24) | (((long)enc[ofs+5] & 0xFF) << 16) | (((long)enc[ofs+6] & 0xFF) << 8) | ((long)enc[ofs+7] & 0xFF);
            double lat = (rawLat32 != 0L)? ((((double)rawLat32 + 0.5) * (-180.0 / POW_32)) +  90.0) : 0.0;
            double lon = (rawLon32 != 0L)? ((((double)rawLon32 + 0.5) * ( 360.0 / POW_32)) - 180.0) : 0.0; // was: - 360.0) : 0.0;
            // TODO: handle +/- 90 latitude, and +/- 180 longitude.
            return new GeoPoint(lat, lon);
        }
        
        /* will never reach here */
        return null;
        
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Returns the distance to the specified point, in radians
    *** @param dest  The destination point
    *** @return The distance to the specified point, in radians
    **/
    public double radiansToPoint(GeoPoint dest)
    {
        // Flat plane approximations:
        //   http://mathforum.org/library/drmath/view/51833.html
        //   http://mathforum.org/library/drmath/view/62720.html
        if (dest == null) {
            // you pass in 'null', you deserver what you get
            return Double.NaN;
        } else
        if (this.equals(dest)) {
            // If the points are equals, the radians would be NaN
            return 0.0;
        } else {
            try {
                double lat1 = this.getLatitudeRadians(), lon1 = this.getLongitudeRadians();
                double lat2 = dest.getLatitudeRadians(), lon2 = dest.getLongitudeRadians();
                double rad  = 0.0;
                if (UseHaversineDistanceFormula) {
                    // Haversine formula:
                    // "The Haversine formula may be more accurate for small distances"
                    // See: http://www.census.gov/cgi-bin/geo/gisfaq?Q5.1
                    //      http://mathforum.org/library/drmath/view/51879.html
                    // Also, use of the Haversine formula is about twice as fast as the Law of Cosines
                    double dlat = lat2 - lat1;
                    double dlon = lon2 - lon1;
                    double a    = SQ(Math.sin(dlat/2.0)) + (Math.cos(lat1) * Math.cos(lat2) * SQ(Math.sin(dlon/2.0)));
                    rad = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
                } else {
                    // Law of Cosines for Spherical Trigonometry:
                    // Per http://www.census.gov/cgi-bin/geo/gisfaq?Q5.1 this method isn't recommended:
                    //  "Although this formula is mathematically exact, it is unreliable for
                    //   small distances because the inverse cosine is ill-conditioned."
                    // Note: this problem appears to be less of an issue in Java.  The amount of error
                    // between Law-of-Cosine and Haversine formulas appears small even when calculating
                    // distance aven as low as 1.5 meters.
                    double dlon = lon2 - lon1;
                    rad = Math.acos((Math.sin(lat1) * Math.sin(lat2)) + (Math.cos(lat1) * Math.cos(lat2) * Math.cos(dlon)));
                }

                return rad;

            } catch (Throwable t) { // trap any Math error

                return Double.NaN;

            }
        }
    }

    /**
    *** Returns the distance to the specified point, in kilometers
    *** @param gp  The destination point
    *** @return The distance to the specified point, in kilometers
    **/
    public double kilometersToPoint(GeoPoint gp)
    {
        double radians = this.radiansToPoint(gp);
        return !Double.isNaN(radians)? (EARTH_MEAN_RADIUS_KM * radians) : Double.NaN;
    }

    /**
    *** Returns the distance to the specified point, in meters
    *** @param gp  The destination point
    *** @return The distance to the specified point, in meters
    **/
    public double metersToPoint(GeoPoint gp)
    {
        double radians = this.radiansToPoint(gp);
        return !Double.isNaN(radians)? ((EARTH_MEAN_RADIUS_KM * 1000.0) * radians) : Double.NaN;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a GeoOffset object containing 'delta' Latitude/Longitude values which 
    *** represent a 'bounding-box' for this GeoPoint with the specified radius (in meters).
    *** That is, this GeoOffset Latitude/Longitude +/- the returned 'delta' Latitude/Longitude,
    *** represent a bounding area for this GeoPoint and the specified meter radius.
    *** @param radiusMeters  The radius in meters
    *** @return The 'delta' Latitude/Longitude GeoOffset
    **/
    public GeoOffset getRadiusDeltaPoint(double radiusMeters)
    {
        double a = EARTH_EQUATORIAL_RADIUS_KM * 1000.0;
        double b = EARTH_POLOR_RADIUS_KM * 1000.0;
        double lat = this.getLatitudeRadians();
        // r(T) = (a^2) / sqrt((a^2)*(cos(T)^2) + (b^2)*(sin(T)^2))
        double r = SQ(a) / Math.sqrt((SQ(a) * SQ(Math.cos(lat))) + (SQ(b) * SQ(Math.sin(lat))));
        // dlat = (180 * R) / (PI * r);
        double dlat = (180.0 * radiusMeters) / (Math.PI * r);
        // dlon = dlat / cos(lat);
        double dlon = dlat / Math.cos(lat);
        return new GeoOffset(dlat, dlon);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a String representation of the spedified compass heading value
    *** @param heading  The compass heading value to convert to a String representation
    *** @param locale   The locale
    *** @return A String representation of the compass heading (ie. "N", "NE", "E", "SE", "S", "SW", "W", "NW")
    **/
    public static String GetHeadingString(double heading, Locale locale)
    {
        if (!Double.isNaN(heading) && (heading >= 0.0)) {
            int h = (int)Math.round(heading / 45.0) % 8;
            //return DIRECTION[(h > 7)? 0 : h];
            switch (h) {
                case 0: return CompassHeading.N .toString(locale);
                case 1: return CompassHeading.NE.toString(locale);
                case 2: return CompassHeading.E .toString(locale);
                case 3: return CompassHeading.SE.toString(locale);
                case 4: return CompassHeading.S .toString(locale);
                case 5: return CompassHeading.SW.toString(locale);
                case 6: return CompassHeading.W .toString(locale);
                case 7: return CompassHeading.NW.toString(locale);
            }
            return CompassHeading.N.toString(locale);
        } else {
            return "";
        }
    }

    /**
    *** Returns the compass heading that would be followed if travelling from this GeoPoint to
    *** the specified GeoPoint
    **/
    public double headingToPoint(GeoPoint dest)
    {
        // Assistance from:
        //   http://mathforum.org/library/drmath/view/55417.html
        //   http://williams.best.vwh.net/avform.htm
        try {              
            double lat1 = this.getLatitudeRadians(), lon1 = this.getLongitudeRadians();
            double lat2 = dest.getLatitudeRadians(), lon2 = dest.getLongitudeRadians();
            double dist = this.radiansToPoint(dest);
            double rad  = Math.acos((Math.sin(lat2) - (Math.sin(lat1) * Math.cos(dist))) / (Math.sin(dist) * Math.cos(lat1)));
            if (Math.sin(lon2 - lon1) < 0) { rad = (2.0 * Math.PI) - rad; }
            double deg  = rad / RADIANS;
            return deg;
        } catch (Throwable t) { // trap any Math error
            Print.logException("headingToPoint", t);
            return 0.0;
        }
    }
    
    /**
    *** Return a new point which is the specified distance (in meters) from this point toward 
    *** the specified heading
    *** @param heading  The heading
    *** @param distM    The distance in meters
    **/
    public GeoPoint getHeadingPoint(double distM, double heading)
    {
        double crLat = this.getLatitudeRadians();           // radians
        double crLon = this.getLongitudeRadians();          // radians
        double d     = distM / GeoPoint.EARTH_MEAN_RADIUS_METERS;
        double xrad  = heading * GeoPoint.RADIANS;          // radians
        double rrLat = Math.asin(Math.sin(crLat) * Math.cos(d) + Math.cos(crLat) * Math.sin(d) * Math.cos(xrad));
        double rrLon = crLon + Math.atan2(Math.sin(xrad) * Math.sin(d) * Math.cos(crLat), Math.cos(d)-Math.sin(crLat) * Math.sin(rrLat));
        return new GeoPoint(rrLat / GeoPoint.RADIANS, rrLon / GeoPoint.RADIANS);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a String representation of this GeoPoint
    *** @return A String representation of this GeoPoint
    **/
    public String toString()
    {
        return this.toString(PointSeparatorChar);
    }
    
    /**
    *** Returns a String representation of this GeoPoint
    *** @param sep  The character used to separate the Latitude from the Longitude
    *** @return A String representation of this GeoPoint
    **/
    public String toString(char sep)
    {
        return this.getLatitudeString(null,null) + sep + this.getLongitudeString(null,null);
    }

    /**
    *** Returns a String representation of this GeoPoint
    *** @param type  The format type
    *** @return A String representation of this GeoPoint
    **/
    public String toString(String type, Locale locale)
    {
        return this.toString(type, PointSeparatorChar, locale);
    }
    
    /**
    *** Returns a String representation of this GeoPoint
    *** @param type  The format type
    *** @param sep  The character used to separate the Latitude from the Longitude
    *** @return A String representation of this GeoPoint
    **/
    public String toString(String type, char sep, Locale locale)
    {
        return this.getLatitudeString(type, locale) + sep + this.getLongitudeString(type, locale);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this GeoPoint is equivalent to the other Object
    *** @return True if this GeoPoint is equivalent to the other Object
    **/
    public boolean equals(Object other)
    {
        if (other instanceof GeoPoint) {
            GeoPoint gp = (GeoPoint)other;
            double deltaLat = Math.abs(gp.getLatitude()  - this.getLatitude() );
            double deltaLon = Math.abs(gp.getLongitude() - this.getLongitude());
            return ((deltaLat < EPSILON) && (deltaLon < EPSILON));
        } else {
            return false;
        }
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Converts the specified degrees/minutes/seconds into degrees
    *** @param deg  The degrees
    *** @param min  The minutes
    *** @param sec  The seconds
    *** @return Decimal degrees
    **/
    public static double convertDmsToDec(int deg, int min, int sec)
    {
        return GeoPoint.convertDmsToDec((double)deg, (double)min, (double)sec);
    }
    
    /**
    *** Converts the specified degrees/minutes/seconds into degrees
    *** @param deg  The degrees
    *** @param min  The minutes
    *** @param sec  The seconds
    *** @return Decimal degrees
    **/
    public static double convertDmsToDec(double deg, double min, double sec)
    {
        double sign = (deg >= 0.0)? 1.0 : -1.0;
        double d = Math.abs(deg);
        double m = Math.abs(min / 60.0);
        double s = Math.abs(sec / 3600.0);
        return sign * (d + m + s);
    }
    
    // ------------------------------------------------------------------------

    /** 
    *** Formats the specified coordinate based on the default decimal format value.
    *** @param loc  The coordinate to format
    *** @param fmt  A degrees/minutes/seconds formatting mask. (logically 'or'ed: 
    **              'FORMAT_DMS' to format in degrees/minutes/seconds format, 
    ***             'FORMAT_LATITUDE' to include N/S specification on Latitude,
    ***             'FORMAT_LONGITUDE' to include E/W specification on Latitude.)
    *** @param decimals The number of decimal places (only used for FORMAT_DECIMAL)
    *** @param locale   The locale (only used for FORMAT_DMS/FORMAT_DM)
    *** @return The String formatted coordinate
    **/
    protected static String _formatCoord(double loc, int fmt, Locale locale)
    {
        boolean html = false;
        if ((fmt & FORMAT_TYPE_MASK) == FORMAT_DMS) {
            String SEP[] = html? DMS_HTML_SEPARATORS : DMS_TEXT_SEPARATORS;
            int    sgn   = (loc >= 0.0)? 1 : -1;
            double abs   = Math.abs(loc);
            int    deg   = (int)abs;
            double accum = (abs - (double)deg) * 60.0;
            int    min   = (int)accum;
            accum = (accum - (double)min) * 60.0;
            int    sec   = (int)accum;
            StringBuffer sb = new StringBuffer();
            sb.append(StringTools.format(deg, "0")).append(SEP[0]);
            sb.append(StringTools.format(min,"00")).append(SEP[1]);
            sb.append(StringTools.format(sec,"00")).append(SEP[2]);
            if ((fmt & FORMAT_AXIS_MASK) == FORMAT_LATITUDE) {
                sb.append((sgn >= 0)? CompassHeading.N.toString(locale) : CompassHeading.S.toString(locale));
            } else
            if ((fmt & FORMAT_AXIS_MASK) == FORMAT_LONGITUDE) {
                sb.append((sgn >= 0)? CompassHeading.E.toString(locale) : CompassHeading.W.toString(locale));
            }
            return sb.toString();
        } else
        if ((fmt & FORMAT_TYPE_MASK) == FORMAT_DM) {
            String SEP[] = html? DMS_HTML_SEPARATORS : DMS_TEXT_SEPARATORS;
            int    sgn   = (loc >= 0.0)? 1 : -1;
            double abs   = Math.abs(loc);
            int    deg   = (int)abs;
            double min   = (abs - (double)deg) * 60.0;
            StringBuffer sb = new StringBuffer();
            sb.append(StringTools.format(deg,    "0")).append(SEP[0]);
            sb.append(StringTools.format(min,"00.00")).append(SEP[1]);
            if ((fmt & FORMAT_AXIS_MASK) == FORMAT_LATITUDE) {
                sb.append((sgn >= 0)? CompassHeading.N.toString(locale) : CompassHeading.S.toString(locale));
            } else
            if ((fmt & FORMAT_AXIS_MASK) == FORMAT_LONGITUDE) {
                sb.append((sgn >= 0)? CompassHeading.E.toString(locale) : CompassHeading.W.toString(locale));
            }
            return sb.toString();
        } else {
            // NOTE: European locale may attempt to format this value with "," instead of "."
            // This needs to be "." in order to work for CSV files, etc.
            return StringTools.format(loc, GetDecimalFormat(fmt));
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    private static GeozoneChecker geozoneCheck = null;

    /**
    *** Returns an object that implements the GeozoneChecker interface implementing a point/radius
    *** geozone check.
    *** @return A object implementing the GeozoneChecker interface.
    **/
    public static GeozoneChecker getGeozoneChecker()
    {
        if (geozoneCheck == null) {
            geozoneCheck = new GeozoneChecker() {
                public boolean containsPoint(GeoPoint gpTest, GeoPoint gpList[], double radiusKM) {
                    if ((gpList != null) && (gpTest != null)) {
                        for (int i = 0; i < gpList.length; i++) {
                            double km = gpList[i].kilometersToPoint(gpTest);
                            //Print.logInfo("Inside? (" + km + " <= " + radiusKM + ")?");
                            if (km <= radiusKM) {
                                return true;
                            }
                        }
                    }
                    return false;
                }
            };
        }
        return geozoneCheck;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Testing/debugging command-line entry point
    *** @param argv  The command-line arguments.
    **/
    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);

        /* encode/decode test */
        GeoPoint gp  = new GeoPoint(RTConfig.getString("gp",""));
        if (gp.isValid()) {
            byte gpEnc[] = new byte[8];
            encodeGeoPoint(gp, gpEnc, 0, 8);
            GeoPoint gp8 = decodeGeoPoint(gpEnc, 0, 8);
            Print.logInfo("8-byte encoded/decoded GeoPoint = " + gp8);
            encodeGeoPoint(gp, gpEnc, 0, 6);
            GeoPoint gp6 = decodeGeoPoint(gpEnc, 0, 6);
            Print.logInfo("6-byte encoded/decoded GeoPoint = " + gp6);
            System.exit(0);
        }
        
        /* distance between points */
        GeoPoint gp1 = new GeoPoint(RTConfig.getString("gp1",""));
        GeoPoint gp2 = new GeoPoint(RTConfig.getString("gp2",""));
        if (gp1.isValid() && gp2.isValid()) {
            double km = gp1.kilometersToPoint(gp2);
            Print.logInfo("Distance = " + km + " km");
            long deltaSec = RTConfig.getLong("deltaSec", 0L);
            if (deltaSec > 0L) {
                double kph = km / ((double)deltaSec / 3600.0);
                Print.logInfo("Speed = " + kph + " kph [" + (kph * MILES_PER_KILOMETER) + " mph]");
            }
            System.exit(0);
        }
        
    }
    
}
