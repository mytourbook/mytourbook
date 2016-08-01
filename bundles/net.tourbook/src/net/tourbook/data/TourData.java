/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.awt.Point;
import java.io.File;
import java.io.PrintStream;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
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
import net.tourbook.common.map.GeoPosition;
import net.tourbook.common.util.MtMath;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.common.weather.IWeather;
import net.tourbook.database.FIELD_VALIDATION;
import net.tourbook.database.TourDatabase;
import net.tourbook.importdata.TourbookDevice;
import net.tourbook.math.Smooth;
import net.tourbook.photo.Photo;
import net.tourbook.photo.PhotoCache;
import net.tourbook.photo.TourPhotoReference;
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
import net.tourbook.ui.UI;
import net.tourbook.ui.tourChart.ChartLabel;
import net.tourbook.ui.tourChart.ChartLayer2ndAltiSerie;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.views.ISmoothingAlgorithm;
import net.tourbook.ui.views.tourDataEditor.TourDataEditorView;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.hibernate.annotations.Cascade;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * Tour data contains all data for a tour (except markers), an entity will be saved in the database
 */
@Entity
@XmlType(name = "TourData")
@XmlRootElement(name = "TourData")
@XmlAccessorType(XmlAccessType.NONE)
public class TourData implements Comparable<Object>, IXmlSerializable {

	public static final int										DB_LENGTH_DEVICE_TOUR_TYPE			= 2;
	public static final int										DB_LENGTH_DEVICE_PLUGIN_ID			= 255;
	public static final int										DB_LENGTH_DEVICE_PLUGIN_NAME		= 255;
	public static final int										DB_LENGTH_DEVICE_MODE_NAME			= 255;
	public static final int										DB_LENGTH_DEVICE_FIRMWARE_VERSION	= 255;

	public static final int										DB_LENGTH_TOUR_TITLE				= 255;
	public static final int										DB_LENGTH_TOUR_DESCRIPTION			= 4096;
	public static final int										DB_LENGTH_TOUR_DESCRIPTION_V10		= 32000;
	public static final int										DB_LENGTH_TOUR_START_PLACE			= 255;
	public static final int										DB_LENGTH_TOUR_END_PLACE			= 255;
	public static final int										DB_LENGTH_TOUR_IMPORT_FILE_PATH		= 255;
	public static final int										DB_LENGTH_TOUR_IMPORT_FILE_NAME		= 255;

	public static final int										DB_LENGTH_WEATHER					= 1000;
	public static final int										DB_LENGTH_WEATHER_CLOUDS			= 255;

	private static final String									TIME_ZONE_ID_EUROPE_BERLIN			= "Europe/Berlin";										//$NON-NLS-1$

	public static final int										MIN_TIMEINTERVAL_FOR_MAX_SPEED		= 20;

	public static final float									MAX_BIKE_SPEED						= 120f;

	/**
	 * Number of defined hr zone fields which is currently {@link #hrZone0} ... {@link #hrZone9} =
	 * 10
	 */
	public static final int										MAX_HR_ZONES						= 10;

	/**
	 * Device Id for manually created tours
	 */
	public static final String									DEVICE_ID_FOR_MANUAL_TOUR			= "manual";											//$NON-NLS-1$

	/**
	 * Device id for csv files which behave like manually created tours, marker and timeslices are
	 * disabled because they are not available, tour duration can be edited<br>
	 * this is the id of the deviceDataReader
	 */
	public static final String									DEVICE_ID_CSV_TOUR_DATA_READER		= "net.tourbook.device.CSVTourDataReader";				//$NON-NLS-1$

	/**
	 * THIS IS NOT UNUSED !!!<br>
	 * <br>
	 * it initializes SRTM
	 */
	@Transient
	private static final NumberForm								srtmNumberForm						= new NumberForm();

	@Transient
	private static final ElevationSRTM3							elevationSRTM3						= new ElevationSRTM3();

	@Transient
	private static IPreferenceStore								_prefStore							= TourbookPlugin
																											.getPrefStore();

	/**
	 * This instance is static, otherwise each TourData creation creates a new instance which can
	 * occure very often.
	 */
	@Transient
	private static final Calendar								_calendar							= GregorianCalendar
																											.getInstance();

	/**
	 * Unique entity id which identifies the tour
	 */
	@Id
	private Long												tourId;

	// ############################################# DATE #############################################

	/**
	 * Tour start time in milliseconds since 1970-01-01T00:00:00Z
	 * 
	 * @since DB version 22
	 */
	private long												tourStartTime;

	/**
	 * Tour end time in milliseconds since 1970-01-01T00:00:00Z
	 * 
	 * @since DB version 22
	 */
	private long												tourEndTime;

	/**
	 * year of tour start
	 */
	private short												startYear;

	/**
	 * mm (d) month of tour
	 */
	private short												startMonth;

	/**
	 * dd (d) day of tour
	 */
	private short												startDay;

	/**
	 * HH (d) hour of tour
	 */
	private short												startHour;

	/**
	 * MM (d) minute of tour
	 */
	private short												startMinute;

	/**
	 * 
	 */
	private int													startSecond;																				// db-version 7

	/**
	 * THIS IS NOT UNUSED !!!<br>
	 * <br>
	 * week of the tour provided by {@link Calendar#get(int)}
	 */
	@SuppressWarnings("unused")
	private short												startWeek;

	/**
	 * THIS IS NOT UNUSED !!!<br>
	 * this field can be read with sql statements
	 * <p>
	 * year for startWeek
	 */
	@SuppressWarnings("unused")
	private short												startWeekYear;

	// ############################################# TIME #############################################

	/**
	 * Total recording time in seconds
	 * 
	 * @since Is long since db version 22, before it was int
	 */
	@XmlElement
	private long												tourRecordingTime;

	/**
	 * Total driving/moving time in seconds
	 * 
	 * @since Is long since db version 22, before it was int
	 */
	@XmlElement
	private long												tourDrivingTime;

	/**
	 * Timezone offset in seconds or {@link Integer#MIN_VALUE} when a timezone is not available. The
	 * default timezone is used (which is set in the preferences) to display the tour when not
	 * available.
	 */
	private int													timeZoneOffset						= Integer.MIN_VALUE;

	// ############################################# DISTANCE #############################################

	/**
	 * Total distance of the device at tour start (km) tttt (h). Distance for the tour is stored in
	 * the field {@link #tourDistance}
	 */
	private float												startDistance;

	/**
	 * total distance of the tour in meters (metric system), this value is computed from the
	 * distance data serie
	 */
	@XmlElement
	private float												tourDistance;

	/**
	 * Are the distance values measured with a distance sensor or with lat/lon values.<br>
	 * <br>
	 * 0 == false <i>(default, no distance sensor)</i> <br>
	 * 1 == true
	 */
	private short												isDistanceFromSensor				= 0;													// db-version 8

	// ############################################# ALTITUDE #############################################

	/**
	 * aaaa (h) initial altitude (m)
	 */
	private short												startAltitude;

	/**
	 * altitude up (m)
	 */
	@XmlElement
	private int													tourAltUp;

	/**
	 * altitude down (m)
	 */
	@XmlElement
	private int													tourAltDown;

	// ############################################# PULSE/WEIGHT/POWER #############################################

	/**
	 * pppp (h) initial pulse (bpm)
	 */
	private short												startPulse;

	@XmlElement
	private int													restPulse;																					// db-version 8

	@XmlElement
	private Integer												calories;																					// db-version 4

	private float												bikerWeight;																				// db-version 4

	/**
	 * A flag indicating that the power is from a sensor. This is the state of the device which is
	 * not related to the availability of power data. Power data should be available but is not
	 * checked.<br>
	 * <br>
	 * 0 == false, 1 == true
	 */
	private int													isPowerSensorPresent				= 0;													// db-version 12

	// ############################################# PULSE #############################################

	/**
	 * Average pulse, this data can also be set from device data and pulse data are not available
	 * 
	 * @since is float since db version 21, before it was int
	 */
	@XmlElement
	private float												avgPulse;																					// db-version 4

	/**
	 * Maximum pulse for the current tour.
	 * 
	 * @since is float since db version 21, before it was int
	 */
	@XmlElement
	private float												maxPulse;																					// db-version 4

	/**
	 * Number of HR zones which are available for this tour, is 0 when HR zones are not defined.
	 */
	private int													numberOfHrZones						= 0;													// db-version 18

	/**
	 * Time for all HR zones are contained in {@link #hrZone0} ... {@link #hrZone9}. Each tour can
	 * have up to 10 HR zones, when HR zone value is <code>-1</code> then this zone is not set.
	 * <p>
	 * These values are used in the statistic views.
	 */
	private int													hrZone0								= -1;													// db-version 16
	private int													hrZone1								= -1;													// db-version 16
	private int													hrZone2								= -1;													// db-version 16
	private int													hrZone3								= -1;													// db-version 16
	private int													hrZone4								= -1;													// db-version 16
	private int													hrZone5								= -1;													// db-version 16
	private int													hrZone6								= -1;													// db-version 16
	private int													hrZone7								= -1;													// db-version 16
	private int													hrZone8								= -1;													// db-version 16
	private int													hrZone9								= -1;													// db-version 16

	/**
	 * A flag indicating that the pulse is from a sensor. This is the state of the device which is
	 * not related to the availability of pulse data. Pulse data should be available but is not
	 * checked.<br>
	 * <br>
	 * 0 == false, 1 == true
	 */
	private int													isPulseSensorPresent				= 0;													// db-version 12

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
	private String												deviceTourType;

	/**
	 * Visible name for the used profile which is defined in {@link #deviceMode}, e.g. Jogging,
	 * Running, Bike1, Bike2...
	 */
	private String												deviceModeName;																			// db-version 4

	/**
	 * maximum altitude in metric system
	 * 
	 * @since is float since db version 21, before it was int
	 */
	@XmlElement
	private float												maxAltitude;																				// db-version 4

	// ############################################# MAX VALUES #############################################

	/**
	 * maximum speed in metric system
	 */
	@XmlElement
	private float												maxSpeed;																					// db-version 4

	// ############################################# AVERAGE VALUES #############################################

	/**
	 * @since is float since db version 21, before it was int
	 */
	@XmlElement
	private float												avgCadence;																				// db-version 4

	/**
	 * @since Is float since db version 21, before it was int. In db version 20 this field was
	 *        already float but not the database field.
	 */
	private float												avgTemperature;																			// db-version 4

	// ############################################# WEATHER #############################################

	private int													weatherWindDir;																			// db-version 8

	private int													weatherWindSpd;																			// db-version 8
	private String												weatherClouds;																				// db-version 8
	private String												weather;																					// db-version 13

	// ############################################# POWER #############################################

	private float												power_Avg;
	private int													power_Max;
	private int													power_Normalized;

	/** Functional Threshold Power (FTP) */
	private int													power_FTP;

	private long												power_TotalWork;
	private float												power_TrainingStressScore;
	private float												power_IntensityFactor;

	private int													power_PedalLeftRightBalance;
	private float												power_AvgLeftTorqueEffectiveness;
	private float												power_AvgRightTorqueEffectiveness;
	private float												power_AvgLeftPedalSmoothness;
	private float												power_AvgRightPedalSmoothness;

	// ############################################# OTHER TOUR/DEVICE DATA #############################################

	@XmlElement
	private String												tourTitle;																					// db-version 4

	@XmlElement
	private String												tourDescription;																			// db-version 4

	@XmlElement
	private String												tourStartPlace;																			// db-version 4

	@XmlElement
	private String												tourEndPlace;																				// db-version 4

	/**
	 * Date/Time when tour data was created. This value is set to the tour start date before db
	 * version 11, otherwise the value is set when the tour is saved the first time.
	 * <p>
	 * Data format: YYYYMMDDhhmmss
	 */
	private long												dateTimeCreated;																			// db-version 11

	/**
	 * Date/Time when tour data was modified, default value is 0
	 * <p>
	 * Data format: YYYYMMDDhhmmss
	 */
	private long												dateTimeModified;																			// db-version 11

	/** Folder path from the import file. */
	private String												tourImportFilePath;																		// db-version 6

	/** File name from the import file. */
	private String												tourImportFileName;																		// db-version 29

	/**
	 * Tolerance for the Douglas Peucker algorithm.
	 */
	private short												dpTolerance							= 50;													// 5.0 since version 14.7

	/**
	 * Time difference in seconds between 2 time slices or <code>-1</code> for GPS devices when the
	 * time slices has variable time duration
	 */
	private short												deviceTimeInterval					= -1;													// db-version 3

	/**
	 * Scaling factor for the temperature data serie, e.g. when set to 10 the temperature data serie
	 * is multiplied by 10, default scaling is <code>1</code>
	 */
	/*
	 * disabled when float was introduces in 11.after8, preserved in database that older ejb objects
	 * can be loaded
	 */
	private int													temperatureScale					= 1;													// db-version 13

	/**
	 * Firmware version of the device
	 */
	private String												deviceFirmwareVersion;																		// db-version 12

	/**
	 * This value is multiplied with the cadence data serie when displayed, cadence data serie is
	 * always saved with rpm.
	 * <p>
	 * 1.0f = Revolutions per minute (RPM) <br>
	 * 2.0f = Steps per minute (SPM)
	 */
	private float												cadenceMultiplier					= 1.0f;

	/**
	 * When <code>1</code> then a stride sensor is available.
	 * <p>
	 * 0 == false, 1 == true
	 */
	private short												isStrideSensorPresent				= 0;

	// ############################################# MERGED DATA #############################################

	/**
	 * when a tour is merged with another tour, {@link #mergeSourceTourId} contains the tour id of
	 * the tour which is merged into this tour
	 */
	private Long												mergeSourceTourId;																			// db-version 7

	/**
	 * when a tour is merged into another tour, {@link #mergeTargetTourId} contains the tour id of
	 * the tour into which this tour is merged
	 */
	private Long												mergeTargetTourId;																			// db-version 7

	/**
	 * positive or negative time offset in seconds for the merged tour
	 */
	private int													mergedTourTimeOffset;																		// db-version 7

	/**
	 * altitude difference for the merged tour
	 */
	private int													mergedAltitudeOffset;																		// db-version 7

	/**
	 * Unique plugin id for the device data reader which created this tour, this id is defined in
	 * plugin.xml
	 * <p>
	 * a better name would be <i>pluginId</i>
	 */
	private String												devicePluginId;

	// ############################################# PLUGIN DATA #############################################

	/**
	 * Visible name for the used {@link TourbookDevice}, this name is defined in plugin.xml
	 * <p>
	 * a better name would be <i>pluginName</i>
	 */
	private String												devicePluginName;																			// db-version 4

	/**
	 * Deflection point in the conconi test, this value is the index for the data serie on the
	 * x-axis
	 */
	private int													conconiDeflection;

	// ############################################# PHOTO  DATA #############################################

	/**
	 * Number of photos which are set in {@link #tourPhotos}. This field is displayed only in views
	 * or works as tour filter.
	 */
	@SuppressWarnings("unused")
	private int													numberOfPhotos;

	/**
	 * Number of time slices in {@link #timeSerie}
	 */
	private int													numberOfTimeSlices;

	/**
	 * Time adjustment in seconds, this is an average value for all photos.
	 */
	private int													photoTimeAdjustment;

	// ############################################# GEARS #############################################

	private int													frontShiftCount;

	private int													rearShiftCount;

	// ############################################# UNUSED FIELDS - START #############################################
	/**
	 * ssss distance msw
	 * <p>
	 * is not used any more since 6.12.2006 but it's necessary then it's a field in the database
	 */
	@SuppressWarnings("unused")
	private int													distance;

	@SuppressWarnings("unused")
	private float												deviceAvgSpeed;																			// db-version 12

	@SuppressWarnings("unused")
	private int													deviceDistance;

	/**
	 * Profile id which is defined by the device
	 */
	@SuppressWarnings("unused")
	private short												deviceMode;																				// db-version 3

	@SuppressWarnings("unused")
	private int													deviceTotalUp;

	@SuppressWarnings("unused")
	private int													deviceTotalDown;

	@SuppressWarnings("unused")
	private long												deviceTravelTime;

	@SuppressWarnings("unused")
	private int													deviceWheel;

	@SuppressWarnings("unused")
	private int													deviceWeight;

	// ############################################# UNUSED FIELDS - END #############################################

	/**
	 * data series for time, altitude,...
	 */
	@Basic(optional = false)
	private SerieData											serieData;

	/**
	 * Photos for this tour
	 */
	@OneToMany(fetch = FetchType.EAGER, cascade = ALL, mappedBy = "tourData")
	@Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
	private Set<TourPhoto>										tourPhotos							= new HashSet<TourPhoto>();

	// ############################################# ASSOCIATED ENTITIES #############################################

	/**
	 * Tour marker
	 */
	@OneToMany(fetch = FetchType.EAGER, cascade = ALL, mappedBy = "tourData")
	@Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
	@XmlElementWrapper(name = "TourMarkers")
	@XmlElement(name = "TourMarker")
	private Set<TourMarker>										tourMarkers							= new HashSet<TourMarker>();

	/**
	 * Contains the tour way points
	 */
	@OneToMany(fetch = FetchType.EAGER, cascade = ALL, mappedBy = "tourData")
	@Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
	private final Set<TourWayPoint>								tourWayPoints						= new HashSet<TourWayPoint>();

	/**
	 * Reference tours
	 */
	@OneToMany(fetch = FetchType.EAGER, cascade = ALL, mappedBy = "tourData")
	@Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
	private final Set<TourReference>							tourReferences						= new HashSet<TourReference>();

	/**
	 * Tags
	 */
	@ManyToMany(fetch = EAGER)
	@JoinTable(inverseJoinColumns = @JoinColumn(name = "TOURTAG_TagID", referencedColumnName = "TagID"))
	private Set<TourTag>										tourTags							= new HashSet<TourTag>();

//	/**
//	 * SharedMarker
//	 */
//	@ManyToMany(fetch = EAGER)
//	@JoinTable(inverseJoinColumns = @JoinColumn(name = "SHAREDMARKER_SharedMarkerID", referencedColumnName = "SharedMarkerID"))
//	private Set<SharedMarker>									sharedMarker						= new HashSet<SharedMarker>();

	/**
	 * Category of the tour, e.g. bike, mountainbike, jogging, inlinescating
	 */
	@ManyToOne
	private TourType											tourType;

	/**
	 * Person which created this tour or <code>null</code> when the tour is not saved in the
	 * database
	 */
	@ManyToOne
	private TourPerson											tourPerson;

	/**
	 * plugin id for the device which was used for this tour Bike used for this tour
	 */
	@ManyToOne
	private TourBike											tourBike;

	/**
	 * <br>
	 * <br>
	 * <br>
	 * <br>
	 * <br>
	 * ################################### TRANSIENT DATA ######################################## <br>
	 * <br>
	 * <br>
	 * <br>
	 * <br>
	 * <br>
	 */

	/**
	 * Contains time in <b>seconds</b> relativ to the tour start which is defined in
	 * {@link #tourStartTime}.
	 * <p>
	 * The array {@link #timeSerie} is <code>null</code> for a manually created tour, it is
	 * <b>always</b> set when tour is from a device or an imported file.
	 * <p>
	 * This field has a copy in {@link #timeSerieFloat}.
	 */
	@Transient
	public int[]												timeSerie;

	/**
	 * contains the absolute distance in m (metric system)
	 */
	@Transient
	public float[]												distanceSerie;

	/**
	 * Distance values with double type to display it on the x-axis
	 */
	@Transient
	private double[]											distanceSerieDouble;

	/**
	 * contains the absolute distance in miles/1000 (imperial system)
	 */
	@Transient
	private double[]											distanceSerieDoubleImperial;

	/**
	 * Contains the absolute altitude in meter (metric system) or <code>null</code> when not
	 * available.
	 */
	@Transient
	public float[]												altitudeSerie;

	/**
	 * smoothed altitude serie is used to display the tour chart when not <code>null</code>
	 */
	@Transient
	private float[]												altitudeSerieSmoothed;

	/**
	 * contains the absolute altitude in feet (imperial system)
	 */
	@Transient
	private float[]												altitudeSerieImperial;

	/**
	 * smoothed altitude serie is used to display the tour chart when not <code>null</code>
	 */
	@Transient
	private float[]												altitudeSerieImperialSmoothed;

	/**
	 * SRTM altitude values, when <code>null</code> srtm data have not yet been attached, when
	 * <code>length()==0</code> data are invalid.
	 */
	@Transient
	private float[]												srtmSerie;

	@Transient
	private float[]												srtmSerieImperial;

	@Transient
	public float[]												cadenceSerie;

	@Transient
	public float[]												pulseSerie;

	@Transient
	private float[]												pulseSerieSmoothed;

	@Transient
	public int[]												pulseTimeSerie;

	/**
	 * Contains <code>true</code> or <code>false</code> for each time slice of the whole tour.
	 * <code>true</code> is set when a time slice is a break.
	 */
	@Transient
	private boolean[]											breakTimeSerie;

	/**
	 * Contains the temperature in the metric measurement system.
	 */
	@Transient
	public float[]												temperatureSerie;

	/**
	 * contains the temperature in the imperial measurement system
	 */
	@Transient
	private float[]												temperatureSerieImperial;

	/**
	 * Contains speed in km/h
	 * <p>
	 * the metric speed serie is required when computing the power even if the current measurement
	 * system is imperial
	 */
	@Transient
	private float[]												speedSerie;

	@Transient
	private float[]												speedSerieImperial;

	/**
	 * Is <code>true</code> when the data in {@link #speedSerie} are from the device and not
	 * computed. Speed data are normally available from an ergometer and not from a bike computer
	 */
	@Transient
	private boolean												isSpeedSerieFromDevice				= false;

	/**
	 * pace in sec/km
	 */
	@Transient
	private float[]												paceSerieSeconds;

	/**
	 * pace in sec/mile
	 */
	@Transient
	private float[]												paceSerieSecondsImperial;

	/**
	 * pace in min/km
	 */
	@Transient
	private float[]												paceSerieMinute;

	/**
	 * pace in min/mile
	 */
	@Transient
	private float[]												paceSerieMinuteImperial;

	@Transient
	private float[]												powerSerie;

	/**
	 * Is <code>true</code> when the data in {@link #powerSerie} are from the device and not
	 * computed. Power data source can be an ergometer or a power sensor
	 */
	@Transient
	private boolean												isPowerSerieFromDevice				= false;

	@Transient
	private float[]												altimeterSerie;

	@Transient
	private float[]												altimeterSerieImperial;

	@Transient
	public float[]												gradientSerie;

	@Transient
	public float[]												tourCompareSerie;

	/*
	 * GPS data
	 */
	/**
	 * Contains tour latitude data or <code>null</code> when GPS data are not available.
	 */
	@Transient
	public double[]												latitudeSerie;

	@Transient
	public double[]												longitudeSerie;

	/**
	 * Gears which are saved in a tour are in this HEX format (left to right)
	 * <p>
	 * Front teeth<br>
	 * Front gear number<br>
	 * Back teeth<br>
	 * Back gear number<br>
	 * <code>
	 * <pre>
final long	frontTeeth	= (gearRaw &gt;&gt; 24 &amp; 0xff);
final long	frontGear	= (gearRaw &gt;&gt; 16 &amp; 0xff);
final long	rearTeeth	= (gearRaw &gt;&gt; 8 &amp; 0xff);
final long	rearGear	= (gearRaw &gt;&gt; 0 &amp; 0xff);
	 * </pre>
	 * </code>
	 */
	@Transient
	public long[]												gearSerie;

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
	private float[][]											_gears;

	/**
	 * Contains the bounds of the tour in latitude/longitude:
	 * <p>
	 * 1st item contains lat/lon minimum values<br>
	 * 2nd item contains lat/lon maximum values<br>
	 */
	@Transient
	private GeoPosition[]										_gpsBounds;

	/**
	 * Index of the segmented data in the data series
	 */
	@Transient
	public int[]												segmentSerieIndex;

	/**
	 * 2nd Index of the segmented data in the data series.
	 * <p>
	 * {@link #segmentSerieIndex} contains the outer index, this contains the inner index.
	 * <p>
	 * This is used, first to create the sements by the outer attribute, e.g. tour marker and then
	 * create the inner segments, e.g. altitude with DP.
	 */
	@Transient
	public int[]												segmentSerieIndex2nd;

	/**
	 * oooo (o) DD-record // offset
	 */
	@Transient
	public int													offsetDDRecord;

	/*
	 * data for the tour segments
	 */
	@Transient
	private int[]												segmentSerie_Time_Total;
	@Transient
	private int[]												segmentSerie_Time_Recording;
	@Transient
	public int[]												segmentSerie_Time_Driving;
	@Transient
	private int[]												segmentSerie_Time_Break;

	@Transient
	private float[]												segmentSerie_Distance_Diff;
	@Transient
	private float[]												segmentSerie_Distance_Total;

	@Transient
	public float[]												segmentSerie_Altitude_Diff;
	@Transient
	public float[]												segmentSerie_Altitude_Diff_Computed;
	@Transient
	public float[]												segmentSerie_Altitude_UpDown_Hour;
	@Transient
	public float												segmentSerieTotal_Altitude_Down;
	@Transient
	public float												segmentSerieTotal_Altitude_Up;

	@Transient
	public float[]												segmentSerie_Speed;
	@Transient
	public float[]												segmentSerie_Cadence;
	@Transient
	public float[]												segmentSerie_Pace;
	@Transient
	public float[]												segmentSerie_Pace_Diff;
	@Transient
	public float[]												segmentSerie_Power;
	@Transient
	public float[]												segmentSerie_Gradient;
	@Transient
	public float[]												segmentSerie_Pulse;

	/**
	 * Keep original import file path, this is used when the tour file should be deleted.
	 */
	@Transient
	public String												importFilePathOriginal;

	/**
	 * Latitude for the center position in the map or {@link Double#MIN_VALUE} when the position is
	 * not set
	 */
	@Transient
	public double												mapCenterPositionLatitude			= Double.MIN_VALUE;

	/**
	 * Longitude for the center position in the map or {@link Double#MIN_VALUE} when the position is
	 * not set
	 */
	@Transient
	public double												mapCenterPositionLongitude			= Double.MIN_VALUE;

	/**
	 * Zoomlevel in the map
	 */
	@Transient
	public int													mapZoomLevel;

	/**
	 * caches the world positions for the tour lat/long values for each zoom level
	 */
	@Transient
//	private final Map<Integer, Point[]>						_tourWorldPosition					= new HashMap<Integer, Point[]>();
	private final TIntObjectHashMap<Point[]>					_tourWorldPosition					= new TIntObjectHashMap<Point[]>();

	/**
	 * caches the world positions for the way point lat/long values for each zoom level
	 */
	@Transient
//	private final HashMap<Integer, HashMap<Integer, Point>>	_twpWorldPosition					= new HashMap<Integer, HashMap<Integer, Point>>();
	private final TIntObjectHashMap<TIntObjectHashMap<Point>>	_twpWorldPosition					= new TIntObjectHashMap<TIntObjectHashMap<Point>>();

	/**
	 * when a tour was deleted and is still visible in the raw data view, resaving the tour or
	 * finding the tour in the entity manager causes lots of trouble with hibernate, therefor this
	 * tour cannot be saved again, it must be reloaded from the file system
	 */
	@Transient
	public boolean												isTourDeleted						= false;

	/**
	 * 2nd data serie, this is used in the {@link ChartLayer2ndAltiSerie} to display the merged tour
	 * or the adjusted altitude
	 */
	@Transient
	public float[]												dataSerie2ndAlti;

	/**
	 * altitude difference between this tour and the merge tour with metric measurement
	 */
	@Transient
	public float[]												dataSerieDiffTo2ndAlti;

	/**
	 * contains the altitude serie which is adjusted
	 */
	@Transient
	public float[]												dataSerieAdjustedAlti;

	/**
	 * contains special data points
	 */
	@Transient
	public SplineData											splineDataPoints;

	/**
	 * Contains a spline data serie
	 */
	@Transient
	public float[]												dataSerieSpline;

	/**
	 * when a tour is not saved, the tour id is not defined, therefore the tour data are provided
	 * from the import view when tours are merged to display the merge layer
	 */
	@Transient
	private TourData											_mergeSourceTourData;

	@Transient
	private DateTime											_dateTimeCreated;

	@Transient
	private DateTime											_dateTimeModified;

	/**
	 * Tour start time
	 */
	@Transient
	private DateTime											_dateTimeStart;

	/**
	 * Tour markers which are sorted by serie index
	 */
	@Transient
	private ArrayList<TourMarker>								_sortedMarkers;

	/**
	 * Contains seconds from all hr zones: {@link #hrZone0} ... {@link #hrZone9}
	 */
	@Transient
	private int[]												_hrZones;

	@Transient
	private HrZoneContext										_hrZoneContext;

	/**
	 * Copy of {@link #timeSerie} with floating type, this is used for the chart x-axis.
	 */
	@Transient
	private double[]											timeSerieDouble;

	/**
	 * Contains photo data from a {@link TourPhotoLink}.
	 * <p>
	 * When this field is set, photos from this photo link are displayed otherwise photos from
	 * {@link #tourPhotos} are displayed.
	 */
	@Transient
	public TourPhotoLink										tourPhotoLink;

	/**
	 * Contains photos which are displayed in photo galleries.
	 */
	@Transient
	private ArrayList<Photo>									_galleryPhotos						= new ArrayList<Photo>();

	/**
	 * 
	 */
	@Transient
	public boolean												isHistoryTour;

	/**
	 * Time serie for history dates, {@link Long} is used instead of {@link Integer} which is used
	 * in {@link #timeSerie} but has a limit of about 67 years {@link Integer#MAX_VALUE}.
	 */
	@Transient
	public long[]												timeSerieHistory;

	/**
	 * Time in double precicion that x-axis values are displayed at the correct position, this is
	 * not the case when max chart pixels is 1'000'000'000 with floating point.
	 */
	@Transient
	private double[]											timeSerieHistoryDouble;

	/**
	 * Contains adjusted time serie when tour is overlapping 1. April 1893. There was a time shift
	 * of 6:32 minutes when CET (Central European Time) was born.
	 */
	@Transient
	private double[]											timeSerieWithTimeZoneAdjustment;

	/**
	 * {@link TourData} contains multiple tours or a virtual tour. It is created when multiple tours
	 * are selected to be displayed in the {@link TourChart}.
	 */
	@Transient
	public boolean												isMultipleTours;

	/**
	 * Contains the tour id's
	 */
	@Transient
	public Long[]												multipleTourIds;

	/**
	 * Contains the tour start index in the data series for each tour.
	 */
	@Transient
	public int[]												multipleTourStartIndex;

	/**
	 * Contains the tour start time for each tour.
	 */
	@Transient
	public long[]												multipleTourStartTime;

	/**
	 * Contains tour titles for each tour.
	 */
	@Transient
	public String[]												multipleTourTitles;

	/**
	 * Contains the number of tour markers for each tour.
	 */
	@Transient
	public int[]												multipleNumberOfMarkers;

	/**
	 * List with all tour markers which is used only for multiple tours. This list is required
	 * because the tour markers cannnot be modified and a Set with all tourmarkers is not sorted as
	 * it should.
	 */
	@Transient
	public ArrayList<TourMarker>								multiTourMarkers;

	@Transient
	public boolean												multipleTour_IsCadenceRpm;

	@Transient
	public boolean												multipleTour_IsCadenceSpm;

	/**
	 * A value is <code>true</code> when cadence is 0.
	 */
	@Transient
	private boolean[]											_cadenceGaps;

	/**
	 * Contains the cadence data serie when the {@link #cadenceMultiplier} != 1.0;
	 */
	@Transient
	private float[]												cadenceSerieWithMultiplier;

	/**
	 * Is <code>true</code> when the tour is imported and contained MT specific fields, e.g. tour
	 * recording time, average temperature, ...
	 */
	@Transient
	private boolean												_isImportedMTTour;

	/**
	 * Is <code>true</code> when tour file is deleted in the device and in the backup folder.
	 */
	@Transient
	public boolean												isTourFileDeleted;

	/**
	 * Is <code>true</code> when the tour file is deleted in the device folder but is kept in the
	 * backup folder.
	 */
	@Transient
	public boolean												isTourFileMoved;

	/**
	 * Is <code>true</code> when the tour import file existed in the backup folder and not in the
	 * device folder.
	 * <p>
	 * <b>THIS FILE SHOULD NOT BE DELETED.</b>
	 */
	@Transient
	public boolean												isBackupImportFile;

	public TourData() {}

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

		double mapMinLatitude = 0;
		double mapMaxLatitude = 0;
		double mapMinLongitude = 0;
		double mapMaxLongitude = 0;

		// get FIRST VALID latitude/longitude
		boolean isGPS = false;
		if ((latitudeSerie != null) && (longitudeSerie != null)) {

			for (int timeIndex = 0; timeIndex < timeSerie.length; timeIndex++) {
				if ((latitudeSerie[timeIndex] != Double.MIN_VALUE) && (longitudeSerie[timeIndex] != Double.MIN_VALUE)) {

					mapMinLatitude = mapMaxLatitude = latitudeSerie[timeIndex] + 90;
					mapMinLongitude = mapMaxLongitude = longitudeSerie[timeIndex] + 180;

					isGPS = true;

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
					// remove invalid values which are set temporaritly
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

			if (isGPS) {

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
				 * check if latitude is not 0, there was a bug until version 1.3.0 where latitude
				 * and longitude has been saved with 0 values
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
		}
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
		speedSerieImperial = null;

		if (isPowerSerieFromDevice == false) {
			powerSerie = null;
		}

		timeSerieDouble = null;
		timeSerieWithTimeZoneAdjustment = null;
		distanceSerieDouble = null;
		distanceSerieDoubleImperial = null;

		breakTimeSerie = null;

		pulseSerieSmoothed = null;
		gradientSerie = null;

		paceSerieSeconds = null;
		paceSerieSecondsImperial = null;
		paceSerieMinute = null;
		paceSerieMinuteImperial = null;

		altimeterSerie = null;
		altimeterSerieImperial = null;

		altitudeSerieSmoothed = null;
		altitudeSerieImperial = null;
		altitudeSerieImperialSmoothed = null;

		cadenceSerieWithMultiplier = null;

		srtmSerie = null;
		srtmSerieImperial = null;

		_gpsBounds = null;

		_hrZones = null;
		_hrZoneContext = null;

		_gears = null;

		_cadenceGaps = null;
	}

	/**
	 * clears the cached world positions, this is necessary when the data serie have been modified
	 */
	public void clearWorldPositions() {

		_tourWorldPosition.clear();
	}

	@Override
	public int compareTo(final Object obj) {

		if (obj instanceof TourData) {

			final TourData otherTourData = (TourData) obj;

			final long tourStartTime2 = otherTourData.tourStartTime;

			return tourStartTime > tourStartTime2 ? 1 : tourStartTime < tourStartTime2 ? -1 : 0;

//			return startYear < otherTourData.startYear ? -1 : startYear == otherTourData.startYear
//					? startMonth < otherTourData.startMonth ? -1 : startMonth == otherTourData.startMonth
//							? startDay < otherTourData.startDay ? -1 : startDay == otherTourData.startDay
//									? startHour < otherTourData.startHour ? -1 : startHour == otherTourData.startHour
//											? startMinute < otherTourData.startMinute
//													? -1
//													: startSecond == otherTourData.startSecond //
//															? 0
//															: 1 //
//											: 1 //
//									: 1 //
//							: 1 //
//					: 1;
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

		if (_prefStore.getString(ITourbookPreferences.GRAPH_SMOOTHING_SMOOTHING_ALGORITHM)//
				.equals(ISmoothingAlgorithm.SMOOTHING_ALGORITHM_JAMET)) {

			computeSmoothedDataSeries();

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

		final float dataSerieAltimeter[] = new float[serieLength];
		final float dataSerieGradient[] = new float[serieLength];

		int adjustIndexLow;
		int adjustmentIndexHigh;

//		if (prefStore.getBoolean(ITourbookPreferences.GRAPH_PROPERTY_IS_VALUE_COMPUTING)) {
//
//			// use custom settings to compute altimeter and gradient
//
//			final int computeTimeSlice = prefStore.getInt(ITourbookPreferences.GRAPH_PROPERTY_CUSTOM_VALUE_TIMESLICE);
//			final int slices = computeTimeSlice / deviceTimeInterval;
//
//			final int slice2 = slices / 2;
//			adjustmentIndexHigh = (1 >= slice2) ? 1 : slice2;
//			adjustIndexLow = slice2;
//
//			// round up
//			if (adjustIndexLow + adjustmentIndexHigh < slices) {
//				adjustmentIndexHigh++;
//			}
//
//		} else {

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
//		}

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
			dataSerieAltimeter[serieIndex] = 3600 * altitudeDiff / timeDiff / UI.UNIT_VALUE_ALTITUDE;

			// keep gradient data
			dataSerieGradient[serieIndex] = distanceDiff == 0 ? 0 : altitudeDiff * 100 / distanceDiff;
		}

		if (UI.UNIT_VALUE_ALTITUDE != 1) {

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

		final float dataSerieAltimeter[] = new float[serieLength];
		final float dataSerieGradient[] = new float[serieLength];

		// get minimum time/distance differences
		final int minTimeDiff = _prefStore.getInt(ITourbookPreferences.APP_DATA_SPEED_MIN_TIMESLICE_VALUE);

//		if (isCustomProperty) {
//			// use custom settings to compute altimeter and gradient
//			minTimeDiff = prefStore.getInt(ITourbookPreferences.GRAPH_PROPERTY_CUSTOM_VALUE_TIMESLICE);
//		} else {
//			// use internal algorithm to compute altimeter and gradient
//			minTimeDiff = 16;
//		}

		final int minDistanceDiff = minTimeDiff;

		final boolean checkPosition = (latitudeSerie != null) && (longitudeSerie != null);

		for (int serieIndex = 1; serieIndex < serieLength; serieIndex++) {

			if (checkSpeedSerie[serieIndex] == 0) {
				// continue when no speed is available
//				dataSerieAltimeter[serieIndex] = 2000;
				continue;
			}

			final int sliceTimeDiff = timeSerie[serieIndex] - timeSerie[serieIndex - 1];

			// check if a lat and long diff is available
			if (checkPosition && (serieIndex > 0) && (serieIndex < serieLengthLast - 1)) {

				if (sliceTimeDiff > 10) {

					if ((latitudeSerie[serieIndex] == latitudeSerie[serieIndex - 1])
							&& (longitudeSerie[serieIndex] == longitudeSerie[serieIndex - 1])) {
//						dataSerieAltimeter[serieIndex] = 100;
						continue;
					}

					if (distanceSerie[serieIndex] == distanceSerie[serieIndex - 1]) {
//						dataSerieAltimeter[serieIndex] = 120;
						continue;
					}

					if (altitudeSerie[serieIndex] == altitudeSerie[serieIndex - 1]) {
//						dataSerieAltimeter[serieIndex] = 130;
						continue;
					}
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
			 * check if a time difference is available between 2 time data, this can happen in gps
			 * data that lat+long is available but no time
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
//					dataSerieAltimeter[serieIndex] = 300;
					continue;
				}

				// check if lat and long diff is available
				if (checkPosition && (lowIndex > 0) && (highIndex < serieLengthLast - 1)) {

					if (sliceTimeDiff > 10) {

						if ((latitudeSerie[lowIndex] == latitudeSerie[lowIndex - 1])
								&& (longitudeSerie[lowIndex] == longitudeSerie[lowIndex - 1])) {
//							dataSerieAltimeter[serieIndex] = 210;
							continue;
						}
						if ((latitudeSerie[highIndex] == latitudeSerie[highIndex + 1])
								&& (longitudeSerie[highIndex] == longitudeSerie[highIndex + 1])) {
//							dataSerieAltimeter[serieIndex] = 220;
							continue;
						}
					}
				}

				// compute altimeter
				if (timeDiff > 0) {
					final float altimeter = 3600f * altitudeDiff / timeDiff / UI.UNIT_VALUE_ALTITUDE;
					dataSerieAltimeter[serieIndex] = altimeter;
				} else {
//					dataSerieAltimeter[serieIndex] = -100;
				}

				// compute gradient
				if (distanceDiff > 0) {
					final float gradient = altitudeDiff * 100 / distanceDiff;
					dataSerieGradient[serieIndex] = gradient;
				} else {
//					dataSerieAltimeter[serieIndex] = -200;
				}

			} else {
//				dataSerieAltimeter[serieIndex] = -300;
			}
		}

		if (UI.UNIT_VALUE_ALTITUDE != 1) {

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

		float prefDPTolerance;

		if (_isImportedMTTour) {
			// use imported value
			prefDPTolerance = dpTolerance / 10;
		} else {
			prefDPTolerance = _prefStore.getFloat(ITourbookPreferences.COMPUTED_ALTITUDE_DP_TOLERANCE);
		}

		AltitudeUpDown altiUpDown;
		if (distanceSerie != null) {

			// DP needs distance

			altiUpDown = computeAltitudeUpDown_20_Algorithm_DP(prefDPTolerance);

			// keep this value to see in the UI (toursegmenter) the value and how it is computed
			dpTolerance = (short) (prefDPTolerance * 10);

		} else {

			altiUpDown = computeAltitudeUpDown_30_Algorithm_9_08(null, prefDPTolerance);
		}

		if (altiUpDown == null) {
			return false;
		}

		setTourAltUp(altiUpDown.altitudeUp);
		setTourAltDown(altiUpDown.altitudeDown);

		return true;
	}

	public AltitudeUpDown computeAltitudeUpDown(final ArrayList<AltitudeUpDownSegment> segmentSerieIndexParameter,
												final float selectedMinAltiDiff) {

		return computeAltitudeUpDown_30_Algorithm_9_08(segmentSerieIndexParameter, selectedMinAltiDiff);
	}

	/**
	 * Compute altitude up/down with Douglas Peuker algorithm.
	 * 
	 * @param dpTolerance
	 * @return Returns <code>null</code> when altitude up/down cannot be computed
	 */
	private AltitudeUpDown computeAltitudeUpDown_20_Algorithm_DP(final float dpTolerance) {

		// check if all necessary data are available
		if (altitudeSerie == null || altitudeSerie.length < 2) {
			return null;
		}

		// convert data series into DP points
		final DPPoint dpPoints[] = new DPPoint[distanceSerie.length];
		for (int serieIndex = 0; serieIndex < dpPoints.length; serieIndex++) {
			dpPoints[serieIndex] = new DPPoint(distanceSerie[serieIndex], altitudeSerie[serieIndex], serieIndex);
		}

		int[] forcedIndices = null;
		if (isMultipleTours) {
			forcedIndices = multipleTourStartIndex;
		}

		final DPPoint[] simplifiedPoints = new DouglasPeuckerSimplifier(dpTolerance, dpPoints, forcedIndices)
				.simplify();

		float altitudeUpTotal = 0;
		float altitudeDownTotal = 0;

		float prevAltitude = altitudeSerie[0];

		/*
		 * Get altitude up/down from the tour altitude values which are found by DP
		 */
		for (int dbIndex = 1; dbIndex < simplifiedPoints.length; dbIndex++) {

			final DPPoint point = simplifiedPoints[dbIndex];
			final float currentAltitude = altitudeSerie[point.serieIndex];
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
	 * This algorithm is abandond because it can cause very wrong values dependend on the terrain.
	 * DP is the preferred algorithm since 14.7.
	 * 
	 * @param segmentSerie
	 *            segments are created for each gradient alternation when segmentSerie is not
	 *            <code>null</code>
	 * @param minAltiDiff
	 * @return Returns <code>null</code> when altitude up/down cannot be computed
	 */
	private AltitudeUpDown computeAltitudeUpDown_30_Algorithm_9_08(	final ArrayList<AltitudeUpDownSegment> segmentSerie,
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
					 * compares with equal 0 (== 0) to prevent initialization error, otherwise the
					 * value is >0 or <0
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

								segmentSerie.add(new AltitudeUpDownSegment(currentSegmentSerieIndex, //
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
					 * compares to == 0 to prevent initialization error, otherwise the value is >0
					 * or <0
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

		for (int serieIndex = firstIndex; serieIndex <= lastIndex; serieIndex++) {

			if (hasBreakTime) {

				/*
				 * break time requires distance data, so it's possible that break time data are not
				 * available
				 */

				if (breakTimeSerie[serieIndex] == true) {

					// break has occured in this time slice

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

			final float cadence = cadenceSerie[serieIndex];

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

	public void computeAvg_Pulse() {

		if ((pulseSerie == null) || (pulseSerie.length == 0) || (timeSerie == null) || (timeSerie.length == 0)) {
			return;
		}

		avgPulse = computeAvg_PulseSegment(0, timeSerie.length - 1);
	}

	public float computeAvg_PulseSegment(final int firstIndex, final int lastIndex) {

		// check if data are available
		if ((pulseSerie == null) || (pulseSerie.length == 0) || (timeSerie == null) || (timeSerie.length == 0)) {
			return 0;
		}

		if (pulseSerieSmoothed == null) {
			computePulseSmoothed();
		}

		// check for 1 point
		if (firstIndex == lastIndex) {
			return pulseSerieSmoothed[firstIndex];
		}

		// check for 2 points
		if (lastIndex - firstIndex == 1) {
			return (pulseSerieSmoothed[firstIndex] + pulseSerieSmoothed[lastIndex]) / 2;
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

					// break has occured in this time slice

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

			final float pulse = pulseSerieSmoothed[serieIndex];

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

	public void computeAvg_Temperature() {

		if (temperatureSerie == null) {
			return;
		}

		float temperatureSum = 0;
		int tempLength = temperatureSerie.length;

		for (final float temperature : temperatureSerie) {
			if (temperature == Float.MIN_VALUE) {
				// ignore invalid values
				tempLength--;
			} else {
				temperatureSum += temperature;
			}
		}

		if (tempLength > 0) {
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
	 * calculate the driving time, ignore the time when the distance is 0 within a time period which
	 * is defined by <code>sliceMin</code>
	 * 
	 * @param minSlices
	 *            A break will occure when the distance will not change within the minimum number of
	 *            time slices.
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
	 * @param startIndex
	 * @param endIndex
	 * @param btConfig
	 * @return Returns break time for the whole tour.
	 */
	private int computeBreakTimeVariable(final int startIndex, final int endIndex, final BreakTimeTool btConfig) {

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
	 * Compute maximum and average fields.
	 */
	public void computeComputedValues() {

		computePulseSmoothed();
		computeSmoothedDataSeries();

		computeMaxAltitude();
		computeMaxPulse();
		computeMaxSpeed();

		computeAvg_Pulse();
		computeAvg_Cadence();
		computeAvg_Temperature();

		computeHrZones();
	}

	private GeoPosition[] computeGeoBounds() {

		if ((latitudeSerie == null) || (longitudeSerie == null)) {
			return null;
		}

		/*
		 * get min/max longitude/latitude
		 */

		double minLatitude = latitudeSerie[0];
		double maxLatitude = latitudeSerie[0];
		double minLongitude = longitudeSerie[0];
		double maxLongitude = longitudeSerie[0];

		for (int serieIndex = 1; serieIndex < latitudeSerie.length; serieIndex++) {

			final double latitude = latitudeSerie[serieIndex];
			final double longitude = longitudeSerie[serieIndex];

			minLatitude = latitude < minLatitude ? latitude : minLatitude;
			maxLatitude = latitude > maxLatitude ? latitude : maxLatitude;

			minLongitude = longitude < minLongitude ? longitude : minLongitude;
			maxLongitude = longitude > maxLongitude ? longitude : maxLongitude;

			if (minLatitude == 0) {
				minLatitude = -180.0;
			}
		}

		final GeoPosition[] gpsBounds = new GeoPosition[] {
				new GeoPosition(minLatitude, minLongitude),
				new GeoPosition(maxLatitude, maxLongitude) };

		return gpsBounds;
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

		if (pulseSerieSmoothed == null) {
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

			final float pulse = pulseSerieSmoothed[serieIndex];
			final int time = timeSerie[serieIndex];

			final int timeDiff = time - prevTime;
			prevTime = time;

			// check if a break occured, break time is ignored
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

		_hrZones = new int[] { hrZone0, //
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
			computeSmoothedDataSeries();
		}

		// double check was necessary because this case occured but it should not
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

		if (pulseSerieSmoothed == null) {
			computePulseSmoothed();
		}

		float maxPulse = 0;

		for (final float pulse : pulseSerieSmoothed) {
			if (pulse > maxPulse) {
				maxPulse = pulse;
			}
		}

		this.maxPulse = maxPulse;
	}

	private void computeMaxSpeed() {
		if (distanceSerie != null) {
			computeSmoothedDataSeries();
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

		if (pulseSerieSmoothed != null) {
			return;
		}

		final boolean isInitialAlgorithm = _prefStore.getString(
				ITourbookPreferences.GRAPH_SMOOTHING_SMOOTHING_ALGORITHM).equals(
				ISmoothingAlgorithm.SMOOTHING_ALGORITHM_INITIAL);

		if (isInitialAlgorithm) {

			// smoothing is disabled for pulse values
			pulseSerieSmoothed = Arrays.copyOf(pulseSerie, pulseSerie.length);

			return;
		}

		final boolean isPulseSmoothed = _prefStore.getBoolean(ITourbookPreferences.GRAPH_JAMET_SMOOTHING_IS_PULSE);

		if (isPulseSmoothed == false) {

			// pulse is not smoothed
			pulseSerieSmoothed = Arrays.copyOf(pulseSerie, pulseSerie.length);

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

		pulseSerieSmoothed = new float[size];

		// convert double into float
		for (int serieIndex = 0; serieIndex < size; serieIndex++) {
			pulseSerieSmoothed[serieIndex] = (float) heart_rate_sc[serieIndex];
		}
	}

	/**
	 * Compute smoothed data series which depend on the distance, this is speed, pace, gradient and
	 * altimeter.<br>
	 * Additionally the altitude smoothed data series is computed.
	 * <p>
	 * This smoothing is based on the algorithm from Didier Jamet.
	 */
	private void computeSmoothedDataSeries() {

		// check if the tour was created manually
		if (timeSerie == null) {
			return;
		}

		// check if smoothed data are already computed
		if (speedSerie != null) {
			return;
		}

		final int size = timeSerie.length;

		final double altitude[] = new double[size];
		final double altitude_sc[] = new double[size];

		final double tauGradient = _prefStore.getDouble(ITourbookPreferences.GRAPH_JAMET_SMOOTHING_GRADIENT_TAU);
		final double tauSpeed = _prefStore.getDouble(ITourbookPreferences.GRAPH_JAMET_SMOOTHING_SPEED_TAU);

		final int repeatedSmoothing = _prefStore.getInt(ITourbookPreferences.GRAPH_JAMET_SMOOTHING_REPEATED_SMOOTHING);
		final double repeatedTau = _prefStore.getDouble(ITourbookPreferences.GRAPH_JAMET_SMOOTHING_REPEATED_TAU);

		/*
		 * smooth altitude
		 */
		final boolean isAltitudeAvailable = altitudeSerie != null;

		if (isAltitudeAvailable) {

			final boolean isAltitudeSmoothed = _prefStore.getBoolean(//
					ITourbookPreferences.GRAPH_JAMET_SMOOTHING_IS_ALTITUDE);

			// convert altitude into double
			for (int serieIndex = 0; serieIndex < size; serieIndex++) {
				altitude[serieIndex] = altitudeSerie[serieIndex];
			}

			// altitude MUST be smoothed because the values are used in the vertical speed
			Smooth.smoothing(timeSerie, altitude, altitude_sc, tauGradient, false, repeatedSmoothing, repeatedTau);

			altitudeSerieSmoothed = new float[size];
			altitudeSerieImperialSmoothed = new float[size];

			gradientSerie = new float[size];

			altimeterSerie = new float[size];
			altimeterSerieImperial = new float[size];

			if (isAltitudeSmoothed) {

				for (int serieIndex = 0; serieIndex < size; serieIndex++) {
					altitudeSerieSmoothed[serieIndex] = (float) altitude_sc[serieIndex];
					altitudeSerieImperialSmoothed[serieIndex] = (float) (altitude_sc[serieIndex] / UI.UNIT_VALUE_ALTITUDE);
				}
			} else {

				// altitude is NOT smoothed, copy original values

				for (int serieIndex = 0; serieIndex < size; serieIndex++) {
					altitudeSerieSmoothed[serieIndex] = altitudeSerie[serieIndex];
					altitudeSerieImperialSmoothed[serieIndex] = (altitudeSerie[serieIndex] / UI.UNIT_VALUE_ALTITUDE);
				}
			}
		}

		// check if required data for speed, gradient... are available
		if (distanceSerie == null && (latitudeSerie == null || longitudeSerie == null)) {
			return;
		}

		speedSerie = new float[size];
		speedSerieImperial = new float[size];

		paceSerieSeconds = new float[size];
		paceSerieSecondsImperial = new float[size];
		paceSerieMinute = new float[size];
		paceSerieMinuteImperial = new float[size];

		// ensure data series are created to prevent exceptions
		if (size < 2) {
			return;
		}

		final double[] distance = new double[size];
		final double[] distance_sc = new double[size];

		final double Vh_ini[] = new double[size];
		final double Vh[] = new double[size];
		final double Vh_sc[] = new double[size];

		final double Vv_ini[] = new double[size];
		final double Vv[] = new double[size];
		final double Vv_sc[] = new double[size];

		/*
		 * get distance
		 */
		if (distanceSerie == null) {

			// compute distance from latitude and longitude data
			distance[0] = 0.;
			for (int serieIndex = 1; serieIndex < size; serieIndex++) {
				distance[serieIndex] = distance[serieIndex - 1]
						+ MtMath.distanceVincenty(
								latitudeSerie[serieIndex],
								latitudeSerie[serieIndex - 1],
								longitudeSerie[serieIndex],
								longitudeSerie[serieIndex - 1]);
			}

		} else {

			// convert distance into double
			for (int serieIndex = 0; serieIndex < size; serieIndex++) {
				distance[serieIndex] = distanceSerie[serieIndex];
			}
		}

		/*
		 * Compute the horizontal and vertical speeds from the raw distance and altitude data
		 */
		for (int serieIndex = 0; serieIndex < size - 1; serieIndex++) {

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

				if (isAltitudeAvailable) {
					Vv_ini[serieIndex] = (altitude[serieIndex + 1] - altitude[serieIndex])
							/ (timeSerie[serieIndex + 1] - timeSerie[serieIndex]);
				}
			}
		}
		Vh_ini[size - 1] = Vh_ini[size - 2];
		Vv_ini[size - 1] = Vv_ini[size - 2];

		/*
		 * Smooth out the time variations of the distance
		 */
		Smooth.smoothing(timeSerie, distance, distance_sc, tauSpeed, false, repeatedSmoothing, repeatedTau);

		/*
		 * Compute the horizontal and vertical speeds from the smoothed distance and altitude
		 */
		for (int serieIndex = 0; serieIndex < size - 1; serieIndex++) {

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

				if (isAltitudeAvailable) {
					Vv[serieIndex] = (altitude_sc[serieIndex + 1] - altitude_sc[serieIndex])
							/ (timeSerie[serieIndex + 1] - timeSerie[serieIndex]);
				}
			}
		}
		Vh[size - 1] = Vh[size - 2];
		Vv[size - 1] = Vv[size - 2];

		/*
		 * Smooth out the time variations of the horizontal and vertical speeds
		 */
		Smooth.smoothing(timeSerie, Vh, Vh_sc, tauSpeed, false, repeatedSmoothing, repeatedTau);
		if (isAltitudeAvailable) {
			Smooth.smoothing(timeSerie, Vv, Vv_sc, tauGradient, false, repeatedSmoothing, repeatedTau);
		}

		/*
		 * Compute the terrain slope
		 */
		if (isAltitudeAvailable) {

			for (int serieIndex = 0; serieIndex < size; serieIndex++) {

				final double vh_sc_Value = Vh_sc[serieIndex];

				// check divide by 0
				gradientSerie[serieIndex] = vh_sc_Value == 0.0 ? //
						0
						: (float) (Vv_sc[serieIndex] / vh_sc_Value * 100.0);

				final double vSpeedSmoothed = Vv_sc[serieIndex] * 3600.0;
				altimeterSerie[serieIndex] = (float) (vSpeedSmoothed);
				altimeterSerieImperial[serieIndex] = (float) (vSpeedSmoothed / UI.UNIT_VALUE_ALTITUDE);
			}
		}

		maxSpeed = 0.0f;
		for (int serieIndex = 0; serieIndex < Vh.length; serieIndex++) {

			final double speedMetric = Vh[serieIndex] * 3.6;
			final double speedImperial = speedMetric / UI.UNIT_MILE;

			if (speedMetric > maxSpeed) {
				maxSpeed = (float) speedMetric;
			}

			speedSerie[serieIndex] = (float) speedMetric;
			speedSerieImperial[serieIndex] = (float) speedImperial;

			final float paceMetricSeconds = speedMetric < 1.0 ? 0 : (float) (3600.0 / speedMetric);
			final float paceImperialSeconds = speedMetric < 0.6 ? 0 : (float) (3600.0 / speedImperial);

			paceSerieSeconds[serieIndex] = paceMetricSeconds;
			paceSerieSecondsImperial[serieIndex] = paceImperialSeconds;

			paceSerieMinute[serieIndex] = paceMetricSeconds / 60;
			paceSerieMinuteImperial[serieIndex] = paceImperialSeconds / 60;
		}
	}

	/**
	 * computes the speed data serie which can be retrieved with {@link TourData#getSpeedSerie()}
	 */
	public void computeSpeedSerie() {

//		final long start = System.nanoTime();

		if ((speedSerie != null)
				&& (speedSerieImperial != null)
				&& (paceSerieSeconds != null)
				&& (paceSerieSecondsImperial != null)
				&& (paceSerieMinute != null)
				&& (paceSerieMinuteImperial != null)) {
			return;
		}

		if (isSpeedSerieFromDevice) {

			// speed is from the device

			computeSpeedSerieFromDevice();

		} else {

			// speed is computed from distance and time

			if (_prefStore.getString(ITourbookPreferences.GRAPH_SMOOTHING_SMOOTHING_ALGORITHM)//
					.equals(ISmoothingAlgorithm.SMOOTHING_ALGORITHM_JAMET)) {

				computeSmoothedDataSeries();

			} else {

				if (deviceTimeInterval == -1) {
					computeSpeedSerieInternalWithVariableInterval();
				} else {
					computeSpeedSerieInternalWithFixedInterval();
				}
			}
		}

//		final long end = System.nanoTime();
//
//		System.out.println("computeSpeedSerie():\t" + ((end - start) / 1000000.0) + "ms");
	}

	/**
	 * Computes the imperial speed data serie and max speed
	 * 
	 * @return
	 */
	private void computeSpeedSerieFromDevice() {

		if (speedSerie == null) {
			return;
		}

		final int serieLength = speedSerie.length;

		speedSerieImperial = new float[serieLength];

		maxSpeed = 0;

		for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

			/*
			 * speed
			 */

			final float speedMetric = speedSerie[serieIndex];

			speedSerieImperial[serieIndex] = speedMetric / UI.UNIT_MILE;
			maxSpeed = Math.max(maxSpeed, speedMetric);
		}
	}

	/**
	 * Computes the speed data serie with the internal algorithm for a fix time interval
	 * 
	 * @return
	 */
	private void computeSpeedSerieInternalWithFixedInterval() {

		// distance is required
		if (distanceSerie == null) {
			return;
		}

		final int serieLength = timeSerie.length;

		maxSpeed = 0;

		speedSerie = new float[serieLength];
		speedSerieImperial = new float[serieLength];

		paceSerieSeconds = new float[serieLength];
		paceSerieSecondsImperial = new float[serieLength];
		paceSerieMinute = new float[serieLength];
		paceSerieMinuteImperial = new float[serieLength];

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

		final int serieLengthLast = serieLength - 1;

		for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

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
			float speedImperial = 0;
			if (timeDiff != 0) {
				final float speed = (distDiff * 3.6f) / timeDiff;
				speedMetric = speed;
				speedImperial = speed / UI.UNIT_MILE;
			}

			speedSerie[serieIndex] = speedMetric;
			speedSerieImperial[serieIndex] = speedImperial;

			maxSpeed = Math.max(maxSpeed, speedMetric);

			final float paceMetricSeconds = speedMetric < 1.0 ? 0 : (float) (3600.0 / speedMetric);
			final float paceImperialSeconds = speedMetric < 0.6 ? 0 : (float) (3600.0 / speedImperial);

			paceSerieSeconds[serieIndex] = paceMetricSeconds;
			paceSerieSecondsImperial[serieIndex] = paceImperialSeconds;

			paceSerieMinute[serieIndex] = paceMetricSeconds / 60;
			paceSerieMinuteImperial[serieIndex] = paceImperialSeconds / 60;
		}
	}

	/**
	 * compute the speed when the time serie has unequal time intervalls, with Wolfgangs algorithm
	 */
	private void computeSpeedSerieInternalWithVariableInterval() {

		// distance is required
		if (distanceSerie == null) {
			return;
		}

		final int minTimeDiff = _prefStore.getInt(ITourbookPreferences.APP_DATA_SPEED_MIN_TIMESLICE_VALUE);

		final int serieLength = timeSerie.length;
		final int lastSerieIndex = serieLength - 1;

		maxSpeed = 0;

		speedSerie = new float[serieLength];
		speedSerieImperial = new float[serieLength];

		paceSerieSeconds = new float[serieLength];
		paceSerieSecondsImperial = new float[serieLength];
		paceSerieMinute = new float[serieLength];
		paceSerieMinuteImperial = new float[serieLength];

		final boolean isUseLatLon = (latitudeSerie != null) && //
				(longitudeSerie != null)
				&& (isDistanceFromSensor == 0); // --> distance is measured with the gps device and not from a sensor

		boolean isLatLongEqual = false;
		int equalStartIndex = 0;

		for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

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
					 * lat/long equality ended, compute distance for all datapoints which has the
					 * same lat/long because this was not correctly computed from the device
					 */

					isLatLongEqual = false;

					final int equalTimeDiff = timeSerie[serieIndex] - timeSerie[equalStartIndex];
					final float equalDistDiff = distanceSerie[serieIndex] - distanceSerie[equalStartIndex];

					float speedMetric = 0;
					float speedImperial = 0;

					if ((equalTimeDiff > 20) && (equalDistDiff < 10)) {
						// speed must be greater than 1.8 km/h
					} else {

						for (int equalSerieIndex = equalStartIndex + 1; equalSerieIndex < serieIndex; equalSerieIndex++) {

							final int equalSegmentTimeDiff = timeSerie[equalSerieIndex]
									- timeSerie[equalSerieIndex - 1];

							final float equalSegmentDistDiff = equalTimeDiff == 0 ? 0 : //
									(float) equalSegmentTimeDiff / equalTimeDiff * equalDistDiff;

							distanceSerie[equalSerieIndex] = distanceSerie[equalSerieIndex - 1] + equalSegmentDistDiff;

							// compute speed for this segment
							if ((equalSegmentTimeDiff == 0) || (equalSegmentDistDiff == 0)) {
								speedMetric = 0;
							} else {
								speedMetric = ((equalSegmentDistDiff * 3.6f) / equalSegmentTimeDiff);
								speedMetric = speedMetric < 0 ? 0 : speedMetric;

								speedImperial = equalSegmentDistDiff * 3.6f / (equalSegmentTimeDiff * UI.UNIT_MILE);
								speedImperial = speedImperial < 0 ? 0 : speedImperial;
							}

							setSpeed(
									equalSerieIndex,
									speedMetric,
									speedImperial,
									equalSegmentTimeDiff,
									equalSegmentDistDiff);
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
				if ((lowIndex < 0) || (highIndex >= serieLength)) {
					break;
				}

				timeDiff = timeSerie[highIndex] - timeSerie[lowIndex];
				distDiff = distanceSerie[highIndex] - distanceSerie[lowIndex];
			}

			/*
			 * speed
			 */
			float speedMetric = 0;
			float speedImperial = 0;

			/*
			 * check if a time difference is available between 2 time data, this can happen in gps
			 * data that lat+long is available but no time
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
							&& (longitudeSerie[lowIndex] == longitudeSerie[lowIndex - 1])) {

						if (distDiff == 0) {
							continue;
						}
					}

					if ((longitudeSerie[highIndex] == longitudeSerie[highIndex + 1])
							&& (latitudeSerie[highIndex] == latitudeSerie[highIndex + 1])) {
						if (distDiff == 0) {
							continue;
						}
					}
				}

				if ((timeDiff > 20) && (distDiff < 10)) {
					// speed must be greater than 1.8 km/h
					speedMetric = 0;
				} else {
					speedMetric = distDiff * 3.6f / timeDiff;
					speedMetric = speedMetric < 0 ? 0 : speedMetric;

					speedImperial = distDiff * 3.6f / (timeDiff * UI.UNIT_MILE);
					speedImperial = speedImperial < 0 ? 0 : speedImperial;
				}
			}

			setSpeed(serieIndex, speedMetric, speedImperial, timeDiff, distDiff);
		}
	}

	/**
	 * Computes the tour driving time in seconds, this is the tour recording time - tour break time.
	 * This value is store in {@link #tourDrivingTime}.
	 */
	public void computeTourDrivingTime() {

		if (isManualTour()) {
			// manual tours do not have data series
			return;
		}

		if (_isImportedMTTour) {
			// these types of tour are setting the driving time
			return;
		}

		if ((timeSerie == null) || (timeSerie.length == 0)) {
			tourDrivingTime = 0;
		} else {
			final int tourDrivingTimeRaw = timeSerie[timeSerie.length - 1] - getBreakTime();
			tourDrivingTime = Math.max(0, tourDrivingTimeRaw);
		}
	}

	private float[] convertDataSeries(final int[] intDataSerie, final int scale) {

		if (intDataSerie == null) {
			return null;
		}

		final float[] floatDataSerie = new float[intDataSerie.length];

		for (int serieIndex = 0; serieIndex < intDataSerie.length; serieIndex++) {

			final int intValue = intDataSerie[serieIndex];

			floatDataSerie[serieIndex] = scale > 0 ? //
					(float) intValue / scale
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

		final int timeDiffRange = 1000;
		final double posDiffRange = 0.00000001;

		final ArrayList<TourWayPoint> removedWayPoints = new ArrayList<TourWayPoint>();

		for (final TourWayPoint wp : tourWayPoints) {

			final long wpTime = wp.getTime();
			final double wpLat = wp.getLatitude();
			final double wpLon = wp.getLongitude();

			for (int serieIndex = 0; serieIndex < timeSerie.length; serieIndex++) {

				final int relativeTime = timeSerie[serieIndex];
				final long tourTime = tourStartTime + relativeTime * 1000;

				long timeDiff = tourTime - wpTime;
				if (timeDiff < 0) {
					timeDiff = -timeDiff;
				}

				if (timeDiff < timeDiffRange) {

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

					if (latDiff < posDiffRange && lonDiff < posDiffRange) {

						// time and position is the same

						final TourMarker tourMarker = new TourMarker(this, ChartLabel.MARKER_TYPE_CUSTOM);

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

						break;
					}
				}
			}
		}

		// collapse waypoints
		tourWayPoints.removeAll(removedWayPoints);
	}

	/**
	 * Create {@link Photo}'s from {@link TourPhoto}'s
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
			 * when a photo is in the photo cache it is possible that the tour is from the file
			 * system, update tour relevant fields
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
		long recordingTime = 0;

		long tourStartTime = 0;

		// convert data from the tour format into long[] array
		for (int serieIndex = 0; serieIndex < serieSize; serieIndex++) {

			final HistoryData timeData = timeDataSerie[serieIndex];

			final long absoluteTime = timeData.absoluteTime;

			if (serieIndex == 0) {

				// 1st trackpoint

				timeSerieHistory[serieIndex] = 0;
				tourStartTime = absoluteTime;

			} else {

				// 1..Nth trackpoint

				recordingTime = (absoluteTime - tourStartTime) / 1000;
				timeSerieHistory[serieIndex] = (recordingTime);

			}
		}

		tourRecordingTime = recordingTime;

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
	 *            in km/h
	 * @param breakMinSpeed2
	 * @param breakDistance
	 * @return
	 */
	public ArrayList<TourSegment> createSegmenterSegments(final BreakTimeTool btConfig) {

		if ((segmentSerieIndex == null) || (segmentSerieIndex.length < 2)) {

			// at least two points are required to build a segment
			return null;
		}

		final float[] segmenterAltitudeSerie = getAltitudeSmoothedSerie(false);

		final boolean isAltitudeSerie = (segmenterAltitudeSerie != null) && (segmenterAltitudeSerie.length > 0);
		final boolean isCadenceSerie = (cadenceSerie != null) && (cadenceSerie.length > 0);
		final boolean isDistanceSerie = (distanceSerie != null) && (distanceSerie.length > 0);
		final boolean isPulseSerie = (pulseSerie != null) && (pulseSerie.length > 0);

		final float[] localPowerSerie = getPowerSerie();
		final boolean isPowerSerie = (localPowerSerie != null) && (localPowerSerie.length > 0);

		final int segmentSerieLength = segmentSerieIndex.length;

		final ArrayList<TourSegment> tourSegments = new ArrayList<TourSegment>(segmentSerieLength);
		final int firstSerieIndex = segmentSerieIndex[0];

		/*
		 * get start values
		 */
		int timeStart = timeSerie[firstSerieIndex];

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

		final float tourPace = tourDistance == 0 ? //
				0
				: tourDrivingTime * 1000 / (tourDistance * UI.UNIT_VALUE_DISTANCE);

		segmentSerie_Time_Recording = new int[segmentSerieLength];
		segmentSerie_Time_Driving = new int[segmentSerieLength];
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

		int totalTime_Recording = 0;
		int totalTime_Driving = 0;
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

			segment.serieIndex_Start = segmentStartIndex;
			segment.serieIndex_End = segmentEndIndex;

			/*
			 * time
			 */
			final int segmentEndTime = timeSerie[segmentEndIndex];
			final int segmentRecordingTime = segmentEndTime - timeStart;
			final int segmentBreakTime = getBreakTime(segmentStartIndex, segmentEndIndex, btConfig);

			final float segmentDrivingTime = segmentRecordingTime - segmentBreakTime;

			segmentSerie_Time_Recording[segmentIndex] = segment.time_Recording = segmentRecordingTime;
			segmentSerie_Time_Driving[segmentIndex] = segment.time_Driving = (int) segmentDrivingTime;
			segmentSerie_Time_Break[segmentIndex] = segment.time_Break = segmentBreakTime;
			segmentSerie_Time_Total[segmentIndex] = segment.time_Total = timeTotal += segmentRecordingTime;

			totalTime_Recording += segmentRecordingTime;
			totalTime_Driving += segmentDrivingTime;
			totalTime_Break += segmentBreakTime;

			float segmentDistance = 0.0f;

			/*
			 * distance
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
					segmentSerie_Speed[segmentIndex] = segment.speed = segmentDrivingTime == 0.0f ? //
							0.0f
							: segmentDistance / segmentDrivingTime * 3.6f / UI.UNIT_VALUE_DISTANCE;

					// pace
					final float segmentPace = segmentDrivingTime * 1000 / (segmentDistance / UI.UNIT_VALUE_DISTANCE);
					segment.pace = segmentPace;
					segment.pace_Diff = segment.pace - tourPace;
					segmentSerie_Pace[segmentIndex] = segmentPace;

					totalDistance += segmentDistance;
				}
			}

			/*
			 * altitude
			 */
			if (isAltitudeSerie) {

				final float altitudeEnd = segmenterAltitudeSerie[segmentEndIndex];
				final float altitudeDiff = altitudeEnd - altitudeStart;

				final float altiUpDownHour = segmentDrivingTime == 0 //
						? 0
						: (altitudeDiff / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE) / segmentDrivingTime * 3600;

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
			}

			// end point of current segment is the start of the next segment
			timeStart = segmentEndTime;
		}

		/*
		 * Add total segment
		 */
		final float totalSpeed = totalTime_Driving == 0 ? //
				0
				: totalDistance / totalTime_Driving * 3.6f / UI.UNIT_VALUE_DISTANCE;

		final float totalPace = totalDistance == 0 //
				? 0
				: totalTime_Driving * 1000 / (totalDistance / UI.UNIT_VALUE_DISTANCE);

		final TourSegment totalSegment = new TourSegment();

		totalSegment.isTotal = true;

		totalSegment.time_Recording = totalTime_Recording;
		totalSegment.time_Driving = totalTime_Driving;
		totalSegment.time_Break = totalTime_Break;

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

		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {

			@Override
			public void run() {

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
						srtmValue = elevationSRTM3.getElevation(new GeoLat(latitude), new GeoLon(longitude));
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
			}
		});
	}

	/**
	 * Convert {@link TimeData} into {@link TourData} this will be done after data are imported or
	 * transfered.
	 * <p>
	 * The array {@link #timeSerie} is always created even when the time is not available.
	 * 
	 * @param isCreateMarker
	 *            creates markers when <code>true</code>
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

		final boolean isAltitude = setupStartingValues_Altitude(timeDataSerie, isAbsoluteData);
		final boolean isCadence = setupStartingValues_Cadence(timeDataSerie);
		final boolean isDistance = setupStartingValues_Distance(timeDataSerie, isAbsoluteData);
		final boolean isGear = setupStartingValues_Gear(timeDataSerie);
		final boolean isGPS = setupStartingValues_LatLon(timeDataSerie);
		final boolean isPower = setupStartingValues_Power(timeDataSerie);
		final boolean isPulse = setupStartingValues_Pulse(timeDataSerie);
		final boolean isTemperature = setupStartingValues_Temperature(timeDataSerie);

		/*
		 * Speed
		 */
		boolean isSpeed = false;
		if (firstTimeDataItem.speed != Float.MIN_VALUE) {
			speedSerie = new float[serieSize];
			isSpeed = true;

			isSpeedSerieFromDevice = true;
		}

		// time in seconds relative to the tour start
		long recordingTime = 0;

		if (isAbsoluteData) {

			/*
			 * absolute data are available when data are from GPS devices
			 */

			long tourStartTime = 0;
			long lastValidTime = 0;
			long lastValidAbsoluteTime = 0;

			// convert data from the tour format into interger[] arrays
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

					recordingTime = 0;
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
							 * rounding cannot be used because the tour id contains the last value
							 * from the distance serie so rounding creates another tour id
							 * <p>
							 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
							 */
							distanceSerie[serieIndex] = (int) (absoluteDistance);
						}
					}

				} else {

					// 1..n trackpoint

					/*
					 * time: absolute time is checked against last valid time because this case
					 * happened but time can NOT be in the past.
					 */
					if (absoluteTime == Long.MIN_VALUE || absoluteTime < lastValidAbsoluteTime) {
						recordingTime = lastValidTime;
					} else {
						recordingTime = (absoluteTime - tourStartTime) / 1000;
						lastValidAbsoluteTime = absoluteTime;
					}
					timeSerie[serieIndex] = (int) (lastValidTime = recordingTime);

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
							 * rounding cannot be used because the tour id contains the last value
							 * from the distance serie so rounding creates another tour id
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
					altitudeSerie[serieIndex] = (absoluteAltitude == Float.MIN_VALUE || (absoluteAltitude >= Integer.MAX_VALUE))
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
			}

		} else {

			/*
			 * relativ data is available, these data are NOT from GPS devices
			 */

			int distanceAbsolute = 0;
			int altitudeAbsolute = 0;

			// convert data from the tour format into an interger[]
			for (int serieIndex = 0; serieIndex < serieSize; serieIndex++) {

				final TimeData timeData = timeDataSerie[serieIndex];

				final int tdTime = timeData.time;

				// set time
				timeSerie[serieIndex] = (int) (recordingTime += tdTime == Integer.MIN_VALUE ? 0 : tdTime);

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
		tourRecordingTime = recordingTime;
		setTourEndTimeMS();

		if (isGear) {
			// set shift counts
			setGears(gearSerie);
		}

		cleanupDataSeries();

		/*
		 * Try to get distance values from lat/long values, this must be done after the cleanup
		 * which can set distanceSerie = null.
		 */
		if (distanceSerie == null) {
			TourManager.computeDistanceValuesFromGeoPosition(this);
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
		 * Distance is measured with the gps device and not with a sensor. Remove all distance
		 * values which are set but lat/lon is not available, this case can happen when a device is
		 * in a tunnel. Distance values will be interpolited later.
		 */

		final int size = timeSerie.length;

		for (int serieIndex = 0; serieIndex < size; serieIndex++) {
			if (latitudeSerie[serieIndex] == Double.MIN_VALUE) {
				distanceSerie[serieIndex] = Float.MIN_VALUE;
			}
		}
	}

	/**
	 * Because of the current algorithm, the first distance value can be <code>0</code> and the
	 * other values can be {@link Float#MIN_VALUE}.
	 * <p>
	 * When this occures, set all distance values to {@link Float#MIN_VALUE}, that distance values
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

			// this case needs more investigation if it occures
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
			// no data
			return;
		}

		final TIntArrayList pulseTimes = new TIntArrayList(allTimeData.length * 3);

		for (final TimeData timeData : allTimeData) {

			final int[] pulseTimes2 = timeData.pulseTime;

			if (pulseTimes2 != null) {

				for (final int pulseTime : pulseTimes2) {
					if (pulseTime != 0) {

						if (pulseTime == 65535) {
							// ignore, this value occured in daum data
						} else {

							pulseTimes.add(pulseTime);
						}

//						445
//						480
//						470
//						590
//						65535
//						1500
//						620
//						65535
//						615
//						620
//						1225
//						615
//						615
//						65535
//						620
//						65535
//						615
//						610
//						615
//						65535
//						835
//						595
//						600
//						605
//						600
//						590
//						595
//						595
//						605
//						65535
//						595
//						585
//						1165
//						585
//						585
//						580
//						1155
//						575
//						580
//						575
//						575
//						575
					}
				}
			}
		}

		if (pulseTimes.size() > 0) {
			pulseTimeSerie = pulseTimes.toArray();
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
	 *            Unique key to identify a tour, this <b>MUST</b> be an {@link Integer} value.
	 * @return
	 */
	public Long createTourId(final String uniqueKeySuffix) {

//		final String uniqueKey = Integer.toString(Math.abs(getStartDistance()));

		String tourIdKey;

		try {
			/*
			 * this is the default implementation to create a tour id, but on the 5.5.2007 a
			 * NumberFormatException occured so the calculation for the tour id was adjusted
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

				tourId = Long.valueOf(new DateTime().getMillis());
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
	private void createTourMarker(	final TimeData timeData,
									final int serieIndex,
									final int relativeTime,
									final float distanceAbsolute) {

		// create a new marker
		final TourMarker tourMarker = new TourMarker(this, ChartLabel.MARKER_TYPE_DEVICE);

		/*
		 * ??? timeData.marker was added until version 14.9 but I have no idea why this was added
		 * ???
		 */
//		tourMarker.setTime((int) (relativeTime + timeData.marker));
		tourMarker.setTime(relativeTime, tourStartTime);
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
		out.println("Date:			" + startDay + "." + startMonth + "." + startYear); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		out.println("Time:			" + startHour + ":" + startMinute); //$NON-NLS-1$ //$NON-NLS-2$
		out.println("Total distance:		" + getStartDistance()); //$NON-NLS-1$
		// out.println("Distance: " + getDistance());
		out.println("Altitude:		" + getStartAltitude()); //$NON-NLS-1$
		out.println("Pulse:			" + getStartPulse()); //$NON-NLS-1$
		out.println("Offset DD record:	" + offsetDDRecord); //$NON-NLS-1$
	}

	public void dumpTime() {
		final PrintStream out = System.out;

		out.print((getTourRecordingTime() / 3600) + ":" //$NON-NLS-1$
				+ ((getTourRecordingTime() % 3600) / 60)
				+ ":" //$NON-NLS-1$
				+ ((getTourRecordingTime() % 3600) % 60)
				+ "  "); //$NON-NLS-1$
		out.print(getTourDistance());
	}

	public void dumpTourTotal() {

		final PrintStream out = System.out;

		out.println("Tour distance (m):	" + getTourDistance()); //$NON-NLS-1$

		out.println("Tour time:		" //$NON-NLS-1$
				+ (getTourRecordingTime() / 3600)
				+ ":" //$NON-NLS-1$
				+ ((getTourRecordingTime() % 3600) / 60)
				+ ":" //$NON-NLS-1$
				+ (getTourRecordingTime() % 3600)
				% 60);

		out.println("Driving time:		" //$NON-NLS-1$
				+ (getTourDrivingTime() / 3600)
				+ ":" //$NON-NLS-1$
				+ ((getTourDrivingTime() % 3600) / 60)
				+ ":" //$NON-NLS-1$
				+ (getTourDrivingTime() % 3600)
				% 60);

		out.println("Altitude up (m):	" + getTourAltUp()); //$NON-NLS-1$
		out.println("Altitude down (m):	" + getTourAltDown()); //$NON-NLS-1$
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
	 * @return Returns the metric or imperial altimeter serie depending on the active measurement
	 */
	public float[] getAltimeterSerie() {

		if (UI.UNIT_VALUE_ALTITUDE != 1) {

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

		if (UI.UNIT_VALUE_ALTITUDE != 1) {

			// imperial system is used

			if (altitudeSerieImperial == null) {

				// compute imperial altitude

				altitudeSerieImperial = new float[altitudeSerie.length];

				for (int valueIndex = 0; valueIndex < altitudeSerie.length; valueIndex++) {
					altitudeSerieImperial[valueIndex] = altitudeSerie[valueIndex] / UI.UNIT_VALUE_ALTITUDE;
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
//		if (isForceSmoothing) {
//
//			// smooth altitude
//			computeSmoothedDataSeries();
//
//		} else {
//
		if (altitudeSerieSmoothed != null) {

			// return already smoothed altitude values

			if (UI.UNIT_VALUE_ALTITUDE != 1) {

				// imperial system is used

				return altitudeSerieImperialSmoothed;

			} else {
				return altitudeSerieSmoothed;
			}
		}
//		}

		if (altitudeSerieSmoothed == null) {

			// smoothed altitude values are not available
			return getAltitudeSerie();

		} else {

			if (UI.UNIT_VALUE_ALTITUDE != 1) {

				// imperial system is used

				return altitudeSerieImperialSmoothed;

			} else {
				return altitudeSerieSmoothed;
			}
		}
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

	/**
	 * @return Returns the body weight.
	 */
	public float getBodyWeight() {
		return bikerWeight;
	}

	private int getBreakTime() {

		if (timeSerie == null) {
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
	 * Computes break time between start and end index when and when a break occures
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

				computeBreakTimeVariable(startIndex, endIndex, breakTimeTool);

			} else {

				// fixed time slices

				if (deviceTimeInterval == 0) {
					return 0;
				}

				final int minBreakTime = 20;

				computeBreakTimeFixed(minBreakTime / deviceTimeInterval);
			}

			computeTourDrivingTime();
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

	/**
	 * @return Returns cadence data serie which is multiplied with the {@link #cadenceMultiplier}
	 */
	public float[] getCadenceSerie() {

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

		return cadenceSerie;
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
	 * @return Returns {@link DateTime} when the tour was created or <code>null</code> when
	 *         date/time is not available
	 */
	public DateTime getDateTimeCreated() {

		if (_dateTimeCreated != null || dateTimeCreated == 0) {
			return _dateTimeCreated;
		}

		_dateTimeCreated = Util.createDateTimeFromYMDhms(dateTimeCreated);

		return _dateTimeCreated;
	}

	/**
	 * @return Returns {@link DateTime} when the tour was modified or <code>null</code> when
	 *         date/time is not available
	 */
	public DateTime getDateTimeModified() {

		if (_dateTimeModified != null || dateTimeModified == 0) {
			return _dateTimeModified;
		}

		_dateTimeModified = Util.createDateTimeFromYMDhms(dateTimeModified);

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
	 * @return Returns the distance data serie for the current measurement system, this can be
	 *         metric or imperial
	 */
	public double[] getDistanceSerieDouble() {

		if (distanceSerie == null) {
			return null;
		}

		double[] serie;

		final float unitValueDistance = UI.UNIT_VALUE_DISTANCE;

		if (unitValueDistance == 1) {

			// use metric system

			if (distanceSerieDouble == null) {

				distanceSerieDouble = new double[distanceSerie.length];

				for (int valueIndex = 0; valueIndex < distanceSerie.length; valueIndex++) {
					distanceSerieDouble[valueIndex] = distanceSerie[valueIndex];
				}
			}

			serie = distanceSerieDouble;

		} else {

			// use imperial system

			if (distanceSerieDoubleImperial == null) {

				// compute imperial data

				distanceSerieDoubleImperial = new double[distanceSerie.length];

				for (int valueIndex = 0; valueIndex < distanceSerie.length; valueIndex++) {
					distanceSerieDoubleImperial[valueIndex] = distanceSerie[valueIndex] / unitValueDistance;
				}
			}

			serie = distanceSerieDoubleImperial;
		}

		return serie;
	}

	public short getDpTolerance() {
		return dpTolerance;
	}

	public int getFrontShiftCount() {
		return frontShiftCount;
	}

	/**
	 * @return Returns <code>null</code> when tour do not contain photos, otherwise a list of
	 *         {@link Photo}'s is returned.
	 */
	public ArrayList<Photo> getGalleryPhotos() {

		if (tourPhotos.size() == 0) {
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
	 * @return Returns <code>null</code> when gears are not available otherwise it returns gears
	 *         with this format
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

		if (_gpsBounds == null) {
			_gpsBounds = computeGeoBounds();
		}

		return _gpsBounds;
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

		if (tourImportFileName == null || tourImportFileName.length() == 0) {
			return null;
		}

		return tourImportFileName;
	}

	/**
	 * @return Returns the import file path (folder) or <code>null</code> when not available.
	 */
	public String getImportFilePath() {

		if (tourImportFilePath == null || tourImportFilePath.length() == 0) {
			return null;
		}

		return tourImportFilePath;
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

		if (tourImportFilePath == null || tourImportFilePath.length() == 0) {

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
	 * @return Returns the distance serie from the metric system, the distance serie is
	 *         <b>always</b> saved in the database with the metric system
	 */
	public float[] getMetricDistanceSerie() {
		return distanceSerie;
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

		if (UI.UNIT_VALUE_DISTANCE == 1) {

			// use metric system

			if (paceSerieMinute == null) {
				computeSpeedSerie();
			}

			return paceSerieMinute;

		} else {

			// use imperial system

			if (paceSerieMinuteImperial == null) {
				computeSpeedSerie();
			}

			return paceSerieMinuteImperial;
		}
	}

	public float[] getPaceSerieSeconds() {

		if (UI.UNIT_VALUE_DISTANCE == 1) {

			// use metric system

			if (paceSerieSeconds == null) {
				computeSpeedSerie();
			}

			return paceSerieSeconds;

		} else {

			// use imperial system

			if (paceSerieSecondsImperial == null) {
				computeSpeedSerie();
			}

			return paceSerieSecondsImperial;
		}
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
			computeSmoothedDataSeries();
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
		final float cD = 0.8f;// Streomungskoeffizient
		final float p = 1.145f; // 20C / 400m
//		float p = 0.968f; // 10C / 2000m

		final float weightTotal = weightBody + weightBike;
		final float bsa = (float) (0.007184f * Math.pow(weightBody, 0.425) * Math.pow(bodyHeight, 0.725));
		final float aP = bsa * 0.185f;

		final float roll = weightTotal * 9.81f * cR;
		final float slope = weightTotal * 9.81f; // * gradient/100
		final float air = 0.5f * p * cD * aP;// * v2;

//		int joule = 0;
//		int prefTime = 0;

		for (int timeIndex = 0; timeIndex < timeSerie.length; timeIndex++) {

			final float speed = speedSerie[timeIndex] / 3.6f; // speed km/h -> m/s
			float gradient = gradientSerie[timeIndex] / 100; // gradient (%) /100

			// adjust computed errors
//			if (gradient < 0.04 && gradient > 0) {
//				gradient *= 0.5;
////				gradient = 0;
//			}

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

//			if (pTotal > 600) {
//				pTotal = pTotal * 1;
//			}
			pTotal = pTotal < 0 ? 0 : pTotal;

			powerSerie[timeIndex] = pTotal;

//			final int currentTime = timeSerie[timeIndex];
//			joule += pTotal * (currentTime - prefTime);

//			prefTime = currentTime;
		}

		return powerSerie;
	}

	public float[] getPulseSmoothedSerie() {

		if (pulseSerie == null) {
			return null;
		}

		if (pulseSerieSmoothed != null) {
			return pulseSerieSmoothed;
		}

		computePulseSmoothed();

		return pulseSerieSmoothed;
	}

	public int getRearShiftCount() {
		return rearShiftCount;
	}

	public int getRestPulse() {
		return restPulse;
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

	public float[] getSpeedSerieFromDevice() {

		if (isSpeedSerieFromDevice) {
			return speedSerie;
		}

		return null;
	}

	private float[] getSpeedSerieInternal() {

		computeSpeedSerie();

		/*
		 * when the speed series are not computed, the internal algorithm will be used to create the
		 * speed data serie
		 */
		if (UI.UNIT_VALUE_DISTANCE == 1) {

			// use metric system

			return speedSerie;

		} else {

			// use imperial system

			return speedSerieImperial;
		}
	}

	/**
	 * @return returns the speed data in the metric measurement system
	 */
	public float[] getSpeedSerieMetric() {

		computeSpeedSerie();

		return speedSerie;
	}

	/**
	 * @return Returns SRTM metric or imperial data serie depending on the active measurement or
	 *         <code>null</code> when SRTM data serie is not available
	 */
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

		if (UI.UNIT_VALUE_ALTITUDE != 1) {

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
	 * @return Returns the tour start date time in seconds.
	 */
	public int getStartTimeOfDay() {
		return (startHour * 3600) + (startMinute * 60) + startSecond;

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

	@XmlElement
	public String getTest() {
		return "jokl"; //$NON-NLS-1$
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

		final DateTime tourStartDefaultZone = getTourStartTime();
		final int utcZoneOffset = tourStartDefaultZone.getZone().getOffset(tourStartDefaultZone.getMillis());

		final long tourStartUTC = tourStartDefaultZone.getMillis() + utcZoneOffset;
		final long tourEnd = tourEndTime;

		final DateTimeZone defaultZone = DateTimeZone.getDefault();

		if (defaultZone.getID().equals(TIME_ZONE_ID_EUROPE_BERLIN)) {

			if (tourStartUTC < net.tourbook.common.UI.beforeCET && tourEnd > net.tourbook.common.UI.afterCETBegin) {

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

						if (absoluteUTCTime > net.tourbook.common.UI.beforeCET) {
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
	 * @return Returns the time zone offset in seconds or {@link Integer#MIN_VALUE} when a time zone
	 *         is not available.
	 */
	public int getTimeZoneOffset() {
		return timeZoneOffset;
	}

	public int getTourAltDown() {
		return tourAltDown;
	}

	public int getTourAltUp() {
		return tourAltUp;
	}

	public TourBike getTourBike() {
		return tourBike;
	}

	/**
	 * @return Returns {@link #tourDescription} or an empty string when value is not set.
	 */
	public String getTourDescription() {
		return tourDescription == null ? UI.EMPTY_STRING : tourDescription;
	}

	/**
	 * @return the tour distance in metric measurement system
	 */
	public float getTourDistance() {
		return tourDistance;
	}

	/**
	 * @return Returns driving/moving time in seconds.
	 */
	public long getTourDrivingTime() {
		return tourDrivingTime;
	}

	/**
	 * @return Returns {@link #tourEndPlace} or an empty string when value is not set.
	 */
	public String getTourEndPlace() {
		return tourEndPlace == null ? UI.EMPTY_STRING : tourEndPlace;
	}

	/**
	 * @return Returns tour end time in ms, this value should be {@link #tourStartTime} +
	 *         {@link #tourRecordingTime}
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
	public Set<TourMarker> getTourMarkers() {
		return tourMarkers;
	}

	/**
	 * @return Returns {@link TourMarker}'s sorted by serie index.
	 */
	public ArrayList<TourMarker> getTourMarkersSorted() {

		if (_sortedMarkers != null) {
			return _sortedMarkers;
		}

		// sort markers by serie index
		_sortedMarkers = new ArrayList<TourMarker>(tourMarkers);

		Collections.sort(_sortedMarkers, new Comparator<TourMarker>() {
			@Override
			public int compare(final TourMarker marker1, final TourMarker marker2) {
				return marker1.getSerieIndex() - marker2.getSerieIndex();
			}
		});

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

	/**
	 * @return Returns total recording time in seconds
	 */
	public long getTourRecordingTime() {
		return tourRecordingTime;
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
	 * @return Returns date/time for the tour start
	 */
	public DateTime getTourStartTime() {

		if (_dateTimeStart == null) {
			_dateTimeStart = new DateTime(tourStartTime);
		}

		return _dateTimeStart;
	}

	/**
	 * @return Returns tour start time in milliseconds since 1970-01-01T00:00:00Z
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

	/**
	 * @return Returns weather text or an empty string when weather text is not set.
	 */
	public String getWeather() {
		return weather == null ? UI.EMPTY_STRING : weather;
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
	 * @return Returns the world position for the suplied zoom level and projection id
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
		result = 37 * result + (int) this.getTourRecordingTime();

		return result;
	}

	/**
	 * @return Returns <code>true</code> when cadence of the tour is spm, otherwise it is rpm.
	 */
	public boolean isCadenceSpm() {

		return cadenceMultiplier != 1.0;
	}

	/**
	 * @return Returns <code>true</code> when {@link TourData} contains refreence tours, otherwise
	 *         <code>false</code>
	 */
	public boolean isContainReferenceTour() {

		if (tourReferences == null) {
			return false;
		} else {
			return tourReferences.size() > 0;
		}
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
	 * @return Returns <code>true</code> when the data in {@link #speedSerie} are from the device
	 *         and not computed. Speed data are normally available from an ergometer and not from a
	 *         bike computer
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
		FIELD_VALIDATION fieldValidation = TourDatabase.isFieldValidForSave(
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
		fieldValidation = TourDatabase.isFieldValidForSave(
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
	 * Called after the object was loaded from the persistence store
	 */
	@PostLoad
	@PostUpdate
	public void onPostLoad() {

		/*
		 * disable post load when database is updated from 19 to 20 because data are converted
		 */
		if (TourDatabase.IS_POST_UPDATE_019_to_020) {
			return;
		}

		onPostLoadGetDataSeries();
	}

	private void onPostLoadGetDataSeries() {

		timeSerie = serieData.timeSerie;

		// manually created tours have currently no time series
		if (timeSerie == null) {
			return;
		}

		altitudeSerie = serieData.altitudeSerie20;
		cadenceSerie = serieData.cadenceSerie20;
		distanceSerie = serieData.distanceSerie20;
		pulseSerie = serieData.pulseSerie20;
		temperatureSerie = serieData.temperatureSerie20;
		powerSerie = serieData.powerSerie20;
		speedSerie = serieData.speedSerie20;

		latitudeSerie = serieData.latitude;
		longitudeSerie = serieData.longitude;

		gearSerie = serieData.gears;

		pulseTimeSerie = serieData.pulseTimes;

		if (powerSerie != null) {
			isPowerSerieFromDevice = true;
		}

		if (speedSerie != null) {
			isSpeedSerieFromDevice = true;
		}
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

		serieData = new SerieData();

		serieData.timeSerie = timeSerie;
		serieData.altitudeSerie20 = altitudeSerie;
		serieData.cadenceSerie20 = cadenceSerie;
		serieData.distanceSerie20 = distanceSerie;
		serieData.pulseSerie20 = pulseSerie;
		serieData.temperatureSerie20 = temperatureSerie;

		/*
		 * don't save computed data series
		 */
		if (isSpeedSerieFromDevice) {
			serieData.speedSerie20 = speedSerie;
		}

		if (isPowerSerieFromDevice) {
			serieData.powerSerie20 = powerSerie;
		}

		serieData.latitude = latitudeSerie;
		serieData.longitude = longitudeSerie;

		serieData.gears = gearSerie;

		serieData.pulseTimes = pulseTimeSerie;

		/*
		 * time serie size
		 */
		numberOfTimeSlices = timeSerie == null ? 0 : timeSerie.length;
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
	 * @param avgCadence
	 *            the avgCadence to set
	 */
	public void setAvgCadence(final float avgCadence) {
		this.avgCadence = avgCadence;
	}

	/**
	 * @param avgPulse
	 *            the avgPulse to set
	 */
	public void setAvgPulse(final float avgPulse) {
		this.avgPulse = avgPulse;
	}

	/**
	 * @param avgTemperature
	 *            the avgTemperature to set
	 */
	public void setAvgTemperature(final float avgTemperature) {
		this.avgTemperature = avgTemperature;
	}

	/**
	 * @param bikerWeight
	 *            Sets the body weight.
	 */
	public void setBodyWeight(final float bikerWeight) {
		this.bikerWeight = bikerWeight;
	}

	public void setBreakTimeSerie(final boolean[] breakTimeSerie) {
		this.breakTimeSerie = breakTimeSerie;
	}

	public void setCadenceMultiplier(final float cadenceMultiplier) {
		this.cadenceMultiplier = cadenceMultiplier;
	}

	/**
	 * @param calories
	 *            the calories to set
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

//						System.out
//								.println((net.tourbook.common.UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//										+ ("\tcurrentFrontGear: " + currentFrontGear)
//										+ ("\tnextFrontGear: " + nextFrontGear)
//										+ ("\tfrontShiftCount: " + frontShiftCount));
//						// TODO remove SYSTEM.OUT.PRINTLN
					}

					if (currentRearGear != nextRearGear) {

						rearShiftCount++;

//						System.out
//								.println((net.tourbook.common.UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//										+ ("\tcurrentRearGear: " + currentRearGear)
//										+ ("\tnextRearGear: " + nextRearGear)
//										+ ("\trearShiftCount: " + rearShiftCount));
//						// TODO remove SYSTEM.OUT.PRINTLN
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

	private void setSpeed(	final int serieIndex,
							final float speedMetric,
							final float speedImperial,
							final int timeDiff,
							final float distDiff) {

		speedSerie[serieIndex] = speedMetric;
		speedSerieImperial[serieIndex] = speedImperial;

		maxSpeed = Math.max(maxSpeed, speedMetric);

		final float paceMetricSeconds = speedMetric < 1.0 ? 0 : (float) (3600.0 / speedMetric);
		final float paceImperialSeconds = speedMetric < 0.6 ? 0 : (float) (3600.0 / speedImperial);

		paceSerieSeconds[serieIndex] = paceMetricSeconds;
		paceSerieSecondsImperial[serieIndex] = paceImperialSeconds;

		paceSerieMinute[serieIndex] = paceMetricSeconds / 60;
		paceSerieMinuteImperial[serieIndex] = paceImperialSeconds / 60;
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

	public void setTimeSerieDouble(final double[] timeSerieDouble) {
		this.timeSerieDouble = timeSerieDouble;
	}

	public void setTimeZoneOffset(final int timeZoneOffset) {
		this.timeZoneOffset = timeZoneOffset;
	}

	public void setTourAltDown(final float tourAltDown) {
		this.tourAltDown = (int) (tourAltDown + 0.5);
	}

	public void setTourAltUp(final float tourAltUp) {
		this.tourAltUp = (int) (tourAltUp + 0.5);
	}

	public void setTourBike(final TourBike tourBike) {
		this.tourBike = tourBike;
	}

	/**
	 * @param tourDescription
	 *            the tourDescription to set
	 */
	public void setTourDescription(final String tourDescription) {
		this.tourDescription = tourDescription;
	}

	public void setTourDistance(final float tourDistance) {
		this.tourDistance = tourDistance;
	}

	/**
	 * Set total driving/moving time in seconds.
	 * 
	 * @param tourDrivingTime
	 */
	public void setTourDrivingTime(final int tourDrivingTime) {
		this.tourDrivingTime = tourDrivingTime;
	}

	/**
	 * @param tourEndPlace
	 *            the tourEndPlace to set
	 */
	public void setTourEndPlace(final String tourEndPlace) {
		this.tourEndPlace = tourEndPlace;
	}

	private void setTourEndTimeMS() {
		tourEndTime = tourStartTime + (tourRecordingTime * 1000);
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
	 * @param linkPhotos
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
					 * photo is not saved any more in this tour, remove tour reference
					 */

					oldGalleryPhoto.removeTour(tourId);

					final HashMap<Long, TourPhotoReference> photoRefs = oldGalleryPhoto.getTourPhotoReferences();

					if (photoRefs.size() == 0) {

						oldGalleryPhoto.isSavedInTour = false;
						oldGalleryPhoto.ratingStars = 0;

						oldGalleryPhoto.resetTourExifState();
					}
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
	 * Set total recording time in seconds
	 * 
	 * @param tourRecordingTime
	 */
	public void setTourRecordingTime(final long tourRecordingTime) {

		this.tourRecordingTime = tourRecordingTime;

		setTourEndTimeMS();
	}

	/**
	 * @param tourStartPlace
	 *            the tourStartPlace to set
	 */
	public void setTourStartPlace(final String tourStartPlace) {
		this.tourStartPlace = tourStartPlace;
	}

	public void setTourStartTime(final DateTime start) {

		_dateTimeStart = start;

		tourStartTime = _dateTimeStart.getMillis();

		startYear = (short) start.getYear();
		startMonth = (short) start.getMonthOfYear();
		startDay = (short) start.getDayOfMonth();
		startHour = (short) start.getHourOfDay();
		startMinute = (short) start.getMinuteOfHour();
		startSecond = start.getSecondOfMinute();

		setWeek(_dateTimeStart);
	}

	/**
	 * Set tour start date/time and week.
	 * 
	 * @param tourStartYear
	 * @param tourStartMonth
	 *            1...12
	 * @param tourStartDay
	 * @param tourStartHour
	 * @param tourStartMinute
	 * @param tourStartSecond
	 */
	public void setTourStartTime(	final int tourStartYear,
									final int tourStartMonth,
									final int tourStartDay,
									final int tourStartHour,
									final int tourStartMinute,
									final int tourStartSecond) {

		_dateTimeStart = new DateTime(
				tourStartYear,
				tourStartMonth,
				tourStartDay,
				tourStartHour,
				tourStartMinute,
				tourStartSecond,
				0);
		tourStartTime = _dateTimeStart.getMillis();

		if (tourStartMonth < 1 || tourStartMonth > 12) {
			StatusUtil.log(new Exception("Month is invalid: " + tourStartMonth)); //$NON-NLS-1$
			startMonth = 1;
		} else {
			startMonth = (short) tourStartMonth;
		}

		startYear = (short) tourStartYear;
//		startMonth = tourStartMonth;
		startDay = (short) tourStartDay;
		startHour = (short) tourStartHour;
		startMinute = (short) tourStartMinute;
		startSecond = tourStartSecond;

		setWeek(_dateTimeStart);
	}

	public void setTourTags(final Set<TourTag> tourTags) {
		this.tourTags = tourTags;
	}

	/**
	 * @param tourTitle
	 *            the tourTitle to set
	 */
	public void setTourTitle(final String tourTitle) {
		this.tourTitle = tourTitle;
	}

	public void setTourType(final TourType tourType) {
		this.tourType = tourType;
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

	public void setWayPoints(final ArrayList<TourWayPoint> wptList) {

		// remove old way points
		tourWayPoints.clear();

		if ((wptList == null) || (wptList.size() == 0)) {
			return;
		}

		// set new way points
		for (final TourWayPoint tourWayPoint : wptList) {

			/**
			 * !!!! <br>
			 * Way point must be cloned because the entity could be saved within different tour data
			 * instances, otherwise hibernate exceptions occure this also sets the createId. <br>
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

	private void setWeek(final DateTime dt) {

		final int firstDayOfWeek = _prefStore.getInt(ITourbookPreferences.CALENDAR_WEEK_FIRST_DAY_OF_WEEK);
		final int minimalDaysInFirstWeek = _prefStore.getInt(ITourbookPreferences.CALENDAR_WEEK_MIN_DAYS_IN_FIRST_WEEK);

		_calendar.setFirstDayOfWeek(firstDayOfWeek);
		_calendar.setMinimalDaysInFirstWeek(minimalDaysInFirstWeek);

		_calendar.set(dt.getYear(), dt.getMonthOfYear() - 1, dt.getDayOfMonth());

		startWeek = (short) _calendar.get(Calendar.WEEK_OF_YEAR);
		startWeekYear = (short) Util.getYearForWeek(_calendar);
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
	public void setWorldPixelForWayPoints(	final TIntObjectHashMap<Point> worldPositions,
											final int zoomLevel,
											final String projectionId) {

		_twpWorldPosition.put(projectionId.hashCode() + zoomLevel, worldPositions);
	}

	@Override
	public String toString() {
		return "TourData [\n" //																		//$NON-NLS-1$

				+ ("start=" + startYear + "-" + startMonth + "-" + startDay + " ") //					//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				+ (startHour + ":" + startMinute + ":" + startSecond + "\n") //							//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

				+ ("tourId=" + tourId + "\n") //														//$NON-NLS-1$ //$NON-NLS-2$

				+ ("object=" + super.toString() + "\n") //												//$NON-NLS-1$ //$NON-NLS-2$
				+ ("identityHashCode=" + System.identityHashCode(this) + "\n") //						//$NON-NLS-1$ //$NON-NLS-2$

//				+ ("marker size:" + tourMarkers.size() + " " + tourMarkers+"\n") //$NON-NLS-1$

				+ "]"; //$NON-NLS-1$
	}

	public String toStringWithHash() {

		final String string = "" //$NON-NLS-1$
				+ ("	tourId: " + tourId) //$NON-NLS-1$
				+ ("	identityHashCode: " + System.identityHashCode(this)); //$NON-NLS-1$

		return string;
	}

	@Override
	public String toXml() {

		try {
			final JAXBContext context = JAXBContext.newInstance(this.getClass());
			final Marshaller marshaller = context.createMarshaller();
			final StringWriter sw = new StringWriter();
			marshaller.marshal(this, sw);
			return sw.toString();

		} catch (final JAXBException e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Converts data series from db version 19 to 20
	 */
	public void updateDatabase_019_to_020() {

		updateDatabase_019_to_020_10DataSeries();
		updateDatabase_019_to_020_20TourMarker();
	}

	private void updateDatabase_019_to_020_10DataSeries() {

		/*
		 * cleanup dataseries because dataseries has been saved before version 1.3.0 even when no
		 * data are available
		 */
// this DO NOT WORK because time serie is not set !!!!
//		cleanupDataSeries();

		final SerieData serieData19 = serieData;
		final SerieData serieData20 = new SerieData();

		serieData20.timeSerie = serieData19.timeSerie;

		serieData20.altitudeSerie20 = convertDataSeries(serieData19.altitudeSerie, 0);
		serieData20.altitudeSerie = null;

		serieData20.cadenceSerie20 = convertDataSeries(serieData19.cadenceSerie, 0);
		serieData20.cadenceSerie = null;

		serieData20.distanceSerie20 = convertDataSeries(serieData19.distanceSerie, 0);
		serieData20.distanceSerie = null;

		serieData20.pulseSerie20 = convertDataSeries(serieData19.pulseSerie, 0);
		serieData20.pulseSerie = null;

		serieData20.temperatureSerie20 = convertDataSeries(serieData19.temperatureSerie, temperatureScale);
		serieData20.temperatureSerie = null;

		/*
		 * don't convert computed data series
		 */

		if (serieData19.speedSerie != null) {
			isSpeedSerieFromDevice = true;
			serieData20.speedSerie20 = convertDataSeries(serieData19.speedSerie, 10);
		}
		serieData20.speedSerie = null;

		if (serieData19.powerSerie != null) {
			isPowerSerieFromDevice = true;
			serieData20.powerSerie20 = convertDataSeries(serieData19.powerSerie, 0);
		}
		serieData20.powerSerie = null;

		serieData20.latitude = serieData19.latitude;
		serieData20.longitude = serieData19.longitude;

		// this serie is never used
		serieData20.deviceMarker = null;

		serieData = serieData20;

		onPostLoadGetDataSeries();
	}

	private void updateDatabase_019_to_020_20TourMarker() {

		for (final TourMarker tourMarker : tourMarkers) {
			tourMarker.updateDatabase_019_to_020();
		}
	}

}
