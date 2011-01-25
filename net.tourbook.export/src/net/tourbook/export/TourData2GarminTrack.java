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

import java.util.ArrayList;
import java.util.Set;

import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourWayPoint;

import org.dinopolis.gpstool.gpsinput.garmin.GarminTrack;
import org.dinopolis.gpstool.gpsinput.garmin.GarminTrackpointAdapter;
import org.dinopolis.gpstool.gpsinput.garmin.GarminTrackpointD304;
import org.dinopolis.gpstool.gpsinput.garmin.GarminWaypoint;
import org.dinopolis.gpstool.gpsinput.garmin.GarminWaypointBase;
import org.joda.time.DateTime;

/**
 * Converts {@link TourData} to {@link GarminTrack}.
 */
public class TourData2GarminTrack {

	private final boolean	_isExportTourPart;
	private final int		_tourStartIndex;
	private final int		_tourEndIndex;

	private DateTime		_lastTrackDateTime	= null;

	public TourData2GarminTrack() {

		this(false, -1, -1);
	}

	public TourData2GarminTrack(final boolean isExportTourPart,
								final int tourStartIndex,
								final int tourEndIndex) {
		this._isExportTourPart = isExportTourPart;
		this._tourStartIndex = tourStartIndex;
		this._tourEndIndex = tourEndIndex;
	}

	public GarminTrack getTrack(final TourData tourData,
								final DateTime trackDateTime,
								final boolean isCamouflageSpeed,
								final float camouflageSpeed) {

		final GarminTrack track = new GarminTrack();

		final int[] timeSerie = tourData.timeSerie;
		final int[] altitudeSerie = tourData.altitudeSerie;
		final int[] distanceSerie = tourData.distanceSerie;
		final int[] cadenceSerie = tourData.cadenceSerie;
		final int[] pulseSerie = tourData.pulseSerie;
		final double[] latitudeSerie = tourData.latitudeSerie;
		final double[] longitudeSerie = tourData.longitudeSerie;

		// check if all dataseries are available
		if ((timeSerie == null) || (latitudeSerie == null) || (longitudeSerie == null)) {
			return null;
		}

		final boolean isAltitude = (altitudeSerie != null) && (altitudeSerie.length > 0);
		final boolean isDistance = (distanceSerie != null) && (distanceSerie.length > 0);
		final boolean isPulse = (pulseSerie != null) && (pulseSerie.length > 0);
		final boolean isCadence = (cadenceSerie != null) && (cadenceSerie.length > 0);

		int prevTime = -1;

		// default is to use all trackpoints
		int startIndex = 0;
		int endIndex = timeSerie.length - 1;

		// adjust start/end when a part is exported
		if (isExportTourPart()) {
			startIndex = _tourStartIndex;
			endIndex = _tourEndIndex;
		}

		// set track name
		final String tourTitle = tourData.getTourTitle();
		if (tourTitle.length() > 0) {
			track.setIdentification(tourTitle);
		}

		/*
		 * loop: trackpoints
		 */
		for (int serieIndex = startIndex; serieIndex <= endIndex; serieIndex++) {

			final GarminTrackpointD304 tp304 = new GarminTrackpointD304();
			final GarminTrackpointAdapter trackPoint = new GarminTrackpointAdapter(tp304);

			// mark as a new track to create the <trkseg>...</trkseg> tags
			if (serieIndex == startIndex) {
				trackPoint.setNewTrack(true);
			}

			if (isAltitude) {
				trackPoint.setAltitude(altitudeSerie[serieIndex]);
			}

			trackPoint.setLongitude(longitudeSerie[serieIndex]);
			trackPoint.setLatitude(latitudeSerie[serieIndex]);

			int currentTime;

			if (isCamouflageSpeed && isDistance) {

				// camouflage speed

				currentTime = (int) (distanceSerie[serieIndex] / camouflageSpeed);

			} else {

				// keep recorded speed

				currentTime = timeSerie[serieIndex];
			}

			if (isDistance) {
				trackPoint.setDistance(distanceSerie[serieIndex]);
			}

			if (isCadence) {
				tp304.setCadence((short) cadenceSerie[serieIndex]);
			}

			if (isPulse) {
				tp304.setHeartrate((short) pulseSerie[serieIndex]);
			}

			// ignore trackpoints which have the same time
			if (currentTime != prevTime) {

				_lastTrackDateTime = trackDateTime.plusSeconds(currentTime);
				trackPoint.setDate(_lastTrackDateTime.toDate());

				track.addWaypoint(trackPoint);
			}

			prevTime = currentTime;
		}

		return track;
	}

	public GarminLap getLap(final TourData tourData, final boolean addNotes) {

		final GarminLap lap = new GarminLap();

		lap.setCalories(tourData.getCalories());

		if (addNotes) {
			final String notes = tourData.getTourDescription();
			if ((notes != null) && (notes.length() > 0)) {
				lap.setNotes(notes);
			}
		}
		return lap;
	}

	public void getWaypoints(final ArrayList<GarminWaypoint> wayPointList, final TourData tourData) {

		final int[] timeSerie = tourData.timeSerie;
		final int[] altitudeSerie = tourData.altitudeSerie;
		final double[] latitudeSerie = tourData.latitudeSerie;
		final double[] longitudeSerie = tourData.longitudeSerie;
		final Set<TourMarker> tourMarkers = tourData.getTourMarkers();
		final Set<TourWayPoint> tourWayPoints = tourData.getTourWayPoints();

		// check if all dataseries are available
		if ((timeSerie == null) || (latitudeSerie == null) || (longitudeSerie == null) || (tourMarkers == null)) {
			return;
		}

		// default is to use all trackpoints
		int startIndex = 0;
		int endIndex = timeSerie.length - 1;
		boolean isRange = false;

		// adjust start/end when a part is exported
		if (isExportTourPart()) {
			startIndex = _tourStartIndex;
			endIndex = _tourEndIndex;
			isRange = true;
		}

		// convert marker into way points
		for (final TourMarker tourMarker : tourMarkers) {

			final int serieIndex = tourMarker.getSerieIndex();

			// skip markers when they are not in the defined range
			if (isRange) {
				if ((serieIndex < startIndex) || (serieIndex > endIndex)) {
					continue;
				}
			}

			final GarminWaypointBase wayPoint = new GarminWaypointBase();
			wayPointList.add(wayPoint);

			wayPoint.setLatitude(latitudeSerie[serieIndex]);
			wayPoint.setLongitude(longitudeSerie[serieIndex]);

			// <name>...<name>
			wayPoint.setIdentification(tourMarker.getLabel());

			// <ele>...</ele>
			if (altitudeSerie != null) {
				wayPoint.setAltitude(altitudeSerie[serieIndex]);
			}
		}

		for (final TourWayPoint twp : tourWayPoints) {

			final GarminWaypointBase wayPoint = new GarminWaypointBase();
			wayPointList.add(wayPoint);

			wayPoint.setLatitude(twp.getLatitude());
			wayPoint.setLongitude(twp.getLongitude());

			// <name>...<name>
			wayPoint.setIdentification(twp.getName());

			// <ele>...</ele>
			if (altitudeSerie != null) {
				wayPoint.setAltitude(twp.getAltitude());
			}

// !!! THIS IS NOT WORKING 2010-07-17 !!!
//			// <desc>...</desc>
//			final String comment = twp.getComment();
//			final String description = twp.getComment();
//			final String descText = description != null ? description : comment;
//			if (descText != null) {
//				wayPoint.setComment(descText);
//			}
//
//			// <sym>...</sym>
//			wayPoint.setSymbolName(twp.getSymbol());
		}
	}

	public DateTime getLastTrackDateTime() {
		return _lastTrackDateTime;
	}

	/**
	 * @return Return <code>true</code> when a part of a tour can be exported
	 */
	public boolean isExportTourPart() {
		return _isExportTourPart;
	}
}
