/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.MtMath;
import net.tourbook.common.util.Util;
import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourType;
import net.tourbook.data.TourWayPoint;
import net.tourbook.database.TourDatabase;
import net.tourbook.device.Activator;
import net.tourbook.device.IPreferences;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.importdata.TourbookDevice;
import net.tourbook.preferences.TourTypeColorDefinition;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class GPX_SAX_Handler extends DefaultHandler {

//	// A CDATA section starts with "<![CDATA[" and ends with "]]>"
//	private static final String				CDATA_START					= "<![CDATA[";										//$NON-NLS-1$
//	private static final String				CDATA_END					= "]]>";											//$NON-NLS-1$

	private static final String				ATTR_GPX_VERSION				= "version";								//$NON-NLS-1$
	private static final String				ATTR_GPX_VERSION_1_0			= "1.0";									//$NON-NLS-1$

	private static final String				NAME_SPACE_GPX_1_0				= "http://www.topografix.com/GPX/1/0";		//$NON-NLS-1$
	private static final String				NAME_SPACE_GPX_1_1				= "http://www.topografix.com/GPX/1/1";		//$NON-NLS-1$
	private static final String				POLAR_WEBSYNC_CREATOR_2_3		= "Polar WebSync 2.3 - www.polar.fi";		//$NON_NLS-1$ //$NON-NLS-1$
	private static final String				GH600							= "code.google.com/p/GH615";				//$NON-NLS-1$

	// namespace for extensions used by Garmin
//	private static final String				NAME_SPACE_TPEXT		= "http://www.garmin.com/xmlschemas/TrackPointExtension/v1";	//$NON-NLS-1$

	private static final int				GPX_VERSION_1_0					= 10;
	private static final int				GPX_VERSION_1_1					= 11;

	/*
	 * gpx tags, attributes
	 */
	private static final String				TAG_GPX							= "gpx";									//$NON-NLS-1$
	private static final String				TAG_METADATA					= "metadata";								//$NON-NLS-1$

	private static final String				TAG_TRK							= "trk";									//$NON-NLS-1$
	private static final String				TAG_TRK_NAME					= "name";									//$NON-NLS-1$
	private static final String				TAG_TRK_DESC					= "desc";									//$NON-NLS-1$
	private static final String				TAG_TRKPT						= "trkpt";									//$NON-NLS-1$

	private static final String				TAG_TIME						= "time";									//$NON-NLS-1$
	private static final String				TAG_ELE							= "ele";									//$NON-NLS-1$

	private static final String				TAG_WPT							= "wpt";									//$NON-NLS-1$
	private static final String				TAG_WPT_CMT						= "cmt";									//$NON-NLS-1$
	private static final String				TAG_WPT_DESC					= "desc";									//$NON-NLS-1$
	private static final String				TAG_WPT_ELE						= "ele";									//$NON-NLS-1$
	private static final String				TAG_WPT_NAME					= "name";									//$NON-NLS-1$
	private static final String				TAG_WPT_SYM						= "sym";									//$NON-NLS-1$
	private static final String				TAG_WPT_TIME					= "time";									//$NON-NLS-1$
	private static final String				TAG_WPT_TYPE					= "type";									//$NON-NLS-1$

	// <url> URL associated with the waypoint
	private static final String				TAG_WPT_URL						= "url";									//$NON-NLS-1$

	// <urlname> Text to display on the <url> hyperlink
	private static final String				TAG_WPT_URLNAME					= "urlname";								//$NON-NLS-1$

	// http://www.cluetrust.com/XML/GPXDATA/1/0
	// http://www.cluetrust.com/Schemas/gpxdata10.xsd
	private static final String				TAG_EXT_DATA_DISTANCE			= "gpxdata:distance";						//$NON-NLS-1$
	private static final String				TAG_EXT_DATA_ELAPSED_TIME		= "gpxdata:elapsedTime";					//$NON-NLS-1$
	private static final String				TAG_EXT_DATA_END_POINT			= "gpxdata:endPoint";						//$NON-NLS-1$
	private static final String				TAG_EXT_DATA_HR					= "gpxdata:hr";							//$NON-NLS-1$
	private static final String				TAG_EXT_DATA_INDEX				= "gpxdata:index";							//$NON-NLS-1$
	private static final String				TAG_EXT_DATA_LAP				= "gpxdata:lap";							//$NON-NLS-1$
	private static final String				TAG_EXT_DATA_START_TIME			= "gpxdata:startTime";						//$NON-NLS-1$
	private static final String				TAG_EXT_DATA_TEMPERATURE		= "gpxdata:temp";							//$NON-NLS-1$

	private static final String				TAG_EXT_CAD						= "cadence";								//$NON-NLS-1$
	private static final String				TAG_EXT_HR						= "heartrate";								//$NON-NLS-1$

	private static final String				TAG_EXT_TPX_CAD					= "gpxtpx:cad";							//$NON-NLS-1$
	private static final String				TAG_EXT_TPX_HR					= "gpxtpx:hr";								//$NON-NLS-1$
	private static final String				TAG_EXT_TPX_TEMP				= "gpxtpx:atemp";							//$NON-NLS-1$

	// xmlns:un="http://www.falk.de/GPX/OutdoorExtension"
	private static final String				TAG_EXT_UN_CAD					= "un:cad";								//$NON-NLS-1$
	private static final String				TAG_EXT_UN_HR					= "un:hr";									//$NON-NLS-1$
	private static final String				TAG_EXT_UN_POWER				= "un:power";								//$NON-NLS-1$

	// xmlns:mt="net.tourbook/1"
	// marker
	private static final String				TAG_MT_MARKER_DISTANCE			= "mt:distance";							//$NON-NLS-1$
	private static final String				TAG_MT_MARKER_IS_VISIBLE		= "mt:isVisible";							//$NON-NLS-1$
	private static final String				TAG_MT_MARKER_LABEL_POS			= "mt:labelPos";							//$NON-NLS-1$
	private static final String				TAG_MT_MARKER_LABEL_X_OFFSET	= "mt:labelXOffset";						//$NON-NLS-1$
	private static final String				TAG_MT_MARKER_LABEL_Y_OFFSET	= "mt:labelYOffset";						//$NON-NLS-1$
	private static final String				TAG_MT_MARKER_SERIE_INDEX		= "mt:serieIndex";							//$NON-NLS-1$
	private static final String				TAG_MT_MARKER_TYPE				= "mt:type";								//$NON-NLS-1$

	// serie data
	private static final String				TAG_MT_SERIE_GEAR				= "mt:gear";								//$NON-NLS-1$

	// tour

//	<mt:tourType>
//		<mt:id>34</mt:id>
//		<mt:name>Rennvelo 2</mt:name>
//	</mt:tourType>
//	<mt:tags>
//		<mt:tag>
//			<mt:id>20</mt:id>
//			<mt:name>Panne</mt:name>
//		</mt:tag>
//	</mt:tags>

	private static final String				TAG_MT_TOUR_DESCRIPTION			= "mt:tourDescription";					//$NON-NLS-1$
	private static final String				TAG_MT_TOUR_TITLE				= "mt:tourTitle";							//$NON-NLS-1$
	private static final String				TAG_MT_TOUR_START_PLACE			= "mt:tourStartPlace";						//$NON-NLS-1$
	private static final String				TAG_MT_TOUR_END_PLACE			= "mt:tourEndPlace";						//$NON-NLS-1$

	private static final String				TAG_MT_TOUR_START_TIME			= "mt:tourStartTime";						//$NON-NLS-1$
	private static final String				TAG_MT_TOUR_END_TIME			= "mt:tourEndTime";						//$NON-NLS-1$
	private static final String				TAG_MT_TOUR_DRIVING_TIME		= "mt:tourDrivingTime";					//$NON-NLS-1$
	private static final String				TAG_MT_TOUR_RECORDING_TIME		= "mt:tourRecordingTime";					//$NON-NLS-1$

	private static final String				TAG_MT_TOUR_ALTITUDE_UP			= "mt:tourAltUp";							//$NON-NLS-1$
	private static final String				TAG_MT_TOUR_ALTITUDE_DOWN		= "mt:tourAltDown";						//$NON-NLS-1$
	private static final String				TAG_MT_TOUR_DISTANCE			= "mt:tourDistance";						//$NON-NLS-1$

	private static final String				TAG_MT_TOUR_CALORIES			= "mt:calories";							//$NON-NLS-1$
	private static final String				TAG_MT_TOUR_REST_PULSE			= "mt:restPulse";							//$NON-NLS-1$

	private static final String				TAG_MT_TOUR_BIKER_WEIGHT		= "mt:bikerWeight";						//$NON-NLS-1$
	private static final String				TAG_MT_TOUR_CONCONI_DEFLECTION	= "mt:conconiDeflection";					//$NON-NLS-1$
	private static final String				TAG_MT_TOUR_DP_TOLERANCE		= "mt:dpTolerance";						//$NON-NLS-1$

	private static final String				TAG_MT_TOUR_TEMPERATURE			= "mt:temperature";						//$NON-NLS-1$
	private static final String				TAG_MT_TOUR_WEATHER				= "mt:weather";							//$NON-NLS-1$
	private static final String				TAG_MT_TOUR_WEATHER_CLOUDS		= "mt:weatherClouds";						//$NON-NLS-1$
	private static final String				TAG_MT_TOUR_WEATHER_WIND_DIR	= "mt:weatherWindDirection";				//$NON-NLS-1$
	private static final String				TAG_MT_TOUR_WEATHER_WIND_SPEED	= "mt:weatherWindSpeed";					//$NON-NLS-1$

	private static final String				TAG_MT_TOUR_TAG					= "mt:tag";								//$NON-NLS-1$
	private static final String				TAG_MT_TOUR_TYPE				= "mt:tourType";							//$NON-NLS-1$
	private static final String				TAG_MT_TOUR_SUB_NAME			= "mt:name";								//$NON-NLS-1$

	private static final String				ATTR_LATITUDE					= "lat";									//$NON-NLS-1$
	private static final String				ATTR_LONGITUDE					= "lon";									//$NON-NLS-1$

	private static final DateTimeFormatter	_dtFormatterShort				= DateTimeFormat.mediumDateTime();

	private static final SimpleDateFormat	GPX_TIME_FORMAT					= new SimpleDateFormat(
																					"yyyy-MM-dd'T'HH:mm:ss'Z'");		//$NON-NLS-1$
	private static final SimpleDateFormat	GPX_TIME_FORMAT_SSSZ			= new SimpleDateFormat(
																					"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");	//$NON-NLS-1$
	private static final SimpleDateFormat	GPX_TIME_FORMAT_RFC822			= new SimpleDateFormat(
																					"yyyy-MM-dd'T'HH:mm:ssZ");			//$NON-NLS-1$
	private static final long				DEFAULT_DATE_TIME				= ZonedDateTime
																					.of(
																							2000,
																							1,
																							1,
																							0,
																							0,
																							0,
																							0,
																							TimeTools
																									.getDefaultTimeZone())
																					.toInstant()
																					.toEpochMilli();

	private IPreferenceStore				_prefStore						= Activator
																					.getDefault()
																					.getPreferenceStore();

	private int								_gpxVersion						= -1;
	private boolean							_gpxHasLocalTime;															// To work around a Polar Websync export bug...

	private boolean							_isInMetaData;
	private boolean							_isInTrk;
	private boolean							_isInTrkName;
	private boolean							_isInTrkDesc;
	private boolean							_isInTrkPt;

	private boolean							_isInTime;
	private boolean							_isInEle;

	// gpx extensions
	private boolean							_isInCadence;
	private boolean							_isInDistance;
	private boolean							_isInHr;
	private boolean							_isInPower;
	private boolean							_isInTemp;

	// www.cluetrust.com extensions
	private boolean							_isInGpxDataIndex;
	private boolean							_isInGpxDataLap;
	private boolean							_isInGpxDataStartTime;
	private boolean							_isInGpxDataElapsedTime;
	private boolean							_isInGpxDataDistance;

	private boolean							_isInMT_Tour;
	private boolean							_isInMT_TourTag;
	private boolean							_isInMT_TourType;
	private boolean							_isInMT_Trk;
	private boolean							_isInMT_Wpt;

	/*
	 * wap points
	 */
	private boolean							_isInWpt;
	private boolean							_isInWpt_Ele;
	private boolean							_isInWpt_Time;
	private boolean							_isInWpt_Name;
	private boolean							_isInWpt_Cmt;
	private boolean							_isInWpt_Desc;
	private boolean							_isInWpt_Sym;
	private boolean							_isInWpt_Type;
	private boolean							_isInWpt_UrlAddress;
	private boolean							_isInWpt_UrlText;

	private final ArrayList<TimeData>		_timeDataList					= new ArrayList<TimeData>();
	private TimeData						_timeSlice;
	private TimeData						_prevTimeSlice;
	private String							_trkDesc;
	private String							_trkName;

	private final TourbookDevice			_device;
	private final String					_importFilePath;
	private HashMap<Long, TourData>			_alreadyImportedTours;
	private HashMap<Long, TourData>			_newlyImportedTours;
	private int								_trackCounter;

	private final Set<TourMarker>			_allMarker						= new HashSet<TourMarker>();
	private final ArrayList<TourWayPoint>	_allWayPoints					= new ArrayList<TourWayPoint>();
	private final ArrayList<String>			_allImportedTagNames			= new ArrayList<String>();

	private TourData						_tourData;
	private TourMarker						_tempTourMarker;
	private TourWayPoint					_wayPoint;
	private String							_tourTypeName;

	private final ArrayList<GPXDataLap>		_gpxDataList					= new ArrayList<GPXDataLap>();
	private GPXDataLap						_gpxDataLap;

	private boolean							_isSetTrackMarker;
	private boolean							_isTourMarkerImported;

	private float							_absoluteDistance;

	private boolean							_isImported;
	private boolean							_isError;

	private final StringBuilder				_characters						= new StringBuilder();

	private boolean							_isAbsoluteDistance;
	private float							_gpxAbsoluteDistance;

	/**
	 * Is <code>true</code> when tour contained mt: custom tags.
	 */
	private boolean							_isMTData;

	{
		GPX_TIME_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$
		GPX_TIME_FORMAT_SSSZ.setTimeZone(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$
		GPX_TIME_FORMAT_RFC822.setTimeZone(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$
	}

	private class GPXDataLap {

		public String	index;
		public long		absoluteTime;
		public double	latitude;
		public double	longitude;
		public String	elapsedTime;
		public float	distance;
	}

	public GPX_SAX_Handler(	final TourbookDevice deviceDataReader,
							final String importFileName,
							final DeviceData deviceData,
							final HashMap<Long, TourData> alreadyImportedTours,
							final HashMap<Long, TourData> newlyImportedTours) {

		_device = deviceDataReader;
		_importFilePath = importFileName;
		_alreadyImportedTours = alreadyImportedTours;
		_newlyImportedTours = newlyImportedTours;

		_gpxHasLocalTime = false; // Polar exports local time :-(

		/*
		 * Relative distances are the first implementation for distance values but the
		 * <gpxdata:distance> tags can also contain absolute values.
		 */
		final boolean isRelativeDistance = _prefStore.getBoolean(IPreferences.GPX_IS_RELATIVE_DISTANCE_VALUE);
		_isAbsoluteDistance = isRelativeDistance == false;
	}

	@Override
	public void characters(final char[] chars, final int startIndex, final int length) throws SAXException {

		if (_isInTrkName
				|| _isInTrkDesc
				|| _isInTime
				|| _isInEle
				|| _isInCadence
				|| _isInHr
				|| _isInPower
				|| _isInTemp
				|| _isInDistance
				//
				|| _isInMT_Tour
				|| _isInMT_TourTag
				|| _isInMT_TourType
				|| _isInMT_Trk
				|| _isInMT_Wpt
				//
				|| _isInWpt_Cmt
				|| _isInWpt_Desc
				|| _isInWpt_Ele
				|| _isInWpt_Name
				|| _isInWpt_Sym
				|| _isInWpt_Time
				|| _isInWpt_Type
				|| _isInWpt_UrlAddress
				|| _isInWpt_UrlText
				//
				|| _isInGpxDataIndex
				|| _isInGpxDataStartTime
				|| _isInGpxDataElapsedTime
				|| _isInGpxDataDistance
		//
		) {

			_characters.append(chars, startIndex, length);
		}
	}

	private void displayError(final ParseException e) {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				final String message = e.getMessage();
				MessageDialog.openError(Display.getCurrent().getActiveShell(), "Error", message); //$NON-NLS-1$
				System.err.println(message + " in " + _importFilePath); //$NON-NLS-1$
			}
		});
	}

	@Override
	public void endDocument() throws SAXException {

		if (_newlyImportedTours.size() == 1 && _device.isMergeTracks) {

			/*
			 * Remove annoying marker when only 1 tour is imported
			 */

			final TourData tourData = (TourData) _newlyImportedTours.values().toArray()[0];

			final Set<TourMarker> tourMarkers = tourData.getTourMarkers();

			if (tourMarkers.size() > 0) {

				// this happened

				// sort by serie index
				final ArrayList<TourMarker> sortedMarkers = new ArrayList<TourMarker>(tourMarkers);
				Collections.sort(sortedMarkers);

				final TourMarker firstMarker = sortedMarkers.get(0);
				tourMarkers.remove(firstMarker);
			}
		}
	}

	@Override
	public void endElement(final String uri, final String localName, final String name) throws SAXException {

//		System.out.println("</" + name + ">");

		if (_isError) {
			return;
		}

		try {

			final String charData = _characters.toString();

//			System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//					+ ("\tcharData: " + charData));
//			// TODO remove SYSTEM.OUT.PRINTLN

//			if (charData.startsWith(CDATA_START)) {
//
//				if (charData.endsWith(CDATA_END)) {
//
//					final int start = CDATA_START.length();
//					final int end = charData.length() - CDATA_END.length();
//
//					charData = charData.substring(start, end);
//				}
//			}

			if (_isInTrk) {

				endElement_TRK(name, charData);

			} else if (_isInWpt) {

				// in <wpt>

				endElement_WPT(name, charData);

			} else if (_isInMT_Tour) {

				endElement_MT_Tour(name, charData);

			} else if (_isInMT_TourTag) {

				if (name.equals(TAG_MT_TOUR_SUB_NAME)) {

					_allImportedTagNames.add(charData);
					_isInMT_TourTag = false;
				}

			} else if (_isInMT_TourType) {

				if (name.equals(TAG_MT_TOUR_SUB_NAME)) {

					_tourTypeName = charData;
					_isInMT_TourType = false;
				}
			}

			if (name.equals(TAG_TRKPT)) {

				/*
				 * trackpoint ends
				 */

				_isInTrkPt = false;

				finalizeTrackpoint();

			} else if (name.equals(TAG_TRK)) {

				/*
				 * track ends
				 */

				_isInTrk = false;

				if (_device.isMergeTracks == false) {
					finalizeTour();
				}

			} else if (name.equals(TAG_WPT)) {

				/*
				 * way point ends
				 */

				_isInWpt = false;

				finalizeWayPoint();

			} else if (name.equals(TAG_GPX)) {

				/*
				 * file end
				 */

				if (_device.isMergeTracks) {
					finalizeTour();
				}

			} else if (name.equals(TAG_EXT_DATA_LAP)) {

				/*
				 * lap ends
				 */
				_isInGpxDataLap = false;
				finalizeLap();

			} else if (name.equals(TAG_METADATA)) {

				_isInMetaData = false;
			}

		} catch (final NumberFormatException e) {
			net.tourbook.common.util.StatusUtil.showStatus(e);
		}

	}

	private void endElement_MT_Tour(final String name, final String charData) {

		if (name.equals(TAG_MT_TOUR_DESCRIPTION)) {

			_tourData.setTourDescription(charData);
			_isInMT_Tour = false;

		} else if (name.equals(TAG_MT_TOUR_TITLE)) {

			_tourData.setTourTitle(charData);
			_isInMT_Tour = false;

		} else if (name.equals(TAG_MT_TOUR_START_PLACE)) {

			_tourData.setTourStartPlace(charData);
			_isInMT_Tour = false;

		} else if (name.equals(TAG_MT_TOUR_END_PLACE)) {

			_tourData.setTourEndPlace(charData);
			_isInMT_Tour = false;

		} else if (name.equals(TAG_MT_TOUR_START_TIME)) {

			final long tourStartMills = getLongValue(charData);

			final ZonedDateTime tourStartTime = TimeTools.getZonedDateTime(tourStartMills);

			_tourData.setTourStartTime(tourStartTime);
			_isInMT_Tour = false;

		} else if (name.equals(TAG_MT_TOUR_END_TIME)) {

			_isInMT_Tour = false;

		} else if (name.equals(TAG_MT_TOUR_DRIVING_TIME)) {

			_tourData.setTourDrivingTime(getIntValue(charData));
			_isInMT_Tour = false;

		} else if (name.equals(TAG_MT_TOUR_RECORDING_TIME)) {

			_tourData.setTourRecordingTime(getLongValue(charData));
			_isInMT_Tour = false;

		} else if (name.equals(TAG_MT_TOUR_ALTITUDE_DOWN)) {

			_tourData.setTourAltDown(getFloatValue(charData));
			_isInMT_Tour = false;

		} else if (name.equals(TAG_MT_TOUR_ALTITUDE_UP)) {

			_tourData.setTourAltUp(getFloatValue(charData));
			_isInMT_Tour = false;

		} else if (name.equals(TAG_MT_TOUR_DISTANCE)) {

			_tourData.setTourDistance(getFloatValue(charData));
			_isInMT_Tour = false;

		} else if (name.equals(TAG_MT_TOUR_CALORIES)) {

			_tourData.setCalories(getIntValue(charData));
			_isInMT_Tour = false;

		} else if (name.equals(TAG_MT_TOUR_REST_PULSE)) {

			_tourData.setRestPulse(getIntValue(charData));
			_isInMT_Tour = false;

		} else if (name.equals(TAG_MT_TOUR_BIKER_WEIGHT)) {

			_tourData.setBodyWeight(getFloatValue(charData));
			_isInMT_Tour = false;

		} else if (name.equals(TAG_MT_TOUR_CONCONI_DEFLECTION)) {

			_tourData.setConconiDeflection(getIntValue(charData));
			_isInMT_Tour = false;

		} else if (name.equals(TAG_MT_TOUR_DP_TOLERANCE)) {

			_tourData.setDpTolerance((short) getIntValue(charData));
			_isInMT_Tour = false;

		} else if (name.equals(TAG_MT_TOUR_TEMPERATURE)) {

			_tourData.setAvgTemperature(getFloatValue(charData));
			_isInMT_Tour = false;

		} else if (name.equals(TAG_MT_TOUR_WEATHER)) {

			_tourData.setWeather(charData);
			_isInMT_Tour = false;

		} else if (name.equals(TAG_MT_TOUR_WEATHER_CLOUDS)) {

			_tourData.setWeatherClouds(charData);
			_isInMT_Tour = false;

		} else if (name.equals(TAG_MT_TOUR_WEATHER_WIND_DIR)) {

			_tourData.setWeatherWindDir(getIntValue(charData));
			_isInMT_Tour = false;

		} else if (name.equals(TAG_MT_TOUR_WEATHER_WIND_SPEED)) {

			_tourData.setWeatherWindSpeed(getIntValue(charData));
			_isInMT_Tour = false;
		}
	}

	private void endElement_TRK(final String name, final String charData) {

		if (_isInTrkPt) {

			if (name.equals(TAG_ELE)) {

				// </ele>

				_isInEle = false;

				_timeSlice.absoluteAltitude = getFloatValue(charData);

			} else if (name.equals(TAG_TIME)) {

				// </time>

				_isInTime = false;

				try {
					_timeSlice.absoluteTime = ZonedDateTime.parse(charData).toInstant().toEpochMilli();
				} catch (final Exception e0) {
					try {
						_timeSlice.absoluteTime = GPX_TIME_FORMAT.parse(charData).getTime();
					} catch (final ParseException e1) {
						try {
							_timeSlice.absoluteTime = GPX_TIME_FORMAT_SSSZ.parse(charData).getTime();
						} catch (final ParseException e2) {
							try {
								_timeSlice.absoluteTime = GPX_TIME_FORMAT_RFC822.parse(charData).getTime();
							} catch (final ParseException e3) {

								_isError = true;

								displayError(e3);
							}
						}
					}
				}

			} else if (name.equals(TAG_MT_SERIE_GEAR)) {

				// </mt:gear>

				_isInMT_Trk = false;
				_timeSlice.gear = getLongValue(charData);

			} else if (name.equals(TAG_EXT_TPX_CAD)) {

				// </gpxtpx:cad>

				_isInCadence = false;
				_timeSlice.cadence = getFloatValue(charData);

			} else if (name.equals(TAG_EXT_CAD) || name.equals(TAG_EXT_UN_CAD)) {

				// </cadence>
				// </un:cad>

				_isInCadence = false;
				_timeSlice.cadence = getIntValue(charData);

			} else if (name.equals(TAG_EXT_TPX_HR)) {

				// </gpxtpx:hr>

				_isInHr = false;
				_timeSlice.pulse = getFloatValue(charData);

			} else if (name.equals(TAG_EXT_DATA_HR) //
					|| name.equals(TAG_EXT_HR)
					|| name.equals(TAG_EXT_UN_HR)) {

				// </gpxdata:hr>
				// </heartrate>
				// </un:hr>

				_isInHr = false;
				_timeSlice.pulse = getIntValue(charData);

			} else if (name.equals(TAG_EXT_UN_POWER)) {

				// </un:power>

				_isInPower = false;
				_timeSlice.power = getFloatValue(charData);

			} else if (name.equals(TAG_EXT_TPX_TEMP) //
					|| name.equals(TAG_EXT_DATA_TEMPERATURE)) {

				// </gpxtpx:atemp>
				// </gpxdata:temp>

				_isInTemp = false;
				_timeSlice.temperature = getFloatValue(charData);

			} else if (name.equals(TAG_EXT_DATA_DISTANCE)) {

				// </gpxdata:distance>

				_isInDistance = false;

				final float gpxExtDistanceValue = getFloatValue(charData);

				float relativeDistanceValue;

				if (_isAbsoluteDistance && gpxExtDistanceValue != Float.MIN_VALUE) {

					final float oldAbsoluteDistance = _gpxAbsoluteDistance;
					_gpxAbsoluteDistance = gpxExtDistanceValue;

					relativeDistanceValue = gpxExtDistanceValue - oldAbsoluteDistance;

				} else {

					// this is the default, distance value is relative and correct

					relativeDistanceValue = gpxExtDistanceValue;
				}

				_timeSlice.gpxDistance = relativeDistanceValue;
			}

		} else if (name.equals(TAG_TRK_NAME)) {

			// </name> track name

			_isInTrkName = false;
			_trkName = charData;

		} else if (name.equals(TAG_TRK_DESC)) {

			// </name> track name

			_isInTrkDesc = false;
			_trkDesc = charData;

		} else if (_isInGpxDataLap) {

			if (name.equals(TAG_EXT_DATA_INDEX)) {

				_isInGpxDataIndex = false;
				_gpxDataLap.index = charData;

			} else if (name.equals(TAG_EXT_DATA_START_TIME)) {

				_isInGpxDataStartTime = false;

				try {
					_gpxDataLap.absoluteTime = ZonedDateTime.parse(charData).toInstant().toEpochMilli();
				} catch (final Exception e0) {
					try {
						_gpxDataLap.absoluteTime = GPX_TIME_FORMAT.parse(charData).getTime();
					} catch (final ParseException e1) {
						try {
							_gpxDataLap.absoluteTime = GPX_TIME_FORMAT_SSSZ.parse(charData).getTime();
						} catch (final ParseException e2) {
							try {
								_gpxDataLap.absoluteTime = GPX_TIME_FORMAT_RFC822.parse(charData).getTime();
							} catch (final ParseException e3) {

								_isError = true;

								displayError(e3);
							}
						}
					}
				}
			} else if (name.equals(TAG_EXT_DATA_ELAPSED_TIME)) {

				_isInGpxDataElapsedTime = false;
				_gpxDataLap.elapsedTime = charData;

			} else if (name.equals(TAG_EXT_DATA_DISTANCE)) {

				_isInGpxDataDistance = false;
				_gpxDataLap.distance = getFloatValue(charData);

			}
		}
	}

	private void endElement_WPT(final String name, final String charData) {

		if (name.equals(TAG_WPT_ELE)) {

			// </ele> elevation

			_isInWpt_Ele = false;

			_wayPoint.setAltitude(getFloatValue(charData));

		} else if (name.equals(TAG_WPT_TIME)) {

			// </time>

			_isInWpt_Time = false;

			try {
				_wayPoint.setTime(ZonedDateTime.parse(charData).toInstant().toEpochMilli());
			} catch (final Exception e0) {
				try {
					_wayPoint.setTime(GPX_TIME_FORMAT.parse(charData).getTime());
				} catch (final ParseException e1) {
					try {
						_wayPoint.setTime(GPX_TIME_FORMAT_SSSZ.parse(charData).getTime());
					} catch (final ParseException e2) {
						try {
							_wayPoint.setTime(GPX_TIME_FORMAT_RFC822.parse(charData).getTime());
						} catch (final ParseException e3) {

							_isError = true;

							displayError(e3);
						}
					}
				}
			}

		} else if (name.equals(TAG_WPT_NAME)) {

			// </name> name

			_isInWpt_Name = false;
			_wayPoint.setName(charData);

		} else if (name.equals(TAG_WPT_CMT)) {

			// </cmt> comment

			_isInWpt_Cmt = false;
			_wayPoint.setComment(charData);

		} else if (name.equals(TAG_WPT_DESC)) {

			// </desc> description

			_isInWpt_Desc = false;
			_wayPoint.setDescription(charData);

		} else if (name.equals(TAG_WPT_SYM)) {

			// </sym> symbol

			_isInWpt_Sym = false;
			_wayPoint.setSymbol(charData);

		} else if (name.equals(TAG_WPT_TYPE)) {

			// </type> type/category

			_isInWpt_Type = true;
			_wayPoint.setCategory(charData);

		} else if (name.equals(TAG_WPT_URL)) {

			// </url> url address

			_isInWpt_UrlAddress = false;
			_wayPoint.setUrlAddress(charData);

		} else if (name.equals(TAG_WPT_URLNAME)) {

			// </urlname> url text

			_isInWpt_UrlText = false;
			_wayPoint.setUrlText(charData);

		} else if (name.equals(TAG_MT_MARKER_DISTANCE)) {

			// </mt:distance>

			_isInMT_Wpt = false;
			_tempTourMarker.setDistance(getFloatValue(charData));

		} else if (name.equals(TAG_MT_MARKER_IS_VISIBLE)) {

			// </mt:isVisible>

			_isInMT_Wpt = false;
			_tempTourMarker.setMarkerVisible(getBooleanValue(charData));

		} else if (name.equals(TAG_MT_MARKER_LABEL_POS)) {

			// </mt:labelPos>

			_isInMT_Wpt = false;
			_tempTourMarker.setLabelPosition(getIntValue(charData));

		} else if (name.equals(TAG_MT_MARKER_LABEL_X_OFFSET)) {

			// </mt:labelXOffset>

			_isInMT_Wpt = false;
			_tempTourMarker.setLabelXOffset(getIntValue(charData));

		} else if (name.equals(TAG_MT_MARKER_LABEL_Y_OFFSET)) {

			// </mt:labelYOffset>

			_isInMT_Wpt = false;
			_tempTourMarker.setLabelYOffset(getIntValue(charData));

		} else if (name.equals(TAG_MT_MARKER_SERIE_INDEX)) {

			// </mt:serieIndex>

			_isInMT_Wpt = false;
			_tempTourMarker.setSerieIndex(getIntValue(charData));

		} else if (name.equals(TAG_MT_MARKER_TYPE)) {

			// </mt:type>

			_isInMT_Wpt = false;
			_tempTourMarker.setType(getIntValue(charData));
		}
	}

	private void finalizeLap() {

		if (_gpxDataLap == null) {
			return;
		}

		_gpxDataList.add(_gpxDataLap);
	}

	private void finalizeTour() {

		if (_timeDataList.size() == 0) {
			// there is not data
// disabled to imports tour without tracks
//			return;
		}

		// insert Laps into _timeDataList
		insertLapData();

		// create data object for each tour

		if (_isMTData) {

			// title and description are already set

			_tourData.setIsImportedMTTour(true);

		} else {
			_tourData.setTourTitle(_trkName);
			_tourData.setTourDescription(_trkDesc);
		}

		if (_timeDataList.size() > 0) {

			// set tour start date/time

			final TimeData firstTimeData = _timeDataList.get(0);
			final Instant tourStartInstant = Instant.ofEpochMilli(firstTimeData.absoluteTime);

			ZonedDateTime dtTourStart;

			if (_gpxHasLocalTime) {

				// Polar WebSync creates GPX files with local time :-(
				// workaround: create DateTime object with UTC TimeZone => time is NOT converted to localtime

				dtTourStart = ZonedDateTime.ofInstant(tourStartInstant, TimeTools.UTC);

			} else {

				dtTourStart = ZonedDateTime.ofInstant(tourStartInstant, TimeTools.getDefaultTimeZone());
			}

			_tourData.setTourStartTime(dtTourStart);
		}

		_tourData.setDeviceTimeInterval((short) -1);

		_tourData.setImportFilePath(_importFilePath);

		_tourData.setTourMarkers(_allMarker);
		_tourData.setWayPoints(_allWayPoints);

		_tourData.setDeviceId(_device.deviceId);
		_tourData.setDeviceName(_device.visibleName);

		_tourData.createTimeSeries(_timeDataList, true);

		// after all data are added, the tour id can be created
		final String uniqueId = _device.createUniqueId(_tourData, Util.UNIQUE_ID_SUFFIX_GPX);
		final Long tourId = _tourData.createTourId(uniqueId);

		// check if the tour is already imported
		if (_alreadyImportedTours.containsKey(tourId) == false) {

			// add new tour to other tours
			_newlyImportedTours.put(tourId, _tourData);

			_tourData.computeAltitudeUpDown();
			_tourData.computeTourDrivingTime();
			_tourData.computeComputedValues();

			finalizeTour_AdjustMarker();
			finalizeTour_TourType();
			finalizeTour_Tags();

			if (_device.isConvertWayPoints) {
				_tourData.convertWayPoints();
			}
		}

		_tourData = null;
		_isImported = true;
	}

	private void finalizeTour_AdjustMarker() {

		final Set<TourMarker> tourMarkers = _tourData.getTourMarkers();

		final long tourStartTime = _tourData.getTourStartTimeMS();

		for (final TourMarker tourMarker : tourMarkers) {

			if (_isTourMarkerImported) {

				// set relative tour time

				final long absoluteMarkerTime = tourMarker.getTourTime();
				final long relativeTime = absoluteMarkerTime - tourStartTime;

				tourMarker.setTime((int) (relativeTime / 1000), absoluteMarkerTime);

			} else {

				// adjust default marker which are created in tourData.createTimeSeries()

				tourMarker.setLabelPosition(TourMarker.LABEL_POS_VERTICAL_BOTTOM_CHART);
			}
		}
	}

	private void finalizeTour_Tags() {

		if (_allImportedTagNames.size() == 0) {
			return;
		}

		final Set<TourTag> tourTags = new HashSet<TourTag>();

		final Collection<TourTag> dbTags = TourDatabase.getAllTourTags().values();
		final ArrayList<TourTag> tempTags = RawDataManager.getInstance().getTempTourTags();

		for (final String tagName : _allImportedTagNames) {

			TourTag tourTag = TourDatabase.findTourTag(tagName, dbTags);

			if (tourTag == null) {
				tourTag = TourDatabase.findTourTag(tagName, tempTags);
			}

			if (tourTag == null) {

				tourTag = new TourTag(tagName);
				tourTag.setRoot(true);
			}

			tourTags.add(tourTag);
		}

		_tourData.setTourTags(tourTags);
	}

	private void finalizeTour_TourType() {

		if (_tourTypeName == null) {
			return;
		}

		TourType tourType = TourDatabase.findTourType(_tourTypeName, TourDatabase.getAllTourTypes());

		final ArrayList<TourType> tempTourTypes = RawDataManager.getInstance().getTempTourTypes();

		if (tourType == null) {
			tourType = TourDatabase.findTourType(_tourTypeName, tempTourTypes);
		}

		if (tourType == null) {

			// create new tour type

			final TourType newTourType = new TourType(_tourTypeName);

			final TourTypeColorDefinition newColor = new TourTypeColorDefinition(//
					newTourType,
					Long.toString(newTourType.getTypeId()),
					newTourType.getName());

			newTourType.setColors(
					newColor.getGradientBright_Default(),
					newColor.getGradientDark_Default(),
					newColor.getLineColor_Default(),
					newColor.getTextColor_Default());

			tempTourTypes.add(newTourType);

			tourType = newTourType;
		}

		_tourData.setTourType(tourType);
	}

	private void finalizeTrackpoint() {

		if (_timeSlice == null) {
			return;
		}

		_timeDataList.add(_timeSlice);

		/*
		 * calculate distance
		 */
		if (_prevTimeSlice == null) {
			// first time data
			_timeSlice.absoluteDistance = 0;
		} else {
			if (_timeSlice.absoluteDistance == Float.MIN_VALUE) {

				if (_timeSlice.gpxDistance != Float.MIN_VALUE) {

					// get distance from gpx tag: <gpxdata:distance>

					_timeSlice.absoluteDistance = _absoluteDistance += _timeSlice.gpxDistance;

				} else {

					// compute distance from lat/lon

					_timeSlice.absoluteDistance = _absoluteDistance += MtMath.distanceVincenty(
							_prevTimeSlice.latitude,
							_prevTimeSlice.longitude,
							_timeSlice.latitude,
							_timeSlice.longitude);
				}
			}
		}

		final long originalTime = _timeSlice.absoluteTime;

		// set virtual time if time is not available
		if (_timeSlice.absoluteTime == Long.MIN_VALUE) {
			_timeSlice.absoluteTime = DEFAULT_DATE_TIME;
		}

		if (_isSetTrackMarker) {

			_isSetTrackMarker = false;

			final String labelText = Integer.toString(_trackCounter) + //
					(originalTime == Long.MIN_VALUE //
							? UI.EMPTY_STRING
							: UI.DASH_WITH_SPACE + _dtFormatterShort.print(_timeSlice.absoluteTime));

			final String markerLabel = NLS.bind(Messages.Marker_Label_Track, labelText);

			_timeSlice.marker = 1;
			_timeSlice.markerLabel = markerLabel;
		}

		_prevTimeSlice = _timeSlice;
	}

	private void finalizeWayPoint() {

		if (_wayPoint == null) {
			return;
		}

		// lat/lon are required fields
		if (_wayPoint.getLatitude() != Double.MIN_VALUE && _wayPoint.getLongitude() != Double.MIN_VALUE) {

			if (_wayPoint.isValidForSave()) {

				if (_tempTourMarker != null) {

					// tour marker is imported, put waypoint values into the tour marker

					final TourMarker tourMarker = new TourMarker(_tourData, _tempTourMarker.getType());

					tourMarker.setLatitude(_wayPoint.getLatitude());
					tourMarker.setLongitude(_wayPoint.getLongitude());

					tourMarker.setTime(-1, _wayPoint.getTime());
					tourMarker.setAltitude(_wayPoint.getAltitude());

					tourMarker.setLabel(_wayPoint.getName());
					tourMarker.setDescription(_wayPoint.getDescription());

					tourMarker.setUrlAddress(_wayPoint.getUrlAddress());
					tourMarker.setUrlText(_wayPoint.getUrlText());

					tourMarker.setDistance(_tempTourMarker.getDistance());
					tourMarker.setMarkerVisible(_tempTourMarker.isMarkerVisible());
					tourMarker.setLabelPosition(_tempTourMarker.getLabelPosition());
					tourMarker.setLabelXOffset(_tempTourMarker.getLabelXOffset());
					tourMarker.setLabelYOffset(_tempTourMarker.getLabelYOffset());
					tourMarker.setSerieIndex(_tempTourMarker.getSerieIndex());

					_allMarker.add(tourMarker);

				} else {

					// waypoint is imported

					_allWayPoints.add(_wayPoint);
				}
			}
		}

		_tempTourMarker = null;
		_wayPoint = null;
	}

	private boolean getBooleanValue(final String textValue) {

		try {
			if (textValue != null) {
				return Boolean.parseBoolean(textValue);
			} else {
				return false;
			}

		} catch (final NumberFormatException e) {
			return false;
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

	private long getLongValue(final String textValue) {

		try {
			if (textValue != null) {
				return Long.parseLong(textValue);
			} else {
				return Long.MIN_VALUE;
			}

		} catch (final NumberFormatException e) {
			return Long.MIN_VALUE;
		}
	}

	private void initNewTrack() {

		_tourData = new TourData();

		_timeDataList.clear();

		_allImportedTagNames.clear();
		_tourTypeName = null;

		_absoluteDistance = 0;
		_gpxAbsoluteDistance = 0;

		_prevTimeSlice = null;
		_trkName = null;
		_isTourMarkerImported = false;
	}

	private void insertLapData() {

		float absoluteDistance = 0;
		boolean needsSort = false;

		for (final GPXDataLap lap : _gpxDataList) {

			boolean found = false;
			absoluteDistance += lap.distance;

			for (final TimeData timeData : _timeDataList) {

				if ((lap.latitude == timeData.latitude) && (lap.longitude == timeData.longitude)) {

					/* timeslice already exists */
					timeData.marker = 1;
					timeData.markerLabel = NLS.bind(Messages.Marker_Label_Lap, Integer.parseInt(lap.index) + 1);

					found = true;
					break;
				}
			}
			if (!found) {
				/* create new timeSlice with Lap Data */
				final TimeData timeSlice = new TimeData();
				timeSlice.absoluteTime = lap.absoluteTime + Integer.parseInt(lap.elapsedTime) * 1000;
				timeSlice.latitude = lap.latitude;
				timeSlice.longitude = lap.longitude;
				timeSlice.marker = 1;
				timeSlice.markerLabel = NLS.bind(Messages.Marker_Label_Lap, Integer.parseInt(lap.index) + 1);
				timeSlice.absoluteDistance = absoluteDistance;

				_timeDataList.add(timeSlice);
				needsSort = true;
			}

		}

		if (needsSort) {
			/* sort the _timeDataList */
			Collections.sort(_timeDataList, new Comparator<TimeData>() {
				@Override
				public int compare(final TimeData td1, final TimeData td2) {
					if (td1.absoluteTime < td2.absoluteTime) {
						return -1;
					}
					if (td1.absoluteTime > td2.absoluteTime) {
						return 1;
					}

					return 0;
				}
			});
		}

	}

	/**
	 * @return Returns <code>true</code> when a tour was imported
	 */
	public boolean isImported() {

		if (_isError) {
			return false;
		}

		return _isImported;
	}

	@Override
	public void startElement(final String uri, final String localName, final String name, final Attributes attributes)
			throws SAXException {

//		System.out.print("<" + name + ">");

		if (_isError) {
			return;
		}

		if (_gpxVersion < 0) {

			// gpx version is not set

			if (name.equals(TAG_GPX)) {

				/*
				 * get version of the xml file
				 */
				for (int attrIndex = 0; attrIndex < attributes.getLength(); attrIndex++) {

					final String qName = attributes.getQName(attrIndex);
					final String value = attributes.getValue(attrIndex);

					if (value.contains(NAME_SPACE_GPX_1_0)

					// tolerate 'version="1.0"'
							|| (qName.toLowerCase().equals(ATTR_GPX_VERSION) && value.equals(ATTR_GPX_VERSION_1_0))) {

						_gpxVersion = GPX_VERSION_1_0;

						if (_device.isMergeTracks) {
							initNewTrack();
						}

					} else if (value.contains(NAME_SPACE_GPX_1_1)) {

						_gpxVersion = GPX_VERSION_1_1;

						if (_device.isMergeTracks) {
							initNewTrack();
						}

					} else if (value.contains(POLAR_WEBSYNC_CREATOR_2_3)) {

						_gpxHasLocalTime = true;

					} else if (value.contains(GH600)) {

						_gpxHasLocalTime = true;

					}
				}
			}

		} else if ((_gpxVersion == GPX_VERSION_1_0) || (_gpxVersion == GPX_VERSION_1_1)) {

			// name space: http://www.topografix.com/GPX/1/0/gpx.xsd

			if (_isInTrk) {

				startElement_TRK(name, attributes);

			} else if (_isInWpt) {

				startElement_WPT(name);

			} else if (_isInMetaData) {

				startElement_META(name);

			} else if (name.equals(TAG_TRK)) {

				/*
				 * new track starts
				 */

				_isInTrk = true;

				if (_device.isMergeTracks) {

					_trackCounter++;

					_isSetTrackMarker = true;

				} else {

					initNewTrack();
				}

			} else if (name.equals(TAG_WPT)) {

				/*
				 * new way point starts
				 */

				_isInWpt = true;

				_wayPoint = new TourWayPoint();

				// get attributes
				_wayPoint.setLatitude(getDoubleValue(attributes.getValue(ATTR_LATITUDE)));
				_wayPoint.setLongitude(getDoubleValue(attributes.getValue(ATTR_LONGITUDE)));

			} else if (name.equals(TAG_METADATA)) {

				_isInMetaData = true;
			}
		}
	}

	private void startElement_META(final String name) {

		if (_isInMT_TourTag || _isInMT_TourType) {

			if (name.equals(TAG_MT_TOUR_SUB_NAME)) {

				_characters.delete(0, _characters.length());
			}

		} else if (name.equals(TAG_MT_TOUR_DESCRIPTION)
				|| name.equals(TAG_MT_TOUR_TITLE)
				|| name.equals(TAG_MT_TOUR_START_PLACE)
				|| name.equals(TAG_MT_TOUR_END_PLACE)

				|| name.equals(TAG_MT_TOUR_START_TIME)
				|| name.equals(TAG_MT_TOUR_END_TIME)
				|| name.equals(TAG_MT_TOUR_DRIVING_TIME)
				|| name.equals(TAG_MT_TOUR_RECORDING_TIME)

				|| name.equals(TAG_MT_TOUR_ALTITUDE_DOWN)
				|| name.equals(TAG_MT_TOUR_ALTITUDE_UP)
				|| name.equals(TAG_MT_TOUR_DISTANCE)

				|| name.equals(TAG_MT_TOUR_CALORIES)
				|| name.equals(TAG_MT_TOUR_REST_PULSE)

				|| name.equals(TAG_MT_TOUR_BIKER_WEIGHT)
				|| name.equals(TAG_MT_TOUR_CONCONI_DEFLECTION)
				|| name.equals(TAG_MT_TOUR_DP_TOLERANCE)

				|| name.equals(TAG_MT_TOUR_TEMPERATURE)
				|| name.equals(TAG_MT_TOUR_WEATHER)
				|| name.equals(TAG_MT_TOUR_WEATHER_CLOUDS)
				|| name.equals(TAG_MT_TOUR_WEATHER_WIND_DIR)
				|| name.equals(TAG_MT_TOUR_WEATHER_WIND_SPEED)) {

			_isMTData = true;

			_isInMT_Tour = true;
			_characters.delete(0, _characters.length());

		} else if (name.equals(TAG_MT_TOUR_TAG)) {

			_isInMT_TourTag = true;

		} else if (name.equals(TAG_MT_TOUR_TYPE)) {

			_isInMT_TourType = true;
		}
	}

	private void startElement_TRK(final String name, final Attributes attributes) {

		if (_isInTrkPt) {

			if (name.equals(TAG_ELE)) {

				_isInEle = true;
				_characters.delete(0, _characters.length());

			} else if (name.equals(TAG_TIME)) {

				_isInTime = true;
				_characters.delete(0, _characters.length());

			} else if (name.equals(TAG_MT_SERIE_GEAR)) {

				_isInMT_Trk = true;
				_characters.delete(0, _characters.length());

			} else if (name.equals(TAG_EXT_TPX_CAD) //
					|| name.equals(TAG_EXT_CAD)
					|| name.equals(TAG_EXT_UN_CAD)) {

				_isInCadence = true;
				_characters.delete(0, _characters.length());

			} else if (name.equals(TAG_EXT_TPX_HR)
					|| name.equals(TAG_EXT_DATA_HR)
					|| name.equals(TAG_EXT_HR)
					|| name.equals(TAG_EXT_UN_HR)) {

				_isInHr = true;
				_characters.delete(0, _characters.length());

			} else if (name.equals(TAG_EXT_UN_POWER)) {

				_isInPower = true;
				_characters.delete(0, _characters.length());

			} else if (name.equals(TAG_EXT_TPX_TEMP) || //
					name.equals(TAG_EXT_DATA_TEMPERATURE)) {

				_isInTemp = true;
				_characters.delete(0, _characters.length());

			} else if (name.equals(TAG_EXT_DATA_DISTANCE)) {

				_isInDistance = true;
				_characters.delete(0, _characters.length());
			}

		} else if (name.equals(TAG_TRKPT) /* || name.equals(TAG_RTEPT) */) {

			/*
			 * new trackpoing
			 */
			_isInTrkPt = true;

			// create new time item
			_timeSlice = new TimeData();

			// get attributes
			_timeSlice.latitude = getDoubleValue(attributes.getValue(ATTR_LATITUDE));
			_timeSlice.longitude = getDoubleValue(attributes.getValue(ATTR_LONGITUDE));

		} else if (name.equals(TAG_TRK_NAME)) {

			_isInTrkName = true;
			_characters.delete(0, _characters.length());

		} else if (name.equals(TAG_TRK_DESC)) {

			_isInTrkDesc = true;
			_characters.delete(0, _characters.length());

		} else if (name.equals(TAG_EXT_DATA_LAP)) {

			_isInGpxDataLap = true;
			_gpxDataLap = new GPXDataLap();

		} else if (_isInGpxDataLap) {

			if (name.equals(TAG_EXT_DATA_INDEX)) {

				_isInGpxDataIndex = true;
				_characters.delete(0, _characters.length());

			} else if (name.equals(TAG_EXT_DATA_START_TIME)) {

				_isInGpxDataStartTime = true;
				_characters.delete(0, _characters.length());

			} else if (name.equals(TAG_EXT_DATA_END_POINT)) {

				_gpxDataLap.latitude = getDoubleValue(attributes.getValue(ATTR_LATITUDE));
				_gpxDataLap.longitude = getDoubleValue(attributes.getValue(ATTR_LONGITUDE));

			} else if (name.equals(TAG_EXT_DATA_ELAPSED_TIME)) {

				_isInGpxDataElapsedTime = true;
				_characters.delete(0, _characters.length());

			} else if (name.equals(TAG_EXT_DATA_DISTANCE)) {

				_isInGpxDataDistance = true;
				_characters.delete(0, _characters.length());
			}

		}
	}

	private void startElement_WPT(final String name) {

		if (name.equals(TAG_WPT_ELE)) {

			_isInWpt_Ele = true;
			_characters.delete(0, _characters.length());

		} else if (name.equals(TAG_WPT_TIME)) {

			_isInWpt_Time = true;
			_characters.delete(0, _characters.length());

		} else if (name.equals(TAG_WPT_NAME)) {

			_isInWpt_Name = true;
			_characters.delete(0, _characters.length());

		} else if (name.equals(TAG_WPT_CMT)) {

			_isInWpt_Cmt = true;
			_characters.delete(0, _characters.length());

		} else if (name.equals(TAG_WPT_DESC)) {

			_isInWpt_Desc = true;
			_characters.delete(0, _characters.length());

		} else if (name.equals(TAG_WPT_SYM)) {

			_isInWpt_Sym = true;
			_characters.delete(0, _characters.length());

		} else if (name.equals(TAG_WPT_TYPE)) {

			_isInWpt_Type = true;
			_characters.delete(0, _characters.length());

		} else if (name.equals(TAG_WPT_URL)) {

			_isInWpt_UrlAddress = true;
			_characters.delete(0, _characters.length());

		} else if (name.equals(TAG_WPT_URLNAME)) {

			_isInWpt_UrlText = true;
			_characters.delete(0, _characters.length());

		} else if (name.equals(TAG_MT_MARKER_DISTANCE)
				|| name.equals(TAG_MT_MARKER_IS_VISIBLE)
				|| name.equals(TAG_MT_MARKER_LABEL_POS)
				|| name.equals(TAG_MT_MARKER_LABEL_X_OFFSET)
				|| name.equals(TAG_MT_MARKER_LABEL_Y_OFFSET)
				|| name.equals(TAG_MT_MARKER_SERIE_INDEX)
				|| name.equals(TAG_MT_MARKER_TYPE)

		) {

			_isInMT_Wpt = true;
			_characters.delete(0, _characters.length());

			if (_tempTourMarker == null) {
				_tempTourMarker = new TourMarker();
			}

			_isTourMarkerImported = true;
		}
	}
}
