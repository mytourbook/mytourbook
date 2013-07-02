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
package net.tourbook.statistics;

import java.util.ArrayList;

import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.color.GraphColorProvider;
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.TourTypeFilter;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.RGB;

public class StatisticServices {

	/**
	 * offset for tour types in the color index
	 */
	public static int	TOUR_TYPE_COLOR_INDEX_OFFSET	= 1;

	/**
	 * Set default colors for the y-axis, the color is defined in
	 * {@link GraphColorProvider#PREF_COLOR_LINE}
	 * 
	 * @param yData
	 * @param graphName
	 */
	public static void setDefaultColors(final ChartDataYSerie yData, final String graphName) {

		final IPreferenceStore commonPrefStore = CommonActivator.getPrefStore();

		final String defaultColorName = ICommonPreferences.GRAPH_COLORS + graphName + "."; //$NON-NLS-1$

		// put the color into the chart data
		yData.setDefaultRGB(PreferenceConverter.getColor(//
				commonPrefStore,
				defaultColorName + GraphColorProvider.PREF_COLOR_LINE));
	}

	/**
	 * create the color index for every tour type, <code>typeIds</code> contain all tour types
	 * 
	 * @param tourTypeFilter
	 */
	public static void setTourTypeColorIndex(	final ChartDataYSerie yData,
												final long[][] allTypeIds,
												final TourTypeFilter tourTypeFilter) {

		final ArrayList<TourType> tourTypes = TourDatabase.getActiveTourTypes();

		int colorOffset = 0;
		if (tourTypeFilter.showUndefinedTourTypes()) {
			colorOffset = StatisticServices.TOUR_TYPE_COLOR_INDEX_OFFSET;
		}

		final int[][] colorIndex = new int[allTypeIds.length][allTypeIds[0].length];

		int serieIndex = 0;
		for (final long[] typeIdSerie : allTypeIds) {

			final int[] colorIndexSerie = new int[typeIdSerie.length];
			for (int tourTypeIdIndex = 0; tourTypeIdIndex < typeIdSerie.length; tourTypeIdIndex++) {

				final long typeId = typeIdSerie[tourTypeIdIndex];
				int tourTypeColorIndex = 0;

				if (typeId != -1) {
					for (int typeIndex = 0; typeIndex < tourTypes.size(); typeIndex++) {
						if ((tourTypes.get(typeIndex)).getTypeId() == typeId) {
							tourTypeColorIndex = colorOffset + typeIndex;
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

	public static void setTourTypeColors(	final ChartDataYSerie yData,
											final String graphName,
											final TourTypeFilter tourTypeFilter) {

		final ArrayList<RGB> rgbBright = new ArrayList<RGB>();
		final ArrayList<RGB> rgbDark = new ArrayList<RGB>();
		final ArrayList<RGB> rgbLine = new ArrayList<RGB>();
		final ArrayList<RGB> rgbText = new ArrayList<RGB>();

		/*
		 * set default color when tours are displayed where the tour type is not set, these tour
		 * will be painted in the default color
		 */
		if (tourTypeFilter.showUndefinedTourTypes()) {

			/*
			 * color index 0: default color
			 */
			final IPreferenceStore commonPrefStore = CommonActivator.getPrefStore();
			final String defaultColorName = ICommonPreferences.GRAPH_COLORS + graphName + "."; //$NON-NLS-1$

			rgbBright.add(PreferenceConverter.getColor(//
					commonPrefStore,
					defaultColorName + GraphColorProvider.PREF_COLOR_BRIGHT));

			rgbDark.add(PreferenceConverter.getColor(//
					commonPrefStore,
					defaultColorName + GraphColorProvider.PREF_COLOR_DARK));

			rgbLine.add(PreferenceConverter.getColor(//
					commonPrefStore,
					defaultColorName + GraphColorProvider.PREF_COLOR_LINE));

			rgbText.add(PreferenceConverter.getColor(//
					commonPrefStore,
					defaultColorName + GraphColorProvider.PREF_COLOR_TEXT));
		}

		/*
		 * color index 1...n+1: tour type colors
		 */
		final ArrayList<TourType> tourTypes = TourDatabase.getActiveTourTypes();
		for (final TourType tourType : tourTypes) {
			rgbBright.add(tourType.getRGBBright());
			rgbDark.add(tourType.getRGBDark());
			rgbLine.add(tourType.getRGBLine());
			rgbText.add(tourType.getRGBText());
		}

		// put the colors into the chart data
		yData.setRgbBright(rgbBright.toArray(new RGB[rgbBright.size()]));
		yData.setRgbDark(rgbDark.toArray(new RGB[rgbDark.size()]));
		yData.setRgbLine(rgbLine.toArray(new RGB[rgbLine.size()]));
		yData.setRgbText(rgbText.toArray(new RGB[rgbText.size()]));

//		/*
//		 * dump tour type colors
//		 */
//		System.out.println(UI.EMPTY_STRING);
//		System.out.println("setTourTypeColors()"); //$NON-NLS-1$
//
//		for (final TourType tourType : tourTypes) {
//
//			System.out.println(UI.EMPTY_STRING);
//			System.out.println(tourType.getName());
//			System.out.println(UI.EMPTY_STRING);
//
//			final StringBuilder sb = new StringBuilder();
//
//			RGB rgb = tourType.getRGBBright();
//			sb.append("new RGB(" + rgb.red + ", " + rgb.green + ", " + rgb.blue + "),\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
//
//			rgb = tourType.getRGBDark();
//			sb.append("new RGB(" + rgb.red + ", " + rgb.green + ", " + rgb.blue + "),\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
//
//			rgb = tourType.getRGBLine();
//			sb.append("new RGB(" + rgb.red + ", " + rgb.green + ", " + rgb.blue + "),\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
//
//			rgb = tourType.getRGBText();
//			sb.append("new RGB(" + rgb.red + ", " + rgb.green + ", " + rgb.blue + "),\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
//
//			System.out.println(sb.toString());
//		}
	}

}
