/*******************************************************************************
 * Copyright (C) 2021 Frédéric Bard
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
package net.tourbook.cloud.oauth2;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import net.tourbook.cloud.Activator;
import net.tourbook.web.PortFinder;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;

public class LocalHostServer {

   private int                     _callBackPort;

   private IPreferenceStore        _prefStore = Activator.getDefault().getPreferenceStore();
   private IPropertyChangeListener _prefChangeListener;

   private HttpServer              _server;

   private ThreadPoolExecutor      _threadPoolExecutor;

   private String                  _vendor;

   public LocalHostServer(final int callBackPort, final String vendor, final IPropertyChangeListener prefChangeListener) {

      _callBackPort = callBackPort;
      _vendor = vendor;
      _prefChangeListener = prefChangeListener;
   }

   private void addPrefListener() {

      _prefStore.addPropertyChangeListener(_prefChangeListener);
   }

   public boolean createCallBackServer(final HttpHandler httpHandler) {

      if (_server != null) {
         stopCallBackServer();
      }

      if (!PortFinder.available(_callBackPort)) {
         MessageDialog.openError(
               Display.getCurrent().getActiveShell(),
               net.tourbook.cloud.Messages.PrefPage_CloudConnectivity_UnavailablePort_Title,
               NLS.bind(net.tourbook.cloud.Messages.PrefPage_CloudConnectivity_UnavailablePort_Message, _callBackPort, _vendor));
         return false;
      }

      try {
         _server = HttpServer.create(new InetSocketAddress("localhost", _callBackPort), 0); //$NON-NLS-1$
         _server.createContext("/", httpHandler); //$NON-NLS-1$
         _threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);

         _server.setExecutor(_threadPoolExecutor);

         _server.start();

         addPrefListener();

      } catch (final IOException e) {
         e.printStackTrace();
         return false;
      }

      return true;
   }

   public void stopCallBackServer() {

      if (_server != null) {
         _server.stop(0);
         _server = null;

         _prefStore.removePropertyChangeListener(_prefChangeListener);
      }
      if (_threadPoolExecutor != null) {
         _threadPoolExecutor.shutdownNow();
      }
   }

}
