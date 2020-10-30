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
   private static final String          STATE_IS_IMPORT_FILE_LOCATION                = "STATE_IS_IMPORT_FILE_LOCATION";                //$NON-NLS-1$
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

   private Button    _btnDeselectAll;

   private Button    _chkData_AllTimeSlices;
   private Button    _chkData_Cadence;
   private Button    _chkData_Elevation;
   private Button    _chkData_EntireTour;
   private Button    _chkData_Gear;
   private Button    _chkData_ImportFileLocation;
   private Button    _chkData_PowerAndPulse;
   private Button    _chkData_PowerAndSpeed;
   private Button    _chkData_RunningDynamics;
   private Button    _chkSkip_Tours_With_ImportFile_NotFound;
   private Button    _chkData_Swimming;
   private Button    _chkData_Temperature;
   private Button    _chkData_Training;
   private Button    _chkData_TourMarkers;
   private Button    _chkData_TourTimerPauses;

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

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.swtDefaults().margins(10, 5).applyTo(container);
      {
         createUI_10_Tours(container);
         createUI_20_Data(container);

         {
            /*
             * Checkbox: Skip tours for which the import file is not found
             */
            _chkSkip_Tours_With_ImportFile_NotFound = new Button(container, SWT.CHECK);
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

      final int verticalDistance = _pc.convertVerticalDLUsToPixels(0);
      final int verticalDistance_MoreVSpace = _pc.convertVerticalDLUsToPixels(4);

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

      final GridDataFactory gridDataItem_FirstColumn = GridDataFactory.fillDefaults()
            .align(SWT.BEGINNING, SWT.CENTER)
            .indent(16, verticalDistance);

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
            _chkData_EntireTour = new Button(group, SWT.CHECK);
            _chkData_EntireTour.setText(Messages.Import_Data_Checkbox_EntireTour);
            _chkData_EntireTour.addSelectionListener(tourListener);
            gridDataTour.applyTo(_chkData_EntireTour);
         }
         {
            /*
             * Checkbox: All time slices
             */
            _chkData_AllTimeSlices = new Button(group, SWT.CHECK);
            _chkData_AllTimeSlices.setText(Messages.Import_Data_Checkbox_TimeSlices);
            _chkData_AllTimeSlices.addSelectionListener(tourListener);
            gridDataTour_MoreVSpace.applyTo(_chkData_AllTimeSlices);
         }

         // row 1
         {
            /*
             * Checkbox: Cadence
             */
            _chkData_Cadence = new Button(group, SWT.CHECK);
            _chkData_Cadence.setText(Messages.Import_Data_Checkbox_CadenceValues);
            _chkData_Cadence.addSelectionListener(_buttonListener);
            gridDataItem_FirstColumn.applyTo(_chkData_Cadence);
         }
         {
            /*
             * Checkbox: Running Dynamics
             */
            _chkData_RunningDynamics = new Button(group, SWT.CHECK);
            _chkData_RunningDynamics.setText(Messages.Import_Data_Checkbox_RunningDynamicsValues);
            _chkData_RunningDynamics.addSelectionListener(_buttonListener);
            gridDataItem.applyTo(_chkData_RunningDynamics);
         }

         // row 2
         {
            /*
             * Checkbox: Elevation
             */
            _chkData_Elevation = new Button(group, SWT.CHECK);
            _chkData_Elevation.setText(Messages.Import_Data_Checkbox_AltitudeValues);
            _chkData_Elevation.addSelectionListener(_buttonListener);
            gridDataItem_FirstColumn.applyTo(_chkData_Elevation);
         }
         {
            /*
             * Checkbox: Swimming
             */
            _chkData_Swimming = new Button(group, SWT.CHECK);
            _chkData_Swimming.setText(Messages.Import_Data_Checkbox_SwimmingValues);
            _chkData_Swimming.addSelectionListener(_buttonListener);
            gridDataItem.applyTo(_chkData_Swimming);
         }

         // row 3
         {
            /*
             * Checkbox: Gear
             */
            _chkData_Gear = new Button(group, SWT.CHECK);
            _chkData_Gear.setText(Messages.Import_Data_Checkbox_GearValues);
            _chkData_Gear.addSelectionListener(_buttonListener);
            gridDataItem_FirstColumn.applyTo(_chkData_Gear);
         }
         {
            /*
             * Checkbox: Temperature
             */
            _chkData_Temperature = new Button(group, SWT.CHECK);
            _chkData_Temperature.setText(Messages.Import_Data_Checkbox_TemperatureValues);
            _chkData_Temperature.addSelectionListener(_buttonListener);
            gridDataItem.applyTo(_chkData_Temperature);
         }

         // row 4
         {
            /*
             * Checkbox: Power And Pulse
             */
            _chkData_PowerAndPulse = new Button(group, SWT.CHECK);
            _chkData_PowerAndPulse.setText(Messages.Import_Data_Checkbox_PowerAndPulseValues);
            _chkData_PowerAndPulse.addSelectionListener(_buttonListener);
            gridDataItem_FirstColumn.applyTo(_chkData_PowerAndPulse);
         }
         {
            /*
             * Checkbox: Timer pauses
             */
            _chkData_TourTimerPauses = new Button(group, SWT.CHECK);
            _chkData_TourTimerPauses.setText(Messages.Import_Data_Checkbox_TourTimerPauses);
            _chkData_TourTimerPauses.addSelectionListener(_buttonListener);
            gridDataItem.applyTo(_chkData_TourTimerPauses);
         }

         // row 5
         {
            /*
             * Checkbox: Power And Speed
             */
            _chkData_PowerAndSpeed = new Button(group, SWT.CHECK);
            _chkData_PowerAndSpeed.setText(Messages.Import_Data_Checkbox_PowerAndSpeedValues);
            _chkData_PowerAndSpeed.addSelectionListener(_buttonListener);
            gridDataItem_FirstColumn.applyTo(_chkData_PowerAndSpeed);
         }
         {
            /*
             * Checkbox: Training
             */
            _chkData_Training = new Button(group, SWT.CHECK);
            _chkData_Training.setText(Messages.Import_Data_Checkbox_TrainingValues);
            _chkData_Training.addSelectionListener(_buttonListener);
            gridDataItem.applyTo(_chkData_Training);
         }

         // row 6
         {
            /*
             * Checkbox: Tour markers
             */
            _chkData_TourMarkers = new Button(group, SWT.CHECK);
            _chkData_TourMarkers.setText(Messages.Import_Data_Checkbox_TourMarkers);
            _chkData_TourMarkers.addSelectionListener(_buttonListener);
            gridDataTour_MoreVSpace.applyTo(_chkData_TourMarkers);
         }
         {
            /*
             * Checkbox: Import file location
             */
            _chkData_ImportFileLocation = new Button(group, SWT.CHECK);
            _chkData_ImportFileLocation.setText(Messages.Import_Data_Checkbox_ImportFileLocation);
            _chkData_ImportFileLocation.setToolTipText(Messages.Import_Data_Checkbox_ImportFileLocation_Tooltip);
            _chkData_ImportFileLocation.addSelectionListener(_buttonListener);
            gridDataItem.applyTo(_chkData_ImportFileLocation);
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

            _chkData_EntireTour,
            _chkData_AllTimeSlices,

            // column 1
            _chkData_Elevation,
            _chkData_Cadence,
            _chkData_Gear,
            _chkData_PowerAndPulse,
            _chkData_PowerAndSpeed,

            // column 2
            _chkData_RunningDynamics,
            _chkData_Swimming,
            _chkData_Temperature,
            _chkData_Training,
            _chkData_TourTimerPauses,

            _chkData_TourMarkers,
            _chkData_ImportFileLocation,
            _btnDeselectAll
      });
   }

   /**
    * Start the re-import process
    *
    * @param reImportPartIds
    *           A list of data IDs to be re-imported
    * @throws IOException
    */
   private void doReimport(final List<ReImportParts> reImportPartIds) throws IOException {

      final boolean isReimportAllTours = _rdoReimport_Tours_All.getSelection();
      final boolean skipToursWithFileNotFound = _chkSkip_Tours_With_ImportFile_NotFound.getSelection();

      if (isReimportAllTours) {

         // re-import ALL tours

         if (RawDataManager.getInstance().actionReimportTour_10_Confirm(reImportPartIds) == false) {
            return;
         }

         saveState();

         TourLogManager.showLogView();

         final File[] reimportedFile = new File[1];
         final IComputeTourValues computeTourValueConfig = new IComputeTourValues() {

            @Override
            public boolean computeTourValues(final TourData oldTourData) {

               final ReImportStatus reImportStatus = new ReImportStatus();

               RawDataManager.getInstance().reimportTour(reImportPartIds,
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

         RawDataManager.getInstance().actionReimportSelectedTours(reImportPartIds, _tourViewer, skipToursWithFileNotFound);
      }
   }

   private void enableDataButtons() {

      final boolean isReimport_EntireTour = _chkData_EntireTour.getSelection();
      final boolean isReimport_AllTimeSlices = _chkData_AllTimeSlices.getSelection();

      final boolean isTimeSlice = !isReimport_EntireTour && !isReimport_AllTimeSlices;

//      _btnDeselectAll.setEnabled(isTimeSlice);

      _chkData_AllTimeSlices.setEnabled(!isReimport_EntireTour);
      _chkData_ImportFileLocation.setEnabled(!isReimport_EntireTour);
      _chkData_TourMarkers.setEnabled(!isReimport_EntireTour);

      _chkData_Elevation.setEnabled(isTimeSlice);
      _chkData_Cadence.setEnabled(isTimeSlice);
      _chkData_Gear.setEnabled(isTimeSlice);
      _chkData_PowerAndPulse.setEnabled(isTimeSlice);
      _chkData_PowerAndSpeed.setEnabled(isTimeSlice);
      _chkData_RunningDynamics.setEnabled(isTimeSlice);
      _chkData_Swimming.setEnabled(isTimeSlice);
      _chkData_Temperature.setEnabled(isTimeSlice);
      _chkData_TourTimerPauses.setEnabled(isTimeSlice);
      _chkData_Training.setEnabled(isTimeSlice);
   }

   private void enableReimportButton() {

      final boolean isEnabled = ((_rdoReimport_Tours_All.getSelection() || _rdoReimport_Tours_Selected.getSelection()) &&
            (_chkData_EntireTour.getSelection() ||
                  _chkData_AllTimeSlices.getSelection() ||
                  _chkData_Elevation.getSelection() ||
                  _chkData_Cadence.getSelection() ||
                  _chkData_Gear.getSelection() ||
                  _chkData_ImportFileLocation.getSelection() ||
                  _chkData_PowerAndPulse.getSelection() ||
                  _chkData_PowerAndSpeed.getSelection() ||
                  _chkData_RunningDynamics.getSelection() ||
                  _chkData_Swimming.getSelection() ||
                  _chkData_Temperature.getSelection() ||
                  _chkData_Training.getSelection() ||
                  _chkData_TourMarkers.getSelection() ||
                  _chkData_TourTimerPauses.getSelection()));

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

               if (_chkData_EntireTour.getSelection()) {

                  reImportPartIds.add(ReImportParts.ENTIRE_TOUR);

               } else {

                  if (_chkData_AllTimeSlices.getSelection()) {

                     reImportPartIds.add(ReImportParts.ALL_TIME_SLICES);

                  } else {

                     if (_chkData_Cadence.getSelection()) {
                        reImportPartIds.add(ReImportParts.TIME_SLICES_CADENCE);
                     }
                     if (_chkData_Elevation.getSelection()) {
                        reImportPartIds.add(ReImportParts.TIME_SLICES_ELEVATION);
                     }
                     if (_chkData_Gear.getSelection()) {
                        reImportPartIds.add(ReImportParts.TIME_SLICES_GEAR);
                     }
                     if (_chkData_PowerAndPulse.getSelection()) {
                        reImportPartIds.add(ReImportParts.TIME_SLICES_POWER_AND_PULSE);
                     }
                     if (_chkData_PowerAndSpeed.getSelection()) {
                        reImportPartIds.add(ReImportParts.TIME_SLICES_POWER_AND_SPEED);
                     }
                     if (_chkData_RunningDynamics.getSelection()) {
                        reImportPartIds.add(ReImportParts.TIME_SLICES_RUNNING_DYNAMICS);
                     }
                     if (_chkData_Swimming.getSelection()) {
                        reImportPartIds.add(ReImportParts.TIME_SLICES_SWIMMING);
                     }
                     if (_chkData_Temperature.getSelection()) {
                        reImportPartIds.add(ReImportParts.TIME_SLICES_TEMPERATURE);
                     }
                     if (_chkData_TourTimerPauses.getSelection()) {
                        reImportPartIds.add(ReImportParts.TIME_SLICES_TIMER_PAUSES);
                     }
                     if (_chkData_Training.getSelection()) {
                        reImportPartIds.add(ReImportParts.TIME_SLICES_TRAINING);
                     }
                  }

                  if (_chkData_TourMarkers.getSelection()) {
                     reImportPartIds.add(ReImportParts.TOUR_MARKER);
                  }
                  if (_chkData_ImportFileLocation.getSelection()) {
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

      _chkData_AllTimeSlices.setSelection(false);
      _chkData_Elevation.setSelection(false);
      _chkData_EntireTour.setSelection(false);
      _chkData_Cadence.setSelection(false);
      _chkData_Gear.setSelection(false);
      _chkData_ImportFileLocation.setSelection(false);
      _chkData_PowerAndPulse.setSelection(false);
      _chkData_PowerAndSpeed.setSelection(false);
      _chkData_RunningDynamics.setSelection(false);
      _chkData_Swimming.setSelection(false);
      _chkData_Temperature.setSelection(false);
      _chkData_TourMarkers.setSelection(false);
      _chkData_TourTimerPauses.setSelection(false);
      _chkData_Training.setSelection(false);

      enableDataButtons();
      enableReimportButton();
   }

   private void restoreState() {

      // Data to re-import
      final boolean isReimportEntireTour = _state.getBoolean(STATE_IS_IMPORT_ENTIRE_TOUR);
      _chkData_AllTimeSlices.setSelection(_state.getBoolean(STATE_IS_IMPORT_ALL_TIME_SLICES));
      _chkData_EntireTour.setSelection(isReimportEntireTour);
      _chkData_Elevation.setSelection(_state.getBoolean(STATE_IS_IMPORT_ELEVATION));
      _chkData_Cadence.setSelection(_state.getBoolean(STATE_IS_IMPORT_CADENCE));
      _chkData_Gear.setSelection(_state.getBoolean(STATE_IS_IMPORT_GEAR));
      _chkData_ImportFileLocation.setSelection(_state.getBoolean(STATE_IS_IMPORT_FILE_LOCATION));
      _chkData_PowerAndPulse.setSelection(_state.getBoolean(STATE_IS_IMPORT_POWER_AND_PULSE));
      _chkData_PowerAndSpeed.setSelection(_state.getBoolean(STATE_IS_IMPORT_POWER_AND_SPEED));
      _chkData_RunningDynamics.setSelection(_state.getBoolean(STATE_IS_IMPORT_RUNNING_DYNAMICS));
      _chkData_Swimming.setSelection(_state.getBoolean(STATE_IS_IMPORT_SWIMMING));
      _chkData_Temperature.setSelection(_state.getBoolean(STATE_IS_IMPORT_TEMPERATURE));
      _chkData_Training.setSelection(_state.getBoolean(STATE_IS_IMPORT_TRAINING));
      _chkData_TourMarkers.setSelection(_state.getBoolean(STATE_IS_IMPORT_TOUR_MARKERS));
      _chkData_TourTimerPauses.setSelection(_state.getBoolean(STATE_IS_IMPORT_TIMER_PAUSES));

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
      _state.put(STATE_IS_IMPORT_ENTIRE_TOUR, _chkData_EntireTour.getSelection());
      _state.put(STATE_IS_IMPORT_ELEVATION, _chkData_Elevation.getSelection());
      _state.put(STATE_IS_IMPORT_CADENCE, _chkData_Cadence.getSelection());
      _state.put(STATE_IS_IMPORT_GEAR, _chkData_Gear.getSelection());
      _state.put(STATE_IS_IMPORT_FILE_LOCATION, _chkData_ImportFileLocation.getSelection());
      _state.put(STATE_IS_IMPORT_POWER_AND_PULSE, _chkData_PowerAndPulse.getSelection());
      _state.put(STATE_IS_IMPORT_POWER_AND_SPEED, _chkData_PowerAndSpeed.getSelection());
      _state.put(STATE_IS_IMPORT_RUNNING_DYNAMICS, _chkData_RunningDynamics.getSelection());
      _state.put(STATE_IS_IMPORT_SWIMMING, _chkData_Swimming.getSelection());
      _state.put(STATE_IS_IMPORT_TEMPERATURE, _chkData_Temperature.getSelection());
      _state.put(STATE_IS_IMPORT_TRAINING, _chkData_Training.getSelection());
      _state.put(STATE_IS_IMPORT_ALL_TIME_SLICES, _chkData_AllTimeSlices.getSelection());
      _state.put(STATE_IS_IMPORT_TOUR_MARKERS, _chkData_TourMarkers.getSelection());
      _state.put(STATE_IS_IMPORT_TIMER_PAUSES, _chkData_TourTimerPauses.getSelection());

      // Skip tours for which the import file is not found
      _state.put(STATE_IS_SKIP_TOURS_WITH_IMPORTFILE_NOTFOUND, _chkSkip_Tours_With_ImportFile_NotFound.getSelection());
   }
}
