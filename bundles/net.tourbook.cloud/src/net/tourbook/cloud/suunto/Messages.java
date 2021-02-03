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
package net.tourbook.cloud.suunto;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

   private static final String BUNDLE_NAME = "net.tourbook.cloud.suunto.messages";  //$NON-NLS-1$

   public static String        Dialog_WorkoutsDownload_Message;
   public static String        Dialog_WorkoutsDownload_Summary;
   public static String        Dialog_SuuntoRoutesUpload_Message;
   public static String        Dialog_SuuntoUpload_Summary;
   public static String        DownloadWorkoutsFromSuunto_Task;
   public static String        DownloadWorkoutsFromSuunto_SubTask;
   public static String        Image__SuuntoApp_Icon;
   public static String        Log_UploadToursToSuunto_001_Start;
   public static String        Log_UploadToursToSuunto_002_NoGpsCoordinate;
   public static String        Log_UploadToursToSuunto_003_UploadStatus;
   public static String        Log_UploadToursToSuunto_004_UploadError;
   public static String        Log_DownloadWorkoutsToSuunto_001_Start;
   public static String        Log_DownloadWorkoutsToSuunto_002_NewWorkoutsNotFound;
   public static String        Log_DownloadWorkoutsToSuunto_003_NoSpecifiedFolder;
   public static String        Log_DownloadWorkoutsToSuunto_004_DownloadStatus;
   public static String        Log_DownloadWorkoutsToSuunto_005_FileAlreadyExists;
   public static String        Log_DownloadWorkoutsToSuunto_006_Error;

   public static String        Pref_AccountInformation_SuuntoApp_WebPage_Link;
   public static String        Pref_Checkbox_Use_SinceDateFilter;
   public static String        Pref_Checkbox_Use_SinceDateFilter_Tooltip;
   public static String        Pref_Combo_Workouts_FolderPath_Combo_Tooltip;

   public static String        Suunto_Workouts_Description;
   public static String        UploadToursToSuunto_Task;
   public static String        UploadToursToSuunto_SubTask;
   public static String        VendorName_Suunto_Routes;
   public static String        VendorName_Suunto_Workouts;

   static {
      NLS.initializeMessages(BUNDLE_NAME, Messages.class);
   }

   private Messages() {}
}
