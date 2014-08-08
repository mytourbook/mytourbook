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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Transient;

import net.tourbook.database.TourDatabase;

@Entity
public class TourTag implements Comparable<Object> {

	public static final int		DB_LENGTH_NAME				= 255;

	public static final int		EXPAND_TYPE_YEAR_MONTH_DAY	= 0;
	public static final int		EXPAND_TYPE_FLAT			= 1;
	public static final int		EXPAND_TYPE_YEAR_DAY		= 2;

	/*
	 * DON'T USE THE FINAL KEYWORD FOR THE ID otherwise the Id cannot be set.
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long				tagId						= TourDatabase.ENTITY_IS_NOT_SAVED;

	/**
	 * This is a root tag when set to <code>1</code>, derby does not support BOOLEAN, 1 =
	 * <code>true</code>, 0 = <code>false</code>
	 */
	private int					isRoot						= 0;

	/**
	 * Display name of the tag
	 */
	@Basic(optional = false)
	private String				name;

	/**
	 * when a tag is expanded in the tag tree viewer, the tours can be displayed in different
	 * structures
	 */
	private int					expandType					= EXPAND_TYPE_FLAT;

	/**
	 * Contains all tours are associated with this tag.
	 */
	@ManyToMany(mappedBy = "tourTags", cascade = ALL, fetch = LAZY)
	private final Set<TourData>	tourData					= new HashSet<TourData>();

//	/**
//	 * A tag belongs to <b>ONE</b> category and not to many as it was implemented in version 14.4
//	 * and before.
//	 * <p>
//	 * This new field needs a column in the db table but the column in the db is empty for entities
//	 * which are created before 14.4 but it's still working ???
//	 * <p>
//	 * <p>
//	 * <b>Old Implementation</b>
//	 *
//	 * <pre>
//	 * &#064;ManyToMany(mappedBy = &quot;tourTags&quot;, cascade = ALL, fetch = LAZY)
//	 * private final Set&lt;TourTagCategory&gt;	tourTagCategory	= new HashSet&lt;TourTagCategory&gt;();
//	 * </pre>
//	 */
//	@ManyToOne()
//	private TourTagCategory		tourTagCategory;

	/**
	 * unique id for manually created tour types because the {@link #tagId} is -1 when it's not
	 * persisted
	 */
	@Transient
	private long				_createId					= 0;

	/**
	 * manually created marker or imported marker create a unique id to identify them, saved marker
	 * are compared with the marker id
	 */
	private static int			_createCounter				= 0;

	public TourTag() {}

	public TourTag(final String tagName) {
		name = tagName.trim();
		_createId = ++_createCounter;
	}

	public int compareTo(final Object obj) {

		if (obj instanceof TourTag) {
			final TourTag otherTag = (TourTag) obj;
			return name.compareTo(otherTag.name);
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

		final TourTag other = (TourTag) obj;

		if (_createId == 0) {

			// tour tag is from the database
			if (tagId != other.tagId) {
				return false;
			}
		} else {

			// tour tag was create or imported
			if (_createId != other._createId) {
				return false;
			}
		}

		return true;
	}

	public int getExpandType() {
		return expandType;
	}

	public long getTagId() {
		return tagId;
	}

	public String getTagName() {
		return name;
	}

	public Collection<TourData> getTourData() {
		return tourData;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (_createId ^ (_createId >>> 32));
		result = prime * result + (int) (tagId ^ (tagId >>> 32));
		return result;
	}

	public boolean isRoot() {
		return isRoot == 1;
	}

	public void setExpandType(final int expandType) {
		this.expandType = expandType;
	}

	/**
	 * set root flag if this tag is a root item or not, 1 = <code>true</code>, 0 =
	 * <code>false</code>
	 */
	public void setRoot(final boolean isRoot) {
		this.isRoot = isRoot ? 1 : 0;
	}

	/**
	 * Set the name for the tour tag
	 * 
	 * @param tagName
	 */
	public void setTagName(final String tagName) {
		this.name = tagName;
	}

	@Override
	public String toString() {
		return "tag: " + name + " (id:" + tagId + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

}
