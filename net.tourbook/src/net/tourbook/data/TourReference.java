/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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

@Entity
public class TourReference {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long		refId;

	@ManyToOne(optional = false)
	private TourData	tourData;

	/**
	 * value index position for the reference tour in the original tour
	 */
	private int			startIndex;
	private int			endIndex;

	private String		label	= "";	//$NON-NLS-1$

	public TourReference() {}

	public TourReference(String label, TourData tourData, int startIndex, int endIndex) {
		this.tourData = tourData;
		this.label = label;
		this.startIndex = startIndex;
		this.endIndex = endIndex;
	}

	public void setGeneratedId(long generatedId) {
		this.refId = generatedId;
	}

	/**
	 * @return Returns the primary key for a {@link TourReference} entity
	 */
	public long getRefId() {
		return refId;
	}

	public void setTourData(TourData tourData) {
		this.tourData = tourData;
	}

	public TourData getTourData() {
		return tourData;
	}

	public void setStartValueIndex(int startIndex) {
		this.startIndex = startIndex;
	}

	public int getStartValueIndex() {
		return startIndex;
	}

	public void setEndValueIndex(int endIndex) {
		this.endIndex = endIndex;
	}

	public int getEndValueIndex() {
		return endIndex;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	/**
	 * @return Return the name for the reference tour
	 */
	public String getLabel() {
		return label;
	}
}
