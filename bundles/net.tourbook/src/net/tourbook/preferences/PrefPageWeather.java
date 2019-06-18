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
import net.tourbook.web.WEB;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageWeather extends PreferencePage implements IWorkbenchPreferencePage {

   public static final String     ID         = "net.tourbook.preferences.PrefPageWeather"; //$NON-NLS-1$

   private final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

   /*
    * UI controls
    */
   private Button    _chkWeatherRetrieval;
   private Text      apiKeyFieldEditor;
   private Link      apiSignupLink;

   @Override
   protected Control createContents(final Composite parent) {
      final Composite ui = createUI(parent);

      restoreState();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final int defaultHIndent = 16;

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         // check box: use the weather retrieval feature
         {
            _chkWeatherRetrieval = new Button(container, SWT.CHECK);
            GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkWeatherRetrieval);
            _chkWeatherRetrieval.setText(Messages.Pref_Weather_Checkbox_UseRetrieval);
            _chkWeatherRetrieval.setToolTipText(Messages.Pref_Weather_Checkbox_UseRetrieval_Tooltip);
            _chkWeatherRetrieval.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onSelectCheckWeatherRetrieval();
               }
            });

            //Label: API Key
            final Label label = new Label(container, SWT.WRAP);
            label.setText(Messages.Pref_Weather_ApiKey_FieldEditor);
            GridDataFactory.swtDefaults().indent(defaultHIndent, 0).applyTo(label);

            // text: API Key
            apiKeyFieldEditor = new Text(container, SWT.BORDER);
            apiKeyFieldEditor.setEnabled(_prefStore.getBoolean(ITourbookPreferences.WEATHER_USE_WEATHER_RETRIEVAL));
            apiKeyFieldEditor.setToolTipText(Messages.Pref_Weather_ApiKey_FieldEditor_Tooltip);
            GridDataFactory.swtDefaults()
                  .indent(defaultHIndent, 0)
                  .hint(200, 15)
                  .applyTo(apiKeyFieldEditor);

            //Link to the WWO Api Sign-up page
            //See https://www.worldweatheronline.com/developer/signup.aspx
            apiSignupLink = new Link(container, SWT.PUSH);
            apiSignupLink.setText(Messages.Pref_Weather_ApiSignupLink);
            GridDataFactory.fillDefaults()
                  .span(2, 1)
                  .indent(defaultHIndent, 0)
                  .applyTo(apiSignupLink);
            apiSignupLink.addListener(SWT.Selection, new Listener() {
               @Override
               public void handleEvent(final Event event) {
                  WEB.openUrl(Messages.Pref_Weather_External_Link_WeatherApi);
               }
            });
         }
      }

      return container;
   }

   private void enableControls() {
      final boolean useWeatherRetrieval = _chkWeatherRetrieval.getSelection();
      apiKeyFieldEditor.setEnabled(useWeatherRetrieval);
      apiSignupLink.setEnabled(useWeatherRetrieval);
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
      _chkWeatherRetrieval.setSelection(_prefStore.getBoolean(ITourbookPreferences.WEATHER_USE_WEATHER_RETRIEVAL));
      apiKeyFieldEditor.setText(_prefStore.getString(ITourbookPreferences.WEATHER_API_KEY));

      enableControls();

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
      _chkWeatherRetrieval.setSelection(_prefStore.getBoolean(
            ITourbookPreferences.WEATHER_USE_WEATHER_RETRIEVAL));
      apiKeyFieldEditor.setText(_prefStore.getString(ITourbookPreferences.WEATHER_API_KEY));
   }

   private void saveState() {

      final boolean useWeatherRetrieval = _chkWeatherRetrieval.getSelection();

      _prefStore.setValue(ITourbookPreferences.WEATHER_USE_WEATHER_RETRIEVAL, useWeatherRetrieval);
      _prefStore.setValue(ITourbookPreferences.WEATHER_API_KEY, apiKeyFieldEditor.getText());
   }

}
