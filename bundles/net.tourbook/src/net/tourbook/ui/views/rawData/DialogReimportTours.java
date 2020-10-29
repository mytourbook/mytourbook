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
import net.tourbook.importdata.RawDataManager.ReImport;
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

   private static final String   STATE_REIMPORT_TOURS_ALL                     = "STATE_REIMPORT_TOURS_ALL";          //$NON-NLS-1$
   private static final String   STATE_REIMPORT_TOURS_SELECTED                = "STATE_REIMPORT_TOURS_SELECTED";     //$NON-NLS-1$

   private static final String   STATE_IS_IMPORT_ALTITUDE                     = "isImportAltitude";                  //$NON-NLS-1$
   private static final String   STATE_IS_IMPORT_CADENCE                      = "isImportCadence";                   //$NON-NLS-1$
   private static final String   STATE_IS_IMPORT_GEAR                         = "isImportGear";                      //$NON-NLS-1$
   private static final String   STATE_IS_IMPORT_POWERANDPULSE                = "isImportPowerAndPulse";             //$NON-NLS-1$
   private static final String   STATE_IS_IMPORT_POWERANDSPEED                = "isImportPowerAndSpeed";             //$NON-NLS-1$
   private static final String   STATE_IS_IMPORT_RUNNINGDYNAMICS              = "isImportRunningDynamics";           //$NON-NLS-1$
   private static final String   STATE_IS_IMPORT_SWIMMING                     = "isImportSwimming";                  //$NON-NLS-1$
   private static final String   STATE_IS_IMPORT_TEMPERATURE                  = "isImportTemperature";               //$NON-NLS-1$
   private static final String   STATE_IS_IMPORT_TRAINING                     = "isImportTraining";                  //$NON-NLS-1$
   private static final String   STATE_IS_IMPORT_TIMESLICES                   = "isImportTimeSlices";                //$NON-NLS-1$
   private static final String   STATE_IS_IMPORT_TOURMARKERS                  = "isImportTourMarkers";               //$NON-NLS-1$
   private static final String   STATE_IS_IMPORT_TIMERPAUSES                  = "isImportTimerPauses";               //$NON-NLS-1$
   private static final String   STATE_IS_IMPORT_ENTIRETOUR                   = "isImportEntireTours";               //$NON-NLS-1$
   private static final String   STATE_IS_SKIP_TOURS_WITH_IMPORTFILE_NOTFOUND = "isSkipToursWithImportFileNotFound"; //$NON-NLS-1$

   private static final int      VERTICAL_SECTION_MARGIN                      = 10;

   private static String         _dlgDefaultMessage;

   private final IDialogSettings _state                                       = TourbookPlugin
         .getState("DialogReimportTours");                                                                           //$NON-NLS-1$

   private Point                 _shellDefaultSize;

   private final ITourViewer3    _tourViewer;
   private PixelConverter        _pc;

   /*
    * UI controls
    */
   private Composite      _parent;
   private Composite      _dlgContainer;
   private Composite      _inputContainer;

   private Button         _btnDeselectAll;

   private Button         _chkCadence;
   private Button         _chkElevation;
   private Button         _chkEntireTour;
   private Button         _chkGear;
   private Button         _chkPowerAndPulse;
   private Button         _chkPowerAndSpeed;
   private Button         _chkRunningDynamics;
   private Button         _chkSkip_Tours_With_ImportFile_NotFound;
   private Button         _chkSwimming;
   private Button         _chkTemperature;
   private Button         _chkTraining;
   private Button         _chkTimeSlices;
   private Button         _chkTourMarkers;
   private Button         _chkTourTimerPauses;

   private Button         _rdoReimport_Tours_All;
   private Button         _rdoReimport_Tours_Selected;

   final SelectionAdapter _buttonListener = new SelectionAdapter() {
                                             @Override
                                             public void widgetSelected(final SelectionEvent e) {
                                                enableReimportButton();
                                             }
                                          };

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
            _chkTimeSlices = new Button(group, SWT.CHECK);
            _chkTimeSlices.setText(Messages.Import_Data_Checkbox_TimeSlices);
            _chkTimeSlices.addSelectionListener(tourListener);
            gridDataTour.applyTo(_chkTimeSlices);
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
            gridDataItem_MoreVSpace.applyTo(_chkTourMarkers);
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
                  .indent(0, verticalDistance_MoreVSpace).applyTo(_btnDeselectAll);
         }
      }

      group.setTabList(new Control[] {

            _chkEntireTour,
            _chkTimeSlices,

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
   private void doReimport(final List<ReImport> reimportIds) throws IOException {

      final boolean isReimportAllTours = _rdoReimport_Tours_All.getSelection();
      final boolean skipToursWithFileNotFound = _chkSkip_Tours_With_ImportFile_NotFound.getSelection();

      if (isReimportAllTours) {

         if (RawDataManager.getInstance().actionReimportTour_10_Confirm(reimportIds) == false) {
            return;
         }

         saveState();

         TourLogManager.showLogView();

         final File[] reimportedFile = new File[1];
         final IComputeTourValues computeTourValueConfig = new IComputeTourValues() {

            @Override
            public boolean computeTourValues(final TourData oldTourData) {

               RawDataManager.getInstance().reimportTour(reimportIds,
                     oldTourData,
                     reimportedFile,
                     skipToursWithFileNotFound);

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

         RawDataManager.getInstance().actionReimportTour(reimportIds, _tourViewer, skipToursWithFileNotFound);
      }
   }

   private void enableDataButtons() {

      final boolean isReimportEntireTour = _chkEntireTour.getSelection();
      final boolean isReimportTimeSlices = _chkTimeSlices.getSelection();

      final boolean isTimeSlice = !isReimportEntireTour && !isReimportTimeSlices;

      _btnDeselectAll.setEnabled(isTimeSlice);

      _chkTimeSlices.setEnabled(!isReimportEntireTour);
      _chkTourMarkers.setEnabled(!isReimportEntireTour);

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
                  _chkElevation.getSelection() ||
                  _chkCadence.getSelection() ||
                  _chkGear.getSelection() ||
                  _chkPowerAndPulse.getSelection() ||
                  _chkPowerAndSpeed.getSelection() ||
                  _chkRunningDynamics.getSelection() ||
                  _chkSwimming.getSelection() ||
                  _chkTemperature.getSelection() ||
                  _chkTraining.getSelection() ||
                  _chkTimeSlices.getSelection() ||
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
   }

   @Override
   protected void okPressed() {

      //We close the window so the user can see that import progress bar and log view
      _parent.getShell().setVisible(false);

      BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
         @Override
         public void run() {
            try {

               final List<ReImport> reimportIds = new ArrayList<>();

               if (_chkEntireTour.getSelection()) {
                  reimportIds.add(ReImport.Tour);
               } else {

                  if (_chkTimeSlices.getSelection()) {
                     reimportIds.add(ReImport.TimeSlices);
                  } else {
                     if (_chkElevation.getSelection()) {
                        reimportIds.add(ReImport.AltitudeValues);
                     }
                     if (_chkCadence.getSelection()) {
                        reimportIds.add(ReImport.CadenceValues);
                     }
                     if (_chkGear.getSelection()) {
                        reimportIds.add(ReImport.GearValues);
                     }
                     if (_chkPowerAndPulse.getSelection()) {
                        reimportIds.add(ReImport.PowerAndPulseValues);
                     }
                     if (_chkPowerAndSpeed.getSelection()) {
                        reimportIds.add(ReImport.PowerAndSpeedValues);
                     }
                     if (_chkRunningDynamics.getSelection()) {
                        reimportIds.add(ReImport.RunningDynamics);
                     }
                     if (_chkSwimming.getSelection()) {
                        reimportIds.add(ReImport.Swimming);
                     }
                     if (_chkTemperature.getSelection()) {
                        reimportIds.add(ReImport.TemperatureValues);
                     }
                     if (_chkTourTimerPauses.getSelection()) {
                        reimportIds.add(ReImport.TourTimerPauses);
                     }
                     if (_chkTraining.getSelection()) {
                        reimportIds.add(ReImport.TrainingValues);
                     }
                  }
                  if (_chkTourMarkers.getSelection()) {
                     reimportIds.add(ReImport.TourMarkers);
                  }

               }

               doReimport(reimportIds);

            } catch (final IOException e) {
               StatusUtil.log(e);
            }
         }
      });

      super.okPressed();
   }

   private void onDeselectAll_DataItems() {

      _chkElevation.setSelection(false);
      _chkEntireTour.setSelection(false);
      _chkCadence.setSelection(false);
      _chkGear.setSelection(false);
      _chkPowerAndPulse.setSelection(false);
      _chkPowerAndSpeed.setSelection(false);
      _chkRunningDynamics.setSelection(false);
      _chkSwimming.setSelection(false);
      _chkTemperature.setSelection(false);
      _chkTimeSlices.setSelection(false);
      _chkTourMarkers.setSelection(false);
      _chkTourTimerPauses.setSelection(false);
      _chkTraining.setSelection(false);

   }

   private void restoreState() {

      // Data to re-import
      final boolean isReimportEntireTour = _state.getBoolean(STATE_IS_IMPORT_ENTIRETOUR);
      _chkEntireTour.setSelection(isReimportEntireTour);
      _chkElevation.setSelection(_state.getBoolean(STATE_IS_IMPORT_ALTITUDE));
      _chkCadence.setSelection(_state.getBoolean(STATE_IS_IMPORT_CADENCE));
      _chkGear.setSelection(_state.getBoolean(STATE_IS_IMPORT_GEAR));
      _chkPowerAndPulse.setSelection(_state.getBoolean(STATE_IS_IMPORT_POWERANDPULSE));
      _chkPowerAndSpeed.setSelection(_state.getBoolean(STATE_IS_IMPORT_POWERANDSPEED));
      _chkRunningDynamics.setSelection(_state.getBoolean(STATE_IS_IMPORT_RUNNINGDYNAMICS));
      _chkSwimming.setSelection(_state.getBoolean(STATE_IS_IMPORT_SWIMMING));
      _chkTemperature.setSelection(_state.getBoolean(STATE_IS_IMPORT_TEMPERATURE));
      _chkTraining.setSelection(_state.getBoolean(STATE_IS_IMPORT_TRAINING));
      _chkTimeSlices.setSelection(_state.getBoolean(STATE_IS_IMPORT_TIMESLICES));
      _chkTourMarkers.setSelection(_state.getBoolean(STATE_IS_IMPORT_TOURMARKERS));
      _chkTourTimerPauses.setSelection(_state.getBoolean(STATE_IS_IMPORT_TIMERPAUSES));

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
      _state.put(STATE_IS_IMPORT_ENTIRETOUR, _chkEntireTour.getSelection());
      _state.put(STATE_IS_IMPORT_ALTITUDE, _chkElevation.getSelection());
      _state.put(STATE_IS_IMPORT_CADENCE, _chkCadence.getSelection());
      _state.put(STATE_IS_IMPORT_GEAR, _chkGear.getSelection());
      _state.put(STATE_IS_IMPORT_POWERANDPULSE, _chkPowerAndPulse.getSelection());
      _state.put(STATE_IS_IMPORT_POWERANDSPEED, _chkPowerAndSpeed.getSelection());
      _state.put(STATE_IS_IMPORT_RUNNINGDYNAMICS, _chkRunningDynamics.getSelection());
      _state.put(STATE_IS_IMPORT_SWIMMING, _chkSwimming.getSelection());
      _state.put(STATE_IS_IMPORT_TEMPERATURE, _chkTemperature.getSelection());
      _state.put(STATE_IS_IMPORT_TRAINING, _chkTraining.getSelection());
      _state.put(STATE_IS_IMPORT_TIMESLICES, _chkTimeSlices.getSelection());
      _state.put(STATE_IS_IMPORT_TOURMARKERS, _chkTourMarkers.getSelection());
      _state.put(STATE_IS_IMPORT_TIMERPAUSES, _chkTourTimerPauses.getSelection());

      // Skip tours for which the import file is not found
      _state.put(STATE_IS_SKIP_TOURS_WITH_IMPORTFILE_NOTFOUND, _chkSkip_Tours_With_ImportFile_NotFound.getSelection());
   }
}
