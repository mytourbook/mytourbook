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
package net.tourbook.data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import net.tourbook.database.TourDatabase;
import de.byteholder.gpx.GeoPosition;

@Entity
public class TourWayPoint implements Cloneable {

	/**
	 * Unique id for the {@link TourWayPoint} entity
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private final long	wayPointId		= TourDatabase.ENTITY_IS_NOT_SAVED;

	@ManyToOne(optional = false)
	private TourData	tourData;

	// initialize with invalid values
	private double		longitude		= Double.MIN_VALUE;

	private double		latitude		= Double.MIN_VALUE;

	/**
	 * absolute time
	 */
	private long		time			= 0;

	/**
	 * altitude in meters
	 */
	private float		altitude		= Float.MIN_VALUE;

	private String		name;
	private String		description;
	private String		comment;
	private String		symbol;
	private String		category;

	/**
	 * unique id for manually created markers because the {@link #markerId} is 0 when the marker is
	 * not persisted
	 */
	@Transient
	private long		createId		= 0;

	@Transient
	private GeoPosition	_geoPosition;

	/**
	 * manually created marker or imported marker create a unique id to identify them, saved marker
	 * are compared with the marker id
	 */
	private static int	_createCounter	= 0;

	public TourWayPoint() {

		this.createId = ++_createCounter;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}

		final TourWayPoint other = (TourWayPoint) obj;
		if (createId == 0) {

			// tour is from the database
			if (wayPointId != other.wayPointId) {
				return false;
			}
		} else {

			// tour was create or imported
			if (createId != other.createId) {
				return false;
			}
		}
		return true;
	}

	public float getAltitude() {
		return altitude;
	}

	public String getCategory() {
		return category;
	}

	public String getComment() {
		return comment;
	}

	public String getDescription() {
		return description;
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public String getName() {
		return name;
	}

	public GeoPosition getPosition() {

		if (_geoPosition == null) {
			_geoPosition = new GeoPosition(latitude, longitude);
		}

		return _geoPosition;
	}

	public String getSymbol() {
		return symbol;
	}

	public long getTime() {
		return time;
	}

	public TourData getTourData() {
		return tourData;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (createId ^ (createId >>> 32));
		result = prime * result + (int) (wayPointId ^ (wayPointId >>> 32));
		return result;
	}

	public void setAltitude(final float altitude) {
		this.altitude = altitude;
	}

	public void setCategory(final String category) {
		this.category = category;
	}

	public void setComment(final String comment) {
		this.comment = comment;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public void setLatitude(final double latitude) {

		this.latitude = latitude;

		if (_geoPosition != null) {
			_geoPosition.latitude = latitude;
		}
	}

	public void setLongitude(final double longitude) {

		this.longitude = longitude;

		if (_geoPosition != null) {
			_geoPosition.longitude = longitude;
		}
	}

	public void setName(final String name) {
		this.name = name;
	}

	public void setSymbol(final String symbol) {
		this.symbol = symbol;
	}

	public void setTime(final long time) {
		this.time = time;
	}

	public void setTourData(final TourData tourData) {
		this.tourData = tourData;
	}

	@Override
	public String toString() {

		final StringBuilder sb = new StringBuilder();
		sb.append("wayPointId:");
		sb.append(wayPointId);
		sb.append("\tcreateId:");
		sb.append(createId);
		sb.append("\tname:");
		sb.append(name);
		sb.append("\tlat:");
		sb.append(latitude);
		sb.append("\tlon:");
		sb.append(longitude);
		sb.append("\tdesc:");
		sb.append(description);
		sb.append("\tcom:");
		sb.append(comment);

		return sb.toString();
	}

}
