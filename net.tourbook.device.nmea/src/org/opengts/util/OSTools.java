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
//  General OS specific tools
// ----------------------------------------------------------------------------
// Change History:
//  2006/03/26  Martin D. Flynn
//     -Initial release
//  2006/06/30  Martin D. Flynn
//     -Repackaged
//  2008/06/20  Martin D. Flynn
//     -Added method 'getProcessID()'
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class OSTools
{

    // ------------------------------------------------------------------------
    // OS and JVM specific tools
    // ------------------------------------------------------------------------

    private static int OS_INITIALIZE = -1;
    public  static int OS_UNKNOWN    = 0x1000;
    public  static int OS_UNIX       = 0x0010;
    public  static int OS_MAC        = 0x0020;
    public  static int OS_WINDOWS    = 0x0040;
    public  static int OS_WINDOWS_XP = OS_WINDOWS | 0x0001;
    public  static int OS_WINDOWS_9X = OS_WINDOWS | 0x0002;

    private static int OSType = OS_INITIALIZE;

    public static final String PROPERTY_JAVA_HOME                   = "java.home";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static final String PROPERTY_JAVA_VENDOR                 = "java.vendor";

    public static final String PROPERTY_JAVA_SPECIFICATION_VERSION  = "java.specification.version";

    /* this does not work on Windows (and seems to return the wrong parent PID on Linux) */
    private static int _getProcessID()
    {
        try {
            final String cmd[] = new String[] { "bash", "-c", "echo $PPID" };
            final Process ppidExec = Runtime.getRuntime().exec(cmd);
            final BufferedReader ppidReader = new BufferedReader(new InputStreamReader(ppidExec.getInputStream()));
            final StringBuffer sb = new StringBuffer();
            for (;;) {
                final String line = ppidReader.readLine();
                if (line == null) { break; }
                sb.append(StringTools.trim(line));
            }
            final int pid = StringTools.parseInt(sb.toString(),-1);
            final int exitVal = ppidExec.waitFor();
            Print.logInfo("Exit value: %d [%s]", exitVal, sb.toString());
            ppidReader.close();
            return pid;
        } catch (final Throwable th) {
            Print.logException("Unable to obtain PID", th);
            return -1;
        }
    }

    /**
    *** Gets the class of the caller at the specified frame index
    *** @param frame The frame index
    *** @return The calling class
    **/
    public static Class getCallerClass(final int frame)
    {
        try {
            // sun.reflect.Reflection.getCallerClass(0) == sun.reflect.Reflection
            // sun.reflect.Reflection.getCallerClass(1) == OSTools
//            Class clz = sun.reflect.Reflection.getCallerClass(frame + 1);
            //Print._println("" + (frame + 1) + "] class " + StringTools.className(clz));
//            return clz;
        } catch (final Throwable th) { // ClassNotFoundException
            // This can occur when the code has been compiled with the Sun Microsystems version
            // of Java, but is executed with the GNU version of Java (or other non-Sun version).
            Print.logException("Sun Microsystems version of Java is not in use", th);
            return null;
        }
        return null;
    }

    /**
    *** Returns the known OS type as an integer bitmask
    *** @return The OS type
    **/
    public static int getOSType()
    {
        if (OSType == OS_INITIALIZE) {
            final String osName = System.getProperty("os.name").toLowerCase();
            //Print.logInfo("OS: " + osName);
            if (osName.startsWith("windows")) {
                if (osName.startsWith("windows xp")) {
                    OSType = OS_WINDOWS_XP;
                } else
                if (osName.startsWith("windows 9") || osName.startsWith("windows m")) {
                    OSType = OS_WINDOWS_9X;
                } else {
                    OSType = OS_WINDOWS;
                }
            } else
            if (File.separatorChar == '/') {
                OSType = OS_UNIX;
            } else {
                OSType = OS_UNKNOWN;
            }
        }
        return OSType;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the Process-ID of this JVM invocation.<br>
    *** IMPORTANT: This implementation relies on a "convention", rather that a documented method
    *** of obtaining the process-id of this JVM within the OS.  <b>Caveat Emptor!</b><br>
    *** (On Windows, this returns the 'WINPID' which is probably useless anyway)
    *** @return The Process-ID
    **/
    public static int getProcessID()
    {
        // References:
        //  - http://blog.igorminar.com/2007/03/how-java-application-can-discover-its.html
        if (OSTools.isSunJava()) {
            try {
                // by convention, returns "<PID>@<host>" (until something changes, and it doesn't)
                final String n = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
                final int pid = StringTools.parseInt(n,-1); // parse PID
                return pid;
            } catch (final Throwable th) {
                Print.logException("Unable to obtain Process ID", th);
                return -1;
            }
        } else {
            return -1;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if 'sun.reflect.Reflection' is present in the runtime libraries.<br>
    *** (will return true when running with the Sun Microsystems version of Java)
    *** @return True if 'getCallerClass' is available.
    **/
    public static boolean hasGetCallerClass()
    {
        try {
//            sun.reflect.Reflection.getCallerClass(0);
//            return true;
        } catch (final Throwable th) {
            return false;
        }
        return false;
    }
    /**
    *** Returns true if this implementation has a broken 'toFront' Swing implementation.<br>
    *** (may only be applicable on Java v1.4.2)
    *** @return True if this implementation has a broken 'toFront' Swing implementation.
    **/
    public static boolean isBrokenToFront()
    {
        return isWindows();
    }
    /**
    *** Returns true if executed from a Sun Microsystems JVM.
    *** @return True is executed from a Sun Microsystems JVM.
    **/
    public static boolean isSunJava()
    {
        final String propVal = System.getProperty(PROPERTY_JAVA_VENDOR); // "Sun Microsystems Inc."
        if ((propVal == null) || (propVal.indexOf("Sun Microsystems") < 0)) {
            return false;
        } else {
            return true;
        }
    }
    
    /**
    *** Returns true if the OS is Unix/Linux
    *** @return True if the OS is Unix/Linux
    **/
    public static boolean isUnix()
    {
        return ((getOSType() & OS_UNIX) != 0);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the OS is unknown
    *** @return True if the OS is unknown
    **/
    public static boolean isUnknown()
    {
        return (getOSType() == OS_UNKNOWN);
    }

    /**
    *** Returns true if the OS is a flavor of Windows
    *** @return True if the OS is a flavor of Windows
    **/
    public static boolean isWindows()
    {
        return ((getOSType() & OS_WINDOWS) != 0);
    }

    /**
    *** Returns true if the OS is Windows 95/98
    *** @return True if the OS is Windows 95/98
    **/
    public static boolean isWindows9X()
    {
        return (getOSType() == OS_WINDOWS_9X);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the OS is Windows XP
    *** @return True if the OS is Windows XP
    **/
    public static boolean isWindowsXP()
    {
        return (getOSType() == OS_WINDOWS_XP);
    }

    /**
    *** Command-line entry point (debug/testing)
    *** @param argv  The command-line arguments
    **/
    public static void main(final String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        Print.logInfo("Is Windows  : " + isWindows());
        Print.logInfo("Is Windows9X: " + isWindows9X());
        Print.logInfo("Is WindowsXP: " + isWindowsXP());
        Print.logInfo("Is Unix     : " + isUnix());
        Print.logInfo("PID #1      : " + getProcessID());
        Print.logInfo("PID #2      : " + _getProcessID());
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Prints the class of the caller (debug purposes only)
    **/
    public static void printCallerClasses()
    {
//        try {
//            for (int i = 0; ; i++) {
//                Class clz = sun.reflect.Reflection.getCallerClass(i);
//                Print.logInfo("" + i + "] class " + StringTools.className(clz));
//                if (clz == null) { break; }
//            }
//        } catch (Throwable th) { // ClassNotFoundException
//            // This can occur when the code has been compiled with the Sun Microsystems version
//            // of Java, but is executed with the GNU version of Java.
//            Print.logException("Sun Microsystems version of Java is not in use", th);
//        }
    }

}
