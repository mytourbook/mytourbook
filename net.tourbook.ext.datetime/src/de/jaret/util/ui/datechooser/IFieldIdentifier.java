/*
 *  File: FieldIdentifier.java 
 *  Copyright (c) 2004-2007  Peter Kliem (Peter.Kliem@jaret.de)
 *  A commercial license is available, see http://www.jaret.de.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 */
package de.jaret.util.ui.datechooser;

/**
 * This interface defines a utility identifying the field (DAY_OF_MONTH, MONTH or YEAR) for a position in a text string.
 * 
 * @author Peter Kliem
 * @version $Id: IFieldIdentifier.java 498 2007-06-18 22:14:29Z olk $
 */
public interface IFieldIdentifier {
    /**
     * Identify the field in a date string.
     * 
     * @param dateString String representation of a date.
     * @param pos index in the String to identify.
     * @return -1 if no field could be identified; one of the Calendar constants: DAY_OF_MONTH, YEAR, MONTH
     */
    int getField(String dateString, int pos);
}
