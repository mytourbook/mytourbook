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
package common.time;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.ZonedDateTime;

import net.tourbook.common.time.TimeTools;

import org.junit.jupiter.api.Test;

public class TimeToolsTests {

   @Test
   void testCreateDateTimeFromYMDhms() {

      final ZonedDateTime testZonedDateTime = TimeTools.createDateTimeFromYMDhms(20220516165348L);
      final ZonedDateTime controlZonedDateTime = ZonedDateTime.of(2022, 05, 16, 16, 53, 48, 0, TimeTools.getDefaultTimeZone());

      assertEquals(controlZonedDateTime, testZonedDateTime);
   }

   @Test
   void testCreateYMDhms_From_DateTime() {

      final long testDateTime = TimeTools.createYMDhms_From_DateTime(
            ZonedDateTime.of(2022, 05, 16, 16, 53, 48, 0, TimeTools.getDefaultTimeZone()));

      assertEquals(20220516165348L, testDateTime);
   }
}
