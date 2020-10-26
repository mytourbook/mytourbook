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
package net.tourbook.measurement_system;

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

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
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

   private static final String                 STATE_FILE                = "measurement-system.xml";     //$NON-NLS-1$

   private static final String                 TAG_ROOT                  = "measurementSystem";          //$NON-NLS-1$
   private static final String                 TAG_PROFILE               = "profile";                    //$NON-NLS-1$

   private static final String                 ATTR_NAME                 = "name";                       //$NON-NLS-1$
   private static final String                 ATTR_IS_ACTIVE            = "isActive";                   //$NON-NLS-1$

   private static final String                 ATTR_ATMOSPHERIC_PRESSURE = "atmosphericPressure";        //$NON-NLS-1$
   private static final String                 ATTR_DISTANCE             = "distance";                   //$NON-NLS-1$
   private static final String                 ATTR_ELEVATION            = "elevation";                  //$NON-NLS-1$
   private static final String                 ATTR_TEMPERATURE          = "temperature";                //$NON-NLS-1$
   private static final String                 ATTR_WEIGHT               = "weight";                     //$NON-NLS-1$

   private static final String                 ATTR_VERSION              = "version";                    //$NON-NLS-1$

   private static final IPreferenceStore       _prefStore                = TourbookPlugin.getPrefStore();

// SET_FORMATTING_OFF

   /**
    * Define default measurement systems
    */
   static {

// SET_FORMATTING_OFF

      ALL_DEFAULT_PROFILES.add(new MeasurementSystem(Messages.Measurement_System_Profile_Metric,

            AtmosphericPressure.MILLIBAR,          Distance.KILOMETER,        Elevation.METER,     Temperature.CELCIUS,          Weight.KILOGRAM));

      ALL_DEFAULT_PROFILES.add(new MeasurementSystem(Messages.Measurement_System_Profile_Imperial,

            AtmosphericPressure.INCH_OF_MERCURY,   Distance.MILE,             Elevation.FOOT,      Temperature.FAHRENHEIT,       Weight.POUND));

      ALL_DEFAULT_PROFILES.add(new MeasurementSystem(Messages.Measurement_System_Profile_Nautic,

            AtmosphericPressure.MILLIBAR,          Distance.NAUTIC_MILE,      Elevation.METER,     Temperature.CELCIUS,          Weight.KILOGRAM));

      ALL_DEFAULT_PROFILES.add(new MeasurementSystem(Messages.Measurement_System_Profile_Other1,

            AtmosphericPressure.MILLIBAR,          Distance.KILOMETER,        Elevation.METER,     Temperature.CELCIUS,          Weight.KILOGRAM));

      ALL_DEFAULT_PROFILES.add(new MeasurementSystem(Messages.Measurement_System_Profile_Other2,

            AtmosphericPressure.MILLIBAR,          Distance.KILOMETER,        Elevation.METER,     Temperature.CELCIUS,          Weight.KILOGRAM));

      ALL_DEFAULT_PROFILES.add(new MeasurementSystem(Messages.Measurement_System_Profile_Other3,

            AtmosphericPressure.MILLIBAR,          Distance.KILOMETER,        Elevation.METER,     Temperature.CELCIUS,          Weight.KILOGRAM));

// SET_FORMATTING_ON

      // overwrite default values with saved values
      readProfiles();
   }

   private static final SystemAtmosphericPressure _allSystemAtmosphericPressure[] = {

         new SystemAtmosphericPressure(AtmosphericPressure.MILLIBAR, Messages.Pref_System_Option_AtmosphericPressure_Millibar),
         new SystemAtmosphericPressure(AtmosphericPressure.INCH_OF_MERCURY, Messages.Pref_System_Option_AtmosphericPressure_InchOfMercury),
   };

   private static final SystemDistance            _allSystemDistances[]           = {

         new SystemDistance(Distance.KILOMETER, Messages.Pref_System_Option_Distance_Kilometer),
         new SystemDistance(Distance.MILE, Messages.Pref_System_Option_Distance_Mile),
         new SystemDistance(Distance.NAUTIC_MILE, Messages.Pref_System_Option_Distance_NauticMile),
   };

   private static final SystemElevation           _allSystemElevations[]          = {

         new SystemElevation(Elevation.METER, Messages.Pref_System_Option_Elevation_Meter),
         new SystemElevation(Elevation.FOOT, Messages.Pref_System_Option_Elevation_Foot),
   };

   private static final SystemTemperature         _allSystemTemperatures[]        = {

         new SystemTemperature(Temperature.CELCIUS, Messages.Pref_System_Option_Temperature_Celcius),
         new SystemTemperature(Temperature.FAHRENHEIT, Messages.Pref_System_Option_Temperature_Fahrenheit),
   };

   private static final SystemWeight              _allSystemWeights[]             = {

         new SystemWeight(Weight.KILOGRAM, Messages.Pref_System_Option_BodyWeight_Kilogram),
         new SystemWeight(Weight.POUND, Messages.Pref_System_Option_BodyWeight_Pound),
   };

   private MeasurementSystem_Manager() {}

   /**
    * @return Returns the currently active measurement system.
    */
   public static MeasurementSystem getActiveMeasurementSystem() {
      return _allSystemProfiles.get(_activeSystemProfileIndex);
   }

   public static SystemAtmosphericPressure getActiveSystem_AtmosphericPressure() {

      return _allSystemAtmosphericPressure[getSystemIndex_AtmosphericPressure(getActiveMeasurementSystem())];
   }

   public static SystemDistance getActiveSystem_Distance() {

      return _allSystemDistances[getSystemIndex_Distance(getActiveMeasurementSystem())];
   }

   public static SystemElevation getActiveSystem_Elevation() {

      return _allSystemElevations[getSystemIndex_Elevation(getActiveMeasurementSystem())];
   }

   /**
    * @return the _activeSystemProfileIndex
    */
   public static int getActiveSystem_ProfileIndex() {
      return _activeSystemProfileIndex;
   }

   public static SystemTemperature getActiveSystem_Temperature() {

      return _allSystemTemperatures[getSystemIndex_Temperature(getActiveMeasurementSystem())];
   }

   public static SystemWeight getActiveSystem_Weight() {

      return _allSystemWeights[getSystemIndex_Weight(getActiveMeasurementSystem())];
   }

   public static SystemAtmosphericPressure[] getAllSystem_AtmosphericPressures() {
      return _allSystemAtmosphericPressure;
   }

   public static SystemDistance[] getAllSystem_Distances() {
      return _allSystemDistances;
   }

   public static SystemElevation[] getAllSystem_Elevations() {
      return _allSystemElevations;
   }

   public static SystemTemperature[] getAllSystem_Temperatures() {
      return _allSystemTemperatures;
   }

   public static SystemWeight[] getAllSystem_Weights() {
      return _allSystemWeights;
   }

   public static ArrayList<MeasurementSystem> getCurrentProfiles() {
      return _allSystemProfiles;
   }

   /**
    * @return the aLL_DEFAULT_PROFILES
    */
   public static ArrayList<MeasurementSystem> getDefaultProfiles() {
      return ALL_DEFAULT_PROFILES;
   }

   public static int getSystemIndex_AtmosphericPressure(final MeasurementSystem selectedSystemProfile) {

      final AtmosphericPressure profilePressure = selectedSystemProfile.getAtmosphericPressure();

      for (int systemIndex = 0; systemIndex < _allSystemAtmosphericPressure.length; systemIndex++) {

         if (profilePressure.equals(_allSystemAtmosphericPressure[systemIndex].getPressure())) {
            return systemIndex;
         }
      }

      // return default
      return 0;
   }

   public static int getSystemIndex_Distance(final MeasurementSystem selectedSystemProfile) {

      final Distance profileDistance = selectedSystemProfile.getDistance();

      for (int systemIndex = 0; systemIndex < _allSystemDistances.length; systemIndex++) {

         if (profileDistance.equals(_allSystemDistances[systemIndex].getDistance())) {
            return systemIndex;
         }
      }

      // return default
      return 0;
   }

   public static int getSystemIndex_Elevation(final MeasurementSystem selectedSystemProfile) {

      final Elevation profileElevation = selectedSystemProfile.getElevation();

      for (int systemIndex = 0; systemIndex < _allSystemElevations.length; systemIndex++) {

         if (profileElevation.equals(_allSystemElevations[systemIndex].getElevation())) {
            return systemIndex;
         }
      }

      // return default
      return 0;
   }

   public static int getSystemIndex_Temperature(final MeasurementSystem selectedSystemProfile) {

      final Temperature profileTemperature = selectedSystemProfile.getTemperature();

      for (int systemIndex = 0; systemIndex < _allSystemTemperatures.length; systemIndex++) {

         if (profileTemperature.equals(_allSystemTemperatures[systemIndex].getTemperature())) {
            return systemIndex;
         }
      }

      // return default
      return 0;
   }

   public static int getSystemIndex_Weight(final MeasurementSystem selectedSystemProfile) {

      final Weight profileWeight = selectedSystemProfile.getWeight();

      for (int systemIndex = 0; systemIndex < _allSystemWeights.length; systemIndex++) {

         if (profileWeight.equals(_allSystemWeights[systemIndex].getWeight())) {
            return systemIndex;
         }
      }

      // return default
      return 0;
   }

   private static void readProfiles() {

      // get loaded system profiles
      final ArrayList<MeasurementSystem> allLoadedProfiles = readProfiles_10_FromXmlFile();

      if (allLoadedProfiles == null || allLoadedProfiles.size() == 0) {

         // profiles are not yet available -> create defaults

         for (final MeasurementSystem profile : ALL_DEFAULT_PROFILES) {
            _allSystemProfiles.add(profile.clone());
         }

         _activeSystemProfileIndex = 0;

         return;
      }

      _allSystemProfiles.addAll(allLoadedProfiles);

      /*
       * Get active profile
       */
      _activeSystemProfileIndex = 0;
      for (int profileIndex = 0; profileIndex < allLoadedProfiles.size(); profileIndex++) {
         final MeasurementSystem system = allLoadedProfiles.get(profileIndex);
         if (system.getSaveState_IsProfileActive()) {
            _activeSystemProfileIndex = profileIndex;
            break;
         }
      }
   }

   /**
    * Read profiles from a xml file.
    *
    * @return
    */
   private static ArrayList<MeasurementSystem> readProfiles_10_FromXmlFile() {

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

         allLoadedProfiles = readProfiles_20_ProfileData(xmlRoot);

      } catch (final IOException | WorkbenchException e) {
         StatusUtil.log(e);
      }

      return allLoadedProfiles;
   }

   private static ArrayList<MeasurementSystem> readProfiles_20_ProfileData(final XMLMemento xmlRoot) {

      final ArrayList<MeasurementSystem> allLoadedProfiles = new ArrayList<>();

      final IMemento[] xmlAllProfiles = xmlRoot.getChildren(TAG_PROFILE);

      for (final IMemento xmlProfile : xmlAllProfiles) {

         final boolean isProfileActive = Util.getXmlBoolean(xmlProfile, ATTR_IS_ACTIVE, false);

// SET_FORMATTING_OFF

         final String      profileName = Util.getXmlString(xmlProfile, ATTR_NAME, "Invalid profilename");//$NON-NLS-1$

         final AtmosphericPressure    atmosphericPressure  = (AtmosphericPressure)  Util.getXmlEnum(xmlProfile, ATTR_ATMOSPHERIC_PRESSURE, AtmosphericPressure.MILLIBAR);
         final Distance               distance             = (Distance)             Util.getXmlEnum(xmlProfile, ATTR_DISTANCE,             Distance.KILOMETER);
         final Elevation              elevation            = (Elevation)            Util.getXmlEnum(xmlProfile, ATTR_ELEVATION,            Elevation.METER);
         final Temperature            temperature          = (Temperature)          Util.getXmlEnum(xmlProfile, ATTR_TEMPERATURE,          Temperature.CELCIUS);
         final Weight                 weight               = (Weight)               Util.getXmlEnum(xmlProfile, ATTR_WEIGHT,               Weight.KILOGRAM);

// SET_FORMATTING_ON

         final MeasurementSystem systemProfile = new MeasurementSystem(profileName, atmosphericPressure, distance, elevation, temperature, weight);
         allLoadedProfiles.add(systemProfile);

         systemProfile.setSavedState_IsProfileActive(isProfileActive);
      }

      return allLoadedProfiles;
   }

   /**
    * Saves all measurement system profiles into a XML file.
    *
    * @param allClonedSystemProfiles
    * @param activeSystemProfileIndex
    *           Index which system profile should be currently active, this do NOT fire a modify
    *           event.
    */
   public static void saveState(final ArrayList<MeasurementSystem> allClonedSystemProfiles, final int activeSystemProfileIndex) {

      // apply new system profiles
      _allSystemProfiles.clear();
      for (final MeasurementSystem measurementSystem : allClonedSystemProfiles) {
         _allSystemProfiles.add(measurementSystem.clone());
      }

      _activeSystemProfileIndex = activeSystemProfileIndex;

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

         Util.setXmlEnum(xmlProfile, ATTR_ATMOSPHERIC_PRESSURE, system.getAtmosphericPressure());
         Util.setXmlEnum(xmlProfile, ATTR_DISTANCE, system.getDistance());
         Util.setXmlEnum(xmlProfile, ATTR_ELEVATION, system.getElevation());
         Util.setXmlEnum(xmlProfile, ATTR_TEMPERATURE, system.getTemperature());
         Util.setXmlEnum(xmlProfile, ATTR_WEIGHT, system.getWeight());
      }
   }

   /**
    * Updates the model when another system profile is selected. Fires a modify event that all pref
    * store listener are notified.
    *
    * @param activeSystemProfileIndex
    */
   public static void setActiveSystemProfileIndex(final int activeSystemProfileIndex) {

      _activeSystemProfileIndex = activeSystemProfileIndex;

      // setup measurement system data which are used in the app
      net.tourbook.ui.UI.updateUnits();

      // fire modify event
      _prefStore.setValue(ITourbookPreferences.MEASUREMENT_SYSTEM, Math.random());
   }
}
