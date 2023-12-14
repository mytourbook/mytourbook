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
import net.tourbook.OtherMessages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.ui.SubMenu;
import net.tourbook.data.TourData;
import net.tourbook.data.TourLocation;
import net.tourbook.tour.location.SlideoutLocationProfiles;
import net.tourbook.tour.location.TourLocationData;
import net.tourbook.tour.location.TourLocationManager;
import net.tourbook.tour.location.TourLocationProfile;
import net.tourbook.ui.ITourProvider;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;

/**
 * Actions to manage start/end location
 *
 * <pre>
 *
 * -- Set Start/End Location
 *
 *   -- >> Profile <<
 *   ----------------------
 *   -- Profile 1
 *   -- Profile 2
 *   -- Profile 3
 *
 *   ----------------------
 *
 *   -- Set only Start Location
 *     -- Profile 1
 *     -- Profile 2
 *     -- Profile 3
 *
 *   -- Set only End Location
 *     -- Profile 1
 *     -- Profile 2
 *     -- Profile 3
 *
 *   ----------------------
 *   -- Open Profile Editor
 *
 * </pre>
 */
public class ActionSetStartEndLocation extends SubMenu {


   private static final String          ID           = "net.tourbook.ui.action.ActionSetStartEndLocation"; //$NON-NLS-1$

   private static final String          PROFILE_NAME = "%s - %d";                                          //$NON-NLS-1$

   private static final IDialogSettings _state       = TourbookPlugin.getState(ID);

   private ITourProvider                _tourProvider;

   private ActionEditProfiles           _actionEditProfiles;
   private Action                       _actionProfileTitle;
   private ActionRemoveLocation_All     _actionRemoveLocation_All;
   private ActionRemoveLocation_Start   _actionRemoveLocation_Start;
   private ActionRemoveLocation_End     _actionRemoveLocation_End;
   private ActionSetLocation_Start      _actionSetLocation_Start;
   private ActionSetLocation_End        _actionSetLocation_End;

   private Control                      _ownerControl;

   private ArrayList<TourData>          _allSelectedTours;

   private class ActionEditProfiles extends Action {

      public ActionEditProfiles() {

         super(Messages.Tour_Location_Action_OpenProfileEditor, AS_PUSH_BUTTON);
      }

      @Override
      public void run() {
         actionOpenProfileSlideout();
      }
   }

   private class ActionLocationProfile extends Action {

      private TourLocationProfile _locationProfile;
      private boolean             _isSetStartLocation;
      private boolean             _isSetEndLocation;

      public ActionLocationProfile(final TourLocationProfile locationProfile,
                                   final boolean isDefaultProfile,
                                   final boolean isSetStartLocation,
                                   final boolean isSetEndLocation) {

         super(UI.EMPTY_STRING, AS_CHECK_BOX);

         _locationProfile = locationProfile;

         _isSetStartLocation = isSetStartLocation;
         _isSetEndLocation = isSetEndLocation;

         final String profileName = PROFILE_NAME.formatted(_locationProfile.getName(), _locationProfile.getZoomlevel());

         setText(profileName);
         setToolTipText(createLocationTooltipText(locationProfile, _isSetStartLocation, isSetEndLocation));

         if (isDefaultProfile) {
            setChecked(true);
         }
      }

      @Override
      public void run() {

         actionSetTourLocation(_locationProfile, _isSetStartLocation, _isSetEndLocation);
      }
   }

   private class ActionRemoveLocation_All extends Action {

      public ActionRemoveLocation_All() {

         super(Messages.Tour_Location_Action_RemoveLocation_All, AS_PUSH_BUTTON);
      }

      @Override
      public void run() {
         actionRemoveLocation(true, true);
      }
   }

   private class ActionRemoveLocation_End extends Action {

      public ActionRemoveLocation_End() {

         super(Messages.Tour_Location_Action_RemoveLocation_End, AS_PUSH_BUTTON);
      }

      @Override
      public void run() {
         actionRemoveLocation(false, true);
      }
   }

   private class ActionRemoveLocation_Start extends Action {

      public ActionRemoveLocation_Start() {

         super(Messages.Tour_Location_Action_RemoveLocation_Start, AS_PUSH_BUTTON);
      }

      @Override
      public void run() {
         actionRemoveLocation(true, false);
      }
   }

   private class ActionSetLocation_End extends SubMenu {

      public ActionSetLocation_End() {

         super(Messages.Tour_Location_Action_SetLocation_End, AS_DROP_DOWN_MENU);
      }

      @Override
      public void enableActions() {}

      @Override
      public void fillMenu(final Menu menu) {

         // create actions for each profile
         final List<TourLocationProfile> allProfiles = TourLocationManager.getProfiles();
         final int numProfiles = allProfiles.size();
         if (numProfiles > 0) {

            addActionToMenu(_actionProfileTitle);
            addSeparatorToMenu();

            fillMenu_AddAllProfileActions(menu, allProfiles, false, true);
         }
      }
   }

   private class ActionSetLocation_Start extends SubMenu {

      public ActionSetLocation_Start() {

         super(Messages.Tour_Location_Action_SetLocation_Start, AS_DROP_DOWN_MENU);
      }

      @Override
      public void enableActions() {}

      @Override
      public void fillMenu(final Menu menu) {

         // create actions for each profile
         final List<TourLocationProfile> allProfiles = TourLocationManager.getProfiles();
         final int numProfiles = allProfiles.size();
         if (numProfiles > 0) {

            addActionToMenu(_actionProfileTitle);
            addSeparatorToMenu();

            fillMenu_AddAllProfileActions(menu, allProfiles, true, false);
         }
      }
   }

   /**
    * Submenu: Set S&tart/End Location
    *
    * @param tourProvider
    * @param ownerControl
    */
   public ActionSetStartEndLocation(final ITourProvider tourProvider,
                                    final Control ownerControl) {

      super(Messages.Tour_Location_Action_SetLocation_StartEnd, AS_DROP_DOWN_MENU);

      setToolTipText(Messages.Tour_Location_Action_SetLocation_StartEnd_Tooltip);
      setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.Tour_StartEnd));

      _tourProvider = tourProvider;
      _ownerControl = ownerControl;

      createActions();
   }

   private void actionOpenProfileSlideout() {

      final List<TourData> selectedTours = _tourProvider.getSelectedTours();
      final TourData tourData = selectedTours.get(0);

      // ensure that location data are available
      final TourLocationData tourLocationData = tourData.tourLocationData_Start;
      if (tourLocationData == null) {

         final double[] latitudeSerie = tourData.latitudeSerie;
         final double[] longitudeSerie = tourData.longitudeSerie;

         if (latitudeSerie == null || latitudeSerie.length == 0) {
            return;
         }

         final TourLocationData retrievedLocationData = TourLocationManager.getLocationData(
               latitudeSerie[0],
               longitudeSerie[0],
               null,
               TourLocationManager.DEFAULT_ZOOM_LEVEL_VALUE);

         if (retrievedLocationData == null) {
            return;
         }

         tourData.setTourLocationStart(retrievedLocationData.tourLocation);
         tourData.tourLocationData_Start = retrievedLocationData;
      }

      final Point cursorLocation = Display.getCurrent().getCursorLocation();
      final Rectangle ownerBounds = new Rectangle(cursorLocation.x, cursorLocation.y, 0, 0);

      // !!! must be created lately otherwise the UI is not fully setup !!!
      final SlideoutLocationProfiles slideoutLocationProfiles = new SlideoutLocationProfiles(

            null,
            tourData,
            _ownerControl,
            ownerBounds,
            _state,

            true // use start location data
      );

      slideoutLocationProfiles.open(false);
   }

   private void actionRemoveLocation(final boolean isStartLocation, final boolean isEndLocation) {

      TourLocationManager.removeTourLocations(

            _allSelectedTours,
            isStartLocation,
            isEndLocation);
   }

   private void actionSetTourLocation(final TourLocationProfile locationProfile,
                                      final boolean isSetStartLocation,
                                      final boolean isSetEndLocation) {

      TourLocationManager.setTourLocations(

            _allSelectedTours,
            locationProfile,
            isSetStartLocation,
            isSetEndLocation,

            false // isForceReloadLocation
      );
   }

   private void createActions() {

      // create a dummy action for the profile title
      _actionProfileTitle = new Action(Messages.Tour_Location_Action_ProfileTitle) {};
      _actionProfileTitle.setEnabled(false);

      _actionEditProfiles = new ActionEditProfiles();

      _actionRemoveLocation_All = new ActionRemoveLocation_All();
      _actionRemoveLocation_Start = new ActionRemoveLocation_Start();
      _actionRemoveLocation_End = new ActionRemoveLocation_End();
      _actionSetLocation_Start = new ActionSetLocation_Start();
      _actionSetLocation_End = new ActionSetLocation_End();
   }

   private String createLocationTooltipText(final TourLocationProfile locationProfile,
                                            final boolean isSetStartLocation,
                                            final boolean isSetEndLocation) {

      final String joinedPartNames = TourLocationManager.createJoinedPartNames(locationProfile, UI.NEW_LINE1);
      final TourData firstTour = _allSelectedTours.get(0);

      final TourLocation tourLocationStart = firstTour.getTourLocationStart();
      final TourLocation tourLocationEnd = firstTour.getTourLocationEnd();

      String locationText = null;

      if (tourLocationStart != null) {

         locationText = OtherMessages.TOUR_TOOLTIP_LABEL_LOCATION_START
               + UI.NEW_LINE
               + TourLocationManager.createLocationDisplayName(tourLocationStart, locationProfile);
      }

      if (tourLocationEnd != null) {

         final String endLocationText = TourLocationManager.createLocationDisplayName(tourLocationEnd, locationProfile);

         if (endLocationText.length() > 0) {

            if (locationText.length() > 0) {
               locationText += UI.NEW_LINE3;
            }

            locationText += OtherMessages.TOUR_TOOLTIP_LABEL_LOCATION_END
                  + UI.NEW_LINE
                  + endLocationText;
         }
      }

      String tooltipText = Messages.Tour_Location_Action_Profile_Tooltip.formatted(

            joinedPartNames,
            locationProfile.getZoomlevel());

      if (locationText.length() > 0) {

         tooltipText += UI.NEW_LINE3 + locationText;
      }

      return tooltipText;
   }

   @Override
   public void enableActions() {}

   @Override
   public void fillMenu(final Menu menu) {

      _allSelectedTours = _tourProvider.getSelectedTours();

      // create actions for each profile
      final List<TourLocationProfile> allProfiles = TourLocationManager.getProfiles();
      final int numProfiles = allProfiles.size();
      if (numProfiles > 0) {

         addActionToMenu(_actionProfileTitle);
         addSeparatorToMenu();

         fillMenu_AddAllProfileActions(menu, allProfiles, true, true);

         addSeparatorToMenu();
      }

      addActionToMenu(_actionSetLocation_Start);
      addActionToMenu(_actionSetLocation_End);
      addActionToMenu(_actionRemoveLocation_All);
      addActionToMenu(_actionRemoveLocation_Start);
      addActionToMenu(_actionRemoveLocation_End);

      addSeparatorToMenu();

      addActionToMenu(_actionEditProfiles);
   }

   private void fillMenu_AddAllProfileActions(final Menu menu,
                                              final List<TourLocationProfile> allProfiles,
                                              final boolean isSetStartLocation,
                                              final boolean isSetEndLocation) {

      final TourLocationProfile defaultProfile = TourLocationManager.getDefaultProfile();

      // sort profiles by name
      Collections.sort(allProfiles);

      for (final TourLocationProfile locationProfile : allProfiles) {

         final boolean isDefaultProfile = locationProfile.equals(defaultProfile);

         addActionToMenu(menu,

               new ActionLocationProfile(
                     locationProfile,
                     isDefaultProfile,

                     isSetStartLocation,
                     isSetEndLocation));
      }
   }

}
