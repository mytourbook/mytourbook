// <--------------------------------------------------------------------------------------------> //
// File name:       $RCSfile: OperationValidator.java $
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
* The interface {@link OperationValidator} describes an abstract means of checking whether an
* operation is valid in the current context. It is up to the implementor what that "context" is
* actually comprised of and how it is obtained. An {@link OperationValidator} is a simple way to
* enable or disabled a operation based on the current state of the application.
* An {@link OperationValidator} also offers the possibility of delayed code execution and removes
* the need to surround operation execution statements with {@code if (valid)}... constructs. In the
* case of the {@link net.sf.eclipsemultitouch.MultiTouchListener} class, the recognition of a
* Multi-Touch gesture is actually a very fast operation involving only a few integer comparisons.
* On the other hand, determining whether an operation actually should be executed (based on whether
* an Eclipse workbench window is the currently active window) is a relatively slow and expensive
* operation since part of the validation needs to be performed in the SWT UI thread, and there might
* be a delay until the validation code actually gets scheduled. Again, encapsulating the validation
* code in its own class provides more flexibility should the validation need to be executed at a
* different point in time.
*
* @author Mirko Raner
**/
public interface OperationValidator
{
    /**
    * Determines whether an operation is valid.
    *
    * @return {@code true} if the operation is valid, {@code false} otherwise
    **/
    public abstract boolean isValid();
}
