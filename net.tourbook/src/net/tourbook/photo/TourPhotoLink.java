/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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
package net.tourbook.photo;

import java.util.ArrayList;

import net.tourbook.data.HistoryData;
import net.tourbook.data.TourData;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class TourPhotoLink {

	private static final PeriodType			_tourPeriodTemplate	= PeriodType.yearMonthDayTime()
																// hide these components
																		.withMinutesRemoved()
																		.withSecondsRemoved()
																		.withMillisRemoved();

	private static final DateTimeFormatter	_dtFormatter		= DateTimeFormat.forStyle("SL");	//$NON-NLS-1$

	boolean									isHistoryTour;

	/**
	 * Contains tour id when it's a real tour, otherwise it contains {@link Long#MIN_VALUE}.
	 */
	public long								tourId				= Long.MIN_VALUE;

	long									tourTypeId			= -1;

	/**
	 * Unique id for this link.
	 */
	long									linkId;

	/**
	 * Tour start time in ms
	 */
	long									tourStartTime;

	/**
	 * Tour end time in ms.
	 */
	long									tourEndTime			= Long.MIN_VALUE;

	long									historyStartTime	= Long.MIN_VALUE;
	long									historyEndTime		= Long.MIN_VALUE;

	private DateTime						tourStartDateTime;
	Period									tourPeriod;

	int										numberOfGPSPhotos;
	int										numberOfNoGPSPhotos;

	/**
	 * Number of photos which are saved in a real tour.
	 */
	int										numberOfTourPhotos;

	/**
	 * Adjusted time in seconds.
	 */
	int										photoTimeAdjustment;

	/**
	 * Contains all photos for this tour.
	 */
	public ArrayList<Photo>					linkPhotos			= new ArrayList<Photo>();

	private TourData						_historyTourData;

	/**
	 * Contains names for all cameras which are used to take pictures for the current tour.
	 */
	String									tourCameras;

	/**
	 * Constructor for a history tour.
	 * 
	 * @param notUsed
	 */
	TourPhotoLink(final long tourStartTime) {

		isHistoryTour = true;

		linkId = System.nanoTime();

		setTourStartTime(tourStartTime);

		_historyTourData = new TourData();
		_historyTourData.setupHistoryTour();
	}

	/**
	 * Constructor for a real tour.
	 * 
	 * @param tourEndTime
	 * @param tourStartTime
	 * @param tourId
	 * @param dbPhotoTimeAdjustment
	 * @param dbNumberOfPhotos
	 */
	TourPhotoLink(	final long tourId,
					final long tourStartTime,
					final long tourEndTime,
					final int numberOfPhotos,
					final int dbPhotoTimeAdjustment) {

		this.tourId = tourId;

		linkId = tourId;

		setTourStartTime(tourStartTime);
		setTourEndTime(tourEndTime);

		numberOfTourPhotos = numberOfPhotos;

		tourPeriod = new Period(tourStartTime, tourEndTime, _tourPeriodTemplate);

		photoTimeAdjustment = dbPhotoTimeAdjustment;
	}

	private void addTimeSlice(final ArrayList<HistoryData> historyList, final long timeSliceTime) {

		final HistoryData historyData = new HistoryData();

		historyData.absoluteTime = timeSliceTime / 1000 * 1000;

		historyList.add(historyData);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof TourPhotoLink)) {
			return false;
		}
		final TourPhotoLink other = (TourPhotoLink) obj;
		if (linkId != other.linkId) {
			return false;
		}
		return true;
	}

	private void finalizeHistoryTour() {

		/*
		 * create time data serie
		 */
		final int timeSerieLength = linkPhotos.size();
		final long[] historyTimeSerie = new long[timeSerieLength];

		for (int photoIndex = 0; photoIndex < timeSerieLength; photoIndex++) {
			historyTimeSerie[photoIndex] = linkPhotos.get(photoIndex).adjustedTime;
		}

		final long tourStart = historyTimeSerie[0];
		final long tourEnd = historyTimeSerie[timeSerieLength - 1];

		historyStartTime = tourStart;
		historyEndTime = tourEnd;

		if (timeSerieLength == 1) {

			// only 1 point is visible

			tourStartTime = tourStart - 1000;
			tourEndTime = tourStart + 1000;

		} else {

			// add additional 3% tour time that the tour do not start/end at the chart border

			final double timeDiff = tourEnd - tourStart;

			/**
			 * very important: round to 0 ms
			 */
			long timeOffset = ((long) (timeDiff * 0.03) / 1000) * 1000;

			// ensure there is a time difference of 1 second
			if (timeOffset == 0) {
				timeOffset = 1000;
			}

			tourStartTime = tourStart - timeOffset;
			tourEndTime = tourEnd + timeOffset;
		}

		// update adjusted start
		tourStartDateTime = new DateTime(tourStartTime);

		/*
		 * adjust start and end that the dummy tour do not start at the chart border
		 */

		final ArrayList<HistoryData> historySlices = new ArrayList<HistoryData>();

		/*
		 * set tour start time line before first time slice
		 */
		addTimeSlice(historySlices, tourStartTime);

		/*
		 * create time data list for all time slices which contains photos
		 */
		long prevTimeSliceTime = Long.MIN_VALUE;
		for (final long timeSliceTime : historyTimeSerie) {

			// skip duplicates
			if (timeSliceTime == prevTimeSliceTime) {
				continue;
			}

			addTimeSlice(historySlices, timeSliceTime);

			prevTimeSliceTime = timeSliceTime;
		}

		/*
		 * set tour end time after the last time slice
		 */
		addTimeSlice(historySlices, tourEndTime);

		_historyTourData.setTourStartTime(tourStartDateTime);
		_historyTourData.createHistoryTimeSerie(historySlices);
	}

	public TourData getHistoryTourData() {
		return _historyTourData;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (linkId ^ (linkId >>> 32));
		return result;
	}

	public boolean isHistoryTour() {
		return isHistoryTour;
	}

	void setTourEndTime(final long endTime) {

		if (isHistoryTour) {

			final int photosSize = linkPhotos.size();

			if (photosSize == 0) {
				// there are no photos in this history tour, THIS SHOULD NOT HAPPEN FOR A HISTORY TOUR
				tourEndTime = tourStartTime;
			} else {
				// get time from last photo
				tourEndTime = linkPhotos.get(photosSize - 1).adjustedTime;
			}

			finalizeHistoryTour();

		} else {

			tourEndTime = endTime;
		}

		// set tour period AFTER history tour is finalized
		tourPeriod = new Period(tourStartTime, tourEndTime, _tourPeriodTemplate);
	}

	private void setTourStartTime(final long time) {

		// remove milliseconds
		tourStartTime = time / 1000 * 1000;
		tourStartDateTime = new DateTime(time);
	}

	@Override
	public String toString() {
		return "TourPhotoLink " //$NON-NLS-1$
//				+ ("\n\ttourStart=\t\t" + tourStartTime)
				+ ("\n\ttourStart=\t\t" + _dtFormatter.print(tourStartTime)) //$NON-NLS-1$
//				+ ("\n\ttourEnd=\t\t" + _dtFormatter.print(tourEndTime))
				+ ("\n\thistoryStartTime=\t" + _dtFormatter.print(historyStartTime)) //$NON-NLS-1$
//				+ ("\n\thistoryEndTime=\t\t" + _dtFormatter.print(historyEndTime))
				+ ("\n\tisHistory=" + isHistoryTour) //$NON-NLS-1$
				+ ("\tlinkId=" + linkId) //$NON-NLS-1$
				+ ("\n") //$NON-NLS-1$
		//
		;
	}

}
