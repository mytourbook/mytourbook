package net.tourbook.photo;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String	BUNDLE_NAME	= "net.tourbook.photo.messages";		//$NON-NLS-1$

	public static String		ColumnFactory_Photo_Altitude;
	public static String		ColumnFactory_Photo_Altitude_Tooltip;
	public static String		ColumnFactory_Photo_Date;
	public static String		ColumnFactory_Photo_Dimension;
	public static String		ColumnFactory_Photo_ImageDirectionDegree_Header;
	public static String		ColumnFactory_Photo_ImageDirectionDegree_Label;
	public static String		ColumnFactory_Photo_ImageDirectionDegree_Tooltip;
	public static String		ColumnFactory_Photo_ImageDirectionText_Header;
	public static String		ColumnFactory_Photo_ImageDirectionText_Label;
	public static String		ColumnFactory_Photo_ImageDirectionText_Tooltip;
	public static String		ColumnFactory_Photo_Location;
	public static String		ColumnFactory_Photo_Name;
	public static String		ColumnFactory_Photo_Orientation;
	public static String		ColumnFactory_Photo_Orientation_Header;
	public static String		ColumnFactory_Photo_Orientation_Tooltip;
	public static String		ColumnFactory_Photo_OtherTags;
	public static String		ColumnFactory_Photo_Time;

	public static String		Title;

	public static String		menu_File_text;
	public static String		menu_File_Close_text;
	public static String		menu_File_SimulateOnly_text;
	public static String		menu_Help_text;
	public static String		menu_Help_About_text;
	public static String		tool_Cut_tiptext;
	public static String		tool_Copy_tiptext;
	public static String		tool_Delete_tiptext;
	public static String		tool_Parent_tiptext;
	public static String		tool_Paste_tiptext;
	public static String		tool_Print_tiptext;
	public static String		tool_Refresh_tiptext;
	public static String		tool_Rename_tiptext;
	public static String		tool_Search_tiptext;
	public static String		details_AllFolders_text;
	public static String		details_ContentsOf_text;
	public static String		details_FileSize_text;
	public static String		details_DirNumberOfObjects_text;
	public static String		details_NumberOfSelectedFiles_text;
	public static String		table_Name_title;
	public static String		table_Size_title;
	public static String		table_Type_title;
	public static String		table_Modified_title;
	public static String		filetype_Unknown;
	public static String		filetype_None;
	public static String		filetype_Folder;
	public static String		filesize_KB;
	public static String		dialog_About_title;
	public static String		dialog_About_description;
	public static String		dialog_NotImplemented_title;
	public static String		dialog_ActionNotImplemented_description;
	public static String		dialog_FailedCopy_title;
	public static String		dialog_FailedCopy_description;
	public static String		dialog_FailedDelete_title;
	public static String		dialog_FailedDelete_description;
	public static String		progressDialog_cancelButton_text;
	public static String		progressDialog_Copy_title;
	public static String		progressDialog_Copy_description;
	public static String		progressDialog_Copy_operation;
	public static String		progressDialog_Move_title;
	public static String		progressDialog_Move_description;
	public static String		progressDialog_Delete_operation;
	public static String		simulate_CopyFromTo_text;
	public static String		simulate_DirectoriesCreated_text;
	public static String		simulate_Delete_text;
	public static String		error_FailedLaunch_message;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
