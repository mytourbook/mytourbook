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

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.equipment.EquipmentMenuManager;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
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

   /*
    * UI controls
    */
   private Spinner _spinnerRecentEquipment;

   @Override
   protected Control createContents(final Composite parent) {

      final Composite ui = createUI(parent);

      restoreState();
      enableControls();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
      {
         {
            /*
             * Number of recent equipment
             */
            final String tooltip = "Number of recently used equipment which are displayed in the context menu.\n\n0 will hide recently used tags.";

            final Label label = UI.createLabel(container, "Number of recent &equipment");
            label.setToolTipText(tooltip);

            // spinner
            _spinnerRecentEquipment = new Spinner(container, SWT.BORDER);
            _spinnerRecentEquipment.setToolTipText(tooltip);
            _spinnerRecentEquipment.setMinimum(0);
            _spinnerRecentEquipment.setMaximum(9);
//            _spinnerRecentEquipment.addSelectionListener(_defaultSelectionListener);
//            _spinnerRecentEquipment.addMouseWheelListener(_defaultMouseWheelListener);
            GridDataFactory.fillDefaults()
//                  .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_spinnerRecentEquipment);

            // button: Remove recent equipment
            final Button btnRemoveRecentTags = new Button(container, SWT.PUSH);
            btnRemoveRecentTags.setText("Remo&ve Recent Equipment");
            btnRemoveRecentTags.setToolTipText("All recent equipment will be removed from the recent equipment list");
            btnRemoveRecentTags.addSelectionListener(SelectionListener.widgetSelectedAdapter(
                  selectionEvent -> EquipmentMenuManager.clearRecentEquipment()));
         }
      }

      return container;
   }

   private void enableControls() {

   }

   @Override
   public void init(final IWorkbench workbench) {

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

      _spinnerRecentEquipment.setSelection(_prefStore.getInt(ITourbookPreferences.EQUIPMENT_NUMBER_OF_RECENT_EQUIPMENT));
   }

   private void saveState() {

      _prefStore.setValue(ITourbookPreferences.EQUIPMENT_NUMBER_OF_RECENT_EQUIPMENT, _spinnerRecentEquipment.getSelection());
   }
}
