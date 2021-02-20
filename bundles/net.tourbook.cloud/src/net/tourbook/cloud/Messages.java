/*******************************************************************************
 * Copyright (C) 2020, 2021 Frédéric Bard
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
package net.tourbook.cloud;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

   private static final String BUNDLE_NAME = "net.tourbook.cloud.messages";   //$NON-NLS-1$

   public static String        Icon__Check;
   public static String        Icon__Hourglass;

   public static String        Html_CloseBrowser_Text;
   public static String        Log_CloudAction_End;
   public static String        Log_CloudAction_InvalidTokens;

   public static String        Pref_CloudConnectivity_AccessToken_Label;
   public static String        Pref_CloudConnectivity_Authorize_Button;
   public static String        Pref_CloudConnectivity_CloudAccount_Group;
   public static String        Pref_CloudConnectivity_ExpiresAt_Label;
   public static String        Pref_CloudConnectivity_RefreshToken_Label;
   public static String        Pref_CloudConnectivity_UnavailablePort_Message;
   public static String        Pref_CloudConnectivity_UnavailablePort_Title;
   public static String        Pref_CloudConnectivity_WebPage_Label;

   //DROPBOX
   public static String Image__Dropbox_File;
   public static String Image__Dropbox_Folder;
   public static String Image__Dropbox_Logo;
   public static String Image__Dropbox_Parentfolder;

   public static String Dialog_DropboxBrowser_Button_ParentFolder_Tooltip;
   public static String Dialog_DropboxBrowser_Button_SelectFolder;
   public static String Dialog_DropboxBrowser_Text_AbsolutePath_Tooltip;
   public static String Dialog_DropboxFolderChooser_Area_Text;
   public static String Dialog_DropboxFolderChooser_Area_Title;
   public static String Pref_CloudConnectivity_Dropbox_AccessToken_Tooltip;
   public static String Pref_CloudConnectivity_Dropbox_WebPage_Link;

   //SUUNTO
   public static String Image__SuuntoApp_Icon;

   public static String Dialog_UploadRoutes_Message;
   public static String Dialog_UploadRoutes_SubTask;
   public static String Dialog_UploadRoutes_Task;
   public static String Dialog_UploadRoutes_Title;
   public static String Dialog_DownloadWorkouts_Message;
   public static String Dialog_DownloadWorkouts_SubTask;
   public static String Dialog_DownloadWorkouts_Task;
   public static String Dialog_DownloadWorkouts_Title;
   public static String Log_DownloadWorkoutsToSuunto_001_Start;
   public static String Log_DownloadWorkoutsToSuunto_002_NewWorkoutsNotFound;
   public static String Log_DownloadWorkoutsToSuunto_003_AllWorkoutsAlreadyExist;
   public static String Log_DownloadWorkoutsToSuunto_004_NoSpecifiedFolder;
   public static String Log_DownloadWorkoutsToSuunto_005_DownloadStatus;
   public static String Log_DownloadWorkoutsToSuunto_006_FileAlreadyExists;
   public static String Log_DownloadWorkoutsToSuunto_007_Error;
   public static String Log_UploadToursToSuunto_001_Start;
   public static String Log_UploadToursToSuunto_002_NoGpsCoordinate;
   public static String Log_UploadToursToSuunto_003_UploadStatus;
   public static String Log_UploadToursToSuunto_004_UploadError;
   public static String Pref_AccountInformation_SuuntoApp_WebPage_Link;
   public static String Pref_Checkbox_Use_SinceDateFilter;
   public static String Pref_Checkbox_Use_SinceDateFilter_Tooltip;
   public static String Pref_Combo_Workouts_FolderPath_Combo_Tooltip;
   public static String Pref_Combo_Workouts_Label_FolderPath;
   public static String Suunto_Workouts_Description;
   public static String ValidatingSuuntoTokens_SubTask;
   public static String VendorName_Suunto_Routes;
   public static String VendorName_Suunto_Workouts;

   //STRAVA
   public static String Image__Connect_With_Strava;

   public static String Dialog_UploadTours_Message;
   public static String Dialog_UploadTours_SubTask;
   public static String Dialog_UploadTours_Task;
   public static String Dialog_UploadTours_Title;
   public static String Log_UploadToursToStrava_001_Start;
   public static String Log_UploadToursToStrava_002_NoTourTitle;
   public static String Log_UploadToursToStrava_003_ActivityLink;
   public static String Log_UploadToursToStrava_003_UploadStatus;
   public static String Log_UploadToursToStrava_004_UploadError;
   public static String PrefPage_Account_Information_Label_AthleteName;
   public static String PrefPage_Account_Information_Label_AthleteWebPage;
   public static String VendorName_Strava;

   static {
      NLS.initializeMessages(BUNDLE_NAME, Messages.class);
   }

   private Messages() {}
}
