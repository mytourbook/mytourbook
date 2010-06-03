package net.tourbook.printing;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	private static final String	BUNDLE_NAME	= "net.tourbook.printing.messages"; //$NON-NLS-1$

	public static String		Dialog_Print_Chk_PrintMarkers;
	public static String		Dialog_Print_Chk_PrintMarkers_Tooltip;
	public static String		Dialog_Print_Chk_PrintNotes;
	public static String		Dialog_Print_Chk_PrintNotes_Tooltip;
	public static String		Dialog_Print_Chk_OverwriteFiles;
	public static String		Dialog_Print_Chk_OverwriteFiles_Tooltip;
	public static String		Dialog_Print_Btn_Print;
	public static String		Dialog_Print_Dialog_Message;
	public static String		Dialog_Print_File_Dialog_Text;
	public static String		Dialog_Print_Dialog_Title;
	public static String		Dialog_Print_Dir_Dialog_Text;
	public static String		Dialog_Print_Dir_Dialog_Message;
	public static String		Dialog_Print_Group_PdfFileName;
	public static String		Dialog_Print_Group_Options;
	public static String		Dialog_Print_Group_Paper;
	public static String		Dialog_Print_Label_PrintFilePath;
	public static String		Dialog_Print_Label_FileName;
	public static String		Dialog_Print_Label_FilePath;
	public static String		Dialog_Print_Label_Paper_Size;
	public static String		Dialog_Print_Label_Paper_Size_A4;
	public static String		Dialog_Print_Label_Paper_Size_Letter;
	public static String		Dialog_Print_Label_Paper_Orientation;
	public static String		Dialog_Print_Label_Paper_Orientation_Portrait;
	public static String		Dialog_Print_Label_Paper_Orientation_Landscape;
	public static String		Dialog_Print_Lbl_PdfFilePath;
	public static String		Dialog_Print_Msg_FileAlreadyExists;
	public static String		Dialog_Print_Msg_FileNameIsInvalid;
	public static String		Dialog_Print_Msg_PathIsNotAvailable;
	public static String		Dialog_Print_Shell_Text;
	public static String		Dialog_Print_Txt_FilePath_Tooltip;

	public static String		app_btn_browse;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
