/*******************************************************************************
 * Copyright (C) 2005, 2020  Wolfgang Schramm and Contributors
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
package net.tourbook.map3.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import javax.tools.ToolProvider;

import net.tourbook.common.UI;
import net.tourbook.map3.view.Map3Manager;

import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
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

/**
 * <p>
 * Part of this tooltip is copied from org.eclipse.jface.window.ToolTip
 */
public abstract class ToolTip3 {

   public static final int    SHELL_MARGIN                      = 5;

   public static final String SHELL_DATA_TOOL                   = "SHELL_DATA_TOOL"; //$NON-NLS-1$

   public static final int    TOOLTIP_STYLE_RECREATE_CONTENT    = 0;
   public static final int    TOOLTIP_STYLE_KEEP_CONTENT        = 1;

   public static final int    MOUSE_OVER_BEHAVIOUR_NO_IGNORE    = 0;
   public static final int    MOUSE_OVER_BEHAVIOUR_IGNORE_OWNER = 1;

   /**
    * how long each tick is when fading in/out (in ms)
    */
   //private static final int				FADE_TIME_INTERVAL					= UI.IS_OSX ? 10 : 10;

   /**
    * Number of steps when fading in
    */
//   private static final int FADE_IN_STEPS = 20;

   /**
    * Number of steps when fading out
    */
   //private static final int				FADE_OUT_STEPS						= 10;

   /**
    * Number of steps before fading out
    */
   //private static final int				FADE_OUT_DELAY_STEPS				= 20;

   //private static final int				MOVE_STEPS							= 20;

   //private static final int				ALPHA_OPAQUE						= 0xff;

   private static final int           MOUSE_HOVER_DELAY   = 10;
   private static final int           AUTO_CLOSE_INTERVAL = 700;

   private OwnerControlListener       _ownerControlListener;
   private OwnerShellListener         _ownerShellListener;
   private ToolTipShellListener       _ttShellListener;
   private ToolTipAllControlsListener _ttAllControlsListener;
   private DisplayFilterListener      _displayFilterListener;

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

//	private Point							_shellStartLocation;
//	private Point							_shellEndLocation					= new Point(0, 0);
//
//	private int								_fadeOutDelayCounter;
//
//	private final AnimationTimer			_animationTimer;
//	private int								_animationMoveCounter;
//
//	private int								_fadeInSteps						= FADE_IN_STEPS;

   private int _mouseOverBehaviour = MOUSE_OVER_BEHAVIOUR_NO_IGNORE;

   /*
    * UI resources
    */
   private Display                       _display;

   private Control                       _ownerControl;

   private OwnerHoverTimer               _ownerHoverTimer;
   private Point                         _ownerHoverPosition;
   private ToolTipAutoCloseTimer         _ttAutoCloseTimer;

   private Cursor                        _cursorDragged;
   private Cursor                        _cursorHand;

   /**
    * Contains all tooltips. Key is the tooltip area for which the tooltip is displayed.
    */
   private HashMap<Object, ToolTip3Tool> _allTools = new HashMap<>();

   /**
    * Tooltip tool which is currently be hovered.
    */
   private ToolTip3Tool                  _hoveredTool;

//   private final class AnimationTimer implements Runnable {
//      @Override
//      public void run() {
//         animation20_Runnable();
//      }
//   }

   private class DisplayFilterListener implements Listener {
      @Override
      public void handleEvent(final Event event) {

         if (event.type == SWT.MouseMove) {
            onDisplayMouseMove();
         }
      }
   }

   private class OwnerControlListener implements Listener {
      @Override
      public void handleEvent(final Event event) {
         onOwnerEventControl(event);
      }
   }

   /**
    * This works like the {@link SWT#MouseHover} event but the time until a tooltip is opened is
    * tooooo slow in the original implementation. In this implementation the open delay can be
    * customized.
    */
   private final class OwnerHoverTimer implements Runnable {
      @Override
      public void run() {
         onOwnerHovered(null);
      }
   }

   private final class OwnerShellListener implements Listener {
      @Override
      public void handleEvent(final Event event) {
         onOwnerEventShell(event);
      }
   }

   /**
    * This listener is added to ALL widgets within the tooltip shell.
    */
   private class ToolTipAllControlsListener implements Listener {
      @Override
      public void handleEvent(final Event event) {
         onTTControlEvent(event);
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
   public ToolTip3(final Control ownerControl) {

      _ownerControl = ownerControl;
      _display = _ownerControl.getDisplay();

      _ttAllControlsListener = new ToolTipAllControlsListener();
      _ttShellListener = new ToolTipShellListener();
      _displayFilterListener = new DisplayFilterListener();

      _ownerControlListener = new OwnerControlListener();
      _ownerShellListener = new OwnerShellListener();

//		_animationTimer = new AnimationTimer();
      _ownerHoverTimer = new OwnerHoverTimer();
      _ttAutoCloseTimer = new ToolTipAutoCloseTimer();

      addOwnerControlListener();
      addOwnerShellListener();

      _cursorDragged = new Cursor(_display, SWT.CURSOR_SIZEALL);
      _cursorHand = new Cursor(_display, SWT.CURSOR_HAND);
   }

   private void addDisplayFilterListener() {

      if (_isDisplayListenerSet == false) {

         _display.addFilter(SWT.MouseMove, _displayFilterListener);

         _isDisplayListenerSet = true;
      }
   }

   /**
    * Activate tooltip support for this control
    */
   private void addOwnerControlListener() {

      removeOwnerControlsListener();

      _ownerControl.addListener(SWT.Dispose, _ownerControlListener);
      _ownerControl.addListener(SWT.MouseExit, _ownerControlListener);
      _ownerControl.addListener(SWT.MouseMove, _ownerControlListener);
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

//		final int a = 1;

//		if (a == 1) {
      animation10_Start_Simple();
//		} else {
//			animation10_StartKomplex();
//		}
   }

   private void animation10_Start_Simple() {

//		System.out.println(UI.timeStampNano() + " animation10_Start_Simple\t");
//		// TODO remove SYSTEM.OUT.PRINTLN

      final Shell activeShell = getHoveredShell();
      if (activeShell == null) {
         return;
      }

      // close other shells
      closeOthers(activeShell);

      if (_isShellFadingIn) {

         // show tool tip

         final Point shellSize = activeShell.getSize();

         Point defaultLocation = null;
         boolean isInitialLocation = false;

         if (_hoveredTool.isVisible()) {
            defaultLocation = _hoveredTool.getInitialLocation();
            isInitialLocation = true;
         }

         if (defaultLocation == null) {
            defaultLocation = getToolTipLocation(shellSize, _ownerHoverPosition, _hoveredTool.getToolProvider());
         }

         if (defaultLocation == null) {
            return;
         }

         final Point shellLocation = fixupDisplayBounds(shellSize, defaultLocation);

         activeShell.setLocation(shellLocation.x, shellLocation.y);
         activeShell.setAlpha(0xff);

         setShellVisible(activeShell, true);

         _hoveredTool.setDefaultLocation(isInitialLocation ? null : defaultLocation);

      } else {

         // hide tooltip

         setShellVisible(activeShell, false);
      }
   }

//   private void animation20_Runnable() {

//		final long start = System.nanoTime();

//		if (_shell == null || _shell.isDisposed() || _shell.isVisible() == false) {
//			return;
//		}
//
//		try {
//			/*
//			 * endAlpha will be the final fadeIn/fadeOut value when the animation stops
//			 */
//			int finalFadeAlpha = -1;
//
//			int currentAlpha = _shell.getAlpha();
//			boolean isLoopBreak = false;
//
//			_animationMoveCounter++;
//
//			while (true) {
//
//				int newAlpha = -1;
//
//				if (_isShellFadingIn) {
//
//					final int shellStartX = _shellStartLocation.x;
//					final int shellStartY = _shellStartLocation.y;
//					final int shellEndX = _shellEndLocation.x;
//					final int shellEndY = _shellEndLocation.y;
//
//					final Point shellCurrentLocation = _shell.getLocation();
//
//					final boolean isInTarget = shellCurrentLocation.x == shellEndX
//							&& shellCurrentLocation.y == shellEndY;
//
//					final int diffAlpha = ALPHA_OPAQUE / _fadeInSteps;
//
//					newAlpha = currentAlpha + diffAlpha;
//					if (newAlpha > ALPHA_OPAQUE) {
//						newAlpha = ALPHA_OPAQUE;
//					}
//					finalFadeAlpha = ALPHA_OPAQUE;
//
//					if (isInTarget && currentAlpha == ALPHA_OPAQUE) {
//
//						// target is reached and fully visible, stop animation
//
//						_isShellFadingIn = false;
//
//						return;
//
//					} else {
//
//						if (isInTarget == false) {
//
//							// move to target
//
//							final int diffX = shellStartX - shellEndX;
//							final int diffY = shellStartY - shellEndY;
//
//							final double moveX = (double) diffX / MOVE_STEPS * _animationMoveCounter;
//							final double moveY = (double) diffY / MOVE_STEPS * _animationMoveCounter;
//
//							final int shellCurrentX = (int) (shellStartX - moveX);
//							final int shellCurrentY = (int) (shellStartY - moveY);
//
//							_shell.setLocation(shellCurrentX, shellCurrentY);
//						}
//					}
//
//				} else if (_isShellFadingOut) {
//
//					if (_fadeOutDelayCounter++ < FADE_OUT_DELAY_STEPS) {
//
//						// delay fade out
//
//						_display.timerExec(FADE_TIME_INTERVAL, _animationTimer);
//
//						return;
//					}
//
//					final int alphaDiff = ALPHA_OPAQUE / FADE_OUT_STEPS;
//
//					newAlpha = currentAlpha - alphaDiff;
//					finalFadeAlpha = 0;
//
//					if (newAlpha <= 0) {
//
//						// shell is not visible any more, hide it now
//
//						_shell.setAlpha(0);
//
//						// hide shell
//						setShellVisible(false);
//
//						_isShellFadingOut = false;
//
//						return;
//					}
//				}
//
//				if (newAlpha == -1) {
//
//					return;
//
//				} else {
//
//					if (newAlpha != currentAlpha) {
//						_shell.setAlpha(newAlpha);
//					}
//
//					if (_shell.getAlpha() != newAlpha) {
//
//						// platform do not support shell alpha, this occured on Ubuntu 12.04
//
//						if (isLoopBreak) {
//							break;
//						}
//
//						// loop only once
//						isLoopBreak = true;
//
//						currentAlpha = finalFadeAlpha;
//
//						continue;
//
//					} else {
//
//						_display.timerExec(FADE_TIME_INTERVAL, _animationTimer);
//
//						break;
//					}
//
//				}
//			}
//
//		} catch (final Exception err) {
//			StatusUtil.log(err);
//		} finally {
//
////			final float timeDiff = (float) (System.nanoTime() - start) / 1000000;
////			System.out.println(UI.timeStampNano() + " animation20_Runnable:\t" + timeDiff + " ms\t" + " ms");
////			// TODO remove SYSTEM.OUT.PRINTLN
//		}
//   }

   /**
    * Is called before the tooltip shell is set to hidden.
    */
   protected void beforeHideToolTip() {}

//
//	private void animation10_StartKomplex() {
//
////		final long start = System.nanoTime();
//
//		if (_isShellFadingIn) {
//
//			// set fading in location
//
//			final Point shellSize = _shell.getSize();
//			Point shellEndLocation = getToolTipLocation(shellSize);
//			shellEndLocation = fixupDisplayBounds(shellSize, shellEndLocation);
//
//			final boolean isShellVisible = _shell.isVisible();
//
//			if (shellEndLocation.x == _shellEndLocation.x
//					&& shellEndLocation.y == _shellEndLocation.y
//					&& isShellVisible) {
//
//				// shell is already fading in with the correct location
//
//				return;
//			}
//
//			// set new end location
//			_shellEndLocation = shellEndLocation;
//
//			if (isShellVisible) {
//
//				// shell is already visible, move from the current position to the target position
//
//				_shellStartLocation = _shell.getLocation();
//
//			} else {
//
//				// shell is not visible, set position directly without moving animation, do only fading animation
//
//				_shellStartLocation = _shellEndLocation;
//
//				_shell.setLocation(_shellStartLocation.x, _shellStartLocation.y);
//
//				_shell.setAlpha(0);
//
//				setShellVisible(true);
//			}
//
//		} else if (_isShellFadingOut) {
//
//			// fading out has no movement
//
//			_fadeOutDelayCounter = 0;
//		}
//
//		// start animation now
//		_animationMoveCounter = 0;
//		animation20_Runnable();
//
////		System.out.println(UI.timeStampNano()
////				+ " animation10_StartKomplex\t"
////				+ ((float) (System.nanoTime() - start) / 1000000)
////				+ " ms");
////		// TODO remove SYSTEM.OUT.PRINTLN
//	}

   private void closeAll() {

//		System.out.println(UI.timeStampNano() + " closeAll\t");
//		// TODO remove SYSTEM.OUT.PRINTLN

      // deactivate auto close timer
      _display.timerExec(-1, _ttAutoCloseTimer);

      for (final ToolTip3Tool tool : _allTools.values()) {

         final Shell checkedShell = tool.getCheckedShell();
         if (checkedShell != null) {
            checkedShell.close();
         }
      }

      _allTools.clear();

      removeDisplayFilterListener();
   }

   /**
    * Close all shell except the parameter shell
    *
    * @param keepShellOpened
    */
   private void closeOthers(final Shell keepShellOpened) {

      /*
       * close all shells which are not flexible
       */
      final ArrayList<Object> removedEntries = new ArrayList<>();
      final Set<Entry<Object, ToolTip3Tool>> allToolEntries = _allTools.entrySet();

      for (final Entry<Object, ToolTip3Tool> toolEntry : allToolEntries) {

         final ToolTip3Tool tool = toolEntry.getValue();

         final Shell checkedShell = tool.getCheckedShell();
         if (checkedShell != null) {

            if (tool.isFlexTool()) {

               hideFlexTool(tool, checkedShell);

            } else {

               if (checkedShell != keepShellOpened) {
                  checkedShell.close();
                  removedEntries.add(tool);
               }
            }
         }
      }

      allToolEntries.removeAll(removedEntries);

      removeDisplayFilterListener();
   }

   private void closeTool(final ToolTip3Tool tool) {

      if (tool != null) {

         final Shell oldShell = tool.getCheckedShell();

         if (oldShell != null) {
            oldShell.close();
         }
      }
   }

   /**
    * Close shells when no area is hovered.
    */
   private void closeWhenNothingIsHovered() {

      if (_allTools.size() == 0) {
         return;
      }

//		System.out.println(UI.timeStampNano() + " closeWhenNothingIsHovered\t");
//		// TODO remove SYSTEM.OUT.PRINTLN

      // close all shells which are not moved, only moved shells stay opened

      final ArrayList<Object> removedKeys = new ArrayList<>();
      final Set<Entry<Object, ToolTip3Tool>> allToolEntries = _allTools.entrySet();

      for (final Entry<Object, ToolTip3Tool> toolEntry : allToolEntries) {

         final ToolTip3Tool tool = toolEntry.getValue();
         final Shell checkedShell = tool.getCheckedShell();

         if (tool.isFlexTool()) {

            // this is a flexible tool, don't hide when tool is visible and moved

            hideFlexTool(tool, checkedShell);

         } else {

            // close all default tools

            if (checkedShell != null) {
               checkedShell.close();
            }

            removedKeys.add(toolEntry.getKey());
         }
      }

      for (final Object toolKey : removedKeys) {
         _allTools.remove(toolKey);
      }

      removeDisplayFilterListener();
   }

   /**
    * Create a shell but do not display it
    *
    * @param toolTipArea
    * @return
    * @return Returns <code>true</code> when shell is created.
    */
   private ToolTip3Tool createToolUI(final Object toolTipArea) {

      // create shell
      final Shell ttShell = new Shell(_ownerControl.getShell(), //
            SWT.ON_TOP //
      /*
       * SWT.TOOL must be disabled that NO_FOCUS is working !!!
       */
//						| SWT.TOOL
//						| SWT.NO_FOCUS
      );

      ttShell.setLayout(new FillLayout());

      final ToolTip3Tool ttTool = new ToolTip3Tool(ttShell, toolTipArea);

      final IToolProvider toolProvider = getToolProvider(toolTipArea);

      toolProvider.setToolTipArea(toolTipArea);

      ttTool.setToolProvider(toolProvider);

      if (toolProvider.isFlexTool()) {

         // a flex tool is visible when it's created
         ttTool.setToolVisibility(true);

         // create a shell which can be moved (therefore it's flexible)

         final FlexTool flexTool = new FlexTool(this, ttTool);

         flexTool.createUI(ttShell);

         ttTool.setFlexable(flexTool);

      } else {

         toolProvider.createToolUI(ttShell);
      }

      ttTool.setInitialLocation(toolProvider.getInitialLocation());

      ttShell.pack(true);
      ttShell.setRedraw(true);

      addTTShellListener(ttShell);
      addTTAllControlsListener(ttShell);

      ttShell.setData(SHELL_DATA_TOOL, ttTool);

      return ttTool;
   }

   /**
    * Ensure that the tooltip is not displayed outside of the display.
    *
    * @param tipSize
    * @param location
    * @return
    */
   protected Point fixupDisplayBounds(final Point tipSize, final Point location) {

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

   protected Cursor getCursorDragged() {
      return _cursorDragged;
   }

   protected Cursor getCursorHand() {
      return _cursorHand;
   }

//	protected boolean isToolTipVisible() {
//
//		if (shell == null || shell.isDisposed()) {
//			return false;
//		}
//
//		final boolean isShellVisible = shell.isVisible();
//
////		System.out.println(UI.timeStampNano() + " isShellVisible=" + isShellVisible);
////		// TODO remove SYSTEM.OUT.PRINTLN
//
//		return isShellVisible;
//	}

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

   private Shell getHoveredShell() {

      if (_hoveredTool == null) {
         return null;
      }

      return _hoveredTool.getCheckedShell();
   }

   /**
    * @param toolTipArea
    * @return Returns a {@link ToolProvider}, cannot be <code>null</code>.
    */
   protected abstract IToolProvider getToolProvider(Object toolTipArea);

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
   protected Object getToolTipArea(final Point ownerHoverPosition) {
      return _ownerControl;
   }

   /**
    * Get tooltip location.
    *
    * @param size
    *           Tooltip size
    * @param toolProvider
    * @return Returns location relative to the device.
    */
   protected abstract Point getToolTipLocation(Point tipSize,
                                               final Point mouseOwnerPosition,
                                               IToolProvider toolProvider);

   /**
    * Hide the currently active tool tip
    */
   public void hide() {
      ttHide();
   }

   /**
    * A flex tool shell is only hidden when the tool is not moved and the tool is hidden.
    *
    * @param tool
    * @param checkedShell
    */
   private void hideFlexTool(final ToolTip3Tool tool, final Shell checkedShell) {

      if (checkedShell == null) {
         return;
      }

      final boolean isNotMoved = tool.isMoved() == false;
      final boolean isToolHidden = tool.isVisible() == false;

      final boolean isHide = isNotMoved || isToolHidden;

      if (isHide) {
         checkedShell.setVisible(false);
         keepParentOpen(false);
      }
   }

   /**
    * Keep or stop to keep slideout open when a sub tooltip/shell is opened
    *
    * @param isKeepOpen
    */
   private void keepParentOpen(final boolean isKeepOpen) {

      final SlideoutMap3Layer map3LayerSlideout = Map3Manager.getMap3LayerSlideout();

      if (map3LayerSlideout != null) {
         map3LayerSlideout.setIsAnotherDialogOpened(isKeepOpen);
      }
   }

   private void moveShellWithParent() {

//		if (_activeTTShell == null || _activeTTShell.isDisposed() || _activeTTShell.isVisible() == false) {
//			return;
//		}
//
//		ttShow();

      // reset default location, they are not valid any more
      for (final ToolTip3Tool ttTool : _allTools.values()) {
         ttTool.setDefaultLocation(null);
      }
   }

   void moveToDefaultLocation(final ToolTip3Tool tooltipTool) {

      final Shell toolShell = tooltipTool.getCheckedShell();
      if (toolShell == null) {
         return;
      }

      final Point defaultLocation = tooltipTool.getDefaultLocation();

      if (defaultLocation == null) {

         toolShell.setVisible(false);
         keepParentOpen(false);

      } else {

         // move tool shell to default location

         final Point shellSize = toolShell.getSize();
         final Point shellLocation = fixupDisplayBounds(shellSize, defaultLocation);

         toolShell.setLocation(shellLocation.x, shellLocation.y);
      }
   }

   /**
    * @return When the returned rectangle (which has display locations) is hit by the mouse, the
    *         tooltip should not be hidden. When <code>null</code> this check is ignored.
    */
   protected Rectangle noHideOnMouseMove() {
      return null;
   }

   /**
    * @return Returns <code>true</code> when tooltip should be kept opened.
    */
   private boolean onDisplayMouseMove() {

//		System.out.println(UI.timeStampNano() + " onDisplayMouseMove\t");
//		// TODO remove SYSTEM.OUT.PRINTLN

//		final long start = System.nanoTime();

      final Shell ttShell = getHoveredShell();
      if (ttShell == null || ttShell.isVisible() == false) {
         return false;
      }

      final Object shellData = ttShell.getData(SHELL_DATA_TOOL);
      if (shellData instanceof ToolTip3Tool) {
         final ToolTip3Tool ttTool = (ToolTip3Tool) shellData;
         if (ttTool.isFlexTool() && ttTool.isMoved()) {
            return true;
         }
      }

      boolean isHide = false;
      boolean isKeepVisible = false;

      // get control which is hovered with the mouse after the exit, can be null
      final Control hoveredControl = _display.getCursorControl();

//		System.out.println(UI.timeStampNano() + " onTTDisplayMouseMove - hoveredControl " + hoveredControl);
//		// TODO remove SYSTEM.OUT.PRINTLN

      if (hoveredControl == null) {

//			System.out.println(UI.timeStampNano() + " exit 0 hide");
//			// TODO remove SYSTEM.OUT.PRINTLN

         isHide = true;

      } else {

         /*
          * check if the hovered control is the owner control, if not, hide the tooltip
          */
         Control hoveredParent = hoveredControl;

         // move up child-parent hierarchy until shell is reached
         while (true) {

            if (hoveredParent == ttShell) {

               // mouse is hovering in this tooltip

               isKeepVisible = true;

//					System.out.println(UI.timeStampNano() + " exit 1 no hide");
//					// TODO remove SYSTEM.OUT.PRINTLN

               break;
            }

            if (hoveredParent == _ownerControl) {

               // mouse is hovering the owner control

               if (_mouseOverBehaviour == MOUSE_OVER_BEHAVIOUR_NO_IGNORE) {

                  /*
                   * owner is not ignored, which means the when the mouse is hovered the
                   * owner, the tooltip keeps opened, this is the default
                   */

                  isKeepVisible = true;
               }

//					System.out.println(UI.timeStampNano() + " exit 2 no hide");
//					// TODO remove SYSTEM.OUT.PRINTLN

               break;
            }

            hoveredParent = hoveredParent.getParent();

            if (hoveredParent == null) {

               // mouse has left the tooltip and the owner control

//					System.out.println(UI.timeStampNano() + " exit 3 hide");
//					// TODO remove SYSTEM.OUT.PRINTLN

               isHide = true;

               break;
            }
         }
      }

      /**
       * !!! this adjustment do not work on Linux because the tooltip gets hidden when the mouse
       * tries to mover over the tooltip <br>
       * <br>
       * it seems to work on windows and linux with margin 1, when set to 0 the tooltip do
       * sometime not be poped up again and the i-icons is not deaktivated<br>
       * wolfgang 2010-07-23
       */

      final Rectangle ttShellRect = ttShell.getBounds();
      final int margin = 10;

      ttShellRect.x -= margin;
      ttShellRect.y -= margin;
      ttShellRect.width += 2 * margin;
      ttShellRect.height += 2 * margin;

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

         final Rectangle noHideArea = noHideOnMouseMove();

         if (noHideArea == null || noHideArea.contains(displayCursorLocation) == false) {

            // hide definitively

            ttHide();

            isKeepOpened = false;
         }
      }

//		System.out.println(UI.timeStampNano()
//				+ " onDisplayMouseMove\t"
//				+ ((float) (System.nanoTime() - start) / 1000000)
//				+ " ms");
//		// TODO remove SYSTEM.OUT.PRINTLN

      return isKeepOpened;
   }

   private void onDispose() {

      // hide all tooltips

      removeOwnerShellListener();
      removeDisplayFilterListener();

      _cursorDragged = UI.disposeResource(_cursorDragged);
      _cursorHand = UI.disposeResource(_cursorHand);

      closeAll();
   }

   protected void onMouseMoveInToolTip(final MouseEvent mouseEvent) {}

   private void onOwnerEventControl(final Event event) {

      if (_ownerControl == null || _ownerControl.isDisposed()) {
         return;
      }

      switch (event.type) {
      case SWT.Dispose:

         onDispose();

         removeOwnerControlsListener();

         break;

      case SWT.MouseExit:

         // suspend hover time
         _display.timerExec(-1, _ownerHoverTimer);

         break;

      case SWT.MouseMove:

         // keep mouse position
         _ownerHoverPosition = new Point(event.x, event.y);

         // start hover time
         _display.timerExec(MOUSE_HOVER_DELAY, _ownerHoverTimer);

         break;

      case SWT.Resize:

         moveShellWithParent();

         break;
      }
   }

   private void onOwnerEventShell(final Event event) {

      final Shell activeShell = getHoveredShell();
      if (activeShell == null) {
         return;
      }

      switch (event.type) {
      case SWT.Deactivate:

         _display.asyncExec(new Runnable() {

            @Override
            public void run() {

               // hide tooltip when another shell is activated

               if (_display.getActiveShell() != activeShell) {
                  ttHide();
               }
            }
         });
         break;

      case SWT.Move:

         moveShellWithParent();

         break;
      }
   }

   private void onOwnerHovered(Object toolTipArea) {

//		if (_hoveredTool != null) {
//			System.out.println(UI.timeStampNano()
//					+ "\t1 isMoved="
//					+ _hoveredTool.isMoved()
//					+ "\tisVisible="
//					+ _hoveredTool.isVisible()
//					+ "\t"
//					+ _hoveredTool.hashCode());
//			// TODO remove SYSTEM.OUT.PRINTLN
//		}

      final ToolTip3Tool prevTool = _hoveredTool;
      _hoveredTool = null;

      // get tooltip area for the hovered position
      if (toolTipArea == null) {
         toolTipArea = getToolTipArea(_ownerHoverPosition);
      }

      if (toolTipArea == null) {

         // nothing is hovered in the tooltip owner, hide tooltips

         closeWhenNothingIsHovered();

         return;
      }

      // a tooltip area is hovered, show/create tooltip

      Shell existingShell = null;
      final ToolTip3Tool existingTool = _allTools.get(toolTipArea);

      boolean useExistingTool = false;

      // check existing tool
      if (existingTool != null) {

         existingShell = existingTool.getCheckedShell();

         if (existingShell != null) {
            useExistingTool = true;
         }
      }

      // close flex tool when it's hidden, this forces that the default tooltip is displayed
      if (useExistingTool && existingTool.isFlexTool() && existingTool.isVisible() == false) {
         closeTool(existingTool);
         useExistingTool = false;
      }

      if (useExistingTool) {

         // show existing tool

         _hoveredTool = existingTool;

         existingShell.moveAbove(null);

         // close other shells
         if (prevTool != existingTool) {
            closeOthers(existingShell);
         }

         // show existing tool
         if (existingShell.isVisible() == false) {
            ttShow();
         }

      } else {

         // create a new shell

         _hoveredTool = createToolUI(toolTipArea);

         // old tool is returned when a flex tool is replaced with a default tool
         final ToolTip3Tool oldTool = _allTools.put(toolTipArea, _hoveredTool);

         closeTool(oldTool);

         ttShow();
      }

//		if (_hoveredTool != null) {
//			System.out.println(UI.timeStampNano()
//					+ "\t2 isMoved="
//					+ _hoveredTool.isMoved()
//					+ "\tisVisible="
//					+ _hoveredTool.isVisible()
//					+ "\t"
//					+ _hoveredTool.hashCode()
//					+ "\r");
//			// TODO remove SYSTEM.OUT.PRINTLN
//		}
   }

   private void onTTAutoCloseTimer() {

//		System.out.println(UI.timeStampNano() + " onTTAutoCloseTimer\t");
//		// TODO remove SYSTEM.OUT.PRINTLN

      final boolean isKeepOpened = onDisplayMouseMove();

      if (isKeepOpened) {

         // start again to check again
         _display.timerExec(AUTO_CLOSE_INTERVAL, _ttAutoCloseTimer);
      }
   }

   private void onTTControlEvent(final Event event) {

      final Shell activeShell = getHoveredShell();
      if (activeShell == null) {
         return;
      }

      switch (event.type) {
      case SWT.KeyDown:

         if (event.keyCode == SWT.ESC) {
            hide();
         }

         break;

      case SWT.MouseDown:

//			System.out.println(UI.timeStampNano() + " mouseDown\t" + event.widget);
//			// TODO remove SYSTEM.OUT.PRINTLN

         break;

      case SWT.MouseEnter:

         if (_isShellFadingIn || _isShellFadingOut) {

            // stop animation
//				_isShellFadingIn = _isShellFadingOut = false;
         }

         break;

      case SWT.MouseExit:

//			System.out.println(UI.timeStampNano() + " mouse exit\t" + event.widget);
//			// TODO remove SYSTEM.OUT.PRINTLN

         break;

      case SWT.MouseMove:

//			System.out.println(UI.timeStampNano() + " mouse move\t" + event.widget);
//			// TODO remove SYSTEM.OUT.PRINTLN

//			if (_isReceiveOnMouseMove //
////					&& _isShellFadingIn == false
////					&& _isShellFadingOut == false//
//			) {
//
//				final Widget widget = event.widget;
//
//				if (widget instanceof Control) {
//
//					final Point ttDisplayLocation = ((Control) widget).toDisplay(event.x, event.y);
//					final Point ownerLocation = _ownerControl.toControl(ttDisplayLocation);
//
//					_display.asyncExec(new Runnable() {
//						public void run() {
//
//							final MouseEvent mouseEvent = new MouseEvent(event);
//							mouseEvent.x = ownerLocation.x;
//							mouseEvent.y = ownerLocation.y;
//
//							onMouseMoveInToolTip(mouseEvent);
//						}
//					});
//				}
//			}

         break;
      }
   }

   private void onTTShellEvent(final Event event) {

      switch (event.type) {
      case SWT.Deactivate:

         final Shell activeShell = getHoveredShell();

         if (activeShell != null //
               && _ownerControl != null
               && !_ownerControl.isDisposed()) {

            _display.asyncExec(new Runnable() {

               @Override
               public void run() {

                  // hide tooltip when another shell is activated

                  // check again
                  final Shell activeShell = getHoveredShell();
                  if (activeShell == null //
                        || _ownerControl == null
                        || _ownerControl.isDisposed()) {
                     return;
                  }

                  if (_ownerControl.getShell() == activeShell.getDisplay().getActiveShell()) {

                     // don't hide when main window is active
                     return;
                  }

                  ttHide();
               }
            });
         }

         break;

      case SWT.Dispose:

         break;

      }

   }

   private void removeDisplayFilterListener() {

      if (_isDisplayListenerSet) {

//			System.out.println(UI.timeStampNano() + " removeDisplayFilterListener\t");
//			// TODO remove SYSTEM.OUT.PRINTLN

         _display.removeFilter(SWT.MouseMove, _displayFilterListener);

         _isDisplayListenerSet = false;
      }
   }

   /**
    * Deactivate tooltip support for the underlying control
    */
   private void removeOwnerControlsListener() {

      _ownerControl.removeListener(SWT.Dispose, _ownerControlListener);
      _ownerControl.removeListener(SWT.MouseExit, _ownerControlListener);
      _ownerControl.removeListener(SWT.MouseMove, _ownerControlListener);
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

//	public void setFadeIsSteps(final int fadeInSteps) {
//		_fadeInSteps = fadeInSteps;
//	}

   private void setShellVisible(final Shell shell, final boolean isVisible) {

      if (isVisible) {

         // show tooltip

         shell.setVisible(true);

         addDisplayFilterListener();

      } else {

         // hide tooltip

         beforeHideToolTip();

         shell.setVisible(false);

         removeDisplayFilterListener();

         keepParentOpen(false);
      }
   }

   /**
    * Show/hide tooltip.
    *
    * @param toolProvider
    * @param isVisible
    * @param isUpdateUI
    */
   protected void toggleToolVisibility(final IToolProvider toolProvider,
                                       final boolean isVisible,
                                       final boolean isUpdateUI) {

      final Object ttArea = toolProvider.getToolTipArea();

      final ToolTip3Tool existingTool = _allTools.get(ttArea);

      if (existingTool != null) {

         if (toolProvider.isFlexTool()) {

            // update tool visibility state
            existingTool.setToolVisibility(isVisible);

         } else {

            // remove default tool that the flex tool is created

            _allTools.remove(ttArea);
         }
      }

      /*
       * force to create toggled tooltip
       */
      closeTool(existingTool);

      // UI is only updated when mouse is hovering the tool, otherwise tool is not set or displayed at the wrong location
      if (isUpdateUI) {
         onOwnerHovered(ttArea);
      }
   }

   /**
    * Hide active {@link #_hoveredTool}.
    */
   private void ttHide() {

//		System.out.println(UI.timeStampNano() + " ttHide\t");
//		// TODO remove SYSTEM.OUT.PRINTLN

      final Shell activeShell = getHoveredShell();
      if (activeShell == null || activeShell.isVisible() == false) {
         return;
      }

      keepParentOpen(false);

      if (_isShellFadingOut) {

         // shell is already fading out
         return;
      }

      // shell is not yet fading out

      _isShellFadingIn = false;
      _isShellFadingOut = true;

      animation10_Start();
   }

   /**
    * Show active {@link #_hoveredTool}.
    */
   private void ttShow() {

      // keep slideout open when a sub tooltip/shell is opened
      keepParentOpen(true);

//		System.out.println(UI.timeStampNano() + " ttShow\t");
//		// TODO remove SYSTEM.OUT.PRINTLN

      // ensure tooltip is closed when mouse is not hovering the tooltip or application
      _display.timerExec(AUTO_CLOSE_INTERVAL, _ttAutoCloseTimer);

      // shell is not yet fading in

      _isShellFadingIn = true;
      _isShellFadingOut = false;

      animation10_Start();
   }

}
