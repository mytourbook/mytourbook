/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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

import net.tourbook.database.TourDatabase;

@Entity
public class TourTag {

	public static final int			EXPAND_TYPE_YEAR_MONTH_DAY	= 0;

	public static final int			EXPAND_TYPE_FLAT			= 1;
	/*
	 * DON'T USE THE FINAL KEYWORD FOR THE ID because the Id cannot be set
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long					tagId						= TourDatabase.ENTITY_IS_NOT_SAVED;

	/**
	 * This is a root tag when set to <code>1</code>, derby does not support BOOLEAN, 1 =
	 * <code>true</code>, 0 = <code>false</code>
	 */
	private int						isRoot						= 0;

	/**
	 * Display name of the tag
	 */
	@Basic(optional = false)
	private String					name;

	/**
	 * when a tag is expanded in the tag tree viewer, the tours can be displayed in different
	 * structures
	 */
	private int						expandType					= EXPAND_TYPE_YEAR_MONTH_DAY;

	@ManyToMany(mappedBy = "tourTags", cascade = ALL, fetch = LAZY)
	private Set<TourData>			tourData					= new HashSet<TourData>();

	@ManyToMany(mappedBy = "tourTags", cascade = ALL, fetch = LAZY)//$NON-NLS-1$
	private Set<TourTagCategory>	tourTagCategory				= new HashSet<TourTagCategory>();

	public TourTag() {}

	public TourTag(final String tagName) {
		this.name = tagName;
	}

	/**
	 * Returns true if both tag id's have the same value.
	 * 
	 * @see IComparator#equals
	 */
	@Override
	public boolean equals(final Object other) {
		if (other instanceof TourTag) {
			final long otherId = ((TourTag) other).getTagId();
			return otherId == tagId;
		}
		return super.equals(other);
	}

	public int getExpandType() {
		return expandType;
	}

	public Set<TourTagCategory> getTagCategories() {
		return tourTagCategory;
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
		int result = 17;
		result = (int) (37 * result + tagId);
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
		return name + "(" + tagId + ")";
	}

}
