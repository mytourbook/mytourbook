/*******************************************************************************
 * Copyright (C) 2019, 2020 Frédéric Bard
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

import de.byteholder.geoclipse.map.UI;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.weather.HistoricalWeatherRetriever;
import net.tourbook.web.WEB;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
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
   private Button _btnTestConnection;
   private Button _chkWeatherRetrieval;

   private Label  _labelApiKey;

   private Link   _linkApiSignup;

   private Text   _textApiKey;

   @Override
   protected Control createContents(final Composite parent) {

      final Composite ui = createUI(parent);

      restoreState();

      enableControls();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final int defaultHIndent = 16;

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         {
            /*
             * Use the weather retrieval feature
             */
            // checkbox
            _chkWeatherRetrieval = new Button(container, SWT.CHECK);
            _chkWeatherRetrieval.setText(Messages.Pref_Weather_Checkbox_UseRetrieval);
            _chkWeatherRetrieval.setToolTipText(Messages.Pref_Weather_Checkbox_UseRetrieval_Tooltip);
            _chkWeatherRetrieval.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onSelectCheckWeatherRetrieval();
               }
            });
            GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkWeatherRetrieval);
         }
         {
            /*
             * API key
             */

            // label
            _labelApiKey = new Label(container, SWT.WRAP);
            _labelApiKey.setText(Messages.Pref_Weather_Label_ApiKey);
            GridDataFactory.fillDefaults()
                  .indent(defaultHIndent, 0)
                  .align(SWT.FILL, SWT.CENTER)
                  .applyTo(_labelApiKey);

            // text
            _textApiKey = new Text(container, SWT.BORDER);
            _textApiKey.setToolTipText(Messages.Pref_Weather_Label_ApiKey_Tooltip);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .applyTo(_textApiKey);
         }
         {
            /*
             * WWO Api Sign-up page
             */

            // Link - see http(s)://www.worldweatheronline.com/developer/signup.aspx
            _linkApiSignup = new Link(container, SWT.PUSH);
            _linkApiSignup.setText(Messages.Pref_Weather_Link_ApiSignup);
            _linkApiSignup.setEnabled(true);
            _linkApiSignup.addListener(SWT.Selection, new Listener() {
               @Override
               public void handleEvent(final Event event) {
                  WEB.openUrl(Messages.External_Link_Weather_ApiSignup);
               }
            });
            GridDataFactory.fillDefaults()
                  .span(2, 1)
                  .indent(defaultHIndent, 0)
                  .applyTo(_linkApiSignup);
         }
         {
            /*
             * Button: test connection
             */
            _btnTestConnection = new Button(container, SWT.NONE);
            _btnTestConnection.setText(Messages.Pref_Weather_Button_TestHTTPConnection);
            _btnTestConnection.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onCheckConnection();
               }
            });
            GridDataFactory.fillDefaults()
                  .indent(defaultHIndent, 0)
                  .align(SWT.BEGINNING, SWT.FILL)
                  .span(2, 1)
                  .applyTo(_btnTestConnection);
         }
      }

      return container;
   }

   private void enableControls() {

      final boolean useWeatherRetrieval = _chkWeatherRetrieval.getSelection();

      _labelApiKey.setEnabled(useWeatherRetrieval);
      _textApiKey.setEnabled(useWeatherRetrieval);
      _btnTestConnection.setEnabled(useWeatherRetrieval);
   }

   @Override
   public void init(final IWorkbench workbench) {
      setPreferenceStore(TourbookPlugin.getDefault().getPreferenceStore());
   }

   @Override
   public boolean okToLeave() {
      return super.okToLeave();
   }

   /**
    * This method ensures the connection to the API can be made successfully.
    */
   private void onCheckConnection() {

      BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
         @Override
         public void run() {

            try {

               final URL url = new URL(HistoricalWeatherRetriever.getApiUrl() + _textApiKey.getText());
               final HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
               urlConn.connect();

               final int response = urlConn.getResponseCode();
               final String responseMessage = urlConn.getResponseMessage();

               final String message = response == HttpURLConnection.HTTP_OK
                     ? NLS.bind(Messages.Pref_Weather_CheckHTTPConnection_OK_Message, HistoricalWeatherRetriever.getBaseApiUrl())
                     : NLS.bind(
                           Messages.Pref_Weather_CheckHTTPConnection_FAILED_Message,
                           new Object[] {
                                 HistoricalWeatherRetriever.getBaseApiUrl(),
                                 Integer.toString(response),
                                 responseMessage == null ? UI.EMPTY_STRING : responseMessage });

               MessageDialog.openInformation(
                     Display.getCurrent().getActiveShell(),
                     Messages.Pref_Weather_CheckHTTPConnection_Message,
                     message);

            } catch (final IOException e) {
               e.printStackTrace();
            }
         }
      });
   }

   private void onSelectCheckWeatherRetrieval() {
      enableControls();
   }

   @Override
   protected void performDefaults() {

      _chkWeatherRetrieval.setSelection(_prefStore.getDefaultBoolean(ITourbookPreferences.WEATHER_USE_WEATHER_RETRIEVAL));
      _textApiKey.setText(_prefStore.getDefaultString(ITourbookPreferences.WEATHER_API_KEY));

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

      _chkWeatherRetrieval.setSelection(_prefStore.getBoolean(ITourbookPreferences.WEATHER_USE_WEATHER_RETRIEVAL));
      _textApiKey.setText(_prefStore.getString(ITourbookPreferences.WEATHER_API_KEY));
   }

   private void saveState() {

      final boolean useWeatherRetrieval = _chkWeatherRetrieval.getSelection();

      _prefStore.setValue(ITourbookPreferences.WEATHER_USE_WEATHER_RETRIEVAL, useWeatherRetrieval);
      _prefStore.setValue(ITourbookPreferences.WEATHER_API_KEY, _textApiKey.getText());
   }

}
