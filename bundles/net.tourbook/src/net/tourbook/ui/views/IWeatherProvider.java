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

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

public interface IWeatherProvider {

   public static final String Pref_Weather_Provider_None               = "NoWeatherProvider";    //$NON-NLS-1$
   public static final String WEATHER_PROVIDER_OPENWEATHERMAP_ID       = "OpenWeatherMap";       //$NON-NLS-1$
   public static final String WEATHER_PROVIDER_WEATHERAPI_ID           = "WeatherAPI";           //$NON-NLS-1$
   public static final String WEATHER_PROVIDER_WEATHERAPI_NAME         = "Weather API";          //$NON-NLS-1$
   public static final String WEATHER_PROVIDER_WORLDWEATHERONLINE_ID   = "WorldWeatherOnline";   //$NON-NLS-1$
   public static final String WEATHER_PROVIDER_WORLDWEATHERONLINE_NAME = "World Weather Online"; //$NON-NLS-1$

   Composite createUI(WeatherProvidersUI weatherProvidersUI,
                      Composite parent,
                      FormToolkit formToolkit);

   void dispose();

   void performDefaults();

   void saveState();

}
