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

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.Offset;
import gov.nasa.worldwind.render.PointPlacemarkAttributes;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Set;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.map3.layer.tourtrack.TourTrackConfig;
import net.tourbook.map3.layer.tourtrack.TourTrackConfigManager;
import net.tourbook.map3.view.ICheckStateListener;
import net.tourbook.map3.view.Map3Manager;
import net.tourbook.map3.view.TVIMap3Layer;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jface.dialogs.IDialogSettings;

public class MarkerLayer extends RenderableLayer implements SelectListener, ICheckStateListener {

	public static final String	MAP3_LAYER_ID			= "MarkerLayer";	//$NON-NLS-1$

	/**
	 * This flag keeps track of adding/removing the listener that it is not done more than once.
	 */
	private int					_lastAddRemoveAction	= -1;

	private MarkerPlacemark		_previousHoveredMarker;

	public MarkerLayer() {}

	public void createMarker(final ArrayList<TourData> allTours) {

		removeAllRenderables();

		final PointPlacemarkAttributes ppAttributes = new PointPlacemarkAttributes();
//		ppAttributes.setLabelScale(1.2);
		ppAttributes.setLabelFont(UI.AWT_FONT_ARIAL_14);
		ppAttributes.setLabelOffset(new Offset(34.0, 9.0, AVKey.PIXELS, AVKey.PIXELS));

//		ppAttributes.setLabelMaterial(new Material(new Color(0xff, 0x0, 0x0, 0x80)));

		try {

			final URL url = TourbookPlugin.getDefault().getBundle().getEntry("/images/map3/map3-marker.png");
			final String fileURL = FileLocator.toFileURL(url).toString();

			ppAttributes.setScale(0.7);
			ppAttributes.setImageAddress(fileURL);
			ppAttributes.setImageOffset(new Offset(16.0, -0.0, AVKey.PIXELS, AVKey.PIXELS));

		} catch (final IOException e) {
			// ignore
			StatusUtil.log(e);
		}

		final TourTrackConfig config = TourTrackConfigManager.getActiveConfig();

		for (final TourData tourData : allTours) {

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
				double absoluteAltitude = 0;
				final float[] altitudeSerie = tourData.altitudeSerie;

				if (altitudeSerie == null) {

					altitudeMode = WorldWind.CLAMP_TO_GROUND;

				} else {

					altitudeMode = config.altitudeMode;
					absoluteAltitude = altitudeSerie[serieIndex];// + 300;
				}

				final MarkerPlacemark markerPl = new MarkerPlacemark(Position.fromDegrees(
						latitudeSerie[serieIndex],
						longitudeSerie[serieIndex],
						absoluteAltitude));

				markerPl.setAltitudeMode(altitudeMode);
				markerPl.setEnableDecluttering(true);

//				markerPl.setLabelText(UI.SPACE);
				markerPl.setLabelText(tourMarker.getLabel());

				markerPl.setLineEnabled(true);

				markerPl.setAttributes(ppAttributes);

				addRenderable(markerPl);
			}
		}
	}

	public void onModifyConfig(final ArrayList<TourData> allTours) {

		final TourTrackConfig trackConfig = TourTrackConfigManager.getActiveConfig();

		if (trackConfig.isRecreateTracks()) {

			// track data has changed

			Map3Manager.getMap3View().showAllTours(false);

		} else {

			createMarker(allTours);

			// ensure marker modifications are redrawn
//			Map3Manager.getWWCanvas().redraw();
			Map3Manager.redrawMap();
		}
	}

	@Override
	public void onSetCheckState(final TVIMap3Layer tviMap3Layer) {

		setupWWSelectionListener(tviMap3Layer.isLayerVisible);
	}

	private void resetPreviousHoveredMarker() {

		if (_previousHoveredMarker != null) {

			// reset previous hovered marker

//			_previousHoveredMarker.setLabelText(null);

			_previousHoveredMarker = null;
		}
	}

	public void saveState(final IDialogSettings state) {

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

//		// check if mouse is consumed
//		if (event.getMouseEvent() != null && event.getMouseEvent().isConsumed()) {
//			return;
//		}
//
//		// prevent actions when context menu is visible
//		if (Map3Manager.getMap3View().isContextMenuVisible()) {
//			return;
//		}
//
//		final String eventAction = event.getEventAction();
//
//		if (eventAction == SelectEvent.HOVER) {
//
//			// not yet used
//
//		} else {
//
//			final PickedObject pickedObject = event.getTopPickedObject();
//
////			System.out.println(UI.timeStampNano()
////					+ " ["
////					+ getClass().getSimpleName()
////					+ "] \teventAction: "
////					+ eventAction
////					+ ("\t" + pickedObject));
////			// TODO remove SYSTEM.OUT.PRINTLN
//
//			if (pickedObject != null && pickedObject.getObject() instanceof MarkerPlacemark) {
//
//				final MarkerPlacemark hoveredMarker = (MarkerPlacemark) pickedObject.getObject();
//
//				if (hoveredMarker == _previousHoveredMarker) {
//
//					// do nothing
//
//					return;
//
//				} else {
//
//					resetPreviousHoveredMarker();
//
//					// set new hovered marker
//
//					hoveredMarker.setLabelText(hoveredMarker.markerText);
//
//					_previousHoveredMarker = hoveredMarker;
//
//					return;
//				}
//			}
//		}
//
//		// ensure hovered marker is reset
//		resetPreviousHoveredMarker();
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
