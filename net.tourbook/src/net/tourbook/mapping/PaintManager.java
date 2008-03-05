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

package net.tourbook.mapping;

import java.util.Set;

import net.tourbook.data.TourData;
import de.byteholder.gpx.GeoPosition;

public class PaintManager {

	private static PaintManager	fInstance;

	private TourData			fTourData;

	/**
	 * contains the upper left and lower right position for a tour
	 */
	private Set<GeoPosition>	fTourBounds;

	private int					fSynchTourZoomLevel;

	private ILegendProvider		fLegendProvider;

	private boolean				fShowStartEndInMap;

	private PaintManager() {}

	public static PaintManager getInstance() {

		if (fInstance == null) {
			fInstance = new PaintManager();
		}

		return fInstance;
	}

	public ILegendProvider getLegendProvider() {
		return fLegendProvider;
	}

	public int getSynchTourZoomLevel() {
		return fSynchTourZoomLevel;
	}

	/**
	 * @return Returns the tour bounds or <code>null</code> when a tour is not set
	 */
	public Set<GeoPosition> getTourBounds() {
		return fTourBounds;
	}

	/**
	 * @return Returns the current {@link TourData} which is selected in a view or editor
	 */
	public TourData getTourData() {
		return fTourData;
	}

	public boolean isShowStartEndInMap() {
		return fShowStartEndInMap;
	}

	public void setLegendProvider(ILegendProvider legendProvider) {
		fLegendProvider = legendProvider;
	}

	public void setShowStartEnd(boolean isVisible) {
		fShowStartEndInMap = isVisible;
	}

	public void setSynchTourZoomLevel(int zoomLevel) {
		fSynchTourZoomLevel = zoomLevel;
	}

	public void setTourBounds(Set<GeoPosition> mapPositions) {
		fTourBounds = mapPositions;
	}

	/**
	 * Set {@link TourData} which is used for the next painting or <code>null</code> to not draw
	 * the tour
	 * 
	 * @param tourData
	 */
	public void setTourData(TourData tourData) {
		fTourData = tourData;
	}
}
