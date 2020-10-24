/*******************************************************************************
 * Copyright (C) 2020 Wolfgang Schramm and Contributors
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
package net.tourbook.common.measurement_system;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.tourbook.common.CommonActivator;
import net.tourbook.common.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Manage management systems
 */
public class MeasurementSystem_Manager {

   private static final int                    FILE_VERSION              = 1;

   private static ArrayList<MeasurementSystem> ALL_DEFAULT_PROFILES      = new ArrayList<>();

   private static ArrayList<MeasurementSystem> _allSystemProfiles        = new ArrayList<>();
   private static int                          _activeSystemProfileIndex = 0;

   private static final String                 STATE_FILE                = "measurement-system.xml"; //$NON-NLS-1$

   private static final String                 TAG_ROOT                  = "measurementSystem";      //$NON-NLS-1$
   private static final String                 TAG_PROFILE               = "profile";                //$NON-NLS-1$

   private static final String                 ATTR_NAME                 = "name";                   //$NON-NLS-1$
   private static final String                 ATTR_IS_ACTIVE            = "isActive";               //$NON-NLS-1$

   private static final String                 ATTR_DISTANCE             = "distance";               //$NON-NLS-1$
   private static final String                 ATTR_ELEVATION            = "elevation";              //$NON-NLS-1$
   private static final String                 ATTR_TEMPERATURE          = "temperature";            //$NON-NLS-1$
   private static final String                 ATTR_WEIGHT               = "weight";                 //$NON-NLS-1$

   private static final String                 ATTR_VERSION              = "version";                //$NON-NLS-1$

   /**
    * Define default measurement systems.
    */
   static {

// SET_FORMATTING_OFF

      ALL_DEFAULT_PROFILES.add(new MeasurementSystem(Messages.Measurement_System_Profile_Metric,

                              Distance.KILOMETER,        Elevation.METER,     Temperature.CELCIUS,          Weight.KILOGRAM));

      ALL_DEFAULT_PROFILES.add(new MeasurementSystem(Messages.Measurement_System_Profile_Imperial,

                              Distance.MILE,             Elevation.FOOT,      Temperature.FAHRENHEIT,       Weight.POUND));

      ALL_DEFAULT_PROFILES.add(new MeasurementSystem(Messages.Measurement_System_Profile_Nautic,

                              Distance.NAUTIC_MILE,      Elevation.METER,     Temperature.CELCIUS,          Weight.KILOGRAM));

      ALL_DEFAULT_PROFILES.add(new MeasurementSystem(Messages.Measurement_System_Profile_Other1,

                              Distance.KILOMETER,        Elevation.METER,     Temperature.CELCIUS,          Weight.KILOGRAM));

      ALL_DEFAULT_PROFILES.add(new MeasurementSystem(Messages.Measurement_System_Profile_Other2,

                              Distance.KILOMETER,        Elevation.METER,     Temperature.CELCIUS,          Weight.KILOGRAM));

      ALL_DEFAULT_PROFILES.add(new MeasurementSystem(Messages.Measurement_System_Profile_Other3,

                              Distance.KILOMETER,        Elevation.METER,     Temperature.CELCIUS,          Weight.KILOGRAM));

// SET_FORMATTING_ON

      // overwrite default values with saved values
      readProfiles();
   }

   private MeasurementSystem_Manager() {}

   /**
    * @return the _activeSystemProfileIndex
    */
   public static int getActiveSystemProfileIndex() {
      return _activeSystemProfileIndex;
   }

   public static ArrayList<MeasurementSystem> getAllProfiles() {
      return _allSystemProfiles;
   }

   private static void readProfiles() {

      // get loaded system profiles
      final ArrayList<MeasurementSystem> allProfiles = readProfiles_10_FromXml();

      if (allProfiles == null || allProfiles.size() == 0) {

         // profiles are not yet available -> create defaults

         for (final MeasurementSystem profile : ALL_DEFAULT_PROFILES) {
            _allSystemProfiles.add(profile.clone());
         }

         _activeSystemProfileIndex = 0;

         return;
      }
   }

   /**
    * Read profiles from a xml file.
    *
    * @return
    */
   private static ArrayList<MeasurementSystem> readProfiles_10_FromXml() {

      final IPath stateLocation = Platform.getStateLocation(CommonActivator.getDefault().getBundle());
      final File file = stateLocation.append(STATE_FILE).toFile();

      // check if file is available
      if (file.exists() == false) {
         return null;
      }

      ArrayList<MeasurementSystem> allLoadedProfiles = null;

      try (FileInputStream inputStream = new FileInputStream(file);
            InputStreamReader reader = new InputStreamReader(inputStream, UI.UTF_8)) {

         final XMLMemento xmlRoot = XMLMemento.createReadRoot(reader);

         allLoadedProfiles = readProfiles_20_Profile(xmlRoot);

      } catch (final IOException | WorkbenchException e) {
         StatusUtil.log(e);
      }

      return allLoadedProfiles;
   }

   private static ArrayList<MeasurementSystem> readProfiles_20_Profile(final XMLMemento xmlRoot) {

      final ArrayList<MeasurementSystem> allLoadedProfiles = new ArrayList<>();

//      final IMemento[] xmlAllProfiles = xmlRoot.getChildren(TAG_PROFILE);
//
//      for (final IMemento xmlProfile : xmlAllProfiles) {
//
//         final MeasurementSystem colorDef = new MeasurementSystem();
//         allLoadedProfiles.add(colorDef);
//
//         final IMemento[] xmlProfiles = xmlProfile.getChildren(TAG_COLOR_PROFILE);
//
//         for (final IMemento xmlProfile : xmlProfiles) {
//
//            final Map3ColorProfile colorProfile = new Map3ColorProfile();
//            colorDef.addProfile(colorProfile);
//
//         }
//         colorProfile.setProfileName(Util.getXmlString(xmlProfile, ATTR_NAME, UI.EMPTY_STRING));
//      }

      return allLoadedProfiles;
   }

   /**
    * Write map color data into a XML file.
    *
    * @param allClonedSystemProfiles
    */
   public static void saveState(final ArrayList<MeasurementSystem> allClonedSystemProfiles) {

      BufferedWriter writer = null;

      try {

         final IPath stateLocation = Platform.getStateLocation(CommonActivator.getDefault().getBundle());
         final File file = stateLocation.append(STATE_FILE).toFile();

         writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), UI.UTF_8));

         final XMLMemento xmlRoot = saveState_0_getXMLRoot();

         saveState_10_System(xmlRoot);

         xmlRoot.save(writer);

      } catch (final IOException e) {
         StatusUtil.log(e);
      } finally {

         if (writer != null) {
            try {
               writer.close();
            } catch (final IOException e) {
               StatusUtil.log(e);
            }
         }
      }
   }

   private static XMLMemento saveState_0_getXMLRoot() {

      Document document;
      try {

         document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

         final Element element = document.createElement(TAG_ROOT);
         element.setAttribute(ATTR_VERSION, Integer.toString(FILE_VERSION));
         document.appendChild(element);

         return new XMLMemento(document, element);

      } catch (final ParserConfigurationException e) {
         throw new Error(e.getMessage());
      }
   }

   private static void saveState_10_System(final XMLMemento xmlRoot) {

      for (int systemIndex = 0; systemIndex < _allSystemProfiles.size(); systemIndex++) {

         final MeasurementSystem system = _allSystemProfiles.get(systemIndex);

         final IMemento xmlProfile = xmlRoot.createChild(TAG_PROFILE);

         xmlProfile.putString(ATTR_NAME, system.getName());

         // set flag for the active system
         if (systemIndex == _activeSystemProfileIndex) {
            xmlProfile.putBoolean(ATTR_IS_ACTIVE, true);
         }

         Util.setXmlEnum(xmlProfile, ATTR_DISTANCE, system.getDistance());
         Util.setXmlEnum(xmlProfile, ATTR_ELEVATION, system.getElevation());
         Util.setXmlEnum(xmlProfile, ATTR_TEMPERATURE, system.getTemperature());
         Util.setXmlEnum(xmlProfile, ATTR_WEIGHT, system.getWeight());
      }
   }

   /**
    * @param _activeSystemProfileIndex
    *           the _activeSystemProfileIndex to set
    */
   public static void setActiveSystemProfileIndex(final int _activeSystemProfileIndex) {
      MeasurementSystem_Manager._activeSystemProfileIndex = _activeSystemProfileIndex;
   }

}
