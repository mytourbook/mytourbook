package net.tourbook.ext.srtm;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String	BUNDLE_NAME	= "net.tourbook.ext.srtm.messages"; //$NON-NLS-1$
	public static String		prefPage_srtm_chk_use_default_location;
	public static String		prefPage_srtm_editor_data_filepath;
	public static String		prefPage_srtm_group_label_data_location;
	public static String	prefPage_srtm_msg_invalid_data_path;
	public static String	srtm_transfer_error_message;
	public static String	srtm_transfer_error_title;
	public static String	srtm_transfer_initialize;
	public static String	srtm_transfer_retrieve_file;
	public static String	srtm_transfer_task;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
