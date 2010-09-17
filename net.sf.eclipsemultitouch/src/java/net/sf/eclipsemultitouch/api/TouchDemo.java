// <--------------------------------------------------------------------------------------------> //
// File name:       $RCSfile: TouchDemo.java $
// First created:   $Created: Dec 16, 2009 by mirko $
// Last modified:   $Date$ by $Author$
// Version:         $Revision$
//
// Copyright (c) 2009 Mirko Raner.
// All rights reserved. This program and the accompanying materials are made available under the
// terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available
// at http://www.eclipse.org/legal/epl-v10.html

package net.sf.eclipsemultitouch.api;

import java.util.Arrays;

/**
* The class {@link TouchDemo} is a simple demonstration class that prints out the raw data
* received from a Multi-Touch&trade; device.
*
* @author Mirko Raner
* @version $Revision: $
**/
public class TouchDemo implements TouchListener
{
    private final static int ONE_SECOND = 1000;

    /**
    * Starts the demo.
    *
    * @param arguments the command line arguments (all arguments are ignored)
    * @throws InterruptedException if a thread interrupt occurred
    **/
    public static void main(String... arguments) throws InterruptedException
    {
        Touch.addTouchListener(new TouchDemo());
        System.out.println("TouchListener added."); //$NON-NLS-1$
        while (true)
        {
            Thread.sleep(ONE_SECOND);
        }
    }

    /**
    * @see TouchListener#touch(double, Touch[])
    **/
    public void touch(double timeStamp, Touch[] touch)
    {
        System.out.println(Arrays.asList(touch));
    }
}
