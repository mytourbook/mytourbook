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
package net.tourbook.tour;

import java.util.Collections;
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
import net.tourbook.common.UI;
import net.tourbook.data.TourData;
import net.tourbook.tour.location.ITourLocationConsumer;
import net.tourbook.tour.location.OSMLocation;
import net.tourbook.tour.location.SlideoutLocationProfiles;
import net.tourbook.tour.location.TourLocationManager;
import net.tourbook.tour.location.TourLocationProfile;

import org.eclipse.jface.action.Action;
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
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * Action to download and set tour locations
 */
public class ActionTourLocation extends ContributionItem {

   private static IDialogSettings         _state;

   private boolean                        _isStartLocation;
   private boolean                        _hasLocationData;

   private ActionSlideoutLocationProfiles _actionSlideoutLocationProfiles;
   private SlideoutLocationProfiles       _slideoutLocationProfiles;

   private ITourLocationConsumer          _tourLocationConsumer;

   private TourData                       _tourData;

   /*
    * UI controls
    */
   private Image    _contribItemImage_Download;
   private Image    _contribItemImage_Download_Disabled;
   private Image    _contribItemImage_Options;
   private Image    _contribItemImage_Options_Disabled;

   private Menu     _contextMenu;

   private ToolBar  _toolbar;
   private ToolItem _toolItem;

   private class ActionLocationProfile extends Action {

      private TourLocationProfile _locationProfile;

      public ActionLocationProfile(final TourLocationProfile locationProfile, final boolean isDefaultProfile) {

         super(UI.EMPTY_STRING, AS_CHECK_BOX);

         _locationProfile = locationProfile;

         final String profileName = _locationProfile.getName();
         final String locationName = createProfileDisplayName(_locationProfile);

         setText(locationName);
         setToolTipText(Messages.Tour_Location_Action_Profile_Tooltip.formatted(profileName));

         if (isDefaultProfile) {
            setChecked(true);
         }
      }

      @Override
      public void run() {

         onSelectProfile(_locationProfile);
      }
   }

   private class ActionSlideoutLocationProfiles extends Action {

      public ActionSlideoutLocationProfiles() {

         super(Messages.Tour_Location_Action_OpenProfileEditor);
      }

      @Override
      public void run() {

         openProfileSlideout();
      }
   }

   /**
    * @param tourLocationConsumer
    * @param tourData
    * @param isStartLocation
    * @param stateId
    */
   public ActionTourLocation(final ITourLocationConsumer tourLocationConsumer,
                             final TourData tourData,
                             final boolean isStartLocation,
                             final String stateId) {

      _tourLocationConsumer = tourLocationConsumer;
      _tourData = tourData;

      _isStartLocation = isStartLocation;

      _hasLocationData = isStartLocation
            ? _tourData.tourLocationData_Start != null
            : _tourData.tourLocationData_End != null;

      _state = isStartLocation
            ? TourbookPlugin.getState(stateId + ".StartLocation") //$NON-NLS-1$
            : TourbookPlugin.getState(stateId + ".EndLocation"); //$NON-NLS-1$

      createActions();
   }

   public void closeSlideout() {

      if (_slideoutLocationProfiles != null) {

         _slideoutLocationProfiles.close();
      }
   }

   private void createActions() {

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

         return _isStartLocation
               ? Messages.Tour_Location_Action_Download_WithProfile_Start_Tooltip.formatted(profileName)
               : Messages.Tour_Location_Action_Download_WithProfile_End_Tooltip.formatted(profileName);
      }
   }

   private String createProfileDisplayName(final TourLocationProfile locationProfile) {

      final OSMLocation osmLocation = _isStartLocation
            ? _tourData.tourLocationData_Start.osmLocation
            : _tourData.tourLocationData_End.osmLocation;

      return TourLocationManager.createLocationDisplayName(osmLocation, locationProfile);
   }

   @Override
   public void fill(final ToolBar toolbar, final int index) {

      if (_toolItem != null) {
         return;
      }

      _toolbar = toolbar;
      toolbar.addDisposeListener(disposeEvent -> onDispose());

// SET_FORMATTING_OFF

      _contribItemImage_Download          = CommonActivator.getThemedImageDescriptor(CommonImages.App_Download).createImage();
      _contribItemImage_Download_Disabled = CommonActivator.getThemedImageDescriptor(CommonImages.App_Download_Disabled).createImage();

      _contribItemImage_Options           = CommonActivator.getThemedImageDescriptor(CommonImages.TourOptions).createImage();
      _contribItemImage_Options_Disabled  = CommonActivator.getThemedImageDescriptor(CommonImages.TourOptions_Disabled).createImage();

// SET_FORMATTING_ON

      _toolItem = new ToolItem(toolbar, SWT.PUSH);

      updateUI_ToolItem();

      _toolItem.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onSelect(selectionEvent, toolbar)));

      /*
       * Context menu
       */
      final MenuManager menuMgr = new MenuManager();

      menuMgr.setRemoveAllWhenShown(true);
      menuMgr.addMenuListener(manager -> fillContextMenu(menuMgr));

      // set context menu
      _contextMenu = menuMgr.createContextMenu(toolbar);
   }

   private void fillContextMenu(final IMenuManager menuMgr) {

      final TourLocationProfile defaultProfile = TourLocationManager.getDefaultProfile();
      final List<TourLocationProfile> allProfiles = TourLocationManager.getProfiles();

      // sort profiles by name
      Collections.sort(allProfiles);

      // create actions for each profile
      if (allProfiles.size() > 0) {

         for (final TourLocationProfile locationProfile : allProfiles) {

            final boolean isDefaultProfile = locationProfile.equals(defaultProfile);

            menuMgr.add(new ActionLocationProfile(locationProfile, isDefaultProfile));
         }

         menuMgr.add(new Separator());
      }

      menuMgr.add(_actionSlideoutLocationProfiles);
   }

   private void onDispose() {

      _contribItemImage_Download.dispose();
      _contribItemImage_Download_Disabled.dispose();
      _contribItemImage_Options.dispose();
      _contribItemImage_Options_Disabled.dispose();

      _toolItem.dispose();
      _toolItem = null;

      _contextMenu.dispose();
   }

   /**
    * @param selectionEvent
    * @param toolbar
    */
   private void onSelect(final SelectionEvent selectionEvent, final ToolBar toolbar) {

      // there are 3 different actions

      if (_hasLocationData) {

         if (UI.isCtrlKey(selectionEvent)) {

            // open profile editor

            openProfileSlideout();

         } else {

            // show context menu

            final Rectangle itemBounds = _toolItem.getBounds();

            final Point relativePos = new Point(itemBounds.x, itemBounds.y + itemBounds.height);
            final Point displayPos = toolbar.toDisplay(relativePos);

            _contextMenu.setLocation(displayPos.x, displayPos.y);
            _contextMenu.setVisible(true);
         }

      } else {

         // download location data

         BusyIndicator.showWhile(toolbar.getDisplay(), () -> {

            if (_isStartLocation) {

               _tourLocationConsumer.setupTourStartLocation();

            } else {

               _tourLocationConsumer.setupTourEndLocation();
            }
         });
      }
   }

   private void onSelectProfile(final TourLocationProfile locationProfile) {

      final String displayName = createProfileDisplayName(locationProfile);

      if (_isStartLocation) {

         _tourLocationConsumer.setTourStartLocation(displayName);

      } else {

         _tourLocationConsumer.setTourEndLocation(displayName);
      }
   }

   private void openProfileSlideout() {

      if (_slideoutLocationProfiles == null) {

         // !!! must be created lately otherwise the UI is not fully setup !!!

         _slideoutLocationProfiles = new SlideoutLocationProfiles(

               _tourLocationConsumer,
               _tourData,
               _toolbar,
               _toolItem,
               _state,
               _isStartLocation);
      }

      /*
       * Close other location slideout that only one slideout is open, otherwise they can
       * conflict because they are using the same model
       */
      _tourLocationConsumer.closeOtherSlideouts(ActionTourLocation.this);

      _slideoutLocationProfiles.open(false);
   }

   public void setEnabled(final boolean isEnabled) {

      _toolItem.setEnabled(isEnabled);

//      _actionDownloadLocation.setEnabled();
   }

   public void setHasLocationData(final boolean hasLocationData) {

      _hasLocationData = hasLocationData;

      updateUI_ToolItem();
   }

   void updateUI_ToolItem() {

      if (_hasLocationData) {

         // set location data

         _toolItem.setImage(_contribItemImage_Options);
         _toolItem.setDisabledImage(_contribItemImage_Options);

         _toolItem.setToolTipText(_isStartLocation
               ? Messages.Tour_Location_Action_Customize_Start_Tooltip
               : Messages.Tour_Location_Action_Customize_End_Tooltip);

      } else {

         // download location data

         _toolItem.setImage(_contribItemImage_Download);
         _toolItem.setDisabledImage(_contribItemImage_Download_Disabled);

         _toolItem.setToolTipText(createDownloadTooltip());
      }

   }

}
