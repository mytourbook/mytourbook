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

import java.util.List;

import net.tourbook.Messages;
import net.tourbook.OtherMessages;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.action.ActionResetToDefaults;
import net.tourbook.common.action.IActionResetToDefault;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.common.util.Util;
import net.tourbook.equipment.EquipmentConfigManager.SortFieldUI;
import net.tourbook.preferences.PrefPageEquipment;
import net.tourbook.ui.views.tourDataEditor.TourDataEditorView;

import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolItem;

/**
 * Slidout for equipment view options
 */
public class SlideoutEquipmentOptions extends AdvancedSlideout implements IActionResetToDefault {

   private IDialogSettings       _state;

   private EquipmentView         _equipmentView;

   private SelectionAdapter      _defaultSelectionListener;
   private MouseWheelListener    _defaultMouseWheelListener;
   private FocusListener         _keepOpenListener;

   private ActionResetToDefaults _actionRestoreDefaults;
   private ActionOpenPrefDialog  _actionPrefDialog;

   private boolean               _isUpdateUI;

   private ToolItem              _toolItem;

   private PixelConverter        _pc;

   /*
    * UI controls
    */
   private Composite _shellContainer;

   private Button    _btnReset;
   private Button    _rdoShowCustomHeight;
   private Button    _rdoShowDefaultHeight;

   private Combo     _comboConfigName;
   private Combo     _comboEquipmentSort1;
   private Combo     _comboEquipmentSort2;
   private Combo     _comboEquipmentSort3;
   private Combo     _comboPartServiceSort1;
   private Combo     _comboPartServiceSort2;
   private Combo     _comboPartServiceSort3;

   private Label     _lblConfigName;

   private Spinner   _spinnerViewerImageHeight;

   private Text      _textConfigName;

   public SlideoutEquipmentOptions(final ToolItem toolItem,
                                   final EquipmentView equipmentView,
                                   final IDialogSettings state) {

      super(toolItem.getParent(), state, null);

      _toolItem = toolItem;
      _equipmentView = equipmentView;
      _state = state;

      setTitleText(Messages.Slideout_EquipmentOptions_Title);
   }

   private void createActions() {

      _actionRestoreDefaults = new ActionResetToDefaults(this);

      _actionPrefDialog = new ActionOpenPrefDialog(Messages.Slideout_EquipmentOptions_Action_EquipmentPreferences, PrefPageEquipment.ID);
      _actionPrefDialog.closeThisTooltip(this);
      _actionPrefDialog.setShell(_equipmentView.getTreeViewer().getTree().getShell());
   }

   @Override
   protected void createSlideoutContent(final Composite parent) {

      initUI(parent);

      createActions();

      createUI(parent);

      fillUI();
      fillUI_Config();

      restoreState();

      enableControls();
   }

   private Composite createUI(final Composite parent) {

      _shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().applyTo(_shellContainer);
      {
         final Composite container = new Composite(_shellContainer, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         GridLayoutFactory.fillDefaults().applyTo(container);
//         container.setBackground(UI.SYS_COLOR_GREEN);
         {
            createUI_000_ConfigHeader(container);
            createUI_100_Sorting(container);
            createUI_500_Options(container);
            createUI_900_ConfigName(container);
         }
      }

      return _shellContainer;
   }

   private void createUI_000_ConfigHeader(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .indent(0, 5)
            .applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
//      container.setBackground(UI.SYS_COLOR_CYAN);
      {
         {
            /*
             * Label: Title
             */
            final Label title = new Label(container, SWT.LEAD);
            title.setText("Configuration");
            GridDataFactory.fillDefaults()
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(title);
         }
         {
            /*
             * Combo: Configuration
             */
            _comboConfigName = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
            _comboConfigName.setVisibleItemCount(20);
            _comboConfigName.addFocusListener(_keepOpenListener);
            _comboConfigName.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onSelectConfig()));
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .align(SWT.FILL, SWT.CENTER)
                  .hint(_pc.convertWidthInCharsToPixels(25), SWT.DEFAULT)

                  // do micro adjustment, on win it is now centered
                  .indent(0, 1)

                  .applyTo(_comboConfigName);
         }
         {
            /*
             * Button: Reset
             */
            _btnReset = new Button(container, SWT.PUSH);
            _btnReset.setText(OtherMessages.TOUR_TRACK_PROPERTIES_BUTTON_DEFAULT);
            _btnReset.setToolTipText(OtherMessages.TOUR_TRACK_PROPERTIES_BUTTON_DEFAULT_TOOLTIP);
            _btnReset.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onSelectConfig_Default(selectionEvent)));
            GridDataFactory.fillDefaults()
                  .align(SWT.END, SWT.CENTER)
                  .applyTo(_btnReset);
         }
      }
   }

   private void createUI_100_Sorting(final Composite parent) {

      final GridDataFactory gd = GridDataFactory.fillDefaults().grab(true, false);

      final Group group = new Group(parent, SWT.NONE);
      group.setText("Sorting");
      gd.applyTo(group);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
//      group.setBackground(UI.SYS_COLOR_YELLOW);
      {
         {
            UI.createLabel(group, "Equipment &1st field");

            _comboEquipmentSort1 = new Combo(group, SWT.READ_ONLY | SWT.BORDER);
            _comboEquipmentSort1.addFocusListener(_keepOpenListener);
            _comboEquipmentSort1.addSelectionListener(_defaultSelectionListener);

            gd.applyTo(_comboEquipmentSort1);
         }
         {
            UI.createLabel(group, "Equipment &2nd field");

            _comboEquipmentSort2 = new Combo(group, SWT.READ_ONLY | SWT.BORDER);
            _comboEquipmentSort2.addFocusListener(_keepOpenListener);
            _comboEquipmentSort2.addSelectionListener(_defaultSelectionListener);

            gd.applyTo(_comboEquipmentSort2);
         }
         {
            UI.createLabel(group, "Equipment &3rd field");

            _comboEquipmentSort3 = new Combo(group, SWT.READ_ONLY | SWT.BORDER);
            _comboEquipmentSort3.addFocusListener(_keepOpenListener);
            _comboEquipmentSort3.addSelectionListener(_defaultSelectionListener);

            gd.applyTo(_comboEquipmentSort3);
         }

         {
            UI.createLabel(group, "Part/Service 1s&t field");

            _comboPartServiceSort1 = new Combo(group, SWT.READ_ONLY | SWT.BORDER);
            _comboPartServiceSort1.addFocusListener(_keepOpenListener);
            _comboPartServiceSort1.addSelectionListener(_defaultSelectionListener);

            gd.applyTo(_comboPartServiceSort1);
         }
         {
            UI.createLabel(group, "Part/Service 2&nd field");

            _comboPartServiceSort2 = new Combo(group, SWT.READ_ONLY | SWT.BORDER);
            _comboPartServiceSort2.addFocusListener(_keepOpenListener);
            _comboPartServiceSort2.addSelectionListener(_defaultSelectionListener);

            gd.applyTo(_comboPartServiceSort2);
         }
         {
            UI.createLabel(group, "Part/Service 3&rd field");

            _comboPartServiceSort3 = new Combo(group, SWT.READ_ONLY | SWT.BORDER);
            _comboPartServiceSort3.addFocusListener(_keepOpenListener);
            _comboPartServiceSort3.addSelectionListener(_defaultSelectionListener);

            gd.applyTo(_comboPartServiceSort3);
         }
      }
   }

   private void createUI_500_Options(final Composite parent) {

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

   private void createUI_900_ConfigName(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         {
            /*
             * Config name
             */

            // Label
            _lblConfigName = new Label(container, SWT.NONE);
            _lblConfigName.setText("&Name");
            _lblConfigName.setToolTipText("Name for the currently selected configuration");
            UI.gridLayoutData_AlignFillCenter().applyTo(_lblConfigName);

            // Text
            _textConfigName = new Text(container, SWT.BORDER);
            _textConfigName.addModifyListener(modifyEvent -> onModifyName());
            UI.gridLayoutData_AlignFillCenter().grab(true, false).applyTo(_textConfigName);
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

   private void fillUI() {

      for (final SortFieldUI sortFieldUI : EquipmentConfigManager.EQUIPMENT_SORT_FIELDS) {
         _comboEquipmentSort1.add(sortFieldUI.label);
      }

      for (final SortFieldUI sortFieldUI : EquipmentConfigManager.EQUIPMENT_SORT_FIELDS) {
         _comboEquipmentSort2.add(sortFieldUI.label);
      }

      for (final SortFieldUI sortFieldUI : EquipmentConfigManager.EQUIPMENT_SORT_FIELDS) {
         _comboEquipmentSort3.add(sortFieldUI.label);
      }

      for (final SortFieldUI sortFieldUI : EquipmentConfigManager.PART_SORT_FIELDS) {
         _comboPartServiceSort1.add(sortFieldUI.label);
      }

      for (final SortFieldUI sortFieldUI : EquipmentConfigManager.PART_SORT_FIELDS) {
         _comboPartServiceSort2.add(sortFieldUI.label);
      }

      for (final SortFieldUI sortFieldUI : EquipmentConfigManager.PART_SORT_FIELDS) {
         _comboPartServiceSort3.add(sortFieldUI.label);
      }
   }

   private void fillUI_Config() {

      final boolean backupIsUpdateUI = _isUpdateUI;
      _isUpdateUI = true;
      {
         _comboConfigName.removeAll();

         for (final EquipmentViewConfig config : EquipmentConfigManager.getAllConfigs()) {
            _comboConfigName.add(config.name);
         }
      }
      _isUpdateUI = backupIsUpdateUI;
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

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

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

      _keepOpenListener = new FocusListener() {

         @Override
         public void focusGained(final FocusEvent e) {

            /*
             * This will fix the problem that when the list of a combobox is displayed, then the
             * slideout will disappear :-(((
             */
            setIsAnotherDialogOpened(true);
         }

         @Override
         public void focusLost(final FocusEvent e) {
            setIsAnotherDialogOpened(false);
         }
      };
   }

   private void onChangeUI() {

      saveState();

      enableControls();

      updateUI();
   }

   @Override
   protected void onFocus() {

//      _rdoFilter_Equipment_ContainsTours_Ignore.setFocus();
   }

   private void onModifyName() {

      if (_isUpdateUI) {
         return;
      }

      // update text in the combo
      final int selectedIndex = _comboConfigName.getSelectionIndex();

      _comboConfigName.setItem(selectedIndex, _textConfigName.getText());
      _comboConfigName.select(selectedIndex);

      saveState();
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

   private void onSelectConfig() {

      final int selectedIndex = _comboConfigName.getSelectionIndex();
      final List<EquipmentViewConfig> allConfigurations = EquipmentConfigManager.getAllConfigs();

      final EquipmentViewConfig selectedConfig = allConfigurations.get(selectedIndex);
      final EquipmentViewConfig activeConfig = EquipmentConfigManager.getActiveConfig();

      if (selectedConfig.equals(activeConfig)) {

         // config has not changed
         return;
      }

      // keep data from previous config
      saveState();

      EquipmentConfigManager.setActiveConfig(selectedConfig);

      restoreState();
      enableControls();

      updateUI();
   }

   private void onSelectConfig_Default(final SelectionEvent selectionEvent) {

      if (Util.isCtrlKeyPressed(selectionEvent)) {

         // reset All configurations

         EquipmentConfigManager.resetAllConfigurations();

         fillUI_Config();

      } else {

         // reset active config

         EquipmentConfigManager.resetActiveConfiguration();
      }

      restoreState();
      enableControls();

      updateUI();
   }

   @Override
   public void resetToDefaults() {

      _rdoShowDefaultHeight.setSelection(true);
      _rdoShowCustomHeight.setSelection(false);

      onChangeUI();
   }

   private void restoreState() {

      // get active config AFTER getting the index because this could change the active config
      final EquipmentViewConfig config = EquipmentConfigManager.getActiveConfig();
      final int activeConfigIndex = EquipmentConfigManager.getActiveConfigIndex();

      final int defaultItemHeight = getDefaultItemHeight();

      final int itemHeight = Util.getStateInt(_state,
            TourDataEditorView.STATE_EQUIPMENT_VIEWER_IMAGE_HEIGHT,
            defaultItemHeight,
            defaultItemHeight,
            TourDataEditorView.STATE_CONTENT_IMAGE_SIZE_MAX);

      final boolean isUseDefaultHeight = Util.getStateBoolean(_state, TourDataEditorView.STATE_EQUIPMENT_IS_USE_VIEWER_DEFAULT_HEIGHT, true);

// SET_FORMATTING_OFF

      _isUpdateUI = true;
      {
         _comboConfigName           .select(activeConfigIndex);
         _textConfigName            .setText(config.name);

         _rdoShowDefaultHeight      .setSelection(isUseDefaultHeight);
         _rdoShowCustomHeight       .setSelection(isUseDefaultHeight == false);
         _spinnerViewerImageHeight  .setSelection(itemHeight);

      }
      _isUpdateUI = false;

// SET_FORMATTING_ON
   }

   @Override
   protected void saveState() {

      // update config

      final EquipmentViewConfig config = EquipmentConfigManager.getActiveConfig();

      config.name = _textConfigName.getText();

// SET_FORMATTING_OFF

      _state.put(TourDataEditorView.STATE_EQUIPMENT_IS_USE_VIEWER_DEFAULT_HEIGHT,   _rdoShowDefaultHeight      .getSelection());
      _state.put(TourDataEditorView.STATE_EQUIPMENT_VIEWER_IMAGE_HEIGHT,            _spinnerViewerImageHeight  .getSelection());

// SET_FORMATTING_ON

      // save slideout position/size
      super.saveState();
   }

   private void updateUI() {

      // run async to update the slideout immediately
      _shellContainer.getDisplay().asyncExec(() -> _equipmentView.updateUI_Viewer());
   }

}
