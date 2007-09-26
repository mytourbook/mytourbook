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

import net.tourbook.data.TourCompared;
import net.tourbook.data.TourData;
import net.tourbook.data.TourReference;
import net.tourbook.tour.TreeViewerItem;

/**
 * Tree view item with the compare result between the reference and the compared tour
 */
public class CompareResultItemComparedTour extends TreeViewerItem {

	/**
	 * Unique key for the {@link TourCompared} entity, when <code>-1</code> the compared tour is
	 * not saved in the database
	 */
	long			compId					= -1;

	TourReference	refTour;

	/**
	 * contains the {@link TourData} for the compared tour
	 */
	TourData		comparedTourData;

	/**
	 * contains the minimum value for the altitude differenz
	 */
	int				minAltitudeDiff			= 0;

	/**
	 * contains the minimum data serie for each compared value
	 */
	int[]			altitudeDiffSerie;

	int				computedStartIndex		= -1;
	int				computedEndIndex		= -1;

	int				normalizedStartIndex	= -1;
	int				normalizedEndIndex		= -1;

	int				compareDrivingTime;
	int				compareRecordingTime;
	int				compareDistance;
	float			compareSpeed;

	int				timeIntervall;

	/*
	 * when a compared tour is stored in the database, the compId is set and the data from the
	 * database are stored in the field's db...
	 */
	int				dbStartIndex;
	int				dbEndIndex;
	float			dbSpeed;

	/*
	 * the moved... fields contain the position of the compared tour when the user moved the
	 * position
	 */
	int				movedStartIndex;
	int				movedEndIndex;
	float			movedSpeed;

	@Override
	public boolean hasChildren() {
		/*
		 * compare result has no children, hide the expand sign
		 */
		return false;
	}

	@Override
	protected void fetchChildren() {}

	@Override
	protected void remove() {}

}
