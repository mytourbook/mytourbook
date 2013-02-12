/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourCatalog;

import net.tourbook.data.TourCompared;
import net.tourbook.data.TourData;
import net.tourbook.data.TourReference;

/**
 * Tree view item with the compare result between the reference and the compared tour
 */
public class TVICompareResultComparedTour extends TVICompareResultItem {

	/**
	 * Unique key for the {@link TourCompared} entity, when <code>-1</code> the compared tour is not
	 * saved in the database
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
	float			minAltitudeDiff			= 0;

	/**
	 * contains the minimum data serie for each compared value
	 */
	float[]			altitudeDiffSerie;

	int				computedStartIndex		= -1;
	int				computedEndIndex		= -1;

	int				normalizedStartIndex	= -1;
	int				normalizedEndIndex		= -1;

	int				compareDrivingTime;
	int				compareRecordingTime;

	float			compareDistance;
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
//	int				movedStartIndex;
//	int				movedEndIndex;

	float			movedSpeed;

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof TVICompareResultComparedTour)) {
			return false;
		}
		final TVICompareResultComparedTour other = (TVICompareResultComparedTour) obj;
		if (comparedTourData == null) {
			if (other.comparedTourData != null) {
				return false;
			}
		} else if (!comparedTourData.equals(other.comparedTourData)) {
			return false;
		}
		if (refTour == null) {
			if (other.refTour != null) {
				return false;
			}
		} else if (!refTour.equals(other.refTour)) {
			return false;
		}
		return true;
	}

	@Override
	protected void fetchChildren() {}

	public TourData getComparedTourData() {
		return comparedTourData;
	}

	@Override
	public boolean hasChildren() {
		/*
		 * compare result has no children, hide the expand sign
		 */
		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((comparedTourData == null) ? 0 : comparedTourData.hashCode());
		result = prime * result + ((refTour == null) ? 0 : refTour.hashCode());
		return result;
	}

	/**
	 * @return Returns <code>true</code> when the compare result is saved in the database
	 */
	boolean isSaved() {
		return compId != -1;
	}

}
