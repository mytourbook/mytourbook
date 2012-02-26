package net.tourbook.photo;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	private static final String	BUNDLE_NAME	= "net.tourbook.photo.messages";					//$NON-NLS-1$

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

	public static String		Pic_Dir_Action_Preferences;

	public static String	Pic_Dir_Action_Refresh;
	public static String		Pic_Dir_Action_SingleClickExpand;
	public static String		Pic_Dir_Action_SingleExpandCollapseOthers;
	public static String		Pic_Dir_Label_FolderIsNotSelected;
	public static String		Pic_Dir_Label_Loading;
	public static String		Pic_Dir_Status_Loaded;

	public static String		PrefPage_Photo_Cache_Button_GetNumberOfImages;
	public static String		PrefPage_Photo_Cache_Dialog_MaxHandle_CreatedImagesBeforeError;
	public static String		PrefPage_Photo_Cache_Dialog_MaxHandle_NoError;
	public static String		PrefPage_Photo_Cache_Dialog_MaxHandle_Title;
	public static String		PrefPage_Photo_Cache_Error_ThumbnailLocation;
	public static String		PrefPage_Photo_Cache_Group_ThumbnailCacheLocation;
	public static String		PrefPage_Photo_Cache_Group_ThumbnailCacheSize;
	public static String		PrefPage_Photo_Cache_Label_NumberOfImages;
	public static String		PrefPage_Photo_Cache_Label_ThumbnailCacheSizeInfo;
	public static String		PrefPage_Photo_Cache_Label_ThumbnailLocation;
	public static String		PrefPage_Photo_Cache_Label_UseDefaultThumbnailLocation;

	public static String		PrefPage_Photo_Viewer_Checkbox_ShowNumbersInFolderView;
	public static String		PrefPage_Photo_Viewer_Checkbox_ShowNumbersInFolderView_Tooltip;
	public static String		PrefPage_Photo_Viewer_Group_Colors;
	public static String		PrefPage_Photo_Viewer_Label_BackgroundColor;
	public static String		PrefPage_Photo_Viewer_Label_FileColor;
	public static String		PrefPage_Photo_Viewer_Label_FolderColor;
	public static String		PrefPage_Photo_Viewer_Label_ForgroundColor;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
