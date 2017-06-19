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

public class Map25Manager {

	private static boolean			_isDebugViewVisible;
	private static Map25DebugView	_map25DebugView;

	/**
	 * @return Returns the map vtm debug view when it is visible, otherwise <code>null</code>
	 */
	public static Map25DebugView getMap25DebugView() {

		if (_map25DebugView != null && _isDebugViewVisible) {
			return _map25DebugView;
		}

		return null;
	}

	public static boolean isDebugViewVisible() {
		return _isDebugViewVisible;
	}

	static void setDebugView(final Map25DebugView map25DebugView) {
		_map25DebugView = map25DebugView;
	}

	static void setDebugViewVisible(final boolean isDebugVisible) {
		_isDebugViewVisible = isDebugVisible;
	}

}
