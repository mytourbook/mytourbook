/*
 *  File: SimpleFieldIdentifier.java 
 *  Copyright (c) 2004-2007  Peter Kliem (Peter.Kliem@jaret.de)
 *  A commercial license is available, see http://www.jaret.de.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 */
package de.jaret.util.ui.datechooser;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Simple implementation of the FieldIdentifier interface for the jaret datechooser.
 * 
 * @author Peter Kliem
 * @version $Id: SimpleFieldIdentifier.java 498 2007-06-18 22:14:29Z olk $
 */
public class SimpleFieldIdentifier implements IFieldIdentifier {
    /** default separators. */
    protected String _separators = "/.";

    /** field mapping. */
    protected int[] _fields;

    /**
     * Construct a new instance. To identfy fields in a date like "dd.mm.yyyy" you would use it like that:
     * <pre>
     * FieldIdentifier fi = new SimpleFieldIdentifier(".", new int[] {Calendar.DAY_OF_MONTH, Calendar.MONTH, Calendar.YEAR});
     * </pre>
     * 
     * @param separators characters used as field seperators. Default is "./" (used if the argument is <code>null</code>.
     * @param fields array of field identifying int values. Parsing will only be succesful if the number of fields is matched exactly.
     */
    public SimpleFieldIdentifier(String separators, int[] fields) {
        if (separators != null) {
            _separators = separators;
        }
        _fields = fields;
    }

    /**
     * See {@link #SimpleFieldIdentifier(String, int[])}.
     * Default separators will be used.
     * 
     * @param fields field mappings
     */
    public SimpleFieldIdentifier(int[] fields) {
        this(null, fields);
    }

    /**
     * {@inheritDoc}
     */
    public int getField(String dateString, int pos) {
        List<String> fields = new ArrayList<String>(5);
        StringTokenizer tokenizer = new StringTokenizer(dateString, _separators);
        while (tokenizer.hasMoreTokens()) {
            fields.add(tokenizer.nextToken());
        }
        if (fields.size() != _fields.length) {
            return -1;
        }
        int p = 0;
        int idx = 0;
        for (String field : fields) {
            p += field.length();
            if (p >= pos) {
                return _fields[idx];
            }
            p++; // seperator
            idx++;
        }

        return -1;
    }

}
