package net.tourbook.map3.view;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String	BUNDLE_NAME	= "net.tourbook.map3.view.messages";	//$NON-NLS-1$
	public static String		Image_Map3_Map3PropertiesView;
	public static String	Image_Map3Property_Layer;
	public static String		Map3_Action_OpenMap3PropertiesView;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
