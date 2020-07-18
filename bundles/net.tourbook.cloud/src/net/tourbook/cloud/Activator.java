/*******************************************************************************
 * Copyright (C) 2020 Frédéric Bard
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
package net.tourbook.cloud;

import java.util.Optional;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ResourceLocator;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

   // The plug-in ID
   public static final String PLUGIN_ID = "net.tourbook.cloud"; //$NON-NLS-1$

   // The shared instance
   private static Activator plugin;

   /**
    * The constructor
    */
   public Activator() {
      plugin = this;
   }

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
    *           the path
    * @return the axisImage descriptor
    */
   public static ImageDescriptor getImageDescriptor(final String path) {
      final Optional<ImageDescriptor> imageDescriptor = ResourceLocator.imageDescriptorFromBundle(PLUGIN_ID, "icons/" + path); //$NON-NLS-1$

      return imageDescriptor.isPresent() ? imageDescriptor.get() : null;
   }

   @Override
   public void start(final BundleContext context) throws Exception {
      super.start(context);
      plugin = this;
   }

   @Override
   public void stop(final BundleContext context) throws Exception {
      plugin = null;
      super.stop(context);
   }
}
