/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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
package net.tourbook.device.csv.tours;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.SerialParameters;
import net.tourbook.importdata.TourbookDevice;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.TourTypeColorDefinition;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;

import org.eclipse.swt.widgets.Display;

public class CSVTourDataReader extends TourbookDevice {

	//
	// csv import data samples
	//
	// Date (yyyy-mm-dd); Time (hh-mm); Duration (sec); Paused Time (sec), Distance (m); Title; Comment; Tour Type; Tags;
	// 2008-09-02;08-20;1200;300;8500;zur Arbeit;kein Kommentar, siehe nächste Tour;Rennvelo;Arbeitsfahrt am Abend, new tag
	// 2008-09-01;14-30;1500;20;6000;auf Fremersberg;;MTB;FB
	// 2008-08-28;18-00;780;120;12000;Feierabendrunde;;TestTourType;no tags

	private static final String	TOUR_CSV_ID			= "Date (yyyy-mm-dd); Time (hh-mm); Duration (sec); Paused Time (sec), Distance (m); Title; Comment; Tour Type; Tags;"; //$NON-NLS-1$

	/**
	 * This header is a modified header for {@link #TOUR_CSV_ID} with a semikolon instead of a komma
	 * after
	 * <p>
	 * <i>Paused Time (sec);</i>
	 */
	private static final String	TOUR_CSV_ID_2		= "Date (yyyy-mm-dd); Time (hh-mm); Duration (sec); Paused Time (sec); Distance (m); Title; Comment; Tour Type; Tags;"; //$NON-NLS-1$

	private static final String	CSV_TOKEN_SEPARATOR	= ";";																													//$NON-NLS-1$
	private static final String	CSV_TAG_SEPARATOR	= ",";																													//$NON-NLS-1$

	private class DateTimeData {
		public int	year;
		public int	month;
		public int	day;
		public int	hour;
		public int	minute;
	}

	// plugin constructor
	public CSVTourDataReader() {}

	@Override
	public String buildFileNameFromRawData(final String rawDataFileName) {
		return null;
	}

	@Override
	public boolean checkStartSequence(final int byteIndex, final int newByte) {
		return false;
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

	private boolean isFileValid(final String fileHeader) {

		if (fileHeader.startsWith(TOUR_CSV_ID) || fileHeader.startsWith(TOUR_CSV_ID_2)) {
			return true;
		}

		return false;
	}

	private void parseDate(final DateTimeData dateTime, final String nextToken) {

		// Date (yyyy-mm-dd)
		dateTime.year = Integer.parseInt(nextToken.substring(0, 4));
		dateTime.month = Integer.parseInt(nextToken.substring(5, 7));
		dateTime.day = Integer.parseInt(nextToken.substring(8, 10));
	}

	/**
	 * @param tourData
	 * @param tagToken
	 * @return <code>true</code> when a new tag is created
	 */
	private boolean parseTags(final TourData tourData, final String tagToken) {

		boolean isNewTag = false;

		final StringTokenizer tokenizer = new StringTokenizer(tagToken, CSV_TAG_SEPARATOR);
		final Set<TourTag> tourTags = new HashSet<TourTag>();

		HashMap<Long, TourTag> tourTagMap = TourDatabase.getAllTourTags();
		TourTag[] allTourTags = tourTagMap.values().toArray(new TourTag[tourTagMap.size()]);

		try {

			String tagLabel;

			while ((tagLabel = tokenizer.nextToken()) != null) {

				tagLabel = tagLabel.trim();
				boolean isTagAvailable = false;

				for (final TourTag tourTag : allTourTags) {
					if (tourTag.getTagName().equals(tagLabel)) {

						// existing tag is found

						isTagAvailable = true;

						tourTags.add(tourTag);
						break;
					}
				}

				if (isTagAvailable == false) {

					// create a new tag

					final TourTag tourTag = new TourTag(tagLabel);
					tourTag.setRoot(true);

					// persist tag
					final TourTag savedTag = TourDatabase.saveEntity(
							tourTag,
							TourDatabase.ENTITY_IS_NOT_SAVED,
							TourTag.class);

					if (savedTag != null) {

						tourTags.add(savedTag);

						// reload tour tag list

						TourDatabase.clearTourTags();

						tourTagMap = TourDatabase.getAllTourTags();
						allTourTags = tourTagMap.values().toArray(new TourTag[tourTagMap.size()]);

						isNewTag = true;
					}
				}
			}

		} catch (final NoSuchElementException e) {
			// no further tokens
		} finally {

			tourData.setTourTags(tourTags);
		}

		return isNewTag;

	}

	private void parseTime(final DateTimeData dateTime, final String nextToken) {

		// Time (hh-mm)
		dateTime.hour = Integer.parseInt(nextToken.substring(0, 2));
		dateTime.minute = Integer.parseInt(nextToken.substring(3, 5));
	}

	/**
	 * @param tourData
	 * @param parsedTourTypeLabel
	 * @return <code>true</code> when a new {@link TourType} is created
	 */
	private boolean parseTourType(final TourData tourData, final String parsedTourTypeLabel) {

		final ArrayList<TourType> tourTypeMap = TourDatabase.getAllTourTypes();
		TourType tourType = null;

		// find tour type in existing tour types
		for (final TourType mapTourType : tourTypeMap) {
			if (parsedTourTypeLabel.equalsIgnoreCase(mapTourType.getName())) {
				tourType = mapTourType;
				break;
			}
		}

		TourType newSavedTourType = null;

		if (tourType == null) {

			// create new tour type

			final TourType newTourType = new TourType(parsedTourTypeLabel);

			final TourTypeColorDefinition newColorDefinition = new TourTypeColorDefinition(
					newTourType,
					Long.toString(newTourType.getTypeId()),
					newTourType.getName());

			newTourType.setColorBright(newColorDefinition.getGradientBright_Default());
			newTourType.setColorDark(newColorDefinition.getGradientDark_Default());
			newTourType.setColorLine(newColorDefinition.getLineColor_Default());
			newTourType.setColorText(newColorDefinition.getTextColor_Default());

			// save new entity
			newSavedTourType = TourDatabase.saveEntity(newTourType, newTourType.getTypeId(), TourType.class);
			if (newSavedTourType != null) {

				tourType = newSavedTourType;

				TourDatabase.clearTourTypes();
				TourManager.getInstance().clearTourDataCache();
			}
		}

		tourData.setTourType(tourType);

		return newSavedTourType != null;
	}

	@Override
	public boolean processDeviceData(	final String importFilePath,
										final DeviceData deviceData,
										final HashMap<Long, TourData> alreadyImportedTours,
										final HashMap<Long, TourData> newlyImportedTours) {

		boolean returnValue = false;

		boolean isNewTag = false;
		boolean isNewTourType = false;

		BufferedReader fileReader = null;

		try {

			fileReader = new BufferedReader(new FileReader(importFilePath));

			// check file header
			final String fileHeader = fileReader.readLine();
			if (isFileValid(fileHeader) == false) {
				return false;
			}

//			StringTokenizer tokenizer;
			String tokenLine;

			// read all tours, each line is one tour
			while ((tokenLine = fileReader.readLine()) != null) {

				int distance = 0;
				int duration = 0;
				int pausedTime = 0;

				final TourData tourData = new TourData();

				try {

					final DateTimeData dateTime = new DateTimeData();

					/*
					 * The split method is used because the Tokenizer ignores empty tokens !!!
					 */

					final String[] allToken = tokenLine.split(CSV_TOKEN_SEPARATOR);

					parseDate(dateTime, allToken[0]);//							1 Date (yyyy-mm-dd);
					parseTime(dateTime, allToken[1]);//							2 Time (hh-mm);
					tourData.setTourStartTime(
							dateTime.year,
							dateTime.month,
							dateTime.day,
							dateTime.hour,
							dateTime.minute,
							0);

					duration = Integer.parseInt(allToken[2]); //				3 Duration (sec);
					tourData.setTourRecordingTime(duration);

					pausedTime = Integer.parseInt(allToken[3]); //				4 Paused Time (sec),
					tourData.setTourDrivingTime(Math.max(0, duration - pausedTime));

					distance = Integer.parseInt(allToken[4]);//					5 Distance (m);
					tourData.setTourDistance(distance);

					tourData.setTourTitle(allToken[5]);//						6 Title;
					tourData.setTourDescription(allToken[6]);//					7 Comment;

					isNewTourType |= parseTourType(tourData, allToken[7]);//	8 Tour Type;
					isNewTag |= parseTags(tourData, allToken[8]);//				9 Tags;

				} catch (final NoSuchElementException e) {
					// not all tokens are defined
				} finally {

					tourData.importRawDataFile = importFilePath;
					tourData.setTourImportFilePath(importFilePath);

					tourData.setDeviceId(deviceId);
					tourData.setDeviceName(visibleName);

					// after all data are added, the tour id can be created
					final String uniqueId = createUniqueId_Legacy(tourData, distance);
					final Long tourId = tourData.createTourId(uniqueId);

					// check if the tour is in the tour map
					if (alreadyImportedTours.containsKey(tourId) == false) {

						// add new tour to the map
						newlyImportedTours.put(tourId, tourData);

						returnValue = true;
					}
				}
			}

		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			try {
				if (fileReader != null) {
					fileReader.close();
				}

				final Display display = Display.getDefault();

				if (isNewTag) {

					// fire modify event

					display.syncExec(new Runnable() {
						public void run() {
							TourManager.fireEvent(TourEventId.TAG_STRUCTURE_CHANGED);
						}
					});
				}

				if (isNewTourType) {

					// fire modify event

					display.syncExec(new Runnable() {
						public void run() {
							TourbookPlugin
									.getDefault()
									.getPreferenceStore()
									.setValue(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED, Math.random());
						}
					});
				}

			} catch (final IOException e) {
				e.printStackTrace();
			}
		}

		return returnValue;
	}

	/**
	 * checks if the data file has a valid .crp data format
	 * 
	 * @return true for a valid .crp data format
	 */
	public boolean validateRawData(final String fileName) {

		BufferedReader fileReader = null;

		try {

			fileReader = new BufferedReader(new FileReader(fileName));

			final String fileHeader = fileReader.readLine();
			if (fileHeader == null || isFileValid(fileHeader) == false) {
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
