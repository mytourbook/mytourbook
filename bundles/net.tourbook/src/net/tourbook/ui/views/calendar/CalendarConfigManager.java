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
import java.time.LocalDate;
import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

public class CalendarConfigManager {

	private static final String		CONFIG_FILE_NAME						= "calendar-config.xml";				//$NON-NLS-1$

	//
	/**
	 * Version number is not yet used.
	 */
	private static final int		CONFIG_VERSION							= 1;

	//
	private static final Bundle		_bundle									= TourbookPlugin
			.getDefault()
			.getBundle();

	private static final IPath		_stateLocation							= Platform.getStateLocation(_bundle);

	//
	static final String				CONFIG_DEFAULT_ID_1						= "#1";									//$NON-NLS-1$
	private static final String		CONFIG_DEFAULT_ID_2						= "#2";									//$NON-NLS-1$
	private static final String		CONFIG_DEFAULT_ID_3						= "#3";									//$NON-NLS-1$
	private static final String		CONFIG_DEFAULT_ID_4						= "#4";									//$NON-NLS-1$
	private static final String		CONFIG_DEFAULT_ID_5						= "#5";									//$NON-NLS-1$
	private static final String		CONFIG_DEFAULT_ID_6						= "#6";									//$NON-NLS-1$
	private static final String		CONFIG_DEFAULT_ID_7						= "#7";									//$NON-NLS-1$
	private static final String		CONFIG_DEFAULT_ID_8						= "#8";									//$NON-NLS-1$
	private static final String		CONFIG_DEFAULT_ID_9						= "#9";									//$NON-NLS-1$
	private static final String		CONFIG_DEFAULT_ID_10					= "#10";								//$NON-NLS-1$
	//
	// common attributes
	private static final String		ATTR_ACTIVE_CONFIG_ID					= "activeConfigId";						//$NON-NLS-1$
	private static final String		ATTR_ID									= "id";									//$NON-NLS-1$
	private static final String		ATTR_CONFIG_NAME						= "name";								//$NON-NLS-1$
	//
	/*
	 * Root
	 */
	private static final String		TAG_ROOT								= "CalendarConfiguration";				//$NON-NLS-1$
	private static final String		ATTR_CONFIG_VERSION						= "configVersion";						//$NON-NLS-1$

	//
	/*
	 * Calendars
	 */
	private static final String		TAG_CALENDAR_CONFIG						= "CalendarConfig";						//$NON-NLS-1$
	private static final String		TAG_CALENDAR							= "Calendar";							//$NON-NLS-1$
	//
	private static final String		TAG_ALTERNATE_MONTH_RGB					= "AlternateMonthRGB";					//$NON-NLS-1$
	//
	private static final String		ATTR_IS_HIDE_DAY_DATE_WHEN_NO_TOUR		= "isHideDayDateWhenNoTour";			//$NON-NLS-1$
	private static final String		ATTR_IS_SHOW_DATE_COLUMN				= "isShowDateColumn";					//$NON-NLS-1$
	private static final String		ATTR_IS_SHOW_DAY_DATE					= "isShowDayDate";						//$NON-NLS-1$
	private static final String		ATTR_IS_SHOW_DAY_DATE_WEEKEND_COLOR		= "isShowDayDateWeekendColor";			//$NON-NLS-1$
	private static final String		ATTR_IS_SHOW_SUMMARY_COLUMN				= "isShowSummaryColumn";				//$NON-NLS-1$
	private static final String		ATTR_IS_SHOW_YEAR_COLUMNS				= "isShowYearColumns";					//$NON-NLS-1$
	private static final String		ATTR_IS_TOGGLE_MONTH_COLOR				= "isToggleMonthColor";					//$NON-NLS-1$
	private static final String		ATTR_NUM_YEAR_COLUMNS					= "calendarColumns";					//$NON-NLS-1$
	private static final String		ATTR_YEAR_COLUMNS_SPACING				= "calendarColumnsSpacing";				//$NON-NLS-1$
	private static final String		ATTR_YEAR_COLUMNS_START					= "calendarColumnsStart";				//$NON-NLS-1$
	private static final String		ATTR_DATE_COLUMN_CONTENT				= "dateColumnContent";					//$NON-NLS-1$
	private static final String		ATTR_DATE_COLUMN_FONT					= "dateColumnFont";						//$NON-NLS-1$
	private static final String		ATTR_DATE_COLUMN_WIDTH					= "dateColumnWidth";					//$NON-NLS-1$
	private static final String		ATTR_DAY_CONTENT_FONT					= "dayContentFont";						//$NON-NLS-1$
	private static final String		ATTR_DAY_CONTENT_COLOR					= "dayContentColor";					//$NON-NLS-1$
	private static final String		ATTR_DAY_DATE_FORMAT					= "dayDateFormat";						//$NON-NLS-1$
	private static final String		ATTR_DAY_DATE_FONT						= "dayDateFont";						//$NON-NLS-1$
	private static final String		ATTR_SUMMARY_COLUMN_WIDTH				= "summaryColumnWidth";					//$NON-NLS-1$
	private static final String		ATTR_TOUR_BACKGROUND					= "tourBackground";						//$NON-NLS-1$
	private static final String		ATTR_TOUR_BACKGROUND_COLOR1				= "tourBackgroundColor1";				//$NON-NLS-1$
	private static final String		ATTR_TOUR_BACKGROUND_COLOR2				= "tourBackgroundColor2";				//$NON-NLS-1$
	private static final String		ATTR_TOUR_BORDER_WIDTH					= "tourBackgroundWidth";				//$NON-NLS-1$
	private static final String		ATTR_TOUR_BORDER						= "tourBorder";							//$NON-NLS-1$
	private static final String		ATTR_TOUR_BORDER_COLOR					= "tourBorderColor";					//$NON-NLS-1$
	private static final String		ATTR_TOUR_BACKGROUND_WIDTH				= "tourBorderWidth";					//$NON-NLS-1$
	private static final String		ATTR_WEEK_HEIGHT						= "weekHeight";							//$NON-NLS-1$
	private static final String		ATTR_YEAR_HEADER_FONT					= "yearHeaderFont";						//$NON-NLS-1$
	//
	static final RGB				DEFAULT_ALTERNATE_MONTH_RGB				= new RGB(0xf0, 0xf0, 0xf0);
	static final int				DEFAULT_NUM_YEAR_COLUMNS				= 2;
	static final ColumnStart		DEFAULT_YEAR_COLUMNS_LAYOUT				= ColumnStart.CONTINUOUSLY;
	static final int				DEFAULT_YEAR_COLUMNS_SPACING			= 10;
	static final CalendarColor		DEFAULT_DAY_CONTENT_COLOR				= CalendarColor.CONTRAST;
	static final DayDateFormat		DEFAULT_DAY_DATE_FORMAT					= DayDateFormat.DAY;
	static final int				DEFAULT_DATE_COLUMN_WIDTH				= 50;
	static final DateColumnContent	DEFAULT_DATE_COLUMN_CONTENT				= DateColumnContent.MONTH;
	static final boolean			DEFAULT_IS_SHOW_DAY_DATE_WEEKEND_COLOR	= false;
	static final int				DEFAULT_SUMMARY_COLUMN_WIDTH			= 50;
	static final TourBackground		DEFAULT_TOUR_BACKGROUND					= TourBackground.FILL;
	static final CalendarColor		DEFAULT_TOUR_BACKGROUND_COLOR1			= CalendarColor.DARK;
	static final CalendarColor		DEFAULT_TOUR_BACKGROUND_COLOR2			= CalendarColor.BRIGHT;
	static final int				DEFAULT_TOUR_BACKGROUND_WIDTH			= 3;
	static final TourBorder			DEFAULT_TOUR_BORDER						= TourBorder.NO_BORDER;
	static final CalendarColor		DEFAULT_TOUR_BORDER_COLOR				= CalendarColor.LINE;
	static final int				DEFAULT_TOUR_BORDER_WIDTH				= 1;
	static final int				DEFAULT_WEEK_HEIGHT						= 70;
	//
	static final int				YEAR_COLUMNS_MIN						= 1;
	static final int				YEAR_COLUMNS_MAX						= 100;
	static final int				CALENDAR_COLUMNS_SPACE_MIN				= 0;
	static final int				CALENDAR_COLUMNS_SPACE_MAX				= 100;
	static final int				WEEK_HEIGHT_MIN							= 1;
	static final int				WEEK_HEIGHT_MAX							= 500;
	//
	// !!! this is a code formatting separator !!!
	static {}
	//
	private static final CalendarColorData[]		_allCalendarColorData			= new CalendarColorData[] {

			new CalendarColorData(
					CalendarColor.BRIGHT,
					Messages.Calendar_Config_Color_Bright),

			new CalendarColorData(
					CalendarColor.DARK,
					Messages.Calendar_Config_Color_Dark),

			new CalendarColorData(
					CalendarColor.LINE,
					Messages.Calendar_Config_Color_Line),

			new CalendarColorData(
					CalendarColor.BLACK,
					Messages.Calendar_Config_Color_Black),

			new CalendarColorData(
					CalendarColor.WHITE,
					Messages.Calendar_Config_Color_White),
	};

	private static final DateColumnData[]			_allDateColumnData				= new DateColumnData[] {

			new DateColumnData(DateColumnContent.WEEK_NUMBER, Messages.Calendar_Config_DateColumn_WeekNumber),
			new DateColumnData(DateColumnContent.MONTH, Messages.Calendar_Config_DateColumn_Month),
			new DateColumnData(DateColumnContent.YEAR, Messages.Calendar_Config_DateColumn_Year),
	};

	private static final ColumnLayoutData[]			_allColumnLayoutData			= new ColumnLayoutData[] {

			new ColumnLayoutData(ColumnStart.JAN, TimeTools.month_Full[0]),
			new ColumnLayoutData(ColumnStart.FEB, TimeTools.month_Full[1]),
			new ColumnLayoutData(ColumnStart.MAR, TimeTools.month_Full[2]),
			new ColumnLayoutData(ColumnStart.APR, TimeTools.month_Full[3]),
			new ColumnLayoutData(ColumnStart.MAY, TimeTools.month_Full[4]),
			new ColumnLayoutData(ColumnStart.JUN, TimeTools.month_Full[5]),
			new ColumnLayoutData(ColumnStart.JUL, TimeTools.month_Full[6]),
			new ColumnLayoutData(ColumnStart.AUG, TimeTools.month_Full[7]),
			new ColumnLayoutData(ColumnStart.SEP, TimeTools.month_Full[8]),
			new ColumnLayoutData(ColumnStart.OCT, TimeTools.month_Full[9]),
			new ColumnLayoutData(ColumnStart.NOV, TimeTools.month_Full[10]),
			new ColumnLayoutData(ColumnStart.DEC, TimeTools.month_Full[11]),

			new ColumnLayoutData(ColumnStart.CONTINUOUSLY, Messages.Calendar_Config_ColumnLayout_Continuously),
	};

	private static final DayHeaderDateFormatData[]	_allDateHeaderDateFormatData	= new DayHeaderDateFormatData[] {

			new DayHeaderDateFormatData(
					DayDateFormat.DAY,
					NLS.bind(
							Messages.Calendar_Config_DayHeaderDateFormat_Day,
							TimeTools.Formatter_Day.format(LocalDate.now()))),

			new DayHeaderDateFormatData(
					DayDateFormat.DAY_MONTH,
					TimeTools.Formatter_DayMonth.format(LocalDate.now())),

			new DayHeaderDateFormatData(
					DayDateFormat.DAY_MONTH_YEAR,
					TimeTools.Formatter_DayMonthYear.format(LocalDate.now())),

			new DayHeaderDateFormatData(
					DayDateFormat.AUTOMATIC,
					Messages.Calendar_Config_DayHeaderDateFormat_Automatic),
	};

	private static final TourBackgroundData[]		_allTourBackgroundData			= new TourBackgroundData[] {

			new TourBackgroundData(
					TourBackground.NO_BACKGROUND,
					Messages.Calendar_Config_TourBackground_NoBackground,
					false,
					false,
					false),

			new TourBackgroundData(
					TourBackground.FILL,
					Messages.Calendar_Config_TourBackground_Fill,
					true,
					false,
					false),

			new TourBackgroundData(
					TourBackground.FILL_LEFT,
					Messages.Calendar_Config_TourBackground_Fill_Left,
					true,
					false,
					true),

			new TourBackgroundData(
					TourBackground.FILL_RIGHT,
					Messages.Calendar_Config_TourBackground_Fill_Right,
					true,
					false,
					true),

			new TourBackgroundData(
					TourBackground.CIRCLE,
					Messages.Calendar_Config_TourBackground_Circle,
					true,
					false,
					false),

			new TourBackgroundData(
					TourBackground.GRADIENT_HORIZONTAL,
					Messages.Calendar_Config_TourBackground_GradientHorizontal,
					true,
					true,
					false),

			new TourBackgroundData(
					TourBackground.GRADIENT_VERTICAL,
					Messages.Calendar_Config_TourBackground_GradientVertical,
					true,
					true,
					false),
	};

	private static final TourBorderData[]			_allTourBorderData				= new TourBorderData[] {

			new TourBorderData(
					TourBorder.NO_BORDER,
					Messages.Calendar_Config_TourBorder_NoBorder,
					false,
					false),

			new TourBorderData(
					TourBorder.BORDER_ALL,
					Messages.Calendar_Config_TourBorder_All,
					true,
					true),

			new TourBorderData(
					TourBorder.BORDER_TOP,
					Messages.Calendar_Config_TourBorder_Top,
					true,
					true),

			new TourBorderData(
					TourBorder.BORDER_BOTTOM,
					Messages.Calendar_Config_TourBorder_Bottom,
					true,
					true),

			new TourBorderData(
					TourBorder.BORDER_TOP_BOTTOM,
					Messages.Calendar_Config_TourBorder_TopBottom,
					true,
					true),

			new TourBorderData(
					TourBorder.BORDER_LEFT,
					Messages.Calendar_Config_TourBorder_Left,
					true,
					true),

			new TourBorderData(
					TourBorder.BORDER_RIGHT,
					Messages.Calendar_Config_TourBorder_Right,
					true,
					true),

			new TourBorderData(
					TourBorder.BORDER_LEFT_RIGHT,
					Messages.Calendar_Config_TourBorder_LeftRight,
					true,
					true),
	};

	private static DayContentColorData[]			_allDayContentColorData			= new DayContentColorData[] {

			new DayContentColorData(CalendarColor.CONTRAST, Messages.Calendar_Config_Color_Contrast),
			new DayContentColorData(CalendarColor.BRIGHT, Messages.Calendar_Config_Color_Bright),
			new DayContentColorData(CalendarColor.DARK, Messages.Calendar_Config_Color_Dark),
			new DayContentColorData(CalendarColor.LINE, Messages.Calendar_Config_Color_Line),
			new DayContentColorData(CalendarColor.BLACK, Messages.Calendar_Config_Color_Black),
			new DayContentColorData(CalendarColor.WHITE, Messages.Calendar_Config_Color_White),
	};

	//
	/**
	 * Contains all configurations which are loaded from a xml file.
	 */
	private static final ArrayList<CalendarConfig>	_allCalendarConfigs				= new ArrayList<>();

	private static CalendarConfig					_activeCalendarConfig;
	//
	private static String							_fromXml_ActiveCalendarConfigId;

	/**
	 * Calendarview or <code>null</code> when closed.
	 */
	private static ICalendarConfigProvider			_configProvider_CalendarView;

	private static ICalendarConfigProvider			_configProvider_SlideoutCalendarOptions;

	public static class CalendarColorData {

		String			label;
		CalendarColor	color;

		public CalendarColorData(final CalendarColor color, final String label) {

			this.color = color;
			this.label = label;
		}

	}

	static class ColumnLayoutData {

		String		label;
		ColumnStart	columnLayout;

		public ColumnLayoutData(final ColumnStart columnLayout, final String label) {

			this.columnLayout = columnLayout;
			this.label = label;
		}
	}

	static class DateColumnData {

		String				label;
		DateColumnContent	dateColumn;

		public DateColumnData(final DateColumnContent dateColumn, final String label) {

			this.dateColumn = dateColumn;
			this.label = label;
		}
	}

	static class DayContentColorData {

		String			label;
		CalendarColor	dayContentColor;

		DayContentColorData(final CalendarColor dayContentColor, final String label) {

			this.label = label;
			this.dayContentColor = dayContentColor;
		}

	}

	static class DayHeaderDateFormatData {

		String			label;
		DayDateFormat	dayHeaderDateFormat;

		public DayHeaderDateFormatData(final DayDateFormat dayHeaderDateFormat, final String label) {

			this.dayHeaderDateFormat = dayHeaderDateFormat;
			this.label = label;
		}
	}

	interface ICalendarConfigProvider {

		/**
		 * Calendar config has changed, update the UI.
		 */
		void updateUI_CalendarConfig();
	}

	static class TourBackgroundData {

		TourBackground	tourBackground;
		String			label;

		boolean			isWidth;
		boolean			isColor1;
		boolean			isColor2;

		public TourBackgroundData(	final TourBackground tourBackground,
									final String label,
									final boolean isColor1,
									final boolean isColor2,
									final boolean isWidth) {

			this.tourBackground = tourBackground;
			this.label = label;

			this.isWidth = isWidth;
			this.isColor1 = isColor1;
			this.isColor2 = isColor2;
		}
	}

	static class TourBorderData {

		TourBorder	tourBorder;
		String		label;

		boolean		isColor;
		boolean		isWidth;

		public TourBorderData(	final TourBorder tourBorder,
								final String label,
								final boolean isColor,
								final boolean isWidth) {

			this.tourBorder = tourBorder;
			this.label = label;
			this.isColor = isColor;
			this.isWidth = isWidth;
		}
	}

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
// SET_FORMATTING_OFF
			
			// config
			xmlConfig.putString(ATTR_ID, 								config.id);
			xmlConfig.putString(ATTR_CONFIG_NAME, 						config.name);

			// day date
			xmlConfig.putBoolean(ATTR_IS_HIDE_DAY_DATE_WHEN_NO_TOUR, 	config.isHideDayDateWhenNoTour);
			xmlConfig.putBoolean(ATTR_IS_SHOW_DAY_DATE, 				config.isShowDayDate);
			xmlConfig.putBoolean(ATTR_IS_SHOW_DAY_DATE_WEEKEND_COLOR, 	config.isShowDayDateWeekendColor);
			Util.setXmlEnum(xmlConfig, ATTR_DAY_DATE_FORMAT, 			config.dayDateFormat);
			Util.setXmlFont(xmlConfig, ATTR_DAY_DATE_FONT, 				config.dayDateFont);

			// day content
			xmlConfig.putBoolean(ATTR_IS_TOGGLE_MONTH_COLOR, 			config.isToggleMonthColor);
			xmlConfig.putInteger(ATTR_TOUR_BACKGROUND_WIDTH, 			config.tourBackgroundWidth);
			xmlConfig.putInteger(ATTR_TOUR_BORDER_WIDTH, 				config.tourBorderWidth);
			Util.setXmlEnum(xmlConfig, ATTR_DAY_CONTENT_COLOR, 			config.dayContentColor);
			Util.setXmlFont(xmlConfig, ATTR_DAY_CONTENT_FONT, 			config.dayContentFont);
			Util.setXmlEnum(xmlConfig, ATTR_TOUR_BACKGROUND, 			config.tourBackground);
			Util.setXmlEnum(xmlConfig, ATTR_TOUR_BACKGROUND_COLOR1, 	config.tourBackgroundColor1);
			Util.setXmlEnum(xmlConfig, ATTR_TOUR_BACKGROUND_COLOR2, 	config.tourBackgroundColor2);
			Util.setXmlEnum(xmlConfig, ATTR_TOUR_BORDER, 				config.tourBorder);
			Util.setXmlEnum(xmlConfig, ATTR_TOUR_BORDER_COLOR, 			config.tourBorderColor);

			Util.setXmlRgb(xmlConfig, TAG_ALTERNATE_MONTH_RGB, 			config.alternateMonthRGB);

			// date column
			xmlConfig.putBoolean(ATTR_IS_SHOW_DATE_COLUMN, 				config.isShowDateColumn);
			xmlConfig.putInteger(ATTR_DATE_COLUMN_WIDTH, 				config.dateColumnWidth);
			Util.setXmlEnum(xmlConfig, ATTR_DATE_COLUMN_CONTENT, 		config.dateColumnContent);
			Util.setXmlFont(xmlConfig, ATTR_DATE_COLUMN_FONT, 			config.dateColumnFont);

			// summary column
			xmlConfig.putBoolean(ATTR_IS_SHOW_SUMMARY_COLUMN, 			config.isShowSummaryColumn);
			xmlConfig.putInteger(ATTR_SUMMARY_COLUMN_WIDTH, 			config.summaryColumnWidth);

			// year column
			xmlConfig.putBoolean(ATTR_IS_SHOW_YEAR_COLUMNS, 			config.isShowYearColumns);
			xmlConfig.putInteger(ATTR_NUM_YEAR_COLUMNS, 				config.numYearColumns);
			xmlConfig.putInteger(ATTR_YEAR_COLUMNS_SPACING, 			config.yearColumnsSpacing);
			Util.setXmlEnum(xmlConfig, ATTR_YEAR_COLUMNS_START, 		config.yearColumnsStart);
			Util.setXmlFont(xmlConfig, ATTR_YEAR_HEADER_FONT, 			config.yearHeaderFont);
			
			// layout
			xmlConfig.putInteger(ATTR_WEEK_HEIGHT, 						config.weekHeight);
			
// SET_FORMATTING_ON
		}
	}

	static CalendarConfig getActiveCalendarConfig() {

		if (_activeCalendarConfig == null) {
			readConfigFromXml();
		}

		return _activeCalendarConfig;
	}

	/**
	 * @return Returns the index for the {@link #_activeCalendarConfig}, the index starts with 0.
	 */
	static int getActiveCalendarConfigIndex() {

		final CalendarConfig activeConfig = getActiveCalendarConfig();

		for (int configIndex = 0; configIndex < _allCalendarConfigs.size(); configIndex++) {

			final CalendarConfig config = _allCalendarConfigs.get(configIndex);

			if (config.equals(activeConfig)) {
				return configIndex;
			}
		}

		// this case should not happen but ensure that a correct config is set

		setActiveCalendarConfigIntern(_allCalendarConfigs.get(0), null);

		return 0;
	}

	static CalendarColorData[] getAllCalendarColorData() {
		return _allCalendarColorData;
	}

	static ArrayList<CalendarConfig> getAllCalendarConfigs() {

		// ensure configs are loaded
		getActiveCalendarConfig();

		return _allCalendarConfigs;
	}

	public static ColumnLayoutData[] getAllColumnLayoutData() {
		return _allColumnLayoutData;
	}

	static DateColumnData[] getAllDateColumnData() {
		return _allDateColumnData;
	}

	static DayContentColorData[] getAllDayContentColorData() {
		return _allDayContentColorData;
	}

	static DayHeaderDateFormatData[] getAllDayHeaderDateFormatData() {
		return _allDateHeaderDateFormatData;
	}

	static TourBackgroundData[] getAllTourBackgroundData() {
		return _allTourBackgroundData;
	}

	static TourBorderData[] getAllTourBorderData() {
		return _allTourBorderData;
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

					// <Calendar>

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

		// !!! getFontData() MUST be created for EVERY font otherwise they use all the SAME font !!!
		final Font defaultFont = JFaceResources.getFontRegistry().defaultFont();

// SET_FORMATTING_OFF
		
		// config
		config.id							= Util.getXmlString(xmlConfig,	ATTR_ID,			Long.toString(System.nanoTime()));
		config.name							= Util.getXmlString(xmlConfig,	ATTR_CONFIG_NAME,	UI.EMPTY_STRING);
		
		// day date
		config.isHideDayDateWhenNoTour		= Util.getXmlBoolean(xmlConfig, 				ATTR_IS_HIDE_DAY_DATE_WHEN_NO_TOUR,		true);
		config.isShowDayDate				= Util.getXmlBoolean(xmlConfig, 				ATTR_IS_SHOW_DAY_DATE,					true);
		config.isShowDayDateWeekendColor	= Util.getXmlBoolean(xmlConfig, 				ATTR_IS_SHOW_DAY_DATE_WEEKEND_COLOR,	DEFAULT_IS_SHOW_DAY_DATE_WEEKEND_COLOR);
		config.dayDateFont 					= Util.getXmlFont(xmlConfig, 					ATTR_DAY_DATE_FONT, 					defaultFont.getFontData()[0]);
		config.dayDateFormat				= (DayDateFormat) Util.getXmlEnum(xmlConfig,	ATTR_DAY_DATE_FORMAT,					DEFAULT_DAY_DATE_FORMAT);
		                                    
		// day content
		config.alternateMonthRGB			= Util.getXmlRgb(xmlConfig, 		TAG_ALTERNATE_MONTH_RGB,		DEFAULT_ALTERNATE_MONTH_RGB);
		config.dayContentFont 				= Util.getXmlFont(xmlConfig, 		ATTR_DAY_CONTENT_FONT, 			defaultFont.getFontData()[0]);
		config.isToggleMonthColor			= Util.getXmlBoolean(xmlConfig, 	ATTR_IS_TOGGLE_MONTH_COLOR,		true);
		config.tourBackgroundWidth			= Util.getXmlInteger(xmlConfig, 	ATTR_TOUR_BACKGROUND_WIDTH, 	DEFAULT_TOUR_BACKGROUND_WIDTH, 1, 100);
		config.tourBorderWidth				= Util.getXmlInteger(xmlConfig, 	ATTR_TOUR_BORDER_WIDTH,		 	DEFAULT_TOUR_BORDER_WIDTH, 1, 100);
		
		config.dayContentColor				= (CalendarColor) Util.getXmlEnum(xmlConfig,	ATTR_DAY_CONTENT_COLOR,			DEFAULT_DAY_CONTENT_COLOR);
		config.tourBackground 				= (TourBackground) Util.getXmlEnum(xmlConfig,	ATTR_TOUR_BACKGROUND,			DEFAULT_TOUR_BACKGROUND);
		config.tourBackgroundColor1 		= (CalendarColor) Util.getXmlEnum(xmlConfig,	ATTR_TOUR_BACKGROUND_COLOR1,	DEFAULT_TOUR_BACKGROUND_COLOR1);
		config.tourBackgroundColor2 		= (CalendarColor) Util.getXmlEnum(xmlConfig,	ATTR_TOUR_BACKGROUND_COLOR2,	DEFAULT_TOUR_BACKGROUND_COLOR2);
		config.tourBorder 					= (TourBorder) Util.getXmlEnum(xmlConfig,		ATTR_TOUR_BORDER,				DEFAULT_TOUR_BORDER);
		config.tourBorderColor 				= (CalendarColor) Util.getXmlEnum(xmlConfig,	ATTR_TOUR_BORDER_COLOR,			DEFAULT_TOUR_BORDER_COLOR);
		                                                                                                            
		// date column
		config.isShowDateColumn				= Util.getXmlBoolean(xmlConfig, 					ATTR_IS_SHOW_DATE_COLUMN,	true);
		config.dateColumnFont 				= Util.getXmlFont(xmlConfig, 						ATTR_DATE_COLUMN_FONT, 		defaultFont.getFontData()[0]);
		config.dateColumnWidth				= Util.getXmlInteger(xmlConfig, 					ATTR_DATE_COLUMN_WIDTH,		DEFAULT_DATE_COLUMN_WIDTH);
		config.dateColumnContent			= (DateColumnContent) Util.getXmlEnum(xmlConfig,	ATTR_DATE_COLUMN_CONTENT,	DateColumnContent.WEEK_NUMBER);
		                                                                                                            
		// summary column
		config.isShowSummaryColumn			= Util.getXmlBoolean(xmlConfig, 	ATTR_IS_SHOW_SUMMARY_COLUMN,	true);
		config.summaryColumnWidth			= Util.getXmlInteger(xmlConfig, 	ATTR_SUMMARY_COLUMN_WIDTH,		DEFAULT_SUMMARY_COLUMN_WIDTH);
        
		// year columns
		config.isShowYearColumns			= Util.getXmlBoolean(xmlConfig, 			ATTR_IS_SHOW_YEAR_COLUMNS,	true);
		config.numYearColumns				= Util.getXmlInteger(xmlConfig,				ATTR_NUM_YEAR_COLUMNS,		DEFAULT_NUM_YEAR_COLUMNS);
		config.yearColumnsSpacing			= Util.getXmlInteger(xmlConfig, 			ATTR_YEAR_COLUMNS_SPACING,	DEFAULT_YEAR_COLUMNS_SPACING);
		config.yearColumnsStart				= (ColumnStart) Util.getXmlEnum(xmlConfig,	ATTR_YEAR_COLUMNS_START,	DEFAULT_YEAR_COLUMNS_LAYOUT);
		config.yearHeaderFont				= Util.getXmlFont(xmlConfig, 				ATTR_YEAR_HEADER_FONT,		defaultFont.getFontData()[0]);
		
		// layout
		config.weekHeight					= Util.getXmlInteger(xmlConfig, 			ATTR_WEEK_HEIGHT,				DEFAULT_WEEK_HEIGHT);

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

			setActiveCalendarConfigIntern(getConfig_Calendar(), null);

		} catch (final Exception e) {
			StatusUtil.log(e);
		} finally {
			Util.close(reader);
		}
	}

	static void resetActiveCalendarConfiguration() {

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
		_allCalendarConfigs.add(activeCalendarConfigIndex, newConfig);
		setActiveCalendarConfigIntern(newConfig, null);
	}

	static void resetAllCalendarConfigurations() {

		createDefaults_Calendars();

		setActiveCalendarConfigIntern(_allCalendarConfigs.get(0), null);
	}

	static void saveState() {

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

	static void setActiveCalendarConfig(final CalendarConfig selectedConfig,
										final ICalendarConfigProvider configProvider) {

		setActiveCalendarConfigIntern(selectedConfig, configProvider);
	}

	private static void setActiveCalendarConfigIntern(	final CalendarConfig calendarConfig,
														final ICalendarConfigProvider configProvider) {

		_activeCalendarConfig = calendarConfig;

		if (configProvider != null) {

			if (_configProvider_CalendarView != null && _configProvider_CalendarView != configProvider) {
				_configProvider_CalendarView.updateUI_CalendarConfig();
			}

			if (_configProvider_SlideoutCalendarOptions != null
					&& _configProvider_SlideoutCalendarOptions != configProvider) {
				_configProvider_SlideoutCalendarOptions.updateUI_CalendarConfig();
			}
		}
	}

	static void setConfigProvider(final CalendarView calendarView) {
		_configProvider_CalendarView = calendarView;
	}

	static void setConfigProvider(final SlideoutCalendarOptions slideoutCalendarOptions) {
		_configProvider_SlideoutCalendarOptions = slideoutCalendarOptions;
	}

}
