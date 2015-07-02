package net.tourbook.device.garmin.fit;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	private static final String	BUNDLE_NAME	= "net.tourbook.device.garmin.fit.messages";	//$NON-NLS-1$

	public static String		PrefPage_Fit_Checkbox_IgnoreLastMarker;
	public static String		PrefPage_Fit_Checkbox_IgnoreLastMarker_Info;
	public static String		PrefPage_Fit_Checkbox_SplitTour;
	public static String		PrefPage_Fit_Checkbox_SplitTour_Info;
	public static String		PrefPage_Fit_Group_AdjustTemperature;
	public static String		PrefPage_Fit_Group_IgnoreLastMarker;
	public static String		PrefPage_Fit_Group_SplitTour;
	public static String		PrefPage_Fit_Label_AdjustTemperature;
	public static String		PrefPage_Fit_Label_AdjustTemperature_Info;
	public static String		PrefPage_Fit_Label_IgnoredTimeSlices;
	public static String		PrefPage_Fit_Label_SplitTour_Duration;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
