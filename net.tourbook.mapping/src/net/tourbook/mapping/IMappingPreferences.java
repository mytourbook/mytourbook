/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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

package net.tourbook.mapping;

public interface IMappingPreferences {

	final String	SHOW_TILE_INFO					= "show.tile-info";

	final String	IS_OFFLINE_CACHE				= "is.off-line.cache";
	final String	OFFLINE_CACHE_PATH				= "off-line.cache.path";

	final String	OFFLINE_LOCATION_INTERNAL		= "off-line.location.internal";
	final String	OFFLINE_LOCATION_SELECTED_PATH	= "off-line.location.selecte-path";
	final String	OFFLINE_LOCATION				= "off-line.selected.cache.path";

}
