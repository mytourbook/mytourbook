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
 * Author:	Wolfgang Schramm
 * Created: 16.06.2005
 *
 * 
 */

package net.tourbook.views.rawData;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Formatter;

import net.tourbook.Messages;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.rawdataimport.DeviceData;
import net.tourbook.rawdataimport.RawDataManager;

import org.eclipse.jface.action.Action;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

public class ActionSaveRawData extends Action {

	private RawDataView	rawDataView;

	/**
	 * Action for saving the raw data into a file
	 * 
	 * @param viewPart
	 */
	public ActionSaveRawData(RawDataView viewPart) {

		this.rawDataView = viewPart;

		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image_save_raw_data_to_file));
		setDisabledImageDescriptor(TourbookPlugin
				.getImageDescriptor(Messages.Image_save_raw_data_to_file_disabled));
		setToolTipText(Messages.RawData_Action_save_raw_data_to_file_tooltip);
		setEnabled(false);
	}

	public void run() {
		saveRawData();
	}

	/**
	 * 
	 */
	public void saveRawData() {

		Shell shell = rawDataView.getSite().getShell();

		RawDataManager rawDataManager = RawDataManager.getInstance();
		DeviceData deviceData = rawDataManager.getDeviceData();

		// create file save dialog
		FileDialog dialog = new FileDialog(shell, SWT.SAVE);

		// set default filename to the transfer date
		dialog.setFileName(new Formatter().format(
				Messages.Format_rawdata_file_yyyy_mm_dd
						+ rawDataManager.getDevice().fileExtension,
				deviceData.transferYear,
				deviceData.transferMonth,
				deviceData.transferDay).toString());

		// open file dialog
		String fileName = dialog.open();

		// check if user canceled the dialog
		if (fileName == null) {
			return;
		}

		// check if file already exist, ask for overwriting the file
		File fileOut = new File(fileName);
		if (fileOut.exists()) {
			MessageBox msgBox = new MessageBox(shell, SWT.ICON_WORKING | SWT.OK | SWT.CANCEL);
			msgBox.setMessage(NLS.bind(Messages.RawData_Label_confirm_overwrite, fileName));
			if (msgBox.open() != SWT.OK) {
				return;
			}
		}

		// get source file
		File fileIn = new File(rawDataManager.getImportFileName());

		// copy source file into destination file
		FileReader inReader = null;
		FileWriter outReader = null;
		try {
			inReader = new FileReader(fileIn);
			outReader = new FileWriter(fileOut);
			int c;

			while ((c = inReader.read()) != -1) {
				outReader.write(c);
			}
			inReader.close();
			outReader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			// close the files
			if (inReader != null) {
				try {
					inReader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (outReader != null) {
				try {
					outReader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		// reload the data from the saved copy
		if (RawDataManager.getInstance().importRawData(fileName)) {
			rawDataView.updateViewer();
			rawDataView.setActionSaveEnabled(false);
		}
	}
}
