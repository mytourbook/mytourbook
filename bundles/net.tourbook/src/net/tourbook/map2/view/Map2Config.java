/*******************************************************************************
 * Copyright (C) 2024 Wolfgang Schramm and Contributors
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
package net.tourbook.map2.view;

import java.awt.Color;

import net.tourbook.common.UI;
import net.tourbook.tour.filter.TourFilterFieldOperator;

import org.eclipse.swt.graphics.RGB;

/**
 * Contains data which are needed to paint a map point into the 2D map
 */
public class Map2Config {

   private static final char NL = UI.NEW_LINE;

   /*
    * Set default values also here to ensure that a valid value is set. A default value would not be
    * set when an xml tag is not available.
    */

   public String id        = Long.toString(System.nanoTime());
   public String defaultId = Map2ConfigManager.CONFIG_DEFAULT_ID_1;
   public String name      = Map2ConfigManager.CONFIG_DEFAULT_ID_1;

   /*
    * Common
    */
   public boolean        isTruncateLabel;
   public int            labelDistributorMaxLabels = Map2ConfigManager.LABEL_DISTRIBUTOR_MAX_LABELS_DEFAULT;
   public int            labelDistributorRadius    = Map2ConfigManager.LABEL_DISTRIBUTOR_RADIUS_DEFAULT;
   public int            labelTruncateLength       = Map2ConfigManager.LABEL_TRUNCATE_LENGTH_DEFAULT;
   public MapLabelLayout labelLayout               = Map2ConfigManager.LABEL_LAYOUT_DEFAULT;

   public boolean        isLabelAntialiased        = true;

   public String         labelFontName             = Map2ConfigManager.LABEL_FONT_NAME_DEFAULT;
   public int            labelFontSize             = Map2ConfigManager.LABEL_FONT_SIZE_DEFAULT;
   public int            labelRespectMargin        = Map2ConfigManager.LABEL_RESPECT_MARGIN_DEFAULT;
   public int            locationSymbolSize        = Map2ConfigManager.LOCATION_SYMBOL_SIZE_DEFAULT;

   /*
    * Tour/common locations
    */
   public boolean                        isShowTourLocation            = true;
   public boolean                        isShowCommonLocation          = true;
   public boolean                        isShowLocationBoundingBox;

   public RGB                            commonLocationFill_RGB        = Map2ConfigManager.DEFAULT_COMMON_LOCATION_FILL_RGB;
   public RGB                            commonLocationOutline_RGB     = Map2ConfigManager.DEFAULT_COMMON_LOCATION_OUTLINE_RGB;

   public RGB                            tourLocationFill_RGB          = Map2ConfigManager.DEFAULT_TOUR_LOCATION_FILL_RGB;
   public RGB                            tourLocationOutline_RGB       = Map2ConfigManager.DEFAULT_TOUR_LOCATION_OUTLINE_RGB;
   public RGB                            tourLocation_StartFill_RGB    = Map2ConfigManager.DEFAULT_TOUR_LOCATION_START_FILL_RGB;
   public RGB                            tourLocation_StartOutline_RGB = Map2ConfigManager.DEFAULT_TOUR_LOCATION_START_OUTLINE_RGB;
   public RGB                            tourLocation_EndFill_RGB      = Map2ConfigManager.DEFAULT_TOUR_LOCATION_END_FILL_RGB;
   public RGB                            tourLocation_EndOutline_RGB   = Map2ConfigManager.DEFAULT_TOUR_LOCATION_END_OUTLINE_RGB;

   public Color                          commonLocationFill_ColorAWT;
   public Color                          commonLocationOutline_ColorAWT;

   public Color                          tourLocationFill_ColorAWT;
   public Color                          tourLocationOutline_ColorAWT;
   public Color                          tourLocation_StartFill_ColorAWT;
   public Color                          tourLocation_StartOutline_ColorAWT;
   public Color                          tourLocation_EndFill_ColorAWT;
   public Color                          tourLocation_EndOutline_ColorAWT;

   public org.eclipse.swt.graphics.Color commonLocationFill_ColorSWT;
   public org.eclipse.swt.graphics.Color commonLocationOutline_ColorSWT;

   public org.eclipse.swt.graphics.Color tourLocationFill_ColorSWT;
   public org.eclipse.swt.graphics.Color tourLocationOutline_ColorSWT;
   public org.eclipse.swt.graphics.Color tourLocation_StartFill_ColorSWT;
   public org.eclipse.swt.graphics.Color tourLocation_StartOutline_ColorSWT;
   public org.eclipse.swt.graphics.Color tourLocation_EndFill_ColorSWT;
   public org.eclipse.swt.graphics.Color tourLocation_EndOutline_ColorSWT;

   /*
    * Tour markers
    */
   public boolean                        isShowTourMarker      = true;

   /** When <code>true</code> then markers with the same label are grouped together */
   public boolean                        isGroupDuplicatedMarkers;
   public String                         groupedMarkers        = UI.EMPTY_STRING;
   public int                            groupGridSize         = Map2ConfigManager.LABEL_GROUP_GRID_SIZE_DEFAULT;

   public RGB                            tourMarkerFill_RGB    = Map2ConfigManager.DEFAULT_TOUR_MARKER_FILL_RGB;
   public RGB                            tourMarkerOutline_RGB = Map2ConfigManager.DEFAULT_TOUR_MARKER_OUTLINE_RGB;

   public Color                          tourMarkerFill_ColorAWT;
   public Color                          tourMarkerOutline_ColorAWT;
   public org.eclipse.swt.graphics.Color tourMarkerFill_ColorSWT;
   public org.eclipse.swt.graphics.Color tourMarkerOutline_ColorSWT;

   /*
    * Tour marker cluster
    */
   public boolean                        isTourMarkerClustered;

   public boolean                        isFillClusterSymbol  = Map2ConfigManager.DEFAULT_IS_FILL_CLUSTER_SYMBOL;
   public int                            clusterGridSize      = Map2ConfigManager.DEFAULT_CLUSTER_GRID_SIZE;
   public RGB                            clusterFill_RGB      = Map2ConfigManager.DEFAULT_CLUSTER_FILL_RGB;
   public RGB                            clusterOutline_RGB   = Map2ConfigManager.DEFAULT_CLUSTER_OUTLINE_RGB;
   public int                            clusterOutline_Width = Map2ConfigManager.DEFAULT_CLUSTER_OUTLINE_WIDTH;
   public int                            clusterSymbol_Size   = Map2ConfigManager.DEFAULT_CLUSTER_SYMBOL_SIZE;

   public Color                          clusterFill_ColorAWT;
   public Color                          clusterOutline_ColorAWT;
   public org.eclipse.swt.graphics.Color clusterFill_ColorSWT;
   public org.eclipse.swt.graphics.Color clusterOutline_ColorSWT;

   /*
    * Tour pauses
    */
   public boolean                        isShowTourPauses     = true;

   public RGB                            tourPauseFill_RGB    = Map2ConfigManager.DEFAULT_TOUR_PAUSE_FILL_RGB;
   public RGB                            tourPauseOutline_RGB = Map2ConfigManager.DEFAULT_TOUR_PAUSE_OUTLINE_RGB;

   public Color                          tourPauseFill_ColorAWT;
   public Color                          tourPauseOutline_ColorAWT;
   public org.eclipse.swt.graphics.Color tourPauseFill_ColorSWT;
   public org.eclipse.swt.graphics.Color tourPauseOutline_ColorSWT;

   /*
    * Tour pause filter
    */
   public boolean                 isFilterTourPauses;
   public boolean                 isFilterTourPause_Duration          = true;
   public boolean                 isShowAutoPauses                    = true;
   public boolean                 isShowUserPauses                    = true;

   public boolean                 useTourPause_DurationFilter_Hours;
   public boolean                 useTourPause_DurationFilter_Minutes = true;
   public boolean                 useTourPause_DurationFilter_Seconds;

   public int                     tourPauseDurationFilter_Hours;
   public int                     tourPauseDurationFilter_Minutes     = 5;
   public int                     tourPauseDurationFilter_Seconds;
   public long                    tourPauseDuration;

   public TourFilterFieldOperator tourPauseDurationFilter_Operator;

   /*
    * Photo
    */
   public RGB                            photoFill_RGB    = Map2ConfigManager.DEFAULT_PHOTO_FILL_RGB;
   public RGB                            photoOutline_RGB = Map2ConfigManager.DEFAULT_PHOTO_OUTLINE_RGB;

   public Color                          photoFill_ColorAWT;
   public Color                          photoOutline_ColorAWT;
   public org.eclipse.swt.graphics.Color photoFill_ColorSWT;
   public org.eclipse.swt.graphics.Color photoOutline_ColorSWT;

   private void logColor(final StringBuilder sb, final org.eclipse.swt.graphics.Color color, final String name) {

      sb.append("public static final RGB %-45s = new RGB(%d, %d, %d);\n" //$NON-NLS-1$

            .formatted(

                  name,

                  color.getRed(),
                  color.getGreen(),
                  color.getBlue()));
   }

   public void setupColors() {

// SET_FORMATTING_OFF

      commonLocationFill_ColorSWT         = new org.eclipse.swt.graphics.Color(commonLocationFill_RGB);
      commonLocationOutline_ColorSWT      = new org.eclipse.swt.graphics.Color(commonLocationOutline_RGB);

      tourLocationFill_ColorSWT           = new org.eclipse.swt.graphics.Color(tourLocationFill_RGB);
      tourLocationOutline_ColorSWT        = new org.eclipse.swt.graphics.Color(tourLocationOutline_RGB);
      tourLocation_StartFill_ColorSWT     = new org.eclipse.swt.graphics.Color(tourLocation_StartFill_RGB);
      tourLocation_StartOutline_ColorSWT  = new org.eclipse.swt.graphics.Color(tourLocation_StartOutline_RGB);
      tourLocation_EndFill_ColorSWT       = new org.eclipse.swt.graphics.Color(tourLocation_EndFill_RGB);
      tourLocation_EndOutline_ColorSWT    = new org.eclipse.swt.graphics.Color(tourLocation_EndOutline_RGB);

      tourMarkerFill_ColorSWT             = new org.eclipse.swt.graphics.Color(tourMarkerFill_RGB);
      tourMarkerOutline_ColorSWT          = new org.eclipse.swt.graphics.Color(tourMarkerOutline_RGB);

      clusterFill_ColorSWT                = new org.eclipse.swt.graphics.Color(clusterFill_RGB);
      clusterOutline_ColorSWT             = new org.eclipse.swt.graphics.Color(clusterOutline_RGB);

      tourPauseFill_ColorSWT              = new org.eclipse.swt.graphics.Color(tourPauseFill_RGB);
      tourPauseOutline_ColorSWT           = new org.eclipse.swt.graphics.Color(tourPauseOutline_RGB);

      photoFill_ColorSWT                  = new org.eclipse.swt.graphics.Color(photoFill_RGB);
      photoOutline_ColorSWT               = new org.eclipse.swt.graphics.Color(photoOutline_RGB);

      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

      commonLocationFill_ColorAWT         = new Color(commonLocationFill_RGB.red,         commonLocationFill_RGB.green,          commonLocationFill_RGB.blue);
      commonLocationOutline_ColorAWT      = new Color(commonLocationOutline_RGB.red,      commonLocationOutline_RGB.green,       commonLocationOutline_RGB.blue);

      tourLocationFill_ColorAWT           = new Color(tourLocationFill_RGB.red,           tourLocationFill_RGB.green,            tourLocationFill_RGB.blue);
      tourLocationOutline_ColorAWT        = new Color(tourLocationOutline_RGB.red,        tourLocationOutline_RGB.green,         tourLocationOutline_RGB.blue);
      tourLocation_StartFill_ColorAWT     = new Color(tourLocation_StartFill_RGB.red,     tourLocation_StartFill_RGB.green,      tourLocation_StartFill_RGB.blue);
      tourLocation_StartOutline_ColorAWT  = new Color(tourLocation_StartOutline_RGB.red,  tourLocation_StartOutline_RGB.green,   tourLocation_StartOutline_RGB.blue);
      tourLocation_EndFill_ColorAWT       = new Color(tourLocation_EndFill_RGB.red,       tourLocation_EndFill_RGB.green,        tourLocation_EndFill_RGB.blue);
      tourLocation_EndOutline_ColorAWT    = new Color(tourLocation_EndOutline_RGB.red,    tourLocation_EndOutline_RGB.green,     tourLocation_EndOutline_RGB.blue);

      tourMarkerFill_ColorAWT             = new Color(tourMarkerFill_RGB.red,    tourMarkerFill_RGB.green,     tourMarkerFill_RGB.blue);
      tourMarkerOutline_ColorAWT          = new Color(tourMarkerOutline_RGB.red, tourMarkerOutline_RGB.green,  tourMarkerOutline_RGB.blue);

      clusterFill_ColorAWT                = new Color(clusterFill_RGB.red,       clusterFill_RGB.green,        clusterFill_RGB.blue);
      clusterOutline_ColorAWT             = new Color(clusterOutline_RGB.red,    clusterOutline_RGB.green,     clusterOutline_RGB.blue);

      tourPauseFill_ColorAWT              = new Color(tourPauseFill_RGB.red,     tourPauseFill_RGB.green,      tourPauseFill_RGB.blue);
      tourPauseOutline_ColorAWT           = new Color(tourPauseOutline_RGB.red,  tourPauseOutline_RGB.green,   tourPauseOutline_RGB.blue);

      photoFill_ColorAWT                  = new Color(photoFill_RGB.red,         photoFill_RGB.green,          photoFill_RGB.blue);
      photoOutline_ColorAWT               = new Color(photoOutline_RGB.red,      photoOutline_RGB.green,       photoOutline_RGB.blue);

      /*
       * Log color which is used when defining defaults
       */
      final boolean isLogColor = false;
      if (isLogColor) {

         final StringBuilder sb = new StringBuilder();


         logColor(sb, clusterFill_ColorSWT,                 "DEFAULT_CLUSTER_FILL_RGB");                    //$NON-NLS-1$
         logColor(sb, clusterOutline_ColorSWT,              "DEFAULT_CLUSTER_OUTLINE_RGB");                 //$NON-NLS-1$

         logColor(sb, commonLocationFill_ColorSWT,          "DEFAULT_COMMON_LOCATION_FILL_RGB");            //$NON-NLS-1$
         logColor(sb, commonLocationOutline_ColorSWT,       "DEFAULT_COMMON_LOCATION_OUTLINE_RGB");         //$NON-NLS-1$

         logColor(sb, photoFill_ColorSWT,                   "DEFAULT_PHOTO_FILL_RGB");                      //$NON-NLS-1$
         logColor(sb, photoOutline_ColorSWT,                "DEFAULT_PHOTO_OUTLINE_RGB");                   //$NON-NLS-1$

         logColor(sb, tourLocationFill_ColorSWT,            "DEFAULT_TOUR_LOCATION_FILL_RGB");              //$NON-NLS-1$
         logColor(sb, tourLocationOutline_ColorSWT,         "DEFAULT_TOUR_LOCATION_OUTLINE_RGB");           //$NON-NLS-1$
         logColor(sb, tourLocation_StartFill_ColorSWT,      "DEFAULT_TOUR_LOCATION_START_FILL_RGB");        //$NON-NLS-1$
         logColor(sb, tourLocation_StartOutline_ColorSWT,   "DEFAULT_TOUR_LOCATION_START_OUTLINE_RGB");     //$NON-NLS-1$
         logColor(sb, tourLocation_EndFill_ColorSWT,        "DEFAULT_TOUR_LOCATION_END_FILL_RGB");          //$NON-NLS-1$
         logColor(sb, tourLocation_EndOutline_ColorSWT,     "DEFAULT_TOUR_LOCATION_END_OUTLINE_RGB");       //$NON-NLS-1$

         logColor(sb, tourMarkerFill_ColorSWT,              "DEFAULT_TOUR_MARKER_FILL_RGB");                //$NON-NLS-1$
         logColor(sb, tourMarkerOutline_ColorSWT,           "DEFAULT_TOUR_MARKER_OUTLINE_RGB");             //$NON-NLS-1$

         logColor(sb, tourPauseFill_ColorSWT,               "DEFAULT_TOUR_PAUSE_FILL_RGB");                 //$NON-NLS-1$
         logColor(sb, tourPauseOutline_ColorSWT,            "DEFAULT_TOUR_PAUSE_OUTLINE_RGB");              //$NON-NLS-1$

         System.out.println(sb.toString());
         System.out.println();
         System.out.println();
      }

// SET_FORMATTING_ON
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "Map2MarkerConfig" + NL //                                               //$NON-NLS-1$

            + " name                      = " + name + NL //                           //$NON-NLS-1$
            + " defaultId                 = " + defaultId + NL //                      //$NON-NLS-1$
            + " id                        = " + id + NL //                             //$NON-NLS-1$

            + " isShowTourMarker          = " + isShowTourMarker + NL //               //$NON-NLS-1$
            + " markerOutline_RGB         = " + tourMarkerOutline_RGB + NL //          //$NON-NLS-1$
            + " markerFill_RGB            = " + tourMarkerFill_RGB + NL //             //$NON-NLS-1$

            + " isMarkerClustered            = " + isTourMarkerClustered + NL //       //$NON-NLS-1$
            + " isLabelAntialiased           = " + isLabelAntialiased + NL //          //$NON-NLS-1$
            + " isSymbolAntialiased          = " + isFillClusterSymbol + NL //         //$NON-NLS-1$
            + " clusterGridSize              = " + clusterGridSize + NL //             //$NON-NLS-1$
            + " clusterSymbol_Size           = " + clusterSymbol_Size + NL //          //$NON-NLS-1$
            + " clusterOutline_Width         = " + clusterOutline_Width + NL //        //$NON-NLS-1$
            + " clusterOutline_RGB           = " + clusterOutline_RGB + NL //          //$NON-NLS-1$
            + " clusterFill_RGB              = " + clusterFill_RGB + NL //             //$NON-NLS-1$

      ;
   }

}
