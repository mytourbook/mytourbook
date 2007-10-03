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
package net.tourbook.device.garmin305;

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
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Garmin305DeviceDataReader extends TourbookDevice {

	private Document		dom;

	private final Calendar	fCalendar	= GregorianCalendar.getInstance();

	// plugin constructor
	public Garmin305DeviceDataReader() {}

	@Override
	public boolean checkStartSequence(int byteIndex, int newByte) {
		return true;
	}

	public String getDeviceModeName(int profileId) {
		return "";
	}

	public int getImportDataSize() {
		return 0x10000;
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

	public boolean processDeviceData(	String importFileName,
										DeviceData deviceData,
										HashMap<String, TourData> tourDataMap) {

		if (parseXmlFile(importFileName) == false) {
			return false;
		}

		parseDocument(importFileName, tourDataMap);

		// set the import or transfer date
//		deviceData.transferYear = tourData.getStartYear();
//		deviceData.transferMonth = tourData.getStartMonth();
//		deviceData.transferDay = tourData.getStartDay();

		return true;
	}

	public boolean validateRawData(String fileName) {
		return true;
	}

	private boolean parseXmlFile(String fileName) {

		//get the factory
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

		try {

			//Using factory get an instance of document builder
			DocumentBuilder db = dbf.newDocumentBuilder();

			//parse using builder to get DOM representation of the XML file
			dom = db.parse("file:" + fileName);
			return true;

		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (SAXException se) {
			se.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return false;
	}

	static TimeZone		utc	= TimeZone.getTimeZone("GMT");
	static DateFormat	iso	= new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	{
		iso.setTimeZone(utc);
	}

	private void parseDocument(String importFileName, HashMap<String, TourData> tourDataMap) {

		TimeData timeData;
		long tourDateTime = 0;

		//get the root element
		Element docEle = dom.getDocumentElement();

		NodeList activityList = docEle.getElementsByTagName("Activity");

		if (activityList != null && activityList.getLength() > 0) {
			for (int activityIndex = 0; activityIndex < activityList.getLength(); activityIndex++) {

				Element activity = (Element) activityList.item(activityIndex);

				NodeList lapList = activity.getElementsByTagName("Lap");

				// create data object for each tour
				TourData tourData = new TourData();

				// create a list which contains all time slices 
				ArrayList<TimeData> timeDataList = new ArrayList<TimeData>();

				int prevAltitude = 0;
				int prevDistance = 0;
				long prevTime = -1;
				long tourTime = -1;

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

										final int altitude = (int) getFloatValue(tp,
												"AltitudeMeters");
										final int distance = (int) getFloatValue(tp,
												"DistanceMeters");
										final int pulse = getIntValue(tp, "HeartRateBpm", "Value");

										// ignore imcomplete values
										if (altitude != Integer.MIN_VALUE
												&& distance != Integer.MIN_VALUE
												&& pulse != Integer.MIN_VALUE) {

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

				/*
				 * unique key is used to create the tour id, this key is combined with the tour
				 * start date/time to identify the tour in the database. The unique key can be the
				 * distance of the tour or any other unique data
				 */
				String uniqueKey = "23221";

				// after all data are added, the tour id can be created
				tourData.createTourId(uniqueKey);

				// check if the tour is already imported
				final String tourId = tourData.getTourId().toString();
				if (tourDataMap.containsKey(tourId) == false) {

					// add new tour to other tours
					tourDataMap.put(tourId, tourData);

					tourData.createTimeSeries(timeDataList, true);
					tourData.computeTourDrivingTime();
					tourData.computeAvgFields();

					tourData.setDeviceId(deviceId);
					tourData.setDeviceName(visibleName);
				}
			}
		}
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
	 * I take a xml element and the tag name, look for the tag and get the text content i.e for
	 * <employee><name>John</name></employee> xml snippet if
	 * the Element points to employee node and tagName is 'name' I will return John
	 */
	private String getTextValue(Element ele, String tagName) {

		String textVal = null;
		NodeList nl = ele.getElementsByTagName(tagName);

		if (nl != null && nl.getLength() > 0) {
			Element el = (Element) nl.item(0);
			textVal = el.getFirstChild().getNodeValue();
		}

		return textVal;
	}

	/**
	 * Calls getTextValue and returns a int value
	 */
//	private int getIntValue(Element ele, String tagName) {
//		//in production application you would catch the exception
//		return Integer.parseInt(getTextValue(ele, tagName));
//	}
	private int getIntValue(Element ele, String parentTagName, String tagName) {
		//in production application you would catch the exception
		final String textValue = getTextValue(ele, parentTagName, tagName);
		if (textValue != null) {
			return Integer.parseInt(textValue);
		} else {
			return Integer.MIN_VALUE;
		}
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
}
