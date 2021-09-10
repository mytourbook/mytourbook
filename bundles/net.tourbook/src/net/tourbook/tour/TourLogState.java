/*******************************************************************************
 * Copyright (C) 2005, 2019 Wolfgang Schramm and Contributors
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
package net.tourbook.tour;

public enum TourLogState {

   /**
    *
    */
   DEFAULT,

   /**
    *
    */
   OK,

   /**
    *
    */
   ERROR,

   /**
    *
    */
   EXCEPTION,

   /**
    * Show info icon in log view
    */
   INFO,

   /**
    * File is copied to the backup folder.
    */
   EASY_IMPORT_COPY,

   /**
    * File is deleted in the device folder.
    */
   EASY_IMPORT_DELETE_DEVICE,

   /**
    * File is deleted in the backup folder.
    */
   EASY_IMPORT_DELETE_BACKUP,

   /**
    * Imported tour is saved.
    */
   TOUR_SAVED,

   /**
    * A saved tour is deleted.
    */
   TOUR_DELETED,

}
