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

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;

import net.tourbook.database.TourDatabase;

@Entity
public class TourTagCategory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long					tagCategoryId	= TourDatabase.ENTITY_IS_NOT_SAVED;

	@Basic(optional = false)
	private String					name;

	@ManyToMany
	@JoinTable(inverseJoinColumns = @JoinColumn(name = "tourTag_tagId"))
	private Set<TourTag>			tourTags		= new HashSet<TourTag>();

	@ManyToMany(cascade = ALL, fetch = LAZY)
	@JoinTable(joinColumns = @JoinColumn(name = "TourTagCategory_tagCategoryId1", //
	referencedColumnName = "tagCategoryId"), //
	inverseJoinColumns = @JoinColumn(name = "tourTagCategory_tagCategoryId2", //
	referencedColumnName = "tagCategoryId"))
	private Set<TourTagCategory>	tourTagCategory	= new HashSet<TourTagCategory>();

	/**
	 * default constructor used in ejb
	 */
	public TourTagCategory() {}

	public long getBikeId() {
		return tagCategoryId;
	}

	public String getName() {
		return name;
	}

	public Set<TourTagCategory> getTagCategories() {
		return tourTagCategory;
	}

	public Set<TourTag> getTourTags() {
		return tourTags;
	}

	public void setName(final String name) {
		this.name = name;
	}

}
