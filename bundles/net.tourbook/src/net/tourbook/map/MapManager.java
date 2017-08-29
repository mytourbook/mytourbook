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

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap.KeySetView;

import org.eclipse.core.runtime.ListenerList;
import org.oscim.core.MapPosition;

public class MapManager {

	private final static ListenerList _allMapSyncListener = new ListenerList(ListenerList.IDENTITY);

	public static void addMapSyncListener(final IMapSyncListener listener) {
		_allMapSyncListener.add(listener);
	}

	public static void fireSyncMapEvent(final MapPosition mapPosition,
										final ConcurrentHashMap<MapPosition, Long> lastReceivedSyncMapPosition) {

		final Object[] allListeners = _allMapSyncListener.getListeners();

		for (final Object listener : allListeners) {
			((IMapSyncListener) (listener)).syncMapWithOtherMap(mapPosition, lastReceivedSyncMapPosition);
		}
	}

	public static boolean isOwnMapPosition(	final ConcurrentHashMap<MapPosition, Long> ownMapPositions,
											final ConcurrentHashMap<MapPosition, Long> otherMapPositions) {

		if (otherMapPositions == null) {
			return false;
		}

		final long currentTime = System.currentTimeMillis();

		final HashSet<MapPosition> removePositions = new HashSet<>();

		final KeySetView<MapPosition, Long> allOtherPos = otherMapPositions.keySet();

		for (final Entry<MapPosition, Long> ownEntry : ownMapPositions.entrySet()) {

			final MapPosition ownPos = ownEntry.getKey();

			final long timeDiff = currentTime - ownEntry.getValue();

//			System.out.println(
//					(UI.timeStampNano() + " [" + "] ") + ("\ttimeDiff:" + timeDiff) + ("\tsize:" + ownMapPositions
//							.size()));
//			// TODO remove SYSTEM.OUT.PRINTLN

			if (timeDiff > 1000) {

				removePositions.add(ownPos);

			} else {

				final double ownX = ownPos.x;
				final double ownY = ownPos.y;
				final double ownScale = ownPos.scale;

				for (final MapPosition otherPos : allOtherPos) {

					if (ownX == otherPos.x && ownY == otherPos.y && ownScale == otherPos.scale) {

						/*
						 * This is a subsequent event which was fired from here but the other map
						 * posted additional sync events because of an animation
						 */

						return true;
					}
				}
			}
		}

//		System.out.println();
//		System.out.println();
//		System.out.println();
		/*
		 * remove old positions from the own set
		 */
		for (final MapPosition mapPosition : removePositions) {
			ownMapPositions.remove(mapPosition);
		}

		return false;
	}

	public static void removeMapSyncListener(final IMapSyncListener listener) {
		_allMapSyncListener.remove(listener);
	}
}
