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
//  Create compile-time contant source module
// ----------------------------------------------------------------------------
// Change History:
//  2009/01/28  Martin D. Flynn
//      Initial release
package org.opengts.util;

import java.util.*;
import java.io.*;

import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.text.DateFormat;

/**
*** Create compile-time contant source module
**/

public class CompiletimeVars
{

    // ------------------------------------------------------------------------

    /* standard default template */
    private static final String TEMPLATE_DEFAULT            = "@default";

    /* compile-time variable delimiters (should be unique to this module) */
    // If these delimters change, make sure that the standard template delimiters 
    // used below also change accordingly.
    private static final String STR_DELIM                   = "%{";
    private static final String END_DELIM                   = "}";
    private static final String DFT_DELIM                   = "=";

    // ------------------------------------------------------------------------

    private static final String JAVA_PACKAGE                = "package";
    private static final String JAVA_PACKAGE_               = JAVA_PACKAGE + " ";

    private static String packageLine(String pkgName)
    {
        if (StringTools.isBlank(pkgName)) {
            return "// no package";
        } else
        if (pkgName.startsWith(JAVA_PACKAGE_)) {
            return pkgName + ";";
        } else {
            return JAVA_PACKAGE_ + pkgName + ";";
        }
    }

    private static String standardTemplate(String tn, String pkg)
    {

        /* default/CompileTimestamp template */
        if (StringTools.isBlank(tn) || tn.equals("@") || tn.equalsIgnoreCase(TEMPLATE_DEFAULT)) {
            StringBuffer sb = new StringBuffer();
            sb.append(CompiletimeVars.packageLine(pkg)).append("\n");
            sb.append("public class CompileTime\n");
            sb.append("{\n");
            sb.append("    // %{datetime=0000/00/00 00:00:00 GMT}\n");
            sb.append("    public static final long COMPILE_TIMESTAMP = %{timestamp=0}L;\n");
            sb.append("}\n");
            return sb.toString();
        }
        
        /* standard template not found */
        Print.errPrintln("Standard Template name not found: " + tn);
        return null;

    }

    private static String readTemplate(File tf, String pkg)
    {
        
        /* read template data */
        byte templData[] = FileTools.readFile(tf);
        if (templData == null) {
            Print.errPrintln("Unable to read Input/Template file: " + tf);
            return null;
        } else
        if (templData.length == 0) {
            Print.errPrintln("Input/Template file is empty: " + tf);
            return null;
        }
        
        /* return template String */
        String templateText = StringTools.toStringValue(templData);
        if (!StringTools.isBlank(pkg) && !StringTools.isBlank(templateText)) {
            String lines[] = StringTools.split(templateText,'\n',false);
            for (int i = 0; i < lines.length; i++) {
                if (lines[i].trim().startsWith(JAVA_PACKAGE_)) {
                    lines[i] = CompiletimeVars.packageLine(pkg);
                    return StringTools.join(lines,'\n') + "\n";
                }
            }
            StringBuffer sb = new StringBuffer();
            sb.append(CompiletimeVars.packageLine(pkg)).append("\n");
            sb.append(templateText);
            return sb.toString();
        } else {
            return templateText;
        }

    }

    private static String getTemplate(String templateName, boolean optional, String pkgName)
    {
        if (templateName.startsWith("@")) {
            String template = standardTemplate(templateName, pkgName);
            if (StringTools.isBlank(template) && optional) {
                template = standardTemplate(null, pkgName);
            }
            return template;
        } else {
            File templateFile = new File(templateName);
            if (!templateFile.isFile()) {
                // file does not exist
                if (RTConfig.hasProperty(ARG_OPTIONAL)) {
                    return standardTemplate(null, pkgName);
                } else {
                    Print.errPrintln("Input/Template file does not exist: " + templateFile);
                    return null;
                }
            } else {
                return readTemplate(templateFile, pkgName);
            }
        }
    }

    // ------------------------------------------------------------------------
    
    private static final String ARG_HELP[]          = new String[] { "help"     , "h"   };
    private static final String ARG_OPTIONAL[]      = new String[] { "template?", "t?"  };
    private static final String ARG_TEMPLATE[]      = new String[] { "template" , "t"   };
    private static final String ARG_PACKAGE[]       = new String[] { "package"  , "p"   };
    private static final String ARG_OUTPUT[]        = new String[] { "output"   , "o"   };
    private static final String ARG_OVERWRITE[]     = new String[] { "overwrite", "w"   };

    /**
    *** Print usage and exit
    **/
    private static void _usage()
    {
        Print.sysPrintln("Usage:");
        Print.sysPrintln("  java ... " + CompiletimeVars.class.getName() + " {options}");
        Print.sysPrintln("Options:");
        Print.sysPrintln("  -template=@default      Create default 'CompileTime' template");
        Print.sysPrintln("  -template=<file>        Input Java 'template' file (must exist)");
        Print.sysPrintln("  -template?=<file>       Optional input Java 'template' file (may exist)");
        Print.sysPrintln("  -package=<packageName>  Optional package name");
        Print.sysPrintln("  -output=<file>          Output Java file");
        Print.sysPrintln("  -overwrite=true         Overwrite output file, if it exists");
        System.exit(1);
    }
    
    /**
    *** Main entry point for testing/debugging
    *** @param argv Comand-line arguments
    **/
    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        
        /* help */
        if (RTConfig.hasProperty(ARG_HELP)) {
            _usage();
        }
        
        /* args */
        String  templateName = StringTools.trim(RTConfig.getString(ARG_OPTIONAL,RTConfig.getString(ARG_TEMPLATE,"")));
        String  packageName  = StringTools.trim(RTConfig.getString(ARG_PACKAGE, null));
        File    outputFile   = RTConfig.getFile(ARG_OUTPUT, null);
        boolean overwrite    = RTConfig.getBoolean(ARG_OVERWRITE, false);

        /* set current time (subject to change) */
        String tzStr = RTConfig.getString("timezone",null);
        TimeZone tz  = !StringTools.isBlank(tzStr)? DateTime.getTimeZone(tzStr) : DateTime.getDefaultTimeZone();
        DateTime now = new DateTime(tz);
        if (!RTConfig.hasProperty("timetamp"))  { RTConfig.setLong  ("timestamp", now.getTimeSec()); }
        if (!RTConfig.hasProperty("datetime"))  { RTConfig.setString("datetime" , now.format("yyyy/MM/dd HH:mm:ss z")); }
        if (!RTConfig.hasProperty("date"    ))  { RTConfig.setString("date"     , now.format("yyyy/MM/dd")); }
        if (!RTConfig.hasProperty("time"    ))  { RTConfig.setString("time"     , now.format("HH:mm:ss")); }
        if (!RTConfig.hasProperty("timezone"))  { RTConfig.setString("timezone" , now.format("z")); }

        /* precheck output file */
        if ((outputFile != null) && outputFile.exists()) {
            if (!overwrite) {
                Print.errPrintln("Output file exists and overwrite not specified.");
                _usage();
            } else
            if (!outputFile.isFile()) {
                Print.errPrintln("Overwrite specified, but specified existing output is not a file.");
                System.exit(1);
            }
        }

        /* adjust packageName */
        if (packageName.equals(JAVA_PACKAGE)) {
            Print.errPrintln("Warning: 'package' argument cannot equal \"package\" (setting to empty string).");
            packageName = "";
        }

        /* get template text */
        String templateText = CompiletimeVars.getTemplate(templateName, RTConfig.hasProperty(ARG_OPTIONAL), packageName);
        if (StringTools.isBlank(templateText)) {
            _usage();
        }

        /* replace runtime vars in text */
        String outputText = RTConfig.insertKeyValues(templateText, STR_DELIM, END_DELIM, DFT_DELIM);
        
        /* write output */
        if (outputFile != null) {
            try {
                boolean didWrite = FileTools.writeFile(outputText.getBytes(), outputFile);
                if (!didWrite) {
                    Print.errPrintln("Unable to write output file.");
                    System.exit(1);
                }
            } catch (IOException ioe) {
                Print.errPrintln("Unable to write output file.");
                System.exit(1);
            }
        } else {
            Print.sysPrintln(outputText);
        }
        
        /* exit successful */
        System.exit(0);

    }
    
}
