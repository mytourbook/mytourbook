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
/*
 * Author: Wolfgang Schramm Created: 10.06.2005
 * 
 * 
 */

package net.tourbook.views.rawData;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.dataImport.RawDataManager;
import net.tourbook.device.DeviceManager;
import net.tourbook.device.TourbookDevice;
import net.tourbook.plugin.TourbookPlugin;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

public class ActionLoad extends Action {

	private RawDataView	rawDataView;

	public ActionLoad(RawDataView viewPart) {

		this.rawDataView = viewPart;

		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image_open_import_file));
		setToolTipText(Messages.RawData_Action_open_import_file_tooltip);
	}

	/**
	 * import tour data from a file
	 */
	public void run() {

		Shell shell = rawDataView.getSite().getShell();

		// setup open dialog
		FileDialog dialog = new FileDialog(shell, SWT.OPEN);

		ArrayList<TourbookDevice> deviceList = DeviceManager.getDeviceList();

		int deviceLength = deviceList.size()+1;
		// create file filter list
		String[] filterExtensions = new String[deviceLength];
		String[] filterNames = new String[deviceLength];
		int deviceIndex = 0;
		for (TourbookDevice device : deviceList) {
			filterExtensions[deviceIndex] = "*." + device.fileExtension; //$NON-NLS-1$
			filterNames[deviceIndex] = device.visibleName
					+ (" (*." + device.fileExtension + ")"); //$NON-NLS-1$ //$NON-NLS-2$

			deviceIndex++;
		}
		
		// add the option to select all files
		filterExtensions[deviceIndex] = "*.*"; //$NON-NLS-1$
		filterNames[deviceIndex] = "*.*"; //$NON-NLS-1$

		// open file dialog
		dialog.setFilterExtensions(filterExtensions);
		dialog.setFilterNames(filterNames);
		String fileName = dialog.open();

		// check if user canceled the dialog
		if (fileName == null) {
			return;
		}

		RawDataManager rawDataManager = RawDataManager.getInstance();

		if (rawDataManager.importRawData(fileName)) {
			rawDataView.updateViewer();
			rawDataView.setActionSaveEnabled(rawDataManager.isDeviceImport());
		}
	}

}
