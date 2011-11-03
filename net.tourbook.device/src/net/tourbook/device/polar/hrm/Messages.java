/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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
package net.tourbook.device.polar.hrm;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	private static final String	BUNDLE_NAME	= "net.tourbook.device.polar.hrm.messages"; //$NON-NLS-1$


	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	public static String		Import_Confirm_PPD;

	public static String		Import_Error_DeviceNameIsUnknown;
	public static String		Import_Error_DialogMessage_InvalidDate;
	public static String		Import_Error_DialogMessage_InvalidField;
	public static String		Import_Error_DialogMessage_InvalidInterval;
	public static String		Import_Error_DialogMessage_InvalidVersion;
	public static String		Import_Error_DialogTitle;

	public static String		PrefPage_Polar_Field_TitleDescription;
	public static String		PrefPage_Polar_Group_HorizontalAdjustment;
	public static String		PrefPage_Polar_Group_TitleDescription;
	public static String		PrefPage_Polar_Group_PPDImportInfo;
	public static String		PrefPage_Polar_Label_AdjustmentInfo;
	public static String		PrefPage_Polar_Label_PPDImportInfo;
	public static String		PrefPage_Polar_Label_SliceAdjustment;
	public static String		PrefPage_Polar_Label_Slices;
	public static String		PrefPage_Polar_Radio_TitleFromDescription;
	public static String		PrefPage_Polar_Radio_TitleFromTitle;

	public static String		Supported_Intervals_1_Second;
	public static String		Supported_Intervals_15_Second;
	public static String		Supported_Intervals_2_Second;
	public static String		Supported_Intervals_30_Second;
	public static String		Supported_Intervals_5_Minutes;
	public static String		Supported_Intervals_5_Second;
	public static String		Supported_Intervals_60_Second;


	private Messages() {}

}
