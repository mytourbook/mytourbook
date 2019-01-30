/*******************************************************************************
 * Copyright (C) 2005, 2019 Wolfgang Schramm and Contributors
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
package net.tourbook.tour.filter.geo;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

import net.tourbook.application.ActionTourGeoFilter;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.preferences.ITourbookPreferences;

public class TourGeoFilterManager {

   private static final Bundle           _bundle                  = TourbookPlugin.getDefault().getBundle();

   private static final IPath            _stateLocation           = Platform.getStateLocation(_bundle);
   private final static IPreferenceStore _prefStore               = TourbookPlugin.getPrefStore();

   private static final String           TOUR_FILTER_FILE_NAME    = "tour-geo-filter.xml";                  //$NON-NLS-1$
   private static final int              TOUR_FILTER_VERSION      = 1;

   private static final String           TAG_ROOT                 = "TourGeoFilterItems";                   //$NON-NLS-1$

   private static final String           ATTR_TOUR_FILTER_VERSION = "tourFilterVersion";                    //$NON-NLS-1$

   private static ActionTourGeoFilter    _actionTourFilter;

   private static boolean                _isTourFilterEnabled;

   private static int[]                  _fireEventCounter        = new int[1];

   /**
    * Fire event that the tour filter has changed.
    */
   static void fireTourFilterModifyEvent() {

      _fireEventCounter[0]++;

      Display.getDefault().asyncExec(new Runnable() {

         final int __runnableCounter = _fireEventCounter[0];

         @Override
         public void run() {

            // skip all events which has not yet been executed
            if (__runnableCounter != _fireEventCounter[0]) {

               // a new event occured
               return;
            }

            _prefStore.setValue(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED, Math.random());
         }
      });

   }

   private static File getXmlFile() {

      final File layerFile = _stateLocation.append(TOUR_FILTER_FILE_NAME).toFile();

      return layerFile;
   }

   /**
    * Read filter profile xml file.
    *
    * @return
    */
   private static void readFilterProfile() {

      final File xmlFile = getXmlFile();

      if (xmlFile.exists()) {

         try (InputStreamReader reader = new InputStreamReader(new FileInputStream(xmlFile), UI.UTF_8)) {

            final XMLMemento xmlRoot = XMLMemento.createReadRoot(reader);
            for (final IMemento mementoChild : xmlRoot.getChildren()) {

//               final XMLMemento xmlProfile = (XMLMemento) mementoChild;
//               if (TAG_PROFILE.equals(xmlProfile.getType())) {
//
//                  final TourFilterProfile tourFilterProfile = new TourFilterProfile();
//
//                  tourFilterProfile.name = Util.getXmlString(xmlProfile, ATTR_NAME, UI.EMPTY_STRING);
//
//                  _filterProfiles.add(tourFilterProfile);
//
//                  // set selected profile
//                  if (Util.getXmlBoolean(xmlProfile, ATTR_IS_SELECTED, false)) {
//                     _selectedProfile = tourFilterProfile;
//                  }
//
//                  // loop: all properties
//                  for (final IMemento mementoProperty : xmlProfile.getChildren(TAG_PROPERTY)) {
//
//                     final XMLMemento xmlProperty = (XMLMemento) mementoProperty;
//
//                     final TourFilterFieldId fieldId = (TourFilterFieldId) Util.getXmlEnum(//
//                           xmlProperty,
//                           ATTR_FIELD_ID,
//                           TourFilterFieldId.TIME_TOUR_DATE);
//
//                     final TourFilterFieldOperator fieldOperator = (TourFilterFieldOperator) Util.getXmlEnum(//
//                           xmlProperty,
//                           ATTR_FIELD_OPERATOR,
//                           TourFilterFieldOperator.EQUALS);
//
//                     final TourFilterFieldConfig fieldConfig = getFieldConfig(fieldId);
//
//                     final TourFilterProperty filterProperty = new TourFilterProperty();
//
//                     filterProperty.fieldConfig = fieldConfig;
//                     filterProperty.fieldOperator = fieldOperator;
//                     filterProperty.isEnabled = Util.getXmlBoolean(xmlProperty, ATTR_IS_ENABLED, true);
//
//                     readFilterProfile_10_PropertyDetail(xmlProperty, filterProperty);
//
//                     tourFilterProfile.filterProperties.add(filterProperty);
//                  }
//               }
            }

         } catch (final Exception e) {
            StatusUtil.log(e);
         }
      }
   }

   public static void restoreState() {

//      _isTourFilterEnabled = _prefStore.getBoolean(ITourbookPreferences.APP_TOUR_DATA_FILTER_IS_SELECTED);
//      _actionTourFilter.setSelection(_isTourFilterEnabled);

      readFilterProfile();
   }

   public static void saveState() {

      _prefStore.setValue(ITourbookPreferences.APP_TOUR_DATA_FILTER_IS_SELECTED, _actionTourFilter.getSelection());

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

      _isTourFilterEnabled = isEnabled;

      fireTourFilterModifyEvent();
   }

   public static void setTourGeoFilterAction(final ActionTourGeoFilter _actionTourGeoFilter) {
      _actionTourFilter = _actionTourGeoFilter;
   }

   /**
    * @return
    */
   private static XMLMemento writeFilterProfile() {

      XMLMemento xmlRoot = null;

      try {

         xmlRoot = writeFilterProfile_10_Root();

         // loop: profiles
//         for (final TourFilterProfile tourFilterProfile : _filterProfiles) {
//
//            final IMemento xmlProfile = xmlRoot.createChild(TAG_PROFILE);
//
//            xmlProfile.putString(ATTR_NAME, tourFilterProfile.name);
//
//            // set flag for active profile
//            if (tourFilterProfile == _selectedProfile) {
//               xmlProfile.putBoolean(ATTR_IS_SELECTED, true);
//            }
//
//            // loop: properties
//            for (final TourFilterProperty filterProperty : tourFilterProfile.filterProperties) {
//
//               final TourFilterFieldConfig fieldConfig = filterProperty.fieldConfig;
//               final TourFilterFieldOperator fieldOperator = filterProperty.fieldOperator;
//
//               final IMemento xmlProperty = xmlProfile.createChild(TAG_PROPERTY);
//
//               Util.setXmlEnum(xmlProperty, ATTR_FIELD_ID, fieldConfig.fieldId);
//               Util.setXmlEnum(xmlProperty, ATTR_FIELD_OPERATOR, fieldOperator);
//               xmlProperty.putBoolean(ATTR_IS_ENABLED, filterProperty.isEnabled);
//
//               writeFilterProfile_20_PropertyDetail(xmlProperty, filterProperty);
//            }
//         }

      } catch (final Exception e) {
         StatusUtil.log(e);
      }

      return xmlRoot;
   }

   private static XMLMemento writeFilterProfile_10_Root() {

      final XMLMemento xmlRoot = XMLMemento.createWriteRoot(TAG_ROOT);

      // date/time
      xmlRoot.putString(Util.ATTR_ROOT_DATETIME, TimeTools.now().toString());

      // plugin version
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
