/*******************************************************************************
 * Copyright (C) 2005, 2019 Wolfgang Schramm and Contributors
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

import net.tourbook.Messages;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.lifecycle.PostContextCreate;
import org.eclipse.equinox.app.IApplicationContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

@SuppressWarnings("restriction")
public class LifeCycleManager {

   private static final String SPLASH_IMAGE_FILE_NAME = "splash.bmp";//$NON-NLS-1$

   @PostContextCreate
   void postContextCreate3(final IEventBroker eventBroker,
                           final IApplicationContext context,
                           final IEclipseContext workbenchContext) {

      /*
       * Setup splash, this was needed in e4 to show db update messages
       */
      final SplashManager splashManager = SplashManager.getInstance();

      splashManager.setSplashPluginId(TourbookPlugin.PLUGIN_ID);

      splashManager.setSplashImagePath(SPLASH_IMAGE_FILE_NAME);
      splashManager.open();
      splashManager.setMessage(Messages.App_SplashMessage_StartingApplication);

      // The should be a better way to close the splash, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=376821
      eventBroker.subscribe(UIEvents.UILifeCycle.APP_STARTUP_COMPLETE, new EventHandler() {

         @Override
         public void handleEvent(final Event event) {

            splashManager.close();
            eventBroker.unsubscribe(this);
         }
      });

      // close static splash screen
      context.applicationRunning();
   }

}
