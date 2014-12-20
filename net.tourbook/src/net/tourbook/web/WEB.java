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
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import net.tourbook.application.TourbookPlugin;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

public class WEB {

	public static final String	UTF_8								= "UTF-8";						//$NON-NLS-1$

	public static String		SERVER_URL;

	static {

		SERVER_URL = WebContentServer.SERVER_URL;
	}

	/**
	 * Root folder for web content in the plugin.
	 */
	private static final String	PLUGIN_WEB_CONTENT_FOLDER			= "/WebContent";				//$NON-NLS-1$

	public static final String	RESPONSE_HEADER_CONTENT_RANGE		= "Content-Range";				//$NON-NLS-1$
	public static final String	RESPONSE_HEADER_CONTENT_TYPE		= "Content-Type";				//$NON-NLS-1$

	private static final String	CONTENT_TYPE_APPLICATION_JAVASCRIPT	= "application/javascript";	//$NON-NLS-1$
	public static final String	CONTENT_TYPE_APPLICATION_JSON		= "application/json";			//$NON-NLS-1$
	private static final String	CONTENT_TYPE_IMAGE_JPG				= "image/jpeg";				//$NON-NLS-1$
	private static final String	CONTENT_TYPE_IMAGE_PNG				= "image/png";					//$NON-NLS-1$
	private static final String	CONTENT_TYPE_TEXT_CSS				= "text/css";					//$NON-NLS-1$
	private static final String	CONTENT_TYPE_TEXT_HTML				= "text/html";					//$NON-NLS-1$
	private static final String	CONTENT_TYPE_UNKNOWN				= "application/octet-stream";	//$NON-NLS-1$

	private static final String	FILE_EXTENSION_CSS					= "css";						//$NON-NLS-1$
	private static final String	FILE_EXTENSION_HTML					= "html";						//$NON-NLS-1$
	private static final String	FILE_EXTENSION_JPG					= "jpg";						//$NON-NLS-1$
	private static final String	FILE_EXTENSION_JS					= "js";						//$NON-NLS-1$
	private static final String	FILE_EXTENSION_PNG					= "png";						//$NON-NLS-1$

	/**
	 * @param filePathName
	 * @return Returns a file from the WebContent folder, this folder is the root for path names.
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public static File getFile(final String filePathName) throws IOException, URISyntaxException {

		final URL bundleUrl = TourbookPlugin
				.getDefault()
				.getBundle()
				.getEntry(PLUGIN_WEB_CONTENT_FOLDER + filePathName);

		final URL fileUrl = FileLocator.toFileURL(bundleUrl);
		final File file = new File(fileUrl.toURI());

		return file;

	}

	/**
	 * @param filePathName
	 * @return Returns the root of the WebContent folder.
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public static URI getRoot() throws IOException, URISyntaxException {

		final URL bundleUrl = TourbookPlugin.getDefault().getBundle().getEntry(PLUGIN_WEB_CONTENT_FOLDER);

		final URL fileUrl = FileLocator.toFileURL(bundleUrl);
		final URI fileUri = fileUrl.toURI();

		return fileUri;

	}

	/**
	 * Set content type from the file extension into the HTTP header. This is necessary otherwise IE
	 * 11 will ignore the files.
	 * 
	 * @param httpExchange
	 * @param file
	 */
	public static void setResponseHeaderContentType(final HttpExchange httpExchange, final File file) {

		final Path path = new Path(file.getAbsolutePath());
		final String extension = path.getFileExtension().toLowerCase();

		String contentType;

		switch (extension) {
		case FILE_EXTENSION_CSS:
			contentType = CONTENT_TYPE_TEXT_CSS;
			break;

		case FILE_EXTENSION_HTML:
			contentType = CONTENT_TYPE_TEXT_HTML;
			break;

		case FILE_EXTENSION_JPG:
			contentType = CONTENT_TYPE_IMAGE_JPG;
			break;

		case FILE_EXTENSION_JS:
			contentType = CONTENT_TYPE_APPLICATION_JAVASCRIPT;
			break;

		case FILE_EXTENSION_PNG:
			contentType = CONTENT_TYPE_IMAGE_PNG;
			break;

		default:
			contentType = CONTENT_TYPE_UNKNOWN;
			break;
		}

		final Headers responseHeaders = httpExchange.getResponseHeaders();
		responseHeaders.set(RESPONSE_HEADER_CONTENT_TYPE, contentType);
	}

}
