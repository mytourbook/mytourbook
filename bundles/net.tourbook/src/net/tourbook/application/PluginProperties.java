/**
 * The Hacked NLS (National Language Support) system.
 * <p>
 * Singleton.
 * <p>
 * Source:
 * https://stackoverflow.com/questions/673265/plugin-properties-mechanism-in-eclipse-rcp?rq=4#answer-801383
 *
 * @author mima
 */
package net.tourbook.application;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;

/**
 * Access texts from the resource file "plugin.properties"
 */
public final class PluginProperties {

   private static final PluginProperties instance = new PluginProperties();

   private final Map<String, String>     translations;

   private final Set<String>             knownMissing;

   /**
    * Create the NLS singleton.
    */
   private PluginProperties() {

      translations = new HashMap<>();
      knownMissing = new HashSet<>();
   }

   /**
    * @return The NLS instance.
    */
   public static PluginProperties getInstance() {

      return instance;
   }

   /**
    * @param key
    *           The key to translate.
    * @param arguments
    *           Array of arguments to format into the translated text. May be empty.
    * @return The formatted translated string.
    */
   public static String getText(final String key, final Object... arguments) {

      return getInstance().getTranslated(key, arguments);
   }

   private String getFileName(final String baseName, final String... arguments) {

      String name = baseName;
      for (final String argument : arguments) {
         name += "_" + argument;
      }

      return name + ".properties";
   }

   private URL getLocalizedEntry(final String baseName, final Bundle bundle) {

      final Locale locale = Locale.getDefault();

      URL entry = bundle.getEntry(getFileName(baseName, locale.getLanguage(), locale.getCountry()));

      if (entry == null) {
         entry = bundle.getResource(getFileName(baseName, locale.getLanguage(), locale.getCountry()));
      }

      if (entry == null) {
         entry = bundle.getEntry(getFileName(baseName, locale.getLanguage()));
      }

      if (entry == null) {
         entry = bundle.getResource(getFileName(baseName, locale.getLanguage()));
      }

      if (entry == null) {
         entry = bundle.getEntry(getFileName(baseName));
      }

      if (entry == null) {
         entry = bundle.getResource(getFileName(baseName));
      }

      return entry;
   }

   /**
    * @param key
    *           The key to translate.
    * @param arguments
    *           Array of arguments to format into the translated text. May be empty.
    * @return The formatted translated string.
    */
   public String getTranslated(final String key, final Object... arguments) {

      String translation = translations.get(key);

      if (translation != null) {

         if (arguments != null) {
            translation = MessageFormat.format(translation, arguments);
         }

      } else {

         translation = "!! " + key;

         if (!knownMissing.contains(key)) {
            warn("Could not find any translation text for " + key, null);
            knownMissing.add(key);
         }
      }

      return translation;
   }

   /**
    * Populates the NLS key/value pairs for the current locale.
    * <p>
    * Plugin localization files may have any name as long as it is declared in the Manifest under
    * the Bundle-Localization key.
    * <p>
    * Fragments <b>MUST</b> define their localization using the base name 'fragment'.
    * This is due to the fact that I have no access to the Bundle-Localization key for the
    * fragment.
    * This may change.
    *
    * @param bundle
    *           The bundle to use for population.
    */
   public void populate(final Bundle bundle) {

      final String baseName = bundle.getHeaders().get("Bundle-Localization");

      populate(getLocalizedEntry(baseName, bundle));
      populate(getLocalizedEntry("fragment", bundle));
   }

   private void populate(final URL resourceUrl) {

      if (resourceUrl != null) {

         final Properties props = new Properties();
         InputStream stream = null;

         try {
            stream = resourceUrl.openStream();
            props.load(stream);
         } catch (final IOException e) {
            warn("Could not open the resource file " + resourceUrl, e);
         } finally {
            try {
               stream.close();
            } catch (final IOException e) {
               warn("Could not close stream for resource file " + resourceUrl, e);
            }
         }

         for (final Object key : props.keySet()) {
            translations.put((String) key, (String) props.get(key));
         }
      }
   }

   private void warn(final String string, final Throwable cause) {

      Status status;

      if (cause == null) {

         status = new Status(IStatus.ERROR, TourbookPlugin.PLUGIN_ID, string);
      } else {

         status = new Status(IStatus.ERROR, TourbookPlugin.PLUGIN_ID, string, cause);
      }

      TourbookPlugin.getDefault().getLog().log(status);
   }
}
