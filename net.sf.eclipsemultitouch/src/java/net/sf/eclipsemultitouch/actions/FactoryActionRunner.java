// <--------------------------------------------------------------------------------------------> //
// File name:       $RCSfile: FactoryActionRunner.java $
// First created:   $Created: Dec 17, 2009 by mirko $
// Last modified:   $Date$ by $Author$
// Version:         $Revision$
//
// Copyright (c) 2009 Mirko Raner.
// All rights reserved. This program and the accompanying materials are made available under the
// terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available
// at http://www.eclipse.org/legal/epl-v10.html

package net.sf.eclipsemultitouch.actions;

import net.sf.eclipsemultitouch.operations.OperationRunner;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

/**
* The class {@link FactoryActionRunner} executes actions that can be created from an
* {@link ActionFactory}. Client code only needs to supply the factory, and the action will be
* automatically created and executed.
*
* @author Mirko Raner
* @version $Revision: $
**/
public class FactoryActionRunner extends OperationRunner<ActionFactory> implements Runnable
{
    private ActionFactory actionFactory;

    private FactoryActionRunner(ActionFactory actionFactory)
    {
        this.actionFactory = actionFactory;
    }

    /**
    * Creates a new, generic {@link FactoryActionRunner} instance that is suitable for being passed
    * to the {@link OperationRunner}'s static {@code run(...)} method.
    **/
    public FactoryActionRunner()
    {
        super();
    }

    /**
    * Runs the {@link IWorkbenchAction} produced by the given {@link ActionFactory}.
    *
    * @param factory the {@link ActionFactory}
    **/
    protected void run(ActionFactory factory)
    {
        runFactoryAction(factory);
    }

    /**
    * Run an action that originates from an {@link ActionFactory}. Actions are always executed in
    * the Eclipse UI thread.
    *
    * @param actionFactory the {@link ActionFactory} (for example,
    * {@link ActionFactory#PREVIOUS_EDITOR})
    **/
    public static void runFactoryAction(ActionFactory actionFactory)
    {
        FactoryActionRunner runner = new FactoryActionRunner(actionFactory);
        Display.getDefault().asyncExec(runner);
    }

    /**
    * Creates and executes and action in the Eclipse UI thread.
    *
    * @see Runnable#run()
    * @see Display#asyncExec(Runnable)
    * @see #runFactoryAction(ActionFactory)
    **/
    public void run()
    {
        IWorkbenchWindow activeWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        IWorkbenchAction action = actionFactory.create(activeWindow);
        action.run();
    }
}
