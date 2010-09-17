// <--------------------------------------------------------------------------------------------> //
// File name:       $RCSfile: MultiTouchStartup.java $
// First created:   $Created: Dec 18, 2009 by mirko $
// Last modified:   $Date$ by $Author$
// Version:         $Revision$
//
// Copyright (c) 2009 Mirko Raner.
// All rights reserved. This program and the accompanying materials are made available under the
// terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available
// at http://www.eclipse.org/legal/epl-v10.html

package net.sf.eclipsemultitouch;

import net.sf.eclipsemultitouch.api.Touch;
import org.eclipse.ui.IStartup;

/**
* The class {@link MultiTouchStartup} activates the enhanced Multi-Touch&trade; support as soon as
* the workbench starts. Early start-up is necessary because the Eclipse Multi-Touch plug-in makes
* no UI contributions that would otherwise trigger bundle activation.
*
* @author Mirko Raner
* @version $Revision: $
**/
public class MultiTouchStartup implements IStartup
{
	/**
	* Instantiates and registers the {@link MultiTouchListener}, the plug-in's central listener.
	*
	* @see IStartup#earlyStartup()
	**/
	public void earlyStartup()
	{
        Touch.addTouchListener(new MultiTouchListener());
	}
}
