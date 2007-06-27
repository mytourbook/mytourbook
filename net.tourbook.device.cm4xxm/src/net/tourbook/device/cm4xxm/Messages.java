package net.tourbook.device.cm4xxm;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String	BUNDLE_NAME	= "net.tourbook.device.cm4xxm.messages";	//$NON-NLS-1$
	public static String		CM4XXM_profile_bike1;
	public static String		CM4XXM_profile_bike2;
	public static String		CM4XXM_profile_unknown;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
