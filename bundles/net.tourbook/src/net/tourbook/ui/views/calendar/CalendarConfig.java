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
package net.tourbook.ui.views.calendar;

public class CalendarConfig {

	/*
	 * Set default values also here to ensure that a valid value is set. A default value would not
	 * be set when an xml tag is not available.
	 */

	// config
	public String				id					= Long.toString(System.nanoTime());
	public String				defaultId			= CalendarConfigManager.CONFIG_DEFAULT_ID_1;
	public String				name				= CalendarConfigManager.CONFIG_DEFAULT_ID_1;

	// layout
	public int					weekHeight			= CalendarConfigManager.DEFAULT_WEEK_HEIGHT;

	// day
	public boolean				isShowDayHeader		= true;
	public DayHeaderDateFormat	dayHeaderFormat		= CalendarConfigManager.DEFAULT_DAY_HEADER_FORMAT;
	public DayHeaderLayout		dayHeaderLayout		= CalendarConfigManager.DEFAULT_DAY_HEADER_LAYOUT;

	// date column
	public boolean				isShowDateColumn	= true;
	public DateColumnContent	dateColumnContent	= CalendarConfigManager.DEFAULT_DATE_COLUMN_CONTENT;
	public int					dateColumnWidth		= CalendarConfigManager.DEFAULT_DATE_COLUMN_WIDTH;

	// summary column
	public boolean				isShowSummaryColumn	= true;
	public int					summaryColumnWidth	= CalendarConfigManager.DEFAULT_SUMMARY_COLUMN_WIDTH;

	@Override
	public boolean equals(final Object obj) {

		if (this == obj) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (getClass() != obj.getClass()) {
			return false;
		}

		final CalendarConfig other = (CalendarConfig) obj;

		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {

		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());

		return result;
	}

}
