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
//  Support for hierarchical runtime properties
// ----------------------------------------------------------------------------
// Change History:
//  2006/03/26  Martin D. Flynn
//     -Initial release
//  2006/04/23  Martin D. Flynn
//     -Changed support for default properties
//  2006/06/30  Martin D. Flynn
//     -Repackaged
//  2007/03/30  Martin D. Flynn
//     -Added support for immutable System properties
//  2007/05/06  Martin D. Flynn
//     -Added support for checking invalid command-line args
//  2007/06/13  Martin D. Flynn
//     -Catch 'SecurityException's when getting System properties.
//  2007/08/09  Martin D. Flynn
//     -Changed the way this module searches for runtime config files.
//  2007/09/16  Martin D. Flynn
//     -Added method 'insertKeyValues'
//     -Added support for key/value replace in config-file value strings
//  2008/06/20  Martin D. Flynn
//     -Added 'System.getenv()' map checking
//  2008/10/16  Martin D. Flynn
//     -Removed check for default value when an explicit default value is specified.
//  2009/01/28  Martin D. Flynn
//     -Added custom key/value map option to 'insertKeyValues' method to allow
//      overriding runtime keys with custom key/value map.
//     -Added ability to specify a thread-temporary RTProperties instance.
//  2009/02/05  Martin D. Flynn
//     -Added "reload()" method
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.io.*;
import java.util.*;
import java.net.*;

public class RTConfig
{

    // ------------------------------------------------------------------------

    // Cannot initialize here, otherwise we would be unable to override 'configFile'
    //static { _startupInit(false); }

    // ------------------------------------------------------------------------

    private static boolean ENABLE_ENVIRONMENT_VARIABLES = true;
    
    public static void setEnvironmentVariablesEnabled(boolean enable)
    {
        ENABLE_ENVIRONMENT_VARIABLES = enable;
    }
    
    public static boolean getEnvironmentVariablesEnabled()
    {
        return ENABLE_ENVIRONMENT_VARIABLES;
    }

    // ------------------------------------------------------------------------

    private static boolean verbose = false;
    private static boolean quiet   = false;

    // ------------------------------------------------------------------------

    private static String localhostName = null;

    /**
    *** Lookup and return the local host name 
    *** @return The local host name
    **/
    public static String getHostName()
    {
        /* host name */
        if (RTConfig.localhostName == null) {
            try {
                String hd = InetAddress.getLocalHost().getHostName();
                int p = hd.indexOf(".");
                RTConfig.localhostName = (p >= 0)? hd.substring(0,p) : hd;
            } catch (UnknownHostException uhe) {
                RTConfig.localhostName = "UNKNOWN";
            }
        }
        return RTConfig.localhostName;
    }

    // ------------------------------------------------------------------------

    private static final int    THREAD_LOCAL        = 0;
    private static final int    RUNTIME_CONSTANT    = THREAD_LOCAL     + 1;
    private static final int    SERVLET_CONTEXT     = RUNTIME_CONSTANT + 1;
    private static final int    COMMAND_LINE        = SERVLET_CONTEXT  + 1;
    private static final int    CONFIG_FILE         = COMMAND_LINE     + 1;
    private static final int    SYSTEM_PROPS        = CONFIG_FILE      + 1;
    private static final int    ENVIRONMENT         = SYSTEM_PROPS     + 1;

    private static RTProperties CFG_PROPERTIES[] = new RTProperties[ENVIRONMENT + 1];

    public static String getConfigPropertiesIndexName(int ndx)
    {
        switch (ndx) {
            case THREAD_LOCAL     : return "ThreadLocal";
            case RUNTIME_CONSTANT : return "RuntimeConstant";
            case SERVLET_CONTEXT  : return "ServletContext";
            case COMMAND_LINE     : return "CommandLine";
            case CONFIG_FILE      : return "ConfigFile";
            case SYSTEM_PROPS     : return "SystemProps";
            case ENVIRONMENT      : return "EnvironmentVars";
            default               : return "Unknown(" + ndx + ")";
        }
    }

    // ------------------------------------------------------------------------

    private static ThreadLocal<Stack<RTProperties>> CFG_THREAD_TEMPORARY = null;

    /**
    *** Prints the temporary RTProperty instances to stdout
    *** @return The temporary RTProperty instance that contains the specified key
    **/
    protected static void printTemporaryProperties(String msg)
    {
        if (CFG_THREAD_TEMPORARY != null) {
            Stack<RTProperties> rtpStack = CFG_THREAD_TEMPORARY.get();
            if (rtpStack != null) {
                int level = 1;
                for (RTProperties rtp : rtpStack) {
                    rtp.printProperties(msg + " #" + level);
                    level++;
                }
            }
        }
    }

    /**
    *** Returns the temporary RTProperty instance that contains the specified key
    *** @return The temporary RTProperty instance that contains the specified key
    **/
    protected static RTProperties getTemporaryProperties(String key)
    {

        /* find temporary properties that has the specified key */
        if (CFG_THREAD_TEMPORARY != null) {
            Stack<RTProperties> rtpStack = CFG_THREAD_TEMPORARY.get();
            if (rtpStack != null) {
                for (RTProperties rtp : rtpStack) {
                    if (rtp.hasProperty(key)) {
                        return rtp;
                    }
                }
            }
        }

        /* not found */
        return null;

    }

    /**
    *** Pushes the RTProperties instance onto a temporary stack for the current thread
    *** @param props  The RTProperties instance
    **/
    public static void pushTemporaryProperties(RTProperties props)
    {

        /* ignore if properties are null */
        if (props == null) {
            Print.logStackTrace("**** Attempting to push a null RTProperties instance!");
            return;
        }

        /* create the ThreadLocal instance */
        if (CFG_THREAD_TEMPORARY == null) {
            synchronized (CFG_PROPERTIES) {
                if (CFG_THREAD_TEMPORARY == null) { // likely still null after lock
                    CFG_THREAD_TEMPORARY = new ThreadLocal<Stack<RTProperties>>();
                }
            }
        }

        /* get the current thread property stack */
        Stack<RTProperties> rtpStack = CFG_THREAD_TEMPORARY.get();
        if (rtpStack == null) {
            rtpStack = new Stack<RTProperties>();
            CFG_THREAD_TEMPORARY.set(rtpStack);
        }

        /* push the properties onto the stack */
        rtpStack.push(props);

    }

    /**
    *** Pops the last temporary RTProperty instance for the current thread
    *** @param props  The RTProperties instance that is the expected entry on the top of the stack
    **/
    public static void popTemporaryProperties(RTProperties props)
    {

        /* remove the last RTProperties from the list [LIFO] */
        if (CFG_THREAD_TEMPORARY != null) {
            Stack<RTProperties> rtpStack = CFG_THREAD_TEMPORARY.get();
            if ((rtpStack != null) && !rtpStack.empty()) {

                /* pop last RTProperties entry */
                RTProperties rtp = rtpStack.pop();
                if ((props != null) && (props != rtp)) {
                    Print.logStackTrace("Unexpected RTProperties at top of stack");
                }
                //Print.logInfo("Stack size: %d", rtpStack.size());

                /* remove Stack if empty */
                if (rtpStack.empty()) {
                    // discard Stack (garbage collect)
                    CFG_THREAD_TEMPORARY.set(null);
                }

            } else
            if (props != null) {

                /* warning */
                Print.logStackTrace("RTProperties Stack is already empty");

            }
        }

    }

    /**
    *** Getss the temporary RTProperty stack size (for current thread)
    *** @param props  The RTProperties instance that is the expected entry on the top of the stack
    **/
    public static int getTemporaryPropertiesStackSize()
    {
        if (CFG_THREAD_TEMPORARY != null) {
            Stack<RTProperties> rtpStack = CFG_THREAD_TEMPORARY.get();
            return (rtpStack != null)? rtpStack.size() : 0;
        } else {
            return 0;
        }
    }

    // ------------------------------------------------------------------------

    public static RTProperties getThreadProperties()
    {
        if (CFG_PROPERTIES[THREAD_LOCAL] == null) {
            synchronized (CFG_PROPERTIES) {
                if (CFG_PROPERTIES[THREAD_LOCAL] == null) {
                    CFG_PROPERTIES[THREAD_LOCAL] = new RTProperties(new ThreadLocalMap<Object,Object>());
                }
            }
        }
        return CFG_PROPERTIES[THREAD_LOCAL];
    }

    public static RTProperties getRuntimeConstantProperties()
    {
        if (CFG_PROPERTIES[RUNTIME_CONSTANT] == null) {
            synchronized (CFG_PROPERTIES) {
                if (CFG_PROPERTIES[RUNTIME_CONSTANT] == null) {
                    CFG_PROPERTIES[RUNTIME_CONSTANT] = new RTProperties();
                }
            }
        }
        return CFG_PROPERTIES[RUNTIME_CONSTANT];
    }

    public static RTProperties getServletContextProperties()
    {
        return CFG_PROPERTIES[SERVLET_CONTEXT]; // may be null if not initialized
    }

    public static RTProperties getCommandLineProperties()
    {
        return CFG_PROPERTIES[COMMAND_LINE]; // may be null if not initialized
    }

    public static RTProperties getConfigFileProperties()
    {
        if (CFG_PROPERTIES[CONFIG_FILE] == null) {
            // this should have been initialized before, but force initialization now
            if (RTConfig.verbose) { Print.logInfo("Late initialization!!!"); }
            _startupInit(false);
        }
        if (CFG_PROPERTIES[CONFIG_FILE] != null) {
            return CFG_PROPERTIES[CONFIG_FILE];
        } else {
            Print.sysPrintln("'RTConfig.getConfigFileProperties()' returning temporary RTProperties");
            return new RTProperties();
        }
    }

    public static RTProperties getSystemProperties()
    {
        if (CFG_PROPERTIES[SYSTEM_PROPS] == null) {
            // this should have been initialized before, but force initialization now
            if (RTConfig.verbose) { Print.logInfo("Late initialization!!!"); }
            _startupInit(false);
        }
        return CFG_PROPERTIES[SYSTEM_PROPS];
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the RTProperties for the specified key
    *** @param key    The key
    *** @param dftOk  True to check defaults, if not found elsewhere
    *** @return The RTProperties
    **/
    public static RTProperties getPropertiesForKey(String key, boolean dftOk)
    {
        if (key != null) {

            /* initialized? */
            if (!isInitialized()) {
                // 'Print._println...' used here to eliminate possible recursion stack-overflow
                //Print._println(null, "ConfigFile not yet loaded");
                //Thread.dumpStack();
                // continue ...
            }

            /* first try the thread local temporary properties */
            RTProperties tempProps = RTConfig.getTemporaryProperties(key);
            if (tempProps != null) {
                //if (key.equals(testKey)) System.out.println("RTConfig.getPropertiesForKey: Found "+testKey+" @ " + getTemporaryProperties());
                return tempProps;
            }

            /* look for key in our property list stack */
            //String testKey = RTKey.LOG_LEVEL_HEADER;
            for (int i = 0; i < CFG_PROPERTIES.length; i++) {
                RTProperties rtProps = CFG_PROPERTIES[i];
                if ((rtProps != null) && rtProps.hasProperty(key)) {
                    //if (key.equals(testKey)) System.out.println("RTConfig.getPropertiesForKey: Found "+testKey+" @ " + getConfigPropertiesIndexName(i));
                    return rtProps; 
                }
            }
            
            /* still not found, try the default properties */
            if (dftOk) {
                RTProperties dftProps = RTKey.getDefaultProperties();
                if ((dftProps != null) && dftProps.hasProperty(key)) {
                    //if (key.equals(testKey)) System.out.println("RTConfig.getPropertiesForKey: Found "+testKey+" in defaults");
                    return dftProps;
                }
            }
            
        }
        return null;
    }

    /**
    *** Returns the name of the RTProperties location for the specified key
    *** @param key    The key
    *** @return The name of the RTProperties location
    **/
    public static String findPropertiesForKey(String key)
    {
        if (key != null) {

            /* first try the thread local temporary properties */
            RTProperties tempProps = RTConfig.getTemporaryProperties(key);
            if (tempProps != null) {
                return "Temporary";
            }

            /* look for key in our property list stack */
            for (int i = 0; i < CFG_PROPERTIES.length; i++) {
                RTProperties rtProps = CFG_PROPERTIES[i];
                if ((rtProps != null) && rtProps.hasProperty(key)) {
                    return getConfigPropertiesIndexName(i); 
                }
            }

            /* still not found, try the default properties */
            RTProperties dftProps = RTKey.getDefaultProperties();
            if ((dftProps != null) && dftProps.hasProperty(key)) {
                return "Defaults";
            }

        }
        return null;
    }

    // ------------------------------------------------------------------------

    public static int setCommandLineArgs(String argv[])
    {
        return RTConfig.setCommandLineArgs(argv, null);
    }

    public static int setCommandLineArgs(String argv[], String keyAttr[])
    {
        RTProperties cmdArgs = (argv != null)? new RTProperties(argv) : new RTProperties();
        RTConfig.setCommandLineArgs(cmdArgs, false);
        if (!ListTools.isEmpty(keyAttr)) {
            PrintStream out = Print.getSystemOut();
            if (!cmdArgs.validateKeyAttributes(out,keyAttr)) {
                throw new RuntimeException("Invalid arguments specified");
            }
        }
        return cmdArgs.getNextCommandLineArgumentIndex();
    }

    public static void setCommandLineArgs(RTProperties cmdLineProps)
    {
        if (cmdLineProps != null) {
            RTConfig.setCommandLineArgs(cmdLineProps, false);
        } else {
            RTConfig.setCommandLineArgs(new RTProperties(), false);
        }
    }

    public static int setCommandLineArgs(String argv[], boolean testMode)
    {
        RTProperties cmdArgs = (argv != null)? new RTProperties(argv) : new RTProperties();
        RTConfig.setCommandLineArgs(cmdArgs, testMode);
        return cmdArgs.getNextCommandLineArgumentIndex();
    }

    public static void setCommandLineArgs(RTProperties cmdLineProps, boolean testMode)
    {
        if (cmdLineProps != null) {
            cmdLineProps.setIgnoreKeyCase(true);
            //Class mainClass = RTConfig.getMainClass(); // may be null
            //if (mainClass != null) {
            //    cmdLineProps.setProperty(RTKey.MAIN_CLASS, mainClass); // sets tha actual class
            //    String mainClassStr = StringTools.className(mainClass);
            //    int p = mainClassStr.lastIndexOf(".");
            //    String contextName = (p >= 0)? mainClassStr.substring(p+1) : mainClassStr;
            //    cmdLineProps.setProperty(RTKey.CONTEXT_NAME, contextName);
            //}
            if (CFG_PROPERTIES[COMMAND_LINE] == null) {
                // first initialization
                CFG_PROPERTIES[COMMAND_LINE] = cmdLineProps;     
                _startupInit(true); // initialize now to allow for overriding 'configFile'
            } else {
                // subsequent re-initialization
                CFG_PROPERTIES[COMMAND_LINE].setProperties(cmdLineProps);
            }
        } else {
            _startupInit(true);
        }
    }

    // ------------------------------------------------------------------------

    private static Set<String> _parseArgs(Object argv, Set<String> set)
    {
        if ((argv != null) && (set != null)) {
            if (argv instanceof Object[]) {
                Object a[] = (Object[])argv;
                for (int i = 0; i < a.length; i++) {
                    RTConfig._parseArgs(a[i], set);
                }
            } else {
                set.add(argv.toString());
            }
        }
        return set;
    }

    public static String[] validateCommandLineArgs(Object argv)
    {
        RTProperties cmdLineProps = RTConfig.getCommandLineProperties();
        if (cmdLineProps != null) {
            java.util.List<String> badArgs = new Vector<String>();
            Set<String> argSet = new HashSet<String>();
            //argSet.add(RTKey.MAIN_CLASS);
            RTConfig._parseArgs(argv, argSet);
            for (Iterator keys = cmdLineProps.keyIterator(); keys.hasNext();) {
                String k = (String)keys.next();
                if (RTKey.hasDefault(k) || RTKey.COMMAND_LINE_CONF.equals(k)) {
                    // defaulted keys and "-conf", "-configFile", "-configFileDir" are ok
                } else
                if (!argSet.contains(k)) {
                    badArgs.add(k);
                }
            }
            return badArgs.isEmpty()? null : (String[])badArgs.toArray(new String[badArgs.size()]);
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /** 
    *** Sets the runtime mode to 'WebApp' and initializes the Servlet context
    *** properties.
    *** @param props  A map containing the Servlet context properties
    **/
    public static void setServletContextProperties(Map<Object,Object> props)
    {
        //RTConfig.verbose = true; // default to verbose
        RTConfig.setWebApp(true); // force isWebapp=true
        CFG_PROPERTIES[SERVLET_CONTEXT] = new RTProperties(props);
        RTConfig._startupInit(false);
    }

    /**
    *** Clears the runtime Servlet context properties
    *** This is intended to be called when the Servlet context is destroyed
    *** @param servlet The servlet instance (not currently used)
    **/
    public static void clearServletContextProperties(Object servlet)
    {
        CFG_PROPERTIES[SERVLET_CONTEXT] = null;
    }

    // ------------------------------------------------------------------------

    private static int      _didStartupInit     = 0;  // 0=not initialized, 1=initializing, 2=initialized
    private static boolean  _allowSysPropChange = false; // valid iff _didStartupInit!=0
    private static URL      _foundConfigURL     = null;
    private static File     _foundConfigFile    = null;

    /**
    *** Returns true if the Runtime config is in the process of being initialized
    *** @return True if in the process of initializing
    **/
    public static boolean isInitializing()
    {
        return (_didStartupInit == 1);
    }

    /**
    *** Returns true if the Runtime config has been initialized
    *** @return True if initialized
    **/
    public static boolean isInitialized()
    {
        return (_didStartupInit == 2);
    }

    /**
    *** Returns the URL of the loaded config file
    *** @return The URL of the loaded config file
    **/
    public static URL getLoadedConfigURL()
    {
        return _foundConfigURL;
    }

    /**
    *** Returns the File instance of the loaded config file, or null if the runtime config
    *** was not loaded from a file.
    *** @return The File instance of the loaded config file.
    **/
    public static File getLoadedConfigFile()
    {
        if (_foundConfigFile == null) {
            if ((_foundConfigURL != null) && (_foundConfigURL.getProtocol().equalsIgnoreCase("file"))) {
                String path = _foundConfigURL.getPath();
                _foundConfigFile = new File(path);
            }
        }
        return _foundConfigFile;
    }
    
    /**
    *** Returns the File instance of the loaded config file directory, or null if the runtime config
    *** was not loaded from a file.
    *** @return The File instance of the loaded config file directory.
    **/
    public static File getLoadedConfigDir()
    {
        File cfgFile = RTConfig.getLoadedConfigFile();
        if (cfgFile != null) {
            return cfgFile.getParentFile();
        } else {
            return null;
        }
    }

    /**
    *** Returns the config file URL
    *** @return The config file URL
    **/
    protected static URL getConfigURL()
    {
        try{
            URL cfgURL = RTConfig._getConfigURL();
            if (cfgURL != null) {
                if (RTConfig.verbose) { 
                    Print.logInfo("Config URL found at " + cfgURL); 
                }
                return cfgURL;
            } else {
                if (!RTConfig.quiet) { 
                    Print.logWarn("No valid config URL was found"); 
                }
                return null;
            }
        } catch (MalformedURLException mue) {
            Print.logError("Invalid URL: " + mue);
        } catch (Throwable t) {
            Print.logException("Invalid URL", t);
        }
        return null;
    }

    /**
    *** Searches for, and returns the URL of the runtime configuration file
    *** @return The URL of the runtime configuration file
    **/
    protected static URL _getConfigURL()
        throws MalformedURLException
    {
        // This module checks for the runtime config file in the following order:
        //  - Explicit "-configFile=<file>" specification
        //  - Explicit "-conf=<file>" specification                         (if command-line)
        //  - Resource "[default|webapp]_<host>.conf"
        //  - Resource "[default|webapp].conf"
        //  - Servlet "<Context>/WEB-INF/webapp_<host>.conf"                (if servlet)
        //  - Servlet "<Context>/WEB-INF/webapp.conf"                       (if servlet)
        //  - FileSystem "<CONFIG_FILE_DIR>/[default|webapp]_<host>.conf"
        //  - FileSystem "<CONFIG_FILE_DIR>/[default|webapp].conf"

        /* init */
        String       hostName      = RTConfig.getHostName();    // should not be null
        RTProperties cmdLineProps  = RTConfig.getCommandLineProperties();
        RTProperties constantProps = RTConfig.getRuntimeConstantProperties();
        boolean      isCommandLine = (cmdLineProps != null);
        boolean      isTrueServlet = !isCommandLine && RTConfig.isWebApp();
        // A 'fake' servlet is one where the command-line app called "setWebApp(true)"

        /* Check for "-configFile=<file>" */
        if (RTConfig.hasProperty(RTKey.CONFIG_FILE,false)) {
            File cfgFile = RTConfig.getFile(RTKey.CONFIG_FILE);
            if (RTConfig.hasProperty(RTKey.CONFIG_FILE_DIR,false)) {
                File cfgDir = RTConfig.getFile(RTKey.CONFIG_FILE_DIR);
                cfgFile = new File(cfgDir, cfgFile.toString());
            }
            if (cfgFile.isFile()) {
                return cfgFile.toURL();
            } else {
                Print.logError("Explicitly specified config file does not exist: " + cfgFile);
                return null;
            }
        }

        /* Check for command-line "-conf=<file>" */
        if (isCommandLine) {
            //Print.logInfo("Search Command-line ...");
            /* check for alternate command line override '-conf=<file>' */
            File cfgFile = cmdLineProps.getFile(RTKey.COMMAND_LINE_CONF, null);
            if (cfgFile != null) {
                if (RTConfig.hasProperty(RTKey.CONFIG_FILE_DIR,false)) {
                    File cfgDir = RTConfig.getFile(RTKey.CONFIG_FILE_DIR);
                    cfgFile = new File(cfgDir, cfgFile.toString());
                }
                if (cfgFile.isFile()) {
                    return cfgFile.toURL();
                } else {
                    Print.logError("Explicitly specified config file does not exist: " + cfgFile);
                    return null;
                }
            } else {
                // continue
            }
        } else {
            //Print.logInfo("Not a Command-Line ...");
            // continue
        }

        /* separate directory from path */
        File rtCfgDir  = null;
        File rtCfgFile = null;
        if (isTrueServlet) {
            rtCfgDir  = RTConfig.getFile(RTKey.CONFIG_FILE_DIR);
            rtCfgFile = RTConfig.getFile(RTKey.WEBAPP_FILE);     // should be simply "webapp.conf"
        } else {
            rtCfgFile = RTConfig.getFile(RTKey.CONFIG_FILE);
            if (rtCfgFile.isAbsolute()) {
                rtCfgDir  = rtCfgFile.getParentFile();
                rtCfgFile = new File(rtCfgFile.getName());
            } else {
                File cf   = new File(RTConfig.getFile(RTKey.CONFIG_FILE_DIR), rtCfgFile.toString());
                rtCfgDir  = cf.getParentFile();
                rtCfgFile = new File(cf.getName());
            }
        }

        /* separate file name from extension */
        String cfgFileName = rtCfgFile.toString();
        int    cfgExtPos   = cfgFileName.lastIndexOf(".");
        String cfgName     = (cfgExtPos >= 0)? cfgFileName.substring(0,cfgExtPos) : cfgFileName;
        String cfgExtn     = (cfgExtPos >= 0)? cfgFileName.substring(cfgExtPos  ) : "";

        /* special servlet context config files */
        if (isTrueServlet) {
            //Print.logInfo("Searching Servlet context for config file ...");
            // RTConfigContextListener check
            if (RTConfig.getServletContextProperties() == null) {
                // If we are here, then this means that 'ContextListener' was not specified for this 
                // context and we are initializing late (possible too late).
                Print.logWarn("---------------------------------------------------------------------");
                Print.logWarn("** WebApp: " + RTConfig.getServletClassName());
                Print.logWarn("** Appears to be missing the 'RTConfigContextListener' initialization");
                Print.logWarn("---------------------------------------------------------------------");
            }
            // check "<ContextPath>/[WEB-INF/]webapp[_<host>].conf"
            String ctxPath = constantProps.getString(RTKey.CONTEXT_PATH, null);
            if (ctxPath != null) {
                File web_inf = new File(ctxPath, "WEB-INF");
                // Check for "<Context>/WEB-INF/webapp_<host>.conf"
                File webInfHostFile = new File(web_inf, cfgName + "_" + hostName + cfgExtn);
                if (webInfHostFile.isFile()) {
                    //Print.logInfo("Servlet - Found at: " + webInfHostFile);
                    return webInfHostFile.toURL();
                }
                // Check for "<Context>/WEB-INF/webapp.conf"
                File webInfFile = new File(web_inf, cfgFileName);
                if (webInfFile.isFile()) {
                    //Print.logInfo("Servlet - Found at: " + webInfFile);
                    return webInfFile.toURL();
                }
                Print.logWarn("Config file not found in Servlet: " + RTConfig.getServletClassName());
            } else {
                Print.logWarn("Servlet is missing 'CONTEXT_PATH': " + RTConfig.getServletClassName());
            }
            // continue
        } else {
            //Print.logWarn("Not a true Servlet ...");
            // continue
        }

        /* check for config file in resources */
        Class mainClass = RTConfig.getMainClass();
        if (mainClass != null) {
            // NOTE: Checking a ClassLoader's resources will check the CLASSPATH, which may also
            // check the current directory (directory from which this JVM was started).  This is
            // important to note if Tomcat happened be started with $GTS_HOME as the current
            // directory.
            //Print.logInfo("Searching Main class resoures ...");
            //Print.logInfo("CLASSPATH: " + System.getProperty("java.class.path","?"));
            try {
                ClassLoader mainClassLoader = mainClass.getClassLoader();
                if (mainClassLoader == null) {
                    // bootstrap classloader
                    mainClassLoader = ClassLoader.getSystemClassLoader(); // may still be null
                }
                if (mainClassLoader != null) {
                    // Check for resource "default_<host>.conf"
                    URL cfgHostRes = mainClassLoader.getResource(cfgName + "_" + hostName + cfgExtn);
                    if (cfgHostRes != null) {
                        //Print.logInfo("MainClass - Found at: " + cfgHostRes);
                        return cfgHostRes;
                    }
                    // Check for resource "default.conf"
                    URL cfgRes = mainClassLoader.getResource(cfgFileName);
                    if (cfgRes != null) {
                        //Print.logInfo("MainClass - Found at: " + cfgRes);
                        return cfgRes;
                    }
                } else {
                    Print.logWarn("System class loader is null");
                    // continue
                }
            } catch (Throwable t) {
                Print.logException("Error retrieving class loader", t);
                // continue
            }
        } else {
            Print.logInfo("Main class not found ...");
        }

        // Check for "<CONFIG_FILE_DIR>/[default|webapp]_<host>.conf"
        File cfgDirHostFile = new File(rtCfgDir, cfgName + "_" + hostName + cfgExtn);
        if (cfgDirHostFile.isFile()) {
            return cfgDirHostFile.toURL();
        }

        //Check for "<CONFIG_FILE_DIR>/[default|webapp].conf"
        File cfgDirFile = new File(rtCfgDir, rtCfgFile.toString());
        if (cfgDirFile.isFile()) {
            return cfgDirFile.toURL();
        }

        /* no config file */
        return null;

    }

    /**
    *** Returns true if the specified file exists as a resource to the current main JVM class
    *** @param file The file to check
    *** @return True if the file was found as a resource in the current main class
    **/
    /*
    protected static boolean _resourceExists(File file)
    {
        if (file == null) {
            return false;
        }
        Class mainClass = RTConfig.getMainClass();
        Print.logInfo("MainClass: " + mainClass);
        URL cfgRes = (mainClass != null)? mainClass.getClassLoader().getResource(file.toString()) : null;
        if (cfgRes != null) {
            Print.logInfo("ConfigFile found as resource: " + file);
            return true;
        } else {
            Print.logInfo("ConfigFile NOT found as resource: " + file);
            return false;
        }
    }
    */

    // ------------------------------------------------------------------------

    /**
    *** Reloads all runtime config properties
    **/
    public static synchronized void reload()
    {

        /* clear init flags */
        if (_didStartupInit == 0) {
            Print.logWarn("Calling 'reload' when initialization was never called in the first place");
        } else {
            Print.logInfo("Reloading RTConfig ...");
        }
        _didStartupInit = 0;

        /* reset */
        // note: this 'synchronized' method is calling '_startupInit', which is also synchronized.
        _startupInit(_allowSysPropChange);

    }

    /**
    *** Runtime config initialization
    *** @param allowChangeSystemProperties True to allow System properties update, false for read-only
    **/
    protected static synchronized void _startupInit(boolean allowChangeSystemProperties)
    {
        // Note: this method is synchronized

        /* check init */
        if (_didStartupInit == 2) {
            // already initialized
            return; 
        } else
        if (_didStartupInit == 1) {
            Print.logError("_startupInit' is already initializing!");
            return; 
        }
        _allowSysPropChange = allowChangeSystemProperties;
        _didStartupInit = 1;

        /* config file/URL */
        _foundConfigFile = null;
        _foundConfigURL = RTConfig.getConfigURL();

        /* RuntimeConstant defaults */
        RTProperties constantProps = RTConfig.getRuntimeConstantProperties();
        // main class
        if (!constantProps.hasProperty(RTKey.MAIN_CLASS)) {
            Class mainClass = RTConfig.getMainClass(); // may be null
            if (mainClass != null) {
                constantProps.setProperty(RTKey.MAIN_CLASS, mainClass); // sets tha actual class
                // Context name
                if (!constantProps.hasProperty(RTKey.CONTEXT_NAME)) {
                    // skip if already set by servlet context
                    String mainClassStr = StringTools.className(mainClass);
                    int p = mainClassStr.lastIndexOf(".");
                    String contextName = (p >= 0)? mainClassStr.substring(p+1) : mainClassStr;
                    constantProps.setProperty(RTKey.CONTEXT_NAME, contextName); 
                }
            }
        }
        // default context path
        if (!constantProps.hasProperty(RTKey.CONTEXT_PATH) &&
            (_foundConfigURL != null) && _foundConfigURL.getProtocol().equalsIgnoreCase("file")) {
            // skip if already set by servlet context
            String contextPath = null;
            File cfgFile = new File(_foundConfigURL.getPath());
            try {
                contextPath = cfgFile.getCanonicalFile().getParent();
            } catch (IOException ioe) {
                contextPath = cfgFile.getParent();
            }
            constantProps.setProperty(RTKey.CONTEXT_PATH, contextPath);
        }

        /* System properties */
        Properties propMap = null;
        if (_allowSysPropChange) {
            try {
                propMap = System.getProperties();
            } catch (SecurityException se) { // SecurityException, AccessControlException
                Print.sysPrintln("ERROR: Attempting to call 'System.getProperties()': " + se);
            }
        } else {
            propMap = new Properties();
            for (Iterator i = RTKey.getRuntimeKeyIterator(); i.hasNext();) {
                String key = (String)i.next();
                try {
                    String val = System.getProperty(key, null);
                    if (val != null) {
                        propMap.setProperty(key, val);
                    }
                } catch (SecurityException se) { // SecurityException, AccessControlException
                    Print.sysPrintln("Attempting to get System property '" + key + "': " + se);
                }
            }
        }
        CFG_PROPERTIES[SYSTEM_PROPS] = new RTProperties(propMap);

        /* environment variables */
        if (RTConfig.getEnvironmentVariablesEnabled()) {
            try {
                Map<String,String> envMap = System.getenv();
                //for (String k:envMap.keySet()) {String v = envMap.get(k);Print.logInfo("Env: "+k+"==>"+v);}
                CFG_PROPERTIES[ENVIRONMENT] = new RTProperties(envMap);
                //CFG_PROPERTIES[ENVIRONMENT].setAllowBlankValues(false);
            } catch (Throwable th) {
                // security error?
                CFG_PROPERTIES[ENVIRONMENT] = null;
            }
        }

        /* verbose? */
        if (hasProperty(RTKey.RT_VERBOSE, false)) {
            RTConfig.verbose = RTConfig.getBoolean(RTKey.RT_VERBOSE, false);
        }
        if (hasProperty(RTKey.RT_QUIET, false)) {
            RTConfig.quiet = RTConfig.getBoolean(RTKey.RT_QUIET, false);
            if (RTConfig.quiet) {
                RTConfig.verbose = false;
            }
        }

        /* load config file/URL */
        if (_foundConfigURL != null) {
            CFG_PROPERTIES[CONFIG_FILE] = new RTProperties(_foundConfigURL);
            if (RTConfig.verbose) { 
                Print.logInfo("Loaded config URL: " + _foundConfigURL); 
            }
        } else {
            //String cfgDir = RTConfig.getFile(RTKey.CONFIG_FILE_DIR);
            //String cfgFile = RTConfig.getFile(RTKey.CONFIG_FILE);
            CFG_PROPERTIES[CONFIG_FILE] = new RTProperties(); // must be non-null
            if (RTConfig.verbose) { 
                Print.logWarn("No config file was found"); 
            }
        }
        CFG_PROPERTIES[CONFIG_FILE].setKeyReplacementMode(RTProperties.KEY_REPLACEMENT_GLOBAL);

        /* initialize http proxy */
        // http.proxyHost
        // http.proxyPort
        // http.nonProxyHosts
        String proxyHost = RTConfig.getString(RTKey.HTTP_PROXY_HOST);
        int    proxyPort = RTConfig.getInt   (RTKey.HTTP_PROXY_PORT);
        if ((proxyHost != null) && (proxyPort > 1024)) {
            String port = String.valueOf(proxyPort);
            //Properties sysProp = System.getProperties();
            //sysProp.put("proxySet" , "true");           // <  jdk 1.3
            //sysProp.put("proxyHost", proxyHost);        // <  jdk 1.3
            //sysProp.put("proxyPort", port);             // <  jdk 1.3
            //sysProp.put("http.proxyHost", proxyHost);   // >= jdk 1.3
            //sysProp.put("http.proxyPort", port);        // >= jdk 1.3
            //sysProp.put("firewallSet", "true");         // MS JVM
            //sysProp.put("firewallHost", proxyHost);     // MS JVM
            //sysProp.put("firewallPort", port);          // MS JVM
            System.setProperty("proxySet" , "true");            // <  jdk 1.3
            System.setProperty("proxyHost", proxyHost);         // <  jdk 1.3
            System.setProperty("proxyPort", port);              // <  jdk 1.3
            System.setProperty("http.proxyHost", proxyHost);    // >= jdk 1.3
            System.setProperty("http.proxyPort", port);         // >= jdk 1.3
            System.setProperty("firewallSet", "true");          // MS JVM
            System.setProperty("firewallHost", proxyHost);      // MS JVM
            System.setProperty("firewallPort", port);           // MS JVM
        }

        /* URLConnection timeouts */
        // sun.net.client.defaultConnectTimeout
        // sun.net.client.defaultReadTimeout
        long urlConnectTimeout = RTConfig.getLong(RTKey.URL_CONNECT_TIMEOUT);
        if (urlConnectTimeout > 0) {
            String timeout = String.valueOf(urlConnectTimeout);
            //System.getProperties().put("sun.net.client.defaultConnectTimeout", timeout);
            System.setProperty("sun.net.client.defaultConnectTimeout", timeout);
        }
        long urlReadTimeout = RTConfig.getLong(RTKey.URL_READ_TIMEOUT);
        if (urlReadTimeout > 0) {
            String timeout = String.valueOf(urlReadTimeout);
            //System.getProperties().put("sun.net.client.defaultReadTimeout", timeout);
            System.setProperty("sun.net.client.defaultReadTimeout", timeout);
        }

        /* now initialized */
        _didStartupInit = 2;

        /* set all of the Print configuration */
        Print.resetVars();

    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the Servlet context path.  Returns null if the current context is not
    *** a Servlet, or if the context path has not been defined
    *** @return The servlet context path File instance
    **/
    public static File getServletContextPath()
    {
        if (RTConfig.isWebApp()) {
            RTProperties constantProps = RTConfig.getRuntimeConstantProperties();
            String ctxPath = constantProps.getString(RTKey.CONTEXT_PATH, null);
            if (ctxPath != null) {
                File ctxPathFile = new File(ctxPath);
                if (ctxPathFile.isDirectory()) {
                    return ctxPathFile;
                }
            } else {
                Print.logWarn("Servlet is missing 'CONTEXT_PATH': " + RTConfig.getServletClassName());
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------

    /*
    public static Properties loadResourceProperties(String name)
    {
        try {
            ClassLoader cl = ClassLoader.getSystemClassLoader();
            InputStream inpStream = cl.getResourceAsStream(name);
            if (inpStream != null) {
                Properties props = new Properties();
                props.load(inpStream);
                return props;
            } else {
                return null;
            }
        } catch (Throwable t) {
            Print.logException("Loading properties: " + name, t);
            return null;
        }
    }
    */

    // ------------------------------------------------------------------------

    /*
    public static Properties loadManifestProperties(Class clzz)
    {
        // NOTE: Experimental! This currently does not work!!! DO NOT USE
        String manifestResource = "/META-INF/MANIFEST.MF";
        try {
            ClassLoader cl = clzz.getClassLoader();
            InputStream input = cl.getResourceAsStream(manifestResource);
            if (input == null) {
                throw new FileNotFoundException("MANIFEST not found");
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(input));
            Properties props = new Properties();
            for (;;) {
                String line = br.readLine();
                if (line == null) { break; }
                int p = line.indexOf(':');
                if (p > 0) {
                    String key = line.substring(0,p).trim();
                    String val = line.substring(p+1).trim();
                    props.setProperty(key, val);
                }
            }
            return props;
        } catch (Throwable t) {
            Print.logException("Loading MANIFEST properties", t);
            return null;
        }
    }
    */

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* replace ${key} strings in the specified text with values from the runtime properties */
    // default key delimiters will be used
    public static String insertKeyValues(String text)   
    {
        return RTConfig._insertKeyValues(null, text, RTProperties.KEY_START_DELIMITER, RTProperties.KEY_END_DELIMITER, RTProperties.KEY_DFT_DELIMITER, null);
    }

    /* replace ${key} strings in the specified text with values from the runtime properties */
    // default key delimiters will be used
    public static String insertKeyValues(String text, Map<String,String> customMap)   
    {
        return RTConfig._insertKeyValues(null, text, RTProperties.KEY_START_DELIMITER, RTProperties.KEY_END_DELIMITER, RTProperties.KEY_DFT_DELIMITER, customMap);
    }

    /* replace ${key} strings in the specified text with values from the runtime properties */
    // specified key delimiters will be used
    public static String insertKeyValues(String text, String startDelim, String endDelim, String dftDelim)
    {
        return RTConfig._insertKeyValues(null, text, startDelim, endDelim, dftDelim, null);
    }

    public static String _insertKeyValues(Object mainKey, String text)   
    {
        return RTConfig._insertKeyValues(mainKey, text, RTProperties.KEY_START_DELIMITER, RTProperties.KEY_END_DELIMITER, RTProperties.KEY_DFT_DELIMITER, null);
    }
    
    private static String _insertKeyValues(final Object mainKey, final String text, String startDelim, String endDelim, final String dftDelim, final Map<String,String> customMap)   
    {
        if (text != null) {
            // replacment call-back 
            StringTools.ReplacementMap rm = new StringTools.ReplacementMap() {
                private Set<Object> thisKeySet = new HashSet<Object>();
                private Set<Object> fullKeySet = new HashSet<Object>();
                public String get(String k) {
                    // reset?
                    if (k == null) {
                        // a bit of a hack here to tell this map to reset the cached keys
                        fullKeySet.addAll(thisKeySet); // prevent the same key from being processed in a recursive pass
                        if (mainKey != null) { fullKeySet.add(mainKey); }
                        thisKeySet.clear();
                        return null;
                    }
                    // parse key/default
                    String key = null;
                    String dft = null;
                    int dftNdx = StringTools.isBlank(dftDelim)? -1 : k.indexOf(dftDelim); // k.lastIndexOf(dftDelim); <-- last index?
                    if (dftNdx >= 0) {
                        dft = k.substring(dftNdx + dftDelim.length()); // leave default as-is (untrimmed)
                        key = k.substring(0,dftNdx).trim();  // trim key
                    } else {
                        key = k.trim();  // trim key
                    }
                    // return value
                    if (fullKeySet.contains(key)) {
                        return null; // recursive value
                    } else {
                        thisKeySet.add(key);
                        String customVal = (customMap != null)? customMap.get(key) : null;
                        String rtnVal = (customVal != null)? customVal : RTConfig._getString(key, dft, true);
                        //if (rtnVal == null) {Print.logError("Key not found: '" + key + "' [" + text + "]");}
                        return rtnVal;
                    }
                }
            };
            // iterate until the string doesn't change
            String s_old = text;
            for (int i = 0; i < RTProperties.KEY_MAX_RECURSION; i++) {
                rm.get(null); // hack to reset the cached keys
                String s_new = StringTools.insertKeyValues(s_old, startDelim, endDelim, rm, false);
                if (s_new.equals(s_old)) {
                    return s_new;
                }
                s_old = s_new;
            }
            return s_old;
        } else {
            return text;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static boolean hasProperty(String key)
    {
        return RTConfig.hasProperty(key, true);
    }

    public static boolean hasProperty(String key, boolean dftOk)
    {
        RTProperties rtp = getPropertiesForKey(key, dftOk);
        return (rtp != null);
    }

    public static boolean hasProperty(String key[])
    {
        return RTConfig.hasProperty(key, true);
    }

    public static boolean hasProperty(String key[], boolean dftOk)
    {
        if (key != null) {
            for (int i = 0; i < key.length; i++) {
                RTProperties rtp = getPropertiesForKey(key[i], dftOk);
                if (rtp != null) { return true; }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------

    public static Object getProperty(String key)
    {
        return RTConfig.getProperty(key, null, true);
    }

    public static Object getProperty(String key, Object dft)
    {
        return RTConfig.getProperty(key, dft, false);
    }

    public static Object getProperty(String key, Object dft, boolean dftOk)
    {
        RTProperties rtp = getPropertiesForKey(key, dftOk);
        return (rtp != null)? rtp.getProperty(key, dft) : dft;
    }

    public static void setProperty(String key, Object value)
    {
        RTProperties cfgProps = getConfigFileProperties();
        cfgProps.setProperty(key, value);
        if ((key != null) && (value == null)) {
            getSystemProperties().removeProperty(key);
        }
    }

    public static void setProperties(Properties props)
    {
        RTProperties cfgProps = getConfigFileProperties();
        cfgProps.setProperties(props);
    }

    // ------------------------------------------------------------------------

    // Extract a Map containing a group of key/values from the config file properties
    /*
    public static Map<String,String> extractMap(String keyEnd, String valEnd)
    {
        // TODO: should include keyEnd/valEnd from all properties
        RTProperties cfgProps = getConfigFileProperties();
        return cfgProps.extractMap(keyEnd, valEnd);
    }
    */

    // ------------------------------------------------------------------------

    public static String _getString(String key, String dft, boolean dftOK)
    {
        RTProperties rtp = getPropertiesForKey(key, dftOK);
        if (rtp != null) {
            Object obj = rtp._getProperty(key, dft);
            return (obj != null)? obj.toString() : dft;
        } else {
            return dft;
        }
    }

    public static String _getString(String key[], String dft, boolean dftOK)
    {
        if (key != null) {
            for (int i = 0; i < key.length; i++) {
                RTProperties rtp = getPropertiesForKey(key[i], dftOK);
                if (rtp != null) { 
                    Object obj = rtp._getProperty(key[i], dft);
                    return (obj != null)? obj.toString() : dft;
                }
            }
        }
        return dft;
    }

    public static String getString(String key)
    {
        return getString(key, null, true);
    }

    public static String getString(String key[])
    {
        return getString(key, null, true);
    }

    public static String getString(String key, String dft)
    {
        return getString(key, dft, false);
    }

    public static String getString(String key, String dft, boolean dftOK)
    {
        RTProperties rtp = getPropertiesForKey(key, dftOK);
        return (rtp != null)? rtp.getString(key, dft) : dft;
    }

    public static String getString(String key[], String dft)
    {
        return getString(key, dft, false);
    }

    public static String getString(String key[], String dft, boolean dftOK)
    {
        if (key != null) {
            for (int i = 0; i < key.length; i++) {
                RTProperties rtp = getPropertiesForKey(key[i], dftOK);
                if (rtp != null) { 
                    return rtp.getString(key[i], dft);
                }
            }
        }
        return dft;
    }

    public static void setString(String key, String value)
    {
        RTProperties cfgFileProps = getConfigFileProperties();
        cfgFileProps.setString(key, value);
    }

    // ------------------------------------------------------------------------

    public static String[] getStringArray(String key)
    {
        return getStringArray(key, null, true);
    }

    public static String[] getStringArray(String key, String dft[])
    {
        return getStringArray(key, dft, false);
    }

    public static String[] getStringArray(String key, String dft[], boolean dftOK)
    {
        RTProperties rtp = getPropertiesForKey(key, dftOK);
        return (rtp != null)? rtp.getStringArray(key, dft) : dft;
    }

    public static String[] getStringArray(String key[], String dft[])
    {
        return getStringArray(key, dft, false);
    }
    
    public static String[] getStringArray(String key[], String dft[], boolean dftOK)
    {
        if (key != null) {
            for (int i = 0; i < key.length; i++) {
                RTProperties rtp = getPropertiesForKey(key[i], dftOK);
                if (rtp != null) { 
                    return rtp.getStringArray(key[i], dft); 
                }
            }
        }
        return dft;
    }

    public static void setStringArray(String key, String val[])
    {
        RTProperties cfgProps = getConfigFileProperties();
        cfgProps.setStringArray(key, val);
    }

    // ------------------------------------------------------------------------

    public static File getFile(String key)
    {
        return getFile(key, null, true);
    }

    // do not include this method, otherwise "getFile(file, null)" would be ambiguous
    //public File getFile(String key, String dft)

    public static File getFile(String key, File dft)
    {
        return getFile(key, dft, false);
    }

    public static File getFile(String key, File dft, boolean dftOK)
    {
        RTProperties rtp = getPropertiesForKey(key, dftOK);
        return (rtp != null)? rtp.getFile(key, dft) : dft;
    }

    public static File getFile(String key[], File dft)
    {
        return getFile(key, dft, false);
    }
    
    public static File getFile(String key[], File dft, boolean dftOK)
    {
        if (key != null) {
            for (int i = 0; i < key.length; i++) {
                RTProperties rtp = getPropertiesForKey(key[i], dftOK);
                if (rtp != null) { return rtp.getFile(key[i], dft); }
            }
        }
        return dft;
    }

    public static void setFile(String key, File value)
    {
        RTProperties cfgProps = getConfigFileProperties();
        cfgProps.setFile(key, value);
    }

    // ------------------------------------------------------------------------

    public static double getDouble(String key)
    {
        return getDouble(key, 0.0, true);
    }

    public static double getDouble(String key, double dft)
    {
        return getDouble(key, dft, false);
    }

    public static double getDouble(String key, double dft, boolean dftOK)
    {
        RTProperties rtp = getPropertiesForKey(key, dftOK);
        return (rtp != null)? rtp.getDouble(key, dft) : dft;
    }

    public static double getDouble(String key[], double dft)
    {
        return getDouble(key, dft, false);
    }
    
    public static double getDouble(String key[], double dft, boolean dftOK)
    {
        if (key != null) {
            for (int i = 0; i < key.length; i++) {
                RTProperties rtp = getPropertiesForKey(key[i], dftOK);
                if (rtp != null) { return rtp.getDouble(key[i], dft); }
            }
        }
        return dft;
    }

    public static void setDouble(String key, double value)
    {
        RTProperties cfgProps = getConfigFileProperties();
        cfgProps.setDouble(key, value);
    }

    // ------------------------------------------------------------------------

    public static float getFloat(String key)
    {
        return RTConfig.getFloat(key, 0.0F, true);
    }

    public static float getFloat(String key, float dft)
    {
        return RTConfig.getFloat(key, dft, false);
    }

    public static float getFloat(String key, float dft, boolean dftOK)
    {
        RTProperties rtp = getPropertiesForKey(key, dftOK);
        return (rtp != null)? rtp.getFloat(key, dft) : dft;
    }

    public static float getFloat(String key[], float dft)
    {
        return RTConfig.getFloat(key, dft, false);
    }
    
    public static float getFloat(String key[], float dft, boolean dftOK)
    {
        if (key != null) {
            for (int i = 0; i < key.length; i++) {
                RTProperties rtp = getPropertiesForKey(key[i], dftOK);
                if (rtp != null) { return rtp.getFloat(key[i], dft); }
            }
        }
        return dft;
    }

    public static void setFloat(String key, float value)
    {
        RTProperties cfgProps = getConfigFileProperties();
        cfgProps.setFloat(key, value);
    }

    // ------------------------------------------------------------------------

    public static long getLong(String key)
    {
        return RTConfig.getLong(key, 0L, true);
    }

    public static long getLong(String key, long dft)
    {
        return RTConfig.getLong(key, dft, false);
    }
    
    public static long getLong(String key, long dft, boolean dftOK)
    {
        RTProperties rtp = getPropertiesForKey(key, dftOK);
        return (rtp != null)? rtp.getLong(key, dft) : dft;
    }

    public static long getLong(String key[], long dft)
    {
        return RTConfig.getLong(key, dft, false);
    }

    public static long getLong(String key[], long dft, boolean dftOK)
    {
        if (key != null) {
            for (int i = 0; i < key.length; i++) {
                RTProperties rtp = getPropertiesForKey(key[i], dftOK);
                if (rtp != null) { return rtp.getLong(key[i], dft); }
            }
        }
        return dft;
    }

    public static void setLong(String key, long value)
    {
        RTProperties cfgProps = getConfigFileProperties();
        cfgProps.setLong(key, value);
    }

    // ------------------------------------------------------------------------

    public static int getInt(String key)
    {
        return RTConfig.getInt(key, 0, true);
    }

    public static int getInt(String key, int dft)
    {
        return RTConfig.getInt(key, dft, false);
    }

    public static int getInt(String key, int dft, boolean dftOK)
    {
        RTProperties rtp = getPropertiesForKey(key, dftOK);
        return (rtp != null)? rtp.getInt(key, dft) : dft;
    }

    public static int getInt(String key[], int dft)
    {
        return RTConfig.getInt(key, dft, false);
    }

    public static int getInt(String key[], int dft, boolean dftOK)
    {
        if (key != null) {
            for (int i = 0; i < key.length; i++) {
                RTProperties rtp = getPropertiesForKey(key[i], dftOK);
                if (rtp != null) { return rtp.getInt(key[i], dft); }
            }
        }
        return dft;
    }

    public static void setInt(String key, int value)
    {
        RTProperties cfgProps = getConfigFileProperties();
        cfgProps.setInt(key, value);
    }

    // ------------------------------------------------------------------------

    public static boolean getBoolean(String key)
    {
        return getBoolean(key, hasProperty(key), true);
    }

    public static boolean getBoolean(String key, boolean dft)
    {
        return getBoolean(key, dft, false);
    }

    public static boolean getBoolean(String key, boolean dft, boolean dftOK)
    {
        RTProperties rtp = getPropertiesForKey(key, dftOK);
        if (rtp == null) {
            return dft; // no key, return default
        } else {
            String s = rtp.getString(key, "");
            if ((s != null) && s.equals("")) {
                return rtp.getBoolean(key, true); // key with no argument
            } else {
                return rtp.getBoolean(key, dft);  // key with argument, use dft if not parsable.
            }
        }
        //return (rtp != null)? rtp.getBoolean(key, dft) : dft;
    }

    public static boolean getBoolean(String key[], boolean dft)
    {
        return getBoolean(key, dft, false);
    }

    public static boolean getBoolean(String key[], boolean dft, boolean dftOK)
    {
        if (key != null) {
            for (int i = 0; i < key.length; i++) {
                RTProperties rtp = getPropertiesForKey(key[i], dftOK);
                if (rtp != null) { return rtp.getBoolean(key[i], dft); }
            }
        }
        return dft;
    }

    public static void setBoolean(String key, boolean value)
    {
        RTProperties cfgProps = getConfigFileProperties();
        cfgProps.setBoolean(key, value);
    }

    // ------------------------------------------------------------------------

    public static void setAdminMode(boolean admin)
    {
        RTConfig.setBoolean(RTKey.ADMIN_MODE, admin);
    }

    public static boolean isAdminMode()
    {
        return RTConfig.getBoolean(RTKey.ADMIN_MODE);
    }

    // ------------------------------------------------------------------------

    public static void setDebugMode(boolean debug)
    {
        RTConfig.setBoolean(RTKey.DEBUG_MODE, debug);
    }

    //private static int _debug_recursion = 0;
    public static boolean isDebugMode()
    {
        //if (_debug_recursion > 0) { Thread.dumpStack(); System.exit(0); }
        //try { _debug_recursion++;
        return !isInitialized() || getBoolean(RTKey.DEBUG_MODE);
        //} finally { _debug_recursion--; }
    }

    // ------------------------------------------------------------------------

    public static void setTestMode(boolean test)
    {
        setBoolean(RTKey.TEST_MODE, test);
    }

    public static boolean isTestMode()
    {
        return getBoolean(RTKey.TEST_MODE);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static Boolean isRunningAsWebApp = null;

    public static void setWebApp(boolean webapp)
    {
        RTProperties constantProps = RTConfig.getRuntimeConstantProperties();
        constantProps.setProperty(RTKey.IS_WEBAPP, webapp);
        isRunningAsWebApp = null; // <== to bypass Boolean check
    }

    public static boolean isWebApp()
    {

        /* already know where we are running? */
        if (isRunningAsWebApp != null) {
            return isRunningAsWebApp.booleanValue();
        }

        /* "isWebApp" explicitly defined? */
        RTProperties constantProps = RTConfig.getRuntimeConstantProperties();
        if (constantProps.hasProperty(RTKey.IS_WEBAPP)) {
            return constantProps.getBoolean(RTKey.IS_WEBAPP);
        }

        /* check invocation stack */
        isRunningAsWebApp = new Boolean(_isWebApp_2());
        return isRunningAsWebApp.booleanValue();

    }

    private static String WebAppClassNames[] = {
        "javax.servlet.http.HttpServlet", // as long as the servlet didn't override 'service'
        "org.apache.catalina.core.ApplicationFilterChain"
    };
    protected static boolean _isWebApp_1()
    {
        // We should also check the invocation stack
        // A typical stack-trace segment for a servlet is as follows:
        //   ...
        //   at com.example.war.DataMessage.doPost(DataMessage.java:46)
        //   at javax.servlet.http.HttpServlet.service(HttpServlet.java:760)
        //   at javax.servlet.http.HttpServlet.service(HttpServlet.java:853)
        //   at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:247)
        //   at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:193)
        //   at org.apache.catalina.core.StandardWrapperValve.invoke(StandardWrapperValve.java:256)
        //   ...
        // Possible search Strings would be:
        //  - "javax.servlet.http.HttpServlet" (assuming 'service' was not overridden)
        //  - "org.apache.catalina.core.ApplicationFilterChain" (only valid for Tomcat)
        Throwable t = new Throwable();
        t.fillInStackTrace();
        //t.printStackTrace();
        StackTraceElement stackFrame[] = t.getStackTrace();
        for (int i = 0; i < stackFrame.length; i++) {
            String cn = stackFrame[i].getClassName();
            for (int w = 0; w < WebAppClassNames.length; w++) {
                if (cn.equalsIgnoreCase(WebAppClassNames[w])) {
                    return true;
                }
            }
        }
        return false;
    }

    protected static boolean _isWebApp_2()
    {
        return (RTConfig.getServletClass() != null);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static boolean  Main_init = false;
    private static Class<?> Main_class = null;

    /**
    *** Gets the main entry point class
    *** @return The main entry point class, or null if the main entry point class
    ***         could not be obtained.
    **/
    public static Class<?> getMainClass()
    {
        if (!Main_init) {
            Main_init = true;
            if (OSTools.hasGetCallerClass()) {
                Class lastClz = null;
                for (int sf = 2; ; sf++) {
                    Class clz = OSTools.getCallerClass(sf);
                    if (clz == null) { break; }
                    lastClz = clz;
                }
                Main_class = lastClz;
            } else {
                Print.logError("Not running with the Sun Microsystems version of Java.");
                Print.logError("Cannot obtain main class.");
            }
        }
        return Main_class;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static String SERVLET_CLASS = "javax.servlet.Servlet"; // GenericServlet
    private static boolean  Servlet_init = false;
    private static Class<?> Servlet_class = null;
    public static Class<?> getServletClass()
    {

        /* init for Servlet class */
        if (!Servlet_init) {
            try {
                Servlet_class = Class.forName(SERVLET_CLASS);
            } catch (Throwable t) {
                // class not found?
                //Print.logWarn("Not a servlet - running as application?");
            }
            Servlet_init = true;
        }

        /* find Servlet in invocation stack */
        if (Servlet_class != null) {
            for (int sf = 2; ; sf++) {
                Class<?> clz = OSTools.getCallerClass(sf);
                if (clz == null) { break; }
                if (Servlet_class.isAssignableFrom(clz)) {
                    return clz;
                }
            }
        }

        /* not found */
        return null;

    }

    /**
    *** Returns the Servlet class name
    *** @return The Servlet class name
    **/
    public static String getServletClassName()
    {
        Class sc = RTConfig.getServletClass();
        return (sc != null)? StringTools.className(sc) : "";
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Command-line entry point (testing/debugging purposes)
    *** @param argv  The command-line arguments
    **/
    public static void main(String argv[])
    {
        //Print.logInfo("Before RTConfig was initialized ...");
        RTConfig.setCommandLineArgs(argv);
        
        /* reload test */
        Print.logInfo("Starting Reload ...");
        RTConfig.reload();
        Print.logInfo("Done Reloading ...");

        /* Thread local temporary properties test */
        /* * /
        final String SETLABEL = "label";
        final String GETLABEL = "userAgent";
        RTProperties tempProps = new RTProperties();
        tempProps.setString(SETLABEL,"Main Hello World");
        Print.logInfo("Main Label = " + RTConfig.getString(GETLABEL));
        RTConfig.pushTemporaryProperties(tempProps);
        Print.logInfo("Main Label = " + RTConfig.getString(GETLABEL));
        (new Thread() {
            public void run() {
                RTProperties tp = new RTProperties();
                tp.setString(SETLABEL,"Thread Universal");
                RTConfig.pushTemporaryProperties(tp);
                Print.logInfo("Thread Label = " + RTConfig.getString(GETLABEL));
                for (int i = 0; i < 10; i++) {
                    Print.logInfo("Thread Label = " + RTConfig.getString(GETLABEL));
                    try { Thread.sleep(900L); } catch (Throwable th) {}
                }
                Print.logInfo("Thread done ...");
            }
        }).start();
        for (int i = 0; i < 10; i++) {
            Print.logInfo("Main Label = " + RTConfig.getString(GETLABEL));
            try { Thread.sleep(1000L); } catch (Throwable th) {}
        }
        RTConfig.popTemporaryProperties(tempProps);
        Print.logInfo("Main Label = " + RTConfig.getString(GETLABEL));
        /* */

        //Print.logInfo("String test = " + RTConfig.insertKeyValues(RTConfig.getString("test","???")));
        //Print.logInfo("Double test = " + RTConfig.getDouble("test",0.0));
        //Print.logInfo("Long   test = " + RTConfig.getLong("test",0L));
        
        //Print.logFatal("DebugMode [false] = " + RTConfig.getBoolean(RTKey.DEBUG_MODE,false));
        //Print.logError("DebugMode [false] = " + RTConfig.getBoolean(RTKey.DEBUG_MODE,false));
        //Print.logWarn ("DebugMode [true ] = " + RTConfig.getBoolean(RTKey.DEBUG_MODE,true));
        //Print.logInfo ("DebugMode [undef] = " + RTConfig.getBoolean(RTKey.DEBUG_MODE));
        //Print.logDebug("DebugMode [undef] = " + RTConfig.getBoolean(RTKey.DEBUG_MODE));
        
    }

}
