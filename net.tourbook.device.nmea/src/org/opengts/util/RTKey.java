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
//  Runtime property keys
// ----------------------------------------------------------------------------
// Change History:
//  2006/03/26  Martin D. Flynn
//     -Initial release
//  2006/04/23  Martin D. Flynn
//     -Modified/Cleaned-up keys
//  2006/06/30  Martin D. Flynn
//     -Repackaged
//  2007/03/30  Martin D. Flynn
//     -Added "getRuntimeKeyIterator()"
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.io.*;
import java.util.*;
import java.lang.reflect.*;

public class RTKey
{

    // ------------------------------------------------------------------------

    public  static final String NULL_VALUE                  = "<null>";

    // ------------------------------------------------------------------------

    public  static final String DEFAULT_LOCALE              = "en";
    public  static final String DEFAULT_DATEFORMAT          = "yyyy/MM/dd";
    public  static final String DEFAULT_TIMEFORMAT          = "HH:mm:ss";

    // ------------------------------------------------------------------------
    
    private static String SENDMAIL_CLASS()
    {
        // This is done this way to avoid having to hardcode the fully qualified class name.
        return RTKey.class.getPackage().getName() + ".Send" + "Mail";
    }

    // ------------------------------------------------------------------------
    // property keys
    
    public static final String SESSION_NAME                 = "session.name";   // ie. PrivateLabel name
    public static final String SESSION_LOCALE               = "session.locale"; // ie. "en"

    public static final String RT_QUIET                     = "rtquiet";        // command-line use only
    public static final String RT_VERBOSE                   = "rtverbose";      // command-line use only

    public static final String LOCALE                       = "locale";
    public static final String LOCALE_DATEFORMAT            = "locale.dateFormat";
    public static final String LOCALE_TIMEFORMAT            = "locale.timeFormat";

    public static final String CONFIG_FILE_DIR              = "configFileDir";
    public static final String WEBAPP_FILE                  = "webappFile";
    public static final String CONFIG_FILE                  = "configFile";
    public static final String COMMAND_LINE_CONF            = "conf";           // alias for CONFIG_FILE for cmdLine use

    public static final String TEST_MODE                    = "testMode";
    public static final String DEBUG_MODE                   = "debugMode";
    public static final String ADMIN_MODE                   = "adminMode";

    public static final String HTTP_USER_AGENT              = "http.userAgent";
    public static final String HTTP_PROXY_HOST              = "http.proxy.host";
    public static final String HTTP_PROXY_PORT              = "http.proxy.port";
    public static final String URL_CONNECT_TIMEOUT          = "url.connect.timeout";
    public static final String URL_READ_TIMEOUT             = "url.read.timeout";

    public static final String SMTP_SERVER_HOST             = "smtp.host";
    public static final String SMTP_SERVER_PORT             = "smtp.port";
    public static final String SMTP_SERVER_USER             = "smtp.user";
    public static final String SMTP_SERVER_USER_EMAIL       = "smtp.user.emailAddress";
    public static final String SMTP_SERVER_PASSWORD         = "smtp.password";
    public static final String SMTP_ENABLE_SSL              = "smtp.enableSSL";
    public static final String SMTP_THREAD_MODEL            = "smtp.threadModel";
    public static final String SMTP_THREAD_MODEL_SHOW       = "smtp.threadModel.show";
    public static final String SMTP_DEBUG                   = "smtp.debug";

    public static final String LOG_NAME                     = "log.name";
    public static final String LOG_LEVEL                    = "log.level";
    public static final String LOG_LEVEL_HEADER             = "log.level.header";
    public static final String LOG_FILE                     = "log.file";
    public static final String LOG_FILE_ROTATE_SIZE         = "log.file.rotate.maxSize";
    public static final String LOG_FILE_ROTATE_EXTN         = "log.file.rotate.dateFormatExtn";
    public static final String LOG_FILE_ROTATE_DELETE_AGE   = "log.file.rotate.deleteAge";
    public static final String LOG_INCL_DATE                = "log.include.date";
    public static final String LOG_INCL_STACKFRAME          = "log.include.frame";
    public static final String LOG_EMAIL_EXCEPTIONS         = "log.email.sendExceptions";
    public static final String LOG_EMAIL_FROM               = "log.email.fromAddr";
    public static final String LOG_EMAIL_TO                 = "log.email.toAddr";
    public static final String LOG_SENDMAIL_CLASS           = "log.email.sendmailClass";
    
    public static final String DB_PROVIDER                  = "db.sql.provider";        // String
    public static final String DB_NAME                      = "db.sql.dbname";          // String
    public static final String DB_CONNECTION                = "db.sql.connection";      // String (replaced by DB_URL)
    public static final String DB_CONNECTION_DB             = "db.sql.connection.db";   // String (replaced by DB_URL_DB)
    public static final String DB_URL                       = "db.sql.url";             // String
    public static final String DB_URL_DB                    = "db.sql.url.db";          // String
    public static final String DB_HOST                      = "db.sql.host";            // String
    public static final String DB_PORT                      = "db.sql.port";            // Integer
    public static final String DB_USER                      = "db.sql.user";            // String
    public static final String DB_PASS                      = "db.sql.password";        // String
    public static final String DB_UTF8                      = "db.sql.utf8";            // Boolean
    public static final String DB_TABLE_NAME_PREFIX         = "db.tableNamePrefix";     // String
    public static final String DB_TABLE_LOCKING             = "db.tableLocking";        // Boolean?
    public static final String DB_SHOW_SQL                  = "db.showSQL";             // Boolean

    // ------------------------------------------------------------------------

    public static final String CONSTANT_PREFIX              = "%";
    
    public static final String VERSION                      = "%version";
    public static final String IS_WEBAPP                    = "%isWebApp";
    public static final String MAIN_CLASS                   = "%mainClass";
    public static final String CONTEXT_NAME                 = "%contextName";
    public static final String CONTEXT_PATH                 = "%contextPath";
    
    public static final String NAME                         = "%name";
    public static final String CONFIG_URL                   = "%configURL";
    public static final String LOG                          = "%log";
    public static final String INCLUDE                      = "%include";
    public static final String INCLUDE_OPT                  = "%include?";

    // ------------------------------------------------------------------------

    protected static Entry NullEntry = new Entry("", null);

    protected static Entry runtimeKeys[] = {

        new Entry("General mode attributes"),
        new Entry(ADMIN_MODE                 , false                            , "Admin mode enabled"),                        // APP
        new Entry(DEBUG_MODE                 , false                            , "Debug mode enabled"),                        // APP|WEB
        new Entry(TEST_MODE                  , false                            , "Test mode enabled"),                         // APP

        new Entry("Runtime config file attributes"),
        new Entry(CONFIG_FILE_DIR            , "/conf"                          , "Runtime config file directory"),             // APP|WEB
        new Entry(CONFIG_FILE                , "default.conf"                   , "Default runtime config file"),               // APP
        new Entry(WEBAPP_FILE                , "webapp.conf"                    , "Default webapp config file"),                //     WEB

        new Entry("Web Session context attributes"),
        new Entry(SESSION_NAME               , null                             , "Session context name"),                      // WEB
        new Entry(SESSION_LOCALE             , "en"                             , "Session locale"),                            // WEB

        new Entry("HTTP/URL attributes"),
        new Entry(HTTP_USER_AGENT            , null                             , "HTTP user agent"),                           // APP
        new Entry(HTTP_PROXY_HOST            , null                             , "HTTP proxy host"),                           // APP
        new Entry(HTTP_PROXY_PORT            , -1                               , "HTTP proxy port"),                           // APP
        new Entry(URL_CONNECT_TIMEOUT        , 60000L                           , "URL connection timeout (msec)"),             // APP
        new Entry(URL_READ_TIMEOUT           , 60000L                           , "URL read timeout (msec)"),                   // APP

        new Entry("Locale attributes"),
        new Entry(LOCALE                     , "en"                             , "Locale"),                                    // APP|WEB
        new Entry(LOCALE_DATEFORMAT          , DEFAULT_DATEFORMAT               , "Locale Date Format"),                        // APP|WEB
        new Entry(LOCALE_TIMEFORMAT          , DEFAULT_TIMEFORMAT               , "Locale Time Format"),                        // APP|WEB

        new Entry("SMTP (mail) attributes"),
        new Entry(SMTP_SERVER_HOST           , "smtp.example.com"               , "SMTP server host"),                          // APP|WEB
        new Entry(SMTP_SERVER_PORT           , 25                               , "SMTP server port"),                          // APP|WEB
        new Entry(SMTP_SERVER_USER           , null                             , "SMTP server user"),                          // APP|WEB
        new Entry(SMTP_SERVER_USER_EMAIL     , null                             , "SMTP User Email address (if required)"),     // APP|WEB
        new Entry(SMTP_SERVER_PASSWORD       , null                             , "SMTP server password"),                      // APP|WEB
        new Entry(SMTP_ENABLE_SSL            , null                             , "SMTP enable SSL"),                           // APP|WEB
        new Entry(SMTP_THREAD_MODEL          , null                             , "Send-Mail thread model"),
        new Entry(SMTP_THREAD_MODEL_SHOW     , false                            , "Print/show Send-Mail thread model"),
        new Entry(SMTP_DEBUG                 , false                            , "Sendmail debug mode"),

        new Entry("'Print' util attributes"),
        new Entry(LOG_NAME                   , null                             , "log name"),                                  // APP|WEB
        new Entry(LOG_LEVEL                  , Print.LOG_ALL                    , "log level"),                                 // APP|WEB
        new Entry(LOG_LEVEL_HEADER           , Print.LOG_ALL                    , "log header level"),                          // APP|WEB
        new Entry(LOG_FILE                   , null                             , "logfile name"),                              // APP|WEB
        new Entry(LOG_FILE_ROTATE_SIZE       , 200000L                          , "logfile rotate max size"),                   // APP|WEB
        new Entry(LOG_FILE_ROTATE_EXTN       , "yyyyMMddHHmmss.SSS'.log'"       , "logfile rotate date format extension"),      // APP|WEB
        new Entry(LOG_FILE_ROTATE_DELETE_AGE , "0"                              , "logfile rotate delete age (default days)"),  // APP|WEB
        new Entry(LOG_INCL_DATE              , false                            , "include date in logs"),                      // APP|WEB
        new Entry(LOG_INCL_STACKFRAME        , false                            , "include stackframe in logs"),                // APP|WEB
        new Entry(LOG_EMAIL_EXCEPTIONS       , false                            , "EMail exceptions"),                          // APP|WEB
        new Entry(LOG_EMAIL_FROM             , null                             , "Error email sender"),
        new Entry(LOG_EMAIL_TO               , null                             , "Error email recipient"),
        new Entry(LOG_SENDMAIL_CLASS         , SENDMAIL_CLASS()                 , "Sendmail class name"),                       // APP|WEB

        new Entry("DB attributes"),
        new Entry(DB_PROVIDER                , "mysql"                          , "Database provider"),                         // APP
        new Entry(DB_NAME                    , "?"                              , "Database name"),                             // APP
        new Entry(DB_URL                     , ""                               , "Database JDBC URL"),                         // APP|WEB
        new Entry(DB_URL_DB                  , ""                               , "Database JDBC URL (incl DB name)"),          // APP|WEB
        new Entry(DB_CONNECTION              , ""                               , "Database JDBC URL"),                         // APP|WEB
        new Entry(DB_CONNECTION_DB           , ""                               , "Database JDBC URL (incl DB name)"),          // APP|WEB
        new Entry(DB_HOST                    , "127.0.0.1" /*"localhost"*/      , "Database server host"),                      // APP|WEB
        new Entry(DB_PORT                    , -1                               , "Database server port"),                      // APP|WEB
        new Entry(DB_USER                    , ""                               , "Database server user"),                      // APP|WEB
        new Entry(DB_PASS                    , ""                               , "Database server password"),                  // APP|WEB
        new Entry(DB_TABLE_NAME_PREFIX       , ""                               , "Table name prefix"),                         // APP|WEB
        new Entry(DB_TABLE_LOCKING           , false                            , "Table locking enabled"),                     // APP|WEB
        new Entry(DB_SHOW_SQL                , false                            , "Show insert/update SQL"),                    // APP|WEB

    };

    // ------------------------------------------------------------------------

    protected static Map<String,Entry>  globalEntryMap = null;
    protected static RTProperties       defaultProperties = null;

    protected static Map<String,Entry> getRuntimeEntryMap()
    {
        if (globalEntryMap == null) {
            /* create map */
            globalEntryMap = new OrderedMap<String,Entry>();
            
            /* load default key entries */
            for (int i = 0; i < RTKey.runtimeKeys.length; i++) {
                String rtKey = RTKey.runtimeKeys[i].getKey();
                if (rtKey != null) {
                    globalEntryMap.put(rtKey, RTKey.runtimeKeys[i]);
                }
            }

        }
        return globalEntryMap;
    }

    public static Iterator<String> getRuntimeKeyIterator()
    {
        return RTKey.getRuntimeEntryMap().keySet().iterator();
    }

    public static void addRuntimeEntries(Entry dftEntry[])
    {
        if (dftEntry != null) {
            Map<String,Entry> gblmap = RTKey.getRuntimeEntryMap();
            for (int i = 0; i < dftEntry.length; i++) {
                String rtKey = dftEntry[i].getKey();
                if (rtKey != null) {
                    gblmap.put(rtKey, dftEntry[i]);
                }
            }
            defaultProperties = null;
        }
    }
    
    public static void addRuntimeEntry(Entry dftEntry)
    {
        if (dftEntry != null) {
            String rtKey = dftEntry.getKey();
            if (rtKey != null) {
                RTKey.getRuntimeEntryMap().put(rtKey, dftEntry);
                defaultProperties = null;
            }
        }
    }

    // ------------------------------------------------------------------------

    protected static Entry getRuntimeEntry(String key)
    {
        return (key != null)? (Entry)RTKey.getRuntimeEntryMap().get(key) : null;
    }

    // ------------------------------------------------------------------------

    public static boolean hasDefault(String key)
    {
        return (RTKey.getRuntimeEntry(key) != null);
    }

    public static Object getDefaultProperty(String key, Object dft)
    {
        Entry rtKey = RTKey.getRuntimeEntry(key);
        return (rtKey != null)? rtKey.getDefault() : dft;
    }
    
    public static void setDefaultProperty(String key, Object val)
    {
        Entry rtKey = RTKey.getRuntimeEntry(key);
        if (rtKey != null) {
            rtKey.setDefault(val);
        } else {
            RTKey.addRuntimeEntry(new Entry(key,val));
        }
    }

    public static RTProperties getDefaultProperties()
    {
        if (defaultProperties == null) {
            RTProperties rtp = new RTProperties();
            for (Iterator<Entry> v = RTKey.getRuntimeEntryMap().values().iterator(); v.hasNext();) {
                Entry rtk = (Entry)v.next();
                if (!rtk.isHelp()) {
                    String key = rtk.getKey();
                    Object val = rtk.getDefault();
                    rtp.setProperty(key, val);
                }
            }
            defaultProperties = rtp;
        }
        return defaultProperties;
    }

    // ------------------------------------------------------------------------

    public static class Entry
    {

        private String key  = null;
        private Object dft  = null;
        private String hlp  = null;
        private int    ref  = 0;    // cyclical reference test

        public Entry(String key, Object dft, String help) {
            this.key = key;
            this.dft = dft;
            this.hlp = help;
        }
        public Entry(String key, Object dft) {
            this(key, dft, null);
        }
        public Entry(String help) {
            this(null, null, help);
        }

        public Entry(String key, int dft, String help) {
            this(key, new Integer(dft), help);
        }
        public Entry(String key, int dft) {
            this(key, dft, null);
        }

        public Entry(String key, long dft, String help) {
            this(key, new Long(dft), help);
        }
        public Entry(String key, long dft) {
            this(key, dft, null);
        }

        public Entry(String key, double dft, String help) {
            this(key, new Double(dft), help);
        }
        public Entry(String key, double dft) {
            this(key, dft, null);
        }

        public Entry(String key, float dft, String help) {
            this(key, new Float(dft), help);
        }
        public Entry(String key, float dft) {
            this(key, dft, null);
        }

        public Entry(String key, boolean dft, String help) {
            this(key, new Boolean(dft), help);
        }
        public Entry(String key, boolean dft) {
            this(key, dft, null);
        }

        public Entry getRealEntry() {
            if (this.dft instanceof EntryReference) {
                Entry entry = null;
                if (this.ref > 0) {
                    Print.logStackTrace("Cyclical EntryReference: " + this.getKey());
                    entry = NullEntry;
                } else {
                    this.ref++;
                    try {
                        EntryReference entryRef = (EntryReference)this.dft;
                        Entry nextEntry = entryRef.getReferencedEntry(); // <-- will display error, if not found
                        entry = (nextEntry != null)? nextEntry.getRealEntry() : NullEntry;
                    } finally {
                        this.ref--;
                    }
                }
                return entry;
            } else {
                return this;
            }
        }

        public boolean isReference() {
            return (this.dft instanceof EntryReference);
        }

        public String getKey() {
            return this.key;
        }
        public Object getDefault() {
            return this.isReference()? this.getRealEntry().getDefault() : this.dft;
        }

        public void setDefault(Object val) {
            this.dft = val;
        }

        public boolean isHelp() {
            return (this.key == null);
        }
        public String getHelp() {
            return (this.hlp != null)? this.hlp : "";
        }

        public String toString(Object v) {
            StringBuffer sb = new StringBuffer();
            if (this.isHelp()) {
                sb.append("# --- ").append(this.getHelp());
            } else {
                sb.append(this.getKey()).append("=");
                sb.append((v != null)? v : NULL_VALUE);
            }
            return sb.toString();
        }

        public String toString() {
            return this.toString(this.getDefault());
        }

    }

    public static class EntryReference
    {
        private String refKey = null;
        public EntryReference(String key) {
            this.refKey = key;
        }
        public String getKey() {
            return this.refKey;
        }
        public Entry getReferencedEntry() {
            Entry entry = getRuntimeEntry(this.getKey());
            if (entry == null) {
                Print.logStackTrace("Entry reference not found: " + this.getKey());
            }
            return entry;
        }
        public String toString() {
            String k = this.getKey();
            return (k != null)? k : "";
        }
        public boolean equals(Object other) {
            if (other instanceof EntryReference) {
                return this.toString().equals(other.toString());
            } else {
                return false;
            }
        }
    }

    // ------------------------------------------------------------------------

    public static void printDefaults(PrintStream out)
    {

        /* print standard runtime entries */
        Set<String> keyList = new OrderedSet<String>();
        String keyGrp = null;

        for (Iterator<Entry> v = RTKey.getRuntimeEntryMap().values().iterator(); v.hasNext();) {
            Entry rtk = (Entry)v.next();
            if (rtk.isHelp()) {
                out.println("");
                out.println("# ===== " + rtk.getHelp());
            } else {
                Object dft = rtk.getDefault();
                out.println("# --- " + rtk.getHelp());
                out.println("# " + rtk.toString(dft));
                String key = rtk.getKey();
                keyList.add(key);
                if (!key.equals(CONFIG_FILE) && RTConfig.hasProperty(key)) {
                    String val = RTConfig.getString(key, null);
                    //if ((val != null) && ((dft == null) || !val.equals(dft.toString()))) {
                        out.println(rtk.toString(val));
                    //}
                }
            }
        }

        /* orphaned entries */
        RTProperties cmdLineProps = RTConfig.getConfigFileProperties();
        if (cmdLineProps != null) {
            boolean orphanHeader = false;
            for (Iterator i = cmdLineProps.keyIterator(); i.hasNext();) {
                Object k = i.next();
                if (!k.equals(COMMAND_LINE_CONF) && !keyList.contains(k)) {
                    if (!orphanHeader) {
                        out.println("");
                        out.println("# ===== Other entries");
                        orphanHeader = true;
                    }
                    Object v = cmdLineProps.getProperty(k, null);
                    out.println(k + "=" + ((v != null)? v : NULL_VALUE));
                }
            }
        }

        /* final blank line */
        out.println("");
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        printDefaults(System.out);
    }

    // ------------------------------------------------------------------------

}