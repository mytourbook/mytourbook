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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

public class WEB {

	public static final String	UTF_8									= "UTF-8";										//$NON-NLS-1$

	public static String		SERVER_URL;

	static {

		SERVER_URL = WebContentServer.SERVER_URL;
	}

	/**
	 * Root folder for web content in the plugin.
	 */
	private static final String	PLUGIN_WEB_CONTENT_FOLDER				= "/WebContent";								//$NON-NLS-1$

	private static final String	RESPONSE_HEADER_CONTENT_ENCODING		= "Content-Encoding";							//$NON-NLS-1$
	public static final String	RESPONSE_HEADER_CONTENT_RANGE			= "Content-Range";								//$NON-NLS-1$
	public static final String	RESPONSE_HEADER_CONTENT_TYPE			= "Content-Type";								//$NON-NLS-1$

	private static final String	CONTENT_ENCODING_GZIP					= "gzip";										//$NON-NLS-1$

	private static final String	CONTENT_TYPE_APPLICATION_JAVASCRIPT		= "application/javascript";					//$NON-NLS-1$
	public static final String	CONTENT_TYPE_APPLICATION_JSON			= "application/json";							//$NON-NLS-1$
	private static final String	CONTENT_TYPE_APPLICATION_X_JAVASCRIPT	= "application/x-javascript; charset=UTF-8";	//$NON-NLS-1$
	private static final String	CONTENT_TYPE_IMAGE_JPG					= "image/jpeg";								//$NON-NLS-1$
	private static final String	CONTENT_TYPE_IMAGE_PNG					= "image/png";									//$NON-NLS-1$
	private static final String	CONTENT_TYPE_TEXT_CSS					= "text/css";									//$NON-NLS-1$
	private static final String	CONTENT_TYPE_TEXT_HTML					= "text/html";									//$NON-NLS-1$
	private static final String	CONTENT_TYPE_UNKNOWN					= "application/octet-stream";					//$NON-NLS-1$

	private static final String	FILE_EXTENSION_CSS						= "css";										//$NON-NLS-1$
	private static final String	FILE_EXTENSION_HTML						= "html";										//$NON-NLS-1$
	private static final String	FILE_EXTENSION_JGZ						= "jgz";										//$NON-NLS-1$
	private static final String	FILE_EXTENSION_JPG						= "jpg";										//$NON-NLS-1$
	private static final String	FILE_EXTENSION_JS						= "js";										//$NON-NLS-1$
	private static final String	FILE_EXTENSION_PNG						= "png";										//$NON-NLS-1$

	private static String getCompressedExtension(final Path path) {

		if (path.hasTrailingSeparator()) {
			return null;
		}

		final String lastSegment = path.lastSegment();
		if (lastSegment == null) {
			return null;
		}

		final int endIndex = lastSegment.lastIndexOf('.');
		if (endIndex == -1) {
			return null;
		}

		final int index = lastSegment.lastIndexOf('.', endIndex - 1);
		if (index == -1) {
			return null;
		}

		return lastSegment.substring(index + 1, endIndex);
	}

	/**
	 * @param filePathName
	 * @return Returns a file from the WebContent folder, this folder is the root for path names.
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public static File getFile(final String filePathName) throws IOException, URISyntaxException {

		final URL bundleUrl = Activator.getDefault().getBundle().getEntry(PLUGIN_WEB_CONTENT_FOLDER + filePathName);

		final URL fileUrl = FileLocator.toFileURL(bundleUrl);
		final File file = new File(fileUrl.toURI());

		return file;

	}

	/**
	 * @param webContentFile
	 *            Absolute file path name which parent is {@value #PLUGIN_WEB_CONTENT_FOLDER}.
	 * @param isConvertPaths
	 *            Converts absolute paths to file paths.
	 * @return Returns the content of a file from the WebContent folder, this folder is the root for
	 *         web resources located in {@value #PLUGIN_WEB_CONTENT_FOLDER}.
	 */
	public static String getFileContent(final String webContentFile, final boolean isConvertPaths) {

		File webFile;
		String webContent = null;

		try {

			webFile = WEB.getFile(webContentFile);
			webContent = Util.readContentFromFile(webFile.getAbsolutePath());

			if (isConvertPaths) {

				final String fromHtmlDojoPath = "/WebContent-dojo";
				final String fromHtmlFirebugPath = "/WebContent-firebug-lite";

				final String toSystemDojoPath = "C:/E/js-resources/dojo/";
				final String toSystemFirebugPath = "C:/E/XULRunner/";

				// replace local paths, which do not start with a /
				webContent = replaceLocalPath(webContent, webFile);

				webContent = replacePath(webContent, fromHtmlDojoPath, toSystemDojoPath);
				webContent = replacePath(webContent, fromHtmlFirebugPath, toSystemFirebugPath);
			}

		} catch (IOException | URISyntaxException e) {
			StatusUtil.showStatus(e);
		}

		return webContent;
	}

	/**
	 * @param filePathName
	 * @return Returns the root of the WebContent folder.
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public static URI getRoot() throws IOException, URISyntaxException {

		final URL bundleUrl = Activator.getDefault().getBundle().getEntry(PLUGIN_WEB_CONTENT_FOLDER);

		final URL fileUrl = FileLocator.toFileURL(bundleUrl);
		final URI fileUri = fileUrl.toURI();

		return fileUri;

	}

	private static String replaceLocalPath(final String webContent, final File webFile) {

		String replacedContent = UI.EMPTY_STRING;

		try {

			final Path filePath = new Path(webFile.getAbsolutePath());
			final File folderPath = filePath.removeLastSegments(1).toFile();
			final String urlPath = folderPath.toURI().toURL().toExternalForm();

			replacedContent = webContent.replaceAll("href=\"(?!/)", "href=\"" + urlPath);
			replacedContent = replacedContent.replaceAll("src=\"(?!/)", "src=\"" + urlPath);

		} catch (final MalformedURLException e) {
			StatusUtil.log(e);
		}

		return replacedContent;
	}

	private static String replacePath(	final String webContent,
										final String htmlContentPath,
										final String systemContentPath) {

		String replacedContent = UI.EMPTY_STRING;

		try {

			String systemUriPath = new File(systemContentPath).toURI().toURL().toExternalForm();

			// remove trailing slash
			systemUriPath = systemUriPath.substring(0, systemUriPath.length() - 1);

			final String systemPath = systemUriPath + htmlContentPath;

			replacedContent = webContent.replaceAll(htmlContentPath, systemPath);

		} catch (final MalformedURLException e) {
			StatusUtil.log(e);
		}

		return replacedContent;
	}

	/**
	 * Set content type from the file extension into the HTTP header. This is necessary otherwise IE
	 * 11 will ignore the files.
	 * 
	 * @param httpExchange
	 * @param file
	 */
	public static void setResponseHeaderContentType(final HttpExchange httpExchange, final File file) {

		String contentType = null;
		boolean isCompressed = false;

		final Path path = new Path(file.getAbsolutePath());

		String rawExtension = path.getFileExtension();
		if (rawExtension != null) {

			final String extension = rawExtension.toLowerCase();

			String compressedExtension = null;

			if (FILE_EXTENSION_JGZ.equals(extension)) {

				rawExtension = getCompressedExtension(path);

				if (rawExtension != null) {
					compressedExtension = rawExtension.toLowerCase();
				}
			}

			switch (extension) {

			case FILE_EXTENSION_CSS:
				contentType = CONTENT_TYPE_TEXT_CSS;
				break;

			case FILE_EXTENSION_HTML:
				contentType = CONTENT_TYPE_TEXT_HTML;
				break;

			case FILE_EXTENSION_JGZ:

				if (compressedExtension != null) {

					switch (compressedExtension) {

					case FILE_EXTENSION_CSS:
						contentType = CONTENT_TYPE_TEXT_CSS;
						isCompressed = true;
						break;

					case FILE_EXTENSION_JS:
						contentType = CONTENT_TYPE_APPLICATION_X_JAVASCRIPT;
						isCompressed = true;
						break;
					}
				}
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
			}
		}

		if (contentType == null) {

			contentType = CONTENT_TYPE_UNKNOWN;
			StatusUtil.log("Content type is unknow for " + file);//$NON-NLS-1$
		}

		final Headers responseHeaders = httpExchange.getResponseHeaders();
		responseHeaders.set(RESPONSE_HEADER_CONTENT_TYPE, contentType);

		if (isCompressed) {
			responseHeaders.set(RESPONSE_HEADER_CONTENT_ENCODING, CONTENT_ENCODING_GZIP);
		}
	}
}
