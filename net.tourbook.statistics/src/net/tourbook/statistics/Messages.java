package net.tourbook.statistics;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	private static final String	BUNDLE_NAME	= "net.tourbook.statistics.messages";	//$NON-NLS-1$

	public static String		Action_zoom_into_month;

	public static String		Statistic_dlg_select_tour_msg;

	public static String		Statistic_dlg_select_tour_title;

	public static String		Statistic_format_time_hhmm_hhmm;

	public static String		Statistic_unit_label_time;

	public static String		Statistic_unit_text_daytime;
	public static String		Statistic_unit_text_driving_time;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}
	private Messages() {}
}
