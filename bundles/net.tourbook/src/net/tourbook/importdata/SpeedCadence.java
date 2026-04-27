/*******************************************************************************
 * Copyright (C) 2026 Wolfgang Schramm and Contributors
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
package net.tourbook.importdata;

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.tour.CadenceMultiplier;

public class SpeedCadence implements Comparable<Object>, Cloneable {

   private static final char NL                = UI.NEW_LINE;

   /**
    * Average speed for this cadence in km/h
    */
   public float              avgSpeed;

   public CadenceMultiplier  cadenceMultiplier = RawDataManager.getCadenceMultiplierDefaultValue();

   public SpeedCadence() {}

   public SpeedCadence(final int avgSpeed, final CadenceMultiplier cadenceMultiplier) {

      this.avgSpeed = avgSpeed;
      this.cadenceMultiplier = cadenceMultiplier;
   }

   @Override
   public SpeedCadence clone() {

      SpeedCadence clonedObject = null;

      try {

         clonedObject = (SpeedCadence) super.clone();

      } catch (final CloneNotSupportedException e) {
         StatusUtil.log(e);
      }

      return clonedObject;
   }

   @Override
   public int compareTo(final Object anotherObject) throws ClassCastException {

      final float anotherValue = ((SpeedCadence) anotherObject).avgSpeed;

      return Float.compare(avgSpeed, anotherValue);
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "SpeedCadence" + NL //                                    //$NON-NLS-1$

            + " avgSpeed				= " + avgSpeed + NL //              //$NON-NLS-1$
            + " cadenceMultiplier	= " + cadenceMultiplier + NL //     //$NON-NLS-1$
      ;
   }

}
