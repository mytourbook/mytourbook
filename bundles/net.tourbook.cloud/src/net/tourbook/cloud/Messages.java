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

   private static final String BUNDLE_NAME = "net.tourbook.cloud.messages";                        //$NON-NLS-1$

   public static String        Html_Text_CloseBrowser;
   public static String        Log_CloudAction_End;
   public static String        Log_CloudAction_InvalidTokens;

   public static String        PrefPage_CloudConnectivity_Label_AccessToken;
   public static String        PrefPage_CloudConnectivity_Button_Authorize;
   public static String        PrefPage_CloudConnectivity_Group_CloudAccount;
   public static String        PrefPage_CloudConnectivity_Label_ExpiresAt;
   public static String        PrefPage_CloudConnectivity_Label_PersonLinkedToCloudAccount;
   public static String        PrefPage_CloudConnectivity_Label_PersonLinkedToCloudAccount_Tooltip;
   public static String        PrefPage_CloudConnectivity_Label_RefreshToken;
   public static String        PrefPage_CloudConnectivity_UnavailablePort_Message;
   public static String        PrefPage_CloudConnectivity_UnavailablePort_Title;
   public static String        PrefPage_CloudConnectivity_Label_WebPage;

   //DROPBOX
   public static String Dialog_DropboxBrowser_Button_ParentFolder_Tooltip;
   public static String Dialog_DropboxBrowser_Button_SelectFolder;
   public static String Dialog_DropboxBrowser_Text_AbsolutePath_Tooltip;
   public static String Dialog_DropboxBrowser_Text;
   public static String Dialog_DropboxBrowser_Title;
   public static String PrefPage_CloudConnectivity_Dropbox_AccessToken_Tooltip;
   public static String PrefPage_CloudConnectivity_Dropbox_WebPage_Link;

   //SUUNTO
   public static String Dialog_DownloadWorkoutsFromSuunto_Message;
   public static String Dialog_DownloadWorkoutsFromSuunto_SubTask;
   public static String Dialog_DownloadWorkoutsFromSuunto_Task;
   public static String Dialog_DownloadWorkoutsFromSuunto_Title;
   public static String Dialog_UploadRoutesToSuunto_Message;
   public static String Dialog_UploadRoutesToSuunto_SubTask;
   public static String Dialog_UploadRoutesToSuunto_Task;
   public static String Dialog_UploadRoutesToSuunto_Title;
   public static String Dialog_ValidatingSuuntoTokens_SubTask;
   public static String Import_Data_HTML_SuuntoWorkoutsDownloader_Tooltip;
   public static String Log_DownloadWorkoutsFromSuunto_001_Start;
   public static String Log_DownloadWorkoutsFromSuunto_002_NewWorkoutsNotFound;
   public static String Log_DownloadWorkoutsFromSuunto_003_AllWorkoutsAlreadyExist;
   public static String Log_DownloadWorkoutsFromSuunto_004_NoSpecifiedFolder;
   public static String Log_DownloadWorkoutsFromSuunto_005_DownloadStatus;
   public static String Log_DownloadWorkoutsFromSuunto_006_FileAlreadyExists;
   public static String Log_DownloadWorkoutsFromSuunto_007_Error;
   public static String Log_UploadRoutesToSuunto_001_Start;
   public static String Log_UploadRoutesToSuunto_002_NoGpsCoordinate;
   public static String Log_UploadRoutesToSuunto_003_UploadStatus;
   public static String Log_UploadRoutesToSuunto_004_UploadError;
   public static String PrefPage_AccountInformation_Link_SuuntoApp_WebPage;
   public static String PrefPage_SuuntoWorkouts_Checkbox_SinceDateFilter;
   public static String PrefPage_SuuntoWorkouts_SinceDateFilter_Tooltip;
   public static String PrefPage_SuuntoWorkouts_Label_FolderPath;
   public static String PrefPage_SuuntoWorkouts_FolderPath_Tooltip;
   public static String VendorName_Suunto_Routes;
   public static String VendorName_Suunto;

   //STRAVA
   public static String Dialog_UploadToursToStrava_Message;
   public static String Dialog_UploadToursToStrava_SubTask;
   public static String Dialog_UploadToursToStrava_Task;
   public static String Dialog_UploadToursToStrava_Title;
   public static String Log_UploadToursToStrava_001_Start;
   public static String Log_UploadToursToStrava_002_NoTourTitle;
   public static String Log_UploadToursToStrava_003_ActivityLink;
   public static String Log_UploadToursToStrava_003_UploadStatus;
   public static String Log_UploadToursToStrava_004_UploadError;
   public static String PrefPage_AccountInformation_Label_AthleteName;
   public static String PrefPage_AccountInformation_Label_AthleteWebPage;
   public static String PrefPage_AccountInformation_Link_Strava_WebPage;
   public static String VendorName_Strava;

   static {
      NLS.initializeMessages(BUNDLE_NAME, Messages.class);
   }

   private Messages() {}
}
