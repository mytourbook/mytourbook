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
package net.tourbook.ui.views.calendar;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

public class CalendarConfigManager {

	private static final String	CONFIG_FILE_NAME		= "calendar-config.xml";					//$NON-NLS-1$
	//
	/**
	 * Version number is not yet used.
	 */
	private static final int	CONFIG_VERSION			= 1;

	private static final Bundle	_bundle					= TourbookPlugin.getDefault().getBundle();
	private static final IPath	_stateLocation			= Platform.getStateLocation(_bundle);

	static final String			CONFIG_DEFAULT_ID_1		= "#1";										//$NON-NLS-1$
	private static final String	CONFIG_DEFAULT_ID_2		= "#2";										//$NON-NLS-1$
	private static final String	CONFIG_DEFAULT_ID_3		= "#3";										//$NON-NLS-1$
	private static final String	CONFIG_DEFAULT_ID_4		= "#4";										//$NON-NLS-1$
	private static final String	CONFIG_DEFAULT_ID_5		= "#5";										//$NON-NLS-1$
	private static final String	CONFIG_DEFAULT_ID_6		= "#6";										//$NON-NLS-1$
	private static final String	CONFIG_DEFAULT_ID_7		= "#7";										//$NON-NLS-1$
	private static final String	CONFIG_DEFAULT_ID_8		= "#8";										//$NON-NLS-1$
	private static final String	CONFIG_DEFAULT_ID_9		= "#9";										//$NON-NLS-1$
	private static final String	CONFIG_DEFAULT_ID_10	= "#10";									//$NON-NLS-1$
	//
	// common attributes
	private static final String	ATTR_ACTIVE_CONFIG_ID	= "activeConfigId";							//$NON-NLS-1$

	private static final String	ATTR_ID					= "id";										//$NON-NLS-1$
	private static final String	ATTR_CONFIG_NAME		= "name";									//$NON-NLS-1$
	//
	/*
	 * Root
	 */
	private static final String	TAG_ROOT				= "CalendarConfiguration";					//$NON-NLS-1$
	private static final String	ATTR_CONFIG_VERSION		= "configVersion";							//$NON-NLS-1$
	//
	/*
	 * Calendars
	 */
	private static final String	TAG_CALENDAR_CONFIG		= "CalendarConfig";							//$NON-NLS-1$
	private static final String	TAG_CALENDAR			= "Calendar";								//$NON-NLS-1$
	//
	static final int			CELL_HEIGHT_MIN			= 3;
	static final int			CELL_HEIGHT_MAX			= 200;
	//
	// !!! this is a code formatting separator !!!
	static {}
	//
	/**
	 * Contains all configurations which are loaded from a xml file.
	 */
	private static final ArrayList<CalendarConfig>	_allCalendarConfigs	= new ArrayList<>();
	private static CalendarConfig					_activeCalendarConfig;
	//
	private static String							_fromXml_ActiveCalendarConfigId;

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

	private static void createDefaults_Calendars() {

		_allCalendarConfigs.clear();

		// append custom configurations
		for (int configIndex = 1; configIndex < 11; configIndex++) {
			_allCalendarConfigs.add(createDefaults_Calendars_One(configIndex));
		}
	}

	/**
	 * @param configIndex
	 *            Index starts with 1.
	 * @return
	 */
	private static CalendarConfig createDefaults_Calendars_One(final int configIndex) {

		final CalendarConfig config = new CalendarConfig();

		switch (configIndex) {

		case 1:
			config.name = config.defaultId = CONFIG_DEFAULT_ID_1;
			break;

		case 2:
			config.name = config.defaultId = CONFIG_DEFAULT_ID_2;
			break;

		case 3:
			config.name = config.defaultId = CONFIG_DEFAULT_ID_3;
			break;

		case 4:
			config.name = config.defaultId = CONFIG_DEFAULT_ID_4;
			break;

		case 5:
			config.name = config.defaultId = CONFIG_DEFAULT_ID_5;
			break;

		case 6:
			config.name = config.defaultId = CONFIG_DEFAULT_ID_6;
			break;

		case 7:
			config.name = config.defaultId = CONFIG_DEFAULT_ID_7;
			break;

		case 8:
			config.name = config.defaultId = CONFIG_DEFAULT_ID_8;
			break;

		case 9:
			config.name = config.defaultId = CONFIG_DEFAULT_ID_9;
			break;

		case 10:
			config.name = config.defaultId = CONFIG_DEFAULT_ID_10;
			break;

		}

		return config;
	}

	private static void createXml_FromCalendarConfig(final CalendarConfig config, final IMemento xmlCalendars) {

		// <Calendar>
		final IMemento xmlConfig = xmlCalendars.createChild(TAG_CALENDAR);
		{
			xmlConfig.putString(ATTR_ID, config.id);
			xmlConfig.putString(ATTR_CONFIG_NAME, config.name);
		}
	}

	public static CalendarConfig getActiveCalendarConfig() {

		if (_activeCalendarConfig == null) {
			readConfigFromXml();
		}

		return _activeCalendarConfig;
	}

	/**
	 * @return Returns the index for the {@link #_activeCalendarConfig}, the index starts with 0.
	 */
	public static int getActiveCalendarConfigIndex() {

		final CalendarConfig activeConfig = getActiveCalendarConfig();

		for (int configIndex = 0; configIndex < _allCalendarConfigs.size(); configIndex++) {

			final CalendarConfig config = _allCalendarConfigs.get(configIndex);

			if (config.equals(activeConfig)) {
				return configIndex;
			}
		}

		// this case should not happen but ensure that a correct config is set

		_activeCalendarConfig = _allCalendarConfigs.get(0);

		return 0;
	}

	public static ArrayList<CalendarConfig> getAllCalendarConfigs() {

		// ensure configs are loaded
		getActiveCalendarConfig();

		return _allCalendarConfigs;
	}

	private static CalendarConfig getConfig_Calendar() {

		CalendarConfig activeConfig = null;

		if (_fromXml_ActiveCalendarConfigId != null) {

			// ensure config id belongs to a config which is available

			for (final CalendarConfig config : _allCalendarConfigs) {

				if (config.id.equals(_fromXml_ActiveCalendarConfigId)) {

					activeConfig = config;
					break;
				}
			}
		}

		if (activeConfig == null) {

			// this case should not happen, create a config

			StatusUtil.log("Created default config for calendar properties");//$NON-NLS-1$

			createDefaults_Calendars();

			activeConfig = _allCalendarConfigs.get(0);
		}

		return activeConfig;
	}

	private static File getConfigXmlFile() {

		final File layerFile = _stateLocation.append(CONFIG_FILE_NAME).toFile();

		return layerFile;
	}

	private static void parse_200_Calendars(final XMLMemento xmlRoot,
											final ArrayList<CalendarConfig> allCalendarConfigs) {

		if (xmlRoot == null) {
			return;
		}

		final XMLMemento xmlCalendars = (XMLMemento) xmlRoot.getChild(TAG_CALENDAR_CONFIG);

		if (xmlCalendars == null) {
			return;
		}

		_fromXml_ActiveCalendarConfigId = Util.getXmlString(xmlCalendars, ATTR_ACTIVE_CONFIG_ID, null);

		for (final IMemento mementoConfig : xmlCalendars.getChildren()) {

			final XMLMemento xmlConfig = (XMLMemento) mementoConfig;

			try {

				final String xmlConfigType = xmlConfig.getType();

				if (xmlConfigType.equals(TAG_CALENDAR)) {

					// <Track>

					final CalendarConfig calendarConfig = new CalendarConfig();

					parse_210_CalendarConfig(xmlConfig, calendarConfig);

					allCalendarConfigs.add(calendarConfig);
				}

			} catch (final Exception e) {
				StatusUtil.log(Util.dumpMemento(xmlConfig), e);
			}
		}
	}

	private static void parse_210_CalendarConfig(final XMLMemento xmlConfig, final CalendarConfig config) {

// SET_FORMATTING_OFF
		
		config.id		= Util.getXmlString(xmlConfig, ATTR_ID, Long.toString(System.nanoTime()));
		config.name		= Util.getXmlString(xmlConfig, ATTR_CONFIG_NAME, UI.EMPTY_STRING);

// SET_FORMATTING_ON

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
			parse_200_Calendars(xmlRoot, _allCalendarConfigs);

			// ensure config is created
			if (_allCalendarConfigs.size() == 0) {
				createDefaults_Calendars();
			}

			_activeCalendarConfig = getConfig_Calendar();

		} catch (final Exception e) {
			StatusUtil.log(e);
		} finally {
			Util.close(reader);
		}
	}

	public static void resetActiveCalendarConfiguration() {

		// do not replace the name
		final String oldName = _activeCalendarConfig.name;

		final int activeCalendarConfigIndex = getActiveCalendarConfigIndex();

		// remove old config
		_allCalendarConfigs.remove(_activeCalendarConfig);

		// create new config
		final int configIndex = activeCalendarConfigIndex + 1;
		final CalendarConfig newConfig = createDefaults_Calendars_One(configIndex);
		newConfig.name = oldName;

		// update model
		_activeCalendarConfig = newConfig;
		_allCalendarConfigs.add(activeCalendarConfigIndex, newConfig);
	}

	public static void resetAllCalendarConfigurations() {

		createDefaults_Calendars();

		_activeCalendarConfig = _allCalendarConfigs.get(0);
	}

	public static void saveState() {

		if (_activeCalendarConfig == null) {

			// this can happen when not yet used

			return;
		}

		final XMLMemento xmlRoot = create_Root();

		saveState_Calendars(xmlRoot);

		Util.writeXml(xmlRoot, getConfigXmlFile());
	}

	/**
	 * Calendars
	 */
	private static void saveState_Calendars(final XMLMemento xmlRoot) {

		final IMemento xmlCalendars = xmlRoot.createChild(TAG_CALENDAR_CONFIG);
		{
			xmlCalendars.putString(ATTR_ACTIVE_CONFIG_ID, _activeCalendarConfig.id);

			for (final CalendarConfig config : _allCalendarConfigs) {
				createXml_FromCalendarConfig(config, xmlCalendars);
			}
		}
	}

	public static void setActiveCalendarConfig(final CalendarConfig newConfig) {

		_activeCalendarConfig = newConfig;
	}

}
