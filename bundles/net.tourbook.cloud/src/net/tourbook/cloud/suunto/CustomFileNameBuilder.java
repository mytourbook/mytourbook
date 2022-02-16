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
package net.tourbook.cloud.suunto;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.cloud.Activator;
import net.tourbook.cloud.Preferences;
import net.tourbook.cloud.suunto.workouts.Payload;
import net.tourbook.common.UI;
import net.tourbook.data.TourPerson;

import org.eclipse.jface.preference.IPreferenceStore;

public class CustomFileNameBuilder {

   private static IPreferenceStore _prefStore = Activator.getDefault().getPreferenceStore();

   public static String buildCustomizedFileName(final Payload workoutPayload,
                                                final String suuntoFileName) {
      final String suuntoFilenameComponents = _prefStore.getString(Preferences.SUUNTO_FILENAME_COMPONENTS);

      //todo fb
      //make a function to parse the startime to datetime ?

      //Replace each component by the appropriate workout data
      final StringBuilder customizedFileName = new StringBuilder();
      final Pattern pattern = Pattern.compile("\\{(.*?)\\}"); //$NON-NLS-1$
      final Matcher matcher = pattern.matcher(suuntoFilenameComponents);
      while (matcher.find()) {

         final String currentComponent = matcher.group(1);
         final PART_TYPE partType = PART_TYPE.valueOf(currentComponent);
         switch (partType) {
         case SUUNTO_FILE_NAME:
            customizedFileName.append(suuntoFileName);
            break;
         case FIT_EXTENSION:
            customizedFileName.append(".fit"); //$NON-NLS-1$
            break;
         case WORKOUT_ID:
            customizedFileName.append(workoutPayload.workoutKey);
            break;
         case YEAR:
            customizedFileName.append(suuntoFileName);
            break;
         case MONTH:
            customizedFileName.append(suuntoFileName);
            break;
         case DAY:
            final Instant i = Instant.ofEpochSecond(workoutPayload.startTime);
            final var toto = ZonedDateTime.ofInstant(i, ZoneOffset.UTC);

            final ZoneOffset zoneOffset = ZoneOffset.ofTotalSeconds(workoutPayload.timeOffsetInMinutes * 60);
            final OffsetDateTime timeUtc = toto.toOffsetDateTime(); //18:11:06 UTC
            final OffsetDateTime offsetTime = timeUtc.withOffsetSameInstant(zoneOffset); //21:11:06 +03:00

            customizedFileName.append(suuntoFileName);
            break;
         case USER_NAME:
            final TourPerson activePerson = TourbookPlugin.getActivePerson();
            final String personName = activePerson == null
                  ? Messages.App_People_item_all
                  : activePerson.getName();
            customizedFileName.append(personName);
            break;
         case USER_TEXT:
            customizedFileName.append(currentComponent.substring(currentComponent.indexOf(UI.SYMBOL_COLON)));
            break;
         case NONE:
         default:
            break;
         }
      }

      return customizedFileName.toString();

      //TODO FB get the configured file name strcture from the prefs
      /*
       * Year
       * Month
       * Day
       * Time (if available) but then which timezone !???Well, the timezone is gien in the json, the
       * offset more specifically
       * User text
       * User name
       * file name <= the one by default
       * Workout Id
       * extension (.fit)
       */

   }
}
