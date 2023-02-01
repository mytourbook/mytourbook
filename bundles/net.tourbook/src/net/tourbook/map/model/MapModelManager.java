/*******************************************************************************
 * Copyright (C) 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.map.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.map25.animation.GLTFModel_Renderer;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

/**
 * Manage animated map models
 */
public class MapModelManager {

   private static final String        XML_FILE_NAME             = "map-models.xml";                       //$NON-NLS-1$

   private static final Bundle        _bundle                   = TourbookPlugin.getDefault().getBundle();
   private static final IPath         _stateLocation            = Platform.getStateLocation(_bundle);

   /**
    * Version number is not yet used
    */
   private static final int           CONFIG_VERSION            = 1;

   private static final String        TAG_ROOT                  = "MapModels";                            //$NON-NLS-1$
   private static final String        ATTR_CONFIG_VERSION       = "configVersion";                        //$NON-NLS-1$

   private static final String        TAG_ALL_MAP_MODELS        = "AllMapModels";                         //$NON-NLS-1$
   private static final String        TAG_MAP_MODELS            = "MapModel";                             //$NON-NLS-1$

   private static final String        ATTR_ID                   = "id";                                   //$NON-NLS-1$
   private static final String        ATTR_NAME                 = "name";                                 //$NON-NLS-1$
   private static final String        ATTR_DESCRIPTION          = "description";                          //$NON-NLS-1$
   private static final String        ATTR_FILE_PATH            = "filePath";                             //$NON-NLS-1$
   private static final String        ATTR_FORWARD_ANGLE        = "forwardAngle";                         //$NON-NLS-1$
   private static final String        ATTR_HEAD_POSITION_FACTOR = "headPositionFactor";                   //$NON-NLS-1$

   public static final String         MAP_MODEL_FILE_EXTENTION  = "gltf";                                 //$NON-NLS-1$

   /**
    * Contains all map models which are loaded from a xml file
    */
   private static ArrayList<MapModel> _allMapModels             = new ArrayList<>();

   private static MapModel            _activeModel;

   private static GLTFModel_Renderer  _gltfModelRenderer;

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

   public static MapModel getActiveModel() {

      if (_activeModel == null) {

         _activeModel = getAllModels().get(0);
      }

      return _activeModel;
   }

   public static ArrayList<MapModel> getAllModels() {

      if (_allMapModels.size() == 0) {
         restoreState();
      }

      return _allMapModels;
   }

   private static File getXmlFile() {

      final File xmlFile = _stateLocation.append(XML_FILE_NAME).toFile();

      return xmlFile;
   }

   private static void restoreState() {

      InputStreamReader reader = null;

      try {

         XMLMemento xmlRoot = null;

         // try to get map models from saved xml file
         final File xmlFile = getXmlFile();
         final String absoluteFilePath = xmlFile.getAbsolutePath();
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
         restoreState_ParseMapModels(xmlRoot, _allMapModels);

      } catch (final Exception e) {
         StatusUtil.log(e);
      } finally {
         Util.close(reader);
      }
   }

   /**
    * @param xmlRoot
    *           Can be <code>null</code> when not available
    * @param _allMapModel
    */
   private static void restoreState_ParseMapModels(final XMLMemento xmlRoot,
                                                   final ArrayList<MapModel> _allMapModel) {

      final XMLMemento xmlAllModels = (XMLMemento) xmlRoot.getChild(TAG_ALL_MAP_MODELS);

      if (xmlAllModels == null) {
         return;
      }

      for (final IMemento mementoModel : xmlAllModels.getChildren()) {

         final XMLMemento xmlModel = (XMLMemento) mementoModel;

         try {

            final String xmlConfigType = xmlModel.getType();

            if (xmlConfigType.equals(TAG_MAP_MODELS)) {

               // <MapModel>

               final MapModel model = new MapModel();

// SET_FORMATTING_OFF

               model.id                   = Util.getXmlString(xmlModel, ATTR_ID,                      Long.toString(System.nanoTime()));
               model.name                 = Util.getXmlString(xmlModel, ATTR_NAME,                    UI.EMPTY_STRING);
               model.description          = Util.getXmlString(xmlModel, ATTR_DESCRIPTION,             UI.EMPTY_STRING);
               model.filepath             = Util.getXmlString(xmlModel, ATTR_FILE_PATH,               UI.EMPTY_STRING);

               model.forwardAngle         = Util.getXmlInteger(xmlModel, ATTR_FORWARD_ANGLE,          0);
               model.headPositionFactor   = Util.getXmlFloat(  xmlModel, ATTR_HEAD_POSITION_FACTOR,   1f);

// SET_FORMATTING_ON

               _allMapModel.add(model);
            }

         } catch (final Exception e) {
            StatusUtil.log(Util.dumpMemento(xmlModel), e);
         }
      }
   }

   public static void saveState() {

      final XMLMemento xmlRoot = create_Root();

      saveState_MapModels(xmlRoot);

      Util.writeXml(xmlRoot, getXmlFile());
   }

   private static void saveState_MapModels(final XMLMemento xmlRoot) {

      // <AllMapModels>
      final IMemento xmlAllMapModels = xmlRoot.createChild(TAG_ALL_MAP_MODELS);

      for (final MapModel mapModel : _allMapModels) {

         // <MapModel>
         final IMemento xmlModel = xmlAllMapModels.createChild(TAG_MAP_MODELS);
         {
// SET_FORMATTING_OFF

            xmlModel.putString(ATTR_ID,                     mapModel.id);
            xmlModel.putString(ATTR_NAME,                   mapModel.name);
            xmlModel.putString(ATTR_DESCRIPTION,            mapModel.description);
            xmlModel.putString(ATTR_FILE_PATH,              mapModel.filepath);

            xmlModel.putInteger(ATTR_FORWARD_ANGLE,         mapModel.forwardAngle);
            xmlModel.putFloat  (ATTR_HEAD_POSITION_FACTOR,  mapModel.headPositionFactor);

// SET_FORMATTING_ON
         }
      }
   }

   /**
    * This is called from {@link net.tourbook.map25.animation.GLTFModel_Renderer#GLTFModel_Renderer}
    *
    * @param gltfModelRenderer
    */
   public static void setGLTFRenderer(final GLTFModel_Renderer gltfModelRenderer) {

      _gltfModelRenderer = gltfModelRenderer;
   }

   /**
    * This is called from {@link net.tourbook.map.model.SlideoutMapModel#onModel_Select}
    *
    * @param selectedModel
    */
   public static void setSelectedModel(final MapModel selectedModel) {

      _activeModel = selectedModel;

      _gltfModelRenderer.setupScene(selectedModel);
   }

}
