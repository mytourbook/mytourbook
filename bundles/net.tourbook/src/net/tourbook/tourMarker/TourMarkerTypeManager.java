/*******************************************************************************
 * Copyright (C) 2025 Wolfgang Schramm and Contributors
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
package net.tourbook.tourMarker;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

/**
 * Manage tour marker types
 */
public class TourMarkerTypeManager {

   private static final String              USER_CONFIG_FILE_NAME       = "tour-marker-types.xml";                //$NON-NLS-1$

   /**
    * Version number is not yet used
    */
   private static final int                 CONFIG_VERSION              = 1;

   private static final String              TAG_ROOT                    = "TourMarkerConfiguration";              //$NON-NLS-1$
   private static final String              ATTR_CONFIG_VERSION         = "configVersion";                        //$NON-NLS-1$

   private static final String              TAG_ALL_TOUR_MARKER_CONFIGS = "AllTourMarkerConfigs";                 //$NON-NLS-1$
   private static final String              TAG_TOUR_MARKER_CONFIG      = "TourMarkerConfig";                     //$NON-NLS-1$

   private static final String              ATTR_DESCRIPTION            = "description";                          //$NON-NLS-1$
   private static final String              ATTR_MARKER_TYPE            = "markerType";                           //$NON-NLS-1$
   private static final String              ATTR_NAME                   = "name";                                 //$NON-NLS-1$

   private static final Bundle              _bundle                     = TourbookPlugin.getDefault().getBundle();
   private static final IPath               _stateLocation              = Platform.getStateLocation(_bundle);

   /**
    * Contains all tour marker configs which are loaded from a xml file
    */
   private static Set<TourMarkerType> _allTourMarkerTypes         = new HashSet<>();

   private static XMLMemento create_Root() {

      final XMLMemento xmlRoot = XMLMemento.createWriteRoot(TAG_ROOT);

// SET_FORMATTING_OFF

      // date/time
      xmlRoot.putString(   Util.ATTR_ROOT_DATETIME,            TimeTools.now().toString());

      // plugin version
      final Version version = _bundle.getVersion();

      xmlRoot.putInteger(  Util.ATTR_ROOT_VERSION_MAJOR,       version.getMajor());
      xmlRoot.putInteger(  Util.ATTR_ROOT_VERSION_MINOR,       version.getMinor());
      xmlRoot.putInteger(  Util.ATTR_ROOT_VERSION_MICRO,       version.getMicro());
      xmlRoot.putString(   Util.ATTR_ROOT_VERSION_QUALIFIER,   version.getQualifier());

      // config version
      xmlRoot.putInteger(  ATTR_CONFIG_VERSION,                CONFIG_VERSION);

// SET_FORMATTING_ON

      return xmlRoot;
   }

   public static Set<TourMarkerType> getAllTourMarkerTypes() {

      if (_allTourMarkerTypes.size() == 0) {
         restoreState();
      }

      return _allTourMarkerTypes;
   }

   private static File getUserConfigFile() {

      final File xmlFile = _stateLocation.append(USER_CONFIG_FILE_NAME).toFile();

      return xmlFile;
   }

   public static boolean importMapModel(final String xmlConfigFile) {

      if (xmlConfigFile == null) {
         return false;
      }

      boolean isImported = false;
      InputStreamReader reader = null;

      try {

         XMLMemento xmlRoot = null;

         // try to get map models from saved xml file
         final File inputFile = new File(xmlConfigFile);

         if (inputFile.exists()) {

            try {

               reader = new InputStreamReader(new FileInputStream(inputFile), UI.UTF_8);
               xmlRoot = XMLMemento.createReadRoot(reader);

            } catch (final Exception e) {
               // ignore
            }
         }

         if (xmlRoot == null) {
            return false;
         }

         // get the folder path from the file path
         final Path xmlConfigPath = Paths.get(xmlConfigFile).getParent();

         // parse xml
         isImported = importMarkerConfig(xmlRoot, xmlConfigPath);

      } catch (final Exception e) {
         StatusUtil.log(e);
      } finally {
         Util.close(reader);
      }

      return isImported;
   }

   private static boolean importMarkerConfig(final XMLMemento xmlRoot, final Path xmlConfigPath) {

      boolean returnValue = false;

      for (final IMemento mementoModel : xmlRoot.getChildren()) {

         final XMLMemento xmlModel = (XMLMemento) mementoModel;

         try {

            final String xmlConfigType = xmlModel.getType();

            if (xmlConfigType.equals(TAG_TOUR_MARKER_CONFIG)) {

               // <TourMarkerConfig>

               final TourMarkerType model = new TourMarkerType();

// SET_FORMATTING_OFF

//               model.description          = Util.getXmlString( xmlModel,   ATTR_DESCRIPTION,          UI.EMPTY_STRING);
//               model.filepath             = modelFilePath;
//               model.id                   = Util.getXmlString( xmlModel,   ATTR_ID,                   Long.toString(System.nanoTime()));
//               model.name                 = Util.getXmlString( xmlModel,   ATTR_NAME,                 UI.EMPTY_STRING);
//
//               model.forwardAngle         = Util.getXmlInteger(xmlModel,   ATTR_FORWARD_ANGLE,        0);
//               model.headPositionFactor   = Util.getXmlFloat(  xmlModel,   ATTR_HEAD_POSITION_FACTOR, 1f);

// SET_FORMATTING_ON

               _allTourMarkerTypes.add(model);

               returnValue = true;
            }

         } catch (final Exception e) {
            StatusUtil.log(Util.dumpMemento(xmlModel), e);
         }
      }

      return returnValue;
   }

   private static void restoreState() {


      restoreState_10_ReadXmlFile(_bundle, getUserConfigFile());

   }

   private static void restoreState_10_ReadXmlFile(final Bundle bundle, final File xmlConfigFile) {

      if (xmlConfigFile == null) {
         return;
      }

      InputStreamReader reader = null;

      try {

         XMLMemento xmlRoot = null;

         // try to get map models from saved xml file
         final String absoluteFilePath = xmlConfigFile.getAbsolutePath();
         final File inputFile = new File(absoluteFilePath);

         if (inputFile.exists()) {

            try {

               reader = new InputStreamReader(new FileInputStream(inputFile), UI.UTF_8);
               xmlRoot = XMLMemento.createReadRoot(reader);

            } catch (final Exception e) {
               // ignore
            }
         }

         if (xmlRoot == null) {
            return;
         }

         // parse xml
//         restoreState_50_ParseMapModels(bundle, xmlRoot);
//         restoreState_60_ParseOptions(xmlRoot);

      } catch (final Exception e) {
         StatusUtil.log(e);
      } finally {
         Util.close(reader);
      }
   }


   public static void saveState() {

      final XMLMemento xmlRoot = create_Root();

      saveState_20_MapModels(xmlRoot);

      Util.writeXml(xmlRoot, getUserConfigFile());
   }

   private static void saveState_20_MapModels(final XMLMemento xmlRoot) {

      // <AllMapModels>
      final IMemento xmlAllMapModels = xmlRoot.createChild(TAG_ALL_TOUR_MARKER_CONFIGS);

   }

}
