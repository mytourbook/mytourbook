/*
 *  File: DateChooserListener.java 
 *  Copyright (c) 2004-2007  Peter Kliem (Peter.Kliem@jaret.de)
 *  A commercial license is available, see http://www.jaret.de.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 */
package de.jaret.util.ui.datechooser;

import java.util.Date;

/**
 * Interface for listening for changes on a DateChooserPanel. The interface indicates intermediate changes,
 * the definitive selection of a date and the cancelation of the choosing activity.
 * 
 * @author Peter Kliem
 * @version $Id: IDateChooserListener.java 587 2007-10-14 12:32:10Z olk $
 */
public interface IDateChooserListener {
    /**
     * Called when the user selected a date.
     * @param date chosen date
     */
    void dateChosen(Date date);

    /**
     * Called on intermediate changes in the date chooser.
     * @param date current date selection
     */
    void dateIntermediateChange(Date date);

    /**
     * Called when the user decided not to choose any date.
     *
     */
    void choosingCanceled();
    
    /**
     * Called when the user input becomes invalid.
     */
    void inputInvalid();
}
