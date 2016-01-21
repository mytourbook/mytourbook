package net.tourbook.device.gpx;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String	BUNDLE_NAME	= "net.tourbook.device.gpx.messages";	//$NON-NLS-1$

	public static String		Marker_Label_Lap;
	public static String		Marker_Label_Track;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
