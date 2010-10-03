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
import java.util.StringTokenizer;

import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.SerialParameters;
import net.tourbook.importdata.TourbookDevice;
import net.tourbook.ui.UI;
import net.tourbook.util.StatusUtil;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.joda.time.DateTime;

/**
 * This device reader is importing data from Polar device files.
 */
public class PolarHRMDataReader extends TourbookDevice {

	private static final String		HR_DATA_DELIMITER		= "\t";
	private static final String		SECTION_START_CHARACTER	= "[";

	private static final String		SECTION_PARAMS			= "[Params]";

	private static final String		SECTION_NOTE			= "[Note]";
	private static final String		SECTION_INT_TIMES		= "[IntTimes]";
	private static final String		SECTION_INT_NOTES		= "[IntNotes]";
	private static final String		SECTION_EXTRA_DATA		= "[ExtraData]";
	private static final String		SECTION_LAP_NAMES		= "[LapNames]";
	private static final String		SECTION_SUMMARY_123		= "[Summary-123]";
	private static final String		SECTION_SUMMARY_TH		= "[Summary-TH]";
	private static final String		SECTION_HR_ZONES		= "[HRZones]";
	private static final String		SECTION_SWAP_TIMES		= "[SwapTimes]";
	private static final String		SECTION_TRIP			= "[Trip]";
	private static final String		SECTION_HR_DATA			= "[HRData]";
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

	private DeviceData				_deviceData;
	private String					_importFilePath;
	private long					_lastUsedImportId;

	private int						_hrmVersion				= -1;
	private Params					_params;

	private boolean					_isDebug				= true;

	private ArrayList<HRDataSlice>	_HRData					= new ArrayList<PolarHRMDataReader.HRDataSlice>();

	class HRDataSlice {

		public int	pulse		= Integer.MIN_VALUE;
		public int	speed		= Integer.MIN_VALUE;
		public int	cadence		= Integer.MIN_VALUE;
		public int	altitude	= Integer.MIN_VALUE;
		public int	power		= Integer.MIN_VALUE;
	}

	class Params {

		public int		version					= Integer.MIN_VALUE;
		public int		monitor					= Integer.MIN_VALUE;
		public int		interval				= Integer.MIN_VALUE;
		public int		restHR					= Integer.MIN_VALUE;

		public int		startYear				= Integer.MIN_VALUE;
		public int		startMonth				= Integer.MIN_VALUE;
		public int		startDay				= Integer.MIN_VALUE;
		public int		startHour				= Integer.MIN_VALUE;
		public int		startMinute				= Integer.MIN_VALUE;
		public int		startSecond				= Integer.MIN_VALUE;

		public String	sMode;
		public boolean	isSpeed					= false;
		public boolean	isCadence				= false;
		public boolean	isAltitude				= false;
		public boolean	isPower					= false;
		public boolean	isPowerLeftRightBalance	= false;
		public boolean	isPowerPedallingIndex	= false;
		public boolean	isHRAndCycling			= false;
		public boolean	isUSUnit				= false;
		public boolean	isAirPressure			= false;

		/*
		 * mytourbook specific fields
		 */
		public int		mtInterval;

		@Override
		public String toString() {

			final StringBuilder sb = new StringBuilder();

			sb.append("Params:\n");

			sb.append("\tversion:\t");
			sb.append(version);
			sb.append(UI.NEW_LINE);

			sb.append("\tmonitor:\t");
			sb.append(monitor);
			sb.append(UI.NEW_LINE);

			sb.append("\tinterval:\t");
			sb.append(interval);
			sb.append(UI.NEW_LINE);

			sb.append("\tRestHR:\t\t");
			sb.append(restHR);
			sb.append(UI.NEW_LINE);

			sb.append("\tStartYear:\t");
			sb.append(startYear);
			sb.append(UI.NEW_LINE);

			sb.append("\tStartMonth:\t");
			sb.append(startMonth);
			sb.append(UI.NEW_LINE);

			sb.append("\tStartDay:\t");
			sb.append(startDay);
			sb.append(UI.NEW_LINE);

			sb.append("\tStartHour:\t");
			sb.append(startHour);
			sb.append(UI.NEW_LINE);

			sb.append("\tStartMinute:\t");
			sb.append(startMinute);
			sb.append(UI.NEW_LINE);

			sb.append("\tStartSecond:\t");
			sb.append(startSecond);
			sb.append(UI.NEW_LINE);

//			sb.append("\t:\t");
//			sb.append();
//			sb.append(UI.NEW_LINE);

			/*
			 * SMode
			 */
			{
				sb.append("SMode:\n");

				sb.append("\tisSpeed\t\t");
				sb.append(_params.isSpeed);
				sb.append(UI.NEW_LINE);

				sb.append("\tisCadence\t");
				sb.append(_params.isCadence);
				sb.append(UI.NEW_LINE);

				sb.append("\tisAltitude\t");
				sb.append(_params.isAltitude);
				sb.append(UI.NEW_LINE);

				sb.append("\tisPower\t\t");
				sb.append(_params.isPower);
				sb.append(UI.NEW_LINE);

				sb.append("\tisPowerLR\t");
				sb.append(_params.isPowerLeftRightBalance);
				sb.append(UI.NEW_LINE);

				sb.append("\tisPowerPed\t");
				sb.append(_params.isPowerPedallingIndex);
				sb.append(UI.NEW_LINE);

				sb.append("\tisHRAndCycle\t");
				sb.append(_params.isHRAndCycling);
				sb.append(UI.NEW_LINE);

				sb.append("\tisUSUnit\t");
				sb.append(_params.isUSUnit);
				sb.append(UI.NEW_LINE);

				sb.append("\tisAirPressure\t");
				sb.append(_params.isAirPressure);
				sb.append(UI.NEW_LINE);
			}

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

		if (_HRData != null) {
			_HRData.clear();
		}
	}

	/**
	 * Converts {@link HRDataSlice} into {@link TimeData}
	 * 
	 * @param dtTourStart
	 * @return
	 */
	private ArrayList<TimeData> createTimeSerie(final DateTime dtTourStart) {

		final boolean isImperial = _params.isUSUnit;
		final int sliceTimeInterval = _params.interval;

		int relativeTime = 0;
		float absoluteDistance = 0;

		final ArrayList<TimeData> timeDataList = new ArrayList<TimeData>();

		for (final HRDataSlice hrSlice : _HRData) {

			final TimeData dtSlice = new TimeData();

			dtSlice.absoluteTime = dtTourStart.plusSeconds(relativeTime).getMillis();

			if (hrSlice.speed != Integer.MIN_VALUE) {

				final float distanceDiff = ((float) hrSlice.speed) * sliceTimeInterval;
				absoluteDistance += distanceDiff;

				dtSlice.absoluteDistance = absoluteDistance;
			}

			/*
			 * convert speed into distance
			 */
			if (hrSlice.altitude != Integer.MIN_VALUE) {
				dtSlice.absoluteAltitude = hrSlice.altitude / (isImperial ? UI.UNIT_FOOT : 1);
			}

			if (hrSlice.cadence != Integer.MIN_VALUE) {
				dtSlice.cadence = hrSlice.cadence;
			}

			timeDataList.add(dtSlice);

			relativeTime += sliceTimeInterval;
		}

		return timeDataList;
	}

	public String getDeviceModeName(final int profileId) {
		return null;
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
	private void parseDate(final String value) {

		final byte[] dataBytes = value.getBytes();
		final int bytesLength = dataBytes.length;

		if (bytesLength < 8) {
			return;
		}

		_params.startYear = Integer.parseInt(value.substring(0, 4));
		_params.startMonth = Integer.parseInt(value.substring(4, 6));
		_params.startDay = Integer.parseInt(value.substring(6, 8));
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
	private void parseSMode(final String dataType) {

		_params.sMode = dataType;

		final byte[] dataBytes = dataType.getBytes();
		final int bytesLength = dataBytes.length;

		if (bytesLength > 0) {
			_params.isSpeed = dataBytes[0] == '1';
		}

		if (bytesLength > 1) {
			_params.isCadence = dataBytes[1] == '1';
		}

		if (bytesLength > 2) {
			_params.isAltitude = dataBytes[2] == '1';
		}

		if (bytesLength > 3) {
			_params.isPower = dataBytes[3] == '1';
		}

		if (bytesLength > 4) {
			_params.isPowerLeftRightBalance = dataBytes[4] == '1';
		}

		if (bytesLength > 5) {
			_params.isPowerPedallingIndex = dataBytes[5] == '1';
		}

		if (bytesLength > 6) {
			_params.isHRAndCycling = dataBytes[6] == '1';
		}

		if (bytesLength > 7) {
			_params.isUSUnit = dataBytes[7] == '1';
		}

		if (bytesLength > 8) {
			_params.isAirPressure = dataBytes[8] == '1';
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
	private void parseStartTime(final String value) {

		final byte[] dataBytes = value.getBytes();
		final int bytesLength = dataBytes.length;

		if (bytesLength < 9) {
			return;
		}

		final int offset = dataBytes[1] == ':' ? 0 : 1;

		_params.startHour = offset == 0 //
				? Integer.parseInt(value.substring(0, 1))
				: Integer.parseInt(value.substring(0, 2));

		_params.startMinute = Integer.parseInt(value.substring(offset + 2, offset + 4));
		_params.startSecond = Integer.parseInt(value.substring(offset + 5, offset + 7));
	}

	@Override
	public boolean processDeviceData(	final String importFileName,
										final DeviceData deviceData,
										final HashMap<Long, TourData> tourDataMap) {

		_importFilePath = importFileName;
		_deviceData = deviceData;

		if (_isDebug) {
			System.out.println(importFileName);
		}
		boolean returnValue = false;

		BufferedReader fileReader = null;

		try {

			String line;
			fileReader = new BufferedReader(new FileReader(importFileName));

			while ((line = fileReader.readLine()) != null) {

				boolean isValid = true;

				if (line.startsWith(SECTION_PARAMS)) {
					isValid = read10Params(fileReader, deviceData);
				} else if (line.startsWith(SECTION_NOTE)) {
					// is not yet supported
				} else if (line.startsWith(SECTION_INT_TIMES)) {
					// is not yet supported
				} else if (line.startsWith(SECTION_INT_NOTES)) {
					// is not yet supported
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
					// is not yet supported
				} else if (line.startsWith(SECTION_HR_DATA)) {

					if (_hrmVersion == 106) {
						isValid = read90HRData106(fileReader);
					}
				}

				if (isValid == false) {
					return false;
				}
			}

			if (validateData() == false) {
				return false;
			}

			setTourData(tourDataMap);

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

	private boolean read10Params(final BufferedReader fileReader, final DeviceData deviceData) throws IOException {

		_params = new Params();

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
					_params.version = Integer.parseInt(value);
					_hrmVersion = _params.version;

				} else if (key.equals(PARAMS_MONITOR)) {

					// Monitor=22
					_params.monitor = Integer.parseInt(value);

				} else if (key.equals(PARAMS_S_MODE)) {

					// SMode=101000100
					parseSMode(value);

				} else if (key.equals(PARAMS_DATE)) {

					// Date=20080227
					parseDate(value);

				} else if (key.equals(PARAMS_START_TIME)) {

					// StartTime=15:16:19.0
					parseStartTime(value);

				} else if (key.equals(PARAMS_LENGTH)) {
					// Length=01:48:49.7
				} else if (key.equals(PARAMS_INTERVAL)) {

					// Interval=5
					_params.interval = Integer.parseInt(value);

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
					_params.restHR = Integer.parseInt(value);

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
			System.out.println(_params.toString());
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
	private boolean read90HRData106(final BufferedReader fileReader) throws IOException {

		// read section
		String line;
		while ((line = fileReader.readLine()) != null) {

			// check if section has ended
			if (line.length() == 0 || line.startsWith(SECTION_START_CHARACTER)) {
				break;
			}

			final HRDataSlice hrDataSlice = new HRDataSlice();
			boolean isSliceAvailable = false;

			final StringTokenizer tokenLine = new StringTokenizer(line, HR_DATA_DELIMITER);

			// loop all tokens in one line
			while (true) {
				try {

					final String token = tokenLine.nextToken();
					final int tokenValue = Integer.parseInt(token);

					// first value should be pulse value
					if (hrDataSlice.pulse == Integer.MIN_VALUE) {
						hrDataSlice.pulse = tokenValue;

					} else

					if (hrDataSlice.speed == Integer.MIN_VALUE && _params.isSpeed) {
						hrDataSlice.speed = tokenValue;

					} else

					if (hrDataSlice.cadence == Integer.MIN_VALUE && _params.isCadence) {
						hrDataSlice.cadence = tokenValue;

					} else

					if (hrDataSlice.altitude == Integer.MIN_VALUE && _params.isAltitude) {
						hrDataSlice.altitude = tokenValue;

					} else

					if (hrDataSlice.power == Integer.MIN_VALUE && _params.isPower) {
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
				_HRData.add(hrDataSlice);
			}
		}

		return true;
	}

	private void setTourData(final HashMap<Long, TourData> tourDataMap) {

		// create data object for each tour
		final TourData tourData = new TourData();

		/*
		 * set tour start date/time
		 */
		final DateTime dtTourStart = new DateTime(
				_params.startYear,
				_params.startMonth,
				_params.startDay,
				_params.startHour,
				_params.startMinute,
				_params.startSecond,
				0);

		tourData.setStartHour((short) dtTourStart.getHourOfDay());
		tourData.setStartMinute((short) dtTourStart.getMinuteOfHour());
		tourData.setStartSecond((short) dtTourStart.getSecondOfMinute());

		tourData.setStartYear((short) dtTourStart.getYear());
		tourData.setStartMonth((short) dtTourStart.getMonthOfYear());
		tourData.setStartDay((short) dtTourStart.getDayOfMonth());

		tourData.setWeek(dtTourStart);

		tourData.setDeviceTimeInterval((short) _params.mtInterval);

		tourData.importRawDataFile = _importFilePath;
		tourData.setTourImportFilePath(_importFilePath);

		tourData.createTimeSeries(createTimeSerie(dtTourStart), true);
		tourData.computeAltitudeUpDown();

//		tourData.setCalories(_calories);

		// after all data are added, the tour id can be created
		final int[] distanceSerie = tourData.getMetricDistanceSerie();
		String uniqueKey;

		uniqueKey = createUniqueId(tourData, distanceSerie, "63193");

		final Long tourId = tourData.createTourId(uniqueKey);

		// check if the tour is already imported
		if (tourDataMap.containsKey(tourId) == false) {

			tourData.computeTourDrivingTime();
			tourData.computeComputedValues();

			tourData.setDeviceId(deviceId);
			tourData.setDeviceName(visibleName);

			// add new tour to other tours
			tourDataMap.put(tourId, tourData);
		}

	}

	private void showError(final String message) {

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

		if (_params == null) {
			return false;
		}

		// check version
		if (_params.version != 106) {
			showError(NLS.bind(Messages.Import_Error_DialogMessage_InvalidVersion, _importFilePath));
			return false;
		}

		// check SMode
		if (_params.sMode == null) {
			showError(NLS.bind(Messages.Import_Error_DialogMessage_InvalidField, _importFilePath, PARAMS_S_MODE));
			return false;
		}

		// check date/time
		if (_params.startYear == Integer.MIN_VALUE
				|| _params.startMonth == Integer.MIN_VALUE
				|| _params.startDay == Integer.MIN_VALUE
				|| _params.startHour == Integer.MIN_VALUE
				|| _params.startMinute == Integer.MIN_VALUE
				|| _params.startSecond == Integer.MIN_VALUE) {
			showError(NLS.bind(Messages.Import_Error_DialogMessage_InvalidDate, _importFilePath));
			return false;
		}

		if (validateData10Monitor() == false) {
			return false;
		}

		if (validateData20Interval() == false) {
			return false;
		}

		return true;
	}

	/**
	 * check monitor
	 * 
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
	 * 
	 * @return
	 */
	private boolean validateData10Monitor() {

		if (_params.monitor == 33) {
			return true;
		}

		final StringBuilder supportedDevices = new StringBuilder();

		supportedDevices.append(Messages.Supported_Devices_Polar_CS400);
		supportedDevices.append(UI.NEW_LINE);

		showError(NLS.bind(
				Messages.Import_Error_DialogMessage_InvalidDevice,
				_importFilePath,
				supportedDevices.toString()));

		return false;
	}

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

		final int interval = _params.interval;

		if (interval == 1
				|| interval == 2
				|| interval == 5
				|| interval == 15
				|| interval == 30
				|| interval == 60
				|| interval == 300) {

			_params.mtInterval = interval;

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
