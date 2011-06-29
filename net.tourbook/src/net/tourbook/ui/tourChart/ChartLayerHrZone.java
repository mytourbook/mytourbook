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
import net.tourbook.chart.IChartLayer;
import net.tourbook.data.HrZoneContext;
import net.tourbook.data.TourData;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourPersonHRZone;
import net.tourbook.tour.TourManager;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;

public class ChartLayerHrZone implements IChartLayer {

	@Override
	public void draw(final GC gcLayer, final GraphDrawingData drawingData, final Chart chart) {

		final ChartDataModel dataModel = chart.getChartDataModel();
		final TourData tourData = (TourData) dataModel.getCustomData(TourManager.CUSTOM_DATA_TOUR_DATA);

		final TourPerson tourPerson = tourData.getTourPerson();
		if (tourPerson == null) {
			return;
		}

		final int numberOfHrZones = tourData.getNumberOfHrZones();
		if (numberOfHrZones == 0) {
			return;
		}

		final HrZoneContext hrZoneContext = tourData.getHrZoneContext();
		final ArrayList<TourPersonHRZone> personHrZones = tourPerson.getHrZonesSorted();
		final int zoneSize = personHrZones.size();

		// get top/bottom border values of the graph
		final int graphYTop = drawingData.getGraphYTop();
		final int graphYBottom = drawingData.getGraphYBottom();

		final float scaleX = drawingData.getScaleX();
		final float scaleY = drawingData.getScaleY();

		final int devCanvasWidth = drawingData.devVirtualGraphWidth;
		final int devCanvasHeight = drawingData.devGraphHeight;
		final int devYTop = drawingData.getDevYTop();
		final int devYBottom = drawingData.getDevYBottom();
		final float devYGraphTop = scaleY * graphYTop;
		final float devYGraphBottom = (float) (scaleY * graphYBottom + 0.5);

		// graph x-axis: y = 0
		final int graphY_XAxisLine = graphYBottom > 0 ? graphYBottom : graphYTop < 0 ? graphYTop : 0;
//		final float devY0 = graphY_XAxisLine * scaleY;
		final float devY0 = graphYBottom * scaleY;

		System.out.println("devY0:\t" + devY0);
		System.out.println();
		// TODO remove SYSTEM.OUT.PRINTLN

		final int[] zoneMinBpm = hrZoneContext.zoneMinBpm;
		final int[] zoneMaxBpm = hrZoneContext.zoneMaxBpm;

		final Device display = gcLayer.getDevice();

		// clip drawing at the graph border
		gcLayer.setClipping(0, devYTop, gcLayer.getClipping().width, devYBottom - devYTop);

		gcLayer.setAlpha(0x60);

		for (int zoneIndex = 0; zoneIndex < zoneMinBpm.length; zoneIndex++) {

			final int minBpm = zoneMinBpm[zoneIndex];
			final int maxBpm = zoneMaxBpm[zoneIndex];

//			// skip hidden zones at the bottom
//			if (maxBpm < graphYBottom) {
//				continue;
//			}
//
//			// skip zones above the graph
//			if (minBpm > graphYTop) {
//				break;
//			}
//
//			// check zone bounds
//			if (zoneIndex >= zoneSize) {
//				break;
//			}

			final int devYMin = devYBottom - ((int) ((minBpm - graphYBottom) * scaleY));
			final int devYMax = devYBottom - ((int) ((maxBpm - graphYBottom) * scaleY));

			final int devZoneHeight = devYMin - devYMax;

			final TourPersonHRZone hrZone = personHrZones.get(zoneIndex);

			final RGB rgb = hrZone.getColor();
			final Color color = new Color(display, rgb);
			{
				gcLayer.setBackground(color);
				gcLayer.fillRectangle(0, devYMin, devCanvasWidth, devZoneHeight);
			}
			color.dispose();
		}

		gcLayer.setAlpha(0xff);

		System.out.println("\t");
		// TODO remove SYSTEM.OUT.PRINTLN

	}

}
