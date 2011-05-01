/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import net.tourbook.database.PersonManager;
import net.tourbook.database.TourDatabase;
import net.tourbook.training.TrainingManager;
import net.tourbook.ui.UI;

import org.hibernate.annotations.Cascade;

@Entity
public class TourPerson implements Comparable<Object> {

	public static final int			DB_LENGTH_LAST_NAME			= 80;
	public static final int			DB_LENGTH_FIRST_NAME		= 80;
	public static final int			DB_LENGTH_RAW_DATA_PATH		= 255;
	public static final int			DB_LENGTH_DEVICE_READER_ID	= 255;

	public static final int			PERSON_ID_NOT_DEFINED		= -1;

	/**
	 * Default rest pulse
	 */
	public static final int			DEFAULT_REST_PULSE			= 60;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long					personId					= PERSON_ID_NOT_DEFINED;

	@Basic(optional = false)
	private String					firstName;

	private String					lastName;

	private float					weight;

	private float					height;

	/**
	 * Date/Time when tour data was modified, default value is 0
	 * <p>
	 * since: db version 15
	 */
	private long					birthDay;

	/**
	 * Gender: Male = 0, Female = 1
	 * <p>
	 * since: db version 16
	 */
	private int						gender;

	/**
	 * Resting heart rate
	 * <p>
	 * since: db version 16
	 */
	private int						restPulse;

	/**
	 * Max heart rate
	 * <p>
	 * since: db version 16
	 */
	private int						maxPulse;

	/**
	 * Formula how max heart rate is computed. The formulas are defined in
	 * {@link TrainingManager#HRMaxFormulaNames} and {@link TrainingManager#HRMaxFormulaKeys}
	 * <p>
	 * Default is 0 which is 220-age
	 * <p>
	 * since: db version 16
	 */
	private int						hrMaxFormula;

	/**
	 * Device used by this person, reference to the device plugin
	 */
	private String					deviceReaderId;

	/**
	 * path where the raw tour data will be saved after import
	 */
	private String					rawDataPath;

	/**
	 * default bike being used by this person
	 */
	@ManyToOne
	private TourBike				tourBike;

	/**
	 * Tour marker
	 */
	@OneToMany(fetch = FetchType.EAGER, cascade = ALL, mappedBy = "tourPerson")
	@Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
	private Set<TourPersonHRZone>	hrZones						= new HashSet<TourPersonHRZone>();

	/**
	 * manually created person creates a unique id to identify it, saved person is compared with the
	 * person id
	 */
	private static int				_createCounter				= 0;

	/**
	 * unique id for manually created person because the {@link #personId} is
	 * {@value #PERSON_ID_NOT_DEFINED} when it's not persisted
	 */
	@Transient
	private long					_createId					= 0;

	/**
	 * default constructor used in ejb
	 */
	public TourPerson() {}

	public TourPerson(final String firstName, final String lastName) {

		_createId = ++_createCounter;

		this.firstName = firstName;
		this.lastName = lastName;
	}

	@Override
	public int compareTo(final Object o) {

		// compare by last + first name

		if (o instanceof TourPerson) {

			final TourPerson otherPerson = (TourPerson) o;

			final int compareLastName = lastName.compareTo(otherPerson.getLastName());

			if (compareLastName != 0) {
				return compareLastName;
			}

			return firstName.compareTo(otherPerson.getFirstName());

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
		if (!(obj instanceof TourPerson)) {
			return false;
		}

		final TourPerson other = (TourPerson) obj;

		if (_createId == 0) {

			// person is from the database
			if (personId != other.personId) {
				return false;
			}
		} else {

			// person was create
			if (_createId != other._createId) {
				return false;
			}
		}

		return true;
	}

	public long getBirthDay() {
		return birthDay;
	}

	public String getDeviceReaderId() {
		return deviceReaderId;
	}

	public String getFirstName() {
		return firstName;
	}

	public int getGender() {
		return gender;
	}

	public float getHeight() {
		return height;
	}

	public int getHrMaxFormula() {
		return hrMaxFormula;
	}

	public Set<TourPersonHRZone> getHrZones() {
		return hrZones;
	}

	public String getLastName() {
		return lastName;
	}

	public int getMaxPulse() {
		return maxPulse;
	}

	/**
	 * @return Return the person first and last name
	 */
	public String getName() {
		return firstName + //
				(lastName.equals(UI.EMPTY_STRING) ? //
						UI.EMPTY_STRING
						: UI.SPACE + lastName);
	}

	public long getPersonId() {
		return personId;
	}

	public String getRawDataPath() {
		return rawDataPath;
	}

	public int getRestPulse() {
		return restPulse;
	}

	public TourBike getTourBike() {
		return tourBike;
	}

	public float getWeight() {
		return weight;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (_createId ^ (_createId >>> 32));
		result = prime * result + (int) (personId ^ (personId >>> 32));
		return result;
	}

	public boolean persist() {

		boolean isSaved = false;

		final EntityManager em = TourDatabase.getInstance().getEntityManager();
		final EntityTransaction ts = em.getTransaction();

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

		} catch (final Exception e) {
			e.printStackTrace();
		} finally {
			if (ts.isActive()) {
				ts.rollback();
			} else {
				isSaved = true;
			}
			em.close();
		}

		if (isSaved) {
			PersonManager.refreshPeople();
		}

		return isSaved;
	}

	public void setBirthDay(final long birthDay) {
		this.birthDay = birthDay;
	}

	public void setDeviceReaderId(final String deviceId) {
		this.deviceReaderId = deviceId;
	}

	public void setFirstName(final String name) {
		this.firstName = name;
	}

	public void setGender(final int gender) {
		this.gender = gender;
	}

	public void setHeight(final float height) {
		this.height = height;
	}

	public void setHrMaxFormula(final int hrMaxFormula) {
		this.hrMaxFormula = hrMaxFormula;
	}

	public void setHrZones(final Set<TourPersonHRZone> hrZones) {
		this.hrZones = hrZones;
	}

	public void setLastName(final String lastName) {
		this.lastName = lastName;
	}

	public void setMaxPulse(final int maxPulse) {
		this.maxPulse = maxPulse;
	}

	public void setRawDataPath(final String rawDataPath) {
		this.rawDataPath = rawDataPath;
	}

	public void setRestPulse(final int restPulse) {
		this.restPulse = restPulse;
	}

	public void setTourBike(final TourBike tourBike) {
		this.tourBike = tourBike;
	}

	public void setWeight(final float weight) {
		this.weight = weight;
	}

	@Override
	public String toString() {
		return "TourPerson [personId=" + personId + ", firstName=" + firstName + ", lastName=" + lastName + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}
}
