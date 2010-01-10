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

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import de.byteholder.geoclipse.map.Tile;

class OSMMapProvider extends MPPlugin {

	static final String			FACTORY_ID		= "osm";									//$NON-NLS-1$
	private static final String	OFFLINE_FOLDER	= "osm";									//$NON-NLS-1$
	private static final String	FACTORY_NAME	= "OpenStreetMap";							//$NON-NLS-1$

	private static final String	SEPARATOR		= "/";										//$NON-NLS-1$

	private static final int	MIN_ZOOM		= 0;
	private static final int	MAX_ZOOM		= 17;

// mp2, I have no idea for what this is used	
//	private static final int	TOTAL_ZOOM		= 17;

	private static final String	BASE_URL		= "http://tile.openstreetmap.org";			//$NON-NLS-1$
	private static final String	FILE_EXT		= MapProviderManager.FILE_EXTENSION_PNG;

	public OSMMapProvider() {

		setZoomLevel(MIN_ZOOM, MAX_ZOOM);
	}

	@Override
	public String getBaseURL() {
		return BASE_URL;
	}

	@Override
	public String getId() {
		return FACTORY_ID;
	}

	@Override
	public String getName() {
		return FACTORY_NAME;
	}

	@Override
	public String getTileOSFolder() {
		return OFFLINE_FOLDER;
	}

	@Override
	public IPath getTileOSPath(final String fullPath, final Tile tile) {

		return new Path(fullPath)//
				.append(OFFLINE_FOLDER)
				.append(Integer.toString(tile.getZoom()))
				.append(Integer.toString(tile.getX()))
				.append(Integer.toString(tile.getY()))
				.addFileExtension(FILE_EXT);
	}

	@Override
	public String getTileUrl(final int x, final int y, final int zoom, final Tile tile) {

		return new StringBuilder()//
				.append(BASE_URL)
				.append(SEPARATOR)
				.append(zoom)
				.append(SEPARATOR)
				.append(x)
				.append(SEPARATOR)
				.append(y)
				.append('.')
				.append(FILE_EXT)
				.toString();
	}

	@Override
	public void setTileOSFolder(final String offlineFolder) {
	// folder is static, nothing to do
	}

// mp2	
//	public OSMMapProvider() {
//		super(FACTORY_ID, MIN_ZOOM, MAX_ZOOM, TOTAL_ZOOM, 256, true, true, BASE_URL, "x", "y", "z"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
//	}
//	
//	@Override
//	public String getFactoryID() {
//		return FACTORY_ID;
//	}
//
//	@Override
//	public String getFactoryName() {
//		return FACTORY_NAME;
//	}
//
//	@Override
//	public String getTileOSFolder() {
//		return FACTORY_OS_NAME;
//	}
}

// mp2
//
///**
// * @author Michael Kanis
// */
//public class TileFactoryOSM extends TileFactoryImpl {
//
//	private static OSMTileFactoryInfo	info	= new OSMTileFactoryInfo();
//	private MP_OLD							fMp;
//
//	public TileFactoryOSM() {
//		super(info);
//	}
//
//	@Override
//	public MP_OLD getMapProvider() {
//		return fMp;
//	}
//
//	@Override
//	public void setMapProvider(final MP_OLD mapProvider) {
//		fMp = mapProvider;
//	}
//}
