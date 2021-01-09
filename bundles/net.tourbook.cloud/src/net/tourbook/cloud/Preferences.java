/*******************************************************************************
 * Copyright (C) 2020 Frédéric Bard
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
package net.tourbook.cloud;

public final class Preferences {

   /*
    * Dropbox preferences
    */
   public static final String DROPBOX_ACCESSTOKEN                = "DROPBOX_ACCESSTOKEN";                //$NON-NLS-1$
   public static final String DROPBOX_REFRESHTOKEN               = "DROPBOX_REFRESHTOKEN";               //$NON-NLS-1$
   public static final String DROPBOX_ACCESSTOKEN_EXPIRES_IN     = "DROPBOX_ACCESSTOKEN_EXPIRES_IN";     //$NON-NLS-1$
   public static final String DROPBOX_ACCESSTOKEN_ISSUE_DATETIME = "DROPBOX_ACCESSTOKEN_ISSUE_DATETIME"; //$NON-NLS-1$

   /*
    * Strava preferences
    */
   public static final String STRAVA_ACCESSTOKEN            = "STRAVA_ACCESSTOKEN";            //$NON-NLS-1$
   public static final String STRAVA_REFRESHTOKEN           = "STRAVA_REFRESHTOKEN";           //$NON-NLS-1$
   public static final String STRAVA_ACCESSTOKEN_EXPIRES_AT = "STRAVA_ACCESSTOKEN_EXPIRES_AT"; //$NON-NLS-1$
   public static final String STRAVA_ATHLETEID              = "STRAVA_ATHLETEID";              //$NON-NLS-1$
   public static final String STRAVA_ATHLETEFULLNAME        = "STRAVA_ATHLETEFULLNAME";        //$NON-NLS-1$
}
