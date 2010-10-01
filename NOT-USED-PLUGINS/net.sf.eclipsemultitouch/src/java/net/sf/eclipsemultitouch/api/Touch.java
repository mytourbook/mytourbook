// <--------------------------------------------------------------------------------------------> //
// File name:       $RCSfile: Touch.java $
// First created:   $Created: Dec 11, 2009 by mirko $
// Last modified:   $Date$ by $Author$
// Version:         $Revision$
//
// Copyright (c) 2009 Mirko Raner.
// All rights reserved. This program and the accompanying materials are made available under the
// terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available
// at http://www.eclipse.org/legal/epl-v10.html

package net.sf.eclipsemultitouch.api;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
* The class {@link Touch} represents an individual contact event from a Multi-Touch&trade; device.
* Multi-finger gestures are represented by multiple {@link Touch} objects (one for each finger).
* Similar to the SWT {@code org.eclipse.swt.widgets.Event} class, {@link Touch} objects have
* public (but non-writable) fields that can be accessed directly, without the necessity for
* accessor methods. {@link Touch} objects are created exclusively by the native Multi-Touch&trade;
* driver and cannot be instantiated by client code. Clients that are interested in receiving
* {@link Touch} events can add themselves using the {@link #addTouchListener(TouchListener)} method
* (and remove themselves using {@link #removeTouchListener(TouchListener)}.
*
* This class is partially based on reverse engineering and previous development work by Erling Alf
* Ellingsen and Wayne Keenan.
*
* @author Mirko Raner
* @version $Revision: $
**/
public final class Touch
{
    /** The time stamp (as a {@code double}, of all things). **/
    public final double timeStamp;

    /** The frame sequence number. **/
    public final int frame;

    /** The finger ID. **/
    public final int fingerID;

    /** The state. **/
    public final int state;

    /** The x position. **/
    public final float positionX;

    /** The y position. **/
    public final float positionY;

    /** The x velocity. **/
    public final float velocityX;

    /** The y velocity. **/
    public final float velocityY;

    /** The size of the elliptical contact area. **/
    public final float size;

    /** The angle of the elliptical contact area's major axis. **/
    public final float angle;

    /** The length of the elliptical contact area's major axis. **/
    public final float majorAxis;

    /** The length of the elliptical contact area's minor axis. **/
    public final float minorAxis;

    private final static List<TouchListener> listeners = new ArrayList<TouchListener>();
    private static int liveTouchObjects;

    static
    {
        System.loadLibrary("multitouch"); //$NON-NLS-1$
    }

    static void callback(double timeStamp, Touch[] touch)
    {
        for (TouchListener listener: listeners)
        {
            try
            {
                listener.touch(timeStamp, touch);
            }
            catch (Exception exception)
            {
                exception.printStackTrace();
            }
        }
    }

    private Touch(double timeStamp, int frame, int fingerID, int state,
    float positionX, float positionY, float velocityX, float velocityY, float size, float angle,
    float majorAxis, float minorAxis)
    {
        this.timeStamp = timeStamp;
        this.frame = frame;
        this.fingerID = fingerID;
        this.state = state;
        this.positionX = positionX;
        this.positionY = positionY;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.size = size;
        this.angle = angle;
        this.majorAxis = majorAxis;
        this.minorAxis = minorAxis;
        liveTouchObjects++;
    }

    /**
    * Adds a new {@link TouchListener} to the list of listeners to be notified of incoming
    * {@link Touch} contact events. Listeners that are added multiple times will also need to be
    * removed multiple times (currently, duplicate listeners are not automatically detected).
    *
    * @param listener the {@link TouchListener} to be added
    **/
    public static void addTouchListener(TouchListener listener)
    {
        listeners.add(listener);
    }
    
    /**
    * Removes an existing {@link TouchListener} from the list of listeners that are notified of
    * incoming {@link Touch} contact events.
    *
    * @param listener the {@link TouchListener} to be removed
    **/
    public static void removeTouchListener(TouchListener listener)
    {
        listeners.remove(listener);
    }

    /**
    * Returns the number of {@link Touch} objects that were created but have not been
    * garbage-collected yet. This method is useful for debugging memory leaks caused by the native
    * driver implementation; regular client code will rarely have the need to call this method.
    *
    * @return the number of live {@link Touch} objects currently in memory
    **/
    public static int getNumberOfLiveTouchObjects()
    {
        return liveTouchObjects;
    }

    /**
    * @see Object#finalize()
    **/
    @Override
    protected void finalize() throws Throwable
    {
        super.finalize();
        liveTouchObjects--;
    }

    /**
    * @see Object#toString()
    **/
    public String toString()
    {
        StringBuffer string = new StringBuffer();
        for (Field field: getClass().getDeclaredFields())
        {
            string.append(string.length() == 0? getClass().getName()+'[':", "); //$NON-NLS-1$
            string.append(field.getName()).append('=');
            try
            {
                string.append(field.get(this));
            }
            catch (IllegalAccessException illegalAccess)
            {
                string.append(illegalAccess);
            }
        }
        return string.append(']').toString();
    }
}
