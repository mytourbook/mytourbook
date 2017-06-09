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
package net.tourbook.map.vtm;

import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.source.UrlTileDataSource;
import org.oscim.tiling.source.UrlTileSource;
import org.oscim.tiling.source.mvt.TileDecoder;

public class CustomTileSource extends UrlTileSource {

//	http://192.168.99.99:8080/all/16/19293/24641.mvt

	private final static String	DEFAULT_URL		= "http://192.168.99.99:8080/all";
	private final static String	DEFAULT_PATH	= "/{Z}/{X}/{Y}.mvt";

	public static class Builder<T extends Builder<T>> extends UrlTileSource.Builder<T> {

		public Builder() {
			super(DEFAULT_URL, DEFAULT_PATH, 1, 17);
		}

		@Override
		public CustomTileSource build() {
			return new CustomTileSource(this);
		}
	}

	private CustomTileSource(final Builder<?> builder) {

		super(builder);

//		final TileCacheMT cache = new TileCacheMT(this);
//		this.setCache(cache);
	}

	@SuppressWarnings("rawtypes")
	public static Builder<?> builder() {
		return new Builder();
	}

	@Override
	public ITileDataSource getDataSource() {

		return new UrlTileDataSource(this, new TileDecoder(), getHttpEngine());
	}
}
