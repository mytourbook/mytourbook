/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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

public interface IPreferences {
	
	// proxy method
	static final String NO_PROXY							= "no-proxy"; 			//$NON-NLS-1$
	static final String PROXY								= "use-http-proxy"; 	//$NON-NLS-1$
	static final String SOCKS_PROXY							= "use-socks-proxy"; 	//$NON-NLS-1$
	static final String SYSTEM_PROXY						= "use-system-settings"; //$NON-NLS-1$
	
	static final String	PROXY_METHOD						= "proxy.method";			//$NON-NLS-1$
	
	// proxy addresses
	static final String	PROXY_SERVER_ADDRESS  				= "proxy.proxy_server";		//$NON-NLS-1$
	static final String	PROXY_SERVER_PORT		  			= "proxy.proxy_port";		//$NON-NLS-1$

	// proxy credentials
	static final String	PROXY_USER  						= "proxy.proxy_user";		//$NON-NLS-1$
	static final String	PROXY_PWD		  					= "proxy.proxy_pwd";		//$NON-NLS-1$
	
	// socks proxy addresses
	static final String	SOCKS_PROXY_SERVER_ADDRESS  		= "proxy.socks_proxy_server"; //$NON-NLS-1$
	static final String	SOCKS_PROXY_SERVER_PORT		  		= "proxy.socks_proxy_port";	 //$NON-NLS-1$
}
