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

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;
import org.osgi.framework.Bundle;

import de.byteholder.geoclipse.mapprovider.IMapProviderListener;

public class Map25ProviderManager {

//	private static final String				ID							= "net.tourbook.map25.Map25Manager";		//$NON-NLS-1$

// SET_FORMATTING_OFF
	private static final Bundle				_bundle						= TourbookPlugin.getDefault().getBundle();
	private static final IPath				_stateLocation				= Platform.getStateLocation(_bundle);
// SET_FORMATTING_ON

	private static final String				MAP_PROVIDER_FILE_NAME		= "map25-provider.xml";						//$NON-NLS-1$
	private static final int				MAP_PROVIDER_VERSION		= 1;

	private static final String				TAG_ROOT					= "Map25Providers";							//$NON-NLS-1$
	private static final String				TAG_MAP_PROVIDER			= "MapProvider";							//$NON-NLS-1$

	private static final String				ATTR_API_KEY				= "APIKey";									//$NON-NLS-1$
	private static final String				ATTR_DESCRIPTION			= "Description";							//$NON-NLS-1$
	private static final String				ATTR_IS_DEFAULT				= "IsDefault";								//$NON-NLS-1$
	private static final String				ATTR_IS_ENABLED				= "IsEnabled";								//$NON-NLS-1$
	private static final String				ATTR_MAP_PROVIDER_VERSION	= "Version";								//$NON-NLS-1$
	private static final String				ATTR_NAME					= "Name";									//$NON-NLS-1$
	private static final String				ATTR_TILE_PATH				= "TilePath";								//$NON-NLS-1$
	private static final String				ATTR_TILE_ENCODING			= "TileEncoding";							//$NON-NLS-1$
	private static final String				ATTR_URL					= "Url";									//$NON-NLS-1$
	private static final String				ATTR_UUID					= "UUID";									//$NON-NLS-1$

	private static boolean					_isDebugViewVisible;
	private static Map25DebugView			_map25DebugView;

	private static ArrayList<Map25Provider>	_allMapProvider;

	
	/**
	 * Contains the default default map provider
	 */
	private static Map25Provider			_defaultMapProvider			= createMapProvider_Default();

	private static final ListenerList		_mapProviderListeners		= new ListenerList(
			ListenerList.IDENTITY);

	public static void addMapProviderListener(final IMapProviderListener listener) {
		_mapProviderListeners.add(listener);
	}

	/**
	 * opensciencemap.org
	 */
	private static Map25Provider createMapProvider_Default() {

		final Map25Provider mapProvider = new Map25Provider();

		mapProvider.isDefault = true;
		mapProvider.isEnabled = true;
		mapProvider.name = Messages.Map25_Provider_OpenScienceMap_Name;
		mapProvider.url = "http://opensciencemap.org/tiles/vtm"; //$NON-NLS-1$
		mapProvider.tilePath = "/{Z}/{X}/{Y}.vtm"; //$NON-NLS-1$
		mapProvider.tileEncoding = TileEncoding.VTM;
		mapProvider.description = Messages.Map25_Provider_OpenScienceMap_Description;

		_defaultMapProvider = mapProvider;

		return mapProvider;
	}

	/**
	 * Mapsforge 
	 */
	private static Map25Provider createMapProvider_Mapsforge() {

		final Map25Provider mapProvider = new Map25Provider();

		mapProvider.isEnabled = false;
		mapProvider.name = "Mapsforge";
		mapProvider.url = "http://opensciencemap.org/tiles/vtm"; //$NON-NLS-1$
		mapProvider.tilePath = "/{Z}/{X}/{Y}.vtm"; //$NON-NLS-1$
		mapProvider.tileEncoding = TileEncoding.MF;
		mapProvider.description = "Offline Mapsforgemaps eg. openandromaps.org";

		return mapProvider;
	}	

	
	/**
	 * mapzen
	 */
	private static Map25Provider createMapProvider_Mapzen() {

		final Map25Provider mapProvider = new Map25Provider();

		mapProvider.isEnabled = false;
		mapProvider.name = Messages.Map25_Provider_MapzenVectorTiles_Name;
		mapProvider.url = "https://tile.mapzen.com/mapzen/vector/v1/all"; //$NON-NLS-1$
		mapProvider.tilePath = "/{Z}/{X}/{Y}.mvt"; //$NON-NLS-1$
		mapProvider.tileEncoding = TileEncoding.MVT;
		mapProvider.apiKey = "mapzen-xxxxxxx"; //$NON-NLS-1$
		mapProvider.description = Messages.Map25_Provider_MapzenVectorTiles_Description;

		return mapProvider;
	}

	/**
	 * Own map tile server
	 */
	private static Map25Provider createMapProvider_MyTileServer() {

		final Map25Provider mapProvider = new Map25Provider();

		mapProvider.isEnabled = false;
		mapProvider.name = Messages.Map25_Provider_MyTileServer_Name;
		mapProvider.url = "http://192.168.99.99:8080/all"; //$NON-NLS-1$
		mapProvider.tilePath = "/{Z}/{X}/{Y}.mvt"; //$NON-NLS-1$
		mapProvider.tileEncoding = TileEncoding.MVT;
		mapProvider.description = Messages.Map25_Provider_MyTileServer_Description;

		return mapProvider;
	}


	
	
	private static void fireChangeEvent() {

		final Object[] allListeners = _mapProviderListeners.getListeners();
		for (final Object listener : allListeners) {
			((IMapProviderListener) listener).mapProviderListChanged();
		}
	}

	/**
	 * @return Returns all available {@link Map25Provider}.
	 */
	public static ArrayList<Map25Provider> getAllMapProviders() {

		if (_allMapProvider == null) {
			_allMapProvider = loadMapProvider();
		}

		return _allMapProvider;
	}

	public static Map25Provider getDefaultMapProvider() {
		return _defaultMapProvider;
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

	public static Map25Provider getMapProvider(final String mapProviderId) {

		for (final Map25Provider map25Provider : getAllMapProviders()) {

			if (mapProviderId.equals(map25Provider.getId())) {
				return map25Provider;
			}
		}

		return getDefaultMapProvider();
	}

	private static File getXmlFile() {

		return _stateLocation.append(MAP_PROVIDER_FILE_NAME).toFile();
	}

	public static boolean isDebugViewVisible() {
		return _isDebugViewVisible;
	}

	/**
	 * This can be called also from the map app thread.
	 * 
	 * @return
	 */
	private static synchronized ArrayList<Map25Provider> loadMapProvider() {

		if (_allMapProvider != null) {
			return _allMapProvider;
		}

		final ArrayList<Map25Provider> allMapProvider = new ArrayList<>();

		final File xmlFile = getXmlFile();

		if (xmlFile.exists()) {

			try (BufferedReader reader = Files.newBufferedReader(Paths.get(xmlFile.toURI()))) {

				final XMLMemento xmlRoot = XMLMemento.createReadRoot(reader);
				for (final IMemento mementoChild : xmlRoot.getChildren()) {

					final XMLMemento xml = (XMLMemento) mementoChild;
					if (TAG_MAP_PROVIDER.equals(xml.getType())) {

						final String xmlUUID = Util.getXmlString(xml, ATTR_UUID, UI.EMPTY_STRING);

						final Map25Provider mp = new Map25Provider(xmlUUID);

						mp.isDefault = Util.getXmlBoolean(xml, ATTR_IS_DEFAULT, false);
						mp.isEnabled = Util.getXmlBoolean(xml, ATTR_IS_ENABLED, false);

						mp.apiKey = Util.getXmlString(xml, ATTR_API_KEY, UI.EMPTY_STRING);
						mp.description = Util.getXmlString(xml, ATTR_DESCRIPTION, UI.EMPTY_STRING);
						mp.name = Util.getXmlString(xml, ATTR_NAME, UI.EMPTY_STRING);
						mp.tilePath = Util.getXmlString(xml, ATTR_TILE_PATH, UI.EMPTY_STRING);
						mp.url = Util.getXmlString(xml, ATTR_URL, UI.EMPTY_STRING);

						mp.tileEncoding = (TileEncoding) Util.getXmlEnum(xml, ATTR_TILE_ENCODING, TileEncoding.VTM);

						allMapProvider.add(mp);
					}
				}

			} catch (final Exception e) {
				StatusUtil.log(e);
			}

			replaceDefault(allMapProvider);

		} else {

			/*
			 * Create default map providers
			 */
			allMapProvider.add(_defaultMapProvider);
			allMapProvider.add(createMapProvider_Mapzen());
			allMapProvider.add(createMapProvider_Mapsforge());
			allMapProvider.add(createMapProvider_MyTileServer());
		}

		/*
		 * Ensure that at least one map provider is enabled
		 */
		boolean isOneEnabled = false;
		for (final Map25Provider map25Provider : allMapProvider) {
			if (map25Provider.isEnabled) {
				isOneEnabled = true;
				break;
			}
		}

		if (!isOneEnabled) {
			// enable default
			_defaultMapProvider.isEnabled = true;
		}

		return allMapProvider;
	}

	public static void removeMapProviderListener(final IMapProviderListener listener) {

		if (listener != null) {
			_mapProviderListeners.remove(listener);
		}
	}

	/**
	 * Replace default with new default provider (cloned/loaded) that the uuid is correctly setup
	 * 
	 * @param allMapProvider
	 */
	private static void replaceDefault(final ArrayList<Map25Provider> allMapProvider) {

		for (final Map25Provider map25Provider1 : allMapProvider) {

			if (map25Provider1.isDefault) {
				_defaultMapProvider = map25Provider1;
				break;
			}
		}
	}

	public static void saveMapProvider(final ArrayList<Map25Provider> allMapProvider) {

		replaceDefault(allMapProvider);

		// update model
		_allMapProvider.clear();
		_allMapProvider.addAll(allMapProvider);

		final XMLMemento xmlRoot = saveMapProvider_10_CreateXml();
		final File xmlFile = getXmlFile();

		Util.writeXml(xmlRoot, xmlFile);

		fireChangeEvent();
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

				xml.putBoolean(ATTR_IS_ENABLED, mapProvider.isEnabled);
				xml.putBoolean(ATTR_IS_DEFAULT, mapProvider.isDefault);

				xml.putString(ATTR_API_KEY, mapProvider.apiKey);
				xml.putString(ATTR_DESCRIPTION, mapProvider.description);
				xml.putString(ATTR_NAME, mapProvider.name);
				xml.putString(ATTR_TILE_PATH, mapProvider.tilePath);
				xml.putString(ATTR_URL, mapProvider.url);
				xml.putString(ATTR_UUID, mapProvider.getId().toString());

				Util.setXmlEnum(xml, ATTR_TILE_ENCODING, mapProvider.tileEncoding);
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
