/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PostLoad;
import javax.persistence.PostUpdate;
import javax.persistence.Transient;

import net.tourbook.Messages;
import net.tourbook.chart.ChartMarker;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.UI;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.Rectangle;
import org.hibernate.annotations.Cascade;

/**
 * Tour data contains all data for a tour (except markers), an entity will be saved in the database
 */
@Entity
public class TourData {

	/**
	 * 
	 */
	@Transient
	public static final int		MIN_TIMEINTERVAL_FOR_MAX_SPEED	= 20;
	@Transient
	public static final float	MAX_BIKE_SPEED					= 120f;

	/**
	 * Unique persistence id which identifies the tour
	 */
	@Id
	private Long				tourId;

	/**
	 * HH (d) hour of tour
	 */
	private short				startHour;

	/**
	 * MM (d) minute of tour
	 */
	private short				startMinute;

	/**
	 * year of tour start
	 */
	private short				startYear;

	/**
	 * mm (d) month of tour
	 */
	private short				startMonth;

	/**
	 * dd (d) day of tour
	 */
	private short				startDay;

	/**
	 * week of the tour, 0 is the first week
	 */
	private short				startWeek;

	/**
	 * tttt (h) total distance at tour start (km)
	 */
	private int					startDistance;

	/**
	 * ssss distance msw
	 * <p>
	 * is not used any more since 6.12.2006 but it's necessary then it's a field in the database
	 */
	@SuppressWarnings("unused")
	private int					distance;

	/**
	 * aaaa (h) initial altitude (m)
	 */
	private short				startAltitude;

	/**
	 * pppp (h) initial pulse (bpm)
	 */
	private short				startPulse;

	/**
	 * tolerance for the Douglas Peucker algorithm
	 */
	private short				dpTolerance						= 50;

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
	private String				deviceTourType;

	/*
	 * data from the device
	 */
	private long				deviceTravelTime;
	private int					deviceDistance;

	private int					deviceWheel;
	private int					deviceWeight;

	private int					deviceTotalUp;
	private int					deviceTotalDown;

	/**
	 * total distance (m)
	 */
	private int					tourDistance;

	/**
	 * total recording time (sec)
	 */
	private int					tourRecordingTime;

	/**
	 * total driving time (sec)
	 */
	private int					tourDrivingTime;

	/**
	 * altitude up (m)
	 */
	private int					tourAltUp;

	/**
	 * altitude down (m)
	 */
	private int					tourAltDown;

	/**
	 * plugin id for the device which was used for this tour
	 */
	private String				devicePluginId;

	/**
	 * Profile used by the device
	 */
	private short				deviceMode;													// db-version 3

	/**
	 * time difference between 2 time slices or <code>-1</code> for GPS devices when the time
	 * slices are unequally
	 */
	private short				deviceTimeInterval;											// db-version 3

	private int					maxAltitude;													// db-version 4
	private int					maxPulse;														// db-version 4
	private float				maxSpeed;														// db-version 4

	private int					avgPulse;														// db-version 4
	private int					avgCadence;													// db-version 4
	private int					avgTemperature;												// db-version 4

	private String				tourTitle;														// db-version 4
	private String				tourDescription;												// db-version 4
	private String				tourStartPlace;												// db-version 4
	private String				tourEndPlace;													// db-version 4

	private String				calories;														// db-version 4
	private float				bikerWeight;													// db-version 4

	/**
	 * visible name for the used plugin to import the data
	 */
	private String				devicePluginName;												// db-version 4

	/**
	 * visible name for {@link #deviceMode}
	 */
	private String				deviceModeName;												// db-version 4

	/**
	 * data series for time, speed, altitude,...
	 */
	@Basic(optional = false)
	private SerieData			serieData;

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "tourData", fetch = FetchType.EAGER)
	@Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
	private Set<TourMarker>		tourMarkers						= new HashSet<TourMarker>();

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "tourData", fetch = FetchType.EAGER)
	@Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
	private Set<TourReference>	tourReferences					= new HashSet<TourReference>();

	@ManyToMany(mappedBy = "tourData", fetch = FetchType.EAGER)
	private Set<TourCategory>	tourCategory					= new HashSet<TourCategory>();

	/**
	 * Category of the tour, e.g. bike, mountainbike, jogging, inlinescating
	 */
	@ManyToOne
	private TourType			tourType;

	/**
	 * Person which created this tour or <code>null</code> when the tour is not saved in the
	 * database
	 */
	@ManyToOne
	private TourPerson			tourPerson;

	/**
	 * plugin id for the device which was used for this tour Bike used for this tour
	 */
	@ManyToOne
	private TourBike			tourBike;

	/*
	 * data series from the device
	 */
	@Transient
	public int[]				timeSerie;

	/**
	 * contains the distance data serie in the metric system
	 */
	@Transient
	public int[]				distanceSerie;

	@Transient
	private int[]				distanceSerieImperial;

	/**
	 * contains the absolute altitude in the metric measurement system
	 */
	@Transient
	public int[]				altitudeSerie;

	/**
	 * contains the absolute altitude in the imperial measurement system
	 */
	@Transient
	private int[]				altitudeSerieImperial;

	@Transient
	public int[]				cadenceSerie;

	@Transient
	public int[]				pulseSerie;

	@Transient
	public int[]				temperatureSerie;

	/**
	 * contains the temperature in the imperial measurement system
	 */
	@Transient
	private int[]				temperatureSerieImperial;

	/*
	 * computed data series
	 */

	/**
	 * the metric speed serie is required form computing the power even if the current measurement
	 * system is imperial
	 */
	@Transient
	private int[]				speedSerie;
	@Transient
	private int[]				speedSerieImperial;

	/**
	 * Is <code>true</code> when the data in {@link #speedSerie} are from the device and not
	 * computed. Speed data are normally available from an ergometer and not from a bike computer
	 */
	@Transient
	private boolean				isSpeedSerieFromDevice			= false;

	@Transient
	private int[]				paceSerie;
	@Transient
	private int[]				paceSerieImperial;

	@Transient
	private int[]				powerSerie;

	/**
	 * Is <code>true</code> when the data in {@link #powerSerie} are from the device and not
	 * computed. Power data are normally available from an ergometer and not from a bike computer
	 */
	@Transient
	private boolean				isPowerSerieFromDevice			= false;

	@Transient
	private int[]				altimeterSerie;
	@Transient
	private int[]				altimeterSerieImperial;

	@Transient
	public int[]				gradientSerie;

	@Transient
	public int[]				tourCompareSerie;

	/*
	 * GPS data
	 */
	@Transient
	public double[]				latitudeSerie;
	@Transient
	public double[]				longitudeSerie;

	/**
	 * contains the bounds of the tour in latitude/longitude
	 */
	@Transient
	public Rectangle			gpsBounds;

	/**
	 * Index of the segmented data in the original serie
	 */
	@Transient
	public int[]				segmentSerieIndex;

	/**
	 * oooo (o) DD-record // offset
	 */
	@Transient
	public int					offsetDDRecord;

	@Transient
	protected Object[]			fTourSegments;

	/*
	 * data for the tour segments
	 */
	@Transient
	public int[]				segmentSerieAltitude;

	@Transient
	public int[]				segmentSerieDistance;

	@Transient
	public int[]				segmentSerieTime;

	@Transient
	public int[]				segmentSerieDrivingTime;

	@Transient
	public float[]				segmentSerieAltimeter;

	@Transient
	public int[]				segmentSerieAltitudeDown;

	@Transient
	public float[]				segmentSerieSpeed;

	@Transient
	public float[]				segmentSeriePace;

	@Transient
	public float[]				segmentSeriePower;

	@Transient
	public float[]				segmentSerieGradient;

	@Transient
	public float[]				segmentSeriePulse;

	@Transient
	public float[]				segmentSerieCadence;

	/**
	 * contains the filename from which the data are imported, when set to <code>null</code> the
	 * data are not imported they are from the database
	 */
	@Transient
	public String				importRawDataFile;

	/**
	 * Latitude for the center position in the map or {@link Double#MIN_VALUE} when the position is
	 * not set
	 */
	@Transient
	public double				mapCenterPositionLatitude		= Double.MIN_VALUE;

	/**
	 * Longitude for the center position in the map or {@link Double#MIN_VALUE} when the position is
	 * not set
	 */
	@Transient
	public double				mapCenterPositionLongitude		= Double.MIN_VALUE;

	/**
	 * Zoomlevel in the map
	 */
	@Transient
	public int					mapZoomLevel;

	@Transient
	public double				mapMinLatitude;
	@Transient
	public double				mapMaxLatitude;
	@Transient
	public double				mapMinLongitude;
	@Transient
	public double				mapMaxLongitude;

	public TourData() {}

	/**
	 * clear imperial altitude series so the next time when it's needed it will be recomputed
	 */
	public void clearAltitudeSeries() {
		altitudeSerieImperial = null;
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

		paceSerie = null;
		altimeterSerie = null;

		speedSerieImperial = null;
		paceSerieImperial = null;
		altimeterSerieImperial = null;
		altitudeSerieImperial = null;
	}

	public void computeAltimeterGradientSerie() {

		// optimization: don't recreate the data series when they are available
		if (altimeterSerie != null && altimeterSerieImperial != null && gradientSerie != null) {
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

		if (distanceSerie == null || altitudeSerie == null) {
			return;
		}

		final int serieLength = timeSerie.length;

		final int dataSerieAltimeter[] = new int[serieLength];
		final int dataSerieGradient[] = new int[serieLength];

		int adjustIndexLow;
		int adjustmentIndexHigh;

		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

		if (prefStore.getBoolean(ITourbookPreferences.GRAPH_PROPERTY_IS_VALUE_COMPUTING)) {

			// use custom settings to compute altimeter and gradient

			final int computeTimeSlice = prefStore.getInt(ITourbookPreferences.GRAPH_PROPERTY_CUSTOM_VALUE_TIMESLICE);
			final int slices = computeTimeSlice / deviceTimeInterval;

			adjustmentIndexHigh = Math.max(1, slices / 2);
			adjustIndexLow = slices / 2;

			// round up
			if (adjustIndexLow + adjustmentIndexHigh < slices) {
				adjustmentIndexHigh++;
			}

		} else {

			// use internal algorithm to compute altimeter and gradient

//			if (deviceTimeInterval <= 2) {
//				adjustIndexLow = 15;
//				adjustmentIndexHigh = 15;
//				
//			} else if (deviceTimeInterval <= 5) {
//				adjustIndexLow = 5;
//				adjustmentIndexHigh = 6;
//				
//			} else if (deviceTimeInterval <= 10) {
//				adjustIndexLow = 2;
//				adjustmentIndexHigh = 3;
//			} else {
//				adjustIndexLow = 1;
//				adjustmentIndexHigh = 2;
//			}

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
		}

		/*
		 * compute values
		 */

		for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

			// adjust index to the array size
			final int indexLow = Math.min(Math.max(0, serieIndex - adjustIndexLow), serieLength - 1);
			final int indexHigh = Math.max(0, Math.min(serieIndex + adjustmentIndexHigh, serieLength - 1));

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

		if (distanceSerie == null || altitudeSerie == null) {
			return;
		}

		final int serieLength = timeSerie.length;

		final int[] dataSerieDistance = distanceSerie;
		final int[] dataSerieAltitude = altitudeSerie;
		final int[] timeSerieDiff = timeSerie;

		final int dataSerieAltimeter[] = new int[serieLength];
		final int dataSerieGradient[] = new int[serieLength];

		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

		// get minimum difference
		int minDataDiff;
		if (prefStore.getBoolean(ITourbookPreferences.GRAPH_PROPERTY_IS_VALUE_COMPUTING)) {
			// use custom settings to compute altimeter and gradient
			minDataDiff = prefStore.getInt(ITourbookPreferences.GRAPH_PROPERTY_CUSTOM_VALUE_TIMESLICE) / 2;
		} else {
			// use internal algorithm to compute altimeter and gradient
			minDataDiff = 10;
		}

		/*
		 * compute values
		 */
		for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

			// get index for the low value
			int indexLow = Math.max(0, serieIndex - 1);
			int dataDiffLow = timeSerieDiff[serieIndex] - timeSerieDiff[indexLow];
			while (dataDiffLow < minDataDiff) {
				// make sure to be in the array bounds
				if (indexLow < 1) {
					break;
				}
				dataDiffLow = timeSerieDiff[serieIndex] - timeSerieDiff[--indexLow];
			}
			// remove lowest index
			indexLow = Math.min(serieLength - 1, ++indexLow);

			// get index for the high value
			int indexHigh = Math.min(serieLength - 1, serieIndex + 1);
			int dataDiffHigh = timeSerieDiff[serieIndex] - timeSerieDiff[indexHigh];
			while (dataDiffHigh < minDataDiff) {
				// make sure to be in the array bounds
				if (indexHigh > serieLength - 2) {
					break;
				}
				dataDiffHigh = timeSerieDiff[indexHigh++] + -timeSerieDiff[serieIndex];
			}
			// remove highest index
			indexHigh = Math.max(0, --indexHigh);

			final int distanceDiff = dataSerieDistance[indexHigh] - dataSerieDistance[indexLow];
			final int altitudeDiff = dataSerieAltitude[indexHigh] - dataSerieAltitude[indexLow];
			final int timeDiff = timeSerie[indexHigh] - timeSerie[indexLow];

			// keep altimeter data
			if (timeDiff == 0) {
				dataSerieAltimeter[serieIndex] = 0;
			} else {
				dataSerieAltimeter[serieIndex] = (int) (3600f * altitudeDiff / timeDiff / UI.UNIT_VALUE_ALTITUDE);
			}

			// keep gradient data
			if (distanceDiff == 0) {
				dataSerieGradient[serieIndex] = 0;
			} else {
				dataSerieGradient[serieIndex] = altitudeDiff * 1000 / distanceDiff;
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

	public void computeAvgCadence() {

		if (cadenceSerie == null) {
			return;
		}

		long cadenceSum = 0;
		int cadenceCount = 0;

		for (int cadence : cadenceSerie) {
			if (cadence > 0) {
				cadenceCount++;
				cadenceSum += cadence;
			}
		}
		if (cadenceCount > 0) {
			avgCadence = (int) cadenceSum / cadenceCount;
		}
	}

	public void computeAvgFields() {
		computeMaxAltitude();
		computeMaxPulse();
		computeAvgPulse();
		computeAvgCadence();
		computeAvgTemperature();
		computeMaxSpeed();
	}

	public void computeAvgPulse() {

		if (pulseSerie == null) {
			return;
		}

		long pulseSum = 0;
		int pulseCount = 0;

		for (int pulse : pulseSerie) {
			if (pulse > 0) {
				pulseCount++;
				pulseSum += pulse;
			}
		}

		if (pulseCount > 0) {
			avgPulse = (int) pulseSum / pulseCount;
		}
	}

	public void computeAvgTemperature() {

		if (temperatureSerie == null) {
			return;
		}

		long temperatureSum = 0;

		for (int temperature : temperatureSerie) {
			temperatureSum += temperature;
		}

		final int tempLength = temperatureSerie.length;
		if (tempLength > 0) {
			avgTemperature = (int) temperatureSum / tempLength;
		}
	}

	private int computeBreakTimeVariable(final int minStopTime, int startIndex, int endIndex) {

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

			if (distDiff == 0 || timeDiff > 20 && distDiff < 5) {

				// distance has not changed, check if a longer stop is done

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

	public void computeMaxAltitude() {

		if (altitudeSerie == null) {
			return;
		}

		int maxAltitude = 0;
		for (int altitude : altitudeSerie) {
			if (altitude > maxAltitude) {
				maxAltitude = altitude;
			}
		}
		this.maxAltitude = maxAltitude;
	}

	public void computeMaxPulse() {

		if (pulseSerie == null) {
			return;
		}

		int maxPulse = 0;

		for (int pulse : pulseSerie) {
			if (pulse > maxPulse) {
				maxPulse = pulse;
			}
		}
		this.maxPulse = maxPulse;
	}

	public void computeMaxSpeed() {

		if (distanceSerie == null) {
			return;
		}

		float maxSpeed = 0;
		int anzValuesSum = 1;

		if (distanceSerie.length >= 2) {

			if (deviceTimeInterval > 0) {
				anzValuesSum = MIN_TIMEINTERVAL_FOR_MAX_SPEED / deviceTimeInterval;
				if (anzValuesSum == 0) {
					anzValuesSum = 1;
				}
			}

			for (int i = 0 + anzValuesSum; i <= distanceSerie.length - 1; i++) {
				float speed = ((float) distanceSerie[i] - (float) distanceSerie[i - anzValuesSum])
						/ ((float) timeSerie[i] - (float) timeSerie[i - anzValuesSum])
						* 3.6f;

				if (speed > maxSpeed && speed < MAX_BIKE_SPEED) {
					maxSpeed = speed;
				}
			}
		}
		this.maxSpeed = maxSpeed;
	}

	/**
	 * computes the speed data serie which can be retrieved with {@link TourData#getSpeedSerie()}
	 */
	public void computeSpeedSerie() {

		if (speedSerie != null && speedSerieImperial != null && paceSerie != null && paceSerieImperial != null) {
			return;
		}

		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

		if (prefStore.getBoolean(ITourbookPreferences.GRAPH_PROPERTY_IS_VALUE_COMPUTING)) {

			// compute speed for custom settings

			if (deviceTimeInterval == -1) {
				computeSpeedSerieInternalWithVariableInterval();
			} else {
				computeSpeedSerieCustomWithFixedInterval();
			}
		} else {

			// compute speed with internal algorithm

			if (deviceTimeInterval == -1) {
				computeSpeedSerieInternalWithVariableInterval();
			} else {
				computeSpeedSerieInternalWithFixedInterval();
			}
		}

//		computeValueClipping();
	}

	private void computeSpeedSerieCustomWithFixedInterval() {

		final int serieLength = timeSerie.length;

		speedSerie = new int[serieLength];
		speedSerieImperial = new int[serieLength];
		paceSerie = new int[serieLength];
		paceSerieImperial = new int[serieLength];

		int lowIndexAdjustment = 0;
		int highIndexAdjustment = 1;

		final int speedTimeSlice = TourbookPlugin.getDefault()
				.getPreferenceStore()
				.getInt(ITourbookPreferences.GRAPH_PROPERTY_CUSTOM_VALUE_TIMESLICE);

		final int slices = speedTimeSlice / deviceTimeInterval;

		highIndexAdjustment = Math.max(1, slices / 2);
		lowIndexAdjustment = slices / 2;

		// round up
		if (lowIndexAdjustment + highIndexAdjustment < slices) {
			highIndexAdjustment++;
		}

		for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

			// adjust index to the array size
			final int distIndexLow = Math.min(Math.max(0, serieIndex - lowIndexAdjustment), serieLength - 1);
			final int distIndexHigh = Math.max(0, Math.min(serieIndex + highIndexAdjustment, serieLength - 1));

			final int distance = distanceSerie[distIndexHigh] - distanceSerie[distIndexLow];
			final float time = deviceTimeInterval * (distIndexHigh - distIndexLow);

			/*
			 * speed
			 */
			int speedMetric = 0;
			int speedImperial = 0;
			if (time != 0) {
				final float speed = (distance * 36F) / time;
				speedMetric = (int) (speed);
				speedImperial = (int) (speed / UI.UNIT_MILE);
			}

			speedSerie[serieIndex] = speedMetric;
			speedSerieImperial[serieIndex] = speedImperial;

			/*
			 * pace
			 */
			int paceMetric = 0;
			int paceImperial = 0;

			if (speedMetric != 0 && distance != 0) {
				final float pace = time * 166.66f / distance;
				paceMetric = (int) (pace);
				paceImperial = (int) (pace * UI.UNIT_MILE);
			}
			paceSerie[serieIndex] = paceMetric;
			paceSerieImperial[serieIndex] = paceImperial;
		}
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
		paceSerie = new int[serieLength];
		paceSerieImperial = new int[serieLength];

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

		for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

			// adjust index to the array size
			int distIndexLow = Math.min(Math.max(0, serieIndex - lowIndexAdjustmentDefault), serieLength - 1);
			int distIndexHigh = Math.max(0, Math.min(serieIndex + highIndexAdjustmentDefault, serieLength - 1));

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
			distIndexLow = Math.min(Math.max(0, serieIndex - lowIndexAdjustment), serieLength - 1);
			distIndexHigh = Math.max(0, Math.min(serieIndex + highIndexAdjustment, serieLength - 1));

			final int distance = distanceSerie[distIndexHigh] - distanceSerie[distIndexLow];
			final float time = timeSerie[distIndexHigh] - timeSerie[distIndexLow];

			/*
			 * speed
			 */
			int speedMetric = 0;
			int speedImperial = 0;
			if (time != 0) {
				final float speed = (distance * 36F) / time;
				speedMetric = (int) (speed);
				speedImperial = (int) (speed / UI.UNIT_MILE);
			}

			speedSerie[serieIndex] = speedMetric;
			speedSerieImperial[serieIndex] = speedImperial;

			/*
			 * pace
			 */
			int paceMetric = 0;
			int paceImperial = 0;

			if (speedMetric != 0 && distance != 0) {
				final float pace = time * 166.66f / distance;
				paceMetric = (int) (pace);
				paceImperial = (int) (pace * UI.UNIT_MILE);
			}
			paceSerie[serieIndex] = paceMetric;
			paceSerieImperial[serieIndex] = paceImperial;
		}
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

		if (prefStore.getBoolean(ITourbookPreferences.GRAPH_PROPERTY_IS_VALUE_COMPUTING)) {
			minTimeDiff = prefStore.getInt(ITourbookPreferences.GRAPH_PROPERTY_CUSTOM_VALUE_TIMESLICE);
			minTimeDiff = minTimeDiff < 1 ? 1 : minTimeDiff;
		} else {
			minTimeDiff = 10;
		}

		final int serieLength = timeSerie.length;

		speedSerie = new int[serieLength];
		speedSerieImperial = new int[serieLength];
		paceSerie = new int[serieLength];
		paceSerieImperial = new int[serieLength];

		for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

			// adjust index to the array size
			int lowIndex = Math.max(0, serieIndex - 1);
			int highIndex = Math.min(serieIndex, serieLength - 1);

			int timeDiff = timeSerie[highIndex] - timeSerie[lowIndex];
			int distDiff = distanceSerie[highIndex] - distanceSerie[lowIndex];

			boolean adjustHighIndex = true;

			while (timeDiff < minTimeDiff) {

				// toggle between low and high index
				if (adjustHighIndex) {
					highIndex++;
				} else {
					lowIndex--;
				}
				adjustHighIndex = !adjustHighIndex;

				// check array scope
				if (lowIndex < 0 || highIndex >= serieLength) {
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
			highIndex = Math.min(highIndex, serieLength - 1);
			lowIndex = Math.max(lowIndex, 0);
			boolean isTimeValid = true;
			int prevTime = timeSerie[lowIndex];
			for (int timeIndex = lowIndex + 1; timeIndex <= highIndex; timeIndex++) {
				int currentTime = timeSerie[timeIndex];
				if (prevTime == currentTime) {
					isTimeValid = false;
					break;
				}
				prevTime = currentTime;
			}

			if (isTimeValid && serieIndex > 0) {
				if (timeDiff != 0) {
					if (timeDiff > 20 && distDiff < 5) {
						speedMetric = 0;
					} else {
						speedMetric = (int) ((distDiff * 36f) / timeDiff);
						speedMetric = speedMetric < 0 ? 0 : speedMetric;

						speedImperial = (int) ((distDiff * 36f) / (timeDiff * UI.UNIT_MILE));
						speedImperial = speedImperial < 0 ? 0 : speedImperial;
					}
				}
			}
			speedSerie[serieIndex] = speedMetric;
			speedSerieImperial[serieIndex] = speedImperial;

			/*
			 * pace
			 */
			int paceMetric = 0;
			int paceImperial = 0;

			if (speedMetric != 0 && distDiff != 0) {
				final float pace = timeDiff * 166.66f / distDiff;
				paceMetric = (int) (pace);
				paceImperial = (int) (pace * UI.UNIT_MILE);
			}
			paceSerie[serieIndex] = paceMetric;
			paceSerieImperial[serieIndex] = paceImperial;
		}
	}

	public void computeTourDrivingTime() {
		tourDrivingTime = Math.max(0, timeSerie[timeSerie.length - 1] - getBreakTime(0, timeSerie.length));
	}

//	/**
//	 * Clip values when a minimum distance is fallen short of
//	 */
//	private void computeValueClipping() {
//
//		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();
//
//		int clippingTime;
//		if (prefStore.getBoolean(ITourbookPreferences.GRAPH_PROPERTY_IS_VALUE_CLIPPING)) {
//			// use custom clipping
//			clippingTime = prefStore.getInt(ITourbookPreferences.GRAPH_PROPERTY_VALUE_CLIPPING_TIMESLICE);
//		} else {
//			// use internal clipping, value was evaluated with experiments
//			clippingTime = 15;
//		}
//
//		int paceClipping;
//		if (prefStore.getBoolean(ITourbookPreferences.GRAPH_PROPERTY_IS_PACE_CLIPPING)) {
//			// use custom clipping
//			paceClipping = prefStore.getInt(ITourbookPreferences.GRAPH_PROPERTY_PACE_CLIPPING_VALUE);
//		} else {
//			// use internal clipping, value was evaluated with experiments
//			paceClipping = 15;
//		}
//
//		final int[] speedSerie = getSpeedSerie();
//		final int[] paceSerie = getPaceSerie();
//		final int[] altimeterSerie = getAltimeterSerie();
//		final int[] distanceSerie = getDistanceSerie();
//
//		final int serieLength = timeSerie.length;
//
//		if (deviceTimeInterval > 0) {
//
//			/*
//			 * clipping for constanct time intervals
//			 */
//
//			final int slices = Math.max(1, clippingTime / deviceTimeInterval);
//
//			for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {
//
//				// adjust index to the array size
//				int sliceIndex = serieIndex + slices;
//				sliceIndex = Math.min(Math.max(0, sliceIndex), serieLength - 1);
//
//				final int distance = distanceSerie[sliceIndex] - distanceSerie[serieIndex];
//
//				if (distance == 0) {
//					altimeterSerie[serieIndex] = 0;
//					gradientSerie[serieIndex] = 0;
//					speedSerie[serieIndex] = 0;
//				}
//
//				// remove peaks in pace
//				if (speedSerie[serieIndex] <= paceClipping) {
//					paceSerie[serieIndex] = 0;
//				}
//			}
//
//		} else {
//
//			/*
//			 * clipping for variable time intervals
//			 */
//
//			for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {
//
//				// adjust index to the array size
//				int lowIndex = Math.max(0, serieIndex - 1);
//
//				int timeDiff = timeSerie[serieIndex] - timeSerie[lowIndex];
//				int distDiff = 0;
//
//				while (timeDiff < clippingTime) {
//
//					// make sure to be in the array range
//					if (lowIndex < 1) {
//						break;
//					}
//					lowIndex--;
//
//					timeDiff = timeSerie[serieIndex] - timeSerie[lowIndex];
//				}
//
//				distDiff = distanceSerie[serieIndex] - distanceSerie[lowIndex];
//
//				if (distDiff == 0) {
//					altimeterSerie[serieIndex] = 0;
//					gradientSerie[serieIndex] = 0;
//					speedSerie[serieIndex] = 0;
//				}
//
//				// remove peaks in pace
//				if (speedSerie[serieIndex] <= paceClipping) {
//					paceSerie[serieIndex] = 0;
//				}
//			}
//		}
//
////		System.out.println("clipping");
//	}

	/**
	 * Create a device marker at the current position
	 * 
	 * @param timeData
	 * @param timeIndex
	 * @param timeAbsolute
	 * @param distanceAbsolute
	 */
	private void createMarker(TimeData timeData, int timeIndex, int timeAbsolute, int distanceAbsolute) {

		// create a new marker
		TourMarker tourMarker = new TourMarker(this, ChartMarker.MARKER_TYPE_DEVICE);

		tourMarker.setVisualPosition(ChartMarker.VISUAL_HORIZONTAL_ABOVE_GRAPH_CENTERED);
		tourMarker.setTime(timeAbsolute + timeData.marker);
		tourMarker.setDistance(distanceAbsolute);
		tourMarker.setSerieIndex(timeIndex);

		if (timeData.markerLabel == null) {
			tourMarker.setLabel(Messages.TourData_Label_device_marker);
		} else {
			tourMarker.setLabel(timeData.markerLabel);
		}

		getTourMarkers().add(tourMarker);
	}

	/**
	 * Convert {@link TimeData} into {@link TourData}
	 * 
	 * @param isCreateMarker
	 *        creates markers when <code>true</code>
	 */
	public void createTimeSeries(final ArrayList<TimeData> timeDataList, final boolean isCreateMarker) {

		final int serieLength = timeDataList.size();

		if (serieLength == 0) {
			return;
		}

		final TimeData firstTimeDataItem = timeDataList.get(0);

		timeSerie = new int[serieLength];

		boolean isDistance = false;
		boolean isAltitude = false;
		boolean isPulse = false;
		boolean isCadence = false;
		boolean isTemperature = false;
		boolean isSpeed = false;
		boolean isPower = false;

		final boolean isAbsoluteData = firstTimeDataItem.absoluteTime != Long.MIN_VALUE;

		/*
		 * create data series only when data are available
		 */
		if (firstTimeDataItem.distance != Integer.MIN_VALUE || isAbsoluteData) {
			distanceSerie = new int[serieLength];
			isDistance = true;
		}

		if (firstTimeDataItem.altitude != Integer.MIN_VALUE || isAbsoluteData) {
			altitudeSerie = new int[serieLength];
			isAltitude = true;
		}

		if (firstTimeDataItem.pulse != Integer.MIN_VALUE || isAbsoluteData) {
			pulseSerie = new int[serieLength];
			isPulse = true;
		}

		if (firstTimeDataItem.cadence != Integer.MIN_VALUE) {
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
		if (firstTimeDataItem.latitude != Integer.MIN_VALUE) {
			latitudeSerie = new double[serieLength];
			longitudeSerie = new double[serieLength];
		}

		int timeIndex = 0;

		int timeAbsolute = 0; // time in seconds
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

			int pulseCounter = 0;
			int lastValidTime = 0;

			// set initial min/max latitude/longitude
			if (firstTimeDataItem.latitude == Double.MIN_VALUE || firstTimeDataItem.longitude == Double.MIN_VALUE) {

				// find first valid latitude/longitude
				for (TimeData timeData : timeDataList) {
					if (timeData.latitude != Double.MIN_VALUE && timeData.longitude != Double.MIN_VALUE) {
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

				if (altitudeStartIndex == -1 && isAltitude) {
					altitudeStartIndex = timeIndex;
					altitudeAbsolute = (int) timeData.absoluteAltitude;
				}

				final long absoluteTime = timeData.absoluteTime;

				if (timeIndex == 0) {

					// first trackpoint

					if (absoluteTime == Long.MIN_VALUE) {
						firstTime = 0;
					} else {
						firstTime = absoluteTime;
					}
					lastValidTime = 0;

					if (isAltitude) {
						altitudeSerie[timeIndex] = altitudeAbsolute;
					}

				} else {

					// 1..n trackpoint

					/*
					 * time
					 */
					if (absoluteTime == Long.MIN_VALUE) {
						timeAbsolute = lastValidTime;
					} else {
						timeAbsolute = (int) ((absoluteTime - firstTime) / 1000);
					}
					timeSerie[timeIndex] = lastValidTime = timeAbsolute;

					/*
					 * distance
					 */
					float tdDistance = timeData.absoluteDistance;
					if (tdDistance == Float.MIN_VALUE) {
						distanceDiff = 0;
					} else {
						distanceDiff = (int) (tdDistance - distanceAbsolute);
					}
					distanceSerie[timeIndex] = distanceAbsolute += distanceDiff;

					/*
					 * altitude
					 */
					if (altitudeStartIndex == -1) {
						altitudeDiff = 0;
					} else {
						final float tdAltitude = timeData.absoluteAltitude;
						if (tdAltitude == Float.MIN_VALUE) {
							altitudeDiff = 0;
						} else {
							altitudeDiff = (int) (tdAltitude - altitudeAbsolute);
						}
					}
					altitudeSerie[timeIndex] = altitudeAbsolute += altitudeDiff;
				}

				/*
				 * latitude & longitude
				 */
				final double latitude = timeData.latitude;
				final double longitude = timeData.longitude;

				if (latitude == Double.MIN_VALUE || longitude == Double.MIN_VALUE) {
					latitudeSerie[timeIndex] = lastValidLatitude;
					longitudeSerie[timeIndex] = lastValidLongitude;
				} else {

					latitudeSerie[timeIndex] = lastValidLatitude = latitude;
					longitudeSerie[timeIndex] = lastValidLongitude = longitude;
				}

				mapMinLatitude = Math.min(mapMinLatitude, lastValidLatitude + 90);
				mapMaxLatitude = Math.max(mapMaxLatitude, lastValidLatitude + 90);
				mapMinLongitude = Math.min(mapMinLongitude, lastValidLongitude + 180);
				mapMaxLongitude = Math.max(mapMaxLongitude, lastValidLongitude + 180);

				/*
				 * pulse
				 */
				final int pulse = timeData.pulse;
				pulseSerie[timeIndex] = pulse;
				if (pulse >= 0) {
					pulseCounter++;
				}

				/*
				 * marker
				 */
				if (isCreateMarker && timeData.marker != 0) {
					createMarker(timeData, timeIndex, timeAbsolute, distanceAbsolute);
				}

				timeIndex++;
			}

			mapMinLatitude -= 90;
			mapMaxLatitude -= 90;
			mapMinLongitude -= 180;
			mapMaxLongitude -= 180;

			/*
			 * make sure that all pulse data points have a valid value
			 */
			if (pulseCounter > 0) {
				for (int pulseIndex = 0; pulseIndex < pulseSerie.length; pulseIndex++) {
					final int pulse = pulseSerie[pulseIndex];
					if (pulse <= 0) {
						pulseSerie[pulseIndex] = 0;
					}
				}
			} else {
				pulseSerie = null;
			}

		} else {

			/*
			 * relativ data are available, these data are from non GPS devices
			 */

			// convert data from the tour format into an interger[]
			for (final TimeData timeData : timeDataList) {

				timeSerie[timeIndex] = timeAbsolute += timeData.time;

				if (isDistance) {
					distanceSerie[timeIndex] = distanceAbsolute += timeData.distance;
				}

				if (isAltitude) {
					altitudeSerie[timeIndex] = altitudeAbsolute += timeData.altitude;
				}

				if (isPulse) {
					pulseSerie[timeIndex] = timeData.pulse;
				}

				if (isTemperature) {
					temperatureSerie[timeIndex] = timeData.temperature;
				}

				if (isCadence) {
					cadenceSerie[timeIndex] = timeData.cadence;
				}

				if (isPower) {
					powerSerie[timeIndex] = timeData.power;
				}

				if (isSpeed) {
					speedSerie[timeIndex] = timeData.speed;
				}

				if (isCreateMarker && timeData.marker != 0) {
					createMarker(timeData, timeIndex, timeAbsolute, distanceAbsolute);
				}

				timeIndex++;
			}
		}

		tourDistance = distanceAbsolute;
		tourRecordingTime = timeAbsolute;

	}

	/**
	 * Creates the unique tour id from the tour date/time and the unique key
	 * 
	 * @param uniqueKey
	 *        unique key to identify a tour
	 */
	public void createTourId(String uniqueKey) {

//		final String uniqueKey = Integer.toString(Math.abs(getStartDistance()));

		String tourId;

		try {
			/*
			 * this is the default implementation to create a tour id, but on the 5.5.2007 a
			 * NumberFormatException occured so the calculation for the tour id was adjusted
			 */
			tourId = Short.toString(getStartYear())
					+ Short.toString(getStartMonth())
					+ Short.toString(getStartDay())
					+ Short.toString(getStartHour())
					+ Short.toString(getStartMinute())
					+ uniqueKey;

			setTourId(Long.parseLong(tourId));

		} catch (NumberFormatException e) {

			/*
			 * the distance was shorted so that the maximum of a Long datatype is not exceeded
			 */

			tourId = Short.toString(getStartYear())
					+ Short.toString(getStartMonth())
					+ Short.toString(getStartDay())
					+ Short.toString(getStartHour())
					+ Short.toString(getStartMinute())
					+ uniqueKey.substring(0, Math.min(5, uniqueKey.length()));

			setTourId(Long.parseLong(tourId));
		}

	}

	/**
	 * Create the tour segment list from the segment index array
	 * 
	 * @return
	 */
	public Object[] createTourSegments() {

		if (segmentSerieIndex == null || segmentSerieIndex.length < 2) {
			// at least two points are required to build a segment
			return new Object[0];
		}

		final int segmentSerieLength = segmentSerieIndex.length;

		final ArrayList<TourSegment> tourSegments = new ArrayList<TourSegment>(segmentSerieLength);
		final int firstSerieIndex = segmentSerieIndex[0];

		// get start values
		int distanceStart = distanceSerie[firstSerieIndex];
		int altitudeStart = altitudeSerie[firstSerieIndex];
		int timeStart = timeSerie[firstSerieIndex];

		segmentSerieAltitude = new int[segmentSerieLength];
		segmentSerieDistance = new int[segmentSerieLength];
		segmentSerieTime = new int[segmentSerieLength];
		segmentSerieDrivingTime = new int[segmentSerieLength];
		segmentSerieAltitudeDown = new int[segmentSerieLength];

		segmentSerieAltimeter = new float[segmentSerieLength];
		segmentSerieSpeed = new float[segmentSerieLength];
		segmentSeriePace = new float[segmentSerieLength];
		segmentSeriePower = new float[segmentSerieLength];
		segmentSerieGradient = new float[segmentSerieLength];
		segmentSeriePulse = new float[segmentSerieLength];
		segmentSerieCadence = new float[segmentSerieLength];

		for (int iSegment = 1; iSegment < segmentSerieLength; iSegment++) {

			final int segmentIndex = iSegment;

			final int segmentStartIndex = segmentSerieIndex[iSegment - 1];
			final int segmentEndIndex = segmentSerieIndex[iSegment];

			final TourSegment segment = new TourSegment();
			tourSegments.add(segment);

			segment.serieIndexStart = segmentStartIndex;
			segment.serieIndexEnd = segmentEndIndex;

			// compute difference values between start and end
			final int altitudeEnd = altitudeSerie[segmentEndIndex];
			final int distanceEnd = distanceSerie[segmentEndIndex];
			final int timeEnd = timeSerie[segmentEndIndex];
			final int recordingTime = timeEnd - timeStart;
			final int drivingTime;

			segmentSerieAltitude[segmentIndex] = segment.altitude = altitudeEnd - altitudeStart;
			segmentSerieDistance[segmentIndex] = segment.distance = distanceEnd - distanceStart;

			segmentSerieTime[segmentIndex] = segment.recordingTime = recordingTime;

			segmentSerieDrivingTime[segmentIndex] = segment.drivingTime = drivingTime = //
			Math.max(0, recordingTime - getBreakTime(segmentStartIndex, segmentEndIndex));

			int[] localPowerSerie = getPowerSerie();
			int altitudeUp = 0;
			int altitudeDown = 0;
			int pulseSum = 0;
			int powerSum = 0;

			int altitude1 = altitudeSerie[segmentStartIndex];

			// compute altitude up/down, pulse and power for a segment
			for (int serieIndex = segmentStartIndex + 1; serieIndex <= segmentEndIndex; serieIndex++) {

				final int altitude2 = altitudeSerie[serieIndex];
				final int altitudeDiff = altitude2 - altitude1;
				altitude1 = altitude2;

				altitudeUp += altitudeDiff >= 0 ? altitudeDiff : 0;
				altitudeDown += altitudeDiff < 0 ? altitudeDiff : 0;

				powerSum += localPowerSerie[serieIndex];

				if (pulseSerie != null) {
					pulseSum += pulseSerie[serieIndex];
				}
			}

			segment.altitudeUp = altitudeUp;
			segmentSerieAltitudeDown[segmentIndex] = segment.altitudeDown = altitudeDown;

			segmentSerieSpeed[segmentIndex] = segment.speed //
			= drivingTime == 0 ? 0 : (float) ((float) segment.distance / drivingTime * 3.6 / UI.UNIT_VALUE_DISTANCE);

			segmentSeriePace[segmentIndex] = segment.pace //
			= drivingTime == 0 ? 0 : (float) ((float) drivingTime * 16.666 / segment.distance * UI.UNIT_VALUE_DISTANCE);

			segmentSerieGradient[segmentIndex] = segment.gradient //
			= (float) segment.altitude * 100 / segment.distance;

			segmentSerieAltimeter[segmentIndex] = drivingTime == 0 ? 0 : (float) (altitudeUp + altitudeDown)
					/ recordingTime
					* 3600
					/ UI.UNIT_VALUE_ALTITUDE;

			segmentSeriePower[segmentIndex] = segment.power = powerSum / (segmentEndIndex - segmentStartIndex);

			if (segmentSeriePulse != null) {
				segmentSeriePulse[segmentIndex] = pulseSum / (segmentEndIndex - segmentStartIndex);
			}

			// end point of current segment is the start of the next segment
			altitudeStart = altitudeEnd;
			distanceStart = distanceEnd;
			timeStart = timeEnd;
		}

		fTourSegments = tourSegments.toArray();

		return fTourSegments;
	}

	public void dumpData() {

		PrintStream out = System.out;

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
		PrintStream out = System.out;

		out.print((getTourRecordingTime() / 3600) + ":" //$NON-NLS-1$
				+ ((getTourRecordingTime() % 3600) / 60)
				+ ":" //$NON-NLS-1$
				+ ((getTourRecordingTime() % 3600) % 60)
				+ "  "); //$NON-NLS-1$
		out.print(getTourDistance());
	}

	public void dumpTourTotal() {

		PrintStream out = System.out;

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

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}

		if (this == obj) {
			return true;
		}

		if (!this.getClass().equals(obj.getClass())) {
			return false;
		}

		TourData td = (TourData) obj;

		return this.getStartYear() == td.getStartYear()
				&& this.getStartMonth() == td.getStartMonth()
				&& this.getStartDay() == td.getStartDay()
				&& this.getStartHour() == td.getStartHour()
				&& this.getStartMinute() == td.getStartMinute()
				&& this.getTourDistance() == td.getTourDistance()
				&& this.getTourRecordingTime() == td.getTourRecordingTime();
	}

	/**
	 * @return Returns the metric or imperial altimeter serie depending on the active measurement
	 */
	public int[] getAltimeterSerie() {

//		if (altimeterSerie == null) {
//			return null;
//		}

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
	 * @return Returns the metric or imperial altitude serie depending on the active measurement
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
	public int getBreakTime(int startIndex, int endIndex) {

		if (distanceSerie == null) {
			return 0;
		}

		final int minBreakTime = 10;

		if (deviceTimeInterval == -1) {

			// variable time slices

			return computeBreakTimeVariable(minBreakTime, startIndex, endIndex);

		} else {

			// fixed time slices

			int ignoreTimeSlices = deviceTimeInterval == 0 ? //
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

		int ignoreTimeCounter = 0;
		int oldDistance = 0;

		sliceMin = Math.max(sliceMin, 1);
		indexRight = Math.min(indexRight, distanceValues.length - 1);

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
	public String getCalories() {
		return calories;
	}

	public int getDeviceDistance() {
		return deviceDistance;
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
		if (devicePluginName == null) {
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

	public int getDeviceTotalDown() {
		return deviceTotalDown;
	}

	public int getDeviceTotalUp() {
		return deviceTotalUp;
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

	public int getDeviceWheel() {
		return deviceWheel;
	}

	/**
	 * @return Returns the distance data serie for the current measurement system which can be
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

	/**
	 * @return Returns the distance serie from the metric system, the distance serie is <b>always</b>
	 *         saved in the database in the metric system
	 */
	public int[] getMetricDistanceSerie() {
		return distanceSerie;
	}

	public int[] getPaceSerie() {

		if (UI.UNIT_VALUE_DISTANCE == 1) {

			// use metric system

			if (paceSerie == null) {
				computeSpeedSerie();
			}

			return paceSerie;

		} else {

			// use imperial system

			if (paceSerieImperial == null) {
				computeSpeedSerie();
			}

			return paceSerieImperial;
		}
	}

	public int[] getPowerSerie() {

		if (powerSerie != null || isPowerSerieFromDevice) {
			return powerSerie;
		}

		if (speedSerie == null) {
			computeSpeedSerie();
		}

		if (gradientSerie == null) {
			computeAltimeterGradientSerie();
		}

		// check if required data series are available 
		if (speedSerie == null || gradientSerie == null) {
			return null;
		}

		powerSerie = new int[timeSerie.length];

		int weightBody = 75;
		int weightBike = 10;
		int bodyHeight = 188;

		float cR = 0.008f; // Rollreibungskoeffizient Asphalt
		float cD = 0.8f;// Strömungskoeffizient
		float p = 1.145f; // 20C / 400m
//		float p = 0.968f; // 10C / 2000m

		float weightTotal = weightBody + weightBike;
		float bsa = (float) (0.007184f * Math.pow(weightBody, 0.425) * Math.pow(bodyHeight, 0.725));
		float aP = bsa * 0.185f;

		float fRoll = weightTotal * 9.81f * cR;
		float fSlope = weightTotal * 9.81f; // * gradient/100
		float fAir = 0.5f * p * cD * aP;// * v2;

		for (int timeIndex = 0; timeIndex < timeSerie.length; timeIndex++) {

			float speed = (float) speedSerie[timeIndex] / 36; // speed (m/s) /10
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

			float fTotal = fRoll + fAirTotal + fSlopeTotal;

			int pTotal = (int) (fTotal * speed);

//			if (pTotal > 600) {
//				pTotal = pTotal * 1;
//			}
			powerSerie[timeIndex] = pTotal < 0 ? 0 : pTotal;
		}

		return powerSerie;
	}

	public SerieData getSerieData() {
		return serieData;
	}

	public int[] getSpeedSerie() {

		if (isSpeedSerieFromDevice) {
			return getSpeedSerieInternal();
		}
		if (distanceSerie == null) {
			return null;
		}

		return getSpeedSerieInternal();
	}

	private int[] getSpeedSerieInternal() {

		/*
		 * when the speed series are not computed, the internal algorithm will be used to create the
		 * speed data serie
		 */
		if (UI.UNIT_VALUE_DISTANCE == 1) {

			// use metric system

			if (speedSerie == null) {
				computeSpeedSerieInternalWithFixedInterval();
			}

			return speedSerie;

		} else {

			// use imperial system

			if (speedSerieImperial == null) {
				computeSpeedSerieInternalWithFixedInterval();
			}

			return speedSerieImperial;
		}
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

	public short getStartMonth() {
		return startMonth;
	}

	public short getStartPulse() {
		return startPulse;
	}

	public short getStartWeek() {
		return startWeek;
	}

	public short getStartYear() {
		return startYear;
	}

	public int[] getTemperatureSerie() {

		if (temperatureSerie == null) {
			return null;
		}

		int[] serie;

		final float unitValueTempterature = UI.UNIT_VALUE_TEMPERATURE;
		float fahrenheitMulti = UI.UNIT_FAHRENHEIT_MULTI;
		float fahrenheitAdd = UI.UNIT_FAHRENHEIT_ADD;

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

	public int getTourAltDown() {
		return tourAltDown;
	}

	public int getTourAltUp() {
		return tourAltUp;
	}

	public TourBike getTourBike() {
		return tourBike;
	}

	public Collection<TourCategory> getTourCategory() {
		return tourCategory;
	}

	/**
	 * @return the tourDescription
	 */
	public String getTourDescription() {
		return tourDescription == null ? "" : tourDescription; //$NON-NLS-1$
	}

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
		return tourEndPlace == null ? "" : tourEndPlace; //$NON-NLS-1$
	}

	/**
	 * @return Returns the unique key in the database for this {@link TourData} entity
	 */
	public Long getTourId() {
		return tourId;
	}

	public Set<TourMarker> getTourMarkers() {
		return tourMarkers;
	}

	/**
	 * @return returns the person for whom the tour data is saved or <code>null</code> when the
	 *         tour is not saved in the database
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

	public Object[] getTourSegments() {
		return fTourSegments;
	}

	/**
	 * @return the tourStartPlace
	 */
	public String getTourStartPlace() {
		return tourStartPlace == null ? "" : tourStartPlace; //$NON-NLS-1$
	}

	/**
	 * @return the tourTitle
	 */
	public String getTourTitle() {
		return tourTitle == null ? "" : tourTitle; //$NON-NLS-1$
	}

	public TourType getTourType() {
		return tourType;
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
	 * Called after the object was loaded from the persistence store
	 */
	@PostLoad
	@PostUpdate
	public void onPostLoad() {

		timeSerie = serieData.timeSerie;

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
		 * cleanup dataseries because dataseries has been saved before version 1.3 even when no data
		 * are available
		 */
		int sumAltitude = 0;
		int sumCadence = 0;
		int sumDistance = 0;
		int sumPulse = 0;
		int sumTemperature = 0;
		int sumPower = 0;
		int sumSpeed = 0;

		// get first valid latitude/longitude
		if (latitudeSerie != null && longitudeSerie != null) {

			for (int timeIndex = 0; timeIndex < timeSerie.length; timeIndex++) {
				if (latitudeSerie[timeIndex] != Double.MIN_VALUE && longitudeSerie[timeIndex] != Double.MIN_VALUE) {
					mapMinLatitude = mapMaxLatitude = latitudeSerie[timeIndex] + 90;
					mapMinLongitude = mapMaxLongitude = longitudeSerie[timeIndex] + 180;
					break;
				}
			}
		}
		double lastValidLatitude = mapMinLatitude - 90;
		double lastValidLongitude = mapMinLongitude - 180;

		for (int timeIndex = 0; timeIndex < timeSerie.length; timeIndex++) {

			if (altitudeSerie != null) {
				sumAltitude += altitudeSerie[timeIndex];
			}
			if (cadenceSerie != null) {
				sumCadence += cadenceSerie[timeIndex];
			}
			if (distanceSerie != null) {
				sumDistance += distanceSerie[timeIndex];
			}
			if (pulseSerie != null) {
				sumPulse += pulseSerie[timeIndex];
			}
			if (temperatureSerie != null) {
				final int temp = temperatureSerie[timeIndex];
				sumTemperature += (temp < 0) ? -temp : temp;
			}
			if (powerSerie != null) {
				sumPower += powerSerie[timeIndex];
			}
			if (speedSerie != null) {
				sumSpeed += speedSerie[timeIndex];
			}

			if (latitudeSerie != null && longitudeSerie != null) {

				final double latitude = latitudeSerie[timeIndex];
				final double longitude = longitudeSerie[timeIndex];

				if (latitude == Double.MIN_VALUE || longitude == Double.MIN_VALUE) {
					latitudeSerie[timeIndex] = lastValidLatitude;
					longitudeSerie[timeIndex] = lastValidLongitude;
				} else {
					latitudeSerie[timeIndex] = lastValidLatitude = latitude;
					longitudeSerie[timeIndex] = lastValidLongitude = longitude;
				}

				mapMinLatitude = Math.min(mapMinLatitude, lastValidLatitude + 90);
				mapMaxLatitude = Math.max(mapMaxLatitude, lastValidLatitude + 90);
				mapMinLongitude = Math.min(mapMinLongitude, lastValidLongitude + 180);
				mapMaxLongitude = Math.max(mapMaxLongitude, lastValidLongitude + 180);
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

	/**
	 * @param avgCadence
	 *        the avgCadence to set
	 */
	public void setAvgCadence(int avgCadence) {
		this.avgCadence = avgCadence;
	}

	/**
	 * @param avgPulse
	 *        the avgPulse to set
	 */
	public void setAvgPulse(int avgPulse) {
		this.avgPulse = avgPulse;
	}

	/**
	 * @param avgTemperature
	 *        the avgTemperature to set
	 */
	public void setAvgTemperature(int avgTemperature) {
		this.avgTemperature = avgTemperature;
	}

	/**
	 * @param bikerWeight
	 *        the bikerWeight to set
	 */
	public void setBikerWeight(float bikerWeight) {
		this.bikerWeight = bikerWeight;
	}

	/**
	 * @param calories
	 *        the calories to set
	 */
	public void setCalories(String calories) {
		this.calories = calories;
	}

	public void setDeviceDistance(int deviceDistance) {
		this.deviceDistance = deviceDistance;
	}

	public void setDeviceId(String deviceId) {
		this.devicePluginId = deviceId;
	}

	public void setDeviceMode(short deviceMode) {
		this.deviceMode = deviceMode;
	}

	public void setDeviceModeName(String deviceModeName) {
		this.deviceModeName = deviceModeName;
	}

	public void setDeviceName(String deviceName) {
		devicePluginName = deviceName;
	}

	/**
	 * time difference between 2 time slices or <code>-1</code> for GPS devices or ergometer when
	 * the time slices are not equally
	 * 
	 * @param deviceTimeInterval
	 */
	public void setDeviceTimeInterval(short deviceTimeInterval) {
		this.deviceTimeInterval = deviceTimeInterval;
	}

	public void setDeviceTotalDown(int deviceTotalDown) {
		this.deviceTotalDown = deviceTotalDown;
	}

	public void setDeviceTotalUp(int deviceTotalUp) {
		this.deviceTotalUp = deviceTotalUp;
	}

/*
 * private int maxAltitude; private int maxPulse; private int avgPulse; private int avgCadence;
 * private int avgTemperature; private float maxSpeed;
 */

	public void setDeviceTourType(String tourType) {
		this.deviceTourType = tourType;
	}

	public void setDeviceTravelTime(long deviceTravelTime) {
		this.deviceTravelTime = deviceTravelTime;
	}

	public void setDeviceWeight(int deviceWeight) {
		this.deviceWeight = deviceWeight;
	}

	public void setDeviceWheel(int deviceWheel) {
		this.deviceWheel = deviceWheel;
	}

	public void setDpTolerance(short dpTolerance) {
		this.dpTolerance = dpTolerance;
	}

	/**
	 * @param maxAltitude
	 *        the maxAltitude to set
	 */
	public void setMaxAltitude(int maxAltitude) {
		this.maxAltitude = maxAltitude;
	}

	/**
	 * @param maxPulse
	 *        the maxPulse to set
	 */
	public void setMaxPulse(int maxPulse) {
		this.maxPulse = maxPulse;
	}

	/**
	 * @param maxSpeed
	 *        the maxSpeed to set
	 */
	public void setMaxSpeed(float maxSpeed) {
		this.maxSpeed = maxSpeed;
	}

//	public void setSerieData(SerieData serieData) {
//		this.serieData = serieData;
//	}

	public void setStartAltitude(short startAltitude) {
		this.startAltitude = startAltitude;
	}

	public void setStartDay(short startDay) {
		this.startDay = startDay;
	}

	public void setStartDistance(int startDistance) {
		this.startDistance = startDistance;
	}

	public void setStartHour(short startHour) {
		this.startHour = startHour;
	}

	public void setStartMinute(short startMinute) {
		this.startMinute = startMinute;
	}

	public void setStartMonth(short startMonth) {
		this.startMonth = startMonth;
	}

	public void setStartPulse(short startPulse) {
		this.startPulse = startPulse;
	}

	public void setStartWeek(short startWeek) {
		this.startWeek = startWeek;
	}

	public void setStartYear(short startYear) {
		this.startYear = startYear;
	}

	public void setTourAltDown(int tourAltDown) {
		this.tourAltDown = tourAltDown;
	}

	public void setTourAltUp(int tourAltUp) {
		this.tourAltUp = tourAltUp;
	}

	public void setTourBike(TourBike tourBike) {
		this.tourBike = tourBike;
	}

	/**
	 * @param tourDescription
	 *        the tourDescription to set
	 */
	public void setTourDescription(String tourDescription) {
		this.tourDescription = tourDescription;
	}

	/**
	 * Set total driving time
	 * 
	 * @param tourDrivingTime
	 */
	public void setTourDrivingTime(int tourDrivingTime) {
		this.tourDrivingTime = tourDrivingTime;
	}

	/**
	 * @param tourEndPlace
	 *        the tourEndPlace to set
	 */
	public void setTourEndPlace(String tourEndPlace) {
		this.tourEndPlace = tourEndPlace;
	}

	public void setTourId(Long tourId) {
		this.tourId = tourId;
	}

	public void setTourMarkers(Set<TourMarker> tourMarkers) {
		this.tourMarkers = tourMarkers;
	}

	/**
	 * Sets the {@link TourPerson} for the tour or <code>null</code> when the tour is not saved in
	 * the database
	 * 
	 * @param tourPerson
	 */
	public void setTourPerson(TourPerson tourPerson) {
		this.tourPerson = tourPerson;
	}

	/**
	 * @param tourStartPlace
	 *        the tourStartPlace to set
	 */
	public void setTourStartPlace(String tourStartPlace) {
		this.tourStartPlace = tourStartPlace;
	}

	/**
	 * @param tourTitle
	 *        the tourTitle to set
	 */
	public void setTourTitle(String tourTitle) {
		this.tourTitle = tourTitle;
	}

	public void setTourType(TourType tourType) {
		this.tourType = tourType;
	}

}
