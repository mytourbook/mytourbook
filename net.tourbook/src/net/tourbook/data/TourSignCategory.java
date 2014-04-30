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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import net.tourbook.database.TourDatabase;

import org.hibernate.annotations.Cascade;

@Entity
public class TourSignCategory implements Comparable<Object> {

	public static final int				DB_LENGTH_NAME		= 1024;

	public static final String			ROOT_KEY			= "root-key-a8d3-s3g1";			//$NON-NLS-1$

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long						signCategoryId		= TourDatabase.ENTITY_IS_NOT_SAVED;

	/**
	 * Derby does not support BOOLEAN
	 * <p>>
	 * 1 = <code>true</code><br>
	 * 0 = <code>false</code>
	 */
	private int							isRoot				= 0;

	@Basic(optional = false)
	private String						name;

	/**
	 * This key is used to identify imported categories (filesystem folders).
	 */
	@Basic(optional = false)
	private String						categoryKey;

	/**
	 * Tour signs for this category.
	 */
	@OneToMany(fetch = FetchType.EAGER, cascade = ALL, mappedBy = "tourSignCategory")
	@Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
	private Set<TourSign>				tourSigns			= new HashSet<TourSign>();

	/**
	 * Tour sign child categories.
	 */
	@OneToMany(fetch = FetchType.EAGER, cascade = ALL, mappedBy = "signCategoryId")
	@Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
	private final Set<TourSignCategory>	tourSignCategories	= new HashSet<TourSignCategory>();

	/**
	 * contains the number of categories or <code>-1</code> when the categories are not loaded
	 */
	@Transient
	private int							_categoryCounter	= -1;

//	@ManyToMany(fetch = FetchType.LAZY)
//	@JoinTable(inverseJoinColumns = @JoinColumn(name = "tourSign_signId", referencedColumnName = "signId"), //
//	joinColumns = @JoinColumn(name = "tourSignCategory_signCategoryId", referencedColumnName = "signCategoryId"))
//	private final Set<TourSign>			tourSigns			= new HashSet<TourSign>();
//
//	@ManyToMany(fetch = FetchType.LAZY)
//	@JoinTable(joinColumns = @JoinColumn(name = "TourSignCategory_signCategoryId1", referencedColumnName = "signCategoryId"), //
//	inverseJoinColumns = @JoinColumn(name = "tourSignCategory_signCategoryId2", referencedColumnName = "signCategoryId"))
//	private final Set<TourSignCategory>	tourSignCategories	= new HashSet<TourSignCategory>();

	/**
	 * contains the number of signs or <code>-1</code> when the signs are not loaded
	 */
	@Transient
	private int							_signCounter		= -1;

	/**
	 * Default constructor used in EJB
	 */
	public TourSignCategory() {}

	public TourSignCategory(final String categoryName, final String newCategoryKey) {

		name = categoryName;
		categoryKey = newCategoryKey;
	}

	public void addTourSign(final TourSign sign) {

		tourSigns.add(sign);
	}

	public void addTourSignCategory(final TourSignCategory signCategory) {

		tourSignCategories.add(signCategory);
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

	/**
	 * @return Returns a unique key for this category, it's the name of the category structure.
	 */
	public String getCategoryKey() {
		return categoryKey;
	}

	public String getCategoryName() {
		return name;
	}

	public Set<TourSignCategory> getSignCategories() {
		return tourSignCategories;
	}

	public int getSignCounter() {
		return _signCounter;
	}

	/**
	 * @return Returns the signs which belong to this category, the signs will be fetched with the
	 *         fetch type {@link FetchType#LAZY}
	 */
	public Set<TourSign> getTourSigns() {
		return tourSigns;
	}

	public boolean isRoot() {
		return isRoot == 1;
	}

	public void setCategoryCounter(final int fCategoryCounter) {
		this._categoryCounter = fCategoryCounter;
	}

	public void setCategoryKey(final String categoryKey) {
		this.categoryKey = categoryKey;
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

		final int maxLen = 10;

		return String.format(""
				+ "TourSignCategory\t"
				+ "signCategoryId=%s\t"
				+ "isRoot=%s\t"
				+ "categoryKey=%30s\t"
				+ "name=%10s\t"
//				+ "tourSigns=%s\t"
//				+ "tourSignCategories=%s\t"//
				, //
				signCategoryId,
				isRoot,
				categoryKey,
				name
//				tourSigns != null ? toString(tourSigns, maxLen) : null,
//				tourSignCategories != null ? toString(tourSignCategories, maxLen) : null
				//
				);
	}

	private String toString(final Collection<?> collection, final int maxLen) {

		final StringBuilder sb = new StringBuilder();
		sb.append("[");
		int i = 0;
		for (final Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {
			if (i > 0) {
				sb.append(", ");
			}
			sb.append(iterator.next());
		}
		sb.append("]");

		return sb.toString();
	}

}
