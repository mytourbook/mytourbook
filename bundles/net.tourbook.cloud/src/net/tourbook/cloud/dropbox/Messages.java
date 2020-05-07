/*******************************************************************************
 * Copyright (C) 2020 Frédéric Bard
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
package net.tourbook.cloud.dropbox;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

   private static final String BUNDLE_NAME = "net.tourbook.cloud.dropbox.messages";       //$NON-NLS-1$

   public static String        Dialog_DropboxFileChooser_Area_Title;
   public static String        Dialog_DropboxFolderChooser_Area_Title;
   public static String        Dialog_DropboxFileChooser_Area_Text;
   public static String        Dialog_DropboxFolderChooser_Area_Text;
   public static String        Dialog_DropboxBrowser_Button_ParentFolder_Tooltip;
   public static String        Dialog_DropboxBrowser_Text_AbsolutePath_Tooltip;

   public static String        Image__Dropbox_Logo;
   public static String        Image__Dropbox_File;
   public static String        Image__Dropbox_Folder;
   public static String        Image__Dropbox_Parentfolder;

   public static String        Pref_CloudConnectivity_Dropbox_AccessToken_Tooltip;
   public static String        Pref_CloudConnectivity_Dropbox_AccessToken_NotRetrieved;
   public static String        Pref_CloudConnectivity_Dropbox_AccessToken_Retrieval_Title;
   public static String        Pref_CloudConnectivity_Dropbox_AccessToken_Retrieved;
   public static String        Pref_CloudConnectivity_Dropbox_Button_Authorize;
   public static String        Pref_CloudConnectivity_Dropbox_Button_ChooseFolder;
   public static String        Pref_CloudConnectivity_Dropbox_FolderPath_Tooltip;

   static {
      // initialize resource bundle
      NLS.initializeMessages(BUNDLE_NAME, Messages.class);
   }

   private Messages() {}
}
