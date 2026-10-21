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
package net.tourbook.preferences;

import net.tourbook.Messages;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.UI;
import net.tourbook.common.util.Util;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageGeneral_System extends PreferencePage implements IWorkbenchPreferencePage {

   public static final String           ID                                     = "net.tourbook.preferences.PrefPageGeneral_System"; //$NON-NLS-1$

   public static final String           STATE_IS_SET_SYSTEM_PROPERTIES         = "STATE_IS_SET_SYSTEM_PROPERTIES";                  //$NON-NLS-1$
   public static final boolean          STATE_IS_SET_SYSTEM_PROPERTIES_DEFAULT = false;
   public static final String           STATE_SYSTEM_PROPERTIES                = "STATE_SYSTEM_PROPERTIES";                         //$NON-NLS-1$

   private static final IDialogSettings _state                                 = CommonActivator.getState(ID);

   /*
    * UI controls
    */
   private Button _chkSetProperties;
   private Text   _txtProperties;

   public PrefPageGeneral_System() {

      // these buttons are not needed
//      noDefaultAndApplyButton();
   }

   public static IDialogSettings getState() {
      return _state;
   }

   @Override
   protected Control createContents(final Composite parent) {

      initUI(parent);

      final Composite container = createUI(parent);

      restoreState();

      enableControls();

      return container;
   }

   private Composite createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults()
//            .spacing(5, 15)
//            .numColumns(2)
            .applyTo(container);
      {
         createUI_SystemProperties(container);
      }

      return container;
   }

   private void createUI_SystemProperties(final Composite parent) {

      _chkSetProperties = new Button(parent, SWT.CHECK);
      _chkSetProperties.setText(Messages.Pref_System_Checkbox_SetSystemProperties);
      _chkSetProperties.setToolTipText(
            Messages.Pref_System_Checkbox_SetSystemProperties_Tooltip);
      _chkSetProperties.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> enableControls()));

      _txtProperties = new Text(parent, SWT.BORDER | SWT.WRAP | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
      _txtProperties.setFont(net.tourbook.ui.UI.getLogFont());

      GridDataFactory.fillDefaults().grab(true, true).applyTo(_txtProperties);
   }

   private void enableControls() {

      final boolean isSetProperties = _chkSetProperties.getSelection();

      _txtProperties.setEnabled(isSetProperties);
   }

   @Override
   public void init(final IWorkbench workbench) {}

   private void initUI(final Composite parent) {

   }

   @Override
   protected void performApply() {

      saveState();

      super.performApply();
   }

   @Override
   protected void performDefaults() {

      _chkSetProperties.setSelection(STATE_IS_SET_SYSTEM_PROPERTIES_DEFAULT);

      enableControls();

      super.performDefaults();
   }

   @Override
   public boolean performOk() {

      saveState();

      return true;
   }

   private void restoreState() {

      _chkSetProperties.setSelection(Util.getStateBoolean(_state, STATE_IS_SET_SYSTEM_PROPERTIES, STATE_IS_SET_SYSTEM_PROPERTIES_DEFAULT));

      _txtProperties.setText(Util.getStateString(_state, STATE_SYSTEM_PROPERTIES, UI.EMPTY_STRING));
   }

   private void saveState() {

      _state.put(STATE_IS_SET_SYSTEM_PROPERTIES, _chkSetProperties.getSelection());
      _state.put(STATE_SYSTEM_PROPERTIES, _txtProperties.getText());
   }
}
