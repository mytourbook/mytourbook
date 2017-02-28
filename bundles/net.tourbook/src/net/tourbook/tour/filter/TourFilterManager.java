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
package net.tourbook.tour.filter;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.ActionTourFilter;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

public class TourFilterManager {

	private static final String							TOUR_FILTER_FILE_NAME		= "tour-filter.xml";											//$NON-NLS-1$
	private static final int							TOUR_FILTER_VERSION			= 1;

	private static final String							TAG_PROFILE					= "Profile";													//$NON-NLS-1$
	private static final String							TAG_PROPERTY				= "Property";													//$NON-NLS-1$
	private static final String							TAG_ROOT					= "TourFilterProfiles";											//$NON-NLS-1$

	private static final String							ATTR_IS_ENABLED				= "isEnabled";													//$NON-NLS-1$
	private static final String							ATTR_IS_SELECTED			= "isSelected";													//$NON-NLS-1$
	private static final String							ATTR_FIELD_ID				= "fieldId";													//$NON-NLS-1$
	private static final String							ATTR_FIELD_OPERATOR			= "fieldOperator";												//$NON-NLS-1$
	private static final String							ATTR_NAME					= "name";														//$NON-NLS-1$
	private static final String							ATTR_SEASON_DAY				= "seasonDay";													//$NON-NLS-1$
	private static final String							ATTR_SEASON_MONTH			= "seasonMonth";												//$NON-NLS-1$
	private static final String							ATTR_TOUR_FILTER_VERSION	= "tourFilterVersion";											//$NON-NLS-1$
	private static final String							ATTR_VALUE					= "value";														//$NON-NLS-1$

	private static final String							ATTR_DATE_YEAR				= "dateYear";													//$NON-NLS-1$
	private static final String							ATTR_DATE_MONTH				= "dateMonth";													//$NON-NLS-1$
	private static final String							ATTR_DATE_DAY				= "dateDay";													//$NON-NLS-1$
	private static final String							ATTR_TIME_HOUR				= "timeHour";													//$NON-NLS-1$
	private static final String							ATTR_TIME_MINUTE			= "timeMinute";													//$NON-NLS-1$

// SET_FORMATTING_OFF

	public static final TourFilterFieldOperatorConfig[]	TOUR_FILTER_OPERATORS		= {
	                                   		                   			   
	   new TourFilterFieldOperatorConfig(TourFilterFieldOperator.STARTS_WITH,				Messages.Tour_Filter_Operator_StartsWith),
	   new TourFilterFieldOperatorConfig(TourFilterFieldOperator.ENDS_WITH,					Messages.Tour_Filter_Operator_EndsWith),
	   new TourFilterFieldOperatorConfig(TourFilterFieldOperator.EQUALS,					Messages.Tour_Filter_Operator_Equals),
	   new TourFilterFieldOperatorConfig(TourFilterFieldOperator.NOT_EQUALS,				Messages.Tour_Filter_Operator_NotEquals),
	   new TourFilterFieldOperatorConfig(TourFilterFieldOperator.LESS_THAN,					Messages.Tour_Filter_Operator_LessThan),
	   new TourFilterFieldOperatorConfig(TourFilterFieldOperator.LESS_THAN_OR_EQUAL,		Messages.Tour_Filter_Operator_LessThanOrEqual),
	   new TourFilterFieldOperatorConfig(TourFilterFieldOperator.GREATER_THAN,				Messages.Tour_Filter_Operator_GreaterThan),
	   new TourFilterFieldOperatorConfig(TourFilterFieldOperator.GREATER_THAN_OR_EQUAL,		Messages.Tour_Filter_Operator_GreaterThanOrEqual),
	   new TourFilterFieldOperatorConfig(TourFilterFieldOperator.BETWEEN,					Messages.Tour_Filter_Operator_Between),
	   new TourFilterFieldOperatorConfig(TourFilterFieldOperator.NOT_BETWEEN,				Messages.Tour_Filter_Operator_NotBetween),
	   new TourFilterFieldOperatorConfig(TourFilterFieldOperator.IS_EMPTY,					Messages.Tour_Filter_Operator_IsEmpty),
	   new TourFilterFieldOperatorConfig(TourFilterFieldOperator.IS_NOT_EMPTY,				Messages.Tour_Filter_Operator_IsNotEmpty),

// is not yet implemented
//
//	   new TourFilterOperatorDef(TourFilterOperator.LIKE,					Messages.Tour_Filter_Operator_Like),
//	   new TourFilterOperatorDef(TourFilterOperator.NOT_LIKE,				Messages.Tour_Filter_Operator_NotLike),
//	   new TourFilterOperatorDef(TourFilterOperator.INCLUDE_ANY,			Messages.Tour_Filter_Operator_IncludeAny),
//	   new TourFilterOperatorDef(TourFilterOperator.EXCLUDE_ALL,			Messages.Tour_Filter_Operator_ExcludeAll),
	};
	
	public static final TourFilterFieldOperator[]			FILTER_OPERATORS_DATE_TIME = {
	                                        	                        
	   TourFilterFieldOperator.EQUALS,
	   TourFilterFieldOperator.NOT_EQUALS,
	   TourFilterFieldOperator.LESS_THAN,
	   TourFilterFieldOperator.LESS_THAN_OR_EQUAL,
	   TourFilterFieldOperator.GREATER_THAN,
	   TourFilterFieldOperator.GREATER_THAN_OR_EQUAL,
	   TourFilterFieldOperator.BETWEEN,
	   TourFilterFieldOperator.NOT_BETWEEN,
	   TourFilterFieldOperator.IS_EMPTY,
	   TourFilterFieldOperator.IS_NOT_EMPTY,
	};

	public static final TourFilterFieldOperator[]			FILTER_OPERATORS_NUMBER = {
	                                        	                         
		TourFilterFieldOperator.EQUALS,
		TourFilterFieldOperator.NOT_EQUALS,
		TourFilterFieldOperator.LESS_THAN,
		TourFilterFieldOperator.LESS_THAN_OR_EQUAL,
		TourFilterFieldOperator.GREATER_THAN,
		TourFilterFieldOperator.GREATER_THAN_OR_EQUAL,
		TourFilterFieldOperator.BETWEEN,
		TourFilterFieldOperator.NOT_BETWEEN,
	};
	
	public static final TourFilterFieldOperator[]			FILTER_OPERATORS_TEXT = {
	                                        	                           
		TourFilterFieldOperator.STARTS_WITH,
		TourFilterFieldOperator.ENDS_WITH,
		TourFilterFieldOperator.EQUALS,
		TourFilterFieldOperator.NOT_EQUALS,
		TourFilterFieldOperator.LESS_THAN,
		TourFilterFieldOperator.LESS_THAN_OR_EQUAL,
		TourFilterFieldOperator.GREATER_THAN,
		TourFilterFieldOperator.GREATER_THAN_OR_EQUAL,
		TourFilterFieldOperator.BETWEEN,
		TourFilterFieldOperator.NOT_BETWEEN,
		TourFilterFieldOperator.IS_EMPTY,
		TourFilterFieldOperator.IS_NOT_EMPTY,
	};

	public static final TourFilterFieldOperator[]			FILTER_OPERATORS_SEASON = {
	                                             			                         
		TourFilterFieldOperator.LESS_THAN,
		TourFilterFieldOperator.LESS_THAN_OR_EQUAL,
		TourFilterFieldOperator.GREATER_THAN,
		TourFilterFieldOperator.GREATER_THAN_OR_EQUAL,
		TourFilterFieldOperator.BETWEEN,
		TourFilterFieldOperator.NOT_BETWEEN,
	};

// SET_FORMATTING_ON

	/**
	 * This is also the sequence how the fields are displayed in the UI
	 */
	public static final TourFilterFieldConfig[]			FILTER_FIELD_CONFIG;

	static {

		final TourFilterFieldConfig[] CONFIG =

				{
					new TourFilterFieldConfig(
							Messages.Tour_Filter_Field_TourDate,
							TourFilterFieldId.TOUR_DATE,
							TourFilterFieldType.DATE,
							FILTER_OPERATORS_DATE_TIME),

					new TourFilterFieldConfig(
							Messages.Tour_Filter_Field_TourTime,
							TourFilterFieldId.TOUR_TIME,
							TourFilterFieldType.TIME,
							FILTER_OPERATORS_DATE_TIME),

					new TourFilterFieldConfig(
							Messages.Tour_Filter_Field_Season,
							TourFilterFieldId.SEASON_DATE,
							TourFilterFieldType.SEASON,
							FILTER_OPERATORS_SEASON),

					new TourFilterFieldConfig(
							Messages.Tour_Filter_Field_RecordingTime,
							TourFilterFieldId.RECORDING_TIME,
							TourFilterFieldType.DURATION,
							FILTER_OPERATORS_NUMBER,
							0,
							Integer.MAX_VALUE,
							60),

					new TourFilterFieldConfig(
							Messages.Tour_Filter_Field_DrivingTime,
							TourFilterFieldId.DRIVING_TIME,
							TourFilterFieldType.DURATION,
							FILTER_OPERATORS_NUMBER,
							0,
							Integer.MAX_VALUE,
							60),

					new TourFilterFieldConfig(
							Messages.Tour_Filter_Field_BreakTime,
							TourFilterFieldId.BREAK_TIME,
							TourFilterFieldType.DURATION,
							FILTER_OPERATORS_NUMBER,
							0,
							Integer.MAX_VALUE,
							60),

					new TourFilterFieldConfig(
							Messages.Tour_Filter_Field_TourTitle,
							TourFilterFieldId.TOUR_TITLE,
							TourFilterFieldType.TEXT,
							FILTER_OPERATORS_TEXT),

					new TourFilterFieldConfig(
							Messages.Tour_Filter_Field_Temperature,
							TourFilterFieldId.TEMPERATURE,
							TourFilterFieldType.NUMBER,
							FILTER_OPERATORS_NUMBER,
							-600,
							1500,
							10), };

		FILTER_FIELD_CONFIG = CONFIG;
	}

// SET_FORMATTING_OFF
	
	private static final Bundle					_bundle				= TourbookPlugin.getDefault().getBundle();

	private static final IDialogSettings		_state				= TourbookPlugin.getState("net.tourbook.tour.filter.TourFilterManager"); //$NON-NLS-1$
	private static final IPath					_stateLocation		= Platform.getStateLocation(_bundle);
	private final static IPreferenceStore		_prefStore			= TourbookPlugin.getPrefStore();

	
// SET_FORMATTING_ON

	private static ArrayList<TourFilterProfile>	_filterProfiles	= new ArrayList<>();

	private static ActionTourFilter				_actionTourFilter;

	private static TourFilterProfile			_selectedProfile;

	/**
	 * Fire event that the tour filter has changed.
	 */
	private static void fireTourFilterModifyEvent() {

		_prefStore.setValue(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED, Math.random());
	}

	/**
	 * @param filterField
	 * @return Returns the configuration for a filter field.
	 */
	static TourFilterFieldConfig getFieldConfig(final TourFilterFieldId filterField) {

		for (final TourFilterFieldConfig fieldConfig : FILTER_FIELD_CONFIG) {

			if (filterField == fieldConfig.fieldId) {
				return fieldConfig;
			}
		}

		// this should not happen
		return null;
	}

	static TourFilterFieldOperator getFieldOperator(final TourFilterFieldId filterField, final int operatorIndex) {

		final TourFilterFieldOperator[] fieldOperators = getFieldOperators(filterField);

		return fieldOperators[operatorIndex];
	}

	static int getFieldOperatorIndex(final TourFilterFieldId filterField, final TourFilterFieldOperator fieldOperator) {

		final TourFilterFieldOperator[] fieldOperators = getFieldOperators(filterField);

		for (int operatorIndex = 0; operatorIndex < fieldOperators.length; operatorIndex++) {

			final TourFilterFieldOperator filterFieldOperator = fieldOperators[operatorIndex];

			if (fieldOperator == filterFieldOperator) {
				return operatorIndex;
			}
		}

		return 0;
	}

	static String getFieldOperatorName(final TourFilterFieldOperator filterOperator) {

		for (final TourFilterFieldOperatorConfig fieldOperatorConfig : TOUR_FILTER_OPERATORS) {
			if (filterOperator == fieldOperatorConfig.fieldOperator) {
				return fieldOperatorConfig.name;
			}
		}

		// this should not happen
		return null;
	}

	static TourFilterFieldOperator[] getFieldOperators(final TourFilterFieldId filterField) {

		for (final TourFilterFieldConfig fieldConfig : FILTER_FIELD_CONFIG) {
			if (filterField == fieldConfig.fieldId) {
				return fieldConfig.fieldOperators;
			}
		}

		// this should not happen
		return null;
	}

	/**
	 * @param filterField
	 * @return Returns the index of the requested filter type.
	 */
	static int getFilterFieldIndex(final TourFilterFieldId filterField) {

		for (int typeIndex = 0; typeIndex < FILTER_FIELD_CONFIG.length; typeIndex++) {

			final TourFilterFieldConfig filterTemplate = FILTER_FIELD_CONFIG[typeIndex];

			if (filterTemplate.fieldId.equals(filterField)) {
				return typeIndex;
			}
		}

		return 0;
	}

	static ArrayList<TourFilterProfile> getProfiles() {
		return _filterProfiles;
	}

	static TourFilterProfile getSelectedProfile() {
		return _selectedProfile;
	}

	private static File getXmlFile() {

		final File layerFile = _stateLocation.append(TOUR_FILTER_FILE_NAME).toFile();

		return layerFile;
	}

	/**
	 * Read filter profile xml file.
	 * 
	 * @return
	 */
	private static void readFilterProfile() {

		final File xmlFile = getXmlFile();

		if (xmlFile.exists()) {

			try (InputStreamReader reader = new InputStreamReader(new FileInputStream(xmlFile), UI.UTF_8)) {

				final XMLMemento xmlRoot = XMLMemento.createReadRoot(reader);
				for (final IMemento mementoChild : xmlRoot.getChildren()) {

					final XMLMemento xmlProfile = (XMLMemento) mementoChild;
					if (TAG_PROFILE.equals(xmlProfile.getType())) {

						final TourFilterProfile tourFilterProfile = new TourFilterProfile();

						tourFilterProfile.name = Util.getXmlString(xmlProfile, ATTR_NAME, UI.EMPTY_STRING);

						_filterProfiles.add(tourFilterProfile);

						// set selected profile
						if (Util.getXmlBoolean(xmlProfile, ATTR_IS_SELECTED, false)) {
							_selectedProfile = tourFilterProfile;
						}

						// loop: all properties
						for (final IMemento mementoProperty : xmlProfile.getChildren(TAG_PROPERTY)) {

							final XMLMemento xmlProperty = (XMLMemento) mementoProperty;

							final TourFilterFieldId fieldId = (TourFilterFieldId) Util.getXmlEnum(//
									xmlProperty,
									ATTR_FIELD_ID,
									TourFilterFieldId.TOUR_DATE);

							final TourFilterFieldOperator fieldOperator = (TourFilterFieldOperator) Util.getXmlEnum(//
									xmlProperty,
									ATTR_FIELD_OPERATOR,
									TourFilterFieldOperator.EQUALS);

							final TourFilterFieldConfig fieldConfig = getFieldConfig(fieldId);

							final TourFilterProperty filterProperty = new TourFilterProperty();

							filterProperty.fieldConfig = fieldConfig;
							filterProperty.fieldOperator = fieldOperator;
							filterProperty.isEnabled = Util.getXmlBoolean(xmlProperty, ATTR_IS_ENABLED, true);

							readFilterProfile_10_PropertyDetail(xmlProperty, filterProperty);

							tourFilterProfile.filterProperties.add(filterProperty);
						}
					}
				}

			} catch (final Exception e) {
				StatusUtil.log(e);
			}
		}
	}

	private static void readFilterProfile_10_PropertyDetail(final XMLMemento xmlProperty,
															final TourFilterProperty filterProperty) {

		final TourFilterFieldConfig fieldConfig = filterProperty.fieldConfig;
		final TourFilterFieldOperator fieldOperator = filterProperty.fieldOperator;
		final TourFilterFieldType fieldType = fieldConfig.fieldType;

		switch (fieldOperator) {
		case GREATER_THAN:
		case GREATER_THAN_OR_EQUAL:
		case LESS_THAN:
		case LESS_THAN_OR_EQUAL:
		case EQUALS:
		case NOT_EQUALS:

			switch (fieldType) {
			case DATE:
				readXml_Date(xmlProperty, filterProperty, 1);
				break;

			case TIME:
				readXml_Time(xmlProperty, filterProperty, 1);
				break;

			case DURATION:
				readXml_Integer(xmlProperty, filterProperty, 1);
				break;

			case NUMBER:

				break;

			case TEXT:

				break;

			case SEASON:
				readXml_Season(xmlProperty, filterProperty, 1);
				break;
			default:
				break;
			}

			break;

		case BETWEEN:
		case NOT_BETWEEN:

			switch (fieldType) {
			case DATE:

				readXml_Date(xmlProperty, filterProperty, 1);
				readXml_Date(xmlProperty, filterProperty, 2);

				break;

			case TIME:

				readXml_Time(xmlProperty, filterProperty, 1);
				readXml_Time(xmlProperty, filterProperty, 2);
				break;

			case DURATION:
				readXml_Integer(xmlProperty, filterProperty, 1);
				readXml_Integer(xmlProperty, filterProperty, 2);
				break;

			case NUMBER:

				break;

			case TEXT:

				break;

			case SEASON:

				readXml_Season(xmlProperty, filterProperty, 1);
				readXml_Season(xmlProperty, filterProperty, 2);
				break;
			}

			break;

		case STARTS_WITH:
			break;
		case ENDS_WITH:
			break;

		case INCLUDE_ANY:
			break;
		case EXCLUDE_ALL:
			break;

		case IS_EMPTY:
			break;
		case IS_NOT_EMPTY:
			break;

		case LIKE:
			break;
		case NOT_LIKE:
			break;
		}
	}

	private static void readXml_Date(	final IMemento xmlProperty,
										final TourFilterProperty filterProperty,
										final int fieldNo) {

		final LocalDateTime defaultDate = fieldNo == 1
				? TourFilterProperty.DEFAULT_DATE_1
				: TourFilterProperty.DEFAULT_DATE_2;

		final int year = Util.getXmlInteger(xmlProperty, ATTR_DATE_YEAR + fieldNo, defaultDate.getYear());
		final int month = Util.getXmlInteger(xmlProperty, ATTR_DATE_MONTH + fieldNo, defaultDate.getMonthValue());
		final int day = Util.getXmlInteger(xmlProperty, ATTR_DATE_DAY + fieldNo, defaultDate.getDayOfMonth());

		final LocalDateTime date = LocalDateTime.of(year, month, day, 0, 0);

		if (fieldNo == 1) {
			filterProperty.dateTime1 = date;
		} else {
			filterProperty.dateTime2 = date;
		}
	}

	private static void readXml_Integer(final IMemento xmlProperty,
										final TourFilterProperty filterProperty,
										final int fieldNo) {

		final int value = Util.getXmlInteger(xmlProperty, ATTR_VALUE + fieldNo, 0);

		if (fieldNo == 1) {
			filterProperty.number1 = value;
		} else {
			filterProperty.number2 = value;
		}
	}

	private static void readXml_Season(	final IMemento xmlProperty,
										final TourFilterProperty filterProperty,
										final int fieldNo) {

		final MonthDay defaultSeason = fieldNo == 1
				? TourFilterProperty.DEFAULT_SEASON_1
				: TourFilterProperty.DEFAULT_SEASON_2;

		final int month = Util.getXmlInteger(xmlProperty, ATTR_SEASON_MONTH + fieldNo, defaultSeason.getMonthValue());
		final int day = Util.getXmlInteger(xmlProperty, ATTR_SEASON_DAY + fieldNo, defaultSeason.getDayOfMonth());

		final MonthDay monthDay = MonthDay.of(month, day);

		if (fieldNo == 1) {
			filterProperty.monthDay1 = monthDay;
		} else {
			filterProperty.monthDay2 = monthDay;
		}
	}

	private static void readXml_Time(	final IMemento xmlProperty,
										final TourFilterProperty filterProperty,
										final int fieldNo) {

		final LocalDateTime defaultTime = fieldNo == 1
				? TourFilterProperty.DEFAULT_DATE_1
				: TourFilterProperty.DEFAULT_DATE_2;

		final int hour = Util.getXmlInteger(xmlProperty, ATTR_TIME_HOUR + fieldNo, defaultTime.getHour());
		final int minute = Util.getXmlInteger(xmlProperty, ATTR_TIME_MINUTE + fieldNo, defaultTime.getMinute());

		final LocalDateTime date = LocalDateTime.now().withHour(hour).withMinute(minute);

		if (fieldNo == 1) {
			filterProperty.dateTime1 = date;
		} else {
			filterProperty.dateTime2 = date;
		}
	}

	public static void restoreState() {

		final boolean isTourFilterActive = _prefStore.getBoolean(ITourbookPreferences.APP_TOUR_FILTER_IS_SELECTED);

		_actionTourFilter.setSelection(isTourFilterActive);

		readFilterProfile();
	}

	public static void saveState() {

		_prefStore.setValue(ITourbookPreferences.APP_TOUR_FILTER_IS_SELECTED, _actionTourFilter.getSelection());

		final XMLMemento xmlRoot = writeFilterProfile();
		final File xmlFile = getXmlFile();

		Util.writeXml(xmlRoot, xmlFile);
	}

	static void setSelectedProfile(final TourFilterProfile selectedProfile) {
		_selectedProfile = selectedProfile;
	}

	/**
	 * Sets the state if the tour filter is active or not.
	 * 
	 * @param isSelected
	 */
	public static void setSelection(final boolean isSelected) {

		fireTourFilterModifyEvent();

	}

	public static void setTourFilterAction(final ActionTourFilter actionTourFilter) {
		_actionTourFilter = actionTourFilter;
	}

	/**
	 * @return
	 */
	private static XMLMemento writeFilterProfile() {

		XMLMemento xmlRoot = null;

		try {

			xmlRoot = writeFilterProfile_10_Root();

			// loop: profiles
			for (final TourFilterProfile tourFilterProfile : _filterProfiles) {

				final IMemento xmlProfile = xmlRoot.createChild(TAG_PROFILE);

				xmlProfile.putString(ATTR_NAME, tourFilterProfile.name);

				// set flag for active profile
				if (tourFilterProfile == _selectedProfile) {
					xmlProfile.putBoolean(ATTR_IS_SELECTED, true);
				}

				// loop: properties
				for (final TourFilterProperty filterProperty : tourFilterProfile.filterProperties) {

					final TourFilterFieldConfig fieldConfig = filterProperty.fieldConfig;
					final TourFilterFieldOperator fieldOperator = filterProperty.fieldOperator;

					final IMemento xmlProperty = xmlProfile.createChild(TAG_PROPERTY);

					Util.setXmlEnum(xmlProperty, ATTR_FIELD_ID, fieldConfig.fieldId);
					Util.setXmlEnum(xmlProperty, ATTR_FIELD_OPERATOR, fieldOperator);
					xmlProperty.putBoolean(ATTR_IS_ENABLED, filterProperty.isEnabled);

					writeFilterProfile_20_PropertyDetail(xmlProperty, filterProperty);
				}
			}

		} catch (final Exception e) {
			StatusUtil.log(e);
		}

		return xmlRoot;
	}

	private static XMLMemento writeFilterProfile_10_Root() {

		final XMLMemento xmlRoot = XMLMemento.createWriteRoot(TAG_ROOT);

		// date/time
		xmlRoot.putString(Util.ATTR_ROOT_DATETIME, TimeTools.now().toString());

		// plugin version
		final Version version = _bundle.getVersion();
		xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MAJOR, version.getMajor());
		xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MINOR, version.getMinor());
		xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MICRO, version.getMicro());
		xmlRoot.putString(Util.ATTR_ROOT_VERSION_QUALIFIER, version.getQualifier());

		// layer structure version
		xmlRoot.putInteger(ATTR_TOUR_FILTER_VERSION, TOUR_FILTER_VERSION);

		return xmlRoot;
	}

	private static void writeFilterProfile_20_PropertyDetail(	final IMemento xmlProperty,
																final TourFilterProperty filterProperty) {

		final TourFilterFieldConfig fieldConfig = filterProperty.fieldConfig;
		final TourFilterFieldOperator fieldOperator = filterProperty.fieldOperator;
		final TourFilterFieldType fieldType = fieldConfig.fieldType;

		final LocalDateTime dateTime1 = filterProperty.dateTime1;
		final LocalDateTime dateTime2 = filterProperty.dateTime2;

		final MonthDay monthDay1 = filterProperty.monthDay1;
		final MonthDay monthDay2 = filterProperty.monthDay2;

		final int number1 = filterProperty.number1;
		final int number2 = filterProperty.number2;

		switch (fieldOperator) {
		case GREATER_THAN:
		case GREATER_THAN_OR_EQUAL:
		case LESS_THAN:
		case LESS_THAN_OR_EQUAL:
		case EQUALS:
		case NOT_EQUALS:

			switch (fieldType) {
			case DATE:
				writeXml_Date(xmlProperty, dateTime1, 1);
				break;

			case TIME:
				writeXml_Time(xmlProperty, dateTime1, 1);
				break;

			case DURATION:
			case NUMBER:
				writeXml_Number(xmlProperty, number1, 1);
				break;

			case TEXT:

				break;

			case SEASON:
				writeXml_Season(xmlProperty, monthDay1, 1);
				break;
			default:
				break;
			}

			break;

		case BETWEEN:
		case NOT_BETWEEN:

			switch (fieldType) {
			case DATE:

				writeXml_Date(xmlProperty, dateTime1, 1);
				writeXml_Date(xmlProperty, dateTime2, 2);

				break;

			case TIME:

				writeXml_Time(xmlProperty, dateTime1, 1);
				writeXml_Time(xmlProperty, dateTime2, 2);
				break;

			case DURATION:
			case NUMBER:
				writeXml_Number(xmlProperty, number1, 1);
				writeXml_Number(xmlProperty, number2, 2);
				break;

			case TEXT:

				break;

			case SEASON:

				writeXml_Season(xmlProperty, monthDay1, 1);
				writeXml_Season(xmlProperty, monthDay2, 2);
				break;
			}

			break;

		case STARTS_WITH:
			break;
		case ENDS_WITH:
			break;

		case INCLUDE_ANY:
			break;
		case EXCLUDE_ALL:
			break;

		case IS_EMPTY:
			break;
		case IS_NOT_EMPTY:
			break;

		case LIKE:
			break;
		case NOT_LIKE:
			break;
		}
	}

	private static void writeXml_Date(final IMemento xmlProperty, final LocalDateTime dateTime, final int fieldNo) {

		xmlProperty.putInteger(ATTR_DATE_YEAR + fieldNo, dateTime.getYear());
		xmlProperty.putInteger(ATTR_DATE_MONTH + fieldNo, dateTime.getMonthValue());
		xmlProperty.putInteger(ATTR_DATE_DAY + fieldNo, dateTime.getDayOfMonth());
	}

	private static void writeXml_Number(final IMemento xmlProperty, final int number, final int fieldNo) {

		xmlProperty.putInteger(ATTR_VALUE + fieldNo, number);
	}

	private static void writeXml_Season(final IMemento xmlProperty, final MonthDay monthDay, final int fieldNo) {

		xmlProperty.putInteger(ATTR_SEASON_MONTH + fieldNo, monthDay.getMonthValue());
		xmlProperty.putInteger(ATTR_SEASON_DAY + fieldNo, monthDay.getDayOfMonth());
	}

	private static void writeXml_Time(final IMemento xmlProperty, final LocalDateTime dateTime, final int fieldNo) {

		xmlProperty.putInteger(ATTR_TIME_HOUR + fieldNo, dateTime.getHour());
		xmlProperty.putInteger(ATTR_TIME_MINUTE + fieldNo, dateTime.getMinute());
	}

}
