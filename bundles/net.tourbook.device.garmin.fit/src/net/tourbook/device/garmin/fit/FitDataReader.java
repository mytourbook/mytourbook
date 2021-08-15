/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.device.garmin.fit;

import com.garmin.fit.Decode;
import com.garmin.fit.DeveloperField;
import com.garmin.fit.Field;
import com.garmin.fit.Fit;
import com.garmin.fit.FitRuntimeException;
import com.garmin.fit.Mesg;
import com.garmin.fit.MesgBroadcaster;
import com.garmin.fit.MesgListener;
import com.garmin.fit.MesgNum;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StringUtils;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.device.garmin.fit.listeners.MesgListener_Activity;
import net.tourbook.device.garmin.fit.listeners.MesgListener_BikeProfile;
import net.tourbook.device.garmin.fit.listeners.MesgListener_DeviceInfo;
import net.tourbook.device.garmin.fit.listeners.MesgListener_Event;
import net.tourbook.device.garmin.fit.listeners.MesgListener_FileCreator;
import net.tourbook.device.garmin.fit.listeners.MesgListener_FileId;
import net.tourbook.device.garmin.fit.listeners.MesgListener_Hr;
import net.tourbook.device.garmin.fit.listeners.MesgListener_Hrv;
import net.tourbook.device.garmin.fit.listeners.MesgListener_Lap;
import net.tourbook.device.garmin.fit.listeners.MesgListener_Length;
import net.tourbook.device.garmin.fit.listeners.MesgListener_Record;
import net.tourbook.device.garmin.fit.listeners.MesgListener_Session;
import net.tourbook.device.garmin.fit.listeners.MesgListener_Sport;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.SerialParameters;
import net.tourbook.importdata.TourbookDevice;
import net.tourbook.tour.TourLogManager;

import org.apache.commons.io.FilenameUtils;

/**
 * Garmin FIT activity reader based on the official Garmin SDK.
 *
 * @author Wolfgang Schramm
 */
public class FitDataReader extends TourbookDevice {

   private static final String SYS_PROP__LOG_FIT_DATA = "logFitData";                                      //$NON-NLS-1$
   private static boolean      _isLogging_FitData     = System.getProperty(SYS_PROP__LOG_FIT_DATA) != null;

   static {

      if (_isLogging_FitData) {

         Util.logSystemProperty_IsEnabled(FitDataReader.class,
               SYS_PROP__LOG_FIT_DATA,
               "Fit data are logged"); //$NON-NLS-1$
      }
   }

   private int     _logCounter;
   private boolean _isVersionLogged;

   @Override
   public String buildFileNameFromRawData(final String rawDataFileName) {
      return null;
   }

   @Override
   public boolean checkStartSequence(final int byteIndex, final int newByte) {
      return false;
   }

   @Override
   public String getDeviceModeName(final int modeId) {
      return null;
   }

   @Override
   public SerialParameters getPortParameters(final String portName) {
      return null;
   }

   @Override
   public int getStartSequenceSize() {
      return 0;
   }

   @Override
   public int getTransferDataSize() {
      return 0;
   }

   private boolean isFieldSkipped(final String fieldName) {

      final boolean isSkipped = false

//            // this is the profile name
//            || fieldName.equals("name") //                                                            //$NON-NLS-1$
//
//            // Time
//
//            || fieldName.equals("time") //                                                            //$NON-NLS-1$
//            || fieldName.equals("timestamp") //                                                       //$NON-NLS-1$
//            || fieldName.equals("time_created") //                                                    //$NON-NLS-1$
//            || fieldName.equals("time_offset") //                                                     //$NON-NLS-1$
//            || fieldName.equals("utc_offset") //                                                      //$NON-NLS-1$
//
//            || fieldName.equals("start_time") //                                                      //$NON-NLS-1$
//            || fieldName.equals("total_elapsed_time") //                                              //$NON-NLS-1$
//            || fieldName.equals("total_moving_time") //                            8747.0 s           //$NON-NLS-1$
//            || fieldName.equals("total_timer_time") //                                                //$NON-NLS-1$
//
//            || fieldName.equals("wake_time") //                                     32400             //$NON-NLS-1$
//            || fieldName.equals("sleep_time") //                                    79200             //$NON-NLS-1$
//
//            || fieldName.equals("event_timestamp_12") //                                              //$NON-NLS-1$
//            || fieldName.equals("event_timestamp") //                                                 //$NON-NLS-1$
//            || fieldName.equals("fractional_timestamp") //                             0.730 s        //$NON-NLS-1$
//            || fieldName.equals("local_timestamp") //                                                 //$NON-NLS-1$
//
//            || fieldName.equals("active_time_zone") //                                                //$NON-NLS-1$
//            || fieldName.equals("time_zone_offset") //                                                //$NON-NLS-1$
//
//            // Activity
//
//            || fieldName.equals("activity_class") //                                                  //$NON-NLS-1$
//            || fieldName.equals("activity_type") //                                                   //$NON-NLS-1$
//            || fieldName.equals("data") //                                                            //$NON-NLS-1$
//            || fieldName.equals("event") //                                                           //$NON-NLS-1$
//            || fieldName.equals("event_group") //                                                     //$NON-NLS-1$
//            || fieldName.equals("event_type") //                                                      //$NON-NLS-1$
//            || fieldName.equals("first_lap_index") //                                                 //$NON-NLS-1$
//            || fieldName.equals("lap_trigger") //                                                     //$NON-NLS-1$
//            || fieldName.equals("message_index") //                                                   //$NON-NLS-1$
//            || fieldName.equals("num_laps") //                                                        //$NON-NLS-1$
//            || fieldName.equals("num_sessions") //                                                    //$NON-NLS-1$
//            || fieldName.equals("sport") //                                                           //$NON-NLS-1$
//            || fieldName.equals("sport_index") //                                                     //$NON-NLS-1$
//            || fieldName.equals("sub_sport") //                                                       //$NON-NLS-1$
//            || fieldName.equals("total_cycles") //                                                    //$NON-NLS-1$
//            || fieldName.equals("trigger") //                                                         //$NON-NLS-1$
//            || fieldName.equals("type") //                                                            //$NON-NLS-1$
//
//            // Altitude
//
//            || fieldName.equals("altitude") //                                                        //$NON-NLS-1$
//            || fieldName.equals("enhanced_altitude") //                                               //$NON-NLS-1$
//            || fieldName.equals("min_altitude") //                     376.79999999999995 m           //$NON-NLS-1$
//            || fieldName.equals("max_altitude") //                                  295.0 m           //$NON-NLS-1$
//            || fieldName.equals("avg_altitude") //                     144.39999999999998 m           //$NON-NLS-1$
//            || fieldName.equals("enhanced_avg_altitude") //            144.39999999999998 m           //$NON-NLS-1$
//            || fieldName.equals("enhanced_min_altitude") //                                           //$NON-NLS-1$
//            || fieldName.equals("enhanced_max_altitude") //                                           //$NON-NLS-1$
//            || fieldName.equals("total_ascent") //                                                    //$NON-NLS-1$
//            || fieldName.equals("total_descent") //                                                   //$NON-NLS-1$
//
//            // Bike
//
//            || fieldName.equals("bike_weight") //                                    10.0 kg          //$NON-NLS-1$
//            || fieldName.equals("custom_wheelsize") //                               2.16 m           //$NON-NLS-1$
//            || fieldName.equals("cycle_length") //                                   0.0 m            //$NON-NLS-1$
//            || fieldName.equals("resistance") //                                       0              //$NON-NLS-1$
//
//            // Cadence
//
//            || fieldName.equals("cadence") //                                                         //$NON-NLS-1$
//            || fieldName.equals("fractional_cadence") //                                              //$NON-NLS-1$
//            || fieldName.equals("avg_cadence") //                                                     //$NON-NLS-1$
//            || fieldName.equals("avg_fractional_cadence") //                                          //$NON-NLS-1$
//            || fieldName.equals("max_cadence") //                                                     //$NON-NLS-1$
//            || fieldName.equals("max_fractional_cadence") //                                          //$NON-NLS-1$
//
//            // Calories
//
//            || fieldName.equals("calories") //                                        46 kcal         //$NON-NLS-1$
//            || fieldName.equals("total_calories") //                                                  //$NON-NLS-1$
//            || fieldName.equals("total_fat_calories") //                                              //$NON-NLS-1$
//
//            // Distance
//
//            || fieldName.equals("distance") //                                                        //$NON-NLS-1$
//            || fieldName.equals("odometer") //                                    47851.0 m           //$NON-NLS-1$
//            || fieldName.equals("total_distance") //                                                  //$NON-NLS-1$
//
//            // HR
//
//            || fieldName.equals("heart_rate") //                                                      //$NON-NLS-1$
//            || fieldName.equals("avg_heart_rate") //                                                  //$NON-NLS-1$
//            || fieldName.equals("min_heart_rate") //                                   62 bpm         //$NON-NLS-1$
//            || fieldName.equals("max_heart_rate") //                                                  //$NON-NLS-1$
//            || fieldName.equals("default_max_biking_heart_rate") //                                   //$NON-NLS-1$
//            || fieldName.equals("default_max_heart_rate") //                                          //$NON-NLS-1$
//            || fieldName.equals("resting_heart_rate") //                                0 bpm         //$NON-NLS-1$
//            || fieldName.equals("threshold_heart_rate") //                              0             //$NON-NLS-1$
//
//            || fieldName.equals("hr_calc_type") //                                                    //$NON-NLS-1$
//            || fieldName.equals("time_in_hr_zone") //                                 9.0 s           //$NON-NLS-1$
//
//            // Gear
//
//            || fieldName.equals("front_gear") //                                                      //$NON-NLS-1$
//            || fieldName.equals("front_gear_num") //                                                  //$NON-NLS-1$
//            || fieldName.equals("rear_gear") //                                                       //$NON-NLS-1$
//            || fieldName.equals("rear_gear_num") //                                                   //$NON-NLS-1$
//
//            // Grade
//
//            || fieldName.equals("grade") //                                                           //$NON-NLS-1$
//            || fieldName.equals("avg_grade") //                                       0.0 %           //$NON-NLS-1$
//            || fieldName.equals("avg_pos_grade") //                                   3.0 %           //$NON-NLS-1$
//            || fieldName.equals("avg_neg_grade") //                                   2.0 %           //$NON-NLS-1$
//            || fieldName.equals("max_pos_grade") //                                  12.0 %           //$NON-NLS-1$
//            || fieldName.equals("max_neg_grade") //                                  10.0 %           //$NON-NLS-1$
//
//            // Position
//
//            || fieldName.equals("position_lat") //                                                    //$NON-NLS-1$
//            || fieldName.equals("position_long") //                                                   //$NON-NLS-1$
//            || fieldName.equals("start_position_lat") //                                              //$NON-NLS-1$
//            || fieldName.equals("start_position_long") //                                             //$NON-NLS-1$
//            || fieldName.equals("end_position_lat") //                                                //$NON-NLS-1$
//            || fieldName.equals("end_position_long") //                                               //$NON-NLS-1$
//
//            || fieldName.equals("nec_lat") //                                                         //$NON-NLS-1$
//            || fieldName.equals("nec_long") //                                                        //$NON-NLS-1$
//            || fieldName.equals("swc_lat") //                                                         //$NON-NLS-1$
//            || fieldName.equals("swc_long") //                                                        //$NON-NLS-1$
//
//            // Power
//
//            || fieldName.equals("power") //                                                           //$NON-NLS-1$
//            || fieldName.equals("accumulated_power") //                                               //$NON-NLS-1$
//            || fieldName.equals("left_right_balance") //                                              //$NON-NLS-1$
//            || fieldName.equals("left_torque_effectiveness") //                                       //$NON-NLS-1$
//            || fieldName.equals("right_torque_effectiveness") //                                      //$NON-NLS-1$
//            || fieldName.equals("left_pedal_smoothness") //                                           //$NON-NLS-1$
//            || fieldName.equals("right_pedal_smoothness") //                                          //$NON-NLS-1$
//            || fieldName.equals("avg_left_torque_effectiveness") //                  68.0 percent     //$NON-NLS-1$
//            || fieldName.equals("avg_left_pedal_smoothness") //                      20.5 percent     //$NON-NLS-1$
//            || fieldName.equals("avg_right_pedal_smoothness") //                     51.0 percent     //$NON-NLS-1$
//
//            || fieldName.equals("functional_threshold_power") //                                      //$NON-NLS-1$
//            || fieldName.equals("pwr_calc_type") //                                                   //$NON-NLS-1$
//
//            || fieldName.equals("total_work") //                                    78109 J           //$NON-NLS-1$
//            || fieldName.equals("time_in_power_zone") //                           10.001 s           //$NON-NLS-1$
//            || fieldName.equals("avg_power") //                                       315 watts       //$NON-NLS-1$
//            || fieldName.equals("max_power") //                                       955 watts       //$NON-NLS-1$
//            || fieldName.equals("normalized_power") //                                330 watts       //$NON-NLS-1$
//            || fieldName.equals("threshold_power") //                                 184 watts       //$NON-NLS-1$
//
//            // Running dynamics
//
//            || fieldName.equals("stance_time") //                                   253.0 ms          //$NON-NLS-1$
//            || fieldName.equals("stance_time_percent") //                           34.75 percent     //$NON-NLS-1$
//            || fieldName.equals("stance_time_balance") //                           51.31 percent     //$NON-NLS-1$
//            || fieldName.equals("step_length") //                                  1526.0 mm          //$NON-NLS-1$
//            || fieldName.equals("avg_step_length") //                                                 //$NON-NLS-1$
//            || fieldName.equals("vertical_oscillation") //                         105.2  mm          //$NON-NLS-1$
//            || fieldName.equals("vertical_ratio") //                                 8.96 percent     //$NON-NLS-1$
//            || fieldName.equals("avg_stance_time") //                                                 //$NON-NLS-1$
//            || fieldName.equals("avg_stance_time_balance") //                                         //$NON-NLS-1$
//            || fieldName.equals("avg_stance_time_percent") //                                         //$NON-NLS-1$
//            || fieldName.equals("avg_vertical_oscillation") //                    103.8   mm          //$NON-NLS-1$
//            || fieldName.equals("avg_vertical_ratio") //                            8.17  percent     //$NON-NLS-1$
//            || fieldName.equals("avg_vam") //                                       0.348 m/s         //$NON-NLS-1$
//            || fieldName.equals("user_running_step_length") //                        0.0 m           //$NON-NLS-1$
//            || fieldName.equals("user_walking_step_length") //                        0.0 m           //$NON-NLS-1$
//
//            // Speed
//
//            || fieldName.equals("speed") //                                                           //$NON-NLS-1$
//            || fieldName.equals("enhanced_speed") //                                                  //$NON-NLS-1$
//            || fieldName.equals("enhanced_avg_speed") //                                              //$NON-NLS-1$
//            || fieldName.equals("enhanced_max_speed") //                                              //$NON-NLS-1$
//            || fieldName.equals("max_speed") //                                                       //$NON-NLS-1$
//            || fieldName.equals("compressed_speed_distance") //                                       //$NON-NLS-1$
//
//            || fieldName.equals("avg_speed") //                                                       //$NON-NLS-1$
//            || fieldName.equals("vertical_speed") //                               -0.018 m/s         //$NON-NLS-1$
//            || fieldName.equals("avg_pos_vertical_speed") //                          0.0 m/s         //$NON-NLS-1$
//            || fieldName.equals("avg_neg_vertical_speed") //                          0.0 m/s         //$NON-NLS-1$
//            || fieldName.equals("max_pos_vertical_speed") //                          0.0 m/s         //$NON-NLS-1$
//            || fieldName.equals("max_neg_vertical_speed") //                          1.0 m/s         //$NON-NLS-1$
//
//            // Swimming
//
//            || fieldName.equals("filtered_bpm") //                                    118 bpm         //$NON-NLS-1$
//
//            || fieldName.equals("avg_swimming_cadence") //                             20 strokes/min  //$NON-NLS-1$
//            || fieldName.equals("length_type") //                                       1              //$NON-NLS-1$
//            || fieldName.equals("swim_stroke") //                                       2 swim_stroke  //$NON-NLS-1$
//            || fieldName.equals("total_strokes") //                                    12 strokes      //$NON-NLS-1$
//
//            || fieldName.equals("pool_length") //                                    25.0 m            //$NON-NLS-1$
//            || fieldName.equals("pool_length_unit") //                                  0              //$NON-NLS-1$
//
//            // Swimming lap data
//
//            || fieldName.equals("first_length_index") //                               54             //$NON-NLS-1$
//            || fieldName.equals("avg_stroke_distance") //                            2.25 m           //$NON-NLS-1$
//            || fieldName.equals("num_lengths") //                                      16 lengths     //$NON-NLS-1$
//            || fieldName.equals("num_active_lengths") //                               16 lengths     //$NON-NLS-1$
//
//            // Temperature
//
//            || fieldName.equals("temperature") //                                                     //$NON-NLS-1$
//            || fieldName.equals("avg_temperature") //                                  28 C           //$NON-NLS-1$
//            || fieldName.equals("max_temperature") //                                  29 C           //$NON-NLS-1$
//
//            // Training
//
//            || fieldName.equals("intensity") //                                                       //$NON-NLS-1$
//            || fieldName.equals("intensity_factor") //                              0.847 if          //$NON-NLS-1$
//            || fieldName.equals("lactate_threshold_autodetect_enabled") //              1             //$NON-NLS-1$
//            || fieldName.equals("total_anaerobic_training_effect") //                 0.0             //$NON-NLS-1$
//            || fieldName.equals("total_training_effect") //                           3.0             //$NON-NLS-1$
//            || fieldName.equals("training_stress_score") //                         119.5 tss         //$NON-NLS-1$
//
//            // User
//
//            || fieldName.equals("age") //                                                             //$NON-NLS-1$
//            || fieldName.equals("gender") //                                                          //$NON-NLS-1$
//            || fieldName.equals("height") //                                                          //$NON-NLS-1$
//            || fieldName.equals("weight") //                                                          //$NON-NLS-1$
//            || fieldName.equals("language") //                                                        //$NON-NLS-1$
//
//            //////////////////////////////////////////////////////////////////////////
//            //////////////////////////////////////////////////////////////////////////
//            //////////////////////////////////////////////////////////////////////////
//
//            // Device
//
//            || fieldName.equals("device_info") //                                                     //$NON-NLS-1$
//            || fieldName.equals("device_index") //                                                    //$NON-NLS-1$
//            || fieldName.equals("device_type") //                                                     //$NON-NLS-1$
//            || fieldName.equals("manufacturer") //                                                    //$NON-NLS-1$
//            || fieldName.equals("serial_number") //                                                   //$NON-NLS-1$
//            || fieldName.equals("product") //                                                         //$NON-NLS-1$
//            || fieldName.equals("product_name") //                          Wahoo Fitness             //$NON-NLS-1$
//            || fieldName.equals("source_type") //                                                     //$NON-NLS-1$
//            || fieldName.equals("favero_product") //                                                  //$NON-NLS-1$
//            || fieldName.equals("garmin_product") //                                                  //$NON-NLS-1$
//            || fieldName.equals("software_version") //                                                //$NON-NLS-1$
//            || fieldName.equals("hardware_version") //                                                //$NON-NLS-1$
//
//            //   battery_status
//            //
//            //         1  new
//            //         2  good
//            //         3  ok
//            //         4  low
//            //         5  critical
//            //         7  unknown
//
//            || fieldName.equals("battery_status") //                                                  //$NON-NLS-1$
//            || fieldName.equals("battery_voltage") //                                                 //$NON-NLS-1$
//            || fieldName.equals("sensor_position") //                                                 //$NON-NLS-1$
//            || fieldName.equals("descriptor") //                                  Android             //$NON-NLS-1$
//            || fieldName.equals("antplus_device_type") //                         Android             //$NON-NLS-1$
//            || fieldName.equals("ant_device_type") //                             Android             //$NON-NLS-1$
//            || fieldName.equals("ant_device_number") //                                               //$NON-NLS-1$
//            || fieldName.equals("ant_network") //                                                     //$NON-NLS-1$
//            || fieldName.equals("ant_transmission_type") //                                           //$NON-NLS-1$
//            || fieldName.equals("cum_operating_time") //                                              //$NON-NLS-1$
//
//            // OTHER fields
//
//            || fieldName.equals("activity_tracker_enabled") //                          1             //$NON-NLS-1$
//            || fieldName.equals("absolute_pressure") //                              966 Pa           //$NON-NLS-1$
//            || fieldName.equals("auto_activity_detect") //                              1             //$NON-NLS-1$
//            || fieldName.equals("autosync_min_steps") //                             2000 steps       //$NON-NLS-1$
//            || fieldName.equals("autosync_min_time") //                               240 minutes     //$NON-NLS-1$
//            || fieldName.equals("backlight_mode") //                                    3             //$NON-NLS-1$
//            || fieldName.equals("display_orientation") //                                             //$NON-NLS-1$
//            || fieldName.equals("friendly_name") //                                                   //$NON-NLS-1$
//            || fieldName.equals("mounting_side") //                                     1             //$NON-NLS-1$
//            || fieldName.equals("move_alert_enabled") //                                0             //$NON-NLS-1$
//            || fieldName.equals("time_mode") //                                         1             //$NON-NLS-1$
//            || fieldName.equals("wkt_name") //                               Cardio Class             //$NON-NLS-1$
//
//            // FIT fields
//
//            || fieldName.equals("application_id") //                                  102             //$NON-NLS-1$
//            || fieldName.equals("application_version") //                              54             //$NON-NLS-1$
//            || fieldName.equals("capabilities") //                                      1             //$NON-NLS-1$
//            || fieldName.equals("developer_data_index") //                              0             //$NON-NLS-1$
//            || fieldName.equals("duration_value") //                               180000             //$NON-NLS-1$
//            || fieldName.equals("duration_type") //                                     0             //$NON-NLS-1$
//            || fieldName.equals("field_definition_number") //                           0             //$NON-NLS-1$
//            || fieldName.equals("field_name") //                                    Power             //$NON-NLS-1$
//            || fieldName.equals("fit_base_type_id") //                                  2             //$NON-NLS-1$
//            || fieldName.equals("native_field_num") //                                 41             //$NON-NLS-1$
//            || fieldName.equals("native_mesg_num") //                                  20             //$NON-NLS-1$
//            || fieldName.equals("num_valid_steps") //                                   3             //$NON-NLS-1$
//            || fieldName.equals("number") //                                         2056             //$NON-NLS-1$
//            || fieldName.equals("units") //                                  Milliseconds             //$NON-NLS-1$
//
//            // Settings
//
//            || fieldName.equals("dist_setting") //                                                    //$NON-NLS-1$
//            || fieldName.equals("elev_setting") //                                                    //$NON-NLS-1$
//            || fieldName.equals("height_setting") //                                    0             //$NON-NLS-1$
//            || fieldName.equals("hr_setting") //                                                      //$NON-NLS-1$
//            || fieldName.equals("position_setting") //                                                //$NON-NLS-1$
//            || fieldName.equals("power_setting") //                                                   //$NON-NLS-1$
//            || fieldName.equals("speed_setting") //                                                   //$NON-NLS-1$
//            || fieldName.equals("temperature_setting") //                                             //$NON-NLS-1$
//            || fieldName.equals("weight_setting") //                                                  //$NON-NLS-1$
//
//            // Other
//
//            || fieldName.equals("") //                                                                //$NON-NLS-1$
//            || fieldName.equals("unknown") //                                                         //$NON-NLS-1$

      ;

      return isSkipped;
   }

   /**
    * <pre>
    *
    *    Java            #       Timestamp                                                  Num      Name      Value Units
    *
    *    Message  104   Device Battery (not documented)  Fields: 5
    *    [FitDataReader] 0       1989-12-31T01:00+01:00[Europe/Berlin]      631065600   0   253   unknown  961412304
    *    [FitDataReader] 1       1989-12-31T01:00+01:00[Europe/Berlin]      631065600   0     0   unknown       4168
    *    [FitDataReader] 2       1989-12-31T01:00+01:00[Europe/Berlin]      631065600   0     1   unknown       -112
    *    [FitDataReader] 3       1989-12-31T01:00+01:00[Europe/Berlin]      631065600   0     2   unknown         88
    *    [FitDataReader] 4       1989-12-31T01:00+01:00[Europe/Berlin]      631065600   0     3   unknown         49
    *
    *    ...
    *
    *    Message  104   Device Battery (not documented)  Fields: 5
    *    [FitDataReader] 270     1989-12-31T01:00+01:00[Europe/Berlin]      631065600   0   253   unknown  961428506
    *    [FitDataReader] 271     1989-12-31T01:00+01:00[Europe/Berlin]      631065600   0     0   unknown       3955
    *    [FitDataReader] 272     1989-12-31T01:00+01:00[Europe/Berlin]      631065600   0     1   unknown       -105
    *    [FitDataReader] 273     1989-12-31T01:00+01:00[Europe/Berlin]      631065600   0     2   unknown         69
    *    [FitDataReader] 274     1989-12-31T01:00+01:00[Europe/Berlin]      631065600   0     3   unknown         50
    * </pre>
    *
    * @param mesg
    * @param fitData
    */
   @SuppressWarnings("unused")
   private void onMesg_104_DeviceBattery_0(final Mesg mesg, final FitData fitData) {

      final Field fieldTime = mesg.getField(253);
      final Field field_0 = mesg.getField(0);

      // ensure both fields are available otherwise the data are out of sync
      if (fieldTime != null && field_0 != null) {

         final Object fieldValue_Time = fieldTime.getValue();
         final Object fieldValue_0 = field_0.getValue();

         if (fieldValue_Time instanceof Long && fieldValue_0 instanceof Integer) {

            final Long garminTimestamp = (Long) fieldValue_Time;
            final Integer batteryPercentage = (Integer) fieldValue_0;

            final long javaTime = FitUtils.convertGarminTimeToJavaTime(garminTimestamp);

            fitData.getBattery_Time().add(javaTime);
            fitData.getBattery_Percentage().add(batteryPercentage.shortValue());
         }
      }
   }

   private void onMesg_104_DeviceBattery_2(final Mesg mesg, final FitData fitData) {

      final Field fieldTime = mesg.getField(253);
      final Field fieldPercentage = mesg.getField(2);

      // ensure both fields are available otherwise the data are out of sync
      if (fieldTime != null && fieldPercentage != null) {

         final Object fieldValue_Time = fieldTime.getValue();
         final Object fieldValue_Percentage = fieldPercentage.getValue();

         if (fieldValue_Time instanceof Long && fieldValue_Percentage instanceof Short) {

            final Long garminTimestamp = (Long) fieldValue_Time;
            final Short batteryPercentage = (Short) fieldValue_Percentage;

            final long javaTime = FitUtils.convertGarminTimeToJavaTime(garminTimestamp);

            fitData.getBattery_Time().add(javaTime);
            fitData.getBattery_Percentage().add(batteryPercentage.shortValue());
         }
      }
   }

   private void onMesg_ForDebugLogging(final Mesg mesg) {

      long garminTimestamp = 0;

      final int mesgNum = mesg.getNum();

      final boolean isSkipMessage = false

//            || mesgNum == 13 //     13    unknown
//            || mesgNum == 22 //     22    unknown
//            || mesgNum == 79 //     79    unknown
//            || mesgNum == 113 //    113   unknown
//            || mesgNum == 140 //    140   unknown
//            || mesgNum == 216 //    216   unknown
//            || mesgNum == 288 //    288   unknown
//
//            || mesgNum == 34 //     ACTIVITY
//            || mesgNum == 23 //     DEVICE_INFO
//            || mesgNum == 2 //      DEVICE_SETTINGS
//            || mesgNum == 21 //     EVENT
//            || mesgNum == 0 //      FILE_ID
//            || mesgNum == 49 //     FILE_CREATOR
//            || mesgNum == 78 //     HRV
//            || mesgNum == 19 //     LAP
//            || mesgNum == 20 //     RECORD
//            || mesgNum == 18 //     SESSION
//            || mesgNum == 12 //     SPORT
//            || mesgNum == 3 //      USER_PROFILE
//            || mesgNum == 7 //      ZONES_TARGET
//
//            || mesgNum == 104 //    Device Battery (not documented)
//            || mesgNum == 147 //    Registered Device Sensor (not documented)

      ;

      if (isSkipMessage == false) {

         String messageName = MesgNum.getStringFromValue(mesgNum);

         if (messageName.length() == 0) {

            switch (mesgNum) {
            case 104:
               messageName = "Device Battery (not documented)"; //$NON-NLS-1$
               break;

            case 147:
               messageName = "Registered Device Sensor (not documented)"; //$NON-NLS-1$
               break;

            default:
               messageName = mesg.getName();
               break;
            }

         }

         final boolean isLogMessageHeader = true;
         if (isLogMessageHeader) {

            System.out.println(String.format("Message  %3d   %s  Fields: %d", //$NON-NLS-1$

                  mesgNum,
                  messageName,
                  mesg.getNumFields()));
         }
      }

      for (final Field field : mesg.getFields()) {

         final String fieldName = field.getName();

         if (fieldName.equals("timestamp")) { //$NON-NLS-1$
            garminTimestamp = (Long) field.getValue();
         }

         /*
          * Set fields which should NOT be displayed in the log
          */

         if (StringUtils.isNullOrEmpty(fieldName)

               || isSkipMessage
               || isFieldSkipped(fieldName)

         ) {

            continue;
         }

         final long javaTime = FitUtils.convertGarminTimeToJavaTime(garminTimestamp);

         final String logMessage = String.format(UI.EMPTY_STRING

               + "[%s]" //       Java class name      //$NON-NLS-1$

               + " %-7s" //      #                    //$NON-NLS-1$

               + " %-42s %-10d  %-10s  " //  time     //$NON-NLS-1$

               + " %5d" //       Num                  //$NON-NLS-1$
               + " %40s" //      Name                 //$NON-NLS-1$
               + " %20s" //      Value                //$NON-NLS-1$
               + " %-12s" //     Units                //$NON-NLS-1$

//             + " %s" //        RawValue             //$NON-NLS-1$

               + UI.EMPTY_STRING,

               FitDataReader.class.getSimpleName(),

               _logCounter++,

               TimeTools.getZonedDateTime(javaTime), //  show readable date/time
               javaTime / 1000, //                       java time in s
               Long.toString(garminTimestamp), //        garmin timestamp

               field.getNum(), //         Num
               fieldName, //              Name

               field.getValue(), //       Value
               field.getUnits() //        Units

//             field.getRawValue().getClass().getCanonicalName()

         );

         System.out.println(logMessage);
      }

      for (final DeveloperField field : mesg.getDeveloperFields()) {

         final String fieldName = field.getName();

         if (fieldName.equals("timestamp")) { //$NON-NLS-1$
            garminTimestamp = (Long) field.getValue();
         }

         /*
          * Set fields which should NOT be displayed in the log
          */
         if (StringUtils.isNullOrEmpty(fieldName)

//               // Developer fields
//
//               || fieldName.equals("Cadence") //                                          91 RPM            //$NON-NLS-1$
//               || fieldName.equals("Elevation") //                                       315 Meters         //$NON-NLS-1$
//               || fieldName.equals("Form Power") //                                       32 Watts          //$NON-NLS-1$
//               || fieldName.equals("Ground Time") //                                     660 Milliseconds   //$NON-NLS-1$
//               || fieldName.equals("Leg Spring Stiffness") //                            0.0 kN/m           //$NON-NLS-1$
//               || fieldName.equals("Power") //                                           112 Watts          //$NON-NLS-1$
//               || fieldName.equals("Vertical Oscillation") //                            0.0 Centimeters    //$NON-NLS-1$

         //
         ) {
            continue;
         }

         final long javaTime = FitUtils.convertGarminTimeToJavaTime(garminTimestamp);

         System.out.println(String.format(UI.EMPTY_STRING

               + "[%s]" //                   Java class name   //$NON-NLS-1$

               + " %-7s" //                  #                 //$NON-NLS-1$

               + " %-42s %-10d  %-10s  " //  time              //$NON-NLS-1$

               + " %5d" //                   Num               //$NON-NLS-1$
               + " %-40s" //                 Name              //$NON-NLS-1$
               + " %20s" //                  Value             //$NON-NLS-1$
               + " %-12s" //                 Units             //$NON-NLS-1$

//             + " %s" //                    RawValue          //$NON-NLS-1$

               + UI.EMPTY_STRING,

               FitDataReader.class.getSimpleName(),

               _logCounter++,

               TimeTools.getZonedDateTime(javaTime), //  show readable date/time
               javaTime / 1000, //                       java time in s
               Long.toString(garminTimestamp), //        garmin timestamp

               field.getNum(), //                        Num
               fieldName, //                             Name

               field.getValue(), //                      Value
               field.getUnits() //                       Units

//             field.getRawValue().getClass().getCanonicalName()

         ));
      }
   }

   private void onMesg_ForNotDocumentedMesg(final Mesg mesg, final FitData fitData) {

      final int mesgNum = mesg.getNum();

      switch (mesgNum) {
      case 104:
         onMesg_104_DeviceBattery_2(mesg, fitData);
         break;

      case 147:
         // Registered Device Sensor (not documented)
         break;

      default:
         break;
      }
   }

   @Override
   public boolean processDeviceData(final String importFilePath,
                                    final DeviceData deviceData,
                                    final Map<Long, TourData> alreadyImportedTours,
                                    final Map<Long, TourData> newlyImportedTours,
                                    final boolean isReimport) {

      boolean returnValue = false;

      try (FileInputStream fileInputStream = new FileInputStream(importFilePath)) {

         final MesgBroadcaster fitBroadcaster = new MesgBroadcaster(new Decode());

         final FitData fitData = new FitData(
               this,
               importFilePath,
               alreadyImportedTours,
               newlyImportedTours);

         // setup all fit listeners
         fitBroadcaster.addListener(new MesgListener_Activity(fitData));
         fitBroadcaster.addListener(new MesgListener_BikeProfile(fitData));
         fitBroadcaster.addListener(new MesgListener_DeviceInfo(fitData));
         fitBroadcaster.addListener(new MesgListener_Event(fitData));
         fitBroadcaster.addListener(new MesgListener_FileCreator(fitData));
         fitBroadcaster.addListener(new MesgListener_FileId(fitData));
         fitBroadcaster.addListener(new MesgListener_Hr(fitData));
         fitBroadcaster.addListener(new MesgListener_Hrv(fitData));
         fitBroadcaster.addListener(new MesgListener_Lap(fitData));
         fitBroadcaster.addListener(new MesgListener_Length(fitData));
         fitBroadcaster.addListener(new MesgListener_Record(fitData));
         fitBroadcaster.addListener(new MesgListener_Session(fitData));
         fitBroadcaster.addListener(new MesgListener_Sport(fitData));
         fitBroadcaster.addListener((MesgListener) mesg -> onMesg_ForNotDocumentedMesg(mesg, fitData));

         if (_isLogging_FitData) {

            // show debug info

            System.out.println();
            System.out.println();
            System.out.println((System.currentTimeMillis() + " [" + getClass().getSimpleName() + "]") + (" \t" + importFilePath)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            System.out.println();

// SET_FORMATTING_OFF
//          System.out.println("         1         2         3         4         5         6         7         8         9         0         1         2         3");
//          System.out.println("1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890");
// SET_FORMATTING_ON

            // show log header
            System.out.println(String.format(UI.EMPTY_STRING

                  + "%-16s" //   Java        //$NON-NLS-1$
                  + "%-8s" //    #           //$NON-NLS-1$
                  + "%-70s" //   Timestamp   //$NON-NLS-1$
                  + "%-5s" //    Num         //$NON-NLS-1$
                  + "%39s" //    Name        //$NON-NLS-1$
                  + "%21s" //    Value       //$NON-NLS-1$
                  + " %s", //    Units       //$NON-NLS-1$

                  "Java", //                 //$NON-NLS-1$
                  "#", //                    //$NON-NLS-1$
                  "Timestamp", //            //$NON-NLS-1$
                  "Num", //                  //$NON-NLS-1$
                  "Name", //                 //$NON-NLS-1$
                  "Value", //                //$NON-NLS-1$
                  "Units" //                 //$NON-NLS-1$

            ));

            System.out.println();

            _logCounter = 0;

            // add debug logger which is listening to all events
            fitBroadcaster.addListener((MesgListener) this::onMesg_ForDebugLogging);
         }

         fitBroadcaster.run(fileInputStream);

         fitData.finalizeTour();

         returnValue = true;

      } catch (final IOException e) {
         TourLogManager.logError_CannotReadDataFile(importFilePath, e);
      }

      return returnValue;
   }

   @Override
   public boolean validateRawData(final String fileName) {

      boolean returnValue = false;

      try (FileInputStream fis = new FileInputStream(fileName)) {

         if (!FilenameUtils.getExtension(fileName).equalsIgnoreCase("fit")) { //$NON-NLS-1$
            return false;
         }

         returnValue = new Decode().checkFileIntegrity(fis);

         if (returnValue) {

            // log version if not yet done

            if (_isVersionLogged == false) {

               TourLogManager.logInfo(
                     String.format(
                           "FIT SDK %d.%d", //$NON-NLS-1$
                           Fit.PROFILE_VERSION_MAJOR,
                           Fit.PROFILE_VERSION_MINOR));

               _isVersionLogged = true;
            }

         } else {

            TourLogManager.logError(
                  String.format(
                        "FIT checkFileIntegrity failed '%s' - FIT SDK %d.%d", //$NON-NLS-1$
                        fileName,
                        Fit.PROFILE_VERSION_MAJOR,
                        Fit.PROFILE_VERSION_MINOR));
         }

      } catch (final FileNotFoundException e) {
         TourLogManager.logError_CannotReadDataFile(fileName, e);
      } catch (final FitRuntimeException e) {
         TourLogManager.logEx(String.format("Invalid data file '%s'", fileName), e); //$NON-NLS-1$
      } catch (final IOException e) {
         TourLogManager.logEx(e);
      }

      return returnValue;
   }

}
