/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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

import java.util.ArrayList;
import java.util.Set;

import net.tourbook.common.map.GeoPosition;
import net.tourbook.data.TourData;
import net.tourbook.photo.Photo;

/**
 * Contains data which are needed to paint a tour into a map.
 */
public class TourPainterConfiguration {

	private static TourPainterConfiguration	_instance;

	private final ArrayList<TourData>		_tourDataList	= new ArrayList<TourData>();
	private final ArrayList<Photo>			_photoList		= new ArrayList<Photo>();

	/**
	 * contains the upper left and lower right position for a tour
	 */
	private Set<GeoPosition>				_tourBounds;

	private int								_synchTourZoomLevel;

	private ILegendProvider					_legendProvider;

	boolean									isShowStartEndInMap;
	boolean									isShowTourMarker;
	boolean									isShowWayPoints;
	boolean									isShowPhoto;

	private TourPainterConfiguration() {}

	public static TourPainterConfiguration getInstance() {

		if (_instance == null) {
			_instance = new TourPainterConfiguration();
		}

		return _instance;
	}

	public ILegendProvider getLegendProvider() {
		return _legendProvider;
	}

	public ArrayList<Photo> getPhotos() {
		return _photoList;
	}

	public int getSynchTourZoomLevel() {
		return _synchTourZoomLevel;
	}

	/**
	 * @return Returns the tour bounds or <code>null</code> when a tour is not set
	 */
	public Set<GeoPosition> getTourBounds() {
		return _tourBounds;
	}

	/**
	 * @return Returns the current {@link TourData} which is selected in a view or editor
	 */
	public ArrayList<TourData> getTourData() {
		return _tourDataList;
	}

	public void setLegendProvider(final ILegendProvider iLegendProvider) {
		if (iLegendProvider != null) {
			_legendProvider = iLegendProvider;
		}
	}

	public void setPhotos(final ArrayList<Photo> photoList) {

		_photoList.clear();

		if (photoList != null) {

			isShowPhoto = true;
			_photoList.addAll(photoList);

		} else {

			isShowPhoto = false;
		}
	}

	public void setSynchTourZoomLevel(final int zoomLevel) {
		_synchTourZoomLevel = zoomLevel;
	}

	public void setTourBounds(final Set<GeoPosition> mapPositions) {
		_tourBounds = mapPositions;
	}

	/**
	 * Set {@link TourData} which is used for the next painting or <code>null</code> to not draw the
	 * tour
	 * 
	 * @param tourData
	 */
	public void setTourData(final TourData tourData) {

		_tourDataList.clear();
		_tourDataList.add(tourData);
	}

	/**
	 * Sets {@link TourData} for all tours which are displayed
	 * 
	 * @param tourDataList
	 */
	public void setTourDataList(final ArrayList<TourData> tourDataList) {

		_tourDataList.clear();

		if (tourDataList != null) {
			_tourDataList.addAll(tourDataList);
		}
	}
}
