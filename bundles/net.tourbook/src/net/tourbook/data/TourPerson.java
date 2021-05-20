/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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

import java.time.Period;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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

import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StringUtils;
import net.tourbook.database.PersonManager;
import net.tourbook.database.TourDatabase;
import net.tourbook.training.TrainingManager;
import net.tourbook.ui.UI;

import org.hibernate.annotations.Cascade;

@Entity
public class TourPerson implements Comparable<Object> {

   public static final ZonedDateTime       DEFAULT_BIRTHDAY           = ZonedDateTime.of(
         1977,
         7,
         7,
         0,
         0,
         0,
         0,
         TimeTools.getDefaultTimeZone());
   public static final int                 DB_LENGTH_LAST_NAME        = 80;
   public static final int                 DB_LENGTH_FIRST_NAME       = 80;
   public static final int                 DB_LENGTH_RAW_DATA_PATH    = 255;
   public static final int                 DB_LENGTH_DEVICE_READER_ID = 255;

   public static final int                 PERSON_ID_NOT_DEFINED      = -1;

   /**
    * Default rest pulse
    */
   public static final int                 DEFAULT_REST_PULSE         = 60;

   /**
    * manually created person creates a unique id to identify it, saved person is compared with the
    * person id
    */
   private static int                      _createCounter             = 0;

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private long                            personId                   = PERSON_ID_NOT_DEFINED;

   @Basic(optional = false)
   private String                          firstName;

   private String                          lastName;

   private float                           weight;

   private float                           height;

   /**
    * Birthday of this person in milliseconds from 1970-01-01T00:00:00, default value is 0 when
    * birthday is not set.
    * <p>
    * since: db version 15
    */
   private long                            birthDay;

   /**
    * Gender: Male = 0, Female = 1
    * <p>
    * since: db version 16
    */
   private int                             gender;

   /**
    * Resting heart rate
    * <p>
    * since: db version 16
    */
   private int                             restPulse;

   /**
    * Max heart rate, when {@link #hrMaxFormula} is not computed
    * <p>
    * since: db version 16
    */
   private int                             maxPulse;

   /**
    * Formula how max heart rate is computed. The formulas are defined in
    * {@link TrainingManager#HRMaxFormulaNames} and {@link TrainingManager#HRMaxFormulaKeys}
    * <p>
    * Default is 0 which is 220-age
    * <p>
    * since: db version 16
    */
   private int                             hrMaxFormula;

   /**
    * Device used by this person, reference to the device plugin
    */
   private String                          deviceReaderId;

   /**
    * path where the raw tour data will be saved after import
    */
   private String                          rawDataPath;

   /**
    * default bike being used by this person
    */
   @ManyToOne
   private TourBike                        tourBike;

   /**
    * Tour hr zones
    */
   @OneToMany(fetch = FetchType.EAGER, cascade = ALL, mappedBy = "tourPerson")
   @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
   private Set<TourPersonHRZone>           hrZones                    = new HashSet<>();

   /**
    * unique id for manually created person because the {@link #personId} is
    * {@value #PERSON_ID_NOT_DEFINED} when it's not persisted
    */
   @Transient
   private long                            _createId                  = 0;

   @Transient
   private ZonedDateTime                   _zonedBirthDay;

   /**
    * Cached HR zones, key is the age of the person
    */
   @Transient
   private HashMap<Integer, HrZoneContext> _hrZoneMinMaxBpm           = new HashMap<>();

   /**
    * Sorted HR zones
    */
   @Transient
   private ArrayList<TourPersonHRZone>     _sortedHrZones;

   /**
    * default constructor used in ejb
    */
   public TourPerson() {}

   public TourPerson(final String firstName, final String lastName) {

      _createId = ++_createCounter;

      this.firstName = firstName;
      this.lastName = lastName;
   }

   /**
    * @return Returns HR max depending on the HR max formula.
    * @param hrMaxFormulaKey
    * @param maxPulse
    * @param age
    */
   public static int getHrMax(int hrMaxFormulaKey, final int maxPulse, final int age) {

      if (hrMaxFormulaKey == TrainingManager.HR_MAX_NOT_COMPUTED) {

         // hr max is not computed

         return maxPulse;

      } else {

         int keyIndex = -1;
         for (final int formulaKey : TrainingManager.HRMaxFormulaKeys) {
            if (formulaKey == hrMaxFormulaKey) {
               keyIndex = hrMaxFormulaKey;
               break;
            }
         }

         if (keyIndex == -1) {
            // key not found, use default value
            hrMaxFormulaKey = TrainingManager.HR_MAX_FORMULA_220_AGE;
         }

         if (hrMaxFormulaKey == TrainingManager.HR_MAX_FORMULA_220_AGE) {

            // HRmax = 220 - age

            return 220 - age;

         } else if (hrMaxFormulaKey == TrainingManager.HR_MAX_FORMULA_205_8) {

            // HRmax = 205.8 - (0.685 x age)

            return (int) (205.8 - (0.685 * age));

         } else if (hrMaxFormulaKey == TrainingManager.HR_MAX_FORMULA_206_9) {

            //  HRmax = 206.9 - (0.67 x age)

            return (int) (206.9 - (0.67 * age));

         } else if (hrMaxFormulaKey == TrainingManager.HR_MAX_FORMULA_191_5) {

            //  HRmax = 191.5 - (0.007 x age2)

            return (int) (191.5 - (0.007 * age * age));
         }

         // return default, this case should never happen
         return 220 - age;
      }
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

   /**
    * @param dateTime
    * @return Returns age for this person at a specific day
    */
   private int getAge(final ZonedDateTime zonedBirthDay, final ZonedDateTime dateTime) {

      final Period age = Period.between(zonedBirthDay.toLocalDate(), dateTime.toLocalDate());

      return age.getYears();
   }

   /**
    * @return Returns birthday of this person in milliseconds from 1970-01-01T00:00:00, is 0 when
    *         birthday is not set.
    */
   public long getBirthDay() {
      return birthDay;
   }

   public ZonedDateTime getBirthDayWithDefault() {

      if (_zonedBirthDay == null) {

         if (birthDay == 0) {

            _zonedBirthDay = DEFAULT_BIRTHDAY;

         } else {

            _zonedBirthDay = TimeTools.getZonedDateTime(birthDay);
         }
      }

      return _zonedBirthDay;
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

   /**
    * @param hrMaxFormulaKey
    * @param hrMaxPulse
    * @param dateTime
    *           Date when the HR zones should be computed, this is the tour date.
    * @return Returns HR zone min/max bpm values or <code>null</code> when hr zones are not
    *         defined.
    */
   public HrZoneContext getHrZoneContext(final int hrMaxFormulaKey,
                                         final int hrMaxPulse,
                                         final ZonedDateTime birthDay,
                                         final ZonedDateTime dateTime) {

      if (hrZones == null || hrZones.isEmpty()) {
         return null;
      }

      final int age = getAge(birthDay, dateTime);
      final HrZoneContext hrZoneMinMax = _hrZoneMinMaxBpm.get(age);

      if (hrZoneMinMax != null) {
         // hr zones for the age is already available
         return hrZoneMinMax;
      }

      final int hrMax = getHrMax(hrMaxFormulaKey, hrMaxPulse, age);
      final int zoneSize = hrZones.size();

      final float[] zoneMinBmps = new float[zoneSize];
      final float[] zoneMaxBmps = new float[zoneSize];

      final ArrayList<TourPersonHRZone> hrZonesList = new ArrayList<>(hrZones);
      Collections.sort(hrZonesList);

      int prevMaxBpm = -1;

      // fill zone min/max values
      for (int zoneIndex = 0; zoneIndex < hrZones.size(); zoneIndex++) {

         final TourPersonHRZone hrZone = hrZonesList.get(zoneIndex);

         final int zoneMaxValue = hrZone.getZoneMaxValue();

         int minBpm = hrZone.getZoneMinValue() * hrMax / 100;

         final int maxBpm = zoneMaxValue == Integer.MAX_VALUE ? //
               Integer.MAX_VALUE
               : (zoneMaxValue * hrMax / 100);

         if (prevMaxBpm != -1) {
            // make sure that "min" is last "max + 1"
            minBpm = prevMaxBpm + 1;
         }

         zoneMinBmps[zoneIndex] = minBpm - 0.5f;
         zoneMaxBmps[zoneIndex] = maxBpm + 0.5f;

         prevMaxBpm = maxBpm;
      }

      final HrZoneContext hrZoneMinMax1 = new HrZoneContext(zoneMinBmps, zoneMaxBmps, age, hrMax);

      _hrZoneMinMaxBpm.put(age, hrZoneMinMax1);

      return hrZoneMinMax1;
   }

   /**
    * @return Returns a list with all HR zones for this person. When the person has no HR zones,
    *         the returned list is empty.
    */
   public ArrayList<TourPersonHRZone> getHrZonesSorted() {

      if (_sortedHrZones == null) {

         _sortedHrZones = new ArrayList<>();
         _sortedHrZones.addAll(hrZones);
         Collections.sort(_sortedHrZones);
      }

      return _sortedHrZones;
   }

   public String getLastName() {
      return lastName;
   }

   public int getMaxPulse() {
      return maxPulse;
   }

   /**
    * @return Return the person first name and the last name when available.
    */
   public String getName() {
      return firstName + //
            (StringUtils.isNullOrEmpty(lastName) ? //
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

   public void resetHrZones() {
      _hrZoneMinMaxBpm.clear();
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

      _hrZoneMinMaxBpm.clear();

      if (_sortedHrZones != null) {
         _sortedHrZones.clear();
         _sortedHrZones = null;
      }
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
