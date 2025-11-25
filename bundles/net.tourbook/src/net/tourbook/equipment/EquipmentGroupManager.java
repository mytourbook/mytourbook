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
package net.tourbook.equipment;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.Equipment;

import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

/**
 * Manage equipment groups
 */
public class EquipmentGroupManager {

   private static final String               CONFIG_FILE_NAME    = "equipment-groups.xml";                 //$NON-NLS-1$

   private static final Bundle               _bundle             = TourbookPlugin.getDefault().getBundle();
   private static final IPath                _stateLocation      = Platform.getStateLocation(_bundle);

   /**
    * Version number is not yet used.
    */
   private static final int                  CONFIG_VERSION      = 1;
   private static final String               ATTR_CONFIG_VERSION = "configVersion";                        //$NON-NLS-1$
   //
   private static final String               EQUIPMENT_ROOT      = "EquipmentGroups";                      //$NON-NLS-1$
   private static final String               EQUIPMENT_GROUP     = "EquipmentGroup";                       //$NON-NLS-1$
   //
   private static final String               ATTR_ID             = "ID";                                   //$NON-NLS-1$
   private static final String               ATTR_NAME           = "name";                                 //$NON-NLS-1$
   private static final String               ATTR_EQUIPMENT_IDS  = "equipmentIDs";                         //$NON-NLS-1$
   //
   private static final List<EquipmentGroup> _allEquipmentGroups = new ArrayList<>();
   private static List<EquipmentGroup>       _allEquipmentGroupsSorted;

   static {

      // load locations
      readEquipmentGroupsFromXml();
   }

   public static void addEquipmentGroup(final EquipmentGroup equipmentGroup) {

      // update model
      _allEquipmentGroups.add(equipmentGroup);

      _allEquipmentGroupsSorted = null;
   }

   private static XMLMemento create_Root() {

      final XMLMemento xmlRoot = XMLMemento.createWriteRoot(EQUIPMENT_ROOT);

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

   /**
    * @param equipmentGroup
    *
    * @return Returns a list with all equipment in the {@link EquipmentGroup} or <code>null</code>
    *         when not available
    */
   public static String createEquipmentSortedList(final EquipmentGroup equipmentGroup) {

      if (equipmentGroup == null || equipmentGroup.allEquipment.size() == 0) {
         return null;
      }

      final List<Equipment> allEquipment = new ArrayList<>(equipmentGroup.allEquipment);

      return createEquipmentSortedList(equipmentGroup.name, allEquipment);
   }

   /**
    * @param title
    *           Can be <code>null</code>
    * @param allEquipment
    *
    * @return
    */
   public static String createEquipmentSortedList(final String title, final List<Equipment> allEquipment) {

      final ArrayList<Equipment> allSortedEquipment = new ArrayList<>(allEquipment);
      Collections.sort(allSortedEquipment);

      final StringBuilder sb = new StringBuilder();

      if (title != null) {

         sb.append(title);
         sb.append(UI.NEW_LINE);
         sb.append(UI.NEW_LINE);
      }

      for (int equipmentIndex = 0; equipmentIndex < allSortedEquipment.size(); equipmentIndex++) {

         final Equipment equipment = allEquipment.get(equipmentIndex);

         if (equipmentIndex > 0) {
            sb.append(UI.NEW_LINE);
         }

         sb.append(UI.SYMBOL_BULLET + UI.SPACE + equipment.getName());
      }

      return sb.toString();
   }

   /**
    * @param equipmentGroupID
    *
    * @return Returns all equipment from the {@link EquipmentGroup} or <code>null</code> when not
    *         available
    */
   public static Set<Equipment> getEquipment(final String equipmentGroupID) {

      for (final EquipmentGroup equipmentGroup : _allEquipmentGroups) {

         if (equipmentGroup.id.equals(equipmentGroupID)) {

            return equipmentGroup.allEquipment;
         }
      }

      return null;
   }

   /**
    * @param equipmentGroupID
    *
    * @return Returns the equipment group or <code>null</code> when not available, e.g. when a group
    *         was
    *         deleted
    */
   public static EquipmentGroup getEquipmentGroup(final String equipmentGroupID) {

      if (equipmentGroupID == null) {

         return null;
      }

      for (final EquipmentGroup equipmentGroup : _allEquipmentGroups) {

         if (equipmentGroup.id.equals(equipmentGroupID)) {

            return equipmentGroup;
         }
      }

      return null;
   }

   /**
    * @param equipmentGroupID
    *
    * @return Returns the equipment group name <code>null</code> when not available, e.g. when a
    *         group was
    *         deleted
    */
   public static String getEquipmentGroupName(final String equipmentGroupID) {

      if (equipmentGroupID == null) {

         return null;
      }

      for (final EquipmentGroup equipmentGroup : _allEquipmentGroups) {

         if (equipmentGroupID.equals(equipmentGroup.id)) {

            return equipmentGroup.name;
         }
      }

      return null;
   }

   /**
    * @return Returns all {@link EquipmentGroup}'s.
    *         <p>
    *         Do not modify this list, use {@link #addEquipmentGroup(EquipmentGroup)} or
    *         {@link #removeEquipmentGroups(List)} to modify this list
    */
   public static List<EquipmentGroup> getEquipmentGroups() {

      return _allEquipmentGroups;
   }

   /**
    * @return Returns a copied list of all {@link EquipmentGroup}'s sorted by name.
    */
   public static List<EquipmentGroup> getEquipmentGroupsSorted() {

      if (_allEquipmentGroupsSorted != null) {

         return _allEquipmentGroupsSorted;
      }

      _allEquipmentGroupsSorted = new ArrayList<>(_allEquipmentGroups);

      Collections.sort(
            _allEquipmentGroupsSorted,
            (equipmentGroup1, equipmentGroup2) -> equipmentGroup1.name.compareTo(equipmentGroup2.name));

      return _allEquipmentGroupsSorted;
   }

   private static File getXmlFile() {

      final File xmlFile = _stateLocation.append(CONFIG_FILE_NAME).toFile();

      return xmlFile;
   }

   /**
    * @param xmlRoot
    *           Can be <code>null</code> when not available
    * @param allEquipmentGroups
    */
   private static void parse_10_EquipmentGroups(final XMLMemento xmlRoot,
                                                final List<EquipmentGroup> allEquipmentGroups) {

      for (final IMemento mementoGroup : xmlRoot.getChildren()) {

         final XMLMemento xmlGroup = (XMLMemento) mementoGroup;

         try {

            final String xmlConfigType = xmlGroup.getType();

            if (xmlConfigType.equals(EQUIPMENT_GROUP)) {

               // <EquipmentGroup>

               allEquipmentGroups.add(parse_22_EquipmentGroups_One(xmlGroup));
            }

         } catch (final Exception e) {
            StatusUtil.log(Util.dumpMemento(xmlGroup), e);
         }
      }
   }

   private static EquipmentGroup parse_22_EquipmentGroups_One(final XMLMemento xmlGroup) {

      final EquipmentGroup equipmentGroup = new EquipmentGroup();

      equipmentGroup.id = Util.getXmlString(xmlGroup, ATTR_ID, UUID.randomUUID().toString());
      equipmentGroup.name = Util.getXmlString(xmlGroup, ATTR_NAME, null);

      final long[] allEquipmentIDs = Util.getXmlLongArray(xmlGroup, ATTR_EQUIPMENT_IDS);

      final Set<Equipment> allXmlEquipment = new HashSet<>();
      final Map<Long, Equipment> allDbEquipment = EquipmentManager.getAllEquipment_ByID();

      for (final long equipmentID : allEquipmentIDs) {

         final Equipment equipment = allDbEquipment.get(equipmentID);

         if (equipment != null) {
            allXmlEquipment.add(equipment);
         }
      }

      equipmentGroup.allEquipment = allXmlEquipment;

      return equipmentGroup;
   }

   /**
    * Read or create configuration a xml file
    *
    * @return
    */
   private static synchronized void readEquipmentGroupsFromXml() {

      InputStreamReader reader = null;

      try {

         XMLMemento xmlRoot = null;

         // try to get locations from saved xml file
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

         parse_10_EquipmentGroups(xmlRoot, _allEquipmentGroups);

      } catch (final Exception e) {
         StatusUtil.log(e);
      } finally {
         Util.close(reader);
      }
   }

   public static void removeEquipmentGroup(final EquipmentGroup equipmentGroup) {

      // update model
      _allEquipmentGroups.remove(equipmentGroup);

      _allEquipmentGroupsSorted = null;
   }

   public static void saveState() {

      final XMLMemento xmlRoot = create_Root();

      saveState_10_EquipmentGroups(xmlRoot);

      Util.writeXml(xmlRoot, getXmlFile());
   }

   private static void saveState_10_EquipmentGroups(final XMLMemento xmlRoot) {

      for (final EquipmentGroup equipmentGroup : _allEquipmentGroups) {

         // <EquipmentGroup>
         final IMemento xmlEquipmentGroup = xmlRoot.createChild(EQUIPMENT_GROUP);
         {
            // get all equipment ID's

            final LongArrayList allEquipmentIDs = new LongArrayList();
            final Set<Equipment> allEquipment = equipmentGroup.allEquipment;

            if (allEquipment != null) {

               for (final Equipment Equipment : allEquipment) {
                  allEquipmentIDs.add(Equipment.getEquipmentId());
               }
            }

            xmlEquipmentGroup.putString(ATTR_ID, equipmentGroup.id);
            xmlEquipmentGroup.putString(ATTR_NAME, equipmentGroup.name);

            Util.setXmlLongArray(xmlEquipmentGroup, ATTR_EQUIPMENT_IDS, allEquipmentIDs.toArray());
         }
      }
   }

}
