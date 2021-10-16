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

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.StringUtils;
import net.tourbook.data.TourPerson;

public final class Preferences {

   /*
    * Dropbox preferences
    */
   public static final String DROPBOX_ACCESSTOKEN                = "DROPBOX_ACCESSTOKEN";                //$NON-NLS-1$
   public static final String DROPBOX_REFRESHTOKEN               = "DROPBOX_REFRESHTOKEN";               //$NON-NLS-1$
   public static final String DROPBOX_ACCESSTOKEN_EXPIRES_IN     = "DROPBOX_ACCESSTOKEN_EXPIRES_IN";     //$NON-NLS-1$
   public static final String DROPBOX_ACCESSTOKEN_ISSUE_DATETIME = "DROPBOX_ACCESSTOKEN_ISSUE_DATETIME"; //$NON-NLS-1$

   /*
    * Strava preferences
    */
   public static final String STRAVA_ACCESSTOKEN            = "STRAVA_ACCESSTOKEN";            //$NON-NLS-1$
   public static final String STRAVA_REFRESHTOKEN           = "STRAVA_REFRESHTOKEN";           //$NON-NLS-1$
   public static final String STRAVA_ACCESSTOKEN_EXPIRES_AT = "STRAVA_ACCESSTOKEN_EXPIRES_AT"; //$NON-NLS-1$
   public static final String STRAVA_ATHLETEID              = "STRAVA_ATHLETEID";              //$NON-NLS-1$
   public static final String STRAVA_ATHLETEFULLNAME        = "STRAVA_ATHLETEFULLNAME";        //$NON-NLS-1$
   public static final String STRAVA_SENDDESCRIPTION        = "STRAVA_SENDDESCRIPTION";        //$NON-NLS-1$
   public static final String STRAVA_USETOURTYPEMAPPING     = "STRAVA_USETOURTYPEMAPPING";     //$NON-NLS-1$

   /*
    * Suunto preferences
    */
   public static final String  SUUNTO_SELECTED_PERSON_INDEX         = "SUUNTO_SELECTED_PERSON_INDEX";         //$NON-NLS-1$
   public static final String  SUUNTO_SELECTED_PERSON_ID            = "SUUNTO_SELECTED_PERSON_ID";            //$NON-NLS-1$
   private static final String SUUNTO_ACCESSTOKEN                   = "SUUNTO_ACCESSTOKEN";                   //$NON-NLS-1$
   private static final String SUUNTO_REFRESHTOKEN                  = "SUUNTO_REFRESHTOKEN";                  //$NON-NLS-1$
   private static final String SUUNTO_ACCESSTOKEN_EXPIRES_IN        = "SUUNTO_ACCESSTOKEN_EXPIRES_IN";        //$NON-NLS-1$
   private static final String SUUNTO_ACCESSTOKEN_ISSUE_DATETIME    = "SUUNTO_ACCESSTOKEN_ISSUE_DATETIME";    //$NON-NLS-1$
   private static final String SUUNTO_WORKOUT_DOWNLOAD_FOLDER       = "SUUNTO_DOWNLOAD_FOLDER";               //$NON-NLS-1$
   private static final String SUUNTO_USE_WORKOUT_FILTER_SINCE_DATE = "SUUNTO_USE_WORKOUT_FILTER_SINCE_DATE"; //$NON-NLS-1$
   private static final String SUUNTO_WORKOUT_FILTER_SINCE_DATE     = "SUUNTO_WORKOUT_FILTER_SINCE_DATE";     //$NON-NLS-1$

   private static String getActivePersonId() {

      final TourPerson activePerson = TourbookPlugin.getActivePerson();

      String activePersonId = null;
      if (activePerson != null) {
         activePersonId = String.valueOf(activePerson.getPersonId());
      }

      return activePersonId;
   }

   public static String getPerson_SuuntoAccessToken_String(final String personId) {

      return getPersonPreferenceString(personId, SUUNTO_ACCESSTOKEN);
   }

   public static String getPerson_SuuntoAccessTokenExpiresIn_String(final String personId) {

      return getPersonPreferenceString(personId, SUUNTO_ACCESSTOKEN_EXPIRES_IN);
   }

   public static String getPerson_SuuntoAccessTokenIssueDateTime_String(final String personId) {

      return getPersonPreferenceString(personId, SUUNTO_ACCESSTOKEN_ISSUE_DATETIME);
   }

   public static String getPerson_SuuntoRefreshToken_String(final String personId) {

      return getPersonPreferenceString(personId, SUUNTO_REFRESHTOKEN);
   }

   public static String getPerson_SuuntoUseWorkoutFilterSinceDate_String(final String personId) {

      return getPersonPreferenceString(personId, SUUNTO_USE_WORKOUT_FILTER_SINCE_DATE);
   }

   public static String getPerson_SuuntoWorkoutDownloadFolder_String(final String personId) {

      return getPersonPreferenceString(personId, SUUNTO_WORKOUT_DOWNLOAD_FOLDER);
   }

   public static String getPerson_SuuntoWorkoutFilterSinceDate_String(final String personId) {

      return getPersonPreferenceString(personId, SUUNTO_WORKOUT_FILTER_SINCE_DATE);
   }

   private static String getPersonPreferenceString(final String personId, final String preferenceString) {

      final StringBuilder personSuuntoAccessToken = new StringBuilder();
      if (StringUtils.hasContent(personId)) {

         personSuuntoAccessToken.append(personId + UI.DASH);
      }

      return personSuuntoAccessToken.append(preferenceString).toString();
   }

   public static String getSuuntoAccessToken_Active_Person_String() {

      final String personId = getActivePersonId();

      return getPerson_SuuntoAccessToken_String(personId);
   }

   public static String getSuuntoAccessTokenExpiresIn_Active_Person_String() {

      final String personId = getActivePersonId();

      return getPerson_SuuntoAccessTokenExpiresIn_String(personId);
   }

   public static String getSuuntoAccessTokenIssueDateTime_Active_Person_String() {

      final String personId = getActivePersonId();

      return getPerson_SuuntoAccessTokenIssueDateTime_String(personId);
   }

   public static String getSuuntoRefreshToken_Active_Person_String() {

      final String personId = getActivePersonId();

      return getPerson_SuuntoRefreshToken_String(personId);
   }

   public static String getSuuntoWorkoutDownloadFolder_Active_Person_String() {

      final String personId = getActivePersonId();

      return getPerson_SuuntoWorkoutDownloadFolder_String(personId);
   }

   public static String getSuuntoWorkoutFilterSinceDate_Active_Person_String() {

      final String personId = getActivePersonId();

      return getPerson_SuuntoWorkoutFilterSinceDate_String(personId);
   }
}
