/*******************************************************************************
 * Copyright (C) 2025, 2026 Wolfgang Schramm and Contributors
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
package net.tourbook.preferences;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.Util;
import net.tourbook.equipment.EquipmentMenuManager;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Preferences for equipment
 */
public class PrefPageEquipment extends PreferencePage implements IWorkbenchPreferencePage {

   public static final String            ID         = "net.tourbook.preferences.PrefPageEquipment"; //$NON-NLS-1$

   private static final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

   private MouseWheelListener            _defaultMouseWheelListener;
   private SelectionListener             _defaultSelectionListener;

   private PixelConverter                _pc;
   private int                           _hintDefaultSpinnerWidth;

   /*
    * UI controls
    */
   private Button  _chkIsDisplayCollateIdOrName;
   private Button  _chkIsDisplayEquipmentImage;

   private Spinner _spinnerRecentEquipment;

   @Override
   protected Control createContents(final Composite parent) {

      initUI(parent);

      final Composite ui = createUI(parent);

      restoreState();
      enableControls();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//      container.setBackground(UI.SYS_COLOR_BLUE);
      {
         createUI_20_ContextMenu(container);
      }

      return container;
   }

   private void createUI_20_ContextMenu(final Composite parent) {

      final Group group = new Group(parent, SWT.NONE);
      group.setText("Equipment Context Menu");
      GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
      GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);
//      group.setBackground(UI.SYS_COLOR_CYAN);
      {
         {
            /*
             * Display equipment image
             */
            _chkIsDisplayEquipmentImage = new Button(group, SWT.CHECK);
            _chkIsDisplayEquipmentImage.setText("Display equipment &image");
            _chkIsDisplayEquipmentImage.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults()
                  .span(3, 1)
                  .applyTo(_chkIsDisplayEquipmentImage);
         }
         {
            /*
             * Display equipment collate ID or name
             */
            _chkIsDisplayCollateIdOrName = new Button(group, SWT.CHECK);
            _chkIsDisplayCollateIdOrName.setText("Display &collate ID");
            _chkIsDisplayCollateIdOrName.setToolTipText("When a collate ID is not available, then the equipment name is displayed");
            _chkIsDisplayCollateIdOrName.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults()
                  .span(3, 1)
                  .applyTo(_chkIsDisplayCollateIdOrName);
         }
         {
            /*
             * Number of recent equipment
             */
            final String tooltip = Messages.Pref_Equipment_Label_NumberOfRecentEquipment_Tooltip;

            final Label label = UI.createLabel(group, Messages.Pref_Equipment_Label_NumberOfRecentEquipment);
            label.setToolTipText(tooltip);

            // spinner
            _spinnerRecentEquipment = new Spinner(group, SWT.BORDER);
            _spinnerRecentEquipment.setToolTipText(tooltip);
            _spinnerRecentEquipment.setMinimum(0);
            _spinnerRecentEquipment.setMaximum(9);
            _spinnerRecentEquipment.addSelectionListener(_defaultSelectionListener);
            _spinnerRecentEquipment.addMouseWheelListener(_defaultMouseWheelListener);
            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_spinnerRecentEquipment);

            // button: Remove recent equipment
            final Button btnRemoveRecentEquipment = new Button(group, SWT.PUSH);
            btnRemoveRecentEquipment.setText(Messages.Pref_Equipment_Button_RemoveRecentEquipment);
            btnRemoveRecentEquipment.setToolTipText(Messages.Pref_Equipment_Button_RemoveRecentEquipment_Tooltip);
            btnRemoveRecentEquipment.addSelectionListener(SelectionListener.widgetSelectedAdapter(
                  selectionEvent -> EquipmentMenuManager.clearPreviousAndRecentEquipment()));
         }
      }
   }

   private void enableControls() {

   }

   @Override
   public void init(final IWorkbench workbench) {

   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      _hintDefaultSpinnerWidth = _pc.convertWidthInCharsToPixels(3);

      _defaultSelectionListener = SelectionListener.widgetSelectedAdapter(selectionEvent -> onModify());

      _defaultMouseWheelListener = mouseEvent -> {

         Util.adjustSpinnerValueOnMouseScroll(mouseEvent);

         onModify();
      };
   }

   private void onModify() {

   }

   @Override
   protected void performApply() {

      saveState();

      super.performApply();
   }

   @Override
   public boolean performCancel() {

      return super.performCancel();
   }

   @Override
   protected void performDefaults() {

      _chkIsDisplayCollateIdOrName.setSelection(_prefStore.getDefaultBoolean(ITourbookPreferences.EQUIPMENT_IS_DISPLAY_COLLATE_ID_IN_CONTEXT_MENU));
      _chkIsDisplayEquipmentImage.setSelection(_prefStore.getDefaultBoolean(ITourbookPreferences.EQUIPMENT_IS_DISPLAY_IMAGE_IN_CONTEXT_MENU));
      _spinnerRecentEquipment.setSelection(_prefStore.getDefaultInt(ITourbookPreferences.EQUIPMENT_NUMBER_OF_RECENT_EQUIPMENT));

      super.performDefaults();

      enableControls();
   }

   @Override
   public boolean performOk() {

      saveState();

      return true;
   }

   private void restoreState() {

      _chkIsDisplayCollateIdOrName.setSelection(_prefStore.getBoolean(ITourbookPreferences.EQUIPMENT_IS_DISPLAY_COLLATE_ID_IN_CONTEXT_MENU));
      _chkIsDisplayEquipmentImage.setSelection(_prefStore.getBoolean(ITourbookPreferences.EQUIPMENT_IS_DISPLAY_IMAGE_IN_CONTEXT_MENU));
      _spinnerRecentEquipment.setSelection(_prefStore.getInt(ITourbookPreferences.EQUIPMENT_NUMBER_OF_RECENT_EQUIPMENT));
   }

   private void saveState() {

      _prefStore.setValue(ITourbookPreferences.EQUIPMENT_IS_DISPLAY_COLLATE_ID_IN_CONTEXT_MENU, _chkIsDisplayCollateIdOrName.getSelection());
      _prefStore.setValue(ITourbookPreferences.EQUIPMENT_IS_DISPLAY_IMAGE_IN_CONTEXT_MENU, _chkIsDisplayEquipmentImage.getSelection());

      // set as LAST which will trigger the listeners
      _prefStore.setValue(ITourbookPreferences.EQUIPMENT_NUMBER_OF_RECENT_EQUIPMENT, _spinnerRecentEquipment.getSelection());
   }
}
