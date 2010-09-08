package net.tourbook.device;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String	BUNDLE_NAME	= "net.tourbook.device.messages";	//$NON-NLS-1$
	public static String		Port_Listener_Error_ntd001;
	public static String		Port_Listener_Error_ntd002;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
