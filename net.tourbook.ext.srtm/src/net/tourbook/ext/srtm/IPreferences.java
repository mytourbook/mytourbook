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
package net.tourbook.ext.srtm;

public interface IPreferences {

	String	SRTM_USE_DEFAULT_DATA_FILEPATH	= "srtm.use.default.data-filepath"; //$NON-NLS-1$
	String	SRTM_DATA_FILEPATH				= "srtm.data.filepath";			//$NON-NLS-1$
	String  SRTM_COLORS_PROFILES            = "srtm.colors.profiles";			//$NON-NLS-1$
	String  SRTM_COLORS_ACTUAL_PROFILE      = "srtm.colors.actual.profile";			//$NON-NLS-1$
	String  SRTM_RESOLUTION                 = "srtm.resolution";			//$NON-NLS-1$
	String  SRTM_RESOLUTION_VERY_FINE       = "srtm.resolution.very-fine";			//$NON-NLS-1$
	String  SRTM_RESOLUTION_FINE            = "srtm.resolution.fine";			//$NON-NLS-1$
	String  SRTM_RESOLUTION_ROUGH           = "srtm.resolution.rough";			//$NON-NLS-1$
	String  SRTM_RESOLUTION_VERY_ROUGH      = "srtm.resolution.very-rough";			//$NON-NLS-1$
	String  SRTM_SHADOW                     = "srtm.shadow";		//$NON-NLS-1$
}
