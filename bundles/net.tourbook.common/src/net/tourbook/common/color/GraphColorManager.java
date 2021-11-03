/******************************************************************************
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
package net.tourbook.common.color;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.tourbook.common.CommonActivator;
import net.tourbook.common.Messages;
import net.tourbook.common.UI;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Manage colors which are displayed in a chart, map or other locations.
 */
public class GraphColorManager {

   public static final String  PREF_GRAPH_ALTIMETER                    = "altimeter";                  //$NON-NLS-1$
   public static final String  PREF_GRAPH_ALTITUDE                     = "altitude";                   //$NON-NLS-1$
   public static final String  PREF_GRAPH_CADENCE                      = "cadence";                    //$NON-NLS-1$
   public static final String  PREF_GRAPH_GEAR                         = "gear";                       //$NON-NLS-1$
   public static final String  PREF_GRAPH_DISTANCE                     = "distance";                   //$NON-NLS-1$
   public static final String  PREF_GRAPH_HEARTBEAT                    = "heartbeat";                  //$NON-NLS-1$
   public static final String  PREF_GRAPH_HISTORY                      = "History";                    //$NON-NLS-1$
   public static final String  PREF_GRAPH_GRADIENT                     = "gradient";                   //$NON-NLS-1$
   public static final String  PREF_GRAPH_PACE                         = "pace";                       //$NON-NLS-1$
   public static final String  PREF_GRAPH_POWER                        = "power";                      //$NON-NLS-1$
   public static final String  PREF_GRAPH_SENSOR                       = "Sensor";                     //$NON-NLS-1$
   public static final String  PREF_GRAPH_SPEED                        = "speed";                      //$NON-NLS-1$
   public static final String  PREF_GRAPH_TEMPTERATURE                 = "tempterature";               //$NON-NLS-1$
   public static final String  PREF_GRAPH_TIME                         = "duration";                   //$NON-NLS-1$
   public static final String  PREF_GRAPH_TOUR                         = "tour";                       //$NON-NLS-1$
   public static final String  PREF_GRAPH_TOUR_COMPARE                 = "tourCompare";                //$NON-NLS-1$
   public static final String  PREF_GRAPH_RUN_DYN_STANCE_TIME          = "RunDyn_StanceTime";          //$NON-NLS-1$
   public static final String  PREF_GRAPH_RUN_DYN_STANCE_TIME_BALANCED = "RunDyn_StanceTime_Balanced"; //$NON-NLS-1$
   public static final String  PREF_GRAPH_RUN_DYN_STEP_LENGTH          = "RunDyn_StepLength";          //$NON-NLS-1$
   public static final String  PREF_GRAPH_RUN_DYN_VERTICAL_OSCILLATION = "RunDyn_VerticalOscillation"; //$NON-NLS-1$
   public static final String  PREF_GRAPH_RUN_DYN_VERTICAL_RATIO       = "RunDyn_VerticalRatio";       //$NON-NLS-1$
   public static final String  PREF_GRAPH_SWIM_STROKES                 = "Swim_Strokes";               //$NON-NLS-1$
   public static final String  PREF_GRAPH_SWIM_SWOLF                   = "Swim_Swolf";                 //$NON-NLS-1$
   public static final String  PREF_GRAPH_TRAINING_EFFECT_AEROB        = "Training_Effect_Aerob";      //$NON-NLS-1$
   public static final String  PREF_GRAPH_TRAINING_EFFECT_ANAEROB      = "Training_Effect_Anaerob";    //$NON-NLS-1$
   public static final String  PREF_GRAPH_TRAINING_PERFORMANCE         = "Training_Performance";       //$NON-NLS-1$
   public static final String  PREF_GRAPH_BODYFAT                      = "BodyFat";                    //$NON-NLS-1$
   public static final String  PREF_GRAPH_BODYWEIGHT                   = "BodyWeight";                 //$NON-NLS-1$

   public static final String  PREF_COLOR_GRADIENT_BRIGHT              = "bright";                     //$NON-NLS-1$
   public static final String  PREF_COLOR_GRADIENT_DARK                = "dark";                       //$NON-NLS-1$
   public static final String  PREF_COLOR_LINE_LIGHT                   = "line";                       //$NON-NLS-1$
   public static final String  PREF_COLOR_LINE_DARK                    = "line-dark";                  //$NON-NLS-1$
   public static final String  PREF_COLOR_TEXT_LIGHT                   = "text";                       //$NON-NLS-1$
   public static final String  PREF_COLOR_TEXT_DARK                    = "text-dark";                  //$NON-NLS-1$
   public static final String  PREF_COLOR_MAPPING                      = "mapping";                    //$NON-NLS-1$

   private static final String MEMENTO_LEGEND_COLOR_FILE               = "legendcolor.xml";            //$NON-NLS-1$
   private static final String MEMENTO_ROOT                            = "legendcolorlist";            //$NON-NLS-1$

   private static final String MEMENTO_CHILD_LEGEND_COLOR              = "legendcolor";                //$NON-NLS-1$
   private static final String TAG_LEGEND_COLOR_PREF_NAME              = "prefname";                   //$NON-NLS-1$

   private static final String MEMENTO_CHILD_VALUE_COLOR               = "valuecolor";                 //$NON-NLS-1$
   private static final String TAG_VALUE_COLOR_VALUE                   = "value";                      //$NON-NLS-1$
   private static final String TAG_VALUE_COLOR_RED                     = "red";                        //$NON-NLS-1$
   private static final String TAG_VALUE_COLOR_GREEN                   = "green";                      //$NON-NLS-1$
   private static final String TAG_VALUE_COLOR_BLUE                    = "blue";                       //$NON-NLS-1$

   static final String         MEMENTO_CHILD_BRIGHTNESS                = "brightness";                 //$NON-NLS-1$
   static final String         TAG_BRIGHTNESS_MIN                      = "min";                        //$NON-NLS-1$
   static final String         TAG_BRIGHTNESS_MIN_FACTOR               = "minFactor";                  //$NON-NLS-1$
   static final String         TAG_BRIGHTNESS_MAX                      = "max";                        //$NON-NLS-1$
   static final String         TAG_BRIGHTNESS_MAX_FACTOR               = "maxFactor";                  //$NON-NLS-1$

   static final String         MEMENTO_CHILD_MIN_MAX_VALUE             = "minmaxValue";                //$NON-NLS-1$
   static final String         TAG_IS_MIN_VALUE_OVERWRITE              = "isMinOverwrite";             //$NON-NLS-1$
   static final String         TAG_MIN_VALUE_OVERWRITE                 = "minValueOverwrite";          //$NON-NLS-1$
   static final String         TAG_IS_MAX_VALUE_OVERWRITE              = "isMaxOverwrite";             //$NON-NLS-1$
   static final String         TAG_MAX_VALUE_OVERWRITE                 = "maxValueOverwrite";          //$NON-NLS-1$

// SET_FORMATTING_OFF

   public static String[][]              colorNames                              = new String[][] {

         { PREF_COLOR_GRADIENT_BRIGHT,    Messages.Graph_Pref_color_gradient_bright },
         { PREF_COLOR_GRADIENT_DARK,      Messages.Graph_Pref_color_gradient_dark   },

         { PREF_COLOR_LINE_LIGHT,         Messages.Graph_Pref_ColorLine_Theme_Light },
         { PREF_COLOR_TEXT_LIGHT,         Messages.Graph_Pref_ColorText_Theme_Light },

         { PREF_COLOR_LINE_DARK,          Messages.Graph_Pref_ColorLine_Theme_Dark  },
         { PREF_COLOR_TEXT_DARK,          Messages.Graph_Pref_ColorText_Theme_Dark  },

         { PREF_COLOR_MAPPING,            Messages.Graph_Pref_color_mapping         }
   };

// SET_FORMATTING_ON

   private static final Map2ColorProfile MAP_COLOR_ELEVATION;
   private static final Map2ColorProfile MAP_COLOR_GRADIENT;
   private static final Map2ColorProfile MAP_COLOR_PACE;
   private static final Map2ColorProfile MAP_COLOR_PULSE;
   private static final Map2ColorProfile MAP_COLOR_SPEED;
   private static final Map2ColorProfile MAP_COLOR_RUN_DYN_STEP_LENGTH;

   /**
    * Set map default colors
    */
   static {

      MAP_COLOR_ELEVATION = new Map2ColorProfile(

            new ColorValue[] {

                  new ColorValue(10, 210, 53, 0),
                  new ColorValue(50, 255, 64, 0),
                  new ColorValue(100, 255, 255, 4),
                  new ColorValue(150, 0, 191, 255),
                  new ColorValue(190, 0, 99, 132)
            },

            MapColorProfile.BRIGHTNESS_DIMMING,
            0,
            MapColorProfile.BRIGHTNESS_DEFAULT,
            5);

      MAP_COLOR_PULSE = new Map2ColorProfile(

            new ColorValue[] {

                  new ColorValue(10, 0, 203, 0),
                  new ColorValue(50, 57, 255, 0),
                  new ColorValue(100, 255, 230, 51),
                  new ColorValue(150, 255, 108, 0),
                  new ColorValue(190, 255, 0, 0)
            },

            MapColorProfile.BRIGHTNESS_DIMMING,
            12,
            MapColorProfile.BRIGHTNESS_LIGHTNING,
            52,

            // overwrite min/max values
            true,
            100,
            true,
            150);

      MAP_COLOR_SPEED = new Map2ColorProfile(

            new ColorValue[] {

                  new ColorValue(10, 255, 255, 255),
                  new ColorValue(50, 242, 255, 0),
                  new ColorValue(100, 174, 225, 0),
                  new ColorValue(150, 0, 128, 255),
                  new ColorValue(190, 0, 98, 196)
            },

            MapColorProfile.BRIGHTNESS_DEFAULT,
            8,
            MapColorProfile.BRIGHTNESS_DIMMING,
            48,

            // overwrite min/max values
            true,
            0,
            true,
            50);

      MAP_COLOR_PACE = new Map2ColorProfile(

            new ColorValue[] {

                  new ColorValue(10, 255, 0, 0),
                  new ColorValue(50, 255, 255, 0),
                  new ColorValue(100, 0, 169, 0),
                  new ColorValue(150, 0, 255, 255),
                  new ColorValue(190, 0, 0, 255)
            },

            MapColorProfile.BRIGHTNESS_DIMMING,
            17,
            MapColorProfile.BRIGHTNESS_DIMMING,
            8,

            // overwrite min/max values
            false,
            4,
            true,
            6);

      MAP_COLOR_GRADIENT = new Map2ColorProfile(

            new ColorValue[] {

                  new ColorValue(10, 0, 111, 0),
                  new ColorValue(50, 0, 255, 0),
                  new ColorValue(100, 255, 255, 255),
                  new ColorValue(150, 255, 57, 0),
                  new ColorValue(190, 183, 41, 0)
            },

            MapColorProfile.BRIGHTNESS_DIMMING,
            19,
            MapColorProfile.BRIGHTNESS_DIMMING,
            12,

            // overwrite min/max values
            true,
            -10,
            true,
            10);

      MAP_COLOR_RUN_DYN_STEP_LENGTH = new Map2ColorProfile(

            new ColorValue[] {

                  new ColorValue(10, 140, 0, 255),
                  new ColorValue(50, 215, 0, 134),
                  new ColorValue(100, 255, 77, 0),
                  new ColorValue(150, 255, 160, 55),
                  new ColorValue(190, 255, 236, 0)
            },

            MapColorProfile.BRIGHTNESS_DIMMING,
            20,
            MapColorProfile.BRIGHTNESS_LIGHTNING,
            20,

            // overwrite min/max values
            true,
            1000,
            true,
            1400);

   }

   private static GraphColorManager _instance;

   private static ColorDefinition[] _allGraphColorDefinitions;

   public GraphColorManager() {}

   /**
    * Create graph default colors
    *
    * @return
    */
   private static List<ColorDefinition> createDefaultColors() {

      final List<ColorDefinition> allColorDef = new ArrayList<>();

      allColorDef.add(new ColorDefinition(PREF_GRAPH_TIME,
            Messages.Graph_Pref_color_statistic_time,

            new RGB(0xff, 0xff, 0xff),
            new RGB(0xbb, 0xbb, 0x8c),
            new RGB(0xaa, 0xaa, 0x7f),
            new RGB(0xc9, 0xc9, 0xc9),
            new RGB(0x58, 0x58, 0x43),
            new RGB(0xc9, 0xc9, 0xc9),

            null));

      allColorDef.add(new ColorDefinition(PREF_GRAPH_DISTANCE,
            Messages.Graph_Pref_color_statistic_distance,

            new RGB(0xff, 0xff, 0xff),
            new RGB(0xef, 0xa7, 0x10),
            new RGB(0xcb, 0x8d, 0xe),
            new RGB(0xf8, 0xcd, 0x76),
            new RGB(0x8b, 0x62, 0xa),
            new RGB(0xf8, 0xcd, 0x76),

            null));

      allColorDef.add(new ColorDefinition(PREF_GRAPH_ALTITUDE, // colorDefinitionId

            Messages.Graph_Label_Altitude, //   visibleName

            new RGB(0xb3, 0xff, 0xb3),
            new RGB(0x0, 0xf2, 0x0),
            new RGB(0x0, 0xbb, 0x0),
            new RGB(0x82, 0xff, 0x82),
            new RGB(0x0, 0xaa, 0x0),
            new RGB(0x64, 0xff, 0x64),

            MAP_COLOR_ELEVATION)); //           defaultMapColorProfile

      allColorDef.add(new ColorDefinition(PREF_GRAPH_HEARTBEAT,
            Messages.Graph_Label_Heartbeat,

            new RGB(0xff, 0xd5, 0xd5),
            new RGB(0xff, 0x0, 0x0),
            new RGB(0xfd, 0x0, 0x0),
            new RGB(0xff, 0x42, 0x42),
            new RGB(0xce, 0x0, 0x0),
            new RGB(0xff, 0x3e, 0x3e),

            MAP_COLOR_PULSE));

      allColorDef.add(new ColorDefinition(PREF_GRAPH_SPEED,
            Messages.Graph_Label_Speed,

            new RGB(0xdd, 0xf3, 0xff),
            new RGB(0x0, 0x9f, 0xf4),
            new RGB(0x0, 0x84, 0xd2),
            new RGB(0x66, 0xc6, 0xff),
            new RGB(0x0, 0x75, 0xbb),
            new RGB(0x48, 0xbb, 0xff),

            MAP_COLOR_SPEED));

      allColorDef.add(new ColorDefinition(PREF_GRAPH_PACE,
            Messages.Graph_Label_Pace,

            new RGB(0xef, 0xdd, 0xff),
            new RGB(0x8c, 0xd, 0xff),
            new RGB(0x9c, 0x2f, 0xff),
            new RGB(0xc5, 0x84, 0xff),
            new RGB(0x77, 0x23, 0xc2),
            new RGB(0xb5, 0x7b, 0xe8),

            MAP_COLOR_PACE));

      allColorDef.add(new ColorDefinition(PREF_GRAPH_POWER,
            Messages.Graph_Label_Power,

            new RGB(0xff, 0xd2, 0xee),
            new RGB(0xf0, 0x0, 0x96),
            new RGB(0xf0, 0x0, 0x96),
            new RGB(0xff, 0x42, 0xb8),
            new RGB(0xdd, 0x0, 0x8a),
            new RGB(0xff, 0x0, 0x9f),

            null));

      allColorDef.add(new ColorDefinition(PREF_GRAPH_TEMPTERATURE,
            Messages.Graph_Label_Temperature,

            new RGB(0xc1, 0xf9, 0xff),
            new RGB(0x0, 0xd9, 0xf0),
            new RGB(0x0, 0xc7, 0xdd),
            new RGB(0x0, 0xd8, 0xf0),
            new RGB(0x0, 0x9e, 0xb0),
            new RGB(0x6, 0xe7, 0xff),

            null));

      allColorDef.add(new ColorDefinition(PREF_GRAPH_GRADIENT,
            Messages.Graph_Label_Gradient,

            new RGB(0xff, 0xfc, 0xd5),
            new RGB(0xf9, 0xe7, 0x0),
            new RGB(0xec, 0xce, 0x0),
            new RGB(0xec, 0xce, 0x0),
            new RGB(0xa8, 0x93, 0x0),
            new RGB(0xff, 0xeb, 0x5b),

            MAP_COLOR_GRADIENT));

      allColorDef.add(new ColorDefinition(PREF_GRAPH_ALTIMETER,
            Messages.Graph_Label_Altimeter,

            new RGB(0x95, 0xff, 0xe2),
            new RGB(0x0, 0xf0, 0xae),
            new RGB(0x0, 0xdd, 0xa0),
            new RGB(0x0, 0xff, 0xb8),
            new RGB(0x0, 0xb0, 0x80),
            new RGB(0x0, 0xff, 0xb8),

            null));

      allColorDef.add(new ColorDefinition(PREF_GRAPH_CADENCE,
            Messages.Graph_Label_Cadence,

            new RGB(0xff, 0xdb, 0xc4),
            new RGB(0xf4, 0x62, 0x0),
            new RGB(0xe4, 0x6a, 0x10),
            new RGB(0xe4, 0x6a, 0x10),
            new RGB(0xab, 0x50, 0xc),
            new RGB(0xf0, 0x81, 0x2f),

            null));

      allColorDef.add(new ColorDefinition(PREF_GRAPH_GEAR,
            Messages.Graph_Label_Gears,

            new RGB(0xff, 0xff, 0xff),
            new RGB(0x57, 0x57, 0x57),
            new RGB(0x57, 0x57, 0x57),
            new RGB(0xdd, 0xdd, 0xdd),
            new RGB(0xff, 0x0, 0x0),
            new RGB(0xf4, 0x3e, 0x3e),

            null));

      final String uiSpacing = UI.SPACE3 + UI.SYMBOL_GREATER_THAN + UI.SPACE3;

      /*
       * Running Dynamics
       */

      final String runDynAndSpacing = Messages.Graph_Label_Prefix_RunningDynamics + uiSpacing;

      allColorDef.add(new ColorDefinition(PREF_GRAPH_RUN_DYN_STANCE_TIME,

            runDynAndSpacing + Messages.Graph_Label_RunDyn_StanceTime,

            new RGB(0xff, 0xff, 0xff),
            new RGB(0x0, 0x8b, 0xe9),
            new RGB(0x0, 0x65, 0xa8),
            new RGB(0x0, 0x93, 0xf4),
            new RGB(0x0, 0x53, 0x8a),
            new RGB(0x28, 0xa9, 0xff),

            null));

      allColorDef.add(new ColorDefinition(PREF_GRAPH_RUN_DYN_STANCE_TIME_BALANCED,

            runDynAndSpacing + Messages.Graph_Label_RunDyn_StanceTimeBalance,

            new RGB(0xff, 0xff, 0xff),
            new RGB(0x86, 0x57, 0xe7),
            new RGB(0x4a, 0x1a, 0xb0),
            new RGB(0xa2, 0x7d, 0xec),
            new RGB(0x28, 0xe, 0x61),
            new RGB(0x99, 0x72, 0xeb),

            null));

      allColorDef.add(new ColorDefinition(PREF_GRAPH_RUN_DYN_STEP_LENGTH,

            runDynAndSpacing + Messages.Graph_Label_RunDyn_StepLength,

            new RGB(0xff, 0xff, 0xff),
            new RGB(0xff, 0x4d, 0x0),
            new RGB(0xe6, 0x45, 0x0),
            new RGB(0xff, 0x57, 0xf),
            new RGB(0xe6, 0x45, 0x0),
            new RGB(0xff, 0x69, 0x28),

            MAP_COLOR_RUN_DYN_STEP_LENGTH));

      allColorDef.add(new ColorDefinition(PREF_GRAPH_RUN_DYN_VERTICAL_OSCILLATION,

            runDynAndSpacing + Messages.Graph_Label_RunDyn_VerticalOscillation,

            new RGB(0xff, 0xff, 0xff),
            new RGB(0xa3, 0xd4, 0xe),
            new RGB(0x74, 0x8e, 0x22),
            new RGB(0x8d, 0xae, 0x2b),
            new RGB(0x5c, 0x78, 0x7),
            new RGB(0xa0, 0xd1, 0xc),

            null));

      allColorDef.add(new ColorDefinition(PREF_GRAPH_RUN_DYN_VERTICAL_RATIO,

            runDynAndSpacing + Messages.Graph_Label_RunDyn_VerticalRatio,

            new RGB(0xff, 0xff, 0xff),
            new RGB(0xff, 0xce, 0x17),
            new RGB(0xc6, 0x9e, 0x0),
            new RGB(0xc6, 0x9e, 0x0),
            new RGB(0x84, 0x69, 0x0),
            new RGB(0xec, 0xbd, 0x0),

            null));

      /*
       * Swimming
       */

      allColorDef.add(new ColorDefinition(PREF_GRAPH_SWIM_STROKES,
            Messages.Graph_Label_Prefix_Swimming + uiSpacing + Messages.Graph_Label_Swim_Strokes,

            new RGB(0xff, 0xff, 0xff),
            new RGB(0xff, 0x4d, 0x0),
            new RGB(0xe6, 0x45, 0x0),
            new RGB(0xff, 0x6a, 0x2b),
            new RGB(0xa4, 0x31, 0x0),
            new RGB(0xff, 0x87, 0x53),

            null));

      allColorDef.add(new ColorDefinition(PREF_GRAPH_SWIM_SWOLF,

            Messages.Graph_Label_Prefix_Swimming + uiSpacing + Messages.Graph_Label_Swim_Swolf,

            new RGB(0xff, 0xff, 0xff),
            new RGB(0x86, 0x57, 0xe7),
            new RGB(0x4a, 0x1a, 0xb0),
            new RGB(0xa8, 0x87, 0xed),
            new RGB(0x28, 0xe, 0x61),
            new RGB(0xa8, 0x87, 0xed),

            null));

      /*
       * Training
       */
      allColorDef.add(new ColorDefinition(PREF_GRAPH_TRAINING_EFFECT_AEROB,
            Messages.Graph_Label_Prefix_Training + uiSpacing + Messages.Graph_Label_Training_Effect_Aerob,

            new RGB(0xff, 0xff, 0xff),
            new RGB(0x0, 0x8b, 0xe9),
            new RGB(0x0, 0x65, 0xa8),
            new RGB(0x11, 0x9f, 0xff),
            new RGB(0x0, 0x53, 0x8a),
            new RGB(0x11, 0x9f, 0xff),

            null));

      allColorDef.add(new ColorDefinition(PREF_GRAPH_TRAINING_EFFECT_ANAEROB,
            Messages.Graph_Label_Prefix_Training + uiSpacing + Messages.Graph_Label_Training_Effect_Anaerob,

            new RGB(0xff, 0xff, 0xff),
            new RGB(0xa3, 0xd4, 0xe),
            new RGB(0x74, 0x8e, 0x22),
            new RGB(0xa4, 0xd6, 0xc),
            new RGB(0x5c, 0x78, 0x7),
            new RGB(0xa4, 0xd6, 0xc),

            null));

      allColorDef.add(new ColorDefinition(PREF_GRAPH_TRAINING_PERFORMANCE,
            Messages.Graph_Label_Prefix_Training + uiSpacing + Messages.Graph_Label_Training_Performance,

            new RGB(0xff, 0xff, 0xff),
            new RGB(0xff, 0x40, 0x0),
            new RGB(0xec, 0x3c, 0x0),
            new RGB(0xff, 0x6d, 0x2f),
            new RGB(0xa4, 0x31, 0x0),
            new RGB(0xff, 0x84, 0x4f),

            null));

      /*
       * Athlete's data
       */
      allColorDef.add(new ColorDefinition(PREF_GRAPH_BODYWEIGHT,
            Messages.Graph_Label_Prefix_AthleteData + uiSpacing + Messages.Graph_Label_Athlete_Body_Weight,

            new RGB(0xff, 0xff, 0xff),
            new RGB(0xff, 0xaa, 0x80),
            new RGB(0xf2, 0x87, 0x16),
            new RGB(0xf2, 0x87, 0x16),
            new RGB(0xab, 0x64, 0x22),
            new RGB(0xf5, 0xa5, 0x4b),

            null));

      allColorDef.add(new ColorDefinition(PREF_GRAPH_BODYFAT,
            Messages.Graph_Label_Prefix_AthleteData + uiSpacing + Messages.Graph_Label_Athlete_Body_Fat,

            new RGB(0xff, 0xff, 0xff),
            new RGB(0xff, 0x87, 0x80),
            new RGB(0xf2, 0x77, 0x68),
            new RGB(0xf2, 0x77, 0x68),
            new RGB(0x85, 0x40, 0x5),
            new RGB(0xf2, 0x77, 0x68),

            null));

      /*
       * Other
       */

      allColorDef.add(new ColorDefinition(PREF_GRAPH_TOUR_COMPARE,
            Messages.Graph_Label_Tour_Compare,

            new RGB(0xff, 0xff, 0xff),
            new RGB(0xff, 0x8c, 0x1a),
            new RGB(0xf2, 0x87, 0x16),
            new RGB(0xf2, 0x87, 0x16),
            new RGB(0x8b, 0x4d, 0xf),
            new RGB(0xf2, 0x87, 0x16),

            null));

      allColorDef.add(new ColorDefinition(PREF_GRAPH_HISTORY,
            Messages.Graph_Label_History,

            new RGB(0xff, 0xff, 0xff),
            new RGB(0xff, 0x80, 0x33),
            new RGB(0xff, 0x80, 0x33),
            new RGB(0xff, 0x80, 0x33),
            new RGB(0xff, 0x80, 0x33),
            new RGB(0xff, 0x80, 0x33),

            null));

      allColorDef.add(new ColorDefinition(PREF_GRAPH_TOUR,
            Messages.Graph_Label_Tour,

            new RGB(0xff, 0xff, 0xff),
            new RGB(0xd, 0xaa, 0xff),
            new RGB(0xd, 0xaa, 0xff),
            new RGB(0xd, 0xaa, 0xff),
            new RGB(0xd, 0xaa, 0xff),
            new RGB(0xd, 0xaa, 0xff),

            null));

      allColorDef.add(new ColorDefinition(PREF_GRAPH_SENSOR,

            Messages.Graph_Label_Sensor,

            new RGB(0xc0, 0xc0, 0xc0),
            new RGB(0xff, 0x42, 0xb8),
            new RGB(0xff, 0x42, 0xb8),
            new RGB(0xff, 0x42, 0xb8),
            new RGB(0xf0, 0x0, 0x96),
            new RGB(0xff, 0x6c, 0xc8),

            null));

      return allColorDef;
   }

   public static ColorDefinition[] getAllColorDefinitions() {

      if (_allGraphColorDefinitions != null) {
         return _allGraphColorDefinitions;
      }

      final List<ColorDefinition> allColorDef = createDefaultColors();

      _allGraphColorDefinitions = allColorDef.toArray(new ColorDefinition[allColorDef.size()]);

      readXmlMapColors();
      setMapColors();

      return _allGraphColorDefinitions;
   }

   public static GraphColorManager getInstance() {

      if (_instance == null) {
         _instance = new GraphColorManager();
      }

      return _instance;
   }

   private static XMLMemento getXMLMementoRoot() {

      Document document;
      try {
         document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
         final Element element = document.createElement(MEMENTO_ROOT);
         element.setAttribute("version", "1"); //$NON-NLS-1$ //$NON-NLS-2$
         document.appendChild(element);

         return new XMLMemento(document, element);

      } catch (final ParserConfigurationException e) {
         throw new Error(e.getMessage());
      }
   }

   /**
    * Read legend data from a xml file
    */
   private static void readXmlMapColors() {

      final IPath stateLocation = Platform.getStateLocation(CommonActivator.getDefault().getBundle());
      final File file = stateLocation.append(MEMENTO_LEGEND_COLOR_FILE).toFile();

      // check if file is available
      if (file.exists() == false) {
         return;
      }

      try (FileInputStream inputStream = new FileInputStream(file);
            InputStreamReader reader = new InputStreamReader(inputStream, UI.UTF_8)) {

         final XMLMemento mementoRoot = XMLMemento.createReadRoot(reader);
         final IMemento[] mementoLegendColors = mementoRoot.getChildren(MEMENTO_CHILD_LEGEND_COLOR);

         // loop: all legend colors
         for (final IMemento mementoLegendColor : mementoLegendColors) {

            // check pref name
            final String prefName = mementoLegendColor.getString(TAG_LEGEND_COLOR_PREF_NAME);
            if (prefName == null) {
               continue;
            }

            // check value colors
            final IMemento[] mementoValueColors = mementoLegendColor.getChildren(MEMENTO_CHILD_VALUE_COLOR);
            if (mementoValueColors == null) {
               continue;
            }

            final Map2ColorProfile loadedProfile = new Map2ColorProfile();

            /*
             * value colors
             */
            final ArrayList<ColorValue> valueColors = new ArrayList<>();

            // loop: all value colors
            for (final IMemento mementoValueColor : mementoValueColors) {

               final Float value = mementoValueColor.getFloat(TAG_VALUE_COLOR_VALUE);
               final Integer red = mementoValueColor.getInteger(TAG_VALUE_COLOR_RED);
               final Integer green = mementoValueColor.getInteger(TAG_VALUE_COLOR_GREEN);
               final Integer blue = mementoValueColor.getInteger(TAG_VALUE_COLOR_BLUE);

               if (value != null && red != null && green != null && blue != null) {
                  valueColors.add(new ColorValue(value, red, green, blue));
               }
            }
            loadedProfile.setColorValues(valueColors.toArray(new ColorValue[valueColors.size()]));

            /*
             * min/max brightness
             */
            final IMemento[] mementoBrightness = mementoLegendColor.getChildren(MEMENTO_CHILD_BRIGHTNESS);
            if (mementoBrightness.length > 0) {

               final IMemento mementoBrightness0 = mementoBrightness[0];

               final Integer minBrightness = mementoBrightness0.getInteger(TAG_BRIGHTNESS_MIN);
               if (minBrightness != null) {
                  loadedProfile.setMinBrightness(minBrightness);
               }
               final Integer minBrightnessFactor = mementoBrightness0.getInteger(TAG_BRIGHTNESS_MIN_FACTOR);
               if (minBrightness != null) {
                  loadedProfile.setMinBrightnessFactor(minBrightnessFactor);
               }
               final Integer maxBrightness = mementoBrightness0.getInteger(TAG_BRIGHTNESS_MAX);
               if (maxBrightness != null) {
                  loadedProfile.setMaxBrightness(maxBrightness);
               }
               final Integer maxBrightnessFactor = mementoBrightness0.getInteger(TAG_BRIGHTNESS_MAX_FACTOR);
               if (minBrightness != null) {
                  loadedProfile.setMaxBrightnessFactor(maxBrightnessFactor);
               }
            }

            /*
             * min/max overwrite
             */
            final IMemento[] mementoMinMaxValue = mementoLegendColor.getChildren(MEMENTO_CHILD_MIN_MAX_VALUE);
            if (mementoMinMaxValue.length > 0) {

               final IMemento mementoMinMaxValue0 = mementoMinMaxValue[0];

               final Integer isMinOverwrite = mementoMinMaxValue0.getInteger(TAG_IS_MIN_VALUE_OVERWRITE);
               if (isMinOverwrite != null) {
                  loadedProfile.setIsMinValueOverwrite(isMinOverwrite == 1);
               }
               final Integer minValue = mementoMinMaxValue0.getInteger(TAG_MIN_VALUE_OVERWRITE);
               if (minValue != null) {
                  loadedProfile.setMinValueOverwrite(minValue);
               }

               final Integer isMaxOverwrite = mementoMinMaxValue0.getInteger(TAG_IS_MAX_VALUE_OVERWRITE);
               if (isMaxOverwrite != null) {
                  loadedProfile.setIsMaxValueOverwrite(isMaxOverwrite == 1);
               }
               final Integer maxValue = mementoMinMaxValue0.getInteger(TAG_MAX_VALUE_OVERWRITE);
               if (maxValue != null) {
                  loadedProfile.setMaxValueOverwrite(maxValue);
               }
            }

            /*
             * update color definition with the read data
             */
            for (final ColorDefinition colorDefinition : _allGraphColorDefinitions) {

               if (colorDefinition.getColorDefinitionId().equals(prefName)) {

                  // color definition found

                  colorDefinition.setMap2Color_Active(loadedProfile);
                  break;
               }
            }
         }

      } catch (final IOException | WorkbenchException | NumberFormatException e) {
         e.printStackTrace();
      }
   }

   public static void saveColors() {

      // save all graph colors, in the pref store and the map color in a xml file.
      saveColors_GraphColors_InPrefStore();
      saveColors_MapColors_InXml();

      // update active colors
      for (final ColorDefinition graphDefinition : getAllColorDefinitions()) {

         // graph color
         graphDefinition.setGradientBright_Active(graphDefinition.getGradientBright_New());
         graphDefinition.setGradientDark_Active(graphDefinition.getGradientDark_New());

         graphDefinition.setLineColor_Active_LightTheme(graphDefinition.getLineColor_New_Light());
         graphDefinition.setLineColor_Active_DarkTheme(graphDefinition.getLineColor_New_Dark());

         graphDefinition.setTextColor_Active_LightTheme(graphDefinition.getTextColor_New_Light());
         graphDefinition.setTextColor_Active_DarkTheme(graphDefinition.getTextColor_New_Dark());

         // 2D map color
         graphDefinition.setMap2Color_Active(graphDefinition.getMap2Color_New());
      }
   }

   private static void saveColors_GraphColors_InPrefStore() {

      final IPreferenceStore commonPrefStore = CommonActivator.getPrefStore();

      for (final ColorDefinition colorDefinition : getAllColorDefinitions()) {

         PreferenceConverter.setValue(
               commonPrefStore,
               colorDefinition.getGraphPrefName(PREF_COLOR_GRADIENT_BRIGHT),
               colorDefinition.getGradientBright_New());

         PreferenceConverter.setValue(
               commonPrefStore,
               colorDefinition.getGraphPrefName(PREF_COLOR_GRADIENT_DARK),
               colorDefinition.getGradientDark_New());

         PreferenceConverter.setValue(
               commonPrefStore,
               colorDefinition.getGraphPrefName(PREF_COLOR_LINE_LIGHT),
               colorDefinition.getLineColor_New_Light());
         PreferenceConverter.setValue(
               commonPrefStore,
               colorDefinition.getGraphPrefName(PREF_COLOR_LINE_DARK),
               colorDefinition.getLineColor_New_Dark());

         PreferenceConverter.setValue(
               commonPrefStore,
               colorDefinition.getGraphPrefName(PREF_COLOR_TEXT_LIGHT),
               colorDefinition.getTextColor_New_Light());
         PreferenceConverter.setValue(
               commonPrefStore,
               colorDefinition.getGraphPrefName(PREF_COLOR_TEXT_DARK),
               colorDefinition.getTextColor_New_Dark());
      }
   }

   /**
    * Write the map color data into a xml file.
    */
   private static void saveColors_MapColors_InXml() {

      BufferedWriter writer = null;

      try {

         final IPath stateLocation = Platform.getStateLocation(CommonActivator.getDefault().getBundle());
         final File file = stateLocation.append(MEMENTO_LEGEND_COLOR_FILE).toFile();

         writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), UI.UTF_8));

         final XMLMemento xmlMemento = getXMLMementoRoot();

         for (final ColorDefinition graphDefinition : getAllColorDefinitions()) {

            final Map2ColorProfile mapColor = graphDefinition.getMap2Color_New();

            // map color can be null when it's not defined
            if (mapColor == null) {
               continue;
            }

            final IMemento mementoLegendColor = xmlMemento.createChild(MEMENTO_CHILD_LEGEND_COLOR);
            mementoLegendColor.putString(TAG_LEGEND_COLOR_PREF_NAME, graphDefinition.getColorDefinitionId());

            for (final ColorValue valueColor : mapColor.getColorValues()) {

               final IMemento mementoValueColor = mementoLegendColor.createChild(MEMENTO_CHILD_VALUE_COLOR);

               mementoValueColor.putFloat(TAG_VALUE_COLOR_VALUE, valueColor.value);
               mementoValueColor.putInteger(TAG_VALUE_COLOR_RED, valueColor.red);
               mementoValueColor.putInteger(TAG_VALUE_COLOR_GREEN, valueColor.green);
               mementoValueColor.putInteger(TAG_VALUE_COLOR_BLUE, valueColor.blue);
            }

            final IMemento mementoBrightness = mementoLegendColor.createChild(MEMENTO_CHILD_BRIGHTNESS);
            mementoBrightness.putInteger(TAG_BRIGHTNESS_MIN, mapColor.getMinBrightness());
            mementoBrightness.putInteger(TAG_BRIGHTNESS_MIN_FACTOR, mapColor.getMinBrightnessFactor());
            mementoBrightness.putInteger(TAG_BRIGHTNESS_MAX, mapColor.getMaxBrightness());
            mementoBrightness.putInteger(TAG_BRIGHTNESS_MAX_FACTOR, mapColor.getMaxBrightnessFactor());

            final IMemento mementoMinMaxValue = mementoLegendColor.createChild(MEMENTO_CHILD_MIN_MAX_VALUE);
            mementoMinMaxValue.putInteger(TAG_IS_MIN_VALUE_OVERWRITE, mapColor.isMinValueOverwrite() ? 1 : 0);
            mementoMinMaxValue.putInteger(TAG_MIN_VALUE_OVERWRITE, mapColor.getMinValueOverwrite());
            mementoMinMaxValue.putInteger(TAG_IS_MAX_VALUE_OVERWRITE, mapColor.isMaxValueOverwrite() ? 1 : 0);
            mementoMinMaxValue.putInteger(TAG_MAX_VALUE_OVERWRITE, mapColor.getMaxValueOverwrite());
         }

         xmlMemento.save(writer);

      } catch (final IOException e) {
         e.printStackTrace();
      } finally {
         if (writer != null) {
            try {
               writer.close();
            } catch (final IOException e) {
               e.printStackTrace();
            }
         }
      }
   }

   /**
    * set legend colors when available
    */
   private static void setMapColors() {

      for (final ColorDefinition colorDefinition : _allGraphColorDefinitions) {

         // set legend color
         if (colorDefinition.getMap2Color_Active() == null) {

            // legend color is not set, try to get default when available

            final Map2ColorProfile defaultMapColor = colorDefinition.getMap2Color_Default();

            if (defaultMapColor != null) {
               colorDefinition.setMap2Color_Active(defaultMapColor.clone());
            }
         }

         // set new map color
         final Map2ColorProfile mapColor = colorDefinition.getMap2Color_Active();
         colorDefinition.setMap2Color_New(mapColor);
      }
   }

   public ColorDefinition getColorDefinition(final MapGraphId graphId) {

      switch (graphId) {

      case Altitude:
         return getGraphColorDefinition(PREF_GRAPH_ALTITUDE);

      case Gradient:
         return getGraphColorDefinition(PREF_GRAPH_GRADIENT);

      case Pace:
         return getGraphColorDefinition(PREF_GRAPH_PACE);

      case Pulse:
         return getGraphColorDefinition(PREF_GRAPH_HEARTBEAT);

      case Speed:
         return getGraphColorDefinition(PREF_GRAPH_SPEED);

      case RunDyn_StepLength:
         return getGraphColorDefinition(PREF_GRAPH_RUN_DYN_STEP_LENGTH);

      default:
         break;
      }

      return null;
   }

   /**
    * @param preferenceName
    *           preference name PREF_GRAPH_...
    * @return Returns the {@link ColorDefinition} for the preference name
    */
   public ColorDefinition getGraphColorDefinition(final String preferenceName) {

      final ColorDefinition[] colorDefinitions = getAllColorDefinitions();

      for (final ColorDefinition colorDefinition : colorDefinitions) {
         if (colorDefinition.getColorDefinitionId().equals(preferenceName)) {
            return colorDefinition;
         }
      }

      return null;
   }
}
