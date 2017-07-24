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
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.map25.layer.tourtrack.Map25TrackConfig;
import net.tourbook.map3.Messages;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

public class Map25ConfigManager {

	private static final String	CONFIG_NAME_DEFAULT			= Messages.Track_Config_ConfigName_Default;
	public static final String	CONFIG_NAME_UNKNOWN			= Messages.Track_Config_ConfigName_Unknown;

// SET_FORMATTING_OFF
// SET_FORMATTING_ON

	static final String			MARKER_DEFAULT_ID_1			= "#1";										//$NON-NLS-1$
	static final String			MARKER_DEFAULT_ID_2			= "#2";										//$NON-NLS-1$
	static final String			MARKER_DEFAULT_ID_3			= "#3";										//$NON-NLS-1$
	static final String			MARKER_DEFAULT_ID_4			= "#4";										//$NON-NLS-1$
	static final String			MARKER_DEFAULT_ID_5			= "#5";										//$NON-NLS-1$
	static final String			MARKER_DEFAULT_ID_6			= "#6";										//$NON-NLS-1$
	static final String			MARKER_DEFAULT_ID_7			= "#7";										//$NON-NLS-1$
	static final String			MARKER_DEFAULT_ID_8			= "#8";										//$NON-NLS-1$
	static final String			MARKER_DEFAULT_ID_9			= "#9";										//$NON-NLS-1$
	static final String			MARKER_DEFAULT_ID_10		= "#10";									//$NON-NLS-1$

	private static final Bundle	_bundle						= TourbookPlugin.getDefault().getBundle();
	private static final IPath	_stateLocation				= Platform.getStateLocation(_bundle);

	private static final String	CONFIG_FILE_NAME			= "map25-config.xml";						//$NON-NLS-1$

	/**
	 * Version number is not yet used.
	 */
	private static final int	CONFIG_VERSION				= 1;

	// common attributes
	private static final String	ATTR_ACTIVE_CONFIG_ID		= "activeConfigId";							//$NON-NLS-1$
	private static final String	ATTR_ID						= "id";										//$NON-NLS-1$
	private static final String	ATTR_CONFIG_NAME			= "name";									//$NON-NLS-1$

	/*
	 * Root
	 */
	private static final String	TAG_ROOT					= "Map25Configuration";						//$NON-NLS-1$
	private static final String	ATTR_CONFIG_VERSION			= "configVersion";							//$NON-NLS-1$

	/*
	 * Tour tracks
	 */
	private static final String	TAG_TOUR_TRACKS				= "TourTracks";								//$NON-NLS-1$
	private static final String	TAG_TRACK					= "Track";									//$NON-NLS-1$

	private static final String	ATTR_ANIMATION_TIME			= "animationTime";							//$NON-NLS-1$

	// outline
	private static final String	TAG_OUTLINE					= "Outline";								//$NON-NLS-1$
	private static final String	ATTR_OUTLINE_WIDTH			= "width";									//$NON-NLS-1$

	/*
	 * Tour Markers
	 */
	private static final String	TAG_TOUR_MARKERS			= "TourMarkers";							//$NON-NLS-1$
	private static final String	TAG_MARKER					= "Marker";									//$NON-NLS-1$
	private static final String	TAG_CLUSTER_BACKGROUND		= "ClusterBackground";						//$NON-NLS-1$
	private static final String	TAG_CLUSTER_FOREGROUND		= "ClusterForeground";						//$NON-NLS-1$

	// outline
	public static final float	OUTLINE_WIDTH_MIN			= 1.0f;
	public static final float	OUTLINE_WIDTH_MAX			= 10.0f;
	public static final float	DEFAULT_OUTLINE_WIDTH		= 2.5f;

	// colors
	public static final RGB		DEFAULT_CLUSTER_FOREGROUND	= new RGB(0xff, 0xff, 0xff);
	public static final RGB		DEFAULT_CLUSTER_BACKGROUND	= new RGB(0xFC, 0x67, 0x00);
	public static final RGB		DEFAULT_OUTLINE_COLOR		= new RGB(0x80, 0x0, 0x80);

	// other properties
	public static final int		DEFAULT_ANIMATION_TIME		= 2000;

	static {

	}

	/**
	 * Contains all configurations which are loaded from a xml file.
	 */
	private static final ArrayList<Map25TrackConfig>	_allTrackConfigs	= new ArrayList<>();
	private static Map25TrackConfig						_activeTrackConfig;
	private static String								_activeTrackConfigIdFromXml;

	private static final ArrayList<Map25MarkerConfig>	_allMarkerConfigs	= new ArrayList<>();
	private static Map25MarkerConfig					_activeMarkerConfig;
	private static String								_activeMarkerConfigIdFromXml;

	private static XMLMemento create_Root() {

		final XMLMemento xmlRoot = XMLMemento.createWriteRoot(TAG_ROOT);

		// date/time
		xmlRoot.putString(Util.ATTR_ROOT_DATETIME, TimeTools.now().toString());

		// plugin version
		final Version version = _bundle.getVersion();
		xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MAJOR, version.getMajor());
		xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MINOR, version.getMinor());
		xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MICRO, version.getMicro());
		xmlRoot.putString(Util.ATTR_ROOT_VERSION_QUALIFIER, version.getQualifier());

		// config version
		xmlRoot.putInteger(ATTR_CONFIG_VERSION, CONFIG_VERSION);

		return xmlRoot;
	}

	private static void createDefaults_Markers() {

		_allMarkerConfigs.clear();

		// append custom configurations
		for (int customIndex = 1; customIndex < 11; customIndex++) {
			_allMarkerConfigs.add(createDefaults_Markers_One(customIndex));
		}
	}

	/**
	 * @param defaultIndex
	 *            Index starts with 1.
	 * @return
	 */
	private static Map25MarkerConfig createDefaults_Markers_One(final int defaultIndex) {

		final Map25MarkerConfig config = new Map25MarkerConfig();

		final RGB fgWhite = new RGB(0xff, 0xff, 0xff);
		final RGB fgBlack = new RGB(0x0, 0x0, 0x0);

		final RGB bg1 = new RGB(0x00, 0xA0, 0xED);
		final RGB bg2 = new RGB(0xC6, 0x00, 0xA2);
		final RGB bg3 = new RGB(0x00, 0xC4, 0x2C);
		final RGB bg4 = new RGB(0xFF, 0xC9, 0x00);
		final RGB bg5 = new RGB(0xFF, 0x00, 0x62);

		switch (defaultIndex) {

		case 1:
			config.name = config.defaultId = MARKER_DEFAULT_ID_1;
			config.clusterColorForeground = fgWhite;
			config.clusterColorBackground = bg1;
			break;
		case 2:
			config.name = config.defaultId = MARKER_DEFAULT_ID_2;
			config.clusterColorForeground = fgBlack;
			config.clusterColorBackground = bg1;
			break;

		case 3:
			config.name = config.defaultId = MARKER_DEFAULT_ID_3;
			config.clusterColorForeground = fgWhite;
			config.clusterColorBackground = bg2;
			break;
		case 4:
			config.name = config.defaultId = MARKER_DEFAULT_ID_4;
			config.clusterColorForeground = fgBlack;
			config.clusterColorBackground = bg2;
			break;

		case 5:
			config.name = config.defaultId = MARKER_DEFAULT_ID_5;
			config.clusterColorForeground = fgWhite;
			config.clusterColorBackground = bg3;
			break;
		case 6:
			config.name = config.defaultId = MARKER_DEFAULT_ID_6;
			config.clusterColorForeground = fgBlack;
			config.clusterColorBackground = bg3;
			break;

		case 7:
			config.name = config.defaultId = MARKER_DEFAULT_ID_7;
			config.clusterColorForeground = fgWhite;
			config.clusterColorBackground = bg4;
			break;
		case 8:
			config.name = config.defaultId = MARKER_DEFAULT_ID_8;
			config.clusterColorForeground = fgBlack;
			config.clusterColorBackground = bg4;
			break;

		case 9:
			config.name = config.defaultId = MARKER_DEFAULT_ID_9;
			config.clusterColorForeground = fgWhite;
			config.clusterColorBackground = bg5;
			break;
		case 10:
			config.name = config.defaultId = MARKER_DEFAULT_ID_10;
			config.clusterColorForeground = fgBlack;
			config.clusterColorBackground = bg5;
			break;

		}

		return config;
	}

	private static void createDefaults_Tracks() {

		_allTrackConfigs.clear();

		_allTrackConfigs.add(createDefaults_Tracks_One(CONFIG_NAME_DEFAULT, DEFAULT_OUTLINE_WIDTH));

		// append custom configurations
		for (int customIndex = 0; customIndex < 10; customIndex++) {

			_allTrackConfigs.add(

					createDefaults_Tracks_One(

							String.format("%s #%d", CONFIG_NAME_DEFAULT, (customIndex + 1)),

							customIndex + 1

					));
		}
	}

	private static Map25TrackConfig createDefaults_Tracks_One(final String name, final float outlineWidth) {

		final Map25TrackConfig trackConfig = new Map25TrackConfig();

		trackConfig.name = name;
		trackConfig.outlineWidth = outlineWidth;

		return trackConfig;
	}

	private static void createXml_FromMarkerConfig(final Map25MarkerConfig config, final IMemento xmlMarkers) {

		// <Marker>
		final IMemento xmlConfig = xmlMarkers.createChild(TAG_MARKER);
		{
			xmlConfig.putString(ATTR_ID, config.id);
			xmlConfig.putString(ATTR_CONFIG_NAME, config.name);

			// Cluster color
			Util.setXmlRgb(xmlConfig, TAG_CLUSTER_FOREGROUND, config.clusterColorForeground);
			Util.setXmlRgb(xmlConfig, TAG_CLUSTER_BACKGROUND, config.clusterColorBackground);
		}
	}

	private static void createXml_FromTrackConfig(final Map25TrackConfig config, final IMemento xmlTourTracks) {

		// <Track>
		final IMemento xmlConfig = xmlTourTracks.createChild(TAG_TRACK);
		{
			xmlConfig.putString(ATTR_ID, config.id);
			xmlConfig.putString(ATTR_CONFIG_NAME, config.name);

			xmlConfig.putInteger(ATTR_ANIMATION_TIME, config.animationTime);

			// <Outline>
			final IMemento xmlOutline = Util.setXmlRgb(xmlConfig, TAG_OUTLINE, config.outlineColor);
			{
				xmlOutline.putFloat(ATTR_OUTLINE_WIDTH, config.outlineWidth);
			}
		}
	}

	public static Map25MarkerConfig getActiveMarkerConfig() {

		if (_activeMarkerConfig == null) {
			readConfigFromXml();
		}

		return _activeMarkerConfig;
	}

	/**
	 * @return Returns the index for the {@link #_activeMarkerConfig}, the index starts with 0.
	 */
	public static int getActiveMarkerConfigIndex() {

		final Map25MarkerConfig activeConfig = getActiveMarkerConfig();

		for (int configIndex = 0; configIndex < _allMarkerConfigs.size(); configIndex++) {

			final Map25MarkerConfig config = _allMarkerConfigs.get(configIndex);

			if (config.equals(activeConfig)) {
				return configIndex;
			}
		}

		// this case should not happen but ensure that a correct config is set

		_activeMarkerConfig = _allMarkerConfigs.get(0);

		return 0;
	}

	public static Map25TrackConfig getActiveTourTrackConfig() {

		if (_activeTrackConfig == null) {
			readConfigFromXml();
		}

		return _activeTrackConfig;
	}

	/**
	 * @return Returns the index of the active config within all configs.
	 */
	public static int getActiveTourTrackConfigIndex() {

		final Map25TrackConfig activeConfig = getActiveTourTrackConfig();

		for (int configIndex = 0; configIndex < _allTrackConfigs.size(); configIndex++) {

			final Map25TrackConfig config = _allTrackConfigs.get(configIndex);

			if (config == activeConfig) {
				return configIndex;
			}
		}

		// this case should not happen but ensure that a correct config is set

		_activeTrackConfig = _allTrackConfigs.get(0);

		return 0;
	}

	public static ArrayList<Map25MarkerConfig> getAllMarkerConfigs() {

		// ensure configs are loaded
		getActiveMarkerConfig();

		return _allMarkerConfigs;
	}

	public static ArrayList<Map25TrackConfig> getAllTourTrackConfigs() {

		// ensure configs are loaded
		getActiveTourTrackConfig();

		return _allTrackConfigs;
	}

	private static Map25MarkerConfig getConfig_Marker() {

		Map25MarkerConfig activeConfig = null;

		if (_activeMarkerConfigIdFromXml != null) {

			// ensure config id belongs to a config which is available

			for (final Map25MarkerConfig config : _allMarkerConfigs) {

				if (config.id.equals(_activeMarkerConfigIdFromXml)) {

					activeConfig = config;
					break;
				}
			}
		}

		if (activeConfig == null) {

			// this case should not happen, create a config

			StatusUtil.log("Created default config for marker properties");//$NON-NLS-1$

			createDefaults_Markers();

			activeConfig = _allMarkerConfigs.get(0);
		}

		return activeConfig;
	}

	private static Map25TrackConfig getConfig_Track() {

		Map25TrackConfig activeConfig = null;

		if (_activeTrackConfigIdFromXml != null) {

			// ensure config id belongs to a config which is available

			for (final Map25TrackConfig config : _allTrackConfigs) {

				if (config.id.equals(_activeTrackConfigIdFromXml)) {

					activeConfig = config;
					break;
				}
			}
		}

		if (activeConfig == null) {

			// this case should not happen, create a config

			StatusUtil.log("Created default config for tour track properties");//$NON-NLS-1$

			createDefaults_Tracks();

			activeConfig = _allTrackConfigs.get(0);
		}

		return activeConfig;
	}

	private static File getConfigXmlFile() {

		final File layerFile = _stateLocation.append(CONFIG_FILE_NAME).toFile();

		return layerFile;
	}

	/**
	 * Parse configuration xml.
	 * 
	 * @param xmlRoot
	 * @param allTourTrackConfig
	 */
	private static void parse_000_Tracks(	final XMLMemento xmlRoot,
											final ArrayList<Map25TrackConfig> allTourTrackConfig) {

		if (xmlRoot == null) {
			return;
		}

		final XMLMemento xmlTourTracks = (XMLMemento) xmlRoot.getChild(TAG_TOUR_TRACKS);

		if (xmlTourTracks == null) {
			return;
		}

		_activeTrackConfigIdFromXml = Util.getXmlString(xmlTourTracks, ATTR_ACTIVE_CONFIG_ID, null);

		for (final IMemento mementoConfig : xmlTourTracks.getChildren()) {

			final XMLMemento xmlConfig = (XMLMemento) mementoConfig;

			try {

				final String xmlConfigType = xmlConfig.getType();

				if (xmlConfigType.equals(TAG_TRACK)) {

					// <Track>

					final Map25TrackConfig trackConfig = new Map25TrackConfig();

					parse_010_Configuration(xmlConfig, trackConfig);
					parse_020_Track(xmlConfig, trackConfig);

					allTourTrackConfig.add(trackConfig);
				}

			} catch (final Exception e) {
				StatusUtil.log(Util.dumpMemento(xmlConfig), e);
			}
		}
	}

	private static void parse_010_Configuration(final XMLMemento xmlConfig, final Map25TrackConfig config) {

		config.id = Util.getXmlString(
				xmlConfig, //
				ATTR_ID,
				Long.toString(System.nanoTime()));

		config.name = Util.getXmlString(
				xmlConfig, //
				ATTR_CONFIG_NAME,
				CONFIG_NAME_UNKNOWN);

	}

	private static void parse_020_Track(final XMLMemento xmlConfig, final Map25TrackConfig config) {

		for (final IMemento mementoConfigChild : xmlConfig.getChildren()) {

			final XMLMemento xmlConfigChild = (XMLMemento) mementoConfigChild;
			final String configTag = xmlConfigChild.getType();

// SET_FORMATTING_OFF
			
			switch (configTag) {

			case TAG_OUTLINE:

				config.outlineWidth = Util.getXmlFloatFloat(xmlConfigChild, ATTR_OUTLINE_WIDTH, DEFAULT_OUTLINE_WIDTH, OUTLINE_WIDTH_MIN, OUTLINE_WIDTH_MAX);
				config.outlineColor = Util.getXmlRgb(xmlConfigChild, DEFAULT_OUTLINE_COLOR);
				config.animationTime = Util.getXmlInteger(xmlConfigChild, ATTR_ANIMATION_TIME, DEFAULT_ANIMATION_TIME);

				break;
			}
			
// SET_FORMATTING_ON
		}
	}

	private static void parse_200_Markers(	final XMLMemento xmlRoot,
											final ArrayList<Map25MarkerConfig> allMarkerConfigs) {

		if (xmlRoot == null) {
			return;
		}

		final XMLMemento xmlMarkers = (XMLMemento) xmlRoot.getChild(TAG_TOUR_MARKERS);

		if (xmlMarkers == null) {
			return;
		}

		_activeMarkerConfigIdFromXml = Util.getXmlString(xmlMarkers, ATTR_ACTIVE_CONFIG_ID, null);

		for (final IMemento mementoConfig : xmlMarkers.getChildren()) {

			final XMLMemento xmlConfig = (XMLMemento) mementoConfig;

			try {

				final String xmlConfigType = xmlConfig.getType();

				if (xmlConfigType.equals(TAG_MARKER)) {

					// <Track>

					final Map25MarkerConfig markerConfig = new Map25MarkerConfig();

					parse_210_Configuration(xmlConfig, markerConfig);
					parse_220_Marker(xmlConfig, markerConfig);

					allMarkerConfigs.add(markerConfig);
				}

			} catch (final Exception e) {
				StatusUtil.log(Util.dumpMemento(xmlConfig), e);
			}
		}
	}

	private static void parse_210_Configuration(final XMLMemento xmlConfig, final Map25MarkerConfig config) {

		config.id = Util.getXmlString(
				xmlConfig, //
				ATTR_ID,
				Long.toString(System.nanoTime()));

		config.name = Util.getXmlString(
				xmlConfig, //
				ATTR_CONFIG_NAME,
				CONFIG_NAME_UNKNOWN);
	}

	private static void parse_220_Marker(final XMLMemento xmlConfig, final Map25MarkerConfig config) {

		for (final IMemento mementoConfigChild : xmlConfig.getChildren()) {

			final XMLMemento xmlConfigChild = (XMLMemento) mementoConfigChild;
			final String configTag = xmlConfigChild.getType();

			switch (configTag) {

			case TAG_CLUSTER_BACKGROUND:
				config.clusterColorBackground = Util.getXmlRgb(xmlConfigChild, DEFAULT_CLUSTER_BACKGROUND);
				break;

			case TAG_CLUSTER_FOREGROUND:
				config.clusterColorForeground = Util.getXmlRgb(xmlConfigChild, DEFAULT_CLUSTER_FOREGROUND);
				break;
			}
		}
	}

	/**
	 * Read or create configuration a xml file
	 * 
	 * @return
	 */
	private static void readConfigFromXml() {

		InputStreamReader reader = null;

		try {

			XMLMemento xmlRoot = null;

			// try to get layer structure from saved xml file
			final File layerFile = getConfigXmlFile();
			final String absoluteLayerPath = layerFile.getAbsolutePath();

			final File inputFile = new File(absoluteLayerPath);
			if (inputFile.exists()) {

				try {

					reader = new InputStreamReader(new FileInputStream(inputFile), UI.UTF_8);
					xmlRoot = XMLMemento.createReadRoot(reader);

				} catch (final Exception e) {
					// ignore
				}
			}

			// parse xml
			parse_000_Tracks(xmlRoot, _allTrackConfigs);
			parse_200_Markers(xmlRoot, _allMarkerConfigs);

			// ensure config is created
			if (_allTrackConfigs.size() == 0) {
				createDefaults_Tracks();
			}

			if (_allMarkerConfigs.size() == 0) {
				createDefaults_Markers();
			}

			_activeTrackConfig = getConfig_Track();
			_activeMarkerConfig = getConfig_Marker();

		} catch (final Exception e) {
			StatusUtil.log(e);
		} finally {
			Util.close(reader);
		}
	}

	public static void resetActiveMarkerConfiguration() {

		final int activeMarkerConfigIndex = getActiveMarkerConfigIndex();

		// remove old config
		_allMarkerConfigs.remove(_activeMarkerConfig);

		// create new config
		final int configIndex = activeMarkerConfigIndex + 1;
		final Map25MarkerConfig newConfig = createDefaults_Markers_One(configIndex);

		// update model
		_activeMarkerConfig = newConfig;
		_allMarkerConfigs.add(newConfig);
	}

	public static void resetAllMarkerConfigurations() {

		createDefaults_Markers();

		_activeMarkerConfig = _allMarkerConfigs.get(0);
	}

	public static void saveState() {

		if (_activeTrackConfig == null) {

			// this can happen when not yet used

			return;
		}

		final XMLMemento xmlRoot = create_Root();

		saveState_Tracks(xmlRoot);
		saveState_Markers(xmlRoot);

		Util.writeXml(xmlRoot, getConfigXmlFile());
	}

	/**
	 * Markers
	 */
	private static void saveState_Markers(final XMLMemento xmlRoot) {

		final IMemento xmlMarkers = xmlRoot.createChild(TAG_TOUR_MARKERS);
		{
			xmlMarkers.putString(ATTR_ACTIVE_CONFIG_ID, _activeMarkerConfig.id);

			for (final Map25MarkerConfig config : _allMarkerConfigs) {
				createXml_FromMarkerConfig(config, xmlMarkers);
			}
		}
	}

	/**
	 * Tracks
	 */
	private static void saveState_Tracks(final XMLMemento xmlRoot) {

		final IMemento xmlTourTracks = xmlRoot.createChild(TAG_TOUR_TRACKS);
		{
			xmlTourTracks.putString(ATTR_ACTIVE_CONFIG_ID, _activeTrackConfig.id);

			for (final Map25TrackConfig config : _allTrackConfigs) {
				createXml_FromTrackConfig(config, xmlTourTracks);
			}
		}
	}

	public static void setActiveMarkerConfig(final Map25MarkerConfig newConfig) {

		_activeMarkerConfig = newConfig;
	}

	public static void setActiveTrackConfig(final Map25TrackConfig newConfig) {

		_activeTrackConfig = newConfig;
	}
}
