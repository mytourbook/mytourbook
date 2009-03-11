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
//  General Printing/Logging utilities.
// ----------------------------------------------------------------------------
// Change History:
//  2006/03/26  Martin D. Flynn
//     -Initial release
//  2006/04/23  Martin D. Flynn
//     -Updated to support a more granular message logging.  Eventually, this
//      should be modified to support Log4J.
//  2006/06/30  Martin D. Flynn
//     -Repackaged
//  2007/03/13  Martin D. Flynn
//     -Optimized calls to 'getLogLevel' and 'getLogHeaderLevel'
//  2007/09/16  Martin D. Flynn
//     -Moved all runtime configuration initialization to 'initRTConfig()'
//  2008/03/28  Martin D. Flynn
//     -Wrapped System.[out|err] in a PrintWriter that understands UTF-8 encoding.
//  2008/05/20  Martin D. Flynn
//     -Updated to use Varargs in print arguments.
//  2009/01/28  Martin D. Flynn
//     -Renamed 'initRTConfig()' to 'resetVars()', and changed vars to lazy init.
//  2009/02/20  Martin D. Flynn
//     -Added aged rotated file deletion
//     -Renamed log file rotation properties (check RTKey.java for latest names)
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.text.*;

/**
*** Performs message logging
**/

public class Print
{

    // ------------------------------------------------------------------------

    public  static final int    LOG_UNDEFINED           = -1;
    public  static final int    LOG_OFF                 = 0;
    public  static final int    LOG_FATAL               = 1;
    public  static final int    LOG_ERROR               = 2;
    public  static final int    LOG_WARN                = 3;
    public  static final int    LOG_INFO                = 4;
    public  static final int    LOG_DEBUG               = 5;
    public  static final int    LOG_ALL                 = 6;

    // ------------------------------------------------------------------------

    private static final String _JAVA                   = ".java";
    
    // ------------------------------------------------------------------------

    private static PrintStream  stdout                  = null;
    private static PrintStream  stderr                  = null;
    private static PrintStream  sysStdout               = null;
    private static PrintStream  sysStderr               = null;

    // ------------------------------------------------------------------------

    private static int          printLogLevel           = LOG_UNDEFINED;
    private static int          printLogHeaderLevel     = LOG_UNDEFINED;

    private static int          printLogIncludeFrame    = -1;       // lazy init
    private static int          printLogIncludeDate     = -1;       // lazy init

    private static boolean      printLogFile_init       = false;    // volatile?
    private static File         printLogFile            = null;
    
    private static long         printRotateLogFileSize  = -1L;      // lazy init
    private static long         printRotateDelAgeSec    = -1L;      // lazy init

    /**
    *** Print runtime configuration (called by RTConfig)
    **/
    public static void resetVars()
    {
        printLogLevel           = LOG_UNDEFINED;
        printLogHeaderLevel     = LOG_UNDEFINED;
        printLogIncludeFrame    = -1;
        printLogIncludeDate     = -1;
        printLogFile_init       = false;
        printLogFile            = null;
        printRotateLogFileSize  = -1L;
        printRotateDelAgeSec    = -1L;
    }

    /**
    *** Returns true if the stack frame is to be included on log messages 
    *** @return True if the stack frame is to be included on log messages 
    **/
    protected static boolean _includeStackFrame()
    {
        if (printLogIncludeFrame < 0) {
            printLogIncludeFrame = (RTConfig.getBoolean(RTKey.LOG_INCL_STACKFRAME,false) || Print.isDebugLoggingLevel())? 1 : 0;
        }
        return (printLogIncludeFrame > 0);
    }

    /**
    *** Returns true if the date/time is to be included on log messages 
    *** @return True if the date/time is to be included on log messages 
    **/
    protected static boolean _includeDate()
    {
        if (printLogIncludeDate < 0) {
            printLogIncludeDate = RTConfig.getBoolean(RTKey.LOG_INCL_DATE,false)? 1 : 0;
        }
        return (printLogIncludeDate > 0);
    }
    
    /**
    *** Returns true if exceptions should be emailed to the recipient on file
    *** @return True if exceptions should be emailed
    **/
    protected static boolean _emailExceptions()
    {
        return RTConfig.getBoolean(RTKey.LOG_EMAIL_EXCEPTIONS,false);
    }

    // ------------------------------------------------------------------------

    private static String localhostName = null;

    /**
    *** Gets the host name for this systenm
    *** @return The host name for this system
    **/
    public static String getHostName()
    {
        /* host name */
        if (Print.localhostName == null) {
            try {
                String hd = InetAddress.getLocalHost().getHostName();
                int p = hd.indexOf(".");
                Print.localhostName = (p >= 0)? hd.substring(0,p) : hd;
            } catch (UnknownHostException uhe) {
                Print.localhostName = "UNKNOWN";
            }
        }
        return Print.localhostName;
    }

    // ------------------------------------------------------------------------

    /**
    *** Formats the current data/time per the specified format specification
    *** @param fmt  The date/time format specification
    *** @return The formatted data/time
    **/
    public static String formatDate(String fmt)
    {
        return (new DateTime()).format(fmt,null);
        //java.util.Date nowDate = new java.util.Date(System.currentTimeMillis());
        //SimpleDateFormat sdf = new SimpleDateFormat(fmt);
        //StringBuffer sb = new StringBuffer();
        //sdf.format(nowDate, sb, new FieldPosition(0));
        //return sb.toString();
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the PrintStream used when printing to 'stdout'
    *** @param out  The 'stdout' PrintStream
    **/
    public static void setStdout(PrintStream out)
    {
        Print.stdout = out;
    }

    /**
    *** Sets the PrintStream used when printing to 'stderr'
    *** @param err  The 'stderr' PrintStream
    **/
    public static void setStderr(PrintStream err)
    {
        Print.stderr = err;
    }

    /**
    *** Gets the PrintStream used when printing to 'stdout'
    *** @return  The 'stdout' PrintStream
    **/
    public static PrintStream getStdout()
    {
        return (Print.stdout != null)? Print.stdout : Print.getSystemOut();
    }

    /**
    *** Gets the PrintStream used when printing to 'stderr'
    *** @return  The 'stderr' PrintStream
    **/
    public static PrintStream getStderr()
    {
        return (Print.stderr != null)? Print.stderr : Print.getSystemErr();
    }

    // ------------------------------------------------------------------------
    
    private static String printEncoding = null;
    private static PrintStream sysOut = null;
    private static PrintStream sysErr = null;
    
    /**
    *** Sets the character encoding for the Printed text output
    *** @param enc  The character encoding
    **/
    public static void setEncoding(String enc)
    {
        Print.printEncoding = ((enc != null) && !enc.equals(""))? enc : null;
        Print.sysOut = null;
        Print.sysErr = null;
    }

    /**
    *** Gets the character encoding for the Printed text output
    *** @return  The character encoding
    **/
    public static String getEncoding()
    {
        return (Print.printEncoding != null)? Print.printEncoding : StringTools.getCharacterEncoding();
    }

    /**
    *** Gets the System 'stdout' PrintStream with the previously specified character encoding
    *** @return The systenm 'stdout' PrintStream
    **/
    public static PrintStream getSystemOut()
    {
        if (Print.sysOut == null) {
            try {
                Print.sysOut = new PrintStream(System.out, true, Print.getEncoding());
            } catch (UnsupportedEncodingException uee) {
                Print.sysOut = System.out;
            }
        }
        return Print.sysOut;
    }

    /**
    *** Gets the System 'stderr' PrintStream with the previously specified character encoding
    *** @return The systenm 'stderr' PrintStream
    **/
    public static PrintStream getSystemErr()
    {
        if (Print.sysErr == null) {
            try {
                Print.sysErr = new PrintStream(System.err, true, "UTF-8");
            } catch (UnsupportedEncodingException uee) {
                Print.sysErr = System.err;
            }
        }
        return Print.sysErr;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a String representation of offset frame within the current stackframe.
    *** @param frame  The current frame offset
    *** @return The String representation of the requested frame
    **/
    protected static String _getStackFrame(int frame)
    {

        /* extract stack frame */
        Throwable t = new Throwable();
        t.fillInStackTrace();
        StackTraceElement st[] = t.getStackTrace();
        StackTraceElement sf = (st != null)? st[frame + 1] : null;

        /* no stack frame? */
        if (sf == null) {
            return "?";
        }

        /* get file */
        String clazz = sf.getClassName();
        String file  = sf.getFileName();
        if (file == null) {
            // Java code was compiled with 'debug=false'
            int p = 0;
            for (; (p < clazz.length()) && !Character.isUpperCase(clazz.charAt(p)); p++);
            if (p < clazz.length()) { clazz = clazz.substring(p); }
        } else
        if (file.toLowerCase().endsWith(_JAVA)) { 
            file = file.substring(0, file.length() - _JAVA.length()); 
            int p = clazz.indexOf(file);
            if (p >= 0) { clazz = clazz.substring(p); }
        }

        /* format frame description */
        StringBuffer sb = new StringBuffer();
        sb.append(clazz);
        sb.append(".").append(sf.getMethodName());
        sb.append(":").append(sf.getLineNumber());

        return sb.toString();
    }

    // ------------------------------------------------------------------------

    /**
    *** Prints the message and optional arguments to 'stdout'
    *** @param msg   The message (or message format)
    *** @param args  The optional message format arguments
    **/
    public static void _println(String msg, Object... args)
    {
        // Does not use RTConfig
        Print._println(null, msg, args);
    }

    /**
    *** Prints the message and optional arguments to the specified PrintStream
    *** @param ps    The output PrintStream
    *** @param msg   The message (or message format)
    *** @param args  The optional message format arguments
    **/
    public static void _println(PrintStream ps, String msg, Object... args)
    {
        // Does not use RTConfig
        Print._print(ps, 1, true, msg + "\n", args);
    }

    /**
    *** Prints the message and optional arguments to the specified PrintStream
    *** @param ps         The output PrintStream
    *** @param frame      The current frame offset used to tag this message
    *** @param printFrame True to print the current stackframe
    *** @param msg        The message (or message format)
    *** @param args       The optional message format arguments
    **/
    protected static void _println(PrintStream ps, int frame, boolean printFrame, String msg, Object... args)
    {
        // Does not use RTConfig
        Print._print(ps, frame + 1, printFrame, msg + "\n", args);
    }

    /**
    *** Prints the message and optional arguments to the specified PrintStream
    *** @param ps         The output PrintStream
    *** @param frame      The current frame offset used to tag this message
    *** @param msg        The message (or message format)
    *** @param args       The optional message format arguments
    **/
    protected static void _println(PrintStream ps, int frame, String msg, Object... args)
    {
        Print._print(ps, frame + 1, _includeStackFrame(), msg + "\n", args);
    }

    /**
    *** Prints the message and optional arguments to the specified PrintStream
    *** @param ps         The output PrintStream
    *** @param frame      The current frame offset used to tag this message
    *** @param msg        The message (or message format)
    *** @param args       The optional message format arguments
    **/
    protected static void _print(PrintStream ps, int frame, String msg, Object... args)
    {
        Print._print(ps, frame + 1, _includeStackFrame(), msg, args);
    }

    /**
    *** Prints the message and optional arguments to the specified PrintStream
    *** @param ps         The output PrintStream
    *** @param frame      The current frame offset used to tag this message
    *** @param printFrame True to print the current stackframe
    *** @param msg        The message (or message format)
    *** @param args       The optional message format arguments
    **/
    protected static void _print(PrintStream ps, int frame, boolean printFrame, String msg, Object... args)
    {
        // - use of RTConfig is NOT allowed in this method!
        // - if not writing to 'Print.stdout', then we really want to open/close this file

        /* Print stream */
        PrintStream out = (ps != null)? ps : Print.getSysStdout();
        if (out == null) {
            // it shouldn't be null
            return;
        }

        /* format */
        StringBuffer sb = new StringBuffer();
        if (printFrame) {
            sb.append("[");
            sb.append(_getStackFrame(frame + 1));
            sb.append("] ");
        }
        if (msg != null) {
            if ((args != null) && (args.length > 0)) {
                try {
                    sb.append(String.format(msg,args));
                } catch (Throwable th) { 
                    // MissingFormatArgumentException, UnknownFormatConversionException
                    System.out.println("ERROR: [" + msg + "] " + th); // [OUTPUT]
                    sb.append(msg);
                }
            } else {
                sb.append(msg);
            }
        }
        
        /* write */
        out.print(sb.toString()); // [OUTPUT]
        out.flush();

    }

    // ------------------------------------------------------------------------

    private static boolean allOutputToStdout = false;

    /**
    *** Set all output to stderr to be sent to stdout.
    *** @param state  True to send stderr output to stdout
    **/
    public static void setAllOutputToStdout(boolean state)
    {
        Print.allOutputToStdout = state;
    }

    /**
    *** Sets the stdout PrintStream
    *** @param out  The PrintStream to use for stdout
    **/
    public static void setSysStdout(PrintStream out)
    {
        Print.sysStdout = out;
    }

    /**
    *** Gets the stdout PrintStream
    *** @return The stdout PrintStream
    **/
    public static PrintStream getSysStdout()
    {
        return (Print.sysStdout != null)? Print.sysStdout : Print.getStdout();
    }

    /**
    *** Sets the stderr PrintStream
    *** @param out  The PrintStream to use for stderr
    **/
    public static void setSysStderr(PrintStream out)
    {
        Print.sysStderr = out;
    }

    /**
    *** Gets the stderr PrintStream
    *** @return The stderr PrintStream
    **/
    public static PrintStream getSysStderr()
    {
        if (Print.allOutputToStdout) {
            return Print.getSysStdout();
        } else {
            return (Print.sysStderr != null)? Print.sysStderr : Print.getStderr();
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Prints the specified message to stdout (no extra line terminator is included)<br>
    *** The stack-frame is ommitted.
    *** @param msg  The message to print
    *** @param args Any associated message arguments
    **/
    public static void sysPrint(String msg, Object... args)
    {
        PrintStream out = Print.getSysStdout();
        Print._print(out, 1, false, msg, args);
    }

    /**
    *** Prints the specified message to stdout (no extra line terminator is included)<br>
    *** The stack-frame is ommitted.
    *** @param msg  The message to print
    *** @param args Any associated message arguments
    **/
    public static void sysPrint(StringBuffer msg, Object... args)
    {
        PrintStream out = Print.getSysStdout();
        Print._print(out, 1, false, msg.toString(), args);
    }

    /**
    *** Prints the specified message to stdout (includes the line-terminator '\n')<br>
    *** The stack-frame is ommitted.
    *** @param msg  The message to print
    *** @param args Any associated message arguments
    **/
    public static void sysPrintln(String msg, Object... args)
    {
        PrintStream out = Print.getSysStdout();
        Print._println(out, 1, false, msg, args);
    }

    /**
    *** Prints the specified message to stdout (includes the line-terminator '\n')<br>
    *** The stack-frame is ommitted.
    *** @param msg  The message to print
    *** @param args Any associated message arguments
    **/
    public static void sysPrintln(StringBuffer msg, Object... args)
    {
        PrintStream out = Print.getSysStdout();
        Print._println(out, 1, false, msg.toString(), args);
    }

    // ------------------------------------------------------------------------

    /**
    *** Prints the specified message to stderr (no extra line terminator is included)<br>
    *** The stack-frame is ommitted.
    *** @param msg  The message to print
    *** @param args Any associated message arguments
    **/
    public static void errPrint(String msg, Object... args)
    {
        PrintStream out = Print.getSysStderr();
        Print._print(out, 1, false, msg, args);
    }

    /**
    *** Prints the specified message to stderr (no extra line terminator is included)<br>
    *** The stack-frame is ommitted.
    *** @param msg  The message to print
    *** @param args Any associated message arguments
    **/
    public static void errPrint(StringBuffer msg, Object... args)
    {
        PrintStream out = Print.getSysStderr();
        Print._print(out, 1, false, msg.toString(), args);
    }

    /**
    *** Prints the specified message to stderr (includes the line-terminator '\n')<br>
    *** The stack-frame is ommitted.
    *** @param msg  The message to print
    *** @param args Any associated message arguments
    **/
    public static void errPrintln(String msg, Object... args)
    {
        PrintStream out = Print.getSysStderr();
        Print._println(out, 1, false, msg, args);
    }

    /**
    *** Prints the specified message to stderr (includes the line-terminator '\n')<br>
    *** The stack-frame is ommitted.
    *** @param msg  The message to print
    *** @param args Any associated message arguments
    **/
    public static void errPrintln(StringBuffer msg, Object... args)
    {
        PrintStream out = Print.getSysStderr();
        Print._println(out, 1, false, msg.toString(), args);
    }

    // ------------------------------------------------------------------------

    /**
    *** Prints the specified message to stdout (no extra line terminator is included)<br>
    *** The stack-frame is ommitted.
    *** @param msg  The message to print
    *** @param args Any associated message arguments
    **/
    public static void print(String msg, Object... args)
    {
        Print._print(null, 1, false, msg, args);
    }

    /**
    *** Prints the specified message to stdout (no extra line terminator is included)<br>
    *** The stack-frame is ommitted.
    *** @param msg  The message to print
    *** @param args Any associated message arguments
    **/
    public static void print(StringBuffer msg, Object... args)
    {
        Print._print(null, 1, false, msg.toString(), args);
    }

    /**
    *** Prints the specified message to stdout (includes the line-terminator '\n')<br>
    *** The stack-frame is ommitted.
    *** @param msg  The message to print
    *** @param args Any associated message arguments
    **/
    public static void println(String msg, Object... args)
    {
        Print._println(null, 1, false, msg, args);
    }

    /**
    *** Prints the specified message to stdout (includes the line-terminator '\n')<br>
    *** The stack-frame is ommitted.
    *** @param msg  The message to print
    *** @param args Any associated message arguments
    **/
    public static void println(StringBuffer msg, Object... args)
    {
        Print._println(null, 1, false, msg.toString(), args);
    }

    // ------------------------------------------------------------------------
    
    protected static void _logStackTrace(int level, int frame, String msg, Throwable t)
    {

        /* pertinent level? */
        if (level > Print.getLogLevel()) {
            return;
        }

        /* log stack trace */
        Print._log(level, frame + 1, msg);
        try {
            PrintStream out = Print.openLogFile();
            _printStackTrace(out, frame + 1, null, t);
        } catch (Throwable loge) {
            _printStackTrace(null, frame + 1, null, t);
        } finally {
            Print.closeLogFile();
        }

        /* email */
        if (_emailExceptions()) {
            Print.sysPrintln("EMailing error...");
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            PrintStream bosOut = new PrintStream(bos);
            String host = Print.getHostName();
            
            /* include hostname */
            bosOut.println("From host: " + host);
            
            /* include stacktrace */
            _printStackTrace(bosOut, frame + 1, msg, t);
            
            /* close and send email */
            bosOut.close(); // may not be necessary
            //Print.emailError("[" + host + "] " + msg, bos.toString());
        }
    }
    
    protected static void _printStackTrace(PrintStream out, int frame, String msg, Throwable t)
    {
        
        /* get default print stream */
        if (out == null) {
            /* first try default stdout */
            out = Print.getSysStdout(); 
            if (out == null) {
                // failing that, set to stderr
                out = Print.getSystemErr();
            }
        }
        
        /* print stack trace */
        if (msg != null) {
            boolean printExceptionFrame = true;
            Print._println(out, frame + 1, printExceptionFrame, msg);
        }
        if (t == null) {
            t = new Throwable();
            t.fillInStackTrace();
            StackTraceElement oldst[] = t.getStackTrace();
            StackTraceElement newst[] = new StackTraceElement[oldst.length - (frame + 1)];
            System.arraycopy(oldst, frame + 1, newst, 0, newst.length);
            t.setStackTrace(newst);
        }
        t.printStackTrace(out); // [OUTPUT]
        if (t instanceof SQLException) {
            SQLException sqe = ((SQLException)t).getNextException();
            for (; (sqe != null); sqe = sqe.getNextException()) { 
                sqe.printStackTrace(out);  // [OUTPUT]
            }
        }
        
    }

    // ------------------------------------------------------------------------

    public static void logNotImplemented(String msg)
    {
        Print._logStackTrace(LOG_ERROR, 1, "Feature Not Implemented: " + msg, null);
    }

    public static void logException(String msg, Throwable t)
    {
        Print._logStackTrace(LOG_ERROR, 1, "Exception: " + msg, t);
    }

    public static void logStackTrace(String msg, Throwable t)
    {
        Print._logStackTrace(LOG_ERROR, 1, "Stacktrace: " + msg, t);
    }

    public static void logStackTrace(Throwable t)
    {
        Print._logStackTrace(LOG_ERROR, 1, "Stacktrace: ", t);
    }

    public static void logStackTrace(String msg)
    {
        Print._logStackTrace(LOG_WARN, 1, "Stacktrace: " + msg, null);
    }

    // ------------------------------------------------------------------------
    
    public static void logSQLError(int frame, String msg, SQLException sqe)
    {
        PrintStream ps = null;
        Print._log(LOG_ERROR, frame + 1, "==> SQLException: " + msg);
        while (sqe != null) {
            Print._log(LOG_ERROR, frame + 1, "Message:   " + sqe.getMessage());
            Print._log(LOG_ERROR, frame + 1, "SQLState:  " + sqe.getSQLState());
            Print._log(LOG_ERROR, frame + 1, "ErrorCode: " + sqe.getErrorCode());
            //if (sqe.getErrorCode() != DBFactory.SQLERR_DUPLICATE_KEY) {
            Print._printStackTrace(ps, frame + 1, sqe.toString(), sqe);
            //}
            sqe = sqe.getNextException();
        }
    }

    public static void logSQLError(SQLException sqe)
    {
        Print.logSQLError(1, "", sqe);
    }

    public static void logSQLError(String msg, SQLException sqe)
    {
        Print.logSQLError(1, msg, sqe);
    }

    // ------------------------------------------------------------------------

    private static Object       logLock                 = new Object();
    private static PrintStream  logOutput               = null;
    private static long         logRefCount             = 0L;
    private static int          openLogFile_recursion   = 0;

    /**
    *** Sets the output log file
    *** @param file  The output log file
    **/
    public static void setLogFile(File file)
    {
        printLogFile = file;
        printLogFile_init = true;
    }

    /**
    *** Gets the output log file
    *** @return  The output log file
    **/
    public static File getLogFile()
    {
        if (!printLogFile_init) {
            synchronized (Print.logLock) {
                if (!printLogFile_init) {
                    String dftLogFileStr = (printLogFile != null)? printLogFile.toString() : null;
                    String fileStr = RTConfig.insertKeyValues(RTConfig.getString(RTKey.LOG_FILE,dftLogFileStr));
                    printLogFile = !StringTools.isBlank(fileStr)? new File(fileStr) : null;
                    printLogFile_init = true;
                }
            }
        }
        return printLogFile;
    }

    /**
    *** Sets the maximum output log file size (in bytes).
    *** @param maxSize  The maximum size (in bytes)
    **/
    public static void setRotateLogFileSize(long maxSize)
    {
        if (maxSize < 0L) {
            printRotateLogFileSize = -1L;
        } else
        if (maxSize == 0L) {
            printRotateLogFileSize = 0L;
        } else
        if (maxSize < 5000L) {
            printRotateLogFileSize = 5000L;
        } else {
            printRotateLogFileSize = maxSize;
        }
    }

    /**
    *** Gets the maximum output log file size (in bytes).
    *** @return The maximum size (in bytes)
    **/
    public static long getRotateLogFileSize()
    {
        if (printRotateLogFileSize < 0L) {
            long maxSize = RTConfig.getLong(RTKey.LOG_FILE_ROTATE_SIZE, 0L);
            Print.setRotateLogFileSize((maxSize >= 0L)? maxSize : 0L);
        }
        return printRotateLogFileSize;
    }

    /**
    *** Sets the rotated log file delete age (in seconds)
    *** @param delAgeSec  The rotated log file delete age (in seconds)
    **/
    public static void setRotateDeleteAgeSec(long delAgeSec)
    {
        if (delAgeSec < 0L) {
            printRotateDelAgeSec = -1L;
        } else
        if (delAgeSec == 0L) {
            printRotateDelAgeSec = 0L;
        } else {
            printRotateDelAgeSec = delAgeSec;
        }
    }

    /**
    *** Gets the maximum age of rotated log files (in seconds)
    *** @return The maximum acceptable age of rotated log files (in seconds)
    **/
    public static long getRotateDeleteAgeSec()
    {
        if (printRotateDelAgeSec < 0L) {
            String delAgeStr = RTConfig.getString(RTKey.LOG_FILE_ROTATE_DELETE_AGE);
            if (StringTools.isBlank(delAgeStr)) {
                // no delete
                printRotateDelAgeSec = 0L;
            } else {
                long delAgeVal = StringTools.parseLong(delAgeStr,0L);
                if (delAgeVal > 0L) {
                    if (delAgeStr.endsWith("s")) {
                        // seconds
                        //Print.sysPrintln("Deleting aged rotated files: %d seconds", delAgeVal);
                        printRotateDelAgeSec = delAgeVal;
                    } else
                    if (delAgeStr.endsWith("m")) {
                        // minutes
                        //Print.sysPrintln("Deleting aged rotated files: %d minutes", delAgeVal);
                        printRotateDelAgeSec = DateTime.MinuteSeconds(delAgeVal);
                    } else {
                        // days
                        //Print.sysPrintln("Deleting aged rotated files: %d days", delAgeVal);
                        printRotateDelAgeSec = DateTime.DaySeconds(delAgeVal);
                    }
                } else {
                    // no delete
                    printRotateDelAgeSec = 0L;
                }
            }
        }
        return printRotateDelAgeSec;
    }

    /**
    *** Opens the output log file
    *** @return The output log file PrintStream
    **/
    protected static PrintStream openLogFile()
    {
        // Do not make calls to "logXXXXXX" from within this method (infinite recursion could result)
        // Calls to 'println' and 'sysPrintln' are ok.

        /* check to see if this has been called before RTConfig has completed initialization */
        if (!RTConfig.isInitialized()) {
            return Print.getSystemErr();
        } 

        /* return log PrintStream */
        PrintStream out = null;
        synchronized (Print.logLock) {
            if (openLogFile_recursion > 0) {
                Print.sysPrintln("[Print.openLogFile] Recursive call to 'openLogFile'!!!");
                Throwable t = new Throwable();
                t.fillInStackTrace();
                t.printStackTrace();  // [OUTPUT]
                return Print.getSystemErr();
            } else {
                openLogFile_recursion++;
                
                /* increment log counter */
                Print.logRefCount++;
    
                /* get log file */
                if (Print.logOutput != null) {

                    /* already open */
                    out = Print.logOutput;

                } else {
    
                    /* get/return log file */
                    File logFile = Print.getLogFile();
                    if ((logFile == null) || logFile.toString().equals("")) {

                        // no log file specified
                        out = Print.getSystemErr();

                    } else
                    if (logFile.isDirectory()) {

                        // invalid file specification
                        Print.setLogFile(null);
                        Print.sysPrintln("ERROR: Invalid Print log file specification: " + logFile);
                        out = Print.getSystemErr();

                    } else {

                        // rotate file
                        long maxSize = Print.getRotateLogFileSize();
                        if ((maxSize > 0L) && logFile.exists() && (logFile.length() > maxSize)) {
                            String rotExtnSep = "."; // must not be empty
                            final long nowMS  = DateTime.getCurrentTimeMillis();

                            // backup existing file
                            String absPath = logFile.getAbsolutePath();
                            String rotExtn = Print.formatDate(RTConfig.getString(RTKey.LOG_FILE_ROTATE_EXTN));
                            String bkuName = absPath + rotExtnSep + rotExtn;
                            File bkuFile = new File(bkuName);
                            for (int i = 1; bkuFile.exists(); i++) { 
                                // find a filename that does not exist
                                bkuName = absPath + rotExtnSep + rotExtn + "." + i;
                                bkuFile = new File(bkuName); 
                            }
                            boolean didRename = false;
                            try {
                                didRename = logFile.renameTo(bkuFile);
                                if (didRename) {
                                    // update modified time (so this file isn't deleted below)
                                    bkuFile.setLastModified(nowMS);
                                }
                            } catch (Throwable th) {
                                // error renaming
                                //Print.sysPrintln("Unable to rename logFile: " + file + " ==> " + bkuFile);
                                //th.printStackTrace(); // to stderr/stdout
                                didRename = false;
                            }

                            // delete old rotated files?
                            final long delAgeSec = Print.getRotateDeleteAgeSec();
                            if (delAgeSec > 0L) {
                                final long   delAgeMS = delAgeSec * 1000L;
                                final String logName_ = logFile.getName() + rotExtnSep; // include rotate extension separator
                                File delFiles[] = logFile.getParentFile().listFiles(new FileFilter() {
                                    public boolean accept(File f) {
                                        if (!f.getName().startsWith(logName_)) {
                                            // not a rotated file
                                            return false;
                                        } else
                                        if ((nowMS - f.lastModified()) < delAgeMS) {
                                            // too young
                                            return false;
                                        }
                                        // mark for deletion
                                        return true;
                                    }
                                });
                                if (!ListTools.isEmpty(delFiles)) {
                                    // delete files
                                    for (int i = 0; i < delFiles.length; i++) {
                                        boolean didDelete = false;
                                        try {
                                            didDelete = delFiles[i].delete();
                                        } catch (Throwable th) {
                                            // error deleting
                                            //Print.sysPrintln("Unable to delete logFile: " + delFiles[i]);
                                            //th.printStackTrace(); // to stderr/stdout
                                            didDelete = false;
                                        }
                                        if (RTConfig.isDebugMode()) {
                                            if (didDelete) {
                                                Print.sysPrintln("Delete : " + delFiles[i]);
                                            } else {
                                                Print.sysPrintln("Delete Failed: " + delFiles[i]);
                                            }
                                        }
                                    }
                                }
                            }

                        }

                        // open output stream
                        try {
                            out = new PrintStream(new FileOutputStream(logFile,true));
                        } catch (IOException ioe) {
                            Print.setLogFile(null);
                            Print.sysPrintln("ERROR: Unable to open Print log file: " + logFile);
                            out = Print.getSystemErr();
                        }

                    }
                    Print.logOutput = out;

                }

                openLogFile_recursion--;
            }
        }
        return out;
    }

    /**
    *** Closes the output log file
    **/
    protected static void closeLogFile()
    {
        synchronized (Print.logLock) {
            
            /* decrement log counter */
            Print.logRefCount--;
            if (Print.logRefCount < 0) { Print.logRefCount = 0L; }
            
            /* close */
            if ((Print.logRefCount == 0L) && (Print.logOutput != null)) {
                // don't close if stderr or stdout
                if ((Print.logOutput != Print.getSystemOut()) && 
                    (Print.logOutput != Print.getSystemErr())   ) {
                    try {
                        Print.logOutput.close();
                    } catch (Throwable t) {
                        Print.sysPrintln("Unable to close log file: " + t);
                    }
                }
                Print.logOutput = null;
            }
            
        }
    }

    // ------------------------------------------------------------------------

    public static void setLogLevel(int level, boolean inclDate, boolean inclFrame)
    {
        Print.setLogLevel(level);
        printLogIncludeDate  = inclDate? 1 : 0;
        printLogIncludeFrame = inclFrame? 1 : 0;
    }

    public static void setLogLevel(int level)
    {
        if (level <= LOG_UNDEFINED) {
            level = LOG_UNDEFINED;
        } else 
        if (level > LOG_ALL) {
            level = LOG_ALL;
        }
        printLogLevel = level;
    }

    public static int getLogLevel()
    {
        if (printLogLevel <= LOG_UNDEFINED) {
            printLogLevel = Print.parseLogLevel(RTConfig.getString(RTKey.LOG_LEVEL,null), LOG_INFO);
        }
        return printLogLevel;
    }

    public static boolean isDebugLoggingLevel()
    {
        return (Print.getLogLevel() >= Print.LOG_DEBUG);
    }

    public static void setLogHeaderLevel(int level)
    {
        if (level <= LOG_UNDEFINED) {
            level = LOG_UNDEFINED;
        } else 
        if (level > LOG_ALL) {
            level = LOG_ALL;
        }
        printLogHeaderLevel = level;
    }

    public static int getLogHeaderLevel()
    {
        if (printLogHeaderLevel <= LOG_UNDEFINED) {
            printLogHeaderLevel = Print.parseLogLevel(RTConfig.getString(RTKey.LOG_LEVEL_HEADER,null), LOG_ALL);
        }
        return printLogHeaderLevel;
    }

    public static String getLogLevelString(int level)
    {
        if (level <= LOG_OFF) { return "OFF"; }
        switch (level) {
            case LOG_FATAL: return "FATAL";
            case LOG_ERROR: return "ERROR";
            case LOG_WARN : return "WARN_";
            case LOG_INFO : return "INFO_";
            case LOG_DEBUG: return "DEBUG";
        }
        return "ALL";
    }

    protected static int parseLogLevel(String val, int dft)
    {
        String v = (val != null)? val.toUpperCase() : null;
        if (StringTools.isBlank(v)) {
            return dft; // LOG_OFF;
        } else
        if (Character.isDigit(v.charAt(0))) {
            int lvl = StringTools.parseInt(v.substring(0,1),LOG_ALL);
            if (lvl < LOG_OFF) {
                return LOG_OFF;
            } else 
            if (lvl > LOG_ALL) {
                return LOG_ALL;
            } else {
                return lvl;
            }
        } else
        if (v.startsWith("OFF")) {
            return LOG_OFF;
        } else
        if (v.startsWith("FAT")) {
            return LOG_FATAL;
        } else
        if (v.startsWith("ERR")) {
            return LOG_ERROR;
        } else
        if (v.startsWith("WAR")) {
            return LOG_WARN;
        } else
        if (v.startsWith("INF")) {
            return LOG_INFO;
        } else
        if (v.startsWith("DEB")) {
            return LOG_DEBUG;
         } else
        if (v.startsWith("ALL")) {
            return LOG_ALL;
       } else {
            return dft; // LOG_ALL;
        }
    }

    // ------------------------------------------------------------------------

    public static void log(int level, String msg)
    {
        Print._log(level, 1, msg);
    }

    public static void logFatal(String msg, Object... args)
    {
        Print._log(LOG_FATAL, 1, msg, args);
    }

    public static void logError(String msg, Object... args)
    {
        Print._log(LOG_ERROR, 1, msg, args);
    }

    public static void logWarn(String msg, Object... args)
    {
        Print._log(LOG_WARN, 1, msg, args);
    }

    public static void logInfo(String msg, Object... args)
    {
        Print._log(LOG_INFO, 1, msg, args);
    }

    public static void logDebug(String msg, Object... args)
    {
        Print._log(LOG_DEBUG, 1, msg, args);
    }

    public static void _log(int level, int frame, String msg, Object... args)
    {

        /* pertinent level? */
        if (level > Print.getLogLevel()) {
            return;
        }

        /* message accumulator */
        StringBuffer logMsg = new StringBuffer();

        /* log message */
        if (level <= Print.getLogHeaderLevel()) {
            // Print this 'header' info for logged messages with a level < 'headerLevel'
            // ie. print header for errors/warnings, but not for info/debug
            logMsg.append("[");
            logMsg.append(Print.getLogLevelString(level));
            if (Print._includeDate()) {
                logMsg.append("|");
                logMsg.append(Print.formatDate("MM/dd HH:mm:ss")); // "yyyy/MM/dd HH:mm:ss"
            }
            if (Print._includeStackFrame()) {
                logMsg.append("|");
                logMsg.append(_getStackFrame(frame + 1));
            }
            logMsg.append("] ");
        }
        
        /* message */
        if (msg != null) {
            if ((args != null) && (args.length > 0)) {
                try {
                    logMsg.append(String.format(msg,args));
                } catch (Throwable th) { 
                    // MissingFormatArgumentException, UnknownFormatConversionException
                    System.out.println("ERROR: [" + msg + "] " + th); // [OUTPUT]
                    logMsg.append(msg);
                }
            } else {
                logMsg.append(msg);
            }
            if (!msg.endsWith("\n")) { logMsg.append("\n"); }
        } else {
            logMsg.append("\n");
        }

        /* print message */
        Print._writeLog(logMsg.toString());

    }

    public static void _writeLog(String logMsg)
    {
        try {
            PrintStream out = RTConfig.isInitialized()? Print.openLogFile() : Print.getSystemErr();
            if (out != null) { 
                // logfile output
                byte d[] = StringTools.getBytes(logMsg);
                out.write(d);  // [OUTPUT] must be "write(...)"
                out.flush();
            } else {
                Print._print(null, 0, false, logMsg);
            }
        } catch (IOException ioe) {
            Print.setLogFile(null);
            Print.logError("Unable to open/write log file: " + ioe);
            Print._print(null, 0, false, logMsg);
        } finally {
            Print.closeLogFile();
        }
    }
    
    // ------------------------------------------------------------------------

    public static class NullOutputStream
        extends OutputStream
    {
        public NullOutputStream() {}
        public void write(int b) throws IOException {}
        public void write(byte[] b) throws IOException {}
        public void write(byte[] b, int off, int len) throws IOException {}
        public void flush() throws IOException {}
        public void close() throws IOException {}
    }
    
    public static class NullPrintStream
        extends PrintStream
    {
        public NullPrintStream() { super(new NullOutputStream()); }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

}
