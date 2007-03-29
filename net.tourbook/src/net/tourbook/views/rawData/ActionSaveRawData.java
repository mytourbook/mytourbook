/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Formatter;

import net.tourbook.Messages;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.plugin.TourbookPlugin;

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
		String fileNameFormat = Messages.Format_rawdata_file_yyyy_mm_dd
				+ rawDataManager.getDevice().fileExtension;

		dialog.setFileName(new Formatter().format(
				fileNameFormat,
				deviceData.transferYear,
				deviceData.transferMonth,
				deviceData.transferDay).toString());

		// open file dialog
		String fileNameOut = dialog.open();

		// check if user canceled the dialog
		if (fileNameOut == null) {
			return;
		}

		// check if file already exist, ask for overwriting the file
		File fileOut = new File(fileNameOut);
		if (fileOut.exists()) {
			MessageBox msgBox = new MessageBox(shell, SWT.ICON_WORKING | SWT.OK | SWT.CANCEL);
			msgBox.setMessage(NLS.bind(Messages.RawData_Label_confirm_overwrite, fileNameOut));
			if (msgBox.open() != SWT.OK) {
				return;
			}
		}

		if (copyFile(rawDataManager.getImportFileName(), fileNameOut)) {

			// reload the data from the saved copy
			if (rawDataManager.importRawData(fileNameOut)) {
				rawDataView.updateViewer();
				rawDataView.setActionSaveEnabled(false);
			}
		}
	}

	private boolean copyFile(String src, String fileNameOut) {

		InputStream in = null;
		OutputStream out = null;

		boolean isCopied = false;

		try {

			in = new FileInputStream(src);
			out = new FileOutputStream(fileNameOut);

			byte[] buf = new byte[1024];
			int len;

			// Transfer bytes from in to out
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			isCopied = true;
		}

		return isCopied;
	}
}
