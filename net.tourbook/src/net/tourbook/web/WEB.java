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

public class WEB {

	public static String		SERVER_URL;

	static {

		SERVER_URL = WebContentServer.SERVER_URL;
	}

	/**
	 * Root folder for web content in the plugin.
	 */
	private static final String	PLUGIN_WEB_CONTENT_FOLDER	= "/WebContent";	//$NON-NLS-1$

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
}
