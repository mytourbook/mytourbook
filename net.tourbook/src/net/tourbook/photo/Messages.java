package net.tourbook.photo;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	private static final String	BUNDLE_NAME	= "net.tourbook.photo.messages";						//$NON-NLS-1$

	public static String		ColumnFactory_Photo_Altitude;
	public static String		ColumnFactory_Photo_Altitude_Tooltip;
	public static String		ColumnFactory_Photo_Date;
	public static String		ColumnFactory_Photo_Dimension;
	public static String		ColumnFactory_Photo_ImageDirectionDegree_Header;
	public static String		ColumnFactory_Photo_ImageDirectionDegree_Label;
	public static String		ColumnFactory_Photo_ImageDirectionDegree_Tooltip;
	public static String		ColumnFactory_Photo_ImageDirectionText_Label;
	public static String		ColumnFactory_Photo_ImageDirectionText_Tooltip;
	public static String		ColumnFactory_Photo_Location;
	public static String		ColumnFactory_Photo_Name;
	public static String		ColumnFactory_Photo_Orientation;
	public static String		ColumnFactory_Photo_Orientation_Header;
	public static String		ColumnFactory_Photo_Orientation_Tooltip;
	public static String		ColumnFactory_Photo_OtherTags;
	public static String		ColumnFactory_Photo_Time;

	public static String	Pic_Dir_Action_ClearHistory;

	public static String	Pic_Dir_Action_NavigateHistoryBackward;

	public static String	Pic_Dir_Action_NavigateHistoryForward;

	public static String		Pic_Dir_Action_Preferences;
	public static String		Pic_Dir_Action_Refresh;
	public static String		Pic_Dir_Action_RunFileBrowser;
	public static String		Pic_Dir_Action_SingleClickExpand;
	public static String		Pic_Dir_Action_SingleExpandCollapseOthers;
	public static String		Pic_Dir_Dialog_ExternalPhotoViewer_Message;
	public static String		Pic_Dir_Dialog_ExternalPhotoViewer_Title;
	public static String	Pic_Dir_Dialog_FolderIsNotAvailable_Message;

	public static String	Pic_Dir_Dialog_FolderIsNotAvailable_Title;

	public static String		Pic_Dir_Label_FolderIsNotSelected;
	public static String		Pic_Dir_Label_Loading;
	public static String		Pic_Dir_Status_Loaded;

	public static String		PrefPage_Photo_Cache_Button_GetNumberOfImages;
	public static String		PrefPage_Photo_Cache_Dialog_MaxHandle_CreatedImagesBeforeError;
	public static String		PrefPage_Photo_Cache_Dialog_MaxHandle_NoError;
	public static String		PrefPage_Photo_Cache_Dialog_MaxHandle_Title;
	public static String		PrefPage_Photo_Cache_Group_ThumbnailCacheSize;
	public static String		PrefPage_Photo_Cache_Label_NumberOfImages;
	public static String		PrefPage_Photo_Cache_Label_ThumbnailCacheSizeInfo;
	public static String		PrefPage_Photo_ExtViewer_Group_ExternalApplication;
	public static String		PrefPage_Photo_ExtViewer_Label_ExternalApplication;
	public static String		PrefPage_Photo_ExtViewer_Label_ExternalApplication_Tooltip;
	public static String		PrefPage_Photo_ExtViewer_Label_Info;
	public static String		PrefPage_Photo_ThumbStore_Button_CleanupAll;
	public static String		PrefPage_Photo_ThumbStore_Button_CleanupAll_Tooltip;
	public static String		PrefPage_Photo_ThumbStore_Button_CleanupNow;
	public static String		PrefPage_Photo_ThumbStore_Button_CleanupNow_Tooltip;
	public static String		PrefPage_Photo_ThumbStore_Checkbox_Cleanup;
	public static String		PrefPage_Photo_ThumbStore_Checkbox_UseDefaultLocation;
	public static String		PrefPage_Photo_ThumbStore_Error_Location;
	public static String		PrefPage_Photo_ThumbStore_Group_Cleanup;
	public static String		PrefPage_Photo_ThumbStore_Group_ThumbnailStoreLocation;
	public static String		PrefPage_Photo_ThumbStore_Text_Location;
	public static String	PrefPage_Photo_Thumbstore_Label_LastCleanup;

	public static String		PrefPage_Photo_Thumbstore_Label_UnitDays;
	public static String		PrefPage_Photo_Thumbstore_Spinner_CleanupPeriod;
	public static String		PrefPage_Photo_Thumbstore_Spinner_CleanupPeriod_Tooltip;
	public static String		PrefPage_Photo_Thumbstore_Spinner_KeepImagesNumberOfDays;
	public static String		PrefPage_Photo_Thumbstore_Spinner_KeepImagesNumberOfDays_Tooltip;

	public static String		PrefPage_Photo_Viewer_Checkbox_ShowNumbersInFolderView;
	public static String		PrefPage_Photo_Viewer_Checkbox_ShowNumbersInFolderView_Tooltip;
	public static String		PrefPage_Photo_Viewer_Group_Colors;
	public static String		PrefPage_Photo_Viewer_Label_BackgroundColor;
	public static String		PrefPage_Photo_Viewer_Label_FileColor;
	public static String		PrefPage_Photo_Viewer_Label_FolderColor;
	public static String		PrefPage_Photo_Viewer_Label_ForgroundColor;

	public static String		Thumbnail_Store_CleanupTask;
	public static String		Thumbnail_Store_CleanupTask_AllFiles;
	public static String		Thumbnail_Store_CleanupTask_Subtask;
	public static String		Thumbnail_Store_Error_CannotDeleteFolder;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
