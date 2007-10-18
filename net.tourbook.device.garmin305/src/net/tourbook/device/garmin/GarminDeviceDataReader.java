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

	static TimeZone			utc			= TimeZone.getTimeZone("GMT");

	static DateFormat		iso			= new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	{
		iso.setTimeZone(utc);
	}

	private final Calendar	fCalendar	= GregorianCalendar.getInstance();

	// plugin constructor
	public GarminDeviceDataReader() {}

	@Override
	public boolean checkStartSequence(int byteIndex, int newByte) {
		return true;
	}

	public String getDeviceModeName(int profileId) {
		return "";
	}

	private Document getDOMDocument(String fileName) {

		//get the factory
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

		try {

			//Using factory get an instance of document builder
			DocumentBuilder db = dbf.newDocumentBuilder();

			//parse using builder to get DOM representation of the XML file
			return db.parse("file:" + fileName);

		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (SAXException se) {
			se.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

		return null;
	}

	private float getFloatValue(Element ele, String tagName) {
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
	private int getIntValue(Element ele, String parentTagName, String tagName) {
		//in production application you would catch the exception
		final String textValue = getTextValue(ele, parentTagName, tagName);
		if (textValue != null) {
			return Integer.parseInt(textValue);
		} else {
			return Integer.MIN_VALUE;
		}
	}

	@Override
	public SerialParameters getPortParameters(String portName) {

		SerialParameters portParameters = new SerialParameters(portName,
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
	private String getTextValue(Element ele, String tagName) {

		String textValue = null;
		NodeList nl = ele.getElementsByTagName(tagName);

		if (nl != null && nl.getLength() > 0) {
			Element el = (Element) nl.item(0);
			textValue = el.getFirstChild().getNodeValue();
		}

		return textValue;
	}

	/**
	 * I take a xml element and the tag name, look for the tag and get the text content i.e for
	 * <employee><name>John</name></employee> xml snippet if
	 * the Element points to employee node and tagName is 'name' I will return John
	 */
	private String getTextValue(Element ele, String parentTagName, String tagName) {

		String textVal = null;

		NodeList parentNL = ele.getElementsByTagName(parentTagName);

		if (parentNL != null && parentNL.getLength() > 0) {

			final Element parentElement = (Element) parentNL.item(0);

			textVal = getTextValue(parentElement, tagName);
		}

		return textVal;
	}

	/**
	 * @param docElement
	 * @param importFileName
	 * @param tourDataMap
	 * @return Returns <code>true</code> when the tours are imported
	 */
	private boolean getToursV1(	Element docElement,
								String importFileName,
								HashMap<String, TourData> tourDataMap) {

		NodeList courseList = docElement.getElementsByTagName("Course");
		if (courseList != null && courseList.getLength() > 0) {

			for (int courseIndex = 0; courseIndex < courseList.getLength(); courseIndex++) {

				Element course = (Element) courseList.item(courseIndex);

				// create data object for each tour
				TourData tourData = new TourData();

				// create a list which contains all time slices 
				ArrayList<TimeData> timeDataList = new ArrayList<TimeData>();
				TimeData timeData;

				int prevAltitude = 0;
				int prevDistance = 0;
				long prevTime = -1;
				long tourTime = -1;

				long tourDateTime = 0;

				NodeList trackList = course.getElementsByTagName("Track");
				if (trackList != null && trackList.getLength() > 0) {

					for (int trackIndex = 0; trackIndex < trackList.getLength(); trackIndex++) {

						Element track = (Element) trackList.item(trackIndex);

						NodeList tpList = track.getElementsByTagName("Trackpoint");
						if (tpList != null && tpList.getLength() > 0) {

							for (int tpIndex = 0; tpIndex < tpList.getLength(); tpIndex++) {

								Element tp = (Element) tpList.item(tpIndex);

								timeData = new TimeData();

								final int altitude = (int) getFloatValue(tp, "AltitudeMeters");
								final int distance = (int) getFloatValue(tp, "DistanceMeters");
								final int pulse = getIntValue(tp, "HeartRateBpm", "Value");

								// ignore incomplete values
								if (altitude != Integer.MIN_VALUE && distance != Integer.MIN_VALUE) {

									try {
										String xmlTime = getTextValue(tp, "Time");
										final Date dtValue = iso.parse(xmlTime);
										tourTime = dtValue.getTime();

										if (prevTime == -1) {
											// set initial value;
											prevTime = tourTime;
											tourDateTime = tourTime;
										}

										timeData.time = (short) ((tourTime - prevTime) / 1000);

										prevTime = tourTime;

									} catch (ParseException e) {
										e.printStackTrace();
									}

									timeData.altitude = (short) (altitude - prevAltitude);
									timeData.distance = distance - prevDistance;
									timeData.pulse = (short) (pulse == Integer.MIN_VALUE
											? 0
											: pulse);

									prevAltitude = altitude;
									prevDistance = distance;

									timeDataList.add(timeData);
								}
							}
						}
					}
				}

				setTourData(importFileName, tourDataMap, tourData, timeDataList, tourDateTime);
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
	private boolean getToursV2(	Element docElement,
								String importFileName,
								HashMap<String, TourData> tourDataMap) {

		NodeList activityList = docElement.getElementsByTagName("Activity");

		if (activityList == null && activityList.getLength() == 0) {
			return false;
		}

		for (int activityIndex = 0; activityIndex < activityList.getLength(); activityIndex++) {

			Element activity = (Element) activityList.item(activityIndex);

			NodeList lapList = activity.getElementsByTagName("Lap");

			// create data object for each tour
			TourData tourData = new TourData();

			// create a list which contains all time slices 
			ArrayList<TimeData> timeDataList = new ArrayList<TimeData>();
			TimeData timeData;

			int prevAltitude = 0;
			int prevDistance = 0;
			long prevTime = -1;
			long tourTime = -1;

			long tourDateTime = 0;

			if (lapList != null && lapList.getLength() > 0) {
				for (int lapIndex = 0; lapIndex < lapList.getLength(); lapIndex++) {

					Element lap = (Element) lapList.item(lapIndex);
					NodeList trackList = lap.getElementsByTagName("Track");

					if (trackList != null && trackList.getLength() > 0) {
						for (int trackIndex = 0; trackIndex < trackList.getLength(); trackIndex++) {

							Element track = (Element) trackList.item(trackIndex);

							NodeList tpList = track.getElementsByTagName("Trackpoint");

							if (tpList != null && tpList.getLength() > 0) {
								for (int tpIndex = 0; tpIndex < tpList.getLength(); tpIndex++) {

									Element tp = (Element) tpList.item(tpIndex);

									timeData = new TimeData();

									final int altitude = (int) getFloatValue(tp, "AltitudeMeters");
									final int distance = (int) getFloatValue(tp, "DistanceMeters");
									final int pulse = getIntValue(tp, "HeartRateBpm", "Value");

									// ignore incomplete values
									if (altitude != Integer.MIN_VALUE
											&& distance != Integer.MIN_VALUE
											&& pulse != Integer.MIN_VALUE) {

										try {
											String xmlTime = getTextValue(tp, "Time");
											final Date dtValue = iso.parse(xmlTime);
											tourTime = dtValue.getTime();

											if (prevTime == -1) {
												// set start time for the tour;
												prevTime = tourTime;
												tourDateTime = tourTime;
											}

											timeData.time = (short) ((tourTime - prevTime) / 1000);

											prevTime = tourTime;

										} catch (ParseException e) {
											e.printStackTrace();
										}

										timeData.altitude = (short) (altitude - prevAltitude);
										timeData.distance = distance - prevDistance;
										timeData.pulse = (short) pulse;

										prevAltitude = altitude;
										prevDistance = distance;

										timeDataList.add(timeData);
									}
								}
							}
						}
					}
				}
			}

			setTourData(importFileName, tourDataMap, tourData, timeDataList, tourDateTime);
		}

		return true;
	}

	private int getVersion(Element docEle) {

		NamedNodeMap docAttributes = docEle.getAttributes();
		if (docAttributes == null) {
			return -1;
		}

		for (int attrIndex = 0; attrIndex < docAttributes.getLength(); attrIndex++) {
			Node attribute = docAttributes.item(attrIndex);

			if (attribute.getNodeName().equalsIgnoreCase("xmlns")) {
				String nodeValue = attribute.getNodeValue();
				if (nodeValue.equalsIgnoreCase("http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v1")) {
					return 1;
				} else if (nodeValue.equalsIgnoreCase("http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2")) {
					return 2;
				}
			}
		}

		return -1;
	}

	private void parseDocument(	Document domDocument,
								String importFileName,
								HashMap<String, TourData> tourDataMap) {

		//get the root element
		Element docElement = domDocument.getDocumentElement();

		int version = getVersion(docElement);
		if (version == -1) {
			return;
		}

		switch (version) {
		case 1:
			getToursV1(docElement, importFileName, tourDataMap);
			break;

		case 2:
			getToursV2(docElement, importFileName, tourDataMap);
			break;

		default:
			break;
		}

	}

	public boolean processDeviceData(	String importFileName,
										DeviceData deviceData,
										HashMap<String, TourData> tourDataMap) {

		final Document domDocument = getDOMDocument(importFileName);
		if (domDocument == null) {
			return false;
		}

		parseDocument(domDocument, importFileName, tourDataMap);

		return true;
	}

	private void setTourData(	String importFileName,
								HashMap<String, TourData> tourDataMap,
								TourData tourData,
								ArrayList<TimeData> timeDataList,
								long tourDateTime) {

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

		short timeInterval = 7;
		tourData.setDeviceTimeInterval(timeInterval);

		tourData.setTourAltUp(10);
		tourData.setTourAltDown(30);

		tourData.importRawDataFile = importFileName;

		tourData.createTimeSeries(timeDataList, true);

		// after all data are added, the tour id can be created
		int[] distance = tourData.distanceSerie;
		String uniqueKey;
		if (distance == null) {
			uniqueKey = "42984";
		} else {
			uniqueKey = Integer.toString(distance[distance.length - 1]);
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

	public boolean validateRawData(String fileName) {
		return true;
	}
}
