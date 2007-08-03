/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm
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
package net.tourbook.statistic;

import net.tourbook.data.TourPerson;

public interface IYearStatistic {

	/**
	 * @param person
	 *        active person or <code>null</code> when no person/all people are
	 *        selected
	 * @param typeId
	 *        TourType id
	 * @param year
	 *        year for the statistic
	 * @param refreshData
	 *        when set to <code>true</code> the data should be updated from the
	 *        database
	 */
	public abstract void refreshStatistic(TourPerson person,
									long typeId,
									int year,
									boolean refreshData);

	public abstract void prefColorChanged();

}
