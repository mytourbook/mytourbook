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
import de.byteholder.geoclipse.map.UI;

public class MPCustom extends MP {

	private String				fCustomUrl	= UI.EMPTY_STRING;

	private ArrayList<UrlPart>	fUrlParts	= new ArrayList<UrlPart>();

	public MPCustom() {

	}

	@Override
	public Object clone() throws CloneNotSupportedException {

		final MPCustom mapProvider = (MPCustom) super.clone();

		mapProvider.fCustomUrl = new String(fCustomUrl);

		// clone url parts
		final ArrayList<UrlPart> newUrlParts = new ArrayList<UrlPart>();
		for (final UrlPart urlPart : fUrlParts) {
			newUrlParts.add((UrlPart) urlPart.clone());
		}
		mapProvider.fUrlParts = newUrlParts;

		return mapProvider;
	}

	public String getCustomUrl() {
		return fCustomUrl;
	}

	@Override
	public IPath getTileOSPath(final String fullPath, final Tile tile) {

		IPath filePath = new Path(fullPath);

		filePath = filePath.append(getOfflineFolder());

		filePath = filePath//
				.append(Integer.toString(tile.getZoom()))
				.append(Integer.toString(tile.getX()))
				.append(Integer.toString(tile.getY()))
				.addFileExtension(MapProviderManager.getImageFileExtension(getImageFormat()));

		return filePath;
	}

	@Override
	public String getTileUrl(final Tile tile) {

		if (fUrlParts.size() == 0) {

			// url parts are not set yet, display openstreetmap

			return MapProviderManager.getInstance().getDefaultMapProvider().getTileUrl(tile);

		} else {

			final StringBuilder sb = new StringBuilder();

			for (final UrlPart urlPart : fUrlParts) {
				switch (urlPart.getPartType()) {

				case HTML:
					sb.append(urlPart.getHtml());
					break;

				case X:
					sb.append(Integer.toString(tile.getX()));
					break;

				case Y:
					sb.append(Integer.toString(tile.getY()));
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
					sb.append(Integer.toString(tile.getZoom()));
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

}
