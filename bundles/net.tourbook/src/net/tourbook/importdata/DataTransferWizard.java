/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.Util;
import net.tourbook.ui.views.rawData.RawDataView;

import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;

public class DataTransferWizard extends Wizard {

   private static final String    DIALOG_SETTINGS_SECTION   = "DataTransferWizard";       //$NON-NLS-1$

   public static final String     SYSPROPERTY_IMPORT_PERSON = "mytourbook.import.person"; //$NON-NLS-1$

   private DataTransferWizardPage _dataTransferWizardPage;
   private final List<File>       _receivedFiles            = new ArrayList<>();

   /**
    * contains the device which is used to read the data from it
    */
   private ExternalDevice         _importDevice;

   private IRunnableWithProgress  _runnableReceiveData;

   DataTransferWizard() {

      setDialogSettings();
      setNeedsProgressMonitor(true);
   }

   @Override
   public void addPages() {

      _dataTransferWizardPage = new DataTransferWizardPage("import-settings"); //$NON-NLS-1$
      addPage(_dataTransferWizardPage);

   }

   @Override
   public boolean performCancel() {

      // save state when fields in the wizard are valid
      if (_dataTransferWizardPage.isPageValid()) {
         _dataTransferWizardPage.saveState();
      }

      return true;
   }

   @Override
   public boolean performFinish() {

      if (_dataTransferWizardPage.isPageValid() == false) {
         return false;
      }

      receiveData();

      _dataTransferWizardPage.saveState();

      return true;
   }

   /**
    * @return Returns <code>true</code> when the import was successful
    */
   private boolean receiveData() {

      final Combo comboPorts = _dataTransferWizardPage._cboPorts;

      if (comboPorts.isDisposed()) {
         return false;
      }

      /*
       * Get port name
       */
      final int selectedComPort = comboPorts.getSelectionIndex();
      if (selectedComPort == -1) {
         return false;
      }

      final String portName = comboPorts.getItem(selectedComPort);

      /*
       * When the Cancel button is pressed multiple times, the app calls this function each time
       */
      if (_runnableReceiveData != null) {
         return false;
      }

      /*
       * Set the device which is used to read the data
       */
      _importDevice = _dataTransferWizardPage.getSelectedDevice();
      if (_importDevice == null) {
         return false;
      }


      /*
       * Receive data from the device
       */
      try {

         _runnableReceiveData = _importDevice.createImportRunnable(portName, _receivedFiles);

         getContainer().run(true, true, _runnableReceiveData);

      } catch (final InvocationTargetException | InterruptedException e) {
         e.printStackTrace();
      }

      if (_receivedFiles.isEmpty() || _importDevice.isImportCanceled()) {

         // data has not been received or the user canceled the import
         return true;
      }

      final RawDataManager rawDataManager = RawDataManager.getInstance();

      final FileCollisionBehavior fileCollision = new FileCollisionBehavior();
      final ImportState_Process importState_Process = new ImportState_Process();

      // import received files
      for (final File inFile : _receivedFiles) {

         rawDataManager.importTours_FromOneFile(

               inFile, //                                                  importFile
               _dataTransferWizardPage._pathEditor.getStringValue(), //    destinationPath
               fileCollision, //                                           fileCollision
               _importDevice.isBuildNewFileNames, //                       isBuildNewFileNames
               true, //                                                    isTourDisplayedInImportView
               new HashMap<>(),
               importState_Process //
         );
      }

      importState_Process.runPostProcess();

      rawDataManager.updateTourData_InImportView_FromDb(null);

      // show imported data in the raw data view
      final RawDataView importView = (RawDataView) Util.showView(RawDataView.ID, true);

      if (importView != null) {
         importView.reloadViewer();
      }

      return true;
   }

   public void setAutoDownload() {

      getContainer().getShell().addShellListener(new ShellAdapter() {
         @Override
         public void shellActivated(final ShellEvent e) {

            Display.getCurrent().asyncExec(() -> {

               // start downloading
               final boolean importResult = receiveData();

               _dataTransferWizardPage.saveState();

               if (importResult) {
                  getContainer().getShell().close();
               }
            });
         }
      });

   }

   public void setDialogSettings() {
      super.setDialogSettings(TourbookPlugin.getDefault().getDialogSettingsSection(DIALOG_SETTINGS_SECTION));
   }

}
