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

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.Path;

import java.awt.Color;
import java.util.ArrayList;

import net.tourbook.common.UI;
import net.tourbook.common.color.ColorDefinition;
import net.tourbook.common.color.GraphColorManager;
import net.tourbook.common.color.IMapColorProvider;
import net.tourbook.common.color.IGradientColors;
import net.tourbook.common.color.LegendUnitFormat;
import net.tourbook.common.color.MapColorConfig;
import net.tourbook.common.color.MapColorId;
import net.tourbook.data.TourData;
import net.tourbook.map2.Messages;
import net.tourbook.map3.layer.ColorCacheAWT;
import net.tourbook.map3.layer.Map3Colors;

class TourPositionColors implements Path.PositionColors {

	private final ColorCacheAWT	_awtColorCache	= new ColorCacheAWT();

	private IMapColorProvider		_colorProvider	= Map3Colors.getColorProvider(MapColorId.Altitude);

	public Color getColor(final Position position, final int ordinal) {

		/**
		 * This returns a dummy color, it is just a placeholder because a Path.PositionColors must
		 * be set in the Path THAT a position color is used :-(
		 */

		return Color.CYAN;
	}

	public Color getDiscreteColor(final int colorValue) {
		return _awtColorCache.get(colorValue);
	}

	public Color getGradientColor(	final float graphValue,
									final Integer positionIndex,
									final boolean isTourTrackedPicked,
									final int tourTrackPickIndex) {

		Color positionColor;

		if (isTourTrackedPicked) {

			// tour track is picked

			if (tourTrackPickIndex != -1 && tourTrackPickIndex == positionIndex) {

				// track position is picked, display with inverse color

				positionColor = Color.GREEN;

			} else {

				positionColor = Color.RED;
			}

		} else {

			int colorValue = -1;

			if (_colorProvider instanceof IGradientColors) {

				final IGradientColors gradientColorProvider = (IGradientColors) _colorProvider;

				colorValue = gradientColorProvider.getColorValue(graphValue);
			}

			if (colorValue == -1) {
				// set ugly default value, this case should not happen
				return Color.MAGENTA;
			}

			positionColor = _awtColorCache.get(colorValue);
		}

		return positionColor;
	}

	public void setColorProvider(final IMapColorProvider legendProvider) {

		_colorProvider = legendProvider;

		_awtColorCache.clear();
	}

	public void updateColors(final ArrayList<TourData> allTours) {

		if (_colorProvider instanceof IGradientColors) {

			final IGradientColors colorProvider = (IGradientColors) _colorProvider;

			updateColors_10_SetMinMaxValues(allTours, colorProvider, 300);
		}
	}

	/**
	 * Update the min/max values in the {@link IGradientColors} for the currently
	 * displayed legend
	 * 
	 * @param allTourData
	 * @param colorProvider
	 * @param legendBounds
	 * @return Return <code>true</code> when the legend value could be updated, <code>false</code>
	 *         when data are not available
	 */
	private boolean updateColors_10_SetMinMaxValues(final ArrayList<TourData> allTourData,
													final IGradientColors colorProvider,
													final int legendHeight) {

		if (allTourData.size() == 0) {
			return false;
		}

		final GraphColorManager graphColorProvider = GraphColorManager.getInstance();

		ColorDefinition colorDefinition = null;
		final MapColorConfig legendConfig = colorProvider.getLegendConfig();

		// tell the legend provider how to draw the legend
		switch (colorProvider.getMapColorId()) {

		case Altitude:

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

			colorDefinition = graphColorProvider.getGraphColorDefinition(GraphColorManager.PREF_GRAPH_ALTITUDE);

			colorProvider.setLegendColorColors(colorDefinition.getNewLegendColor());

			colorProvider.setLegendColorValues(
					legendHeight,
					minValue,
					maxValue,
					UI.UNIT_LABEL_ALTITUDE,
					LegendUnitFormat.Number);

			break;

		case Pulse:

			minValue = Float.MIN_VALUE;
			maxValue = Float.MAX_VALUE;

			setInitialValue = true;

			for (final TourData tourData : allTourData) {

				final float[] dataSerie = tourData.pulseSerie;
				if ((dataSerie == null) || (dataSerie.length == 0)) {
					continue;
				}

				/*
				 * get min/max values
				 */
				for (final float dataValue : dataSerie) {

					// patch from Kenny Moens / 2011-08-04
					if (dataValue == 0) {
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

			colorDefinition = graphColorProvider.getGraphColorDefinition(GraphColorManager.PREF_GRAPH_HEARTBEAT);

			colorProvider.setLegendColorColors(colorDefinition.getNewLegendColor());
			colorProvider.setLegendColorValues(
					legendHeight,
					minValue,
					maxValue,
					Messages.graph_label_heartbeat_unit,
					LegendUnitFormat.Number);

			break;

		case Speed:

			minValue = Float.MIN_VALUE;
			maxValue = Float.MAX_VALUE;
			setInitialValue = true;

			for (final TourData tourData : allTourData) {

				final float[] dataSerie = tourData.getSpeedSerie();
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

			legendConfig.numberFormatDigits = 1;
			colorDefinition = graphColorProvider.getGraphColorDefinition(GraphColorManager.PREF_GRAPH_SPEED);

			colorProvider.setLegendColorColors(colorDefinition.getNewLegendColor());
			colorProvider.setLegendColorValues(
					legendHeight,
					minValue,
					maxValue,
					UI.UNIT_LABEL_SPEED,
					LegendUnitFormat.Number);

			break;

		case Pace:

			minValue = Float.MIN_VALUE;
			maxValue = Float.MAX_VALUE;
			setInitialValue = true;

			for (final TourData tourData : allTourData) {

				final float[] dataSerie = tourData.getPaceSerieSeconds();
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

			legendConfig.unitFormat = LegendUnitFormat.Pace;
			colorDefinition = graphColorProvider.getGraphColorDefinition(GraphColorManager.PREF_GRAPH_PACE);

			colorProvider.setLegendColorColors(colorDefinition.getNewLegendColor());
			colorProvider.setLegendColorValues(
					legendHeight,
					minValue,
					maxValue,
					UI.UNIT_LABEL_PACE,
					LegendUnitFormat.Pace);

			break;

		case Gradient:

			minValue = Float.MIN_VALUE;
			maxValue = Float.MAX_VALUE;
			setInitialValue = true;

			for (final TourData tourData : allTourData) {

				final float[] dataSerie = tourData.getGradientSerie();
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

			legendConfig.numberFormatDigits = 1;
			colorDefinition = graphColorProvider.getGraphColorDefinition(GraphColorManager.PREF_GRAPH_GRADIENT);

			colorProvider.setLegendColorColors(colorDefinition.getNewLegendColor());
			colorProvider.setLegendColorValues(
					legendHeight,
					minValue,
					maxValue,
					Messages.graph_label_gradient_unit,
					LegendUnitFormat.Number);

			break;

		default:
			break;
		}

		return true;
	}
}
