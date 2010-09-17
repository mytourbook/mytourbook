// <--------------------------------------------------------------------------------------------> //
// File name:       $RCSfile: OperationRunner.java $
// First created:   $Created: Jan 5, 2010 by mirko $
// Last modified:   $Date$ by $Author$
// Version:         $Revision$
//
// Copyright (c) 2010 Mirko Raner.
// All rights reserved. This program and the accompanying materials are made available under the
// terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available
// at http://www.eclipse.org/legal/epl-v10.html

package net.sf.eclipsemultitouch.operations;

/**
* The class {@link OperationRunner} is the abstract base class for classes that execute certain
* user operations when a Multi-Touch gesture was recognized. Subclasses will typically run actions,
* execute commands, or maybe simulate keystrokes.
*
* @param <$Type$> the argument type of the concrete {@link OperationRunner} subclass (this could
* be a simple string ID, an Eclipse command object, or a factory)
*
* @author Mirko Raner
**/
public abstract class OperationRunner<$Type$>
{
    /**
    * Runs an operation by first checking the {@link OperationValidator} and then by simply passing
    * the supplied argument to the given {@link OperationRunner}.
    *
    * @param <$Type$> the argument type associated with the {@link OperationRunner}
    * @param validator the {@link OperationValidator} for checking whether the operation should be
    * executed
    * @param runner the {@link OperationRunner} (type must match the argument type)
    * @param argument the argument to be passed to the {@link OperationRunner}
    **/
    public static <$Type$> void run(OperationValidator validator, OperationRunner<$Type$> runner,
    $Type$ argument)
    {
        if (validator.isValid())
        {
            runner.run(argument);
        }
    }

    /**
    * Runs an operation - <i>this method must be implemented by subclasses</i>.
    *
    * @param parameter the operation parameter (may be a command ID or a factory of sorts)
    **/
    protected abstract void run($Type$ parameter);
}
