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
package net.tourbook.importdata;

import net.tourbook.data.TourData;

public abstract class TourbookDevice implements IRawDataReader {

	/**
	 * Unique id for each device reader
	 */
	public String	deviceId;

	/**
	 * Visible device name, e.g. HAC4, HAC5
	 */
	public String	visibleName;

	/**
	 * File extension used when tour data are imported from a file
	 */
	public String	fileExtension;

// disabled in version 10.10, it seems to be not used anymore
//	/**
//	 * <code>true</code> when this device reader can read from the device, <code>false</code>
//	 * (default) when the device reader can only import from a file
//	 */
//	public boolean	canReadFromDevice		= false;
//
//	/**
//	 * when <code>true</code>, multiple files can be selected in the import, default is
//	 * <code>false</code>
//	 */
//	public boolean	canSelectMultipleFilesInImportDialog	= false;

	/**
	 * when set to <code>-1</code> this is ignored otherwise this year is used as the import year
	 */
	public int		importYear				= -1;

	/**
	 * When <code>true</code> the tracks in one file will be merged into one track, a marker is
	 * created for each track
	 */
	public boolean	isMergeTracks			= false;

	/**
	 * when <code>true</code> validate the checksum when importing data
	 */
	public boolean	isChecksumValidation	= true;

	/**
	 * when <code>true</code> the tour id will be created with the recording time
	 */
	public boolean	isCreateTourIdWithRecordingTime;

	public TourbookDevice() {}

	public TourbookDevice(final String deviceName) {
		visibleName = deviceName;
	}

	public abstract String buildFileNameFromRawData(String rawDataFileName);

	/**
	 * Check if the received data are correct for this device, Returns <code>true</code> when the
	 * received data are correct for this device
	 * 
	 * @param byteIndex
	 *            index in the byte stream, this will be incremented when the return value is true
	 * @param newByte
	 *            received byte
	 * @return Return <code>true</code> when the receice data are correct for this device
	 */
	public abstract boolean checkStartSequence(int byteIndex, int newByte);

	/**
	 * Creates a unique id for the tour, {@link TourData#createTimeSeries()} must be called before
	 * to create recording time.
	 * 
	 * @param tourData
	 * @param distanceSerie
	 * @param dummyKey
	 *            The dummy key is used when distance serie is not available
	 * @return Returns a unique key for a tour
	 */
	public String createUniqueId(final TourData tourData, final int[] distanceSerie, final String dummyKey) {

		String uniqueKey;

		if (isCreateTourIdWithRecordingTime) {

			/*
			 * 25.5.2009: added recording time to the tour distance for the unique key because tour
			 * export and import found a wrong tour when exporting was done with camouflage speed ->
			 * this will result in a NEW tour
			 */
			final int tourRecordingTime = tourData.getTourRecordingTime();

			if (distanceSerie == null) {
				uniqueKey = Integer.toString(tourRecordingTime);
			} else {

				final long tourDistance = distanceSerie[(distanceSerie.length - 1)];

				uniqueKey = Long.toString(tourDistance + tourRecordingTime);
			}

		} else {

			/*
			 * original version to create tour id
			 */
			if (distanceSerie == null) {
				uniqueKey = dummyKey;
			} else {
				uniqueKey = Integer.toString(distanceSerie[distanceSerie.length - 1]);
			}
		}

		return uniqueKey;
	}

	/**
	 * @param portName
	 * @return returns the serial port parameters which are use to receive data from the device or
	 *         <code>null</code> when data transfer from a device is not supported
	 */
	public abstract SerialParameters getPortParameters(String portName);

	/**
	 * Returns the number of bytes which will be checked in the startsequence. For a HAC4/5 this can
	 * be set to 4 because the first 4 bytes of the input stream are always the characters AFRO
	 * 
	 * @return
	 */
	public abstract int getStartSequenceSize();

	public void setCreateTourIdWithTime(final boolean isCreateTourIdWithTime) {
		this.isCreateTourIdWithRecordingTime = isCreateTourIdWithTime;
	}

	public void setImportYear(final int importYear) {
		this.importYear = importYear;
	}

	public void setIsChecksumValidation(final boolean isChecksumValidation) {
		this.isChecksumValidation = isChecksumValidation;
	}

	public void setMergeTracks(final boolean isMergeTracks) {
		this.isMergeTracks = isMergeTracks;
	}

}
