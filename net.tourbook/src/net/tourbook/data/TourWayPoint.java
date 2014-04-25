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

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.map.GeoPosition;
import net.tourbook.common.util.IHoveredArea;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.database.TourDatabase;
import net.tourbook.map2.Messages;

import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;

/**
 * A waypoint is associated with a tour but the way point position is independently from a tour, it
 * has it's own lat/lon position.
 */
@Entity
public class TourWayPoint implements Cloneable, Comparable<Object>, IHoveredArea {

	public static final int	DB_LENGTH_NAME			= 1024;
	public static final int	DB_LENGTH_DESCRIPTION	= 4096;
	public static final int	DB_LENGTH_COMMENT		= 4096;
	public static final int	DB_LENGTH_SYMBOL		= 1024;
	public static final int	DB_LENGTH_CATEGORY		= 1024;

	/**
	 * Unique id for the {@link TourWayPoint} entity
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long			wayPointId				= TourDatabase.ENTITY_IS_NOT_SAVED;

	@ManyToOne(optional = false)
	private TourData		tourData;

//	/**
//	 * @since Db version 24
//	 */
//	@ManyToOne
//	private TourSign		tourSign;

	// initialize with invalid values
	private double			longitude				= Double.MIN_VALUE;

	private double			latitude				= Double.MIN_VALUE;

	/**
	 * absolute time
	 */
	private long			time					= 0;

	/**
	 * altitude in meters
	 */
	private float			altitude				= Float.MIN_VALUE;

	private String			name;

	private String			description;
	private String			comment;
	private String			symbol;
	private String			category;

	@Transient
	private GeoPosition		_geoPosition;

	/**
	 * unique id for manually created markers because the {@link #markerId} is 0 when the marker is
	 * not persisted
	 */
	@Transient
	private long			_createId				= 0;

	@Transient
	private static Image	_twpHoveredImage;

	/**
	 * manually created way points or imported way points create a unique id to identify them, saved
	 * way points are compared with the way point id
	 */
	private static int		_createCounter			= 0;

	public TourWayPoint() {}

	public TourWayPoint clone(final TourData wpTourData) {

		try {

			// create a shallow copy
			final TourWayPoint newWayPoint = (TourWayPoint) super.clone();

			// set create id to uniquely identify the way point
			newWayPoint._createId = ++_createCounter;

			newWayPoint.wayPointId = TourDatabase.ENTITY_IS_NOT_SAVED;

			newWayPoint.tourData = wpTourData;

			return newWayPoint;

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

		if (other instanceof TourWayPoint) {

			final TourWayPoint otherWP = (TourWayPoint) other;

			if (time != 0 && otherWP.time != 0) {
				return time > otherWP.time ? 1 : -1;
			}

			if (_createId == 0) {

				if (otherWP._createId == 0) {

					// both way points are persisted
					return wayPointId > otherWP.wayPointId ? 1 : -1;
				}

				return 1;

			} else {

				// _createId != 0

				if (otherWP._createId != 0) {

					// both way points are created and not persisted
					return _createId > otherWP._createId ? 1 : -1;
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

		final TourWayPoint other = (TourWayPoint) obj;
		if (_createId == 0) {

			// tour is from the database
			if (wayPointId != other.wayPointId) {
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

	/**
	 * @return Returns altitude in meters or {@link Float#MIN_VALUE} when altitude is not set
	 */
	public float getAltitude() {
		return altitude;
	}

	public String getCategory() {
		return category;
	}

	public String getComment() {
		return comment;
	}

	/**
	 * @return Returns a unique id for manually created way points because the {@link #wayPointId}
	 *         is {@link TourDatabase#ENTITY_IS_NOT_SAVED} when it's not yet persisted
	 */
	public long getCreateId() {
		return _createId;
	}

	public String getDescription() {
		return description;
	}

	@Override
	public Image getHoveredImage() {

		if (_twpHoveredImage != null) {
			return _twpHoveredImage;
		}

		final ImageRegistry imageRegistry = TourbookPlugin.getDefault().getImageRegistry();

		imageRegistry.put(
				Messages.Image_Map_WayPoint_Hovered,
				TourbookPlugin.getImageDescriptor(Messages.Image_Map_WayPoint_Hovered));

		_twpHoveredImage = imageRegistry.get(Messages.Image_Map_WayPoint_Hovered);

		return _twpHoveredImage;
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

	/**
	 * @return Returns the persistence id or {@link TourDatabase#ENTITY_IS_NOT_SAVED} when the
	 *         entity is not yet saved
	 */
	public long getWayPointId() {
		return wayPointId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (_createId ^ (_createId >>> 32));
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

	@Override
	public String toString() {

		final StringBuilder sb = new StringBuilder();
		sb.append("wayPointId:"); //$NON-NLS-1$
		sb.append(wayPointId);
		sb.append("\tcreateId:"); //$NON-NLS-1$
		sb.append(_createId);
		sb.append("\tname:"); //$NON-NLS-1$
		sb.append(name);
		sb.append("\tlat:"); //$NON-NLS-1$
		sb.append(latitude);
		sb.append("\tlon:"); //$NON-NLS-1$
		sb.append(longitude);
		sb.append("\tdesc:"); //$NON-NLS-1$
		sb.append(description);
		sb.append("\tcom:"); //$NON-NLS-1$
		sb.append(comment);

		return sb.toString();
	}

}
