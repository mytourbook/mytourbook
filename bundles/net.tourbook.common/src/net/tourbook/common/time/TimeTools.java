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
package net.tourbook.common.time;

import static java.time.temporal.ChronoField.*;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.ValueRange;
import java.time.temporal.WeekFields;
import java.time.zone.ZoneRules;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import net.tourbook.common.CommonActivator;
import net.tourbook.common.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.preferences.ICommonPreferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import com.skedgo.converter.TimezoneMapper;

public class TimeTools {

	private static final String						ZERO_0					= ":0";										//$NON-NLS-1$
	private static final String						ZERO_00_00				= "+00:00";									//$NON-NLS-1$
	private static final String						ZERO_00_00_DEFAULT		= "*";										//$NON-NLS-1$

	/**
	 * Cached time zone labels.
	 */
	private static final TIntObjectHashMap<String>	_timeZoneOffsetLabels	= new TIntObjectHashMap<>();

	/** Minutes per hour. */
	private static final int						MINUTES_PER_HOUR		= 60;
	/** Seconds per minute. */
	private static final int						SECONDS_PER_MINUTE		= 60;
	/** Seconds per hour. */
	private static final int						SECONDS_PER_HOUR		= SECONDS_PER_MINUTE * MINUTES_PER_HOUR;

	private static final PeriodFormatter			DURATION_FORMATTER;

	public static final ZoneId						UTC						= ZoneId.of("UTC");							//$NON-NLS-1$

	/**
	 * Calendar week which is defined in the preferences and applied in the whole app.
	 */
	public static WeekFields						calendarWeek;

	/**
	 * Contains the short weekday strings. For example: "Sun", "Mon", etc.
	 */
	public static String[]							weekDays_Short;

	/**
	 * Contails the full text, typically the full description. For example, day-of-week Monday might
	 * output "Monday".
	 */
	public static String[]							weekDays_Full;

// SET_FORMATTING_OFF
		
	public static final DateTimeFormatter	Formatter_Date_S		= DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);
	public static final DateTimeFormatter	Formatter_Date_M		= DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
	public static final DateTimeFormatter	Formatter_Date_L		= DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG);
	public static final DateTimeFormatter	Formatter_Date_F		= DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL);
	
	public static final DateTimeFormatter	Formatter_Time_S		= DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT);
	public static final DateTimeFormatter	Formatter_Time_M		= DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM);
	public static final DateTimeFormatter	Formatter_Time_F		= DateTimeFormatter.ofLocalizedTime(FormatStyle.FULL);
	
	public static final DateTimeFormatter	Formatter_DateTime_S	= DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT);
	public static final DateTimeFormatter	Formatter_DateTime_SM	= DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT,FormatStyle.MEDIUM);
	public static final DateTimeFormatter	Formatter_DateTime_M	= DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);
	public static final DateTimeFormatter	Formatter_DateTime_MS	= DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT);
	public static final DateTimeFormatter	Formatter_DateTime_ML	= DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.LONG);
	public static final DateTimeFormatter	Formatter_DateTime_F	= DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL);
	
	
	public static final DateTimeFormatter	Formatter_FileName		= DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");		//$NON-NLS-1$
	
	public static final DateTimeFormatter	Formatter_Day			= DateTimeFormatter.ofPattern("dd");
	public static final DateTimeFormatter	Formatter_DayMonth		= DateTimeFormatter.ofPattern("d MMM");
	public static final DateTimeFormatter	Formatter_DayMonthYear	= DateTimeFormatter.ofPattern("d MMM uu");
	public static final DateTimeFormatter	Formatter_Month			= DateTimeFormatter.ofPattern("MMM");						//$NON-NLS-1$
	public static final DateTimeFormatter	Formatter_Week_Month	= DateTimeFormatter.ofPattern("dd MMM");					//$NON-NLS-1$
	public static final DateTimeFormatter	Formatter_Weekday		= DateTimeFormatter.ofPattern("E");							//$NON-NLS-1$
	public static final DateTimeFormatter	Formatter_Weekday_L		= DateTimeFormatter.ofPattern("EEEE");						//$NON-NLS-1$

	
// SET_FORMATTING_ON

	public static final DateTimeFormatter			Formatter_Time_ISO;


	private final static IPreferenceStore			_prefStoreCommon		= CommonActivator.getPrefStore();

	private static ArrayList<TimeZoneData>			_allSortedTimeZones;

	/**
	 * Default time zone ID which is set in the preferences.
	 */
	private static ZoneId							_defaultTimeZoneId;

	/**
	 * The date must not be in the first or last week of the year.
	 */
	private static LocalDate						_dateToGetNumOfWeeks	= LocalDate.of(2000, 5, 5);
	static {

		Formatter_Time_ISO = new DateTimeFormatterBuilder()//

				.appendValue(ChronoField.HOUR_OF_DAY, 2)
				.appendLiteral(':')

				.appendValue(ChronoField.MINUTE_OF_HOUR, 2)
				.appendLiteral(':')

				.appendValue(ChronoField.SECOND_OF_MINUTE, 2)
				.toFormatter();

		DURATION_FORMATTER = new PeriodFormatterBuilder()
				//
				.appendHours()
				.appendSuffix(Messages.Period_Format_Hour_Short, Messages.Period_Format_Hour_Short)

				.appendMinutes()
				.appendSuffix(Messages.Period_Format_Minute_Short, Messages.Period_Format_Minute_Short)

				.toFormatter();

		_defaultTimeZoneId = ZoneId.of(_prefStoreCommon.getString(ICommonPreferences.TIME_ZONE_LOCAL_ID));

		/*
		 * Set calendar week
		 */
		final int firstDayOfWeek = _prefStoreCommon.getInt(ICommonPreferences.CALENDAR_WEEK_FIRST_DAY_OF_WEEK);
		final int minDaysInFirstWeek = _prefStoreCommon.getInt(ICommonPreferences.CALENDAR_WEEK_MIN_DAYS_IN_FIRST_WEEK);

		setCalendarWeek(firstDayOfWeek, minDaysInFirstWeek);

		/*
		 * Create week day names. Found no better solution, the old API contained
		 * "DateFormatSymbols.getInstance().getShortWeekdays()"
		 */
		final DateTimeFormatter weekDayFormatter_Short = new DateTimeFormatterBuilder()//
				.appendText(DAY_OF_WEEK, TextStyle.SHORT)
				.toFormatter();

		final DateTimeFormatter weekDayFormatter_Full = new DateTimeFormatterBuilder()//
				.appendText(DAY_OF_WEEK, TextStyle.FULL)
				.toFormatter();

		weekDays_Short = new String[] {

				weekDayFormatter_Short.format(DayOfWeek.MONDAY),
				weekDayFormatter_Short.format(DayOfWeek.TUESDAY),
				weekDayFormatter_Short.format(DayOfWeek.WEDNESDAY),
				weekDayFormatter_Short.format(DayOfWeek.THURSDAY),
				weekDayFormatter_Short.format(DayOfWeek.FRIDAY),
				weekDayFormatter_Short.format(DayOfWeek.SATURDAY),
				weekDayFormatter_Short.format(DayOfWeek.SUNDAY) //
		};

		weekDays_Full = new String[] {

				weekDayFormatter_Full.format(DayOfWeek.MONDAY),
				weekDayFormatter_Full.format(DayOfWeek.TUESDAY),
				weekDayFormatter_Full.format(DayOfWeek.WEDNESDAY),
				weekDayFormatter_Full.format(DayOfWeek.THURSDAY),
				weekDayFormatter_Full.format(DayOfWeek.FRIDAY),
				weekDayFormatter_Full.format(DayOfWeek.SATURDAY),
				weekDayFormatter_Full.format(DayOfWeek.SUNDAY) //
		};
	}

	/**
	 * Creates a tour date time with the tour time zone.
	 * 
	 * @param epochMilli
	 *            The number of milliseconds from 1970-01-01T00:00:00Z
	 * @param dbTimeZoneId
	 *            Time zone ID or <code>null</code> when the time zone ID is not defined, then the
	 *            local time zone is used.
	 * @return
	 */
	public static TourDateTime createTourDateTime(final long epochMilli, final String dbTimeZoneId) {

		final Instant tourStartInstant = Instant.ofEpochMilli(epochMilli);

		final boolean isDefaultZone = dbTimeZoneId == null;
		final boolean isTourTimeZone = dbTimeZoneId != null;

		final ZoneId zoneId = isDefaultZone //
				? _defaultTimeZoneId
				: ZoneId.of(dbTimeZoneId);

		ZonedDateTime tourZonedDateTime;
		String timeZoneOffsetLabel = null;

		if (isTourTimeZone) {

			// use tour time zone

			tourZonedDateTime = ZonedDateTime.ofInstant(tourStartInstant, zoneId);

			final ZonedDateTime tourDateTimeWithDefaultZoneId = tourZonedDateTime
					.withZoneSameInstant(_defaultTimeZoneId);

			final int tourOffset = tourZonedDateTime.getOffset().getTotalSeconds();
			final int defaultOffset = tourDateTimeWithDefaultZoneId.getOffset().getTotalSeconds();

			final int offsetDiff = tourOffset - defaultOffset;
			timeZoneOffsetLabel = printOffset(offsetDiff, isDefaultZone);

		} else {

			tourZonedDateTime = ZonedDateTime.ofInstant(tourStartInstant, zoneId);

			timeZoneOffsetLabel = printOffset(0, true);
		}

		final int weekDayIndex = tourZonedDateTime.getDayOfWeek().getValue()
				// use an offset to have the index in the week array
				- 1;

		return new TourDateTime(tourZonedDateTime, timeZoneOffsetLabel, weekDays_Short[weekDayIndex]);
	}

	/**
	 * @return Returns a list with all available time zones which are sorted by zone offset, zone id
	 *         and zone name key.
	 */
	public static ArrayList<TimeZoneData> getAllTimeZones() {

		if (_allSortedTimeZones == null) {

			final int currentYear = LocalDate.now().getYear();

			final ArrayList<TimeZoneData> sortedTimeZones = new ArrayList<>();

			for (final String rawZoneId : ZoneId.getAvailableZoneIds()) {

				final ZoneId zoneId = ZoneId.of(rawZoneId);
				final ZoneRules zoneRules = zoneId.getRules();

				final ZonedDateTime zonedDateTimeWinter = ZonedDateTime.of(currentYear, 1, 1, 12, 0, 0, 0, zoneId);
				final ZonedDateTime zonedDateTimeSummer = ZonedDateTime.of(currentYear, 6, 1, 12, 0, 0, 0, zoneId);

				final ZoneOffset zoneOffsetWinter = zonedDateTimeWinter.getOffset();
				final ZoneOffset zoneOffsetSummer = zonedDateTimeSummer.getOffset();
				final int utcTimeZoneSecondsWinter = zoneOffsetWinter.getTotalSeconds();
				final int utcTimeZoneSecondsSummer = zoneOffsetSummer.getTotalSeconds();

				final Instant nowInstantWinter = zonedDateTimeWinter.toInstant();
				final Instant nowInstantSummer = zonedDateTimeSummer.toInstant();

				final Boolean isDstWinter = zoneRules.isDaylightSavings(nowInstantWinter);
				final Boolean isDstSummer = zoneRules.isDaylightSavings(nowInstantSummer);

				final Duration dstDurationWinter = zoneRules.getDaylightSavings(nowInstantWinter);
				final Duration dstDurationSummer = zoneRules.getDaylightSavings(nowInstantSummer);

				final String dstSouth = NLS.bind(
						Messages.Time_Tools_DST_South,
						printDSTDuration(dstDurationWinter.getSeconds() * 1000));

				final String dstNorth = NLS.bind(
						Messages.Time_Tools_DST_North,
						printDSTDuration(dstDurationSummer.getSeconds() * 1000));

				final String dst = UI.EMPTY_STRING
						+ (isDstWinter ? dstSouth : UI.EMPTY_STRING)
						+ (isDstSummer ? dstNorth : UI.EMPTY_STRING);

				final String label = UI.EMPTY_STRING
						+ (printOffset(utcTimeZoneSecondsWinter, false) + UI.SPACE4)
						+ (printOffset(utcTimeZoneSecondsSummer, false) + UI.SPACE4)
						+ zoneId.getId()
						+ (dst.length() == 0 //
								? UI.EMPTY_STRING
								: UI.DASH_WITH_DOUBLE_SPACE + dst);

				final TimeZoneData timeZone = new TimeZoneData();

				timeZone.label = label;
				timeZone.zoneId = zoneId.getId();
				timeZone.zoneOffsetSeconds = utcTimeZoneSecondsWinter;

				sortedTimeZones.add(timeZone);
			}

			Collections.sort(sortedTimeZones, new Comparator<TimeZoneData>() {

				@Override
				public int compare(final TimeZoneData tz1, final TimeZoneData tz2) {

					int result;

					result = tz1.zoneId.compareTo(tz2.zoneId);

					if (result == 0) {
						result = tz1.zoneOffsetSeconds - tz2.zoneOffsetSeconds;
					}

					return result;
				}
			});

			_allSortedTimeZones = sortedTimeZones;
		}

		return _allSortedTimeZones;
	}

	/**
	 * @return Returns the time zone which is defined in the preferences.
	 */
	public static ZoneId getDefaultTimeZone() {

		return _defaultTimeZoneId;
	}

	/**
	 * @return Returns the time zone ID which is defined in the preferences.
	 */
	public static String getDefaultTimeZoneId() {

		return _defaultTimeZoneId.getId();
	}

	/**
	 * @return Returns the time zone offset for the default time zone.
	 */
	public static String getDefaultTimeZoneOffset() {

		final ZonedDateTime zdt = ZonedDateTime.now();
		final int tzOffset = zdt.getOffset().getTotalSeconds();

		final Period period = new Period(tzOffset * 1000);

		return period.toString(DURATION_FORMATTER);
	}

	/**
	 * @param year
	 * @return Returns the number of days in a year
	 */
	public static int getNumberOfDaysWithYear(final int year) {

		return Year.of(year).length();
	}

	/**
	 * @param year
	 * @return Returns the number of weeks in a year.
	 */
	public static int getNumberOfWeeksWithYear(final int year) {

		/*
		 * The date MUST not be in the first or last week of the year, this is very tricky to get
		 * the number of weeks in a year, found in the www.
		 */
		final LocalDate date = _dateToGetNumOfWeeks.withYear(year);
		final ValueRange range = date.range(calendarWeek.weekOfWeekBasedYear());

		final long numOfWeeks = range.getMaximum();

		return (int) numOfWeeks;
	}

	/**
	 * @param timeZoneId
	 * @return Returns the timezone for the ID or <code>null</code> when not available.
	 */
	private static TimeZoneData getTimeZone(final String timeZoneId) {

		final ArrayList<TimeZoneData> allTimeZones = getAllTimeZones();

		for (final TimeZoneData timeZone : allTimeZones) {

			if (timeZone.zoneId.equals(timeZoneId)) {
				return timeZone;
			}
		}

		return null;
	}

	public static TimeZoneData getTimeZone_ByIndex(final int selectedTimeZoneIndex) {

		final ArrayList<TimeZoneData> allTimeZone = getAllTimeZones();

		if (selectedTimeZoneIndex == -1) {
			return allTimeZone.get(0);
		}

		final TimeZoneData selectedTimeZone = allTimeZone.get(selectedTimeZoneIndex);

		return selectedTimeZone;
	}

	/**
	 * @param latitude
	 * @param longitude
	 * @return Returns the time zone index in {@link #getAllTimeZones()}, when latitude is
	 *         {@link Double#MIN_VALUE} then the time zone index for the default time zone is
	 *         returned.
	 */
	public static int getTimeZoneIndex(final double latitude, final double longitude) {

		TimeZoneData timeZoneData = null;

		if (latitude != Double.MIN_VALUE) {

			final String timeZoneIdFromLatLon = TimezoneMapper.latLngToTimezoneString(latitude, longitude);
			final TimeZoneData timeZoneFromLatLon = getTimeZone(timeZoneIdFromLatLon);

			timeZoneData = timeZoneFromLatLon;
		}

		if (timeZoneData == null) {

			// use default
			timeZoneData = getTimeZone(_defaultTimeZoneId.getId());
		}

		return getTimeZoneIndex(timeZoneData.zoneId);
	}

	/**
	 * @param timeZoneId
	 * @return Returns the time zone index for the time zone ID or -1 when not available.
	 */
	public static int getTimeZoneIndex(final String timeZoneId) {

		final ArrayList<TimeZoneData> allTimeZones = getAllTimeZones();

		for (int timeZoneIndex = 0; timeZoneIndex < allTimeZones.size(); timeZoneIndex++) {

			final TimeZoneData timeZone = allTimeZones.get(timeZoneIndex);

			if (timeZone.zoneId.equals(timeZoneId)) {
				return timeZoneIndex;
			}
		}

		return -1;
	}

	/**
	 * @return Returns the time zone index for the default time zone.
	 */
	public static int getTimeZoneIndex_Default() {
		return getTimeZoneIndex(_defaultTimeZoneId.getId());
	}

	/**
	 * @param timeZoneId
	 * @return Returns the time zone index for the time zone ID or the index for the default time
	 *         zone when not available.
	 */
	public static int getTimeZoneIndex_WithDefault(final String timeZoneId) {

		int tzIndex = getTimeZoneIndex(timeZoneId);

		if (tzIndex == -1) {
			tzIndex = getTimeZoneIndex(_defaultTimeZoneId.getId());
		}

		return tzIndex;
	}

	/**
	 * @param epochOfMilli
	 *            The number of milliseconds from 1970-01-01T00:00:00Z
	 * @return Returns a zoned date time from epochOfMilli with the default time zone.
	 */
	public static ZonedDateTime getZonedDateTime(final long epochOfMilli) {

		return ZonedDateTime.ofInstant(//
				Instant.ofEpochMilli(epochOfMilli),
				getDefaultTimeZone());
	}

	/**
	 * @param epochOfMilli
	 *            The number of milliseconds from 1970-01-01T00:00:00Z
	 * @return Returns a zoned date time from epochOfMilli with the UTC time zone.
	 */
	public static ZonedDateTime getZonedDateTimeWithUTC(final long epochOfMilli) {

		return ZonedDateTime.ofInstant(//
				Instant.ofEpochMilli(epochOfMilli),
				ZoneOffset.UTC);
	}

	/**
	 * @return Return now with the default time zone.
	 */
	public static ZonedDateTime now() {

		return ZonedDateTime.now(getDefaultTimeZone());
	}

	/**
	 * @param duration
	 *            in milliseconds.
	 * @return
	 */
	public static String printDSTDuration(final long duration) {

		final Period period = new Period(duration);

		return period.toString(DURATION_FORMATTER);
	}

	/*
	 * Copied (and modified) from java.time.ZoneOffset.buildId(int)
	 */
	/**
	 * @param timeZoneOffset
	 *            Time zone offset in seconds
	 * @param isDefaultZone
	 *            When <code>true</code>, then a star is added to the offset value to indicate the
	 *            default zone
	 * @return Returns a time offset string
	 */
	private static String printOffset(final int timeZoneOffset, final boolean isDefaultZone) {

		if (timeZoneOffset == 0) {

			if (isDefaultZone) {

				return ZERO_00_00_DEFAULT;

			} else {

				return ZERO_00_00;
			}

		} else {

			String tzText = _timeZoneOffsetLabels.get(timeZoneOffset);

			if (tzText == null) {

				// create text

				final int absTotalSeconds = Math.abs(timeZoneOffset);
				final int absHours = absTotalSeconds / SECONDS_PER_HOUR;
				final int absMinutes = (absTotalSeconds / SECONDS_PER_MINUTE) % MINUTES_PER_HOUR;

				final StringBuilder sb = new StringBuilder()
						.append(timeZoneOffset < 0 ? '-' : '+')
						.append(absHours < 10 ? '0' : UI.EMPTY_STRING)
						.append(absHours)
						.append(absMinutes < 10 ? ZERO_0 : ':')
						.append(absMinutes);

				final int absSeconds = absTotalSeconds % SECONDS_PER_MINUTE;

				if (absSeconds != 0) {
					sb.append(absSeconds < 10 ? ZERO_0 : ':').append(absSeconds);
				}

				tzText = sb.toString();

				if (isDefaultZone) {
					// mark the default zone with a star
					tzText += UI.SYMBOL_STAR;
				}

				_timeZoneOffsetLabels.put(timeZoneOffset, tzText);
			}

			return tzText;
		}
	}

	/**
	 * Define when a calendar week starts for the whole app.
	 * 
	 * @param firstDayOfWeek
	 * @param minimalDaysInFirstWeek
	 */
	public static void setCalendarWeek(final int firstDayOfWeek, final int minimalDaysInFirstWeek) {

		final DayOfWeek dow = DayOfWeek.SUNDAY.plus(firstDayOfWeek);

		calendarWeek = WeekFields.of(dow, minimalDaysInFirstWeek);
	}

	public static void setDefaultTimeZone(final String selectedTimeZoneId) {

		_defaultTimeZoneId = ZoneId.of(selectedTimeZoneId);
	}

	/**
	 * Converts a {@link LocalDateTime} to the number of milliseconds from the epoch of
	 * 1970-01-01T00:00:00Z.
	 * 
	 * @param localDateTime
	 * @return
	 */
	public static long toEpochMilli(final LocalDateTime localDateTime) {

		return localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
	}

	public static long toEpochMilli(final ZonedDateTime zonedDateTime) {

		return zonedDateTime.toInstant().toEpochMilli();
	}

	/**
	 * @param epochOfMilli
	 *            The number of milliseconds from 1970-01-01T00:00:00Z
	 * @return Returns a zoned date time from epochOfMilli with the default time zone.
	 */
	public static LocalDateTime toLocalDateTime(final long epochOfMilli) {

		return LocalDateTime.ofInstant(//
				Instant.ofEpochMilli(epochOfMilli),
				ZoneOffset.UTC);
	}

}
