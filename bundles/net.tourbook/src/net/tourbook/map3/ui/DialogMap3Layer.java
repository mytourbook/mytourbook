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
package net.tourbook.map3.ui;

import static org.eclipse.swt.events.MouseTrackListener.mouseExitAdapter;

import net.tourbook.common.tooltip.AnimatedToolTipShell;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Map3 tour track layer properties dialog.
 */
public class DialogMap3Layer extends AnimatedToolTipShell {

   private static final int   SHELL_MARGIN    = 0;

   public static final Double DEFAULT_OPACITY = Double.valueOf(1.0);

   // initialize with default values which are (should) never be used
   private Rectangle       _toolTipItemBounds = new Rectangle(0, 0, 50, 50);

   private final WaitTimer _waitTimer         = new WaitTimer();

   private boolean         _canOpenToolTip;
   private boolean         _isWaitTimerStarted;

   /*
    * UI controls
    */
   private Composite _shellContainer;

   private final class WaitTimer implements Runnable {
      @Override
      public void run() {
         open_Runnable();
      }
   }

   public DialogMap3Layer(final Control ownerControl, final ToolBar toolBar) {

      super(ownerControl);

      addListener(toolBar);

      setToolTipCreateStyle(AnimatedToolTipShell.TOOLTIP_STYLE_KEEP_CONTENT);
      setBehaviourOnMouseOver(AnimatedToolTipShell.MOUSE_OVER_BEHAVIOUR_IGNORE_OWNER);
      setIsKeepShellOpenWhenMoved(false);
      setFadeInSteps(1);
      setFadeOutSteps(10);
      setFadeOutDelaySteps(1);
   }

   private void addListener(final ToolBar toolBar) {

      // prevent to open the tooltip
      toolBar.addMouseTrackListener(mouseExitAdapter(
            mouseEvent -> _canOpenToolTip = false));
   }

   @Override
   protected boolean canShowToolTip() {
      return true;
   }

   @Override
   protected boolean closeShellAfterHidden() {

      /*
       * Close the tooltip that the state is saved.
       */

      return true;
   }

   @Override
   protected Composite createToolTipContentArea(final Composite parent) {

//		Map3Manager.setMap3LayerDialog(this);

      return createUI(parent);
   }

   private Composite createUI(final Composite parent) {

      _shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults()//
            .margins(SHELL_MARGIN, SHELL_MARGIN)
            .spacing(0, 0)
            .applyTo(_shellContainer);
      {
         new Map3LayerUI(_shellContainer);
      }

      return _shellContainer;
   }

   public Shell getShell() {

      if (_shellContainer == null) {
         return null;
      }

      return _shellContainer.getShell();
   }

   @Override
   public Point getToolTipLocation(final Point tipSize) {

//		final int tipWidth = tipSize.x;
//
//		final int itemWidth = _toolTipItemBounds.width;
      final int itemHeight = _toolTipItemBounds.height;

      // center horizontally
      final int devX = _toolTipItemBounds.x;// + itemWidth / 2 - tipWidth / 2;
      final int devY = _toolTipItemBounds.y + itemHeight + 0;

      return new Point(devX, devY);
   }

   @Override
   protected Rectangle noHideOnMouseMove() {

      return _toolTipItemBounds;
   }

   /**
    * @param toolTipItemBounds
    * @param isOpenDelayed
    */
   public void open(final Rectangle toolTipItemBounds, final boolean isOpenDelayed) {

      if (isToolTipVisible()) {

         return;
      }

      if (isOpenDelayed == false) {

         if (toolTipItemBounds != null) {

            _toolTipItemBounds = toolTipItemBounds;

            showToolTip();
         }

      } else {

         if (toolTipItemBounds == null) {

            // item is not hovered any more

            _canOpenToolTip = false;

            return;
         }

         _toolTipItemBounds = toolTipItemBounds;
         _canOpenToolTip = true;

         if (_isWaitTimerStarted == false) {

            _isWaitTimerStarted = true;

            Display.getCurrent().timerExec(50, _waitTimer);
         }
      }
   }

   private void open_Runnable() {

      _isWaitTimerStarted = false;

      if (_canOpenToolTip) {
         showToolTip();
      }
   }

}
