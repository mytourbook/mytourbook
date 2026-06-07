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

/**
 * This formatter is used when value formatting is provided externally and not in this formatter.
 * It was needed to fix the column manager which was using the default format when a formatter
 * was <code>null</code>
 */
public class ValueFormatter_Dummy implements IValueFormatter {

   /**
    * This should never be displayed, its only defined to find issues when it may be displayed
    */
   private static final String DUMMY_FORMATTER = "Dummy Formatter"; //$NON-NLS-1$

   public ValueFormatter_Dummy() {}

   @Override
   public String printDouble(final double value) {

      return DUMMY_FORMATTER;
   }

   @Override
   public String printLong(final long value) {
      return DUMMY_FORMATTER;
   }

   @Override
   public String printLong(final long value, final boolean isHide0Value, final boolean isShowBiggerThan0) {
      return DUMMY_FORMATTER;
   }

   @Override
   public String toString() {

      return "ValueFormatter_Dummy()"; //$NON-NLS-1$
   }

}
