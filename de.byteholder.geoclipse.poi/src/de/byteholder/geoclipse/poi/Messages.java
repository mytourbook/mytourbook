package de.byteholder.geoclipse.poi;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String	BUNDLE_NAME	= "de.byteholder.geoclipse.poi.messages";	//$NON-NLS-1$
	public static String	job_name_searchingPOI;
	public static String		PoiView_0;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
