/*******************************************************************************
 * Copyright (C) 2005, 2026 Wolfgang Schramm and Contributors
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

public class TourSegment {

   private static final char NL = UI.NEW_LINE;

   public int                sequence;

   public int                serieIndex_Start;
   public int                serieIndex_End;

   public int                deviceTime_Elapsed;
   public int                deviceTime_Recorded;
   public int                deviceTime_Paused;
   public int                computedTime_Moving;
   public int                computedTime_Break;

   public int                time_Total;

   public float              distance_Diff;
   public float              distance_Total;

   public float              altitude_Segment_Up;
   public float              altitude_Segment_Down;
   public float              altitude_Segment_Border_Diff;
   public float              altitude_Segment_Computed_Diff;

   public float              altitude_Summarized_Border_Up;
   public float              altitude_Summarized_Border_Down;
   public float              altitude_Summarized_Computed_Up;
   public float              altitude_Summarized_Computed_Down;

   public float              elevationGain_InBreakTime;
   public float              elevationLoss_InBreakTime;

   public float              cadence;
   public float              gradient;
   public float              power;
   public float              speed;
   public float              strideLength;

   public float              pace;
   public float              pace_Diff;
   public float              pulse;
   public float              pulse_Diff;

   public int                filter;

   /** Is <code>true</code> when this segment is the totals segment. */
   public boolean            isTotal;

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "TourSegment" + NL //                                                                   //$NON-NLS-1$

            + " sequence                  = " + sequence + NL //                                      //$NON-NLS-1$
            + " serieIndex_Start          = " + serieIndex_Start + NL //                              //$NON-NLS-1$
            + " serieIndex_End            = " + serieIndex_End + NL //                                //$NON-NLS-1$

            + " elevationGain_InBreakTime = " + elevationGain_InBreakTime + NL //                     //$NON-NLS-1$
            + " elevationLoss_InBreakTime = " + elevationLoss_InBreakTime + NL //                     //$NON-NLS-1$

//            + " deviceTime_Elapsed     = " + deviceTime_Elapsed + NL //                               //$NON-NLS-1$
//            + " deviceTime_Recorded    = " + deviceTime_Recorded + NL //                              //$NON-NLS-1$
//            + " deviceTime_Paused      = " + deviceTime_Paused + NL //                                //$NON-NLS-1$
//            + " computedTime_Moving    = " + computedTime_Moving + NL //                              //$NON-NLS-1$
//            + " computedTime_Break     = " + computedTime_Break + NL //                               //$NON-NLS-1$
//            + " time_Total             = " + time_Total + NL //                                       //$NON-NLS-1$
//            + " distance_Diff          = " + distance_Diff + NL //                                    //$NON-NLS-1$
//            + " distance_Total         = " + distance_Total + NL //                                   //$NON-NLS-1$
//            + " cadence                = " + cadence + NL //                                          //$NON-NLS-1$
//            + " gradient               = " + gradient + NL //                                         //$NON-NLS-1$
//            + " power                  = " + power + NL //                                            //$NON-NLS-1$
//            + " speed                  = " + speed + NL //                                            //$NON-NLS-1$
//            + " strideLength           = " + strideLength + NL //                                     //$NON-NLS-1$
//            + " pace                   = " + pace + NL //                                             //$NON-NLS-1$
//            + " pace_Diff              = " + pace_Diff + NL //                                        //$NON-NLS-1$
//            + " pulse                  = " + pulse + NL //                                            //$NON-NLS-1$
//            + " pulse_Diff             = " + pulse_Diff + NL //                                       //$NON-NLS-1$
//            + " filter                 = " + filter + NL //                                           //$NON-NLS-1$
//            + " isTotal                = " + isTotal + NL //                                          //$NON-NLS-1$
//            + " altitude_Segment_Up                 = " + altitude_Segment_Up + NL //                 //$NON-NLS-1$
//            + " altitude_Segment_Down               = " + altitude_Segment_Down + NL //               //$NON-NLS-1$
//            + " altitude_Segment_Border_Diff        = " + altitude_Segment_Border_Diff + NL //        //$NON-NLS-1$
//            + " altitude_Segment_Computed_Diff      = " + altitude_Segment_Computed_Diff + NL //      //$NON-NLS-1$
//            + " altitude_Summarized_Border_Up       = " + altitude_Summarized_Border_Up + NL //       //$NON-NLS-1$
//            + " altitude_Summarized_Border_Down     = " + altitude_Summarized_Border_Down + NL //     //$NON-NLS-1$
//            + " altitude_Summarized_Computed_Up     = " + altitude_Summarized_Computed_Up + NL //     //$NON-NLS-1$
//            + " altitude_Summarized_Computed_Down   = " + altitude_Summarized_Computed_Down + NL //   //$NON-NLS-1$
      ;
   }

}
