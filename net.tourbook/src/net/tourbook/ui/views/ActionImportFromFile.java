package net.tourbook.ui.views;



import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.importdata.DeviceManager;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.importdata.TourbookDevice;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.ui.views.rawData.RawDataView;

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

		RawDataView fRawDataView = null;

		try {
			// show raw data view
			fRawDataView = (RawDataView) PlatformUI
					.getWorkbench()
					.getActiveWorkbenchWindow()
					.getActivePage()
					.showView(RawDataView.ID, null, IWorkbenchPage.VIEW_ACTIVATE);

			if (fRawDataView == null) {
				return;
			}
		} catch (PartInitException e) {
			e.printStackTrace();
		}

		// setup open dialog
		FileDialog dialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.OPEN);

		ArrayList<TourbookDevice> deviceList = DeviceManager.getDeviceList();

		int deviceLength = deviceList.size() + 1;
		// create file filter list
		String[] filterExtensions = new String[deviceLength];
		String[] filterNames = new String[deviceLength];
		int deviceIndex = 0;
		for (TourbookDevice device : deviceList) {
			filterExtensions[deviceIndex] = "*." + device.fileExtension; //$NON-NLS-1$
			filterNames[deviceIndex] = device.visibleName + (" (*." + device.fileExtension + ")"); //$NON-NLS-1$ //$NON-NLS-2$

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
			fRawDataView.updateViewer();
			fRawDataView.setActionSaveEnabled(rawDataManager.isDeviceImport());
		}
	}

}
