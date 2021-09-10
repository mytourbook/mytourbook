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
package net.tourbook.device.garmin;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

   private static final String BUNDLE_NAME = "net.tourbook.device.garmin.messages"; //$NON-NLS-1$

   public static String        Garmin_commuication_error;
   public static String        Garmin_data_transfer_error;
   public static String        Garmin_error_receiving_data;
   public static String        Garmin_no_connection;

   public static String        Garmin_SAXHandler_InvalidDate_2007_04_01;

   public static String        Garmin_Transfer_msg;
   public static String        Garmin_unknown_device;

   public static String        PrefPage_TCX_Check_IgnoreSpeedValues;

   public static String        PrefPage_TCX_Label_IgnoreSpeedValues;
   public static String        prefPage_tcx_check_importIntoDescriptionField;
   public static String        prefPage_tcx_check_importIntoTitleField;
   public static String        prefPage_tcx_group_importNotes;
   public static String        prefPage_tcx_label_importNotes;
   public static String        prefPage_tcx_radio_importIntoTitleAll;
   public static String        prefPage_tcx_radio_importIntoTitleTruncated;

   static {
      // initialize resource bundle
      NLS.initializeMessages(BUNDLE_NAME, Messages.class);
   }

   private Messages() {}
}
