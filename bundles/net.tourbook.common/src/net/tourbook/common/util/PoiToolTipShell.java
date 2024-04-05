/*******************************************************************************
 * Copyright (C) 2010, 2024 Wolfgang Schramm and Contributors
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
package net.tourbook.common.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public abstract class PoiToolTipShell {

   /**
    * maximum width in pixel for the width of the tooltip
    */
   private static final int MAX_TOOLTIP_WIDTH = 500;

   private final Shell      _shell;

   private final Composite  _toolTipContent;

   public PoiToolTipShell(final Composite parent) {

      final Display display = parent.getDisplay();

      _shell = new Shell(display, SWT.ON_TOP | SWT.TOOL);

      _shell.addMouseTrackListener(MouseTrackListener.mouseExitAdapter(mouseEvent -> _shell.setVisible(false)));

      _shell.addKeyListener(KeyListener.keyPressedAdapter(keyEvent -> {

         if (keyEvent.keyCode == SWT.ESC) {
            _shell.setVisible(false);
         }
      }));

      _shell.setLayout(new FillLayout());

      _toolTipContent = setContent(_shell);
   }

   public void dispose() {
      _shell.dispose();
   }

   public void hide() {

      _shell.setVisible(false);
   }

   public boolean isVisible() {
      return _shell.isVisible();
   }

   protected abstract Composite setContent(Shell shell);

   /**
    * Position the tooltip and ensure that it is not located off the screen.
    *
    * @param shellArea
    *           client area for the shell
    * @param noCoverX
    *           left position which should not be covered
    * @param noCoverY
    *           top position which should not be coverd
    * @param noCoverWidth
    *           width relative to left which should not be covered
    * @param noCoverHeight
    *           height relative to top which should not be covered
    * @param noCoverYOffset
    */
   private void setToolTipPosition(final Rectangle shellArea,
                                   final int noCoverX,
                                   final int noCoverY,
                                   final int noCoverWidth,
                                   final int noCoverHeight,
                                   final int noCoverYOffset) {

      final int devX = noCoverX - (shellArea.width / 2) + (noCoverWidth / 2);
      final int devY = noCoverY - shellArea.height - noCoverYOffset;

      _shell.setLocation(devX, devY);
   }

   /**
    * Shows the tooltip
    *
    * @param x
    *           left position which should not be covered
    * @param y
    *           top position which should not be coverd
    * @param width
    *           width relative to left which should not be covered
    * @param height
    *           height relative to top which should not be covered
    */
   public void show(final int noCoverX,
                    final int noCoverY,
                    final int noCoverWidth,
                    final int noCoverHeight,
                    final int noCoverYOffset) {

      /*
       * adjust width of the tooltip when it exeeds the maximum
       */
      Point containerSize = _toolTipContent.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
      if (containerSize.x > MAX_TOOLTIP_WIDTH) {

         containerSize = _toolTipContent.computeSize(MAX_TOOLTIP_WIDTH, SWT.DEFAULT, true);
      }

      _toolTipContent.setSize(containerSize);
      _shell.pack(true);

      /*
       * On some platforms, there is a minimum size for a shell which may be greater than the
       * label size. To avoid having the background of the tip shell showing around the label,
       * force the label to fill the entire client area.
       */
      final Rectangle shellArea = _shell.getClientArea();
      _toolTipContent.setSize(shellArea.width, shellArea.height);

      setToolTipPosition(shellArea, noCoverX, noCoverY, noCoverWidth, noCoverHeight, noCoverYOffset);

      _shell.setVisible(true);
   }

}
