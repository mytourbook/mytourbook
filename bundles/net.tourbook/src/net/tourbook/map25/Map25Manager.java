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
import java.util.ArrayList;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;
import org.osgi.framework.Bundle;

public class Map25Manager {

	private static final Bundle				_bundle						= TourbookPlugin.getDefault().getBundle();
	private static final IPath				_stateLocation				= Platform.getStateLocation(_bundle);

	private static final String				MAP_PROVIDER_FILE_NAME		= "map25-provider.xml";						//$NON-NLS-1$
	private static final int				MAP_PROVIDER_VERSION		= 1;

	private static final String				TAG_ROOT					= "Map25Provider";							//$NON-NLS-1$
	private static final String				TAG_PROFILE					= "Profile";								//$NON-NLS-1$

	private static final String				ATTR_API_KEY				= "APIKey";									//$NON-NLS-1$
	private static final String				ATTR_NAME					= "Name";									//$NON-NLS-1$
	private static final String				ATTR_TILE_PATH				= "TilePath";								//$NON-NLS-1$
	private static final String				ATTR_URL					= "Url";									//$NON-NLS-1$
	private static final String				ATTR_MAP_PROVIDER_VERSION	= "Version";								//$NON-NLS-1$

	private static boolean					_isDebugViewVisible;
	private static Map25DebugView			_map25DebugView;

	private static ArrayList<Map25Provider>	_allMapProvider;

//    private final static String 	DEFAULT_URL 	= "http://opensciencemap.org/tiles/vtm";
//    private final static String 	DEFAULT_PATH	= "/{Z}/{X}/{Y}.vtm";

//    private final static String 	DEFAULT_URL 	= "https://tile.mapzen.com/mapzen/vector/v1/all";
//    private final static String 	DEFAULT_PATH 	= "/{Z}/{X}/{Y}.mvt";

//	  private final static String	DEFAULT_URL		= "http://192.168.99.99:8080/all";
//	  private final static String	DEFAULT_PATH	= "/{Z}/{X}/{Y}.mvt";

	/**
	 * @return
	 */
	public static ArrayList<Map25Provider> getAllMapProviders() {

		if (_allMapProvider == null) {
			_allMapProvider = loadMapProvider();
		}

		return _allMapProvider;
	}

	/**
	 * @return Returns the map vtm debug view when it is visible, otherwise <code>null</code>
	 */
	public static Map25DebugView getMap25DebugView() {

		if (_map25DebugView != null && _isDebugViewVisible) {
			return _map25DebugView;
		}

		return null;
	}

	private static File getXmlFile() {

		return _stateLocation.append(MAP_PROVIDER_FILE_NAME).toFile();
	}

	public static boolean isDebugViewVisible() {
		return _isDebugViewVisible;
	}

	private static ArrayList<Map25Provider> loadMapProvider() {

		final ArrayList<Map25Provider> mapProvider = new ArrayList<>();

		return mapProvider;
	}

	public static void saveMapProvider(final ArrayList<Map25Provider> allMapProvider) {

		final XMLMemento xmlRoot = saveMapProvider_10();
		final File xmlFile = getXmlFile();

		Util.writeXml(xmlRoot, xmlFile);
	}

	/**
	 * @return
	 */
	private static XMLMemento saveMapProvider_10() {

		XMLMemento xmlRoot = null;

		try {

			xmlRoot = saveMapProvider_20_Root();

			// loop: profiles
			for (final Map25Provider mapProvider : _allMapProvider) {

				final IMemento xmlProfile = xmlRoot.createChild(TAG_PROFILE);

				xmlProfile.putString(ATTR_API_KEY, mapProvider.apiKey);
				xmlProfile.putString(ATTR_NAME, mapProvider.name);
				xmlProfile.putString(ATTR_TILE_PATH, mapProvider.tilePath);
				xmlProfile.putString(ATTR_URL, mapProvider.url);
			}

		} catch (final Exception e) {
			StatusUtil.log(e);
		}

		return xmlRoot;
	}

	private static XMLMemento saveMapProvider_20_Root() {

		final XMLMemento xmlRoot = XMLMemento.createWriteRoot(TAG_ROOT);

		Util.setXmlDefaultHeader(xmlRoot, _bundle);

		// map provider version
		xmlRoot.putInteger(ATTR_MAP_PROVIDER_VERSION, MAP_PROVIDER_VERSION);

		return xmlRoot;
	}

	static void setDebugView(final Map25DebugView map25DebugView) {
		_map25DebugView = map25DebugView;
	}

	static void setDebugViewVisible(final boolean isDebugVisible) {
		_isDebugViewVisible = isDebugVisible;
	}

	public static void updateOfflineLocation() {
		// TODO Auto-generated method stub

	}

}
