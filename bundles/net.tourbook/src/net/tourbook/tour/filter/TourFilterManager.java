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
import java.math.BigDecimal;
import java.time.LocalDate;
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
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

public class TourFilterManager {

	private static final String							TOUR_FILTER_FILE_NAME			= "tour-filter.xml";													//$NON-NLS-1$
	private static final int							TOUR_FILTER_VERSION				= 1;

	private static final String							TAG_PROFILE						= "Profile";															//$NON-NLS-1$
	private static final String							TAG_PROPERTY					= "Property";															//$NON-NLS-1$
	private static final String							TAG_ROOT						= "TourFilterProfiles";													//$NON-NLS-1$

	private static final String							ATTR_IS_ENABLED					= "isEnabled";															//$NON-NLS-1$
	private static final String							ATTR_IS_SELECTED				= "isSelected";															//$NON-NLS-1$
	private static final String							ATTR_FIELD_ID					= "fieldId";															//$NON-NLS-1$
	private static final String							ATTR_FIELD_OPERATOR				= "fieldOperator";														//$NON-NLS-1$
	private static final String							ATTR_NAME						= "name";																//$NON-NLS-1$
	private static final String							ATTR_SEASON_DAY					= "seasonDay";															//$NON-NLS-1$
	private static final String							ATTR_SEASON_MONTH				= "seasonMonth";														//$NON-NLS-1$
	private static final String							ATTR_TOUR_FILTER_VERSION		= "tourFilterVersion";													//$NON-NLS-1$
	private static final String							ATTR_VALUE						= "value";																//$NON-NLS-1$

	private static final String							ATTR_DATE_YEAR					= "dateYear";															//$NON-NLS-1$
	private static final String							ATTR_DATE_MONTH					= "dateMonth";															//$NON-NLS-1$
	private static final String							ATTR_DATE_DAY					= "dateDay";															//$NON-NLS-1$
	private static final String							ATTR_TIME_HOUR					= "timeHour";															//$NON-NLS-1$
	private static final String							ATTR_TIME_MINUTE				= "timeMinute";															//$NON-NLS-1$

	private static final String							OP_BR_OPEN						= "(";																	//$NON-NLS-1$
	private static final String							OP_BR_CLOSE						= ")";																	//$NON-NLS-1$
	private static final String							OP_AND							= " AND ";																//$NON-NLS-1$
	private static final String							OP_BETWEEN						= " BETWEEN ";															//$NON-NLS-1$
	private static final String							OP_NOT_BETWEEN					= " NOT BETWEEN ";														//$NON-NLS-1$
	private static final String							OP_OR							= " OR ";																//$NON-NLS-1$

	private static final String							OP_PARAMETER					= " ?\n";																//$NON-NLS-1$
	private static final String							OP_EQUALS						= " = ?\n";																//$NON-NLS-1$
	private static final String							OP_NOT_EQUALS					= " != ?\n";															//$NON-NLS-1$
	private static final String							OP_GREATER_THAN					= " > ?\n";																//$NON-NLS-1$
	private static final String							OP_GREATER_THAN_OR_EQUAL		= " >= ?\n";															//$NON-NLS-1$
	private static final String							OP_LESS_THAN					= " < ?\n";																//$NON-NLS-1$
	private static final String							OP_LESS_THAN_OR_EQUAL			= " <= ?\n";															//$NON-NLS-1$

	private static final String							OP_NULL							= " IS NULL\n";															//$NON-NLS-1$
	private static final String							OP_NOT_NULL						= " IS NOT NULL\n";														//$NON-NLS-1$

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
	   
	   new TourFilterFieldOperatorConfig(TourFilterFieldOperator.SEASON_YEAR_START_UNTIL_TODAY,	Messages.Tour_Filter_Operator_Season_YearStart_Until_Today),
	   new TourFilterFieldOperatorConfig(TourFilterFieldOperator.SEASON_DATE_UNTIL_TODAY,		Messages.Tour_Filter_Operator_Season_Date_Until_Today),
	   new TourFilterFieldOperatorConfig(TourFilterFieldOperator.SEASON_TODAY_UNTIL_YEAR_END,	Messages.Tour_Filter_Operator_Season_Today_Until_YearEnd),
	   new TourFilterFieldOperatorConfig(TourFilterFieldOperator.SEASON_TODAY_UNTIL_DATE,		Messages.Tour_Filter_Operator_Season_Today_Until_Date),

// is not yet implemented
//
//	   new TourFilterOperatorDef(TourFilterOperator.LIKE,					Messages.Tour_Filter_Operator_Like),
//	   new TourFilterOperatorDef(TourFilterOperator.NOT_LIKE,				Messages.Tour_Filter_Operator_NotLike),
//	   new TourFilterOperatorDef(TourFilterOperator.INCLUDE_ANY,			Messages.Tour_Filter_Operator_IncludeAny),
//	   new TourFilterOperatorDef(TourFilterOperator.EXCLUDE_ALL,			Messages.Tour_Filter_Operator_ExcludeAll),
	};
	
	public static final TourFilterFieldOperator[]			FILTER_OPERATORS_DATE_TIME = {
	                                        	                        
	   TourFilterFieldOperator.LESS_THAN,
	   TourFilterFieldOperator.LESS_THAN_OR_EQUAL,
	   TourFilterFieldOperator.GREATER_THAN,
	   TourFilterFieldOperator.GREATER_THAN_OR_EQUAL,
	   TourFilterFieldOperator.EQUALS,
	   TourFilterFieldOperator.NOT_EQUALS,
	   TourFilterFieldOperator.BETWEEN,
	   TourFilterFieldOperator.NOT_BETWEEN,
	};

	public static final TourFilterFieldOperator[]			FILTER_OPERATORS_NUMBER = {
	                                        	                         
		TourFilterFieldOperator.LESS_THAN,
		TourFilterFieldOperator.LESS_THAN_OR_EQUAL,
		TourFilterFieldOperator.GREATER_THAN,
		TourFilterFieldOperator.GREATER_THAN_OR_EQUAL,
		TourFilterFieldOperator.EQUALS,
		TourFilterFieldOperator.NOT_EQUALS,
		TourFilterFieldOperator.BETWEEN,
		TourFilterFieldOperator.NOT_BETWEEN,
	};

	public static final TourFilterFieldOperator[]			FILTER_OPERATORS_SEASON = {
	                                             			                         
		TourFilterFieldOperator.SEASON_YEAR_START_UNTIL_TODAY,
		TourFilterFieldOperator.SEASON_TODAY_UNTIL_YEAR_END,
		TourFilterFieldOperator.SEASON_DATE_UNTIL_TODAY,
		TourFilterFieldOperator.SEASON_TODAY_UNTIL_DATE,
		TourFilterFieldOperator.BETWEEN,
		TourFilterFieldOperator.NOT_BETWEEN,
	};
	
	public static final TourFilterFieldOperator[]			FILTER_OPERATORS_TEXT = {
	                                             			                         
		TourFilterFieldOperator.IS_EMPTY,
		TourFilterFieldOperator.IS_NOT_EMPTY,
	};

// SET_FORMATTING_ON

	private static FieldValueProvider					_fieldValueProvider_distance	= new FieldValueProvider_Distance();
	private static FieldValueProvider					_fieldValueProvider_temperature	= new FieldValueProvider_Temperature();

	/**
	 * This is also the sequence how the fields are displayed in the UI
	 */
	public static final TourFilterFieldConfig[]			FILTER_FIELD_CONFIG;

	static {

//		public static String		ColumnFactory_Category_Altitude;
//		public static String		ColumnFactory_Category_Body;
//		public static String		ColumnFactory_Category_Data;
//		public static String		ColumnFactory_Category_Device;
//		public static String		ColumnFactory_Category_Marker;
//		public static String		ColumnFactory_Category_Motion;
//		public static String		ColumnFactory_Category_Photo;
//		public static String		ColumnFactory_Category_Power;
//		public static String		ColumnFactory_Category_Powertrain;
//		public static String		ColumnFactory_Category_State;
//		public static String		ColumnFactory_Category_Time;
//		public static String		ColumnFactory_Category_Tour;
//		public static String		ColumnFactory_Category_Training;
//		public static String		ColumnFactory_Category_Waypoint;
//		public static String		ColumnFactory_Category_Weather;

		final TourFilterFieldConfig[] CONFIG =

				{
					new TourFilterFieldConfig(
							Messages.Tour_Filter_Field_TourDate,
							TourFilterFieldId.TOUR_DATE,
							TourFilterFieldType.DATE,
							FILTER_OPERATORS_DATE_TIME),

					new TourFilterFieldConfig(
							Messages.Tour_Filter_Field_TourStartTime,
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
							TourFilterFieldType.NUMBER_METRIC,
							FILTER_OPERATORS_NUMBER,
							-600,
							1500,
							10,
							1,
							_fieldValueProvider_temperature),

					new TourFilterFieldConfig(
							Messages.Tour_Filter_Field_Distance,
							TourFilterFieldId.DISTANCE,
							TourFilterFieldType.NUMBER_METRIC,
							FILTER_OPERATORS_NUMBER,
							0,
							Integer.MAX_VALUE,
							100,
							1,
							_fieldValueProvider_distance),
				//
				};

		FILTER_FIELD_CONFIG = CONFIG;
	}

// SET_FORMATTING_OFF
	
	private static final Bundle					_bundle				= TourbookPlugin.getDefault().getBundle();

	private static final IPath					_stateLocation		= Platform.getStateLocation(_bundle);
	private final static IPreferenceStore		_prefStore			= TourbookPlugin.getPrefStore();
	
// SET_FORMATTING_ON

	private static IPropertyChangeListener	_prefChangeListener;
	static {

		_prefChangeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					updateUnits();
				}
			}
		};

		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	/**
	 * Contains all available profiles.
	 */
	private static ArrayList<TourFilterProfile>	_filterProfiles		= new ArrayList<>();

	/**
	 * Contains the selected profile or <code>null</code> when a profile is not selected.
	 */
	private static TourFilterProfile			_selectedProfile;

	private static boolean						_isTourFilterEnabled;

	private static int[]						_fireEventCounter	= new int[1];

	private static ActionTourFilter				_actionTourFilter;

	/**
	 * Fire event that the tour filter has changed.
	 */
	static void fireTourFilterModifyEvent() {

		_fireEventCounter[0]++;

		Display.getDefault().asyncExec(new Runnable() {

			final int __runnableCounter = _fireEventCounter[0];

			@Override
			public void run() {

				// skip all events which has not yet been executed
				if (__runnableCounter != _fireEventCounter[0]) {

					// a new event occured
					return;
				}

				_prefStore.setValue(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED, Math.random());
			}
		});

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

	/**
	 * @return Returns the selected profile or <code>null</code> when a profile is not selected.
	 */
	public static TourFilterProfile getSelectedProfile() {
		return _selectedProfile;
	}

	/**
	 * @return Returns sql data for the selected tour filter profile or <code>null</code> when not
	 *         available.
	 */
	public static TourFilterSQLData getSQL() {

		if (_isTourFilterEnabled == false || _selectedProfile == null) {

			// tour filter is not enabled or not selected

			return null;
		}

		final LocalDate today = LocalDate.now();

		final StringBuilder sqlWhere = new StringBuilder();
		final ArrayList<Object> sqlParameters = new ArrayList<>();

		for (final TourFilterProperty filterProperty : _selectedProfile.filterProperties) {

			if (filterProperty.isEnabled == false) {
				continue;
			}

			final TourFilterFieldConfig fieldConfig = filterProperty.fieldConfig;
			final TourFilterFieldOperator fieldOperator = filterProperty.fieldOperator;

			final TourFilterFieldId fieldId = fieldConfig.fieldId;

			final LocalDateTime dateTime1 = filterProperty.dateTime1;
			final LocalDateTime dateTime2 = filterProperty.dateTime2;

			final Integer int1 = filterProperty.intValue1;
			final Integer int2 = filterProperty.intValue2;
			final Double double1 = truncateValue(fieldConfig, filterProperty.doubleValue1);
			final Double double2 = truncateValue(fieldConfig, filterProperty.doubleValue2);

			final String text1 = filterProperty.textValue1;
			final String text2 = filterProperty.textValue2;

			String sql;

			long value1;
			long value2;

			switch (fieldId) {
			case TOUR_DATE:

				sql = "TourData.tourStartTime"; //$NON-NLS-1$

				value1 = LocalDate
						.of(dateTime1.getYear(), dateTime1.getMonthValue(), dateTime1.getDayOfMonth())
						.toEpochDay() * 86400_000;

				value2 = LocalDate
						.of(dateTime2.getYear(), dateTime2.getMonthValue(), dateTime2.getDayOfMonth())
						.toEpochDay() * 86400_000;

				getSQL__FieldOperators_DateTime(sqlWhere, sqlParameters, fieldOperator, sql, value1, value2);
				break;

			case TOUR_TIME:

				sql = "(TourData.startHour * 3600 + TourData.startMinute)"; //$NON-NLS-1$

				value1 = dateTime1.getHour() * 3600 + dateTime1.getMinute();
				value2 = dateTime2.getHour() * 3600 + dateTime2.getMinute();

				getSQL__FieldOperators_DateTime(sqlWhere, sqlParameters, fieldOperator, sql, value1, value2);
				break;

			case SEASON_DATE:
				getSQL__FieldOperators_SeasonDate(sqlWhere, sqlParameters, filterProperty, today);
				break;

			case BREAK_TIME:
				sql = "(TourData.tourRecordingTime - TourData.tourDrivingTime)"; //$NON-NLS-1$
				getSQL__FieldOperators_Number(sqlWhere, sqlParameters, fieldOperator, sql, int1, int2);
				break;

			case DRIVING_TIME:
				sql = "TourData.tourDrivingTime"; //$NON-NLS-1$
				getSQL__FieldOperators_Number(sqlWhere, sqlParameters, fieldOperator, sql, int1, int2);

				break;

			case RECORDING_TIME:
				sql = "TourData.tourRecordingTime"; //$NON-NLS-1$
				getSQL__FieldOperators_Number(sqlWhere, sqlParameters, fieldOperator, sql, int1, int2);
				break;

			case DISTANCE:

				sql = "TourData.tourDistance"; //$NON-NLS-1$

				// convert from km to m
				final double distance1 = double1 * 1000;
				final double distance2 = double2 * 1000;

				getSQL__FieldOperators_Number(sqlWhere, sqlParameters, fieldOperator, sql, distance1, distance2);
				break;

			case TEMPERATURE:
				sql = "TourData.avgTemperature"; //$NON-NLS-1$
				getSQL__FieldOperators_Number(sqlWhere, sqlParameters, fieldOperator, sql, double1, double2);
				break;

			case TOUR_TITLE:
				sql = "TourData.tourTitle"; //$NON-NLS-1$
				getSQL__FieldOperators_Text(sqlWhere, sqlParameters, fieldOperator, sql, text1, text2);
				break;
			}
		}

		final TourFilterSQLData tourFilterSQLData = new TourFilterSQLData(sqlWhere.toString(), sqlParameters);

//		System.out.println((UI.timeStampNano() + " [" + "] ") + ("\ttourFilterSQLData: " + tourFilterSQLData));
//		// TODO remove SYSTEM.OUT.PRINTLN

		return tourFilterSQLData;
	}

	private static void getSQL__FieldOperators_DateTime(final StringBuilder sqlWhere,
														final ArrayList<Object> sqlParameters,
														final TourFilterFieldOperator fieldOperator,
														final String sqlField,
														final Long value1,
														final Long value2) {

		switch (fieldOperator) {
		case LESS_THAN:
			getSQL_LessThan(sqlWhere, sqlParameters, sqlField, value1);
			break;
		case LESS_THAN_OR_EQUAL:
			getSQL_LessThanOrEqual(sqlWhere, sqlParameters, sqlField, value1);
			break;

		case GREATER_THAN:
			getSQL_GreaterThan(sqlWhere, sqlParameters, sqlField, value1);
			break;
		case GREATER_THAN_OR_EQUAL:
			getSQL_GreaterThanOrEqual(sqlWhere, sqlParameters, sqlField, value1);
			break;

		case EQUALS:
			getSQL_Equals(sqlWhere, sqlParameters, sqlField, value1, true);
			break;
		case NOT_EQUALS:
			getSQL_Equals(sqlWhere, sqlParameters, sqlField, value1, false);
			break;

		case BETWEEN:
			getSQL_Between(sqlWhere, sqlParameters, sqlField, value1, value2, true);
			break;
		case NOT_BETWEEN:
			getSQL_Between(sqlWhere, sqlParameters, sqlField, value1, value2, false);
			break;
		}
	}

	private static void getSQL__FieldOperators_Number(	final StringBuilder sqlWhere,
														final ArrayList<Object> sqlParameters,
														final TourFilterFieldOperator fieldOperator,
														final String sqlField,
														final Object value1,
														final Object value2) {

		switch (fieldOperator) {
		case LESS_THAN:
			getSQL_LessThan(sqlWhere, sqlParameters, sqlField, value1);
			break;
		case LESS_THAN_OR_EQUAL:
			getSQL_LessThanOrEqual(sqlWhere, sqlParameters, sqlField, value1);
			break;

		case GREATER_THAN:
			getSQL_GreaterThan(sqlWhere, sqlParameters, sqlField, value1);
			break;
		case GREATER_THAN_OR_EQUAL:
			getSQL_GreaterThanOrEqual(sqlWhere, sqlParameters, sqlField, value1);
			break;

		case EQUALS:
			getSQL_Equals(sqlWhere, sqlParameters, sqlField, value1, true);
			break;
		case NOT_EQUALS:
			getSQL_Equals(sqlWhere, sqlParameters, sqlField, value1, false);
			break;

		case BETWEEN:
			getSQL_Between(sqlWhere, sqlParameters, sqlField, value1, value2, true);
			break;
		case NOT_BETWEEN:
			getSQL_Between(sqlWhere, sqlParameters, sqlField, value1, value2, false);
			break;
		}
	}

	private static void getSQL__FieldOperators_SeasonDate(	final StringBuilder sqlWhere,
															final ArrayList<Object> sqlParameters,
															final TourFilterProperty filterProperty,
															final LocalDate today) {

		final int todayDay = today.getDayOfMonth();
		final int todayMonth = today.getMonthValue();

		final int todayValue = todayMonth * 100 + todayDay;

		final MonthDay monthDay1 = filterProperty.monthDay1;
		final MonthDay monthDay2 = filterProperty.monthDay2;

		final int day1 = monthDay1.getDayOfMonth();
		final int day2 = monthDay2.getDayOfMonth();
		final int month1 = monthDay1.getMonthValue();
		final int month2 = monthDay2.getMonthValue();

		final int dateValue1 = month1 * 100 + day1;
		final int dateValue2 = month2 * 100 + day2;

		final String sql = "(TourData.startMonth * 100 + TourData.startDay)"; //$NON-NLS-1$

		switch (filterProperty.fieldOperator) {
		case SEASON_YEAR_START_UNTIL_TODAY:
			sqlWhere.append(OP_AND + sql + OP_LESS_THAN_OR_EQUAL);
			sqlParameters.add(todayValue);
			break;

		case SEASON_TODAY_UNTIL_YEAR_END:
			sqlWhere.append(OP_AND + sql + OP_GREATER_THAN_OR_EQUAL);
			sqlParameters.add(todayValue);
			break;

		case SEASON_DATE_UNTIL_TODAY:

			sqlWhere.append(OP_AND
					+ (sql + OP_GREATER_THAN_OR_EQUAL) // date
					+ OP_AND
					+ (sql + OP_LESS_THAN_OR_EQUAL) // today
			);

			sqlParameters.add(dateValue1);
			sqlParameters.add(todayValue);

			break;

		case SEASON_TODAY_UNTIL_DATE:

			sqlWhere.append(OP_AND
					+ (sql + OP_GREATER_THAN_OR_EQUAL) // date
					+ OP_AND
					+ (sql + OP_LESS_THAN_OR_EQUAL) // today
			);

			sqlParameters.add(todayValue);
			sqlParameters.add(dateValue1);

			break;

		case BETWEEN:

			sqlWhere.append(OP_AND

					+ sql
					+ OP_BETWEEN
					+ OP_PARAMETER
					+ OP_AND
					+ OP_PARAMETER);

			sqlParameters.add(dateValue1);
			sqlParameters.add(dateValue2);
			break;

		case NOT_BETWEEN:

			sqlWhere.append(OP_AND

					+ sql
					+ OP_NOT_BETWEEN
					+ OP_PARAMETER
					+ OP_AND
					+ OP_PARAMETER
			//
			);

			sqlParameters.add(dateValue1);
			sqlParameters.add(dateValue2);
			break;
		}
	}

	private static void getSQL__FieldOperators_Text(final StringBuilder sqlWhere,
													final ArrayList<Object> sqlParameters,
													final TourFilterFieldOperator fieldOperator,
													final String sqlField,
													final String value1,
													final String value2) {

		switch (fieldOperator) {
//		case LESS_THAN:
//			getSQL_LessThan(sqlWhere, sqlParameters, sqlField, value1);
//			break;
//		case LESS_THAN_OR_EQUAL:
//			getSQL_LessThanOrEqual(sqlWhere, sqlParameters, sqlField, value1);
//			break;
//
//		case GREATER_THAN:
//			getSQL_GreaterThan(sqlWhere, sqlParameters, sqlField, value1);
//			break;
//		case GREATER_THAN_OR_EQUAL:
//			getSQL_GreaterThanOrEqual(sqlWhere, sqlParameters, sqlField, value1);
//			break;
//
//		case EQUALS:
//			getSQL_Equals(sqlWhere, sqlParameters, sqlField, value1, true);
//			break;
//		case NOT_EQUALS:
//			getSQL_Equals(sqlWhere, sqlParameters, sqlField, value1, false);
//			break;

		case IS_EMPTY:
			getSQL_Empty(sqlWhere, sqlParameters, sqlField, true);
			break;
		case IS_NOT_EMPTY:
			getSQL_Empty(sqlWhere, sqlParameters, sqlField, false);
			break;

//		case BETWEEN:
//			getSQL_Between(sqlWhere, sqlParameters, sqlField, value1, value2, true);
//			break;
//		case NOT_BETWEEN:
//			getSQL_Between(sqlWhere, sqlParameters, sqlField, value1, value2, false);
//			break;
		}
	}

	private static void getSQL_Between(	final StringBuilder sqlWhere,
										final ArrayList<Object> sqlParameters,
										final String sqlField,
										final Object value1,
										final Object value2,
										final boolean isBetween) {

		final String op = isBetween ? OP_BETWEEN : OP_NOT_BETWEEN;

		sqlWhere.append(OP_AND

				+ sqlField
				+ op

				+ OP_PARAMETER
				+ OP_AND
				+ OP_PARAMETER
		//
		);

		sqlParameters.add(value1);
		sqlParameters.add(value2);
	}

	private static void getSQL_Empty(	final StringBuilder sqlWhere,
										final ArrayList<Object> sqlParameters,
										final String sqlField,
										final boolean isOp) {

		if (isOp) {

			sqlWhere.append(OP_AND //

					+ OP_BR_OPEN

					+ (sqlField + OP_NULL)
					+ OP_OR
					+ ("LENGTH(TRIM(" + sqlField + ")) = 0\n") //$NON-NLS-1$ //$NON-NLS-2$

					+ OP_BR_CLOSE);

		} else {

			sqlWhere.append(OP_AND //

					+ (sqlField + OP_NOT_NULL)
					+ OP_AND
					+ ("LENGTH(TRIM(" + sqlField + ")) > 0\n") //$NON-NLS-1$ //$NON-NLS-2$

			);
		}
	}

	private static void getSQL_Equals(	final StringBuilder sqlWhere,
										final ArrayList<Object> sqlParameters,
										final String sqlField,
										final Object value1,
										final boolean isOp) {

		if (isOp) {

			sqlWhere.append(OP_AND + sqlField + OP_EQUALS);

		} else {

			sqlWhere.append(OP_AND + sqlField + OP_NOT_EQUALS);
		}

		sqlParameters.add(value1);
	}

	private static void getSQL_GreaterThan(	final StringBuilder sqlWhere,
											final ArrayList<Object> sqlParameters,
											final String sqlField,
											final Object value1) {

		sqlWhere.append(OP_AND + sqlField + OP_GREATER_THAN);
		sqlParameters.add(value1);
	}

	private static void getSQL_GreaterThanOrEqual(	final StringBuilder sqlWhere,
													final ArrayList<Object> sqlParameters,
													final String sqlField,
													final Object value1) {

		sqlWhere.append(OP_AND + sqlField + OP_GREATER_THAN_OR_EQUAL);
		sqlParameters.add(value1);
	}

	private static void getSQL_LessThan(final StringBuilder sqlWhere,
										final ArrayList<Object> sqlParameters,
										final String sqlField,
										final Object value1) {

		sqlWhere.append(OP_AND + sqlField + OP_LESS_THAN);
		sqlParameters.add(value1);
	}

	private static void getSQL_LessThanOrEqual(	final StringBuilder sqlWhere,
												final ArrayList<Object> sqlParameters,
												final String sqlField,
												final Object value1) {

		sqlWhere.append(OP_AND + sqlField + OP_LESS_THAN_OR_EQUAL);
		sqlParameters.add(value1);
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
				readXml_Number_Integer(xmlProperty, filterProperty, 1);
				break;

			case NUMBER_INTEGER:
				readXml_Number_Integer(xmlProperty, filterProperty, 1);
				break;

			case NUMBER_METRIC:
				readXml_Number_Float(xmlProperty, filterProperty, 1);
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
				readXml_Number_Integer(xmlProperty, filterProperty, 1);
				readXml_Number_Integer(xmlProperty, filterProperty, 2);
				break;

			case NUMBER_INTEGER:
				readXml_Number_Integer(xmlProperty, filterProperty, 1);
				readXml_Number_Integer(xmlProperty, filterProperty, 2);
				break;

			case NUMBER_METRIC:
				readXml_Number_Float(xmlProperty, filterProperty, 1);
				readXml_Number_Float(xmlProperty, filterProperty, 2);
				break;

			case TEXT:
				break;

			case SEASON:
				readXml_Season(xmlProperty, filterProperty, 1);
				readXml_Season(xmlProperty, filterProperty, 2);
				break;
			}

			break;

		case SEASON_DATE_UNTIL_TODAY:
		case SEASON_TODAY_UNTIL_DATE:
			readXml_Season(xmlProperty, filterProperty, 1);
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

	private static void readXml_Number_Float(	final IMemento xmlProperty,
												final TourFilterProperty filterProperty,
												final int fieldNo) {

		final TourFilterFieldConfig fieldConfig = filterProperty.fieldConfig;

		final float value = Util.getXmlFloatFloat(

				(XMLMemento) xmlProperty,
				ATTR_VALUE + fieldNo,

				0f,
				fieldConfig.minValue,
				fieldConfig.maxValue);

		if (fieldNo == 1) {
			filterProperty.doubleValue1 = value;
		} else {
			filterProperty.doubleValue2 = value;
		}
	}

	private static void readXml_Number_Integer(	final IMemento xmlProperty,
												final TourFilterProperty filterProperty,
												final int fieldNo) {

		final int value = Util.getXmlInteger(xmlProperty, ATTR_VALUE + fieldNo, 0);

		if (fieldNo == 1) {
			filterProperty.intValue1 = value;
		} else {
			filterProperty.intValue2 = value;
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

		_isTourFilterEnabled = _prefStore.getBoolean(ITourbookPreferences.APP_TOUR_FILTER_IS_SELECTED);

		_actionTourFilter.setSelection(_isTourFilterEnabled);

		readFilterProfile();
	}

	public static void saveState() {

		_prefStore.setValue(ITourbookPreferences.APP_TOUR_FILTER_IS_SELECTED, _actionTourFilter.getSelection());

		final XMLMemento xmlRoot = writeFilterProfile();
		final File xmlFile = getXmlFile();

		Util.writeXml(xmlRoot, xmlFile);
	}

	/**
	 * Sets the state if the tour filter is active or not.
	 * 
	 * @param isEnabled
	 */
	public static void setFilterEnabled(final boolean isEnabled) {

		_isTourFilterEnabled = isEnabled;

		fireTourFilterModifyEvent();
	}

	static void setSelectedProfile(final TourFilterProfile selectedProfile) {

		_selectedProfile = selectedProfile;
	}

	public static void setTourFilterAction(final ActionTourFilter actionTourFilterAdv) {
		_actionTourFilter = actionTourFilterAdv;
	}

	private static double truncateValue(final TourFilterFieldConfig fieldConfig, final double doubleValue) {

		final int decimals = 5;

		return BigDecimal//
				.valueOf(doubleValue)
				.setScale(decimals, BigDecimal.ROUND_HALF_UP)
				.doubleValue();
	}

	public static void updateUnits() {

		TourFilterFieldConfig fieldConfig;

		// set label km or mi
		fieldConfig = getFieldConfig(TourFilterFieldId.DISTANCE);
		fieldConfig.unitLabel = UI.UNIT_LABEL_DISTANCE;

		// set label celcius or fahrenheit
		fieldConfig = getFieldConfig(TourFilterFieldId.TEMPERATURE);
		fieldConfig.unitLabel = UI.UNIT_LABEL_TEMPERATURE;
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

		final int intValue1 = filterProperty.intValue1;
		final int intValue2 = filterProperty.intValue2;

		final double doubleValue1 = filterProperty.doubleValue1;
		final double doubleValue2 = filterProperty.doubleValue2;

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
			case NUMBER_INTEGER:
				writeXml_Number_Integer(xmlProperty, intValue1, 1);
				break;

			case NUMBER_METRIC:
				writeXml_Number_Double(xmlProperty, doubleValue1, 1);
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
			case NUMBER_INTEGER:
				writeXml_Number_Integer(xmlProperty, intValue1, 1);
				writeXml_Number_Integer(xmlProperty, intValue2, 2);
				break;

			case NUMBER_METRIC:
				writeXml_Number_Double(xmlProperty, doubleValue1, 1);
				writeXml_Number_Double(xmlProperty, doubleValue2, 2);
				break;

			case SEASON:
				writeXml_Season(xmlProperty, monthDay1, 1);
				writeXml_Season(xmlProperty, monthDay2, 2);
				break;
			}

			break;

		case SEASON_DATE_UNTIL_TODAY:
		case SEASON_TODAY_UNTIL_DATE:
			writeXml_Season(xmlProperty, monthDay1, 1);
			break;
		}
	}

	private static void writeXml_Date(final IMemento xmlProperty, final LocalDateTime dateTime, final int fieldNo) {

		xmlProperty.putInteger(ATTR_DATE_YEAR + fieldNo, dateTime.getYear());
		xmlProperty.putInteger(ATTR_DATE_MONTH + fieldNo, dateTime.getMonthValue());
		xmlProperty.putInteger(ATTR_DATE_DAY + fieldNo, dateTime.getDayOfMonth());
	}

	private static void writeXml_Number_Double(final IMemento xmlProperty, final double value, final int fieldNo) {

		xmlProperty.putFloat(ATTR_VALUE + fieldNo, (float) value);
	}

	private static void writeXml_Number_Integer(final IMemento xmlProperty, final int value, final int fieldNo) {

		xmlProperty.putInteger(ATTR_VALUE + fieldNo, value);
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
