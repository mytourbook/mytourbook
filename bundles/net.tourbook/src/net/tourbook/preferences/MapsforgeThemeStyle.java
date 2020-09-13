/*******************************************************************************
 * Copyright (C) 2019, 2020 Wolfgang Schramm and Contributors
 * Copyright (C) 2019, 2020 Thomas Theussing
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
package net.tourbook.preferences;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Bean: MapsforgeThemeStyle containes a visible style
 *
 * @author telemaxx
 */
public class MapsforgeThemeStyle {

   private static final String USER_LOCALE     = Locale.getDefault().toString();

   private Map<String, String> name            = new HashMap<>();

   private String              xmlLayer;
   private String              defaultlanguage = "en";                          //$NON-NLS-1$

   public String getDefaultLaguage() {
      return defaultlanguage;
   }

   /**
    * @return Returns localized style name
    */
   public String getLocaleName() {
      //System.out.println("#### MapsforgeThemeStyle: language , name: " + USER_LOCALE + " , " + getName(USER_LOCALE));
      return getName(USER_LOCALE);
   }

   /**
    * getting the name as map with all localizations
    *
    * @return Map<String language,String name>
    */
   public Map<String, String> getName() {
      return name;
   }

   /**
    * getting a local name of the mapstyle
    *
    * @param language
    *           string like "en" or "en_EN"
    * @return a String with the local name like "hiking"
    */
   public String getName(final String language) {

      //System.out.println("#### MapsforgeThemeStyle: language: " + language);
      //System.out.println("#### MapsforgeThemeStyle: toString: " + toString("de"));

      if ("default".equals(language)) { //$NON-NLS-1$
         return name.get(defaultlanguage);
      } else if (language.length() > 2) { //eg, when using "en_EN, then using only first 2 chars"
         if (name.containsKey(language.substring(0, 2))) {
            return name.get(language.substring(0, 2));
         } else { //is already short like "en"
            return name.get(language);
         }
      } else if (name.containsKey(language)) {
         return name.get(language);
      } else {
         return name.get(defaultlanguage);
      }
   }

   /**
    * get the style name like
    *
    * @return Returns the stylename, e.g. "elv-mtb"
    */
   public String getXmlLayer() {
      return xmlLayer;
   }

   public void setDefaultLanguage(final String language) {
      this.defaultlanguage = language;
   }

   /**
    * set the style name with a given language
    *
    * @param language
    * @param name
    */
   public void setName(final String language, final String name) {
      this.name.put(language, name);
   }

   public void setXmlLayer(final String xmlLayer) {
      this.xmlLayer = xmlLayer;
   }

   @Override
   public String toString() {

      return "MapsforgeThemeStyle " //$NON-NLS-1$

            + "xmlLayer=" + xmlLayer + " " //$NON-NLS-1$ //$NON-NLS-2$
            + "name= " + name.get(defaultlanguage) + " " //$NON-NLS-1$ //$NON-NLS-2$
            + "\n"; //$NON-NLS-1$
   }

   public String toString(final String language) {

      return "MapsforgeThemeStyle " //$NON-NLS-1$

            + "xmlLayer=" + xmlLayer + " " //$NON-NLS-1$ //$NON-NLS-2$
            + "name= " + name.get(defaultlanguage) + " " //$NON-NLS-1$ //$NON-NLS-2$
            + "\n"; //$NON-NLS-1$
   }

}
