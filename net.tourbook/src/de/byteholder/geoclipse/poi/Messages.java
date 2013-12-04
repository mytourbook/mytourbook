package de.byteholder.geoclipse.poi;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	private static final String	BUNDLE_NAME	= "de.byteholder.geoclipse.poi.messages";	//$NON-NLS-1$

	public static String	Dialog_DropNoPOI_InvalidData;

	public static String	Dialog_DropNoPOI_Message;

	public static String	Dialog_DropNoPOI_Title;

	public static String	Dialog_OfflineArea_Error;

	public static String	Dialog_OfflineArea_Error_IsLoading;

	public static String	Dialog_OfflineArea_Error_NoOffline;

	public static String		job_name_searchingPOI;
	public static String		Poi_View_Button_Search;
	public static String		Poi_View_Label_Near;
	public static String		Poi_View_Label_NearestPlacesPart1;
	public static String		Poi_View_Label_NearestPlacesPart2;
	public static String		Poi_View_Label_NearestPlacesPart3;
	public static String		Poi_View_Label_NearestPlacesPart4;
	public static String		Poi_View_Label_POI;
	public static String		Poi_View_Label_POI_Tooltip;

	public static String		Image_POI_Anchor;
	public static String		Image_POI_Car;
	public static String		Image_POI_Cart;
	public static String		Image_POI_Flag;
	public static String		Image_POI_House;
	public static String		Image_POI_InMap;
	public static String		Image_POI_Soccer;
	public static String		Image_POI_Star;

	public static String	Offline_Area_Label_AreaMarker;

	public static String	Offline_Area_Label_OldAreaMarker;

	public static String	Offline_Area_Label_SelectInfo;

	public static String	TileInfo_Position_Latitude;

	public static String	TileInfo_Position_Longitude;

	public static String	TileInfo_Position_X;

	public static String	TileInfo_Position_Y;

	public static String	TileInfo_Position_Zoom;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
