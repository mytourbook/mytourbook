package net.tourbook.device.hac4pro;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String	BUNDLE_NAME	= "net.tourbook.device.hac4pro.messages";	//$NON-NLS-1$
	public static String		HAC4_Profile_bike1;
	public static String		HAC4_Profile_bike2;
	public static String		HAC4_Profile_run;
	public static String		HAC4_Profile_ski;
	public static String		HAC4_Profile_unknown;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
