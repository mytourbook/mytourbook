/*******************************************************************************
 * Copyright (C) 2020 Frédéric Bard and Contributors
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
package net.tourbook.ui;

import org.eclipse.swt.widgets.Combo;

public class ChartUtils {

   /**
    * For a given combo selection of years, it computes and returns a list of
    * years the user can display at any time in a given chart.
    *
    * @param availableYears
    *           A set of available years
    * @param selectedYearItem
    *           The current year selected in a given combo box
    * @return
    *         A list of year durations
    */
   public static String[] produceListOfYearsToBeDisplayed(final Combo availableYears, int selectedYearIndex) {

      if (availableYears == null || availableYears.getItemCount() == 0) {
         return null;
      }

//      if (selectedYearIndex < 0 || selectedYearIndex >= availableYears.getItemCount()) {
//         selectedYearIndex = availableYears.getItemCount() - 1;
//      }

      selectedYearIndex = availableYears.getItemCount() - 1;

      final String[] yearsToDisplayList = new String[selectedYearIndex + 1];

      final int currentYear = Integer.parseInt(availableYears.getItem(selectedYearIndex));

      int arrayIndex = 0;
      for (int index = selectedYearIndex; index >= 0; --index) {

         final int previousYear = Integer.parseInt(availableYears.getItem(index));

         final int yearsDifference = currentYear - previousYear + 1;
         yearsToDisplayList[arrayIndex++] = Integer.toString(yearsDifference);
      }

      return yearsToDisplayList;
   }
}
