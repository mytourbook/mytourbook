/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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
package net.tourbook.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/*
 * Part of this code is copied (23.11.2014) from
 * http://www.microhowto.info/howto/serve_web_pages_using_an_embedded_http_server_in_java.html
 */
public class WebContentServer {

	private static boolean					IS_DEBUG_PORT				= true;
	private static final int				NUMBER_OF_SERVER_THREADS	= 1;

	// logs: time, url
	private static boolean					LOG_URL						= true;
	private static boolean					LOG_DOJO					= false;

	// logs: header
	private static boolean					LOG_HEADER					= false;

	// logs: xhr
	private static boolean					LOG_XHR						= true;

	private static final String				ROOT_FILE_PATH_NAME			= "/";							//$NON-NLS-1$

	private static final String				PROTOCOL_HTTP				= "http://";					//$NON-NLS-1$
	private static final String				PROTOCOL_COLUMN				= ":";							//$NON-NLS-1$

	private static final String				REQUEST_PATH_DOJO			= "/WebContent-dojo";			//$NON-NLS-1$
	private static final String				REQUEST_PATH_FIREBUG_LITE	= "/WebContent-firebug-lite";	//$NON-NLS-1$

	/**
	 * Path for custom dojo widgets
	 */
	private static final String				REQUEST_PATH_TOURBOOK		= "/tourbook";					//$NON-NLS-1$

	private static final String				XHR_HEADER_KEY				= "X-requested-with";			//$NON-NLS-1$
	private static final String				XHR_HEADER_VALUE			= "XMLHttpRequest";			//$NON-NLS-1$

	static String							SERVER_URL;

	private static Map<String, XHRHandler>	_allXHRHandler				= new HashMap<>();

	private static HttpServer				_server;
	private static final int				_serverPort;

	private static InetSocketAddress		inetAddress;

	private String[]						set;

	static {

		if (IS_DEBUG_PORT) {
			_serverPort = 24114;
		} else {
			_serverPort = PortFinder.findFreePort();
		}

		final InetAddress loopbackAddress = InetAddress.getLoopbackAddress();
		inetAddress = new InetSocketAddress(loopbackAddress, _serverPort);

		SERVER_URL = PROTOCOL_HTTP + loopbackAddress.getHostAddress() + PROTOCOL_COLUMN + _serverPort;
	}

	private static class DefaultHandler implements HttpHandler {

		@Override
		public void handle(final HttpExchange httpExchange) throws IOException {
			WebContentServer.handle(httpExchange);
		}
	}

	/**
	 * @param xhrKey
	 * @param xhrHandler
	 * @return Returns the previous handler or <code>null</code> when a handler is not available.
	 */
	public static XHRHandler addXHRHandler(final String xhrKey, final XHRHandler xhrHandler) {

		return _allXHRHandler.put(xhrKey, xhrHandler);
	}

	private static void handle(final HttpExchange httpExchange) {

		final long start = System.nanoTime();

		final StringBuilder log = new StringBuilder();

		try {

			boolean isResourceUrl = false;

			final String rootPath = WEB.getFile(ROOT_FILE_PATH_NAME).getCanonicalFile().getPath();

			final URI requestURI = httpExchange.getRequestURI();
			final String requestUriPath = requestURI.getPath();

			final Headers requestHeaders = httpExchange.getRequestHeaders();
			final Set<Entry<String, List<String>>> headerEntries = requestHeaders.entrySet();

			final String xhrValue = requestHeaders.getFirst(XHR_HEADER_KEY);
			final boolean isXHR = XHR_HEADER_VALUE.equals(xhrValue);
			final boolean isDojo = requestUriPath.startsWith(REQUEST_PATH_DOJO);

			if (isDojo) {
				if (LOG_DOJO) {
					log.append(requestUriPath);
				}
			} else {

				if (LOG_URL || LOG_XHR) {
					log.append(requestUriPath);
				}
				if (LOG_URL && LOG_XHR) {
					logParameter(httpExchange, log);
				}
				if (LOG_HEADER && isXHR) {
					logHeader(log, headerEntries);
				}
			}

			if (isXHR) {

				// XHR request

				handle_XHR(httpExchange, requestUriPath, log);

			} else {

				String requestedOSPath = null;

				if (isDojo) {

					// Dojo requests

					isResourceUrl = true;
					requestedOSPath = "C:/E/js-resources/dojo/" + requestUriPath;

				} else if (requestUriPath.startsWith(REQUEST_PATH_TOURBOOK)) {

					// Tourbook widget requests

					isResourceUrl = true;
					requestedOSPath = rootPath + requestUriPath;

				} else if (requestUriPath.startsWith(REQUEST_PATH_FIREBUG_LITE)) {

					// Firebug lite requests

					isResourceUrl = true;
					requestedOSPath = "C:/E/XULRunner/" + requestUriPath;

				} else {

					// default request

					requestedOSPath = rootPath + requestUriPath;
				}

				if (requestedOSPath != null) {

					final File file = new File(requestedOSPath).getCanonicalFile();

					if (!file.getPath().startsWith(rootPath) && !isResourceUrl) {

						// Suspected path traversal attack: reject with 403 error.

						handle_403(httpExchange, file);

					} else if (!file.isFile()) {

						// Object does not exist or is not a file: reject with 404 error.

						handle_404(httpExchange, file, requestUriPath);

					} else {

						// Object exists and is a file: accept with response code 200.

						handle_File(httpExchange, file);
					}
				}
			}

		} catch (final Exception e) {
			StatusUtil.log(e);
		} finally {

			if (log.length() > 0 && (LOG_URL || LOG_XHR || LOG_HEADER || LOG_DOJO)) {

				final String msg = String.format("%s %5.1f ms  %-16s  %s", //
						UI.timeStampNano(),
						(float) (System.nanoTime() - start) / 1000000,
						Thread.currentThread().getName(),
						log);

				System.out.println(msg);
			}
		}
	}

	private static void handle_403(final HttpExchange httpExchange, final File file) {

		OutputStream os = null;

		try {

			final String response = "403 (Forbidden)\n";
			httpExchange.sendResponseHeaders(403, response.length());

			os = httpExchange.getResponseBody();
			os.write(response.getBytes());

			StatusUtil.log(response + " " + file.getPath());

		} catch (final Exception e) {
			StatusUtil.log(e);
		} finally {
			Util.close(os);
		}
	}

	private static void handle_404(final HttpExchange httpExchange, final File file, final String requestUriPath) {

		OutputStream os = null;

		try {

			final String response = String.format("%s\n404 (Not Found)\n", requestUriPath);
			httpExchange.sendResponseHeaders(404, response.length());

			os = httpExchange.getResponseBody();
			os.write(response.getBytes());

			StatusUtil.log(response + " " + file.getPath());

		} catch (final Exception e) {
			StatusUtil.log(e);
		} finally {
			Util.close(os);
		}
	}

	private static void handle_File(final HttpExchange httpExchange, final File file) {

		FileInputStream fs = null;
		OutputStream os = null;

		try {

			httpExchange.sendResponseHeaders(200, 0);

			os = httpExchange.getResponseBody();
			fs = new FileInputStream(file);

			final byte[] buffer = new byte[0x10000];
			int count = 0;
			while ((count = fs.read(buffer)) >= 0) {
				os.write(buffer, 0, count);
			}

		} catch (final Exception e) {
			StatusUtil.log(e);
		} finally {

			Util.close(fs);
			Util.close(os);
		}
	}

	private static void handle_XHR(final HttpExchange httpExchange, final String requestUriPath, final StringBuilder log) {

		final InputStream reqBody = null;

		try {

			if (LOG_XHR) {

//				reqBody = httpExchange.getRequestBody();
//
//				final StringBuilder sb = new StringBuilder();
//				final byte[] buffer = new byte[0x10000];
//
//				while (reqBody.read(buffer) != -1) {
//					sb.append(buffer);
//				}
//
//				// log content
//				log.append("\nXHR-\n");
//				log.append(sb.toString());
//				log.append("\n-XHR\n");
			}

			final String xhrKey = requestUriPath;
			final XHRHandler xhrHandler = _allXHRHandler.get(xhrKey);

			if (xhrHandler == null) {
				StatusUtil.logError("XHR handler is not set for " + xhrKey);
			} else {
				xhrHandler.handleXHREvent(httpExchange, log);
			}

		} catch (final Exception e) {
			StatusUtil.log(e);
		} finally {

			Util.close(reqBody);
		}

	}

	private static void logHeader(final StringBuilder log, final Set<Entry<String, List<String>>> headerEntries) {

		log.append("\n");

		for (final Entry<String, List<String>> entry : headerEntries) {
			log.append(String.format("%-20s %s\n", entry.getKey(), entry.getValue()));
		}
	}

	private static void logParameter(final HttpExchange httpExchange, final StringBuilder log) {

		// get parameters from url query string

		@SuppressWarnings("unchecked")
		final Map<String, Object> params = (Map<String, Object>) httpExchange
				.getAttribute(RequestParameterFilter.ATTRIBUTE_PARAMETERS);

		log.append("\tparams: " + params);
	}

	/**
	 * @param xhrKey
	 * @return Returns the previous handler or <code>null</code> when a handler is not available.
	 */
	public static XHRHandler removeXHRHandler(final String xhrKey) {

		return _allXHRHandler.remove(xhrKey);
	}

	public static void start() {

		if (_server != null) {
			return;
		}

		try {

			_server = HttpServer.create(inetAddress, 0);

			final HttpContext context = _server.createContext("/", new DefaultHandler());

			// convert uri query parameters into a "parameters" map
			context.getFilters().add(new RequestParameterFilter());

			// ensure that the server is running in another thread
			final ExecutorService executor = Executors.newFixedThreadPool(NUMBER_OF_SERVER_THREADS);
			_server.setExecutor(executor);

			_server.start();

			StatusUtil.logInfo("Started WebContentServer " + SERVER_URL);//$NON-NLS-1$

		} catch (final IOException e) {
			StatusUtil.showStatus(e);
		}
	}

	public static void stop() {

		if (_server != null) {

			_server.stop(0);
			_server = null;

			StatusUtil.logInfo("Stopped WebContentServer " + SERVER_URL);//$NON-NLS-1$
		}
	}

	@Override
	public String toString() {
		return "WebContentServer [set=" + Arrays.toString(set) + "]";
	}

}
