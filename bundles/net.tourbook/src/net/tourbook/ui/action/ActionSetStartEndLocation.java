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

import java.util.Collections;
import java.util.List;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.ui.SubMenu;
import net.tourbook.data.TourData;
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
 * Actions to set start/end location
 *
 * <pre>
 *
 * -- Set start/end location
 *
 *   -- >> Profile <<
 *   ----------------------
 *   -- Profile 1
 *   -- Profile 2
 *   -- Profile 3
 *   ----------------------
 *   -- Open Profile Editor
 *
 * </pre>
 */
public class ActionSetStartEndLocation extends SubMenu {

   private static final String          ID     = "net.tourbook.ui.action.ActionSetStartEndLocation"; //$NON-NLS-1$

   private static final IDialogSettings _state = TourbookPlugin.getState(ID);

   private ITourProvider                _tourProvider;

   private ActionEditProfiles           _actionEditProfiles;
   private Action                       _actionProfileTitle;

   private Control                      _ownerControl;

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

      public ActionLocationProfile(final TourLocationProfile locationProfile, final boolean isDefaultProfile) {

         super(UI.EMPTY_STRING, AS_CHECK_BOX);

         _locationProfile = locationProfile;

         final String profileName = _locationProfile.getName();
         final String joinedPartNames = TourLocationManager.createJoinedPartNames(locationProfile, UI.NEW_LINE1);

         setText(profileName);
         setToolTipText(Messages.Tour_Location_Action_Profile_Tooltip.formatted(joinedPartNames));

         if (isDefaultProfile) {
            setChecked(true);
         }
      }

      @Override
      public void run() {

         actionSetTourLocation(_locationProfile);
      }
   }

   /**
    * @param tourProvider
    * @param ownerControl
    */
   public ActionSetStartEndLocation(final ITourProvider tourProvider,
                                    final Control ownerControl) {

      super(Messages.Tour_Action_SetStartEndLocation, AS_DROP_DOWN_MENU);

      setToolTipText(Messages.Tour_Action_SetStartEndLocation_Tooltip);
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
               longitudeSerie[0]);

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

   private void actionSetTourLocation(final TourLocationProfile locationProfile) {

      final List<TourData> selectedTours = _tourProvider.getSelectedTours();

      TourLocationManager.setTourLocations(selectedTours, locationProfile);


//      System.out.println();
//      System.out.println("       "
//
//            + "Date  "
//
////          + "TourData  "
//            + "SerieData      "
//
//            + "Start        "
//            + "End    "
//
//            + "%"
//
//      );
//
//      for (final TourData tourData : selectedTours) {
//
//         final TourDateTime tourDateTime = tourData.getTourDateTime();
//
//         final int sizeofSerieData = Util.sizeof(tourData.getSerieData());
//         final int sizeofStart = Util.sizeof(tourData.tourLocationData_Start.osmLocation);
//         final int sizeofEnd = Util.sizeof(tourData.tourLocationData_End.osmLocation);
//
//         final float percent = (float) (sizeofStart + sizeofEnd) / sizeofSerieData * 100f;
//
//         System.out.println(""
//
//               + " %8s".formatted(TimeTools.Formatter_Date_S.format(tourDateTime.tourZonedDateTime))
//
////             + " %10s".formatted(FormatManager.formatNumber_0(Util.sizeof(tourData)))
//               + " %10s".formatted(FormatManager.formatNumber_0(sizeofSerieData))
//
//               + " %10s".formatted(sizeofStart)
//               + " %10s".formatted(sizeofEnd)
//
//               + " %4.1f".formatted(percent)
//
//         );
//// TODO remove SYSTEM.OUT.PRINTLN
//
//      }
   }

   private void createActions() {

      _actionProfileTitle = new Action(Messages.Tour_Location_Action_ProfileTitle) {};
      _actionProfileTitle.setEnabled(false);

      _actionEditProfiles = new ActionEditProfiles();
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

         addActionToMenu(_actionProfileTitle);
         addSeparatorToMenu();

         fillMenu_AddAllProfileActions(allProfiles);

         addSeparatorToMenu();
      }

      addActionToMenu(_actionEditProfiles);
   }

   private void fillMenu_AddAllProfileActions(final List<TourLocationProfile> allProfiles) {

      final TourLocationProfile defaultProfile = TourLocationManager.getDefaultProfile();

      // sort profiles by name
      Collections.sort(allProfiles);

      for (final TourLocationProfile locationProfile : allProfiles) {

         final boolean isDefaultProfile = locationProfile.equals(defaultProfile);

         addActionToMenu(new ActionLocationProfile(locationProfile, isDefaultProfile));
      }
   }

}
