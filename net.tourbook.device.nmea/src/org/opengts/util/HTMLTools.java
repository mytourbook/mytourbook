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
//  HTML Tools
// ----------------------------------------------------------------------------
// Change History:
//  2006/05/15  Martin D. Flynn
//     -Initial release
//  2008/02/27  Martin D. Flynn
//     -Added methods 'getMimeTypeFromExtension' and 'getMimeTypeFromData'
//  2008/03/28  Martin D. Flynn
//     -Added method 'inputStream_GET'.
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.awt.*;
import java.net.*;

/**
*** Various HTML and HTTP utilities
**/

public class HTMLTools
{

    // ------------------------------------------------------------------------

    public  static final String SP                  = StringTools.HTML_SP;
    public  static final String LT                  = StringTools.HTML_LT;
    public  static final String GT                  = StringTools.HTML_GT;
    public  static final String AMP                 = StringTools.HTML_AMP;
    public  static final String QUOTE               = StringTools.HTML_QUOTE;
    public  static final String BR                  = StringTools.HTML_BR;
    public  static final String HR                  = StringTools.HTML_HR;

    public  static final String HTML                = "<html>";

    public  static final String REQUEST_GET         = "GET";
    public  static final String REQUEST_POST        = "POST";

    // ------------------------------------------------------------------------

    public  static final byte   MAGIC_GIF_87a[]     = new byte[]{(byte)0x47,(byte)0x49,(byte)0x46,(byte)0x38,(byte)0x37,(byte)0x61}; // "GIF87a"
    public  static final byte   MAGIC_GIF_89a[]     = new byte[]{(byte)0x47,(byte)0x49,(byte)0x46,(byte)0x38,(byte)0x39,(byte)0x61}; // "GIF89a"
    public  static final byte   MAGIC_JPEG[]        = new byte[]{(byte)0xFF,(byte)0xD8,(byte)0xFF,(byte)0xE0};
    public  static final byte   MAGIC_PNG[]         = new byte[]{(byte)0x89,(byte)0x50,(byte)0x4E,(byte)0x47,(byte)0x0D,(byte)0x0A,(byte)0x1A,(byte)0x0A};

    // ------------------------------------------------------------------------

    public  static final String CHARSET_UTF8        = "charset=utf-8";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public  static final String PROP_User_Agent     = "User-Agent";

    public  static final String DEFAULT_USER_AGENT  = "HTMLTools/1.3";

    private static       String HTTPUserAgent       = null;

    /**
    *** Sets the HTTP "User-Agent" value to use for HTTP requests.
    *** @param userAgent  The user-agent
    **/
    public static void setHttpUserAgent(String userAgent)
    {
        HTTPUserAgent = userAgent;
    }

    /**
    *** Gets the HTTP "User-Agent" value to use for HTTP requests.
    *** @return  The user-agent
    **/
    public static String getHttpUserAgent()
    {
        if (!StringTools.isBlank(HTTPUserAgent)) {
            return HTTPUserAgent;
        } else {
            return RTConfig.getString(RTKey.HTTP_USER_AGENT, DEFAULT_USER_AGENT);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static final String CONTENT_TYPE_PLAIN   = "text/plain";
    public static final String CONTENT_TYPE_TEXT    = CONTENT_TYPE_PLAIN;
    public static final String CONTENT_TYPE_XML     = "text/xml";
    public static final String CONTENT_TYPE_HTML    = "text/html";
    public static final String CONTENT_TYPE_GIF     = "image/gif";  // "GIF87a", "GIF89a"
    public static final String CONTENT_TYPE_JPEG    = "image/jpeg"; // 0xFF,0xD8,0xFF,0xE0
    public static final String CONTENT_TYPE_PNG     = "image/png";  // 0x89,0x50,0x4E,0x47,0x0D,0x0A,0x1A,0x0A
    public static final String CONTENT_TYPE_OCTET   = "application/octet-stream";
    public static final String CONTENT_TYPE_DOC     = "application/msword";
    public static final String CONTENT_TYPE_XLS     = "application/vnd.ms-excel";
    public static final String CONTENT_TYPE_CSV_OLD = "text/comma-separated-values";
    public static final String CONTENT_TYPE_CSV     = "text/csv";   // RFC-4180 [http://tools.ietf.org/html/rfc4180]
    public static final String CONTENT_TYPE_PDF     = "application/pdf";
    public static final String CONTENT_TYPE_PS      = "application/postscript";
    public static final String CONTENT_TYPE_ZIP     = "application/x-zip-compressed";
    public static final String CONTENT_TYPE_JAD     = "text/vnd.sun.j2me.app-descriptor";
    public static final String CONTENT_TYPE_JAR     = "application/java-archive";
    public static final String CONTENT_TYPE_KML     = "application/vnd.google-earth.kml+xml kml";

    /**
    *** Appends the default "charset" to mime type
    *** @param type     The Mime type
    *** @return The combined mime-type/charset
    **/
    public static String getMimeTypeCharset(String type)
    {
        return HTMLTools.getMimeTypeCharset(type, StringTools.getCharacterEncoding());
    }

    /**
    *** Appends the specified "charset" to mime type
    *** @param type     The Mime type
    *** @param charSet  The 'charset'
    *** @return The combined mime-type/charset
    **/
    public static String getMimeTypeCharset(String type, String charSet)
    {
        if (StringTools.isBlank(type) || StringTools.isBlank(charSet)) {
            return type;
        } else {
            return type + "; charset=" + charSet;
        }
    }
    
    /**
    *** Returns the MIME type for the specified extension
    *** @param extn  The extension
    *** @return The MIME type (default to CONTENT_TYPE_OCTET if the extension is not recognized)
    **/
    public static String getMimeTypeFromExtension(String extn)
    {
        return getMimeTypeFromExtension(extn, CONTENT_TYPE_OCTET);
    }

    /**
    *** Returns the MIME type for the specified extension
    *** @param extn  The extension
    *** @param dft   The default MIME type to return if the extension is not recognized
    *** @return The MIME type
    **/
    public static String getMimeTypeFromExtension(String extn, String dft)
    {
        if (extn == null) {
            return dft;
        } else
        if (extn.equalsIgnoreCase("gif")) {
            return CONTENT_TYPE_GIF;
        } else
        if (extn.equalsIgnoreCase("jpeg") || extn.equalsIgnoreCase("jpg")) {
            return CONTENT_TYPE_JPEG;
        } else
        if (extn.equalsIgnoreCase("png")) {
            return CONTENT_TYPE_PNG;
        } else
        if (extn.equalsIgnoreCase("js")) {
            return CONTENT_TYPE_PLAIN;
        } else
        if (extn.equalsIgnoreCase("txt")) {
            return CONTENT_TYPE_PLAIN;
        } else
        if (extn.equalsIgnoreCase("xml")) {
            return CONTENT_TYPE_XML;
        } else
        if (extn.equalsIgnoreCase("html") || extn.equalsIgnoreCase("htm")) {
            return CONTENT_TYPE_HTML;
        } else
        if (extn.equalsIgnoreCase("pdf")) {
            return CONTENT_TYPE_PDF;
        } else {
            return dft;
        }
        // add additional as needed
    }

    /**
    *** Return the MIME type based on the data Magic Number
    *** @param data The data buffer to test for specific Magin-Numbers
    *** @return The MIME type (default to CONTENT_TYPE_OCTET if data is not recognized)
    **/
    public static String getMimeTypeFromData(byte data[])
    {
        return getMimeTypeFromData(data, CONTENT_TYPE_OCTET);
    }

    /**
    *** Return the MIME type based on the data Magic Number
    *** @param data  The data buffer to test for specific Magin-Numbers
    *** @param dft   The default MIME type to return if the data is not recognized
    *** @return The MIME type
    **/
    public static String getMimeTypeFromData(byte data[], String dft)
    {
        
        /* invalid data? */
        if (data == null) {
            return dft;
        }

        /* GIF */
        if (StringTools.compareEquals(data,MAGIC_GIF_87a,-1)) {
            return CONTENT_TYPE_GIF;
        } else
        if (StringTools.compareEquals(data,MAGIC_GIF_89a,-1)) {
            return CONTENT_TYPE_GIF;
        }

        /* JPEG */
        if (StringTools.compareEquals(data,MAGIC_JPEG,-1)) { // ([6..10]=="JFIF")
            return CONTENT_TYPE_JPEG;
        }

        /* PNG */
        if (StringTools.compareEquals(data,MAGIC_PNG,-1)) {
            return CONTENT_TYPE_PNG;
        }
        
        /* default */
        return dft;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static final TagBlock TAG_HTML           = new TagBlock("<html>", "</html>");
    public static final TagBlock TAG_H1             = new TagBlock("<h1>", "</h1>");
    public static final TagBlock TAG_H2             = new TagBlock("<h2>", "</h2>");
    public static final TagBlock TAG_H3             = new TagBlock("<h3>", "</h3>");
    public static final TagBlock TAG_CENTER         = new TagBlock("<center>", "</center>");
    public static final TagBlock TAG_BLOCKQUOTE     = new TagBlock("<blockquote>", "</blockquote>");
    public static final TagBlock TAG_RIGHT          = new TagBlock("<div align=right>", "</div>");
    public static final TagBlock TAG_BOLD           = new TagBlock("<B>", "</B>");
    public static final TagBlock TAG_ITALIC         = new TagBlock("<I>", "</I>");
    public static final TagBlock TAG_MONOSPACE      = new TagBlock("<tt>", "</tt>");
    public static final TagBlock TAG_MONORIGHT      = new TagBlock(TAG_MONOSPACE, TAG_RIGHT);
    public static final TagBlock TAG_MONOCENTER     = new TagBlock(TAG_MONOSPACE, TAG_CENTER);
    public static final TagBlock TAG_RED            = TAG_COLOR("#FF0000");
    public static final TagBlock TAG_REDBOLD        = new TagBlock(TAG_BOLD, TAG_RED);
    public static final TagBlock TAG_GREEN          = TAG_COLOR("#007700");
    public static final TagBlock TAG_GREENBOLD      = new TagBlock(TAG_BOLD, TAG_GREEN);
    public static final TagBlock TAG_BLUE           = TAG_COLOR("#0000FF");
    public static final TagBlock TAG_BLUEBOLD       = new TagBlock(TAG_BOLD, TAG_BLUE);
    public static final TagBlock TAG_SMALLFONT      = new TagBlock("<font size=-1>", "</font>");
    public static final TagBlock TAG_SMALLCENTER    = new TagBlock(TAG_SMALLFONT, TAG_CENTER);
    public static final TagBlock TAG_NUMBERLIST     = new TagBlock("<ol>", "</ol>");
    public static final TagBlock TAG_BULLETLIST     = new TagBlock("<ul>", "</ul>");
    public static final TagBlock TAG_LISTITEM       = new TagBlock("<li>", "</li>");

    /**
    *** Returns a TagBlock 'font' wrapper with the specified color
    *** @param c  The font color
    *** @return The TagBlock 'font' wrapper
    **/
    public static TagBlock TAG_COLOR(Color c)
    {
        return TAG_COLOR(ColorTools.toHexString(c));
    }

    /**
    *** Returns a TagBlock 'font' wrapper with the specified color String
    *** @param cs  The font color String
    *** @return The TagBlock 'font' wrapper
    **/
    public static TagBlock TAG_COLOR(String cs)
    {
        if ((cs != null) && !cs.startsWith("#")) { cs = "#" + cs; }
        return new TagBlock("<font color=\"" + cs + "\">", "</font>");
    }

    /**
    *** Returns a TagBlock 'span' wrapper with the specified title/tooltip
    *** @param tip  The title/tooltip
    *** @return The TagBlock 'span' wrapper
    **/
    public static TagBlock TAG_TOOLTIP(String tip)
    {
        String t = StringTools.quoteString(tip);
        return new TagBlock("<span title=" + t + ">", "</span>");
    }

    /**
    *** Returns a TagBlock 'a href=' wrapper with the specified url
    *** @param url  The URL
    *** @return The TagBlock 'a' wrapper
    **/
    public static TagBlock TAG_LINK(String url)
    {
        return new TagBlock("<a href=\"" + url + "\">", "</a>");
    }

    /**
    *** Returns a TagBlock 'a href=' wrapper with the specified url
    *** @param url  The URL
    *** @param newWindow True to specify "target=_blank"
    *** @return The TagBlock 'a' wrapper
    **/
    public static TagBlock TAG_LINK(String url, boolean newWindow)
    {
        StringBuffer a = new StringBuffer();
        a.append("<a href=\"").append(url).append("\"");
        if (newWindow) { a.append(" target='_blank'"); }
        a.append(">");
        return new TagBlock(a.toString(), "</a>");
    }

    // ------------------------------------------------------------------------

    /* encode HTML parameter */
    private static final String ENC_CHARS = " %=<>&'\"";

    /**
    *** Encode specific characters in the specified text object
    *** @param text  The text Object
    *** @return The encoded String
    **/
    public static String parameter(Object text)
    {
        String s = text.toString();

        /* encode special characters */
        char ch[] = new char[s.length()];
        s.getChars(0, s.length(), ch, 0);
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < ch.length; i++) {
            if (ENC_CHARS.indexOf(ch[i]) >= 0) {
                int x = (int)ch[i] + 0x100;
                sb.append("%" + Integer.toHexString(x).substring(1).toUpperCase());
            } else {
                sb.append(ch[i]);
            }
        }

        return sb.toString();
    }

    // ------------------------------------------------------------------------

    /**
    *** Encode the specified color into a String value
    *** @param color  The Color object
    *** @return The String representation of the color
    **/
    public static String color(Color color)
    {
        int ci = 0x000000;
        ci |= (color.getRed()   << 16) & 0xFF0000;
        ci |= (color.getGreen() <<  8) & 0x00FF00;
        ci |= (color.getBlue()  <<  0) & 0x0000FF;
        String cs = Integer.toHexString(ci | 0x1000000).substring(1).toUpperCase();
        return "#" + cs;
    }

    // ------------------------------------------------------------------------

    /**
    *** Creates/Returns a String representing a link to a URL with the specified text description
    *** and attributes.
    *** @param ref        The referenced URL link ("target='_blank'" will be applied)
    *** @param text       The link text (html filtering will be applied)
    *** @return The assembled link ('a' tag)
    **/
    public static String createLink(String ref, Object text)
    {
        return HTMLTools.createLink(ref, true, text, true);
    }

    /**
    *** Creates/Returns a String representing a link to a URL with the specified text description
    *** and attributes.
    *** @param ref        The referenced URL link
    *** @param newWindow  If true, "target='_blank'" will be specified
    *** @param text       The link text (html filtering will be applied)
    *** @return The assembled link ('a' tag)
    **/
    public static String createLink(String ref, boolean newWindow, Object text)
    {
        return createLink(ref, newWindow, text, true);
    }

    /**
    *** Creates/Returns a String representing a link to a URL with the specified text description
    *** and attributes.
    *** @param ref        The referenced URL link
    *** @param newWindow  If true, "target='_blank'" will be specified
    *** @param text       The link text
    *** @param filterText True to apply HTML filtering to the specified text
    *** @return The assembled link ('a' tag)
    **/
    public static String createLink(String ref, boolean newWindow, Object text, boolean filterText)
    {
        StringBuffer sb = new StringBuffer();
        sb.append("<a href='").append(ref).append("'");
        if (newWindow) { sb.append(" target='_blank'"); }
        sb.append(">");
        sb.append(filterText? StringTools.htmlFilter(text.toString()) : text.toString());
        sb.append("</a>");
        return sb.toString();
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a 'span' wrapper with the specified 'font' specification
    *** @param family  The font family
    *** @param style   The font style ('normal', 'italic', 'oblique')
    *** @param variant The font variant ('normal', 'small-caps')
    *** @param weight  The font weight ('normal', 'bold', 'bolder', 'lighter', etc)
    *** @param ptsize  The font point size
    *** @return The 'font' tag specification
    ***/
    public static String font(String family, String style, String variant, String weight, int ptsize)
    {
        // Reference: http://www.w3.org/TR/CSS21/fonts.html
        StringBuffer sb = new StringBuffer();
        sb.append("<span style='");
        if ((family != null) && !family.equals("")) {
            // "Arial,Helvetica,san-serif"
            sb.append("font-family:").append(family).append(";");
        }
        if ((style != null) && !style.equals("")) {
            // "normal", "italic", "oblique"
            sb.append("font-style:").append(style).append(";");
        }
        if ((variant != null) && !variant.equals("")) {
            // "normal", "small-caps"
            sb.append("font-variant:").append(variant).append(";");
        }
        if ((weight != null) && !weight.equals("")) {
            // "normal"(400), "bold"(700), "bolder", "lighter", "100".."900"
            sb.append("font-weight:").append(weight).append(";");
        }
        if (ptsize > 0) {
            sb.append("font-size:").append(ptsize).append("pt;");
        }
        sb.append("'>");
        return sb.toString();
    }

    /**
    *** Closes a 'span' tag used to specify 'font' style.
    **/
    public static String _font()
    {
        return "</span>";
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a String representing a start-of-JavaScript tag
    ***/
    public static String startJavaScript()
    {
        return "<script language='JavaScript'><!--";
    }

    /**
    *** Returns a String representing an end-of-JavaScript tag
    ***/
    public static String endJavaScript()
    {
        return "// --></script>";
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a 'meta' tag with a 'refresh' option
    *** @param delay  Delay before next update 
    *** @param url    The URL to jump to after the delay
    *** @return  The String representing the assembled 'meta' tag
    ***/
    public static String autoRefresh(int delay, String url)
    {
        StringBuffer sb = new StringBuffer();
        sb.append("<meta http-equiv='Refresh' ");
        sb.append("content='").append(delay).append(";URL=").append(url).append("'>");
        return sb.toString();
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Sends a POST to the specified URL, then reads and returns the response
    *** @param pageURL      The URL to which the POST is sent
    *** @param contentType  The MIME type of the POST data sent to the server
    *** @param postData     The data sent to the server
    *** @return The response from the server
    ***/
    public static byte[] readPage_POST(String pageURL, String contentType, byte postData[])
        throws IOException
    {
        byte data[] = null;
        HttpURLConnection httpConnect = null;
        OutputStream postOutput = null;
        InputStream  postInput = null;
        try {
            //Print.logInfo("POST v1.2");
            
            /* init connection */
            httpConnect = (HttpURLConnection)((new URL(pageURL)).openConnection());
            httpConnect.setRequestMethod(REQUEST_POST);
            httpConnect.setAllowUserInteraction(false);
            httpConnect.setDoInput(true);
            httpConnect.setDoOutput(true);
            httpConnect.setUseCaches(false);
            httpConnect.setRequestProperty(PROP_User_Agent, HTMLTools.getHttpUserAgent());
            
            /* write data */
            if (postData != null) {
                if ((contentType != null) && !contentType.equals("")) {
                    httpConnect.setRequestProperty("Content-Type", contentType);
                }
                httpConnect.setRequestProperty("Content-Length", String.valueOf(postData.length));
                postOutput = httpConnect.getOutputStream();
                postOutput.write(postData);
                postOutput.flush();
            }

            /* connect */
            httpConnect.connect(); // possible NoRouteToHostException, etc.
            
            /* read data */
            //int contentLen = httpConnect.getContentLength();
            //Print.logInfo("Read ContentLength: " + contentLen);
            //if ((contentLen > 0) || (contentLen == -1)) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            postInput = new BufferedInputStream(httpConnect.getInputStream());
            FileTools.copyStreams(postInput, output);
            data = output.toByteArray();
            //} else {
            //    data = new byte[0];
            //}
            
        } finally {
            
            /* close */
            if (postOutput  != null) { postOutput.close(); }
            if (postInput   != null) { postInput.close(); }
            if (httpConnect != null) { httpConnect.disconnect(); }

        }
        return data;
    }

    /**
    *** Sends a GET to the specified URL, then reads and returns the response
    *** @param pageURL The URL to which the GET is sent
    *** @return The response from the server
    ***/
    public static byte[] readPage_GET(String pageURL)
        throws Throwable
    {
        byte data[] = null;
        HttpURLConnection httpConnect = null;
        try {            
           
            /* init connection */
            httpConnect = (HttpURLConnection)((new URL(pageURL)).openConnection());
            httpConnect.setAllowUserInteraction(false);
            httpConnect.setRequestMethod(REQUEST_GET);
            httpConnect.setRequestProperty(PROP_User_Agent, HTMLTools.getHttpUserAgent());

            /* connect */
            //Print.logDebug("Connecting ...");
            httpConnect.connect(); // possible NoRouteToHostException, etc.
            //Print.logDebug("Connected.");
            
            /* read data */
            int contentLen = httpConnect.getContentLength();
            if ((contentLen > 0) || (contentLen == -1)) {
                data = FileTools.readStream(new BufferedInputStream(httpConnect.getInputStream()));
            } else {
                data = new byte[0];
            }
            
        } finally {
            if (httpConnect != null) { httpConnect.disconnect(); }
        }
        return data;
    }
    
    /**
    *** Sends a GET to the specified URL, then reads and returns the response.  This method
    *** will not throw any Exceptions.  Instead, the returned response is null if any errors
    *** are encountered.
    *** @param pageURL The URL to which the GET is sent
    *** @return The response from the server
    ***/
    public static byte[] readPage_GET_LogError(String pageURL)
    {
        try {
            return HTMLTools.readPage_GET(pageURL);
        } catch (UnknownHostException uhe) {
            Print.logError(uhe.toString()); // DNS?
        } catch (NoRouteToHostException nrthe) {
            Print.logError(nrthe.toString()); // DNS?
        } catch (ConnectException ce) {
            Print.logError(ce.toString()); // timed out?
        } catch (SocketException se) {
            Print.logError(se.toString()); // DNS?
        } catch (FileNotFoundException fnfe) {
            Print.logError(fnfe.toString()); // wrong URL?
        } catch (Throwable t) {
            Print.logStackTrace("Read page", t);
        }
        return null;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns an InputStream for reading the contents of the specified URL
    *** @param pageURL  The URL
    *** @return The InputStream
    **/
    public static InputStream inputStream_GET(String pageURL)
        throws IOException
    {
           
        /* init connection */
        final HttpURLConnection httpConnect = (HttpURLConnection)((new URL(pageURL)).openConnection());
        httpConnect.setAllowUserInteraction(false);
        httpConnect.setRequestMethod(REQUEST_GET);
        httpConnect.setRequestProperty(PROP_User_Agent, HTMLTools.getHttpUserAgent());

        /* connect */
        httpConnect.connect(); // possible NoRouteToHostException, etc.

        /* read data */
        int contentLen = httpConnect.getContentLength();
        if ((contentLen > 0) || (contentLen == -1)) {
            return new BufferedInputStream(httpConnect.getInputStream()) {
                public void close() throws IOException {
                    //Print.logInfo("Closing HTTP stream ...");
                    httpConnect.disconnect();
                }
            };
        } else {
            httpConnect.disconnect();
            return null;
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Custom TagBlock class
    **/
    public static class TagBlock
    {
        private String   startTag   = null;
        private String   endTag     = null;
        private TagBlock tagGroup[] = null;
        public TagBlock(String startTag, String endTag) {
            this.startTag = startTag;
            this.endTag   = endTag;
        }
        public TagBlock(TagBlock group[]) {
            this((String)null, (String)null);
            this.tagGroup = group;
        }
        public TagBlock(TagBlock tb1, TagBlock tb2) {
            this(new TagBlock[] { tb1, tb2 });
        }
        public TagBlock(TagBlock tb1, TagBlock tb2, TagBlock tb3) {
            this(new TagBlock[] { tb1, tb2, tb3 });
        }
        public String getStartTag() {
            StringBuffer sb = new StringBuffer();
            if (this.tagGroup != null) {
                for (int i = 0; i < this.tagGroup.length; i++) {
                    if (this.tagGroup[i] != null) {
                        sb.append(this.tagGroup[i].getStartTag());
                    }
                }
            }
            if (this.startTag != null) {
                sb.append(this.startTag);
            }
            return sb.toString();
        }
        public String getEndTag() {
            StringBuffer sb = new StringBuffer();
            if (this.endTag != null) {
                sb.append(this.endTag);
            }
            if (this.tagGroup != null) {
                for (int i = (this.tagGroup.length - 1); i >= 0; i--) {
                    if (this.tagGroup[i] != null) {
                        sb.append(this.tagGroup[i].getEndTag());
                    }
                }
            }
            return sb.toString();
        }
        public StringBuffer wrap(Object text, boolean htmlFilter, StringBuffer sb) {
            if (sb == null) { sb = new StringBuffer(); }
            if (text != null) {
                String v = text.toString();
                sb.append(this.getStartTag());
                sb.append(htmlFilter? StringTools.htmlFilter(v) : v);
                sb.append(this.getEndTag());
            }
            return sb;
        }
        public String wrap(Object text, boolean htmlFilter) {
            return this.wrap(text, htmlFilter, new StringBuffer()).toString();
        }
        public String wrap(Object text) {
            return this.wrap(text, false);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Custom tag wrapper class
    **/
    public static class TagWrap
    {
        private Object text = null;
        private TagBlock tags[] = null;
        public TagWrap(Object text, TagBlock tags[]) {
            this.text = text;
            this.tags = tags;
        }
        public TagWrap(Object text, TagBlock tag) {
            this(text, new TagBlock[] { tag });
        }
        public String toString(boolean html) {
            if (this.text != null) {
                if (html) {
                    String v = (this.text instanceof TagWrap)? ((TagWrap)this.text).toString(html) : StringTools.htmlFilter(this.text);
                    if ((this.tags != null) && (this.tags.length > 0)) {
                        for (int i = 0; i < this.tags.length; i++) {
                            if (this.tags[i] != null) { v = this.tags[i].wrap(v); }
                        }
                    }
                    return v;
                } else {
                    return this.text.toString();
                }
            } else {
                return "";
            }
        }
        public String toString() {
            return this.toString(false);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Creates an IField/ILayer section.
    *** @param srcURL  The source URL displayed within the frame
    *** @param sb      The destination StringBuffer
    *** @return The StringBuffer where the created IFrame/ILayer section is placed
    **/
    public static StringBuffer createIFrameTemplate(String srcURL, StringBuffer sb)
    {
        return HTMLTools.createIFrameTemplate(-1, -1, srcURL, sb);
    }

    /**
    *** Creates an IField/ILayer section.
    *** @param srcURL  The source URL displayed within the frame
    *** @return The String containing the IFrame/ILayer section
    **/
    public static String createIFrameTemplate(String srcURL)
    {
        return HTMLTools.createIFrameTemplate(-1, -1, srcURL, null).toString();
    }

    /**
    *** Creates an IField/ILayer section.
    *** @param w       The width of the IFrame
    *** @param h       The height of the IFrame
    *** @param srcURL  The source URL displayed within the frame
    *** @return The String containing the IFrame/ILayer section
    **/
    public static String createIFrameTemplate(int w, int h, String srcURL)
    {
        return HTMLTools.createIFrameTemplate(w, h, srcURL, null).toString();
    }

    /**
    *** Creates an IField/ILayer section.
    *** @param w       The width of the IFrame
    *** @param h       The height of the IFrame
    *** @param srcURL  The source URL displayed within the frame
    *** @param sb      The destination StringBuffer
    *** @return The StringBuffer where the created IFrame/ILayer section is placed
    **/
    public static StringBuffer createIFrameTemplate(int w, int h, String srcURL, StringBuffer sb)
    {
        if (sb == null) { sb = new StringBuffer(); }
        String id = "id.iframe";
        String ws = (w > 0)? String.valueOf(w) : "100%";
        String hs = (h > 0)? String.valueOf(h) : "100%";
        //String style = "STYLE='{visibility=visible;width=" + w + "px;height=" + h + "px}' ";

        sb.append("<IFRAME WIDTH='"+ws+"' HEIGHT='"+hs+"' FRAMEBORDER='0' ID='"+id+"' SRC='"+srcURL+"'>\n");
        sb.append("<ILAYER WIDTH='"+ws+"' HEIGHT='"+hs+"' FRAMEBORDER='0' ID='"+id+"' SRC='"+srcURL+"'>\n");
        sb.append("InternetExplorer or Netscape is required to view this page\n");
        sb.append("</ILAYER>\n");
        sb.append("</IFRAME>\n");

        return sb;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Main test/debug entry point 
    **/
    public static void main(String argv[])
    {
        try {
            String url = "http://localhost:8080";
            byte data[] = readPage_POST(url, null, "Hello World".getBytes());
            Print.logInfo("Data: " + new String(data));
        } catch (Throwable t) {
            Print.logException("Error",t);
        }
    }

}
