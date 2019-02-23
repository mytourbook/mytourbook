/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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

import net.tourbook.preferences.PrefPageGeneral;
import net.tourbook.tour.TourManager;

import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

public class ApplicationWorkbenchAdvisor extends WorkbenchAdvisor {

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

   @Override
   public void initialize(final IWorkbenchConfigurer configurer) {
      configurer.setSaveAndRestore(true);
   }

   @Override
   public boolean preShutdown() {
      return TourManager.getInstance().saveTours();
   }

}
