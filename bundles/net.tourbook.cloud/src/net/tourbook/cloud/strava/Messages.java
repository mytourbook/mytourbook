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
package net.tourbook.cloud.strava;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

   private static final String BUNDLE_NAME = "net.tourbook.cloud.strava.messages"; //$NON-NLS-1$

   public static String        Dialog_StravaUpload_Summary;
   public static String        Dialog_StravaUpload_Message;

   public static String        Image__Connect_With_Strava;

   public static String        Log_UploadToursToStrava_001_Start;
   public static String        Log_UploadToursToStrava_002_NoTimeDataSeries;
   public static String        Log_UploadToursToStrava_003_UploadStatus;
   public static String        Log_UploadToursToStrava_004_UploadError;
   public static String        Log_UploadToursToStrava_005_End;

   public static String        VendorName_Strava;
   public static String        PrefPage_Account_Information_AthleteName_Label;
   public static String        PrefPage_Account_Information_AthleteWebPage_Label;
   public static String        PrefPage_Account_Information_AccessToken_Label;
   public static String        PrefPage_Account_Information_RefreshToken_Label;
   public static String        PrefPage_Account_Information_ExpiresAt_Label;

   public static String        UploadToursToStrava_Task;
   public static String        UploadToursToStrava_SubTask;
   public static String        UploadToursToStrava_Icon_Hourglass;
   public static String        UploadToursToStrava_Icon_Check;

   static {
      NLS.initializeMessages(BUNDLE_NAME, Messages.class);
   }

   private Messages() {}
}
