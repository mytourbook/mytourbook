/*******************************************************************************
 * Copyright (C) 2005, 2022 Wolfgang Schramm and Contributors
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
package net.tourbook.common.preferences;

import java.time.DayOfWeek;
import java.time.ZoneId;

import net.tourbook.common.CommonActivator;
import net.tourbook.common.color.ColorDefinition;
import net.tourbook.common.color.GraphColorManager;
import net.tourbook.common.formatter.ValueFormat;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;

/**
 * Initialize default preference values.
 */
public class CommonPreferenceInitializer extends AbstractPreferenceInitializer {

   @Override
   public void initializeDefaultPreferences() {

      final IPreferenceStore store = CommonActivator.getPrefStore();

      /*
       * Graph default colors
       */
      for (final ColorDefinition colorDefinition : GraphColorManager.getAllColorDefinitions()) {

         PreferenceConverter.setDefault(
               store,
               colorDefinition.getGraphPrefName(GraphColorManager.PREF_COLOR_GRADIENT_BRIGHT),
               colorDefinition.getGradientBright_Default());
         PreferenceConverter.setDefault(
               store,
               colorDefinition.getGraphPrefName(GraphColorManager.PREF_COLOR_GRADIENT_DARK),
               colorDefinition.getGradientDark_Default());

         PreferenceConverter.setDefault(
               store,
               colorDefinition.getGraphPrefName(GraphColorManager.PREF_COLOR_LINE_LIGHT),
               colorDefinition.getLineColor_Default_Light());
         PreferenceConverter.setDefault(
               store,
               colorDefinition.getGraphPrefName(GraphColorManager.PREF_COLOR_LINE_DARK),
               colorDefinition.getLineColor_Default_Dark());

         PreferenceConverter.setDefault(
               store,
               colorDefinition.getGraphPrefName(GraphColorManager.PREF_COLOR_TEXT_LIGHT),
               colorDefinition.getTextColor_Default_Light());
         PreferenceConverter.setDefault(
               store,
               colorDefinition.getGraphPrefName(GraphColorManager.PREF_COLOR_TEXT_DARK),
               colorDefinition.getTextColor_Default_Dark());
      }

// SET_FORMATTING_OFF

      // measurement system is hidden by default since version 11.7
      store.setDefault(ICommonPreferences.MEASUREMENT_SYSTEM_SHOW_IN_UI,        false);

      /*
       * Display formats
       */
      store.setDefault(ICommonPreferences.DISPLAY_FORMAT_IS_LIVE_UPDATE,        true);
      store.setDefault(ICommonPreferences.DISPLAY_FORMAT_SELECTED_TAB,          0);

      store.setDefault(ICommonPreferences.DISPLAY_FORMAT_ALTITUDE,              ValueFormat.NUMBER_1_0.name());
      store.setDefault(ICommonPreferences.DISPLAY_FORMAT_CADENCE,               ValueFormat.NUMBER_1_0.name());
      store.setDefault(ICommonPreferences.DISPLAY_FORMAT_DISTANCE,              ValueFormat.NUMBER_1_0.name());
      store.setDefault(ICommonPreferences.DISPLAY_FORMAT_POWER,                 ValueFormat.NUMBER_1_0.name());
      store.setDefault(ICommonPreferences.DISPLAY_FORMAT_PULSE,                 ValueFormat.NUMBER_1_0.name());
      store.setDefault(ICommonPreferences.DISPLAY_FORMAT_SPEED,                 ValueFormat.NUMBER_1_0.name());
      store.setDefault(ICommonPreferences.DISPLAY_FORMAT_TEMPERATURE,           ValueFormat.NUMBER_1_0.name());

      store.setDefault(ICommonPreferences.DISPLAY_FORMAT_ELAPSED_TIME,          ValueFormat.TIME_HH_MM.name());
      store.setDefault(ICommonPreferences.DISPLAY_FORMAT_RECORDED_TIME,         ValueFormat.TIME_HH_MM.name());
      store.setDefault(ICommonPreferences.DISPLAY_FORMAT_PAUSED_TIME,           ValueFormat.TIME_HH_MM.name());
      store.setDefault(ICommonPreferences.DISPLAY_FORMAT_MOVING_TIME,           ValueFormat.TIME_HH_MM.name());
      store.setDefault(ICommonPreferences.DISPLAY_FORMAT_BREAK_TIME,            ValueFormat.TIME_HH_MM.name());

      store.setDefault(ICommonPreferences.DISPLAY_FORMAT_ALTITUDE_SUMMARY,      ValueFormat.NUMBER_1_0.name());
      store.setDefault(ICommonPreferences.DISPLAY_FORMAT_CADENCE_SUMMARY,       ValueFormat.NUMBER_1_0.name());
      store.setDefault(ICommonPreferences.DISPLAY_FORMAT_DISTANCE_SUMMARY,      ValueFormat.NUMBER_1_0.name());
      store.setDefault(ICommonPreferences.DISPLAY_FORMAT_POWER_SUMMARY,         ValueFormat.NUMBER_1_0.name());
      store.setDefault(ICommonPreferences.DISPLAY_FORMAT_PULSE_SUMMARY,         ValueFormat.NUMBER_1_0.name());
      store.setDefault(ICommonPreferences.DISPLAY_FORMAT_SPEED_SUMMARY,         ValueFormat.NUMBER_1_0.name());
      store.setDefault(ICommonPreferences.DISPLAY_FORMAT_TEMPERATURE_SUMMARY,   ValueFormat.NUMBER_1_0.name());

      store.setDefault(ICommonPreferences.DISPLAY_FORMAT_ELAPSED_TIME_SUMMARY,  ValueFormat.TIME_HH.name());
      store.setDefault(ICommonPreferences.DISPLAY_FORMAT_RECORDED_TIME_SUMMARY, ValueFormat.TIME_HH.name());
      store.setDefault(ICommonPreferences.DISPLAY_FORMAT_PAUSED_TIME_SUMMARY,   ValueFormat.TIME_HH.name());
      store.setDefault(ICommonPreferences.DISPLAY_FORMAT_MOVING_TIME_SUMMARY,   ValueFormat.TIME_HH.name());
      store.setDefault(ICommonPreferences.DISPLAY_FORMAT_BREAK_TIME_SUMMARY,    ValueFormat.TIME_HH.name());

      /*
       * Time zone
       */
      final ZoneId defaultZoneId = ZoneId.systemDefault();
      final String defaultId = defaultZoneId.getId();

      store.setDefault(ICommonPreferences.TIME_ZONE_SELECTED_CUSTOM_ZONE,       1);
      store.setDefault(ICommonPreferences.TIME_ZONE_IS_LIVE_UPDATE,             true);
      store.setDefault(ICommonPreferences.TIME_ZONE_IS_USE_SYSTEM_TIME_ZONE,    true);
      store.setDefault(ICommonPreferences.TIME_ZONE_LOCAL_ID,                   defaultId);
      store.setDefault(ICommonPreferences.TIME_ZONE_LOCAL_ID_1,                 defaultId);
      store.setDefault(ICommonPreferences.TIME_ZONE_LOCAL_ID_2,                 defaultId);
      store.setDefault(ICommonPreferences.TIME_ZONE_LOCAL_ID_3,                 defaultId);

      /*
       * calendar week
       */
      store.setDefault(ICommonPreferences.CALENDAR_WEEK_FIRST_DAY_OF_WEEK,       DayOfWeek.MONDAY.getValue());
      store.setDefault(ICommonPreferences.CALENDAR_WEEK_MIN_DAYS_IN_FIRST_WEEK,  4);

      /*
       * Transform values
       */
      store.setDefault(ICommonPreferences.TRANSFORM_VALUE_OPACITY_MAX,           10);

// SET_FORMATTING_ON
   }
}
