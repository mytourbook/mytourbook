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
package net.tourbook.device.gpx;

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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class GPX_SAXHandler extends DefaultHandler {

	/**
	 * Earth radius is 6367 km.
	 */
	static final double						EARTH_RADIUS			= 6367d;

	// --- Mathematic constants ---
	private static final double				DEGRAD					= Math.PI / 180.0d;

	private static final String				NAME_SPACE_GPX_1_0		= "http://www.topografix.com/GPX/1/0";				//$NON-NLS-1$
	private static final String				NAME_SPACE_GPX_1_1		= "http://www.topografix.com/GPX/1/1";				//$NON-NLS-1$

	private static final int				GPX_VERSION_1_0			= 10;
	private static final int				GPX_VERSION_1_1			= 11;

	private static final String				TAG_GPX					= "gpx";											//$NON-NLS-1$

	private static final String				TAG_WPT					= "wpt";											//$NON-NLS-1$
	private static final String				TAG_TRK					= "trk";											//$NON-NLS-1$
//	private static final String				TAG_TRKSEG				= "trkseg";										//$NON-NLS-1$
	private static final String				TAG_TRKPT				= "trkpt";											//$NON-NLS-1$

	private static final String				TAG_TIME				= "time";											//$NON-NLS-1$
	private static final String				TAG_ELE					= "ele";											//$NON-NLS-1$

	private static final String				ATTR_LATITUDE			= "lat";
	private static final String				ATTR_LONGITUDE			= "lon";

	private int								fGpxVersion				= -1;

	private boolean							fInWpt					= false;
	private boolean							fInTrk					= false;
//	private boolean							fInTrkSeg				= false;
	private boolean							fInTrkPt				= false;

	private boolean							fIsInTime				= false;
	private boolean							fIsInEle				= false;
	private boolean							fIsInName				= false;

	private ArrayList<TimeData>				fTimeDataList;
	private TimeData						fTimeData;
	private TimeData						fPrevTimeData;
	private TourbookDevice					fDeviceDataReader;

	private String							fImportFileName;

	private HashMap<String, TourData>		fTourDataMap;

	private int								fLapCounter;
	private boolean							fSetLapMarker			= false;
	private boolean							fSetLapStartTime		= false;
	private ArrayList<Long>					fLapStart;

	private boolean							fIsImported;
	private long							fCurrentTime;

	private StringBuilder					fCharacters				= new StringBuilder();

	private int								fAbsoluteDistance;

	private static final Calendar			fCalendar				= GregorianCalendar.getInstance();

	private static final SimpleDateFormat	GPX_TIME_FORMAT			= new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); //$NON-NLS-1$
	private static final SimpleDateFormat	GPX_TIME_FORMAT_RFC822	= new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");	//$NON-NLS-1$

	{
		GPX_TIME_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT")); //$NON-NLS-1$
	}

	private boolean							fIsError				= false;

	public GPX_SAXHandler(TourbookDevice deviceDataReader, String importFileName, DeviceData deviceData,
			HashMap<String, TourData> tourDataMap) {

		fDeviceDataReader = deviceDataReader;
		fImportFileName = importFileName;
		fTourDataMap = tourDataMap;
	}

	@Override
	public void characters(char[] chars, int startIndex, int length) throws SAXException {

		if (fIsInTime || fIsInEle || fIsInName) {
			fCharacters.append(chars, startIndex, length);
		}
	}

	private void computeAltitudeUpDown(TourData tourData) {

		final int[] timeSerie = tourData.timeSerie;
		final int[] altitudeSerie = tourData.altitudeSerie;

		if (altitudeSerie == null) {
			return;
		}

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

		if (fIsError) {
			return;
		}

		try {

			if (fInTrkPt) {

				if (name.equals(TAG_ELE)) {

					fIsInEle = false;

					fTimeData.absoluteAltitude = getFloatValue(fCharacters.toString());

				} else if (name.equals(TAG_TIME)) {

					fIsInTime = false;

					try {
						fTimeData.absoluteTime = fCurrentTime = GPX_TIME_FORMAT.parse(fCharacters.toString()).getTime();
					} catch (ParseException e) {
						try {
							fTimeData.absoluteTime = fCurrentTime = GPX_TIME_FORMAT_RFC822.parse(fCharacters.toString())
									.getTime();
						} catch (ParseException e2) {
							MessageDialog.openError(Display.getCurrent().getActiveShell(), "Error", e.getMessage());
							e2.printStackTrace();
							fIsError = true;
						}
					}
				}
			}

			if (name.equals(TAG_TRKPT)) {

				/*
				 * complete trackpoint data
				 */

				fInTrkPt = false;

				finalizeTrackpoint();

			} else if (name.equals(TAG_TRK)) {

				/*
				 * track ends
				 */

				fInTrk = false;

				setTourData();

			} else if (name.equals(TAG_GPX)) {

				/*
				 * file end
				 */

//				setTourData();
			}

		} catch (final NumberFormatException e) {
			e.printStackTrace();
		}

	}

	private void finalizeTrackpoint() {

		if (fSetLapMarker) {
			fSetLapMarker = false;

			fTimeData.marker = 1;
			fTimeData.markerLabel = Integer.toString(fLapCounter - 1);
		}

		fTimeDataList.add(fTimeData);

		if (fSetLapStartTime) {
			fSetLapStartTime = false;
			fLapStart.add(fCurrentTime);
		}

		// calculate distance
		if (fPrevTimeData == null) {
			// first time data
			fTimeData.absoluteDistance = 0;
		} else {
			if (fTimeData.absoluteDistance == Float.MIN_VALUE) {
				fTimeData.absoluteDistance = fAbsoluteDistance += (int) getDistance();
			}
		}

		fPrevTimeData = fTimeData;
	}

	/**
	 * @return Return the distance in meters between two positions
	 */
	private double getDistance() {

		double lat1 = fPrevTimeData.latitude;
		double lon1 = fPrevTimeData.longitude;

		double lat2 = fTimeData.latitude;
		double lon2 = fTimeData.longitude;

		double a, c;

		// convert the degree values to radians before calculation
		lat1 = lat1 * DEGRAD;
		lon1 = lon1 * DEGRAD;
		lat2 = lat2 * DEGRAD;
		lon2 = lon2 * DEGRAD;

		double dlon = lon2 - lon1;
		double dlat = lat2 - lat1;

		a = Math.pow(Math.sin(dlat / 2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dlon / 2), 2);

		c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

		return (EARTH_RADIUS * c) * 1000;
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

	private float getFloatValue(String textValue) {

		try {
			if (textValue != null) {
				return Float.parseFloat(textValue);
			} else {
				return Float.MIN_VALUE;
			}

		} catch (NumberFormatException e) {
			return Float.MIN_VALUE;
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

		if (fIsError) {
			return false;
		}

		return fIsImported;
	}

	private void setTourData() {

		if (fTimeDataList == null || fTimeDataList.size() == 0) {
			return;
		}

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
		tourData.importRawDataFile = fImportFileName;

		tourData.createTimeSeries(fTimeDataList, true);
		computeAltitudeUpDown(tourData);

		// after all data are added, the tour id can be created
		final int[] distanceSerie = tourData.getMetricDistanceSerie();
		String uniqueKey;
		if (distanceSerie == null) {
			uniqueKey = "42984"; //$NON-NLS-1$
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

	@Override
	public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {

		if (fIsError) {
			return;
		}

		if (fGpxVersion < 0) {

			// gpx version is not set

			if (name.equals(TAG_GPX)) {

				/*
				 * get version of the xml file
				 */
				for (int attrIndex = 0; attrIndex < attributes.getLength(); attrIndex++) {

					final String value = attributes.getValue(attrIndex);

					if (value.contains(NAME_SPACE_GPX_1_0)) {

						fGpxVersion = GPX_VERSION_1_0;

//						fTimeDataList = new ArrayList<TimeData>();

						break;

					} else if (value.contains(NAME_SPACE_GPX_1_1)) {

						fGpxVersion = GPX_VERSION_1_1;

//						fTimeDataList = new ArrayList<TimeData>();

						break;
					}
				}
			}

		} else if (fGpxVersion == GPX_VERSION_1_0 || fGpxVersion == GPX_VERSION_1_1) {

			/*
			 * name space: http://www.topografix.com/GPX/1/0/gpx.xsd
			 */
			if (fInTrk) {

				if (fInTrkPt) {

					if (name.equals(TAG_ELE)) {

						fIsInEle = true;
						fCharacters.delete(0, fCharacters.length());

					} else if (name.equals(TAG_TIME)) {

						fIsInTime = true;
						fCharacters.delete(0, fCharacters.length());
					}

				} else if (name.equals(TAG_TRKPT)) {

					/*
					 * new trackpoing
					 */
					fInTrkPt = true;

					// create new time item
					fTimeData = new TimeData();

					// get attributes
					fTimeData.latitude = getDoubleValue(attributes.getValue(ATTR_LATITUDE));
					fTimeData.longitude = getDoubleValue(attributes.getValue(ATTR_LONGITUDE));
				}

			} else if (name.equals(TAG_TRK)) {

				/*
				 * new track starts
				 */

				fInTrk = true;

//				fLapCounter = 0;
//				fSetLapMarker = false;
//				fLapStart = new ArrayList<Long>();

				fTimeDataList = new ArrayList<TimeData>();
			}
		}
	}
}
