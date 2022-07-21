/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
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
import org.eclipse.swt.widgets.Widget;

/**
 * Part of this tooltip is copied from {@link org.eclipse.jface.window.ToolTip}.
 */
public abstract class AnimatedToolTipShell {

   public static final int            TOOLTIP_STYLE_RECREATE_CONTENT    = 0;
   public static final int            TOOLTIP_STYLE_KEEP_CONTENT        = 1;

   public static final int            MOUSE_OVER_BEHAVIOUR_NO_IGNORE    = 0;
   public static final int            MOUSE_OVER_BEHAVIOUR_IGNORE_OWNER = 1;

   /**
    * How long each tick is when fading in/out (in ms)
    */
   private static final int           FADE_TIME_INTERVAL                = UI.IS_OSX ? 10 : 10;

   /**
    * Number of steps when fading in
    */
   private static final int           FADE_IN_STEPS                     = 20;

   /**
    * Number of steps when fading out
    */
   private static final int           FADE_OUT_STEPS                    = 10;

   /**
    * Number of steps before fading out
    */
   private static final int           FADE_OUT_DELAY_STEPS              = 20;

   private static final int           MOVE_STEPS                        = 20;

   private static final int           AUTO_CLOSE_INTERVAL               = 700;

   private static final int           ALPHA_OPAQUE                      = 0xff;

   private OwnerControlListener       _ownerControlListener;
   private OwnerShellListener         _ownerShellListener;
   private ToolTipShellListener       _ttShellListener;
   private ToolTipAllControlsListener _ttAllControlsListener;
   private ToolTipDisplayListener     _ttDisplayListener;

   /**
    * When <code>true</code> (default) the tooltip will be moved from the previous location to the
    * current location.
    */
   private boolean                    _isAnimateLocation                = true;

   /**
    * When <code>true</code> the vertical position is not animated and the position is the bottom.
    */
   private boolean                    _isFixedBottomLocation;

   /**
    * Keep track of added display listener that no more than <b>1</b> is set.
    */
   private boolean                    _isDisplayListenerSet;

   /**
    * Is <code>true</code> when shell is fading out, otherwise <code>false</code>.
    */
   private boolean                    _isShellFadingOut;

   /**
    * Is <code>true</code> when shell is fading in, otherwise <code>false</code>.
    */
   private boolean                    _isShellFadingIn;

   /**
    * When <code>true</code> (default) the shell trim style is used to create the tooltip shell
    * which creates a border. Set <code>false</code> to hide the border.
    */
   private boolean                    _isShowShellTrimStyle             = true;

   /**
    * When <code>true</code> then the tooltip is <b>not</b> closed. This can be used when the
    * tooltip opens another dialog which prevents to close this tooltip, default is
    * <code>false</code>.
    */
   private boolean                    _isAnotherDialogOpened;

   private Point                      _shellStartLocation;
   private Point                      _shellEndLocation                 = new Point(0, 0);

   private int                        _fadeOutDelayCounter;

   private final AnimationTimer       _animationTimer;
   private int                        _animationMoveCounter;

   private ToolTipAutoCloseTimer      _ttAutoCloseTimer;

   private boolean                    _isReceiveOnMouseExit;
   private boolean                    _isReceiveOnMouseMove;

   /**
    * Number of {@link #FADE_IN_STEPS} until the tooltip starts to fade in.
    */
   private int                        _fadeInDelaySteps;

   private int                        _fadeInSteps                      = FADE_IN_STEPS;
   private int                        _fadeInDelayCounter;
   private int                        _fadeOutSteps                     = FADE_OUT_STEPS;
   private int                        _fadeOutDelaySteps                = FADE_OUT_DELAY_STEPS;

   /*
    * these settings are modifying the default behaviour which was implemented to show a photo
    * gallery tooltip
    */
   private int     _toolTipStyle                        = TOOLTIP_STYLE_RECREATE_CONTENT;
   private int     _mouseOverBehaviour                  = MOUSE_OVER_BEHAVIOUR_NO_IGNORE;
   private boolean _isKeepToolTipOpenWhenResizedOrMoved = true;

   /*
    * UI resources
    */
   private Display _display;

   /**
    * Tooltip shell which is currently be visible
    */
   private Shell   _shell;

   private Control _ownerControl;

   private final class AnimationTimer implements Runnable {
      @Override
      public void run() {
         animation20_Runnable();
      }
   }

   private class OwnerControlListener implements Listener {
      @Override
      public void handleEvent(final Event event) {
         onOwnerControlEvent(event);
      }
   }

   private final class OwnerShellListener implements Listener {
      @Override
      public void handleEvent(final Event event) {
         onOwnerShellEvent(event);
      }
   }

   /**
    * This listener is added to ALL widgets within the tooltip shell.
    */
   private class ToolTipAllControlsListener implements Listener {
      @Override
      public void handleEvent(final Event event) {
         onTTAllControlsEvent(event);
      }

   }

   /**
    * This checks if a mouse is above a tooltip. When mouse is above the 3D map or outside of this
    * application, mouse events are not captured and a tooltip keeps opened until other actions are
    * done.
    */
   private final class ToolTipAutoCloseTimer implements Runnable {
      @Override
      public void run() {
         onTTAutoCloseTimer();
      }
   }

   private class ToolTipDisplayListener implements Listener {
      @Override
      public void handleEvent(final Event event) {

         switch (event.type) {
         case SWT.MouseMove:
            onDisplayMouseMove();
            break;
         }
      }
   }

   private final class ToolTipShellListener implements Listener {
      @Override
      public void handleEvent(final Event event) {
         onTTShellEvent(event);
      }
   }

   /**
    * Create new instance which add TooltipSupport to the widget
    *
    * @param state
    * @param ownerControl
    *           the control on whose action the tooltip is shown
    */
   public AnimatedToolTipShell(final Control ownerControl) {

      _ownerControl = ownerControl;
      _display = _ownerControl.getDisplay();

      _ttAllControlsListener = new ToolTipAllControlsListener();
      _ttShellListener = new ToolTipShellListener();
      _ttDisplayListener = new ToolTipDisplayListener();

      _ownerControlListener = new OwnerControlListener();
      _ownerShellListener = new OwnerShellListener();

      _animationTimer = new AnimationTimer();
      _ttAutoCloseTimer = new ToolTipAutoCloseTimer();

      addOwnerControlListener();
      addOwnerShellListener();
   }

   private void addDisplayFilterListener() {

      if (_isDisplayListenerSet) {
         return;
      }

      _display.addFilter(SWT.MouseMove, _ttDisplayListener);

      _isDisplayListenerSet = true;
   }

   /**
    * Activate tooltip support for this control
    */
   private void addOwnerControlListener() {

      removeOwnerControlsListener();

      _ownerControl.addListener(SWT.Dispose, _ownerControlListener);
      _ownerControl.addListener(SWT.Resize, _ownerControlListener);
   }

   private void addOwnerShellListener() {

      final Shell ownerShell = _ownerControl.getShell();

      ownerShell.addListener(SWT.Deactivate, _ownerShellListener);
      ownerShell.addListener(SWT.Move, _ownerShellListener);
   }

   /**
    * ########################### Recursive #########################################<br>
    * <p>
    * Add listener to all controls within the tooltip
    * <p>
    * ########################### Recursive #########################################<br>
    *
    * @param control
    */
   private void addTTAllControlsListener(final Control control) {

      control.addListener(SWT.KeyDown, _ttAllControlsListener);

      control.addListener(SWT.MouseDown, _ttAllControlsListener);
      control.addListener(SWT.MouseUp, _ttAllControlsListener);
      control.addListener(SWT.MouseMove, _ttAllControlsListener);
      control.addListener(SWT.MouseExit, _ttAllControlsListener);
      control.addListener(SWT.MouseEnter, _ttAllControlsListener);

      if (control instanceof Composite) {
         final Control[] children = ((Composite) control).getChildren();
         for (final Control child : children) {
            addTTAllControlsListener(child);
         }
      }
   }

   private void addTTShellListener(final Shell shell) {
      // hide tooltip if user selects outside of the shell
      shell.addListener(SWT.Deactivate, _ttShellListener);
      shell.addListener(SWT.Dispose, _ttShellListener);
   }

   private void animation10_Start() {

      int a = 0;
      a++;
      a++;

      if (a == 1) {
         animation10_Start_Simple();
      } else {
         animation10_StartKomplex();
      }
   }

   private void animation10_Start_Simple() {

      if (_isShellFadingIn) {

         // show tool tip

         final Point size = _shell.getSize();
         final Point defaultLocation = getToolTipLocation(size);

         final Point shellLocation = fixupDisplayBounds(size, defaultLocation);

         _shell.setLocation(shellLocation.x, shellLocation.y);
         setShellAlpha(_shell, 0xff);

         setShellVisible(true);

      } else {

         // hide tooltip

         setShellVisible(false);
      }
   }

   private void animation10_StartKomplex() {

//      System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//            + ("\tanimation10_StartKomplex"));
//      // TODO remove SYSTEM.OUT.PRINTLN

//      final long start = System.nanoTime();

      if (_isShellFadingIn) {

         // set fading in location

         final Point shellSize = _shell.getSize();
         Point shellEndLocation = getToolTipLocation(shellSize);
         shellEndLocation = fixupDisplayBounds(shellSize, shellEndLocation);

         final boolean isShellVisible = _shell.isVisible();

         if (shellEndLocation.x == _shellEndLocation.x
               && shellEndLocation.y == _shellEndLocation.y
               && isShellVisible) {

            // shell is already fading in with the correct location

            return;
         }

         // set new end location
         _shellEndLocation = shellEndLocation;

         if (isShellVisible && _isAnimateLocation) {

            // shell is already visible, move from the current position to the target position

            _shellStartLocation = _shell.getLocation();

            if (_isFixedBottomLocation) {

               // do not animate the vertical position

               _shellStartLocation.y = shellEndLocation.y;
            }

         } else {

            // shell is not visible, set position directly without moving animation, do only fading animation

            _fadeInDelayCounter = 0;

            _shellStartLocation = _shellEndLocation;

            _shell.setLocation(_shellStartLocation.x, _shellStartLocation.y);

            setShellAlpha(_shell, 0);
            setShellVisible(true);
         }

      } else if (_isShellFadingOut) {

         // fading out has no movement

         _fadeOutDelayCounter = 0;
      }

      // start animation now
      _animationMoveCounter = 0;
      animation20_Runnable();

//      System.out.println(UI.timeStampNano()
//            + " animation10_StartKomplex\t"
//            + ((float) (System.nanoTime() - start) / 1000000)
//            + " ms");
//      // TODO remove SYSTEM.OUT.PRINTLN
   }

   private void animation20_Runnable() {

//      final long start = System.nanoTime();

      if (isShellHidden()) {
         return;
      }

      try {
         /*
          * endAlpha will be the final fadeIn/fadeOut value when the animation stops
          */
         int finalFadeAlpha = -1;

         int currentAlpha = _shell.getAlpha();
         boolean isLoopBreak = false;

         _animationMoveCounter++;

         while (true) {

            int newAlpha = -1;

            if (_isShellFadingIn) {

               if (_fadeInDelayCounter++ < _fadeInDelaySteps) {

                  // delay fade in

                  _display.timerExec(FADE_TIME_INTERVAL, _animationTimer);

                  return;
               }

               final int shellStartX = _shellStartLocation.x;
               final int shellStartY = _shellStartLocation.y;
               final int shellEndX = _shellEndLocation.x;
               final int shellEndY = _shellEndLocation.y;

               final Point shellCurrentLocation = _shell.getLocation();

               final boolean isInTarget = shellCurrentLocation.x == shellEndX
                     && shellCurrentLocation.y == shellEndY;

               if (_fadeInSteps <= 0) {

                  newAlpha = ALPHA_OPAQUE;

               } else {

                  final int diffAlpha = ALPHA_OPAQUE / _fadeInSteps;

                  newAlpha = currentAlpha + diffAlpha;

                  if (newAlpha > ALPHA_OPAQUE) {
                     newAlpha = ALPHA_OPAQUE;
                  }
               }

               finalFadeAlpha = ALPHA_OPAQUE;

               if (isInTarget && currentAlpha == ALPHA_OPAQUE) {

                  // target is reached and fully visible, stop animation

                  _isShellFadingIn = false;

                  return;

               } else {

                  if (isInTarget == false) {

                     // move to target

                     final int diffX = shellStartX - shellEndX;
                     final int diffY = shellStartY - shellEndY;

                     final int moveX = (int) ((double) diffX / MOVE_STEPS * _animationMoveCounter);
                     final int moveY = (int) ((double) diffY / MOVE_STEPS * _animationMoveCounter);

                     int shellCurrentX = shellStartX - moveX;
                     int shellCurrentY = shellStartY - moveY;

                     /**
                      * This wrong behaviour occured (not very often but it occured) that the
                      * tooltip was moving to the invinity of the right or the left.
                      * <p>
                      * This check will prevent this bug.
                      */
                     if ((shellStartX - shellEndX < 0 && shellCurrentX > shellEndX)
                           || (shellStartX - shellEndX > 0 && shellCurrentX < shellEndX)) {

                        shellCurrentX = shellEndX;
                     }

                     if ((shellStartY - shellEndY < 0 && shellCurrentY > shellEndY)
                           || (shellStartY - shellEndY > 0 && shellCurrentY < shellEndY)) {

                        shellCurrentY = shellEndY;
                     }

                     _shell.setLocation(shellCurrentX, shellCurrentY);
                  }
               }

            } else if (_isShellFadingOut) {

               if (_fadeOutDelayCounter++ < _fadeOutDelaySteps) {

                  // delay fade out

                  _display.timerExec(FADE_TIME_INTERVAL, _animationTimer);

                  return;
               }

               if (_fadeInSteps <= 0) {

                  newAlpha = 0;

               } else {

                  final int alphaDiff = ALPHA_OPAQUE / _fadeOutSteps;

                  newAlpha = currentAlpha - alphaDiff;
                  finalFadeAlpha = 0;
               }

               if (newAlpha <= 0) {

                  // shell is not visible any more, hide it now

                  // hide shell
                  setShellVisible(false);

                  _isShellFadingOut = false;

                  return;
               }
            }

            if (newAlpha == -1) {

               return;

            } else {

               if (newAlpha != currentAlpha) {
                  setShellAlpha(_shell, newAlpha);
               }

               if (_shell.getAlpha() != newAlpha) {

                  // platform do not support shell alpha, this occured on Ubuntu 12.04

                  if (isLoopBreak) {
                     break;
                  }

                  // loop only once
                  isLoopBreak = true;

                  currentAlpha = finalFadeAlpha;

                  continue;

               } else {

                  _display.timerExec(FADE_TIME_INTERVAL, _animationTimer);

                  break;
               }
            }
         }

      } catch (final Exception err) {
         StatusUtil.log(err);
      } finally {

//         final float timeDiff = (float) (System.nanoTime() - start) / 1000000;
//         System.out.println(UI.timeStampNano() + " animation20_Runnable:\t" + timeDiff + " ms\t" + " ms");
//         // TODO remove SYSTEM.OUT.PRINTLN
      }
   }

   /**
    * Is called before the tooltip shell is set hidden, the default is doing nothing.
    */
   protected void beforeHideToolTip() {

      // do nothing
   }

   /**
    * Is called before the tooltip is displayed.
    *
    * @return Returns <code>true</code> when the tooltip should be opened, otherwise
    *         <code>false</code>.
    */
   protected abstract boolean canShowToolTip();

   /**
    * Close tooltip immediatedly without animation.
    */
   public void close() {

      if (_shell != null && _shell.isDisposed() == false) {

         _shell.close();
         _shell = null;
      }
   }

   /**
    * @return Returns <code>true</code> to close the shell after it is completely hidden.
    */
   protected boolean closeShellAfterHidden() {

      return false;
   }

   /**
    * Creates the content area of the the tooltip.
    *
    * @param shell
    *           the parent of the content area
    * @return the content area created
    */
   protected abstract Composite createToolTipContentArea(Composite shell);

   /**
    * Create a shell but do not display it
    *
    * @return Returns <code>true</code> when shell is created.
    */
   private void createUI() {

      final boolean isRecreateContent = _toolTipStyle == TOOLTIP_STYLE_RECREATE_CONTENT;
      boolean isShellCreated = false;
      boolean isCreateContent = false;

      final int trimStyle = _isShowShellTrimStyle ? 0 : SWT.NO_TRIM;

      if (_shell == null || _shell.isDisposed()) {

         /*
          * create shell
          */
         final int shellStyle = SWT.ON_TOP //
         /*
          * SWT.TOOL must be disabled that NO_FOCUS is working !!!
          */
//               | SWT.TOOL
//               | SWT.RESIZE
               | SWT.NO_FOCUS //
               | trimStyle;

         _shell = new Shell(_ownerControl.getShell(), shellStyle);

         _shell.setLayout(new FillLayout());

         addTTShellListener(_shell);

         isShellCreated = true;

      } else {

         if (isRecreateContent) {

            // hide previous tooltip content

            _shell.setRedraw(false);

            final Control[] shellChildren = _shell.getChildren();
            for (final Control control : shellChildren) {
               control.dispose();
            }

            isCreateContent = true;
         }
      }

      final boolean isNewContent = isShellCreated || isCreateContent;

      if (isNewContent) {

         // create content
         createToolTipContentArea(_shell);
      }

      onAfterCreateUI();

      _shell.layout();
      _shell.pack(true);

      _shell.setRedraw(true);

      if (isNewContent) {
         addTTAllControlsListener(_shell);
      }
   }

   private Point fixupDisplayBounds(final Point tipSize, final Point location) {

      final Rectangle displayBounds = getDisplayBounds(location);
      final Point rightBottomBounds = new Point(tipSize.x + location.x, tipSize.y + location.y);

      if (!(displayBounds.contains(location) && displayBounds.contains(rightBottomBounds))) {

         if (rightBottomBounds.x > displayBounds.x + displayBounds.width) {
            location.x -= rightBottomBounds.x - (displayBounds.x + displayBounds.width);
         }

         if (rightBottomBounds.y > displayBounds.y + displayBounds.height) {
            location.y -= rightBottomBounds.y - (displayBounds.y + displayBounds.height);
         }

         if (location.x < displayBounds.x) {
            location.x = displayBounds.x;
         }

         if (location.y < displayBounds.y) {
            location.y = displayBounds.y;
         }
      }

      return location;
   }

   private Rectangle getDisplayBounds(final Point location) {

      Rectangle displayBounds;
      final Monitor[] allMonitors = _ownerControl.getDisplay().getMonitors();

      if (allMonitors.length > 1) {
         // By default present in the monitor of the control
         displayBounds = _ownerControl.getMonitor().getBounds();
         final Point p = new Point(location.x, location.y);

         // Search on which monitor the event occurred
         Rectangle tmp;
         for (final Monitor element : allMonitors) {
            tmp = element.getBounds();
            if (tmp.contains(p)) {
               displayBounds = tmp;
               break;
            }
         }

      } else {
         displayBounds = _ownerControl.getDisplay().getBounds();
      }

      return displayBounds;
   }

   /**
    * Get tooltip location.
    *
    * @param size
    *           Tooltip size
    * @return Returns location relative to the device.
    */
   protected abstract Point getToolTipLocation(Point size);

   /**
    * Hide tool tip with animation.
    */
   public void hide() {
      ttHide();
   }

   /**
    * Hide tool tip without delay.
    */
   public void hideNow() {

      if (isShellHidden()) {
         return;
      }

      setShellAlpha(_shell, 0);

      // hide shell
      setShellVisible(false);
   }

   /**
    * @return When <code>true</code> then the tooltip is <b>not</b> closed. This can be used when
    *         the tooltip opens another dialog which prevents to close this tooltip, default is
    *         <code>false</code>.
    */
   public boolean isAnotherDialogOpened() {
      return _isAnotherDialogOpened;
   }

   /**
    * @param displayCursorLocation
    * @return Returns <code>true</code> when the tooltip should not be closed.
    */
   protected boolean isInNoHideArea(final Point displayCursorLocation) {
      return false;
   }

   /**
    * @return Return <code>true</code> when the tooltip shell is <code>null</code>, disposed or not
    *         visible.
    */
   private boolean isShellHidden() {

      return _shell == null || _shell.isDisposed() || _shell.isVisible() == false;
   }

   /**
    * @return Returns <code>true</code> when the tooltip is opened but is started to close it.
    */
   public boolean isTooltipClosing() {

      return _isShellFadingOut && isToolTipVisible();
   }

   public boolean isToolTipVisible() {

      if (_shell == null || _shell.isDisposed()) {
         return false;
      }

      final boolean isShellVisible = _shell.isVisible();

//      System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//            + ("\tisShellVisible: " + isShellVisible));
//      // TODO remove SYSTEM.OUT.PRINTLN

      return isShellVisible;
   }

   /**
    * @return When the returned rectangle (which has display locations) is hit by the mouse, the
    *         tooltip should not be hidden. When <code>null</code> this check is ignored.
    */
   protected Rectangle noHideOnMouseMove() {
      return null;
   }

   /**
    * Is called after the UI is created but before it is packed.
    */
   protected void onAfterCreateUI() {
      // default do nothing
   }

   /**
    * @return Returns <code>true</code> when the tooltip should be kept opened.
    */
   private boolean onDisplayMouseMove() {

//      final long start = System.nanoTime();

      if (isShellHidden()) {
         return false;
      }

      if (isAnotherDialogOpened()) {

//         System.out.println(UI.timeStampNano()
//               + " ["
//               + getClass().getSimpleName()
//               + "] onDisplayMouseMove::canCloseToolTip\t");
//         // TODO remove SYSTEM.OUT.PRINTLN

         return true;
      }

      boolean isHide = false;
      boolean isKeepVisible = false;

      // get control which is hovered with the mouse after the exit, can be null
      final Control hoveredControl = _display.getCursorControl();

//      System.out.println(UI.timeStampNano() + " onTTDisplayMouseMove - hoveredControl " + hoveredControl);
//      // TODO remove SYSTEM.OUT.PRINTLN

      if (hoveredControl == null) {

//         System.out.println(UI.timeStampNano() + " exit 0 hide");
//         // TODO remove SYSTEM.OUT.PRINTLN

         isHide = true;

      } else {

         /*
          * check if the hovered control is the owner control, if not, hide the tooltip
          */
         Control hoveredParent = hoveredControl;

         // move up child-parent hierarchy until shell is reached
         while (true) {

            if (hoveredParent == _shell) {

               // mouse is hovering in this tooltip

               isKeepVisible = true;

//               System.out.println(UI.timeStampNano() + " exit 1 no hide");
//               // TODO remove SYSTEM.OUT.PRINTLN

               break;
            }

            if (hoveredParent == _ownerControl) {

               // mouse is hovering the owner control

               if (_mouseOverBehaviour == MOUSE_OVER_BEHAVIOUR_NO_IGNORE) {

                  /*
                   * owner is not ignored, which means when the mouse is hovered the owner,
                   * the tooltip keeps opened, this is the default
                   */

                  isKeepVisible = true;
               }

//               System.out.println(UI.timeStampNano() + " exit 2 no hide");
//               // TODO remove SYSTEM.OUT.PRINTLN

               break;
            }

            hoveredParent = hoveredParent.getParent();

            if (hoveredParent == null) {

               // mouse has left the tooltip and the owner control

//               System.out.println(UI.timeStampNano() + " exit 3 hide");
//               // TODO remove SYSTEM.OUT.PRINTLN

               isHide = true;

               break;
            }
         }
      }

      /**
       * !!! This adjustment do not work on Linux because the tooltip gets hidden when the mouse
       * tries to mover over the tooltip <br>
       * <br>
       * It seems to work on windows and linux with margin 1, when set to 0 the tooltip do
       * sometime not be poped up again and the i-icons is not deaktivated<br>
       * wolfgang 2010-07-23
       */

      final Rectangle ttShellRect = _shell.getBounds();

      final int margin = 20;

      ttShellRect.x -= margin;
      ttShellRect.width += 2 * margin;
      ttShellRect.height += margin;

      final Point displayCursorLocation = _display.getCursorLocation();

      final boolean isInTooltip = ttShellRect.contains(displayCursorLocation);

      if (isKeepVisible == false && isHide == false && isInTooltip == false) {
         isHide = true;
      }

      boolean isKeepOpened = true;

      if (isInTooltip && _isShellFadingOut) {

         // don't hide when mouse is hovering hiding tooltip

         ttShow();

      } else if (isHide) {

         final boolean isInNoHideArea = isInNoHideArea(displayCursorLocation);

         if (isInNoHideArea) {

            // keep open

         } else {

            final Rectangle noHideArea = noHideOnMouseMove();

            if (noHideArea == null || noHideArea.contains(displayCursorLocation) == false) {

               // hide definitively

               if (_isShellFadingOut) {

                  // do nothing, wait until it is finally hidden

                  hideNow();

               } else {

                  ttHide();
               }

               isKeepOpened = false;
            }
         }
      }

//      System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//            + ("\tdisplayCursorLocation:" + displayCursorLocation));
//      // TODO remove SYSTEM.OUT.PRINTLN

      return isKeepOpened;

   }

   /**
    * Is called when the shell is disposed.
    */
   protected void onDispose() {}

   private void onDispose(final Event event) {

      if (_shell == null || _shell.isDisposed()) {
         return;
      }

      // hide tooltip definitively

      removeOwnerShellListener();
      removeDisplayFilterListener();

      // deactivate auto close timer
      _display.timerExec(-1, _ttAutoCloseTimer);

      _shell.dispose();
   }

   protected void onMouseExitToolTip(final MouseEvent mouseEvent) {}

   protected void onMouseMoveInToolTip(final MouseEvent mouseEvent) {}

   private void onOwnerControlEvent(final Event event) {

      if (_ownerControl == null || _ownerControl.isDisposed()) {
         return;
      }

      switch (event.type) {
      case SWT.Dispose:

         onDispose(event);

         removeOwnerControlsListener();

         break;

      case SWT.Resize:

         showShellWhenVisible();

         break;
      }
   }

   private void onOwnerShellEvent(final Event event) {

      if (_shell == null || _shell.isDisposed()) {
         return;
      }

      switch (event.type) {
      case SWT.Deactivate:

         _display.asyncExec(() -> {

            // hide tooltip when another shell is activated

            if (_display.getActiveShell() != _shell) {
               ttHide();
            }
         });

         break;

      case SWT.Move:

         showShellWhenVisible();

         break;
      }
   }

   public void onReparentShell(final Shell reparentedShell) {

      /*
       * reparenting a shells parent which is a shell do NOT work
       */

      if (_shell != null && _shell.isDisposed() == false && _shell.isVisible()) {
         _shell.setVisible(false);
      }

      // stop animation
      _isShellFadingIn = _isShellFadingOut = false;
   }

   private void onTTAllControlsEvent(final Event event) {

      if (_shell == null || _shell.isDisposed()) {
         return;
      }

      switch (event.type) {
      case SWT.KeyDown:

         if (event.keyCode == SWT.ESC) {
            hide();
         }

         break;

      case SWT.MouseEnter:

         if (_isShellFadingIn || _isShellFadingOut) {

            // stop animation
//            _isShellFadingIn = _isShellFadingOut = false;
         }

         break;

      case SWT.MouseExit:

         if (_isReceiveOnMouseExit) {

            final Widget widget = event.widget;

            if (widget instanceof Control) {

               final Point ttDisplayLocation = ((Control) widget).toDisplay(event.x, event.y);
               final Point ownerLocation = _ownerControl.toControl(ttDisplayLocation);

               _display.asyncExec(() -> {

                  final MouseEvent mouseEvent = new MouseEvent(event);
                  mouseEvent.x = ownerLocation.x;
                  mouseEvent.y = ownerLocation.y;

                  onMouseExitToolTip(mouseEvent);
               });
            }
         }
         break;

      case SWT.MouseMove:

         if (_isReceiveOnMouseMove) {

            final Widget widget = event.widget;

            if (widget instanceof Control) {

               final Point ttDisplayLocation = ((Control) widget).toDisplay(event.x, event.y);
               final Point ownerLocation = _ownerControl.toControl(ttDisplayLocation);

               _display.asyncExec(() -> {

                  final MouseEvent mouseEvent = new MouseEvent(event);
                  mouseEvent.x = ownerLocation.x;
                  mouseEvent.y = ownerLocation.y;

                  onMouseMoveInToolTip(mouseEvent);
               });
            }
         }

         break;
      }
   }

   private void onTTAutoCloseTimer() {

//      System.out.println(UI.timeStampNano() + " onTTAutoCloseTimer\t");
//      // TODO remove SYSTEM.OUT.PRINTLN

      final boolean isKeepOpened = onDisplayMouseMove();

      if (isKeepOpened) {

         // start again to check again
         _display.timerExec(AUTO_CLOSE_INTERVAL, _ttAutoCloseTimer);
      }
   }

   private void onTTShellEvent(final Event event) {

      switch (event.type) {
      case SWT.Deactivate:

         if (_shell != null && !_shell.isDisposed() && _ownerControl != null && !_ownerControl.isDisposed()) {

            _display.asyncExec(() -> {

               // hide tooltip when another shell is activated

               // check again
               if (_shell == null
                     || _shell.isDisposed()
                     || _ownerControl == null
                     || _ownerControl.isDisposed()) {
                  return;
               }

               if (// don't hide when ...

               // ... main window is active
               _ownerControl.getShell() == _shell.getDisplay().getActiveShell()

                     // ... a sub shell is opened
                     || isAnotherDialogOpened()) {

                  return;
               }

               ttHide();
            });
         }

         break;

      case SWT.Dispose:

         onDispose();

         break;
      }
   }

   private void removeDisplayFilterListener() {

      if (_isDisplayListenerSet == false) {
         return;
      }

      _display.removeFilter(SWT.MouseMove, _ttDisplayListener);

      _isDisplayListenerSet = false;
   }

   /**
    * Deactivate tooltip support for the underlying control
    */
   private void removeOwnerControlsListener() {

      _ownerControl.removeListener(SWT.Dispose, _ownerControlListener);
      _ownerControl.removeListener(SWT.Resize, _ownerControlListener);
   }

   private void removeOwnerShellListener() {

      final Shell ownerShell = _ownerControl.getShell();

      ownerShell.removeListener(SWT.Deactivate, _ownerShellListener);
      ownerShell.removeListener(SWT.Move, _ownerShellListener);
   }

   protected void setBehaviourOnMouseOver(final int mouseOverBehaviour) {
      _mouseOverBehaviour = mouseOverBehaviour;
   }

   public void setFadeInDelayTime(final int delayTimeInMS) {
      _fadeInDelaySteps = delayTimeInMS / FADE_TIME_INTERVAL;
   }

   public void setFadeInSteps(final int fadeInSteps) {
      _fadeInSteps = fadeInSteps;
   }

   public void setFadeOutDelaySteps(final int fadeOutDelaySteps) {
      _fadeOutDelaySteps = fadeOutDelaySteps;
   }

   public void setFadeOutSteps(final int fadeOutSteps) {
      _fadeOutSteps = fadeOutSteps;
   }

   /**
    * When <code>true</code> (default) the tooltip will be moved from the previous location to the
    * current location.
    */
   public void setIsAnimateLocation(final boolean isAnimateLocation) {
      _isAnimateLocation = isAnimateLocation;
   }

   /**
    * @param isAnotherDialogOpened
    *           When <code>true</code> then this tooltip will not be closed until
    *           <code>false</code> is set.
    *           <p>
    *           This is used to keep the tooltip opened until a subshell is closed which were
    *           opened in the tooltip.
    */
   public void setIsAnotherDialogOpened(final boolean isAnotherDialogOpened) {
      _isAnotherDialogOpened = isAnotherDialogOpened;
   }

   public void setIsFixedBottomLocation(final boolean isFixedBottomLocation) {
      _isFixedBottomLocation = isFixedBottomLocation;
   }

   public void setIsKeepShellOpenWhenMoved(final boolean isKeepShellOpenWhenMoved) {
      _isKeepToolTipOpenWhenResizedOrMoved = isKeepShellOpenWhenMoved;
   }

   /**
    * When <code>true</code> (default) the shell trim style is used to create the tooltip shell
    * which creates a border. Set <code>false</code> to hide the border.
    */
   public void setIsShowShellTrimStyle(final boolean isShowShellTrimStyle) {
      _isShowShellTrimStyle = isShowShellTrimStyle;
   }

   public void setReceiveMouseExitEvent(final boolean isReceive) {
      _isReceiveOnMouseExit = isReceive;
   }

   public void setReceiveMouseMoveEvent(final boolean isReceive) {
      _isReceiveOnMouseMove = isReceive;
   }

   private void setShellAlpha(final Shell shell, final int alpha) {

//      System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//            + ("\tshell: " + _shell.hashCode())
//            + ("\tsetShellAlpha: " + alpha));
//      // TODO remove SYSTEM.OUT.PRINTLN

      shell.setAlpha(alpha);
   }

   private void setShellVisible(final boolean isVisible) {

//      System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//            + ("\tshell: " + _shell.hashCode())
//            + ("\tsetShellVisible: " + isVisible));
//      // TODO remove SYSTEM.OUT.PRINTLN

      if (isVisible) {

         // show tooltip

         _shell.setVisible(true);

         addDisplayFilterListener();

      } else {

         // hide tooltip

         beforeHideToolTip();

         _shell.setVisible(false);

         removeDisplayFilterListener();

         if (closeShellAfterHidden()) {
            close();
         }
      }
   }

   /**
    * Set's the tooltip style, default is
    * {@link AnimatedToolTipShell#TOOLTIP_STYLE_RECREATE_CONTENT}.
    *
    * @param toolTipStyle
    *           This is the style how the tooltip content is created.
    *           <p>
    *           Possible values:<br>
    *           {@link AnimatedToolTipShell#TOOLTIP_STYLE_RECREATE_CONTENT}
    *           {@link AnimatedToolTipShell#TOOLTIP_STYLE_KEEP_CONTENT}
    */
   protected void setToolTipCreateStyle(final int toolTipStyle) {
      _toolTipStyle = toolTipStyle;
   }

   private void showShellWhenVisible() {

      if (_isKeepToolTipOpenWhenResizedOrMoved == false) {
         return;
      }

      if (isShellHidden()) {
         return;
      }

      ttShow();
   }

   protected void showToolTip() {

      /*
       * show tooltip only when this is the active shell, this check is necessary that when a tour
       * chart is opened in a dialog (e.g. adjust altitude) that a hidden tour chart tooltip in
       * the tour chart view is also displayed
       */
//      if (_display.getActiveShell() != _ownerControl.getShell() || _ownerControl.isVisible() == false) {
//         return false;
//      }

      createUI();

      ttShow();
   }

   private void ttHide() {

      if (isShellHidden()) {
         return;
      }

      // prevent closing when a sub shell is opened
      if (isAnotherDialogOpened()) {
         return;
      }

      if (_fadeInDelayCounter < _fadeInDelaySteps) {

         // fading in have not yet started -> hide shell
         setShellVisible(false);

         return;
      }

      if (_isShellFadingOut) {

         // shell is already fading out
         return;
      }

      // shell is not yet fading out

      _isShellFadingIn = false;
      _isShellFadingOut = true;

      animation10_Start();
   }

   private void ttShow() {

      // shell is not yet fading in

      if (canShowToolTip() == false) {

         return;
      }

      // ensure tooltip is closed when mouse is not hovering the tooltip or application
      _display.timerExec(AUTO_CLOSE_INTERVAL, _ttAutoCloseTimer);

      _isShellFadingIn = true;
      _isShellFadingOut = false;

      animation10_Start();
   }

}
