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

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import net.tourbook.map25.HttpLoggingInterceptorMT.Level;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.oscim.tiling.source.HttpEngine;
import org.oscim.tiling.source.OkHttpEngine;
import org.oscim.tiling.source.UrlTileSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class OkHttpEngineMT extends OkHttpEngine {

	public static final Logger						log			= LoggerFactory.getLogger(OkHttpEngineMT.class);

	private static OkHttpClient						_httpClient;
	private static Cache							_httpCache;

	private static final HttpLoggingInterceptorMT	LOGGING_INTERCEPTOR;
	private static final Interceptor				REWRITE_CACHE_CONTROL_INTERCEPTOR;

	public static boolean							_isLogHttp	= false;
	static {

		LOGGING_INTERCEPTOR = new HttpLoggingInterceptorMT().setLevel(Level.BODY);

		REWRITE_CACHE_CONTROL_INTERCEPTOR = new Interceptor() {
			@Override
			public Response intercept(final Chain chain) throws IOException {

				final Response originalResponse = chain.proceed(chain.request());

				return originalResponse
						.newBuilder()
						.header(
								"Cache-Control",
								"public, max-age=31536000") // 365 days
						.build();
			}
		};
	}

	public static class OkHttpFactoryMT extends OkHttpEngine.OkHttpFactory {

		/**
		 * Use custom factory to set more parameters
		 */
		public OkHttpFactoryMT() {

			_httpCache = new Cache(new File(getCacheDir()), Long.MAX_VALUE);

			final OkHttpClient.Builder httpBuilder = new OkHttpClient.Builder();

			httpBuilder

					.cache(_httpCache)

					.connectTimeout(60, TimeUnit.SECONDS)
					.readTimeout(60, TimeUnit.SECONDS)
					.writeTimeout(60, TimeUnit.SECONDS)

					.addNetworkInterceptor(REWRITE_CACHE_CONTROL_INTERCEPTOR);

			if (_isLogHttp) {
				httpBuilder.addInterceptor(LOGGING_INTERCEPTOR);
			}

			_httpClient = httpBuilder.build();
		}

		@Override
		public HttpEngine create(final UrlTileSource tileSource) {

//			LwHttp.setSocketTimeout(1000);

			return new OkHttpEngineMT(_httpClient, tileSource);
		}
	}

	public OkHttpEngineMT(final OkHttpClient client, final UrlTileSource tileSource) {
		super(client, tileSource);
	}

	/**
	 * @return Returns folder location where http requests are cached.
	 */
	static String getCacheDir() {

		final String workingDirectory = Platform.getInstanceLocation().getURL().getPath();

		final IPath tileCachePath = new Path(workingDirectory).append("vtm-tile-cache");

		if (tileCachePath.toFile().exists() == false) {
			tileCachePath.toFile().mkdirs();
		}

		return tileCachePath.toOSString();
	}

	static Cache getHttpCache() {
		return _httpCache;
	}
}
