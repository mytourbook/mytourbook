package net.tourbook.map3.view;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	private static final String	BUNDLE_NAME	= "net.tourbook.map3.view.messages";	//$NON-NLS-1$

	public static String	Custom_Category_Name_Tour;

	public static String		Default_Category_Name_Info;
	public static String		Default_Category_Name_Map;
	public static String		Default_Category_Name_Tools;

	public static String		Default_Layer_Atmosphere;
	public static String		Default_Layer_Compass;
	public static String		Default_Layer_MS_VirtualEarchAerial;
	public static String		Default_Layer_NASABlueMarble;
	public static String		Default_Layer_PlaceNames;
	public static String		Default_Layer_PoliticalBoundaries;
	public static String		Default_Layer_ScaleBar;
	public static String		Default_Layer_USGS_UrbanArea;
	public static String		Default_Layer_WMS_BingImagery;
	public static String		Default_Layer_WMS_i_cubed_Landsat;
	public static String		Default_Layer_WMS_NASABlueMarble2004;
	public static String		Default_Layer_WMS_OpenStreetMap;
	public static String		Default_Layer_WMS_USDA_NAIP;
	public static String		Default_Layer_WMS_USDA_NAIP_USGS;
	public static String		Default_Layer_WMS_USGS_Topo100;
	public static String		Default_Layer_WMS_USGS_Topo24;
	public static String		Default_Layer_WMS_USGS_Topo250;
	public static String		Default_Layer_WorldMap;
	public static String		Default_LayerName_Stars;

	public static String		Image_Map3_Map3PropertiesView;

	public static String		Map3_Action_OpenMap3PropertiesView;

	public static String		Prop_Viewer_Label_AltitudeRange_Tooltip;

	public static String		Terrain_Follow_Cursor;
	public static String		Terrain_Follow_Eye;
	public static String		Terrain_Follow_None;
	public static String		Terrain_Follow_Object;
	public static String		Terrain_Follow_View;

	public static String		Terrain_Profile_Checkbox_KeepProportions;
	public static String		Terrain_Profile_Checkbox_ShowEye;
	public static String		Terrain_Profile_Checkbox_ZeroBased;
	public static String		Terrain_Profile_Dimension_Large;
	public static String		Terrain_Profile_Dimension_Medium;
	public static String		Terrain_Profile_Dimension_Small;
	public static String		Terrain_Profile_Label_Dimension;
	public static String		Terrain_Profile_Label_Follow;
	public static String		Terrain_Profile_Label_ProfileLength;

	public static String	TourTrack_Layer_Name;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
