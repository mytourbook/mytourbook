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

public interface IValueFormatter {

   static final String BIGGER_THAN_ZERO         = ">0";      //$NON-NLS-1$

   static final String ZERO_VALUE_TEXT_0        = "0";       //$NON-NLS-1$
   static final String ZERO_VALUE_TEXT_0_0      = "0.0";     //$NON-NLS-1$
   static final String ZERO_VALUE_TEXT_0_00     = "0.00";    //$NON-NLS-1$
   static final String ZERO_VALUE_TEXT_0_000    = "0.000";   //$NON-NLS-1$

   static final String ZERO_VALUE_TEXT_HH_MM    = "0:00";    //$NON-NLS-1$
   static final String ZERO_VALUE_TEXT_HH_MM_SS = "0:00:00"; //$NON-NLS-1$

   /**
    * @param value
    * @return
    */
   String printDouble(double value);

   /**
    * Formats the long values to a string. The formatted value is an empty string when it is 0 and
    * ">0" is displayed when the formatted text is "0" but the value is larger than 0.
    *
    * @param value
    * @return
    */
   String printLong(long value);

   /**
    * @param value
    * @param isHide0Value
    *           When <code>false</code> then a "0" value is displayed
    * @param isShowBiggerThan0
    *           When <code>false</code> then ">0" is displayed when the formatted text is "0" but
    *           the values larger than 0.
    * @return
    */
   String printLong(long value, boolean isHide0Value, boolean isShowBiggerThan0);

}
