package net.tourbook.srtmtilefactory;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String	BUNDLE_NAME	= "net.tourbook.tilefactory.srtm.messages"; //$NON-NLS-1$
	public static String		SRTM_MapProvider_Description;
	public static String		SRTM_MapProvider_Log_PaintingTile;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
