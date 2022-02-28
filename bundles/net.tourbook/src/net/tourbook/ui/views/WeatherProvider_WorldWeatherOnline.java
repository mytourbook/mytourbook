/*******************************************************************************
 * Copyright (C) 2022 Frédéric Bard
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
package net.tourbook.ui.views;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.weather.worldweatheronline.WorldWeatherOnlineRetriever;
import net.tourbook.web.WEB;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class WeatherProvider_WorldWeatherOnline implements IWeatherProvider {

   private static HttpClient      httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
   private final IPreferenceStore _prefStore = TourbookPlugin.getDefault().getPreferenceStore();

   //todo fb hide the api key just like in the cloud pref page
   /*
    * UI controls
    */
   private Button _btnTestConnection;
   private Label  _labelApiKey;

   private Text   _textApiKey;

   public WeatherProvider_WorldWeatherOnline() {}

   @Override
   public Composite createUI(final WeatherProvidersUI weatherProvidersUI,
                             final Composite parent,
                             final FormToolkit tk) {

      final int defaultHIndent = 16;

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
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
            _textApiKey.addModifyListener(event -> onModifyApiKey());
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .applyTo(_textApiKey);
         }
         {
            /*
             * WWO Api Sign-up page
             */

            // Link - see http(s)://www.worldweatheronline.com/developer/signup.aspx
            final Link linkApiSignup = new Link(container, SWT.PUSH);
            linkApiSignup.setText(Messages.Pref_Weather_Link_ApiSignup);
            linkApiSignup.setEnabled(true);
            linkApiSignup.addListener(
                  SWT.Selection,
                  event -> WEB.openUrl(Messages.External_Link_Weather_ApiSignup));
            GridDataFactory.fillDefaults()
                  .span(2, 1)
                  .indent(defaultHIndent, 0)
                  .applyTo(linkApiSignup);
         }
         {
            /*
             * Button: test connection
             */
            _btnTestConnection = new Button(container, SWT.NONE);
            _btnTestConnection.setText(Messages.Pref_Weather_Button_TestHTTPConnection);
            _btnTestConnection.addSelectionListener(widgetSelectedAdapter(
                  selectionEvent -> onCheckConnection()));
            GridDataFactory.fillDefaults()
                  .indent(defaultHIndent, 0)
                  .align(SWT.BEGINNING, SWT.FILL)
                  .span(2, 1)
                  .applyTo(_btnTestConnection);
         }
      }

      restoreState();
      enableControls();

      return container;
   }

   @Override
   public void dispose() {}

   private void enableControls() {

      onModifyApiKey();
   }

   /**
    * This method ensures the connection to the API can be made successfully.
    */
   private void onCheckConnection() {

      BusyIndicator.showWhile(Display.getCurrent(), () -> {

         try {

            final HttpRequest request = HttpRequest
                  .newBuilder(URI.create(WorldWeatherOnlineRetriever.getApiUrl() +
                        _textApiKey.getText()))
                  .GET()
                  .build();

            final HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());

            final int statusCode = response.statusCode();
            final String responseMessage = response.body();

            final String message = statusCode == HttpURLConnection.HTTP_OK
                  ? NLS.bind(Messages.Pref_Weather_CheckHTTPConnection_OK_Message, WorldWeatherOnlineRetriever.getBaseApiUrl())
                  : NLS.bind(
                        Messages.Pref_Weather_CheckHTTPConnection_FAILED_Message,
                        new Object[] {
                              WorldWeatherOnlineRetriever.getBaseApiUrl(),
                              statusCode,
                              responseMessage });

            MessageDialog.openInformation(
                  Display.getCurrent().getActiveShell(),
                  Messages.Pref_Weather_CheckHTTPConnection_Message,
                  message);

         } catch (final IOException | InterruptedException e) {
            StatusUtil.log(e);
            Thread.currentThread().interrupt();
         }
      });
   }

   private void onModifyApiKey() {

      _btnTestConnection.setEnabled(StringUtils.hasContent(_textApiKey.getText()));
   }

   @Override
   public void performDefaults() {

      _textApiKey.setText(_prefStore.getDefaultString(ITourbookPreferences.WEATHER_API_KEY));
   }

   private void restoreState() {

      _textApiKey.setText(_prefStore.getString(ITourbookPreferences.WEATHER_API_KEY));
   }

   @Override
   public void saveState() {

      _prefStore.setValue(ITourbookPreferences.WEATHER_API_KEY, _textApiKey.getText());
   }
}
