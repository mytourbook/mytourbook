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
 * Class used to initialize default preference values.
 */
public class CommonPreferenceInitializer extends AbstractPreferenceInitializer {

   @Override
   public void initializeDefaultPreferences() {

      final IPreferenceStore store = CommonActivator.getPrefStore();

      /*
       * graph color preferences
       */
      for (final ColorDefinition colorDefinition : GraphColorManager.getInstance().getGraphColorDefinitions()) {

         PreferenceConverter.setDefault(
               store,
               colorDefinition.getGraphPrefName(GraphColorManager.PREF_COLOR_BRIGHT),
               colorDefinition.getGradientBright_Default());

         PreferenceConverter.setDefault(
               store,
               colorDefinition.getGraphPrefName(GraphColorManager.PREF_COLOR_DARK),
               colorDefinition.getGradientDark_Default());

         PreferenceConverter.setDefault(
               store,
               colorDefinition.getGraphPrefName(GraphColorManager.PREF_COLOR_LINE),
               colorDefinition.getLineColor_Default());
      }

      /*
       * Display formats
       */
      store.setDefault(ICommonPreferences.DISPLAY_FORMAT_IS_LIVE_UPDATE, true);

      store.setDefault(ICommonPreferences.DISPLAY_FORMAT_ALTITUDE, ValueFormat.NUMBER_1_0.name());
      store.setDefault(ICommonPreferences.DISPLAY_FORMAT_CADENCE, ValueFormat.NUMBER_1_0.name());
      store.setDefault(ICommonPreferences.DISPLAY_FORMAT_DISTANCE, ValueFormat.NUMBER_1_0.name());
      store.setDefault(ICommonPreferences.DISPLAY_FORMAT_POWER, ValueFormat.NUMBER_1_0.name());
      store.setDefault(ICommonPreferences.DISPLAY_FORMAT_PULSE, ValueFormat.NUMBER_1_0.name());
      store.setDefault(ICommonPreferences.DISPLAY_FORMAT_SPEED, ValueFormat.NUMBER_1_0.name());

      store.setDefault(ICommonPreferences.DISPLAY_FORMAT_ELAPSED_TIME, ValueFormat.TIME_HH_MM.name());
      store.setDefault(ICommonPreferences.DISPLAY_FORMAT_RECORDED_TIME, ValueFormat.TIME_HH_MM.name());
      store.setDefault(ICommonPreferences.DISPLAY_FORMAT_PAUSED_TIME, ValueFormat.TIME_HH_MM.name());
      store.setDefault(ICommonPreferences.DISPLAY_FORMAT_MOVING_TIME, ValueFormat.TIME_HH_MM.name());
      store.setDefault(ICommonPreferences.DISPLAY_FORMAT_BREAK_TIME, ValueFormat.TIME_HH_MM.name());

      /*
       * Time zone
       */
      final ZoneId defaultZoneId = ZoneId.systemDefault();
      final String defaultId = defaultZoneId.getId();

      store.setDefault(ICommonPreferences.TIME_ZONE_SELECTED_CUSTOM_ZONE, 1);
      store.setDefault(ICommonPreferences.TIME_ZONE_IS_LIVE_UPDATE, true);
      store.setDefault(ICommonPreferences.TIME_ZONE_IS_USE_SYSTEM_TIME_ZONE, true);
      store.setDefault(ICommonPreferences.TIME_ZONE_LOCAL_ID, defaultId);
      store.setDefault(ICommonPreferences.TIME_ZONE_LOCAL_ID_1, defaultId);
      store.setDefault(ICommonPreferences.TIME_ZONE_LOCAL_ID_2, defaultId);
      store.setDefault(ICommonPreferences.TIME_ZONE_LOCAL_ID_3, defaultId);

      /*
       * calendar week
       */
      store.setDefault(ICommonPreferences.CALENDAR_WEEK_FIRST_DAY_OF_WEEK, DayOfWeek.MONDAY.getValue());
      store.setDefault(ICommonPreferences.CALENDAR_WEEK_MIN_DAYS_IN_FIRST_WEEK, 4);
   }
}
