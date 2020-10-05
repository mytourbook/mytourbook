/*******************************************************************************
 * Copyright (C) 2005, 2020 Wolfgang Schramm and Contributors
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
package net.tourbook.tag.tour.filter;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import net.tourbook.application.ActionTourTagFilter;
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

public class TourTagFilterManager {

   private static final char                      NL                          = UI.NEW_LINE;

   private static final String                    TOUR_FILTER_FILE_NAME       = "tour-tag-filter.xml";                  //$NON-NLS-1$
   private static final int                       TOUR_FILTER_VERSION         = 1;

   private static final String                    TAG_PROFILE                 = "Profile";                              //$NON-NLS-1$
   private static final String                    TAG_ROOT                    = "TourTagFilterProfiles";                //$NON-NLS-1$

   private static final String                    ATTR_TOUR_FILTER_VERSION    = "tourTagFilterVersion";                 //$NON-NLS-1$
   private static final String                    ATTR_IS_OR_OPERATOR         = "isOrOperator";                         //$NON-NLS-1$
   static final boolean                           ATTR_IS_OR_OPERATOR_DEFAULT = true;
   private static final String                    ATTR_IS_SELECTED            = "isSelected";                           //$NON-NLS-1$
   private static final String                    ATTR_NAME                   = "name";                                 //$NON-NLS-1$
   private static final String                    ATTR_TAG_ID                 = "tagIds";                               //$NON-NLS-1$
   private static final String                    ATTR_TAG_ID_UNCHECKED       = "tagIdsUnchecked";                      //$NON-NLS-1$

   private static final String                    PARAMETER_FIRST             = " ?";                                   //$NON-NLS-1$
   private static final String                    PARAMETER_FOLLOWING         = ", ?";                                  //$NON-NLS-1$

   private static final Bundle                    _bundle                     = TourbookPlugin.getDefault().getBundle();

   private static final IPath                     _stateLocation              = Platform.getStateLocation(_bundle);
   private static final IPreferenceStore          _prefStore                  = TourbookPlugin.getPrefStore();

   private static boolean                         _isTourTagFilterEnabled;

   /**
    * Contains all available profiles.
    */
   private static ArrayList<TourTagFilterProfile> _filterProfiles             = new ArrayList<>();

   /**
    * Contains the selected profile or <code>null</code> when a profile is not selected.
    */
   private static TourTagFilterProfile            _selectedProfile;

   private static int[]                           _fireEventCounter           = new int[1];

   private static ActionTourTagFilter             _actionTourTagFilter;

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

   public static ArrayList<TourTagFilterProfile> getProfiles() {
      return _filterProfiles;
   }

   /**
    * @return Returns the selected profile or <code>null</code> when a profile is not selected.
    */
   public static TourTagFilterProfile getSelectedProfile() {
      return _selectedProfile;
   }

   /**
    * @return Returns the SQL <code>WHERE</code> part for the tag filter when a tag filter is
    *         enabled and tag's are OR'ed, otherwise <code>null</code>.
    */
   public static SQLData getSQL_WherePart() {

      if (_selectedProfile == null) {
         return null;
      }

      final long[] tagIds = _selectedProfile.tagFilterIds.toArray();

      if (_isTourTagFilterEnabled == false || tagIds.length == 0) {

         // tour tag filter is not enabled

         return null;
      }

      if (_selectedProfile.isOrOperator == false) {

         /**
          * Tags are combined with AND
          * <p>
          * This cannot simply be done by using an AND operator between tag's, it is done with an
          * inner join -> complicated
          */

         return null;
      }

      /*
       * Combine tags with OR
       */

      final ArrayList<Object> sqlParameters = new ArrayList<>();
      final StringBuilder parameterTagIds = new StringBuilder();

      for (int tagIndex = 0; tagIndex < tagIds.length; tagIndex++) {
         if (tagIndex == 0) {
            parameterTagIds.append(PARAMETER_FIRST);
         } else {
            parameterTagIds.append(PARAMETER_FOLLOWING);
         }

         sqlParameters.add(tagIds[tagIndex]);
      }

      final String sqlWhere = " AND jTdataTtag.TourTag_tagId IN (" + parameterTagIds.toString() + ")" + NL; //$NON-NLS-1$ //$NON-NLS-2$

      return new SQLData(sqlWhere, sqlParameters);
   }

   private static File getXmlFile() {

      return _stateLocation.append(TOUR_FILTER_FILE_NAME).toFile();
   }

   /**
    * @return Returns <code>true</code> when the filter tags are OR'ed or the tour tag filter is not
    *         enabled, otherwise tags's are AND'ed
    */
   public static boolean isNoTagsFilter_Or_CombineTagsWithOr() {

      boolean isNoTagFilter_Or_CombineTagsWithOr = true;
      final boolean isTourTagFilterEnabled = isTourTagFilterEnabled();

      if (isTourTagFilterEnabled && getSelectedProfile().isOrOperator == false) {
         isNoTagFilter_Or_CombineTagsWithOr = false;
      }

      return isNoTagFilter_Or_CombineTagsWithOr;
   }

   /**
    * @return Returns <code>true</code> when a tour tag filter is enabled in the current tour tag
    *         filter profile.
    */
   public static boolean isTourTagFilterEnabled() {

      if (_selectedProfile == null) {
         return false;
      }

      if (_isTourTagFilterEnabled == false || _selectedProfile.tagFilterIds.size() == 0) {

         // tour tag filter is not enabled

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
               if (TAG_PROFILE.equals(xmlProfile.getType())) {

                  final TourTagFilterProfile tagFilterProfile = new TourTagFilterProfile();

                  tagFilterProfile.name = Util.getXmlString(xmlProfile, ATTR_NAME, UI.EMPTY_STRING);
                  tagFilterProfile.isOrOperator = Util.getXmlBoolean(xmlProfile, ATTR_IS_OR_OPERATOR, true);

                  _filterProfiles.add(tagFilterProfile);

                  // set selected profile
                  if (Util.getXmlBoolean(xmlProfile, ATTR_IS_SELECTED, false)) {
                     _selectedProfile = tagFilterProfile;
                  }

                  final long[] tagIds = Util.getXmlLongArray(xmlProfile, ATTR_TAG_ID);
                  final long[] tagIdsUnchecked = Util.getXmlLongArray(xmlProfile, ATTR_TAG_ID_UNCHECKED);

                  tagFilterProfile.tagFilterIds.addAll(tagIds);
                  tagFilterProfile.tagFilterIds_Unchecked.addAll(tagIdsUnchecked);
               }
            }

         } catch (final Exception e) {
            StatusUtil.log(e);
         }
      }
   }

   public static void restoreState() {

      // is filter enabled
      _isTourTagFilterEnabled = _prefStore.getBoolean(ITourbookPreferences.APP_TOUR_TAG_FILTER_IS_SELECTED);
      _actionTourTagFilter.setSelection(_isTourTagFilterEnabled);

      readFilterProfile();
   }

   public static void saveState() {

      _prefStore.setValue(ITourbookPreferences.APP_TOUR_TAG_FILTER_IS_SELECTED, _actionTourTagFilter.getSelection());

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

      _isTourTagFilterEnabled = isEnabled;

      fireFilterModifyEvent();
   }

   static void setSelectedProfile(final TourTagFilterProfile selectedProfile) {

      _selectedProfile = selectedProfile;
   }

   public static void setTourTagFilterAction(final ActionTourTagFilter actionTourTagFilter) {

      _actionTourTagFilter = actionTourTagFilter;
   }

   /**
    * @return
    */
   private static XMLMemento writeFilterProfile() {

      XMLMemento xmlRoot = null;

      try {

         xmlRoot = writeFilterProfile_10_Root();

         // loop: profiles
         for (final TourTagFilterProfile tagFilterProfile : _filterProfiles) {

            final IMemento xmlProfile = xmlRoot.createChild(TAG_PROFILE);

            xmlProfile.putString(ATTR_NAME, tagFilterProfile.name);
            xmlProfile.putBoolean(ATTR_IS_OR_OPERATOR, tagFilterProfile.isOrOperator);

            // set flag for active profile
            if (tagFilterProfile == _selectedProfile) {
               xmlProfile.putBoolean(ATTR_IS_SELECTED, true);
            }

            Util.setXmlLongArray(xmlProfile, ATTR_TAG_ID, tagFilterProfile.tagFilterIds.toArray());
            Util.setXmlLongArray(xmlProfile, ATTR_TAG_ID_UNCHECKED, tagFilterProfile.tagFilterIds_Unchecked.toArray());
         }

      } catch (final Exception e) {
         StatusUtil.log(e);
      }

      return xmlRoot;
   }

   private static XMLMemento writeFilterProfile_10_Root() {

      final XMLMemento xmlRoot = XMLMemento.createWriteRoot(TAG_ROOT);

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
