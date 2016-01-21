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

import net.tourbook.database.TourDatabase;

@Entity
public class TourSignCategory implements Comparable<Object> {

	public static final int				DB_LENGTH_NAME		= 1024;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long						signCategoryId		= TourDatabase.ENTITY_IS_NOT_SAVED;

	@Override
	public int compareTo(final Object o) {
		// TODO Auto-generated method stub
		return 0;
	}

//	/**
//	 * Derby does not support BOOLEAN
//	 * <p>>
//	 * 0 = <code>false</code> <br>
//	 * 1 = <code>true</code>
//	 */
//	private int							isRoot				= 0;
//
//	@Basic(optional = false)
//	private String						name;
//
//	/**
//	 * A sign category can contain many tour signs.
//	 */
//	@ManyToMany(fetch = FetchType.LAZY)
//	@JoinTable(//
//	joinColumns = @JoinColumn(name = "tourSignCategory_signCategoryId", referencedColumnName = "signCategoryId"), //
//	inverseJoinColumns = @JoinColumn(name = "tourSign_signId", referencedColumnName = "signId"))
//	private final Set<TourSign>			tourSigns			= new HashSet<TourSign>();
//
//	/**
//	 * A sign category can contain many sign categories.
//	 */
//	@ManyToMany(fetch = FetchType.LAZY)
//	@JoinTable(//
//	joinColumns = @JoinColumn(name = "TourSignCategory_signCategoryId1", referencedColumnName = "signCategoryId"), //
//	inverseJoinColumns = @JoinColumn(name = "tourSignCategory_signCategoryId2", referencedColumnName = "signCategoryId"))
//	private final Set<TourSignCategory>	tourSignCategories	= new HashSet<TourSignCategory>();
//
//	/**
//	 * contains the number of categories or <code>-1</code> when the categories are not loaded
//	 */
//	@Transient
//	private int							_categoryCounter	= -1;
//
//	/**
//	 * contains the number of signs or <code>-1</code> when the signs are not loaded
//	 */
//	@Transient
//	private int							_signCounter		= -1;
//
//	/**
//	 * Default constructor used in EJB
//	 */
//	public TourSignCategory() {}
//
//	public TourSignCategory(final String categoryName) {
//
//		name = categoryName;
//	}
//
//	public void addTourSign(final TourSign sign) {
//
//		tourSigns.add(sign);
//	}
//
//	public void addTourSignCategory(final TourSignCategory signCategory) {
//
//		tourSignCategories.add(signCategory);
//	}
//
//	public int compareTo(final Object obj) {
//
//		if (obj instanceof TourSignCategory) {
//			final TourSignCategory otherCategory = (TourSignCategory) obj;
//			return name.compareTo(otherCategory.name);
//		}
//
//		return 0;
//	}
//
//	public int getCategoryCounter() {
//		return _categoryCounter;
//	}

	public long getCategoryId() {
		return signCategoryId;
	}

//	public String getCategoryName() {
//		return name;
//	}
//
//	public int getSignCounter() {
//		return _signCounter;
//	}
//
//	public Set<TourSignCategory> getTourSignCategories() {
//		return tourSignCategories;
//	}
//
//	/**
//	 * @return Returns the signs which belong to this category, the signs will be fetched with the
//	 *         fetch type {@link FetchType#LAZY}
//	 */
//	public Set<TourSign> getTourSigns() {
//		return tourSigns;
//	}
//
//	public boolean isRoot() {
//		return isRoot == 1;
//	}
//
//	public void setCategoryCounter(final int fCategoryCounter) {
//		this._categoryCounter = fCategoryCounter;
//	}
//
//	/**
//	 * Set the name for the sign category
//	 *
//	 * @param name
//	 */
//	public void setName(final String name) {
//		this.name = name;
//	}
//
//	/**
//	 * set root flag if this sign is a root item or not, 1 = <code>true</code>, 0 =
//	 * <code>false</code>
//	 */
//	public void setRoot(final boolean isRoot) {
//		this.isRoot = isRoot ? 1 : 0;
//	}
//
//	public void setSignCounter(final int signCounter) {
//		_signCounter = signCounter;
//	}
//
//	@Override
//	public String toString() {
//
//		final int maxLen = 10;
//
//		return String.format(""
//				+ "TourSignCategory\t"
//				+ "signCategoryId=%s\t"
//				+ "isRoot=%s\t"
//				+ "categoryKey=%30s\t"
//				+ "name=%10s\t"
////				+ "tourSigns=%s\t"
////				+ "tourSignCategories=%s\t"//
//				, //
//				signCategoryId,
//				isRoot,
//				name
////				tourSigns != null ? toString(tourSigns, maxLen) : null,
////				tourSignCategories != null ? toString(tourSignCategories, maxLen) : null
//				//
//				);
//	}
//
//	private String toString(final Collection<?> collection, final int maxLen) {
//
//		final StringBuilder sb = new StringBuilder();
//		sb.append("[");
//		int i = 0;
//		for (final Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {
//			if (i > 0) {
//				sb.append(", ");
//			}
//			sb.append(iterator.next());
//		}
//		sb.append("]");
//
//		return sb.toString();
//	}

}
