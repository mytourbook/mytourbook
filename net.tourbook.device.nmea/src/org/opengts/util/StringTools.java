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
//  This class provides many String based utilities.
// ----------------------------------------------------------------------------
// Change History:
//  2006/03/26  Martin D. Flynn
//     -Initial release
//  2006/04/11  Martin D. Flynn
//     -Decimal formatting now explicitly uses "Locale.US" symbols.  This fixes 
//      a problem that caused values such as Latitude "37,1234" from appearing 
//      in CSV files.
//  2006/05/11  Martin D. Flynn
//     -Replaced deprecated 'Character.isSpace' with 'Character.isWhitespace'
//  2006/06/30  Martin D. Flynn
//     -Repackaged
//  2007/02/25  Martin D. Flynn
//     -Added 'isDouble', 'isFloat', 'isInt', 'isLong', 'isBoolean'
//     -Fixed negative case in 'parseBoolean'.
//     -Removed "on"/"off" from valid boolean string values.
//  2007/07/27  Martin D. Flynn
//     -Added support for encoding primitive arrays
//  2007/09/16  Martin D. Flynn
//     -Fixed 'insertKeyValues' to properly account for the length of the beginning
//      and ending delimiter lengths.
//  2008/02/04  Martin D. Flynn
//     -Added 'parseFloat'/'parseDouble' method for decoding 32-bit and 64-bit 
//      IEEE 754 floating-point values.
//     -Validate 'blockLen' argument in 'formatHexString'.
//  2008/02/27  Martin D. Flynn
//     -Added date formatting to 'format(...)'
//  2008/03/28  Martin D. Flynn
//     -Added additional support for character encoding
//  2008/04/11  Martin D. Flynn
//     -Added method 'endsWithIgnoreCase'
//  2008/05/14  Martin D. Flynn
//     -Added methods 'escapeUnicode', 'unescapeUnicode', 'trim', 'isBlank'
//  2008/06/20  Martin D. Flynn
//     -Added support for 'String.format(...)'
//  2008/07/20  Martin D. Flynn
//     -Added key/value separator option to 'parseProperties'
//  2008/07/27  Martin D. Flynn
//     -Added 'isDouble', 'isFloat', 'isInt', 'isLong', 'isBoolean' methods that 
//      accept an 'Object' data type.
//  2009/01/01  Martin D. Flynn
//     -Added 'isAlphaNumeric'
//  2009/01/28  Martin D. Flynn
//     -Changed 'insertKeyValues' to allow recursive variable replacement within 
//      default replacement value.
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.text.*;
import java.math.*;
import java.awt.*;

import java.lang.reflect.Array;

/**
*** Provides various String parsing/format utilities
**/

public class StringTools
{

    // ------------------------------------------------------------------------

    public static final char   BACKSPACE                = '\b';
    public static final char   FORM_FEED                = '\f';
    public static final char   NEW_LINE                 = '\n';
    public static final char   CARRIAGE_RETURN          = '\r';
    public static final char   TAB                      = '\t';
    
    public static final String WhitespaceChars          = " \t\b\f\r\n";

    public static final char   KeyValSeparatorChar      = '='; // "="
    public static final char   PropertySeparatorChar    = ' '; // ';'

    // ------------------------------------------------------------------------

    private static final String BooleanTRUE[]           = { "true" , "yes", "on" , "1" }; // must be lower-case
    private static final String BooleanFALSE[]          = { "false", "no" , "off", "0" }; // must be lower-case

    // ------------------------------------------------------------------------

    public  static final String FORMAT_TIME             = "time";

    // ------------------------------------------------------------------------

    public  static final String CharEncoding_UTF_8      = "UTF-8";
    public  static final String CharEncoding_UTF_16     = "UTF-16";
    public  static final String CharEncoding_8859_1     = "ISO-8859-1";

  //private static final String DefaultCharEncoding     = CharEncoding_8859_1;

    // ------------------------------------------------------------------------

    //private static String charEncoding = DefaultCharEncoding;
    
    //**
    //*** Sets the character encoding used to encode/decode Strings
    //*** @param charEnc  The character encoding
    //**/
    //public static void setCharacterEncoding(String charEnc)
    //{
    //    StringTools.charEncoding = !StringTools.isBlank(charEnc)? charEnc : DefaultCharEncoding;
    //}
    
    /**
    *** Gets the character encoding used to encode/decode Strings
    *** @return  The character encoding
    **/
    public static String getCharacterEncoding()
    {
        try {
            //String charSet = System.getProperty("file.encoding");
            String charSet = java.nio.charset.Charset.defaultCharset().name();
            return !StringTools.isBlank(charSet)? charSet : CharEncoding_UTF_8;
        } catch (Throwable th) {
            Print.logException("Unsupported Character Set?", th);
            return CharEncoding_UTF_8;
        }
    }

    /**
    *** Returns an array of available character encodings
    *** @return The default character set
    **/
    public static String[] getCharacterEncodings()
    {
        Map csMap = java.nio.charset.Charset.availableCharsets();
        String charEnc[] = new String[csMap.size()];
        int c = 0;
        for (Iterator i = csMap.keySet().iterator(); i.hasNext();) {
            charEnc[c++] = (String)i.next();
        }
        return charEnc;
    }

    /**
    *** Returns the default character set 
    *** @return The default character set
    **/
    /*
    private static int CHARSET_SOURCE = 2;
    public static String getDefaultCharacterEncoding()
    {
        // References:
        //  - http://blogs.warwick.ac.uk/kieranshaw/entry/utf-8_internationalisation_with/
        String charSet = null;
        switch (CHARSET_SOURCE) {
            case 0:
                // not cross-plateform safe
                charSet = System.getProperty("file.encoding");
                // Note: Setting this property will not change the default character encoding for 
                // the current Java process.  In order to change the character encoding, this 
                // property must be set on the start-up command line. (IE. "-Dfile.encoding=UTF-8")
                break;
            case 1:
                // JDK1.4
                charSet = new java.io.OutputStreamWriter(new java.io.ByteArrayOutputStream()).getEncoding();
                break;
            case 2:
                // This requires JDK1.5 to compile
                charSet = java.nio.charset.Charset.defaultCharset().name();
                break;
        }
        return charSet;
    }
    */

    // ------------------------------------------------------------------------

    /**
    *** Return a 'char' array of the specified String
    *** @param s  The String from which the char array will be returned
    *** @return The array of 'char's from the specified String
    **/
    public static char[] getChars(String s)
    {
        return (s != null)? s.toCharArray() : null;
    }

    /**
    *** Converts the specified byte array to an array of chars.  
    *** This method converts a single byte to a single character.  Multibyte character
    *** conversions are not supported.
    *** @param b The array of bytes to convert to characters
    *** @return The array of characters created from the byte array
    **/
    public static char[] getChars(byte b[])
    {
        if (b != null) {
            char c[] = new char[b.length];
            for (int i = 0; i < b.length; i++) { 
                c[i] = (char)((int)b[i] & 0xFF); 
            }
            return c;
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Converts the String represented by the specified StringBuffer into a byte
    *** array based on the default character set (see <code>StringTools.getCharacterEncoding()</code>)
    *** @param sb  The StringBuffer which will be converted to a byte array
    *** @return The byte array representation of the StringBuffer
    **/
    public static byte[] getBytes(StringBuffer sb)
    {
        return (sb != null)? getBytes(sb.toString()) : null;
    }

    /**
    *** Converts the specified String into a byte array based on the default character
    *** set (see <code>StringTools.getCharacterEncoding()</code>)
    *** @param s  The String which will be converted to a byte array
    *** @return The byte array representation of the specified String
    **/
    public static byte[] getBytes(String s)
    {
        if (s != null) {
            try {
                return s.getBytes(StringTools.getCharacterEncoding());
            } catch (UnsupportedEncodingException uce) {
                // will not occur
                Print.logStackTrace("Charset not found: " + StringTools.getCharacterEncoding());
                return s.getBytes();
            }
        } else {
            return null;
        }
    }

    /**
    *** Converts the specified char array into a byte array.  Character are converted 
    *** as 1 byte per character.  Multibyte conversions are not supported by this method.
    *** @param c  The char array which will be converted to a byte array
    *** @return The byte array representation of the specified char array
    **/
    public static byte[] getBytes(char c[])
    {
        if (c != null) {
            byte b[] = new byte[c.length];
            for (int i = 0; i < c.length; i++) {
                b[i] = (byte)c[i];
            }
            return b;
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified String contains only alpha-numeric characters
    *** @param s          The String tested for alpha-numeric characters
    *** @param inclSpace  True to include the space (" ") character
    *** @return True if the specified String contains only alpha-numeric characters
    **/
    public static boolean isAlphaNumeric(String s, boolean inclSpace)
    {
        if (s == null) {
            return false;
        } else {
            char ch[] = s.toCharArray();
            for (int i = 0; i < ch.length; i++) {
                if (Character.isLetterOrDigit(ch[i]) ) {
                    // continue;
                } else
                if (inclSpace && (ch[i] == ' ')) {
                    // continue;
                } else {
                    return false;
                }
            }
            // everything checks out
            return true;
        }
    }

    /**
    *** Returns true if the specified byte array contains only printable ASCII characters
    *** @param b          The byte array tested for printable ASCII
    *** @param inclSpace  True to include space characters (' ', '\t', '\n', '\r'), false to omit.
    *** @return True if the specified byte array contains only printable ASCII characters
    **/
    public static boolean isPrintableASCII(byte b[], boolean inclSpace)
    {
        // Printable ASCII has a valid range of 33 ('!') to 126 ('~')
        // Space characters are 9 ('\t'), 10 ('\n'), and 13 ('\r'), and 32 (' ')
        if ((b == null) || (b.length == 0)) {
            return false;
        } else {
            for (int i = 0; i < b.length; i++) {
                if ((b[i] < (byte)33) || (b[i] > 126)) {
                    if (!inclSpace) {
                        // outside of acceptable range, and we're not including space characters
                        return false;
                    } else
                    if ((b[i] != ' ') && (b[i] != '\t') && (b[i] != '\n') && (b[i] != '\r')) {
                        // outside of acceptable range, and not a space character
                        return false;
                    }
                }
            }
            // everything checks out
            return true;
        }
    }

    /**
    *** Converts the specified byte array to a String based on the default character set,
    *** replacing any unprintable characters.
    *** @param b  The byte array to convert to a String, based on the default character set.
    *** @param repUnp The character used to replace any detected unprintable characters.
    *** @return The String representation of the specified byte array
    **/
    public static String toStringValue(byte b[], char repUnp)
    {
        if (b != null) {
            byte p[] = new byte[b.length];
            System.arraycopy(b, 0, p, 0, b.length);
            for (int i = 0; i < p.length; i++) {
                if ((p[i] < 32) || (p[i] > 126)) {
                    p[i] = (byte)repUnp;
                }
            }
            return StringTools.toStringValue(p, 0, p.length);
        } else {
            return null;
        }
    }

    /**
    *** Converts the specified byte array to a String based on the default character set.
    *** @param b  The byte array to convert to a String, based on the default character set.
    *** @return The String representation of the specified byte array
    **/
    public static String toStringValue(byte b[])
    {
        return (b != null)? StringTools.toStringValue(b, 0, b.length) : null;
    }

    /**
    *** Converts the specified byte array to a String based on the default character set.
    *** @param b  The byte array to convert to a String, based on the default character set.
    *** @param ofs  The offset within the byte array to convert to a String
    *** @param len  The number of bytes starting at the specified offset to convert to a String
    *** @return The String representation of the specified byte array
    **/
    public static String toStringValue(byte b[], int ofs, int len)
    {
        if (b != null) {
            try {
                return new String(b, ofs, len, StringTools.getCharacterEncoding());
            } catch (Throwable t) {
                // This should NEVER occur (at least not because of the charset)
                Print.logException("Byte=>String conversion error", t);
                return new String(b, ofs, len);
            }
        } else {
            return null; // what goes around ...
        }
    }

    /**
    *** Converts the specified character array to a String.
    *** @param c  The char array to convert to a String
    *** @return The String representation of the specified char array
    **/
    public static String toStringValue(char c[])
    {
        return new String(c);
    }

    // ------------------------------------------------------------------------

    /**
    *** Converts the specified String to a Unicode escaped String.<br>
    *** That is, convert non-ASCII characters to '\u0000' encoded characters.
    *** @param s  The String to convert to a Unicode encoded String
    *** @return The Unicode encoded String
    **/
    public static String escapeUnicode(String s)
    {
        if (s != null) {
            StringBuffer sb = new StringBuffer();
            int len = s.length();
            for (int i = 0; i < len; i++) {
                char ch = s.charAt(i);
                if ((ch == '\n') || (ch == '\r')) {
                    sb.append(ch);
                } else
                if ((ch == '\t') || (ch == '\f')) {
                    sb.append(ch);
                } else
                if ((ch < 0x0020) || (ch > 0x007e)) {
                    sb.append('\\');
                    sb.append('u');
                    sb.append(StringTools.hexNybble((ch >> 12) & 0xF));
                    sb.append(StringTools.hexNybble((ch >>  8) & 0xF));
                    sb.append(StringTools.hexNybble((ch >>  4) & 0xF));
                    sb.append(StringTools.hexNybble( ch        & 0xF));
                } else {
                    sb.append(ch);
                }
            }
            return sb.toString();
        } else {
            return "";
        }
    }

    /**
    *** Converts the specified String to a Unicode encoded String.<br>
    *** That is, convert unicode '\u0000' escapes characters sequences into the unicode character.
    *** @param u  The Unicode escaped ASCII String to convert to unicode character String
    *** @return The Unicode encoded String
    **/
    public static String unescapeUnicode(String u)
    {
        if (u != null) {
            StringBuffer sb = new StringBuffer();
            int len = u.length();
            for (int i = 0; i < len;) {
                char ch = u.charAt(i);
                if ((ch == '\\') && ((i + 5) < len) && (u.charAt(i+1) == 'u')) {
                    i += 2;
                    int val = 0;
                    for (int x = i; i < x + 4; i++) {
                        int hndx = hexIndex(u.charAt(i));
                        if (hndx < 0) {
                            break;
                        }                        
                        val = (val << 4) | hndx;
                    }
                    sb.append((char)val);
                } else {
                    sb.append(ch);
                    i++;
                }
            }
            return sb.toString();
        } else {
            return "";
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Trims the leading/trailing blanks from the specified String argument.<br>
    *** Similar to the String 'trim()' method with the addition that if the argument
    *** is null, a non-null empty String will be returned.
    *** @param s  The String to trim, or null to return an empty String
    *** @return The trimmed String
    **/
    public static String trim(String s)
    {
        return (s != null)? s.trim() : "";
    }
    
    /**
    *** Returns true if the specified argument is null, or contains 0 or more whitespace characters.
    *** @param s  The String to test for blank.
    *** @return True if the specified argument is blank, or null.
    **/
    public static boolean isBlank(String s)
    {
        return ((s == null) || s.trim().equals(""));
    }
    
    /**
    *** Returns the specified String, is not blank, or the default String if the specifgied String is blank
    *** @param target  The specified target String
    *** @param dft     The default String
    *** @return The target String is not blank, otherwise the default String
    **/
    public static String blankDefault(String target, String dft)
    {
        return StringTools.isBlank(target)? dft : target;
    }

    // ------------------------------------------------------------------------

    private static final char ESCAPE_CHAR = '\\';

    /**
    *** Return the specified String as a Quoted String, using "double-quotes"
    *** @param s The String to quote
    *** @return The quoted String
    **/
    public static String quoteString(String s)
    {
        return StringTools.quoteString(s, '\"');
    }

    /**
    *** Return the specified String as a Quoted String, using the specified quote character
    *** @param s The String to quote
    *** @param q The quote character to use to quote the String
    *** @return The quoted String
    **/
    public static String quoteString(String s, char q)
    {
        if (s == null) { s = ""; }
        int c = 0, len = s.length();
        char ch[] = new char[len];
        s.getChars(0, len, ch, 0);
        StringBuffer qsb = new StringBuffer();
        qsb.append(q);
        for (;c < len; c++) {
            if (ch[c] == q) {
                // TODO: option should be provided to select how literal quotes are to be specified:
                // IE. "\\"" or "\"\""
                qsb.append(ESCAPE_CHAR).append(q); // \\"
            } else
            if (ch[c] == ESCAPE_CHAR) {
                qsb.append(ESCAPE_CHAR).append(ESCAPE_CHAR);
            } else
            if (ch[c] == '\n') {
                qsb.append(ESCAPE_CHAR).append('n');
            } else
            if (ch[c] == '\r') {
                qsb.append(ESCAPE_CHAR).append('r');
            } else
            if (ch[c] == '\t') {
                qsb.append(ESCAPE_CHAR).append('t');
            } else {
                qsb.append(ch[c]);
            }
        }
        qsb.append(q);
        return qsb.toString();
    }

    // ------------------------------------------------------------------------
    // From: http://rath.ca/Misc/Perl_CSV/CSV-2.0.html#csv%20specification
    //   CSV_RECORD     ::= (* FIELD DELIM *) FIELD REC_SEP
    //   FIELD          ::= QUOTED_TEXT | TEXT
    //   DELIM          ::= `,'
    //   REC_SEP        ::= `\n'
    //   TEXT           ::= LIT_STR | ["] LIT_STR [^"] | [^"] LIT_STR ["]
    //   LIT_STR        ::= (* LITERAL_CHAR *)
    //   LITERAL_CHAR   ::= NOT_COMMA_NL
    //   NOT_COMMA_NL   ::= [^,\n]
    //   QUOTED_TEXT    ::= ["] (* NOT_A_QUOTE *) ["]
    //   NOT_A_QUOTE    ::= [^"] | ESCAPED_QUOTE
    //   ESCAPED_QUOTE  ::= `""'

    /**
    *** Quote the specified String based on CSV rules
    *** @param s The String to quote
    *** @return The quotes String
    **/
    public static String quoteCSVString(String s)
    {
        if (s == null) { s = ""; }
        boolean needsQuotes = true; // (s.indexOf(',') >= 0);
        char q = '\"';
        if (s.indexOf(q) >= 0) {
            StringBuffer sb = new StringBuffer();
            if (needsQuotes) { sb.append(q); }
            for (int i = 0; i < s.length(); i++) {
                char ch = s.charAt(i);
                if (ch == q) {
                    sb.append("\"\"");
                } else {
                    sb.append(ch);
                }
            }
            if (needsQuotes) { sb.append(q); }
            return sb.toString();
        } else
        if (needsQuotes) {
            return "\"" + s + "\"";
        } else {
            return s;
        }
    }

    /**
    *** Encode the specified array of Strings based on CSV encoding rules
    *** @param d The array of Strings to encode into a CSV line
    *** @param checkTextQuote Set true to prefix values with a "'" tick (required by Excel?)
    *** @return The encoded CSV line
    **/
    public static String encodeCSV(String d[], boolean checkTextQuote)
    {
        if (d != null) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < d.length; i++) {
                if (sb.length() > 0) { sb.append(","); }
                String v = (d[i] != null)? d[i] : "";
                String t = checkTextQuote? ("'" + v) : v;
                sb.append(StringTools.quoteCSVString(t));
            }
            return sb.toString();
        } else {
            return "";
        }
    }

    /**
    *** Encode the specified array of Strings based on CSV encoding rules
    *** @param d The array of Strings to encode into a CSV line
    *** @return The encoded CSV line
    **/
    public static String encodeCSV(String d[])
    {
        return StringTools.encodeCSV(d, false);
    }

    // ------------------------------------------------------------------------

    /**
    *** Parse the specified quoted String into a non-quoted String
    *** @param s  The quoted String to parse
    *** @return The non-quoted String
    **/
    public static String parseQuote(String s)
    {
        StringBuffer sb = new StringBuffer();
        StringTools.parseQuote(s.toCharArray(), 0, sb);
        return sb.toString();
    }

    /**
    *** Parse the specified character array into a non-quoted String
    *** @param ch  The quoted char array to parse
    *** @param a   The starting index within the char array to begin parsing
    *** @param sb  The destination where the parsed un-quoted String is placed
    *** @return The index of the next character following the parsed String
    **/
    public static int parseQuote(char ch[], int a, StringBuffer sb)
    {
        // Note on escaped octal values:
        //   Java supports octal values specified in Strings
        //   MySQL dump files do not support octal values in strings
        //   Thus, the interpretation of the value "\00" is ambiguous:
        //     - Java  == 0x00
        //     - MySQL == 0x0030
        // 'parseOctal' currently forced to false in order to support MySQL dump files.
        boolean parseOctal = false;

        /* validate args */
        int chLen = (ch != null)? ch.length : 0;
        if ((chLen <= 0) || (a < 0) || (a >= chLen)) {
            return a;
        }

        /* check first character to determine if value is quoted */
        if ((ch[a] == '\"') || (ch[a] == '\'')) { // quoted string
            char quote = ch[a]; // save type of quote

            /* skip past first quote */
            a++; // skip past first quote

            /* parse quoted string */
            for (; (a < chLen) && (ch[a] != quote); a++) {

                /* '\' escaped character? */
                if (((a + 1) < chLen) && (ch[a] == '\\')) {
                    a++; // skip past '\\'

                    /* parse octal values */
                    if (parseOctal) {
                        // look for "\<octal>" values
                        int n = a;
                        for (;(n < chLen) && (n < (a + 3)) && (ch[n] >= '0') && (ch[n] <= '8'); n++);
                        if (n > a) {
                            String octalStr = new String(ch, a, (n - a));
                            try {
                                int octal = Integer.parseInt(octalStr, 8) & 0xFF;
                                sb.append((char)octal);
                            } catch (NumberFormatException nfe) {
                                // highly unlikely, since we pre-qualified the parsed value
                                Print.logStackTrace("Unable to parse octal: " + octalStr);
                                //sb.append("?");
                            }
                            a = n - 1; // reset a to last character of octal value
                            continue;
                        }
                    }

                    /* check for specific filtered characters */
                    if (ch[a] == '0') { // "\0" (this is the only 'octal' value that is allowed
                        sb.append((char)0);
                    } else
                    if (ch[a] == 'r') { // "\r"
                        sb.append('\r'); // ch[a]);
                    } else
                    if (ch[a] == 'n') { // "\n"
                        sb.append('\n'); // ch[a]);
                    } else
                    if (ch[a] == 't') { // "\t"
                        sb.append('\t'); // ch[a]);
                    } else {
                        sb.append(ch[a]);
                    }

                } else {

                    /* standard unfiltered characters */
                    sb.append(ch[a]);

                }
            }

            /* skip past last quote */
            if (a < chLen) { a++; } // skip past last quote

        } else {

            /* break at first whitespace */
            for (;(a < chLen) && !Character.isWhitespace(ch[a]); a++) {
                sb.append(ch[a]);
            }

        }
        return a;
    }

    // ------------------------------------------------------------------------

    /**
    *** Parse the specified object into a Number value
    *** @param data  The object to parse
    *** @param dft   The default Number value if unable to parse the specified object
    *** @return The parsed Number value
    **/
    public static <T> Number parseNumber(Object data, Class<?> numberClass, Number dft)
    {
        if (data == null) {
            return dft;
        } else
        if ((numberClass == null) || !Number.class.isAssignableFrom(numberClass)) {
            return dft;
        } else
        if (numberClass.isAssignableFrom(data.getClass())) {
            return (Number)data;
        } else {
            FilterNumber num = new FilterNumber(data.toString(), numberClass);
            if (!num.supportsType(numberClass)) {
                return dft;
            } else {
                return num.toNumber(dft);
            }
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Parse the specified byte array, representing a IEEE 754 floating-point into a double value
    *** @param b  The byte array to parse
    *** @param ofs The offset within the byte array to begin parsing
    *** @param isBigEndian  True if the IEEE 754 with the byte array in in BigEndian order
    *** @param dft The default double returned if unable to parse a double value
    *** @return The parsed IEEE 754 double value
    **/
    public static double parseDouble(byte b[], int ofs, boolean isBigEndian, double dft)
    {
        
        /* valid byte array */
        if ((b == null) || ((ofs + 8) > b.length)) {
            return dft;
        }
        
        /* parse IEEE 754 double */
        int i = ofs;
        long doubleLong = 0L;
        if (isBigEndian) {
            doubleLong = 
                (((long)b[i+0] & 0xFF) << (7*8)) + 
                (((long)b[i+1] & 0xFF) << (6*8)) + 
                (((long)b[i+2] & 0xFF) << (5*8)) + 
                (((long)b[i+3] & 0xFF) << (4*8)) + 
                (((long)b[i+4] & 0xFF) << (3*8)) + 
                (((long)b[i+5] & 0xFF) << (2*8)) + 
                (((long)b[i+6] & 0xFF) << (1*8)) + 
                 ((long)b[i+7] & 0xFF);
        } else {
            doubleLong = 
                (((long)b[i+7] & 0xFF) << (7*8)) + 
                (((long)b[i+6] & 0xFF) << (6*8)) + 
                (((long)b[i+5] & 0xFF) << (5*8)) + 
                (((long)b[i+4] & 0xFF) << (4*8)) + 
                (((long)b[i+3] & 0xFF) << (3*8)) + 
                (((long)b[i+2] & 0xFF) << (2*8)) + 
                (((long)b[i+1] & 0xFF) << (1*8)) + 
                 ((long)b[i+0] & 0xFF);
        }
        return Double.longBitsToDouble(doubleLong);

    }

    /**
    *** Parse the specified object into a double value
    *** @param data  The object to parse
    *** @param dft   The default double value if unable to parse the specified object
    *** @return The parsed double value
    **/
    public static double parseDouble(Object data, double dft)
    {
        if (data == null) {
            return dft;
        } else
        if (data instanceof Number) {
            return ((Number)data).doubleValue();
        } else {
            return StringTools.parseDouble(data.toString(), dft);
        }
    }

    /**
    *** Parse the specified String into a double value
    *** @param data  The String to parse
    *** @param dft   The default double value if unable to parse the specified object
    *** @return The parsed double value
    **/
    public static double parseDouble(String data, double dft)
    {
        return StringTools.parseDouble(new FilterNumber(data, Double.class), dft);
    }

    /**
    *** Parse the specified FilterNumber into a double value
    *** @param num   The FilterNumber to parse
    *** @param dft   The default double value if unable to parse the specified object
    *** @return The parsed double value
    **/
    public static double parseDouble(FilterNumber num, double dft)
    {
        if ((num != null) && num.supportsType(Double.class)) {
            try {
                return Double.parseDouble(num.getValueString());
            } catch (NumberFormatException nfe) {
                // ignore
            }
        }
        return dft;
    }

    /**
    *** Return true if the specified String contains a valid double value
    *** @param data  The String to test
    *** @param strict True to test for a strict double value (ie. does not contain
    ***               any other superfluous trailing characters), false to allow for 
    ***               other non-critical trailing characters.
    *** @return True if the specified String contains a valid double value
    **/
    public static boolean isDouble(String data, boolean strict)
    {
        if (StringTools.isBlank(data)) {
            return false;
        } else {
            FilterNumber fn = new FilterNumber(data, Double.class);
            return fn.isValid(strict);
        }
    }

    /**
    *** Return true if the specified Object contains a valid double value
    *** @param data   The Object to test
    *** @param strict True to test for a strict double value (ie. does not contain
    ***               any other superfluous trailing characters), false to allow for 
    ***               other non-critical trailing characters.
    *** @return True if the specified String contains a valid double value
    **/
    public static boolean isDouble(Object data, boolean strict)
    {
        if (data == null) {
            return false;
        } else
        if (data instanceof Number) {
            return strict? (data instanceof Double) : true;
        } else {
            return StringTools.isDouble(data.toString(), strict);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Parse the specified byte array, representing a IEEE 754 floating-point into a float value
    *** @param b  The byte array to parse
    *** @param ofs The offset within the byte array to begin parsing
    *** @param isBigEndian  True if the IEEE 754 with the byte array in in BigEndian order
    *** @param dft The default float returned if unable to parse a float value
    *** @return The parsed IEEE 754 float value
    **/
    public static float parseFloat(byte b[], int ofs, boolean isBigEndian, float dft)
    {
        
        /* valid byte array */
        if ((b == null) || ((ofs + 4) > b.length)) {
            return dft;
        }
        
        /* parse IEEE 754 float */
        int i = ofs;
        int floatInt = 0;
        if (isBigEndian) {
            floatInt = 
                (((int)b[i+0] & 0xFF) << (3*8)) + 
                (((int)b[i+1] & 0xFF) << (2*8)) + 
                (((int)b[i+2] & 0xFF) << (1*8)) + 
                 ((int)b[i+3] & 0xFF);
        } else {
            floatInt = 
                (((int)b[i+3] & 0xFF) << (3*8)) + 
                (((int)b[i+2] & 0xFF) << (2*8)) + 
                (((int)b[i+1] & 0xFF) << (1*8)) + 
                 ((int)b[i+0] & 0xFF);
        }
        return Float.intBitsToFloat(floatInt);

    }
    
    /**
    *** Parse the specified object into a float value
    *** @param data  The object to parse
    *** @param dft   The default float value if unable to parse the specified object
    *** @return The parsed float value
    **/
    public static float parseFloat(Object data, float dft)
    {
        if (data == null) {
            return dft;
        } else
        if (data instanceof Number) {
            return ((Number)data).floatValue();
        } else {
            return StringTools.parseFloat(data.toString(), dft);
        }
    }

    /**
    *** Parse the specified String into a float value
    *** @param data  The String to parse
    *** @param dft   The default float value if unable to parse the specified object
    *** @return The parsed float value
    **/
    public static float parseFloat(String data, float dft)
    {
        return StringTools.parseFloat(new FilterNumber(data, Float.class), dft);
    }

    /**
    *** Parse the specified FilterNumber into a float value
    *** @param num  The FilterNumber to parse
    *** @param dft   The default float value if unable to parse the specified object
    *** @return The parsed float value
    **/
    public static float parseFloat(FilterNumber num, float dft)
    {
        if ((num != null) && num.supportsType(Float.class)) {
            try {
                return Float.parseFloat(num.getValueString());
            } catch (NumberFormatException nfe) {
                // ignore
            }
        }
        return dft;
    }

    /**
    *** Return true if the specified String contains a valid float value
    *** @param data   The String to test
    *** @param strict True to test for a strict float value (ie. does not contain
    ***               any other superfluous trailing characters), false to allow for 
    ***               other non-critical trailing characters.
    *** @return True if the specified String contains a valid float value
    **/
    public static boolean isFloat(String data, boolean strict)
    {
        if (StringTools.isBlank(data)) {
            return false;
        } else {
            FilterNumber fn = new FilterNumber(data, Float.class);
            return fn.isValid(strict);
        }
    }

    /**
    *** Return true if the specified Object contains a valid float value
    *** @param data   The Object to test
    *** @param strict True to test for a strict float value (ie. does not contain
    ***               any other superfluous trailing characters), false to allow for 
    ***               other non-critical trailing characters.
    *** @return True if the specified String contains a valid float value
    **/
    public static boolean isFloat(Object data, boolean strict)
    {
        if (data == null) {
            return false;
        } else
        if (data instanceof Number) {
            return strict? (data instanceof Float) : true;
        } else {
            return StringTools.isFloat(data.toString(), strict);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Parse the specified object into a long value
    *** @param data  The object to parse
    *** @param dft   The default long value if unable to parse the specified object
    *** @return The parsed long value
    **/
    public static long parseLong(Object data, long dft)
    {
        if (data == null) {
            return dft;
        } else
        if (data instanceof Number) {
            return ((Number)data).longValue();
        } else
        if (data instanceof DateTime) {
            return ((DateTime)data).getTimeSec();
        } else {
            return StringTools.parseLong(data.toString(), dft);
        }
    }

    /**
    *** Parse the specified String into a long value
    *** @param data  The String to parse
    *** @param dft   The default long value if unable to parse the specified object
    *** @return The parsed long value
    **/
    public static long parseLong(String data, long dft)
    {
        return StringTools.parseLong(new FilterNumber(data, Long.class), dft);
    }

    /**
    *** Parse the specified FilterNumber into a long value
    *** @param num  The FilterNumber to parse
    *** @param dft  The default long value if unable to parse the specified object
    *** @return The parsed long value
    **/
    public static long parseLong(FilterNumber num, long dft)
    {
        if ((num != null) && num.supportsType(Long.class)) {
            if (num.isHex()) {
                return StringTools.parseHexLong(num.getValueString(), dft);
            } else {
                try {
                    return Long.parseLong(num.getValueString());
                } catch (NumberFormatException nfe) {
                    // Since 'FilterNumber' makes sure that only digits are parsed,
                    // this likely means that the specified digit string is too large
                    // for this required data type.  Our last ditch effort is to
                    // attempt to convert it to a BigInteger and extract the lower
                    // number of bits to match our data type.
                    BigInteger bigLong = parseBigInteger(num, null);
                    if (bigLong != null) {
                        return bigLong.longValue();
                    }
                }
            }
        }
        return dft;
    }

    /**
    *** Return true if the specified String contains a valid long value
    *** @param data   The String to test
    *** @param strict True to test for a strict long value (ie. does not contain
    ***               any other superfluous trailing characters), false to allow for 
    ***               other non-critical trailing characters.
    *** @return True if the specified String contains a valid long value
    **/
    public static boolean isLong(String data, boolean strict)
    {
        if (StringTools.isBlank(data)) {
            return false;
        } else {
            FilterNumber fn = new FilterNumber(data, Long.class);
            return fn.isValid(strict);
        }
    }

    /**
    *** Return true if the specified Object contains a valid long value
    *** @param data   The Object to test
    *** @param strict True to test for a strict long value (ie. does not contain
    ***               any other superfluous trailing characters), false to allow for 
    ***               other non-critical trailing characters.
    *** @return True if the specified String contains a valid long value
    **/
    public static boolean isLong(Object data, boolean strict)
    {
        if (data == null) {
            return false;
        } else
        if (data instanceof Number) {
            return strict? (data instanceof Long) : true;
        } else {
            return StringTools.isLong(data.toString(), strict);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Parse the specified object into a int value
    *** @param data  The object to parse
    *** @param dft   The default int value if unable to parse the specified object
    *** @return The parsed int value
    **/
    public static int parseInt(Object data, int dft)
    {
        if (data == null) {
            return dft;
        } else
        if (data instanceof Number) {
            return ((Number)data).intValue();
        } else {
            return StringTools.parseInt(data.toString(), dft);
        }
    }

    /**
    *** Parse the specified String into a int value
    *** @param data  The String to parse
    *** @param dft   The default int value if unable to parse the specified object
    *** @return The parsed int value
    **/
    public static int parseInt(String data, int dft)
    {
        return StringTools.parseInt(new FilterNumber(data, Integer.class), dft);
    }

    /**
    *** Parse the specified FilterNumber into a int value
    *** @param num  The FilterNumber to parse
    *** @param dft  The default int value if unable to parse the specified object
    *** @return The parsed int value
    **/
    public static int parseInt(FilterNumber num, int dft)
    {
        if ((num != null) && num.supportsType(Integer.class)) {
            if (num.isHex()) {
                return (int)StringTools.parseHexLong(num.getValueString(), dft);
            } else {
                try {
                    return Integer.parseInt(num.getValueString());
                } catch (NumberFormatException nfe) {
                    // Since 'FilterNumber' makes sure that only digits are parsed,
                    // this likely means that the specified digit string is too large
                    // for this required data type.  Our last ditch effort is to
                    // attempt to convert it to a BigInteger and extract the lower
                    // number of bits to match our data type.
                    BigInteger bigLong = parseBigInteger(num, null);
                    if (bigLong != null) {
                        return bigLong.intValue();
                    }
                }
            }
        }
        return dft;
    }

    /**
    *** Return true if the specified String contains a valid int value
    *** @param data   The String to test
    *** @param strict True to test for a strict int value (ie. does not contain
    ***               any other superfluous trailing characters), false to allow for 
    ***               other non-critical trailing characters.
    *** @return True if the specified String contains a valid int value
    **/
    public static boolean isInt(String data, boolean strict)
    {
        if (StringTools.isBlank(data)) {
            return false;
        } else {
            FilterNumber fn = new FilterNumber(data, Integer.class);
            return fn.isValid(strict);
        }
    }
    
    /**
    *** Return true if the specified Object contains a valid int value
    *** @param data   The Object to test
    *** @param strict True to test for a strict int value (ie. does not contain
    ***               any other superfluous trailing characters), false to allow for 
    ***               other non-critical trailing characters.
    *** @return True if the specified String contains a valid int value
    **/
    public static boolean isInt(Object data, boolean strict)
    {
        if (data == null) {
            return false;
        } else
        if (data instanceof Number) {
            return strict? (data instanceof Integer) : true;
        } else {
            return StringTools.isInt(data.toString(), strict);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Parse the specified object into a short value
    *** @param data  The object to parse
    *** @param dft   The default short value if unable to parse the specified object
    *** @return The parsed short value
    **/
    public static int parseShort(Object data, short dft)
    {
        if (data == null) {
            return dft;
        } else
        if (data instanceof Number) {
            return ((Number)data).shortValue();
        } else {
            return StringTools.parseShort(data.toString(), dft);
        }
    }

    /**
    *** Parse the specified String into a short value
    *** @param data  The String to parse
    *** @param dft   The default short value if unable to parse the specified object
    *** @return The parsed short value
    **/
    public static short parseShort(String data, short dft)
    {
        return StringTools.parseShort(new FilterNumber(data, Short.class), dft);
    }

    /**
    *** Parse the specified FilterNumber into a short value
    *** @param num  The FilterNumber to parse
    *** @param dft  The default short value if unable to parse the specified object
    *** @return The parsed short value
    **/
    public static short parseShort(FilterNumber num, short dft)
    {
        if ((num != null) && num.supportsType(Short.class)) {
            if (num.isHex()) {
                return (short)StringTools.parseHexLong(num.getValueString(), dft);
            } else {
                try {
                    return Short.parseShort(num.getValueString());
                } catch (NumberFormatException nfe) {
                    // Since 'FilterNumber' makes sure that only digits are parsed,
                    // this likely means that the specified digit string is too large
                    // for this required data type.  Our last ditch effort is to
                    // attempt to convert it to a BigInteger and extract the lower
                    // number of bits to match our data type.
                    BigInteger bigLong = parseBigInteger(num, null);
                    if (bigLong != null) {
                        return bigLong.shortValue();
                    }
                }
            }
        }
        return dft;
    }

    /**
    *** Return true if the specified String contains a valid short value
    *** @param data   The String to test
    *** @param strict True to test for a strict short value (ie. does not contain
    ***               any other superfluous trailing characters), false to allow for 
    ***               other non-critical trailing characters.
    *** @return True if the specified String contains a valid short value
    **/
    public static boolean isShort(String data, boolean strict)
    {
        if (StringTools.isBlank(data)) {
            return false;
        } else {
            FilterNumber fn = new FilterNumber(data, Short.class);
            return fn.isValid(strict);
        }
    }
    
    /**
    *** Return true if the specified Object contains a valid short value
    *** @param data   The Object to test
    *** @param strict True to test for a strict short value (ie. does not contain
    ***               any other superfluous trailing characters), false to allow for 
    ***               other non-critical trailing characters.
    *** @return True if the specified String contains a valid short value
    **/
    public static boolean isShort(Object data, boolean strict)
    {
        if (data == null) {
            return false;
        } else
        if (data instanceof Number) {
            return strict? (data instanceof Short) : true;
        } else {
            return StringTools.isShort(data.toString(), strict);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Parse the specified String into a BigInteger value
    *** @param data  The String to parse
    *** @param dft   The default BigInteger value if unable to parse the specified object
    *** @return The parsed BigInteger value
    **/
    public static BigInteger parseBigInteger(String data, BigInteger dft)
    {
        return StringTools.parseBigInteger(new FilterNumber(data, BigInteger.class), dft);
    }

    /**
    *** Parse the specified FilterNumber into a BigInteger value
    *** @param num  The FilterNumber to parse
    *** @param dft  The default BigInteger value if unable to parse the specified object
    *** @return The parsed BigInteger value
    **/
    public static BigInteger parseBigInteger(FilterNumber num, BigInteger dft)
    {
        if ((num != null) && num.supportsType(BigInteger.class)) {
            if (num.isHex()) {
                try {
                    return new BigInteger(num.getHexBytes());
                } catch (NumberFormatException nfe) {
                    // ignore (not likely to occur)
                }
                return null;
            } else {
                try {
                    return new BigInteger(num.getValueString());
                } catch (NumberFormatException nfe) {
                    // ignore (not likely to occur)
                }
            }
        }
        return dft;
    }

    // ------------------------------------------------------------------------

    /**
    *** Class used to parse numeric values
    **/
    public static class FilterNumber
    {

        private String   inpStr = null;
        private Class<?> type   = null;
        private boolean  isHex  = false;

        private String numStr = null;
        private int startPos  = -1;
        private int endPos    = -1;

        public FilterNumber(String val, Class<?> type) {

            /* null string */
            if (val == null) { // null string
                //Print.logDebug("'null' value");
                return;
            }

            /* skip initial whitespace */
            int s = 0;
            while ((s < val.length()) && Character.isWhitespace(val.charAt(s))) { s++; }
            if (s == val.length()) { // empty string
                //Print.logDebug("empty value");
                return;
            }
            String v = val; // (val != null)? val.trim() : "";
            int vlen = v.length();

            /* hex number */
            boolean hex = false;
            if (((s + 1) < vlen) && (v.charAt(s) == '0') && (Character.toLowerCase(v.charAt(s + 1)) == 'x')) {
                // we will be parsing a hex value
                hex = true;
                s += 2; // skip "0x"
            }

            /* plus sign? */
            if (!hex && (s < vlen) && (v.charAt(s) == '+')) {
                // skip over prefixing '+'
                s++;
            }

            /* negative number */
            int ps, p = (!hex && (s < vlen) && (v.charAt(s) == '-'))? (s + 1) : s;

            /* skip initial digits */
            if (hex) {
                // scan until end of hex digits
                for (ps = p; (p < vlen) && ("0123456789ABCDEF".indexOf(Character.toUpperCase(v.charAt(p))) >= 0);) { p++; }
            } else {
                // scan until end of decimal digits
                for (ps = p; (p < vlen) && Character.isDigit(v.charAt(p));) { p++; }
            }
            boolean foundDigit = (p > ps);

            /* end of digits? */
            String num;
            if ((p >= vlen)
                || Long.class.isAssignableFrom(type)
                || Integer.class.isAssignableFrom(type)
                || Short.class.isAssignableFrom(type)
                || Byte.class.isAssignableFrom(type)
                || BigInteger.class.isAssignableFrom(type)
                ) {
                // end of String or Long/Integer/Short/Byte/BigInteger
                num = v.substring(s, p);
            } else
            if (v.charAt(p) != '.') {
                // Double/Float, but doesn't contain decimal
                num = v.substring(s, p);
            } else {
                // Double/Float, decimal digits
                p++; // skip '.'
                for (ps = p; (p < vlen) && Character.isDigit(v.charAt(p));) { p++; }
                if (p > ps) { foundDigit = true; }
                num = v.substring(s, p);
            }

            /* set instance vars */
            if (foundDigit) {
                this.isHex      = hex;
                this.inpStr     = val;
                this.type       = type;
                this.numStr     = num;
                this.startPos   = s;
                this.endPos     = p;
            }

        }

        public <T> boolean supportsType(Class<T> ct) {
            if ((this.numStr != null) && (this.type != null)) {
                if (this.type.isAssignableFrom(ct)) {
                    return true; // quick check (Double/Float/BigInteger/Long/Integer/Byte)
                } else
                if (Short.class.isAssignableFrom(this.type)) {
                    return this.supportsType(Byte.class);
                } else
                if (Integer.class.isAssignableFrom(this.type)) {
                    return this.supportsType(Short.class);
                } else
                if (Long.class.isAssignableFrom(this.type)) {
                    return this.supportsType(Integer.class);
                } else
                if (BigInteger.class.isAssignableFrom(this.type)) {
                    return this.supportsType(Long.class);
                } else
                if (Float.class.isAssignableFrom(this.type)) {
                    return this.supportsType(BigInteger.class);
                } else
                if (Double.class.isAssignableFrom(this.type)) {
                    return this.supportsType(Float.class);
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }

        public String getInputString() {
            return this.inpStr;
        }

        public Class getClassType() {
            return this.type;
        }

        public String getClassTypeName() {
            if (this.type != null) {
                String cn = this.type.getName();
                if (cn.startsWith("java.lang.")) {
                    return cn.substring("java.lang.".length());
                } else
                if (cn.startsWith("java.math.")) {
                    return cn.substring("java.math.".length());
                } else {
                    return cn;
                }
            } else {
                return "null";
            }
        }

        public boolean isHex() {
            return this.isHex;
        }

        public String getValueString() {
            return this.numStr;
        }

        public boolean isValid(boolean strict) {
            if (this.getValueString() == null) {
                return false;
            } else
            if (!strict) {
                // don't care about trailing characters
                return true;
            } else {
                // must not have any trailing characters
                return (this.getInputString().length() == this.getEnd());
            }
        }

        public byte[] getHexBytes() {
            if (this.isHex) {
                return StringTools.parseHex(this.getValueString(), new byte[0]);
            } else {
                // not tested yet
                return (new BigInteger(this.getValueString())).toByteArray();
            }
        }

        public int getStart() {
            return this.startPos;
        }

        public int getEnd() {
            return this.endPos;
        }

        public int getLength() {
            return (this.endPos - this.startPos);
        }

        public Number toNumber(Number dft) {
            if ((this.numStr != null) && (this.type != null)) {
                try {
                    if (Byte.class.equals(this.type)) {
                        return new Byte(this.numStr);
                    } else
                    if (Short.class.equals(this.type)) {
                        return new Short(this.numStr);
                    } else
                    if (Integer.class.equals(this.type)) {
                        return new Integer(this.numStr);
                    } else
                    if (Long.class.equals(this.type)) {
                        return new Long(this.numStr);
                    } else
                    if (BigInteger.class.equals(this.type)) {
                        return new BigInteger(this.numStr);
                    } else
                    if (Float.class.equals(this.type)) {
                        return new Float(this.numStr);
                    } else
                    if (Double.class.equals(this.type)) {
                        return new Double(this.numStr);
                    } else {
                        Print.logError("Unkrecognized Number type: " + StringTools.className(this.type));
                        return dft;
                    }
                } catch (NumberFormatException nfe) {
                    // should not occur
                    Print.logException("Number conversion error", nfe);
                    return dft;
                }
            }
            return dft;
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(StringTools.quoteString(this.getInputString()));
            sb.append("/");
            sb.append(this.getClassTypeName());
            sb.append("/");
            sb.append(this.getStart());
            sb.append("/");
            sb.append(this.getEnd());
            return sb.toString();
        }

    }

    // ------------------------------------------------------------------------

    /**
    *** Parse the specified object into a boolean value
    *** @param data  The object to parse
    *** @param dft   The default boolean value if unable to parse the specified object
    *** @return The parsed boolean value
    **/
    public static boolean parseBoolean(Object data, boolean dft)
    {
        if (data == null) {
            return dft;
        } else
        if (data instanceof Boolean) {
            return ((Boolean)data).booleanValue();
        } else {
            return StringTools.parseBoolean(data.toString(), dft);
        }
    }

    /**
    *** Parse the specified String into a boolean value
    *** @param data  The String to parse
    *** @param dft   The default boolean value if unable to parse the specified object
    *** @return The parsed boolean value
    **/
    public static boolean parseBoolean(String data, boolean dft)
    {
        if (data != null) {
            String v = data.toLowerCase();
            if (dft) {
                // if default is 'true', only test for 'false'
                for (int i = 0; i < BooleanFALSE.length; i++) {
                    if (v.startsWith(BooleanFALSE[i])) {
                        return false;
                    }
                }
            } else {
                // if default is 'false', only test for 'true'
                for (int i = 0; i < BooleanTRUE.length; i++) {
                    if (v.startsWith(BooleanTRUE[i])) {
                        return true;
                    }
                }
            }
            // else return default
            return dft;
        }
        return dft;
    }

    /**
    *** Return true if the specified String contains a valid boolean value
    *** @param data  The String to test
    *** @param strict True to test for a strict boolean value (ie. does not contain
    ***               any other superfluous trailing characters), false to allow for 
    ***               other non-critical trailing characters.
    *** @return True if the specified String contains a valid boolean value
    **/
    public static boolean isBoolean(String data, boolean strict)
    {
        String v = data.toLowerCase();
        for (int i = 0; i < BooleanTRUE.length; i++) {
            boolean ok = strict? v.equals(BooleanTRUE[i]) : v.startsWith(BooleanTRUE[i]);
            if (ok) {
                return true;
            }
        }
        for (int i = 0; i < BooleanFALSE.length; i++) {
            boolean ok = strict? v.equals(BooleanFALSE[i]) : v.startsWith(BooleanFALSE[i]);
            if (ok) {
                return true;
            }
        }
        return false;
    }

    /**
    *** Return true if the specified String contains a valid boolean value
    *** @param data  The String to test
    *** @param strict True to test for a strict boolean value (ie. does not contain
    ***               any other superfluous trailing characters), false to allow for 
    ***               other non-critical trailing characters.
    *** @return True if the specified String contains a valid boolean value
    **/
    public static boolean isBoolean(Object data, boolean strict)
    {
        if (data == null) {
            return false;
        } else
        if (data instanceof Boolean) {
            return true;
        } else {
            return StringTools.isBoolean(data.toString(), strict);
        }
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Parse the specified String into a Dimension object
    *** @param data  The String object to parse
    *** @param dft   The default Dimension object if unable to parse the String
    *** @return The parsed Dimension object
    **/
    public static Dimension parseDimension(String data, Dimension dft)
    {
        int p = (data != null)? data.indexOf("/") : -1;
        if (p > 0) {
            int w = StringTools.parseInt(data.substring(0,p),0);
            int h = StringTools.parseInt(data.substring(p+1),0);
            return new Dimension(w, h);
        } else {
            return dft;
        }
    }

    // ------------------------------------------------------------------------

    public static final String HEX = "0123456789ABCDEF";

    /** 
    *** Returns the value of the specified hex character
    *** @param ch  The hex character to return the value
    *** @return The value of the specified hex character, or -1 if the specified
    ***         character is not a valid hex character
    **/
    public static int hexIndex(char ch)
    {
        return StringTools.HEX.indexOf(Character.toUpperCase(ch));
    }

    /**
    *** Returns the hex character for the least significant nybble of the specified byte
    *** @param nybble The value to convert to a hex character.  Only the least significant
    ***               nybble of this byte will be used to convert to the hex character.
    *** @return The character representation of the specifified nybble
    **/
    public static char hexNybble(byte nybble)
    {
        return HEX.charAt(nybble & 0xF);
    }

    /**
    *** Returns the hex character for the least significant nybble of the specified byte
    *** @param nybble The value to convert to a hex character.  Only the least significant
    ***               nybble of this byte will be used to convert to the hex character.
    *** @return The character representation of the specifified nybble
    **/
    public static char hexNybble(int nybble)
    {
        return HEX.charAt(nybble & 0xF);
    }

    /**
    *** Parse the specified String, containing a hex representation, into a byte array
    *** @param data  The String containing the hex character values
    *** @param dft   The default byte array return if unable to convert the specified String value
    *** @return The parse byte array
    **/
    public static byte[] parseHex(String data, byte dft[])
    {
        if (data != null) {

            /* get data string */
            String d = data.toUpperCase();
            String s = d.startsWith("0X")? d.substring(2) : d;

            /* remove any invalid trailing characters */
            // scan until we find an invalid character (or the end of the string)
            for (int i = 0; i < s.length(); i++) {
                if (HEX.indexOf(s.charAt(i)) < 0) {
                    s = s.substring(0, i);
                    break;
                }
            }

            /* return default if nothing to parse */
            if (s.equals("")) {
                return dft;
            }

            /* right justify */
            if ((s.length() & 1) == 1) { s = "0" + s; } // right justified

            /* parse data */
            byte rtn[] = new byte[s.length() / 2];
            for (int i = 0; i < s.length(); i += 2) {
                int c1 = HEX.indexOf(s.charAt(i));
                if (c1 < 0) { c1 = 0; /* Invalid Hex char */ }
                int c2 = HEX.indexOf(s.charAt(i+1));
                if (c2 < 0) { c2 = 0; /* Invalid Hex char */ }
                rtn[i/2] = (byte)(((c1 << 4) & 0xF0) | (c2 & 0x0F));
            }

            /* return value */
            return rtn;

        } else {

            return dft;

        }
    }

    /**
    *** Parse the String containing a hex representation into an int value
    *** @param data The String hex representation to convert to a String
    *** @param dft  The default int value to return if unable to convert the specified String hex representation.
    *** @return The parse int value
    **/
    public static int parseHex(String data, int dft)
    {
        return (int)StringTools.parseHexLong(data, (long)dft);
    }

    /**
    *** Parse the String containing a hex representation into an int value
    *** @param data The String hex representation to convert to a String
    *** @param dft  The default int value to return if unable to convert the specified String hex representation.
    *** @return The parse int value
    **/
    public static int parseHexInt(String data, int dft)
    {
        return (int)StringTools.parseHexLong(data, (long)dft);
    }

    /**
    *** Parse the String containing a hex representation into a long value
    *** @param data The String hex representation to convert to a String
    *** @param dft  The default long value to return if unable to convert the specified String hex representation.
    *** @return The parse long value
    **/
    public static long parseHex(String data, long dft)
    {
        return StringTools.parseHexLong(data, dft);
    }

    /**
    *** Parse the String containing a hex representation into a long value
    *** @param data The String hex representation to convert to a String
    *** @param dft  The default long value to return if unable to convert the specified String hex representation.
    *** @return The parse long value
    **/
    public static long parseHexLong(String data, long dft)
    {
        byte b[] = parseHex(data, null);
        if (b != null) {
            long val = 0L;
            for (int i = 0; i < b.length; i++) {
                val = (val << 8) | ((int)b[i] & 0xFF);
            }
            return val;
        } else {
            return dft;
        }
    }

    /**
    *** Returns the number of valid hex characters found in the specified String
    *** @param data  The String containing the hex representation
    *** @return The number of valid hex characters
    **/
    public static int hexLength(String data)
    {
        if (StringTools.isBlank(data)) {
            return 0;
        } else {
            String d = data.toUpperCase();
            int s = d.startsWith("0X")? 2 : 0, e = s;
            for (; (e < d.length()) && (HEX.indexOf(d.charAt(e)) >= 0); e++);
            return e;
        }
    }

    /* return true if the specified string represents a valid hex value */
    /**
    *** Returns true if the specified String contains hext characters
    *** @param data  The String representation of the hex characters to test
    *** @param strict  True to check for strict hex character values, false to allow for
    ***                trailing superfluous characters.
    *** @return True if the specified String contains a valie hex representation, false otherwise.
    **/
    public static boolean isHex(String data, boolean strict)
    {
        if (StringTools.isBlank(data)) {
            return false;
        } else {
            String d = data.toUpperCase();
            int s = d.startsWith("0X")? 2 : 0, e = s;
            for (; e < d.length(); e++) {
                if (HEX.indexOf(d.charAt(e)) < 0) {
                    if (strict) {
                        return false;
                    } else {
                        break;
                    }
                }
            }
            return (e > s);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** This method prints the specified byte array to a String hex representation
    *** showing the contained bytes with corresponding displayed printable characters.
    *** @param b  The byte array to convert to a String representation
    *** @return The hex representation in the form of a StringBuffer
    **/
    public static StringBuffer formatHexString(byte b[])
    {
        return StringTools.formatHexString(b, 0, -1, 16, true, null);
    }

    /**
    *** This method prints the specified byte array to a String hex representation
    *** showing the contained bytes with corresponding displayed printable characters.
    *** @param b  The byte array to convert to a String representation
    *** @param blockLen  The number of bytes display on a single row.
    *** @return The hex representation in the form of a StringBuffer
    **/
    public static StringBuffer formatHexString(byte b[], int blockLen)
    {
        return StringTools.formatHexString(b, 0, -1, blockLen, true, null);
    }

    /**
    *** This method prints the specified byte array to a String hex representation
    *** showing the contained bytes with corresponding displayed printable characters.
    *** @param b  The byte array to convert to a String representation
    *** @param blockLen  The number of bytes display on a single row
    *** @param sb        The destination ouput StringBuffer 
    *** @return The hex representation in the form of a StringBuffer
    **/
    public static StringBuffer formatHexString(byte b[], int blockLen, StringBuffer sb)
    {
        return StringTools.formatHexString(b, 0, -1, blockLen, true, sb);
    }

    /**
    *** This method prints the specified byte array to a String hex representation
    *** showing the contained bytes with corresponding displayed printable characters.
    *** @param b  The byte array to convert to a String representation
    *** @param bOfs      The starting index where the byte array contents will be dipsplayed
    *** @param bLen      The number of byte to display from the specified byte array
    *** @param blockLen  The number of bytes display on a single row
    *** @param showAscii True to display the 
    *** @param sb        The destination ouput StringBuffer 
    *** @return The hex representation in the form of a StringBuffer
    **/
    public static StringBuffer formatHexString(byte b[], int bOfs, int bLen, int blockLen, boolean showAscii, StringBuffer sb)
    {
        int headerLen = 0;
        if (b  == null) { b = new byte[0]; }
        if (sb == null) { sb = new StringBuffer(); }
        int bi = (bOfs >= 0)? bOfs : 0; // byte index
        int bMaxNdx = ((bLen >= 0) && ((bi + bLen) <= b.length))? (bi + bLen) : b.length;

        /* validate block length */
        if (blockLen <= 0) {
            blockLen = ((bMaxNdx - bi) < 16)? bLen : 16;
        }

        /* position ruler */
        int rulerLen = (headerLen > blockLen)? headerLen : blockLen;
        sb.append("    : ** ");
        for (int ri = 1; ri < rulerLen;) {
            for (int j = ri; (ri < rulerLen) & ((ri - j) < 4); ri++) { sb.append("-- "); }
            if (ri < rulerLen) { sb.append("++ "); ri++; }
            for (int j = ri; (ri < rulerLen) & ((ri - j) < 4); ri++) { sb.append("-- "); }
            if (ri < rulerLen) { sb.append(format(ri,"00 ")); ri++; }
        }
        sb.append("\n");

        /* byte header */
        if (headerLen > 0) {
            sb.append(format(bi,"0000")).append(": ");
            for (int j = bi; ((j - bi) < headerLen); j++) {
                if (j < bMaxNdx) {
                    toHexString(b[j], sb);
                } else {
                    sb.append("  ");
                }
                sb.append(" ");
            }
            if (showAscii) {
                sb.append(" ");
                for (int j = bi; ((j - bi) < headerLen); j++) {
                    if (j < bMaxNdx) {
                        if ((b[j] >= ' ') && (b[j] <= '~')) {
                            sb.append((char)b[j]);
                        } else {
                            sb.append('.');
                        }
                    } else {
                        sb.append(" ");
                    }
                }
            }
            sb.append("\n");
            bi += headerLen;
        }

        /* byte data */
        int count = 0;
        for (; bi < bMaxNdx; bi += blockLen) {
            sb.append(format(bi,"0000")).append(": ");
            for (int j = bi; ((j - bi) < blockLen); j++) {
                if (j < bMaxNdx) {
                    toHexString(b[j], sb);
                    count++;
                } else {
                    sb.append("  ");
                }
                sb.append(" ");
            }
            if (showAscii) {
                sb.append(" ");
                for (int j = bi; ((j - bi) < blockLen); j++) {
                    if (j < bMaxNdx) {
                        if ((b[j] >= ' ') && (b[j] <= '~')) {
                            sb.append((char)b[j]);
                        } else {
                            sb.append('.');
                        }
                    } else {
                        sb.append(" ");
                    }
                }
            }
            sb.append("\n");
        }
        
        sb.append(count).append(" bytes\n");
        return sb;
    }

    // ------------------------------------------------------------------------

    /**
    *** Converts the specified byte to a String hex representation
    *** @param b  The byte to convert to a String hex representation
    *** @param sb  The destination StringBuffer where the hex String is placed.  If
    ***            null, a new StringBuffer will be created.
    *** @return The StringBuffer where the String hex representation is placed
    **/
    public static StringBuffer toHexString(byte b, StringBuffer sb)
    {
        if (sb == null) { sb = new StringBuffer(); }
        sb.append(HEX.charAt((b >> 4) & 0xF));
        sb.append(HEX.charAt(b & 0xF));
        return sb;
    }

    /**
    *** Converts the specified byte to a String hex representation
    *** @param b  The byte to convert to a String hex representation
    *** @return The String containing the hex representation
    **/
    public static String toHexString(byte b)
    {
        return StringTools.toHexString(b,null).toString();
    }

    /**
    *** Converts the specified byte array to a String hex representation
    *** @param b   The byte array to convert to a String hex representation
    *** @param ofs The offset into the byte array to start the hex conversion
    *** @param len The number of bytes to convert to hex
    *** @param sb  The destination StringBuffer where the hex String is placed.  If
    ***            null, a new StringBuffer will be created.
    *** @return The StringBuffer where the String hex representation is placed
    **/
    public static StringBuffer toHexString(byte b[], int ofs, int len, StringBuffer sb)
    {
        if (sb == null) { sb = new StringBuffer(); }
        if (b != null) {
            int bstrt = (ofs < 0)? 0 : ofs;
            int bstop = (len < 0)? b.length : Math.min(b.length,(ofs + len));
            for (int i = bstrt; i < bstop; i++) { StringTools.toHexString(b[i], sb); }
        }
        return sb;
    }

    /**
    *** Converts the specified byte array to a String hex representation
    *** @param b   The byte array to convert to a String hex representation
    *** @param sb  The destination StringBuffer where the hex String is placed.  If
    ***            null, a new StringBuffer will be created.
    *** @return The StringBuffer where the String hex representation is placed
    **/
    public static StringBuffer toHexString(byte b[], StringBuffer sb)
    {
        return StringTools.toHexString(b,0,-1,sb);
    }

    /**
    *** Converts the specified byte array to a String hex representation
    *** @param b   The byte array to convert to a String hex representation
    *** @return The String containing the hex representation
    **/
    public static String toHexString(byte b[])
    {
        return StringTools.toHexString(b,0,-1,null).toString();
    }

    /**
    *** Converts the specified byte array to a String hex representation
    *** @param b   The byte array to convert to a String hex representation
    *** @param ofs The offset into the byte array to start the hex conversion
    *** @param len The number of bytes to convert to hex
    *** @return The String containing the hex representation
    **/
    public static String toHexString(byte b[], int ofs, int len)
    {
        return StringTools.toHexString(b,ofs,len,null).toString();
    }

    /**
    *** Converts the specified long value to a hex representation
    *** @param val  The long value to convert to hex
    *** @param bitLen  The length of significant bits to include in the hex representation
    *** @return The String containing the hex representation
    **/
    public static String toHexString(long val, int bitLen)
    {

        /* bounds check 'bitLen' */
        // TODO: what if 'val' is < 0?
        if (bitLen <= 0) {
            if ((val & 0xFFFFFFFF00000000L) != 0L) {
                bitLen = 64;
            } else
            if ((val & 0x00000000FFFF0000L) != 0L) {
                bitLen = 32;
            } else
            if ((val & 0x000000000000FF00L) != 0L) {
                bitLen = 16;
            } else {
                bitLen = 8;
            }
        } else 
        if (bitLen > 64) {
            bitLen = 64;
        }

        /* format and return hex value */
        int nybbleLen = ((bitLen + 7) / 8) * 2;
        StringBuffer hex = new StringBuffer(Long.toHexString(val).toUpperCase());
        //Print.logInfo("NybbleLen: " + nybbleLen + " : " + hex + " [" + hex.length());
        if ((nybbleLen <= 16) && (nybbleLen > hex.length())) {
            String mask = "0000000000000000"; // 64 bit (16 nybbles)
            hex.insert(0, mask.substring(0, nybbleLen - hex.length()));
        }
        return hex.toString();

    }

    /**
    *** Converts the specified long value to a hex representation
    *** @param val  The long value to convert to hex
    *** @return The String containing the hex representation
    **/
    public static String toHexString(long val)
    {
        return StringTools.toHexString(val, 64);
    }

    /**
    *** Converts the specified int value to a hex representation
    *** @param val  The int value to convert to hex
    *** @return The String containing the hex representation
    **/
    public static String toHexString(int val)
    {
        return StringTools.toHexString((long)val & 0xFFFFFFFF, 32);
    }

    /**
    *** Converts the specified short value to a hex representation
    *** @param val  The short value to convert to hex
    *** @return The String containing the hex representation
    **/
    public static String toHexString(short val)
    {
        return StringTools.toHexString((long)val & 0xFFFF, 16);
    }

    // ------------------------------------------------------------------------

    /**
    *** Converts the specified byte array to a String binary representation
    *** @param b   The byte array to convert to a String binary representation
    *** @param ofs The offset into the byte array to start the binary conversion
    *** @param len The number of bytes to convert to binary
    *** @param sb  The destination StringBuffer where the binary String is placed.  If
    ***            null, a new StringBuffer will be created.
    *** @return The StringBuffer where the String binary representation is placed
    **/
    public static StringBuffer toBinaryString(byte b[], int ofs, int len, StringBuffer sb)
    {
        if (sb == null) { sb = new StringBuffer(); }
        if (b != null) {
            int bstrt = (ofs < 0)? 0 : ofs;
            int bstop = (len < 0)? b.length : Math.min(b.length,(ofs + len));
            for (int i = bstrt; i < bstop; i++) {
                if (i > 0) { sb.append(" "); }
                StringTools.toBinaryString(b[i], sb);
            }
        }
        return sb;
    }

    /**
    *** Converts the specified byte array to a String binary representation
    *** @param b   The byte array to convert to a String binary representation
    *** @param sb  The destination StringBuffer where the binary String is placed.  If
    ***            null, a new StringBuffer will be created.
    *** @return The StringBuffer where the String binary representation is placed
    **/
    public static StringBuffer toBinaryString(byte b[], StringBuffer sb)
    {
        if (sb == null) { sb = new StringBuffer(); }
        if (b != null) {
            for (int i = 0; i < b.length; i++) {
                if (i > 0) { sb.append(" "); }
                StringTools.toBinaryString(b[i], sb);
            }
        }
        return sb;
    }

    /**
    *** Converts the specified byte array to a String binary representation
    *** @param b   The byte array to convert to a String binary representation
    *** @return The String containing the binary representation
    **/
    public static String toBinaryString(byte b[])
    {
        return StringTools.toBinaryString(b,new StringBuffer()).toString();
    }

    /**
    *** Converts the specified byte array to a String binary representation
    *** @param b   The byte array to convert to a String binary representation
    *** @param ofs The offset into the byte array to start the binary conversion
    *** @param len The number of bytes to convert to binary
    *** @return The String containing the binary representation
    **/
    public static String toBinaryString(byte b[], int ofs, int len)
    {
        return StringTools.toBinaryString(b,ofs,len,null).toString();
    }

    // ------------------------------------------------------------------------

    /**
    *** Converts the specified byte to a String binary representation
    *** @param b  The byte to convert to a String binary representation
    *** @param sb  The destination StringBuffer where the binary String is placed.  If
    ***            null, a new StringBuffer will be created.
    *** @return The StringBuffer where the String binary representation is placed
    **/
    public static StringBuffer toBinaryString(byte b, StringBuffer sb)
    {
        return StringTools.toBinaryString((long)b, 8, sb);
    }

    /**
    *** Converts the specified byte to a String binary representation
    *** @param b  The byte to convert to a String binary representation
    *** @return The String containing the binary representation
    **/
    public static String toBinaryString(byte b)
    {
        return StringTools.toBinaryString(b, null).toString();
    }

    // ------------------------------------------------------------------------

    /**
    *** Converts the specified int to a String binary representation
    *** @param i  The int to convert to a String binary representation
    *** @param sb  The destination StringBuffer where the binary String is placed.  If
    ***            null, a new StringBuffer will be created.
    *** @return The StringBuffer where the String binary representation is placed
    **/
    public static StringBuffer toBinaryString(int i, StringBuffer sb)
    {
        return StringTools.toBinaryString((long)i, 32, sb);
    }

    /**
    *** Converts the specified int to a String binary representation
    *** @param i  The int to convert to a String binary representation
    *** @return The String containing the binary representation
    **/
    public static String toBinaryString(int i)
    {
        return StringTools.toBinaryString(i, null).toString();
    }

    // ------------------------------------------------------------------------

    /**
    *** Converts the specified int to a String binary representation
    *** @param i   The 'long' to convert to a String binary representation
    *** @param bc  The bit-count to return
    *** @param sb  The destination StringBuffer where the binary String is placed.  If
    ***            null, a new StringBuffer will be created.
    *** @return The StringBuffer where the String binary representation is placed
    **/
    public static StringBuffer toBinaryString(long i, int bc, StringBuffer sb)
    {
        if (bc <= 0) { bc = 64; }
        if (sb == null) { sb = new StringBuffer(); }
        String s = Long.toBinaryString((long)i);
        if (s.length() > bc) {
            s = s.substring(s.length() - bc);
        } else
        if (s.length() < bc) {
            while (s.length() < bc) { s = "0" + s; }
        }
        sb.append(s);
        return sb;
    }

    /**
    *** Converts the specified int to a String binary representation
    *** @param i   The 'long' to convert to a String binary representation
    *** @param sb  The destination StringBuffer where the binary String is placed.  If
    ***            null, a new StringBuffer will be created.
    *** @return The StringBuffer where the String binary representation is placed
    **/
    public static StringBuffer toBinaryString(long i, StringBuffer sb)
    {
        return StringTools.toBinaryString(i, -1, sb);
    }
    
    /**
    *** Converts the specified int to a String binary representation
    *** @param i  The 'long' to convert to a String binary representation
    *** @return The String containing the binary representation
    **/
    public static String toBinaryString(long i)
    {
        return StringTools.toBinaryString(i, -1, null).toString();
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the first character of the beginning of each new word in the specified
    *** String to upper-case, and sets the remaining characters in each word to lower-case.
    *** @param s  The String to convert to upper/lower case characters
    *** @return The converted String
    **/
    public static String setFirstUpperCase(String s)
    {
        if (s != null) {
            boolean space = true, digitSpace = true;
            StringBuffer sb = new StringBuffer(s);
            for (int i = 0; i < sb.length(); i++) {
                char ch = sb.charAt(i);
                if (Character.isWhitespace(ch)) { // isSpace
                    space = true;
                } else
                if (digitSpace && Character.isDigit(ch)) {
                    space = true;
                } else
                if (space) {
                    if (Character.isLowerCase(ch)) {
                        sb.setCharAt(i, (char)(ch - ' ')); // toUpperCase
                    }
                    space = false;
                } else
                if (Character.isUpperCase(ch)) {
                    sb.setCharAt(i, (char)(ch + ' ')); // toLowerCase
                }
            }
            return sb.toString();
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified text String starts with the pattern String
    *** without regard to case (a method that should be on the String class itself, but isn't)
    *** @param t The test String
    *** @param m The pattern String
    *** @return True if the test String starts with the pattern String, false otherwise.
    **/
    public static boolean startsWithIgnoreCase(String t, String m)
    {
        if ((t != null) && (m != null)) {
            return t.toLowerCase().startsWith(m.toLowerCase());
        } else {
            return false;
        }
    }

    /**
    *** Returns true if the specified text String ends with the pattern String
    *** without regard to case (a method that should be on the String class itself, but isn't)
    *** @param t The test String
    *** @param p The pattern String
    *** @return True if the test String ends with the pattern String, false otherwise.
    **/
    public static boolean endsWithIgnoreCase(String t, String p)
    {
        if ((t != null) && (p != null)) {
            return t.toLowerCase().endsWith(p.toLowerCase());
        } else {
            return false;
        }
    }

    /**
    *** Returns true if the specified text String ends with one of the pattern Strings in 
    *** the specified array, without regard to case.
    *** @param t The test String
    *** @param p An array of pattern Strings
    *** @return True if the test String ends with any pattern String, false otherwise.
    **/
    public static boolean endsWithIgnoreCase(String t, String p[])
    {
        if ((t != null) && (p != null)) {
            String tlc = t.toLowerCase();
            for (int i = 0; i < p.length; i++) {
                if (p[i] != null) {
                    String plc = p[i].toLowerCase();
                    if (tlc.endsWith(plc)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
    *** Returns index/position of the second pattern String within the first test String
    *** @param t The test String
    *** @param m The pattern String
    *** @return The position of the pattern String within the test String, or -1 if the pattern
    ***         String does not exist within the test String.
    **/
    public static int indexOfIgnoreCase(String t, String m)
    {
        if ((t != null) && (m != null)) {
            return t.toLowerCase().indexOf(m.toLowerCase());
        } else {
            return -1;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the position of the specified pattern character within the character array
    *** @param A  The character array
    *** @param c  The pattern character
    *** @return The position of the pattern character within the test character array, or -1
    ***         if the pattern character does not exist within the test character array
    **/
    public static int indexOf(char A[], char c)
    {
        if (A != null) {
            for (int i = 0; i < A.length; i++) {
                if (A[i] == c) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
    *** Returns the position of the specified pattern byte within the byte array
    *** @param B  The byte array
    *** @param b  The pattern byte
    *** @return The position of the pattern byte within the test byte array, or -1
    ***         if the pattern byte does not exist within the test byte array
    **/
    public static int indexOf(byte B[], byte b)
    {
        if (B != null) {
            for (int i = 0; i < B.length; i++) {
                if (B[i] == b) {
                    return i;
                }
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------------
    // Parse/Encode array from/to String, quoting as necessary

    public  static final String ArraySeparator      = ",";
    public  static final char   ARRAY_DELIM         = ',';
    public  static final char   ARRAY_DOUBLE_QUOTE  = '\"';
    public  static final char   ARRAY_SINGLE_QUOTE  = '\'';
    public  static final char   ARRAY_QUOTE         = ARRAY_DOUBLE_QUOTE;

    /**
    *** Parses the specified "," delimited String to an array of Strings.  Quoted values
    *** are allowed within the delimited String and will be parsed as literal values in
    *** the String array.
    *** @param s  The "," delimited String to parse
    *** @return An array of Strings which have been parsed from the input String
    **/
    public static String[] parseArray(String s)
    {
        return StringTools.parseArray(s, ARRAY_DELIM);
    }

    /**
    *** Parses the specified character delimited String to an array of Strings.  Quoted values
    *** are allowed within the delimited String and will be parsed as literal values in
    *** the String array.
    *** @param s  The character delimited String to parse
    *** @param arrayDelim  The character delimiter
    *** @return An array of Strings which have been parsed from the input String
    **/
    public static String[] parseArray(String s, char arrayDelim)
    {

        /* invalid string? */
        if (StringTools.isBlank(s)) {
            return new String[0];
        }

        /* parse */
        int len = s.length();
        char ch[] = new char[len];
        s.getChars(0, len, ch, 0);
        java.util.List<String> v = new Vector<String>();
        for (int a = 0; a < len;) {
            if (ch[a] == arrayDelim) { // token == ','
                v.add("");
                a++;
            } else
            if ((ch[a] == ARRAY_DOUBLE_QUOTE) || (ch[a] == ARRAY_SINGLE_QUOTE)) { // token = '\"'
                StringBuffer sb = new StringBuffer();
                a = StringTools.parseQuote(ch, a, sb);
                v.add(sb.toString());
                while ((a < len) && (ch[a] != arrayDelim)) { a++; } // discard
                if ((a < len) && (ch[a] == arrayDelim)) { a++; }
            } else
            if (Character.isWhitespace(ch[a])) {
                while ((a < len) && Character.isWhitespace(ch[a])) { a++; }
            } else {
                StringBuffer sb = new StringBuffer();
                while ((a < len) && (ch[a] != arrayDelim)) { sb.append(ch[a++]); }
                v.add(sb.toString());
                if ((a < len) && (ch[a] == arrayDelim)) { a++; }
            }
        }
        return (String[])ListTools.toArray(v, String.class);

    }

    /**
    *** Encodes an array of Strings/Objects into a single String, using the specified character
    *** as the String field delimiter.
    *** @param list         The array of Strings/Objects to encode
    *** @param delim        The character delimter
    *** @param alwaysQuote  True to always quote each String field.  If false, a String field
    ***                     will only be quoted if it contains embedded spaces or other characters
    ***                     that need to be specified literally.\
    *** @return The encoded String
    **/
    public static String encodeArray(Object list[], char delim, boolean alwaysQuote)
    {
        return StringTools.encodeArray(list, 0, -1, delim, alwaysQuote);
    }

    /**
    *** Encodes an array of Strings/Objects into a single String, using the specified character
    *** as the String field delimiter.
    *** @param list  The array of Strings/Objects to encode
    *** @param ofs   The offset within list to begin encoding into the returned String
    *** @param max   The number of String fields to include from the specified list
    *** @param delim The character delimter
    *** @param alwaysQuote  True to always quote each String field.  If false, a String field
    ***                     will only be quoted if it contains embedded spaces or other characters
    ***                     that need to be specified literally.\
    *** @return The encoded String
    **/
    public static String encodeArray(Object list[], int ofs, int max, char delim, boolean alwaysQuote)
    {
        StringBuffer sb = new StringBuffer();
        if (list != null) {
            if ((max < 0) || (max > list.length)) { max = list.length; }
            for (int i = ((ofs >= 0)? ofs : 0); i < max; i++) {
                if (sb.length() > 0) { sb.append(delim); }
                String s = (list[i] != null)? list[i].toString() : "";
                if (alwaysQuote || (s.indexOf(' ') >= 0) || (s.indexOf('\t') >= 0) || (s.indexOf('\"') >= 0) || (s.indexOf(delim) >= 0)) {
                    s = StringTools.quoteString(s);
                }
                sb.append(s);
            }
        }
        return sb.toString();
    }

    /**
    *** Encodes an array/list of Strings/Objects into a single String, using the specified character
    *** as the String field delimiter.
    *** @param list  The Object containing an array or list of Strings to encode
    *** @param delim The character delimter
    *** @param alwaysQuote  True to always quote each String field.  If false, a String field
    ***                     will only be quoted if it contains embedded spaces or other characters
    ***                     that need to be specified literally.\
    *** @return The encoded String
    **/
    public static String encodeArray(Object list, char delim, boolean alwaysQuote)
    {
        return StringTools.encodeArray(list, 0, -1, delim, alwaysQuote);
    }

    /**
    *** Encodes an array/list of Strings/Objects into a single String, using the specified character
    *** as the String field delimiter.
    *** @param list  The Object containing an array or list of Strings to encode
    *** @param ofs   The offset within list to begin encoding into the returned String
    *** @param max   The number of String fields to include from the specified list
    *** @param delim The character delimter
    *** @param alwaysQuote  True to always quote each String field.  If false, a String field
    ***                     will only be quoted if it contains embedded spaces or other characters
    ***                     that need to be specified literally.\
    *** @return The encoded String
    **/
    public static String encodeArray(Object list, int ofs, int max, char delim, boolean alwaysQuote)
    {
        if (list == null) {
            // nothing to encode
            return "";
        } else
        if (list instanceof Object[]) {
            // standard Object array
            return StringTools.encodeArray((Object[])list, ofs, max, delim, alwaysQuote);
        } else 
        if (list.getClass().isArray()) {
            // assume a primitive array
            StringBuffer sb = new StringBuffer();
            int listLen = Array.getLength(list);
            if ((max < 0) || (max > listLen)) { max = listLen; }
            for (int i = ((ofs >= 0)? ofs : 0); i < max; i++) {
                if (sb.length() > 0) { sb.append(delim); }
                Object list_i = Array.get(list, i);
                String s = (list_i != null)? list_i.toString() : "";
                if (alwaysQuote || (s.indexOf(' ') >= 0) || (s.indexOf('\t') >= 0) || (s.indexOf('\"') >= 0) || (s.indexOf(delim) >= 0)) {
                    s = StringTools.quoteString(s);
                }
                sb.append(s);
            }
            return sb.toString();
        } else {
            // a single object (assume a single element array)
            if ((ofs <= 0) && (max != 0)) {
                // offset is '0' with at least '1' element to copy
                return alwaysQuote? StringTools.quoteString(list.toString()) : list.toString();
            } else {
                // offset is out of bounds, or max == 0
                return "";
            }
        }
    }

    /**
    *** Encodes a list of Strings/Objects into a single String, using the specified character
    *** as the String field delimiter.
    *** @param list  The List containing the Strings to encode
    *** @param delim The character delimter
    *** @param alwaysQuote  True to always quote each String field.  If false, a String field
    ***                     will only be quoted if it contains embedded spaces or other characters
    ***                     that need to be specified literally.\
    *** @return The encoded String
    **/
    public static String encodeArray(java.util.List<Object> list, char delim, boolean alwaysQuote)
    {
        return StringTools.encodeArray(ListTools.toArray(list), delim, alwaysQuote);
    }                                          

    /**
    *** Encodes a list of Strings/Objects into a single String, using the "," character
    *** as the String field delimiter.  All returned String fields will be quoted.
    *** @param list  The List containing the Strings to encode
    *** @return The encoded String
    **/
    public static String encodeArray(java.util.List<Object> list)
    {
        return StringTools.encodeArray(ListTools.toArray(list), ARRAY_DELIM, true);
    }

    /**
    *** Encodes a list of Strings/Objects into a single String, using the "," character
    *** as the String field delimiter.
    *** @param list  The List containing the Strings to encode
    *** @param alwaysQuote  True to always quote each String field.  If false, a String field
    ***                     will only be quoted if it contains embedded spaces or other characters
    ***                     that need to be specified literally.\
    *** @return The encoded String
    **/
    public static String encodeArray(java.util.List<Object> list, boolean alwaysQuote)
    {
        return StringTools.encodeArray(ListTools.toArray(list), ARRAY_DELIM, alwaysQuote);
    }

    /**
    *** Encodes an array of Strings/Objects into a single String, using the "," character
    *** as the String field delimiter.  All returned String fields will be quoted.
    *** @param list  The array containing the Strings to encode
    *** @return The encoded String
    **/
    public static String encodeArray(Object list[])
    {
        return StringTools.encodeArray(list, ARRAY_DELIM, true);
    }

    /**
    *** Encodes an array of Strings/Objects into a single String, using the "," character
    *** as the String field delimiter.
    *** @param list  The array containing the Strings to encode
    *** @param alwaysQuote  True to always quote each String field.  If false, a String field
    ***                     will only be quoted if it contains embedded spaces or other characters
    ***                     that need to be specified literally.\
    *** @return The encoded String
    **/
    public static String encodeArray(Object list[], boolean alwaysQuote)
    {
        return StringTools.encodeArray(list, ARRAY_DELIM, alwaysQuote);
    }

    /**
    *** Encodes an array of Strings into a single String, using the "," character
    *** as the String field delimiter.  All returned String fields will be quoted.
    *** @param list  The array containing the Strings to encode
    *** @return The encoded String
    **/
    public static String encodeArray(String list[])
    {
        return StringTools.encodeArray((Object[])list, ARRAY_DELIM, true);
    }

    /**
    *** Encodes an array of Strings into a single String, using the "," character
    *** as the String field delimiter. 
    *** @param list  The array containing the Strings to encode
    *** @param alwaysQuote  True to always quote each String field.  If false, a String field
    ***                     will only be quoted if it contains embedded spaces or other characters
    ***                     that need to be specified literally.\
    *** @return The encoded String
    **/
    public static String encodeArray(String list[], boolean alwaysQuote)
    {
        return StringTools.encodeArray((Object[])list, ARRAY_DELIM, alwaysQuote);
    }

    /**
    *** Encodes an Object containing a list of Strings into a single String, using the "," character
    *** as the String field delimiter.  All returned String fields will be quoted.
    *** @param list  The array containing the Strings to encode
    *** @return The encoded String
    **/
    public static String encodeArray(Object list)
    {
        return StringTools.encodeArray((Object)list, ARRAY_DELIM, true);
    }

    /**
    *** Encodes an Object containing a list of Strings into a single String, using the "," character
    *** as the String field delimiter.
    *** @param list  The array containing the Strings to encode
    *** @param alwaysQuote  True to always quote each String field.  If false, a String field
    ***                     will only be quoted if it contains embedded spaces or other characters
    ***                     that need to be specified literally.\
    *** @return The encoded String
    **/
    public static String encodeArray(Object list, boolean alwaysQuote)
    {
        return StringTools.encodeArray((Object)list, ARRAY_DELIM, alwaysQuote);
    }

    // ------------------------------------------------------------------------

    /**
    *** Converts a list of Objects to an array of Strings
    *** @param list  The list of objects to convert to an array of Strings
    *** @return  The array of Strings
    **/
    public static String[] toArray(java.util.List list)
    {
        if (list != null) {
            String s[] = new String[list.size()];
            for (int i = 0; i < list.size(); i++) {
                Object obj = list.get(i);
                s[i] = (obj != null)? obj.toString() : null;
            }
            return s;
        } else {
            return new String[0];
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Parse the character delimited input String into an array of Strings.  This
    *** method does not take quoted Strings into account.  This method is similar to Perl's
    *** "split" function.  This method does not return null (however the returned String array
    *** may be empty).
    *** @param value  The String to parse
    *** @param delim  The character delimiter
    *** @return The array of parse Strings
    **/
    public static String[] parseString(String value, char delim)
    {
        return StringTools.parseString(value, String.valueOf(delim), true);
    }

    /**
    *** Parse the character delimited input String into an array of Strings.  This
    *** method does not take quoted Strings into account.  This method is similar to Perl's
    *** "split" function.  This method does not return null (however the returned String array
    *** may be empty).
    *** @param value  The String to parse
    *** @param sdelim The character delimiters (all characters in this String are considered 
    ***               individual delimiter candidates)
    *** @return The array of parse Strings
    **/
    public static String[] parseString(String value, String sdelim)
    {
        return StringTools.parseString(value, sdelim, true);
    }
    
    /**
    *** Parse the character delimited input String into an array of Strings.  This
    *** method does not take quoted Strings into account.  This method is similar to Perl's
    *** "split" function.  This method does not return null (however the returned String array
    *** may be empty).
    *** @param value  The String to parse
    *** @param sdelim The character delimiters (all characters in this String are considered 
    ***               individual delimiter candidates)
    *** @param trim   True to trim leading/trailing spaces
    *** @return The array of parse Strings
    **/
    public static String[] parseString(String value, String sdelim, boolean trim)
    {
        if (value != null) {
            boolean skipNL = sdelim.equals("\r\n"); // special case

            /* parse */
            java.util.List<String> v1 = new Vector<String>();
            ListTools.toList(new StringTokenizer(value, sdelim, true), v1);

            /* examine all tokens to make sure we include blank items */
            int dupDelim = 1; // assume we've started with a delimiter
            boolean consumeNextNL = false;
            java.util.List<String> v2 = new Vector<String>();
            for (Iterator i = v1.iterator(); i.hasNext();) {
                String s = (String)i.next();
                if ((s.length() == 1) && (sdelim.indexOf(s) >= 0)) { 
                    // found a delimiter
                    if (skipNL) {
                        char ch = s.charAt(0);
                        if (consumeNextNL && (ch == '\n')) {
                            consumeNextNL = false;
                        } else {
                            consumeNextNL = (ch == '\r');
                            if (dupDelim > 0) { v2.add(""); } // blank item
                            dupDelim++;
                        }
                    } else {
                        if (dupDelim > 0) { v2.add(""); } // blank item
                        dupDelim++;
                    }
                } else {
                    v2.add(trim?s.trim():s);
                    dupDelim = 0;
                    consumeNextNL = false;
                }
            }

            /* return parsed array */
            return (String[])v2.toArray(new String[v2.size()]);

        } else {

            /* nothing parsed */
            return new String[0];

        }
    }

    /**
    *** See StringTools.parseString(String, char)
    **/
    public static String[] split(String value, char delim)
    {
        return StringTools.parseString(value, String.valueOf(delim), true);
    }

    /**
    *** See StringTools.parseString(String, char, boolean)
    **/
    public static String[] split(String value, char delim, boolean trim)
    {
        return StringTools.parseString(value, String.valueOf(delim), trim);
    }

    /** 
    *** Concatenates the specified String array into a single String using the specified
    *** character as the delimiter.  Null elements in the input String array are skipped.
    *** @param val  The input String array
    *** @param delim  The character delimiter
    *** @return The concatinated String
    **/
    public static String join(String val[], char delim)
    {
        StringBuffer sb = new StringBuffer();
        if (val != null) {
            for (int i = 0; i < val.length; i++) {
                if (val[i] != null) {
                    if (sb.length() > 0) { sb.append(delim); }
                    sb.append(val[i]);
                }
            }
        }
        return sb.toString();
    }

    /** 
    *** Concatenates the specified String array into a single String using the specified
    *** String as the delimiter.  Null elements in the input String array are skipped.
    *** @param val  The input String array
    *** @param delim  The String delimiter
    *** @return The concatinated String
    **/
    public static String join(String val[], String delim)
    {
        StringBuffer sb = new StringBuffer();
        if (val != null) {
            for (int i = 0; i < val.length; i++) {
                if (sb.length() > 0) { sb.append(delim); }
                if (val[i] != null) {
                    sb.append(val[i]);
                }
            }
        }
        return sb.toString();
    }

    /** 
    *** Concatenates the specified List objects into a single String using the specified
    *** String as the delimiter.  Null elements in the input list are skipped.
    *** @param list   The input object list
    *** @param delim  The String delimiter
    *** @return The concatinated String
    **/
    public static String join(java.util.List list, String delim)
    {
        StringBuffer sb = new StringBuffer();
        if (list != null) {
            for (Iterator i = list.iterator(); i.hasNext();) {
                Object val = i.next();
                if (sb.length() > 0) { sb.append(delim); }
                if (val != null) {
                    sb.append(val.toString());
                }
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------------

    private static boolean _isPropSep(char sep, char ch)
    {
        if ((sep == 0) || (sep == ' ')) {
            return Character.isWhitespace(ch);
        } else {
            return (ch == sep);
        }
    }

    /** 
    *** Parses a series of properties in the specified String into a property map.  Properties
    *** are specified as "key=value" pairs, and are separated from other property specifications
    *** by whitespace.  Neither the property keys, or values may contain whitespace.
    *** @param props  The String containing the list of properties
    *** @return The map containing the parsed property list
    **/
    public static Map<String,String> parseProperties(String props)
    {
        return StringTools.parseProperties(props, StringTools.PropertySeparatorChar, null);
    }

    /** 
    *** Parses a series of properties in the specified String into a property map.  Properties
    *** are specified as "key=value" pairs, and are separated from other property specifications
    *** by the specified property separator character.  Neither the property keys, or values may 
    *** contain the specified property separator character.
    *** @param props  The String containing the list of properties
    *** @param propSep  The property separator charactor (ie. ' ', or ';')
    *** @return The map containing the parsed property list
    **/
    public static Map<String,String> parseProperties(String props, char propSep)
    {
        return StringTools.parseProperties(props, propSep, null);
    }

    /** 
    *** Parses a series of properties in the specified String into a property map.  Properties
    *** are specified as "key=value" pairs, and are separated from other property specifications
    *** by the specified property separator character.  Neither the property keys, or values may 
    *** contain the specified property separator character.
    *** @param props       The String containing the list of properties
    *** @param propSep     The property separator charactor (ie. ' ', or ';')
    *** @param keyValSep   The key/value separator char (ie. '=' or ':')
    *** @return The map containing the parsed property list
    **/
    public static Map<String,String> parseProperties(String props, char propSep, char keyValSep)
    {
        return StringTools.parseProperties(props, propSep, keyValSep, null);
    }

    /** 
    *** Parses a series of properties in the specified String into a property map.  Properties
    *** are specified as "key=value" pairs, and are separated from other property specifications
    *** by whitespace.  Neither the property keys, or values may contain whitespace.
    *** @param props       The String containing the list of properties
    *** @param properties  The map where the parsed properties will be placed.  If null, a mew
    ***                    map object will be created.
    *** @return The map containing the parsed property list
    **/
    public static Map<String,String> parseProperties(String props, Map<String,String> properties)
    {
        return StringTools.parseProperties(props, StringTools.PropertySeparatorChar, properties);
    }

    /** 
    *** Parses a series of properties in the specified String into a property map.  Properties
    *** are specified as "key=value" pairs, and are separated from other property specifications
    *** by the specified property separator character.  Neither the property keys, or values may 
    *** contain the specified property separator character.
    *** @param props       The String containing the list of properties
    *** @param propSep     The property separator charactor (ie. ' ', or ';')
    *** @param properties  The map where the parsed properties will be placed.  If null, a mew
    ***                    map object will be created.
    *** @return The map containing the parsed property list
    **/
    public static Map<String,String> parseProperties(String props, char propSep, Map<String,String> properties)
    {
        return StringTools.parseProperties(props, propSep, StringTools.KeyValSeparatorChar, properties);
    }
    
    /** 
    *** Parses a series of properties in the specified String into a property map.  Properties
    *** are specified as "key=value" pairs, and are separated from other property specifications
    *** by the specified property separator character.  Neither the property keys, or values may 
    *** contain the specified property separator character.
    *** @param props       The String containing the list of properties
    *** @param propSep     The property separator charactor (ie. ' ', or ';')
    *** @param keyValSep   The key/value separator char (ie. '=' or ':')
    *** @param properties  The map where the parsed properties will be placed.  If null, a mew
    ***                    map object will be created.
    *** @return The map containing the parsed property list
    **/
    public static Map<String,String> parseProperties(String props, char propSep, char keyValSep, Map<String,String> properties)
    {
        boolean spacePropSep = (propSep == 0) || (propSep == ' ');

        /* new properties? */
        if (properties == null) { properties = new OrderedMap<String,String>(); }

        /* init */
        String r = StringTools.trim(props);
        char ch[] = new char[r.length()];
        r.getChars(0, r.length(), ch, 0);
        int c = 0;

        /* skip prefixing spaces */
        while ((c < ch.length) && (ch[c] == ' ')) { c++; }
        if (c > 0) { r = r.substring(c); }

        /* check for name */
        int n1 = 0, n2 = r.indexOf(" "), n3 = r.indexOf(keyValSep);
        if (n2 < 0) { n2 = r.length(); }
        if ((n3 < 0) || (n2 < n3)) { // no '=', or position of '=' is before ' '
            //if (allowNameChange) { this.setName(r.substring(n1, n2)); }
            //if (this.getIncludeNameInArgs()) { n2 = 0; }
            n2 = 0; // start at beginning of string
        } else {
            n2 = 0; // start at beginning of string
        }

        /* extract properties */
        int argsLen = r.length() - n2, a = 0;
        char args[] = new char[argsLen];
        r.getChars(n2, r.length(), args, 0);
        for (;a < argsLen;) {

            /* skip whitespace (and any prefixing property separators) */
            while ((a < argsLen) && (Character.isWhitespace(args[a]) || _isPropSep(propSep,args[a]))) { a++; }

            /* prop name */
            // scan until first Whitespace, PropertySeparator, or KeyValSeparator
            StringBuffer propName = new StringBuffer();
            for (;(a < argsLen) && !Character.isWhitespace(args[a]) && !_isPropSep(propSep,args[a]) && (args[a] != keyValSep); a++) {
                propName.append(args[a]);
            }
            
            /* skip whitespace? */
            if (!spacePropSep) {
                while ((a < argsLen) && Character.isWhitespace(args[a])) { a++; }
            }

            /* prop value */
            // only if next char is '='
            StringBuffer propValue = new StringBuffer();
            if ((a < argsLen) && (args[a] == keyValSep)) {
                a++; // skip past '='
                if (!spacePropSep) {
                    // skip whitespace
                    while ((a < argsLen) && Character.isWhitespace(args[a])) { a++; }
                }
                if ((a < argsLen) && (args[a] == '\"')) { // quoted string
                    // stop at end of quoted string
                    a++; // skip past first '\"'
                    for (; (a < argsLen) && (args[a] != '\"'); a++) {
                        if (((a + 1) < argsLen) && (args[a] == '\\')) { a++; }
                        propValue.append(args[a]);
                    }
                    if (a < argsLen) { a++; } // skip past last '\"'
                } else {
                    // stop at first Whitespace or PropertySeparator
                    for (;(a < argsLen) && !Character.isWhitespace(args[a]) && !_isPropSep(propSep,args[a]); a++) {
                        propValue.append(args[a]);
                    }
                }
            }

            /* add property */
            if (propName.length() > 0) {
                // we must have a property key
                String key = propName.toString();
                String val = propValue.toString();
                properties.put(key, val);
            }

            /* skip past any trailing junk */
            // stop at first PropertySeparator
            while ((a < argsLen) && !_isPropSep(propSep,args[a])) { a++; } // trailing junk
            if ((a < argsLen) && !_isPropSep(propSep,args[a])) { a++; } // skip PropertySeparator

        }

        return properties;

    }

    // ------------------------------------------------------------------------

    public static final int STRIP_INCLUDE = 0;
    public static final int STRIP_EXCLUDE = 1;

    /**
    *** Strips the specified characters from the input String
    *** @param src  The input String from which the characters will be removed
    *** @param chars The list of characters which will be removed from the input String
    *** @return The stripped String
    **/
    public static String stripChars(String src, char chars[])
    {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < chars.length; i++) { sb.append(chars[i]); }
        return stripChars(src, sb.toString(), STRIP_EXCLUDE);
    }

    /**
    *** Strips the specified character from the input String
    *** @param src  The input String from which the character will be removed
    *** @param ch The character which will be removed from the input String
    *** @return The stripped String
    **/
    public static String stripChars(String src, char ch)
    {
        return stripChars(src, String.valueOf(ch), STRIP_EXCLUDE);
    }

    /**
    *** Strips the specified characters from the input String
    *** @param src  The input String from which the characters will be removed
    *** @param chars The String of characters which will be removed from the input String
    *** @return The stripped String
    **/
    public static String stripChars(String src, String chars)
    {
        return stripChars(src, chars, STRIP_EXCLUDE);
    }

    /**
    *** Strips the specified character from the input String
    *** @param src  The input String from which the character will be removed
    *** @param ch The character which will be removed from the input String
    *** @param stripType May be either STRIP_INCLUDE to include only the specified character, 
    ***                  or STRIP_EXCLUDE to exclude the specified character.
    *** @return The stripped String
    **/
    public static String stripChars(String src, char ch, int stripType)
    {
        return stripChars(src, String.valueOf(ch), stripType);
    }

    /**
    *** Strips the specified characters from the input String
    *** @param src   The input String from which the characters will be removed
    *** @param chars The list of characters which will be removed from the input String
    *** @param stripType May be either STRIP_INCLUDE to include only the specified characters, 
    ***                  or STRIP_EXCLUDE to exclude the specified characters.
    *** @return The stripped String
    **/
    public static String stripChars(String src, String chars, int stripType)
    {
        if ((src != null) && (chars != null)) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < src.length(); i++) {
                char ch = src.charAt(i);
                if (stripType == STRIP_INCLUDE) { // include chars
                    if (chars.indexOf(ch) >= 0) { sb.append(ch); }
                } else { // exclude chars
                    if (chars.indexOf(ch) <  0) { sb.append(ch); }
                }
            }
            return sb.toString();
        } else {
            return src;
        }
    }

    // ------------------------------------------------------------------------

    /** 
    *** Replaces specific characters in the input String with the specified replacement character
    *** @param src  The input String
    *** @param ch   The character to replace
    *** @param repChar The replacement character
    *** @return The Stirng containing the replaced characters
    **/
    public static String replaceChars(String src, char ch, char repChar)
    {
        return StringTools.replaceChars(src, String.valueOf(ch), String.valueOf(repChar));
    }

    /** 
    *** Replaces specific characters in the input String with the specified replacement character
    *** @param src     The input String
    *** @param chars   The characters to replace (any character found in this String will be replaced)
    *** @param repChar The replacement character
    *** @return The Stirng containing the replaced characters
    **/
    public static String replaceChars(String src, String chars, char repChar)
    {
        return StringTools.replaceChars(src, chars, String.valueOf(repChar));
    }

    /** 
    *** Replaces specific characters in the input String with the specified replacement String
    *** @param src    The input String
    *** @param chars  The characters to replace (any character found in this String will be replaced)
    *** @param repStr The replacement String
    *** @return The Stirng containing the replaced characters
    **/
    public static String replaceChars(String src, String chars, String repStr)
    {
        if ((src != null) && (chars != null)) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < src.length(); i++) {
                char ch = src.charAt(i);
                if (chars.indexOf(ch) >= 0) {
                    if (repStr != null) {
                        sb.append(repStr); 
                    }
                } else {
                    sb.append(ch);
                }
            }
            return sb.toString();
        } else {
            return src;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Replaces all whitespace found in the input String with the specified replacement character
    *** @param src  The input String
    *** @param repChar  The character used to replace whitespace characters
    *** @return The String containing the replaced characters (sans whitespace)
    **/
    public static String replaceWhitespace(String src, char repChar)
    {
        return StringTools.replaceWhitespace(src, String.valueOf(repChar));
    }

    /**
    *** Replaces all whitespace found in the input String with the specified replacement String
    *** @param src  The input String
    *** @param repStr  The String used to replace whitespace characters
    *** @return The String containing the replaced characters (sans whitespace)
    **/
    public static String replaceWhitespace(String src, String repStr)
    {
        if ((src != null) && (repStr != null)) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < src.length(); i++) {
                char ch = src.charAt(i);
                if (Character.isWhitespace(ch)) {
                    sb.append(repStr);
                } else {
                    sb.append(ch);
                }
            }
            return sb.toString();
        } else {
            return src;
        }
    }

    // ------------------------------------------------------------------------
    // Replace keys found in target String with their corresponding values
    // Key format: ${key[:arg]}

    private static final String KEY_START               = "${";
    private static final String KEY_END                 = "}";
    
    /**
    *** KeyValueMap interface.  Used to provide key/value replacement lookups
    **/
    public interface KeyValueMap
    {
        public String getKeyValue(String key, String arg);
    }

    /**
    *** Replaces all occurances of "${key}" with the value returned by the KeyValueMap interface
    *** 'getKeyValue' method.
    *** @param text The text containing the "${key}" fields
    *** @param keyMap  The KeyValueMap object used to retrieve values for the specific 'keys'
    **/
    public static String replaceKeys(String text, KeyValueMap keyMap)
    {
        
        /* first check to see if there are any keys in this string */
        int ks = text.indexOf(KEY_START);
        if (ks < 0) {
            return text;
        }
        
        /* start replacing keys */
        StringBuffer repText = new StringBuffer(text);
        for (;(ks >= 0);) {
            
            /* find end of key */
            int ke = repText.indexOf(KEY_END,ks);
            if (ke < 0) {
                // invalid key specification, stop here
                break;
            }
            
            /* extract key/arg */
            String key = repText.substring(ks+2, ke);
            String arg = null;
            int a = key.indexOf(':');
            if (a >= 0) {
                arg = key.substring(a+1);
                key = key.substring(0,a);
            }

            /* replace key with value */
            String val = (keyMap != null)? keyMap.getKeyValue(key,arg) : null;
            repText.replace(ks, ke+1, ((val!=null)?val:""));
            
            /* find start of next key */
            ks = repText.indexOf(KEY_START, ks);
        }

        /* return new text */
        return repText.toString();
        
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a String containing the 'pattern' String replicated 'count' times.
    *** @param pattern The pattern String
    *** @param count   The number of times to replicate the pattern Strin
    *** @return The repllicated pattern String
    **/
    public static String replicateString(String pattern, int count)
    {
        if ((pattern != null) && (count > 0)) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < count; i++) { sb.append(pattern); }
            return sb.toString();
        } else {
            return "";
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Pads the input String on the right with specified number of pad characters.  If the
    *** length of the String is equal-to, or greater-than, the specified length, the the 
    *** input String is returned as-is.
    *** @param s  The input String
    *** @param padChar  The pad character
    *** @param len The length up to which pad characters will be appended
    *** @return The padded String
    **/
    public static String padRight(String s, char padChar, int len)
    {
        if ((s == null) || (s.length() >= len)) {
            return s;
        } else {
            return s + StringTools.replicateString(String.valueOf(padChar), len - s.length());
        }
    }

    /**
    *** Pads the input String on the right with specified number of ' ' characters.  If the
    *** length of the String is equal-to, or greater-than, the specified length, the the 
    *** input String is returned as-is.
    *** @param s  The input String
    *** @param len The length up to which ' ' characters will be appended
    *** @return The padded String
    **/
    public static String leftAlign(String s, int len)
    {
        return StringTools.padRight(s, ' ', len);
    }

    /**
    *** See StringTools.leftAlign(String,int)
    **/
    public static String leftJustify(String s, int len)
    {
        return StringTools.padRight(s, ' ', len);
    }

    /**
    *** Pads the input String on the left with specified number of pad characters.  If the
    *** length of the String is equal-to, or greater-than, the specified length, the the 
    *** input String is returned as-is.
    *** @param s  The input String
    *** @param padChar  The pad character
    *** @param len The length up to which pad characters will be pre-pended
    *** @return The padded String
    **/
    public static String padLeft(String s, char padChar, int len)
    {
        if ((s == null) || (s.length() >= len)) {
            return s;
        } else {
            return StringTools.replicateString(String.valueOf(padChar), len - s.length()) + s;
        }
    }

    /**
    *** Pads the input String on the left with specified number of ' ' characters.  If the
    *** length of the String is equal-to, or greater-than, the specified length, the the 
    *** input String is returned as-is.
    *** @param s  The input String
    *** @param len The length up to which ' ' characters will be pre-pended
    *** @return The padded String
    **/
    public static String rightAlign(String s, int len)
    {
        return StringTools.padLeft(s, ' ', len);
    }

    /**
    *** See StringTools.rightAlign(String,int)
    **/
    public static String rightJustify(String s, int len)
    {
        return StringTools.padLeft(s, ' ', len);
    }

    // ------------------------------------------------------------------------

    /** 
    *** Remove all '\r', and replace all '\n' with "\\n".
    *** @param text  The input String
    *** @return The NL encoded String
    **/
    public static String encodeNewline(String text)
    {
        if (text != null) {
            return StringTools.encodeNewline(new StringBuffer(text)).toString();
        } else {
            return null;
        }
    }

    /** 
    *** Remove all '\r', and replace all '\n' with "\\n".
    *** @param text  The input StringBuffer
    *** @return The NL encoded StringBuffer
    **/
    public static StringBuffer encodeNewline(StringBuffer sb)
    {
        if (sb != null) {
            for (int c = 0; c < sb.length();) {
                char ch = sb.charAt(c);
                if (ch == '\r') {
                    sb.deleteCharAt(c);
                } else
                if (ch == '\n') {
                    sb.replace(c, c + 1, "\\n");
                    c += 2;
                } else {
                    c++;
                }
            }
        }
        return sb;
    }

    // ------------------------------------------------------------------------

    /** 
    *** Replace all '\\n' with "\n".
    *** @param text  The input String
    *** @return The NL decoded String
    **/
    public static String decodeNewline(String text)
    {
        if (text != null) {
            return StringTools.decodeNewline(new StringBuffer(text)).toString();
        } else {
            return null;
        }
    }

    /** 
    *** Replace all '\\n' with "\n".
    *** @param text  The input StringBuffer
    *** @return The NL decoded StringBuffer
    **/
    public static StringBuffer decodeNewline(StringBuffer sb)
    {
        return StringTools.replace(sb,"\\n","\n");
    }

    // ------------------------------------------------------------------------

    /**
    *** Within the input 'text' String, replaces all occurances of the 'key' String with the 'val' String.
    *** @param text  The input String
    *** @param key   The key pattern String
    *** @param val   The replacement value String
    *** @return The String containing the replaced keys
    **/
    public static String replace(String text, String key, String val)
    {
        if (text != null) {
            return StringTools.replace(new StringBuffer(text), key, val).toString();
        } else {
            return null;
        }
    }

    /**
    *** Within the input 'sb' StringBuffer, replaces all occurances of the 'key' String with the 'val' String.
    *** @param sb    The input StringBuffer
    *** @param key   The key pattern String
    *** @param val   The replacement value String
    *** @return The StringBuffer containing the replaced keys
    **/
    public static StringBuffer replace(StringBuffer sb, String key, String val)
    {
        if (sb != null) {
            int s = 0;
            while (true) {
                s = sb.indexOf(key, s);
                if (s < 0) { break; }
                int e = s + key.length();
                sb.replace(s, e, val);
                s += val.length(); // = e;
            }
        }
        return sb;
    }

    // ------------------------------------------------------------------------

    /**
    *** Within the input 'target' String, replaces all occurances of the regular expression 'regex',
    *** with the 'val' String.
    *** @param target The input String
    *** @param regex The regular expression pattern String
    *** @param val   The replacement value String
    *** @return The String containing the replaced text
    **/
    public static String regexReplace(String target, String regex, String val)
    {
        int flags = Pattern.MULTILINE | Pattern.CASE_INSENSITIVE;
        Pattern pattern = Pattern.compile(regex, flags);
        Matcher matcher = pattern.matcher(target);
        return matcher.replaceAll(val);
    }

    /**
    *** Within the input 'target' StringBuffer, replaces all occurances of the regular expression 'regex',
    *** with the 'val' String.
    *** @param target   The input StringBuffer
    *** @param regexKey The regular expression pattern String
    *** @param val      The replacement value String
    *** @return The StringBuffer containing the replaced text
    **/
    public static StringBuffer regexReplace(StringBuffer target, String regexKey, String val)
    {
        String s = StringTools.regexReplace(target.toString(), regexKey, val);
        return target.replace(0, target.length(), s);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the target String matches the regular expression
    *** @param target The tested String
    *** @param regex  The regular expression
    *** @return True if the target String matches the specified regular expression
    **/
    public static boolean regexMatches(String target, String regex)
    {
        //Print.logInfo("Regex=" + regex + ", Target=" + target);
        return Pattern.matches(regex, target);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns regular expressing index of the specified regular expression within the target String
    *** @param target The target String
    *** @param regex  The regular expression
    *** @return The RegexIndex of the location of the regular expression within the target String
    **/
    public static RegexIndex regexIndexOf(String target, String regex)
    {
        return StringTools.regexIndexOf(target, regex, 0);
    }

    /**
    *** Returns regular expressing index of the specified regular expression within the target String
    *** @param target The target String
    *** @param regex  The regular expression
    *** @param ndx    The position within the target String to start searching for the regular expression
    *** @return The RegexIndex of the location of the regular expression within the target String
    **/
    public static RegexIndex regexIndexOf(String target, String regex, int ndx)
    {
        int flags = Pattern.MULTILINE; //  | Pattern.CASE_INSENSITIVE;
        Pattern pattern = Pattern.compile(regex, flags);
        Matcher match = pattern.matcher(target);
        if (match.find(ndx)) {
            return new RegexIndex(match);
        } else {
            return null;
        }
    }

    /**
    *** Finds the next occurance of the matching regular expression
    *** @param regNdx A previously obtained RegexIndex object
    *** @return The RegexIndex of the location of the regular expression within the target String
    **/
    public static RegexIndex regexIndexOf(RegexIndex regNdx)
    {
        if (regNdx == null) {
            return null;
        } else
        if (regNdx.getMatcher() == null) { 
            return null; 
        } else
        if (regNdx.getMatcher().find()) {
            return regNdx;
        } else {
            return null;
        }
    }

    public static class RegexIndex
    {
        private Matcher matcher = null;
        private int startPos    = -1;
        private int endPos      = -1;
        public RegexIndex(Matcher match) {
            this.matcher = match;
        }
        public RegexIndex(int start, int end) {
            this.startPos = start;
            this.endPos = end;
        }
        public Matcher getMatcher() {
            return this.matcher;
        }
        public int getStart() {
            return (this.matcher != null)? this.matcher.start() : this.startPos;
        }
        public int getEnd() {
            return (this.matcher != null)? this.matcher.end() : this.endPos;
        }
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(this.getStart());
            sb.append("/");
            sb.append(this.getEnd());
            return sb.toString();
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** ReplacementMap interface
    **/
    public static interface ReplacementMap
    {
        public String get(String key);
    }

    /**
    *** Searches the input String for key variables (determined by the specified start/end delimiters)
    *** and replaces matching keys with the values specified in the replacement array
    *** @param text  The target String
    *** @param startDelim  The pattern used to determine the start of a 'key' variable
    *** @param endDelim    The pattern used to determine the end of a key variable
    *** @param rep  An array containing key/value pairs
    *** @return The String containing the replaced key variables
    **/
    public static String insertKeyValues(String text, String startDelim, String endDelim, String rep[][])
    {
        return StringTools.insertKeyValues(text, startDelim, endDelim, rep, false);
    }

    /**
    *** Searches the input String for key variables (determined by the specified start/end delimiters)
    *** and replaces matching keys with the values specified in the replacement array
    *** @param text  The target String
    *** @param startDelim  The pattern used to determine the start of a 'key' variable
    *** @param endDelim    The pattern used to determine the end of a key variable
    *** @param rep  An array containing key/value pairs
    *** @param htmlFilter True to encode the resulting key value for display within an html context
    *** @return The String containing the replaced key variables
    **/
    public static String insertKeyValues(String text, String startDelim, String endDelim, String rep[][], boolean htmlFilter)
    {
        if (text != null) {
            Map<String,String> repMap = new HashMap<String,String>();
            for (int i = 0; i < rep.length; i++) {
                if ((rep[i] == null) || (rep[i].length < 2)) { continue; }
                repMap.put(rep[i][0], rep[i][1]);
            }
            return insertKeyValues(text, startDelim, endDelim, repMap, htmlFilter);
        } else {
            return text;
        }
    }

    /**
    *** Searches the input String for key variables (determined by the specified start/end delimiters)
    *** and replaces matching keys with the values specified in the replacement map
    *** @param text  The target String
    *** @param startDelim  The pattern used to determine the start of a 'key' variable
    *** @param endDelim    The pattern used to determine the end of a key variable
    *** @param map  A map containing key/value pairs
    *** @return The String containing the replaced key variables
    **/
    public static String insertKeyValues(String text, String startDelim, String endDelim, Map<String,String> map)
    {
        return StringTools.insertKeyValues(text, startDelim, endDelim, map, false);
    }

    /**
    *** Searches the input String for key variables (determined by the specified start/end delimiters)
    *** and replaces matching keys with the values specified in the replacement map
    *** @param text  The target String
    *** @param startDelim  The pattern used to determine the start of a 'key' variable
    *** @param endDelim    The pattern used to determine the end of a key variable
    *** @param map  A map containing key/value pairs
    *** @param htmlFilter True to encode the resulting key value for display within an html context
    *** @return The String containing the replaced key variables
    **/
    public static String insertKeyValues(String text, String startDelim, String endDelim, final Map<String,String> map, boolean htmlFilter)
    {
        if (text != null) {
            ReplacementMap rm = new ReplacementMap() {
                public String get(String key) {
                    Object val = (key != null)? (Object)map.get(key) : null;
                    return (val != null)? val.toString() : "";
                }
            };
            return insertKeyValues(text, startDelim, endDelim, rm, htmlFilter);
        } else {
            return text;
        }
    }

    /**
    *** Searches the input String for key variables (determined by the specified start/end delimiters)
    *** and replaces matching keys with the values specified in the replacement map
    *** @param text  The target String
    *** @param startDelim  The pattern used to determine the start of a 'key' variable
    *** @param endDelim    The pattern used to determine the end of a key variable
    *** @param rmap  A StringTools.ReplacementMap containing key/value pairs
    *** @return The String containing the replaced key variables
    **/
    public static String insertKeyValues(String text, String startDelim, String endDelim, StringTools.ReplacementMap rmap)
    {
        return StringTools.insertKeyValues(text, startDelim, endDelim, rmap, false);
    }

    /**
    *** Searches the input String for key variables (determined by the specified start/end delimiters)
    *** and replaces matching keys with the values specified in the replacement map
    *** @param text  The target String
    *** @param startDelim  The pattern used to determine the start of a 'key' variable
    *** @param endDelim    The pattern used to determine the end of a key variable
    *** @param rmap  A StringTools.ReplacementMap containing key/value pairs
    *** @param htmlFilter True to encode the resulting key value for display within an html context
    *** @return The String containing the replaced key variables
    **/
    public static String insertKeyValues(String text, String startDelim, String endDelim, StringTools.ReplacementMap rmap, boolean htmlFilter)
    {

        /* null text? */
        if (text == null) {
            return text;
        }

        /* start/end delimiters */
        char startDelimChars[] = startDelim.toCharArray();
        int  startDelimLen     = startDelimChars.length;
        char endDelimChars[]   = endDelim.toCharArray();
        int  endDelimLen       = endDelimChars.length;

        /* replace keys in text string */
        StringBuffer sb   = new StringBuffer(text);
        int s = 0;
        while (s < sb.length()) {

            /* start delimiter */
            s = sb.indexOf(startDelim, s);
            if (s < 0) { break; } // no more starting delimiters (exit)

            /* ignore delimiter if prefixed with '\' [ie. \${hello}] */
            if ((s > 0) && (sb.charAt(s - 1) == '\\')) {
                // skip this literal delimiter char
                s += startDelimLen;
                continue;
            }

            /* end delimiter */
            //int e = sb.indexOf(endDelim, s + startDelimLen);
            int e = _findEndDelimiter(sb, s + startDelimLen, startDelimChars, endDelimChars);
            if (e < 0) { break; } // ending delimiter not found (exit)

            /* ignore this start/end delimiter? */
            // no longer necessary, since '_findEndDelimiter' returns the proper ending-delimiter pointer
            /*
            int sn = sb.indexOf(startDelim, s + startDelimLen); // next start delimiter
            if ((sn >= 0) && (e > sn)) {
                // ending delimiter is beyond next start delimiter
                // likely a syntax error, skip the current var replacement and move to the next
                s = sn; // reset starting delimiter
                continue;
            }
            */

            /* set replacement value */
            String key = sb.substring(s + startDelimLen, e).trim(); // trim key
            String val = (rmap != null)? rmap.get(key) : ("?" + key + "?");
            if (val != null) {
                //Print.sysPrintln("Found Key: " + key + " [replace with '" + val + "']");
                sb.replace(s, e + endDelimLen, (htmlFilter?StringTools.htmlFilter(val):val));
                s += val.length();
            } else {
                //Print.sysPrintln("Key not found: " + key + " [replace with '']");
                s = e + endDelimLen;
            }

        }
        return sb.toString();

    }

    private static int _findEndDelimiter(StringBuffer sb, int c, char sd[], char ed[])
    {
        int level = 1, sblen = sb.length();
        for (;c < sblen;) {
            // explicit escape
            if (sb.charAt(c) == '\\') {
                c++; // skip '\'
                if (c < sblen) {
                    c++; // skip next char
                }
                continue;
            }
            // start-delimiter match?
            if (sb.charAt(c) == sd[0]) {
                int x = 1; // index '0' already matched
                for (;(x<sd.length)&&((c+x)<sblen)&&(sb.charAt(c+x)==sd[x]);x++);
                if (x == sd.length) {
                    level++;
                    c += x;
                    continue;
                }
            }
            // end-delimiter match
            if (sb.charAt(c) == ed[0]) {
                int x = 1; // index '0' already matched
                for (;(x<ed.length)&&((c+x)<sblen)&&(sb.charAt(c+x)==ed[x]);x++);
                if (x == ed.length) {
                    level--;
                    if (level == 0) {
                        return c;
                    }
                    c += x;
                    continue;
                }
            }
            // advance to next char
            c++;
        }
        return -1;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Compares byte arrays for equality
    *** @param b1  First byte array
    *** @param b2  Second byte array
    *** @param len Length of bytes to compare
    *** @return 0 if byte arrays are equal, < 0 if the first non-matching byte is less than the second, > 0 if
    ***         the first non-matching byte is greater than the second
    **/
    public static int compare(byte b1[], byte b2[], int len)
    {
        if ((b1 == null) && (b2 == null)) {
            return 0;
        } else
        if (b1 == null) {
            return 1;
        } else
        if (b2 == null) {
            return -1;
        } else {
            int n1 = b1.length, n2 = b2.length, i = 0;
            if (len < 0) { len = (n1 >= n2)? n1 : n2; }
            for (i = 0; (i < n1) && (i < n2) && (i < len); i++) {
                if (b1[i] != b2[i]) { 
                    // return comparison of differing bytes
                    return b1[i] - b2[i]; 
                }
            }
            return (i < len)? (n1 - n2) : 0;
        }
    }

    /**
    *** Compares byte arrays for equality
    *** @param b1  First byte array
    *** @param s   Second String value
    *** @return 0 if the byte array is equal to the specified String, < 0 if the first non-matching 
    ***         byte is less than the correspoinding character/byte in the String, > 0 if
    ***         the first non-matching byte is greater than the correspoinding character/byte in the String
    **/
    public static int compare(byte b1[], String s)
    {
        return StringTools.compare(b1, ((s != null)? StringTools.getBytes(s) : null), -1);
    }

    /**
    *** Returns true if the specified byte arrays are equals
    *** @param b1  First byte array
    *** @param b2  Second byte array
    *** @param len Length of bytes to compare
    *** @return True if the byte arrays are equals, false otherwise
    **/
    public static boolean compareEquals(byte b1[], byte b2[], int len)
    {
        if (b1 == b2) {
            return true;
        } else
        if ((b1 == null) || (b2 == null)) {
            return false;
        } else {
            return (StringTools.compare(b1, b2, len) == 0);
        }
    }

    /**
    *** Returns true if the specified byte arrays are equals
    *** @param b1  First byte array
    *** @param b2  Second byte array
    *** @return True if the byte arrays are equals, false otherwise
    **/
    public static boolean compareEquals(byte b1[], byte b2[]) 
    {
        return StringTools.compareEquals(b1, b2, -1);
    }

    /**
    *** Returns true if the specified byte array is equal to the specified String
    *** @param b   First byte array
    *** @param s   Second String value
    *** @return True if the byte array is equal to the String, false otherwise
    **/
    public static boolean compareEquals(byte b[], String s)
    {
        return StringTools.compareEquals(b, ((s != null)? StringTools.getBytes(s) : null), -1);
    }

    // ------------------------------------------------------------------------

    /**
    *** Compares 2 byte arrays, returing the index of the byte where they differ
    *** @param b1  First byte array
    *** @param b2  Second byte array
    *** @return The index/location where the byte arrays differ, or -1 if they are the same
    **/
    public static int diff(byte b1[], byte b2[])
    {
        return StringTools.diff(b1, b2, -1);
    }

    /**
    *** Compares 2 byte arrays, returing the index of the byte where they differ
    *** @param b1  First byte array
    *** @param b2  Second byte array
    *** @param len Length of bytes to compare
    *** @return The index/location where the byte arrays differ, or -1 if they are the same
    **/
    public static int diff(byte b1[], byte b2[], int len)
    {
        if ((b1 == null) && (b2 == null)) {
            return -1; // equals
        } else
        if ((b1 == null) || (b2 == null)) {
            return 0; // diff on first byte
        } else {
            int n1 = b1.length, n2 = b2.length, i = 0;
            if (len < 0) { len = (n1 >= n2)? n1 : n2; } // larger of two lengths
            for (i = 0; (i < n1) && (i < n2) && (i < len); i++) {
                if (b1[i] != b2[i]) { 
                    // return index of differing bytes
                    return i; 
                }
            }
            return (i < len)? i : -1;
        }
    }

    // ------------------------------------------------------------------------
    // Number formatting
    // [may be useful: http://pws.prserv.net/ad/programs/Programs.html#PaddedDecimalFormat]

    /* return a DecimalFormat for specified format String */
    private static Map<String,DecimalFormat> formatMap = null;
    private static DecimalFormat _getFormatter(String fmt)
    {
        if (StringTools.isBlank(fmt)) { fmt = "0"; }
        if (formatMap == null) { formatMap = new HashMap<String,DecimalFormat> (); }
        DecimalFormat df = (DecimalFormat)formatMap.get(fmt);
        if (df == null) {
            //df = new DecimalFormat(fmt); // use default locale
            df = new DecimalFormat(fmt, new DecimalFormatSymbols(Locale.US));
            formatMap.put(fmt, df);
        }
        return df;
    }

    /**
    *** Format/Convert the specified double value to a String, based on the specified format pattern
    *** @param val  The double value to format
    *** @param fmt  The format pattern
    *** @return The String containing the formatted double value
    **/
    public static String format(double val, String fmt)
    {
        return format(val, fmt, -1);
    }

    /**
    *** Format/Convert the specified double value to a String, based on the specified format pattern<br>
    *** If the format String begins with '%', then String.format(...) will be used.<br>
    *** Otherwise DecimalFormat will be used.
    *** @param val  The double value to format
    *** @param fmt  The format pattern
    *** @param fieldSize  The minimum formatted field width.
    *** @return The String containing the formatted double value
    **/
    public static String format(double val, String fmt, int fieldSize)
    {
        String s = "";
        if (fmt == null) {
            s = String.valueOf(val);
        } else
        if (fmt.startsWith("%")) {
            try {
                s = String.format(fmt, new Object[] { val });
            } catch (Throwable th) { 
                // IllegalFormatPrecisionException, IllegalFormatConversionException
                Print.logException("Format exception [" + fmt + "]", th);
                s = String.valueOf(val);
            }
        } else {
            s = StringTools._getFormatter(fmt).format(val);
        }
        if (fieldSize > s.length()) {
            s = StringTools.rightAlign(s, fieldSize);
        }
        return s;
    }

    /**
    *** Format/Convert the specified long value to a String, based on the specified format pattern
    *** @param val  The long value to format
    *** @param fmt  The format pattern
    *** @return The String containing the formatted long value
    **/
    public static String format(long val, String fmt)
    {
        return format(val, fmt, -1);
    }

    /**
    *** Format/Convert the specified long value to a String, based on the specified format pattern.<br>
    *** The format String may be one of the following:
    *** <ul>
    ***   <li>null - String.valueOf(val) will be returned.</li>
    ***   <li>"time" - 'val' will be formatted as a date/time value.</li>
    ***   <li>"Xn" - 'val' will be formatted as an 'n' length hex value.</li>
    ***   <li>"%nf" - 'val' will be formatted as an 'n' length field value.</li>
    *** </ul>
    *** @param val  The long value to format
    *** @param fmt  The format pattern
    *** @param fieldSize  The minimum formatted field width.
    *** @return The String containing the formatted long value
    **/
    public static String format(long val, String fmt, int fieldSize)
    {
        String s = null;
        if (fmt == null) {
            s = String.valueOf(val);
        } else
        if (fmt.startsWith(FORMAT_TIME)) {
            if (val > 0L) {
                int p = fmt.indexOf(':');
                TimeZone tz = (p > 0)? DateTime.getTimeZone(fmt.substring(p+1)) : DateTime.getDefaultTimeZone();
                s = (new DateTime(val)).format("yyyy/MM/dd HH:mm:ss zzz",tz);
            } else {
                s = "";
            }
        } else
        if (fmt.startsWith("x") || fmt.startsWith("X")) {
            int byteLen = StringTools.parseInt(fmt.substring(1), -1);
            String hex = StringTools.toHexString(val, byteLen * 8); // uppercase
            if (fmt.startsWith("x")) { hex = hex.toLowerCase(); }
            s = "0x" + hex;
        } else
        if (fmt.startsWith("hex") || fmt.startsWith("HEX")) {
            int byteLen = StringTools.parseInt(fmt.substring(3), -1);
            String hex = StringTools.toHexString(val, byteLen * 8); // uppercase
            if (fmt.startsWith("h")) { hex = hex.toLowerCase(); }
            s = "0x" + hex;
        } else
        if (fmt.startsWith("%")) {
            try {
                s = String.format(fmt, new Object[] { val });
            } catch (Throwable th) {
                // IllegalFormatPrecisionException, IllegalFormatConversionException
                Print.logException("Format exception [" + fmt + "]", th);
                s = String.valueOf(val);
            }
        } else {
            s = StringTools._getFormatter(fmt).format(val);
        }
        if (fieldSize > s.length()) {
            s = StringTools.rightAlign(s, fieldSize);
        }
        return s;
    }

    /**
    *** Format/Convert the specified int value to a String, based on the specified format pattern
    *** @param val  The int value to format
    *** @param fmt  The format pattern
    *** @return The String containing the formatted int value
    **/
    public static String format(int val, String fmt)
    {
        return StringTools.format((long)val, fmt, -1);
    }

    /**
    *** Format/Convert the specified int value to a String, based on the specified format pattern
    *** @param val  The int value to format
    *** @param fmt  The format pattern
    *** @param fieldSize  The minimum formatted field width.
    *** @return The String containing the formatted int value
    **/
    public static String format(int val, String fmt, int fieldSize)
    {
        return StringTools.format((long)val, fmt, fieldSize);
    }

    /**
    *** Format/Convert the specified short value to a String, based on the specified format pattern
    *** @param val  The short value to format
    *** @param fmt  The format pattern
    *** @return The String containing the formatted short value
    **/
    public static String format(short val, String fmt)
    {
        return StringTools.format((long)val, fmt, -1);
    }

    /**
    *** Format/Convert the specified short value to a String, based on the specified format pattern
    *** @param val  The short value to format
    *** @param fmt  The format pattern
    *** @param fieldSize  The minimum formatted field width.
    *** @return The String containing the formatted short value
    **/
    public static String format(short val, String fmt, int fieldSize)
    {
        return StringTools.format((long)val, fmt, fieldSize);
    }

    // ------------------------------------------------------------------------
    // Probably should be in a module called 'ClassTools'

    /**
    *** Returns the class name for the specified object.  Does not return null.
    *** @param c  The object for which the class name is returned
    *** @return The class name of the specified object.  If the specified object is a Class object, 
    ***         then 'getName()' is used on the object to return the class name directly.
    **/
    public static String className(Object c)
    {
        if (c == null) {
            return "null";
        } else
        if (c instanceof Class) {
            Class clzz = (Class)c;
            if (clzz.isArray()) {
                Class elemClz = clzz.getComponentType();
                return elemClz.getName() + "[]";
            } else {
                return clzz.getName();
            }
        } else
        if (c.getClass().isArray()) {
            Class clzz = (Class)c.getClass();
            Class elemClz = clzz.getComponentType();
            return elemClz.getName() + "[]";
        } else {
            return c.getClass().getName();
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** For Debug purposes.  Prints the contents of the specified array
    *** @param m  The message title to print
    *** @param s  The array to print
    **/
    private static void printArray(String m, String s[])
    {
        Print.logInfo(m);
        for (int i = 0; i < s.length; i++) {
            Print.logInfo(i + ") " + s[i]);
        }
    }

    // ------------------------------------------------------------------------

    private static String BASE_DIGITS =  "0123456789abcdefghijklmnopqrstvwxyzABCDEFGHIJKLMNOPQRSTVWXYZ()[]{}<>!@#&-=_+:~";
    private static int    BASE_LEN    = BASE_DIGITS.length();

    /** 
    *** Obfuscate the specified long value into a String
    *** @param num The long value to obfuscate
    *** @return The obfuscated long String
    **/
    public static String compressDigits(long num)
    {
        return compressDigits(num, BASE_DIGITS);
    }

    /** 
    *** Compress/Obfuscate the specified long value into a String using the specified alphabet
    *** (Note: In this context "compress" means the length of the String representation, and not
    *** the number of byte required to represent the long value).
    *** @param num The long value to obfuscate
    *** @param alpha The alphabet used to compress/obfuscate the long value
    *** @return The compressed/obfuscated long String
    **/
    public static String compressDigits(long num, String alpha)
    {
        int alphaLen = alpha.length();
        StringBuffer sb = new StringBuffer();
        for (long v = num; v > 0; v /= alphaLen) {
            sb.append(alpha.charAt((int)(v % alphaLen)));
        }
        return sb.reverse().toString();
    }

    /** 
    *** Decompress/Unobfuscate the specified String into a long value.
    *** @param str The String from which the long value will be decompressed/unobfuscated
    *** @return The decompressed/unobfuscated long value
    **/
    public static long decompressDigits(String str)
    {
        return decompressDigits(str, BASE_DIGITS);
    }

    /** 
    *** Decompress/Unobfuscate the specified String into a long value using the specified
    *** alphabet (this must be the same alphabet used to encode the long value)
    *** @param str The String from which the long value will be decompressed/unobfuscated
    *** @param alpha The alphabet used to decompress/unobfuscate the long value (this must be
    ***              same alphabet used to compress/obfuscate the long value)
    *** @return The decompressed/unobfuscated long value
    **/
    public static long decompressDigits(String str, String alpha)
    {
        int alphaLen = alpha.length();
        long accum = 0L;
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            accum = (accum * alphaLen) + alpha.indexOf(ch);
        }
        return accum;
    }

    // ------------------------------------------------------------------------

    public static final String HTML_SP              = "&nbsp;";
    public static final String HTML_LT              = "&lt;";
    public static final String HTML_GT              = "&gt;";
    public static final String HTML_AMP             = "&amp;";
    public static final String HTML_DEG             = "&deg;";
    public static final String HTML_QUOTE           = "&quote;";
    public static final String HTML_DOUBLE_QUOTE    = HTML_QUOTE;
    public static final String HTML_APOS            = "&apos;";
    public static final String HTML_SINGLE_QUOTE    = HTML_APOS;
    public static final String HTML_BR              = "<BR>";
    public static final String HTML_HR              = "<HR>";

    /**
    *** Encode special HTML character string
    *** @param text The Object to encode [via 'toString()' method]
    *** @return     The encoded string.
    **/
    public static String htmlFilter(Object text)
    {
        String s = (text != null)? text.toString() : "";

        /* empty */
        if (s.length() == 0) {
            return "";
        }
        
        /* single space */
        if (s.equals(" ")) { 
            return HTML_SP;
        }

        /* encode special characters */
        int sp = 0; // adjacent space counter
        char ch[] = new char[s.length()];
        s.getChars(0, s.length(), ch, 0);
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < ch.length; i++) {
            if ((i == 0) && (ch[i] == ' ')) {
                sb.append(HTML_SP); // first character is a space
            } else 
            if ((i == (ch.length - 1)) && (ch[i] == ' ')) {
                sb.append(HTML_SP); // last character is a space
            } else {
                sp = (ch[i] == ' ')? (sp + 1) : 0; // count adjacent spaces
                switch (ch[i]) {
                    case '<' : sb.append(HTML_LT          ); break;
                    case '>' : sb.append(HTML_GT          ); break;
                    case '&' : sb.append(HTML_AMP         ); break;
                    case 176 : sb.append(HTML_DEG         ); break;
                  //case '"' : sb.append(HTML_DOUBLE_QUOTE); break;
                  //case '\'': sb.append(HTML_SINGLE_QUOTE); break;
                    case '\n': sb.append(HTML_BR          ); break;
                    case ' ' : sb.append(((sp & 1) == 0)? HTML_SP : " "); break; // every even space
                    default  : sb.append(ch[i]            ); break;
                }
            }
        }

        return sb.toString();
    }

    // ------------------------------------------------------------------------
    
    private static String RANDOM_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ.!@#&-=_+";

    /** 
    *** Creates a random String value with the specified length
    *** @param len  The resulting length of the returned String
    *** @return The String containing the random characters
    **/
    public static String createRandomString(int len)
    {
        return StringTools.createRandomString(len, RANDOM_CHARS);
    }

    /** 
    *** Creates a random String value with the specified length
    *** @param len  The resulting length of the returned String
    *** @param alpha The random characters will be pulled from this alphabet
    *** @return The String containing the random characters
    **/
    public static String createRandomString(int len, String alpha)
    {
        Random ran = new Random();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < len; i++) {
            sb.append(alpha.charAt(ran.nextInt(alpha.length())));
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    /**
    *** Main entry point, used for debugging
    ***/
    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        Print.logInfo("Current Character set: " + StringTools.getCharacterEncoding());
        
        /* display current CharacterSet */
        //if (RTConfig.hasProperty("charset")) {
        //    Print.sysPrintln("Character Set: %s", StringTools.getCharacterEncoding());                
        //    System.exit(0);
        //}

        /* hex test */
        String hex = RTConfig.getString("hex",null);
        if (hex != null) {
            byte b[] = StringTools.parseHex(hex, null);
            if (b != null) {
                byte sb[] = new byte[b.length];
                for (int i = 0; i < b.length; i++) {
                    if ((b[i] < ' ') || (b[i] > '~')) {
                        sb[i] = '.';
                    } else {
                        sb[i] = b[i];
                    }
                }
                Print.logInfo("String: " + StringTools.toStringValue(sb));
            } else {
                Print.logError("Invalid hex value");
            }
        }
        
    }

}
