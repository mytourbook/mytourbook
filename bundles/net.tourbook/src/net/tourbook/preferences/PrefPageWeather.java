/*******************************************************************************
 * Copyright (C) 2019, 2022 Frédéric Bard
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

import net.tourbook.ui.views.WeatherProvidersUI;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageWeather extends PreferencePage implements IWorkbenchPreferencePage {

   public static final String ID = "net.tourbook.preferences.PrefPageWeather"; //$NON-NLS-1$

   private WeatherProvidersUI _weatherProvidersUI;

   @Override
   protected Control createContents(final Composite parent) {

      final Composite ui = createUI(parent);

      return ui;
   }

   private Composite createUI(final Composite parent) {

      _weatherProvidersUI = new WeatherProvidersUI();
      _weatherProvidersUI.createUI(parent);

      return parent;
   }

   @Override
   public void init(final IWorkbench workbench) {}

}
