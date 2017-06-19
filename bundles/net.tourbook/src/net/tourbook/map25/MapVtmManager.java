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
package net.tourbook.map25;

public class MapVtmManager {

	private static boolean			_isDebugViewVisible;
	private static MapVtmDebugView	_mapVtmDebugView;

	/**
	 * @return Returns the map vtm debug view when it is visible, otherwise <code>null</code>
	 */
	public static MapVtmDebugView getMapVtmDebugView() {

		if (_mapVtmDebugView != null && _isDebugViewVisible) {
			return _mapVtmDebugView;
		}

		return null;
	}

	public static boolean isDebugViewVisible() {
		return _isDebugViewVisible;
	}

	static void setDebugView(final MapVtmDebugView mapVtmDebugView) {
		_mapVtmDebugView = mapVtmDebugView;
	}

	static void setDebugViewVisible(final boolean isDebugVisible) {
		_isDebugViewVisible = isDebugVisible;
	}

}
