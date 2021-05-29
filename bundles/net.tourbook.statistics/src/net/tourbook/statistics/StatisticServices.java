/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartTitleSegmentConfig;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.UI;
import net.tourbook.common.color.GraphColorManager;
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.statistic.StatisticContext;
import net.tourbook.ui.TourTypeFilter;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.RGB;

public class StatisticServices {

   /**
    * Offset for tour types in the color index
    */
   public static int                     TOUR_TYPE_COLOR_INDEX_OFFSET = 1;

   private static final IPreferenceStore _prefStore_Common            = CommonActivator.getPrefStore();

   /**
    * @param serieIndex
    * @param activeTourTypeFilter
    * @return Returns the tour type id or -1 when type id is not set
    */
   public static long getTourTypeId(final int serieIndex, final TourTypeFilter activeTourTypeFilter) {

      int colorOffset = 0;
      if (activeTourTypeFilter.showUndefinedTourTypes()) {
         colorOffset = TOUR_TYPE_COLOR_INDEX_OFFSET;
      }

      if (serieIndex - colorOffset < 0) {
         return -1;
      }

      final ArrayList<TourType> allTourTypes = TourDatabase.getActiveTourTypes();
      final long typeId = allTourTypes.get(serieIndex - colorOffset).getTypeId();

      return typeId;
   }

   /**
    * @param serieIndex
    * @param valueIndex
    * @param resortedTypeIds
    * @param activeTourTypeFilter
    * @return Returns the tour type name for a data serie
    */
   public static String getTourTypeName(final int serieIndex,
                                        final int valueIndex,
                                        final long[][] resortedTypeIds,
                                        final TourTypeFilter activeTourTypeFilter) {

      final long typeId = resortedTypeIds[serieIndex][valueIndex];

      final String tourTypeName = TourDatabase.getTourTypeName(typeId);

      return tourTypeName;
   }

   /**
    * @param serieIndex
    * @param activeTourTypeFilter
    * @return Returns the tour type name for a data serie
    */
   public static String getTourTypeName(final int serieIndex, final TourTypeFilter activeTourTypeFilter) {

      int colorOffset = 0;
      if (activeTourTypeFilter.showUndefinedTourTypes()) {
         colorOffset = TOUR_TYPE_COLOR_INDEX_OFFSET;
      }

      if (serieIndex - colorOffset < 0) {
         return Messages.ui_tour_not_defined;
      }

      final ArrayList<TourType> tourTypeList = TourDatabase.getActiveTourTypes();
      final long typeId = tourTypeList.get(serieIndex - colorOffset).getTypeId();

      final String tourTypeName = TourDatabase.getTourTypeName(typeId);

      return tourTypeName;
   }

   /**
    * Set bar names into the statistic context. The names will be displayed in a combobox in the
    * statistics toolbar.
    *
    * @param statContext
    * @param allUsedTourTypeIds
    */
   @SuppressWarnings("unchecked")
   public static void setBarNames(final StatisticContext statContext,
                                  final long[] allUsedTourTypeIds,
                                  final int barOrderStart) {

      int numUsedTypes = 0;

      // get number of used tour types, a used tour type is not NO_TOUR_TYPE
      for (final long tourTypeId : allUsedTourTypeIds) {
         if (tourTypeId != TourType.TOUR_TYPE_IS_NOT_USED) {
            numUsedTypes++;
         }
      }

      ArrayList<TourType> allTourTypes = TourDatabase.getActiveTourTypes();

      final boolean isShowNoTourTypes = TourbookPlugin.getActiveTourTypeFilter().showUndefinedTourTypes();
      if (isShowNoTourTypes) {

         ArrayList<TourType> clonedTourTypes = new ArrayList<>();

         if (allTourTypes != null) {
            clonedTourTypes = (ArrayList<TourType>) allTourTypes.clone();
         }

         // add dummy tour type
         final TourType dummyTourType = new TourType(Messages.ui_tour_not_defined);
         dummyTourType.setTourId_NotDefinedInTourData();
         clonedTourTypes.add(0, dummyTourType);

         allTourTypes = clonedTourTypes;

      } else {

         if (allTourTypes == null || allTourTypes.isEmpty() || numUsedTypes == 0) {

            statContext.outIsUpdateBarNames = true;
            statContext.outBarNames = null;

            return;
         }
      }

      int barIndex = 0;

      // create bar names 2 times
      final String[] barNames = new String[numUsedTypes * 2];

      for (int inverseIndex = 0; inverseIndex < 2; inverseIndex++) {

         for (final TourType tourType : allTourTypes) {

            final long tourTypeId = tourType.getTypeId();

            /*
             * Check if this type is used
             */
            boolean isTourTypeUsed = false;
            long usedTourTypeId = 0;

            for (final long usedTourTypeIdValue : allUsedTourTypeIds) {

               usedTourTypeId = usedTourTypeIdValue;

               if (usedTourTypeId == tourTypeId) {
                  isTourTypeUsed = true;
                  break;
               }
            }

            if (isTourTypeUsed) {

               String barName;

               final String tourTypeName = usedTourTypeId == TourType.TOUR_TYPE_IS_NOT_DEFINED_IN_TOUR_DATA
                     ? Messages.ui_tour_not_defined
                     : tourType.getName();

               if (inverseIndex == 0) {
                  barName = tourTypeName;
               } else {
                  barName = tourTypeName
                        + UI.SPACE
                        + net.tourbook.statistics.Messages.Statistic_Label_Invers;
               }

               barNames[barIndex++] = barName;
            }
         }
      }

      // set state what the statistic container should do
      statContext.outIsUpdateBarNames = true;
      statContext.outBarNames = barNames;
      statContext.outVerticalBarIndex = barOrderStart;
   }

   /**
    * Set default colors for the y-axis, the color is defined in
    * {@link GraphColorManager#PREF_COLOR_LINE_LIGHT}
    *
    * @param yData
    * @param graphName
    */
   public static void setDefaultColors(final ChartDataYSerie yData, final String graphName) {

      final String defaultColorName = ICommonPreferences.GRAPH_COLORS + graphName + UI.SYMBOL_DOT;

      // put the color into the chart data
      yData.setDefaultRGB(PreferenceConverter.getColor(
            _prefStore_Common,
            defaultColorName + GraphColorManager.PREF_COLOR_LINE_LIGHT));
   }

   /**
    * create the color index for every tour type, <code>typeIds</code> contain all tour types
    *
    * @param tourTypeFilter
    */
   public static void setTourTypeColorIndex(final ChartDataYSerie yData,
                                            final long[][] resortedTypeIds,
                                            final TourTypeFilter tourTypeFilter) {

      final ArrayList<TourType> tourTypes = TourDatabase.getActiveTourTypes();

      int colorOffset = 0;
      if (tourTypeFilter.showUndefinedTourTypes()) {
         colorOffset = TOUR_TYPE_COLOR_INDEX_OFFSET;
      }

      final int[][] colorIndex = new int[resortedTypeIds.length][resortedTypeIds[0].length];

      int serieIndex = 0;
      for (final long[] typeIdSerie : resortedTypeIds) {

         final int[] colorIndexSerie = new int[typeIdSerie.length];
         for (int tourTypeIdIndex = 0; tourTypeIdIndex < typeIdSerie.length; tourTypeIdIndex++) {

            int tourTypeColorIndex = 0;

            final long typeId = typeIdSerie[tourTypeIdIndex];

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

   public static void setTourTypeColors(final ChartDataYSerie yData,
                                        final String graphName,
                                        final TourTypeFilter tourTypeFilter) {

      final ArrayList<RGB> rgbGradient_Bright = new ArrayList<>();
      final ArrayList<RGB> rgbGradient_Dark = new ArrayList<>();
      final ArrayList<RGB> rgbLine = new ArrayList<>();
      final ArrayList<RGB> rgbText = new ArrayList<>();

      /*
       * Set default color when tours are displayed where the tour type is not set, these tour will
       * be painted in the default color
       */
      if (tourTypeFilter.showUndefinedTourTypes()) {

         /*
          * Color index 0: default color
          */
         final String defaultColorName = ICommonPreferences.GRAPH_COLORS + graphName + UI.SYMBOL_DOT;

         final String prefColorLineThemed = UI.IS_DARK_THEME
               ? GraphColorManager.PREF_COLOR_LINE_DARK
               : GraphColorManager.PREF_COLOR_LINE_LIGHT;

         final String prefColorTextThemed = UI.IS_DARK_THEME
               ? GraphColorManager.PREF_COLOR_TEXT_DARK
               : GraphColorManager.PREF_COLOR_TEXT_LIGHT;

         rgbGradient_Bright.add(PreferenceConverter.getColor(_prefStore_Common, defaultColorName + GraphColorManager.PREF_COLOR_GRADIENT_BRIGHT));
         rgbGradient_Dark.add(PreferenceConverter.getColor(_prefStore_Common, defaultColorName + GraphColorManager.PREF_COLOR_GRADIENT_DARK));

         rgbLine.add(PreferenceConverter.getColor(_prefStore_Common, defaultColorName + prefColorLineThemed));
         rgbText.add(PreferenceConverter.getColor(_prefStore_Common, defaultColorName + prefColorTextThemed));
      }

      /*
       * Color index 1...n+1: tour type colors
       */
      final ArrayList<TourType> tourTypes = TourDatabase.getActiveTourTypes();

      for (final TourType tourType : tourTypes) {

         rgbGradient_Bright.add(tourType.getRGB_Gradient_Bright());
         rgbGradient_Dark.add(tourType.getRGB_Gradient_Dark());

         rgbLine.add(tourType.getRGB_Line_Themed());
         rgbText.add(tourType.getRGB_Text_Themed());
      }

      // put the colors into the chart data
      yData.setRgbGradient_Bright(rgbGradient_Bright.toArray(new RGB[rgbGradient_Bright.size()]));
      yData.setRgbGradient_Dark(rgbGradient_Dark.toArray(new RGB[rgbGradient_Dark.size()]));
      yData.setRgbLine(rgbLine.toArray(new RGB[rgbLine.size()]));
      yData.setRgbText(rgbText.toArray(new RGB[rgbText.size()]));

//      /*
//       * Dump tour type colors
//       */
//      System.out.println(UI.EMPTY_STRING);
//      System.out.println("setTourTypeColors()"); //$NON-NLS-1$
//
//      for (final TourType tourType : tourTypes) {
//
//         System.out.println(UI.EMPTY_STRING);
//         System.out.println(tourType.getName());
//         System.out.println(UI.EMPTY_STRING);
//
//         final StringBuilder sb = new StringBuilder();
//
//         RGB rgb = tourType.getRGBBright();
//         sb.append("new RGB(" + rgb.red + ", " + rgb.green + ", " + rgb.blue + "),\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
//
//         rgb = tourType.getRGBDark();
//         sb.append("new RGB(" + rgb.red + ", " + rgb.green + ", " + rgb.blue + "),\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
//
//         rgb = tourType.getRGBLine();
//         sb.append("new RGB(" + rgb.red + ", " + rgb.green + ", " + rgb.blue + "),\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
//
//         rgb = tourType.getRGBText();
//         sb.append("new RGB(" + rgb.red + ", " + rgb.green + ", " + rgb.blue + "),\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
//
//         System.out.println(sb.toString());
//      }
   }

   /**
    * Set chart properties from the pref store.
    *
    * @param chart
    * @param prefGridPrefix
    *           Prefix for grid preferences.
    */
   public static void updateChartProperties(final Chart chart, final String prefGridPrefix) {

      net.tourbook.ui.UI.updateChartProperties(chart, prefGridPrefix);

      /*
       * These settings are currently static, a UI to modify it is not yet implemented.
       */
      final ChartTitleSegmentConfig ctsConfig = chart.getChartTitleSegmentConfig();

      ctsConfig.isMultipleSegments = true;

      ctsConfig.isShowSegmentBackground = false;
      ctsConfig.isShowSegmentSeparator = true;
      ctsConfig.isShowSegmentTitle = true;
   }

}
