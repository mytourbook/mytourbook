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
import net.tourbook.device.DeviceReaderTools;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.TourbookDevice;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class GPX_SAX_Handler extends DefaultHandler {

	private static final String				NAME_SPACE_GPX_1_0		= "http://www.topografix.com/GPX/1/0";							//$NON-NLS-1$
	private static final String				NAME_SPACE_GPX_1_1		= "http://www.topografix.com/GPX/1/1";							//$NON-NLS-1$

	// namespace for extensions used by Garmin
	private static final String				NAME_SPACE_TPEXT		= "http://www.garmin.com/xmlschemas/TrackPointExtension/v1";	//$NON-NLS-1$

	private static final int				GPX_VERSION_1_0			= 10;
	private static final int				GPX_VERSION_1_1			= 11;

	private static final String				TAG_GPX					= "gpx";														//$NON-NLS-1$

	private static final String				TAG_TRK					= "trk";														//$NON-NLS-1$
	private static final String				TAG_TRKPT				= "trkpt";														//$NON-NLS-1$
//	private static final String				TAG_RTE					= "rte";												//$NON-NLS-1$
//	private static final String				TAG_RTEPT				= "rtept";												//$NON-NLS-1$

	private static final String				TAG_TIME				= "time";														//$NON-NLS-1$
	private static final String				TAG_ELE					= "ele";														//$NON-NLS-1$

	// Extension element for heart rate
	private static final String				TAG_EXT_HR				= "gpxtpx:hr";													//$NON-NLS-1$
	// Extension element for temperature
	private static final String				TAG_EXT_TEMP			= "gpxtpx:atemp";												//$NON-NLS-1$

	private static final String				ATTR_LATITUDE			= "lat";														//$NON-NLS-1$
	private static final String				ATTR_LONGITUDE			= "lon";														//$NON-NLS-1$

	private static final Calendar			fCalendar				= GregorianCalendar.getInstance();

	private static final SimpleDateFormat	GPX_TIME_FORMAT			= new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");			//$NON-NLS-1$
	private static final SimpleDateFormat	GPX_TIME_FORMAT_SSSZ	= new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");		//$NON-NLS-1$
	private static final SimpleDateFormat	GPX_TIME_FORMAT_RFC822	= new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");				//$NON-NLS-1$

	private int								fGpxVersion				= -1;
	private boolean							fInTrk					= false;
	private boolean							fInTrkPt				= false;
	//	private boolean							fInWpt					= false;
	//	private boolean							fInTrkSeg				= false;

	private boolean							fIsInTime				= false;
	private boolean							fIsInEle				= false;
	private boolean							fIsInName				= false;
	private boolean							fIsInHr					= false;
	private boolean							fIsInTemp				= false;

	private ArrayList<TimeData>				fTimeDataList;

	private TimeData						fTimeData;
	private TimeData						fPrevTimeData;

	private TourbookDevice					fDeviceDataReader;
	private String							fImportFilePath;
	private HashMap<Long, TourData>			fTourDataMap;
	private int								fLapCounter;

	private boolean							fIsSetLapMarker			= false;
	private boolean							fSetLapStartTime		= false;

	private ArrayList<Long>					fLapStart;

	private boolean							fIsImported;

	private long							fCurrentTime;

	private StringBuilder					fCharacters				= new StringBuilder();
	private float							fAbsoluteDistance;

	private boolean							fIsError				= false;

	{
		GPX_TIME_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$
		GPX_TIME_FORMAT_SSSZ.setTimeZone(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$
		GPX_TIME_FORMAT_RFC822.setTimeZone(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$
	}

	public GPX_SAX_Handler(	final TourbookDevice deviceDataReader,
							final String importFileName,
							final DeviceData deviceData,
							final HashMap<Long, TourData> tourDataMap) {

		fDeviceDataReader = deviceDataReader;
		fImportFilePath = importFileName;
		fTourDataMap = tourDataMap;
	}

	@Override
	public void characters(final char[] chars, final int startIndex, final int length) throws SAXException {

		if (fIsInTime || fIsInEle || fIsInName || fIsInHr || fIsInTemp) {
			fCharacters.append(chars, startIndex, length);
		}
	}

	private void computeAltitudeUpDown(final TourData tourData) {

		final int[] timeSerie = tourData.timeSerie;
		final int[] altitudeSerie = tourData.altitudeSerie;

		if (timeSerie == null || altitudeSerie == null) {
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

		final int minTimeDiff = 10;

		for (int timeIndex = 0; timeIndex < serieLength; timeIndex++) {

			final int currentTime = timeSerie[timeIndex];

			final int timeDiff = currentTime - lastTime;

			currentAltitude = altitudeSerie[timeIndex];

			if (timeDiff >= minTimeDiff) {

				final int altitudeDiff = currentAltitude - lastAltitude;

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
	public void endElement(final String uri, final String localName, final String name) throws SAXException {

		if (fIsError) {
			return;
		}

		try {

			if (fInTrkPt) {

				final String timeString = fCharacters.toString();

				if (name.equals(TAG_ELE)) {

					fIsInEle = false;

					fTimeData.absoluteAltitude = getFloatValue(timeString);

				} else if (name.equals(TAG_TIME)) {

					fIsInTime = false;

					try {
						fTimeData.absoluteTime = fCurrentTime = GPX_TIME_FORMAT.parse(timeString).getTime();
					} catch (final ParseException e) {
						try {
							fTimeData.absoluteTime = fCurrentTime = GPX_TIME_FORMAT_SSSZ.parse(timeString).getTime();
						} catch (final ParseException e2) {
							try {
								fTimeData.absoluteTime = fCurrentTime = GPX_TIME_FORMAT_RFC822.parse(timeString)
										.getTime();
							} catch (final ParseException e3) {

								fIsError = true;

								final String message = e3.getMessage();
								MessageDialog.openError(Display.getCurrent().getActiveShell(), "Error", message); //$NON-NLS-1$
								System.err.println(message + " in " + fImportFilePath); //$NON-NLS-1$
							}
						}
					}
				} else if (name.equals(TAG_EXT_HR)) {
					fIsInHr = false;
					fTimeData.pulse = getIntValue(timeString);
				} else if (name.equals(TAG_EXT_TEMP)) {
					fIsInTemp = false;
					fTimeData.temperature = Math.round(getFloatValue(timeString));
				}
			}

			if (name.equals(TAG_TRKPT) /* || name.equals(TAG_RTEPT) */) {

				/*
				 * trackpoint ends
				 */

				fInTrkPt = false;

				finalizeTrackpoint();

			} else if (name.equals(TAG_TRK) /* || name.equals(TAG_RTE) */) {

				/*
				 * track ends
				 */

				fInTrk = false;

				if (fDeviceDataReader.isMergeTracks == false) {
					setTourData();
				}

			} else if (name.equals(TAG_GPX)) {

				/*
				 * file end
				 */

				if (fDeviceDataReader.isMergeTracks) {
					setTourData();
				}
			}

		} catch (final NumberFormatException e) {
			e.printStackTrace();
		}

	}

	private void finalizeTrackpoint() {

		if (fTimeData == null) {
			return;
		}

		if (fIsSetLapMarker) {
			fIsSetLapMarker = false;

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
				fTimeData.absoluteDistance = fAbsoluteDistance += DeviceReaderTools.computeDistance(fPrevTimeData.latitude,
						fPrevTimeData.longitude,
						fTimeData.latitude,
						fTimeData.longitude);
			}
		}

		// set virtual time if time is not available
		if (fTimeData.absoluteTime == Long.MIN_VALUE) {

			fCalendar.set(2000, 0, 1, 0, 0, 0);
			fTimeData.absoluteTime = fCalendar.getTimeInMillis();
		}

		fPrevTimeData = fTimeData;
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

	private int getIntValue(final String textValue) {
		try {
			if (textValue != null) {
				return Integer.parseInt(textValue);
			} else {
				return Integer.MIN_VALUE;
			}

		} catch (final NumberFormatException e) {
			return Integer.MIN_VALUE;
		}
	}

//	private short getShortValue(String textValue) {
//
//		try {
//			if (textValue != null) {
//				return Short.parseShort(textValue);
//			} else {
//				return Short.MIN_VALUE;
//			}
//		} catch (NumberFormatException e) {
//			return Short.MIN_VALUE;
//		}
//	}

	private void initNewTrack() {
		fTimeDataList = new ArrayList<TimeData>();
		fAbsoluteDistance = 0;
		fPrevTimeData = null;
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
		tourData.importRawDataFile = fImportFilePath;
		tourData.setTourImportFilePath(fImportFilePath);

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

						if (fDeviceDataReader.isMergeTracks) {
							initNewTrack();
						}

						break;

					} else if (value.contains(NAME_SPACE_GPX_1_1)) {

						fGpxVersion = GPX_VERSION_1_1;

						if (fDeviceDataReader.isMergeTracks) {
							initNewTrack();
						}

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
						
					} else if (name.equals(TAG_EXT_HR)) {
						
						fIsInHr = true;
						fCharacters.delete(0, fCharacters.length());
						
					} else if (name.equals(TAG_EXT_TEMP)) {
						
						fIsInTemp = true;
						fCharacters.delete(0, fCharacters.length());
					}

				} else if (name.equals(TAG_TRKPT) /* || name.equals(TAG_RTEPT) */) {

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

			} else if (name.equals(TAG_TRK) /* || name.equals(TAG_RTE) */) {

				/*
				 * new track starts
				 */

				fInTrk = true;

//				fLapCounter = 0;
//				fSetLapMarker = false;
//				fLapStart = new ArrayList<Long>();

				if (fDeviceDataReader.isMergeTracks == false) {
					initNewTrack();
				}
			}
		}
	}
}
