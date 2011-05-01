package net.tourbook.training;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String	BUNDLE_NAME	= "net.tourbook.training.messages"; //$NON-NLS-1$
	public static String	Dialog_HRZone_Button_AddZone;
	public static String	Dialog_HRZone_DialogMessage;
	public static String	Dialog_HRZone_DialogTitle;
	public static String	HRMaxFormula_Name_HRmax_191_5;
	public static String	HRMaxFormula_Name_HRmax_205_8;
	public static String	HRMaxFormula_Name_HRmax_206_9;
	public static String	HRMaxFormula_Name_HRmax_220_age;
	public static String		HRMaxFormula_Name_Manual;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
