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
package net.tourbook.ui;

import java.util.ArrayList;

import net.tourbook.data.TourData;
import net.tourbook.tour.TourEditor;

public interface ISelectedTours {

	/**
	 * Returns the tours which are selected or <code>null</code> when a tour is not selected
	 */
	ArrayList<TourData> getSelectedTours();

	/**
	 * @return Returns <code>true</code> when {@link ISelectedTours#getSelectedTours()} is created
	 *         in a {@link TourEditor}
	 */
	boolean isFromTourEditor();

}
