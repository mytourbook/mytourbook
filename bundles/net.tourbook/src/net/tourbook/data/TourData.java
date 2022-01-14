/*******************************************************************************
 * Copyright (C) 2005, 2022 Wolfgang Schramm and Contributors
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
package net.tourbook.data;

import static javax.persistence.CascadeType.ALL;
import static javax.persistence.FetchType.EAGER;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skedgo.converter.TimezoneMapper;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.hash.TIntHashSet;

import java.awt.Point;
import java.io.File;
import java.io.PrintStream;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PostLoad;
import javax.persistence.PostUpdate;
import javax.persistence.Transient;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import net.tourbook.Messages;
import net.tourbook.algorithm.DPPoint;
import net.tourbook.algorithm.DouglasPeuckerSimplifier;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.map.GeoPosition;
import net.tourbook.common.swimming.SwimStroke;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.time.TourDateTime;
import net.tourbook.common.util.MtMath;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.common.weather.IWeather;
import net.tourbook.database.FIELD_VALIDATION;
import net.tourbook.database.TourDatabase;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.importdata.TourbookDevice;
import net.tourbook.math.Smooth;
import net.tourbook.photo.Photo;
import net.tourbook.photo.PhotoCache;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.srtm.ElevationSRTM3;
import net.tourbook.srtm.GeoLat;
import net.tourbook.srtm.GeoLon;
import net.tourbook.srtm.NumberForm;
import net.tourbook.tour.BreakTimeResult;
import net.tourbook.tour.BreakTimeTool;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.photo.TourPhotoLink;
import net.tourbook.tour.photo.TourPhotoManager;
import net.tourbook.ui.tourChart.ChartLabelMarker;
import net.tourbook.ui.tourChart.ChartLayer2ndAltiSerie;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.views.ISmoothingAlgorithm;
import net.tourbook.ui.views.tourDataEditor.TourDataEditorView;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.hibernate.annotations.Cascade;

/**
 * Tour data contains all data for a tour (except markers), an entity will be saved in the database
 */
@Entity
@XmlType(name = "TourData")
@XmlRootElement(name = "TourData")
@XmlAccessorType(XmlAccessType.NONE)
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "tourId")
public class TourData implements Comparable<Object>, IXmlSerializable, Cloneable {

   private static final char           NL                                = UI.NEW_LINE;
   private static final String         INTERVAL_SUMMARY_UNIT             = " ∑  ";                                  //$NON-NLS-1$

   public static final int             DB_LENGTH_DEVICE_TOUR_TYPE        = 2;
   public static final int             DB_LENGTH_DEVICE_PLUGIN_ID        = 255;
   public static final int             DB_LENGTH_DEVICE_PLUGIN_NAME      = 255;
   public static final int             DB_LENGTH_DEVICE_MODE_NAME        = 255;
   public static final int             DB_LENGTH_DEVICE_FIRMWARE_VERSION = 255;

   public static final int             DB_LENGTH_TOUR_TITLE              = 255;
   public static final int             DB_LENGTH_TOUR_DESCRIPTION        = 4096;
   public static final int             DB_LENGTH_TOUR_DESCRIPTION_V10    = 32000;
   public static final int             DB_LENGTH_TOUR_START_PLACE        = 255;
   public static final int             DB_LENGTH_TOUR_END_PLACE          = 255;
   public static final int             DB_LENGTH_TOUR_IMPORT_FILE_PATH   = 255;
   public static final int             DB_LENGTH_TOUR_IMPORT_FILE_NAME   = 255;
   public static final int             DB_LENGTH_TIME_ZONE_ID            = 255;

   public static final int             DB_LENGTH_WEATHER                 = 1000;
   public static final int             DB_LENGTH_WEATHER_CLOUDS          = 255;

   public static final int             DB_LENGTH_POWER_DATA_SOURCE       = 255;

   /**
    * <pre>
    *
    *   decimal  decimal
    *   places   degrees      DMS                  qualitative scale that           N/S or E/W      E/W at         E/W at       E/W at
    *                                              can be identified                at equator      23N/S          45N/S        67N/S
    *
    *    0       1.0          1° 00′ 0″       country or large region            111.32   km   102.47   km     78.71  km    43.496  km
    *    1       0.1          0° 06′ 0″       large city or district              11.132  km    10.247  km      7.871 km     4.3496 km
    *    2       0.01         0° 00′ 36″      town or village                      1.1132 km     1.0247 km    787.1   m    434.96   m
    *    3       0.001        0° 00′ 3.6″     neighborhood, street               111.32   m    102.47   m      78.71  m     43.496  m
    *    4       0.0001       0° 00′ 0.36″    individual street, land parcel      11.132  m     10.247  m       7.871 m      4.3496 m
    *    5       0.00001      0° 00′ 0.036″   individual trees                     1.1132 m      1.0247 m     787.1   mm   434.96   mm
    *    6       0.000001     0° 00′ 0.0036″  individual humans                  111.32   mm   102.47   mm     78.71  mm    43.496  mm
    *
    * https://en.wikipedia.org/wiki/Decimal_degrees
    *
    * Factor to normalize lat/lon
    *
    * Degree * INT    resolution
    * ---------------------------------------
    * Deg * 100      1570 m
    * Deg * 1000      157 m
    * Deg * 10000      16 m
    * Deg * 100000    1.6 m
    * </pre>
    */
   public static final double          MAX_GEO_DIFF                      = 0.0001;

   public static final double          NORMALIZED_LATITUDE_OFFSET        = 90.0;
   public static final int             NORMALIZED_LATITUDE_OFFSET_E2     = 9000;
   public static final double          NORMALIZED_LONGITUDE_OFFSET       = 180.0;
   public static final int             NORMALIZED_LONGITUDE_OFFSET_E2    = 18000;

   private static final String         TIME_ZONE_ID_EUROPE_BERLIN        = "Europe/Berlin";                         //$NON-NLS-1$

   public static final int             MIN_TIMEINTERVAL_FOR_MAX_SPEED    = 20;

   public static final float           MAX_BIKE_SPEED                    = 120f;

   /**
    * Number of defined hr zone fields which is currently {@link #hrZone0} ... {@link #hrZone9} = 10
    */
   public static final int             MAX_HR_ZONES                      = 10;

   public static final Float           RUN_DYN_DATA_MULTIPLIER           = 100f;

   public static final short           SURFING_VALUE_IS_NOT_SET          = -1;

   /**
    * Device Id for manually created tours
    */
   public static final String          DEVICE_ID_FOR_MANUAL_TOUR         = "manual";                                //$NON-NLS-1$

   /**
    * Device id for csv files which behave like manually created tours, marker and timeslices are
    * disabled because they are not available, tour duration can be edited<br>
    * this is the id of the deviceDataReader
    */
   public static final String          DEVICE_ID_CSV_TOUR_DATA_READER    = "net.tourbook.device.CSVTourDataReader"; //$NON-NLS-1$

   /**
    * THIS IS NOT UNUSED !!!<br>
    * <br>
    * it initializes SRTM
    */
   @Transient
   private static final NumberForm     srtmNumberForm                    = new NumberForm();

   @Transient
   private static final ElevationSRTM3 _elevationSRTM3                   = new ElevationSRTM3();

   @Transient
   private static IPreferenceStore     _prefStore                        = TourbookPlugin.getPrefStore();

// SET_FORMATTING_OFF

   /**
    * Unique entity id which identifies the tour
    */
   @Id
   private Long                  tourId;

   // ############################################# DATE #############################################

   /**
    * Tour start time in milliseconds since 1970-01-01T00:00:00Z
    *
    * @since DB version 22
    */
   @XmlElement
   private long                  tourStartTime;

   /**
    * Tour end time in milliseconds since 1970-01-01T00:00:00Z
    *
    * @since DB version 22
    */
   @XmlElement
   private long                  tourEndTime;

   /**
    * year of tour start
    */
   private short                 startYear;

   /**
    * mm (d) month of tour
    */
   private short                 startMonth;

   /**
    * dd (d) day of tour
    */
   private short                 startDay;

   /**
    * HH (d) hour of tour
    */
   private short                 startHour;

   /**
    * MM (d) minute of tour
    */
   private short                 startMinute;

   /**
    *
    */
   private int                   startSecond;                                          // db-version 7

   /**
    * Calendar week of the tour.
    * <p>
    * This is used in sql queries.
    */
   private short                 startWeek;

   /**
    * Year for {@link #startWeek}, this is mostly the {@link #startYear} but it can be the year
    * before or after depending when a week starts and ends.
    * <p>
    * This is used in sql queries.
    */
   private short                 startWeekYear;

   // ############################################# TIME #############################################

   /**
    * Total elapsed time in seconds
    *
    * @since Is long since db version 22, before it was int
    */
   @XmlElement
   private long                  tourDeviceTime_Elapsed;

   /**
    * Total recorded time in seconds
    */
   private long                  tourDeviceTime_Recorded;

   /**
    * Total paused time in seconds
    *
    * This number could come from a direct value or from {@link tourTimerPauses}
    */
   private long                  tourDeviceTime_Paused;

   /**
    * Total moving time in seconds
    *
    * @since Is long since db version 22, before it was int
    */
   @XmlElement
   private long                  tourComputedTime_Moving;

   /**
    * Time zone ID or <code>null</code> when the time zone ID is not available.
    */
   private String                timeZoneId;

   // ############################################# DISTANCE #############################################

   /**
    * Total distance of the device at tour start (km) tttt (h). Distance for the tour is stored in
    * the field {@link #tourDistance}
    */
   private float                 startDistance;

   /**
    * total distance of the tour in meters (metric system), this value is computed from the
    * distance data serie
    */
   @XmlElement
   private float                 tourDistance;

   /**
    * Are the distance values measured with a distance sensor or with lat/lon values.<br>
    * <br>
    * 0 == false <i>(default, no distance sensor)</i> <br>
    * 1 == true
    */
   private short                 isDistanceFromSensor            = 0;                     // db-version 8

   // ############################################# ELEVATION #############################################

   /**
    * aaaa (h) initial altitude (m)
    */
   private short                 startAltitude;

   /**
    * altitude up (m)
    */
   @XmlElement
   private int                   tourAltUp;

   /**
    * altitude down (m)
    */
   @XmlElement
   private int                   tourAltDown;


   /**
    * Average altitude change (m/km)
    */
   private int                   avgAltitudeChange;

   // ############################################# PULSE/WEIGHT/POWER #############################################

   /**
    * pppp (h) initial pulse (bpm)
    */
   private short                 startPulse;

   @XmlElement
   private int                   restPulse;                                             // db-version 8

   @XmlElement
   private Integer               calories;                                             // db-version 4

   private float                 bodyWeight;                                          // db-version 4

   private float                 bodyFat;

   /**
    * A flag indicating that the power is from a sensor. This is the state of the device which is
    * not related to the availability of power data. Power data should be available but is not
    * checked.<br>
    * <br>
    * 0 == false, 1 == true
    */
   private int                   isPowerSensorPresent            = 0;                     // db-version 12

   // ############################################# PULSE #############################################

   /**
    * Average pulse, this data can also be set from device data and pulse data are not available
    *
    * @since is float since db version 21, before it was int
    */
   @XmlElement
   private float                 avgPulse;                                             // db-version 4

   /**
    * Maximum pulse for the current tour.
    *
    * @since is float since db version 21, before it was int
    */
   @XmlElement
   private float                 maxPulse;                                             // db-version 4

   /**
    * Number of HR zones which are available for this tour, is 0 when HR zones are not defined.
    */
   private int                   numberOfHrZones               = 0;                     // db-version 18
   /**
    * Time for all HR zones are contained in {@link #hrZone0} ... {@link #hrZone9}. Each tour can
    * have up to 10 HR zones, when HR zone value is <code>-1</code> then this zone is not set.
    * <p>
    * These values are used in the statistic views.
    */
   private int                   hrZone0                        = -1;                     // db-version 16
   private int                   hrZone1                        = -1;                     // db-version 16
   private int                   hrZone2                        = -1;                     // db-version 16
   private int                   hrZone3                        = -1;                     // db-version 16
   private int                   hrZone4                        = -1;                     // db-version 16
   private int                   hrZone5                        = -1;                     // db-version 16
   private int                   hrZone6                        = -1;                     // db-version 16
   private int                   hrZone7                        = -1;                     // db-version 16
   private int                   hrZone8                        = -1;                     // db-version 16
   private int                   hrZone9                        = -1;                     // db-version 16

   /**
    * Time spent (in seconds) in the "Slow" cadence zone (for example: Hiking).
    */
   private int                   cadenceZone_SlowTime                        = 0;                     // db-version 40
   /**
    * Time spent (in seconds) in the "Fast" cadence zone (for example: Running).
    */
   private int                   cadenceZone_FastTime                        = 0;                     // db-version 40
   /**
    * The delimiter used when computing the existing values of cadenceZone_SlowTime & cadenceZone_FastTime
    */
   private int                   cadenceZones_DelimiterValue;

   /**
    * A flag indicating that the pulse is from a sensor. This is the state of the device which is
    * not related to the availability of pulse data. Pulse data should be available but is not
    * checked.<br>
    * <br>
    * 0 == false, 1 == true
    */
   private int                   isPulseSensorPresent            = 0;                     // db-version 12

   // ############################################# DEVICE TOUR TYPE #############################################

   /**
    * tt (h) type of tour <br>
    * "2E" bike2 (CM414M) <br>
    * "3E" bike1 (CM414M) <br>
    * "81" jogging <br>
    * "91" ski <br>
    * "A1" bike<br>
    * "B1" ski-bike
    */
   @Column(length = 2)
   private String                deviceTourType;

   /**
    * Visible name for the used profile which is defined in {@link #deviceMode}, e.g. Jogging,
    * Running, Bike1, Bike2...
    */
   private String                deviceModeName;                                       // db-version 4

   /**
    * maximum altitude in metric system
    *
    * @since is float since db version 21, before it was int
    */
   @XmlElement
   private float                 maxAltitude;                                          // db-version 4

   // ############################################# MAX VALUES #############################################

   /**
    * maximum speed in metric system
    */
   @XmlElement
   private float                 maxSpeed;                                             // db-version 4

   /**
    * maximum pace in metric system
    */
   private float                 maxPace;

   // ############################################# AVERAGE VALUES #############################################

   /**
    * @since is float since db version 21, before it was int
    */
   @XmlElement
   private float                 avgCadence;                                             // db-version 4

   /**
    * Average temperature with metric system
    *
    * @since Is float since db version 21, before it was int. In db version 20 this field was
    *        already float but not the database field.
    */
   private float                 avgTemperature;                                          // db-version 4

   // ############################################# WEATHER #############################################

   /**
    * Is <code>true</code> when the weather data below is from the weather API and not
    * manually entered or from the device.
    */
   private boolean               isWeatherDataFromApi = false;

   private int                   weatherWindDir;                                       // db-version 8
   private int                   weatherWindSpd;                                       // db-version 8
   private String                weatherClouds;                                        // db-version 8

   private String                weather;                                              // db-version 13

   /**
    * Humidity in percentage (%)
    */
   private short                 weather_Humidity;                                     // db-version 39

   /**
    * Precipitation in millimeters
    */
   private float                 weather_Precipitation;                                // db-version 39

   /**
    * Atmospheric pressure in millibars (mb)
    */
   private float                 weather_Pressure;                                     // db-version 39

   private float                 weather_Temperature_Min;                              // db-version 39
   private float                 weather_Temperature_Max;                              // db-version 39
   private float                 weather_Temperature_WindChill;                        // db-version 39


   // ############################################# POWER #############################################

   /** Unit is Watt */
   private float                 power_Avg;

   /** Unit is Watt */
   private int                   power_Max;

   /** Unit is Watt */
   private int                   power_Normalized;

   /** Functional Threshold Power (FTP) */
   private int                   power_FTP;

   /** Total work in Joule */
   private long                  power_TotalWork;
   private float                 power_TrainingStressScore;
   private float                 power_IntensityFactor;

   private int                   power_PedalLeftRightBalance;
   private float                 power_AvgLeftTorqueEffectiveness;
   private float                 power_AvgRightTorqueEffectiveness;
   private float                 power_AvgLeftPedalSmoothness;
   private float                 power_AvgRightPedalSmoothness;

   private String                power_DataSource;

   // ############################################# TRAINING #############################################

   /**
    * Naming variants from Jesús Pérez
    * <ul>
    * <li>Impact of training</li>
    * <li>Impact of effort</li>
    * <li>Hardness of effort</li>
    * <li>Training requirement</li>
    * </ul>
    * <p>
    * Manufacturer names
    * <ul>
    * <li>Garmin: TotalTrainingEffect</li>
    * <li>Suunto: PeakTrainingEffect</li>
    * </ul>
    *
    * @param trainingEffect
    */
   private float                 training_TrainingEffect_Aerob;                        // db-version 38
   private float                 training_TrainingEffect_Anaerob;                      // db-version 38
   private float                 training_TrainingPerformance;                         // db-version 38

   // ############################################# OTHER TOUR/DEVICE DATA #############################################

   @XmlElement
   private String                tourTitle;                                            // db-version 4

   @XmlElement
   private String                tourDescription;                                      // db-version 4

   @XmlElement
   private String                tourStartPlace;                                       // db-version 4

   @XmlElement
   private String                tourEndPlace;                                         // db-version 4

   /**
    * Date/Time when tour data was created. This value is set to the tour start date before db
    * version 11, otherwise the value is set when the tour is saved the first time.
    * <p>
    * Data format: YYYYMMDDhhmmss
    */
   private long                  dateTimeCreated;                                      // db-version 11

   /**
    * Date/Time when tour data was modified, default value is 0
    * <p>
    * Data format: YYYYMMDDhhmmss
    */
   private long                  dateTimeModified;                                     // db-version 11

   /** Folder path from the import file. */
   private String                tourImportFilePath;                                   // db-version 6

   /** File name from the import file. */
   private String                tourImportFileName;                                   // db-version 29

   /**
    * Tolerance for the Douglas Peucker algorithm.
    */
   private short                 dpTolerance                     = 50;                 // 5.0 since version 14.7

   /**
    * Time difference in seconds between 2 time slices or <code>-1</code> for GPS devices when the
    * time slices has variable time duration
    */
   private short                 deviceTimeInterval               = -1;                // db-version 3

   /**
    * Scaling factor for the temperature data serie, e.g. when set to 10 the temperature data serie
    * is multiplied by 10, default scaling is <code>1</code>
    */
   /*
    * disabled when float was introduces in 11.after8, preserved in database that older ejb objects
    * can be loaded
    */
   private int                   temperatureScale               = 1;                   // db-version 13

   /**
    * Firmware version of the device
    */
   private String                deviceFirmwareVersion;                                // db-version 12

   /**
    * This value is multiplied with the cadence data serie when displayed, cadence data serie is
    * always saved with rpm.
    * <p>
    * 1.0f = Revolutions per minute (RPM) <br>
    * 2.0f = Steps per minute (SPM)
    */
   private float                  cadenceMultiplier               = RawDataManager.getCadenceMultiplierDefaultValue().getMultiplier();

   /**
    * When <code>1</code> then a stride sensor is available.
    * <p>
    * 0 == false, 1 == true
    */
   private short                  isStrideSensorPresent            = 0;

   // ############################################# MERGED DATA #############################################

   /**
    * when a tour is merged with another tour, {@link #mergeSourceTourId} contains the tour id of
    * the tour which is merged into this tour
    */
   private Long                  mergeSourceTourId;                                    // db-version 7

   /**
    * when a tour is merged into another tour, {@link #mergeTargetTourId} contains the tour id of
    * the tour into which this tour is merged
    */
   private Long                  mergeTargetTourId;                                    // db-version 7

   /**
    * positive or negative time offset in seconds for the merged tour
    */
   private int                   mergedTourTimeOffset;                                 // db-version 7

   /**
    * altitude difference for the merged tour
    */
   private int                   mergedAltitudeOffset;                                 // db-version 7

   /**
    * Unique plugin id for the device data reader which created this tour, this id is defined in
    * plugin.xml
    * <p>
    * a better name would be <i>pluginId</i>
    */
   private String                devicePluginId;

   // ############################################# PLUGIN DATA #############################################

   /**
    * Visible name for the used {@link TourbookDevice}, this name is defined in plugin.xml
    * <p>
    * a better name would be <i>pluginName</i>
    */
   private String                devicePluginName;                                     // db-version 4

   /**
    * Deflection point in the conconi test, this value is the index for the data serie on the
    * x-axis
    */
   private int                   conconiDeflection;

   // ############################################# PHOTO  DATA #############################################

   /**
    * Number of photos.
    */
   @SuppressWarnings("unused")
   private int                   numberOfPhotos;

   /**
    * Number of time slices in {@link #timeSerie}
    */
   private int                   numberOfTimeSlices;

   /**
    * Time adjustment in seconds, this is an average value for all photos.
    */
   private int                   photoTimeAdjustment;

   // ############################################# GEARS #############################################

   private int                   frontShiftCount;
   private int                   rearShiftCount;

   // ############################################# RUNNING DYNAMICS #######################################

   private short                 runDyn_StanceTime_Min;
   private short                 runDyn_StanceTime_Max;
   private float                 runDyn_StanceTime_Avg;

   private short                 runDyn_StanceTimeBalance_Min;
   private short                 runDyn_StanceTimeBalance_Max;
   private float                 runDyn_StanceTimeBalance_Avg;

   private short                 runDyn_StepLength_Min;
   private short                 runDyn_StepLength_Max;
   private float                 runDyn_StepLength_Avg;

   private short                 runDyn_VerticalOscillation_Min;
   private short                 runDyn_VerticalOscillation_Max;
   private float                 runDyn_VerticalOscillation_Avg;

   private short                 runDyn_VerticalRatio_Min;
   private short                 runDyn_VerticalRatio_Max;
   private float                 runDyn_VerticalRatio_Avg;

   // ############################################# SURFING #######################################

   // -1 indicate that the value is not yet set

   private short                 surfing_NumberOfEvents        = 0; // must be 0 because of totals in tourbook view
   private short                 surfing_MinSpeed_StartStop    = SURFING_VALUE_IS_NOT_SET;
   private short                 surfing_MinSpeed_Surfing      = SURFING_VALUE_IS_NOT_SET;
   private short                 surfing_MinTimeDuration       = SURFING_VALUE_IS_NOT_SET;

   private boolean               surfing_IsMinDistance;
   private short                 surfing_MinDistance           = SURFING_VALUE_IS_NOT_SET;

   // ############################################# GEO BOUNDS #############################################

   /**
    * Is <code>true</code> when latitude/longitude data are available
    */
   private boolean               hasGeoData;

   // ############################################# BATTERY #############################################

   /**
    * Battery start/end values of the used recording device
    *
    * -1 indicate that the value is not yet set
    */
   private short                 battery_Percentage_Start      = -1;
   private short                 battery_Percentage_End        = -1;

   // ############################################# UNUSED FIELDS - START #############################################
   /**
    * ssss distance msw
    * <p>
    * is not used any more since 6.12.2006 but it's necessary then it's a field in the database
    */
   @SuppressWarnings("unused")
   private int                      distance;

   @SuppressWarnings("unused")
   private float                    deviceAvgSpeed;                     // db-version 12

   @SuppressWarnings("unused")
   private int                      deviceDistance;

   /**
    * Profile id which is defined by the device
    */
   @SuppressWarnings("unused")
   private short                    deviceMode;                           // db-version 3

   @SuppressWarnings("unused")
   private int                      deviceTotalUp;

   @SuppressWarnings("unused")
   private int                      deviceTotalDown;

   @SuppressWarnings("unused")
   private long                     deviceTravelTime;

   @SuppressWarnings("unused")
   private int                      deviceWheel;

   @SuppressWarnings("unused")
   private int                      deviceWeight;

   // ############################################# UNUSED FIELDS - END #############################################

   /**
    * All data series for time, altitude,... A BLOB CANNOT BE MULTIPLE !
    */
   @Basic(optional = false)
   private SerieData                   serieData;

   // ############################################# ASSOCIATED ENTITIES #############################################

   /**
    * Photos for this tour
    */
   @OneToMany(fetch = FetchType.EAGER, cascade = ALL, mappedBy = "tourData")
   @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
   private Set<TourPhoto>              tourPhotos                          = new HashSet<>();

   /**
    * Tour marker
    */
   @OneToMany(fetch = FetchType.EAGER, cascade = ALL, mappedBy = "tourData")
   @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
   @XmlElementWrapper(name = "TourMarkers")
   @XmlElement(name = "TourMarker")
   private Set<TourMarker>             tourMarkers                         = new HashSet<>();

   /**
    * Contains the tour way points
    */
   @OneToMany(fetch = FetchType.EAGER, cascade = ALL, mappedBy = "tourData")
   @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
   private final Set<TourWayPoint>     tourWayPoints                       = new HashSet<>();

   /**
    * Reference tours
    */
   @OneToMany(fetch = FetchType.EAGER, cascade = ALL, mappedBy = "tourData")
   @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
   private final Set<TourReference>    tourReferences                     = new HashSet<>();

   /**
    * Tags
    */
   @ManyToMany(fetch = EAGER)
   @JoinTable(inverseJoinColumns = @JoinColumn(name = "TOURTAG_TagID", referencedColumnName = "TagID"))
   private Set<TourTag>                tourTags                            = new HashSet<>();

   /**
    * Sensors
    */
   @OneToMany(fetch = FetchType.EAGER, cascade = ALL, mappedBy = "tourData")
   @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
   private Set<DeviceSensorValue>     deviceSensorValues                  = new HashSet<>();

//   /**
//    * SharedMarker
//    */
//   @ManyToMany(fetch = EAGER)
//   @JoinTable(inverseJoinColumns = @JoinColumn(name = "SHAREDMARKER_SharedMarkerID", referencedColumnName = "SharedMarkerID"))
//   private Set<SharedMarker>                           sharedMarker                  = new HashSet<SharedMarker>();

   /**
    * Category of the tour, e.g. bike, mountainbike, jogging, inlinescating
    */
   @ManyToOne
   private TourType                    tourType;

   /**
    * Person which created this tour or <code>null</code> when the tour is not saved in the
    * database.
    * <p>
    * SQL access to this field:
    * <pre>tourPerson_personId</pre>
    */
   @ManyToOne
   private TourPerson                  tourPerson;

   /**
    * plugin id for the device which was used for this tour Bike used for this tour
    */
   @ManyToOne
   private TourBike                    tourBike;

   /**
    * <br>
    * <br>
    * <br>
    * <br>
    * <br>
    * ################################### TRANSIENT DATA ########################################
    * <br>
    * <br>
    * <br>
    * <br>
    * <br>
    * <br>
    */

   /**
    * Contains time in <b>seconds</b> relative to the tour start which is defined in
    * {@link #tourStartTime}.
    * <p>
    * The array {@link #timeSerie} is <code>null</code> for a manually created tour, it is
    * <b>always</b> set when tour is from a device or an imported file.
    */
   @Transient
   public int[]               timeSerie;

   /**
    * Contains the absolute distance in m (metric system) or <code>null</code> when not available
    */
   @Transient
   public float[]             distanceSerie;

   /**
    * Distance values with double type to display it on the x-axis
    */
   @Transient
   private double[]           distanceSerieDouble_Kilometer;

   /**
    * Contains the absolute distance in miles
    */
   @Transient
   private double[]           distanceSerieDouble_Mile;

   /**
    * Contains the absolute distance in nautical miles
    */
   @Transient
   private double[]           distanceSerieDouble_NauticalMile;

   /**
    * Contains the absolute elevation in meter (metric system) or <code>null</code> when not
    * available.
    */
   @Transient
   public float[]             altitudeSerie;

   /**
    * Smoothed elevation serie is used to display the tour chart when not <code>null</code>
    */
   @Transient
   private float[]            altitudeSerieSmoothed;

   /**
    * Contains the absolute elevation in feet (imperial system)
    */
   @Transient
   private float[]               altitudeSerieImperial;

   /**
    * Smoothed elevation serie is used to display the tour chart when not <code>null</code>
    */
   @Transient
   private float[]               altitudeSerieImperialSmoothed;

   /**
    * SRTM elevation values, when <code>null</code> srtm data have not yet been attached, when
    * <code>length()==0</code> data are invalid.
    */
   @Transient
   private float[]               srtmSerie;

   @Transient
   private float[]               srtmSerieImperial;

   @Transient
   private float[]               cadenceSerie;

   /**
    * Pulse values from the device
    */
   @Transient
   public float[]                pulseSerie;

   @Transient
   private float[]               _pulseSerie_Smoothed;

   /**
    * Pulse values computed from the pulse times in {@link #pulseTime_Milliseconds}.
    * One pulse value is the average of all pulse times within one timeslice.
    */
   @Transient
   public float[]                pulseSerie_FromTime;

   /**
    * One time slice contains all of it's R-R interval values.
    */
   @Transient
   public String[]               pulseSerie_RRIntervals;

   /**
    * This value is contained in the saved {@link SerieData}
    * <p>
    * Pulse times in milliseconds.
    * <p>
    * <b>This data serie has not the same serie length as the other data series because 1 second can
    * have multiple values, depending on the heartrate.</b>
    */
   @Transient
   public int[]                  pulseTime_Milliseconds;

   /**
    * This value is contained in the saved {@link SerieData}
    * <p>
    * Contains the time index into {@link #timeSerie} for the pulse time(s) in {@link #pulseTime_Milliseconds}.
    * A time index value can be -1 when there is no pulse time within a second -> heartbeat value is below 60 bpm.
   */
   @Transient
   public int[]                  pulseTime_TimeIndex;

   /**
    * Contains <code>true</code> or <code>false</code> for each time slice of the whole tour.
    * <code>true</code> is set when a time slice is a break.
    */
   @Transient
   private boolean[]             breakTimeSerie;

   /**
    * Contains the temperature in the metric measurement system.
    */
   @Transient
   public float[]                temperatureSerie;

   /**
    * contains the temperature in the imperial measurement system
    */
   @Transient
   private float[]               temperatureSerieImperial;

   /**
    * Speed in km/h
    * <p>
    * The metric speed serie is required when computing the power even if the current measurement
    * system is imperial
    */
   @Transient
   private float[]               speedSerie;

   @Transient
   private float[]               speedSerie_Mile;

   @Transient
   private float[]               speedSerie_NauticalMile;

   /**
    * Summarized average speed in km/h from the first time slice
    */
   @Transient
   private float[]               speedSerie_Summarized;

   @Transient
   private float[]               speedSerie_Summarized_Mile;

   @Transient
   private float[]               speedSerie_Summarized_NauticalMile;

   /**
    * Is <code>true</code> when the data in {@link #speedSerie} are from the device and not
    * computed. Speed data are normally available from an ergometer and not from a bike computer
    */
   @Transient
   private boolean               isSpeedSerieFromDevice               = false;

   /**
    * Pace in sec/km
    */
   @Transient
   private float[]               paceSerie_Seconds;

   /**
    * Pace in sec/mile
    */
   @Transient
   private float[]               paceSerie_Seconds_Imperial;

   /**
    * Pace in min/km
    */
   @Transient
   private float[]               paceSerie_Minute;

   /**
    * Pace in min/mile
    */
   @Transient
   private float[]               paceSerie_Minute_Imperial;

   /**
    * Summarized average pace in sec/km from the first time slice
    */
   @Transient
   private float[]               paceSerie_Summarized_Seconds;

   /**
    * Summarized average pace in sec/mile from the first time slice
    */
   @Transient
   private float[]               paceSerie_Summarized_Seconds_Imperial;

   /**
    * Summarized average pace in min/km from the first time slice
    */
   @Transient
   private float[]               paceSerie_Summarized_Minute;

   /**
    * Summarized average pace in min/mile from the first time slice
    */
   @Transient
   private float[]               paceSerie_Summarized_Minute_Imperial;

   @Transient
   private float[]               powerSerie;

   /**
    * Is <code>true</code> when the data in {@link #powerSerie} are from the device and not
    * computed. Power data source can be an ergometer or a power sensor
    */
   @Transient
   private boolean               isPowerSerieFromDevice               = false;

   @Transient
   private float[]               altimeterSerie;

   @Transient
   private float[]               altimeterSerieImperial;

   @Transient
   public float[]                gradientSerie;

   @Transient
   public float[]                tourCompareSerie;

   /*
    * GPS data
    */
   /**
    * Contains tour latitude data or <code>null</code> when GPS data are not available.
    */
   @Transient
   public double[]               latitudeSerie;

   @Transient
   public double[]               longitudeSerie;

   /**
    * Gears which are saved in a tour are in this HEX format (left to right)
    * <p>
    * Front teeth<br>
    * Front gear number<br>
    * Back teeth<br>
    * Back gear number<br>
    * <code>
    * <pre>
   final long   frontTeeth   = (gearRaw &gt;&gt; 24 &amp; 0xff);
   final long   frontGear   = (gearRaw &gt;&gt; 16 &amp; 0xff);
   final long   rearTeeth   = (gearRaw &gt;&gt; 8 &amp; 0xff);
   final long   rearGear   = (gearRaw &gt;&gt; 0 &amp; 0xff);
    * </pre>
    * </code>
    */
   @Transient
   public long[]                 gearSerie;

   /**
    * Gears have this format:
    * <p>
    * _gears[0] = gear ratio<br>
    * _gears[1] = front gear teeth<br>
    * _gears[2] = rear gear teeth<br>
    * _gears[3] = front gear number, starting with 1<br>
    * _gears[4] = rear gear number, starting with 1<br>
    */
   @Transient
   private float[][]             _gears;

   /**
    * Contains the bounds of the tour in latitude/longitude:
    * <p>
    * 1st item contains lat/lon minimum values<br>
    * 2nd item contains lat/lon maximum values<br>
    */
   @Transient
   private GeoPosition[]         _geoBounds;

   /**
    * Is <code>true</code> when geo bounds are checked
    */
   @Transient
   private boolean               _isGeoBoundsChecked;

   /**
    * Contains the rough geo parts of the tour or <code>null</code> when geo data are not available. A
    * grid square is an integer of lat + 90° and lon + 180° multiplied by 100 (1570 m)
    *
    * <pre>

      int latPart = (int) (latitude * 100);
      int lonPart = (int) (longitude * 100);

      lat      ( -90 ... + 90) * 100 =  -9_000 +  9_000 = 18_000
      lon      (-180 ... +180) * 100 = -18_000 + 18_000 = 36_000

      max      (9_000 + 9_000) * 100_000 = 18_000 * 100_000  = 1_800_000_000

                                 Integer.MAX_VALUE = 2_147_483_647

      Factor to normalize lat/lon

      Degree * Integer = Resolution
      ---------------------------------------
      Deg *     100      1570   m
      Deg *   1_000       157   m
      Deg *  10_000        16   m
      Deg * 100_000         1.6 m

    * </pre>
    */
   @Transient
   public int[]               geoGrid;

   /**
    * Latitude/longitude multiplied with {@link #_normalizedGeoAccuracy}
    */
   @Transient
   private NormalizedGeoData  _rasterizedLatLon;

   @Transient
   private int                _normalizedGeoAccuracy;

   /**
    * Index of the segmented data in the data series
    */
   @Transient
   public int[]               segmentSerieIndex;

   /**
    *
    */
   @Transient
   public int[]               segmentSerieFilter;

   /**
    * 2nd Index of the segmented data in the data series.
    * <p>
    * {@link #segmentSerieIndex} contains the outer index, this contains the inner index.
    * <p>
    * This is used, first to create the segments by the outer attribute, e.g. tour marker and then
    * create the inner segments, e.g. altitude with DP.
    */
   @Transient
   public int[]               segmentSerieIndex2nd;

   /**
    * oooo (o) DD-record // offset
    */
   @Transient
   public int                 offsetDDRecord;
   /*
    * data for the tour segments
    */
   @Transient
   private int[]              segmentSerie_Time_Total;
   @Transient
   private int[]              segmentSerie_Time_Elapsed;
   @Transient
   private int[]              segmentSerie_Time_Recorded;
   @Transient
   private int[]              segmentSerie_Time_Paused;
   @Transient
   public int[]               segmentSerie_Time_Moving;

   @Transient
   private int[]              segmentSerie_Time_Break;
   @Transient
   private float[]            segmentSerie_Distance_Diff;
   @Transient
   private float[]            segmentSerie_Distance_Total;
   @Transient
   public float[]             segmentSerie_Altitude_Diff;
   @Transient
   public float[]             segmentSerie_Altitude_Diff_Computed;
   @Transient
   public float[]             segmentSerie_Altitude_UpDown_Hour;
   @Transient
   public float               segmentSerieTotal_Altitude_Down;

   @Transient
   public float               segmentSerieTotal_Altitude_Up;
   @Transient
   public float[]             segmentSerie_Speed;
   @Transient
   public float[]             segmentSerie_Cadence;
   @Transient
   public float[]             segmentSerie_Pace;
   @Transient
   public float[]             segmentSerie_Pace_Diff;
   @Transient
   public float[]             segmentSerie_Power;
   @Transient
   public float[]             segmentSerie_Gradient;

   @Transient
   public float[]             segmentSerie_Pulse;

   /**
    * Keep original import file path, this is used when the tour file should be deleted.
    */
   @Transient
   public String              importFilePathOriginal;

   /**
    * Latitude for the center position in the map or {@link Double#MIN_VALUE} when the position is
    * not set
    */
   @Transient
   public double              mapCenterPositionLatitude            = Double.MIN_VALUE;

   /**
    * Longitude for the center position in the map or {@link Double#MIN_VALUE} when the position is
    * not set
    */
   @Transient
   public double              mapCenterPositionLongitude            = Double.MIN_VALUE;

   /**
    * Zoomlevel in the map
    */
   @Transient
   public int                 mapZoomLevel;

   /**
    * caches the world positions for the tour lat/long values for each zoom level
    */
   @Transient
   private final TIntObjectHashMap<Point[]>                    _tourWorldPosition   = new TIntObjectHashMap<>();

   /**
    * caches the world positions for the way point lat/long values for each zoom level
    */
   @Transient
   private final TIntObjectHashMap<TIntObjectHashMap<Point>>   _twpWorldPosition   = new TIntObjectHashMap<>();

   /**
    * when a tour was deleted and is still visible in the raw data view, resaving the tour or
    * finding the tour in the entity manager causes lots of trouble with hibernate, therefor this
    * tour cannot be saved again, it must be reloaded from the file system
    */
   @Transient
   public boolean             isTourDeleted   = false;

   /**
    * 2nd data serie, this is used in the {@link ChartLayer2ndAltiSerie} to display the merged tour
    * or the adjusted altitude
    */
   @Transient
   public float[]             dataSerie2ndAlti;

   /**
    * altitude difference between this tour and the merge tour with metric measurement
    */
   @Transient
   public float[]             dataSerieDiffTo2ndAlti;

   /**
    * Contains the adjusted elevation serie in the current measurement system
    */
   @Transient
   public float[]             dataSerieAdjustedAlti;

   /**
    * contains special data points
    */
   @Transient
   public SplineData          splineDataPoints;

   /**
    * Contains a spline data serie
    */
   @Transient
   public float[]             dataSerieSpline;

   /**
    * when a tour is not saved, the tour id is not defined, therefore the tour data are provided
    * from the import view when tours are merged to display the merge layer
    */
   @Transient
   private TourData           _mergeSourceTourData;

   @Transient
   private ZonedDateTime      _dateTimeCreated;

   @Transient
   private ZonedDateTime      _dateTimeModified;

   /**
    * Tour start time with a time zone.
    */
   @Transient
   private ZonedDateTime      _zonedStartTime;

   /**
    * Tour markers which are sorted by serie index
    */
   @Transient
   private ArrayList<TourMarker>   _sortedMarkers;

   /**
    * Contains seconds from all hr zones: {@link #hrZone0} ... {@link #hrZone9}
    */
   @Transient
   private int[]               _hrZones;

   @Transient
   private HrZoneContext      _hrZoneContext;

   /**
    * Copy of {@link #timeSerie} with double type, this is used for the chart x-axis to support history tours
    */
   @Transient
   private double[]            timeSerieDouble;

   /**
    * Contains photo data from a {@link TourPhotoLink}.
    * <p>
    * When this field is set, photos from this photo link are displayed otherwise photos from
    * {@link #tourPhotos} are displayed.
    */
   @Transient
   public TourPhotoLink         tourPhotoLink;

   /**
    * Contains photos which are displayed in photo galleries.
    */
   @Transient
   private ArrayList<Photo>   _galleryPhotos                           = new ArrayList<>();

   /**
    *
    */
   @Transient
   public boolean             isHistoryTour;

   /**
    * Time serie for history dates, {@link Long} is used instead of {@link Integer} which is used
    * in {@link #timeSerie} but has a limit of about 67 years {@link Integer#MAX_VALUE}.
    */
   @Transient
   public long[]              timeSerieHistory;

   /**
    * Time in double precision that x-axis values are displayed at the correct position, this is
    * not the case when max chart pixels is 1'000'000'000 with floating point.
    */
   @Transient
   private double[]           timeSerieHistoryDouble;

   /**
    * Contains adjusted time serie when tour is overlapping 1. April 1893. There was a time shift
    * of 6:32 minutes when CET (Central European Time) was born.
    */
   @Transient
   private double[]           timeSerieWithTimeZoneAdjustment;

   /**
    * {@link TourData} contains multiple tours or a virtual tour or sub tours. It is created when
    * multiple tours are selected to be displayed in the {@link TourChart}.
    */
   @Transient
   private boolean            isMultipleTours;

   /**
    * Contains the tour id's
    */
   @Transient
   public Long[]              multipleTourIds;

   /**
    * Contains the tour start index in the data series for each tour.
    */
   @Transient
   public int[]               multipleTourStartIndex;

   /**
    * Contains the tour start time with a time zone for each tour.
    */
   @Transient
   public ZonedDateTime[]     multipleTourZonedStartTime;

   /**
    * Contains tour titles for each tour.
    */
   @Transient
   public String[]            multipleTourTitles;

   /**
    * Contains the number of tour markers for each tour.
    */
   @Transient
   public int[]               multipleNumberOfMarkers;

   /**
    * List with all tour markers which is used only for multiple tours. This list is required
    * because the tour markers cannot be modified and a Set with all tourmarkers is not sorted as
    * it should.
    */
   @Transient
   public ArrayList<TourMarker>   multiTourMarkers;

   /**
    * Contains the number of tour pauses for each tour.
    */
   @Transient
   public int[]               multipleNumberOfPauses;

   /**
    * List containing all the tour pauses used only for multiple tours.
    */
   @Transient
   public List<List<Long>>    multiTourPauses;


   @Transient
   public boolean             multipleTour_IsCadenceRpm;

   @Transient
   public boolean             multipleTour_IsCadenceSpm;

   /**
    * Contains the cadence multiplier for each tour to display the unit correctly.
    */
   @Transient
   public float[]             multipleTours_CadenceMultiplier;

   /**
    * Contains the swim start index in the swim data series for each tour.
    */
   @Transient
   public int[]               multipleSwimStartIndex;

   /**
    * A value is <code>true</code> when cadence is 0.
    */
   @Transient
   private boolean[]          _cadenceGaps;

   /**
    * Contains the cadence data serie when the {@link #cadenceMultiplier} != 1.0;
    */
   @Transient
   private float[]            cadenceSerieWithMultiplier;

   /**
    * Is <code>true</code> when the tour is imported and contained MT specific fields, e.g. tour
    * elapsed time, average temperature, ...
    */
   @Transient
   private boolean            _isImportedMTTour;

   /**
    * Is <code>true</code> when tour file is deleted in the device and in the backup folder.
    */
   @Transient
   public boolean             isTourFileDeleted;

   /**
    * Is <code>true</code> when the tour file is deleted in the device folder but is kept in the
    * backup folder.
    */
   @Transient
   public boolean             isTourFileMoved;

   /**
    * Is <code>true</code> when the tour import file existed in the backup folder and not in the
    * device folder.
    * <p>
    * <b>THIS FILE SHOULD NOT BE DELETED.</b>
    */
   @Transient
   public boolean             isBackupImportFile;
   /*
    * Running dynamics data
    *
    *   stance_time                  267.0  ms
    *   stance_time_balance           50.56 percent      * TourData.RUN_DYN_DATA_MULTIPLIER
    *   step_length                 1147.0  mm
    *   vertical_oscillation         107.2  mm            * TourData.RUN_DYN_DATA_MULTIPLIER
    *   vertical_ratio                 9.15 percent      * TourData.RUN_DYN_DATA_MULTIPLIER
    *
    *   stance_time                  272.0  ms
    *   stance_time_balance           50.46 percent      * TourData.RUN_DYN_DATA_MULTIPLIER
    *   step_length                 1169.0  mm
    *   vertical_oscillation         119.0  mm            * TourData.RUN_DYN_DATA_MULTIPLIER
    *   vertical_ratio                 9.84 percent      * TourData.RUN_DYN_DATA_MULTIPLIER
    *
    * @since Version 18.7
    */
   @Transient
   public short[]       runDyn_StanceTime;

   @Transient
   private float[]      _runDyn_StanceTime_UI;
   @Transient
   public short[]       runDyn_StanceTimeBalance;

   @Transient
   private float[]      _runDyn_StanceTimeBalance_UI;
   @Transient
   public short[]       runDyn_StepLength;
   @Transient
   private float[]      _runDyn_StepLength_UI;

   @Transient
   private float[]      _runDyn_StepLength_UI_Imperial;
   @Transient
   public short[]       runDyn_VerticalOscillation;
   @Transient
   private float[]      _runDyn_VerticalOscillation_UI;

   @Transient
   private float[]      _runDyn_VerticalOscillation_UI_Imperial;
   @Transient
   public short[]       runDyn_VerticalRatio;
   @Transient
   private float[]      _runDyn_VerticalRatio_UI;

   /**
    * Swimming data has a different number of time slices than the other data series !!!
    *
    * @since Version 18.10
    */

   /**
    * Swimming data: Relative time in seconds to the tour start time. Contains
    * {@link Short#MIN_VALUE} when value is not set.
    */
   @Transient
   public int[]         swim_Time;

   /**
    * Swimming data: Activity is defined in {@link LengthType} e.g. active, idle. Contains
    * {@link Short#MIN_VALUE} when value is not set.
    */
   @Transient
   public short[]       swim_LengthType;

   @Transient
   private float[]      _swim_LengthType_UI;

   /**
    * Swimming data: Number of strokes. Contains {@link Short#MIN_VALUE} when value is not set.
    */
   @Transient
   public short[]       swim_Strokes;

   @Transient
   private float[]      _swim_Strokes_UI;

   /**
    * Swimming data: Stroke style is defined in {@link SwimStroke} e.g. freestyle, breaststroke...
    * Contains {@link Short#MIN_VALUE} when value is not set.
    */
   @Transient
   public short[]       swim_StrokeStyle;

   @Transient
   private float[]      _swim_StrokeStyle_UI;

   /**
    * Swimming data: Swimming cadence in strokes/min. Contains {@link Short#MIN_VALUE} when value is
    * not set.
    */
   @Transient
   public short[]       swim_Cadence;

   @Transient
   private float[]      _swim_Cadence_UI;

   /**
    * Is <code>true</code> when {@link #cadenceSerie} is computed from swimming cadence
    * {@link #swim_Cadence} values.
    */
   @Transient
   public boolean       isSwimCadence;

   /**
    * Computed swim data serie
    */
   @Transient
   private float[]      _swim_Swolf;

   /**
    * When values are <code>true</code>, then the data are visible, otherwise they are hidden. This
    * is used to show surfing parts and to hide the none surfing parts.
    * <p>
    * With <code>null</code>, it will be ignored and all data points are visible.
    */
   @Transient
   public boolean[]     visibleDataPointSerie;

   /**
    *
    */
   @Transient
   public boolean[]     visiblePoints_ForSurfing;

   /**
    * An array containing the start time of each pause (in milliseconds)
    * A timer pause is a device event triggered by the user.
    */
   @Transient
   private long[]       pausedTime_Start;

   /**
    * An array containing the end time of each pause (in milliseconds)
    * A timer pause is a device event triggered by the user.
    */
   @Transient
   private long[]       pausedTime_End;

   /**
    * An auto-pause happened when a value is 1, otherwise it was triggered by the user.
    * This field could also be <code>null</code> when pause data are not available.
    */
   @Transient
   private long[]       pausedTime_Data;

   /**
    * Containing the battery time in seconds, relative to the tour start time
    *
    * @since after 21.6
    */
   @Transient
   private int[]         battery_Time;

   /**
    * Containing the battery percentage values
    *
    * @since after 21.6
    */
   @Transient
   private short[]       battery_Percentage;


// SET_FORMATTING_ON

   public TourData() {}

   /**
    * Add photos into this tour and save it.
    *
    * @param allNewTourPhotos
    */
   public void addPhotos(final Collection<TourPhoto> allNewTourPhotos) {

      if (allNewTourPhotos.size() > 0) {

         final HashSet<TourPhoto> currentTourPhotos = new HashSet<>(tourPhotos);

         currentTourPhotos.addAll(allNewTourPhotos);

         saveTourPhotos(currentTourPhotos);
      }
   }

   /**
    * Removed data series when the sum of all values is 0.
    */
   public void cleanupDataSeries() {

      if (timeSerie == null) {
         return;
      }

      int sumAltitude = 0;
      int sumCadence = 0;
      int sumDistance = 0;
      int sumPulse = 0;
      int sumTemperature = 0;
      int sumPower = 0;
      int sumSpeed = 0;

      int sumRunDyn_StanceTime = 0;
      int sumRunDyn_StanceTimeBalance = 0;
      int sumRunDyn_StepLength = 0;
      int sumRunDyn_VerticalOscillation = 0;
      int sumRunDyn_VerticalRatio = 0;

      double mapMinLatitude = 0;
      double mapMaxLatitude = 0;
      double mapMinLongitude = 0;
      double mapMaxLongitude = 0;

      // get FIRST VALID latitude/longitude
      hasGeoData = false;
      if ((latitudeSerie != null) && (longitudeSerie != null)) {

         for (int timeIndex = 0; timeIndex < timeSerie.length; timeIndex++) {
            if ((latitudeSerie[timeIndex] != Double.MIN_VALUE) && (longitudeSerie[timeIndex] != Double.MIN_VALUE)) {

               mapMinLatitude = mapMaxLatitude = latitudeSerie[timeIndex] + 90;
               mapMinLongitude = mapMaxLongitude = longitudeSerie[timeIndex] + 180;

               hasGeoData = true;

               break;
            }
         }
      }
      double lastValidLatitude = mapMinLatitude - 90;
      double lastValidLongitude = mapMinLongitude - 180;
      boolean isLatitudeValid = false;

      for (int serieIndex = 0; serieIndex < timeSerie.length; serieIndex++) {

         if (altitudeSerie != null) {
            sumAltitude += altitudeSerie[serieIndex];
         }

         if (cadenceSerie != null) {
            sumCadence += cadenceSerie[serieIndex];
         }
         if (distanceSerie != null) {
            sumDistance += distanceSerie[serieIndex];
         }
         if (pulseSerie != null) {
            sumPulse += pulseSerie[serieIndex];
         }
         if (temperatureSerie != null) {

            final float temp = temperatureSerie[serieIndex];

            if (temp == Float.MIN_VALUE) {
               // remove invalid values which are set temporarily
               temperatureSerie[serieIndex] = 0;
            } else {
               sumTemperature += temp < 0 ? -temp : temp;
            }
         }
         if (powerSerie != null) {
            sumPower += powerSerie[serieIndex];
         }
         if (speedSerie != null) {
            sumSpeed += speedSerie[serieIndex];
         }

         if (runDyn_StanceTime != null && sumRunDyn_StanceTime == 0) {
            sumRunDyn_StanceTime += runDyn_StanceTime[serieIndex];
         }
         if (runDyn_StanceTimeBalance != null && sumRunDyn_StanceTimeBalance == 0) {
            sumRunDyn_StanceTimeBalance += runDyn_StanceTimeBalance[serieIndex];
         }
         if (runDyn_StepLength != null && sumRunDyn_StepLength == 0) {
            sumRunDyn_StepLength += runDyn_StepLength[serieIndex];
         }
         if (runDyn_VerticalOscillation != null && sumRunDyn_VerticalOscillation == 0) {
            sumRunDyn_VerticalOscillation += runDyn_VerticalOscillation[serieIndex];
         }
         if (runDyn_VerticalRatio != null && sumRunDyn_VerticalRatio == 0) {
            sumRunDyn_VerticalRatio += runDyn_VerticalRatio[serieIndex];
         }

         if (hasGeoData) {

            final double latitude = latitudeSerie[serieIndex];
            final double longitude = longitudeSerie[serieIndex];

            if ((latitude == Double.MIN_VALUE) || (longitude == Double.MIN_VALUE)) {
               latitudeSerie[serieIndex] = lastValidLatitude;
               longitudeSerie[serieIndex] = lastValidLongitude;
            } else {
               latitudeSerie[serieIndex] = lastValidLatitude = latitude;
               longitudeSerie[serieIndex] = lastValidLongitude = longitude;
            }

            // optimized performance for Math.min/max
            final double lastValidLatAdjusted = lastValidLatitude + 90;
            final double lastValidLonAdjusted = lastValidLongitude + 180;

            mapMinLatitude = mapMinLatitude < lastValidLatAdjusted ? mapMinLatitude : lastValidLatAdjusted;
            mapMaxLatitude = mapMaxLatitude > lastValidLatAdjusted ? mapMaxLatitude : lastValidLatAdjusted;
            mapMinLongitude = mapMinLongitude < lastValidLonAdjusted ? mapMinLongitude : lastValidLonAdjusted;
            mapMaxLongitude = mapMaxLongitude > lastValidLonAdjusted ? mapMaxLongitude : lastValidLonAdjusted;

            /*
             * check if latitude is not 0, there was a bug until version 1.3.0 where latitude and
             * longitude has been saved with 0 values
             */
            if ((isLatitudeValid == false) && (lastValidLatitude != 0)) {
               isLatitudeValid = true;
            }
         }
      }

      mapMinLatitude -= 90;
      mapMaxLatitude -= 90;
      mapMinLongitude -= 180;
      mapMaxLongitude -= 180;

      /*
       * remove data series when the summary of the values is 0, for temperature this can be a
       * problem but for a longer tour the temperature varies
       */

      if (sumAltitude == 0) {
         altitudeSerie = null;
      }
      if (sumCadence == 0) {
         cadenceSerie = null;
      }
      if (sumDistance == 0) {
         distanceSerie = null;
      }
      if (sumPulse == 0) {
         pulseSerie = null;
      }
      if (sumTemperature == 0) {
         temperatureSerie = null;
      }
      if (sumPower == 0) {
         powerSerie = null;
      }
      if (sumSpeed == 0) {
         speedSerie = null;
      }

      if (powerSerie != null) {
         isPowerSerieFromDevice = true;
      }

      if (speedSerie != null) {
         isSpeedSerieFromDevice = true;
      }

      if (isLatitudeValid == false) {
         latitudeSerie = null;
         longitudeSerie = null;
         _rasterizedLatLon = null;
         geoGrid = null;
         hasGeoData = false;
      }

      if (sumRunDyn_StanceTime == 0) {
         clear_RunDyn_StanceTime();
      }

      if (sumRunDyn_StanceTimeBalance == 0) {
         clear_RunDyn_StanceTimeBalance();
      }

      if (sumRunDyn_StepLength == 0) {
         clear_RunDyn_StepLength();
      }

      if (sumRunDyn_VerticalOscillation == 0) {
         clear_RunDyn_VerticalOscillation();
      }

      if (sumRunDyn_VerticalRatio == 0) {
         clear_RunDyn_VerticalRatio();
      }
   }

   public void clear_RunDyn_StanceTime() {

      runDyn_StanceTime = null;
      runDyn_StanceTime_Min = 0;
      runDyn_StanceTime_Max = 0;
      runDyn_StanceTime_Avg = 0;

      _runDyn_StanceTime_UI = null;
   }

   public void clear_RunDyn_StanceTimeBalance() {

      runDyn_StanceTimeBalance = null;
      runDyn_StanceTimeBalance_Min = 0;
      runDyn_StanceTimeBalance_Max = 0;
      runDyn_StanceTimeBalance_Avg = 0;

      _runDyn_StanceTimeBalance_UI = null;
   }

   public void clear_RunDyn_StepLength() {

      runDyn_StepLength = null;
      runDyn_StepLength_Min = 0;
      runDyn_StepLength_Max = 0;
      runDyn_StepLength_Avg = 0;

      _runDyn_StepLength_UI = null;
      _runDyn_StepLength_UI_Imperial = null;
   }

   public void clear_RunDyn_VerticalOscillation() {

      runDyn_VerticalOscillation = null;
      runDyn_VerticalOscillation_Min = 0;
      runDyn_VerticalOscillation_Max = 0;
      runDyn_VerticalOscillation_Avg = 0;

      _runDyn_VerticalOscillation_UI = null;
      _runDyn_VerticalOscillation_UI_Imperial = null;
   }

   public void clear_RunDyn_VerticalRatio() {

      runDyn_VerticalRatio = null;
      runDyn_VerticalRatio_Min = 0;
      runDyn_VerticalRatio_Max = 0;
      runDyn_VerticalRatio_Avg = 0;

      _runDyn_VerticalRatio_UI = null;
   }

   public void clear_Swim_Cadence() {

      swim_Cadence = null;
      _swim_Cadence_UI = null;
   }

   public void clear_swim_LengthType() {

      swim_LengthType = null;
      _swim_LengthType_UI = null;
   }

   public void clear_Swim_Strokes() {

      swim_Strokes = null;
      _swim_Strokes_UI = null;
   }

   public void clear_Swim_StrokeStyle() {

      swim_StrokeStyle = null;
      _swim_StrokeStyle_UI = null;
   }

   public void clear_Swim_Time() {

      swim_Time = null;
   }

   /**
    * clear imperial altitude series so the next time when it's needed it will be recomputed
    */
   public void clearAltitudeSeries() {

      altitudeSerieSmoothed = null;

      altitudeSerieImperial = null;
      altitudeSerieImperialSmoothed = null;
   }

   /**
    * Clear computed data series so the next time, when they are needed, they are recomputed.
    */
   public void clearComputedSeries() {

      if (isSpeedSerieFromDevice == false) {
         speedSerie = null;
      }

      // this serie is always computed
      speedSerie_Summarized = null;

      speedSerie_Mile = null;
      speedSerie_NauticalMile = null;

      speedSerie_Summarized_Mile = null;
      speedSerie_Summarized_NauticalMile = null;

      if (isPowerSerieFromDevice == false) {
         powerSerie = null;
      }

      timeSerieDouble = null;
      timeSerieWithTimeZoneAdjustment = null;

      distanceSerieDouble_Kilometer = null;
      distanceSerieDouble_Mile = null;
      distanceSerieDouble_NauticalMile = null;

      breakTimeSerie = null;

      _pulseSerie_Smoothed = null;
      pulseSerie_FromTime = null;
      pulseSerie_RRIntervals = null;

      gradientSerie = null;

      paceSerie_Seconds = null;
      paceSerie_Seconds_Imperial = null;
      paceSerie_Minute = null;
      paceSerie_Minute_Imperial = null;

      paceSerie_Summarized_Seconds = null;
      paceSerie_Summarized_Seconds_Imperial = null;
      paceSerie_Summarized_Minute = null;
      paceSerie_Summarized_Minute_Imperial = null;

      altimeterSerie = null;
      altimeterSerieImperial = null;

      altitudeSerieSmoothed = null;
      altitudeSerieImperial = null;
      altitudeSerieImperialSmoothed = null;

      cadenceSerieWithMultiplier = null;

      _runDyn_StanceTime_UI = null;
      _runDyn_StanceTimeBalance_UI = null;
      _runDyn_StepLength_UI = null;
      _runDyn_StepLength_UI_Imperial = null;
      _runDyn_VerticalOscillation_UI = null;
      _runDyn_VerticalOscillation_UI_Imperial = null;
      _runDyn_VerticalRatio_UI = null;

      _swim_LengthType_UI = null;
      _swim_Cadence_UI = null;
      _swim_Strokes_UI = null;
      _swim_StrokeStyle_UI = null;
      _swim_Swolf = null;

      if (isSwimCadence) {
         // cadence is from swim cadence
         cadenceSerie = null;
      }

      srtmSerie = null;
      srtmSerieImperial = null;

      _geoBounds = null;
      _isGeoBoundsChecked = false;

      _rasterizedLatLon = null;
      geoGrid = null;
//      latitudeMinE6 = 0;
//      longitudeMinE6 = 0;
//      latitudeMaxE6 = 0;
//      longitudeMaxE6 = 0;

      _hrZones = null;
      _hrZoneContext = null;

      _gears = null;

      _cadenceGaps = null;
   }

   /**
    * Clears the cached world positions, this is necessary when the data serie have been modified
    */
   public void clearWorldPositions() {

      _tourWorldPosition.clear();
   }

   /**
    * This clone() method is cloning only a part of the tour, e.g. {@link #serieData} is not cloned
    */
   @Override
   public Object clone() throws CloneNotSupportedException {

      final TourData tourDataCopy = new TourData();

      tourDataCopy.setTourPerson(this.getTourPerson());
      tourDataCopy.setDeviceId(this.getDeviceId());

      tourDataCopy.setTourStartTime(this.getTourStartTime());

      tourDataCopy.setTourTitle(this.getTourTitle());
      tourDataCopy.setTourDescription(this.getTourDescription());

      tourDataCopy.setTourTags(new HashSet<>(this.getTourTags()));
      tourDataCopy.setTourType(this.getTourType());

      return tourDataCopy;
   }

   /**
    * The default sorting for tours are by date/time
    */
   @Override
   public int compareTo(final Object obj) {

      if (obj instanceof TourData) {

         final TourData otherTourData = (TourData) obj;

         final long tourStartTime2 = otherTourData.tourStartTime;

         return tourStartTime > tourStartTime2 ? 1 : tourStartTime < tourStartTime2 ? -1 : 0;
      }

      return 0;
   }

   /**
    * Complete tour marker with altitude/lat/lon.
    *
    * @param tourMarker
    * @param serieIndex
    */
   public void completeTourMarker(final TourMarker tourMarker, final int serieIndex) {

      if (altitudeSerie != null) {
         tourMarker.setAltitude(altitudeSerie[serieIndex]);
      }

      if (latitudeSerie != null) {
         tourMarker.setGeoPosition(latitudeSerie[serieIndex], longitudeSerie[serieIndex]);
      }
   }

   /**
    * Complete all tour marker after all data are imported.
    * <p>
    * Relative tour time must be available that the absolute time can be set.
    */
   public void completeTourMarkerWithRelativeTime() {

      for (final TourMarker tourMarker : this.getTourMarkers()) {

         final int serieIndex = tourMarker.getSerieIndex();
         final int relativeTourTime = tourMarker.getTime();

         tourMarker.setTime(relativeTourTime, tourStartTime + (relativeTourTime * 1000));

         completeTourMarker(tourMarker, serieIndex);
      }
   }

   public void computeAltimeterGradientSerie() {

      // check if needed data are available
      if (timeSerie == null || timeSerie.length < 2 || distanceSerie == null || altitudeSerie == null) {
         return;
      }

      // optimization: don't recreate the data series when they are available
      if ((altimeterSerie != null) && (altimeterSerieImperial != null) && (gradientSerie != null)) {
         return;
      }

      final String smoothingAlgo = _prefStore.getString(ITourbookPreferences.GRAPH_SMOOTHING_SMOOTHING_ALGORITHM);

      if (smoothingAlgo.equals(ISmoothingAlgorithm.SMOOTHING_ALGORITHM_JAMET)) {

         computeDataSeries_Smoothed();

      } else if (smoothingAlgo.equals(ISmoothingAlgorithm.SMOOTHING_ALGORITHM_NO_SMOOTHING)) {

         computeDataSeries_NotSmoothed();

      } else {

         if (deviceTimeInterval == -1) {
            computeAltimeterGradientSerieWithVariableInterval();
         } else {
            computeAltimeterGradientSerieWithFixedInterval();
         }
      }
   }

   /**
    * Computes the data serie for altimeters with the internal algorithm for a fix time interval
    */
   private void computeAltimeterGradientSerieWithFixedInterval() {

      final int serieLength = timeSerie.length;

      final float[] dataSerieAltimeter = new float[serieLength];
      final float[] dataSerieGradient = new float[serieLength];

      int adjustIndexLow;
      int adjustmentIndexHigh;

//      if (prefStore.getBoolean(ITourbookPreferences.GRAPH_PROPERTY_IS_VALUE_COMPUTING)) {
//
//         // use custom settings to compute altimeter and gradient
//
//         final int computeTimeSlice = prefStore.getInt(ITourbookPreferences.GRAPH_PROPERTY_CUSTOM_VALUE_TIMESLICE);
//         final int slices = computeTimeSlice / deviceTimeInterval;
//
//         final int slice2 = slices / 2;
//         adjustmentIndexHigh = (1 >= slice2) ? 1 : slice2;
//         adjustIndexLow = slice2;
//
//         // round up
//         if (adjustIndexLow + adjustmentIndexHigh < slices) {
//            adjustmentIndexHigh++;
//         }
//
//      } else {

      // use internal algorithm to compute altimeter and gradient

      if (deviceTimeInterval <= 2) {
         adjustIndexLow = 15;
         adjustmentIndexHigh = 15;

      } else if (deviceTimeInterval <= 5) {
         adjustIndexLow = 4;
         adjustmentIndexHigh = 4;

      } else if (deviceTimeInterval <= 10) {
         adjustIndexLow = 2;
         adjustmentIndexHigh = 3;
      } else {
         adjustIndexLow = 1;
         adjustmentIndexHigh = 2;
      }
//      }

      /*
       * compute values
       */

      for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

         /*
          * adjust index to the array size, this is optimized to NOT use Math.min or Math.max
          */
         final int serieLengthLow = serieLength - 1;

         final int indexLowTemp = serieIndex - adjustIndexLow;
         final int indexLowTempMax = ((0 >= indexLowTemp) ? 0 : indexLowTemp);
         final int indexLow = ((indexLowTempMax <= serieLengthLow) ? indexLowTempMax : serieLengthLow);

         final int indexHighTemp = serieIndex + adjustmentIndexHigh;
         final int indexHighTempMin = ((indexHighTemp <= serieLengthLow) ? indexHighTemp : serieLengthLow);
         final int indexHigh = ((0 >= indexHighTempMin) ? 0 : indexHighTempMin);

         final float distanceDiff = distanceSerie[indexHigh] - distanceSerie[indexLow];
         final float altitudeDiff = altitudeSerie[indexHigh] - altitudeSerie[indexLow];

         final float timeDiff = deviceTimeInterval * (indexHigh - indexLow);

         // keep altimeter data
         dataSerieAltimeter[serieIndex] = 3600 * altitudeDiff / timeDiff / UI.UNIT_VALUE_ELEVATION;

         // keep gradient data
         dataSerieGradient[serieIndex] = distanceDiff == 0 ? 0 : altitudeDiff * 100 / distanceDiff;
      }

      if (UI.UNIT_IS_ELEVATION_FOOT) {

         // set imperial system

         altimeterSerieImperial = dataSerieAltimeter;

      } else {

         // set metric system

         altimeterSerie = dataSerieAltimeter;
      }

      gradientSerie = dataSerieGradient;
   }

   /**
    * Computes the data serie for gradient and altimeters for a variable time interval
    */
   private void computeAltimeterGradientSerieWithVariableInterval() {

      final float[] checkSpeedSerie = getSpeedSerie();

      final int serieLength = timeSerie.length;
      final int serieLengthLast = serieLength - 1;

      final float[] dataSerieAltimeter = new float[serieLength];
      final float[] dataSerieGradient = new float[serieLength];

      // get minimum time/distance differences
      final int minTimeDiff = _prefStore.getInt(ITourbookPreferences.APP_DATA_SPEED_MIN_TIMESLICE_VALUE);

//      if (isCustomProperty) {
//         // use custom settings to compute altimeter and gradient
//         minTimeDiff = prefStore.getInt(ITourbookPreferences.GRAPH_PROPERTY_CUSTOM_VALUE_TIMESLICE);
//      } else {
//         // use internal algorithm to compute altimeter and gradient
//         minTimeDiff = 16;
//      }

      final int minDistanceDiff = minTimeDiff;

      final boolean checkPosition = (latitudeSerie != null) && (longitudeSerie != null);

      for (int serieIndex = 1; serieIndex < serieLength; serieIndex++) {

         if (checkSpeedSerie[serieIndex] == 0) {
            // continue when no speed is available
//            dataSerieAltimeter[serieIndex] = 2000;
            continue;
         }

         final int sliceTimeDiff = timeSerie[serieIndex] - timeSerie[serieIndex - 1];

         // check if a lat and long diff is available
         if (checkPosition && (serieIndex > 0) && (serieIndex < serieLengthLast - 1) && (sliceTimeDiff > 10)) {

            if ((latitudeSerie[serieIndex] == latitudeSerie[serieIndex - 1])
                  && (longitudeSerie[serieIndex] == longitudeSerie[serieIndex - 1])) {
//                  dataSerieAltimeter[serieIndex] = 100;
               continue;
            }

            if (distanceSerie[serieIndex] == distanceSerie[serieIndex - 1]) {
//                  dataSerieAltimeter[serieIndex] = 120;
               continue;
            }

            if (altitudeSerie[serieIndex] == altitudeSerie[serieIndex - 1]) {
//                  dataSerieAltimeter[serieIndex] = 130;
               continue;
            }
         }
         final int serieIndexPrev = serieIndex - 1;

         // adjust index to the array size
         int lowIndex = ((0 >= serieIndexPrev) ? 0 : serieIndexPrev);
         int highIndex = ((serieIndex <= serieLengthLast) ? serieIndex : serieLengthLast);

         int timeDiff = timeSerie[highIndex] - timeSerie[lowIndex];
         float distanceDiff = distanceSerie[highIndex] - distanceSerie[lowIndex];
         float altitudeDiff = altitudeSerie[highIndex] - altitudeSerie[lowIndex];

         boolean toggleIndex = true;

         while ((timeDiff < minTimeDiff) || (distanceDiff < minDistanceDiff)) {

            // toggle between low and high index
            if (toggleIndex) {
               lowIndex--;
            } else {
               highIndex++;
            }
            toggleIndex = !toggleIndex;

            // check array scope
            if ((lowIndex < 0) || (highIndex >= serieLength)) {
               break;
            }

            timeDiff = timeSerie[highIndex] - timeSerie[lowIndex];
            distanceDiff = distanceSerie[highIndex] - distanceSerie[lowIndex];
            altitudeDiff = altitudeSerie[highIndex] - altitudeSerie[lowIndex];

         }

         highIndex = (highIndex <= serieLengthLast) ? highIndex : serieLengthLast;
         lowIndex = (lowIndex >= 0) ? lowIndex : 0;

         /*
          * check if a time difference is available between 2 time data, this can happen in gps data
          * that lat+long is available but no time
          */
         boolean isTimeValid = true;
         int prevTime = timeSerie[lowIndex];
         for (int timeIndex = lowIndex + 1; timeIndex <= highIndex; timeIndex++) {
            final int currentTime = timeSerie[timeIndex];
            if (prevTime == currentTime) {
               isTimeValid = false;
               break;
            }
            prevTime = currentTime;
         }

         if (isTimeValid) {

            if (timeDiff > 50 /* && isCustomProperty == false */) {
//               dataSerieAltimeter[serieIndex] = 300;
               continue;
            }

            // check if lat and long diff is available
            if (checkPosition && (lowIndex > 0) && (highIndex < serieLengthLast - 1) && (sliceTimeDiff > 10)) {

               if ((latitudeSerie[lowIndex] == latitudeSerie[lowIndex - 1])
                     && (longitudeSerie[lowIndex] == longitudeSerie[lowIndex - 1])) {
//                     dataSerieAltimeter[serieIndex] = 210;
                  continue;
               }
               if ((latitudeSerie[highIndex] == latitudeSerie[highIndex + 1])
                     && (longitudeSerie[highIndex] == longitudeSerie[highIndex + 1])) {
//                     dataSerieAltimeter[serieIndex] = 220;
                  continue;
               }
            }

            // compute altimeter
            if (timeDiff > 0) {
               final float altimeter = 3600f * altitudeDiff / timeDiff / UI.UNIT_VALUE_ELEVATION;
               dataSerieAltimeter[serieIndex] = altimeter;
            } else {
//               dataSerieAltimeter[serieIndex] = -100;
            }

            // compute gradient
            if (distanceDiff > 0) {
               final float gradient = altitudeDiff * 100 / distanceDiff;
               dataSerieGradient[serieIndex] = gradient;
            } else {
//               dataSerieAltimeter[serieIndex] = -200;
            }

         } else {
//            dataSerieAltimeter[serieIndex] = -300;
         }
      }

      if (UI.UNIT_IS_ELEVATION_FOOT) {

         // set imperial system

         altimeterSerieImperial = dataSerieAltimeter;

      } else {

         // set metric system

         altimeterSerie = dataSerieAltimeter;
      }

      gradientSerie = dataSerieGradient;
   }

   /**
    * Computes and sets the altitude up/down values into {@link TourData}
    *
    * @return Returns <code>true</code> when altitude was computed otherwise <code>false</code>
    */
   public boolean computeAltitudeUpDown() {

      if (altitudeSerie == null) {
         return false;
      }

      final AltitudeUpDown altiUpDown = computeAltitudeUpDown(0, altitudeSerie.length - 1);

      if (altiUpDown != null) {
         setTourAltUp(altiUpDown.altitudeUp);
         setTourAltDown(altiUpDown.altitudeDown);
      }

      return altiUpDown != null;
   }

   public AltitudeUpDown computeAltitudeUpDown(final ArrayList<AltitudeUpDownSegment> segmentSerieIndexParameter,
                                               final float selectedMinAltiDiff) {

      return computeAltitudeUpDown_30_Algorithm_9_08(segmentSerieIndexParameter, selectedMinAltiDiff);
   }

   /**
    * Compute elevation up/down for an elevation serie with the current Douglas Peucker tolerance.
    *
    * @param elevationSerie
    * @return
    */
   public AltitudeUpDown computeAltitudeUpDown(final float[] elevationSerie) {

      float prefDPTolerance;

      if (_isImportedMTTour) {
         // use imported value
         prefDPTolerance = dpTolerance / 10f;
      } else {
         prefDPTolerance = _prefStore.getFloat(ITourbookPreferences.COMPUTED_ALTITUDE_DP_TOLERANCE);
      }

      AltitudeUpDown altiUpDown;
      if (elevationSerie != null) {

         // DP needs distance

         altiUpDown = computeAltitudeUpDown_20_Algorithm_DP(elevationSerie, prefDPTolerance, 0, elevationSerie.length - 1);

         // keep this value to see in the UI (tour segmenter) the value and how it is computed
         dpTolerance = (short) (prefDPTolerance * 10);

      } else {

         altiUpDown = computeAltitudeUpDown_30_Algorithm_9_08(null, prefDPTolerance);
      }

      return altiUpDown;
   }

   /**
    * Computes the elevation gain/loss values for a specific range.
    *
    * @param startIndex
    *           The index of the range start
    * @param endIndex
    *           The index of the range end
    * @return Returns an <code>AltitudeUpDown</code> when altitude was computed otherwise
    *         <code>null</code>
    */
   public AltitudeUpDown computeAltitudeUpDown(final int startIndex, final int endIndex) {

      float prefDPTolerance;

      if (_isImportedMTTour) {
         // use imported value
         prefDPTolerance = dpTolerance / 10f;
      } else {
         prefDPTolerance = _prefStore.getFloat(ITourbookPreferences.COMPUTED_ALTITUDE_DP_TOLERANCE);
      }

      AltitudeUpDown altiUpDown;
      if (distanceSerie != null) {

         // DP needs distance

         altiUpDown = computeAltitudeUpDown_20_Algorithm_DP(altitudeSerie, prefDPTolerance, startIndex, endIndex);

         // keep this value to see in the UI (tour segmenter) the value and how it is computed
         dpTolerance = (short) (prefDPTolerance * 10);

      } else {

         altiUpDown = computeAltitudeUpDown_30_Algorithm_9_08(null, prefDPTolerance);
      }

      return altiUpDown;
   }

   /**
    * Compute altitude up/down with Douglas Peucker algorithm.
    *
    * @param elevationSerie
    * @param dpTolerance
    *           The Douglas-Peucker tolerance value
    * @param startIndex
    *           The start of the section for which to compute the elevation gain/loss
    * @param endIndex
    *           The end of the section for which to compute the elevation gain/loss
    * @return Returns <code>null</code> when altitude up/down cannot be computed
    */
   private AltitudeUpDown computeAltitudeUpDown_20_Algorithm_DP(final float[] elevationSerie,
                                                                final float dpTolerance,
                                                                final int startIndex,
                                                                final int endIndex) {

      // check if all necessary data are available
      if (elevationSerie == null
            || elevationSerie.length < 2
            || startIndex > elevationSerie.length
            || endIndex >= elevationSerie.length
            || startIndex >= endIndex) {

         return null;
      }

      // convert data series into DP points
      final DPPoint[] dpPoints = new DPPoint[endIndex - startIndex];
      int dpPointsIndex = 0;
      for (int serieIndex = startIndex; serieIndex < endIndex; serieIndex++) {
         dpPoints[dpPointsIndex] = new DPPoint(distanceSerie[serieIndex], elevationSerie[serieIndex], serieIndex);
         dpPointsIndex++;
      }

      int[] forcedIndices = null;
      if (isMultipleTours) {
         forcedIndices = multipleTourStartIndex;
      }

      final DPPoint[] simplifiedPoints = new DouglasPeuckerSimplifier(dpTolerance, dpPoints, forcedIndices).simplify();

      float altitudeUpTotal = 0;
      float altitudeDownTotal = 0;

      float prevAltitude = elevationSerie[startIndex];

      /*
       * Get altitude up/down from the tour altitude values which are found by DP
       */
      for (int dbIndex = 1; dbIndex < simplifiedPoints.length; dbIndex++) {

         final DPPoint point = simplifiedPoints[dbIndex];
         final float currentAltitude = elevationSerie[startIndex + point.serieIndex];
         final float altiDiff = currentAltitude - prevAltitude;

         if (altiDiff > 0) {
            altitudeUpTotal += altiDiff;
         } else {
            altitudeDownTotal += altiDiff;
         }

         prevAltitude = currentAltitude;
      }

      return new AltitudeUpDown(altitudeUpTotal, -altitudeDownTotal);
   }

   /**
    * Compute altitude up/down since version 9.08
    * <p>
    * This algorithm is abandoned because it can cause very wrong values depending on the terrain.
    * DP
    * is the preferred algorithm since 14.7.
    *
    * @param segmentSerie
    *           segments are created for each gradient alternation when segmentSerie is not
    *           <code>null</code>
    * @param minAltiDiff
    * @return Returns <code>null</code> when altitude up/down cannot be computed
    */
   private AltitudeUpDown computeAltitudeUpDown_30_Algorithm_9_08(final ArrayList<AltitudeUpDownSegment> segmentSerie,
                                                                  final float minAltiDiff) {

      // check if all necessary data are available
      if ((altitudeSerie == null) || (timeSerie == null) || (timeSerie.length < 2)) {
         return null;
      }

      final boolean isCreateSegments = segmentSerie != null;

      float prevAltitude = 0;
      float prevSegmentAltitude = 0;
      float prevAltiDiff = 0;

      float angleAltiUp = 0;
      float angleAltiDown = 0;

      float segmentAltitudeMin = 0;
      float segmentAltitudeMax = 0;

      float altitudeUpTotal = 0;
      float altitudeDownTotal = 0;

      final int serieLength = timeSerie.length;
      int currentSegmentSerieIndex = 0;

      for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

         final float altitude = altitudeSerie[serieIndex];
         float altiDiff = 0;

         if (serieIndex == 0) {

            // first data point

            if (isCreateSegments) {

               // create start for the first segment
               segmentSerie.add(new AltitudeUpDownSegment(currentSegmentSerieIndex, 0));
            }

            segmentAltitudeMin = altitude;
            segmentAltitudeMax = altitude;

            prevSegmentAltitude = altitude;

         } else if (serieIndex == serieLength - 1) {

            // last data point

            // check if last segment is set
            if (serieIndex != currentSegmentSerieIndex) {

               // create end point for the last segment

               float segmentMinMaxDiff = segmentAltitudeMax - segmentAltitudeMin;
               segmentMinMaxDiff = altitude > prevSegmentAltitude ? segmentMinMaxDiff : -segmentMinMaxDiff;

               if (isCreateSegments) {
                  segmentSerie.add(new AltitudeUpDownSegment(serieIndex, segmentMinMaxDiff));
               }

               if (segmentMinMaxDiff > 0) {
                  altitudeUpTotal += segmentMinMaxDiff;
               }
               if (segmentMinMaxDiff < 0) {
                  altitudeDownTotal += segmentMinMaxDiff;
               }
            }

         } else if (serieIndex > 0) {

            altiDiff = altitude - prevAltitude;

            if (altiDiff > 0) {

               // altitude is ascending

               /*
                * compares with equal 0 (== 0) to prevent initialization error, otherwise the value
                * is >0 or <0
                */
               if (prevAltiDiff >= 0) {

                  // tour is ascending again

                  angleAltiUp += altiDiff;

                  segmentAltitudeMax = segmentAltitudeMax < altitude ? altitude : segmentAltitudeMax;

               } else if (prevAltiDiff < 0) {

                  // angel changed, tour was descending and is now ascending

                  if (angleAltiDown <= -minAltiDiff) {

                     final float segmentAltiDiff = segmentAltitudeMin - segmentAltitudeMax;
                     altitudeDownTotal += segmentAltiDiff;

                     if (isCreateSegments) {

                        // create segment point for the descending altitude

                        currentSegmentSerieIndex = serieIndex - 1;

                        segmentSerie.add(
                              new AltitudeUpDownSegment(
                                    currentSegmentSerieIndex, //
                                    segmentAltiDiff));
                     }

                     segmentAltitudeMin = prevAltitude;
                     segmentAltitudeMax = prevAltitude + altiDiff;

                     prevSegmentAltitude = prevAltitude;
                  }

                  angleAltiUp = altiDiff;
                  angleAltiDown = 0;
               }

            } else if (altiDiff < 0) {

               // altitude is descending

               /*
                * compares to == 0 to prevent initialization error, otherwise the value is >0 or <0
                */
               if (prevAltiDiff <= 0) {

                  // tour is descending again

                  angleAltiDown += altiDiff;

                  segmentAltitudeMin = segmentAltitudeMin > altitude ? altitude : segmentAltitudeMin;

               } else if (prevAltiDiff > 0) {

                  // angel changed, tour was ascending and is now descending

                  if (angleAltiUp >= minAltiDiff) {

                     final float segmentAltiDiff = segmentAltitudeMax - segmentAltitudeMin;
                     altitudeUpTotal += segmentAltiDiff;

                     // create segment
                     if (isCreateSegments) {

                        currentSegmentSerieIndex = serieIndex - 1;

                        segmentSerie.add(new AltitudeUpDownSegment(currentSegmentSerieIndex, segmentAltiDiff));
                     }

                     // initialize new segment
                     segmentAltitudeMin = prevAltitude + altiDiff;
                     segmentAltitudeMax = prevAltitude;

                     prevSegmentAltitude = prevAltitude;
                  }

                  angleAltiUp = 0;
                  angleAltiDown = altiDiff;
               }
            }
         }

         // prevent setting previous alti to 0
         if (altiDiff != 0) {
            prevAltiDiff = altiDiff;
         }

         prevAltitude = altitude;
      }

      return new AltitudeUpDown(altitudeUpTotal, -altitudeDownTotal);
   }

   public float computeAvg_Altitude(final int valueIndexLeft, final int valueIndexRight) {

      // check if all necessary data are available
      if (altitudeSerie == null || altitudeSerie.length < 2
            || distanceSerie == null || distanceSerie.length < 2
            || valueIndexLeft == valueIndexRight) {

         return 0;
      }

      final int serieLength = Math.abs(valueIndexRight - valueIndexLeft);

      // convert data series into DP points
      final DPPoint[] dpPoints = new DPPoint[serieLength];
      for (int serieIndex = 0; serieIndex < dpPoints.length; serieIndex++) {

         final int valueIndex = serieIndex + valueIndexLeft;

         if (valueIndex >= distanceSerie.length) {

            StatusUtil.logError(String.format(

                  "[TourData.computeAvg_Altitude()] valueIndex=%d is larger than the distanceSerie.length=%d", //$NON-NLS-1$

                  valueIndex,
                  distanceSerie.length));

            return Float.MAX_VALUE;
         }

         dpPoints[serieIndex] = new DPPoint(

               distanceSerie[valueIndex],
               altitudeSerie[valueIndex],

               serieIndex);
      }

      int[] forcedIndices = null;
      if (isMultipleTours) {
         forcedIndices = multipleTourStartIndex;
      }

      final DPPoint[] simplifiedPoints = new DouglasPeuckerSimplifier(dpTolerance / 10.0f, dpPoints, forcedIndices).simplify();

      float altitudeUpTotal = 0;
      float altitudeDownTotal = 0;

      float prevAltitude = altitudeSerie[valueIndexLeft];

      /*
       * Get altitude up/down from the tour altitude values which are found by DP
       */
      for (int dbIndex = 1; dbIndex < simplifiedPoints.length; dbIndex++) {

         final DPPoint point = simplifiedPoints[dbIndex];
         final float currentAltitude = altitudeSerie[point.serieIndex + valueIndexLeft];
         final float altiDiff = currentAltitude - prevAltitude;

         if (altiDiff > 0) {
            altitudeUpTotal += altiDiff;
         } else {
            altitudeDownTotal += altiDiff;
         }

         prevAltitude = currentAltitude;
      }

      /**
       * Very special behavior until the tour chart analyzer can show both values:
       * <p>
       * Returns the up values, when 0 then the down values
       */

      return altitudeUpTotal > 0 ? altitudeUpTotal : altitudeDownTotal;
   }

   /**
    * Computes the average elevation change with given values of elevation gain, loss and total
    * distance.
    *
    * @return
    *         If successful, the average elevation change (in m/km) of a given tour, 0 otherwise.
    */
   private void computeAvg_AltitudeChange() {

      avgAltitudeChange = Math.round(UI.computeAverageElevationChange(tourAltUp + tourAltDown, tourDistance));
   }

   private void computeAvg_Cadence() {

      if (cadenceSerie == null) {
         return;
      }

      float cadenceSum = 0;
      int cadenceCount = 0;

      for (final float cadence : cadenceSerie) {

         if (cadence > 0) {
            cadenceCount++;
            cadenceSum += cadence;
         }
      }
      if (cadenceCount > 0) {
         avgCadence = cadenceSum / cadenceCount;
      }
   }

   public float computeAvg_CadenceSegment(final int firstIndex, final int lastIndex) {

      // check if data are available
      if (cadenceSerie == null || cadenceSerie.length == 0 || timeSerie == null || timeSerie.length == 0) {
         return 0;
      }

      // check for 1 point
      if (firstIndex == lastIndex) {
         return cadenceSerie[firstIndex];
      }

      // check for 2 points
      if (lastIndex - firstIndex == 1) {
         return (cadenceSerie[firstIndex] + cadenceSerie[lastIndex]) / 2;
      }

      // get break time when not yet set
      if (breakTimeSerie == null) {
         getBreakTime();
      }

      // at least 3 points are available
      int prevTime = timeSerie[firstIndex];
      int currentTime = timeSerie[firstIndex];
      int nextTime = timeSerie[firstIndex + 1];

      /**
       * a break is set from the previous to the current time slice
       */
      final boolean hasBreakTime = breakTimeSerie != null;
      boolean isPrevBreak = hasBreakTime ? breakTimeSerie[firstIndex] : false;
      boolean isNextBreak = hasBreakTime ? breakTimeSerie[firstIndex + 1] : false;

      double cadenceSquare = 0;
      double timeSquare = 0;

      final float[] cadenceWithMuliplier = getCadenceSerieWithMuliplier();

      for (int serieIndex = firstIndex; serieIndex <= lastIndex; serieIndex++) {

         if (hasBreakTime) {

            /*
             * break time requires distance data, so it's possible that break time data are not
             * available
             */

            if (breakTimeSerie[serieIndex] == true) {

               // break has occurred in this time slice

               if (serieIndex < lastIndex) {

                  isPrevBreak = isNextBreak;
                  isNextBreak = breakTimeSerie[serieIndex + 1];

                  prevTime = currentTime;
                  currentTime = nextTime;
                  nextTime = timeSerie[serieIndex + 1];
               }

               continue;
            }
         }

         final float cadence = cadenceWithMuliplier[serieIndex];

         float timeDiffPrev = 0;
         float timeDiffNext = 0;

         if (serieIndex > firstIndex && isPrevBreak == false) {
            // prev is available
            timeDiffPrev = ((float) currentTime - prevTime) / 2;
         }

         if (serieIndex < lastIndex && isNextBreak == false) {
            // next is available
            timeDiffNext = ((float) nextTime - currentTime) / 2;
         }

         // ignore 0 values
         if (cadence > 0) {

            cadenceSquare += cadence * timeDiffPrev + cadence * timeDiffNext;
            timeSquare += timeDiffPrev + timeDiffNext;
         }

         if (serieIndex < lastIndex) {

            isPrevBreak = isNextBreak;
            isNextBreak = hasBreakTime ? breakTimeSerie[serieIndex + 1] : false;

            prevTime = currentTime;
            currentTime = nextTime;
            nextTime = timeSerie[serieIndex + 1];
         }

      }

      return (float) (timeSquare == 0 ? 0 : cadenceSquare / timeSquare);
   }

   public float computeAvg_FromValues(final float[] valueSerie, final int firstIndex, final int lastIndex) {

      // check if data are available
      if (valueSerie == null || valueSerie.length == 0 || timeSerie == null || timeSerie.length == 0) {
         return 0;
      }

      // check for 1 point
      if (firstIndex == lastIndex) {
         return valueSerie[firstIndex];
      }

      // check for 2 points
      if (lastIndex - firstIndex == 1) {
         return (valueSerie[firstIndex] + valueSerie[lastIndex]) / 2;
      }

      // get break time when not yet set
      if (breakTimeSerie == null) {
         getBreakTime();
      }

      // at least 3 points are available
      int prevTime = timeSerie[firstIndex];
      int currentTime = timeSerie[firstIndex];
      int nextTime = timeSerie[firstIndex + 1];

      /**
       * a break is set from the previous to the current time slice
       */
      final boolean hasBreakTime = breakTimeSerie != null;
      boolean isPrevBreak = hasBreakTime ? breakTimeSerie[firstIndex] : false;
      boolean isNextBreak = hasBreakTime ? breakTimeSerie[firstIndex + 1] : false;

      double valueSquare = 0;
      double timeSquare = 0;

      for (int serieIndex = firstIndex; serieIndex <= lastIndex; serieIndex++) {

         if (hasBreakTime) {

            /*
             * break time requires distance data, so it's possible that break time data are not
             * available
             */

            if (breakTimeSerie[serieIndex] == true) {

               // break has occurred in this time slice

               if (serieIndex < lastIndex) {

                  isPrevBreak = isNextBreak;
                  isNextBreak = breakTimeSerie[serieIndex + 1];

                  prevTime = currentTime;
                  currentTime = nextTime;
                  nextTime = timeSerie[serieIndex + 1];
               }

               continue;
            }
         }

         final float value = valueSerie[serieIndex];

         float timeDiffPrev = 0;
         float timeDiffNext = 0;

         if (serieIndex > firstIndex && isPrevBreak == false) {
            // prev is available
            timeDiffPrev = ((float) currentTime - prevTime) / 2;
         }

         if (serieIndex < lastIndex && isNextBreak == false) {
            // next is available
            timeDiffNext = ((float) nextTime - currentTime) / 2;
         }

         // ignore 0 values
         if (value > 0) {

            valueSquare += value * timeDiffPrev + value * timeDiffNext;
            timeSquare += timeDiffPrev + timeDiffNext;
         }

         if (serieIndex < lastIndex) {

            isPrevBreak = isNextBreak;
            isNextBreak = hasBreakTime ? breakTimeSerie[serieIndex + 1] : false;

            prevTime = currentTime;
            currentTime = nextTime;
            nextTime = timeSerie[serieIndex + 1];
         }

      }

      return (float) (timeSquare == 0 ? 0 : valueSquare / timeSquare);
   }

   public float computeAvg_FromValues(final short[] valueSerie, final int firstIndex, final int lastIndex) {

      // convert short[] into float[]
      final float[] floatValueSerie = new float[valueSerie.length];

      for (int valueIndex = 0; valueIndex < floatValueSerie.length; valueIndex++) {
         floatValueSerie[valueIndex] = valueSerie[valueIndex];
      }

      return computeAvg_FromValues(floatValueSerie, firstIndex, lastIndex);
   }

   public void computeAvg_Pulse() {

      if ((pulseSerie == null) || (pulseSerie.length == 0) || (timeSerie == null) || (timeSerie.length == 0)) {
         return;
      }

      avgPulse = computeAvg_PulseSegment(0, timeSerie.length - 1);
   }

   /**
    * @param firstIndex
    * @param lastIndex
    * @return Returns the average pulse or 0 when not available.
    */
   public float computeAvg_PulseSegment(final int firstIndex, final int lastIndex) {

      // check if data are available
      if ((pulseSerie == null) || (pulseSerie.length == 0) || (timeSerie == null) || (timeSerie.length == 0)) {
         return 0;
      }

      if (_pulseSerie_Smoothed == null) {
         computePulseSmoothed();
      }

      // check for 1 point
      if (firstIndex == lastIndex) {
         return _pulseSerie_Smoothed[firstIndex];
      }

      // check for 2 points
      if (lastIndex - firstIndex == 1) {
         return (_pulseSerie_Smoothed[firstIndex] + _pulseSerie_Smoothed[lastIndex]) / 2;
      }

      // get break time when not yet set
      if (breakTimeSerie == null) {
         getBreakTime();
      }

      // at least 3 points are available
      int prevTime = timeSerie[firstIndex];
      int currentTime = timeSerie[firstIndex];
      int nextTime = timeSerie[firstIndex + 1];

      /**
       * a break is set from the previous to the current time slice
       */
      boolean isPrevBreak = breakTimeSerie == null ? false : breakTimeSerie[firstIndex];
      boolean isNextBreak = breakTimeSerie == null ? false : breakTimeSerie[firstIndex + 1];

      double pulseSquare = 0;
      double timeSquare = 0;

      for (int serieIndex = firstIndex; serieIndex <= lastIndex; serieIndex++) {

         if (breakTimeSerie != null) {

            /*
             * break time requires distance data, so it's possible that break time data are not
             * available
             */

            if (breakTimeSerie[serieIndex] == true) {

               // break has occurred in this time slice

               if (serieIndex < lastIndex) {

                  isPrevBreak = isNextBreak;
                  isNextBreak = breakTimeSerie == null ? false : breakTimeSerie[serieIndex + 1];

                  prevTime = currentTime;
                  currentTime = nextTime;
                  nextTime = timeSerie[serieIndex + 1];
               }

               continue;
            }
         }

         final float pulse = _pulseSerie_Smoothed[serieIndex];

         float timeDiffPrev = 0;
         float timeDiffNext = 0;

         if (serieIndex > firstIndex && isPrevBreak == false) {
            // prev is available
            timeDiffPrev = ((float) currentTime - prevTime) / 2;
         }

         if (serieIndex < lastIndex && isNextBreak == false) {
            // next is available
            timeDiffNext = ((float) nextTime - currentTime) / 2;
         }

         if (pulse > 0) {
            pulseSquare += pulse * timeDiffPrev + pulse * timeDiffNext;
            timeSquare += timeDiffPrev + timeDiffNext;
         }

         if (serieIndex < lastIndex) {

            isPrevBreak = isNextBreak;
            isNextBreak = breakTimeSerie == null ? false : breakTimeSerie[serieIndex + 1];

            prevTime = currentTime;
            currentTime = nextTime;
            nextTime = timeSerie[serieIndex + 1];
         }

      }

      return (float) (timeSquare == 0 ? 0 : pulseSquare / timeSquare);
   }

   /**
    * Is computing the tour min/max/avg temperature
    */
   public void computeAvg_Temperature() {

      if (temperatureSerie == null || temperatureSerie.length == 0) {
         return;
      }

      float temperatureMin = Float.MIN_VALUE;
      float temperatureMax = Float.MIN_VALUE;
      float temperatureSum = 0;

      int tempLength = temperatureSerie.length;

      for (final float temperature : temperatureSerie) {

         if (temperature == Float.MIN_VALUE) {

            // ignore invalid values
            tempLength--;

         } else {

            if (temperatureMin == Float.MIN_VALUE) {

               // set initial value
               temperatureMin = temperature;
               temperatureMax = temperature;

            } else {

               if (temperature < temperatureMin) {

                  temperatureMin = temperature;

               } else if (temperature > temperatureMax) {

                  temperatureMax = temperature;
               }
            }

            temperatureSum += temperature;
         }
      }

      if (tempLength > 0) {

         weather_Temperature_Min = temperatureMin;
         weather_Temperature_Max = temperatureMax;

         avgTemperature = temperatureSum / tempLength;
      }
   }

   private int computeBreakTime(final int startIndex, int endIndex) {

      int totalBreakTime = 0;

      endIndex = Math.min(endIndex, timeSerie.length - 1);

      int prevTime = timeSerie[startIndex];

      for (int serieIndex = startIndex + 1; serieIndex <= endIndex; serieIndex++) {

         final int currentTime = timeSerie[serieIndex];
         final boolean isBreak = breakTimeSerie[serieIndex];

         if (isBreak) {
            totalBreakTime += currentTime - prevTime;
         }

         prevTime = currentTime;
      }

      return totalBreakTime;
   }

   /**
    * calculate the moving time, ignore the time when the distance is 0 within a time period which
    * is defined by <code>sliceMin</code>
    *
    * @param minSlices
    *           A break will occur when the distance will not change within the minimum number of
    *           time slices.
    * @return Returns the number of slices which can be ignored
    */
   private void computeBreakTimeFixed(int minSlices) {

      final float[] distanceSerieMetric = getMetricDistanceSerie();

      breakTimeSerie = new boolean[timeSerie.length];

      float minSlicesDistance = 0;

      minSlices = (minSlices >= 1) ? minSlices : 1;

      for (int serieIndex = 0; serieIndex < timeSerie.length; serieIndex++) {

         breakTimeSerie[serieIndex] = distanceSerieMetric[serieIndex] == minSlicesDistance;

         int minSlicesIndex = serieIndex - minSlices;
         if (minSlicesIndex < 0) {
            minSlicesIndex = 0;
         }

         minSlicesDistance = distanceSerieMetric[minSlicesIndex];
      }
   }

   /**
    * Computes tour break time
    *
    * @param endIndex
    * @param btConfig
    * @return Returns break time for the whole tour.
    */
   private int computeBreakTimeVariable(final BreakTimeTool btConfig) {

      /*
       * compute break time according to the selected method
       */
      BreakTimeResult breakTimeResult = null;

      if (btConfig.breakTimeMethodId.equals(BreakTimeTool.BREAK_TIME_METHOD_BY_TIME_DISTANCE)) {

         breakTimeResult = BreakTimeTool.computeBreakTimeByTimeDistance(
               this,
               btConfig.breakShortestTime,
               btConfig.breakMaxDistance,
               btConfig.breakSliceDiff);

      } else if (btConfig.breakTimeMethodId.equals(BreakTimeTool.BREAK_TIME_METHOD_BY_SLICE_SPEED)) {

         breakTimeResult = BreakTimeTool.computeBreakTimeBySpeed(
               this,
               btConfig.breakTimeMethodId,
               btConfig.breakMinSliceSpeed);

      } else if (btConfig.breakTimeMethodId.equals(BreakTimeTool.BREAK_TIME_METHOD_BY_AVG_SPEED)) {

         breakTimeResult = BreakTimeTool.computeBreakTimeBySpeed(
               this,
               btConfig.breakTimeMethodId,
               btConfig.breakMinAvgSpeed);

      } else if (btConfig.breakTimeMethodId.equals(BreakTimeTool.BREAK_TIME_METHOD_BY_AVG_SLICE_SPEED)) {

         breakTimeResult = BreakTimeTool.computeBreakTimeByAvgSliceSpeed(
               this,
               btConfig.breakMinAvgSpeedAS,
               btConfig.breakMinSliceSpeedAS,
               btConfig.breakMinSliceTimeAS);
      }

      breakTimeSerie = breakTimeResult.breakTimeSerie;

      return breakTimeResult.tourBreakTime;
   }

   /**
    * Computes time (seconds) spent in each cadence zone (slow and fast).
    */
   public boolean computeCadenceZonesTimes() {

      if (timeSerie == null || cadenceSerie == null) {
         return false;
      }

      final boolean isPaceAndSpeedFromRecordedTime = _prefStore.getBoolean(ITourbookPreferences.APPEARANCE_IS_PACEANDSPEED_FROM_RECORDED_TIME);

      if (!isPaceAndSpeedFromRecordedTime && breakTimeSerie == null) {
         getBreakTime();
      }

      int prevTime = 0;

      final int cadenceZonesDelimiter = _prefStore.getInt(ITourbookPreferences.CADENCE_ZONES_DELIMITER);
      cadenceZone_FastTime = 0;
      cadenceZone_SlowTime = 0;

      // compute zone values
      for (int serieIndex = 0; serieIndex < timeSerie.length; serieIndex++) {

         final float cadence = cadenceSerie[serieIndex];
         final int time = timeSerie[serieIndex];

         int timeDiff = time - prevTime;
         prevTime = time;

         // Check if the user has selected to use the recorded time instead of
         // the moving time.
         if (isPaceAndSpeedFromRecordedTime) {

            // Check if a pause occurred. Pauses time is ignored.
            final int pausedTime = getPausedTime(serieIndex - 1, serieIndex);
            if (pausedTime > 0 && timeDiff >= pausedTime) {
               timeDiff = Math.max(0, timeDiff - pausedTime);
            }
         }
         // Check if a break occurred, break time is ignored
         else if (breakTimeSerie != null) {

            /*
             * break time requires distance data, so it's possible that break time data are not
             * available
             */

            if (breakTimeSerie[serieIndex]) {
               // cadence zones are not set for break time
               continue;
            }
         }

         if (cadence >= cadenceZonesDelimiter) {
            cadenceZone_FastTime += timeDiff;
         } else {
            cadenceZone_SlowTime += timeDiff;
         }
      }

      // If the cadence zones times were computed, we store the delimiter value used
      if (cadenceZone_SlowTime > 0 || cadenceZone_FastTime > 0) {
         cadenceZones_DelimiterValue = cadenceZonesDelimiter;
      }

      return true;
   }

   /**
    * Compute min/max/avg and other computed fields.
    */
   public void computeComputedValues() {

      computePulseSmoothed();
      computeDataSeries_Smoothed();

      computeMaxAltitude();
      computeMaxPulse();
      computeMaxSpeed();

      computeAvg_Pulse();
      computeAvg_Cadence();
      computeAvg_Temperature();

      computeHrZones();
      computeCadenceZonesTimes();
      computeRunningDynamics();

      computeGeo_Bounds();
      computeGeo_Grid();
   }

   private void computeDataSeries_NotSmoothed() {

      // check if the tour was created manually
      if (timeSerie == null || timeSerie.length == 0) {
         return;
      }

      final boolean isAltitudeAvailable = altitudeSerie != null;

      // check if smoothed data are already computed
      if (speedSerie != null && (isAltitudeAvailable && altitudeSerieSmoothed != null)) {
         return;
      }

      final int numTimeSlices = timeSerie.length;

      final double[] altitude = new double[numTimeSlices];

      /*
       * smooth altitude
       */

      if (isAltitudeAvailable) {

         // convert altitude into double
         for (int serieIndex = 0; serieIndex < numTimeSlices; serieIndex++) {
            altitude[serieIndex] = altitudeSerie[serieIndex];
         }

         altitudeSerieSmoothed = new float[numTimeSlices];
         altitudeSerieImperialSmoothed = new float[numTimeSlices];

         altimeterSerie = new float[numTimeSlices];
         altimeterSerieImperial = new float[numTimeSlices];

         // altitude is NOT smoothed, copy original values

         for (int serieIndex = 0; serieIndex < numTimeSlices; serieIndex++) {
            altitudeSerieSmoothed[serieIndex] = altitudeSerie[serieIndex];
            altitudeSerieImperialSmoothed[serieIndex] = (altitudeSerie[serieIndex] / UI.UNIT_FOOT);
         }
      }

      // check if required data for speed, gradient... are available
      if (distanceSerie == null && (latitudeSerie == null || longitudeSerie == null)) {
         return;
      }

      speedSerie = new float[numTimeSlices];
      speedSerie_Mile = new float[numTimeSlices];
      speedSerie_NauticalMile = new float[numTimeSlices];

      paceSerie_Seconds = new float[numTimeSlices];
      paceSerie_Seconds_Imperial = new float[numTimeSlices];
      paceSerie_Minute = new float[numTimeSlices];
      paceSerie_Minute_Imperial = new float[numTimeSlices];

      // ensure data series are created to prevent exceptions
      if (numTimeSlices < 2) {
         return;
      }

      final double[] distance = new double[numTimeSlices];

      /*
       * get distance
       */
      if (distanceSerie == null) {

         // compute distance from latitude and longitude data

         distance[0] = 0.;

         for (int serieIndex = 1; serieIndex < numTimeSlices; serieIndex++) {

            distance[serieIndex] = distance[serieIndex - 1] + MtMath.distanceVincenty(
                  latitudeSerie[serieIndex],
                  latitudeSerie[serieIndex - 1],
                  longitudeSerie[serieIndex],
                  longitudeSerie[serieIndex - 1]);
         }

      } else {

         // convert distance into double
         for (int serieIndex = 0; serieIndex < numTimeSlices; serieIndex++) {
            distance[serieIndex] = distanceSerie[serieIndex];
         }
      }

      final int serieLength = timeSerie.length;

      /*
       * Compute the terrain slope
       */
      if (isAltitudeAvailable) {

         final float[] dataSerieAltimeter = new float[serieLength];
         final float[] dataSerieGradient = new float[serieLength];

         for (int serieIndex = 1; serieIndex < serieLength; serieIndex++) {

            final float timeDiff = timeSerie[serieIndex] - timeSerie[serieIndex - 1];
            final float distanceDiff = distanceSerie[serieIndex] - distanceSerie[serieIndex - 1];
            final float altitudeDiff = altitudeSerie[serieIndex] - altitudeSerie[serieIndex - 1];

            // keep altimeter data
            dataSerieAltimeter[serieIndex] = 3600 * altitudeDiff / timeDiff / UI.UNIT_VALUE_ELEVATION;

            // keep gradient data
            dataSerieGradient[serieIndex] = distanceDiff == 0 ? 0 : altitudeDiff * 100 / distanceDiff;
         }

         if (UI.UNIT_IS_ELEVATION_FOOT) {

            // set imperial system

            altimeterSerieImperial = dataSerieAltimeter;

         } else {

            // set metric system

            altimeterSerie = dataSerieAltimeter;
         }

         gradientSerie = dataSerieGradient;

      }

      maxSpeed = 0.0f;

      for (int serieIndex = 1; serieIndex < serieLength; serieIndex++) {

         final float timeDiff = timeSerie[serieIndex] - timeSerie[serieIndex - 1];
         final float distanceDiff = distanceSerie[serieIndex] - distanceSerie[serieIndex - 1];

         if (timeDiff == 0) {

            // this case occurred -> copy data from previous time slice

            final int prevSerieIndex = serieIndex - 1;

            speedSerie[serieIndex] = speedSerie[prevSerieIndex];
            speedSerie_Mile[serieIndex] = speedSerie_Mile[prevSerieIndex];
            speedSerie_NauticalMile[serieIndex] = speedSerie_NauticalMile[prevSerieIndex];

            paceSerie_Seconds[serieIndex] = paceSerie_Seconds[prevSerieIndex];
            paceSerie_Seconds_Imperial[serieIndex] = paceSerie_Seconds_Imperial[prevSerieIndex];

            paceSerie_Minute[serieIndex] = paceSerie_Minute[prevSerieIndex];
            paceSerie_Minute_Imperial[serieIndex] = paceSerie_Minute_Imperial[prevSerieIndex];

         } else {

            final double speedMetric = distanceDiff / timeDiff * 3.6;
            final double speed_Mile = speedMetric / UI.UNIT_MILE;
            final double speed_NauticalMile = speedMetric / UI.UNIT_NAUTICAL_MILE;

            speedSerie[serieIndex] = (float) speedMetric;
            speedSerie_Mile[serieIndex] = (float) speed_Mile;
            speedSerie_NauticalMile[serieIndex] = (float) speed_NauticalMile;

            final float paceMetricSeconds = speedMetric < 1.0 ? 0 : (float) (3600.0 / speedMetric);
            final float paceImperialSeconds = speedMetric < 0.6 ? 0 : (float) (3600.0 / speed_Mile);

            if (speedMetric > maxSpeed) {
               maxSpeed = (float) speedMetric;
               maxPace = paceMetricSeconds;
            }

            paceSerie_Seconds[serieIndex] = paceMetricSeconds;
            paceSerie_Seconds_Imperial[serieIndex] = paceImperialSeconds;

            paceSerie_Minute[serieIndex] = paceMetricSeconds / 60;
            paceSerie_Minute_Imperial[serieIndex] = paceImperialSeconds / 60;
         }
      }

      /*
       * Compute summarized speeds
       */
      // convert distance double -> float
      final float[] distance_Float = new float[numTimeSlices];
      for (int serieIndex = 0; serieIndex < numTimeSlices; serieIndex++) {
         distance_Float[serieIndex] = (float) distance[serieIndex];
      }
      computeSpeedSeries_Summarized(numTimeSlices, distance_Float);
   }

   /**
    * Compute smoothed data series which depend on the distance, this is speed, pace, gradient and
    * altimeter.<br>
    * Additionally the altitude smoothed data series is computed.
    * <p>
    * This smoothing is based on the algorithm from Didier Jamet.
    */
   private void computeDataSeries_Smoothed() {

      // check if the tour was created manually
      if (timeSerie == null || timeSerie.length == 0) {
         return;
      }

      final boolean isElevationAvailable = altitudeSerie != null;

      // check if smoothed data are already computed
      if (speedSerie != null && (isElevationAvailable && altitudeSerieSmoothed != null)) {
         return;
      }

      final int numTimeSlices = timeSerie.length;

      final double[] altitude = new double[numTimeSlices];
      final double[] altitude_sc = new double[numTimeSlices];

      final double tauGradient = _prefStore.getDouble(ITourbookPreferences.GRAPH_JAMET_SMOOTHING_GRADIENT_TAU);
      final double tauSpeed = _prefStore.getDouble(ITourbookPreferences.GRAPH_JAMET_SMOOTHING_SPEED_TAU);

      final int repeatedSmoothing = _prefStore.getInt(ITourbookPreferences.GRAPH_JAMET_SMOOTHING_REPEATED_SMOOTHING);
      final double repeatedTau = _prefStore.getDouble(ITourbookPreferences.GRAPH_JAMET_SMOOTHING_REPEATED_TAU);

      /*
       * Smooth elevation
       */

      if (isElevationAvailable) {

         final boolean isAltitudeSmoothed = _prefStore.getBoolean(//
               ITourbookPreferences.GRAPH_JAMET_SMOOTHING_IS_ALTITUDE);

         // convert altitude into double
         for (int serieIndex = 0; serieIndex < numTimeSlices; serieIndex++) {
            altitude[serieIndex] = altitudeSerie[serieIndex];
         }

         // altitude MUST be smoothed because the values are used in the vertical speed
         Smooth.smoothing(timeSerie, altitude, altitude_sc, tauGradient, false, repeatedSmoothing, repeatedTau);

         altitudeSerieSmoothed = new float[numTimeSlices];
         altitudeSerieImperialSmoothed = new float[numTimeSlices];

         altimeterSerie = new float[numTimeSlices];
         altimeterSerieImperial = new float[numTimeSlices];

         if (isAltitudeSmoothed) {

            for (int serieIndex = 0; serieIndex < numTimeSlices; serieIndex++) {
               altitudeSerieSmoothed[serieIndex] = (float) altitude_sc[serieIndex];
               altitudeSerieImperialSmoothed[serieIndex] = (float) (altitude_sc[serieIndex] / UI.UNIT_FOOT);
            }
         } else {

            // altitude is NOT smoothed, copy original values

            for (int serieIndex = 0; serieIndex < numTimeSlices; serieIndex++) {
               altitudeSerieSmoothed[serieIndex] = altitudeSerie[serieIndex];
               altitudeSerieImperialSmoothed[serieIndex] = (altitudeSerie[serieIndex] / UI.UNIT_FOOT);
            }
         }
      }

      // check if required data for speed, gradient... are available
      if (distanceSerie == null && (latitudeSerie == null || longitudeSerie == null)) {
         return;
      }

      speedSerie = new float[numTimeSlices];
      speedSerie_Mile = new float[numTimeSlices];
      speedSerie_NauticalMile = new float[numTimeSlices];

      paceSerie_Seconds = new float[numTimeSlices];
      paceSerie_Seconds_Imperial = new float[numTimeSlices];
      paceSerie_Minute = new float[numTimeSlices];
      paceSerie_Minute_Imperial = new float[numTimeSlices];

      // ensure data series are created to prevent exceptions
      if (numTimeSlices < 2) {
         return;
      }

      final double[] distance = new double[numTimeSlices];
      final double[] distance_sc = new double[numTimeSlices];

      final double[] Vh_ini = new double[numTimeSlices];
      final double[] Vh = new double[numTimeSlices];
      final double[] Vh_sc = new double[numTimeSlices];

      final double[] Vv_ini = new double[numTimeSlices];
      final double[] Vv = new double[numTimeSlices];
      final double[] Vv_sc = new double[numTimeSlices];

      /*
       * Get distance
       */
      if (distanceSerie == null) {

         // compute distance from latitude and longitude data

         distance[0] = 0.;

         for (int serieIndex = 1; serieIndex < numTimeSlices; serieIndex++) {

            distance[serieIndex] = distance[serieIndex - 1]

                  + MtMath.distanceVincenty(
                        latitudeSerie[serieIndex],
                        latitudeSerie[serieIndex - 1],
                        longitudeSerie[serieIndex],
                        longitudeSerie[serieIndex - 1]);
         }

      } else {

         // convert distance into double
         for (int serieIndex = 0; serieIndex < numTimeSlices; serieIndex++) {
            distance[serieIndex] = distanceSerie[serieIndex];
         }
      }

      /*
       * Compute the horizontal and vertical speeds from the raw distance and altitude data
       */
      for (int serieIndex = 0; serieIndex < numTimeSlices - 1; serieIndex++) {

         if (timeSerie[serieIndex + 1] == timeSerie[serieIndex]) {

            if (serieIndex == 0) {
               Vh_ini[serieIndex] = 0.;
               Vv_ini[serieIndex] = 0.;
            } else {
               Vh_ini[serieIndex] = Vh_ini[serieIndex - 1];
               Vv_ini[serieIndex] = Vv_ini[serieIndex - 1];
            }

         } else {

            Vh_ini[serieIndex] = (distance[serieIndex + 1] - distance[serieIndex])
                  / (timeSerie[serieIndex + 1] - timeSerie[serieIndex]);

            if (isElevationAvailable) {
               Vv_ini[serieIndex] = (altitude[serieIndex + 1] - altitude[serieIndex])
                     / (timeSerie[serieIndex + 1] - timeSerie[serieIndex]);
            }
         }
      }
      Vh_ini[numTimeSlices - 1] = Vh_ini[numTimeSlices - 2];
      Vv_ini[numTimeSlices - 1] = Vv_ini[numTimeSlices - 2];

      /*
       * Smooth out the time variations of the distance
       */
      Smooth.smoothing(timeSerie, distance, distance_sc, tauSpeed, false, repeatedSmoothing, repeatedTau);

      /*
       * Compute the horizontal and vertical speeds from the smoothed distance and altitude
       */
      for (int serieIndex = 0; serieIndex < numTimeSlices - 1; serieIndex++) {

         if (timeSerie[serieIndex + 1] == timeSerie[serieIndex]) {

            // time has not changed

            if (serieIndex == 0) {
               Vh[serieIndex] = 0.;
               Vv[serieIndex] = 0.;
            } else {
               Vh[serieIndex] = Vh[serieIndex - 1];
               Vv[serieIndex] = Vv[serieIndex - 1];
            }

         } else {

            Vh[serieIndex] = (distance_sc[serieIndex + 1] - distance_sc[serieIndex])
                  / (timeSerie[serieIndex + 1] - timeSerie[serieIndex]);

            if (isElevationAvailable) {
               Vv[serieIndex] = (altitude_sc[serieIndex + 1] - altitude_sc[serieIndex])
                     / (timeSerie[serieIndex + 1] - timeSerie[serieIndex]);
            }
         }
      }
      Vh[numTimeSlices - 1] = Vh[numTimeSlices - 2];
      Vv[numTimeSlices - 1] = Vv[numTimeSlices - 2];

      /*
       * Smooth out the time variations of the horizontal and vertical speeds
       */
      Smooth.smoothing(timeSerie, Vh, Vh_sc, tauSpeed, false, repeatedSmoothing, repeatedTau);
      if (isElevationAvailable) {
         Smooth.smoothing(timeSerie, Vv, Vv_sc, tauGradient, false, repeatedSmoothing, repeatedTau);
      }

      /*
       * Compute the terrain slope
       */
      if (isElevationAvailable) {

         gradientSerie = new float[numTimeSlices];

         for (int serieIndex = 0; serieIndex < numTimeSlices; serieIndex++) {

            final double vh_sc_Value = Vh_sc[serieIndex];

            // check divide by 0
            gradientSerie[serieIndex] = vh_sc_Value == 0.0 //
                  ? 0
                  : (float) (Vv_sc[serieIndex] / vh_sc_Value * 100.0);

            final double vSpeedSmoothed = Vv_sc[serieIndex] * 3600.0;
            altimeterSerie[serieIndex] = (float) (vSpeedSmoothed);
            altimeterSerieImperial[serieIndex] = (float) (vSpeedSmoothed / UI.UNIT_FOOT);
         }
      }

      maxSpeed = maxPace = 0.0f;

      for (int serieIndex = 0; serieIndex < numTimeSlices; serieIndex++) {

         final double speed_Metric = Vh[serieIndex] * 3.6;
         final double speed_Mile = speed_Metric / UI.UNIT_MILE;
         final double speed_NauticalMile = speed_Metric / UI.UNIT_NAUTICAL_MILE;

         final float paceMetricSeconds = speed_Metric < 1.0 ? 0 : (float) (3600.0 / speed_Metric);
         final float paceImperialSeconds = speed_Metric < 0.6 ? 0 : (float) (3600.0 / speed_Mile);

         if (speed_Metric > maxSpeed) {
            maxSpeed = (float) speed_Metric;
            maxPace = paceMetricSeconds;
         }

         speedSerie[serieIndex] = (float) speed_Metric;
         speedSerie_Mile[serieIndex] = (float) speed_Mile;
         speedSerie_NauticalMile[serieIndex] = (float) speed_NauticalMile;

         paceSerie_Seconds[serieIndex] = paceMetricSeconds;
         paceSerie_Seconds_Imperial[serieIndex] = paceImperialSeconds;

         paceSerie_Minute[serieIndex] = paceMetricSeconds / 60;
         paceSerie_Minute_Imperial[serieIndex] = paceImperialSeconds / 60;
      }

      /*
       * Compute summarized speeds, they are always smoothed
       */
      // convert distance double -> float
      final float[] distance_Float = new float[numTimeSlices];
      for (int serieIndex = 0; serieIndex < numTimeSlices; serieIndex++) {
         distance_Float[serieIndex] = (float) distance[serieIndex];
      }
      computeSpeedSeries_Summarized(numTimeSlices, distance_Float);
   }

   /**
    * Computes geo bounds when data are available.
    *
    * @return Returns geo min/max positions when data are available, otherwise <code>null</code>.
    */
   public void computeGeo_Bounds() {

      if (latitudeSerie == null || longitudeSerie == null
            || latitudeSerie.length == 0 || longitudeSerie.length == 0
            || _isGeoBoundsChecked) {

         return;
      }

      double minLatitude = Double.MIN_VALUE;
      double maxLatitude = Double.MIN_VALUE;
      double minLongitude = Double.MIN_VALUE;
      double maxLongitude = Double.MIN_VALUE;

      int serieIndex = 0;

      // find first value where lat/long != 0
      for (; serieIndex < latitudeSerie.length; serieIndex++) {

         final double latitude = latitudeSerie[serieIndex];
         final double longitude = longitudeSerie[serieIndex];

         if (latitude != 0 || longitude != 0) {

            minLatitude = latitudeSerie[serieIndex];
            maxLatitude = latitudeSerie[serieIndex];
            minLongitude = longitudeSerie[serieIndex];
            maxLongitude = longitudeSerie[serieIndex];

            break;
         }
      }

      for (; serieIndex < latitudeSerie.length; serieIndex++) {

         final double latitude = latitudeSerie[serieIndex];
         final double longitude = longitudeSerie[serieIndex];

         // ignore lat/long == 0
         if (latitude == 0 && longitude == 0) {
            continue;
         }

         minLatitude = latitude < minLatitude ? latitude : minLatitude;
         maxLatitude = latitude > maxLatitude ? latitude : maxLatitude;

         minLongitude = longitude < minLongitude ? longitude : minLongitude;
         maxLongitude = longitude > maxLongitude ? longitude : maxLongitude;

         if (minLatitude == 0) {
            minLatitude = -180.0;
         }
      }

      _isGeoBoundsChecked = true;

      _geoBounds = minLatitude == Double.MIN_VALUE

            ? null

            // geo data are available
            : new GeoPosition[] {
                  new GeoPosition(minLatitude, minLongitude),
                  new GeoPosition(maxLatitude, maxLongitude) };
   }

   /**
    * Computes geo partitions from {@link #latitudeSerie} and {@link #latitudeSerie} into
    * {@link #geoGrid} when geo data are available, otherwise {@link #geoGrid} is <code>null</code>.
    */
   public void computeGeo_Grid() {

      if (latitudeSerie == null || longitudeSerie == null) {
         return;
      }

      geoGrid = computeGeo_Grid(latitudeSerie, longitudeSerie, 0, latitudeSerie.length).toArray();
   }

   /**
    * Computes geo partitions when geo data are available, a geo partition is a square which was
    * touched by the tour
    *
    * @param partLatitude
    * @param partLongitude
    * @param indexStart
    * @param indexEnd
    *           Last index + 1
    * @return Returns all geo partitions or <code>null</code> when geo data are not available.
    */
   private TIntHashSet computeGeo_Grid(final double[] partLatitude,
                                       final double[] partLongitude,
                                       final int indexStart,
                                       final int indexEnd) {

      if (partLatitude == null || partLongitude == null) {
         return null;
      }

      // validate indices
      int firstIndex = indexStart < indexEnd ? indexStart : indexEnd;
      int lastIndex = indexStart > indexEnd ? indexStart : indexEnd;

      if (firstIndex < 0) {
         firstIndex = 0;
      }

      if (lastIndex > partLatitude.length) {
         lastIndex = partLatitude.length;
      }

      // unique set with all geo parts
      final TIntHashSet allGeoParts = new TIntHashSet();

      for (int serieIndex = firstIndex; serieIndex < lastIndex; serieIndex++) {

         //         int latPart = (int) (latitude * 100);
         //         int lonPart = (int) (longitude * 100);
         //
         //         lat      ( -90 ... + 90) * 100 =  -9_000 +  9_000 = 18_000
         //         lon      (-180 ... +180) * 100 = -18_000 + 18_000 = 36_000
         //
         //         max      (9_000 + 9_000) * 100_000 = 18_000 * 100_000  = 1_800_000_000
         //
         //                                    Integer.MAX_VALUE = 2_147_483_647

         final double latitude = partLatitude[serieIndex];
         final double longitude = partLongitude[serieIndex];

         final int latPart = (int) (latitude * 100);
         final int lonPart = (int) (longitude * 100);

         final int latLonPart = (latPart + 9_000) * 100_000 + (lonPart + 18_000);

         allGeoParts.add(latLonPart);
      }

//      System.out.println();
//      System.out.println();
//      System.out.println();
//
//      for (final int latLonPart : allGeoParts.toArray()) {
//
//         final int lat = (latLonPart / 100_000) - 9_000;
//         final int lon = (latLonPart % 100_000) - 18_000;
//
//         System.out.println(
//               (net.tourbook.common.UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//                     + ("\t: " + latLonPart)
//                     + ("\tlat: " + lat)
//                     + ("\tlon: " + lon));
//      }
//      System.out.println();
//      System.out.println(
//            (net.tourbook.common.UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//                  + ("\tsize: " + allGeoParts.toArray().length));

      return allGeoParts;
   }

   /**
    * @param firstIndex
    * @param lastIndex
    * @return Returns the geo partitions or <code>null</code> when geo data are not available
    */
   public int[] computeGeo_Grid(final int firstIndex, final int lastIndex) {

      if (latitudeSerie == null || longitudeSerie == null) {
         return null;
      }

      final TIntHashSet computedGeoParts = computeGeo_Grid(
            latitudeSerie,
            longitudeSerie,
            firstIndex,
            lastIndex);

      return computedGeoParts.toArray();
   }

   public NormalizedGeoData computeGeo_NormalizeLatLon(final int measureStartIndex,
                                                       final int measureEndIndex,
                                                       final int geoAccuracy,
                                                       final int distanceInterval) {

      final float[] measureAllDistance = distanceSerie;
      final double[] measureAllLat = latitudeSerie;
      final double[] measureAllLon = longitudeSerie;

      if (measureAllLat == null || measureAllDistance == null) {
         return null;
      }

      // create normalized data, the distance will be normalized to 100m
      final float measureStartDistance = measureAllDistance[measureStartIndex];
      final float measureEndDistance = measureAllDistance[measureEndIndex];

      final float normalizedStartDistance = measureStartDistance / distanceInterval;
      final float normalizedEndDistance = measureEndDistance / distanceInterval;
      final int normalizedSize = (int) (normalizedEndDistance - normalizedStartDistance + 1);

      final float[] normalizedAllDist = new float[normalizedSize];
      final int[] normalizedAllLat = new int[normalizedSize];
      final int[] normalizedAllLon = new int[normalizedSize];
      final int[] allNormalized2OriginalIndex = new int[normalizedSize];

      float normalizedDistance = normalizedStartDistance * distanceInterval;

      int measureIndex = measureStartIndex;

      float measureNextDistance = 0;

      for (int normIndex = 0; normIndex < normalizedSize; normIndex++) {

         // get the last measure point before the next normalized distance
         while (measureNextDistance <= normalizedDistance && measureIndex < measureAllDistance.length - 1) {

            // move index to the next measure point
            measureIndex++;

            measureNextDistance = measureAllDistance[measureIndex];
         }

         // convert lat + lon into a positive value
         final double latValueWithOffset = measureAllLat[measureIndex] + NORMALIZED_LATITUDE_OFFSET;
         final double lonValueWithOffset = measureAllLon[measureIndex] + NORMALIZED_LONGITUDE_OFFSET;

         final int latNormalized = (int) (latValueWithOffset * geoAccuracy);
         final int lonNormalized = (int) (lonValueWithOffset * geoAccuracy);

         normalizedAllDist[normIndex] = normalizedDistance;

         normalizedAllLat[normIndex] = latNormalized;
         normalizedAllLon[normIndex] = lonNormalized;

         allNormalized2OriginalIndex[normIndex] = measureIndex;

         // next normalized distance
         normalizedDistance += distanceInterval;
      }

      final NormalizedGeoData returnData = new NormalizedGeoData();

      returnData.tourId = tourId;

      returnData.originalFirstIndex = measureStartIndex;
      returnData.originalLastIndex = measureEndIndex;

      returnData.normalizedLat = normalizedAllLat;
      returnData.normalizedLon = normalizedAllLon;

      returnData.normalized2OriginalIndices = allNormalized2OriginalIndex;

      returnData.geoAccuracy = geoAccuracy;
      returnData.distanceAccuracy = distanceInterval;
      returnData.normalizedDistance = normalizedDistance - measureStartDistance;

      return returnData;
   }

   /**
    * Computes seconds for each hr zone and sets the number of available HR zones in
    * {@link #numberOfHrZones}.
    */
   private void computeHrZones() {

      final TourPerson hrPerson = getDataPerson();

      if (timeSerie == null || pulseSerie == null || hrPerson == null) {
         return;
      }

      if (_pulseSerie_Smoothed == null) {
         computePulseSmoothed();

      }
      _hrZoneContext = hrPerson.getHrZoneContext(
            hrPerson.getHrMaxFormula(),
            hrPerson.getMaxPulse(),
            hrPerson.getBirthDayWithDefault(),
            getTourStartTime());

      if (_hrZoneContext == null) {
         // hr zones are not defined
         return;
      }

      if (breakTimeSerie == null) {
         getBreakTime();
      }

      final float[] zoneMinBpm = _hrZoneContext.zoneMinBpm;
      final float[] zoneMaxBpm = _hrZoneContext.zoneMaxBpm;

      final int zoneSize = zoneMinBpm.length;
      final int[] hrZones = new int[zoneSize];
      int prevTime = 0;

      // compute zone values
      for (int serieIndex = 0; serieIndex < timeSerie.length; serieIndex++) {

         final float pulse = _pulseSerie_Smoothed[serieIndex];
         final int time = timeSerie[serieIndex];

         final int timeDiff = time - prevTime;
         prevTime = time;

         // check if a break occurred, break time is ignored
         if (breakTimeSerie != null) {

            /*
             * break time requires distance data, so it's possible that break time data are not
             * available
             */

            if (breakTimeSerie[serieIndex] == true) {
               // hr zones are not set for break time
               continue;
            }
         }

         boolean isZoneAvailable = false;
         int zoneIndex = 0;

         for (; zoneIndex < zoneSize; zoneIndex++) {

            final float minValue = zoneMinBpm[zoneIndex];
            final float maxValue = zoneMaxBpm[zoneIndex];

            if (pulse >= minValue && pulse <= maxValue) {
               hrZones[zoneIndex] += timeDiff;
               isZoneAvailable = true;
               break;
            }
         }

         if (isZoneAvailable == false) {
            @SuppressWarnings("unused")
            int a = 0;
            a++;
         }
      }

      numberOfHrZones = zoneSize;

      hrZone0 = zoneSize > 0 ? hrZones[0] : -1;
      hrZone1 = zoneSize > 1 ? hrZones[1] : -1;
      hrZone2 = zoneSize > 2 ? hrZones[2] : -1;
      hrZone3 = zoneSize > 3 ? hrZones[3] : -1;
      hrZone4 = zoneSize > 4 ? hrZones[4] : -1;
      hrZone5 = zoneSize > 5 ? hrZones[5] : -1;
      hrZone6 = zoneSize > 6 ? hrZones[6] : -1;
      hrZone7 = zoneSize > 7 ? hrZones[7] : -1;
      hrZone8 = zoneSize > 8 ? hrZones[8] : -1;
      hrZone9 = zoneSize > 9 ? hrZones[9] : -1;

      _hrZones = new int[] {
            hrZone0, //
            hrZone1,
            hrZone2,
            hrZone3,
            hrZone4,
            hrZone5,
            hrZone6,
            hrZone7,
            hrZone8,
            hrZone9 };
   }

   private void computeMaxAltitude() {

      if (altitudeSerie == null) {
         return;
      }

      if (altitudeSerieSmoothed == null) {
         computeDataSeries_Smoothed();
      }

      // double check was necessary because this case occurred but it should not
      if (altitudeSerieSmoothed == null) {
         return;
      }

      float maxAltitude = 0;
      for (final float altitude : altitudeSerieSmoothed) {
         if (altitude > maxAltitude) {
            maxAltitude = altitude;
         }
      }

      this.maxAltitude = maxAltitude;
   }

   private void computeMaxPulse() {

      if (pulseSerie == null) {
         return;
      }

      if (_pulseSerie_Smoothed == null) {
         computePulseSmoothed();
      }

      float maxPulse = 0;

      for (final float pulse : _pulseSerie_Smoothed) {
         if (pulse > maxPulse) {
            maxPulse = pulse;
         }
      }

      this.maxPulse = maxPulse;
   }

   private void computeMaxSpeed() {
      if (distanceSerie != null) {
         computeDataSeries_Smoothed();
      }
   }

   private void computePhotoTimeAdjustment() {

      long allPhotoTimeAdjustment = 0;
      int photoCounter = 0;

      for (final TourPhoto tourPhoto : tourPhotos) {

         allPhotoTimeAdjustment += (tourPhoto.getAdjustedTime() - tourPhoto.getImageExifTime()) / 1000;

         photoCounter++;
      }

      photoTimeAdjustment = photoCounter == 0 ? 0 : (int) (allPhotoTimeAdjustment / photoCounter);
   }

   private void computePulseSmoothed() {

      if (pulseSerie == null || timeSerie == null) {
         return;
      }

      if (_pulseSerie_Smoothed != null) {
         return;
      }

      final boolean isJametAlgorithm = _prefStore.getString(ITourbookPreferences.GRAPH_SMOOTHING_SMOOTHING_ALGORITHM)
            .equals(
                  ISmoothingAlgorithm.SMOOTHING_ALGORITHM_JAMET);

      if (isJametAlgorithm == false) {

         // smoothing is disabled for pulse values
         _pulseSerie_Smoothed = Arrays.copyOf(pulseSerie, pulseSerie.length);

         return;
      }

      final boolean isPulseSmoothed = _prefStore.getBoolean(ITourbookPreferences.GRAPH_JAMET_SMOOTHING_IS_PULSE);

      if (isPulseSmoothed == false) {

         // pulse is not smoothed
         _pulseSerie_Smoothed = Arrays.copyOf(pulseSerie, pulseSerie.length);

         return;
      }

      final int repeatedSmoothing = _prefStore.getInt(ITourbookPreferences.GRAPH_JAMET_SMOOTHING_REPEATED_SMOOTHING);
      final double repeatedTau = _prefStore.getDouble(ITourbookPreferences.GRAPH_JAMET_SMOOTHING_REPEATED_TAU);
      final double tauPulse = _prefStore.getDouble(ITourbookPreferences.GRAPH_JAMET_SMOOTHING_PULSE_TAU);

      final int size = timeSerie.length;
      final double[] heart_rate = new double[size];
      final double[] heart_rate_sc = new double[size];

      // convert float into double
      for (int serieIndex = 0; serieIndex < size; serieIndex++) {
         heart_rate[serieIndex] = pulseSerie[serieIndex];
      }

      Smooth.smoothing(timeSerie, heart_rate, heart_rate_sc, tauPulse, false, repeatedSmoothing, repeatedTau);

      _pulseSerie_Smoothed = new float[size];

      // convert double into float
      for (int serieIndex = 0; serieIndex < size; serieIndex++) {
         _pulseSerie_Smoothed[serieIndex] = (float) heart_rate_sc[serieIndex];
      }
   }

   private void computeRunningDynamics() {

      /*
       * Stance time
       */
      if (runDyn_StanceTime != null && runDyn_StanceTime.length > 0) {

         short minValue;
         short maxValue;

         minValue = maxValue = getFirstNot0Value(runDyn_StanceTime);

         int numValues = 0;
         float sumValue = 0;

         for (final short value : runDyn_StanceTime) {

            // ignore 0 values
            if (value == 0) {
               continue;
            }

            if (value > maxValue) {
               maxValue = value;
            }

            if (value < minValue) {
               minValue = value;
            }

            numValues++;
            sumValue += value;
         }

         runDyn_StanceTime_Min = minValue;
         runDyn_StanceTime_Max = maxValue;
         runDyn_StanceTime_Avg = numValues == 0 ? 0 : sumValue / numValues;
      }

      /*
       * Stance time balance
       */
      if (runDyn_StanceTimeBalance != null && runDyn_StanceTimeBalance.length > 0) {

         short minValue;
         short maxValue;

         minValue = maxValue = getFirstNot0Value(runDyn_StanceTimeBalance);

         int numValues = 0;
         float sumValue = 0;

         for (final short value : runDyn_StanceTimeBalance) {

            // ignore 0 values
            if (value == 0) {
               continue;
            }

            if (value > maxValue) {
               maxValue = value;
            }

            if (value < minValue) {
               minValue = value;
            }

            numValues++;
            sumValue += value;
         }

         runDyn_StanceTimeBalance_Min = minValue;
         runDyn_StanceTimeBalance_Max = maxValue;
         runDyn_StanceTimeBalance_Avg = numValues == 0 ? 0 : sumValue / numValues;
      }

      /*
       * Step length
       */
      if (runDyn_StepLength != null && runDyn_StepLength.length > 0) {

         short minValue;
         short maxValue;

         minValue = maxValue = getFirstNot0Value(runDyn_StepLength);

         int numValues = 0;
         float sumValue = 0;

         for (final short value : runDyn_StepLength) {

            // ignore 0 values
            if (value == 0) {
               continue;
            }

            if (value > maxValue) {
               maxValue = value;
            }

            if (value < minValue) {
               minValue = value;
            }

            numValues++;
            sumValue += value;
         }

         runDyn_StepLength_Min = minValue;
         runDyn_StepLength_Max = maxValue;
         runDyn_StepLength_Avg = numValues == 0 ? 0 : sumValue / numValues;
      }

      /*
       * Vertical oscillation
       */
      if (runDyn_VerticalOscillation != null && runDyn_VerticalOscillation.length > 0) {

         short minValue;
         short maxValue;

         minValue = maxValue = getFirstNot0Value(runDyn_VerticalOscillation);

         int numValues = 0;
         float sumValue = 0;

         for (final short value : runDyn_VerticalOscillation) {

            // ignore 0 values
            if (value == 0) {
               continue;
            }

            if (value > maxValue) {
               maxValue = value;
            }

            if (value < minValue) {
               minValue = value;
            }

            numValues++;
            sumValue += value;
         }

         runDyn_VerticalOscillation_Min = minValue;
         runDyn_VerticalOscillation_Max = maxValue;
         runDyn_VerticalOscillation_Avg = numValues == 0 ? 0 : sumValue / numValues;
      }

      /*
       * Vertical ratio
       */
      if (runDyn_VerticalRatio != null && runDyn_VerticalRatio.length > 0) {

         short minValue;
         short maxValue;

         minValue = maxValue = getFirstNot0Value(runDyn_VerticalRatio);

         int numValues = 0;
         float sumValue = 0;

         for (final short value : runDyn_VerticalRatio) {

            // ignore 0 values
            if (value == 0) {
               continue;
            }

            if (value > maxValue) {
               maxValue = value;
            }

            if (value < minValue) {
               minValue = value;
            }

            numValues++;
            sumValue += value;
         }

         runDyn_VerticalRatio_Min = minValue;
         runDyn_VerticalRatio_Max = maxValue;
         runDyn_VerticalRatio_Avg = numValues == 0 ? 0 : sumValue / numValues;
      }
   }

   /**
    * Computes the speed data serie which can be retrieved with {@link TourData#getSpeedSerie()}
    */
   public void computeSpeedSeries() {

//      final long start = System.nanoTime();

      if (speedSerie != null
            && speedSerie_Mile != null
            && speedSerie_NauticalMile != null
            && paceSerie_Seconds != null
            && paceSerie_Seconds_Imperial != null
            && paceSerie_Minute != null
            && paceSerie_Minute_Imperial != null) {

         return;
      }

      if (isSpeedSerieFromDevice) {

         // speed is from the device

         computeSpeedSeries_FromDevice();

      } else {

         // speed is computed from distance and time

         final String smoothingAlgo = _prefStore.getString(ITourbookPreferences.GRAPH_SMOOTHING_SMOOTHING_ALGORITHM);

         if (smoothingAlgo.equals(ISmoothingAlgorithm.SMOOTHING_ALGORITHM_JAMET)) {

            computeDataSeries_Smoothed();

         } else if (smoothingAlgo.equals(ISmoothingAlgorithm.SMOOTHING_ALGORITHM_NO_SMOOTHING)) {

            computeDataSeries_NotSmoothed();

         } else {

            if (deviceTimeInterval == -1) {

               computeSpeedSeries_InternalWithVariableInterval();

            } else {

               computeSpeedSeries_InternalWithFixedInterval();
            }
         }
      }

//      final long end = System.nanoTime();
//
//      System.out.println("computeSpeedSeries():\t" + ((end - start) / 1000000.0) + "ms");
   }

   /**
    * Computes the imperial speed data serie and max speed
    *
    * @return
    */
   private void computeSpeedSeries_FromDevice() {

      if (speedSerie == null) {
         return;
      }

      final int numTimeSlices = speedSerie.length;

      speedSerie_Mile = new float[numTimeSlices];
      speedSerie_NauticalMile = new float[numTimeSlices];

      maxSpeed = maxPace = 0;

      for (int serieIndex = 0; serieIndex < numTimeSlices; serieIndex++) {

         /*
          * speed
          */

         final float speedMetric = speedSerie[serieIndex];

         speedSerie_Mile[serieIndex] = speedMetric / UI.UNIT_MILE;
         speedSerie_NauticalMile[serieIndex] = speedMetric / UI.UNIT_NAUTICAL_MILE;

         maxSpeed = Math.max(maxSpeed, speedMetric);
      }

      //Convert the max speed to max pace
      maxPace = maxSpeed < 1.0 ? 0 : (float) (3600.0 / maxSpeed);

      computeSpeedSeries_Summarized(numTimeSlices, distanceSerie);
   }

   /**
    * Computes the speed data serie with the internal algorithm for a fix time interval
    *
    * @return
    */
   private void computeSpeedSeries_InternalWithFixedInterval() {

      // distance is required
      if (distanceSerie == null) {
         return;
      }

      final int numTimeSlices = timeSerie.length;

      speedSerie = new float[numTimeSlices];
      speedSerie_Mile = new float[numTimeSlices];
      speedSerie_NauticalMile = new float[numTimeSlices];

      paceSerie_Seconds = new float[numTimeSlices];
      paceSerie_Seconds_Imperial = new float[numTimeSlices];
      paceSerie_Minute = new float[numTimeSlices];
      paceSerie_Minute_Imperial = new float[numTimeSlices];

      int lowIndexAdjustmentDefault = 0;
      int highIndexAdjustmentDefault = 0;

      if (deviceTimeInterval <= 2) {
         lowIndexAdjustmentDefault = 3;
         highIndexAdjustmentDefault = 3;

      } else if (deviceTimeInterval <= 5) {
         lowIndexAdjustmentDefault = 1;
         highIndexAdjustmentDefault = 1;

      } else if (deviceTimeInterval <= 10) {
         lowIndexAdjustmentDefault = 0;
         highIndexAdjustmentDefault = 1;
      } else {
         lowIndexAdjustmentDefault = 0;
         highIndexAdjustmentDefault = 1;
      }

      final int serieLengthLast = numTimeSlices - 1;

      maxSpeed = maxPace = 0;

      for (int serieIndex = 0; serieIndex < numTimeSlices; serieIndex++) {

         // adjust index to the array size
         final int serieIndexLow = serieIndex - lowIndexAdjustmentDefault;
         final int serieIndexLowMax = ((0 >= serieIndexLow) ? 0 : serieIndexLow);
         int distIndexLow = ((serieIndexLowMax <= serieLengthLast) ? serieIndexLowMax : serieLengthLast);

         final int serieIndexHigh = serieIndex + highIndexAdjustmentDefault;
         final int serieIndexHighMax = ((serieIndexHigh <= serieLengthLast) ? serieIndexHigh : serieLengthLast);
         int distIndexHigh = ((0 >= serieIndexHighMax) ? 0 : serieIndexHighMax);

         final float distanceDefault = distanceSerie[distIndexHigh] - distanceSerie[distIndexLow];

         // adjust the accuracy for the distance
         int lowIndexAdjustment = lowIndexAdjustmentDefault;
         int highIndexAdjustment = highIndexAdjustmentDefault;

         if (distanceDefault < 30) {
            lowIndexAdjustment = lowIndexAdjustmentDefault + 3;
            highIndexAdjustment = highIndexAdjustmentDefault + 3;
         } else if (distanceDefault < 50) {
            lowIndexAdjustment = lowIndexAdjustmentDefault + 2;
            highIndexAdjustment = highIndexAdjustmentDefault + 2;
         } else if (distanceDefault < 100) {
            lowIndexAdjustment = lowIndexAdjustmentDefault + 1;
            highIndexAdjustment = highIndexAdjustmentDefault + 1;
         }

         // adjust index to the array size
         final int serieIndexLowAdjusted = serieIndex - lowIndexAdjustment;
         final int serieIndexLowAdjustedMax = ((0 >= serieIndexLowAdjusted) ? 0 : serieIndexLowAdjusted);

         distIndexLow = (serieIndexLowAdjustedMax <= serieLengthLast) ? serieIndexLowAdjustedMax : serieLengthLast;

         final int serieIndexHighAdjusted = serieIndex + highIndexAdjustment;
         final int serieIndexHighAdjustedMin = ((serieIndexHighAdjusted <= serieLengthLast)
               ? serieIndexHighAdjusted
               : serieLengthLast);

         distIndexHigh = (0 >= serieIndexHighAdjustedMin) ? 0 : serieIndexHighAdjustedMin;

         final float distDiff = distanceSerie[distIndexHigh] - distanceSerie[distIndexLow];
         final float timeDiff = timeSerie[distIndexHigh] - timeSerie[distIndexLow];

         /*
          * speed
          */
         float speedMetric = 0;
         float speed_Mile = 0;
         float speed_NauticalMile = 0;
         if (timeDiff != 0) {

            final float speed = (distDiff * 3.6f) / timeDiff;

            speedMetric = speed;
            speed_Mile = speed / UI.UNIT_MILE;
            speed_NauticalMile = speed / UI.UNIT_NAUTICAL_MILE;
         }

         speedSerie[serieIndex] = speedMetric;
         speedSerie_Mile[serieIndex] = speed_Mile;
         speedSerie_NauticalMile[serieIndex] = speed_NauticalMile;

         maxSpeed = Math.max(maxSpeed, speedMetric);

         final float paceMetricSeconds = speedMetric < 1.0 ? 0 : (float) (3600.0 / speedMetric);
         final float paceImperialSeconds = speedMetric < 0.6 ? 0 : (float) (3600.0 / speed_Mile);

         paceSerie_Seconds[serieIndex] = paceMetricSeconds;
         paceSerie_Seconds_Imperial[serieIndex] = paceImperialSeconds;

         paceSerie_Minute[serieIndex] = paceMetricSeconds / 60;
         paceSerie_Minute_Imperial[serieIndex] = paceImperialSeconds / 60;
      }

      //Convert the max speed to max pace
      maxPace = maxSpeed < 1.0 ? 0 : (float) (3600.0 / maxSpeed);

      computeSpeedSeries_Summarized(numTimeSlices, distanceSerie);
   }

   /**
    * Compute the speed when the time serie has unequal time intervals, with original algorithm
    */
   private void computeSpeedSeries_InternalWithVariableInterval() {

      // distance is required
      if (distanceSerie == null) {
         return;
      }

      final int minTimeDiff = _prefStore.getInt(ITourbookPreferences.APP_DATA_SPEED_MIN_TIMESLICE_VALUE);

      final int numTimeSlices = timeSerie.length;
      final int lastSerieIndex = numTimeSlices - 1;

      maxSpeed = maxPace = 0;

      speedSerie = new float[numTimeSlices];
      speedSerie_Mile = new float[numTimeSlices];
      speedSerie_NauticalMile = new float[numTimeSlices];

      paceSerie_Seconds = new float[numTimeSlices];
      paceSerie_Seconds_Imperial = new float[numTimeSlices];
      paceSerie_Minute = new float[numTimeSlices];
      paceSerie_Minute_Imperial = new float[numTimeSlices];

      final boolean isUseLatLon = (latitudeSerie != null)
            && (longitudeSerie != null)
            && (isDistanceFromSensor == 0) // --> distance is measured with the gps device and not from a sensor
      ;

      boolean isLatLongEqual = false;
      int equalStartIndex = 0;

      for (int serieIndex = 0; serieIndex < numTimeSlices; serieIndex++) {

         final int prevSerieIndex = serieIndex - 1;

         // adjust index to the array size
         int lowIndex = ((0 >= prevSerieIndex) ? 0 : prevSerieIndex);
         int highIndex = ((serieIndex <= lastSerieIndex) ? serieIndex : lastSerieIndex);

         int timeDiff = timeSerie[highIndex] - timeSerie[lowIndex];
         float distDiff = distanceSerie[highIndex] - distanceSerie[lowIndex];

         // check if a lat and long diff is available
         if (isUseLatLon && (serieIndex > 0) && (serieIndex < lastSerieIndex - 1)) {

            if ((latitudeSerie[serieIndex] == latitudeSerie[prevSerieIndex])
                  && (longitudeSerie[serieIndex] == longitudeSerie[prevSerieIndex])) {

               if (isLatLongEqual == false) {
                  equalStartIndex = prevSerieIndex;
                  isLatLongEqual = true;
               }

               continue;

            } else if (isLatLongEqual) {

               /*
                * lat/long equality ended, compute distance for all datapoints which has the same
                * lat/long because this was not correctly computed from the device
                */

               isLatLongEqual = false;

               final int equalTimeDiff = timeSerie[serieIndex] - timeSerie[equalStartIndex];
               final float equalDistDiff = distanceSerie[serieIndex] - distanceSerie[equalStartIndex];

               float speed_Metric = 0;
               float speed_Mile = 0;
               float speed_NauticalMile = 0;

               if ((equalTimeDiff > 20) && (equalDistDiff < 10)) {
                  // speed must be greater than 1.8 km/h
               } else {

                  for (int equalSerieIndex = equalStartIndex + 1; equalSerieIndex < serieIndex; equalSerieIndex++) {

                     final int equalSegmentTimeDiff = timeSerie[equalSerieIndex] - timeSerie[equalSerieIndex - 1];

                     final float equalSegmentDistDiff = equalTimeDiff == 0
                           ? 0
                           : (float) equalSegmentTimeDiff / equalTimeDiff * equalDistDiff;

                     distanceSerie[equalSerieIndex] = distanceSerie[equalSerieIndex - 1] + equalSegmentDistDiff;

                     // compute speed for this segment
                     if ((equalSegmentTimeDiff == 0) || (equalSegmentDistDiff == 0)) {

                        speed_Metric = 0;

                     } else {

                        speed_Metric = ((equalSegmentDistDiff * 3.6f) / equalSegmentTimeDiff);
                        speed_Metric = speed_Metric < 0 ? 0 : speed_Metric;

                        speed_Mile = equalSegmentDistDiff * 3.6f / (equalSegmentTimeDiff * UI.UNIT_MILE);
                        speed_Mile = speed_Mile < 0 ? 0 : speed_Mile;

                        speed_NauticalMile = equalSegmentDistDiff * 3.6f / (equalSegmentTimeDiff * UI.UNIT_NAUTICAL_MILE);
                        speed_NauticalMile = speed_NauticalMile < 0 ? 0 : speed_NauticalMile;
                     }

                     setSpeed(
                           equalSerieIndex,
                           speed_Metric,
                           speed_Mile,
                           speed_NauticalMile);
                  }
               }
            }
         }

         boolean swapIndexDirection = true;

         while (timeDiff < minTimeDiff) {

            // toggle between low and high index
            if (swapIndexDirection) {
               highIndex++;
            } else {
               lowIndex--;
            }
            swapIndexDirection = !swapIndexDirection;

            // check bounds
            if (lowIndex < 0 || highIndex >= numTimeSlices) {
               break;
            }

            timeDiff = timeSerie[highIndex] - timeSerie[lowIndex];
            distDiff = distanceSerie[highIndex] - distanceSerie[lowIndex];
         }

         /*
          * speed
          */
         float speed_Metric = 0;
         float speed_Mile = 0;
         float speed_NauticalMile = 0;

         /*
          * check if a time difference is available between 2 time data, this can happen in gps data
          * that lat+long is available but no time
          */
         highIndex = (highIndex <= lastSerieIndex) ? highIndex : lastSerieIndex;
         lowIndex = (lowIndex >= 0) ? lowIndex : 0;

         boolean isTimeValid = true;
         int prevTime = timeSerie[lowIndex];

         for (int timeIndex = lowIndex + 1; timeIndex <= highIndex; timeIndex++) {
            final int currentTime = timeSerie[timeIndex];
            if (prevTime == currentTime) {
               isTimeValid = false;
               break;
            }
            prevTime = currentTime;
         }

         if (isTimeValid && (serieIndex > 0) && (timeDiff != 0)) {

            // check if a lat and long diff is available
            if (isUseLatLon && (lowIndex > 0) && (highIndex < lastSerieIndex - 1)) {

               if ((latitudeSerie[lowIndex] == latitudeSerie[lowIndex - 1])
                     && (longitudeSerie[lowIndex] == longitudeSerie[lowIndex - 1])
                     && (distDiff == 0)) {
                  continue;
               }

               if ((longitudeSerie[highIndex] == longitudeSerie[highIndex + 1])
                     && (latitudeSerie[highIndex] == latitudeSerie[highIndex + 1]) && (distDiff == 0)) {
                  continue;
               }
            }

            if ((timeDiff > 20) && (distDiff < 10)) {

               // speed must be greater than 1.8 km/h

               speed_Metric = 0;

            } else {

               speed_Metric = distDiff * 3.6f / timeDiff;
               speed_Metric = speed_Metric < 0 ? 0 : speed_Metric;

               speed_Mile = distDiff * 3.6f / (timeDiff * UI.UNIT_MILE);
               speed_Mile = speed_Mile < 0 ? 0 : speed_Mile;

               speed_NauticalMile = distDiff * 3.6f / (timeDiff * UI.UNIT_NAUTICAL_MILE);
               speed_NauticalMile = speed_NauticalMile < 0 ? 0 : speed_NauticalMile;
            }
         }

         setSpeed(serieIndex,
               speed_Metric,
               speed_Mile,
               speed_NauticalMile);
      }

      computeSpeedSeries_Summarized(numTimeSlices, distanceSerie);
   }

   /**
    * Compute summarized speed/pace series
    *
    * @param numTimeSlices
    * @param distance
    */
   private void computeSpeedSeries_Summarized(final int numTimeSlices, final float[] distance) {

      // distance is required
      if (distance == null) {
         return;
      }

      // check if already computed
      if (speedSerie_Summarized != null) {
         return;
      }

      speedSerie_Summarized = new float[numTimeSlices];
      speedSerie_Summarized_Mile = new float[numTimeSlices];
      speedSerie_Summarized_NauticalMile = new float[numTimeSlices];

      paceSerie_Summarized_Seconds = new float[numTimeSlices];
      paceSerie_Summarized_Seconds_Imperial = new float[numTimeSlices];
      paceSerie_Summarized_Minute = new float[numTimeSlices];
      paceSerie_Summarized_Minute_Imperial = new float[numTimeSlices];

      final boolean[] breakTimeSerie = getBreakTimeSerie();
      int prevTime = 0;
      int sumBreakTime = 0;

      int sumSkipTime = 0;
      boolean isAfterSkipTime_Plus1 = true;
      float skipTourDistance = 0;

      // skip 1st value to prevent div by 0
      for (int serieIndex = 1; serieIndex < numTimeSlices; serieIndex++) {

         final int currentTime = timeSerie[serieIndex];
         final float tourDistance = distance[serieIndex];
         final boolean isBreak = breakTimeSerie[serieIndex];

         final int timeDiff = currentTime - prevTime;

//         if (serieIndex == 83) {
//            int a = 0;
//            a++;
//         }

         if (tourDistance < 0.01) {

            /*
             * Ignore start distances when they are 0, this can occure when there is no GPS
             * signal at the tour start
             */
            sumSkipTime += timeDiff;

         } else if (isAfterSkipTime_Plus1) {

            /*
             * The distance of the first value could be too high -> subsequent time slices
             * will have a too high average value -> ignore this value
             */

            isAfterSkipTime_Plus1 = false;

            skipTourDistance = tourDistance;

         } else if (isBreak) {

            sumBreakTime += timeDiff;
         }

         prevTime = currentTime;

         final double durationTime = currentTime - sumBreakTime - sumSkipTime;

         final float speed_Metric = durationTime == 0
               ? 0
               : (float) (((tourDistance - skipTourDistance) / durationTime) * 3.6f);

         final float speed_Mile = speed_Metric / UI.UNIT_MILE;
         final float speed_NauticalMile = speed_Metric / UI.UNIT_NAUTICAL_MILE;

         final float paceMetricSeconds = speed_Metric < 1.0 ? 0 : (float) (3600.0 / speed_Metric);
         final float paceImperialSeconds = speed_Metric < 0.6 ? 0 : (float) (3600.0 / speed_Mile);

         speedSerie_Summarized[serieIndex] = speed_Metric;
         speedSerie_Summarized_Mile[serieIndex] = speed_Mile;
         speedSerie_Summarized_NauticalMile[serieIndex] = speed_NauticalMile;

         paceSerie_Summarized_Seconds[serieIndex] = paceMetricSeconds;
         paceSerie_Summarized_Seconds_Imperial[serieIndex] = paceImperialSeconds;

         paceSerie_Summarized_Minute[serieIndex] = paceMetricSeconds / 60;
         paceSerie_Summarized_Minute_Imperial[serieIndex] = paceImperialSeconds / 60;
      }
   }

   /**
    * Computes the tour moving time in seconds, this is the tour elapsed time - tour break time.
    * This value is store in {@link #tourComputedTime_Moving}.
    */
   public void computeTourMovingTime() {

      if (isManualTour()) {
         // manual tours do not have data series
         return;
      }

      if (_isImportedMTTour) {
         // these types of tour are setting the moving time
         return;
      }

      if ((timeSerie == null) || (timeSerie.length == 0)) {
         tourComputedTime_Moving = 0;
      } else {
         final int tourMovingTimeRaw = timeSerie[timeSerie.length - 1] - getBreakTime();
         tourComputedTime_Moving = Math.max(0, tourMovingTimeRaw);
      }
   }

   /**
    * Convert old int[] data series into float[], this was done in the previous versions in this
    * method updateDatabase_019_to_020() but did not work in any cases
    */
   public void convertDataSeries() {

      if (serieData.altitudeSerie != null) {

         if (isDataSerieWithContent(serieData.altitudeSerie)) {
            serieData.altitudeSerie20 = convertDataSeries_ToFloat(serieData.altitudeSerie, 0);
         }

         serieData.altitudeSerie = null;
      }

      if (serieData.cadenceSerie != null) {

         if (isDataSerieWithContent(serieData.cadenceSerie)) {
            serieData.cadenceSerie20 = convertDataSeries_ToFloat(serieData.cadenceSerie, 0);
         }

         serieData.cadenceSerie = null;
      }

      if (serieData.distanceSerie != null) {

         if (isDataSerieWithContent(serieData.distanceSerie)) {
            serieData.distanceSerie20 = convertDataSeries_ToFloat(serieData.distanceSerie, 0);
         }

         serieData.distanceSerie = null;
      }

      if (serieData.pulseSerie != null) {

         if (isDataSerieWithContent(serieData.pulseSerie)) {
            serieData.pulseSerie20 = convertDataSeries_ToFloat(serieData.pulseSerie, 0);
         }

         serieData.pulseSerie = null;
      }

      if (serieData.temperatureSerie != null) {

         if (isDataSerieWithContent(serieData.temperatureSerie)) {
            serieData.temperatureSerie20 = convertDataSeries_ToFloat(serieData.temperatureSerie, temperatureScale);
         }

         serieData.temperatureSerie = null;
      }

      /*
       * Don't convert computed data series
       */
      if (serieData.speedSerie != null) {

         if (isSpeedSerieFromDevice && isDataSerieWithContent(serieData.speedSerie)) {
            serieData.speedSerie20 = convertDataSeries_ToFloat(serieData.speedSerie, 10);
         }

         serieData.speedSerie = null;
      }

      if (serieData.powerSerie != null) {

         if (isPowerSerieFromDevice && isDataSerieWithContent(serieData.powerSerie)) {
            serieData.powerSerie20 = convertDataSeries_ToFloat(serieData.powerSerie, 0);
         }

         serieData.powerSerie = null;
      }

      for (final TourMarker tourMarker : tourMarkers) {
         tourMarker.updateDatabase_019_to_020();
      }
   }

   private double[] convertDataSeries_FromE6(final int[] dataSerieE6) {

      if (dataSerieE6 == null || dataSerieE6.length == 0) {
         return null;
      }
      final int serieSize = dataSerieE6.length;

      final double[] doubleDataSerie = new double[serieSize];

      for (int serieIndex = 0; serieIndex < serieSize; serieIndex++) {
         doubleDataSerie[serieIndex] = dataSerieE6[serieIndex] / 1E6;
      }

      return doubleDataSerie;
   }

   private int[] convertDataSeries_ToE6(final double[] dataSerieDouble) {

      if (dataSerieDouble == null || dataSerieDouble.length == 0) {
         return null;
      }

      final int serieSize = dataSerieDouble.length;

      final int[] dataSerieE6 = new int[serieSize];

      for (int serieIndex = 0; serieIndex < serieSize; serieIndex++) {
         dataSerieE6[serieIndex] = (int) (dataSerieDouble[serieIndex] * 1E6);
      }

      return dataSerieE6;
   }

   private float[] convertDataSeries_ToFloat(final int[] intDataSerie, final int scale) {

      if (intDataSerie == null) {
         return null;
      }

      final float[] floatDataSerie = new float[intDataSerie.length];

      for (int serieIndex = 0; serieIndex < intDataSerie.length; serieIndex++) {

         final int intValue = intDataSerie[serieIndex];

         floatDataSerie[serieIndex] = scale > 0 //
               ? (float) intValue / scale
               : (float) intValue;
      }

      return floatDataSerie;
   }

   /**
    * Converts all waypoints into {@link TourMarker}s when position and time are the same.
    */
   public void convertWayPoints() {

      if (timeSerie == null || latitudeSerie == null || longitudeSerie == null) {
         return;
      }

      // get a copy of the way points
      final Set<TourWayPoint> remainingWayPoints = new HashSet<>();
      remainingWayPoints.addAll(tourWayPoints);

      final ArrayList<TourWayPoint> allRemovedWayPoints = new ArrayList<>();

      /**
       * Approach waypoint time to the nearest time slice time
       */
      for (double maxGeoDiff = MAX_GEO_DIFF; maxGeoDiff <= 1;) {

         for (int timeDiffRange = 0; timeDiffRange < 1000;) {

            final int timeDiffRangeMS = timeDiffRange * 1000;

            final ArrayList<TourWayPoint> removedWayPoints = new ArrayList<>();

            for (final TourWayPoint wp : remainingWayPoints) {

               final long wpTime = wp.getTime();
               final double wpLat = wp.getLatitude();
               final double wpLon = wp.getLongitude();

               for (int serieIndex = 0; serieIndex < timeSerie.length; serieIndex++) {

                  final int relativeTime = timeSerie[serieIndex];
                  final long tourTime = tourStartTime + relativeTime * 1000;

                  // get absolute time diff
                  long timeDiff = tourTime - wpTime;
                  if (timeDiff < 0) {
                     timeDiff = -timeDiff;
                  }

                  if (timeDiff <= timeDiffRangeMS) {

                     final double tourLat = latitudeSerie[serieIndex];
                     final double tourLon = longitudeSerie[serieIndex];

                     double latDiff = tourLat - wpLat;
                     double lonDiff = tourLon - wpLon;

                     if (latDiff < 0) {
                        latDiff = -latDiff;
                     }
                     if (lonDiff < 0) {
                        lonDiff = -lonDiff;
                     }

                     if (latDiff < maxGeoDiff && lonDiff < maxGeoDiff) {

                        // time and position is the same

                        final TourMarker tourMarker = new TourMarker(this, ChartLabelMarker.MARKER_TYPE_CUSTOM);

                        tourMarker.setSerieIndex(serieIndex);
                        tourMarker.setTime(relativeTime, wpTime);

                        tourMarker.setLatitude(wpLat);
                        tourMarker.setLongitude(wpLon);

                        tourMarker.setDescription(wp.getDescription());
                        tourMarker.setLabel(wp.getName());

                        tourMarker.setUrlAddress(wp.getUrlAddress());
                        tourMarker.setUrlText(wp.getUrlText());

                        final float altitude = wp.getAltitude();
                        if (altitude != Float.MIN_VALUE) {
                           tourMarker.setAltitude(altitude);
                        }

                        tourMarkers.add(tourMarker);

                        removedWayPoints.add(wp);
                        allRemovedWayPoints.add(wp);

                        break;
                     }
                  }
               }
            }

            remainingWayPoints.removeAll(removedWayPoints);

            if (remainingWayPoints.isEmpty()) {

               // all waypoints are converted

               break;
            }

            if (timeDiffRange < 100) {
               timeDiffRange += 10;
            } else {
               timeDiffRange += 100;
            }
         }

         maxGeoDiff *= 10;
      }

      // collapse waypoints
      tourWayPoints.removeAll(allRemovedWayPoints);
   }

   /**
    * Create {@link Photo}'s from {@link TourPhoto}'s which are displayed in views, e.g. tour chart,
    * map, ...
    */
   public void createGalleryPhotos() {

      _galleryPhotos.clear();

      // create gallery photos for all tour photos
      for (final TourPhoto tourPhoto : tourPhotos) {

         final String imageFilePathName = tourPhoto.getImageFilePathName();

         Photo galleryPhoto = PhotoCache.getPhoto(imageFilePathName);
         if (galleryPhoto == null) {

            /*
             * photo is not found in the photo cache, create a new photo
             */

            final File photoFile = new File(imageFilePathName);

            galleryPhoto = new Photo(photoFile);
         }

         /*
          * when a photo is in the photo cache it is possible that the tour is from the file system,
          * update tour relevant fields
          */
         galleryPhoto.isSavedInTour = true;

         // ensure this tour is set in the photo
         galleryPhoto.addTour(tourPhoto.getTourId(), tourPhoto.getPhotoId());

         galleryPhoto.adjustedTimeTour = tourPhoto.getAdjustedTime();
         galleryPhoto.imageExifTime = tourPhoto.getImageExifTime();

         final double tourLatitude = tourPhoto.getLatitude();

         if (tourLatitude != 0) {
            galleryPhoto.setTourGeoPosition(tourLatitude, tourPhoto.getLongitude());
         }

         galleryPhoto.isTourPhotoWithGps = tourLatitude != 0;
         galleryPhoto.isGeoFromExif = tourPhoto.isGeoFromExif();

         galleryPhoto.ratingStars = tourPhoto.getRatingStars();

         // add photo after it's initialized
         PhotoCache.setPhoto(galleryPhoto);

         _galleryPhotos.add(galleryPhoto);
      }

      Collections.sort(_galleryPhotos, TourPhotoManager.AdjustTimeComparatorTour);
   }

   public void createHistoryTimeSerie(final ArrayList<HistoryData> historySlices) {

      final int serieSize = historySlices.size();
      if (serieSize == 0) {
         return;
      }

      final HistoryData[] timeDataSerie = historySlices.toArray(new HistoryData[serieSize]);

      /*
       * time serie is always available, except when tours are created manually
       */
      timeSerieHistory = new long[serieSize];

      // time is in seconds relative to the tour start
      long elapsedTime = 0;

      long historyTourStartTime = 0;

      // convert data from the tour format into long[] array
      for (int serieIndex = 0; serieIndex < serieSize; serieIndex++) {

         final HistoryData timeData = timeDataSerie[serieIndex];

         final long absoluteTime = timeData.absoluteTime;

         if (serieIndex == 0) {

            // 1st trackpoint

            timeSerieHistory[serieIndex] = 0;
            historyTourStartTime = absoluteTime;

         } else {

            // 1..Nth trackpoint

            elapsedTime = (absoluteTime - historyTourStartTime) / 1000;
            timeSerieHistory[serieIndex] = (elapsedTime);

         }
      }

      tourDeviceTime_Elapsed = elapsedTime;

      setTourEndTimeMS();
   }

   /**
    * Create the tour segment list from the segment index array
    *
    * @param breakMinSpeedDiff
    * @param breakMaxDistance
    * @param breakMinTime
    * @param segmenterBreakDistance
    * @param breakMinSpeedDiff
    *           in km/h
    * @param breakMinSpeed2
    * @param breakDistance
    * @return
    */
   public ArrayList<TourSegment> createSegmenterSegments(final BreakTimeTool btConfig) {

      if ((segmentSerieIndex == null) || (segmentSerieIndex.length < 2)) {

         // at least two points are required to build a segment
         return null;
      }

      final boolean isPaceAndSpeedFromRecordedTime = _prefStore.getBoolean(ITourbookPreferences.APPEARANCE_IS_PACEANDSPEED_FROM_RECORDED_TIME);

      final float[] segmenterAltitudeSerie = getAltitudeSmoothedSerie(false);

      final boolean isAltitudeSerie = (segmenterAltitudeSerie != null) && (segmenterAltitudeSerie.length > 0);
      final boolean isCadenceSerie = (cadenceSerie != null) && (cadenceSerie.length > 0);
      final boolean isDistanceSerie = (distanceSerie != null) && (distanceSerie.length > 0);
      final boolean isPulseSerie = (pulseSerie != null) && (pulseSerie.length > 0);

      final float[] localPowerSerie = getPowerSerie();
      final boolean isPowerSerie = (localPowerSerie != null) && (localPowerSerie.length > 0);

      final int segmentSerieLength = segmentSerieIndex.length;

      final ArrayList<TourSegment> tourSegments = new ArrayList<>(segmentSerieLength);
      final int firstSerieIndex = segmentSerieIndex[0];

      /*
       * get start values
       */
      int segmentStartTime = timeSerie[firstSerieIndex];

      float altitudeStart = 0;
      if (isAltitudeSerie) {
         altitudeStart = segmenterAltitudeSerie[firstSerieIndex];
      }

      float distanceStart = 0;
      if (isDistanceSerie) {
         distanceStart = distanceSerie[firstSerieIndex];
      }

      int timeTotal = 0;
      float distanceTotal = 0;

      float altitudeUpSummarizedBorder = 0;
      float altitudeUpSummarizedComputed = 0;
      float altitudeDownSummarizedBorder = 0;
      float altitudeDownSummarizedComputed = 0;

      final float tourPace = tourDistance == 0 //
            ? 0
            : tourComputedTime_Moving * 1000 / (tourDistance * UI.UNIT_VALUE_DISTANCE);

      segmentSerie_Time_Elapsed = new int[segmentSerieLength];
      segmentSerie_Time_Recorded = new int[segmentSerieLength];
      segmentSerie_Time_Paused = new int[segmentSerieLength];
      segmentSerie_Time_Moving = new int[segmentSerieLength];
      segmentSerie_Time_Break = new int[segmentSerieLength];
      segmentSerie_Time_Total = new int[segmentSerieLength];

      segmentSerie_Distance_Diff = new float[segmentSerieLength];
      segmentSerie_Distance_Total = new float[segmentSerieLength];

      segmentSerie_Altitude_Diff = new float[segmentSerieLength];
      segmentSerie_Altitude_UpDown_Hour = new float[segmentSerieLength];

      segmentSerie_Speed = new float[segmentSerieLength];
      segmentSerie_Pace = new float[segmentSerieLength];

      segmentSerie_Cadence = new float[segmentSerieLength];
      segmentSerie_Gradient = new float[segmentSerieLength];
      segmentSerie_Power = new float[segmentSerieLength];
      segmentSerie_Pulse = new float[segmentSerieLength];

      int segmentIndex2nd = 0;

      int totalTime_Elapsed = 0;
      int totalTime_Recorded = 0;
      int totalTime_Paused = 0;
      int totalTime_Moving = 0;
      int totalTime_Break = 0;
      float totalDistance = 0;
      float totalAltitude_Up = 0;
      float totalAltitude_Down = 0;

      // compute segment values between tour start and tour end
      for (int segmentIndex = 1; segmentIndex < segmentSerieLength; segmentIndex++) {

         final int segmentStartIndex = segmentSerieIndex[segmentIndex - 1];
         final int segmentEndIndex = segmentSerieIndex[segmentIndex];

         final TourSegment segment = new TourSegment();
         tourSegments.add(segment);

         segment.sequence = segmentIndex;

         segment.serieIndex_Start = segmentStartIndex;
         segment.serieIndex_End = segmentEndIndex;

         if (segmentSerieFilter != null) {
            segment.filter = segmentSerieFilter[segmentIndex];
         }

         /*
          * Time
          */
         final int segmentEndTime = timeSerie[segmentEndIndex];
         final int segmentElapsedTime = segmentEndTime - segmentStartTime;

         final int segmentPausedTime = getPausedTime(segmentStartIndex, segmentEndIndex);
         final int segmentBreakTime = getBreakTime(segmentStartIndex, segmentEndIndex, btConfig);

         final int segmentRecordedTime = segmentElapsedTime - segmentPausedTime;
         final int segmentMovingTime = segmentElapsedTime - segmentBreakTime;

         final float segmentTime = isPaceAndSpeedFromRecordedTime
               ? segmentRecordedTime
               : segmentMovingTime;

         segmentSerie_Time_Elapsed[segmentIndex] = segment.deviceTime_Elapsed = segmentElapsedTime;
         segmentSerie_Time_Recorded[segmentIndex] = segment.deviceTime_Recorded = segmentRecordedTime;
         segmentSerie_Time_Paused[segmentIndex] = segment.deviceTime_Paused = segmentPausedTime;
         segmentSerie_Time_Moving[segmentIndex] = segment.computedTime_Moving = segmentMovingTime;
         segmentSerie_Time_Break[segmentIndex] = segment.computedTime_Break = segmentBreakTime;
         segmentSerie_Time_Total[segmentIndex] = segment.time_Total = timeTotal += segmentElapsedTime;

         totalTime_Elapsed += segmentElapsedTime;
         totalTime_Recorded += segmentRecordedTime;
         totalTime_Paused += segmentPausedTime;
         totalTime_Moving += segmentMovingTime;
         totalTime_Break += segmentBreakTime;

         float segmentDistance = 0.0f;

         /*
          * Distance
          */
         if (isDistanceSerie) {

            final float distanceEnd = distanceSerie[segmentEndIndex];
            final float distanceDiff = distanceEnd - distanceStart;

            segmentSerie_Distance_Diff[segmentIndex] = segment.distance_Diff = distanceDiff;
            segmentSerie_Distance_Total[segmentIndex] = segment.distance_Total = distanceTotal += distanceDiff;

            // end point of current segment is the start of the next segment
            distanceStart = distanceEnd;

            segmentDistance = segment.distance_Diff;
            if (segmentDistance != 0.0) {

               // speed
               segmentSerie_Speed[segmentIndex] = segment.speed = segmentTime == 0.0f
                     ? 0.0f
                     : segmentDistance / segmentTime * 3.6f / UI.UNIT_VALUE_DISTANCE;

               // pace
               final float segmentPace = segmentTime * 1000 / (segmentDistance / UI.UNIT_VALUE_DISTANCE);
               segment.pace = segmentPace;
               segment.pace_Diff = segment.pace - tourPace;
               segmentSerie_Pace[segmentIndex] = segmentPace;

               totalDistance += segmentDistance;
            }
         }

         /*
          * Elevation
          */
         if (isAltitudeSerie) {

            final float altitudeEnd = segmenterAltitudeSerie[segmentEndIndex];
            final float altitudeDiff = altitudeEnd - altitudeStart;

            final float altiUpDownHour = segmentMovingTime == 0 //
                  ? 0
                  : (altitudeDiff / UI.UNIT_VALUE_ELEVATION) / segmentMovingTime * 3600;

            segmentSerie_Altitude_Diff[segmentIndex] = segment.altitude_Segment_Border_Diff = altitudeDiff;
            segmentSerie_Altitude_UpDown_Hour[segmentIndex] = altiUpDownHour;

            if (altitudeDiff > 0) {
               segment.altitude_Summarized_Border_Up = altitudeUpSummarizedBorder += altitudeDiff;
               segment.altitude_Summarized_Border_Down = altitudeDownSummarizedBorder;
               segment.altitude_Segment_Up = altitudeDiff;

            } else {
               segment.altitude_Summarized_Border_Up = altitudeUpSummarizedBorder;
               segment.altitude_Summarized_Border_Down = altitudeDownSummarizedBorder += altitudeDiff;
               segment.altitude_Segment_Down = altitudeDiff;
            }

            if ((segmentSerie_Altitude_Diff_Computed != null)
                  && (segmentIndex < segmentSerie_Altitude_Diff_Computed.length)) {

               final float segmentDiff = segmentSerie_Altitude_Diff_Computed[segmentIndex];

               segment.altitude_Segment_Computed_Diff = segmentDiff;

               if (segmentDiff > 0) {

                  segment.altitude_Summarized_Computed_Up = altitudeUpSummarizedComputed += segmentDiff;
                  segment.altitude_Summarized_Computed_Down = altitudeDownSummarizedComputed;

               } else {

                  segment.altitude_Summarized_Computed_Up = altitudeUpSummarizedComputed;
                  segment.altitude_Summarized_Computed_Down = altitudeDownSummarizedComputed += segmentDiff;
               }
            }

            // get computed values: altitude up/down from the 2nd index
            if (segmentSerieIndex2nd != null) {

               float sumSegmentAltitude_Up = 0;
               float sumSegmentAltitude_Down = 0;

               // get initial altitude
               float altitude1 = segmenterAltitudeSerie[segmentStartIndex];

               for (; segmentIndex2nd < segmentSerieIndex2nd.length; segmentIndex2nd++) {

                  final int serieIndex2nd = segmentSerieIndex2nd[segmentIndex2nd];

                  if (serieIndex2nd > segmentEndIndex) {
                     break;
                  }

                  final float altitude2 = segmenterAltitudeSerie[serieIndex2nd];
                  final float altitude2Diff = altitude2 - altitude1;

                  altitude1 = altitude2;

                  sumSegmentAltitude_Up += altitude2Diff > 0 ? altitude2Diff : 0;
                  sumSegmentAltitude_Down += altitude2Diff < 0 ? altitude2Diff : 0;
               }

               segment.altitude_Segment_Up = sumSegmentAltitude_Up;
               segment.altitude_Segment_Down = sumSegmentAltitude_Down;

               totalAltitude_Up += sumSegmentAltitude_Up;
               totalAltitude_Down += sumSegmentAltitude_Down;
            }

            // get computed values: power for a segment
            float sumPower = 0;
            for (int serieIndex = segmentStartIndex + 1; serieIndex <= segmentEndIndex; serieIndex++) {

               if (isPowerSerie) {
                  sumPower += localPowerSerie[serieIndex];
               }
            }
            final int segmentIndexDiff = segmentEndIndex - segmentStartIndex;
            segmentSerie_Power[segmentIndex] = segment.power = segmentIndexDiff == 0 //
                  ? 0
                  : sumPower / segmentIndexDiff;

            // end point of the current segment is the start of the next segment
            altitudeStart = altitudeEnd;
         }

         if (isDistanceSerie && isAltitudeSerie && (segmentDistance != 0.0)) {

            // gradient
            segmentSerie_Gradient[segmentIndex] = segment.gradient = //
                  segment.altitude_Segment_Border_Diff * 100 / segmentDistance;
         }

         if (isPulseSerie) {
            final float segmentAvgPulse = computeAvg_PulseSegment(segmentStartIndex, segmentEndIndex);
            segmentSerie_Pulse[segmentIndex] = segment.pulse = segmentAvgPulse;
            segment.pulse_Diff = segmentAvgPulse - avgPulse;
         } else {
            // hide pulse in the view
            segment.pulse_Diff = Float.MIN_VALUE;
         }

         if (isCadenceSerie) {

            final float segmentAvgCadence = computeAvg_CadenceSegment(segmentStartIndex, segmentEndIndex);
            segmentSerie_Cadence[segmentIndex] = segment.cadence = segmentAvgCadence;

            // stride length with rule of 3
            final float revolutionTotal = segmentAvgCadence / 60 * segment.deviceTime_Elapsed;
            segment.strideLength = segment.distance_Diff / revolutionTotal;
         }

         // end point of current segment is the start of the next segment
         segmentStartTime = segmentEndTime;
      }

      /*
       * Add total segment
       */
      final float totalSpeed = totalTime_Moving == 0 //
            ? 0
            : totalDistance / totalTime_Moving * 3.6f / UI.UNIT_VALUE_DISTANCE;

      final float totalPace = totalDistance == 0 //
            ? 0
            : totalTime_Moving * 1000 / (totalDistance / UI.UNIT_VALUE_DISTANCE);

      final TourSegment totalSegment = new TourSegment();

      totalSegment.isTotal = true;

      totalSegment.deviceTime_Elapsed = totalTime_Elapsed;
      totalSegment.deviceTime_Recorded = totalTime_Recorded;
      totalSegment.deviceTime_Paused = totalTime_Paused;
      totalSegment.computedTime_Moving = totalTime_Moving;
      totalSegment.computedTime_Break = totalTime_Break;

      totalSegment.distance_Diff = totalDistance;
      totalSegment.pace = totalPace;
      totalSegment.speed = totalSpeed;

      totalSegment.cadence = avgCadence;
      totalSegment.pulse = avgPulse;

      totalSegment.altitude_Segment_Down = totalAltitude_Down;
      totalSegment.altitude_Segment_Up = totalAltitude_Up;

      tourSegments.add(totalSegment);

      segmentSerieTotal_Altitude_Up = totalAltitude_Up;
      segmentSerieTotal_Altitude_Down = totalAltitude_Down;

      return tourSegments;
   }

   private void createSRTMDataSerie() {

      BusyIndicator.showWhile(Display.getCurrent(), () -> {

         int serieIndex = 0;
         float lastValidSRTM = 0;
         boolean isSRTMValid = false;

         final int serieLength = timeSerie.length;

         final float[] newSRTMSerie = new float[serieLength];
         final float[] newSRTMSerieImperial = new float[serieLength];

         for (final double latitude : latitudeSerie) {

            final double longitude = longitudeSerie[serieIndex];

            float srtmValue = 0;

            // ignore lat/lon 0/0, this is in the ocean
            if (latitude != 0 || longitude != 0) {
               srtmValue = _elevationSRTM3.getElevation(new GeoLat(latitude), new GeoLon(longitude));
            }

            /*
             * set invalid values to the previous valid value
             */
            if (srtmValue == Float.MIN_VALUE) {
               // invalid data
               srtmValue = lastValidSRTM;
            } else {
               // valid data are available
               isSRTMValid = true;
               lastValidSRTM = srtmValue;
            }

            // adjust wrong values
            if (srtmValue < -1000) {
               srtmValue = 0;
            } else if (srtmValue > 10000) {
               srtmValue = 10000;
            }

            newSRTMSerie[serieIndex] = srtmValue;
            newSRTMSerieImperial[serieIndex] = srtmValue / UI.UNIT_FOOT;

            serieIndex++;
         }

         if (isSRTMValid) {
            srtmSerie = newSRTMSerie;
            srtmSerieImperial = newSRTMSerieImperial;
         } else {
            // set state that srtm altitude is invalid
            srtmSerie = new float[0];
         }
      });
   }

   private float[] createSwimUI_DataSerie(final short[] swimDataSerie) {

      if (timeSerie == null || swim_Time == null || swim_Time.length == 0 || swimDataSerie == null) {
         return null;
      }

      // create UI data serie

      float[] swimUIValues = null;

      final int timeSerieSize = timeSerie.length;
      final int swimSerieSize = swimDataSerie.length;

      swimUIValues = new float[timeSerieSize];

      if (isMultipleTours) {

         // tour data contains multiple tours

         final int numTours = multipleSwimStartIndex.length;

         for (int tourIndex = 0; tourIndex < numTours; tourIndex++) {

            final int timeSerieStartIndex = multipleTourStartIndex[tourIndex];
            final int swimSerieStartIndex = multipleSwimStartIndex[tourIndex];

            if (swimSerieStartIndex >= swimSerieSize) {

               // there are no further swim data, this can occur when the last tour(s) have no swim data
               break;
            }

            final int timeSerieEndIndex = tourIndex < numTours - 1 ? multipleTourStartIndex[tourIndex + 1] : timeSerieSize;
            final int swimSerieEndIndex = tourIndex < numTours - 1 ? multipleSwimStartIndex[tourIndex + 1] : swimSerieSize;

            final long swimTourStartTime = tourStartTime + (timeSerie[timeSerieStartIndex] * 1000);
            long swimTime = tourStartTime + (swim_Time[swimSerieStartIndex] * 1000);

            short swimValue = 0;
            int swimSerieIndex = swimSerieStartIndex;

            for (int timeSerieIndex = timeSerieStartIndex; timeSerieIndex < timeSerieEndIndex; timeSerieIndex++) {

               final long tourTime = swimTourStartTime + (timeSerie[timeSerieIndex] * 1000);

               if (tourTime >= swimTime) {

                  // advance to the next swim slice, swim slices are less frequent than tour slices

                  swimSerieIndex++;

                  // check bounds
                  if (swimSerieIndex < swimSerieEndIndex) {

                     swimValue = swimDataSerie[swimSerieIndex];

                     if (swimValue == Short.MIN_VALUE) {

                        // use MIN_VALUE that the original color is displayed which makes a rest time more visible
                        //   swimValue = 0;
                     }

                     swimTime = swimTourStartTime + (swim_Time[swimSerieIndex] * 1000);
                  }
               }

               swimUIValues[timeSerieIndex] = swimValue;
            }
         }

      } else {

         // tour data contains 1 tour

         // set values for 1st swim slice
         long swimTime = tourStartTime + (swim_Time[0] * 1000);
         short swimValue = swimDataSerie[0];

         int swimSerieIndex = 0;

         for (int timeSerieIndex = 0; timeSerieIndex < timeSerieSize; timeSerieIndex++) {

            final long tourTime = tourStartTime + (timeSerie[timeSerieIndex] * 1000);

            if (tourTime >= swimTime) {

               // advance to the next swim slice, swim slices are less frequent than tour slices

               swimSerieIndex++;

               // check bounds
               if (swimSerieIndex < swimSerieSize) {

                  swimValue = swimDataSerie[swimSerieIndex];

                  if (swimValue == Short.MIN_VALUE) {

                     // use MIN_VALUE that the original color is displayed which makes a rest time more visible
                     //   swimValue = 0;
                  }

                  swimTime = tourStartTime + (swim_Time[swimSerieIndex] * 1000);
               }
            }

            swimUIValues[timeSerieIndex] = swimValue;
         }

      }

      return swimUIValues;
   }

   /**
    * Compute swim Swolf data and convert it into a 'normal' data serie
    *
    * @return
    */
   private float[] createSwimUI_SwolfDataSerie() {

      if (timeSerie == null
            || swim_Time == null || swim_Time.length == 0
            || swim_Strokes == null || swim_Strokes.length == 0) {

         return null;
      }

      final int swimSerieSize = swim_Time.length;

      final short[] swolfData = new short[swimSerieSize];

      int prevSwimTime = swim_Time[0];

      for (int swimIndex = 0; swimIndex < swim_Time.length; swimIndex++) {

         final int currentSwimTime = swim_Time[swimIndex];
         final short strokes = swim_Strokes[swimIndex];

         final int timeDiff = currentSwimTime - prevSwimTime;

         swolfData[swimIndex] = (short) (strokes == Short.MIN_VALUE || strokes == 0
               ? 0
               : strokes + timeDiff);

         prevSwimTime = currentSwimTime;
      }

      return createSwimUI_DataSerie(swolfData);
   }

   /**
    * Convert {@link TimeData} into {@link TourData} this will be done after data are imported or
    * transfered.
    * <p>
    * The array {@link #timeSerie} is always created even when the time is not available.
    *
    * @param isCreateMarker
    *           creates markers when <code>true</code>
    */
   public void createTimeSeries(final List<TimeData> timeDataList, final boolean isCreateMarker) {

      final int serieSize = timeDataList.size();
      if (serieSize == 0) {
         return;
      }

      final TimeData[] timeDataSerie = timeDataList.toArray(new TimeData[serieSize]);
      final TimeData firstTimeDataItem = timeDataSerie[0];

      /*
       * absolute time is set when absolute data are available which are mostly data from GPS
       * devices
       */
      final boolean isAbsoluteData = firstTimeDataItem.absoluteTime != Long.MIN_VALUE;

      /*
       * time serie is always available, except when tours are created manually
       */
      timeSerie = new int[serieSize];

// SET_FORMATTING_OFF

      final boolean isAltitude                     = setupStartingValues_Altitude(timeDataSerie, isAbsoluteData);
      final boolean isCadence                      = setupStartingValues_Cadence(timeDataSerie);
      final boolean isDistance                     = setupStartingValues_Distance(timeDataSerie, isAbsoluteData);
      final boolean isGear                         = setupStartingValues_Gear(timeDataSerie);
      final boolean isGPS                          = setupStartingValues_LatLon(timeDataSerie);
      final boolean isPower                        = setupStartingValues_Power(timeDataSerie);
      final boolean isPulse                        = setupStartingValues_Pulse(timeDataSerie);
      final boolean isSpeed                        = setupStartingValues_Speed(timeDataSerie);
      final boolean isTemperature                  = setupStartingValues_Temperature(timeDataSerie);

      final boolean isRunDyn_StanceTime            = setupStartingValues_RunDyn_StanceTime(timeDataSerie);
      final boolean isRunDyn_StanceTimeBalance     = setupStartingValues_RunDyn_StanceTimeBalance(timeDataSerie);
      final boolean isRunDyn_StepLength            = setupStartingValues_RunDyn_StepLength(timeDataSerie);
      final boolean isRunDyn_VerticalOscillation   = setupStartingValues_RunDyn_VerticalOscillation(timeDataSerie);
      final boolean isRunDyn_VerticalRatio         = setupStartingValues_RunDyn_VerticalRatio(timeDataSerie);

// SET_FORMATTING_ON

      // time in seconds relative to the tour start
      long elapsedTime = 0;

      if (isAbsoluteData) {

         /*
          * absolute data are available when data are from GPS devices
          */

         long tourStartTime = 0;
         long lastValidTime = 0;
         long lastValidAbsoluteTime = 0;

         // convert data from the tour format into integer[] arrays
         for (int serieIndex = 0; serieIndex < serieSize; serieIndex++) {

            final TimeData timeData = timeDataSerie[serieIndex];

            final long absoluteTime = timeData.absoluteTime;

            if (serieIndex == 0) {

               // first trackpoint

               /*
                * time
                */
               timeSerie[serieIndex] = 0;
               if (absoluteTime == Long.MIN_VALUE) {
                  tourStartTime = 0;
               } else {
                  tourStartTime = absoluteTime;
               }

               elapsedTime = 0;
               lastValidTime = tourStartTime;
               lastValidAbsoluteTime = tourStartTime;

               /*
                * distance
                */
               if (isDistance) {

                  final float absoluteDistance = timeData.absoluteDistance;
                  if ((absoluteDistance == Float.MIN_VALUE) || (absoluteDistance >= Integer.MAX_VALUE)) {
                     distanceSerie[serieIndex] = 0;
                  } else {
                     /**
                      * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                      * <p>
                      * rounding cannot be used because the tour id contains the last value from the
                      * distance serie so rounding creates another tour id
                      * <p>
                      * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                      */
                     distanceSerie[serieIndex] = (int) (absoluteDistance);
                  }
               }

            } else {

               // 1..n trackpoint

               /*
                * time: absolute time is checked against last valid time because this case happened
                * but time can NOT be in the past.
                */
               if (absoluteTime == Long.MIN_VALUE || absoluteTime < lastValidAbsoluteTime) {
                  elapsedTime = lastValidTime;
               } else {
                  elapsedTime = (absoluteTime - tourStartTime) / 1000;
                  lastValidAbsoluteTime = absoluteTime;
               }
               timeSerie[serieIndex] = (int) (lastValidTime = elapsedTime);

               /*
                * distance
                */
               if (isDistance) {

                  final float absoluteDistance = timeData.absoluteDistance;
                  if ((absoluteDistance == Float.MIN_VALUE) || (absoluteDistance >= Integer.MAX_VALUE)) {
                     distanceSerie[serieIndex] = Float.MIN_VALUE;
                  } else {
                     /**
                      * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                      * <p>
                      * rounding cannot be used because the tour id contains the last value from the
                      * distance serie so rounding creates another tour id
                      * <p>
                      * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                      */
                     distanceSerie[serieIndex] = absoluteDistance;
                  }
               }
            }

            /*
             * altitude
             */
            if (isAltitude) {
               final float absoluteAltitude = timeData.absoluteAltitude;
               altitudeSerie[serieIndex] = (absoluteAltitude == Float.MIN_VALUE
                     || (absoluteAltitude >= Integer.MAX_VALUE)) //
                           ? Float.MIN_VALUE
                           : absoluteAltitude;
            }

            /*
             * latitude & longitude
             */
            if (isGPS) {
               latitudeSerie[serieIndex] = timeData.latitude;
               longitudeSerie[serieIndex] = timeData.longitude;
            }

            /*
             * pulse
             */
            if (isPulse) {
               pulseSerie[serieIndex] = timeData.pulse;
            }

            /*
             * temperature
             */
            if (isTemperature) {
               temperatureSerie[serieIndex] = timeData.temperature;
            }

            /*
             * cadence
             */
            if (isCadence) {
               // cadence is not interpolated, ensure to set valid values
               final float tdCadence = timeData.cadence;
               cadenceSerie[serieIndex] = tdCadence == Float.MIN_VALUE ? 0 : tdCadence;
            }

            /*
             * Gear
             */
            if (isGear) {
               gearSerie[serieIndex] = timeData.gear;
            }

            /*
             * power
             */
            if (isPower) {
               final float tdPower = timeData.power;
               powerSerie[serieIndex] = tdPower == Float.MIN_VALUE ? 0 : tdPower;
            }

            /*
             * speed
             */
            if (isSpeed) {
               // speed is not interpolated, ensure to set valid values
               final float tdSpeed = timeData.speed;
               speedSerie[serieIndex] = tdSpeed == Float.MIN_VALUE ? 0 : tdSpeed;
            }

            /*
             * Running Dynamics
             */
            if (isRunDyn_StanceTime) {
               final short tdValue = timeData.runDyn_StanceTime;
               runDyn_StanceTime[serieIndex] = tdValue == Short.MIN_VALUE ? 0 : tdValue;
            }
            if (isRunDyn_StanceTimeBalance) {
               final short tdValue = timeData.runDyn_StanceTimeBalance;
               runDyn_StanceTimeBalance[serieIndex] = tdValue == Short.MIN_VALUE ? 0 : tdValue;
            }
            if (isRunDyn_StepLength) {
               final short tdValue = timeData.runDyn_StepLength;
               runDyn_StepLength[serieIndex] = tdValue == Short.MIN_VALUE ? 0 : tdValue;
            }
            if (isRunDyn_VerticalOscillation) {
               final short tdValue = timeData.runDyn_VerticalOscillation;
               runDyn_VerticalOscillation[serieIndex] = tdValue == Short.MIN_VALUE ? 0 : tdValue;
            }
            if (isRunDyn_VerticalRatio) {
               final short tdValue = timeData.runDyn_VerticalRatio;
               runDyn_VerticalRatio[serieIndex] = tdValue == Short.MIN_VALUE ? 0 : tdValue;
            }
         }

      } else {

         /*
          * relative data is available, these data are NOT from GPS devices
          */

         int distanceAbsolute = 0;
         int altitudeAbsolute = 0;

         // convert data from the tour format into an integer[]
         for (int serieIndex = 0; serieIndex < serieSize; serieIndex++) {

            final TimeData timeData = timeDataSerie[serieIndex];

            final int tdTime = timeData.time;

            // set time
            timeSerie[serieIndex] = (int) (elapsedTime += tdTime == Integer.MIN_VALUE ? 0 : tdTime);

            if (isDistance) {
               final float tdDistance = timeData.distance;
               if (tdDistance == Float.MIN_VALUE) {
                  distanceSerie[serieIndex] = Float.MIN_VALUE;
               } else {
                  distanceSerie[serieIndex] = distanceAbsolute += tdDistance;
               }
            }

            if (isAltitude) {
               final float tdAltitude = timeData.altitude;
               if (tdAltitude == Float.MIN_VALUE) {
                  altitudeSerie[serieIndex] = Float.MIN_VALUE;
               } else {
                  altitudeSerie[serieIndex] = altitudeAbsolute += tdAltitude;
               }
            }

            if (isPulse) {
               pulseSerie[serieIndex] = timeData.pulse;
            }

            if (isTemperature) {
               temperatureSerie[serieIndex] = timeData.temperature;
            }

            if (isCadence) {
               final float tdCadence = timeData.cadence;
               cadenceSerie[serieIndex] = tdCadence == Float.MIN_VALUE ? 0 : tdCadence;
            }

            if (isPower) {
               final float tdPower = timeData.power;
               powerSerie[serieIndex] = tdPower == Float.MIN_VALUE ? 0 : tdPower;
            }

            if (isSpeed) {
               final float tdSpeed = timeData.speed;
               speedSerie[serieIndex] = tdSpeed == Float.MIN_VALUE ? 0 : tdSpeed;
            }
         }
      }

      createTimeSeries_10_DataCompleting();
      createTimeSeries_50_PulseTimes(timeDataSerie);

      tourDistance = isDistance ? distanceSerie[serieSize - 1] : 0;
      tourDeviceTime_Elapsed = elapsedTime;
      setTourEndTimeMS();

      if (isGear) {
         // set shift counts
         setGears(gearSerie);
      }

      cleanupDataSeries();

      /*
       * Try to get distance values from lat/long values, this must be done after the cleanup which
       * can set distanceSerie = null.
       */
      if (distanceSerie == null) {
         TourManager.computeDistanceValuesFromGeoPosition(this);
      }

      /*
       * Set time zone from geo position
       */
      if (latitudeSerie != null) {

         // latitude can be null AFTER cleanup data series

         // get time zone from lat/lon
         final double lat = latitudeSerie[0];
         final double lon = longitudeSerie[0];

         final String rawZoneId = TimezoneMapper.latLngToTimezoneString(lat, lon);
         final ZoneId zoneId = ZoneId.of(rawZoneId);

         setTimeZoneId(zoneId.getId());
      }

      /*
       * create marker after all other data are setup
       */
      if (isCreateMarker) {

         for (int serieIndex = 0; serieIndex < serieSize; serieIndex++) {

            final TimeData timeData = timeDataSerie[serieIndex];

            if (timeData.marker != 0) {

               int relativeTime = 0;
               float distanceValue = 0;

               if (timeSerie != null) {
                  relativeTime = timeSerie[serieIndex];
               }
               if (distanceSerie != null) {
                  distanceValue = distanceSerie[serieIndex];
               }

               createTourMarker(timeData, serieIndex, relativeTime, distanceValue);
            }
         }
      }
      resetSortedMarkers();
   }

   /**
    * Interpolations of missing data
    */
   private void createTimeSeries_10_DataCompleting() {

      createTimeSeries_12_RemoveInvalidDistanceValues();
      createTimeSeries_14_RemoveInvalidDistanceValues();

      createTimeSeries_20_data_completing(latitudeSerie, timeSerie);
      createTimeSeries_20_data_completing(longitudeSerie, timeSerie);

      createTimeSeries_30_data_completing(altitudeSerie, timeSerie);
      createTimeSeries_30_data_completing(distanceSerie, timeSerie);
      createTimeSeries_30_data_completing(temperatureSerie, timeSerie);
      createTimeSeries_30_data_completing(pulseSerie, timeSerie);
   }

   private void createTimeSeries_12_RemoveInvalidDistanceValues() {

      if (isDistanceFromSensor == 1 || latitudeSerie == null || distanceSerie == null) {
         return;
      }

      /*
       * Distance is measured with the gps device and not with a sensor. Remove all distance values
       * which are set but lat/lon is not available, this case can happen when a device is in a
       * tunnel. Distance values will be interpolated later.
       */

      final int size = timeSerie.length;

      for (int serieIndex = 0; serieIndex < size; serieIndex++) {
         if (latitudeSerie[serieIndex] == Double.MIN_VALUE) {
            distanceSerie[serieIndex] = Float.MIN_VALUE;
         }
      }
   }

   /**
    * Because of the current algorithm, the first distance value can be <code>0</code> and the other
    * values can be {@link Float#MIN_VALUE}.
    * <p>
    * When this occurs, set all distance values to {@link Float#MIN_VALUE}, that distance values
    * are not recognized.
    */
   private void createTimeSeries_14_RemoveInvalidDistanceValues() {

      if (distanceSerie == null || distanceSerie.length < 2) {
         return;
      }

      boolean isDataValid = false;

      for (int serieIndex = 1; serieIndex < distanceSerie.length; serieIndex++) {

         final float distanceValue = distanceSerie[serieIndex];

         if (distanceValue < 0) {

            // distance is invalid, set to a 'valid' value which is corrected in DataCompleting

            distanceSerie[serieIndex] = Float.MIN_VALUE;

         } else if (distanceValue != Float.MIN_VALUE) {

            // there are valid values, data are OK
            isDataValid = true;
         }
      }

      if (isDataValid) {
         return;
      }

      if (distanceSerie[0] == 0.0) {

         // set distance to be unavailable

         distanceSerie[0] = Float.MIN_VALUE;

      } else {

         // this case needs more investigation if it occurs
      }
   }

   private void createTimeSeries_20_data_completing(final double[] field, final int[] time) {

      if (field == null) {
         return;
      }

      final int size = time.length;

      for (int serieIndex = 0; serieIndex < size; serieIndex++) {

         if (field[serieIndex] == Double.MIN_VALUE) {

            // search forward to the next valid data
            int invalidIndex = serieIndex;
            while (field[invalidIndex] == Double.MIN_VALUE && invalidIndex < size - 1) {
               invalidIndex++;
            }

            final int nextValidIndex = invalidIndex;

            if (field[nextValidIndex] == Double.MIN_VALUE) {

               double lastValidValue;
               if (serieIndex - 1 < 0) {
                  // ??????????????????
                  lastValidValue = 0;
               } else {
                  lastValidValue = field[serieIndex - 1];
               }

               field[nextValidIndex] = lastValidValue;
            }

            final int time1 = time[serieIndex - 1];
            final int time2 = time[nextValidIndex];
            final double val1 = field[serieIndex - 1];
            final double val2 = field[nextValidIndex];

            for (int interpolationIndex = serieIndex; interpolationIndex < nextValidIndex; interpolationIndex++) {

               final double interpolationValue = createTimeSeries_40_linear_interpolation(
                     time1,
                     time2,
                     val1,
                     val2,
                     time[interpolationIndex]);

               field[interpolationIndex] = interpolationValue;
            }

            serieIndex = nextValidIndex - 1;
         }
      }
   }

   private void createTimeSeries_30_data_completing(final float[] field, final int[] time) {

      if (field == null) {
         return;
      }

      final int size = time.length;

      for (int serieIndex = 0; serieIndex < size; serieIndex++) {

         if (field[serieIndex] == Float.MIN_VALUE) {

            // search forward to the next valid data
            int invalidIndex = serieIndex;
            while (field[invalidIndex] == Float.MIN_VALUE && invalidIndex < size - 1) {
               invalidIndex++;
            }

            final int nextValidIndex = invalidIndex;

            if (field[nextValidIndex] == Float.MIN_VALUE) {

               float lastValidValue;
               if (serieIndex - 1 < 0) {
                  // ??????????????????
                  lastValidValue = 0;
               } else {
                  lastValidValue = field[serieIndex - 1];
               }

               field[nextValidIndex] = lastValidValue;
            }

            final int validValueIndex = serieIndex == 0 ? 0 : serieIndex - 1;

            final int time1 = time[validValueIndex];
            final int time2 = time[nextValidIndex];
            final double val1 = field[validValueIndex];
            final double val2 = field[nextValidIndex];

            for (int interpolationIndex = serieIndex; interpolationIndex < nextValidIndex; interpolationIndex++) {

               final double linearInterpolation = createTimeSeries_40_linear_interpolation(
                     time1,
                     time2,
                     val1,
                     val2,
                     time[interpolationIndex]);

               field[interpolationIndex] = (float) linearInterpolation;
            }

            serieIndex = nextValidIndex - 1;
         }
      }
   }

   private double createTimeSeries_40_linear_interpolation(final double time1,
                                                           final double time2,
                                                           final double val1,
                                                           final double val2,
                                                           final double time) {
      if (time2 == time1) {
         return ((val1 + val2) / 2.);
      } else {
         return (val1 + (val2 - val1) / (time2 - time1) * (time - time1));
      }
   }

   private void createTimeSeries_50_PulseTimes(final TimeData[] allTimeData) {

      boolean isPulseTimes = false;

      PULSE_TIMES:

      // check if any pulse time data is available
      for (final TimeData timeData : allTimeData) {

         final int[] pulseTimes = timeData.pulseTime;

         if (pulseTimes != null) {

            for (final int pulseTime : pulseTimes) {
               if (pulseTime != 0) {
                  isPulseTimes = true;
                  break PULSE_TIMES;
               }
            }
         }
      }

      if (isPulseTimes == false) {

         // no pulse time data
         return;
      }

      final int numTimeSlices = allTimeData.length;

      final TIntArrayList allPulseTimes = new TIntArrayList(numTimeSlices * 3);
      final int[] allPulseTime_TimeIndex = new int[numTimeSlices];

      Arrays.fill(allPulseTime_TimeIndex, -1);

      int pulseTimesIndex = 0;

      for (int timeIndex = 0; timeIndex < numTimeSlices; timeIndex++) {

         final TimeData timeData = allTimeData[timeIndex];
         final int[] timeSlice_AllPulseTimes = timeData.pulseTime;

         if (timeSlice_AllPulseTimes != null) {

            boolean isTimeIndexSet = false;

            // loop: all pulse times within one time slice
            for (final int pulseTimeMS : timeSlice_AllPulseTimes) {

               if (pulseTimeMS != 0) {

                  if (pulseTimeMS == 65535) {

                     // ignore, this value occurred in daum data

                  } else {

                     allPulseTimes.add(pulseTimeMS);

                     if (!isTimeIndexSet) {

                        // set index only for the first pulse time
                        isTimeIndexSet = true;

                        allPulseTime_TimeIndex[timeIndex] = pulseTimesIndex;
                     }

                     pulseTimesIndex++;
                  }
               }
            }
         }
      }

      if (allPulseTimes.size() > 0) {

         pulseTime_Milliseconds = allPulseTimes.toArray();
         pulseTime_TimeIndex = allPulseTime_TimeIndex;
      }
   }

   /**
    * Creates a unique tour id depending on the tour start time and current time
    */
   public void createTourId() {

      final String uniqueKey = Long.toString(System.currentTimeMillis());

      createTourId(uniqueKey.substring(uniqueKey.length() - 5, uniqueKey.length()));
   }

   /**
    * Creates the unique tour id from the tour date/time and the unique key.
    *
    * @param uniqueKeySuffix
    *           Unique key to identify a tour, this <b>MUST</b> be an {@link Integer} value.
    * @return
    */
   public Long createTourId(final String uniqueKeySuffix) {

//      final String uniqueKey = Integer.toString(Math.abs(getStartDistance()));

      String tourIdKey;

      try {
         /*
          * this is the default implementation to create a tour id, but on the 5.5.2007 a
          * NumberFormatException occurred so the calculation for the tour id was adjusted
          */
         tourIdKey = Short.toString(startYear)
               + Short.toString(startMonth)
               + Short.toString(startDay)
               + Short.toString(startHour)
               + Short.toString(startMinute)
               //
               + uniqueKeySuffix;

         tourId = Long.valueOf(tourIdKey);

      } catch (final NumberFormatException e) {

         /*
          * the distance is shorted that the maximum of a Long datatype is not exceeded
          */
         try {

            tourIdKey = Short.toString(startYear)
                  + Short.toString(startMonth)
                  + Short.toString(startDay)
                  + Short.toString(startHour)
                  + Short.toString(startMinute)
                  //
                  + uniqueKeySuffix.substring(0, Math.min(5, uniqueKeySuffix.length()));

            tourId = Long.valueOf(tourIdKey);

         } catch (final NumberFormatException e2) {

            // this case happened when startMonth had a wrong value

            tourId = Long.valueOf(TimeTools.now().toInstant().toEpochMilli());
         }
      }

      return tourId;
   }

   /**
    * Creates a dummy tour id which should be replaced by setting the tour id with
    * {@link #createTourId()} or {@link #createTourId(String)}
    */
   public void createTourIdDummy() {
      tourId = System.nanoTime();
   }

   /**
    * Create a device marker at the current position
    *
    * @param timeData
    * @param serieIndex
    * @param relativeTime
    * @param distanceAbsolute
    */
   private void createTourMarker(final TimeData timeData,
                                 final int serieIndex,
                                 final int relativeTime,
                                 final float distanceAbsolute) {

      // create a new marker
      final TourMarker tourMarker = new TourMarker(this, ChartLabelMarker.MARKER_TYPE_DEVICE);

      /*
       * ??? timeData.marker was added until version 14.9 but I have no idea why this was added ???
       */
//      tourMarker.setTime((int) (relativeTime + timeData.marker));

      tourMarker.setTime(relativeTime, tourStartTime + (relativeTime * 1000));
      tourMarker.setDistance(distanceAbsolute);
      tourMarker.setSerieIndex(serieIndex);

      if (timeData.markerLabel == null) {
         tourMarker.setLabel(Messages.tour_data_label_device_marker);
      } else {
         tourMarker.setLabel(timeData.markerLabel);
      }

      tourMarkers.add(tourMarker);
   }

   public void dumpData() {

      final PrintStream out = System.out;

      out.println("----------------------------------------------------"); //$NON-NLS-1$
      out.println("TOUR DATA"); //$NON-NLS-1$
      out.println("----------------------------------------------------"); //$NON-NLS-1$
// out.println("Typ: " + getDeviceTourType()); //$NON-NLS-1$
      out.println("Date:               " + startDay + UI.SYMBOL_DOT + startMonth + UI.SYMBOL_DOT + startYear); //$NON-NLS-1$
      out.println("Time:               " + startHour + UI.SYMBOL_COLON + startMinute); //$NON-NLS-1$
      out.println("Total distance:     " + getStartDistance()); //$NON-NLS-1$
      // out.println("Distance:           " + getDistance());
      out.println("Altitude:           " + getStartAltitude()); //$NON-NLS-1$
      out.println("Pulse:              " + getStartPulse()); //$NON-NLS-1$
      out.println("Offset DD record:   " + offsetDDRecord); //$NON-NLS-1$
   }

   public void dumpTime() {
      final PrintStream out = System.out;

      out.print(
            (tourDeviceTime_Elapsed / 3600)
                  + UI.SYMBOL_COLON
                  + ((tourDeviceTime_Elapsed % 3600) / 60)
                  + UI.SYMBOL_COLON
                  + ((tourDeviceTime_Elapsed % 3600) % 60)
                  + UI.SPACE2);
      out.print(getTourDistance());
   }

   public void dumpTourTotal() {

      final PrintStream out = System.out;

      out.println("Tour distance (m):   " + getTourDistance()); //$NON-NLS-1$

      out.println(
            "Tour time:      " //$NON-NLS-1$
                  + (tourDeviceTime_Elapsed / 3600)
                  + UI.SYMBOL_COLON
                  + ((tourDeviceTime_Elapsed % 3600) / 60)
                  + UI.SYMBOL_COLON
                  + (tourDeviceTime_Elapsed % 3600) % 60);

      out.println(
            "Recorded time:      " //$NON-NLS-1$
                  + (getTourDeviceTime_Recorded() / 3600)
                  + UI.SYMBOL_COLON
                  + ((getTourDeviceTime_Recorded() % 3600) / 60)
                  + UI.SYMBOL_COLON
                  + (getTourDeviceTime_Recorded() % 3600) % 60);

      out.println(
            "Moving time:      " //$NON-NLS-1$
                  + (getTourComputedTime_Moving() / 3600)
                  + UI.SYMBOL_COLON
                  + ((getTourComputedTime_Moving() % 3600) / 60)
                  + UI.SYMBOL_COLON
                  + (getTourComputedTime_Moving() % 3600) % 60);

      out.println("Altitude up (m):   " + getTourAltUp()); //$NON-NLS-1$
      out.println("Altitude down (m):   " + getTourAltDown()); //$NON-NLS-1$
   }

   @Override
   public boolean equals(final Object obj) {

      if (this == obj) {
         return true;
      }

      if (obj instanceof TourData) {
         return tourId.longValue() == ((TourData) obj).tourId.longValue();
      }

      return false;
   }

   /**
    * Fill swim data into tourdata.
    *
    * @param tourData
    * @param allTourSwimData
    */
   public void finalizeTour_SwimData(final TourData tourData, final List<SwimData> allTourSwimData) {

      // check if swim data are available
      if (allTourSwimData == null) {
         return;
      }

      final long tourStartTime = tourData.getTourStartTimeMS();

      final int swimDataSize = allTourSwimData.size();

      final short[] lengthType = new short[swimDataSize];
      final short[] cadence = new short[swimDataSize];
      final short[] strokes = new short[swimDataSize];
      final short[] strokeStyle = new short[swimDataSize];
      final int[] swimTime = new int[swimDataSize];

      tourData.swim_LengthType = lengthType;
      tourData.swim_Cadence = cadence;
      tourData.swim_Strokes = strokes;
      tourData.swim_StrokeStyle = strokeStyle;
      tourData.swim_Time = swimTime;

      boolean isSwimLengthType = false;
      boolean isSwimCadence = false;
      boolean isSwimStrokes = false;
      boolean isSwimStrokeStyle = false;
      boolean isSwimTime = false;

      for (int swimSerieIndex = 0; swimSerieIndex < allTourSwimData.size(); swimSerieIndex++) {

         final SwimData swimData = allTourSwimData.get(swimSerieIndex);

         final long absoluteSwimTime = swimData.absoluteTime;
         final short relativeSwimTime = (short) ((absoluteSwimTime - tourStartTime) / 1000);

         final short swimLengthType = swimData.swim_LengthType;
         short swimCadence = swimData.swim_Cadence;
         short swimStrokes = swimData.swim_Strokes;
         final short swimStrokeStyle = swimData.swim_StrokeStyle;

         /*
          * Length type
          */
         if (swimLengthType != Short.MIN_VALUE && swimLengthType > 0) {
            isSwimLengthType = true;
         }

         /*
          * Cadence
          */
         if (swimCadence == Short.MIN_VALUE) {
            swimCadence = 0;
         }
         if (swimCadence > 0) {
            isSwimCadence = true;
         }

         /*
          * Strokes
          */
         if (swimStrokes == Short.MIN_VALUE) {
            swimStrokes = 0;
         }
         if (swimStrokes > 0) {
            isSwimStrokes = true;
         }

         /*
          * Stroke style
          */
         if (swimStrokeStyle != Short.MIN_VALUE && swimStrokeStyle > 0) {
            isSwimStrokeStyle = true;
         }

         /*
          * Swim time
          */
         if (relativeSwimTime > 0) {
            isSwimTime = true;
         }

         lengthType[swimSerieIndex] = swimLengthType;
         cadence[swimSerieIndex] = swimCadence;
         strokes[swimSerieIndex] = swimStrokes;
         strokeStyle[swimSerieIndex] = swimStrokeStyle;
         swimTime[swimSerieIndex] = relativeSwimTime;
      }

      /*
       * Cleanup data series
       */
      if (isSwimLengthType == false) {
         tourData.swim_LengthType = null;
      }
      if (isSwimStrokes == false) {
         tourData.swim_Strokes = null;
      }
      if (isSwimStrokeStyle == false) {
         tourData.swim_StrokeStyle = null;
      }
      if (isSwimTime == false) {
         tourData.swim_Time = null;
      }

      // cadence is very special
      if (isSwimCadence) {
         // removed 'normal' cadence data serie when swim cadence is available
         tourData.setCadenceSerie(null);
      } else {
         tourData.swim_Cadence = null;
      }
   }

   /**
    * Sets the paused time start and end arrays based on given arrays.
    * Note: This procedure ensures that the finalized arrays are of the same size.
    *
    * @param pausedTime_Start
    *           A given array of paused time start times
    * @param pausedTime_End
    *           A given array of paused time end times
    * @param pausedTime_Data
    *           A value of 1 indicate that it is an auto-pause
    */
   public void finalizeTour_TimerPauses(List<Long> pausedTime_Start,
                                        final List<Long> pausedTime_End,
                                        List<Long> pausedTime_Data) {

      if (pausedTime_Start == null || pausedTime_Start.isEmpty() ||
            pausedTime_End == null || pausedTime_End.isEmpty()) {

         return;
      }

      final int numPauses = pausedTime_End.size();

      /*
       * Ensure bounds
       */
      if (pausedTime_Start.size() > numPauses) {
         pausedTime_Start = pausedTime_Start.subList(0, numPauses);
      }
      if (pausedTime_Data != null && pausedTime_Data.size() > numPauses) {
         pausedTime_Data = pausedTime_Data.subList(0, numPauses);
      }

      setPausedTime_Start(pausedTime_Start.stream().mapToLong(l -> l).toArray());
      setPausedTime_End(pausedTime_End.stream().mapToLong(l -> l).toArray());

      if (pausedTime_Data != null) {
         setPausedTime_Data(pausedTime_Data.stream().mapToLong(l -> l).toArray());
      }

      setTourDeviceTime_Paused(getTotalTourTimerPauses());
   }

   /**
    * @return Returns the metric or imperial altimeter serie depending on the active measurement
    */
   public float[] getAltimeterSerie() {

      if (UI.UNIT_IS_ELEVATION_FOOT) {

         // use imperial system

         if (altimeterSerieImperial == null) {
            computeAltimeterGradientSerie();
         }
         return altimeterSerieImperial;

      } else {

         // use metric system

         if (altimeterSerie == null) {
            computeAltimeterGradientSerie();
         }
         return altimeterSerie;
      }
   }

   /**
    * @return Returns the metric or imperial altitude serie depending on the active measurement or
    *         <code>null</code> when altitude data serie is not available
    */
   public float[] getAltitudeSerie() {

      if (altitudeSerie == null) {
         return null;
      }

      if (UI.UNIT_IS_ELEVATION_FOOT) {

         // imperial system is used

         if (altitudeSerieImperial == null) {

            // compute imperial altitude

            altitudeSerieImperial = new float[altitudeSerie.length];

            for (int valueIndex = 0; valueIndex < altitudeSerie.length; valueIndex++) {
               altitudeSerieImperial[valueIndex] = altitudeSerie[valueIndex] / UI.UNIT_FOOT;
            }
         }
         return altitudeSerieImperial;

      } else {

         return altitudeSerie;
      }
   }

   /**
    * @param isForceSmoothing
    * @return Returns smoothed altitude values (according to the measurement system) when they are
    *         set to be smoothed otherwise it returns normal altitude values or <code>null</code>
    *         when altitude is not available.
    */
   public float[] getAltitudeSmoothedSerie(final boolean isForceSmoothing) {

      if (altitudeSerie == null) {
         return null;
      }

// ??? HAVE NO IDEA WHY THIS IS USED ???
//      if (isForceSmoothing) {
//
//         // smooth altitude
//         computeSmoothedDataSeries();
//
//      } else {
//
      if (altitudeSerieSmoothed != null) {

         // return already smoothed altitude values

         if (UI.UNIT_IS_ELEVATION_FOOT) {

            // imperial system is used

            return altitudeSerieImperialSmoothed;

         } else {
            return altitudeSerieSmoothed;
         }
      }
//      }

      if (altitudeSerieSmoothed == null) {

         // smoothed altitude values are not available
         return getAltitudeSerie();

      } else {

         if (UI.UNIT_IS_ELEVATION_FOOT) {

            // imperial system is used

            return altitudeSerieImperialSmoothed;

         } else {
            return altitudeSerieSmoothed;
         }
      }
   }

   /**
    * @return the {@link #avgAltitudeChange}
    */
   public int getAvgAltitudeChange() {
      return avgAltitudeChange;
   }

   /**
    * @return the avgCadence
    */
   public float getAvgCadence() {
      return avgCadence;
   }

   /**
    * @return the avgPulse
    */
   public float getAvgPulse() {
      return avgPulse;
   }

   /**
    * @return Returns metric average temperature
    */
   public float getAvgTemperature() {
      return avgTemperature;
   }

   public short[] getBattery_Percentage() {
      return battery_Percentage;
   }

   public short getBattery_Percentage_End() {
      return battery_Percentage_End;
   }

   public short getBattery_Percentage_Start() {
      return battery_Percentage_Start;
   }

   /**
    * @return Returns battery time in seconds, relative to the tour start time
    */
   public int[] getBattery_Time() {
      return battery_Time;
   }

   /**
    * @return Returns the body fat.
    */
   public float getBodyFat() {
      return bodyFat;
   }

   /**
    * @return Returns the body weight.
    */
   public float getBodyWeight() {
      return bodyWeight;
   }

   private int getBreakTime() {

      if (timeSerie == null || timeSerie.length == 0) {
         return 0;
      }

      return getBreakTime(0, timeSerie.length, BreakTimeTool.getPrefValues());
   }

   public int getBreakTime(final int startIndex, final int endIndex) {

      if (timeSerie == null) {
         return 0;
      }

      return getBreakTime(startIndex, endIndex, BreakTimeTool.getPrefValues());
   }

   /**
    * Computes break time between start and end index when and when a break occurs
    *
    * @param startIndex
    * @param endIndex
    * @param breakTimeTool
    * @return Returns the break time in seconds
    */
   private int getBreakTime(final int startIndex, final int endIndex, final BreakTimeTool breakTimeTool) {

      // check required data
      if (timeSerie == null || distanceSerie == null) {
         return 0;
      }

      // check if break time for each time slice is already computed (for the whole tour)
      if (breakTimeSerie == null) {

         if (deviceTimeInterval == -1) {

            // variable time slices

            computeBreakTimeVariable(breakTimeTool);

         } else {

            // fixed time slices

            if (deviceTimeInterval == 0) {
               return 0;
            }

            final int minBreakTime = 20;

            computeBreakTimeFixed(minBreakTime / deviceTimeInterval);
         }

         computeTourMovingTime();
      }

      return computeBreakTime(startIndex, endIndex);
   }

   public boolean[] getBreakTimeSerie() {

      if (breakTimeSerie == null) {
         getBreakTime();
      }

      return breakTimeSerie;
   }

   public boolean[] getCadenceGaps() {

      if (cadenceSerie == null) {
         return null;
      }

      if (_cadenceGaps != null) {
         return _cadenceGaps;
      }

      _cadenceGaps = new boolean[cadenceSerie.length];

      for (int serieIndex = 0; serieIndex < cadenceSerie.length; serieIndex++) {

         final float cadence = cadenceSerie[serieIndex];
         _cadenceGaps[serieIndex] = cadence == 0 ? true : false;
      }

      return _cadenceGaps;
   }

   public float getCadenceMultiplier() {
      return cadenceMultiplier;
   }

   public float[] getCadenceSerie() {

      if (isSwimCadence) {

         // cadence is computed from swim cadence, these cadence values are not saaved

         return getSwim_Cadence();

      } else {

         return cadenceSerie;
      }
   }

   /**
    * @return Returns cadence data serie which is multiplied with the {@link #cadenceMultiplier}
    */
   public float[] getCadenceSerieWithMuliplier() {

      if (isSwimCadence) {

         return getSwim_Cadence();

      } else {

         if (isMultipleTours) {

            /*
             * Each tour of a multiple tour do already contain the cadence values with multiplier,
             * so an additional multiplication would display the wrong value.
             */

            return cadenceSerie;

         } else {

            if (cadenceMultiplier != 1.0 && cadenceSerie != null) {

               if (cadenceSerieWithMultiplier == null) {

                  // create cadence with multiplier

                  cadenceSerieWithMultiplier = new float[cadenceSerie.length];

                  for (int serieIndex = 0; serieIndex < cadenceSerie.length; serieIndex++) {
                     cadenceSerieWithMultiplier[serieIndex] = cadenceSerie[serieIndex] * cadenceMultiplier;
                  }
               }

               return cadenceSerieWithMultiplier;
            }
         }

         return cadenceSerie;
      }
   }

   public int getCadenceZone_FastTime() {
      return cadenceZone_FastTime;
   }

   public int getCadenceZone_SlowTime() {
      return cadenceZone_SlowTime;
   }

   public int getCadenceZones_DelimiterValue() {
      return cadenceZones_DelimiterValue;
   }

   /**
    * @return Returns the calories or <code>0</code> when calories are not available.
    */
   public int getCalories() {

      if (calories == null) {
         return 0;
      }

      return calories;
   }

   public int getConconiDeflection() {
      return conconiDeflection;
   }

   /**
    * @return Returns the person for which the tour is saved or the active person when
    *         {@link TourData} contains multiple tours or <code>null</code> when the tour is not
    *         saved in the database.
    */
   public TourPerson getDataPerson() {

      TourPerson dataPerson = null;

      if (isMultipleTours) {

         // multiple tours do not have a person, get active person

         dataPerson = TourbookPlugin.getActivePerson();

      } else {

         dataPerson = tourPerson;
      }

      return dataPerson;
   }

   /**
    * @return Returns {@link ZonedDateTime} when the tour was created or <code>null</code> when
    *         date/time is not available
    */
   @JsonIgnore
   public ZonedDateTime getDateTimeCreated() {

      if (_dateTimeCreated != null || dateTimeCreated == 0) {
         return _dateTimeCreated;
      }

      _dateTimeCreated = TimeTools.createDateTimeFromYMDhms(dateTimeCreated);

      return _dateTimeCreated;
   }

   /**
    * @return Returns {@link ZonedDateTime} when the tour was modified or <code>null</code> when
    *         date/time is not available
    */
   @JsonIgnore
   public ZonedDateTime getDateTimeModified() {

      if (_dateTimeModified != null || dateTimeModified == 0) {
         return _dateTimeModified;
      }

      _dateTimeModified = TimeTools.createDateTimeFromYMDhms(dateTimeModified);

      return _dateTimeModified;
   }

   public String getDeviceFirmwareVersion() {
      return deviceFirmwareVersion == null ? UI.EMPTY_STRING : deviceFirmwareVersion;
   }

   public String getDeviceId() {
      return devicePluginId;
   }

   /**
    * This info is only displayed in viewer columns
    *
    * @return
    */
   public String getDeviceModeName() {
      return deviceModeName;
   }

   /**
    * @return Returns device name which is displayed in the tour editor info tab
    */
   public String getDeviceName() {

      if ((devicePluginId != null) && devicePluginId.equals(DEVICE_ID_FOR_MANUAL_TOUR)) {

         return Messages.tour_data_label_manually_created_tour;

      } else if ((devicePluginName == null) || (devicePluginName.length() == 0)) {

         return UI.EMPTY_STRING;

      } else {

         return devicePluginName;
      }
   }

   public String getDevicePluginName() {
      return devicePluginName;
   }

   public Set<DeviceSensorValue> getDeviceSensorValues() {
      return deviceSensorValues;
   }

   /**
    * @return Returns the time difference between 2 time slices or <code>-1</code> when the time
    *         slices are unequally
    */
   public short getDeviceTimeInterval() {
      return deviceTimeInterval;
   }

   public String getDeviceTourType() {
      return deviceTourType;
   }

   /**
    * @return Returns the distance data serie for the current measurement system, this can be metric
    *         or imperial
    */
   public double[] getDistanceSerieDouble() {

      if (distanceSerie == null) {
         return null;
      }

      double[] serie = null;

      if (net.tourbook.common.UI.UNIT_IS_DISTANCE_MILE) {

         // use mile

         if (distanceSerieDouble_Mile == null) {

            distanceSerieDouble_Mile = new double[distanceSerie.length];

            for (int valueIndex = 0; valueIndex < distanceSerie.length; valueIndex++) {
               distanceSerieDouble_Mile[valueIndex] = distanceSerie[valueIndex] / UI.UNIT_MILE;
            }
         }

         serie = distanceSerieDouble_Mile;

      } else if (net.tourbook.common.UI.UNIT_IS_DISTANCE_NAUTICAL_MILE) {

         // use nautical mile

         if (distanceSerieDouble_NauticalMile == null) {

            distanceSerieDouble_NauticalMile = new double[distanceSerie.length];

            for (int valueIndex = 0; valueIndex < distanceSerie.length; valueIndex++) {
               distanceSerieDouble_NauticalMile[valueIndex] = distanceSerie[valueIndex] / UI.UNIT_NAUTICAL_MILE;
            }
         }

         serie = distanceSerieDouble_NauticalMile;

      } else {

         // use default, kilometer

         if (distanceSerieDouble_Kilometer == null) {

            distanceSerieDouble_Kilometer = new double[distanceSerie.length];

            for (int valueIndex = 0; valueIndex < distanceSerie.length; valueIndex++) {
               distanceSerieDouble_Kilometer[valueIndex] = distanceSerie[valueIndex];
            }
         }

         serie = distanceSerieDouble_Kilometer;

      }

      return serie;
   }

   public short getDpTolerance() {
      return dpTolerance;
   }

   /**
    * @param values
    * @return Returns first value which is not 0
    */
   private short getFirstNot0Value(final short[] values) {

      for (final short value : values) {

         if (value > 0 || value < 0) {
            return value;
         }
      }

      return 0;
   }

   public int getFrontShiftCount() {
      return frontShiftCount;
   }

   /**
    * @return Returns <code>null</code> when tour do not contain photos, otherwise a list of
    *         {@link Photo}'s is returned.
    */
   public ArrayList<Photo> getGalleryPhotos() {

      if (tourPhotos.isEmpty()) {
         return null;
      }

      // photos are available in this tour

      if (_galleryPhotos.size() > 0) {

         // photos are set
         return _galleryPhotos;
      }

      // photos are not yet set

      createGalleryPhotos();

      return _galleryPhotos;
   }

   /**
    * @return Returns <code>null</code> when gears are not available otherwise it returns gears with
    *         this format
    *         <p>
    *         _gears[0] = gear ratio<br>
    *         _gears[1] = front gear teeth<br>
    *         _gears[2] = rear gear teeth<br>
    *         _gears[3] = front gear number, starting with 1<br>
    *         _gears[4] = rear gear number, starting with 1<br>
    */
   public float[][] getGears() {

      if (gearSerie == null || timeSerie == null) {
         return null;
      }

      if (_gears != null) {
         return _gears;
      }

      /*
       * Create gears from gear raw data
       */

      final int gearSize = timeSerie.length;

      _gears = new float[5][gearSize];

      for (int gearIndex = 0; gearIndex < gearSize; gearIndex++) {

         final long gearRaw = gearSerie[gearIndex];

         final float frontTeeth = (gearRaw >> 24 & 0xff);
         final float rearTeeth = (gearRaw >> 8 & 0xff);

         final float frontGearNo = (gearRaw >> 16 & 0xff);
         final float rearGearNo = (gearRaw >> 0 & 0xff);

         final float gearRatio = frontTeeth / rearTeeth;

         _gears[0][gearIndex] = gearRatio;
         _gears[1][gearIndex] = frontTeeth;
         _gears[2][gearIndex] = rearTeeth;
         _gears[3][gearIndex] = frontGearNo;
         _gears[4][gearIndex] = rearGearNo;
      }

      return _gears;
   }

   /**
    * @return Returns bounds of the tour in latitude/longitude:
    *         <p>
    *         1st item contains lat/lon minimum values<br>
    *         2nd item contains lat/lon maximum values
    *         <p>
    *         Returns <code>null</code> when geo positions are not available.
    */

   public GeoPosition[] getGeoBounds() {

      if (_geoBounds == null) {
         computeGeo_Bounds();
      }

      return _geoBounds;
   }

   /**
    * @return Returns the metric or imperial altimeter serie depending on the active measurement
    */
   public float[] getGradientSerie() {

      if (gradientSerie == null) {
         computeAltimeterGradientSerie();
      }

      return gradientSerie;
   }

   public HrZoneContext getHrZoneContext() {

      if (_hrZoneContext == null) {
         computeHrZones();
      }

      return _hrZoneContext;
   }

   /**
    * @return Returns all available HR zones. How many zones are really used, depends on the
    *         {@link TourPerson} and how many zones are defined for the person.
    *         <p>
    *         Each tour can have up to 10 HR zones, when HR zone is <code>-1</code> then this zone
    *         is not set.
    */
   public int[] getHrZones() {

      if (_hrZones == null) {
         computeHrZones();
      }

      return _hrZones;
   }

   /**
    * @return Returns the import file name or <code>null</code> when not available.
    */
   public String getImportFileName() {

      return StringUtils.hasContent(tourImportFileName) ? tourImportFileName : null;
   }

   /**
    * @return Returns the import file path (folder) or <code>null</code> when not available.
    */
   public String getImportFilePath() {

      return StringUtils.hasContent(tourImportFilePath) ? tourImportFilePath : null;
   }

   /**
    * @return Returns the full import file path name or <code>null</code> when not available.
    */
   public String getImportFilePathName() {

      if (tourImportFilePath != null && tourImportFilePath.length() > 0) {

         try {

            final Path importPath = Paths.get(tourImportFilePath, tourImportFileName);

            return importPath.toString();

         } catch (final Exception e) {
            // folder can be invalid
         }
      }

      return null;
   }

   /**
    * @return Returns the full import file path name or an empty string when not available.
    */
   public String getImportFilePathNameText() {

      if (StringUtils.isNullOrEmpty(tourImportFilePath)) {

         if (isManualTour()) {
            return UI.EMPTY_STRING;
         } else {
            return Messages.tour_data_label_feature_since_version_9_01;
         }

      } else {

         try {

            final Path importPath = Paths.get(tourImportFilePath, tourImportFileName);

            return importPath.toString();

         } catch (final Exception e) {
            // folder can be invalid
         }
      }

      return UI.EMPTY_STRING;
   }

   /**
    * @return the maxAltitude
    */
   public float getMaxAltitude() {
      return maxAltitude;
   }

   public float getMaxPace() {
      return maxPace;
   }

   /**
    * @return the maxPulse
    */
   public float getMaxPulse() {
      return maxPulse;
   }

   /**
    * @return the maxSpeed
    */
   public float getMaxSpeed() {
      return maxSpeed;
   }

   public int getMergedAltitudeOffset() {
      return mergedAltitudeOffset;
   }

   public int getMergedTourTimeOffset() {
      return mergedTourTimeOffset;
   }

   public TourData getMergeSourceTourData() {
      return _mergeSourceTourData;
   }

   /**
    * @return tour id which is merged into this tour
    */
   public Long getMergeSourceTourId() {
      return mergeSourceTourId;
   }

   /**
    * @return tour Id into which this tour is merged or <code>null</code> when this tour is not
    *         merged into another tour
    */
   public Long getMergeTargetTourId() {
      return mergeTargetTourId;
   }

   /**
    * @return Returns the distance serie from the metric system, the distance serie is <b>always</b>
    *         saved in the database with the metric system
    */
   public float[] getMetricDistanceSerie() {
      return distanceSerie;
   }

   /**
    * @param geoAccuracy
    * @param distanceAccuracy
    * @return Returns tour lat/lon data multiplied by {@link #NORMALIZED_GEO_DATA_FACTOR} and
    *         normalized (removed duplicates), or <code>null</code> when not available
    */
   public NormalizedGeoData getNormalizedLatLon(final int geoAccuracy, final int distanceAccuracy) {

      if (latitudeSerie == null) {
         return null;
      }

      if (_rasterizedLatLon == null || _normalizedGeoAccuracy != geoAccuracy) {
         _rasterizedLatLon = computeGeo_NormalizeLatLon(0, latitudeSerie.length - 1, geoAccuracy, distanceAccuracy);
      }

      return _rasterizedLatLon;
   }

   /**
    * @return Returns number of HR zones which are available for this tour. Will be 0 when HR zones
    *         are not defined.
    */
   public int getNumberOfHrZones() {

      if (_hrZones == null) {
         computeHrZones();
      }

      return numberOfHrZones;
   }

   public int getNumberOfTimeSlices() {
      return numberOfTimeSlices;
   }

   /**
    * @return Returns pace minute data serie in the current measurement system
    */
   public float[] getPaceSerie() {

      if (net.tourbook.common.UI.UNIT_IS_PACE_MIN_PER_MILE) {

         // use imperial system

         if (paceSerie_Minute_Imperial == null) {
            computeSpeedSeries();
         }

         return paceSerie_Minute_Imperial;

      } else {

         // use metric system

         if (paceSerie_Minute == null) {
            computeSpeedSeries();
         }

         return paceSerie_Minute;
      }
   }

   public float[] getPaceSerie_Summarized_Seconds() {

      if (net.tourbook.common.UI.UNIT_IS_PACE_MIN_PER_MILE) {

         // use imperial system

         if (paceSerie_Summarized_Seconds_Imperial == null) {
            computeSpeedSeries();
         }

         return paceSerie_Summarized_Seconds_Imperial;

      } else {

         // use metric system

         if (paceSerie_Summarized_Seconds == null) {
            computeSpeedSeries();
         }

         return paceSerie_Summarized_Seconds;
      }
   }

   public float[] getPaceSerieSeconds() {

      if (net.tourbook.common.UI.UNIT_IS_PACE_MIN_PER_MILE) {

         // use imperial system

         if (paceSerie_Seconds_Imperial == null) {
            computeSpeedSeries();
         }

         return paceSerie_Seconds_Imperial;

      } else {

         // use metric system

         if (paceSerie_Seconds == null) {
            computeSpeedSeries();
         }

         return paceSerie_Seconds;
      }
   }

   /**
    * Calculates the total amount of paused time between a start and an end indices
    *
    * @param tourStartIndex
    * @param tourEndIndex
    * @return Returns the paused time in seconds
    */
   public int getPausedTime(final int tourStartIndex, final int tourEndIndex) {

      if (timeSerie == null
            || pausedTime_Start == null
            || tourStartIndex < 0
            || tourEndIndex < 0
            || tourStartIndex == tourEndIndex
            || tourStartIndex > timeSerie.length
            || tourEndIndex > timeSerie.length) {

         return 0;
      }

      final int tour_RelativeStartTime = timeSerie[tourStartIndex];
      final int tour_RelativeEndTime = timeSerie[tourEndIndex];

      int totalPausedTime = 0;

      // loop: all tour pauses
      for (int index = 0; index < pausedTime_Start.length; ++index) {

         final long paused_AbsoluteStartTime_MS = pausedTime_Start[index];
         final long paused_RelativeStartTime = (paused_AbsoluteStartTime_MS - tourStartTime) / 1000;

         // check if tour segment is within pauses
         if (paused_RelativeStartTime > tour_RelativeEndTime) {

            // pause is starting after tour segment -> all is done

            break;
         }

         if (paused_RelativeStartTime >= tour_RelativeStartTime
               && paused_RelativeStartTime <= tour_RelativeEndTime) {

            final long paused_AbsoluteEndTime_MS = pausedTime_End[index];
            final long paused_RelativeEndTime = (paused_AbsoluteEndTime_MS - tourStartTime) / 1000;

            long pausedTimeDiff = 0;

            if (paused_RelativeEndTime > tour_RelativeEndTime) {

               // pause continues after tour segment -> clip paused time

               pausedTimeDiff = tour_RelativeEndTime - paused_RelativeStartTime;

            } else {

               pausedTimeDiff = paused_RelativeEndTime - paused_RelativeStartTime;
            }

            totalPausedTime += pausedTimeDiff;
         }
      }

      return totalPausedTime;
   }

   public long[] getPausedTime_Data() {
      return pausedTime_Data;
   }

   public long[] getPausedTime_End() {
      return pausedTime_End;
   }

   public long[] getPausedTime_Start() {
      return pausedTime_Start;
   }

   public int getPhotoTimeAdjustment() {
      return photoTimeAdjustment;
   }

   public float getPower_Avg() {
      return power_Avg;
   }

   public float getPower_AvgLeftPedalSmoothness() {
      return power_AvgLeftPedalSmoothness;
   }

   public float getPower_AvgLeftTorqueEffectiveness() {
      return power_AvgLeftTorqueEffectiveness;
   }

   public float getPower_AvgRightPedalSmoothness() {
      return power_AvgRightPedalSmoothness;
   }

   public float getPower_AvgRightTorqueEffectiveness() {
      return power_AvgRightTorqueEffectiveness;
   }

   public String getPower_DataSource() {
      return power_DataSource;
   }

   /**
    * @return Returns Functional Threshold Power (FTP)
    */
   public int getPower_FTP() {
      return power_FTP;
   }

   public float getPower_IntensityFactor() {
      return power_IntensityFactor;
   }

   public int getPower_Max() {
      return power_Max;
   }

   public int getPower_Normalized() {
      return power_Normalized;
   }

   public int getPower_PedalLeftRightBalance() {
      return power_PedalLeftRightBalance;
   }

   public long getPower_TotalWork() {
      return power_TotalWork;
   }

   public float getPower_TrainingStressScore() {
      return power_TrainingStressScore;
   }

   public float[] getPowerSerie() {

      if (powerSerie != null || isPowerSerieFromDevice) {
         return powerSerie;
      }

      if (speedSerie == null || gradientSerie == null) {
         computeDataSeries_Smoothed();
      }

      // check if required data series are available
      if ((speedSerie == null) || (gradientSerie == null)) {
         return null;
      }

      powerSerie = new float[timeSerie.length];

      final float weightBody = 75;
      final float weightBike = 10;
      final float bodyHeight = 188;

      final float cR = 0.008f; // Rollreibungskoeffizient Asphalt
      final float cD = 0.8f; // Streomungskoeffizient
      final float p = 1.145f; // 20C / 400m
//      float p = 0.968f; // 10C / 2000m

      final float weightTotal = weightBody + weightBike;
      final float bsa = (float) (0.007184f * Math.pow(weightBody, 0.425) * Math.pow(bodyHeight, 0.725));
      final float aP = bsa * 0.185f;

      final float roll = weightTotal * 9.81f * cR;
      final float slope = weightTotal * 9.81f; // * gradient/100
      final float air = 0.5f * p * cD * aP; // * v2;

//      int joule = 0;
//      int prefTime = 0;

      for (int timeIndex = 0; timeIndex < timeSerie.length; timeIndex++) {

         final float speed = speedSerie[timeIndex] / 3.6f; // speed km/h -> m/s
         float gradient = gradientSerie[timeIndex] / 100; // gradient (%) /100

         // adjust computed errors
//         if (gradient < 0.04 && gradient > 0) {
//            gradient *= 0.5;
////            gradient = 0;
//         }

         if (gradient < 0) {
            if (gradient < -0.02) {
               gradient *= 3;
            } else {
               gradient *= 1.5;
            }
         }

         final float slopeTotal = slope * gradient;
         final float airTotal = air * speed * speed;

         final float total = roll + airTotal + slopeTotal;

         float pTotal = total * speed;

//         if (pTotal > 600) {
//            pTotal = pTotal * 1;
//         }
         pTotal = pTotal < 0 ? 0 : pTotal;

         powerSerie[timeIndex] = pTotal;

//         final int currentTime = timeSerie[timeIndex];
//         joule += pTotal * (currentTime - prefTime);

//         prefTime = currentTime;
      }

      return powerSerie;
   }

   /**
    * @return Pulse values computed from the pulse times in {@link #pulseTime_Milliseconds}.
    *         One pulse value is the average of all pulse times within one timeslice.
    */
   public float[] getPulse_AvgBpmFromRRIntervals() {

      if (pulseSerie_FromTime != null) {
         return pulseSerie_FromTime;
      }

      /**
       * !!! VERY IMPORTANT !!!
       * <P>
       * The check for
       * <P>
       * <code>
       *    pulseTime_Milliseconds == null || pulseTime_TimeIndex == null
       * </code>
       * MUST BE AFTER
       * <code>
       *    pulseSerie_FromTime != null
       * </code>
       * OTHERWISE JOINED TOURS DO NOT WORK !!!
       */

      // ensure that needed data are available
      if (pulseTime_Milliseconds == null || pulseTime_TimeIndex == null) {
         return null;
      }

      /*
       * Compute pulse serie from pulse times
       */

      final int numTimeSlices = timeSerie.length;

      pulseSerie_FromTime = new float[numTimeSlices];

      /*
       * Garmin Edge 1030 creates R-R intervals with 5000 ms when there is no heartbeat value or
       * the devices signal is interrupted !!!
       */

      for (int serieIndex = 0; serieIndex < numTimeSlices - 1; serieIndex++) {

         final int rrIndex_FromTimeSerie = pulseTime_TimeIndex[serieIndex];
         final int rrIndex_FromTimeSerie_Next = pulseTime_TimeIndex[serieIndex + 1];

         if (serieIndex > 0 && (rrIndex_FromTimeSerie < 0 || rrIndex_FromTimeSerie_Next < 0)) {

            // time index can be -1 -> heartbeat is below 60 bpm -> use value from the previous time slice

            if (rrIndex_FromTimeSerie == -1) {

               pulseSerie_FromTime[serieIndex] = pulseSerie_FromTime[serieIndex - 1];

            } else {

               final int pulseTimeMS = pulseTime_Milliseconds[rrIndex_FromTimeSerie];

               if (pulseTimeMS == 5_000) {

                  pulseSerie_FromTime[serieIndex] = 0;

               } else {

                  pulseSerie_FromTime[serieIndex] = pulseSerie_FromTime[serieIndex - 1];
               }
            }

            continue;
         }

         if (rrIndex_FromTimeSerie < 0 || rrIndex_FromTimeSerie_Next < 0) {

            continue;
         }

         float pulseFromPulseTime = 0;

         final int numPulseTimes = rrIndex_FromTimeSerie_Next - rrIndex_FromTimeSerie;

         if (numPulseTimes == 0) {

            // there is only 1 pulse time

            final int pulseTimeMS = pulseTime_Milliseconds[rrIndex_FromTimeSerie];

            if (pulseTimeMS > 0 && pulseTimeMS != 5_000) {

               pulseFromPulseTime = 60.0f / (pulseTimeMS / 1000.0f);
            }

         } else {

            long sumPulseTimeMS = 0;

            for (int avgSerieIndex = rrIndex_FromTimeSerie; avgSerieIndex < rrIndex_FromTimeSerie_Next; avgSerieIndex++) {

               final int pulseTimeMS = pulseTime_Milliseconds[avgSerieIndex];

               if (pulseTimeMS != 5_000) {
                  sumPulseTimeMS += pulseTimeMS;
               }

            }

            if (sumPulseTimeMS > 0) {

               final float avgPulseTimeMS = sumPulseTimeMS / (float) numPulseTimes;
               pulseFromPulseTime = 60.0f / (avgPulseTimeMS / 1000.0f);
            }
         }

         pulseSerie_FromTime[serieIndex] = pulseFromPulseTime;
      }

      return pulseSerie_FromTime;
   }

   public String[] getPulse_RRIntervals() {

      if (pulseTime_TimeIndex == null) {
         return null;
      }

      if (pulseSerie_RRIntervals != null) {
         return pulseSerie_RRIntervals;
      }

      final int numTimeSlices = timeSerie.length;

      pulseSerie_RRIntervals = new String[numTimeSlices];

      for (int serieIndex = 0; serieIndex < numTimeSlices - 1; serieIndex++) {

         int rrIndex_Current = pulseTime_TimeIndex[serieIndex];
         final int rrIndex_Next = pulseTime_TimeIndex[serieIndex + 1];

         if (serieIndex > 0 && rrIndex_Current == -1) {

            final int rrIndex_Prev = pulseTime_TimeIndex[serieIndex - 1];

            if (rrIndex_Prev != -1) {

               final int rrIndexDiff = rrIndex_Next - rrIndex_Prev;

               if (rrIndexDiff > 2) {

                  /**
                   * Adjust current index when there is a gap between previous and next index,
                   * otherwise these values are not displayed
                   * <p>
                   * Example:
                   * <p>
                   * <code>
                   *
                   *     rrIndex_Current  = -1
                   *     rrIndex_Next     = 4162
                   *     rrIndex_Prev     = 4107
                   *     rrIndexDiff      = 55
                   *
                   * </code>
                   */

                  rrIndex_Current = rrIndex_Prev + 1;
               }
            }
         }

         final StringBuilder sb = new StringBuilder();

         if (rrIndex_Current >= 0 && rrIndex_Next >= 0) {

            final int numRR = rrIndex_Next - rrIndex_Current;
            if (numRR > 4) {
               sb.append(numRR + INTERVAL_SUMMARY_UNIT);
            }

            for (int rrIndex = rrIndex_Current; rrIndex < rrIndex_Next; rrIndex++) {

               final int rrValue = pulseTime_Milliseconds[rrIndex];

               final String space = rrIndex < rrIndex_Next - 1
                     ? UI.SPACE1
                     : UI.EMPTY_STRING;

               sb.append(rrValue + space);
            }

         } else if (rrIndex_Current >= 0) {

            final int rrValue = pulseTime_Milliseconds[rrIndex_Current];

            sb.append(rrValue);

         } else if (rrIndex_Current < 0) {

            sb.append(rrIndex_Current);

         } else if (rrIndex_Next < 0) {

            // translation is currently diabled because I cannot remember when this case occur
            sb.append("Next: " + rrIndex_Next); //$NON-NLS-1$

         }

         pulseSerie_RRIntervals[serieIndex] = sb.toString();
      }

      return pulseSerie_RRIntervals;
   }

   public float[] getPulse_SmoothedSerie() {

      if (pulseSerie == null) {
         return null;
      }

      if (_pulseSerie_Smoothed != null) {
         return _pulseSerie_Smoothed;
      }

      computePulseSmoothed();

      return _pulseSerie_Smoothed;
   }

   public int getRearShiftCount() {
      return rearShiftCount;
   }

   public int getRestPulse() {
      return restPulse;
   }

   /**
    * @return Returns the UI values for stance time.
    */
   public float[] getRunDyn_StanceTime() {

      if (_runDyn_StanceTime_UI == null && runDyn_StanceTime != null) {

         // create UI data serie

         final int serieSize = runDyn_StanceTime.length;

         _runDyn_StanceTime_UI = new float[serieSize];

         for (int serieIndex = 0; serieIndex < serieSize; serieIndex++) {
            _runDyn_StanceTime_UI[serieIndex] = runDyn_StanceTime[serieIndex];
         }
      }

      return _runDyn_StanceTime_UI;
   }

   public float getRunDyn_StanceTime_Avg() {
      return runDyn_StanceTime_Avg;
   }

   public short getRunDyn_StanceTime_Max() {
      return runDyn_StanceTime_Max;
   }

   public short getRunDyn_StanceTime_Min() {
      return runDyn_StanceTime_Min;
   }

   /**
    * @return Returns the UI values for stance time balance
    */
   public float[] getRunDyn_StanceTimeBalance() {

      if (_runDyn_StanceTimeBalance_UI == null && runDyn_StanceTimeBalance != null) {

         // create UI data serie

         final int serieSize = runDyn_StanceTimeBalance.length;

         _runDyn_StanceTimeBalance_UI = new float[serieSize];

         for (int serieIndex = 0; serieIndex < serieSize; serieIndex++) {
            _runDyn_StanceTimeBalance_UI[serieIndex] = runDyn_StanceTimeBalance[serieIndex] / RUN_DYN_DATA_MULTIPLIER;
         }
      }

      return _runDyn_StanceTimeBalance_UI;
   }

   public float getRunDyn_StanceTimeBalance_Avg() {
      return runDyn_StanceTimeBalance_Avg / TourData.RUN_DYN_DATA_MULTIPLIER;
   }

   public float getRunDyn_StanceTimeBalance_Max() {
      return runDyn_StanceTimeBalance_Max / TourData.RUN_DYN_DATA_MULTIPLIER;
   }

   public float getRunDyn_StanceTimeBalance_Min() {
      return runDyn_StanceTimeBalance_Min / TourData.RUN_DYN_DATA_MULTIPLIER;
   }

   /**
    * @return Returns the metric/imperial UI values for step length
    */
   public float[] getRunDyn_StepLength() {

      if (net.tourbook.common.UI.UNIT_IS_LENGTH_SMALL_INCH) {

         // use imperial system

         if (_runDyn_StepLength_UI_Imperial == null && runDyn_StepLength != null) {

            // create UI data serie

            final int serieSize = runDyn_StepLength.length;

            _runDyn_StepLength_UI_Imperial = new float[serieSize];

            for (int serieIndex = 0; serieIndex < serieSize; serieIndex++) {
               _runDyn_StepLength_UI_Imperial[serieIndex] = runDyn_StepLength[serieIndex] * UI.UNIT_VALUE_DISTANCE_MM_OR_INCH;
            }
         }

         return _runDyn_StepLength_UI_Imperial;

      } else {

         // use metric system

         if (_runDyn_StepLength_UI == null && runDyn_StepLength != null) {

            // create UI data serie

            final int serieSize = runDyn_StepLength.length;

            _runDyn_StepLength_UI = new float[serieSize];

            for (int serieIndex = 0; serieIndex < serieSize; serieIndex++) {
               _runDyn_StepLength_UI[serieIndex] = runDyn_StepLength[serieIndex];
            }
         }

         return _runDyn_StepLength_UI;
      }
   }

   public float getRunDyn_StepLength_Avg() {
      return runDyn_StepLength_Avg;
   }

   public short getRunDyn_StepLength_Max() {
      return runDyn_StepLength_Max;
   }

   public short getRunDyn_StepLength_Min() {
      return runDyn_StepLength_Min;
   }

   /**
    * @return Returns the metric/imperial UI values for vertical oscillation
    */
   public float[] getRunDyn_VerticalOscillation() {

      if (net.tourbook.common.UI.UNIT_IS_LENGTH_SMALL_INCH) {

         // use imperial system

         if (_runDyn_VerticalOscillation_UI_Imperial == null && runDyn_VerticalOscillation != null) {

            // create UI data serie

            final int serieSize = runDyn_VerticalOscillation.length;

            _runDyn_VerticalOscillation_UI_Imperial = new float[serieSize];

            for (int serieIndex = 0; serieIndex < serieSize; serieIndex++) {
               _runDyn_VerticalOscillation_UI_Imperial[serieIndex] =
                     runDyn_VerticalOscillation[serieIndex] / RUN_DYN_DATA_MULTIPLIER * UI.UNIT_VALUE_DISTANCE_MM_OR_INCH;
            }
         }

         return _runDyn_VerticalOscillation_UI_Imperial;

      } else {

         // use metric system

         if (_runDyn_VerticalOscillation_UI == null && runDyn_VerticalOscillation != null) {

            // create UI data serie

            final int serieSize = runDyn_VerticalOscillation.length;

            _runDyn_VerticalOscillation_UI = new float[serieSize];

            for (int serieIndex = 0; serieIndex < serieSize; serieIndex++) {
               _runDyn_VerticalOscillation_UI[serieIndex] = runDyn_VerticalOscillation[serieIndex] / RUN_DYN_DATA_MULTIPLIER;
            }
         }

         return _runDyn_VerticalOscillation_UI;
      }
   }

   public float getRunDyn_VerticalOscillation_Avg() {
      return runDyn_VerticalOscillation_Avg / TourData.RUN_DYN_DATA_MULTIPLIER;
   }

   public float getRunDyn_VerticalOscillation_Max() {
      return runDyn_VerticalOscillation_Max / TourData.RUN_DYN_DATA_MULTIPLIER;
   }

   public float getRunDyn_VerticalOscillation_Min() {
      return runDyn_VerticalOscillation_Min / TourData.RUN_DYN_DATA_MULTIPLIER;
   }

   /**
    * @return Returns the UI values for vertical ratio
    */
   public float[] getRunDyn_VerticalRatio() {

      if (_runDyn_VerticalRatio_UI == null && runDyn_VerticalRatio != null) {

         // create UI data serie

         final int serieSize = runDyn_VerticalRatio.length;

         _runDyn_VerticalRatio_UI = new float[serieSize];

         for (int serieIndex = 0; serieIndex < serieSize; serieIndex++) {
            _runDyn_VerticalRatio_UI[serieIndex] = runDyn_VerticalRatio[serieIndex] / RUN_DYN_DATA_MULTIPLIER;
         }
      }

      return _runDyn_VerticalRatio_UI;
   }

   public float getRunDyn_VerticalRatio_Avg() {
      return runDyn_VerticalRatio_Avg / TourData.RUN_DYN_DATA_MULTIPLIER;
   }

   public float getRunDyn_VerticalRatio_Max() {
      return runDyn_VerticalRatio_Max / TourData.RUN_DYN_DATA_MULTIPLIER;
   }

   public float getRunDyn_VerticalRatio_Min() {
      return runDyn_VerticalRatio_Min / TourData.RUN_DYN_DATA_MULTIPLIER;
   }

   /**
    * @return the speed data in the current measurement system, which is defined in
    *         {@link UI#UNIT_VALUE_DISTANCE}
    */
   public float[] getSpeedSerie() {

      if (isSpeedSerieFromDevice) {
         return getSpeedSerieInternal();
      }
      if (distanceSerie == null) {
         return null;
      }

      return getSpeedSerieInternal();
   }

   /**
    * @return the speed data in the current measurement system, which is defined in
    *         {@link UI#UNIT_VALUE_DISTANCE}
    */
   public float[] getSpeedSerie_Summarized() {

      if (isSpeedSerieFromDevice) {
         return getSpeedSerie_Summarized_Internal();
      }
      if (distanceSerie == null) {
         return null;
      }

      return getSpeedSerie_Summarized_Internal();
   }

   private float[] getSpeedSerie_Summarized_Internal() {

      computeSpeedSeries();

      /*
       * when the speed series are not computed, the internal algorithm will be used to create the
       * speed data serie
       */
      if (net.tourbook.common.UI.UNIT_IS_DISTANCE_MILE) {

         return speedSerie_Summarized_Mile;

      } else if (net.tourbook.common.UI.UNIT_IS_DISTANCE_NAUTICAL_MILE) {

         // use imperial system

         return speedSerie_Summarized_NauticalMile;

      } else {

         // use metric system

         return speedSerie_Summarized;
      }
   }

   public float[] getSpeedSerieFromDevice() {

      if (isSpeedSerieFromDevice) {
         return speedSerie;
      }

      return null;
   }

   private float[] getSpeedSerieInternal() {

      computeSpeedSeries();

      /*
       * when the speed series are not computed, the internal algorithm will be used to create the
       * speed data serie
       */
      if (net.tourbook.common.UI.UNIT_IS_DISTANCE_MILE) {

         return speedSerie_Mile;

      } else if (net.tourbook.common.UI.UNIT_IS_DISTANCE_NAUTICAL_MILE) {

         // use imperial system

         return speedSerie_NauticalMile;

      } else {

         // use metric system

         return speedSerie;
      }
   }

   /**
    * @return returns the speed data in the metric measurement system
    */
   public float[] getSpeedSerieMetric() {

      computeSpeedSeries();

      return speedSerie;
   }

   /**
    * @return Returns SRTM metric or imperial data serie depending on the active measurement or
    *         <code>null</code> when SRTM data serie is not available
    */
   @JsonIgnore
   public float[] getSRTMSerie() {

      if (latitudeSerie == null) {
         return null;
      }

      if (srtmSerie == null) {
         createSRTMDataSerie();
      }

      if (srtmSerie.length == 0) {
         // SRTM data are invalid
         return null;
      }

      if (UI.UNIT_IS_ELEVATION_FOOT) {

         // imperial system is used

         return srtmSerieImperial;

      } else {

         return srtmSerie;
      }
   }

   /**
    * @return Returned SRTM values:
    *         <p>
    *         metric <br>
    *         imperial
    *         <p>
    *         or <code>null</code> when SRTM data serie is not available
    */
   @JsonIgnore
   public float[][] getSRTMValues() {

      if (latitudeSerie == null) {
         return null;
      }

      if (srtmSerie == null) {
         createSRTMDataSerie();
      }

      if (srtmSerie.length == 0) {
         // invalid SRTM values
         return null;
      }

      return new float[][] { srtmSerie, srtmSerieImperial };
   }

   public short getStartAltitude() {
      return startAltitude;
   }

   public float getStartDistance() {
      return startDistance;
   }

   public short getStartPulse() {
      return startPulse;
   }

   /**
    * @return Returns the tour start date time in seconds of the day.
    */
   public int getStartTimeOfDay() {
      return (startHour * 3600) + (startMinute * 60) + startSecond;

   }

   public short getStartWeek() {
      return startWeek;
   }

   public short getStartWeekYear() {
      return startWeekYear;
   }

   @JsonIgnore
   public short getStartYear() {
      return startYear;
   }

   public short getSurfing_MinDistance() {
      return surfing_MinDistance;
   }

   public short getSurfing_MinSpeed_StartStop() {
      return surfing_MinSpeed_StartStop;
   }

   public short getSurfing_MinSpeed_Surfing() {
      return surfing_MinSpeed_Surfing;
   }

   public short getSurfing_MinTimeDuration() {
      return surfing_MinTimeDuration;
   }

   public short getSurfing_NumberOfEvents() {
      return surfing_NumberOfEvents;
   }

   /**
    * @return Returns the UI values for cadence.
    */
   public float[] getSwim_Cadence() {

      if (_swim_Cadence_UI == null) {
         _swim_Cadence_UI = createSwimUI_DataSerie(swim_Cadence);
      }

      return _swim_Cadence_UI;
   }

   /**
    * @return Returns the UI values for number of strokes.
    */
   public float[] getSwim_Strokes() {

      if (_swim_Strokes_UI == null) {
         _swim_Strokes_UI = createSwimUI_DataSerie(swim_Strokes);
      }

      return _swim_Strokes_UI;
   }

   /**
    * @return Returns the UI values for number of strokes.
    */
   public float[] getSwim_StrokeStyle() {

      if (_swim_StrokeStyle_UI == null) {
         _swim_StrokeStyle_UI = createSwimUI_DataSerie(swim_StrokeStyle);
      }

      return _swim_StrokeStyle_UI;
   }

   /**
    * @return Returns the UI values for number of strokes.
    */
   public float[] getSwim_Swolf() {

      if (_swim_Swolf == null) {
         _swim_Swolf = createSwimUI_SwolfDataSerie();
      }

      return _swim_Swolf;
   }

   /**
    * @return Returns the temperature serie for the current measurement system or <code>null</code>
    *         when temperature is not available
    */
   public float[] getTemperatureSerie() {

      if (temperatureSerie == null) {
         return null;
      }

      float[] serie;

      final float unitValueTempterature = UI.UNIT_VALUE_TEMPERATURE;
      final float fahrenheitMulti = UI.UNIT_FAHRENHEIT_MULTI;
      final float fahrenheitAdd = UI.UNIT_FAHRENHEIT_ADD;

      if (unitValueTempterature != 1) {

         // use imperial system

         if (temperatureSerieImperial == null) {

            // compute imperial data

            temperatureSerieImperial = new float[temperatureSerie.length];

            for (int valueIndex = 0; valueIndex < temperatureSerie.length; valueIndex++) {

               final float scaledTemperature = temperatureSerie[valueIndex];

               temperatureSerieImperial[valueIndex] = scaledTemperature * fahrenheitMulti + fahrenheitAdd;
            }
         }
         serie = temperatureSerieImperial;

      } else {

         // use metric system

         serie = temperatureSerie;
      }

      return serie;
   }

   /**
    * @return Returns time data serie in floating points which is used for drawing charts.
    */
   public double[] getTimeSerieDouble() {

      if (timeSerieDouble != null) {
         return timeSerieDouble;
      }

      if (timeSerie == null && timeSerieHistory == null) {
         return null;
      }

      if (timeSerie != null) {

         timeSerieDouble = new double[timeSerie.length];

         for (int serieIndex = 0; serieIndex < timeSerie.length; serieIndex++) {
            timeSerieDouble[serieIndex] = timeSerie[serieIndex];
         }

      } else if (timeSerieHistory != null) {

         timeSerieDouble = new double[timeSerieHistory.length];

         for (int serieIndex = 0; serieIndex < timeSerieHistory.length; serieIndex++) {
            timeSerieDouble[serieIndex] = timeSerieHistory[serieIndex];
         }
      }

      return timeSerieDouble;
   }

   /**
    * @return Returns time data serie in floating points which is used for drawing charts. Time
    *         serie is adjusted to the time shift 6:32 when CET (central european time) started at
    *         1. April 1893.
    */
   public double[] getTimeSerieWithTimeZoneAdjusted() {

      if (timeSerieWithTimeZoneAdjustment != null) {
         return timeSerieWithTimeZoneAdjustment;
      }

      if (timeSerie == null && timeSerieHistory == null) {
         return null;
      }

      final ZonedDateTime tourStartDefaultZone = getTourStartTime();
      final int utcZoneOffset = tourStartDefaultZone.getOffset().getTotalSeconds();

      final long tourStartUTC = tourStartDefaultZone.plusSeconds(utcZoneOffset).toInstant().toEpochMilli();
      final long tourEnd = tourEndTime;

      final ZoneId defaultZone = TimeTools.getDefaultTimeZone();

      if (defaultZone.getId().equals(TIME_ZONE_ID_EUROPE_BERLIN)) {

         final long beforeCET = net.tourbook.common.UI.beforeCET;

         if (tourStartUTC < beforeCET && tourEnd > net.tourbook.common.UI.afterCETBegin) {

            // tour overlaps CET begin

            if (timeSerie != null) {

               timeSerieWithTimeZoneAdjustment = new double[timeSerie.length];

               for (int serieIndex = 0; serieIndex < timeSerie.length; serieIndex++) {
                  timeSerieWithTimeZoneAdjustment[serieIndex] = timeSerie[serieIndex];
               }

            } else if (timeSerieHistory != null) {

               timeSerieWithTimeZoneAdjustment = new double[timeSerieHistory.length];

               for (int serieIndex = 0; serieIndex < timeSerieHistory.length; serieIndex++) {

                  long historyTimeSlice = timeSerieHistory[serieIndex];

                  final long absoluteUTCTime = tourStartUTC + historyTimeSlice * 1000;

                  if (absoluteUTCTime > beforeCET) {
                     historyTimeSlice += net.tourbook.common.UI.BERLIN_HISTORY_ADJUSTMENT;
                  }

                  timeSerieWithTimeZoneAdjustment[serieIndex] = historyTimeSlice;
               }
            }

            return timeSerieWithTimeZoneAdjustment;
         }
      }

      return getTimeSerieDouble();
   }

   /**
    * @return Returns Tour time zone ID or <code>null</code> when the time zone ID is not available.
    */
   public String getTimeZoneId() {
      return timeZoneId;
   }

   /**
    * @return Returns the tour time zone id, when the tour time zone is not set in the tour, then
    *         the default time zone is returned which is defined in the preferences.
    */
   @JsonIgnore
   public ZoneId getTimeZoneIdWithDefault() {

      final String zoneIdRaw = timeZoneId == null //

            ? TimeTools.getDefaultTimeZoneId()
            : timeZoneId;

      final ZoneId tzId = ZoneId.of(zoneIdRaw);

      return tzId;
   }

   private long getTotalTourTimerPauses() {

      if (pausedTime_Start == null || pausedTime_Start.length == 0 ||
            pausedTime_End == null || pausedTime_End.length == 0) {
         return 0;
      }

      long totalTourTimerPauses = 0;
      for (int index = 0; index < pausedTime_Start.length; ++index) {
         totalTourTimerPauses += pausedTime_End[index] - pausedTime_Start[index];
      }

      return Math.round(totalTourTimerPauses / 1000f);
   }

   /**
    * @return Returns elevation loss in metric system (m)
    */
   public int getTourAltDown() {
      return tourAltDown;
   }

   /**
    * @return Returns elevation gain in metric system (m)
    */
   public int getTourAltUp() {
      return tourAltUp;
   }

   public TourBike getTourBike() {
      return tourBike;
   }

   /**
    * @return Returns the total moving time in seconds.
    */
   public long getTourComputedTime_Moving() {
      return tourComputedTime_Moving;
   }

   /**
    * !!! THIS VALUE IS NOT CACHED BECAUSE WHEN THE DEFAULT TIME ZONE IS CHANGING THEN THIS VALUE IS
    * WRONG !!!
    */
   @JsonIgnore
   public TourDateTime getTourDateTime() {

      return TimeTools.createTourDateTime(tourStartTime, timeZoneId);
   }

   /**
    * @return Returns {@link #tourDescription} or an empty string when value is not set.
    */
   public String getTourDescription() {
      return tourDescription == null ? UI.EMPTY_STRING : tourDescription;
   }

   /**
    * @return Returns the total elapsed time in seconds
    */
   public long getTourDeviceTime_Elapsed() {
      return tourDeviceTime_Elapsed;
   }

   /**
    * @return Returns the total paused time in seconds.
    */
   public long getTourDeviceTime_Paused() {
      return tourDeviceTime_Paused;
   }

   /**
    * @return Returns the total recorded time in seconds.
    */
   public long getTourDeviceTime_Recorded() {
      return tourDeviceTime_Recorded;
   }

   /**
    * @return the tour distance in metric measurement system
    */
   public float getTourDistance() {
      return tourDistance;
   }

   /**
    * @return Returns {@link #tourEndPlace} or an empty string when value is not set.
    */
   public String getTourEndPlace() {
      return tourEndPlace == null ? UI.EMPTY_STRING : tourEndPlace;
   }

   /**
    * @return Returns tour end time in ms, this value should be {@link #tourStartTime} +
    *         {@link #tourDeviceTime_Elapsed}
    */
   public long getTourEndTimeMS() {
      return tourEndTime;
   }

   /**
    * @return Returns the unique key in the database for this {@link TourData} entity
    */
   public Long getTourId() {
      return tourId;
   }

   /**
    * @return Returns a set with all {@link TourMarker} for the tour or an empty set when markers
    *         are not available.
    */
   @JsonIgnore
   public Set<TourMarker> getTourMarkers() {
      return tourMarkers;
   }

   /**
    * @return Returns {@link TourMarker}'s sorted by serie index.
    */
   @JsonProperty("tourMarkers")
   public ArrayList<TourMarker> getTourMarkersSorted() {

      if (_sortedMarkers != null) {
         return _sortedMarkers;
      }

      // sort markers by serie index
      _sortedMarkers = new ArrayList<>(tourMarkers);

      Collections.sort(
            _sortedMarkers,
            (tourMarker1, tourMarker2) -> tourMarker1.getSerieIndex() - tourMarker2.getSerieIndex());

      return _sortedMarkers;
   }

   /**
    * @return Returns the person for which the tour is saved or <code>null</code> when the tour is
    *         not saved in the database.
    */
   public TourPerson getTourPerson() {
      return tourPerson;
   }

   /**
    * @return Returns all {@link TourPhoto}'s which are saved in this tour.
    */
   public Set<TourPhoto> getTourPhotos() {
      return tourPhotos;
   }

   public Collection<TourReference> getTourReferences() {
      return tourReferences;
   }

   /**
    * @return Returns {@link #tourStartPlace} or an empty string when value is not set
    */
   public String getTourStartPlace() {
      return tourStartPlace == null ? UI.EMPTY_STRING : tourStartPlace;
   }

   /**
    * @return Returns the tour start date time with the tour time zone, when not available with the
    *         default time zone.
    */
   @JsonIgnore
   public ZonedDateTime getTourStartTime() {

      if (_zonedStartTime == null) {

         final Instant tourStartMills = Instant.ofEpochMilli(tourStartTime);
         final ZoneId tourStartTimeZoneId = getTimeZoneIdWithDefault();

         final ZonedDateTime zonedStartTime = ZonedDateTime.ofInstant(tourStartMills, tourStartTimeZoneId);

         if (timeZoneId == null) {

            /*
             * Tour has no time zone but this can be changed in the preferences, so this value is
             * not cached
             */

            setCalendarWeek(zonedStartTime);

            return zonedStartTime;

         } else {

            /*
             * Cache this values until the tour zone is modified
             */

            _zonedStartTime = zonedStartTime;

            setCalendarWeek(_zonedStartTime);
         }
      }

      return _zonedStartTime;
   }

   /**
    * @return Returns the tour start time in milliseconds since 1970-01-01T00:00:00Z with the
    *         default time zone.
    */
   public long getTourStartTimeMS() {
      return tourStartTime;
   }

   /**
    * @return Returns the tags {@link #tourTags} which are defined for this tour
    */
   public Set<TourTag> getTourTags() {
      return tourTags;
   }

   /**
    * @return Returns {@link #tourTitle} or an empty string when value is not set
    */
   public String getTourTitle() {
      return tourTitle == null ? UI.EMPTY_STRING : tourTitle;
   }

   /**
    * @return Returns the {@link TourType} for the tour or <code>null</code> when tour type is not
    *         defined
    */
   public TourType getTourType() {
      return tourType;
   }

   public Set<TourWayPoint> getTourWayPoints() {
      return tourWayPoints;
   }

   public float getTraining_TrainingEffect_Aerob() {
      return training_TrainingEffect_Aerob;
   }

   public float getTraining_TrainingEffect_Anaerob() {
      return training_TrainingEffect_Anaerob;
   }

   public float getTraining_TrainingPerformance() {
      return training_TrainingPerformance;
   }

   /**
    * @return Returns weather text or an empty string when weather text is not set.
    */
   public String getWeather() {
      return weather == null ? UI.EMPTY_STRING : weather;
   }

   /**
    * @return {@link #weather_Humidity}
    */
   public int getWeather_Humidity() {
      return weather_Humidity;
   }

   /**
    * @return {@link #weather_Precipitation}
    */
   public float getWeather_Precipitation() {
      return weather_Precipitation;
   }

   /**
    * @return {@link #weather_Pressure}
    */
   public float getWeather_Pressure() {
      return weather_Pressure;
   }

   public float getWeather_Temperature_Max() {
      return weather_Temperature_Max;
   }

   public float getWeather_Temperature_Min() {
      return weather_Temperature_Min;
   }

   public float getWeather_Temperature_WindChill() {
      return weather_Temperature_WindChill;
   }

   /**
    * @return Returns the {@link IWeather#WEATHER_ID_}... or <code>null</code> when weather is not
    *         set.
    */
   public String getWeatherClouds() {
      return weatherClouds;
   }

   /**
    * @return Returns the index for the cloud values in {@link IWeather#cloudIcon} and
    *         {@link IWeather#cloudText} or 0 when the clouds are not defined
    */
   public int getWeatherIndex() {

      int weatherCloudsIndex = -1;

      if (weatherClouds != null) {
         // binary search cannot be done because it requires sorting which we cannot...
         for (int cloudIndex = 0; cloudIndex < IWeather.cloudIcon.length; ++cloudIndex) {
            if (IWeather.cloudIcon[cloudIndex].equalsIgnoreCase(weatherClouds)) {
               weatherCloudsIndex = cloudIndex;
               break;
            }
         }
      }

      return weatherCloudsIndex < 0 ? 0 : weatherCloudsIndex;
   }

   public int getWeatherWindDir() {
      return weatherWindDir;
   }

   public int getWeatherWindSpeed() {
      return weatherWindSpd;
   }

   /**
    * @param zoomLevel
    * @param projectionId
    * @return Returns the world position for the supplied zoom level and projection id
    */
   public Point[] getWorldPositionForTour(final String projectionId, final int zoomLevel) {

      return _tourWorldPosition.get(projectionId.hashCode() + zoomLevel);
   }

   /**
    * @param zoomLevel
    * @param projectionId
    * @return Returns the world position for way points
    */
   public TIntObjectHashMap<Point> getWorldPositionForWayPoints(final String projectionId, final int zoomLevel) {
      return _twpWorldPosition.get(projectionId.hashCode() + zoomLevel);
   }

   /**
    * @return Returns <code>true</code> when the tour has a time zone.
    */
   public boolean hasATimeZone() {
      return timeZoneId != null;
   }

   public boolean hasGeoData() {
      return hasGeoData;
   }

   /**
    * @return Returns <code>true</code> when latitude/longitude data are available
    */
   /**
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {

      int result = 17;

      result = 37 * result + startYear;
      result = 37 * result + startMonth;
      result = 37 * result + startDay;
      result = 37 * result + startHour;
      result = 37 * result + startMinute;
      result = 37 * result + (int) this.getTourDistance();
      result = 37 * result + (int) tourDeviceTime_Elapsed;

      return result;
   }

   /**
    * @return Returns <code>true</code> when cadence of the tour is spm, otherwise it is rpm.
    */
   public boolean isCadenceSpm() {

      return cadenceMultiplier != 1.0;
   }

   /**
    * @return Returns <code>true</code> when {@link TourData} contains reference tours, otherwise
    *         <code>false</code>
    */
   public boolean isContainReferenceTour() {

      if (tourReferences == null) {
         return false;
      } else {
         return tourReferences.size() > 0;
      }
   }

   /**
    * @param dataSerie
    * @return Returns <code>true</code> when the data serie contains at least one value which is > 0
    */
   private boolean isDataSerieWithContent(final int[] dataSerie) {

//      return Arrays.stream(dataSerie).anyMatch(value -> value > 0);

      for (final int dataValue : dataSerie) {
         if (dataValue > 0) {
            return true;
         }
      }

      return false;
   }

   public boolean isDistanceSensorPresent() {
      return isDistanceFromSensor == 1;
   }

   /**
    * @return <code>true</code> when the tour is manually created and not imported from a file or
    *         device
    */
   public boolean isManualTour() {

      if (devicePluginId == null) {
         return false;
      }

      return devicePluginId.equals(DEVICE_ID_FOR_MANUAL_TOUR)
            || devicePluginId.equals(DEVICE_ID_CSV_TOUR_DATA_READER);
   }

   public boolean isMultipleTours() {
      return isMultipleTours;
   }

   /**
    * This is the state of the device which is not related to the availability of power data. Power
    * data should be available but is not checked.
    *
    * @return Returns <code>true</code> when the device has a power sensor
    */
   public boolean isPowerSensorPresent() {
      return isPowerSensorPresent == 1;
   }

   /**
    * @return Returns <code>true</code> when the data in {@link #powerSerie} is from a device and
    *         not computed. Power data are normally available from an ergometer and not from a bike
    *         computer
    */
   public boolean isPowerSerieFromDevice() {
      return isPowerSerieFromDevice;
   }

   /**
    * This is the state of the device which is not related to the availability of pulse data. Pulse
    * data should be available but is not checked.
    *
    * @return Returns <code>true</code> when the device has a pulse sensor
    */
   public boolean isPulseSensorPresent() {
      return isPulseSensorPresent == 1;
   }

   /**
    * @return Returns <code>true</code> when running dynamics data are available
    */
   public boolean isRunDynAvailable() {

      if (runDyn_StanceTime != null && runDyn_StanceTime.length > 0) {
         return true;
      }

      if (runDyn_StanceTimeBalance != null && runDyn_StanceTimeBalance.length > 0) {
         return true;
      }

      if (runDyn_StepLength != null && runDyn_StepLength.length > 0) {
         return true;
      }

      if (runDyn_VerticalOscillation != null && runDyn_VerticalOscillation.length > 0) {
         return true;
      }

      if (runDyn_VerticalRatio != null && runDyn_VerticalRatio.length > 0) {
         return true;
      }

      return false;
   }

   /**
    * @return Returns <code>true</code> when the data in {@link #speedSerie} are from the device and
    *         not computed. Speed data are normally available from an ergometer and not from a bike
    *         computer
    */
   public boolean isSpeedSerieFromDevice() {
      return isSpeedSerieFromDevice;
   }

   /**
    * @return Returns <code>true</code> when SRTM data are available or when they can be available
    *         but not yet computed.
    */
   public boolean isSRTMAvailable() {

      if (latitudeSerie == null) {
         return false;
      }

      if (srtmSerie == null) {
         // srtm data can be available but are not yet computed
         return true;
      }

      if (srtmSerie.length == 0) {
         // SRTM data are invalid
         return false;
      } else {
         return true;
      }
   }

   public boolean isStrideSensorPresent() {
      return isStrideSensorPresent == 1;
   }

   public boolean isSurfing_IsMinDistance() {
      return surfing_IsMinDistance;
   }

   public boolean isTimeSerieWithTimeZoneAdjustment() {

      if (timeSerieWithTimeZoneAdjustment == null) {
         // build time serie with time zone dataserie
         getTimeSerieWithTimeZoneAdjusted();
      }

      return timeSerieWithTimeZoneAdjustment != null;
   }

   /**
    * @return Returns <code>true</code> when the tour is saved in the database.
    */
   public boolean isTourSaved() {
      return tourPerson != null;
   }

   /**
    * Checks if VARCHAR fields have the correct length
    *
    * @return Returns <code>true</code> when the data are valid and can be saved
    */
   public boolean isValidForSave() {

      /*
       * check: tour title
       */
      FIELD_VALIDATION fieldValidation = TourDatabase.isFieldValidForSave(//
            tourTitle,
            DB_LENGTH_TOUR_TITLE,
            Messages.Db_Field_TourData_Title,
            false);

      if (fieldValidation == FIELD_VALIDATION.IS_INVALID) {
         return false;
      } else if (fieldValidation == FIELD_VALIDATION.TRUNCATE) {
         tourTitle = tourTitle.substring(0, DB_LENGTH_TOUR_TITLE);
      }

      /*
       * check: tour description
       */
      fieldValidation = TourDatabase.isFieldValidForSave(
            tourDescription,
            DB_LENGTH_TOUR_DESCRIPTION_V10,
            Messages.Db_Field_TourData_Description,
            false);

      if (fieldValidation == FIELD_VALIDATION.IS_INVALID) {
         return false;
      } else if (fieldValidation == FIELD_VALIDATION.TRUNCATE) {
         tourDescription = tourDescription.substring(0, DB_LENGTH_TOUR_DESCRIPTION_V10);
      }

      /*
       * check: tour start location
       */
      fieldValidation = TourDatabase.isFieldValidForSave(
            tourStartPlace,
            DB_LENGTH_TOUR_START_PLACE,
            Messages.Db_Field_TourData_StartPlace,
            false);

      if (fieldValidation == FIELD_VALIDATION.IS_INVALID) {
         return false;
      } else if (fieldValidation == FIELD_VALIDATION.TRUNCATE) {
         tourStartPlace = tourStartPlace.substring(0, DB_LENGTH_TOUR_START_PLACE);
      }

      /*
       * check: tour end location
       */
      fieldValidation = TourDatabase.isFieldValidForSave(
            tourEndPlace,
            DB_LENGTH_TOUR_END_PLACE,
            Messages.Db_Field_TourData_EndPlace,
            false);

      if (fieldValidation == FIELD_VALIDATION.IS_INVALID) {
         return false;
      } else if (fieldValidation == FIELD_VALIDATION.TRUNCATE) {
         tourEndPlace = tourEndPlace.substring(0, DB_LENGTH_TOUR_END_PLACE);
      }

      /*
       * check: tour import file path
       */
      fieldValidation = TourDatabase.isFieldValidForSave(
            tourImportFilePath,
            DB_LENGTH_TOUR_IMPORT_FILE_PATH,
            Messages.Db_Field_TourData_TourImportFilePath,
            true);

      if (fieldValidation == FIELD_VALIDATION.IS_INVALID) {
         return false;
      } else if (fieldValidation == FIELD_VALIDATION.TRUNCATE) {
         tourImportFilePath = tourImportFilePath.substring(0, DB_LENGTH_TOUR_IMPORT_FILE_PATH);
      }

      /*
       * check: tour import file name
       */
      fieldValidation = TourDatabase.isFieldValidForSave(
            tourImportFileName,
            DB_LENGTH_TOUR_IMPORT_FILE_NAME,
            Messages.Db_Field_TourData_TourImportFilePath,
            true);

      if (fieldValidation == FIELD_VALIDATION.IS_INVALID) {
         return false;
      } else if (fieldValidation == FIELD_VALIDATION.TRUNCATE) {
         tourImportFileName = tourImportFileName.substring(0, DB_LENGTH_TOUR_IMPORT_FILE_NAME);
      }

      /*
       * check: weather
       */
      fieldValidation = TourDatabase.isFieldValidForSave(//
            weather,
            DB_LENGTH_WEATHER,
            Messages.Db_Field_TourData_Weather,
            false);

      if (fieldValidation == FIELD_VALIDATION.IS_INVALID) {
         return false;
      } else if (fieldValidation == FIELD_VALIDATION.TRUNCATE) {
         weather = weather.substring(0, DB_LENGTH_WEATHER);
      }

      return true;
   }

   /**
    * @return Returns <code>true</code> when the {@link SerieData#visiblePoints_Surfing} is saved in
    *         the tour.
    */
   public boolean isVisiblePointsSaved_ForSurfing() {

      // serieData can be null for multiple tours

      return serieData != null && serieData.visiblePoints_Surfing != null;
   }

   public boolean isWeatherDataFromApi() {
      return isWeatherDataFromApi;
   }

   /**
    * Called after the object was loaded from the persistence store
    */
   @PostLoad
   @PostUpdate
   public void onPostLoad() {

      if (TourDatabase.getDbVersionOnStartup() < 20) {
         convertDataSeries();
      }

      onPostLoadGetDataSeries();
   }

   private void onPostLoadGetDataSeries() {

      timeSerie = serieData.timeSerie;

      // manually created tours have currently no time series
      if (timeSerie == null) {
         return;
      }

// SET_FORMATTING_OFF

      altitudeSerie        = serieData.altitudeSerie20;
      cadenceSerie         = serieData.cadenceSerie20;
      distanceSerie        = serieData.distanceSerie20;
      pulseSerie           = serieData.pulseSerie20;
      temperatureSerie     = serieData.temperatureSerie20;
      powerSerie           = serieData.powerSerie20;
      speedSerie           = serieData.speedSerie20;
      pausedTime_Start     = serieData.pausedTime_Start;
      pausedTime_End       = serieData.pausedTime_End;
      pausedTime_Data      = serieData.pausedTime_Data;

      if (serieData.latitude != null) {

         // use existing lat/lon double serie data from older versions
         // -> saving the tour will convert them into E6 format

         latitudeSerie        = serieData.latitude;
         longitudeSerie       = serieData.longitude;

      } else {

         /*
          * Db version >= 43 contain lat/lon in E6 format
          */
         latitudeSerie        = convertDataSeries_FromE6(serieData.latitudeE6);
         longitudeSerie       = convertDataSeries_FromE6(serieData.longitudeE6);
      }
      computeGeo_Grid();

      gearSerie               = serieData.gears;

      pulseTime_Milliseconds  = serieData.pulseTimes;
      pulseTime_TimeIndex     = serieData.pulseTime_TimeIndex;

      if (powerSerie != null) {
         isPowerSerieFromDevice = true;
      }

      if (speedSerie != null) {
         isSpeedSerieFromDevice = true;
      }

      hasGeoData = latitudeSerie != null && latitudeSerie.length > 0;

      // running dynamics
      runDyn_StanceTime          = serieData.runDyn_StanceTime;
      runDyn_StanceTimeBalance   = serieData.runDyn_StanceTimeBalance;
      runDyn_StepLength          = serieData.runDyn_StepLength;
      runDyn_VerticalOscillation = serieData.runDyn_VerticalOscillation;
      runDyn_VerticalRatio       = serieData.runDyn_VerticalRatio;

      // swimming
      swim_LengthType            = serieData.swim_LengthType;
      swim_Cadence               = serieData.swim_Cadence;
      swim_Strokes               = serieData.swim_Strokes;
      swim_StrokeStyle           = serieData.swim_StrokeStyle;
      swim_Time                  = serieData.swim_Time;
      isSwimCadence              = swim_Cadence != null;

      // currently only surfing data can be made visible/hidden
      visibleDataPointSerie      = serieData.visiblePoints_Surfing;

      // battery
      battery_Percentage         = serieData.battery_Percentage;
      battery_Time               = serieData.battery_Time;

// SET_FORMATTING_ON
   }

   /**
    * Called before this object gets persisted, copy data from the tourdata object into the object
    * which gets serialized
    */
   /*
    * @PrePersist + @PreUpdate is currently disabled for EJB events because of bug
    * http://opensource.atlassian.com/projects/hibernate/browse/HHH-1921 2006-08-11
    */
   public void onPrePersist() {

      /*
       * Create new data series
       */
      serieData = new SerieData();

// SET_FORMATTING_OFF

      serieData.timeSerie           = timeSerie;
      serieData.altitudeSerie20     = altitudeSerie;
      serieData.cadenceSerie20      = cadenceSerie;
      serieData.distanceSerie20     = distanceSerie;
      serieData.pulseSerie20        = pulseSerie;
      serieData.temperatureSerie20  = temperatureSerie;
      serieData.pausedTime_Start    = pausedTime_Start;
      serieData.pausedTime_End      = pausedTime_End;
      serieData.pausedTime_Data     = pausedTime_Data;

      /*
       * don't save computed data series
       */
      if (isSpeedSerieFromDevice) {
         serieData.speedSerie20 = speedSerie;
      }

      if (isPowerSerieFromDevice) {
         serieData.powerSerie20 = powerSerie;
      }

      serieData.latitudeE6          = convertDataSeries_ToE6(latitudeSerie);
      serieData.longitudeE6         = convertDataSeries_ToE6(longitudeSerie);

      serieData.gears               = gearSerie;

      serieData.pulseTimes          = pulseTime_Milliseconds;
      serieData.pulseTime_TimeIndex = pulseTime_TimeIndex;

      // running dynamics
      serieData.runDyn_StanceTime            = runDyn_StanceTime;
      serieData.runDyn_StanceTimeBalance     = runDyn_StanceTimeBalance;
      serieData.runDyn_StepLength            = runDyn_StepLength;
      serieData.runDyn_VerticalRatio         = runDyn_VerticalRatio;
      serieData.runDyn_VerticalOscillation   = runDyn_VerticalOscillation;

      // swimming
      serieData.swim_LengthType              = swim_LengthType;
      serieData.swim_Cadence                 = swim_Cadence;
      serieData.swim_Strokes                 = swim_Strokes;
      serieData.swim_StrokeStyle             = swim_StrokeStyle;
      serieData.swim_Time                    = swim_Time;

      if (isSwimCadence) {
         // cadence is computed from cadence swim data
         serieData.cadenceSerie20 = null;
      }

      // surfing
      serieData.visiblePoints_Surfing        = visiblePoints_ForSurfing;

      // battery
      serieData.battery_Percentage           = battery_Percentage;
      serieData.battery_Time                 = battery_Time;

// SET_FORMATTING_ON

      // time serie size
      numberOfTimeSlices = timeSerie == null ? 0 : timeSerie.length;
   }

   /**
    * Remove photos from this tour and save it.
    *
    * @param allRemovedTourPhotos
    */
   public void removePhotos(final Collection<TourPhoto> allRemovedTourPhotos) {

      if (allRemovedTourPhotos.size() > 0) {

         final HashSet<TourPhoto> currentTourPhotos = new HashSet<>(tourPhotos);

         currentTourPhotos.removeAll(allRemovedTourPhotos);

         saveTourPhotos(currentTourPhotos);
      }
   }

   public boolean replaceAltitudeWithSRTM() {

      if (getSRTMSerie() == null) {
         return false;
      }

      altitudeSerie = Arrays.copyOf(srtmSerie, srtmSerie.length);
      altitudeSerieImperial = Arrays.copyOf(srtmSerieImperial, srtmSerieImperial.length);

      altitudeSerieSmoothed = null;
      altitudeSerieImperialSmoothed = null;

      // adjust computed altitude values
      computeAltitudeUpDown();
      computeMaxAltitude();

      return true;
   }

   /**
    * Reset sorted markers that they are sorted again.
    */
   private void resetSortedMarkers() {

      if (_sortedMarkers != null) {
         _sortedMarkers.clear();
         _sortedMarkers = null;
      }
   }

   /**
    * Set {@link #visibleDataPointSerie} to it's saved values.
    */
   public void restoreVisiblePoints_ForSurfing() {

      if (serieData != null) {
         visibleDataPointSerie = serieData.visiblePoints_Surfing;
      }
   }

   /**
    * Save photos in this tour.
    *
    * @param allTourPhotos
    */
   private void saveTourPhotos(final HashSet<TourPhoto> allTourPhotos) {

      // force gallery photos to be recreated
      _galleryPhotos.clear();

      tourPhotos.clear();
      tourPhotos.addAll(allTourPhotos);

      numberOfPhotos = tourPhotos.size();

      computePhotoTimeAdjustment();

      TourManager.saveModifiedTour(this, true);
   }

   /**
    * @param avgCadence
    *           the avgCadence to set
    */
   public void setAvgCadence(final float avgCadence) {
      this.avgCadence = avgCadence;
   }

   /**
    * @param avgPulse
    *           the avgPulse to set
    */
   public void setAvgPulse(final float avgPulse) {
      this.avgPulse = avgPulse;
   }

   /**
    * @param avgTemperature
    *           the avgTemperature to set
    */
   public void setAvgTemperature(final float avgTemperature) {
      this.avgTemperature = avgTemperature;
   }

   public void setBattery_Percentage(final short[] battery_Percentage) {
      this.battery_Percentage = battery_Percentage;
   }

   public void setBattery_Percentage_End(final short battery_Percentage_End) {
      this.battery_Percentage_End = battery_Percentage_End;
   }

   public void setBattery_Percentage_Start(final short battery_Percentage_Start) {
      this.battery_Percentage_Start = battery_Percentage_Start;
   }

   public void setBattery_Time(final int[] battery_Time) {
      this.battery_Time = battery_Time;
   }

   /**
    * @param bodyFat
    *           Sets the body fat.
    */
   public void setBodyFat(final float bodyFat) {
      this.bodyFat = bodyFat;
   }

   /**
    * @param bodyWeight
    *           Sets the body weight in kilograms.
    */
   public void setBodyWeight(final float bodyWeight) {
      this.bodyWeight = bodyWeight;
   }

   public void setBreakTimeSerie(final boolean[] breakTimeSerie) {
      this.breakTimeSerie = breakTimeSerie;
   }

   public void setCadenceMultiplier(final float cadenceMultiplier) {
      this.cadenceMultiplier = cadenceMultiplier;
   }

   public void setCadenceSerie(final float[] cadenceSerieData) {
      cadenceSerie = cadenceSerieData;
   }

   public void setCadenceZone_FastTime(final int cadenceZone_FastTime) {
      this.cadenceZone_FastTime = cadenceZone_FastTime;
   }

   public void setCadenceZone_SlowTime(final int cadenceZone_SlowTime) {
      this.cadenceZone_SlowTime = cadenceZone_SlowTime;
   }

   /**
    * Set the calendar week in the tour.
    *
    * @param dateTime
    */
   private void setCalendarWeek(final ZonedDateTime dateTime) {

      final WeekFields cw = TimeTools.calendarWeek;

      startWeek = (short) dateTime.get(cw.weekOfWeekBasedYear());
      startWeekYear = (short) dateTime.get(cw.weekBasedYear());
   }

   /**
    * @param calories
    *           the calories to set
    */
   public void setCalories(final int calories) {
      this.calories = calories;
   }

   public void setConconiDeflection(final int conconiDeflection) {
      this.conconiDeflection = conconiDeflection;
   }

   public void setDateTimeCreated(final long dateTimeCreated) {
      this.dateTimeCreated = dateTimeCreated;
   }

   public void setDateTimeModified(final long dateTimeModified) {
      this.dateTimeModified = dateTimeModified;
   }

   public void setDeviceFirmwareVersion(final String deviceFirmwareVersion) {
      this.deviceFirmwareVersion = deviceFirmwareVersion;
   }

   public void setDeviceId(final String deviceId) {
      this.devicePluginId = deviceId;
   }

   public void setDeviceMode(final short deviceMode) {
      this.deviceMode = deviceMode;
   }

   public void setDeviceModeName(final String deviceModeName) {
      this.deviceModeName = deviceModeName;
   }

   /**
    * Set {@link #devicePluginName}
    *
    * @param deviceName
    */
   public void setDeviceName(final String deviceName) {
      devicePluginName = deviceName;
   }

   /**
    * Time difference in seconds between 2 time slices when the interval is constant for the whole
    * tour or <code>-1</code> for GPS devices or ergometer when the time slice duration are not
    * equally
    *
    * @param deviceTimeInterval
    */
   public void setDeviceTimeInterval(final short deviceTimeInterval) {
      this.deviceTimeInterval = deviceTimeInterval;
   }

   public void setDeviceTourType(final String tourType) {
      this.deviceTourType = tourType;
   }

   public void setDpTolerance(final short dpTolerance) {
      this.dpTolerance = dpTolerance;
   }

   public void setFrontShiftCount(final int frontShiftCount) {
      this.frontShiftCount = frontShiftCount;
   }

   public void setGears(final List<GearData> gears) {

      final int gearSize = gears.size();

      if (gearSize == 0) {
         return;
      }

      // convert gear data into a gearSerie

      final long[] gearSerie = new long[timeSerie.length];

      int gearIndex = 0;
      final int nextGearIndex = gearSize > 0 ? 1 : 0;

      GearData currentGear = gears.get(0);
      GearData nextGear = gears.get(nextGearIndex);

      long nextGearTime;
      if (gearIndex >= nextGearIndex) {
         // there are no further gears
         nextGearTime = Long.MAX_VALUE;
      } else {
         nextGearTime = nextGear.absoluteTime;
      }

      int frontShiftCount = 0;
      int rearShiftCount = 0;
      int currentFrontGear = currentGear.getFrontGearTeeth();
      int currentRearGear = currentGear.getRearGearTeeth();

      for (int timeIndex = 0; timeIndex < gearSerie.length; timeIndex++) {

         final long currentTime = tourStartTime + timeSerie[timeIndex] * 1000;

         if (currentTime >= nextGearTime) {

            // advance to the next gear

            gearIndex++;

            if (gearIndex < gearSize - 1) {

               // next gear is available

               currentGear = nextGear;

               nextGear = gears.get(gearIndex);
               nextGearTime = nextGear.absoluteTime;

               final int nextFrontGear = nextGear.getFrontGearTeeth();
               final int nextRearGear = nextGear.getRearGearTeeth();

               if (currentFrontGear != nextFrontGear) {

                  frontShiftCount++;
               }

               if (currentRearGear != nextRearGear) {

                  rearShiftCount++;
               }

               currentFrontGear = nextFrontGear;
               currentRearGear = nextRearGear;

            } else {

               // there are no further gears

               nextGearTime = Long.MAX_VALUE;

               currentGear = nextGear;
            }
         }

         gearSerie[timeIndex] = currentGear.gears;
      }

      this.gearSerie = gearSerie;

      this.frontShiftCount = frontShiftCount;
      this.rearShiftCount = rearShiftCount;
   }

   public void setGears(final long[] gearSerieData) {

      if (gearSerieData.length < 1) {
         return;
      }

      int frontShifts = 0;
      int rearShifts = 0;

      int currentFrontGear = (int) (gearSerieData[0] >> 24 & 0xff);
      int currentRearGear = (int) (gearSerieData[0] >> 8 & 0xff);

      for (final long gear : gearSerieData) {

         final int nextFrontGear = (int) (gear >> 24 & 0xff);
         final int nextRearGear = (int) (gear >> 8 & 0xff);

         if (currentFrontGear != nextFrontGear) {
            frontShifts++;
         }

         if (currentRearGear != nextRearGear) {
            rearShifts++;
         }

         currentFrontGear = nextFrontGear;
         currentRearGear = nextRearGear;
      }

      this.gearSerie = gearSerieData;

      this.frontShiftCount = frontShifts;
      this.rearShiftCount = rearShifts;
   }

   public void setHasGeoData(final boolean hasGeoData) {
      this.hasGeoData = hasGeoData;
   }

   /**
    * Set only the import folder.
    *
    * @param backupOSFolder
    */
   public void setImportBackupFileFolder(final String backupOSFolder) {

      // keep original which is used when this file and the backup file should be deleted
      this.importFilePathOriginal = tourImportFilePath;

      // overwrite import file path with the backup folder
      this.tourImportFilePath = backupOSFolder;
   }

   /**
    * Sets the file path (folder + file name) for the imported file, this is displayed in the
    * {@link TourDataEditorView}
    *
    * @param tourImportFilePath
    */
   public void setImportFilePath(final String tourImportFilePath) {

      try {

         final Path filePath = Paths.get(tourImportFilePath);

         final Path fileName = filePath.getFileName();
         final Path folderPath = filePath.getParent();

         // extract file name
         this.tourImportFileName = fileName == null ? UI.EMPTY_STRING : fileName.toString();
         this.tourImportFilePath = folderPath == null ? UI.EMPTY_STRING : folderPath.toString();

      } catch (final Exception e) {
         // folder can be invalid
      }
   }

   /**
    * Set state if the distance is from a sensor or not, default is <code>false</code>
    *
    * @param isFromSensor
    */
   public void setIsDistanceFromSensor(final boolean isFromSensor) {
      this.isDistanceFromSensor = (short) (isFromSensor ? 1 : 0);
   }

   public void setIsImportedMTTour(final boolean isImportedMTTour) {
      _isImportedMTTour = isImportedMTTour;
   }

   public void setIsPowerSensorPresent(final boolean isFromSensor) {
      this.isPowerSensorPresent = (short) (isFromSensor ? 1 : 0);
   }

   public void setIsPulseSensorPresent(final boolean isFromSensor) {
      this.isPulseSensorPresent = (short) (isFromSensor ? 1 : 0);
   }

   public void setIsStrideSensorPresent(final boolean isFromSensor) {

      this.isStrideSensorPresent = (short) (isFromSensor ? 1 : 0);

      if (isFromSensor) {
         cadenceMultiplier = 2.0f;
      }
   }

   public void setIsWeatherDataFromApi(final boolean isWeatherDataFromApi) {
      this.isWeatherDataFromApi = isWeatherDataFromApi;
   }

   public void setMaxPulse(final float maxPulse) {
      this.maxPulse = maxPulse;
   }

   public void setMergedAltitudeOffset(final int altitudeDiff) {
      mergedAltitudeOffset = altitudeDiff;
   }

   public void setMergedTourTimeOffset(final int mergedTourTimeOffset) {
      this.mergedTourTimeOffset = mergedTourTimeOffset;
   }

   public void setMergeSourceTour(final TourData mergeSourceTour) {
      _mergeSourceTourData = mergeSourceTour;
   }

   public void setMergeSourceTourId(final Long mergeSourceTourId) {
      this.mergeSourceTourId = mergeSourceTourId;
   }

   public void setMergeTargetTourId(final Long mergeTargetTourId) {
      this.mergeTargetTourId = mergeTargetTourId;
   }

   public void setPausedTime_Data(final long[] pausedTime_Data) {
      this.pausedTime_Data = pausedTime_Data;
   }

   public void setPausedTime_End(final long[] pausedTime_End) {
      this.pausedTime_End = pausedTime_End;
   }

   public void setPausedTime_Start(final long[] pausedTime_Start) {
      this.pausedTime_Start = pausedTime_Start;
   }

   public void setPower_Avg(final float avgPower) {
      this.power_Avg = avgPower;
   }

   public void setPower_AvgLeftPedalSmoothness(final float avgLeftPedalSmoothness) {
      this.power_AvgLeftPedalSmoothness = avgLeftPedalSmoothness;
   }

   public void setPower_AvgLeftTorqueEffectiveness(final float avgLeftTorqueEffectiveness) {
      this.power_AvgLeftTorqueEffectiveness = avgLeftTorqueEffectiveness;
   }

   public void setPower_AvgRightPedalSmoothness(final float avgRightPedalSmoothness) {
      this.power_AvgRightPedalSmoothness = avgRightPedalSmoothness;
   }

   public void setPower_AvgRightTorqueEffectiveness(final float avgRightTorqueEffectiveness) {
      this.power_AvgRightTorqueEffectiveness = avgRightTorqueEffectiveness;
   }

   public void setPower_DataSource(final String power_DataSource) {
      this.power_DataSource = power_DataSource;
   }

   /**
    * Sets Functional Threshold Power (FTP)
    *
    * @param ftp
    */
   public void setPower_FTP(final int ftp) {
      this.power_FTP = ftp;
   }

   public void setPower_IntensityFactor(final float intensityFactor) {
      this.power_IntensityFactor = intensityFactor;
   }

   public void setPower_Max(final int maxPower) {
      this.power_Max = maxPower;
   }

   public void setPower_Normalized(final int normalizedPower) {
      this.power_Normalized = normalizedPower;
   }

   public void setPower_PedalLeftRightBalance(final int leftRightBalance) {
      this.power_PedalLeftRightBalance = leftRightBalance;
   }

   public void setPower_TotalWork(final long totalWork) {
      this.power_TotalWork = totalWork;
   }

   public void setPower_TrainingStressScore(final float trainingStressScore) {
      this.power_TrainingStressScore = trainingStressScore;
   }

   /**
    * Sets the power data serie and set's a flag that the data serie is from a device
    *
    * @param powerSerie
    */
   public void setPowerSerie(final float[] powerSerie) {
      this.powerSerie = powerSerie;
      this.isPowerSerieFromDevice = true;
   }

   public void setRearShiftCount(final int rearShiftCount) {
      this.rearShiftCount = rearShiftCount;
   }

   public void setRestPulse(final int restPulse) {
      this.restPulse = restPulse;
   }

   private void setSpeed(final int serieIndex,
                         final float speed_Metric,
                         final float speed_Mile,
                         final float speed_NauticalMile) {

      speedSerie[serieIndex] = speed_Metric;
      speedSerie_Mile[serieIndex] = speed_Mile;
      speedSerie_NauticalMile[serieIndex] = speed_NauticalMile;

      maxSpeed = Math.max(maxSpeed, speed_Metric);

      final float paceMetricSeconds = speed_Metric < 1.0 ? 0 : (float) (3600.0 / speed_Metric);
      final float paceImperialSeconds = speed_Metric < 0.6 ? 0 : (float) (3600.0 / speed_Mile);

      //Convert the max speed to max pace
      maxPace = maxSpeed < 1.0 ? 0 : (float) (3600.0 / maxSpeed);

      paceSerie_Seconds[serieIndex] = paceMetricSeconds;
      paceSerie_Seconds_Imperial[serieIndex] = paceImperialSeconds;

      paceSerie_Minute[serieIndex] = paceMetricSeconds / 60;
      paceSerie_Minute_Imperial[serieIndex] = paceImperialSeconds / 60;
   }

   /**
    * Sets the speed data serie and set's a flag that the data serie is from a device
    *
    * @param speedSerie
    */
   public void setSpeedSerie(final float[] speedSerie) {
      this.speedSerie = speedSerie;
      this.isSpeedSerieFromDevice = speedSerie != null;
   }

   public void setSRTMValues(final float[] srtm, final float[] srtmImperial) {
      srtmSerie = srtm;
      srtmSerieImperial = srtmImperial;
   }

   public void setStartAltitude(final short startAltitude) {
      this.startAltitude = startAltitude;
   }

   /**
    * Odometer value, this is the distance which the device is accumulating
    *
    * @param startDistance
    */
   public void setStartDistance(final float startDistance) {
      this.startDistance = startDistance;
   }

   public void setStartPulse(final short startPulse) {
      this.startPulse = startPulse;
   }

   public void setSurfing_IsMinDistance(final boolean surfing_IsMinDistance) {
      this.surfing_IsMinDistance = surfing_IsMinDistance;
   }

   public void setSurfing_MinDistance(final short surfing_MinDistance) {
      this.surfing_MinDistance = surfing_MinDistance;
   }

   public void setSurfing_MinSpeed_StartStop(final short surfing_MinSpeed_StartStop) {
      this.surfing_MinSpeed_StartStop = surfing_MinSpeed_StartStop;
   }

   public void setSurfing_MinSpeed_Surfing(final short surfing_MinSpeed_Surfing) {
      this.surfing_MinSpeed_Surfing = surfing_MinSpeed_Surfing;
   }

   public void setSurfing_MinTimeDuration(final short surfing_MinTimeDuration) {
      this.surfing_MinTimeDuration = surfing_MinTimeDuration;
   }

   public void setSurfing_NumberOfEvents(final short surfing_NumberOfEvents) {
      this.surfing_NumberOfEvents = surfing_NumberOfEvents;
   }

   public void setTimeSerieDouble(final double[] timeSerieDouble) {
      this.timeSerieDouble = timeSerieDouble;
   }

   public void setTimeZoneId(final String timeZoneId) {

      this.timeZoneId = timeZoneId;

      // reset cached date time with time zone to recognize the new time zone
      _zonedStartTime = null;
   }

   public void setTourAltDown(final float tourAltDown) {
      this.tourAltDown = (int) (tourAltDown + 0.5);

      // We update the average elevation change
      // Note : We only do it here since most of the call to the function
      // setTourAltDown() is performed AFTER setTourAltUp()
      // Hence, we know that at this point, we will be able to compute the
      // average elevation change with the latest values of tourAltUp and tourAltDown.
      computeAvg_AltitudeChange();
   }

   public void setTourAltUp(final float tourAltUp) {
      this.tourAltUp = (int) (tourAltUp + 0.5);
   }

   public void setTourBike(final TourBike tourBike) {
      this.tourBike = tourBike;
   }

   /**
    * Set total moving time in seconds.
    *
    * @param tourComputedTime_Moving
    */
   public void setTourComputedTime_Moving(final int tourComputedTime_Moving) {
      this.tourComputedTime_Moving = tourComputedTime_Moving;
   }

   /**
    * @param tourDescription
    *           the tourDescription to set
    */
   public void setTourDescription(final String tourDescription) {
      this.tourDescription = tourDescription;
   }

   /**
    * Set total elapsed time in seconds
    *
    * @param tourElapsedTime
    */
   public void setTourDeviceTime_Elapsed(final long tourDeviceTime_Elapsed) {

      this.tourDeviceTime_Elapsed = tourDeviceTime_Elapsed;

      setTourEndTimeMS();
   }

   /**
    * Set total paused time in seconds
    *
    * @param tourDeviceTime_Paused
    */
   public void setTourDeviceTime_Paused(final long tourDeviceTime_Paused) {

      this.tourDeviceTime_Paused = tourDeviceTime_Paused;
   }

   public void setTourDeviceTime_Recorded(final long tourDeviceTime_Recorded) {
      this.tourDeviceTime_Recorded = tourDeviceTime_Recorded;
   }

   public void setTourDistance(final float tourDistance) {
      this.tourDistance = tourDistance;
   }

   /**
    * @param tourEndPlace
    *           the tourEndPlace to set
    */
   public void setTourEndPlace(final String tourEndPlace) {
      this.tourEndPlace = tourEndPlace;
   }

   private void setTourEndTimeMS() {

      tourEndTime = tourStartTime + (tourDeviceTime_Elapsed * 1000);
   }

   public void setTourMarkers(final Set<TourMarker> tourMarkers) {

      if (this.tourMarkers != null) {
         this.tourMarkers.clear();
      }

      this.tourMarkers = tourMarkers;

      resetSortedMarkers();
   }

   /**
    * Sets the {@link TourPerson} for the tour or <code>null</code> when the tour is not saved in
    * the database
    *
    * @param tourPerson
    */
   public void setTourPerson(final TourPerson tourPerson) {
      this.tourPerson = tourPerson;
   }

   /**
    * Set new photos into the tour, existing photos will be replaced.
    *
    * @param newTourPhotos
    * @param newGalleryPhotos
    */
   public void setTourPhotos(final HashSet<TourPhoto> newTourPhotos, final ArrayList<Photo> newGalleryPhotos) {

      /*
       * reset state for photos which are not saved any more in this tour
       */
      final ArrayList<Photo> oldGalleryPhotos = getGalleryPhotos();

      if (oldGalleryPhotos != null) {

         for (final Photo oldGalleryPhoto : oldGalleryPhotos) {

            final String oldImageFilePathName = oldGalleryPhoto.imageFilePathName;
            boolean isPhotoUsed = false;

            for (final Photo newGalleryPhoto : newGalleryPhotos) {

               if (oldImageFilePathName.equals(newGalleryPhoto.imageFilePathName)) {
                  isPhotoUsed = true;
                  break;
               }
            }

            if (isPhotoUsed == false) {

               /*
                * Photo is not saved any more in this tour, remove tour reference
                */
               oldGalleryPhoto.removeTour(tourId);
            }
         }
      }

      // force photos to be recreated
      _galleryPhotos.clear();

      tourPhotos.clear();
      tourPhotos.addAll(newTourPhotos);

      numberOfPhotos = tourPhotos.size();

      computePhotoTimeAdjustment();
   }

   /**
    * @param tourStartPlace
    *           the tourStartPlace to set
    */
   public void setTourStartPlace(final String tourStartPlace) {
      this.tourStartPlace = tourStartPlace;
   }

   /**
    * Set tour start date/time and week.
    *
    * @param tourStartYear
    * @param tourStartMonth
    *           1...12
    * @param tourStartDay
    * @param tourStartHour
    * @param tourStartMinute
    * @param tourStartSecond
    */
   public void setTourStartTime(final int tourStartYear,
                                final int tourStartMonth,
                                final int tourStartDay,
                                final int tourStartHour,
                                final int tourStartMinute,
                                final int tourStartSecond) {

      final ZonedDateTime zonedStartTime = ZonedDateTime.of(
            tourStartYear,
            tourStartMonth,
            tourStartDay,
            tourStartHour,
            tourStartMinute,
            tourStartSecond,
            0,
            TimeTools.getDefaultTimeZone());

      tourStartTime = zonedStartTime.toInstant().toEpochMilli();

      if (tourStartMonth < 1 || tourStartMonth > 12) {
         StatusUtil.log(new Exception("Month is invalid: " + tourStartMonth)); //$NON-NLS-1$
         startMonth = 1;
      } else {
         startMonth = (short) tourStartMonth;
      }

      startYear = (short) tourStartYear;
//      startMonth = tourStartMonth;
      startDay = (short) tourStartDay;
      startHour = (short) tourStartHour;
      startMinute = (short) tourStartMinute;
      startSecond = tourStartSecond;

      setCalendarWeek(zonedStartTime);

      // cache zoned date time
      _zonedStartTime = zonedStartTime;
   }

   public void setTourStartTime(final ZonedDateTime zonedStartTime) {

      final long newZonedStartTime = zonedStartTime.toInstant().toEpochMilli();
      if (tourStartTime != 0) {
         updatePausedTimes(newZonedStartTime - tourStartTime);
      }

      // set the start of the tour

      tourStartTime = newZonedStartTime;

      startYear = (short) zonedStartTime.getYear();
      startMonth = (short) zonedStartTime.getMonthValue();
      startDay = (short) zonedStartTime.getDayOfMonth();
      startHour = (short) zonedStartTime.getHour();
      startMinute = (short) zonedStartTime.getMinute();
      startSecond = zonedStartTime.getSecond();

      setCalendarWeek(zonedStartTime);

      // cache zoned date time
      _zonedStartTime = zonedStartTime;
   }

   /**
    * Set tour start year/month/day which are used in views, e.g. tourbook view, they are
    * accessed with SQL statements.
    *
    * @param tourStartTime
    */
   public void setTourStartTime_YYMMDD(final ZonedDateTime tourStartTime) {

      startYear = (short) tourStartTime.getYear();
      startMonth = (short) tourStartTime.getMonthValue();
      startDay = (short) tourStartTime.getDayOfMonth();
      startHour = (short) tourStartTime.getHour();
      startMinute = (short) tourStartTime.getMinute();
      startSecond = tourStartTime.getSecond();
   }

   public void setTourTags(final Set<TourTag> tourTags) {
      this.tourTags = tourTags;
   }

   /**
    * @param tourTitle
    *           the tourTitle to set
    */
   public void setTourTitle(final String tourTitle) {
      this.tourTitle = tourTitle;
   }

   public void setTourType(final TourType tourType) {
      this.tourType = tourType;
   }

   public void setTraining_TrainingEffect_Aerob(final float trainingEffect) {
      this.training_TrainingEffect_Aerob = trainingEffect;
   }

   public void setTraining_TrainingEffect_Anaerob(final float trainingEffect_Anaerobic) {
      this.training_TrainingEffect_Anaerob = trainingEffect_Anaerobic;
   }

   public void setTraining_TrainingPerformance(final float trainingPerformance) {
      this.training_TrainingPerformance = trainingPerformance;
   }

   public void setupHistoryTour() {

      // each tourData requires a tour id to identify it in equals();
      tourId = System.nanoTime();

      isHistoryTour = true;
   }

   public void setupMultipleTour() {

      // each tourData requires a tour id to identify it in equals();
      tourId = System.nanoTime();

      isMultipleTours = true;
   }

   /**
    * Search for first valid value and fill up the data serie until the first valid value is
    * reached.
    * <p>
    *
    * @param timeDataSerie
    * @param isAbsoluteData
    * @return Returns <code>true</code> when values are available in the data serie and
    *         {@link #altitudeSerie} has valid start values.
    */
   private boolean setupStartingValues_Altitude(final TimeData[] timeDataSerie, final boolean isAbsoluteData) {

      final TimeData firstTimeData = timeDataSerie[0];
      final int serieSize = timeDataSerie.length;

      boolean isAvailable = false;

      if (isAbsoluteData) {

         if (firstTimeData.absoluteAltitude == Float.MIN_VALUE) {

            for (int timeDataIndex = 0; timeDataIndex < serieSize; timeDataIndex++) {

               final TimeData timeData = timeDataSerie[timeDataIndex];
               if (timeData.absoluteAltitude != Float.MIN_VALUE) {

                  // valid value is available

                  altitudeSerie = new float[serieSize];
                  isAvailable = true;

                  // update values to the first valid value

                  final float firstValidValue = timeData.absoluteAltitude;

                  for (int invalidIndex = 0; invalidIndex < timeDataIndex; invalidIndex++) {
                     timeDataSerie[invalidIndex].absoluteAltitude = firstValidValue;
                  }

                  break;
               }
            }

         } else {

            // altitude is available

            altitudeSerie = new float[serieSize];
            isAvailable = true;
         }

      } else if (firstTimeData.altitude != Float.MIN_VALUE) {

         // altitude is available

         altitudeSerie = new float[serieSize];
         isAvailable = true;
      }

      return isAvailable;
   }

   private boolean setupStartingValues_Cadence(final TimeData[] timeDataSerie) {

      final TimeData firstTimeData = timeDataSerie[0];
      final int serieSize = timeDataSerie.length;

      boolean isAvailable = false;

      if (firstTimeData.cadence == Float.MIN_VALUE) {

         // search for first cadence value

         for (int timeDataIndex = 0; timeDataIndex < serieSize; timeDataIndex++) {

            final TimeData timeData = timeDataSerie[timeDataIndex];
            if (timeData.cadence != Float.MIN_VALUE) {

               // cadence is available, starting values are set to 0

               cadenceSerie = new float[serieSize];
               isAvailable = true;

               for (int invalidIndex = 0; invalidIndex < timeDataIndex; invalidIndex++) {
                  timeDataSerie[invalidIndex].cadence = 0;
               }
               break;
            }
         }

      } else {

         // cadence is available

         cadenceSerie = new float[serieSize];
         isAvailable = true;
      }

      return isAvailable;
   }

   private boolean setupStartingValues_Distance(final TimeData[] timeDataSerie, final boolean isAbsoluteData) {

      final TimeData firstTimeData = timeDataSerie[0];
      final int serieSize = timeDataSerie.length;

      boolean isAvailable = false;

      if ((firstTimeData.distance != Float.MIN_VALUE) || isAbsoluteData) {
         distanceSerie = new float[serieSize];
         isAvailable = true;
      }

      return isAvailable;
   }

   private boolean setupStartingValues_Gear(final TimeData[] timeDataSerie) {

      final TimeData firstTimeData = timeDataSerie[0];
      final int serieSize = timeDataSerie.length;

      boolean isAvailable = false;

      if (firstTimeData.gear == 0) {

         // search for first gear value

         for (int timeDataIndex = 0; timeDataIndex < serieSize; timeDataIndex++) {

            final TimeData timeData = timeDataSerie[timeDataIndex];
            final long gearValue = timeData.gear;

            if (gearValue != 0) {

               // gear is available, starting values are set to first valid gear value

               gearSerie = new long[serieSize];
               isAvailable = true;

               for (int invalidIndex = 0; invalidIndex < timeDataIndex; invalidIndex++) {
                  timeDataSerie[invalidIndex].gear = gearValue;
               }
               break;
            }
         }

      } else {

         // gear is available

         gearSerie = new long[serieSize];
         isAvailable = true;
      }

      return isAvailable;
   }

   private boolean setupStartingValues_LatLon(final TimeData[] timeDataSerie) {

      final int serieSize = timeDataSerie.length;
      boolean isGPS = false;

      for (int timeDataIndex = 0; timeDataIndex < serieSize; timeDataIndex++) {

         final TimeData timeData = timeDataSerie[timeDataIndex];

         if (timeData.latitude != Double.MIN_VALUE) {

            isGPS = true;

            final double firstValidLatitude = timeData.latitude;
            final double firstValidLongitude = timeData.longitude;

            latitudeSerie = new double[serieSize];
            longitudeSerie = new double[serieSize];

            // fill beginning of lat/lon data series with first valid values

            for (int invalidIndex = 0; invalidIndex < timeDataIndex; invalidIndex++) {
               timeDataSerie[invalidIndex].latitude = firstValidLatitude;
               timeDataSerie[invalidIndex].longitude = firstValidLongitude;
            }

            break;
         }
      }

      return isGPS;
   }

   private boolean setupStartingValues_Power(final TimeData[] timeDataSerie) {

      final TimeData firstTimeData = timeDataSerie[0];
      final int serieSize = timeDataSerie.length;

      boolean isAvailable = false;

      if (firstTimeData.power == Float.MIN_VALUE) {

         for (int timeDataIndex = 0; timeDataIndex < serieSize; timeDataIndex++) {

            final TimeData timeData = timeDataSerie[timeDataIndex];
            final float power = timeData.power;

            if (power != Float.MIN_VALUE) {

               // power values are available, starting values are set to 0

               powerSerie = new float[serieSize];
               isAvailable = true;

               // update values to 0
               for (int invalidIndex = 0; invalidIndex < timeDataIndex; invalidIndex++) {
                  timeDataSerie[invalidIndex].power = 0;
               }

               break;
            }
         }

      } else {

         // power values are available

         powerSerie = new float[serieSize];
         isAvailable = true;
      }

      isPowerSerieFromDevice = isAvailable;

      return isAvailable;
   }

   private boolean setupStartingValues_Pulse(final TimeData[] timeDataSerie) {

      final TimeData firstTimeData = timeDataSerie[0];
      final int serieSize = timeDataSerie.length;

      boolean isAvailable = false;

      if (firstTimeData.pulse == Float.MIN_VALUE) {

         for (int timeDataIndex = 0; timeDataIndex < serieSize; timeDataIndex++) {

            final TimeData timeData = timeDataSerie[timeDataIndex];
            final float pulse = timeData.pulse;

            if (pulse > 0) {

               // pulse values are available, starting values are set to the first valid value

               pulseSerie = new float[serieSize];
               isAvailable = true;

               for (int invalidIndex = 0; invalidIndex < timeDataIndex; invalidIndex++) {
                  timeDataSerie[invalidIndex].pulse = pulse;
               }

               break;
            }
         }

      } else {

         // pulse values are available

         pulseSerie = new float[serieSize];
         isAvailable = true;
      }

      return isAvailable;
   }

   private boolean setupStartingValues_RunDyn_StanceTime(final TimeData[] timeDataSerie) {

      final TimeData firstTimeData = timeDataSerie[0];
      final int serieSize = timeDataSerie.length;

      boolean isAvailable = false;

      if (firstTimeData.runDyn_StanceTime == Short.MIN_VALUE) {

         // search for first valid value

         for (int timeDataIndex = 0; timeDataIndex < serieSize; timeDataIndex++) {

            final short value = timeDataSerie[timeDataIndex].runDyn_StanceTime;

            if (value != Short.MIN_VALUE) {

               // data are available, starting values are set to first valid value

               runDyn_StanceTime = new short[serieSize];
               isAvailable = true;

               for (int invalidIndex = 0; invalidIndex < timeDataIndex; invalidIndex++) {
                  timeDataSerie[invalidIndex].runDyn_StanceTime = value;
               }
               break;
            }
         }

      } else {

         // data are available

         runDyn_StanceTime = new short[serieSize];
         isAvailable = true;
      }

      return isAvailable;
   }

   private boolean setupStartingValues_RunDyn_StanceTimeBalance(final TimeData[] timeDataSerie) {

      final TimeData firstTimeData = timeDataSerie[0];
      final int serieSize = timeDataSerie.length;

      boolean isAvailable = false;

      if (firstTimeData.runDyn_StanceTimeBalance == Short.MIN_VALUE) {

         // search for first valid value

         for (int timeDataIndex = 0; timeDataIndex < serieSize; timeDataIndex++) {

            final short value = timeDataSerie[timeDataIndex].runDyn_StanceTimeBalance;

            if (value != Short.MIN_VALUE) {

               // data are available, starting values are set to first valid value

               runDyn_StanceTimeBalance = new short[serieSize];
               isAvailable = true;

               for (int invalidIndex = 0; invalidIndex < timeDataIndex; invalidIndex++) {
                  timeDataSerie[invalidIndex].runDyn_StanceTimeBalance = value;
               }
               break;
            }
         }

      } else {

         // data are available

         runDyn_StanceTimeBalance = new short[serieSize];
         isAvailable = true;
      }

      return isAvailable;
   }

   private boolean setupStartingValues_RunDyn_StepLength(final TimeData[] timeDataSerie) {

      final TimeData firstTimeData = timeDataSerie[0];
      final int serieSize = timeDataSerie.length;

      boolean isAvailable = false;

      if (firstTimeData.runDyn_StepLength == Short.MIN_VALUE) {

         // search for first valid value

         for (int timeDataIndex = 0; timeDataIndex < serieSize; timeDataIndex++) {

            final short value = timeDataSerie[timeDataIndex].runDyn_StepLength;

            if (value != Short.MIN_VALUE) {

               // data are available, starting values are set to first valid value

               runDyn_StepLength = new short[serieSize];
               isAvailable = true;

               for (int invalidIndex = 0; invalidIndex < timeDataIndex; invalidIndex++) {
                  timeDataSerie[invalidIndex].runDyn_StepLength = value;
               }
               break;
            }
         }

      } else {

         // data are available

         runDyn_StepLength = new short[serieSize];
         isAvailable = true;
      }

      return isAvailable;
   }

   private boolean setupStartingValues_RunDyn_VerticalOscillation(final TimeData[] timeDataSerie) {

      final TimeData firstTimeData = timeDataSerie[0];
      final int serieSize = timeDataSerie.length;

      boolean isAvailable = false;

      if (firstTimeData.runDyn_VerticalOscillation == Short.MIN_VALUE) {

         // search for first valid value

         for (int timeDataIndex = 0; timeDataIndex < serieSize; timeDataIndex++) {

            final short value = timeDataSerie[timeDataIndex].runDyn_VerticalOscillation;

            if (value != Short.MIN_VALUE) {

               // data are available, starting values are set to first valid value

               runDyn_VerticalOscillation = new short[serieSize];
               isAvailable = true;

               for (int invalidIndex = 0; invalidIndex < timeDataIndex; invalidIndex++) {
                  timeDataSerie[invalidIndex].runDyn_VerticalOscillation = value;
               }
               break;
            }
         }

      } else {

         // data are available

         runDyn_VerticalOscillation = new short[serieSize];
         isAvailable = true;
      }

      return isAvailable;
   }

   private boolean setupStartingValues_RunDyn_VerticalRatio(final TimeData[] timeDataSerie) {

      final TimeData firstTimeData = timeDataSerie[0];
      final int serieSize = timeDataSerie.length;

      boolean isAvailable = false;

      if (firstTimeData.runDyn_VerticalRatio == Short.MIN_VALUE) {

         // search for first valid value

         for (int timeDataIndex = 0; timeDataIndex < serieSize; timeDataIndex++) {

            final short value = timeDataSerie[timeDataIndex].runDyn_VerticalRatio;

            if (value != Short.MIN_VALUE) {

               // data are available, starting values are set to first valid value

               runDyn_VerticalRatio = new short[serieSize];
               isAvailable = true;

               for (int invalidIndex = 0; invalidIndex < timeDataIndex; invalidIndex++) {
                  timeDataSerie[invalidIndex].runDyn_VerticalRatio = value;
               }
               break;
            }
         }

      } else {

         // data are available

         runDyn_VerticalRatio = new short[serieSize];
         isAvailable = true;
      }

      return isAvailable;
   }

   private boolean setupStartingValues_Speed(final TimeData[] timeDataSerie) {

      boolean isAvailable = false;

      // find valid speed slices
      for (final TimeData timeData : timeDataSerie) {

         if (timeData.speed != Float.MIN_VALUE) {
            isAvailable = true;
            break;
         }
      }

      if (isAvailable) {

         // cleanup speed serie, remove invalid values

         for (final TimeData timeData : timeDataSerie) {

            if (timeData.speed == Float.MIN_VALUE) {
               timeData.speed = 0;
            }
         }

         speedSerie = new float[timeDataSerie.length];
         isSpeedSerieFromDevice = true;
      }

      return isAvailable;
   }

   private boolean setupStartingValues_Temperature(final TimeData[] timeDataSerie) {

      final TimeData firstTimeData = timeDataSerie[0];
      final int serieSize = timeDataSerie.length;

      boolean isAvailable = false;

      if (firstTimeData.temperature == Float.MIN_VALUE) {

         for (int timeDataIndex = 0; timeDataIndex < serieSize; timeDataIndex++) {

            final TimeData timeData = timeDataSerie[timeDataIndex];
            final float temperature = timeData.temperature;

            if (temperature != Float.MIN_VALUE) {

               // temperature values are available, starting values are set to the first valid value

               temperatureSerie = new float[serieSize];
               isAvailable = true;

               // update values to the first valid value
               for (int invalidIndex = 0; invalidIndex < timeDataIndex; invalidIndex++) {
                  timeDataSerie[invalidIndex].temperature = temperature;
               }

               break;
            }
         }

      } else {

         // temperature values are available

         temperatureSerie = new float[serieSize];
         isAvailable = true;
      }

      return isAvailable;
   }

   /**
    * Set surfing visible points which can be saved in the tour.
    *
    * @param visiblePoints_ForSurfing
    */
   public void setVisiblePoints_ForSurfing(final boolean[] visiblePoints_ForSurfing) {

      this.visiblePoints_ForSurfing = visiblePoints_ForSurfing;
   }

   public void setWayPoints(final ArrayList<TourWayPoint> wptList) {

      // remove old way points
      tourWayPoints.clear();

      if ((wptList == null) || (wptList.isEmpty())) {
         return;
      }

      // set new way points
      for (final TourWayPoint tourWayPoint : wptList) {

         /**
          * !!!! <br>
          * Way point must be cloned because the entity could be saved within different tour data
          * instances, otherwise hibernate exceptions occur this also sets the createId. <br>
          * !!!!
          */
         final TourWayPoint clonedWP = tourWayPoint.clone(this);

         tourWayPoints.add(clonedWP);
      }
   }

   public void setWeather(final String weather) {
      this.weather = weather;
   }

   /**
    * {@link #weather_Humidity}
    *
    * @param weatherHumidity
    */
   public void setWeather_Humidity(final short weatherHumidity) {
      this.weather_Humidity = weatherHumidity;
   }

   /**
    * {@link #weather_Precipitation}
    *
    * @param weatherPrecipitation
    */
   public void setWeather_Precipitation(final float weatherPrecipitation) {
      this.weather_Precipitation = weatherPrecipitation;
   }

   /**
    * {@link #weather_Pressure}
    *
    * @param weatherPressure
    */
   public void setWeather_Pressure(final float weatherPressure) {
      this.weather_Pressure = weatherPressure;
   }

   public void setWeather_Temperature_Max(final float weatherMaxTemperature) {
      this.weather_Temperature_Max = weatherMaxTemperature;
   }

   public void setWeather_Temperature_Min(final float weatherMinTemperature) {
      this.weather_Temperature_Min = weatherMinTemperature;
   }

   public void setWeather_Temperature_WindChill(final float weatherWindChill) {
      this.weather_Temperature_WindChill = weatherWindChill;
   }

   /**
    * Sets the weather id which is defined in {@link IWeather#WEATHER_ID_}... or <code>null</code>
    * when weather id is not defined
    *
    * @param weatherClouds
    */
   public void setWeatherClouds(final String weatherClouds) {
      this.weatherClouds = weatherClouds;
   }

   public void setWeatherWindDir(final int weatherWindDir) {
      this.weatherWindDir = weatherWindDir;
   }

   public void setWeatherWindSpeed(final int weatherWindSpeed) {
      this.weatherWindSpd = weatherWindSpeed;
   }

   /**
    * Set world positions which are cached
    *
    * @param worldPositions
    * @param zoomLevel
    * @param projectionId
    */
   public void setWorldPixelForTour(final Point[] worldPositions, final int zoomLevel, final String projectionId) {

      _tourWorldPosition.put(projectionId.hashCode() + zoomLevel, worldPositions);
   }

   /**
    * Set world positions which are cached
    *
    * @param worldPositions
    * @param zoomLevel
    * @param projectionId
    */
   public void setWorldPixelForWayPoints(final TIntObjectHashMap<Point> worldPositions,
                                         final int zoomLevel,
                                         final String projectionId) {

      _twpWorldPosition.put(projectionId.hashCode() + zoomLevel, worldPositions);
   }

   public String toJson() {

      final ObjectMapper mapper = new ObjectMapper();
      mapper.setSerializationInclusion(Include.NON_NULL);
      mapper.setSerializationInclusion(Include.NON_EMPTY);
      mapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);

      String jsonString = UI.EMPTY_STRING;
      try {
         jsonString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
      } catch (final JsonProcessingException e) {
         e.printStackTrace();
      }
      return jsonString;
   }

   @Override
   public String toString() {

      return "TourData [" + NL //                                                                     //$NON-NLS-1$

            + "start          = " + startYear + UI.DASH + startMonth + UI.DASH + startDay + UI.SPACE //$NON-NLS-1$
            + startHour + UI.SYMBOL_COLON + startMinute + UI.SYMBOL_COLON + startSecond + NL

            + "tourId         = " + tourId + NL //                                                    //$NON-NLS-1$
            + "isMultipleTours= " + isMultipleTours + NL //                                           //$NON-NLS-1$

//            + "object   = " + super.toString() + NL //                                              //$NON-NLS-1$
//            + "identityHashCode=" + System.identityHashCode(this) + NL //                           //$NON-NLS-1$

//            + "marker size:" + tourMarkers.size() + " " + tourMarkers + NL //                       //$NON-NLS-1$

            + "]"; //$NON-NLS-1$
   }

   public String toStringWithHash() {

      final String string = UI.EMPTY_STRING
            + "   tourId: " + tourId //$NON-NLS-1$
            + "   identityHashCode: " + System.identityHashCode(this); //$NON-NLS-1$

      return string;
   }

   @Override
   public String toXml() {

      try {
         final Marshaller marshaller = JAXBContext.newInstance(TourData.class).createMarshaller();
         final StringWriter sw = new StringWriter();
         marshaller.marshal(this, sw);
         return sw.toString();

      } catch (final JAXBException e) {
         e.printStackTrace();
      }

      return null;
   }

   /**
    * Converts data series from db version 42 to 43
    */
   public void updateDatabaseDesign_042_to_043() {

      // convert lat/lon double -> E6
      onPrePersist();
   }

   /**
    * Adjust paused times when tour start has changed.
    *
    * @param startTimeOffset
    */
   private void updatePausedTimes(final long startTimeOffset) {

      if (pausedTime_Start == null || pausedTime_End == null) {
         return;
      }

      for (int index = 0; index < pausedTime_Start.length && index < pausedTime_End.length; ++index) {
         pausedTime_Start[index] += startTimeOffset;
         pausedTime_End[index] += startTimeOffset;
      }
   }
}
