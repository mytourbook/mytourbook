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
package net.tourbook.tour.location;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.MtMath;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourLocation;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;

public class DialogResizeTourLocation extends TitleAreaDialog {

// SET_FORMATTING_OFF

   private static final IDialogSettings _state     = TourbookPlugin.getState("net.tourbook.tour.location.DialogResizeTourLocation");   //$NON-NLS-1$

// SET_FORMATTING_ON

   private static final String STATE_IS_LIVE_UPDATE          = "STATE_IS_LIVE_UPDATE";          //$NON-NLS-1$
   private static final String STATE_MOUSE_WHEEL_INCREMENTER = "STATE_MOUSE_WHEEL_INCREMENTER"; //$NON-NLS-1$

   private PixelConverter      _pc;

   private TourLocation        _tourLocation;

   private int                 _mouseWheelIncrementer;
   private boolean             _isLiveUpdate;

   private MouseWheelListener  _defaultMouseWheelListener;
   private SelectionListener   _defaultSelectionListener;

   private Action_ResetValue   _actionResetValue_North;
   private Action_ResetValue   _actionResetValue_South;
   private Action_ResetValue   _actionResetValue_West;
   private Action_ResetValue   _actionResetValue_East;

   /*
    * UI controls
    */
   private Combo            _comboMouseWheelIncrementer;

   private Button           _chkLiveUpdate;

   private Label            _lblDistance_North;
   private Label            _lblDistance_South;
   private Label            _lblDistance_West;
   private Label            _lblDistance_East;

   private Spinner          _spinnerDistance_North;
   private Spinner          _spinnerDistance_South;
   private Spinner          _spinnerDistance_West;
   private Spinner          _spinnerDistance_East;

   private TourLocationView _tourLocationView;

   /**
    * Reset spinner value
    */
   private class Action_ResetValue extends Action {

      private Spinner _spinner;

      public Action_ResetValue(final Spinner spinner) {

         super(UI.RESET_LABEL, AS_PUSH_BUTTON);

         setToolTipText(Messages.Dialog_ResizeTourLocation_Action_ResetValue_Tooltip);

         _spinner = spinner;
      }

      @Override
      public void run() {

         onResetValue(_spinner);
      }

   }

   public DialogResizeTourLocation(final TourLocationView tourLocationView, final TourLocation tourLocation) {

      super(Display.getDefault().getActiveShell());

      _tourLocationView = tourLocationView;
      _tourLocation = tourLocation;
   }

   @Override
   protected void configureShell(final Shell shell) {

      super.configureShell(shell);

      // set window title
      shell.setText(Messages.Dialog_ResizeTourLocation_Title);

      shell.addDisposeListener(disposeEvent -> dispose());
   }

   @Override
   public void create() {

      super.create();

      setTitle(Messages.Dialog_ResizeTourLocation_Title);
   }

   @Override
   protected void createButtonsForButtonBar(final Composite parent) {

      // OK -> Goto Location
      createButton(parent, IDialogConstants.OK_ID, Messages.Dialog_ResizeTourLocation_Action_Resize, true);

      // create close button
      createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      initUI(parent);

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(shellContainer);
      GridLayoutFactory.swtDefaults().numColumns(1).applyTo(shellContainer);
//      shellContainer.setBackground(UI.SYS_COLOR_BLUE);
      {
         createUI_10_BoundingBox(shellContainer);
         createUI_20_LiveUpdate(shellContainer);
      }

      fillUI();

      // wait until the OK button is also created, otherwise it is null
      parent.getDisplay().asyncExec(() -> {

         restoreState();
         enableControls();
      });

      return shellContainer;
   }

   private Composite createUI_10_BoundingBox(final Composite parent) {

      final GridDataFactory gdCurrentValue = GridDataFactory.fillDefaults()
            .grab(false, false)
            .hint(_pc.convertWidthInCharsToPixels(8), SWT.DEFAULT)
            .align(SWT.TRAIL, SWT.CENTER);

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(6).applyTo(container);
      {
         {
            /*
             * North
             */
            UI.createLabel(container, Messages.Dialog_ResizeTourLocation_Label_Orientation_North);

            // Spinner
            _spinnerDistance_North = new Spinner(container, SWT.BORDER);
            _spinnerDistance_North.setMinimum(1);
            _spinnerDistance_North.setMaximum(10_000_000);
            _spinnerDistance_North.addSelectionListener(_defaultSelectionListener);
            _spinnerDistance_North.addMouseWheelListener(_defaultMouseWheelListener);
            GridDataFactory.fillDefaults().applyTo(_spinnerDistance_North);

            // m / yd
            UI.createLabel(container, UI.UNIT_LABEL_DISTANCE_M_OR_YD);

            // action: Reset value
            _actionResetValue_North = createUI_Action_ResetValue(container, _spinnerDistance_North);

            // current value
            _lblDistance_North = UI.createLabel(container, UI.EMPTY_STRING, SWT.TRAIL);
            gdCurrentValue.applyTo(_lblDistance_North);

            // m / yd
            UI.createLabel(container, UI.UNIT_LABEL_DISTANCE_M_OR_YD);
         }
         {
            /*
             * West
             */
            UI.createLabel(container, Messages.Dialog_ResizeTourLocation_Label_Orientation_West);

            // Spinner
            _spinnerDistance_West = new Spinner(container, SWT.BORDER);
            _spinnerDistance_West.setMinimum(1);
            _spinnerDistance_West.setMaximum(10_000_000);
            _spinnerDistance_West.addSelectionListener(_defaultSelectionListener);
            _spinnerDistance_West.addMouseWheelListener(_defaultMouseWheelListener);
            GridDataFactory.fillDefaults().applyTo(_spinnerDistance_West);

            // m / yd
            UI.createLabel(container, UI.UNIT_LABEL_DISTANCE_M_OR_YD);

            // action: Reset value
            _actionResetValue_West = createUI_Action_ResetValue(container, _spinnerDistance_West);

            // current value
            _lblDistance_West = UI.createLabel(container, UI.EMPTY_STRING, SWT.TRAIL);
            gdCurrentValue.applyTo(_lblDistance_West);

            // m / yd
            UI.createLabel(container, UI.UNIT_LABEL_DISTANCE_M_OR_YD);
         }
         {
            /*
             * East
             */
            UI.createLabel(container, Messages.Dialog_ResizeTourLocation_Label_Orientation_East);

            // Spinner
            _spinnerDistance_East = new Spinner(container, SWT.BORDER);
            _spinnerDistance_East.setMinimum(1);
            _spinnerDistance_East.setMaximum(10_000_000);
            _spinnerDistance_East.addSelectionListener(_defaultSelectionListener);
            _spinnerDistance_East.addMouseWheelListener(_defaultMouseWheelListener);
            GridDataFactory.fillDefaults().applyTo(_spinnerDistance_East);

            // m / yd
            UI.createLabel(container, UI.UNIT_LABEL_DISTANCE_M_OR_YD);

            // action: Reset value
            _actionResetValue_East = createUI_Action_ResetValue(container, _spinnerDistance_East);

            // current value
            _lblDistance_East = UI.createLabel(container, UI.EMPTY_STRING, SWT.TRAIL);
            gdCurrentValue.applyTo(_lblDistance_East);

            // m / yd
            UI.createLabel(container, UI.UNIT_LABEL_DISTANCE_M_OR_YD);
         }
         {
            /*
             * South
             */
            UI.createLabel(container, Messages.Dialog_ResizeTourLocation_Label_Orientation_South);

            // Spinner
            _spinnerDistance_South = new Spinner(container, SWT.BORDER);
            _spinnerDistance_South.setMinimum(1);
            _spinnerDistance_South.setMaximum(10_000_000);
            _spinnerDistance_South.addSelectionListener(_defaultSelectionListener);
            _spinnerDistance_South.addMouseWheelListener(_defaultMouseWheelListener);
            GridDataFactory.fillDefaults().applyTo(_spinnerDistance_South);

            // m / yd
            UI.createLabel(container, UI.UNIT_LABEL_DISTANCE_M_OR_YD);

            // action: Reset value
            _actionResetValue_South = createUI_Action_ResetValue(container, _spinnerDistance_South);

            // current value
            _lblDistance_South = UI.createLabel(container, UI.EMPTY_STRING, SWT.TRAIL);
            gdCurrentValue.applyTo(_lblDistance_South);

            // m / yd
            UI.createLabel(container, UI.UNIT_LABEL_DISTANCE_M_OR_YD);
         }

         UI.createSpacer_Horizontal(container, 6);

         {
            /*
             * Page values
             */

            final String tooltip = Messages.Dialog_ResizeTourLocation_Label_PageValue_Tooltip;

            UI.createLabel(container, Messages.Dialog_ResizeTourLocation_Label_PageValue, tooltip);

            // Combo: Mouse wheel incrementer
            _comboMouseWheelIncrementer = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
            _comboMouseWheelIncrementer.setVisibleItemCount(10);
            _comboMouseWheelIncrementer.setToolTipText(tooltip);

            _comboMouseWheelIncrementer.addSelectionListener(SelectionListener.widgetSelectedAdapter(
                  selectionEvent -> onMouseWheelIncrementer()));

            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_comboMouseWheelIncrementer);

            // m / yd
            UI.createLabel(container, UI.UNIT_LABEL_DISTANCE_M_OR_YD);
         }
      }
      return container;
   }

   private void createUI_20_LiveUpdate(final Composite parent) {
      // TODO Auto-generated method stub

      {
         /*
          * Checkbox: live update
          */
         _chkLiveUpdate = new Button(parent, SWT.CHECK);
         _chkLiveUpdate.setText(Messages.Slideout_TourFilter_Checkbox_IsLiveUpdate);
         _chkLiveUpdate.setToolTipText(Messages.Slideout_TourFilter_Checkbox_IsLiveUpdate_Tooltip);
         _chkLiveUpdate.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> doLiveUpdate()));

         GridDataFactory.fillDefaults()
               .grab(true, false)
               .align(SWT.END, SWT.CENTER)
               .applyTo(_chkLiveUpdate);
      }
   }

   private Action_ResetValue createUI_Action_ResetValue(final Composite parent, final Spinner spinner) {

      final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);

      final ToolBarManager tbm = new ToolBarManager(toolbar);
      final Action_ResetValue action = new Action_ResetValue(spinner);

      tbm.add(action);
      tbm.update(true);

      return action;
   }

   private void dispose() {

   }

   private void doLiveUpdate() {

      _isLiveUpdate = _chkLiveUpdate.getSelection();

      _state.put(STATE_IS_LIVE_UPDATE, _isLiveUpdate);

      enableControls();

      fireModifyEvent();
   }

   private void enableControls() {

   }

   private void fillUI() {

      /*
       * Fill in the same order as the mouse wheel is increasing/decreasing the spinner value,
       * otherwise it is in the opposite direction which is confusing !!!
       */
      _comboMouseWheelIncrementer.add(UI.INCREMENTER_100_000);
      _comboMouseWheelIncrementer.add(UI.INCREMENTER_10_000);
      _comboMouseWheelIncrementer.add(UI.INCREMENTER_1_000);
      _comboMouseWheelIncrementer.add(UI.INCREMENTER_100);
      _comboMouseWheelIncrementer.add(UI.INCREMENTER_10);
      _comboMouseWheelIncrementer.add(UI.INCREMENTER_1);
   }

   private void fireModifyEvent() {

      if (_isLiveUpdate) {

         _tourLocationView.setResizedBoundingBox(_tourLocation.getLocationId());
      }
   }

   @Override
   protected IDialogSettings getDialogBoundsSettings() {

      // keep window size and position
      return _state;

      // for debugging: test default position/size
//    return null;
   }

   @Override
   protected int getDialogBoundsStrategy() {

      // persist only the location, dialog cannot be resized
      return DIALOG_PERSISTLOCATION;
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      _defaultSelectionListener = SelectionListener.widgetSelectedAdapter(selectionEvent -> onResizeBoundingBox());

      _defaultMouseWheelListener = mouseEvent -> {
         UI.adjustSpinnerValueOnMouseScroll(mouseEvent, _mouseWheelIncrementer);
         onResizeBoundingBox();
      };

   }

   /**
    * Get combo index from selected value
    *
    * @return
    */
   private int mouseWheelIncrementer_GetIndex() {

// SET_FORMATTING_OFF

      if (        _mouseWheelIncrementer ==      1) {    return 5;
      } else if ( _mouseWheelIncrementer ==     10) {    return 4;
      } else if ( _mouseWheelIncrementer ==    100) {    return 3;
      } else if ( _mouseWheelIncrementer ==  1_000) {    return 2;
      } else if ( _mouseWheelIncrementer == 10_000) {    return 1;
      } else {                                           return 0;      // 100_000
      }

// SET_FORMATTING_ON
   }

   /**
    * Set value from selected index
    *
    * @param selectionIndex
    */
   private void mouseWheelIncrementer_SetValue(final int selectionIndex) {

// SET_FORMATTING_OFF

      if (selectionIndex == 0) {             _mouseWheelIncrementer = 100_000;
      } else if (selectionIndex == 1) {      _mouseWheelIncrementer =  10_000;
      } else if (selectionIndex == 2) {      _mouseWheelIncrementer =   1_000;
      } else if (selectionIndex == 3) {      _mouseWheelIncrementer =     100;
      } else if (selectionIndex == 4) {      _mouseWheelIncrementer =      10;
      } else {                               _mouseWheelIncrementer =       1;
      }

// SET_FORMATTING_ON
   }

   @Override
   protected void okPressed() {

      saveState();

      super.okPressed();
   }

   private void onMouseWheelIncrementer() {

      final int selectionIndex = _comboMouseWheelIncrementer.getSelectionIndex();

      mouseWheelIncrementer_SetValue(selectionIndex);

      _spinnerDistance_North.setPageIncrement(_mouseWheelIncrementer);
      _spinnerDistance_South.setPageIncrement(_mouseWheelIncrementer);
      _spinnerDistance_West.setPageIncrement(_mouseWheelIncrementer);
      _spinnerDistance_East.setPageIncrement(_mouseWheelIncrementer);
   }

   private void onResetValue(final Spinner _spinner2) {
      // TODO Auto-generated method stub

      fireModifyEvent();
   }

   private void onResizeBoundingBox() {
      // TODO Auto-generated method stub

      fireModifyEvent();
   }

   private void restoreState() {

      updateUI();

      _mouseWheelIncrementer = Util.getStateInt(_state, STATE_MOUSE_WHEEL_INCREMENTER, 100);

      _comboMouseWheelIncrementer.select(mouseWheelIncrementer_GetIndex());

      _isLiveUpdate = Util.getStateBoolean(_state, STATE_IS_LIVE_UPDATE, false);
      _chkLiveUpdate.setSelection(_isLiveUpdate);
   }

   private void saveState() {

      _state.put(STATE_MOUSE_WHEEL_INCREMENTER, _mouseWheelIncrementer);
   }

   private void updateUI() {
      // TODO Auto-generated method stub

// SET_FORMATTING_OFF

      final int latitudeE6_Normalized              = _tourLocation.latitudeE6_Normalized;
      final int longitudeE6_Normalized             = _tourLocation.longitudeE6_Normalized;

      final int latitudeMinE6_Normalized           = _tourLocation.latitudeMinE6_Normalized;
      final int latitudeMaxE6_Normalized           = _tourLocation.latitudeMaxE6_Normalized;
      final int longitudeMinE6_Normalized          = _tourLocation.longitudeMinE6_Normalized;
      final int longitudeMaxE6_Normalized          = _tourLocation.longitudeMaxE6_Normalized;

      final int latitudeMinE6_Resized_Normalized   = _tourLocation.latitudeMinE6_Resized_Normalized;
      final int latitudeMaxE6_Resized_Normalized   = _tourLocation.latitudeMaxE6_Resized_Normalized;
      final int longitudeMinE6_Resized_Normalized  = _tourLocation.longitudeMinE6_Resized_Normalized;
      final int longitudeMaxE6_Resized_Normalized  = _tourLocation.longitudeMaxE6_Resized_Normalized;

      final double latitude                        = _tourLocation.latitude;
      final double longitude                       = _tourLocation.longitude;

      final double latitudeMin                     = _tourLocation.latitudeMin;
      final double latitudeMax                     = _tourLocation.latitudeMax;
      final double longitudeMin                    = _tourLocation.longitudeMin;
      final double longitudeMax                    = _tourLocation.longitudeMax;

      final double latitudeMin_Resized             = _tourLocation.latitudeMin_Resized;
      final double latitudeMax_Resized             = _tourLocation.latitudeMax_Resized;
      final double longitudeMin_Resized            = _tourLocation.longitudeMin_Resized;
      final double longitudeMax_Resized            = _tourLocation.longitudeMax_Resized;

// SET_FORMATTING_ON

      final int latitudeDiff_Normalized =

            latitudeE6_Normalized < latitudeMinE6_Normalized

                  ? latitudeE6_Normalized - latitudeMinE6_Normalized
                  : latitudeE6_Normalized > latitudeMaxE6_Normalized

                        ? latitudeE6_Normalized - latitudeMaxE6_Normalized
                        : 0;

      final int longitudeDiff_Normalized =

            longitudeE6_Normalized < longitudeMinE6_Normalized

                  ? longitudeE6_Normalized - longitudeMinE6_Normalized
                  : longitudeE6_Normalized > longitudeMaxE6_Normalized

                        ? longitudeE6_Normalized - longitudeMaxE6_Normalized
                        : 0;

      final int latitudeHeight_Normalized = latitudeMaxE6_Resized_Normalized - latitudeMinE6_Resized_Normalized;
      final int longitudeWidth_Normalized = longitudeMaxE6_Resized_Normalized - longitudeMinE6_Resized_Normalized;

      final double bboxHeight_Distance = MtMath.distanceVincenty(

            latitudeMin_Resized,
            longitudeMin_Resized,
            latitudeMax_Resized,
            longitudeMin_Resized

      ) / UI.UNIT_VALUE_DISTANCE_SMALL;

      final double bboxWidth_Distance = MtMath.distanceVincenty(

            latitudeMin_Resized,
            longitudeMin_Resized,
            latitudeMin_Resized,
            longitudeMax_Resized

      ) / UI.UNIT_VALUE_DISTANCE_SMALL;

      final double latitudeDiff_Distance = MtMath.distanceVincenty(

            latitude,
            longitude,

            latitude + (latitudeDiff_Normalized / 10e5),
            longitude

      ) / UI.UNIT_VALUE_DISTANCE_SMALL;

      final double longitudeDiff_Distance = MtMath.distanceVincenty(

            latitude,
            longitude,

            latitude,
            longitude + (longitudeDiff_Normalized / 10e5)

      ) / UI.UNIT_VALUE_DISTANCE_SMALL;

      // create formatted text
      final String latDiffText = latitudeDiff_Normalized == 0

            ? UI.EMPTY_STRING
            : latitudeDiff_Normalized < 0

                  ? UI.DASH + Integer.toString((int) (latitudeDiff_Distance + 0.5))
                  : Integer.toString((int) (latitudeDiff_Distance + 0.5));

      final String lonDiffText = longitudeDiff_Normalized == 0

            ? UI.EMPTY_STRING
            : longitudeDiff_Normalized < 0

                  ? UI.DASH + Integer.toString((int) (longitudeDiff_Distance + 0.5))
                  : Integer.toString((int) (longitudeDiff_Distance + 0.5));

      final int latitudeDiff_Value = latitudeDiff_Normalized;
      final int longitudeDiff_Value = longitudeDiff_Normalized;

      final String latitudeDiff_Text = latDiffText;
      final String longitudeDiff_Text = lonDiffText;

      final int boundingBoxHeight_Value = latitudeHeight_Normalized;
      final int boundingBoxWidth_Value = longitudeWidth_Normalized;

      final String boundingBoxHeight_Text = Integer.toString((int) (bboxHeight_Distance + 0.5));
      final String boundingBoxWidth_Text = Integer.toString((int) (bboxWidth_Distance + 0.5));

      _lblDistance_North.setText(boundingBoxHeight_Text);
      _lblDistance_West.setText(boundingBoxWidth_Text);
   }

}
