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
package net.tourbook.device.suunto;

import org.eclipse.osgi.util.NLS;

class Messages extends NLS {

   private static final String BUNDLE_NAME = "net.tourbook.device.suunto.messages"; //$NON-NLS-1$

   static String               pref_data_source;
   static String               pref_altitude_source;
   static String               pref_altitude_gps;
   static String               pref_altitude_barometer;
   static String               pref_distance_source;
   static String               pref_distance_gps;
   static String               pref_distance_providedvalues;

   static {
      // initialize resource bundle
      NLS.initializeMessages(BUNDLE_NAME, Messages.class);
   }

   private Messages() {}
}
