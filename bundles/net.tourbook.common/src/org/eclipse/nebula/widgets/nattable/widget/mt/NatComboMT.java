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

import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.nebula.widgets.nattable.style.CellStyleAttributes;
import org.eclipse.nebula.widgets.nattable.style.HorizontalAlignmentEnum;
import org.eclipse.nebula.widgets.nattable.style.IStyle;
import org.eclipse.nebula.widgets.nattable.ui.matcher.LetterOrDigitKeyEventMatcher;
import org.eclipse.nebula.widgets.nattable.widget.NatCombo;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

/**
 *
 */
public class NatComboMT extends NatCombo {

   /**
    * List of SelectionListener that should be added to the dropdown table once
    * it is created. Kept locally because the table creation is deferred to the
    * first access.
    */
   private final ListenerList<SelectionListener> selectionAllListener = new ListenerList<>(ListenerList.IDENTITY);

   public NatComboMT(final Composite parent,
                     final IStyle cellStyle,
                     final int maxVisibleItems,
                     final int style,
                     final boolean showDropdownFilter,
                     final boolean linkItemAndCheckbox) {

      super(parent, cellStyle, maxVisibleItems, style, showDropdownFilter, linkItemAndCheckbox);
   }

   public void addSelectionAllListener(final SelectionListener listener) {

      if (listener != null) {
         selectionAllListener.add(listener);
      }
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

   public void check(final int[] allCheckedIndices) {

      final Table table = getDropdownTable();

      // deselect all
      table.deselectAll();

      final TableItem[] allItems = table.getItems();

      for (final int itemIndex : allCheckedIndices) {
         allItems[itemIndex].setChecked(true);
      }
   }

   /**
    * Creates the Text control of this NatCombo, adding styles, look&amp;feel
    * and needed listeners for the control only.
    *
    * @param style
    *           The style for the Text Control to construct. Uses this style
    *           adding internal styles via ConfigRegistry.
    */
   @Override
   protected void createTextControl(final int style) {

      final int textStyle = style | HorizontalAlignmentEnum.getSWTStyle(this.cellStyle);
      this.text = new Text(this, textStyle);
      this.text.setBackground(this.cellStyle.getAttributeValue(CellStyleAttributes.BACKGROUND_COLOR));
      this.text.setForeground(this.cellStyle.getAttributeValue(CellStyleAttributes.FOREGROUND_COLOR));
      this.text.setFont(this.cellStyle.getAttributeValue(CellStyleAttributes.FONT));

      final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
      this.text.setLayoutData(gridData);

      this.text.addKeyListener(new KeyAdapter() {
         @Override
         public void keyPressed(final KeyEvent event) {
            if (event.keyCode == SWT.ARROW_DOWN
                  || event.keyCode == SWT.ARROW_UP) {
               showDropdownControl();

               final int selectionIndex = getDropdownTable().getSelectionIndex();
               if (selectionIndex < 0) {
                  select(0);
               } else {
                  // only visualize the selection in the dropdown, do not
                  // perform a selection
                  getDropdownTable().select(selectionIndex);
               }

               // ensure the arrow key events do not have any further
               // effect
               event.doit = false;
            } else if (!LetterOrDigitKeyEventMatcher.isLetterOrDigit(event.character)) {
               if (freeEdit) {
                  // simply clear the selection in dropdownlist so the
                  // free value in text control will be used
                  if (!getDropdownTable().isDisposed()) {
                     getDropdownTable().deselectAll();
                     for (final Map.Entry<String, Boolean> entry : selectionStateMap.entrySet()) {
                        entry.setValue(Boolean.FALSE);
                     }
                  }
               } else {
                  showDropdownControl();
               }
            }
         }
      });

      this.text.addMouseListener(new MouseAdapter() {

         @Override
         public void mouseDown(final MouseEvent e) {
            if (!freeEdit) {
               if (getDropdownTable().isDisposed()
                     || !getDropdownTable().isVisible()) {
                  showDropdownControl();
               } else {
                  // if there is no free edit enabled, set the focus back
                  // to the dropdownlist so it handles key strokes itself
                  getDropdownTable().forceFocus();
               }
            }
         }
      });

      this.text.addControlListener(new ControlListener() {
         @Override
         public void controlMoved(final ControlEvent e) {
            calculateBounds();
         }

         @Override
         public void controlResized(final ControlEvent e) {
            calculateBounds();
         }
      });

      this.text.addFocusListener(new FocusListenerWrapper());

      /**
       * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
       *
       * The iconCanvas is hidden, because it cannot be disabled
       *
       * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
       */

//      final Canvas iconCanvas = new Canvas(this, SWT.NONE) {
//
//         @Override
//         public Point computeSize(final int wHint, final int hHint, final boolean changed) {
//            final Rectangle iconImageBounds = iconImage.getBounds();
//            return new Point(iconImageBounds.width + 2, iconImageBounds.height + 2);
//         }
//
//      };
//
//      gridData = new GridData(GridData.BEGINNING, SWT.FILL, false, true);
//      iconCanvas.setLayoutData(gridData);
//
//      iconCanvas.addPaintListener(event -> {
//         final GC gc = event.gc;
//
//         final Rectangle iconCanvasBounds = iconCanvas.getBounds();
//         final Rectangle iconImageBounds = iconImage.getBounds();
//         final int horizontalAlignmentPadding =
//               CellStyleUtil.getHorizontalAlignmentPadding(
//                     HorizontalAlignmentEnum.CENTER,
//                     iconCanvasBounds,
//                     iconImageBounds.width);
//         final int verticalAlignmentPadding =
//               CellStyleUtil.getVerticalAlignmentPadding(
//                     VerticalAlignmentEnum.MIDDLE,
//                     iconCanvasBounds,
//                     iconImageBounds.height);
//         gc.drawImage(iconImage, horizontalAlignmentPadding, verticalAlignmentPadding);
//
//         final Color originalFg = gc.getForeground();
//         gc.setForeground(GUIHelper.COLOR_WIDGET_BORDER);
//         gc.drawRectangle(0, 0, iconCanvasBounds.width - 1, iconCanvasBounds.height - 1);
//         gc.setForeground(originalFg);
//      });
//
//      iconCanvas.addMouseListener(new MouseAdapter() {
//
//         @Override
//         public void mouseDown(final MouseEvent e) {
//            if (dropdownShell != null && !dropdownShell.isDisposed()) {
//               if (dropdownShell.isVisible()) {
//                  text.forceFocus();
//                  hideDropdownControl();
//               } else {
//                  showDropdownControl();
//               }
//            } else {
//               showDropdownControl();
//            }
//         }
//      });
   }

   private void fireSelectionAllListener() {

      final Event event = new Event();
      event.widget = text;

      final SelectionEvent selectionEvent = new SelectionEvent(event);

      for (final SelectionListener listeners : selectionAllListener) {
         listeners.widgetSelected(selectionEvent);
      }
   }

   public void removeSelectionAllListener(final SelectionListener listener) {

      selectionAllListener.remove(listener);
   }

   /**
    * Select or deselect all items
    *
    * @param isSelected
    */
   public void selectAll(final Boolean isSelected) {

      if (isSelected == null) {

         // invert selection

         // update model
         for (final Entry<String, Boolean> entrySet : selectionStateMap.entrySet()) {

            final Boolean inverseValue = !entrySet.getValue();

            entrySet.setValue(inverseValue);
         }

         // update UI
         final TableItem[] allItems = getDropdownTable().getItems();
         for (final TableItem item : allItems) {

            final boolean inverseChecked = !item.getChecked();

            item.setChecked(inverseChecked);
         }

      } else {

         // update model
         for (final Entry<String, Boolean> entrySet : selectionStateMap.entrySet()) {
            entrySet.setValue(isSelected);
         }

         // update UI
         final TableItem[] allItems = getDropdownTable().getItems();
         for (final TableItem item : allItems) {
            item.setChecked(isSelected);
         }
      }

      updateTextControl(false);

      // fire selection
      fireSelectionAllListener();
   }

   @Override
   public void setEnabled(final boolean isEnabled) {

      if (text != null) {

         text.setEnabled(isEnabled);
      }
   }

   @Override
   public void setItems(final String[] items) {

      // ensure that the table is create that items can be populated !!!
      getDropdownTable();

      super.setItems(items);
   }
}
