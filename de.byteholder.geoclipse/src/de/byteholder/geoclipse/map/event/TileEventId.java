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

package de.byteholder.geoclipse.map.event;

public enum TileEventId {

	/**
	 * tile is put into the loading/painting queue
	 */
	TILE_IS_QUEUED,

	/**
	 * tile gets loading from a url
	 */
	TILE_START_LOADING,

	/**
	 * tile loading ended
	 */
	TILE_END_LOADING,

	/**
	 * error occured when loading
	 */
	TILE_ERROR_LOADING,

	/**
	 * tile queue is reset, tile parameter is <code>null</code>
	 */
	TILE_RESET_QUEUES,

	/////////////////////////////////////////////

	/**
	 * painting started
	 */
	SRTM_PAINTING_START,

	/**
	 * painting ended
	 */
	SRTM_PAINTING_END,

	/**
	 * error occured when painting a tile
	 */
	SRTM_PAINTING_ERROR,

	/////////////////////////////////////////////

	/**
	 * start loading srtm data
	 */
	SRTM_DATA_START_LOADING,

	/**
	 * end loading srtm data
	 */
	SRTM_DATA_END_LOADING,

	/**
	 * monitor info when srtm data are loaded
	 */
	SRTM_DATA_LOADING_MONITOR,

	/**
	 * error occured when downloading srtm data
	 */
	SRTM_DATA_ERROR_LOADING,

	//
	;

	public static final int	COLUMN_WIDTH_EVENT_ID	= 19;

	@Override
	public String toString() {

		// create text string which has the same length

		//                              01234567890123456789		
		return super.toString().concat("                    ").substring(0, COLUMN_WIDTH_EVENT_ID);
	}
}
