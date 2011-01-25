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
package net.tourbook.export;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.tourbook.data.TourData;
import net.tourbook.tour.TourManager;

import org.dinopolis.gpstool.gpsinput.garmin.GarminTrack;
import org.dinopolis.gpstool.gpsinput.garmin.GarminWaypoint;

/**
 * Exports tours without user interaction (without export dialog).
 */
public class HeadlessExportTour {

	private final AbstractExportTourFormat	_exportFormat;
	private final boolean					_isExportMarkers;
	private final boolean					_isExportNotes;
	private final boolean					_isCamouflageSpeed;
	private final float						_camouflageSpeed;

	/**
	 * Simple constructor using default settings
	 * 
	 * @param exportFormat the file format to export the tours to
	 */
	public HeadlessExportTour(final AbstractExportTourFormat exportFormat) {

		this(exportFormat, true, false, false, 0);
	}

	/**
	 * Constructor with settings how to export tours (correspond with the settings in the export
	 * dialog).
	 * 
	 * @param exportFormat
	 *            the file format to export the tours to
	 * @param isExportMarkers
	 *            flag if markers and waypoints will be exported (true) or not (false)
	 * @param isExportNotes
	 *            flag if notes (tour description) will be exported (true) or not (false)
	 * @param isCamouflageSpeed
	 *            flag if tour speed will be anonymized (true) or not (false)
	 * @param camouflageSpeed
	 *            speed to anonymize the tour to (only if <code>isCamouflageSpeed</code> is set)
	 */
	public HeadlessExportTour(	final AbstractExportTourFormat exportFormat,
								final boolean isExportMarkers,
								final boolean isExportNotes,
								final boolean isCamouflageSpeed,
								final float camouflageSpeed) {
		this._exportFormat = exportFormat;
		this._isExportMarkers = isExportMarkers;
		this._isExportNotes = isExportNotes;
		this._isCamouflageSpeed = isCamouflageSpeed;
		this._camouflageSpeed = camouflageSpeed;
	}

	/**
	 * Exports the tours in the {@link TourData} list without user interaction (does not
	 * open the export dialog).
	 * 
	 * @param tourDataList
	 *            one or more tours to be exported
	 * @param targetDir
	 *            directory to which the exported files will be written
	 * @return list of the complete path of each exported file
	 * @throws IOException
	 *             when writing the export file went wrong
	 */
	public List<String> doExportTour(final List<TourData> tourDataList, final String targetDir) throws IOException {

		List<String> allFilePath = new ArrayList<String>();
		for (TourData tourData : tourDataList) {

			final String filePath = doExportTour(tourData, targetDir, -1, -1);
			allFilePath.add(filePath);
		}
		return allFilePath;
	}

	/**
	 * Exports a single tour without user interaction (does not open the export dialog).
	 * 
	 * @param tourData
	 *            tour to be exported
	 * @param targetDir
	 *            directory to which the exported file will be written
	 * @return complete path of the exported file
	 * @throws IOException
	 *             when writing the export file went wrong
	 */
	public String doExportTour(final TourData tourData, final String targetDir) throws IOException {

		return doExportTour(tourData, targetDir, -1, -1);
	}

	/**
	 * Exports a single tour without user interaction (does not open the export dialog). To export
	 * only a part of the tour use <code>tourStartIndex</code> and <code>tourEndIndex</code> to
	 * determine the range of points to be exported. Set to <code>-1</code> to export the whole
	 * tour.
	 * 
	 * @param tourData
	 *            tour to be exported
	 * @param targetDir
	 *            directory to which the exported file will be written
	 * @param tourStartIndex
	 *            start index of the tour points to include in the export, -1 to export whole tour
	 * @param tourEndIndex
	 *            end index of the tour points to include in the export, -1 to export whole tour
	 * @return complete path of the exported file
	 * @throws IOException
	 *             when writing the export file went wrong
	 */
	public String doExportTour(	final TourData tourData,
								final String targetDir,
								final int tourStartIndex,
								final int tourEndIndex) throws IOException {

		final ArrayList<GarminTrack> trackList = new ArrayList<GarminTrack>();
		final ArrayList<GarminWaypoint> wayPointList = new ArrayList<GarminWaypoint>();

		final boolean isExportTourPart = (tourStartIndex != -1) && (tourEndIndex != -1);
		final TourData2GarminTrack converter = new TourData2GarminTrack(isExportTourPart, tourStartIndex, tourEndIndex);
		final GarminLap tourLap = converter.getLap(tourData, _isExportNotes);
		final GarminTrack track = converter.getTrack(
				tourData,
				TourManager.getTourDateTime(tourData),
				_isCamouflageSpeed,
				_camouflageSpeed);

		if (track != null) {
			trackList.add(track);
		}

		if (_isExportMarkers) {
			// get markers when this option is checked
			converter.getWaypoints(wayPointList, tourData);
		}

		final String fileName = ExportUtil.getFileName(tourData, _exportFormat.getFileExtension());
		final String filePath = new StringBuilder(targetDir).append(File.separatorChar).append(fileName).toString();

		final VelocityTourExporter tourExporter = new VelocityTourExporter(_exportFormat.getFormatTemplate());
		tourExporter.doExportTour(tourLap, trackList, wayPointList, filePath);
		
		return filePath;
	}
}
