/*******************************************************************************
 * Copyright (C) 2006, 2007  Wolfgang Schramm
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
package net.tourbook.device;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.plugin.TourbookPlugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;

public class DeviceManager {

	public static final String					DEVICE_IS_NOT_SELECTED	= Messages.DeviceManager_Selection_device_is_not_selected;

	private static ArrayList<TourbookDevice>	fDeviceList;

	/**
	 * read devicees from the extension registry
	 * 
	 * @return
	 */
	public static ArrayList<TourbookDevice> getDeviceList() {

		if (fDeviceList != null) {
			// read the list only once
			return fDeviceList;
		}

		fDeviceList = new ArrayList<TourbookDevice>();

		IExtensionPoint extPoint = Platform.getExtensionRegistry().getExtensionPoint(
				TourbookPlugin.PLUGIN_ID,
				TourbookPlugin.EXT_POINT_DEVICE_DATA_READER);

		if (extPoint != null) {

			for (IExtension extension : extPoint.getExtensions()) {

				for (IConfigurationElement configElement : extension
						.getConfigurationElements()) {

					if (configElement.getName().equalsIgnoreCase("device")) { //$NON-NLS-1$

						Object object;
						try {
							object = configElement.createExecutableExtension("class"); //$NON-NLS-1$
							if (object instanceof TourbookDevice) {

								TourbookDevice device = (TourbookDevice) object;

								device.deviceId = configElement.getAttribute("id"); //$NON-NLS-1$
								device.visibleName = configElement.getAttribute("name"); //$NON-NLS-1$
								device.fileExtension = configElement
										.getAttribute("fileextension"); //$NON-NLS-1$

								fDeviceList.add(device);
							}
						} catch (CoreException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
		return fDeviceList;
	}
}
