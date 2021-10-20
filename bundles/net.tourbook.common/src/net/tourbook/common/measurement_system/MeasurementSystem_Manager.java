/*******************************************************************************
 * Copyright (C) 2020, 2021 Wolfgang Schramm and Contributors
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
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
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

   private static final String                 STATE_FILE                = "measurement-system.xml";      //$NON-NLS-1$

   private static final String                 TAG_ROOT                  = "measurementSystem";           //$NON-NLS-1$
   private static final String                 TAG_PROFILE               = "profile";                     //$NON-NLS-1$

   private static final String                 ATTR_NAME                 = "name";                        //$NON-NLS-1$
   private static final String                 ATTR_IS_ACTIVE            = "isActive";                    //$NON-NLS-1$

   private static final String                 ATTR_DISTANCE             = "distance";                    //$NON-NLS-1$
   private static final String                 ATTR_ELEVATION            = "elevation";                   //$NON-NLS-1$
   private static final String                 ATTR_HEIGHT               = "height";                      //$NON-NLS-1$
   private static final String                 ATTR_LENGTH               = "length";                      //$NON-NLS-1$
   private static final String                 ATTR_LENGTH_SMALL         = "lengthSmall";                 //$NON-NLS-1$
   private static final String                 ATTR_PACE                 = "pace";                        //$NON-NLS-1$
   private static final String                 ATTR_PRESSURE_ATMOSPHERE  = "pressureAtmosphere";          //$NON-NLS-1$
   private static final String                 ATTR_TEMPERATURE          = "temperature";                 //$NON-NLS-1$
   private static final String                 ATTR_WEIGHT               = "weight";                      //$NON-NLS-1$

   private static final String                 ATTR_VERSION              = "version";                     //$NON-NLS-1$

   private static ArrayList<MeasurementSystem> ALL_DEFAULT_PROFILES      = new ArrayList<>();

   private static ArrayList<MeasurementSystem> _allSystemProfiles        = new ArrayList<>();
   private static int                          _activeSystemProfileIndex = 0;

   private static boolean                      _isDefaultProfileSelected;

   private static final IPreferenceStore       _prefStore_Common         = CommonActivator.getPrefStore();

   /**
    * Define default measurement systems
    */
   static {

      ALL_DEFAULT_PROFILES.add(new MeasurementSystem(Messages.Measurement_System_Profile_Metric,

            Unit_Distance.KILOMETER,
            Unit_Length.METER,
            Unit_Length_Small.MILLIMETER,
            Unit_Elevation.METER,
            Unit_Height_Body.METER,
            Unit_Pace.MINUTES_PER_KILOMETER,
            Unit_Pressure_Atmosphere.MILLIBAR,
            Unit_Temperature.CELSIUS,
            Unit_Weight.KILOGRAM));

      ALL_DEFAULT_PROFILES.add(new MeasurementSystem(Messages.Measurement_System_Profile_Imperial,

            Unit_Distance.MILE,
            Unit_Length.YARD,
            Unit_Length_Small.INCH,
            Unit_Elevation.FOOT,
            Unit_Height_Body.INCH,
            Unit_Pace.MINUTES_PER_MILE,
            Unit_Pressure_Atmosphere.INCH_OF_MERCURY,
            Unit_Temperature.FAHRENHEIT,
            Unit_Weight.POUND));

      ALL_DEFAULT_PROFILES.add(new MeasurementSystem(Messages.Measurement_System_Profile_Nautic,

            Unit_Distance.NAUTIC_MILE,
            Unit_Length.METER,
            Unit_Length_Small.MILLIMETER,
            Unit_Elevation.METER,
            Unit_Height_Body.METER,
            Unit_Pace.MINUTES_PER_KILOMETER,
            Unit_Pressure_Atmosphere.MILLIBAR,
            Unit_Temperature.CELSIUS,
            Unit_Weight.KILOGRAM));

      ALL_DEFAULT_PROFILES.add(new MeasurementSystem(Messages.Measurement_System_Profile_Other1,

            Unit_Distance.KILOMETER,
            Unit_Length.METER,
            Unit_Length_Small.MILLIMETER,
            Unit_Elevation.METER,
            Unit_Height_Body.METER,
            Unit_Pace.MINUTES_PER_KILOMETER,
            Unit_Pressure_Atmosphere.MILLIBAR,
            Unit_Temperature.CELSIUS,
            Unit_Weight.KILOGRAM));

      ALL_DEFAULT_PROFILES.add(new MeasurementSystem(Messages.Measurement_System_Profile_Other2,

            Unit_Distance.KILOMETER,
            Unit_Length.METER,
            Unit_Length_Small.MILLIMETER,
            Unit_Elevation.METER,
            Unit_Height_Body.METER,
            Unit_Pace.MINUTES_PER_KILOMETER,
            Unit_Pressure_Atmosphere.MILLIBAR,
            Unit_Temperature.CELSIUS,
            Unit_Weight.KILOGRAM));

      ALL_DEFAULT_PROFILES.add(new MeasurementSystem(Messages.Measurement_System_Profile_Other3,

            Unit_Distance.KILOMETER,
            Unit_Length.METER,
            Unit_Length_Small.MILLIMETER,
            Unit_Elevation.METER,
            Unit_Height_Body.METER,
            Unit_Pace.MINUTES_PER_KILOMETER,
            Unit_Pressure_Atmosphere.MILLIBAR,
            Unit_Temperature.CELSIUS,
            Unit_Weight.KILOGRAM));

      // overwrite default values with saved values
      readProfiles();
   }

// SET_FORMATTING_OFF

   private static final System_Distance[]                        _allSystem_Distances = {

         new System_Distance(Unit_Distance.KILOMETER,          Messages.Pref_System_Option_Distance_Kilometer),
         new System_Distance(Unit_Distance.MILE,               Messages.Pref_System_Option_Distance_Mile),
         new System_Distance(Unit_Distance.NAUTIC_MILE,        Messages.Pref_System_Option_Distance_NauticMile),
   };

   private static final System_Elevation[]                       _allSystem_Elevations = {

         new System_Elevation(Unit_Elevation.METER,            Messages.Pref_System_Option_Elevation_Meter),
         new System_Elevation(Unit_Elevation.FOOT,             Messages.Pref_System_Option_Elevation_Foot),
   };

   private static final System_Height[]                          _allSystem_Heights = {

         new System_Height(Unit_Height_Body.METER,             Messages.Pref_System_Option_Height_Meter),
         new System_Height(Unit_Height_Body.INCH,              Messages.Pref_System_Option_Height_Inch),
   };

   private static final System_Length[]                          _allSystem_Lengths = {

         new System_Length(Unit_Length.METER,                  Messages.Pref_System_Option_Length_Meter),
         new System_Length(Unit_Length.YARD,                   Messages.Pref_System_Option_Length_Yard),
   };

   private static final System_LengthSmall[]                     _allSystem_Length_Small = {

         new System_LengthSmall(Unit_Length_Small.MILLIMETER,  Messages.Pref_System_Option_SmallLength_Millimeter),
         new System_LengthSmall(Unit_Length_Small.INCH,        Messages.Pref_System_Option_SmallLength_Inch),
   };

   private static final System_Pace []                           _allSystem_Pace = {

         new System_Pace(Unit_Pace.MINUTES_PER_KILOMETER,      Messages.Pref_System_Option_Pace_MinutesPerKilometer),
         new System_Pace(Unit_Pace.MINUTES_PER_MILE,           Messages.Pref_System_Option_Pace_MinutesPerMile),
   };

   private static final System_Pressure_Atmosphere[]             _allSystem_Pressure_Atmosphere = {

         new System_Pressure_Atmosphere(Unit_Pressure_Atmosphere.MILLIBAR,          Messages.Pref_System_Option_Pressure_Atmosphere_Millibar),
         new System_Pressure_Atmosphere(Unit_Pressure_Atmosphere.INCH_OF_MERCURY,   Messages.Pref_System_Option_Pressure_Atmosphere_InchOfMercury),
   };

   private static final System_Temperature[]                     _allSystem_Temperatures = {

         new System_Temperature(Unit_Temperature.CELSIUS,      Messages.Pref_System_Option_Temperature_Celsius),
         new System_Temperature(Unit_Temperature.FAHRENHEIT,   Messages.Pref_System_Option_Temperature_Fahrenheit),
   };

   private static final System_Weight[]                          _allSystem_Weights = {

         new System_Weight(Unit_Weight.KILOGRAM,               Messages.Pref_System_Option_BodyWeight_Kilogram),
         new System_Weight(Unit_Weight.POUND,                  Messages.Pref_System_Option_BodyWeight_Pound),
   };

// SET_FORMATTING_ON

   private MeasurementSystem_Manager() {}

   /**
    * @return Returns the currently active measurement system.
    */
   public static MeasurementSystem getActiveMeasurementSystem() {
      return _allSystemProfiles.get(_activeSystemProfileIndex);
   }

   /**
    * @return the _activeSystemProfileIndex
    */
   public static int getActiveSystem_ProfileIndex() {
      return _activeSystemProfileIndex;
   }

   public static System_Distance getActiveSystemOption_Distance() {
      return _allSystem_Distances[getSystemIndex_Distance(getActiveMeasurementSystem())];
   }

   public static System_Elevation getActiveSystemOption_Elevation() {
      return _allSystem_Elevations[getSystemIndex_Elevation(getActiveMeasurementSystem())];
   }

   public static System_Height getActiveSystemOption_Height() {
      return _allSystem_Heights[getSystemIndex_Height(getActiveMeasurementSystem())];
   }

   public static System_Length getActiveSystemOption_Length() {
      return _allSystem_Lengths[getSystemIndex_Length(getActiveMeasurementSystem())];
   }

   public static System_LengthSmall getActiveSystemOption_Length_Small() {
      return _allSystem_Length_Small[getSystemIndex_Length_Small(getActiveMeasurementSystem())];
   }

   public static System_Pace getActiveSystemOption_Pace() {
      return _allSystem_Pace[getSystemIndex_Pace(getActiveMeasurementSystem())];
   }

   public static System_Pressure_Atmosphere getActiveSystemOption_Pressure_Atmospheric() {
      return _allSystem_Pressure_Atmosphere[getSystemIndex_Pressure_Atmosphere(getActiveMeasurementSystem())];
   }

   public static System_Temperature getActiveSystemOption_Temperature() {
      return _allSystem_Temperatures[getSystemIndex_Temperature(getActiveMeasurementSystem())];
   }

   public static System_Weight getActiveSystemOption_Weight() {
      return _allSystem_Weights[getSystemIndex_Weight(getActiveMeasurementSystem())];
   }

   public static System_Distance[] getAllSystem_Distances() {
      return _allSystem_Distances;
   }

   public static System_Elevation[] getAllSystem_Elevations() {
      return _allSystem_Elevations;
   }

   public static System_Height[] getAllSystem_Heights() {
      return _allSystem_Heights;
   }

   public static System_Length[] getAllSystem_Length() {
      return _allSystem_Lengths;
   }

   public static System_LengthSmall[] getAllSystem_Length_Small() {
      return _allSystem_Length_Small;
   }

   public static System_Pace[] getAllSystem_Pace() {
      return _allSystem_Pace;
   }

   public static System_Pressure_Atmosphere[] getAllSystem_Pressures_Atmospheric() {
      return _allSystem_Pressure_Atmosphere;
   }

   public static System_Temperature[] getAllSystem_Temperatures() {
      return _allSystem_Temperatures;
   }

   public static System_Weight[] getAllSystem_Weights() {
      return _allSystem_Weights;
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

   public static int getSystemIndex_Distance(final MeasurementSystem selectedSystemProfile) {

      final Unit_Distance profileUnit = selectedSystemProfile.getDistance();

      for (int systemIndex = 0; systemIndex < _allSystem_Distances.length; systemIndex++) {

         if (profileUnit.equals(_allSystem_Distances[systemIndex].getDistance())) {
            return systemIndex;
         }
      }

      // return default
      return 0;
   }

   public static int getSystemIndex_Elevation(final MeasurementSystem selectedSystemProfile) {

      final Unit_Elevation profileUnit = selectedSystemProfile.getElevation();

      for (int systemIndex = 0; systemIndex < _allSystem_Elevations.length; systemIndex++) {

         if (profileUnit.equals(_allSystem_Elevations[systemIndex].getElevation())) {
            return systemIndex;
         }
      }

      // return default
      return 0;
   }

   public static int getSystemIndex_Height(final MeasurementSystem selectedSystemProfile) {

      final Unit_Height_Body profileUnit = selectedSystemProfile.getHeight();

      for (int systemIndex = 0; systemIndex < _allSystem_Heights.length; systemIndex++) {

         if (profileUnit.equals(_allSystem_Heights[systemIndex].getHeight())) {
            return systemIndex;
         }
      }

      // return default
      return 0;
   }

   public static int getSystemIndex_Length(final MeasurementSystem selectedSystemProfile) {

      final Unit_Length profileUnit = selectedSystemProfile.getLength();

      for (int systemIndex = 0; systemIndex < _allSystem_Lengths.length; systemIndex++) {

         if (profileUnit.equals(_allSystem_Lengths[systemIndex].getLength())) {
            return systemIndex;
         }
      }

      // return default
      return 0;
   }

   public static int getSystemIndex_Length_Small(final MeasurementSystem selectedSystemProfile) {

      final Unit_Length_Small profileUnit = selectedSystemProfile.getLengthSmall();

      for (int systemIndex = 0; systemIndex < _allSystem_Length_Small.length; systemIndex++) {

         if (profileUnit.equals(_allSystem_Length_Small[systemIndex].getLength_Small())) {
            return systemIndex;
         }
      }

      // return default
      return 0;
   }

   public static int getSystemIndex_Pace(final MeasurementSystem selectedSystemProfile) {

      final Unit_Pace profileUnit = selectedSystemProfile.getPace();

      for (int systemIndex = 0; systemIndex < _allSystem_Pace.length; systemIndex++) {

         if (profileUnit.equals(_allSystem_Pace[systemIndex].getPace())) {
            return systemIndex;
         }
      }

      // return default
      return 0;
   }

   public static int getSystemIndex_Pressure_Atmosphere(final MeasurementSystem selectedSystemProfile) {

      final Unit_Pressure_Atmosphere profileUnit = selectedSystemProfile.getPressure_Atmosphere();

      for (int systemIndex = 0; systemIndex < _allSystem_Pressure_Atmosphere.length; systemIndex++) {

         if (profileUnit.equals(_allSystem_Pressure_Atmosphere[systemIndex].getPressure())) {
            return systemIndex;
         }
      }

      // return default
      return 0;
   }

   public static int getSystemIndex_Temperature(final MeasurementSystem selectedSystemProfile) {

      final Unit_Temperature profileUnit = selectedSystemProfile.getTemperature();

      for (int systemIndex = 0; systemIndex < _allSystem_Temperatures.length; systemIndex++) {

         if (profileUnit.equals(_allSystem_Temperatures[systemIndex].getTemperature())) {
            return systemIndex;
         }
      }

      // return default
      return 0;
   }

   public static int getSystemIndex_Weight(final MeasurementSystem selectedSystemProfile) {

      final Unit_Weight profileUnit = selectedSystemProfile.getWeight();

      for (int systemIndex = 0; systemIndex < _allSystem_Weights.length; systemIndex++) {

         if (profileUnit.equals(_allSystem_Weights[systemIndex].getWeight())) {
            return systemIndex;
         }
      }

      // return default
      return 0;
   }

   private static void readProfiles() {

      // get loaded system profiles
      final ArrayList<MeasurementSystem> allLoadedProfiles = readProfiles_10_FromXmlFile();

      if (allLoadedProfiles == null || allLoadedProfiles.isEmpty()) {

         // profiles are not yet available -> create defaults

         for (final MeasurementSystem profile : ALL_DEFAULT_PROFILES) {
            _allSystemProfiles.add(profile.clone());
         }

         _activeSystemProfileIndex = 0;

         // set a flag that the user is selecting a system afterwards
         _isDefaultProfileSelected = true;

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
         final String profileName = Util.getXmlString(xmlProfile, ATTR_NAME, "Invalid profilename");//$NON-NLS-1$

// SET_FORMATTING_OFF

         final Unit_Distance              distance             = (Unit_Distance)             Util.getXmlEnum(xmlProfile, ATTR_DISTANCE,               Unit_Distance.KILOMETER);
         final Unit_Elevation             elevation            = (Unit_Elevation)            Util.getXmlEnum(xmlProfile, ATTR_ELEVATION,              Unit_Elevation.METER);
         final Unit_Height_Body           height               = (Unit_Height_Body)          Util.getXmlEnum(xmlProfile, ATTR_HEIGHT,                 Unit_Height_Body.METER);
         final Unit_Length                length               = (Unit_Length)               Util.getXmlEnum(xmlProfile, ATTR_LENGTH,                 Unit_Length.METER);
         final Unit_Length_Small          length_Small         = (Unit_Length_Small)         Util.getXmlEnum(xmlProfile, ATTR_LENGTH_SMALL,           Unit_Length_Small.MILLIMETER);
         final Unit_Pace                  pace                 = (Unit_Pace)                 Util.getXmlEnum(xmlProfile, ATTR_PACE,                   Unit_Pace.MINUTES_PER_KILOMETER);
         final Unit_Pressure_Atmosphere   pressure_Atmospheric = (Unit_Pressure_Atmosphere)  Util.getXmlEnum(xmlProfile, ATTR_PRESSURE_ATMOSPHERE,    Unit_Pressure_Atmosphere.MILLIBAR);
         final Unit_Temperature           temperature          = (Unit_Temperature)          Util.getXmlEnum(xmlProfile, ATTR_TEMPERATURE,            Unit_Temperature.CELSIUS);
         final Unit_Weight                weight               = (Unit_Weight)               Util.getXmlEnum(xmlProfile, ATTR_WEIGHT,                 Unit_Weight.KILOGRAM);

// SET_FORMATTING_ON

         final MeasurementSystem systemProfile = new MeasurementSystem(profileName,
               distance,
               length,
               length_Small,
               elevation,
               height,
               pace,
               pressure_Atmospheric,
               temperature,
               weight);

         allLoadedProfiles.add(systemProfile);

         systemProfile.setSavedState_IsProfileActive(isProfileActive);
      }

      return allLoadedProfiles;
   }

   /**
    * Save current measurement system.
    */
   public static void saveState() {

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

      saveState();
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
         Util.setXmlEnum(xmlProfile, ATTR_HEIGHT, system.getHeight());
         Util.setXmlEnum(xmlProfile, ATTR_LENGTH, system.getLength());
         Util.setXmlEnum(xmlProfile, ATTR_LENGTH_SMALL, system.getLengthSmall());
         Util.setXmlEnum(xmlProfile, ATTR_PACE, system.getPace());
         Util.setXmlEnum(xmlProfile, ATTR_PRESSURE_ATMOSPHERE, system.getPressure_Atmosphere());
         Util.setXmlEnum(xmlProfile, ATTR_TEMPERATURE, system.getTemperature());
         Util.setXmlEnum(xmlProfile, ATTR_WEIGHT, system.getWeight());
      }
   }

   /**
    * Select measurement system when it is not yet selected
    */
   public static void selectMeasurementSystem() {

      if (_isDefaultProfileSelected) {

         // the user have not yet selected a measurement system after version 20.11
         new DialogSelectMeasurementSystem(Display.getDefault().getActiveShell()).open();
      }
   }

   /**
    * Updates the model when another system profile is selected. Fires a modify event that all pref
    * store listener are notified.
    *
    * @param activeSystemProfileIndex
    * @param isFireEvent
    */
   public static void setActiveSystemProfileIndex(final int activeSystemProfileIndex, final boolean isFireEvent) {

      _activeSystemProfileIndex = activeSystemProfileIndex;

      // setup measurement system data which are used in the app
      UI.updateUnits();

      if (isFireEvent) {

         // fire modify event
         _prefStore_Common.setValue(ICommonPreferences.MEASUREMENT_SYSTEM, Math.random());
      }
   }
}
