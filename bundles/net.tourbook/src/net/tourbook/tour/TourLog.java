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
package net.tourbook.tour;

import java.util.concurrent.atomic.AtomicInteger;

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;

class TourLog {

   private static final char    NL          = UI.NEW_LINE;

   private static AtomicInteger _logCounter = new AtomicInteger();

   public int                   logNumber;
   public String                time;
   public String                threadName;

   public TourLogState          state;
   public String                message;

   public boolean               isSubLogItem;

   public String                css;

   public TourLog(final TourLogState state, final String message) {

      this.time = TimeTools.now().format(TimeTools.Formatter_Time_ISO);

      this.threadName = Thread.currentThread().getName() + UI.DASH_WITH_SPACE + Thread.currentThread().getId();

      this.state = state;
      this.message = message;

      this.logNumber = _logCounter.incrementAndGet();
   }

   public static void clear() {

      _logCounter.set(0);
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "TourLog [" + NL //                        //$NON-NLS-1$

            + "threadName   = " + threadName + NL //     //$NON-NLS-1$
            + "time         = " + time + NL //           //$NON-NLS-1$
            + "state        = " + state + NL //          //$NON-NLS-1$
            + "isSubLogItem = " + isSubLogItem + NL //   //$NON-NLS-1$
            + "css          = " + css + NL //            //$NON-NLS-1$
            + "message      = " + message + NL //        //$NON-NLS-1$

            + "]"; //                                    //$NON-NLS-1$
   }

}
