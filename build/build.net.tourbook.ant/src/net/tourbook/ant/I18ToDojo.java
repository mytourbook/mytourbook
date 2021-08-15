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
package net.tourbook.ant;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * Convert Java property files into Dojo language files.
 */
public class I18ToDojo extends Task {

   private static final String EMPTY_STRING       = "";                                                           //$NON-NLS-1$
   private static final String NL                 = "\n";                                                         //$NON-NLS-1$
   private static final String UTF_8              = "UTF-8";                                                      //$NON-NLS-1$

   private String              FILE_CREATE_HEADER = "// created with " + I18ToDojo.class.getCanonicalName() + NL; //$NON-NLS-1$

   /**
    * Java properties file
    */
   private String              _javaProperties;
   private String              _javaPropFileName;
   private String              _javaPropFileExt;

   /**
    * Created Javascript dojo file
    */
   private String              _dojoProperties;

   private String              _i18dir;

   private String              _rootLanguage;
   private String[]            _otherLanguages;

   @Override
   public void execute() throws BuildException {

      writeDojo_Root();
      writeDojo_i18();
   }

   /**
    * Load properties from a properties file.
    *
    * @param javaProperties
    *
    * @return Returns properties from the properties file.
    */
   private Properties loadJavaProperties(final String javaProperties) {

      try (FileInputStream fileStream = new FileInputStream(new File(javaProperties))) {
         final Properties properties = new Properties();

         properties.load(fileStream);

         return properties;

      } catch (final Exception e) {
         e.printStackTrace();
      }

      return null;
   }

   /**
    * This file is created and contains the Dojo properties.
    *
    * @param properties
    *           File which contains Java properties.
    */
   public void setDojoProperties(final String properties) {

      _dojoProperties = properties;
   }

   public void setI18dir(final String i18dir) {

      _i18dir = i18dir;
   }

   /**
    * This file contains the Java text strings in a properties file format.
    *
    * @param properties
    *           File which contains Java properties.
    */
   public void setJavaProperties(final String properties) {

      _javaProperties = properties;

      final String[] fileParts = properties.split("\\."); //$NON-NLS-1$
      _javaPropFileName = fileParts[0];
      _javaPropFileExt = fileParts[1];
   }

   public void setOtherLanguages(final String otherLanguages) {
      _otherLanguages = otherLanguages.split(","); //$NON-NLS-1$
   }

   public void setRootLanguage(final String rootLanguage) {
      _rootLanguage = rootLanguage;
   }

   private void writeDojo_i18() {

      System.out.println("i18 convert"); //$NON-NLS-1$

      for (final String language : _otherLanguages) {

//         /net.tourbook.web/WebContent-dev/tourbook/search/nls/messages_de.properties
//         /net.tourbook.web/WebContent-dev/tourbook/search/nls/de/Messages.js

         final String dojoI18Folder = _i18dir + "/" + language; //$NON-NLS-1$

         final String javaI18PropFilePath = (_i18dir + "/" + _javaPropFileName + "_" + language + "." + _javaPropFileExt); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
         final String dojoI18PropFilePath = (dojoI18Folder + "/" + _dojoProperties); //$NON-NLS-1$

         /*
          * Check if java language file is available, it can be unavailable
          */
         final File i18File = new File(javaI18PropFilePath);
         if (i18File.exists() == false) {
            continue;
         }

         System.out.println("   from: " + javaI18PropFilePath); //$NON-NLS-1$
         System.out.println("     --> " + dojoI18PropFilePath); //$NON-NLS-1$

         // create dojo file
         try (BufferedWriter writer = new BufferedWriter(
               new OutputStreamWriter(
                     new FileOutputStream(dojoI18PropFilePath, false),
                     UTF_8))) {

            // ensure folder is created
            new File(dojoI18Folder).mkdirs();

            writeDojo_I18_10_Header(writer);
            writeDojo_Messages(writer, javaI18PropFilePath);
            writeDojo_I18_20_Footer(writer);

         } catch (final IOException e) {
            e.printStackTrace();
         }
      }
   }

   private void writeDojo_I18_10_Header(final BufferedWriter writer) throws IOException {

      final String header = EMPTY_STRING

            + FILE_CREATE_HEADER
            + "define(                     " + NL // //$NON-NLS-1$
            + "{                           " + NL; //$NON-NLS-1$

      writer.append(header);
   }

   private void writeDojo_I18_20_Footer(final BufferedWriter writer) throws IOException {

      final String footer = EMPTY_STRING

            + "});      " + NL; //$NON-NLS-1$

      writer.append(footer);
   }

   /**
    * @param writer
    * @param javaProperties
    *
    * @throws IOException
    */
   private void writeDojo_Messages(final BufferedWriter writer, final String javaProperties) throws IOException {

      final Properties properties = loadJavaProperties(javaProperties);

      /*
       * sort keys
       */
      final Collection<Object> keys = properties.keySet();

      final ArrayList<String> sortedKeys = new ArrayList<>();
      for (final Object key : keys) {
         if (key instanceof String) {
            sortedKeys.add((String) key);
         }
      }

      Collections.sort(sortedKeys);

      final int lastKey = sortedKeys.size() - 1;

      for (int keyIndex = 0; keyIndex < sortedKeys.size(); keyIndex++) {

         final String key = sortedKeys.get(keyIndex);
         final Object keyValue = properties.get(key);

         if (keyValue instanceof String) {

            final String value = (String) keyValue;

            // convert js string delimiter ' -> \'
            final String jsValue = value.replace("\'", "\\\'").replace("" + NL, "' + '"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

            final StringBuilder sb = new StringBuilder();

            sb.append(String.format("      %-60s : '%s'", key, jsValue)); //$NON-NLS-1$

            if (keyIndex == lastKey) {
               sb.append(NL); //$NON-NLS-1$
            } else {
               sb.append("," + NL); //$NON-NLS-1$
            }

            writer.append(sb.toString());
         }
      }
   }

   private void writeDojo_Root() {

      final String javaPropFilePath = _i18dir + "/" + _javaProperties; //$NON-NLS-1$
      final String dojoPropFilePath = _i18dir + "/" + _dojoProperties; //$NON-NLS-1$

      System.out.println("i18 root convert"); //$NON-NLS-1$
      System.out.println("   from: " + javaPropFilePath); //$NON-NLS-1$
      System.out.println("     --> " + dojoPropFilePath); //$NON-NLS-1$

      try (BufferedWriter writer = new BufferedWriter(
            new OutputStreamWriter(
                  new FileOutputStream(dojoPropFilePath, false),
                  UTF_8))) {

         writeDojo_Root_10_Header(writer);
         writeDojo_Messages(writer, javaPropFilePath);
         writeDojo_Root_20_Footer(writer);

      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   private void writeDojo_Root_10_Header(final BufferedWriter writer) throws IOException {

      final String header = EMPTY_STRING

            + "define(                                   " + NL //$NON-NLS-1$
            + "{                                         " + NL //$NON-NLS-1$
            + "   // 'root' is default language (" + _rootLanguage + ")   " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "                                          " + NL //$NON-NLS-1$
            + "   root :                                 " + NL //$NON-NLS-1$
            + "   {                                      " + NL //$NON-NLS-1$
      ;

      writer.write(header);
   }

   private void writeDojo_Root_20_Footer(final BufferedWriter writer) throws IOException {

      final String footer = EMPTY_STRING

            + "   },                                     " + NL //$NON-NLS-1$
            + "                                          " + NL //$NON-NLS-1$
            + "   // list of available languages, default (" + _rootLanguage + ") is defined in 'root'" + NL //$NON-NLS-1$ //$NON-NLS-2$
            + writeDojo_Root_22_Languages().toString()
            + "})                                        " + NL; //$NON-NLS-1$

      writer.write(footer);
   }

   private StringBuilder writeDojo_Root_22_Languages() {

      final StringBuilder sb = new StringBuilder();

      final int lastLanguage = _otherLanguages.length - 1;

      for (int languageIndex = 0; languageIndex < _otherLanguages.length; languageIndex++) {

         final String language = _otherLanguages[languageIndex];

         sb.append("\t" + language.trim() + " : true"); //$NON-NLS-1$ //$NON-NLS-2$

         if (languageIndex == lastLanguage) {
            sb.append(NL);
         } else {
            sb.append("," + NL); //$NON-NLS-1$
         }
      }

      return sb;
   }
}
