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

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.common.widgets.ComboEntry;
import net.tourbook.map25.layer.marker.ClusterAlgorithm;
import net.tourbook.map25.layer.marker.MarkerConfig;
import net.tourbook.map25.layer.tourtrack.Map25TrackConfig;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

public class Map25ConfigManager {

// SET_FORMATTING_OFF

	public static final int						SYMBOL_ORIENTATION_BILLBOARD	= 0;
	public static final int						SYMBOL_ORIENTATION_GROUND		= 1;
	
	public static final ComboEntry[]			SYMBOL_ORIENTATION				= {
			
		new ComboEntry(Messages.Map25_Config_SymbolOrientation_Billboard,	SYMBOL_ORIENTATION_BILLBOARD),
		new ComboEntry(Messages.Map25_Config_SymbolOrientation_Ground,		SYMBOL_ORIENTATION_GROUND),
	};

	public static final ClusterAlgorithmItem[]	ALL_CLUSTER_ALGORITHM			= {
			
		new ClusterAlgorithmItem(Messages.Map25_Config_ClusterAlgorithm_FirstMarker,	ClusterAlgorithm.FirstMarker),
		new ClusterAlgorithmItem(Messages.Map25_Config_ClusterAlgorithm_Distance,		ClusterAlgorithm.Distance),
		new ClusterAlgorithmItem(Messages.Map25_Config_ClusterAlgorithm_Grid,			ClusterAlgorithm.Grid),
	};

// SET_FORMATTING_ON

	public static final String					CONFIG_DEFAULT_ID_1				= "#1";									//$NON-NLS-1$
	static final String							CONFIG_DEFAULT_ID_2				= "#2";									//$NON-NLS-1$
	static final String							CONFIG_DEFAULT_ID_3				= "#3";									//$NON-NLS-1$
	static final String							CONFIG_DEFAULT_ID_4				= "#4";									//$NON-NLS-1$
	static final String							CONFIG_DEFAULT_ID_5				= "#5";									//$NON-NLS-1$
	static final String							CONFIG_DEFAULT_ID_6				= "#6";									//$NON-NLS-1$
	static final String							CONFIG_DEFAULT_ID_7				= "#7";									//$NON-NLS-1$
	static final String							CONFIG_DEFAULT_ID_8				= "#8";									//$NON-NLS-1$
	static final String							CONFIG_DEFAULT_ID_9				= "#9";									//$NON-NLS-1$
	static final String							CONFIG_DEFAULT_ID_10			= "#10";								//$NON-NLS-1$

	private static final Bundle					_bundle							= TourbookPlugin
			.getDefault()
			.getBundle();
	private static final IPath					_stateLocation					= Platform.getStateLocation(_bundle);
	private static final String					CONFIG_FILE_NAME				= "map25-config.xml";					//$NON-NLS-1$
	//
	/**
	 * Version number is not yet used.
	 */
	private static final int					CONFIG_VERSION					= 1;
	//
	// common attributes
	private static final String					ATTR_ACTIVE_CONFIG_ID			= "activeConfigId";						//$NON-NLS-1$
	private static final String					ATTR_ID							= "id";									//$NON-NLS-1$
	private static final String					ATTR_CONFIG_NAME				= "name";								//$NON-NLS-1$
	//
	/*
	 * Root
	 */
	private static final String					TAG_ROOT						= "Map25Configuration";					//$NON-NLS-1$
	private static final String					ATTR_CONFIG_VERSION				= "configVersion";						//$NON-NLS-1$
	//
	/*
	 * Tour tracks
	 */
	private static final String					TAG_TOUR_TRACKS					= "TourTracks";							//$NON-NLS-1$
	private static final String					TAG_TRACK						= "Track";								//$NON-NLS-1$
	private static final String					ATTR_ANIMATION_TIME				= "animationTime";						//$NON-NLS-1$
	//
	// outline
	private static final String					TAG_OUTLINE						= "Outline";							//$NON-NLS-1$
	private static final String					ATTR_OUTLINE_WIDTH				= "width";								//$NON-NLS-1$
	//
	public static final float					OUTLINE_WIDTH_MIN				= 0.1f;
	public static final float					OUTLINE_WIDTH_MAX				= 10.0f;
	public static final float					DEFAULT_OUTLINE_WIDTH			= 2.5f;
	public static final RGB						DEFAULT_OUTLINE_COLOR			= new RGB(0x80, 0x0, 0x80);
	//
	// other properties
	public static final int						DEFAULT_ANIMATION_TIME			= 2000;
	//
	/*
	 * Tour Markers
	 */
	private static final String					TAG_TOUR_MARKERS				= "TourMarkers";						//$NON-NLS-1$
	private static final String					TAG_MARKER						= "Marker";								//$NON-NLS-1$
	//
	// marker
	private static final String					TAG_MARKER_FILL_COLOR			= "MarkerFillColor";					//$NON-NLS-1$
	private static final String					TAG_MARKER_OUTLINE_COLOR		= "MarkerOutlineColor";					//$NON-NLS-1$
	private static final String					ATTR_IS_SHOW_MARKER_LABEL		= "isShowMarkerLabel";					//$NON-NLS-1$
	private static final String					ATTR_IS_SHOW_MARKER_POINT		= "isShowMarkerPoint";					//$NON-NLS-1$
	private static final String					ATTR_MARKER_ORIENTATION			= "markerOrientation";					//$NON-NLS-1$
	private static final String					ATTR_MARKER_SYMBOL_SIZE			= "markerSymbolSize";					//$NON-NLS-1$
	//
	// cluster
	private static final String					TAG_CLUSTER_FILL_COLOR			= "ClusterFillColor";					//$NON-NLS-1$
	private static final String					TAG_CLUSTER_OUTLINE_COLOR		= "ClusterOutlineColor";				//$NON-NLS-1$
	private static final String					ATTR_CLUSTER_ALGORITHM			= "clusterAlgorithm";					//$NON-NLS-1$
	private static final String					ATTR_CLUSTER_GRID_SIZE			= "clusterGridSize";					//$NON-NLS-1$
	private static final String					ATTR_CLUSTER_ORIENTATION		= "clusterOrientation";					//$NON-NLS-1$
	private static final String					ATTR_CLUSTER_SYMBOL_SIZE		= "clusterSymbolSize";					//$NON-NLS-1$
	private static final String					ATTR_IS_MARKER_CLUSTERED		= "isMarkerClustered";					//$NON-NLS-1$
	//
	// symbol
	public static final int						DEFAULT_MARKER_SYMBOL_SIZE		= 20;
	public static final int						MARKER_SYMBOL_SIZE_MIN			= 10;
	public static final int						MARKER_SYMBOL_SIZE_MAX			= 200;
	//
	// CLUSTER
	public static final int						DEFAULT_CLUSTER_GRID_SIZE		= 60;
	public static final int						DEFAULT_CLUSTER_SYMBOL_SIZE		= 40;
	public static final int						CLUSTER_GRID_MIN_SIZE			= 1;
	public static final int						CLUSTER_GRID_MAX_SIZE			= 10000;
	public static final int						CLUSTER_SYMBOL_SIZE_MIN			= 20;
	public static final int						CLUSTER_SYMBOL_SIZE_MAX			= 200;
	//
	// colors
	public static final int						DEFAULT_CLUSTER_OPACITY			= 0xe0;
	public static final RGB						DEFAULT_CLUSTER_OUTLINE_COLOR	= new RGB(0xff, 0xff, 0xff);
	public static final RGB						DEFAULT_CLUSTER_FILL_COLOR		= new RGB(0xFC, 0x67, 0x00);
	public static final int						DEFAULT_MARKER_OPACITY			= 0xe0;
	public static final RGB						DEFAULT_MARKER_OUTLINE_COLOR	= new RGB(0, 0, 0);
	public static final RGB						DEFAULT_MARKER_FILL_COLOR		= new RGB(0xFF, 0xFF, 0x00);
	//
	// !!! this is a code formatting separator !!!
	static {}
	//
	/**
	 * Contains all configurations which are loaded from a xml file.
	 */
	private static final ArrayList<Map25TrackConfig>	_allTrackConfigs	= new ArrayList<>();
	private static Map25TrackConfig						_activeTrackConfig;
	private static final ArrayList<MarkerConfig>		_allMarkerConfigs	= new ArrayList<>();
	private static MarkerConfig							_activeMarkerConfig;
	//
	private static String								_fromXml_ActiveMarkerConfigId;
	private static String								_fromXml_ActiveTrackConfigId;

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
		for (int configIndex = 1; configIndex < 11; configIndex++) {
			_allMarkerConfigs.add(createDefaults_Markers_One(configIndex));
		}
	}

	/**
	 * @param configIndex
	 *            Index starts with 1.
	 * @return
	 */
	private static MarkerConfig createDefaults_Markers_One(final int configIndex) {

		final MarkerConfig config = new MarkerConfig();

		final RGB fgBlack = new RGB(0, 0, 0);
		final RGB fgWhite = new RGB(0xff, 0xff, 0xff);

		final RGB bg1 = new RGB(0x00, 0xA0, 0xED);
		final RGB bg2 = new RGB(0xC6, 0x00, 0xA2);
		final RGB bg3 = new RGB(0x00, 0xC4, 0x2C);
		final RGB bg4 = new RGB(0xFF, 0xC9, 0x00);
		final RGB bg5 = new RGB(0xFF, 0x00, 0x62);

		config.markerOutline_Color = fgBlack;
		config.markerFill_Color = fgWhite;

		switch (configIndex) {

		case 1:
			config.name = config.defaultId = CONFIG_DEFAULT_ID_1;
			config.clusterOutline_Color = fgBlack;
			config.clusterFill_Color = bg1;
			config.markerOutline_Color = fgBlack;
			config.markerFill_Color = bg5;
			break;
		case 2:
			config.name = config.defaultId = CONFIG_DEFAULT_ID_2;
			config.clusterOutline_Color = fgWhite;
			config.clusterFill_Color = bg1;
			break;

		case 3:
			config.name = config.defaultId = CONFIG_DEFAULT_ID_3;
			config.clusterOutline_Color = fgBlack;
			config.clusterFill_Color = bg2;
			break;
		case 4:
			config.name = config.defaultId = CONFIG_DEFAULT_ID_4;
			config.clusterOutline_Color = fgWhite;
			config.clusterFill_Color = bg2;
			break;

		case 5:
			config.name = config.defaultId = CONFIG_DEFAULT_ID_5;
			config.clusterOutline_Color = fgBlack;
			config.clusterFill_Color = bg3;
			break;
		case 6:
			config.name = config.defaultId = CONFIG_DEFAULT_ID_6;
			config.clusterOutline_Color = fgWhite;
			config.clusterFill_Color = bg3;
			break;

		case 7:
			config.name = config.defaultId = CONFIG_DEFAULT_ID_7;
			config.clusterOutline_Color = fgBlack;
			config.clusterFill_Color = bg4;
			break;
		case 8:
			config.name = config.defaultId = CONFIG_DEFAULT_ID_8;
			config.clusterOutline_Color = fgWhite;
			config.clusterFill_Color = bg4;
			break;

		case 9:
			config.name = config.defaultId = CONFIG_DEFAULT_ID_9;
			config.clusterOutline_Color = fgBlack;
			config.clusterFill_Color = bg5;
			break;
		case 10:
			config.name = config.defaultId = CONFIG_DEFAULT_ID_10;
			config.clusterOutline_Color = fgWhite;
			config.clusterFill_Color = bg5;
			break;

		}

		return config;
	}

	private static void createDefaults_Tracks() {

		_allTrackConfigs.clear();

		// append custom configurations
		for (int customIndex = 1; customIndex < 11; customIndex++) {
			_allTrackConfigs.add(createDefaults_Tracks_One(customIndex));
		}
	}

	/**
	 * @param configIndex
	 *            Index starts with 1.
	 * @return
	 */
	private static Map25TrackConfig createDefaults_Tracks_One(final int configIndex) {

		final Map25TrackConfig config = new Map25TrackConfig();

		switch (configIndex) {

		case 1:
			config.name = config.defaultId = CONFIG_DEFAULT_ID_1;
			config.outlineWidth = DEFAULT_OUTLINE_WIDTH;
			break;

		case 2:
			config.name = config.defaultId = CONFIG_DEFAULT_ID_2;
			config.outlineWidth = 1;
			break;

		case 3:
			config.name = config.defaultId = CONFIG_DEFAULT_ID_3;
			config.outlineWidth = 3;
			break;

		case 4:
			config.name = config.defaultId = CONFIG_DEFAULT_ID_4;
			config.outlineWidth = 4;
			break;

		case 5:
			config.name = config.defaultId = CONFIG_DEFAULT_ID_5;
			config.outlineWidth = 5;
			break;

		case 6:
			config.name = config.defaultId = CONFIG_DEFAULT_ID_6;
			config.outlineWidth = 6;
			break;

		case 7:
			config.name = config.defaultId = CONFIG_DEFAULT_ID_7;
			config.outlineWidth = 7;
			break;

		case 8:
			config.name = config.defaultId = CONFIG_DEFAULT_ID_8;
			config.outlineWidth = 8;
			break;

		case 9:
			config.name = config.defaultId = CONFIG_DEFAULT_ID_9;
			config.outlineWidth = 9;
			break;

		case 10:
			config.name = config.defaultId = CONFIG_DEFAULT_ID_10;
			config.outlineWidth = 10;
			break;

		}

		return config;
	}

	private static void createXml_FromMarkerConfig(final MarkerConfig config, final IMemento xmlMarkers) {

		// <Marker>
		final IMemento xmlConfig = xmlMarkers.createChild(TAG_MARKER);
		{
			xmlConfig.putString(ATTR_ID, config.id);
			xmlConfig.putString(ATTR_CONFIG_NAME, config.name);

			// Marker
			xmlConfig.putBoolean(ATTR_IS_SHOW_MARKER_LABEL, config.isShowMarkerLabel);
			xmlConfig.putBoolean(ATTR_IS_SHOW_MARKER_POINT, config.isShowMarkerPoint);
			xmlConfig.putInteger(ATTR_MARKER_SYMBOL_SIZE, config.markerSymbolSize);
			xmlConfig.putInteger(ATTR_MARKER_ORIENTATION, config.markerOrientation);

			Util.setXmlRgb(xmlConfig, TAG_MARKER_OUTLINE_COLOR, config.markerOutline_Color);
			Util.setXmlRgb(xmlConfig, TAG_MARKER_FILL_COLOR, config.markerFill_Color);

			// Cluster
			xmlConfig.putInteger(ATTR_CLUSTER_GRID_SIZE, config.clusterGridSize);
			xmlConfig.putInteger(ATTR_CLUSTER_SYMBOL_SIZE, config.clusterSymbolSize);
			xmlConfig.putInteger(ATTR_CLUSTER_ORIENTATION, config.clusterOrientation);
			xmlConfig.putBoolean(ATTR_IS_MARKER_CLUSTERED, config.isMarkerClustered);

			Util.setXmlEnum(xmlConfig, ATTR_CLUSTER_ALGORITHM, config.clusterAlgorithm);

			Util.setXmlRgb(xmlConfig, TAG_CLUSTER_OUTLINE_COLOR, config.clusterOutline_Color);
			Util.setXmlRgb(xmlConfig, TAG_CLUSTER_FILL_COLOR, config.clusterFill_Color);
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

	public static MarkerConfig getActiveMarkerConfig() {

		if (_activeMarkerConfig == null) {
			readConfigFromXml();
		}

		return _activeMarkerConfig;
	}

	/**
	 * @return Returns the index for the {@link #_activeMarkerConfig}, the index starts with 0.
	 */
	public static int getActiveMarkerConfigIndex() {

		final MarkerConfig activeConfig = getActiveMarkerConfig();

		for (int configIndex = 0; configIndex < _allMarkerConfigs.size(); configIndex++) {

			final MarkerConfig config = _allMarkerConfigs.get(configIndex);

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

	public static ArrayList<MarkerConfig> getAllMarkerConfigs() {

		// ensure configs are loaded
		getActiveMarkerConfig();

		return _allMarkerConfigs;
	}

	public static ArrayList<Map25TrackConfig> getAllTourTrackConfigs() {

		// ensure configs are loaded
		getActiveTourTrackConfig();

		return _allTrackConfigs;
	}

	private static MarkerConfig getConfig_Marker() {

		MarkerConfig activeConfig = null;

		if (_fromXml_ActiveMarkerConfigId != null) {

			// ensure config id belongs to a config which is available

			for (final MarkerConfig config : _allMarkerConfigs) {

				if (config.id.equals(_fromXml_ActiveMarkerConfigId)) {

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

		if (_fromXml_ActiveTrackConfigId != null) {

			// ensure config id belongs to a config which is available

			for (final Map25TrackConfig config : _allTrackConfigs) {

				if (config.id.equals(_fromXml_ActiveTrackConfigId)) {

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

		_fromXml_ActiveTrackConfigId = Util.getXmlString(xmlTourTracks, ATTR_ACTIVE_CONFIG_ID, null);

		for (final IMemento mementoConfig : xmlTourTracks.getChildren()) {

			final XMLMemento xmlConfig = (XMLMemento) mementoConfig;

			try {

				final String xmlConfigType = xmlConfig.getType();

				if (xmlConfigType.equals(TAG_TRACK)) {

					// <Track>

					final Map25TrackConfig trackConfig = new Map25TrackConfig();

					parse_010_TrackConfig(xmlConfig, trackConfig);

					allTourTrackConfig.add(trackConfig);
				}

			} catch (final Exception e) {
				StatusUtil.log(Util.dumpMemento(xmlConfig), e);
			}
		}
	}

	private static void parse_010_TrackConfig(final XMLMemento xmlConfig, final Map25TrackConfig config) {

		config.id = Util.getXmlString(
				xmlConfig, //
				ATTR_ID,
				Long.toString(System.nanoTime()));

		config.name = Util.getXmlString(
				xmlConfig, //
				ATTR_CONFIG_NAME,
				UI.EMPTY_STRING);

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
											final ArrayList<MarkerConfig> allMarkerConfigs) {

		if (xmlRoot == null) {
			return;
		}

		final XMLMemento xmlMarkers = (XMLMemento) xmlRoot.getChild(TAG_TOUR_MARKERS);

		if (xmlMarkers == null) {
			return;
		}

		_fromXml_ActiveMarkerConfigId = Util.getXmlString(xmlMarkers, ATTR_ACTIVE_CONFIG_ID, null);

		for (final IMemento mementoConfig : xmlMarkers.getChildren()) {

			final XMLMemento xmlConfig = (XMLMemento) mementoConfig;

			try {

				final String xmlConfigType = xmlConfig.getType();

				if (xmlConfigType.equals(TAG_MARKER)) {

					// <Track>

					final MarkerConfig markerConfig = new MarkerConfig();

					parse_210_MarkerConfig(xmlConfig, markerConfig);

					allMarkerConfigs.add(markerConfig);
				}

			} catch (final Exception e) {
				StatusUtil.log(Util.dumpMemento(xmlConfig), e);
			}
		}
	}

	private static void parse_210_MarkerConfig(final XMLMemento xmlConfig, final MarkerConfig config) {

// SET_FORMATTING_OFF
		
		config.id		= Util.getXmlString(xmlConfig, ATTR_ID, Long.toString(System.nanoTime()));
		config.name		= Util.getXmlString(xmlConfig, ATTR_CONFIG_NAME, UI.EMPTY_STRING);

		config.clusterAlgorithm		= Util.getXmlEnum(xmlConfig,	ATTR_CLUSTER_ALGORITHM,		ClusterAlgorithm.FirstMarker);
		config.clusterGridSize		= Util.getXmlInteger(xmlConfig, ATTR_CLUSTER_GRID_SIZE,		DEFAULT_CLUSTER_GRID_SIZE, CLUSTER_GRID_MIN_SIZE, CLUSTER_GRID_MAX_SIZE);
		config.clusterOrientation	= Util.getXmlInteger(xmlConfig, ATTR_CLUSTER_ORIENTATION,	Map25ConfigManager.SYMBOL_ORIENTATION_BILLBOARD);
		config.clusterSymbolSize	= Util.getXmlInteger(xmlConfig, ATTR_CLUSTER_SYMBOL_SIZE,	DEFAULT_CLUSTER_SYMBOL_SIZE, CLUSTER_SYMBOL_SIZE_MIN, CLUSTER_SYMBOL_SIZE_MAX);
		config.isMarkerClustered	= Util.getXmlBoolean(xmlConfig, ATTR_IS_MARKER_CLUSTERED,	true);
		
		config.isShowMarkerLabel	= Util.getXmlBoolean(xmlConfig, ATTR_IS_SHOW_MARKER_LABEL,	true);
		config.isShowMarkerPoint	= Util.getXmlBoolean(xmlConfig, ATTR_IS_SHOW_MARKER_POINT,	true);
		config.markerOrientation	= Util.getXmlInteger(xmlConfig, ATTR_MARKER_ORIENTATION,	Map25ConfigManager.SYMBOL_ORIENTATION_BILLBOARD);
		config.markerSymbolSize		= Util.getXmlInteger(xmlConfig, ATTR_MARKER_SYMBOL_SIZE,	DEFAULT_MARKER_SYMBOL_SIZE, MARKER_SYMBOL_SIZE_MIN, MARKER_SYMBOL_SIZE_MAX);

// SET_FORMATTING_ON

		/*
		 * Each color has a seaparate tag
		 */
		for (final IMemento mementoConfigChild : xmlConfig.getChildren()) {

			final XMLMemento xmlConfigChild = (XMLMemento) mementoConfigChild;
			final String configTag = xmlConfigChild.getType();

			switch (configTag) {

			case TAG_CLUSTER_OUTLINE_COLOR:
				config.clusterOutline_Color = Util.getXmlRgb(xmlConfigChild, DEFAULT_CLUSTER_OUTLINE_COLOR);
				break;
			case TAG_CLUSTER_FILL_COLOR:
				config.clusterFill_Color = Util.getXmlRgb(xmlConfigChild, DEFAULT_CLUSTER_FILL_COLOR);
				break;

			case TAG_MARKER_OUTLINE_COLOR:
				config.markerOutline_Color = Util.getXmlRgb(xmlConfigChild, DEFAULT_MARKER_OUTLINE_COLOR);
				break;
			case TAG_MARKER_FILL_COLOR:
				config.markerFill_Color = Util.getXmlRgb(xmlConfigChild, DEFAULT_MARKER_FILL_COLOR);
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

		// do not replace the name
		final String oldName = _activeMarkerConfig.name;

		final int activeMarkerConfigIndex = getActiveMarkerConfigIndex();

		// remove old config
		_allMarkerConfigs.remove(_activeMarkerConfig);

		// create new config
		final int configIndex = activeMarkerConfigIndex + 1;
		final MarkerConfig newConfig = createDefaults_Markers_One(configIndex);
		newConfig.name = oldName;

		// update model
		_activeMarkerConfig = newConfig;
		_allMarkerConfigs.add(activeMarkerConfigIndex, newConfig);
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

			for (final MarkerConfig config : _allMarkerConfigs) {
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

	public static void setActiveMarkerConfig(final MarkerConfig newConfig) {

		_activeMarkerConfig = newConfig;
	}

	public static void setActiveTrackConfig(final Map25TrackConfig newConfig) {

		_activeTrackConfig = newConfig;
	}
}
