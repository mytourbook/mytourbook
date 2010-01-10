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

import java.io.InputStream;

import org.eclipse.core.runtime.IPath;

import de.byteholder.geoclipse.logging.GeoException;
import de.byteholder.geoclipse.map.ITileLoader;
import de.byteholder.geoclipse.map.Tile;
import de.byteholder.geoclipse.map.TileFactoryImpl;
import de.byteholder.geoclipse.map.TileFactoryInfo_OLD;

public class TileFactoryWms extends TileFactoryImpl {

	private MPWms				fMpWms;

	private WmsTileFactoryInfo	fFactoryInfo;

	public TileFactoryWms(final MPWms wmsMapProvider) {
		fMpWms = wmsMapProvider;
	}

	/**
	 * Clone constructor
	 * 
	 * @param mapProvider
	 * @param fTileFactory
	 */
	public TileFactoryWms(final MPWms mapProvider, final TileFactoryWms fTileFactory) {

		fMpWms = mapProvider;

		// create factory info
		getInfo();
	}

	@Override
	public TileFactoryInfo_OLD getInfo() {

		if (fFactoryInfo == null) {

			/*
			 * factory info is stored in the map provider
			 */

			fFactoryInfo = new WmsTileFactoryInfo();
			fFactoryInfo.mpWms = fMpWms;

			final int minimumZoomLevel = fMpWms.getMinZoomLevel();
			final int maximumZoomLevel = fMpWms.getMaxZoomLevel();
			final int totalMapZoom = maximumZoomLevel;

			fFactoryInfo.initializeInfo(//
					fMpWms.getId(),
					minimumZoomLevel,
					maximumZoomLevel,
					totalMapZoom,
					fMpWms.getImageSize());

			initializeTileFactory(fFactoryInfo);
		}

		return fFactoryInfo;
	}

	@Override
	public MP_OLD getMapProvider() {
		return fMpWms;
	}

	@Override
	public int getTileSize() {
		return fMpWms.getImageSize();
	}

	@Override
	public void setMapProvider(final MP_OLD mapProvider) {
	// this is done in the constructor
	}
}

/**
 * Wrapper which wraps all methods to the {@link MPWms}
 */
class WmsTileFactoryInfo extends TileFactoryInfo_OLD implements ITileLoader {

	MPWms	mpWms;

	public WmsTileFactoryInfo() {}

	@Override
	public String getCustomTileKey() {
		return mpWms.getCustomTileKey();
	}

	@Override
	public String getFactoryID() {
		return mpWms.getId();
	}

	@Override
	public String getFactoryName() {
		return mpWms.getName();
	}

	public InputStream getTileImageStream(final Tile tile) throws GeoException {
		return mpWms.getTileImageStream(tile);
	}

	@Override
	public ITileLoader getTileLoader() {
		return this;
	}

	@Override
	public String getTileOSFolder() {
		return mpWms.getOfflineFolder();
	}

	@Override
	public IPath getTileOSPath(final String fullPath, final int x, final int y, final int zoomLevel, final Tile tile) {
		return mpWms.getTileOSPath(fullPath, x, y, zoomLevel);
	}

	@Override
	public String getTileUrl(final int x, final int y, final int zoom, final Tile tile) {
		// this map provider is supporting the ITileLoader interface
		return null;
	}

	@Override
	public void setFactoryId(final String factoryId) {

		mpWms.setMapProviderId(factoryId);

		/*
		 * !!! very importand !!!
		 * the factory id must be set in the superclass to make the tile factory
		 * info unique, otherwise factorId is null and all created custom tile factory infos cannot
		 * be distinguished with the equals/hashcode methods
		 */
		super.setFactoryId(factoryId);
	}

}
