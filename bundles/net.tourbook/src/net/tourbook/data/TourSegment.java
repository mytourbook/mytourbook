/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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

public class TourSegment {

   public int     sequence;

   public int     serieIndex_Start;
   public int     serieIndex_End;

   public int     time_Recording;

   public int     time_Driving;
   public int     time_Break;
   public int     time_Total;

   public float   distance_Diff;
   public float   distance_Total;

   public float   altitude_Segment_Up;
   public float   altitude_Segment_Down;
   public float   altitude_Segment_Border_Diff;
   public float   altitude_Segment_Computed_Diff;
   public float   altitude_Summarized_Border_Up;

   public float   altitude_Summarized_Border_Down;
   public float   altitude_Summarized_Computed_Up;
   public float   altitude_Summarized_Computed_Down;

   public float   cadence;
   public float   gradient;
   public float   power;
   public float   speed;
   public float   strideLength;

   public float   pace;
   public float   pace_Diff;
   public float   pulse;
   public float   pulse_Diff;

   public int     filter;

   /** Is <code>true</code> when this segment is the totals segment. */
   public boolean isTotal;

   @Override
   public String toString() {

      return "TourSegment\n" //$NON-NLS-1$

            + "sequence " + sequence + "\n" //$NON-NLS-1$ //$NON-NLS-2$
            + "serieIndex_Start " + serieIndex_Start + "\n" //$NON-NLS-1$ //$NON-NLS-2$
            + "serieIndex_End   " + serieIndex_End + "\n" //$NON-NLS-1$ //$NON-NLS-2$

//            + "time_Recording=" + time_Recording + "\n"
//            + "time_Driving=" + time_Driving + "\n"
//            + "time_Break=" + time_Break + "\n"
//            + "time_Total=" + time_Total + "\n"
//
//            + "distance_Diff=" + distance_Diff + "\n"
//            + "distance_Total=" + distance_Total + "\n"
//
//            + "altitude_Segment_Up=" + altitude_Segment_Up + "\n"
//            + "altitude_Segment_Down=" + altitude_Segment_Down + "\n"
//            + "altitude_Segment_Border_Diff=" + altitude_Segment_Border_Diff + "\n"
//            + "altitude_Segment_Computed_Diff=" + altitude_Segment_Computed_Diff + "\n"
//            + "altitude_Summarized_Border_Up=" + altitude_Summarized_Border_Up + "\n"
//            + "altitude_Summarized_Border_Down=" + altitude_Summarized_Border_Down + "\n"
//            + "altitude_Summarized_Computed_Up=" + altitude_Summarized_Computed_Up + "\n"
//            + "altitude_Summarized_Computed_Down=" + altitude_Summarized_Computed_Down + "\n"
//
//            + "cadence=" + cadence + "\n"
//            + "gradient=" + gradient + "\n"
//            + "power=" + power + "\n"
//            + "speed=" + speed + "\n"
//            + "strideLength=" + strideLength + "\n"
//            + "pace=" + pace + "\n"
//            + "pace_Diff=" + pace_Diff + "\n"
//            + "pulse=" + pulse + "\n"
//            + "pulse_Diff=" + pulse_Diff + "\n"
            + "filter " + filter + "\n" //$NON-NLS-1$ //$NON-NLS-2$
//            + "isTotal=" + isTotal + "\n"

      ;
   }
}
