/*******************************************************************************
 * Copyright (C) 2020, 2022 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.tourChart;

import net.tourbook.common.UI;

public class HoveredValueData {

   private static final char NL = UI.NEW_LINE;

   public Long               tourId;
   public int                hoveredTourSerieIndex;

   /**
    * @param tourId
    *           Id for a single real tour and not for a multiple tour
    * @param hoveredTourSerieIndex
    *           Index into the tour data series
    */
   public HoveredValueData(final Long tourId, final int hoveredTourSerieIndex) {

      this.tourId = tourId;
      this.hoveredTourSerieIndex = hoveredTourSerieIndex;
   }

   @Override
   public String toString() {

      return "HoveredValueData" + NL //$NON-NLS-1$

            + "[" + NL//$NON-NLS-1$

            + "tourId                  = " + tourId + NL//$NON-NLS-1$
            + "hoveredTourSerieIndex   = " + hoveredTourSerieIndex + NL //$NON-NLS-1$

            + "]"; //$NON-NLS-1$
   }

}
