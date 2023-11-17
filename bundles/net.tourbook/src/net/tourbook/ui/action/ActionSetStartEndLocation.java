/*******************************************************************************
 * Copyright (C) 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.ui.SubMenu;
import net.tourbook.data.TourData;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.location.TourLocationManager;
import net.tourbook.tour.location.TourLocationProfile;
import net.tourbook.ui.ITourProvider;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Menu;

public class ActionSetStartEndLocation extends SubMenu {

   private ITourProvider _tourProvider;

   private Action        _actionProfileTitle;

   private class ActionLocationProfile extends Action {

      private TourLocationProfile _locationProfile;

      public ActionLocationProfile(final TourLocationProfile locationProfile, final boolean isDefaultProfile) {

         super(UI.EMPTY_STRING, AS_CHECK_BOX);

         _locationProfile = locationProfile;

         final String profileName = _locationProfile.getName();
         final String joinedPartNames = TourLocationManager.createJoinedPartNames(locationProfile, UI.NEW_LINE1);

         setText(profileName);
         setToolTipText(Messages.Tour_Location_Action_Profile_Tooltip.formatted(profileName, joinedPartNames));

         if (isDefaultProfile) {
            setChecked(true);
         }
      }

      @Override
      public void run() {

         onSelectProfile(_locationProfile);
      }
   }

   /**
    * @param tourProvider
    */
   public ActionSetStartEndLocation(final ITourProvider tourProvider) {

      super(Messages.Tour_Action_SetStartEndLocation, AS_DROP_DOWN_MENU);

      setToolTipText(Messages.Tour_Action_SetStartEndLocation_Tooltip);
      setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.Tour_StartEnd));

      _tourProvider = tourProvider;

      createActions();
   }

   private void createActions() {

      _actionProfileTitle = new Action(Messages.Tour_Location_Action_ProfileTitle) {};
      _actionProfileTitle.setEnabled(false);
   }

   @Override
   public void enableActions() {

   }

   @Override
   public void fillMenu(final Menu menu) {

      // create actions for each profile
      final List<TourLocationProfile> allProfiles = TourLocationManager.getProfiles();
      final int numProfiles = allProfiles.size();
      if (numProfiles > 0) {

         final TourLocationProfile defaultProfile = TourLocationManager.getDefaultProfile();

         // sort profiles by name
         Collections.sort(allProfiles);

         addActionToMenu(_actionProfileTitle);
         addSeparatorToMenu();

         for (final TourLocationProfile locationProfile : allProfiles) {

            final boolean isDefaultProfile = locationProfile.equals(defaultProfile);

            addActionToMenu(new ActionLocationProfile(locationProfile, isDefaultProfile));
         }
      }
   }

   private void onSelectProfile(final TourLocationProfile locationProfile) {

      final List<TourData> selectedTours = _tourProvider.getSelectedTours();
      final ArrayList<TourData> modifiedTours = new ArrayList<>();

      TourLocationManager.setLocationNames(selectedTours, modifiedTours, locationProfile);

      if (modifiedTours.size() > 0) {

         TourManager.saveModifiedTours(modifiedTours);
      }
   }
}
