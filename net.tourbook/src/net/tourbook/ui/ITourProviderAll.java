/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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
package net.tourbook.ui;

import java.util.ArrayList;

import net.tourbook.data.TourData;

/**
 * This interface provides tours which are selected in a view but must not be saved, deleted tours
 * are excluded
 */
public interface ITourProviderAll extends ITourProvider {

	/**
	 * Returns tours which are selected in a view but must not be saved, <code>null</code> is
	 * returned when a tour is not selected.
	 * 
	 * @return Returns all selected tour this includes tours which are not saved, deleted tours are
	 *         excluded.
	 */
	ArrayList<TourData> getAllSelectedTours();

}
