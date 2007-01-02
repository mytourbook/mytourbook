/*******************************************************************************
 * Copyright (C) 2006, 2007  Wolfgang Schramm
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
package net.tourbook.statistics;

import java.util.ArrayList;

import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.colors.GraphColors;
import net.tourbook.data.TourType;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.RGB;

public class StatisticServices {

	static IPreferenceStore	fPrefStore			= TourbookPlugin
														.getDefault()
														.getPreferenceStore();

	static final RGB		LINECOLOR_MARKER	= new RGB(200, 200, 200);
	static final RGB		FILLCOLOR_MARKER	= new RGB(216, 216, 216);
	static final RGB		FILLCOLOR2_MARKER	= new RGB(21, 0, 216);

	static void setChartColors(ChartDataYSerie yData, String graphName) {

		String prefGraphName = ITourbookPreferences.GRAPH_COLORS + graphName + ".";

		yData.setRgbBright(new RGB[] {
				PreferenceConverter.getColor(fPrefStore, prefGraphName
						+ GraphColors.PREF_COLOR_BRIGHT),
				FILLCOLOR_MARKER });

		yData.setRgbDark(new RGB[] {
				PreferenceConverter.getColor(fPrefStore, prefGraphName
						+ GraphColors.PREF_COLOR_DARK),
				FILLCOLOR2_MARKER });

		yData.setRgbLine(new RGB[] {
				PreferenceConverter.getColor(fPrefStore, prefGraphName
						+ GraphColors.PREF_COLOR_LINE),
				LINECOLOR_MARKER });
	}

	public static void setTourTypeColors(ChartDataYSerie yData, String graphName) {

		ArrayList<RGB> rgbBright = new ArrayList<RGB>();
		ArrayList<RGB> rgbDark = new ArrayList<RGB>();
		ArrayList<RGB> rgbLine = new ArrayList<RGB>();

		IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();
		ArrayList<TourType> tourTypes = TourbookPlugin.getDefault().getTourTypes();

		/*
		 * add default color
		 */
		String defaultColorName = ITourbookPreferences.GRAPH_COLORS + graphName + ".";
		rgbBright.add(PreferenceConverter.getColor(prefStore, defaultColorName
				+ GraphColors.PREF_COLOR_BRIGHT));

		rgbDark.add(PreferenceConverter.getColor(prefStore, defaultColorName
				+ GraphColors.PREF_COLOR_DARK));

		rgbLine.add(PreferenceConverter.getColor(prefStore, defaultColorName
				+ GraphColors.PREF_COLOR_LINE));

		/*
		 * add marker color
		 */
		rgbBright.add(StatisticServices.FILLCOLOR_MARKER);
		rgbDark.add(StatisticServices.FILLCOLOR2_MARKER);
		rgbLine.add(StatisticServices.LINECOLOR_MARKER);

		/*
		 * add tour type colors
		 */
		for (TourType tourType : tourTypes) {
			if (tourType.getTypeId() >= 0) {
				// type id is valid
				rgbBright.add(tourType.getRGBBright());
				rgbDark.add(tourType.getRGBDark());
				rgbLine.add(tourType.getRGBLine());
			}
		}

		// put the colors into the chart data
		yData.setRgbBright(rgbBright.toArray(new RGB[rgbBright.size()]));
		yData.setRgbDark(rgbDark.toArray(new RGB[rgbDark.size()]));
		yData.setRgbLine(rgbLine.toArray(new RGB[rgbLine.size()]));
	}

	/**
	 * create the color index for every tour type, <code>typeIds</code>
	 * contains all tour types
	 */
	public static void setTourTypeColorIndex(ChartDataYSerie yData, long[] typeIds) {
		setTourTypeColorIndex(yData, new long[][] { typeIds });
	}

	public static void setTourTypeColorIndex(ChartDataYSerie yData, long[][] typeIds) {

		ArrayList<TourType> tourTypes = TourbookPlugin.getDefault().getTourTypes();

		int[][] colorIndex = new int[typeIds.length][typeIds[0].length];
		int serieIndex = 0;

		for (long[] serieTypeIds : typeIds) {

			final int[] colorIndexSerie = new int[serieTypeIds.length];
			for (int tourTypeIdIndex = 0; tourTypeIdIndex < serieTypeIds.length; tourTypeIdIndex++) {

				long typeId = serieTypeIds[tourTypeIdIndex];
				int colorIndexIndex = 0;

				if (typeId != -1) {
					for (int typeIndex = 0; typeIndex < tourTypes.size(); typeIndex++) {
						if (typeId == (tourTypes.get(typeIndex)).getTypeId()) {
							colorIndexIndex = typeIndex;
							break;
						}
					}
				}
				colorIndexSerie[tourTypeIdIndex] = colorIndexIndex;
			}

			colorIndex[serieIndex] = colorIndexSerie;

			serieIndex++;
		}

		yData.setColorIndex(colorIndex);
	}

}
