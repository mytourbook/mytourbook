/*******************************************************************************
 * Copyright (C) 2026 Wolfgang Schramm and Contributors
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
package net.tourbook.equipment;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

public class EquipmentConfigManager {

   private static final String CONFIG_FILE_NAME = "equipment-config.xml";                 //$NON-NLS-1$

   /**
    * Version number is not yet used.
    */
   private static final int    CONFIG_VERSION   = 1;

   private static final Bundle _bundle          = TourbookPlugin.getDefault().getBundle();
   private static final IPath  _stateLocation   = Platform.getStateLocation(_bundle);

   // common attributes
   private static final String ATTR_ACTIVE_CONFIG_ID = "activeConfigId"; //$NON-NLS-1$

   private static final String ATTR_ID               = "id";             //$NON-NLS-1$
   private static final String ATTR_CONFIG_NAME      = "name";           //$NON-NLS-1$

   /*
    * Root
    */
   private static final String                    TAG_ROOT                 = "EquipmentConfiguration"; //$NON-NLS-1$
   private static final String                    ATTR_CONFIG_VERSION      = "configVersion";          //$NON-NLS-1$

   private static final String                    TAG_SORT_VIEW            = "SortView";               //$NON-NLS-1$
   private static final String                    TAG_SORT_FIELDS          = "SortField";              //$NON-NLS-1$

   private static final String                    ATTR_EQUIPMENT_SORT_1    = "equipmentSort1";         //$NON-NLS-1$
   private static final String                    ATTR_EQUIPMENT_SORT_2    = "equipmentSort2";         //$NON-NLS-1$
   private static final String                    ATTR_EQUIPMENT_SORT_3    = "equipmentSort3";         //$NON-NLS-1$
   private static final String                    ATTR_PART_SERVICE_SORT_1 = "partServiceSort1";       //$NON-NLS-1$
   private static final String                    ATTR_PART_SERVICE_SORT_2 = "partServiceSort2";       //$NON-NLS-1$
   private static final String                    ATTR_PART_SERVICE_SORT_3 = "partServiceSort3";       //$NON-NLS-1$

   static final String                            CONFIG_DEFAULT_ID_1      = "#1";                     //$NON-NLS-1$
   static final String                            CONFIG_DEFAULT_ID_2      = "#2";                     //$NON-NLS-1$
   static final String                            CONFIG_DEFAULT_ID_3      = "#3";                     //$NON-NLS-1$
   static final String                            CONFIG_DEFAULT_ID_4      = "#4";                     //$NON-NLS-1$
   static final String                            CONFIG_DEFAULT_ID_5      = "#5";                     //$NON-NLS-1$
   static final String                            CONFIG_DEFAULT_ID_6      = "#6";                     //$NON-NLS-1$
   static final String                            CONFIG_DEFAULT_ID_7      = "#7";                     //$NON-NLS-1$
   static final String                            CONFIG_DEFAULT_ID_8      = "#8";                     //$NON-NLS-1$
   static final String                            CONFIG_DEFAULT_ID_9      = "#9";                     //$NON-NLS-1$
   static final String                            CONFIG_DEFAULT_ID_10     = "#10";                    //$NON-NLS-1$

   private static final List<EquipmentViewConfig> _allEquipmentViewConfigs = new ArrayList<>();

   private static EquipmentViewConfig             _activeConfig;
   private static String                          _fromXml_ActiveConfigId;

// SET_FORMATTING_OFF

   static final SortFieldUI[]                EQUIPMENT_SORT_FIELDS        = {

         new SortFieldUI("<NONE>",                 SortField.None),
         new SortFieldUI("Equipment name",         SortField.EquipmentName),
         new SortFieldUI("Equipment brand",        SortField.EquipmentBrand),
         new SortFieldUI("Equipment model",        SortField.EquipmentModel),

         new SortFieldUI("Collate ID",             SortField.CollateID),

         new SortFieldUI("First used date",        SortField.DateFirstUsed),
         new SortFieldUI("Purchased date",         SortField.DatePurchased),
   };

   static final SortFieldUI[]                PART_SORT_FIELDS        = {

         new SortFieldUI("<NONE>",                 SortField.None),
         new SortFieldUI("Part name",              SortField.EquipmentName),
         new SortFieldUI("Part brand",             SortField.EquipmentBrand),
         new SortFieldUI("Part model",             SortField.EquipmentModel),

         new SortFieldUI("Collate ID",             SortField.CollateID),

         new SortFieldUI("First used date",        SortField.DateFirstUsed),
         new SortFieldUI("Purchased date",         SortField.DatePurchased),

         new SortFieldUI("Parts before services",  SortField.PartsBeforeServices),
         new SortFieldUI("Services before parts",  SortField.ServicesBeforeParts),
   };

// SET_FORMATTING_ON

   static class SortFieldUI {

      String    label;
      SortField sortField;

      public SortFieldUI(final String label, final SortField sortField) {

         this.label = label;
         this.sortField = sortField;
      }
   }

   private static XMLMemento create_Root() {

      final XMLMemento xmlRoot = XMLMemento.createWriteRoot(TAG_ROOT);

      // date/time
      xmlRoot.putString(Util.ATTR_ROOT_DATETIME, TimeTools.now().toString());

      // plugin version
      final Version version = _bundle.getVersion();
      xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MAJOR, version.getMajor());
      xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MINOR, version.getMinor());
      xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MICRO, version.getMicro());
      xmlRoot.putString(Util.ATTR_ROOT_VERSION_QUALIFIER, version.getQualifier());

      // config version
      xmlRoot.putInteger(ATTR_CONFIG_VERSION, CONFIG_VERSION);

      return xmlRoot;
   }

   private static void createDefaults() {

      _allEquipmentViewConfigs.clear();

      // append custom configurations
      for (int configIndex = 1; configIndex < 11; configIndex++) {

         _allEquipmentViewConfigs.add(createDefaults_One(configIndex));
      }
   }

   /**
    * @param configIndex
    *           Index starts with 1.
    *
    * @return
    */
   private static EquipmentViewConfig createDefaults_One(final int configIndex) {

      final EquipmentViewConfig config = new EquipmentViewConfig();

// SET_FORMATTING_OFF

      switch (configIndex) {

      case 1:
         config.name                   = config.defaultId = CONFIG_DEFAULT_ID_1;
         break;

      case 2:
         config.name                   = config.defaultId = CONFIG_DEFAULT_ID_2;
         break;

      case 3:
         config.name                   = config.defaultId = CONFIG_DEFAULT_ID_3;
         break;

      case 4:
         config.name                   = config.defaultId = CONFIG_DEFAULT_ID_4;
         break;

      case 5:
         config.name                   = config.defaultId = CONFIG_DEFAULT_ID_5;
         break;

      case 6:
         config.name                   = config.defaultId = CONFIG_DEFAULT_ID_6;
         break;

      case 7:
         config.name                   = config.defaultId = CONFIG_DEFAULT_ID_7;
         break;

      case 8:
         config.name                   = config.defaultId = CONFIG_DEFAULT_ID_8;
         break;

      case 9:
         config.name                   = config.defaultId = CONFIG_DEFAULT_ID_9;
         break;

      case 10:
         config.name                   = config.defaultId = CONFIG_DEFAULT_ID_10;
         break;
      }

// SET_FORMATTING_ON

      return config;
   }

   private static void createXml_FromConfig(final EquipmentViewConfig config, final IMemento xmlAllConfigs) {

      // <SortFields>
      final IMemento xmlConfig = xmlAllConfigs.createChild(TAG_SORT_FIELDS);
      {
         xmlConfig.putString(ATTR_ID, config.id);
         xmlConfig.putString(ATTR_CONFIG_NAME, config.name);

         Util.setXmlEnum(xmlConfig, ATTR_EQUIPMENT_SORT_1, config.equipmentSort1);
         Util.setXmlEnum(xmlConfig, ATTR_EQUIPMENT_SORT_2, config.equipmentSort2);
         Util.setXmlEnum(xmlConfig, ATTR_EQUIPMENT_SORT_3, config.equipmentSort3);

         Util.setXmlEnum(xmlConfig, ATTR_PART_SERVICE_SORT_1, config.partServiceSort1);
         Util.setXmlEnum(xmlConfig, ATTR_PART_SERVICE_SORT_2, config.partServiceSort2);
         Util.setXmlEnum(xmlConfig, ATTR_PART_SERVICE_SORT_3, config.partServiceSort3);
      }
   }

   public static EquipmentViewConfig getActiveConfig() {

      if (_activeConfig == null) {
         readConfigFromXml();
      }

      return _activeConfig;
   }

   /**
    * @return Returns the index for the {@link #_activeConfig}, the index starts with 0.
    */
   public static int getActiveConfigIndex() {

      final EquipmentViewConfig activeConfig = getActiveConfig();

      for (int configIndex = 0; configIndex < _allEquipmentViewConfigs.size(); configIndex++) {

         final EquipmentViewConfig config = _allEquipmentViewConfigs.get(configIndex);

         if (config.equals(activeConfig)) {
            return configIndex;
         }
      }

      // this case should not happen but ensure that a correct config is set

      setActiveConfig(_allEquipmentViewConfigs.get(0));

      return 0;
   }

   public static List<EquipmentViewConfig> getAllConfigs() {

      // ensure configs are loaded
      getActiveConfig();

      return _allEquipmentViewConfigs;
   }

   private static EquipmentViewConfig getConfig() {

      EquipmentViewConfig activeConfig = null;

      if (_fromXml_ActiveConfigId != null) {

         // ensure config id belongs to a config which is available

         for (final EquipmentViewConfig config : _allEquipmentViewConfigs) {

            if (config.id.equals(_fromXml_ActiveConfigId)) {

               activeConfig = config;
               break;
            }
         }
      }

      if (activeConfig == null) {

         // this case should not happen, create a config

         StatusUtil.logInfo("Created default config for equipment properties");//$NON-NLS-1$

         createDefaults();

         activeConfig = _allEquipmentViewConfigs.get(0);
      }

      return activeConfig;
   }

   private static File getConfigXmlFile() {

      final File configFile = _stateLocation.append(CONFIG_FILE_NAME).toFile();

      return configFile;
   }

   private static void parse_210_SortConfig(final XMLMemento xmlConfig, final EquipmentViewConfig config) {

// SET_FORMATTING_OFF

      config.id                  = Util.getXmlString(xmlConfig,            ATTR_ID,                   UUID.randomUUID().toString());
      config.name                = Util.getXmlString(xmlConfig,            ATTR_CONFIG_NAME,          UI.EMPTY_STRING);


      config.equipmentSort1      = (SortField) Util.getXmlEnum(xmlConfig,  ATTR_EQUIPMENT_SORT_1,     SortField.None);
      config.equipmentSort2      = (SortField) Util.getXmlEnum(xmlConfig,  ATTR_EQUIPMENT_SORT_2,     SortField.None);
      config.equipmentSort3      = (SortField) Util.getXmlEnum(xmlConfig,  ATTR_EQUIPMENT_SORT_3,     SortField.None);

      config.partServiceSort1    = (SortField) Util.getXmlEnum(xmlConfig,  ATTR_PART_SERVICE_SORT_1,  SortField.None);
      config.partServiceSort2    = (SortField) Util.getXmlEnum(xmlConfig,  ATTR_PART_SERVICE_SORT_2,  SortField.None);
      config.partServiceSort3    = (SortField) Util.getXmlEnum(xmlConfig,  ATTR_PART_SERVICE_SORT_3,  SortField.None);

// SET_FORMATTING_ON
   }

   /**
    * Read or create configuration a xml file
    *
    * @return
    */
   private static void readConfigFromXml() {

      InputStreamReader reader = null;

      try {

         XMLMemento xmlRoot = null;

         // try to get layer structure from saved xml file
         final File layerFile = getConfigXmlFile();
         final String absoluteLayerPath = layerFile.getAbsolutePath();

         final File inputFile = new File(absoluteLayerPath);
         if (inputFile.exists()) {

            try {

               reader = new InputStreamReader(new FileInputStream(inputFile), UI.UTF_8);
               xmlRoot = XMLMemento.createReadRoot(reader);

            } catch (final Exception e) {
               // ignore
            }
         }

         // parse xml and set states
         restoreState_10(xmlRoot, _allEquipmentViewConfigs);

         // ensure config is created

         if (_allEquipmentViewConfigs.isEmpty()) {
            createDefaults();
         }

         setActiveConfig(getConfig());

      } catch (final Exception e) {
         StatusUtil.log(e);
      } finally {
         Util.close(reader);
      }
   }

   public static void resetActiveConfiguration() {

      // do not replace the name
      final String oldName = _activeConfig.name;

      final int activeConfigIndex = getActiveConfigIndex();

      // remove old config
      _allEquipmentViewConfigs.remove(_activeConfig);

      // create new config
      final int configID = activeConfigIndex + 1;
      final EquipmentViewConfig newConfig = createDefaults_One(configID);
      newConfig.name = oldName;

      // update model
      setActiveConfig(newConfig);
      _allEquipmentViewConfigs.add(activeConfigIndex, newConfig);
   }

   public static void resetAllConfigurations() {

      createDefaults();

      setActiveConfig(_allEquipmentViewConfigs.get(0));
   }

   private static void restoreState_10(final XMLMemento xmlRoot,
                                       final List<EquipmentViewConfig> allequipmentviewconfigs) {

      if (xmlRoot == null) {
         return;
      }

      final XMLMemento xmlSortView = (XMLMemento) xmlRoot.getChild(TAG_SORT_VIEW);

      if (xmlSortView == null) {
         return;
      }

      _fromXml_ActiveConfigId = Util.getXmlString(xmlSortView, ATTR_ACTIVE_CONFIG_ID, null);

      for (final IMemento mementoConfig : xmlSortView.getChildren()) {

         final XMLMemento xmlConfig = (XMLMemento) mementoConfig;

         try {

            final String xmlConfigType = xmlConfig.getType();

            if (xmlConfigType.equals(TAG_SORT_FIELDS)) {

               // <Track>

               final EquipmentViewConfig config = new EquipmentViewConfig();

               parse_210_SortConfig(xmlConfig, config);

               allequipmentviewconfigs.add(config);
            }

         } catch (final Exception e) {
            StatusUtil.log(Util.dumpMemento(xmlConfig), e);
         }
      }
   }

   public static void saveState() {

      if (_activeConfig == null) {

         // this can happen when not yet used

         return;
      }

      final XMLMemento xmlRoot = create_Root();

      saveState_SortView(xmlRoot);

      Util.writeXml(xmlRoot, getConfigXmlFile());
   }

   /**
    * View sorting
    */
   private static void saveState_SortView(final XMLMemento xmlRoot) {

      final IMemento xmlSortView = xmlRoot.createChild(TAG_SORT_VIEW);
      {
         xmlSortView.putString(ATTR_ACTIVE_CONFIG_ID, _activeConfig.id);

         for (final EquipmentViewConfig config : _allEquipmentViewConfigs) {
            createXml_FromConfig(config, xmlSortView);
         }
      }
   }

   public static void setActiveConfig(final EquipmentViewConfig equipmentViewConfig) {

      _activeConfig = equipmentViewConfig;
   }
}
