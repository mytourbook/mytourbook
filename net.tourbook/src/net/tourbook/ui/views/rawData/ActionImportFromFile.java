package net.tourbook.ui.views.rawData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import net.tourbook.Messages;
import net.tourbook.importdata.DeviceManager;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.importdata.TourbookDevice;
import net.tourbook.plugin.TourbookPlugin;

import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

public class ActionImportFromFile extends Action {

	public ActionImportFromFile() {
		setText(Messages.RawData_Action_open_import_file);
		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image_open_import_file));
		setToolTipText(Messages.RawData_Action_open_import_file_tooltip);
	}

	/**
	 * import tour data from a file
	 */
	public void run() {

		RawDataView rawDataView = null;

		try {
			// show raw data view
			rawDataView = (RawDataView) PlatformUI
					.getWorkbench()
					.getActiveWorkbenchWindow()
					.getActivePage()
					.showView(RawDataView.ID, null, IWorkbenchPage.VIEW_ACTIVATE);

			if (rawDataView == null) {
				return;
			}
		} catch (PartInitException e) {
			e.printStackTrace();
		}

		// setup open dialog
		FileDialog fileDialog = new FileDialog(
				Display.getCurrent().getActiveShell(),
				(SWT.OPEN | SWT.MULTI));

		ArrayList<TourbookDevice> deviceList = DeviceManager.getDeviceList();

		// sort device list alphabetically
		Collections.sort(deviceList, new Comparator<TourbookDevice>() {
			public int compare(TourbookDevice o1, TourbookDevice o2) {
				return o1.visibleName.compareTo(o2.visibleName);
			}
		});

		// create file filter list
		int deviceLength = deviceList.size() + 1;
		String[] filterExtensions = new String[deviceLength];
		String[] filterNames = new String[deviceLength];
		
		int deviceIndex = 0;

		// add option to show all files
		filterExtensions[deviceIndex] = "*.*"; //$NON-NLS-1$
		filterNames[deviceIndex] = "*.*"; //$NON-NLS-1$
		
		deviceIndex++;
		
		// add option for every file extension
		for (TourbookDevice device : deviceList) {
			filterExtensions[deviceIndex] = "*." + device.fileExtension; //$NON-NLS-1$
			filterNames[deviceIndex] = device.visibleName + (" (*." + device.fileExtension + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			deviceIndex++;
		}

		// open file dialog
		fileDialog.setFilterExtensions(filterExtensions);
		fileDialog.setFilterNames(filterNames);

		String firstFileName = fileDialog.open();

		// check if user canceled the dialog
		if (firstFileName == null) {
			return;
		}

		RawDataManager rawDataManager = RawDataManager.getInstance();
		
		Path filePath = new Path(firstFileName);
		final String[] selectedFileNames = fileDialog.getFileNames();
		boolean isImported = false;

		// loop: import all selected files
		for (String fileName : selectedFileNames) {

			// replace filename, keep the directory path
			fileName = filePath.removeLastSegments(1).append(fileName).makeAbsolute().toString();

			if (isImported) {
				rawDataManager.importRawData(fileName);
			} else {
				isImported = rawDataManager.importRawData(fileName);
			}
		}

		if (isImported) {
			rawDataManager.updatePersonInRawData();
			rawDataView.updateViewer();
			rawDataView.selectFirstTour();
			rawDataView.setActionSaveEnabled(rawDataManager.isDeviceImport());
		}
	}

}
