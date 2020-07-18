/*******************************************************************************
 * Copyright (C) 2005, 2020 Wolfgang Schramm and Contributors
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.importdata.ExternalDevice;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.importdata.SerialParameters;
import net.tourbook.importdata.TourbookDevice;
import net.tourbook.ui.UI;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;

public abstract class SimpleSerialDevice extends ExternalDevice {

   protected TourbookDevice            tourbookDevice;
   protected boolean                   isCancelImport;

   private final ByteArrayOutputStream _rawDataBuffer = new ByteArrayOutputStream();
   private List<File>                  _receivedFiles;

   public SimpleSerialDevice() {
      tourbookDevice = getTourbookDevice();
   }

   /**
    * Append newly received data to the internal data buffer
    *
    * @param newData
    *           data being written into the buffer
    */
   public void appendReceivedData(final int newData) {
      _rawDataBuffer.write(newData);
   }

   public void cancelImport() {
      isCancelImport = true;
   }

   @Override
   public IRunnableWithProgress createImportRunnable(final String portName, final List<File> receivedFiles) {

      _receivedFiles = receivedFiles;

      return new IRunnableWithProgress() {
         @Override
         public void run(final IProgressMonitor monitor) {

            final SerialParameters portParameters = tourbookDevice.getPortParameters(portName);

            if (portParameters == null) {
               return;
            }

            final String msg = NLS.bind(Messages.Import_Wizard_Monitor_task_msg,
                  new Object[] {
                        visibleName,
                        portName,
                        portParameters.getBaudRate() });

            monitor.beginTask(msg, tourbookDevice.getTransferDataSize());

            readDeviceData(monitor, portName);
            saveReceivedData();
         }

      };
   }

   public abstract String getFileExtension();

   public abstract TourbookDevice getTourbookDevice();

   @Override
   public boolean isImportCanceled() {
      return isCancelImport;
   }

   /**
    * Reads data from the device and save it in a buffer
    *
    * @param monitor
    */
   private void readDeviceData(final IProgressMonitor monitor, final String portName) {

      // truncate databuffer
      _rawDataBuffer.reset();

      int receiveTimeout = 0;
      int receiveTimer = 0;
      int receivedData = 0;
      boolean isReceivingStarted = false;

      isCancelImport = false;

      final int importDataSize = tourbookDevice.getTransferDataSize();
      int timer = 0;

      // start the port thread which reads data from the com port
      final PortThread portThreadRunnable = new PortThread(this, portName);

      final Thread portThread = new Thread(portThreadRunnable, Messages.Import_Wizard_Thread_name_read_device_data);
      portThread.start();

      /*
       * wait for the data thread, terminate after a certain time (600 x 100 milliseconds = 60
       * seconds)
       */

      while (receiveTimeout < RECEIVE_TIMEOUT) {

         try {
            Thread.sleep(100);
         } catch (final InterruptedException e2) {
            e2.printStackTrace();
         }

         if (isReceivingStarted == false) {
            monitor.subTask(NLS.bind(Messages.Import_Wizard_Monitor_wait_for_data,
                  (RECEIVE_TIMEOUT / 10 - (receiveTimeout / 10))));
         }

         final int rawDataSize = _rawDataBuffer.size();

         /*
          * if receiving data was started and no more data are coming in, stop receiving
          * additional data
          */
         if (isReceivingStarted && receiveTimer == 10 & rawDataSize == receivedData) {
            break;
         }

         // if user pressed the cancel button, exit the import
         if (monitor.isCanceled() || isCancelImport == true) {

            // close the dialog when the monitor was canceled
            isCancelImport = true;

            // reset databuffer to prevent saving the content
            _rawDataBuffer.reset();
            break;
         }

         // check if new data are arrived
         if (rawDataSize > receivedData) {

            // set status that receiving of the data has been started
            isReceivingStarted = true;
            timer++;

            // reset timeout if data are being received
            receiveTimeout = 0;
            receiveTimer = 0;

            // advance progress monitor
            monitor.worked(rawDataSize - receivedData);

            // display the bytes which have been received
            monitor.subTask(NLS.bind(Messages.Import_Wizard_Monitor_task_received_bytes,
                  new Object[] {
                        Integer.toString(receivedData * 100 / importDataSize),
                        Integer.toString(timer / 10),
                        Integer.toString(receivedData) }));
         }

         receiveTimeout++;
         receiveTimer++;

         receivedData = rawDataSize;
      }

      // tell the port listener thread to stop
      monitor.subTask(Messages.Import_Wizard_Monitor_stop_port);
      portThreadRunnable.prepareInterrupt();

      portThread.interrupt();

      // wait for the port thread to be terminated
      try {
         portThread.join();
      } catch (final InterruptedException e) {
         e.printStackTrace();
      }
   }

   /**
    * write received data into a temp file
    */
   private void saveReceivedData() {

      try {

         final File tempFile = File.createTempFile("myTourbook", //$NON-NLS-1$
               UI.SYMBOL_DOT + getFileExtension(),
               new File(RawDataManager.getTempDir()));

         final FileOutputStream fileStream = new FileOutputStream(tempFile);
         fileStream.write(_rawDataBuffer.toByteArray());
         fileStream.close();

         _receivedFiles.add(tempFile);

      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

}
