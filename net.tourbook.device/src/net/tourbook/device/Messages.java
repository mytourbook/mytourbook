/*******************************************************************************
 * Copyright (C) 2005, 2014 Wolfgang Schramm and Contributors
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
package net.tourbook.device;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	private static final String	BUNDLE_NAME	= "net.tourbook.device.messages";		//$NON-NLS-1$

	public static String		FitLog_Error_InvalidStartTime;

	public static String		Port_Listener_Error_ntd001;
	public static String		Port_Listener_Error_ntd002;

	public static String		PrefPage_GPX_Checkbox_OneTour;
	public static String		PrefPage_GPX_Group_DistanceValues;
	public static String		PrefPage_GPX_Label_DistanceValues;
	public static String		PrefPage_GPX_Radio_DistanceAbsolute;
	public static String		PrefPage_GPX_Radio_DistanceAbsolute_Tooltip;
	public static String		PrefPage_GPX_Radio_DistanceRelative;
	public static String		PrefPage_GPX_Radio_DistanceRelative_Tooltip;

	public static String		PrefPage_HAC4_Checkbox_DisableChecksumValidation;
	public static String		PrefPage_HAC4_Group;

	public static String		PrefPage_HAC45_Checkbox_AdjustImportYear;
	public static String		PrefPage_HAC45_Group;
	public static String		PrefPage_HAC45_Label_ImportYear;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
