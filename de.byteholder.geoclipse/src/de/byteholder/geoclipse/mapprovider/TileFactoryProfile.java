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

import de.byteholder.geoclipse.map.ITileChildrenCreator;
import de.byteholder.geoclipse.map.ParentImageStatus;
import de.byteholder.geoclipse.map.Tile;
import de.byteholder.geoclipse.map.TileFactoryImpl;
import de.byteholder.geoclipse.map.TileFactoryInfo;

class ProfileTileFactoryInfo extends TileFactoryInfo {

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
	public String getTileUrl(final int x, final int y, final int zoom, final Tile tile) {
		return null;
	}

	@Override
	public void setFactoryId(final String factoryId) {

		mpProfile.setMapProviderId(factoryId);

		/*
		 * !!! very importand !!!
		 * the factory id must be set in the superclass to make the tile factory
		 * info unique, otherwise factorId is null and all created custom tile factory infos cannot
		 * be distinguished with the equals/hashcode methods
		 */
		super.setFactoryId(factoryId);
	}
}

public class TileFactoryProfile extends TileFactoryImpl implements ITileChildrenCreator {

	private MPProfile				fProfileMapProvider;

	private ProfileTileFactoryInfo	fFactoryInfo;

	public TileFactoryProfile(final MPProfile mpProfile) {

		super();

		fProfileMapProvider = mpProfile;
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

	public ArrayList<Tile> createTileChildren(final Tile parentTile, final ConcurrentHashMap<String, Tile> loadingTiles) {
		return fProfileMapProvider.createTileChildren(parentTile, loadingTiles);
	}

	@Override
	public TileFactoryInfo getInfo() {

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
	public MP getMapProvider() {
		return fProfileMapProvider;
	}

	public ParentImageStatus getParentImage(final Tile parentTile, final Tile childTile) {
		return fProfileMapProvider.createParentImage(parentTile);
	}

	@Override
	public void setMapProvider(final MP mapProvider) {
	// this is done in the constructor
	}

}
