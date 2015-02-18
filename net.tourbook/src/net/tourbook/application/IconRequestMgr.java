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
package net.tourbook.application;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;

import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.web.IconRequestHandler;
import net.tourbook.web.WEB;

import org.eclipse.core.runtime.FileLocator;

import com.sun.net.httpserver.HttpExchange;

/**
 * The icon request handler must be located in this plugin because it contains the icon images.
 */
public class IconRequestMgr implements IconRequestHandler {

	private static final String			ICON_FOLDER	= "/icons/";	//$NON-NLS-1$

	private static IconRequestMgr	_instance;

	public static IconRequestHandler getInstance() {

		if (_instance == null) {
			_instance = new IconRequestMgr();
		}

		return _instance;
	}


	/**
	 * @param filePathName
	 * @return Returns a file from the WebContent folder, this folder is the root for path names.
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	private File getFile(final String filePathName) throws IOException, URISyntaxException {

		final String bundleFileName = ICON_FOLDER + filePathName;

		final URL bundleUrl = TourbookPlugin.getDefault().getBundle().getEntry(bundleFileName);

		if (bundleUrl == null) {
			StatusUtil.log("File is not available: " + bundleFileName);//$NON-NLS-1$
			return null;
		}

		final URL fileUrl = FileLocator.toFileURL(bundleUrl);
		final File file = new File(fileUrl.toURI());

		return file;

	}

	@Override
	public void handleIconRequest(final HttpExchange httpExchange, final String iconFileName, final StringBuilder log)
			throws IOException {


		FileInputStream fs = null;
		OutputStream os = null;

		try {

			final File file = getFile(iconFileName);

			WEB.setResponseHeaderContentType(httpExchange, file);

			httpExchange.sendResponseHeaders(200, 0);

			fs = new FileInputStream(file);
			os = httpExchange.getResponseBody();

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
}
