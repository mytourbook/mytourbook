/*
 *  File: DateChoserAdapter.java 
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
 * Adapter class implementing the IDateChooserListener.
 * 
 * @author kliem
 * @version $Id: DateChooserAdapter.java 587 2007-10-14 12:32:10Z olk $
 */
public class DateChooserAdapter implements IDateChooserListener {

    /**
     * {@inheritDoc} Empty implementation.
     */
    public void choosingCanceled() {
    }

    /**
     * {@inheritDoc} Empty implementation.
     */
    public void dateChosen(Date date) {
    }

    /**
     * {@inheritDoc} Empty implementation.
     */
    public void dateIntermediateChange(Date date) {
    }

    /**
     * {@inheritDoc} Empty implementation.
     */
    public void inputInvalid() {
    }
}
