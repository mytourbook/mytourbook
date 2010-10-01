// <--------------------------------------------------------------------------------------------> //
// File name:       $RCSfile: MultiTouchListener.java $
// First created:   $Created: Dec 16, 2009 by mirko $
// Last modified:   $Date$ by $Author$
// Version:         $Revision$
//
// Copyright (c) 2009 Mirko Raner.
// All rights reserved. This program and the accompanying materials are made available under the
// terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available
// at http://www.eclipse.org/legal/epl-v10.html

package net.sf.eclipsemultitouch;

import java.util.Arrays;
import net.sf.eclipsemultitouch.actions.FactoryActionRunner;
import net.sf.eclipsemultitouch.api.Touch;
import net.sf.eclipsemultitouch.api.TouchListener;
import net.sf.eclipsemultitouch.api.TouchState;
import net.sf.eclipsemultitouch.operations.OperationRunner;
import net.sf.eclipsemultitouch.operations.OperationValidator;
import net.sf.eclipsemultitouch.utilities.CommandRunner;
import net.sf.eclipsemultitouch.utilities.LeftToRightTouchComparator;
import net.sf.eclipsemultitouch.utilities.UIUtilities;
import net.sf.eclipsemultitouch.utilities.WidgetEventDispatcher;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

/**
* The class {@link MultiTouchListener} is a {@link TouchListener} that processes low-level
* {@link Touch} data from a Multi-Touch&trade; device and recognizes additional multi-finger
* gestures which are then translated into Eclipse actions and commands.
*
* @author Mirko Raner
* @version $Revision: $
**/
public class MultiTouchListener implements TouchListener
{
    private final static int PRESSED = TouchState.PRESSED.getState();
    private final static int TAP = TouchState.TAP.getState();
    private final static float RIGHT_SWIPE_VELOCITY_X = 1.2F;
    private final static float LEFT_SWIPE_VELOCITY_X = -1.2F;
    private final static float DOWN_SWIPE_VELOCITY_Y = -1.2F;
    private final static float UP_SWIPE_VELOCITY_Y = 1.2F;
    private final static float LIMIT_X = 0.8F;
    private final static float LIMIT_Y = 0.8F;

    private boolean gesture;
    private OperationValidator validator;
    private WidgetEventDispatcher widgetEventDispatcher;
    private FactoryActionRunner factoryActionRunner;
    private CommandRunner commandRunner;

    /**
    * Creates a new {@link MultiTouchListener}.
    **/
    public MultiTouchListener()
    {
        validator = new MultiTouchOperationValidator();
        widgetEventDispatcher = new WidgetEventDispatcher();
        factoryActionRunner = new FactoryActionRunner();
        commandRunner = new CommandRunner();
    }

    /**
    * @see TouchListener#touch(double, Touch[])
    **/
    public void touch(double timeStamp, Touch[] touch)
    {
        if (touch.length == 3 && !gesture)
        {
            Touch touch1 = touch[0];
            Touch touch2 = touch[1];
            Touch touch3 = touch[2];
            if (touch1.state == PRESSED && touch2.state == PRESSED && touch3.state == PRESSED)
            {
                if (touch1.velocityX > RIGHT_SWIPE_VELOCITY_X
                && (touch2.velocityX > RIGHT_SWIPE_VELOCITY_X)
                && (touch3.velocityX > RIGHT_SWIPE_VELOCITY_X))
                {
                    threeFingerSwipeRight();
                    gesture = true;
                    return;
                }
                if (touch1.velocityX < LEFT_SWIPE_VELOCITY_X
                && (touch2.velocityX < LEFT_SWIPE_VELOCITY_X)
                && (touch3.velocityX < LEFT_SWIPE_VELOCITY_X))
                {
                    threeFingerSwipeLeft();
                    gesture = true;
                    return;
                }
                if (touch1.velocityY > UP_SWIPE_VELOCITY_Y
                && (touch2.velocityY > UP_SWIPE_VELOCITY_Y)
                && (touch3.velocityY > UP_SWIPE_VELOCITY_Y))
                {
                    threeFingerSwipeUp();
                    gesture = true;
                    return;
                }
                if (touch1.velocityY < DOWN_SWIPE_VELOCITY_Y
                && (touch2.velocityY < DOWN_SWIPE_VELOCITY_Y)
                && (touch3.velocityY < DOWN_SWIPE_VELOCITY_Y))
                {
                    threeFingerSwipeDown();
                    gesture = true;
                    return;
                }
            }
            // None of the three-finder swipe gesture were recognized so far (recognition of a gesture
            // would have caused an early return). It is possible that the user made another gesture,
            // though. Non-swipe gestures may be re-entered even if a gesture is already in progress
            // (there is no additional "!gesture" condition):
            //
            // Non-swipe gestures always require ordering of the touch points:
            //
            Arrays.sort(touch, LeftToRightTouchComparator.INSTANCE);
            touch1 = touch[0];
            touch2 = touch[1];
            touch3 = touch[2];
            if (touch1.state == TAP && touch2.state == PRESSED && touch3.state == PRESSED
            && (Math.abs(touch1.velocityX) < LIMIT_X && Math.abs(touch1.velocityY) < LIMIT_Y)
            && (Math.abs(touch2.velocityX) < LIMIT_X && Math.abs(touch2.velocityY) < LIMIT_Y)
            && (Math.abs(touch3.velocityX) < LIMIT_X && Math.abs(touch3.velocityY) < LIMIT_Y))
            {
                threeFingerControlClick();
                gesture = true;
                return;
            }
            if (touch1.state == PRESSED && touch2.state == PRESSED && touch3.state == TAP
            && (Math.abs(touch1.velocityX) < LIMIT_X && Math.abs(touch1.velocityY) < LIMIT_Y)
            && (Math.abs(touch2.velocityX) < LIMIT_X && Math.abs(touch2.velocityY) < LIMIT_Y)
            && (Math.abs(touch3.velocityX) < LIMIT_X && Math.abs(touch3.velocityY) < LIMIT_Y))
            {
                threeFingerFileClose();
                gesture = true;
                return;
            }
        }
        else if (touch.length < 3)
        {
            gesture = false;
        }
    }

    void threeFingerSwipeLeft()
    {
        OperationRunner.run(validator, factoryActionRunner, ActionFactory.BACKWARD_HISTORY);
    }

	void threeFingerSwipeRight()
    {
	    OperationRunner.run(validator, factoryActionRunner, ActionFactory.FORWARD_HISTORY);
    }

    void threeFingerSwipeUp()
    {
        OperationRunner.run(validator, commandRunner, ITextEditorActionDefinitionIds.TEXT_START);
    }

    void threeFingerSwipeDown()
    {
        OperationRunner.run(validator, commandRunner, ITextEditorActionDefinitionIds.TEXT_END);
    }

    void threeFingerFileClose()
    {
        OperationRunner.run(validator, commandRunner, IWorkbenchCommandConstants.FILE_CLOSE);
    }

    void threeFingerControlClick()
    {
        // NOTE: This functionality was originally implemented with a two-step approach: the
        //       first three-finger tap-hold-hold would send an SWT.MouseMove event that would
        //       merely reveal the hyperlink; the second tap-hold-hold would send an SWT.MouseUp
        //       that would navigate to the link's target. However, in those cases where revealing
        //       the hyperlink would pop up a context menu (for example, "Open Declaration, Open
        //       Implementation") and the user would click the context menu (with a regular button-1
        //       click) the workbench would go into a livelock. As the livelock takes up enormous
        //       amounts of CPU time, the whole system becomes so sluggish that it is not possible
        //       to operate the debugger in the development workbench to diagnose the exact nature
        //       of the livelock.
        //       To prevent this problem from happening, the MouseMove and MouseUp events are now
        //       sent together in sequence.
        //
        Point relativeLocation = new Point(-1, -1);
        Control cursorControl = UIUtilities.getCursorLocationAndControl(relativeLocation);
        if (cursorControl instanceof StyledText)
        {
            StyledText styledText = (StyledText)cursorControl;
            Event[] event = {new Event(), new Event()};
            for (int index = 0; index <= 1; index++)
            {
                event[index].x = relativeLocation.x;
                event[index].y = relativeLocation.y;
                event[index].display = Display.getDefault();
                event[index].doit = true;
                event[index].time = (int)System.currentTimeMillis();
                event[index].widget = styledText;
                event[index].type = index == 0? SWT.MouseMove:SWT.MouseUp;
                event[index].stateMask = index == 0? SWT.COMMAND:SWT.COMMAND|SWT.BUTTON1;
                event[index].button = index;
            }
            OperationRunner.run(validator, widgetEventDispatcher, event);
        }
    }
}
