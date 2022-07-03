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

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.StringUtils;
import net.tourbook.common.util.Util;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.weather.HistoricalWeatherRetriever;
import net.tourbook.weather.worldweatheronline.WorldWeatherOnlineRetriever;
import net.tourbook.web.WEB;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class WeatherProvider_WorldWeatherOnline implements IWeatherProvider {

   private final IPreferenceStore _prefStore = TourbookPlugin.getDefault().getPreferenceStore();

   /*
    * UI controls
    */
   private Button _btnTestConnection;
   private Button _chkShowHideApiKey;

   private Label  _labelApiKey;

   private Text   _textApiKey_Value;

   public WeatherProvider_WorldWeatherOnline() {}

   @Override
   public Composite createUI(final WeatherProvidersUI weatherProvidersUI,
                             final Composite parent,
                             final FormToolkit formToolkit) {

      final int defaultHIndent = 16;

      final Composite container = formToolkit.createComposite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         {
            /*
             * API key
             */

            // label
            _labelApiKey = formToolkit.createLabel(container,
                  Messages.Pref_Weather_Label_ApiKey,
                  SWT.WRAP);
            GridDataFactory.fillDefaults()
                  .indent(defaultHIndent, 0)
                  .align(SWT.FILL, SWT.CENTER)
                  .applyTo(_labelApiKey);

            // text
            _textApiKey_Value = new Text(container, SWT.PASSWORD | SWT.BORDER);
            _textApiKey_Value.setToolTipText(Messages.Pref_Weather_Label_WorldWeatherOnline_ApiKey_Tooltip);
            _textApiKey_Value.addModifyListener(event -> onModifyApiKey());
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .applyTo(_textApiKey_Value);
         }
         {
            /*
             * Show/hide API key
             */

            _chkShowHideApiKey = formToolkit.createButton(
                  container,
                  Messages.Pref_Weather_Checkbox_ShowOrHideApiKey,
                  SWT.CHECK);
            _chkShowHideApiKey.setToolTipText(
                  Messages.Pref_Weather_Checkbox_ShowOrHideApiKey_Tooltip);
            _chkShowHideApiKey.addSelectionListener(widgetSelectedAdapter(
                  selectionEvent -> Util.showOrHidePassword(
                        _textApiKey_Value,
                        _chkShowHideApiKey.getSelection())));
            GridDataFactory.fillDefaults()
                  .indent(defaultHIndent, 0)
                  .applyTo(_chkShowHideApiKey);
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
                  event -> WEB.openUrl("https://www.worldweatheronline.com/developer/signup.aspx")); //$NON-NLS-1$
            GridDataFactory.fillDefaults()
                  .span(2, 1)
                  .indent(defaultHIndent, 0)
                  .applyTo(linkApiSignup);
         }
         {
            /*
             * Button: test connection
             */
            _btnTestConnection = formToolkit.createButton(
                  container,
                  Messages.Pref_Weather_Button_TestHTTPConnection,
                  SWT.NONE);
            _btnTestConnection.addSelectionListener(widgetSelectedAdapter(
                  selectionEvent -> HistoricalWeatherRetriever.checkVendorConnection(
                        WorldWeatherOnlineRetriever.getApiUrl() + _textApiKey_Value.getText(),
                        IWeatherProvider.WEATHER_PROVIDER_WORLDWEATHERONLINE_NAME)));
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

   private void onModifyApiKey() {

      _btnTestConnection.setEnabled(StringUtils.hasContent(_textApiKey_Value.getText()));
   }

   @Override
   public void performDefaults() {

      _textApiKey_Value.setText(_prefStore.getDefaultString(ITourbookPreferences.WEATHER_API_KEY));
   }

   private void restoreState() {

      _textApiKey_Value.setText(_prefStore.getString(ITourbookPreferences.WEATHER_API_KEY));
   }

   @Override
   public void saveState() {

      _prefStore.setValue(ITourbookPreferences.WEATHER_API_KEY, _textApiKey_Value.getText());
   }
}
