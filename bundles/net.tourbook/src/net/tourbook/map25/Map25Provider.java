/*******************************************************************************
 * Copyright (C) 2005, 2022 Wolfgang Schramm and Contributors
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
package net.tourbook.map25;

import de.byteholder.geoclipse.map.UI;

import java.util.List;
import java.util.UUID;

import net.tourbook.preferences.MapsforgeThemeStyle;

import org.oscim.theme.VtmThemes;

public class Map25Provider implements Cloneable {

   private static final String NL                    = UI.NEW_LINE;

   private String              _id;
   private UUID                _uuid;

   public boolean              isEnabled;
   public boolean              isDefault;

   public String               name                  = UI.EMPTY_STRING;
   public String               description           = UI.EMPTY_STRING;

   public VtmThemes            vtmTheme;

   /**
    * When tile encoding is {@link TileEncoding#MF} then this is an offline map provider and
    * {@link #isOfflineMap} is <code>true</code>
    */
   public TileEncoding         tileEncoding          = TileEncoding.MVT;

   /**
    * When <code>true</code> then the map provider is using the tile encoding
    * {@link TileEncoding#MF}
    */
   public boolean              isOfflineMap;

   /**
    * Online map provider
    */
   public String               online_url            = UI.EMPTY_STRING;
   public String               online_TilePath       = UI.EMPTY_STRING;
   public String               online_ApiKey         = UI.EMPTY_STRING;

   /**
    * Requires that {@link #isOfflineMap} is <code>true</code> which is setting the theme filepath
    * {@link #offline_ThemeFilepath}
    */
   public boolean              offline_IsThemeFromFile;
   public String               offline_MapFilepath   = UI.EMPTY_STRING;
   public String               offline_ThemeFilepath = UI.EMPTY_STRING;
   public String               offline_ThemeStyle    = UI.EMPTY_STRING;

   /*
    * Cached theme properties
    */
   private String                    _cachedThemeFilepath;
   private List<MapsforgeThemeStyle> _cachedThemeStyles;

   public Map25Provider() {

      setUUID();
   }

   /**
    * @param notCheckedUUID
    *           Contains a UUID string but it can be invalid.
    */
   public Map25Provider(final String notCheckedUUID) {

      UUID uuid;
      try {
         uuid = UUID.fromString(notCheckedUUID);
      } catch (final Exception e) {
         uuid = UUID.randomUUID();
      }

      _uuid = uuid;
      _id = _uuid.toString();
   }

   @Override
   public Object clone() {

      try {

         final Map25Provider clonedProvider = (Map25Provider) super.clone();

         clonedProvider._cachedThemeFilepath = null;
         clonedProvider._cachedThemeStyles = null;

         return clonedProvider;

      } catch (final CloneNotSupportedException e) {

         // this shouldn't happen, since we are Cloneable
         throw new InternalError(e);
      }
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

      final Map25Provider other = (Map25Provider) obj;
      if (_uuid == null) {
         if (other._uuid != null) {
            return false;
         }
      } else if (!_uuid.equals(other._uuid)) {
         return false;
      }

      return true;
   }

   /**
    * @return Returns the map provider {@link UUID} as string.
    */
   public String getId() {
      return _id;
   }

   /**
    * Is loading the theme styles when not yet loaded.
    *
    * @param isForceThemeStyleReload
    * @return Returns theme styles or <code>null</code> when not available.
    */
   public List<MapsforgeThemeStyle> getThemeStyles(final boolean isForceThemeStyleReload) {

      List<MapsforgeThemeStyle> mfStyles;

      if (isForceThemeStyleReload

            // styles are not yet loaded
            || _cachedThemeFilepath == null

            // check if styles for the theme filepath are not yet loaded
            || (_cachedThemeFilepath != null && _cachedThemeFilepath.equals(offline_ThemeFilepath) == false)) {

         // styles needs to be loaded
         mfStyles = Map25ProviderManager.loadMapsforgeThemeStyles(offline_ThemeFilepath);

         // mark styles to be loaded for this filepath
         _cachedThemeFilepath = offline_ThemeFilepath;

         // cache theme styles
         _cachedThemeStyles = mfStyles;

      } else {

         mfStyles = _cachedThemeStyles;
      }

      return mfStyles;
   }

   @Override
   public int hashCode() {

      return _uuid.hashCode();
   }

   public void setUUID() {

      _uuid = UUID.randomUUID();
      _id = _uuid.toString();
   }

   @Override
   public String toString() {

      return

      getClass().getName() + NL

            + "name           = " + name + NL //                           //$NON-NLS-1$
            + "isEnabled      = " + isEnabled + NL //                      //$NON-NLS-1$
            + "isDefault      = " + isDefault + NL //                      //$NON-NLS-1$
            + "description    = " + description + NL //                    //$NON-NLS-1$

            + "_uuid          = " + _uuid + NL //                          //$NON-NLS-1$

            + "isOfflineMap   = " + isOfflineMap + NL //                   //$NON-NLS-1$
            + "tileEncoding   = " + tileEncoding + NL //                   //$NON-NLS-1$
            + "theme          = " + vtmTheme + NL //                          //$NON-NLS-1$

            + "online_url              = " + online_url + NL //            //$NON-NLS-1$
            + "online_TilePath         = " + online_TilePath + NL //       //$NON-NLS-1$
            + "online_ApiKey           = " + online_ApiKey + NL //         //$NON-NLS-1$

            + "offline_MapFilepath     = " + offline_MapFilepath + NL //        //$NON-NLS-1$
            + "offline_ThemeFilepath   = " + offline_ThemeFilepath + NL //      //$NON-NLS-1$
            + "offline_ThemeStyle      = " + offline_ThemeStyle + NL //         //$NON-NLS-1$
            + "offline_IsThemeFromFile = " + offline_IsThemeFromFile + NL //    //$NON-NLS-1$

      ;
   }

}
