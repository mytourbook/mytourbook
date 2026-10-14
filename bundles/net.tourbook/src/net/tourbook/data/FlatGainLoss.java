/*******************************************************************************
 * Copyright (C) 2009, 2026 Wolfgang Schramm and Contributors
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

   public float                elevationGain_InBreakTime;
   public float                elevationLoss_InBreakTime;

   public FlatGainLoss() {}

   @Override
   public String toString() {

      final float elevationGain_WithoutBreak = elevationGain - elevationGain_InBreakTime;
      final float elevationLoss_WithoutBreak = elevationLoss - elevationLoss_InBreakTime;

      return UI.EMPTY_STRING

            + "FlatGainLoss" + NL //                                                      //$NON-NLS-1$

            + " timeGain                     = " + timeGain + NL //                       //$NON-NLS-1$
            + " timeLoss                     = " + timeLoss + NL //                       //$NON-NLS-1$
            + " timeFlat                     = " + timeFlat + NL //                       //$NON-NLS-1$

            + " distanceGain                 = " + distanceGain + NL //                   //$NON-NLS-1$
            + " distanceLoss                 = " + distanceLoss + NL //                   //$NON-NLS-1$
            + " distanceFlat                 = " + distanceFlat + NL //                   //$NON-NLS-1$
            + NL

            + " elevationGain                = " + elevationGain + NL //                  //$NON-NLS-1$
            + " elevationGain_InBreakTime    = " + elevationGain_InBreakTime + NL //      //$NON-NLS-1$
            + " gain without break           = " + elevationGain_WithoutBreak + NL //     //$NON-NLS-1$

            + NL
            + " elevationLoss                = " + elevationLoss + NL //                  //$NON-NLS-1$
            + " elevationLoss_InBreakTime    = " + elevationLoss_InBreakTime + NL //      //$NON-NLS-1$
            + " loss without break           = " + elevationLoss_WithoutBreak + NL //     //$NON-NLS-1$

      ;
   }

}
