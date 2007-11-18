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

import gnu.io.SerialPort;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.SerialParameters;
import net.tourbook.importdata.TourbookDevice;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class GarminDeviceDataReader extends TourbookDevice {

	private static final String		TAG_ACTIVITY		= "Activity";										//$NON-NLS-1$
	private static final String		TAG_ALTITUDE_METERS	= "AltitudeMeters";								//$NON-NLS-1$
	private static final String		TAG_COURSE			= "Course";										//$NON-NLS-1$
	private static final String		TAG_DISTANCE_METERS	= "DistanceMeters";								//$NON-NLS-1$
	private static final String		TAG_HEART_RATE_BPM	= "HeartRateBpm";									//$NON-NLS-1$
	private static final String		TAG_LAP				= "Lap";											//$NON-NLS-1$
	private static final String		TAG_TIME			= "Time";											//$NON-NLS-1$
	private static final String		TAG_TRACK			= "Track";											//$NON-NLS-1$
	private static final String		TAG_TRACKPOINT		= "Trackpoint";									//$NON-NLS-1$
	private static final String		TAG_VALUE			= "Value";											//$NON-NLS-1$

	private static final Calendar	fCalendar			= GregorianCalendar.getInstance();

	private static final DateFormat	iso					= new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); //$NON-NLS-1$
	{
		iso.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	// plugin constructor
	public GarminDeviceDataReader() {}

	@Override
	public boolean checkStartSequence(final int byteIndex, final int newByte) {
		return true;
	}

	public String getDeviceModeName(final int profileId) {
		return ""; //$NON-NLS-1$
	}

	private float getFloatValue(final Element ele, final String tagName) {
		//in production application you would catch the exception
		final String textValue = getTextValue(ele, tagName);
		if (textValue != null) {
			return Float.parseFloat(textValue);
		} else {
			return Float.MIN_VALUE;
		}
	}

	public int getImportDataSize() {
		return 0x10000;
	}

	/**
	 * Calls getTextValue and returns a int value
	 */
	private int getIntValue(final Element ele, final String parentTagName, final String tagName) {
		//in production application you would catch the exception
		final String textValue = getTextValue(ele, parentTagName, tagName);
		if (textValue != null) {
			return Integer.parseInt(textValue);
		} else {
			return Integer.MIN_VALUE;
		}
	}

	@Override
	public SerialParameters getPortParameters(final String portName) {

		final SerialParameters portParameters = new SerialParameters(portName,
				4800,
				SerialPort.FLOWCONTROL_NONE,
				SerialPort.FLOWCONTROL_NONE,
				SerialPort.DATABITS_8,
				SerialPort.STOPBITS_1,
				SerialPort.PARITY_NONE);

		return portParameters;
	}

	@Override
	public int getStartSequenceSize() {
		return 0;
	}

	/**
	 * I take a xml element and the tag name, look for the tag and get the text content i.e for
	 * <employee><name>John</name></employee> xml snippet if
	 * the Element points to employee node and tagName is 'name' I will return John
	 */
	private String getTextValue(final Element ele, final String tagName) {

		String textValue = null;
		final NodeList nl = ele.getElementsByTagName(tagName);

		if (nl != null && nl.getLength() > 0) {
			final Element el = (Element) nl.item(0);
			textValue = el.getFirstChild().getNodeValue();
		}

		return textValue;
	}

	/**
	 * I take a xml element and the tag name, look for the tag and get the text content i.e for
	 * <employee><name>John</name></employee> xml snippet if
	 * the Element points to employee node and tagName is 'name' I will return John
	 */
	private String getTextValue(final Element ele, final String parentTagName, final String tagName) {

		String textVal = null;

		final NodeList parentNL = ele.getElementsByTagName(parentTagName);

		if (parentNL != null && parentNL.getLength() > 0) {

			final Element parentElement = (Element) parentNL.item(0);

			textVal = getTextValue(parentElement, tagName);
		}

		return textVal;
	}

	/**
	 */
	private void OLDcomputeAltitudeUpDown(TourData tourData) {

//		final int[] timeSerie = tourData.timeSerie;
//		final int[] altitudeSerie = tourData.altitudeSerie;
//
//		final int serieLength = timeSerie.length;
//
//		if (serieLength == 0) {
//			return;
//		}
//
//		int lastTime = 0;
//		int currentAltitude = altitudeSerie[0];
//		int lastAltitude = currentAltitude;
//
//		int altitudeUp = 0;
//		int altitudeDown = 0;
//
//		int minTimeInterval = 1;
//
//		for (int timeIndex = 0; timeIndex < serieLength; timeIndex++) {
//
//			final int currentTime = timeSerie[timeIndex];
//
//			int timeDiff = currentTime - lastTime;
//
//			currentAltitude = altitudeSerie[timeIndex];
//
//			if (timeDiff >= minTimeInterval) {
//
//				int altitudeDiff = lastAltitude - currentAltitude;
//
//				if (altitudeDiff < 0) {
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
//		tourData.setTourAltUp(altitudeUp);
//		tourData.setTourAltDown(-altitudeDown);
	}

	private Document OLDgetDOMDocument(final String fileName) {

		//get the factory
		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

		try {

			//Using factory get an instance of document builder
			final DocumentBuilder db = dbf.newDocumentBuilder();

			//parse using builder to get DOM representation of the XML file
			return db.parse("file:" + fileName); //$NON-NLS-1$

		} catch (final ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (final SAXException se) {
			se.printStackTrace();
		} catch (final IOException ioe) {
			ioe.printStackTrace();
		}

		return null;
	}

	/**
	 * @param docElement
	 * @param importFileName
	 * @param tourDataMap
	 * @return Returns <code>true</code> when the tours are imported
	 */
	private boolean OLDgetToursV1(	final Element docElement,
									final String importFileName,
									final HashMap<String, TourData> tourDataMap) {

		final NodeList courseList = docElement.getElementsByTagName(TAG_COURSE);
		if (courseList != null && courseList.getLength() > 0) {

			for (int courseIndex = 0; courseIndex < courseList.getLength(); courseIndex++) {

				final Element course = (Element) courseList.item(courseIndex);

				// create data object for each tour
				final TourData tourData = new TourData();

				// create a list which contains all time slices 
				final ArrayList<TimeData> timeDataList = new ArrayList<TimeData>();
				TimeData timeData;

				int prevAltitude = 0;
				int prevDistance = 0;
				long prevTime = -1;
				long tourTime = -1;

				long tourDateTime = 0;

				final NodeList trackList = course.getElementsByTagName(TAG_TRACK);

				if (trackList != null && trackList.getLength() > 0) {

					for (int trackIndex = 0; trackIndex < trackList.getLength(); trackIndex++) {

						final Element track = (Element) trackList.item(trackIndex);

						final NodeList tpList = track.getElementsByTagName(TAG_TRACKPOINT);
						if (tpList != null && tpList.getLength() > 0) {

							for (int tpIndex = 0; tpIndex < tpList.getLength(); tpIndex++) {

								final Element tp = (Element) tpList.item(tpIndex);

								timeData = new TimeData();

								final int absoluteAltitude = (int) getFloatValue(tp,
										TAG_ALTITUDE_METERS);
								final int distance = (int) getFloatValue(tp, TAG_DISTANCE_METERS);
								final int pulse = getIntValue(tp, TAG_HEART_RATE_BPM, TAG_VALUE);

								// ignore incomplete values
								if (absoluteAltitude != Integer.MIN_VALUE
										&& distance != Integer.MIN_VALUE) {

									try {
										final String xmlTime = getTextValue(tp, TAG_TIME);
										final Date dtValue = iso.parse(xmlTime);
										tourTime = dtValue.getTime();

										if (prevTime == -1) {
											// set initial value;
											prevTime = tourTime;
											tourDateTime = tourTime;
										}

										timeData.time = (short) ((tourTime - prevTime) / 1000);

										prevTime = tourTime;

									} catch (final ParseException e) {
										e.printStackTrace();
									}

									final int altitudeDiff = absoluteAltitude - prevAltitude;

									timeData.altitude = (short) (altitudeDiff);
									timeData.distance = distance - prevDistance;
									timeData.pulse = (short) pulse;

									prevAltitude = absoluteAltitude;
									prevDistance = distance;

									timeDataList.add(timeData);
								}
							}
						}
					}
				}

				OLDsetTourData(importFileName, tourDataMap, tourData, timeDataList, tourDateTime);
			}
		}

		return true;
	}

	/**
	 * @param docElement
	 * @param importFileName
	 * @param tourDataMap
	 * @return Returns <code>true</code> when the tours are imported
	 */
	private boolean OLDgetToursV2(	final Element docElement,
									final String importFileName,
									final HashMap<String, TourData> tourDataMap) {

		final NodeList activityList = docElement.getElementsByTagName(TAG_ACTIVITY);

		if (activityList == null && activityList.getLength() == 0) {
			return false;
		}

		for (int activityIndex = 0; activityIndex < activityList.getLength(); activityIndex++) {

			final Element activity = (Element) activityList.item(activityIndex);

			final NodeList lapList = activity.getElementsByTagName(TAG_LAP);

			// create data object for each tour
			final TourData tourData = new TourData();

			// create a list which contains all time slices 
			final ArrayList<TimeData> timeDataList = new ArrayList<TimeData>();
			TimeData timeData;

			int prevAltitude = 0;
			int prevDistance = 0;

			if (lapList != null && lapList.getLength() > 0) {
				for (int lapIndex = 0; lapIndex < lapList.getLength(); lapIndex++) {

					final Element lap = (Element) lapList.item(lapIndex);
					final NodeList trackList = lap.getElementsByTagName(TAG_TRACK);

					if (trackList != null && trackList.getLength() > 0) {
						for (int trackIndex = 0; trackIndex < trackList.getLength(); trackIndex++) {

							final Element track = (Element) trackList.item(trackIndex);

							final NodeList tpList = track.getElementsByTagName(TAG_TRACKPOINT);

							if (tpList != null && tpList.getLength() > 0) {
								for (int tpIndex = 0; tpIndex < tpList.getLength(); tpIndex++) {

									final Element tp = (Element) tpList.item(tpIndex);

									timeData = new TimeData();

									final int absoluteAltitude = (int) getFloatValue(tp,
											TAG_ALTITUDE_METERS);
									final int absoluteDistance = (int) getFloatValue(tp,
											TAG_DISTANCE_METERS);
									final int pulse = getIntValue(tp, TAG_HEART_RATE_BPM, TAG_VALUE);

									// ignore incomplete values
									if (absoluteAltitude != Integer.MIN_VALUE
											&& absoluteDistance != Integer.MIN_VALUE
											&& pulse != Integer.MIN_VALUE) {

										try {
											final String xmlTime = getTextValue(tp, TAG_TIME);
											final Date dtValue = iso.parse(xmlTime);
											timeData.absoluteTime = dtValue.getTime();

										} catch (final ParseException e) {
											e.printStackTrace();
											continue;
										}

										timeData.altitude = (short) ((absoluteAltitude - prevAltitude));

//										timeData.distance = absoluteDistance - prevDistance;
										timeData.absoluteDistance = absoluteDistance;

										timeData.pulse = (short) (pulse == Integer.MIN_VALUE
												? 0
												: pulse);

										prevAltitude = absoluteAltitude;
										prevDistance = absoluteDistance;

										timeDataList.add(timeData);
									}
								}
							}
						}
					}
				}
			}

			/*
			 * sort timedata by time, because there can be in a chaotic order of the track points
			 */
//			Collections.sort(timeDataList, new Comparator<TimeData>() {
//				public int compare(TimeData td1, TimeData td2) {
//					return (int) (td1.absoluteDistance - td2.absoluteDistance);
//				}
//			});
//			Collections.sort(timeDataList, new Comparator<TimeData>() {
//				public int compare(TimeData td1, TimeData td2) {
//					return (int) (td1.absoluteTime - td2.absoluteTime);
//				}
//			});
			long tourDateTime = 0;
			long prevTime = -1;

			long currentTime = -1;
			int currentDistance = 0;

			TimeData prevTimeData = null;

			/*
			 * convert absolute time into time difference with the previous time (this is the way
			 * how the data has been saved in the database)
			 */
			for (TimeData adjustTimeData : timeDataList) {

				currentTime = adjustTimeData.absoluteTime;
				currentDistance = adjustTimeData.absoluteDistance;

				if (prevTime == -1) {

					// set start time for the tour;
					tourDateTime = currentTime;

				} else {

					prevTimeData.time = (short) ((currentTime - prevTime) / 1000);
					prevTimeData.distance = currentDistance - prevDistance;
				}

				prevTime = currentTime;
				prevDistance = currentDistance;

				prevTimeData = adjustTimeData;
			}

			// set data for the last TimeData
			if (prevTimeData != null) {
				prevTimeData.time = (short) ((currentTime - prevTime) / 1000);
				prevTimeData.distance = currentDistance - prevDistance;
			}

			OLDsetTourData(importFileName, tourDataMap, tourData, timeDataList, tourDateTime);
		}

		return true;
	}

	private int OLDgetVersion(final Element docEle) {

		final NamedNodeMap docAttributes = docEle.getAttributes();
		if (docAttributes == null) {
			return -1;
		}

		for (int attrIndex = 0; attrIndex < docAttributes.getLength(); attrIndex++) {
			final Node attribute = docAttributes.item(attrIndex);

			if (attribute.getNodeName().equalsIgnoreCase("xmlns")) { //$NON-NLS-1$
				final String nodeValue = attribute.getNodeValue();
				if (nodeValue.equalsIgnoreCase("http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v1")) { //$NON-NLS-1$
					return 1;
				} else if (nodeValue.equalsIgnoreCase("http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2")) { //$NON-NLS-1$
					return 2;
				}
			}
		}

		return -1;
	}

	private void OLDparseDocument(	final Document domDocument,
									final String importFileName,
									final HashMap<String, TourData> tourDataMap) {

		//get the root element
		final Element docElement = domDocument.getDocumentElement();

		final int version = OLDgetVersion(docElement);
		if (version == -1) {
			return;
		}

		switch (version) {
		case 1:
			OLDgetToursV1(docElement, importFileName, tourDataMap);
			break;

		case 2:
			OLDgetToursV2(docElement, importFileName, tourDataMap);
			break;

		default:
			break;
		}

	}

	public boolean OLDprocessDeviceData(final String importFileName,
										final DeviceData deviceData,
										final HashMap<String, TourData> tourDataMap) {

		final Document domDocument = OLDgetDOMDocument(importFileName);
		if (domDocument == null) {
			return false;
		}

		OLDparseDocument(domDocument, importFileName, tourDataMap);

		return true;
	}

	private void OLDsetTourData(final String importFileName,
								final HashMap<String, TourData> tourDataMap,
								final TourData tourData,
								final ArrayList<TimeData> timeDataList,
								final long tourDateTime) {

		if (timeDataList.size() == 0) {
			return;
		}

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

		tourData.importRawDataFile = importFileName;

		tourData.createTimeSeries(timeDataList, true);

		OLDcomputeAltitudeUpDown(tourData);

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
		if (tourDataMap.containsKey(tourId) == false) {

			tourData.computeTourDrivingTime();
			tourData.computeAvgFields();

			tourData.setDeviceId(deviceId);
			tourData.setDeviceName(visibleName);

			// add new tour to other tours
			tourDataMap.put(tourId, tourData);
		}
	}

	public boolean processDeviceData(	final String importFileName,
										final DeviceData deviceData,
										final HashMap<String, TourData> tourDataMap) {

		GarminSAXHandler handler = new GarminSAXHandler(this,
				importFileName,
				deviceData,
				tourDataMap);

		try {

			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser parser = factory.newSAXParser();

			parser.parse("file:" + importFileName, handler);//$NON-NLS-1$

		} catch (Exception e) {
			System.err.println("Error parsing " + importFileName + ": " + e);
			e.printStackTrace();
			return false;
		}

		return handler.isImported();
	}

	public boolean validateRawData(final String fileName) {
		return true;
	}
}
