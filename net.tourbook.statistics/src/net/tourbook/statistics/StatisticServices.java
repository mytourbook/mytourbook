/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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
import net.tourbook.colors.GraphColorDefaults;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.RGB;

public class StatisticServices {

	static IPreferenceStore	fPrefStore						= TourbookPlugin.getDefault()
																	.getPreferenceStore();

	/**
	 * offset for tour types in the color index
	 */
	public static int		TOUR_TYPE_COLOR_INDEX_OFFSET	= 1;

	public static void setTourTypeColors(ChartDataYSerie yData, String graphName) {

		ArrayList<RGB> rgbBright = new ArrayList<RGB>();
		ArrayList<RGB> rgbDark = new ArrayList<RGB>();
		ArrayList<RGB> rgbLine = new ArrayList<RGB>();

		/*
		 * color index 0: default color
		 */
		IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();
		String defaultColorName = ITourbookPreferences.GRAPH_COLORS + graphName + "."; //$NON-NLS-1$
		rgbBright.add(PreferenceConverter.getColor(prefStore, defaultColorName
				+ GraphColorDefaults.PREF_COLOR_BRIGHT));

		rgbDark.add(PreferenceConverter.getColor(prefStore, defaultColorName
				+ GraphColorDefaults.PREF_COLOR_DARK));

		rgbLine.add(PreferenceConverter.getColor(prefStore, defaultColorName
				+ GraphColorDefaults.PREF_COLOR_LINE));

		/*
		 * color index 1...n+1: tour type colors
		 */
		ArrayList<TourType> tourTypes = TourDatabase.getTourTypes();
		for (TourType tourType : tourTypes) {
			rgbBright.add(tourType.getRGBBright());
			rgbDark.add(tourType.getRGBDark());
			rgbLine.add(tourType.getRGBLine());
		}

		// put the colors into the chart data
		yData.setRgbBright(rgbBright.toArray(new RGB[rgbBright.size()]));
		yData.setRgbDark(rgbDark.toArray(new RGB[rgbDark.size()]));
		yData.setRgbLine(rgbLine.toArray(new RGB[rgbLine.size()]));
	}

	/**
	 * create the color index for every tour type, <code>typeIds</code> contain all tour types
	 */
	public static void setTourTypeColorIndex(ChartDataYSerie yData, long[][] typeIds) {

		ArrayList<TourType> tourTypes = TourDatabase.getTourTypes();

		int[][] colorIndex = new int[typeIds.length][typeIds[0].length];

		int serieIndex = 0;
		for (long[] serieTypeIds : typeIds) {

			final int[] colorIndexSerie = new int[serieTypeIds.length];
			for (int tourTypeIdIndex = 0; tourTypeIdIndex < serieTypeIds.length; tourTypeIdIndex++) {

				long typeId = serieTypeIds[tourTypeIdIndex];
				int tourTypeColorIndex = 0;

				if (typeId != -1) {
					for (int typeIndex = 0; typeIndex < tourTypes.size(); typeIndex++) {
						if ((tourTypes.get(typeIndex)).getTypeId() == typeId) {
							tourTypeColorIndex = typeIndex
									+ StatisticServices.TOUR_TYPE_COLOR_INDEX_OFFSET;
							break;
						}
					}
				}
				colorIndexSerie[tourTypeIdIndex] = tourTypeColorIndex;
			}

			colorIndex[serieIndex] = colorIndexSerie;

			serieIndex++;
		}

		yData.setColorIndex(colorIndex);
	}

}
