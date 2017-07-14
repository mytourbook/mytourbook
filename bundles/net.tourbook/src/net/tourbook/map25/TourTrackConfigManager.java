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
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.map25.layer.tourtrack.TourTrackConfig;
import net.tourbook.map3.Messages;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

public class TourTrackConfigManager {

// SET_FORMATTING_OFF
	private static final Bundle		_bundle							= TourbookPlugin.getDefault().getBundle();
	private static final IPath		_stateLocation					= Platform.getStateLocation(_bundle);
	
	private static final String		CONFIG_NAME_DEFAULT				= Messages.Track_Config_ConfigName_Default;
	public static final String		CONFIG_NAME_UNKNOWN				= Messages.Track_Config_ConfigName_Unknown;
// SET_FORMATTING_ON

	/*
	 * Default id is used to reset a configuration to default values.
	 */
	public static final String	DEFAULT_ID_DEFAULT		= "#default";								//$NON-NLS-1$

	// outline
	public static final float	OUTLINE_WIDTH_MIN		= 1.0f;
	public static final float	OUTLINE_WIDTH_MAX		= 10.0f;
	public static final float	OUTLINE_WIDTH_DEFAULT	= 2.5f;

	// colors
	public static final RGB		RGB_DEFAULT;

	static {

		RGB_DEFAULT = new RGB(0x80, 0x0, 0x80);
	}

	// track position
	public static final boolean						IS_SHOW_TRACK_POSITION_DEFAULT	= true;

	private static final String						CONFIG_FILE_NAME				= "map25-config.xml";				//$NON-NLS-1$

	/**
	 * This version number is incremented, when structural changes (e.g. new category) are done.
	 * When this happens, the <b>default</b> structure is created.
	 */
	private static final int						CONFIG_VERSION					= 1;

	// root
	private static final String						TAG_ROOT						= "Map25Configuration";				//$NON-NLS-1$
	private static final String						ATTR_ACTIVE_CONFIG_ID			= "activeConfigId";					//$NON-NLS-1$
	private static final String						ATTR_CONFIG_VERSION				= "configVersion";					//$NON-NLS-1$

	/*
	 * Tour tracks
	 */
	private static final String						TAG_TOUR_TRACKS					= "TourTracks";						//$NON-NLS-1$
	private static final String						TAG_TRACK						= "Track";							//$NON-NLS-1$
	private static final String						ATTR_ID							= "id";								//$NON-NLS-1$
	private static final String						ATTR_DEFAULT_ID					= "defaultId";						//$NON-NLS-1$
	private static final String						ATTR_CONFIG_NAME				= "name";							//$NON-NLS-1$

	// outline
	private static final String						TAG_OUTLINE						= "Outline";						//$NON-NLS-1$
	private static final String						ATTR_OUTLINE_WIDTH				= "width";							//$NON-NLS-1$
	//

	/**
	 * Contains all configurations which are loaded from a xml file.
	 */
	private static final ArrayList<TourTrackConfig>	_allTourTrackConfigs			= new ArrayList<TourTrackConfig>();

	private static TourTrackConfig					_activeConfig;
	private static String							_activeConfigIdFromXml;

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

	private static void createAllDefaults() {

		_allTourTrackConfigs.clear();

		// create default config
		final XMLMemento xmlRoot = createDefaultXml_0_All();

		// parse default config
		parse_000_TourTracks(_allTourTrackConfigs, xmlRoot, true);
	}

	private static XMLMemento createDefaultXml_0_All() {

		XMLMemento xmlRoot;

		try {

			xmlRoot = create_Root();

			final IMemento xmlTourTracks = xmlRoot.createChild(TAG_TOUR_TRACKS);
			{
				createDefaultXml_10_TourTracks(
						xmlTourTracks,
						CONFIG_NAME_DEFAULT,
						DEFAULT_ID_DEFAULT,
						OUTLINE_WIDTH_DEFAULT);

				// append custom configurations
				for (int customIndex = 0; customIndex < 10; customIndex++) {

					createDefaultXml_10_TourTracks(//
							xmlTourTracks,
							String.format("%s #%d", CONFIG_NAME_DEFAULT, (customIndex + 1)), //$NON-NLS-1$
							DEFAULT_ID_DEFAULT,
							customIndex + 1);
				}
			}

		} catch (final Exception e) {
			throw new Error(e.getMessage());
		}

		return xmlRoot;
	}

	private static void createDefaultXml_10_TourTracks(	final IMemento xmlTourTracks,
														final String configName,
														final String defaultId,
														final float outlineWidth) {

		// <Track>
		final IMemento xmlConfig = xmlTourTracks.createChild(TAG_TRACK);
		{
			xmlConfig.putString(ATTR_ID, Long.toString(System.nanoTime()));
			xmlConfig.putString(ATTR_DEFAULT_ID, defaultId);
			xmlConfig.putString(ATTR_CONFIG_NAME, configName);

			// <outline>
			final IMemento xmlOutline = xmlConfig.createChild(TAG_OUTLINE);
			{
				xmlOutline.putFloat(ATTR_OUTLINE_WIDTH, outlineWidth);
				Util.setXmlRgb(xmlOutline, RGB_DEFAULT);
			}
		}
	}

	private static void createXml_FromConfig(final IMemento xmlTourTracks, final TourTrackConfig config) {

		// <Track>
		final IMemento xmlConfig = xmlTourTracks.createChild(TAG_TRACK);
		{
			xmlConfig.putString(ATTR_ID, config.id);
			xmlConfig.putString(ATTR_DEFAULT_ID, config.defaultId);
			xmlConfig.putString(ATTR_CONFIG_NAME, config.name);

			// <outline>
			final IMemento xmlOutline = xmlConfig.createChild(TAG_OUTLINE);
			{
				xmlOutline.putFloat(ATTR_OUTLINE_WIDTH, config.outlineWidth);
				Util.setXmlRgb(xmlOutline, config.outlineColor);
			}
		}
	}

	public static TourTrackConfig getActiveConfig() {

		if (_activeConfig == null) {
			readConfigFromXml();
		}

		return _activeConfig;
	}

	/**
	 * @return Returns the index of the active config within all configs.
	 */
	public static int getActiveConfigIndex() {

		final TourTrackConfig activeConfig = getActiveConfig();

		for (int configIndex = 0; configIndex < _allTourTrackConfigs.size(); configIndex++) {

			final TourTrackConfig config = _allTourTrackConfigs.get(configIndex);

			if (config == activeConfig) {
				return configIndex;
			}
		}

		// this case should not happen but ensure that the correct config is set

		_activeConfig = _allTourTrackConfigs.get(0);

		return 0;
	}

	private static int getActiveConfigIndexFromId() {

		for (int configIndex = 0; configIndex < _allTourTrackConfigs.size(); configIndex++) {

			final TourTrackConfig config = _allTourTrackConfigs.get(configIndex);

			if (config.id.equals(_activeConfig.id)) {
				return configIndex;
			}
		}

		return 0;
	}

	public static ArrayList<TourTrackConfig> getAllConfigurations() {

		// ensure configs are loaded
		getActiveConfig();

		return _allTourTrackConfigs;
	}

	private static File getConfigXmlFile() {

		final File layerFile = _stateLocation.append(CONFIG_FILE_NAME).toFile();

		return layerFile;
	}

	private static TourTrackConfig getFirstDefaultConfig() {

		for (final TourTrackConfig config : _allTourTrackConfigs) {

			if (config.defaultId.equals(DEFAULT_ID_DEFAULT)) {
				return config;
			}
		}

		return null;
	}

	/**
	 * Overwrite default values according to the default id {@link TourTrackConfig#defaultId}.
	 * 
	 * @param config
	 */
	private static void overwriteConfig_DefaultValues(final TourTrackConfig config) {

		if (config.defaultId.equals(DEFAULT_ID_DEFAULT)) {

			// default id contains only default values -> nothing to do

//		} else if (config.defaultId.equals(DEFAULT_ID_RELATIVE_BRIGHT)) {
//
//			overwriteConfigValues_Bright(config);
//
//			config.altitudeOffsetMode = TourTrackConfigManager.ALTITUDE_OFFSET_MODE_RELATIVE;
		}
	}

	private static void overwriteConfig_DefaultValues_ForAll() {

		for (final TourTrackConfig config : _allTourTrackConfigs) {
			overwriteConfig_DefaultValues(config);
		}
	}

	/**
	 * Parse configuration xml.
	 * 
	 * @param allConfigurations
	 * @param xmlRoot
	 * @param parseActiveConfig
	 *            When <code>true</code> the value {@link #_activeConfigIdFromXml} is set from the
	 *            root item.
	 */
	private static void parse_000_TourTracks(	final ArrayList<TourTrackConfig> allConfigurations,
												final XMLMemento xmlRoot,
												final boolean parseActiveConfig) {

		final XMLMemento xmlTourTracks = (XMLMemento) xmlRoot.getChild(TAG_TOUR_TRACKS);

		if (xmlTourTracks == null) {
			return;
		}

		if (parseActiveConfig) {
			parse_010_ActiveConfig(xmlTourTracks);
		}

		for (final IMemento mementoConfig : xmlTourTracks.getChildren()) {

			final XMLMemento xmlConfig = (XMLMemento) mementoConfig;

			try {

				final String xmlConfigType = xmlConfig.getType();

				if (xmlConfigType.equals(TAG_TRACK)) {

					// <trackConfig>

					final TourTrackConfig trackConfig = new TourTrackConfig();

					parse_100_ConfigAttr(xmlConfig, trackConfig);

					for (final IMemento mementoConfigChild : xmlConfig.getChildren()) {

						final XMLMemento xmlConfigChild = (XMLMemento) mementoConfigChild;
						final String configTag = xmlConfigChild.getType();

						if (configTag.equals(TAG_OUTLINE)) {

							parse_300_Outline(xmlConfigChild, trackConfig);

//						} else if (configTag.equals(TAG_ALTITUDE)) {
//
//							parse_600_Altitude(xmlConfigChild, trackConfig);
						}
					}

					allConfigurations.add(trackConfig);
				}

			} catch (final Exception e) {
				StatusUtil.log(Util.dumpMemento(xmlConfig), e);
			}
		}
	}

	private static void parse_010_ActiveConfig(final XMLMemento xmlRoot) {

		_activeConfigIdFromXml = Util.getXmlString(//
				xmlRoot,
				ATTR_ACTIVE_CONFIG_ID,
				null);
	}

	private static void parse_100_ConfigAttr(final XMLMemento xmlConfig, final TourTrackConfig config) {

		config.id = Util.getXmlString(
				xmlConfig, //
				ATTR_ID,
				Long.toString(System.nanoTime()));

		config.defaultId = Util.getXmlString(
				xmlConfig, //
				ATTR_DEFAULT_ID,
				DEFAULT_ID_DEFAULT);

		config.name = Util.getXmlString(
				xmlConfig, //
				ATTR_CONFIG_NAME,
				CONFIG_NAME_UNKNOWN);

	}

	private static void parse_300_Outline(final XMLMemento xmlOutline, final TourTrackConfig config) {

		config.outlineWidth = Util.getXmlFloatFloat(
				xmlOutline,
				ATTR_OUTLINE_WIDTH,
				OUTLINE_WIDTH_DEFAULT,
				OUTLINE_WIDTH_MIN,
				OUTLINE_WIDTH_MAX);

		config.outlineColor = Util.getXmlRgb(xmlOutline, RGB_DEFAULT);
	}

	/**
	 * Read/create tour track configuration a xml file
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

			Integer configVersion = null;

			// get current layer version, when available
			if (xmlRoot != null) {
				configVersion = xmlRoot.getInteger(ATTR_CONFIG_VERSION);
			}

			boolean isSetConfigDefaultValues = false;

			if (xmlRoot == null || configVersion == null || configVersion < CONFIG_VERSION) {

				// create default config
				xmlRoot = createDefaultXml_0_All();
				isSetConfigDefaultValues = true;
			}

			// parse xml
			parse_000_TourTracks(_allTourTrackConfigs, xmlRoot, true);

			// ensure a config is created
			if (_allTourTrackConfigs.size() == 0) {
				createAllDefaults();
				isSetConfigDefaultValues = true;
			}

			if (isSetConfigDefaultValues) {

				// overwrite config default values

				overwriteConfig_DefaultValues_ForAll();
			}

			_activeConfig = readConfigFromXml_GetActive();

		} catch (final Exception e) {
			StatusUtil.log(e);
		} finally {

			if (reader != null) {
				try {
					reader.close();
				} catch (final IOException e) {
					StatusUtil.log(e);
				}
			}
		}
	}

	private static TourTrackConfig readConfigFromXml_GetActive() {

		TourTrackConfig activeConfig = null;

		if (_activeConfigIdFromXml == null) {

			// get first default config

			activeConfig = getFirstDefaultConfig();

		} else {

			// ensure config id belongs to a config which is available

			for (final TourTrackConfig config : _allTourTrackConfigs) {

				if (config.id.equals(_activeConfigIdFromXml)) {

					activeConfig = config;
					break;
				}
			}
		}

		if (activeConfig == null) {

			// this case should not happen, create a clean default config

			StatusUtil.log("Created default config for tour track properties.");//$NON-NLS-1$

			createAllDefaults();
			overwriteConfig_DefaultValues_ForAll();

			activeConfig = getFirstDefaultConfig();
		}

		return activeConfig;
	}

	/**
	 * Set default values for the active config.
	 */
	public static void resetActiveConfig() {

		final String backupConfigName = _activeConfig.name;

		// create xml with default values for the active config
		final XMLMemento xmlRoot = resetConfig(_activeConfig.defaultId);

		// parse xml
		final ArrayList<TourTrackConfig> newConfigs = new ArrayList<TourTrackConfig>();
		parse_000_TourTracks(newConfigs, xmlRoot, false);

		final TourTrackConfig newConfig = newConfigs.get(0);

		overwriteConfig_DefaultValues(newConfig);

		newConfig.name = backupConfigName;
//		newConfig.checkTrackRecreation(_activeConfig);

		// replace config
		_allTourTrackConfigs.set(getActiveConfigIndexFromId(), newConfig);

		_activeConfig = newConfig;
	}

	public static void resetAllConfigurations() {

		createAllDefaults();
		overwriteConfig_DefaultValues_ForAll();

		_activeConfig = _allTourTrackConfigs.get(0);
	}

	private static XMLMemento resetConfig(final String defaultId) throws Error {

		XMLMemento xmlRoot;

		try {

			xmlRoot = create_Root();

			// default default
			createDefaultXml_10_TourTracks(xmlRoot, CONFIG_NAME_DEFAULT, DEFAULT_ID_DEFAULT, OUTLINE_WIDTH_DEFAULT);

		} catch (final Exception e) {
			throw new Error(e.getMessage());
		}
		return xmlRoot;
	}

	public static void saveState() {

		if (_activeConfig == null) {

			// this can happen when not yet used

			return;
		}

		final XMLMemento xmlRoot = create_Root();

		final IMemento xmlTourTracks = xmlRoot.createChild(TAG_TOUR_TRACKS);
		{
			xmlTourTracks.putString(ATTR_ACTIVE_CONFIG_ID, _activeConfig.id);

			for (final TourTrackConfig config : _allTourTrackConfigs) {
				createXml_FromConfig(xmlTourTracks, config);
			}
		}

		Util.writeXml(xmlRoot, getConfigXmlFile());
	}

	public static void setActiveConfig(final TourTrackConfig newConfig) {
		_activeConfig = newConfig;
	}
}
