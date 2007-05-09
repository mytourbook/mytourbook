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
package net.tourbook.ui.views.tourMap;

import net.tourbook.data.TourData;
import net.tourbook.data.TourReference;
import net.tourbook.tour.TreeViewerItem;

/**
 * Contains the result for the comparision between two tours
 */
public class TVICompareResult extends TreeViewerItem {

	/**
	 * id for the TourCompared entity, when set to -1 the compared tour is not
	 * saved in the database
	 */
	long					compId				= -1;

	protected TourReference	refTour;
	protected TourData		compTour;

	protected int			altitudeDiff		= 0;

	protected int			compareIndexStart	= -1;
	protected int			compareIndexEnd		= -1;

	protected int			normIndexStart		= -1;
	protected int			normIndexEnd		= -1;

	protected int			compareTime;
	protected int			compareDistance;
	protected int			timeIntervall;

	public boolean hasChildren() {
		/*
		 * compare result has no children, hide the expand sign
		 */
		return false;
	}

	protected void fetchChildren() {}
	protected void remove() {}
}
