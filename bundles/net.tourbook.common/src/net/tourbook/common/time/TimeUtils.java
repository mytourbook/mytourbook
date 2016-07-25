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

import java.util.ArrayList;

import net.tourbook.common.UI;

import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.format.FormatUtils;

public class TimeUtils {

	private static ArrayList<TimeZone>	_allSortedTimeZones;

	/**
	 * @return Returns a list with all available time zones which are sorted by zone offset, zone id
	 *         and zone name key.
	 */
	public static ArrayList<TimeZone> getAllTimeZones() {

		if (_allSortedTimeZones == null) {

			final ArrayList<TimeZone> sortedTimeZones = new ArrayList<>();

			for (final String dtZoneId : DateTimeZone.getAvailableIDs()) {

				final DateTimeZone dtZone = DateTimeZone.forID(dtZoneId);

				final int dtZoneOffset = dtZone.getOffset(0);
				final String dtZoneNameKey = dtZone.getNameKey(0);

				final String label = TimeUtils.printOffset(dtZoneOffset) + UI.SPACE4 + dtZoneId;

				final TimeZone timeZone = new TimeZone();

				timeZone.label = label;

				timeZone.zoneId = dtZoneId;
				timeZone.zoneOffset = dtZoneOffset;
				timeZone.zoneNameKey = dtZoneNameKey;

				sortedTimeZones.add(timeZone);
			}

//		Collections.sort(sortedTimeZones, new Comparator<TimeZone>() {
//
//			@Override
//			public int compare(final TimeZone tz1, final TimeZone tz2) {
//
//				int result = tz1.zoneOffset - tz2.zoneOffset;
//
//				if (result == 0) {
//					result = tz1.zoneId.compareTo(tz2.zoneId);
//				}
//
//				if (result == 0) {
//					result = tz1.zoneNameKey.compareTo(tz2.zoneNameKey);
//				}
//
//				return result;
//			}
//		});

			_allSortedTimeZones = sortedTimeZones;
		}

		return _allSortedTimeZones;
	}

	/**
	 * Formats a timezone offset string.
	 * <p>
	 * This method is kept separate from the formatting classes to speed and simplify startup and
	 * classloading.
	 * 
	 * @param offset
	 *            the offset in milliseconds
	 * @return the time zone string
	 */
	public static String printOffset(int offset) {

		/*
		 * Copied from org.joda.time.DateTimeZone
		 */

		final StringBuffer buf = new StringBuffer();
		if (offset >= 0) {
			buf.append('+');
		} else {
			buf.append('-');
			offset = -offset;
		}

		final int hours = offset / DateTimeConstants.MILLIS_PER_HOUR;
		FormatUtils.appendPaddedInteger(buf, hours, 2);
		offset -= hours * DateTimeConstants.MILLIS_PER_HOUR;

		final int minutes = offset / DateTimeConstants.MILLIS_PER_MINUTE;
		buf.append(':');
		FormatUtils.appendPaddedInteger(buf, minutes, 2);
		offset -= minutes * DateTimeConstants.MILLIS_PER_MINUTE;
		if (offset == 0) {
			return buf.toString();
		}

		final int seconds = offset / DateTimeConstants.MILLIS_PER_SECOND;
		buf.append(':');
		FormatUtils.appendPaddedInteger(buf, seconds, 2);
		offset -= seconds * DateTimeConstants.MILLIS_PER_SECOND;
		if (offset == 0) {
			return buf.toString();
		}

		buf.append('.');
		FormatUtils.appendPaddedInteger(buf, offset, 3);
		return buf.toString();
	}
}
