// <--------------------------------------------------------------------------------------------> //
// File name:       $RCSfile: CommandRunner.java $
// First created:   $Created: Dec 18, 2009 by mirko $
// Last modified:   $Date$ by $Author$
// Version:         $Revision$
//
// Copyright (c) 2009 Mirko Raner.
// All rights reserved. This program and the accompanying materials are made available under the
// terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available
// at http://www.eclipse.org/legal/epl-v10.html

package net.sf.eclipsemultitouch.utilities;

import net.sf.eclipsemultitouch.MultiTouchPlugin;
import net.sf.eclipsemultitouch.operations.OperationRunner;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.common.CommandException;
import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.EMPTY_MAP;

/**
* The class {@link CommandRunner} is a utility class for finding and executing Eclipse
* {@link Command}s based on their command ID.
*
* @author Mirko Raner
* @version $Revision: $
**/
public class CommandRunner extends OperationRunner<String> implements Runnable
{
    private final Command command;
    private final ExecutionEvent executionEvent;

    private CommandRunner(Command command, ExecutionEvent executionEvent)
    {
        this.command = command;
        this.executionEvent = executionEvent;
    }

    /**
    * Creates a new, generic {@link CommandRunner} instance that is suitable for being passed
    * to the {@link OperationRunner}'s static {@code run(...)} method.
    **/
    public CommandRunner()
    {
        command = null;
        executionEvent = null;
    }

    /**
    * Runs the {@link Command} specified by the given command ID.
    *
    * @param commandID the command ID
    **/
    protected void run(String commandID)
    {
        runCommand(commandID);
    }

    /**
    * Finds the {@link Command} with the given ID and executes it.
    *
    * @param commandID the unique ID of the {@link Command}
    **/
    public static void runCommand(String commandID)
    {
        IEvaluationContext context;
        ICommandService commandService;
        IHandlerService handlerService;
        IWorkbench workbench = PlatformUI.getWorkbench();
        commandService = (ICommandService)workbench.getAdapter(ICommandService.class);
        handlerService = (IHandlerService)workbench.getService(IHandlerService.class);
        context = new EvaluationContext(handlerService.getCurrentState(), EMPTY_LIST);
        Command command = commandService.getCommand(commandID);
        ExecutionEvent executionEvent = new ExecutionEvent(command, EMPTY_MAP, null, context);
        CommandRunner commandRunner = new CommandRunner(command, executionEvent);
        Display.getDefault().asyncExec(commandRunner);
    }

    /**
    * Asynchronously executes a {@link Command} in the Eclipse UI thread.
    *
    * @see Runnable#run()
    * @see Display#asyncExec(Runnable)
    * @see #runCommand(String)
    **/
    public void run()
    {
        try
        {
        	if (command.isHandled() && command.isEnabled())
        	{
        		command.executeWithChecks(executionEvent);
        	}
        }
        catch (CommandException exception)
        {
            ILog log = MultiTouchPlugin.getDefault().getLog();
            String info = "Unexpected exception while executing " + command; //$NON-NLS-1$
            IStatus status = new Status(IStatus.ERROR, MultiTouchPlugin.PLUGIN_ID, info, exception);
            log.log(status);
        }
    }
}
