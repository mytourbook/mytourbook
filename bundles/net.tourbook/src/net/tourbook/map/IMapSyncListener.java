/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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
package net.tourbook.map;

import java.util.concurrent.ConcurrentHashMap;

import org.oscim.core.MapPosition;

public interface IMapSyncListener {

	/**
	 * Sync map location and zoomlevel with another map
	 * 
	 * @param newMapPosition
	 *            Map position of the other map
	 * @param lastReceivedSyncMapPositions
	 *            Map position from the previous received sync event, can be <code>null</code>
	 */
	void syncMapWithOtherMap(	MapPosition newMapPosition,
								ConcurrentHashMap<MapPosition, Long> lastReceivedSyncMapPositions);

}
