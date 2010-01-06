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

import org.eclipse.core.runtime.IPath;

import de.byteholder.geoclipse.map.Tile;
import de.byteholder.geoclipse.map.TileFactoryImpl;
import de.byteholder.geoclipse.map.TileFactoryInfo;

class CustomTileFactoryInfo extends TileFactoryInfo {

	MPCustom	customMapProvider;

	public CustomTileFactoryInfo() {}

	@Override
	public String getFactoryID() {
		return customMapProvider.getId();
	}

	@Override
	public String getFactoryName() {
		return customMapProvider.getName();
	}

	@Override
	public String getTileOSFolder() {
		return customMapProvider.getOfflineFolder();
	}

	@Override
	public IPath getTileOSPath(final String fullPath, final int x, final int y, final int zoomLevel, final Tile tile) {
		return customMapProvider.getTileOSPath(fullPath, x, y, zoomLevel);
	}

	@Override
	public String getTileUrl(final int x, final int y, final int zoom, final Tile tile) {
		return customMapProvider.getTileUrl(x, y, zoom, tile);
	}

	@Override
	public void setFactoryId(final String factoryId) {

		customMapProvider.setMapProviderId(factoryId);

		/*
		 * !!! very importand !!!
		 * the factory id must be set in the superclass to make the tile factory
		 * info unique, otherwise factorId is null and all created custom tile factory infos cannot
		 * be distinguished with the equals/hashcode methods
		 */
		super.setFactoryId(factoryId);
	}
}

public class TileFactoryCustom extends TileFactoryImpl {

	private MPCustom				fCustomMapProvider;

	private CustomTileFactoryInfo	fFactoryInfo;

	public TileFactoryCustom(final MPCustom customMapProvider) {

		super();

		fCustomMapProvider = customMapProvider;
	}

	/**
	 * Clone constructor
	 * 
	 * @param mapProvider
	 * @param tileFactory
	 */
	public TileFactoryCustom(final MPCustom mapProvider, final TileFactoryCustom tileFactory) {

		fCustomMapProvider = mapProvider;

		// create factory info
		getInfo();
	}

	@Override
	public TileFactoryInfo getInfo() {

		if (fFactoryInfo == null) {

			fFactoryInfo = new CustomTileFactoryInfo();
			fFactoryInfo.customMapProvider = fCustomMapProvider;

			final int minimumZoomLevel = fCustomMapProvider.getMinZoomLevel();
			final int maximumZoomLevel = fCustomMapProvider.getMaxZoomLevel();
			final int totalMapZoom = maximumZoomLevel;

			fFactoryInfo.initializeInfo(
					fCustomMapProvider.getId(),
					minimumZoomLevel,
					maximumZoomLevel,
					totalMapZoom,
					fCustomMapProvider.getImageSize());

			initializeTileFactory(fFactoryInfo);
		}

		return fFactoryInfo;
	}

	@Override
	public MP getMapProvider() {
		return fCustomMapProvider;
	}

	@Override
	public void setMapProvider(final MP mp) {
	// this is done in the constructor
	}

}
