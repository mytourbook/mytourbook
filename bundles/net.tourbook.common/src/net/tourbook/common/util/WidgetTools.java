/*******************************************************************************
 * Copyright (C) 2020 Wolfgang Schramm and Contributors
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
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

/**
 * SWT widget utilities.
 */
public final class WidgetTools {

   /**
    * @return Returns a scrollbar listener for the {@link Text} widget which automatically hides the
    *         scrollbars when not necessary, this do not work for {@link StyledText}!
    *         <p>
    *         Idea:
    *         <a href=
    *         "https://stackoverflow.com/questions/8547428/how-to-implement-auto-hide-scrollbar-in-swt-text-component">how-to-implement-auto-hide-scrollbar-in-swt-text-component</a>
    */
   public static Listener getTextScrollbarListener() {

      final Listener scrollBarListener = new Listener() {
         @Override
         public void handleEvent(final Event event) {

            final Text txtWidget = (Text) event.widget;
            final Rectangle rectClient = txtWidget.getClientArea();
            final Rectangle rectTrim = txtWidget.computeTrim(rectClient.x, rectClient.y, rectClient.width, rectClient.height);
            final Point defaultSize = txtWidget.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);

            txtWidget.getHorizontalBar().setVisible(rectTrim.width <= defaultSize.x);
            txtWidget.getVerticalBar().setVisible(rectTrim.height <= defaultSize.y);

            if (event.type == SWT.Modify) {
               txtWidget.getParent().layout(true);
               txtWidget.showSelection();
            }
         }
      };

      return scrollBarListener;
   }

}
