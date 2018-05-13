/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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

import net.tourbook.map.IMapSyncListener;
import net.tourbook.map25.layer.labeling.LabelLayerMT;
import net.tourbook.map25.layer.tourtrack.TourLayer;

import org.eclipse.swt.widgets.Display;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.gdx.InputHandler;
import org.oscim.layers.Layer;
import org.oscim.map.Map;
import org.oscim.map.ViewController;
import org.oscim.scalebar.MapScaleBarLayer;
import org.oscim.utils.animation.Easing;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Buttons;

public class InputHandlerMT extends InputHandler {

	private Map25App		_mapApp;
	private Map				_map;
	private Map25View		_mapView;
	private ViewController	_viewport;

	/**
	 * Recenter map to the clicked position. Needs to be extended when items in the map can be
	 * selected.
	 */
	private boolean			_isReCenter;

	private boolean			_isMouseRightButtonDown;

	private boolean			_isShift;
	private boolean			_isCtrl;

	public InputHandlerMT(final Map25App mapApp) {

		super(mapApp);

		_mapApp = mapApp;
		_mapView = _mapApp.getMap25View();

		_map = mapApp.getMap();
		_viewport = _map.viewport();
	}

	private int getNavigatePixel() {

		int navigatePixel = 50;

		if (_isCtrl && _isShift) {

			navigatePixel = 1000;

		} else if (_isCtrl) {

			navigatePixel = 200;

		} else if (_isShift) {

			navigatePixel = 1;
		}

		if (Map25ConfigManager.useDraggedKeyboardNavigation == false) {

			// invert navigation
			navigatePixel *= -1;
		}

		return navigatePixel;
	}

	@Override
	public boolean keyDown(final int keycode) {

//		System.out.println(
//				(UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//						+ ("\tkeyDown:\t" + keycode)
////				+ ("\t: " + )
//		);
//// TODO remove SYSTEM.OUT.PRINTLN
//
//		return false;

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

		/*
		 * Zoom faster
		 */
		if (_isCtrl) {

			switch (keycode) {

			// zoom in faster
			case Input.Keys.D:
			case Input.Keys.PLUS:

				_map.animator().animateZoom(500, 2, 0, 0);
				_map.updateMap(false);
				return true;

			// zoom out faster
			case Input.Keys.A:
			case Input.Keys.MINUS:

				_map.animator().animateZoom(500, 0.5, 0, 0);
				_map.updateMap(false);
				return true;

			}
		}

		MapPosition mapPosition;

		switch (keycode) {

		// disable app exit
		case Input.Keys.ESCAPE:
			return true;

		/*
		 * Navigate
		 */

		case Input.Keys.LEFT:
			_viewport.moveMap(-getNavigatePixel(), 0);
			_map.updateMap(true);
			return true;

		case Input.Keys.RIGHT:
			_viewport.moveMap(getNavigatePixel(), 0);
			_map.updateMap(true);
			return true;

		case Input.Keys.UP:
			_viewport.moveMap(0, -getNavigatePixel());
			_map.updateMap(true);
			return true;

		case Input.Keys.DOWN:
			_viewport.moveMap(0, getNavigatePixel());
			_map.updateMap(true);
			return true;

		/*
		 * Layers
		 */

// Default keys
//		case Input.Keys.B:	// building
//		case Input.Keys.G:	// grid

		// Label
		case Input.Keys.V:

			toggle_Label_Layer();
			_map.render();

			return true;

		// Scale
		case Input.Keys.C:

			toggle_Scale_Layer();
			_map.render();

			return true;

		// Grid
		case Input.Keys.G:

			final TileGridLayerMT layerTileInfo = _mapApp.getLayer_TileInfo();

			layerTileInfo.setEnabled(!layerTileInfo.isEnabled());

			_map.render();

			return true;

		// Tour
		case Input.Keys.T:

			final TourLayer layerTour = _mapApp.getLayer_Tour();
			layerTour.setEnabled(!layerTour.isEnabled());

			_map.render();

			// update actions in UI thread
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					_mapView.enableActions();
				}
			});

			return true;

		/*
		 * These keys are processed in keyTyped(), they can be repeated !!!
		 */

		// navigate
		case Input.Keys.J: // left
		case Input.Keys.L: // right
		case Input.Keys.I: // up
		case Input.Keys.K: // down

			// zoom
		case Input.Keys.D: // in
		case Input.Keys.A: // out
		case Input.Keys.W: // fast in
		case Input.Keys.S: // fast out
		case Input.Keys.PLUS: // in
		case Input.Keys.MINUS: // out

		case Input.Keys.M: // tilt
		case Input.Keys.N: // bearing

			return true;

		/*
		 * Orientation
		 */

		// Reset bearing/tilt
		case Input.Keys.O:

			if (_isCtrl) {

				// Reset tilt

				mapPosition = _map.getMapPosition();
				mapPosition.tilt = 0;

				_map.setMapPosition(mapPosition);
				_map.render();

				_mapView.fireSyncMapEvent(mapPosition, IMapSyncListener.RESET_TILT);

			} else {

				// Reset rotation

				mapPosition = _map.getMapPosition();
				mapPosition.bearing = 0;

				_map.setMapPosition(mapPosition);
				_map.render();

				_mapView.fireSyncMapEvent(mapPosition, IMapSyncListener.RESET_BEARING);
			}

			return true;

		// set <shift> state
		case Input.Keys.SHIFT_LEFT:
		case Input.Keys.SHIFT_RIGHT:
			_isShift = true;
			break;

		// set <ctrl> state
		case Input.Keys.CONTROL_LEFT:
		case Input.Keys.CONTROL_RIGHT:
			_isCtrl = true;
			break;

		default:
			break;
		}

		return super.keyDown(keycode);
	}

	@Override
	public boolean keyTyped(final char character) {

		/**
		 * Repeated key can ONLY be a CHARACTER and NOT other keys, e.g. left, right, up, down,...
		 */

//		System.out.println(
//				(UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//						+ ("\t_isCtrl:\t" + _isCtrl)
//						+ ("\t_isShift:\t" + _isShift)
//						+ ("\t" + character)
//						+ ("\t\tkeyTyped:\t" + Integer.toString(character))
////				+ ("\t: " + )
//		);
//// TODO remove SYSTEM.OUT.PRINTLN

		final MapPosition mapPosition = _map.getMapPosition();

		final float minTilt = _viewport.getMinTilt();
		final float maxTilt = _viewport.getMaxTilt();
		float tiltDiff = (maxTilt - minTilt) / 100;

		double bearingDiff;

		switch (character) {

		/*
		 * Navigate
		 */

		// move left
		case 'J':
		case 'j':
		case 10: // ctrl + J
			_viewport.moveMap(-getNavigatePixel(), 0);
			_map.updateMap(true);
			return true;

		// move right
		case 'L':
		case 'l':
		case 12: // ctrl + L
			_viewport.moveMap(getNavigatePixel(), 0);
			_map.updateMap(true);
			return true;

		// move up
		case 'I':
		case 'i':
		case 9: // ctrl + I
			_viewport.moveMap(0, -getNavigatePixel());
			_map.updateMap(true);
			return true;

		// move down
		case 'K':
		case 'k':
		case 11: // ctrl + K
			_viewport.moveMap(0, getNavigatePixel());
			_map.updateMap(true);
			return true;


		/*
		 * Zoom
		 */

		// zoom in
		case '+':
		case 'd':
			_viewport.scaleMap(1.05f, 0, 0);
			_map.updateMap(true);
			return true;

		// zoom out
		case '-':
		case 'a':
			_viewport.scaleMap(0.95f, 0, 0);
			_map.updateMap(true);
			return true;

		case 's':
			_map.animator().animateZoom(500, 0.5, 0, 0);
			_map.updateMap(false);
			return true;

		case 'w':
			_map.animator().animateZoom(500, 2, 0, 0);
			_map.updateMap(false);
			return true;

		/*
		 * Orientation
		 */

		// rotate right
		case 'm':
		case 'M':

			bearingDiff = character == 'M' ? 0.1 : 5;

			_viewport.setRotation(mapPosition.bearing + bearingDiff);

			_map.updateMap(true);

			return true;

		// rotate left
		case 'n':
		case 'N':

			bearingDiff = character == 'N' ? 0.1 : 5;

			_viewport.setRotation(mapPosition.bearing - bearingDiff);

			_map.updateMap(true);

			return true;

		// Ctrl + M
		case 13:

			// tilt up

			if (_isShift) {
				tiltDiff /= 10;
			}

			_viewport.setTilt(mapPosition.tilt - tiltDiff);

			_map.updateMap(true);

			return true;

		// Ctrl + N
		case 14:

			// tilt down

			if (_isShift) {
				tiltDiff /= 10;
			}

			_viewport.setTilt(mapPosition.tilt + tiltDiff);

			_map.updateMap(true);

			return true;

		default:
			break;
		}

		return super.keyTyped(character);
	}

	@Override
	public boolean keyUp(final int keycode) {

//		System.out.println(
//				(UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//						+ ("\tkeyUp:\t" + keycode)
////				+ ("\t: " + )
//		);
//// TODO remove SYSTEM.OUT.PRINTLN
//
//		return false;

		switch (keycode) {

		// <shift>
		case Input.Keys.SHIFT_LEFT:
		case Input.Keys.SHIFT_RIGHT:
			_isShift = false;
			break;

		// <ctrl>
		case Input.Keys.CONTROL_LEFT:
		case Input.Keys.CONTROL_RIGHT:
			_isCtrl = false;
			break;

		default:
			break;
		}

		return super.keyUp(keycode);
	}

	@Override
	public boolean mouseMoved(final int screenX, final int screenY) {

		/*
		 * Set map geoposition in the app status line
		 */
		final GeoPoint mapGeoPoint = _map.viewport().fromScreenPoint(screenX, screenY);

		final MapPosition mapPosition = new MapPosition();
		_map.viewport().getMapPosition(mapPosition);

		_mapView.onMapPosition(mapGeoPoint, mapPosition.zoomLevel);

		return super.mouseMoved(screenX, screenY);
	}

	private void toggle_Label_Layer() {

		for (final Layer layer : _map.layers()) {

			if (layer instanceof LabelLayerMT) {

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

			_mapView.actionContextMenu(screenX, screenY);

			return true;
		}

		return false;
	}

}
