/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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

import java.util.List;
import java.util.UUID;

import net.tourbook.preferences.MapsforgeThemeStyle;

import de.byteholder.geoclipse.map.UI;

public class Map25Provider implements Cloneable {

   private String _id;
   private UUID   _uuid;

   public boolean isEnabled;
   public boolean isDefault;

   public boolean isOfflineMap;

   public String  name        = UI.EMPTY_STRING;
   public String  description = UI.EMPTY_STRING;

   /*
    * Online map provider
    */
   public String url      = UI.EMPTY_STRING;
   public String tilePath = UI.EMPTY_STRING;
   public String apiKey   = UI.EMPTY_STRING;

   /*
    * Offline map provider
    */
   public String       mapFilepath   = UI.EMPTY_STRING;
   public String       themeFilepath = UI.EMPTY_STRING;
   public String       themeStyle    = UI.EMPTY_STRING;

   public TileEncoding tileEncoding  = TileEncoding.MVT;

   /*
    * Cached theme properties
    */
   private String                    _cachedThemeFilepath;
   private List<MapsforgeThemeStyle> _cachedThemeStyles;

   public Map25Provider() {

      _uuid = UUID.randomUUID();
      _id = _uuid.toString();
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
            || (_cachedThemeFilepath != null && _cachedThemeFilepath.equals(themeFilepath) == false)) {

         // styles needs to be loaded
         mfStyles = Map25ProviderManager.loadMapsforgeThemeStyles(themeFilepath);

         // mark styles to be loaded for this filepath
         _cachedThemeFilepath = themeFilepath;

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

   @Override
   public String toString() {

      return

      getClass().getName() + "\n" //$NON-NLS-1$

            + "name           = " + name + "\n" //$NON-NLS-1$ //$NON-NLS-2$
            + "isEnabled      = " + isEnabled + "\n" //$NON-NLS-1$ //$NON-NLS-2$
            + "isDefault      = " + isDefault + "\n" //$NON-NLS-1$ //$NON-NLS-2$
            + "description    = " + description + "\n" //$NON-NLS-1$ //$NON-NLS-2$

//            + "_id            = " + _id + "\n" //$NON-NLS-1$ //$NON-NLS-2$
            + "_uuid          = " + _uuid + "\n" //$NON-NLS-1$ //$NON-NLS-2$

            + "isOfflineMap   = " + isOfflineMap + "\n" //$NON-NLS-1$ //$NON-NLS-2$
            + "tileEncoding   = " + tileEncoding + "\n" //$NON-NLS-1$ //$NON-NLS-2$

            + "url            = " + url + "\n" //$NON-NLS-1$ //$NON-NLS-2$
            + "tilePath       = " + tilePath + "\n" //$NON-NLS-1$ //$NON-NLS-2$
            + "apiKey         = " + apiKey + "\n" //$NON-NLS-1$ //$NON-NLS-2$

            + "mapFilepath    = " + mapFilepath + "\n" //$NON-NLS-1$ //$NON-NLS-2$
            + "themeFilepath  = " + themeFilepath + "\n" //$NON-NLS-1$ //$NON-NLS-2$
            + "themeStyle     = " + themeStyle + "\n" //$NON-NLS-1$ //$NON-NLS-2$

      ;
   }

}
