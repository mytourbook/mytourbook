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

   private long   _pausedTime_Start;
   private long   _pausedTime_End;

   private String timeZoneId;

   /*
    * Painted label positions
    */
   public int devXPause;
   public int devYPause;

   ChartLabelPause() {}

   public long getPausedTime_End() {
      return _pausedTime_End;
   }

   public long getPausedTime_Start() {
      return _pausedTime_Start;
   }

   public String getPauseDuration() {
      return UI.format_hh_mm_ss(Math.round((_pausedTime_End - _pausedTime_Start) / 1000f));
   }

   public String getTimeZoneId() {
      return timeZoneId;
   }

   public void setPausedTime_End(final long pausedTime_End) {
      _pausedTime_End = pausedTime_End;
   }

   public void setPausedTime_Start(final long pausedTime_Start) {
      _pausedTime_Start = pausedTime_Start;
   }

   public void setTimeZoneId(final String timeZoneId) {
      this.timeZoneId = timeZoneId;
   }

   @Override
   public String toString() {
      return "ChartLabel [" // //$NON-NLS-1$
//				+ ("serieIndex=" + serieIndex + ", ")
//				+ ("graphX=" + graphX + ", ")
            + ("pauseDuration=" + getPauseDuration()) //$NON-NLS-1$
            + "]"; //$NON-NLS-1$
   }

}
