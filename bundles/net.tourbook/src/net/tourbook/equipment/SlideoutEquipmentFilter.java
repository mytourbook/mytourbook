/*******************************************************************************
 * Copyright (C) 2026 Wolfgang Schramm and Contributors
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
package net.tourbook.equipment;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.tooltip.AdvancedSlideout;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;

/**
 * Equipment filter slideout
 */
public class SlideoutEquipmentFilter extends AdvancedSlideout {

   private static final IDialogSettings _state = TourbookPlugin.getState("net.tourbook.equipment.SlideoutEquipmentFilter"); //$NON-NLS-1$

   private ToolItem                     _toolItem;

   /*
    * UI controls
    */
   private Composite     _shellContainer;

   private Button        _rdoFilter_Equipment_ContainsTours_Yes;
   private Button        _rdoFilter_Equipment_ContainsTours_No;
   private Button        _rdoFilter_Equipment_ContainsTours_Ignore;
   private Button        _rdoFilter_Equipment_Retired_IsRetired;
   private Button        _rdoFilter_Equipment_Retired_IsActive;
   private Button        _rdoFilter_Equipment_Retired_Ignore;

   private EquipmentView _equipmentView;

   public SlideoutEquipmentFilter(final ToolItem toolItem,
                                  final EquipmentView equipmentView) {

      super(toolItem.getParent(), _state, new int[] { 220, 100, 220, 100 });

      _toolItem = toolItem;
      _equipmentView = equipmentView;

      setTitleText("Equipment Filter");

      // prevent that the opened slideout is partly hidden
      setIsForceBoundsToBeInsideOfViewport(true);
   }

   @Override
   protected void createSlideoutContent(final Composite parent) {

      initUI(parent);

      createUI(parent);

      restoreState();
   }

   private Composite createUI(final Composite parent) {

      _shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults()
            .margins(0, 0)
            .applyTo(_shellContainer);
      {
         UI.createLabel(_shellContainer, "Choose which items to display");

         createUI_10_Filter(_shellContainer);
      }

      return _shellContainer;
   }

   private void createUI_10_Filter(final Composite parent) {

      final String tooltip = UI.EMPTY_STRING
            + "When 'Active equipment' is selected and an equipment is retired\n"
            + "but a contained part is still active, this equipment will be displayed.\n\n"
            + "An equal algorithm is applied for 'Retired equipment'";

      final SelectionListener defaultListener = SelectionListener.widgetSelectedAdapter(selectionEvent -> onModified(selectionEvent));

      final Composite container = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//      container.setBackground(UI.SYS_COLOR_BLUE);
      {
         {
            final Label label = UI.createLabel(container, "Retirement");
            GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.BEGINNING).applyTo(label);

            final Composite containerRetired = new Composite(container, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(containerRetired);
            GridLayoutFactory.fillDefaults().numColumns(1).applyTo(containerRetired);
            {
               {
                  _rdoFilter_Equipment_Retired_IsActive = new Button(containerRetired, SWT.RADIO);
                  _rdoFilter_Equipment_Retired_IsActive.setText("&Active equipment");
                  _rdoFilter_Equipment_Retired_IsActive.setToolTipText(tooltip);
                  _rdoFilter_Equipment_Retired_IsActive.addSelectionListener(defaultListener);
               }
               {
                  _rdoFilter_Equipment_Retired_IsRetired = new Button(containerRetired, SWT.RADIO);
                  _rdoFilter_Equipment_Retired_IsRetired.setText("&Retired equipment");
                  _rdoFilter_Equipment_Retired_IsRetired.setToolTipText(tooltip);
                  _rdoFilter_Equipment_Retired_IsRetired.addSelectionListener(defaultListener);
               }
               {
                  _rdoFilter_Equipment_Retired_Ignore = new Button(containerRetired, SWT.RADIO);
                  _rdoFilter_Equipment_Retired_Ignore.setText("&Ignore retirement");
                  _rdoFilter_Equipment_Retired_Ignore.addSelectionListener(defaultListener);
               }
            }
         }
         {
            final Label label = UI.createLabel(container, "Contained tours");
            GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.BEGINNING).applyTo(label);

            final Composite containerTours = new Composite(container, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(containerTours);
            GridLayoutFactory.fillDefaults().numColumns(1).applyTo(containerTours);
            {
               {
                  _rdoFilter_Equipment_ContainsTours_Yes = new Button(containerTours, SWT.RADIO);
                  _rdoFilter_Equipment_ContainsTours_Yes.setText("Equipment &with tours");
                  _rdoFilter_Equipment_ContainsTours_Yes.addSelectionListener(defaultListener);
               }
               {
                  _rdoFilter_Equipment_ContainsTours_No = new Button(containerTours, SWT.RADIO);
                  _rdoFilter_Equipment_ContainsTours_No.setText("Equipment with&out tours");
                  _rdoFilter_Equipment_ContainsTours_No.addSelectionListener(defaultListener);
               }
               {
                  _rdoFilter_Equipment_ContainsTours_Ignore = new Button(containerTours, SWT.RADIO);
                  _rdoFilter_Equipment_ContainsTours_Ignore.setText("Ignore &tours");
                  _rdoFilter_Equipment_ContainsTours_Ignore.addSelectionListener(defaultListener);
               }
            }
         }
      }
   }

   @Override
   protected Rectangle getParentBounds() {

      final Rectangle itemBounds = _toolItem.getBounds();
      final Point itemDisplayPosition = _toolItem.getParent().toDisplay(itemBounds.x, itemBounds.y);

      itemBounds.x = itemDisplayPosition.x;
      itemBounds.y = itemDisplayPosition.y;

      return itemBounds;
   }

   private void initUI(final Composite parent) {

   }

   @Override
   protected void onFocus() {

      _rdoFilter_Equipment_ContainsTours_Ignore.setFocus();
   }

   private void onModified(final SelectionEvent selectionEvent) {

      final Widget widget = selectionEvent.widget;

      if (widget instanceof final Button button) {

         if (button.getSelection() == false) {

            // skip unselection event otherwise there will be 2 events

            return;
         }
      }

// SET_FORMATTING_OFF

      final int containsTours =

            _rdoFilter_Equipment_ContainsTours_Yes.getSelection() ? EquipmentManager.FILTER_CONTAINS_TOURS_YES
          : _rdoFilter_Equipment_ContainsTours_No.getSelection()  ? EquipmentManager.FILTER_CONTAINS_TOURS_NO
                                                                  : EquipmentManager.FILTER_CONTAINS_TOURS_IGNORE;

      final int retired =

            _rdoFilter_Equipment_Retired_IsRetired.getSelection() ? EquipmentManager.FILTER_RETIRED_IS_RETIRED
          : _rdoFilter_Equipment_Retired_IsActive.getSelection()  ? EquipmentManager.FILTER_RETIRED_IS_ACTIVE
                                                                  : EquipmentManager.FILTER_RETIRED_IGNORE;

// SET_FORMATTING_ON

      EquipmentManager.setEquipmentFilter_ContainsTours(containsTours);
      EquipmentManager.setEquipmentFilter_Retired(retired);

      _shellContainer.getDisplay().asyncExec(() -> _equipmentView.updateEquipmentFilter_FromSlideout());
   }

   @Override
   protected Point onResize(final int newContentWidth, final int newContentHeight) {

      if (_shellContainer.isDisposed()) {

         // this happened during debugging

         return null;
      }

      // there is no need to resize this dialog
      final Point defaultSize = _shellContainer.getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT);

      return defaultSize;
   }

   private void restoreState() {

// SET_FORMATTING_OFF

      final int containsTours = EquipmentManager.getEquipmentFilter_ContainsTours();
      final int retired       = EquipmentManager.getEquipmentFilter_Retired();

      _rdoFilter_Equipment_ContainsTours_Ignore .setSelection(containsTours   == EquipmentManager.FILTER_CONTAINS_TOURS_IGNORE);
      _rdoFilter_Equipment_ContainsTours_Yes    .setSelection(containsTours   == EquipmentManager.FILTER_CONTAINS_TOURS_YES);
      _rdoFilter_Equipment_ContainsTours_No     .setSelection(containsTours   == EquipmentManager.FILTER_CONTAINS_TOURS_NO);

      _rdoFilter_Equipment_Retired_Ignore       .setSelection(retired         == EquipmentManager.FILTER_RETIRED_IGNORE);
      _rdoFilter_Equipment_Retired_IsActive     .setSelection(retired         == EquipmentManager.FILTER_RETIRED_IS_ACTIVE);
      _rdoFilter_Equipment_Retired_IsRetired    .setSelection(retired         == EquipmentManager.FILTER_RETIRED_IS_RETIRED);

// SET_FORMATTING_ON
   }

   @Override
   public void saveState() {

      // save slideout position/size
      super.saveState();
   }

}
