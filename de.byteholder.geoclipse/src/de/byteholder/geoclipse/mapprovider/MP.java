/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
package de.byteholder.geoclipse.mapprovider;

import org.eclipse.core.runtime.ListenerList;

import de.byteholder.geoclipse.map.TileFactory;
import de.byteholder.geoclipse.swt.UI;
import de.byteholder.gpx.GeoPosition;

/**
 * A map provider ({@link MP}) wraps a plugin or custom tile factory and contains data to display a
 * map provider in the UI
 */
public abstract class MP implements Cloneable, Comparable<Object> {

	public static final int				OFFLINE_INFO_NOT_READ			= -1;

	public static final int				UI_MIN_ZOOM_LEVEL				= 1;
	public static final int				UI_MAX_ZOOM_LEVEL				= 18;

	/**
	 * unique id to identify a map provider
	 */
	private String						fMapProviderId;

	/**
	 * name of the map provider which is displayed in the UI
	 */
	private String						fMapProviderName;

	/**
	 * map provider description
	 */
	private String						fDescription					= UI.EMPTY_STRING;

	/**
	 * OS folder to save offline images
	 */
	private String						fOfflineFolder;

	/**
	 * number of files in the offline cache
	 */
	private int							fOfflineFileCounter				= -1;

	/**
	 * size in Bytes for the offline images
	 */
	private long						fOfflineFileSize				= -1;

	private static final ListenerList	fOfflineReloadEventListeners	= new ListenerList(ListenerList.IDENTITY);

	/**
	 * image size in pixel for a square image
	 */
	private int							fImageSize						= Integer
																				.parseInt(MapProviderManager.DEFAULT_IMAGE_SIZE);

	/**
	 * mime image format which is currently used
	 */
	private String						fImageFormat					= MapProviderManager.DEFAULT_IMAGE_FORMAT;

	private int							fFavoriteZoom					= 0;
	private GeoPosition					fFavoritePosition				= new GeoPosition(0.0, 0.0);

	private int							fLastUsedZoom					= 0;
	private GeoPosition					fLastUsedPosition				= new GeoPosition(0.0, 0.0);

	private int							fMinZoomLevel					= 0;
	private int							fMaxZoomLevel					= UI_MAX_ZOOM_LEVEL - UI_MIN_ZOOM_LEVEL;

	/**
	 * State if the map provider can be toggled in the map
	 */
	private boolean						fCanBeToggled;

	public static void addOfflineInfoListener(final IOfflineInfoListener listener) {
		fOfflineReloadEventListeners.add(listener);
	}

	public static void removeOfflineInfoListener(final IOfflineInfoListener listener) {
		if (listener != null) {
			fOfflineReloadEventListeners.remove(listener);
		}
	}

	public MP() {}

	public boolean canBeToggled() {
		return fCanBeToggled;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {

		final MP mapProvider = (MP) super.clone();

		if (this instanceof MPProfile) {

			/*
			 * a map profile contains all map providers which are not a map profile, clone all of
			 * them in the clone constructor
			 */

		} else {

			mapProvider.fImageFormat = new String(fImageFormat);

			mapProvider.fFavoritePosition = new GeoPosition(fFavoritePosition == null
					? new GeoPosition(0.0, 0.0)
					: fFavoritePosition);

			mapProvider.fLastUsedPosition = new GeoPosition(fLastUsedPosition == null
					? new GeoPosition(0.0, 0.0)
					: fLastUsedPosition);
		}

		return mapProvider;
	}

	public int compareTo(final Object otherObject) {

		final MP otherMapProvider = (MP) otherObject;

		if (this instanceof MPPlugin && otherMapProvider instanceof MPPlugin) {

			return fMapProviderName.compareTo(otherMapProvider.getName());

		} else {

			if (this instanceof MPPlugin) {
				return -1;
			}
			if (otherMapProvider instanceof MPPlugin) {
				return 1;
			}

			return fMapProviderName.compareTo(otherMapProvider.getName());
		}
	}

	public abstract void disposeCachedImages();

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof MP)) {
			return false;
		}

		final MP other = (MP) obj;
		if (fMapProviderId == null) {
			if (other.fMapProviderId != null) {
				return false;
			}
		} else if (!fMapProviderId.equals(other.fMapProviderId)) {
			return false;
		}

		return true;
	}

	private void fireOfflineReloadEvent(final MP mapProvider) {

		final Object[] allListeners = fOfflineReloadEventListeners.getListeners();
		for (final Object listener : allListeners) {
			((IOfflineInfoListener) listener).offlineInfoIsDirty(mapProvider);
		}
	}

	public String getDescription() {
		return fDescription;
	}

	public GeoPosition getFavoritePosition() {
		return fFavoritePosition;
	}

	public int getFavoriteZoom() {
		return fFavoriteZoom;
	}

	/**
	 * @return Returns a unique id for the map provider
	 */
	public String getId() {
		return fMapProviderId;
	}

	public String getImageFormat() {
		return fImageFormat;
	}

	public int getImageSize() {
		return fImageSize;
	}

	public GeoPosition getLastUsedPosition() {
		return fLastUsedPosition;
	}

	public int getLastUsedZoom() {
		return fLastUsedZoom;
	}

	public int getMaxZoomLevel() {
		return fMaxZoomLevel;
	}

	public int getMinZoomLevel() {
		return fMinZoomLevel;
	}

	/**
	 * @return Returns the name of the map provider which is displayed in the UI
	 */
	public String getName() {
		return fMapProviderName;
	}

	public int getOfflineFileCounter() {
		return fOfflineFileCounter;
	}

	public long getOfflineFileSize() {
		return fOfflineFileSize;
	}

	public String getOfflineFolder() {
		return fOfflineFolder;
	}

	/**
	 * @return Returns the tile factory which provides the map images or <code>null</code> when the
	 *         tile factory is no initialized
	 */
	public abstract TileFactory getTileFactory();

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fMapProviderId == null) ? 0 : fMapProviderId.hashCode());
		return result;
	}

	public void setCanBeToggled(final boolean canBeToggled) {
		fCanBeToggled=canBeToggled;
	}

	public void setDescription(final String fDescription) {
		this.fDescription = fDescription;
	}

	public void setFavoritePosition(final GeoPosition fFavoritePosition) {
		this.fFavoritePosition = fFavoritePosition;
	}

	public void setFavoriteZoom(final int fFavoriteZoom) {
		this.fFavoriteZoom = fFavoriteZoom;
	}

	public void setImageFormat(final String imageFormat) {
		fImageFormat = imageFormat;
	}

	public void setImageSize(final int imageSize) {
		fImageSize = imageSize;
	}

	public void setLastUsedPosition(final GeoPosition position) {
		fLastUsedPosition = position;
	}

	public void setLastUsedZoom(final int zoom) {
		fLastUsedZoom = zoom;
	}

	public void setMapProviderId(final String mapProviderId) {

		fMapProviderId = mapProviderId;

		/*
		 * !!! very importand !!!
		 * the factory id must be set in the superclass to make the tile factory
		 * info unique, otherwise factorId is null and all created custom tile factory infos cannot
		 * be distinguished with the equals/hashcode methods
		 */
		//		super.setFactoryId(factoryId);
	}

	public void setName(final String mapProviderName) {
		fMapProviderName = mapProviderName;
	}

	public void setOfflineFileCounter(final int offlineFileCounter) {
		fOfflineFileCounter = offlineFileCounter;
	}

	public void setOfflineFileSize(final long offlineFileSize) {
		fOfflineFileSize = offlineFileSize;
	}

	public void setOfflineFolder(final String offlineFolder) {
		fOfflineFolder = offlineFolder;
	}

	public void setStateToReloadOfflineCounter() {

		if (fOfflineFileCounter != MP.OFFLINE_INFO_NOT_READ) {

			fOfflineFileCounter = MP.OFFLINE_INFO_NOT_READ;
			fOfflineFileSize = MP.OFFLINE_INFO_NOT_READ;

			fireOfflineReloadEvent(this);
		}
	}

	public void setZoomLevel(final int minZoom, final int maxZoom) {
		fMinZoomLevel = minZoom;
		fMaxZoomLevel = maxZoom;
	}

	@Override
	public String toString() {
		return fMapProviderName + "(" + fMapProviderId + ")"; //$NON-NLS-1$ //$NON-NLS-2$
	}
}
