/*******************************************************************************
 * Copyright (C) 2013, 2025 Wolfgang Schramm and Contributors
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
package net.tourbook.photo;

import java.io.Serializable;

import net.tourbook.common.UI;

public class TourPhotoReference implements Serializable {

   private static final char NL               = UI.NEW_LINE;

   private static final long serialVersionUID = 1L;

   public long               tourId;
   public long               photoId;

   public TourPhotoReference(final long tourId, final long photoId) {

      this.photoId = photoId;
      this.tourId = tourId;
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "TourPhotoReference" + NL //      //$NON-NLS-1$

            + " tourId  = " + tourId + NL //    //$NON-NLS-1$
            + " photoId = " + photoId + NL //   //$NON-NLS-1$
      ;
   }
}
