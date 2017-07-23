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

// SET_FORMATTING_OFF
// SET_FORMATTING_ON

	private static final Bundle	_bundle						= TourbookPlugin.getDefault().getBundle();
	private static final IPath	_stateLocation				= Platform.getStateLocation(_bundle);

	private static final String	CONFIG_NAME_DEFAULT			= Messages.Track_Config_ConfigName_Default;
	public static final String	CONFIG_NAME_UNKNOWN			= Messages.Track_Config_ConfigName_Unknown;

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

		_allMarkerConfigs.add(createDefaults_Markers_One(CONFIG_NAME_DEFAULT));

		// append custom configurations
		for (int customIndex = 0; customIndex < 10; customIndex++) {

			final Map25MarkerConfig markerConfig = createDefaults_Markers_One(//
					String.format("%s #%d", CONFIG_NAME_DEFAULT, (customIndex + 1)));

			/*
			 * Individualize custom configurations
			 */
			switch (customIndex) {
			case 0:
				markerConfig.clusterColorBackground = new RGB(0x03, 0xAD, 0xFF);
				break;

			case 1:
				markerConfig.clusterColorBackground = new RGB(0xC6, 0x00, 0xA2);
				break;

			case 2:
				markerConfig.clusterColorBackground = new RGB(0x00, 0xBC, 0xC4);
				break;

//			case :
//				markerConfig.clusterColorBackground = new RGB();
//				break;
//
			default:
				break;
			}
			_allMarkerConfigs.add(markerConfig);
		}
	}

	private static Map25MarkerConfig createDefaults_Markers_One(final String name) {

		final Map25MarkerConfig markerConfig = new Map25MarkerConfig();

		markerConfig.name = name;

		return markerConfig;
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

					parse_010_ConfigAttr(xmlConfig, trackConfig);

					for (final IMemento mementoConfigChild : xmlConfig.getChildren()) {

						final XMLMemento xmlConfigChild = (XMLMemento) mementoConfigChild;
						final String configTag = xmlConfigChild.getType();

						if (configTag.equals(TAG_OUTLINE)) {

							// <Outline>

							parse_030_Outline(xmlConfigChild, trackConfig);

//						} else if (configTag.equals(TAG_ALTITUDE)) {
//
//							parse_600_Altitude(xmlConfigChild, trackConfig);
						}
					}

					allTourTrackConfig.add(trackConfig);
				}

			} catch (final Exception e) {
				StatusUtil.log(Util.dumpMemento(xmlConfig), e);
			}
		}
	}

	private static void parse_010_ConfigAttr(final XMLMemento xmlConfig, final Map25TrackConfig config) {

		config.id = Util.getXmlString(
				xmlConfig, //
				ATTR_ID,
				Long.toString(System.nanoTime()));

		config.name = Util.getXmlString(
				xmlConfig, //
				ATTR_CONFIG_NAME,
				CONFIG_NAME_UNKNOWN);

	}

	private static void parse_030_Outline(final XMLMemento xmlOutline, final Map25TrackConfig config) {

		config.outlineWidth = Util.getXmlFloatFloat(
				xmlOutline,
				ATTR_OUTLINE_WIDTH,
				DEFAULT_OUTLINE_WIDTH,
				OUTLINE_WIDTH_MIN,
				OUTLINE_WIDTH_MAX);

		config.outlineColor = Util.getXmlRgb(xmlOutline, DEFAULT_OUTLINE_COLOR);

		config.animationTime = Util.getXmlInteger(xmlOutline, ATTR_ANIMATION_TIME, DEFAULT_ANIMATION_TIME);
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

					parse_210_ConfigAttr(xmlConfig, markerConfig);

					for (final IMemento mementoConfigChild : xmlConfig.getChildren()) {

//						final XMLMemento xmlConfigChild = (XMLMemento) mementoConfigChild;
//						final String configTag = xmlConfigChild.getType();
//
//						if (configTag.equals(TAG_OUTLINE)) {
//
//							// <Outline>
//
//							parse_030_Outline(xmlConfigChild, markerConfig);
//
////						} else if (configTag.equals(TAG_ALTITUDE)) {
////
////							parse_600_Altitude(xmlConfigChild, trackConfig);
//						}
					}

					allMarkerConfigs.add(markerConfig);
				}

			} catch (final Exception e) {
				StatusUtil.log(Util.dumpMemento(xmlConfig), e);
			}
		}
	}

	private static void parse_210_ConfigAttr(final XMLMemento xmlConfig, final Map25MarkerConfig config) {

		config.id = Util.getXmlString(
				xmlConfig, //
				ATTR_ID,
				Long.toString(System.nanoTime()));

		config.name = Util.getXmlString(
				xmlConfig, //
				ATTR_CONFIG_NAME,
				CONFIG_NAME_UNKNOWN);
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

	public static void saveState() {

		if (_activeTrackConfig == null) {

			// this can happen when not yet used

			return;
		}

		final XMLMemento xmlRoot = create_Root();

		/*
		 * Tracks
		 */
		final IMemento xmlTourTracks = xmlRoot.createChild(TAG_TOUR_TRACKS);
		{
			xmlTourTracks.putString(ATTR_ACTIVE_CONFIG_ID, _activeTrackConfig.id);

			for (final Map25TrackConfig config : _allTrackConfigs) {
				createXml_FromTrackConfig(config, xmlTourTracks);
			}
		}

		/*
		 * Markers
		 */
		final IMemento xmlMarkers = xmlRoot.createChild(TAG_TOUR_MARKERS);
		{
			xmlMarkers.putString(ATTR_ACTIVE_CONFIG_ID, _activeTrackConfig.id);

			for (final Map25MarkerConfig config : _allMarkerConfigs) {
				createXml_FromMarkerConfig(config, xmlMarkers);
			}
		}

		Util.writeXml(xmlRoot, getConfigXmlFile());
	}

	public static void setActiveMarkerConfig(final Map25MarkerConfig newConfig) {

		_activeMarkerConfig = newConfig;
	}

	public static void setActiveTrackConfig(final Map25TrackConfig newConfig) {

		_activeTrackConfig = newConfig;
	}
}
