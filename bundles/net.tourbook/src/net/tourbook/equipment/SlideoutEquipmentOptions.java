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

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.action.ActionResetToDefaults;
import net.tourbook.common.action.IActionResetToDefault;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.common.util.Util;
import net.tourbook.preferences.PrefPageEquipment;
import net.tourbook.ui.views.tourDataEditor.TourDataEditorView;

import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolItem;

/**
 * Slidout for equipment view options
 */
public class SlideoutEquipmentOptions extends AdvancedSlideout implements IActionResetToDefault {

   private EquipmentView         _equipmentView;

   private SelectionAdapter      _defaultSelectionListener;
   private MouseWheelListener    _defaultMouseWheelListener;

   private ActionResetToDefaults _actionRestoreDefaults;
   private ActionOpenPrefDialog  _actionPrefDialog;

   private ToolItem              _toolItem;

   /*
    * UI controls
    */
   private Composite       _shellContainer;

   private Button          _rdoShowCustomHeight;
   private Button          _rdoShowDefaultHeight;

   private Spinner         _spinnerViewerImageHeight;

   private IDialogSettings _state;

   public SlideoutEquipmentOptions(final ToolItem toolItem,
                                   final EquipmentView equipmentView,
                                   final IDialogSettings state) {

      super(toolItem.getParent(), state, null);

      _toolItem = toolItem;
      _equipmentView = equipmentView;
      _state = state;

      setTitleText(Messages.Slideout_EquipmentOptions_Title);

      // prevent that the opened slideout is partly hidden
      setIsForceBoundsToBeInsideOfViewport(true);
   }

   private void createActions() {

      _actionRestoreDefaults = new ActionResetToDefaults(this);

      _actionPrefDialog = new ActionOpenPrefDialog(Messages.Slideout_EquipmentOptions_Action_EquipmentPreferences, PrefPageEquipment.ID);
      _actionPrefDialog.closeThisTooltip(this);
      _actionPrefDialog.setShell(_equipmentView.getTreeViewer().getTree().getShell());
   }

   @Override
   protected void createSlideoutContent(final Composite parent) {

      initUI();

      createActions();

      createUI(parent);

      restoreState();

      enableControls();
   }

   private Composite createUI(final Composite parent) {

      _shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.swtDefaults().applyTo(_shellContainer);
      {
         final Composite container = new Composite(_shellContainer, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         GridLayoutFactory.fillDefaults().applyTo(container);
         container.setBackground(UI.SYS_COLOR_GREEN);
         {
            createUI_20_Options(container);
         }
      }

      return _shellContainer;
   }

   private void createUI_20_Options(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//      container.setBackground(UI.SYS_COLOR_YELLOW);
      {
         {
            // Label
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Slideout_EquipmentOptions_Label_RowHeight);
            GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(label);

         }
         {
            final Composite heightContainer = new Composite(container, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(heightContainer);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(heightContainer);
            {
               /*
                * Show default height
                */
               _rdoShowDefaultHeight = new Button(heightContainer, SWT.RADIO);
               _rdoShowDefaultHeight.setText(Messages.Slideout_EquipmentOptions_Radio_DefaultHeight);
               _rdoShowDefaultHeight.addSelectionListener(_defaultSelectionListener);

               GridDataFactory.fillDefaults().span(2, 1).applyTo(_rdoShowDefaultHeight);
            }
            {

               /*
                * Show custom height
                */
               _rdoShowCustomHeight = new Button(heightContainer, SWT.RADIO);
               _rdoShowCustomHeight.setText(Messages.Slideout_EquipmentOptions_Radio_CustomHeight);
               _rdoShowCustomHeight.setToolTipText(Messages.Slideout_EquipmentOptions_Radio_CustomHeight_Tooltip);
               _rdoShowCustomHeight.addSelectionListener(_defaultSelectionListener);

               /*
                * Image height
                */
               _spinnerViewerImageHeight = new Spinner(heightContainer, SWT.BORDER);
               _spinnerViewerImageHeight.setMinimum(getDefaultItemHeight());
               _spinnerViewerImageHeight.setMaximum(TourDataEditorView.STATE_CONTENT_IMAGE_SIZE_MAX);
               _spinnerViewerImageHeight.setPageIncrement(10);
               _spinnerViewerImageHeight.addSelectionListener(_defaultSelectionListener);
               _spinnerViewerImageHeight.addMouseWheelListener(_defaultMouseWheelListener);
            }
         }
      }
   }

   private void enableControls() {

      final boolean isUseCustomHeight = _rdoShowCustomHeight.getSelection();

      _spinnerViewerImageHeight.setEnabled(isUseCustomHeight);
   }

   @Override
   protected void fillHeaderToolbar(final ToolBarManager toolbarManager) {

      toolbarManager.add(_actionRestoreDefaults);
      toolbarManager.add(_actionPrefDialog);

      toolbarManager.add(new Separator());
   }

   /**
    * This looks complicated but the slideout is created twice, so we retrieve the current value
    *
    * @return
    */
   private int getDefaultItemHeight() {

      return _equipmentView.getDefaultItemHeight();
   }

   @Override
   protected Rectangle getParentBounds() {

      final Rectangle itemBounds = _toolItem.getBounds();
      final Point itemDisplayPosition = _toolItem.getParent().toDisplay(itemBounds.x, itemBounds.y);

      itemBounds.x = itemDisplayPosition.x;
      itemBounds.y = itemDisplayPosition.y;

      return itemBounds;
   }

   private void initUI() {

      _defaultSelectionListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            onChangeUI();
         }
      };

      _defaultMouseWheelListener = mouseEvent -> {

         UI.adjustSpinnerValueOnMouseScroll(mouseEvent, 10);
         onChangeUI();
      };
   }

   private void onChangeUI() {

      saveState();

      enableControls();

      // run async to update the slideout immediately
      _shellContainer.getDisplay().asyncExec(() -> _equipmentView.updateUI_Viewer());
   }

   @Override
   protected void onFocus() {

//      _rdoFilter_Equipment_ContainsTours_Ignore.setFocus();
   }

   @Override
   protected Point onResize(final int newContentWidth, final int newContentHeight) {

      if (_shellContainer.isDisposed()) {

         // this happened during debugging

         return null;
      }

      // prevent the dialog resize, there is no need
      final Point defaultSize = _shellContainer.getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT);

      return defaultSize;
   }

   @Override
   public void resetToDefaults() {

      _rdoShowDefaultHeight.setSelection(true);
      _rdoShowCustomHeight.setSelection(false);

      onChangeUI();
   }

   private void restoreState() {

      final int defaultItemHeight = getDefaultItemHeight();

      final int itemHeight = Util.getStateInt(_state,
            TourDataEditorView.STATE_EQUIPMENT_VIEWER_IMAGE_HEIGHT,
            defaultItemHeight,
            defaultItemHeight,
            TourDataEditorView.STATE_CONTENT_IMAGE_SIZE_MAX);

      final boolean isUseDefaultHeight = Util.getStateBoolean(_state, TourDataEditorView.STATE_EQUIPMENT_IS_USE_VIEWER_DEFAULT_HEIGHT, true);

// SET_FORMATTING_OFF

      _rdoShowDefaultHeight      .setSelection(isUseDefaultHeight);
      _rdoShowCustomHeight       .setSelection(isUseDefaultHeight == false);
      _spinnerViewerImageHeight  .setSelection(itemHeight);

// SET_FORMATTING_ON
   }

   @Override
   protected void saveState() {

// SET_FORMATTING_OFF

      _state.put(TourDataEditorView.STATE_EQUIPMENT_IS_USE_VIEWER_DEFAULT_HEIGHT,   _rdoShowDefaultHeight      .getSelection());
      _state.put(TourDataEditorView.STATE_EQUIPMENT_VIEWER_IMAGE_HEIGHT,            _spinnerViewerImageHeight  .getSelection());

// SET_FORMATTING_ON

      // save slideout position/size
      super.saveState();
   }

}
