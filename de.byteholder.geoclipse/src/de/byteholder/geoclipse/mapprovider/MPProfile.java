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
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;

import de.byteholder.geoclipse.Messages;
import de.byteholder.geoclipse.logging.StatusUtil;
import de.byteholder.geoclipse.map.ParentImageStatus;
import de.byteholder.geoclipse.map.Tile;
import de.byteholder.geoclipse.map.UI;

/**
 * Wraps all map providers into a map profile, these map providers can be selected individually
 */
public class MPProfile extends MP {

	public static final String				WMS_CUSTOM_TILE_PATH	= "all-map-profile-wms";	//$NON-NLS-1$

	/**
	 * this list contains wrappers for all none profile map providers
	 */
	private ArrayList<MapProviderWrapper>	fMpWrappers;

	private TileFactoryProfile				fTileFactory;

	/**
	 * background color for the profile image, this color is displayed in the transparent areas
	 */
	private int								fBackgroundColor		= 0xFFFFFF;

	private boolean							fIsSaveImage			= true;

	/**
	 * Sort map provider wrapper by position (by name when position is not available)
	 * 
	 * @param mpWrapper
	 */
	static void sortMpWrapper(final ArrayList<MapProviderWrapper> mpWrapper) {

		Collections.sort(mpWrapper, new Comparator<MapProviderWrapper>() {

			public int compare(final MapProviderWrapper mp1, final MapProviderWrapper mp2) {

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
					return mp1.getMapProvider().getName().compareTo(mp2.getMapProvider().getName());
				}
			}
		});
	}

	/**
	 * Updates values from the wrapper into the tile factory
	 * 
	 * @param allMpWrapper
	 */
	static void updateWrapperTileFactory(final ArrayList<MapProviderWrapper> allMpWrapper) {

		for (final MapProviderWrapper mpWrapper : allMpWrapper) {

			if (mpWrapper.isDisplayedInMap()) {

				final MP_OLD wrapperMp = mpWrapper.getMapProvider();
				final TileFactory_OLD tileFactory = wrapperMp.getTileFactory(false);

				if (tileFactory != null) {
					tileFactory.setIsTransparentColors(mpWrapper.isTransparentColors());
					tileFactory.setIsTransparentBlack(mpWrapper.isTransparentBlack());
					tileFactory.setTransparentColors(mpWrapper.getTransparentColors());
					tileFactory.setProfileAlpha(mpWrapper.getAlpha());
					tileFactory.setIsProfileBrightness(mpWrapper.isBrightness());
					tileFactory.setProfileBrightness(mpWrapper.getBrightness());
				}
			}
		}
	}

	public MPProfile() {}

	public MPProfile(final ArrayList<MapProviderWrapper> mpWrappers) {
		fMpWrappers = mpWrappers;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {

		final MPProfile clonedMpProfile = (MPProfile) super.clone();

		// create deep copies

		final ArrayList<MapProviderWrapper> clonedMpWrapperList = new ArrayList<MapProviderWrapper>();

		for (final MapProviderWrapper mpWrapper : fMpWrappers) {

			final MapProviderWrapper clonedMpWrapper = (MapProviderWrapper) mpWrapper.clone();

			// set wms properties
			final MP_OLD clonedMP = clonedMpWrapper.getMapProvider();
			if (clonedMP instanceof MPWms) {

				final MPWms clonedWmsMp = (MPWms) clonedMP;

				// load images always transparent, a profile image has always a background color
				clonedWmsMp.setTransparent(true);

				// only the image size of 256 in the map profile is currently supported
				clonedWmsMp.setImageSize(256);
			}

			clonedMpWrapperList.add(clonedMpWrapper);
		}

		clonedMpProfile.fMpWrappers = clonedMpWrapperList;

		clonedMpProfile.fTileFactory = new TileFactoryProfile(clonedMpProfile);

		return clonedMpProfile;
	}

	/**
	 * Draw parent image by drawing all children over each other. When all children have errors, an
	 * image with the background color is returned
	 * 
	 * @param parentTile
	 * @return
	 */
	public ParentImageStatus createParentImage(final Tile parentTile) {

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

//		System.out.println();
//		System.out.println(parentTile.getTileKey());
//		// TODO remove SYSTEM.OUT.PRINTLN

		// loop: all children
		for (final Tile childTile : tileChildren) {

			final String childLoadingError = childTile.getLoadingError();

			if (childLoadingError != null) {

				// child is loaded but has errors

				isChildError = true;

				continue;
			}

			final ImageData[] childImageData = childTile.getChildImageData();

			if (childImageData == null || childImageData[0] == null) {

				// loading of this child has not yet finished
				isFinal = false;

				continue;
			}

			if (childTile.getMP().isProfileBrightness()) {

				// use the brightness of the current tile for the next tile

				brightnessTile = childTile;
				brightnessImageData = childImageData[0];

				continue;
			}

//			System.out.println("\t" + childTile.getUrl());
//			// TODO remove SYSTEM.OUT.PRINTLN

			// draw child image into the parent image
			parentImage.drawImage(childImageData[0], childTile, brightnessImageData, brightnessTile);

			brightnessTile = null;
			brightnessImageData = null;
		}

		return new ParentImageStatus(//
				new ImageData[] { parentImage.getImageData() },
				isFinal,
				fIsSaveImage && isChildError == false);
	}

	/**
	 * Creates tile children for all mp wrapper which are displayed in one tile
	 * 
	 * @param parentTile
	 * @param loadingTiles
	 * @return
	 */
	public ArrayList<Tile> createTileChildren(final Tile parentTile, final ConcurrentHashMap<String, Tile> loadingTiles) {

		final ArrayList<Tile> tileChildren = new ArrayList<Tile>();

		for (final MapProviderWrapper mpWrapper : fMpWrappers) {

			final int parentZoom = parentTile.getZoom();
			final MP_OLD mapProvider = mpWrapper.getMapProvider();

			if (parentZoom < mapProvider.getMinZoomLevel() || parentZoom > mapProvider.getMaxZoomLevel()) {

				// ignore map providers which does not support the current zoom level

				continue;
			}

			// create child tile for each visible map provider
			if (mpWrapper.isDisplayedInMap() && mpWrapper.isEnabled()) {

				final MP_OLD childMP = mapProvider;
				final TileFactory_OLD childTileFactory = childMP.getTileFactory(true);

				// check if this child is already being loaded
				final String childTileKey = Tile.getTileKey(
						childTileFactory,
						parentTile.getX(),
						parentTile.getY(),
						parentZoom,
						childMP.getId(),
						null,
						childTileFactory.getProjection().getId());

				Tile childTile = loadingTiles.get(childTileKey);

				if (childTile != null) {

					// child is currently being loaded or has a loading error

					if (childTile.isLoadingError()) {

						// update notify parent about the loading error

						parentTile.setChildLoadingError(childTile);

						continue;
					}

				} else {

					// create child tile

					childTile = new Tile(//
							childTileFactory,
							parentTile.getX(),
							parentTile.getY(),
							parentZoom,

							// create a unique tile for each child (map provider)
							childMP.getId());

					childTileFactory.doPostCreation(childTile);

					childTile.setParentTile(parentTile);
					childTile.setBoundingBoxEPSG4326();

					if (childTileFactory instanceof TileFactoryWms) {

						/*
						 * wms tiles must be saved separately because a map profile supports only
						 * 256
						 * pixel but these tiles can have other pixels in another wms map provider
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
	 * @param mapProvider
	 * @return
	 */
	private MapProviderWrapper createWrapper(final MP_OLD mapProvider) {

		MapProviderWrapper mpWrapper = null;

		try {

			final MP_OLD clonedMapProvider = (MP_OLD) mapProvider.clone();

			mpWrapper = new MapProviderWrapper(clonedMapProvider);

			// hide map provider but keep the layer visibility
			mpWrapper.setIsDisplayedInMap(false);

		} catch (final CloneNotSupportedException e) {
			StatusUtil.log(e);
		}

		return mpWrapper;
	}

	@Override
	public void disposeCachedImages() {
	// TODO Auto-generated method stub

	}

	/**
	 * @return Returns a list which contains wrappers for all none profile map providers
	 */
	public ArrayList<MapProviderWrapper> getAllWrappers() {
		return fMpWrappers;
	}

	public int getBackgroundColor() {
		return fBackgroundColor;
	}

	@Override
	public TileFactory_OLD getTileFactory(final boolean initTileFactory) {

		if (initTileFactory == false) {
			return fTileFactory;
		}

		if (fTileFactory == null) {
			synchronizeMPWrapper();
		}

		// initialize tile factory when it's not done yet
		fTileFactory.getInfo();

		return fTileFactory;
	}

	IPath getTileOSPath(final String fullPath, final int x, final int y, final int zoomLevel) {

		final IPath filePath = new Path(fullPath)//
				.append(getOfflineFolder())
				.append(Integer.toString(zoomLevel))
				.append(Integer.toString(x))
				.append(Integer.toString(y))
				.addFileExtension(MapProviderManager.getImageFileExtension(getImageFormat()));

		return filePath;
	}

	public void setBackgroundColor(final int backgroundColor) {
		fBackgroundColor = backgroundColor;
	}

	public void setBackgroundColor(final RGB rgb) {
		fBackgroundColor = ((rgb.red & 0xFF) << 0) | ((rgb.green & 0xFF) << 8) | ((rgb.blue & 0xFF) << 16);
	}

	public void setIsSaveImage(final boolean isSaveImage) {
		fIsSaveImage = isSaveImage;
		fTileFactory.setUseOfflineImage(isSaveImage);
	}

	@Override
	public void setZoomLevel(final int minZoom, final int maxZoom) {

		super.setZoomLevel(minZoom, maxZoom);

		// initialize zoom level in the tile factory info because the map gets zoom data from this location
		if (fTileFactory != null) {
			fTileFactory.getInfo().initializeZoomLevel(minZoom, maxZoom);
		}
	}

	/**
	 * Synchronizes two map providers
	 * 
	 * @param mpWrapper
	 * @param validMapProvider
	 * @return Returns <code>false</code> when the synchronization fails
	 */
	private boolean synchMpWrapper(final MapProviderWrapper mpWrapper, final MP_OLD validMapProvider) {

		final MP_OLD wrapperMapProvider = mpWrapper.getMapProvider();
		if (wrapperMapProvider == null) {

			/*
			 * wms map provider was not yet created, make a clone
			 */

			try {
				mpWrapper.setMapProvider((MP_OLD) validMapProvider.clone());
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

		final ArrayList<MP_OLD> allMPsWithoutProfile = MapProviderManager.getInstance().getAllMapProviders(false);

		if (fMpWrappers == null) {

			/*
			 * this case happens when a profile map provider is created, add all map providers
			 */

			fMpWrappers = new ArrayList<MapProviderWrapper>();

			for (final MP_OLD mapProvider : allMPsWithoutProfile) {
				fMpWrappers.add(createWrapper(mapProvider));
			}

		} else {

			/*
			 * synchronize profile mp wrapper with the available map providers
			 */

			final ArrayList<MapProviderWrapper> currentMpWrappers = fMpWrappers;
			final ArrayList<MapProviderWrapper> remainingMpWrappers = new ArrayList<MapProviderWrapper>(fMpWrappers);

			fMpWrappers = new ArrayList<MapProviderWrapper>();

			// loop: all available map providers
			for (final MP_OLD validMapProvider : allMPsWithoutProfile) {

				final String validMapProviderId = validMapProvider.getId();

				// check if a valid map provider is available in this profile
				boolean isMpValid = false;
				for (final MapProviderWrapper mpWrapper : currentMpWrappers) {

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
				final MapProviderWrapper mpWrapperClone = createWrapper(validMapProvider);

				fMpWrappers.add(mpWrapperClone);
			}

			if (remainingMpWrappers.size() > 0) {

				/*
				 * there are mappers in the profile which do not exist in the available map
				 * provider list, these mappers will be ignored
				 */
				final StringBuilder sb = new StringBuilder();

				sb.append(NLS.bind(Messages.DBG055_MapProfile_InvalidMapProvider, getName()));

				for (final MapProviderWrapper mpWrapper : remainingMpWrappers) {
					sb.append(mpWrapper.getMapProviderId());
					sb.append(UI.NEW_LINE);
				}

				StatusUtil.showStatus(sb.toString(), new Exception());
			}
		}

		sortMpWrapper(fMpWrappers);
		updateWrapperTileFactory(fMpWrappers);

		fTileFactory = new TileFactoryProfile(this);
	}
}
