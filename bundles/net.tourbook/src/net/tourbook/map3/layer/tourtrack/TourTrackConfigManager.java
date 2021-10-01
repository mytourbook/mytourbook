/*******************************************************************************
 * Copyright (C) 2005, 2020 Wolfgang Schramm and Contributors
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
package net.tourbook.map3.layer.tourtrack;

import gov.nasa.worldwind.WorldWind;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.color.Map3GradientColorManager;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.map3.Messages;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

public class TourTrackConfigManager {

   public static final int TRACK_POSITION_THRESHOLD_MIN = 3;
   public static final int TRACK_POSITION_THRESHOLD_MAX = 10;

   /*
    * Default id is used to reset a configuration to default values.
    */
   static final String         DEFAULT_ID_DEFAULT               = "#default";                                     //$NON-NLS-1$
   private static final String DEFAULT_ID_RELATIVE_BRIGHT       = "#relativeBright";                              //$NON-NLS-1$
   private static final String DEFAULT_ID_RELATIVE_DARK         = "#relativeDark";                                //$NON-NLS-1$
   private static final String DEFAULT_ID_CLOSE_BRIGHT          = "#closeBright";                                 //$NON-NLS-1$
   private static final String DEFAULT_ID_CLOSE_DARK            = "#closeDark";                                   //$NON-NLS-1$
   private static final String DEFAULT_ID_MIDDLE_BRIGHT         = "#middleBright";                                //$NON-NLS-1$
   private static final String DEFAULT_ID_MIDDLE_DARK           = "#middleDark";                                  //$NON-NLS-1$
   private static final String DEFAULT_ID_FAR_BRIGHT            = "#farBright";                                   //$NON-NLS-1$
   private static final String DEFAULT_ID_FAR_DARK              = "#farDark";                                     //$NON-NLS-1$
   private static final String DEFAULT_ID_EXTREM                = "#extrem";                                      //$NON-NLS-1$

   private static final String CONFIG_NAME_DEFAULT              = Messages.Track_Config_ConfigName_Default;
   private static final String CONFIG_NAME_RELATIVE_BRIGHT      = Messages.Track_Config_ConfigName_RelativeBright;
   private static final String CONFIG_NAME_RELATIVE_DARK        = Messages.Track_Config_ConfigName_RelativeDark;
   private static final String CONFIG_NAME_CLOSE_BRIGHT         = Messages.Track_Config_ConfigName_CloseBright;
   private static final String CONFIG_NAME_CLOSE_DARK           = Messages.Track_Config_ConfigName_CloseDark;
   private static final String CONFIG_NAME_MIDDLE_BRIGHT        = Messages.Track_Config_ConfigName_MiddleBright;
   private static final String CONFIG_NAME_MIDDLE_DARK          = Messages.Track_Config_ConfigName_MiddleDark;
   private static final String CONFIG_NAME_FAR_BRIGHT           = Messages.Track_Config_ConfigName_FarBright;
   private static final String CONFIG_NAME_FAR_DARK             = Messages.Track_Config_ConfigName_FarDark;
   private static final String CONFIG_NAME_EXTREM               = Messages.Track_Config_ConfigName_Extrem;

   static final String         CONFIG_NAME_UNKNOWN              = Messages.Track_Config_ConfigName_Unknown;

   public static final Boolean CONFIG_IS_FOLLOW_TERRAIN_DEFAULT = Boolean.FALSE;

   // direction arrows
   public static final Boolean IS_DIRECTION_ARROWS_VISIBLE_DEFAULT       = Boolean.TRUE;

   public static final int     DIRECTION_ARROW_SIZE_MIN                  = 10;
   public static final int     DIRECTION_ARROW_SIZE_MAX                  = 100;
   public static final float   DIRECTION_ARROW_SIZE_DEFAULT              = 40.0f;

   public static final int     DIRECTION_ARROW_VERTICAL_DISTANCE_MIN     = 1;
   public static final int     DIRECTION_ARROW_VERTICAL_DISTANCE_MAX     = 100;
   public static final float   DIRECTION_ARROW_VERTICAL_DISTANCE_DEFAULT = 2.0f;

   // outline
   public static final int   OUTLINE_WIDTH_MIN                   = 0;
   public static final int   OUTLINE_WIDTH_MAX                   = 10;
   public static final float OUTLINE_WIDTH_DEFAULT               = 1.0f;

   public static final int   OUTLINE_COLOR_MODE_NORMAL_DEFAULT   = TourTrackConfig.COLOR_MODE_TRACK_VALUE;
   public static final int   OUTLINE_COLOR_MODE_HOVERED_DEFAULT  = TourTrackConfig.COLOR_MODE_SOLID_COLOR;
   public static final int   OUTLINE_COLOR_MODE_SELECTED_DEFAULT = TourTrackConfig.COLOR_MODE_SOLID_COLOR;
   public static final int   OUTLINE_COLOR_MODE_HOV_SEL_DEFAULT  = TourTrackConfig.COLOR_MODE_TRACK_VALUE;

   public static final float OUTLINE_OPACITY_NORMAL_DEFAULT      = 1.0f;
   public static final float OUTLINE_OPACITY_HOVERED_DEFAULT     = 1.0f;
   public static final float OUTLINE_OPACITY_SELECTED_DEFAULT    = 1.0f;
   public static final float OUTLINE_OPACITY_HOV_SEL_DEFAULT     = 1.0f;

   // interior
   public static final boolean IS_INTERIOR_VISIBLE_DEFAULT          = true;
   public static final boolean IS_DRAW_VERTICALS_DEFAULT            = false;

   public static final int     INTERIOR_COLOR_MODE_NORMAL_DEFAULT   = TourTrackConfig.COLOR_MODE_SOLID_COLOR;
   public static final int     INTERIOR_COLOR_MODE_HOVERED_DEFAULT  = TourTrackConfig.COLOR_MODE_TRACK_VALUE;
   public static final int     INTERIOR_COLOR_MODE_SELECTED_DEFAULT = TourTrackConfig.COLOR_MODE_TRACK_VALUE;
   public static final int     INTERIOR_COLOR_MODE_HOV_SEL_DEFAULT  = TourTrackConfig.COLOR_MODE_TRACK_VALUE;

   public static final float   INTERIOR_OPACITY_NORMAL_DEFAULT      = 0;
   public static final float   INTERIOR_OPACITY_HOVERED_DEFAULT     = 0.2f;
   public static final float   INTERIOR_OPACITY_SELECTED_DEFAULT    = 0.2f;
   public static final float   INTERIOR_OPACITY_HOV_SEL_DEFAULT     = 0.2f;

   // track position
   public static final boolean IS_SHOW_TRACK_POSITION_DEFAULT       = true;

   /**
    * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    * <p>
    * Setting minimum position size to 0 will almost every time crash the whole app when the user
    * is changing from 0 to 1, could not figure out why, therefore the minimum is set to 1.
    * <p>
    * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    */
   public static final int     TRACK_POSITION_SIZE_MIN              = 1;
   public static final int     TRACK_POSITION_SIZE_MAX              = 60;

   public static final float   TRACK_POSITION_SIZE_NORMAL_DEFAULT   = 10;
   public static final float   TRACK_POSITION_SIZE_HOVERED_DEFAULT  = 12;
   public static final float   TRACK_POSITION_SIZE_SELECTED_DEFAULT = 10;
   public static final float   TRACK_POSITION_SIZE_HOV_SEL_DEFAULT  = 14;

   // this is an exponent
   public static final int TRACK_POSITION_THRESHOLD_DEFAULT = 10;

   // altitude
   public static final int     ALTITUDE_MODE_DEFAULT             = WorldWind.ABSOLUTE;
   public static final boolean IS_ALTITUDE_OFFSET_DEFAULT        = true;
   public static final boolean IS_ALTITUDE_OFFSET_RANDOM_DEFAULT = false;
   public static final int     ALTITUDE_OFFSET_MODE_ABSOLUTE     = 0;
   public static final int     ALTITUDE_OFFSET_MODE_RELATIVE     = 1;
   public static final int     ALTITUDE_OFFSET_MODE_DEFAULT      = ALTITUDE_OFFSET_MODE_ABSOLUTE;

   public static final int     ALTITUDE_OFFSET_ABSOLUTE_MIN      = -1000000;
   public static final int     ALTITUDE_OFFSET_ABSOLUTE_MAX      = 1000000;
   public static final int     ALTITUDE_OFFSET_ABSOLUTE_DEFAULT  = 50;
   public static final int     ALTITUDE_OFFSET_RELATIVE_MIN      = 0;
   public static final int     ALTITUDE_OFFSET_RELATIVE_MAX      = 50;
   public static final int     ALTITUDE_OFFSET_RELATIVE_DEFAULT  = 3;

   // colors
   public static final RGB RGB_BLACK;
   public static final RGB RGB_WHITE;
   public static final RGB RGB_NORMAL_DEFAULT;
   public static final RGB RGB_HOVERED_DEFAULT;
   public static final RGB RGB_HOV_SEL_DEFAULT;
   public static final RGB RGB_SELECTED_DEFAULT;

   static {

      RGB_BLACK = new RGB(0x0, 0x0, 0x0);
      RGB_WHITE = new RGB(0xff, 0xff, 0xff);

      RGB_NORMAL_DEFAULT = new RGB(0xFF, 0xFF, 0x0);
      RGB_HOVERED_DEFAULT = new RGB(0x0, 0xFF, 0xff);
      RGB_HOV_SEL_DEFAULT = new RGB(0xff, 0x0, 0xff);
      RGB_SELECTED_DEFAULT = new RGB(0xFF, 0xff, 0xff);
   }

   private static final String TOUR_TRACK_CONFIG_FILE_NAME = "tour-track-config.xml"; //$NON-NLS-1$

   /**
    * This version number is incremented, when structural changes (e.g. new category) are done.
    * When this happens, the <b>default</b> structure is created.
    */
   private static final int    TOUR_TRACK_CONFIG_VERSION   = 1;

   // root
   private static final String TAG_ROOT              = "TourTrackConfigurationRoot"; //$NON-NLS-1$
   private static final String ATTR_ACTIVE_CONFIG_ID = "activeConfigId";             //$NON-NLS-1$
   private static final String ATTR_CONFIG_VERSION   = "configVersion";              //$NON-NLS-1$

   // config
   private static final String TAG_TRACK_CONFIG                 = "trackConfig";       //$NON-NLS-1$
   private static final String ATTR_ID                          = "id";                //$NON-NLS-1$
   private static final String ATTR_DEFAULT_ID                  = "defaultId";         //$NON-NLS-1$
   private static final String ATTR_CONFIG_NAME                 = "name";              //$NON-NLS-1$
   private static final String ATTR_IS_FOLLOW_TERRAIN           = "isFollowTerrain";   //$NON-NLS-1$
   private static final String ATTR_TRACK_COLOR_OPACITY_DEFAULT = "trackColorOpacity"; //$NON-NLS-1$

   // direction arrows
   private static final String TAG_DIRECTION_ARROWS      = "directionArrows";  //$NON-NLS-1$
   private static final String ATTR_VERTICAL_DISTANCE    = "verticalDistance"; //$NON-NLS-1$
   private static final String ATTR_DIRECTION_ARROW_SIZE = "arrowSize";        //$NON-NLS-1$

   // outline
   private static final String TAG_OUTLINE        = "outline"; //$NON-NLS-1$
   private static final String ATTR_OUTLINE_WIDTH = "width";   //$NON-NLS-1$

   // track position
   private static final String TAG_TRACK_POSITION            = "trackPosition"; //$NON-NLS-1$
   private static final String TAG_TRACK_POSITION_SIZE       = "size";          //$NON-NLS-1$
   private static final String ATTR_TRACK_POSITION_THRESHOLD = "threshold";     //$NON-NLS-1$

   // interior
   private static final String TAG_INTERIOR           = "interior";        //$NON-NLS-1$
   private static final String ATTR_IS_DRAW_VERTICALS = "isDrawVerticals"; //$NON-NLS-1$

   // altitude
   private static final String TAG_ALTITUDE                   = "altitude";               //$NON-NLS-1$
   private static final String ATTR_ALTITUDE_MODE             = "altitudeMode";           //$NON-NLS-1$
   private static final String ATTR_IS_ABSOLUTE_OFFSET        = "isAbsoluteOffset";       //$NON-NLS-1$
   private static final String ATTR_IS_ABSOLUTE_OFFSET_RANDOM = "isAbsoluteOffsetRandom"; //$NON-NLS-1$
   private static final String ATTR_ALTITUDE_OFFSET_MODE      = "offsetMode";             //$NON-NLS-1$
   private static final String ATTR_ALTITUDE_OFFSET_ABSOLUTE  = "absoluteOffset";         //$NON-NLS-1$
   private static final String ATTR_ALTITUDE_OFFSET_RELATIVE  = "relativeOffset";         //$NON-NLS-1$

   // common attributes
   private static final String TAG_COLOR_MODE        = "colorMode";          //$NON-NLS-1$
   private static final String TAG_COLOR             = "color";              //$NON-NLS-1$
   private static final String TAG_OPACITY           = "opacity";            //$NON-NLS-1$
   private static final String ATTR_IS_VISIBLE       = "isVisible";          //$NON-NLS-1$
   private static final String TAG_COLOR_NORMAL      = "normal";             //$NON-NLS-1$
   private static final String TAG_COLOR_HOVERED     = "hovered";            //$NON-NLS-1$
   private static final String TAG_COLOR_SELECTED    = "selected";           //$NON-NLS-1$
   private static final String TAG_COLOR_HOV_AND_SEL = "hovsel";             //$NON-NLS-1$
   private static final String ATTR_NORMAL           = TAG_COLOR_NORMAL;
   private static final String ATTR_HOVERED          = TAG_COLOR_HOVERED;
   private static final String ATTR_SELECTED         = TAG_COLOR_SELECTED;
   private static final String ATTR_HOV_AND_SEL      = TAG_COLOR_HOV_AND_SEL;

   //
   private static final Bundle                     _bundle        = TourbookPlugin
         .getDefault()
         .getBundle();
   private static final IPath                      _stateLocation = Platform
         .getStateLocation(_bundle);

   /**
    * Contains all configurations which are loaded from a xml file.
    */
   private static final ArrayList<TourTrackConfig> _allConfigs    = new ArrayList<>();

   private static TourTrackConfig                  _activeConfig;
   private static String                           _activeConfigIdFromXml;

   private static XMLMemento create_Root() {

      final XMLMemento xmlRoot = XMLMemento.createWriteRoot(TAG_ROOT);

      // date/time
      xmlRoot.putString(Util.ATTR_ROOT_DATETIME, TimeTools.now().toString());

      // plugin version
      final Version version = _bundle.getVersion();
      xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MAJOR, version.getMajor());
      xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MINOR, version.getMinor());
      xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MICRO, version.getMicro());
      xmlRoot.putString(Util.ATTR_ROOT_VERSION_QUALIFIER, version.getQualifier());

      // config version
      xmlRoot.putInteger(ATTR_CONFIG_VERSION, TOUR_TRACK_CONFIG_VERSION);

      return xmlRoot;
   }

   private static void createAllDefaults() {

      _allConfigs.clear();

      // create default config
      final XMLMemento xmlRoot = createDefaultXml_0_All();

      // parse default config
      parse_000_All(_allConfigs, xmlRoot, true);
   }

   private static XMLMemento createDefaultXml_0_All() {

      XMLMemento xmlRoot;

      try {

         xmlRoot = create_Root();

         createDefaultXml_10(xmlRoot, CONFIG_NAME_DEFAULT, DEFAULT_ID_DEFAULT);

         createDefaultXml_10(xmlRoot, CONFIG_NAME_RELATIVE_BRIGHT, DEFAULT_ID_RELATIVE_BRIGHT);
         createDefaultXml_10(xmlRoot, CONFIG_NAME_RELATIVE_DARK, DEFAULT_ID_RELATIVE_DARK);

         createDefaultXml_10(xmlRoot, CONFIG_NAME_CLOSE_BRIGHT, DEFAULT_ID_CLOSE_BRIGHT);
         createDefaultXml_10(xmlRoot, CONFIG_NAME_CLOSE_DARK, DEFAULT_ID_CLOSE_DARK);

         createDefaultXml_10(xmlRoot, CONFIG_NAME_MIDDLE_BRIGHT, DEFAULT_ID_MIDDLE_BRIGHT);
         createDefaultXml_10(xmlRoot, CONFIG_NAME_MIDDLE_DARK, DEFAULT_ID_MIDDLE_DARK);

         createDefaultXml_10(xmlRoot, CONFIG_NAME_FAR_BRIGHT, DEFAULT_ID_FAR_BRIGHT);
         createDefaultXml_10(xmlRoot, CONFIG_NAME_FAR_DARK, DEFAULT_ID_FAR_DARK);

         createDefaultXml_10(xmlRoot, CONFIG_NAME_EXTREM, DEFAULT_ID_EXTREM);

         // append custom configurations
         for (int customIndex = 0; customIndex < 10; customIndex++) {

            createDefaultXml_10(//
                  xmlRoot,
                  String.format("%s #%d", CONFIG_NAME_DEFAULT, (customIndex + 1)), //$NON-NLS-1$
                  DEFAULT_ID_DEFAULT);
         }

      } catch (final Exception e) {
         throw new Error(e.getMessage());
      }

      return xmlRoot;
   }

   private static void createDefaultXml_10(final IMemento xmlRoot, final String configName, final String defaultId) {

      // <trackConfig>
      final IMemento xmlConfig = xmlRoot.createChild(TAG_TRACK_CONFIG);
      {
         xmlConfig.putString(ATTR_ID, Long.toString(System.nanoTime()));
         xmlConfig.putString(ATTR_DEFAULT_ID, defaultId);
         xmlConfig.putString(ATTR_CONFIG_NAME, configName);

         xmlConfig.putBoolean(ATTR_IS_FOLLOW_TERRAIN, CONFIG_IS_FOLLOW_TERRAIN_DEFAULT);
         xmlConfig.putFloat(ATTR_TRACK_COLOR_OPACITY_DEFAULT, Map3GradientColorManager.OPACITY_DEFAULT);

         // <directionArrows>
         final IMemento xmlDirectionArrows = xmlConfig.createChild(TAG_DIRECTION_ARROWS);
         {
            xmlDirectionArrows.putBoolean(ATTR_IS_VISIBLE, IS_DIRECTION_ARROWS_VISIBLE_DEFAULT);
            xmlDirectionArrows.putFloat(ATTR_DIRECTION_ARROW_SIZE, DIRECTION_ARROW_SIZE_DEFAULT);
            xmlDirectionArrows.putFloat(ATTR_VERTICAL_DISTANCE, DIRECTION_ARROW_VERTICAL_DISTANCE_DEFAULT);
         }

         // <outline>
         final IMemento xmlOutline = xmlConfig.createChild(TAG_OUTLINE);
         {
            xmlOutline.putFloat(ATTR_OUTLINE_WIDTH, OUTLINE_WIDTH_DEFAULT);

            // <colorMode>
            final IMemento xmlColorMode = xmlOutline.createChild(TAG_COLOR_MODE);
            {
               xmlColorMode.putInteger(ATTR_NORMAL, OUTLINE_COLOR_MODE_NORMAL_DEFAULT);
               xmlColorMode.putInteger(ATTR_HOVERED, OUTLINE_COLOR_MODE_HOVERED_DEFAULT);
               xmlColorMode.putInteger(ATTR_SELECTED, OUTLINE_COLOR_MODE_SELECTED_DEFAULT);
               xmlColorMode.putInteger(ATTR_HOV_AND_SEL, OUTLINE_COLOR_MODE_HOV_SEL_DEFAULT);
            }

            // <color>
            final IMemento xmlColor = xmlOutline.createChild(TAG_COLOR);
            {
               Util.setXmlRgb(xmlColor, TAG_COLOR_NORMAL, RGB_NORMAL_DEFAULT);
               Util.setXmlRgb(xmlColor, TAG_COLOR_HOVERED, RGB_HOVERED_DEFAULT);
               Util.setXmlRgb(xmlColor, TAG_COLOR_SELECTED, RGB_SELECTED_DEFAULT);
               Util.setXmlRgb(xmlColor, TAG_COLOR_HOV_AND_SEL, RGB_HOV_SEL_DEFAULT);
            }

            // <opacity>
            final IMemento xmlOpacity = xmlOutline.createChild(TAG_OPACITY);
            {
               xmlOpacity.putFloat(ATTR_NORMAL, OUTLINE_OPACITY_NORMAL_DEFAULT);
               xmlOpacity.putFloat(ATTR_HOVERED, OUTLINE_OPACITY_HOVERED_DEFAULT);
               xmlOpacity.putFloat(ATTR_SELECTED, OUTLINE_OPACITY_SELECTED_DEFAULT);
               xmlOpacity.putFloat(ATTR_HOV_AND_SEL, OUTLINE_OPACITY_HOV_SEL_DEFAULT);
            }
         }

         // <interior>
         final IMemento xmlInterior = xmlConfig.createChild(TAG_INTERIOR);
         {
            xmlInterior.putBoolean(ATTR_IS_VISIBLE, IS_INTERIOR_VISIBLE_DEFAULT);
            xmlInterior.putBoolean(ATTR_IS_DRAW_VERTICALS, IS_DRAW_VERTICALS_DEFAULT);

            // <colorMode>
            final IMemento xmlColorMode = xmlInterior.createChild(TAG_COLOR_MODE);
            {
               xmlColorMode.putInteger(ATTR_NORMAL, INTERIOR_COLOR_MODE_NORMAL_DEFAULT);
               xmlColorMode.putInteger(ATTR_HOVERED, INTERIOR_COLOR_MODE_HOVERED_DEFAULT);
               xmlColorMode.putInteger(ATTR_SELECTED, INTERIOR_COLOR_MODE_SELECTED_DEFAULT);
               xmlColorMode.putInteger(ATTR_HOV_AND_SEL, INTERIOR_COLOR_MODE_HOV_SEL_DEFAULT);
            }

            // <color>
            final IMemento xmlColor = xmlInterior.createChild(TAG_COLOR);
            {
               Util.setXmlRgb(xmlColor, TAG_COLOR_NORMAL, RGB_NORMAL_DEFAULT);
               Util.setXmlRgb(xmlColor, TAG_COLOR_HOVERED, RGB_HOVERED_DEFAULT);
               Util.setXmlRgb(xmlColor, TAG_COLOR_SELECTED, RGB_SELECTED_DEFAULT);
               Util.setXmlRgb(xmlColor, TAG_COLOR_HOV_AND_SEL, RGB_HOV_SEL_DEFAULT);
            }

            // <opacity>
            final IMemento xmlOpacity = xmlInterior.createChild(TAG_OPACITY);
            {
               xmlOpacity.putFloat(ATTR_NORMAL, INTERIOR_OPACITY_NORMAL_DEFAULT);
               xmlOpacity.putFloat(ATTR_HOVERED, INTERIOR_OPACITY_HOVERED_DEFAULT);
               xmlOpacity.putFloat(ATTR_SELECTED, INTERIOR_OPACITY_SELECTED_DEFAULT);
               xmlOpacity.putFloat(ATTR_HOV_AND_SEL, INTERIOR_OPACITY_HOV_SEL_DEFAULT);
            }
         }

         // <trackPosition>
         final IMemento xmlTrackPosition = xmlConfig.createChild(TAG_TRACK_POSITION);
         {
            xmlTrackPosition.putBoolean(ATTR_IS_VISIBLE, IS_SHOW_TRACK_POSITION_DEFAULT);
            xmlTrackPosition.putInteger(ATTR_TRACK_POSITION_THRESHOLD, TRACK_POSITION_THRESHOLD_DEFAULT);

            // <size>
            final IMemento xmlTrackSize = xmlTrackPosition.createChild(TAG_TRACK_POSITION_SIZE);
            {
               xmlTrackSize.putFloat(ATTR_NORMAL, TRACK_POSITION_SIZE_NORMAL_DEFAULT);
               xmlTrackSize.putFloat(ATTR_HOVERED, TRACK_POSITION_SIZE_HOVERED_DEFAULT);
               xmlTrackSize.putFloat(ATTR_SELECTED, TRACK_POSITION_SIZE_SELECTED_DEFAULT);
               xmlTrackSize.putFloat(ATTR_HOV_AND_SEL, TRACK_POSITION_SIZE_HOV_SEL_DEFAULT);
            }
         }

         // <altitude>
         final IMemento xmlAltitude = xmlConfig.createChild(TAG_ALTITUDE);
         {
            xmlAltitude.putInteger(ATTR_ALTITUDE_MODE, ALTITUDE_MODE_DEFAULT);
            xmlAltitude.putBoolean(ATTR_IS_ABSOLUTE_OFFSET, IS_ALTITUDE_OFFSET_DEFAULT);
            xmlAltitude.putBoolean(ATTR_IS_ABSOLUTE_OFFSET_RANDOM, IS_ALTITUDE_OFFSET_RANDOM_DEFAULT);
            xmlAltitude.putInteger(ATTR_ALTITUDE_OFFSET_MODE, ALTITUDE_OFFSET_MODE_DEFAULT);
            xmlAltitude.putInteger(ATTR_ALTITUDE_OFFSET_ABSOLUTE, ALTITUDE_OFFSET_ABSOLUTE_DEFAULT);
            xmlAltitude.putInteger(ATTR_ALTITUDE_OFFSET_RELATIVE, ALTITUDE_OFFSET_RELATIVE_DEFAULT);
         }
      }
   }

   private static void createXml_FromConfig(final IMemento xmlRoot, final TourTrackConfig config) {

      // <trackConfig>
      final IMemento xmlConfig = xmlRoot.createChild(TAG_TRACK_CONFIG);
      {
         xmlConfig.putString(ATTR_ID, config.id);
         xmlConfig.putString(ATTR_DEFAULT_ID, config.defaultId);
         xmlConfig.putString(ATTR_CONFIG_NAME, config.name);

         xmlConfig.putBoolean(ATTR_IS_FOLLOW_TERRAIN, config.isFollowTerrain);
         xmlConfig.putFloat(ATTR_TRACK_COLOR_OPACITY_DEFAULT, (float) config.trackColorOpacity);

         // <directionArrows>
         final IMemento xmlDirectionArrows = xmlConfig.createChild(TAG_DIRECTION_ARROWS);
         {
            xmlDirectionArrows.putBoolean(ATTR_IS_VISIBLE, config.isShowDirectionArrows);
            xmlDirectionArrows.putFloat(ATTR_DIRECTION_ARROW_SIZE, (float) config.directionArrowSize);
            xmlDirectionArrows.putFloat(ATTR_VERTICAL_DISTANCE, (float) config.directionArrowDistance);
         }

         // <outline>
         final IMemento xmlOutline = xmlConfig.createChild(TAG_OUTLINE);
         {
            xmlOutline.putFloat(ATTR_OUTLINE_WIDTH, (float) config.outlineWidth);

            // <colorMode>
            final IMemento xmlColorMode = xmlOutline.createChild(TAG_COLOR_MODE);
            {
               xmlColorMode.putInteger(ATTR_NORMAL, config.outlineColorMode);
               xmlColorMode.putInteger(ATTR_HOVERED, config.outlineColorMode_Hovered);
               xmlColorMode.putInteger(ATTR_SELECTED, config.outlineColorMode_Selected);
               xmlColorMode.putInteger(ATTR_HOV_AND_SEL, config.outlineColorMode_HovSel);
            }

            // <color>
            final IMemento xmlColor = xmlOutline.createChild(TAG_COLOR);
            {
               Util.setXmlRgb(xmlColor, ATTR_NORMAL, config.outlineColor);
               Util.setXmlRgb(xmlColor, ATTR_HOVERED, config.outlineColor_Hovered);
               Util.setXmlRgb(xmlColor, ATTR_SELECTED, config.outlineColor_Selected);
               Util.setXmlRgb(xmlColor, ATTR_HOV_AND_SEL, config.outlineColor_HovSel);
            }

            // <opacity>
            final IMemento xmlOpacity = xmlOutline.createChild(TAG_OPACITY);
            {
               xmlOpacity.putFloat(ATTR_NORMAL, (float) config.outlineOpacity);
               xmlOpacity.putFloat(ATTR_HOVERED, (float) config.outlineOpacity_Hovered);
               xmlOpacity.putFloat(ATTR_SELECTED, (float) config.outlineOpacity_Selected);
               xmlOpacity.putFloat(ATTR_HOV_AND_SEL, (float) config.outlineOpacity_HovSel);
            }
         }

         // <interior>
         final IMemento xmlInterior = xmlConfig.createChild(TAG_INTERIOR);
         {
            xmlInterior.putBoolean(ATTR_IS_VISIBLE, config.isShowInterior);
            xmlInterior.putBoolean(ATTR_IS_DRAW_VERTICALS, config.isDrawVerticals);

            // <colorMode>
            final IMemento xmlColorMode = xmlInterior.createChild(TAG_COLOR_MODE);
            {
               xmlColorMode.putInteger(ATTR_NORMAL, config.interiorColorMode);
               xmlColorMode.putInteger(ATTR_HOVERED, config.interiorColorMode_Hovered);
               xmlColorMode.putInteger(ATTR_SELECTED, config.interiorColorMode_Selected);
               xmlColorMode.putInteger(ATTR_HOV_AND_SEL, config.interiorColorMode_HovSel);
            }

            // <color>
            final IMemento xmlColor = xmlInterior.createChild(TAG_COLOR);
            {
               Util.setXmlRgb(xmlColor, ATTR_NORMAL, config.interiorColor);
               Util.setXmlRgb(xmlColor, ATTR_HOVERED, config.interiorColor_Hovered);
               Util.setXmlRgb(xmlColor, ATTR_SELECTED, config.interiorColor_Selected);
               Util.setXmlRgb(xmlColor, ATTR_HOV_AND_SEL, config.interiorColor_HovSel);
            }

            // <opacity>
            final IMemento xmlOpacity = xmlInterior.createChild(TAG_OPACITY);
            {
               xmlOpacity.putFloat(ATTR_NORMAL, (float) config.interiorOpacity);
               xmlOpacity.putFloat(ATTR_HOVERED, (float) config.interiorOpacity_Hovered);
               xmlOpacity.putFloat(ATTR_SELECTED, (float) config.interiorOpacity_Selected);
               xmlOpacity.putFloat(ATTR_HOV_AND_SEL, (float) config.interiorOpacity_HovSel);
            }
         }

         // <trackPosition>
         final IMemento xmlTrackPosition = xmlConfig.createChild(TAG_TRACK_POSITION);
         {
            xmlTrackPosition.putBoolean(ATTR_IS_VISIBLE, config.isShowTrackPosition);
            xmlTrackPosition.putInteger(ATTR_TRACK_POSITION_THRESHOLD, config.trackPositionThreshold);

            // <size>
            final IMemento xmlTrackSize = xmlTrackPosition.createChild(TAG_TRACK_POSITION_SIZE);
            {
               xmlTrackSize.putFloat(ATTR_NORMAL, (float) config.trackPositionSize);
               xmlTrackSize.putFloat(ATTR_HOVERED, (float) config.trackPositionSize_Hovered);
               xmlTrackSize.putFloat(ATTR_SELECTED, (float) config.trackPositionSize_Selected);
               xmlTrackSize.putFloat(ATTR_HOV_AND_SEL, (float) config.trackPositionSize_HovSel);
            }
         }

         // <altitude>
         final IMemento xmlAltitude = xmlConfig.createChild(TAG_ALTITUDE);
         {
            xmlAltitude.putInteger(ATTR_ALTITUDE_MODE, config.altitudeMode);
            xmlAltitude.putBoolean(ATTR_IS_ABSOLUTE_OFFSET, config.isAltitudeOffset);
            xmlAltitude.putBoolean(ATTR_IS_ABSOLUTE_OFFSET_RANDOM, config.isAltitudeOffsetRandom);
            xmlAltitude.putInteger(ATTR_ALTITUDE_OFFSET_MODE, config.altitudeOffsetMode);
            xmlAltitude.putInteger(ATTR_ALTITUDE_OFFSET_ABSOLUTE, config.altitudeOffsetDistanceAbsolute);
            xmlAltitude.putInteger(ATTR_ALTITUDE_OFFSET_RELATIVE, config.altitudeOffsetDistanceRelative);
         }
      }
   }

   public static TourTrackConfig getActiveConfig() {

      if (_activeConfig == null) {
         readConfigFromXml();
      }

      return _activeConfig;
   }

   /**
    * @return Returns the index of the active config within all configs.
    */
   public static int getActiveConfigIndex() {

      final TourTrackConfig activeConfig = getActiveConfig();

      for (int configIndex = 0; configIndex < _allConfigs.size(); configIndex++) {

         final TourTrackConfig config = _allConfigs.get(configIndex);

         if (config == activeConfig) {
            return configIndex;
         }
      }

      // this case should not happen but ensure that the correct config is set

      _activeConfig = _allConfigs.get(0);

      return 0;
   }

   private static int getActiveConfigIndexFromId() {

      for (int configIndex = 0; configIndex < _allConfigs.size(); configIndex++) {

         final TourTrackConfig config = _allConfigs.get(configIndex);

         if (config.id.equals(_activeConfig.id)) {
            return configIndex;
         }
      }

      return 0;
   }

   public static ArrayList<TourTrackConfig> getAllConfigurations() {

      // ensure configs are loaded
      getActiveConfig();

      return _allConfigs;
   }

   private static File getConfigXmlFile() {

      final File layerFile = _stateLocation.append(TOUR_TRACK_CONFIG_FILE_NAME).toFile();

      return layerFile;
   }

   private static TourTrackConfig getFirstDefaultConfig() {

      for (final TourTrackConfig config : _allConfigs) {

         if (config.defaultId.equals(DEFAULT_ID_DEFAULT)) {
            return config;
         }
      }

      return null;
   }

   /**
    * Overwrite default values according to the default id {@link TourTrackConfig#defaultId}.
    *
    * @param config
    */
   private static void overwriteConfig_DefaultValues(final TourTrackConfig config) {

      if (config.defaultId.equals(DEFAULT_ID_DEFAULT)) {

         // default id contains only default values -> nothing to do

      } else if (config.defaultId.equals(DEFAULT_ID_RELATIVE_BRIGHT)) {

         overwriteConfigValues_Bright(config);

         config.altitudeOffsetMode = TourTrackConfigManager.ALTITUDE_OFFSET_MODE_RELATIVE;

      } else if (config.defaultId.equals(DEFAULT_ID_RELATIVE_DARK)) {

         overwriteConfigValues_Dark(config);

         config.altitudeOffsetMode = TourTrackConfigManager.ALTITUDE_OFFSET_MODE_RELATIVE;

      } else if (config.defaultId.equals(DEFAULT_ID_CLOSE_BRIGHT)) {

         overwriteConfigValues_Bright(config);

      } else if (config.defaultId.equals(DEFAULT_ID_CLOSE_DARK)) {

         overwriteConfigValues_Dark(config);

      } else if (config.defaultId.equals(DEFAULT_ID_MIDDLE_BRIGHT)) {

         overwriteConfigValues_Bright(config);

         config.altitudeOffsetDistanceAbsolute = 500;

      } else if (config.defaultId.equals(DEFAULT_ID_MIDDLE_DARK)) {

         overwriteConfigValues_Dark(config);

         config.altitudeOffsetDistanceAbsolute = 500;

      } else if (config.defaultId.equals(DEFAULT_ID_FAR_BRIGHT)) {

         overwriteConfigValues_Bright(config);

         config.altitudeOffsetDistanceAbsolute = 5000;

      } else if (config.defaultId.equals(DEFAULT_ID_FAR_DARK)) {

         overwriteConfigValues_Dark(config);

         config.altitudeOffsetDistanceAbsolute = 5000;

      } else if (config.defaultId.equals(DEFAULT_ID_EXTREM)) {

         overwriteConfigValues_Dark(config);

         config.altitudeOffsetDistanceAbsolute = 500000;
      }
   }

   private static void overwriteConfig_DefaultValues_ForAll() {

      for (final TourTrackConfig config : _allConfigs) {
         overwriteConfig_DefaultValues(config);
      }
   }

   private static void overwriteConfigValues_Bright(final TourTrackConfig config) {

      config.trackColorOpacity = 0.5f;

      config.outlineColorMode = TourTrackConfig.COLOR_MODE_SOLID_COLOR;
      config.outlineColor = RGB_BLACK;
      config.trackPositionSize = 1;

      config.interiorColorMode = TourTrackConfig.COLOR_MODE_TRACK_VALUE;
   }

   private static void overwriteConfigValues_Dark(final TourTrackConfig config) {

      config.trackColorOpacity = 0.5f;

      config.outlineColorMode = TourTrackConfig.COLOR_MODE_SOLID_COLOR;
      config.outlineColor = RGB_WHITE;
      config.trackPositionSize = 1;

      config.interiorColorMode = TourTrackConfig.COLOR_MODE_TRACK_VALUE;
   }

   /**
    * Parse configuration xml.
    *
    * @param allConfigurations
    * @param xmlRoot
    * @param parseRootXml
    *           When <code>true</code> the value {@link #_activeConfigIdFromXml} is set from the
    *           root item.
    */
   private static void parse_000_All(final ArrayList<TourTrackConfig> allConfigurations,
                                     final XMLMemento xmlRoot,
                                     final boolean parseRootXml) {

      if (parseRootXml) {
         parse_010_Root(xmlRoot);
      }

      for (final IMemento mementoConfig : xmlRoot.getChildren()) {

         final XMLMemento xmlConfig = (XMLMemento) mementoConfig;

         try {

            final String xmlConfigType = xmlConfig.getType();

            if (xmlConfigType.equals(TAG_TRACK_CONFIG)) {

               // <trackConfig>

               final TourTrackConfig trackConfig = new TourTrackConfig();

               parse_100_ConfigAttr(xmlConfig, trackConfig);

               for (final IMemento mementoConfigChild : xmlConfig.getChildren()) {

                  final XMLMemento xmlConfigChild = (XMLMemento) mementoConfigChild;
                  final String configTag = xmlConfigChild.getType();

                  if (configTag.equals(TAG_DIRECTION_ARROWS)) {

                     parse_200_DirectionArrows(xmlConfigChild, trackConfig);

                  } else if (configTag.equals(TAG_OUTLINE)) {

                     parse_300_Outline(xmlConfigChild, trackConfig);

                  } else if (configTag.equals(TAG_INTERIOR)) {

                     parse_400_Interior(xmlConfigChild, trackConfig);

                  } else if (configTag.equals(TAG_TRACK_POSITION)) {

                     parse_500_TrackPosition(xmlConfigChild, trackConfig);

                  } else if (configTag.equals(TAG_ALTITUDE)) {

                     parse_600_Altitude(xmlConfigChild, trackConfig);
                  }
               }

               allConfigurations.add(trackConfig);
            }

         } catch (final Exception e) {
            StatusUtil.log(Util.dumpMemento(xmlConfig), e);
         }
      }
   }

   private static void parse_010_Root(final XMLMemento xmlRoot) {

      _activeConfigIdFromXml = Util.getXmlString(//
            xmlRoot,
            ATTR_ACTIVE_CONFIG_ID,
            null);
   }

   private static void parse_100_ConfigAttr(final XMLMemento xmlConfig, final TourTrackConfig config) {

      config.id = Util.getXmlString(xmlConfig, //
            ATTR_ID,
            Long.toString(System.nanoTime()));

      config.defaultId = Util.getXmlString(xmlConfig, //
            ATTR_DEFAULT_ID,
            DEFAULT_ID_DEFAULT);

      config.name = Util.getXmlString(xmlConfig, //
            ATTR_CONFIG_NAME,
            CONFIG_NAME_UNKNOWN);

      config.isFollowTerrain = Util.getXmlBoolean(xmlConfig, //
            ATTR_IS_FOLLOW_TERRAIN,
            CONFIG_IS_FOLLOW_TERRAIN_DEFAULT);

      config.trackColorOpacity = Util.getXmlFloatInt(
            xmlConfig,
            ATTR_TRACK_COLOR_OPACITY_DEFAULT,
            Map3GradientColorManager.OPACITY_DEFAULT,
            Map3GradientColorManager.OPACITY_MIN,
            Map3GradientColorManager.OPACITY_MAX);
   }

   private static void parse_200_DirectionArrows(final XMLMemento xmlDirectionArrow, final TourTrackConfig config) {

      config.isShowDirectionArrows = Util.getXmlBoolean(xmlDirectionArrow, //
            ATTR_IS_VISIBLE,
            IS_DIRECTION_ARROWS_VISIBLE_DEFAULT);

      config.directionArrowSize = Util.getXmlFloatInt(xmlDirectionArrow, //
            ATTR_DIRECTION_ARROW_SIZE,
            DIRECTION_ARROW_SIZE_DEFAULT,
            DIRECTION_ARROW_SIZE_MIN,
            DIRECTION_ARROW_SIZE_MAX);

      config.directionArrowDistance = Util.getXmlFloatInt(xmlDirectionArrow, //
            ATTR_VERTICAL_DISTANCE,
            DIRECTION_ARROW_VERTICAL_DISTANCE_DEFAULT,
            DIRECTION_ARROW_VERTICAL_DISTANCE_MIN,
            DIRECTION_ARROW_VERTICAL_DISTANCE_MAX);
   }

   private static void parse_300_Outline(final XMLMemento xmlOutline, final TourTrackConfig config) {

      config.outlineWidth = Util.getXmlFloatInt(
            xmlOutline,
            ATTR_OUTLINE_WIDTH,
            OUTLINE_WIDTH_DEFAULT,
            OUTLINE_WIDTH_MIN,
            OUTLINE_WIDTH_MAX);

      for (final IMemento mementoOutlineChild : xmlOutline.getChildren()) {

         final XMLMemento xmlOutlineChild = (XMLMemento) mementoOutlineChild;
         final String outlineChildTag = xmlOutlineChild.getType();

         if (outlineChildTag.equals(TAG_COLOR_MODE)) {

            parse_310__ColorMode(xmlOutlineChild, config);

         } else if (outlineChildTag.equals(TAG_COLOR)) {

            parse_320__Color(xmlOutlineChild, config);

         } else if (outlineChildTag.equals(TAG_OPACITY)) {

            parse_330__Opacity(xmlOutlineChild, config);
         }
      }
   }

   private static void parse_310__ColorMode(final XMLMemento xmlColorMode, final TourTrackConfig config) {

      config.outlineColorMode = Util.getXmlInteger(xmlColorMode, //
            ATTR_NORMAL,
            OUTLINE_COLOR_MODE_NORMAL_DEFAULT);

      config.outlineColorMode_Hovered = Util.getXmlInteger(xmlColorMode, //
            ATTR_HOVERED,
            OUTLINE_COLOR_MODE_HOVERED_DEFAULT);

      config.outlineColorMode_Selected = Util.getXmlInteger(xmlColorMode, //
            ATTR_SELECTED,
            OUTLINE_COLOR_MODE_SELECTED_DEFAULT);

      config.outlineColorMode_HovSel = Util.getXmlInteger(xmlColorMode, //
            ATTR_HOV_AND_SEL,
            OUTLINE_COLOR_MODE_HOV_SEL_DEFAULT);
   }

   private static void parse_320__Color(final XMLMemento xmlOutlineChild, final TourTrackConfig config) {

      for (final IMemento mementoColorChild : xmlOutlineChild.getChildren()) {

         final XMLMemento xmlColorChild = (XMLMemento) mementoColorChild;
         final String colorChildTag = xmlColorChild.getType();

         if (colorChildTag.equals(TAG_COLOR_NORMAL)) {

            config.outlineColor = Util.getXmlRgb(xmlColorChild, RGB_NORMAL_DEFAULT);

         } else if (colorChildTag.equals(TAG_COLOR_HOVERED)) {

            config.outlineColor_Hovered = Util.getXmlRgb(xmlColorChild, RGB_HOVERED_DEFAULT);

         } else if (colorChildTag.equals(TAG_COLOR_SELECTED)) {

            config.outlineColor_Selected = Util.getXmlRgb(xmlColorChild, RGB_SELECTED_DEFAULT);

         } else if (colorChildTag.equals(TAG_COLOR_HOV_AND_SEL)) {

            config.outlineColor_HovSel = Util.getXmlRgb(xmlColorChild, RGB_HOV_SEL_DEFAULT);
         }
      }
   }

   private static void parse_330__Opacity(final XMLMemento xmlOpacity, final TourTrackConfig trackConfig) {

      trackConfig.outlineOpacity = Util.getXmlFloatInt(xmlOpacity, //
            ATTR_NORMAL,
            OUTLINE_OPACITY_NORMAL_DEFAULT,
            Map3GradientColorManager.OPACITY_MIN,
            Map3GradientColorManager.OPACITY_MAX);

      trackConfig.outlineOpacity_Hovered = Util.getXmlFloatInt(xmlOpacity, //
            ATTR_HOVERED,
            OUTLINE_OPACITY_HOVERED_DEFAULT,
            Map3GradientColorManager.OPACITY_MIN,
            Map3GradientColorManager.OPACITY_MAX);

      trackConfig.outlineOpacity_Selected = Util.getXmlFloatInt(xmlOpacity, //
            ATTR_SELECTED,
            OUTLINE_OPACITY_SELECTED_DEFAULT,
            Map3GradientColorManager.OPACITY_MIN,
            Map3GradientColorManager.OPACITY_MAX);

      trackConfig.outlineOpacity_HovSel = Util.getXmlFloatInt(xmlOpacity, //
            ATTR_HOV_AND_SEL,
            OUTLINE_OPACITY_HOV_SEL_DEFAULT,
            Map3GradientColorManager.OPACITY_MIN,
            Map3GradientColorManager.OPACITY_MAX);
   }

   private static void parse_400_Interior(final XMLMemento xmlInterior, final TourTrackConfig trackConfig) {

      trackConfig.isShowInterior = Util.getXmlBoolean(//
            xmlInterior,
            ATTR_IS_VISIBLE,
            IS_INTERIOR_VISIBLE_DEFAULT);

      trackConfig.isDrawVerticals = Util.getXmlBoolean(//
            xmlInterior,
            ATTR_IS_DRAW_VERTICALS,
            IS_DRAW_VERTICALS_DEFAULT);

      for (final IMemento mementoInteriorChild : xmlInterior.getChildren()) {

         final XMLMemento xmlInteriorChild = (XMLMemento) mementoInteriorChild;
         final String InteriorTag = xmlInteriorChild.getType();

         if (InteriorTag.equals(TAG_COLOR_MODE)) {

            parse_410__ColorMode(xmlInteriorChild, trackConfig);

         } else if (InteriorTag.equals(TAG_COLOR)) {

            parse_420__Color(xmlInteriorChild, trackConfig);

         } else if (InteriorTag.equals(TAG_OPACITY)) {

            parse_430__Opacity(xmlInteriorChild, trackConfig);
         }
      }
   }

   private static void parse_410__ColorMode(final XMLMemento xmlColorMode, final TourTrackConfig config) {

      config.interiorColorMode = Util.getXmlInteger(xmlColorMode, //
            ATTR_NORMAL,
            INTERIOR_COLOR_MODE_NORMAL_DEFAULT);

      config.interiorColorMode_Hovered = Util.getXmlInteger(xmlColorMode, //
            ATTR_HOVERED,
            INTERIOR_COLOR_MODE_HOVERED_DEFAULT);

      config.interiorColorMode_Selected = Util.getXmlInteger(xmlColorMode, //
            ATTR_SELECTED,
            INTERIOR_COLOR_MODE_SELECTED_DEFAULT);

      config.interiorColorMode_HovSel = Util.getXmlInteger(xmlColorMode, //
            ATTR_HOV_AND_SEL,
            INTERIOR_COLOR_MODE_HOV_SEL_DEFAULT);
   }

   private static void parse_420__Color(final XMLMemento xmlInteriorChild, final TourTrackConfig config) {

      for (final IMemento mementoColorChild : xmlInteriorChild.getChildren()) {

         final XMLMemento xmlColorChild = (XMLMemento) mementoColorChild;
         final String colorChildTag = xmlColorChild.getType();

         if (colorChildTag.equals(TAG_COLOR_NORMAL)) {

            config.interiorColor = Util.getXmlRgb(xmlColorChild, RGB_NORMAL_DEFAULT);

         } else if (colorChildTag.equals(TAG_COLOR_HOVERED)) {

            config.interiorColor_Hovered = Util.getXmlRgb(xmlColorChild, RGB_HOVERED_DEFAULT);

         } else if (colorChildTag.equals(TAG_COLOR_SELECTED)) {

            config.interiorColor_Selected = Util.getXmlRgb(xmlColorChild, RGB_SELECTED_DEFAULT);

         } else if (colorChildTag.equals(TAG_COLOR_HOV_AND_SEL)) {

            config.interiorColor_HovSel = Util.getXmlRgb(xmlColorChild, RGB_HOV_SEL_DEFAULT);
         }
      }
   }

   private static void parse_430__Opacity(final XMLMemento xmlOpacity, final TourTrackConfig config) {

      config.interiorOpacity = Util.getXmlFloatInt(xmlOpacity, //
            ATTR_NORMAL,
            INTERIOR_OPACITY_NORMAL_DEFAULT,
            Map3GradientColorManager.OPACITY_MIN,
            Map3GradientColorManager.OPACITY_MAX);

      config.interiorOpacity_Hovered = Util.getXmlFloatInt(xmlOpacity, //
            ATTR_HOVERED,
            INTERIOR_OPACITY_HOVERED_DEFAULT,
            Map3GradientColorManager.OPACITY_MIN,
            Map3GradientColorManager.OPACITY_MAX);

      config.interiorOpacity_Selected = Util.getXmlFloatInt(xmlOpacity, //
            ATTR_SELECTED,
            INTERIOR_OPACITY_SELECTED_DEFAULT,
            Map3GradientColorManager.OPACITY_MIN,
            Map3GradientColorManager.OPACITY_MAX);

      config.interiorOpacity_HovSel = Util.getXmlFloatInt(xmlOpacity, //
            ATTR_HOV_AND_SEL,
            INTERIOR_OPACITY_HOV_SEL_DEFAULT,
            Map3GradientColorManager.OPACITY_MIN,
            Map3GradientColorManager.OPACITY_MAX);
   }

   private static void parse_500_TrackPosition(final XMLMemento xmlTrackPosition, final TourTrackConfig config) {

      config.isShowTrackPosition = Util.getXmlBoolean(xmlTrackPosition, //
            ATTR_IS_VISIBLE,
            IS_SHOW_TRACK_POSITION_DEFAULT);

      config.trackPositionThreshold = Util.getXmlInteger(xmlTrackPosition, //
            ATTR_TRACK_POSITION_THRESHOLD,
            TRACK_POSITION_THRESHOLD_DEFAULT,
            TRACK_POSITION_THRESHOLD_MIN,
            TRACK_POSITION_THRESHOLD_MAX);

      for (final IMemento mementoTrackSize : xmlTrackPosition.getChildren()) {

         final XMLMemento xmlTrackSize = (XMLMemento) mementoTrackSize;

         config.trackPositionSize = Util.getXmlFloatInt(xmlTrackSize, //
               ATTR_NORMAL,
               TRACK_POSITION_SIZE_NORMAL_DEFAULT,
               TRACK_POSITION_SIZE_MIN,
               TRACK_POSITION_SIZE_MAX);

         config.trackPositionSize_Hovered = Util.getXmlFloatInt(xmlTrackSize, //
               ATTR_HOVERED,
               TRACK_POSITION_SIZE_HOVERED_DEFAULT,
               TRACK_POSITION_SIZE_MIN,
               TRACK_POSITION_SIZE_MAX);

         config.trackPositionSize_Selected = Util.getXmlFloatInt(xmlTrackSize, //
               ATTR_SELECTED,
               TRACK_POSITION_SIZE_SELECTED_DEFAULT,
               TRACK_POSITION_SIZE_MIN,
               TRACK_POSITION_SIZE_MAX);

         config.trackPositionSize_HovSel = Util.getXmlFloatInt(xmlTrackSize, //
               ATTR_HOV_AND_SEL,
               TRACK_POSITION_SIZE_HOV_SEL_DEFAULT,
               TRACK_POSITION_SIZE_MIN,
               TRACK_POSITION_SIZE_MAX);

      }
   }

   private static void parse_600_Altitude(final XMLMemento xmlAltitude, final TourTrackConfig config) {

      final int xmlAltitudeMode = Util.getXmlInteger(xmlAltitude, //
            ATTR_ALTITUDE_MODE,
            ALTITUDE_MODE_DEFAULT);

      config.altitudeMode = TourTrackConfig.getValidAltitudeModeValue(xmlAltitudeMode);

      config.isAltitudeOffset = Util.getXmlBoolean(xmlAltitude, //
            ATTR_IS_ABSOLUTE_OFFSET,
            IS_ALTITUDE_OFFSET_DEFAULT);

      config.isAltitudeOffsetRandom = Util.getXmlBoolean(xmlAltitude, //
            ATTR_IS_ABSOLUTE_OFFSET_RANDOM,
            IS_ALTITUDE_OFFSET_RANDOM_DEFAULT);

      config.altitudeOffsetMode = Util.getXmlInteger(xmlAltitude, //
            ATTR_ALTITUDE_OFFSET_MODE,
            ALTITUDE_OFFSET_MODE_DEFAULT);

      config.altitudeOffsetDistanceAbsolute = Util.getXmlInteger(xmlAltitude, //
            ATTR_ALTITUDE_OFFSET_ABSOLUTE,
            ALTITUDE_OFFSET_ABSOLUTE_DEFAULT,
            ALTITUDE_OFFSET_ABSOLUTE_MIN,
            ALTITUDE_OFFSET_ABSOLUTE_MAX);

      config.altitudeOffsetDistanceRelative = Util.getXmlInteger(xmlAltitude, //
            ATTR_ALTITUDE_OFFSET_RELATIVE,
            ALTITUDE_OFFSET_RELATIVE_DEFAULT,
            ALTITUDE_OFFSET_RELATIVE_MIN,
            ALTITUDE_OFFSET_RELATIVE_MAX);
   }

   /**
    * Read/create tour track configuration a xml file
    *
    * @return
    */
   private static void readConfigFromXml() {

      try {

         XMLMemento xmlRoot = null;

         // try to get layer structure from saved xml file
         final File layerFile = getConfigXmlFile();
         final String absoluteLayerPath = layerFile.getAbsolutePath();

         final File inputFile = new File(absoluteLayerPath);
         if (inputFile.exists()) {

            try (FileInputStream inputStream = new FileInputStream(inputFile);
                  InputStreamReader reader = new InputStreamReader(inputStream, UI.UTF_8)) {

               xmlRoot = XMLMemento.createReadRoot(reader);

            } catch (final Exception e) {
               // ignore
            }
         }

         Integer configVersion = null;

         // get current layer version, when available
         if (xmlRoot != null) {
            configVersion = xmlRoot.getInteger(ATTR_CONFIG_VERSION);
         }

         boolean isSetConfigDefaultValues = false;

         if (xmlRoot == null || configVersion == null || configVersion < TOUR_TRACK_CONFIG_VERSION) {

            // create default config
            xmlRoot = createDefaultXml_0_All();
            isSetConfigDefaultValues = true;
         }

         // parse xml
         parse_000_All(_allConfigs, xmlRoot, true);

         // ensure a config is created
         if (_allConfigs.isEmpty()) {
            createAllDefaults();
            isSetConfigDefaultValues = true;
         }

         if (isSetConfigDefaultValues) {

            // overwrite config default values

            overwriteConfig_DefaultValues_ForAll();
         }

         _activeConfig = readConfigFromXml_GetActive();

      } catch (final Exception e) {
         StatusUtil.log(e);
      }
   }

   private static TourTrackConfig readConfigFromXml_GetActive() {

      TourTrackConfig activeConfig = null;

      if (_activeConfigIdFromXml == null) {

         // get first default config

         activeConfig = getFirstDefaultConfig();

      } else {

         // ensure config id belongs to a config which is available

         for (final TourTrackConfig config : _allConfigs) {

            if (config.id.equals(_activeConfigIdFromXml)) {

               activeConfig = config;
               break;
            }
         }
      }

      if (activeConfig == null) {

         // this case should not happen, create a clean default config

         StatusUtil.logInfo("Created default config for tour track properties.");//$NON-NLS-1$

         createAllDefaults();
         overwriteConfig_DefaultValues_ForAll();

         activeConfig = getFirstDefaultConfig();
      }

      return activeConfig;
   }

   /**
    * Set default values for the active config.
    */
   public static void resetActiveConfig() {

      final String backupConfigName = _activeConfig.name;

      // create xml with default values for the active config
      final XMLMemento xmlRoot = resetConfig(_activeConfig.defaultId);

      // parse xml
      final ArrayList<TourTrackConfig> newConfigs = new ArrayList<>();
      parse_000_All(newConfigs, xmlRoot, false);

      final TourTrackConfig newConfig = newConfigs.get(0);

      overwriteConfig_DefaultValues(newConfig);

      newConfig.name = backupConfigName;
      newConfig.checkTrackRecreation(_activeConfig);

      // replace config
      _allConfigs.set(getActiveConfigIndexFromId(), newConfig);

      _activeConfig = newConfig;
   }

   public static void resetAllConfigurations() {

      createAllDefaults();
      overwriteConfig_DefaultValues_ForAll();

      _activeConfig = _allConfigs.get(0);
   }

   private static XMLMemento resetConfig(final String defaultId) throws Error {

      XMLMemento xmlRoot;

      try {

         xmlRoot = create_Root();

         if (defaultId.equals(DEFAULT_ID_CLOSE_BRIGHT)) {
            createDefaultXml_10(xmlRoot, CONFIG_NAME_CLOSE_BRIGHT, DEFAULT_ID_CLOSE_BRIGHT);
         } else if (defaultId.equals(DEFAULT_ID_CLOSE_DARK)) {
            createDefaultXml_10(xmlRoot, CONFIG_NAME_CLOSE_DARK, DEFAULT_ID_CLOSE_DARK);

         } else

         // between

         if (defaultId.equals(DEFAULT_ID_MIDDLE_BRIGHT)) {
            createDefaultXml_10(xmlRoot, CONFIG_NAME_MIDDLE_BRIGHT, DEFAULT_ID_MIDDLE_BRIGHT);
         } else if (defaultId.equals(DEFAULT_ID_MIDDLE_DARK)) {
            createDefaultXml_10(xmlRoot, CONFIG_NAME_MIDDLE_DARK, DEFAULT_ID_MIDDLE_DARK);
         } else

         // far

         if (defaultId.equals(DEFAULT_ID_FAR_BRIGHT)) {
            createDefaultXml_10(xmlRoot, CONFIG_NAME_FAR_BRIGHT, DEFAULT_ID_FAR_BRIGHT);
         } else if (defaultId.equals(DEFAULT_ID_FAR_DARK)) {
            createDefaultXml_10(xmlRoot, CONFIG_NAME_FAR_DARK, DEFAULT_ID_FAR_DARK);
         } else

         // relative

         if (defaultId.equals(DEFAULT_ID_RELATIVE_BRIGHT)) {
            createDefaultXml_10(xmlRoot, CONFIG_NAME_RELATIVE_BRIGHT, DEFAULT_ID_RELATIVE_BRIGHT);
         } else if (defaultId.equals(DEFAULT_ID_RELATIVE_DARK)) {
            createDefaultXml_10(xmlRoot, CONFIG_NAME_RELATIVE_DARK, DEFAULT_ID_RELATIVE_DARK);

         } else if (defaultId.equals(DEFAULT_ID_EXTREM)) {
            createDefaultXml_10(xmlRoot, CONFIG_NAME_EXTREM, DEFAULT_ID_EXTREM);

         } else {

            // default default

            createDefaultXml_10(xmlRoot, CONFIG_NAME_DEFAULT, DEFAULT_ID_DEFAULT);
         }

      } catch (final Exception e) {
         throw new Error(e.getMessage());
      }
      return xmlRoot;
   }

   public static void saveState() {

      if (_activeConfig == null) {

         // this can happen when not yet used

         return;
      }

      final XMLMemento xmlRoot = create_Root();

      xmlRoot.putString(ATTR_ACTIVE_CONFIG_ID, _activeConfig.id);

      for (final TourTrackConfig config : _allConfigs) {
         createXml_FromConfig(xmlRoot, config);
      }

      Util.writeXml(xmlRoot, getConfigXmlFile());
   }

   public static void setActiveConfig(final TourTrackConfig newConfig) {
      _activeConfig = newConfig;
   }
}
