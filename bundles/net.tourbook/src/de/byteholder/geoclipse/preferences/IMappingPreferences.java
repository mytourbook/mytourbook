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

	final String				OFFLINE_CACHE_USE_OFFLINE				= "OffLineCache_IsUsed";				//$NON-NLS-1$
	final String				OFFLINE_CACHE_USE_DEFAULT_LOCATION		= "OffLineCache_UseSelectedLocation";	//$NON-NLS-1$
	final String				OFFLINE_CACHE_PATH						= "OffLineCache_Path";					//$NON-NLS-1$

	final String				OFFLINE_CACHE_PERIOD_OF_VALIDITY		= "OffLineCache_PeriodOfValidity";		//$NON-NLS-1$
	final String				OFFLINE_CACHE_MAX_SIZE					= "OffLineCache_MaxCacheSize";			//$NON-NLS-1$

	final String				SHOW_MAP_TILE_INFO						= "MapTileInfo_IsShowInfo";			//$NON-NLS-1$

	final String				MAP_FACTORY_IS_READ_TILE_SIZE			= "MapFactory_IsReadTileSize";			//$NON-NLS-1$
	final String				MAP_FACTORY_LAST_SELECTED_MAP_PROVIDER	= "MapFactory_LastSelectedMapProvider"; //$NON-NLS-1$

	final String				THEME_FONT_LOGGING						= "Theme_Font_Logging";				//$NON-NLS-1$

	public static final String	MAP_PROVIDER_SORT_ORDER					= "MapProvider_SortOrder";				//$NON-NLS-1$
	public static final String	MAP_PROVIDER_TOGGLE_LIST				= "MapProvider_ToggleList";			//$NON-NLS-1$

}
