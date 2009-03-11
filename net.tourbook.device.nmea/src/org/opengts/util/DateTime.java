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
//  This class provides many Date/Time utilities
// ----------------------------------------------------------------------------
// Change History:
//  2006/03/26  Martin D. Flynn
//      Initial release
//  2006/06/30  Martin D. Flynn
//     -Repackaged
//  2008/01/10  Martin D. Flynn
//     -Added 'getMonthStart'/'getMonthEnd' methods.
//  2008/02/17  Martin D. Flynn
//     -Added date format constants
//  2008/02/21  Martin D. Flynn
//     -Replaced date format constants with 'default' date/time formats
//  2008/03/12  Martin Dl Flynn
//     -Added method 'parseArgumentDate'
//  2008/04/11  Martin Dl Flynn
//     -Modified 'parseArgumentDate' to use the argument timezone when calculating
//      the default year/month/day.
//  2008/05/20  Martin Dl Flynn
//     -Modified 'parseArgumentDate' to support additional date/time formats
//  2008/07/20  Martin Dl Flynn
//     -Extracted 'parseDateTime' from 'parseArgumentDate'
//     -Added methods for returning the days since October 15, 1582 (Gregorian calendar)
//  2009/01/28  Martin D. Flynn
//     -Fixed bug in ParsedDateTime that didn't correctly initialize the timezone.
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.util.*;
import java.io.*;

import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.text.DateFormat;

/**
*** Performs every manner of function imaginable based on date/time values
**/

public class DateTime
    implements Comparable, Cloneable
{
    
    // ------------------------------------------------------------------------

    public static final String GMT_TIMEZONE             = "GMT";

    // ------------------------------------------------------------------------

    public static final String DEFAULT_TIME_FORMAT      = "HH:mm:ss";
    public static final String DEFAULT_DATE_FORMAT      = "yyyy/MM/dd";

    // ------------------------------------------------------------------------
    // DateParseException
    
    /**
    *** DateParseException class
    **/
    public static class DateParseException
        extends Exception
    {
        public DateParseException(String msg) {
            super(msg);
        }
        public DateParseException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Analyzes the specified date format to determine the date separator characters
    *** @param dateFormat The date format to analyze
    *** @return The date separator characters
    **/
    public static char[] GetDateSeparatorChars(String dateFormat)
    {
        char sep[] = new char[] { '/', '/' };
        if (dateFormat != null) {
            for (int i = 0, c = 0; (i < dateFormat.length()) && (c < sep.length); i++) {
                char ch = dateFormat.charAt(i);
                if (!Character.isLetterOrDigit(ch)) {
                    sep[c++] = ch;
                }
            }
        }
        return sep;
    }

    // ------------------------------------------------------------------------
    
    public static final long HOURS_PER_DAY              = 24L;
    public static final long SECONDS_PER_MINUTE         = 60L;
    public static final long MINUTES_PER_HOUR           = 60L;
    public static final long DAYS_PER_WEEK              = 7L;
    public static final long SECONDS_PER_HOUR           = SECONDS_PER_MINUTE * MINUTES_PER_HOUR;
    public static final long MINUTES_PER_DAY            = HOURS_PER_DAY * MINUTES_PER_HOUR;
    public static final long SECONDS_PER_DAY            = MINUTES_PER_DAY * SECONDS_PER_MINUTE;
    public static final long MINUTES_PER_WEEK           = DAYS_PER_WEEK * MINUTES_PER_DAY;
    public static final long SECONDS_PER_WEEK           = MINUTES_PER_WEEK * SECONDS_PER_MINUTE;

    /** 
    *** Returns the number of seconds in the specified number of days
    *** @param days  The number of days to convert to seconds
    *** @return The number of seconds
    **/
    public static long DaySeconds(long days)
    {
        return days * SECONDS_PER_DAY;
    }

    /** 
    *** Returns the number of seconds in the specified number of days
    *** @param days  The number of days to convert to seconds
    *** @return The number of seconds
    **/
    public static long DaySeconds(double days)
    {
        return (long)Math.round(days * (double)SECONDS_PER_DAY);
    }
    
    /** 
    *** Returns the number of seconds in the specified number of hours
    *** @param hours  The number of hours to convert to seconds
    *** @return The number of seconds
    **/
    public static long HourSeconds(long hours)
    {
        return hours * SECONDS_PER_HOUR;
    }
    
    /** 
    *** Returns the number of seconds in the specified number of minutes
    *** @param minutes  The number of minutes to convert to seconds
    *** @return The number of seconds
    **/
    public static long MinuteSeconds(long minutes)
    {
        return minutes * SECONDS_PER_MINUTE;
    }

    // ------------------------------------------------------------------------
    
    public static final int JAN       = 0;
    public static final int FEB       = 1;
    public static final int MAR       = 2;
    public static final int APR       = 3;
    public static final int MAY       = 4;
    public static final int JUN       = 5;
    public static final int JUL       = 6;
    public static final int AUG       = 7;
    public static final int SEP       = 8;
    public static final int OCT       = 9;
    public static final int NOV       = 10;
    public static final int DEC       = 11;
    
    public static final int JANUARY   = JAN;
    public static final int FEBRUARY  = FEB;
    public static final int MARCH     = MAR;
    public static final int APRIL     = APR;
  //public static final int MAY       = MAY;
    public static final int JUNE      = JUN;
    public static final int JULY      = JUL;
    public static final int AUGUST    = AUG;
    public static final int SEPTEMBER = SEP;
    public static final int OCTOBER   = OCT;
    public static final int NOVEMBER  = NOV;
    public static final int DECEMBER  = DEC;
    
    // I18N?
    private static final String MONTH_NAME[][] = {
        { "January"  , "Jan" },
        { "February" , "Feb" },
        { "March"    , "Mar" },
        { "April"    , "Apr" },
        { "May"      , "May" },
        { "June"     , "Jun" },
        { "July"     , "Jul" },
        { "August"   , "Aug" },
        { "September", "Sep" },
        { "October"  , "Oct" },
        { "November" , "Nov" },
        { "December" , "Dec" },
    };

    /**
    *** Gets the 0-based index for the specified month abbreviation
    *** @param month  The month abbreviation
    *** @return The 0-based index [0..11] of the specified month appreviation
    **/
    public static int getMonthIndex0(String month, int dft)
    {
        String m = (month != null)? month.toLowerCase().trim() : null;
        if ((m != null) && !m.equals("")) {
            if (Character.isDigit(m.charAt(0))) {
                int v = StringTools.parseInt(m,-1);
                if ((v >= 0) && (v < 12)) {
                    return v;
                }
            } else {
                for (int i = 0; i < MONTH_NAME.length; i++) {
                    if (m.startsWith(MONTH_NAME[i][1].toLowerCase())) {
                        return i;
                    }
                }
            }
        }
        return dft;
    }

    /**
    *** Gets the 1-based index for the specified month abbreviation
    *** @param month  The month abbreviation
    *** @return The 1-based index [1..12] of the specified month appreviation
    **/
    public static int getMonthIndex1(String month, int dft)
    {
        return DateTime.getMonthIndex0(month, dft - 1) + 1;
    }

    /**
    *** Gets the month name/abbreviation for the specified 1-based month index
    *** @param mon1  A 1-based month index [1..12]
    *** @param abbrev  True to return the month abbreviation, false to return the name
    *** @return The month name/abbreviation
    **/
    public static String getMonthName(int mon1, boolean abbrev)
    {
        int mon0 = mon1 - 1;
        if ((mon0 >= JANUARY) && (mon0 <= DECEMBER)) {
            return abbrev? MONTH_NAME[mon0][1] : MONTH_NAME[mon0][0];
        } else {
            return "";
        }
    }

    /**
    *** Returns all month names/appreviations
    *** @param abbrev  True to return month abbreviations, false to return month names
    *** @return  An array of month names/abbreviations
    **/
    public static String[] getMonthNames(boolean abbrev)
    {
        String mo[] = new String[MONTH_NAME.length];
        for (int i = 0; i < MONTH_NAME.length; i++) {
            mo[i] = DateTime.getMonthName(i, abbrev);
        }
        return mo;
    }
    
    /**
    *** Returns a Map object containing a map of month names/abbreviations and it's
    *** 0-based month index [0..11]
    *** @param abbrev  True to create the Map object with abbreviations, false for names
    *** @return The Map object
    **/
    public static Map<String,Integer> getMonthNameMap(boolean abbrev)
    {
        Map<String,Integer> map = new OrderedMap<String,Integer>();
        for (int i = 0; i < MONTH_NAME.length; i++) {
            map.put(DateTime.getMonthName(i, abbrev), new Integer(i));
        }
        return map;
    }

    // ------------------------------------------------------------------------

    private static final int MONTH_DAYS[] = {
        31, // Jan
        29, // Feb
        31, // Mar
        30, // Apr
        31, // May
        30, // Jun
        31, // Jul
        31, // Aug
        30, // Sep
        31, // Oct
        30, // Nov        
        31, // Dec
    };
    
    /**
    *** Gets the number of days in the specified month
    *** @param tz   The TimeZone
    *** @param mon1 The 1-based month index [1..12]
    *** @param year The year [valid for all AD years]
    *** @return The number of days in the specified month
    **/
    public static int getDaysInMonth(TimeZone tz, int mon1, int year)
    {
        int yy = (year > mon1)? year : mon1; // larger of the two
        int mm = (year > mon1)? mon1 : year; // smaller of the two
        return DateTime.getMaxMonthDayCount(mm, DateTime.isLeapYear(yy));
    }
    
    /**
    *** Gets the maximum number of days in the specified month
    *** @param mon1  The 1-based month index [1..12]
    *** @param isLeapYear  True for leap-year, false otherwise
    *** @return The maximum number of days in the specified month
    **/
    public static int getMaxMonthDayCount(int mon1, boolean isLeapYear)
    {
        int m0 = mon1 - 1;
        int d = ((m0 >= 0) && (m0 < DateTime.MONTH_DAYS.length))? DateTime.MONTH_DAYS[m0] : 31;
        return ((m0 != FEBRUARY) || isLeapYear)? d : 28;
    }
    
    /** 
    *** Returns true if the specified year represents a leap-year
    *** @param year  The year [valid for all AD years]
    *** @return True if the year is a leap-year, false otherwise
    **/
    public static boolean isLeapYear(int year)
    {
        return (new GregorianCalendar()).isLeapYear(year);
    }
    
    // ------------------------------------------------------------------------
    
    public static final int SUN       = 0;
    public static final int MON       = 1;
    public static final int TUE       = 2;
    public static final int WED       = 3;
    public static final int THU       = 4;
    public static final int FRI       = 5;
    public static final int SAT       = 6;
    
    public static final int SUNDAY    = SUN;
    public static final int MONDAY    = MON;
    public static final int TUESDAY   = TUE;
    public static final int WEDNESDAY = WED;
    public static final int THURSDAY  = THU;
    public static final int FRIDAY    = FRI;
    public static final int SATURDAY  = SAT;
    
    // I18N?
    private static final String DAY_NAME[][] = {
        // 0            1      2
        { "Sunday"   , "Sun", "Su" },
        { "Monday"   , "Mon", "Mo" },
        { "Tuesday"  , "Tue", "Tu" },
        { "Wednesday", "Wed", "We" },
        { "Thursday" , "Thu", "Th" },
        { "Friday"   , "Fri", "Fr" },
        { "Saturday" , "Sat", "Sa" },
    };
    
    /**
    *** Gets the day-of-week number for the specified day short abbreviation
    *** @param day  The day short abbreviation
    *** @return The 0-based day index [0..6]
    **/
    public static int getDayIndex(String day, int dft)
    {
        String d = (day != null)? day.toLowerCase().trim() : null;
        if (!StringTools.isBlank(d)) {
            if (Character.isDigit(d.charAt(0))) {
                int v = StringTools.parseInt(d,-1);
                if ((v >= 0) && (v < 7)) {
                    return v;
                }
            } else {
                for (int i = 0; i < DAY_NAME.length; i++) {
                    if (d.startsWith(DAY_NAME[i][2].toLowerCase())) {
                        return i;
                    }
                }
            }
        }
        return dft;
    }

    /**
    *** Gets the day-of-week name for the specified day number/index
    *** @param day  A 0-based day number/index [0..6]
    *** @param abbrev  0 for full name, 1 for abbreviation, 2 for short abbreviation
    *** @return  The day-of-week name/abbreviation
    **/
    public static String getDayName(int day, int abbrev)
    {
        if ((day >= SUNDAY) && (day <= SATURDAY) && (abbrev >= 0) && (abbrev <= 2)) {
            return DAY_NAME[day][abbrev];
        } else {
            return "";
        }
    }
    
    /**
    *** Gets the day-of-week name for the specified day number/index
    *** @param day  A 0-based day number/index [0..6]
    *** @param abbrev  True for abbreviation, false for full name
    *** @return  The day-of-week name/abbreviation
    **/
    public static String getDayName(int day, boolean abbrev)
    {
        return DateTime.getDayName(day, abbrev? 1 : 0);
    }

    /**
    *** Returns an array of day-of-week names
    *** @param abbrev  0 for full name, 1 for abbreviation, 2 for short abbreviation
    *** @return An array of day-of-week names/abbreviations
    **/
    public static String[] getDayNames(int abbrev)
    {
        String dy[] = new String[DAY_NAME.length];
        for (int i = 0; i < DAY_NAME.length; i++) {
            dy[i] = DateTime.getDayName(i, abbrev);
        }
        return dy;
    }

    /**
    *** Returns an array of day-of-week names
    *** @param abbrev  True abbreviations, false for full names
    *** @return An array of day-of-week names/abbreviations
    **/
    public static String[] getDayNames(boolean abbrev)
    {
        return DateTime.getDayNames(abbrev? 1 : 0);
    }

    /**
    *** Returns a Map object of day-of-week names to their 0-based number/index
    *** (used as a VComboBox item list)
    *** @param abbrev 0 for full name, 1 for abbreviation, 2 for short abbreviation
    *** @return The Map object
    **/
    public static Map<String,Integer> getDayNameMap(int abbrev)
    {
        Map<String,Integer> map = new OrderedMap<String,Integer>();
        for (int i = 0; i < DAY_NAME.length; i++) {
            map.put(DateTime.getDayName(i, abbrev), new Integer(i));
        }
        return map;
    }

    /**
    *** Returns a Map object of day-of-week names to their 0-based number/index
    *** (used as a VComboBox item list)
    *** @param abbrev True for abbreviations, false for full names
    *** @return The Map object
    **/
    public static Map<String,Integer> getDayNameMap(boolean abbrev)
    {
        return DateTime.getDayNameMap(abbrev? 1 : 0);
    }
    
    /**
    *** Returns the day of the week for the specified year/month/day
    *** @param year  The year [valid for dates after 1582/10/15]
    *** @param mon1  The month [1..12]
    *** @param day   The day [1..31]
    *** @return The day of the week (0=Sunday, 6=Saturday)
    **/
    public static int getDayOfWeek(int year, int mon1, int day)
    {
        int mon0 = mon1 - 1;
        GregorianCalendar cal = new GregorianCalendar(year, mon0, day);
        return cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;
    }
    
    /**
    *** Returns the day of the year for the specified year/month/day
    *** @param year  The year [valid for dates after 1582/10/15]
    *** @param mon1  The month [1..12]
    *** @param day   The day [1..31]
    *** @return The day of the year
    **/
    public static int getDayOfYear(int year, int mon1, int day)
    {
        int mon0 = mon1 - 1;
        GregorianCalendar cal = new GregorianCalendar(year, mon0, day);
        return cal.get(Calendar.DAY_OF_YEAR);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets a String array of hours in a day
    *** (used as a VComboBox item list)
    *** @param hr24  True for 24 hour clock, false for 12 hour clock
    *** @return A String array of hours in a day
    **/
    public static String[] getHours(boolean hr24)
    {
        String hrs[] = new String[hr24? 24 : 12];
        for (int i = 0; i < hrs.length; i++) {
            hrs[i] = String.valueOf(i);
        }
        return hrs;
    }

    /**
    *** Gets a String array of minutes in a day
    *** (used as a VComboBox item list)
    *** @return A String array of minutes in a day
    **/
    public static String[] getMinutes()
    {
        String min[] = new String[60];
        for (int i = 0; i < min.length; i++) {
            min[i] = String.valueOf(i);
        }
        return min;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Returns a String representation of the specified epoch time seconds
    *** @param timeSec  The Epoch time seconds
    *** @return The String representation
    **/
    public static String toString(long timeSec)
    {
        return (new java.util.Date(timeSec * 1000L)).toString();
    }

    /**
    *** Returns the current Epoch time in seconds since January 1, 1970 00:00:00 GMT
    *** @return The current Epoch time in seconds
    **/
    public static long getCurrentTimeSec()
    {
        // Number of seconds since the 'epoch' January 1, 1970, 00:00:00 GMT
        return getCurrentTimeMillis() / 1000L;
    }

    /**
    *** Returns the current Epoch time in milliseconds since January 1, 1970 00:00:00 GMT
    *** @return The current Epoch time in milliseconds
    **/
    public static long getCurrentTimeMillis()
    {
        // Number of milliseconds since the 'epoch' January 1, 1970, 00:00:00 GMT
        return System.currentTimeMillis();
    }
    
    /**
    *** Returns true if the specified time is within the specified lapsed time
    *** @param timeSec  An Epoch time in seconds
    *** @param lapseSec The time duration to test
    *** @return True if the specified time is within the specified lapsed time
    **/
    public static boolean isRecentSec(long timeSec, long lapseSec)
    {
        Print.logDebug("timeSec: " + timeSec);
        Print.logDebug("getCurrentTimeSec: " + DateTime.getCurrentTimeSec());
        return (timeSec >= (DateTime.getCurrentTimeSec() - lapseSec));
    }

    // ------------------------------------------------------------------------

    /** 
    *** Gets the minimum acceptable DateTime
    *** @return The minimum acceptable DateTime
    **/
    public static DateTime getMinDate()
    {
        return DateTime.getMinDate(null);
    }

    /** 
    *** Gets the minimum acceptable DateTime
    *** @param tz  The TimeZone (not that it really matters)
    *** @return The minimum acceptable DateTime
    **/
    public static DateTime getMinDate(TimeZone tz)
    {
        return new DateTime(1, tz);
    }

    /** 
    *** Gets the maximum acceptable DateTime
    *** @return The maximum acceptable DateTime
    **/
    public static DateTime getMaxDate()
    {
        return DateTime.getMaxDate(null);
    }
    
    /** 
    *** Gets the maximum acceptable DateTime
    *** @param tz  The TimeZone (not that it really matters)
    *** @return The maximum acceptable DateTime
    **/
    public static DateTime getMaxDate(TimeZone tz)
    {
        return new DateTime(tz, 2050, 12, 31);
    }

    // ------------------------------------------------------------------------

    /**
    *** Class/Structure which holds date/time information
    **/
    public static class ParsedDateTime
    {
        public TimeZone timeZone    = null;
        public long     epoch       = -1L;
        public int      year        = -1;
        public int      month1      = -1;
        public int      day         = -1;
        public int      hour        = -1;
        public int      minute      = -1;
        public int      second      = -1;
        public ParsedDateTime(TimeZone tz, long epoch) { 
            this.timeZone = tz;
            this.epoch = epoch; 
        }
        public ParsedDateTime(TimeZone tz, int year, int month1, int day) { 
            this.timeZone = tz;
            this.year     = year; 
            this.month1   = month1; 
            this.day      = day; 
        }
        public ParsedDateTime(TimeZone tz, int year, int month1, int day, int hour, int minute, int second) { 
            this(tz, year, month1, day);
            this.hour     = hour; 
            this.minute   = minute; 
            this.second   = second; 
        }
        public String toString() {
            StringBuffer sb = new StringBuffer();
            if (this.epoch >= 0L) {
                sb.append(this.epoch);
            } else {
                sb.append(StringTools.format(this.year  ,"%04d")).append("/");
                sb.append(StringTools.format(this.month1,"%02d")).append("/");
                sb.append(StringTools.format(this.day   ,"%02d"));
                if (this.hour >= 0) {
                    sb.append(",");
                    sb.append(StringTools.format(this.hour  ,"%02d")).append(":");
                    sb.append(StringTools.format(this.minute,"%02d"));
                    if (this.second >= 0) {
                        sb.append(":");
                        sb.append(StringTools.format(this.second,"%02d"));
                    }
                }
            }
            return sb.toString();
        }
        public DateTime createDateTime() {
            // only valid for years beyond 1970
            if (this.epoch >= 0L) {
                return new DateTime(this.epoch, this.timeZone);
            } else {
                return new DateTime(this.timeZone, this.year, this.month1, this.day, this.hour, this.minute, this.second);
            }
        }
    }
    
    /**
    *** Parse specified String into a ParsedDateTime instance.
    *** This method parses a date which has been provided as an argument to a command-line tool
    *** or in a URL request string.
    *** Allowable Date Formats:<br>
    *** YYYY/MM[/DD[/hh[:mm[:ss]]]][,ZZZ]<br>
    *** YYYY/MM/DD,hh[:mm[:ss]]][,ZZZ]<br>
    *** EEEEEEEE[,ZZZ]<br>
    *** @param dateStr  The date String to parse
    *** @param dftTZ    The default TimeZone
    *** @param isToDate True to default to an end-of-day time if the time is not specified
    *** @return The parsed ParsedDateTime instance, or null if unable to parse the specified date/time string
    *** @throws DateParseException if an invalid date/time format was specified.
    **/
    public static ParsedDateTime parseDateTime(String dateStr, TimeZone dftTZ, boolean isToDate)
        throws DateParseException
    {
        
        /* no date string specified? */
        if (StringTools.isBlank(dateStr)) {
            throw new DateParseException("'dateStr' argtument is null/empty");
        }
        
        /* parse fields */
        String dateGrp[] = StringTools.parseString(dateStr, ",");
        int grpLen = dateGrp.length;
        if ((dateGrp.length < 1) || (dateGrp.length > 3)) {
            throw new DateParseException("Invalid number of ',' separated groups");
        }

        /* extract timezone (if any) */
        TimeZone timeZone = dftTZ;
        if ((dateGrp[grpLen - 1].length() > 0) && Character.isLetter(dateGrp[grpLen - 1].charAt(0))) {
            timeZone = DateTime.getTimeZone(dateGrp[grpLen - 1], null);
            if (timeZone == null) {
                throw new DateParseException("Invalid TimeZone: " + dateGrp[grpLen - 1]);
            }
            grpLen--;
        }

        /* parse date/time fields */
        String dateFld[] = null;
        if (grpLen == 1) {
            // EEEEEEEE
            // YYYY/MM[/DD[/hh[:mm[:ss]]]]
            String d[] = StringTools.parseString(dateGrp[0], "/:");
            if ((d.length < 1) || (d.length > 6)) {
                throw new DateParseException("Invalid Date/Time specification: " + dateGrp[0]);
            } else
            if (d.length == 1) {
                // EEEEEEEE
                long epoch = StringTools.parseLong(d[0], DateTime.getCurrentTimeSec());
                return new ParsedDateTime(timeZone, epoch);
            } else {
                dateFld = d; // (length >= 2) && (length <= 6)
            }
        } else
        if (grpLen == 2) {
            // YYYY/MM/DD,hh[:mm[:ss]]]]
            String d[] = StringTools.parseString(dateGrp[0], "/:");
            String t[] = StringTools.parseString(dateGrp[1], "/:");
            if (d.length != 3) {
                throw new DateParseException("Invalid Date specification: " + dateGrp[0]);
            } else
            if ((t.length < 1) || (t.length > 3)) {
                throw new DateParseException("Invalid Time specification: " + dateGrp[1]);
            } else {
                dateFld = new String[d.length + t.length];  // (length >= 3) && (length <= 6)
                System.arraycopy(d, 0, dateFld, 0, d.length);
                System.arraycopy(t, 0, dateFld, d.length, t.length);
            }
        }

        /* evaluate date/time fields */
        DateTime now = new DateTime(timeZone);
        int YY = StringTools.parseInt(dateFld[0], now.getYear());
        int MM = StringTools.parseInt(dateFld[1], now.getMonth1());
        int DD = 0;
        int hh = isToDate? 23 : 0; // default to end of day
        int mm = isToDate? 59 : 0; // default to end of hour
        int ss = isToDate? 59 : 0; // default to end of minute
        if (dateFld.length >= 3) {
            // at least YYYY/MM/DD provided
            DD = StringTools.parseInt(dateFld[2], now.getDayOfMonth());
            if (dateFld.length >= 4) { hh = StringTools.parseInt(dateFld[3], hh); }
            if (dateFld.length >= 5) { mm = StringTools.parseInt(dateFld[4], mm); }
            if (dateFld.length >= 6) { ss = StringTools.parseInt(dateFld[5], ss); }
        } else {
            // only YYYY/MM provided
            DD = isToDate? DateTime.getDaysInMonth(timeZone, MM, YY) : 1;
        }

        /* return new ParsedDateTime instance */
        return new ParsedDateTime(timeZone, YY, MM, DD, hh, mm, ss);

    }

    /**
    *** Parse specified String into a DateTime instance.
    *** This method parses a date which has been provided as an argument to a command-line tool
    *** or in a URL request string.
    *** Allowable Date Formats:<br>
    *** YYYY/MM[/DD[/hh[:mm[:ss]]]][,ZZZ]<br>
    *** YYYY/MM/DD,hh[:mm[:ss]]][,ZZZ]<br>
    *** EEEEEEEE[,ZZZ]<br>
    *** @param dateStr  The date String to parse
    *** @param dftTZ    The default TimeZone
    *** @param isToDate True to default to an end-of-day time if the time is not specified
    *** @return The parsed DateTime instance, or null if unable to parse the specified date/time string
    *** @throws DateParseException if an invalid date/time format was specified.
    **/
    public static DateTime parseArgumentDate(String dateStr, TimeZone dftTZ, boolean isToDate)
        throws DateParseException
    {
        return DateTime.parseDateTime(dateStr, dftTZ, isToDate).createDateTime();
    }

    // ------------------------------------------------------------------------

    /** 
    *** Returns the day number of days since October 15, 1582 (the first day of the Gregorian Calendar)
    *** @param year   The year [1582..4000]
    *** @param month1 The month [1..12]
    *** @param day    The day [1..31]
    *** @return The number of days since October 15, 1582.
    **/
    public static long getDayNumberFromDate(int year, int month1, int day)
    {
        long yr = ((long)year * 1000L) + (long)(((month1 - 3) * 1000) / 12);
        return ((367L * yr + 625L) / 1000L) - (2L * (yr / 1000L))
               + (yr / 4000L) - (yr / 100000L) + (yr / 400000L)
               + (long)day - 578042L; // October 15, 1582, beginning of Gregorian Calendar
    }

    /** 
    *** Returns the day number of days since October 15, 1582 (the first day of the Gregorian Calendar)
    **/
    public static long getDayNumberFromDate(ParsedDateTime pdt)
    {
        if (pdt == null) {
            return 0L;
        } else
        if (pdt.epoch >= 0L) {
            DateTime dt = pdt.createDateTime();
            return DateTime.getDayNumberFromDate(dt.getYear(), dt.getMonth1(), dt.getDayOfMonth());
        } else {
            return DateTime.getDayNumberFromDate(pdt.year, pdt.month1, pdt.day);
        }
    }

    /** 
    *** Returns the day number of days since October 15, 1582 (the first day of the Gregorian Calendar)
    **/
    public static long getDayNumberFromDate(DateTime dt)
    {
        if (dt == null) {
            return 0L;
        } else {
            return DateTime.getDayNumberFromDate(dt.getYear(), dt.getMonth1(), dt.getDayOfMonth());
        }
    }

    /** 
    *** Returns the year/month/day from the day number (days since October 15, 1582)
    *** @param dayNumber  The number of days since October 15, 1582
    *** @return ParsedDateTime structure containing the year/month/day.
    **/
    public static ParsedDateTime getDateFromDayNumber(long dayNumber)
    {
        long N      = dayNumber + 578042L;
        long C      = ((N * 1000L) - 200L) / 36524250L;
        long N1     = N + C - (C / 4L);
        long Y1     = ((N1 * 1000L) - 200L) / 365250L;
        long N2     = N1 - ((365250L * Y1) / 1000L);
        long M1     = ((N2 * 1000L) - 500L) / 30600L;
        int  day    = (int)(((N2 * 1000L) - (30600L * M1) + 500L) / 1000L);
        int  month1 = (int)((M1 <= 9L)? (M1 + 3L) : (M1 - 9L));
        int  year   = (int)((M1 <= 9L)? Y1 : (Y1 + 1));
        return new ParsedDateTime(null, year, month1, day);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private TimeZone timeZone   = null;
    private long     timeMillis = 0L; // ms since January 1, 1970, 00:00:00 GMT

    /**
    *** Default constructor.  
    *** Initialize with the current Epoch time.
    **/
    public DateTime()
    {
        this.setTimeMillis(getCurrentTimeMillis());
    }

    /**
    *** Constructor.  
    *** Initialize with the current Epoch time.
    *** @param tz  The TimeZone
    **/
    public DateTime(TimeZone tz)
    {
        this();
        this.setTimeZone(tz);
    }

    /**
    *** Constructor.  
    *** @param date  The Date object used to initialize this DateTime
    **/
    public DateTime(Date date)
    {
        this.setTimeMillis(date.getTime());
    }

    /**
    *** Constructor.  
    *** @param date  The Date object used to initialize this DateTime
    *** @param tz  The TimeZone
    **/
    public DateTime(Date date, TimeZone tz)
    {
        this.setTimeMillis(date.getTime());
        this.setTimeZone(tz);
    }
    
    /**
    *** Constructor.  
    *** @param tz      The TimeZone
    *** @param year    The year
    *** @param month1  The 1-based month index [1..12]
    *** @param day     The day
    **/
    public DateTime(TimeZone tz, int year, int month1, int day)
    {
        this.setDate(tz, year, month1, day);
    }
    
    /**
    *** Constructor.  
    *** @param tz      The TimeZone
    *** @param year    The year
    *** @param month1  The 1-based month index [1..12]
    *** @param day     The day
    *** @param hour24  The hour of the day [24 hour clock]
    *** @param minute  The minute of the hour
    *** @param second  The second of the minute
    **/
    public DateTime(TimeZone tz, int year, int month1, int day, int hour24, int minute, int second)
    {
        this.setDate(tz, year, month1, day, hour24, minute, second);
    }

    /**
    *** Constructor.  
    *** @param timeSec  The Epoch time in seconds
    **/
    public DateTime(long timeSec)
    {
        if (timeSec > 3000000000L) { // 1168537360L
            this.setTimeMillis(timeSec);
        } else {
            this.setTimeSec(timeSec);
        }
    }

    /**
    *** Constructor.  
    *** @param timeSec  The Epoch time in seconds
    *** @param tz       The TimeZone
    **/
    public DateTime(long timeSec, TimeZone tz)
    {
        this(timeSec);
        this.setTimeZone(tz);
    }

    /**
    *** Constructor.  
    *** @param d  A date/time String representation
    **/
    public DateTime(String d)
        throws DateParseException
    {
        this.setDate(d);
    }
    
    /**
    *** Copy constructor
    *** @param dt  Another DateTime instance 
    **/
    public DateTime(DateTime dt)
    {
        this.timeMillis = dt.timeMillis;
        this.timeZone   = dt.timeZone;
    }
    
    /**
    *** Copy constructor with delta offset time
    *** @param dt  Another DateTime instance 
    *** @param deltaOffsetSec  +/- offset from time specified in DateTime instance
    **/
    public DateTime(DateTime dt, long deltaOffsetSec)
    {
        this.timeMillis = dt.timeMillis + (deltaOffsetSec * 1000L);
        this.timeZone   = dt.timeZone;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Gets a Date object based on the time in this DateTime object
    *** @return A Date instance
    **/
    public java.util.Date getDate()
    {
        return new java.util.Date(this.getTimeMillis());
    }

    /**
    *** Sets the current time of this instance
    *** @param tz      The TimeZone
    *** @param year    The year
    *** @param month1  The 1-based month index [1..12]
    *** @param day     The day
    **/
    public void setDate(TimeZone tz, int year, int month1, int day)
    {
        this.setDate(tz, year, month1, day, 12, 0, 0);
    }

    /**
    *** Sets the current time of this instance
    *** @param tz      The TimeZone
    *** @param year    The year
    *** @param month1  The 1-based month index [1..12]
    *** @param day     The day
    *** @param hour24  The hour of the day [24 hour clock]
    *** @param minute  The minute of the hour
    *** @param second  The second of the minute
    **/
    public void setDate(TimeZone tz, int year, int month1, int day, int hour24, int minute, int second)
    {
        int month0 = month1 - 1;
        this.setTimeZone(tz);
        Calendar cal = new GregorianCalendar(this._timeZone(tz));
        cal.set(year, month0, day, hour24, minute, second);
        Date date = cal.getTime();
        this.setTimeMillis(date.getTime());
    }

    /**
    *** Sets the current time of this instance
    *** @param d  The String representation of a date/time
    **/
    public void setDate(String d)
        throws DateParseException
    {
        this.setDate(d, (TimeZone)null);
    }

    /**
    *** Sets the current time of this instance
    *** @param d  The String representation of a date/time
    *** @param dftTMZ The TimeZone used if no timezone was parsed from the String
    **/
    public void setDate(String d, TimeZone dftTMZ)
        throws DateParseException
    {
        String ds[] = StringTools.parseString(d, " _");
        String dt = (ds.length > 0)? ds[0] : "";
        String tm = (ds.length > 1)? ds[1] : "";
        String tz = (ds.length > 2)? ds[2] : "";
        // Valid format: "YYYY/MM/DD [HH:MM:SS] [PST]"
        //Print.logInfo("Parsing " + dt + " " + tm + " " + tz);

        /* time-zone */
        TimeZone timeZone = null;
        if (ds.length > 2) {
            if (tz.equals("+0000") || tz.equals("-0000")) {
                timeZone = DateTime.getGMTTimeZone();
            } else
            if (DateTime.isValidTimeZone(tz)) {
                timeZone = DateTime.getTimeZone(tz);
            } else {
                throw new DateParseException("Invalid TimeZone: " + tz);
            }
        } else
        if ((ds.length > 1) && DateTime.isValidTimeZone(tm)) {
            tz = tm;
            tm = "";
            timeZone = DateTime.getTimeZone(tz);
        } else {
            timeZone = (dftTMZ != null)? dftTMZ : this.getTimeZone();
        }
        //Print.logInfo("Timezone = " + timeZone);
        this.setTimeZone(timeZone);
        Calendar calendar = new GregorianCalendar(timeZone);
        
        /* date */
        int yr = -1, mo = -1, dy = -1;
        int d1 = dt.indexOf('/'), d2 = (d1 > 0)? dt.indexOf('/', d1 + 1) : -1;
        if ((d1 > 0) && (d2 > d1)) {
            
            /* year */
            String YR = dt.substring(0, d1);
            yr = StringTools.parseInt(YR, -1);
            if ((yr >=  0) && (yr <= 49)) { yr += 2000; } else
            if ((yr >= 50) && (yr <= 99)) { yr += 1900; }
            if ((yr < 1900) || (yr > 2100)) {
                throw new DateParseException("Date/Year out of range: " + YR);
            }
            
            /* month */
            String MO = dt.substring(d1 + 1, d2);
            mo = StringTools.parseInt(MO, -1) - 1; // 0 indexed
            if ((mo < 0) || (mo > 11)) {
                throw new DateParseException("Date/Month out of range: " + MO);
            }
            
            /* seconds */
            String DY = dt.substring(d2 + 1);
            dy = StringTools.parseInt(DY, -1);
            if ((dy < 1) || (dy > 31)) {
                throw new DateParseException("Date/Day out of range: " + DY);
            }
            
        } else {
            
            throw new DateParseException("Invalid date format (Y/M/D): " + dt);
            
        }
        
        /* time */
        if (tm.equals("")) {
            
            //Print.logInfo("Just using YMD:" + yr + "/" + mo + "/" + dy);
            calendar.set(yr, mo, dy);
            
        } else {
            
            int hr = -1, mn = -1, sc = -1;
            int t1 = tm.indexOf(':'), t2 = (t1 > 0)? tm.indexOf(':', t1 + 1) : -1;
            if (t1 > 0) {
                
                /* hour */
                String HR = tm.substring(0, t1);
                hr = StringTools.parseInt(HR, -1);
                if ((hr < 0) || (hr > 23)) {
                    throw new DateParseException("Time/Hour out of range: " + HR);
                }
                if (t2 > t1) {
                    
                    /* minute */
                    String MN = tm.substring(t1 + 1, t2);
                    mn = StringTools.parseInt(MN, -1);
                    if ((mn < 0) || (mn > 59)) {
                        throw new DateParseException("Time/Minute out of range: " + MN);
                    }
                    
                    /* second */
                    String SC = tm.substring(t2 + 1);
                    sc = StringTools.parseInt(SC, -1);
                    if ((sc < 0) || (sc > 59)) {
                        throw new DateParseException("Time/Second out of range: " + SC);
                    }
                    
                    //Print.logInfo("Setting YMDHMS:" + yr + "/" + mo + "/" + dy+ " " + hr + ":" + mn + ":" + sc);
                    calendar.set(yr, mo, dy, hr, mn, sc);
                    
                } else {
                    
                    /* minute */
                    String MN = tm.substring(t1 + 1);
                    mn = StringTools.parseInt(MN, -1);
                    if ((mn < 0) || (mn > 59)) {
                        throw new DateParseException("Time/Minute out of range: " + MN);
                    }
                    
                    //Print.logInfo("Setting YMDHM:" + yr + "/" + mo + "/" + dy+ " " + hr + ":" + mn);
                    calendar.set(yr, mo, dy, hr, mn);
                    
                }
            } else {
                
                throw new DateParseException("Invalid time format (H:M:S): " + tm);
                
            }
        }
        
        /* ok */
        this.setTimeMillis(calendar.getTime().getTime());
        
    }

    /**
    *** Does not work, do not use
    **/
    private boolean setParsedDate_formatted(String d) // does not work!
    {
        try {
            java.util.Date date = DateFormat.getDateInstance().parse(d);
            this.setTimeMillis(date.getTime());
            return true;
        } catch (java.text.ParseException pe) {
            Print.logStackTrace("Unable to parse date: " + d, pe);
            this.setTimeMillis(0L);
            return false;
        }
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Gets a value from a GregorianCalendar based on the Epoch time of this instance
    *** @param tz    The TimeZone
    *** @param value The value number to return
    **/
    private int _get(TimeZone tz, int value)
    {
        return this.getCalendar(tz).get(value);
    }

    /**
    *** Gets a GregorianCalendar calendar instance
    *** @param tz  The TimeZone
    *** @return The Calendar object
    **/
    public Calendar getCalendar(TimeZone tz)
    {
        Calendar c = new GregorianCalendar(this._timeZone(tz));
        c.setTimeInMillis(this.getTimeMillis());
        return c;
    }

    /**
    *** Gets a GregorianCalendar calendar instance
    *** @return The Calendar object
    **/
    public Calendar getCalendar()
    {
        return this.getCalendar(null);
    }
    
    /**
    *** Gets the 0-based index of the month represented by this DateTime instance
    *** @param tz  The TimeZone used when calculating the 0-based month index
    *** @return The 0-based month index
    **/
    public int getMonth0(TimeZone tz)
    {
        // return 0..11
        return this._get(tz, Calendar.MONTH); // 0 indexed
    }

    /**
    *** Gets the 0-based index of the month represented by this DateTime instance
    *** @return The 0-based month index
    **/
    public int getMonth0()
    {
        return this.getMonth0(null);
    }

    /**
    *** Gets the 1-based index of the month represented by this DateTime instance
    *** @param tz  The TimeZone used when calculating the 0-based month index
    *** @return The 1-based month index
    **/
    public int getMonth1(TimeZone tz)
    {
        // return 1..12
        return this.getMonth0(tz) + 1;
    }

    /**
    *** Gets the 1-based index of the month represented by this DateTime instance
    *** @return The 1-based month index
    **/
    public int getMonth1()
    {
        return this.getMonth1(null);
    }

    /**
    *** Gets the day of the month represented by this DateTime instance
    *** @param tz  The TimeZone used when calculating the day of the month
    *** @return The day of the month
    **/
    public int getDayOfMonth(TimeZone tz)
    {
        // return 1..31
        return this._get(tz, Calendar.DAY_OF_MONTH);
    }

    /**
    *** Gets the day of the month represented by this DateTime instance
    *** @return The day of the month
    **/
    public int getDayOfMonth()
    {
        return this.getDayOfMonth(null);
    }

    /**
    *** Gets the number of days in the month represented by this DateTime instance
    *** @param tz  The TimeZone used when calculating the number of days in the month
    *** @return The number of days in the month
    **/
    public int getDaysInMonth(TimeZone tz)
    {
        return DateTime.getDaysInMonth(tz, this.getMonth1(tz), this.getYear(tz));
    }

    /**
    *** Gets the number of days in the month represented by this DateTime instance
    *** @return The number of days in the month
    **/
    public int getDaysInMonth()
    {
        return this.getDaysInMonth(null);
    }

    /**
    *** Returns the day of week for this DateTime instance
    *** @param tz  The TimeZone used when calculating the day of week
    *** @return The day of week (0=Sunday ... 6=Saturday)
    **/
    public int getDayOfWeek(TimeZone tz)
    {
        // return 0..6
        return this._get(tz, Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;
    }

    /**
    *** Returns the day of week for this DateTime instance
    *** @return The day of week (0=Sunday ... 6=Saturday)
    **/
    public int getDayOfWeek()
    {
        return this.getDayOfWeek(null);
    }
   
    /**
    *** Returns the year for this DataTime instance
    *** @param tz  The TimeZone used when calculating the year
    *** @return The year.
    **/
    public int getYear(TimeZone tz)
    {
        return this._get(tz, Calendar.YEAR);
    }
   
    /**
    *** Returns the year for this DataTime instance
    *** @return The year.
    **/
    public int getYear()
    {
        return this.getYear(null);
    }

    /** 
    *** Returns true if this DateTime instance represents a leap-year
    *** @param tz  The TimeZone used when calculating the leap-year
    *** @return True if the year is a leap-year, false otherwise
    **/
    public boolean isLeapYear(TimeZone tz)
    {
        GregorianCalendar gc = (GregorianCalendar)this.getCalendar(tz);
        return gc.isLeapYear(gc.get(Calendar.YEAR));
    }
 
    /** 
    *** Returns true if this DateTime instance represents a leap-year
    *** @return True if the year is a leap-year, false otherwise
    **/
    public boolean isLeapYear()
    {
        return this.isLeapYear(null);
    }

    /**
    *** Returns the hour of the day (24-hour clock)
    *** @param tz  The TimeZone used when calculating the hour of the day
    *** @return The hour of the day
    **/
    public int getHour24(TimeZone tz)
    {
        return this._get(tz, Calendar.HOUR_OF_DAY);
    }

    /**
    *** Returns the hour of the day (24-hour clock)
    *** @return The hour of the day
    **/
    public int getHour24()
    {
        return this.getHour24(null);
    }

    /**
    *** Returns the hour of the day (12-hour clock)
    *** @param tz  The TimeZone used when calculating the hour of the day
    *** @return The hour of the day
    **/
    public int getHour12(TimeZone tz)
    {
        return this._get(tz, Calendar.HOUR);
    }

    /**
    *** Returns the hour of the day (12-hour clock)
    *** @return The hour of the day
    **/
    public int getHour12()
    {
        return this.getHour12(null);
    }

    /**
    *** Returns true if AM, false if PM
    *** @param tz  The TimeZone used when calculating AM/PM
    *** @return True if AM, false if PM
    **/
    public boolean isAM(TimeZone tz)
    {
        return this._get(tz, Calendar.AM_PM) == Calendar.AM;
    }

    /**
    *** Returns true if AM, false if PM
    *** @return True if AM, false if PM
    **/
    public boolean isAM()
    {
        return this.isAM(null);
    }

    /**
    *** Returns true if PM, false if AM
    *** @param tz  The TimeZone used when calculating AM/PM
    *** @return True if PM, false if AM
    **/
    public boolean isPM(TimeZone tz)
    {
        return this._get(tz, Calendar.AM_PM) == Calendar.PM;
    }

    /**
    *** Returns true if PM, false if AM
    *** @return True if PM, false if AM
    **/
    public boolean isPM()
    {
        return this.isPM(null);
    }

    /**
    *** Returns the minute of the hour
    *** @param tz  The TimeZone used when calculating the minute
    *** @return The minute of the hour
    **/
    public int getMinute(TimeZone tz)
    {
        return this._get(tz, Calendar.MINUTE);
    }

    /**
    *** Returns the minute of the hour
    *** @return The minute of the hour
    **/
    public int getMinute()
    {
        return this.getMinute(null);
    }

    /**
    *** Returns the second of the minute
    *** @param tz  The TimeZone used when calculating the second
    *** @return The second of the minute
    **/
    public int getSecond(TimeZone tz)
    {
        return this._get(tz, Calendar.SECOND);
    }

    /**
    *** Returns the second of the minute
    *** @return The second of the minute
    **/
    public int getSecond()
    {
        return this.getSecond(null);
    }

    /**
    *** Returns true if this DateTime instance currently represents a daylight savings time.
    *** @param tz  The TimeZone used when calculating daylight savings time.
    *** @return True if this DateTime instance currently represents a daylight savings time.
    **/
    public boolean isDaylightSavings(TimeZone tz)
    {
        return _timeZone(tz).inDaylightTime(this.getDate());
    }

    /**
    *** Returns true if this DateTime instance currently represents a daylight savings time.
    *** @return True if this DateTime instance currently represents a daylight savings time.
    **/
    public boolean isDaylightSavings()
    {
        return this.isDaylightSavings(null);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the Epoch time in seconds represented by this instance
    *** @return The Epoch time in seconds
    **/
    public long getTimeSec()
    {
        return this.getTimeMillis() / 1000L;
    }

    /** 
    *** Sets the Epoch time in seconds represented by this instance
    *** @param timeSec  Epoch time in seconds
    **/
    public void setTimeSec(long timeSec)
    {
        this.timeMillis = timeSec * 1000L;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the Epoch time in milliseconds represented by this instance
    *** @return The Epoch time in milliseconds
    **/
    public long getTimeMillis()
    {
        return this.timeMillis;
    }

    /** 
    *** Sets the Epoch time in milliseconds represented by this instance
    *** @param timeMillis  Epoch time in milliseconds
    **/
    public void setTimeMillis(long timeMillis)
    {
        this.timeMillis = timeMillis;
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Returns an Epoch time in seconds which is the beginning of the day
    *** represented by this instance.
    *** @param tz  The TimeZone
    *** @return An Epoch time which is at the beginning of the day represented
    ***         by this instance.
    **/
    public long getDayStart(TimeZone tz)
    {
        if (tz == null) { tz = _timeZone(tz); }
        Calendar c  = this.getCalendar(tz);
        Calendar nc = new GregorianCalendar(tz);
        nc.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        return nc.getTime().getTime() / 1000L;
    }
    
    /**
    *** Returns an Epoch time in seconds which is the beginning of the day
    *** represented by this instance.
    *** @return An Epoch time which is at the beginning of the day represented
    ***         by this instance.
    **/
    public long getDayStart()
    {
        return this.getDayStart(null);
    }
    
    /**
    *** Returns an Epoch time in seconds which is the end of the day
    *** represented by this instance.
    *** @param tz  The TimeZone
    *** @return An Epoch time which is at the end of the day represented
    ***         by this instance.
    **/
    public long getDayEnd(TimeZone tz)
    {
        if (tz == null) { tz = _timeZone(tz); }
        Calendar c  = this.getCalendar(tz);
        Calendar nc = new GregorianCalendar(tz);
        nc.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH), 23, 59, 59);
        return nc.getTime().getTime() / 1000L;
    }
    
    /**
    *** Returns an Epoch time in seconds which is the end of the day
    *** represented by this instance.
    *** @return An Epoch time which is at the end of the day represented
    ***         by this instance.
    **/
    public long getDayEnd()
    {
        return this.getDayEnd(null);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the Epoch timestamp representing the beginning of the month
    *** represented by this DateTime instance, plus the specified delta month offset.
    *** @param tz  The overriding TimeZone
    *** @param deltaMo  The delta monnths (added to the month represented by this DateTime instance)
    *** @return The Epoch timestamp
    **/
    public long getMonthStart(TimeZone tz, int deltaMo)
    {
        if (tz == null) { tz = _timeZone(tz); }
        Calendar c  = this.getCalendar(tz);
        int YY = c.get(Calendar.YEAR);
        int MM = c.get(Calendar.MONTH); // 0..11
        if (deltaMo != 0) {
            MM += deltaMo;
            if (MM < 0) {
                YY += (MM - 11) / 12;
                MM %= 12;
                if (MM < 0) {
                    MM += 12;
                }
            } else {
                YY += MM / 12;
                MM %= 12;
            }
        }
        int DD = 1;
        Calendar nc = new GregorianCalendar(tz);
        nc.set(YY, MM, DD, 0, 0, 0);
        return nc.getTime().getTime() / 1000L;
    }

    /**
    *** Returns the Epoch timestamp representing the beginning of the month
    *** represented by this DateTime instance.
    *** @param tz  The overriding TimeZone
    *** @return The Epoch timestamp
    **/
    public long getMonthStart(TimeZone tz)
    {
        return this.getMonthStart(tz, 0);
    }

    /**
    *** Returns the Epoch timestamp representing the beginning of the month
    *** represented by this DateTime instance, plus the specified delta month offset.
    *** @param deltaMo  The delta monnths (added to the month represented by this DateTime instance)
    *** @return The Epoch timestamp
    **/
    public long getMonthStart(int deltaMo)
    {
        return this.getMonthStart(null, deltaMo);
    }

    /**
    *** Returns the Epoch timestamp representing the beginning of the month
    *** represented by this DateTime instance.
    *** @return The Epoch timestamp
    **/
    public long getMonthStart()
    {
        return this.getMonthStart(null, 0);
    }

    /**
    *** Returns the Epoch timestamp representing the end of the month
    *** represented by this DateTime instance, plus the specified delta month offset.
    *** @param tz  The overriding TimeZone
    *** @param deltaMo  The delta monnths (added to the month represented by this DateTime instance)
    *** @return The Epoch timestamp
    **/
    public long getMonthEnd(TimeZone tz, int deltaMo)
    {
        if (tz == null) { tz = _timeZone(tz); }
        Calendar c  = this.getCalendar(tz);
        int YY = c.get(Calendar.YEAR);
        int MM = c.get(Calendar.MONTH); // 0..11
        if (deltaMo != 0) {
            MM += deltaMo;
            if (MM < 0) {
                YY += (MM - 11) / 12;
                MM %= 12;
                if (MM < 0) {
                    MM += 12;
                }
            } else {
                YY += MM / 12;
                MM %= 12;
            }
        }
        int DD = DateTime.getDaysInMonth(tz, MM + 1, YY);
        Calendar nc = new GregorianCalendar(tz);
        nc.set(YY, MM, DD, 23, 59, 59);
        return nc.getTime().getTime() / 1000L;
    }

    /**
    *** Returns the Epoch timestamp representing the end of the month
    *** represented by this DateTime instance.
    *** @param tz  The overriding TimeZone
    *** @return The Epoch timestamp
    **/
    public long getMonthEnd(TimeZone tz)
    {
        return this.getMonthEnd(tz, 0);
    }

    /**
    *** Returns the Epoch timestamp representing the end of the month
    *** represented by this DateTime instance, plus the specified delta month offset.
    *** @param deltaMo  The delta monnths (added to the month represented by this DateTime instance)
    *** @return The Epoch timestamp
    **/
    public long getMonthEnd(int deltaMo)
    {
        return this.getMonthEnd(null, deltaMo);
    }

    /**
    *** Returns the Epoch timestamp representing the end of the month
    *** represented by this DateTime instance.
    *** @return The Epoch timestamp
    **/
    public long getMonthEnd()
    {
        return this.getMonthEnd(null, 0);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the Epoch timestamp representing the start of the current day represented
    *** by this DateTime instance, based on the GMT TimeZone.
    *** @return The Epoch timestamp
    **/
    public long getDayStartGMT()
    {
        // GMT TimeZone
        return (this.getTimeSec() / SECONDS_PER_DAY) * SECONDS_PER_DAY;
    }

    /**
    *** Returns the Epoch timestamp representing the end of the current day represented
    *** by this DateTime instance, based on the GMT TimeZone.
    *** @return The Epoch timestamp
    **/
    public long getDayEndGMT()
    {
        // GMT TimeZone
        return this.getDayStartGMT() + SECONDS_PER_DAY - 1L;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified DateTime is <b>after</b> this DateTime instance
    *** @param dt the other DateTime instance
    *** @return True if the specified DateTime is <b>after</b> this DateTime instance
    **/
    public boolean after(DateTime dt)
    {
        return this.after(dt, false);
    }

    /**
    *** Returns true if the specified DateTime is <b>after</b> this DateTime instance
    *** @param dt the other DateTime instance
    *** @param inclusive  True to test for "equal-to or after", false to test only for "after".
    *** @return True if the specified DateTime is after this DateTime instance (or "equal-to" if
    ***         'inclusive' is true).
    **/
    public boolean after(DateTime dt, boolean inclusive)
    {
        if (dt == null) {
            return true; // arbitrary
        } else
        if (inclusive) {
            return (this.getTimeMillis() >= dt.getTimeMillis());
        } else {
            return (this.getTimeMillis() > dt.getTimeMillis());
        }
    }

    /**
    *** Returns true if the specified DateTime is <b>before</b> this DateTime instance
    *** @param dt the other DateTime instance
    *** @return True if the specified DateTime is <b>before</b> this DateTime instance
    **/
    public boolean before(DateTime dt)
    {
        return this.before(dt, false);
    }

    /**
    *** Returns true if the specified DateTime is <b>before</b> this DateTime instance
    *** @param dt the other DateTime instance
    *** @param inclusive  True to test for "equal-to or before", false to test only for "before".
    *** @return True if the specified DateTime is before this DateTime instance (or "equal-to" if
    ***         'inclusive' is true).
    **/
    public boolean before(DateTime dt, boolean inclusive)
    {
        if (dt == null) {
            return false; // arbitrary
        } else
        if (inclusive) {
            return (this.getTimeMillis() <= dt.getTimeMillis());
        } else {
            return (this.getTimeMillis() < dt.getTimeMillis());
        }
    }
    
    /**
    *** Returns true if the specified DateTime is <b>equal-to</b> this DateTime instance
    *** @param obj the other DateTime instance
    *** @return True if the specified DateTime is <b>equal-to</b> this DateTime instance
    **/
    public boolean equals(Object obj) 
    {
        if (obj instanceof DateTime) {
            return (this.getTimeMillis() == ((DateTime)obj).getTimeMillis());
        } else {
            return false;
        }
    }
    
    /**
    *** Compares another DateTime instance to this instance.
    *** @param other  The other DateTime instance.
    *** @return &lt;0 of the other DateTime instance is before this instance, 0 if the other DateTime
    ***         instance is equal to this instance, and &gt;0 if the other DateTime instance is
    ***         after this instance.
    **/
    public int compareTo(Object other)
    {
        if (other instanceof DateTime) {
            long otherTime = ((DateTime)other).getTimeMillis();
            long thisTime  = this.getTimeMillis();
            if (thisTime < otherTime) { return -1; }
            if (thisTime > otherTime) { return  1; }
            return 0;
        } else {
            return -1;
        }
    }
    
    // ------------------------------------------------------------------------

    /**
    *** TimeZoneProvider interface
    **/
    public interface TimeZoneProvider
    {
        public TimeZone getTimeZone();
    }

    /**
    *** Returns a non-null TimeZone
    *** @param tz The TimeZone returned (if non-null)
    *** @return The specified TimeZone, or the default TimeZone if the specified TimeZone is null
    **/
    protected TimeZone _timeZone(TimeZone tz)
    {
        return (tz != null)? tz : this.getTimeZone();
    }

    /**
    *** Returns the default (or current) TimeZone
    *** @return The default (or current) TimeZone
    **/
    public TimeZone getTimeZone()
    {
        return (this.timeZone != null)? this.timeZone : DateTime.getDefaultTimeZone();
    }

    /**
    *** Returns the ID of the current TimeZone
    *** @return The ID of the current TimeZone
    **/
    public String getTimeZoneID()
    {
        return this.getTimeZone().getID();
    }

    /**
    *** Returns the short-name of the current TimeZone
    *** @return The short-name of the current TimeZone
    **/
    public String getTimeZoneShortName()
    {
        boolean dst = this.isDaylightSavings();
        return this.getTimeZone().getDisplayName(dst, TimeZone.SHORT);
    }
    
    /**
    *** Sets the current TimeZone
    *** @param tz  The TimeZone to set
    **/
    public void setTimeZone(TimeZone tz)
    {
        this.timeZone = tz;
    }
    
    /**
    *** Sets the current TimeZone
    *** @param tz  The TimeZone ID to set
    **/
    public void setTimeZone(String tz)
    {
        this.setTimeZone(DateTime.getTimeZone(tz, null));
    }

    /**
    *** Reads the available TimeZone IDs from the specified file.
    *** @param tmzFile  The file from which TimeZone IDs are read.
    *** @return A String array of read TimeZone IDs.
    **/
    public static String[] readTimeZones(File tmzFile)
    {
        if ((tmzFile != null) && tmzFile.exists()) {
            java.util.List<String> tzList = new Vector<String>();
            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader(tmzFile));
                for (;;) {
                    String rline = br.readLine();
                    if (rline == null) { break; }
                    String tline = rline.trim();
                    if (!tline.equals("") && !tline.startsWith("#")) {
                        tzList.add(tline);
                    }
                }
                return (String[])tzList.toArray(new String[tzList.size()]);
            } catch (IOException ioe) {
                Print.logError("Unable to read file: " + tmzFile + " [" + ioe + "]");
                return null;
            } finally {
                if (br != null) { try { br.close(); } catch (IOException ioe) {/*ignore*/} }
            }
        }
        return null;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified TimeZone is valid
    *** @param tzid  The String representation of a TimeZone
    *** @return True if the TimeZone is valid, false otherwise
    **/
    public static boolean isValidTimeZone(String tzid)
    {
        if ((tzid == null) || tzid.equals("")) {
            return false;
        } else
        if (tzid.equalsIgnoreCase("GMT") || 
            tzid.equalsIgnoreCase("UTC") || 
            tzid.equalsIgnoreCase("Zulu")) {
            return true;
        } else {
            // "TimeZone.getTimeZone" returns GMT for invalid timezones
            TimeZone tmz = TimeZone.getTimeZone(tzid);
            if (tmz.getRawOffset() != 0) { // ie. !(GMT+0)
                return true; // must be a valid time-zone
            } else {
                return false; // GMT+0 (must be invalid)
            }
            // TimeZone understands more than what is listed in the available IDs
            // so this check may end up failing valid time-zones.
            //String tz[] = TimeZone.getAvailableIDs();
            //for (int i = 0; i < tz.length; i++) {
            //    if (tz[i].equals(tzid)) { return true; }
            //}
            //return false;
        }
    }

    /**
    *** Gets the TimeZone instance for the specified TimeZone id
    *** @param tzid  The TimeZone id
    *** @param dft   The default TimeZone to return if the specified TimeZone id
    ***              does not exist.
    *** @return The TimZone
    **/
    public static TimeZone getTimeZone(String tzid, TimeZone dft)
    {
        if ((tzid == null) || tzid.equals("")) {
            return dft;
        } else
        if (tzid.equalsIgnoreCase("GMT") || 
            tzid.equalsIgnoreCase("UTC") || 
            tzid.equalsIgnoreCase("Zulu")) {
            return TimeZone.getTimeZone(tzid);
        } else {
            // "TimeZone.getTimeZone" returns GMT for invalid timezones
            TimeZone tmz = TimeZone.getTimeZone(tzid);
            if (tmz.getRawOffset() != 0) { // ie. !(GMT+0)
                return tmz; // must be a valid time-zone
            } else {
                return dft; // GMT+0 (must be invalid)
            }
        }
    }

    /**
    *** Gets the TimeZone instance for the specified TimeZone id
    *** @param tzid  The TimeZone id
    *** @return The TimZone
    **/
    public static TimeZone getTimeZone(String tzid)
    {
        if ((tzid == null) || tzid.equals("")) {
            return TimeZone.getDefault(); // local system default time-zone
        } else {
            // 'TimeZone' will return GMT if an invalid name is specified
            return TimeZone.getTimeZone(tzid);
        }
    }

    /**
    *** Returns the default TimeZone
    *** @return The default TimeZone
    **/
    public static TimeZone getDefaultTimeZone()
    {
        return DateTime.getTimeZone(null);
    }

    /**
    *** Returns the GMT TimeZone
    *** @return The GMT TimeZone
    **/
    public static TimeZone getGMTTimeZone()
    {
        return TimeZone.getTimeZone(GMT_TIMEZONE);
    }

    // ------------------------------------------------------------------------

    private static SimpleDateFormat simpleFormatter = null;

    /**
    *** Returns a String representation of this DateTime instance
    **/
    public String toString() 
    {
        if (simpleFormatter == null) {
            // eg. "Sun Mar 26 12:38:12 PST 2006"
            simpleFormatter = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US);
        }
        synchronized (simpleFormatter) {
            simpleFormatter.setTimeZone(this.getTimeZone());
            return simpleFormatter.format(this.getDate());
        }
    }

    // ------------------------------------------------------------------------

    /** 
    *** Formats the specified Date instance.
    *** @param date The Date instance
    *** @param tz   The TimeZone
    *** @param fmt  The Date/Time format
    *** @return The String representation of the StringBuffer destination.
    **/
    public static String format(java.util.Date date, TimeZone tz, String fmt)
    {
        StringBuffer sb = null;
        SimpleDateFormat sdf = new SimpleDateFormat(fmt);
        sdf.setTimeZone((tz != null)? tz : DateTime.getDefaultTimeZone()); 
        if (sb == null) { sb = new StringBuffer(); }
        sdf.format(date, sb, new FieldPosition(0));
        return sb.toString();
    }

    /** 
    *** Formats the current DateTime instance.
    *** @param fmt  The Date/Time format
    *** @param tz   The overriding TimeZone
    *** @param sb   The StringBuffer where the formatted Date/Time is placed (may be null)
    *** @return The String representation of the StringBuffer destination.
    **/
    public String format(String fmt, TimeZone tz, StringBuffer sb)
    {
        if (fmt == null) { fmt = "yyyy/MM/dd HH:mm:ss"; }
        SimpleDateFormat sdf = new SimpleDateFormat(fmt);
        sdf.setTimeZone(this._timeZone(tz)); 
        if (sb == null) { sb = new StringBuffer(); }
        sdf.format(this.getDate(), sb, new FieldPosition(0));
        return sb.toString();
    }

    /** 
    *** Formats the current DateTime instance.
    *** @param fmt  The Date/Time format
    *** @param tz   The overriding TimeZone
    *** @return The formatted Date/Time String
    **/
    public String format(String fmt, TimeZone tz)
    {
        return this.format(fmt, tz, null);
    }

    /** 
    *** Formats the current DateTime instance (using format "MMM dd, yyyy HH:mm:ss z")
    *** @param tz   The overriding TimeZone
    *** @return The formatted Date/Time String
    **/
    public String format(TimeZone tz)
    {   // ie. "Oct 22, 2003 7:23:18 PM"
        return this.format("MMM dd, yyyy HH:mm:ss z", tz, null);
        //DateFormat dateFmt = DateFormat.getDateTimeInstance();
        //dateFmt.setTimeZone(tz);
        //return dateFmt.format(new java.util.Date(this.getTimeMillis()));
    }

    /** 
    *** Formats the current DateTime instance.
    *** @param fmt  The Date/Time format
    *** @return The formatted Date/Time String
    **/
    public String format(String fmt)
    {
        return this.format(fmt, null, null);
    }

    /** 
    *** Formats the current DateTime instance, based on the GMT TimeZone.
    *** @param fmt  The Date/Time format
    *** @return The formatted Date/Time String
    **/
    public String gmtFormat(String fmt)
    {
        return this.format(fmt, DateTime.getGMTTimeZone(), null);
    }

    /** 
    *** Formats the current DateTime instance (using format "yyyy/MM/dd HH:mm:ss 'GMT'"), based on
    *** the GMT TimeZone.
    *** @return The formatted Date/Time String
    **/
    public String gmtFormat()
    {
        return this.gmtFormat("yyyy/MM/dd HH:mm:ss 'GMT'");
    }

    /** 
    *** Formats the current DateTime instance, based on the GMT TimeZone.
    *** @param fmt  The Date/Time format
    *** @param sb   The StringBuffer where the formatted Date/Time is placed (may be null)
    *** @return The String representation of the StringBuffer destination.
    **/
    public String gmtFormat(String fmt, StringBuffer sb)
    {
        return this.format(fmt, DateTime.getGMTTimeZone(), sb);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a clone of this DateTime instance
    *** @return A clone of this DateTime instance
    **/
    public Object clone()
    {
        return new DateTime(this);
    }
    
    // ------------------------------------------------------------------------
    
    /**
    *** Formats the time-of-day, based on the specified format.
    *** @param tod  The time-of-day
    *** @param fmt  The format
    *** @return The formatted time-of-day
    **/
    protected static String encodeHourMinuteSecond(long tod, String fmt)
    {
        StringBuffer sb = new StringBuffer();
        int h = (int)(tod / (60L * 60L)), m = (int)((tod / 60L) % 60), s = (int)(tod % 60);
        if (fmt != null) {
            // format: "00:00:00", "00:00"
            String f[] = StringTools.parseString(fmt, ':');
            if (f.length > 0) { sb.append(StringTools.format(h,f[0])); }              // hours
            if (f.length > 1) { sb.append(':').append(StringTools.format(m,f[1])); }  // minutes
            if (f.length > 2) { sb.append(':').append(StringTools.format(s,f[2])); }  // seconds
        } else {
            sb.append(h);
            sb.append(':').append(StringTools.format(m,"00"));
            if (s > 0) {
                sb.append(':').append(StringTools.format(s,"00"));
            }
        }
        return sb.toString();
    }

    /**
    *** Parses the time-of-day from the specified String
    *** @param hms  The String containing the time-of-day to parse
    *** @return The time-of-day
    **/
    protected static int parseHourMinuteSecond(String hms)
    {
        return parseHourMinuteSecond(hms, 0);
    }

    /**
    *** Parses the time-of-day from the specified String
    *** @param hms  The String containing the time-of-day to parse
    *** @param dft  The default time-of-day returned if unable to parse the specified String.
    *** @return The time-of-day
    **/
    protected static int parseHourMinuteSecond(String hms, int dft)
    {
        String a[] = StringTools.parseString(hms,":");
        if (a.length <= 1) {
            // assume all seconds
            return StringTools.parseInt(hms, dft);
        } else
        if (a.length == 2) {
            // assume hh:mm
            int h = StringTools.parseInt(a[0], -1);
            int m = StringTools.parseInt(a[1], -1);
            return ((h >= 0) && (m >= 0))? (((h * 60) + m) * 60) : dft;
        } else { // (a.length >= 3)
            // assume hh:mm:ss
            int h = StringTools.parseInt(a[0], -1);
            int m = StringTools.parseInt(a[1], -1);
            int s = StringTools.parseInt(a[2], -1);
            return ((h >= 0) && (m >= 0) && (s >= 0))? ((((h * 60) + m) * 60) + s) : dft;
        }
    }

    // ------------------------------------------------------------------------

    private static String mainTMZ[] = { "US/Pacific" , "GMT", "GMT-00:00", "GMT-01:00" };

    /**
    *** Main entry point for testing/debugging
    *** @param argv Comand-line arguments
    **/
    public static void main(String argv[])
    {
        
        /* precheck for current epoch time */
        if (argv.length > 0) {
            if (argv[0].equals("-now_sec")) {
                System.out.println(String.valueOf(DateTime.getCurrentTimeSec()));
                System.exit(0);
            } else 
            if (argv[0].equals("-now_ms")) {
                System.out.println(String.valueOf(DateTime.getCurrentTimeMillis()));
                System.exit(0);
            } else
            if (argv[0].startsWith("-compileTime")) {
                int p = argv[0].indexOf("=");
                String pkg = "";
                DateTime now = new DateTime();
                StringBuffer sb = new StringBuffer();
                if (p > 0) {
                    sb.append("package "+argv[0].substring(p+1)+";\n");
                }
                sb.append("public class CompileTime {\n");
                sb.append("    public static final long COMPILE_TIMESTAMP = " + now.getTimeSec() + "L; // " + now + "\n");
                sb.append("}\n");
                System.out.println(sb.toString());
                System.exit(0);
            }
        }

        /* command-line args */
        RTConfig.setCommandLineArgs(argv);

        /* All available TimeZones */
        if (RTConfig.getBoolean(new String[]{"tzlist","timezones"},false)) {
            String tzid[] = (String[])ListTools.sort(TimeZone.getAvailableIDs(), null);
            for (int i = 0; i < tzid.length; i++) {
                TimeZone tz       = TimeZone.getTimeZone(tzid[i]);
                String id         = tz.getID();
                String name       = tz.getDisplayName();
                String shortName  = tz.getDisplayName(false, TimeZone.SHORT);
                String longName   = tz.getDisplayName(false, TimeZone.LONG);
                //String testName   = TimeZone.getTimeZone(shortName).getDisplayName(false, TimeZone.SHORT);
                Print.sysPrintln(tzid[i] + "[" +id + "]: " + shortName + " / " + longName); // + " [" + testName + "]");
            }
            System.exit(0);
        }

        /* Read TimeZones from file */
        File tmzFile = RTConfig.getFile("tmzfile",null);
        if (tmzFile != null) {
            long nowTime = DateTime.getCurrentTimeSec();
            String TMZ_NAME[] = DateTime.readTimeZones(tmzFile);
            for (int i = 0; i < TMZ_NAME.length; i++) {
                String tzname    = StringTools.leftJustify(TMZ_NAME[i], 28);
                TimeZone tz      = TimeZone.getTimeZone(TMZ_NAME[i]);
                String idName    = tz.getID().equals(TMZ_NAME[i])? "*" : tz.getID();
                String shortName = StringTools.leftJustify(tz.getDisplayName(false, TimeZone.SHORT), 5);
                String longName  = tz.getDisplayName(false, TimeZone.LONG);
                int rawOfsMIN    = tz.getOffset(nowTime) / (1000 * 60);
                int rawOfsHH     = Math.abs(rawOfsMIN) / 60;
                int rawOfsMM     = Math.abs(rawOfsMIN) % 60;
                String gmtStr    = "GMT"+((rawOfsMIN>=0)?"+":"-")+StringTools.format(rawOfsHH,"00")+":"+StringTools.format(rawOfsMM,"00");
                Print.sysPrintln(tzname+" ["+gmtStr+"]: " + idName + " , " + shortName + " , " + longName);
            }
            System.exit(0);
        }

        /* TimeZone */
        String tmz = RTConfig.getString("tmz",null);
        if ((tmz != null) && !tmz.equals("")) {
            long nowTime = DateTime.getCurrentTimeSec();
            TimeZone tz = TimeZone.getTimeZone(tmz);
            int rawOfsMIN  = (tz.getOffset(nowTime) + tz.getDSTSavings()) / (1000 * 60);
            int rawOfsHH   = Math.abs(rawOfsMIN) / 60;
            int rawOfsMM   = Math.abs(rawOfsMIN) % 60;
            String gmtStr  = "GMT"+((rawOfsMIN>=0)?"+":"-")+StringTools.format(rawOfsHH,"00")+":"+StringTools.format(rawOfsMM,"00");
            Print.sysPrintln(tmz + " [" + gmtStr + "]: " + tz);
            Print.sysPrintln("Time : " + (new DateTime(nowTime,tz)));
            Print.sysPrintln("GMT  : " + (new DateTime(nowTime,DateTime.getGMTTimeZone())));
            System.exit(0);
        }

        /* time zones */
        String tzStr   = RTConfig.getString("tz",null);
        TimeZone tz    = null;
        if (StringTools.isBlank(tzStr)) {
            tz = DateTime.getDefaultTimeZone();
        } else {
            tz = DateTime.getTimeZone(tzStr);
        }
        TimeZone pstTZ = DateTime.getTimeZone("US/Pacific");
        TimeZone gmtTZ = DateTime.getGMTTimeZone();

        /* time */
        Print.sysPrintln("");
        DateTime nowDT = new DateTime(tz);
        if (RTConfig.hasProperty("time")) {
            String stime = RTConfig.getString("time",null);
            try {
                nowDT = DateTime.parseArgumentDate(stime, tz, false);
            } catch (DateParseException dpe) {
                Print.logError("Unable to parse date: " + stime + " [" + dpe.getMessage() + "]");
            }
        }
        long now = nowDT.getTimeSec();

        /* format */
        String fmt = RTConfig.getString("format",null);

        /* display dates */
        DateTime gmtNowDt = new DateTime(now,gmtTZ);
        DateTime pstNowDt = new DateTime(now,pstTZ);
        long     pstDayN  = DateTime.getDayNumberFromDate(pstNowDt);
        DateTime pstStrDt = new DateTime(pstNowDt.getDayStart(),pstTZ);
        DateTime pstEndDt = new DateTime(pstNowDt.getDayEnd(),pstTZ);
        DateTime tmzNowDt = nowDT;
        Print.sysPrintln("GMT time  : " + gmtNowDt + " [" + now + ":0x" + StringTools.toHexString(now,32) + "]");
        Print.sysPrintln("TZ  time  : " + tmzNowDt);
        Print.sysPrintln("PST time  : " + pstNowDt);
      //Print.sysPrintln("PST ftime : " + pstNowDt.format("yyyy/MM/dd HH:mm:ss z"));
      //Print.sysPrintln("PST ftime : " + pstNowDt.format("yyyy/MM/dd HH:mm:ss zzzz"));
      //Print.sysPrintln("PST ftime : " + pstNowDt.format("yyyy/MM/dd HH:mm:ss Z"));
        Print.sysPrintln("PST Day#  : " + pstDayN + " [" + ((pstDayN + 5) % 7) + "]");
        Print.sysPrintln("PST start : " + pstStrDt + " [" + pstStrDt.getTimeSec() + "]");
        Print.sysPrintln("PST end   : " + pstEndDt + " [" + pstEndDt.getTimeSec() + "]");
        if (!StringTools.isBlank(fmt)) {
        Print.sysPrintln("Formatted : " + nowDT.format(fmt));
        }

    }
    
}
