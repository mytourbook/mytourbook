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

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
import net.tourbook.data.TourData;
import net.tourbook.tour.location.ITourLocationConsumer;
import net.tourbook.tour.location.SlideoutLocationProfiles;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

public class ContribItem_DownloadTourLocation extends ContributionItem {

// SET_FORMATTING_OFF

   // start/end needs separate states otherwise the slideout states (window position/location) cannot be differentiated
   private static final IDialogSettings         _state_StartLocation          = TourbookPlugin.getState("net.tourbook.tour.DialogQuickEdit.StartLocation");   //$NON-NLS-1$
   private static final IDialogSettings         _state_EndLocation            = TourbookPlugin.getState("net.tourbook.tour.DialogQuickEdit.EndLocation");     //$NON-NLS-1$

// SET_FORMATTING_ON

   private String                          _tooltip;

   private boolean                         _isStartLocation;
   private boolean                         _hasLocationData;

   private ActionSlideout_LocationProfiles _actionSlideout_LocationProfiles;

   private ITourLocationConsumer           _tourLocationConsumer;

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
   private TourData _tourData;

   private class ActionSlideout_LocationProfiles extends Action {

      private SlideoutLocationProfiles _slideoutLocationOptions;

      public ActionSlideout_LocationProfiles() {

         super("Edit Location &Profiles");
      }

      @Override
      public void run() {

         if (_slideoutLocationOptions == null) {

            _slideoutLocationOptions = new SlideoutLocationProfiles(

                  _tourLocationConsumer,
                  _tourData,
                  _toolbar,
                  _toolItem,
                  _state_StartLocation,
                  true);
         }

         // update slideout for the current location
         _slideoutLocationOptions.setIsStartLocation(_isStartLocation);

         _slideoutLocationOptions.open(false);
      }

   }

   public ContribItem_DownloadTourLocation(final ITourLocationConsumer tourLocation,
                                                final TourData tourData,
                                                final boolean isStartLocation) {

      _tourLocationConsumer = tourLocation;
      _tourData = tourData;

      _isStartLocation = isStartLocation;

      _hasLocationData = isStartLocation
            ? _tourData.osmLocation_Start != null
            : _tourData.osmLocation_End != null;

      createActions();
   }

   private void createActions() {

      _actionSlideout_LocationProfiles = new ActionSlideout_LocationProfiles();
   }

   private void enableActions() {
      // TODO Auto-generated method stub

   }

   @Override
   public void fill(final ToolBar toolbar, final int index) {

      if (_toolItem != null) {
         return;
      }

      _toolbar = toolbar;
      toolbar.addDisposeListener(disposeEvent -> onDispose());

      _contribItemImage_Download = CommonActivator.getThemedImageDescriptor(CommonImages.App_Download).createImage();
      _contribItemImage_Download_Disabled = CommonActivator.getThemedImageDescriptor(CommonImages.App_Download_Disabled).createImage();

      _contribItemImage_Options = CommonActivator.getThemedImageDescriptor(CommonImages.TourOptions).createImage();
      _contribItemImage_Options_Disabled = CommonActivator.getThemedImageDescriptor(CommonImages.TourOptions_Disabled).createImage();

      _toolItem = new ToolItem(toolbar, SWT.PUSH);

      _toolItem.setToolTipText(_tooltip);

      updateUI_ToolItem();

      _toolItem.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onSelect(toolbar)));

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

      menuMgr.add(_actionSlideout_LocationProfiles);

      enableActions();
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

   private void onSelect(final ToolBar toolbar) {

      if (_hasLocationData) {

         // show context menu

         final Rectangle toolItemBounds = _toolItem.getBounds();

         final Point toolItemRelativePos = new Point(
               toolItemBounds.x,
               toolItemBounds.y + toolItemBounds.height);

         final Point toolbarDisplayPos = toolbar.toDisplay(toolItemRelativePos);

         _contextMenu.setLocation(
               toolbarDisplayPos.x,
               toolbarDisplayPos.y);

         _contextMenu.setVisible(true);

      } else {

         // download location data

         BusyIndicator.showWhile(toolbar.getDisplay(), () -> {

            if (_isStartLocation) {

               _tourLocationConsumer.setTourStartLocation();

            } else {

               _tourLocationConsumer.setTourEndLocation();
            }
         });
      }
   }

   public void setEnabled(final boolean isEnabled) {

      _toolItem.setEnabled(isEnabled);

//      _actionDownloadLocation.setEnabled();
   }

   public void setHasLocationData(final boolean hasLocationData) {

      _hasLocationData = hasLocationData;

      updateUI_ToolItem();
   }

   private void updateUI_ToolItem() {

      if (_hasLocationData) {

         // set location data

         _toolItem.setImage(_contribItemImage_Options);
         _toolItem.setDisabledImage(_contribItemImage_Options);

         _toolItem.setToolTipText(_isStartLocation
               ? Messages.Tour_Action_LocationCustomize_Start_Tooltip
               : Messages.Tour_Action_LocationCustomize_End_Tooltip);

      } else {

         // download location data

         _toolItem.setImage(_contribItemImage_Download);
         _toolItem.setDisabledImage(_contribItemImage_Download_Disabled);

         _toolItem.setToolTipText(_isStartLocation
               ? Messages.Tour_Action_LocationDownload_Start_Tooltip
               : Messages.Tour_Action_LocationDownload_End_Tooltip);
      }

   }

}
