/*******************************************************************************
 * Copyright (C) 2006, 2007  Wolfgang Schramm
 *  
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software 
 * Foundation version 2 of the License.
 *  
 * This program is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with 
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA    
 *******************************************************************************/
package net.tourbook.preferences;

import org.eclipse.osgi.util.NLS;

public class PreferenceMessages extends NLS {
	private static final String	BUNDLE_NAME	= "net.tourbook.preferences.messages";	//$NON-NLS-1$
	private PreferenceMessages() {}
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, PreferenceMessages.class);
	}

	
	public static String	GraphButtonUp;
	public static String	GraphButtonDown;


	public static String	IntegerFieldEditorErrorMessage;

	// tab: graph defaults
	public static String	TabGraphDefaults;
	public static String	LabelSelectTheGraph;
	public static String	GraphAltitude;
	public static String	GraphSpeed;
	public static String	GraphPulse;
	public static String	GraphTemperature;
	public static String	GraphCadence;
	public static String	GraphAltimeter;
	public static String	GraphGradient;

}
