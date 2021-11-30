/*******************************************************************************
 * Copyright (C) 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.sensors;

import java.util.HashMap;
import java.util.Map;

import net.tourbook.common.Messages;

/**
 * Copied from com.garmin.fit.BatteryStatus, it cannot be imported because it would create a cyclic
 * dependency
 */
public class BatteryStatus {

   public static final short               NEW              = 1;
   public static final short               GOOD             = 2;
   public static final short               OK               = 3;
   public static final short               LOW              = 4;
   public static final short               CRITICAL         = 5;
   public static final short               CHARGING         = 6;
   public static final short               UNKNOWN          = 7;

   private static final Map<Short, String> _allStatusLabels = new HashMap<>();

   static {

      _allStatusLabels.put(NEW, Messages.Battery_Status_NEW);
      _allStatusLabels.put(GOOD, Messages.Battery_Status_GOOD);
      _allStatusLabels.put(OK, Messages.Battery_Status_OK);
      _allStatusLabels.put(LOW, Messages.Battery_Status_LOW);
      _allStatusLabels.put(CRITICAL, Messages.Battery_Status_CRITICAL);
      _allStatusLabels.put(CHARGING, Messages.Battery_Status_CHARGING);
      _allStatusLabels.put(UNKNOWN, Messages.Battery_Status_UNKNOWN);
   }

   /**
    * Retrieves the String Representation of the Value
    *
    * @return The string representation of the value, or empty if unknown
    */
   public static String getLabelFromValue(final Short value) {

      if (_allStatusLabels.containsKey(value)) {
         return _allStatusLabels.get(value);
      }

      return Messages.Battery_Status_UNKNOWN;
   }
}
