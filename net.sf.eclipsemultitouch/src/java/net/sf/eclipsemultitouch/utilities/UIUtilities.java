// <--------------------------------------------------------------------------------------------> //
// File name:       $RCSfile: UIUtilities.java $
// First created:   $Created: May 4, 2010 by mirko $
// Last modified:   $Date$ by $Author$
// Version:         $Revision$
//
// Copyright (c) 2010 Mirko Raner.
// All rights reserved. This program and the accompanying materials are made available under the
// terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available
// at http://www.eclipse.org/legal/epl-v10.html

package net.sf.eclipsemultitouch.utilities;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

/**
* The class {@link UIUtilities} provides utility methods for various operations that can only be
* performed from within the SWT UI thread.
*
* @param <$ReturnType$> the return type of the executed utility method (internal use only)
*
* @author Mirko Raner
* @version $Revision: $ $Change: $
**/
public class UIUtilities <$ReturnType$> implements Runnable
{
    private enum Method {getCursorLocationAndControl}

    private Method method;
    private Object[] argument;
    private $ReturnType$ result;

    private UIUtilities(Method method, Object... arguments)
    {
        this.method = method;
        this.argument = arguments;
    }

    /**
     * Gets the SWT {@link Control} that is currently located under the mouse pointer and the
     * mouse pointer's current coordinates relative to that {@link Control}.
     *
     * @param location a return parameter for the location (the {@link Control}-relative coordinates
     * will be written into this object); must not be {@code null}
     * @return the {@link Control} that is currently under the mouse pointer
     */
    public static Control getCursorLocationAndControl(Point location)
    {
        return new UIUtilities<Control>(Method.getCursorLocationAndControl, location).execute();
    }

    /**
    * Executes code in the SWT UI thread.
    *
    * @see Runnable#run()
    * @see Display#syncExec(Runnable)
    **/
    @SuppressWarnings("unchecked")
    public void run()
    {
        if (method == Method.getCursorLocationAndControl)
        {
            Display display = Display.getDefault();
            Control cursorControl = display.getCursorControl();
            if (cursorControl != null)
            {
                Point cursorLocation = display.getCursorLocation();
                Point relative = display.map(null, cursorControl, cursorLocation);
                ((Point)argument[0]).x = relative.x;
                ((Point)argument[0]).y = relative.y;
                result = ($ReturnType$)cursorControl;
            }
            return;
        }
        throw new UnsupportedOperationException(String.valueOf(method));
    }

    //-------------------------------------- PRIVATE SECTION -------------------------------------//

    private $ReturnType$ execute()
    {
        Display.getDefault().syncExec(this);
        return result;
    }
}
