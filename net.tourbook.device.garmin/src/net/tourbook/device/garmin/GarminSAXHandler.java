/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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
package net.tourbook.device.garmin;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.TimeZone;

import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.TourbookDevice;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class GarminSAXHandler extends DefaultHandler {

	private static final String		TRAINING_CENTER_DATABASE_V1	= "http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v1"; //$NON-NLS-1$
	private static final String		TRAINING_CENTER_DATABASE_V2	= "http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2"; //$NON-NLS-1$

	private static final String		TAG_DATABASE				= "TrainingCenterDatabase";									//$NON-NLS-1$

	private static final String		TAG_ACTIVITY				= "Activity";													//$NON-NLS-1$
	private static final String		TAG_COURSE					= "Course";													//$NON-NLS-1$
	private static final String		TAG_HISTORY					= "History";													//$NON-NLS-1$

	private static final String		TAG_LAP						= "Lap";														//$NON-NLS-1$
	private static final String		TAG_TRACKPOINT				= "Trackpoint";												//$NON-NLS-1$

	private static final String		TAG_LONGITUDE_DEGREES		= "LongitudeDegrees";											//$NON-NLS-1$
	private static final String		TAG_LATITUDE_DEGREES		= "LatitudeDegrees";											//$NON-NLS-1$
	private static final String		TAG_ALTITUDE_METERS			= "AltitudeMeters";											//$NON-NLS-1$
	private static final String		TAG_DISTANCE_METERS			= "DistanceMeters";											//$NON-NLS-1$
	private static final String		TAG_CADENCE					= "Cadence";													//$NON-NLS-1$
	private static final String		TAG_HEART_RATE_BPM			= "HeartRateBpm";												//$NON-NLS-1$
	private static final String		TAG_TIME					= "Time";														//$NON-NLS-1$
	private static final String		TAG_VALUE					= "Value";														//$NON-NLS-1$

	private static final String		DEFALULT_UNIQUE_KEY			= "42984";														//$NON-NLS-1$

	private static final Calendar	fCalendar					= GregorianCalendar.getInstance();
	private static final DateFormat	iso							= new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");			//$NON-NLS-1$

	private int						fDataVersion				= -1;
	private boolean					fIsInActivity				= false;
	private boolean					fIsInCourse					= false;

	private boolean					fIsInLap					= false;
	private boolean					fIsInTrackpoint				= false;
	private boolean					fIsInTime					= false;
	private boolean					fIsInLatitude				= false;
	private boolean					fIsInLongitude				= false;
	private boolean					fIsInAltitude				= false;
	private boolean					fIsInDistance				= false;
	private boolean					fIsInCadence;

	private boolean					fIsInHeartRate				= false;
	private boolean					fIsInHeartRateValue			= false;

	private ArrayList<TimeData>		fTimeDataList				= new ArrayList<TimeData>();
	private TimeData				fTimeData;
	private TourbookDevice			fDeviceDataReader;

	private String					fImportFilePath;

	private HashMap<Long, TourData>	fTourDataMap;

	private int						fLapCounter;
	private boolean					fSetLapMarker				= false;
	private boolean					fSetLapStartTime			= false;
	private ArrayList<Long>			fLapStart					= new ArrayList<Long>();

	private boolean					fIsImported;
	private long					fCurrentTime;

	private StringBuilder			fCharacters					= new StringBuilder();

	{
		iso.setTimeZone(TimeZone.getTimeZone("GMT")); //$NON-NLS-1$
	}

	public GarminSAXHandler(final TourbookDevice deviceDataReader,
							final String importFileName,
							final DeviceData deviceData,
							final HashMap<Long, TourData> tourDataMap) {

		fDeviceDataReader = deviceDataReader;
		fImportFilePath = importFileName;
		fTourDataMap = tourDataMap;
	}

	@Override
	public void characters(final char[] chars, final int startIndex, final int length) throws SAXException {

		if (fIsInTime
				|| fIsInLatitude
				|| fIsInLongitude
				|| fIsInAltitude
				|| fIsInDistance
				|| fIsInCadence
				|| fIsInHeartRate
				|| fIsInHeartRateValue) {

			fCharacters.append(chars, startIndex, length);
		}
	}

//	/**
//	 * Polar Coordinate Flat-Earth Formula
//	 * 
//	 * <pre>
//	 * a = pi/2 - lat1
//	 * b = pi/2 - lat2
//	 * c = sqrt( a&circ;2 + b&circ;2 - 2 * a * b * cos(lon2 - lon1) )
//	 * d = R * c 
//	 * 
//	 * with R = 6,378.8 km or 3,963.0 mi
//	 * </pre>
//	 * 
//	 * @param td2
//	 * @param td1
//	 */
//	private void computeDistance1() {
//
//		TimeData td1 = null;
//		float totalDistance = 0;
//
//		for (TimeData td2 : fTimeDataList) {
//
//			if (td1 == null) {
//				// set initial data
//				td1 = td2;
//				continue;
//			}
//
//			final double lat1 = td1.latitude;
//			final double lat2 = td2.latitude;
//			final double lon1 = td1.longitude;
//			final double lon2 = td2.longitude;
//
//			final double a = MATH_PI2 - lat1;
//			final double b = MATH_PI2 - lat2;
//
//			double c = Math.sqrt(a * a + b * b - 2 * a * b * Math.cos(lon2 - lon1));
//
//			td1.absoluteDistance = (int) (totalDistance += 6378.8f * c);
//
//			td1 = td2;
//		}
//	}

//	private int getFloatIntValue(String textValue) {
//
//		try {
//			if (textValue != null) {
//				return (int) Float.parseFloat(textValue);
//			} else {
//				return Integer.MIN_VALUE;
//			}
//
//		} catch (NumberFormatException e) {
//			return Integer.MIN_VALUE;
//		}
//	}

//	/**
//	 */
//	private void computeDistance3() {
//
//		TimeData td1 = null;
//		float totalDistance = 0;
//
//		for (TimeData td2 : fTimeDataList) {
//
//			if (td1 == null) {
//				// set initial data
//				td1 = td2;
//				continue;
//			}
//
//			final double lat1 = td1.latitude;
//			final double lat2 = td2.latitude;
//			final double lon1 = td1.longitude;
//			final double lon2 = td2.longitude;
//
//			double dLat = (lat2 - lat1) / DEGREE_TO_RAD;
//			double dLon = (lon2 - lon1) / DEGREE_TO_RAD;
//
//			double a = Math.sin(dLat / 2)
//					* Math.sin(dLat / 2)
//					+ Math.cos(lat1 / DEGREE_TO_RAD)
//					* Math.cos(lat2 / DEGREE_TO_RAD)
//					* Math.sin(dLon / 2)
//					* Math.sin(dLon / 2);
//
//			double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)) / Math.PI;
//
//			td1.absoluteDistance = (int) (totalDistance += 6378.8f * c);
//
//			td1 = td2;
//		}
//	}

//	private short getFloatShortValue(String textValue) {
//
//		try {
//			if (textValue != null) {
//				return (short) Float.parseFloat(textValue);
//			} else {
//				return Short.MIN_VALUE;
//			}
//
//		} catch (NumberFormatException e) {
//			return Short.MIN_VALUE;
//		}
//	}

//	/**
//	 * Polar Coordinate Flat-Earth Formula
//	 * 
//	 * <pre>
//	 * dlon = lon2 - lon1
//	 * dlat = lat2 - lat1
//	 * a = (sin(dlat/2))&circ;2 + cos(lat1) * cos(lat2) * (sin(dlon/2))&circ;2
//	 * c = 2 * arcsin(min(1,sqrt(a)))
//	 * d = R * c
//	 * 
//	 * with R = 6,378.8 km or 3,963.0 mi
//	 * </pre>
//	 * 
//	 * @param td2
//	 * @param td1
//	 */
//	private void computeDistanceFromLatLon() {
//
//		TimeData prevTimeData = null;
//		float totalDistance = 0;
//
//		final boolean adjustDistance = fTimeDataList.get(0).absoluteAltitude != Float.MIN_VALUE;
//
//		for (TimeData currentTimeData : fTimeDataList) {
//
//			if (prevTimeData == null) {
//
//				// set first timedata
//
//				prevTimeData = currentTimeData;
//				continue;
//			}
//
//			final double lat1 = prevTimeData.latitude / DEGREE_TO_RAD;
//			final double lat2 = currentTimeData.latitude / DEGREE_TO_RAD;
//			final double lon1 = prevTimeData.longitude / DEGREE_TO_RAD;
//			final double lon2 = currentTimeData.longitude / DEGREE_TO_RAD;
//
//			final double dlon = lon2 - lon1;
//			final double dlat = lat2 - lat1;
//
//			final double sinDlat = Math.sin(dlat / 2);
//			final double sinDlon = Math.sin(dlon / 2);
//
//			final double a = (sinDlat * sinDlat)
//					+ Math.cos(lat1)
//					* Math.cos(lat2)
//					* (sinDlon * sinDlon);
//
//			final double c = 2 * Math.asin(Math.min(1, Math.sqrt(a))) / Math.PI;
//
//			double distance = 6378.8f * c;
//
//			if (adjustDistance) {
//
////				Strecke = SQRT(DIST² + ALT²))
//
//				double altitude = currentTimeData.absoluteAltitude - prevTimeData.absoluteAltitude;
//				distance = Math.sqrt(distance * distance + altitude * altitude);
//			}
//
//			currentTimeData.absoluteDistance = (int) (totalDistance += distance) / 1000;
//
//			prevTimeData = currentTimeData;
//		}
//	}

//	/**
//	 * Polar Coordinate Flat-Earth Formula
//	 * 
//	 * <pre>
//	 * 	DIST = 1,852 * 60 * ARCCOS( SIN(B1) * SIN(B2) + COS(B1) * COS(B2) * COS(A1-A2) )
//	 * 
//	 * 	Spherical law of cosines: 	d = acos(sin(lat1).sin(lat2)+cos(lat1).cos(lat2).cos(long2-long1)).R
//	 * 
//	 * 	var R = 6371; // km
//	 * 	var d = Math.acos(Math.sin(lat1)*Math.sin(lat2) + 
//	 * 	              Math.cos(lat1)*Math.cos(lat2) *
//	 * 	              Math.cos(lon2-lon1)) * R;
//	 * </pre>
//	 * 
//	 * @param td2
//	 * @param td1
//	 */
//	private void computeDistanceFromLatLon2() {
//
//		TimeData prevTimeData = null;
//		float totalDistance = 0;
//
//		final boolean adjustDistance = fTimeDataList.get(0).absoluteAltitude != Float.MIN_VALUE;
//
//		for (TimeData currentTimeData : fTimeDataList) {
//
//			if (prevTimeData == null) {
//
//				// set first timedata
//
//				prevTimeData = currentTimeData;
//				continue;
//			}
//
//			final double lat1 = prevTimeData.latitude / DEGREE_TO_RAD;
//			final double lat2 = currentTimeData.latitude / DEGREE_TO_RAD;
//			final double lon1 = prevTimeData.longitude / DEGREE_TO_RAD;
//			final double lon2 = currentTimeData.longitude / DEGREE_TO_RAD;
//
////			double distance = 1.852f * 60 * Math.acos(Math.sin(lat1)
////					* Math.sin(lat2)
////					+ Math.cos(lat1)
////					* Math.cos(lat2)
////					* Math.cos(lon2 - lon1));
//
//			double distance = Math.acos(Math.sin(lat1)
//					* Math.sin(lat2)
//					+ Math.cos(lat1)
//					* Math.cos(lat2)
//					* Math.cos(lon2 - lon1))
//					* 6371
//					/ Math.PI;
//
////			if (adjustDistance) {
////
////				// Strecke = SQRT(DIST² + ALT²))
////
////				double altitude = currentTimeData.absoluteAltitude - prevTimeData.absoluteAltitude;
////				distance = Math.sqrt(distance * distance + altitude * altitude);
////			}
//
//			currentTimeData.absoluteDistance = (int) (totalDistance += distance);
//
////			System.out.println((int) distance);
//
//			prevTimeData = currentTimeData;
//		}
//
////		System.out.println("total distance:" + totalDistance);
//	}

	public void dispose() {
		fLapStart.clear();
		fTimeDataList.clear();
	}

	@Override
	public void endElement(final String uri, final String localName, final String name) throws SAXException {

//		System.out.println("</" + name + ">");

		try {

			if (fIsInTrackpoint) {
				getTrackPointDataEnd(name);
			}

			if (name.equals(TAG_TRACKPOINT)) {

				/*
				 * keep trackpoint data
				 */
				fIsInTrackpoint = false;

				if (fTimeData != null) {

					// set virtual time if time is not available
					if (fTimeData.absoluteTime == Long.MIN_VALUE) {
						fCalendar.set(2000, 0, 1, 0, 0, 0);
						fTimeData.absoluteTime = fCalendar.getTimeInMillis();
					}

					if (fSetLapMarker) {
						fSetLapMarker = false;

						fTimeData.marker = 1;
						fTimeData.markerLabel = Integer.toString(fLapCounter - 1);
					}

					fTimeDataList.add(fTimeData);
				}

				if (fSetLapStartTime) {
					fSetLapStartTime = false;
					fLapStart.add(fCurrentTime);
				}

			} else if (name.equals(TAG_LAP)) {

				fIsInLap = false;

			} else if (name.equals(TAG_ACTIVITY)) {

				/*
				 * version 2: activity and tour ends
				 */

				fIsInActivity = false;

				setTourData();

			} else if (name.equals(TAG_COURSE) || name.equals(TAG_HISTORY)) {

				/*
				 * version 1+2: course and tour ends, v1: history ends
				 */

				fIsInCourse = false;

				setTourData();
			}

		} catch (final NumberFormatException e) {
			e.printStackTrace();
		} catch (final ParseException e) {
			e.printStackTrace();
		}

	}

	private double getDoubleValue(final String textValue) {

		try {
			if (textValue != null) {
				return Double.parseDouble(textValue);
			} else {
				return Double.MIN_VALUE;
			}

		} catch (final NumberFormatException e) {
			return Double.MIN_VALUE;
		}
	}

	private float getFloatValue(final String textValue) {

		try {
			if (textValue != null) {
				return Float.parseFloat(textValue);
			} else {
				return Float.MIN_VALUE;
			}

		} catch (final NumberFormatException e) {
			return Float.MIN_VALUE;
		}
	}

	private short getShortValue(final String textValue) {

		try {
			if (textValue != null) {
				return Short.parseShort(textValue);
			} else {
				return Short.MIN_VALUE;
			}
		} catch (final NumberFormatException e) {
			return Short.MIN_VALUE;
		}
	}

	private void getTrackPointDataEnd(final String name) throws ParseException {

		if (fIsInHeartRateValue && name.equals(TAG_VALUE)) {

			fIsInHeartRateValue = false;

			if (fDataVersion == 2) {
				short pulse = getShortValue(fCharacters.toString());
				pulse = pulse == Integer.MIN_VALUE ? 0 : pulse;
				fTimeData.pulse = pulse;
			}

		} else if (name.equals(TAG_HEART_RATE_BPM)) {

			fIsInHeartRate = false;

			if (fDataVersion == 1) {
				short pulse = getShortValue(fCharacters.toString());
				pulse = pulse == Integer.MIN_VALUE ? 0 : pulse;
				fTimeData.pulse = pulse;
			}

		} else if (name.equals(TAG_ALTITUDE_METERS)) {

			fIsInAltitude = false;

			fTimeData.absoluteAltitude = getFloatValue(fCharacters.toString());

		} else if (name.equals(TAG_DISTANCE_METERS)) {

			fIsInDistance = false;
			fTimeData.absoluteDistance = getFloatValue(fCharacters.toString());

		} else if (name.equals(TAG_CADENCE)) {

			fIsInCadence = false;
			short cadence = getShortValue(fCharacters.toString());
			cadence = cadence == Integer.MIN_VALUE ? 0 : cadence;
			fTimeData.cadence = cadence;

		} else if (name.equals(TAG_LATITUDE_DEGREES)) {

			fIsInLatitude = false;

			fTimeData.latitude = getDoubleValue(fCharacters.toString());

		} else if (name.equals(TAG_LONGITUDE_DEGREES)) {

			fIsInLongitude = false;

			fTimeData.longitude = getDoubleValue(fCharacters.toString());

		} else if (name.equals(TAG_TIME)) {

			fIsInTime = false;

			fCurrentTime = iso.parse(fCharacters.toString()).getTime();

			fTimeData.absoluteTime = fCurrentTime;

		}
	}

	private void getTrackPointDataStart(final String name) {

		if (name.equals(TAG_HEART_RATE_BPM)) {
			fIsInHeartRate = true;
			fCharacters.delete(0, fCharacters.length());

		} else if (name.equals(TAG_ALTITUDE_METERS)) {
			fIsInAltitude = true;
			fCharacters.delete(0, fCharacters.length());

		} else if (name.equals(TAG_DISTANCE_METERS)) {
			fIsInDistance = true;
			fCharacters.delete(0, fCharacters.length());

		} else if (name.equals(TAG_CADENCE)) {
			fIsInCadence = true;
			fCharacters.delete(0, fCharacters.length());

		} else if (name.equals(TAG_TIME)) {
			fIsInTime = true;
			fCharacters.delete(0, fCharacters.length());

		} else if (name.equals(TAG_LATITUDE_DEGREES)) {
			fIsInLatitude = true;
			fCharacters.delete(0, fCharacters.length());

		} else if (name.equals(TAG_LONGITUDE_DEGREES)) {
			fIsInLongitude = true;
			fCharacters.delete(0, fCharacters.length());

		} else if (fIsInHeartRate && name.equals(TAG_VALUE)) {
			fIsInHeartRateValue = true;
			fCharacters.delete(0, fCharacters.length());
		}
	}

	/**
	 * @return Returns <code>true</code> when a tour was imported
	 */
	public boolean isImported() {
		return fIsImported;
	}

	private void setTourData() {

		if (fTimeDataList.size() == 0) {
			return;
		}

		// check if the distance is set
//		if (fTimeDataList.get(0).absoluteDistance == Float.MIN_VALUE) {
//		computeDistanceFromLatLon();
//		}

		validateTimeSeries();

		// create data object for each tour
		final TourData tourData = new TourData();

		/*
		 * set tour start date/time
		 */
		fCalendar.setTimeInMillis(fTimeDataList.get(0).absoluteTime);

		tourData.setStartMinute((short) fCalendar.get(Calendar.MINUTE));
		tourData.setStartHour((short) fCalendar.get(Calendar.HOUR_OF_DAY));
		tourData.setStartDay((short) fCalendar.get(Calendar.DAY_OF_MONTH));
		tourData.setStartMonth((short) (fCalendar.get(Calendar.MONTH) + 1));
		tourData.setStartYear((short) fCalendar.get(Calendar.YEAR));
		tourData.setStartWeek((short) fCalendar.get(Calendar.WEEK_OF_YEAR));

		tourData.setDeviceTimeInterval((short) -1);
		tourData.importRawDataFile = fImportFilePath;
		tourData.setTourImportFilePath(fImportFilePath);

		tourData.createTimeSeries(fTimeDataList, true);
		tourData.computeAltitudeUpDown();

		// after all data are added, the tour id can be created
		final int[] distanceSerie = tourData.getMetricDistanceSerie();
		String uniqueKey;
		if (distanceSerie == null) {
			uniqueKey = DEFALULT_UNIQUE_KEY;
		} else {
			final int lastDistance = distanceSerie[distanceSerie.length - 1];
			if (lastDistance < 0) {
				uniqueKey = DEFALULT_UNIQUE_KEY;
			} else {
				uniqueKey = Integer.toString(lastDistance);
			}
		}
		final Long tourId = tourData.createTourId(uniqueKey);

		// check if the tour is already imported
		if (fTourDataMap.containsKey(tourId) == false) {

			tourData.computeTourDrivingTime();
			tourData.computeComputedValues();

			tourData.setDeviceId(fDeviceDataReader.deviceId);
			tourData.setDeviceName(fDeviceDataReader.visibleName);

			// add new tour to other tours
			fTourDataMap.put(tourId, tourData);
		}

		fIsImported = true;
	}

	@Override
	public void startElement(final String uri, final String localName, final String name, final Attributes attributes)
			throws SAXException {

//		System.out.print("<" + name + ">");

		if (fDataVersion > 0) {

			if (fDataVersion == 1) {

				/*
				 * http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v1
				 */
				if (fIsInCourse) {

					if (fIsInTrackpoint) {

						getTrackPointDataStart(name);

					} else if (name.equals(TAG_TRACKPOINT)) {

						fIsInTrackpoint = true;

						// create new time item
						fTimeData = new TimeData();

					} else if (name.equals(TAG_LAP)) {

						fIsInLap = true;

						fLapCounter++;
						if (fLapCounter > 1) {
							fSetLapMarker = true;
						}
						fSetLapStartTime = true;
					}

				} else if (name.equals(TAG_COURSE) || name.equals(TAG_HISTORY)) {

					/*
					 * a new activity starts
					 */

					fIsInCourse = true;

					fLapCounter = 0;
					fSetLapMarker = false;
					fLapStart.clear();

					fTimeDataList.clear();
				}

			} else if (fDataVersion == 2) {

				/*
				 * http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2
				 */

				if (fIsInActivity) {

					if (fIsInLap) {

						if (fIsInTrackpoint) {

							getTrackPointDataStart(name);

						} else if (name.equals(TAG_TRACKPOINT)) {

							fIsInTrackpoint = true;

							// create new time item
							fTimeData = new TimeData();
						}

					} else if (name.equals(TAG_LAP)) {

						fIsInLap = true;

						fLapCounter++;
						if (fLapCounter > 1) {
							fSetLapMarker = true;
						}
						fSetLapStartTime = true;
					}

				} else if (fIsInCourse) {

					if (fIsInTrackpoint) {

						getTrackPointDataStart(name);

					} else if (name.equals(TAG_TRACKPOINT)) {

						fIsInTrackpoint = true;

						// create new time item
						fTimeData = new TimeData();
					}

				} else if (name.equals(TAG_ACTIVITY) || name.equals(TAG_COURSE)) {

					/*
					 * a new activity/course starts
					 */

					if (name.equals(TAG_ACTIVITY)) {
						fIsInActivity = true;
					} else if (name.equals(TAG_COURSE)) {
						fIsInCourse = true;
					}

					fLapCounter = 0;
					fSetLapMarker = false;
					fLapStart.clear();

					fTimeDataList.clear();
				}
			}

		} else if (name.equals(TAG_DATABASE)) {

			/*
			 * get version of the xml file
			 */
			for (int attrIndex = 0; attrIndex < attributes.getLength(); attrIndex++) {

				final String value = attributes.getValue(attrIndex);

				if (value.equalsIgnoreCase(TRAINING_CENTER_DATABASE_V1)) {
					fDataVersion = 1;
					return;
				} else if (value.equalsIgnoreCase(TRAINING_CENTER_DATABASE_V2)) {
					fDataVersion = 2;
					return;
				}
			}
		}
	}

	/**
	 * Remove duplicated entries
	 * <p>
	 * There are cases where the lap end time and the next lap start time have the same time value,
	 * so there are duplicated times which causes problems like markers are not displayed because
	 * the marker time is twice available.
	 */
	private void validateTimeSeries() {

		final ArrayList<TimeData> removeTimeData = new ArrayList<TimeData>();
		TimeData lastTimeData = null;

		for (final TimeData timeData : fTimeDataList) {

			if (lastTimeData != null) {
				if (lastTimeData.absoluteTime == timeData.absoluteTime) {
					removeTimeData.add(lastTimeData);
				}
			}

			lastTimeData = timeData;
		}

		fTimeDataList.removeAll(removeTimeData);
	}
}
