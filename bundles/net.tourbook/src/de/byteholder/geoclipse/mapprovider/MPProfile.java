/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import net.tourbook.common.util.StatusUtil;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;

import de.byteholder.geoclipse.Messages;
import de.byteholder.geoclipse.map.ITileChildrenCreator;
import de.byteholder.geoclipse.map.ParentImageStatus;
import de.byteholder.geoclipse.map.Tile;
import de.byteholder.geoclipse.map.TileCache;
import de.byteholder.geoclipse.map.UI;

/**
 * Wraps all map providers into a map profile, these map providers can be selected individually
 */
public class MPProfile extends MP implements ITileChildrenCreator {

	public static final String		WMS_CUSTOM_TILE_PATH	= "all-map-profile-wms";	//$NON-NLS-1$

	/**
	 * this list contains wrappers for all none profile map providers
	 */
	private ArrayList<MPWrapper>	_mpWrappers;

	/**
	 * background color for the profile image, this color is displayed in the transparent areas
	 */
	private int						_backgroundColor		= 0xFFFFFF;

	private boolean					_isSaveImage			= true;

	public MPProfile() {}

	public MPProfile(final ArrayList<MPWrapper> mpWrappers) {
		_mpWrappers = mpWrappers;
	}

	/**
	 * Sort map provider wrapper by position (by name when position is not available)
	 * 
	 * @param mpWrapper
	 */
	static void sortMpWrapper(final ArrayList<MPWrapper> mpWrapper) {

		Collections.sort(mpWrapper, new Comparator<MPWrapper>() {

			public int compare(final MPWrapper mp1, final MPWrapper mp2) {

				final int pos1 = mp1.getPositionIndex();
				final int pos2 = mp2.getPositionIndex();

				if (pos1 > -1 && pos2 > -1) {

					// sort by position
					return pos1 - pos2;

				} else if (pos1 > -1) {

					// set wrapper with position before the others
					return -2;

				} else if (pos2 > -1) {

					// set wrapper with position before the others
					return 2;

				} else {

					// sort by name when position is not set
					return mp1.getMP().getName().compareTo(mp2.getMP().getName());
				}
			}
		});
	}

	/**
	 * Set data from the wrapper into the map provider
	 * 
	 * @param allMpWrapper
	 */
	static void updateMpFromWrapper(final ArrayList<MPWrapper> allMpWrapper) {

		for (final MPWrapper wrapper : allMpWrapper) {
			if (wrapper.isDisplayedInMap()) {
				updateMpFromWrapper(wrapper.getMP(), wrapper);
			}
		}
	}

	static void updateMpFromWrapper(final MP mp, final MPWrapper wrapper) {

		mp.setIsProfileBrightnessForNextMp(wrapper.isBrightnessForNextMp());
		mp.setProfileBrightnessForNextMp(wrapper.getBrightnessValueForNextMp());

		mp.setIsProfileTransparentColors(wrapper.isTransparentColors());
		mp.setProfileTransparentColors(wrapper.getTransparentColors());

		mp.setProfileAlpha(wrapper.getAlpha());
		mp.setIsProfileTransparentBlack(wrapper.isTransparentBlack());
	}

	/**
	 * Set data from the map provider into the wrapper
	 * 
	 * @param wrapper
	 * @param mp
	 */
	static void updateWrapperFromMp(final MPWrapper wrapper, final MP mp) {

		wrapper.setIsBrightnessForNextMp(mp.isProfileBrightnessForNextMp());
		wrapper.setBrightnessForNextMp(mp.getProfileBrightnessForNextMp());

		wrapper.setIsTransparentColors(mp.isProfileTransparentColors());
		wrapper.setTransparentColors(mp.getProfileTransparentColors());

		wrapper.setAlpha(mp.getProfileAlpha());
		wrapper.setIsTransparentBlack(mp.isProfileTransparentBlack());
	}

	@Override
	public Object clone() throws CloneNotSupportedException {

		final MPProfile clonedMpProfile = (MPProfile) super.clone();

		// create deep copies

		final ArrayList<MPWrapper> clonedMpWrapperList = new ArrayList<MPWrapper>();

		for (final MPWrapper mpWrapper : _mpWrappers) {

			final MPWrapper clonedMpWrapper = (MPWrapper) mpWrapper.clone();

			// set wms properties
			final MP clonedMP = clonedMpWrapper.getMP();
			if (clonedMP instanceof MPWms) {

				final MPWms clonedWmsMp = (MPWms) clonedMP;

				// load images always transparent, a profile image has always a background color
				clonedWmsMp.setTransparent(true);

				// only the image size of 256 in the map profile is currently supported
				clonedWmsMp.setTileSize(256);
			}

			clonedMpWrapperList.add(clonedMpWrapper);
		}

		clonedMpProfile._mpWrappers = clonedMpWrapperList;

		return clonedMpProfile;
	}

	/**
	 * Creates tile children for all mp wrapper which are displayed in one tile
	 * 
	 * @param parentTile
	 * @return Returns a list with children which are not yet available in the tile cache or error
	 *         cache, children are skipped when they already exist and have loading errord
	 */
	public ArrayList<Tile> createTileChildren(final Tile parentTile) {

		final ArrayList<Tile> tileChildren = new ArrayList<Tile>();

		final TileCache tileCache = getTileCache();
		final TileCache errorTiles = getErrorTiles();

		for (final MPWrapper mpWrapper : _mpWrappers) {

			final int parentZoom = parentTile.getZoom();
			final MP wrapperMP = mpWrapper.getMP();

			if (parentZoom < wrapperMP.getMinZoomLevel() || parentZoom > wrapperMP.getMaxZoomLevel()) {

				// ignore map providers which do not support the current zoom level

				continue;
			}

			// create child tile for each visible map provider
			if (mpWrapper.isDisplayedInMap() && mpWrapper.isEnabled()) {

				final MP childMp = wrapperMP;

				// check if this child is already being loaded

				final String childTileKey = Tile.getTileKey(
						childMp,
						parentZoom,
						parentTile.getX(),
						parentTile.getY(),
						childMp.getId(),
						null,
						childMp.getProjection().getId());

				/*
				 * check if a tile with the requested child tile key is already in the normal tile
				 * cache or in the error tile cache
				 */
				Tile childTile = tileCache.get(childTileKey);
				if (childTile == null) {
					childTile = errorTiles.get(childTileKey);
				}

				if (childTile != null) {

					// child tile is currently being loaded or has a loading error

					if (childTile.isLoadingError()) {

						// update notify parent about the loading error

						parentTile.setChildLoadingError(childTile);

						continue;
					}

				} else {

					// create child tile

					childTile = new Tile(//
							childMp,
							parentZoom,
							parentTile.getX(),
							parentTile.getY(),

							// create a unique tile for each child (map provider)
							childMp.getId());

					childMp.doPostCreation(childTile);

					childTile.setParentTile(parentTile);
					childTile.setBoundingBoxEPSG4326();

					if (childMp instanceof MPWms) {

						/**
						 * wms tiles must be saved separately because a map profile supports only
						 * 256 pixel but these tiles can have other pixels in another wms map
						 * provider
						 */

						childTile.setTileCustomPath(WMS_CUSTOM_TILE_PATH);
					}
				}

				tileChildren.add(childTile);
			}
		}

		return tileChildren;
	}

	/**
	 * Create a wrapper from a map provider
	 * 
	 * @param mp
	 * @return
	 */
	private MPWrapper createWrapper(final MP mp) {

		MPWrapper mpWrapper = null;

		try {

			final MP clonedMapProvider = (MP) mp.clone();

			mpWrapper = new MPWrapper(clonedMapProvider);

			// hide map provider but keep the layer visibility
			mpWrapper.setIsDisplayedInMap(false);

		} catch (final CloneNotSupportedException e) {
			StatusUtil.log(e);
		}

		return mpWrapper;
	}

	/**
	 * @return Returns a list which contains wrappers for all none profile map providers
	 */
	public ArrayList<MPWrapper> getAllWrappers() {
		return _mpWrappers;
	}

	public int getBackgroundColor() {
		return _backgroundColor;
	}

	/**
	 * this method is synchronized because when live View is checked in the map profile dialog
	 * and the brightness is changed, many ConcurrentModificationException occured
	 * 
	 * <pre>
	 * java.util.ConcurrentModificationException
	 * 	at java.util.AbstractList$Itr.checkForComodification(Unknown Source)
	 * 	at java.util.AbstractList$Itr.next(Unknown Source)
	 * 	at de.byteholder.geoclipse.mapprovider.MPProfile.getParentImage(MPProfile.java:334)
	 * 	at de.byteholder.geoclipse.map.Tile.createParentImage(Tile.java:477)
	 * 	at de.byteholder.geoclipse.map.TileImageLoader.getTileImage(TileImageLoader.java:275)
	 * </pre>
	 */
	public ParentImageStatus getParentImage(final Tile parentTile) {

		@SuppressWarnings("unchecked")
		final ArrayList<Tile> tileChildren = (ArrayList<Tile>) parentTile.getChildren().clone();
		if (tileChildren == null) {
			return null;
		}

		final ProfileTileImage parentImage = new ProfileTileImage();

		parentImage.setBackgroundColor(_backgroundColor);

		boolean isFinal = true;

		Tile brightnessTile = null;
		ImageData brightnessImageData = null;
		boolean isChildError = false;

		// loop: all children
		for (final Tile childTile : tileChildren) {

			final String childLoadingError = childTile.getLoadingError();

			if (childLoadingError != null) {

				// child is loaded but has errors

				isChildError = true;

				continue;
			}

			final ImageData childImageData = childTile.getChildImageData();

			if (childImageData == null) {

				// loading of this child has not yet finished
				isFinal = false;

				continue;
			}

			if (childTile.getMP().isProfileBrightnessForNextMp()) {

				// use the brightness of the current tile for the next tile

				brightnessTile = childTile;
				brightnessImageData = childImageData;

				continue;
			}

			// draw child image into the parent image
			parentImage.drawImage(childImageData, childTile, brightnessImageData, brightnessTile);

			brightnessTile = null;
			brightnessImageData = null;
		}

		return new ParentImageStatus(//
				parentImage.getImageData(),
				isFinal,
				_isSaveImage,
				isChildError);
	}

	@Override
	public IPath getTileOSPath(final String fullPath, final Tile tile) {

		final IPath filePath = new Path(fullPath)//
				.append(getOfflineFolder())
				.append(Integer.toString(tile.getZoom()))
				.append(Integer.toString(tile.getX()))
				.append(Integer.toString(tile.getY()))
				.addFileExtension(MapProviderManager.getImageFileExtension(getImageFormat()));

		return filePath;
	}

	public void setBackgroundColor(final int backgroundColor) {
		_backgroundColor = backgroundColor;
	}

	public void setBackgroundColor(final RGB rgb) {
		_backgroundColor = ((rgb.red & 0xFF) << 0) | ((rgb.green & 0xFF) << 8) | ((rgb.blue & 0xFF) << 16);
	}

	public void setIsSaveImage(final boolean isSaveImage) {
		_isSaveImage = isSaveImage;
		setUseOfflineImage(isSaveImage);
	}

	/**
	 * Synchronizes two map providers
	 * 
	 * @param mpWrapper
	 * @param validMapProvider
	 * @return Returns <code>false</code> when the synchronization fails
	 */
	private boolean synchMpWrapper(final MPWrapper mpWrapper, final MP validMapProvider) {

		final MP wrapperMapProvider = mpWrapper.getMP();
		if (wrapperMapProvider == null) {

			/*
			 * map provider was not yet created, create a clone
			 */

			try {
				mpWrapper.setMP((MP) validMapProvider.clone());
			} catch (final CloneNotSupportedException e) {
				StatusUtil.showStatus(e.getMessage(), e);
				return false;
			}

			return true;
		}

		// check type with the class name
		final String wrapperClassName = wrapperMapProvider.getClass().getName();
		final String validClassName = validMapProvider.getClass().getName();
		if (wrapperClassName.equals(validClassName) == false) {

			StatusUtil.showStatus(
					NLS.bind(
							Messages.DBG056_MapProfile_WrongClassForMapProvider,
							new Object[] { mpWrapper.getMapProviderId(), wrapperClassName, validClassName }),
					new Exception());

			return false;
		}

		// synch wrapper from valid map provider
		wrapperMapProvider.setName(validMapProvider.getName());
		wrapperMapProvider.setOfflineFolder(validMapProvider.getOfflineFolder());

		return true;
	}

	/**
	 * creates wrappers for all map providers which are not a map profile
	 */
	public void synchronizeMPWrapper() {

		final ArrayList<MP> allMPsWithoutProfile = MapProviderManager.getInstance().getAllMapProviders(false);

		if (_mpWrappers == null) {

			/*
			 * this case happens when a profile map provider is created, add all map providers
			 */

			_mpWrappers = new ArrayList<MPWrapper>();

			for (final MP mapProvider : allMPsWithoutProfile) {
				_mpWrappers.add(createWrapper(mapProvider));
			}

		} else {

			/*
			 * synchronize profile mp wrapper with the available map providers
			 */

			final ArrayList<MPWrapper> currentMpWrappers = _mpWrappers;
			final ArrayList<MPWrapper> remainingMpWrappers = new ArrayList<MPWrapper>(_mpWrappers);

			_mpWrappers = new ArrayList<MPWrapper>();

			// loop: all available map providers
			for (final MP validMapProvider : allMPsWithoutProfile) {

				final String validMapProviderId = validMapProvider.getId();

				// check if a valid map provider is available in this profile
				boolean isMpValid = false;
				for (final MPWrapper mpWrapper : currentMpWrappers) {

					if (validMapProviderId.equalsIgnoreCase(mpWrapper.getMapProviderId())) {

						// mp wrapper is found with the same id

						if (synchMpWrapper(mpWrapper, validMapProvider) == false) {
							break;
						}

						_mpWrappers.add(mpWrapper);
						remainingMpWrappers.remove(mpWrapper);

						isMpValid = true;

						break;
					}
				}

				if (isMpValid) {
					continue;
				}

				// valid map provider is not yet in the profile, append it now
				final MPWrapper mpWrapperClone = createWrapper(validMapProvider);

				_mpWrappers.add(mpWrapperClone);
			}

			if (remainingMpWrappers.size() > 0) {

				/*
				 * there are mappers in the profile which do not exist in the available map
				 * provider list, these mappers will be ignored
				 */
				final StringBuilder sb = new StringBuilder();

				sb.append(NLS.bind(Messages.DBG055_MapProfile_InvalidMapProvider, getName()));

				for (final MPWrapper mpWrapper : remainingMpWrappers) {
					sb.append(mpWrapper.getMapProviderId());
					sb.append(UI.NEW_LINE);
				}

				StatusUtil.showStatus(sb.toString(), new Exception());
			}
		}

		sortMpWrapper(_mpWrappers);
		updateMpFromWrapper(_mpWrappers);
	}
}
