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
package net.tourbook.equipment.tour.filter;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import net.tourbook.application.ActionTourEquipmentFilter;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.SQLData;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

public class TourEquipmentFilterManager {

   private static final char                            NL                          = UI.NEW_LINE;

   private static final String                          TOUR_FILTER_FILE_NAME       = "tour-equipment-filter.xml";            //$NON-NLS-1$
   private static final int                             TOUR_FILTER_VERSION         = 1;

   private static final String                          EQUIPMENT_PROFILE           = "Profile";                              //$NON-NLS-1$
   private static final String                          EQUIPMENT_ROOT              = "TourEquipmentFilterProfiles";          //$NON-NLS-1$

   private static final String                          ATTR_TOUR_FILTER_VERSION    = "tourEquipmentFilterVersion";           //$NON-NLS-1$
   private static final String                          ATTR_IS_OR_OPERATOR         = "isOrOperator";                         //$NON-NLS-1$
   static final boolean                                 ATTR_IS_OR_OPERATOR_DEFAULT = true;
   private static final String                          ATTR_IS_SELECTED            = "isSelected";                           //$NON-NLS-1$
   private static final String                          ATTR_NAME                   = "name";                                 //$NON-NLS-1$
   private static final String                          ATTR_EQUIPMENT_ID           = "equipmentIDs";                         //$NON-NLS-1$
   private static final String                          ATTR_EQUIPMENT_ID_UNCHECKED = "equipmentIdsUnchecked";                //$NON-NLS-1$

   private static final String                          PARAMETER_FIRST             = " ?";                                   //$NON-NLS-1$
   private static final String                          PARAMETER_FOLLOWING         = ", ?";                                  //$NON-NLS-1$

   private static final Bundle                          _bundle                     = TourbookPlugin.getDefault().getBundle();

   private static final IPath                           _stateLocation              = Platform.getStateLocation(_bundle);
   private static final IPreferenceStore                _prefStore                  = TourbookPlugin.getPrefStore();

   private static boolean                               _isTourEquipmentFilterEnabled;

   /**
    * Contains all available profiles.
    */
   private static List<TourEquipmentFilterProfile> _filterProfiles             = new ArrayList<>();

   /**
    * Contains the selected profile or <code>null</code> when a profile is not selected.
    */
   private static TourEquipmentFilterProfile            _selectedProfile;

   private static int[]                                 _fireEventCounter           = new int[1];

   private static ActionTourEquipmentFilter             _actionTourEquipmentFilter;

   /**
    * Fire event that the tour filter has changed.
    */
   static void fireFilterModifyEvent() {

      _fireEventCounter[0]++;

      Display.getDefault().asyncExec(new Runnable() {

         final int __runnableCounter = _fireEventCounter[0];

         @Override
         public void run() {

            // skip all events which has not yet been executed
            if (__runnableCounter != _fireEventCounter[0]) {

               // a new event occurred
               return;
            }

            _prefStore.setValue(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED, Math.random());
         }
      });

   }

   public static List<TourEquipmentFilterProfile> getProfiles() {
      return _filterProfiles;
   }

   /**
    * @return Returns the selected profile or <code>null</code> when a profile is not selected.
    */
   public static TourEquipmentFilterProfile getSelectedProfile() {
      return _selectedProfile;
   }

   /**
    * @return Returns the SQL <code>WHERE</code> part for the equipment filter when a equipment
    *         filter is
    *         enabled and equipment's are OR'ed, otherwise <code>null</code>.
    */
   public static SQLData getSQL_WherePart() {

      if (_selectedProfile == null) {
         return null;
      }

      final long[] equipmentIds = _selectedProfile.equipmentFilterIDs.toArray();

      if (_isTourEquipmentFilterEnabled == false || equipmentIds.length == 0) {

         // tour equipment filter is not enabled

         return null;
      }

      if (_selectedProfile.isOrOperator == false) {

         /**
          * Equipment are combined with AND
          * <p>
          * This cannot simply be done by using an AND operator between equipment, it is done with
          * an
          * inner join -> complicated
          */

         return null;
      }

      /*
       * Combine equipment with OR
       */

      final ArrayList<Object> sqlParameters = new ArrayList<>();
      final StringBuilder parameterEquipmentIDs = new StringBuilder();

      for (int equipmentIndex = 0; equipmentIndex < equipmentIds.length; equipmentIndex++) {
         if (equipmentIndex == 0) {
            parameterEquipmentIDs.append(PARAMETER_FIRST);
         } else {
            parameterEquipmentIDs.append(PARAMETER_FOLLOWING);
         }

         sqlParameters.add(equipmentIds[equipmentIndex]);
      }

      final String sqlWhere = " AND jTdataTtag.TourTag_tagId IN (" + parameterEquipmentIDs.toString() + ")" + NL; //$NON-NLS-1$ //$NON-NLS-2$

      return new SQLData(sqlWhere, sqlParameters);
   }

   private static File getXmlFile() {

      return _stateLocation.append(TOUR_FILTER_FILE_NAME).toFile();
   }

   /**
    * @return Returns <code>true</code> when the filter equipment are OR'ed or the tour equipment
    *         filter is not
    *         enabled, otherwise equipment are AND'ed
    */
   public static boolean isNoTagsFilter_Or_CombineTagsWithOr() {

      boolean isNoEquipmentFilter_Or_CombineEquipmentsWithOr = true;
      final boolean isTourEquipmentFilterEnabled = isTourTagFilterEnabled();

      if (isTourEquipmentFilterEnabled && getSelectedProfile().isOrOperator == false) {
         isNoEquipmentFilter_Or_CombineEquipmentsWithOr = false;
      }

      return isNoEquipmentFilter_Or_CombineEquipmentsWithOr;
   }

   /**
    * @return Returns <code>true</code> when a tour equipment filter is enabled in the current tour
    *         equipment
    *         filter profile.
    */
   public static boolean isTourTagFilterEnabled() {

      if (_selectedProfile == null) {
         return false;
      }

      if (_isTourEquipmentFilterEnabled == false || _selectedProfile.equipmentFilterIDs.isEmpty()) {

         // tour equipment filter is not enabled

         return false;
      }

      return true;
   }

   /**
    * Read filter profile XML file.
    *
    * @return
    */
   private static void readFilterProfile() {

      final File xmlFile = getXmlFile();

      if (xmlFile.exists()) {

         try (InputStreamReader reader = new InputStreamReader(new FileInputStream(xmlFile), UI.UTF_8)) {

            final XMLMemento xmlRoot = XMLMemento.createReadRoot(reader);
            for (final IMemento mementoChild : xmlRoot.getChildren()) {

               final XMLMemento xmlProfile = (XMLMemento) mementoChild;
               if (EQUIPMENT_PROFILE.equals(xmlProfile.getType())) {

                  final TourEquipmentFilterProfile equipmentFilterProfile = new TourEquipmentFilterProfile();

                  equipmentFilterProfile.name = Util.getXmlString(xmlProfile, ATTR_NAME, UI.EMPTY_STRING);
                  equipmentFilterProfile.isOrOperator = Util.getXmlBoolean(xmlProfile, ATTR_IS_OR_OPERATOR, true);

                  _filterProfiles.add(equipmentFilterProfile);

                  // set selected profile
                  if (Util.getXmlBoolean(xmlProfile, ATTR_IS_SELECTED, false)) {
                     _selectedProfile = equipmentFilterProfile;
                  }

                  final long[] equipmentIDs = Util.getXmlLongArray(xmlProfile, ATTR_EQUIPMENT_ID);
                  final long[] equipmentIDsUnchecked = Util.getXmlLongArray(xmlProfile, ATTR_EQUIPMENT_ID_UNCHECKED);

                  equipmentFilterProfile.equipmentFilterIDs.addAll(equipmentIDs);
                  equipmentFilterProfile.equipmentFilterIds_Unchecked.addAll(equipmentIDsUnchecked);
               }
            }

         } catch (final Exception e) {
            StatusUtil.log(e);
         }
      }
   }

   public static void restoreState() {

      // is filter enabled
      _isTourEquipmentFilterEnabled = _prefStore.getBoolean(ITourbookPreferences.APP_TOUR_EQUIPMENT_FILTER_IS_SELECTED);
      _actionTourEquipmentFilter.setSelection(_isTourEquipmentFilterEnabled);

      readFilterProfile();
   }

   public static void saveState() {

      _prefStore.setValue(ITourbookPreferences.APP_TOUR_EQUIPMENT_FILTER_IS_SELECTED, _actionTourEquipmentFilter.getSelection());

      final XMLMemento xmlRoot = writeFilterProfile();
      final File xmlFile = getXmlFile();

      Util.writeXml(xmlRoot, xmlFile);
   }

   /**
    * Sets the state if the tour filter is active or not.
    *
    * @param isEnabled
    */
   public static void setFilterEnabled(final boolean isEnabled) {

      _isTourEquipmentFilterEnabled = isEnabled;

      fireFilterModifyEvent();
   }

   static void setSelectedProfile(final TourEquipmentFilterProfile selectedProfile) {

      _selectedProfile = selectedProfile;
   }

   public static void setTourEquipmentFilterAction(final ActionTourEquipmentFilter actionTourFilter_Equipment) {

      _actionTourEquipmentFilter = actionTourFilter_Equipment;
   }

   /**
    * @return
    */
   private static XMLMemento writeFilterProfile() {

      XMLMemento xmlRoot = null;

      try {

         xmlRoot = writeFilterProfile_10_Root();

         // loop: profiles
         for (final TourEquipmentFilterProfile tagFilterProfile : _filterProfiles) {

            final IMemento xmlProfile = xmlRoot.createChild(EQUIPMENT_PROFILE);

            xmlProfile.putString(ATTR_NAME, tagFilterProfile.name);
            xmlProfile.putBoolean(ATTR_IS_OR_OPERATOR, tagFilterProfile.isOrOperator);

            // set flag for active profile
            if (tagFilterProfile == _selectedProfile) {
               xmlProfile.putBoolean(ATTR_IS_SELECTED, true);
            }

            Util.setXmlLongArray(xmlProfile, ATTR_EQUIPMENT_ID, tagFilterProfile.equipmentFilterIDs.toArray());
            Util.setXmlLongArray(xmlProfile, ATTR_EQUIPMENT_ID_UNCHECKED, tagFilterProfile.equipmentFilterIds_Unchecked.toArray());
         }

      } catch (final Exception e) {
         StatusUtil.log(e);
      }

      return xmlRoot;
   }

   private static XMLMemento writeFilterProfile_10_Root() {

      final XMLMemento xmlRoot = XMLMemento.createWriteRoot(EQUIPMENT_ROOT);

      // date/time
      xmlRoot.putString(Util.ATTR_ROOT_DATETIME, TimeTools.now().toString());

      // plug-in version
      final Version version = _bundle.getVersion();
      xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MAJOR, version.getMajor());
      xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MINOR, version.getMinor());
      xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MICRO, version.getMicro());
      xmlRoot.putString(Util.ATTR_ROOT_VERSION_QUALIFIER, version.getQualifier());

      // layer structure version
      xmlRoot.putInteger(ATTR_TOUR_FILTER_VERSION, TOUR_FILTER_VERSION);

      return xmlRoot;
   }

}
