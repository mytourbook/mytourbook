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
package net.tourbook.tour;

import java.text.NumberFormat;

import net.tourbook.chart.ColorCache;
import net.tourbook.chart.IChartInfoPainter;
import net.tourbook.colors.GraphColorProvider;
import net.tourbook.data.TourData;
import net.tourbook.ui.UI;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class ChartInfoPainter implements IChartInfoPainter {

	private final DateTimeFormatter	_dtFormatter	= DateTimeFormat.mediumDateTime();

	private final NumberFormat		_nf1			= NumberFormat.getNumberInstance();
	private final NumberFormat		_nf1NoGroup		= NumberFormat.getNumberInstance();
	private final NumberFormat		_nf3			= NumberFormat.getNumberInstance();
	private final NumberFormat		_nf3NoGroup		= NumberFormat.getNumberInstance();
	{
		_nf1.setMinimumFractionDigits(1);
		_nf1.setMaximumFractionDigits(1);
		_nf3.setMinimumFractionDigits(3);
		_nf3.setMaximumFractionDigits(3);

		_nf1NoGroup.setMinimumFractionDigits(1);
		_nf1NoGroup.setMaximumFractionDigits(1);
		_nf1NoGroup.setGroupingUsed(false);

		_nf3NoGroup.setMinimumFractionDigits(3);
		_nf3NoGroup.setMaximumFractionDigits(3);
		_nf3NoGroup.setGroupingUsed(false);
	}

	private TourData				_tourData;

	@Override
	public void drawChartInfo(	final GC gc,
								final int devXMouse,
								final int devYMouse,
								final int valueIndex,
								final ColorCache colorCache) {

		if (_tourData == null) {
			return;
		}

		final int[] timeSerie = _tourData.timeSerie;

		// check array bounds
		if (valueIndex >= timeSerie.length) {
			return;
		}

//		final long start = System.nanoTime();

		final float[] altitudeSerie = _tourData.altitudeSerie;
		final float[] cadenceSerie = _tourData.cadenceSerie;
		final float[] distanceSerie = _tourData.distanceSerie;
		final float[] gradientSerie = _tourData.gradientSerie;
		final float[] paceSerie = _tourData.getPaceSerie();
		final float[] powerSerie = _tourData.getPowerSerie();
		final float[] pulseSerie = _tourData.pulseSerie;
		final float[] temperatureSerie = _tourData.temperatureSerie;
//				final float[] Serie = _tourData

		final boolean isTime = timeSerie != null;
		final boolean isDistance = distanceSerie != null;
//		final boolean isDistance = distanceSerie != null;
//
//		final boolean isDistance = setupDistanceStartingValues(timeDataSerie, isAbsoluteData);
//		final boolean isAltitude = setupAltitudeStartingValues(timeDataSerie, isAbsoluteData);
//		final boolean isPulse = setupPulseStartingValues(timeDataSerie);
//		final boolean isCadence = setupCadenceStartingValues(timeDataSerie);
//		final boolean isTemperature = setupTemperatureStartingValues(timeDataSerie);

		final boolean isSpeed = false;
		final boolean isPower = false;

		final GraphColorProvider colorProvider = GraphColorProvider.getInstance();

		gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));

		final int devX = devXMouse - 10;
		final int devY = devYMouse - 80;

		final Point textExtend = gc.textExtent(UI.SPACE);
		final int lineHeight = textExtend.y + 0;
		int devYLine = devY;

		if (timeSerie != null) {

			final Color fgColor = colorCache.getColor(//
					GraphColorProvider.PREF_GRAPH_TIME, //
					colorProvider.getGraphColorDefinition(GraphColorProvider.PREF_GRAPH_TIME).getTextColor());

			final float time = timeSerie[valueIndex];
			final String valueText = UI.format_hhh_mm_ss((long) time) + UI.SPACE + UI.UNIT_LABEL_TIME;

			devYLine += lineHeight;

			gc.setForeground(fgColor);
			gc.drawText(valueText, devX, devYLine);
		}
		if (distanceSerie != null) {

			final Color fgColor = colorCache.getColor(//
					GraphColorProvider.PREF_GRAPH_DISTANCE, //
					colorProvider.getGraphColorDefinition(GraphColorProvider.PREF_GRAPH_DISTANCE).getTextColor());

			final float distance = distanceSerie[valueIndex] / 1000 / UI.UNIT_VALUE_DISTANCE;
			final String valueText = _nf3NoGroup.format(distance) + UI.SPACE + UI.UNIT_LABEL_DISTANCE;

			devYLine += lineHeight;

			gc.setForeground(fgColor);
			gc.drawText(valueText, devX, devYLine);
		}

		if (altitudeSerie != null) {

			final Color fgColor = colorCache.getColor(//
					GraphColorProvider.PREF_GRAPH_ALTITUDE, //
					colorProvider.getGraphColorDefinition(GraphColorProvider.PREF_GRAPH_ALTITUDE).getTextColor());

			devYLine += lineHeight;
			final String valueText = _nf3NoGroup.format(altitudeSerie[valueIndex]) + UI.SPACE + UI.UNIT_LABEL_ALTITUDE;

			gc.setForeground(fgColor);
			gc.drawText(valueText, devX, devYLine);
		}

//		final long end = System.nanoTime();
//		System.out.println("drawChartInfo\t" + (((double) end - start) / 1000000) + " ms");
//		// TODO remove SYSTEM.OUT.PRINTLN

	}

	public void setTourData(final TourData tourData) {
		_tourData = tourData;
	}
}
