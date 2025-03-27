/*******************************************************************************
 * Copyright (C) 2025 Wolfgang Schramm and Contributors
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
package org.eclipse.nebula.widgets.nattable.widget.mt;

import java.util.Map.Entry;

import org.eclipse.nebula.widgets.nattable.style.IStyle;
import org.eclipse.nebula.widgets.nattable.widget.NatCombo;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TableItem;

/**
 *
 */
public class NatComboMT extends NatCombo {

   public NatComboMT(final Composite parent,
                     final IStyle cellStyle,
                     final int maxVisibleItems,
                     final int style,
                     final boolean showDropdownFilter,
                     final boolean linkItemAndCheckbox) {

      super(parent, cellStyle, maxVisibleItems, style, showDropdownFilter, linkItemAndCheckbox);
   }

   /**
    * Calculates the size and location of the Shell that represents the
    * dropdown control of this NatCombo. Size and location will be calculated
    * dependent the position and size of the corresponding Text control and the
    * information showed in the dropdown.
    */
   @Override
   protected void calculateBounds() {

      if (this.dropdownShell != null && !this.dropdownShell.isDisposed()) {

         final Point size = getSize();
         // calculate the height by multiplying the number of visible items
         // with the item height of items in the list and adding 2*grid line
         // width to work around a calculation error regarding the descent of
         // the font metrics for the last shown item
         // Note: if there are no items to show in the combo, calculate with
         // the item count of 3 so an empty combo will open
         final int listHeight = (getVisibleItemCount() > 0 ? getVisibleItemCount() : 3)
               * this.dropdownTable.getItemHeight()

               + this.dropdownTable.getGridLineWidth() * 2

               /**
                * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                *
                * MT fix, otherwise the list is scrolled and the top item is not selectable with
                * the mouse
                *
                * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                */
               + 1

         ;

         // since introduced the TableColumn for real full row selection, we
         // call pack() to perform autoresize to ensure the width shows the
         // whole content
         this.dropdownTable.getColumn(0).pack();
         final int listWidth = Math.max(
               this.dropdownTable.computeSize(SWT.DEFAULT, listHeight, true).x,
               size.x);

         final Point textPosition = this.text.toDisplay(this.text.getLocation());

         // by default the dropdown shell will be created below the cell in
         // the table
         int dropdownShellStartingY = textPosition.y + this.text.getBounds().height;

         final int textBottomY = textPosition.y + this.text.getBounds().height + listHeight;
         // if the bottom of the drowdown is below the display, render it
         // above the cell
         if (textBottomY > Display.getCurrent().getBounds().height) {
            dropdownShellStartingY = textPosition.y - listHeight;
         }

         final Rectangle parentBounds = getParent().getBounds();
         final Point parentStart = getParent().toDisplay(parentBounds.x, parentBounds.y);
         int parentBottomY = parentStart.y + parentBounds.height - parentBounds.y;
         if (getParent().getHorizontalBar() != null && getParent().getHorizontalBar().isVisible()) {
            parentBottomY -= getParent().getHorizontalBar().getSize().y;
         }
         if (dropdownShellStartingY > parentBottomY) {
            dropdownShellStartingY = parentBottomY;
         }

         final int filterTextBoxHeight = this.showDropdownFilter
               ? this.filterBox.computeSize(SWT.DEFAULT, SWT.DEFAULT).y
               : 0;

         final Rectangle shellBounds = new Rectangle(
               textPosition.x,
               dropdownShellStartingY,
               listWidth + (this.dropdownTable.getGridLineWidth() * 2),
               listHeight + filterTextBoxHeight);

         this.dropdownShell.setBounds(shellBounds);

         calculateColumnWidth();
      }
   }

   /**
    * Select or deselect all items
    *
    * @param isSelected
    */
   public void selectAll(final Boolean isSelected) {

      // update model
      for (final Entry<String, Boolean> entrySet : selectionStateMap.entrySet()) {
         entrySet.setValue(isSelected);
      }

      // update UI
      if (dropdownTable != null) {

         final TableItem[] allItems = dropdownTable.getItems();

         for (final TableItem item : allItems) {
            item.setChecked(isSelected);
         }
      }

      updateTextControl(false);
   }

   @Override
   public void setEnabled(final boolean isEnabled) {

//      super.setEnabled(isEnabled);

      if (text != null) {

         text.setEnabled(isEnabled);
      }
   }
}
