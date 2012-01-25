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
package net.tourbook.device.polar.hrm;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;

import net.tourbook.chart.ChartLabel;
import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.SerialParameters;
import net.tourbook.importdata.TourbookDevice;
import net.tourbook.ui.UI;
import net.tourbook.util.StatusUtil;
import net.tourbook.util.Util;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.joda.time.DateTime;

/**
 * This device reader is importing data from Polar device files.
 */
public class PolarHRMDataReader extends TourbookDevice {

	/**
	 * Speed is saved in the .hrm file with 0.1 km/h
	 */
	private static final int		SPEED_SCALING			= 10;

	private static final String		DATA_DELIMITER			= "\t";											//$NON-NLS-1$

	private static final String		SECTION_START_CHARACTER	= "[";												//$NON-NLS-1$
	private static final String		SECTION_PARAMS			= "[Params]";										//$NON-NLS-1$
	//
	private static final String		SECTION_NOTE			= "[Note]";										//$NON-NLS-1$
	private static final String		SECTION_INT_TIMES		= "[IntTimes]";									//$NON-NLS-1$
	private static final String		SECTION_INT_NOTES		= "[IntNotes]";									//$NON-NLS-1$
	private static final String		SECTION_EXTRA_DATA		= "[ExtraData]";									//$NON-NLS-1$
	private static final String		SECTION_LAP_NAMES		= "[LapNames]";									//$NON-NLS-1$
	private static final String		SECTION_SUMMARY_123		= "[Summary-123]";									//$NON-NLS-1$
	private static final String		SECTION_SUMMARY_TH		= "[Summary-TH]";									//$NON-NLS-1$
	private static final String		SECTION_HR_ZONES		= "[HRZones]";										//$NON-NLS-1$
	private static final String		SECTION_SWAP_TIMES		= "[SwapTimes]";									//$NON-NLS-1$
	private static final String		SECTION_TRIP			= "[Trip]";										//$NON-NLS-1$
	private static final String		SECTION_HR_DATA			= "[HRData]";										//$NON-NLS-1$
	//
	private static final String		PARAMS_MONITOR			= "Monitor";										//$NON-NLS-1$
	private static final String		PARAMS_VERSION			= "Version";										//$NON-NLS-1$
	private static final String		PARAMS_S_MODE			= "SMode";											//$NON-NLS-1$
	private static final String		PARAMS_DATE				= "Date";											//$NON-NLS-1$
	private static final String		PARAMS_START_TIME		= "StartTime";										//$NON-NLS-1$
	private static final String		PARAMS_LENGTH			= "Length";										//$NON-NLS-1$
	private static final String		PARAMS_INTERVAL			= "Interval";										//$NON-NLS-1$
	private static final String		PARAMS_UPPER1			= "Upper1";										//$NON-NLS-1$
	private static final String		PARAMS_LOWER1			= "Lower1";										//$NON-NLS-1$
	private static final String		PARAMS_UPPER2			= "Upper2";										//$NON-NLS-1$
	private static final String		PARAMS_LOWER2			= "Lower2";										//$NON-NLS-1$
	private static final String		PARAMS_UPPER3			= "Upper3";										//$NON-NLS-1$
	private static final String		PARAMS_LOWER3			= "Lower3";										//$NON-NLS-1$
	private static final String		PARAMS_TIMER1			= "Timer1";										//$NON-NLS-1$
	private static final String		PARAMS_TIMER2			= "Timer2";										//$NON-NLS-1$
	private static final String		PARAMS_TIMER3			= "Timer3";										//$NON-NLS-1$
	private static final String		PARAMS_ACTIVE_LIMIT		= "ActiveLimit";									//$NON-NLS-1$
	private static final String		PARAMS_MAX_HR			= "MaxHR";											//$NON-NLS-1$
	private static final String		PARAMS_REST_HR			= "RestHR";										//$NON-NLS-1$
	private static final String		PARAMS_START_DELAY		= "StartDelay";									//$NON-NLS-1$
	private static final String		PARAMS_VO2MAX			= "VO2max";										//$NON-NLS-1$
	private static final String		PARAMS_WEIGHT			= "Weight";										//$NON-NLS-1$
	//
	private DeviceData				_deviceData;
	private String					_importFilePath;
	private long					_lastUsedImportId;
	private int						_hrmVersion				= -1;

	private SectionParams			_sectionParams;
	private SectionTrip				_sectionTrip;
	private ArrayList<LapData>		_sectionLapData			= new ArrayList<PolarHRMDataReader.LapData>();
	private ArrayList<LapNotes>		_sectionLapNotes		= new ArrayList<PolarHRMDataReader.LapNotes>();
	private ArrayList<HRDataSlice>	_sectionHRData			= new ArrayList<PolarHRMDataReader.HRDataSlice>();
	//
	private boolean					_isDebug				= false;

	/**
	 * <pre>
	 * 
	 * Heart rate monitor type
	 * 
	 *  1 = Polar Sport Tester / Vantage XL
	 *  2 = Polar Vantage NV (VNV)
	 *  3 = Polar Accurex Plus
	 *  4 = Polar XTrainer Plus
	 *  6 = Polar S520
	 *  7 = Polar Coach
	 *  8 = Polar S210
	 *  9 = Polar S410
	 * 10 = Polar S510
	 * 11 = Polar S610 / S610i
	 * 12 = Polar S710 / S710i / S720i
	 * 13 = Polar S810 / S810i
	 * 15 = Polar E600
	 * 20 = Polar AXN500
	 * 21 = Polar AXN700
	 * 22 = Polar S625X / S725X
	 * 23 = Polar S725
	 * 33 = Polar CS400
	 * 34 = Polar CS600X
	 * 35 = Polar CS600
	 * 36 = Polar RS400
	 * 37 = Polar RS800
	 * 38 = Polar RS800X
	 * 
	 * </pre>
	 */
	private final PolarDevice[]		_polarDevices;

	{
		_polarDevices = new PolarDevice[] { //
		//
			new PolarDevice(1, "Polar Sport Tester / Vantage XL"), //$NON-NLS-1$
			new PolarDevice(2, "Polar Vantage NV (VNV)"), //$NON-NLS-1$
			new PolarDevice(3, "Polar Accurex Plus"), //$NON-NLS-1$
			new PolarDevice(4, "Polar XTrainer Plus"), //$NON-NLS-1$
			new PolarDevice(6, "Polar S520"), //$NON-NLS-1$
			new PolarDevice(7, "Polar Coach"), //$NON-NLS-1$
			new PolarDevice(8, "Polar S210"), //$NON-NLS-1$
			new PolarDevice(9, "Polar S410"), //$NON-NLS-1$
			new PolarDevice(10, "Polar S510"), //$NON-NLS-1$
			new PolarDevice(11, "Polar S610 / S610i"), //$NON-NLS-1$
			new PolarDevice(12, "Polar S710 / S710i / S720i"), //$NON-NLS-1$
			new PolarDevice(13, "Polar S810 / S810i"), //$NON-NLS-1$
			new PolarDevice(15, "Polar E600"), //$NON-NLS-1$
			new PolarDevice(20, "Polar AXN500"), //$NON-NLS-1$
			new PolarDevice(21, "Polar AXN700"), //$NON-NLS-1$
			new PolarDevice(22, "Polar S625X / S725X"), //$NON-NLS-1$
			new PolarDevice(23, "Polar S725"), //$NON-NLS-1$
			new PolarDevice(33, "Polar CS400"), //$NON-NLS-1$
			new PolarDevice(34, "Polar CS600X"), //$NON-NLS-1$
			new PolarDevice(35, "Polar CS600"), //$NON-NLS-1$
			new PolarDevice(36, "Polar RS400"), //$NON-NLS-1$
			new PolarDevice(37, "Polar RS800"), //$NON-NLS-1$
			new PolarDevice(38, "Polar RS800X"), //$NON-NLS-1$
		//
		};
	}

	private class HRDataSlice {

		private int	pulse		= Integer.MIN_VALUE;
		private int	speed		= Integer.MIN_VALUE;
		private int	cadence		= Integer.MIN_VALUE;
		private int	altitude	= Integer.MIN_VALUE;
		private int	power		= Integer.MIN_VALUE;
	}

	private class LapData extends HRDataSlice {

		/**
		 * Relative time of the lap in seconds
		 */
		private int	time;

		/**
		 * Temperature in metric or imperial value
		 */
		private int	temperature;

		@Override
		public String toString() {

			final StringBuilder sb = new StringBuilder();

			sb.append("IntTimes (LapTimes):\n"); //$NON-NLS-1$

			sb.append("\ttime:\t"); //$NON-NLS-1$
			sb.append(time);
			sb.append(UI.NEW_LINE);

			return sb.toString();
		}
	}

	private class LapNotes extends LapData {

		private int		lapNo	= Integer.MIN_VALUE;
		private String	noteText;

		@Override
		public String toString() {

			final StringBuilder sb = new StringBuilder();

			sb.append("IntNotes (Lap Notes):\n"); //$NON-NLS-1$

			sb.append("\tlapNo:\t"); //$NON-NLS-1$
			sb.append(lapNo);
			sb.append(UI.NEW_LINE);

			sb.append("\tnoteText:\t"); //$NON-NLS-1$
			sb.append(noteText);
			sb.append(UI.NEW_LINE);

			return sb.toString();
		}
	}

	private class PolarDevice {

		private int		_deviceNo;
		private String	_deviceName;

		public PolarDevice(final int deviceNo, final String deviceName) {
			_deviceNo = deviceNo;
			_deviceName = deviceName;
		}
	}

	private class SectionParams {

		private int		version					= Integer.MIN_VALUE;
		private int		monitor					= Integer.MIN_VALUE;
		private int		interval				= Integer.MIN_VALUE;
		private int		restHR					= Integer.MIN_VALUE;

		private int		startYear				= Integer.MIN_VALUE;
		private int		startMonth				= Integer.MIN_VALUE;
		private int		startDay				= Integer.MIN_VALUE;
		private int		startHour				= Integer.MIN_VALUE;
		private int		startMinute				= Integer.MIN_VALUE;
		private int		startSecond				= Integer.MIN_VALUE;

		private String	sMode;
		private boolean	isSpeed					= false;
		private boolean	isCadence				= false;
		private boolean	isAltitude				= false;
		private boolean	isPower					= false;
		private boolean	isPowerLeftRightBalance	= false;
		private boolean	isPowerPedallingIndex	= false;
		private boolean	isHRAndCycling			= false;
		private boolean	isUSUnit				= false;
		private boolean	isAirPressure			= false;

		// mytourbook specific fields
		private int		mtInterval;
		private String	monitorName;

		@Override
		public String toString() {

			final StringBuilder sb = new StringBuilder();

			sb.append("Params:\n"); //$NON-NLS-1$

			sb.append("\tversion:\t"); //$NON-NLS-1$
			sb.append(version);
			sb.append(UI.NEW_LINE);

			sb.append("\tmonitor:\t"); //$NON-NLS-1$
			sb.append(monitor);
			sb.append(UI.NEW_LINE);

			sb.append("\tinterval:\t"); //$NON-NLS-1$
			sb.append(interval);
			sb.append(UI.NEW_LINE);

			sb.append("\tRestHR:\t\t"); //$NON-NLS-1$
			sb.append(restHR);
			sb.append(UI.NEW_LINE);

			sb.append("\tStartYear:\t"); //$NON-NLS-1$
			sb.append(startYear);
			sb.append(UI.NEW_LINE);

			sb.append("\tStartMonth:\t"); //$NON-NLS-1$
			sb.append(startMonth);
			sb.append(UI.NEW_LINE);

			sb.append("\tStartDay:\t"); //$NON-NLS-1$
			sb.append(startDay);
			sb.append(UI.NEW_LINE);

			sb.append("\tStartHour:\t"); //$NON-NLS-1$
			sb.append(startHour);
			sb.append(UI.NEW_LINE);

			sb.append("\tStartMinute:\t"); //$NON-NLS-1$
			sb.append(startMinute);
			sb.append(UI.NEW_LINE);

			sb.append("\tStartSecond:\t"); //$NON-NLS-1$
			sb.append(startSecond);
			sb.append(UI.NEW_LINE);

			/*
			 * SMode
			 */
			{
				sb.append("SMode:\n"); //$NON-NLS-1$

				sb.append("\tisSpeed\t\t"); //$NON-NLS-1$
				sb.append(_sectionParams.isSpeed);
				sb.append(UI.NEW_LINE);

				sb.append("\tisCadence\t"); //$NON-NLS-1$
				sb.append(_sectionParams.isCadence);
				sb.append(UI.NEW_LINE);

				sb.append("\tisAltitude\t"); //$NON-NLS-1$
				sb.append(_sectionParams.isAltitude);
				sb.append(UI.NEW_LINE);

				sb.append("\tisPower\t\t"); //$NON-NLS-1$
				sb.append(_sectionParams.isPower);
				sb.append(UI.NEW_LINE);

				sb.append("\tisPowerLR\t"); //$NON-NLS-1$
				sb.append(_sectionParams.isPowerLeftRightBalance);
				sb.append(UI.NEW_LINE);

				sb.append("\tisPowerPed\t"); //$NON-NLS-1$
				sb.append(_sectionParams.isPowerPedallingIndex);
				sb.append(UI.NEW_LINE);

				sb.append("\tisHRAndCycle\t"); //$NON-NLS-1$
				sb.append(_sectionParams.isHRAndCycling);
				sb.append(UI.NEW_LINE);

				sb.append("\tisUSUnit\t"); //$NON-NLS-1$
				sb.append(_sectionParams.isUSUnit);
				sb.append(UI.NEW_LINE);

				sb.append("\tisAirPressure\t"); //$NON-NLS-1$
				sb.append(_sectionParams.isAirPressure);
				sb.append(UI.NEW_LINE);
			}

			return sb.toString();
		}
	}

	public class SectionTrip {

		private int	distance		= Integer.MIN_VALUE;
		private int	ascent			= Integer.MIN_VALUE;
		private int	totalTime		= Integer.MIN_VALUE;
		private int	avgAlititude	= Integer.MIN_VALUE;
		private int	maxAltitude		= Integer.MIN_VALUE;
		private int	avgSpeed		= Integer.MIN_VALUE;
		private int	maxSpeed		= Integer.MIN_VALUE;
		private int	odometer		= Integer.MIN_VALUE;

		@Override
		public String toString() {

			final StringBuilder sb = new StringBuilder();

			sb.append("Trip:\n"); //$NON-NLS-1$

			sb.append("\tdistance:\t"); //$NON-NLS-1$
			sb.append(distance);
			sb.append(UI.NEW_LINE);

			sb.append("\tascent:\t\t"); //$NON-NLS-1$
			sb.append(ascent);
			sb.append(UI.NEW_LINE);

			sb.append("\ttotalTime:\t"); //$NON-NLS-1$
			sb.append(totalTime);
			sb.append(UI.NEW_LINE);

			sb.append("\tavgAlititude:\t"); //$NON-NLS-1$
			sb.append(avgAlititude);
			sb.append(UI.NEW_LINE);

			sb.append("\tmaxAltitude:\t"); //$NON-NLS-1$
			sb.append(maxAltitude);
			sb.append(UI.NEW_LINE);

			sb.append("\tavgSpeed:\t"); //$NON-NLS-1$
			sb.append(avgSpeed);
			sb.append(UI.NEW_LINE);

			sb.append("\tmaxSpeed:\t"); //$NON-NLS-1$
			sb.append(maxSpeed);
			sb.append(UI.NEW_LINE);

			sb.append("\todometer:\t"); //$NON-NLS-1$
			sb.append(odometer);
			sb.append(UI.NEW_LINE);

			return sb.toString();
		}
	}

	// plugin constructor
	public PolarHRMDataReader() {}

	@Override
	public String buildFileNameFromRawData(final String rawDataFileName) {
		return null;
	}

	@Override
	public boolean checkStartSequence(final int byteIndex, final int newByte) {
		return false;
	}

	private void cleanup() {

		_hrmVersion = -1;

		if (_sectionHRData != null) {
			_sectionHRData.clear();
		}

		if (_sectionLapData != null) {
			_sectionLapData.clear();
		}

		if (_sectionLapNotes != null) {
			_sectionLapNotes.clear();
		}
	}

	private void createTourData(final HashMap<Long, TourData> alreadyImportedTours,
								final HashMap<Long, TourData> newlyImportedTours) {

		// create data object for each tour
		final TourData tourData = new TourData();

		/*
		 * set tour start date/time
		 */
		final DateTime dtTourStart = new DateTime(
				_sectionParams.startYear,
				_sectionParams.startMonth,
				_sectionParams.startDay,
				_sectionParams.startHour,
				_sectionParams.startMinute,
				_sectionParams.startSecond,
				0);

		tourData.setStartHour((short) dtTourStart.getHourOfDay());
		tourData.setStartMinute((short) dtTourStart.getMinuteOfHour());
		tourData.setStartSecond((short) dtTourStart.getSecondOfMinute());

		tourData.setStartYear((short) dtTourStart.getYear());
		tourData.setStartMonth((short) dtTourStart.getMonthOfYear());
		tourData.setStartDay((short) dtTourStart.getDayOfMonth());

		tourData.setWeek(dtTourStart);

		tourData.setDeviceTimeInterval((short) _sectionParams.mtInterval);

		tourData.importRawDataFile = _importFilePath;
		tourData.setTourImportFilePath(_importFilePath);

//		tourData.setCalories(_calories);
		tourData.setRestPulse(_sectionParams.restHR == Integer.MIN_VALUE ? 0 : _sectionParams.restHR);

		if (_sectionTrip != null) {
			tourData.setStartDistance(_sectionTrip.odometer == Integer.MIN_VALUE ? 0 : _sectionTrip.odometer);
		}

		final ArrayList<TimeData> timeSeries = createTourData10CreateTimeSeries(dtTourStart);
		createTourData20SetTemperature(tourData, timeSeries);

		tourData.createTimeSeries(timeSeries, true);

		createTourData30CreateMarkers(tourData);
		tourData.computeAltitudeUpDown();

		// after all data are added, the tour id can be created
		final Long tourId = tourData.createTourId(createUniqueId(tourData, Util.UNIQUE_ID_SUFFIX_POLAR_HRM));

		// check if the tour is already imported
		if (alreadyImportedTours.containsKey(tourId) == false) {

			tourData.computeTourDrivingTime();
			tourData.computeComputedValues();

			tourData.setDeviceId(deviceId);
			tourData.setDeviceName(_sectionParams.monitorName);
			tourData.setDeviceFirmwareVersion(Integer.toString(_hrmVersion));

			// add new tour to other tours
			newlyImportedTours.put(tourId, tourData);
		}
	}

	/**
	 * Converts {@link HRDataSlice} into {@link TimeData}
	 * 
	 * @param dtTourStart
	 * @return
	 */
	private ArrayList<TimeData> createTourData10CreateTimeSeries(final DateTime dtTourStart) {

		final boolean isImperial = _sectionParams.isUSUnit;
		final int sliceTimeInterval = _sectionParams.interval;

		int relativeTime = 0;
		float absoluteDistance = 0;

		final ArrayList<TimeData> timeDataList = new ArrayList<TimeData>();

		for (final HRDataSlice hrSlice : _sectionHRData) {

			final TimeData tourSlice = new TimeData();

			tourSlice.relativeTime = relativeTime;
			tourSlice.absoluteTime = dtTourStart.plusSeconds(relativeTime).getMillis();

			if (hrSlice.pulse != Integer.MIN_VALUE) {
				tourSlice.pulse = hrSlice.pulse;
			}

			if (hrSlice.speed != Integer.MIN_VALUE) {

				// convert speed into distance, speed is computed internally and not saved

				final float speed = (isImperial ? UI.UNIT_MILE : 1) * hrSlice.speed / SPEED_SCALING * 1000 / 3600;

				final float distanceDiff = speed * sliceTimeInterval;

				absoluteDistance += distanceDiff;

				tourSlice.absoluteDistance = absoluteDistance;
			}

			if (hrSlice.altitude != Integer.MIN_VALUE) {
				tourSlice.absoluteAltitude = hrSlice.altitude / (isImperial ? UI.UNIT_FOOT : 1);
			}

			if (hrSlice.cadence != Integer.MIN_VALUE) {
				tourSlice.cadence = hrSlice.cadence;
			}

			timeDataList.add(tourSlice);

			relativeTime += sliceTimeInterval;
		}

		return timeDataList;
	}

	private void createTourData20SetTemperature(final TourData tourData, final ArrayList<TimeData> timeSeries) {

		if (_sectionLapData.size() == 0) {
			return;
		}

		final boolean isImperial = _sectionParams.isUSUnit;

		final TimeData[] timeSlices = timeSeries.toArray(new TimeData[timeSeries.size()]);
		int serieIndex = 0;

		for (final LapData lapData : _sectionLapData) {

			final int lapRelativeTime = lapData.time;

			for (; serieIndex < timeSlices.length; serieIndex++) {

				final TimeData currentTimeSlice = timeSlices[serieIndex];

				// check if time is within current lap
				if (currentTimeSlice.relativeTime > lapRelativeTime) {
					break;
				}

				// temperature is scaled by 10 in the raw data
				float metricTemperature = (float) lapData.temperature / 10;

				if (isImperial) {
					metricTemperature = metricTemperature * UI.UNIT_FAHRENHEIT_MULTI + UI.UNIT_FAHRENHEIT_ADD;
				}

				currentTimeSlice.temperature = metricTemperature;
			}
		}
	}

	/**
	 * Create a marker for each lap, the markers are currently numbered 1...n
	 * 
	 * @param tourData
	 */
	private void createTourData30CreateMarkers(final TourData tourData) {

		if (_sectionLapData.size() == 0) {
			return;
		}

		final int[] timeSerie = tourData.timeSerie;
		if (timeSerie.length == 0) {
			return;
		}

		final Set<TourMarker> tourMarkers = tourData.getTourMarkers();
		final float[] distanceSerie = tourData.distanceSerie;

		int lapCounter = 1;

		for (final LapData lapData : _sectionLapData) {

			final int lapRelativeTime = lapData.time;
			int serieIndex = 0;

			// get serie index
			for (final int relativeTime : timeSerie) {
				if (relativeTime >= lapRelativeTime) {
					break;
				}
				serieIndex++;
			}

			// check array bounds
			if (serieIndex >= timeSerie.length) {
				serieIndex = timeSerie.length - 1;
			}

			// get marker text from lap notes
			String markerText = null;
			for (final LapNotes lapNote : _sectionLapNotes) {

				final String lapText = lapNote.noteText;

				if (lapNote.lapNo == lapCounter && lapText != null && lapText.length() > 0) {
					markerText = lapText;
				}
			}

			if (markerText == null) {
				// set lap number as label when label is not definded in the polar data
				markerText = Integer.toString(lapCounter);
			}

			final TourMarker tourMarker = new TourMarker(tourData, ChartLabel.MARKER_TYPE_DEVICE);
			tourMarker.setLabel(markerText);
			tourMarker.setVisualPosition(ChartLabel.VISUAL_HORIZONTAL_ABOVE_GRAPH_CENTERED);

			tourMarker.setSerieIndex(serieIndex);
			tourMarker.setTime(lapRelativeTime);

			if (distanceSerie != null) {
				tourMarker.setDistance(distanceSerie[serieIndex]);
			}

			tourMarkers.add(tourMarker);

			lapCounter++;
		}
	}

	public String getDeviceModeName(final int profileId) {
		return null;
	}

	private String getMonitorName(final int monitor) {

		for (final PolarDevice device : _polarDevices) {
			if (device._deviceNo == monitor) {
				return device._deviceName;
			}
		}

		return visibleName + UI.SPACE + Messages.Import_Error_DeviceNameIsUnknown;
	}

	@Override
	public SerialParameters getPortParameters(final String portName) {
		return null;
	}

	@Override
	public int getStartSequenceSize() {
		return -1;
	}

	public int getTransferDataSize() {
		return -1;
	}

	/**
	 * <pre>
	 * 
	 * Date = 20040831  Date of exercise (yyyymmdd)
	 * 		  01234567
	 * 
	 * For example 20040831 means 31 st  August 2004)
	 * 
	 * </pre>
	 * 
	 * @param value
	 */
	private void parseFieldDate(final String value) {

		final byte[] dataBytes = value.getBytes();
		final int bytesLength = dataBytes.length;

		if (bytesLength < 8) {
			return;
		}

		_sectionParams.startYear = Integer.parseInt(value.substring(0, 4));
		_sectionParams.startMonth = Integer.parseInt(value.substring(4, 6));
		_sectionParams.startDay = Integer.parseInt(value.substring(6, 8));
	}

	/**
	 * <pre>
	 * 
	 * SMode = 11011010
	 * 		  (abcdefgh)	With versions 1.06
	 * 
	 * SMode = 110110100
	 * 		  (abcdefghi)	With versions 1.07
	 * 
	 * Data type parameters
	 * 
	 * a)  Speed						(0=off, 1=on)
	 * b)  Cadence						(0=off, 1=on)
	 * c)  Altitude						(0=off, 1=on)
	 * d)  Power						(0=off, 1=on)
	 * e)  Power Left Right Balance 	(0=off, 1=on)
	 * f)  Power Pedalling Index 		(0=off, 1=on)
	 * g)  HR/CC data
	 * 			0 = HR data only,
	 * 			1 = HR + cycling data
	 * h)  US / Euro unit
	 * 			0 = Euro (km, km/h, m, °C)
	 * 			1 = US (miles, mph, ft, °F)
	 * 
	 * All distance, speed, altitude and temperature values depend on US/Euro unit
	 * selection (km / miles, km/h / mph, m / ft, °C / °F).
	 * 
	 * i)  Air pressure (0=off, 1=on)
	 * 
	 * </pre>
	 * 
	 * @param dataType
	 */
	private void parseFieldSMode(final String dataType) {

		_sectionParams.sMode = dataType;

		final byte[] dataBytes = dataType.getBytes();
		final int bytesLength = dataBytes.length;

		if (bytesLength > 0) {
			_sectionParams.isSpeed = dataBytes[0] == '1';
		}

		if (bytesLength > 1) {
			_sectionParams.isCadence = dataBytes[1] == '1';
		}

		if (bytesLength > 2) {
			_sectionParams.isAltitude = dataBytes[2] == '1';
		}

		if (bytesLength > 3) {
			_sectionParams.isPower = dataBytes[3] == '1';
		}

		if (bytesLength > 4) {
			_sectionParams.isPowerLeftRightBalance = dataBytes[4] == '1';
		}

		if (bytesLength > 5) {
			_sectionParams.isPowerPedallingIndex = dataBytes[5] == '1';
		}

		if (bytesLength > 6) {
			_sectionParams.isHRAndCycling = dataBytes[6] == '1';
		}

		if (bytesLength > 7) {
			_sectionParams.isUSUnit = dataBytes[7] == '1';
		}

		if (bytesLength > 8) {
			_sectionParams.isAirPressure = dataBytes[8] == '1';
		}
	}

	/**
	 * <pre>
	 * 
	 * StartTime=14:23:36.0          Start time (hh:mm:ss.d)
	 * 
	 * hh:mm:ss.d	h:mm:ss.d
	 * 0123456789	012345678
	 * 
	 * If hours are less than 10, format h:mm:ss.d have also been used. Check time format by checking : character.
	 * 
	 * </pre>
	 * 
	 * @param value
	 */
	private void parseFieldStartTime(final String value) {

		final byte[] dataBytes = value.getBytes();
		final int bytesLength = dataBytes.length;

		if (bytesLength < 9) {
			return;
		}

		final int offset = dataBytes[1] == ':' ? 0 : 1;

		_sectionParams.startHour = offset == 0 //
				? Integer.parseInt(value.substring(0, 1))
				: Integer.parseInt(value.substring(0, 2));

		_sectionParams.startMinute = Integer.parseInt(value.substring(offset + 2, offset + 4));
		_sectionParams.startSecond = Integer.parseInt(value.substring(offset + 5, offset + 7));
	}

	private boolean parseSection(	final String importFileName,
									final DeviceData deviceData,
									final HashMap<Long, TourData> alreadyImportedTours,
									final HashMap<Long, TourData> newlyImportedTours) {

		boolean returnValue = false;

		BufferedReader fileReader = null;

		try {

			String line;
			fileReader = new BufferedReader(new FileReader(importFileName));

			while ((line = fileReader.readLine()) != null) {

				boolean isValid = true;

				if (line.startsWith(SECTION_PARAMS)) {

					isValid = parseSection10Params(fileReader, deviceData);

				} else if (line.startsWith(SECTION_NOTE)) {

					// is not yet supported

				} else if (line.startsWith(SECTION_INT_TIMES)) {

					if (_hrmVersion == 106) {
						isValid = parseSection20LapData106(fileReader);
					}

				} else if (line.startsWith(SECTION_INT_NOTES)) {

					isValid = parseSection21LapNotes(fileReader);

				} else if (line.startsWith(SECTION_EXTRA_DATA)) {

					// is not yet supported

				} else if (line.startsWith(SECTION_LAP_NAMES)) {

					// is not yet supported

				} else if (line.startsWith(SECTION_SUMMARY_123)) {

					// is not yet supported

				} else if (line.startsWith(SECTION_SUMMARY_TH)) {

					// is not yet supported

				} else if (line.startsWith(SECTION_HR_ZONES)) {

					// is not yet supported

				} else if (line.startsWith(SECTION_SWAP_TIMES)) {

					// is not yet supported

				} else if (line.startsWith(SECTION_TRIP)) {

					isValid = parseSection80Trip(fileReader);

				} else if (line.startsWith(SECTION_HR_DATA)) {

					if (_hrmVersion == 106) {
						isValid = parseSection90HRData106(fileReader);
					}
				}

				if (isValid == false) {
					return false;
				}
			}

			if (validateData() == false) {
				return false;
			}

			createTourData(alreadyImportedTours, newlyImportedTours);

			returnValue = true;

		} catch (final Exception e) {
			StatusUtil.showStatus(e);
			return false;
		} finally {

			cleanup();

			try {
				if (fileReader != null) {
					fileReader.close();
				}
			} catch (final IOException e) {
				StatusUtil.showStatus(e);
				return false;
			}
		}
		return returnValue;
	}

	private boolean parseSection10Params(final BufferedReader fileReader, final DeviceData deviceData)
			throws IOException {

		_sectionParams = new SectionParams();

		String line;

		// read section
		while ((line = fileReader.readLine()) != null) {

			// check if section has ended
			if (line.length() == 0 || line.startsWith(SECTION_START_CHARACTER)) {
				break;
			}

			final StringTokenizer tokenLine = new StringTokenizer(line, "="); //$NON-NLS-1$

			try {

				final String key = tokenLine.nextToken();
				final String value = tokenLine.nextToken();

				if (key.equals(PARAMS_VERSION)) {

					// Version=106
					_sectionParams.version = Integer.parseInt(value);
					_hrmVersion = _sectionParams.version;

				} else if (key.equals(PARAMS_MONITOR)) {

					// Monitor=22
					final int monitor = Integer.parseInt(value);
					_sectionParams.monitor = monitor;
					_sectionParams.monitorName = getMonitorName(monitor);

				} else if (key.equals(PARAMS_S_MODE)) {

					// SMode=101000100
					parseFieldSMode(value);

				} else if (key.equals(PARAMS_DATE)) {

					// Date=20080227
					parseFieldDate(value);

				} else if (key.equals(PARAMS_START_TIME)) {

					// StartTime=15:16:19.0
					parseFieldStartTime(value);

				} else if (key.equals(PARAMS_LENGTH)) {
					// Length=01:48:49.7
				} else if (key.equals(PARAMS_INTERVAL)) {

					// Interval=5
					_sectionParams.interval = Integer.parseInt(value);

				} else if (key.equals(PARAMS_UPPER1)) {
					// Upper1=0
				} else if (key.equals(PARAMS_LOWER1)) {
					// Lower1=0
				} else if (key.equals(PARAMS_UPPER2)) {
					// Upper2=0
				} else if (key.equals(PARAMS_LOWER2)) {
					// Lower2=0
				} else if (key.equals(PARAMS_UPPER3)) {
					// Upper3=0
				} else if (key.equals(PARAMS_LOWER3)) {
					// Lower3=0
				} else if (key.equals(PARAMS_TIMER1)) {
					// Timer1=00:00:00.0
				} else if (key.equals(PARAMS_TIMER2)) {
					// Timer2=00:00:00.0
				} else if (key.equals(PARAMS_TIMER3)) {
					// Timer3=00:00:00.0
				} else if (key.equals(PARAMS_ACTIVE_LIMIT)) {
					// ActiveLimit=0
				} else if (key.equals(PARAMS_MAX_HR)) {
					// MaxHR=200
				} else if (key.equals(PARAMS_REST_HR)) {

					// RestHR=60
					_sectionParams.restHR = Integer.parseInt(value);

				} else if (key.equals(PARAMS_START_DELAY)) {
					// StartDelay=0
				} else if (key.equals(PARAMS_VO2MAX)) {
					// VO2max=50
				} else if (key.equals(PARAMS_WEIGHT)) {
					// Weight=70
				}

			} catch (final NumberFormatException e) {
				// this should not happen, it's just ignored -> value is not set
			} catch (final NoSuchElementException e) {
				// this should not happen, it's just ignored -> value is not set
			}
		}

		if (_isDebug) {
			System.out.println(_sectionParams.toString());
		}

		return true;
	}

	/**
	 * <pre>
	 * 
	 * 8.  Lap Times
	 * 
	 * DATA  COMMENTS
	 * 
	 * [IntTimes]                  							Lap times
	 * 
	 * 00:03:43.7   123     100     150		200     		Row 1
	 * 32           0		0		0       0  		0		Row 2       Lap time 0
	 * 0  			0       0  		0       0    			Row 3
	 * 0 			400     455     21      0  		0		Row 4 #
	 * 0			0		0		0       0		0		Row 5 #
	 * 
	 * 00:04:54.7   159     130     170     200       		Row 1
	 * 32           0       0  		0       0  		0  		Row 2       Lap time 1
	 * 0  			0       0  		0       0    			Row 3
	 * 0  			400     470     21      0  		0  		Row 4 #
	 * 0  			0       0  		0       0  		0  		Row 5 #
	 * 
	 * Field descriptions:
	 * 
	 * [IntTimes]   										Lap times
	 * Time  		HR     	HR      HR      HR          	Row 1
	 * 						min     avg     max
	 * 
	 * Flags        Rec.	Rec.	Speed   Cad		Alt		Row 2
	 * 				Time	HR
	 * 
	 * Extra1       Extra2  Extra3  Asc		Dist            Row 3
	 * 
	 * Lap type     Lap		Power   Tempe	Phas	Air		Row 4 #
	 * 				Dist			rature	eLap	Pr
	 * 
	 * StrideAvg   	Autom.	0  		0  		0               Row 5 #
	 * 				lap
	 * 
	 * 
	 * Row 1
	 * Time         Lap time in format hh:mm:ss.d
	 * HR           Momentary heart rate value in bpm
	 * HR min       Lap’s minimum heart rate value in bpm
	 * HR avg       Lap’s average heart rate value in bpm
	 * HR max       Lap’s maximum heart rate value in bpm
	 * 
	 * Row 2
	 * Flags        Misc lap time information in 8 bits, 87654321
	 * 				bit 8 = Polar Coach lap/interval flag (0 = lap, 1 = interval)
	 *              bit 7 = Int. time erased (for Conconi test, not included to calculation)
	 *              bit 6 = Int. type (0 = fixed, 1 = from hrm)
	 *              bit 5 = Extra data 3 (1 = selected to draw)
	 *              bit 4 = Extra data 2 (1 = selected to draw)
	 *              bit 3 = Extra data 1 (1 = selected to draw)
	 *              bits 1,2 = Recovery (0 = no rec, 1 = Time rec, 2 = HR rec)
	 * Rec. Time    Recovery time (seconds)
	 * Rec. HR      Recovery HR (bpm)
	 * Speed        Momentary speed in Xtrainer units (km/h or mph = X/128)
	 * Cad          Momentary cadence (rpm)
	 * Alt          Momentary altitude (HRM version 1.02: 10m / 10ft, version 1.05 1m/1ft)*
	 * 
	 * Row 3
	 * Extra 1 - 3 	Values of extra data series (0 - 3000) (the actual value is multiplied by ten)
	 * Asc          Lap ascent value from XTr+ 10m / 10ft
	 * Dist         Lap distance value from XTr+ 0.1km / 0.1ft
	 * 
	 * Row 4 #
	 * Lap type     Lap type identifier, replaces flag 8 (Polar Coach lap/interval flag) value
	 * 
	 * 				Type    Description  			Type  		Description
	 * 
	 *              0  		normal lap  			8192  		end of exercise
	 *              1  		interval  				16384       off road
	 *              2  		start of exercise  		32768       road
	 *              4  		finishing line  		65536       head wind
	 *              8  		uphill  				131072      tail wind
	 *              16  	downhill  				262144      Score / goal
	 *              32  	service  				524288      penalty
	 *              64  	stopped  				1048576     city/down
	 *              128     orienteering marker  	2097152     navigation
	 *              256     u-turn  				4194304     altitude calibration
	 *              512     summit / peak  			8388608     crossroads
	 *              1024    sprint  				16777216	landmark
	 *              2048    crash
	 *              4096    timeout
	 * 
	 * 
	 * Lap Dist     Manually given lap distance in meters / yards, units are depending on
	 * 				US/Euro unit selection
	 * Power        Momentary power value in Watts
	 * Temperature	Momentary temperature value in Celcius / Fahrenheit, units are depending
	 * 				on US/Euro unit selection
	 * PhaseLap     Internal phase/lap information used for interval calculation
	 * AirPr        Air pressure value from AXN products
	 * 
	 * Row 5 #
	 * StrideAvg    Stride average in cm (RS800, RS800CX only)
	 * Autom.lap    Automatic lap used (TRUE/FALSE) (RS and CS products)
	 * 
	 * The rest of the new lap time parameters are reserved for future usage.
	 * Lap times were formerly known as Intermediate times.
	 * 
	 * 
	 * </pre>
	 * 
	 * @param fileReader
	 * @return
	 * @throws IOException
	 */
	private boolean parseSection20LapData106(final BufferedReader fileReader) throws IOException {

		String line;

		while ((line = fileReader.readLine()) != null) {

			// check if section has ended
			if (line.length() == 0 || line.startsWith(SECTION_START_CHARACTER)) {
				break;
			}

			final LapData lapData = new LapData();

			try {

				StringTokenizer tokenLine = new StringTokenizer(line, DATA_DELIMITER);

				// 1: time
				String token = tokenLine.nextToken();

				// 2:
				// final int timeHour = Integer.parseInt(token.substring(0, hourLength));

				// 3:
				// final int timeMin = Integer.parseInt(token.substring(hourLength + 1, hourLength + 3));

				// 4:
				// final int timeSec = Integer.parseInt(token.substring(hourLength + 4, hourLength + 6));

				// HRM Files exported by polarpersonaltrainer.com can contain lap times with
				// one digit hours and/or minutes: e.g. 9:5:45.0 (9 hours, 5 min, 45.0 sec)

				final String timeString = token.substring(0, token.indexOf(".")); //$NON-NLS-1$
				final StringTokenizer timeTokens = new StringTokenizer(timeString, ":"); //$NON-NLS-1$

				final int timeHour = Integer.parseInt(timeTokens.nextToken());
				final int timeMin = Integer.parseInt(timeTokens.nextToken());
				final int timeSec = Integer.parseInt(timeTokens.nextToken());

				lapData.time = timeHour * 3600 + timeMin * 60 + timeSec;

// not yet used
//				lapData.hr = Integer.parseInt(tokenLine.nextToken());
//				lapData.hrMin = Integer.parseInt(tokenLine.nextToken());
//				lapData.hrAvg = Integer.parseInt(tokenLine.nextToken());
//				lapData.hrMax = Integer.parseInt(tokenLine.nextToken());

				/**
				 * <pre>
				 * 
				 * Row 2
				 * 
				 * Flags        Misc lap time information in 8 bits, 87654321
				 * 				bit 8 = Polar Coach lap/interval flag (0 = lap, 1 = interval)
				 *              bit 7 = Int. time erased (for Conconi test, not included to calculation)
				 *              bit 6 = Int. type (0 = fixed, 1 = from hrm)
				 *              bit 5 = Extra data 3 (1 = selected to draw)
				 *              bit 4 = Extra data 2 (1 = selected to draw)
				 *              bit 3 = Extra data 1 (1 = selected to draw)
				 *              bits 1,2 = Recovery (0 = no rec, 1 = Time rec, 2 = HR rec)
				 * Rec. Time    Recovery time (seconds)
				 * Rec. HR      Recovery HR (bpm)
				 * Speed        Momentary speed in Xtrainer units (km/h or mph = X/128)
				 * Cad          Momentary cadence (rpm)
				 * Alt          Momentary altitude (HRM version 1.02: 10m / 10ft, version 1.05 1m/1ft)*
				 * 
				 * 
				 * </pre>
				 */

				// next line
				line = fileReader.readLine();
				if (line == null || line.length() == 0 || line.startsWith(SECTION_START_CHARACTER)) {
					break;
				}

				/**
				 * <pre>
				 * 
				 * Row 3
				 * 
				 * Extra 1 - 3 	Values of extra data series (0 - 3000) (the actual value is multiplied by ten)
				 * Asc          Lap ascent value from XTr+ 10m / 10ft
				 * Dist         Lap distance value from XTr+ 0.1km / 0.1ft
				 * 
				 * </pre>
				 */

				// next line
				line = fileReader.readLine();
				if (line == null || line.length() == 0 || line.startsWith(SECTION_START_CHARACTER)) {
					break;
				}

				/**
				 * <pre>
				 * 
				 * Row 4 #
				 * 
				 * Lap type     Lap type identifier, replaces flag 8 (Polar Coach lap/interval flag) value
				 * 
				 * 				Type    Description  			Type  		Description
				 * 
				 *              0  		normal lap  			8192  		end of exercise
				 *              1  		interval  				16384       off road
				 *              2  		start of exercise  		32768       road
				 *              4  		finishing line  		65536       head wind
				 *              8  		uphill  				131072      tail wind
				 *              16  	downhill  				262144      Score / goal
				 *              32  	service  				524288      penalty
				 *              64  	stopped  				1048576     city/down
				 *              128     orienteering marker  	2097152     navigation
				 *              256     u-turn  				4194304     altitude calibration
				 *              512     summit / peak  			8388608     crossroads
				 *              1024    sprint  				16777216	landmark
				 *              2048    crash
				 *              4096    timeout
				 * 
				 * 
				 * Lap Dist     Manually given lap distance in meters / yards, units are depending on
				 * 				US/Euro unit selection
				 * Power        Momentary power value in Watts
				 * Temperature	Momentary temperature value in Celcius / Fahrenheit, units are depending
				 * 				on US/Euro unit selection
				 * PhaseLap     Internal phase/lap information used for interval calculation
				 * AirPr        Air pressure value from AXN products
				 * 
				 * </pre>
				 */

				// next line
				line = fileReader.readLine();
				if (line == null || line.length() == 0 || line.startsWith(SECTION_START_CHARACTER)) {
					break;
				}

				tokenLine = new StringTokenizer(line, DATA_DELIMITER);

				// 1: lap type
				token = tokenLine.nextToken();

				// 2: lap distance
				token = tokenLine.nextToken();

				// 3: power
				token = tokenLine.nextToken();

				// 4: temperature
				lapData.temperature = Integer.parseInt(tokenLine.nextToken());

				// 5: phase/lap
				token = tokenLine.nextToken();

				// 6: air pressure
				token = tokenLine.nextToken();

				/**
				 * <pre>
				 * 
				 * Row 5 #
				 * 
				 * StrideAvg    Stride average in cm (RS800, RS800CX only)
				 * Autom.lap    Automatic lap used (TRUE/FALSE) (RS and CS products)
				 * 
				 * </pre>
				 */

				// next line
				line = fileReader.readLine();
				if (line == null || line.length() == 0 || line.startsWith(SECTION_START_CHARACTER)) {
					break;
				}

				// keep lap data
				_sectionLapData.add(lapData);

			} catch (final Exception e) {
				StatusUtil.log(e);
				continue;
			}
		}

		if (_isDebug) {
			System.out.println(_sectionLapData.toString());
		}

		return true;
	}

	/**
	 * <pre>
	 * 
	 * 9.  Lap Time notes
	 * 
	 * [IntNotes]  	Intermediate time note texts
	 * 
	 * 3  			Traffic lights  Third intermediate time’s note text.
	 * 5  			Interval  Fifth intermediate time’s note text.
	 * </pre>
	 * 
	 * @param fileReader
	 * @return
	 * @throws IOException
	 */
	private boolean parseSection21LapNotes(final BufferedReader fileReader) throws IOException {

		String line;

		while ((line = fileReader.readLine()) != null) {

			// check if section has ended
			if (line.length() == 0 || line.startsWith(SECTION_START_CHARACTER)) {
				break;
			}

			final LapNotes lapNotes = new LapNotes();

			try {

				final StringTokenizer tokenLine = new StringTokenizer(line, DATA_DELIMITER);

				lapNotes.lapNo = Integer.parseInt(tokenLine.nextToken());
				lapNotes.noteText = tokenLine.nextToken();

				// keep lap notes
				_sectionLapNotes.add(lapNotes);

			} catch (final Exception e) {
				// ignore missing text
				continue;
			}
		}

		if (_isDebug) {
			System.out.println(_sectionLapNotes.toString());
		}

		return true;
	}

	/**
	 * <pre>
	 * 
	 * Cycling parameters are available from XTr+, S710, S710i, S720i, S725, S725X.
	 * 
	 * [Trip]  Cycling trip data
	 * 
	 * 1:	87 		Distance = 8,7 km / mile
	 * 2:	1400 	Ascent (hrm 1.02 10m / 10ft, hrm 1.05: 1m / 1ft)
	 * 3:	92982	Total time in seconds
	 * 4:	1159 	Average altitude (HRM 1.02 10m / 10ft, HRM 1.05: 1m / 1ft)
	 * 5:	1304	Maximum altitude (HRM 1.02 10m / 10ft, HRM 1.05: 1m / 1ft)
	 * 6:	1882	Average speed = 1882 / 128 = 14,7 km/h / mph
	 * 7:	3396	Maximum speed = 3396 / 128 = 26,5 km/h / mph
	 * 8:	418		Odometer value at the end of an exercise, 418 = 418 km / mile
	 * 
	 * </pre>
	 * 
	 * @param fileReader
	 * @return
	 * @throws IOException
	 */
	private boolean parseSection80Trip(final BufferedReader fileReader) throws IOException {

		_sectionTrip = new SectionTrip();

		String line;
		int lineNo = 1;
		// read section
		while ((line = fileReader.readLine()) != null) {

			// check if section has ended
			if (line.length() == 0 || line.startsWith(SECTION_START_CHARACTER)) {
				break;
			}

			try {

				final int value = Integer.parseInt(line);

				switch (lineNo) {
				case 1:
					_sectionTrip.distance = value;
					break;
				case 2:
					_sectionTrip.ascent = value;
					break;
				case 3:
					_sectionTrip.totalTime = value;
					break;
				case 4:
					_sectionTrip.avgAlititude = value;
					break;
				case 5:
					_sectionTrip.maxAltitude = value;
					break;
				case 6:
					_sectionTrip.avgSpeed = value;
					break;
				case 7:
					_sectionTrip.maxSpeed = value;
					break;
				case 8:
					_sectionTrip.odometer = value;
					break;

				default:
					break;
				}

			} catch (final NumberFormatException e) {
				// just ignore it
			}

			lineNo++;
		}

		if (_isDebug) {
			System.out.println(_sectionTrip.toString());
		}

		return true;
	}

	/**
	 * <pre>
	 * 
	 * SMode = 11011010
	 * 		  (abcdefgh)	With versions 1.06
	 * 
	 * SMode = 110110100
	 * 		  (abcdefghi)	With versions 1.07
	 * 
	 * Data type parameters
	 * 
	 * a)  Speed						(0=off, 1=on)
	 * b)  Cadence						(0=off, 1=on)
	 * c)  Altitude						(0=off, 1=on)
	 * d)  Power						(0=off, 1=on)
	 * e)  Power Left Right Balance 	(0=off, 1=on)
	 * f)  Power Pedalling Index 		(0=off, 1=on)
	 * g)  HR/CC data
	 * 			0 = HR data only,
	 * 			1 = HR + cycling data
	 * h)  US / Euro unit
	 * 			0 = Euro (km, km/h, m, °C)
	 * 			1 = US (miles, mph, ft, °F)
	 * 
	 * All distance, speed, altitude and temperature values depend on US/Euro unit
	 * selection (km / miles, km/h / mph, m / ft, °C / °F).
	 * 
	 * i)  Air pressure (0=off, 1=on)
	 * 
	 * ------------------------------------------------------------------------
	 * 
	 * The following data format is for HRM version 1.06
	 * 
	 * DATA  COMMENTS
	 * 
	 * [HRData] Heart Rates (bpm)
	 * |
	 * |		Speed (0.1 km/h or mph)
	 * |		|
	 * |		|		Cadence (rpm)
	 * |		|		|
	 * |		|		|		Altitude (m/ft)
	 * |		|		|		|
	 * |		|		|		|		Power (Watts)
	 * |		|		|		|		|
	 * |		|		|		|		|		Power Balance and Pedalling Index
	 * |		|		|		|		|		|
	 * 83 		173  	81		760		325		12857
	 * 85 		171  	90		780		340		12857
	 * 94 		165  	92		770		335 	12857
	 * 
	 * </pre>
	 * 
	 * @param fileReader
	 * @return
	 * @throws IOException
	 */
	private boolean parseSection90HRData106(final BufferedReader fileReader) throws IOException {

		String line;
		while ((line = fileReader.readLine()) != null) {

			// check if section has ended
			if (line.length() == 0 || line.startsWith(SECTION_START_CHARACTER)) {
				break;
			}

			final HRDataSlice hrDataSlice = new HRDataSlice();
			boolean isSliceAvailable = false;

			final StringTokenizer tokenLine = new StringTokenizer(line, DATA_DELIMITER);

			// loop all tokens in one line
			while (true) {
				try {

					final String token = tokenLine.nextToken();
					final int tokenValue = Integer.parseInt(token);

					// first value should be pulse value
					if (hrDataSlice.pulse == Integer.MIN_VALUE) {
						hrDataSlice.pulse = tokenValue;

					} else

					if (hrDataSlice.speed == Integer.MIN_VALUE && _sectionParams.isSpeed) {
						hrDataSlice.speed = tokenValue;

					} else

					if (hrDataSlice.cadence == Integer.MIN_VALUE && _sectionParams.isCadence) {
						hrDataSlice.cadence = tokenValue;

					} else

					if (hrDataSlice.altitude == Integer.MIN_VALUE && _sectionParams.isAltitude) {
						hrDataSlice.altitude = tokenValue;

					} else

					if (hrDataSlice.power == Integer.MIN_VALUE && _sectionParams.isPower) {
						hrDataSlice.power = tokenValue;
					}

					/*
					 * to implement power fields I need some test data
					 */

					isSliceAvailable = true;

				} catch (final NumberFormatException e) {
					break;
				} catch (final NoSuchElementException e) {
					break;
				}
			}

			if (isSliceAvailable) {
				_sectionHRData.add(hrDataSlice);
			}
		}

		return true;
	}

//	public boolean processDeviceData(	final String importFileName,
//										final DeviceData deviceData,
//										final HashMap<Long, TourData> tourDataMap) {
	@Override
	public boolean processDeviceData(	final String importFilePath,
										final DeviceData deviceData,
										final HashMap<Long, TourData> alreadyImportedTours,
										final HashMap<Long, TourData> newlyImportedTours) {

		_importFilePath = importFilePath;
		_deviceData = deviceData;

		if (_isDebug) {
			System.out.println(importFilePath);
		}

		return parseSection(importFilePath, deviceData, alreadyImportedTours, newlyImportedTours);
	}

	protected void showError(final String message) {

		if (_lastUsedImportId == _deviceData.importId) {

			// do not bother the user with the same error message

		} else {

			_lastUsedImportId = _deviceData.importId;

			Display.getDefault().syncExec(new Runnable() {
				public void run() {

					MessageDialog.openError(
							Display.getCurrent().getActiveShell(),
							Messages.Import_Error_DialogTitle,
							message);
				}
			});
		}
	}

	private boolean validateData() {

		if (_sectionParams == null) {
			return false;
		}

		// check version
		if (_sectionParams.version != 106) {
			showError(NLS.bind(Messages.Import_Error_DialogMessage_InvalidVersion, _importFilePath));
			return false;
		}

		// check SMode
		if (_sectionParams.sMode == null) {
			showError(NLS.bind(Messages.Import_Error_DialogMessage_InvalidField, _importFilePath, PARAMS_S_MODE));
			return false;
		}

		// check date/time
		if (_sectionParams.startYear == Integer.MIN_VALUE
				|| _sectionParams.startMonth == Integer.MIN_VALUE
				|| _sectionParams.startDay == Integer.MIN_VALUE
				|| _sectionParams.startHour == Integer.MIN_VALUE
				|| _sectionParams.startMinute == Integer.MIN_VALUE
				|| _sectionParams.startSecond == Integer.MIN_VALUE) {
			showError(NLS.bind(Messages.Import_Error_DialogMessage_InvalidDate, _importFilePath));
			return false;
		}

// disabled because data version is already checked
//		if (validateData10Monitor() == false) {
//			return false;
//		}

		if (validateData20Interval() == false) {
			return false;
		}

		return true;
	}

//	/**
//	 * check monitor
//	 *
//	 * <pre>
//	 *
//	 * Heart rate monitor type
//	 *
//	 *  1 = Polar Sport Tester / Vantage XL
//	 *  2 = Polar Vantage NV (VNV)
//	 *  3 = Polar Accurex Plus
//	 *  4 = Polar XTrainer Plus
//	 *  6 = Polar S520
//	 *  7 = Polar Coach
//	 *  8 = Polar S210
//	 *  9 = Polar S410
//	 * 10 = Polar S510
//	 * 11 = Polar S610 / S610i
//	 * 12 = Polar S710 / S710i / S720i
//	 * 13 = Polar S810 / S810i
//	 * 15 = Polar E600
//	 * 20 = Polar AXN500
//	 * 21 = Polar AXN700
//	 * 22 = Polar S625X / S725X
//	 * 23 = Polar S725
//	 * 33 = Polar CS400
//	 * 34 = Polar CS600X
//	 * 35 = Polar CS600
//	 * 36 = Polar RS400
//	 * 37 = Polar RS800
//	 * 38 = Polar RS800X
//	 *
//	 * </pre>
//	 *
//	 * @return
//	 */
//	private boolean validateData10Monitor() {
//
//		if (_sectionParams.monitor == 33) {
//			return true;
//		}
//
//		final StringBuilder supportedDevices = new StringBuilder();
//
//		supportedDevices.append(Messages.Supported_Devices_Polar_CS400);
//		supportedDevices.append(UI.NEW_LINE);
//
//		showError(NLS.bind(
//				Messages.Import_Error_DialogMessage_InvalidDevice,
//				_importFilePath,
//				supportedDevices.toString()));
//
//		return false;
//	}

	/**
	 * check interval
	 * 
	 * <pre>
	 * 
	 * Interval=5  Data type:
	 * 
	 *   1     =   1 seconds recording interval
	 *   2     =   2 seconds recording interval
	 *   5     =   5 seconds recording interval
	 *  15     =  15 seconds recording interval
	 *  30     =  30 seconds recording interval
	 *  60     =  60 seconds recording interval
	 * 300     =   5 minutes recording interval
	 * 
	 * 120     = 120 seconds recording interval (dynamic)
	 * 240     = 240 seconds recording interval (dynamic)
	 * 480     = 480 seconds recording interval (dynamic)
	 * 
	 * 238     = R - R data (VNV, S810, S810i, RS, CS)
	 * 204     = intermediate times only (PST, VXL, VNV, XTr+, Acc+)
	 * 
	 * </pre>
	 */
	private boolean validateData20Interval() {

		final int interval = _sectionParams.interval;

		if (interval == 1
				|| interval == 2
				|| interval == 5
				|| interval == 15
				|| interval == 30
				|| interval == 60
				|| interval == 300
//				|| interval == 238
		//
		) {

			_sectionParams.mtInterval = interval;

			return true;
		}

		final StringBuilder supportedDevices = new StringBuilder();

		supportedDevices.append(Messages.Supported_Intervals_1_Second);
		supportedDevices.append(UI.NEW_LINE);
		supportedDevices.append(Messages.Supported_Intervals_2_Second);
		supportedDevices.append(UI.NEW_LINE);
		supportedDevices.append(Messages.Supported_Intervals_5_Second);
		supportedDevices.append(UI.NEW_LINE);
		supportedDevices.append(Messages.Supported_Intervals_15_Second);
		supportedDevices.append(UI.NEW_LINE);
		supportedDevices.append(Messages.Supported_Intervals_30_Second);
		supportedDevices.append(UI.NEW_LINE);
		supportedDevices.append(Messages.Supported_Intervals_60_Second);
		supportedDevices.append(UI.NEW_LINE);
		supportedDevices.append(Messages.Supported_Intervals_5_Minutes);
//		supportedDevices.append(UI.NEW_LINE);
//		supportedDevices.append(Messages.Supported_Intervals_238);

		showError(NLS.bind(
				Messages.Import_Error_DialogMessage_InvalidInterval,
				_importFilePath,
				supportedDevices.toString()));

		return false;
	}

	/**
	 * @return Return <code>true</code> when the file has a valid .hrm data format
	 */
	public boolean validateRawData(final String fileName) {

		BufferedReader fileReader = null;

		try {

			fileReader = new BufferedReader(new FileReader(fileName));

			final String firstLine = fileReader.readLine();
			if (firstLine == null || firstLine.startsWith(SECTION_PARAMS) == false) {
				return false;
			}

			final String secondLine = fileReader.readLine();
			if (secondLine == null || secondLine.startsWith("Version=") == false) { //$NON-NLS-1$
				return false;
			}

		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		} finally {
			if (fileReader != null) {
				try {
					fileReader.close();
				} catch (final IOException e1) {
					e1.printStackTrace();
				}
			}
		}

		return true;
	}

}
