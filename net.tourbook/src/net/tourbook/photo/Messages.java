package net.tourbook.photo;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String	BUNDLE_NAME	= "net.tourbook.photo.messages";	//$NON-NLS-1$
	public static String	ColumnFactory_Photo_DateTime;
	public static String	ColumnFactory_Photo_DateTime_Header;
	public static String		ColumnFactory_Photo_Name;
	public static String	ColumnFactory_Photo_OtherTags;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
