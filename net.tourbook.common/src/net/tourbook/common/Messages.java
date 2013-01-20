/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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
package net.tourbook.common;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	private static final String	BUNDLE_NAME	= "net.tourbook.common.messages";	//$NON-NLS-1$

	public static String		Image___Empty16;
	public static String		Image__ConfigureColumns;
	public static String		Image__options;
	public static String		Image__weather_sunny;
	public static String		Image__weather_cloudy;
	public static String		Image__weather_clouds;
	public static String		Image__weather_lightning;
	public static String		Image__weather_rain;
	public static String		Image__weather_snow;
	public static String		Image__Weather_ScatteredShowers;
	public static String		Image__Weather_Severe;

	public static String	Image_Action_PhotoFilter;

	public static String	Image_Action_PhotoFilter_Disabled;

	public static String	Image_Action_PhotoProperties;

	public static String	Image_Action_PhotoProperties_Disabled;

	public static String		Weather_Clounds_Clouds;
	public static String		Weather_Clounds_Clouny;
	public static String		Weather_Clounds_IsNotDefined;
	public static String		Weather_Clounds_Lightning;
	public static String		Weather_Clounds_Rain;
	public static String		Weather_Clounds_ScatteredShowers;
	public static String		Weather_Clounds_SevereWeatherAlert;
	public static String		Weather_Clounds_Snow;
	public static String		Weather_Clounds_Sunny;

	public static String		Weather_WindDirection_E;
	public static String		Weather_WindDirection_N;
	public static String		Weather_WindDirection_NE;
	public static String		Weather_WindDirection_NW;
	public static String		Weather_WindDirection_S;
	public static String		Weather_WindDirection_SE;
	public static String		Weather_WindDirection_SW;
	public static String		Weather_WindDirection_W;

	public static String		Weather_WindSpeed_Bft00;
	public static String		Weather_WindSpeed_Bft00_Short;
	public static String		Weather_WindSpeed_Bft01;
	public static String		Weather_WindSpeed_Bft01_Short;
	public static String		Weather_WindSpeed_Bft02;
	public static String		Weather_WindSpeed_Bft02_Short;
	public static String		Weather_WindSpeed_Bft03;
	public static String		Weather_WindSpeed_Bft03_Short;
	public static String		Weather_WindSpeed_Bft04;
	public static String		Weather_WindSpeed_Bft04_Short;
	public static String		Weather_WindSpeed_Bft05;
	public static String		Weather_WindSpeed_Bft05_Short;
	public static String		Weather_WindSpeed_Bft06;
	public static String		Weather_WindSpeed_Bft06_Short;
	public static String		Weather_WindSpeed_Bft07;
	public static String		Weather_WindSpeed_Bft07_Short;
	public static String		Weather_WindSpeed_Bft08;
	public static String		Weather_WindSpeed_Bft08_Short;
	public static String		Weather_WindSpeed_Bft09;
	public static String		Weather_WindSpeed_Bft09_Short;
	public static String		Weather_WindSpeed_Bft10;
	public static String		Weather_WindSpeed_Bft10_Short;
	public static String		Weather_WindSpeed_Bft11;
	public static String		Weather_WindSpeed_Bft11_Short;
	public static String		Weather_WindSpeed_Bft12;
	public static String		Weather_WindSpeed_Bft12_Short;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
