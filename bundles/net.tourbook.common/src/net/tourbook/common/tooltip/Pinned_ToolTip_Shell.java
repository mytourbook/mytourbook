/*******************************************************************************
 * Copyright (C) 2005, 2019  Wolfgang Schramm and Contributors
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *******************************************************************************/
package net.tourbook.common.tooltip;

import net.tourbook.common.PointLong;
import net.tourbook.common.util.Util;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Part of this tooltip is copied from {@link ToolTip}.
 */
public abstract class Pinned_ToolTip_Shell {

   private static final int                          VALUE_POINT_OFFSET                   = 20;

   private static final String                       STATE_MOUSE_X_POSITION_RELATIVE      = "Pinned_ToolTip_MouseXPositionRelative";   //$NON-NLS-1$
   private static final String                       STATE_IS_TOOLTIP_ABOVE_VALUE_POINT   = "Pinned_ToolTip_IsToolTipAboveValuePoint"; //$NON-NLS-1$
   private static final String                       STATE_PINNED_TOOLTIP_X               = "Pinned_ToolTip_DiffPositionX";            //$NON-NLS-1$
   private static final String                       STATE_PINNED_TOOLTIP_Y               = "Pinned_ToolTip_DiffPositionY";            //$NON-NLS-1$
   protected static final String                     STATE_PINNED_TOOLTIP_PIN_LOCATION    = "Pinned_ToolTip_PinnedLocation";           //$NON-NLS-1$

   protected static final Pinned_ToolTip_PinLocation DEFAULT_PIN_LOCATION                 = Pinned_ToolTip_PinLocation.TopRight;

   private static final String                       STATE_PINNED_TOOLTIP_SCREEN_PINNED_X = "Pinned_ToolTip_ScreenPinnedX";            //$NON-NLS-1$
   private static final String                       STATE_PINNED_TOOLTIP_SCREEN_PINNED_Y = "Pinned_ToolTip_ScreenPinnedY";            //$NON-NLS-1$

   protected IDialogSettings                         state;

   private IPinned_Tooltip_Owner                     _tooltipOwner;

   private Shell                                     _ttShell;

   private Object                                    _currentArea;
   private Control                                   _ownerControl;

   private OwnerShellListener                        _ownerShellListener;
   private OwnerControlListener                      _ownerControlListener;
   private TooltipListener                           _ttListener                          = new TooltipListener();
   private TooltipShellListener                      _ttShellListener                     = new TooltipShellListener();

   private boolean                                   _isTTDragged;

   private int                                       _devXTTMouseDown;
   private int                                       _devYTTMouseDown;
   private int                                       _devXOwnerMouseMove;

   /**
    * Relative y position for the pinned location {@link Pinned_ToolTip_PinLocation#MouseXPosition}
    */
   private int                                       _devPinnedMouseXPositionRelative;

   /**
    * Is <code>true</code> when the tool tip location is above the value point in the chart.
    */
   private boolean                                   _isTTAboveValuePoint                 = true;

   /**
    * Position where the mouse is hovered, the position is relative to the
    * client.
    */
   private PointLong                                 _ownerValuePoint_DevPosition         = new PointLong(0, 0);

   protected int                                     snapBorder_Top;
   protected int                                     snapBorder_Bottom;

   /**
    * Relative location for the tooltip shell to the pinned location when it's moved with the mouse.
    */
   private Point                                     _ttShellDiff                         = new Point(0, 0);

   /**
    * Contains position where the tt shell should be positioned
    */
   private Point                                     _screenDefaultTTShellLocation;

   /**
    * Screen location for the screen pinned location
    */
   private Point                                     _screenScreenTTShellLocation;

   private Point                                     _screenRequestedAnimationLocation    = new Point(0, 0);

   Pinned_ToolTip_PinLocation                        pinnedLocation;

   /**
    * This value is > 0 when tooltip could not be set at the default location
    */
   private int                                       _defaultOffsetY;
   private Display                                   _display;
   private Runnable                                  _ttShellPositioningRunnable;
   private int                                       _animationCounter;
   private int                                       _repeatTime;

   /*
    * UI resources
    */
   private Cursor _cursorDragged;
   private Cursor _cursorHand;

   private class OwnerControlListener implements Listener {
      @Override
      public void handleEvent(final Event event) {

         switch (event.type) {
         case SWT.Dispose:
            toolTipHide(_ttShell, event);
            break;

         case SWT.Resize:
            onResizeOwner(event);
         }
      }
   }

   private final class OwnerShellListener implements Listener {
      @Override
      public void handleEvent(final Event event) {

         if (_ownerControl == null || _ownerControl.isDisposed()) {
            return;
         }

         switch (event.type) {
         case SWT.Deactivate:

            _display.asyncExec(new Runnable() {

               @Override
               public void run() {

                  // hide tooltip when another shell is activated

                  if (_display.getActiveShell() != _ttShell) {
                     toolTipHide(_ttShell, event);
                  }
               }
            });
            break;

         case SWT.Move:
            onMoveOwner(event);
            break;
         }
      }
   }

   /**
    * This listener is added to ALL widgets within the tooltip shell.
    */
   private class TooltipListener implements Listener {
      @Override
      public void handleEvent(final Event event) {

         if (_ttShell == null || _ttShell.isDisposed()) {
            return;
         }

         if (event.widget instanceof Control) {

            final Control control = (Control) event.widget;
            boolean isToolbar = false;

            Cursor cursor = null;

            if (control instanceof ToolBar) {

               // disable other features when toolbar actions are hovered
               isToolbar = true;

            } else {

               boolean isForwardEvent = false;

               switch (event.type) {
               case SWT.MouseMove:

                  if (_isTTDragged) {
                     onMoveTT(event);
                  } else {

                     /*
                      * move value point in the chart when tooltip is hovered and the mouse position
                      * is within the chart
                      */
                     isForwardEvent = true;

                     cursor = _cursorHand;
                  }

                  break;

               case SWT.MouseDown:

                  _isTTDragged = true;

                  _devXTTMouseDown = event.x;
                  _devYTTMouseDown = event.y;

                  cursor = _cursorDragged;

                  break;

               case SWT.MouseUp:

                  if (_isTTDragged) {

                     _isTTDragged = false;

                     onMouseUpTT(event);
                  }

                  cursor = _cursorHand;

                  break;

               case SWT.MouseVerticalWheel:

                  // pass to tt owner for zooming in/out
                  isForwardEvent = true;

                  break;

               case SWT.MouseEnter:

                  isForwardEvent = true;

                  break;

               case SWT.MouseExit:

                  isForwardEvent = true;
                  _isTTDragged = false;

                  break;
               }

               if (isForwardEvent) {
                  _tooltipOwner.handleMouseEvent(event, control.toDisplay(event.x, event.y));
               }
            }

            /*
             * shell could be hidden (it was during testing)
             */
            if (_ttShell != null && !_ttShell.isDisposed()) {

               if (cursor != null) {

                  _ttShell.setCursor(cursor);

               } else if (isToolbar) {

                  // display normal cursor when toolbar actions are hovered
                  _ttShell.setCursor(null);
               }
            }
         }
      }
   }

   private final class TooltipShellListener implements Listener {
      @Override
      public void handleEvent(final Event e) {

         if (_ttShell != null && !_ttShell.isDisposed() && _ownerControl != null && !_ownerControl.isDisposed()) {

            _display.asyncExec(new Runnable() {

               @Override
               public void run() {

                  // hide tooltip when another shell is activated

                  // check again
                  if (_ttShell == null
                        || _ttShell.isDisposed()
                        || _ownerControl == null
                        || _ownerControl.isDisposed()) {
                     return;
                  }

                  if (_ownerControl.getShell() == _ttShell.getDisplay().getActiveShell()) {

                     // don't hide when main window is active
                     return;
                  }

                  toolTipHide(_ttShell, null);
               }
            });
         }
      }
   }

   /**
    * Create new instance which add TooltipSupport to the widget
    *
    * @param state
    * @param control
    *           the control on whose action the tooltip is shown
    */
   public Pinned_ToolTip_Shell(final IPinned_Tooltip_Owner tooltipOwner, final IDialogSettings state) {

      _tooltipOwner = tooltipOwner;
      _ownerControl = tooltipOwner.getControl();
      _display = _ownerControl.getDisplay();

      this.state = state;

      _ownerControl.addDisposeListener(new DisposeListener() {
         @Override
         public void widgetDisposed(final DisposeEvent e) {
            onDispose();
         }
      });

      _ownerControlListener = new OwnerControlListener();
      _ownerShellListener = new OwnerShellListener();

      addOwnerControlListener();

      _cursorDragged = new Cursor(_display, SWT.CURSOR_SIZEALL);
      _cursorHand = new Cursor(_display, SWT.CURSOR_HAND);

      _ttShellPositioningRunnable = new Runnable() {
         @Override
         public void run() {
            setTTShellLocation20Runnable();
         }
      };

      restoreState();
   }

   /**
    * Pin the tooltip to a corder which is defined in PIN_LOCATION_...
    *
    * @param locationId
    */
   public void actionPinLocation(final Pinned_ToolTip_PinLocation locationId) {

      // set new location
      pinnedLocation = locationId;

      if (_ttShell == null || _ttShell.isDisposed()) {
         return;
      }

//      if (pinnedLocation == ValuePointToolTipPinLocation.Screen) {
//         return;
//      }

      // display at default location without offset
      _ttShellDiff = new Point(0, 0);

      setTTShellLocation(false, true, true, true);
   }

   /**
    * Activate tooltip support for this control
    */
   private void addOwnerControlListener() {

      removeOwnerControlListener();

      _ownerControl.addListener(SWT.Dispose, _ownerControlListener);
      _ownerControl.addListener(SWT.Resize, _ownerControlListener);
   }

   /**
    * Add listener to all controls within the tooltip
    * <p>
    * ########################### Recursive #########################################<br>
    *
    * @param control
    */
   private void addTooltipListener(final Control control) {

      control.addListener(SWT.MouseMove, _ttListener);
      control.addListener(SWT.MouseDown, _ttListener);
      control.addListener(SWT.MouseUp, _ttListener);
      control.addListener(SWT.MouseEnter, _ttListener);
      control.addListener(SWT.MouseExit, _ttListener);
      control.addListener(SWT.MouseVerticalWheel, _ttListener);

      if (control instanceof Composite) {
         final Control[] children = ((Composite) control).getChildren();
         for (final Control child : children) {
            addTooltipListener(child);
         }
      }
   }

   /**
    * Creates the content area of the the tooltip.
    *
    * @param event
    *           the event that triggered the activation of the tooltip
    * @param shell
    *           the parent of the content area
    * @return the content area created
    */
   protected abstract Composite createToolTipContentArea(Event event, Composite shell);

   /**
    * @param tipSize
    * @param tipLocation
    * @param screenValuePoint
    * @param isTTDragged
    * @return
    */
   private Point fixupDisplayBounds(final Point tipSize,
                                    final Point tipLocation,
                                    final Point screenValuePoint,
                                    final boolean isTTDragged) {

      int tipLeft = tipLocation.x;
      int tipTop = tipLocation.y;
      final int tipWidth = tipSize.x;
      final int tipHeight = tipSize.y;
      final int tipRight = tipLeft + tipWidth;
      final int tipBottom = tipTop + tipHeight;

      Rectangle screenBounds;

      final Monitor[] monitors = _display.getMonitors();

      if (monitors.length > 1) {
         // By default present in the monitor of the control
         screenBounds = _ownerControl.getMonitor().getBounds();
         final Point p = new Point(tipLeft, tipTop);

         // Search on which monitor the event occurred
         Rectangle tmp;
         for (final Monitor monitor : monitors) {
            tmp = monitor.getBounds();
            if (tmp.contains(p)) {
               screenBounds = tmp;
               break;
            }
         }

      } else {
         screenBounds = _display.getBounds();
      }

      // create a copy that the original value is not modified
//      final Point newTipLocation = new Point(tipLeft, tipTop);
      final Point tipBottomRight = new Point(tipRight, tipBottom);

      if (!(screenBounds.contains(tipLocation) && screenBounds.contains(tipBottomRight))) {

         if (tipRight > screenBounds.x + screenBounds.width) {
            tipLeft -= tipRight - (screenBounds.x + screenBounds.width);
         }

         if (tipBottom > screenBounds.y + screenBounds.height) {

            // move to screen bottom
            tipTop -= tipBottom - (screenBounds.y + screenBounds.height);

            if (isTTDragged == false) {

               // check if value point is covered by the tooltip
               if (tipTop < screenValuePoint.y + VALUE_POINT_OFFSET) {

                  // this implementation do NOT respect multiple monitors

                  // move tip above the value point
                  tipTop = screenValuePoint.y - tipHeight - VALUE_POINT_OFFSET;
               }
            }
         }

         if (tipLeft < screenBounds.x) {
            // move to screen left
            tipLeft = screenBounds.x;
         }

         if (tipTop < screenBounds.y) {

            // move to screen top
            tipTop = screenBounds.y;

            if (isTTDragged == false) {

               // check if value point is covered by the tooltip
               if (tipTop + tipHeight > screenValuePoint.y - VALUE_POINT_OFFSET) {

                  // this implementation do NOT respect multiple monitors

                  // move tip above the value point
                  tipTop = screenValuePoint.y + VALUE_POINT_OFFSET;
               }
            }
         }
      }

      return new Point(tipLeft, tipTop);
   }

   public Pinned_ToolTip_PinLocation getPinnedLocation() {
      return pinnedLocation;
   }

   /**
    * This method is called to check for which area the tooltip is created/hidden for. In case of
    * {@link #NO_RECREATE} this is used to decide if the tooltip is hidden recreated.
    * <code>By the default it is the widget the tooltip is created for but could be any object. To decide if
    * the area changed the {@link Object#equals(Object)} method is used.</code>
    *
    * @param event
    *           the event
    * @return the area responsible for the tooltip creation or <code>null</code> this could be any
    *         object describing the area (e.g. the {@link Control} onto which the tooltip is bound
    *         to, a part of this area e.g. for {@link ColumnViewer} this could be a
    *         {@link ViewerCell})
    */
   protected Object getToolTipArea(final Event event) {
      return _ownerControl;
   }

   protected Shell getToolTipShell() {
      return _ttShell;
   }

   /**
    * Hide the currently active tool tip
    */
   public void hide() {
      toolTipHide(_ttShell, null);
   }

   protected void onDispose() {

      _cursorDragged = (Cursor) Util.disposeResource(_cursorDragged);
      _cursorHand = (Cursor) Util.disposeResource(_cursorHand);

      removeOwnerControlListener();
   }

   private void onMouseUpTT(final Event event) {

      /*
       * get the tt vertical position
       */
      // value point position
      final int devYValuePoint = _ownerControl.toDisplay(0, (int) _ownerValuePoint_DevPosition.y).y;

      final Rectangle scrTTBounds = _ttShell.getBounds();
      final int srcTTTop = scrTTBounds.y;
      final int scrTTBottom = srcTTTop + scrTTBounds.height;

      _isTTAboveValuePoint = devYValuePoint > scrTTBottom;

      /*
       * get tt relative position
       */
      final Point scrOwnerControlLocation = _ownerControl.toDisplay(0, 0);
      final Point ownerSize = _ownerControl.getSize();
      final int scrOwnerTop = scrOwnerControlLocation.y;
      final int ownerHeight = ownerSize.y;
      final int srcOwnerBottom = scrOwnerTop + ownerHeight;

      if (_isTTAboveValuePoint) {
         _devPinnedMouseXPositionRelative = scrOwnerTop - scrTTBottom;
      } else {
         _devPinnedMouseXPositionRelative = srcOwnerBottom - srcTTTop;
      }

      setTTShellLocation(false, true, false, true);
   }

   /**
    * The owner shell has been moved, adjust tooltip shell that it moves also with the owner control
    * but preserves the display border.
    */
   private void onMoveOwner(final Event event) {

      if (_ttShell == null || _ttShell.isDisposed()) {
         return;
      }

      setTTShellLocation(false, false, false, false);
   }

   /**
    * Tooltip location has been moved with the mouse.
    *
    * @param event
    */
   private void onMoveTT(final Event event) {

      final int xDiff = event.x - _devXTTMouseDown;
      final int yDiff = event.y - _devYTTMouseDown;

      if (pinnedLocation == Pinned_ToolTip_PinLocation.Screen) {

         _ttShellDiff.x = xDiff;
         _ttShellDiff.y = yDiff;

      } else if (pinnedLocation == Pinned_ToolTip_PinLocation.MouseXPosition) {

         _ttShellDiff.x = 0;
         _ttShellDiff.y = yDiff;

      } else {

         _ttShellDiff.x += xDiff;
         _ttShellDiff.y += yDiff;
      }

      setTTShellLocation(true, false, false, true);
   }

   /**
    * Owner control is resized.
    *
    * @param event
    */
   private void onResizeOwner(final Event event) {

      if (_ttShell == null || _ttShell.isDisposed()) {
         return;
      }

      setTTShellLocation(false, false, false, false);
   }

   private void passOnEvent(final Shell tip, final Event event) {
      if (_ownerControl != null
            && !_ownerControl.isDisposed()
            && event != null
            && event.widget != _ownerControl
            && event.type == SWT.MouseDown) {
         // the following was left in order to fix bug 298770 with minimal change. In 3.7, the complete method should be removed.
         tip.close();
      }
   }

   /**
    * Deactivate tooltip support for the underlying control
    */
   private void removeOwnerControlListener() {

      _ownerControl.removeListener(SWT.Dispose, _ownerControlListener);
      _ownerControl.removeListener(SWT.Resize, _ownerControlListener);
   }

   private void restoreState() {

      /*
       * restore value point tooltip location, when location is not set, don't set it to 0,0 instead
       * use tooltip default location which is positioning the tooltip into the center of the chart
       */
      if (state.get(STATE_PINNED_TOOLTIP_X) != null) {

         _ttShellDiff = new Point(//
               Util.getStateInt(state, STATE_PINNED_TOOLTIP_X, 0),
               Util.getStateInt(state, STATE_PINNED_TOOLTIP_Y, 0));
      }

      if (state.get(STATE_PINNED_TOOLTIP_SCREEN_PINNED_X) != null) {

         _screenScreenTTShellLocation = new Point(
               Util.getStateInt(state, STATE_PINNED_TOOLTIP_SCREEN_PINNED_X, 0),
               Util.getStateInt(state, STATE_PINNED_TOOLTIP_SCREEN_PINNED_Y, 0));
      }

      // tooltip orientation
      final String statePinnedLocation = Util.getStateString(
            state,
            STATE_PINNED_TOOLTIP_PIN_LOCATION,
            DEFAULT_PIN_LOCATION.name());

      pinnedLocation = Pinned_ToolTip_PinLocation.valueOf(statePinnedLocation);

      _devPinnedMouseXPositionRelative = Util.getStateInt(state, STATE_MOUSE_X_POSITION_RELATIVE, 0);
      _isTTAboveValuePoint = Util.getStateBoolean(state, STATE_IS_TOOLTIP_ABOVE_VALUE_POINT, true);
   }

   public void saveState() {

      state.put(STATE_PINNED_TOOLTIP_X, _ttShellDiff.x);
      state.put(STATE_PINNED_TOOLTIP_Y, _ttShellDiff.y);

      if (_screenScreenTTShellLocation != null) {

         state.put(STATE_PINNED_TOOLTIP_SCREEN_PINNED_X, _screenScreenTTShellLocation.x);
         state.put(STATE_PINNED_TOOLTIP_SCREEN_PINNED_Y, _screenScreenTTShellLocation.y);
      }

      state.put(STATE_PINNED_TOOLTIP_PIN_LOCATION, pinnedLocation.name());
      state.put(STATE_MOUSE_X_POSITION_RELATIVE, _devPinnedMouseXPositionRelative);
      state.put(STATE_IS_TOOLTIP_ABOVE_VALUE_POINT, _isTTAboveValuePoint);
   }

   public void setShellVisible(final boolean isVisible) {

      if (_ttShell == null || _ttShell.isDisposed()) {
         return;
      }

      _ttShell.setVisible(isVisible);
   }

   /**
    * Set tooltip location according to the pinned location.
    *
    * @param isTTDragged
    *           is <code>true</code> when the tooltip is dragged
    * @param isAnimation
    * @param isSetDefaultLocation
    *           When <code>true</code> set tooltip to a default location
    */
   private void setTTShellLocation(final boolean isTTDragged,
                                   final boolean isAnimation,
                                   final boolean isSetDefaultLocation,
                                   final boolean isCheckVPCovered) {

      final Point screenOwnerControlLocation = _ownerControl.toDisplay(0, 0);
      final Point ownerSize = _ownerControl.getSize();
      final int ownerWidth = ownerSize.x;
      final int ownerHeight = ownerSize.y;
      final int screenOwnerLeft = screenOwnerControlLocation.x;
      final int screenOwnerTop = screenOwnerControlLocation.y;
      final int screenOwnerBotton = screenOwnerTop + ownerHeight;

      final Point ttSize = _ttShell.getSize();
      final int ttWidth = ttSize.x;
      final int ttHeight = ttSize.y;

      // get edge default values
      final int screenEdgeLeft = screenOwnerLeft;
      final int screenEdgeRight = screenOwnerLeft + ownerWidth - ttWidth;
      final int screenEdgeTop = screenOwnerTop + snapBorder_Top;
      final int screenEdgeBottom = screenOwnerBotton - ttHeight - snapBorder_Bottom;

      final Point screenValuePoint = _ownerControl.toDisplay(
            (int) _ownerValuePoint_DevPosition.x,
            (int) _ownerValuePoint_DevPosition.y);
      final int screenValuePointTop = screenValuePoint.y;

      boolean isSetLocation = isSetDefaultLocation;
      boolean isCheckDefaultOffset = false;
      final Point screenDefaultLocation = new Point(0, 0);

      switch (pinnedLocation) {
      case Screen:

         // use default location when location was not yet set, center the tooltip in the center of the owner
         if (_screenScreenTTShellLocation == null || isSetDefaultLocation) {

            screenDefaultLocation.x = screenOwnerLeft + (ownerWidth / 2) - (ttWidth / 2);
            screenDefaultLocation.y = screenOwnerTop + (ownerHeight / 2) - (ttHeight / 2);

            _screenScreenTTShellLocation = new Point(screenDefaultLocation.x, screenDefaultLocation.y);
         }

         screenDefaultLocation.x = _screenScreenTTShellLocation.x + _ttShellDiff.x;
         screenDefaultLocation.y = _screenScreenTTShellLocation.y + _ttShellDiff.y;

         if (isTTDragged) {

//            if (screenDefaultLocation.y > 1100) {
//               int a = 0;
//               a++;
//            }

            // ensure that the dragged tooltip is within the screen border
            final Point screenNewLocation = fixupDisplayBounds(
                  ttSize,
                  screenDefaultLocation,
                  screenValuePoint,
                  isTTDragged);

            screenDefaultLocation.x = screenNewLocation.x;
            screenDefaultLocation.y = screenNewLocation.y;

            _screenScreenTTShellLocation.x = screenNewLocation.x;
            _screenScreenTTShellLocation.y = screenNewLocation.y;
            _ttShellDiff.x = 0;
            _ttShellDiff.y = 0;
         }

         isCheckDefaultOffset = true;

         break;

      case MouseXPosition:

         if (isTTDragged) {

            // tooltip is currently dragged

            screenDefaultLocation.y = _screenDefaultTTShellLocation.y + _ttShellDiff.y;

         } else {

            int screenTTDefaultY;

            if (_isTTAboveValuePoint) {
               screenTTDefaultY = screenOwnerTop - _devPinnedMouseXPositionRelative - ttHeight;
            } else {
               screenTTDefaultY = screenOwnerBotton - _devPinnedMouseXPositionRelative;
            }

            int devY = screenTTDefaultY;

            if (_isTTAboveValuePoint) {

               // tt must be above value point

               if (screenValuePointTop < (screenTTDefaultY + ttHeight + VALUE_POINT_OFFSET)) {
                  // set above value point
                  devY = screenValuePointTop - VALUE_POINT_OFFSET - ttHeight;
               }

            } else {

               // tt must be below value point

               if (screenValuePointTop > (screenTTDefaultY - VALUE_POINT_OFFSET)) {
                  // set below value point
                  devY = screenValuePointTop + VALUE_POINT_OFFSET;
               }
            }

            screenDefaultLocation.y = devY;

            isSetLocation = true;
         }

         screenDefaultLocation.x = screenOwnerLeft - (ttWidth / 2) + _devXOwnerMouseMove;

         break;

      case TopLeft:
         screenDefaultLocation.x = screenEdgeLeft + _ttShellDiff.x;
         screenDefaultLocation.y = screenEdgeTop + _ttShellDiff.y;
         isCheckDefaultOffset = true;
         break;

      case BottomLeft:
         screenDefaultLocation.x = screenEdgeLeft + _ttShellDiff.x;
         screenDefaultLocation.y = screenEdgeBottom + _ttShellDiff.y;
         isCheckDefaultOffset = true;
         break;

      case BottomRight:
         screenDefaultLocation.x = screenEdgeRight + _ttShellDiff.x;
         screenDefaultLocation.y = screenEdgeBottom + _ttShellDiff.y;
         isCheckDefaultOffset = true;
         break;

      case TopRight:
      default:
         screenDefaultLocation.x = screenEdgeRight + _ttShellDiff.x;
         screenDefaultLocation.y = screenEdgeTop + _ttShellDiff.y;
         isCheckDefaultOffset = true;
      }

      final int defaultY = screenDefaultLocation.y;

      // check if the new tooltip location is covering the value point
      if (isCheckVPCovered && isCheckDefaultOffset) {

         // increase tooltip size
         Rectangle increasedShellArea;
         if (isSetDefaultLocation) {

            increasedShellArea = new Rectangle(//
                  screenDefaultLocation.x - VALUE_POINT_OFFSET,
                  screenDefaultLocation.y - VALUE_POINT_OFFSET,
                  ttSize.x + 2 * VALUE_POINT_OFFSET,
                  ttSize.y + 2 * VALUE_POINT_OFFSET);

         } else {

            increasedShellArea = new Rectangle(//
                  _screenRequestedAnimationLocation.x - VALUE_POINT_OFFSET,
                  _screenRequestedAnimationLocation.y - VALUE_POINT_OFFSET + _defaultOffsetY,
                  ttSize.x + 2 * VALUE_POINT_OFFSET,
                  ttSize.y + 2 * VALUE_POINT_OFFSET);
         }

         // check if value point is hidden by the tooltip
         if (increasedShellArea.contains(screenValuePoint)) {

            int screenUncoveredPosY;

            switch (pinnedLocation) {
            case BottomLeft:
            case BottomRight:

               // show tooltip below the value point
               screenUncoveredPosY = screenValuePointTop + VALUE_POINT_OFFSET;
               break;

            case Screen:
            case TopLeft:
            case TopRight:
            default:
               // show tooltip above the value point
               screenUncoveredPosY = screenValuePointTop - VALUE_POINT_OFFSET - ttHeight;
               break;
            }

            _defaultOffsetY = screenDefaultLocation.y - screenUncoveredPosY;

            screenDefaultLocation.y = screenUncoveredPosY;

         } else {

            // reset, very important !!!
            _defaultOffsetY = 0;
         }
      }

      _screenDefaultTTShellLocation = screenDefaultLocation;

      final Point screenNewLocation = fixupDisplayBounds(ttSize, screenDefaultLocation, screenValuePoint, isTTDragged);

      if (isTTDragged || isAnimation == false) {

         // no animation, set location directly

         _ttShell.setLocation(screenNewLocation);

      } else {

         if (_defaultOffsetY != 0 || _screenRequestedAnimationLocation.y != defaultY || isSetLocation) {

            // move when default location is not yet reached

            _screenRequestedAnimationLocation = screenNewLocation;

            setTTShellLocation10Start();
         }
      }
   }

   /**
    * Move tooltip according to the mouse position.
    *
    * @param devXMouseMove
    * @param devYMouseMove
    * @param valuePoint_DevPosition
    */
   protected void setTTShellLocation(final int devXMouseMove, final int devYMouseMove, final PointLong valuePoint_DevPosition) {

      _devXOwnerMouseMove = devXMouseMove;

      if (valuePoint_DevPosition == null) {
         _ownerValuePoint_DevPosition = new PointLong(0, 0);
      } else {
         _ownerValuePoint_DevPosition = valuePoint_DevPosition;
      }

      setTTShellLocation(false, true, false, true);
   }

   private synchronized void setTTShellLocation10Start() {

      final int oldCounter = _animationCounter;

      _animationCounter = 8;
      _repeatTime = 30;

      // check if animation is already running
      if (oldCounter == 0) {

         // animation is not running, start a new animantion
         _display.syncExec(_ttShellPositioningRunnable);

      } else {

         // do the first movement
         setTTShellLocation20Runnable();
      }
   }

   private void setTTShellLocation20Runnable() {

      if (_animationCounter == 0 || _ownerControl.isDisposed() || _ttShell == null || _ttShell.isDisposed()) {
         return;
      }

      Point nextLocation = null;
      final Point currentLocation = _ttShell.getLocation();

      if (_animationCounter == 1) {

         // this is the last movement, move to the desired location

         nextLocation = _screenRequestedAnimationLocation;

      } else {

         // animate movement

         final Point requestedLocation = _screenRequestedAnimationLocation;

         final float diffX = currentLocation.x - requestedLocation.x;
         final float diffY = currentLocation.y - requestedLocation.y;

         final float stepX = diffX / _animationCounter;
         final float stepY = diffY / _animationCounter;

         final float stepCounter = (_animationCounter - 1) * 1f;

         final float devXRemainder = stepX * stepCounter;
         final float devYRemainder = stepY * stepCounter;

         final float devX = requestedLocation.x + devXRemainder;
         final float devY = requestedLocation.y + devYRemainder;

         nextLocation = new Point((int) devX, (int) devY);
      }

      _ttShell.setLocation(nextLocation);

      _animationCounter--;

      if (_animationCounter > 0) {
         // start new animation
         _display.timerExec(_repeatTime, _ttShellPositioningRunnable);
      }
   }

   /**
    * Should the tooltip displayed because of the given event.
    * <p>
    * <b>Subclasses may overwrite this to get custom behavior</b>
    * </p>
    *
    * @param event
    *           the event
    * @return <code>true</code> if tooltip should be displayed
    */
   protected boolean shouldCreateToolTip(final Event event) {

      final Object ttArea = getToolTipArea(event);

      // No new area close the current tooltip
      if (ttArea == null) {
         hide();
         return false;
      }

      final boolean rv = !ttArea.equals(_currentArea);
      return rv;
   }

   /**
    * This method is called before the tooltip is hidden
    *
    * @param event
    *           the event trying to hide the tooltip
    * @return <code>true</code> if the tooltip should be hidden
    */
   private boolean shouldHideToolTip(final Event event) {

      if (event != null && event.type == SWT.MouseMove) {

         final Object ttArea = getToolTipArea(event);

         // No new area close the current tooltip
         if (ttArea == null) {
            hide();
            return false;
         }

         final boolean rv = !ttArea.equals(_currentArea);
         return rv;
      }

      return true;
   }

   /**
    * Start up the tooltip programmatically
    *
    * @param location
    *           the location relative to the control the tooltip is shown
    */
   public void show(final Point location) {

      /*
       * show tooltip only when this is the active shell, this check is necessary that when a tour
       * chart is opened in a dialog (e.g. adjust altitude) that a hidden tour chart tooltip in the
       * tour chart view is also displayed
       */
      if (_display.getActiveShell() != _ownerControl.getShell() || _ownerControl.isVisible() == false) {
         return;
      }

      final Event event = new Event();
      event.x = location.x;
      event.y = location.y;
      event.widget = _ownerControl;

      toolTipCreate(event);
   }

   private void toolTipCreate(final Event event) {

      if (shouldCreateToolTip(event)) {

         final Shell shell = new Shell(_ownerControl.getShell(), //
               SWT.ON_TOP //
//                     | SWT.TOOL
                     | SWT.NO_FOCUS
                     | SWT.NO_TRIM
         //
         );

         shell.setLayout(new FillLayout());

         toolTipOpen(shell, event);
      }
   }

   private void toolTipHide(final Shell ttShell, final Event event) {

      // initialize next animation, otherwise tooltip would never be displayed again
      _animationCounter = 0;

      if (ttShell == null || ttShell.isDisposed()) {
         return;
      }

      if (shouldHideToolTip(event)) {

         final Shell ownerShell = _ownerControl.getShell();
         ownerShell.removeListener(SWT.Deactivate, _ownerShellListener);
         ownerShell.removeListener(SWT.Move, _ownerShellListener);

         _currentArea = null;

         passOnEvent(ttShell, event);

         ttShell.dispose();
         _ttShell = null;

         return;
      }
   }

   private void toolTipOpen(final Shell shell, final Event event) {

      // Ensure that only one Tooltip is shown in time
      if (_ttShell != null) {
         toolTipHide(_ttShell, null);
      }

      _ttShell = shell;

      // close tooltip if user selects outside of the shell
      _ttShell.addListener(SWT.Deactivate, _ttShellListener);

      final Shell ownerShell = _ownerControl.getShell();
      ownerShell.addListener(SWT.Deactivate, _ownerShellListener);
      ownerShell.addListener(SWT.Move, _ownerShellListener);

      toolTipShow(event);
   }

   private void toolTipShow(final Event event) {

      if (!_ttShell.isDisposed()) {

         _currentArea = getToolTipArea(event);

         createToolTipContentArea(event, _ttShell);

         addTooltipListener(_ttShell);

         _ttShell.pack();

         setTTShellLocation(false, false, false, true);

         _ttShell.setVisible(true);
      }
   }
}
