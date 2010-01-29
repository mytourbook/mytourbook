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
/**
 * @author Alfred Barten
 */
package net.tourbook.ext.srtm.download;

public final class DownloadETOPO extends DownloadResource {

	private final static String	urlBase	= "http://www.ngdc.noaa.gov/mgg/global/relief/ETOPO5/TOPO/ETOPO5/"; //$NON-NLS-1$

	public static void get(final String remoteFileName, final String localFilePathName) throws Exception {
//		get(urlBase, remoteFileName, localFilePathName);
		HTTPDownloader.get(urlBase, remoteFileName, localFilePathName);
	}
}
