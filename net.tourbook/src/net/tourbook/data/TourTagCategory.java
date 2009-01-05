/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Transient;

import net.tourbook.database.TourDatabase;

@Entity
public class TourTagCategory implements Comparable<Object> {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long					tagCategoryId		= TourDatabase.ENTITY_IS_NOT_SAVED;

	/**
	 * derby does not support BOOLEAN, 1 = <code>true</code>, 0 = <code>false</code>
	 */
	private int						isRoot				= 0;

	@Basic(optional = false)
	private String					name;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(inverseJoinColumns = @JoinColumn(name = "tourTag_tagId", referencedColumnName = "tagId"), //
	joinColumns = @JoinColumn(name = "tourTagCategory_tagCategoryId", referencedColumnName = "tagCategoryId"))
	private Set<TourTag>			tourTags			= new HashSet<TourTag>();

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(joinColumns = @JoinColumn(name = "TourTagCategory_tagCategoryId1", referencedColumnName = "tagCategoryId"), //
	inverseJoinColumns = @JoinColumn(name = "tourTagCategory_tagCategoryId2", referencedColumnName = "tagCategoryId"))
	private Set<TourTagCategory>	tourTagCategory		= new HashSet<TourTagCategory>();

	/**
	 * contains the number of categories or <code>-1</code> when the categories are not loaded
	 */
	@Transient
	private int						fCategoryCounter	= -1;

	/**
	 * contains the number of tags or <code>-1</code> when the tags are not loaded
	 */
	@Transient
	private int						fTagCounter			= -1;

	/**
	 * default constructor used in ejb
	 */
	public TourTagCategory() {}

	public TourTagCategory(final String categoryName) {
		name = categoryName;
	}

	public int compareTo(final Object obj) {

		if (obj instanceof TourTagCategory) {
			final TourTagCategory otherCategory = (TourTagCategory) obj;
			return name.compareTo(otherCategory.name);
		}

		return 0;
	}

	public int getCategoryCounter() {
		return fCategoryCounter;
	}

	public long getCategoryId() {
		return tagCategoryId;
	}

	public String getCategoryName() {
		return name;
	}

	public Set<TourTagCategory> getTagCategories() {
		return tourTagCategory;
	}

	public int getTagCounter() {
		return fTagCounter;
	}

	/**
	 * @return Returns the tags which belong to this category, the tags will be fetched with the
	 *         fetch type {@link FetchType#LAZY}
	 */
	public Set<TourTag> getTourTags() {
		return tourTags;
	}

	public boolean isRoot() {
		return isRoot == 1;
	}

	public void setCategoryCounter(final int fCategoryCounter) {
		this.fCategoryCounter = fCategoryCounter;
	}

	/**
	 * Set the name for the tag category
	 * 
	 * @param name
	 */
	public void setName(final String name) {
		this.name = name;
	}

	/**
	 * set root flag if this tag is a root item or not, 1 = <code>true</code>, 0 =
	 * <code>false</code>
	 */
	public void setRoot(final boolean isRoot) {
		this.isRoot = isRoot ? 1 : 0;
	}

	public void setTagCounter(final int fTagCounter) {
		this.fTagCounter = fTagCounter;
	}

	@Override
	public String toString() {
		final String category = "TourTagCategory ID:" + tagCategoryId + "\tisRoot:" + isRoot + "\t" + name; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
//		if (tourTagCategory.size() > 0) {
//			category += UI.NEW_LINE + "\tchildren:";
//			for (final TourTagCategory ttCategory : tourTagCategory) {
//				category += UI.NEW_LINE + "\t" + ttCategory.toString();
//			}
//		}
		return category;
	}

}
