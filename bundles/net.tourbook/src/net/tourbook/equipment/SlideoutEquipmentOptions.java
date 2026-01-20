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

import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.action.ActionResetToDefaults;
import net.tourbook.common.action.IActionResetToDefault;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.Util;
import net.tourbook.preferences.PrefPageEquipment;
import net.tourbook.ui.views.tourDataEditor.TourDataEditorView;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;

public class SlideoutEquipmentOptions extends ToolbarSlideout implements IActionResetToDefault {

   private IDialogSettings       _state;

   private EquipmentView         _equipmentView;

   private SelectionAdapter      _defaultSelectionListener;
   private MouseWheelListener    _defaultMouseWheelListener;

   private ActionResetToDefaults _actionRestoreDefaults;
   private ActionOpenPrefDialog  _actionPrefDialog;

   /*
    * UI controls
    */
   private Spinner _spinnerViewerImageHeight;

   private Button  _rdoShowCustomHeight;
   private Button  _rdoShowDefaultHeight;

   /**
    * @param ownerControl
    * @param toolBar
    * @param equipmentView
    * @param state
    */
   public SlideoutEquipmentOptions(final Control ownerControl,
                                   final ToolBar toolBar,
                                   final EquipmentView equipmentView,
                                   final IDialogSettings state) {

      super(ownerControl, toolBar);

      _equipmentView = equipmentView;
      _state = state;
   }

   private void createActions() {

      _actionRestoreDefaults = new ActionResetToDefaults(this);

      _actionPrefDialog = new ActionOpenPrefDialog("Equipment preferences...", PrefPageEquipment.ID);
      _actionPrefDialog.closeThisTooltip(this);
      _actionPrefDialog.setShell(_equipmentView.getTreeViewer().getTree().getShell());
   }

   @Override
   protected Composite createToolTipContentArea(final Composite parent) {

      initUI();

      createActions();

      final Composite ui = createUI(parent);

      restoreState();
      enableControls();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.swtDefaults().applyTo(shellContainer);
      {
         final Composite container = new Composite(shellContainer, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         GridLayoutFactory.fillDefaults().applyTo(container);
//       container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
         {
            final Composite titleContainer = new Composite(container, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(titleContainer);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(titleContainer);
            {
               createUI_10_Title(titleContainer);
               createUI_12_Actions(titleContainer);
            }

            createUI_20_Options(container);
         }
      }

      return shellContainer;
   }

   private void createUI_10_Title(final Composite parent) {

      /*
       * Label: Slideout title
       */
      final Label label = new Label(parent, SWT.NONE);
      GridDataFactory.fillDefaults().applyTo(label);
      label.setText("Equipment Options");
      label.setFont(JFaceResources.getBannerFont());

      MTFont.setBannerFont(label);
   }

   private void createUI_12_Actions(final Composite parent) {

      final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .align(SWT.END, SWT.BEGINNING)
            .applyTo(toolbar);

      final ToolBarManager tbm = new ToolBarManager(toolbar);

      tbm.add(_actionRestoreDefaults);
      tbm.add(_actionPrefDialog);

      tbm.update(true);
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
            label.setText("Row height");
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
               _rdoShowDefaultHeight.setText("Use &default height");
               _rdoShowDefaultHeight.addSelectionListener(_defaultSelectionListener);

               GridDataFactory.fillDefaults().span(2, 1).applyTo(_rdoShowDefaultHeight);
            }
            {

               /*
                * Show custom height
                */
               _rdoShowCustomHeight = new Button(heightContainer, SWT.RADIO);
               _rdoShowCustomHeight.setText("&Customize height");
               _rdoShowCustomHeight.addSelectionListener(_defaultSelectionListener);

               /*
                * Image height
                */
               _spinnerViewerImageHeight = new Spinner(heightContainer, SWT.BORDER);
               _spinnerViewerImageHeight.setMinimum(getDefaultItemHeight());
               _spinnerViewerImageHeight.setMaximum(TourDataEditorView.STATE_EQUIPMENT_IMAGE_SIZE_MAX);
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

   /**
    * This looks complicated but the slideout is created twice, so we retrieve the current value
    *
    * @return
    */
   private int getDefaultItemHeight() {

      return _equipmentView.getDefaultItemHeight();
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

      _equipmentView.updateUI_Viewer();
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
            TourDataEditorView.STATE_EQUIPMENT_IMAGE_SIZE_MAX);

      final boolean isUseDefaultHeight = Util.getStateBoolean(_state, TourDataEditorView.STATE_EQUIPMENT_IS_USE_VIEWER_DEFAULT_HEIGHT, true);

// SET_FORMATTING_OFF

      _rdoShowDefaultHeight      .setSelection(isUseDefaultHeight);
      _rdoShowCustomHeight       .setSelection(isUseDefaultHeight == false);
      _spinnerViewerImageHeight  .setSelection(itemHeight);

// SET_FORMATTING_ON
   }

   private void saveState() {

// SET_FORMATTING_OFF

      _state.put(TourDataEditorView.STATE_EQUIPMENT_IS_USE_VIEWER_DEFAULT_HEIGHT,   _rdoShowDefaultHeight      .getSelection());
      _state.put(TourDataEditorView.STATE_EQUIPMENT_VIEWER_IMAGE_HEIGHT,            _spinnerViewerImageHeight  .getSelection());

// SET_FORMATTING_ON
   }

}
