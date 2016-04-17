/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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
package net.tourbook.ui;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageAppearanceDisplayFormat;

import org.eclipse.jface.preference.IPreferenceStore;

public class FormatManager {

	private final static IPreferenceStore	_prefStore	= TourbookPlugin.getPrefStore();

	/** When <code>true</code> then avg cadence format is #.#, default is # */
	public static boolean					isAvgCadence_1_1;

	/** When <code>true</code> then avg cadence format is #.##, default is # */
	public static boolean					isAvgCadence_1_2;

	/** When <code>true</code> then avg power format is #.#, default is # */
	public static boolean					isAvgPower_1_1;

	/** When <code>true</code> then avg pulse format is #.#, default is # */
	public static boolean					isAvgPulse_1_1;

	/** When <code>true</code> then calories is <i>cal</i>, default is <i>kcal</i>. */
	public static boolean					isCalories_cal;

	/** When <code>true</code> then driving time format is <i>hh:mm:ss</i>, default is <i>hh:mm</i>. */
	public static boolean					isDrivingTime_hhmmss;

	/** When <code>true</code> then recording time format is <i>hh:mm:ss</i>, default is <i>hh:mm</i>. */
	public static boolean					isRecordingTime_hhmmss;

	public static void updateDisplayFormats() {

		/*
		 * Cadence
		 */
		final String cadence = _prefStore.getString(ITourbookPreferences.DISPLAY_FORMAT_AVG_CADENCE);
		isAvgCadence_1_1 = PrefPageAppearanceDisplayFormat.DISPLAY_FORMAT_1_1.equals(cadence);
		isAvgCadence_1_2 = PrefPageAppearanceDisplayFormat.DISPLAY_FORMAT_1_2.equals(cadence);

		/*
		 * Power
		 */
		final String power = _prefStore.getString(ITourbookPreferences.DISPLAY_FORMAT_AVG_POWER);
		isAvgPower_1_1 = PrefPageAppearanceDisplayFormat.DISPLAY_FORMAT_1_1.equals(power);

		/*
		 * Pulse
		 */
		final String pulse = _prefStore.getString(ITourbookPreferences.DISPLAY_FORMAT_AVG_PULSE);
		isAvgPulse_1_1 = PrefPageAppearanceDisplayFormat.DISPLAY_FORMAT_1_1.equals(pulse);

		/*
		 * Calories
		 */
		isCalories_cal = PrefPageAppearanceDisplayFormat.DISPLAY_FORMAT_CAL.equals(//
				_prefStore.getString(ITourbookPreferences.DISPLAY_FORMAT_CALORIES));

		/*
		 * Time formats
		 */
		isDrivingTime_hhmmss = PrefPageAppearanceDisplayFormat.DISPLAY_FORMAT_HH_MM_SS.equals(//
				_prefStore.getString(ITourbookPreferences.DISPLAY_FORMAT_DRIVING_TIME));

		isRecordingTime_hhmmss = PrefPageAppearanceDisplayFormat.DISPLAY_FORMAT_HH_MM_SS.equals(//
				_prefStore.getString(ITourbookPreferences.DISPLAY_FORMAT_RECORDING_TIME));
	}
}
