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
public class TourSignCategory implements Comparable<Object> {

	public static final int				DB_LENGTH_NAME		= 1024;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long						signCategoryId		= TourDatabase.ENTITY_IS_NOT_SAVED;

	/**
	 * derby does not support BOOLEAN, 1 = <code>true</code>, 0 = <code>false</code>
	 */
	private int							isRoot				= 0;

	@Basic(optional = false)
	private String						name;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(inverseJoinColumns = @JoinColumn(name = "tourSign_signId", referencedColumnName = "signId"), //
	joinColumns = @JoinColumn(name = "tourSignCategory_signCategoryId", referencedColumnName = "signCategoryId"))
	private final Set<TourSign>	tourSigns			= new HashSet<TourSign>();

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(joinColumns = @JoinColumn(name = "TourSignCategory_signCategoryId1", referencedColumnName = "signCategoryId"), //
	inverseJoinColumns = @JoinColumn(name = "tourSignCategory_signCategoryId2", referencedColumnName = "signCategoryId"))
	private final Set<TourSignCategory>	tourSignCategories	= new HashSet<TourSignCategory>();

	/**
	 * contains the number of categories or <code>-1</code> when the categories are not loaded
	 */
	@Transient
	private int							_categoryCounter	= -1;

	/**
	 * contains the number of signs or <code>-1</code> when the signs are not loaded
	 */
	@Transient
	private int							_signCounter		= -1;

	/**
	 * default constructor used in ejb
	 */
	public TourSignCategory() {}

	public TourSignCategory(final String categoryName) {
		name = categoryName;
	}

	public int compareTo(final Object obj) {

		if (obj instanceof TourSignCategory) {
			final TourSignCategory otherCategory = (TourSignCategory) obj;
			return name.compareTo(otherCategory.name);
		}

		return 0;
	}

	public int getCategoryCounter() {
		return _categoryCounter;
	}

	public long getCategoryId() {
		return signCategoryId;
	}

	public String getCategoryName() {
		return name;
	}

//	public Set<TourSignCategory> getSignCategories() {
//		return tourSignCategories;
//	}

	public int getSignCounter() {
		return _signCounter;
	}

//	/**
//	 * @return Returns the signs which belong to this category, the signs will be fetched with the
//	 *         fetch type {@link FetchType#LAZY}
//	 */
//	public Set<TourSign> getTourSigns() {
//		return tourSigns;
//	}

	public boolean isRoot() {
		return isRoot == 1;
	}

	public void setCategoryCounter(final int fCategoryCounter) {
		this._categoryCounter = fCategoryCounter;
	}

	/**
	 * Set the name for the sign category
	 * 
	 * @param name
	 */
	public void setName(final String name) {
		this.name = name;
	}

	/**
	 * set root flag if this sign is a root item or not, 1 = <code>true</code>, 0 =
	 * <code>false</code>
	 */
	public void setRoot(final boolean isRoot) {
		this.isRoot = isRoot ? 1 : 0;
	}

	public void setSignCounter(final int signCounter) {
		_signCounter = signCounter;
	}

	@Override
	public String toString() {
		final String category = "TourSignCategory ID:" + signCategoryId + "\tisRoot:" + isRoot + "\t" + name; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
//		if (tourSignCategory.size() > 0) {
//			category += UI.NEW_LINE + "\tchildren:";
//			for (final TourSignCategory ttCategory : tourSignCategory) {
//				category += UI.NEW_LINE + "\t" + ttCategory.toString();
//			}
//		}
		return category;
	}

}
