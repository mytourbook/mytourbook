package net.tourbook.preferences;

import java.util.HashMap;
import java.util.Map;

/**
 * Bean: MapsforgeThemeStyle containes a visible Style
 *
 * @author telemaxx
 */
public class MapsforgeThemeStyle {

   private Map<String, String> name            = new HashMap<>();

   private String              xmlLayer;
   private String              defaultlanguage = "en";

   public String getDefaultLaguage() {
      return defaultlanguage;
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

      if ("default".equals(language)) {
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
    * @return String containing the stylename like "elv-mtb"
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
      //System.out.println("setname: " + language + " name: " + name);
      this.name.put(language, name);
   }

   public void setXmlLayer(final String xmlLayer) {
      this.xmlLayer = xmlLayer;
   }

   @Override
   public String toString() {
      return "Item [xmlLAyer=" + xmlLayer + " Name= " + name.get(defaultlanguage) + "]";
   }
}
