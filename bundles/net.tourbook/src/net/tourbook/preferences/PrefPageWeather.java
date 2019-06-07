/*******************************************************************************
 * Copyright (C) 2005, 2019  Wolfgang Schramm and Contributors
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

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageWeather extends PreferencePage implements IWorkbenchPreferencePage {

   public static final String     ID         = "net.tourbook.preferences.PrefPageWeather"; //$NON-NLS-1$

   private final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

   /*
    * UI controls
    */
   private Button            _chkWeatherRetrieval;
   private StringFieldEditor apiKeyFieldEditor;

   private Composite         container;

   @Override
   protected Control createContents(final Composite parent) {
      final Composite ui = createUI(parent);

      restoreState();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         // checkbox: use the weather retrieval feature
         {
            _chkWeatherRetrieval = new Button(container, SWT.CHECK);
            GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkWeatherRetrieval);
            _chkWeatherRetrieval.setText(Messages.PrefPage_Weather_Checkbox_UseRetrieval);
            _chkWeatherRetrieval.setToolTipText(Messages.PrefPage_Weather_Checkbox_UseRetrieval_Tooltip);
            _chkWeatherRetrieval.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onSelectCheckWeatherRetrieval();
               }
            });

            // text: API Key
            apiKeyFieldEditor = new StringFieldEditor(ITourbookPreferences.TOUR_EDITOR_DESCRIPTION_HEIGHT,
                  Messages.pref_weather_apiKey_FieldEditor,
                  container);
            apiKeyFieldEditor.setEnabled(_prefStore.getBoolean(ITourbookPreferences.STATE_USE_WEATHER_RETRIEVAL), container);
            apiKeyFieldEditor.getLabelControl(container).setToolTipText(Messages.pref_weather_apiKey_FieldEditor_tooltip);
            UI.setFieldWidth(container, apiKeyFieldEditor, 320);
//TODO add link to "Get API KEY"
         }
      }

      return container;
   }

   private void enableControls() {
      final boolean useWeatherRetrieval = _chkWeatherRetrieval.getSelection();
      apiKeyFieldEditor.setEnabled(useWeatherRetrieval, container);
   }

   @Override
   public void init(final IWorkbench workbench) {
      setPreferenceStore(TourbookPlugin.getDefault().getPreferenceStore());
   }

   @Override
   public boolean okToLeave() {
      return super.okToLeave();
   }

   private void onSelectCheckWeatherRetrieval() {
      enableControls();
   }

   @Override
   protected void performDefaults() {
      _chkWeatherRetrieval.setSelection(ITourbookPreferences.STATE_USE_WEATHER_RETRIEVAL_DEFAULT);
      apiKeyFieldEditor.setEnabled(ITourbookPreferences.STATE_USE_WEATHER_RETRIEVAL_DEFAULT, container);
      apiKeyFieldEditor.setStringValue(_prefStore.getString(ITourbookPreferences.API_KEY_DEFAULT));

      super.performDefaults();
   }

   @Override
   public boolean performOk() {

      final boolean isOK = super.performOk();

      if (isOK) {
         saveState();
      }

      return isOK;
   }

   private void restoreState() {

      final boolean useWeatherRetrieval = _prefStore.getBoolean(
            ITourbookPreferences.STATE_USE_WEATHER_RETRIEVAL);
      _chkWeatherRetrieval.setSelection(useWeatherRetrieval);
      apiKeyFieldEditor.setStringValue(_prefStore.getString(ITourbookPreferences.API_KEY));
   }

   private void saveState() {

      final boolean useWeatherRetrieval = _chkWeatherRetrieval.getSelection();

      _prefStore.setValue(ITourbookPreferences.STATE_USE_WEATHER_RETRIEVAL, useWeatherRetrieval);
      _prefStore.setValue(ITourbookPreferences.API_KEY, apiKeyFieldEditor.getStringValue());
   }

}
