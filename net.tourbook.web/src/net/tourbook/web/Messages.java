/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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
package net.tourbook.web;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	private static final String	BUNDLE_NAME	= "net.tourbook.web.messages";				//$NON-NLS-1$

	public static String		PrefPage_Web_Checkbox_CustomWebBrowser;
	public static String		PrefPage_Web_Checkbox_CustomWebBrowser_Tooltip_Linux;
	public static String		PrefPage_Web_Checkbox_CustomWebBrowser_Tooltip_Win;

	public static String		PrefPage_Web_Label_CustomWebBrowser_Hint_Linux;
	public static String		PrefPage_Web_Label_CustomWebBrowser_Hint_Win;
	public static String		PrefPage_Web_Label_CustomWebBrowser_Info;

	public static String		Web_Page_ContentLoading;
	public static String		Web_Page_Search_Title;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
