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

public enum TourFilterFieldId {

	ALTITUDE_UP, //
	ALTITUDE_DOWN, //
	ALTITUDE_MAX, //

	TIME_RECORDING_TIME, //
	TIME_DRIVING_TIME, //
	TIME_BREAK_TIME, //
	TIME_TOUR_DATE, //
	TIME_TOUR_TIME, //

	/**
	 * DATE_SEASON or DATE_PERIOD, is a repeated interval, e.g. from start year until today,
	 * repeated for each year
	 */
	TIME_SEASON_DATE,

//	TOUR_MARKERS, //
	TOUR_PHOTOS, //
	TOUR_TITLE, //
	TOUR_MANUAL_TOUR, //

	MOTION_DISTANCE, //

	POWER_AVERAGE, //
	POWER_MAX, //
	POWER_NORMALIZED, //
	POWER_TOTAL_WORK, //

//	// Powertrain - Antrieb/Pedal
//	defineColumn_Powertrain_AvgCadence();
//	defineColumn_Powertrain_CadenceMultiplier();
//	defineColumn_Powertrain_Gear_FrontShiftCount();
//	defineColumn_Powertrain_Gear_RearShiftCount();
//	defineColumn_Powertrain_AvgLeftPedalSmoothness();
//	defineColumn_Powertrain_AvgLeftTorqueEffectiveness();
//	defineColumn_Powertrain_AvgRightPedalSmoothness();
//	defineColumn_Powertrain_AvgRightTorqueEffectiveness();
//	defineColumn_Powertrain_PedalLeftRightBalance();

	POWERTRAIN_AVG_CADENCE, //
	POWERTRAIN_GEAR_FRONT_SHIFT_COUNT, //
	POWERTRAIN_GEAR_REAR_SHIFT_COUNT, //

	TRAINING_FTP, //
	TRAINING_POWER_TO_WEIGHT_RATIO, //
	TRAINING_INTENSITY_FACTOR, //
	TRAINING_STRESS_SCORE, //

	WEATHER_TEMPERATURE, //


}
