/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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
package net.tourbook.map3.layer.tourtrack;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Highlightable;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.Path;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.ShapeAttributes;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;

import net.tourbook.common.UI;
import net.tourbook.common.color.IMapColorProvider;
import net.tourbook.data.TourData;
import net.tourbook.map2.view.IDiscreteColors;
import net.tourbook.map3.Messages;
import net.tourbook.map3.view.ICheckStateListener;
import net.tourbook.map3.view.Map3Manager;
import net.tourbook.map3.view.TVIMap3Layer;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.graphics.RGB;

/**
 */
public class TourTrackLayer extends RenderableLayer implements SelectListener, ICheckStateListener {

	public static final String			MAP3_LAYER_ID			= "TourTrackLayer"; //$NON-NLS-1$

	private IDialogSettings				_state;

	private final TourPositionColors	_tourPositionColors;
	private IMapColorProvider				_colorProvider;

	private final TourTrackConfig		_trackConfig;

	private ITrackPath					_lastPickedTourTrack;
	private Integer						_lastPickPositionIndex;

	private boolean						_selectedTrack;

	/**
	 * This flag keeps track of adding/removing the listener that it is not done more than once.
	 */
	private int							_lastAddRemoveAction	= -1;

	public TourTrackLayer(final IDialogSettings state) {

		_state = state;

		_trackConfig = new TourTrackConfig(state);
		_tourPositionColors = new TourPositionColors();

		addPropertyChangeListener(this);
	}

	/**
	 * Create a path for each tour.
	 * 
	 * @param allTours
	 * @return
	 */
	public ArrayList<TourMap3Position> createTrackPaths(final ArrayList<TourData> allTours) {

//		final long start = System.currentTimeMillis();

		removeAllRenderables();

		final boolean isAbsoluteAltitudeMode = _trackConfig.altitudeMode == WorldWind.ABSOLUTE;

		final int altitudeOffset = isAbsoluteAltitudeMode && _trackConfig.isAbsoluteOffset
				? _trackConfig.altitudeOffsetDistance
				: 0;

		final ArrayList<TourMap3Position> allPositions = new ArrayList<TourMap3Position>();

		for (final TourData tourData : allTours) {

			final double[] latSerie = tourData.latitudeSerie;
			final double[] lonSerie = tourData.longitudeSerie;

			if (latSerie == null) {
				continue;
			}

			final float[] altiSerie = tourData.altitudeSerie;
			final float[] dataSerie = getDataSerie(tourData);

			/*
			 * create positions for all slices
			 */
			final ArrayList<TourMap3Position> trackPositions = new ArrayList<TourMap3Position>();

			for (int serieIndex = 0; serieIndex < latSerie.length; serieIndex++) {

				final double lat = latSerie[serieIndex];
				final double lon = lonSerie[serieIndex];

				float altitude = 0;
				if (altiSerie != null) {
					altitude = altiSerie[serieIndex] + altitudeOffset;
				}

				float dataSerieValue = 0;
				if (dataSerie != null) {
					dataSerieValue = dataSerie[serieIndex];
				}

				final TourMap3Position trackPosition = new TourMap3Position(
						LatLon.fromDegrees(lat, lon),
						altitude,
						dataSerieValue);

				if (_colorProvider instanceof IDiscreteColors) {

					final IDiscreteColors discreteColorProvider = (IDiscreteColors) _colorProvider;

					trackPosition.colorValue = discreteColorProvider.getColorValue(tourData, serieIndex);
				}

				trackPositions.add(trackPosition);
			}

			/*
			 * create one path for each tour
			 */
			TourTrack tourTrack;
			ITrackPath trackPath;

			if (_trackConfig.pathResolution == TourTrackConfig.PATH_RESOLUTION_MULTI_VIEWPORT) {

				trackPath = new TrackPathResolutionViewport(trackPositions);

			} else if (_trackConfig.pathResolution == TourTrackConfig.PATH_RESOLUTION_MULTI_ALL) {

				trackPath = new TrackPathResolutionFewer(trackPositions);

			} else {

				// default == all track positions
				trackPath = new TrackPathResolutionAll(trackPositions);
			}

			tourTrack = new TourTrack(trackPath, tourData, trackPositions, _colorProvider);

			trackPath.setTourTrack(tourTrack);

			if (trackPath instanceof Path) {

				final Path path = (Path) trackPath;

				setPathAttributes(path);
				addRenderable(path);

				// keep all positions which is used to find the outline for ALL selected tours
				allPositions.addAll(trackPositions);
			}
		}

		_tourPositionColors.updateColors(allTours);

//		System.out.println(UI.timeStampNano() + " showTour\t" + (System.currentTimeMillis() - start) + " ms");
//		// TODO remove SYSTEM.OUT.PRINTLN

		return allPositions;
	}

	public TourTrackConfig getConfig() {
		return _trackConfig;
	}

	/**
	 * Set the data serie which is painted
	 * 
	 * @param tourData
	 */
	private float[] getDataSerie(final TourData tourData) {

//		final ILegendProvider legendProvider = _tourPaintConfig.getLegendProvider();
//		if (legendProvider == null) {
//			_dataSerie = null;
//			return;
//		}

//		final ILegendProvider colorProvider = _tourPositionColors.getColorProvider();

		switch (_colorProvider.getMapColorId()) {
		case Altitude:
			return tourData.altitudeSerie;

		case Gradient:
			return tourData.getGradientSerie();

		case Pace:
			return tourData.getPaceSerieSeconds();

		case Pulse:
			return tourData.pulseSerie;

		case Speed:
			return tourData.getSpeedSerie();

		case HrZone:
			return tourData.pulseSerie;

		default:
			return tourData.altitudeSerie;
		}
	}

	@Override
	public String getName() {
		return Messages.TourTrack_Layer_Name;
	}

	public void onModifyConfig() {

		if (_trackConfig.isRecreateTracks) {

			// track data has changed

			Map3Manager.getMap3View().showAllTours(false);

		} else {

			for (final Renderable renderable : getRenderables()) {

				if (renderable instanceof Path) {
					setPathAttributes((Path) renderable);
				}
			}

			// ensure path modifications are redrawn
			Map3Manager.getWWCanvas().redraw();
		}
	}

	@Override
	public void onSetCheckState(final TVIMap3Layer tviMap3Layer) {

		setupWWSelectionListener(tviMap3Layer.isLayerVisible);
	}

	@Override
	public void propertyChange(final PropertyChangeEvent propEvent) {

		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] \t")
				+ propEvent.getPropertyName()
				+ " \t"
				+ propEvent);
		// TODO remove SYSTEM.OUT

		if (propEvent.getPropertyName().equals(Map3Manager.PROPERTY_NAME_ENABLED)) {

			// layer is set to visible/hidden

			final boolean isLayerVisible = propEvent.getNewValue().equals(Boolean.TRUE);

			setupWWSelectionListener(isLayerVisible);
		}
	}

	public void saveState() {

		_trackConfig.saveState(_state);
	}

	/**
	 * This listener is set in
	 * net.tourbook.map3.layer.tourtrack.TourTrackLayerWithPaths.setupWWSelectionListener(boolean)
	 * <p>
	 * (non-Javadoc)
	 * 
	 * @see gov.nasa.worldwind.event.SelectListener#selected(gov.nasa.worldwind.event.SelectEvent)
	 */
	@Override
	public void selected(final SelectEvent event) {

		if (event.getMouseEvent() != null && event.getMouseEvent().isConsumed()) {
			return;
		}

		final PickedObject topPickedObject = event.getTopPickedObject();

		final String topObjectText = topPickedObject == null ? //
				"null"
				: topPickedObject.getClass().getSimpleName()
						+ "\t"
						+ topPickedObject.getObject().getClass().getSimpleName();

		final StringBuilder sb = new StringBuilder();
		sb.append(UI.timeStampNano() + " [" + getClass().getSimpleName() + "] \t");
		sb.append("topObject: " + topObjectText);

		ITrackPath pickedTrackPath = null;
		Integer pickPositionIndex = null;

		if (topPickedObject != null && topPickedObject.getObject() instanceof ITrackPath) {

			pickedTrackPath = (ITrackPath) topPickedObject.getObject();

			final Object pickOrdinal = topPickedObject.getValue(AVKey.ORDINAL);
			pickPositionIndex = (Integer) pickOrdinal;

			sb.append("\t" + pickedTrackPath);
			sb.append("\tpickIndex: " + pickPositionIndex);
		}

		// Ignore hover and rollover events. We're only interested in mouse pressed and mouse clicked events.
//        String eventAction = event.getEventAction();
//		if (eventAction == SelectEvent.HOVER || eventAction == SelectEvent.ROLLOVER)
//            return;

		selected_WithAttributeColor(pickedTrackPath);
		selected_WithPositionColor(pickedTrackPath, pickPositionIndex);

		_lastPickedTourTrack = pickedTrackPath;
		_lastPickPositionIndex = pickPositionIndex;

//		System.out.println(sb.toString());
//		// TODO remove SYSTEM.OUT.PRINTLN
	}

	private void selected_WithAttributeColor(final ITrackPath pickedTourTrack) {

		if (_lastPickedTourTrack == pickedTourTrack) {
			// same tour as before
			return;
		}

		// Turn off highlight if on.
		if (_lastPickedTourTrack != null) {
			_lastPickedTourTrack.setPathHighlighted(false);
		}

		// Turn on highlight if object selected.
		if (pickedTourTrack instanceof Highlightable) {
			pickedTourTrack.setPathHighlighted(true);
		}
	}

	private void selected_WithPositionColor(final ITrackPath pickedTrackPath, final Integer pickPositionIndex) {

		boolean isRedraw = false;
		if (pickedTrackPath == null) {

			// a new tour track is not picked

			if (_lastPickedTourTrack == null) {

				// nothing is picked, nothing must be reset

			} else {

				// reset last picked tour

				_lastPickedTourTrack.setPicked(false, null);
				isRedraw = true;
			}

		} else {

			// a tour track is picked

			if (_lastPickedTourTrack == null) {

				// a new track is picked

				pickedTrackPath.setPicked(true, pickPositionIndex);
				isRedraw = true;

			} else {

				// an old track is picked, check if a new track is picked

				if (_lastPickedTourTrack == pickedTrackPath) {

					// the same track is picked, check if another position is picked

					if (_lastPickPositionIndex == null) {

						// a new position is picked

						pickedTrackPath.setPicked(true, pickPositionIndex);
						isRedraw = true;

					} else {

						// an old position is picked

						if (pickPositionIndex == null) {

							// a new position is not picked, reset pick position but keep the track picked

							pickedTrackPath.setPicked(true, null);
							isRedraw = true;

						} else {

							if (pickPositionIndex.equals(_lastPickPositionIndex)) {

								// the same position and the same track is picked, do nothing

							} else {

								// another position is picked

								pickedTrackPath.setPicked(true, pickPositionIndex);
								isRedraw = true;
							}
						}
					}

				} else {

					// another track is picked

					// first reset last track
					_lastPickedTourTrack.setPicked(false, null);

					// pick new track
					pickedTrackPath.setPicked(true, pickPositionIndex);

					isRedraw = true;
				}
			}
		}

		if (isRedraw) {
			Map3Manager.redraw();
		}
	}

	public void setColorProvider(final IMapColorProvider colorProvider) {

		_colorProvider = colorProvider;

		_tourPositionColors.setColorProvider(colorProvider);
	}

	/**
	 * Set attributes from the configuration into the path but <b>only</b> when they have changed
	 * because setting some properties will reset the path and it will be recreated.
	 * 
	 * @param path
	 */
	private void setPathAttributes(final Path path) {

		// Indicate that dots are to be drawn at each specified path position.
		path.setShowPositions(_trackConfig.isShowTrackPosition);
		path.setShowPositionsScale(_trackConfig.trackPositionSize);
		path.setShowPositionsThreshold(Math.pow(10, _trackConfig.trackPositionThreshold));

		final int altitudeMode = _trackConfig.altitudeMode;
		if (altitudeMode != path.getAltitudeMode()) {
			path.setAltitudeMode(altitudeMode);
		}

		final boolean isFollowTerrain = _trackConfig.isFollowTerrain;
		if (isFollowTerrain != path.isFollowTerrain()) {
			path.setFollowTerrain(isFollowTerrain);
		}

		final boolean isExtrudePath = _trackConfig.isExtrudePath;
		if (isExtrudePath != path.isExtrude()) {
			path.setExtrude(isExtrudePath);
		}

		final boolean isDrawVerticals = _trackConfig.isDrawVerticals;
		if (isDrawVerticals != path.isDrawVerticals()) {
			path.setDrawVerticals(isDrawVerticals);
		}

// UI is disabled
//		final String pathType = _trackConfig.pathType;
//		if (pathType.equals(path.getPathType()) == false) {
//			path.setPathType(pathType);
//		}

// UI is disabled
//		final int numSubsegments = _trackConfig.numSubsegments;
//		if (numSubsegments != path.getNumSubsegments()) {
//			path.setNumSubsegments(numSubsegments);
//		}
		/*
		 * numSubsegments do not have a UI (it's disabled) but ensure that 0 subsegments are set
		 */
		if (path.getNumSubsegments() != 0) {
			path.setNumSubsegments(0);
			_trackConfig.numSubsegments = 0;
		}

		/*
		 * Shape attributes for the path
		 */
		ShapeAttributes normalAttributes = path.getAttributes();
		ShapeAttributes highAttributes = path.getHighlightAttributes();

		if (normalAttributes == null) {

			// initialize colors

			normalAttributes = new BasicShapeAttributes();
			highAttributes = new BasicShapeAttributes();

			// Show how to make the colors vary along the paths.
			path.setPositionColors(_tourPositionColors);
		}

		setPathAttributes_Default(path, normalAttributes);
		setPathAttributes_Hovered(path, highAttributes);

	}

	/**
	 * Set default shape attributes
	 * 
	 * @param path
	 * @param shapeAttributes
	 */
	private void setPathAttributes_Default(final Path path, final ShapeAttributes shapeAttributes) {

		final RGB interiorColor = _trackConfig.interiorColor;
		final RGB outlineColor = _trackConfig.outlineColor;
		shapeAttributes.setDrawOutline(true);
		shapeAttributes.setOutlineWidth(_trackConfig.outlineWidth);
		shapeAttributes.setOutlineOpacity(_trackConfig.outlineOpacity);
		shapeAttributes.setOutlineMaterial(new Material(new Color(
				outlineColor.red,
				outlineColor.green,
				outlineColor.blue)));

		shapeAttributes.setDrawInterior(true);
		shapeAttributes.setInteriorOpacity(_trackConfig.interiorOpacity);
		shapeAttributes.setInteriorMaterial(new Material(new Color(
				interiorColor.red,
				interiorColor.green,
				interiorColor.blue)));

		path.setAttributes(shapeAttributes);
	}

	/**
	 * Set hovered shape attributes
	 * 
	 * @param path
	 * @param shapeAttributes
	 */
	private void setPathAttributes_Hovered(final Path path, final ShapeAttributes shapeAttributes) {

		final RGB interiorColor = _trackConfig.interiorColorHovered;
		final RGB outlineColor = _trackConfig.outlineColorHovered;

		shapeAttributes.setDrawInterior(true);
		shapeAttributes.setInteriorOpacity(_trackConfig.interiorOpacityHovered);
		shapeAttributes.setInteriorMaterial(new Material(new Color(
				interiorColor.red,
				interiorColor.green,
				interiorColor.blue)));

		shapeAttributes.setDrawOutline(true);
		shapeAttributes.setOutlineWidth(_trackConfig.outlineWidth);
		shapeAttributes.setOutlineOpacity(_trackConfig.outlineOpacityHovered);
		shapeAttributes.setOutlineMaterial(new Material(new Color(
				outlineColor.red,
				outlineColor.green,
				outlineColor.blue)));

		path.setHighlightAttributes(shapeAttributes);
	}

	private void setupWWSelectionListener(final boolean isLayerVisible) {

		final WorldWindowGLCanvas ww = Map3Manager.getWWCanvas();

		if (isLayerVisible) {

			if (_lastAddRemoveAction != 1) {

				_lastAddRemoveAction = 1;
				ww.addSelectListener(this);

				System.out.println(UI.timeStampNano()
						+ " ["
						+ getClass().getSimpleName()
						+ "] \tsetupWWSelectionListener\tadd");
				// TODO remove SYSTEM.OUT.PRINTLN

			}

		} else {

			if (_lastAddRemoveAction != 0) {

				_lastAddRemoveAction = 0;
				ww.removeSelectListener(this);

				System.out.println(UI.timeStampNano()
						+ " ["
						+ getClass().getSimpleName()
						+ "] \tsetupWWSelectionListener\tremove");
				// TODO remove SYSTEM.OUT.PRINTLN
			}
		}
	}

	public void updateColors(final ArrayList<TourData> allTours) {

		_tourPositionColors.updateColors(allTours);
	}

}
