/*******************************************************************************
 * Copyright (C) 2024 Frédéric Bard
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

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageNutrition extends PreferencePage implements IWorkbenchPreferencePage {

   public static final String     ID         = "net.tourbook.preferences.PrefPageNutrition"; //$NON-NLS-1$

   private final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

   /*
    * UI controls
    */
   private Button _chkIgnoreFirstHour;

   @Override
   protected Control createContents(final Composite parent) {

      final Composite container = createUI(parent);

      restoreState();

      return container;
   }

   private Composite createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults()
            .spacing(5, 15)
            .applyTo(container);
      {
         /*
          * Ignore the 1st hour for nutrition averages computation
          */
         {
            _chkIgnoreFirstHour = new Button(container, SWT.CHECK);
            _chkIgnoreFirstHour.setText(Messages.PrefPage_Nutrition_Checkbox_IgnoreFirstHour);
            _chkIgnoreFirstHour.setToolTipText(Messages.PrefPage_Nutrition_Checkbox_IgnoreFirstHour_Tooltip);
            GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL).applyTo(_chkIgnoreFirstHour);
         }
      }

      return container;
   }

   @Override
   public void init(final IWorkbench workbench) {
      // Nothing to do
   }

   @Override
   protected void performDefaults() {

      _chkIgnoreFirstHour.setSelection(_prefStore.getDefaultBoolean(ITourbookPreferences.NUTRITION_IGNORE_FIRST_HOUR));

      super.performDefaults();
   }

   @Override
   public boolean performOk() {

      final boolean isOK = super.performOk();

      if (isOK) {

         _prefStore.setValue(ITourbookPreferences.NUTRITION_IGNORE_FIRST_HOUR, _chkIgnoreFirstHour.getSelection());
      }

      return isOK;
   }

   private void restoreState() {

      _chkIgnoreFirstHour.setSelection(_prefStore.getBoolean(ITourbookPreferences.NUTRITION_IGNORE_FIRST_HOUR));
   }

}
