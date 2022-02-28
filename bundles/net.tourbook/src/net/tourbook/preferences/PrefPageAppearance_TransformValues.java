/*******************************************************************************
 * Copyright (C) 2022 Wolfgang Schramm and Contributors
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
import net.tourbook.common.CommonActivator;
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.common.util.Util;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageAppearance_TransformValues extends PreferencePage implements IWorkbenchPreferencePage {

   public static final String      ID                = "net.tourbook.preferences.PrefPageAppearance_TransformValues"; //$NON-NLS-1$

   private static IPreferenceStore _prefStore_Common = CommonActivator.getPrefStore();

//   private SelectionListener _defaultSelectionListener;

   /*
    * UI controls
    */
   private Spinner _spinnerTransformOpacity;

   public PrefPageAppearance_TransformValues() {
//		noDefaultAndApplyButton();
   }

   @Override
   protected Control createContents(final Composite parent) {

      initUI(parent);

      final Composite container = createUI(parent);

      restoreState();

      return container;
   }

   private Composite createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults()
            .spacing(5, 15)
            .numColumns(2)
            .applyTo(container);
      {
         {
            /*
             * Opacity value range
             */
            {
               // label
               final Label label = new Label(container, SWT.NONE);
               label.setText(Messages.Pref_TransformValues_Label_Opacity);
               label.setToolTipText(Messages.Pref_TransformValues_Label_Opacity_Tooltip);
               GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);
            }
            {
               // spinner
               _spinnerTransformOpacity = new Spinner(container, SWT.BORDER);
               _spinnerTransformOpacity.setPageIncrement(10);
               _spinnerTransformOpacity.setMinimum(1);
               _spinnerTransformOpacity.setMaximum(100);
               _spinnerTransformOpacity.addMouseWheelListener(mouseEvent -> {
                  Util.adjustSpinnerValueOnMouseScroll(mouseEvent);
                  onChangeUI();
               });
               GridDataFactory.fillDefaults()
                     .align(SWT.BEGINNING, SWT.FILL)
                     .applyTo(_spinnerTransformOpacity);
            }
         }
      }

      return container;
   }

   @Override
   public void init(final IWorkbench workbench) {}

   private void initUI(final Composite parent) {

//      _defaultSelectionListener = SelectionListener.widgetSelectedAdapter(selectionEvent -> onChangeUI());
   }

   @Override
   public boolean okToLeave() {

      return super.okToLeave();
   }

   private void onChangeUI() {
      saveState();
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

// SET_FORMATTING_OFF

      final int defaultInt = _prefStore_Common.getDefaultInt(ICommonPreferences.TRANSFORM_VALUE_OPACITY_MAX);

      _spinnerTransformOpacity.setSelection(defaultInt);

// SET_FORMATTING_ON

      super.performDefaults();
   }

   @Override
   public boolean performOk() {

      saveState();

      return super.performOk();
   }

   private void restoreState() {

// SET_FORMATTING_OFF

      _spinnerTransformOpacity.setSelection(_prefStore_Common.getInt(ICommonPreferences.TRANSFORM_VALUE_OPACITY_MAX));

// SET_FORMATTING_ON

   }

   private void saveState() {

// SET_FORMATTING_OFF

      _prefStore_Common.setValue(ICommonPreferences.TRANSFORM_VALUE_OPACITY_MAX, _spinnerTransformOpacity.getSelection());

// SET_FORMATTING_ON

   }

}
