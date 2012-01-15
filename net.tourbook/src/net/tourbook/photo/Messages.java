package net.tourbook.photo;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String	BUNDLE_NAME	= "net.tourbook.photo.messages";	//$NON-NLS-1$
	public static String	ColumnFactory_Photo_Altitude;
	public static String	ColumnFactory_Photo_Altitude_Tooltip;
	public static String	ColumnFactory_Photo_Date;
	public static String	ColumnFactory_Photo_Dimension;
	public static String	ColumnFactory_Photo_ImageDirectionDegree_Header;
	public static String	ColumnFactory_Photo_ImageDirectionDegree_Label;
	public static String	ColumnFactory_Photo_ImageDirectionDegree_Tooltip;
	public static String	ColumnFactory_Photo_ImageDirectionText_Header;
	public static String	ColumnFactory_Photo_ImageDirectionText_Label;
	public static String	ColumnFactory_Photo_ImageDirectionText_Tooltip;
	public static String	ColumnFactory_Photo_Location;
	public static String		ColumnFactory_Photo_Name;
	public static String	ColumnFactory_Photo_Orientation;
	public static String	ColumnFactory_Photo_Orientation_Header;
	public static String	ColumnFactory_Photo_Orientation_Tooltip;
	public static String	ColumnFactory_Photo_OtherTags;
	public static String	ColumnFactory_Photo_Time;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
