// <--------------------------------------------------------------------------------------------> //
// File name:       $RCSfile: MultiTouchOperationValidator.java $
// First created:   $Created: Jan 5, 2010 by mirko $
// Last modified:   $Date$ by $Author$
// Version:         $Revision$
//
// Copyright (c) 2010 Mirko Raner.
// All rights reserved. This program and the accompanying materials are made available under the
// terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available
// at http://www.eclipse.org/legal/epl-v10.html

package net.sf.eclipsemultitouch;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import net.sf.eclipsemultitouch.operations.OperationValidator;

/**
* The class {@link MultiTouchOperationValidator} is an {@link OperationValidator} whose
* {@link #isValid()} method returns {@code true} if Eclipse is the currently active application
* and the active window is an Eclipse workbench window. This prevents operations from executing
* when Eclipse is currently in the background (for details see
* <a href="http://sf.net/support/tracker.php?aid=2926080">bug 2926080</a>).
*
* @author Mirko Raner
* @version $Revision: $
**/
public class MultiTouchOperationValidator implements OperationValidator, Runnable
{
    private boolean valid;

    /**
    * @see net.sf.eclipsemultitouch.operations.OperationValidator#isValid()
    **/
    public boolean isValid()
    {
        MultiTouchOperationValidator validator = new MultiTouchOperationValidator();
        Display.getDefault().syncExec(validator);
        return validator.valid;
    }

    /**
    * Executes the part of the validator that must run in the SWT UI thread.
    *
    * @see Display#syncExec(Runnable)
    * @see Runnable#run()
    **/
    public void run()
    {
        Shell activeShell = Display.getDefault().getActiveShell();
        if (activeShell != null)
        {
            IWorkbenchWindow activeWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            valid = activeWindow != null && activeShell.equals(activeWindow.getShell());
        }
    }
}
