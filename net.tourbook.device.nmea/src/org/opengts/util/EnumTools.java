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
//  This class provides Enum based utilities.
// ----------------------------------------------------------------------------
// Change History:
//  2008/05/20  Martin D. Flynn
//     -Initial release
//  2008/06/20  Martin D. Flynn
//     -Added additional methods to support bitmask type values.
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.lang.reflect.*;
import java.util.*;

/**
*** Contains tools for custom enumerated types.
**/

public class EnumTools
{

    // ------------------------------------------------------------------------

    /**
    *** StringLocale interface
    **/
    public interface StringLocale
    {
        public String toString(Locale loc);
    }

    /**
    *** StringValue interface
    **/
    public interface StringValue
    {
        public String getStringValue();
    }

    /**
    *** DoubleValue interface
    **/
    public interface DoubleValue
    {
        public double getDoubleValue();
    }

    /**
    *** IntValue interface
    **/
    public interface IntValue
    {
        public int getIntValue();
    }

    /**
    *** LongValue interface
    **/
    public interface LongValue
    {
        public long getLongValue();
    }

    /**
    *** BitMask interface
    **/
    public interface BitMask
        extends LongValue
    {
        // 
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a localized String array of enumerated type 'toString()' values for the specified Enum type class
    *** @param enumClass The enumerated type class
    *** @param list      The array of enumerated types from which the localized Strings will be extracted
    *** @param loc       The Locale
    *** @return A String array of enumerated type String values
    **/
    public static <T extends Enum<T>> Map<String,String> getValueMap(Class<T> enumClass, T list[], Locale loc)
    {
        OrderedMap<String,String> m = new OrderedMap<String,String>();
        if (enumClass != null) {
            Enum e[] = list;
            if (e != null) {
                for (int n = 0; n < e.length; n++) {
                    String k = e[n].name(); // toString();
                    String d = k;
                    if ((loc != null) && (e[n] instanceof EnumTools.StringLocale)) {
                        d = ((EnumTools.StringLocale)e[n]).toString(loc);
                    }
                    //Print.logInfo("Enum: %s/%s", k, d);
                    m.put(k, d);
                }
            }
        }
        return m;
    }

    /**
    *** Returns a localized String array of enumerated type 'toString()' values for the specified Enum type class
    *** @param enumClass The enumerated type class
    *** @param loc       The Locale
    *** @return A String array of enumerated type String values
    **/
    public static <T extends Enum<T>> Map<String,String> getValueMap(Class<T> enumClass, Locale loc)
    {
        OrderedMap<String,String> m = new OrderedMap<String,String>();
        if (enumClass != null) {
            Enum e[] = enumClass.getEnumConstants();
            if (e != null) {
                for (int n = 0; n < e.length; n++) {
                    String k = e[n].name();
                    String d = k;
                    if ((loc != null) && (e[n] instanceof EnumTools.StringLocale)) {
                        d = ((EnumTools.StringLocale)e[n]).toString(loc);
                    }
                    //Print.logInfo("Enum: %s/%s", k, d);
                    m.put(k, d);
                }
            }
        }
        return m;
    }

    /**
    *** Returns a String array of enumerated type 'toString()' values for the specified Enum type class
    *** @param enumClass The enumerated type class
    *** @return A String array of enumerated type String values
    **/
    public static <T extends Enum<T>> Map<String,String> getValueMap(Class<T> enumClass)
    {
        return EnumTools.getValueMap(enumClass, null);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a localized String array of enumerated type 'toString()' values for the specified Enum type class
    *** @param enumClass The enumerated type class
    *** @param list      The array of enumerated types from which the localized Strings will be extracted
    *** @param loc       The Locale
    *** @return A String array of enumerated type String values
    **/
    public static <T extends Enum<T>> String[] getValueNames(Class<T> enumClass, T list[], Locale loc)
    {
        if (enumClass != null) {
            Enum e[] = list;
            if (e != null) {
                String s[] = new String[e.length];
                for (int n = 0; n < e.length; n++) {
                    if ((loc != null) && (e[n] instanceof EnumTools.StringLocale)) {
                        s[n] = ((EnumTools.StringLocale)e[n]).toString(loc);
                    } else {
                        s[n] = e[n].toString();
                    }
                }
                return s;
            }
        }
        return new String[0];
    }

    /**
    *** Returns a localized String array of enumerated type 'toString()' values for the specified Enum type class
    *** @param enumClass The enumerated type class
    *** @param loc       The Locale
    *** @return A String array of enumerated type String values
    **/
    public static <T extends Enum<T>> String[] getValueNames(Class<T> enumClass, Locale loc)
    {
        if (enumClass != null) {
            Enum e[] = enumClass.getEnumConstants();
            if (e != null) {
                String s[] = new String[e.length];
                for (int n = 0; n < e.length; n++) {
                    if ((loc != null) && (e[n] instanceof EnumTools.StringLocale)) {
                        s[n] = ((EnumTools.StringLocale)e[n]).toString(loc);
                    } else {
                        s[n] = e[n].toString();
                    }
                }
                return s;
            }
        }
        return new String[0];
    }

    /**
    *** Returns a String array of enumerated type 'toString()' values for the specified Enum type class
    *** @param enumClass The enumerated type class
    *** @return A String array of enumerated type String values
    **/
    public static <T extends Enum<T>> String[] getValueNames(Class<T> enumClass)
    {
        return EnumTools.getValueNames(enumClass, null);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the default enum constant of the specified enum type.  Currently, the 'default' constant
    *** is defined to be the first constant in the list.
    *** @param enumClass The <tt>Class</tt> object of the enum type from which to return a constant
    *** @return The default enum constant of the specified enum type.
    **/
    public static <T extends Enum<T>> T getDefault(Class<T> enumClass)
    {
        if (enumClass != null) {
            T e[] = enumClass.getEnumConstants();
            if ((e != null) && (e.length > 0)) {
                return e[0];
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the enum constant of the specified enum type with the specified name or 'toString()' value.
    *** @param enumClass The <tt>Class</tt> object of the enum type from which to return a constant
    *** @param name      The name of the constant to return
    *** @param dft       The default type to return if the name was not found
    *** @return The enum constant of the specified enum type with the specified name or 'toString()' value
    **/
    public static <T extends Enum<T>> T getValueOf(Class<T> enumClass, String name, T dft)
    {
        return EnumTools.getValueOf(enumClass, name, null, dft, false);
    }

    /**
    *** Returns the enum constant of the specified enum type with the specified name or 'toString()' value.
    *** The default (first) constant will be returned, if the name is not found.
    *** @param enumClass The <tt>Class</tt> object of the enum type from which to return a constant
    *** @param name      The name of the constant to return
    *** @return The enum constant of the specified enum type with the specified name or 'toString()' value
    **/
    public static <T extends Enum<T>> T getValueOf(Class<T> enumClass, String name)
    {
        return EnumTools.getValueOf(enumClass, name, null, null, true);
    }

    /**
    *** Returns the enum constant of the specified enum type with the specified name or 'toString()' value.
    *** The default (first) constant will be returned, if the name is not found.
    *** @param enumClass The <tt>Class</tt> object of the enum type from which to return a constant
    *** @param name      The name of the constant to return
    *** @param loc       The Locale
    *** @return The enum constant of the specified enum type with the specified name or 'toString()' value
    **/
    public static <T extends Enum<T>> T getValueOf(Class<T> enumClass, String name, Locale loc)
    {
        return EnumTools.getValueOf(enumClass, name, loc, null, true);
    }

    /**
    *** Returns the enum constant of the specified enum type with the specified name or 'toString()' value.
    *** @param enumClass  The <tt>Class</tt> object of the enum type from which to return a constant
    *** @param name       The name of the constant to return
    *** @param loc        The Locale
    *** @param dft        The default type to return if the name was not found, and 'rtnDefault' is false.
    *** @param rtnDefault True to return the first constant if name was not found and 'dft' is null.
    *** @return The enum constant of the specified enum type with the specified name or 'toString()' value
    **/
    protected static <T extends Enum<T>> T getValueOf(Class<T> enumClass, String name, Locale loc, T dft, boolean rtnDefault)
    {
        if ((enumClass != null) && (name != null)) {
            name = StringTools.trim(name); // remove leading/trailing space

            /* first try 'Enum.valueOf' [this checks for an exact match on 'name()'] */
            try {
                return Enum.valueOf(enumClass, name); // does not return null
            } catch (Throwable th) {
                // not found, continue
            }

            /* now scan for a case-insensitive match on 'toString()' */
            T e[] = enumClass.getEnumConstants();
            if ((e != null) && (e.length > 0)) {

                /* search */
                for (int n = 0; n < e.length; n++) {
                    if (e[n].toString().equalsIgnoreCase(name)) {
                        return e[n];
                    } else
                    if ((e[n] instanceof StringValue) && (((StringValue)e[n]).getStringValue().equalsIgnoreCase(name))) {
                        return e[n];
                    } else
                    if ((loc != null) && (e[n] instanceof StringLocale) && ((StringLocale)e[n]).toString(loc).equalsIgnoreCase(name)) {
                        return e[n];
                    } else
                    if ((e[n] instanceof IntValue) && (((IntValue)e[n]).getIntValue() == StringTools.parseInt(name,Integer.MIN_VALUE))) {
                        return e[n];
                    } else
                    if ((e[n] instanceof LongValue) && (((LongValue)e[n]).getLongValue() == StringTools.parseLong(name,Long.MIN_VALUE))) {
                        return e[n];
                    }
                }

                /* not found, return default? */
                if (rtnDefault && (dft == null)) {
                    // return the default constant
                    return EnumTools.getDefault(enumClass);
                } else {
                    return dft;
                }

            } else {
                // not an Enum class
                return dft;
            }

        } else {
            // invalid class/name
            return dft;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the enum constant of the specified enum type with the specified integer value.
    *** @param enumClass The <tt>Class</tt> object of the enum type from which to return a constant
    *** @param value     The integer value of the constant to return
    *** @param dft       The default type to return if the integer value was not found, and 'rtnDefault' is false.
    *** @return The enum constant of the specified enum type with the specified integer value
    **/
    public static <T extends Enum<T>> T getValueOf(Class<T> enumClass, int value, T dft)
    {
        return EnumTools.getValueOf(enumClass, value, dft, false);
    }

    /**
    *** Returns the enum constant of the specified enum type with the specified integer value.
    *** The default (first) constant will be returned, if the name is not found.
    *** @param enumClass The <tt>Class</tt> object of the enum type from which to return a constant
    *** @param value     The integer value of the constant to return
    *** @return The enum constant of the specified enum type with the specified integer value
    **/
    public static <T extends Enum<T>> T getValueOf(Class<T> enumClass, int value)
    {
        return EnumTools.getValueOf(enumClass, value, null, true);
    }

    /**
    *** Returns the enum constant of the specified enum type with the specified integer value.
    *** @param enumClass  The <tt>Class</tt> object of the enum type from which to return a constant
    *** @param value      The integer value of the constant to return
    *** @param dft        The default type to return if the integer value was not found, and 'rtnDefault' is false.
    *** @param rtnDefault True to return the first constant if integer value was not found and 'dft' is null.
    *** @return The enum constant of the specified enum type with the specified integer value
    **/
    public static <T extends Enum<T>> T getValueOf(Class<T> enumClass, int value, T dft, boolean rtnDefault)
    {
        if (enumClass != null) {

            /* now scan for a case-insensitive match on 'toString()' */
            T e[] = enumClass.getEnumConstants();
            if ((e != null) && (e.length > 0) && 
                ((e[0] instanceof IntValue) || (e[0] instanceof LongValue))) {

                /* search */
                for (int n = 0; n < e.length; n++) {
                    if ((e[0] instanceof IntValue) && (((IntValue)e[n]).getIntValue() == value)) {
                        return e[n];
                    } else
                    if ((e[0] instanceof LongValue) && (((LongValue)e[n]).getLongValue() == value)) {
                        return e[n];
                    }
                }

                /* not found, return default? */
                if (rtnDefault && (dft == null)) {
                    // return the default constant
                    return EnumTools.getDefault(enumClass);
                } else {
                    return dft;
                }

            } else {
                // not an Enum class, or does not implement 'IntValue'
                return dft;
            }

        } else {
            // invalid class/name
            return dft;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the specified enum constant or a default if the enum constant is null.
    *** @param enumClass The <tt>Class</tt> object of the enum type from which to return a constant
    *** @param value      The enum type value to return
    *** @param dft       The default type to return if the integer value was not found, and 'rtnDefault' is false.
    *** @return The enum constant of the specified enum type
    **/
    public static <T extends Enum<T>> T getValueOf(Class<T> enumClass, T value, T dft)
    {
        return EnumTools.getValueOf(enumClass, value, dft, false);
    }

    /**
    *** Returns the enum constant of the specified enum type with the specified integer value.
    *** The default (first) constant will be returned, if the specified value is null.
    *** @param enumClass The <tt>Class</tt> object of the enum type from which to return a constant
    *** @param value      The enum type value to return
    *** @return The enum constant of the specified enum type
    **/
    public static <T extends Enum<T>> T getValueOf(Class<T> enumClass, T value)
    {
        return EnumTools.getValueOf(enumClass, value, null, true);
    }

    /**
    *** Returns the specified enum constant or a default if the enum constant is null.
    *** @param enumClass  The <tt>Class</tt> object of the enum type from which to return a constant
    *** @param value      The enum type value to return
    *** @param dft        The default type to return if the enum type value is null, and 'rtnDefault' is false.
    *** @param rtnDefault True to return the first constant if the enum type value was not found and 'dft' is null.
    *** @return The enum constant of the specified enum type
    **/
    public static <T extends Enum<T>> T getValueOf(Class<T> enumClass, T value, T dft, boolean rtnDefault)
    {

        /* enum value defined? */
        if (value != null) {
            return value;
        }

        /* return a default value */
        if ((enumClass != null) && rtnDefault && (dft == null)) {
            // return the default constant
            return EnumTools.getDefault(enumClass);
        } else {
            return dft;
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns a list of Enumerated BitMask values for the specified mask
    *** @param enumClass The <tt>Class</tt> object of the enum type from which to return constants
    *** @param mask      The bitmask
    *** @return A list of enumerated bitmask types, or null if the Enum class type is not a bitmask
    **/
    public static <T extends Enum<T>> T[] getValuesForMask(Class<T> enumClass, long mask)
    {
        if ((enumClass != null) && EnumTools.BitMask.class.isAssignableFrom(enumClass)) {
            java.util.List<T> list = new Vector<T>();
            T e[] = enumClass.getEnumConstants();
            if (e != null) {
                for (int n = 0; n < e.length; n++) {
                    long val = ((EnumTools.BitMask)e[n]).getLongValue();
                    if (val == 0L) {
                        if (mask == 0L) {
                            list.add(e[n]);
                        }
                    } else
                    if ((val & mask) == val) {
                        list.add(e[n]);
                    }
                }
            }
            return ListTools.toArray(list, enumClass);
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** For 'BitMask' enumerated types, this method returns the logical 'OR' or all values.
    *** @param enumClass  The <tt>Class</tt> object of the enum type from which to return the bitmask
    *** @return The logical 'OR" of all enumerated types for the specified Enum class
    **/
    public static <T extends Enum<T>> long getValueMask(Class<T> enumClass)
    {
        if ((enumClass != null) && EnumTools.BitMask.class.isAssignableFrom(enumClass)) {
            long mask = 0L;
            Enum e[] = enumClass.getEnumConstants();
            if (e != null) {
                for (int n = 0; n < e.length; n++) {
                    mask |= ((EnumTools.BitMask)e[n]).getLongValue();
                }
            }
            return mask;
        } else {
            return -1L; // all bits are set
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static Map<String,Class<? extends Enum>> enumRegistry = null;
    
    /**
    *** Registers all public Enum classes defined within the specified class.<br>
    *** This is used to allow referencing an enumerated type by name (ie. for the
    *** purpose of obtaining a localized list of String representations).
    *** @param enumContainerClass  The class which defined Enum inner classes
    **/
    @SuppressWarnings("unchecked")
    public static void registerPublicEnumClasses(Class<?> enumContainerClass)
    {
        if (enumContainerClass != null) {
            if (enumContainerClass.isEnum()) {
                EnumTools.registerEnumClass((Class<? extends Enum>)enumContainerClass);
            } else {
                Class classList[] = enumContainerClass.getClasses();
                for (int i = 0; i < classList.length; i++) {
                    if (classList[i].isEnum()) {
                        EnumTools.registerEnumClass((Class<? extends Enum>)classList[i]);
                    }
                }
            }
        }
    }

    /**
    *** Registers the specified Enum classes
    *** @param enumClass  The Enum class to register.  The default name used in the registration
    ***                   will be the name of the class with the package name removed.
    **/
    public static <T extends Enum> void registerEnumClass(Class<T> enumClass)
    {
        EnumTools.registerEnumClass(null, enumClass);
    }

    /**
    *** Registers the specified Enum classes
    *** @param name       The name under which the Enum class will be registered
    *** @param enumClass  The Enum class to register.
    **/
    public static <T extends Enum> void registerEnumClass(String name, Class<T> enumClass)
    {
        if ((enumClass != null) && enumClass.isEnum()) {
            
            /* init registry */
            if (enumRegistry == null) {
                enumRegistry = new HashMap<String,Class<? extends Enum>>();
            }
            
            /* default name */
            if (StringTools.isBlank(name)) {
                String cn = enumClass.getName();
                int p = cn.lastIndexOf(".");
                name = (p >= 0)? cn.substring(p+1) : cn;
            }
            
            /* save */
            if (enumRegistry.containsKey(name)) {
                Class<? extends Enum> eclz = enumRegistry.get(name);
                if ((eclz != null) && !eclz.equals(enumClass)) {
                    Print.logStackTrace("Duplicate registered Enum names: "+name+" ==> ("+eclz.getName()+" != "+enumClass.getName()+")");
                }
            } else {
                //Print.logInfo("Registering Enum class: %s => %s", name, StringTools.className(enumClass));
                enumRegistry.put(name,enumClass);
            }
        
        }
    }
    
    /**
    *** Returns the registered Enum class for the specified name
    *** @param name  The register Enum class name
    *** @return The registered Enum class for the specified name, or null if the name does not exist.
    **/
    public static Class<? extends Enum> getEnumClass(String name)
    {
        return EnumTools.getEnumClass(null, name);
    }
    
    /**
    *** Returns the registered Enum class for the specified name
    *** @param enumContainerClass   The parent class in which the specified name is defined
    *** @param name                 The register Enum class name
    *** @return The registered Enum class for the specified name, or null if the name does not exist.
    **/
    public static Class<? extends Enum> getEnumClass(Class enumContainerClass, String name)
    {
        if ((enumRegistry != null) && (name != null)) {
            // first try name "EnumContainer$EnumType"
            if (enumContainerClass != null) {
                String eccn = StringTools.className(enumContainerClass);
                int p = eccn.lastIndexOf(".");
                String n = ((p >= 0)? eccn.substring(p+1) : eccn) + "$" + name;
                if (enumRegistry.containsKey(n)) {
                    return enumRegistry.get(n);
                }
            }
            // then try name "EnumType"
            if (enumRegistry.containsKey(name)) {
                return enumRegistry.get(name);
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    public enum TestEnum implements StringLocale, IntValue {
        ONE     ( 1, I18N._getString(EnumTools.class,"EnumTools.type.one"  ,"One"  )),
        TWO     ( 2, I18N._getString(EnumTools.class,"EnumTools.type.two"  ,"Two"  )),
        THREE   ( 3, I18N._getString(EnumTools.class,"EnumTools.type.three","Three")),
        FOUR    ( 4, I18N._getString(EnumTools.class,"EnumTools.type.four" ,"Four" )),
        FIVE    ( 5, I18N._getString(EnumTools.class,"EnumTools.type.five" ,"Five" ));
        private int         vv = 0;
        private I18N.Text   aa = null;
        TestEnum(int v, I18N.Text a)            { vv = v; aa = a; }
        public int     getIntValue()            { return vv; }
        public String  toString()               { return aa.toString(); }
        public String  toString(Locale loc)     { return aa.toString(loc); }
    };
    
    @SuppressWarnings("unchecked")
    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        
        Print.logInfo("Referencing TestEnum.ONE ...");
        EnumTools.registerPublicEnumClasses(EnumTools.class);
        //TestEnum x = TestEnum.ONE;
        Print.logInfo("Done referencing TestEnum.ONE ...");

        /* enum class */
        String enumName = RTConfig.getString("enum",StringTools.className(TestEnum.class));
        Class<? extends Enum> enumClass = EnumTools.getEnumClass(enumName);
        if (enumClass != null) {
            Print.logInfo("Found registered enum class: " + StringTools.className(enumClass));
        } else {
            try {
                enumClass = (Class<? extends Enum>)Class.forName(enumName);
                Print.logInfo("Found Enum Class.forName: " + enumName);
            } catch (Throwable th) {
                Print.logException("Unable to locate Enum Class.forName: " + enumName, th);
                System.exit(1);
            }
        }

        /* list */
        Enum e[] = enumClass.getEnumConstants();
        if (e != null) {
            for (int n = 0; n < e.length; n++) {
                StringBuffer sb = new StringBuffer();
                sb.append(e[n].ordinal()).append(": ");
                sb.append(e[n].toString());
                if (e[n] instanceof IntValue) {
                    sb.append(" [").append(((IntValue)e[n]).getIntValue()).append("]");
                }
                Print.sysPrintln(sb.toString());
            }
        } else {
            Print.logError("Not an Enum type? " + StringTools.className(enumClass));
        }

    }

}