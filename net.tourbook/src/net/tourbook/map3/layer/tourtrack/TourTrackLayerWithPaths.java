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
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.Path;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.ShapeAttributes;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;

import net.tourbook.common.UI;
import net.tourbook.common.color.ILegendProvider;
import net.tourbook.data.TourData;
import net.tourbook.map2.view.ILegendProviderDiscreteColors;
import net.tourbook.map3.Messages;
import net.tourbook.map3.view.ICheckStateListener;
import net.tourbook.map3.view.Map3Manager;
import net.tourbook.map3.view.TVIMap3Layer;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.graphics.RGB;

/**
 */
public class TourTrackLayerWithPaths extends RenderableLayer implements SelectListener, ICheckStateListener {

	public static final String			MAP3_LAYER_ID			= "TourTrackLayer"; //$NON-NLS-1$

	private IDialogSettings				_state;

	private final TourPositionColors	_tourPositionColors;
	private ILegendProvider				_colorProvider;

	private final TourTrackConfig		_trackConfig;

	/**
	 * This flag keeps track of adding/removing the listener that it is not done more than once.
	 */
	private int							_lastAddRemoveAction	= -1;

	public TourTrackLayerWithPaths(final IDialogSettings state) {

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

				if (_colorProvider instanceof ILegendProviderDiscreteColors) {

					final ILegendProviderDiscreteColors discreteColorProvider = (ILegendProviderDiscreteColors) _colorProvider;

					trackPosition.colorValue = discreteColorProvider.getColorValue(tourData, serieIndex);
				}

				trackPositions.add(trackPosition);
			}

			/*
			 * create one path for each tour
			 */
//			final MultiResolutionPath tourPath = new MTMultiResPath(positions);
			final PathWithTour tourPath = new PathWithTour(trackPositions, _colorProvider);

			setPathAttributes(tourPath);

			addRenderable(tourPath);

			// keep all positions which is used to find the outline for ALL selected tours
			allPositions.addAll(trackPositions);
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

		final ILegendProvider colorProvider = _tourPositionColors.getColorProvider();

		switch (colorProvider.getTourColorId()) {
		case ILegendProvider.TOUR_COLOR_ALTITUDE:
			return tourData.altitudeSerie;

		case ILegendProvider.TOUR_COLOR_GRADIENT:
			return tourData.getGradientSerie();

		case ILegendProvider.TOUR_COLOR_PULSE:
			return tourData.pulseSerie;

		case ILegendProvider.TOUR_COLOR_SPEED:
			return tourData.getSpeedSerie();

		case ILegendProvider.TOUR_COLOR_PACE:
			return tourData.getPaceSerieSeconds();

		case ILegendProvider.TOUR_COLOR_HR_ZONE:
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

			final boolean isLayerVisible = propEvent.getNewValue().equals(UI.BOOLEAN_TRUE);

			setupWWSelectionListener(isLayerVisible);
		}
	}

	public void saveState() {

		_trackConfig.saveState(_state);
	}

	@Override
	public void selected(final SelectEvent event) {

		System.out.println(UI.timeStampNano() + " [" + getClass().getSimpleName() + "] \t" + event);
		// TODO remove SYSTEM.OUT.PRINTLN

	}

	public void setColorProvider(final ILegendProvider colorProvider) {

		_colorProvider = colorProvider;

		_tourPositionColors.setColorProvider(colorProvider);
	}

	/**
	 * Set attributes from the configuration into the path.
	 * 
	 * @param path
	 */
	private void setPathAttributes(final Path path) {

//		tourPath.setValue(AVKey.DISPLAY_NAME, MAP3_LAYER_ID);

//		tourPath.setSkipCountComputer(tourPath.getSkipCountComputer());

		// Indicate that the dots be drawn only when the path is less than 5 KM from the eye point.
//		tourPath.setShowPositionsThreshold(5e3);

		// Override generic view-distance geometry regeneration because multi-res Paths handle that themselves.
//		tourPath.setViewDistanceExpiration(false);

		// Indicate that dots are to be drawn at each specified path position.
		path.setShowPositions(_trackConfig.isShowTrackPosition);
		path.setShowPositionsScale(_trackConfig.trackPositionSize);

		path.setAltitudeMode(_trackConfig.altitudeMode);
		path.setFollowTerrain(_trackConfig.isFollowTerrain);
		path.setPathType(_trackConfig.pathType);

		path.setExtrude(_trackConfig.isExtrudePath);
		path.setDrawVerticals(_trackConfig.isDrawVerticals);

		path.setNumSubsegments(_trackConfig.numSubsegments);

		// Show how to make the colors vary along the paths.
		path.setPositionColors(_tourPositionColors);

		/*
		 * Create attributes for the Path.
		 */
		final RGB interiorColor = _trackConfig.interiorColor;
		final RGB outlineColor = _trackConfig.outlineColor;
		final Color colorInterior = new Color(interiorColor.red, interiorColor.green, interiorColor.blue);
		final Color colorOutline = new Color(outlineColor.red, outlineColor.green, outlineColor.blue);

		final ShapeAttributes shapeAttrs = new BasicShapeAttributes();

		shapeAttrs.setDrawOutline(true);
		shapeAttrs.setOutlineWidth(_trackConfig.outlineWidth);
		shapeAttrs.setOutlineOpacity(_trackConfig.outlineOpacity);
		shapeAttrs.setOutlineMaterial(new Material(colorOutline));

		shapeAttrs.setDrawInterior(true);
		shapeAttrs.setInteriorOpacity(_trackConfig.interiorOpacity);
		shapeAttrs.setInteriorMaterial(new Material(colorInterior));

		path.setAttributes(shapeAttrs);
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

		_tourPositionColors.updateColors(allTours);
	}

}
