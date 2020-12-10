/*******************************************************************************
 * Copyright (C) 2005, 2020 Wolfgang Schramm and Contributors
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

import java.text.NumberFormat;

import net.tourbook.common.Messages;

public class ValueFormatter_Number_1_2 implements IValueFormatter {

   private final static NumberFormat _nf2 = NumberFormat.getNumberInstance();

   public ValueFormatter_Number_1_2() {

      _nf2.setMinimumFractionDigits(2);
      _nf2.setMaximumFractionDigits(2);
   }

   public ValueFormatter_Number_1_2(final boolean isGroupingUsed) {

      this();

      _nf2.setGroupingUsed(isGroupingUsed);
   }
   @Override
   public String printDouble(final double value) {

      final String formattedValue = _nf2.format(value);

      if (value > 0 && ZERO_VALUE_TEXT_0_00.equals(formattedValue)) {
         return BIGGER_THAN_ZERO;
      }

      return formattedValue;
   }

   @Override
   public String printLong(final long value) {
      return Messages.App_Error_NotSupportedValueFormatter;
   }

   @Override
   public String printLong(final long value, final boolean isHide0Value, final boolean isShowBiggerThan0) {
      return printLong(value);
   }

   @Override
   public String toString() {
      return "ValueFormatter_Number_1_2 [" // //$NON-NLS-1$
            + "printDouble()" //$NON-NLS-1$
            + "]"; //$NON-NLS-1$
   }

}
