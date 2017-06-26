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

import java.io.BufferedReader;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
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

	private static final String				TAG_ROOT					= "Map25Providers";							//$NON-NLS-1$
	private static final String				TAG_MAP_PROVIDER			= "MapProvider";							//$NON-NLS-1$

	private static final String				ATTR_API_KEY				= "APIKey";									//$NON-NLS-1$
	private static final String				ATTR_DESCRIPTION			= "Description";							//$NON-NLS-1$
	private static final String				ATTR_OFFLINE_FOLDER			= "OfflineFolder";							//$NON-NLS-1$
	private static final String				ATTR_MAP_PROVIDER_VERSION	= "Version";								//$NON-NLS-1$
	private static final String				ATTR_NAME					= "Name";									//$NON-NLS-1$
	private static final String				ATTR_TILE_PATH				= "TilePath";								//$NON-NLS-1$
	private static final String				ATTR_URL					= "Url";									//$NON-NLS-1$

	private static boolean					_isDebugViewVisible;
	private static Map25DebugView			_map25DebugView;

	private static ArrayList<Map25Provider>	_allMapProvider;

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

		final ArrayList<Map25Provider> allMapProvider = new ArrayList<>();

		final File xmlFile = getXmlFile();

		if (xmlFile.exists()) {

			try (BufferedReader reader = Files.newBufferedReader(Paths.get(xmlFile.toURI()))) {

				final XMLMemento xmlRoot = XMLMemento.createReadRoot(reader);
				for (final IMemento mementoChild : xmlRoot.getChildren()) {

					final XMLMemento xml = (XMLMemento) mementoChild;
					if (TAG_MAP_PROVIDER.equals(xml.getType())) {

						final Map25Provider mp = new Map25Provider();

						mp.apiKey = Util.getXmlString(xml, ATTR_API_KEY, UI.EMPTY_STRING);
						mp.description = Util.getXmlString(xml, ATTR_DESCRIPTION, UI.EMPTY_STRING);
						mp.name = Util.getXmlString(xml, ATTR_NAME, UI.EMPTY_STRING);
						mp.offlineFolder = Util.getXmlString(xml, ATTR_OFFLINE_FOLDER, UI.EMPTY_STRING);
						mp.tilePath = Util.getXmlString(xml, ATTR_TILE_PATH, UI.EMPTY_STRING);
						mp.url = Util.getXmlString(xml, ATTR_URL, UI.EMPTY_STRING);

						allMapProvider.add(mp);
					}
				}

			} catch (final Exception e) {
				StatusUtil.log(e);
			}

		} else {

			/*
			 * Create default map providers
			 */

			Map25Provider mapProvider;

			/*
			 * opensciencemap.org
			 */
			mapProvider = new Map25Provider();
			mapProvider.name = "Open Science Map";
			mapProvider.url = "http://opensciencemap.org/tiles/vtm";
			mapProvider.tilePath = "/{Z}/{X}/{Y}.vtm";
			mapProvider.offlineFolder = "open-science-map";
			mapProvider.description = "This server is sometimes very slow !\n\n"
					+ "http://opensciencemap.org";

			allMapProvider.add(mapProvider);

			/*
			 * mapzen
			 */
			mapProvider = new Map25Provider();
			mapProvider.name = "Mapzen Vector Tiles";
			mapProvider.url = "https://tile.mapzen.com/mapzen/vector/v1/all";
			mapProvider.tilePath = "/{Z}/{X}/{Y}.mvt";
			mapProvider.apiKey = "mapzen-xxxxxxx";
			mapProvider.offlineFolder = "mapzen";
			mapProvider.description = "https://mapzen.com/projects/vector-tiles/";

			allMapProvider.add(mapProvider);

			/*
			 * Own map tile server
			 */
			mapProvider = new Map25Provider();
			mapProvider.name = "My Tile Server";
			mapProvider.url = "http://192.168.99.99:8080/all";
			mapProvider.tilePath = "/{Z}/{X}/{Y}.mvt";
			mapProvider.offlineFolder = "my-tile-server";
			mapProvider.description = "How to build an own tile server is described here\n\n"
					+ "https://github.com/tilezen/vector-datasource/wiki/Mapzen-Vector-Tile-Service";

			allMapProvider.add(mapProvider);
		}

		return allMapProvider;
	}

	public static void saveMapProvider(final ArrayList<Map25Provider> allMapProvider) {

		// update model
		_allMapProvider.clear();
		_allMapProvider.addAll(allMapProvider);

		final XMLMemento xmlRoot = saveMapProvider_10_CreateXml();
		final File xmlFile = getXmlFile();

		Util.writeXml(xmlRoot, xmlFile);
	}

	/**
	 * @return
	 */
	private static XMLMemento saveMapProvider_10_CreateXml() {

		XMLMemento xmlRoot = null;

		try {

			xmlRoot = saveMapProvider_20_CreateRoot();

			// loop: profiles
			for (final Map25Provider mapProvider : _allMapProvider) {

				final IMemento xml = xmlRoot.createChild(TAG_MAP_PROVIDER);

				xml.putString(ATTR_API_KEY, mapProvider.apiKey);
				xml.putString(ATTR_NAME, mapProvider.name);
				xml.putString(ATTR_TILE_PATH, mapProvider.tilePath);
				xml.putString(ATTR_URL, mapProvider.url);
				xml.putString(ATTR_DESCRIPTION, mapProvider.description);
				xml.putString(ATTR_OFFLINE_FOLDER, mapProvider.offlineFolder);
			}

		} catch (final Exception e) {
			StatusUtil.log(e);
		}

		return xmlRoot;
	}

	private static XMLMemento saveMapProvider_20_CreateRoot() {

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
