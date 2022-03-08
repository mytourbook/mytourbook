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

import java.util.Arrays;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.StringUtils;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.PageBook;

public class WeatherProvidersUI {

   private static final WeatherProvider[] WEATHER_PROVIDER                    = {

         new WeatherProvider(
               IWeatherProvider.Pref_Weather_Provider_None,
               Messages.Pref_Weather_Provider_None),

         new WeatherProvider(
               IWeatherProvider.WEATHER_PROVIDER_OPENWEATHERMAP,
               IWeatherProvider.WEATHER_PROVIDER_OPENWEATHERMAP),

         new WeatherProvider(
               IWeatherProvider.WEATHER_PROVIDER_WORLDWEATHERONLINE,
               IWeatherProvider.WEATHER_PROVIDER_WORLDWEATHERONLINE)
   };

   private final IPreferenceStore         _prefStore                          = TourbookPlugin.getPrefStore();

   private boolean                        _isUpdateUI;

   private IWeatherProvider               _weatherProvider_None               = new WeatherProvider_None();
   private IWeatherProvider               _weatherProvider_OpenWeatherMap     = new WeatherProvider_OpenWeatherMap();
   private IWeatherProvider               _weatherProvider_WorldWeatherOnline = new WeatherProvider_WorldWeatherOnline();

   /*
    * UI controls
    */
   private FormToolkit _formToolkit;

   private Composite   _uiContainer;

   private Combo       _comboWeatherProvider;

   private PageBook    _pagebookWeatherProvider;

   private Composite   _pageNoneUI;
   private Composite   _pageOpenWeatherMapUI;
   private Composite   _pageWorldWeatherOnlineUI;

   private Button      _chkDisplayFullLog;

   public WeatherProvidersUI() {}

   public void createUI(final Composite parent) {

      initUI(parent);

      createUI_10(parent);

      setupUI();

      restoreState();
      updateUI();
   }

   private void createUI_10(final Composite parent) {

      _uiContainer = _formToolkit.createComposite(parent);
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .applyTo(_uiContainer);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(_uiContainer);
      {
         createUI_10_WeatherProvider();
         createUI_20_WeatherProviderPagebook();
         createUI_30_WeatherProviderMainPreferences();
      }
   }

   private void createUI_10_WeatherProvider() {

      final Composite container = _formToolkit.createComposite(_uiContainer);
      GridDataFactory.fillDefaults().grab(false, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).extendedMargins(0, 0, 0, 5).applyTo(container);
      {
         /*
          * Label: weather provider
          */
         final Label label = _formToolkit.createLabel(
               container,
               Messages.Pref_Weather_Label_WeatherProvider);
         GridDataFactory.fillDefaults()
               .align(SWT.FILL, SWT.CENTER)
               .applyTo(label);

         /*
          * Combo: weather provider
          */
         _comboWeatherProvider = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
         _comboWeatherProvider.setVisibleItemCount(10);
         _comboWeatherProvider.addSelectionListener(widgetSelectedAdapter(selectionEvent -> {
            if (_isUpdateUI) {
               return;
            }
            onSelectWeatherProvider();
         }));
         GridDataFactory.fillDefaults()
               .indent(20, 0)
               .applyTo(_comboWeatherProvider);
      }
   }

   private void createUI_20_WeatherProviderPagebook() {
      /*
       * Pagebook: Weather provider
       */
      _pagebookWeatherProvider = new PageBook(_uiContainer, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .span(2, 1)
            .applyTo(_pagebookWeatherProvider);
      {
         _pageNoneUI = _weatherProvider_None.createUI(
               this,
               _pagebookWeatherProvider,
               _formToolkit);

         _pageOpenWeatherMapUI = _weatherProvider_OpenWeatherMap.createUI(
               this,
               _pagebookWeatherProvider,
               _formToolkit);

         _pageWorldWeatherOnlineUI = _weatherProvider_WorldWeatherOnline.createUI(
               this,
               _pagebookWeatherProvider,
               _formToolkit);
      }
   }

   private void createUI_30_WeatherProviderMainPreferences() {

      final Composite container = new Composite(_uiContainer, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().applyTo(container);
      {
         // Checkbox: Displays the full log
         _chkDisplayFullLog = new Button(container, SWT.CHECK);
         _chkDisplayFullLog.setText(
               Messages.Pref_Weather_Check_DisplayFullLog);
         _chkDisplayFullLog.setToolTipText(
               Messages.Pref_Weather_Check_DisplayFullLog_Tooltip);
         GridDataFactory.fillDefaults().applyTo(_chkDisplayFullLog);
      }
   }

   public void dispose() {

      _weatherProvider_None.dispose();
      _weatherProvider_OpenWeatherMap.dispose();
      _weatherProvider_WorldWeatherOnline.dispose();

      _formToolkit.dispose();
   }

   private WeatherProvider getSelectedWeatherProvider() {
      return WEATHER_PROVIDER[_comboWeatherProvider.getSelectionIndex()];
   }

   private void initUI(final Composite parent) {

      if (_formToolkit != null) {
         return;
      }

      // it could be already created
      _formToolkit = new FormToolkit(parent.getDisplay());
      _formToolkit.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
   }

   private void onSelectWeatherProvider() {

      updateUI();

      saveState();
   }

   public void performDefaults() {

      _chkDisplayFullLog.setSelection(
            _prefStore.getDefaultBoolean(ITourbookPreferences.WEATHER_DISPLAY_FULL_LOG));

      selectWeatherProvider(
            _prefStore.getDefaultString(ITourbookPreferences.WEATHER_WEATHER_PROVIDER_ID));

      updateUI();

      _weatherProvider_None.performDefaults();
      _weatherProvider_OpenWeatherMap.performDefaults();
      _weatherProvider_WorldWeatherOnline.performDefaults();
   }

   private void restoreState() {

      _isUpdateUI = true;
      {
         String weatherProviderId =
               _prefStore.getString(ITourbookPreferences.WEATHER_WEATHER_PROVIDER_ID);

         //For backwards compatibility purpose, we should select WorldWeatherOnline when
         //the api key is not.
         //Maybe this code could be suppressed in the future once most of the
         //users have upgraded to 22.X ?
         if (StringUtils.isNullOrEmpty(weatherProviderId) &&
               _prefStore.getBoolean(ITourbookPreferences.WEATHER_USE_WEATHER_RETRIEVAL)) {
            weatherProviderId = IWeatherProvider.WEATHER_PROVIDER_WORLDWEATHERONLINE;
         }

         // Weather provider
         selectWeatherProvider(weatherProviderId);

         _chkDisplayFullLog.setSelection(
               _prefStore.getBoolean(ITourbookPreferences.WEATHER_DISPLAY_FULL_LOG));
      }
      _isUpdateUI = false;
   }

   /**
    * Update new values in the preference store
    */
   public void saveState() {

      _prefStore.setValue(
            ITourbookPreferences.WEATHER_WEATHER_PROVIDER_ID,
            getSelectedWeatherProvider().weatherProviderId);

      _prefStore.setValue(
            ITourbookPreferences.WEATHER_DISPLAY_FULL_LOG,
            _chkDisplayFullLog.getSelection());

      _weatherProvider_None.saveState();
      _weatherProvider_OpenWeatherMap.saveState();
      _weatherProvider_WorldWeatherOnline.saveState();
   }

   private void selectWeatherProvider(final String prefWeatherProviderId) {

      int prefWeatherProviderIndex = -1;
      for (int weatherProviderIndex = 0; weatherProviderIndex < WEATHER_PROVIDER.length; weatherProviderIndex++) {

         if (WEATHER_PROVIDER[weatherProviderIndex].weatherProviderId.equals(prefWeatherProviderId)) {
            prefWeatherProviderIndex = weatherProviderIndex;
            break;
         }
      }
      if (prefWeatherProviderIndex == -1) {

         prefWeatherProviderIndex = 0;
      }
      _comboWeatherProvider.select(prefWeatherProviderIndex);
   }

   private void setupUI() {

      _isUpdateUI = true;
      {
         /*
          * Fillup weather provider combo
          */
         Arrays.asList(WEATHER_PROVIDER)
               .forEach(weatherProvider -> _comboWeatherProvider.add(weatherProvider.uiText));
         _comboWeatherProvider.select(0);
      }
      _isUpdateUI = false;
   }

   private void updateUI() {

      final String selectedWeatherProvider = getSelectedWeatherProvider().weatherProviderId;
      boolean areMainPreferencesVisible = true;

      // select weather provider page
      if (selectedWeatherProvider.equals(IWeatherProvider.Pref_Weather_Provider_None)) {

         _pagebookWeatherProvider.showPage(_pageNoneUI);
         areMainPreferencesVisible = false;

      } else if (selectedWeatherProvider.equals(IWeatherProvider.WEATHER_PROVIDER_OPENWEATHERMAP)) {

         _pagebookWeatherProvider.showPage(_pageOpenWeatherMapUI);

      } else if (selectedWeatherProvider.equals(IWeatherProvider.WEATHER_PROVIDER_WORLDWEATHERONLINE)) {

         _pagebookWeatherProvider.showPage(_pageWorldWeatherOnlineUI);
      }

      _chkDisplayFullLog.setVisible(areMainPreferencesVisible);

      UI.updateScrolledContent(_uiContainer);
   }
}
