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

import java.text.NumberFormat;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.StringUtils;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.PageBook;

public class WeatherProvidersUI {

   private static final WeatherProvider[] WEATHER_PROVIDER = {

         new WeatherProvider(
               IWeatherProvider.WEATHER_PROVIDER_NONE,
               "None"),                                                                    //Messages.TourChart_Smoothing_Algorithm_Initial),

         new WeatherProvider(
               IWeatherProvider.WEATHER_PROVIDER_OPENWEATHERMAP,
               "OpenWeatherMap"),                                                          //Messages.TourChart_Smoothing_Algorithm_Jamet),

         new WeatherProvider(
               IWeatherProvider.WEATHER_PROVIDER_WORLDWEATHERONLINE,
               "WWO")                                                                      //Messages.TourChart_Smoothing_Algorithm_NoSmoothing),
   };

   private final IPreferenceStore         _prefStore       = TourbookPlugin.getPrefStore();

   private ITourEventListener             _tourEventListener;

   private FocusListener                  _keepOpenListener;
   {
      _keepOpenListener = new FocusListener() {

         @Override
         public void focusGained(final FocusEvent e) {

            if (_slideout != null) {

               /*
                * This will fix the problem that when the list of a combobox is displayed, then
                * the slideout will disappear :-(((
                */
               _slideout.setIsAnotherDialogOpened(true);
            }
         }

         @Override
         public void focusLost(final FocusEvent e) {

            if (_slideout != null) {
               _slideout.setIsAnotherDialogOpened(false);
            }

         }
      };
   }

   private boolean          _isUpdateUI;

   private IWeatherProvider _weatherProvider_None               = new WeatherProvider_None();
   private IWeatherProvider _weatherProvider_OpenWeatherMap     = new WeatherProvider_OpenWeatherMap();
   private IWeatherProvider _weatherProvider_WorldWeatherOnline = new WeatherProvider_WorldWeatherOnline();

   private NumberFormat     _nf0                                = NumberFormat.getNumberInstance();

   {
      _nf0.setMinimumFractionDigits(0);
      _nf0.setMaximumFractionDigits(0);
   }

   /*
    * UI controls
    */
   private FormToolkit     _tk;

   private Composite       _uiContainer;

   private Combo           _comboWeatherProvider;

   private PageBook        _pagebookWeatherProvider;

   private Composite       _pageNoneUI;
   private Composite       _pageOpenWeatherMapUI;
   private Composite       _pageWorldWeatherOnlineUI;

   private ToolbarSlideout _slideout;

   public WeatherProvidersUI() {}

   /**
    * @param tk
    *           This toolkit will be disposed when the UI is disposed;
    * @param toolbarSlideout
    */
   public WeatherProvidersUI(final FormToolkit tk, final ToolbarSlideout slideout) {

      _tk = tk;
      _slideout = slideout;
   }

   public void createUI(final Composite parent) {

      initUI(parent);

      createUI_10(parent);

      setupUI();

      restoreState();
      updateUI();
   }

   private void createUI_10(final Composite parent) {

      _uiContainer = _tk.createComposite(parent);
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .applyTo(_uiContainer);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(_uiContainer);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
      {
         createUI_10_WeatherProvider(_uiContainer);
         createUI_20_WeatherProviderPagebook(_uiContainer);
      }
   }

   private void createUI_10_WeatherProvider(final Composite parent) {

      final Composite container = _tk.createComposite(parent);
      GridDataFactory.fillDefaults().grab(false, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).extendedMargins(0, 0, 0, 5).applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
      {
         /*
          * Label: weather provider
          */
         final Label label = _tk.createLabel(container, "weather provider");//Messages.TourChart_Smoothing_Label_SmoothingAlgorithm);
         GridDataFactory.fillDefaults()
               .align(SWT.FILL, SWT.CENTER)
               .applyTo(label);

         /*
          * Combo: weather provider
          */
         _comboWeatherProvider = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
         _comboWeatherProvider.setVisibleItemCount(10);
         _comboWeatherProvider.addFocusListener(_keepOpenListener);
         _comboWeatherProvider.addSelectionListener(widgetSelectedAdapter(selectionEvent -> {
            if (_isUpdateUI) {
               return;
            }
            onSelectWeatherProvider();
         }));
         GridDataFactory.fillDefaults()
//               .align(SWT.END, SWT.FILL)
               .indent(20, 0)
               .applyTo(_comboWeatherProvider);
         _tk.adapt(_comboWeatherProvider, true, true);
      }
   }

   private void createUI_20_WeatherProviderPagebook(final Composite parent) {
      /*
       * Pagebook: Weather provider
       */
      _pagebookWeatherProvider = new PageBook(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .span(2, 1)
            .applyTo(_pagebookWeatherProvider);
      {
         _pageNoneUI = _weatherProvider_None.createUI(
               this,
               _pagebookWeatherProvider,
               _tk);

         _pageOpenWeatherMapUI = _weatherProvider_OpenWeatherMap.createUI(
               this,
               _pagebookWeatherProvider,
               _tk);

         _pageWorldWeatherOnlineUI = _weatherProvider_WorldWeatherOnline.createUI(
               this,
               _pagebookWeatherProvider,
               _tk);
      }
   }

   public void dispose() {

      TourManager.getInstance().removeTourEventListener(_tourEventListener);

      _weatherProvider_None.dispose();
      _weatherProvider_OpenWeatherMap.dispose();
      _weatherProvider_WorldWeatherOnline.dispose();

      _tk.dispose();
   }

   private WeatherProvider getSelectedWeatherProvider() {
      return WEATHER_PROVIDER[_comboWeatherProvider.getSelectionIndex()];
   }

   private void initUI(final Composite parent) {

      if (_tk == null) {

         // it could be already created
         _tk = new FormToolkit(parent.getDisplay());
      }
   }

   protected void onModifyWeatherProvider() {}

   private void onSelectWeatherProvider() {

      updateUI();

      // update pref store
      saveState();

      // force tours to be recomputed
      TourManager.getInstance().removeAllToursFromCache();

      // fire unique event for all changes
      TourManager.fireEvent(TourEventId.TOUR_CHART_PROPERTY_IS_MODIFIED);
   }

   public void performDefaults() {

      final String defaultWeatherProviderId = _prefStore.getDefaultString(//
            ITourbookPreferences.WEATHER_WEATHER_PROVIDER_ID);

      _prefStore.setValue(ITourbookPreferences.WEATHER_WEATHER_PROVIDER_ID, defaultWeatherProviderId);

      selectWeatherProvider(defaultWeatherProviderId);

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
      }
      _isUpdateUI = false;
   }

   /**
    * Update new values in the pref store
    */
   private void saveState() {

      _prefStore.setValue(//
            ITourbookPreferences.WEATHER_WEATHER_PROVIDER_ID,
            getSelectedWeatherProvider().weatherProviderId);

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
         for (final WeatherProvider weatherProvider : WEATHER_PROVIDER) {
            _comboWeatherProvider.add(weatherProvider.uiText);
         }
         _comboWeatherProvider.select(0);
      }
      _isUpdateUI = false;
   }

   private void updateUI() {

      final String selectedWeatherProvider = getSelectedWeatherProvider().weatherProviderId;

      // select weather provider page
      if (selectedWeatherProvider.equals(IWeatherProvider.WEATHER_PROVIDER_NONE)) {

         _pagebookWeatherProvider.showPage(_pageNoneUI);

      } else if (selectedWeatherProvider.equals(IWeatherProvider.WEATHER_PROVIDER_OPENWEATHERMAP)) {

         _pagebookWeatherProvider.showPage(_pageOpenWeatherMapUI);

      } else if (selectedWeatherProvider.equals(IWeatherProvider.WEATHER_PROVIDER_WORLDWEATHERONLINE)) {

         _pagebookWeatherProvider.showPage(_pageWorldWeatherOnlineUI);
      }

      UI.updateScrolledContent(_uiContainer);

      // fire event to pack the UI, this is needed when the UI is in a slideout
      onModifyWeatherProvider();
   }
}
