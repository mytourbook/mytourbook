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

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.Path;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.ShapeAttributes;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;

import net.tourbook.common.color.IMapColorProvider;
import net.tourbook.data.TourData;
import net.tourbook.map2.view.IDiscreteColorProvider;
import net.tourbook.map3.view.ICheckStateListener;
import net.tourbook.map3.view.Map3Manager;
import net.tourbook.map3.view.Map3View;
import net.tourbook.map3.view.TVIMap3Layer;
import net.tourbook.tour.SelectionTourData;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.graphics.RGB;

public class TourTrackLayer extends RenderableLayer implements SelectListener, ICheckStateListener {

	public static final String			MAP3_LAYER_ID			= "TourTrackLayer";			//$NON-NLS-1$

	private final TourPositionColors	_tourPositionColors;
	private IMapColorProvider			_colorProvider;

	private ITrackPath					_lastHoveredTourTrack;
	private Integer						_lastHoveredPositionIndex;

	/**
	 * Contains the track which is currently selected, otherwise <code>null</code>.
	 */
	private ITrackPath					_selectedTrackPath;

	private ShapeAttributes				_normalAttributes		= new BasicShapeAttributes();
	private ShapeAttributes				_hoveredAttributes		= new BasicShapeAttributes();
	private ShapeAttributes				_selecedAttributes		= new BasicShapeAttributes();
	private ShapeAttributes				_hovselAttributes		= new BasicShapeAttributes();

	/**
	 * This flag keeps track of adding/removing the listener that it is not done more than once.
	 */
	private int							_lastAddRemoveAction	= -1;

	public TourTrackLayer(final IDialogSettings state) {

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

		// preserve track selection
		Long selectedTourId = null;
		if (_selectedTrackPath != null) {
			selectedTourId = _selectedTrackPath.getTourTrack().getTourData().getTourId();
		}
		_selectedTrackPath = null;

		// remove all tracks from layer
		removeAllRenderables();

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
					altitude = altiSerie[serieIndex];
				}

				float dataSerieValue = 0;
				if (dataSerie != null) {
					dataSerieValue = dataSerie[serieIndex];
				}

				final TourMap3Position trackPosition = new TourMap3Position(
						LatLon.fromDegrees(lat, lon),
						altitude,
						dataSerieValue);

				if (_colorProvider instanceof IDiscreteColorProvider) {

					final IDiscreteColorProvider discreteColorProvider = (IDiscreteColorProvider) _colorProvider;

					trackPosition.colorValue = discreteColorProvider.getColorValue(tourData, serieIndex);
				}

				trackPositions.add(trackPosition);
			}

			/*
			 * create one path for each tour
			 */
			final ITrackPath trackPath = new TrackPathOptimized(trackPositions);

			final TourTrack tourTrack = new TourTrack(trackPath, tourData, trackPositions, _colorProvider);

			trackPath.setTourTrack(tourTrack);

			// Show how to make the colors vary along the paths.
			trackPath.getPath().setPositionColors(_tourPositionColors);

			if (selectedTourId != null && tourData.getTourId().equals(selectedTourId)) {
				// set selected track
				_selectedTrackPath = trackPath;
			}

			setPathAttributes(trackPath);

			// add track to layer
			addRenderable(trackPath.getPath());

			// keep all positions which is used to find the outline for ALL selected tours
			allPositions.addAll(trackPositions);
		}

		_tourPositionColors.updateColorProvider(allTours);

		// initialize previously selected track
		if (_selectedTrackPath != null) {

			_selectedTrackPath.getTourTrack().setSelected(true);

			setPathHighlighAttributes(_selectedTrackPath);
		}

//		System.out.println(UI.timeStampNano() + " showTour\t" + (System.currentTimeMillis() - start) + " ms");
//		// TODO remove SYSTEM.OUT.PRINTLN

		return allPositions;
	}

	/**
	 * Set the data serie which is painted
	 * 
	 * @param tourData
	 */
	private float[] getDataSerie(final TourData tourData) {

		switch (_colorProvider.getGraphId()) {
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

	/**
	 * @return Returns selected tour track or <code>null</code> when nothing is selected.
	 */
	public ITrackPath getSelectedTrack() {
		return _selectedTrackPath;
	}

	public void onModifyConfig() {

		final TourTrackConfig trackConfig = TourTrackConfigManager.getActiveConfig();

		if (trackConfig.isRecreateTracks()) {

			// track data has changed

			Map3Manager.getMap3View().showAllTours(false);

		} else {

			for (final Renderable renderable : getRenderables()) {

				if (renderable instanceof ITrackPath) {
					setPathAttributes((ITrackPath) renderable);
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

//		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] \t") //$NON-NLS-1$ //$NON-NLS-2$
//				+ propEvent.getPropertyName()
//				+ " \t" //$NON-NLS-1$
//				+ propEvent);
//		// TODO remove SYSTEM.OUT

		if (propEvent.getPropertyName().equals(Map3Manager.PROPERTY_NAME_ENABLED)) {

			// layer is set to be visible/hidden

			final boolean isLayerVisible = propEvent.getNewValue().equals(Boolean.TRUE);

			setupWWSelectionListener(isLayerVisible);
		}
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

		if (event.getMouseEvent() != null && event.getMouseEvent().isConsumed()) {
			return;
		}

		final String eventAction = event.getEventAction();

//		System.out.println(UI.timeStampNano() + " [" + getClass().getSimpleName() + "] \teventAction: " + eventAction);
//		// TODO remove SYSTEM.OUT.PRINTLN

		if (Map3Manager.getMap3View().isContextMenuVisible()) {

			// prevent actions when context menu is visible

			return;
		}

		if (eventAction == SelectEvent.HOVER) {

			// not yet used

		} else if (eventAction == SelectEvent.RIGHT_PRESS) {

			/**
			 * When the context menu should be displayed and the right mouse button is pressed,
			 * first a SelectEvent.HOVER is fired before SelectEvent.RIGHT_PRESS is fired. Therefor
			 * the context state must be set here.
			 * <p>
			 * The context menu is opened with a ww mouse listener in Map3View.
			 */

//			map3View.setContextMenuVisible(true);

		} else {

			// get hovered object
			final PickedObject pickedObject = event.getTopPickedObject();

			ITrackPath hoveredTrackPath = null;
			Integer hoveredPositionIndex = null;

			if (pickedObject != null && pickedObject.getObject() instanceof ITrackPath) {

				hoveredTrackPath = (ITrackPath) pickedObject.getObject();

				final Object pickOrdinal = pickedObject.getValue(AVKey.ORDINAL);
				hoveredPositionIndex = (Integer) pickOrdinal;
			}

			selectTrackPath(hoveredTrackPath, hoveredPositionIndex, eventAction, true);
		}
	}

	/**
	 * @param hoveredTrackPath
	 * @param hoveredPositionIndex
	 *            Can be <code>null</code> when a position is not hovered.
	 * @param eventAction
	 *            To select a track path set this parameter to {@link SelectEvent#LEFT_CLICK}.
	 * @param isFireSelection
	 */
	private void selectTrackPath(	final ITrackPath hoveredTrackPath,
									final Integer hoveredPositionIndex,
									final String eventAction,
									final boolean isFireSelection) {

		final ITrackPath backupSelectedTrackPath = _selectedTrackPath;

		if (eventAction == SelectEvent.LEFT_CLICK) {

			if (_lastHoveredTourTrack != null) {

				// update select state
				selectTrackPath_Clicked_TourTrack(_lastHoveredTourTrack);
			}
		}

		if (eventAction == SelectEvent.ROLLOVER || eventAction == SelectEvent.LEFT_CLICK) {

			// updated colors

			selectTrackPath_Hovered_WithPositionColor(hoveredTrackPath, hoveredPositionIndex);

			_lastHoveredTourTrack = hoveredTrackPath;
			_lastHoveredPositionIndex = hoveredPositionIndex;
		}

		// fire selection
		if (isFireSelection) {

			final Map3View map3View = Map3Manager.getMap3View();

			if (_selectedTrackPath != null
					&& (backupSelectedTrackPath == null || _selectedTrackPath != backupSelectedTrackPath)) {

				// a new track is selected, fire selection

				map3View.fireTourSelection(new SelectionTourData(_selectedTrackPath.getTourTrack().getTourData()));

			} else {

				map3View.setTourInfo(hoveredTrackPath, hoveredPositionIndex);
			}
		}
	}

	/**
	 * @param tourData
	 * @return Returns track positions or <code>null</code> when track is already selected.
	 */
	public ArrayList<TourMap3Position> selectTrackPath(final TourData tourData) {

		if (_selectedTrackPath != null && _selectedTrackPath.getTourTrack().getTourData().equals(tourData)) {

			// track is already selected -> nothing to do

			return null;
		}

		for (final Renderable renderable : getRenderables()) {

			if (renderable instanceof ITrackPath) {

				final ITrackPath trackPath = (ITrackPath) renderable;

				if (trackPath.getTourTrack().getTourData().equals(tourData)) {

					// found track for the selectable tour

					// prevent to fire a selection again because we are currently within a fired selection

					// 1. hover track which should be selected, this will set the last hovered tour track
					selectTrackPath(trackPath, null, SelectEvent.ROLLOVER, false);

					// 2. select tour track
					selectTrackPath(trackPath, null, SelectEvent.LEFT_CLICK, false);

					// 3. simulate hover out of the track that the tour is displayed with the selected color
					//    and not with the hovered color
					selectTrackPath(null, null, SelectEvent.ROLLOVER, false);

					return trackPath.getTourTrack().getTrackPositions();
				}
			}
		}

		// this case should not happen
		return null;
	}

	/**
	 * @param clickedTrack
	 *            Contains the {@link ITrackPath} which is clicked with the mouse.
	 */
	private void selectTrackPath_Clicked_TourTrack(final ITrackPath clickedTrack) {

		if (clickedTrack == _selectedTrackPath) {

			// same tour is clicked again, deselect path

			_selectedTrackPath.getTourTrack().setSelected(false);
			setPathHighlighAttributes(_selectedTrackPath);

			_selectedTrackPath = null;

		} else {

			// another tour is clicked

			if (_selectedTrackPath != null) {

				// deselect last selected tour

				_selectedTrackPath.getTourTrack().setSelected(false);
				setPathHighlighAttributes(_selectedTrackPath);

				// Very Important: reset colors that the track is displayed again with position colors !!!
				_selectedTrackPath.setPicked(false, null);

				_selectedTrackPath = null;
			}

			// select clicked tour

			_selectedTrackPath = clickedTrack;

			_selectedTrackPath.getTourTrack().setSelected(true);
			setPathHighlighAttributes(_selectedTrackPath);
		}
	}

	private void selectTrackPath_Hovered_WithAttributeColor(final ITrackPath hoveredTrack) {

		if (_lastHoveredTourTrack == hoveredTrack) {
			// same tour as before
//			System.out.println(UI.timeStampNano() + " [" + getClass().getSimpleName() + "] \tsame tour as before");
//			// TODO remove SYSTEM.OUT.PRINTLN
			return;
		}

		// Turn off highlight if on.
		if (_lastHoveredTourTrack != null && _lastHoveredTourTrack.getTourTrack().isSelected() == false) {
//			_lastHoveredTourTrack.setPathHighlighted(false);
		}

		// Turn on highlight if object is selected.
		if (hoveredTrack != null) {
//			hoveredTrack.setPathHighlighted(true);
		}
	}

	private void selectTrackPath_Hovered_WithPositionColor(	final ITrackPath hoveredTrack,
															final Integer hoveredPositionIndex) {

		if (hoveredTrack == null) {

			// a new tour track is not picked

			if (_lastHoveredTourTrack == null) {

				// nothing is picked, nothing must be reset

			} else {

				// reset last picked tour

				_lastHoveredTourTrack.setPicked(false, null);
				setPathHighlighAttributes(_lastHoveredTourTrack);
			}

		} else {

			// a tour track is picked

			if (_lastHoveredTourTrack == null) {

				// a new track is picked

				hoveredTrack.setPicked(true, hoveredPositionIndex);
				setPathHighlighAttributes(hoveredTrack);

			} else {

				// an old track is picked, check if a new track is picked

				if (_lastHoveredTourTrack == hoveredTrack) {

					// the same track is picked, check if another position is picked

					if (_lastHoveredPositionIndex == null) {

						// a new position is picked

						hoveredTrack.setPicked(true, hoveredPositionIndex);
						setPathHighlighAttributes(hoveredTrack);

					} else {

						// an old position is picked

						if (hoveredPositionIndex == null) {

							// a new position is not picked, reset pick position but keep the track picked

							hoveredTrack.setPicked(true, null);
							setPathHighlighAttributes(hoveredTrack);

						} else {

							if (hoveredPositionIndex.equals(_lastHoveredPositionIndex)) {

								// the same position and the same track is picked, do nothing

							} else {

								// another position is picked

								hoveredTrack.setPicked(true, hoveredPositionIndex);
								setPathHighlighAttributes(hoveredTrack);
							}
						}
					}

				} else {

					// another track is picked

					// first reset last track
					_lastHoveredTourTrack.setPicked(false, null);
					setPathHighlighAttributes(_lastHoveredTourTrack);

					// pick new track
					hoveredTrack.setPicked(true, hoveredPositionIndex);
					setPathHighlighAttributes(hoveredTrack);

				}
			}
		}
	}

	public void setColorProvider(final IMapColorProvider colorProvider) {

		_colorProvider = colorProvider;

		_tourPositionColors.setColorProvider(colorProvider);
	}

	public void setExpired() {

		for (final Renderable renderable : getRenderables()) {

			if (renderable instanceof ITrackPath) {
				final ITrackPath trackPath = (ITrackPath) renderable;

				trackPath.setExpired();
			}
		}

		Map3Manager.getWWCanvas().redraw();
	}

	/**
	 * Set attributes from the configuration into the path but <b>only</b> when they have changed
	 * because setting some properties will reset the path and it will be recreated.
	 * 
	 * @param path
	 */
	private void setPathAttributes(final ITrackPath trackPath) {

//		System.out.println(UI.timeStampNano() + " [" + getClass().getSimpleName() + "] \tsetPathAttributes()");
//		// TODO remove SYSTEM.OUT.PRINTLN

		final TourTrackConfig trackConfig = TourTrackConfigManager.getActiveConfig();

		// force the track colors to be recreated, opacity can have been changed
		trackPath.getTourTrack().updateColors(trackConfig.trackColorOpacity);

		final Path path = trackPath.getPath();

		path.setShowPositionsThreshold(Math.pow(10, trackConfig.trackPositionThreshold));

		final int altitudeMode = trackConfig.altitudeMode;
		if (altitudeMode != path.getAltitudeMode()) {
			path.setAltitudeMode(altitudeMode);
		}

		final boolean isFollowTerrain = trackConfig.isFollowTerrain;
		if (isFollowTerrain != path.isFollowTerrain()) {
			path.setFollowTerrain(isFollowTerrain);
		}

		final boolean isExtrudePath = trackConfig.isShowInterior;
		if (isExtrudePath != path.isExtrude()) {
			path.setExtrude(isExtrudePath);
		}

		final boolean isDrawVerticals = trackConfig.isDrawVerticals;
		if (isDrawVerticals != path.isDrawVerticals()) {
			path.setDrawVerticals(isDrawVerticals);
		}

		/*
		 * numSubsegments do not have a UI (it's disabled) but ensure that 0 subsegments are set
		 */
		if (path.getNumSubsegments() != 0) {
			path.setNumSubsegments(0);
		}

		/*
		 * Set shape attributes for the path
		 */

		setPathAttributes_Normal();
		setPathAttributes_Hovered();
		setPathAttributes_Selected();
		setPathAttributes_HovSel();

//		System.out.println(UI.timeStampNano()
//				+ " ["
//				+ getClass().getSimpleName()
//				+ "] \t\trgb: "
//				+ _normalAttributes.getOutlineMaterial().getDiffuse());
//		// TODO remove SYSTEM.OUT.PRINTLN

		path.setAttributes(_normalAttributes);
		setPathHighlighAttributes(trackPath);

		path.setEnableDepthOffset(true);

		/*
		 * ensure that cached data are recreated, e.g. direction arrow size
		 */
		trackPath.setExpired();
	}

	/**
	 * Set hovered shape attributes
	 * 
	 * @param shapeAttributes
	 */
	private void setPathAttributes_Hovered() {

		final TourTrackConfig trackConfig = TourTrackConfigManager.getActiveConfig();

		final RGB interiorRGB = trackConfig.interiorColor_Hovered;
		final RGB outlineRGB = trackConfig.outlineColor_Hovered;

		final Color interiorColor = new Color(interiorRGB.red, interiorRGB.green, interiorRGB.blue);
		final Color outlineColor = new Color(outlineRGB.red, outlineRGB.green, outlineRGB.blue);

		_hoveredAttributes.setDrawOutline(true);
		_hoveredAttributes.setOutlineWidth(trackConfig.outlineWidth);
		_hoveredAttributes.setOutlineOpacity(trackConfig.outlineOpacity_Hovered);
		_hoveredAttributes.setOutlineMaterial(new Material(outlineColor));

		_hoveredAttributes.setDrawInterior(true);
		_hoveredAttributes.setInteriorOpacity(trackConfig.interiorOpacity_Hovered);
		_hoveredAttributes.setInteriorMaterial(new Material(interiorColor));
	}

	/**
	 * Set hovered & selected shape attributes
	 * 
	 * @param shapeAttributes
	 */
	private void setPathAttributes_HovSel() {

		final TourTrackConfig trackConfig = TourTrackConfigManager.getActiveConfig();

		final RGB interiorRGB = trackConfig.interiorColor_HovSel;
		final RGB outlineRGB = trackConfig.outlineColor_HovSel;

		final Color interiorColor = new Color(interiorRGB.red, interiorRGB.green, interiorRGB.blue);
		final Color outlineColor = new Color(outlineRGB.red, outlineRGB.green, outlineRGB.blue);

		_hovselAttributes.setDrawOutline(true);
		_hovselAttributes.setOutlineWidth(trackConfig.outlineWidth);
		_hovselAttributes.setOutlineOpacity(trackConfig.outlineOpacity_HovSel);
		_hovselAttributes.setOutlineMaterial(new Material(outlineColor));

		_hovselAttributes.setDrawInterior(true);
		_hovselAttributes.setInteriorOpacity(trackConfig.interiorOpacity_HovSel);
		_hovselAttributes.setInteriorMaterial(new Material(interiorColor));
	}

	/**
	 * Set default shape attributes
	 * 
	 * @param shapeAttributes
	 */
	private void setPathAttributes_Normal() {

		final TourTrackConfig trackConfig = TourTrackConfigManager.getActiveConfig();

		final RGB interiorRGB = trackConfig.interiorColor;
		final RGB outlineRGB = trackConfig.outlineColor;

		final Color interiorColor = new Color(interiorRGB.red, interiorRGB.green, interiorRGB.blue);
		final Color outlineColor = new Color(outlineRGB.red, outlineRGB.green, outlineRGB.blue);

		_normalAttributes.setDrawOutline(true);
		_normalAttributes.setOutlineWidth(trackConfig.outlineWidth);
		_normalAttributes.setOutlineOpacity(trackConfig.outlineOpacity);
		_normalAttributes.setOutlineMaterial(new Material(outlineColor));

		_normalAttributes.setDrawInterior(true);
		_normalAttributes.setInteriorOpacity(trackConfig.interiorOpacity);
		_normalAttributes.setInteriorMaterial(new Material(interiorColor));
	}

	/**
	 * Set selected shape attributes
	 * 
	 * @param shapeAttributes
	 */
	private void setPathAttributes_Selected() {

		final TourTrackConfig trackConfig = TourTrackConfigManager.getActiveConfig();

		final RGB interiorRGB = trackConfig.interiorColor_Selected;
		final RGB outlineRGB = trackConfig.outlineColor_Selected;

		final Color interiorColor = new Color(interiorRGB.red, interiorRGB.green, interiorRGB.blue);
		final Color outlineColor = new Color(outlineRGB.red, outlineRGB.green, outlineRGB.blue);

		_selecedAttributes.setDrawOutline(true);
		_selecedAttributes.setOutlineWidth(trackConfig.outlineWidth);
		_selecedAttributes.setOutlineOpacity(trackConfig.outlineOpacity_Selected);
		_selecedAttributes.setOutlineMaterial(new Material(outlineColor));

		_selecedAttributes.setDrawInterior(true);
		_selecedAttributes.setInteriorOpacity(trackConfig.interiorOpacity_Selected);
		_selecedAttributes.setInteriorMaterial(new Material(interiorColor));
	}

	/**
	 * Set's highlight attributes into the track path.
	 * 
	 * @param trackPath
	 */
	private void setPathHighlighAttributes(final ITrackPath trackPath) {

		final TourTrack tourTrack = trackPath.getTourTrack();

		final boolean isHovered = tourTrack.isHovered();
		final boolean isSelected = tourTrack.isSelected();

		final TourTrackConfig trackConfig = TourTrackConfigManager.getActiveConfig();
		final ShapeAttributes shapeAttrs;

		// defaults are for selected/hovered
		boolean isShowPositions;
		double positionsScale;

		if (isHovered && isSelected) {

			shapeAttrs = _hovselAttributes;

			positionsScale = trackConfig.trackPositionSize_HovSel;
			isShowPositions = positionsScale == 0.0 ? false : true;

		} else if (isHovered) {

			shapeAttrs = _hoveredAttributes;

			positionsScale = trackConfig.trackPositionSize_Hovered;
			isShowPositions = positionsScale == 0.0 ? false : true;

		} else if (isSelected) {

			shapeAttrs = _selecedAttributes;

			positionsScale = trackConfig.trackPositionSize_Selected;
			isShowPositions = positionsScale == 0.0 ? false : true;

		} else {

			// not hovered and not selected => normal

			shapeAttrs = _normalAttributes;

			isShowPositions = trackConfig.isShowTrackPosition;
			positionsScale = trackConfig.trackPositionSize;
		}

		final Path path = trackPath.getPath();

		path.setShowPositions(isShowPositions);
		path.setShowPositionsScale(positionsScale / trackConfig.outlineWidth);

		path.setHighlightAttributes(shapeAttrs);
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

	public void updateColors(final ArrayList<TourData> allTours) {

		_tourPositionColors.updateColorProvider(allTours);
	}

}
