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

   public static final String WEATHER_PROVIDER_NONE               = "NoWeatherProvider";  //$NON-NLS-1$
   public static final String WEATHER_PROVIDER_OPENWEATHERMAP     = "OpenWeatherMap";     //$NON-NLS-1$
   public static final String WEATHER_PROVIDER_WORLDWEATHERONLINE = "WorldWeatherOnline"; //$NON-NLS-1$

   /**
    * @param weatherProvidersUI
    * @param parent
    * @param toolkit
    * @return
    */
   Composite createUI(WeatherProvidersUI weatherProvidersUI,
                      Composite parent,
                      FormToolkit toolkit);

   void dispose();

   void performDefaults();

   void saveState();

}
