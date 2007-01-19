/*******************************************************************************
 * Copyright (C) 2006, 2007  Wolfgang Schramm
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
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import net.tourbook.database.TourDatabase;

@Entity
public class TourPerson {

	public static final int	PERSON_ID_NOT_DEFINED	= -1;
	// public static final int PERSON_ID_ALL = -2;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long			personId				= PERSON_ID_NOT_DEFINED;

	@Basic(optional = false)
	private String			firstName;

	private String			lastName;

	private float			weight;

	private float			height;

	/**
	 * Device used by this person, reference to the device plugin
	 */
	private String			deviceReaderId;

	/**
	 * path where the raw tour data will be saved after import
	 */
	private String			rawDataPath;

	/**
	 * default bike being used by this person
	 */
	@ManyToOne
	private TourBike		tourBike;

	/**
	 * default constructor used in ejb
	 */
	public TourPerson() {}

	public boolean persist() {

		boolean isSaved = false;

		EntityManager em = TourDatabase.getInstance().getEntityManager();
		EntityTransaction ts = em.getTransaction();

		try {

			if (getPersonId() == PERSON_ID_NOT_DEFINED) {
				// entity is new
				ts.begin();
				em.persist(this);
				ts.commit();
			} else {
				// update entity
				ts.begin();
				em.merge(this);
				ts.commit();
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (ts.isActive()) {
				ts.rollback();
			} else {
				isSaved = true;
			}
			em.close();
		}
		return isSaved;
	}

	public String getDeviceReaderId() {
		return deviceReaderId;
	}

	public String getFirstName() {
		return firstName;
	}

	public float getHeight() {
		return height;
	}

	public String getLastName() {
		return lastName;
	}

	public long getPersonId() {
		return personId;
	}

	public float getWeight() {
		return weight;
	}

	public void setDeviceReaderId(String deviceId) {
		this.deviceReaderId = deviceId;
	}

	public void setFirstName(String name) {
		this.firstName = name;
	}

	public void setHeight(float height) {
		this.height = height;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public void setWeight(float weight) {
		this.weight = weight;
	}

	public TourBike getTourBike() {
		return tourBike;
	}

	public void setTourBike(TourBike tourBike) {
		this.tourBike = tourBike;
	}

	public String getRawDataPath() {
		return rawDataPath;
	}

	public void setRawDataPath(String rawDataPath) {
		this.rawDataPath = rawDataPath;
	}

	public String getName() {
		return firstName + (lastName.equals("") ? "" : " " + lastName); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}
