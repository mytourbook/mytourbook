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
 
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;

import de.byteholder.geoclipse.Messages;
import de.byteholder.geoclipse.logging.StatusUtil;
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
	private ArrayList<MPWrapper>	fMpWrappers;

	/**
	 * background color for the profile image, this color is displayed in the transparent areas
	 */
	private int						fBackgroundColor		= 0xFFFFFF;

	private boolean					fIsSaveImage			= true;

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
	 * Updates values from the wrapper into the tile factory
	 * 
	 * @param allMpWrapper
	 */
	static void updateWrapperTileFactory(final ArrayList<MPWrapper> allMpWrapper) {

		for (final MPWrapper mpWrapper : allMpWrapper) {

			if (mpWrapper.isDisplayedInMap()) {

				final MP wrappedMp = mpWrapper.getMP();

				wrappedMp.setIsProfileTransparentColors(mpWrapper.isTransparentColors());
				wrappedMp.setIsProfileTransparentBlack(mpWrapper.isTransparentBlack());
				wrappedMp.setProfileTransparentColors(mpWrapper.getTransparentColors());
				wrappedMp.setProfileAlpha(mpWrapper.getAlpha());
				wrappedMp.setIsProfileBrightness(mpWrapper.isBrightness());
				wrappedMp.setProfileBrightness(mpWrapper.getBrightness());
			}
		}
	}

	public MPProfile() {}

	public MPProfile(final ArrayList<MPWrapper> mpWrappers) {
		fMpWrappers = mpWrappers;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {

		final MPProfile clonedMpProfile = (MPProfile) super.clone();

		// create deep copies

		final ArrayList<MPWrapper> clonedMpWrapperList = new ArrayList<MPWrapper>();

		for (final MPWrapper mpWrapper : fMpWrappers) {

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

		clonedMpProfile.fMpWrappers = clonedMpWrapperList;

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

		for (final MPWrapper mpWrapper : fMpWrappers) {

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
						parentTile.getX(),
						parentTile.getY(),
						parentZoom,
						childMp.getId(),
						null,
						childMp.getProjection().getId());

				// check if a tile with the requested child tile key is already in a cache
				Tile childTile = tileCache.get(childTileKey);
				if (childTile != null) {
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
		return fMpWrappers;
	}

	public int getBackgroundColor() {
		return fBackgroundColor;
	}

// mp2	
//	@Override
//	public TileFactory_OLD getTileFactory(final boolean initTileFactory) {
//
//		if (initTileFactory == false) {
//			return fTileFactory;
//		}
//
//		if (fTileFactory == null) {
//			synchronizeMPWrapper();
//		}
//
//		// initialize tile factory when it's not done yet
//		fTileFactory.getInfo();
//
//		return fTileFactory;
//	}

	public ParentImageStatus getParentImage(final Tile parentTile, final Tile childTile) {

		final ArrayList<Tile> tileChildren = parentTile.getChildren();
		if (tileChildren == null) {
			return null;
		}

		final ProfileTileImage parentImage = new ProfileTileImage();

		parentImage.setBackgroundColor(fBackgroundColor);

		boolean isFinal = true;

		Tile brightnessTile = null;
		ImageData brightnessImageData = null;
		boolean isChildError = false;

		// loop: all children
		for (final Tile childTile1 : tileChildren) {

			final String childLoadingError = childTile1.getLoadingError();

			if (childLoadingError != null) {

				// child is loaded but has errors

				isChildError = true;

				continue;
			}

			final ImageData[] childImageData = childTile1.getChildImageData();

			if (childImageData == null || childImageData[0] == null) {

				// loading of this child has not yet finished
				isFinal = false;

				continue;
			}

			if (childTile1.getMP().isProfileBrightness()) {

				// use the brightness of the current tile for the next tile

				brightnessTile = childTile1;
				brightnessImageData = childImageData[0];

				continue;
			}

			// draw child image into the parent image
			parentImage.drawImage(childImageData[0], childTile1, brightnessImageData, brightnessTile);

			brightnessTile = null;
			brightnessImageData = null;
		}

		return new ParentImageStatus(//
				new ImageData[] { parentImage.getImageData() },
				isFinal,
				fIsSaveImage && isChildError == false);
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
		fBackgroundColor = backgroundColor;
	}

// mp2
//	@Override
//	public void setZoomLevel(final int minZoom, final int maxZoom) {
//
//		super.setZoomLevel(minZoom, maxZoom);
//
//		// initialize zoom level in the tile factory info because the map gets zoom data from this location
//		if (fTileFactory != null) {
//			fTileFactory.getInfo().initializeZoomLevel(minZoom, maxZoom);
//		}
//	}

	public void setBackgroundColor(final RGB rgb) {
		fBackgroundColor = ((rgb.red & 0xFF) << 0) | ((rgb.green & 0xFF) << 8) | ((rgb.blue & 0xFF) << 16);
	}

	public void setIsSaveImage(final boolean isSaveImage) {
		fIsSaveImage = isSaveImage;
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
			 * wms map provider was not yet created, make a clone
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

			StatusUtil.showStatus(NLS.bind(Messages.DBG056_MapProfile_WrongClassForMapProvider, new Object[] {
					mpWrapper.getMapProviderId(),
					wrapperClassName,
					validClassName }), new Exception());

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

		if (fMpWrappers == null) {

			/*
			 * this case happens when a profile map provider is created, add all map providers
			 */

			fMpWrappers = new ArrayList<MPWrapper>();

			for (final MP mapProvider : allMPsWithoutProfile) {
				fMpWrappers.add(createWrapper(mapProvider));
			}

		} else {

			/*
			 * synchronize profile mp wrapper with the available map providers
			 */

			final ArrayList<MPWrapper> currentMpWrappers = fMpWrappers;
			final ArrayList<MPWrapper> remainingMpWrappers = new ArrayList<MPWrapper>(fMpWrappers);

			fMpWrappers = new ArrayList<MPWrapper>();

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

						fMpWrappers.add(mpWrapper);
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

				fMpWrappers.add(mpWrapperClone);
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

		sortMpWrapper(fMpWrappers);
		updateWrapperTileFactory(fMpWrappers);
	}
}
