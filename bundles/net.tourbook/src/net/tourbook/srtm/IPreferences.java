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
package net.tourbook.srtm;

public interface IPreferences {

   static final String SRTM_USE_DEFAULT_DATA_FILEPATH      = "srtm.use.default.data-filepath";      //$NON-NLS-1$
   static final String SRTM_DATA_FILEPATH                  = "srtm.data.filepath";                  //$NON-NLS-1$

   /**
    * apply profile when it's selected in the profile list
    */
   static final String SRTM_APPLY_WHEN_PROFILE_IS_SELECTED = "srtm.apply-when-profile-is-selected"; //$NON-NLS-1$

   /**
    * contains the profile id which is currently selected
    */
   static final String SRTM_COLORS_SELECTED_PROFILE_ID     = "srtm.colors.selected-profile-id";     //$NON-NLS-1$

   /**
    * contains a unique key for the profile settings: colors, shadow and grid
    */
   static final String SRTM_COLORS_SELECTED_PROFILE_KEY    = "srtm.colors.selected-profile-key";    //$NON-NLS-1$

   static final String SRTM_RESOLUTION_VERY_FINE           = "srtm.resolution.very-fine";           //$NON-NLS-1$
   static final String SRTM_RESOLUTION_FINE                = "srtm.resolution.fine";                //$NON-NLS-1$
   static final String SRTM_RESOLUTION_ROUGH               = "srtm.resolution.rough";               //$NON-NLS-1$
   static final String SRTM_RESOLUTION_VERY_ROUGH          = "srtm.resolution.very-rough";          //$NON-NLS-1$

   static final String SRTM_SHADOW                         = "srtm.shadow";                         //$NON-NLS-1$

   // Access to SRTM server
   static final String NASA_EARTHDATA_LOGIN_USER_NAME         = "NASA_EARTHDATA_LOGIN_USER_NAME";         //$NON-NLS-1$
   static final String NASA_EARTHDATA_LOGIN_PASSWORD          = "NASA_EARTHDATA_LOGIN_PASSWORD";          //$NON-NLS-1$
   static final String NASA_EARTHDATA_ACCOUNT_VALIDATION_DATE = "NASA_EARTHDATA_ACCOUNT_VALIDATION_DATE"; //$NON-NLS-1$
}
