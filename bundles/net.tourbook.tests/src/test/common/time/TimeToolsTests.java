/*******************************************************************************
 * Copyright (C) 2022, 2023 Frédéric Bard
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
package common.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import net.tourbook.common.time.TimeTools;
import net.tourbook.common.time.TimeZoneData;
import net.tourbook.common.time.TourDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TimeTools should")
public class TimeToolsTests {

   private double eiffelTowerLat = 48.858093;
   private double eiffelTowerLon = 2.294694;

   private String parisZoneId    = "Europe/Paris"; //$NON-NLS-1$

   @Test
   @DisplayName("Create Date Time from a string")
   void testCreateDateTimeFromYMDhms() {

      final ZonedDateTime testZonedDateTime = TimeTools.createDateTimeFromYMDhms(20220516165348L);
      final ZonedDateTime controlZonedDateTime = ZonedDateTime.of(
            2022,
            05,
            16,
            16,
            53,
            48,
            0,
            TimeTools.getDefaultTimeZone());

      assertEquals(controlZonedDateTime, testZonedDateTime);
   }

   @Test
   void testCreatedNowAsYMDhms() {

      assertNotNull(TimeTools.createdNowAsYMDhms());
   }

   @Test
   void testCreateTourDateTime() {

      final String chileZoneId = "Chile/Continental"; //$NON-NLS-1$
      final ZonedDateTime controlZonedDateTime = ZonedDateTime.of(
            2022,
            05,
            16,
            17,
            46,
            10,
            0,
            ZoneId.of(chileZoneId));

      final TourDateTime controlTourDateTime = new TourDateTime(controlZonedDateTime);

      assertEquals(controlTourDateTime, TimeTools.createTourDateTime(1652737570000L, chileZoneId));
   }

   @Test
   void testCreateYMDhms_From_DateTime() {

      final long testDateTime = TimeTools.createYMDhms_From_DateTime(
            ZonedDateTime.of(2022, 05, 16, 16, 53, 48, 0, TimeTools.getDefaultTimeZone()));

      assertEquals(20220516165348L, testDateTime);
   }

   @Test
   void testDetermineSunriseTimes() {

      final ZonedDateTime controlZonedDateTime = ZonedDateTime.of(
            2022,
            05,
            16,
            6,
            8,
            4,
            0,
            ZoneId.of(parisZoneId));

      final ZonedDateTime testZonedDateTime = ZonedDateTime.of(
            2022,
            05,
            16,
            16,
            53,
            48,
            0,
            ZoneId.of(parisZoneId));

      assertEquals(controlZonedDateTime,
            TimeTools.determineSunriseTimes(testZonedDateTime, eiffelTowerLat, eiffelTowerLon));
   }

   @Test
   void testDetermineSunsetTimes() {

      final ZonedDateTime controlZonedDateTime = ZonedDateTime.of(
            2022,
            05,
            16,
            21,
            27,
            28,
            0,
            ZoneId.of(parisZoneId));

      final ZonedDateTime testZonedDateTime = ZonedDateTime.of(
            2022,
            05,
            16,
            16,
            53,
            48,
            0,
            ZoneId.of(parisZoneId));

      assertEquals(controlZonedDateTime,
            TimeTools.determineSunsetTimes(testZonedDateTime, eiffelTowerLat, eiffelTowerLon));
   }

   @Test
   void testGetDefaultTimeZoneOffset() {

      assertEquals("0 m", TimeTools.getDefaultTimeZoneOffset()); //$NON-NLS-1$
   }

   @Test
   void testGetFirstDayOfWeek() {

      assertEquals(DayOfWeek.MONDAY, TimeTools.getFirstDayOfWeek());
   }

   @Test
   void testGetNumberOfDaysWithYear() {

      //2022 is not a leap year
      assertEquals(365, TimeTools.getNumberOfDaysWithYear(2022));
      //2020 is a leap year
      assertEquals(366, TimeTools.getNumberOfDaysWithYear(2020));
   }

   @Test
   void testGetNumberOfWeeksWithYear() {

      assertEquals(52, TimeTools.getNumberOfWeeksWithYear(2022));
      assertEquals(53, TimeTools.getNumberOfWeeksWithYear(2020));
   }

   @Test
   void testGetTimeZone_ByIndex() {

      final TimeZoneData controlTimeZoneData = new TimeZoneData();

      controlTimeZoneData.label = "+00:00    +00:00    Africa/Abidjan"; //$NON-NLS-1$
      controlTimeZoneData.zoneId = ZoneId.of("Africa/Abidjan").getId(); //$NON-NLS-1$
      controlTimeZoneData.zoneOffsetSeconds = 0;

      final TimeZoneData testTimeZoneData = TimeTools.getTimeZone_ByIndex(-1);

      assertEquals(controlTimeZoneData.label, testTimeZoneData.label);
      assertEquals(controlTimeZoneData.zoneId, testTimeZoneData.zoneId);
      assertEquals(controlTimeZoneData.zoneOffsetSeconds, testTimeZoneData.zoneOffsetSeconds);
   }

   @Test
   void testToEpochMilli_LocalDateTime() {

      final LocalDateTime localDateTime = LocalDateTime.of(
            2022,
            05,
            16,
            8,
            0);

      assertEquals(1652688000000L, TimeTools.toEpochMilli(localDateTime));
   }

   @Test
   void testToEpochMilli_ZonedDateTime() {

      final ZonedDateTime testZonedDateTime = ZonedDateTime.of(
            2022,
            05,
            16,
            8,
            0,
            0,
            0,
            TimeTools.getDefaultTimeZone());

      assertEquals(1652688000000L, TimeTools.toEpochMilli(testZonedDateTime));
   }

   @Test
   void testToLocalDate() {

      final LocalDate testLocalDate = LocalDate.of(2022, 05, 16);

      assertEquals(testLocalDate, TimeTools.toLocalDate(1652688000000L));
   }

   @Test
   void testToLocalDateTime() {

      final LocalDateTime testLocalDateTime = LocalDateTime.of(
            2022,
            05,
            16,
            8,
            0);

      assertEquals(testLocalDateTime, TimeTools.toLocalDateTime(1652688000000L));
   }
}
