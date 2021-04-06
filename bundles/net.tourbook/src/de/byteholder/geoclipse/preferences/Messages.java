/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation version 2 of the License.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *******************************************************************************/
package de.byteholder.geoclipse.preferences;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

   private static final String BUNDLE_NAME = "de.byteholder.geoclipse.preferences.messages"; //$NON-NLS-1$

   public static String        External_Link_Forum;
   public static String        External_Link_HomePage;
   public static String        External_Link_MapProviders;

   public static String        Map2Provider_Tooltip_Checkbox_IncludeHillshading;
   public static String        Map2Provider_Tooltip_Checkbox_IsTransparentLayer;
   public static String        Map2Provider_Tooltip_Label_Description;
   public static String        Map2Provider_Tooltip_Label_Layers;
   public static String        Map2Provider_Tooltip_Label_OnlineMap;
   public static String        Map2Provider_Tooltip_Label_MapProviderId;
   public static String        Map2Provider_Tooltip_Label_MapProviderType;
   public static String        Map2Provider_Tooltip_Label_OfflineFolder;

   public static String        pref_cache_clear_cache;
   public static String        pref_cache_location;
   public static String        pref_cache_message_box_text;
   public static String        pref_cache_message_box_title;
   public static String        pref_cache_use_default_location;
   public static String        pref_cache_use_offline;

   public static String        pref_error_invalid_path;

   public static String        Pref_Map_Button_AddMapProfile;
   public static String        Pref_Map_Button_AddMapProfile_Tooltip;
   public static String        Pref_Map_Button_AddMapProviderCustom;
   public static String        Pref_Map_Button_AddMapProviderCustom_Tooltip;
   public static String        Pref_Map_Button_AddMapProviderWms;
   public static String        Pref_Map_Button_AddMapProviderWms_Tooltip;
   public static String        Pref_Map_Button_CancelMapProvider;
   public static String        Pref_Map_Button_CancelTileInfo_Tooltip;
   public static String        Pref_Map_Button_DeleteMapProvider;
   public static String        Pref_Map_Button_DeleteOfflineMap;
   public static String        Pref_Map_Button_Edit;
   public static String        Pref_Map_Button_ExportMP;
   public static String        Pref_Map_Button_ImportMP;
   public static String        Pref_Map_Button_RefreshTileInfo_Tooltip;
   public static String        Pref_Map_Button_RefreshTileInfoNotAssessed_Tooltip;
   public static String        Pref_Map_Button_RefreshTileInfoSelected_Tooltip;
   public static String        Pref_Map_Button_UpdateMapProvider;
   public static String        Pref_Map_Checkbox_IncludeHillshading;
   public static String        Pref_Map_Checkbox_IsTransparentLayer;
   public static String        Pref_Map_Checkbox_IsTransparentLayer_Tooltip;
   public static String        Pref_Map_Dialog_Export_Title;
   public static String        Pref_Map_Dialog_Import_Title;
   public static String        Pref_Map_Dialog_WmsInput_Message;
   public static String        Pref_Map_Dialog_WmsInput_Title;
   public static String        Pref_Map_Error_Dialog_DragDropError_Message;
   public static String        Pref_Map_Error_Dialog_DragDropError_Title;
   public static String        Pref_Map_Group_Detail_ModifiedMapProvider;
   public static String        Pref_Map_Group_Detail_NewMapProvider;
   public static String        Pref_Map_Group_Detail_SelectedMapProvider;
   public static String        Pref_Map_JobName_DropUrl;
   public static String        Pref_Map_JobName_ReadMapFactoryOfflineInfo;
   public static String        Pref_Map_Label_AvailableMapProvider;
   public static String        Pref_Map_Label_Description;
   public static String        Pref_Map_Label_Layers;
   public static String        Pref_Map_Label_MapProviderDropTarget;
   public static String        Pref_Map_Label_MapProviderDropTarget_Tooltip;
   public static String        Pref_Map_Label_OfflineInfo_NotDone;
   public static String        Pref_Map_Label_OfflineInfo_Partly;
   public static String        Pref_Map_Label_OfflineInfo_Total;
   public static String        Pref_Map_Label_WmsDropTarget;
   public static String        Pref_Map_Label_WmsDropTarget_Tooltip;
   public static String        Pref_Map_Label_Category;
   public static String        Pref_Map_Label_Files;
   public static String        Pref_Map_Label_MapProvider;
   public static String        Pref_Map_Label_MapProviderId;
   public static String        Pref_Map_Label_MapProviderType;
   public static String        Pref_Map_Label_NotAvailable;
   public static String        Pref_Map_Label_NotRetrieved;
   public static String        Pref_Map_Label_OfflineFolder;
   public static String        Pref_Map_Label_OnlineMap;
   public static String        Pref_Map_Link_MapProvider;
   public static String        Pref_Map_Link_MapProvider_Tooltip;
   public static String        Pref_Map_ProviderType_Custom;
   public static String        Pref_Map_ProviderType_MapProfile;
   public static String        Pref_Map_ProviderType_Plugin;
   public static String        Pref_Map_ProviderType_Wms;
   public static String        Pref_Map_ValidationError_NameIsRequired;
   public static String        Pref_Map_ValidationError_OfflineFolderIsUsedInMapProfile;
   public static String        Pref_Map2_Viewer_Column_IsHillshading;
   public static String        Pref_Map2_Viewer_Column_IsHillshading_Tooltip;
   public static String        Pref_Map2_Viewer_Column_IsTransparent;
   public static String        Pref_Map2_Viewer_Column_IsTransparent_Tooltip;
   public static String        Pref_Map2_Viewer_Column_IsVisible;
   public static String        Pref_Map2_Viewer_Column_IsVisible_Tooltip;
   public static String        Pref_Map2_Viewer_Column_Category;
   public static String        Pref_Map2_Viewer_Column_Description;
   public static String        Pref_Map2_Viewer_Column_Layers;
   public static String        Pref_Map2_Viewer_Column_MapProvider;
   public static String        Pref_Map2_Viewer_Column_Modified;
   public static String        Pref_Map2_Viewer_Column_OfflineFileCounter;
   public static String        Pref_Map2_Viewer_Column_OfflineFileSize;
   public static String        Pref_Map2_Viewer_Column_OfflinePath;
   public static String        Pref_Map2_Viewer_Column_OnlineMapUrl;
   public static String        Pref_Map2_Viewer_Column_MPType;
   public static String        Pref_Map2_Viewer_Column_MPType_Tooltip;
   public static String        Pref_Map2_Viewer_Column_TileUrl;

   public static String        PrefPageMapProviders_Pref_Map_FileDialog_AllFiles;
   public static String        PrefPageMapProviders_Pref_Map_FileDialog_XmlFiles;

   public static String        pref_map_dlg_cancelModifiedMapProvider_message;
   public static String        pref_map_dlg_cancelModifiedMapProvider_title;
   public static String        pref_map_dlg_confirmDeleteMapProvider_message;
   public static String        pref_map_dlg_confirmDeleteMapProvider_title;
   public static String        pref_map_dlg_saveModifiedMapProvider_message;
   public static String        pref_map_dlg_saveModifiedMapProvider_title;
   public static String        pref_map_dlg_saveOtherMapProvider_message;
   public static String        pref_map_label_NA;
   public static String        pref_map_message_loadingWmsCapabilities;
   public static String        pref_map_show_tile_info;
   public static String        pref_map_validationError_factoryIdIsAlreadyUsed;
   public static String        pref_map_validationError_factoryIdIsRequired;
   public static String        pref_map_validationError_offlineFolderInvalidCharacters;
   public static String        pref_map_validationError_offlineFolderIsAlreadyUsed;
   public static String        pref_map_validationError_offlineFolderIsRequired;

   public static String        prefPage_cache_dlg_confirmDelete_message;
   public static String        prefPage_cache_dlg_confirmDelete_title;
   public static String        prefPage_cache_group_offlineDirectory;
   public static String        prefPage_cache_group_offlineInfo;
   public static String        prefPage_cache_jobNameReadOfflineInfo;
   public static String        prefPage_cache_label_files;
   public static String        prefPage_cache_label_path;
   public static String        prefPage_cache_label_size;
   public static String        prefPage_cache_label_status;
   public static String        prefPage_cache_MByte;
   public static String        prefPage_cache_status_deletingFiles;
   public static String        prefPage_cache_status_directoryIsNotAvailable;
   public static String        prefPage_cache_status_infoWasCanceled;
   public static String        prefPage_cache_status_noValue;
   public static String        prefPage_cache_status_retrieving;

   public static String        Theme_Font_Logging;
   public static String        Theme_Font_Logging_PREVIEW_TEXT;

   public static String        Wms_Error_InvalidUrl;

   static {
      // initialize resource bundle
      NLS.initializeMessages(BUNDLE_NAME, Messages.class);
   }

   private Messages() {}
}
