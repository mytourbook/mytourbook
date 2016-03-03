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
package net.tourbook.tour;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

class TourLog {

	private final DateTimeFormatter	_dtFormatterTime	= new DateTimeFormatterBuilder()
																.appendHourOfDay(2)
																.appendLiteral(':')
																.appendMinuteOfHour(2)
																.appendLiteral(':')
																.appendSecondOfMinute(2)
// TOO MUCH DETAIL
//																.appendLiteral(',')
//																.appendFractionOfSecond(3, 3)

																.toFormatter();

	public String					time;

	public TourLogState				state;

	public String					message;
	public boolean					isSubLogItem;

	public String					css;

	public TourLog(final TourLogState state, final String message) {

		this.time = _dtFormatterTime.print(System.currentTimeMillis());

		this.state = state;
		this.message = message;
	}

	@Override
	public String toString() {
		return "TourLog [" //$NON-NLS-1$

//				+ ("time=" + time + ", ")
//				+ ("state=" + state + ", ")
				+ ("message=" + message + ", ") //$NON-NLS-1$ //$NON-NLS-2$
//				+ ("isSubLogItem=" + isSubLogItem + ", ")
//				+ ("css=" + css)

				+ "]"; //$NON-NLS-1$
	}

}
