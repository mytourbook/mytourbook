/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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
import java.util.Calendar;
import java.util.Collection;
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
import net.tourbook.chart.ChartLabel;
import net.tourbook.database.FIELD_VALIDATION;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageComputedValues;
import net.tourbook.srtm.ElevationSRTM3;
import net.tourbook.srtm.GeoLat;
import net.tourbook.srtm.GeoLon;
import net.tourbook.srtm.NumberForm;
import net.tourbook.ui.UI;
import net.tourbook.ui.tourChart.ChartLayer2ndAltiSerie;
import net.tourbook.ui.views.tourDataEditor.TourDataEditorView;
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

	public static final int				DB_LENGTH_TOUR_TITLE			= 255;
	public static final int				DB_LENGTH_TOUR_DESCRIPTION		= 4096;
	public static final int				DB_LENGTH_TOUR_DESCRIPTION_V10	= 32000;
	public static final int				DB_LENGTH_TOUR_START_PLACE		= 255;
	public static final int				DB_LENGTH_TOUR_END_PLACE		= 255;
	public static final int				DB_LENGTH_TOUR_IMPORT_FILE_PATH	= 255;
	public static final int				DB_LENGTH_WEATHER_CLOUDS		= 255;
	public static final int				DB_LENGTH_DEVICE_TOUR_TYPE		= 2;
	public static final int				DB_LENGTH_DEVICE_PLUGIN_ID		= 255;
	public static final int				DB_LENGTH_DEVICE_PLUGIN_NAME	= 255;
	public static final int				DB_LENGTH_DEVICE_MODE_NAME		= 255;

	/**
	 * 
	 */
	public static final int				MIN_TIMEINTERVAL_FOR_MAX_SPEED	= 20;

	public static final float			MAX_BIKE_SPEED					= 120f;

	/**
	 * Device Id for manually created tours
	 */
	public static final String			DEVICE_ID_FOR_MANUAL_TOUR		= "manual";								//$NON-NLS-1$

	/**
	 * Device id for csv files which behave like manually created tours, marker and timeslices are
	 * disabled because they are not available, tour duration can be edited<br>
	 * this is the id of the deviceDataReader
	 */
	public static final String			DEVICE_ID_CSV_TOUR_DATA_READER	= "net.tourbook.device.CSVTourDataReader";	//$NON-NLS-1$

	/*
	 * initialize SRTM
	 */
	@SuppressWarnings("unused")
	@Transient
	private static final NumberForm		srtmNumberForm					= new NumberForm();

	@Transient
	private static final ElevationSRTM3	elevationSRTM3					= new ElevationSRTM3();

	@Transient
	private static IPreferenceStore		_prefStore						= TourbookPlugin
																				.getDefault()
																				.getPreferenceStore();

	@Transient
	private final Calendar				_calendar						= GregorianCalendar.getInstance();

	/**
	 * entity id which identifies the tour
	 */
	@Id
	private Long						tourId;

	/**
	 * year of tour start
	 */
	private short						startYear;

	/**
	 * mm (d) month of tour
	 */
	private short						startMonth;

	/**
	 * dd (d) day of tour
	 */
	private short						startDay;

	/**
	 * HH (d) hour of tour
	 */
	private short						startHour;

	/**
	 * MM (d) minute of tour
	 */
	private short						startMinute;

	/**
	 * altitude difference for the merged tour
	 */
	private int							startSecond;																// db-version 7

	/**
	 * THIS IS NOT UNUSED !!!<br>
	 * <br>
	 * week of the tour provided by {@link Calendar#get(int)}
	 */
	@SuppressWarnings("unused")
	private short						startWeek;

	/**
	 * THIS IS NOT UNUSED !!!<br>
	 * <br>
	 * this field can be read with sql statements <br>
	 * year for startWeek
	 */
	@SuppressWarnings("unused")
	private short						startWeekYear;

	/**
	 * total distance of the device at tour start (km) tttt (h), the distance for the tour is stored
	 * the field tourDistance
	 */
	private int							startDistance;

	/**
	 * ssss distance msw
	 * <p>
	 * is not used any more since 6.12.2006 but it's necessary then it's a field in the database
	 */
	@SuppressWarnings("unused")
	private int							distance;

	/**
	 * A flag indicating that the distance of this series is defined by a distance sensor and not
	 * from the GPS device.<br>
	 * <br>
	 * 0 == false, 1 == true
	 */
	private short						isDistanceFromSensor			= 0;

	/**
	 * aaaa (h) initial altitude (m)
	 */
	private short						startAltitude;

	/**
	 * pppp (h) initial pulse (bpm)
	 */
	private short						startPulse;

	/**
	 * tolerance for the Douglas Peucker algorithm
	 */
	private short						dpTolerance						= 50;

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
	private String						deviceTourType;

	/*
	 * data from the device
	 */
	private long						deviceTravelTime;

	@SuppressWarnings("unused")
	private int							deviceDistance;

	private int							deviceWheel;

	private int							deviceWeight;

	@SuppressWarnings("unused")
	private int							deviceTotalUp;

	@SuppressWarnings("unused")
	private int							deviceTotalDown;

	/**
	 * total distance of the tour in meters (metric system), this value is computed from the
	 * distance data serie
	 */
	@XmlElement
	private int							tourDistance;

	/**
	 * total recording time in seconds
	 */
	@XmlElement
	private int							tourRecordingTime;

	/**
	 * total driving time in seconds
	 */
	@XmlElement
	private int							tourDrivingTime;

	/**
	 * altitude up (m)
	 */
	@XmlElement
	private int							tourAltUp;

	/**
	 * altitude down (m)
	 */
	@XmlElement
	private int							tourAltDown;

	/**
	 * plugin id for the device which was used to create this tour
	 */
	private String						devicePluginId;

	/**
	 * Profile used by the device
	 */
	private short						deviceMode;																// db-version 3

	/**
	 * time difference between 2 time slices or <code>-1</code> for GPS devices when the time slices
	 * are unequally
	 */
	private short						deviceTimeInterval				= -1;										// db-version 3

	/**
	 * maximum altitude in metric system
	 */
	@XmlElement
	private int							maxAltitude;																// db-version 4

	@XmlElement
	private int							maxPulse;																	// db-version 4

	/**
	 * maximum speed in metric system
	 */
	@XmlElement
	private float						maxSpeed;																	// db-version 4

	@XmlElement
	private int							avgPulse;																	// db-version 4

	@XmlElement
	private int							avgCadence;																// db-version 4
	private int							avgTemperature;															// db-version 4

	private int							weatherWindDir;															// db-version 8
	private int							weatherWindSpd;															// db-version 8
	private String						weatherClouds;																// db-version 8
	@XmlElement
	private int							restPulse;																	// db-version 8

	@XmlElement
	private String						tourTitle;																	// db-version 4
	@XmlElement
	private String						tourDescription;															// db-version 4

	@XmlElement
	private String						tourStartPlace;															// db-version 4
	@XmlElement
	private String						tourEndPlace;																// db-version 4

	@XmlElement
	private Integer						calories;																	// db-version 4
	private float						bikerWeight;																// db-version 4

	/**
	 * visible name for the used plugin to import the data
	 */
	private String						devicePluginName;															// db-version 4
	/**
	 * visible name for {@link #deviceMode}
	 */
	private String						deviceModeName;															// db-version 4

	/**
	 * file path for the imported tour
	 */
	private String						tourImportFilePath;														// db-version 6

	/**
	 * when a tour is merged with another tour, {@link #mergeSourceTourId} contains the tour id of
	 * the tour which is merged into this tour
	 */
	private Long						mergeSourceTourId;															// db-version 7

	/**
	 * when a tour is merged into another tour, {@link #mergeTargetTourId} contains the tour id of
	 * the tour into which this tour is merged
	 */
	private Long						mergeTargetTourId;															// db-version 7

	/**
	 * positive or negative time offset in seconds for the merged tour
	 */
	private int							mergedTourTimeOffset;														// db-version 7

	/**
	 * altitude difference for the merged tour
	 */
	private int							mergedAltitudeOffset;														// db-version 7

	/**
	 * Date/Time when tour data was created. This value is set to the tour start date before db
	 * version 11, otherwise the value is set when the tour is saved the first time.
	 */
	private long						dateTimeCreated;															// db-version 11

	/**
	 * Date/Time when tour data was modified, default value is 0
	 */
	private long						dateTimeModified;															// db-version 11

	/**
	 * data series for time, speed, altitude,...
	 */
	@Basic(optional = false)
	private SerieData					serieData;

	/**
	 * Tour marker
	 */
	@OneToMany(fetch = FetchType.EAGER, cascade = ALL, mappedBy = "tourData")
	@Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
	@XmlElementWrapper(name = "TourMarkers")
	@XmlElement(name = "TourMarker")
	private Set<TourMarker>				tourMarkers						= new HashSet<TourMarker>();

	/**
	 * Way points
	 */
	@OneToMany(fetch = FetchType.EAGER, cascade = ALL, mappedBy = "tourData")
	@Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
	private final Set<TourWayPoint>		tourWayPoints					= new HashSet<TourWayPoint>();

	/**
	 * Reference tours
	 */
	@OneToMany(fetch = FetchType.EAGER, cascade = ALL, mappedBy = "tourData")
	@Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
	private final Set<TourReference>	tourReferences					= new HashSet<TourReference>();

	/**
	 * Tags
	 */
	@ManyToMany(fetch = EAGER)
	@JoinTable(inverseJoinColumns = @JoinColumn(name = "tourTag_tagId", referencedColumnName = "tagId"))
	private Set<TourTag>				tourTags						= new HashSet<TourTag>();

	/**
	 * Category of the tour, e.g. bike, mountainbike, jogging, inlinescating
	 */
	@ManyToOne
	private TourType					tourType;

	/**
	 * Person which created this tour or <code>null</code> when the tour is not saved in the
	 * database
	 */
	@ManyToOne
	private TourPerson					tourPerson;

	/**
	 * plugin id for the device which was used for this tour Bike used for this tour
	 */
	@ManyToOne
	private TourBike					tourBike;

	/*
	 * tourCategory is currently (version 1.6) not used but is defined in older databases, it is
	 * disabled because the field is not available in the database table
	 */
	//	@ManyToMany(fetch = FetchType.LAZY, mappedBy = "tourData")
	//	private Set<TourCategory>	tourCategory					= new HashSet<TourCategory>();
	//
	//
	/////////////////////////////////////////////////////////////////////
	/*
	 * ........................TRANSIENT.DATA............................
	 */
	/////////////////////////////////////////////////////////////////////
	//
	//
	/**
	 * Contains time in seconds relativ to the tour start which is defined in: {@link #startYear},
	 * {@link #startMonth}, {@link #startDay}, {@link #startHour}, {@link #startMinute} and
	 * {@link #startSecond}.
	 * <p>
	 * {@link #timeSerie} is <code>null</code> for a manually created tour
	 */
	@Transient
	public int[]						timeSerie;

	/**
	 * contains the absolute distance in m (metric system)
	 */
	@Transient
	public int[]						distanceSerie;

	/**
	 * contains the absolute distance in miles/1000 (imperial system)
	 */
	@Transient
	private int[]						distanceSerieImperial;

	/**
	 * contains the absolute altitude in m (metric system)
	 */
	@Transient
	public int[]						altitudeSerie;

	/**
	 * contains the absolute altitude in feet (imperial system)
	 */
	@Transient
	private int[]						altitudeSerieImperial;

	/**
	 * SRTM altitude values, when <code>null</code> srtm data have not yet been attached, when
	 * length()==0 data are invalid
	 */
	@Transient
	private int[]						srtmSerie;

	@Transient
	private int[]						srtmSerieImperial;

	@Transient
	public int[]						cadenceSerie;

	@Transient
	public int[]						pulseSerie;

	/**
	 * contains the temperature in the metric measurement system
	 */
	@Transient
	public int[]						temperatureSerie;

	/**
	 * contains the temperature in the imperial measurement system
	 */
	@Transient
	private int[]						temperatureSerieImperial;

	/**
	 * the metric speed serie is required form computing the power even if the current measurement
	 * system is imperial
	 */
	@Transient
	private int[]						speedSerie;

	@Transient
	private int[]						speedSerieImperial;

	/**
	 * Is <code>true</code> when the data in {@link #speedSerie} are from the device and not
	 * computed. Speed data are normally available from an ergometer and not from a bike computer
	 */
	@Transient
	private boolean						isSpeedSerieFromDevice			= false;

	/**
	 * pace in min/km
	 */
	@Transient
	private int[]						paceSerieMinute;

	/**
	 * pace in sec/km
	 */
	@Transient
	private int[]						paceSerieSeconds;

	/**
	 * pace in min/mile
	 */
	@Transient
	private int[]						paceSerieMinuteImperial;

	/**
	 * pace in sec/mile
	 */
	@Transient
	private int[]						paceSerieSecondsImperial;

	@Transient
	private int[]						powerSerie;

	/**
	 * Is <code>true</code> when the data in {@link #powerSerie} are from the device and not
	 * computed. Power data are normally available from an ergometer and not from a bike computer
	 */
	@Transient
	private boolean						isPowerSerieFromDevice			= false;

	@Transient
	private int[]						altimeterSerie;

	@Transient
	private int[]						altimeterSerieImperial;

	@Transient
	public int[]						gradientSerie;

	/*
	 * computed data series
	 */

	@Transient
	public int[]						tourCompareSerie;

	/*
	 * GPS data
	 */
	@Transient
	public double[]						latitudeSerie;

	@Transient
	public double[]						longitudeSerie;

	/**
	 * contains the bounds of the tour in latitude/longitude
	 */
	@Transient
	public Rectangle					gpsBounds;

	/**
	 * Index of the segmented data in the data series
	 */
	@Transient
	public int[]						segmentSerieIndex;

	/**
	 * oooo (o) DD-record // offset
	 */
	@Transient
	public int							offsetDDRecord;

	/*
	 * data for the tour segments
	 */
	@Transient
	public int[]						segmentSerieTimeTotal;
	@Transient
	public int[]						segmentSerieRecordingTime;
	@Transient
	public int[]						segmentSerieDrivingTime;
	@Transient
	public int[]						segmentSerieBreakTime;

	@Transient
	public int[]						segmentSerieDistanceDiff;
	@Transient
	public int[]						segmentSerieDistanceTotal;

	@Transient
	public int[]						segmentSerieAltitudeDiff;
	@Transient
	public int[]						segmentSerieComputedAltitudeDiff;

	@Transient
	public float[]						segmentSerieAltitudeUpH;
	@Transient
	public int[]						segmentSerieAltitudeDownH;

	@Transient
	public float[]						segmentSerieSpeed;
	@Transient
	public float[]						segmentSeriePace;
	@Transient
	public float[]						segmentSeriePaceDiff;

	@Transient
	public float[]						segmentSeriePower;
	@Transient
	public float[]						segmentSerieGradient;

	@Transient
	public float[]						segmentSeriePulse;

	@Transient
	public float[]						segmentSerieCadence;
	/**
	 * contains the filename from which the data are imported, when set to <code>null</code> the
	 * data are not imported they are from the database
	 */
	@Transient
	public String						importRawDataFile;

	/**
	 * Latitude for the center position in the map or {@link Double#MIN_VALUE} when the position is
	 * not set
	 */
	@Transient
	public double						mapCenterPositionLatitude		= Double.MIN_VALUE;

	/**
	 * Longitude for the center position in the map or {@link Double#MIN_VALUE} when the position is
	 * not set
	 */
	@Transient
	public double						mapCenterPositionLongitude		= Double.MIN_VALUE;

	/**
	 * Zoomlevel in the map
	 */
	@Transient
	public int							mapZoomLevel;

	@Transient
	public double						mapMinLatitude;

	@Transient
	public double						mapMaxLatitude;

	@Transient
	public double						mapMinLongitude;

	@Transient
	public double						mapMaxLongitude;

	/**
	 * caches the world positions for lat/long values for each zoom level
	 */
	@Transient
	private final Map<Integer, Point[]>	_worldPosition					= new HashMap<Integer, Point[]>();

	/**
	 * when a tour was deleted and is still visible in the raw data view, resaving the tour or
	 * finding the tour in the entity manager causes lots of trouble with hibernate, therefor this
	 * tour cannot be saved again, it must be reloaded from the file system
	 */
	@Transient
	public boolean						isTourDeleted					= false;

	/**
	 * 2nd data serie, this is used in the {@link ChartLayer2ndAltiSerie} to display the merged tour
	 * or the adjusted altitude
	 */
	@Transient
	public int[]						dataSerie2ndAlti;

	/**
	 * altitude difference between this tour and the merge tour with metric measurement
	 */
	@Transient
	public int[]						dataSerieDiffTo2ndAlti;

	/**
	 * contains the altitude serie which is adjusted
	 */
	@Transient
	public int[]						dataSerieAdjustedAlti;

	/**
	 * contains special data points
	 */
	@Transient
	public SplineData					splineDataPoints;

	/**
	 * Contains a spline data serie
	 */
	@Transient
	public float[]						dataSerieSpline;

	/**
	 * when a tour is not saved, the tour id is not defined, therefore the tour data are provided
	 * from the import view when tours are merged to display the merge layer
	 */
	@Transient
	private TourData					_mergeSourceTourData;

	@Transient
	private DateTime					_dateTimeCreated;

	@Transient
	private DateTime					_dateTimeModified;

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
		if ((latitudeSerie != null) && (longitudeSerie != null)) {

			for (int timeIndex = 0; timeIndex < timeSerie.length; timeIndex++) {
				if ((latitudeSerie[timeIndex] != Double.MIN_VALUE) && (longitudeSerie[timeIndex] != Double.MIN_VALUE)) {
					mapMinLatitude = mapMaxLatitude = latitudeSerie[timeIndex] + 90;
					mapMinLongitude = mapMaxLongitude = longitudeSerie[timeIndex] + 180;
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

				final int temp = temperatureSerie[serieIndex];

				if (temp == Integer.MIN_VALUE) {
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

			if ((latitudeSerie != null) && (longitudeSerie != null)) {

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

		altitudeSerieImperial = null;
//
//		srtmSerie = null;
//		srtmSerieImperial = null;
	}

	/**
	 * clear computed data series so the next time when they are needed they will be recomputed
	 */
	public void clearComputedSeries() {

		if (isSpeedSerieFromDevice == false) {
			speedSerie = null;
		}
		if (isPowerSerieFromDevice == false) {
			powerSerie = null;
		}

		paceSerieMinute = null;
		paceSerieSeconds = null;
		paceSerieMinuteImperial = null;
		paceSerieSecondsImperial = null;

		altimeterSerie = null;
		gradientSerie = null;

		speedSerieImperial = null;
		paceSerieMinuteImperial = null;
		altimeterSerieImperial = null;
		altitudeSerieImperial = null;

		srtmSerie = null;
		srtmSerieImperial = null;
	}

	/**
	 * clears the cached world positions, this is necessary when the data serie have been modified
	 */
	public void clearWorldPositions() {
		_worldPosition.clear();
	}

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

		// optimization: don't recreate the data series when they are available
		if ((altimeterSerie != null) && (altimeterSerieImperial != null) && (gradientSerie != null)) {
			return;
		}

		if (deviceTimeInterval == -1) {
			computeAltimeterGradientSerieWithVariableInterval();
		} else {
			computeAltimeterGradientSerieWithFixedInterval();
		}
	}

	/**
	 * Computes the data serie for altimeters with the internal algorithm for a fix time interval
	 */
	private void computeAltimeterGradientSerieWithFixedInterval() {

		if ((distanceSerie == null) || (altitudeSerie == null)) {
			return;
		}

		final int serieLength = timeSerie.length;

		final int dataSerieAltimeter[] = new int[serieLength];
		final int dataSerieGradient[] = new int[serieLength];

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

			final int distanceDiff = distanceSerie[indexHigh] - distanceSerie[indexLow];
			final int altitudeDiff = altitudeSerie[indexHigh] - altitudeSerie[indexLow];

			final float timeDiff = deviceTimeInterval * (indexHigh - indexLow);

			// keep altimeter data
			dataSerieAltimeter[serieIndex] = (int) (3600F * altitudeDiff / timeDiff / UI.UNIT_VALUE_ALTITUDE);

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

		if ((distanceSerie == null) || (altitudeSerie == null)) {
			return;
		}

		final int[] checkSpeedSerie = getSpeedSerie();

		final int serieLength = timeSerie.length;
		final int serieLengthLast = serieLength - 1;

		final int dataSerieAltimeter[] = new int[serieLength];
		final int dataSerieGradient[] = new int[serieLength];

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

		for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

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
			int distanceDiff = distanceSerie[highIndex] - distanceSerie[lowIndex];
			int altitudeDiff = altitudeSerie[highIndex] - altitudeSerie[lowIndex];

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
					final int altimeter = (int) (3600f * altitudeDiff / timeDiff / UI.UNIT_VALUE_ALTITUDE);
					dataSerieAltimeter[serieIndex] = altimeter;
				} else {
//					dataSerieAltimeter[serieIndex] = -100;
				}

				// compute gradient
				if (distanceDiff > 0) {
					final int gradient = altitudeDiff * 1000 / distanceDiff;
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

		final int prefMinAltitude = TourbookPlugin.getDefault()//
				.getPreferenceStore()
				.getInt(PrefPageComputedValues.STATE_COMPUTED_VALUE_MIN_ALTITUDE);

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

		int prevAltitude = 0;
		int prevSegmentAltitude = 0;
		int prevAltiDiff = 0;

		int angleAltiUp = 0;
		int angleAltiDown = 0;

		int segmentAltitudeMin = 0;
		int segmentAltitudeMax = 0;

		int altitudeUpTotal = 0;
		int altitudeDownTotal = 0;

		final int serieLength = timeSerie.length;
		int currentSegmentSerieIndex = 0;

		for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

			final int altitude = altitudeSerie[serieIndex];
			int altiDiff = 0;

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

					int segmentMinMaxDiff = segmentAltitudeMax - segmentAltitudeMin;
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

							final int segmentAltiDiff = segmentAltitudeMin - segmentAltitudeMax;
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

							final int segmentAltiDiff = segmentAltitudeMax - segmentAltitudeMin;
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

		long cadenceSum = 0;
		int cadenceCount = 0;

		for (final int cadence : cadenceSerie) {
			if (cadence > 0) {
				cadenceCount++;
				cadenceSum += cadence;
			}
		}
		if (cadenceCount > 0) {
			avgCadence = (int) cadenceSum / cadenceCount;
		}
	}

	private void computeAvgPulse() {

		if ((pulseSerie == null) || (pulseSerie.length == 0) || (timeSerie == null) || (timeSerie.length == 0)) {
			return;
		}

		avgPulse = computeAvgPulseSegment(0, timeSerie.length - 1);
	}

	private int computeAvgPulseSegment(final int firstIndex, final int lastIndex) {

		// check if data are available
		if ((pulseSerie == null) || (pulseSerie.length == 0) || (timeSerie == null) || (timeSerie.length == 0)) {
			return 0;
		}

		// check for 1 point
		if (firstIndex == lastIndex) {
			return pulseSerie[firstIndex];
		}

		// check for 2 points
		if (lastIndex - firstIndex == 1) {
			return (int) (((float) pulseSerie[firstIndex] + pulseSerie[lastIndex]) / 2 + 0.5f);
		}

		// at least 3 points are available
		int prevTime = timeSerie[firstIndex];
		int currentTime = timeSerie[firstIndex];
		int nextTime = timeSerie[firstIndex + 1];

		float pulseSquare = 0;
		float timeSquare = 0;

		for (int serieIndex = firstIndex; serieIndex <= lastIndex; serieIndex++) {

			final float pulse = pulseSerie[serieIndex];

			float timeDiffPrev = 0;
			float timeDiffNext = 0;

			if (serieIndex > firstIndex) {
				// prev is available
				timeDiffPrev = ((float) currentTime - prevTime) / 2;
			}

			if (serieIndex < lastIndex) {
				// next is available
				timeDiffNext = ((float) nextTime - currentTime) / 2;
			}

			if (pulse > 0) {
				pulseSquare += pulse * timeDiffPrev + pulse * timeDiffNext;
				timeSquare += timeDiffPrev + timeDiffNext;
			}

			if (serieIndex < lastIndex) {
				prevTime = currentTime;
				currentTime = nextTime;
				nextTime = timeSerie[serieIndex + 1];
			}
		}

		return timeSquare == 0f ? 0 : (int) (pulseSquare / timeSquare + 0.5f);
	}

	private void computeAvgTemperature() {

		if (temperatureSerie == null) {
			return;
		}

		long temperatureSum = 0;
		int tempLength = temperatureSerie.length;

		for (final int temperature : temperatureSerie) {
			if (temperature == Integer.MIN_VALUE) {
				// ignore invalid values
				tempLength--;
			} else {
				temperatureSum += temperature;
			}
		}

		if (tempLength > 0) {
			avgTemperature = (int) temperatureSum / tempLength;
		}
	}

	private int computeBreakTimeVariable(final int minStopTime, final int startIndex, int endIndex) {

		endIndex = Math.min(endIndex, timeSerie.length - 1);

		int lastMovingDistance = 0;
		int lastMovingTime = 0;

		int totalBreakTime = 0;
		int breakTime = 0;
		int currentBreakTime = 0;

		for (int serieIndex = startIndex; serieIndex <= endIndex; serieIndex++) {

			final int currentDistance = distanceSerie[serieIndex];
			final int currentTime = timeSerie[serieIndex];

			final int timeDiff = currentTime - lastMovingTime;
			final int distDiff = currentDistance - lastMovingDistance;

			if ((distDiff == 0) || ((timeDiff > minStopTime) && (distDiff < 10))) {

				// distance has not changed, check if a longer stop is done
				// speed must be greater than 1.8 km/h (10m in 20 sec)

				final int breakDiff = currentTime - currentBreakTime;

				breakTime += breakDiff;

				if (timeDiff > minStopTime) {

					// person has stopped for a break
					totalBreakTime += breakTime;

					breakTime = 0;
					currentBreakTime = currentTime;
				}

			} else {

				// keep time and distance when the distance is changing
				lastMovingTime = currentTime;
				lastMovingDistance = currentDistance;

				breakTime = 0;
				currentBreakTime = currentTime;
			}
		}

		return totalBreakTime;
	}

//	/**
//	 * compute altitude up/down which was used until version 9.07.0
//	 */
//	private void computeAltitudeUpDownWithTime() {
//
//		if (altitudeSerie == null || timeSerie == null || timeSerie.length < 2) {
//			return;
//		}
//
//		final int serieLength = timeSerie.length;
//
//		int lastTime = 0;
//		int currentAltitude = altitudeSerie[0];
//		int lastAltitude = currentAltitude;
//
//		int altitudeUp = 0;
//		int altitudeDown = 0;
//
//		final int minTimeDiff = 10;
//
//		for (int timeIndex = 0; timeIndex < serieLength; timeIndex++) {
//
//			final int currentTime = timeSerie[timeIndex];
//
//			final int timeDiff = currentTime - lastTime;
//
//			currentAltitude = altitudeSerie[timeIndex];
//
//			if (timeDiff >= minTimeDiff) {
//
//				final int altitudeDiff = currentAltitude - lastAltitude;
//
//				if (altitudeDiff >= 0) {
//					altitudeUp += altitudeDiff;
//				} else {
//					altitudeDown += altitudeDiff;
//				}
//
//				lastTime = currentTime;
//				lastAltitude = currentAltitude;
//			}
//		}
//
//		setTourAltUp(altitudeUp);
//		setTourAltDown(-altitudeDown);
//	}

	/**
	 * compute maximum and average fields
	 */
	public void computeComputedValues() {

		computeMaxAltitude();
		computeMaxPulse();
		computeMaxSpeed();

		computeAvgPulse();
		computeAvgCadence();
		computeAvgTemperature();
	}

	private void computeMaxAltitude() {

		if (altitudeSerie == null) {
			return;
		}

		int maxAltitude = 0;
		for (final int altitude : altitudeSerie) {
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

		int maxPulse = 0;

		for (final int pulse : pulseSerie) {
			if (pulse > maxPulse) {
				maxPulse = pulse;
			}
		}
		this.maxPulse = maxPulse;
	}

	private void computeMaxSpeed() {
		if (distanceSerie != null) {
			computeSpeedSerie();
		}
	}

	/**
	 * computes the speed data serie which can be retrieved with {@link TourData#getSpeedSerie()}
	 */
	public void computeSpeedSerie() {

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

			if (deviceTimeInterval == -1) {
				computeSpeedSerieInternalWithVariableInterval();
			} else {
				computeSpeedSerieInternalWithFixedInterval();
			}
		}
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

		speedSerieImperial = new int[serieLength];

		for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

			/*
			 * speed
			 */

			final int speedMetric = speedSerie[serieIndex];

			speedSerieImperial[serieIndex] = ((int) (speedMetric / UI.UNIT_MILE));
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

		if (distanceSerie == null) {
			return;
		}

		final int serieLength = timeSerie.length;

		speedSerie = new int[serieLength];
		speedSerieImperial = new int[serieLength];

		paceSerieMinute = new int[serieLength];
		paceSerieSeconds = new int[serieLength];
		paceSerieMinuteImperial = new int[serieLength];
		paceSerieSecondsImperial = new int[serieLength];

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

			final int distanceDefault = distanceSerie[distIndexHigh] - distanceSerie[distIndexLow];

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

			final int distDiff = distanceSerie[distIndexHigh] - distanceSerie[distIndexLow];
			final float timeDiff = timeSerie[distIndexHigh] - timeSerie[distIndexLow];

			/*
			 * speed
			 */
			int speedMetric = 0;
			int speedImperial = 0;
			if (timeDiff != 0) {
				final float speed = (distDiff * 36F) / timeDiff;
				speedMetric = (int) (speed);
				speedImperial = (int) (speed / UI.UNIT_MILE);
			}

			speedSerie[serieIndex] = speedMetric;
			speedSerieImperial[serieIndex] = speedImperial;

			maxSpeed = Math.max(maxSpeed, speedMetric);

			/*
			 * pace (computed with divisor 10)
			 */
			float paceMetricSeconds = 0;
			float paceImperialSeconds = 0;
			int paceMetricMinute = 0;
			int paceImperialMinute = 0;

			if ((speedMetric != 0) && (distDiff != 0)) {

//				final float pace = timeDiff * 166.66f / distDiff;
//				final float pace = 10 * (((float) timeDiff / 60) / ((float) distDiff / 1000));

//				paceMetricSeconds = 10* timeDiff * 1000 / (float) distDiff;
				paceMetricSeconds = timeDiff * 10000 / distDiff;
				paceImperialSeconds = paceMetricSeconds * UI.UNIT_MILE;

//				paceMetricMinute = (int) ((paceMetricSeconds / 60));
				paceMetricMinute = (int) ((paceMetricSeconds / 60));
				paceImperialMinute = (int) ((paceImperialSeconds / 60));
			}

			paceSerieMinute[serieIndex] = paceMetricMinute;
			paceSerieMinuteImperial[serieIndex] = paceImperialMinute;

			paceSerieSeconds[serieIndex] = (int) paceMetricSeconds / 10;
			paceSerieSecondsImperial[serieIndex] = (int) paceImperialSeconds / 10;
		}

		maxSpeed /= 10;
	}

	/**
	 * compute the speed when the time serie has unequal time intervalls
	 */
	private void computeSpeedSerieInternalWithVariableInterval() {

		if (distanceSerie == null) {
			return;
		}

		int minTimeDiff;
		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

		minTimeDiff = prefStore.getInt(ITourbookPreferences.APP_DATA_SPEED_MIN_TIMESLICE_VALUE);

		final int serieLength = timeSerie.length;
		final int lastSerieIndex = serieLength - 1;

		speedSerie = new int[serieLength];
		speedSerieImperial = new int[serieLength];

		paceSerieMinute = new int[serieLength];
		paceSerieSeconds = new int[serieLength];
		paceSerieMinuteImperial = new int[serieLength];
		paceSerieSecondsImperial = new int[serieLength];

		final boolean isCheckPosition = (latitudeSerie != null) && //
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
			int distDiff = distanceSerie[highIndex] - distanceSerie[lowIndex];

			// check if a lat and long diff is available
			if (isCheckPosition && (serieIndex > 0) && (serieIndex < lastSerieIndex - 1)) {

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
					final int equalDistDiff = distanceSerie[serieIndex] - distanceSerie[equalStartIndex];

					int speedMetric = 0;
					int speedImperial = 0;

					if ((equalTimeDiff > 20) && (equalDistDiff < 10)) {
						// speed must be greater than 1.8 km/h
					} else {

						for (int equalSerieIndex = equalStartIndex + 1; equalSerieIndex < serieIndex; equalSerieIndex++) {

							final int equalSegmentTimeDiff = timeSerie[equalSerieIndex]
									- timeSerie[equalSerieIndex - 1];

							final int equalSegmentDistDiff = equalTimeDiff == 0 ? 0 : //
									(int) (((float) equalSegmentTimeDiff / equalTimeDiff) * equalDistDiff);

							distanceSerie[equalSerieIndex] = distanceSerie[equalSerieIndex - 1] + equalSegmentDistDiff;

							// compute speed for this segment
							if ((equalSegmentTimeDiff == 0) || (equalSegmentDistDiff == 0)) {
								speedMetric = 0;
							} else {
								speedMetric = (int) ((equalSegmentDistDiff * 36f) / equalSegmentTimeDiff);
								speedMetric = speedMetric < 0 ? 0 : speedMetric;

								speedImperial = (int) ((equalSegmentDistDiff * 36f) / (equalSegmentTimeDiff * UI.UNIT_MILE));
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

				// check array scope
				if ((lowIndex < 0) || (highIndex >= serieLength)) {
					break;
				}

				timeDiff = timeSerie[highIndex] - timeSerie[lowIndex];
				distDiff = distanceSerie[highIndex] - distanceSerie[lowIndex];
			}

			/*
			 * speed
			 */
			int speedMetric = 0;
			int speedImperial = 0;

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
				if (isCheckPosition && (lowIndex > 0) && (highIndex < lastSerieIndex - 1)) {

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
					speedMetric = (int) ((distDiff * 36f) / timeDiff);
					speedMetric = speedMetric < 0 ? 0 : speedMetric;

					speedImperial = (int) ((distDiff * 36f) / (timeDiff * UI.UNIT_MILE));
					speedImperial = speedImperial < 0 ? 0 : speedImperial;
				}
			}

			setSpeed(serieIndex, speedMetric, speedImperial, timeDiff, distDiff);
		}

		maxSpeed /= 10;
	}

	public void computeTourDrivingTime() {

		if (isManualTour()) {
			// manual tours do not have data series
			return;
		}

		if ((timeSerie == null) || (timeSerie.length == 0)) {
			tourDrivingTime = 0;
		} else {
			tourDrivingTime = Math.max(0, timeSerie[timeSerie.length - 1] - getBreakTime(0, timeSerie.length));
		}
	}

	/**
	 * Create a device marker at the current position
	 * 
	 * @param timeData
	 * @param timeIndex
	 * @param timeAbsolute
	 * @param distanceAbsolute
	 */
	private void createMarker(	final TimeData timeData,
								final int timeIndex,
								final int timeAbsolute,
								final int distanceAbsolute) {

		// create a new marker
		final TourMarker tourMarker = new TourMarker(this, ChartLabel.MARKER_TYPE_DEVICE);

		tourMarker.setVisualPosition(ChartLabel.VISUAL_HORIZONTAL_ABOVE_GRAPH_CENTERED);
		tourMarker.setTime(timeAbsolute + timeData.marker);
		tourMarker.setDistance(distanceAbsolute);
		tourMarker.setSerieIndex(timeIndex);

		if (timeData.markerLabel == null) {
			tourMarker.setLabel(Messages.tour_data_label_device_marker);
		} else {
			tourMarker.setLabel(timeData.markerLabel);
		}

		tourMarkers.add(tourMarker);
	}

	private void createSRTMDataSerie() {

		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {

			public void run() {

				int serieIndex = 0;
				short lastValidSRTM = 0;
				boolean isSRTMValid = false;

				final int serieLength = timeSerie.length;

				final int[] newSRTMSerie = new int[serieLength];
				final int[] newSRTMSerieImperial = new int[serieLength];

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
					newSRTMSerieImperial[serieIndex] = (int) (srtmValue / UI.UNIT_FOOT);

					serieIndex++;
				}

				if (isSRTMValid) {
					srtmSerie = newSRTMSerie;
					srtmSerieImperial = newSRTMSerieImperial;
				} else {
					srtmSerie = new int[0];
				}
			}
		});
	}

	/**
	 * Convert {@link TimeData} into {@link TourData} this will be done after data are imported or
	 * transfered
	 * 
	 * @param isCreateMarker
	 *            creates markers when <code>true</code>
	 */
	public void createTimeSeries(final ArrayList<TimeData> timeDataList, final boolean isCreateMarker) {

		final int serieLength = timeDataList.size();
		if (serieLength == 0) {
			return;
		}

		final TimeData firstTimeDataItem = timeDataList.get(0);

		boolean isDistance = false;
		boolean isAltitude = false;
		boolean isPulse = false;
		boolean isCadence = false;
		boolean isTemperature = false;
		boolean isSpeed = false;
		boolean isPower = false;

		final boolean isAbsoluteData = firstTimeDataItem.absoluteTime != Long.MIN_VALUE;

		/*
		 * time serie is always available
		 */
		timeSerie = new int[serieLength];

		/*
		 * create data series only when data are available
		 */
		if ((firstTimeDataItem.distance != Integer.MIN_VALUE) || isAbsoluteData) {
			distanceSerie = new int[serieLength];
			isDistance = true;
		}

		/*
		 * altitude serie
		 */
		if (isAbsoluteData) {

			if (firstTimeDataItem.absoluteAltitude == Integer.MIN_VALUE) {

				// search for first altitude value

				int firstAltitudeIndex = 0;
				for (final TimeData timeData : timeDataList) {
					if (timeData.absoluteAltitude != Integer.MIN_VALUE) {

						// altitude was found

						altitudeSerie = new int[serieLength];
						isAltitude = true;

						// set altitude to the first available altitude value

						final int firstAltitudeValue = (int) timeData.absoluteAltitude;

						for (int valueIndex = 0; valueIndex < firstAltitudeIndex; valueIndex++) {
							altitudeSerie[valueIndex] = firstAltitudeValue;
						}
						break;
					}

					firstAltitudeIndex++;
				}

			} else {

				// altitude is available

				altitudeSerie = new int[serieLength];
				isAltitude = true;
			}

		} else if (firstTimeDataItem.altitude != Integer.MIN_VALUE) {

			// altitude is available

			altitudeSerie = new int[serieLength];
			isAltitude = true;
		}

		/*
		 * pulse serie
		 */
		if (firstTimeDataItem.pulse == Integer.MIN_VALUE) {

			// search for first pulse value

			for (final TimeData timeData : timeDataList) {
				if (timeData.pulse != Integer.MIN_VALUE) {

					// pulse was found

					pulseSerie = new int[serieLength];
					isPulse = true;

					break;
				}
			}

		} else {

			// pulse is available

			pulseSerie = new int[serieLength];
			isPulse = true;
		}

		/*
		 * cadence serie
		 */
		if (firstTimeDataItem.cadence == Integer.MIN_VALUE) {

			// search for first cadence value

			for (final TimeData timeData : timeDataList) {
				if (timeData.cadence != Integer.MIN_VALUE) {

					// cadence was found

					cadenceSerie = new int[serieLength];
					isCadence = true;

					break;
				}
			}

		} else {

			// cadence is available

			cadenceSerie = new int[serieLength];
			isCadence = true;
		}

		if (firstTimeDataItem.temperature != Integer.MIN_VALUE) {
			temperatureSerie = new int[serieLength];
			isTemperature = true;
		}

		if (firstTimeDataItem.speed != Integer.MIN_VALUE) {
			speedSerie = new int[serieLength];
			isSpeed = true;

			isSpeedSerieFromDevice = true;
		}

		if (firstTimeDataItem.power != Integer.MIN_VALUE) {
			powerSerie = new int[serieLength];
			isPower = true;

			isPowerSerieFromDevice = true;
		}

		// check if GPS data are available
		boolean isGPS = false;
		if (firstTimeDataItem.latitude != Double.MIN_VALUE) {
			isGPS = true;
		} else {

			// check all data if lat/long is available

			for (final TimeData timeDataItem : timeDataList) {
				if (timeDataItem.latitude != Double.MIN_VALUE) {
					isGPS = true;
					break;
				}
			}
		}
		if (isGPS) {
			latitudeSerie = new double[serieLength];
			longitudeSerie = new double[serieLength];
		}

		int timeIndex = 0;

		int recordingTime = 0; // time in seconds

		int altitudeAbsolute = 0;
		int distanceAbsolute = 0;

		if (isAbsoluteData) {

			/*
			 * absolute data are available when data are from GPS devices
			 */

			long firstTime = 0;

			// index when altitude is available in the time data list
			int altitudeStartIndex = -1;

			int distanceDiff;
			int altitudeDiff;

			int lastValidTime = 0;

			/*
			 * get first valid altitude
			 */
			// set initial min/max latitude/longitude
			if ((firstTimeDataItem.latitude == Double.MIN_VALUE) || (firstTimeDataItem.longitude == Double.MIN_VALUE)) {

				// find first valid latitude/longitude
				for (final TimeData timeData : timeDataList) {
					if ((timeData.latitude != Double.MIN_VALUE) && (timeData.longitude != Double.MIN_VALUE)) {
						mapMinLatitude = timeData.latitude + 90;
						mapMaxLatitude = timeData.latitude + 90;
						mapMinLongitude = timeData.longitude + 180;
						mapMaxLongitude = timeData.longitude + 180;
						break;
					}
				}
			} else {
				mapMinLatitude = firstTimeDataItem.latitude + 90;
				mapMaxLatitude = firstTimeDataItem.latitude + 90;
				mapMinLongitude = firstTimeDataItem.longitude + 180;
				mapMaxLongitude = firstTimeDataItem.longitude + 180;
			}
			double lastValidLatitude = mapMinLatitude - 90;
			double lastValidLongitude = mapMinLongitude - 180;

			// convert data from the tour format into interger[] arrays
			for (final TimeData timeData : timeDataList) {

				if ((altitudeStartIndex == -1) && isAltitude) {
					altitudeStartIndex = timeIndex;
					altitudeAbsolute = (int) timeData.absoluteAltitude;
				}

				final long absoluteTime = timeData.absoluteTime;

				if (timeIndex == 0) {

					// first trackpoint

					/*
					 * time
					 */
					timeSerie[timeIndex] = 0;
					if (absoluteTime == Long.MIN_VALUE) {
						firstTime = 0;
					} else {
						firstTime = absoluteTime;
					}

					recordingTime = 0;
					lastValidTime = (int) firstTime;

					/*
					 * distance
					 */
					final float tdDistance = timeData.absoluteDistance;
					if ((tdDistance == Float.MIN_VALUE) || (tdDistance >= Integer.MAX_VALUE)) {
						distanceDiff = 0;
					} else {
						distanceDiff = (int) tdDistance;
					}
					distanceSerie[timeIndex] = distanceAbsolute += distanceDiff < 0 ? 0 : distanceDiff;

					/*
					 * altitude
					 */
					if (isAltitude) {
						altitudeSerie[timeIndex] = altitudeAbsolute;
					}

				} else {

					// 1..n trackpoint

					/*
					 * time
					 */
					if (absoluteTime == Long.MIN_VALUE) {
						recordingTime = lastValidTime;
					} else {
						recordingTime = (int) ((absoluteTime - firstTime) / 1000);
					}
					timeSerie[timeIndex] = lastValidTime = recordingTime;

					/*
					 * distance
					 */
					final float tdDistance = timeData.absoluteDistance;
					if ((tdDistance == Float.MIN_VALUE) || (tdDistance >= Integer.MAX_VALUE)) {
						// ensure to have correct data
						distanceDiff = 0;
					} else {
						/*
						 * Math.round() cannot be used because the tour id contains the last
						 * distance serie value, Math.round() creates another tour id
						 */
						distanceDiff = (int) tdDistance - distanceAbsolute;
					}
					distanceSerie[timeIndex] = distanceAbsolute += distanceDiff < 0 ? 0 : distanceDiff;

					/*
					 * altitude
					 */
					if (isAltitude) {

						if (altitudeStartIndex == -1) {
							altitudeDiff = 0;
						} else {
							final float tdAltitude = timeData.absoluteAltitude;
							if ((tdAltitude == Float.MIN_VALUE) || (tdAltitude >= Integer.MAX_VALUE)) {
								altitudeDiff = 0;
							} else {
								altitudeDiff = (int) (tdAltitude - altitudeAbsolute);
							}
						}
						altitudeSerie[timeIndex] = altitudeAbsolute += altitudeDiff;
					}
				}

				/*
				 * latitude & longitude
				 */
				final double latitude = timeData.latitude;
				final double longitude = timeData.longitude;

				if ((latitudeSerie != null) && (longitudeSerie != null)) {

					if ((latitude == Double.MIN_VALUE) || (longitude == Double.MIN_VALUE)) {
						latitudeSerie[timeIndex] = lastValidLatitude;
						longitudeSerie[timeIndex] = lastValidLongitude;
					} else {

						latitudeSerie[timeIndex] = lastValidLatitude = latitude;
						longitudeSerie[timeIndex] = lastValidLongitude = longitude;
					}

					final double lastValidLatitude90 = lastValidLatitude + 90;
					mapMinLatitude = Math.min(mapMinLatitude, lastValidLatitude90);
					mapMaxLatitude = Math.max(mapMaxLatitude, lastValidLatitude90);

					final double lastValidLongitude180 = lastValidLongitude + 180;
					mapMinLongitude = Math.min(mapMinLongitude, lastValidLongitude180);
					mapMaxLongitude = Math.max(mapMaxLongitude, lastValidLongitude180);
				}

				/*
				 * pulse
				 */
				if (isPulse) {
					final int tdPulse = timeData.pulse;
					pulseSerie[timeIndex] = (tdPulse == Integer.MIN_VALUE) || (tdPulse == Integer.MAX_VALUE)
							? 0
							: tdPulse;
				}

				/*
				 * cadence
				 */
				if (isCadence) {
					final int tdCadence = timeData.cadence;
					cadenceSerie[timeIndex] = (tdCadence == Integer.MIN_VALUE) || (tdCadence == Integer.MAX_VALUE)
							? 0
							: tdCadence;
				}

				/*
				 * temperature
				 */
				if (isTemperature) {
					final int tdTemperature = timeData.temperature;
//					if (tdTemperature == Integer.MIN_VALUE) {
//						System.out.println("tourId:" + tourId + " - tdTemperature is MIN_VALUE"); //$NON-NLS-1$ //$NON-NLS-1$ //$NON-NLS-2$
//					}
					temperatureSerie[timeIndex] = tdTemperature == Integer.MIN_VALUE ? 0 : tdTemperature;
				}

				/*
				 * marker
				 */
				if (isCreateMarker && (timeData.marker != 0)) {
					createMarker(timeData, timeIndex, recordingTime, distanceAbsolute);
				}

				// speed
				if (isSpeed) {
					final int tdSpeed = timeData.speed;
					speedSerie[timeIndex] = tdSpeed == Integer.MIN_VALUE ? 0 : tdSpeed;
				}

				timeIndex++;
			}

			mapMinLatitude -= 90;
			mapMaxLatitude -= 90;
			mapMinLongitude -= 180;
			mapMaxLongitude -= 180;

		} else {

			/*
			 * relativ data are available, these data are from non GPS devices
			 */

			// convert data from the tour format into an interger[]
			for (final TimeData timeData : timeDataList) {

				final int tdTime = timeData.time;
				timeSerie[timeIndex] = recordingTime += tdTime == Integer.MIN_VALUE ? 0 : tdTime;

				if (isDistance) {
					final int tdDistance = timeData.distance;
//					if (tdDistance == Integer.MIN_VALUE) {
//						System.out.println("tourId:" + tourId + " - tdDistance is MIN_VALUE"); //$NON-NLS-1$ //$NON-NLS-1$ //$NON-NLS-2$
//					}
					distanceSerie[timeIndex] = distanceAbsolute += tdDistance == Integer.MIN_VALUE ? 0 : tdDistance;
				}

				if (isAltitude) {
					final int tdAltitude = timeData.altitude;
//					if (tdAltitude == Integer.MIN_VALUE) {
//						System.out.println("tourId:" + tourId + " - tdAltitude is MIN_VALUE"); //$NON-NLS-1$ //$NON-NLS-1$ //$NON-NLS-2$
//					}
					altitudeSerie[timeIndex] = altitudeAbsolute += tdAltitude == Integer.MIN_VALUE ? 0 : tdAltitude;
				}

				if (isPulse) {
					final int tdPulse = timeData.pulse;
//					if (tdPulse == Integer.MIN_VALUE) {
//						System.out.println("tourId:" + tourId + " - tdPulse is MIN_VALUE"); //$NON-NLS-1$ //$NON-NLS-1$ //$NON-NLS-2$
//					}
					pulseSerie[timeIndex] = tdPulse == Integer.MIN_VALUE ? 0 : tdPulse;
				}

				if (isTemperature) {
					final int tdTemperature = timeData.temperature;
//					if (tdTemperature == Integer.MIN_VALUE) {
//						System.out.println("tourId:" + tourId + " - tdTemperature is MIN_VALUE"); //$NON-NLS-1$ //$NON-NLS-1$ //$NON-NLS-2$
//					}
					temperatureSerie[timeIndex] = tdTemperature == Integer.MIN_VALUE ? 0 : tdTemperature;
				}

				if (isCadence) {
					final int tdCadence = timeData.cadence;
//					if (tdCadence == Integer.MIN_VALUE) {
//						System.out.println("tourId:" + tourId + " - tdCadence is MIN_VALUE"); //$NON-NLS-1$ //$NON-NLS-1$ //$NON-NLS-2$
//					}
					cadenceSerie[timeIndex] = tdCadence == Integer.MIN_VALUE ? 0 : tdCadence;
				}

				if (isPower) {
					final int tdPower = timeData.power;
//					if (tdPower == Integer.MIN_VALUE) {
//						System.out.println("tourId:" + tourId + " - tdPower is MIN_VALUE"); //$NON-NLS-1$ //$NON-NLS-1$ //$NON-NLS-2$
//					}
					powerSerie[timeIndex] = tdPower == Integer.MIN_VALUE ? 0 : tdPower;
				}

				if (isSpeed) {
					final int tdSpeed = timeData.speed;
					speedSerie[timeIndex] = tdSpeed == Integer.MIN_VALUE ? 0 : tdSpeed;
				}

				if (isCreateMarker && (timeData.marker != 0)) {
					createMarker(timeData, timeIndex, recordingTime, distanceAbsolute);
				}

				timeIndex++;
			}
		}

		tourDistance = distanceAbsolute;
		tourRecordingTime = recordingTime;

		cleanupDataSeries();
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
	 * @param uniqueKey
	 *            unique key to identify a tour
	 * @return
	 */
	public Long createTourId(final String uniqueKey) {

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
					+ uniqueKey;

			tourId = Long.valueOf(tourIdKey);

		} catch (final NumberFormatException e) {

			/*
			 * the distance is shorted that the maximum of a Long datatype is not exceeded
			 */

			tourIdKey = Short.toString(getStartYear())
					+ Short.toString(getStartMonth())
					+ Short.toString(getStartDay())
					+ Short.toString(getStartHour())
					+ Short.toString(getStartMinute())
					//
					+ uniqueKey.substring(0, Math.min(5, uniqueKey.length()));

			tourId = Long.valueOf(tourIdKey);
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
	 * Create the tour segment list from the segment index array
	 * 
	 * @return
	 */
	public Object[] createTourSegments() {

		if ((segmentSerieIndex == null) || (segmentSerieIndex.length < 2)) {
			// at least two points are required to build a segment
			return new Object[0];
		}

		final boolean isPulseSerie = (pulseSerie != null) && (pulseSerie.length > 0);
		final boolean isAltitudeSerie = (altitudeSerie != null) && (altitudeSerie.length > 0);
		final boolean isDistanceSerie = (distanceSerie != null) && (distanceSerie.length > 0);

		final int[] localPowerSerie = getPowerSerie();
		final boolean isPowerSerie = (localPowerSerie != null) && (localPowerSerie.length > 0);

		final int segmentSerieLength = segmentSerieIndex.length;

		final ArrayList<TourSegment> tourSegments = new ArrayList<TourSegment>(segmentSerieLength);
		final int firstSerieIndex = segmentSerieIndex[0];

		/*
		 * get start values
		 */
		int timeStart = timeSerie[firstSerieIndex];

		int altitudeStart = 0;
		if (isAltitudeSerie) {
			altitudeStart = altitudeSerie[firstSerieIndex];
		}

		int distanceStart = 0;
		if (isDistanceSerie) {
			distanceStart = distanceSerie[firstSerieIndex];
		}

		int timeTotal = 0;
		int distanceTotal = 0;

		int altitudeUpSummarizedBorder = 0;
		int altitudeUpSummarizedComputed = 0;
		int altitudeDownSummarizedBorder = 0;
		int altitudeDownSummarizedComputed = 0;

		final int tourPace = (int) (tourDistance == 0
				? 0
				: (tourDrivingTime * 1000 / (tourDistance * UI.UNIT_VALUE_DISTANCE)));

		segmentSerieRecordingTime = new int[segmentSerieLength];
		segmentSerieDrivingTime = new int[segmentSerieLength];
		segmentSerieBreakTime = new int[segmentSerieLength];
		segmentSerieTimeTotal = new int[segmentSerieLength];

		segmentSerieDistanceDiff = new int[segmentSerieLength];
		segmentSerieDistanceTotal = new int[segmentSerieLength];

		segmentSerieAltitudeDiff = new int[segmentSerieLength];
		segmentSerieAltitudeUpH = new float[segmentSerieLength];
		segmentSerieAltitudeDownH = new int[segmentSerieLength];

		segmentSerieSpeed = new float[segmentSerieLength];
		segmentSeriePace = new float[segmentSerieLength];

		segmentSeriePower = new float[segmentSerieLength];
		segmentSerieGradient = new float[segmentSerieLength];
		segmentSerieCadence = new float[segmentSerieLength];
		segmentSeriePulse = new float[segmentSerieLength];

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
			final int breakTime = getBreakTime(segmentStartIndex, segmentEndIndex);
			final float drivingTime = recordingTime - breakTime;

			segmentSerieRecordingTime[segmentIndex] = segment.recordingTime = recordingTime;
			segmentSerieDrivingTime[segmentIndex] = segment.drivingTime = (int) drivingTime;
			segmentSerieBreakTime[segmentIndex] = segment.breakTime = breakTime;
			segmentSerieTimeTotal[segmentIndex] = segment.timeTotal = timeTotal += recordingTime;

			float segmentDistance = 0.0f;

			/*
			 * distance
			 */
			if (isDistanceSerie) {

				final int distanceEnd = distanceSerie[segmentEndIndex];
				final int distanceDiff = distanceEnd - distanceStart;

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
					final float segmentPace = (drivingTime * 1000 / (segmentDistance / UI.UNIT_VALUE_DISTANCE));
					segment.pace = (int) segmentPace;
					segment.paceDiff = segment.pace - tourPace;
					segmentSeriePace[segmentIndex] = segmentPace;
				}
			}

			/*
			 * altitude
			 */
			if (isAltitudeSerie) {

				final int altitudeEnd = altitudeSerie[segmentEndIndex];
				final int altitudeDiff = altitudeEnd - altitudeStart;

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

					final int segmentDiff = segmentSerieComputedAltitudeDiff[segmentIndex];

					segment.altitudeDiffSegmentComputed = segmentDiff;

					if (segmentDiff > 0) {

						segment.altitudeUpSummarizedComputed = altitudeUpSummarizedComputed += segmentDiff;
						segment.altitudeDownSummarizedComputed = altitudeDownSummarizedComputed;

					} else {

						segment.altitudeUpSummarizedComputed = altitudeUpSummarizedComputed;
						segment.altitudeDownSummarizedComputed = altitudeDownSummarizedComputed += segmentDiff;
					}
				}

				int altitudeUpH = 0;
				int altitudeDownH = 0;
				int powerSum = 0;

				int altitude1 = altitudeSerie[segmentStartIndex];

				// get computed values: altitude up/down, pulse and power for a segment
				for (int serieIndex = segmentStartIndex + 1; serieIndex <= segmentEndIndex; serieIndex++) {

					final int altitude2 = altitudeSerie[serieIndex];
					final int altitude2Diff = altitude2 - altitude1;
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
						: (float) (altitudeUpH + altitudeDownH) / recordingTime * 3600 / UI.UNIT_VALUE_ALTITUDE;

				final int segmentIndexDiff = segmentEndIndex - segmentStartIndex;
				segmentSeriePower[segmentIndex] = segment.power = segmentIndexDiff == 0 ? 0 : powerSum
						/ segmentIndexDiff;

				// end point of current segment is the start of the next segment
				altitudeStart = altitudeEnd;
			}

			if (isDistanceSerie && isAltitudeSerie && (segmentDistance != 0.0)) {

				// gradient
				segmentSerieGradient[segmentIndex] = segment.gradient = //
				(float) segment.altitudeDiffSegmentBorder * 100 / segmentDistance;
			}

			if (isPulseSerie) {
				final int segmentAvgPulse = computeAvgPulseSegment(segmentStartIndex, segmentEndIndex);
				segmentSeriePulse[segmentIndex] = segment.pulse = segmentAvgPulse;
				segment.pulseDiff = segmentAvgPulse - avgPulse;
			} else {
				// hide pulse in the view
				segment.pulseDiff = Integer.MIN_VALUE;
			}

			// end point of current segment is the start of the next segment
			timeStart = timeEnd;
		}

		return tourSegments.toArray();
	}

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
	public int[] getAltimeterSerie() {

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
	public int[] getAltitudeSerie() {

		if (altitudeSerie == null) {
			return null;
		}

		if (UI.UNIT_VALUE_ALTITUDE != 1) {

			// imperial system is used

			if (altitudeSerieImperial == null) {

				// compute imperial altitude

				altitudeSerieImperial = new int[altitudeSerie.length];

				for (int valueIndex = 0; valueIndex < altitudeSerie.length; valueIndex++) {
					altitudeSerieImperial[valueIndex] = (int) (altitudeSerie[valueIndex] / UI.UNIT_VALUE_ALTITUDE);
				}
			}
			return altitudeSerieImperial;

		} else {

			return altitudeSerie;
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
	 * @return the avgTemperature
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

	/**
	 * Computes the time between start index and end index when the speed is <code>0</code>
	 * 
	 * @param startIndex
	 * @param endIndex
	 * @return Returns the break time in seconds
	 */
	public int getBreakTime(final int startIndex, final int endIndex) {

		if (distanceSerie == null) {
			return 0;
		}

		final int minBreakTime = 20;

		if (deviceTimeInterval == -1) {

			// variable time slices

			return computeBreakTimeVariable(minBreakTime, startIndex, endIndex);

		} else {

			// fixed time slices

			final int ignoreTimeSlices = deviceTimeInterval == 0 ? //
					0
					: getBreakTimeSlices(distanceSerie, startIndex, endIndex, minBreakTime / deviceTimeInterval);

			return ignoreTimeSlices * deviceTimeInterval;
		}
	}

	/**
	 * calculate the driving time, ignore the time when the distance is 0 within a time period which
	 * is defined by <code>sliceMin</code>
	 * 
	 * @param distanceValues
	 * @param indexLeft
	 * @param indexRight
	 * @param sliceMin
	 * @return Returns the number of slices which can be ignored
	 */
	private int getBreakTimeSlices(final int[] distanceValues, final int indexLeft, int indexRight, int sliceMin) {

		final int distanceLengthLast = distanceValues.length - 1;

		int ignoreTimeCounter = 0;
		int oldDistance = 0;

		sliceMin = (sliceMin >= 1) ? sliceMin : 1;
		indexRight = (indexRight <= distanceLengthLast) ? indexRight : distanceLengthLast;

		for (int valueIndex = indexLeft; valueIndex <= indexRight; valueIndex++) {

			if (distanceValues[valueIndex] == oldDistance) {
				ignoreTimeCounter++;
			}

			int oldIndex = valueIndex - sliceMin;
			if (oldIndex < 0) {
				oldIndex = 0;
			}
			oldDistance = distanceValues[oldIndex];
		}
		return ignoreTimeCounter;
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

	public String getDeviceId() {
		return devicePluginId;
	}

	public short getDeviceMode() {
		return deviceMode;
	}

	public String getDeviceModeName() {
		return deviceModeName;
	}

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

	public long getDeviceTravelTime() {
		return deviceTravelTime;
	}

	public int getDeviceWeight() {
		return deviceWeight;
	}

// not used 5.10.2008
//	public int getDeviceDistance() {
//		return deviceDistance;
//	}

	public int getDeviceWheel() {
		return deviceWheel;
	}

	/**
	 * @return Returns the distance data serie for the current measurement system, this can be
	 *         metric or imperial
	 */
	public int[] getDistanceSerie() {

		if (distanceSerie == null) {
			return null;
		}

		int[] serie;

		final float unitValueDistance = UI.UNIT_VALUE_DISTANCE;

		if (unitValueDistance != 1) {

			// use imperial system

			if (distanceSerieImperial == null) {

				// compute imperial data

				distanceSerieImperial = new int[distanceSerie.length];

				for (int valueIndex = 0; valueIndex < distanceSerie.length; valueIndex++) {
					distanceSerieImperial[valueIndex] = (int) (distanceSerie[valueIndex] / unitValueDistance);
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
	public int[] getGradientSerie() {

		if (gradientSerie == null) {
			computeAltimeterGradientSerie();
		}

		return gradientSerie;
	}

	public boolean getIsDistanceFromSensor() {
		return isDistanceFromSensor == 1;
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
	public int[] getMetricDistanceSerie() {
		return distanceSerie;
	}

	public int[] getPaceSerie() {

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

	public int[] getPaceSerieSeconds() {

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

	public int[] getPowerSerie() {

		if ((powerSerie != null) || isPowerSerieFromDevice) {
			return powerSerie;
		}

		if (speedSerie == null) {
			computeSpeedSerie();
		}

		if (gradientSerie == null) {
			computeAltimeterGradientSerie();
		}

		// check if required data series are available
		if ((speedSerie == null) || (gradientSerie == null)) {
			return null;
		}

		powerSerie = new int[timeSerie.length];

		final int weightBody = 75;
		final int weightBike = 10;
		final int bodyHeight = 188;

		final float cR = 0.008f; // Rollreibungskoeffizient Asphalt
		final float cD = 0.8f;// Streomungskoeffizient
		final float p = 1.145f; // 20C / 400m
//		float p = 0.968f; // 10C / 2000m

		final float weightTotal = weightBody + weightBike;
		final float bsa = (float) (0.007184f * Math.pow(weightBody, 0.425) * Math.pow(bodyHeight, 0.725));
		final float aP = bsa * 0.185f;

		final float fRoll = weightTotal * 9.81f * cR;
		final float fSlope = weightTotal * 9.81f; // * gradient/100
		final float fAir = 0.5f * p * cD * aP;// * v2;

		int joule = 0;
		int prefTime = 0;

		for (int timeIndex = 0; timeIndex < timeSerie.length; timeIndex++) {

			final float speed = (float) speedSerie[timeIndex] / 36; // speed (m/s) /10
			float gradient = (float) gradientSerie[timeIndex] / 1000; // gradient (%) /10 /100

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

			final float fSlopeTotal = fSlope * gradient;
			final float fAirTotal = fAir * speed * speed;

			final float fTotal = fRoll + fAirTotal + fSlopeTotal;

			int pTotal = (int) (fTotal * speed);

//			if (pTotal > 600) {
//				pTotal = pTotal * 1;
//			}
			pTotal = pTotal < 0 ? 0 : pTotal;

			powerSerie[timeIndex] = pTotal;

			final int currentTime = timeSerie[timeIndex];
			joule += pTotal * (currentTime - prefTime);

			prefTime = currentTime;
		}

//		System.out.println("joule: " + joule / 1000);
// TODO remove SYSTEM.OUT.PRINTLN

		return powerSerie;
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
	public int[] getSpeedSerie() {

		if (isSpeedSerieFromDevice) {
			return getSpeedSerieInternal();
		}
		if (distanceSerie == null) {
			return null;
		}

		return getSpeedSerieInternal();
	}

	public int[] getSpeedSerieFromDevice() {

		if (isSpeedSerieFromDevice) {
			return speedSerie;
		}

		return null;
	}

	private int[] getSpeedSerieInternal() {

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
	 * @return Returns SRTM metric or imperial data serie depending on the active measurement or
	 *         <code>null</code> when SRTM data serie is not available
	 */
	public int[] getSRTMSerie() {

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
	 * @return Returns SRTM metric data serie or <code>null</code> when SRTM data serie is not
	 *         available
	 */
	public int[] getSRTMSerieMetric() {

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

		return srtmSerie;
	}

	public short getStartAltitude() {
		return startAltitude;
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

//	public short getStartWeek() {
//		return startWeek;
//	}
//
//	/**
//	 * @return the startWeekYear
//	 */
//	public short getStartWeekYear() {
//		return startWeekYear;
//	}

	public short getStartYear() {
		return startYear;
	}

	/**
	 * @return Returns the temperature serie for the current measurement system or <code>null</code>
	 *         when temperature is not available
	 */
	public int[] getTemperatureSerie() {

		if (temperatureSerie == null) {
			return null;
		}

		int[] serie;

		final float unitValueTempterature = UI.UNIT_VALUE_TEMPERATURE;
		final float fahrenheitMulti = UI.UNIT_FAHRENHEIT_MULTI;
		final float fahrenheitAdd = UI.UNIT_FAHRENHEIT_ADD;

		if (unitValueTempterature != 1) {

			// use imperial system

			if (temperatureSerieImperial == null) {

				// compute imperial data

				temperatureSerieImperial = new int[temperatureSerie.length];

				for (int valueIndex = 0; valueIndex < temperatureSerie.length; valueIndex++) {
					temperatureSerieImperial[valueIndex] = (int) (temperatureSerie[valueIndex] * fahrenheitMulti + fahrenheitAdd);
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

	/**
	 * @return Returns the person for which the tour is saved or <code>null</code> when the tour
	 *         is not saved in the database
	 */
	public TourPerson getTourPerson() {
		return tourPerson;
	}

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
	 * @return the tourTitle
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

//	/**
//	 * Called before this object gets persisted, copy data from the tourdata object into the object
//	 * which gets serialized
//	 */
//	/*
//	 * @PrePersist + @PreUpdate is currently disabled for EJB events because of bug
//	 * http://opensource.atlassian.com/projects/hibernate/browse/HHH-1921 2006-08-11
//	 */
//	public void onPrePersistOLD() {
//
//		if (timeSerie == null) {
//			serieData = new SerieData();
//			return;
//		}
//
//		final int serieLength = timeSerie.length;
//
//		serieData = new SerieData(serieLength);
//
//		System.arraycopy(altitudeSerie, 0, serieData.altitudeSerie, 0, serieLength);
//		System.arraycopy(cadenceSerie, 0, serieData.cadenceSerie, 0, serieLength);
//		System.arraycopy(distanceSerie, 0, serieData.distanceSerie, 0, serieLength);
//		System.arraycopy(pulseSerie, 0, serieData.pulseSerie, 0, serieLength);
//		System.arraycopy(temperatureSerie, 0, serieData.temperatureSerie, 0, serieLength);
//		System.arraycopy(timeSerie, 0, serieData.timeSerie, 0, serieLength);
//
//		// System.arraycopy(speedSerie, 0, serieData.speedSerie, 0,
//		// serieLength);
//		// System.arraycopy(powerSerie, 0, serieData.powerSerie, 0,
//		// serieLength);
//
//		if (latitudeSerie != null) {
//
//			serieData.initializeGPSData(serieLength);
//
//			System.arraycopy(latitudeSerie, 0, serieData.latitude, 0, serieLength);
//			System.arraycopy(longitudeSerie, 0, serieData.longitude, 0, serieLength);
//		}
//	}

	public String getWeatherClouds() {
		return weatherClouds;
	}

	/**
	 * @return Returns the index for the cloud values in {@link IWeather#cloudIcon} and
	 *         {@link IWeather#cloudText} or 0 when the clouds are not defined
	 */
	public int getWeatherIndex() {

		int weatherCloudsIndex = -1;
		final String cloudValue = getWeatherClouds();

		if (cloudValue != null) {
			// we cannot use a binary search as that requires sorting which we cannot...
			for (int cloudIndex = 0; cloudIndex < IWeather.cloudIcon.length; ++cloudIndex) {
				if (IWeather.cloudIcon[cloudIndex].equalsIgnoreCase(cloudValue)) {
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
	 * @return Returns the world position for the suplied zoom level and projection id
	 */
	public Point[] getWorldPosition(final String projectionId, final int zoomLevel) {
		return _worldPosition.get(projectionId.hashCode() + zoomLevel);
	}

// not used 5.10.2008
//	public void setDeviceDistance(final int deviceDistance) {
//		this.deviceDistance = deviceDistance;
//	}

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
	 * @return Returns <code>true</code> when the data in {@link #powerSerie} is from a device and
	 *         not computed. Power data are normally available from an ergometer and not from a bike
	 *         computer
	 */
	public boolean isPowerSerieFromDevice() {
		return isPowerSerieFromDevice;
	}

	/**
	 * @return
	 *         Returns <code>true</code> when the data in {@link #speedSerie} are from the device
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
				Messages.Db_Field_TourData_Title);

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
				Messages.Db_Field_TourData_Description);

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
				Messages.Db_Field_TourData_StartPlace);

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
				Messages.Db_Field_TourData_EndPlace);

		if (fieldValidation == FIELD_VALIDATION.IS_INVALID) {
			return false;
		} else if (fieldValidation == FIELD_VALIDATION.TRUNCATE) {
			tourEndPlace = tourEndPlace.substring(0, DB_LENGTH_TOUR_END_PLACE);
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

	/**
	 * @param calories
	 *            the calories to set
	 */
	public void setCalories(final int calories) {
		this.calories = calories;
	}

	public void setDateTimeCreated(final long dateTimeCreated) {
		this.dateTimeCreated = dateTimeCreated;
	}

	public void setDateTimeModified(final long dateTimeModified) {
		this.dateTimeModified = dateTimeModified;
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
	 * Time difference between 2 time slices or <code>-1</code> for GPS devices or ergometer when
	 * the time slices are not equally
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
		this.deviceTravelTime = deviceTravelTime;
	}

	public void setDeviceWeight(final int deviceWeight) {
		this.deviceWeight = deviceWeight;
	}

	public void setDeviceWheel(final int deviceWheel) {
		this.deviceWheel = deviceWheel;
	}

	public void setDpTolerance(final short dpTolerance) {
		this.dpTolerance = dpTolerance;
	}

	public void setIsDistanceFromSensor(final boolean isFromSensor) {
		this.isDistanceFromSensor = (short) (isFromSensor ? 1 : 0);
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
	public void setPowerSerie(final int[] powerSerie) {
		this.powerSerie = powerSerie;
		this.isPowerSerieFromDevice = true;
	}

	public void setRestPulse(final int restPulse) {
		this.restPulse = restPulse;
	}

	private void setSpeed(	final int serieIndex,
							final int speedMetric,
							final int speedImperial,
							final int timeDiff,
							final int distDiff) {

		speedSerie[serieIndex] = speedMetric;
		speedSerieImperial[serieIndex] = speedImperial;

		maxSpeed = Math.max(maxSpeed, speedMetric);

		/*
		 * pace (computed with divisor 10)
		 */
		float paceMetricSeconds = 0;
		float paceImperialSeconds = 0;
		int paceMetricMinute = 0;
		int paceImperialMinute = 0;

		if ((speedMetric != 0) && (distDiff != 0)) {

			paceMetricSeconds = timeDiff * 10000 / (float) distDiff;
			paceImperialSeconds = paceMetricSeconds * UI.UNIT_MILE;

			paceMetricMinute = (int) ((paceMetricSeconds / 60));
			paceImperialMinute = (int) ((paceImperialSeconds / 60));
		}

		paceSerieMinute[serieIndex] = paceMetricMinute;
		paceSerieMinuteImperial[serieIndex] = paceImperialMinute;

		paceSerieSeconds[serieIndex] = (int) paceMetricSeconds / 10;
		paceSerieSecondsImperial[serieIndex] = (int) paceImperialSeconds / 10;
	}

	/**
	 * Sets the speed data serie and set's a flag that the data serie is from a device
	 * 
	 * @param speedSerie
	 */
	public void setSpeedSerie(final int[] speedSerie) {
		this.speedSerie = speedSerie;
		this.isSpeedSerieFromDevice = true;
	}

	public void setStartAltitude(final short startAltitude) {
		this.startAltitude = startAltitude;
	}

	public void setStartDay(final short startDay) {
		this.startDay = startDay;
	}

	/**
	 * Set the distance at tour start, this is the distance which the device has accumulated
	 * 
	 * @param startDistance
	 */
	public void setStartDistance(final int startDistance) {
		this.startDistance = startDistance;
	}

	public void setStartHour(final short startHour) {
		this.startHour = startHour;
	}

	public void setStartMinute(final short startMinute) {
		this.startMinute = startMinute;
	}

	/**
	 * Sets the month for the tour start in the range 1...12
	 */
	public void setStartMonth(final short startMonth) {
		this.startMonth = startMonth;
	}

	public void setStartPulse(final short startPulse) {
		this.startPulse = startPulse;
	}

	public void setStartSecond(final int startSecond) {
		this.startSecond = startSecond;
	}

	public void setStartYear(final short startYear) {
		this.startYear = startYear;
	}

	public void setTourAltDown(final int tourAltDown) {
		this.tourAltDown = tourAltDown;
	}

	public void setTourAltUp(final int tourAltUp) {
		this.tourAltUp = tourAltUp;
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
	 * Set total driving time
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

	public void setWayPoints(final ArrayList<TourWayPoint> wptList) {

		// remove old way points
		tourWayPoints.clear();

		if ((wptList == null) || (wptList.size() == 0)) {
			return;
		}

		// set new way points
		for (final TourWayPoint tourWayPoint : wptList) {
			tourWayPoint.setTourData(this);
			tourWayPoints.add(tourWayPoint);
		}
	}

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
	 * @param projectionId
	 * @param worldPositions
	 * @param zoomLevel
	 */
	public void setWorldPosition(final String projectionId, final Point[] worldPositions, final int zoomLevel) {
		_worldPosition.put(projectionId.hashCode() + zoomLevel, worldPositions);
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
