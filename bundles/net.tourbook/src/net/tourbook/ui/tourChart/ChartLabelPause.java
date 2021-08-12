/*******************************************************************************
 * Copyright (C) 2021 Frédéric Bard
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

public class ChartLabelPause extends ChartLabel {

   public String pauseDuration = UI.EMPTY_STRING;

   /*
    * Painted label positions
    */
   public int devXPause;
   public int devYPause;

   ChartLabelPause() {}

   @Override
   public String toString() {
      return "ChartLabel [" // //$NON-NLS-1$
//				+ ("serieIndex=" + serieIndex + ", ")
//				+ ("graphX=" + graphX + ", ")
            + ("pauseDuration=" + pauseDuration) //$NON-NLS-1$
            + "]"; //$NON-NLS-1$
   }

}
