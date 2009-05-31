/*
 *  File: IAdditionalDayInformationProvider.java 
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
 * Interface for a service suplying additional information to a datechooser or datechooser panel. By implementing this
 * interface days can be rendered using a bold font and a tooltip for a day can be supplied. The service can be used in
 * addition to a HolidayEnumerator.
 * 
 * @author kliem
 * @version $Id: IAdditionalDayInformationProvider.java 574 2007-10-03 11:59:15Z olk $
 */
public interface IAdditionalDayInformationProvider {
    /**
     * Check whether a date should be rendered bold.
     * 
     * @param date date to check
     * @return <code>true</code> if the date should be rendered using a bold font
     */
    boolean renderBold(Date date);

    /**
     * Retrive a tooltip text for a day.
     * 
     * @param date the date
     * @return the tooltip or <code>null</code> to indicate no tooltip
     */
    String getToolTipText(Date date);
}
