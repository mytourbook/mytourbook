package net.tourbook.srtm.tilefactory;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	private static final String	BUNDLE_NAME	= "net.tourbook.srtm.tilefactory.messages"; //$NON-NLS-1$

	public static String		SRTM_MapProvider_Description;
	public static String		SRTM_MapProvider_Log_PaintingTile;

	public static String		SRTM_MapProvider_Name;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
