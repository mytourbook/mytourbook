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
package net.tourbook.common;

import java.util.Optional;

import net.tourbook.common.color.ThemeUtil;
import net.tourbook.common.map.MapUI;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ResourceLocator;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class CommonActivator extends AbstractUIPlugin {

   // The plug-in ID
   public static final String PLUGIN_ID = "net.tourbook.common"; //$NON-NLS-1$

   // The shared instance
   private static CommonActivator plugin;

   /**
    * The constructor
    */
   public CommonActivator() {}

   /**
    * Returns the shared instance
    *
    * @return the shared instance
    */
   public static CommonActivator getDefault() {
      return plugin;
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

   public static IPreferenceStore getPrefStore() {
      return getDefault().getPreferenceStore();
   }

   /**
    * @param sectionName
    * @return Returns the dialog setting section for the sectionName, a section is always returned
    *         even when it's empty
    */
   public static IDialogSettings getState(final String sectionName) {

      final IDialogSettings dialogSettings = getDefault().getDialogSettings();
      IDialogSettings section = dialogSettings.getSection(sectionName);

      if (section == null) {
         section = dialogSettings.addNewSection(sectionName);
      }

      return section;
   }

   /**
    * @param imageName
    * @return Returns the themed image descriptor from {@link CommonActivator} plugin images
    */
   public static ImageDescriptor getThemedImageDescriptor(final String imageName) {

      return CommonActivator.getImageDescriptor(ThemeUtil.getThemedImageName(imageName));
   }

   @Override
   public void start(final BundleContext context) throws Exception {

      super.start(context);
      plugin = this;

      MapUI.init();
   }

   @Override
   public void stop(final BundleContext context) throws Exception {

      plugin = null;
      super.stop(context);
   }

}
