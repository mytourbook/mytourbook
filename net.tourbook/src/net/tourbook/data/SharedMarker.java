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
package net.tourbook.data;

import static javax.persistence.CascadeType.ALL;
import static javax.persistence.FetchType.LAZY;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Transient;

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.database.TourDatabase;

/**
 * Shared marker entity.
 */
@Entity
public class SharedMarker implements Cloneable, Comparable<Object> {

	/**
	 * Unique id for a {@link SharedMarker} entity.
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long				sharedMarkerId	= TourDatabase.ENTITY_IS_NOT_SAVED;

	/**
	 * Contains all tours are associated with this tag.
	 */
	@ManyToMany(mappedBy = "sharedMarker", cascade = ALL, fetch = LAZY)
	private final Set<TourData>	tourData		= new HashSet<TourData>();

	private String				name;
	private String				description;
	private String				comment;

	// initialize with invalid values
	private double				longitude		= Double.MIN_VALUE;
	private double				latitude		= Double.MIN_VALUE;

	/**
	 * Altitude in meters.
	 */
	private float				altitude		= Float.MIN_VALUE;

	/**
	 * Can be <code>null</code>
	 * 
	 * @since db version 24
	 */
	private String				urlText;

	/**
	 * Can be <code>null</code>
	 * 
	 * @since db version 24
	 */
	private String				urlAddress;

	/**
	 * Unique id for manually created shared markers because the {@link #sharedMarkerId} is 0 when
	 * the marker is not persisted.
	 */
	@Transient
	private long				_createId		= 0;

	/**
	 * A manually created shared marker creates a unique id to identify it, saved shared markers are
	 * compared with the {@link #sharedMarkerId}.
	 */
	@Transient
	private static int			_createCounter	= 0;

	public SharedMarker() {}

	public SharedMarker clone(final TourData wpTourData) {

		try {

			// create a shallow copy
			final SharedMarker newSharedMarker = (SharedMarker) super.clone();

			// set create id to uniquely identify the shared marker
			newSharedMarker._createId = ++_createCounter;

			newSharedMarker.sharedMarkerId = TourDatabase.ENTITY_IS_NOT_SAVED;

//			newWayPoint.tourData = wpTourData;

			return newSharedMarker;

		} catch (final CloneNotSupportedException e) {
			StatusUtil.log(e);
		}

		return null;
	}

	@Override
	public int compareTo(final Object other) {

		/*
		 * set default sorting by time or by id (creation time)
		 */

		if (other instanceof SharedMarker) {

			final SharedMarker otherSharedMarker = (SharedMarker) other;

			if (_createId == 0) {

				if (otherSharedMarker._createId == 0) {

					// both shared markers are persisted
					return sharedMarkerId > otherSharedMarker.sharedMarkerId ? 1 : -1;
				}

				return 1;

			} else {

				// _createId != 0

				if (otherSharedMarker._createId != 0) {

					// both shared markers are created and not persisted
					return _createId > otherSharedMarker._createId ? 1 : -1;
				}

				return -1;
			}
		}

		return 0;
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

		final SharedMarker other = (SharedMarker) obj;
		if (_createId == 0) {

			// tour is from the database
			if (sharedMarkerId != other.sharedMarkerId) {
				return false;
			}
		} else {

			// tour was create or imported
			if (_createId != other._createId) {
				return false;
			}
		}
		return true;
	}

	public float getAltitude() {
		return altitude;
	}

	public String getComment() {
		return comment;
	}

	/**
	 * @return Returns a unique id for manually created shared marker because the
	 *         {@link #sharedMarkerId} is {@link TourDatabase#ENTITY_IS_NOT_SAVED} when it's not yet
	 *         persisted.
	 */
	public long getCreateId() {
		return _createId;
	}

	public String getDescription() {
		return description;
	}

	/**
	 * @return Returns the entity id or {@link TourDatabase#ENTITY_IS_NOT_SAVED} when the entity is
	 *         not yet saved.
	 */
	public long getId() {
		return sharedMarkerId;
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

	public Set<TourData> getTourData() {
		return tourData;
	}

	public String getUrlAddress() {
		return urlAddress == null ? UI.EMPTY_STRING : urlAddress;
	}

	public String getUrlText() {
		return urlText == null ? UI.EMPTY_STRING : urlText;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (_createId ^ (_createId >>> 32));
		result = prime * result + (int) (sharedMarkerId ^ (sharedMarkerId >>> 32));
		return result;
	}

	/**
	 * Set altitude in meters.
	 * 
	 * @param altitude
	 */
	public void setAltitude(final float altitude) {
		this.altitude = altitude;
	}

	public void setComment(final String comment) {
		this.comment = comment;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public void setLatitude(final double latitude) {

		this.latitude = latitude;
	}

	public void setLongitude(final double longitude) {

		this.longitude = longitude;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public void setUrlAddress(final String urlAddress) {
		this.urlAddress = urlAddress;
	}

	public void setUrlText(final String urlText) {
		this.urlText = urlText;
	}

}
