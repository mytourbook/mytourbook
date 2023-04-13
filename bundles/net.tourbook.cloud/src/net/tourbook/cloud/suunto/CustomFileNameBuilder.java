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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.cloud.Activator;
import net.tourbook.cloud.Preferences;
import net.tourbook.cloud.suunto.workouts.Payload;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.FileUtils;
import net.tourbook.data.TourPerson;

import org.eclipse.jface.preference.IPreferenceStore;

public class CustomFileNameBuilder {

   private static IPreferenceStore _prefStore =
         Activator.getDefault().getPreferenceStore();

   private static String addLeadingZero(final int number) {
      return String.format("%02d", number); //$NON-NLS-1$
   }

   /**
    * Build the file name to download based on what the user has configured
    *
    * @param workoutPayload
    * @param suuntoFileName
    * @return
    */
   public static String buildCustomizedFileName(final Payload workoutPayload,
                                                final String suuntoFileName) {

      final String suuntoFilenameComponents =
            _prefStore.getString(Preferences.SUUNTO_FILENAME_COMPONENTS);

      final List<String> fileNameComponents = extractFileNameComponents(suuntoFilenameComponents);

      //Replace each component by the appropriate workout data
      final StringBuilder customizedFileName = new StringBuilder();

      for (final String fileNameComponent : fileNameComponents) {

         final PART_TYPE partType = getPartTypeFromComponent(fileNameComponent);

         switch (partType) {

         case SUUNTO_FILE_NAME:

            customizedFileName.append(FileUtils.removeExtensions(suuntoFileName));
            break;

         case FIT_EXTENSION:

            customizedFileName.append(".fit"); //$NON-NLS-1$
            break;

         case WORKOUT_ID:

            customizedFileName.append(workoutPayload.workoutKey);
            break;

         case ACTIVITY_TYPE:

            customizedFileName.append(workoutPayload.getSportNameFromActivityId());
            break;

         case YEAR:

            final int year = getWorkoutOffsetDateTime(workoutPayload).getYear();
            customizedFileName.append(year);
            break;

         case MONTH:

            final int month = getWorkoutOffsetDateTime(workoutPayload).getMonthValue();
            customizedFileName.append(addLeadingZero(month));
            break;

         case DAY:

            final int day = getWorkoutOffsetDateTime(workoutPayload).getDayOfMonth();
            customizedFileName.append(addLeadingZero(day));
            break;

         case HOUR:

            final int hour = getWorkoutOffsetDateTime(workoutPayload).getHour();
            customizedFileName.append(addLeadingZero(hour));
            break;

         case MINUTE:

            final int minute = getWorkoutOffsetDateTime(workoutPayload).getMinute();
            customizedFileName.append(addLeadingZero(minute));
            break;

         case USER_NAME:

            final TourPerson activePerson = TourbookPlugin.getActivePerson();
            final String personName = activePerson == null
                  ? Messages.App_People_item_all
                  : activePerson.getName();
            customizedFileName.append(personName);
            break;

         case USER_TEXT:

            customizedFileName.append(
                  fileNameComponent.substring(
                        fileNameComponent.indexOf(UI.SYMBOL_COLON) + 1));
            break;

         case NONE:
         default:
            break;
         }
      }

      return customizedFileName.toString();
   }

   public static List<String> extractFileNameComponents(final String suuntoFilenameComponents) {

      //This pattern looks for all the substrings in between '{' and '}'
      final Pattern pattern = Pattern.compile("\\{(.*?)\\}"); //$NON-NLS-1$
      final Matcher matcher = pattern.matcher(suuntoFilenameComponents);

      final List<String> fileNameComponents = new ArrayList<>();
      while (matcher.find()) {

         fileNameComponents.add(matcher.group(1));
      }

      return fileNameComponents;
   }

   public static PART_TYPE getPartTypeFromComponent(final String fileNameComponent) {

      final PART_TYPE partType = fileNameComponent.startsWith(
            PART_TYPE.USER_TEXT.toString())
                  ? PART_TYPE.USER_TEXT
                  : PART_TYPE.valueOf(fileNameComponent);
      return partType;
   }

   private static OffsetDateTime getWorkoutOffsetDateTime(final Payload workoutPayload) {

      final OffsetDateTime offsetDateTime = TimeTools
            .getZonedDateTimeWithUTC(workoutPayload.startTime)
            .toOffsetDateTime();

      final ZoneOffset zoneOffset =
            ZoneOffset.ofTotalSeconds(workoutPayload.timeOffsetInMinutes * 60);

      final OffsetDateTime offsetTime =
            offsetDateTime.withOffsetSameInstant(zoneOffset);

      return offsetTime;
   }
}
