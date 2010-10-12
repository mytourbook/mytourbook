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
//  Runtime properties container
// ----------------------------------------------------------------------------
// This class supports including other runtime property files.
// Notes: 
// -- Files will be included at the point they are specified.  Any values specified
//    in the included file which have already been defined will be overwritten.
// -- Included files must be specified in URL form, as in the following examples:
//       %include=file:/home/user/some.conf
//       %include=http:/server:8080/dir/some.conf
//    Optional included files may be specified as follows:
//       %include?=file:/home/user/some.conf
//       %include?=http:/server:8080/dir/some.conf
//    If an include file is required, and the file/url does not exist, an exception will
//    be thrown.  If the include is optional, and the file/url does not exist, then the
//    include will be quietly ignored.
// -- Relative URLs may also be specified.  Relative references will be resolved relative
//    to the URL which included the current file.  The relative URL must include the protocol.  
//    Thus relative file URLs may be specified as:
//       file:dir/file.conf
//    and relative http[s] URLs may be specified as:
//       http:dir/file.conf
// -- Replacement variables may be used, however, since the 'include' file is resolved at
//    the point where it is placed in the config file, the reference property keys must
//    already be preveiously defined, either in the current file, a parent file, on the
//    command-line, in an environment variable, or in a Java system property.
// -- Recursive config file inclusions is allow up to at least up to 3 levels deep.
//    That is, the main file can include a child config file, which can include another
//    child config file.  Beyond that, an error may be generated.   File-based property
//    definitions may include http-based property definitions, however, http-based
//    property definitions may not include file-based property definitions.
// ----------------------------------------------------------------------------
// Change History:
//  2006/03/26  Martin D. Flynn
//     -Initial release
//  2006/04/02  Martin D. Flynn
//     -Added ability to separate command-line key/value pairs with a ':'.
//  2006/04/23  Martin D. Flynn
//     -Integrated logging changes made to Print
//  2006/06/30  Martin D. Flynn
//     -Repackaged
//  2007/07/27  Martin D. Flynn
//     -Added support for primitive array types
//  2007/08/09  Martin D. Flynn
//     -Added support for URL and InputStream initializers.
//  2007/09/16  Martin D. Flynn
//     -Added method 'insertKeyValues'
//     -Added support for key/value replace in config-file value strings
//  2008/05/14  Martin D. Flynn
//     -Added 'setProperties(String props, char propSep)' method.
//     -Added 'PropertyChangeListener' support
//  2008/06/20  Martin D. Flynn
//     -Removed 'System.getenv' checking (moved to RTConfig.java)
//  2008/07/08  Martin D. Flynn
//     -Added additional command-line argument parsing.
//     -Added method 'validateKeyAttributes' for command-line argument validation.
//  2008/07/20  Martin D. Flynn
//     -Added 'setKeyValueSeparatorChar'/'getKeyValueSeparatorChar' methods
//  2008/07/27  Martin D. Flynn
//     -Added "StringTools.KeyValueMap" implementation.
//  2009/01/01  Martin D. Flynn
//     -Added ability to 'include' other config files.
//  2009/01/28  Martin D. Flynn
//     -Relative 'include' file/http URL specifications are now resolved relative 
//      to the parent file/url.  Replacement variables specified on 'include'
//      statements may now include variable defined in the current/parent file.
//     - Changed 'include[?]' reservered key to '%include[?]'
//     -Added '%log' reserved key to display the specified value to the log output.
//  2009/02/20  Martin D. Flynn
//     -Added 'getAllowBlankValues' and 'setAllowBlankValues'.
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.io.*;
import java.util.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.lang.reflect.*;

public class RTProperties
    implements Cloneable, StringTools.KeyValueMap
{

    // ------------------------------------------------------------------------

    private static final boolean USE_PROPERTIES_LOADER  = true;
    
    private static final String  INCLUDE_PROTOCOL_FILE  = "file";
    private static final String  INCLUDE_PROTOCOL_HTTP  = "http";
    private static final String  INCLUDE_PROTOCOL_HTTPS = "https";

    // ------------------------------------------------------------------------

    public static final char   NameSeparatorChar        = ':';
    public static final char   KeyValSeparatorChar      = StringTools.KeyValSeparatorChar;
    public static final char   PropertySeparatorChar    = StringTools.PropertySeparatorChar;

    public static final char   ARRAY_DELIM              = StringTools.ARRAY_DELIM;

    // ------------------------------------------------------------------------

    public static final String KEY_START_DELIMITER      = "${";
    public static final String KEY_END_DELIMITER        = "}";
    public static final String KEY_DFT_DELIMITER        = "=";
    public static final int    KEY_MAX_RECURSION        = 6;

    public static final int    KEY_REPLACEMENT_NONE     = 0;
    public static final int    KEY_REPLACEMENT_LOCAL    = 1;
    public static final int    KEY_REPLACEMENT_GLOBAL   = 2;

    // ------------------------------------------------------------------------
    // This constant controls whether boolean properties with unspecified values
    // will return true, or false.  Example:
    //   ""              - getBoolean("bool", dft) returns dft.
    //   "bool=true"     - getBoolean("bool", dft) returns 'true'.
    //   "bool=false"    - getBoolean("bool", dft) returns 'false'.
    //   "bool=badvalue" - getBoolean("bool", dft) returns dft.
    //   "bool"          - getBoolean("bool", dft) returns DEFAULT_TRUE_IF_BOOLEAN_STRING_EMPTY.

    private static final boolean DEFAULT_TRUE_IF_BOOLEAN_STRING_EMPTY   = true;

    // ------------------------------------------------------------------------
    
    public  static final String KEYVAL_PREFIX           = "-";
    public  static final char   KEYVAL_PREFIX_CHAR      = '-';
    public  static final char   KEYVAL_SEPARATOR_CHAR_1 = '=';
    public  static final char   KEYVAL_SEPARATOR_CHAR_2 = ':';

    /**
    *** Returns the index of the key/value separator (either '=' or ':').
    *** @param kv  The String parsed for the key/value separator
    *** @return The index of the key/value separator
    **/
    private int _indexOfKeyValSeparator(String kv)
    {
        //return kv.indexOf('=');
        for (int i = 0; i < kv.length(); i++) {
            char ch = kv.charAt(i);
            if ((ch == KEYVAL_SEPARATOR_CHAR_1) || (ch == KEYVAL_SEPARATOR_CHAR_2)) {
                return i;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------------

    private String              cfgDirRoot              = null;

    private Map<Object,Object>  cfgProperties           = null;
    private boolean             ignoreCase              = false;
    private boolean             allowBlankValues        = true;

    private char                propertySeparator       = PropertySeparatorChar;
    private char                keyValueSeparator       = KeyValSeparatorChar;
    private int                 keyReplacementMode      = KEY_REPLACEMENT_NONE;

    private int                 nextCmdLineArg          = -1;

    private boolean             enableConfigLogMessages = true;

    // ------------------------------------------------------------------------

    /**
    *** Constructor
    *** @param map  The Object key/value map used to initialize this instance
    **/
    public RTProperties(Map<?,?> map)
    {
        this.setBackingProperties(map);
    }

    /**
    *** Constructor
    **/
    public RTProperties()
    {
        this((Map<Object,Object>)null);
    }

    /**
    *** Constructor
    *** @param props  A String containing "key=value key=value ..." specifications used to
    ***               initialize this instance.
    **/
    public RTProperties(String props)
    {
        this();
        this.setProperties(props, true);
    }

    /**
    *** Constructor
    *** @param props  A String containing "key=value key=value ..." specifications used to
    ***               initialize this instance.
    *** @param inclName True to parse and set the name of this instance.
    **/
    public RTProperties(String props, boolean inclName)
    {
        this();
        this.setProperties(props, inclName);
    }

    /**
    *** Constructor
    *** @param props  A String containing "key=value key=value ..." specifications used to
    ***               initialize this instance.
    *** @param propSep The separator character between one "key=value" pair and the next.
    ***                (ie. in "key=value;key=value", ';' is the property separator)
    **/
    public RTProperties(String props, char propSep)
    {
        this();
        this.setPropertySeparatorChar(propSep);
        this.setProperties(props, true);
    }

    /**
    *** Constructor
    *** @param props     A String containing "key=value key=value ..." specifications used to
    ***                  initialize this instance.
    *** @param propSep   The separator character between one "key=value" pair and the next.
    ***                  (ie. in "key=value;key=value", ';' is the property separator)
    *** @param keyValSep The separator character between the property "key" and "value".
    ***                  (ie. in "key=value", ':' is the key/value separator)
    **/
    public RTProperties(String props, char propSep, char keyValSep)
    {
        this();
        this.setPropertySeparatorChar(propSep);
        this.setKeyValueSeparatorChar(keyValSep);
        this.setProperties(props, true);
    }

    /**
    *** Constructor
    *** @param argv    An array of "key=value" specifications.
    **/
    public RTProperties(String argv[])
    {
        this();
        if (argv != null) {
            for (int i = 0; i < argv.length; i++) {
                
                /* ignore blank arguments */
                if (StringTools.isBlank(argv[i])) {
                    continue;
                }

                /* key/val */
                int p = this._indexOfKeyValSeparator(argv[i]); // argv[i].indexOf("=");
                String key = (p >= 0)? argv[i].substring(0,p).trim() : argv[i];
                String val = (p >= 0)? argv[i].substring(p+1).trim() : "";

                /* remove prefixing "-" from key */
                if (key.startsWith(KEYVAL_PREFIX)) {
                    
                    /* remove prefixing "-" from key */
                    while (key.startsWith(KEYVAL_PREFIX)) { 
                        key = key.substring(1); 
                    }
                    
                    /* special case when separator not specified after key */
                    if (p < 0) {
                        // no separator specified

                        /* end of parameter check? */
                        if (key.equals("")) {
                            // stop at first "-","--",... without a key specifiation
                            // (ie. "-arg1=a -arg2=b -- this is not parsed")
                            if (i < (argv.length + 1)) {
                                this.nextCmdLineArg = i + 1;
                            }
                            break;
                        }

                        /* "-key" was specified without a "=" separator */
                        // check following argument for prefixing "-"
                        if (((i + 1) < argv.length) && !argv[i+1].startsWith(KEYVAL_PREFIX)) {
                            // next argument doesn't have a prefixing "-" (ie. "-file /tmp/myFile")
                            // assume this should be the value for the key
                            // (ie. "-arg1 val1 -arg2 val2")
                            i++; // advance argument pointer
                            val = argv[i];
                        }
                        
                    }

                }

                /* store key/value */
                if (key.equals("")) {
                    // skip "=value", "-=value", etc.
                    Print.logWarn("Ignoring invalid key argument: '%s'", argv[i]);
                } else {
                    this.setString(key, val);
                }

            }
        }
    }

    /**
    *** Constructor
    *** @param cfgFile A file specification from which the key=value properties are loaded.
    **/
    public RTProperties(File cfgFile)
    {
        this(CreateDefaultMap());
        if ((cfgFile == null) || cfgFile.equals("")) {
            // ignore this case
        } else
        if (cfgFile.isFile()) {
            if (!RTConfig.getBoolean(RTKey.RT_QUIET,true)) {
                Print.logInfo("Loading config file: " + cfgFile);
            }
            try {
                this.setProperties(cfgFile, true);
            } catch (IOException ioe) {
                Print.logError("Unable to load config file: " + cfgFile + " [" + ioe + "]");
            }
        } else {
            Print.logError("Config file doesn't exist: " + cfgFile);
        }
    }

    /**
    *** Constructor
    *** @param cfgURL A URL specification from which the key=value properties are loaded.
    **/
    public RTProperties(URL cfgURL)
    {
        this(CreateDefaultMap());
        if (cfgURL == null) {
            // ignore this case
        } else {
            if (!RTConfig.getBoolean(RTKey.RT_QUIET,true)) {
                Print.logInfo("Loading config file: " + cfgURL);
            }
            try {
                this.setProperties(cfgURL, true);
            } catch (IOException ioe) {
                Print.logError("Unable to load config file: " + cfgURL + " [" + ioe + "]");
            }
        }
    }

    /**
    *** Copy Constructor
    *** @param rtp A RTProperties instance from this this instance is initialized
    **/
    public RTProperties(RTProperties rtp)
    {
        this();
        this.setProperties(rtp, true);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a clone of this RTProperties instance
    *** @return A clone of this RTProperties instance
    **/
    public Object clone()
    {
        return new RTProperties(this);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the key case on lookups is to be ignored
    *** @return True if the key case on lookups is to be ignored
    **/
    public boolean getIgnoreKeyCase()
    {
        return this.ignoreCase;
    }

    /**
    *** Sets whether key-case is to be ignored on propery lookups.  Only valid if the backing Map
    *** is an <code>OrderedMap</code>.
    *** @param ignCase True ignore key-case on lookups, false otherwise
    **/
    public void setIgnoreKeyCase(boolean ignCase)
    {
        this.ignoreCase = ignCase;
        Map props = this.getProperties();
        if (props instanceof OrderedMap) {
            ((OrderedMap)props).setIgnoreCase(this.ignoreCase);
        } else
        if (ignCase) {
            Print.logWarn("Backing map is not an 'OrderedMap', case insensitive keys not in effect");
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if empty String values are allowed
    *** @return True if empty String values are allowed
    **/
    public boolean getAllowBlankValues()
    {
        return this.allowBlankValues;
    }

    /**
    *** Sets whether empty String values are allowed
    *** @param allowBlank True to allow blank String values
    **/
    public void setAllowBlankValues(boolean allowBlank)
    {
        this.allowBlankValues = allowBlank;
        if (!allowBlank) {
            // TODO: remove existing blank values?
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if configuration log messages (ie. "%log=:) are enabled
    *** @return True if configuration log messages (ie. "%log=:) are enabled
    **/
    public boolean getConfigLogMessagesEnabled()
    {
        return this.enableConfigLogMessages;
    }

    /**
    *** Sets Configuration log messages (ie. "%log=") enabled/disabled
    *** @param enable True to enable, false to disable
    **/
    public void setConfigLogMessagesEnabled(boolean enable)
    {
        this.enableConfigLogMessages = enable;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the name of this instance.
    *** @return The name of this instance
    **/
    public String getName()
    {
        return this.getString(RTKey.NAME, "");
    }

    /**
    *** Sets the name of this instance
    *** @param name  The name of this instance to set
    **/
    public void setName(String name)
    {
        this.setString(RTKey.NAME, name);
    }

    // ------------------------------------------------------------------------

    /**
    *** List all defined property keys which do not have a registered default value.<br>
    *** Used for diagnostice purposes.
    **/
    public void checkDefaults()
    {
        // This produces a list of keys in the properties list for which RTKey has not 
        // default value.  This is typically for listing unregistered, and possibly 
        // obsolete, properties found in a config file.
        for (Iterator<?> i = this.keyIterator(); i.hasNext();) {
            String key = i.next().toString();
            if (!RTKey.hasDefault(key)) {
                Print.logDebug("No default for key: " + key);
            }
        }
    }

    // ------------------------------------------------------------------------

    protected static Class<OrderedMap> DefaultMapClass = OrderedMap.class;

    /**
    *** Creates a default Map object container
    *** @return A default Map object container
    **/
    protected static Map<Object,Object> CreateDefaultMap()
    {
        /*
        try {
            Map<Object,Object> map = (Map<Object,Object>)DefaultMapClass.newInstance();  // "unchecked cast"
            return map;
        } catch (Throwable t) {
            // (Do not use 'Print' here!!!)
            System.out.println("[RTProperties] Error instantiating: " + DefaultMapClass); // 
            return new OrderedMap<Object,Object>();
        }
        */
        return new OrderedMap<Object,Object>();
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the next command-line argument following the last argument
    *** processed by the command-line argument RTProperties constructor.
    *** @return The next command-line argument, or '-1' if there are no additional
    ***         command-line arguments.
    **/
    public int getNextCommandLineArgumentIndex()
    {
        return this.nextCmdLineArg;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Validates the key/values against the expected set of keys and value types.
    *** @param keyAttr  A list of expected keys and attributes
    *** @return The index of the first invalid key
    **/
    public boolean validateKeyAttributes(PrintStream out, String keyAttr[])
    {
        // key[=|:][o,m][s|i|f|b]
        
        /* nothing to check? */
        if (ListTools.isEmpty(keyAttr)) {
            return true; // all is ok
        }
        
        /* loop through key attributes */
        int error = 0;
        Set<?> argKeys = new HashSet<Object>(this.getPropertyKeys());
        for (int i = 0; i < keyAttr.length; i++) {
            String aKey[] = null;
            boolean mandatory = false;
            int valType = 0; // 0=s,1=i,2=f|d,3=b
            
            int p = this._indexOfKeyValSeparator(keyAttr[i]);
            if (p == 0) {
                // ignore this invalid keyAttr entry
            } else
            if (p < 0) {
                // optional key
                aKey = StringTools.split(keyAttr[i],',');
                mandatory = false;
                valType = 0;
            } else {
                aKey = StringTools.split(keyAttr[i].substring(0,p),',');
                mandatory = (keyAttr[i].charAt(p) == '=')? true : false;
                String attr[] = StringTools.split(keyAttr[i].substring(p+1),',');
                for (int a = 0; a < attr.length; a++) {
                    if (attr[a].equals("m")) { mandatory = true;  } else
                    if (attr[a].equals("o")) { mandatory = false; } else
                    if (attr[a].equals("s")) { valType   = 0;     } else
                    if (attr[a].equals("i")) { valType   = 1;     } else
                    if (attr[a].equals("f")) { valType   = 2;     } else
                    if (attr[a].equals("d")) { valType   = 2;     } else
                    if (attr[a].equals("b")) { valType   = 3;     }
                }
            }

            /* remove keys */
            boolean keyFound = false;
            String keyStr = StringTools.join(aKey,',');
            if (ListTools.isEmpty(aKey)) {
                // invalid keyAttr entry
                continue;
            } else {
                int found = 0;
                for (int k = 0; k < aKey.length; k++) {
                    if (this.hasProperty(aKey[k])) { found++; }
                    argKeys.remove(aKey[k]);
                }
                if (found > 1) {
                    if (out != null) { out.println("ERROR: Multiple values found for keys: " + keyStr); }
                    error++;
                }
                keyFound = (found > 0);
            }

            /* get value */
            String keyValue = this.getString(aKey, null);

            /* blank value? */
            if (StringTools.isBlank(keyValue)) {
                if (mandatory && (!keyFound || (valType != 3))) {
                    // mandatory argument/value not specified
                    if (out != null) { out.println("ERROR: Mandatory key not specified: " + keyStr); }
                    error++;
                }
                continue;
            }
            
            /* check value against type */
            String firstKey = this.getFirstDefinedKey(aKey);
            switch (valType) {
                case 0: // String
                    break;
                case 1: // Integer/Long
                    if (!StringTools.isLong(keyValue,true)) {
                        if (out != null) { out.println("ERROR: Invalid value for key (i): " + firstKey); }
                        error++;
                    }
                    break;
                case 2: // Float/Double
                    if (!StringTools.isDouble(keyValue,true)) {
                        if (out != null) { out.println("ERROR: Invalid value for key (f): " + firstKey); }
                        error++;
                    }
                    break;
                case 3: // Boolean
                    if (!StringTools.isBoolean(keyValue,true)) {
                        if (out != null) { out.println("ERROR: Invalid value for key (b): " + firstKey); }
                        error++;
                    }
                    break;
            }
            
        }
        
        /* check for remaining unrecognized keys */
        if (!argKeys.isEmpty()) {
            boolean UNRECOGNIZED_ARGUMENT_ERROR = false;
            for (Object key : argKeys) {
                String ks = key.toString();
                if (ks.startsWith("$")) { continue; }
                if (UNRECOGNIZED_ARGUMENT_ERROR) {
                    if (out != null) { out.println("ERROR: Unrecognized argument specified: " + ks); }
                    error++;
                } else {
                    if (out != null) { out.println("WARNING: Unrecognized argument specified: " + ks); }
                }
            }
        }
        
        /* return validation result */
        return (error == 0);
        
    }
    
    // ------------------------------------------------------------------------

    /**
    *** PropertyChangeListener interface
    **/
    public interface PropertyChangeListener
    {
        void propertyChange(RTProperties.PropertyChangeEvent pce);
    }

    /**
    *** PropertyChangeEvent class 
    **/
    public class PropertyChangeEvent
    {
        private Object keyObj = null;
        private Object oldVal = null;
        private Object newVal = null;
        public PropertyChangeEvent(Object key, Object oldValue, Object newValue) {
            this.keyObj = key;      // may be null
            this.oldVal = oldValue; // may be null
            this.newVal = newValue; // may be null
        }
        public RTProperties getSource() {
            return RTProperties.this;
        }
        public Object getKey() {
            return this.keyObj; // may be null
        }
        public Object getOldValue() {
            return this.oldVal; // may be null
        }
        public Object getNewValue() {
            return this.newVal; // may be null
        }
    }

    private java.util.List<PropertyChangeListener> changeListeners = null;

    /** 
    *** Adds a PropertyChangeListener to this instance
    *** @param pcl  A PropertyChangeListener to add to this instance
    **/
    public void addChangeListener(PropertyChangeListener pcl)
    {
        if (this.changeListeners == null) { 
            this.changeListeners = new Vector<PropertyChangeListener>();
        }
        this.changeListeners.add(pcl);
    }

    /** 
    *** Removes a PropertyChangeListener from this instance
    *** @param pcl  A PropertyChangeListener to remove from this instance
    **/
    public void removeChangeListener(PropertyChangeListener pcl)
    {
        if (this.changeListeners != null) {
            this.changeListeners.remove(pcl);
        }
    }

    /**
    *** Fires a PropertyChange event
    *** @param key  The property key which changed
    *** @param oldVal  The old value of the property key which changed
    **/
    protected void firePropertyChanged(Object key, Object oldVal)
    {
        if (this.changeListeners != null) {
            Object newVal = this.getProperties().get(key);
            RTProperties.PropertyChangeEvent pce = new RTProperties.PropertyChangeEvent(key,oldVal,newVal);
            for (Iterator i = this.changeListeners.iterator(); i.hasNext();) {
                ((RTProperties.PropertyChangeListener)i.next()).propertyChange(pce);
            }
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the backing properties Map for this instance
    *** @return  The backing properties Map for this instance
    **/
    @SuppressWarnings("unchecked")
    public void setBackingProperties(Map<?,?> map)
    {
        this.cfgProperties = (Map<Object,Object>)map;
        /*
        if (this.cfgProperties != null) {
            for (Object k : this.cfgProperties.keySet()) {
                Object v = this.cfgProperties.get(k);
                Print.sysPrintln(k + " ==> " + v);
            }
        }
        */
    }

    /**
    *** Gets the backing properties Map for this instance
    *** @return  The backing properties Map for this instance
    **/
    public Map<Object,Object> getProperties()
    {
        if (this.cfgProperties == null) { 
            this.cfgProperties = CreateDefaultMap();
            if (this.cfgProperties instanceof OrderedMap) {
                ((OrderedMap)this.cfgProperties).setIgnoreCase(this.ignoreCase);
            }
        }
        return this.cfgProperties;
    }

    /**
    *** Returns true if this RTProperties instance is empty (ie. contains no properties)
    *** @return  True if empty
    **/
    public boolean isEmpty()
    {
        if (this.cfgProperties == null) {
            return true;
        } else {
            return (this.cfgProperties.size() <= 0);
        }
    }

    /**
    *** Returns an Iterator over the property keys defined in this RTProperties instance
    *** @return An Iterator over the property keys defined in this RTProperties instance
    **/ 
    public Iterator<?> keyIterator()
    {
        return this.getPropertyKeys().iterator();
    }

    /**
    *** Gets a set of property keys defined by this RTProperties instance
    *** @return A set of property keys defined by this RTProperties instance
    **/
    public Set<?> getPropertyKeys()
    {
        return this.getProperties().keySet();
    }

    /**
    *** Returns a set of property keys defined in this RTProperties instance which start with the specified String
    *** @return A set of property keys defined in this RTProperties instance which start with the specified String
    **/ 
    public Set<?> getPropertyKeys(String startsWith)
    {
        OrderedSet<String> keys = new OrderedSet<String>();
        for (Iterator<?> i = this.keyIterator(); i.hasNext();) {
            String k = i.next().toString();
            if (StringTools.startsWithIgnoreCase(k, startsWith)) {
                keys.add(k);
            }
        }
        return keys;
    }

    /**
    *** Returns a subset of this RTProperties instance containing key/value pairs which match the
    *** specified partial key.
    *** @param keyStartsWith  The partial key used to match keys in this instance
    *** @return The RTProperties subset
    **/
    public RTProperties getSubset(String keyStartsWith)
    {
        RTProperties rtp = new RTProperties();
        for (Iterator<?> i = this.keyIterator(); i.hasNext();) {
            Object k = i.next();
            if (k instanceof String) {
                String ks = (String)k;
                if (StringTools.startsWithIgnoreCase(ks,keyStartsWith)) {
                    String v = this.getString(ks, null);
                    rtp.setProperty(ks, v);
                }
            }
        }
        return rtp;
    }

    /* Extract a Map containing a group of key/values from the runtime config */
    /*
    public Map<String,String> extractMap(String keyEnd, String valEnd)
    {
        Map<String,String> m = new OrderedMap<String,String>();
        for (Iterator<?> i = this.keyIterator(); i.hasNext();) {
            String mkKey = i.next().toString();
            if (mkKey.endsWith(keyEnd)) {
                String key = getString(mkKey, null);
                if (key != null) { // <-- will never be null anyway
                    String mvKey = mkKey.substring(0, mkKey.length() - keyEnd.length()) + valEnd;
                    String val = this.getString(mvKey, "");
                    m.put(key, val);
                }
            }
        }
        return m;
    }
    */

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified property key is defined
    *** @param key  A property key
    *** @return True if the specified property key is defined
    **/
    public static boolean containsKey(Map<Object,Object> map, Object key, boolean blankOK)
    {

        /* quick false checks */
        if ((map == null) || (key == null)) {
            return false;
        }

        /* check for contains */
        if (blankOK) {
            // blank values are ok
            return map.containsKey(key);
        } else {
            // blank String values are considered 'null'
            Object val = map.get(key);
            if (val instanceof String) {
                return !StringTools.isBlank((String)val);
            } else {
                return (val != null);
            }
        }

    }

    /**
    *** Returns true if the specified property key is defined
    *** @param key  A property key
    *** @return True if the specified property key is defined
    **/
    public boolean hasProperty(Object key)
    {
        if (key != null) {
            Map<Object,Object> props = this.getProperties();
            boolean allowBlanks = this.getAllowBlankValues();
            return RTProperties.containsKey(props, key, allowBlanks);
        } else {
            return false;
        }
    }

    /**
    *** Returns the first defined property key in the list 
    *** @param key  An array of property keys
    *** @return the first defined property key in the list
    **/
    public String getFirstDefinedKey(String key[])
    {
        if (key != null) {
            for (int i = 0; i < key.length; i++) {
                if (this.hasProperty(key[i])) {
                    return key[i];
                }
            }
        }
        return null;
    }

    /**
    *** Returns the specified key, if defined
    *** @param key  The propery key
    *** @return The property key if defined, or null otherwise
    **/
    public String getFirstDefinedKey(String key)
    {
        return this.hasProperty(key)? key : null;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the value for the specified key
    *** @param key  The property key
    *** @param value The value to associate with the specified key
    **/
    public void setProperty(Object key, Object value)
    {
        if (key != null) {

            /* properties */
            Map<Object,Object> props = this.getProperties();

            /* disallow blank values? */
            if (!this.getAllowBlankValues() && (value instanceof String) && StringTools.isBlank((String)value)) {
                value = null; // will be removed below
            }

            /* "!<key>" implies removable of <key> from Map (value is ignored) */
            String k = (key instanceof String)? (String)key : null;
            if (!StringTools.isBlank(k) && ("|!^".indexOf(k.charAt(0)) >= 0)) {
                key   = k.substring(1);
                value = null;
            }

            /* encode arrays? */
            if ((value != null) && value.getClass().isArray()) {
                Class arrayClass = value.getClass();
                if (arrayClass.getComponentType().isPrimitive()) {
                    value = StringTools.encodeArray(value, ARRAY_DELIM, false);
                } else {
                    Object a[] = (Object[])value;
                    boolean quote = (a instanceof Number[])? false : true;
                    value = StringTools.encodeArray(a, ARRAY_DELIM, quote);
                }
            } else {
                //
            }

            /* add/remove key/value */
            if (!(props instanceof Properties) || (key instanceof String)) {
                Object oldVal = props.get(key);
                if (value == null) {
                    //Print._println("Removing key: " + key);
                    props.remove(key);
                } else
                if ((props instanceof OrderedMap) && key.equals(RTKey.NAME)) {
                    //Print._println("Setting name: " + value);
                    ((OrderedMap<Object,Object>)props).put(0, key, value);
                } else {
                    //Print._println("Setting key: " + key + "=" + value);
                    props.put(key, value);
                }
                this.firePropertyChanged(key, oldVal);
            } else {
                // Non-String are not supported in the 'Properties' class
            }

        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Adds the properties in the specified RTProperties instance to this instance
    *** @param rtp  The RTProperties instance from which properties will be copied to this instance
    *** @return The name of this RTProperties instance
    **/ 
    public String setProperties(RTProperties rtp)
    {
        return this.setProperties(rtp, false);
    }

    /**
    *** Adds the properties in the specified RTProperties instance to this instance
    *** @param rtp  The RTProperties instance from which properties will be copied to this instance
    *** @param inclName  True to set the name of this instace to the instance of the specified RTProperties instance.
    *** @return The name of this RTProperties instance
    **/ 
    public String setProperties(RTProperties rtp, boolean inclName)
    {
        if (rtp != null) {
            return this.setProperties(rtp.getProperties(), inclName);
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------

    public String setProperties(URL url)
        throws IOException
    {
        return this.setProperties(url, false);
    }
    
    public String setProperties(URL url, boolean inclName)
        throws IOException
    {
        String name = null;
        if (url != null) {
            InputStream uis = url.openStream(); // may throw IOException
            try {
                name = this._setProperties(uis, inclName, url);
            } finally {
                try { uis.close(); } catch (IOException ioe) {/*ignore*/}
            }
        }
        return name;
    }

    // ------------------------------------------------------------------------

    public String setProperties(File file)
        throws IOException
    {
        return this.setProperties(file, false);
    }
    
    public String setProperties(File file, boolean inclName)
        throws IOException
    {
        String name = null;
        if (file != null) {
            File absFile = file.getAbsoluteFile();
            FileInputStream fis = new FileInputStream(absFile); // may throw IOException
            try {
                name = this._setProperties(fis, inclName, absFile.toURL());
            } finally {
                try { fis.close(); } catch (IOException ioe) {/*ignore*/}
            }
        }
        return name;
    }

    // ------------------------------------------------------------------------

    public String setProperties(InputStream in)
        throws IOException
    {
        return this._setProperties(in, false, null);
    }

    public String setProperties(InputStream in, boolean inclName)
        throws IOException
    {
        return this._setProperties(in, false, null);
    }

    private String _setProperties(InputStream in, boolean inclName, URL inputURL)
        throws IOException
    {

        /* create temporary Properties holder */
        OrderedProperties props = new OrderedProperties(inputURL);

        /* set property for this loaded URL */
        if (inputURL != null) {
            props.put(RTKey.CONFIG_URL, inputURL.toString());
        }

        /* load the properties from the specified input-stream */
        if (USE_PROPERTIES_LOADER) {
            // Warning! '<Properties>.load' uses character encoding "ISO-8859-1"
            props.load(in); // "props.put(key,value)" is used for insertion
        } else {
            Print.logWarn("Non-standard Properties file loading ...");
            RTProperties.loadProperties(props, in);
        }

        /* convert these loaded properties to an internal format */
        return this.setProperties(props.getOrderedMap(), inclName);

    }

    // ------------------------------------------------------------------------

    public String setProperties(Map props)
    {
        return this.setProperties(props, false);
    }

    public String setProperties(Map props, boolean inclName)
    {
        // Note: Does NOT remove old properties (by design)
        if (props != null) {
            String n = null;
            for (Iterator i = props.keySet().iterator(); i.hasNext();) {
                Object key = i.next();
                Object val = props.get(key);
                if (RTKey.NAME.equals(key)) {
                    n = (val != null)? val.toString() : null;
                    if (inclName) {
                        this.setName(n);
                    }
                } else {
                    this.setProperty(key, val);
                }
            }
            return n;
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------
    
    public void setPropertySeparatorChar(char propSep)
    {
        this.propertySeparator = propSep;
    }
    
    public char getPropertySeparatorChar()
    {
        return this.propertySeparator;
    }

    // ------------------------------------------------------------------------
    
    public void setKeyValueSeparatorChar(char keyValSep)
    {
        this.keyValueSeparator = keyValSep;
    }
    
    public char getKeyValueSeparatorChar()
    {
        return this.keyValueSeparator;
    }

    // ------------------------------------------------------------------------

    public String setProperties(String props)
    {
        return this.setProperties(props, false);
    }

    public String setProperties(String props, char propSep)
    {
        this.setPropertySeparatorChar(propSep);
        return this.setProperties(props, false);
    }

    public String setProperties(String props, boolean inclName)
    {
        if (props != null) {
            char propSep   = this.getPropertySeparatorChar();
            char keyValSep = this.getKeyValueSeparatorChar();

            /* check for prefixing name in string (ie. "[name] key=value") */
            String n = null, p = props.trim();
            if (p.startsWith("[")) {
                int x = p.indexOf("]");
                if (x > 0) {
                    // found "[name]"
                    n = p.substring(1,x).trim();
                    p = p.substring(x+1).trim();
                } else {
                    // missing name terminating ']'
                    p = p.substring(1).trim(); // just skip first '['
                }
            }

            /* parse and set properties */
            Map propMap = StringTools.parseProperties(p, propSep, keyValSep);
            if (n == null) {
                n = this.setProperties(propMap, inclName);
            } else {
                this.setProperties(propMap, false);
                if (inclName) {
                    this.setName(n);
                }
            }

            /* return name, if any */
            return n;

        } else {

            return null;

        }
    }

    // ------------------------------------------------------------------------

    public void removeProperty(Object key)
    {
        if (key != null) {
            Map props = this.getProperties();
            if (!(props instanceof Properties) || (key instanceof String)) {
                Object oldVal = props.get(key);
                props.remove(key);
                this.firePropertyChanged(key, oldVal);
            }
        }
    }

    public void clearProperties()
    {
        this.getProperties().clear();
        this.firePropertyChanged(null, null);
    }

    public void resetProperties(Map props)
    {
        this.clearProperties();
        this.setProperties(props, true);
    }

    // ------------------------------------------------------------------------

    public String insertKeyValues(String text)   
    {
        return this._insertKeyValues(null, text, KEY_START_DELIMITER, KEY_END_DELIMITER);
    }

    public String insertKeyValues(String text, String startDelim, String endDelim)
    {
        return this._insertKeyValues(null, text, startDelim, endDelim);
    }

    public String _insertKeyValues(Object key, String text)   
    {
        return this._insertKeyValues(key, text, KEY_START_DELIMITER, KEY_END_DELIMITER);
    }

    public String _insertKeyValues(final Object mainKey, String text, String startDelim, String endDelim)
    {
        if (text != null) {
            //Print.logInfo("Inserting local keyvalues: " + text);
            // replacment call-back 
            StringTools.ReplacementMap rm = new StringTools.ReplacementMap() {
                private Set<Object> thisKeySet = new HashSet<Object>();
                private Set<Object> fullKeySet = new HashSet<Object>();
                public String get(String k) {
                    if (k == null) {
                        // a bit of a hack here to tell this map to reset the cached keys
                        fullKeySet.addAll(thisKeySet);
                        if (mainKey != null) { fullKeySet.add(mainKey); }
                        thisKeySet.clear();
                        return null;
                    } else
                    if (fullKeySet.contains(k)) {
                        //Print.logInfo("Key already processed: " + k);
                        return null;
                    } else {
                        //Print.logInfo("Processing key: " + k);
                        thisKeySet.add(k);
                        Object obj = RTProperties.this._getProperty(k, null);
                        return (obj != null)? obj.toString() : null;
                    }
                }
            };
            // iterate until the string doesn't change
            String s_old = text;
            for (int i = 0; i < RTProperties.KEY_MAX_RECURSION; i++) {
                rm.get(null); // hack to reset the cached keys
                String s_new = StringTools.insertKeyValues(s_old, startDelim, endDelim, rm, false);
                //Print.logInfo("New String: " + s_new);
                if (s_new.equals(s_old)) {
                    return s_new;
                }
                s_old = s_new;
            }
            return s_old;
        } else {
            return text; // return null
        }
    }

    // ------------------------------------------------------------------------

    public void setKeyReplacementMode(int mode)
    {
        this.keyReplacementMode = mode;
    }

    private Object _replaceKeyValues(Object key, Object obj)   
    {
        if (this.keyReplacementMode == KEY_REPLACEMENT_NONE) {
            //Print.logInfo("No replacement to be performed: " + obj);
            return obj;
        } else
        if ((obj == null) || !(obj instanceof String)) {
            //Print.logInfo("Returning non-String object as-is: " + obj);
            return obj;
        } else
        if (this.keyReplacementMode == KEY_REPLACEMENT_LOCAL) {
            //Print.logInfo("Replacing local keys: " + obj);
            return this._insertKeyValues(key,(String)obj);
        } else {
            //Print.logInfo("Replacing global keys: " + obj);
            return RTConfig._insertKeyValues(key,(String)obj);
        }
    }

    private Object _getProperty(Object key, Object dft, Class dftClass, boolean replaceKeys)
    {
        Object value = this.getProperties().get(key);
        if (value == null) {
            return replaceKeys? this._replaceKeyValues(key,dft) : dft; // no value, return default
        } else
        if ((dft == null) && (dftClass == null)) {
            return replaceKeys? this._replaceKeyValues(key,value) : value; // return as-is
        } else {
            // convert 'value' to same type (class) as 'dft' (if specified)
            Class c = (dftClass != null)? dftClass : dft.getClass();
            try {
                return convertToType(replaceKeys? this._replaceKeyValues(key,value) : value, c);
            } catch (Throwable t) {
                return replaceKeys? this._replaceKeyValues(key,dft) : dft; // inconvertable, return as-is
            }
        }
    }

    public Object _getProperty(Object key, Object dft)
    {
        return this._getProperty(key, dft, null, false);
    }

    public Object getProperty(Object key, Object dft)
    {
        return this._getProperty(key, dft, null, true);
    }

    protected static Object convertToType(Object val, Class<?> type)
        throws Throwable
    {
        if ((type == null) || (val == null)) {
            // not converted
            return val;
        } else
        if (type.isAssignableFrom(val.getClass())) {
            // already converted
            return val;
        } else
        if (type == String.class) {
            // convert to String
            return val.toString();
        } else {
            // ie:
            //   new File(String.class)
            //   new Long(String.class)
            try {
                Constructor meth = type.getConstructor(new Class[] { type });
                return meth.newInstance(new Object[] { val });
            } catch (Throwable t1) {
                try {
                    Constructor meth = type.getConstructor(new Class[] { String.class });
                    return meth.newInstance(new Object[] { val.toString() });
                } catch (Throwable t2) {
                    Print.logError("Can't convert value to " + type.getName() + ": " + val);
                    throw t2; // inconvertable
                }
            }
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the String value for the specified key
    *** @param key  The property key
    *** @return The String value, or null if the key is not found
    **/
    public String getString(String key)
    {
        return this.getString(key, null);
    }

    /**
    *** Gets the String value for the specified key
    *** @param key  An array or property keys.  The value of the first matching 
    ***             key will be returned.
    *** @param dft  The default value return if the key is not found
    *** @return The String value, or 'dft' if the key is not found
    **/
    public String getString(String key[], String dft)
    {
        return this.getString(this.getFirstDefinedKey(key), dft);
    }

    /**
    *** Gets the String value for the specified key
    *** @param key  The property key.
    *** @param dft  The default value return if the key is not found
    *** @return The String value, or 'dft' if the key is not found
    **/
    public String getString(String key, String dft)
    {
        return this.getString(key, dft, true);
    }

    /**
    *** Gets the String value for the specified key
    *** @param key  The property key.
    *** @param dft  The default value return if the key is not found
    *** @param replaceKeys  True to perform ${...} key replace, false to return raw String
    *** @return The String value, or 'dft' if the key is not found
    **/
    public String getString(String key, String dft, boolean replaceKeys)
    {
        Object val = this._getProperty(key, dft, String.class, replaceKeys);
        if (val == null) {
            return null;
        } else
        if (val.equals(RTKey.NULL_VALUE)) {
            return null;
        } else {
            return val.toString();
        }
    }

    /**
    *** Sets the property value for the specified key
    *** @param key    The property key
    *** @param value  The property value to set.
    **/
    public void setString(String key, String value)
    {
        this.setProperty(key, value);
    }
    
    /**
    *** "StringTools.KeyValueMap" interface
    *** @param key  The property key
    *** @param arg  The property argument (used as the 'default' String value here)
    *** @return The property value
    **/
    public String getKeyValue(String key, String arg)
    {
        return this.getString(key, arg);
    }

    // ------------------------------------------------------------------------

    public String[] getStringArray(String key)
    {
        return this.getStringArray(key, null);
    }

    public String[] getStringArray(String key[], String dft[])
    {
        return this.getStringArray(this.getFirstDefinedKey(key), dft);
    }
    
    public String[] getStringArray(String key, String dft[])
    {
        String val = this.getString(key, null);
        if (val == null) {
            return dft;
        } else {
            String va[] = StringTools.parseArray(val);
            // TODO: check for RTKey.NULL_VALUE in string array
            return va;
        }
    }

    public void setStringArray(String key, String val[])
    {
        this.setStringArray(key, val, true);
    }

    public void setStringArray(String key, String val[], boolean alwaysQuote)
    {
        String valStr = StringTools.encodeArray(val, ARRAY_DELIM, alwaysQuote);
        this.setString(key, valStr);
    }

    public void setProperty(String key, String val[])
    {
        this.setStringArray(key, val, true);
    }

    // ------------------------------------------------------------------------

    public File getFile(String key)
    {
        return this.getFile(key, null);
    }

    // do not include the following method, otherwise "getFile(file, null)" would be ambiguous
    //public File getFile(String key, String dft)

    public File getFile(String key, File dft)
    {
        Object val = this._getProperty(key, null, null, true);
        if (val == null) {
            return dft;
        } else
        if (val instanceof File) {
            return (File)val;
        } else {
            return new File(val.toString());
        }
    }

    public void setFile(String key, File value)
    {
        this.setProperty(key, value);
    }

    // ------------------------------------------------------------------------

    public double getDouble(String key)
    {
        return this.getDouble(key, 0.0);
    }

    public double getDouble(String key[], double dft)
    {
        return this.getDouble(this.getFirstDefinedKey(key), dft);
    }

    public double getDouble(String key, double dft)
    {
        Object val = this._getProperty(key, null, null, true);
        if (val == null) {
            return dft;
        } else
        if (val instanceof Number) {
            return ((Number)val).doubleValue();
        } else {
            return StringTools.parseDouble(val.toString(), dft);
        }
    }

    public double[] getDoubleArray(String key, double dft[])
    {
        String val[] = this.getStringArray(key, null);
        if (val == null) {
            return dft;
        } else {
            double n[] = new double[val.length];
            for (int i = 0; i < val.length; i++) {
                n[i] = StringTools.parseDouble(val[i], 0.0);
            }
            return n;
        }
    }

    public void setDouble(String key, double value)
    {
        this.setProperty(key, value);
    }

    public void setDoubleArray(String key, double value[])
    {
        this.setProperty(key, value);
    }

    public void setProperty(String key, double value)
    {
        this.setProperty(key, new Double(value));
    }

    // ------------------------------------------------------------------------

    public float getFloat(String key)
    {
        return this.getFloat(key, 0.0F);
    }

    public float getFloat(String key[], float dft)
    {
        return this.getFloat(this.getFirstDefinedKey(key), dft);
    }

    public float getFloat(String key, float dft)
    {
        Object val = this._getProperty(key, null, null, true);
        if (val == null) {
            return dft;
        } else
        if (val instanceof Number) {
            return ((Number)val).floatValue();
        } else {
            return StringTools.parseFloat(val.toString(), dft);
        }
    }

    public float[] getFloatArray(String key, float dft[])
    {
        String val[] = this.getStringArray(key, null);
        if (val == null) {
            return dft;
        } else {
            float n[] = new float[val.length];
            for (int i = 0; i < val.length; i++) {
                n[i] = StringTools.parseFloat(val[i], 0.0F);
            }
            return n;
        }
    }

    public void setFloat(String key, float value)
    {
        this.setProperty(key, value);
    }

    public void setFloatArray(String key, float value[])
    {
        this.setProperty(key, value);
    }

    public void setProperty(String key, float value)
    {
        this.setProperty(key, new Float(value));
    }

    // ------------------------------------------------------------------------

    public long getLong(String key)
    {
        return this.getLong(key, 0L);
    }

    public long getLong(String key[], long dft)
    {
        return this.getLong(this.getFirstDefinedKey(key), dft);
    }

    public long getLong(String key, long dft)
    {
        Object val = this._getProperty(key, null, null, true);
        if (val == null) {
            return dft;
        } else
        if (val instanceof Number) {
            return ((Number)val).longValue();
        } else {
            return StringTools.parseLong(val.toString(), dft);
        }
    }

    public long[] getLongArray(String key, long dft[])
    {
        String val[] = this.getStringArray(key, null);
        if (val == null) {
            return dft;
        } else {
            long n[] = new long[val.length];
            for (int i = 0; i < val.length; i++) {
                n[i] = StringTools.parseLong(val[i], 0L);
            }
            return n;
        }
    }

    public void setLong(String key, long value)
    {
        this.setProperty(key, value);
    }

    public void setLongArray(String key, long value[])
    {
        this.setProperty(key, value);
    }

    public void setProperty(String key, long value)
    {
        this.setProperty(key, new Long(value));
    }

    // ------------------------------------------------------------------------

    public int getInt(String key)
    {
        return this.getInt(key, 0);
    }

    public int getInt(String key[], int dft)
    {
        return this.getInt(this.getFirstDefinedKey(key), dft);
    }

    public int getInt(String key, int dft)
    {
        Object val = this._getProperty(key, null, null, true);
        if (val == null) {
            return dft;
        } else
        if (val instanceof Number) {
            return ((Number)val).intValue();
        } else {
            return StringTools.parseInt(val.toString(), dft);
        }
    }

    public int[] getIntArray(String key, int dft[])
    {
        String val[] = this.getStringArray(key, null);
        if (val == null) {
            return dft;
        } else {
            int n[] = new int[val.length];
            for (int i = 0; i < val.length; i++) {
                n[i] = StringTools.parseInt(val[i], 0);
            }
            return n;
        }
    }

    public void setInt(String key, int value)
    {
        this.setProperty(key, value);
    }

    public void setIntArray(String key, int value[])
    {
        this.setProperty(key, value);
    }

    public void setProperty(String key, int value)
    {
        this.setProperty(key, new Integer(value));
    }

    // ------------------------------------------------------------------------

    public boolean getBoolean(String key)
    {
        boolean dft = false;
        return this._getBoolean_dft(key, dft, true);
    }

    public boolean getBoolean(String key[], boolean dft)
    {
        return this.getBoolean(this.getFirstDefinedKey(key), dft);
    }

    public boolean getBoolean(String key, boolean dft)
    {
        return this._getBoolean_dft(key, dft, DEFAULT_TRUE_IF_BOOLEAN_STRING_EMPTY);
    }

    private boolean _getBoolean_dft(String key, boolean dft, boolean dftTrueIfEmpty)
    {
        Object val = this._getProperty(key, null, null, true);
        if (val == null) {
            return dft;
        } else
        if (val instanceof Boolean) {
            return ((Boolean)val).booleanValue();
        } else
        if (val.toString().equals("")) {
            return dftTrueIfEmpty? true : dft;
        } else {
            return StringTools.parseBoolean(val.toString(), dft);
        }
    }

    public void setBoolean(String key, boolean value)
    {
        this.setProperty(key, value);
    }

    public void setBooleanArray(String key, boolean value[])
    {
        this.setProperty(key, value);
    }

    public void setProperty(String key, boolean value)
    {
        this.setProperty(key, new Boolean(value));
    }

    // ------------------------------------------------------------------------

    public void printProperties(String msg)
    {
        this.printProperties(msg, null, null);
    }

    public void printProperties(String msg, RTProperties exclProps)
    {
        this.printProperties(msg, exclProps, null);
    }

    public void printProperties(String msg, Collection<?> orderBy)
    {
        this.printProperties(msg, null, orderBy);
    }

    public void printProperties(String msg, RTProperties exclProps, Collection<?> orderBy)
    {
        if (!StringTools.isBlank(msg)) {
            Print.sysPrintln(msg);
        }
        String prefix = "   ";
        if (this.isEmpty()) {
            Print.sysPrintln(prefix + "<empty>\n");
        } else {
            if (orderBy == null) {
                orderBy = new Vector<Object>(this.getPropertyKeys());
                ListTools.sort((java.util.List<?>)orderBy, null);
            }
            Print.sysPrintln(this.toString(exclProps, orderBy, prefix));
        }
    }

    // ------------------------------------------------------------------------

    public boolean equals(Object other)
    {
        if (other instanceof RTProperties) {
            // We need to perform our own 'equals' checking here:
            // Two RTProperties are equal if they contain the same properties irrespective of ordering.
            // [All property values are compared as Strings]
            RTProperties rtp = (RTProperties)other;
            Map M1 = this.getProperties();
            Map M2 = rtp.getProperties();
            if (M1.size() == M2.size()) {
                for (Iterator i = M1.keySet().iterator(); i.hasNext();) {
                    Object key = i.next();
                    if (M2.containsKey(key)) {
                        Object m1Val = M1.get(key);
                        Object m2Val = M2.get(key);
                        String m1ValStr = (m1Val != null)? m1Val.toString() : null;
                        String m2ValStr = (m2Val != null)? m2Val.toString() : null;
                        if (m1Val == m2Val) {
                            continue; // they are the same object (or both null)
                        } else
                        if ((m1ValStr != null) && m1ValStr.equals(m2ValStr)) {
                            continue; // the values are equals
                        } else {
                            //Print.logInfo("Values not equal: " + m1ValStr + " <==> " + m2ValStr);
                            return false; // values are not equal
                        }
                    } else {
                        //Print.logInfo("Key doesn't exist in M2");
                        return false; // key doesn't exist in M2
                    }
                }
                return true; // all key/vals matched
            } else {
                //Print.logInfo("Sizes don't match");
                return false;
            }
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------
    
    public void saveProperties(File cfgFile)
        throws IOException
    {

        /* property maps */
        Map propMap = this.getProperties();

        /* encode properties */
        StringBuffer strProps = new StringBuffer();
        for (Iterator i = propMap.keySet().iterator(); i.hasNext();) {
            Object keyObj = i.next();
            Object valObj = propMap.get(keyObj);
            strProps.append(keyObj.toString());
            strProps.append(this.getKeyValueSeparatorChar());
            if (valObj != null) {
                strProps.append(valObj.toString());
            }
            strProps.append("\n");
        }
        
        /* save to file */
        FileTools.writeFile(strProps.toString().getBytes(), cfgFile);

    }

    // ------------------------------------------------------------------------

    public String toString()
    {
        return this.toString(null, null, null);
    }

    public String toString(RTProperties exclProps)
    {
        return this.toString(exclProps, null, null);
    }

    public String toString(Collection<?> orderBy)
    {
        return this.toString(null, orderBy, null);
    }

    public String toString(RTProperties exclProps, Collection<?> orderBy)
    {
        return this.toString(null, orderBy, null);
    }

    public String toString(RTProperties exclProps, Collection<?> orderBy, String newLinePrefix)
    {
        StringBuffer sb = new StringBuffer();
        boolean inclNewLine = (newLinePrefix != null);

        /* append name */
        String n = this.getName();
        if (!n.equals("")) {
            sb.append(n).append(NameSeparatorChar).append(" ");
        }

        /* property maps */
        Map<Object,Object> propMap = this.getProperties();
        Map<Object,Object> exclMap = (exclProps != null)? exclProps.getProperties() : null;

        /* order by */
        Set<Object> orderSet = null;
        if (orderBy != null) {
            orderSet = new OrderedSet<Object>(orderBy, true);
            orderSet.addAll(propMap.keySet());
            // 'orderSet' now contains the union of keys from 'orderBy' and 'propMap.keySet()'
        } else {
            orderSet = propMap.keySet();
        }

        /* encode properties */
        for (Iterator<Object> i = orderSet.iterator(); i.hasNext();) {
            Object keyObj = i.next(); // possible this key doesn't exist in 'propMap' if 'orderBy' used.
            if (!RTKey.NAME.equals(keyObj) && RTProperties.containsKey(propMap,keyObj,this.getAllowBlankValues())) {

                Object valObj = propMap.get(keyObj); // key guaranteed here to be in 'propMap'
                if ((exclMap == null) || !RTProperties.compareMapValues(valObj, exclMap.get(keyObj))) {

                    /* prefix? */
                    if (inclNewLine) {
                        sb.append(newLinePrefix);
                    }

                    /* key/value */
                    sb.append(keyObj.toString()).append(this.getKeyValueSeparatorChar());
                    String v = (valObj != null)? valObj.toString() : "";
                    if ((v.indexOf(" ") >= 0) || (v.indexOf("\t") >= 0) || (v.indexOf("\"") >= 0)) {
                        sb.append(StringTools.quoteString(v));
                    } else {
                        sb.append(v);
                    }

                    /* property separator */
                    if (inclNewLine) {
                        sb.append("\n");
                    } else
                    if (i.hasNext()) {
                        sb.append(this.getPropertySeparatorChar());
                    }

                } else {
                    //Print.logDebug("Key hasn't changed: " + key);
                }
            }
        }
        return inclNewLine? sb.toString() : sb.toString().trim();

    }

    private static boolean compareMapValues(Object value, Object target)
    {
        if ((value == null) && (target == null)) {
            return true;
        } else
        if ((value == null) || (target == null)) {
            return false;
        } else
        if (value.equals(target)) {
            return true;
        } else {
            return value.toString().equals(target.toString());
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* config file 'include' */
    private static final String KEY_INCLUDE_URL         = RTKey.INCLUDE;      // file _MUST_ exist
    private static final String KEY_INCLUDE_URL_OPT     = RTKey.INCLUDE_OPT;  // file _may_ exist
    private static final int    MAX_INCLUDE_RECURSION   = 3; // reasonable max recursion (including 'main ' file)

    /**
    *** OrderedProperties class
    **/
    public class OrderedProperties
        extends Properties
    {
        private boolean debugMode = false;
        private int recursionLevel = 0;
        private OrderedMap<String,String> orderedMap = null;
        private URL inputURL = null;
        public OrderedProperties(URL inputURL) {
            this(1, inputURL); // arbitrarily call the starting level, the 'first' recursion level
        }
        private OrderedProperties(int recursion, URL inputURL) {
            super();
            this.recursionLevel = recursion;
            this.orderedMap     = new OrderedMap<String,String>();
            this.inputURL       = inputURL;
        }
        public Object put(Object key, Object value) {
            if ((key == null) || (value == null)) {
                return value;
            }
            String ks = key.toString();
            String vs = value.toString();
            if (ks.startsWith(RTKey.CONSTANT_PREFIX)) {
                if (this.debugMode) {
                    Print.logInfo("(DEBUG) Found Constant key: " + ks);
                }
                if (ks.equalsIgnoreCase("%debugMode")) {
                    this.debugMode = StringTools.parseBoolean(vs,false);
                    if (this.debugMode) {
                        Print.logInfo("(DEBUG) 'debugMode' set to " + this.debugMode);
                    }
                    return value;
                } else
                if (ks.equalsIgnoreCase(KEY_INCLUDE_URL) || ks.equalsIgnoreCase(KEY_INCLUDE_URL_OPT)) {
                    String v = RTConfig.insertKeyValues(vs, this.orderedMap); // replace any reference variables
                    if (StringTools.isBlank(v)) {
                        Print.logError("Invalid/blank 'include' URL: " + vs);
                    } else
                    if (this.recursionLevel >= MAX_INCLUDE_RECURSION) { 
                        Print.logWarn("Excessive 'include' recursion [%s] ...", v);
                    } else {
                        InputStream uis = null;
                        URL url = null;
                        try {
                            if (this.debugMode) {
                                Print.logInfo("(DEBUG) Including: " + v);
                            }
                            url = new URL(v);
                            String parent   = (this.inputURL != null)? this.inputURL.toString() : "";
                            String parProto = (this.inputURL != null)? this.inputURL.getProtocol().toLowerCase() : "";
                            String urlProto = url.getProtocol().toLowerCase();
                            String urlPath  = url.getPath();
                            //Print.logInfo("Protocol '%s' Path '%s'", urlProto, urlPath);
                            if (StringTools.isBlank(parProto)) {
                                // leave as-is
                            } else
                            if (parProto.equals(INCLUDE_PROTOCOL_FILE)) {
                                // parent URL is "file:/...."
                                if (urlProto.equals(INCLUDE_PROTOCOL_FILE) && !urlPath.startsWith("/")) {
                                    // included URL is "file:..." with relative path.  construct absolute URL
                                    int ls = parent.lastIndexOf("/");
                                    if (ls > 0) {
                                        url = new URL(parent.substring(0,ls+1) + urlPath);
                                    }
                                }
                            } else
                            if (parProto.startsWith(INCLUDE_PROTOCOL_HTTP)) { // http, https
                                // parent URL is "http[s]://...."
                                if (urlProto.equals(INCLUDE_PROTOCOL_FILE)) {
                                    // cannot specify included "file:/..." from "http[s]://..."
                                    Print.logError("Invalid 'include' URL protocol: " + url);
                                    url = null;
                                } else
                                if (urlProto.equals(parProto) && !urlPath.startsWith("/")) {
                                    // included URL is "http[s]:..." with relative path.  construct absolute URL
                                    int cs = parent.indexOf("://");
                                    int ls = parent.lastIndexOf("/");
                                    if ((cs > 0) && (ls >= (cs + 3))) {
                                        url = new URL(parent.substring(0,ls+1) + urlPath);
                                    }
                                }
                            } else {
                                // unrecognized URL, leave as-is
                            }
                            if (url != null) {
                                if (this.debugMode) {
                                    Print.logInfo("(DEBUG) Including URL: ["+vs+"] " + url);
                                }
                                uis = url.openStream(); // may throw MalformedURLException
                                OrderedProperties props = new OrderedProperties(this.recursionLevel + 1, url);
                                props.put(RTKey.CONFIG_URL, url.toString());  // save CONFIG_URL for internal referencing 
                                props.load(uis);
                                props.remove(RTKey.CONFIG_URL);               // remove CONFIG_URL before saving to parent properties
                                this.orderedMap.putAll(props.getOrderedMap());
                            }
                        } catch (MalformedURLException mue) {
                            Print.logException("Invalid URL: " + url, mue);
                        } catch (IllegalArgumentException iae) {
                            Print.logException("Invalid URL arguments: " + url, iae);
                        } catch (Throwable th) { // IOException, UnknownHostException
                            if (!ks.equalsIgnoreCase(KEY_INCLUDE_URL_OPT)) {
                                Print.logException("Error including properties: " + url, th);
                            } else {
                                //Print.logWarn("Unable to include URL: " + v);
                            }
                        } finally {
                            if (uis != null) { try { uis.close(); } catch (IOException ioe) {/*ignore*/} }
                        }
                    }
                    return value;
                } else
                if (ks.equalsIgnoreCase(RTKey.LOG)) {
                    if (RTProperties.this.getConfigLogMessagesEnabled()) {
                        // not very efficient, but this doesn't need to be efficient since config files are seldom loaded.
                        StringBuffer sb = new StringBuffer();
                        if (this.inputURL != null) {
                            String filePath = this.inputURL.getPath();
                            int p = filePath.lastIndexOf("/");
                            String fileName = (p >= 0)? filePath.substring(p+1) : filePath;
                            sb.append("[").append(fileName).append("] ");
                        }
                        RTProperties tempProps = new RTProperties(this);
                        RTConfig.pushTemporaryProperties(tempProps);
                        Print.resetVars();
                        sb.append(RTConfig.insertKeyValues(vs,this.orderedMap)).append("\n");
                        Print._writeLog(sb.toString());
                        RTConfig.popTemporaryProperties(tempProps);
                    }
                    return value;
                } else
                if (ks.equalsIgnoreCase(RTKey.CONFIG_URL)) {
                    // special case assignment because the constant '%configURL' key is placed in
                    // the Properties map that is currently being loaded
                    Object rtn = super.put(key, value);
                    this.orderedMap.put(ks, vs);
                    return rtn;
                } else {
                    // invalid key reference
                    Print.logError("Invalid/unrecognized key specified: " + ks);
                    return value;
                }
            } else {
                Object rtn = super.put(key, value);
                this.orderedMap.put(ks, vs);
                return rtn;
            }
        }
        public Object remove(Object key) {
            if (key != null) {
                Object rtn = super.remove(key);
                this.orderedMap.remove(key.toString());
                return rtn;
            } else {
                return null;
            }
        }
        public OrderedMap<String,String> getOrderedMap() {
            return this.orderedMap;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static boolean STRING_PARSE_PROPS = true;
    
    private static boolean isEOL(byte b)     { return ((b == '\n') || (b == '\r')); }
    private static boolean isEOL(char b)     { return ((b == '\n') || (b == '\r')); }
    private static boolean isCOMMENT(byte b) { return ((b == '#')  || (b == '!') ); }
    private static boolean isCOMMENT(char b) { return ((b == '#')  || (b == '!') ); }
    private static boolean isSEP(byte b)     { return ((b == '=')  || (b == ':') ); }
    private static boolean isSEP(char b)     { return ((b == '=')  || (b == ':') ); }

    public static Properties loadProperties(Properties props, InputStream in)
        throws IOException
    {
        byte data[] = FileTools.readStream(in);
        if (STRING_PARSE_PROPS) {
            String dataStr = StringTools.toStringValue(data);
            String ds[] = StringTools.split(dataStr,'\n');
            for (int i = 0; i < ds.length; i++) {
                String d = ds[i].trim();
                if (d.equals("") || isCOMMENT(d.charAt(0))) { continue; }
                int p = d.indexOf("=");
                if (p < 0) { p = d.indexOf(":"); }
                String key = (p >= 0)? d.substring(0,p) : d;
                String val = (p >= 0)? d.substring(p+1) : "";
                if (!key.equals("")) {
                    Print.logInfo("S)Prop: " + key + " ==> " + val);
                    props.setProperty(key, val);
                }
            }
        } else {
            // This may not be safe for non-"ISO-8859-1" character sets
            for (int s = 0; s < data.length;) {
                // skip to start of next key
                while ((s < data.length) && Character.isWhitespace(data[s])) { s++; }
                if ((s >= data.length) || isCOMMENT(data[s])) {
                    while ((s < data.length) && !isEOL(data[s])) { s++; }
                    continue;
                }
                // find separator/eol
                int e, sep = -1;
                for (e = s; (e < data.length) && !isEOL(data[e]); e++) {
                    if ((sep < 0) && isSEP(data[e])) { sep = e; }
                }
                // parse key/value
                String key = "";
                String val = "";
                if (sep >= 0) {
                    key = StringTools.toStringValue(data, s, sep - s).trim();
                    // TODO: decode Unicode value
                    val = StringTools.toStringValue(data, sep + 1, e - sep).trim();
                } else {
                    key = StringTools.toStringValue(data, s, e - s).trim();
                    val = "";
                }
                if (!key.equals("")) {
                    Print.logInfo("B)Prop: " + key + " ==> " + val);
                    props.setProperty(key, val);
                }
                // start at nexe character
                s = e + 1;
            }
        }
        return props;
    }

    // ------------------------------------------------------------------------

    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv, new String[] { // validateKeyAttributes
            "s:",
            "b,bb:m,b",
            "f,ff,fff:f",
            "d,dd,ddd,dddd:d",
            "i=i",
            "g=o",
        });
    }

}
