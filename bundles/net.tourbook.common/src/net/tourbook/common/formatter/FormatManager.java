/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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
package net.tourbook.common.formatter;

import net.tourbook.common.CommonActivator;
import net.tourbook.common.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.preferences.ICommonPreferences;

import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Centralize text formatting for different values.
 */
public class FormatManager {

	private final static IPreferenceStore	_prefStore					= CommonActivator.getPrefStore();

	private static IValueFormatter			_valueFormatter_Number_1_0	= new ValueFormatter_Number_1_0();
	private static IValueFormatter			_valueFormatter_Number_1_1	= new ValueFormatter_Number_1_1();
	private static IValueFormatter			_valueFormatter_Number_1_2	= new ValueFormatter_Number_1_2();
	private static IValueFormatter			_valueFormatter_Number_1_3	= new ValueFormatter_Number_1_3();
	private static IValueFormatter			_valueFormatter_Time_HH		= new ValueFormatter_Time_HH();
	private static IValueFormatter			_valueFormatter_Time_HHMM	= new ValueFormatter_Time_HHMM();
	private static IValueFormatter			_valueFormatter_Time_HHMMSS	= new ValueFormatter_Time_HHMMSS();

	private static IValueFormatter			_altitudeFormatter;
	private static IValueFormatter			_cadenceFormatter;
	private static IValueFormatter			_distanceFormatter;
	private static IValueFormatter			_powerFormatter;
	private static IValueFormatter			_pulseFormatter;
	private static IValueFormatter			_speedFormatter;

	private static IValueFormatter			_drivingTimeFormatter;
	private static IValueFormatter			_pausedTimeFormatter;
	private static IValueFormatter			_recordingTimeFormatter;

	public static String formatAltitude(final float value) {
		return _altitudeFormatter.printDouble(value);
	}

	public static String formatCadence(final double value) {
		return _cadenceFormatter.printDouble(value);
	}

	public static String formatDistance(final double value) {
		return _distanceFormatter.printDouble(value);
	}

	public static String formatDrivingTime(final long value) {
		return _drivingTimeFormatter.printLong(value);
	}

	public static String formatNumber_0(final double value) {

		if (value == 0) {
			return UI.EMPTY_STRING;
		}

		return _valueFormatter_Number_1_0.printDouble(value);
	}

	/**
	 * @param value
	 * @return Format a number with 0 digits but with thousender markers.
	 */
	public static String formatPausedTime(final long value) {
		return _pausedTimeFormatter.printLong(value);
	}

	public static String formatPower(final double value) {
		return _powerFormatter.printDouble(value);
	}

	public static String formatPulse(final double value) {
		return _pulseFormatter.printDouble(value);
	}

	public static String formatRecordingTime(final long value) {
		return _recordingTimeFormatter.printLong(value);
	}

	public static String formatSpeed(final double value) {
		return _speedFormatter.printDouble(value);
	}

	private static IValueFormatter getNumberFormatter(final String formatName) {

		if (formatName.equals(ValueFormat.NUMBER_1_0.name())) {

			return _valueFormatter_Number_1_0;

		} else if (formatName.equals(ValueFormat.NUMBER_1_1.name())) {

			return _valueFormatter_Number_1_1;

		} else if (formatName.equals(ValueFormat.NUMBER_1_2.name())) {

			return _valueFormatter_Number_1_2;

		} else if (formatName.equals(ValueFormat.NUMBER_1_3.name())) {

			return _valueFormatter_Number_1_3;

		} else {

			return _valueFormatter_Number_1_0;
		}
	}

	private static IValueFormatter getTimeFormatter(final String formatName) {

		if (formatName.equals(ValueFormat.TIME_HH.name())) {

			return _valueFormatter_Time_HH;

		} else if (formatName.equals(ValueFormat.TIME_HH_MM.name())) {

			return _valueFormatter_Time_HHMM;

		} else if (formatName.equals(ValueFormat.TIME_HH_MM_SS.name())) {

			return _valueFormatter_Time_HHMMSS;

		} else {

			return _valueFormatter_Time_HHMMSS;
		}
	}

	public static String getValueFormatterName(final ValueFormat valueFormat) {

		switch (valueFormat) {

		case NUMBER_1_0:
			return Messages.Value_Formatter_Number_1_0;

		case NUMBER_1_1:
			return Messages.Value_Formatter_Number_1_1;

		case NUMBER_1_2:
			return Messages.Value_Formatter_Number_1_2;

		case NUMBER_1_3:
			return Messages.Value_Formatter_Number_1_3;

		case PACE_MM_SS:
			return Messages.Value_Formatter_Pace_MM_SS;

		case TIME_HH:
			return Messages.Value_Formatter_Time_HH;

		case TIME_HH_MM:
			return Messages.Value_Formatter_Time_HH_MM;

		case TIME_HH_MM_SS:
			return Messages.Value_Formatter_Time_HH_MM_SS;

		case DEFAULT:
		case DUMMY_VALUE:
		default:
			break;
		}

		return UI.EMPTY_STRING;
	}

	public static void updateDisplayFormats() {

		final String altitude = _prefStore.getString(ICommonPreferences.DISPLAY_FORMAT_ALTITUDE);
		final String cadence = _prefStore.getString(ICommonPreferences.DISPLAY_FORMAT_CADENCE);
		final String distance = _prefStore.getString(ICommonPreferences.DISPLAY_FORMAT_DISTANCE);
		final String power = _prefStore.getString(ICommonPreferences.DISPLAY_FORMAT_POWER);
		final String pulse = _prefStore.getString(ICommonPreferences.DISPLAY_FORMAT_PULSE);
		final String speed = _prefStore.getString(ICommonPreferences.DISPLAY_FORMAT_SPEED);

		final String drivingTime = _prefStore.getString(ICommonPreferences.DISPLAY_FORMAT_DRIVING_TIME);
		final String pausedTime = _prefStore.getString(ICommonPreferences.DISPLAY_FORMAT_PAUSED_TIME);
		final String recordingTime = _prefStore.getString(ICommonPreferences.DISPLAY_FORMAT_RECORDING_TIME);

		_altitudeFormatter = getNumberFormatter(altitude);
		_cadenceFormatter = getNumberFormatter(cadence);
		_distanceFormatter = getNumberFormatter(distance);
		_powerFormatter = getNumberFormatter(power);
		_pulseFormatter = getNumberFormatter(pulse);
		_speedFormatter = getNumberFormatter(speed);

		_drivingTimeFormatter = getTimeFormatter(drivingTime);
		_pausedTimeFormatter = getTimeFormatter(pausedTime);
		_recordingTimeFormatter = getTimeFormatter(recordingTime);
	}
}
