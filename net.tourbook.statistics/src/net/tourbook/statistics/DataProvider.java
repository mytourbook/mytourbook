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

//	protected String getSQLFilterTourType(TourTypeFilter tourTypeFilter) {
//		return tourTypeFilter == TourType.TOUR_TYPE_ID_ALL
//				? "" //$NON-NLS-1$
//				: tourTypeFilter == TourType.TOUR_TYPE_ID_NOT_DEFINED
//						? " AND tourType_typeId is null" //$NON-NLS-1$
//						: " AND tourType_typeId =" + Long.toString(tourTypeFilter); //$NON-NLS-1$
//	}

	protected String getSQLFilterPerson(TourPerson person) {
		return person == null ? "" : " AND tourPerson_personId = " //$NON-NLS-1$ //$NON-NLS-2$
				+ Long.toString(person.getPersonId());
	}
}
