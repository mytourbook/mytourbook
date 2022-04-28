/*******************************************************************************
 * Copyright (C) 2021, 2022 Frédéric Bard
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
package net.tourbook.ui.views.rawData;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import de.byteholder.geoclipse.map.UI;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
import net.tourbook.common.util.ITourViewer3;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.database.IComputeTourValues;
import net.tourbook.database.TourDatabase;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.importdata.RawDataManager.TourValueType;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourLogManager;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class DialogDeleteTourValues extends TitleAreaDialog {

   private static final String          STATE_DELETE_TOURVALUES_BETWEEN_DATES_FROM  = "STATE_DELETE_TOURVALUES_BETWEEN_DATES_FROM";      //$NON-NLS-1$
   private static final String          STATE_DELETE_TOURVALUES_BETWEEN_DATES_UNTIL = "STATE_DELETE_TOURVALUES_BETWEEN_DATES_UNTIL";     //$NON-NLS-1$

   private static final String          STATE_IS_DELETE_CADENCE                     = "STATE_IS_DELETE_CADENCE";                         //$NON-NLS-1$
   private static final String          STATE_IS_DELETE_CALORIES                    = "STATE_IS_DELETE_CALORIES";                        //$NON-NLS-1$
   private static final String          STATE_IS_DELETE_ELEVATION                   = "STATE_IS_DELETE_ELEVATION";                       //$NON-NLS-1$
   private static final String          STATE_IS_DELETE_GEAR                        = "STATE_IS_DELETE_GEAR";                            //$NON-NLS-1$
   private static final String          STATE_IS_DELETE_POWER_AND_PULSE             = "STATE_IS_DELETE_POWER_AND_PULSE";                 //$NON-NLS-1$
   private static final String          STATE_IS_DELETE_POWER_AND_SPEED             = "STATE_IS_DELETE_POWER_AND_SPEED";                 //$NON-NLS-1$
   private static final String          STATE_IS_DELETE_RUNNING_DYNAMICS            = "STATE_IS_DELETE_RUNNING_DYNAMICS";                //$NON-NLS-1$
   private static final String          STATE_IS_DELETE_SWIMMING                    = "STATE_IS_DELETE_SWIMMING";                        //$NON-NLS-1$
   private static final String          STATE_IS_DELETE_TEMPERATURE_FROMDEVICE      = "STATE_IS_DELETE_TEMPERATURE_FROMDEVICE";          //$NON-NLS-1$
   private static final String          STATE_IS_DELETE_TIME                        = "STATE_IS_DELETE_TIME";                            //$NON-NLS-1$
   private static final String          STATE_IS_DELETE_TIMER_PAUSES                = "STATE_IS_DELETE_TIMER_PAUSES";                    //$NON-NLS-1$
   private static final String          STATE_IS_DELETE_TOUR_MARKERS                = "STATE_IS_DELETE_TOUR_MARKERS";                    //$NON-NLS-1$
   private static final String          STATE_IS_DELETE_TRAINING                    = "STATE_IS_DELETE_TRAINING";                        //$NON-NLS-1$
   private static final String          STATE_IS_DELETE_WEATHER                     = "STATE_IS_DELETE_WEATHER";                         //$NON-NLS-1$

   private static final IDialogSettings _state                                      = TourbookPlugin.getState("DialogDeleteTourValues"); //$NON-NLS-1$

   private final ITourViewer3           _tourViewer;

   private SelectionListener            _defaultListener;

   //
   private Image _imageLockClosed = CommonActivator.getThemedImageDescriptor(CommonImages.Lock_Closed).createImage();
   private Image _imageLockOpen   = CommonActivator.getThemedImageDescriptor(CommonImages.Lock_Open).createImage();

   /*
    * UI controls
    */
   private Composite _parent;

   private Button    _btnDeselectAll;
   private Button    _btnUnlockAllToursSelection;
   private Button    _btnUnlockBetweenDatesSelection;

   private Button    _chkData_Time;
   private Button    _chkData_Cadence;
   private Button    _chkData_Calories;
   private Button    _chkData_Elevation;
   private Button    _chkData_Gear;
   private Button    _chkData_PowerAndPulse;
   private Button    _chkData_PowerAndSpeed;
   private Button    _chkData_RunningDynamics;
   private Button    _chkData_Swimming;
   private Button    _chkData_Weather;
   private Button    _chkData_Temperature_FromDevice;
   private Button    _chkData_Training;
   private Button    _chkData_TourMarkers;
   private Button    _chkData_TourTimerPauses;

   private Button    _rdoDeleteTourValues_Tours_All;
   private Button    _rdoDeleteTourValues_Tours_BetweenDates;
   private Button    _rdoDeleteTourValues_Tours_Selected;

   private DateTime  _dtTourDate_From;
   private DateTime  _dtTourDate_Until;

   private Group     _groupTours;

   public DialogDeleteTourValues(final Shell parentShell,
                                 final ITourViewer3 tourViewer) {

      super(parentShell);

      _tourViewer = tourViewer;
   }

   @Override
   public boolean close() {

      saveState();

      return super.close();
   }

   @Override
   protected void configureShell(final Shell shell) {

      super.configureShell(shell);

      shell.setText(Messages.Dialog_DeleteTourValues_Dialog_Title);

      shell.addListener(SWT.Resize, event -> {

         // force shell default size

         final Point shellDefaultSize = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT);

         shell.setSize(shellDefaultSize);
      });
   }

   @Override
   public void create() {

      super.create();

      setTitle(Messages.Dialog_DeleteTourValues_Dialog_Title);
      setMessage(Messages.Dialog_DeleteTourValues_Dialog_Message);

      restoreState();
   }

   @Override
   protected final void createButtonsForButtonBar(final Composite parent) {

      super.createButtonsForButtonBar(parent);

      // set text for the OK button
      getButton(IDialogConstants.OK_ID).setText(Messages.Dialog_DeleteTourValues_Button_Delete);
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      _parent = parent;

      initUI();

      final Composite dlgContainer = (Composite) super.createDialogArea(parent);

      createUI(dlgContainer);

      // must be run async because the dark theme is overwriting colors after calling createDialogArea()
      _parent.getDisplay().asyncExec(this::updateUI_LockUnlockButtons);

      return dlgContainer;
   }

   private void createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.swtDefaults().margins(10, 5).applyTo(container);
      {
         createUI_10_Tours(container);
         createUI_20_ValuesToReset(container);
         createUI_30_ValuesToDelete(container);
      }
   }

   /**
    * UI to select either all the tours in the database or only the selected tours
    *
    * @param parent
    */
   private void createUI_10_Tours(final Composite parent) {

      _groupTours = new Group(parent, SWT.NONE);
      _groupTours.setText(Messages.Dialog_DeleteTourValues_Group_Tours);
      _groupTours.setToolTipText(Messages.Dialog_DeleteTourValues_Group_Tours_Tooltip);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(_groupTours);
      GridLayoutFactory.swtDefaults().spacing(5, 7).numColumns(4).applyTo(_groupTours);
      {
         {
            /*
             * Modify the SELECTED tours
             */
            _rdoDeleteTourValues_Tours_Selected = new Button(_groupTours, SWT.RADIO);
            _rdoDeleteTourValues_Tours_Selected.setText(Messages.Dialog_ModifyTours_Radio_SelectedTours);
            _rdoDeleteTourValues_Tours_Selected.addSelectionListener(_defaultListener);
            _rdoDeleteTourValues_Tours_Selected.setSelection(true);
            GridDataFactory.fillDefaults().span(4, 1).applyTo(_rdoDeleteTourValues_Tours_Selected);
         }
         {
            /*
             * Modify ALL tours in the database
             */
            {
               _rdoDeleteTourValues_Tours_All = new Button(_groupTours, SWT.RADIO);
               _rdoDeleteTourValues_Tours_All.setText(Messages.Dialog_ModifyTours_Radio_AllTours);
               _rdoDeleteTourValues_Tours_All.setSelection(false);
               _rdoDeleteTourValues_Tours_All.setEnabled(false);
               GridDataFactory.fillDefaults().span(3, 1).applyTo(_rdoDeleteTourValues_Tours_All);
            }
            {
               _btnUnlockAllToursSelection = new Button(_groupTours, SWT.PUSH);
               _btnUnlockAllToursSelection.setText(Messages.Dialog_ModifyTours_Button_UnlockMultipleToursSelection_Text);
               _btnUnlockAllToursSelection.setImage(_imageLockClosed);
               _btnUnlockAllToursSelection.addSelectionListener(
                     widgetSelectedAdapter(selectionEvent -> onSelect_Unlock_AllTours()));
            }
         }
         {
            /*
             * Modify between dates
             */
            {
               _rdoDeleteTourValues_Tours_BetweenDates = new Button(_groupTours, SWT.RADIO);
               _rdoDeleteTourValues_Tours_BetweenDates.setText(Messages.Dialog_ModifyTours_Radio_BetweenDates);
               _rdoDeleteTourValues_Tours_BetweenDates.setSelection(false);
               _rdoDeleteTourValues_Tours_BetweenDates.setEnabled(false);
            }
            {
               _dtTourDate_From = new DateTime(_groupTours, SWT.DATE | SWT.MEDIUM | SWT.DROP_DOWN | SWT.BORDER);
               _dtTourDate_From.addSelectionListener(_defaultListener);
               _dtTourDate_From.setEnabled(false);
            }
            {
               _dtTourDate_Until = new DateTime(_groupTours, SWT.DATE | SWT.MEDIUM | SWT.DROP_DOWN | SWT.BORDER);
               _dtTourDate_Until.addSelectionListener(_defaultListener);
               _dtTourDate_Until.setEnabled(false);
            }
            {
               _btnUnlockBetweenDatesSelection = new Button(_groupTours, SWT.PUSH);
               _btnUnlockBetweenDatesSelection.setText(Messages.Dialog_ModifyTours_Button_UnlockMultipleToursSelection_Text);
               _btnUnlockBetweenDatesSelection.setImage(_imageLockClosed);
               _btnUnlockBetweenDatesSelection.addSelectionListener(
                     widgetSelectedAdapter(selectionEvent -> onSelect_Unlock_BetweenDates()));
            }
         }
      }
   }

   /**
    * UI to select the values to reset for the chosen tours
    *
    * @param parent
    */
   private void createUI_20_ValuesToReset(final Composite parent) {

      final GridDataFactory gridDataItem = GridDataFactory.fillDefaults()
            .align(SWT.BEGINNING, SWT.CENTER)
            .indent(16, 0);

      /*
       * group: data
       */
      final Group group = new Group(parent, SWT.NONE);
      group.setText(Messages.Dialog_DeleteTourValues_Group_Reset);
      group.setToolTipText(Messages.Dialog_DeleteTourValues_Group_Reset_Tooltip);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
      {
         final Label label = new Label(group, SWT.WRAP);
         label.setText(Messages.Dialog_DeleteTourValues_Group_Reset_Label_Info);
         gridDataItem.span(2, 1).applyTo(label);

         // row 1
         {
            /*
             * Checkbox:Time
             */
            _chkData_Time = new Button(group, SWT.CHECK);
            _chkData_Time.setText(Messages.Dialog_DeleteTourValues_Checkbox_Time);
            _chkData_Time.addSelectionListener(_defaultListener);
            gridDataItem.applyTo(_chkData_Time);
         }

      }
   }

   /**
    * UI to select the values to delete for the chosen tours
    *
    * @param parent
    */
   private void createUI_30_ValuesToDelete(final Composite parent) {

      final GridDataFactory gridDataItem = GridDataFactory.fillDefaults()
            .align(SWT.BEGINNING, SWT.CENTER);

      final GridDataFactory gridDataItem_FirstColumn = GridDataFactory.fillDefaults()
            .align(SWT.BEGINNING, SWT.CENTER)
            .indent(16, 0);

      /*
       * group: data
       */
      final Group group = new Group(parent, SWT.NONE);
      group.setText(Messages.Dialog_DeleteTourValues_Group_Delete);
      group.setToolTipText(Messages.Dialog_DeleteTourValues_Group_Delete_Tooltip);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
      {
         final Label label = new Label(group, SWT.WRAP);
         label.setText(Messages.Dialog_DeleteTourValues_Group_Delete_Label_Info);
         GridDataFactory.fillDefaults().indent(16, 0).span(2, 1).applyTo(label);

         // row 1
         {
            /*
             * Checkbox: Cadence
             */
            _chkData_Cadence = new Button(group, SWT.CHECK);
            _chkData_Cadence.setText(Messages.Dialog_ModifyTours_Checkbox_CadenceValues);
            _chkData_Cadence.addSelectionListener(_defaultListener);
            gridDataItem_FirstColumn.applyTo(_chkData_Cadence);
         }
         {
            /*
             * Checkbox: Running Dynamics
             */
            _chkData_RunningDynamics = new Button(group, SWT.CHECK);
            _chkData_RunningDynamics.setText(Messages.Dialog_ModifyTours_Checkbox_RunningDynamicsValues);
            _chkData_RunningDynamics.addSelectionListener(_defaultListener);
            gridDataItem.applyTo(_chkData_RunningDynamics);
         }

         // row 2
         {
            /*
             * Checkbox: Calories
             */
            _chkData_Calories = new Button(group, SWT.CHECK);
            _chkData_Calories.setText(Messages.Dialog_ModifyTours_Checkbox_Calories);
            _chkData_Calories.addSelectionListener(_defaultListener);
            gridDataItem_FirstColumn.applyTo(_chkData_Calories);
         }
         {
            /*
             * Checkbox: Swimming
             */
            _chkData_Swimming = new Button(group, SWT.CHECK);
            _chkData_Swimming.setText(Messages.Dialog_ModifyTours_Checkbox_SwimmingValues);
            _chkData_Swimming.addSelectionListener(_defaultListener);
            gridDataItem.applyTo(_chkData_Swimming);
         }

         // row 3
         {
            /*
             * Checkbox: Weather
             */
            _chkData_Weather = new Button(group, SWT.CHECK);
            _chkData_Weather.setText(Messages.Dialog_ModifyTours_Checkbox_WeatherValues);
            _chkData_Weather.addSelectionListener(_defaultListener);
            gridDataItem_FirstColumn.applyTo(_chkData_Weather);
         }
         {
            /*
             * Checkbox: Temperature from device
             */
            _chkData_Temperature_FromDevice = new Button(group, SWT.CHECK);
            _chkData_Temperature_FromDevice.setText(Messages.Dialog_ModifyTours_Checkbox_TemperatureValues_FromDevice);
            _chkData_Temperature_FromDevice.addSelectionListener(_defaultListener);
            gridDataItem.applyTo(_chkData_Temperature_FromDevice);
         }

         // row 4
         {
            /*
             * Checkbox: Elevation
             */
            _chkData_Elevation = new Button(group, SWT.CHECK);
            _chkData_Elevation.setText(Messages.Dialog_ModifyTours_Checkbox_ElevationValues);
            _chkData_Elevation.addSelectionListener(_defaultListener);
            gridDataItem_FirstColumn.applyTo(_chkData_Elevation);
         }
         {
            /*
             * Checkbox: Gear
             */
            _chkData_Gear = new Button(group, SWT.CHECK);
            _chkData_Gear.setText(Messages.Dialog_ModifyTours_Checkbox_GearValues);
            _chkData_Gear.addSelectionListener(_defaultListener);
            gridDataItem.applyTo(_chkData_Gear);
         }

         // row 5
         {
            /*
             * Checkbox: Tour markers
             */
            _chkData_TourMarkers = new Button(group, SWT.CHECK);
            _chkData_TourMarkers.setText(Messages.Dialog_ModifyTours_Checkbox_TourMarkers);
            _chkData_TourMarkers.addSelectionListener(_defaultListener);
            gridDataItem_FirstColumn.applyTo(_chkData_TourMarkers);
         }
         {
            /*
             * Checkbox: Power And Pulse
             */
            _chkData_PowerAndPulse = new Button(group, SWT.CHECK);
            _chkData_PowerAndPulse.setText(Messages.Dialog_ModifyTours_Checkbox_PowerAndPulseValues);
            _chkData_PowerAndPulse.addSelectionListener(_defaultListener);
            gridDataItem.applyTo(_chkData_PowerAndPulse);
         }

         // row 6
         {
            /*
             * Checkbox: Tour pauses
             */
            _chkData_TourTimerPauses = new Button(group, SWT.CHECK);
            _chkData_TourTimerPauses.setText(Messages.Dialog_ModifyTours_Checkbox_TourTimerPauses);
            _chkData_TourTimerPauses.addSelectionListener(_defaultListener);
            gridDataItem_FirstColumn.applyTo(_chkData_TourTimerPauses);
         }
         {
            /*
             * Checkbox: Power And Speed
             */
            _chkData_PowerAndSpeed = new Button(group, SWT.CHECK);
            _chkData_PowerAndSpeed.setText(Messages.Dialog_ModifyTours_Checkbox_PowerAndSpeedValues);
            _chkData_PowerAndSpeed.addSelectionListener(_defaultListener);
            gridDataItem.applyTo(_chkData_PowerAndSpeed);
         }

         // row 7
         {
            /*
             * Checkbox: Training
             */
            _chkData_Training = new Button(group, SWT.CHECK);
            _chkData_Training.setText(Messages.Dialog_ModifyTours_Checkbox_TrainingValues);
            _chkData_Training.addSelectionListener(_defaultListener);
            gridDataItem_FirstColumn.applyTo(_chkData_Training);
         }

         {
            /*
             * Button: Deselect all
             */
            _btnDeselectAll = new Button(group, SWT.PUSH);
            _btnDeselectAll.setText(Messages.App_Action_DeselectAll);
            _btnDeselectAll.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onDeselectAll_DataItems()));
            GridDataFactory.fillDefaults()
                  .align(SWT.RIGHT, SWT.CENTER).span(2, 1).applyTo(_btnDeselectAll);
         }
      }

      // set tab ordering, cool feature but all controls MUST have the same parent !!!
      group.setTabList(new Control[] {

            // column 1
            _chkData_Cadence,
            _chkData_Calories,
            _chkData_Temperature_FromDevice,
            _chkData_Elevation,
            _chkData_TourMarkers,
            _chkData_TourTimerPauses,
            _chkData_Training,

            // column 2
            _chkData_RunningDynamics,
            _chkData_Swimming,
            _chkData_Weather,
            _chkData_Gear,
            _chkData_PowerAndPulse,
            _chkData_PowerAndSpeed,

            _btnDeselectAll
      });
   }

   /**
    * Start the values deletion process
    *
    * @param tourValueTypes
    *           A list of tour values to delete
    */
   private void doDeleteValues(final List<TourValueType> tourValueTypes) {

      /*
       * There maybe too much tour cleanup but it is very complex how all the caches/selection
       * provider work together
       */

      // prevent async error in the save tour method, cleanup environment
      _tourViewer.getPostSelectionProvider().clearSelection();

      Util.clearSelection();

      TourManager.fireEvent(TourEventId.CLEAR_DISPLAYED_TOUR, null, null);

      TourManager.getInstance().clearTourDataCache();

      final boolean isDeleteTourValues_AllTours = _rdoDeleteTourValues_Tours_All.getSelection();
      final boolean isDeleteTourValues_BetweenDates = _rdoDeleteTourValues_Tours_BetweenDates.getSelection();

      if (isDeleteTourValues_AllTours || isDeleteTourValues_BetweenDates) {

         //The user MUST always confirm when the tool is running for ALL tours
         if (isDeleteTourValues_AllTours) {

            final MessageDialog dialog = new MessageDialog(
                  Display.getDefault().getActiveShell(),
                  Messages.Dialog_DatabaseAction_Confirmation_Title,
                  null,
                  Messages.Dialog_DatabaseAction_Confirmation_Message,
                  MessageDialog.QUESTION,
                  new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL },
                  1);

            final int choice = dialog.open();

            if (choice == IDialogConstants.CANCEL_ID) {

               return;
            }
         }

         // Modify ALL tours or BETWEEN tours

         if (!RawDataManager.getInstance().actionModifyTourValues_10_Confirm(tourValueTypes, false)) {
            return;
         }

         saveState();

         TourLogManager.showLogView();

         final IComputeTourValues computeTourValueConfig = new IComputeTourValues() {

            @Override
            public boolean computeTourValues(final TourData tourData) {

               RawDataManager.getInstance().deleteTourValuesFromTour(tourValueTypes, tourData);

               return true;
            }

            @Override
            public String getResultText() {

               return UI.EMPTY_STRING;
            }

            @Override
            public String getSubTaskText(final TourData savedTourData) {

               return UI.EMPTY_STRING;
            }
         };

         ArrayList<Long> allTourIDs = null;

         if (isDeleteTourValues_BetweenDates) {

            // get tours between the dates

            allTourIDs = TourDatabase.getAllTourIds_BetweenTwoDates(

                  LocalDate.of(
                        _dtTourDate_From.getYear(),
                        _dtTourDate_From.getMonth() + 1,
                        _dtTourDate_From.getDay()),

                  LocalDate.of(
                        _dtTourDate_Until.getYear(),
                        _dtTourDate_Until.getMonth() + 1,
                        _dtTourDate_Until.getDay())

            );

            if (allTourIDs.isEmpty()) {

               MessageDialog.openInformation(getShell(),
                     Messages.Dialog_DeleteTourValues_Dialog_Title,
                     Messages.Dialog_ModifyTours_Dialog_ToursAreNotAvailable);

               return;
            }
         }

         TourDatabase.computeAnyValues_ForAllTours(computeTourValueConfig, allTourIDs);

         fireTourModifyEvent();

      } else {

         // Delete values for the SELECTED tours

         RawDataManager.getInstance().deleteTourValues(tourValueTypes, _tourViewer);
      }
   }

   private void enableControls() {

      final boolean isDataSelected = _chkData_Time.getSelection() ||
            _chkData_Elevation.getSelection() ||
            _chkData_Cadence.getSelection() ||
            _chkData_Calories.getSelection() ||
            _chkData_Gear.getSelection() ||
            _chkData_PowerAndPulse.getSelection() ||
            _chkData_PowerAndSpeed.getSelection() ||
            _chkData_RunningDynamics.getSelection() ||
            _chkData_Swimming.getSelection() ||
            _chkData_Weather.getSelection() ||
            _chkData_Temperature_FromDevice.getSelection() ||
            _chkData_Training.getSelection() ||
            _chkData_TourMarkers.getSelection() ||
            _chkData_TourTimerPauses.getSelection();

      // OK button
      getButton(IDialogConstants.OK_ID).setEnabled(isDataSelected && isDataValid());
   }

   private void fireTourModifyEvent() {

      TourManager.getInstance().removeAllToursFromCache();
      TourManager.fireEvent(TourEventId.CLEAR_DISPLAYED_TOUR);

      // prevent re-importing in the import view
      RawDataManager.setIsDeleteValuesActive(true);
      {
         // fire unique event for all changes
         TourManager.fireEvent(TourEventId.ALL_TOURS_ARE_MODIFIED);
      }
      RawDataManager.setIsDeleteValuesActive(false);
   }

   @Override
   protected IDialogSettings getDialogBoundsSettings() {

      // keep window size and position
      return _state;
   }

   private void initUI() {

      _defaultListener = widgetSelectedAdapter(selectionEvent -> enableControls());

      _parent.addDisposeListener(disposeEvent -> {
         _imageLockClosed.dispose();
         _imageLockOpen.dispose();
      });
   }

   private boolean isDataValid() {

      final LocalDate dtFrom = LocalDate.of(
            _dtTourDate_From.getYear(),
            _dtTourDate_From.getMonth() + 1,
            _dtTourDate_From.getDay());

      final LocalDate dtUntil = LocalDate.of(
            _dtTourDate_Until.getYear(),
            _dtTourDate_Until.getMonth() + 1,
            _dtTourDate_Until.getDay());

      if (dtUntil.toEpochDay() >= dtFrom.toEpochDay()) {

         setErrorMessage(null);
         return true;

      } else {

         setErrorMessage(Messages.Dialog_ModifyTours_Error_2ndDateMustBeLarger);
         return false;
      }
   }

   @Override
   protected void okPressed() {

      //We close the window so the user can see the progress bar and log view
      _parent.getShell().setVisible(false);

      BusyIndicator.showWhile(Display.getCurrent(), () -> {

         final List<TourValueType> tourValueTypes = new ArrayList<>();

         DialogUtils.addTourValueTypeFromCheckbox(_chkData_Time, TourValueType.TIME_SLICES__TIME, tourValueTypes);
         DialogUtils.addTourValueTypeFromCheckbox(_chkData_Cadence, TourValueType.TIME_SLICES__CADENCE, tourValueTypes);
         DialogUtils.addTourValueTypeFromCheckbox(_chkData_Calories, TourValueType.TOUR__CALORIES, tourValueTypes);
         DialogUtils.addTourValueTypeFromCheckbox(_chkData_Elevation, TourValueType.TIME_SLICES__ELEVATION, tourValueTypes);
         DialogUtils.addTourValueTypeFromCheckbox(_chkData_Gear, TourValueType.TIME_SLICES__GEAR, tourValueTypes);
         DialogUtils.addTourValueTypeFromCheckbox(_chkData_PowerAndPulse, TourValueType.TIME_SLICES__POWER_AND_PULSE, tourValueTypes);
         DialogUtils.addTourValueTypeFromCheckbox(_chkData_PowerAndSpeed, TourValueType.TIME_SLICES__POWER_AND_SPEED, tourValueTypes);
         DialogUtils.addTourValueTypeFromCheckbox(_chkData_RunningDynamics, TourValueType.TIME_SLICES__RUNNING_DYNAMICS, tourValueTypes);
         DialogUtils.addTourValueTypeFromCheckbox(_chkData_Swimming, TourValueType.TIME_SLICES__SWIMMING, tourValueTypes);
         DialogUtils.addTourValueTypeFromCheckbox(_chkData_Weather, TourValueType.TOUR__WEATHER, tourValueTypes);
         DialogUtils.addTourValueTypeFromCheckbox(_chkData_Temperature_FromDevice, TourValueType.TIME_SLICES__TEMPERATURE_FROMDEVICE, tourValueTypes);
         DialogUtils.addTourValueTypeFromCheckbox(_chkData_TourTimerPauses, TourValueType.TIME_SLICES__TIMER_PAUSES, tourValueTypes);
         DialogUtils.addTourValueTypeFromCheckbox(_chkData_Training, TourValueType.TIME_SLICES__TRAINING, tourValueTypes);
         DialogUtils.addTourValueTypeFromCheckbox(_chkData_TourMarkers, TourValueType.TOUR__MARKER, tourValueTypes);

         doDeleteValues(tourValueTypes);
      });

      super.okPressed();
   }

   private void onDeselectAll_DataItems() {

      _chkData_Time.setSelection(false);
      _chkData_Elevation.setSelection(false);
      _chkData_Cadence.setSelection(false);
      _chkData_Calories.setSelection(false);
      _chkData_Gear.setSelection(false);
      _chkData_PowerAndPulse.setSelection(false);
      _chkData_PowerAndSpeed.setSelection(false);
      _chkData_RunningDynamics.setSelection(false);
      _chkData_Swimming.setSelection(false);
      _chkData_Weather.setSelection(false);
      _chkData_Temperature_FromDevice.setSelection(false);
      _chkData_TourMarkers.setSelection(false);
      _chkData_TourTimerPauses.setSelection(false);
      _chkData_Training.setSelection(false);

      enableControls();
   }

   private void onSelect_Unlock_AllTours() {

      // toggle radio
      _rdoDeleteTourValues_Tours_All.setEnabled(!_rdoDeleteTourValues_Tours_All.isEnabled());

      final boolean isEnabled = _rdoDeleteTourValues_Tours_All.isEnabled();

      _btnUnlockAllToursSelection.setText(isEnabled
            ? Messages.Dialog_ModifyTours_Button_LockMultipleToursSelection_Text
            : Messages.Dialog_ModifyTours_Button_UnlockMultipleToursSelection_Text);

      _btnUnlockAllToursSelection.setImage(isEnabled
            ? _imageLockOpen
            : _imageLockClosed);

      if (!isEnabled) {
         _rdoDeleteTourValues_Tours_All.setSelection(false);
         _rdoDeleteTourValues_Tours_Selected.setSelection(true);
      }

      updateUI_LockUnlockButtons();
   }

   private void onSelect_Unlock_BetweenDates() {

      // toggle radio
      _rdoDeleteTourValues_Tours_BetweenDates.setEnabled(!_rdoDeleteTourValues_Tours_BetweenDates.isEnabled());

      final boolean isEnabled = _rdoDeleteTourValues_Tours_BetweenDates.isEnabled();

      _dtTourDate_From.setEnabled(isEnabled);
      _dtTourDate_Until.setEnabled(isEnabled);

      _btnUnlockBetweenDatesSelection.setText(isEnabled
            ? Messages.Dialog_ModifyTours_Button_LockMultipleToursSelection_Text
            : Messages.Dialog_ModifyTours_Button_UnlockMultipleToursSelection_Text);

      _btnUnlockBetweenDatesSelection.setImage(isEnabled
            ? _imageLockOpen
            : _imageLockClosed);

      if (!isEnabled) {
         _rdoDeleteTourValues_Tours_BetweenDates.setSelection(false);
         _rdoDeleteTourValues_Tours_Selected.setSelection(true);
      }

      updateUI_LockUnlockButtons();
   }

   private void restoreState() {

      Util.getStateDate(_state, STATE_DELETE_TOURVALUES_BETWEEN_DATES_FROM, LocalDate.now(), _dtTourDate_From);
      Util.getStateDate(_state, STATE_DELETE_TOURVALUES_BETWEEN_DATES_UNTIL, LocalDate.now(), _dtTourDate_Until);

      // Data to delete
      _chkData_Time.setSelection(_state.getBoolean(STATE_IS_DELETE_TIME));
      _chkData_Elevation.setSelection(_state.getBoolean(STATE_IS_DELETE_ELEVATION));
      _chkData_Cadence.setSelection(_state.getBoolean(STATE_IS_DELETE_CADENCE));
      _chkData_Calories.setSelection(_state.getBoolean(STATE_IS_DELETE_CALORIES));
      _chkData_Gear.setSelection(_state.getBoolean(STATE_IS_DELETE_GEAR));
      _chkData_PowerAndPulse.setSelection(_state.getBoolean(STATE_IS_DELETE_POWER_AND_PULSE));
      _chkData_PowerAndSpeed.setSelection(_state.getBoolean(STATE_IS_DELETE_POWER_AND_SPEED));
      _chkData_RunningDynamics.setSelection(_state.getBoolean(STATE_IS_DELETE_RUNNING_DYNAMICS));
      _chkData_Swimming.setSelection(_state.getBoolean(STATE_IS_DELETE_SWIMMING));
      _chkData_Weather.setSelection(_state.getBoolean(STATE_IS_DELETE_WEATHER));
      _chkData_Temperature_FromDevice.setSelection(_state.getBoolean(STATE_IS_DELETE_TEMPERATURE_FROMDEVICE));
      _chkData_Training.setSelection(_state.getBoolean(STATE_IS_DELETE_TRAINING));
      _chkData_TourMarkers.setSelection(_state.getBoolean(STATE_IS_DELETE_TOUR_MARKERS));
      _chkData_TourTimerPauses.setSelection(_state.getBoolean(STATE_IS_DELETE_TIMER_PAUSES));

      enableControls();
   }

   private void saveState() {

      Util.setStateDate(_state, STATE_DELETE_TOURVALUES_BETWEEN_DATES_FROM, _dtTourDate_From);
      Util.setStateDate(_state, STATE_DELETE_TOURVALUES_BETWEEN_DATES_UNTIL, _dtTourDate_Until);

      // Data to delete
      _state.put(STATE_IS_DELETE_ELEVATION, _chkData_Elevation.getSelection());
      _state.put(STATE_IS_DELETE_CADENCE, _chkData_Cadence.getSelection());
      _state.put(STATE_IS_DELETE_CALORIES, _chkData_Calories.getSelection());
      _state.put(STATE_IS_DELETE_GEAR, _chkData_Gear.getSelection());
      _state.put(STATE_IS_DELETE_POWER_AND_PULSE, _chkData_PowerAndPulse.getSelection());
      _state.put(STATE_IS_DELETE_POWER_AND_SPEED, _chkData_PowerAndSpeed.getSelection());
      _state.put(STATE_IS_DELETE_RUNNING_DYNAMICS, _chkData_RunningDynamics.getSelection());
      _state.put(STATE_IS_DELETE_SWIMMING, _chkData_Swimming.getSelection());
      _state.put(STATE_IS_DELETE_WEATHER, _chkData_Weather.getSelection());
      _state.put(STATE_IS_DELETE_TEMPERATURE_FROMDEVICE, _chkData_Temperature_FromDevice.getSelection());
      _state.put(STATE_IS_DELETE_TRAINING, _chkData_Training.getSelection());
      _state.put(STATE_IS_DELETE_TIME, _chkData_Time.getSelection());
      _state.put(STATE_IS_DELETE_TOUR_MARKERS, _chkData_TourMarkers.getSelection());
      _state.put(STATE_IS_DELETE_TIMER_PAUSES, _chkData_TourTimerPauses.getSelection());
   }

   /**
    * The relayout is needed because when setting a text for a button to "Lock" or "Unlock" an the
    * initial text was "Lock", when setting "Unlock", it will be truncate.
    */
   private void updateUI_LockUnlockButtons() {

      final boolean isDarkTheme = net.tourbook.common.UI.isDarkTheme();

      // get default foreground color
      final Color unlockColor = _parent.getForeground();
      final Color lockColor = isDarkTheme ? DialogUtils.LOCK_COLOR_DARK : DialogUtils.LOCK_COLOR_LIGHT;

      _btnUnlockAllToursSelection.setForeground(_rdoDeleteTourValues_Tours_All.isEnabled()
            ? unlockColor
            : lockColor);

      _btnUnlockBetweenDatesSelection.setForeground(_rdoDeleteTourValues_Tours_BetweenDates.isEnabled()
            ? unlockColor
            : lockColor);

      // ensure the modified text is fully visible
      _groupTours.layout(true, true);
   }
}
