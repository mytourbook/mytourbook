/*******************************************************************************
 * Copyright (c) 2007 Pascal Essiembre.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Pascal Essiembre - initial API and implementation
 ******************************************************************************/
package org.eclipse.babel.core.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

/**
 * Utility methods of all kinds used across the Babel API. 
 * @author Pascal Essiembre
 */
public final class BabelUtils {

	//TODO find a better sport for these methods?
	
    public static final String[] EMPTY_STRINGS = new String[] {};
    
    /**
     * Constructor.
     */
    private BabelUtils() {
        super();
    }

    /**
     * Null-safe testing of two objects for equality.
     * @param o1 object 1
     * @param o2 object 2
     * @return <code>true</code> if to objects are equal or if they are both
     * <code>null</code>.
     */
    public static boolean equals(Object o1, Object o2) {
        return (o1 == null && o2 == null || o1 != null && o1.equals(o2));
    }

    /**
     * Joins an array by the given separator.
     * @param array the array to join
     * @param separator the joining separator
     * @return joined string
     */
    public static String join(Object[] array, String separator) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < array.length; i++) {
            Object item = array[i];
            if (item != null) {
                if (buf.length() > 0) {
                    buf.append(separator);
                }
                buf.append(item);
            }
        }
        return buf.toString();
    }

    
    /**
     * Parses a string into a locale.  The string is expected to be of the
     * same format of the string obtained by calling Locale.toString().
     * @param localeString string representation of a locale
     * @return a locale or <code>null</code> if string is empty or null
     */
    public static Locale parseLocale(String localeString) {
        if (localeString == null || localeString.trim().length() == 0) {
            return null;
        }
        StringTokenizer tokens = 
            new StringTokenizer(localeString, "_"); //$NON-NLS-1$
        List<String> localeSections = new ArrayList<String>();
        while (tokens.hasMoreTokens()) {
            localeSections.add(tokens.nextToken());
        }
        Locale locale = null;
        switch (localeSections.size()) {
        case 1:
            locale = new Locale(localeSections.get(0));
            break;
        case 2:
            locale = new Locale(
                    localeSections.get(0),
                    localeSections.get(1));
            break;
        case 3:
            locale = new Locale(
                    localeSections.get(0),
                    localeSections.get(1),
                    localeSections.get(2));
            break;
        default:
            break;
        }
        return locale;
    }
}
