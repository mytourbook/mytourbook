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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.ui.SubMenu;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.TourData;
import net.tourbook.data.TourLocation;
import net.tourbook.tour.location.LocationPartID;
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

   private static final String            ID                 = "net.tourbook.ui.action.ActionSetStartEndLocation"; //$NON-NLS-1$

   private static final char              NL                 = UI.NEW_LINE;

   private static final String            LOCATION_SEPARATOR = "     ·     ";                                      //$NON-NLS-1$
   private static final String            PROFILE_NAME       = "%s - %d";                                          //$NON-NLS-1$

   private static final IDialogSettings   _state             = TourbookPlugin.getState(ID);

   private ITourProvider                  _tourProvider;

   private ActionEditProfiles             _actionEditProfiles;
   private Action                         _actionPartTitle_All;
   private Action                         _actionPartTitle_Start;
   private Action                         _actionPartTitle_End;
   private Action                         _actionProfileTitle_All;
   private Action                         _actionProfileTitle_Start;
   private Action                         _actionProfileTitle_End;

   private ActionAppendLocationPart_All   _actionAppendLocationPart_All;
   private ActionAppendLocationPart_Start _actionAppendLocationPart_Start;
   private ActionAppendLocationPart_End   _actionAppendLocationPart_End;
   private ActionRemoveLocation_All       _actionRemoveLocation_All;
   private ActionRemoveLocation_Start     _actionRemoveLocation_Start;
   private ActionRemoveLocation_End       _actionRemoveLocation_End;
   private ActionSetLocation_Start        _actionSetLocation_Start;
   private ActionSetLocation_End          _actionSetLocation_End;

   private Control                        _ownerControl;

   private ArrayList<TourData>            _allSelectedTours;

   /**
    * When <code>null</code> then a start or end location is not hovered
    */
   private Boolean                        _isStartLocationInContextMenu;

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

         TourLocationManager.appendLocationPart(

               _allSelectedTours,

               _partID_Start,
               _partID_End,

               _isSetStartLocation,
               _isSetEndLocation);
      }

   }

   private class ActionAppendLocationPart_All extends SubMenu {

      public ActionAppendLocationPart_All() {

         super(Messages.Tour_Location_Action_AppendLocationPart_All, AS_DROP_DOWN_MENU);
      }

      @Override
      public void enableActions() {}

      @Override
      public void fillMenu(final Menu menu) {

         addActionToMenu(_actionPartTitle_All);

         fillMenu_AddAll_PartActions(menu, true, true);
      }
   }

   private class ActionAppendLocationPart_End extends SubMenu {

      public ActionAppendLocationPart_End() {

         super(Messages.Tour_Location_Action_AppendLocationPart_End, AS_DROP_DOWN_MENU);
      }

      @Override
      public void enableActions() {}

      @Override
      public void fillMenu(final Menu menu) {

         addActionToMenu(_actionPartTitle_End);

         fillMenu_AddAll_PartActions(menu, false, true);
      }
   }

   private class ActionAppendLocationPart_Start extends SubMenu {

      public ActionAppendLocationPart_Start() {

         super(Messages.Tour_Location_Action_AppendLocationPart_Start, AS_DROP_DOWN_MENU);

      }

      @Override
      public void enableActions() {}

      @Override
      public void fillMenu(final Menu menu) {

         addActionToMenu(_actionPartTitle_Start);

         fillMenu_AddAll_PartActions(menu, true, false);
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

   private class ActionLocationProfile extends Action {

      private TourLocationProfile _locationProfile;

      private Boolean             _isSetStartLocation;
      private Boolean             _isSetEndLocation;

      public ActionLocationProfile(final TourLocationProfile locationProfile,
                                   final boolean isDefaultProfile,

                                   final boolean isSetStartLocation,
                                   final boolean isSetEndLocation) {

         super(UI.EMPTY_STRING, AS_PUSH_BUTTON);

         _locationProfile = locationProfile;

         _isSetStartLocation = isSetStartLocation;
         _isSetEndLocation = isSetEndLocation;

         final String profileName = (isDefaultProfile

               // show a marker for the default profile
               ? UI.SYMBOL_STAR + UI.SPACE

               : UI.EMPTY_STRING)

               + PROFILE_NAME.formatted(_locationProfile.getName(), _locationProfile.getZoomlevel());

         setupActionTextAndTooltip_Profile(

               this,

               locationProfile,
               profileName,

               isSetStartLocation,
               isSetEndLocation);
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

   private class PartItem {

      LocationPartID partID_Start;
      LocationPartID partID_End;

      String         partLabel_Start;
      String         partLabel_End;

      String         locationLabel_Start = UI.EMPTY_STRING;
      String         locationLabel_End   = UI.EMPTY_STRING;

      public PartItem(final LocationPartID partID,
                      final String partLabel,

                      final String locationLabel,
                      final boolean isSetStartLocation) {

         if (isSetStartLocation) {

            this.partID_Start = partID;
            this.partLabel_Start = partLabel;

            this.locationLabel_Start = locationLabel;

         } else {

            this.partID_End = partID;
            this.partLabel_End = partLabel;

            this.locationLabel_End = locationLabel;
         }
      }

      @Override
      public String toString() {

         return UI.EMPTY_STRING

               + "PartItem" + NL //                                              //$NON-NLS-1$

               + "   partID_Start         = " + partID_Start + NL //             //$NON-NLS-1$
               + "   partID_End           = " + partID_End + NL //               //$NON-NLS-1$

               + "   partLabel_Start      = " + partLabel_Start + NL //          //$NON-NLS-1$
               + "   partLabel_End        = " + partLabel_End + NL //            //$NON-NLS-1$

               + NL

               + "   locationLabel_Start  = " + locationLabel_Start + NL //      //$NON-NLS-1$
               + "   locationLabel_End    = " + locationLabel_End + NL //        //$NON-NLS-1$

               + NL;
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

      final List<TourData> selectedTours = _tourProvider.getSelectedTours();
      final TourData tourData = selectedTours.get(0);

      // ensure that location data are available
      final TourLocationData tourLocationData = tourData.tourLocationData_Start;
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

            _isStartLocationInContextMenu == null ? true : _isStartLocationInContextMenu);

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

// SET_FORMATTING_OFF

      _actionEditProfiles              = new ActionEditProfiles();

      _actionAppendLocationPart_All    = new ActionAppendLocationPart_All();
      _actionAppendLocationPart_Start  = new ActionAppendLocationPart_Start();
      _actionAppendLocationPart_End    = new ActionAppendLocationPart_End();
      _actionRemoveLocation_All        = new ActionRemoveLocation_All();
      _actionRemoveLocation_Start      = new ActionRemoveLocation_Start();
      _actionRemoveLocation_End        = new ActionRemoveLocation_End();
      _actionSetLocation_Start         = new ActionSetLocation_Start();
      _actionSetLocation_End           = new ActionSetLocation_End();

      // create dummy actions for the part/profile title
      _actionPartTitle_All             = new Action(Messages.Tour_Location_Action_PartTitle_All) {};
      _actionPartTitle_Start           = new Action(Messages.Tour_Location_Action_PartTitle_Start) {};
      _actionPartTitle_End             = new Action(Messages.Tour_Location_Action_PartTitle_End) {};
      _actionProfileTitle_All          = new Action(Messages.Tour_Location_Action_ProfileTitle_All) {};
      _actionProfileTitle_Start        = new Action(Messages.Tour_Location_Action_ProfileTitle_Start) {};
      _actionProfileTitle_End          = new Action(Messages.Tour_Location_Action_ProfileTitle_End) {};

      _actionPartTitle_All             .setEnabled(false);
      _actionPartTitle_Start           .setEnabled(false);
      _actionPartTitle_End             .setEnabled(false);
      _actionProfileTitle_All          .setEnabled(false);
      _actionProfileTitle_Start        .setEnabled(false);
      _actionProfileTitle_End          .setEnabled(false);

// SET_FORMATTING_ON
   }

   @Override
   public void enableActions() {}

   @Override
   public void fillMenu(final Menu menu) {

      // get tours which are needed in the menu actions
      _allSelectedTours = _tourProvider.getSelectedTours();

      addActionToMenu(_actionAppendLocationPart_Start);
      addActionToMenu(_actionAppendLocationPart_End);
      addActionToMenu(_actionAppendLocationPart_All);

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

      addActionToMenu(_actionRemoveLocation_All);
      addActionToMenu(_actionRemoveLocation_Start);
      addActionToMenu(_actionRemoveLocation_End);

      addSeparatorToMenu();

      addActionToMenu(_actionEditProfiles);
   }

   private void fillMenu_AddAll_PartActions(final Menu menu,
                                            final boolean isSetStartLocation,
                                            final boolean isSetEndLocation) {

      // create actions for each part

      TourLocation tourLocationStart = null;
      TourLocation tourLocationEnd = null;

      if (_allSelectedTours.size() == 1) {

         final TourData tourData = _allSelectedTours.get(0);

         tourLocationStart = tourData.getTourLocationStart();
         tourLocationEnd = tourData.getTourLocationEnd();
      }

      final boolean isStartLocationAvailable = tourLocationStart != null;
      final boolean isEndLocationAvailable = tourLocationEnd != null;

      Map<LocationPartID, PartItem> allLocationParts = isStartLocationAvailable ? getAllPartItems(tourLocationStart, true) : null;
      Map<LocationPartID, PartItem> allEndLocationParts = isEndLocationAvailable ? getAllPartItems(tourLocationEnd, false) : null;

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

      for (final Entry<LocationPartID, PartItem> entry : allLocationParts.entrySet()) {

         final PartItem partItem = entry.getValue();

         LocationPartID startPartID = null;
         LocationPartID endPartID = null;

         if (isSetStartLocation && isSetEndLocation && isStartLocationAvailable && isEndLocationAvailable) {

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

         if (isCreateAction) {

            addActionToMenu(menu,

                  new ActionAppendLocationPart(

                        actionText,
                        actionTooltip,

                        isSetStartLocation,
                        isSetEndLocation,

                        startPartID,
                        endPartID));
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

   private Map<LocationPartID, PartItem> getAllPartItems(final TourLocation tourLocation, final boolean isSetStartLocation) {

      final Map<LocationPartID, PartItem> allPartItems = new LinkedHashMap<>();

      try {

         final Field[] allAddressFields = tourLocation.getClass().getFields();

         // loop: all fields in the retrieved address
         for (final Field field : allAddressFields) {

            final String fieldName = field.getName();

            // skip field names which are not address parts
            if (TourLocation.IGNORED_FIELDS.contains(fieldName)) {
               continue;
            }

            final Object fieldValue = field.get(tourLocation);

            if (fieldValue instanceof final String stringValue) {

               // use only fields with a value
               if (stringValue.length() > 0) {

                  final LocationPartID partID = LocationPartID.valueOf(fieldName);
                  final String partLabel = TourLocationManager.ALL_LOCATION_PART_AND_LABEL.get(partID);

                  allPartItems.put(partID, new PartItem(partID, partLabel, stringValue, isSetStartLocation));
               }
            }
         }

      } catch (IllegalArgumentException | IllegalAccessException e) {
         StatusUtil.showStatus(e);
      }

      return allPartItems;
   }

   public void setIsStartLocation(final Boolean isStartLocationInContextMenu) {

      _isStartLocationInContextMenu = isStartLocationInContextMenu;
   }

   private void setupActionTextAndTooltip_Profile(final Action action,
                                                  final TourLocationProfile locationProfile,
                                                  final String profileName,
                                                  final boolean isSetStartLocation,
                                                  final boolean isSetEndLocation) {
      /*
       * Get part text
       */
      final String partText = Messages.Tour_Location_Action_Profile_Tooltip.formatted(

            TourLocationManager.createJoinedPartNames(locationProfile, UI.NEW_LINE1),
            locationProfile.getZoomlevel());

      final String locationTooltip = profileName + UI.NEW_LINE2 + partText;

      /*
       * Get start/end location text
       */
      TourLocation tourLocationStart = null;
      TourLocation tourLocationEnd = null;

      if (_allSelectedTours.size() == 1) {

         final TourData firstTour = _allSelectedTours.get(0);

         tourLocationStart = firstTour.getTourLocationStart();
         tourLocationEnd = firstTour.getTourLocationEnd();
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

      /*
       * Set action text/tooltip
       */
      if (isSetStartLocation && isSetEndLocation && isStartLocationAvailable && isEndLocationAvailable) {

         final String locationText = tourLocationStart == tourLocationEnd

               // both locations are the same, display only one location
               ? startLocationText

               : startLocationText + LOCATION_SEPARATOR + endLocationText;

         // indent action to be better visible
         action.setText(UI.SPACE8 + locationText);
         action.setToolTipText(locationTooltip);

      } else if (isSetStartLocation) {

         if (isStartLocationAvailable) {

            action.setText(startLocationText);
            action.setToolTipText(locationTooltip);

         } else {

            isShowDefaultLabel = true;
         }

      } else if (isSetEndLocation) {

         if (isEndLocationAvailable) {

            action.setText(endLocationText);
            action.setToolTipText(locationTooltip);

         } else {

            isShowDefaultLabel = true;
         }

      } else {

         isShowDefaultLabel = true;
      }

      if (isShowDefaultLabel) {

         // indent action to be better visible
         action.setText(UI.SPACE8 + profileName);
         action.setToolTipText(partText);
      }
   }

}
