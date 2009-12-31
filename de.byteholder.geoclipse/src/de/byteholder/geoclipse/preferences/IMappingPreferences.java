/*******************************************************************************
 * Copyright (C) 2005, 2009 Wolfgang Schramm and Contributors
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

package de.byteholder.geoclipse.preferences;

public interface IMappingPreferences {

	final String	OFFLINE_CACHE_USE_OFFLINE				= "offLineCache.isUsed";				//$NON-NLS-1$
	final String	OFFLINE_CACHE_USE_DEFAULT_LOCATION		= "offLineCache.useSelectedLocation";	//$NON-NLS-1$
	final String	OFFLINE_CACHE_PATH						= "offLineCache.path";					//$NON-NLS-1$

	final String	OFFLINE_CACHE_PERIOD_OF_VALIDITY		= "offLineCache.periodOfValidity";		//$NON-NLS-1$
	final String	OFFLINE_CACHE_MAX_SIZE					= "offLineCache.maxCacheSize";			//$NON-NLS-1$

	final String	SHOW_MAP_TILE_INFO						= "mapTileInfo.isShowInfo";			//$NON-NLS-1$

	final String	MAP_FACTORY_IS_READ_TILE_SIZE			= "mapFactory.isReadTileSize";			//$NON-NLS-1$
	final String	MAP_FACTORY_LAST_SELECTED_MAP_PROVIDER	= "mapFactory.lastSelectedMapProvider"; //$NON-NLS-1$

	final String	THEME_FONT_LOGGING						= "Theme_Font_Logging";				//$NON-NLS-1$
}
