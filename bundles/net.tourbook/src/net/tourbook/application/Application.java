/*******************************************************************************
 * Copyright (C) 2005, 2026 Wolfgang Schramm and Contributors
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
package net.tourbook.application;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import net.tourbook.common.util.StatusUtil;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

/**
 *
 */
public class Application implements IApplication {

   private static final String LOCALHOST            = "127.0.0.1";
   private static final String NOTIFICATION_MESSAGE = "BRING_TO_FRONT";

   /**
    * 1024 to 49151 (Registered Ports): Best for user applications and custom servers.
    */
   private static final int    PORT                 = 37217;           // Choose a unique, unused port

   private static ServerSocket _serverSocket;


   private boolean checkFirstInstance() {
      try {

         // Bind to localhost only for security
         _serverSocket = new ServerSocket(PORT, 10, InetAddress.getByName(LOCALHOST));

         return true;

      } catch (final Exception e) {

         // Port is blocked; an instance is already running
         return false;
      }
   }

   private void notifyFirstInstance() {

      try (Socket socket = new Socket(LOCALHOST, PORT);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

         // send a message
         out.println(NOTIFICATION_MESSAGE);

      } catch (final Exception e) {

         StatusUtil.logError("Could not contact the primary instance: " + e.getMessage());
      }
   }

   @Override
   public Object start(final IApplicationContext context) throws Exception {

      final Display display = PlatformUI.createDisplay();

      try {

         // 1. Attempt to claim the port
         final boolean isFirstInstance = checkFirstInstance();

         if (isFirstInstance == false) {

            // show message to user on the second instance before closing
            final Shell shell = new Shell(display);
            {
               MessageDialog.openInformation(shell,
                     "MyTourbook Already Running",
                     "An instance of MyTourbook is already running. Switching to the active window.");
            }
            shell.dispose();

            // this is a duplicate instance. Tell the first instance to focus.
            notifyFirstInstance();

            // exit immediately
            return IApplication.EXIT_OK;
         }

         // 2. Start the background listener for subsequent instances
         startInstancelListener();

         // 3. Normal Eclipse RCP startup sequence

         final int returnCode = PlatformUI.createAndRunWorkbench(display, new ApplicationWorkbenchAdvisor());

         if (returnCode == PlatformUI.RETURN_RESTART) {
            return IApplication.EXIT_RESTART;
         }

         return IApplication.EXIT_OK;

      } finally {

         display.dispose();

         if (_serverSocket != null && !_serverSocket.isClosed()) {
            _serverSocket.close();
         }
      }
   }

   private void startInstancelListener() {

      final Thread thread = new Thread(() -> {

         while (_serverSocket != null && !_serverSocket.isClosed()) {

            try (Socket clientSocket = _serverSocket.accept();
                  BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

               final String message = in.readLine();

               if (NOTIFICATION_MESSAGE.equals(message)) {

                  // SWT commands must be executed on the UI thread
                  final Display display = PlatformUI.getWorkbench().getDisplay();

                  if (display != null && !display.isDisposed()) {

                     display.asyncExec(() -> {

                        final Shell shell = display.getActiveShell();

                        if (shell != null) {

                           if (shell.getMinimized()) {
                              shell.setMinimized(false);
                           }

                           shell.forceActive(); // Forces OS window focus
                           shell.setActive();
                        }
                     });
                  }
               }
            } catch (final Exception e) {

               // Loop ends if socket closes during shutdown
            }
         }

      }, "MyTourbook 2nd Instance Listener Thread");

      thread.setDaemon(true);
      thread.start();
   }

   @Override
   public void stop() {

      if (!PlatformUI.isWorkbenchRunning()) {
         return;
      }

      final IWorkbench workbench = PlatformUI.getWorkbench();
      final Display display = workbench.getDisplay();

      display.syncExec(() -> {

         if (!display.isDisposed()) {
            workbench.close();
         }
      });
   }
}
