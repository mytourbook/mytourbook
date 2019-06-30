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
package net.tourbook.common.map;

import net.tourbook.common.CommonActivator;
import net.tourbook.common.Messages;
import net.tourbook.common.UI;

import org.eclipse.jface.resource.ImageRegistry;

/**
 * Map common resources
 */
public class MapUI {

   public static final String MAP_PROVIDER_CUSTOM      = "map-provider-custom";      //$NON-NLS-1$
   public static final String MAP_PROVIDER_INTERNAL    = "map-provider-internal";    //$NON-NLS-1$
   public static final String MAP_PROVIDER_PROFILE     = "map-provider-profile";     //$NON-NLS-1$
   public static final String MAP_PROVIDER_TRANSPARENT = "map-provider-transparent"; //$NON-NLS-1$

   static {

      final ImageRegistry imageRegistry = UI.IMAGE_REGISTRY;

// SET_FORMATTING_OFF

      imageRegistry.put(MAP_PROVIDER_CUSTOM,       CommonActivator.getImageDescriptor(Messages.Image__MapProvider_Custom));
      imageRegistry.put(MAP_PROVIDER_INTERNAL,     CommonActivator.getImageDescriptor(Messages.Image__MapProvider_Internal));
      imageRegistry.put(MAP_PROVIDER_PROFILE,      CommonActivator.getImageDescriptor(Messages.Image__MapProvider_Profile));
      imageRegistry.put(MAP_PROVIDER_TRANSPARENT,  CommonActivator.getImageDescriptor(Messages.Image__MapProvider_Transparent));

// SET_FORMATTING_ON

   }

   /**
    * When this method is called, this class is loaded and initialized in the static initializer,
    * which is setting the images in the image registry
    */
   public static void init() {}
}
