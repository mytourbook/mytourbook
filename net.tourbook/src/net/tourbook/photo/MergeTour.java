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

import gnu.trove.list.array.TLongArrayList;

import java.util.ArrayList;
import java.util.HashMap;

import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.PeriodType;

public class MergeTour {

	private static PeriodType		_tourPeriodTemplate	= PeriodType.yearMonthDayTime()
														// hide these components
																.withMonthsRemoved()
																.withMinutesRemoved()
																.withSecondsRemoved()
																.withMillisRemoved();

	private boolean					isDummyTour;

	/**
	 * Contains tour id when it's a real tour, otherwise it contains {@link Long#MIN_VALUE}.
	 */
	long							tourId				= Long.MIN_VALUE;
	long							tourTypeId			= -1;

	/**
	 * Tour start time in ms
	 */
	long							tourStartTime;

	/**
	 * Tour end time is {@link Long#MAX_VALUE} when not yet set.
	 */
	long							tourEndTime			= Long.MAX_VALUE;

	private DateTime				tourStartDateTime;
	Period							tourPeriod;

	int								numberOfPhotos;
	int								numberOfGPSPhotos;
	int								numberOfNoGPSPhotos;

	HashMap<String, Camera>			cameras				= new HashMap<String, Camera>();
	Camera[]						cameraList;

	/**
	 * Contains all photos for this tour.
	 */
	public ArrayList<PhotoWrapper>	tourPhotos			= new ArrayList<PhotoWrapper>();

	private TourData				_dummyTourData;

	private TLongArrayList			_dummyTimeSerie;

	/**
	 * Constructor for a dummy tour.
	 * 
	 * @param notUsed
	 */
	MergeTour(final long tourStartTime) {

		isDummyTour = true;

		setTourStartTime(tourStartTime);

		_dummyTourData = new TourData();
		_dummyTourData.createDummyTour();

		_dummyTimeSerie = new TLongArrayList();
		_dummyTimeSerie.add(tourStartTime);
	}

	/**
	 * Constructor for a real tour.
	 * 
	 * @param tourEndTime
	 * @param tourStartTime
	 * @param tourId
	 */
	MergeTour(final long tourId, final long tourStartTime, final long tourEndTime) {

		this.tourId = tourId;

		setTourStartTime(tourStartTime);
		setTourEndTime(tourEndTime);
	}

	void addPhotoTime(final long photoTime) {
		_dummyTimeSerie.add(photoTime);
	}

	private void addTimeSlice(final ArrayList<TimeData> dtList, final long timeSliceTime) {

		final TimeData timeData = new TimeData();

		timeData.absoluteTime = timeSliceTime;
		timeData.absoluteAltitude = 1.0f;

		dtList.add(timeData);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof MergeTour)) {
			return false;
		}
		final MergeTour other = (MergeTour) obj;
		if (tourId != other.tourId) {
			return false;
		}
		return true;
	}

	private void finalizeDummyTour() {

		final ArrayList<TimeData> dtList = new ArrayList<TimeData>();

		final long[] dummyTimeSerie = _dummyTimeSerie.toArray();

		final long tourStart = dummyTimeSerie[0];
		final long tourEnd = dummyTimeSerie[dummyTimeSerie.length - 1];

		if (dummyTimeSerie.length == 1) {

			// only 1 point is visible

			tourStartTime = tourStart - 1000;
			tourEndTime = tourStart + 1000;

		} else {

			// set offset to 5% tour time

			final long timeDiff = tourEnd - tourStart;
			final long timeOffset = (long) (timeDiff * 0.05);

			tourStartTime = tourStart - timeOffset;
			tourEndTime = tourEnd + timeOffset;
		}

		/*
		 * adjust start and end that the dummy tour do not start at the chart border
		 */

		// update adjusted start
		tourStartDateTime = new DateTime(tourStartTime);

		/*
		 * set tour start time line before first time slice
		 */
		addTimeSlice(dtList, tourStartTime);

		/*
		 * create time data list for all time slices which contains photos
		 */
		for (final long timeSliceTime : dummyTimeSerie) {
			addTimeSlice(dtList, timeSliceTime);
		}

		/*
		 * set tour end time after the last time slice
		 */
		addTimeSlice(dtList, tourEndTime);

		_dummyTourData.setTourStartTime(tourStartDateTime);
		_dummyTourData.createTimeSeries(dtList, false);
	}

	public TourData getDummyTourData() {
		return _dummyTourData;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (tourId ^ (tourId >>> 32));
		return result;
	}

	public boolean isDummyTour() {
		return isDummyTour;
	}

	void setTourEndTime(long endTime) {

		// ensure that a time difference of at least 1 second is set for a tour
		if (endTime < (tourStartTime + 1000)) {
			endTime = tourStartTime + 1000;
		}

		tourEndTime = endTime;

		if (isDummyTour) {
			finalizeDummyTour();
		}

		// set tour period AFTER dummy tour is finalized
		tourPeriod = new Period(tourStartTime, tourEndTime, _tourPeriodTemplate);
	}

	private void setTourStartTime(final long time) {

		tourStartTime = time;
		tourStartDateTime = new DateTime(time);
	}

}
