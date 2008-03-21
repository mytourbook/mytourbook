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
package net.tourbook.statistics;

import net.tourbook.data.TourPerson;
import net.tourbook.ui.TourTypeFilter;

public abstract class DataProvider {

	protected String getSQLFilter(TourPerson person, TourTypeFilter tourTypeFilter) {
		return getSQLFilterPerson(person) + tourTypeFilter.getSQLString();
	}

	protected String getSQLFilterPerson(TourPerson person) {
		return person == null ? "" : " AND tourPerson_personId = " //$NON-NLS-1$ //$NON-NLS-2$
				+ Long.toString(person.getPersonId());
	}

	/**
	 * @param finalYear
	 * @param numberOfYears
	 * @return Returns a list with all years
	 */
	static String getYearList(final int finalYear, final int numberOfYears) {

		final StringBuffer buffer = new StringBuffer();

		for (int currentYear = finalYear; currentYear >= finalYear - numberOfYears + 1; currentYear--) {

			if (currentYear != finalYear) {
				buffer.append(',');
			}

			buffer.append(Integer.toString(currentYear));
		}

		return buffer.toString();
	}
}
