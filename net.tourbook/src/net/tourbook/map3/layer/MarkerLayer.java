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
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.render.PointPlacemarkAttributes;

import java.util.ArrayList;
import java.util.Set;

import net.tourbook.common.UI;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.map3.layer.tourtrack.TourTrackConfig;
import net.tourbook.map3.layer.tourtrack.TourTrackConfigManager;
import net.tourbook.map3.view.ICheckStateListener;
import net.tourbook.map3.view.Map3Manager;
import net.tourbook.map3.view.TVIMap3Layer;

import org.eclipse.jface.dialogs.IDialogSettings;

public class MarkerLayer extends RenderableLayer implements SelectListener, ICheckStateListener {

	public static final String	MAP3_LAYER_ID			= "POILayer";	//$NON-NLS-1$

	/**
	 * This flag keeps track of adding/removing the listener that it is not done more than once.
	 */
	private int					_lastAddRemoveAction	= -1;

	private MTClutterFilter		_clutterFilter;

	public MarkerLayer(final IDialogSettings state) {

		_clutterFilter = new MTClutterFilter();

//		addPropertyChangeListener(this);
	}

	public void createMarker(final ArrayList<TourData> _allTours) {

		removeAllRenderables();

		final PointPlacemarkAttributes ppAttributes = new PointPlacemarkAttributes();
		ppAttributes.setScale(3.0);
		ppAttributes.setLabelScale(1.0);
		ppAttributes.setLabelFont(UI.AWT_FONT_ARIAL_48);

		final TourTrackConfig config = TourTrackConfigManager.getActiveConfig();

		for (final TourData tourData : _allTours) {

			final Set<TourMarker> tourMarkerList = tourData.getTourMarkers();
			if (tourMarkerList.size() == 0) {
				continue;
			}

			// check if geo position is available
			final double[] latitudeSerie = tourData.latitudeSerie;
			final double[] longitudeSerie = tourData.longitudeSerie;
			if (latitudeSerie == null || longitudeSerie == null) {
				continue;
			}

			for (final TourMarker tourMarker : tourMarkerList) {

				// skip marker when hidden or not set
				if (tourMarker.isMarkerVisible() == false || tourMarker.getLabel().length() == 0) {
					continue;
				}

				final int serieIndex = tourMarker.getSerieIndex();

				/*
				 * check bounds because when a tour is split, it can happen that the marker serie
				 * index is out of scope
				 */
				if (serieIndex >= latitudeSerie.length) {
					continue;
				}

				/*
				 * draw tour marker
				 */

				int altitudeMode;
				double altitude = 0;
				final float[] altitudeSerie = tourData.altitudeSerie;

//				if (altitudeSerie == null) {
//
//					altitudeMode = WorldWind.CLAMP_TO_GROUND;
//
//				} else {
//
				altitude = altitudeSerie[serieIndex] + 000;
				altitudeMode = config.altitudeMode;
//				}

				final MTPointPlacemark pp = new MTPointPlacemark(Position.fromDegrees(
						latitudeSerie[serieIndex],
						longitudeSerie[serieIndex],
						altitude));

				pp.setAltitudeMode(altitudeMode);
//				pp.setEnableDecluttering(true);

				pp.setLabelText(tourMarker.getLabel());

				// set tooltip
//				pp.setValue(AVKey.DISPLAY_NAME, "Clamp to ground, Label, Semi-transparent, Audio icon");

				pp.setLineEnabled(true);

				pp.setAttributes(ppAttributes);

				addRenderable(pp);
			}
		}
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

//				ww.getSceneController().setClutterFilter(_clutterFilter);

			}

		} else {

			if (_lastAddRemoveAction != 0) {

				_lastAddRemoveAction = 0;
				ww.removeSelectListener(this);

//				ww.getSceneController().setClutterFilter(null);
			}
		}
	}

}
