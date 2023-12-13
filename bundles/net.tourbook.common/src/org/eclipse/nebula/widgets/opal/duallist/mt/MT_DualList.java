/*******************************************************************************
 * Copyright (c) 2011-2021 Laurent CARON
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: Laurent CARON (laurent.caron at gmail dot com) - initial API
 * and implementation
 *******************************************************************************/
package org.eclipse.nebula.widgets.opal.duallist.mt;

import java.util.ArrayList;
import java.util.List;

import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;

import org.eclipse.nebula.widgets.opal.commons.SWTGraphicUtil;
import org.eclipse.nebula.widgets.opal.commons.SelectionListenerUtil;
import org.eclipse.nebula.widgets.opal.duallist.mt.MT_DLItem.LAST_ACTION;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

/**
 * Instances of this class are controls that allow the user to select multiple
 * elements.
 * <dl>
 * <dt><b>Styles:</b></dt>
 * <dd>(none)</dd>
 * <dt><b>Events:</b></dt>
 * <dd>Selection</dd>
 * </dl>
 */

public class MT_DualList extends Composite {

// SET_FORMATTING_OFF

   private Image _imageMoveLeft     = CommonActivator.getThemedImageDescriptor(CommonImages.App_Move_Left)     .createImage();
   private Image _imageMoveLeft_2x  = CommonActivator.getThemedImageDescriptor(CommonImages.App_Move_Left_2x)  .createImage();
   private Image _imageMoveRight    = CommonActivator.getThemedImageDescriptor(CommonImages.App_Move_Right)    .createImage();
   private Image _imageMoveRight_2x = CommonActivator.getThemedImageDescriptor(CommonImages.App_Move_Right_2x) .createImage();
   private Image _imageMoveUp       = CommonActivator.getThemedImageDescriptor(CommonImages.App_Move_Up)       .createImage();
   private Image _imageMoveUp_2x    = CommonActivator.getThemedImageDescriptor(CommonImages.App_Move_Up_2x)    .createImage();
   private Image _imageMoveDown     = CommonActivator.getThemedImageDescriptor(CommonImages.App_Move_Down)     .createImage();
   private Image _imageMoveDown_2x  = CommonActivator.getThemedImageDescriptor(CommonImages.App_Move_Down_2x)  .createImage();

// SET_FORMATTING_ON

   private final List<MT_DLItem>            _allItems;
   private final List<MT_DLItem>            _allSelectedItems;

   private List<MT_SelectionChangeListener> selectionChangeListeners;
   private MT_DLConfiguration               configuration;

   /*
    * UI controls
    */
   private Button _btnMoveFirst;
   private Button _btnMoveLast;
   private Button _btnMoveUp;
   private Button _btnMoveDown;
   private Button _btnSelect;
   private Button _btnSelectAll;
   private Button _btnDeselect;
   private Button _btnDeselectAll;

   private Table  _tableAllItems;
   private Table  _tableAllSelectedItems;

   /**
    * Constructs a new instance of this class given its parent and a style value
    * describing its behavior and appearance.
    * <p>
    * The style value is either one of the style constants defined in class
    * <code>SWT</code> which is applicable to instances of this class, or must be
    * built by <em>bitwise OR</em>'ing together (that is, using the
    * <code>int</code> "|" operator) two or more of those <code>SWT</code> style
    * constants. The class description lists the style constants that are
    * applicable to the class. Style bits are also inherited from superclasses.
    * </p>
    *
    * @param parent
    *           a composite control which will be the parent of the new
    *           instance (cannot be null)
    * @param style
    *           the style of control to construct
    *
    * @exception IllegalArgumentException
    *               <ul>
    *               <li>ERROR_NULL_ARGUMENT - if the parent is null</li>
    *               </ul>
    * @exception SWTException
    *               <ul>
    *               <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
    *               thread that created the parent</li>
    *               </ul>
    *
    */
   public MT_DualList(final Composite parent, final int style) {

      super(parent, style);

      _allItems = new ArrayList<>();
      _allSelectedItems = new ArrayList<>();

      final GridLayout layout = new GridLayout(4, false);
      layout.marginWidth = 0;
      layout.marginHeight = 0;
      setLayout(layout);

      createTable_AllItems();
      createButtonSelectAll();
      createTable_SelectedItems();

      createButtonMoveFirst();
      createButtonSelect();
      createButtonMoveUp();
      createButtonDeselect();
      createButtonMoveDown();
      createButtonDeselectAll();
      createButtonMoveLast();

      this.setTabList(new Control[] {

            _tableAllItems,

            _btnSelectAll,
            _btnSelect,
            _btnDeselect,
            _btnDeselectAll,

            _tableAllSelectedItems,

            _btnMoveFirst,
            _btnMoveUp,
            _btnMoveDown,
            _btnMoveLast
      });
   }

   /**
    * Adds the argument to the end of the receiver's list.
    *
    * @param item
    *           the new item
    *
    * @exception IllegalArgumentException
    *               <ul>
    *               <li>ERROR_NULL_ARGUMENT - if the item is null</li>
    *               </ul>
    * @exception SWTException
    *               <ul>
    *               <li>ERROR_WIDGET_DISPOSED - if the receiver has been
    *               disposed</li>
    *               <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
    *               thread that created the receiver</li>
    *               </ul>
    *
    * @see #add(MT_DLItem,int)
    */
   public void add(final MT_DLItem item) {
      checkWidget();
      if (item == null) {
         SWT.error(SWT.ERROR_NULL_ARGUMENT);
      }
      _allItems.add(item);
      redrawTables();
   }

   /**
    * Adds the argument to the receiver's list at the given zero-relative index.
    * <p>
    * Note: To add an item at the end of the list, use the result of calling
    * <code>getItemCount()</code> as the index or use <code>add(MT_DLItem)</code>.
    * </p>
    *
    * @param item
    *           the new item
    * @param index
    *           the index for the item
    *
    * @exception IllegalArgumentException
    *               <ul>
    *               <li>ERROR_NULL_ARGUMENT - if the item is null</li>
    *               <li>ERROR_INVALID_RANGE - if the index is not between 0 and
    *               the number of elements in the list (inclusive)</li>
    *               </ul>
    * @exception SWTException
    *               <ul>
    *               <li>ERROR_WIDGET_DISPOSED - if the receiver has been
    *               disposed</li>
    *               <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
    *               thread that created the receiver</li>
    *               </ul>
    *
    * @see #add(String)
    */
   public void add(final MT_DLItem item, final int index) {
      checkWidget();
      if (item == null) {
         SWT.error(SWT.ERROR_NULL_ARGUMENT);
      }
      if (index < 0 || index >= _allItems.size()) {
         SWT.error(SWT.ERROR_INVALID_ARGUMENT);
      }
      _allItems.add(index, item);
      redrawTables();
   }

   /**
    * Adds the listener to the collection of listeners who will be notified when
    * the user changes the receiver's selection, by sending it one of the messages
    * defined in the <code>SelectionChangeListener</code> interface.
    *
    * @param listener
    *           the listener which should be notified
    *
    * @exception IllegalArgumentException
    *               <ul>
    *               <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
    *               </ul>
    * @exception SWTException
    *               <ul>
    *               <li>ERROR_WIDGET_DISPOSED - if the receiver has been
    *               disposed</li>
    *               <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
    *               thread that created the receiver</li>
    *               </ul>
    *
    * @see SelectionChangeListener
    * @see #removeSelectionChangeListener
    * @see SelectionChangeEvent
    */
   public void addSelectionChangeListener(final MT_SelectionChangeListener listener) {
      checkWidget();
      if (listener == null) {
         SWT.error(SWT.ERROR_NULL_ARGUMENT);
      }
      if (selectionChangeListeners == null) {
         selectionChangeListeners = new ArrayList<>();
      }
      selectionChangeListeners.add(listener);
   }

   /**
    * Adds the listener to the collection of listeners who will be notified when
    * the user changes the receiver's selection, by sending it one of the messages
    * defined in the <code>SelectionListener</code> interface.
    *
    * @param listener
    *           the listener which should be notified
    *
    * @exception IllegalArgumentException
    *               <ul>
    *               <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
    *               </ul>
    * @exception SWTException
    *               <ul>
    *               <li>ERROR_WIDGET_DISPOSED - if the receiver has been
    *               disposed</li>
    *               <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
    *               thread that created the receiver</li>
    *               </ul>
    *
    * @see SelectionListener
    * @see #removeSelectionListener
    * @see SelectionEvent
    */
   public void addSelectionListener(final SelectionListener listener) {
      checkWidget();
      if (listener == null) {
         SWT.error(SWT.ERROR_NULL_ARGUMENT);
      }
      SelectionListenerUtil.addSelectionListener(this, listener);
   }

   private void applyNewConfiguration() {

      try {
         setRedraw(true);
         if (configuration == null) {
            resetConfigurationToDefault();
         } else {
            modifyPanelsColors();
            modifyTextAlignment();
            modifyButtonImages();
            modifyButtonVisibility();
         }
         redrawTables();
      } finally {
         setRedraw(true);
      }
   }

   /**
    * Cleans the content of a table
    *
    * @param table
    *           table to be emptied
    */
   private void clean(final Table table) {

      if (table == null) {
         return;
      }

      for (final TableItem item : table.getItems()) {
         item.dispose();
      }
   }

   /**
    * Create a button
    *
    * @param image
    *           file name of the icon
    * @param verticalExpand
    *           if <code>true</code>, the button will take all the
    *           available space vertically
    * @param alignment
    *           button alignment
    *
    * @return a new button
    */
   private Button createButton(final Image image, final boolean verticalExpand, final int alignment) {

      final Button button = new Button(this, SWT.PUSH);

      button.setImage(image);
      button.setLayoutData(new GridData(GridData.CENTER, alignment, false, verticalExpand));

      SWTGraphicUtil.addDisposer(button, image);

      return button;
   }

   private void createButtonDeselect() {
      _btnDeselect = createButton(_imageMoveLeft, false, GridData.CENTER);
      _btnDeselect.addListener(SWT.Selection, e -> {
         deselectItem();
      });
   }

   private void createButtonDeselectAll() {
      _btnDeselectAll = createButton(_imageMoveLeft_2x, false, GridData.BEGINNING);
      _btnDeselectAll.addListener(SWT.Selection, e -> {
         deselectAll();
      });
   }

   private void createButtonMoveDown() {
      _btnMoveDown = createButton(_imageMoveDown, false, GridData.CENTER);
      _btnMoveDown.addListener(SWT.Selection, e -> {
         moveSelectionDown();
      });
   }

   private void createButtonMoveFirst() {
      _btnMoveFirst = createButton(_imageMoveUp_2x, true, GridData.END);
      _btnMoveFirst.addListener(SWT.Selection, e -> {
         moveSelectionToFirstPosition();
      });
   }

   private void createButtonMoveLast() {
      _btnMoveLast = createButton(_imageMoveDown_2x, true, GridData.BEGINNING);
      _btnMoveLast.addListener(SWT.Selection, e -> {
         moveSelectionToLastPosition();
      });
   }

   private void createButtonMoveUp() {
      _btnMoveUp = createButton(_imageMoveUp, false, GridData.CENTER);
      _btnMoveUp.addListener(SWT.Selection, e -> {
         moveSelectionUp();
      });
   }

   private void createButtonSelect() {
      _btnSelect = createButton(_imageMoveRight, false, GridData.CENTER);
      _btnSelect.addListener(SWT.Selection, e -> {
         selectItem();
      });
   }

   private void createButtonSelectAll() {
      _btnSelectAll = createButton(_imageMoveRight_2x, true, GridData.END);
      _btnSelectAll.addListener(SWT.Selection, e -> {
         selectAll();
      });
   }

   /**
    * @return a table that will contain data
    */
   private Table createTable() {

      final Table table = new Table(this, SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);
      table.setLinesVisible(false);
      table.setHeaderVisible(true);
      table.setData(-1);

      final GridData gd = new GridData(GridData.FILL, GridData.FILL, true, true, 1, 4);
      gd.widthHint = 200;
      table.setLayoutData(gd);

      new TableColumn(table, SWT.CENTER); // image
      new TableColumn(table, SWT.LEFT); // text
      new TableColumn(table, SWT.LEFT); // text2

      return table;
   }

   private void createTable_AllItems() {
      _tableAllItems = createTable();
      _tableAllItems.addListener(SWT.MouseDoubleClick, event -> {
         selectItem();
      });
   }

   private void createTable_SelectedItems() {
      _tableAllSelectedItems = createTable();
      _tableAllSelectedItems.addListener(SWT.MouseDoubleClick, event -> {
         deselectItem();
      });
   }

   /**
    * Deselects the item at the given zero-relative index in the receiver. If the
    * item at the index was already deselected, it remains deselected. Indices that
    * are out of range are ignored.
    *
    * @param index
    *           the index of the item to deselect
    *
    * @exception SWTException
    *               <ul>
    *               <li>ERROR_WIDGET_DISPOSED - if the receiver has been
    *               disposed</li>
    *               <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
    *               thread that created the receiver</li>
    *               </ul>
    */
   public void deselect(final int index) {
      deselect(index, true);
   }

   /**
    * Deselects the item at the given zero-relative index in the receiver. If the
    * item at the index was already deselected, it remains deselected. Indices that
    * are out of range are ignored.
    *
    * @param index
    *           the index of the item to deselect
    *
    * @exception SWTException
    *               <ul>
    *               <li>ERROR_WIDGET_DISPOSED - if the receiver has been
    *               disposed</li>
    *               <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
    *               thread that created the receiver</li>
    *               </ul>
    */
   private void deselect(final int index, final boolean shouldFireEvents) {
      checkWidget();
      if (index < 0 || index >= _allItems.size()) {
         return;
      }
      final MT_DLItem item = _allSelectedItems.remove(index);
      if (shouldFireEvents) {
         fireSelectionEvent(item);
      }

      final List<MT_DLItem> deselectedItems = new ArrayList<>();
      item.setLastAction(LAST_ACTION.DESELECTION);
      deselectedItems.add(item);
      if (shouldFireEvents) {
         fireSelectionChangeEvent(deselectedItems);
      }
      redrawTables();
   }

   /**
    * Deselects the items at the given zero-relative indices in the receiver. If
    * the item at the given zero-relative index in the receiver is selected, it is
    * deselected. If the item at the index was not selected, it remains deselected.
    * The range of the indices is inclusive. Indices that are out of range are
    * ignored.
    *
    * @param start
    *           the start index of the items to deselect
    * @param end
    *           the end index of the items to deselect
    *
    * @exception IllegalArgumentException
    *               <ul>
    *               <li>ERROR_INVALID_RANGE - if start is greater than end</li>
    *               </ul>
    * @exception SWTException
    *               <ul>
    *               <li>ERROR_WIDGET_DISPOSED - if the receiver has been
    *               disposed</li>
    *               <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
    *               thread that created the receiver</li>
    *               </ul>
    */
   public void deselect(final int start, final int end) {
      deselect(start, end, true);
   }

   private void deselect(final int start, final int end, final boolean shouldFireEvents) {
      checkWidget();
      if (start > end) {
         SWT.error(SWT.ERROR_INVALID_RANGE);
      }
      final List<MT_DLItem> toBeRemoved = new ArrayList<>();

      for (int index = start; index <= end; index++) {
         if (index < 0 || index >= _allItems.size()) {
            continue;
         }
         toBeRemoved.add(_allSelectedItems.get(index));
      }

      for (final MT_DLItem item : toBeRemoved) {
         _allSelectedItems.remove(item);
         if (shouldFireEvents) {
            fireSelectionEvent(item);
         }
      }
      if (shouldFireEvents) {
         fireSelectionChangeEvent(toBeRemoved);
      }
      toBeRemoved.clear();
      redrawTables();
   }

   /**
    * Deselects the items at the given zero-relative indices in the receiver. If
    * the item at the given zero-relative index in the receiver is selected, it is
    * deselected. If the item at the index was not selected, it remains deselected.
    * Indices that are out of range and duplicate indices are ignored.
    *
    * @param indices
    *           the array of indices for the items to deselect
    *
    * @exception IllegalArgumentException
    *               <ul>
    *               <li>ERROR_NULL_ARGUMENT - if the set of indices is null</li>
    *               </ul>
    * @exception SWTException
    *               <ul>
    *               <li>ERROR_WIDGET_DISPOSED - if the receiver has been
    *               disposed</li>
    *               <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
    *               thread that created the receiver</li>
    *               </ul>
    */
   public void deselect(final int[] indices) {
      deselect(indices, true);
   }

   /**
    * Deselects the items at the given zero-relative indices in the receiver. If
    * the item at the given zero-relative index in the receiver is selected, it is
    * deselected. If the item at the index was not selected, it remains deselected.
    * Indices that are out of range and duplicate indices are ignored.
    *
    * @param indices
    *           the array of indices for the items to deselect
    *
    * @exception IllegalArgumentException
    *               <ul>
    *               <li>ERROR_NULL_ARGUMENT - if the set of indices is null</li>
    *               </ul>
    * @exception SWTException
    *               <ul>
    *               <li>ERROR_WIDGET_DISPOSED - if the receiver has been
    *               disposed</li>
    *               <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
    *               thread that created the receiver</li>
    *               </ul>
    */
   private void deselect(final int[] indices, final boolean shouldFireEvents) {
      checkWidget();
      if (indices == null) {
         SWT.error(SWT.ERROR_NULL_ARGUMENT);
      }

      final List<MT_DLItem> toBeRemoved = new ArrayList<>();

      for (final int index : indices) {
         if (index < 0 || index >= _allItems.size()) {
            continue;
         }
         toBeRemoved.add(_allSelectedItems.get(index));
      }

      for (final MT_DLItem item : toBeRemoved) {
         _allSelectedItems.remove(item);
         if (shouldFireEvents) {
            fireSelectionEvent(item);
         }
      }
      if (shouldFireEvents) {
         fireSelectionChangeEvent(toBeRemoved);
      }

      toBeRemoved.clear();
      redrawTables();
   }

   /**
    * Deselects all selected items in the receiver.
    *
    * @exception SWTException
    *               <ul>
    *               <li>ERROR_WIDGET_DISPOSED - if the receiver has been
    *               disposed</li>
    *               <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
    *               thread that created the receiver</li>
    *               </ul>
    */
   public void deselectAll() {
      deselectAll(true);
   }

   /**
    * Deselects all selected items in the receiver.
    *
    * @exception SWTException
    *               <ul>
    *               <li>ERROR_WIDGET_DISPOSED - if the receiver has been
    *               disposed</li>
    *               <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
    *               thread that created the receiver</li>
    *               </ul>
    */
   public void deselectAll(final boolean shouldFireEvents) {
      checkWidget();
      _allItems.addAll(_allSelectedItems);

      final List<MT_DLItem> deselectedItems = new ArrayList<>();
      for (final MT_DLItem item : _allSelectedItems) {
         item.setLastAction(LAST_ACTION.DESELECTION);
         deselectedItems.add(item);
         if (shouldFireEvents) {
            fireSelectionEvent(item);
         }
      }
      fireSelectionChangeEvent(deselectedItems);

      _allSelectedItems.clear();
      redrawTables();
   }

   /**
    * Deselects all selected items in the receiver.
    *
    * @exception SWTException
    *               <ul>
    *               <li>ERROR_WIDGET_DISPOSED - if the receiver has been
    *               disposed</li>
    *               <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
    *               thread that created the receiver</li>
    *               </ul>
    */
   public void deselectAllDoNotFireEvent() {
      deselectAll(false);
   }

   /**
    * Deselects the item at the given zero-relative index in the receiver. If the
    * item at the index was already deselected, it remains deselected. Indices that
    * are out of range are ignored.
    *
    * @param index
    *           the index of the item to deselect
    *
    * @exception SWTException
    *               <ul>
    *               <li>ERROR_WIDGET_DISPOSED - if the receiver has been
    *               disposed</li>
    *               <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
    *               thread that created the receiver</li>
    *               </ul>
    */
   public void deselectDoNotFireEvent(final int index) {
      deselect(index, false);
   }

   /**
    * Deselects the items at the given zero-relative indices in the receiver. If
    * the item at the given zero-relative index in the receiver is selected, it is
    * deselected. If the item at the index was not selected, it remains deselected.
    * The range of the indices is inclusive. Indices that are out of range are
    * ignored.
    *
    * @param start
    *           the start index of the items to deselect
    * @param end
    *           the end index of the items to deselect
    *
    * @exception IllegalArgumentException
    *               <ul>
    *               <li>ERROR_INVALID_RANGE - if start is greater than end</li>
    *               </ul>
    * @exception SWTException
    *               <ul>
    *               <li>ERROR_WIDGET_DISPOSED - if the receiver has been
    *               disposed</li>
    *               <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
    *               thread that created the receiver</li>
    *               </ul>
    */
   public void deselectDoNotFireEvent(final int start, final int end) {
      deselect(start, end, false);
   }

   /**
    * Deselects the items at the given zero-relative indices in the receiver. If
    * the item at the given zero-relative index in the receiver is selected, it is
    * deselected. If the item at the index was not selected, it remains deselected.
    * Indices that are out of range and duplicate indices are ignored.
    *
    * @param indices
    *           the array of indices for the items to deselect
    *
    * @exception IllegalArgumentException
    *               <ul>
    *               <li>ERROR_NULL_ARGUMENT - if the set of indices is null</li>
    *               </ul>
    * @exception SWTException
    *               <ul>
    *               <li>ERROR_WIDGET_DISPOSED - if the receiver has been
    *               disposed</li>
    *               <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
    *               thread that created the receiver</li>
    *               </ul>
    */
   public void deselectDoNotFireEvent(final int[] indices) {
      deselect(indices, false);
   }

   /**
    * Deselect a given item
    */
   protected void deselectItem() {
      if (_tableAllSelectedItems.getSelectionCount() == 0) {
         return;
      }
      final List<MT_DLItem> deselectedItems = new ArrayList<>();
      for (final TableItem tableItem : _tableAllSelectedItems.getSelection()) {
         final MT_DLItem item = (MT_DLItem) tableItem.getData();
         item.setLastAction(LAST_ACTION.DESELECTION);
         deselectedItems.add(item);
         _allItems.add(item);
         _allSelectedItems.remove(item);
         fireSelectionEvent(item);
      }
      fireSelectionChangeEvent(deselectedItems);
      redrawTables();
   }

   /**
    * Fill a table with data
    *
    * @param table
    *           table to be filled
    * @param listOfData
    *           list of data
    */
   private void fillData(final Table table, final boolean isSelected) {

      final List<MT_DLItem> listOfData = isSelected ? _allSelectedItems : _allItems;

      int counter = 0;

      for (final MT_DLItem item : listOfData) {

         final TableItem tableItem = new TableItem(table, SWT.NONE);
         tableItem.setData(item);

         if (item.getBackground() != null) {
            tableItem.setBackground(item.getBackground());
         }

         if (item.getForeground() != null) {
            tableItem.setForeground(item.getForeground());
         }

         if (item.getImage() != null) {
            tableItem.setImage(0, item.getImage());
         }

         if (item.getFont() != null) {
            tableItem.setFont(item.getFont());
         }

         final String text1 = item.getText();
         final String text2 = item.getText2();

         if (text1 != null) {
            tableItem.setText(1, text1);
         }
         if (text2 != null) {
            tableItem.setText(2, text2);
         }

         if (configuration != null && item.getBackground() == null && counter % 2 == 0) {
            if (isSelected) {
               tableItem.setBackground(configuration.getSelectionOddLinesColor());
            } else {
               tableItem.setBackground(configuration.getItemsOddLinesColor());
            }
         }
         counter++;
      }
   }

   private void fireSelectionChangeEvent(final List<MT_DLItem> items) {
      if (selectionChangeListeners == null) {
         return;
      }

      final Event event = new Event();
      event.button = 1;
      event.display = getDisplay();
      event.item = null;
      event.widget = this;
      final MT_SelectionChangeEvent selectionChangeEvent = new MT_SelectionChangeEvent(event);
      selectionChangeEvent.setItems(items);

      for (final MT_SelectionChangeListener listener : selectionChangeListeners) {
         listener.widgetSelected(selectionChangeEvent);
      }
   }

   /**
    * Call all selection listeners
    *
    * @param item
    *           selected item
    */
   private void fireSelectionEvent(final MT_DLItem item) {
      final Event event = new Event();
      event.button = 1;
      event.display = getDisplay();
      event.item = null;
      event.widget = this;
      event.data = item;

      SelectionListenerUtil.fireSelectionListeners(this, event);
   }

   /**
    * Returns the configuration of the receiver.
    *
    * @return the current configuration of the receiver
    *
    * @exception SWTException
    *               <ul>
    *               <li>ERROR_WIDGET_DISPOSED - if the receiver has been
    *               disposed</li>
    *               <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
    *               thread that created the receiver</li>
    *               </ul>
    */
   public MT_DLConfiguration getConfiguration() {
      checkWidget();
      return configuration;
   }

   /**
    * Returns the item at the given, zero-relative index in the receiver. Throws an
    * exception if the index is out of range.
    *
    * @param index
    *           the index of the item to return
    *
    * @return the item at the given index
    *
    * @exception IllegalArgumentException
    *               <ul>
    *               <li>ERROR_INVALID_RANGE - if the index is not between 0 and
    *               the number of elements in the list minus 1 (inclusive)</li>
    *               </ul>
    * @exception SWTException
    *               <ul>
    *               <li>ERROR_WIDGET_DISPOSED - if the receiver has been
    *               disposed</li>
    *               <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
    *               thread that created the receiver</li>
    *               </ul>
    */

   public MT_DLItem getItem(final int index) {
      checkWidget();
      if (index < 0 || index >= _allItems.size()) {
         SWT.error(SWT.ERROR_INVALID_ARGUMENT);
      }
      return _allItems.get(index);
   }

   /**
    * Returns the number of items contained in the receiver.
    *
    * @return the number of items
    *
    * @exception SWTException
    *               <ul>
    *               <li>ERROR_WIDGET_DISPOSED - if the receiver has been
    *               disposed</li>
    *               <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
    *               thread that created the receiver</li>
    *               </ul>
    */
   public int getItemCount() {
      checkWidget();
      return _allItems.size();
   }

   /**
    * Returns a (possibly empty) array of <code>MT_DLItem</code>s which are the items
    * in the receiver.
    * <p>
    * Note: This is not the actual structure used by the receiver to maintain its
    * list of items, so modifying the array will not affect the receiver.
    * </p>
    *
    * @return the items in the receiver's list
    *
    * @exception SWTException
    *               <ul>
    *               <li>ERROR_WIDGET_DISPOSED - if the receiver has been
    *               disposed</li>
    *               <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
    *               thread that created the receiver</li>
    *               </ul>
    */
   public MT_DLItem[] getItems() {
      checkWidget();
      return _allItems.toArray(new MT_DLItem[_allItems.size()]);
   }

   /**
    * Returns a (possibly empty) list of <code>MT_DLItem</code>s which are the items
    * in the receiver.
    * <p>
    * Note: This is not the actual structure used by the receiver to maintain its
    * list of items, so modifying the array will not affect the receiver.
    * </p>
    *
    * @return the items in the receiver's list
    *
    * @exception SWTException
    *               <ul>
    *               <li>ERROR_WIDGET_DISPOSED - if the receiver has been
    *               disposed</li>
    *               <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
    *               thread that created the receiver</li>
    *               </ul>
    */
   public List<MT_DLItem> getItemsAsList() {
      checkWidget();
      return new ArrayList<>(_allItems);
   }

   /**
    * Returns an array of <code>MT_DLItem</code>s that are currently selected in the
    * receiver. An empty array indicates that no items are selected.
    * <p>
    * Note: This is not the actual structure used by the receiver to maintain its
    * selection, so modifying the array will not affect the receiver.
    * </p>
    *
    * @return an array representing the selection
    *
    * @exception SWTException
    *               <ul>
    *               <li>ERROR_WIDGET_DISPOSED - if the receiver has been
    *               disposed</li>
    *               <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
    *               thread that created the receiver</li>
    *               </ul>
    */
   public MT_DLItem[] getSelection() {
      checkWidget();
      return _allSelectedItems.toArray(new MT_DLItem[_allSelectedItems.size()]);
   }

   /**
    * Returns a list of <code>MT_DLItem</code>s that are currently selected in the
    * receiver. An empty array indicates that no items are selected.
    * <p>
    * Note: This is not the actual structure used by the receiver to maintain its
    * selection, so modifying the array will not affect the receiver.
    * </p>
    *
    * @return an array representing the selection
    *
    * @exception SWTException
    *               <ul>
    *               <li>ERROR_WIDGET_DISPOSED - if the receiver has been
    *               disposed</li>
    *               <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
    *               thread that created the receiver</li>
    *               </ul>
    */
   public List<MT_DLItem> getSelectionAsList() {
      checkWidget();
      return new ArrayList<>(_allSelectedItems);
   }

   /**
    * Returns the number of selected items contained in the receiver.
    *
    * @return the number of selected items
    *
    * @exception SWTException
    *               <ul>
    *               <li>ERROR_WIDGET_DISPOSED - if the receiver has been
    *               disposed</li>
    *               <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
    *               thread that created the receiver</li>
    *               </ul>
    */
   public int getSelectionCount() {
      checkWidget();
      return _allSelectedItems.size();
   }

   /**
    * @return <code>true</code> if any item contains an image
    */
   private boolean isItemsContainImage() {

      for (final MT_DLItem item : _allItems) {
         if (item.getImage() != null) {
            return true;
         }
      }

      for (final MT_DLItem item : _allSelectedItems) {
         if (item.getImage() != null) {
            return true;
         }
      }

      return false;
   }

   private void modifyButtonImages() {

      if (configuration.getDoubleDownImage() != null) {
         _btnMoveLast.setImage(configuration.getDoubleDownImage());
      }
      if (configuration.getDoubleUpImage() != null) {
         _btnMoveFirst.setImage(configuration.getDoubleUpImage());
      }
      if (configuration.getDoubleLeftImage() != null) {
         _btnDeselectAll.setImage(configuration.getDoubleLeftImage());
      }
      if (configuration.getDoubleRightImage() != null) {
         _btnSelectAll.setImage(configuration.getDoubleRightImage());
      }
      if (configuration.getDownImage() != null) {
         _btnMoveDown.setImage(configuration.getDownImage());
      }
      if (configuration.getUpImage() != null) {
         _btnMoveUp.setImage(configuration.getUpImage());
      }
      if (configuration.getLeftImage() != null) {
         _btnDeselect.setImage(configuration.getLeftImage());
      }
      if (configuration.getRightImage() != null) {
         _btnSelect.setImage(configuration.getRightImage());
      }
   }

   private void modifyButtonVisibility() {

      _btnMoveLast.setVisible(configuration.isDoubleDownVisible());
      _btnMoveFirst.setVisible(configuration.isDoubleUpVisible());
      _btnDeselectAll.setVisible(configuration.isDoubleLeftVisible());
      _btnSelectAll.setVisible(configuration.isDoubleRightVisible());
      _btnMoveDown.setVisible(configuration.isDownVisible());
      _btnMoveUp.setVisible(configuration.isUpVisible());
   }

   private void modifyPanelsColors() {

      if (configuration.getItemsBackgroundColor() != null) {
         _tableAllItems.setBackground(configuration.getItemsBackgroundColor());
      }
      if (configuration.getItemsForegroundColor() != null) {
         _tableAllItems.setForeground(configuration.getItemsForegroundColor());
      }
      if (configuration.getSelectionBackgroundColor() != null) {
         _tableAllSelectedItems.setBackground(configuration.getSelectionBackgroundColor());
      }
      if (configuration.getSelectionForegroundColor() != null) {
         _tableAllSelectedItems.setForeground(configuration.getSelectionForegroundColor());
      }
   }

   private void modifyTextAlignment() {

      recreateTableColumns(_tableAllItems, configuration.getItemsTextAlignment());
      recreateTableColumns(_tableAllSelectedItems, configuration.getSelectionTextAlignment());
   }

   /**
    * Move the selected item down
    */
   protected void moveSelectionDown() {
      if (_tableAllSelectedItems.getSelectionCount() == 0) {
         return;
      }

      for (final int index : _tableAllSelectedItems.getSelectionIndices()) {
         if (index == _tableAllSelectedItems.getItemCount() - 1) {
            _tableAllSelectedItems.forceFocus();
            return;
         }
      }

      final int[] newSelection = new int[_tableAllSelectedItems.getSelectionCount()];
      int newSelectionIndex = 0;
      for (final TableItem tableItem : _tableAllSelectedItems.getSelection()) {
         final int position = _allSelectedItems.indexOf(tableItem.getData());
         swap(position, position + 1);
         newSelection[newSelectionIndex++] = position + 1;
      }

      redrawTables();
      _tableAllSelectedItems.select(newSelection);
      _tableAllSelectedItems.forceFocus();

      fireSelectionChangeEvent(getSelectionAsList());
   }

   /**
    * Move the selected item to the first position
    */
   protected void moveSelectionToFirstPosition() {
      if (_tableAllSelectedItems.getSelectionCount() == 0) {
         return;
      }

      int index = 0;
      for (final TableItem tableItem : _tableAllSelectedItems.getSelection()) {
         final MT_DLItem item = (MT_DLItem) tableItem.getData();
         _allSelectedItems.remove(item);
         _allSelectedItems.add(index++, item);
      }

      redrawTables();
      _tableAllSelectedItems.select(0, index - 1);
      _tableAllSelectedItems.forceFocus();

      fireSelectionChangeEvent(getSelectionAsList());
   }

   /**
    * Move the selected item to the last position
    */
   protected void moveSelectionToLastPosition() {
      if (_tableAllSelectedItems.getSelectionCount() == 0) {
         return;
      }

      final int numberOfSelectedElements = _tableAllSelectedItems.getSelectionCount();
      for (final TableItem tableItem : _tableAllSelectedItems.getSelection()) {
         final MT_DLItem item = (MT_DLItem) tableItem.getData();
         _allSelectedItems.remove(item);
         _allSelectedItems.add(item);
      }

      redrawTables();
      final int numberOfElements = _tableAllSelectedItems.getItemCount();
      _tableAllSelectedItems.select(numberOfElements - numberOfSelectedElements, numberOfElements - 1);
      _tableAllSelectedItems.forceFocus();

      fireSelectionChangeEvent(getSelectionAsList());
   }

   /**
    * Move the selected item up
    */
   protected void moveSelectionUp() {
      if (_tableAllSelectedItems.getSelectionCount() == 0) {
         return;
      }

      for (final int index : _tableAllSelectedItems.getSelectionIndices()) {
         if (index == 0) {
            _tableAllSelectedItems.forceFocus();
            return;
         }
      }

      final int[] newSelection = new int[_tableAllSelectedItems.getSelectionCount()];
      int newSelectionIndex = 0;
      for (final TableItem tableItem : _tableAllSelectedItems.getSelection()) {
         final int position = _allSelectedItems.indexOf(tableItem.getData());
         swap(position, position - 1);
         newSelection[newSelectionIndex++] = position - 1;
      }

      redrawTables();
      _tableAllSelectedItems.select(newSelection);
      _tableAllSelectedItems.forceFocus();

      fireSelectionChangeEvent(getSelectionAsList());
   }

   private void recreateTableColumns(final Table table, final int textAlignment) {

      for (final TableColumn tc : table.getColumns()) {
         tc.dispose();
      }

      new TableColumn(table, SWT.CENTER); // image
      new TableColumn(table, textAlignment); // text
      new TableColumn(table, textAlignment); // text2
   }

   /**
    * Redraw a given table
    *
    * @param table
    *           table to be redrawned
    * @param isSelected
    *           if <code>true</code>, fill the table with the selection.
    *           Otherwise, fill the table with the unselected items.
    */
   private void redrawTable(final Table table, final boolean isSelected) {

      clean(table);

      fillData(table, isSelected);
   }

   /**
    * Redraws all tables that compose this widget
    */
   private void redrawTables() {

      setRedraw(false);

      redrawTable(_tableAllItems, false);
      redrawTable(_tableAllSelectedItems, true);

      final Rectangle bounds = getBounds();
      this.setBounds(bounds.x, bounds.y, bounds.width, bounds.height);

      setRedraw(true);
   }

   /**
    * Removes the item from the receiver at the given zero-relative index.
    *
    * @param index
    *           the index for the item
    *
    * @exception IllegalArgumentException
    *               <ul>
    *               <li>ERROR_INVALID_RANGE - if the index is not between 0 and
    *               the number of elements in the list minus 1 (inclusive)</li>
    *               </ul>
    * @exception SWTException
    *               <ul>
    *               <li>ERROR_WIDGET_DISPOSED - if the receiver has been
    *               disposed</li>
    *               <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
    *               thread that created the receiver</li>
    *               </ul>
    */
   public void remove(final int index) {
      checkWidget();
      if (index < 0 || index >= _allItems.size()) {
         SWT.error(SWT.ERROR_INVALID_ARGUMENT);
      }
      _allItems.remove(index);
      redrawTables();
   }

   /**
    * Removes the items from the receiver which are between the given zero-relative
    * start and end indices (inclusive).
    *
    * @param start
    *           the start of the range
    * @param end
    *           the end of the range
    *
    * @exception IllegalArgumentException
    *               <ul>
    *               <li>ERROR_INVALID_RANGE - if either the start or end are not
    *               between 0 and the number of elements in the list minus 1
    *               (inclusive) or if start>end</li>
    *               </ul>
    * @exception SWTException
    *               <ul>
    *               <li>ERROR_WIDGET_DISPOSED - if the receiver has been
    *               disposed</li>
    *               <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
    *               thread that created the receiver</li>
    *               </ul>
    */
   public void remove(final int start, final int end) {
      checkWidget();
      if (start > end) {
         SWT.error(SWT.ERROR_INVALID_ARGUMENT);
      }
      for (int index = start; index < end; index++) {
         if (index < 0 || index >= _allItems.size()) {
            SWT.error(SWT.ERROR_INVALID_ARGUMENT);
         }
         _allItems.remove(index);
      }
      redrawTables();
   }

   /**
    * Removes the items from the receiver at the given zero-relative indices.
    *
    * @param indices
    *           the array of indices of the items
    *
    * @exception IllegalArgumentException
    *               <ul>
    *               <li>ERROR_INVALID_RANGE - if the index is not between 0 and
    *               the number of elements in the list minus 1 (inclusive)</li>
    *               <li>ERROR_NULL_ARGUMENT - if the indices array is null</li>
    *               </ul>
    * @exception SWTException
    *               <ul>
    *               <li>ERROR_WIDGET_DISPOSED - if the receiver has been
    *               disposed</li>
    *               <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
    *               thread that created the receiver</li>
    *               </ul>
    */
   public void remove(final int[] indices) {
      checkWidget();
      for (final int index : indices) {
         if (index < 0 || index >= _allItems.size()) {
            SWT.error(SWT.ERROR_INVALID_ARGUMENT);
         }
         _allItems.remove(index);
      }
      redrawTables();
   }

   /**
    * Searches the receiver's list starting at the first item until an item is
    * found that is equal to the argument, and removes that item from the list.
    *
    * @param item
    *           the item to remove
    *
    * @exception IllegalArgumentException
    *               <ul>
    *               <li>ERROR_NULL_ARGUMENT - if the item is null</li>
    *               <li>ERROR_INVALID_ARGUMENT - if the item is not found in the
    *               list</li>
    *               </ul>
    * @exception SWTException
    *               <ul>
    *               <li>ERROR_WIDGET_DISPOSED - if the receiver has been
    *               disposed</li>
    *               <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
    *               thread that created the receiver</li>
    *               </ul>
    */
   public void remove(final MT_DLItem item) {
      checkWidget();
      if (item == null) {
         SWT.error(SWT.ERROR_NULL_ARGUMENT);
      }
      if (!_allItems.contains(item)) {
         SWT.error(SWT.ERROR_INVALID_ARGUMENT);
      }
      _allItems.remove(item);
      redrawTables();
   }

   /**
    * Removes all of the items from the receiver.
    * <p>
    *
    * @exception SWTException
    *               <ul>
    *               <li>ERROR_WIDGET_DISPOSED - if the receiver has been
    *               disposed</li>
    *               <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
    *               thread that created the receiver</li>
    *               </ul>
    */
   public void removeAll() {
      checkWidget();
      _allItems.clear();
      redrawTables();
   }

   /**
    * Removes the listener from the collection of listeners who will be notified
    * when the user changes the receiver's selection.
    *
    * @param listener
    *           the listener which should no longer be notified
    *
    * @exception IllegalArgumentException
    *               <ul>
    *               <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
    *               </ul>
    * @exception SWTException
    *               <ul>
    *               <li>ERROR_WIDGET_DISPOSED - if the receiver has been
    *               disposed</li>
    *               <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
    *               thread that created the receiver</li>
    *               </ul>
    *
    * @see SelectionChangeListener
    * @see #addSelectionChangeListener
    */
   public void removeSelectionChangeListener(final MT_SelectionChangeListener listener) {
      checkWidget();
      if (listener == null) {
         SWT.error(SWT.ERROR_NULL_ARGUMENT);
      }
      if (selectionChangeListeners == null) {
         return;
      }
      selectionChangeListeners.remove(listener);
   }

   /**
    * Removes the listener from the collection of listeners who will be notified
    * when the user changes the receiver's selection.
    *
    * @param listener
    *           the listener which should no longer be notified
    *
    * @exception IllegalArgumentException
    *               <ul>
    *               <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
    *               </ul>
    * @exception SWTException
    *               <ul>
    *               <li>ERROR_WIDGET_DISPOSED - if the receiver has been
    *               disposed</li>
    *               <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
    *               thread that created the receiver</li>
    *               </ul>
    *
    * @see SelectionListener
    * @see #addSelectionListener
    */
   public void removeSelectionListener(final SelectionListener listener) {
      checkWidget();
      SelectionListenerUtil.removeSelectionListener(this, listener);
   }

   private void resetButton(final Button button, final Image image) {

      button.setImage(image);
      button.setVisible(true);

      SWTGraphicUtil.addDisposer(button, image);
   }

   private void resetConfigurationToDefault() {

      _tableAllItems.setBackground(null);
      _tableAllItems.setForeground(null);
      _tableAllSelectedItems.setBackground(null);
      _tableAllSelectedItems.setForeground(null);

      recreateTableColumns(_tableAllItems, SWT.LEFT);
      recreateTableColumns(_tableAllSelectedItems, SWT.LEFT);

// SET_FORMATTING_OFF

      resetButton(_btnMoveDown,      _imageMoveDown);
      resetButton(_btnMoveUp,        _imageMoveUp);
      resetButton(_btnDeselect,      _imageMoveLeft);
      resetButton(_btnSelect,        _imageMoveRight);
      resetButton(_btnMoveLast,      _imageMoveDown_2x);
      resetButton(_btnMoveFirst,     _imageMoveUp_2x);
      resetButton(_btnDeselectAll,   _imageMoveLeft_2x);
      resetButton(_btnSelectAll,     _imageMoveRight_2x);

// SET_FORMATTING_ON
   }

   /**
    * Selects the item at the given zero-relative index in the receiver's list. If
    * the item at the index was already selected, it remains selected. Indices that
    * are out of range are ignored.
    *
    * @param index
    *           the index of the item to select
    *
    * @exception SWTException
    *               <ul>
    *               <li>ERROR_WIDGET_DISPOSED - if the receiver has been
    *               disposed</li>
    *               <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
    *               thread that created the receiver</li>
    *               </ul>
    */
   public void select(final int index) {
      select(index, true);
   }

   private void select(final int index, final boolean shouldFireEvents) {
      checkWidget();
      if (index < 0 || index >= _allItems.size()) {
         return;
      }
      final List<MT_DLItem> selectedItems = new ArrayList<>();
      final MT_DLItem item = _allItems.remove(index);
      item.setLastAction(LAST_ACTION.SELECTION);
      selectedItems.add(item);
      _allSelectedItems.add(item);

      if (shouldFireEvents) {
         fireSelectionEvent(item);
         fireSelectionChangeEvent(selectedItems);
      }

      redrawTables();
   }

   /**
    * Selects the items in the range specified by the given zero-relative indices
    * in the receiver. The range of indices is inclusive. The current selection is
    * not cleared before the new items are selected.
    * <p>
    * If an item in the given range is not selected, it is selected. If an item in
    * the given range was already selected, it remains selected. Indices that are
    * out of range are ignored and no items will be selected if start is greater
    * than end. If the receiver is single-select and there is more than one item in
    * the given range, then all indices are ignored.
    *
    * @param start
    *           the start of the range
    * @param end
    *           the end of the range
    *
    * @exception SWTException
    *               <ul>
    *               <li>ERROR_WIDGET_DISPOSED - if the receiver has been
    *               disposed</li>
    *               <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
    *               thread that created the receiver</li>
    *               </ul>
    *
    * @see List#setSelection(int,int)
    */
   public void select(final int start, final int end) {
      select(start, end, true);
   }

   private void select(final int start, final int end, final boolean shouldFireEvents) {
      checkWidget();
      if (start > end) {
         SWT.error(SWT.ERROR_INVALID_RANGE);
      }
      final List<MT_DLItem> selectedItems = new ArrayList<>();
      for (int index = start; index <= end; index++) {
         if (index < 0 || index >= _allItems.size()) {
            continue;
         }
         final MT_DLItem item = _allItems.get(index);
         item.setLastAction(LAST_ACTION.SELECTION);
         selectedItems.add(item);
         _allSelectedItems.add(item);
         if (shouldFireEvents) {
            fireSelectionEvent(item);
         }
      }
      if (shouldFireEvents) {
         fireSelectionChangeEvent(selectedItems);
      }
      redrawTables();
   }

   /**
    * Selects the items at the given zero-relative indices in the receiver. The
    * current selection is not cleared before the new items are selected.
    * <p>
    * If the item at a given index is not selected, it is selected. If the item at
    * a given index was already selected, it remains selected. Indices that are out
    * of range and duplicate indices are ignored. If the receiver is single-select
    * and multiple indices are specified, then all indices are ignored.
    *
    * @param indices
    *           the array of indices for the items to select
    *
    * @exception IllegalArgumentException
    *               <ul>
    *               <li>ERROR_NULL_ARGUMENT - if the array of indices is null</li>
    *               </ul>
    * @exception SWTException
    *               <ul>
    *               <li>ERROR_WIDGET_DISPOSED - if the receiver has been
    *               disposed</li>
    *               <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
    *               thread that created the receiver</li>
    *               </ul>
    */
   public void select(final int[] indices) {
      select(indices, true);
   }

   private void select(final int[] indices, final boolean shouldFireEvents) {
      checkWidget();
      if (indices == null) {
         SWT.error(SWT.ERROR_NULL_ARGUMENT);
      }

      final List<MT_DLItem> selectedItems = new ArrayList<>();
      for (final int index : indices) {
         if (index < 0 || index >= _allItems.size()) {
            continue;
         }
         final MT_DLItem item = _allItems.get(index);
         item.setLastAction(LAST_ACTION.SELECTION);
         selectedItems.add(item);

         _allSelectedItems.add(item);
         if (shouldFireEvents) {
            fireSelectionEvent(item);
         }
      }
      _allItems.removeAll(selectedItems);
      if (shouldFireEvents) {
         fireSelectionChangeEvent(selectedItems);
      }
      redrawTables();
   }

   /**
    * Selects all of the items in the receiver.
    * <p>
    * If the receiver is single-select, do nothing.
    *
    * @exception SWTException
    *               <ul>
    *               <li>ERROR_WIDGET_DISPOSED - if the receiver has been
    *               disposed</li>
    *               <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
    *               thread that created the receiver</li>
    *               </ul>
    */
   public void selectAll() {
      selectAll(true);
   }

   private void selectAll(final boolean shouldFireEvents) {
      checkWidget();
      _allSelectedItems.addAll(_allItems);
      if (shouldFireEvents) {
         for (final MT_DLItem item : _allItems) {
            fireSelectionEvent(item);
         }
      }

      if (shouldFireEvents) {
         final List<MT_DLItem> selectedItems = new ArrayList<>();
         for (final MT_DLItem item : _allItems) {
            item.setLastAction(LAST_ACTION.SELECTION);
            selectedItems.add(item);
         }
         fireSelectionChangeEvent(selectedItems);
      }
      _allItems.clear();
      redrawTables();
   }

   /**
    * Selects all of the items in the receiver.
    * <p>
    * If the receiver is single-select, do nothing.
    *
    * @exception SWTException
    *               <ul>
    *               <li>ERROR_WIDGET_DISPOSED - if the receiver has been
    *               disposed</li>
    *               <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
    *               thread that created the receiver</li>
    *               </ul>
    */
   public void selectAllDoNotFireEvent() {
      selectAll(false);
   }

   /**
    * Selects the item at the given zero-relative index in the receiver's list. If
    * the item at the index was already selected, it remains selected. Indices that
    * are out of range are ignored.
    *
    * @param index
    *           the index of the item to select
    *
    * @exception SWTException
    *               <ul>
    *               <li>ERROR_WIDGET_DISPOSED - if the receiver has been
    *               disposed</li>
    *               <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
    *               thread that created the receiver</li>
    *               </ul>
    */
   public void selectDoNotFireEvent(final int index) {
      select(index, false);
   }

   /**
    * Selects the items in the range specified by the given zero-relative indices
    * in the receiver. The range of indices is inclusive. The current selection is
    * not cleared before the new items are selected.
    * <p>
    * If an item in the given range is not selected, it is selected. If an item in
    * the given range was already selected, it remains selected. Indices that are
    * out of range are ignored and no items will be selected if start is greater
    * than end. If the receiver is single-select and there is more than one item in
    * the given range, then all indices are ignored.
    *
    * @param start
    *           the start of the range
    * @param end
    *           the end of the range
    *
    * @exception SWTException
    *               <ul>
    *               <li>ERROR_WIDGET_DISPOSED - if the receiver has been
    *               disposed</li>
    *               <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
    *               thread that created the receiver</li>
    *               </ul>
    *
    * @see List#setSelection(int,int)
    */
   public void selectDoNotFireEvent(final int start, final int end) {
      select(start, end, false);
   }

   /**
    * Selects the items at the given zero-relative indices in the receiver. The
    * current selection is not cleared before the new items are selected.
    * <p>
    * If the item at a given index is not selected, it is selected. If the item at
    * a given index was already selected, it remains selected. Indices that are out
    * of range and duplicate indices are ignored. If the receiver is single-select
    * and multiple indices are specified, then all indices are ignored.
    *
    * @param indices
    *           the array of indices for the items to select
    *
    * @exception IllegalArgumentException
    *               <ul>
    *               <li>ERROR_NULL_ARGUMENT - if the array of indices is null</li>
    *               </ul>
    * @exception SWTException
    *               <ul>
    *               <li>ERROR_WIDGET_DISPOSED - if the receiver has been
    *               disposed</li>
    *               <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
    *               thread that created the receiver</li>
    *               </ul>
    */
   public void selectDoNotFireEvent(final int[] indices) {
      select(indices, false);
   }

   /**
    * Select a given item
    */
   protected void selectItem() {
      if (_tableAllItems.getSelectionCount() == 0) {
         return;
      }
      final List<MT_DLItem> selectedItems = new ArrayList<>();
      for (final TableItem tableItem : _tableAllItems.getSelection()) {
         final MT_DLItem item = (MT_DLItem) tableItem.getData();
         item.setLastAction(LAST_ACTION.SELECTION);
         selectedItems.add(item);
         _allSelectedItems.add(item);
         _allItems.remove(item);
         fireSelectionEvent(item);
      }
      fireSelectionChangeEvent(selectedItems);

      redrawTables();
   }

   /**
    * @see org.eclipse.swt.widgets.Control#setBounds(int, int, int, int)
    */
   @Override
   public void setBounds(final int x, final int y, final int width, final int height) {

      super.setBounds(x, y, width, height);

      layout(true);

      final boolean isContainsImage = isItemsContainImage();

      final Point itemsTableDefaultSize = _tableAllItems.computeSize(SWT.DEFAULT, SWT.DEFAULT);
      final Point selectionTableDefaultSize = _tableAllSelectedItems.computeSize(SWT.DEFAULT, SWT.DEFAULT);

      int itemsTableSize = _tableAllItems.getSize().x;

      if (itemsTableDefaultSize.y > _tableAllItems.getSize().y) {
         itemsTableSize -= _tableAllItems.getVerticalBar().getSize().x + 1;
      }

      int selectionTableSize = _tableAllSelectedItems.getSize().x;

      if (selectionTableDefaultSize.y > _tableAllSelectedItems.getSize().y) {
         selectionTableSize -= _tableAllSelectedItems.getVerticalBar().getSize().x;
      }

      if (isContainsImage) {

         _tableAllItems.getColumn(0).pack();
         _tableAllItems.getColumn(1).setWidth(itemsTableSize - _tableAllItems.getColumn(0).getWidth());

         _tableAllSelectedItems.getColumn(0).pack();
         _tableAllSelectedItems.getColumn(1).setWidth(selectionTableSize - _tableAllSelectedItems.getColumn(0).getWidth());

      } else {

         final double textWidth = 0.5;
         final double partWidth = 1 - textWidth;

         _tableAllItems.getColumn(0).setWidth(0);
         _tableAllItems.getColumn(1).setWidth((int) (itemsTableSize * textWidth));
         _tableAllItems.getColumn(2).setWidth((int) (itemsTableSize * partWidth));

         _tableAllSelectedItems.getColumn(0).setWidth(0);
         _tableAllSelectedItems.getColumn(1).setWidth((int) (selectionTableSize * textWidth));
         _tableAllSelectedItems.getColumn(2).setWidth((int) (selectionTableSize * partWidth));
      }

   }

   /**
    * Sets the receiver's configuration
    *
    * @param configuration
    *           the new configuration
    *
    * @exception SWTException
    *               <ul>
    *               <li>ERROR_WIDGET_DISPOSED - if the receiver has been
    *               disposed</li>
    *               <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
    *               thread that created the receiver</li>
    *               </ul>
    */
   public void setConfiguration(final MT_DLConfiguration configuration) {
      checkWidget();
      this.configuration = configuration;
      applyNewConfiguration();
   }

   @Override
   public void setEnabled(final boolean isEnabled) {

      _tableAllItems.setEnabled(isEnabled);
      _tableAllSelectedItems.setEnabled(isEnabled);

      _btnMoveFirst.setEnabled(isEnabled);
      _btnMoveLast.setEnabled(isEnabled);
      _btnMoveUp.setEnabled(isEnabled);
      _btnMoveDown.setEnabled(isEnabled);

      _btnSelect.setEnabled(isEnabled);
      _btnSelectAll.setEnabled(isEnabled);
      _btnDeselect.setEnabled(isEnabled);
      _btnDeselectAll.setEnabled(isEnabled);
   }

   /**
    * Sets the item in the receiver's list at the given zero-relative index to the
    * item argument.
    *
    * @param index
    *           the index for the item
    * @param item
    *           the new item
    *
    * @exception IllegalArgumentException
    *               <ul>
    *               <li>ERROR_INVALID_RANGE - if the index is not between 0 and
    *               the number of elements in the list minus 1 (inclusive)</li>
    *               <li>ERROR_NULL_ARGUMENT - if the item is null</li>
    *               </ul>
    * @exception SWTException
    *               <ul>
    *               <li>ERROR_WIDGET_DISPOSED - if the receiver has been
    *               disposed</li>
    *               <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
    *               thread that created the receiver</li>
    *               </ul>
    */
   public void setItem(final int index, final MT_DLItem item) {
      checkWidget();
      if (item == null) {
         SWT.error(SWT.ERROR_NULL_ARGUMENT);
      }
      if (index < 0 || index >= _allItems.size()) {
         SWT.error(SWT.ERROR_INVALID_RANGE);
      }
      _allItems.set(index, item);
      redrawTables();
   }

   /**
    * Sets the receiver's items to be the given list of items.
    *
    * @param items
    *           the list of items
    *
    * @exception IllegalArgumentException
    *               <ul>
    *               <li>ERROR_NULL_ARGUMENT - if the items list is null</li>
    *               <li>ERROR_INVALID_ARGUMENT - if an item in the items list is
    *               null</li>
    *               </ul>
    * @exception SWTException
    *               <ul>
    *               <li>ERROR_WIDGET_DISPOSED - if the receiver has been
    *               disposed</li>
    *               <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
    *               thread that created the receiver</li>
    *               </ul>
    */
   public void setItems(final List<MT_DLItem> items) {

      checkWidget();

      if (items == null) {
         SWT.error(SWT.ERROR_NULL_ARGUMENT);
      }

      final List<MT_DLItem> unselectedItems = new ArrayList<>();
      final List<MT_DLItem> selectedItems = new ArrayList<>();

      for (final MT_DLItem item : items) {

         if (item == null) {
            SWT.error(SWT.ERROR_INVALID_ARGUMENT);
         }

         if (item.getLastAction() == LAST_ACTION.SELECTION) {
            selectedItems.add(item);
         } else {
            unselectedItems.add(item);
         }
      }

      _allItems.clear();
      _allItems.addAll(unselectedItems);

      _allSelectedItems.clear();
      _allSelectedItems.addAll(selectedItems);

      redrawTables();
   }

   /**
    * Sets the receiver's items to be the given array of items.
    *
    * @param items
    *           the array of items
    *
    * @exception IllegalArgumentException
    *               <ul>
    *               <li>ERROR_NULL_ARGUMENT - if the items array is null</li>
    *               <li>ERROR_INVALID_ARGUMENT - if an item in the items array is
    *               null</li>
    *               </ul>
    * @exception SWTException
    *               <ul>
    *               <li>ERROR_WIDGET_DISPOSED - if the receiver has been
    *               disposed</li>
    *               <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
    *               thread that created the receiver</li>
    *               </ul>
    */
   public void setItems(final MT_DLItem[] items) {

      checkWidget();

      if (items == null) {
         SWT.error(SWT.ERROR_NULL_ARGUMENT);
      }

      final List<MT_DLItem> temp = new ArrayList<>();

      for (final MT_DLItem item : items) {
         if (item == null) {
            SWT.error(SWT.ERROR_INVALID_ARGUMENT);
         }
         temp.add(item);
      }

      _allItems.clear();
      _allItems.addAll(temp);

      redrawTables();
   }

   /**
    * Swap 2 items
    *
    * @param first
    *           position of the first item to swap
    * @param second
    *           position of the second item to swap
    */
   private void swap(final int first, final int second) {
      final MT_DLItem temp = _allSelectedItems.get(first);
      _allSelectedItems.set(first, _allSelectedItems.get(second));
      _allSelectedItems.set(second, temp);
   }

}
