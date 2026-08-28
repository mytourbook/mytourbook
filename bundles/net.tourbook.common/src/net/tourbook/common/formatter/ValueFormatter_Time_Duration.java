/*******************************************************************************
 * Copyright (C) 2026 Wolfgang Schramm and Contributors
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
package net.tourbook.common.formatter;

import net.tourbook.common.Messages;
import net.tourbook.common.UI;

import org.joda.time.Period;
import org.joda.time.PeriodType;

public class ValueFormatter_Time_Duration implements IValueFormatter {

   private static PeriodType _usedDurationFields = PeriodType.yearMonthDayTime()

         // exclude these fields that days are cumulating months and years

         .withYearsRemoved()
         .withMonthsRemoved()

         .withMillisRemoved();

   @Override
   public String printDouble(final double value) {
      return Messages.App_Error_NotSupportedValueFormatter;
   }

   @Override
   public String printLong(final long value) {
      return printLong(value, true, true);
   }

   @Override
   public String printLong(final long value, final boolean isHide0Value, final boolean isShowBiggerThan0) {

      if (value == 0 && isHide0Value) {
         return UI.EMPTY_STRING;
      }

      final Period durationPeriod = new Period(0, value * 1000, _usedDurationFields);
      final String formattedDuration = durationPeriod.toString(UI.DURATION_FORMATTER_DDD_HH_MM_SS);

      if (isShowBiggerThan0 && value > 0 && ZERO_VALUE_TEXT_0.equals(formattedDuration)) {
         return BIGGER_THAN_ZERO;
      }

      return formattedDuration;
   }
}
