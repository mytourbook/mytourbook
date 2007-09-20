/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm
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

import java.sql.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * This entity contains the data for a tour which is compared with a reference tour
 */
@Entity
public class TourCompared {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long	comparedId;

	/**
	 * ref tour id which is compared with the tour contained in the tourId
	 */
	private long	refTourId;

	/**
	 * tourId which is compared with the refTourId
	 */
	private long	tourId;

	/*
	 * start/end index for the reference tour in the compared tour
	 */
	private int		startIndex	= -1;
	private int		endIndex	= -1;

	private Date	tourDate;
	private int		startYear;

	private float	tourSpeed;

	public int getEndIndex() {
		return endIndex;
	}

	public void setEndIndex(int endIndex) {
		this.endIndex = endIndex;
	}

	public int getStartIndex() {
		return startIndex;
	}

	public void setStartIndex(int startIndex) {
		this.startIndex = startIndex;
	}

	public long getComparedId() {
		return comparedId;
	}

	public Date getTourDate() {
		return tourDate;
	}

	public float getTourSpeed() {
		return tourSpeed;
	}

	public void setTourSpeed(float speed) {
		this.tourSpeed = speed;
	}

	public void setTourDate(long timeInMillis) {
		tourDate = new Date(timeInMillis);
	}

	public int getStartYear() {
		return startYear;
	}

	public void setStartYear(int startYear) {
		this.startYear = startYear;
	}

	public long getRefTourId() {
		return refTourId;
	}

	public void setRefTourId(long refTourId) {
		this.refTourId = refTourId;
	}

	public long getTourId() {
		return tourId;
	}

	public void setTourId(long tourId) {
		this.tourId = tourId;
	}

}
