/*******************************************************************************
 * Copyright (C) 2024 Wolfgang Schramm and Contributors
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

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.ui.tourChart.ChartLabelMarker;

import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;

/**
 * Dialog to automatically create tour markers by distance or time
 */
public class DialogCreateTourMarkers extends Dialog {

   private static final String          ID                   = "net.tourbook.tour.DialogCreateTourMarkers"; //$NON-NLS-1$

   public static final String           STATE_IS_BY_DISTANCE = "STATE_IS_BY_DISTANCE";                      //$NON-NLS-1$
   public static final String           STATE_DISTANCE_VALUE = "STATE_DISTANCE_VALUE";                      //$NON-NLS-1$
   public static final String           STATE_TIME_VALUE     = "STATE_TIME_VALUE";                          //$NON-NLS-1$

   private static final IDialogSettings _state               = TourbookPlugin.getState(ID);

   private static final NumberFormat    _nf0                 = NumberFormat.getInstance();
   private static final NumberFormat    _nf1                 = NumberFormat.getInstance();
   {
      _nf0.setMinimumFractionDigits(0);
      _nf0.setMaximumFractionDigits(0);
      _nf1.setMinimumFractionDigits(1);
      _nf1.setMaximumFractionDigits(1);
   }

   private MouseWheelListener _defaultMouseWheelListener;
   private SelectionListener  _defaultSelectedListener;

   private Set<Long>          _allSelectedTourIDs;

   private PixelConverter     _pc;

   /*
    * UI controls
    */
   private Button  _radioByDistance;
   private Button  _radioByTime;

   private Label   _labelDistanceUnit;
   private Label   _labelTimeUnit;

   private Spinner _spinnerByDistance;
   private Spinner _spinnerByTime;

   private Image   _imageDialog;

   public DialogCreateTourMarkers(final Shell parentShell, final Set<Long> allSelectedTourIDs) {

      super(parentShell);

      _allSelectedTourIDs = allSelectedTourIDs;

      _imageDialog = TourbookPlugin.getThemedImageDescriptor(Images.TourMarker_New).createImage();
      setDefaultImage(_imageDialog);
   }

   @Override
   public boolean close() {

      saveState();

      return super.close();
   }

   @Override
   protected void configureShell(final Shell shell) {

      super.configureShell(shell);

      // set window title
      shell.setText(Messages.Dialog_CreateTourMarkers_Title);
   }

   @Override
   protected final void createButtonsForButtonBar(final Composite parent) {

      super.createButtonsForButtonBar(parent);

      // OK -> Create Markers
      getButton(IDialogConstants.OK_ID).setText(Messages.Dialog_CreateTourMarkers_Button_ValidateMarkers);
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      initUI(parent);
      parent.addDisposeListener(disposeEvent -> UI.disposeResource(_imageDialog));

      final Composite ui = (Composite) super.createDialogArea(parent);

      createUI(ui);

      restoreState();

      updateUI_Distance();

      // must be delayed because the OK button is not yet created
      parent.getDisplay().asyncExec(() -> enableControls());

      return ui;
   }

   private void createTourMarkers() {

      final List<TourData> allModifiedTours = new ArrayList<>();
      final int[] numCreatedMarkers = { 0 };

      if (_radioByDistance.getSelection()) {

         createTourMarkers_ByDistance(allModifiedTours, numCreatedMarkers);

      } else {

         createTourMarkers_ByTime(allModifiedTours, numCreatedMarkers);
      }

      final Display display = getShell().getDisplay();

      if (numCreatedMarkers[0] == 0) {

         MessageDialog.openInformation(getShell(),

               Messages.Dialog_CreateTourMarkers_Title,

               // There are no new tour markers
               Messages.Dialog_CreateTourMarkers_Message_NoNewMarkers);

      } else {

         final MessageDialog dialog = new MessageDialog(

               display.getActiveShell(),

               Messages.Dialog_CreateTourMarkers_Title,
               _imageDialog, // window image

               Messages.Dialog_CreateTourMarkers_Message_CreateMarkers.formatted(numCreatedMarkers[0]),

               MessageDialog.CONFIRM,

               0, // default index

               Messages.Dialog_CreateTourMarkers_Button_CreateMarkers,
               Messages.App_Action_Cancel);

         if (dialog.open() == IDialogConstants.OK_ID) {

            if (allModifiedTours.size() > 0) {

               TourManager.saveModifiedTours(allModifiedTours);
            }

         } else {

            // revert modified tours

            numCreatedMarkers[0] = 0;

            TourManager.getInstance().clearTourDataCache();

//          TourManager.fireEvent(TourEventId.CLEAR_DISPLAYED_TOUR);
         }

         TourManager.fireEvent(TourEventId.ALL_TOURS_ARE_MODIFIED);
      }
   }

   private void createTourMarkers_ByDistance(final List<TourData> allModifiedTours,
                                             final int[] numCreatedMarkers) {

      final float repeatedDistance = getSelectedDistance();

      for (final Long tourId : _allSelectedTourIDs) {

         final TourData tourData = TourManager.getTour(tourId);

         final float[] distanceSerie = tourData.distanceSerie;

         if (distanceSerie == null) {
            continue;
         }

         final IntArrayList allNewMarker_SerieIndices = new IntArrayList();

         float nextDistance = repeatedDistance;

         /*
          * Create distance serie indices
          */
         for (int distanceIndex = 0; distanceIndex < distanceSerie.length; distanceIndex++) {

            final float distance = distanceSerie[distanceIndex];
            if (distance >= nextDistance) {

               allNewMarker_SerieIndices.add(distanceIndex);

               // set minimum distance for the next marker
               nextDistance += repeatedDistance;
            }
         }

         /*
          * Create new markers
          */
         createTourMarkers_OneTour(

               allNewMarker_SerieIndices,
               tourData,

               allModifiedTours,
               numCreatedMarkers,

               true // is distance
         );
      }
   }

   private void createTourMarkers_ByTime(final List<TourData> allModifiedTours,
                                         final int[] numCreatedMarkers) {

      final int timeIntervalSeconds = _spinnerByTime.getSelection() * 60;

      for (final Long tourId : _allSelectedTourIDs) {

         final TourData tourData = TourManager.getTour(tourId);

         final int[] timeSerie = tourData.timeSerie;

         if (timeSerie == null) {
            continue;
         }

         final IntArrayList allNewMarker_SerieIndices = new IntArrayList();

         float nextRepeatedTimeSeconds = timeIntervalSeconds;

         /*
          * Create time serie indices
          */
         for (int timeIndex = 0; timeIndex < timeSerie.length; timeIndex++) {

            float timeSerieSeconds = timeSerie[timeIndex];

            if (timeSerieSeconds >= nextRepeatedTimeSeconds) {

               allNewMarker_SerieIndices.add(timeIndex);

               // set minimum time for the next marker
               nextRepeatedTimeSeconds += timeIntervalSeconds;

               // check if there are pauses
               if (timeSerieSeconds >= nextRepeatedTimeSeconds) {

                  // move to the next repeated time after the pause
                  for (; timeIndex < timeSerie.length; timeIndex++) {

                     timeSerieSeconds = timeSerie[timeIndex];

                     if (timeSerieSeconds >= nextRepeatedTimeSeconds) {

                        // set minimum time for the next marker
                        nextRepeatedTimeSeconds += timeIntervalSeconds;

                     } else {

                        break;
                     }
                  }
               }
            }
         }

         /*
          * Create new markers
          */
         createTourMarkers_OneTour(

               allNewMarker_SerieIndices,
               tourData,

               allModifiedTours,
               numCreatedMarkers,

               false // is time
         );
      }
   }

   private void createTourMarkers_OneTour(final IntArrayList allNewMarker_SerieIndices,
                                          final TourData tourData,
                                          final List<TourData> allModifiedTours,
                                          final int[] numCreatedMarkers,
                                          final boolean isDistance) {

      final List<TourMarker> allExistingMarkers = tourData.getTourMarkersSorted();
      final List<TourMarker> allNewMarkers = new ArrayList<>();

      int existingMarkerIndex = 0;

      int serieIndex_ExistingMarker = allExistingMarkers.size() > 0
            ? allExistingMarkers.get(0).getSerieIndex()
            : Integer.MAX_VALUE;

      // Loop: all new markers, skip existing markers
      for (final int serieIndex_NewMarker : allNewMarker_SerieIndices.toArray()) {

         boolean isCreateMarker = false;

         if (serieIndex_NewMarker == serieIndex_ExistingMarker) {

            // skip this marker

         } else if (serieIndex_NewMarker < serieIndex_ExistingMarker) {

            // new marker is before the next marker

            isCreateMarker = true;

         } else {

            // move to the next marker

            isCreateMarker = true;

            for (; existingMarkerIndex < allExistingMarkers.size(); existingMarkerIndex++) {

               final TourMarker existingTourMarker = allExistingMarkers.get(existingMarkerIndex);

               serieIndex_ExistingMarker = existingTourMarker.getSerieIndex();

               if (serieIndex_ExistingMarker > serieIndex_NewMarker) {

                  // this is the next marker after the current new marker

                  break;

               } else if (serieIndex_ExistingMarker == serieIndex_NewMarker) {

                  isCreateMarker = false;

                  break;
               }
            }
         }

         if (isCreateMarker) {

            final TourMarker newTourMarker = new TourMarker(tourData, ChartLabelMarker.MARKER_TYPE_CUSTOM);

            String valueText;

            if (isDistance) {

               final double markerDistanceKm = tourData.getDistanceSerieDouble()[serieIndex_NewMarker] / 1000;

               valueText = _nf1.format(markerDistanceKm);

               if (valueText.endsWith(UI.SYMBOL_ZERO)) {

                  // skip digits

                  valueText = valueText.substring(0, valueText.length() - 2);
               }

            } else {

               // by time

               final int time = tourData.timeSerie[serieIndex_NewMarker];

               valueText = net.tourbook.chart.Util.format_hh_mm(time);
            }

            boolean isWithUnit = false;
            isWithUnit = false;

            if (isWithUnit) {
               newTourMarker.setLabel("%s %s".formatted(valueText, UI.UNIT_LABEL_DISTANCE)); //$NON-NLS-1$
            } else {
               newTourMarker.setLabel("%s".formatted(valueText)); //$NON-NLS-1$
            }

            tourData.completeTourMarkerWithDataSeriesData(newTourMarker, serieIndex_NewMarker);

            allNewMarkers.add(newTourMarker);
         }
      }

      final int numNewMarkers = allNewMarkers.size();

      if (numNewMarkers > 0) {

         numCreatedMarkers[0] += numNewMarkers;

         // append new markers to existing markers
         final Set<TourMarker> allTourMarkers = tourData.getTourMarkers();
         allTourMarkers.addAll(allNewMarkers);

         allModifiedTours.add(tourData);
      }
   }

   private void createUI(final Composite parent) {

      final GridDataFactory gdSpinner = GridDataFactory.fillDefaults()

            // force fixed width
            .hint(_pc.convertWidthInCharsToPixels(5), SWT.DEFAULT);

      {
         final Long firstTourId = _allSelectedTourIDs.stream().findFirst().get();

         final String labelText = _allSelectedTourIDs.size() > 1
               ? Messages.Dialog_CreateTourMarkers_Label_Title_MultipleTours.formatted(_allSelectedTourIDs.size())
               : Messages.Dialog_CreateTourMarkers_Label_Title_OneTour.formatted(TourManager.getTourDateTimeShort(TourManager.getTour(firstTourId)));

         UI.createLabel(parent, labelText);
      }

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().indent(0, 10)
            .grab(true, false)
            .applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
      {
         {
            /*
             * By distance
             */
            _radioByDistance = new Button(container, SWT.RADIO);
            _radioByDistance.setText(Messages.Dialog_CreateTourMarkers_Radio_ByDistance);
            _radioByDistance.addSelectionListener(_defaultSelectedListener);

            _spinnerByDistance = new Spinner(container, SWT.BORDER);
            _spinnerByDistance.setDigits(1);
            _spinnerByDistance.setMinimum(0);
            _spinnerByDistance.setMaximum(1000); // 100.0 km
            _spinnerByDistance.setIncrement(1);
            _spinnerByDistance.setPageIncrement(10);
            _spinnerByDistance.addSelectionListener(_defaultSelectedListener);
            _spinnerByDistance.addMouseWheelListener(_defaultMouseWheelListener);
            gdSpinner.applyTo(_spinnerByDistance);

            _labelDistanceUnit = new Label(container, SWT.NONE);
            _labelDistanceUnit.setText(UI.UNIT_LABEL_DISTANCE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_labelDistanceUnit);
         }
         {
            /*
             * By time
             */
            _radioByTime = new Button(container, SWT.RADIO);
            _radioByTime.setText(Messages.Dialog_CreateTourMarkers_Radio_ByTime);
            _radioByTime.addSelectionListener(_defaultSelectedListener);

            _spinnerByTime = new Spinner(container, SWT.BORDER);
            _spinnerByTime.setMinimum(0);
            _spinnerByTime.setMaximum(24 * 60); // 10 minutes
            _spinnerByTime.setIncrement(1);
            _spinnerByTime.setPageIncrement(10);
            _spinnerByTime.addSelectionListener(_defaultSelectedListener);
            _spinnerByTime.addMouseWheelListener(_defaultMouseWheelListener);
            gdSpinner.applyTo(_spinnerByTime);

            _labelTimeUnit = new Label(container, SWT.NONE);
            _labelTimeUnit.setText(Messages.App_Unit_Minute);
            GridDataFactory.fillDefaults().applyTo(_labelTimeUnit);
         }
      }
   }

   private void enableControls() {

      final boolean isDistanceSelected = _radioByDistance.getSelection();
      final boolean isTimeSelected = !isDistanceSelected;

      boolean isDistanceValid = false;
      boolean isTimeValid = false;

      if (isTimeSelected) {

         isTimeValid = _spinnerByTime.getSelection() > 0;

      } else {

         isDistanceValid = _spinnerByDistance.getSelection() > 0;
      }

      _labelDistanceUnit.setEnabled(isDistanceSelected);
      _labelTimeUnit.setEnabled(isTimeSelected);

      _spinnerByDistance.setEnabled(isDistanceSelected);
      _spinnerByTime.setEnabled(isTimeSelected);

      getButton(IDialogConstants.OK_ID).setEnabled(isTimeSelected ? isTimeValid : isDistanceValid);
   }

   @Override
   protected IDialogSettings getDialogBoundsSettings() {

      // keep only window position
      return _state;
   }

   @Override
   protected int getDialogBoundsStrategy() {

      return DIALOG_PERSISTLOCATION;
   }

   /**
    * @return Returns distance in meters from the distance spinner control
    *         <p>
    *         spinner 1.0 != 1.0 mile, it is 1 1/4 mile
    */
   private float getSelectedDistance() {

      final float selectedDistance = _spinnerByDistance.getSelection();

      float distanceMeter;

      if (UI.UNIT_IS_DISTANCE_MILE) {

         // mile

         distanceMeter = selectedDistance * 1000 / 8;

         // convert mile -> meters
         distanceMeter *= UI.UNIT_MILE;

      } else if (UI.UNIT_IS_DISTANCE_NAUTICAL_MILE) {

         // nautical mile

         distanceMeter = selectedDistance * 1000 / 8;

         // convert nautical mile -> meters
         distanceMeter *= UI.UNIT_NAUTICAL_MILE;

      } else {

         // km (metric)

         distanceMeter = selectedDistance * 100;
      }

      return distanceMeter;
   }

   public IDialogSettings getState() {
      return _state;
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      _defaultSelectedListener = SelectionListener.widgetSelectedAdapter(selectionEvent -> onChangeUI());

      _defaultMouseWheelListener = mouseEvent -> {
         UI.adjustSpinnerValueOnMouseScroll(mouseEvent);
         onChangeUI();
      };
   }

   @Override
   protected void okPressed() {

      createTourMarkers();

      super.okPressed();
   }

   private void onChangeUI() {

      updateUI_Distance();

      enableControls();
   }

   private void restoreState() {

      final boolean isByDistance = Util.getStateBoolean(_state, STATE_IS_BY_DISTANCE, true);

      _radioByDistance.setSelection(isByDistance);
      _radioByTime.setSelection(!isByDistance);

      _spinnerByDistance.setSelection(Util.getStateInt(_state, STATE_DISTANCE_VALUE, 10));
      _spinnerByTime.setSelection(Util.getStateInt(_state, STATE_TIME_VALUE, 60));
   }

   private void saveState() {

      _state.put(STATE_IS_BY_DISTANCE, _radioByDistance.getSelection());

      _state.put(STATE_DISTANCE_VALUE, _spinnerByDistance.getSelection());
      _state.put(STATE_TIME_VALUE, _spinnerByTime.getSelection());
   }

   /**
    * This is overwritten because the default width for the OK button is too small
    *
    * @param button
    *           The button which layout data is to be set.
    */
   @Override
   protected void setButtonLayoutData(final Button button) {

      int okButtonWidth;

      // get OK button width, the default is too small
      final GC gc = new GC(button);
      {
         okButtonWidth = gc.textExtent(Messages.Dialog_CreateTourMarkers_Button_ValidateMarkers).x;
      }
      gc.dispose();

      final GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
      data.widthHint = okButtonWidth;

      button.setLayoutData(data);
   }

   private void updateUI_Distance() {

      final float spinnerDistanceKm = getSelectedDistance() / UI.UNIT_VALUE_DISTANCE;

      if (UI.UNIT_IS_DISTANCE_MILE) {

         // mile

         // update UI
         _labelDistanceUnit.setText(UI.convertKmIntoMiles(spinnerDistanceKm));

      } else {

         // metric + nautical mile

         // update UI, the spinner already displays the correct value
         _labelDistanceUnit.setText(UI.UNIT_LABEL_DISTANCE);
      }
   }
}
