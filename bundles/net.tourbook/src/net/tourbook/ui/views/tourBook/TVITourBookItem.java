/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourBook;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;

import net.tourbook.common.time.TimeTools;
import net.tourbook.common.time.TourDateTime;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.tour.ITourItem;

public abstract class TVITourBookItem extends TreeViewerItem implements ITourItem {

	static ZonedDateTime	calendar8	= ZonedDateTime.now().with(TimeTools.calendarWeek.dayOfWeek(), 1);

	static final char		NL			= net.tourbook.common.UI.NEW_LINE;

	static final String		SQL_SUM_COLUMNS;
	static final String		SQL_SUM_FIELDS;

// SET_FORMATTING_OFF
	
	static {
		
		SQL_SUM_FIELDS = NL
				
			+ "TourDistance,					" + NL //$NON-NLS-1$
			+ "TourRecordingTime,				" + NL //$NON-NLS-1$
			+ "TourDrivingTime,					" + NL //$NON-NLS-1$
			+ "TourAltUp,						" + NL //$NON-NLS-1$
			+ "TourAltDown,						" + NL //$NON-NLS-1$
                                                
			+ "MaxAltitude,						" + NL //$NON-NLS-1$
			+ "MaxPulse,						" + NL //$NON-NLS-1$
			+ "MaxSpeed,						" + NL //$NON-NLS-1$
                                                
			+ "AvgCadence,						" + NL //$NON-NLS-1$
			+ "AvgPulse,						" + NL //$NON-NLS-1$
			+ "AvgTemperature,					" + NL //$NON-NLS-1$
			+ "CadenceMultiplier,				" + NL //$NON-NLS-1$
			+ "TemperatureScale,				" + NL //$NON-NLS-1$
			+ "WeatherWindDir,					" + NL //$NON-NLS-1$
			+ "WeatherWindSpd,					" + NL //$NON-NLS-1$
                                              
			+ "Calories,						" + NL //$NON-NLS-1$
			+ "RestPulse,						" + NL //$NON-NLS-1$
                                              
			+ "Power_TotalWork,					" + NL //$NON-NLS-1$
                                              
			+ "NumberOfTimeSlices,				" + NL //$NON-NLS-1$
			+ "NumberOfPhotos,					" + NL //$NON-NLS-1$
                                              
			+ "FrontShiftCount,					" + NL //$NON-NLS-1$
			+ "RearShiftCount					" + NL //$NON-NLS-1$
		;
		
		
		SQL_SUM_COLUMNS = NL

			+ "SUM(TourDistance),				" + NL // 0	//$NON-NLS-1$
			+ "SUM(TourRecordingTime),			" + NL // 1	//$NON-NLS-1$
			+ "SUM(TourDrivingTime),			" + NL // 2	//$NON-NLS-1$
			+ "SUM(TourAltUp),					" + NL // 3	//$NON-NLS-1$
			+ "SUM(TourAltDown),				" + NL // 4	//$NON-NLS-1$
			+ "SUM(1),							" + NL // 5	//$NON-NLS-1$
			//
			+ "MAX(MaxSpeed),					" + NL // 6	//$NON-NLS-1$
			+ "SUM(TourDistance),				" + NL // 7	//$NON-NLS-1$
			+ "SUM(TourDrivingTime),			" + NL // 8	//$NON-NLS-1$
			+ "MAX(MaxAltitude),				" + NL // 9	//$NON-NLS-1$
			+ "MAX(MaxPulse),					" + NL // 10 //$NON-NLS-1$
			//
			+ "AVG( CASE WHEN AvgPulse = 0			THEN NULL ELSE AvgPulse END),			" + NL //								11	//$NON-NLS-1$
			+ "AVG( CASE WHEN AvgCadence = 0		THEN NULL ELSE DOUBLE(AvgCadence) * CadenceMultiplier END ),		" + NL //	12	//$NON-NLS-1$
			+ "AVG( CASE WHEN AvgTemperature = 0	THEN NULL ELSE DOUBLE(AvgTemperature) / TemperatureScale END ),		" + NL //	13	//$NON-NLS-1$
			+ "AVG( CASE WHEN WeatherWindDir = 0	THEN NULL ELSE WeatherWindDir END ),	" + NL //								14	//$NON-NLS-1$
			+ "AVG( CASE WHEN WeatherWindSpd = 0	THEN NULL ELSE WeatherWindSpd END ),	" + NL //								15	//$NON-NLS-1$
			+ "AVG( CASE WHEN RestPulse = 0			THEN NULL ELSE RestPulse END ),			" + NL //								16	//$NON-NLS-1$
			//
			+ "SUM(Calories),					" + NL // 17	//$NON-NLS-1$
			+ "SUM(Power_TotalWork),			" + NL // 18	//$NON-NLS-1$

			+ "SUM(NumberOfTimeSlices),			" + NL // 19	//$NON-NLS-1$
			+ "SUM(NumberOfPhotos),				" + NL // 20	//$NON-NLS-1$
			//
			+ "SUM(FrontShiftCount),			" + NL // 21	//$NON-NLS-1$
			+ "SUM(RearShiftCount)				" + NL // 22	//$NON-NLS-1$
		;
	}
	
// SET_FORMATTING_ON

	TourBookView	tourBookView;

	String			treeColumn;

	int				tourYear;

	/**
	 * Month starts with 1 for January
	 */
	int				tourMonth;
	int				tourWeek;
	int				tourYearSub;
	int				tourDay;

	/**
	 * Contain the tour date time with time zone info when available.
	 */
	TourDateTime	colTourDateTime;
	String			colTimeZoneId;

	String			colTourTitle;
	long			colPersonId;							// tourPerson_personId

	long			colCounter;
	long			colCalories;
	long			colDistance;
	float			colBodyWeight;

	long			colRecordingTime;
	long			colDrivingTime;
	long			colPausedTime;

	long			colAltitudeUp;
	long			colAltitudeDown;

	float			colMaxSpeed;
	long			colMaxAltitude;
	long			colMaxPulse;

	float			colAvgSpeed;
	float			colAvgPace;
	float			colAvgPulse;
	float			colAvgCadence;
	float			colAvgTemperature;

	int				colWindSpd;
	int				colWindDir;
	String			colClouds;
	int				colRestPulse;

	int				colWeekNo;
	String			colWeekDay;
	int				colWeekYear;

	int				colNumberOfTimeSlices;
	int				colNumberOfPhotos;

	int				colDPTolerance;

	int				colFrontShiftCount;
	int				colRearShiftCount;

	float			colCadenceMultiplier;

	// ----------- Running Dynamics ---------

	int				colRunDyn_StanceTime;
	int				colRunDyn_StanceTime_Max;
	float			colRunDyn_StanceTime_Avg;

	float			colRunDyn_StanceTimeBalance_Min;
	float			colRunDyn_StanceTimeBalance_Max;
	float			colRunDyn_StanceTimeBalance_Avg;

	int				colRunDyn_StepLength_Min;
	int				colRunDyn_StepLength_Max;
	float			colRunDyn_StepLength_Avg;

	float			colRunDyn_VerticalOscillation_Min;
	float			colRunDyn_VerticalOscillation_Max;
	float			colRunDyn_VerticalOscillation_Avg;

	float			colRunDyn_VerticalRatio_Min;
	float			colRunDyn_VerticalRatio_Max;
	float			colRunDyn_VerticalRatio_Avg;

	// ----------- POWER ---------

	float			colPower_AvgLeftTorqueEffectiveness;
	float			colPower_AvgRightTorqueEffectiveness;
	float			colPower_AvgLeftPedalSmoothness;
	float			colPower_AvgRightPedalSmoothness;
	int				colPower_PedalLeftRightBalance;

	float			colPower_Avg;
	int				colPower_Max;
	int				colPower_Normalized;
	long			colPower_TotalWork;

	int				colPower_FTP;
	float			colPower_TrainingStressScore;
	float			colPower_IntensityFactor;

	float			colPower_PowerToWeight;

	// ----------- IMPORT ---------

	String			col_ImportFileName;
	String			col_ImportFilePath;
	String			col_DeviceName;

	TVITourBookItem(final TourBookView view) {

		tourBookView = view;
	}

	public void addSumColumns(final ResultSet result, final int startIndex) throws SQLException {

		colDistance = result.getLong(startIndex + 0);

		colRecordingTime = result.getLong(startIndex + 1);
		colDrivingTime = result.getLong(startIndex + 2);

		colAltitudeUp = result.getLong(startIndex + 3);
		colAltitudeDown = result.getLong(startIndex + 4);

		colCounter = result.getLong(startIndex + 5);

		colMaxSpeed = result.getFloat(startIndex + 6);

		// compute average speed/pace, prevent divide by 0
		final long dbDistance = result.getLong(startIndex + 7);
		final long dbDrivingTime = result.getLong(startIndex + 8);

		colAvgSpeed = dbDrivingTime == 0 ? 0 : 3.6f * dbDistance / dbDrivingTime;
		colAvgPace = dbDistance == 0 ? 0 : dbDrivingTime * 1000f / dbDistance;

		colMaxAltitude = result.getLong(startIndex + 9);
		colMaxPulse = result.getLong(startIndex + 10);

		colAvgPulse = result.getFloat(startIndex + 11);
		colAvgCadence = result.getFloat(startIndex + 12);
		colAvgTemperature = result.getFloat(startIndex + 13);

		colWindDir = result.getInt(startIndex + 14);
		colWindSpd = result.getInt(startIndex + 15);
		colRestPulse = result.getInt(startIndex + 16);

		colCalories = result.getLong(startIndex + 17);
		colPower_TotalWork = result.getLong(startIndex + 18);

		colNumberOfTimeSlices = result.getInt(startIndex + 19);
		colNumberOfPhotos = result.getInt(startIndex + 20);

		colFrontShiftCount = result.getInt(startIndex + 21);
		colRearShiftCount = result.getInt(startIndex + 22);

		colPausedTime = colRecordingTime - colDrivingTime;
	}

	@Override
	public Long getTourId() {
		return null;
	}

}
