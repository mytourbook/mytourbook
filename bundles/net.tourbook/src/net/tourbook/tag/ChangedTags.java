/*******************************************************************************
 * Copyright (C) 2005, 2020 Wolfgang Schramm and Contributors
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
package net.tourbook.tag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;

public class ChangedTags {

   private HashMap<Long, TourTag> _modifiedTags = null;
   private List<TourData>         _modifiedTours;

   private boolean                _isAddMode;

   /**
    * Creates a copy of the modifiedTags parameter and modifiedTours parameter
    *
    * @param modifiedTags
    *           tags which are added or removed from the tours
    * @param modifiedTours
    *           tours which have been modified
    * @param isAddMode
    *           <code>true</code> when tags were added to tours, <code>false</code> when tags are
    *           removed from tours
    */
   public ChangedTags(final Map<Long, TourTag> modifiedTags,
                      final List<TourData> modifiedTours,
                      final boolean isAddMode) {

      if (_modifiedTags == null) {
         _modifiedTags = new HashMap<>();
      }

      _modifiedTags.putAll(modifiedTags);
      _modifiedTours = new ArrayList<>(modifiedTours);
      _isAddMode = isAddMode;
   }

   /**
    * @return Returns the modified tags
    */
   public Map<Long, TourTag> getModifiedTags() {
      return _modifiedTags;
   }

   public List<TourData> getModifiedTours() {
      return _modifiedTours;
   }

   public boolean isAddMode() {
      return _isAddMode;
   }

}
