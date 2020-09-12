/*******************************************************************************
 * Copyright (C) 2020 Wolfgang Schramm and Contributors
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
package net.tourbook.statistic;

import org.eclipse.jface.viewers.ISelection;

/**
 * Contains tours which has been deleted in the database or removed from a view (tours in the import
 * view which are not saved but removed)
 */
public class Selection_StatisticValues implements ISelection {

   String statisticValuesRaw;

   public Selection_StatisticValues(final String statisticValuesRaw) {

      super();

      this.statisticValuesRaw = statisticValuesRaw;
   }

   @Override
   public boolean isEmpty() {
      return false;
   }

   @Override
   public String toString() {

      return "Selection_StatisticValues\n" //$NON-NLS-1$
            + "[\n" //$NON-NLS-1$
            + "statisticValues=" + statisticValuesRaw + "\n" //$NON-NLS-1$ //$NON-NLS-2$
            + "]"; //$NON-NLS-1$
   }

}
