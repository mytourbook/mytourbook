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
package net.tourbook.ui.tourChart;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.GraphDrawingData;
import net.tourbook.chart.IFillPainter;
import net.tourbook.data.HrZoneContext;
import net.tourbook.data.TourData;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourPersonHRZone;
import net.tourbook.tour.TourManager;
import net.tourbook.training.TrainingManager;

/**
 * Draws background color into the graph, e.g. HR zone, swim style
 */
public class GraphBackgroundPainter implements IFillPainter {

	private Color[]	_hrZoneColors;

	private void createHrZoneColors(final GC gcGraph, final TourPerson tourPerson) {

		final Device display = gcGraph.getDevice();
		final ArrayList<TourPersonHRZone> personHrZones = tourPerson.getHrZonesSorted();

		_hrZoneColors = new Color[personHrZones.size()];

		for (int colorIndex = 0; colorIndex < personHrZones.size(); colorIndex++) {

			final TourPersonHRZone hrZone = personHrZones.get(colorIndex);
			final RGB rgb = hrZone.getColor();

			_hrZoneColors[colorIndex] = new Color(display, rgb);
		}
	}

	@Override
	public void draw(	final GC gcGraph,
						final GraphDrawingData graphDrawingData,
						final Chart chart,
						final long[] devXPositions,
						final int valueIndexFirstPoint,
						final int valueIndexLastPoint) {

//		drawVertical(gcGraph, graphDrawingData, chart);
		drawHorizontal(gcGraph, graphDrawingData, chart, devXPositions, valueIndexFirstPoint, valueIndexLastPoint);
	}

	private void drawHorizontal(final GC gcGraph,
								final GraphDrawingData graphDrawingData,
								final Chart chart,
								final long[] devXPositions,
								final int valueIndexFirstPoint,
								final int valueIndexLastPoint) {

		final ChartDataModel dataModel = chart.getChartDataModel();

		final TourData tourData = (TourData) dataModel.getCustomData(//
				TourManager.CUSTOM_DATA_TOUR_DATA);
		final TourChartConfiguration tourChartConfig = (TourChartConfiguration) dataModel.getCustomData(//
				TourManager.CUSTOM_DATA_TOUR_CHART_CONFIGURATION);

		final TourPerson tourPerson = tourData.getDataPerson();
		if (tourPerson == null) {
			return;
		}

		final int numberOfHrZones = tourData.getNumberOfHrZones();
		if (numberOfHrZones == 0) {
			return;
		}

		final float[] pulseSerie = tourData.pulseSerie;
		if (pulseSerie == null) {
			return;
		}

		createHrZoneColors(gcGraph, tourPerson);

		boolean isGradient = false;
		boolean isWhite = false;
		boolean isBgColor = false;
		final String graphBgStyle = tourChartConfig.graphBackgroundStyle;

		if (graphBgStyle.equals(TourChart.ACTION_ID_GRAPH_BG_STYLE_GRAPH_TOP)) {

			isGradient = true;
			isWhite = false;
			isBgColor = true;

		} else if (graphBgStyle.equals(TourChart.ACTION_ID_GRAPH_BG_STYLE_NO_GRADIENT)) {

			isGradient = false;
			isWhite = false;
			isBgColor = true;

		} else if (graphBgStyle.equals(TourChart.ACTION_ID_GRAPH_BG_STYLE_WHITE_TOP)) {

			isGradient = true;
			isWhite = true;
			isBgColor = true;

		} else if (graphBgStyle.equals(TourChart.ACTION_ID_GRAPH_BG_STYLE_WHITE_BOTTOM)) {

			isGradient = true;
			isWhite = true;
			isBgColor = false;
		}

		if (isWhite) {
			gcGraph.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		}

		final int devCanvasHeight = graphDrawingData.devGraphHeight;

		final long devXPrev = devXPositions[valueIndexFirstPoint];
		long devXHrStart = devXPositions[valueIndexFirstPoint];

		final HrZoneContext hrZoneContext = tourData.getHrZoneContext();
		int prevZoneIndex = TrainingManager.getZoneIndex(hrZoneContext, pulseSerie[valueIndexFirstPoint]);

		for (int valueIndex = valueIndexFirstPoint + 1; valueIndex <= valueIndexLastPoint; valueIndex++) {

			final long devXCurrent = devXPositions[valueIndex];
			final boolean isLastIndex = valueIndex == valueIndexLastPoint;

			// ignore same position even when the HR zone has changed
			if (devXCurrent == devXPrev && isLastIndex == false) {
				continue;
			}

			// check if zone has changed
			final int zoneIndex = TrainingManager.getZoneIndex(hrZoneContext, pulseSerie[valueIndex]);
			if (zoneIndex == prevZoneIndex && isLastIndex == false) {
				continue;
			}

			final int devWidth = (int) (devXCurrent - devXHrStart);

			if (isBgColor) {
				gcGraph.setBackground(_hrZoneColors[prevZoneIndex]);
			} else {
				gcGraph.setForeground(_hrZoneColors[prevZoneIndex]);
			}

			if (isGradient) {
				gcGraph.fillGradientRectangle((int) devXHrStart, 0, devWidth, devCanvasHeight, true);
			} else {
				gcGraph.fillRectangle((int) devXHrStart, 0, devWidth, devCanvasHeight);
			}

			// set start for the next HR zone
			devXHrStart = devXCurrent;
			prevZoneIndex = zoneIndex;
		}

		// dispose colors
		for (final Color color : _hrZoneColors) {
			color.dispose();
		}
	}
}
