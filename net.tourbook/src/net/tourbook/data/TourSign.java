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

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import net.tourbook.database.TourDatabase;

@Entity
public class TourSign implements Comparable<Object> {

	public static final int		DB_LENGTH_NAME				= 1024;
	public static final int		DB_LENGTH_IMAGE_FILE_PATH	= 1024;

	public static final int		EXPAND_TYPE_YEAR_MONTH_DAY	= 0;
	public static final int		EXPAND_TYPE_FLAT			= 1;
	public static final int		EXPAND_TYPE_YEAR_DAY		= 2;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long				signId						= TourDatabase.ENTITY_IS_NOT_SAVED;

	/**
	 * Display name of the sign.
	 */
	@Basic(optional = false)
	private String				name;

	/**
	 * Derby does not support BOOLEAN
	 * <p>>
	 * 1 = <code>true</code><br>
	 * 0 = <code>false</code>
	 */
	private int					isRoot						= 0;

	/**
	 * Full sign image file name with path.
	 */
	private String				imageFilePathName;

	/**
	 * when a sign is expanded in the sign tree viewer, the tours can be displayed in different
	 * structures
	 */
	private int					expandType					= EXPAND_TYPE_FLAT;

	/**
	 *
	 */
	@ManyToOne
	private TourSignCategory	tourSignCategory;

//	/**
//	 * All tour marker for this tour sign.
//	 */
//	@OneToMany(fetch = FetchType.EAGER, cascade = ALL, mappedBy = "tourSign")
//	@Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
//	private Set<TourMarker>		tourMarker					= new HashSet<TourMarker>();

//	/**
//	 * All way points for this tour sign.
//	 */
//	@OneToMany(fetch = FetchType.EAGER, cascade = ALL, mappedBy = "tourSign")
//	@Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
//	private Set<TourWayPoint>	tourWayPoint				= new HashSet<TourWayPoint>();

//
//
//
//
// this is from TourTag
//
//
//
//
//	@ManyToMany(mappedBy = "tourSigns", cascade = ALL, fetch = LAZY)
//	private final Set<TourData>			tourData					= new HashSet<TourData>();
//
//	@ManyToMany(mappedBy = "tourSigns", cascade = ALL, fetch = LAZY)
//	private final Set<TourSignCategory>	tourSignCategory			= new HashSet<TourSignCategory>();

	/**
	 * unique id for manually created tour types because the {@link #signId} is -1 when it's not
	 * persisted
	 */
	@Transient
	private long				_createId					= 0;

	/**
	 * manually created marker or imported marker create a unique id to identify them, saved marker
	 * are compared with the marker id
	 */
	private static int			_createCounter				= 0;

	public TourSign() {}

	public TourSign(final String signName) {

		name = signName.trim();
		_createId = ++_createCounter;
	}

	public int compareTo(final Object obj) {

		if (obj instanceof TourSign) {
			final TourSign otherSign = (TourSign) obj;
			return name.compareTo(otherSign.name);
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

		final TourSign other = (TourSign) obj;

		if (_createId == 0) {

			// tour sign is from the database
			if (signId != other.signId) {
				return false;
			}
		} else {

			// tour sign was create or imported
			if (_createId != other._createId) {
				return false;
			}
		}

		return true;
	}

	public int getExpandType() {
		return expandType;
	}

	public String getImageFilePathName() {
		return imageFilePathName;
	}

	public TourSignCategory getSignCategory() {
		return tourSignCategory;
	}

	public long getSignId() {
		return signId;
	}

	public String getSignName() {
		return name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (_createId ^ (_createId >>> 32));
		result = prime * result + (int) (signId ^ (signId >>> 32));
		return result;
	}

	public boolean isRoot() {
		return isRoot == 1;
	}

	public void setExpandType(final int expandType) {
		this.expandType = expandType;
	}

	public void setImageFilePathName(final String imageFilePathName) {
		this.imageFilePathName = imageFilePathName;
	}

	/**
	 * set root flag if this sign is a root item or not, 1 = <code>true</code>, 0 =
	 * <code>false</code>
	 */
	public void setRoot(final boolean isRoot) {
		this.isRoot = isRoot ? 1 : 0;
	}

	/**
	 * Set the name for the tour sign
	 * 
	 * @param signName
	 */
	public void setSignName(final String signName) {
		this.name = signName;
	}

	@Override
	public String toString() {
		return "sign: " + name + " (id:" + signId + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

}
