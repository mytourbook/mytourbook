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
//  This class provides many File based utilities
// ----------------------------------------------------------------------------
// Change History:
//  2006/03/26  Martin D. Flynn
//      Initial release
//  2006/06/30  Martin D. Flynn
//     -Repackaged
//  2008/05/14  Martin D. Flynn
//     -Added method 'writeEscapedUnicode'
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.io.*;

/**
*** File handling tools
**/

public class FileTools
{

    // ------------------------------------------------------------------------

    /**
    *** Copies bytes from one stream to another
    *** @param input  The InputStream
    *** @param output The OutputStream
    *** @return The number of bytes copied
    **/
    public static int copyStreams(InputStream input, OutputStream output)
        throws IOException
    {
        return FileTools.copyStreams(input, output, -1);
    }

    /**
    *** Copies bytes from one stream to another
    *** @param input  The InputStream
    *** @param output The OutputStream
    *** @param maxLen The maximum number of bytes to copy
    *** @return The number of bytes copied
    **/
    public static int copyStreams(InputStream input, OutputStream output, int maxLen)
        throws IOException
    {

        /* copy nothing? */
        if (maxLen == 0) {
            return 0;
        }

        /* copy bytes */
        int length = 0; // count of bytes copied
        byte tmpBuff[] = new byte[10 * 1024]; // 10K blocks
        while (true) {

            /* read length */
            int readLen;
            if (maxLen >= 0) {
                readLen = maxLen - length;
                if (readLen == 0) {
                    break; // done reading
                } else
                if (readLen > tmpBuff.length) {
                    readLen = tmpBuff.length; // max block size
                }
            } else {
                readLen = tmpBuff.length;
            }

            /* read input stream */
            int cnt = input.read(tmpBuff, 0, readLen);

            /* copy to output stream */
            if (cnt < 0) {
                if ((maxLen >= 0) && (length != maxLen)) {
                    Print.logError("Copy size mismatch: " + maxLen + " => " + length);
                }
                break;
            } else
            if (cnt > 0) {
                output.write(tmpBuff, 0, cnt);
                length += cnt;
                if ((maxLen >= 0) && (length >= maxLen)) {
                    break; // per 'maxLen', done copying
                }
            } else {
                //Print.logDebug("Read 0 bytes ... continuing");
            }

        }
        output.flush();

        /* return number of bytes copied */
        return length;
    }

    // ------------------------------------------------------------------------

    /**
    *** Opens the specified file for reading
    *** @param file  The path of the file to open
    *** @return The opened InputStream
    **/
    public static InputStream openInputFile(String file)
    {
        if ((file != null) && !file.equals("")) {
            return FileTools.openInputFile(new File(file));
        } else {
            return null;
        }
    }

    /**
    *** Opens the specified file for reading
    *** @param file  The file to open
    *** @return The opened InputStream
    **/
    public static InputStream openInputFile(File file)
    {
        try {
            return new FileInputStream(file);
        } catch (IOException ioe) {
            Print.logError("Unable to open file: " + file + " [" + ioe + "]");
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Closes the specified InputStream
    *** @param in  The InputStream to close
    **/
    public static void closeStream(InputStream in)
    {
        if (in != null) {
            try {
                in.close();
            } catch (IOException ioe) {
                //Print.logError("Unable to close stream: " + ioe);
            }
        }
    }
    
    /**
    *** Closes the specified OutputStream
    *** @param out  The OutputStream to close
    **/
    public static void closeStream(OutputStream out)
    {
        if (out != null) {
            try {
                out.close();
            } catch (IOException ioe) {
                //Print.logError("Unable to close stream: " + ioe);
            }
        }
    }

    // ------------------------------------------------------------------------

    /** 
    *** Returns an array of bytes read from the specified InputStream
    *** @param input  The InputStream
    *** @return The array of bytes read from the InputStream
    **/
    public static byte[] readStream(InputStream input)
        throws IOException
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        copyStreams(input, output);
        return output.toByteArray();
    }

    // ------------------------------------------------------------------------

    /**
    *** Writes a String to the specified OutputStream
    *** @param output  The OutputStream 
    *** @param dataStr The String to write to the OutputStream
    **/
    public static void writeStream(OutputStream output, String dataStr)
        throws IOException
    {
        byte data[] = dataStr.getBytes();
        output.write(data, 0, data.length);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns an array of bytes read from the specified file
    *** @param file  The file path from which the byte array is read
    *** @return The byte array read from the specified file
    **/
    public static byte[] readFile(String file)
    {
        if ((file != null) && !file.equals("")) {
            return FileTools.readFile(new File(file));
        } else {
            return null;
        }
    }

    /**
    *** Returns an array of bytes read from the specified file
    *** @param file  The file from which the byte array is read
    *** @return The byte array read from the specified file
    **/
    public static byte[] readFile(File file)
    {
        if (file == null) {
            return null;
        } else
        if (!file.exists()) {
            Print.logError("File does not exist: " + file);
            return null;
        } else {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                return readStream(fis);
            } catch (IOException ioe) {
                Print.logError("Unable to read file: " + file + " [" + ioe + "]");
            } finally {
                if (fis != null) { try { fis.close(); } catch (IOException ioe) {/*ignore*/} }
            }
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Reads a single line of characters from the specified InputStream, terminated by
    *** either a newline (\n) or carriage-return (\r)
    *** @param input  The InputStream
    *** @return The line read from the InputStream
    **/
    public static String readLine(InputStream input)
        throws IOException
    {
        StringBuffer sb = new StringBuffer();
        while (true) {
            int ch = input.read();
            if (ch < 0) { // eof
                throw new EOFException("End of InputStream");
            } else
            if ((ch == '\r') || (ch == '\n')) {
                return sb.toString();
            }
            sb.append((char)ch);
        }
    }

    /**
    *** Reads a single line of characters from stdin, terminated by
    *** either a newline (\n) or carriage-return (\r)
    *** @return The line read from stdin
    **/
    public static String readLine_stdin()
        throws IOException
    {
        while (System.in.available() > 0) { System.in.read(); }
        return FileTools.readLine(System.in);
    }

    /**
    *** Prints a message, and reads a line of text from stdin
    *** @param msg  The message to print
    *** @param dft  The default String returned, if no text was entered
    *** @return The line of text read from stdin
    **/
    public static String readString_stdin(String msg, String dft)
        throws IOException
    {
        if (msg == null) { msg = ""; }
        Print.sysPrintln(msg + "    [String: default='" + dft + "'] ");
        for (;;) {
            Print.sysPrint("?");
            String line = FileTools.readLine_stdin();
            if (line.equals("")) {
                if (dft != null) {
                    return dft;
                } else {
                    // if there is no default, a non-empty String is required
                    Print.sysPrint("String required, please re-enter] ");
                    continue;
                }
            }
            return line;
        }
    }

    /**
    *** Prints a message, and reads a boolean value from stdin
    *** @param msg  The message to print
    *** @param dft  The default boolean value returned, if no value was entered
    *** @return The boolean value read from stdin
    **/
    public static boolean readBoolean_stdin(String msg, boolean dft)
        throws IOException
    {
        if (msg == null) { msg = ""; }
        Print.sysPrintln(msg + "    [Boolean: default='" + dft + "'] ");
        for (;;) {
            Print.sysPrint("?");
            String line = FileTools.readLine_stdin().trim();
            if (line.equals("")) {
                return dft;
            } else
            if (!StringTools.isBoolean(line,true)) {
                Print.sysPrint("Boolean required, please re-enter] ");
                continue;
            }
            return StringTools.parseBoolean(line, dft);
        }
    }

    /**
    *** Prints a message, and reads a long value from stdin
    *** @param msg  The message to print
    *** @param dft  The default long value returned, if no value was entered
    *** @return The long value read from stdin
    **/
    public static long readLong_stdin(String msg, long dft)
        throws IOException
    {
        if (msg == null) { msg = ""; }
        Print.sysPrintln(msg + "    [Long: default='" + dft + "'] ");
        for (;;) {
            Print.sysPrint("?");
            String line = FileTools.readLine_stdin().trim();
            if (line.equals("")) {
                return dft;
            } else
            if (!Character.isDigit(line.charAt(0)) && (line.charAt(0) != '-')) {
                Print.sysPrint("Long required, please re-enter] ");
                continue;
            }
            return StringTools.parseLong(line, dft);
        }
    }

    /**
    *** Prints a message, and reads a double value from stdin
    *** @param msg  The message to print
    *** @param dft  The default double value returned, if no value was entered
    *** @return The double value read from stdin
    **/
    public static double readDouble_stdin(String msg, double dft)
        throws IOException
    {
        if (msg == null) { msg = ""; }
        Print.sysPrintln(msg + "    [Double: default='" + dft + "'] ");
        for (;;) {
            Print.sysPrint("?");
            String line = FileTools.readLine_stdin().trim();
            if (line.equals("")) {
                return dft;
            } else
            if (!Character.isDigit(line.charAt(0)) && (line.charAt(0) != '-') && (line.charAt(0) != '.')) {
                Print.sysPrint("Double required, please re-enter] ");
                continue;
            }
            return StringTools.parseDouble(line, dft);
        }
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Writes a byte array to the specified file
    *** @param data  The byte array to write to the file
    *** @param file  The file to which the byte array is written
    *** @return True if the bytes were successfully written to the file
    *** @throws IOException if an error occurred.
    **/
    public static boolean writeFile(byte data[], File file)
        throws IOException
    {
        return FileTools.writeFile(data, file, false);
    }

    /**
    *** Writes a byte array to the specified file
    *** @param data  The byte array to write to the file
    *** @param file  The file to which the byte array is written
    *** @param append True to append the bytes to the file, false to overwrite.
    *** @return True if the bytes were successfully written to the file
    *** @throws IOException if an error occurred.
    **/
    public static boolean writeFile(byte data[], File file, boolean append)
        throws IOException
    {
        if ((data != null) && (file != null)) {
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(file, append);
                fos.write(data, 0, data.length);
                return true;
            } finally {
                try { fos.close(); } catch (Throwable t) {/* ignore */}
            }
        } 
        return false;
    }

    /**
    *** Writes a String to the specified file in "ISO-8859-1" character encoding.<br>
    *** Unicode characters are escaped using the '\u0000' format.
    *** @param dataStr  The String to write to the file
    *** @param file     The file to which the byte array is written
    *** @return True if the String was successfully written to the file
    *** @throws IOException if an error occurred.
    **/
    public static boolean writeEscapedUnicode(String dataStr, File file)
        throws IOException
    {
        boolean append = false;
        if ((dataStr != null) && (file != null)) {
            FileOutputStream fos = new FileOutputStream(file, append);
            BufferedWriter fbw = null;
            try {
                fbw = new BufferedWriter(new OutputStreamWriter(fos, "8859_1"));
                int len = dataStr.length();
                for (int i = 0; i < len; i++) {
                    char ch = dataStr.charAt(i);
                    if ((ch == '\n') || (ch == '\r')) {
                        fbw.write(ch);
                    } else
                    if ((ch == '\t') || (ch == '\f')) {
                        fbw.write(ch);
                    } else
                    if ((ch < 0x0020) || (ch > 0x007e)) {
                        fbw.write('\\');
                        fbw.write('u');
                        fbw.write(StringTools.hexNybble((ch >> 12) & 0xF));
                        fbw.write(StringTools.hexNybble((ch >>  8) & 0xF));
                        fbw.write(StringTools.hexNybble((ch >>  4) & 0xF));
                        fbw.write(StringTools.hexNybble( ch        & 0xF));
                    } else {
                        fbw.write(ch);
                    }
                }
                return true;
            } finally {
                try { fbw.close(); } catch (Throwable t) {/* ignore */}
            }
        } 
        return false;
    }

    // ------------------------------------------------------------------------

    /** 
    *** Gets the extension characters from the specified file name
    *** @param filePath  The file name
    *** @return The extension characters
    **/
    public static String getExtension(String filePath) 
    {
        if (filePath != null) {
            return getExtension(new File(filePath));
        }
        return "";
    }

    /** 
    *** Gets the extension characters from the specified file
    *** @param file  The file
    *** @return The extension characters
    **/
    public static String getExtension(File file) 
    {
        if (file != null) {
            String fileName = file.getName();
            int p = fileName.indexOf(".");
            if ((p >= 0) && (p < (fileName.length() - 1))) {
                return fileName.substring(p + 1);
            }
        }
        return "";
    }

    /**
    *** Returns true if the specified file path has an extension which matches one of the
    *** extensions listed in the specified String array
    *** @param filePath  The file path/name
    *** @param extn      An array of file extensions
    *** @return True if teh specified file path has a matching exention
    **/
    public static boolean hasExtension(String filePath, String extn[])
    {
        if (filePath != null) {
            return hasExtension(new File(filePath), extn);
        }
        return false;
    }

    /**
    *** Returns true if the specified file has an extension which matches one of the
    *** extensions listed in the specified String array
    *** @param file      The file
    *** @param extn      An array of file extensions
    *** @return True if teh specified file has a matching exention
    **/
    public static boolean hasExtension(File file, String extn[])
    {
        if ((file != null) && (extn != null)) {
            String e = getExtension(file);
            for (int i = 0; i < extn.length; i++) {
                if (e.equalsIgnoreCase(extn[i])) { return true; }
            }
        }
        return false;
    }

    /**
    *** Removes the extension from the specified file path
    *** @param filePath  The file path from which the extension will be removed
    *** @return The file path with the extension removed
    **/
    public static String removeExtension(String filePath)
    {
        if (filePath != null) {
            return removeExtension(new File(filePath));
        }
        return filePath;
    }

    /**
    *** Removes the extension from the specified file
    *** @param file  The file from which the extension will be removed
    *** @return The file path with the extension removed
    **/
    public static String removeExtension(File file)
    {
        if (file != null) {
            String fileName = file.getName();
            int p = fileName.indexOf(".");
            if (p > 0) { // '.' in column 0 not allowed
                file = new File(file.getParentFile(), fileName.substring(0, p));
            }
            return file.getPath();
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified character is a file separator
    *** @param ch  The character to test
    *** @return True if the specified character is a file separator
    **/
    public static boolean isFileSeparatorChar(char ch)
    {
        if (ch == File.separatorChar) {
            // simple test, matches Java's understanding of a file path separator
            return true;
        } else 
        if (OSTools.isWindows() && (ch == '/')) {
            // '/' can be used as a file path separator on Windows
            return true;
        } else {
            // not a file path separator character
            return false;
        }
    }

    /**
    *** Returns true if the specified String contains a file separator
    *** @param fn  The String file path
    *** @return True if the file String contains a file separator
    **/
    public static boolean hasFileSeparator(String fn)
    {
        if (fn == null) {
            // no string, no file separator
            return false;
        } else
        if (fn.indexOf(File.separator) >= 0) {
            // simple test, matches Java's understanding of a file path separator
            return true;
        } else
        if (OSTools.isWindows() && (fn.indexOf('/') >= 0)) {
            // '/' can be used as a file path separator on Windows
            return true;
        } else {
            // no file path separator found
            return false;
        }
    }

    // ------------------------------------------------------------------------

    private static final String ARG_FILE[]          = new String[] { "file" };              // file path
    private static final String ARG_HEX[]           = new String[] { "hex"  };              // boolean
    private static final String ARG_WIDTH[]         = new String[] { "width", "w" };        // int
    private static final String ARG_DUMP[]          = new String[] { "dump" };              // boolean
    private static final String ARG_PACK[]          = new String[] { "pack" };              // boolean
    private static final String ARG_UNI_ENCODE[]    = new String[] { "ue", "uniencode" };   // boolean
    private static final String ARG_UNI_DECODE[]    = new String[] { "ud", "unidecode" };   // boolean

    /**
    *** Display usage
    **/
    private static void usage()
    {
        Print.sysPrintln("Usage:");
        Print.sysPrintln("  java ... " + FileTools.class.getName() + " {options}");
        Print.sysPrintln("Options:");
        Print.sysPrintln("  -file=<file>    File path");
        Print.sysPrintln("  -dump           Print hex dump");
        Print.sysPrintln("  -unidecode      Decode unicode escaped file (output to stdout)");
        System.exit(1);
    }

    /**
    *** Debug/Testing entry point
    *** @param argv  The COmmand-line args
    **/
    public static void main(String argv[])
        throws Throwable
    {
        RTConfig.setCommandLineArgs(argv);
        File file = RTConfig.getFile(ARG_FILE,null);
        if (file == null) {
            usage();
        }
        byte data[] = FileTools.readFile(file);

        /* parse hex */
        if (RTConfig.getBoolean(ARG_HEX,false)) {
            String hexStr = new String(data);
            data = StringTools.parseHex(hexStr, data);
        }

        /* hex dump */
        if (RTConfig.getBoolean(ARG_DUMP,false)) {
            System.out.println("Size " + ((data!=null)?data.length:-1));
            int width = RTConfig.getInt(ARG_WIDTH,16);
            System.out.println(StringTools.formatHexString(data,width));
            System.exit(0);
        }

        /* hex pack */
        if (RTConfig.getBoolean(ARG_PACK,false)) {
            System.out.println("Size " + ((data!=null)?data.length:-1));
            System.out.println(StringTools.toHexString(data));
            System.exit(0);
        }

        /* unicode encode */
        if (RTConfig.getBoolean(ARG_UNI_DECODE,false)) {
            String dataStr = StringTools.unescapeUnicode(StringTools.toStringValue(data));
            Print.sysPrintln(dataStr);
            System.exit(0);
        }

        /* done */
        usage();

    }

}
