package net.tourbook.web;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	private static final String	BUNDLE_NAME	= "net.tourbook.web.messages";	//$NON-NLS-1$

	public static String	Web_Page_ContentLoading;

	public static String	Web_Page_Search_Title;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
