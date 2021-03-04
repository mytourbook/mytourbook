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
package net.tourbook.photo;

import java.util.ArrayList;

public enum PhotoEventId {

   /**
    * Photo attributes, e.g star rating has been modified. Event data contain an {@link ArrayList}
    * with the modified {@link Photo}'s.
    */
   PHOTO_ATTRIBUTES_ARE_MODIFIED,

   /**
    * File image path has been modified.
    */
   PHOTO_IMAGE_PATH_IS_MODIFIED,

   /**
    * Event data contains a selection with photos.
    */
   PHOTO_SELECTION,
}
