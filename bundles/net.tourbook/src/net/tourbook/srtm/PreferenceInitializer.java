/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.srtm;

import net.tourbook.application.TourbookPlugin;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Initialize preferences for SRTM
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

   @Override
   public void initializeDefaultPreferences() {

      final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

      prefStore.setDefault(IPreferences.SRTM_USE_DEFAULT_DATA_FILEPATH, true);

      // set srtm default data path to the working directory
      prefStore.setDefault(IPreferences.SRTM_DATA_FILEPATH, Platform.getInstanceLocation().getURL().getPath());

      // set srtm actual color profile index
      prefStore.setDefault(IPreferences.SRTM_COLORS_SELECTED_PROFILE_ID, 0);
      prefStore.setDefault(IPreferences.SRTM_COLORS_SELECTED_PROFILE_KEY, 0);

      // apply profile when it's selected in the profile list
      prefStore.setDefault(IPreferences.SRTM_APPLY_WHEN_PROFILE_IS_SELECTED, false);

      // set validation date to be discated
      prefStore.setDefault(IPreferences.NASA_EARTHDATA_ACCOUNT_VALIDATION_DATE, Long.MIN_VALUE);
   }
}
