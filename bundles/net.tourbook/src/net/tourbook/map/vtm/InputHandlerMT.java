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
package net.tourbook.map.vtm;

import org.oscim.core.GeoPoint;
import org.oscim.gdx.GdxMap;
import org.oscim.gdx.InputHandler;
import org.oscim.map.Map;
import org.oscim.map.ViewController;
import org.oscim.utils.Easing;

import com.badlogic.gdx.Input.Buttons;

public class InputHandlerMT extends InputHandler {

	private Map				_map;
	private ViewController	_viewport;

	/**
	 * Recenter map to the clicked position. Needs to be extended when items in the map can be
	 * selected.
	 */
	private boolean			_isReCenter;

	public InputHandlerMT(final GdxMap map) {

		super(map);

		_map = map.getMap();
		_viewport = _map.viewport();
	}

	@Override
	public boolean touchDown(final int screenX, final int screenY, final int pointer, final int button) {

		if (button == Buttons.LEFT) {
			_isReCenter = true;
		}

		return super.touchDown(screenX, screenY, pointer, button);
	}

	@Override
	public boolean touchDragged(final int screenX, final int screenY, final int pointer) {

		_isReCenter = false;

		return super.touchDragged(screenX, screenY, pointer);
	}

	@Override
	public boolean touchUp(final int screenX, final int screenY, final int pointer, final int button) {

		// !!! deactivate default behaviour !!!
		super.touchUp(screenX, screenY, pointer, button);

		if (button == Buttons.LEFT && _isReCenter) {

			_isReCenter = false;

			// get map center
			final GeoPoint mapCenter = _viewport.fromScreenPoint(screenX, screenY);

			_map.animator().animateTo(800, mapCenter, 1, true, Easing.Type.SINE_INOUT);
			_map.updateMap(false);

//			return true;
		}

		return false;
	}

}
