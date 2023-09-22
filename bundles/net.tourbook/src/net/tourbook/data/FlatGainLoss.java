/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.data;

import net.tourbook.common.UI;

public class FlatGainLoss {

   private static final String NL = UI.NEW_LINE1;

   public int                  timeFlat;
   public int                  timeGain;
   public int                  timeLoss;

   public float                distanceFlat;
   public float                distanceGain;
   public float                distanceLoss;

   public float                elevationGain;
   public float                elevationLoss;

   public FlatGainLoss() {}

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "ElevationGainLoss" + NL //                      //$NON-NLS-1$

            + "[" + NL //                                      //$NON-NLS-1$

            + "  elevationGain = " + elevationGain + NL //     //$NON-NLS-1$
            + "  elevationLoss = " + elevationLoss + NL //     //$NON-NLS-1$

            + "]"; //$NON-NLS-1$
   }

}
