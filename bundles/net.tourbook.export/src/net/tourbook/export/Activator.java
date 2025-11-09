/*******************************************************************************
 * Copyright (C) 2005, 2025 Wolfgang Schramm and Contributors
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
package net.tourbook.export;

import java.util.Optional;

import net.tourbook.common.UI;
import net.tourbook.common.color.ThemeUtil;
import net.tourbook.common.util.StatusUtil;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ResourceLocator;
import org.eclipse.osgi.internal.framework.EquinoxBundle;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

/**
 * The activator class controls the plug-in life cycle
 */
@SuppressWarnings("restriction")
public class Activator extends AbstractUIPlugin {

   // The plug-in ID
   public static final String PLUGIN_ID = "net.tourbook.export"; //$NON-NLS-1$

   // The shared instance
   private static Activator plugin;

   private Version          version;

   /**
    * The constructor
    */
   public Activator() {}

   /**
    * Returns the shared instance
    *
    * @return the shared instance
    */
   public static Activator getDefault() {
      return plugin;
   }

   /**
    * Returns an image descriptor for images in the plug-in path.
    *
    * @param path
    *           the image path
    *
    * @return the image descriptor
    */
   public static ImageDescriptor getImageDescriptor(final String path) {

      final Optional<ImageDescriptor> imageDescriptor = ResourceLocator.imageDescriptorFromBundle(PLUGIN_ID, "icons/" + path); //$NON-NLS-1$

      return imageDescriptor.isPresent() ? imageDescriptor.get() : null;
   }

   private static ImageDescriptor getImageDescriptor_Dark_Win(final String imageName) {

      if (UI.IS_DARK_THEME && UI.IS_WIN) {

         /**
          * Since windows 11, a hovered or selected action are displaying a very bright background
          * which makes it very difficult to see the dark images which content is mostly very
          * bright.
          * <p>
          * Because of this reason, the HDR images were created and are displayed on windows 11 in
          * the dark theme and when available.
          */

         if (UI.IS_USE_HDR_IMAGES) {

            final ImageDescriptor hdrImageDescriptor = getImageDescriptor(ThemeUtil.getThemedImageName_HDR(imageName));

            if (hdrImageDescriptor != null) {

               return hdrImageDescriptor;
            }
         }

         // display bright theme image

         return getImageDescriptor(imageName);
      }

      return null;
   }

   /**
    * @param imageName
    *
    * @return Returns the themed image descriptor from this plugin images
    */
   public static ImageDescriptor getThemedImageDescriptor(final String imageName) {

      final ImageDescriptor winDarkImageDescriptor = getImageDescriptor_Dark_Win(imageName);

      if (winDarkImageDescriptor != null) {
         return winDarkImageDescriptor;
      }

      final ImageDescriptor themedImageDescriptor = getImageDescriptor(ThemeUtil.getThemedImageName(imageName));

      if (themedImageDescriptor == null) {

         StatusUtil.logError("Cannot get themed image descriptor for '%s'".formatted(imageName)); //$NON-NLS-1$

      } else {

         return themedImageDescriptor;
      }

      return getImageDescriptor(imageName);
   }

   public Version getVersion() {
      return version;
   }

   @Override
   public void start(final BundleContext context) throws Exception {

      super.start(context);
      plugin = this;

      final Bundle bundle = context.getBundle();
      if (bundle instanceof EquinoxBundle) {
         final EquinoxBundle abstractBundle = (EquinoxBundle) bundle;
         version = abstractBundle.getVersion();
      }
   }

   /*
    * (non-Javadoc)
    * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
    */
   @Override
   public void stop(final BundleContext context) throws Exception {
      plugin = null;
      super.stop(context);
   }
}
