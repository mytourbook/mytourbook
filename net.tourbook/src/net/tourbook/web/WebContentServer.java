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
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/*
 * Part of this code is copied (23.11.2014) from
 * http://www.microhowto.info/howto/serve_web_pages_using_an_embedded_http_server_in_java.html
 */
public class WebContentServer {

	private static boolean				DEBUG						= true;

	private static final String			PROTOCOL_HTTP				= "http://";	//$NON-NLS-1$
	private static final String			PROTOCOL_COLUMN				= ":";			//$NON-NLS-1$
	private static final String			ROOT_FILE_PATH_NAME			= "/";			//$NON-NLS-1$

	static String						SERVER_URL;

	private static final int			NUMBER_OF_SERVER_THREADS	= 10;

	private static HttpServer			_server;
	private static final int			_serverPort;

	private static InetSocketAddress	inetAddress;

	static {

		if (DEBUG) {
			_serverPort = 24114;
		} else {
			_serverPort = PortFinder.findFreePort();
		}

		final InetAddress loopbackAddress = InetAddress.getLoopbackAddress();
		inetAddress = new InetSocketAddress(loopbackAddress, _serverPort);

		SERVER_URL = PROTOCOL_HTTP + loopbackAddress.getHostAddress() + PROTOCOL_COLUMN + _serverPort;
	}

	private static class DefaultHandler implements HttpHandler {

		public void handle(final HttpExchange httpExchange) throws IOException {

			final long start = System.nanoTime();
			String log = UI.EMPTY_STRING;

			FileInputStream fs = null;
			OutputStream os = null;

			try {

				boolean isResource = false;

				final String root = WEB.getFile(ROOT_FILE_PATH_NAME).getCanonicalFile().getPath();
				final URI requestURI = httpExchange.getRequestURI();
				final String requestUriPath = requestURI.getPath();

				String requestedOSPath;

				if (requestUriPath.startsWith("/WebContent-firebug-lite")) {

					isResource = true;
					requestedOSPath = "C:/E/XULRunner/" + requestUriPath;

				} else if (requestUriPath.startsWith("/WebContent-dojo")) {

					isResource = true;
					requestedOSPath = "C:/E/js-resources/dojo/" + requestUriPath;

				} else {

					requestedOSPath = root + requestUriPath;
				}

				final File file = new File(requestedOSPath).getCanonicalFile();

//				System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//						+ ("\trequestURI\t" + requestUriPath)//
////						+ ("\tfile.getPath()\t" + file.getPath())//
//				);
//				// TODO remove SYSTEM.OUT.PRINTLN
				if (DEBUG) {
					log += requestUriPath;
				}

				if (!file.getPath().startsWith(root) && !isResource) {

					// Suspected path traversal attack: reject with 403 error.

					final String response = "403 (Forbidden)\n";
					httpExchange.sendResponseHeaders(403, response.length());

					os = httpExchange.getResponseBody();
					os.write(response.getBytes());

					StatusUtil.log(response + " " + file.getPath());

				} else if (!file.isFile()) {

					// Object does not exist or is not a file: reject with 404 error.

					final String response = String.format("%s\n404 (Not Found)\n", requestUriPath);
					httpExchange.sendResponseHeaders(404, response.length());

					os = httpExchange.getResponseBody();
					os.write(response.getBytes());

					StatusUtil.log(response + " " + file.getPath());

				} else {

					// Object exists and is a file: accept with response code 200.

					httpExchange.sendResponseHeaders(200, 0);

					os = httpExchange.getResponseBody();
					fs = new FileInputStream(file);

					final byte[] buffer = new byte[0x10000];
					int count = 0;
					while ((count = fs.read(buffer)) >= 0) {
						os.write(buffer, 0, count);
					}
				}

			} catch (final Exception e) {
				StatusUtil.log(e);
			} finally {

				Util.close(fs);
				Util.close(os);

				if (DEBUG) {

					final String msg2 = String.format("%s    %03.2f ms  %-16s  %s", //
							UI.timeStampNano(),
							(float) (System.nanoTime() - start) / 1000000,
							Thread.currentThread().getName(),
							log);

					System.out.println(msg2);
				}
			}
		}
	}

	private static void checkServer() {

		if (_server != null) {
			return;
		}

		try {

			_server = HttpServer.create(inetAddress, 0);

			_server.createContext("/", new DefaultHandler());

			// ensure that the server is running in another thread
			final ExecutorService executor = Executors.newFixedThreadPool(NUMBER_OF_SERVER_THREADS);
			_server.setExecutor(executor);

			_server.start();

			StatusUtil.logInfo("Started WebContentServer " + SERVER_URL);//$NON-NLS-1$

		} catch (final IOException e) {
			StatusUtil.showStatus(e);
		}
	}

	public static void init() {

		checkServer();
	}

	public static void stop() {

		if (_server != null) {

			_server.stop(0);
			_server = null;

			StatusUtil.logInfo("Stopped WebContentServer " + SERVER_URL);//$NON-NLS-1$
		}
	}

}
