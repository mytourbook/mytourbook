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

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import de.byteholder.geoclipse.map.Tile;
import de.byteholder.geoclipse.map.TileFactoryInfo;
import de.byteholder.geoclipse.swt.UI;

public class MPCustom extends MP {

	private String				fCustomUrl	= UI.EMPTY_STRING;

	private TileFactoryCustom	fTileFactory;

	private ArrayList<UrlPart>	fUrlParts	= new ArrayList<UrlPart>();

	private TileFactoryInfo		fDefaultFactoryInfo;

	public MPCustom() {
		fTileFactory = new TileFactoryCustom(this);
	}

	@Override
	public Object clone() throws CloneNotSupportedException {

		final MPCustom mapProvider = (MPCustom) super.clone();

		mapProvider.fCustomUrl = new String(fCustomUrl);

		mapProvider.fTileFactory = new TileFactoryCustom(mapProvider, fTileFactory);

		// clone url parts
		final ArrayList<UrlPart> newUrlParts = new ArrayList<UrlPart>();
		for (final UrlPart urlPart : fUrlParts) {
			newUrlParts.add((UrlPart) urlPart.clone());
		}
		mapProvider.fUrlParts = newUrlParts;

		return mapProvider;
	}

	@Override
	public void disposeCachedImages() {
		fTileFactory.disposeCachedImages();
	}

	public String getCustomUrl() {
		return fCustomUrl;
	}

	@Override
	public TileFactoryCustom getTileFactory() {

		// initialize tile factory when it's not yet done
		fTileFactory.getInfo();

		return fTileFactory;
	}

	public IPath getTileOSPath(final String fullPath, final int x, final int y, final int zoomLevel) {

		IPath filePath = new Path(fullPath);
		filePath = filePath.append(getOfflineFolder());

		filePath = filePath
				.append(Integer.toString(zoomLevel))
				.append(Integer.toString(x))
				.append(Integer.toString(y))
				.addFileExtension(MapProviderManager.getFileExtension(getImageFormat()));

		return filePath;
	}

	public String getTileUrl(final int x, final int y, final int zoom, final Tile tile) {

		if (fUrlParts.size() == 0) {

			// url parts are not set yet, display openstreetmap

			if (fDefaultFactoryInfo == null) {
				fDefaultFactoryInfo = MapProviderManager
						.getInstance()
						.getDefaultMapProvider()
						.getTileFactory()
						.getInfo();
			}

			return fDefaultFactoryInfo.getTileUrl(x, y, zoom, tile);

		} else {

			final StringBuilder sb = new StringBuilder();

			for (final UrlPart urlPart : fUrlParts) {
				switch (urlPart.getPartType()) {

				case HTML:
					sb.append(urlPart.getHtml());
					break;

				case X:
					sb.append(Integer.toString(x));
					break;

				case Y:
					sb.append(Integer.toString(y));
					break;

//				case LAT_TOP:
//					sb.append(Double.toString(tile.getBboxLatitudeTop()));
//					break;
//
//				case LAT_BOTTOM:
//					sb.append(Double.toString(tile.getBboxLatitudeBottom()));
//					break;
//
//				case LON_LEFT:
//					sb.append(Double.toString(tile.getBboxLongitudeLeft()));
//					break;
//
//				case LON_RIGHT:
//					sb.append(Double.toString(tile.getBboxLongitudeRight()));
//					break;

				case ZOOM:
					sb.append(Integer.toString(zoom));
					break;

				case RANDOM_INTEGER:

					final int startValue = urlPart.getRandomIntegerStart();
					final int endValue = urlPart.getRandomIntegerEnd();
					final int randomDiff = endValue - startValue + 1;

					final double random = Math.random() * randomDiff + startValue;
					final int randomInt = (int) random;

					sb.append(randomInt);

					break;

				default:
					break;
				}
			}

			return sb.toString();
		}
	}

	public ArrayList<UrlPart> getUrlParts() {
		return fUrlParts;
	}

	public void setCustomUrl(final String customUrl) {
		fCustomUrl = customUrl;
	}

	public void setUrlParts(final ArrayList<UrlPart> urlParts) {

		fUrlParts.clear();

		fUrlParts = urlParts;
	}

	@Override
	public void setZoomLevel(final int minZoom, final int maxZoom) {

		super.setZoomLevel(minZoom, maxZoom);

		// initialize the new zoom level by setting internal data
		if (fTileFactory != null) {
			fTileFactory.getInfo().initializeZoomLevel(minZoom, maxZoom);
		}

	}

}
