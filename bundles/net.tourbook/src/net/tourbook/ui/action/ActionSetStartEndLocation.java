/*******************************************************************************
 * Copyright (C) 2023, 2024 Wolfgang Schramm and Contributors
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.ui.SubMenu;
import net.tourbook.data.TourData;
import net.tourbook.data.TourLocation;
import net.tourbook.tour.location.LocationPartID;
import net.tourbook.tour.location.PartItem;
import net.tourbook.tour.location.SlideoutLocationProfiles;
import net.tourbook.tour.location.TourLocationData;
import net.tourbook.tour.location.TourLocationManager;
import net.tourbook.tour.location.TourLocationProfile;
import net.tourbook.ui.ITourProvider;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
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

   private static final String             ID                 = "net.tourbook.ui.action.ActionSetStartEndLocation"; //$NON-NLS-1$

   private static final String             LOCATION_SEPARATOR = "     ·     ";                                      //$NON-NLS-1$

   private static final String             PROFILE_NAME       = "%s - %d";                                          //$NON-NLS-1$

   private static final IDialogSettings    _state             = TourbookPlugin.getState(ID);

   /**
    * This set is used to prevent duplicated action names
    */
   private static final Set<String>        _usedDisplayNames  = new HashSet<>();
   //
   private ITourProvider                   _tourProvider;
   //
   private Action                          _actionPartTitle_Append_All;
   private Action                          _actionPartTitle_Append_Start;
   private Action                          _actionPartTitle_Append_End;
   private Action                          _actionPartTitle_Set_All;
   private Action                          _actionPartTitle_Set_Start;
   private Action                          _actionPartTitle_Set_End;
   private Action                          _actionProfileTitle_All;
   private Action                          _actionProfileTitle_Start;
   private Action                          _actionProfileTitle_End;
   private ActionEditProfiles              _actionEditProfiles;
   private ActionLocationPart_Append_All   _actionLocationPart_Append_All;
   private ActionLocationPart_Append_Start _actionLocationPart_Append_Start;
   private ActionLocationPart_Append_End   _actionLocationPart_Append_End;
   private ActionLocationPart_Set_All      _actionLocationPart_Set_All;
   private ActionLocationPart_Set_Start    _actionLocationPart_Set_Start;
   private ActionLocationPart_Set_End      _actionLocationPart_Set_End;
   private ActionRemoveLocation_All        _actionRemoveLocation_All;
   private ActionRemoveLocation_All        _actionRemoveLocation_All_Complete;
   private ActionRemoveLocation_Start      _actionRemoveLocation_Start;
   private ActionRemoveLocation_Start      _actionRemoveLocation_Start_Complete;
   private ActionRemoveLocation_End        _actionRemoveLocation_End;
   private ActionRemoveLocation_End        _actionRemoveLocation_End_Complete;
   private ActionSetLocation_Start         _actionSetLocation_Start;
   private ActionSetLocation_End           _actionSetLocation_End;
   //
   private SlideoutLocationProfiles        _slideoutLocationProfiles;
   //
   private ArrayList<TourData>             _allSelectedTours;
   //
   /**
    * When <code>null</code> then a start or end location is not hovered
    */
   private Boolean                         _isStartLocationInContextMenu;
   //
   /*
    * UI controls
    */
   private Control _ownerControl;

   private class ActionAppendLocationPart extends Action {

      private LocationPartID _partID_Start;
      private LocationPartID _partID_End;

      private boolean        _isSetStartLocation;
      private boolean        _isSetEndLocation;

      public ActionAppendLocationPart(final String actionText,
                                      final String actionTooltip,

                                      final boolean isSetStartLocation,
                                      final boolean isSetEndLocation,

                                      final LocationPartID startPartID,
                                      final LocationPartID endPartID) {

         super(actionText, AS_PUSH_BUTTON);

         setToolTipText(actionTooltip);

         _isSetStartLocation = isSetStartLocation;
         _isSetEndLocation = isSetEndLocation;

         _partID_Start = startPartID;
         _partID_End = endPartID;
      }

      @Override
      public void run() {

         TourLocationManager.locationPart_Append(

               _allSelectedTours,

               _partID_Start,
               _partID_End,

               _isSetStartLocation,
               _isSetEndLocation);
      }

   }

   public class ActionData_Part {

      private String         actionText;
      private String         actionTooltip;

      private boolean        isSetStartLocation;
      private boolean        isSetEndLocation;

      private LocationPartID startPartID;
      private LocationPartID endPartID;

      public ActionData_Part(final String actionText,
                             final String actionTooltip,
                             final boolean isSetStartLocation,
                             final boolean isSetEndLocation,
                             final LocationPartID startPartID,
                             final LocationPartID endPartID) {

         this.actionText = actionText;
         this.actionTooltip = actionTooltip;

         this.isSetStartLocation = isSetStartLocation;
         this.isSetEndLocation = isSetEndLocation;

         this.startPartID = startPartID;
         this.endPartID = endPartID;
      }

   }

   public class ActionData_Profile {

      private TourLocationProfile locationProfile;

      private boolean             isSetStartLocation;
      private boolean             isSetEndLocation;

      private String              actionText;
      private String              tooltipText;

      public ActionData_Profile(final TourLocationProfile locationProfile,
                                final boolean isSetStartLocation,
                                final boolean isSetEndLocation,
                                final String actionText,
                                final String tooltipText) {

         this.locationProfile = locationProfile;

         this.isSetStartLocation = isSetStartLocation;
         this.isSetEndLocation = isSetEndLocation;

         this.actionText = actionText;
         this.tooltipText = tooltipText;
      }
   }

   private class ActionEditProfiles extends Action {

      public ActionEditProfiles() {

         super(Messages.Tour_Location_Action_OpenProfileEditor, AS_PUSH_BUTTON);
      }

      @Override
      public void run() {
         actionOpenProfileSlideout();
      }
   }

   private class ActionLocationPart_Append_All extends SubMenu {

      public ActionLocationPart_Append_All() {

         super(Messages.Tour_Location_Action_AppendLocationPart_All, AS_DROP_DOWN_MENU);
      }

      @Override
      public void enableActions() {}

      @Override
      public void fillMenu(final Menu menu) {

         addActionToMenu(_actionPartTitle_Append_All);

         fillMenu_AddAll_PartActions(menu,

               true, // isStart
               true, // isEnd

               true // isAppend
         );
      }
   }

   private class ActionLocationPart_Append_End extends SubMenu {

      public ActionLocationPart_Append_End() {

         super(Messages.Tour_Location_Action_AppendLocationPart_End, AS_DROP_DOWN_MENU);
      }

      @Override
      public void enableActions() {}

      @Override
      public void fillMenu(final Menu menu) {

         addActionToMenu(_actionPartTitle_Append_End);

         fillMenu_AddAll_PartActions(menu,

               false, // isStart
               true, // isEnd

               true // isAppend
         );
      }
   }

   private class ActionLocationPart_Append_Start extends SubMenu {

      public ActionLocationPart_Append_Start() {

         super(Messages.Tour_Location_Action_AppendLocationPart_Start, AS_DROP_DOWN_MENU);

      }

      @Override
      public void enableActions() {}

      @Override
      public void fillMenu(final Menu menu) {

         addActionToMenu(_actionPartTitle_Append_Start);

         fillMenu_AddAll_PartActions(menu,

               true, // isStart
               false, // isEnd

               true // isAppend
         );
      }
   }

   private class ActionLocationPart_Set_All extends SubMenu {

      public ActionLocationPart_Set_All() {

         super(Messages.Tour_Location_Action_SetLocationPart_All, AS_DROP_DOWN_MENU);
      }

      @Override
      public void enableActions() {}

      @Override
      public void fillMenu(final Menu menu) {

         addActionToMenu(_actionPartTitle_Set_All);

         fillMenu_AddAll_PartActions(menu,

               true, // isStart
               true, // isEnd

               false // isAppend
         );
      }
   }

   private class ActionLocationPart_Set_End extends SubMenu {

      public ActionLocationPart_Set_End() {

         super(Messages.Tour_Location_Action_SetLocationPart_End, AS_DROP_DOWN_MENU);
      }

      @Override
      public void enableActions() {}

      @Override
      public void fillMenu(final Menu menu) {

         addActionToMenu(_actionPartTitle_Set_End);

         fillMenu_AddAll_PartActions(menu,

               false, // isStart
               true, // isEnd

               false // isAppend
         );
      }
   }

   private class ActionLocationPart_Set_Start extends SubMenu {

      public ActionLocationPart_Set_Start() {

         super(Messages.Tour_Location_Action_SetLocationPart_Start, AS_DROP_DOWN_MENU);
      }

      @Override
      public void enableActions() {}

      @Override
      public void fillMenu(final Menu menu) {

         addActionToMenu(_actionPartTitle_Set_Start);

         fillMenu_AddAll_PartActions(menu,

               true, // isStart
               false, // isEnd

               false // isAppend
         );
      }
   }

   private class ActionRemoveLocation_All extends Action {

      private boolean _isCompleteRemoval;

      public ActionRemoveLocation_All(final boolean isCompleteRemoval) {

         super(UI.EMPTY_STRING, AS_PUSH_BUTTON);

         setText(isCompleteRemoval
               ? Messages.Tour_Location_Action_RemoveLocation_All_Complete
               : Messages.Tour_Location_Action_RemoveLocation_All);

         setToolTipText(Messages.Tour_Location_Action_RemoveLocation_Tooltip);

         _isCompleteRemoval = isCompleteRemoval;
      }

      @Override
      public void run() {
         actionRemoveLocation(true, true, _isCompleteRemoval);
      }
   }

   private class ActionRemoveLocation_End extends Action {

      private boolean _isCompleteRemoval;

      public ActionRemoveLocation_End(final boolean isCompleteRemoval) {

         super(UI.EMPTY_STRING, AS_PUSH_BUTTON);

         setText(isCompleteRemoval
               ? Messages.Tour_Location_Action_RemoveLocation_End_Complete
               : Messages.Tour_Location_Action_RemoveLocation_End);

         setToolTipText(Messages.Tour_Location_Action_RemoveLocation_Tooltip);

         _isCompleteRemoval = isCompleteRemoval;
      }

      @Override
      public void run() {
         actionRemoveLocation(false, true, _isCompleteRemoval);
      }
   }

   private class ActionRemoveLocation_Start extends Action {

      private boolean _isCompleteRemoval;

      public ActionRemoveLocation_Start(final boolean isCompleteRemoval) {

         super(UI.EMPTY_STRING, AS_PUSH_BUTTON);

         setText(isCompleteRemoval
               ? Messages.Tour_Location_Action_RemoveLocation_Start_Complete
               : Messages.Tour_Location_Action_RemoveLocation_Start);

         setToolTipText(Messages.Tour_Location_Action_RemoveLocation_Tooltip);

         _isCompleteRemoval = isCompleteRemoval;
      }

      @Override
      public void run() {
         actionRemoveLocation(true, false, _isCompleteRemoval);
      }
   }

   private class ActionSetLocation extends Action {

      private TourLocationProfile _locationProfile;

      private Boolean             _isSetStartLocation;
      private Boolean             _isSetEndLocation;

      /**
       * @param locationProfile
       * @param isDefaultProfile
       * @param isSetStartLocation
       * @param isSetEndLocation
       * @param actionText
       * @param tooltipText
       */
      public ActionSetLocation(final TourLocationProfile locationProfile,

                               final boolean isSetStartLocation,
                               final boolean isSetEndLocation,

                               final String actionText,
                               final String tooltipText) {

         super(actionText, AS_PUSH_BUTTON);

         setToolTipText(tooltipText);

         _locationProfile = locationProfile;

         _isSetStartLocation = isSetStartLocation;
         _isSetEndLocation = isSetEndLocation;
      }

      @Override
      public void run() {

         actionSetTourLocation(_locationProfile, _isSetStartLocation, _isSetEndLocation);
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

            addActionToMenu(_actionProfileTitle_End);

            fillMenu_AddAll_ProfileActions(menu, allProfiles, false, true);
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

            addActionToMenu(_actionProfileTitle_Start);

            fillMenu_AddAll_ProfileActions(menu, allProfiles, true, false);
         }
      }
   }

   private class ActionSetLocationPart extends Action {

      private LocationPartID _partID_Start;
      private LocationPartID _partID_End;

      private boolean        _isSetStartLocation;
      private boolean        _isSetEndLocation;

      public ActionSetLocationPart(final String actionText,
                                   final String actionTooltip,

                                   final boolean isSetStartLocation,
                                   final boolean isSetEndLocation,

                                   final LocationPartID startPartID,
                                   final LocationPartID endPartID) {

         super(actionText, AS_PUSH_BUTTON);

         setToolTipText(actionTooltip);

         _isSetStartLocation = isSetStartLocation;
         _isSetEndLocation = isSetEndLocation;

         _partID_Start = startPartID;
         _partID_End = endPartID;
      }

      @Override
      public void run() {

         TourLocationManager.locationPart_Set(

               _allSelectedTours,

               _partID_Start,
               _partID_End,

               _isSetStartLocation,
               _isSetEndLocation);
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

      super(Messages.Tour_Location_Action_ManageStartEndLocation, AS_DROP_DOWN_MENU);

      setToolTipText(Messages.Tour_Location_Action_ManageStartEndLocation_Tooltip);
      setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.Tour_StartEnd));

      _tourProvider = tourProvider;
      _ownerControl = ownerControl;

      createActions();
   }

   private void actionOpenProfileSlideout() {

      if (_slideoutLocationProfiles != null) {

         // close previous slideout otherwise they could be conflicting

         _slideoutLocationProfiles.close();
         _slideoutLocationProfiles = null;
      }

      final boolean isStartLocation = _isStartLocationInContextMenu == null
            ? true
            : _isStartLocationInContextMenu;

      final List<TourData> selectedTours = _tourProvider.getSelectedTours();
      final TourData tourData = selectedTours.get(0);

      /*
       * Ensure that location data are available
       */
      TourLocationData tourLocationData = isStartLocation
            ? tourData.tourLocationData_Start
            : tourData.tourLocationData_End;

      if (tourLocationData == null) {

         final double[] latitudeSerie = tourData.latitudeSerie;
         final double[] longitudeSerie = tourData.longitudeSerie;

         if (latitudeSerie == null || latitudeSerie.length == 0) {

            MessageDialog.openInformation(
                  _ownerControl.getShell(),
                  Messages.Tour_Location_Dialog_OpenProfileEditor_Title,
                  Messages.Tour_Location_Dialog_OpenProfileEditor_NoGeoPosition_Message);

            return;
         }

         final int lastIndex = latitudeSerie.length - 1;

         final double latitude = isStartLocation ? latitudeSerie[0] : latitudeSerie[lastIndex];
         final double longitude = isStartLocation ? longitudeSerie[0] : longitudeSerie[lastIndex];

         final TourLocationData retrievedLocationData = TourLocationManager.getLocationData(
               latitude,
               longitude,
               null,
               TourLocationManager.getProfileZoomlevel());

         if (retrievedLocationData == null) {
            return;
         }

         tourLocationData = retrievedLocationData;
      }

      final TourLocation tourLocation = tourLocationData.tourLocation;

      if (tourLocation == null) {
         return;
      }

      if (isStartLocation) {

         tourData.setTourLocationStart(tourLocation);
         tourData.tourLocationData_Start = tourLocationData;

      } else {

         tourData.setTourLocationEnd(tourLocation);
         tourData.tourLocationData_End = tourLocationData;
      }

      final Point cursorLocation = Display.getCurrent().getCursorLocation();
      final Rectangle ownerBounds = new Rectangle(cursorLocation.x, cursorLocation.y, 0, 0);

      // !!! must be created lately otherwise the UI is not fully setup !!!
      _slideoutLocationProfiles = new SlideoutLocationProfiles(

            null,
            tourLocation,

            _ownerControl,
            ownerBounds,
            _state,

            isStartLocation);

      _slideoutLocationProfiles.open(false);
   }

   private void actionRemoveLocation(final boolean isStartLocation,
                                     final boolean isEndLocation,
                                     final boolean isCompleteRemoval) {

      TourLocationManager.removeTourLocations(

            _allSelectedTours,
            isStartLocation,
            isEndLocation,
            isCompleteRemoval);
   }

   private void actionSetTourLocation(final TourLocationProfile locationProfile,
                                      final boolean isSetStartLocation,
                                      final boolean isSetEndLocation) {

      TourLocationManager.setTourLocations(

            _allSelectedTours,
            locationProfile,

            isSetStartLocation,
            isSetEndLocation,

            false, // isOneAction
            null, // oneActionLocation

            true, // isSaveTour
            false // isLogLocation
      );
   }

   private void createActions() {

// SET_FORMATTING_OFF

      _actionEditProfiles                    = new ActionEditProfiles();

      _actionLocationPart_Append_All         = new ActionLocationPart_Append_All();
      _actionLocationPart_Append_Start       = new ActionLocationPart_Append_Start();
      _actionLocationPart_Append_End         = new ActionLocationPart_Append_End();
      _actionLocationPart_Set_All            = new ActionLocationPart_Set_All();
      _actionLocationPart_Set_Start          = new ActionLocationPart_Set_Start();
      _actionLocationPart_Set_End            = new ActionLocationPart_Set_End();
      _actionRemoveLocation_All_Complete     = new ActionRemoveLocation_All(true);
      _actionRemoveLocation_All              = new ActionRemoveLocation_All(false);
      _actionRemoveLocation_Start_Complete   = new ActionRemoveLocation_Start(true);
      _actionRemoveLocation_Start            = new ActionRemoveLocation_Start(false);
      _actionRemoveLocation_End_Complete     = new ActionRemoveLocation_End(true);
      _actionRemoveLocation_End              = new ActionRemoveLocation_End(false);
      _actionSetLocation_Start               = new ActionSetLocation_Start();
      _actionSetLocation_End                 = new ActionSetLocation_End();

      // create dummy actions for the part/profile title
      _actionPartTitle_Append_All            = new Action(Messages.Tour_Location_Action_PartTitle_Append_All) {};
      _actionPartTitle_Append_Start          = new Action(Messages.Tour_Location_Action_PartTitle_Append_Start) {};
      _actionPartTitle_Append_End            = new Action(Messages.Tour_Location_Action_PartTitle_Append_End) {};
      _actionPartTitle_Set_All               = new Action(Messages.Tour_Location_Action_PartTitle_Set_All) {};
      _actionPartTitle_Set_Start             = new Action(Messages.Tour_Location_Action_PartTitle_Set_Start) {};
      _actionPartTitle_Set_End               = new Action(Messages.Tour_Location_Action_PartTitle_Set_End) {};
      _actionProfileTitle_All                = new Action(Messages.Tour_Location_Action_ProfileTitle_All) {};
      _actionProfileTitle_Start              = new Action(Messages.Tour_Location_Action_ProfileTitle_Start) {};
      _actionProfileTitle_End                = new Action(Messages.Tour_Location_Action_ProfileTitle_End) {};

      _actionPartTitle_Append_All            .setEnabled(false);
      _actionPartTitle_Append_Start          .setEnabled(false);
      _actionPartTitle_Append_End            .setEnabled(false);
      _actionPartTitle_Set_All               .setEnabled(false);
      _actionPartTitle_Set_Start             .setEnabled(false);
      _actionPartTitle_Set_End               .setEnabled(false);
      _actionProfileTitle_All                .setEnabled(false);
      _actionProfileTitle_Start              .setEnabled(false);
      _actionProfileTitle_End                .setEnabled(false);

// SET_FORMATTING_ON
   }

   @Override
   public void enableActions() {}

   @Override
   public void fillMenu(final Menu menu) {

      // get tours which are needed in the menu actions
      _allSelectedTours = _tourProvider.getSelectedTours();

      addActionToMenu(_actionEditProfiles);

      addSeparatorToMenu();

      addActionToMenu(_actionLocationPart_Set_Start);
      addActionToMenu(_actionLocationPart_Set_End);
      addActionToMenu(_actionLocationPart_Set_All);

      addActionToMenu(_actionLocationPart_Append_Start);
      addActionToMenu(_actionLocationPart_Append_End);
      addActionToMenu(_actionLocationPart_Append_All);

      addSeparatorToMenu();

      addActionToMenu(_actionSetLocation_Start);
      addActionToMenu(_actionSetLocation_End);

      // create an action for each profile
      final List<TourLocationProfile> allProfiles = TourLocationManager.getProfiles();
      if (allProfiles.size() > 0) {

         addActionToMenu(_actionProfileTitle_All);

         fillMenu_AddAll_ProfileActions(menu, allProfiles, true, true);

         addSeparatorToMenu();
      }

      addActionToMenu(_actionRemoveLocation_Start);
      addActionToMenu(_actionRemoveLocation_End);
      addActionToMenu(_actionRemoveLocation_All);

      addSeparatorToMenu();

      addActionToMenu(_actionRemoveLocation_Start_Complete);
      addActionToMenu(_actionRemoveLocation_End_Complete);
      addActionToMenu(_actionRemoveLocation_All_Complete);
   }

   private void fillMenu_AddAll_PartActions(final Menu menu,
                                            final boolean isSetStartLocation,
                                            final boolean isSetEndLocation,
                                            final boolean isAppend) {

      // create actions for each part

      TourLocation tourLocationStart = null;
      TourLocation tourLocationEnd = null;

      if (_allSelectedTours.size() == 1) {

         final TourData firstTour = _allSelectedTours.get(0);

         tourLocationStart = firstTour.getTourLocationStart();
         tourLocationEnd = firstTour.getTourLocationEnd();

      } else {

         // check if locations of all tours are the same

         final TourLocation[] sameLocations = getSameLocations();

         tourLocationStart = sameLocations[0];
         tourLocationEnd = sameLocations[1];
      }

      final boolean isStartLocationAvailable = tourLocationStart != null;
      final boolean isEndLocationAvailable = tourLocationEnd != null;

      Map<LocationPartID, PartItem> allLocationParts = isStartLocationAvailable ? PartItem.getAllPartItems(tourLocationStart, true) : null;
      Map<LocationPartID, PartItem> allEndLocationParts = isEndLocationAvailable ? PartItem.getAllPartItems(tourLocationEnd, false) : null;

      if (isStartLocationAvailable && isEndLocationAvailable) {

         // merge both part item maps into one map

         for (final Entry<LocationPartID, PartItem> entry : allLocationParts.entrySet()) {

            final LocationPartID partID = entry.getKey();
            final PartItem partItem = entry.getValue();

            final PartItem partItemEnd = allEndLocationParts.get(partID);

            if (partItemEnd != null) {

               // set end location
               partItem.partID_End = partItemEnd.partID_End;
               partItem.partLabel_End = partItemEnd.partLabel_End;
               partItem.locationLabel_End = partItemEnd.locationLabel_End;

               allEndLocationParts.remove(partID);
            }
         }

         // add remaining end locations
         allLocationParts.putAll(allEndLocationParts);

         /*
          * Sort by settlement size
          */
         final ArrayList<LocationPartID> allPartIDs = new ArrayList<>(allLocationParts.keySet());
         Collections.sort(allPartIDs);

         final List<PartItem> allSortedLocationParts = new ArrayList<>();

         for (final LocationPartID locationPartID : allPartIDs) {
            allSortedLocationParts.add(allLocationParts.get(locationPartID));
         }

      } else if (isStartLocationAvailable) {

         // all parts are already in allLocationParts

      } else if (isEndLocationAvailable) {

         // move end parts into allLocationParts

         allLocationParts = allEndLocationParts;
         allEndLocationParts = null;
      }

      if (allLocationParts == null) {
         return;
      }

      /*
       * All parts are now in allLocationParts, when available
       */
      String actionText = null;
      String actionTooltip = null;
      boolean isCreateAction = true;

      final Map<String, ActionData_Part> allUnsortedParts = new HashMap<>();

      _usedDisplayNames.clear();

      for (final PartItem partItem : allLocationParts.values()) {

         LocationPartID startPartID = null;
         LocationPartID endPartID = null;

         if (isSetStartLocation && isSetEndLocation) {

            if (isStartLocationAvailable && isEndLocationAvailable) {

               final String locationText = tourLocationStart == tourLocationEnd

                     // both locations are the same, display only one location
                     ? partItem.locationLabel_Start

                     : partItem.locationLabel_Start + LOCATION_SEPARATOR + partItem.locationLabel_End;

               startPartID = partItem.partID_Start;
               endPartID = partItem.partID_End;

               actionText = locationText;
               actionTooltip = partItem.partLabel_Start != null
                     ? partItem.partLabel_Start
                     : partItem.partLabel_End;

            } else {

               // start and end locations are needed but both are not available

               break;
            }

         } else if (isSetStartLocation) {

            if (isStartLocationAvailable) {

               startPartID = partItem.partID_Start;

               actionText = partItem.locationLabel_Start;
               actionTooltip = partItem.partLabel_Start;

            } else {

               isCreateAction = false;
            }

         } else if (isSetEndLocation) {

            if (isEndLocationAvailable) {

               endPartID = partItem.partID_End;

               actionText = partItem.locationLabel_End;
               actionTooltip = partItem.partLabel_End;

            } else {

               isCreateAction = false;
            }

         } else {

            actionText = partItem.partLabel_Start;
            actionTooltip = UI.EMPTY_STRING;
         }

         if (isCreateAction

               && actionText.length() > 0

               // prevent to display duplicated labels
               && _usedDisplayNames.contains(actionText) == false) {

            _usedDisplayNames.add(actionText);

            allUnsortedParts.put(actionText,

                  new ActionData_Part(

                        actionText,
                        actionTooltip,

                        isSetStartLocation,
                        isSetEndLocation,

                        startPartID,
                        endPartID));
         }
      }

      // sort parts by name
      final Map<String, ActionData_Part> allSortedParts = new TreeMap<>(allUnsortedParts);

      for (final ActionData_Part part : allSortedParts.values()) {

         if (isAppend) {

            // append part

            addActionToMenu(menu,

                  new ActionAppendLocationPart(

                        part.actionText,
                        part.actionTooltip,

                        part.isSetStartLocation,
                        part.isSetEndLocation,

                        part.startPartID,
                        part.endPartID));

         } else {

            // replace part

            addActionToMenu(menu,

                  new ActionSetLocationPart(

                        part.actionText,
                        part.actionTooltip,

                        part.isSetStartLocation,
                        part.isSetEndLocation,

                        part.startPartID,
                        part.endPartID));
         }
      }

   }

   /**
    * Fill menu with all profile actions
    *
    * @param menu
    * @param allProfiles
    * @param isSetStartLocation
    * @param isSetEndLocation
    */
   private void fillMenu_AddAll_ProfileActions(final Menu menu,
                                               final List<TourLocationProfile> allProfiles,
                                               final boolean isSetStartLocation,
                                               final boolean isSetEndLocation) {

      final TourLocationProfile defaultProfile = TourLocationManager.getDefaultProfile();

      final Map<String, ActionData_Profile> allUnsortedProfiles = new HashMap<>();

      _usedDisplayNames.clear();

      for (final TourLocationProfile locationProfile : allProfiles) {

         final boolean isDefaultProfile = locationProfile.equals(defaultProfile);

         final String profileName = (isDefaultProfile

               // show a marker for the default profile
               ? UI.SYMBOL_STAR + UI.SPACE

               : UI.EMPTY_STRING)

               + PROFILE_NAME.formatted(locationProfile.getName(), locationProfile.getZoomlevel());

         /*
          * Create part text
          */
         final String partText = Messages.Tour_Location_Action_Profile_Tooltip.formatted(

               TourLocationManager.createJoinedPartNames(locationProfile, UI.NEW_LINE1),
               locationProfile.getZoomlevel());

         final String locationTooltip = profileName + UI.NEW_LINE2 + partText;

         /*
          * Create start/end location text
          */
         TourLocation tourLocationStart = null;
         TourLocation tourLocationEnd = null;

         if (_allSelectedTours.size() == 1) {

            final TourData firstTour = _allSelectedTours.get(0);

            tourLocationStart = firstTour.getTourLocationStart();
            tourLocationEnd = firstTour.getTourLocationEnd();

         } else {

            // check if locations of all tours are the same

            final TourLocation[] sameLocations = getSameLocations();

            tourLocationStart = sameLocations[0];
            tourLocationEnd = sameLocations[1];
         }

         final boolean isStartLocationAvailable = tourLocationStart != null;
         final boolean isEndLocationAvailable = tourLocationEnd != null;

         final String startLocationText = isStartLocationAvailable
               ? TourLocationManager.createLocationDisplayName(tourLocationStart, locationProfile)
               : null;

         final String endLocationText = isEndLocationAvailable
               ? TourLocationManager.createLocationDisplayName(tourLocationEnd, locationProfile)
               : null;

         boolean isShowDefaultLabel = false;

         String actionText = UI.EMPTY_STRING;
         String tooltipText = UI.EMPTY_STRING;

         /*
          * Set action text/tooltip
          */
         if (isSetStartLocation && isSetEndLocation && isStartLocationAvailable && isEndLocationAvailable) {

            final String locationText = tourLocationStart == tourLocationEnd

                  // both locations are the same, display only one location
                  ? startLocationText

                  : startLocationText + LOCATION_SEPARATOR + endLocationText;

            // indent action to be better visible
            actionText = locationText;
            tooltipText = locationTooltip;

         } else if (isSetStartLocation && isSetEndLocation

               && (isStartLocationAvailable || isEndLocationAvailable)) {

            // only one location is available

            final String locationText = tourLocationStart != null

                  ? startLocationText + LOCATION_SEPARATOR + profileName
                  : profileName + LOCATION_SEPARATOR + endLocationText;

            // indent action to be better visible
            actionText = locationText;
            tooltipText = locationTooltip;

         } else if (isSetStartLocation) {

            if (isStartLocationAvailable) {

               actionText = startLocationText;
               tooltipText = locationTooltip;

            } else {

               isShowDefaultLabel = true;
            }

         } else if (isSetEndLocation) {

            if (isEndLocationAvailable) {

               actionText = endLocationText;
               tooltipText = locationTooltip;

            } else {

               isShowDefaultLabel = true;
            }

         } else {

            isShowDefaultLabel = true;
         }

         if (isShowDefaultLabel) {

            // indent action to be better visible
            actionText = profileName;
            tooltipText = partText;
         }

         if (actionText.length() > 0

               // prevent duplicate names
               && _usedDisplayNames.contains(actionText) == false) {

            _usedDisplayNames.add(actionText);

            // indent action text to be better visible
            actionText = UI.SPACE8 + actionText;

            allUnsortedProfiles.put(actionText,

                  new ActionData_Profile(

                        locationProfile,

                        isSetStartLocation,
                        isSetEndLocation,

                        actionText,
                        tooltipText));
         }
      }

      // sort profiles by actionText
      final Map<String, ActionData_Profile> allSortedProfiles = new TreeMap<>(allUnsortedProfiles);

      for (final ActionData_Profile sortItem : allSortedProfiles.values()) {

         addActionToMenu(menu,

               new ActionSetLocation(

                     sortItem.locationProfile,

                     sortItem.isSetStartLocation,
                     sortItem.isSetEndLocation,

                     sortItem.actionText,
                     sortItem.tooltipText));
      }
   }

   private TourLocation[] getSameLocations() {

      TourLocation tourLocationStart = null;
      TourLocation tourLocationEnd = null;

      boolean canSetStartLocation = true;
      boolean canSetEndLocation = true;

      for (final TourData tourData : _allSelectedTours) {

         final TourLocation locationStart = tourData.getTourLocationStart();
         final TourLocation locationEnd = tourData.getTourLocationEnd();

         if (tourLocationStart == null) {

            // location is not yet set

            if (canSetStartLocation) {

               tourLocationStart = locationStart;
            }

         } else {

            // location is already set

            if (locationStart == null) {

               // needs to be location which can be compared

               tourLocationStart = null;

               canSetStartLocation = false;

            } else {

               if (tourLocationStart.getLocationId() == locationStart.getLocationId()) {

                  // it's the same location

               } else {

                  // it's a different location -> there is no common location

                  tourLocationStart = null;

                  canSetStartLocation = false;
               }
            }

         }

         if (tourLocationEnd == null) {

            // location is not yet set

            if (canSetEndLocation) {

               tourLocationEnd = locationEnd;
            }

         } else {

            // location is already set

            if (locationEnd == null) {

               // needs to be location which can be compared

               tourLocationEnd = null;

               canSetEndLocation = false;

            } else {

               if (tourLocationEnd.getLocationId() == locationEnd.getLocationId()) {

                  // it's the same location

               } else {

                  // it's a different location -> there is no common location

                  tourLocationEnd = null;

                  canSetEndLocation = false;
               }
            }

         }
      }

      return new TourLocation[] { tourLocationStart, tourLocationEnd };
   }

   public void setIsStartLocation(final Boolean isStartLocationInContextMenu) {

      _isStartLocationInContextMenu = isStartLocationInContextMenu;
   }

}
