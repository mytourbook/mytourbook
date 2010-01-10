/*******************************************************************************
 * Copyright (C) 2005, 2010 Wolfgang Schramm and Contributors
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

import de.byteholder.geoclipse.map.TileFactory_OLD;
import de.byteholder.geoclipse.map.TileFactoryInfo_OLD;

/**
 * This is a map provider for a plugin tile factory. <br>
 * <br>
 * The plugin tile factory must implement the
 * methods {@link TileFactory_OLD#setMapProvider(MP_OLD)} and {@link TileFactory_OLD#getMapProvider()}
 */
public class MPPlugin extends MP_OLD {

	private TileFactory_OLD	fTileFactory;

	public MPPlugin(final TileFactory_OLD tileFactory) {

		fTileFactory = tileFactory;

		tileFactory.setMapProvider(this);

		final TileFactoryInfo_OLD factoryInfo = tileFactory.getInfo();

		setMapProviderId(factoryInfo.getFactoryID());
		setName(factoryInfo.getFactoryName());
		setOfflineFolder(factoryInfo.getTileOSFolder());
	}

	@Override
	public void disposeCachedImages() {
		fTileFactory.disposeCachedImages();
	}

	@Override
	public TileFactory_OLD getTileFactory(final boolean initTileFactory) {
		return fTileFactory;
	}
}
