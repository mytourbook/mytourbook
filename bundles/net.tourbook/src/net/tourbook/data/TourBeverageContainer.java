/*******************************************************************************
 * Copyright (C) 2024 Frédéric Bard
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
public class TourBeverageContainer {

   /**
    * Unique id for the {@link TourBeverageContainer} entity
    */
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private long   containerId = TourDatabase.ENTITY_IS_NOT_SAVED;

   private String name;

   //todo fb in mL?
   private float capacity;

   public TourBeverageContainer() {}

   public TourBeverageContainer(final String name, final float capacity) {

      this.name = name;
      this.capacity = capacity;
   }

   public float getCapacity() {
      return capacity;
   }

   public long getContainerId() {
      return containerId;
   }

   public String getName() {
      return name;
   }
}
