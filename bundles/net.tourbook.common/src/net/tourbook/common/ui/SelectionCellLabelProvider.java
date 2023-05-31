/*******************************************************************************
 * Copyright (C) 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.common.ui;

import net.tourbook.common.UI;

import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;

/**
 * Paint selection background with a bit more contrast compared to the original color
 * <p>
 * <a href=
 * "https://stackoverflow.com/questions/64502873/how-to-change-swt-table-item-selection-background-color#answer-64503345"
 * >Original Code</v>
 */
public class SelectionCellLabelProvider extends StyledCellLabelProvider {

   private Color _selectionColor = new Color(111, 111, 111);

   private Color _hoveredColor             = new Color(177, 77, 77);
   private Color _hoveredAndSelectionColor = new Color(54, 85, 108);

   @Override
   protected void erase(final Event event, final Object element) {

      if (UI.IS_DARK_THEME) {

         if ((event.detail & SWT.SELECTED) != 0 && (event.detail & SWT.HOT) != 0) {

            // item is selected + hovered

// !!! THIS IS NOT WORKING FOR THE 2ND... COLUMNS IN THE TREE

//            final Rectangle bounds = event.getBounds();
//
//            event.gc.setBackground(_hoveredAndSelectionColor);
//            event.gc.setBackground(_selectionColor);
//            event.gc.fillRectangle(bounds);
//
//            event.detail &= ~SWT.SELECTED;
//            event.detail &= ~SWT.HOT;

         } else if ((event.detail & SWT.HOT) != 0) {

            // item is hovered

// !!! THIS IS NOT WORKING FOR THE 2ND... COLUMNS IN THE TREE

//            final Rectangle bounds = event.getBounds();
//
//            event.gc.setBackground(_hoveredColor);
//            event.gc.fillRectangle(bounds);
//
//            event.detail &= ~SWT.HOT;

         } else if ((event.detail & SWT.SELECTED) != 0) {

            // item is selected

            final Rectangle bounds = event.getBounds();

            event.gc.setBackground(_selectionColor);
            event.gc.fillRectangle(bounds);

            event.detail &= ~SWT.SELECTED;
         }
      }

      super.erase(event, element);
   }

//   @Override
//   protected void measure(final Event event, final Object element) {
//
//      if (UI.IS_DARK_THEME) {
//
//         if ((event.detail & SWT.SELECTED) != 0 && (event.detail & SWT.HOT) != 0) {
//
//            // item is selected + hovered
//
//            event.detail &= ~SWT.SELECTED;
//            event.detail &= ~SWT.HOT;
//
//         } else if ((event.detail & SWT.HOT) != 0) {
//
//            // item is hovered
//
//            event.detail &= ~SWT.HOT;
//
//         } else if ((event.detail & SWT.SELECTED) != 0) {
//
//            // item is selected
//
//            event.detail &= ~SWT.SELECTED;
//         }
//      }
//
//      super.measure(event, element);
//   }
//
//   @Override
//   protected void paint(final Event event, final Object element) {
//
//      if (UI.IS_DARK_THEME) {
//
//         if ((event.detail & SWT.SELECTED) != 0 && (event.detail & SWT.HOT) != 0) {
//
//            // item is selected + hovered
//
//            event.detail &= ~SWT.SELECTED;
//            event.detail &= ~SWT.HOT;
//
//         } else if ((event.detail & SWT.HOT) != 0) {
//
//            // item is hovered
//
//            event.detail &= ~SWT.HOT;
//
//         } else if ((event.detail & SWT.SELECTED) != 0) {
//
//            // item is selected
//
//            event.detail &= ~SWT.SELECTED;
//
//         }
//      }
//
//      super.paint(event, element);
//   }

}
