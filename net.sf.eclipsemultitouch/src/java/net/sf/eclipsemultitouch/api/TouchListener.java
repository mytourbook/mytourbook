// <--------------------------------------------------------------------------------------------> //
// File name:       $RCSfile: TouchListener.java $
// First created:   $Created: Dec 15, 2009 by mirko $
// Last modified:   $Date$ by $Author$
// Version:         $Revision$
//
// Copyright (c) 2009 Mirko Raner.
// All rights reserved. This program and the accompanying materials are made available under the
// terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available
// at http://www.eclipse.org/legal/epl-v10.html

package net.sf.eclipsemultitouch.api;

/**
* The interface {@link TouchListener} must be implemented by clients that are interested in
* receiving {@link Touch} contact events from a Multi-Touch&trade; device.
*
* @author Mirko Raner
* @version $Revision: $
**/
public interface TouchListener
{
    /**
    * Receives Multi-Touch&trade; contact events.
    * <p/>
    * <b>NOTE:</b> All {@link Touch} events are delivered by the same thread and must be processed
    * in a timely fashion. A slow {@link TouchListener} may delay other listeners further down in
    * the chain and can cause some events to be delivered long after they originally happened. No
    * complicated processing should happen inside the {@link #touch(double, Touch[])} method.
    * Rather than evaluating all received data (which is usually too much to process),
    * {@link TouchListener}s should try to recognize Multi-Touch&trade; gestures by a small number
    * of characteristic traits, such as the number of fingers and a high velocity in a particular
    * direction, for example.
    *
    * @param timeStamp a time stamp (in seconds, represented as a {@code double})
    * @param touch an array of {@link Touch} objects (one for each contact/finger; will be empty to
    * indicate that all fingers were just released)
    **/
    public abstract void touch(double timeStamp, Touch[] touch);
}
