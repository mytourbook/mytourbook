package net.tourbook.device.hac4;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String	BUNDLE_NAME	= "net.tourbook.device.hac4.messages";	//$NON-NLS-1$
	public static String		HAC4_Profile_bike;
	public static String		HAC4_Profile_jogging;
	public static String		HAC4_Profile_ski;
	public static String		HAC4_Profile_ski_bike;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
