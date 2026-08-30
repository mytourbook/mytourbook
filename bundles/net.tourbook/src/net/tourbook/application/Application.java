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

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

/**
 * This class controls all aspects of the application's execution
 */
public class Application implements IApplication {

   private static final String LOCALHOST                   = "127.0.0.1";                                            //$NON-NLS-1$
   private static final String INSTANCE_MESSAGE_SET_FOCUS  = "INSTANCE_MESSAGE_SET_FOCUS";                           //$NON-NLS-1$

   /**
    * Choose a unique, unused port
    * <p>
    * 1024 to 49151 (Registered Ports): Best for user applications and custom servers.
    */
   private static final int    PORT                        = 37217;

   private static ServerSocket _serverSocket;

   private static final String SYS_PROP__NO_INSTANCE_CHECK = "noInstanceCheck";                                      //$NON-NLS-1$

   /**
    * When this parameter is set, then another instance of MT is not checked during the app startup
    * <p>
    * Commandline parameter: <code>-DnoInstanceCheck</code>
    */
   private static boolean      IS_INSTANCE_CHECK           = System.getProperty(SYS_PROP__NO_INSTANCE_CHECK) == null;

   static {

      if (IS_INSTANCE_CHECK == false) {

         Util.logSystemProperty_IsEnabled(UI.class,
               SYS_PROP__NO_INSTANCE_CHECK,
               "A 2nd instance of MyTourbook is NOT checked"); //$NON-NLS-1$
      }
   }

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
         out.println(INSTANCE_MESSAGE_SET_FOCUS);

      } catch (final Exception e) {

         StatusUtil.logError("Could not contact the primary instance of MyTourbook on port: %d - %s".formatted(
               PORT,
               e.getMessage()));
      }
   }

   @Override
   public Object start(final IApplicationContext context) throws Exception {

      final Display display = PlatformUI.createDisplay();

      try {

         if (IS_INSTANCE_CHECK) {

            // instance is checked

            // 1. Attempt to claim the port
            final boolean isFirstInstance = checkFirstInstance();

            if (isFirstInstance == false) {

               // show message to user on the second instance before closing
               final Shell shell = new Shell(display);
               {
                  MessageDialog.openInformation(shell,
                        "MyTourbook", //$NON-NLS-1$
                        Messages.App_Info_AnotherInstanceOfMyTourbookIsRunning);
               }
               shell.dispose();

               // this is a duplicate instance. Tell the first instance to focus.
               notifyFirstInstance();

               // exit immediately
               return IApplication.EXIT_OK;
            }

            // 2. Start the background listener for subsequent instances
            startInstanceListenerThread();
         }

         // 3. Normal Eclipse RCP startup sequence
         final int returnCode = PlatformUI.createAndRunWorkbench(display, new ApplicationWorkbenchAdvisor());

         if (returnCode == PlatformUI.RETURN_RESTART) {
            return IApplication.EXIT_RESTART;
         } else {
            return IApplication.EXIT_OK;
         }

      } finally {

         display.dispose();

         if (_serverSocket != null && !_serverSocket.isClosed()) {
            _serverSocket.close();
         }
      }
   }

   private void startInstanceListenerThread() {

      final Thread listenerThread = new Thread(() -> {

         while (_serverSocket != null && !_serverSocket.isClosed()) {

            try ( /*
                   * Listens for a connection to be made to this socket and accepts it. The method
                   * blocks until a connection is made.
                   */
                  Socket clientSocket = _serverSocket.accept();

                  BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

               final String message = in.readLine();

               if (INSTANCE_MESSAGE_SET_FOCUS.equals(message)) {

                  // SWT commands must be executed on the UI thread
                  final Display display = PlatformUI.getWorkbench().getDisplay();

                  if (display != null && !display.isDisposed()) {

                     display.asyncExec(() -> {

                        if (display.isDisposed()) {
                           return;
                        }

                        /*
                         * Find MT shell
                         */
                        final Shell[] allShells = display.getShells();

                        for (final Shell shell : allShells) {

                           final String shellTitle = shell.getText();

                           if (shellTitle.contains("MyTourbook")) { //$NON-NLS-1$

                              if (shell.getMinimized()) {
                                 shell.setMinimized(false);
                              }

                              shell.forceActive(); // forces OS window focus
                              shell.setActive();

                              break;
                           }
                        }
                     });
                  }
               }
            } catch (final Exception e) {

               // Loop ends if socket closes during shutdown

               StatusUtil.log(e);
            }
         }

      }, "MyTourbook 2nd Instance Listener Thread"); //$NON-NLS-1$

      listenerThread.setDaemon(true);
      listenerThread.start();
   }

   @Override
   public void stop() {

      if (PlatformUI.isWorkbenchRunning() == false) {
         return;
      }

      final IWorkbench workbench = PlatformUI.getWorkbench();
      if (workbench == null) {
         return;
      }

      final Display display = workbench.getDisplay();

      display.syncExec(() -> {

         if (!display.isDisposed()) {
            workbench.close();
         }
      });
   }
}
