/*******************************************************************************
 * Copyright (C) 2023 Frédéric Bard and Contributors
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
package net.tourbook.ui.views.calendar;

import net.tourbook.common.formatter.FormatManager;
import net.tourbook.common.formatter.ValueFormat;

abstract class TimeDataFormatter extends DataFormatter {

   TimeDataFormatter(final FormatterID id, final String name, final String colorName) {
      super(id, name, colorName);
   }

   @Override
   public ValueFormat getDefaultFormat() {
      return ValueFormat.TIME_HH_MM;
   }

   @Override
   public ValueFormat[] getValueFormats() {

      return new ValueFormat[] {
            ValueFormat.TIME_HH,
            ValueFormat.TIME_HH_MM,
            ValueFormat.TIME_HH_MM_SS };
   }

   @Override
   void setValueFormat(final ValueFormat valueFormat) {

      valueFormatId = valueFormat;
      valueFormatter = FormatManager.getTimeFormatter(valueFormat.name());
   }
}
