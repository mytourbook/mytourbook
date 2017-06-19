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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import net.tourbook.common.util.StatusUtil;

import org.oscim.core.Tile;
import org.oscim.tiling.ITileCache;
import org.oscim.tiling.source.UrlTileSource;

import okhttp3.Cache;
import okhttp3.HttpUrl;

public class TileCacheMT implements ITileCache {

	private static final String	CACHE_DIR	= OkHttpEngineMT.getCacheDir();
	private static final Path	CACHE_PATH	= Paths.get(CACHE_DIR);

	private UrlTileSource		_tileSource;

	public TileCacheMT(final UrlTileSource tileSource) {

		_tileSource = tileSource;
	}

	private String getCacheKey(final Tile tile) {

		try {

			final String tileUrl = _tileSource.getTileUrl(tile);
			final HttpUrl httpUrl = HttpUrl.get(new URL(tileUrl));
			final String cacheKey = Cache.key(httpUrl);

			return cacheKey;

		} catch (final MalformedURLException e) {
			StatusUtil.log(e);
		}

		return null;
	}

	@Override
	public TileReader getTile(final Tile tile) {

		final String cacheKey = getCacheKey(tile);
		final String cacheFileName = cacheKey + ".1";
		final Path cacheFilePath = CACHE_PATH.resolve(cacheFileName);

		if (Files.exists(cacheFilePath)) {

			return new TileReader() {

				@Override
				public InputStream getInputStream() {

					try {

						return Files.newInputStream(cacheFilePath);

					} catch (final IOException e) {
						StatusUtil.log(e);
					}

					return null;
				}

				@Override
				public Tile getTile() {
					return tile;
				}
			};
		}

		return null;
	}

	@Override
	public void setCacheSize(final long size) {}

	@Override
	public TileWriter writeTile(final Tile tile) {

		return new TileWriter() {

			@Override
			public void complete(final boolean success) {}

			@Override
			public OutputStream getOutputStream() {

				final String cacheKey = getCacheKey(tile);
				final String cacheFileName = cacheKey + ".dummy";
				final Path cacheFilePath = CACHE_PATH.resolve(cacheFileName);

				try {

					return Files.newOutputStream(cacheFilePath);

				} catch (final IOException e) {
					StatusUtil.log(e);
				}

				return null;
			}

			@Override
			public Tile getTile() {
				return tile;
			}
		};
	}

}
