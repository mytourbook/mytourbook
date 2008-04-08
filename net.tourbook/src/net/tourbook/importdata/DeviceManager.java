/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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

package net.tourbook.importdata;

import java.util.ArrayList;
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.plugin.TourbookPlugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;

public class DeviceManager {

	public static final String			DEVICE_IS_NOT_SELECTED	= Messages.DeviceManager_Selection_device_is_not_selected;

	private static List<TourbookDevice>	fDeviceList;
	private static List<ExternalDevice>	fExternalDeviceList;

	/**
	 * read devices from the extension registry
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static List<TourbookDevice> getDeviceList() {

		if (fDeviceList == null) {
			fDeviceList = readDeviceExtensions(TourbookPlugin.EXT_POINT_DEVICE_DATA_READER);
		}
		return fDeviceList;
	}

	/**
	 * read external devices from the extension registry
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static List<ExternalDevice> getExternalDeviceList() {

		if (fExternalDeviceList == null) {
			fExternalDeviceList = readDeviceExtensions(TourbookPlugin.EXT_POINT_EXTERNAL_DEVICE_DATA_READER);
		}
		return fExternalDeviceList;
	}

	@SuppressWarnings("unchecked")
	private static List readDeviceExtensions(String extensionPointName) {
		ArrayList ret = new ArrayList();

		IExtensionPoint extPoint = Platform.getExtensionRegistry().getExtensionPoint(TourbookPlugin.PLUGIN_ID,
				extensionPointName);

		if (extPoint != null) {

			for (IExtension extension : extPoint.getExtensions()) {

				for (IConfigurationElement configElement : extension.getConfigurationElements()) {

					if (configElement.getName().equalsIgnoreCase("device")) { //$NON-NLS-1$

						Object object;
						try {
							object = configElement.createExecutableExtension("class"); //$NON-NLS-1$
							if (object instanceof TourbookDevice) {

								TourbookDevice device = (TourbookDevice) object;

								device.deviceId = configElement.getAttribute("id"); //$NON-NLS-1$
								device.visibleName = configElement.getAttribute("name"); //$NON-NLS-1$
								device.fileExtension = configElement.getAttribute("fileextension"); //$NON-NLS-1$

								ret.add(device);
							}
							if (object instanceof ExternalDevice) {

								ExternalDevice device = (ExternalDevice) object;

								device.deviceId = configElement.getAttribute("id"); //$NON-NLS-1$
								device.visibleName = configElement.getAttribute("name"); //$NON-NLS-1$

								ret.add(device);
							}
						} catch (CoreException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
		return ret;
	}
}
