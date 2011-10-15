/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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

import java.awt.Point;
import java.io.PrintStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.ChartLabel;
import net.tourbook.database.FIELD_VALIDATION;
import net.tourbook.database.TourDatabase;
import net.tourbook.importdata.TourbookDevice;
import net.tourbook.math.Smooth;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageComputedValues;
import net.tourbook.srtm.ElevationSRTM3;
import net.tourbook.srtm.GeoLat;
import net.tourbook.srtm.GeoLon;
import net.tourbook.srtm.NumberForm;
import net.tourbook.tour.BreakTimeResult;
import net.tourbook.tour.BreakTimeTool;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;
import net.tourbook.ui.tourChart.ChartLayer2ndAltiSerie;
import net.tourbook.ui.views.ISmoothingAlgorithm;
import net.tourbook.ui.views.tourDataEditor.TourDataEditorView;
import net.tourbook.util.MtMath;
import net.tourbook.util.StatusUtil;
import net.tourbook.util.Util;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.hibernate.annotations.Cascade;
import org.joda.time.DateTime;

/**
 * Tour data contains all data for a tour (except markers), an entity will be saved in the database
 */
@Entity
@XmlType(name = "TourData")
@XmlRootElement(name = "TourData")
@XmlAccessorType(XmlAccessType.NONE)
public class TourData implements Comparable<Object>, IXmlSerializable {

	public static final int									DB_LENGTH_DEVICE_TOUR_TYPE			= 2;
	public static final int									DB_LENGTH_DEVICE_PLUGIN_ID			= 255;
	public static final int									DB_LENGTH_DEVICE_PLUGIN_NAME		= 255;
	public static final int									DB_LENGTH_DEVICE_MODE_NAME			= 255;
	public static final int									DB_LENGTH_DEVICE_FIRMWARE_VERSION	= 255;

	public static final int									DB_LENGTH_TOUR_TITLE				= 255;
	public static final int									DB_LENGTH_TOUR_DESCRIPTION			= 4096;
	public static final int									DB_LENGTH_TOUR_DESCRIPTION_V10		= 32000;
	public static final int									DB_LENGTH_TOUR_START_PLACE			= 255;
	public static final int									DB_LENGTH_TOUR_END_PLACE			= 255;
	public static final int									DB_LENGTH_TOUR_IMPORT_FILE_PATH		= 255;

	public static final int									DB_LENGTH_WEATHER					= 1000;
	public static final int									DB_LENGTH_WEATHER_CLOUDS			= 255;

	public static final int									MIN_TIMEINTERVAL_FOR_MAX_SPEED		= 20;

	public static final float								MAX_BIKE_SPEED						= 120f;

	/**
	 * Number of defined hr zone fields which is currently {@link #hrZone0} ... {@link #hrZone9} =
	 * 10
	 */
	public static final int									MAX_HR_ZONES						= 10;

	/**
	 * Device Id for manually created tours
	 */
	public static final String								DEVICE_ID_FOR_MANUAL_TOUR			= "manual";										//$NON-NLS-1$

	/**
	 * Device id for csv files which behave like manually created tours, marker and timeslices are
	 * disabled because they are not available, tour duration can be edited<br>
	 * this is the id of the deviceDataReader
	 */
	public static final String								DEVICE_ID_CSV_TOUR_DATA_READER		= "net.tourbook.device.CSVTourDataReader";			//$NON-NLS-1$

	/**
	 * THIS IS NOT UNUSED !!!<br>
	 * <br>
	 * initialize SRTM
	 */
	@SuppressWarnings("unused")
	@Transient
	private static final NumberForm							srtmNumberForm						= new NumberForm();

	@Transient
	private static final ElevationSRTM3						elevationSRTM3						= new ElevationSRTM3();

	@Transient
	private static IPreferenceStore							_prefStore							= TourbookPlugin
																										.getDefault()
																										.getPreferenceStore();

	@Transient
	private final Calendar									_calendar							= GregorianCalendar
																										.getInstance();

	/**
	 * Unique entity id which identifies the tour
	 */
	@Id
	private Long											tourId;

	// ############################################# DATE #############################################

	/**
	 * year of tour start
	 */
	private short											startYear;

	/**
	 * mm (d) month of tour
	 */
	private short											startMonth;

	/**
	 * dd (d) day of tour
	 */
	private short											startDay;

	/**
	 * HH (d) hour of tour
	 */
	private short											startHour;

	/**
	 * MM (d) minute of tour
	 */
	private short											startMinute;

	/**
	 * altitude difference for the merged tour
	 */
	private int												startSecond;																			// db-version 7

	/**
	 * THIS IS NOT UNUSED !!!<br>
	 * <br>
	 * week of the tour provided by {@link Calendar#get(int)}
	 */
	@SuppressWarnings("unused")
	private short											startWeek;

	/**
	 * THIS IS NOT UNUSED !!!<br>
	 * this field can be read with sql statements
	 * <p>
	 * year for startWeek
	 */
	@SuppressWarnings("unused")
	private short											startWeekYear;

	// ############################################# TIME #############################################

	/**
	 * Total recording time in seconds
	 */
	@XmlElement
	private int												tourRecordingTime;

	/**
	 * Total driving/moving time in seconds
	 */
	@XmlElement
	private int												tourDrivingTime;

	// ############################################# DISTANCE #############################################

	/**
	 * Total distance of the device at tour start (km) tttt (h). Distance for the tour is stored in
	 * the field {@link #tourDistance}
	 */
	private int												startDistance;

	/**
	 * total distance of the tour in meters (metric system), this value is computed from the
	 * distance data serie
	 */
	@XmlElement
	private int												tourDistance;

	/**
	 * Are the distance values measured with a distance sensor or with lat/lon values.<br>
	 * <br>
	 * 0 == false <i>(default, no distance sensor)</i> <br>
	 * 1 == true
	 */
	private short											isDistanceFromSensor				= 0;												// db-version 8

	// ############################################# ALTITUDE #############################################

	/**
	 * aaaa (h) initial altitude (m)
	 */
	private short											startAltitude;

	/**
	 * altitude up (m)
	 */
	@XmlElement
	private int												tourAltUp;

	/**
	 * altitude down (m)
	 */
	@XmlElement
	private int												tourAltDown;

	// ############################################# PULSE/WEIGHT/POWER #############################################

	/**
	 * pppp (h) initial pulse (bpm)
	 */
	private short											startPulse;

	@XmlElement
	private int												restPulse;																				// db-version 8

	@XmlElement
	private Integer											calories;																				// db-version 4

	private float											bikerWeight;																			// db-version 4

	/**
	 * A flag indicating that the power is from a sensor. This is the state of the device which is
	 * not related to the availability of power data. Power data should be available but is not
	 * checked.<br>
	 * <br>
	 * 0 == false, 1 == true
	 */
	private int												isPowerSensorPresent				= 0;												// db-version 12

	// ############################################# PULSE #############################################

	/**
	 * Average pulse, this data can also be set from device data and pulse data are not available
	 */
	@XmlElement
	private int												avgPulse;																				// db-version 4

	/**
	 * Maximum pulse for the current tour.
	 */
	@XmlElement
	private int												maxPulse;																				// db-version 4

	/**
	 * Number of HR zones which are available for this tour, is 0 when HR zones are not defined.
	 */
	private int												numberOfHrZones						= 0;												// db-version 18

	/**
	 * Time for all HR zones are contained in {@link #hrZone0} ... {@link #hrZone9}. Each tour can
	 * have up to 10 HR zones, when HR zone value is <code>-1</code> then this zone is not set.
	 */
	private int												hrZone0								= -1;												// db-version 16
	private int												hrZone1								= -1;												// db-version 16
	private int												hrZone2								= -1;												// db-version 16
	private int												hrZone3								= -1;												// db-version 16
	private int												hrZone4								= -1;												// db-version 16
	private int												hrZone5								= -1;												// db-version 16
	private int												hrZone6								= -1;												// db-version 16
	private int												hrZone7								= -1;												// db-version 16
	private int												hrZone8								= -1;												// db-version 16
	private int												hrZone9								= -1;												// db-version 16

	/**
	 * A flag indicating that the pulse is from a sensor. This is the state of the device which is
	 * not related to the availability of pulse data. Pulse data should be available but is not
	 * checked.<br>
	 * <br>
	 * 0 == false, 1 == true
	 */
	private int												isPulseSensorPresent				= 0;												// db-version 12

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
	private String											deviceTourType;

	/**
	 * Profile id which is defined by the device
	 */
	private short											deviceMode;																			// db-version 3

	/**
	 * Visible name for the used profile which is defined in {@link #deviceMode}, e.g. Jogging,
	 * Running, Bike1, Bike2...
	 */
	private String											deviceModeName;																		// db-version 4

	/**
	 * maximum altitude in metric system
	 */
	@XmlElement
	private int												maxAltitude;																			// db-version 4

	// ############################################# MAX VALUES #############################################

	/**
	 * maximum speed in metric system
	 */
	@XmlElement
	private float											maxSpeed;																				// db-version 4

	// ############################################# AVERAGE VALUES #############################################

	@XmlElement
	private int												avgCadence;																			// db-version 4

	private int												avgTemperature;																		// db-version 4
	private int												weatherWindDir;																		// db-version 8

	private int												weatherWindSpd;																		// db-version 8
	private String											weatherClouds;																			// db-version 8
	private String											weather;																				// db-version 13
	private float											deviceAvgSpeed;																		// db-version 12

	@XmlElement
	private String											tourTitle;																				// db-version 4

	// ############################################# OTHER TOUR/DEVICE DATA #############################################

	@XmlElement
	private String											tourDescription;																		// db-version 4

	@XmlElement
	private String											tourStartPlace;																		// db-version 4

	@XmlElement
	private String											tourEndPlace;																			// db-version 4

	/**
	 * Date/Time when tour data was created. This value is set to the tour start date before db
	 * version 11, otherwise the value is set when the tour is saved the first time.
	 * <p>
	 * Data format: YYYYMMDDhhmmss
	 */
	private long											dateTimeCreated;																		// db-version 11

	/**
	 * Date/Time when tour data was modified, default value is 0
	 * <p>
	 * Data format: YYYYMMDDhhmmss
	 */
	private long											dateTimeModified;																		// db-version 11

	/**
	 * file path for the imported tour
	 */
	private String											tourImportFilePath;																	// db-version 6

	/**
	 * tolerance for the Douglas Peucker algorithm
	 */
	private short											dpTolerance							= 50;

	/**
	 * Time difference in seconds between 2 time slices or <code>-1</code> for GPS devices when the
	 * time slices has variable time duration
	 */
	private short											deviceTimeInterval					= -1;												// db-version 3

	/**
	 * Scaling factor for the temperature data serie, e.g. when set to 10 the temperature data serie
	 * is multiplied by 10, default scaling is <code>1</code>
	 */
	/*
	 * disabled when float was introduces in 11.after8, preserved in database but can be removed
	 */
	@SuppressWarnings("unused")
	private int												temperatureScale					= 1;												// db-version 13

	/**
	 * Firmware version of the device
	 */
	private String											deviceFirmwareVersion;																	// db-version 12

	/**
	 * when a tour is merged with another tour, {@link #mergeSourceTourId} contains the tour id of
	 * the tour which is merged into this tour
	 */
	private Long											mergeSourceTourId;																		// db-version 7

	// ############################################# MERGED DATA #############################################

	/**
	 * when a tour is merged into another tour, {@link #mergeTargetTourId} contains the tour id of
	 * the tour into which this tour is merged
	 */
	private Long											mergeTargetTourId;																		// db-version 7

	/**
	 * positive or negative time offset in seconds for the merged tour
	 */
	private int												mergedTourTimeOffset;																	// db-version 7

	/**
	 * altitude difference for the merged tour
	 */
	private int												mergedAltitudeOffset;																	// db-version 7

	/**
	 * Unique plugin id for the device data reader which created this tour, this id is defined in
	 * plugin.xml
	 * <p>
	 * a better name would be <i>pluginId</i>
	 */
	private String											devicePluginId;

	// ############################################# PLUGIN DATA #############################################

	/**
	 * Visible name for the used {@link TourbookDevice}, this name is defined in plugin.xml
	 * <p>
	 * a better name would be <i>pluginName</i>
	 */
	private String											devicePluginName;																		// db-version 4

	/**
	 * Deflection point in the conconi test, this value is the index for the data serie on the
	 * x-axis
	 */
	private int												conconiDeflection;

	// ############################################# CONCONI TEST #############################################

	/**
	 * ssss distance msw
	 * <p>
	 * is not used any more since 6.12.2006 but it's necessary then it's a field in the database
	 */
	@SuppressWarnings("unused")
	private int												distance;

	// ############################################# UNUSED FIELDS #############################################

	@SuppressWarnings("unused")
	private int												deviceDistance;

	@SuppressWarnings("unused")
	private int												deviceTotalUp;

	@SuppressWarnings("unused")
	private int												deviceTotalDown;

	@SuppressWarnings("unused")
	private long											deviceTravelTime;

	@SuppressWarnings("unused")
	private int												deviceWheel;

	@SuppressWarnings("unused")
	private int												deviceWeight;

	/**
	 * data series for time, altitude,...
	 */
	@Basic(optional = false)
	private SerieData										serieData;

	/**
	 * Tour marker
	 */
	@OneToMany(fetch = FetchType.EAGER, cascade = ALL, mappedBy = "tourData")
	@Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
	@XmlElementWrapper(name = "TourMarkers")
	@XmlElement(name = "TourMarker")
	private Set<TourMarker>									tourMarkers							= new HashSet<TourMarker>();

	// ############################################# ASSOCIATED ENTITIES #############################################

	/**
	 * Contains the tour way points
	 */
	@OneToMany(fetch = FetchType.EAGER, cascade = ALL, mappedBy = "tourData")
	@Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
	private final Set<TourWayPoint>							tourWayPoints						= new HashSet<TourWayPoint>();

	/**
	 * Reference tours
	 */
	@OneToMany(fetch = FetchType.EAGER, cascade = ALL, mappedBy = "tourData")
	@Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
	private final Set<TourReference>						tourReferences						= new HashSet<TourReference>();

	/**
	 * Tags
	 */
	@ManyToMany(fetch = EAGER)
	@JoinTable(inverseJoinColumns = @JoinColumn(name = "tourTag_tagId", referencedColumnName = "tagId"))
	private Set<TourTag>									tourTags							= new HashSet<TourTag>();

	/**
	 * Category of the tour, e.g. bike, mountainbike, jogging, inlinescating
	 */
	@ManyToOne
	private TourType										tourType;

	/**
	 * Person which created this tour or <code>null</code> when the tour is not saved in the
	 * database
	 */
	@ManyToOne
	private TourPerson										tourPerson;

	/**
	 * plugin id for the device which was used for this tour Bike used for this tour
	 */
	@ManyToOne
	private TourBike										tourBike;

	/**
	 * Contains time in seconds relativ to the tour start which is defined in: {@link #startYear},
	 * {@link #startMonth}, {@link #startDay}, {@link #startHour}, {@link #startMinute} and
	 * {@link #startSecond}.
	 * <p>
	 * The array {@link #timeSerie} is <code>null</code> for a manually created tour, it is
	 * <b>always</b> set when tour is from a device or imported file.
	 * <p>
	 * This field has a copy in {@link #timeSerieFloat}.
	 */
	@Transient
	public int[]											timeSerie;

	/*
	 * tourCategory is currently (version 1.6) not used but is defined in older databases, it is
	 * disabled because the field is not available in the database table
	 */
	//	@ManyToMany(fetch = FetchType.LAZY, mappedBy = "tourData")
	//	private Set<TourCategory>	tourCategory					= new HashSet<TourCategory>();

	// ############################################# TRANSIENT DATA #############################################

	/**
	 * contains the absolute distance in m (metric system)
	 */
	@Transient
	public float[]											distanceSerie;

	/**
	 * contains the absolute distance in miles/1000 (imperial system)
	 */
	@Transient
	private float[]											distanceSerieImperial;

	/**
	 * contains the absolute altitude in m (metric system)
	 */
	@Transient
	public float[]											altitudeSerie;

	/**
	 * smoothed altitude serie is used to display the tour chart when not <code>null</code>
	 */
	@Transient
	private float[]											altitudeSerieSmoothed;

	/**
	 * contains the absolute altitude in feet (imperial system)
	 */
	@Transient
	private float[]											altitudeSerieImperial;

	/**
	 * smoothed altitude serie is used to display the tour chart when not <code>null</code>
	 */
	@Transient
	private float[]											altitudeSerieImperialSmoothed;

	/**
	 * SRTM altitude values, when <code>null</code> srtm data have not yet been attached, when
	 * <code>length()==0</code> data are invalid.
	 */
	@Transient
	private float[]											srtmSerie;

	@Transient
	private float[]											srtmSerieImperial;

	@Transient
	public float[]											cadenceSerie;

	@Transient
	public float[]											pulseSerie;

	@Transient
	public float[]											pulseSerieSmoothed;

	/**
	 * Contains <code>true</code> or <code>false</code> for each time slice of the whole tour.
	 * <code>true</code> is set when a time slice is a break.
	 */
	@Transient
	private boolean[]										breakTimeSerie;

	/**
	 * Contains the temperature in the metric measurement system.
	 */
	@Transient
	public float[]											temperatureSerie;

	/**
	 * contains the temperature in the imperial measurement system
	 */
	@Transient
	private float[]											temperatureSerieImperial;

	/**
	 * contains speed in km/h multiplied by {@link TourManager#SPEED_DIVISOR}
	 * <p>
	 * the metric speed serie is required when computing the power even if the current measurement
	 * system is imperial
	 */
	@Transient
	private float[]											speedSerie;

	@Transient
	private float[]											speedSerieImperial;

	/**
	 * Is <code>true</code> when the data in {@link #speedSerie} are from the device and not
	 * computed. Speed data are normally available from an ergometer and not from a bike computer
	 */
	@Transient
	private boolean											isSpeedSerieFromDevice				= false;

	/**
	 * pace in min/km
	 */
	@Transient
	private float[]											paceSerieMinute;

	/**
	 * pace in sec/km
	 */
	@Transient
	private float[]											paceSerieSeconds;

	/**
	 * pace in min/mile
	 */
	@Transient
	private float[]											paceSerieMinuteImperial;

	/**
	 * pace in sec/mile
	 */
	@Transient
	private float[]											paceSerieSecondsImperial;

	@Transient
	private float[]											powerSerie;

	/**
	 * Is <code>true</code> when the data in {@link #powerSerie} are from the device and not
	 * computed. Power data source can be an ergometer or a power sensor
	 */
	@Transient
	private boolean											isPowerSerieFromDevice				= false;

	@Transient
	private float[]											altimeterSerie;

	@Transient
	private float[]											altimeterSerieImperial;

	@Transient
	public float[]											gradientSerie;

	@Transient
	public float[]											tourCompareSerie;

	/*
	 * computed data series
	 */

	/*
	 * GPS data
	 */
	@Transient
	public double[]											latitudeSerie;

	@Transient
	public double[]											longitudeSerie;

	/**
	 * contains the bounds of the tour in latitude/longitude
	 */
	@Transient
	public Rectangle										gpsBounds;

	/**
	 * Index of the segmented data in the data series
	 */
	@Transient
	public int[]											segmentSerieIndex;

	/**
	 * oooo (o) DD-record // offset
	 */
	@Transient
	public int												offsetDDRecord;

	/*
	 * data for the tour segments
	 */
	@Transient
	public int[]											segmentSerieTimeTotal;

	@Transient
	public int[]											segmentSerieRecordingTime;
	@Transient
	public int[]											segmentSerieDrivingTime;
	@Transient
	public int[]											segmentSerieBreakTime;

	@Transient
	public float[]											segmentSerieDistanceDiff;
	@Transient
	public float[]											segmentSerieDistanceTotal;
	@Transient
	public float[]											segmentSerieAltitudeDiff;
	@Transient
	public float[]											segmentSerieComputedAltitudeDiff;
	@Transient
	public float[]											segmentSerieAltitudeUpH;
	@Transient
	public float[]											segmentSerieAltitudeDownH;

	@Transient
	public float[]											segmentSerieSpeed;
	@Transient
	public float[]											segmentSeriePace;
	@Transient
	public float[]											segmentSeriePaceDiff;
	@Transient
	public float[]											segmentSeriePower;
	@Transient
	public float[]											segmentSerieGradient;
	@Transient
	public float[]											segmentSeriePulse;

	/**
	 * contains the filename from which the data are imported, when set to <code>null</code> the
	 * data it not imported they are from the database
	 */
	@Transient
	public String											importRawDataFile;

	/**
	 * Latitude for the center position in the map or {@link Double#MIN_VALUE} when the position is
	 * not set
	 */
	@Transient
	public double											mapCenterPositionLatitude			= Double.MIN_VALUE;

	/**
	 * Longitude for the center position in the map or {@link Double#MIN_VALUE} when the position is
	 * not set
	 */
	@Transient
	public double											mapCenterPositionLongitude			= Double.MIN_VALUE;

	/**
	 * Zoomlevel in the map
	 */
	@Transient
	public int												mapZoomLevel;

	@Transient
	public double											mapMinLatitude;

	@Transient
	public double											mapMaxLatitude;

	@Transient
	public double											mapMinLongitude;

	@Transient
	public double											mapMaxLongitude;

	/**
	 * caches the world positions for the tour lat/long values for each zoom level
	 */
	@Transient
	private final Map<Integer, Point[]>						_tourWorldPosition					= new HashMap<Integer, Point[]>();

	/**
	 * caches the world positions for the way point lat/long values for each zoom level
	 */
	@Transient
	private final HashMap<Integer, HashMap<Integer, Point>>	_twpWorldPosition					= new HashMap<Integer, HashMap<Integer, Point>>();

	/**
	 * when a tour was deleted and is still visible in the raw data view, resaving the tour or
	 * finding the tour in the entity manager causes lots of trouble with hibernate, therefor this
	 * tour cannot be saved again, it must be reloaded from the file system
	 */
	@Transient
	public boolean											isTourDeleted						= false;

	/**
	 * 2nd data serie, this is used in the {@link ChartLayer2ndAltiSerie} to display the merged tour
	 * or the adjusted altitude
	 */
	@Transient
	public float[]											dataSerie2ndAlti;

	/**
	 * altitude difference between this tour and the merge tour with metric measurement
	 */
	@Transient
	public float[]											dataSerieDiffTo2ndAlti;

	/**
	 * contains the altitude serie which is adjusted
	 */
	@Transient
	public float[]											dataSerieAdjustedAlti;

	/**
	 * contains special data points
	 */
	@Transient
	public SplineData										splineDataPoints;

	/**
	 * Contains a spline data serie
	 */
	@Transient
	public float[]											dataSerieSpline;

	/**
	 * when a tour is not saved, the tour id is not defined, therefore the tour data are provided
	 * from the import view when tours are merged to display the merge layer
	 */
	@Transient
	private TourData										_mergeSourceTourData;

	@Transient
	private DateTime										_dateTimeCreated;

	@Transient
	private DateTime										_dateTimeModified;

	/**
	 * Tour start time
	 */
	@Transient
	private DateTime										_dateTimeStart;

	/**
	 * Tour markers which are sorted by serie index
	 */
	@Transient
	private ArrayList<TourMarker>							_sortedMarkers;

	/**
	 * Contains seconds from all hr zones: {@link #hrZone0} ... {@link #hrZone9}
	 */
	@Transient
	private int[]											_hrZones;

	@Transient
	private HrZoneContext									_hrZoneContext;

	/**
	 * Copy of {@link #timeSerie} with floating type, this is used for the chart x-axis.
	 */
	@Transient
	private float[]											timeSerieFloat;

	public TourData() {}

	/**
	 * Removed data series when the sum of all values is 0
	 */
	public void cleanupDataSeries() {

		int sumAltitude = 0;
		int sumCadence = 0;
		int sumDistance = 0;
		int sumPulse = 0;
		int sumTemperature = 0;
		int sumPower = 0;
		int sumSpeed = 0;

		// get first valid latitude/longitude
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
		if (isPowerSerieFromDevice == false) {
			powerSerie = null;
		}

		timeSerieFloat = null;

		paceSerieMinute = null;
		paceSerieSeconds = null;
		paceSerieMinuteImperial = null;
		paceSerieSecondsImperial = null;

		altimeterSerie = null;
		gradientSerie = null;

		breakTimeSerie = null;

		speedSerieImperial = null;
		paceSerieMinuteImperial = null;
		altimeterSerieImperial = null;

		altitudeSerieSmoothed = null;
		altitudeSerieImperial = null;
		altitudeSerieImperialSmoothed = null;

		pulseSerieSmoothed = null;

		srtmSerie = null;
		srtmSerieImperial = null;

		_hrZones = null;
		_hrZoneContext = null;
	}

	/**
	 * clears the cached world positions, this is necessary when the data serie have been modified
	 */
	public void clearWorldPositions() {
		_tourWorldPosition.clear();
	}

	/*
	 * Set default sort method (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(final Object obj) {

		if (obj instanceof TourData) {

			final TourData otherTourData = (TourData) obj;

			return startYear < otherTourData.startYear ? -1 : startYear == otherTourData.startYear
					? startMonth < otherTourData.startMonth ? -1 : startMonth == otherTourData.startMonth
							? startDay < otherTourData.startDay ? -1 : startDay == otherTourData.startDay
									? startHour < otherTourData.startHour ? -1 : startHour == otherTourData.startHour
											? startMinute < otherTourData.startMinute
													? -1
													: startSecond == otherTourData.startSecond //
															? 0
															: 1 //
											: 1 //
									: 1 //
							: 1 //
					: 1;
		}

		return 0;
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
			dataSerieGradient[serieIndex] = distanceDiff == 0 ? 0 : altitudeDiff * 1000 / distanceDiff;
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
					final float gradient = altitudeDiff * 1000 / distanceDiff;
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

		final int prefMinAltitude = _prefStore.getInt(PrefPageComputedValues.STATE_COMPUTED_VALUE_MIN_ALTITUDE);

		final AltitudeUpDown altiUpDown = computeAltitudeUpDownInternal(null, prefMinAltitude);

		if (altiUpDown == null) {
			return false;
		}

		setTourAltUp(altiUpDown.altitudeUp);
		setTourAltDown(altiUpDown.altitudeDown);

		return true;
	}

	public AltitudeUpDown computeAltitudeUpDown(final ArrayList<AltitudeUpDownSegment> segmentSerieIndexParameter,
												final int minAltiDiff) {
		return computeAltitudeUpDownInternal(segmentSerieIndexParameter, minAltiDiff);
	}

	/**
	 * compute altitude up/down since version 9.08
	 * 
	 * @param segmentSerie
	 *            segments are created for each gradient alternation when segmentSerie is not
	 *            <code>null</code>
	 * @param altitudeMinDiff
	 * @return Returns <code>null</code> when altitude up/down cannot be computed
	 */
	private AltitudeUpDown computeAltitudeUpDownInternal(	final ArrayList<AltitudeUpDownSegment> segmentSerie,
															final int altitudeMinDiff) {

		// check if data are available
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

						if (angleAltiDown <= -altitudeMinDiff) {

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

						if (angleAltiUp >= altitudeMinDiff) {

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

	private void computeAvgCadence() {

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
			avgCadence = (int) cadenceSum / cadenceCount;
		}
	}

	public void computeAvgPulse() {

		if ((pulseSerie == null) || (pulseSerie.length == 0) || (timeSerie == null) || (timeSerie.length == 0)) {
			return;
		}

		avgPulse = (int) (computeAvgPulseSegment(0, timeSerie.length - 1) + 0.5);
	}

	private float computeAvgPulseSegment(final int firstIndex, final int lastIndex) {

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

		float pulseSquare = 0;
		float timeSquare = 0;

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

		return timeSquare == 0 ? 0 : pulseSquare / timeSquare;
	}

	private void computeAvgTemperature() {

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
			avgTemperature = (int) (temperatureSum / tempLength + .5);
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
	 * compute maximum and average fields
	 */
	public void computeComputedValues() {

		computePulseSmoothed();
		computeSmoothedDataSeries();

		computeMaxAltitude();
		computeMaxPulse();
		computeMaxSpeed();

		computeAvgPulse();
		computeAvgCadence();
		computeAvgTemperature();

		computeHrZones();
	}

	/**
	 * Computes seconds for each hr zone and sets the number of available HR zones in
	 * {@link #numberOfHrZones}.
	 */
	private void computeHrZones() {

		if (timeSerie == null || pulseSerie == null || tourPerson == null) {
			return;
		}

		if (pulseSerieSmoothed == null) {
			computePulseSmoothed();

		}
		_hrZoneContext = tourPerson.getHrZoneContext(
				tourPerson.getHrMaxFormula(),
				tourPerson.getMaxPulse(),
				tourPerson.getBirthDayWithDefault(),
				getStartDateTime());

		if (_hrZoneContext == null) {
			// hr zones are not defined
			return;
		}

		if (breakTimeSerie == null) {
			getBreakTime();
		}

		final int zoneSize = _hrZoneContext.zoneMinBpm.length;
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

			for (int zoneIndex = 0; zoneIndex < zoneSize; zoneIndex++) {

				final int minValue = _hrZoneContext.zoneMinBpm[zoneIndex];
				final int maxValue = _hrZoneContext.zoneMaxBpm[zoneIndex];

				if (pulse >= minValue && pulse <= maxValue) {
					hrZones[zoneIndex] += timeDiff;
					break;
				}
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

		this.maxAltitude = (int) maxAltitude;
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

		this.maxPulse = (int) maxPulse;
	}

	private void computeMaxSpeed() {
		if (distanceSerie != null) {
			computeSmoothedDataSeries();
		}
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
		if (size < 2 || distanceSerie == null && (latitudeSerie == null || longitudeSerie == null)) {
			return;
		}

		speedSerie = new float[size];
		speedSerieImperial = new float[size];

		paceSerieMinute = new float[size];
		paceSerieMinuteImperial = new float[size];
		paceSerieSeconds = new float[size];
		paceSerieSecondsImperial = new float[size];

		gradientSerie = new float[size];

		altimeterSerie = new float[size];
		altimeterSerieImperial = new float[size];

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

				gradientSerie[serieIndex] = (float) (Vv_sc[serieIndex] / Vh_sc[serieIndex] * 1000.);

				final double vSpeedSmoothed = Vv_sc[serieIndex] * 3600.;
				altimeterSerie[serieIndex] = (float) (vSpeedSmoothed);
				altimeterSerieImperial[serieIndex] = (float) (vSpeedSmoothed / UI.UNIT_VALUE_ALTITUDE);
			}
		}

		maxSpeed = 0.0f;
		for (int serieIndex = 0; serieIndex < Vh.length; serieIndex++) {

			final double speedMetric = Vh[serieIndex] * 36;
			final double speedImperial = speedMetric / UI.UNIT_MILE;

			if (speedMetric > maxSpeed) {
				maxSpeed = (float) speedMetric;
			}

			speedSerie[serieIndex] = (float) speedMetric;
			speedSerieImperial[serieIndex] = (float) speedImperial;

			paceSerieSeconds[serieIndex] = speedMetric < 10 ? 0 : (float) (36000.0 / speedMetric);
			paceSerieSecondsImperial[serieIndex] = speedMetric < 6 ? 0 : (float) (36000.0 / speedImperial);
		}
		maxSpeed /= 10;
	}

	/**
	 * computes the speed data serie which can be retrieved with {@link TourData#getSpeedSerie()}
	 */
	public void computeSpeedSerie() {

//		final long start = System.nanoTime();

		if ((speedSerie != null)
				&& (speedSerieImperial != null)
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

		for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

			/*
			 * speed
			 */

			final float speedMetric = speedSerie[serieIndex];

			speedSerieImperial[serieIndex] = speedMetric / UI.UNIT_MILE;
			maxSpeed = Math.max(maxSpeed, speedMetric);
		}

		maxSpeed /= 10;
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

		speedSerie = new float[serieLength];
		speedSerieImperial = new float[serieLength];

		paceSerieMinute = new float[serieLength];
		paceSerieSeconds = new float[serieLength];
		paceSerieMinuteImperial = new float[serieLength];
		paceSerieSecondsImperial = new float[serieLength];

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
				final float speed = (distDiff * 36) / timeDiff;
				speedMetric = speed;
				speedImperial = speed / UI.UNIT_MILE;
			}

			speedSerie[serieIndex] = speedMetric;
			speedSerieImperial[serieIndex] = speedImperial;

			maxSpeed = Math.max(maxSpeed, speedMetric);

			/*
			 * pace (computed with divisor 10)
			 */
			float paceMetricSeconds = 0;
			float paceImperialSeconds = 0;
			float paceMetricMinute = 0;
			float paceImperialMinute = 0;

			if ((speedMetric != 0) && (distDiff != 0)) {

//				final float pace = timeDiff * 166.66f / distDiff;
//				final float pace = 10 * (((float) timeDiff / 60) / ((float) distDiff / 1000));

//				paceMetricSeconds = 10* timeDiff * 1000 / (float) distDiff;
				paceMetricSeconds = timeDiff * 10000 / distDiff;
				paceImperialSeconds = paceMetricSeconds * UI.UNIT_MILE;

				paceMetricMinute = paceMetricSeconds / 60;
				paceImperialMinute = paceImperialSeconds / 60;
			}

			paceSerieMinute[serieIndex] = paceMetricMinute;
			paceSerieMinuteImperial[serieIndex] = paceImperialMinute;

			paceSerieSeconds[serieIndex] = (int) paceMetricSeconds / 10;
			paceSerieSecondsImperial[serieIndex] = (int) paceImperialSeconds / 10;
		}

		maxSpeed /= 10;
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

		speedSerie = new float[serieLength];
		speedSerieImperial = new float[serieLength];

		paceSerieMinute = new float[serieLength];
		paceSerieSeconds = new float[serieLength];
		paceSerieMinuteImperial = new float[serieLength];
		paceSerieSecondsImperial = new float[serieLength];

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
								speedMetric = ((equalSegmentDistDiff * 36) / equalSegmentTimeDiff);
								speedMetric = speedMetric < 0 ? 0 : speedMetric;

								speedImperial = equalSegmentDistDiff * 36 / (equalSegmentTimeDiff * UI.UNIT_MILE);
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
					speedMetric = distDiff * 36 / timeDiff;
					speedMetric = speedMetric < 0 ? 0 : speedMetric;

					speedImperial = distDiff * 36 / (timeDiff * UI.UNIT_MILE);
					speedImperial = speedImperial < 0 ? 0 : speedImperial;
				}
			}

			setSpeed(serieIndex, speedMetric, speedImperial, timeDiff, distDiff);
		}

		maxSpeed /= 10;
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

		if ((timeSerie == null) || (timeSerie.length == 0)) {
			tourDrivingTime = 0;
		} else {
			final int tourDrivingTimeRaw = timeSerie[timeSerie.length - 1] - getBreakTime();
			tourDrivingTime = Math.max(0, tourDrivingTimeRaw);
		}
	}

	private void createSRTMDataSerie() {

		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {

			@Override
			public void run() {

				int serieIndex = 0;
				short lastValidSRTM = 0;
				boolean isSRTMValid = false;

				final int serieLength = timeSerie.length;

				final float[] newSRTMSerie = new float[serieLength];
				final float[] newSRTMSerieImperial = new float[serieLength];

				for (final double latitude : latitudeSerie) {

					short srtmValue = elevationSRTM3.getElevation(new GeoLat(latitude), new GeoLon(
							longitudeSerie[serieIndex]));

					/*
					 * set invalid values to the previous valid value
					 */
					if (srtmValue == Short.MIN_VALUE) {
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
	public void createTimeSeries(final ArrayList<TimeData> timeDataList, final boolean isCreateMarker) {

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
		timeSerieFloat = new float[serieSize];

		final boolean isDistance = setupDistanceStartingValues(timeDataSerie, isAbsoluteData);
		final boolean isAltitude = setupAltitudeStartingValues(timeDataSerie, isAbsoluteData);
		final boolean isPulse = setupPulseStartingValues(timeDataSerie);
		final boolean isCadence = setupCadenceStartingValues(timeDataSerie);
		final boolean isTemperature = setupTemperatureStartingValues(timeDataSerie);
		final boolean isGPS = setupLatLonStartingValues(timeDataSerie);

		/*
		 * Speed
		 */
		boolean isSpeed = false;
		if (firstTimeDataItem.speed != Float.MIN_VALUE) {
			speedSerie = new float[serieSize];
			isSpeed = true;

			isSpeedSerieFromDevice = true;
		}

		/*
		 * Power
		 */
		boolean isPower = false;
		if (firstTimeDataItem.power != Float.MIN_VALUE) {
			powerSerie = new float[serieSize];
			isPower = true;

			isPowerSerieFromDevice = true;
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

		createTimeSeries10DataCompleting();

		tourDistance = (int) (isDistance ? distanceSerie[serieSize - 1] : 0);
		tourRecordingTime = (int) recordingTime;

		/*
		 * create marker after all other data are setup
		 */
		if (isCreateMarker) {
			for (int serieIndex = 0; serieIndex < serieSize; serieIndex++) {

				final TimeData timeData = timeDataSerie[serieIndex];

				if (timeData.marker != 0) {
					int time = 0;
					float distanceValue = 0;

					if (timeSerie != null) {
						time = timeSerie[serieIndex];
					}
					if (distanceSerie != null) {
						distanceValue = distanceSerie[serieIndex];
					}

					createTourMarker(timeData, serieIndex, time, distanceValue);
				}
			}
		}

		cleanupDataSeries();
		resetSortedMarkers();
	}

	/**
	 * Interpolations of missing data
	 */
	private void createTimeSeries10DataCompleting() {

		createTimeSeries12RemoveInvalidDistanceValues();

		createTimeSeries20data_completing(latitudeSerie, timeSerie);
		createTimeSeries20data_completing(longitudeSerie, timeSerie);

		createTimeSeries30data_completing(altitudeSerie, timeSerie);
		createTimeSeries30data_completing(distanceSerie, timeSerie);
		createTimeSeries30data_completing(temperatureSerie, timeSerie);
		createTimeSeries30data_completing(pulseSerie, timeSerie);
	}

	private void createTimeSeries12RemoveInvalidDistanceValues() {

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

	private void createTimeSeries20data_completing(final double[] field, final int[] time) {

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

					field[interpolationIndex] = createTimeSeries40linear_interpolation(
							time1,
							time2,
							val1,
							val2,
							time[interpolationIndex]);
				}

				serieIndex = nextValidIndex - 1;
			}
		}
	}

	private void createTimeSeries30data_completing(final float[] field, final int[] time) {

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

					final double linearInterpolation = createTimeSeries40linear_interpolation(
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

	private double createTimeSeries40linear_interpolation(	final double time1,
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

	/**
	 * Creates a unique tour id depending on the tour start time and current time
	 */
	public void createTourId() {

		final String uniqueKey = Long.toString(System.currentTimeMillis());

		createTourId(uniqueKey.substring(uniqueKey.length() - 5, uniqueKey.length()));
	}

	/**
	 * Creates the unique tour id from the tour date/time and the unique key
	 * 
	 * @param uniqueKeySuffix
	 *            unique key to identify a tour
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
			tourIdKey = Short.toString(getStartYear())
					+ Short.toString(getStartMonth())
					+ Short.toString(getStartDay())
					+ Short.toString(getStartHour())
					+ Short.toString(getStartMinute())
					//
					+ uniqueKeySuffix;

			tourId = Long.valueOf(tourIdKey);

		} catch (final NumberFormatException e) {

			/*
			 * the distance is shorted that the maximum of a Long datatype is not exceeded
			 */
			try {

				tourIdKey = Short.toString(getStartYear())
						+ Short.toString(getStartMonth())
						+ Short.toString(getStartDay())
						+ Short.toString(getStartHour())
						+ Short.toString(getStartMinute())
						//
						+ uniqueKeySuffix.substring(0, Math.min(5, uniqueKeySuffix.length()));

				tourId = Long.valueOf(tourIdKey);

			} catch (final NumberFormatException e2) {

				// this case happened when getStartMonth() had a wrong value

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
	 * @param recordingTime
	 * @param distanceAbsolute
	 */
	private void createTourMarker(	final TimeData timeData,
									final int serieIndex,
									final long recordingTime,
									final float distanceAbsolute) {

		// create a new marker
		final TourMarker tourMarker = new TourMarker(this, ChartLabel.MARKER_TYPE_DEVICE);

		tourMarker.setVisualPosition(ChartLabel.VISUAL_HORIZONTAL_ABOVE_GRAPH_CENTERED);
		tourMarker.setTime((int) (recordingTime + timeData.marker));
		tourMarker.setDistance((int) (distanceAbsolute + .5));
		tourMarker.setSerieIndex(serieIndex);

		if (timeData.markerLabel == null) {
			tourMarker.setLabel(Messages.tour_data_label_device_marker);
		} else {
			tourMarker.setLabel(timeData.markerLabel);
		}

		tourMarkers.add(tourMarker);
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
	public Object[] createTourSegments(final BreakTimeTool btConfig) {

		if ((segmentSerieIndex == null) || (segmentSerieIndex.length < 2)) {
			// at least two points are required to build a segment
			return new Object[0];
		}

		final boolean isPulseSerie = (pulseSerie != null) && (pulseSerie.length > 0);
		final boolean isAltitudeSerie = (altitudeSerie != null) && (altitudeSerie.length > 0);
		final boolean isDistanceSerie = (distanceSerie != null) && (distanceSerie.length > 0);

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
			altitudeStart = altitudeSerie[firstSerieIndex];
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

		segmentSerieRecordingTime = new int[segmentSerieLength];
		segmentSerieDrivingTime = new int[segmentSerieLength];
		segmentSerieBreakTime = new int[segmentSerieLength];
		segmentSerieTimeTotal = new int[segmentSerieLength];

		segmentSerieDistanceDiff = new float[segmentSerieLength];
		segmentSerieDistanceTotal = new float[segmentSerieLength];

		segmentSerieAltitudeDiff = new float[segmentSerieLength];
		segmentSerieAltitudeUpH = new float[segmentSerieLength];
		segmentSerieAltitudeDownH = new float[segmentSerieLength];

		segmentSerieSpeed = new float[segmentSerieLength];
		segmentSeriePace = new float[segmentSerieLength];

		segmentSeriePulse = new float[segmentSerieLength];
		segmentSeriePower = new float[segmentSerieLength];
		segmentSerieGradient = new float[segmentSerieLength];
//		segmentSerieCadence = new float[segmentSerieLength];

		// compute values between start and end
		for (int segmentIndex = 1; segmentIndex < segmentSerieLength; segmentIndex++) {

			final int segmentStartIndex = segmentSerieIndex[segmentIndex - 1];
			final int segmentEndIndex = segmentSerieIndex[segmentIndex];

			final TourSegment segment = new TourSegment();
			tourSegments.add(segment);

			segment.serieIndexStart = segmentStartIndex;
			segment.serieIndexEnd = segmentEndIndex;

			/*
			 * time
			 */
			final int timeEnd = timeSerie[segmentEndIndex];
			final int recordingTime = timeEnd - timeStart;
			final int segmentBreakTime = getBreakTime(segmentStartIndex, segmentEndIndex, btConfig);

			final float drivingTime = recordingTime - segmentBreakTime;

			segmentSerieRecordingTime[segmentIndex] = segment.recordingTime = recordingTime;
			segmentSerieDrivingTime[segmentIndex] = segment.drivingTime = (int) drivingTime;
			segmentSerieBreakTime[segmentIndex] = segment.breakTime = segmentBreakTime;
			segmentSerieTimeTotal[segmentIndex] = segment.timeTotal = timeTotal += recordingTime;

			float segmentDistance = 0.0f;

			/*
			 * distance
			 */
			if (isDistanceSerie) {

				final float distanceEnd = distanceSerie[segmentEndIndex];
				final float distanceDiff = distanceEnd - distanceStart;

				segmentSerieDistanceDiff[segmentIndex] = segment.distanceDiff = distanceDiff;
				segmentSerieDistanceTotal[segmentIndex] = segment.distanceTotal = distanceTotal += distanceDiff;

				// end point of current segment is the start of the next segment
				distanceStart = distanceEnd;

				segmentDistance = segment.distanceDiff;
				if (segmentDistance != 0.0) {

					// speed
					segmentSerieSpeed[segmentIndex] = segment.speed = drivingTime == 0.0f ? //
							0.0f
							: segmentDistance / drivingTime * 3.6f / UI.UNIT_VALUE_DISTANCE;

					// pace
					final float segmentPace = drivingTime * 1000 / (segmentDistance / UI.UNIT_VALUE_DISTANCE);
					segment.pace = (int) segmentPace;
					segment.paceDiff = segment.pace - tourPace;
					segmentSeriePace[segmentIndex] = segmentPace;
				}
			}

			/*
			 * altitude
			 */
			if (isAltitudeSerie) {

				final float altitudeEnd = altitudeSerie[segmentEndIndex];
				final float altitudeDiff = altitudeEnd - altitudeStart;

				segmentSerieAltitudeDiff[segmentIndex] = segment.altitudeDiffSegmentBorder = altitudeDiff;

				if (altitudeDiff > 0) {
					segment.altitudeUpSummarizedBorder = altitudeUpSummarizedBorder += altitudeDiff;
					segment.altitudeDownSummarizedBorder = altitudeDownSummarizedBorder;

				} else {
					segment.altitudeUpSummarizedBorder = altitudeUpSummarizedBorder;
					segment.altitudeDownSummarizedBorder = altitudeDownSummarizedBorder += altitudeDiff;
				}

				if ((segmentSerieComputedAltitudeDiff != null)
						&& (segmentIndex < segmentSerieComputedAltitudeDiff.length)) {

					final float segmentDiff = segmentSerieComputedAltitudeDiff[segmentIndex];

					segment.altitudeDiffSegmentComputed = segmentDiff;

					if (segmentDiff > 0) {

						segment.altitudeUpSummarizedComputed = altitudeUpSummarizedComputed += segmentDiff;
						segment.altitudeDownSummarizedComputed = altitudeDownSummarizedComputed;

					} else {

						segment.altitudeUpSummarizedComputed = altitudeUpSummarizedComputed;
						segment.altitudeDownSummarizedComputed = altitudeDownSummarizedComputed += segmentDiff;
					}
				}

				float altitudeUpH = 0;
				float altitudeDownH = 0;
				float powerSum = 0;

				float altitude1 = altitudeSerie[segmentStartIndex];

				// get computed values: altitude up/down, pulse and power for a segment
				for (int serieIndex = segmentStartIndex + 1; serieIndex <= segmentEndIndex; serieIndex++) {

					final float altitude2 = altitudeSerie[serieIndex];
					final float altitude2Diff = altitude2 - altitude1;
					altitude1 = altitude2;

					altitudeUpH += altitude2Diff >= 0 ? altitude2Diff : 0;
					altitudeDownH += altitude2Diff < 0 ? altitude2Diff : 0;

					if (isPowerSerie) {
						powerSum += localPowerSerie[serieIndex];
					}
				}

				segment.altitudeUpHour = altitudeUpH;

				segmentSerieAltitudeDownH[segmentIndex] = segment.altitudeDownHour = altitudeDownH;
				segmentSerieAltitudeUpH[segmentIndex] = recordingTime == 0 ? //
						0
						: (altitudeUpH + altitudeDownH) / recordingTime * 3600 / UI.UNIT_VALUE_ALTITUDE;

				final int segmentIndexDiff = segmentEndIndex - segmentStartIndex;
				segmentSeriePower[segmentIndex] = segment.power = segmentIndexDiff == 0 ? 0 : powerSum
						/ segmentIndexDiff;

				// end point of current segment is the start of the next segment
				altitudeStart = altitudeEnd;
			}

			if (isDistanceSerie && isAltitudeSerie && (segmentDistance != 0.0)) {

				// gradient
				segmentSerieGradient[segmentIndex] = segment.gradient = //
				segment.altitudeDiffSegmentBorder * 100 / segmentDistance;
			}

			if (isPulseSerie) {
				final float segmentAvgPulse = computeAvgPulseSegment(segmentStartIndex, segmentEndIndex);
				segmentSeriePulse[segmentIndex] = segment.pulse = segmentAvgPulse;
				segment.pulseDiff = segmentAvgPulse - avgPulse;
			} else {
				// hide pulse in the view
				segment.pulseDiff = Float.MIN_VALUE;
			}

			// end point of current segment is the start of the next segment
			timeStart = timeEnd;
		}

		return tourSegments.toArray();
	}

//	#include <stdio.h>
//	#include <stdlib.h>
//	#include <math.h>
//
//	#define SIZE 1528
//	#define pi 3.1415926535897932384626433832795
//
//	int index_next_valid_data(double* field, double invalid_data, int i_start)
//	{
//	  int i, i_valid;
//
//	  i = i_start;
//
//	  while ( field[i] == invalid_data )
//	    {
//	      i++;
//	    }
//
//	  return(i);
//	}
//
//	double linear_interpolation(double time1, double time2, double val1, double val2, double time)
//	{
//	  if (time2 == time1)
//	    return( (val1+val2)/2. );
//	  else
//	    return( val1 + (val2 - val1) / (time2 - time1) * (time - time1) );
//	}
//
//	double distance_ellipsoid_gps(double lat1, double lat2, double lon1, double lon2)
//	{
//	  double earth_radius = 6371000.;
//	// This value is not important for the interpolation
//	// Any constant value can be chosen
//
//	  double lat1_rad = lat1*pi/180.;
//	  double lat2_rad = lat2*pi/180.;
//	  double lon1_rad = lon1*pi/180.;
//	  double lon2_rad = lon2*pi/180.;
//
//	  return( earth_radius * 2. * asin( sqrt( (sin((lat1_rad-lat2_rad)/2.)) * (sin((lat1_rad-lat2_rad)/2.)) + cos(lat1_rad) * cos(lat2_rad) * (sin((lon1_rad-lon2_rad)/2.)) * (sin((lon1_rad-lon2_rad)/2.)) ) ) );
//	}
//
//	void data_completing(double* field, double* var, double invalid_data, double* field_c)
//	{
//	  int i, j;
//	  int i_valid;
//
//	  for (i=0; i<SIZE; i++)
//	    {
//	      if (field[i] == invalid_data)
//	        {
//	          i_valid = index_next_valid_data(field, invalid_data, i);
//	          if (field[i_valid] == invalid_data)
//	            printf("ERROR: the field should be a valid data)");
//
//	          for (j=i; j<i_valid; j++)
//	            {
//	              field_c[j] = linear_interpolation(var[i-1], var[i_valid], field[i-1], field[i_valid], var[j]);
//	            }
//	          i=i_valid-1;
//	        }
//	      else
//	        {
//	          field_c[i] = field[i];
//	        }
//	    }
//	}
//
//
//	main()
//	{
//	  int i;
//	  double time[SIZE];
//	  double latitude[SIZE], longitude[SIZE];
//	  double latitude_c[SIZE], longitude_c[SIZE];
//	  double altitude[SIZE], altitude_c[SIZE];
//	  double distance_gps[SIZE];
//	  double distance[SIZE], distance_ct[SIZE], distance_cd[SIZE];
//	  double speed[SIZE], speed_c[SIZE];
//	  double heart_rate[SIZE];
//
//	  FILE *file;
//
//	// Initialization
//	//===============
//	  for(i=0; i<SIZE; i++)
//	    {
//	      time[i] = -1.;
//	      latitude[i] = -1.;
//	      longitude[i] = -1.;
//	      altitude[i] = -1.;
//	      distance[i] = -1.;
//	      speed[i] = -1.;
//	      heart_rate[i] = -1.;
//	    }
//
//	// Reading the data in a text file
//	//================================
//	// In the initial file, all the missing data are set to -999.
//	//-----------------------------------------------------------
//	  file = fopen("example_complete.txt", "r");
//	  for(i=0; i<SIZE; i++)
//	    {
//	      fscanf(file, "%lf\t%lf\t%lf\t%lf\t%lf\t%lf\t%lf\n", &time[i], &latitude[i], &longitude[i], &altitude[i], &distance[i], &speed[i], &heart_rate[i]);
//	    }
//	  fclose(file);
//
//	// Interpolations of missing data
//	//===============================
//	// Latitude and longitude are interpolated linearly in time
//	//---------------------------------------------------------
//	  data_completing(latitude, time, -999., latitude_c);
//	  data_completing(longitude, time, -999., longitude_c);
//
//	// Altitude is interpolated linearly in time
//	//------------------------------------------
//	  data_completing(altitude, time, -999., altitude_c);
//
//	// Speed is interpolated linearly in time
//	//---------------------------------------
//	  data_completing(speed, time, -999., speed_c);
//
//	// Distance is interpolated linearly in time
//	//------------------------------------------
//	  data_completing(distance, time, -999., distance_ct);
//
//	// Results
//	//========
//	  file = fopen("result_time.txt", "w");
//	  for (i=0; i<SIZE; i++)
//	    fprintf(file, "%g\t%g\t%g\t%g\t%g\t%g\t%g\t%g\t%g\t%g\t%g\t%g\n", time[i], latitude[i], latitude_c[i], longitude[i], longitude_c[i], altitude[i], altitude_c[i], distance[i], distance_ct[i], distance_gps[i], speed[i], speed_c[i]);
//	  fclose(file);
//
//	// Compute an approximate distance from latitude and longitude data
//	//-----------------------------------------------------------------
//	  distance_gps[0] = 0.;
//	  for (i=1; i<SIZE; i++)
//	    {
//	      distance_gps[i] = distance_gps[i-1] + distance_ellipsoid_gps(latitude_c[i-1], latitude_c[i], longitude_c[i-1], longitude_c[i]);
//	    }
//	// Distance is interpolated linearly in distance_gps
//	//--------------------------------------------------
//	  data_completing(distance, distance_gps, -999., distance_cd);
//
//	// Results
//	//========
//	  file = fopen("result_ellipsoid.txt", "w");
//	  for (i=0; i<SIZE; i++)
//	    fprintf(file, "%g\t%g\t%g\t%g\t%g\t%g\t%g\t%g\t%g\t%g\t%g\t%g\n", time[i], latitude[i], latitude_c[i], longitude[i], longitude_c[i], altitude[i], altitude_c[i], distance[i], distance_cd[i], distance_gps[i], speed[i], speed_c[i]);
//	  fclose(file);
//
//	}

	public void dumpData() {

		final PrintStream out = System.out;

		out.println("----------------------------------------------------"); //$NON-NLS-1$
		out.println("TOUR DATA"); //$NON-NLS-1$
		out.println("----------------------------------------------------"); //$NON-NLS-1$
// out.println("Typ: " + getDeviceTourType()); //$NON-NLS-1$
		out.println("Date:			" + getStartDay() + "." + getStartMonth() + "." + getStartYear()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		out.println("Time:			" + getStartHour() + ":" + getStartMinute()); //$NON-NLS-1$ //$NON-NLS-2$
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
	 * @return Returns altitude smoothed values when they are set to be smoothed otherwise it
	 *         returns normal altitude values or <code>null</code> when altitude is not available.
	 */
	public float[] getAltitudeSmoothedSerie() {

		if (altitudeSerie == null) {
			return null;
		}

		// smooth altitude
		computeSmoothedDataSeries();

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
	public int getAvgCadence() {
		return avgCadence;
	}

	/**
	 * @return the avgPulse
	 */
	public int getAvgPulse() {
		return avgPulse;
	}

	/**
	 * @return Returns metric average temperature
	 */
	public int getAvgTemperature() {
		return avgTemperature;
	}

	/**
	 * @return the bikerWeight
	 */
	public float getBikerWeight() {
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

	/**
	 * @return the calories
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

	public float getDeviceAvgSpeed() {
		return deviceAvgSpeed;
	}

	public String getDeviceFirmwareVersion() {
		return deviceFirmwareVersion == null ? UI.EMPTY_STRING : deviceFirmwareVersion;
	}

	public String getDeviceId() {
		return devicePluginId;
	}

	public short getDeviceMode() {
		return deviceMode;
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

// NOT USED 18.8.2010
//	public long getDeviceTravelTime() {
//		return deviceTravelTime;
//	}
//
//	public int getDeviceWeight() {
//		return deviceWeight;
//	}
//
//	public int getDeviceWheel() {
//		return deviceWheel;
//	}

// not used 5.10.2008
//	public int getDeviceDistance() {
//		return deviceDistance;
//	}

	/**
	 * @return Returns the distance data serie for the current measurement system, this can be
	 *         metric or imperial
	 */
	public float[] getDistanceSerie() {

		if (distanceSerie == null) {
			return null;
		}

		float[] serie;

		final float unitValueDistance = UI.UNIT_VALUE_DISTANCE;

		if (unitValueDistance != 1) {

			// use imperial system

			if (distanceSerieImperial == null) {

				// compute imperial data

				distanceSerieImperial = new float[distanceSerie.length];

				for (int valueIndex = 0; valueIndex < distanceSerie.length; valueIndex++) {
					distanceSerieImperial[valueIndex] = distanceSerie[valueIndex] / unitValueDistance;
				}
			}
			serie = distanceSerieImperial;

		} else {

			// use metric system

			serie = distanceSerie;
		}

		return serie;
	}

	public short getDpTolerance() {
		return dpTolerance;
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

			_hrZones = new int[] {
					hrZone0,
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

		return _hrZones;
	}

// not used 5.10.2008
//	public int getDeviceTotalDown() {
//		return deviceTotalDown;
//	}

//	public int getDeviceTotalUp() {
//		return deviceTotalUp;
//	}

	/**
	 * @return the maxAltitude
	 */
	public int getMaxAltitude() {
		return maxAltitude;
	}

	/**
	 * @return the maxPulse
	 */
	public int getMaxPulse() {
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

			if (paceSerieMinute == null) {
				computeSpeedSerie();
			}

			return paceSerieSeconds;

		} else {

			// use imperial system

			if (paceSerieMinuteImperial == null) {
				computeSpeedSerie();
			}

			return paceSerieSecondsImperial;
		}
	}

	public float[] getPowerSerie() {

		if ((powerSerie != null) || isPowerSerieFromDevice) {
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

			final float speed = speedSerie[timeIndex] / 36; // speed (m/s) /10
			float gradient = gradientSerie[timeIndex] / 1000; // gradient (%) /10 /100

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

	public int getRestPulse() {
		return restPulse;
	}

	public SerieData getSerieData() {
		return serieData;
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

	/**
	 * @return Returns date/time for the tour start
	 */
	public DateTime getStartDateTime() {

		if (_dateTimeStart == null) {
			_dateTimeStart = new DateTime(startYear, startMonth, startDay, startHour, startMinute, startSecond, 0);
		}

		return _dateTimeStart;
	}

	public short getStartDay() {
		return startDay;
	}

	public int getStartDistance() {
		return startDistance;
	}

	public short getStartHour() {
		return startHour;
	}

	public short getStartMinute() {
		return startMinute;
	}

	/**
	 * @return Returns the month for the tour start in the range 1...12
	 */
	public short getStartMonth() {
		return startMonth;
	}

	public short getStartPulse() {
		return startPulse;
	}

	public int getStartSecond() {
		return startSecond;
	}

	public short getStartYear() {
		return startYear;
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

	public float[] getTimeSerieFloat() {

		if (timeSerie == null) {
			return null;
		}

		if (timeSerieFloat != null) {
			return timeSerieFloat;
		}

		timeSerieFloat = new float[timeSerie.length];

		for (int serieIndex = 0; serieIndex < timeSerie.length; serieIndex++) {
			timeSerieFloat[serieIndex] = timeSerie[serieIndex];
		}

		return timeSerieFloat;
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
	 * @return the tourDescription
	 */
	public String getTourDescription() {
		return tourDescription == null ? UI.EMPTY_STRING : tourDescription;
	}

	/**
	 * @return the tour distance in metric measurement system
	 */
	public int getTourDistance() {
		return tourDistance;
	}

	public int getTourDrivingTime() {
		return tourDrivingTime;
	}

	/**
	 * @return the tourEndPlace
	 */
	public String getTourEndPlace() {
		return tourEndPlace == null ? UI.EMPTY_STRING : tourEndPlace;
	}

	/**
	 * @return Returns the unique key in the database for this {@link TourData} entity
	 */
	public Long getTourId() {
		return tourId;
	}

	public String getTourImportFilePath() {
		if ((tourImportFilePath == null) || (tourImportFilePath.length() == 0)) {
			if (isManualTour()) {
				return UI.EMPTY_STRING;
			} else {
				return Messages.tour_data_label_feature_since_version_9_01;
			}
		} else {
			return tourImportFilePath;
		}
	}

	public String getTourImportFilePathRaw() {
		return tourImportFilePath;
	}

	/**
	 * @return Returns a set with all {@link TourMarker} for the tour or an empty set when markers
	 *         are not available.
	 */
	public Set<TourMarker> getTourMarkers() {
		return tourMarkers;
	}

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
	 *         not saved in the database
	 */
	public TourPerson getTourPerson() {
		return tourPerson;
	}

	/**
	 * @return Returns total recording time in seconds
	 */
	public int getTourRecordingTime() {
		return tourRecordingTime;
	}

	public Collection<TourReference> getTourReferences() {
		return tourReferences;
	}

	/**
	 * @return the tourStartPlace
	 */
	public String getTourStartPlace() {
		return tourStartPlace == null ? UI.EMPTY_STRING : tourStartPlace;
	}

	/**
	 * @return Returns the tags {@link #tourTags} which are defined for this tour
	 */
	public Set<TourTag> getTourTags() {
		return tourTags;
	}

	/**
	 * @return Returns tour title or an empty string when title is not set
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
	public HashMap<Integer, Point> getWorldPositionForWayPoints(final String projectionId, final int zoomLevel) {
		return _twpWorldPosition.get(projectionId.hashCode() + zoomLevel);
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		int result = 17;

		result = 37 * result + this.getStartYear();
		result = 37 * result + this.getStartMonth();
		result = 37 * result + this.getStartDay();
		result = 37 * result + this.getStartHour();
		result = 37 * result + this.getStartMinute();
		result = 37 * result + this.getTourDistance();
		result = 37 * result + this.getTourRecordingTime();

		return result;
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

	public boolean isTourImportFilePathAvailable() {

		if (tourImportFilePath != null && tourImportFilePath.length() > 0) {
			return true;
		}

		return false;
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

		timeSerie = serieData.timeSerie;

		// manually created tours have currently no time series
		if (timeSerie == null) {
			return;
		}

		altitudeSerie = serieData.altitudeSerie;
		cadenceSerie = serieData.cadenceSerie;
		distanceSerie = serieData.distanceSerie;
		pulseSerie = serieData.pulseSerie;
		temperatureSerie = serieData.temperatureSerie;
		powerSerie = serieData.powerSerie;
		speedSerie = serieData.speedSerie;

		latitudeSerie = serieData.latitude;
		longitudeSerie = serieData.longitude;

		/*
		 * cleanup dataseries because dataseries has been saved before version 1.3.0 even when no
		 * data are available
		 */
		cleanupDataSeries();
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

		serieData.altitudeSerie = altitudeSerie;
		serieData.cadenceSerie = cadenceSerie;
		serieData.distanceSerie = distanceSerie;
		serieData.pulseSerie = pulseSerie;
		serieData.temperatureSerie = temperatureSerie;
		serieData.timeSerie = timeSerie;

		/*
		 * don't save computed data series
		 */
		if (isSpeedSerieFromDevice) {
			serieData.speedSerie = speedSerie;
		}

		if (isPowerSerieFromDevice) {
			serieData.powerSerie = powerSerie;
		}

		serieData.latitude = latitudeSerie;
		serieData.longitude = longitudeSerie;
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
	public void setAvgCadence(final int avgCadence) {
		this.avgCadence = avgCadence;
	}

	/**
	 * @param avgPulse
	 *            the avgPulse to set
	 */
	public void setAvgPulse(final int avgPulse) {
		this.avgPulse = avgPulse;
	}

	/**
	 * @param avgTemperature
	 *            the avgTemperature to set
	 */
	public void setAvgTemperature(final int avgTemperature) {
		this.avgTemperature = avgTemperature;
	}

	/**
	 * @param bikerWeight
	 *            the bikerWeight to set
	 */
	public void setBikerWeight(final float bikerWeight) {
		this.bikerWeight = bikerWeight;
	}

	public void setBreakTimeSerie(final boolean[] breakTimeSerie) {
		this.breakTimeSerie = breakTimeSerie;
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

	public void setDeviceAvgSpeed(final float deviceAvgSpeed) {
		this.deviceAvgSpeed = deviceAvgSpeed;
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

	public void setDeviceTotalDown(final int deviceTotalDown) {
		this.deviceTotalDown = deviceTotalDown;
	}

	public void setDeviceTotalUp(final int deviceTotalUp) {
		this.deviceTotalUp = deviceTotalUp;
	}

	public void setDeviceTourType(final String tourType) {
		this.deviceTourType = tourType;
	}

	public void setDeviceTravelTime(final long deviceTravelTime) {
// NOT USED 18.8.2010
//		this.deviceTravelTime = deviceTravelTime;
	}

	public void setDeviceWeight(final int deviceWeight) {
// NOT USED 18.8.2010
//		this.deviceWeight = deviceWeight;
	}

	public void setDeviceWheel(final int deviceWheel) {
// NOT USED 18.8.2010
//		this.deviceWheel = deviceWheel;
	}

	public void setDpTolerance(final short dpTolerance) {
		this.dpTolerance = dpTolerance;
	}

	/**
	 * Set state if the distance is from a sensor or not, default is <code>false</code>
	 * 
	 * @param isFromSensor
	 */
	public void setIsDistanceFromSensor(final boolean isFromSensor) {
		this.isDistanceFromSensor = (short) (isFromSensor ? 1 : 0);
	}

	public void setMaxPulse(final int maxPulse) {
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

	/**
	 * Sets the power data serie and set's a flag that the data serie is from a device
	 * 
	 * @param powerSerie
	 */
	public void setPowerSerie(final float[] powerSerie) {
		this.powerSerie = powerSerie;
		this.isPowerSerieFromDevice = true;
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

		/*
		 * pace (computed with divisor 10)
		 */
		float paceMetricSeconds = 0;
		float paceImperialSeconds = 0;
		float paceMetricMinute = 0;
		float paceImperialMinute = 0;

		if ((speedMetric != 0) && (distDiff != 0)) {

			paceMetricSeconds = timeDiff * 10000 / distDiff;
			paceImperialSeconds = paceMetricSeconds * UI.UNIT_MILE;

			paceMetricMinute = ((paceMetricSeconds / 60));
			paceImperialMinute = (int) ((paceImperialSeconds / 60));
		}

		paceSerieMinute[serieIndex] = paceMetricMinute;
		paceSerieMinuteImperial[serieIndex] = paceImperialMinute;

		paceSerieSeconds[serieIndex] = paceMetricSeconds / 10;
		paceSerieSecondsImperial[serieIndex] = paceImperialSeconds / 10;
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

	public void setStartDay(final short startDay) {
		_dateTimeStart = null;
		this.startDay = startDay;
	}

	/**
	 * Odometer value, this is the distance which the device is accumulating
	 * 
	 * @param startDistance
	 */
	public void setStartDistance(final int startDistance) {
		this.startDistance = startDistance;
	}

	public void setStartHour(final short startHour) {
		_dateTimeStart = null;
		this.startHour = startHour;
	}

	public void setStartMinute(final short startMinute) {
		_dateTimeStart = null;
		this.startMinute = startMinute;
	}

	/**
	 * Sets the month for the tour start in the range 1...12
	 */
	public void setStartMonth(final short startMonth) {
		_dateTimeStart = null;

		if (startMonth < 1 || startMonth > 12) {
			StatusUtil.log(new Exception("Month is invalid: " + startMonth)); //$NON-NLS-1$
			this.startMonth = 1;
		} else {
			this.startMonth = startMonth;
		}
	}

	public void setStartPulse(final short startPulse) {
		this.startPulse = startPulse;
	}

	public void setStartSecond(final int startSecond) {
		_dateTimeStart = null;
		this.startSecond = startSecond;
	}

	public void setStartYear(final short startYear) {
		_dateTimeStart = null;
		this.startYear = startYear;
	}

	public void setTimeSerieFloat(final float[] timeSerieFloat) {
		this.timeSerieFloat = timeSerieFloat;
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

	public void setTourDistance(final int tourDistance) {
		this.tourDistance = tourDistance;
	}

	/**
	 * Set total driving/moving time in seconds
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

	/**
	 * Sets the file path for the imported file, this is displayed in the {@link TourDataEditorView}
	 * 
	 * @param tourImportFilePath
	 */
	public void setTourImportFilePath(final String tourImportFilePath) {
		this.tourImportFilePath = tourImportFilePath;
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
	 * Set total recording time in seconds
	 * 
	 * @param tourRecordingTime
	 */
	public void setTourRecordingTime(final int tourRecordingTime) {
		this.tourRecordingTime = tourRecordingTime;
	}

	/**
	 * @param tourStartPlace
	 *            the tourStartPlace to set
	 */
	public void setTourStartPlace(final String tourStartPlace) {
		this.tourStartPlace = tourStartPlace;
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
	private boolean setupAltitudeStartingValues(final TimeData[] timeDataSerie, final boolean isAbsoluteData) {

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

	private boolean setupCadenceStartingValues(final TimeData[] timeDataSerie) {

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

	private boolean setupDistanceStartingValues(final TimeData[] timeDataSerie, final boolean isAbsoluteData) {

		final TimeData firstTimeData = timeDataSerie[0];
		final int serieSize = timeDataSerie.length;

		boolean isAvailable = false;

		if ((firstTimeData.distance != Float.MIN_VALUE) || isAbsoluteData) {
			distanceSerie = new float[serieSize];
			isAvailable = true;
		}

		return isAvailable;
	}

	private boolean setupLatLonStartingValues(final TimeData[] timeDataSerie) {

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

	private boolean setupPulseStartingValues(final TimeData[] timeDataSerie) {

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

	private boolean setupTemperatureStartingValues(final TimeData[] timeDataSerie) {

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
			 * way point must be cloned because the entity could be saved within different tour data
			 * instances, otherwise hibernate exceptions occure <br>
			 * this also sets the createId <br>
			 * !!!!
			 */
			final TourWayPoint clonedWP = (TourWayPoint) tourWayPoint.clone();

			clonedWP.setTourData(this);
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

	public void setWeek(final DateTime dt) {

		final int firstDayOfWeek = _prefStore.getInt(ITourbookPreferences.CALENDAR_WEEK_FIRST_DAY_OF_WEEK);
		final int minimalDaysInFirstWeek = _prefStore.getInt(ITourbookPreferences.CALENDAR_WEEK_MIN_DAYS_IN_FIRST_WEEK);

		_calendar.setFirstDayOfWeek(firstDayOfWeek);
		_calendar.setMinimalDaysInFirstWeek(minimalDaysInFirstWeek);

		_calendar.set(dt.getYear(), dt.getMonthOfYear() - 1, dt.getDayOfMonth());

		startWeek = (short) _calendar.get(Calendar.WEEK_OF_YEAR);
		startWeekYear = (short) Util.getYearForWeek(_calendar);
	}

	/**
	 * @param year
	 * @param month
	 *            month starts with 1
	 * @param tourDay
	 */
	public void setWeek(final short year, final short month, final short tourDay) {

		final int firstDayOfWeek = _prefStore.getInt(ITourbookPreferences.CALENDAR_WEEK_FIRST_DAY_OF_WEEK);
		final int minimalDaysInFirstWeek = _prefStore.getInt(ITourbookPreferences.CALENDAR_WEEK_MIN_DAYS_IN_FIRST_WEEK);

		_calendar.setFirstDayOfWeek(firstDayOfWeek);
		_calendar.setMinimalDaysInFirstWeek(minimalDaysInFirstWeek);

		_calendar.set(year, month - 1, tourDay);

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
	public void setWorldPixelForWayPoints(	final HashMap<Integer, Point> worldPositions,
											final int zoomLevel,
											final String projectionId) {

		_twpWorldPosition.put(projectionId.hashCode() + zoomLevel, worldPositions);
	}

	@Override
	public String toString() {

		return new StringBuilder()//
				.append("[TourData]") //$NON-NLS-1$
				.append(" tourId:") //$NON-NLS-1$
				.append(tourId)
				.append(" object:") //$NON-NLS-1$
				.append(super.toString())
				.append(" identityHashCode:") //$NON-NLS-1$
				.append(System.identityHashCode(this))
				.toString();
	}

	public String toStringWithHash() {

		final StringBuilder sb = new StringBuilder();

		sb.append("   tourId:");//$NON-NLS-1$
		sb.append(tourId);
		sb.append("   identityHashCode:");//$NON-NLS-1$
		sb.append(System.identityHashCode(this));

		return sb.toString();
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

}
