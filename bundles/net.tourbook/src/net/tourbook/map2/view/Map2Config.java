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

import net.tourbook.common.UI;
import net.tourbook.tour.filter.TourFilterFieldOperator;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;

/**
 * Contains data which are needed to paint a marker into the 2D map
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
    * Tour/common locations
    */
   public boolean isShowTourLocation                = true;
   public boolean isShowCommonLocation              = true;
   public boolean isShowLocationBoundingBox;

   public RGB     commonLocationFill_RGB            = Map2ConfigManager.DEFAULT_COMMON_LOCATION_FILL_RGB;
   public RGB     commonLocationFill_Hovered_RGB    = Map2ConfigManager.DEFAULT_COMMON_LOCATION_FILL_HOVERED_RGB;
   public RGB     commonLocationOutline_RGB         = Map2ConfigManager.DEFAULT_COMMON_LOCATION_OUTLINE_RGB;
   public RGB     commonLocationOutline_Hovered_RGB = Map2ConfigManager.DEFAULT_COMMON_LOCATION_OUTLINE_HOVERED_RGB;

   public RGB     tourLocationFill_RGB              = Map2ConfigManager.DEFAULT_TOUR_LOCATION_FILL_RGB;
   public RGB     tourLocationFill_Hovered_RGB      = Map2ConfigManager.DEFAULT_TOUR_LOCATION_FILL_HOVERED_RGB;
   public RGB     tourLocationOutline_RGB           = Map2ConfigManager.DEFAULT_TOUR_LOCATION_OUTLINE_RGB;
   public RGB     tourLocationOutline_Hovered_RGB   = Map2ConfigManager.DEFAULT_TOUR_LOCATION_OUTLINE_HOVERED_RGB;

   public Color   commonLocationFill_Color;
   public Color   commonLocationFill_Hovered_Color;
   public Color   commonLocationOutline_Color;
   public Color   commonLocationOutline_Hovered_Color;

   public Color   tourLocationFill_Color;
   public Color   tourLocationFill_Hovered_Color;
   public Color   tourLocationOutline_Color;
   public Color   tourLocationOutline_Hovered_Color;

   /*
    * Tour markers
    */
   public boolean isShowTourMarker              = true;

   /** When <code>true</code> then markers with the same label are grouped together */
   public boolean isGroupDuplicatedMarkers;
   public String  groupedMarkers                = UI.EMPTY_STRING;
   public int     groupGridSize                 = Map2ConfigManager.LABEL_GROUP_GRID_SIZE_DEFAULT;

   public RGB     tourMarkerFill_RGB            = Map2ConfigManager.DEFAULT_TOUR_MARKER_FILL_RGB;
   public RGB     tourMarkerFill_Hovered_RGB    = Map2ConfigManager.DEFAULT_TOUR_MARKER_FILL_HOVERED_RGB;
   public RGB     tourMarkerOutline_RGB         = Map2ConfigManager.DEFAULT_TOUR_MARKER_OUTLINE_RGB;
   public RGB     tourMarkerOutline_Hovered_RGB = Map2ConfigManager.DEFAULT_TOUR_MARKER_OUTLINE_HOVERED_RGB;

   public Color   tourMarkerFill_Color;
   public Color   tourMarkerFill_Hovered_Color;
   public Color   tourMarkerOutline_Color;
   public Color   tourMarkerOutline_Hovered_Color;

   /*
    * Labels
    */
   public boolean        isLabelAntialiased        = true;
   public boolean        isTruncateLabel;
   public boolean        isWrapLabel;
   public int            labelDistributorMaxLabels = Map2ConfigManager.LABEL_DISTRIBUTOR_MAX_LABELS_DEFAULT;
   public int            labelDistributorRadius    = Map2ConfigManager.LABEL_DISTRIBUTOR_RADIUS_DEFAULT;
   public int            labelTruncateLength       = Map2ConfigManager.LABEL_TRUNCATE_LENGTH_DEFAULT;
   public int            labelWrapLength           = Map2ConfigManager.LABEL_WRAP_LENGTH_DEFAULT;
   public MapLabelLayout labelLayout               = Map2ConfigManager.LABEL_LAYOUT_DEFAULT;

   /*
    * Tour marker cluster
    */
   public boolean isTourMarkerClustered      = true;
   public boolean isClusterSymbolAntialiased = true;
   public boolean isClusterTextAntialiased   = true;
   public boolean isShowClusterMarker        = true;

   public boolean isFillClusterSymbol        = Map2ConfigManager.DEFAULT_IS_FILL_CLUSTER_SYMBOL;
   public int     clusterGridSize            = Map2ConfigManager.DEFAULT_CLUSTER_GRID_SIZE;
   public RGB     clusterFill_RGB            = Map2ConfigManager.DEFAULT_CLUSTER_FILL_RGB;
   public RGB     clusterOutline_RGB         = Map2ConfigManager.DEFAULT_CLUSTER_OUTLINE_RGB;
   public int     clusterOutline_Width       = Map2ConfigManager.DEFAULT_CLUSTER_OUTLINE_WIDTH;
   public int     clusterSymbol_Size         = Map2ConfigManager.DEFAULT_CLUSTER_SYMBOL_SIZE;

   public Color   clusterFill_Color;
   public Color   clusterOutline_Color;

   /*
    * Tour pauses
    */
   public boolean isShowTourPauses;

   public RGB     tourPauseFill_RGB            = Map2ConfigManager.DEFAULT_TOUR_PAUSE_FILL_RGB;
   public RGB     tourPauseFill_Hovered_RGB    = Map2ConfigManager.DEFAULT_TOUR_PAUSE_FILL_HOVERED_RGB;
   public RGB     tourPauseOutline_RGB         = Map2ConfigManager.DEFAULT_TOUR_PAUSE_OUTLINE_RGB;
   public RGB     tourPauseOutline_Hovered_RGB = Map2ConfigManager.DEFAULT_TOUR_PAUSE_OUTLINE_HOVERED_RGB;

   public Color   tourPauseFill_Color;
   public Color   tourPauseFill_Hovered_Color;
   public Color   tourPauseOutline_Color;
   public Color   tourPauseOutline_Hovered_Color;

   /*
    * Tour pause filter
    */
   public boolean                 isFilterTourPauses;
   public boolean                 isFilterTourPause_Duration;
   public boolean                 isShowAutoPauses;
   public boolean                 isShowUserPauses;

   public boolean                 useTourPause_DurationFilter_Hours;
   public boolean                 useTourPause_DurationFilter_Minutes;
   public boolean                 useTourPause_DurationFilter_Seconds;

   public int                     tourPauseDurationFilter_Hours;
   public int                     tourPauseDurationFilter_Minutes;
   public int                     tourPauseDurationFilter_Seconds;
   public long                    tourPauseDuration;

   public TourFilterFieldOperator tourPauseDurationFilter_Operator;

   public void setupColors() {

// SET_FORMATTING_OFF

      tourMarkerFill_Color                = new Color(tourMarkerFill_RGB);
      tourMarkerFill_Hovered_Color        = new Color(tourMarkerFill_Hovered_RGB);
      tourMarkerOutline_Color             = new Color(tourMarkerOutline_RGB);
      tourMarkerOutline_Hovered_Color     = new Color(tourMarkerOutline_Hovered_RGB);

      clusterFill_Color                   = new Color(clusterFill_RGB);
      clusterOutline_Color                = new Color(clusterOutline_RGB);

      commonLocationFill_Color            = new Color(commonLocationFill_RGB);
      commonLocationFill_Hovered_Color    = new Color(commonLocationFill_Hovered_RGB);
      commonLocationOutline_Color         = new Color(commonLocationOutline_RGB);
      commonLocationOutline_Hovered_Color = new Color(commonLocationOutline_Hovered_RGB);

      tourLocationFill_Color              = new Color(tourLocationFill_RGB);
      tourLocationFill_Hovered_Color      = new Color(tourLocationFill_Hovered_RGB);
      tourLocationOutline_Color           = new Color(tourLocationOutline_RGB);
      tourLocationOutline_Hovered_Color   = new Color(tourLocationOutline_Hovered_RGB);

      tourPauseFill_Color                 = new Color(tourPauseFill_RGB);
      tourPauseFill_Hovered_Color         = new Color(tourPauseFill_Hovered_RGB);
      tourPauseOutline_Color              = new Color(tourPauseOutline_RGB);
      tourPauseOutline_Hovered_Color      = new Color(tourPauseOutline_Hovered_RGB);

// SET_FORMATTING_ON
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "Map2MarkerConfig" + NL //                                            //$NON-NLS-1$

            + " name                      = " + name + NL //                        //$NON-NLS-1$
            + " defaultId                 = " + defaultId + NL //                   //$NON-NLS-1$
            + " id                        = " + id + NL //                          //$NON-NLS-1$

            + " isShowTourMarker          = " + isShowTourMarker + NL //            //$NON-NLS-1$
            + " markerOutline_RGB         = " + tourMarkerOutline_RGB + NL //           //$NON-NLS-1$
            + " markerFill_RGB            = " + tourMarkerFill_RGB + NL //              //$NON-NLS-1$

            + " isMarkerClustered            = " + isTourMarkerClustered + NL //           //$NON-NLS-1$
            + " isClusterSymbolAntialiased   = " + isClusterSymbolAntialiased + NL //  //$NON-NLS-1$
            + " isClusterTextAntialiased     = " + isClusterTextAntialiased + NL //    //$NON-NLS-1$
            + " isFillClusterSymbol          = " + isFillClusterSymbol + NL //         //$NON-NLS-1$
            + " clusterGridSize              = " + clusterGridSize + NL //             //$NON-NLS-1$
            + " clusterSymbol_Size           = " + clusterSymbol_Size + NL //          //$NON-NLS-1$
            + " clusterOutline_Width         = " + clusterOutline_Width + NL //        //$NON-NLS-1$
            + " clusterOutline_RGB           = " + clusterOutline_RGB + NL //          //$NON-NLS-1$
            + " clusterFill_RGB              = " + clusterFill_RGB + NL //             //$NON-NLS-1$

      ;
   }

}
