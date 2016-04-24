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

import java.text.NumberFormat;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Centralize text formatting for different values.
 */
public class FormatManager {

	public static final String				DISPLAY_FORMAT_1		= "1";								//$NON-NLS-1$
	public static final String				DISPLAY_FORMAT_1_1		= "1.1";							//$NON-NLS-1$
	public static final String				DISPLAY_FORMAT_1_2		= "1.2";							//$NON-NLS-1$
	public static final String				DISPLAY_FORMAT_CAL		= "cal";							//$NON-NLS-1$
	public static final String				DISPLAY_FORMAT_KCAL		= "kcal";							//$NON-NLS-1$
	public static final String				DISPLAY_FORMAT_HH_MM	= "hh_mm";							//$NON-NLS-1$
	public static final String				DISPLAY_FORMAT_HH_MM_SS	= "hh_mm_ss";						//$NON-NLS-1$

	private final static IPreferenceStore	_prefStore				= TourbookPlugin.getPrefStore();

	/** When <code>true</code> then avg cadence format is #.#, default is # */
	private static boolean					_isAvgCadence_1_1;

	/** When <code>true</code> then avg cadence format is #.##, default is # */
	private static boolean					_isAvgCadence_1_2;

	/** When <code>true</code> then avg power format is #.#, default is # */
	private static boolean					_isAvgPower_1_1;

	/** When <code>true</code> then avg pulse format is #.#, default is # */
	private static boolean					_isAvgPulse_1_1;

	/** When <code>true</code> then calories is <i>cal</i>, default is <i>kcal</i>. */
	private static boolean					_isCalories_cal;

	/** When <code>true</code> then driving time format is <i>hh:mm:ss</i>, default is <i>hh:mm</i>. */
	private static boolean					_isDrivingTime_hhmmss;

	/** When <code>true</code> then paused time format is <i>hh:mm:ss</i>, default is <i>hh:mm</i>. */
	private static boolean					_isPausedTime_hhmmss;

	/**
	 * When <code>true</code> then recording time format is <i>hh:mm:ss</i>, default is
	 * <i>hh:mm</i>.
	 */
	private static boolean					_isRecordingTime_hhmmss;

	private final static NumberFormat		_nf0					= NumberFormat.getNumberInstance();
	private final static NumberFormat		_nf1					= NumberFormat.getNumberInstance();
	private final static NumberFormat		_nf2					= NumberFormat.getNumberInstance();
	private final static NumberFormat		_nf3					= NumberFormat.getNumberInstance();

	static {

		_nf0.setMinimumFractionDigits(0);
		_nf0.setMaximumFractionDigits(0);

		_nf1.setMinimumFractionDigits(1);
		_nf1.setMaximumFractionDigits(1);

		_nf2.setMinimumFractionDigits(2);
		_nf2.setMaximumFractionDigits(2);

		_nf3.setMinimumFractionDigits(3);
		_nf3.setMaximumFractionDigits(3);
	}

	public static String getAvgCadence(final float avgCadence) {

		return _isAvgCadence_1_2 //

				? _nf2.format(avgCadence)
				: _isAvgCadence_1_1 //

						? _nf1.format(avgCadence)
						: _nf0.format(avgCadence);
	}

	public static String getAvgPower(final float avgPower) {

		return _isAvgPower_1_1 //

				? _nf1.format(avgPower)
				: _nf0.format(avgPower);

	}

	public static String getAvgPulse(final float avgPulse) {

		return _isAvgPulse_1_1//

				? _nf1.format(avgPulse)
				: _nf0.format(avgPulse);

	}

	public static String getCalories(final long calories) {

		return _isCalories_cal//

				? _nf0.format(calories)
				: _nf1.format((double) calories / 1000);
	}

	public static String getCaloriesUnit() {

		return _isCalories_cal ? Messages.Value_Unit_Calories : Messages.Value_Unit_KCalories;

	}

	public static String getDrivingTime(final long time) {

		if (_isDrivingTime_hhmmss) {
			return net.tourbook.common.UI.format_hh_mm_ss(time);
		} else {
			return net.tourbook.common.UI.format_hh_mm(time + 30);
		}
	}

	public static String getPauseTime(final long time) {

		if (_isPausedTime_hhmmss) {
			return net.tourbook.common.UI.format_hh_mm_ss(time);
		} else {
			return net.tourbook.common.UI.format_hh_mm(time + 30);
		}
	}

	public static String getRecordingTime(final long time) {

		if (_isRecordingTime_hhmmss) {
			return net.tourbook.common.UI.format_hh_mm_ss(time);
		} else {
			return net.tourbook.common.UI.format_hh_mm(time + 30);
		}
	}

	public static void updateDisplayFormats() {

		/*
		 * Cadence
		 */
		final String cadence = _prefStore.getString(ITourbookPreferences.DISPLAY_FORMAT_AVG_CADENCE);
		_isAvgCadence_1_1 = DISPLAY_FORMAT_1_1.equals(cadence);
		_isAvgCadence_1_2 = DISPLAY_FORMAT_1_2.equals(cadence);

		/*
		 * Power
		 */
		final String power = _prefStore.getString(ITourbookPreferences.DISPLAY_FORMAT_AVG_POWER);
		_isAvgPower_1_1 = DISPLAY_FORMAT_1_1.equals(power);

		/*
		 * Pulse
		 */
		final String pulse = _prefStore.getString(ITourbookPreferences.DISPLAY_FORMAT_AVG_PULSE);
		_isAvgPulse_1_1 = DISPLAY_FORMAT_1_1.equals(pulse);

		/*
		 * Calories
		 */
		_isCalories_cal = DISPLAY_FORMAT_CAL.equals(//
				_prefStore.getString(ITourbookPreferences.DISPLAY_FORMAT_CALORIES));

		/*
		 * Time formats
		 */
		_isDrivingTime_hhmmss = DISPLAY_FORMAT_HH_MM_SS.equals(//
				_prefStore.getString(ITourbookPreferences.DISPLAY_FORMAT_DRIVING_TIME));

		_isPausedTime_hhmmss = DISPLAY_FORMAT_HH_MM_SS.equals(//
				_prefStore.getString(ITourbookPreferences.DISPLAY_FORMAT_PAUSED_TIME));

		_isRecordingTime_hhmmss = DISPLAY_FORMAT_HH_MM_SS.equals(//
				_prefStore.getString(ITourbookPreferences.DISPLAY_FORMAT_RECORDING_TIME));
	}
}
