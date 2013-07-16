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
package net.tourbook.map2.view;

import java.util.ArrayList;
import java.util.Set;

import net.tourbook.common.color.ILegendProvider;
import net.tourbook.common.map.GeoPosition;
import net.tourbook.data.TourData;
import net.tourbook.photo.Photo;

/**
 * Contains data which are needed to paint a tour into a map.
 */
public class TourPainterConfiguration {

	private static TourPainterConfiguration	_instance;

	private final ArrayList<TourData>		_tourDataList	= new ArrayList<TourData>();
	private final ArrayList<Photo>			_photos			= new ArrayList<Photo>();

	/**
	 * contains the upper left and lower right position for a tour
	 */
	private Set<GeoPosition>				_tourBounds;

	private int								_synchTourZoomLevel;

	private ILegendProvider					_legendProvider;

	boolean									isShowStartEndInMap;
	boolean									isShowTourMarker;
	boolean									isShowWayPoints;
	boolean									isPhotoVisible;
	boolean									isTourVisible;

	/**
	 * Is <code>true</code> when a link photo is displayed, otherwise a tour photo (photo which is
	 * save in a tour) is displayed.
	 */
	boolean									isLinkPhotoDisplayed;

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
		return _photos;
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

	/**
	 * Do not draw a tour
	 * 
	 * @param tourData
	 */
	public void resetTourData() {

		_tourDataList.clear();
		_tourDataList.add(null);
	}

	public void setLegendProvider(final ILegendProvider iLegendProvider) {
		if (iLegendProvider != null) {
			_legendProvider = iLegendProvider;
		}
	}

	/**
	 * @param allPhotos
	 *            When <code>null</code>, photos are not displayed.
	 * @param isShowPhoto
	 */
	public void setPhotos(final ArrayList<Photo> allPhotos, final boolean isShowPhoto, final boolean isLinkPhoto) {

		_photos.clear();

		if (allPhotos != null) {
			_photos.addAll(allPhotos);
		}

		isPhotoVisible = isShowPhoto && _photos.size() > 0;

		isLinkPhotoDisplayed = isLinkPhoto;
	}

	public void setSynchTourZoomLevel(final int zoomLevel) {
		_synchTourZoomLevel = zoomLevel;
	}

	public void setTourBounds(final Set<GeoPosition> mapPositions) {
		_tourBounds = mapPositions;
	}

	/**
	 * Sets {@link TourData} for all tours which are displayed
	 * 
	 * @param tourDataList
	 * @param isShowTour
	 */
	public void setTourData(final ArrayList<TourData> tourDataList, final boolean isShowTour) {

		_tourDataList.clear();

		if (tourDataList != null) {
			_tourDataList.addAll(tourDataList);
		}

		isTourVisible = isShowTour & _tourDataList.size() > 0;
	}

	/**
	 * Set {@link TourData} which is used for the next painting or <code>null</code> to not draw the
	 * tour
	 * 
	 * @param tourData
	 * @param isShowTour
	 */
	public void setTourData(final TourData tourData, final boolean isShowTour) {

		_tourDataList.clear();
		_tourDataList.add(tourData);

		isTourVisible = isShowTour & _tourDataList.size() > 0;
	}
}
