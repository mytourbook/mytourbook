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
package de.byteholder.geoclipse.tileinfo;

import de.byteholder.geoclipse.map.Tile;
import de.byteholder.geoclipse.map.TileFactory;
import de.byteholder.geoclipse.map.event.ITileListener;
import de.byteholder.geoclipse.map.event.TileEventId;

public class TileInfoManager implements ITileListener {

	private static TileInfoManager	fInstance;

//	private ArrayList<TileFactory>	fTileFactories;

	private TileInfoContribution	fTileInfo;

	public static TileInfoManager getInstance() {

		if (fInstance == null) {
			fInstance = new TileInfoManager();
		}

		return fInstance;
	}

	/**
	 * The constructor.
	 */
	public TileInfoManager() {
//		getMapProviders();
		TileFactory.addTileListener(this);
	}

//	private void getMapProviders() {
//
//		final List<TileFactory> allPluginTileFactories = GeoclipseExtensions.getInstance().readFactories();
//		fTileFactories = new ArrayList<TileFactory>();
//
//		// get visible map providers
//		for (final TileFactory tileFactory : allPluginTileFactories) {
//
//			if (tileFactory.getInfo().isMapEmpty()) {
//				// ignore maps which does not display anything
//				continue;
//			}
//
//			fTileFactories.add(tileFactory);
//
//		}
//	}

	public void setTileInfoContribution(final TileInfoContribution tileInfoContribution) {
		fTileInfo = tileInfoContribution;
	}

	public void tileEvent(final TileEventId tileEventId, final Tile tile) {

		// check widget, can be null when it's hidden
		if (fTileInfo == null) {
			return;
		}

		fTileInfo.updateInfo(tileEventId);
	}

	/**
	 * Loading SRTM data
	 * 
	 * @param tileEvent
	 * @param remoteName
	 * @param receivedBytes
	 */
	public void updateSRTMTileInfo(final TileEventId tileEvent, final String remoteName, final long receivedBytes) {

		// check widget, can be null when it's hidden
		if (fTileInfo == null) {
			return;
		}

		fTileInfo.updateSRTMInfo(tileEvent, remoteName, receivedBytes);
	}

}
