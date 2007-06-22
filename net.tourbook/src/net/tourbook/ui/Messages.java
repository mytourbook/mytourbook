package net.tourbook.ui;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String	BUNDLE_NAME	= "net.tourbook.ui.messages";	//$NON-NLS-1$

	public static String		ColumnFactory_altitude_down_title;

	public static String		ColumnFactory_altitude_up;
	public static String		ColumnFactory_altitude_up_label;
	public static String		ColumnFactory_altitude_up_tooltip;
	public static String		ColumnFactory_date;
	public static String		ColumnFactory_date_label;

	public static String	ColumnFactory_date_tooltip;
	public static String		ColumnFactory_db_status_label;
	public static String		ColumnFactory_db_status_tooltip;
	public static String		ColumnFactory_device;
	public static String		ColumnFactory_device_label;

	public static String	ColumnFactory_device_start_distance_label;

	public static String	ColumnFactory_device_start_distance_tooltip;
	public static String		ColumnFactory_device_tooltip;
	public static String		ColumnFactory_distance;
	public static String		ColumnFactory_distance_label;
	public static String		ColumnFactory_distance_tooltip;
	public static String		ColumnFactory_driving_time;
	public static String		ColumnFactory_driving_time_label;
	public static String		ColumnFactory_driving_time_tooltip;
	public static String		ColumnFactory_import_filename;
	public static String		ColumnFactory_import_filename_label;
	public static String		ColumnFactory_import_filename_tooltip;
	public static String		ColumnFactory_import_filepath;
	public static String		ColumnFactory_import_filepath_label;
	public static String		ColumnFactory_import_filepath_tooltip;
	public static String		ColumnFactory_profile;
	public static String		ColumnFactory_profile_label;
	public static String		ColumnFactory_profile_tooltip;
	public static String		ColumnFactory_recording_time;
	public static String		ColumnFactory_recording_time_label;
	public static String		ColumnFactory_recording_time_tooltip;
	public static String		ColumnFactory_speed;
	public static String		ColumnFactory_speed_label;
	public static String		ColumnFactory_speed_tooltip;
	public static String		ColumnFactory_time;
	public static String		ColumnFactory_time_interval;
	public static String		ColumnFactory_time_interval_label;
	public static String		ColumnFactory_time_interval_tooltip;
	public static String		ColumnFactory_time_label;
	public static String		ColumnFactory_time_tooltip;

	public static String	ColumnFactory_tour_numbers;

	public static String	ColumnFactory_tour_numbers_lable;

	public static String	ColumnFactory_tour_numbers_tooltip;
	public static String		ColumnFactory_tour_type_label;
	public static String		ColumnFactory_tour_type_tooltip;
	public static String		ColumnFactory_tour_title;
	public static String		ColumnFactory_tour_title_label;
	public static String		ColumnFactory_tour_title_tooltip;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
