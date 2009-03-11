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
//  URI argument wrapper
// ----------------------------------------------------------------------------
// Change History:
//  2006/05/15  Martin D. Flynn
//     -Initial release
//  2008/05/14  Martin D. Flynn
//     -Added method 'obfuscateArg'.
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.awt.*;
import java.net.*;

public class URIArg
{
    
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified character should be hex-encoded in a URL
    *** @param ch  The character to test
    *** @return True if the specified character should be hex-encoded in a URL
    **/
    private static boolean shouldEncodeArgChar(char ch)
    {
        if (Character.isLetterOrDigit(ch)) {
            return false;
        } else
        if ((ch == '_') || (ch == '-') || (ch == '.')) {
            return false;
        } else {
            return true;
        }
    }
    
    /**
    *** Hex-encodes a URL argument (if required)
    *** @param sb   The StringBuffer where the hex encoded String argument will be placed
    *** @param s    The URL argument to encode (if required)
    *** @return The StringBuffer where the hex-encoded String will be placed
    **/
    public static StringBuffer encodeArg(StringBuffer sb, String s)
    {
        return URIArg.encodeArg(sb, s, false);
    }

    /**
    *** Hex-encodes a URL argument
    *** @param sb   The StringBuffer where the hex encoded String argument will be placed
    *** @param s    The URL argument to encode
    *** @param obfuscateAll  True to force hex-encoding on all argument characters
    *** @return The StringBuffer where the hex-encoded String will be placed
    **/
    public static StringBuffer encodeArg(StringBuffer sb, String s, boolean obfuscateAll)
    {
        if (sb == null) { sb = new StringBuffer(); }
        if (s != null) {
            char ch[] = new char[s.length()];
            s.getChars(0, s.length(), ch, 0);
            for (int i = 0; i < ch.length; i++) {
                if (obfuscateAll || URIArg.shouldEncodeArgChar(ch[i])) {
                    // escape non-alphanumeric characters
                    sb.append("%");
                    sb.append(Integer.toHexString(0x100 + (ch[i] & 0xFF)).substring(1));
                } else {
                    // letters and digits are ok as-is
                    sb.append(ch[i]);
                }
            }
        }
        return sb;
    }

    /**
    *** Obfuscates (hex-encodes) all characters in the String
    *** @param s  The String to hex-encode
    *** @return The hex-encoded String
    **/
    public static String obfuscateArg(String s)
    {
        return URIArg.encodeArg(new StringBuffer(),s,true).toString();
    }
    
    // ------------------------------------------------------------------------
    
    /**
    *** Decodes the specified hex-encoded argument (not yet fully tested)
    *** @param sb   The StringBuffer where the decoded String argument will be placed
    *** @param s    The String to decode
    *** @return The StringBuffer where the decoded String will be placed
    **/
    public static StringBuffer decodeArg(StringBuffer sb, String s)
    {
        if (sb == null) { sb = new StringBuffer(); }
        if (s != null) {
            char ch[] = new char[s.length()];
            s.getChars(0, s.length(), ch, 0);
            for (int i = 0; i < ch.length; i++) {
                if (ch[i] == '%') {
                    if ((i + 2) < ch.length) {
                        sb.append((char)(((StringTools.hexIndex(ch[i+1]) << 8) | StringTools.hexIndex(ch[i+2])) & 0xFF));
                        i += 2;
                    } else {
                        i = ch.length - 1;
                    }
                } else {
                    sb.append(ch[i]);
                }
            }
        }
        return sb;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private String                  uri  = "";
    private java.util.List<KeyVal>  keys = null;
    
    public URIArg(URIArg uriArg) 
    {
        this._setURI(uriArg.uri);
        this.setKeys(uriArg.keys);
    }
    
    public URIArg(String uri) 
    {
        this.setURI(uri);
    }
    
    // ------------------------------------------------------------------------

    public void setURI(String uri) 
    {
        int p = (uri != null)? uri.indexOf("?") : -1;
        if (p >= 0) {
            this._setURI(uri.substring(0, p));
            String a[] = StringTools.parseString(uri.substring(p+1), "&");
            for (int i = 0; i < a.length; i++) {
                String key = "", val = "";
                int e = a[i].indexOf("=");
                if (e >= 0) {
                    key = a[i].substring(0,e);
                    val = a[i].substring(e+1);
                } else {
                    key = a[i];
                    val = "";
                }
                this._addArg(key, val, false, false); // assume already encoded
            }
        } else {
            this._setURI(uri);
        }
    }
    
    protected void _setURI(String uri)
    {
        this.uri = (uri != null)? uri : "";
    }

    public String getURI()
    {
        return this.uri;
    }
    
    public void addExtension(String ext)
    {
        if (!StringTools.isBlank(ext) && !this.uri.endsWith(ext)) {
            this.uri += ext;
        }
    }

    // ------------------------------------------------------------------------

    public void setKeys(java.util.List<KeyVal> k)
    {
        this.keys = null;
        if (k != null) {
            this.getKeys().addAll(k);
        }
    }

    public java.util.List<KeyVal> getKeys()
    {
        if (this.keys == null) {
            this.keys = new Vector<KeyVal>();
        }
        return this.keys;
    }

    // ------------------------------------------------------------------------

    public URIArg addArg(String key, double value) 
    {
        return this._addArg(key, String.valueOf(value), true, false);
    }
    
    public URIArg addArg(String key, int value) 
    {
        return this._addArg(key, String.valueOf(value), true, false);
    }
    
    public URIArg addArg(String key, long value) 
    {
        return this._addArg(key, String.valueOf(value), true, false);
    }
    
    public URIArg addArg(String key, String value)
    {
        return this._addArg(key, value, true, false);
    }
    
    public URIArg addArg(String key, String value, boolean obfuscate)
    {
        return this._addArg(key, value, true, obfuscate);
    }

    protected URIArg _addArg(String key, String value, boolean encode, boolean obfuscate)
    {
        if (!StringTools.isBlank(key) && (value != null)) {
            String val = encode? this.encodeArg(value,obfuscate) : value;
            this.getKeys().add(new KeyVal(key,val));
        }
        return this;
    }
    
    // ------------------------------------------------------------------------

    /* return true if this URI contains the specified argument */
    public boolean hasArg(String key)
    {
        // TODO: optimize
        if (key != null) {
            for (Iterator i = this.getKeys().iterator(); i.hasNext();) {
                KeyVal kv = (KeyVal)i.next();
                if (kv.getKey().equals(key)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    // ------------------------------------------------------------------------

    /* if the specified argument exists, then set the specified value and return true */
    public boolean setArgValue(String key, int value)
    {
        return this.setArgValue(key, String.valueOf(value), false);
    }

    /* if the specified argument exists, then set the specified value and return true */
    public boolean setArgValue(String key, String value)
    {
        return this.setArgValue(key, value, false);
    }
    
    /* if the specified argument exists, then set the specified value and return true */
    public boolean setArgValue(String key, String value, boolean obfuscate)
    {
        if (key != null) {
            for (Iterator i = this.getKeys().iterator(); i.hasNext();) {
                KeyVal kv = (KeyVal)i.next();
                if (kv.getKey().equals(key)) {
                    kv.setValue(this.encodeArg(value,obfuscate));
                    return true;
                }
            }
        }
        return false;
    }
    
    // ------------------------------------------------------------------------

    public String toString()
    {
        StringBuffer sb = new StringBuffer(this.getURI());
        if (!ListTools.isEmpty(this.getKeys())) {
            sb.append("?");
            for (Iterator i = this.getKeys().iterator(); i.hasNext();) {
                KeyVal kv = (KeyVal)i.next();
                sb.append(kv.toString());
                if (i.hasNext()) {
                    sb.append("&");
                }
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------------

    public URL toURL()
        throws MalformedURLException 
    {
        return new URL(this.toString());
    }

    private String encodeArg(String s, boolean obfuscateAll)
    {
        StringBuffer sb = URIArg.encodeArg(null, s, obfuscateAll);
        return sb.toString();
    }

    private String decodeArg(String s)
    {
        StringBuffer sb = URIArg.decodeArg(null, s);
        return sb.toString();
    }

    // ------------------------------------------------------------------------

    public static class KeyVal
    {
        private String key = null;
        private String val = null;
        public KeyVal(String key, String val) {
            this.key = (key != null)? key : "";
            this.val = (val != null)? val : "";
        }
        public String getKey() {
            return this.key;
        }
        public String getValue() {
            return this.val;
        }
        public void setValue(String val) {
            this.val = (val != null)? val : "";
        }
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(this.key);
            String v = this.val;
            if ((v != null) && !v.equals("")) {
                sb.append("=").append(v);
            }
            return sb.toString();
        }
        public boolean equals(Object other) {
            if (other instanceof KeyVal) {
                return this.toString().equals(other.toString());
            } else {
                return false;
            }
        }
    }
    
    // ------------------------------------------------------------------------

}