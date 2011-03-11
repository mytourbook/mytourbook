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
package org.eclipse.babel.core.message.checks.proximity;


/**
 * Compares two strings (case insensitive) and returns a proximity level based
 * on the number of character transformation required to have identical strings.
 * Non-string objects are converted to strings using the <code>toString()</code>
 * method. The exact algorithm was taken from Micheal Gilleland
 * (<a href="http://merriampark.com/ld.htm">http://merriampark.com/ld.htm</a>).
 * @author Pascal Essiembre (pascal@essiembre.com)
 */
public class LevenshteinDistanceAnalyzer implements IProximityAnalyzer {

    private static final IProximityAnalyzer INSTANCE =
            new LevenshteinDistanceAnalyzer();
    
    /**
     * Constructor.
     */
    private LevenshteinDistanceAnalyzer() {
        //TODO add case sensitivity?
        super();
    }

    /**
     * Gets the unique instance.
     * @return a proximity analyzer
     */
    public static IProximityAnalyzer getInstance() {
        return INSTANCE;
    }
    
    /**
     * @see com.essiembre.eclipse.rbe.model.utils.IProximityAnalyzer
     *      #analyse(java.lang.Object, java.lang.Object)
     */
   public double analyse(String str1, String str2) {
        int maxLength = Math.max(str1.length(), str2.length());
        double distance = distance(str1, str2);

        return 1d - (distance / maxLength);
    }
   
   
    /**
     * Retuns the minimum of three values.
     * @param a first value
     * @param b second value
     * @param c third value
     * @return lowest value
     */
    private int minimum(int a, int b, int c) {
        int mi;

        mi = a;
        if (b < mi) {
            mi = b;
        }
        if (c < mi) {
            mi = c;
        }
        return mi;

    }

    /***
     * Compute the distance
     * @param s source string
     * @param t target string
     * @return distance
     */
    public int distance(String s, String t) {
        int d[][]; // matrix
        int n; // length of s
        int m; // length of t
        int i; // iterates through s
        int j; // iterates through t
        char s_i; // ith character of s
        char t_j; // jth character of t
        int cost; // cost

        // Step 1
        n = s.length();
        m = t.length();
        if (n == 0) {
            return m;
        }
        if (m == 0) {
            return n;
        }
        d = new int[n + 1][m + 1];

        // Step 2
        for (i = 0; i <= n; i++) {
            d[i][0] = i;
        }

        for (j = 0; j <= m; j++) {
            d[0][j] = j;
        }

        // Step 3
        for (i = 1; i <= n; i++) {
            s_i = s.charAt(i - 1);

            // Step 4
            for (j = 1; j <= m; j++) {
                t_j = t.charAt(j - 1);

                // Step 5
                if (s_i == t_j) {
                    cost = 0;
                } else {
                    cost = 1;
                }

                // Step 6
                d[i][j] = minimum(d[i - 1][j] + 1, d[i][j - 1] + 1,
                        d[i - 1][j - 1] + cost);
            }
        }

        // Step 7
        return d[n][m];
    }
}
