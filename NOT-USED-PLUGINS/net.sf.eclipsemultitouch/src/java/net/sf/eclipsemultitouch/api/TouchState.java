// <--------------------------------------------------------------------------------------------> //
// File name:       $RCSfile: TouchState.java $
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
* The enumeration {@link TouchState} lists the possible states of a Multi-Touch&trade; contact
* event.
*
* This class is partially based on reverse engineering and previous development work by Erling Alf
* Ellingsen and Wayne Keenan.
*
* @author Mirko Raner
* @version $Revision: $
**/
public enum TouchState
{
    /** An unknown state (can correspond to any numeric value other than 1 to 7). **/
    UNKNOWN(0),

    /** The state with the numeric value of 1 (exact meaning is not known). **/
    UNKNOWN_ONE(1),

    /** The hover state. **/
    HOVER(2),

    /** The tap state. **/
    TAP(3),

    /** The pressed state. **/
    PRESSED(4),

    /** The pressing state (transition from not pressed to pressed). **/
    PRESSING(5),

    /** The releasing state (transition from pressed to not pressed). **/
    RELEASING(6),

    /** The released state. **/
    RELEASED(7);

    private int state;

    private TouchState(int state)
    {
        this.state = state;
    }

    /**
    * Returns the numeric state value of the {@link TouchState}.
    *
    * @return the numeric state (currently between 0 and 7 inclusive)
    **/
    public int getState()
    {
        return state;
    }

    /**
    * Returns the {@link TouchState} object corresponding to a numeric value.
    *
    * @param state the numeric value
    * @return the corresponding {@link TouchState}
    **/
    public static TouchState getTouchState(int state)
    {
        switch (state)
        {
            case 1: return UNKNOWN_ONE;
            case 2: return HOVER;
            case 3: return TAP;
            case 4: return PRESSED;
            case 5: return PRESSING;
            case 6: return RELEASING;
            case 7: return RELEASED;
            default: return UNKNOWN;
        }
    }
}
