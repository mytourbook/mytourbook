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

   public static String        Icon_Check;
   public static String        Icon_Hourglass;

   static {
      NLS.initializeMessages(BUNDLE_NAME, Messages.class);
   }

   private Messages() {}
}
