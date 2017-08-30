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

import org.oscim.core.GeoPoint;
import org.oscim.gdx.InputHandler;
import org.oscim.layers.Layer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.map.Map;
import org.oscim.map.ViewController;
import org.oscim.scalebar.MapScaleBarLayer;
import org.oscim.utils.Easing;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Buttons;

public class InputHandlerMT extends InputHandler {

	private Map25App		_mapApp;
	private Map				_map;
	private ViewController	_viewport;

	/**
	 * Recenter map to the clicked position. Needs to be extended when items in the map can be
	 * selected.
	 */
	private boolean			_isReCenter;

	private boolean			_isMouseRightButtonDown;

	public InputHandlerMT(final Map25App mapApp) {

		super(mapApp);

		_mapApp = mapApp;
		_map = mapApp.getMap();
		_viewport = _map.viewport();
	}

	@Override
	public boolean keyDown(final int keycode) {

		if (keycode == Input.Keys.ESCAPE) {

			// disable app exit
			return true;
		}

		/*
		 * Disable keys which work ONLY with openscimap
		 */
		final Map25Provider selectedMapProvider = _mapApp.getSelectedMapProvider();
		if (selectedMapProvider.tileEncoding != TileEncoding.VTM) {

			// disable themes
			switch (keycode) {
			case Input.Keys.NUM_1:
			case Input.Keys.NUM_2:
			case Input.Keys.NUM_3:
			case Input.Keys.NUM_4:
			case Input.Keys.NUM_5:

				return true;
			}
		}

		switch (keycode) {

		case Input.Keys.L:

			// toggle label

			toggle_Label_Layer();
			_map.render();

			break;

		case Input.Keys.C:

			// toggle scale

			toggle_Scale_Layer();
			_map.render();

			break;

		default:
			break;
		}

		return super.keyDown(keycode);
	}

	private void toggle_Label_Layer() {

		for (final Layer layer : _map.layers()) {

			if (layer instanceof LabelLayer) {

				layer.setEnabled(!layer.isEnabled());

				return;
			}
		}
	}

	private void toggle_Scale_Layer() {

		for (final Layer layer : _map.layers()) {

			if (layer instanceof MapScaleBarLayer) {

				layer.setEnabled(!layer.isEnabled());

				return;
			}
		}
	}

	@Override
	public boolean touchDown(final int screenX, final int screenY, final int pointer, final int button) {

		if (button == Buttons.LEFT) {

			if (!_mapApp.getAndReset_IsMapItemHit()) {

				_isReCenter = true;
			}

		} else if (button == Buttons.RIGHT) {

			_isMouseRightButtonDown = true;
		}

		return super.touchDown(screenX, screenY, pointer, button);
	}

	@Override
	public boolean touchDragged(final int screenX, final int screenY, final int pointer) {

		_isReCenter = false;

		// prevent opening context menu
		_isMouseRightButtonDown = false;

//		fireMapPosition();

		return super.touchDragged(screenX, screenY, pointer);
	}

	@Override
	public boolean touchUp(final int screenX, final int screenY, final int pointer, final int button) {

		// keep and reset state
		final boolean isMouseRightButtonDown = _isMouseRightButtonDown;
		_isMouseRightButtonDown = false;

		// !!! deactivate default behaviour !!!
		super.touchUp(screenX, screenY, pointer, button);

		if (button == Buttons.LEFT && _isReCenter) {

			_isReCenter = false;

			// get map center
			final GeoPoint mapCenter = _viewport.fromScreenPoint(screenX, screenY);

			_map.animator().animateTo(800, mapCenter, 1, true, Easing.Type.SINE_INOUT);
			_map.updateMap(false);

//			return true;

		} else if (button == Buttons.RIGHT && isMouseRightButtonDown) {

			// open context menu

			_mapApp.getMap25View().actionContextMenu(screenX, screenY);

			return true;
		}

		return false;
	}

}
