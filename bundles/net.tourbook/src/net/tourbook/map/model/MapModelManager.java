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
import java.net.URL;
import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.NIO;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.map25.animation.GLTFModel_Renderer;
import net.tourbook.model.ModelActivator;

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

   private static final String        USER_CONFIG_FILE_NAME            = "map-models.xml";                       //$NON-NLS-1$
   private static final String        DEFAULT_CONFIG_FILE_NAME_PATH    = "/map-models/default-map-models.xml";   //$NON-NLS-1$

   /**
    * Version number is not yet used
    */
   private static final int           CONFIG_VERSION                   = 1;

   private static final String        TAG_ROOT                         = "MapModels";                            //$NON-NLS-1$
   private static final String        ATTR_CONFIG_VERSION              = "configVersion";                        //$NON-NLS-1$

   private static final String        TAG_ALL_MAP_MODELS               = "AllMapModels";                         //$NON-NLS-1$
   private static final String        TAG_MAP_MODELS                   = "MapModel";                             //$NON-NLS-1$

   private static final String        ATTR_DESCRIPTION                 = "description";                          //$NON-NLS-1$
   private static final String        ATTR_FILE_PATH                   = "filePath";                             //$NON-NLS-1$
   private static final String        ATTR_FORWARD_ANGLE               = "forwardAngle";                         //$NON-NLS-1$
   private static final String        ATTR_HEAD_POSITION_FACTOR        = "headPositionFactor";                   //$NON-NLS-1$
   private static final String        ATTR_ID                          = "id";                                   //$NON-NLS-1$
   private static final String        ATTR_IS_DEFAULT_MODEL            = "isDefaultModel";                       //$NON-NLS-1$
   private static final String        ATTR_NAME                        = "name";                                 //$NON-NLS-1$

   private static final String        TAG_OPTIONS                      = "Options";                              //$NON-NLS-1$
   private static final String        ATTR_SELECTED_MODEL_ID           = "selectedModelId";                      //$NON-NLS-1$

   public static final String         MAP_MODEL_FILE_EXTENTION         = "gltf";                                 //$NON-NLS-1$

   private static final String        DEFAULT_MODEL_SKATEBOARD_ID      = "35417da1-d92a-4c33-8d0f-41e2d81d94bd"; //$NON-NLS-1$
   private static final String        DEFAULT_MODEL_HIGH_WHEELER_ID    = "76f66cbb-d3ca-447a-9874-643c9ec42399"; //$NON-NLS-1$
   private static final String        DEFAULT_MODEL_PAINTED_BICYCLE_ID = "1af34a28-157d-4a31-8065-3bb94fb81094"; //$NON-NLS-1$

   /**
    * Model ID for the skateboard model
    */
   private static final String        DEFAULT_DEFAULT_MODEL_ID         = DEFAULT_MODEL_SKATEBOARD_ID;

   private static final Bundle        _bundle                          = TourbookPlugin.getDefault().getBundle();
   private static final IPath         _stateLocation                   = Platform.getStateLocation(_bundle);

   /**
    * Contains all map models which are loaded from a xml file
    */
   private static ArrayList<MapModel> _allMapModels                    = new ArrayList<>();

   private static MapModel            _selectedModel;

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

   /**
    * @param bundleRelativeFilePathname
    * @return Returns the absolute filepath name for a bundle relative name
    */
   private static File getAbsoluteFilePath(final String bundleRelativeFilePathname) {

      try {

         final URL bundleUrl = ModelActivator.getDefault().getBundle().getEntry(bundleRelativeFilePathname);

         if (bundleUrl == null) {
            throw new Exception("Default map model file is not in bundle: " + bundleRelativeFilePathname); //$NON-NLS-1$
         }

         final String fileURL = NIO.getAbsolutePathFromBundleUrl(bundleUrl);

         return new File(fileURL);

      } catch (final Exception e) {
         StatusUtil.log(e);
      }

      return null;
   }

   public static ArrayList<MapModel> getAllModels() {

      if (_allMapModels.size() == 0) {
         restoreState();
      }

      return _allMapModels;
   }

   public static MapModel getDefaultDefaultModel() {

      for (final MapModel mapModel : getAllModels()) {

         if (mapModel.id.equals(DEFAULT_DEFAULT_MODEL_ID)) {
            return mapModel;
         }
      }

      // this should not happen
      throw new RuntimeException("The default default model is not available"); //$NON-NLS-1$
   }

   public static MapModel getSelectedModel() {

      if (_selectedModel == null) {

         // load models and set the selected model
         getAllModels();
      }

      return _selectedModel;
   }

   private static File getUserConfigFile() {

      final File xmlFile = _stateLocation.append(USER_CONFIG_FILE_NAME).toFile();

      return xmlFile;
   }

   private static void restoreState() {

      restoreState_10_ReadXmlFile(getAbsoluteFilePath(DEFAULT_CONFIG_FILE_NAME_PATH));
      restoreState_10_ReadXmlFile(getUserConfigFile());

      /*
       * Translate default model names
       */
      for (final MapModel mapModel : _allMapModels) {

         switch (mapModel.id) {
         case DEFAULT_MODEL_HIGH_WHEELER_ID:

            mapModel.name = Messages.Map_Model_Name_HighWheeler;
            break;

         case DEFAULT_MODEL_PAINTED_BICYCLE_ID:

            mapModel.name = Messages.Map_Model_Name_Bicycle;
            break;

         case DEFAULT_MODEL_SKATEBOARD_ID:

            mapModel.name = Messages.Map_Model_Name_Skateboard;
            break;
         }
      }
   }

   private static void restoreState_10_ReadXmlFile(final File xmlFile) {

      if (xmlFile == null) {
         return;
      }

      InputStreamReader reader = null;

      try {

         XMLMemento xmlRoot = null;

         // try to get map models from saved xml file
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
         restoreState_50_ParseMapModels(xmlRoot);
         restoreState_60_ParseOptions(xmlRoot);

      } catch (final Exception e) {
         StatusUtil.log(e);
      } finally {
         Util.close(reader);
      }
   }

   /**
    * @param xmlRoot
    *           Can be <code>null</code> when not available
    */
   private static void restoreState_50_ParseMapModels(final XMLMemento xmlRoot) {

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

               String modelFilePath = Util.getXmlString(xmlModel, ATTR_FILE_PATH, UI.EMPTY_STRING);
               final boolean isDefaultModel = Util.getXmlBoolean(xmlModel, ATTR_IS_DEFAULT_MODEL, false);

               if (isDefaultModel) {

                  // modelFilePath is relative to the bundle for default models

                  final File defaultFile = getAbsoluteFilePath(modelFilePath);

                  modelFilePath = defaultFile.getAbsolutePath();
               }

               final MapModel model = new MapModel();

// SET_FORMATTING_OFF

               model.description          = Util.getXmlString(xmlModel,  ATTR_DESCRIPTION,            UI.EMPTY_STRING);
               model.filepath             = modelFilePath;
               model.id                   = Util.getXmlString(xmlModel,  ATTR_ID,                     Long.toString(System.nanoTime()));
               model.isDefaultModel       = isDefaultModel;
               model.name                 = Util.getXmlString(xmlModel,  ATTR_NAME,                   UI.EMPTY_STRING);

               model.forwardAngle         = Util.getXmlInteger(xmlModel, ATTR_FORWARD_ANGLE,          0);
               model.headPositionFactor   = Util.getXmlFloat(  xmlModel, ATTR_HEAD_POSITION_FACTOR,   1f);

// SET_FORMATTING_ON

               _allMapModels.add(model);
            }

         } catch (final Exception e) {
            StatusUtil.log(Util.dumpMemento(xmlModel), e);
         }
      }
   }

   private static void restoreState_60_ParseOptions(final XMLMemento xmlRoot) {

      final XMLMemento xmlOptions = (XMLMemento) xmlRoot.getChild(TAG_OPTIONS);

      String xmlModelId = null;

      if (xmlOptions != null) {

         xmlModelId = Util.getXmlString(xmlOptions, ATTR_SELECTED_MODEL_ID, DEFAULT_DEFAULT_MODEL_ID);
      }

      setSelectedModel(xmlModelId);
   }

   public static void saveState() {

      final XMLMemento xmlRoot = create_Root();

      saveState_10_Options(xmlRoot);
      saveState_20_MapModels(xmlRoot);

      Util.writeXml(xmlRoot, getUserConfigFile());
   }

   private static void saveState_10_Options(final XMLMemento xmlRoot) {

      // <Options>
      final IMemento xmlOptions = xmlRoot.createChild(TAG_OPTIONS);

      xmlOptions.putString(ATTR_SELECTED_MODEL_ID, getSelectedModel().id);
   }

   private static void saveState_20_MapModels(final XMLMemento xmlRoot) {

      // <AllMapModels>
      final IMemento xmlAllMapModels = xmlRoot.createChild(TAG_ALL_MAP_MODELS);

      for (final MapModel mapModel : _allMapModels) {

         // skip default models
         if (mapModel.isDefaultModel) {
            continue;
         }

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

      if (_selectedModel == selectedModel) {

         // model is already selected -> nothing to do

         return;
      }

      _selectedModel = selectedModel;

      _gltfModelRenderer.setupScene(selectedModel);
   }

   private static void setSelectedModel(final String selectedModelId) {

      for (final MapModel mapModel : _allMapModels) {

         if (mapModel.id.equals(selectedModelId)) {
            _selectedModel = mapModel;
            break;
         }
      }

      if (_selectedModel == null) {

         for (final MapModel mapModel : _allMapModels) {

            if (mapModel.id.equals(DEFAULT_DEFAULT_MODEL_ID)) {
               _selectedModel = mapModel;
               break;
            }
         }

         if (_selectedModel == null) {
            throw new RuntimeException("The default default model is not available"); //$NON-NLS-1$
         }
      }
   }

   /**
    * Update UI from the {@link #_selectedModel}
    */
   public static void updateUI() {

      _gltfModelRenderer.updateUI_ModelProperties(_selectedModel);
   }

}
