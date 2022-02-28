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

import java.net.http.HttpClient;
import java.time.Duration;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.ui.views.WeatherProvidersUI;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageWeather extends PreferencePage implements IWorkbenchPreferencePage {

   //todo fb hide the api key just like in the cloud pref page
   public static final String ID = "net.tourbook.preferences.PrefPageWeather"; //$NON-NLS-1$

   //todo fb add drop down menu for each provider
   //make it as generic as possible so a new provider can be quickly added
   //put each tab's code in a new file and create a generic file for "Generic Weather Provider's tab.java"

   private static HttpClient      httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

   private WeatherProvidersUI     _weatherProvidersUI;

   private final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

   @Override
   protected Control createContents(final Composite parent) {

      final Composite ui = createUI(parent);

      restoreState();

      enableControls();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      _weatherProvidersUI = new WeatherProvidersUI();

      _weatherProvidersUI.createUI(parent, true, true);

      return parent;

   }

   private void enableControls() {

   }

   @Override
   public void init(final IWorkbench workbench) {
      setPreferenceStore(TourbookPlugin.getDefault().getPreferenceStore());
   }

   @Override
   protected void performDefaults() {

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

   }

   private void saveState() {

   }

}
