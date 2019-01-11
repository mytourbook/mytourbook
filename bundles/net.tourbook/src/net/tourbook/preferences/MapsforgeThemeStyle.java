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
   private String              defaultlanguage = "en"; //$NON-NLS-1$

   public String getDefaultLaguage() {
      return defaultlanguage;
   }

   /**
    * @return Returns localized style name
    */
   public String getLocaleName() {
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
    *           string like "en"
    * @return a String with the local name like "hiking"
    */
   public String getName(final String language) {

      if ("default".equals(language)) { //$NON-NLS-1$
         return name.get(defaultlanguage);
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
}
