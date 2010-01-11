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
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.graphics.ImageData;

import de.byteholder.geoclipse.map.ITileChildrenCreator;
import de.byteholder.geoclipse.map.ParentImageStatus;
import de.byteholder.geoclipse.map.Tile;
import de.byteholder.geoclipse.map.TileFactoryImpl;
import de.byteholder.geoclipse.map.TileFactoryInfo_OLD;

public class OLD_TileFactoryProfile extends TileFactoryImpl implements ITileChildrenCreator {

	private MPProfile				fProfileMapProvider;

	private ProfileTileFactoryInfo	fFactoryInfo;

	public ArrayList<Tile> createTileChildren(final Tile parentTile, final ConcurrentHashMap<String, Tile> loadingTiles) {
		return fProfileMapProvider.createTileChildren(parentTile, loadingTiles);
	}

//	/**
//	 * Clone constructor
//	 * 
//	 * @param mapProvider
//	 * @param tileFactory
//	 */
//	public TileFactoryProfile(final MPProfile mapProvider, final TileFactoryProfile tileFactory) {
//
//		fProfileMapProvider = mapProvider;
//
//		// create factory info
//		getInfo();
//	}

	@Override
	public TileFactoryInfo_OLD getInfo() {

		if (fFactoryInfo == null) {

			fFactoryInfo = new ProfileTileFactoryInfo();
			fFactoryInfo.mpProfile = fProfileMapProvider;

			final int minimumZoomLevel = fProfileMapProvider.getMinZoomLevel();
			final int maximumZoomLevel = fProfileMapProvider.getMaxZoomLevel();
			final int totalMapZoom = maximumZoomLevel;

			fFactoryInfo.initializeInfo(
					fProfileMapProvider.getId(),
					minimumZoomLevel,
					maximumZoomLevel,
					totalMapZoom,
					fProfileMapProvider.getImageSize());

			initializeTileFactory(fFactoryInfo);
		}

		return fFactoryInfo;
	}

	@Override
	public MP_OLD getMP() {
		return fProfileMapProvider;
	}

	public ParentImageStatus getParentImage(final Tile parentTile, final Tile childTile) {
		final ArrayList<Tile> tileChildren = parentTile.getChildren();
		if (tileChildren == null) {
			return null;
		}
		
		final ProfileTileImage parentImage = new ProfileTileImage();
		
		parentImage.setBackgroundColor(fProfileMapProvider.fBackgroundColor);
		
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
				fProfileMapProvider.fIsSaveImage && isChildError == false);
	}

	@Override
	public void setMapProvider(final MP_OLD mapProvider) {
	// this is done in the constructor
	}

	public TileFactoryProfile(final MPProfile mpProfile) {

		super();

		fProfileMapProvider = mpProfile;
	}

}

class ProfileTileFactoryInfo extends TileFactoryInfo_OLD {

	MPProfile	mpProfile;

	public ProfileTileFactoryInfo() {}

	@Override
	public String getFactoryID() {
		return mpProfile.getId();
	}

	@Override
	public String getFactoryName() {
		return mpProfile.getName();
	}

	@Override
	public String getTileOSFolder() {
		return mpProfile.getOfflineFolder();
	}

	@Override
	public IPath getTileOSPath(final String fullPath, final int x, final int y, final int zoomLevel, final Tile tile) {
		return mpProfile.getTileOSPath(fullPath, x, y, zoomLevel);
	}

	@Override
	public String getTileUrl(final Tile tile) {
		return null;
	}

	@Override
	public void setFactoryId(final String factoryId) {

		mpProfile.setId(factoryId);

		/*
		 * !!! very importand !!!
		 * the factory id must be set in the superclass to make the tile factory
		 * info unique, otherwise factorId is null and all created custom tile factory infos cannot
		 * be distinguished with the equals/hashcode methods
		 */
		super.setFactoryId(factoryId);
	}
}
