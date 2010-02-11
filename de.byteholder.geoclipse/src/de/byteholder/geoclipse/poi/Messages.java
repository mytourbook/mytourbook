package de.byteholder.geoclipse.poi;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String	BUNDLE_NAME	= "de.byteholder.geoclipse.poi.messages";	//$NON-NLS-1$
	public static String	job_name_searchingPOI;
	public static String	Poi_View_Button_Search;
	public static String	Poi_View_Label_Near;
	public static String	Poi_View_Label_NearestPlacesPart1;
	public static String	Poi_View_Label_NearestPlacesPart2;
	public static String	Poi_View_Label_NearestPlacesPart3;
	public static String	Poi_View_Label_NearestPlacesPart4;
	public static String	Poi_View_Label_POI;
	public static String	Poi_View_Label_POI_Tooltip;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
