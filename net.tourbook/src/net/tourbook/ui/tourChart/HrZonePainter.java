/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

/**
 * Draws HR zone as background color into the graph.
 */
public class HrZonePainter implements IFillPainter {

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
						final int[] devXPositions,
						final int valueIndexFirstPoint,
						final int valueIndexLastPoint) {

//		drawVertical(gcGraph, graphDrawingData, chart);
		drawHorizontal(gcGraph, graphDrawingData, chart, devXPositions, valueIndexFirstPoint, valueIndexLastPoint);
	}

	private void drawHorizontal(final GC gcGraph,
								final GraphDrawingData graphDrawingData,
								final Chart chart,
								final int[] devXPositions,
								final int valueIndexFirstPoint,
								final int valueIndexLastPoint) {

		final ChartDataModel dataModel = chart.getChartDataModel();

		final TourData tourData = (TourData) dataModel.getCustomData(//
				TourManager.CUSTOM_DATA_TOUR_DATA);
		final TourChartConfiguration tourChartConfig = (TourChartConfiguration) dataModel.getCustomData(//
				TourManager.CUSTOM_DATA_TOUR_CHART_CONFIGURATION);

		final TourPerson tourPerson = tourData.getTourPerson();
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
		final String hrZoneStyle = tourChartConfig.hrZoneStyle;

		if (hrZoneStyle.equals(TourChart.COMMAND_ID_HR_ZONE_STYLE_GRAPH_TOP)) {
			isGradient = true;
			isWhite = false;
			isBgColor = true;
		} else if (hrZoneStyle.equals(TourChart.COMMAND_ID_HR_ZONE_STYLE_NO_GRADIENT)) {
			isGradient = false;
			isWhite = false;
			isBgColor = true;
		} else if (hrZoneStyle.equals(TourChart.COMMAND_ID_HR_ZONE_STYLE_WHITE_TOP)) {
			isGradient = true;
			isWhite = true;
			isBgColor = true;
		} else if (hrZoneStyle.equals(TourChart.COMMAND_ID_HR_ZONE_STYLE_WHITE_BOTTOM)) {
			isGradient = true;
			isWhite = true;
			isBgColor = false;
		}

		if (isWhite) {
			gcGraph.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		}

		final int devCanvasHeight = graphDrawingData.devGraphHeight;

		final int devXPrev = devXPositions[valueIndexFirstPoint];
		int devXHrStart = devXPositions[valueIndexFirstPoint];

		final HrZoneContext hrZoneContext = tourData.getHrZoneContext();
		int prevZoneIndex = TrainingManager.getZoneIndex(hrZoneContext, pulseSerie[valueIndexFirstPoint]);

		for (int valueIndex = valueIndexFirstPoint + 1; valueIndex <= valueIndexLastPoint; valueIndex++) {

			final int devXCurrent = devXPositions[valueIndex];
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

			final int devWidth = devXCurrent - devXHrStart;

			if (isBgColor) {
				gcGraph.setBackground(_hrZoneColors[prevZoneIndex]);
			} else {
				gcGraph.setForeground(_hrZoneColors[prevZoneIndex]);
			}

			if (isGradient) {
				gcGraph.fillGradientRectangle(devXHrStart, 0, devWidth, devCanvasHeight, true);
			} else {
				gcGraph.fillRectangle(devXHrStart, 0, devWidth, devCanvasHeight);
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
//	private void drawVertical(final GC gcGraph, final GraphDrawingData graphDrawingData, final Chart chart) {
//
//		final ChartDataModel dataModel = chart.getChartDataModel();
//		final TourData tourData = (TourData) dataModel.getCustomData(TourManager.CUSTOM_DATA_TOUR_DATA);
//
//		final TourPerson tourPerson = tourData.getTourPerson();
//		if (tourPerson == null) {
//			return;
//		}
//
//		final int numberOfHrZones = tourData.getNumberOfHrZones();
//		if (numberOfHrZones == 0) {
//			return;
//		}
//
//		final HrZoneContext hrZoneContext = tourData.getHrZoneContext();
//		final ArrayList<TourPersonHRZone> personHrZones = tourPerson.getHrZonesSorted();
////		final int zoneSize = personHrZones.size();
//
//		// get top/bottom border values of the graph
//		final int graphYTop = graphDrawingData.getGraphYTop();
//		final int graphYBottom = graphDrawingData.getGraphYBottom();
//
////		System.out.println("graphYTop:" + graphYTop + "\tgraphYBottom:" + graphYBottom);
////		// TODO remove SYSTEM.OUT.PRINTLN
//
//		final float scaleY = graphDrawingData.getScaleY();
//
//		final int devCanvasHeight = graphDrawingData.devGraphHeight;
//		final int devCanvasWidth = graphDrawingData.devVirtualGraphWidth;
//
////		final int devYTop = 0;
//		final int devYBottom = devCanvasHeight;
//
//		final int[] zoneMinBpm = hrZoneContext.zoneMinBpm;
//		final int[] zoneMaxBpm = hrZoneContext.zoneMaxBpm;
//
//		final Device display = gcGraph.getDevice();
//
//		// clip drawing at the graph border
////		gcGraph.setClipping(0, devYTop, gcGraph.getClipping().width, devYBottom - devYTop);
//
//		gcGraph.setAlpha(0xff);
//
//		for (int zoneIndex = 0; zoneIndex < zoneMinBpm.length; zoneIndex++) {
//
//			final int minBpm = zoneMinBpm[zoneIndex];
//			int maxBpm = zoneMaxBpm[zoneIndex];
//
//			// skip zones below the bottom
//			maxBpm = maxBpm == Integer.MAX_VALUE ? 10000 : maxBpm;
//
//			// + 1 is added because max is always next min -1
//			if (maxBpm + 1 < graphYBottom) {
//
////				System.out.println("maxBpm + 1 < graphYBottom:" + maxBpm + 1 + " / " + graphYBottom);
////				// TODO remove SYSTEM.OUT.PRINTLN
//
//				continue;
//			}
//
//			// skip zones above the graph
//			if (minBpm > graphYTop) {
////				System.out.println("minBpm > graphYTop:" + minBpm + " / " + graphYTop);
////				// TODO remove SYSTEM.OUT.PRINTLN
//
//				break;
//			}
////
////			// check zone bounds
////			if (zoneIndex >= zoneSize) {
////				break;
////			}
//
//			int devYMin = devYBottom - (int) ((minBpm - graphYBottom) * scaleY);
//			final int devYMax = devYBottom - (int) ((maxBpm + 1 - graphYBottom) * scaleY);
//
//			// ensure the dev values are within the graph otherwise they are not painted !!!
//			devYMin = devYMin > devCanvasHeight ? devCanvasHeight : devYMin;
//
//			final int devZoneHeight = devYMin - devYMax;
//
//			//
//
//			final TourPersonHRZone hrZone = personHrZones.get(zoneIndex);
//
//			final RGB rgb = hrZone.getColor();
//			final Color color = new Color(display, rgb);
//			{
//				gcGraph.setBackground(color);
//				gcGraph.fillRectangle(0, devYMin, devCanvasWidth, -devZoneHeight);
//			}
//			color.dispose();
//
////			System.out.println();
////			System.out.println(rgb);
//			// TODO remove SYSTEM.OUT.PRINTLN
//
////			System.out.println("bpm:"
////					+ minBpm
////					+ "/"
////					+ maxBpm
////					+ "\tdevY:"
////					+ devYMin
////					+ " / "
////					+ devYMax
////					+ "\th:"
////					+ devZoneHeight);
////			// TODO remove SYSTEM.OUT.PRINTLN
//		}
////
////		System.out.println("\t");
////		// TODO remove SYSTEM.OUT.PRINTLN
//
//		gcGraph.setAlpha(0xc0);
//
//	}

}
