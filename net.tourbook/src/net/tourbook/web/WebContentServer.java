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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/*
 * Part of this code is copied (23.11.2014) from
 * http://www.microhowto.info/howto/serve_web_pages_using_an_embedded_http_server_in_java.html
 */
public class WebContentServer {

	private static final int	SERVER_PORT	= 80;

	private static HttpServer	_server;

	private static class GetHandler implements HttpHandler {
		public void handle(final HttpExchange httpExchange) throws IOException {

			final StringBuilder response = new StringBuilder();

			final Map<String, String> parms = WebContentServer.queryToMap(httpExchange.getRequestURI().getQuery());

			response.append("<html><body>");
			response.append("hello : " + parms.get("hello") + "<br/>");
			response.append("foo : " + parms.get("foo") + "<br/>");
			response.append("</body></html>");

			WebContentServer.writeResponse(httpExchange, response.toString());
		}
	}

	private static class GetHandler2 implements HttpHandler {
		public void handle(final HttpExchange httpExchange) throws IOException {

			// add the required response header for a PDF file
			final Headers headers = httpExchange.getResponseHeaders();
			headers.add("Content-Type", "application/pdf");

			// a PDF (you provide your own!)
			final File file = new File("c:/temp/doc.pdf");
			final byte[] bytearray = new byte[(int) file.length()];
			final FileInputStream fis = new FileInputStream(file);

			final BufferedInputStream bis = new BufferedInputStream(fis);
			bis.read(bytearray, 0, bytearray.length);

			// ok, we are ready to send the response.
			httpExchange.sendResponseHeaders(200, file.length());

			final OutputStream responseBody = httpExchange.getResponseBody();
			responseBody.write(bytearray, 0, bytearray.length);
			responseBody.close();

			bis.close();
		}
	}

	// http://localhost:8000/info
	private static class InfoHandler implements HttpHandler {
		public void handle(final HttpExchange httpExchange) throws IOException {

			final String response = "Use /get?hello=word&foo=bar to see how to handle url parameters";

			WebContentServer.writeResponse(httpExchange, response.toString());
		}
	}

	private static class InfoHandler2 implements HttpHandler {
		public void handle(final HttpExchange t) throws IOException {

			final String response = "Use /get to download a PDF";
			t.sendResponseHeaders(200, response.length());

			final OutputStream os = t.getResponseBody();
			os.write(response.getBytes());
			os.close();
		}
	}

	private static class RootHandler implements HttpHandler {
		public void handle(final HttpExchange httpExchange) throws IOException {

			FileInputStream fs = null;
			OutputStream os = null;

			try {

				boolean isResource = false;

				final String root = WEB.getFile(UI.EMPTY_STRING).getCanonicalFile().getPath();
				final URI requestURI = httpExchange.getRequestURI();
				final String requestUriPath = requestURI.getPath();

				String requestedOSPath;

				if (requestUriPath.startsWith("/firebug-lite")) {

					isResource = true;
					requestedOSPath = "C:/E/XULRunner/" + requestUriPath;

				} else if (requestUriPath.startsWith("/???")) {

					isResource = true;
					requestedOSPath = "C:/E/js-resources/dojo/dojo-release/" + requestUriPath;

				} else {

					requestedOSPath = root + requestUriPath;
				}

				final File file = new File(requestedOSPath).getCanonicalFile();

				System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
						+ ("\trequestURI\t" + requestUriPath)//
						+ ("\tfile.getPath()\t" + file.getPath())//
				);
				// TODO remove SYSTEM.OUT.PRINTLN

				if (!file.getPath().startsWith(root) && !isResource) {

					// Suspected path traversal attack: reject with 403 error.

					final String response = "403 (Forbidden)\n";
					httpExchange.sendResponseHeaders(403, response.length());

					os = httpExchange.getResponseBody();
					os.write(response.getBytes());

					StatusUtil.log(response + " " + file.getPath());

				} else if (!file.isFile()) {

					// Object does not exist or is not a file: reject with 404 error.

					final String response = "404 (Not Found)\n";
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
			}
		}
	}

	private static void checkServer() {

		if (_server != null) {
			return;
		}

		try {

//			final InetSocketAddress inetSocketAddress = new InetSocketAddress(SERVER_PORT);
//			_server = HttpServer.create(inetSocketAddress, 0);

			final InetSocketAddress inetAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(), SERVER_PORT);

			_server = HttpServer.create(inetAddress, 0);

			_server.createContext("/", new RootHandler());

			// ensure that the server is running in another thread
			_server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(10));

			_server.start();

			StatusUtil.logInfo("Started WebContentServer (HTTP) at port: " + SERVER_PORT);//$NON-NLS-1$

		} catch (final IOException e) {
			StatusUtil.showStatus(e);
		}
	}

	public static void init() {

		checkServer();
	}

	/**
	 * returns the url parameters in a map
	 * 
	 * @param query
	 * @return map
	 */
	private static Map<String, String> queryToMap(final String query) {

		final Map<String, String> result = new HashMap<String, String>();

		for (final String param : query.split("&")) {

			final String pair[] = param.split("=");

			if (pair.length > 1) {
				result.put(pair[0], pair[1]);
			} else {
				result.put(pair[0], "");
			}
		}

		return result;
	}

	public static void stop() {

		if (_server != null) {

			_server.stop(0);
			_server = null;

			StatusUtil.logInfo("Stopped WebContentServer (HTTP) at port: " + SERVER_PORT);//$NON-NLS-1$
		}

	}

	private static void writeResponse(final HttpExchange httpExchange, final String response) throws IOException {

		httpExchange.sendResponseHeaders(200, response.length());

		final OutputStream os = httpExchange.getResponseBody();
		os.write(response.getBytes());
		os.close();
	}

}
