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

   private final static IPreferenceStore _prefStore                  = CommonActivator.getPrefStore();

   private static IValueFormatter        _valueFormatter_Number_1_0  = new ValueFormatter_Number_1_0();
   private static IValueFormatter        _valueFormatter_Number_1_1  = new ValueFormatter_Number_1_1();
   private static IValueFormatter        _valueFormatter_Number_1_2  = new ValueFormatter_Number_1_2();
   private static IValueFormatter        _valueFormatter_Number_1_3  = new ValueFormatter_Number_1_3();
   private static IValueFormatter        _valueFormatter_Time_HH     = new ValueFormatter_Time_HH();
   private static IValueFormatter        _valueFormatter_Time_HHMM   = new ValueFormatter_Time_HHMM();
   private static IValueFormatter        _valueFormatter_Time_HHMMSS = new ValueFormatter_Time_HHMMSS();
   private static IValueFormatter        _valueFormatter_Time_SSS    = new ValueFormatter_Time_SSS();

   private static IValueFormatter        _altitudeFormatter;
   private static IValueFormatter        _cadenceFormatter;
   private static IValueFormatter        _distanceFormatter;
   private static IValueFormatter        _powerFormatter;
   private static IValueFormatter        _pulseFormatter;
   private static IValueFormatter        _speedFormatter;

   private static IValueFormatter        _elapsedTimeFormatter;
   private static IValueFormatter        _recordedTimeFormatter;
   private static IValueFormatter        _pausedTimeFormatter;
   private static IValueFormatter        _movingTimeFormatter;
   private static IValueFormatter        _breakTimeFormatter;

   public static String formatAltitude(final float value) {
      return _altitudeFormatter.printDouble(value);
   }

   public static String formatBreakTime(final long value) {
      return _breakTimeFormatter.printLong(value);
   }

   public static String formatCadence(final double value) {
      return _cadenceFormatter.printDouble(value);
   }

   public static String formatDistance(final double value) {
      return _distanceFormatter.printDouble(value);
   }

   public static String formatElapsedTime(final long value) {
      return _elapsedTimeFormatter.printLong(value);
   }

   public static String formatMovingTime(final long value) {
      return _movingTimeFormatter.printLong(value);
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

   public static String formatRecordedTime(final long value) {
      return _recordedTimeFormatter.printLong(value);
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

      } else if (formatName.equals(ValueFormat.TIME_SSS.name())) {

         return _valueFormatter_Time_SSS;

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

      case TIME_SSS:
         return Messages.Value_Formatter_Time_SSS;

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

      final String elapsedTime = _prefStore.getString(ICommonPreferences.DISPLAY_FORMAT_ELAPSED_TIME);
      final String recordedTime = _prefStore.getString(ICommonPreferences.DISPLAY_FORMAT_RECORDED_TIME);
      final String pausedTime = _prefStore.getString(ICommonPreferences.DISPLAY_FORMAT_PAUSED_TIME);
      final String movingTime = _prefStore.getString(ICommonPreferences.DISPLAY_FORMAT_MOVING_TIME);
      final String breakTime = _prefStore.getString(ICommonPreferences.DISPLAY_FORMAT_BREAK_TIME);

      _altitudeFormatter = getNumberFormatter(altitude);
      _cadenceFormatter = getNumberFormatter(cadence);
      _distanceFormatter = getNumberFormatter(distance);
      _powerFormatter = getNumberFormatter(power);
      _pulseFormatter = getNumberFormatter(pulse);
      _speedFormatter = getNumberFormatter(speed);

      _elapsedTimeFormatter = getTimeFormatter(elapsedTime);
      _recordedTimeFormatter = getTimeFormatter(recordedTime);
      _pausedTimeFormatter = getTimeFormatter(pausedTime);
      _movingTimeFormatter = getTimeFormatter(movingTime);
      _breakTimeFormatter = getTimeFormatter(breakTime);
   }
}
