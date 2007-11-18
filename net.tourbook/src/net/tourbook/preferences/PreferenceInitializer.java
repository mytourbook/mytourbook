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
package net.tourbook.preferences;

import net.tourbook.colors.ColorDefinition;
import net.tourbook.colors.GraphColors;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.tour.TourManager;
import net.tourbook.util.StringToArrayConverter;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	/*
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	@Override
	public void initializeDefaultPreferences() {

		IPreferenceStore store = TourbookPlugin.getDefault().getPreferenceStore();

		/*
		 * general
		 */
		store.setDefault(ITourbookPreferences.MEASUREMENT_SYSTEM_DISTANCE,
				ITourbookPreferences.MEASUREMENT_SYSTEM_DISTANCE_KM);

		store.setDefault(ITourbookPreferences.MEASUREMENT_SYSTEM_ALTITUDE,
				ITourbookPreferences.MEASUREMENT_SYSTEM_ALTITUDE_M);

		store.setDefault(ITourbookPreferences.MEASUREMENT_SYSTEM_TEMPERATURE,
				ITourbookPreferences.MEASUREMENT_SYSTEM_TEMPERATURE_C);

		store.setDefault(ITourbookPreferences.MEASUREMENT_SYSTEM_SHOW_IN_UI, true);

		/*
		 * statistics
		 */
		store.setDefault(ITourbookPreferences.STAT_ALTITUDE_LOW_VALUE, 0);
		store.setDefault(ITourbookPreferences.STAT_ALTITUDE_INTERVAL, 250);
		store.setDefault(ITourbookPreferences.STAT_ALTITUDE_NUMBERS, 10);

		store.setDefault(ITourbookPreferences.STAT_DISTANCE_LOW_VALUE, 0);
		store.setDefault(ITourbookPreferences.STAT_DISTANCE_INTERVAL, 10);
		store.setDefault(ITourbookPreferences.STAT_DISTANCE_NUMBERS, 10);

		store.setDefault(ITourbookPreferences.STAT_DURATION_LOW_VALUE, 0);
		store.setDefault(ITourbookPreferences.STAT_DURATION_INTERVAL, 60);
		store.setDefault(ITourbookPreferences.STAT_DURATION_NUMBERS, 10);

		/*
		 * graph color preferences
		 */

		for (ColorDefinition graphDefinition : GraphColors.getInstance().getGraphDefinitionList()) {

			String graphName = graphDefinition.getGraphPrefName();

			PreferenceConverter.setDefault(store,
					graphName + GraphColors.PREF_COLOR_BRIGHT,
					graphDefinition.getDefaultGradientBright());

			PreferenceConverter.setDefault(store,
					graphName + GraphColors.PREF_COLOR_DARK,
					graphDefinition.getDefaultGradientDark());

			PreferenceConverter.setDefault(store,
					graphName + GraphColors.PREF_COLOR_LINE,
					graphDefinition.getDefaultLineColor());
		}

		/*
		 * graph preferences
		 */
		store.setDefault(ITourbookPreferences.GRAPH_VISIBLE,
				Integer.toString(TourManager.GRAPH_ALTITUDE));

		final String separator = StringToArrayConverter.STRING_SEPARATOR;

		store.setDefault(ITourbookPreferences.GRAPH_ALL,
				(Integer.toString(TourManager.GRAPH_ALTITUDE) + separator)
						+ (Integer.toString(TourManager.GRAPH_SPEED) + separator)
						+ (Integer.toString(TourManager.GRAPH_PACE) + separator)
						+ (Integer.toString(TourManager.GRAPH_PULSE) + separator)
						+ (Integer.toString(TourManager.GRAPH_TEMPERATURE) + separator)
						+ (Integer.toString(TourManager.GRAPH_CADENCE) + separator)
						+ (Integer.toString(TourManager.GRAPH_ALTIMETER) + separator)
						+ (Integer.toString(TourManager.GRAPH_GRADIENT) + separator)
						+ Integer.toString(TourManager.GRAPH_POWER));

		// define which unit is shown on the x-axis
		store.setDefault(ITourbookPreferences.GRAPH_X_AXIS, TourManager.X_AXIS_DISTANCE);
	}
}
