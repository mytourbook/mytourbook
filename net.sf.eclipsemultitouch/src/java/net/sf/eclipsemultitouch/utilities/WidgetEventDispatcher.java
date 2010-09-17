// <--------------------------------------------------------------------------------------------> //
// File name:       $RCSfile: WidgetEventDispatcher.java $
// First created:   $Created: May 4, 2010 by mirko $
// Last modified:   $Date$ by $Author$
// Version:         $Revision$
//
// Copyright (c) 2010 Mirko Raner.
// All rights reserved. This program and the accompanying materials are made available under the
// terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available
// at http://www.eclipse.org/legal/epl-v10.html

package net.sf.eclipsemultitouch.utilities;

import net.sf.eclipsemultitouch.operations.OperationRunner;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Widget;

/**
* The class {@link WidgetEventDispatcher} is an {@link OperationRunner} for sending out
* {@link SWT} {@link Event}s to their target {@link Widget}s.
*
* @author Mirko Raner
* @version $Revision: $ $Change: $
**/
public class WidgetEventDispatcher extends OperationRunner<Event[]> implements Runnable
{
    private final Object LOCK = new Object();

    private Event event;

    /**
    * Sends {@link Event}s to the specified {@link Widget}.
    *
    * @param events the {@link Event}s
    **/
    @Override
    public void run(Event[] events)
    {
        synchronized (LOCK)
        {
            int lastEvent = events.length-1;
            for (int index = 0; index <= lastEvent; index++)
            {
                event = events[index];
                Display.getDefault().syncExec(this);
                if (index < lastEvent)
                {
                    // When sending multiple events sleep for a short interval between events so
                    // that certain UI updates can happen and the user experience is similar to
                    // manually triggering the events (for example, this allows the underlining for
                    // hyperlinks to show up, so that the user is made aware that hyperlink
                    // navigation is taking place):
                    //
                    try
                    {
                        Thread.sleep(200);
                    }
                    catch (InterruptedException interrupt)
                    {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

    /**
    * Executes {@link WidgetEventDispatcher} code in the SWT UI thread.
    *
    * @see #run(Event[])
    * @see Runnable#run()
    * @see Display#syncExec(Runnable)
    **/
    public void run()
    {
        event.widget.notifyListeners(event.type, event);
    }
}
