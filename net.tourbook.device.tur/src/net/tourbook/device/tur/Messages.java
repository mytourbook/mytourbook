/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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
