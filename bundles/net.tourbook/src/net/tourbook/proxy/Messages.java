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
package net.tourbook.proxy;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	private static final String	BUNDLE_NAME	= "net.tourbook.proxy.messages";			//$NON-NLS-1$

	public static String        prefPage_proxy_radio_noProxy;
	public static String		prefPage_proxy_radio_systemProxy;
	public static String 		prefPage_proxy_radio_Proxy;
	public static String        prefPage_proxy_radio_socksProxy;
	
	public static String		systemProxyInformationDeactivated;
	public static String		systemProxyInformationDeactivatedToolTip;
	public static String		systemProxyInformationActivated;
	
	public static String		http_ProxyCredentials;
	public static String		http_ProxyCredentialsToolTip;
	
	public static String        prefPage_proxy_ProxyServer;
	public static String        prefPage_proxy_ProxyPort;
	public static String        prefPage_proxy_ProxyUser;
	public static String        prefPage_proxy_ProxyPassword;
	public static String        prefPage_proxy_SocksProxyServer;
	public static String        prefPage_proxy_SocksProxyPort;
	
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}

}
