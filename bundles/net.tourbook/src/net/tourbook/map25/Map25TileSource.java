/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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
package net.tourbook.map25;

import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.source.ITileDecoder;
import org.oscim.tiling.source.UrlTileDataSource;
import org.oscim.tiling.source.UrlTileSource;

public class Map25TileSource extends UrlTileSource {

	private static Map25Provider _mapProvider;

	public static class Builder<T extends Builder<T>> extends UrlTileSource.Builder<T> {

		public Builder() {
			super(_mapProvider.online_url, _mapProvider.online_TilePath, 1, 17);
		}

		@Override
		public Map25TileSource build() {
			return new Map25TileSource(this);
		}
	}

	private Map25TileSource(final Builder<?> builder) {
		super(builder);
	}

	@SuppressWarnings("rawtypes")
	public static Builder<?> builder(final Map25Provider mapProvider) {

		_mapProvider = mapProvider;

		return new Builder();
	}

	@Override
	public ITileDataSource getDataSource() {

		ITileDecoder tileDecoder;

		switch (_mapProvider.tileEncoding) {
		case MVT:

			tileDecoder = new org.oscim.tiling.source.mvt.TileDecoder();

			break;

		case VTM:
		default:
			tileDecoder = new org.oscim.tiling.source.oscimap4.TileDecoder();
			break;
		}

		return new UrlTileDataSource(this, tileDecoder, getHttpEngine());
	}
}
