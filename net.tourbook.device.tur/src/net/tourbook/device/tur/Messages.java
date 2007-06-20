/**
 * Project:  net.tourbook.device.tur
 * Filename: Messages.java
 * Date:     20.06.2007
 * 
 */
package net.tourbook.device.tur;

import org.eclipse.osgi.util.NLS;

/**
 * @author stm
 *
 */
public class Messages {
	private static final String			BUNDLE_NAME		= "net.tourbook.device.tur.messages";		//$NON-NLS-1$

	public static String        TourData_Tour_Marker_unnamed;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}

}
