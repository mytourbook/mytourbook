/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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
package net.tourbook.database;

import net.tourbook.data.TourData;

/**
 * Interface to compute tour values for {@link TourData}
 */
public interface IComputeTourValues {

	/**
	 * @param originalTourData
	 *            {@link TourData} which is not yet modified
	 * @return Returns <code>true</code> when {@link TourData} was modified and the tour needs to be
	 *         saved
	 */
	public boolean computeTourValues(TourData originalTourData);

	/**
	 * @return Returns the text which is displayed at the end of the task to the user to show the
	 *         result of the computation
	 */
	public String getResultText();

	/**
	 * @param savedTourData
	 * @return Returns the text which should be displayed in the progress bar when one tour was
	 *         computed
	 */
	public String getSubTaskText(TourData savedTourData);
}
