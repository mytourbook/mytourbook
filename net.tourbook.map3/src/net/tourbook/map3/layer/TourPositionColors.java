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
package net.tourbook.map3.layer;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.Path;

import java.awt.Color;
import java.util.ArrayList;

import net.tourbook.common.UI;
import net.tourbook.common.color.ColorDefinition;
import net.tourbook.common.color.GraphColorProvider;
import net.tourbook.common.color.ILegendProvider;
import net.tourbook.common.color.ILegendProviderGradientColors;
import net.tourbook.common.color.LegendConfig;
import net.tourbook.common.color.LegendUnitFormat;
import net.tourbook.data.TourData;

class TourPositionColors implements Path.PositionColors {

	private final ColorCacheAWT	_colorCache		= new ColorCacheAWT();

	private ILegendProvider		_colorProvider	= Map3Colors.getColorProvider(ILegendProvider.TOUR_COLOR_ALTITUDE);

	public Color getColor(final float altitude) {

		// Color the positions based on their altitude.

		int colorValue = -1;

		if (_colorProvider instanceof ILegendProviderGradientColors) {

			final ILegendProviderGradientColors gradientColorProvider = (ILegendProviderGradientColors) _colorProvider;

			colorValue = gradientColorProvider.getColorValue(altitude);
		}

		if (colorValue == -1) {
			// set ugly default value
			return Color.MAGENTA;
		}

		return _colorCache.get(colorValue);
	}

	public Color getColor(final Position position, final int ordinal) {

		final double altitude = position.getAltitude();

		return getColor((float) altitude);
	}

	public void updateColors(final ArrayList<TourData> allTours) {

		if (_colorProvider instanceof ILegendProviderGradientColors) {

			final ILegendProviderGradientColors colorProvider = (ILegendProviderGradientColors) _colorProvider;

			updateColors_10(allTours, colorProvider, 300);
		}
	}

	/**
	 * Update the min/max values in the {@link ILegendProviderGradientColors} for the currently
	 * displayed legend
	 * 
	 * @param allTourData
	 * @param legendProvider
	 * @param legendBounds
	 * @return Return <code>true</code> when the legend value could be updated, <code>false</code>
	 *         when data are not available
	 */
	private boolean updateColors_10(final ArrayList<TourData> allTourData,
									final ILegendProviderGradientColors legendProvider,
									final int legendHeight) {

		if (allTourData.size() == 0) {
			return false;
		}

		final GraphColorProvider colorProvider = GraphColorProvider.getInstance();

		ColorDefinition colorDefinition = null;
		final LegendConfig legendConfig = legendProvider.getLegendConfig();

		// tell the legend provider how to draw the legend
		switch (legendProvider.getTourColorId()) {

		case ILegendProvider.TOUR_COLOR_ALTITUDE:

			float minValue = Float.MIN_VALUE;
			float maxValue = Float.MAX_VALUE;
			boolean setInitialValue = true;

			for (final TourData tourData : allTourData) {

				final float[] dataSerie = tourData.getAltitudeSerie();
				if ((dataSerie == null) || (dataSerie.length == 0)) {
					continue;
				}

				/*
				 * get min/max values
				 */
				for (final float dataValue : dataSerie) {

					if (dataValue == Float.MIN_VALUE) {
						// skip invalid values
						continue;
					}

					if (setInitialValue) {

						setInitialValue = false;
						minValue = maxValue = dataValue;
					}

					minValue = (minValue <= dataValue) ? minValue : dataValue;
					maxValue = (maxValue >= dataValue) ? maxValue : dataValue;
				}
			}

			if ((minValue == Float.MIN_VALUE) || (maxValue == Float.MAX_VALUE)) {
				return false;
			}

			colorDefinition = colorProvider.getGraphColorDefinition(GraphColorProvider.PREF_GRAPH_ALTITUDE);

			legendProvider.setLegendColorColors(colorDefinition.getNewLegendColor());
			legendProvider.setLegendColorValues(
					legendHeight,
					minValue,
					maxValue,
					UI.UNIT_LABEL_ALTITUDE,
					LegendUnitFormat.Number);

			break;

//		case TourMapColors.TOUR_COLOR_PULSE:
//
//			minValue = Float.MIN_VALUE;
//			maxValue = Float.MAX_VALUE;
//			setInitialValue = true;
//
//			for (final TourData tourData : _allTourData) {
//
//				final float[] dataSerie = tourData.pulseSerie;
//				if ((dataSerie == null) || (dataSerie.length == 0)) {
//					continue;
//				}
//
//				/*
//				 * get min/max values
//				 */
//				for (final float dataValue : dataSerie) {
//
//					// patch from Kenny Moens / 2011-08-04
//					if (dataValue == 0) {
//						continue;
//					}
//
//					if (setInitialValue) {
//						setInitialValue = false;
//						minValue = maxValue = dataValue;
//					}
//
//					minValue = (minValue <= dataValue) ? minValue : dataValue;
//					maxValue = (maxValue >= dataValue) ? maxValue : dataValue;
//				}
//			}
//
//			if ((minValue == Float.MIN_VALUE) || (maxValue == Float.MAX_VALUE)) {
//				return false;
//			}
//
//			colorDefinition = colorProvider.getGraphColorDefinition(GraphColorProvider.PREF_GRAPH_HEARTBEAT);
//
//			legendProvider.setLegendColorColors(colorDefinition.getNewLegendColor());
//			legendProvider.setLegendColorValues(
//					legendBounds,
//					minValue,
//					maxValue,
//					Messages.graph_label_heartbeat_unit,
//					LegendUnitFormat.Number);
//
//			break;
//
//		case TourMapColors.TOUR_COLOR_SPEED:
//
//			minValue = Float.MIN_VALUE;
//			maxValue = Float.MAX_VALUE;
//			setInitialValue = true;
//
//			for (final TourData tourData : _allTourData) {
//
//				final float[] dataSerie = tourData.getSpeedSerie();
//				if ((dataSerie == null) || (dataSerie.length == 0)) {
//					continue;
//				}
//
//				/*
//				 * get min/max values
//				 */
//				for (final float dataValue : dataSerie) {
//
//					if (dataValue == Float.MIN_VALUE) {
//						// skip invalid values
//						continue;
//					}
//
//					if (setInitialValue) {
//						setInitialValue = false;
//						minValue = maxValue = dataValue;
//					}
//
//					minValue = (minValue <= dataValue) ? minValue : dataValue;
//					maxValue = (maxValue >= dataValue) ? maxValue : dataValue;
//				}
//			}
//
//			if ((minValue == Float.MIN_VALUE) || (maxValue == Float.MAX_VALUE)) {
//				return false;
//			}
//
//			legendConfig.numberFormatDigits = 1;
//			colorDefinition = colorProvider.getGraphColorDefinition(GraphColorProvider.PREF_GRAPH_SPEED);
//
//			legendProvider.setLegendColorColors(colorDefinition.getNewLegendColor());
//			legendProvider.setLegendColorValues(
//					legendBounds,
//					minValue,
//					maxValue,
//					UI.UNIT_LABEL_SPEED,
//					LegendUnitFormat.Number);
//
//			break;
//
//		case TourMapColors.TOUR_COLOR_PACE:
//
//			minValue = Float.MIN_VALUE;
//			maxValue = Float.MAX_VALUE;
//			setInitialValue = true;
//
//			for (final TourData tourData : _allTourData) {
//
//				final float[] dataSerie = tourData.getPaceSerieSeconds();
//				if ((dataSerie == null) || (dataSerie.length == 0)) {
//					continue;
//				}
//
//				/*
//				 * get min/max values
//				 */
//				for (final float dataValue : dataSerie) {
//
//					if (dataValue == Float.MIN_VALUE) {
//						// skip invalid values
//						continue;
//					}
//
//					if (setInitialValue) {
//						setInitialValue = false;
//						minValue = maxValue = dataValue;
//					}
//
//					minValue = (minValue <= dataValue) ? minValue : dataValue;
//					maxValue = (maxValue >= dataValue) ? maxValue : dataValue;
//				}
//			}
//
//			if ((minValue == Float.MIN_VALUE) || (maxValue == Float.MAX_VALUE)) {
//				return false;
//			}
//
//			legendConfig.unitFormat = LegendUnitFormat.Pace;
//			colorDefinition = colorProvider.getGraphColorDefinition(GraphColorProvider.PREF_GRAPH_PACE);
//
//			legendProvider.setLegendColorColors(colorDefinition.getNewLegendColor());
//			legendProvider.setLegendColorValues(
//					legendBounds,
//					minValue,
//					maxValue,
//					UI.UNIT_LABEL_PACE,
//					LegendUnitFormat.Pace);
//
//			break;
//
//		case TourMapColors.TOUR_COLOR_GRADIENT:
//
//			minValue = Float.MIN_VALUE;
//			maxValue = Float.MAX_VALUE;
//			setInitialValue = true;
//
//			for (final TourData tourData : _allTourData) {
//
//				final float[] dataSerie = tourData.getGradientSerie();
//				if ((dataSerie == null) || (dataSerie.length == 0)) {
//					continue;
//				}
//
//				/*
//				 * get min/max values
//				 */
//				for (final float dataValue : dataSerie) {
//
//					if (dataValue == Float.MIN_VALUE) {
//						// skip invalid values
//						continue;
//					}
//
//					if (setInitialValue) {
//						setInitialValue = false;
//						minValue = maxValue = dataValue;
//					}
//
//					minValue = (minValue <= dataValue) ? minValue : dataValue;
//					maxValue = (maxValue >= dataValue) ? maxValue : dataValue;
//				}
//			}
//
//			if ((minValue == Float.MIN_VALUE) || (maxValue == Float.MAX_VALUE)) {
//				return false;
//			}
//
//			legendConfig.numberFormatDigits = 1;
//			colorDefinition = colorProvider.getGraphColorDefinition(GraphColorProvider.PREF_GRAPH_GRADIENT);
//
//			legendProvider.setLegendColorColors(colorDefinition.getNewLegendColor());
//			legendProvider.setLegendColorValues(
//					legendBounds,
//					minValue,
//					maxValue,
//					Messages.graph_label_gradient_unit,
//					LegendUnitFormat.Number);
//
//			break;

		default:
			break;
		}

		return true;
	}
}
