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
package net.tourbook.common.time;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import net.tourbook.common.CommonActivator;
import net.tourbook.common.UI;
import net.tourbook.common.preferences.ICommonPreferences;

import org.eclipse.jface.preference.IPreferenceStore;

import com.skedgo.converter.TimezoneMapper;

public class TimeZoneUtils {

	private static final String						ZERO_0				= ":0";									//$NON-NLS-1$
	private static final String						ZERO_00_00			= "+00:00";								//$NON-NLS-1$

	public static final String						TIME_ZONE_UTC		= "UTC";									//$NON-NLS-1$

	/**
	 * Cached time zone text.
	 */
	private static final TIntObjectHashMap<String>	_timeZoneOffsets	= new TIntObjectHashMap<>();
	private static final TIntObjectHashMap<String>	_dbTimeZoneOffsets	= new TIntObjectHashMap<>();

	/*
	 * Copied from java.time.LocalTime
	 */
	/** Hours per day. */
	static final int								HOURS_PER_DAY		= 24;
	/** Minutes per hour. */
	static final int								MINUTES_PER_HOUR	= 60;
	/** Minutes per day. */
	static final int								MINUTES_PER_DAY		= MINUTES_PER_HOUR * HOURS_PER_DAY;
	/** Seconds per minute. */
	static final int								SECONDS_PER_MINUTE	= 60;
	/** Seconds per hour. */
	static final int								SECONDS_PER_HOUR	= SECONDS_PER_MINUTE * MINUTES_PER_HOUR;
	/** Seconds per day. */
	static final int								SECONDS_PER_DAY		= SECONDS_PER_HOUR * HOURS_PER_DAY;

	final static IPreferenceStore					_prefStoreCommon	= CommonActivator.getPrefStore();

	private static ArrayList<TimeZoneData>			_allSortedTimeZones;

	private static boolean							_isUseTimeZone;
	private static String							_defaultTimeZoneId;
	private static int								_defaultTimeZoneOffset;

	static {

		_isUseTimeZone = _prefStoreCommon.getBoolean(ICommonPreferences.TIME_ZONE_IS_USE_TIME_ZONE);
		_defaultTimeZoneId = _prefStoreCommon.getString(ICommonPreferences.TIME_ZONE_LOCAL_ID);
		_defaultTimeZoneOffset = _prefStoreCommon.getInt(ICommonPreferences.TIME_ZONE_LOCAL_OFFSET);
	}

	/**
	 * @return Returns a list with all available time zones which are sorted by zone offset, zone id
	 *         and zone name key.
	 */
	public static ArrayList<TimeZoneData> getAllTimeZones() {

		if (_allSortedTimeZones == null) {

			final ArrayList<TimeZoneData> sortedTimeZones = new ArrayList<>();

			for (final String rawZoneId : ZoneId.getAvailableZoneIds()) {

				final ZoneId zoneId = ZoneId.of(rawZoneId);
				final ZonedDateTime zonedDateTime = ZonedDateTime.now(zoneId);

				final ZoneOffset zoneOffset = zonedDateTime.getOffset();
				final int utcTimeZoneSeconds = zoneOffset.getTotalSeconds();

				final String label = printOffset(utcTimeZoneSeconds) + UI.SPACE4 + zoneId.getId();

				final TimeZoneData timeZone = new TimeZoneData();

				timeZone.label = label;
				timeZone.zoneId = zoneId.getId();
				timeZone.zoneOffsetSeconds = utcTimeZoneSeconds;

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
	 * @param timeZoneId
	 * @return Returns the timezone for the ID or <code>null</code> when not available.
	 */
	public static TimeZoneData getTimeZone(final String timeZoneId) {

		final ArrayList<TimeZoneData> allTimeZones = getAllTimeZones();

		for (final TimeZoneData timeZone : allTimeZones) {

			if (timeZone.zoneId.equals(timeZoneId)) {
				return timeZone;
			}
		}

		return null;
	}

	/**
	 * @param latitude
	 * @param longitude
	 * @return Returns the time zone index in {@link #getAllTimeZones()}, when latitude is
	 *         {@link Double#MIN_VALUE} then the time zone index for the default time zone is
	 *         returned.
	 */
	public static int getTimeZoneIndex(final double latitude, final double longitude) {

		TimeZoneData timeZone = null;

		if (latitude != Double.MIN_VALUE) {

			final String timeZoneIdFromLatLon = TimezoneMapper.latLngToTimezoneString(latitude, longitude);
			final TimeZoneData timeZoneFromLatLon = TimeZoneUtils.getTimeZone(timeZoneIdFromLatLon);

			timeZone = timeZoneFromLatLon;
		}

		if (timeZone == null) {
			// use default
			timeZone = TimeZoneUtils.getTimeZone(_defaultTimeZoneId);
		}

		return getTimeZoneIndex(timeZone.zoneId);
	}

	/**
	 * @param timeZoneId
	 * @return Returns the timezone index for the timezone ID or -1 when not available.
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
	 * @param dbStartTimeMilli
	 * @param dbTimeZoneOffset
	 *            Time zone offset in seconds or {@link Integer#MIN_VALUE} when the time zone offset
	 *            is not defined then the local time zone is used.
	 * @return Returns the text for the time zone, adds a * when default time zone is used.
	 */
	public static String getTimeZoneOffsetTextFromDbValue(final int dbTimeZoneOffset) {

		String tzText = _dbTimeZoneOffsets.get(dbTimeZoneOffset);

		if (tzText == null) {

			if (_isUseTimeZone) {

				boolean isDefaultZone = false;
				int tourTimeOffset;

				if (dbTimeZoneOffset == Integer.MIN_VALUE) {

					// timezone is not set, use default

					isDefaultZone = true;

					tourTimeOffset = _defaultTimeZoneOffset;

				} else {

					tourTimeOffset = dbTimeZoneOffset;
				}

				final int tour2defaultOffset = _defaultTimeZoneOffset - tourTimeOffset;

				String tzOffset = printOffset(tour2defaultOffset);

				if (isDefaultZone) {
					// mark the default zone with a star
					tzOffset += UI.SYMBOL_STAR;
				}

				tzText = tzOffset;

				_dbTimeZoneOffsets.put(dbTimeZoneOffset, tzText);

			} else {

				tzText = "no TZ";
			}
		}

		return tzText;
	}

	/**
	 * Adjust tour time with the time zone offset.
	 * 
	 * @param epochMilli
	 *            The number of milliseconds from 1970-01-01T00:00:00Z
	 * @param dbTimeZoneOffset
	 *            Time zone offset in seconds or {@link Integer#MIN_VALUE} when the time zone offset
	 *            is not defined then the local time zone is used.
	 * @return
	 */
	public static ZonedDateTime getTourTime(final long epochMilli, final int dbTimeZoneOffset) {

		final Instant tourStartInstant = Instant.ofEpochMilli(epochMilli);

		final int tourUtcTimeOffset = getTourUTCTimeOffset(dbTimeZoneOffset);

		final ZoneOffset zoneOffset = ZoneOffset.ofTotalSeconds(tourUtcTimeOffset);
		final ZoneId zoneId = ZoneId.ofOffset(TIME_ZONE_UTC, zoneOffset);

		final ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(tourStartInstant, zoneId);

		return zonedDateTime;
	}

	/**
	 * @param dbTimeZoneOffset
	 *            Time zone offset in seconds or {@link Integer#MIN_VALUE} when the time zone offset
	 *            is not defined then the local time zone is used.
	 * @return
	 */
	private static int getTourUTCTimeOffset(final int dbTimeZoneOffset) {

		int tourUTCTimeOffset = _defaultTimeZoneOffset;

		if (dbTimeZoneOffset == Integer.MIN_VALUE) {

			// timezone is not set, use original time without offset

		} else if (_isUseTimeZone) {

			// tour has a UTC time zone offset

			tourUTCTimeOffset = dbTimeZoneOffset;
		}

		return tourUTCTimeOffset;
	}

	/*
	 * Copied (and modified) from java.time.ZoneOffset.buildId(int)
	 */
	/**
	 * @param timeZoneOffset
	 *            Time zone offset in seconds.
	 * @return
	 */
	private static String printOffset(final int timeZoneOffset) {

		if (timeZoneOffset == 0) {

			return ZERO_00_00;

		} else {

			String tzText = _timeZoneOffsets.get(timeZoneOffset);

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

				_timeZoneOffsets.put(timeZoneOffset, tzText);
			}

			return tzText;
		}
	}

	public static void setDefaultTimeZoneOffset(final boolean isUseTimeZone,
												final String timeZoneId,
												final int timeZoneOffset) {

		_isUseTimeZone = isUseTimeZone;
		_defaultTimeZoneId = timeZoneId;
		_defaultTimeZoneOffset = timeZoneOffset;

		_dbTimeZoneOffsets.clear();

		System.out.println((UI.timeStampNano() + " [TimeZoneUtils] ") + ("\ttimeZoneOffset: " + timeZoneOffset));
		// TODO remove SYSTEM.OUT.PRINTLN

	}

}
