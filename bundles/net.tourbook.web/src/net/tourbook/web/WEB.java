/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;

import net.tourbook.common.ReplacingOutputStream;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.json.JSONArray;
import org.json.JSONObject;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

/**
 * Web tools.
 */
public class WEB {

	/**
	 * This is the <b>MAIN</b> switch to run Dojo in the dev or release folder. In debug mode the
	 * Dojo files are delivered separately to debug them, in release mode all Dojo files are
	 * compacted and gzip'ed.
	 * <p>
	 * When <code>true</code> the web content is delivered from the
	 * {@value #WEB_CONTENT_DEVELOPMENT_FOLDER} folder otherwise it is delivered from the
	 * {@value #WEB_CONTENT_RELEASE_FOLDER} folder.
	 */
	static boolean							IS_DEBUG								= false;

	/*
	 * It is very complicated to support testing for language translators, therefore it is currently
	 * not yet implemented.
	 */
//	static boolean							IS_DEBUG_NLS							= true;

	static String							DEFAULT_LANGUAGE						= "en";										//$NON-NLS-1$

	/**
	 * Supported languages.
	 */
	static String[]							SUPPORTED_LANGUAGES						= { //
																					// cs_CZ
			"cs", //$NON-NLS-1$
			"de",//$NON-NLS-1$
			DEFAULT_LANGUAGE,
			"es",//$NON-NLS-1$
			"fr",//$NON-NLS-1$
			"it",//$NON-NLS-1$
			"nl" //$NON-NLS-1$
																					};

	static final String						DEBUG_PATH_DOJO							= "C:/E/js-resources/dojo/";					//$NON-NLS-1$
	private static final String				DEBUG_PATH_XUL_RUNNER					= "C:/E/XULRunner/";							//$NON-NLS-1$
	private static final String				DEBUG_PATH_FIREBUG_LITE					= "/WebContent-firebug-lite";					//$NON-NLS-1$

	public static final String				PROTOCOL_HTTP							= "http://";									//$NON-NLS-1$

	static final String						DOJO_TOOLKIT_FOLDER						= "/MyTourbook-DojoToolkit";					//$NON-NLS-1$

	private static final String				WEB_CONTENT_DEVELOPMENT_FOLDER			= "/WebContent-dev";							//$NON-NLS-1$
	private static final String				WEB_CONTENT_RELEASE_FOLDER				= "/WebContent-rel";							//$NON-NLS-1$
	private static final String				RESOURCE_PATH							= "/tourbook/resources/";						//$NON-NLS-1$

	/**
	 * Root folder for web content in the web plugin.
	 */
	private static final String				WEB_CONTENT_FOLDER						= IS_DEBUG
																							? WEB_CONTENT_DEVELOPMENT_FOLDER
																							: WEB_CONTENT_RELEASE_FOLDER;

	public static final String				UTF_8									= "UTF-8";										//$NON-NLS-1$

	private static final String				URL_SPACE								= " ";											//$NON-NLS-1$
	private static final String				URL_SPACE_REPLACEMENT					= "%20";										//$NON-NLS-1$

	public static final String				HTML_ELEMENT_BR							= "<br>";										//$NON-NLS-1$

	public static final String				RESPONSE_HEADER_ACCEPT_LANGUAGE			= "Accept-Language";							//$NON-NLS-1$
	private static final String				RESPONSE_HEADER_CONTENT_ENCODING		= "Content-Encoding";							//$NON-NLS-1$
	public static final String				RESPONSE_HEADER_CONTENT_RANGE			= "Content-Range";								//$NON-NLS-1$
	public static final String				RESPONSE_HEADER_CONTENT_TYPE			= "Content-Type";								//$NON-NLS-1$

	private static final String				CONTENT_ENCODING_GZIP					= "gzip";										//$NON-NLS-1$

	private static final String				CONTENT_TYPE_APPLICATION_JAVASCRIPT		= "application/javascript";					//$NON-NLS-1$
	public static final String				CONTENT_TYPE_APPLICATION_JSON			= "application/json";							//$NON-NLS-1$
	private static final String				CONTENT_TYPE_APPLICATION_X_JAVASCRIPT	= "application/x-javascript; charset=UTF-8";	//$NON-NLS-1$
	private static final String				CONTENT_TYPE_IMAGE_GIF					= "image/gif";									//$NON-NLS-1$
	private static final String				CONTENT_TYPE_IMAGE_JPG					= "image/jpeg";								//$NON-NLS-1$
	private static final String				CONTENT_TYPE_IMAGE_PNG					= "image/png";									//$NON-NLS-1$
	private static final String				CONTENT_TYPE_IMAGE_X_ICO				= "image/x-icon";								//$NON-NLS-1$
	private static final String				CONTENT_TYPE_TEXT_CSS					= "text/css";									//$NON-NLS-1$
	private static final String				CONTENT_TYPE_TEXT_HTML					= "text/html";									//$NON-NLS-1$
	private static final String				CONTENT_TYPE_UNKNOWN					= "application/octet-stream";					//$NON-NLS-1$

	private static final String				FILE_EXTENSION_CSS						= "css";										//$NON-NLS-1$
	private static final String				FILE_EXTENSION_GIF						= "gif";										//$NON-NLS-1$
	private static final String				FILE_EXTENSION_HTML						= "html";										//$NON-NLS-1$
	private static final String				FILE_EXTENSION_ICO						= "ico";										//$NON-NLS-1$
	private static final String				FILE_EXTENSION_JGZ						= "jgz";										//$NON-NLS-1$
	private static final String				FILE_EXTENSION_JPG						= "jpg";										//$NON-NLS-1$
	private static final String				FILE_EXTENSION_JS						= "js";										//$NON-NLS-1$
	private static final String				FILE_EXTENSION_MAP						= "map";										//$NON-NLS-1$
	private static final String				FILE_EXTENSION_PNG						= "png";										//$NON-NLS-1$

	/**
	 * This file extension is for HTML pages which contain variable replacements, processed in
	 * {@link ReplacingOutputStream}.
	 */
	public static final String				FILE_EXTENSION_MTHTML					= "mthtml";									//$NON-NLS-1$

	public static final String				STATE_EXTERNAL_WEB_BROWSER				= "STATE_EXTERNAL_WEB_BROWSER";				//$NON-NLS-1$
	public static final String				STATE_EXTERNAL_WEB_BROWSER_DEFAULT		= UI.EMPTY_STRING;
	public static final String				STATE_USE_EXTERNAL_WEB_BROWSER			= "STATE_USE_EXTERNAL_WEB_BROWSER";			//$NON-NLS-1$
	public static final boolean				STATE_USE_EXTERNAL_WEB_BROWSER_DEFAULT	= false;

	private static final IDialogSettings	_state									= Activator.getState(WEB.class
																							.getCanonicalName());

	/**
	 * Converts Java newline into HTML newline.
	 * 
	 * @param text
	 * @return
	 */
	public static String convertHTML_LineBreaks(final String text) {

		return text.replaceAll("\\r\\n|\\r|\\n", HTML_ELEMENT_BR); //$NON-NLS-1$
	}

	/**
	 * Convert Java newline into JS newline.
	 * 
	 * @param text
	 * @return
	 */
	public static String convertJS_LineBreaks(final String text) {

		return text.replaceAll("\\r\\n|\\r|\\n", "\\\\n"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Replace space characters with <code>%20</code>.
	 */
	public static String encodeSpace(final String urlString) {

		return urlString.replaceAll(URL_SPACE, URL_SPACE_REPLACEMENT);
	}

	/**
	 * @param path
	 * @return Returns the 2nd last extension or <code>null</code> when not available.
	 */
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

	public static File getEscapedBundleFile(final URL bundleUrl) {

		File file = null;

		try {

			final URL fileUrl = FileLocator.toFileURL(bundleUrl);
			final String encodedFileUrl = WEB.encodeSpace(fileUrl.toExternalForm());

			final URI uri = new URI(encodedFileUrl);
			file = new File(uri);

		} catch (final Exception e) {

			// this should not happen otherwise the resource is not available
			StatusUtil.showStatus(e);
		}

		return file;
	}

	/**
	 * @param filePathName
	 * @return Returns a file from the WebContent folder, this folder is the root for path names.
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public static File getFile(final String filePathName) throws IOException, URISyntaxException {

		final String bundleFileName = WEB_CONTENT_FOLDER + filePathName;

		final URL bundleUrl = Activator.getDefault().getBundle().getEntry(bundleFileName);

		if (bundleUrl == null) {
			StatusUtil.log("File is not available: " + bundleFileName);//$NON-NLS-1$
			return null;
		}

		final File file = getEscapedBundleFile(bundleUrl);

		return file;
	}

	/**
	 * @param webContentFile
	 *            Absolute file path name which parent is {@value #WEB_CONTENT_FOLDER}.
	 * @param isConvertPaths
	 *            Converts absolute paths to file paths.
	 * @return Returns the content of a file from the WebContent folder, this folder is the root for
	 *         web resources located in {@value #WEB_CONTENT_FOLDER}.
	 */
	public static String getFileContent(final String webContentFile, final boolean isConvertPaths) {

		File webFile;
		String webContent = null;

		try {

			webFile = WEB.getFile(webContentFile);
			webContent = Util.readContentFromFile(webFile.getAbsolutePath());

			if (isConvertPaths) {

				final String fromHtmlDojoPath = DOJO_TOOLKIT_FOLDER;
				final String fromHtmlFirebugPath = DEBUG_PATH_FIREBUG_LITE;

				final String toSystemDojoPath = DEBUG_PATH_DOJO;
				final String toSystemFirebugPath = DEBUG_PATH_XUL_RUNNER;

				// replace local paths, which do not start with a /
				webContent = replaceLocalDebugPath(webContent, webFile);

				webContent = replacePath(webContent, fromHtmlDojoPath, toSystemDojoPath);
				webContent = replacePath(webContent, fromHtmlFirebugPath, toSystemFirebugPath);
			}

		} catch (IOException | URISyntaxException e) {
			StatusUtil.showStatus(e);
		}

		return webContent;
	}

	/**
	 * @param fileName
	 * @return Returns a file from the resource folder {@value #RESOURCE_PATH}.
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public static File getResourceFile(final String fileName) throws IOException, URISyntaxException {

		final String bundleFileName = WEB_CONTENT_FOLDER + RESOURCE_PATH + fileName;

		final URL bundleUrl = Activator.getDefault().getBundle().getEntry(bundleFileName);

		if (bundleUrl == null) {
			StatusUtil.log("File is not available: " + bundleFileName);//$NON-NLS-1$
			return null;
		}

		final File file = getEscapedBundleFile(bundleUrl);

		return file;
	}

	public static IDialogSettings getState() {
		return _state;
	}

	/**
	 * Open url with default or external web browser.
	 * 
	 * @param url
	 */
	public static void openUrl(String href) {

		// format the href for an html file (file:///<filename.html>) required for Mac only.

		if (href.startsWith("file:")) { //$NON-NLS-1$
			href = href.substring(5);
			while (href.startsWith("/")) { //$NON-NLS-1$
				href = href.substring(1);
			}
			href = "file:///" + href; //$NON-NLS-1$

		} else if (href.startsWith("http") == false) { //$NON-NLS-1$

			// Ensure that a protocol is set otherwise a MalformedURLException exception occures
			href = "http://" + href; //$NON-NLS-1$
		}

		final boolean useExternalWebBrowser = _state.getBoolean(STATE_USE_EXTERNAL_WEB_BROWSER);
		final String externalWebBrowser = _state.get(STATE_EXTERNAL_WEB_BROWSER);

		if (useExternalWebBrowser == false) {

			// external web browser is not defined
			openUrl_WithDefaultWebbrowser(href);

		} else {

			openUrl_WithExternalBrowser(externalWebBrowser, href);
		}
	}

	/**
	 * Open a link
	 */
	private static void openUrl_WithDefaultWebbrowser(final String href) {

		final IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();

		try {

			final IWebBrowser browser = support.getExternalBrowser();
			browser.openURL(new URL(encodeSpace(href)));

		} catch (final MalformedURLException e) {
			StatusUtil.showStatus(e);
		} catch (final PartInitException e) {
			StatusUtil.showStatus(e);
		}
	}

	private static void openUrl_WithExternalBrowser(final String externalWebBrowser, final String url) {

		final String encodedUrl = encodeSpace(url);

		final ArrayList<String> commands = new ArrayList<String>();

		if (UI.IS_WIN) {

			final String[] appCmdLines = externalWebBrowser.split("[\\r\\n]+"); //$NON-NLS-1$

			for (int cmdIndex = 0; cmdIndex < appCmdLines.length; cmdIndex++) {

				final String appCmd = appCmdLines[cmdIndex];

				if (cmdIndex == 0) {
					commands.add("\"" + appCmd + "\""); //$NON-NLS-1$ //$NON-NLS-2$
				} else {
					// don't add apostrophes, this causes errors
					commands.add(appCmd);
				}
			}

			commands.add("\"" + encodedUrl + "\""); //$NON-NLS-1$ //$NON-NLS-2$

		} else if (UI.IS_OSX) {

			final String[] appCmdLines = externalWebBrowser.split("[\\r\\n\\s]+"); //$NON-NLS-1$

			commands.add("/usr/bin/open"); //$NON-NLS-1$
			commands.add("-a"); //$NON-NLS-1$

			for (final String appCmd : appCmdLines) {
				commands.add(appCmd);
			}

			commands.add(encodedUrl);

		} else if (UI.IS_LINUX) {

			final String[] appCmdLines = externalWebBrowser.split("[\\r\\n\\s]+"); //$NON-NLS-1$

			for (final String appCmd : appCmdLines) {
				commands.add(appCmd);
			}

			commands.add(encodedUrl);
		}

		try {

			// log command
			final StringBuilder cmdLine = new StringBuilder();
			for (int cmdIndex = 0; cmdIndex < commands.size(); cmdIndex++) {

				final String cmd = commands.get(cmdIndex);

				if (cmdIndex > 0) {
					cmdLine.append(' ');
				}

				cmdLine.append(cmd);
			}
			StatusUtil.logInfo(cmdLine.toString());

			// run command
			final String[] commandArray = commands.toArray(new String[commands.size()]);

			/**
			 * Spaces or " in parameters (web browser profile name) are not working, not supported
			 */
			Runtime.getRuntime().exec(commandArray);

		} catch (final Exception e) {
			StatusUtil.showStatus(e);
		}
	}

	/**
	 * Decodes the raw json data and converts it in a {@link JSONArray}.
	 * 
	 * @param jsonRawData
	 * @return Returns a {@link JSONArray} from the parsed json data.
	 * @throws UnsupportedEncodingException
	 */
	public static JSONArray parseJSONArray(final Object jsonRawData) throws UnsupportedEncodingException {
		
		final String jsData = URLDecoder.decode((String) jsonRawData, UTF_8);
		
		final JSONArray jsonData = new JSONArray(jsData);
		
		return jsonData;
	}

	/**
	 * Decodes the raw json data and converts it in a {@link JSONObject}.
	 * 
	 * @param jsonRawData
	 * @return Returns a {@link JSONObject} from the parsed json data.
	 * @throws UnsupportedEncodingException
	 */
	public static JSONObject parseJSONObject(final Object jsonRawData) throws UnsupportedEncodingException {
 
		final String jsData = URLDecoder.decode((String) jsonRawData, UTF_8);

		final JSONObject jsonData = new JSONObject(jsData);

		return jsonData;
	}

	private static String replaceLocalDebugPath(final String webContent, final File webFile) {

		String replacedContent = UI.EMPTY_STRING;

		try {

			final Path filePath = new Path(webFile.getAbsolutePath());
			final File folderPath = filePath.removeLastSegments(1).toFile();
			final String urlPath = folderPath.toURI().toURL().toExternalForm();

			replacedContent = webContent.replaceAll("href=\"(?!/)", "href=\"" + urlPath); //$NON-NLS-1$ //$NON-NLS-2$
			replacedContent = replacedContent.replaceAll("src=\"(?!/)", "src=\"" + urlPath); //$NON-NLS-1$ //$NON-NLS-2$

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
	 * @return Returns the file extension.
	 */
	public static String setResponseHeaderContentType(final HttpExchange httpExchange, final File file) {

		String contentType = null;
		boolean isCompressed = false;

		final Path path = new Path(file.getAbsolutePath());

		String extension = null;
		String rawExtension = path.getFileExtension();

		if (rawExtension != null) {

			extension = rawExtension.toLowerCase();

			String compressedExtension = null;

			if (FILE_EXTENSION_JGZ.equals(extension)) {

				// file is compressed

				rawExtension = getCompressedExtension(path);

				if (rawExtension != null) {
					compressedExtension = rawExtension.toLowerCase();
				}
			}

			switch (extension) {

			case FILE_EXTENSION_CSS:
				contentType = CONTENT_TYPE_TEXT_CSS;
				break;

			case FILE_EXTENSION_GIF:
				contentType = CONTENT_TYPE_IMAGE_GIF;
				break;

			case FILE_EXTENSION_HTML:
			case FILE_EXTENSION_MTHTML:
				contentType = CONTENT_TYPE_TEXT_HTML;
				break;

			case FILE_EXTENSION_ICO:
				contentType = CONTENT_TYPE_IMAGE_X_ICO;
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

			case FILE_EXTENSION_MAP: // .js.map
				contentType = CONTENT_TYPE_APPLICATION_JSON;
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

		return extension;
	}
}
