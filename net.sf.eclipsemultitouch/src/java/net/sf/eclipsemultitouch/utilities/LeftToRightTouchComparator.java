// <--------------------------------------------------------------------------------------------> //
// File name:       $RCSfile: LeftToRightTouchComparator.java $
// First created:   $Created: May 4, 2010 by mirko $
// Last modified:   $Date$ by $Author$
// Version:         $Revision$
//
// Copyright (c) 2010 Mirko Raner.
// All rights reserved. This program and the accompanying materials are made available under the
// terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available
// at http://www.eclipse.org/legal/epl-v10.html

package net.sf.eclipsemultitouch.utilities;

import java.util.Comparator;
import net.sf.eclipsemultitouch.api.Touch;

/**
* The class {@link LeftToRightTouchComparator} is a {@link Comparator} that compares the x
* coordinates of {@link Touch} events in such a fashion that sorting with this comparator will
* arrange them in an order from left to right.
* {@link LeftToRightTouchComparator} is a non-lazy singleton, and its instance object is publicly
* accessible as {@link LeftToRightTouchComparator#INSTANCE}.
*
* @author Mirko Raner
* @version $Revision: $ $Change: $
**/
public class LeftToRightTouchComparator implements Comparator<Touch>
{
    /**
     * The {@link LeftToRightTouchComparator} instance.
     */
    public final static Comparator<Touch> INSTANCE = new LeftToRightTouchComparator();

    private LeftToRightTouchComparator()
    {
        super();
    }

    /**
    * @see Comparator#compare(Object, Object)
    **/
    public int compare(Touch touch1, Touch touch2)
    {
        float x1 = touch1.positionX;
        float x2 = touch2.positionX;
        return x1 < x2? -1:(x1 > x2? 1:0);
    }
}
