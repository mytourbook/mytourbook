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
import java.util.Arrays;

import net.tourbook.Messages;
import net.tourbook.application.ActionTourFilter;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.core.runtime.Assert;
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

// SET_FORMATTING_OFF

	private static final String							LABEL_POWER_AVG						= net.tourbook.ui.Messages.ColumnFactory_Power_Avg_Tooltip;
	private static final String							LABEL_POWER_MAX						= net.tourbook.ui.Messages.ColumnFactory_Power_Max_Tooltip;
	private static final String							LABEL_POWER_NORMALIZED				= net.tourbook.ui.Messages.ColumnFactory_Power_Normalized_Tooltip;
	private static final String							LABEL_POWER_TOTAL_WORK				= net.tourbook.ui.Messages.ColumnFactory_Power_TotalWork;

	/* Training */
	private static final String							LABEL_POWER_FTP						= net.tourbook.ui.Messages.ColumnFactory_Power_FTP_Tooltip;
	private static final String							LABEL_POWER_TO_WEIGHT				= net.tourbook.ui.Messages.ColumnFactory_Power_PowerToWeight_Tooltip;
	private static final String							LABEL_POWERTRAIN_AVG_CADENCE		= net.tourbook.ui.Messages.ColumnFactory_avg_cadence_label;
	private static final String							LABEL_POWERTRAIN_AVG_CADENCE_UNIT	= net.tourbook.ui.Messages.ColumnFactory_avg_cadence;
	private static final String							LABEL_POWERTRAIN_GEAR_FRONT_SHIFT	= net.tourbook.ui.Messages.ColumnFactory_GearFrontShiftCount_Label;
	private static final String							LABEL_POWERTRAIN_GEAR_REAR_SHIFT	= net.tourbook.ui.Messages.ColumnFactory_GearRearShiftCount_Label;

	private static final String							LABEL_CATEGORY_ALTITUDE				= net.tourbook.ui.Messages.ColumnFactory_Category_Altitude;
	private static final String							LABEL_CATEGORY_BODY					= net.tourbook.ui.Messages.ColumnFactory_Category_Body;
	private static final String							LABEL_CATEGORY_DATA					= net.tourbook.ui.Messages.ColumnFactory_Category_Data;
	private static final String							LABEL_CATEGORY_DEVICE				= net.tourbook.ui.Messages.ColumnFactory_Category_Device;
	private static final String							LABEL_CATEGORY_MARKER				= net.tourbook.ui.Messages.ColumnFactory_Category_Marker;
	private static final String							LABEL_CATEGORY_MOTION				= net.tourbook.ui.Messages.ColumnFactory_Category_Motion;
	private static final String							LABEL_CATEGORY_PHOTO				= net.tourbook.ui.Messages.ColumnFactory_Category_Photo;
	private static final String							LABEL_CATEGORY_POWER				= net.tourbook.ui.Messages.ColumnFactory_Category_Power;
	private static final String							LABEL_CATEGORY_POWERTRAIN			= net.tourbook.ui.Messages.ColumnFactory_Category_Powertrain;
	private static final String							LABEL_CATEGORY_STATE				= net.tourbook.ui.Messages.ColumnFactory_Category_State;
	private static final String							LABEL_CATEGORY_TIME					= net.tourbook.ui.Messages.ColumnFactory_Category_Time;
	private static final String							LABEL_CATEGORY_TOUR					= net.tourbook.ui.Messages.ColumnFactory_Category_Tour;
	private static final String							LABEL_CATEGORY_TRAINING				= net.tourbook.ui.Messages.ColumnFactory_Category_Training;
	private static final String							LABEL_CATEGORY_WAYPOINT				= net.tourbook.ui.Messages.ColumnFactory_Category_Waypoint;
	private static final String							LABEL_CATEGORY_WEATHER				= net.tourbook.ui.Messages.ColumnFactory_Category_Weather;

	private static final String							TOUR_DATA_ALTITUDE_DOWN 			= "TourData.tourAltDown";					//$NON-NLS-1$
	private static final String 						TOUR_DATA_ALTITUDE_UP 				= "TourData.tourAltUp";						//$NON-NLS-1$
	private static final String 						TOUR_DATA_ALTITUDE_MAX 				= "TourData.maxAltitude";					//$NON-NLS-1$
	private static final String							TOUR_DATA_AVG_TEMPERATURE			= "TourData.avgTemperature";				//$NON-NLS-1$
	private static final String							TOUR_DATA_NUMBER_OF_PHOTOS			= "TourData.numberOfPhotos";				//$NON-NLS-1$
	private static final String							TOUR_DATA_POWER_AVG					= "TourData.power_Avg";						//$NON-NLS-1$
	private static final String							TOUR_DATA_POWER_MAX					= "TourData.power_Max";						//$NON-NLS-1$
	private static final String							TOUR_DATA_POWER_NORMALIZED			= "TourData.power_Normalized";				//$NON-NLS-1$
	private static final String							TOUR_DATA_POWER_TOTAL_WORK			= "TourData.power_TotalWork";				//$NON-NLS-1$
	private static final String							TOUR_DATA_POWERTRAIN_AVG_CADENCE	= "TourData.avgCadence";					//$NON-NLS-1$
	private static final String							TOUR_DATA_POWERTRAIN_FRONT_SHIFT	= "TourData.frontShiftCount";				//$NON-NLS-1$
	private static final String							TOUR_DATA_POWERTRAIN_REAR_SHIFT		= "TourData.rearShiftCount";				//$NON-NLS-1$
	private static final String							TOUR_DATA_TRAINING_FTP				= "TourData.power_FTP";						//$NON-NLS-1$

	private static final String							TOUR_DATA_TOUR_DISTANCE				= "TourData.tourDistance";					//$NON-NLS-1$
	private static final String							TOUR_DATA_TOUR_DRIVING_TIME			= "TourData.tourDrivingTime";				//$NON-NLS-1$
	private static final String							TOUR_DATA_TOUR_RECORDING_TIME		= "TourData.tourRecordingTime";				//$NON-NLS-1$
	private static final String							TOUR_DATA_TOUR_START_TIME			= "TourData.tourStartTime";					//$NON-NLS-1$
	private static final String							TOUR_DATA_TOUR_TITLE				= "TourData.tourTitle";						//$NON-NLS-1$
	

	private static final String							TOUR_FILTER_FILE_NAME			= "tour-filter.xml";		//$NON-NLS-1$
	private static final int							TOUR_FILTER_VERSION				= 1;

	private static final String							TAG_PROFILE						= "Profile";				//$NON-NLS-1$
	private static final String							TAG_PROPERTY					= "Property";				//$NON-NLS-1$
	private static final String							TAG_ROOT						= "TourFilterProfiles";		//$NON-NLS-1$

	private static final String							ATTR_IS_ENABLED					= "isEnabled";				//$NON-NLS-1$
	private static final String							ATTR_IS_SELECTED				= "isSelected";				//$NON-NLS-1$
	private static final String							ATTR_FIELD_ID					= "fieldId";				//$NON-NLS-1$
	private static final String							ATTR_FIELD_OPERATOR				= "fieldOperator";			//$NON-NLS-1$
	private static final String							ATTR_NAME						= "name";					//$NON-NLS-1$
	private static final String							ATTR_SEASON_DAY					= "seasonDay";				//$NON-NLS-1$
	private static final String							ATTR_SEASON_MONTH				= "seasonMonth";			//$NON-NLS-1$
	private static final String							ATTR_TOUR_FILTER_VERSION		= "tourFilterVersion";		//$NON-NLS-1$
	private static final String							ATTR_VALUE						= "value";					//$NON-NLS-1$

	private static final String							ATTR_DATE_YEAR					= "dateYear";				//$NON-NLS-1$
	private static final String							ATTR_DATE_MONTH					= "dateMonth";				//$NON-NLS-1$
	private static final String							ATTR_DATE_DAY					= "dateDay";				//$NON-NLS-1$
	private static final String							ATTR_TIME_HOUR					= "timeHour";				//$NON-NLS-1$
	private static final String							ATTR_TIME_MINUTE				= "timeMinute";				//$NON-NLS-1$

	private static final String							OP_BR_OPEN						= "(";						//$NON-NLS-1$
	private static final String							OP_BR_CLOSE						= ")";						//$NON-NLS-1$
	private static final String							OP_AND							= " AND ";					//$NON-NLS-1$
	private static final String							OP_BETWEEN						= " BETWEEN ";				//$NON-NLS-1$
	private static final String							OP_NOT_BETWEEN					= " NOT BETWEEN ";			//$NON-NLS-1$
	private static final String							OP_OR							= " OR ";					//$NON-NLS-1$

	private static final String							OP_PARAMETER					= " ?\n";					//$NON-NLS-1$
	private static final String							OP_EQUALS						= " = ?\n";					//$NON-NLS-1$
	private static final String							OP_NOT_EQUALS					= " != ?\n";				//$NON-NLS-1$
	private static final String							OP_GREATER_THAN					= " > ?\n";					//$NON-NLS-1$
	private static final String							OP_GREATER_THAN_OR_EQUAL		= " >= ?\n";				//$NON-NLS-1$
	private static final String							OP_LESS_THAN					= " < ?\n";					//$NON-NLS-1$
	private static final String							OP_LESS_THAN_OR_EQUAL			= " <= ?\n";				//$NON-NLS-1$

	private static final String							OP_NULL							= " IS NULL\n";				//$NON-NLS-1$
	private static final String							OP_NOT_NULL						= " IS NOT NULL\n";			//$NON-NLS-1$

// SET_FORMATTING_ON
// SET_FORMATTING_OFF

	public static final TourFilterFieldOperatorConfig[]	TOUR_FILTER_OPERATORS = {
	                                   		                   			   
	   new TourFilterFieldOperatorConfig(TourFilterFieldOperator.STARTS_WITH,					Messages.Tour_Filter_Operator_StartsWith),
	   new TourFilterFieldOperatorConfig(TourFilterFieldOperator.ENDS_WITH,						Messages.Tour_Filter_Operator_EndsWith),
	   new TourFilterFieldOperatorConfig(TourFilterFieldOperator.EQUALS,						Messages.Tour_Filter_Operator_Equals),
	   new TourFilterFieldOperatorConfig(TourFilterFieldOperator.NOT_EQUALS,					Messages.Tour_Filter_Operator_NotEquals),
	   new TourFilterFieldOperatorConfig(TourFilterFieldOperator.LESS_THAN,						Messages.Tour_Filter_Operator_LessThan),
	   new TourFilterFieldOperatorConfig(TourFilterFieldOperator.LESS_THAN_OR_EQUAL,			Messages.Tour_Filter_Operator_LessThanOrEqual),
	   new TourFilterFieldOperatorConfig(TourFilterFieldOperator.GREATER_THAN,					Messages.Tour_Filter_Operator_GreaterThan),
	   new TourFilterFieldOperatorConfig(TourFilterFieldOperator.GREATER_THAN_OR_EQUAL,			Messages.Tour_Filter_Operator_GreaterThanOrEqual),
	   new TourFilterFieldOperatorConfig(TourFilterFieldOperator.BETWEEN,						Messages.Tour_Filter_Operator_Between),
	   new TourFilterFieldOperatorConfig(TourFilterFieldOperator.NOT_BETWEEN,					Messages.Tour_Filter_Operator_NotBetween),
	   new TourFilterFieldOperatorConfig(TourFilterFieldOperator.IS_EMPTY,						Messages.Tour_Filter_Operator_IsEmpty),
	   new TourFilterFieldOperatorConfig(TourFilterFieldOperator.IS_NOT_EMPTY,					Messages.Tour_Filter_Operator_IsNotEmpty),
	   
	   new TourFilterFieldOperatorConfig(TourFilterFieldOperator.SEASON_UNTIL_TODAY_FROM_YEAR_START,	Messages.Tour_Filter_Operator_Season_UntilToday_From_YearStart),
	   new TourFilterFieldOperatorConfig(TourFilterFieldOperator.SEASON_UNTIL_TODAY_FROM_DATE,			Messages.Tour_Filter_Operator_Season_UntilToday_From_Date),
	   new TourFilterFieldOperatorConfig(TourFilterFieldOperator.SEASON_CURRENT_DAY,					Messages.Tour_Filter_Operator_Season_Current_Day),
	   new TourFilterFieldOperatorConfig(TourFilterFieldOperator.SEASON_CURRENT_MONTH,					Messages.Tour_Filter_Operator_Season_Current_Month),
	   new TourFilterFieldOperatorConfig(TourFilterFieldOperator.SEASON_MONTH,							Messages.Tour_Filter_Operator_Season_Month),
	   new TourFilterFieldOperatorConfig(TourFilterFieldOperator.SEASON_TODAY_UNTIL_YEAR_END,			Messages.Tour_Filter_Operator_Season_Today_Until_YearEnd),
	   new TourFilterFieldOperatorConfig(TourFilterFieldOperator.SEASON_TODAY_UNTIL_DATE,				Messages.Tour_Filter_Operator_Season_Today_Until_Date),
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
	                                             			                         
		TourFilterFieldOperator.SEASON_UNTIL_TODAY_FROM_YEAR_START,
		TourFilterFieldOperator.SEASON_UNTIL_TODAY_FROM_DATE,
		TourFilterFieldOperator.SEASON_TODAY_UNTIL_YEAR_END,
		TourFilterFieldOperator.SEASON_TODAY_UNTIL_DATE,
		TourFilterFieldOperator.SEASON_CURRENT_DAY,
		TourFilterFieldOperator.SEASON_CURRENT_MONTH,
		TourFilterFieldOperator.SEASON_MONTH,
		TourFilterFieldOperator.BETWEEN,
		TourFilterFieldOperator.NOT_BETWEEN,
	};
	
	public static final TourFilterFieldOperator[]			FILTER_OPERATORS_TEXT = {
	                                             			                         
		TourFilterFieldOperator.IS_EMPTY,
		TourFilterFieldOperator.IS_NOT_EMPTY,
	};

	private static FieldValueConverter		_fieldValueProvider_Altitude		= new FieldValueProvider_Altitude();
	private static FieldValueConverter		_fieldValueProvider_Distance		= new FieldValueProvider_Distance();
	private static FieldValueConverter		_fieldValueProvider_Temperature		= new FieldValueProvider_Temperature();

// SET_FORMATTING_ON

	/**
	 * This is also the sequence how the fields are displayed in the UI
	 */
	public static final TourFilterFieldConfig[]			FILTER_FIELD_CONFIG;

	static {

		/*
		 * Get all category labels sorted by localized name
		 */
		final String[] allCategories = new String[] {
				LABEL_CATEGORY_ALTITUDE,
				LABEL_CATEGORY_STATE,
				LABEL_CATEGORY_BODY,
				LABEL_CATEGORY_DATA,
				LABEL_CATEGORY_DEVICE,
				LABEL_CATEGORY_MARKER,
				LABEL_CATEGORY_MOTION,
				LABEL_CATEGORY_PHOTO,
				LABEL_CATEGORY_POWER,
				LABEL_CATEGORY_POWERTRAIN,
				LABEL_CATEGORY_TIME,
				LABEL_CATEGORY_TOUR,
				LABEL_CATEGORY_TRAINING,
				LABEL_CATEGORY_WAYPOINT,
				LABEL_CATEGORY_WEATHER
		};
		Arrays.sort(allCategories);

		/*
		 * Create category configs sorted by localized name
		 */
		final ArrayList<TourFilterFieldConfig> allConfigs = new ArrayList<TourFilterFieldConfig>();

		for (final String category : allCategories) {

			if (category.equals(LABEL_CATEGORY_ALTITUDE)) {
				createConfig_Altitude(allConfigs);

			} else if (category.equals(LABEL_CATEGORY_BODY)) {
				createConfig_Body(allConfigs);

			} else if (category.equals(LABEL_CATEGORY_DATA)) {
				createConfig_Data(allConfigs);

			} else if (category.equals(LABEL_CATEGORY_DEVICE)) {
				createConfig_Device(allConfigs);

			} else if (category.equals(LABEL_CATEGORY_MARKER)) {

			} else if (category.equals(LABEL_CATEGORY_MOTION)) {
				createConfig_Motion(allConfigs);

			} else if (category.equals(LABEL_CATEGORY_PHOTO)) {
				createConfig_Power(allConfigs);

			} else if (category.equals(LABEL_CATEGORY_POWER)) {

			} else if (category.equals(LABEL_CATEGORY_POWERTRAIN)) {
				createConfig_Powertrain(allConfigs);

			} else if (category.equals(LABEL_CATEGORY_STATE)) {

			} else if (category.equals(LABEL_CATEGORY_TIME)) {
				createConfig_Time(allConfigs);

			} else if (category.equals(LABEL_CATEGORY_TOUR)) {
				createConfig_Tour(allConfigs);

			} else if (category.equals(LABEL_CATEGORY_TRAINING)) {
				createConfig_Training(allConfigs);

			} else if (category.equals(LABEL_CATEGORY_WAYPOINT)) {

			} else if (category.equals(LABEL_CATEGORY_WEATHER)) {
				createConfig_Weather(allConfigs);
			}
		}

		FILTER_FIELD_CONFIG = allConfigs.toArray(new TourFilterFieldConfig[allConfigs.size()]);
	}

	private static final Bundle				_bundle			= TourbookPlugin.getDefault().getBundle();

	private static final IPath				_stateLocation	= Platform.getStateLocation(_bundle);
	private final static IPreferenceStore	_prefStore		= TourbookPlugin.getPrefStore();

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

	private static void createConfig_Altitude(final ArrayList<TourFilterFieldConfig> allConfigs) {

		allConfigs.add(new TourFilterFieldConfig(LABEL_CATEGORY_ALTITUDE, TourFilterFieldId.ALTITUDE_UP));

		allConfigs.add(TourFilterFieldConfig//
				.name(Messages.Tour_Filter_Field_Altitude_Ascent)
				.fieldId(TourFilterFieldId.ALTITUDE_UP)
				.pageIncrement(100)
				.fieldValueProvider(_fieldValueProvider_Altitude));

		allConfigs.add(TourFilterFieldConfig//
				.name(Messages.Tour_Filter_Field_Altitude_Descent)
				.fieldId(TourFilterFieldId.ALTITUDE_DOWN)
				.pageIncrement(100)
				.minValue(Integer.MIN_VALUE)
				.fieldValueProvider(_fieldValueProvider_Altitude));

		allConfigs.add(TourFilterFieldConfig//
				.name(Messages.Tour_Filter_Field_Altitude_Max)
				.fieldId(TourFilterFieldId.ALTITUDE_MAX)
				.pageIncrement(100)
				.fieldValueProvider(_fieldValueProvider_Altitude));
	}

	private static void createConfig_Body(final ArrayList<TourFilterFieldConfig> allConfigs) {

//		// Body
//		defineColumn_Body_Calories();
//		defineColumn_Body_RestPulse();
//		defineColumn_Body_MaxPulse();
//		defineColumn_Body_AvgPulse();
//		defineColumn_Body_Weight();
//		defineColumn_Body_Person();

//		allConfigs.add(new TourFilterFieldConfig(COLUMN_FACTORY_CATEGORY_BODY, TourFilterFieldType.CATEGORY));
	}

	private static void createConfig_Data(final ArrayList<TourFilterFieldConfig> allConfigs) {

		//
//		// Data
//		defineColumn_Data_DPTolerance();
//		defineColumn_Data_ImportFilePath();
//		defineColumn_Data_ImportFileName();
//		defineColumn_Data_TimeInterval();
//		defineColumn_Data_NumTimeSlices();

//		allConfigs.add(new TourFilterFieldConfig(COLUMN_FACTORY_CATEGORY_DATA, TourFilterFieldType.CATEGORY));
	}

	private static void createConfig_Device(final ArrayList<TourFilterFieldConfig> allConfigs) {

		//
//		// Device
//		defineColumn_Device_Name();
//		defineColumn_Device_Distance();

//		allConfigs.add(new TourFilterFieldConfig(COLUMN_FACTORY_CATEGORY_DEVICE, TourFilterFieldType.CATEGORY));
	}

	private static void createConfig_Motion(final ArrayList<TourFilterFieldConfig> allConfigs) {

//
//		// Motion / Bewegung
//		defineColumn_Motion_Distance();
//		defineColumn_Motion_MaxSpeed();
//		defineColumn_Motion_AvgSpeed();
//		defineColumn_Motion_AvgPace();
//
		allConfigs.add(new TourFilterFieldConfig(LABEL_CATEGORY_MOTION, TourFilterFieldId.MOTION_DISTANCE));

		allConfigs.add(TourFilterFieldConfig//
				.name(Messages.Tour_Filter_Field_Distance)
				.fieldId(TourFilterFieldId.MOTION_DISTANCE)
				.fieldType(TourFilterFieldType.NUMBER_FLOAT)
				.fieldOperators(FILTER_OPERATORS_NUMBER)
				.defaultFieldOperator(TourFilterFieldOperator.GREATER_THAN)
				.pageIncrement(100)
				.numDigits(1)
				.fieldValueProvider(_fieldValueProvider_Distance));
	}

	private static void createConfig_Power(final ArrayList<TourFilterFieldConfig> allConfigs) {

		//
//		// Power - Leistung
//		defineColumn_Power_Avg();
//		defineColumn_Power_Max();
//		defineColumn_Power_Normalized();
//		defineColumn_Power_TotalWork();
//				(net.tourbook.ui.Messages.ColumnFactory_Category_Power)
//						+ (net.tourbook.ui.Messages.ColumnFactory_Power_Avg_Label)
//						+ (net.tourbook.ui.Messages.ColumnFactory_Power_Avg_Header)
//						+ (net.tourbook.ui.Messages.ColumnFactory_power)
//						+ (net.tourbook.ui.Messages.ColumnFactory_Power_Avg_Tooltip)

		allConfigs.add(new TourFilterFieldConfig(LABEL_CATEGORY_POWER, TourFilterFieldId.POWER_AVERAGE));

		allConfigs.add(TourFilterFieldConfig//
				.name(LABEL_POWER_AVG)
				.fieldId(TourFilterFieldId.POWER_AVERAGE)
				.defaultFieldOperator(TourFilterFieldOperator.GREATER_THAN)
				.unitLabel(UI.UNIT_POWER_SHORT));

		allConfigs.add(TourFilterFieldConfig//
				.name(LABEL_POWER_MAX)
				.fieldId(TourFilterFieldId.POWER_MAX)
				.defaultFieldOperator(TourFilterFieldOperator.GREATER_THAN)
				.unitLabel(UI.UNIT_POWER_SHORT));

		allConfigs.add(TourFilterFieldConfig//
				.name(LABEL_POWER_NORMALIZED)
				.fieldId(TourFilterFieldId.POWER_NORMALIZED)
				.defaultFieldOperator(TourFilterFieldOperator.GREATER_THAN)
				.unitLabel(UI.UNIT_POWER_SHORT));

		allConfigs.add(TourFilterFieldConfig//
				.name(LABEL_POWER_TOTAL_WORK)
				.fieldId(TourFilterFieldId.POWER_TOTAL_WORK)
				.fieldType(TourFilterFieldType.NUMBER_FLOAT)
				.defaultFieldOperator(TourFilterFieldOperator.GREATER_THAN)
				.unitLabel(UI.UNIT_JOULE_MEGA)
				.numDigits(1));
	}

	private static void createConfig_Powertrain(final ArrayList<TourFilterFieldConfig> allConfigs) {
		//
//		// Powertrain - Antrieb/Pedal
//		defineColumn_Powertrain_AvgCadence();
//		defineColumn_Powertrain_CadenceMultiplier();
//		defineColumn_Powertrain_Gear_FrontShiftCount();
//		defineColumn_Powertrain_Gear_RearShiftCount();
//		defineColumn_Powertrain_AvgLeftPedalSmoothness();
//		defineColumn_Powertrain_AvgLeftTorqueEffectiveness();
//		defineColumn_Powertrain_AvgRightPedalSmoothness();
//		defineColumn_Powertrain_AvgRightTorqueEffectiveness();
//		defineColumn_Powertrain_PedalLeftRightBalance();

		allConfigs.add(new TourFilterFieldConfig(LABEL_CATEGORY_POWERTRAIN, TourFilterFieldId.POWERTRAIN_AVG_CADENCE));

		allConfigs.add(TourFilterFieldConfig//
				.name(LABEL_POWERTRAIN_AVG_CADENCE)
				.fieldId(TourFilterFieldId.POWERTRAIN_AVG_CADENCE)
				.fieldType(TourFilterFieldType.NUMBER_FLOAT)
				.defaultFieldOperator(TourFilterFieldOperator.GREATER_THAN)
				.unitLabel(LABEL_POWERTRAIN_AVG_CADENCE_UNIT)
				.numDigits(1));

		allConfigs.add(TourFilterFieldConfig//
				.name(LABEL_POWERTRAIN_GEAR_FRONT_SHIFT)
				.fieldId(TourFilterFieldId.POWERTRAIN_GEAR_FRONT_SHIFT_COUNT)
				.defaultFieldOperator(TourFilterFieldOperator.GREATER_THAN));

		allConfigs.add(TourFilterFieldConfig//
				.name(LABEL_POWERTRAIN_GEAR_REAR_SHIFT)
				.fieldId(TourFilterFieldId.POWERTRAIN_GEAR_REAR_SHIFT_COUNT)
				.defaultFieldOperator(TourFilterFieldOperator.GREATER_THAN));

	}

	private static void createConfig_Time(final ArrayList<TourFilterFieldConfig> allConfigs) {

//		// Time
//		defineColumn_1stColumn_Date();
//		defineColumn_Time_WeekDay();
//		defineColumn_Time_TourStartTime();
//		defineColumn_Time_TimeZoneDifference();
//		defineColumn_Time_TimeZone();
//		defineColumn_Time_DrivingTime();
//		defineColumn_Time_RecordingTime();
//		defineColumn_Time_PausedTime();
//		defineColumn_Time_PausedTime_Relative();
//		defineColumn_Time_WeekNo();
//		defineColumn_Time_WeekYear();

		allConfigs.add(new TourFilterFieldConfig(LABEL_CATEGORY_TIME, TourFilterFieldId.TIME_TOUR_DATE));

		allConfigs.add(TourFilterFieldConfig //
				.name(Messages.Tour_Filter_Field_TourDate)
				.fieldId(TourFilterFieldId.TIME_TOUR_DATE)
				.fieldType(TourFilterFieldType.DATE)
				.fieldOperators(FILTER_OPERATORS_DATE_TIME)
				.defaultFieldOperator(TourFilterFieldOperator.GREATER_THAN));

		allConfigs.add(TourFilterFieldConfig //
				.name(Messages.Tour_Filter_Field_TourStartTime)
				.fieldId(TourFilterFieldId.TIME_TOUR_TIME)
				.fieldType(TourFilterFieldType.TIME)
				.fieldOperators(FILTER_OPERATORS_DATE_TIME)
				.defaultFieldOperator(TourFilterFieldOperator.GREATER_THAN));

		allConfigs.add(TourFilterFieldConfig //
				.name(Messages.Tour_Filter_Field_Season)
				.fieldId(TourFilterFieldId.TIME_SEASON_DATE)
				.fieldType(TourFilterFieldType.SEASON)
				.fieldOperators(FILTER_OPERATORS_SEASON)
				.defaultFieldOperator(TourFilterFieldOperator.LESS_THAN_OR_EQUAL));

		allConfigs.add(TourFilterFieldConfig //
				.name(Messages.Tour_Filter_Field_RecordingTime)
				.fieldId(TourFilterFieldId.TIME_RECORDING_TIME)
				.fieldType(TourFilterFieldType.DURATION)
				.defaultFieldOperator(TourFilterFieldOperator.GREATER_THAN)
				.pageIncrement(60));

		allConfigs.add(TourFilterFieldConfig //
				.name(Messages.Tour_Filter_Field_DrivingTime)
				.fieldId(TourFilterFieldId.TIME_DRIVING_TIME)
				.fieldType(TourFilterFieldType.DURATION)
				.defaultFieldOperator(TourFilterFieldOperator.GREATER_THAN)
				.pageIncrement(60));

		allConfigs.add(TourFilterFieldConfig //
				.name(Messages.Tour_Filter_Field_BreakTime)
				.fieldId(TourFilterFieldId.TIME_BREAK_TIME)
				.fieldType(TourFilterFieldType.DURATION)
				.defaultFieldOperator(TourFilterFieldOperator.GREATER_THAN)
				.pageIncrement(60));
	}

	private static void createConfig_Tour(final ArrayList<TourFilterFieldConfig> allConfigs) {

//		// Tour
//		defineColumn_Tour_TypeImage();
//		defineColumn_Tour_TypeText();
//		defineColumn_Tour_Title();
//		defineColumn_Tour_Marker();
//		defineColumn_Tour_Photos();
//		defineColumn_Tour_Tags();

		allConfigs.add(new TourFilterFieldConfig(LABEL_CATEGORY_TOUR, TourFilterFieldId.TOUR_TITLE));

		allConfigs.add(TourFilterFieldConfig //
				.name(Messages.Tour_Filter_Field_TourTitle)
				.fieldId(TourFilterFieldId.TOUR_TITLE)
				.fieldType(TourFilterFieldType.TEXT)
				.fieldOperators(FILTER_OPERATORS_TEXT));

		allConfigs.add(TourFilterFieldConfig //
				.name(Messages.Tour_Filter_Field_Photos)
				.fieldId(TourFilterFieldId.TOUR_PHOTOS)
				.defaultFieldOperator(TourFilterFieldOperator.GREATER_THAN)
				.pageIncrement(10));

//		allConfigs.add(
//				new TourFilterFieldConfig(
//						Messages.Tour_Filter_Field_Photos,
//						TourFilterFieldId.TOUR_PHOTOS,
//						TourFilterFieldType.NUMBER_INTEGER,
//						FILTER_OPERATORS_NUMBER,
//						0,
//						Integer.MAX_VALUE,
//						10));

//		allConfigs.add(
//				new TourFilterFieldConfig(
//						Messages.Tour_Filter_Field_Markers,
//						TourFilterFieldId.TOUR_MARKERS,
//						TourFilterFieldType.NUMBER_INTEGER,
//						FILTER_OPERATORS_NUMBER,
//						0,
//						Integer.MAX_VALUE,
//						10));
	}

	private static void createConfig_Training(final ArrayList<TourFilterFieldConfig> allConfigs) {

		//
//		// Training - Trainingsanalyse
//		defineColumn_Training_FTP();
//		defineColumn_Training_PowerToWeightRatio();
//		defineColumn_Training_IntensityFactor();
//		defineColumn_Training_StressScore();

//		allConfigs.add(new TourFilterFieldConfig(COLUMN_FACTORY_CATEGORY_TRAINING, TourFilterFieldType.CATEGORY));

		allConfigs.add(new TourFilterFieldConfig(LABEL_CATEGORY_TRAINING, TourFilterFieldId.TRAINING_FTP));

		allConfigs.add(TourFilterFieldConfig//
				.name(LABEL_POWER_FTP)
				.fieldId(TourFilterFieldId.TRAINING_FTP)
				.defaultFieldOperator(TourFilterFieldOperator.GREATER_THAN)
				.unitLabel(UI.UNIT_POWER_SHORT));

//		allConfigs.add(TourFilterFieldConfig//
//				.name(COLUMN_FACTORY_POWER_TO_WEIGHT)
//				.fieldId(TourFilterFieldId.TRAINING_POWER_TO_WEIGHT_RATIO)
//				.fieldType(TourFilterFieldType.NUMBER_FLOAT)
//				.numDigits(1));
	}

	private static void createConfig_Weather(final ArrayList<TourFilterFieldConfig> allConfigs) {

//		// Weather
//		defineColumn_Weather_Clouds();
//		defineColumn_Weather_AvgTemperature();
//		defineColumn_Weather_WindSpeed();
//		defineColumn_Weather_WindDirection();
//
		allConfigs.add(new TourFilterFieldConfig(LABEL_CATEGORY_WEATHER, TourFilterFieldId.WEATHER_TEMPERATURE));

		allConfigs.add(TourFilterFieldConfig
				.name(Messages.Tour_Filter_Field_Temperature)
				.fieldId(TourFilterFieldId.WEATHER_TEMPERATURE)
				.fieldType(TourFilterFieldType.NUMBER_FLOAT)
				.defaultFieldOperator(TourFilterFieldOperator.GREATER_THAN)
				.minValue(-600)
				.maxValue(1500)
				.pageIncrement(10)
				.numDigits(1)
				.fieldValueProvider(_fieldValueProvider_Temperature));
	}

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
	 * @param requestedFieldId
	 * @param startIndex
	 * @return Returns the index of the requested field id.
	 */
	static int getFilterFieldIndex(final TourFilterFieldId requestedFieldId) {

		TourFilterFieldConfig categoryConfig = null;

		/*
		 * get index for a 'normal' field id
		 */
		for (int typeIndex = 0; typeIndex < FILTER_FIELD_CONFIG.length; typeIndex++) {

			final TourFilterFieldConfig fieldConfig = FILTER_FIELD_CONFIG[typeIndex];
			final TourFilterFieldId fieldId = fieldConfig.fieldId;

			if (fieldId != null && fieldId.equals(requestedFieldId)) {

				if (fieldConfig.fieldType == TourFilterFieldType.CATEGORY) {

					categoryConfig = fieldConfig;
					break;
				}

				return typeIndex;
			}
		}

		Assert.isNotNull(categoryConfig);

		/*
		 * get index for the category default field id
		 */
		final TourFilterFieldId defaultFieldId = categoryConfig.categoryDefaultFieldId;
		for (int typeIndex = 0; typeIndex < FILTER_FIELD_CONFIG.length; typeIndex++) {

			final TourFilterFieldConfig fieldConfig = FILTER_FIELD_CONFIG[typeIndex];
			final TourFilterFieldId fieldId = fieldConfig.fieldId;

			if (fieldId.equals(defaultFieldId)) {
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
			case ALTITUDE_DOWN:

				sql = TOUR_DATA_ALTITUDE_DOWN;

				/*
				 * Altitude down values have no sign in the database, they are always > 0
				 */
				value1 = int1 < 0 ? -int1 : int1;
				value2 = int2 < 0 ? -int2 : int2;

				getSQL__FieldOperators_Number(sqlWhere, sqlParameters, fieldOperator, sql, value1, value2);
				break;

			case ALTITUDE_UP:
				sql = TOUR_DATA_ALTITUDE_UP;
				getSQL__FieldOperators_Number(sqlWhere, sqlParameters, fieldOperator, sql, int1, int2);
				break;

			case ALTITUDE_MAX:
				sql = TOUR_DATA_ALTITUDE_MAX;
				getSQL__FieldOperators_Number(sqlWhere, sqlParameters, fieldOperator, sql, int1, int2);
				break;

			case TIME_TOUR_DATE:

				sql = TOUR_DATA_TOUR_START_TIME;

				value1 = (LocalDate
						.of(dateTime1.getYear(), dateTime1.getMonthValue(), dateTime1.getDayOfMonth())
						.toEpochDay() + 1) * 86400_000;

				value2 = (LocalDate
						.of(dateTime2.getYear(), dateTime2.getMonthValue(), dateTime2.getDayOfMonth())
						.toEpochDay() + 1) * 86400_000;

				getSQL__FieldOperators_DateTime(sqlWhere, sqlParameters, fieldOperator, sql, value1, value2);
				break;

			case TIME_TOUR_TIME:

				sql = "(TourData.startHour * 3600 + TourData.startMinute)"; //$NON-NLS-1$

				value1 = dateTime1.getHour() * 3600 + dateTime1.getMinute();
				value2 = dateTime2.getHour() * 3600 + dateTime2.getMinute();

				getSQL__FieldOperators_DateTime(sqlWhere, sqlParameters, fieldOperator, sql, value1, value2);
				break;

			case TIME_SEASON_DATE:
				getSQL__FieldOperators_SeasonDate(sqlWhere, sqlParameters, filterProperty, today);
				break;

			case TIME_BREAK_TIME:
				sql = "(TourData.tourRecordingTime - TourData.tourDrivingTime)"; //$NON-NLS-1$
				getSQL__FieldOperators_Number(sqlWhere, sqlParameters, fieldOperator, sql, int1, int2);
				break;

			case TIME_DRIVING_TIME:
				sql = TOUR_DATA_TOUR_DRIVING_TIME;
				getSQL__FieldOperators_Number(sqlWhere, sqlParameters, fieldOperator, sql, int1, int2);

				break;

			case TIME_RECORDING_TIME:
				sql = TOUR_DATA_TOUR_RECORDING_TIME;
				getSQL__FieldOperators_Number(sqlWhere, sqlParameters, fieldOperator, sql, int1, int2);
				break;

			case MOTION_DISTANCE:

				sql = TOUR_DATA_TOUR_DISTANCE;

				// convert from km to m
				final double distance1 = double1 * 1000;
				final double distance2 = double2 * 1000;

				getSQL__FieldOperators_Number(sqlWhere, sqlParameters, fieldOperator, sql, distance1, distance2);
				break;

			case WEATHER_TEMPERATURE:
				sql = TOUR_DATA_AVG_TEMPERATURE;
				getSQL__FieldOperators_Number(sqlWhere, sqlParameters, fieldOperator, sql, double1, double2);
				break;

			case POWER_AVERAGE:
				sql = TOUR_DATA_POWER_AVG;
				getSQL__FieldOperators_Number(sqlWhere, sqlParameters, fieldOperator, sql, int1, int2);
				break;

			case POWER_MAX:
				sql = TOUR_DATA_POWER_MAX;
				getSQL__FieldOperators_Number(sqlWhere, sqlParameters, fieldOperator, sql, int1, int2);
				break;

			case POWER_NORMALIZED:
				sql = TOUR_DATA_POWER_NORMALIZED;
				getSQL__FieldOperators_Number(sqlWhere, sqlParameters, fieldOperator, sql, int1, int2);
				break;

			case POWER_TOTAL_WORK:

				sql = TOUR_DATA_POWER_TOTAL_WORK;

				// convert from MJ into J
				final double work1 = double1 * 1000_000;
				final double work2 = double2 * 1000_000;

				getSQL__FieldOperators_Number(sqlWhere, sqlParameters, fieldOperator, sql, work1, work2);
				break;

			case POWERTRAIN_AVG_CADENCE:
				sql = TOUR_DATA_POWERTRAIN_AVG_CADENCE;
				getSQL__FieldOperators_Number(sqlWhere, sqlParameters, fieldOperator, sql, double1, double2);
				break;

			case POWERTRAIN_GEAR_FRONT_SHIFT_COUNT:
				sql = TOUR_DATA_POWERTRAIN_FRONT_SHIFT;
				getSQL__FieldOperators_Number(sqlWhere, sqlParameters, fieldOperator, sql, int1, int2);
				break;

			case POWERTRAIN_GEAR_REAR_SHIFT_COUNT:
				sql = TOUR_DATA_POWERTRAIN_REAR_SHIFT;
				getSQL__FieldOperators_Number(sqlWhere, sqlParameters, fieldOperator, sql, int1, int2);
				break;

			case TRAINING_FTP:
				sql = TOUR_DATA_TRAINING_FTP;
				getSQL__FieldOperators_Number(sqlWhere, sqlParameters, fieldOperator, sql, int1, int2);
				break;

			case TOUR_PHOTOS:
				sql = TOUR_DATA_NUMBER_OF_PHOTOS;
				getSQL__FieldOperators_Number(sqlWhere, sqlParameters, fieldOperator, sql, int1, int2);
				break;

			case TOUR_TITLE:
				sql = TOUR_DATA_TOUR_TITLE;
				getSQL__FieldOperators_Text(sqlWhere, sqlParameters, fieldOperator, sql, text1, text2);
				break;
			}
		}

		final TourFilterSQLData tourFilterSQLData = new TourFilterSQLData(sqlWhere.toString(), sqlParameters);

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
		final String sqlMonth = "TourData.startMonth";//$NON-NLS-1$

		switch (filterProperty.fieldOperator) {
		case SEASON_UNTIL_TODAY_FROM_YEAR_START:
			sqlWhere.append(OP_AND + sql + OP_LESS_THAN_OR_EQUAL);
			sqlParameters.add(todayValue);
			break;

		case SEASON_TODAY_UNTIL_YEAR_END:
			sqlWhere.append(OP_AND + sql + OP_GREATER_THAN_OR_EQUAL);
			sqlParameters.add(todayValue);
			break;

		case SEASON_UNTIL_TODAY_FROM_DATE:

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

		case SEASON_CURRENT_DAY:

			sqlWhere.append(OP_AND
					+ (sql + OP_EQUALS) // today
			);

			sqlParameters.add(todayValue);

			break;

		case SEASON_CURRENT_MONTH:

			sqlWhere.append(OP_AND + (sqlMonth + OP_EQUALS));
			sqlParameters.add(todayMonth);

			break;

		case SEASON_MONTH:

			sqlWhere.append(OP_AND + (sqlMonth + OP_EQUALS));
			sqlParameters.add(month1);

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
									TourFilterFieldId.TIME_TOUR_DATE);

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

			case NUMBER_FLOAT:
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

			case NUMBER_FLOAT:
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

		case SEASON_UNTIL_TODAY_FROM_DATE:
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
		fieldConfig = getFieldConfig(TourFilterFieldId.MOTION_DISTANCE);
		fieldConfig.unitLabel = UI.UNIT_LABEL_DISTANCE;

		// set label celcius or fahrenheit
		fieldConfig = getFieldConfig(TourFilterFieldId.WEATHER_TEMPERATURE);
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

			case NUMBER_FLOAT:
				writeXml_Number_Float(xmlProperty, doubleValue1, 1);
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

			case NUMBER_FLOAT:
				writeXml_Number_Float(xmlProperty, doubleValue1, 1);
				writeXml_Number_Float(xmlProperty, doubleValue2, 2);
				break;

			case SEASON:
				writeXml_Season(xmlProperty, monthDay1, 1);
				writeXml_Season(xmlProperty, monthDay2, 2);
				break;
			}

			break;

		case SEASON_UNTIL_TODAY_FROM_DATE:
		case SEASON_TODAY_UNTIL_DATE:
		case SEASON_MONTH:
			writeXml_Season(xmlProperty, monthDay1, 1);
			break;
		}
	}

	private static void writeXml_Date(final IMemento xmlProperty, final LocalDateTime dateTime, final int fieldNo) {

		xmlProperty.putInteger(ATTR_DATE_YEAR + fieldNo, dateTime.getYear());
		xmlProperty.putInteger(ATTR_DATE_MONTH + fieldNo, dateTime.getMonthValue());
		xmlProperty.putInteger(ATTR_DATE_DAY + fieldNo, dateTime.getDayOfMonth());
	}

	private static void writeXml_Number_Float(final IMemento xmlProperty, final double value, final int fieldNo) {

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
