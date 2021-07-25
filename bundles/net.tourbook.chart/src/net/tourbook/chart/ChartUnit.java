/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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
/**
 * @author Wolfgang Schramm
 *
 * 06.07.2005
 */
package net.tourbook.chart;

import net.tourbook.common.UI;

public class ChartUnit {

   public double value;

   public String valueLabel;

   boolean       isMajorValue = false;

   public ChartUnit(final double value, final String valueLabel) {

      this.value = value;
      this.valueLabel = valueLabel;
   }

   public ChartUnit(final double value, final String valueLabel, final boolean isMajorValue) {

      this.value = value;
      this.valueLabel = valueLabel;
      this.isMajorValue = isMajorValue;
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "\tChartUnit" //                           //$NON-NLS-1$

            + " value=" + (long) value + ", " //         //$NON-NLS-1$ //$NON-NLS-2$
            + "\tvalueLabel=" + valueLabel + ", " //     //$NON-NLS-1$ //$NON-NLS-2$
            + "\tisMajorValue=" + isMajorValue //        //$NON-NLS-1$

      ;
   }
}
