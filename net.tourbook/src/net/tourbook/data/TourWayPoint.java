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

import net.tourbook.database.TourDatabase;

@Entity
public class TourWayPoint implements Cloneable {

	/**
	 * Unique id for the {@link TourWayPoint} entity
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private final long	wayPointId	= TourDatabase.ENTITY_IS_NOT_SAVED;

	@ManyToOne(optional = false)
	private TourData	tourData;

	private double		longitude;
	private double		latitude;

	public TourWayPoint() {}

	public TourWayPoint(final TourData tourData) {

		this.tourData = tourData;

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
		if (wayPointId != other.wayPointId) {
			return false;
		}
		return true;
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (wayPointId ^ (wayPointId >>> 32));
		return result;
	}

	public void setLatitude(final double latitude) {
		this.latitude = latitude;
	}

	public void setLongitude(final double longitude) {
		this.longitude = longitude;
	}

}
