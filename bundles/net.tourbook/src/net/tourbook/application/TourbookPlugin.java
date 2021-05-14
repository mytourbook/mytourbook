/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.application;

import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

import net.tourbook.common.color.ThemeUtil;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.TourPerson;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.TourTypeFilter;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ResourceLocator;
import org.eclipse.osgi.internal.framework.EquinoxBundle;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

/**
 * The main plugin class to be used in the desktop.
 */
@SuppressWarnings("restriction")
public class TourbookPlugin extends AbstractUIPlugin {

   public static final String PLUGIN_ID                             = "net.tourbook";             //$NON-NLS-1$

   public static final String EXT_POINT_STATISTIC_YEAR              = "statisticYear";            //$NON-NLS-1$
   public static final String EXT_POINT_EXPORT_TOUR                 = "exportTour";               //$NON-NLS-1$
   public static final String EXT_POINT_PRINT_TOUR                  = "printTour";                //$NON-NLS-1$
   public static final String EXT_POINT_DEVICE_DATA_READER          = "deviceDataReader";         //$NON-NLS-1$
   public static final String EXT_POINT_EXTERNAL_DEVICE_DATA_READER = "externalDeviceDataReader"; //$NON-NLS-1$

   // The shared instance.
   private static TourbookPlugin _instance;

   private static ResourceBundle _resourceBundle;

   /**
    * Active person which is selected in the app toolbar, when <code>null</code> then 'All People'
    * are selected.
    */
   private static TourPerson     _activePerson;

   private static TourTypeFilter _activeTourTypeFilter;
   private static boolean        _isPhotoFilterActive;

   private static BundleContext  _bundleContext;

   private Version               _version;

   /**
    * The constructor.
    */
   public TourbookPlugin() {}

   /**
    * @return Returns the active selected person or <code>null</code> when all people are selected
    */
   public static TourPerson getActivePerson() {
      return _activePerson;
   }

   /**
    * @return Returns <code>true</code> when the photo filter is active, this means, only tours are
    *         displayed which contains photos.
    */
   public static boolean getActivePhotoFilter() {
      return _isPhotoFilterActive;
   }

   /**
    * @return Returns {@link TourTypeFilter} which is currently selected in the UI
    */
   public static TourTypeFilter getActiveTourTypeFilter() {
      return _activeTourTypeFilter;
   }

   /**
    * @return Returns the shell of the app
    */
   public static Shell getAppShell() {

      return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
   }

   public static BundleContext getBundleContext() {
      return _bundleContext;
   }

   /**
    * Returns the shared instance.
    */
   public static TourbookPlugin getDefault() {
      return _instance;
   }

   public static Image getImage(final String imagePath) {
      return getImageDescriptor(imagePath).createImage();
   }

   /**
    * Returns an image descriptor for images in the plug-in path.
    *
    * @param path
    *           the path
    * @return the axisImage descriptor
    */
   public static ImageDescriptor getImageDescriptor(final String path) {

      final Optional<ImageDescriptor> imageDescriptor = ResourceLocator.imageDescriptorFromBundle(PLUGIN_ID, "icons/" + path); //$NON-NLS-1$

      return imageDescriptor.isPresent() ? imageDescriptor.get() : null;
   }

   /**
    * This is a shortcut for {@link TourbookPlugin#getDefault()#getPreferenceStore()}
    *
    * @return
    */
   public static IPreferenceStore getPrefStore() {
      return TourbookPlugin.getDefault().getPreferenceStore();
   }

   /**
    * Returns the plugin's resource bundle,
    */
   private static ResourceBundle getResourceBundle() {

      try {
         if (_resourceBundle == null) {
            _resourceBundle = ResourceBundle.getBundle("net.tourbook.data.TourbookPluginResources"); //$NON-NLS-1$
         }
      } catch (final MissingResourceException x) {
         _resourceBundle = null;
      }

      return _resourceBundle;
   }

   /**
    * Returns the string from the plugin's resource bundle, or 'key' if not found.
    */
   public static String getResourceString(final String key) {

      final ResourceBundle bundle = getResourceBundle();

      try {
         return (bundle != null) ? bundle.getString(key) : key;
      } catch (final MissingResourceException e) {
         return key;
      }
   }

   /**
    * This is a shortcut for {@link getDefault().getDialogSettingsSection(String)}
    *
    * @param stateSectionName
    * @return
    */
   public static IDialogSettings getState(final String stateSectionName) {

      return getDefault().getDialogSettingsSection(stateSectionName);
   }

   /**
    * @param imageName
    * @return Returns the themed image descriptor from {@link TourbookPlugin} plugin images
    */
   public static ImageDescriptor getThemedImageDescriptor(final String imageName) {

      return getImageDescriptor(ThemeUtil.getThemedImageName(imageName));
   }

   public static void setActivePerson(final TourPerson currentPerson) {

      _activePerson = currentPerson;
   }

   public static void setActivePhotoFilter(final boolean isPhotoFilterActive) {

      _isPhotoFilterActive = isPhotoFilterActive;
   }

   public static void setActiveTourTypeFilter(final TourTypeFilter tourTypeFilter) {

      _activeTourTypeFilter = tourTypeFilter;

      TourDatabase.updateActiveTourTypeList(tourTypeFilter);
   }

   /**
    * @param sectionName
    * @return Returns the dialog setting section for the sectionName, a section is always returned
    *         even when it's empty
    */
   public IDialogSettings getDialogSettingsSection(final String sectionName) {

      final IDialogSettings dialogSettings = getDialogSettings();
      IDialogSettings section = dialogSettings.getSection(sectionName);

      if (section == null) {
         section = dialogSettings.addNewSection(sectionName);
      }

      return section;
   }

   public Version getVersion() {
      return _version;
   }

   public void log(final String message, final Throwable exception) {

      getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, 0, message, exception));
   }

   @Override
   public void start(final BundleContext context) throws Exception {

      super.start(context);

      _instance = this;
      _bundleContext = context;

      final Bundle bundle = context.getBundle();
      if (bundle instanceof EquinoxBundle) {
         final EquinoxBundle abstractBundle = (EquinoxBundle) bundle;
         _version = abstractBundle.getVersion();
      }

      // log version that this info is easier accessable
      final Version version = TourbookPlugin.getDefault().getVersion();

      /**
       * StatusUtil will force to load net.tourbook.common plugin which forces to load
       * net.tourbook.common.UI which is initializing the measurement system.
       */
      StatusUtil.logInfo("[AppVersion] " + version.toString());//$NON-NLS-1$
   }

   @Override
   public void stop(final BundleContext context) throws Exception {

      _instance = null;
      _bundleContext = null;

      super.stop(context);
   }

}
