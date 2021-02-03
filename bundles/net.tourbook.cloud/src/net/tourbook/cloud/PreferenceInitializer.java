/*******************************************************************************
 * Copyright (C) 2020, 2021 Frédéric Bard
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
package net.tourbook.cloud;

import net.tourbook.common.UI;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

public class PreferenceInitializer extends AbstractPreferenceInitializer {

   public PreferenceInitializer() {}

   @Override
   public void initializeDefaultPreferences() {

      final IPreferenceStore store = Activator.getDefault().getPreferenceStore();

      store.setDefault(Preferences.DROPBOX_ACCESSTOKEN, UI.EMPTY_STRING);
      store.setDefault(Preferences.DROPBOX_REFRESHTOKEN, UI.EMPTY_STRING);
      store.setDefault(Preferences.DROPBOX_ACCESSTOKEN_EXPIRES_IN, 0);
      store.setDefault(Preferences.DROPBOX_ACCESSTOKEN_ISSUE_DATETIME, 0);

      store.setDefault(Preferences.STRAVA_ACCESSTOKEN, UI.EMPTY_STRING);
      store.setDefault(Preferences.STRAVA_REFRESHTOKEN, UI.EMPTY_STRING);
      store.setDefault(Preferences.STRAVA_ACCESSTOKEN_EXPIRES_AT, 0);
      store.setDefault(Preferences.STRAVA_ATHLETEID, UI.EMPTY_STRING);
      store.setDefault(Preferences.STRAVA_ATHLETEFULLNAME, UI.EMPTY_STRING);

      store.setDefault(Preferences.SUUNTO_ACCESSTOKEN, UI.EMPTY_STRING);
      store.setDefault(Preferences.SUUNTO_REFRESHTOKEN, UI.EMPTY_STRING);
      store.setDefault(Preferences.SUUNTO_ACCESSTOKEN_EXPIRES_IN, 0);
      store.setDefault(Preferences.SUUNTO_ACCESSTOKEN_ISSUE_DATETIME, 0);
      store.setDefault(Preferences.SUUNTO_WORKOUT_DOWNLOAD_FOLDER, UI.EMPTY_STRING);
      store.setDefault(Preferences.SUUNTO_USE_WORKOUT_FILTER_SINCE_DATE, false);
      store.setDefault(Preferences.SUUNTO_WORKOUT_FILTER_SINCE_DATE, 1611619200000L);

   }
}
