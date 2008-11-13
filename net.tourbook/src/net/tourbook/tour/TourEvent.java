/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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

import java.util.ArrayList;

import net.tourbook.data.TourData;
import net.tourbook.ui.views.tourDataEditor.TourDataEditorView;

public class TourEvent {

	/**
	 * contains the tours which have been modified
	 */
	private ArrayList<TourData>	modifiedTours;

	/**
	 * when <code>true</code>, tour data have been reverted and {@link TourEvent#modifiedTours}
	 * contains the reverted {@link TourData}
	 */
	public boolean				isReverted		= false;

	/**
	 * when <code>true</code>, tour data have been modified in the editor,
	 * {@link TourEvent#modifiedTours} contains the modified {@link TourData}
	 */
	public boolean				isTourModified	= false;

	/**
	 * contains the {@link TourData} which is edited in the {@link TourDataEditorView}
	 */
	public TourData				tourDataEditorSavedTour;

	public TourEvent(final ArrayList<TourData> modifiedTour) {
		this.modifiedTours = modifiedTour;
	}

	public TourEvent(final TourData tourData) {
		modifiedTours = new ArrayList<TourData>();
		modifiedTours.add(tourData);
	}

	/**
	 * @return Returns all tours which have been modified
	 */
	public ArrayList<TourData> getModifiedTours() {
		return modifiedTours;
	}

}
