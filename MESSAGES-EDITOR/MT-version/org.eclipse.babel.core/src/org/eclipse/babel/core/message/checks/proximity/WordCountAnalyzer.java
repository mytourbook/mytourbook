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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Compares two strings (case insensitive) and returns a proximity level
 * based on how many words there are, and how many words are the same 
 * in both strings.  Non-string objects are converted to strings using
 * the <code>toString()</code> method.
 * @author Pascal Essiembre (pascal@essiembre.com)
 */
public class WordCountAnalyzer implements IProximityAnalyzer {

    private static final IProximityAnalyzer INSTANCE = new WordCountAnalyzer();
    private static final String WORD_SPLIT_PATTERN =
            "\r\n|\r|\n|\\s"; //$NON-NLS-1$

    /**
     * Constructor.
     */
    private WordCountAnalyzer() {
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
     *         #analyse(java.lang.Object, java.lang.Object)
     */
    public double analyse(Object obj1, Object obj2) {
        if (obj2 == null || obj2 == null) {
            return 0;
        }
        
        Collection<String> str1 = new ArrayList<String>(
                Arrays.asList(obj1.toString().split(WORD_SPLIT_PATTERN)));
        Collection<String> str2 = new ArrayList<String>(
                Arrays.asList(obj2.toString().split(WORD_SPLIT_PATTERN)));
        
        int maxWords = Math.max(str1.size(), str2.size());
        if (maxWords == 0) {
            return 0;
        }
        
        int matchedWords = 0;
        for (String str : str1) {
            if (str2.remove(str)) {
                matchedWords++;
            }
        }

        return (double) matchedWords / (double) maxWords;
    }

}
