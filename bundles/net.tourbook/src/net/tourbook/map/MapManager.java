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

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.ui.part.ViewPart;
import org.oscim.core.MapPosition;

public class MapManager {

	private final static ListenerList _allMapSyncListener = new ListenerList(ListenerList.IDENTITY);

	public static void addMapSyncListener(final IMapSyncListener listener) {
		_allMapSyncListener.add(listener);
	}

	public static void fireSyncMapEvent(final MapPosition mapPosition,
										final ViewPart viewPart,
										final int positionFlags) {

		final Object[] allListeners = _allMapSyncListener.getListeners();

		for (final Object listener : allListeners) {
			((IMapSyncListener) (listener)).syncMapWithOtherMap(mapPosition, viewPart, positionFlags);
		}
	}

	public static boolean isInOwnMapPosition(	final ConcurrentHashMap<MapPosition, Long> ownMapPositions,
												final ConcurrentHashMap<MapPosition, Long> otherMapPositions) {

		if (otherMapPositions == null) {
			return false;
		}

		final long currentTime = System.currentTimeMillis();
		final int keepingTime = 5000;

		final HashSet<MapPosition> removeOwnPositions = new HashSet<>();
		final HashSet<MapPosition> removeOtherPositions = new HashSet<>();
		final ConcurrentHashMap<MapPosition, Long> keepOtherPositions = new ConcurrentHashMap<>();

		boolean returnValue = false;
		try {

			for (final Entry<MapPosition, Long> otherEntry : otherMapPositions.entrySet()) {

				final MapPosition otherPos = otherEntry.getKey();
				final Long otherValue = otherEntry.getValue();

				final long timeDiff = currentTime - otherValue;

				if (timeDiff > keepingTime) {

					removeOtherPositions.add(otherPos);

				} else {

					keepOtherPositions.put(otherPos, otherValue);
				}
			}

			outerLoop:

			for (final Entry<MapPosition, Long> ownEntry : ownMapPositions.entrySet()) {

				final MapPosition ownKey = ownEntry.getKey();

				final long timeDiff = currentTime - ownEntry.getValue();

				if (timeDiff > keepingTime) {

					removeOwnPositions.add(ownKey);

				} else {

					for (final MapPosition otherKey : keepOtherPositions.keySet()) {

						if (ownKey.x == otherKey.x && ownKey.y == otherKey.y && ownKey.scale == otherKey.scale) {

							/*
							 * This is a subsequent event which was fired from here but the other
							 * map posted additional sync events because of an animation
							 */

							returnValue = true;

							break outerLoop;
						}
					}
				}
			}

		} finally {

			/*
			 * remove old positions
			 */
			for (final MapPosition mapPosition : removeOwnPositions) {
				ownMapPositions.remove(mapPosition);
			}
			for (final MapPosition mapPosition : removeOtherPositions) {
				otherMapPositions.remove(mapPosition);
			}
		}

		return returnValue;
	}

	public static void removeMapSyncListener(final IMapSyncListener listener) {
		_allMapSyncListener.remove(listener);
	}
}
