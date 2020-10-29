/*******************************************************************************
 * Copyright (C) 2020 Frédéric Bard
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

import de.byteholder.geoclipse.map.UI;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.ITourViewer3;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.TourData;
import net.tourbook.database.IComputeTourValues;
import net.tourbook.database.TourDatabase;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.importdata.RawDataManager.ReImportParts;
import net.tourbook.importdata.ReImportStatus;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourLogManager;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

public class DialogReimportTours extends TitleAreaDialog {

   private static final String          STATE_REIMPORT_TOURS_ALL                     = "STATE_REIMPORT_TOURS_ALL";                     //$NON-NLS-1$
   private static final String          STATE_REIMPORT_TOURS_SELECTED                = "STATE_REIMPORT_TOURS_SELECTED";                //$NON-NLS-1$

   private static final String          STATE_IS_IMPORT_ALL_TIME_SLICES              = "STATE_IS_IMPORT_ALL_TIME_SLICES";              //$NON-NLS-1$
   private static final String          STATE_IS_IMPORT_CADENCE                      = "STATE_IS_IMPORT_CADENCE";                      //$NON-NLS-1$
   private static final String          STATE_IS_IMPORT_ELEVATION                    = "STATE_IS_IMPORT_ELEVATION";                    //$NON-NLS-1$
   private static final String          STATE_IS_IMPORT_ENTIRE_TOUR                  = "STATE_IS_IMPORT_ENTIRE_TOUR";                  //$NON-NLS-1$
   private static final String          STATE_IS_IMPORT_FILE_LOCATION                = "STATE_IS_IMPORT_FILE_LOCATION"; //$NON-NLS-1$
   private static final String          STATE_IS_IMPORT_GEAR                         = "STATE_IS_IMPORT_GEAR";                         //$NON-NLS-1$
   private static final String          STATE_IS_IMPORT_POWER_AND_PULSE              = "STATE_IS_IMPORT_POWER_AND_PULSE";              //$NON-NLS-1$
   private static final String          STATE_IS_IMPORT_POWER_AND_SPEED              = "STATE_IS_IMPORT_POWER_AND_SPEED";              //$NON-NLS-1$
   private static final String          STATE_IS_IMPORT_RUNNING_DYNAMICS             = "STATE_IS_IMPORT_RUNNING_DYNAMICS";             //$NON-NLS-1$
   private static final String          STATE_IS_IMPORT_SWIMMING                     = "STATE_IS_IMPORT_SWIMMING";                     //$NON-NLS-1$
   private static final String          STATE_IS_IMPORT_TEMPERATURE                  = "STATE_IS_IMPORT_TEMPERATURE";                  //$NON-NLS-1$
   private static final String          STATE_IS_IMPORT_TRAINING                     = "STATE_IS_IMPORT_TRAINING";                     //$NON-NLS-1$
   private static final String          STATE_IS_IMPORT_TOUR_MARKERS                 = "STATE_IS_IMPORT_TOUR_MARKERS";                 //$NON-NLS-1$
   private static final String          STATE_IS_IMPORT_TIMER_PAUSES                 = "STATE_IS_IMPORT_TIMER_PAUSES";                 //$NON-NLS-1$
   private static final String          STATE_IS_SKIP_TOURS_WITH_IMPORTFILE_NOTFOUND = "STATE_IS_SKIP_TOURS_WITH_IMPORTFILE_NOTFOUND"; //$NON-NLS-1$

   private static final int             VERTICAL_SECTION_MARGIN                      = 10;

   private static final IDialogSettings _state                                       = TourbookPlugin.getState("DialogReimportTours"); //$NON-NLS-1$

   private static String                _dlgDefaultMessage;

   private Point                        _shellDefaultSize;

   private final ITourViewer3           _tourViewer;
   private PixelConverter               _pc;

   private SelectionAdapter             _buttonListener;

   /*
    * UI controls
    */
   private Composite _parent;
   private Composite _dlgContainer;
   private Composite _inputContainer;

   private Button    _btnDeselectAll;

   private Button    _chkAllTimeSlices;
   private Button    _chkCadence;
   private Button    _chkElevation;
   private Button    _chkEntireTour;
   private Button    _chkGear;
   private Button    _chkImportFileLocation;
   private Button    _chkPowerAndPulse;
   private Button    _chkPowerAndSpeed;
   private Button    _chkRunningDynamics;
   private Button    _chkSkip_Tours_With_ImportFile_NotFound;
   private Button    _chkSwimming;
   private Button    _chkTemperature;
   private Button    _chkTraining;
   private Button    _chkTourMarkers;
   private Button    _chkTourTimerPauses;

   private Button    _rdoReimport_Tours_All;
   private Button    _rdoReimport_Tours_Selected;

   /**
    * @param parentShell
    */
   public DialogReimportTours(final Shell parentShell,
                              final ITourViewer3 tourViewer) {

      super(parentShell);

      _tourViewer = tourViewer;

      int shellStyle = getShellStyle();

      shellStyle = SWT.NONE
            | SWT.TITLE
            | SWT.CLOSE
            | SWT.MIN
            | SWT.RESIZE
            | SWT.NONE;

      // make dialog resizable
      setShellStyle(shellStyle);
   }

   @Override
   public boolean close() {

      saveState();

      return super.close();
   }

   @Override
   protected void configureShell(final Shell shell) {

      super.configureShell(shell);

      shell.setText(Messages.dialog_reimport_tours_dialog_title);

      shell.addListener(SWT.Resize, new Listener() {
         @Override
         public void handleEvent(final Event event) {

            // allow resizing the width but not the height

            if (_shellDefaultSize == null) {
               _shellDefaultSize = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
            }

            final Point shellSize = shell.getSize();

            shellSize.x = shellSize.x < _shellDefaultSize.x ? _shellDefaultSize.x : shellSize.x;
            shellSize.y = _shellDefaultSize.y;

            shell.setSize(shellSize);
         }
      });
   }

   @Override
   public void create() {

      super.create();

      setTitle(Messages.dialog_reimport_tours_dialog_title);
      setMessage(_dlgDefaultMessage);

      restoreState();
   }

   @Override
   protected final void createButtonsForButtonBar(final Composite parent) {

      super.createButtonsForButtonBar(parent);

      // set text for the OK button
      getButton(IDialogConstants.OK_ID).setText(Messages.dialog_reimport_tours_btn_reimport);
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      _parent = parent;

      initUI();

      _dlgContainer = (Composite) super.createDialogArea(parent);

      createUI(_dlgContainer);

      return _dlgContainer;
   }

   private void createUI(final Composite parent) {

      _inputContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_inputContainer);
      GridLayoutFactory.swtDefaults().margins(10, 5).applyTo(_inputContainer);
      {
         createUI_10_Tours(_inputContainer);
         createUI_20_Data(_inputContainer);

         {
            /*
             * Checkbox: Skip tours for which the import file is not found
             */
            _chkSkip_Tours_With_ImportFile_NotFound = new Button(_inputContainer, SWT.CHECK);
            _chkSkip_Tours_With_ImportFile_NotFound.setText(Messages.dialog_reimport_tours_btn_skip_tours_with_importFile_notfound);
            GridDataFactory.fillDefaults().grab(true, false).indent(0, VERTICAL_SECTION_MARGIN).applyTo(_chkSkip_Tours_With_ImportFile_NotFound);
         }
      }
   }

   /**
    * UI to select either all the tours in the database or only the selected tours
    *
    * @param parent
    */
   private void createUI_10_Tours(final Composite parent) {

      final Group groupTours = new Group(parent, SWT.NONE);
      groupTours.setText(Messages.Dialog_Reimport_Tours_Group_Tours);
      groupTours.setToolTipText(Messages.Dialog_Reimport_Tours_Group_Tours_Tooltip);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(groupTours);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(groupTours);
      {
         /*
          * checkbox: Re-import all tours in the database
          */
         _rdoReimport_Tours_All = new Button(groupTours, SWT.RADIO);
         _rdoReimport_Tours_All.setText(Messages.dialog_reimport_tours_checkbox_alltours);
         _rdoReimport_Tours_All.addSelectionListener(_buttonListener);

         /*
          * checkbox: Re-import the selected tours
          */
         _rdoReimport_Tours_Selected = new Button(groupTours, SWT.RADIO);
         _rdoReimport_Tours_Selected.setText(Messages.dialog_reimport_tours_checkbox_selectedtours);
         _rdoReimport_Tours_Selected.addSelectionListener(_buttonListener);
      }
   }

   /**
    * UI to select the data to re-import for the chosen tours
    *
    * @param parent
    */
   private void createUI_20_Data(final Composite parent) {

      final int verticalDistance = _pc.convertVerticalDLUsToPixels(4);
      final int verticalDistance_MoreVSpace = _pc.convertVerticalDLUsToPixels(12);

      final GridDataFactory gridDataTour = GridDataFactory.fillDefaults()
            .align(SWT.BEGINNING, SWT.CENTER)
            .span(2, 1)
            .indent(0, verticalDistance);

      final GridDataFactory gridDataTour_MoreVSpace = GridDataFactory.fillDefaults()
            .align(SWT.BEGINNING, SWT.CENTER)
            .span(2, 1)
            .indent(0, verticalDistance_MoreVSpace);

      final GridDataFactory gridDataItem = GridDataFactory.fillDefaults()
            .align(SWT.BEGINNING, SWT.CENTER)
            .indent(0, verticalDistance);

      final GridDataFactory gridDataItem_MoreVSpace = GridDataFactory.fillDefaults()
            .align(SWT.BEGINNING, SWT.CENTER)
            .indent(0, verticalDistance_MoreVSpace);

      final SelectionAdapter tourListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            enableDataButtons();
            enableReimportButton();
         }
      };

      /*
       * group: data
       */
      final Group group = new Group(parent, SWT.NONE);
      group.setText(Messages.Dialog_Reimport_Tours_Group_Data);
      group.setText(Messages.Dialog_Reimport_Tours_Group_Data_Tooltip);
      GridDataFactory.fillDefaults().grab(true, false).indent(0, VERTICAL_SECTION_MARGIN).applyTo(group);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
      {
         {
            /*
             * Checkbox: Entire Tour
             */
            _chkEntireTour = new Button(group, SWT.CHECK);
            _chkEntireTour.setText(Messages.Import_Data_Checkbox_EntireTour);
            _chkEntireTour.addSelectionListener(tourListener);
            gridDataTour.applyTo(_chkEntireTour);
         }
         {
            /*
             * Checkbox: All time slices
             */
            _chkAllTimeSlices = new Button(group, SWT.CHECK);
            _chkAllTimeSlices.setText(Messages.Import_Data_Checkbox_TimeSlices);
            _chkAllTimeSlices.addSelectionListener(tourListener);
            gridDataTour.applyTo(_chkAllTimeSlices);
         }

         // row 1
         {
            /*
             * Checkbox: Elevation
             */
            _chkElevation = new Button(group, SWT.CHECK);
            _chkElevation.setText(Messages.Import_Data_Checkbox_AltitudeValues);
            _chkElevation.addSelectionListener(_buttonListener);
            gridDataItem_MoreVSpace.applyTo(_chkElevation);
         }
         {
            /*
             * Checkbox: Running Dynamics
             */
            _chkRunningDynamics = new Button(group, SWT.CHECK);
            _chkRunningDynamics.setText(Messages.Import_Data_Checkbox_RunningDynamicsValues);
            _chkRunningDynamics.addSelectionListener(_buttonListener);
            gridDataItem_MoreVSpace.applyTo(_chkRunningDynamics);
         }

         // row 2
         {
            /*
             * Checkbox: Cadence
             */
            _chkCadence = new Button(group, SWT.CHECK);
            _chkCadence.setText(Messages.Import_Data_Checkbox_CadenceValues);
            _chkCadence.addSelectionListener(_buttonListener);
            gridDataItem.applyTo(_chkCadence);
         }
         {
            /*
             * Checkbox: Swimming
             */
            _chkSwimming = new Button(group, SWT.CHECK);
            _chkSwimming.setText(Messages.Import_Data_Checkbox_SwimmingValues);
            _chkSwimming.addSelectionListener(_buttonListener);
            gridDataItem.applyTo(_chkSwimming);
         }

         // row 3
         {
            /*
             * Checkbox: Gear
             */
            _chkGear = new Button(group, SWT.CHECK);
            _chkGear.setText(Messages.Import_Data_Checkbox_GearValues);
            _chkGear.addSelectionListener(_buttonListener);
            gridDataItem.applyTo(_chkGear);
         }
         {
            /*
             * Checkbox: Temperature
             */
            _chkTemperature = new Button(group, SWT.CHECK);
            _chkTemperature.setText(Messages.Import_Data_Checkbox_TemperatureValues);
            _chkTemperature.addSelectionListener(_buttonListener);
            gridDataItem.applyTo(_chkTemperature);
         }

         // row 4
         {
            /*
             * Checkbox: Power And Pulse
             */
            _chkPowerAndPulse = new Button(group, SWT.CHECK);
            _chkPowerAndPulse.setText(Messages.Import_Data_Checkbox_PowerAndPulseValues);
            _chkPowerAndPulse.addSelectionListener(_buttonListener);
            gridDataItem.applyTo(_chkPowerAndPulse);
         }
         {
            /*
             * Checkbox: Training
             */
            _chkTraining = new Button(group, SWT.CHECK);
            _chkTraining.setText(Messages.Import_Data_Checkbox_TrainingValues);
            _chkTraining.addSelectionListener(_buttonListener);
            gridDataItem.applyTo(_chkTraining);
         }

         // row 5
         {
            /*
             * Checkbox: Power And Speed
             */
            _chkPowerAndSpeed = new Button(group, SWT.CHECK);
            _chkPowerAndSpeed.setText(Messages.Import_Data_Checkbox_PowerAndSpeedValues);
            _chkPowerAndSpeed.addSelectionListener(_buttonListener);
            gridDataItem.applyTo(_chkPowerAndSpeed);
         }
         {
            /*
             * Checkbox: Timer pauses
             */
            _chkTourTimerPauses = new Button(group, SWT.CHECK);
            _chkTourTimerPauses.setText(Messages.Import_Data_Checkbox_TourTimerPauses);
            _chkTourTimerPauses.addSelectionListener(_buttonListener);
            gridDataItem.applyTo(_chkTourTimerPauses);
         }

         // row 6
         {
            /*
             * Checkbox: Tour markers
             */
            _chkTourMarkers = new Button(group, SWT.CHECK);
            _chkTourMarkers.setText(Messages.Import_Data_Checkbox_TourMarkers);
            _chkTourMarkers.addSelectionListener(_buttonListener);
            gridDataTour_MoreVSpace.applyTo(_chkTourMarkers);
         }
         {
            /*
             * Checkbox: Import file location
             */
            _chkImportFileLocation = new Button(group, SWT.CHECK);
            _chkImportFileLocation.setText(Messages.Import_Data_Checkbox_ImportFileLocation);
            _chkImportFileLocation.addSelectionListener(_buttonListener);
            gridDataItem.applyTo(_chkImportFileLocation);
         }
         {
            /*
             * Button: Deselect all
             */
            _btnDeselectAll = new Button(group, SWT.PUSH);
            _btnDeselectAll.setText(Messages.App_Action_DeselectAll);
            _btnDeselectAll.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onDeselectAll_DataItems();
               }
            });
            GridDataFactory.fillDefaults()
                  .align(SWT.END, SWT.CENTER)
                  .grab(true, false)
                  .indent(0, verticalDistance).applyTo(_btnDeselectAll);
         }
      }

      group.setTabList(new Control[] {

            _chkEntireTour,
            _chkAllTimeSlices,

            // column 1
            _chkElevation,
            _chkCadence,
            _chkGear,
            _chkPowerAndPulse,
            _chkPowerAndSpeed,

            // column 2
            _chkRunningDynamics,
            _chkSwimming,
            _chkTemperature,
            _chkTraining,
            _chkTourTimerPauses,

            _chkTourMarkers,
            _chkImportFileLocation,
            _btnDeselectAll
      });
   }

   /**
    * Start the re-import process
    *
    * @param reimportIds
    *           A list of data IDs to be re-imported
    * @throws IOException
    */
   private void doReimport(final List<ReImportParts> reimportIds) throws IOException {

      final boolean isReimportAllTours = _rdoReimport_Tours_All.getSelection();
      final boolean skipToursWithFileNotFound = _chkSkip_Tours_With_ImportFile_NotFound.getSelection();

      if (isReimportAllTours) {

         // re-import ALL tours

         if (RawDataManager.getInstance().actionReimportTour_10_Confirm(reimportIds) == false) {
            return;
         }

         saveState();

         TourLogManager.showLogView();

         final File[] reimportedFile = new File[1];
         final IComputeTourValues computeTourValueConfig = new IComputeTourValues() {

            @Override
            public boolean computeTourValues(final TourData oldTourData) {

               final ReImportStatus reImportStatus = new ReImportStatus();

               RawDataManager.getInstance().reimportTour(reimportIds,
                     oldTourData,
                     reimportedFile,
                     skipToursWithFileNotFound,
                     reImportStatus);

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

         TourDatabase.computeAnyValues_ForAllTours(computeTourValueConfig, null);

         fireTourModifyEvent();

      } else {

         // re-import SELECTED tours

         RawDataManager.getInstance().actionReimportSelectedTours(reimportIds, _tourViewer, skipToursWithFileNotFound);
      }
   }

   private void enableDataButtons() {

      final boolean isReimport_EntireTour = _chkEntireTour.getSelection();
      final boolean isReimport_AllTimeSlices = _chkAllTimeSlices.getSelection();

      final boolean isTimeSlice = !isReimport_EntireTour && !isReimport_AllTimeSlices;

      _btnDeselectAll.setEnabled(isTimeSlice);

      _chkAllTimeSlices.setEnabled(!isReimport_EntireTour);
      _chkImportFileLocation.setEnabled(!isReimport_EntireTour);
      _chkTourMarkers.setEnabled(!isReimport_EntireTour);

      _chkElevation.setEnabled(isTimeSlice);
      _chkCadence.setEnabled(isTimeSlice);
      _chkGear.setEnabled(isTimeSlice);
      _chkPowerAndPulse.setEnabled(isTimeSlice);
      _chkPowerAndSpeed.setEnabled(isTimeSlice);
      _chkRunningDynamics.setEnabled(isTimeSlice);
      _chkSwimming.setEnabled(isTimeSlice);
      _chkTemperature.setEnabled(isTimeSlice);
      _chkTourTimerPauses.setEnabled(isTimeSlice);
      _chkTraining.setEnabled(isTimeSlice);
   }

   private void enableReimportButton() {

      final boolean isEnabled = ((_rdoReimport_Tours_All.getSelection() || _rdoReimport_Tours_Selected.getSelection()) &&
            (_chkEntireTour.getSelection() ||
                  _chkAllTimeSlices.getSelection() ||
                  _chkElevation.getSelection() ||
                  _chkCadence.getSelection() ||
                  _chkGear.getSelection() ||
                  _chkImportFileLocation.getSelection() ||
                  _chkPowerAndPulse.getSelection() ||
                  _chkPowerAndSpeed.getSelection() ||
                  _chkRunningDynamics.getSelection() ||
                  _chkSwimming.getSelection() ||
                  _chkTemperature.getSelection() ||
                  _chkTraining.getSelection() ||
                  _chkTourMarkers.getSelection() ||
                  _chkTourTimerPauses.getSelection()));

      final Button okButton = getButton(IDialogConstants.OK_ID);
      if (okButton != null) {
         okButton.setEnabled(isEnabled);
      }
   }

   private void fireTourModifyEvent() {

      TourManager.getInstance().removeAllToursFromCache();
      TourManager.fireEvent(TourEventId.CLEAR_DISPLAYED_TOUR);

      // fire unique event for all changes
      TourManager.fireEvent(TourEventId.ALL_TOURS_ARE_MODIFIED);
   }

   @Override
   protected IDialogSettings getDialogBoundsSettings() {
      // keep window size and position
      return _state;
   }

   private void initUI() {

      _pc = new PixelConverter(_parent);

      _buttonListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            enableReimportButton();
         }
      };
   }

   @Override
   protected void okPressed() {

      //We close the window so the user can see that import progress bar and log view
      _parent.getShell().setVisible(false);

      BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
         @Override
         public void run() {
            try {

               final List<ReImportParts> reImportPartIds = new ArrayList<>();

               if (_chkEntireTour.getSelection()) {
                  reImportPartIds.add(ReImportParts.ENTIRE_TOUR);
               } else {

                  if (_chkAllTimeSlices.getSelection()) {
                     reImportPartIds.add(ReImportParts.ALL_TIME_SLICES);
                  } else {
                     if (_chkElevation.getSelection()) {
                        reImportPartIds.add(ReImportParts.TIME_SLICES_ELEVATION);
                     }
                     if (_chkCadence.getSelection()) {
                        reImportPartIds.add(ReImportParts.TIME_SLICES_CADENCE);
                     }
                     if (_chkGear.getSelection()) {
                        reImportPartIds.add(ReImportParts.TIME_SLICES_GEAR);
                     }
                     if (_chkPowerAndPulse.getSelection()) {
                        reImportPartIds.add(ReImportParts.TIME_SLICES_POWER_AND_PULSE);
                     }
                     if (_chkPowerAndSpeed.getSelection()) {
                        reImportPartIds.add(ReImportParts.TIME_SLICES_POWER_AND_SPEED);
                     }
                     if (_chkRunningDynamics.getSelection()) {
                        reImportPartIds.add(ReImportParts.TIME_SLICES_RUNNING_DYNAMICS);
                     }
                     if (_chkSwimming.getSelection()) {
                        reImportPartIds.add(ReImportParts.TIME_SLICES_SWIMMING);
                     }
                     if (_chkTemperature.getSelection()) {
                        reImportPartIds.add(ReImportParts.TIME_SLICES_TEMPERATURE);
                     }
                     if (_chkTourTimerPauses.getSelection()) {
                        reImportPartIds.add(ReImportParts.TIME_SLICES_TIMER_PAUSES);
                     }
                     if (_chkTraining.getSelection()) {
                        reImportPartIds.add(ReImportParts.TIME_SLICES_TRAINING);
                     }
                  }

                  if (_chkTourMarkers.getSelection()) {
                     reImportPartIds.add(ReImportParts.TOUR_MARKER);
                  }
                  if (_chkImportFileLocation.getSelection()) {
                     reImportPartIds.add(ReImportParts.IMPORT_FILE_LOCATION);
                  }
               }

               doReimport(reImportPartIds);

            } catch (final IOException e) {
               StatusUtil.log(e);
            }
         }
      });

      super.okPressed();
   }

   private void onDeselectAll_DataItems() {

      _chkAllTimeSlices.setSelection(false);
      _chkElevation.setSelection(false);
      _chkEntireTour.setSelection(false);
      _chkCadence.setSelection(false);
      _chkGear.setSelection(false);
      _chkImportFileLocation.setSelection(false);
      _chkPowerAndPulse.setSelection(false);
      _chkPowerAndSpeed.setSelection(false);
      _chkRunningDynamics.setSelection(false);
      _chkSwimming.setSelection(false);
      _chkTemperature.setSelection(false);
      _chkTourMarkers.setSelection(false);
      _chkTourTimerPauses.setSelection(false);
      _chkTraining.setSelection(false);

      enableReimportButton();
   }

   private void restoreState() {

      // Data to re-import
      final boolean isReimportEntireTour = _state.getBoolean(STATE_IS_IMPORT_ENTIRE_TOUR);
      _chkAllTimeSlices.setSelection(_state.getBoolean(STATE_IS_IMPORT_ALL_TIME_SLICES));
      _chkEntireTour.setSelection(isReimportEntireTour);
      _chkElevation.setSelection(_state.getBoolean(STATE_IS_IMPORT_ELEVATION));
      _chkCadence.setSelection(_state.getBoolean(STATE_IS_IMPORT_CADENCE));
      _chkGear.setSelection(_state.getBoolean(STATE_IS_IMPORT_GEAR));
      _chkImportFileLocation.setSelection(_state.getBoolean(STATE_IS_IMPORT_FILE_LOCATION));
      _chkPowerAndPulse.setSelection(_state.getBoolean(STATE_IS_IMPORT_POWER_AND_PULSE));
      _chkPowerAndSpeed.setSelection(_state.getBoolean(STATE_IS_IMPORT_POWER_AND_SPEED));
      _chkRunningDynamics.setSelection(_state.getBoolean(STATE_IS_IMPORT_RUNNING_DYNAMICS));
      _chkSwimming.setSelection(_state.getBoolean(STATE_IS_IMPORT_SWIMMING));
      _chkTemperature.setSelection(_state.getBoolean(STATE_IS_IMPORT_TEMPERATURE));
      _chkTraining.setSelection(_state.getBoolean(STATE_IS_IMPORT_TRAINING));
      _chkTourMarkers.setSelection(_state.getBoolean(STATE_IS_IMPORT_TOUR_MARKERS));
      _chkTourTimerPauses.setSelection(_state.getBoolean(STATE_IS_IMPORT_TIMER_PAUSES));

      //Tours to re-import
      final boolean isReimportAllTours = _state.getBoolean(STATE_REIMPORT_TOURS_ALL);
      _rdoReimport_Tours_All.setSelection(isReimportAllTours);

      final boolean isReimportSelectedTours = _state.getBoolean(STATE_REIMPORT_TOURS_SELECTED);
      _rdoReimport_Tours_Selected.setSelection(isReimportSelectedTours);

      // Skip tours for which the import file is not found
      _chkSkip_Tours_With_ImportFile_NotFound.setSelection(_state.getBoolean(STATE_IS_SKIP_TOURS_WITH_IMPORTFILE_NOTFOUND));

      enableDataButtons();
      enableReimportButton();
   }

   private void saveState() {

      //Tours to re-import
      _state.put(STATE_REIMPORT_TOURS_ALL, _rdoReimport_Tours_All.getSelection());
      _state.put(STATE_REIMPORT_TOURS_SELECTED, _rdoReimport_Tours_Selected.getSelection());

      // Data to import
      _state.put(STATE_IS_IMPORT_ENTIRE_TOUR, _chkEntireTour.getSelection());
      _state.put(STATE_IS_IMPORT_ELEVATION, _chkElevation.getSelection());
      _state.put(STATE_IS_IMPORT_CADENCE, _chkCadence.getSelection());
      _state.put(STATE_IS_IMPORT_GEAR, _chkGear.getSelection());
      _state.put(STATE_IS_IMPORT_FILE_LOCATION, _chkImportFileLocation.getSelection());
      _state.put(STATE_IS_IMPORT_POWER_AND_PULSE, _chkPowerAndPulse.getSelection());
      _state.put(STATE_IS_IMPORT_POWER_AND_SPEED, _chkPowerAndSpeed.getSelection());
      _state.put(STATE_IS_IMPORT_RUNNING_DYNAMICS, _chkRunningDynamics.getSelection());
      _state.put(STATE_IS_IMPORT_SWIMMING, _chkSwimming.getSelection());
      _state.put(STATE_IS_IMPORT_TEMPERATURE, _chkTemperature.getSelection());
      _state.put(STATE_IS_IMPORT_TRAINING, _chkTraining.getSelection());
      _state.put(STATE_IS_IMPORT_ALL_TIME_SLICES, _chkAllTimeSlices.getSelection());
      _state.put(STATE_IS_IMPORT_TOUR_MARKERS, _chkTourMarkers.getSelection());
      _state.put(STATE_IS_IMPORT_TIMER_PAUSES, _chkTourTimerPauses.getSelection());

      // Skip tours for which the import file is not found
      _state.put(STATE_IS_SKIP_TOURS_WITH_IMPORTFILE_NOTFOUND, _chkSkip_Tours_With_ImportFile_NotFound.getSelection());
   }
}
