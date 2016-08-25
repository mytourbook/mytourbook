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
package net.tourbook.device.polar.hrm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.device.Activator;
import net.tourbook.device.gpx.GPXDeviceDataReader;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.SerialParameters;
import net.tourbook.importdata.TourbookDevice;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.joda.time.DateTime;

/**
 * This device reader is importing data from Polar device files.
 */
public class Polar_PDD_DataReader extends TourbookDevice {

	private static final String		DATA_DELIMITER				= "\t";										//$NON-NLS-1$

	private static final String		SECTION_DAY_INFO			= "[DayInfo]";									//$NON-NLS-1$
	private static final String		SECTION_EXERCISE_INFO		= "[ExerciseInfo";								//$NON-NLS-1$
	//
	private final IPreferenceStore	_prefStore					= Activator.getDefault().getPreferenceStore();

	private DeviceData				_deviceData;
	private String					_importFilePath;
	private HashMap<Long, TourData>	_alreadyImportedTours;
	private HashMap<Long, TourData>	_newlyImportedTours;
	//
	private boolean					_isDebug					= false;
	private int						_fileVersionDayInfo			= -1;

	private Day						_currentDay;
	private Exercise				_currentExercise;

	private ArrayList<String>		_exerciseFiles				= new ArrayList<String>();
	private ArrayList<String>		_additionalImportedFiles	= new ArrayList<String>();
	private HashMap<Long, Integer>	_tourSportMap				= new HashMap<Long, Integer>();

	private class Day {

		private DateTime	date;

	}

	private class Exercise {

		private int			fileVersion;

		private String		title;
		private String		description;

		private DateTime	startTime;
		private int			distance;
		private int			duration;

		private int			calories;
		private int			sport;
	}

	// plugin constructor
	public Polar_PDD_DataReader() {}

	@Override
	public String buildFileNameFromRawData(final String rawDataFileName) {
		return null;
	}

	@Override
	public boolean checkStartSequence(final int byteIndex, final int newByte) {
		return false;
	}

	private boolean createExercise() throws Exception {

		final TourData exerciseData = new TourData();

		/*
		 * set tour start date/time
		 */
		exerciseData.setTourStartTime(_currentExercise.startTime);

		exerciseData.setImportFilePath(_importFilePath);

		// set title
		final String title = _currentExercise.title;
		if (title != null) {
			exerciseData.setTourTitle(title);
		} else {
			exerciseData.setTourTitle(""); //$NON-NLS-1$
		}
		// set description
		final String description = _currentExercise.description;
		if (description != null) {
			exerciseData.setTourDescription(description);
		} else {
			exerciseData.setTourDescription(""); //$NON-NLS-1$
		}

		exerciseData.setTourDistance(_currentExercise.distance);
		exerciseData.setTourDrivingTime(_currentExercise.duration);

		// set other fields
		exerciseData.setCalories(_currentExercise.calories);

		// after all data are added, the tour id can be created
		final String uniqueId = createUniqueId(exerciseData, Util.UNIQUE_ID_SUFFIX_POLAR_PDD);
		final Long tourId = exerciseData.createTourId(uniqueId);

		// check if the tour is already imported
		if (_alreadyImportedTours.containsKey(tourId) == false) {

			// add new tour to other tours
			_newlyImportedTours.put(tourId, exerciseData);
		}

		// save the sport type for this exercise
		if (_tourSportMap.containsKey(tourId) == false) {
			_tourSportMap.put(tourId, _currentExercise.sport);
		}

		return true;
	}

	private boolean createExercise(final String hrmFileName, final String gpxFileName) throws Exception {

		// hrm data must be available
		if (hrmFileName == null) {
			return false;
		}

		_exerciseFiles.clear();

		final String titleFromTitle = _prefStore.getString(IPreferences.TITLE_DESCRIPTION);
		final boolean isTitleFromTitle = titleFromTitle
				.equalsIgnoreCase(IPreferences.TITLE_DESCRIPTION_TITLE_FROM_TITLE) || titleFromTitle.length() == 0;

		final IPath importPath = new Path(_importFilePath).removeLastSegments(1);

		// get .hrm data
		final IPath hrmFilePath = importPath.append(hrmFileName);
		final TourData hrmTourData = createExercise_10_ImportSeparatedFile(hrmFilePath, getPolarHRMDataReader());

		if (hrmTourData == null) {
			return false;
		}
		_exerciseFiles.add(hrmFilePath.toOSString());

		// get .gpx data
		if (gpxFileName != null) {

			final IPath gpxFilePath = importPath.append(gpxFileName);
			final TourData gpxTourData = createExercise_10_ImportSeparatedFile(gpxFilePath, getGPXDeviceDataReader());

			if (gpxTourData != null && gpxTourData.latitudeSerie != null) {

				createExercise_20_SyncHrmGpx(hrmTourData, gpxTourData);
				createExercise_22_AdjustTimeSlices(hrmTourData, gpxTourData);
			}

			_exerciseFiles.add(gpxFilePath.toOSString());
		}

		// overwrite path and set it to pdd file so that a reimport works
		hrmTourData.setImportFilePath(_importFilePath);

		// set title
		final String title = _currentExercise.title;
		if (title != null && title.length() > 0) {
			if (isTitleFromTitle) {
				hrmTourData.setTourTitle(title);
			} else {
				hrmTourData.setTourDescription(title);
			}
		}

		// set description
		final String description = _currentExercise.description;
		if (description != null && description.length() > 0) {
			if (isTitleFromTitle) {
				hrmTourData.setTourDescription(description);
			} else {
				hrmTourData.setTourTitle(description);
			}
		}

		// set other fields
		hrmTourData.setCalories(_currentExercise.calories);

		// after all data are added, the tour id can be created
		final String uniqueId = createUniqueId(hrmTourData, Util.UNIQUE_ID_SUFFIX_POLAR_PDD);
		final Long tourId = hrmTourData.createTourId(uniqueId);

		// check if the tour is already imported
		if (_alreadyImportedTours.containsKey(tourId) == false) {

			// add new tour to other tours
			_newlyImportedTours.put(tourId, hrmTourData);
		}

		// save the sport type for this exercise
		if (_tourSportMap.containsKey(tourId) == false) {
			_tourSportMap.put(tourId, _currentExercise.sport);
		}

		if (_exerciseFiles.size() > 0) {
			_additionalImportedFiles.addAll(_exerciseFiles);
		}

		return true;
	}

	private TourData createExercise_10_ImportSeparatedFile(	final IPath importFilePath,
															final TourbookDevice deviceDataReader) throws Exception {

		final File importFile = importFilePath.toFile();

		if (importFile.exists() == false) {
			throw new Exception(NLS.bind("File {0} is not available but is defined in file {1}", //$NON-NLS-1$
					importFile.toString(),
					_importFilePath));
		}

		if (deviceDataReader.validateRawData(importFilePath.toOSString()) == false) {
			throw new Exception(NLS.bind("File {0} in parent file {1} is invalid", //$NON-NLS-1$
					importFile.toString(),
					_importFilePath));
		}

		final HashMap<Long, TourData> alreadyImportedTours = new HashMap<Long, TourData>();
		final HashMap<Long, TourData> newlyImportedTours = new HashMap<Long, TourData>();
		if (deviceDataReader.processDeviceData(
				importFilePath.toOSString(),
				_deviceData,
				alreadyImportedTours,
				newlyImportedTours) == false) {
			return null;
		}

		final TourData[] importTourData = newlyImportedTours.values().toArray(
				new TourData[newlyImportedTours.values().size()]);

		// check bounds
		if (importTourData.length == 0) {
			return null;
		}

		return importTourData[0];
	}

	/**
	 * Sets gpx lat/lon data into hrm tour data. HRM tour data are the leading data serie, GPX data
	 * is set according to the time.
	 * 
	 * @param hrmTourData
	 * @param gpxTourData
	 */
	private void createExercise_20_SyncHrmGpx(final TourData hrmTourData, final TourData gpxTourData) {

		/*
		 * set gpx tour start to the same time as the hrm tour start
		 */
		final ZonedDateTime hrmTourStart = hrmTourData.getTourStartTime();
		final ZonedDateTime gpxTourStart = gpxTourData.getTourStartTime();

		final long absoluteHrmTourStart = hrmTourStart.toInstant().getEpochSecond();
		long absoluteGpxTourStart = gpxTourStart.toInstant().getEpochSecond();

		final int timeDiff = (int) (absoluteHrmTourStart - absoluteGpxTourStart);
		final int timeDiffHours = (timeDiff / 3600) * 3600;

		// adjust gpx to hrm tour start
		absoluteGpxTourStart = absoluteGpxTourStart + timeDiffHours;

		/*
		 * define shortcuts for the data series
		 */
		final int[] hrmTimeSerie = hrmTourData.timeSerie;
		final int[] gpxTimeSerie = gpxTourData.timeSerie;

		final int hrmSerieLength = hrmTimeSerie.length;
		final int gpxSerieLength = gpxTimeSerie.length;

		final double[] gpxLatSerie = gpxTourData.latitudeSerie;
		final double[] gpxLonSerie = gpxTourData.longitudeSerie;
		final double[] hrmLatSerie = hrmTourData.latitudeSerie = new double[hrmSerieLength];
		final double[] hrmLonSerie = hrmTourData.longitudeSerie = new double[hrmSerieLength];

		boolean isFirstGpx = true;
		final double firstLat = gpxLatSerie[0];
		final double firstLon = gpxLonSerie[0];
		double prevLat = firstLat;
		double prevLon = firstLon;

		int gpxSerieIndex = 0;

		for (int hrmSerieIndex = 0; hrmSerieIndex < hrmSerieLength; hrmSerieIndex++) {

			final int relativeHrmTime = hrmTimeSerie[hrmSerieIndex];
			final int relativeGpxTime = gpxTimeSerie[gpxSerieIndex];

			final long hrmTime = absoluteHrmTourStart + relativeHrmTime;
			final long gpxTime = absoluteGpxTourStart + relativeGpxTime;

			if (isFirstGpx && gpxTime <= hrmTime) {
				isFirstGpx = false;
			}

			if (gpxTime > hrmTime) {

				// gpx data are not available

				if (isFirstGpx) {

					hrmLatSerie[hrmSerieIndex] = firstLat;
					hrmLonSerie[hrmSerieIndex] = firstLon;

				} else {

					/*
					 * set lat/lon from previous slice because it is possible that gpx has missing
					 * slices
					 */
					hrmLatSerie[hrmSerieIndex] = prevLat;
					hrmLonSerie[hrmSerieIndex] = prevLon;
				}

			} else {

				hrmLatSerie[hrmSerieIndex] = prevLat = gpxLatSerie[gpxSerieIndex];
				hrmLonSerie[hrmSerieIndex] = prevLon = gpxLonSerie[gpxSerieIndex];
			}

			// advance to next slice
			if (hrmTime >= gpxTime) {

				// the case > should not occure but is used to move gpx slice forward

				gpxSerieIndex++;

				// check bounds
				if (gpxSerieIndex >= gpxSerieLength) {
					gpxSerieIndex = gpxSerieLength - 1;
				}
			}
		}
	}

	private void createExercise_22_AdjustTimeSlices(final TourData hrmTourData, final TourData gpxTourData) {

		int diffGeoSlices = _prefStore.getInt(IPreferences.SLICE_ADJUSTMENT_VALUE);

		// check if time slices needs to be adjusted
		if (diffGeoSlices == 0) {
			return;
		}

		final int[] hrmTimeSerie = hrmTourData.timeSerie;
		final int hrmSerieLength = hrmTimeSerie.length;

		// adjust slices to bounds
		if (diffGeoSlices > hrmSerieLength) {
			diffGeoSlices = hrmSerieLength - 1;
		} else if (-diffGeoSlices > hrmSerieLength) {
			diffGeoSlices = -(hrmSerieLength - 1);
		}

		final double[] hrmLatSerie = hrmTourData.latitudeSerie;
		final double[] hrmLonSerie = hrmTourData.longitudeSerie;

		final int srcPos = diffGeoSlices >= 0 ? 0 : -diffGeoSlices;
		final int destPos = diffGeoSlices >= 0 ? diffGeoSlices : 0;
		final int adjustedLength = hrmSerieLength - (diffGeoSlices < 0 ? -diffGeoSlices : diffGeoSlices);

		System.arraycopy(hrmLatSerie, srcPos, hrmLatSerie, destPos, adjustedLength);
		System.arraycopy(hrmLonSerie, srcPos, hrmLonSerie, destPos, adjustedLength);

		// fill gaps with starting/ending position
		if (diffGeoSlices >= 0) {

			final double startLat = hrmLatSerie[0];
			final double startLon = hrmLonSerie[0];

			for (int serieIndex = 0; serieIndex < diffGeoSlices; serieIndex++) {
				hrmLatSerie[serieIndex] = startLat;
				hrmLonSerie[serieIndex] = startLon;
			}

		} else {

			// diffGeoSlices < 0

			final int lastIndex = hrmSerieLength - 1;
			final int validEndIndex = lastIndex - (-diffGeoSlices);
			final double endLat = hrmLatSerie[lastIndex];
			final double endLon = hrmLonSerie[lastIndex];

			for (int serieIndex = validEndIndex; serieIndex < hrmSerieLength; serieIndex++) {
				hrmLatSerie[serieIndex] = endLat;
				hrmLonSerie[serieIndex] = endLon;
			}
		}
	}

	@Override
	public ArrayList<String> getAdditionalImportedFiles() {

		if (_additionalImportedFiles.size() > 0) {
			return _additionalImportedFiles;
		}

		return null;
	}

	@Override
	public String getDeviceModeName(final int profileId) {
		return null;
	}

	protected TourbookDevice getGPXDeviceDataReader() {
		return new GPXDeviceDataReader();
	}

	protected TourbookDevice getPolarHRMDataReader() {
		return new Polar_HRM_DataReader();
	}

	@Override
	public SerialParameters getPortParameters(final String portName) {
		return null;
	}

	@Override
	public int getStartSequenceSize() {
		return -1;
	}

	public int getTourSport(final Long tourId) {
		if (_tourSportMap.containsKey(tourId)) {
			return _tourSportMap.get(tourId);
		} else {
			return -1;
		}
	}

	@Override
	public int getTransferDataSize() {
		return -1;
	}

	private boolean parseSection() {

		boolean returnValue = false;

		BufferedReader fileReader = null;

		try {

			// fileReader = new BufferedReader(new FileReader(_importFilePath));

			// the default charset has not handled correctly the german umlaute in uppercase on Linux/OSX
			fileReader = new BufferedReader(new InputStreamReader(new FileInputStream(_importFilePath), UI.ISO_8859_1));

			String line;
			while ((line = fileReader.readLine()) != null) {

				boolean isValid = true;

				if (line.startsWith(SECTION_DAY_INFO)) {

					_currentDay = new Day();

					isValid = parseSection10DayInfo(fileReader);

					// check version
					if (_fileVersionDayInfo != 100) {
						throw new Exception(NLS.bind("File {0} has an invalid version in section {1}", //$NON-NLS-1$
								_importFilePath,
								SECTION_DAY_INFO));
					}

				} else if (line.startsWith(SECTION_EXERCISE_INFO)) {

					_currentExercise = new Exercise();

					isValid = parseSection20ExerciseInfo(fileReader);

					// check version
					if (_currentExercise.fileVersion != 101) {
						throw new Exception(NLS.bind("File {0} has an invalid version in section {1}", //$NON-NLS-1$
								_importFilePath,
								SECTION_EXERCISE_INFO));
					}
				}

				if (isValid == false) {
					return false;
				}
			}

			returnValue = true;

		} catch (final Exception e) {
			StatusUtil.showStatus(e);
			return false;
		} finally {

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

	/**
	 * <pre>
	 * 3.  Daily information
	 * 
	 * The following weekly information applies for one day.
	 * 
	 * [DayInfo]
	 * 100         1           4           6            1       512		// row 0
	 * 20011116    1           65         20         7500     25200		// row 1
	 * ...         														// row 2 ... n
	 * Day note text                									// text row
	 * </pre>
	 * 
	 * @param fileReader
	 * @return
	 * @throws IOException
	 */
	private boolean parseSection10DayInfo(final BufferedReader fileReader) throws IOException {

		String line;
		StringTokenizer tokenLine;

		try {
			/**
			 * <pre>
			 * Row 0
			 * 
			 * Data  					Example
			 * 
			 * FileVersion 				100
			 * Nbr Of Info Rows  		1
			 * Nbr Of Num Rows  		4
			 * Nbr Of Num Columns  		6
			 * Nbr Of Text Rows  		1
			 * Max Char Per Text Row  512
			 * 
			 * </pre>
			 */
			line = fileReader.readLine();
			if (line == null) {
				return false;
			}

			tokenLine = new StringTokenizer(line, DATA_DELIMITER);

			// 1
			_fileVersionDayInfo = Integer.parseInt(tokenLine.nextToken());

			// 2
			final int numOfInfoRows = Integer.parseInt(tokenLine.nextToken());

			// 3
			final int numOfNumberRows = Integer.parseInt(tokenLine.nextToken());

			// 4
			@SuppressWarnings("unused")
			final int numOfNumberColumns = Integer.parseInt(tokenLine.nextToken());

			// 5
			final int numOfTextRows = Integer.parseInt(tokenLine.nextToken());

			int numOfNumberRowsAnalyzed = 0;

			/**
			 * <pre>
			 * Row 1
			 * 
			 * Data  					Example
			 * 
			 * Date	 					20110705
			 * ?
			 * ?
			 * ?
			 * ?
			 * ?
			 * 
			 * </pre>
			 */
			line = fileReader.readLine();
			if (line == null) {
				return false;
			}

			tokenLine = new StringTokenizer(line, DATA_DELIMITER);

			// 1
			final String date = tokenLine.nextToken();
			final int year = Integer.parseInt(date.substring(0, 4));
			final int month = Integer.parseInt(date.substring(4, 6));
			final int day = Integer.parseInt(date.substring(6, 8));
			_currentDay.date = new DateTime(year, month, day, 0, 0, 0, 0);

			// 2
			tokenLine.nextToken();

			// 3
			tokenLine.nextToken();

			// 4
			tokenLine.nextToken();

			// 5
			tokenLine.nextToken();

			numOfNumberRowsAnalyzed++;

			// skip additional info rows (which are not available in version 100)
			if (numOfInfoRows > 1) {
				skipRows(fileReader, numOfInfoRows - 1);
			}

			if (skipRows(fileReader, numOfNumberRows + numOfTextRows - numOfNumberRowsAnalyzed) == null) {
				return false;
			}

		} catch (final Exception e) {
			StatusUtil.log(e);
			return false;
		}

		return true;
	}

	/**
	 * <pre>
	 * 4.  Exercise information
	 * 
	 * The following exercise information applies for one exercise. If the day includes more than one
	 * exercise, the ExerciseInfo section will be multiplied at file. The one day can include at max 10
	 * exercises.
	 * 
	 * [ExerciseInfo1]
	 * 101       1           12         6           12         512          // row 0
	 * ...                        											// row 1...n
	 * exercise name              											// text row 0
	 * exercise note text                    								// text row 1
	 * attached hrm file (if in same folder, no folder info with file name) // text row 2
	 * reserved text                 										// text row 3
	 * ...                       											// text row 4...n
	 * </pre>
	 * 
	 * @param fileReader
	 * @return
	 */
	private boolean parseSection20ExerciseInfo(final BufferedReader fileReader) {

		try {

			/**
			 * <pre>
			 * 
			 * Row 0
			 * 
			 * Data  				Example    Format
			 * 
			 * 1:	FileVersion  			101
			 * 2:	Nbr Of Info Rows  		1
			 * 3:	Nbr Of Num Rows  		12
			 * 4:	Nbr Of Num Columns  		6
			 * 5:	Nbr Of Text Rows  		12
			 * 6:	Max Char Per Text Row  	512
			 * 
			 * </pre>
			 */
			String line = fileReader.readLine();
			if (line == null) {
				return false;
			}

			StringTokenizer tokenLine = new StringTokenizer(line, DATA_DELIMITER);

			// column 1
			_currentExercise.fileVersion = Integer.parseInt(tokenLine.nextToken());

			// column 2
			final int numOfInfoRows = Integer.parseInt(tokenLine.nextToken());

			// column 3
			final int numOfNumberRows = Integer.parseInt(tokenLine.nextToken());

			// column 4
			@SuppressWarnings("unused")
			final int numOfNumberColumns = Integer.parseInt(tokenLine.nextToken());

			// column 5
			@SuppressWarnings("unused")
			final int numOfTextRows = Integer.parseInt(tokenLine.nextToken());

			// skip additional info rows (which are not available in version 101)
			if (numOfInfoRows > 1) {
				skipRows(fileReader, numOfInfoRows - 1);
			}

			int numOfNumberRowsAnalyzed = 0;

			/**
			 * <pre>
			 * 
			 * Number Row 1
			 * 
			 * Data  						Example    	Format
			 * 
			 * 1:	- Reserved -  			0
			 * 2:	No report  				1
			 * 3:	Not edited manually  	0
			 * 4:	- Reserved -  			0
			 * 5:	Start time  			36000   	Seconds (from midnight 0:00:00) 36000 = 10:00:00
			 * 6:	Total time  			2700        Seconds 2700 = 0:45:00, 0 ... 99 h 59 min, for one
			 * 											phase 10 s ... 99 min 59 s
			 * </pre>
			 */

			line = fileReader.readLine();
			if (line == null) {
				return false;
			}
			numOfNumberRowsAnalyzed++;

			tokenLine = new StringTokenizer(line, DATA_DELIMITER);

			// column 1
			tokenLine.nextToken();

			// column 2
			tokenLine.nextToken();

			// column 3
			tokenLine.nextToken();

			// column 4
			tokenLine.nextToken();

			// column 5
			if (null != _currentDay.date) {
				_currentExercise.startTime = _currentDay.date.plusSeconds(Integer.parseInt(tokenLine.nextToken()));
			}

			// column 6
			_currentExercise.duration = Integer.parseInt(tokenLine.nextToken());

			/**
			 * <pre>
			 * 
			 * Number Row 2
			 * 
			 * Data							Example		Format
			 * 
			 * 1:	Sport					3			Personal Sport ID
			 * 2:	Distance OLD  			150         km * 10 NOT IN USE AFTER PPP version 3.02.008
			 * 3:	Feeling  				0 ... 5		0 = :-)	5 = :-(
			 * 4:	Recovery  				0 ... 4  	0 = Fully Recovered ... 4 = Exhausted
			 * 5:	- Reserved -  			0
			 * 6:	Energy consumption  	376
			 * 
			 * </pre>
			 */

			line = fileReader.readLine();
			if (line == null) {
				return false;
			}
			numOfNumberRowsAnalyzed++;

			tokenLine = new StringTokenizer(line, DATA_DELIMITER);

			// column 1
			_currentExercise.sport = Integer.parseInt(tokenLine.nextToken());

			// column 2
			tokenLine.nextToken();

			// column 3
			tokenLine.nextToken();

			// column 4
			tokenLine.nextToken();

			// column 5
			tokenLine.nextToken();

			// column 6
			_currentExercise.calories = Integer.parseInt(tokenLine.nextToken());

			/**
			 * <pre>
			 * 
			 * Number Row 3
			 * 
			 * Data							Example		Format
			 * 
			 * 1:   Distance                12000       m
			 * 2:	?
			 * 3:	?
			 * 4:	?
			 * 5:	?
			 * 6:	?
			 * 
			 * </pre>
			 */

			line = fileReader.readLine();
			if (line == null) {
				return false;
			}
			numOfNumberRowsAnalyzed++;

			tokenLine = new StringTokenizer(line, DATA_DELIMITER);

			// column 1
			_currentExercise.distance = Integer.parseInt(tokenLine.nextToken());

			// column 2
			tokenLine.nextToken();

			// column 3
			tokenLine.nextToken();

			// column 4
			tokenLine.nextToken();

			// column 5
			tokenLine.nextToken();

			// column 6
			_currentExercise.calories = Integer.parseInt(tokenLine.nextToken());

			/**
			 * skip further number rows
			 */
			if (skipRows(fileReader, numOfNumberRows - numOfNumberRowsAnalyzed) == null) {
				return false;
			}

			/**
			 * <pre>
			 * 
			 * Text rows
			 * 
			 * Row 0  exercise name
			 * Row 1  exercise note text
			 * Row 2  attached hrm file (if in same folder, no folder info with file name)
			 * Row 3  hyperlink
			 * Row 4  hyperlink info text
			 * Row 5  attached location file
			 * Row 6  attached RR file
			 * Row 7  previous multisport file
			 * Row 8  next multisport file
			 * Row n  - Reserved -
			 * 
			 * </pre>
			 */

			String hrmFileName = null;
			String gpxFileName = null;

			// row 0
			line = fileReader.readLine();
			if (line == null) {
				return false;
			}
			_currentExercise.title = line;

			// row 1
			line = fileReader.readLine();
			if (line == null) {
				return false;
			}
			_currentExercise.description = line;

			// row 2
			line = fileReader.readLine();
			if (line == null) {
				return false;
			}
			if (line.length() > 0) {
				hrmFileName = line;
			}

			// row 3
			line = fileReader.readLine();
			if (line == null) {
				return false;
			}

			// row 4
			line = fileReader.readLine();
			if (line == null) {
				return false;
			}

			// row 5
			line = fileReader.readLine();
			if (line == null) {
				return false;
			}
			if (line.length() > 0) {
				gpxFileName = line;
			}

			// hrmFile and GpxFile might both be empty, Polar allows creation of an exercise without recorded data
			if (null != hrmFileName) {
				return createExercise(hrmFileName, gpxFileName);
			} else {
				return createExercise();
			}

		} catch (final Exception e) {
			StatusUtil.showStatus(e);
			return false;
		}
	}

	@Override
	public boolean processDeviceData(	final String importFilePath,
										final DeviceData deviceData,
										final HashMap<Long, TourData> alreadyImportedTours,
										final HashMap<Long, TourData> newlyImportedTours) {

		_importFilePath = importFilePath;
		_deviceData = deviceData;
		_alreadyImportedTours = alreadyImportedTours;
		_newlyImportedTours = newlyImportedTours;

		_additionalImportedFiles.clear();
		_exerciseFiles.clear();

		if (_isDebug) {
			System.out.println(importFilePath);
		}

		final boolean returnValue = parseSection();

		return returnValue;
	}

	private String skipRows(final BufferedReader fileReader, final int numberOfRows) throws IOException {

		int rowCounter = 0;

		String line = null;
		while (rowCounter < numberOfRows) {

			line = fileReader.readLine();

			if (line == null) {
				return null;
			}

			rowCounter++;
		}

		return line;
	}

	/**
	 * @return Return <code>true</code> when the file has a valid .hrm data format
	 */
	@Override
	public boolean validateRawData(final String fileName) {

		BufferedReader fileReader = null;

		try {

			fileReader = new BufferedReader(new FileReader(fileName));

			final String firstLine = fileReader.readLine();
			if (firstLine == null || firstLine.startsWith(SECTION_DAY_INFO) == false) {
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
