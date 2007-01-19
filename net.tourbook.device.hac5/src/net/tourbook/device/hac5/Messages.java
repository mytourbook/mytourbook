package net.tourbook.device.hac5;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String	BUNDLE_NAME	= "net.tourbook.device.hac5.messages";	//$NON-NLS-1$
	public static String		HAC5_profile_alpine;
	public static String		HAC5_profile_bike1;
	public static String		HAC5_profile_bike2;
	public static String		HAC5_profile_none;
	public static String		HAC5_profile_rds;
	public static String		HAC5_profile_run;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}
	private Messages() {}
}
