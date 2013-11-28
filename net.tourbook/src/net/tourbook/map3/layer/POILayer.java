/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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
package net.tourbook.map3.layer;

import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.pick.PickedObject;
import net.tourbook.map3.layer.tourtrack.TourTrackConfigManager;
import net.tourbook.map3.view.ICheckStateListener;
import net.tourbook.map3.view.Map3Manager;
import net.tourbook.map3.view.TVIMap3Layer;

import org.eclipse.jface.dialogs.IDialogSettings;

public class POILayer extends RenderableLayer implements SelectListener, ICheckStateListener {

	public static final String	MAP3_LAYER_ID			= "POILayer";	//$NON-NLS-1$

	/**
	 * This flag keeps track of adding/removing the listener that it is not done more than once.
	 */
	private int					_lastAddRemoveAction	= -1;

	public POILayer(final IDialogSettings state) {

//		addPropertyChangeListener(this);
	}

	@Override
	public void onSetCheckState(final TVIMap3Layer tviMap3Layer) {

		setupWWSelectionListener(tviMap3Layer.isLayerVisible);
	}

	public void saveState(final IDialogSettings state) {

		TourTrackConfigManager.saveState();
	}

	/**
	 * This listener is set in set {@link #setupWWSelectionListener(boolean)}
	 * <p>
	 * {@inheritDoc}
	 * 
	 * @see gov.nasa.worldwind.event.SelectListener#selected(gov.nasa.worldwind.event.SelectEvent)
	 */
	@Override
	public void selected(final SelectEvent event) {

		// check if mouse is consumed
		if (event.getMouseEvent() != null && event.getMouseEvent().isConsumed()) {
			return;
		}

		// prevent actions when context menu is visible
		if (Map3Manager.getMap3View().isContextMenuVisible()) {
			return;
		}

		final String eventAction = event.getEventAction();

//		System.out.println(UI.timeStampNano() + " [" + getClass().getSimpleName() + "] \teventAction: " + eventAction);
//		// TODO remove SYSTEM.OUT.PRINTLN

		if (eventAction == SelectEvent.HOVER) {

			// not yet used

		} else {

			final PickedObject pickedObject = event.getTopPickedObject();

		}
	}

	private void setupWWSelectionListener(final boolean isLayerVisible) {

		final WorldWindowGLCanvas ww = Map3Manager.getWWCanvas();

		if (isLayerVisible) {

			if (_lastAddRemoveAction != 1) {

				_lastAddRemoveAction = 1;
				ww.addSelectListener(this);
			}

		} else {

			if (_lastAddRemoveAction != 0) {

				_lastAddRemoveAction = 0;
				ww.removeSelectListener(this);
			}
		}
	}

}
