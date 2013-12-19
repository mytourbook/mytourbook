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
package net.tourbook.map;

import java.util.ArrayList;

import net.tourbook.common.UI;
import net.tourbook.common.color.ColorDefinition;
import net.tourbook.common.color.GraphColorManager;
import net.tourbook.common.color.IGradientColors;
import net.tourbook.common.color.LegendUnitFormat;
import net.tourbook.common.color.Map2ColorProfile;
import net.tourbook.common.color.MapUnitsConfiguration;
import net.tourbook.data.TourData;
import net.tourbook.map2.Messages;

public class MapUtils {

	/**
	 * Update the min/max values in the {@link IGradientColors} for the currently displayed legend.
	 * 
	 * @param allTourData
	 * @param colorProvider
	 * @param legendHeight
	 * @return Return <code>true</code> when the legend value could be updated, <code>false</code>
	 *         when data are not available
	 */
	public static boolean updateLegendMinMaxValues(	final ArrayList<TourData> allTourData,
													final IGradientColors colorProvider,
													final int legendHeight) {

		if (allTourData.size() == 0) {
			return false;
		}

		final GraphColorManager colorManager = GraphColorManager.getInstance();

		final MapUnitsConfiguration legendConfig = colorProvider.getMapUnitsConfiguration();
		ColorDefinition colorDefinition;

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

			colorDefinition = colorManager.getGraphColorDefinition(GraphColorManager.PREF_GRAPH_ALTITUDE);
			final Map2ColorProfile colorProfile = colorDefinition.getNewMapColor();

			colorProvider.setColorProfile(colorProfile);
			colorProvider.configureColorProvider(
					legendHeight,
					minValue,
					maxValue,
					UI.UNIT_LABEL_ALTITUDE,
					LegendUnitFormat.Number);

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
			colorDefinition = colorManager.getGraphColorDefinition(GraphColorManager.PREF_GRAPH_GRADIENT);

			colorProvider.setColorProfile(colorDefinition.getNewMapColor());
			colorProvider.configureColorProvider(
					legendHeight,
					minValue,
					maxValue,
					Messages.graph_label_gradient_unit,
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
			colorDefinition = colorManager.getGraphColorDefinition(GraphColorManager.PREF_GRAPH_PACE);

			colorProvider.setColorProfile(colorDefinition.getNewMapColor());
			colorProvider.configureColorProvider(
					legendHeight,
					minValue,
					maxValue,
					UI.UNIT_LABEL_PACE,
					LegendUnitFormat.Pace);

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
					if (dataValue == 0 || dataValue == Float.MIN_VALUE) {
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

			colorDefinition = colorManager.getGraphColorDefinition(GraphColorManager.PREF_GRAPH_HEARTBEAT);

			colorProvider.setColorProfile(colorDefinition.getNewMapColor());
			colorProvider.configureColorProvider(
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
			colorDefinition = colorManager.getGraphColorDefinition(GraphColorManager.PREF_GRAPH_SPEED);

			colorProvider.setColorProfile(colorDefinition.getNewMapColor());
			colorProvider.configureColorProvider(
					legendHeight,
					minValue,
					maxValue,
					UI.UNIT_LABEL_SPEED,
					LegendUnitFormat.Number);

			break;

		default:
			break;
		}

		return true;
	}
}
