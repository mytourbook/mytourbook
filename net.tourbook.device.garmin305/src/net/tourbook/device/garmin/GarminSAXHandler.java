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

	private static double				MATH_PI2				= Math.PI / 2;
	private static double				DEGREE_TO_RAD			= Math.PI / 180;

	private static final String			TAG_DATABASE			= "TrainingCenterDatabase";						//$NON-NLS-1$
	private static final String			TAG_ACTIVITY			= "Activity";										//$NON-NLS-1$
	private static final String			TAG_COURSE				= "Course";										//$NON-NLS-1$

	private static final String			TAG_LAP					= "Lap";											//$NON-NLS-1$

	private static final String			TAG_LONGITUDE_DEGREES	= "LongitudeDegrees";								//$NON-NLS-1$
	private static final String			TAG_LATITUDE_DEGREES	= "LatitudeDegrees";								//$NON-NLS-1$
	private static final String			TAG_ALTITUDE_METERS		= "AltitudeMeters";								//$NON-NLS-1$
	private static final String			TAG_DISTANCE_METERS		= "DistanceMeters";								//$NON-NLS-1$
	private static final String			TAG_HEART_RATE_BPM		= "HeartRateBpm";									//$NON-NLS-1$
	private static final String			TAG_TIME				= "Time";											//$NON-NLS-1$
	private static final String			TAG_TRACKPOINT			= "Trackpoint";									//$NON-NLS-1$
	private static final String			TAG_VALUE				= "Value";											//$NON-NLS-1$

	private static final Calendar		fCalendar				= GregorianCalendar.getInstance();

	private static final DateFormat		iso						= new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); //$NON-NLS-1$
	private int							fVersion				= -1;
	private boolean						fIsInActivity			= false;
	private boolean						fIsInCourse				= false;

	private boolean						fIsInLap				= false;
	private boolean						fIsInTrackpoint			= false;
	private boolean						fIsInTime				= false;
	private boolean						fIsInLatitude			= false;
	private boolean						fIsInLongitude			= false;
	private boolean						fIsInAltitude			= false;
	private boolean						fIsInDistance			= false;

	private boolean						fIsInHeartRate			= false;
	private boolean						fIsInHeartRateValue		= false;

	private ArrayList<TimeData>			fTimeDataList;
	private TimeData					fTimeData;
	private TourbookDevice				fDeviceDataReader;

	private String						fImportFileName;

	private HashMap<String, TourData>	fTourDataMap;

	private int							fLapCounter;
	private boolean						fSetLapMarker;

	private boolean						fIsImported;

	{
		iso.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	public GarminSAXHandler(TourbookDevice deviceDataReader, String importFileName,
			DeviceData deviceData, HashMap<String, TourData> tourDataMap) {

		fDeviceDataReader = deviceDataReader;
		fImportFileName = importFileName;
		fTourDataMap = tourDataMap;
	}

	@Override
	public void characters(char[] chars, int startIndex, int length) throws SAXException {

		if (fIsInTime
				|| fIsInLatitude
				|| fIsInLongitude
				|| fIsInDistance
				|| fIsInAltitude
				|| fIsInHeartRateValue) {

			String dataString = new String(chars, startIndex, length).trim();

			try {

				if (fIsInTime) {

					fTimeData.absoluteTime = iso.parse(dataString).getTime();

				} else if (fIsInLatitude) {

					fTimeData.latitude = getDoubleValue(dataString);

				} else if (fIsInLongitude) {

					fTimeData.longitude = getDoubleValue(dataString);

				} else if (fIsInDistance) {

					fTimeData.absoluteDistance = getFloatIntValue(dataString);

				} else if (fIsInAltitude) {

					fTimeData.absoluteAltitude = getFloatShortValue(dataString);

				} else if (fVersion == 1 && fIsInHeartRate) {

					final short pulse = getShortValue(dataString);
					fTimeData.pulse = pulse == Integer.MIN_VALUE ? 0 : pulse;

				} else if (fVersion == 2 && fIsInHeartRateValue) {

					final short pulse = getShortValue(dataString);
					fTimeData.pulse = pulse == Integer.MIN_VALUE ? 0 : pulse;
				}

			} catch (final NumberFormatException e) {
				e.printStackTrace();
			} catch (final ParseException e) {
				e.printStackTrace();
			}
		}
	}

	private void computeAltitudeUpDown(TourData tourData) {

		final int[] timeSerie = tourData.timeSerie;
		final int[] altitudeSerie = tourData.altitudeSerie;

		final int serieLength = timeSerie.length;

		if (serieLength == 0) {
			return;
		}

		int lastTime = 0;
		int currentAltitude = altitudeSerie[0];
		int lastAltitude = currentAltitude;

		int altitudeUp = 0;
		int altitudeDown = 0;

		int minTimeDiff = 10;

		for (int timeIndex = 0; timeIndex < serieLength; timeIndex++) {

			final int currentTime = timeSerie[timeIndex];

			int timeDiff = currentTime - lastTime;

			currentAltitude = altitudeSerie[timeIndex];

			if (timeDiff >= minTimeDiff) {

				int altitudeDiff = lastAltitude - currentAltitude;

				if (altitudeDiff >= 0) {
					altitudeUp += altitudeDiff;
				} else {
					altitudeDown += altitudeDiff;
				}

				lastTime = currentTime;
				lastAltitude = currentAltitude;
			}
		}

		tourData.setTourAltUp(altitudeUp);
		tourData.setTourAltDown(-altitudeDown);
	}

	@Override
	public void endElement(String uri, String localName, String name) throws SAXException {

		if (fIsInHeartRateValue && name.equals(TAG_VALUE)) {
			fIsInHeartRateValue = false;
		} else if (name.equals(TAG_HEART_RATE_BPM)) {
			fIsInHeartRate = false;
		} else if (name.equals(TAG_ALTITUDE_METERS)) {
			fIsInAltitude = false;
		} else if (name.equals(TAG_DISTANCE_METERS)) {
			fIsInDistance = false;
		} else if (name.equals(TAG_LATITUDE_DEGREES)) {
			fIsInLatitude = false;
		} else if (name.equals(TAG_LONGITUDE_DEGREES)) {
			fIsInLongitude = false;
		} else if (name.equals(TAG_TIME)) {
			fIsInTime = false;

		} else if (name.equals(TAG_TRACKPOINT)) {

			/*
			 * keep trackpoint data
			 */
			fIsInTrackpoint = false;

			// ignore corrupt values
			if (fTimeData != null
					&& fTimeData.absoluteTime != Long.MIN_VALUE
					&& fTimeData.absoluteAltitude != Short.MIN_VALUE) {

				if (fSetLapMarker) {
					fSetLapMarker = false;

					fTimeData.marker = 1;
					fTimeData.markerLabel = Integer.toString(fLapCounter - 1);
				}

				fTimeDataList.add(fTimeData);
			}

		} else if (name.equals(TAG_LAP)) {

			fIsInLap = false;

		} else if (name.equals(TAG_ACTIVITY)) {

			/*
			 * version 2: activity and tour ends
			 */

			fIsInActivity = false;

			setTourData();

		} else if (name.equals(TAG_COURSE)) {

			/*
			 * version 1: course and tour ends
			 */

			fIsInCourse = false;

			setTourData();
		}
	}

	private int getFloatIntValue(String textValue) {

		try {
			if (textValue != null) {
				return (int) Float.parseFloat(textValue);
			} else {
				return Integer.MIN_VALUE;
			}

		} catch (NumberFormatException e) {
			return Integer.MIN_VALUE;
		}
	}

	private short getFloatShortValue(String textValue) {

		try {
			if (textValue != null) {
				return (short) Float.parseFloat(textValue);
			} else {
				return Short.MIN_VALUE;
			}

		} catch (NumberFormatException e) {
			return Short.MIN_VALUE;
		}
	}

	private double getDoubleValue(String textValue) {

		try {
			if (textValue != null) {
				return Double.parseDouble(textValue);
			} else {
				return Double.MIN_VALUE;
			}

		} catch (NumberFormatException e) {
			return Double.MIN_VALUE;
		}
	}

	private short getShortValue(String textValue) {

		try {
			if (textValue != null) {
				return Short.parseShort(textValue);
			} else {
				return Short.MIN_VALUE;
			}
		} catch (NumberFormatException e) {
			return Short.MIN_VALUE;
		}
	}

	/**
	 * @return Returns <code>true</code> when a tour was imported
	 */
	public boolean isImported() {
		return fIsImported;
	}

	private void setTourData() {

		if (fTimeDataList == null || fTimeDataList.size() == 0) {
			return;
		}

		/*
		 * sort timedata by time, because there can be in a chaotic order of the track points
		 */
//		Collections.sort(timeDataList, new Comparator<TimeData>() {
//			public int compare(TimeData td1, TimeData td2) {
//				return (int) (td1.absoluteDistance - td2.absoluteDistance);
//			}
//		});
//		Collections.sort(timeDataList, new Comparator<TimeData>() {
//			public int compare(TimeData td1, TimeData td2) {
//				return (int) (td1.absoluteTime - td2.absoluteTime);
//			}
//		});
		long tourDateTime = 0;

		long prevTime = -1;
		int prevDistance = 0;
		short prevAltitude = 0;

		long currentTime = -1;
		int currentDistance = 0;
		short currentAltitude = 0;

		TimeData prevTimeData = null;

//		computeDistance();

		/*
		 * convert absolute time into time difference with the previous time (this is the way how
		 * the data has been saved in the database)
		 */
		for (TimeData currentTimeData : fTimeDataList) {

			currentTime = currentTimeData.absoluteTime;
			currentDistance = currentTimeData.absoluteDistance;
			currentAltitude = currentTimeData.absoluteAltitude;

			if (prevTime == -1) {

				/*
				 * first data point
				 */

				// set start time for the tour;
				tourDateTime = currentTime;

			} else {

				prevTimeData.time = (short) ((currentTime - prevTime) / 1000);
				prevTimeData.altitude = (short) (currentAltitude - prevAltitude);

				prevTimeData.distance = currentDistance - prevDistance;

				System.out.println(prevTimeData.distance);

				prevAltitude = currentAltitude;
			}

			prevTime = currentTime;
			prevDistance = currentDistance;

			prevTimeData = currentTimeData;
		}

		// set data for the last TimeData
		if (prevTimeData != null) {
			prevTimeData.time = (short) ((currentTime - prevTime) / 1000);
			prevTimeData.distance = currentDistance - prevDistance;
		}

		// create data object for each tour
		final TourData tourData = new TourData();

		/*
		 * set tour date/time
		 */
		fCalendar.setTimeInMillis(tourDateTime);
		tourData.setStartMinute((short) fCalendar.get(Calendar.MINUTE));
		tourData.setStartHour((short) fCalendar.get(Calendar.HOUR_OF_DAY));
		tourData.setStartDay((short) fCalendar.get(Calendar.DAY_OF_MONTH));
		tourData.setStartMonth((short) (fCalendar.get(Calendar.MONTH) + 1));
		tourData.setStartYear((short) fCalendar.get(Calendar.YEAR));
		tourData.setStartWeek((short) fCalendar.get(Calendar.WEEK_OF_YEAR));

		tourData.setDeviceTimeInterval((short) -1);

		tourData.importRawDataFile = fImportFileName;

		tourData.createTimeSeries(fTimeDataList, true);

		computeAltitudeUpDown(tourData);

		// after all data are added, the tour id can be created
		final int[] distanceSerie = tourData.getMetricDistanceSerie();
		String uniqueKey;
		if (distanceSerie == null) {
			uniqueKey = "42984";
		} else {
			uniqueKey = Integer.toString(distanceSerie[distanceSerie.length - 1]);
		}
		tourData.createTourId(uniqueKey);

		// check if the tour is already imported
		final String tourId = tourData.getTourId().toString();
		if (fTourDataMap.containsKey(tourId) == false) {

			tourData.computeTourDrivingTime();
			tourData.computeAvgFields();

			tourData.setDeviceId(fDeviceDataReader.deviceId);
			tourData.setDeviceName(fDeviceDataReader.visibleName);

			// add new tour to other tours
			fTourDataMap.put(tourId, tourData);
		}

		fIsImported = true;
	}

	/**
	 */
	private void computeDistance3() {

		TimeData td1 = null;
		float totalDistance = 0;

		for (TimeData td2 : fTimeDataList) {

			if (td1 == null) {
				// set initial data
				td1 = td2;
				continue;
			}

			final double lat1 = td1.latitude;
			final double lat2 = td2.latitude;
			final double lon1 = td1.longitude;
			final double lon2 = td2.longitude;

			double dLat = (lat2 - lat1) / DEGREE_TO_RAD;
			double dLon = (lon2 - lon1) / DEGREE_TO_RAD;

			double a = Math.sin(dLat / 2)
					* Math.sin(dLat / 2)
					+ Math.cos(lat1 / DEGREE_TO_RAD)
					* Math.cos(lat2 / DEGREE_TO_RAD)
					* Math.sin(dLon / 2)
					* Math.sin(dLon / 2);

			double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)) / Math.PI;

			td1.absoluteDistance = (int) (totalDistance += 6378.8f * c);

			td1 = td2;
		}
	}

	/**
	 * Polar Coordinate Flat-Earth Formula
	 * 
	 * <pre>
	 * dlon = lon2 - lon1
	 * dlat = lat2 - lat1
	 * a = (sin(dlat/2))&circ;2 + cos(lat1) * cos(lat2) * (sin(dlon/2))&circ;2
	 * c = 2 * arcsin(min(1,sqrt(a)))
	 * d = R * c
	 * 
	 * with R = 6,378.8 km or 3,963.0 mi
	 * </pre>
	 * 
	 * @param td2
	 * @param td1
	 */
	private void computeDistance() {

		TimeData td1 = null;
		float totalDistance = 0;

		for (TimeData td2 : fTimeDataList) {

			if (td1 == null) {
				// set initial data
				td1 = td2;
				continue;
			}

			final double lat1 = td1.latitude / DEGREE_TO_RAD;
			final double lat2 = td2.latitude / DEGREE_TO_RAD;
			final double lon1 = td1.longitude / DEGREE_TO_RAD;
			final double lon2 = td2.longitude / DEGREE_TO_RAD;

			final double dlon = lon2 - lon1;
			final double dlat = lat2 - lat1;

			final double sinDlat = Math.sin(dlat / 2);
			final double sinDlon = Math.sin(dlon / 2);

			final double a = (sinDlat * sinDlat)
					+ Math.cos(lat1)
					* Math.cos(lat2)
					* (sinDlon * sinDlon);

			final double c = 2 * Math.asin(Math.min(1, Math.sqrt(a))) / Math.PI;

			td1.absoluteDistance = (int) (totalDistance += 6378.8f * c);

			td1 = td2;
		}
	}

	/**
	 * Polar Coordinate Flat-Earth Formula
	 * 
	 * <pre>
	 * a = pi/2 - lat1
	 * b = pi/2 - lat2
	 * c = sqrt( a&circ;2 + b&circ;2 - 2 * a * b * cos(lon2 - lon1) )
	 * d = R * c 
	 * 
	 * with R = 6,378.8 km or 3,963.0 mi
	 * </pre>
	 * 
	 * @param td2
	 * @param td1
	 */
	private void computeDistance1() {

		TimeData td1 = null;
		float totalDistance = 0;

		for (TimeData td2 : fTimeDataList) {

			if (td1 == null) {
				// set initial data
				td1 = td2;
				continue;
			}

			final double lat1 = td1.latitude;
			final double lat2 = td2.latitude;
			final double lon1 = td1.longitude;
			final double lon2 = td2.longitude;

			final double a = MATH_PI2 - lat1;
			final double b = MATH_PI2 - lat2;

			double c = Math.sqrt(a * a + b * b - 2 * a * b * Math.cos(lon2 - lon1));

			td1.absoluteDistance = (int) (totalDistance += 6378.8f * c);

			td1 = td2;
		}
	}

	@Override
	public void startElement(String uri, String localName, String name, Attributes attributes)
			throws SAXException {

		if (fVersion > 0) {

			if (fVersion == 1) {

				if (fIsInCourse) {

					if (fIsInTrackpoint) {

						if (name.equals(TAG_HEART_RATE_BPM)) {
							fIsInHeartRate = true;
						} else if (name.equals(TAG_ALTITUDE_METERS)) {
							fIsInAltitude = true;
						} else if (name.equals(TAG_DISTANCE_METERS)) {
							fIsInDistance = true;
						} else if (name.equals(TAG_TIME)) {
							fIsInTime = true;
						} else if (name.equals(TAG_LATITUDE_DEGREES)) {
							fIsInLatitude = true;
						} else if (name.equals(TAG_LONGITUDE_DEGREES)) {
							fIsInLongitude = true;
						} else if (fIsInHeartRate && name.equals(TAG_VALUE)) {
							fIsInHeartRateValue = true;
						}

					} else if (name.equals(TAG_TRACKPOINT)) {

						fIsInTrackpoint = true;

						// create new time item
						fTimeData = new TimeData();
					}

					if (fIsInLap) {

					} else if (name.equals(TAG_LAP)) {

						fIsInLap = true;

						fLapCounter++;
						if (fLapCounter > 1) {
							fSetLapMarker = true;
						}
					}

				} else if (name.equals(TAG_COURSE)) {

					/*
					 * a new activity starts
					 */

					fIsInCourse = true;

					fLapCounter = 0;
					fSetLapMarker = false;

					fTimeDataList = new ArrayList<TimeData>();
				}

			} else if (fVersion == 2) {

				if (fIsInActivity) {

					if (fIsInLap) {

						if (fIsInTrackpoint) {

							if (name.equals(TAG_HEART_RATE_BPM)) {
								fIsInHeartRate = true;
							} else if (name.equals(TAG_ALTITUDE_METERS)) {
								fIsInAltitude = true;
							} else if (name.equals(TAG_DISTANCE_METERS)) {
								fIsInDistance = true;
							} else if (name.equals(TAG_TIME)) {
								fIsInTime = true;
							} else if (name.equals(TAG_LATITUDE_DEGREES)) {
								fIsInLatitude = true;
							} else if (name.equals(TAG_LONGITUDE_DEGREES)) {
								fIsInLongitude = true;
							} else if (fIsInHeartRate && name.equals(TAG_VALUE)) {
								fIsInHeartRateValue = true;
							}

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
					}

				} else if (name.equals(TAG_ACTIVITY)) {

					/*
					 * a new activity starts
					 */

					fIsInActivity = true;

					fLapCounter = 0;
					fSetLapMarker = false;

					fTimeDataList = new ArrayList<TimeData>();
				}
			}

		} else if (name.equals(TAG_DATABASE)) {

			/*
			 * get version of the xml file
			 */
			for (int attrIndex = 0; attrIndex < attributes.getLength(); attrIndex++) {

				final String value = attributes.getValue(attrIndex);

				if (value.equalsIgnoreCase("http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v1")) {
					fVersion = 1;
					return;
				} else if (value.equalsIgnoreCase("http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2")) {
					fVersion = 2;
					return;
				}
			}
		}
	}
}
