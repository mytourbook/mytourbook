/*******************************************************************************
 * Copyright (C) 2024 Wolfgang Schramm and Contributors
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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import net.tourbook.common.UI;
import net.tourbook.data.TourTag;

public class TagGroup {

   private static final char          NL             = UI.NEW_LINE;

   private static final AtomicInteger _createCounter = new AtomicInteger();

   public String                      name;

   public List<TourTag>               tourTags       = new ArrayList<>();

   private int          _createId;

   public TagGroup() {

      _createId = _createCounter.incrementAndGet();
   }

   @Override
   public boolean equals(final Object obj) {

      if (this == obj) {
         return true;
      }

      if (obj == null) {
         return false;
      }

      if (getClass() != obj.getClass()) {
         return false;
      }

      final TagGroup other = (TagGroup) obj;

      return _createId == other._createId;
   }

   @Override
   public int hashCode() {

      return Objects.hash(_createId);
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "TagGroup" + NL

            + "[" + NL

            + " name     = " + name + NL
            + " tourTags = " + tourTags + NL

            + "]";
   }

}
