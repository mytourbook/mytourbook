/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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

import java.io.File;
import java.net.URISyntaxException;

import net.tourbook.common.UI;
import net.tourbook.common.util.Util;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.PrefPageGeneral;
import net.tourbook.tour.TourManager;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

public class ApplicationWorkbenchAdvisor extends WorkbenchAdvisor {

   private static final String SYS_PROP__SET_ALL_VIEWS_CLOSABLE = "setAllViewsClosable";                               //$NON-NLS-1$

   /**
    * When <code>false</code> then all views will not be set closable in workbench.xmi
    * <p>
    * Sometimes the attribute "closeable=true" for views do disappear and a view cannot be closed
    * anymore with the mouse.
    * <p>
    * Commandline parameter: <code>-DsetAllViewsClosable=false</code>
    */
   private static String       SET_ALL_VIEWS_CLOSABLE           = System.getProperty(SYS_PROP__SET_ALL_VIEWS_CLOSABLE);
   private static boolean      IS_SET_ALL_VIEWS_CLOSABLE        = UI.FALSE.equals(SET_ALL_VIEWS_CLOSABLE) == false;

   static {

      Util.logSystemProperty_Value(ApplicationWorkbenchAdvisor.class,
            SYS_PROP__SET_ALL_VIEWS_CLOSABLE,
            SET_ALL_VIEWS_CLOSABLE,
            String.format(
                  "When \"false\" then all views will not be set closeable=\"true\" in workbench.xmi when the app closes, recognized value: \"%s\"", //$NON-NLS-1$
                  Boolean.toString(IS_SET_ALL_VIEWS_CLOSABLE)));
   }

   @Override
   public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(final IWorkbenchWindowConfigurer configurer) {
      return new ApplicationWorkbenchWindowAdvisor(this, configurer);
   }

   @Override
   public String getInitialWindowPerspectiveId() {

      // set default perspective
      return PerspectiveFactoryTourBook.PERSPECTIVE_ID;
   }

   @Override
   public String getMainPreferencePageId() {

      // set default pref page
      return PrefPageGeneral.ID;
   }

   /**
    * Copied from org.eclipse.e4.ui.internal.workbench.ResourceHandler.getBaseLocation()
    *
    * @return
    */
   private File getWorkbenchFolderPath() {

      File baseLocation;
      try {
         baseLocation = new File(URIUtil.toURI(Platform.getInstanceLocation().getURL()));
      } catch (final URISyntaxException e) {
         throw new RuntimeException(e);
      }

      baseLocation = new File(baseLocation, ".metadata"); //$NON-NLS-1$
      baseLocation = new File(baseLocation, ".plugins"); //$NON-NLS-1$

      return new File(baseLocation, "org.eclipse.e4.workbench"); //$NON-NLS-1$
   }

   @Override
   public void initialize(final IWorkbenchConfigurer configurer) {

      configurer.setSaveAndRestore(true);
   }

   @Override
   public void postShutdown() {

      if (IS_SET_ALL_VIEWS_CLOSABLE) {

         // when the timer delay is 100 ms then the task is not run
         Display.getDefault().timerExec(0, () -> {

            ApplicationTools.fixClosableAttribute(getWorkbenchFolderPath());
         });
      }

      /**
       * Ensure the derby database is shutted down to fix
       * https://github.com/mytourbook/mytourbook/pull/1091#issuecomment-1559732337
       */
      TourDatabase.getInstance().shutdownDatabaseServer();
   }

   @Override
   public boolean preShutdown() {

      return TourManager.getInstance().saveTours();
   }
}
