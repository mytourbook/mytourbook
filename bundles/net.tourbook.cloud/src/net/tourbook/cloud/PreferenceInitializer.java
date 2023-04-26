/*******************************************************************************
 * Copyright (C) 2020, 2023 Frédéric Bard
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

import java.util.ArrayList;
import java.util.List;

import net.tourbook.common.UI;
import net.tourbook.data.TourPerson;
import net.tourbook.database.PersonManager;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

public class PreferenceInitializer extends AbstractPreferenceInitializer {

   //This is the date (01/26/2021) that Suunto forced the users to switch to Suunto App.
   public static final long SUUNTO_FILTER_SINCE_DATE = 1611619200000L;
   // Tuesday, January 1, 2030 1:00:00 AM GMT
   public static final long SUUNTO_FILTER_END_DATE   = 1893459600000L;

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
      store.setDefault(Preferences.STRAVA_ADDWEATHERICON_IN_TITLE, false);
      store.setDefault(Preferences.STRAVA_SENDDESCRIPTION, true);
      store.setDefault(Preferences.STRAVA_SENDWEATHERDATA_IN_DESCRIPTION, false);

      initializeDefaultSuuntoPreferences(store);
      store.setDefault(Preferences.SUUNTO_SELECTED_PERSON_INDEX, 0);
      store.setDefault(Preferences.SUUNTO_SELECTED_PERSON_ID, UI.EMPTY_STRING);
      store.setDefault(Preferences.SUUNTO_FILENAME_COMPONENTS, "{SUUNTO_FILE_NAME}.{FIT_EXTENSION}"); //$NON-NLS-1$
   }

   private void initializeDefaultSuuntoPreferences(final IPreferenceStore store) {

      final List<TourPerson> tourPeopleList = PersonManager.getTourPeople();
      final List<String> tourPersonIds = new ArrayList<>();

      // This empty string represents "All people"
      tourPersonIds.add(UI.EMPTY_STRING);
      tourPeopleList.forEach(
            tourPerson -> tourPersonIds.add(
                  String.valueOf(tourPerson.getPersonId())));

      for (final String tourPersonId : tourPersonIds) {

         store.setDefault(Preferences.getPerson_SuuntoAccessToken_String(tourPersonId), UI.EMPTY_STRING);
         store.setDefault(Preferences.getPerson_SuuntoRefreshToken_String(tourPersonId), UI.EMPTY_STRING);
         store.setDefault(Preferences.getPerson_SuuntoAccessTokenExpiresIn_String(tourPersonId), 0L);
         store.setDefault(Preferences.getPerson_SuuntoAccessTokenIssueDateTime_String(tourPersonId), 0L);
         store.setDefault(Preferences.getPerson_SuuntoWorkoutDownloadFolder_String(tourPersonId), UI.EMPTY_STRING);
         store.setDefault(Preferences.getPerson_SuuntoUseWorkoutFilterStartDate_String(tourPersonId), false);
         store.setDefault(Preferences.getPerson_SuuntoWorkoutFilterStartDate_String(tourPersonId), SUUNTO_FILTER_SINCE_DATE);
         store.setDefault(Preferences.getPerson_SuuntoUseWorkoutFilterEndDate_String(tourPersonId), false);
         store.setDefault(Preferences.getPerson_SuuntoWorkoutFilterEndDate_String(tourPersonId), SUUNTO_FILTER_END_DATE);
      }
   }
}
