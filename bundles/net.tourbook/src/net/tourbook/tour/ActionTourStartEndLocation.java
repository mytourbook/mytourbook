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
package net.tourbook.tour;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
import net.tourbook.common.UI;
import net.tourbook.common.ui.SubMenu;
import net.tourbook.data.TourData;
import net.tourbook.data.TourLocation;
import net.tourbook.tour.location.ITourLocationConsumer;
import net.tourbook.tour.location.LocationPartID;
import net.tourbook.tour.location.PartItem;
import net.tourbook.tour.location.SlideoutStartEndLocationProfiles;
import net.tourbook.tour.location.TourLocationManager;
import net.tourbook.tour.location.TourLocationProfile;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * Action to download and set tour locations
 */
public class ActionTourStartEndLocation extends ContributionItem {

   private static IDialogSettings         _state;

   /**
    * This set is used to prevent duplicated action names
    */
   private static final Set<String>       _usedDisplayNames = new HashSet<>();
   //
   private boolean                        _isStartLocation;
   private boolean                        _hasLocationData;
   //
   private Action                         _actionProfileTitle;
   private ActionLocationPart             _actionLocationPart;
   private ActionSlideoutLocationProfiles _actionSlideoutLocationProfiles;
   //
   private SlideoutStartEndLocationProfiles       _slideoutLocationProfiles;
   //
   private ITourLocationConsumer          _tourLocationConsumer;
   //
   private TourData                       _tourData;
   //
   /*
    * UI controls
    */
   private Image    _actionImage_Download;
   private Image    _actionImage_Download_Disabled;
   private Image    _actionImage_Options;
   private Image    _actionImage_Options_Disabled;
   //
   private Menu     _contextMenu;
   //
   private ToolBar  _toolbar;
   //
   private ToolItem _toolItem;

   public class ActionData {

      TourLocationProfile locationProfile;
      String              locationName;
      boolean             isDefaultProfile;

      public ActionData(final TourLocationProfile locationProfile,
                        final String locationName,
                        final boolean isDefaultProfile) {

         this.locationProfile = locationProfile;
         this.locationName = locationName;
         this.isDefaultProfile = isDefaultProfile;
      }

   }

   private class ActionLocationPart extends SubMenu {

      public ActionLocationPart() {

         super(Messages.Tour_Location_Action_SetLocationPart, AS_DROP_DOWN_MENU);
      }

      @Override
      public void enableActions() {}

      @Override
      public void fillMenu(final Menu menu) {

         fillSubMenu_AllPartActions(menu);
      }
   }

   private class ActionLocationProfile extends Action {

      private TourLocationProfile _locationProfile;

      public ActionLocationProfile(final TourLocationProfile locationProfile,
                                   String locationText,
                                   final boolean isDefaultProfile) {

         super(UI.EMPTY_STRING, AS_PUSH_BUTTON);

         _locationProfile = locationProfile;

         final String joinedPartNames = TourLocationManager.createJoinedPartNames(locationProfile, UI.NEW_LINE1);

         if (isDefaultProfile) {

            locationText = UI.SYMBOL_STAR + UI.SPACE + locationText;
         }

         // indent action label to be better visible
         locationText = UI.SPACE8 + locationText;

         setText(locationText);
         setToolTipText(Messages.Tour_Location_Action_Profile_Tooltip.formatted(
               joinedPartNames,
               locationProfile.getZoomlevel()));

      }

      @Override
      public void run() {

         actionSetTourLocation(_locationProfile);
      }
   }

   private class ActionSetLocationPart extends Action {

      private String _partLabel;

      public ActionSetLocationPart(final String partLabel,
                                   final String actionTooltip) {

         super(partLabel, AS_PUSH_BUTTON);

         _partLabel = partLabel;

         setToolTipText(actionTooltip);
      }

      @Override
      public void run() {

         actionSetTourLocation(_partLabel);
      }
   }

   private class ActionSlideoutLocationProfiles extends Action {

      public ActionSlideoutLocationProfiles() {

         super(Messages.Tour_Location_Action_OpenProfileEditor);
      }

      @Override
      public void run() {

         actionOpenProfileSlideout();
      }
   }

   /**
    * @param tourLocationConsumer
    * @param tourData
    * @param isStartLocation
    * @param stateId
    */
   public ActionTourStartEndLocation(final ITourLocationConsumer tourLocationConsumer,
                             final boolean isStartLocation,
                             final String stateId) {

      _tourLocationConsumer = tourLocationConsumer;

      _isStartLocation = isStartLocation;

      _state = isStartLocation
            ? TourbookPlugin.getState(stateId + ".StartLocation") //$NON-NLS-1$
            : TourbookPlugin.getState(stateId + ".EndLocation"); //$NON-NLS-1$

      createActions();
   }

   private void actionOpenProfileSlideout() {

      if (_slideoutLocationProfiles == null) {

         // !!! must be created lately otherwise the UI is not fully setup !!!

         final Rectangle itemBounds = _toolItem.getBounds();
         final Point itemDisplayPosition = _toolbar.toDisplay(itemBounds.x, itemBounds.y);

         itemBounds.x = itemDisplayPosition.x;
         itemBounds.y = itemDisplayPosition.y;

         final TourLocation tourLocation = _isStartLocation
               ? _tourData.getTourLocationStart()
               : _tourData.getTourLocationEnd();

         _slideoutLocationProfiles = new SlideoutStartEndLocationProfiles(

               _tourLocationConsumer,
               tourLocation,

               _toolbar,
               itemBounds,
               _state,
               _isStartLocation);
      }

      /*
       * Close other location slideout that only one slideout is open, otherwise they can
       * conflict because they are using the same model
       */
      _tourLocationConsumer.closeOtherSlideouts(ActionTourStartEndLocation.this);

      _slideoutLocationProfiles.open(false);
   }

   private void actionSetTourLocation(final String locationLabel) {

      if (_isStartLocation) {

         _tourLocationConsumer.setTourStartLocation(locationLabel);

      } else {

         _tourLocationConsumer.setTourEndLocation(locationLabel);
      }
   }

   /**
    * Set location which was created with the provided profile
    *
    * @param locationProfile
    */
   private void actionSetTourLocation(final TourLocationProfile locationProfile) {

      final String displayName = createProfileDisplayName(locationProfile);

      if (_isStartLocation) {

         _tourLocationConsumer.setTourStartLocation(displayName);

      } else {

         _tourLocationConsumer.setTourEndLocation(displayName);
      }
   }

   /**
    * @param selectionEvent
    * @param toolbar
    */
   private void actionTourLocation(final SelectionEvent selectionEvent, final ToolBar toolbar) {

      // there are 3 different actions

      if (_hasLocationData) {

         if (UI.isCtrlKey(selectionEvent)) {

            // open profile editor

            actionOpenProfileSlideout();

         } else {

            // show context menu

            final Rectangle itemBounds = _toolItem.getBounds();

            final Point relativePos = new Point(itemBounds.x, itemBounds.y + itemBounds.height);
            final Point displayPos = toolbar.toDisplay(relativePos);

            _contextMenu.setLocation(displayPos.x, displayPos.y);
            _contextMenu.setVisible(true);
         }

      } else {

         // location data are not yet available -> download location data

         downloadAndSetLocationData();
      }
   }

   /**
    * Add an action to the provided menu
    *
    * @param menu
    * @param action
    */
   private void addActionToMenu(final Menu menu, final Action action) {

      new ActionContributionItem(action).fill(menu, -1);
   }

   public void closeSlideout() {

      if (_slideoutLocationProfiles != null) {

         _slideoutLocationProfiles.close();
      }
   }

   private void createActions() {

      _actionProfileTitle = new Action(Messages.Tour_Location_Action_ProfileTitle_All) {};
      _actionProfileTitle.setEnabled(false);

      _actionLocationPart = new ActionLocationPart();

      _actionSlideoutLocationProfiles = new ActionSlideoutLocationProfiles();
   }

   private String createDownloadTooltip() {

      final TourLocationProfile defaultProfile = TourLocationManager.getDefaultProfile();

      if (defaultProfile == null) {

         return _isStartLocation
               ? Messages.Tour_Location_Action_Download_Start_Tooltip
               : Messages.Tour_Location_Action_Download_End_Tooltip;

      } else {

         final String profileName = defaultProfile.getName();
         final String joinedPartNames = TourLocationManager.createJoinedPartNames(defaultProfile, UI.NEW_LINE1);

         return _isStartLocation
               ? Messages.Tour_Location_Action_Download_WithProfile_Start_Tooltip.formatted(profileName, joinedPartNames)
               : Messages.Tour_Location_Action_Download_WithProfile_End_Tooltip.formatted(profileName, joinedPartNames);
      }
   }

   private String createProfileDisplayName(final TourLocationProfile locationProfile) {

      final TourLocation osmLocation = _isStartLocation
            ? _tourData.getTourLocationStart()
            : _tourData.getTourLocationEnd();

      return TourLocationManager.createLocationDisplayName(osmLocation, locationProfile);
   }

   private void downloadAndSetLocationData() {

      BusyIndicator.showWhile(Display.getDefault(), () -> {

         if (_isStartLocation) {

            _tourLocationConsumer.downloadAndSetTourStartLocation();

         } else {

            _tourLocationConsumer.downloadAndSetTourEndLocation();
         }
      });
   }

   @Override
   public void fill(final ToolBar toolbar, final int index) {

      if (_toolItem != null) {
         return;
      }

      _toolbar = toolbar;
      toolbar.addDisposeListener(disposeEvent -> onDispose());

// SET_FORMATTING_OFF

      _actionImage_Download          = CommonActivator.getThemedImageDescriptor(CommonImages.App_Download).createImage();
      _actionImage_Download_Disabled = CommonActivator.getThemedImageDescriptor(CommonImages.App_Download_Disabled).createImage();

      _actionImage_Options           = CommonActivator.getThemedImageDescriptor(CommonImages.TourOptions).createImage();
      _actionImage_Options_Disabled  = CommonActivator.getThemedImageDescriptor(CommonImages.TourOptions_Disabled).createImage();

// SET_FORMATTING_ON

      _toolItem = new ToolItem(toolbar, SWT.PUSH);

      updateUI_ToolItem();

      _toolItem.addSelectionListener(SelectionListener.widgetSelectedAdapter(
            selectionEvent -> actionTourLocation(selectionEvent, toolbar)));

      /*
       * Context menu
       */
      final MenuManager menuMgr = new MenuManager();

      menuMgr.setRemoveAllWhenShown(true);
      menuMgr.addMenuListener(manager -> fillMenu(menuMgr));

      // set context menu
      _contextMenu = menuMgr.createContextMenu(toolbar);
   }

   private void fillMenu(final IMenuManager menuMgr) {

      menuMgr.add(_actionLocationPart);

      fillMenu_AllProfileActions(menuMgr);

      menuMgr.add(new Separator());
      menuMgr.add(_actionSlideoutLocationProfiles);
   }

   /**
    * Create an action for each profile
    *
    * @param menuMgr
    */
   private void fillMenu_AllProfileActions(final IMenuManager menuMgr) {

      final List<TourLocationProfile> allProfiles = TourLocationManager.getProfiles();
      final int numProfiles = allProfiles.size();

      if (numProfiles == 0) {
         return;
      }

      final TourLocationProfile defaultProfile = TourLocationManager.getDefaultProfile();

      // sort profiles by name
      Collections.sort(allProfiles);

      menuMgr.add(_actionProfileTitle);

      fillMenu_AllProfileActions(menuMgr, allProfiles, defaultProfile);
   }

   private void fillMenu_AllProfileActions(final IMenuManager menuMgr,
                                           final List<TourLocationProfile> allProfiles,
                                           final TourLocationProfile defaultProfile) {

      final Map<String, ActionData> allUnsortedActions = new HashMap<>();

      _usedDisplayNames.clear();

      // create actions for each profile
      for (final TourLocationProfile locationProfile : allProfiles) {

         final boolean isDefaultProfile = locationProfile.equals(defaultProfile);

         final String locationName = createProfileDisplayName(locationProfile);

         // skip empty names
         if (locationName.length() > 0

               // skip duplicate names
               && _usedDisplayNames.contains(locationName) == false) {

            _usedDisplayNames.add(locationName);

            allUnsortedActions.put(locationName, new ActionData(locationProfile, locationName, isDefaultProfile));
         }
      }

      // sort actions by name
      final Map<String, ActionData> allSortedActions = new TreeMap<>(allUnsortedActions);

      for (final ActionData actionData : allSortedActions.values()) {

         menuMgr.add(new ActionLocationProfile(

               actionData.locationProfile,
               actionData.locationName,
               actionData.isDefaultProfile));
      }
   }

   private void fillSubMenu_AllPartActions(final Menu menu) {

      // create actions for each part

      TourLocation tourLocationStart = null;
      TourLocation tourLocationEnd = null;

      tourLocationStart = _tourData.getTourLocationStart();
      tourLocationEnd = _tourData.getTourLocationEnd();

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
      String locationLabel = null;
      String actionTooltip = null;
      boolean isCreateAction = true;

      _usedDisplayNames.clear();

      for (final Entry<LocationPartID, PartItem> entry : allLocationParts.entrySet()) {

         final PartItem partItem = entry.getValue();

         if (_isStartLocation) {

            if (isStartLocationAvailable) {

               locationLabel = partItem.locationLabel_Start;
               actionTooltip = partItem.partLabel_Start;

            } else {

               isCreateAction = false;
            }

         } else {

            if (isEndLocationAvailable) {

               locationLabel = partItem.locationLabel_End;
               actionTooltip = partItem.partLabel_End;

            } else {

               isCreateAction = false;
            }
         }

         if (isCreateAction

               // skip empty names
               && locationLabel.length() > 0

               // skip duplicate names
               && _usedDisplayNames.contains(locationLabel) == false) {

            _usedDisplayNames.add(locationLabel);

            addActionToMenu(menu, new ActionSetLocationPart(locationLabel, actionTooltip));
         }
      }
   }

   private void onDispose() {

      _actionImage_Download.dispose();
      _actionImage_Download_Disabled.dispose();
      _actionImage_Options.dispose();
      _actionImage_Options_Disabled.dispose();

      _toolItem.dispose();
      _toolItem = null;

      _contextMenu.dispose();
   }

   public void setEnabled(final boolean isEnabled) {

      if (_toolItem == null) {
         return;
      }

      _toolItem.setEnabled(isEnabled);
   }

   public void setHasLocationData(final boolean hasLocationData) {

      _hasLocationData = hasLocationData;

      updateUI_ToolItem();
   }

   public void setupTourData(final TourData tourData) {

      _tourData = tourData;

      _hasLocationData = _isStartLocation
            ? _tourData.tourLocationData_Start != null
            : _tourData.tourLocationData_End != null;
   }

   public void updateUI_ToolItem() {

      if (_hasLocationData) {

         // set location data

         _toolItem.setImage(_actionImage_Options);
         _toolItem.setDisabledImage(_actionImage_Options_Disabled);

         _toolItem.setToolTipText(_isStartLocation
               ? Messages.Tour_Location_Action_Customize_Start_Tooltip
               : Messages.Tour_Location_Action_Customize_End_Tooltip);

      } else {

         // download location data

         _toolItem.setImage(_actionImage_Download);
         _toolItem.setDisabledImage(_actionImage_Download_Disabled);

         _toolItem.setToolTipText(createDownloadTooltip());
      }

   }

}
