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

import java.util.Arrays;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.formatter.FormatManager;
import net.tourbook.common.map.GeoPosition;
import net.tourbook.common.util.MtMath;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourLocation;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;

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

   private static final String BOUNDING_BOX_SIZE                     = "%s x %s %s";                    //$NON-NLS-1$

   private static final String STATE_MOUSE_WHEEL_INCREMENTER         = "STATE_MOUSE_WHEEL_INCREMENTER"; //$NON-NLS-1$
   private static final int    STATE_MOUSE_WHEEL_INCREMENTER_DEFAULT = 10;
   private static final String STATE_RESIZE_ALL_TOGETHER             = "STATE_RESIZE_ALL_TOGETHER";     //$NON-NLS-1$

   private PixelConverter      _pc;

   private TourLocationView    _tourLocationView;
   private TourLocation        _tourLocation;

   private int                 _mouseWheelIncrementer;

   private MouseWheelListener  _defaultMouseWheelListener;
   private SelectionListener   _defaultSelectionListener;

   private Action_ResetValue   _actionResetValue_Top;
   private Action_ResetValue   _actionResetValue_Bottom;
   private Action_ResetValue   _actionResetValue_Left;
   private Action_ResetValue   _actionResetValue_Right;

   private int                 _currentDistance_Top;
   private int                 _currentDistance_Bottom;
   private int                 _currentDistance_Left;
   private int                 _currentDistance_Right;

   /*
    * UI controls
    */
   private Combo   _comboMouseWheelIncrementer;

   private Button  _btnIncludeGeoPosition;
   private Button  _btnRelocateBBox;
   private Button  _btnResetBBox;

   private Button  _chkOneForAll4;

   private Label   _lblBBoxDefaultSize;
   private Label   _lblBBoxResizedSize;
   private Label   _lblDistance_Top;
   private Label   _lblDistance_Bottom;
   private Label   _lblDistance_Left;
   private Label   _lblDistance_Right;

   private Spinner _spinnerDistance_Top;
   private Spinner _spinnerDistance_Bottom;
   private Spinner _spinnerDistance_Left;
   private Spinner _spinnerDistance_Right;

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
      setMessage(Messages.Dialog_ResizeTourLocation_Message);
   }

   @Override
   protected void createButtonsForButtonBar(final Composite parent) {

      // OK -> Resize
//    _btnResize = createButton(parent, IDialogConstants.OK_ID, Messages.Dialog_ResizeTourLocation_Action_Resize, true);

      // create close button
      createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      initUI(parent);

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(shellContainer);
      GridLayoutFactory.swtDefaults().margins(6, 6).numColumns(2).applyTo(shellContainer);
//      shellContainer.setBackground(UI.SYS_COLOR_GREEN);
      {
         createUI_10_LocationName(shellContainer);

         UI.createSpacer_Horizontal(shellContainer, 2);

         final Composite container = new Composite(shellContainer, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
         {
            createUI_20_BoundingBox(container);
            createUI_30_OneForAll4(container);
            createUI_40_Size(container);
         }

         createUI_50_Actions(shellContainer);
      }

      fillUI();

      restoreState();

      enableControls();

      _spinnerDistance_Top.setFocus();

      return shellContainer;
   }

   private void createUI_10_LocationName(final Composite parent) {

      final Label label = UI.createLabel(parent, _tourLocation.display_name, SWT.WRAP);
      GridDataFactory.fillDefaults()
            .span(2, 1)
            .hint(_pc.convertWidthInCharsToPixels(40), SWT.DEFAULT)
            .grab(true, false)
            .applyTo(label);

   }

   private void createUI_20_BoundingBox(final Composite parent) {

      final GridDataFactory gdCurrentValue = GridDataFactory.fillDefaults()
            .grab(false, false)
            .hint(_pc.convertWidthInCharsToPixels(8), SWT.DEFAULT)
            .align(SWT.TRAIL, SWT.CENTER);

      final int minDistance = -10_000_000;
      final int maxDistance = +10_000_000;

      final Composite containerTopBottom = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(containerTopBottom);
      GridLayoutFactory.fillDefaults().numColumns(5).applyTo(containerTopBottom);
      {
         {
            /*
             * Top
             */
            UI.createLabel(containerTopBottom, Messages.Dialog_ResizeTourLocation_Label_Orientation_Top);

            // Spinner
            _spinnerDistance_Top = new Spinner(containerTopBottom, SWT.BORDER);
            _spinnerDistance_Top.setMinimum(minDistance);
            _spinnerDistance_Top.setMaximum(maxDistance);
            _spinnerDistance_Top.addSelectionListener(_defaultSelectionListener);
            _spinnerDistance_Top.addMouseWheelListener(_defaultMouseWheelListener);
            GridDataFactory.fillDefaults().applyTo(_spinnerDistance_Top);

            // m / yd
            UI.createLabel(containerTopBottom, UI.UNIT_LABEL_DISTANCE_M_OR_YD);

            // action: Reset value
            _actionResetValue_Top = createUI_Action_ResetValue(containerTopBottom, _spinnerDistance_Top);

            // current value
            _lblDistance_Top = UI.createLabel(containerTopBottom, UI.EMPTY_STRING, SWT.TRAIL);
            gdCurrentValue.applyTo(_lblDistance_Top);
         }
         {
            /*
             * Bottom
             */
            UI.createLabel(containerTopBottom, Messages.Dialog_ResizeTourLocation_Label_Orientation_Bottom);

            // Spinner
            _spinnerDistance_Bottom = new Spinner(containerTopBottom, SWT.BORDER);
            _spinnerDistance_Bottom.setMinimum(minDistance);
            _spinnerDistance_Bottom.setMaximum(maxDistance);
            _spinnerDistance_Bottom.addSelectionListener(_defaultSelectionListener);
            _spinnerDistance_Bottom.addMouseWheelListener(_defaultMouseWheelListener);
            GridDataFactory.fillDefaults().applyTo(_spinnerDistance_Bottom);

            // m / yd
            UI.createLabel(containerTopBottom, UI.UNIT_LABEL_DISTANCE_M_OR_YD);

            // action: Reset value
            _actionResetValue_Bottom = createUI_Action_ResetValue(containerTopBottom, _spinnerDistance_Bottom);

            // current value
            _lblDistance_Bottom = UI.createLabel(containerTopBottom, UI.EMPTY_STRING, SWT.TRAIL);
            gdCurrentValue.applyTo(_lblDistance_Bottom);
         }
      }

      final Composite containerLeftRight = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(containerLeftRight);
      GridLayoutFactory.fillDefaults().numColumns(5).applyTo(containerLeftRight);
      {
         {
            /*
             * Left
             */
            UI.createLabel(containerLeftRight, Messages.Dialog_ResizeTourLocation_Label_Orientation_Left);

            // Spinner
            _spinnerDistance_Left = new Spinner(containerLeftRight, SWT.BORDER);
            _spinnerDistance_Left.setMinimum(minDistance);
            _spinnerDistance_Left.setMaximum(maxDistance);
            _spinnerDistance_Left.addSelectionListener(_defaultSelectionListener);
            _spinnerDistance_Left.addMouseWheelListener(_defaultMouseWheelListener);
            GridDataFactory.fillDefaults().applyTo(_spinnerDistance_Left);

            // m / yd
            UI.createLabel(containerLeftRight, UI.UNIT_LABEL_DISTANCE_M_OR_YD);

            // action: Reset value
            _actionResetValue_Left = createUI_Action_ResetValue(containerLeftRight, _spinnerDistance_Left);

            // current value
            _lblDistance_Left = UI.createLabel(containerLeftRight, UI.EMPTY_STRING, SWT.TRAIL);
            gdCurrentValue.applyTo(_lblDistance_Left);
         }
         {
            /*
             * Right
             */
            UI.createLabel(containerLeftRight, Messages.Dialog_ResizeTourLocation_Label_Orientation_Right);

            // Spinner
            _spinnerDistance_Right = new Spinner(containerLeftRight, SWT.BORDER);
            _spinnerDistance_Right.setMinimum(minDistance);
            _spinnerDistance_Right.setMaximum(maxDistance);
            _spinnerDistance_Right.addSelectionListener(_defaultSelectionListener);
            _spinnerDistance_Right.addMouseWheelListener(_defaultMouseWheelListener);
            GridDataFactory.fillDefaults().applyTo(_spinnerDistance_Right);

            // m / yd
            UI.createLabel(containerLeftRight, UI.UNIT_LABEL_DISTANCE_M_OR_YD);

            // action: Reset value
            _actionResetValue_Right = createUI_Action_ResetValue(containerLeftRight, _spinnerDistance_Right);

            // current value
            _lblDistance_Right = UI.createLabel(containerLeftRight, UI.EMPTY_STRING, SWT.TRAIL);
            gdCurrentValue.applyTo(_lblDistance_Right);
         }
      }
   }

   private void createUI_30_OneForAll4(final Composite parent) {

      UI.createSpacer_Horizontal(parent, 2);

      _chkOneForAll4 = new Button(parent, SWT.CHECK);
      _chkOneForAll4.setText(Messages.Dialog_ResizeTourLocation_Checkbox_ResizeAllTogether);
      _chkOneForAll4.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> saveState()));

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .span(2, 1)
            .grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
      {
         {
            /*
             * Page values
             */

            final String tooltip = Messages.Dialog_ResizeTourLocation_Label_PageValue_Tooltip;

            UI.createLabel(container, Messages.Dialog_ResizeTourLocation_Label_IncrementDecrementValue, tooltip);

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
   }

   private void createUI_40_Size(final Composite parent) {

      UI.createSpacer_Horizontal(parent, 3);

      final Composite containerLeft = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(containerLeft);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerLeft);
      {
         {
            /*
             * Resized bounding box size
             */
            UI.createLabel(containerLeft, Messages.Dialog_ResizeTourLocation_Label_BoundingBox_ResizedSize);

            _lblBBoxResizedSize = UI.createLabel(containerLeft, UI.EMPTY_STRING);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblBBoxResizedSize);
         }
      }

      final Composite containerRight = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(containerRight);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerRight);
      {
         {
            /*
             * Default bounding box size
             */
            UI.createLabel(containerRight, Messages.Dialog_ResizeTourLocation_Label_BoundingBox_DefaultSize);

            _lblBBoxDefaultSize = UI.createLabel(containerRight, UI.EMPTY_STRING);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
//                  .hint(_pc.convertWidthInCharsToPixels(20), SWT.DEFAULT)
                  .applyTo(_lblBBoxDefaultSize);
         }
      }
   }

   private void createUI_50_Actions(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//    container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
         /*
          * Include
          */
         _btnIncludeGeoPosition = new Button(container, SWT.NONE);
         _btnIncludeGeoPosition.setText(Messages.Tour_Location_Action_IncludeGeoPosition_Short);
         _btnIncludeGeoPosition.setToolTipText(Messages.Tour_Location_Action_IncludeGeoPosition_Tooltip);
         _btnIncludeGeoPosition.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onBoundingBox_IncludeGeoPos()));
         setButtonLayoutData(_btnIncludeGeoPosition);

         /*
          * Relocate
          */
         _btnRelocateBBox = new Button(container, SWT.NONE);
         _btnRelocateBBox.setText(Messages.Tour_Location_Action_RelocateBoundingBox_Short);
         _btnRelocateBBox.setToolTipText(Messages.Tour_Location_Action_RelocateBoundingBox_Tooltip);
         _btnRelocateBBox.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onBoundingBox_Relocate()));
         setButtonLayoutData(_btnRelocateBBox);

         /*
          * Reset
          */
         _btnResetBBox = new Button(container, SWT.NONE);
         _btnResetBBox.setText(Messages.Tour_Location_Action_ResetBoundingBox_Short);
         _btnResetBBox.setToolTipText(Messages.Tour_Location_Action_ResetBoundingBox_Tooltip);
         _btnResetBBox.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onBoundingBox_Reset()));
         setButtonLayoutData(_btnResetBBox);
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

   private void enableControls() {

      final boolean canReset_Top = _spinnerDistance_Top.getSelection() != 0;
      final boolean canReset_Bottom = _spinnerDistance_Bottom.getSelection() != 0;
      final boolean canReset_Left = _spinnerDistance_Left.getSelection() != 0;
      final boolean canReset_Right = _spinnerDistance_Right.getSelection() != 0;

      _actionResetValue_Top.setEnabled(canReset_Top);
      _actionResetValue_Bottom.setEnabled(canReset_Bottom);
      _actionResetValue_Left.setEnabled(canReset_Left);
      _actionResetValue_Right.setEnabled(canReset_Right);
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

      saveState();

      TourManager.fireEventWithCustomData(

            TourEventId.TOUR_LOCATION_SELECTION,
            Arrays.asList(_tourLocation),
            null);
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

      _defaultSelectionListener = SelectionListener.widgetSelectedAdapter(selectionEvent -> onBoundingBox_Resize());

      _defaultMouseWheelListener = mouseEvent -> {
         UI.adjustSpinnerValueOnMouseScroll(mouseEvent, _mouseWheelIncrementer);
         onBoundingBox_Resize();
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

      // OK button is not available, all changes are live updated

      super.okPressed();
   }

   private void onBoundingBox_IncludeGeoPos() {

      TourLocationManager.setResizedBoundingBox_IncludeGeoPosition(Arrays.asList(_tourLocation));

      updateUIfromModel();

      enableControls();

      fireModifyEvent();
   }

   private void onBoundingBox_Relocate() {

      TourLocationManager.setResizedBoundingBox_Relocate(Arrays.asList(_tourLocation));

      updateUIfromModel();

      enableControls();

      fireModifyEvent();
   }

   private void onBoundingBox_Reset() {

      _currentDistance_Top = 0;
      _currentDistance_Bottom = 0;
      _currentDistance_Left = 0;
      _currentDistance_Right = 0;

      _spinnerDistance_Top.setSelection(0);
      _spinnerDistance_Bottom.setSelection(0);
      _spinnerDistance_Left.setSelection(0);
      _spinnerDistance_Right.setSelection(0);

      updateModelFromUI_AndFireEvent();
   }

   private void onBoundingBox_Resize() {

      updateModelFromUI_AndFireEvent();
   }

   private void onMouseWheelIncrementer() {

      final int selectionIndex = _comboMouseWheelIncrementer.getSelectionIndex();

      mouseWheelIncrementer_SetValue(selectionIndex);

      _spinnerDistance_Top.setPageIncrement(_mouseWheelIncrementer);
      _spinnerDistance_Bottom.setPageIncrement(_mouseWheelIncrementer);
      _spinnerDistance_Left.setPageIncrement(_mouseWheelIncrementer);
      _spinnerDistance_Right.setPageIncrement(_mouseWheelIncrementer);

      saveState();
   }

   /**
    * Reset value for one spinner control
    *
    * @param spinner
    */
   private void onResetValue(final Spinner spinner) {

      if (_chkOneForAll4.getSelection()) {

         _spinnerDistance_Top.setSelection(0);
         _spinnerDistance_Bottom.setSelection(0);
         _spinnerDistance_Left.setSelection(0);
         _spinnerDistance_Right.setSelection(0);

      } else {

         spinner.setSelection(0);
      }

      updateModelFromUI_AndFireEvent();
   }

   private void restoreState() {

      updateUIfromModel();

      _mouseWheelIncrementer = Util.getStateInt(_state, STATE_MOUSE_WHEEL_INCREMENTER, STATE_MOUSE_WHEEL_INCREMENTER_DEFAULT);

      _comboMouseWheelIncrementer.select(mouseWheelIncrementer_GetIndex());

      _chkOneForAll4.setSelection(Util.getStateBoolean(_state, STATE_RESIZE_ALL_TOGETHER, false));
   }

   private void saveState() {

      _state.put(STATE_MOUSE_WHEEL_INCREMENTER, _mouseWheelIncrementer);
      _state.put(STATE_RESIZE_ALL_TOGETHER, _chkOneForAll4.getSelection());
   }

   private void updateModelFromUI() {

// SET_FORMATTING_OFF

      int resizeValue_Top     = _spinnerDistance_Top     .getSelection();
      int resizeValue_Bottom  = _spinnerDistance_Bottom  .getSelection();
      int resizeValue_Left    = _spinnerDistance_Left    .getSelection();
      int resizeValue_Right   = _spinnerDistance_Right   .getSelection();

// SET_FORMATTING_ON

      if (_chkOneForAll4.getSelection()) {

         // increment/decrement all with the same diff value

         int valueDiff = 0;
         Spinner spinnerDiff = null;

         // ckeck which value was modified
         if (resizeValue_Top != _currentDistance_Top) {

            valueDiff = resizeValue_Top - _currentDistance_Top;
            spinnerDiff = _spinnerDistance_Top;

         } else if (resizeValue_Bottom != _currentDistance_Bottom) {

            valueDiff = resizeValue_Bottom - _currentDistance_Bottom;
            spinnerDiff = _spinnerDistance_Bottom;

         } else if (resizeValue_Left != _currentDistance_Left) {

            valueDiff = resizeValue_Left - _currentDistance_Left;
            spinnerDiff = _spinnerDistance_Left;

         } else if (resizeValue_Right != _currentDistance_Right) {

            valueDiff = resizeValue_Right - _currentDistance_Right;
            spinnerDiff = _spinnerDistance_Right;
         }

         // adjust other values
         if (spinnerDiff == _spinnerDistance_Top) {

            resizeValue_Bottom += valueDiff;
            resizeValue_Left += valueDiff;
            resizeValue_Right += valueDiff;

            _spinnerDistance_Bottom.setSelection(resizeValue_Bottom);
            _spinnerDistance_Left.setSelection(resizeValue_Left);
            _spinnerDistance_Right.setSelection(resizeValue_Right);

         } else if (spinnerDiff == _spinnerDistance_Bottom) {

            resizeValue_Top += valueDiff;
            resizeValue_Left += valueDiff;
            resizeValue_Right += valueDiff;

            _spinnerDistance_Top.setSelection(resizeValue_Top);
            _spinnerDistance_Left.setSelection(resizeValue_Left);
            _spinnerDistance_Right.setSelection(resizeValue_Right);

         } else if (spinnerDiff == _spinnerDistance_Left) {

            resizeValue_Top += valueDiff;
            resizeValue_Bottom += valueDiff;
            resizeValue_Right += valueDiff;

            _spinnerDistance_Top.setSelection(resizeValue_Top);
            _spinnerDistance_Bottom.setSelection(resizeValue_Bottom);
            _spinnerDistance_Right.setSelection(resizeValue_Right);

         } else if (spinnerDiff == _spinnerDistance_Right) {

            resizeValue_Top += valueDiff;
            resizeValue_Bottom += valueDiff;
            resizeValue_Left += valueDiff;

            _spinnerDistance_Top.setSelection(resizeValue_Top);
            _spinnerDistance_Bottom.setSelection(resizeValue_Bottom);
            _spinnerDistance_Left.setSelection(resizeValue_Left);
         }
      }

// SET_FORMATTING_OFF

      _currentDistance_Top       = resizeValue_Top;
      _currentDistance_Bottom    = resizeValue_Bottom;
      _currentDistance_Left      = resizeValue_Left;
      _currentDistance_Right     = resizeValue_Right;

      final float resizedTop     = resizeValue_Top    / UI.UNIT_VALUE_DISTANCE_SMALL;
      final float resizedBottom  = resizeValue_Bottom / UI.UNIT_VALUE_DISTANCE_SMALL;
      final float resizedLeft    = resizeValue_Left   / UI.UNIT_VALUE_DISTANCE_SMALL;
      final float resizedRight   = resizeValue_Right  / UI.UNIT_VALUE_DISTANCE_SMALL;

      final double latitudeMin   = _tourLocation.latitudeMin;
      final double latitudeMax   = _tourLocation.latitudeMax;
      final double longitudeMin  = _tourLocation.longitudeMin;
      final double longitudeMax  = _tourLocation.longitudeMax;

      final GeoPosition topRight_TopPos      = MtMath.destinationPoint(latitudeMax, longitudeMax, resizedTop,      0);
      final GeoPosition topRight_RightPos    = MtMath.destinationPoint(latitudeMax, longitudeMax, resizedRight,    90);
      final GeoPosition bottomLeft_BottomPos = MtMath.destinationPoint(latitudeMin, longitudeMin, resizedBottom,   180);
      final GeoPosition bottomLeft_LeftPos   = MtMath.destinationPoint(latitudeMin, longitudeMin, resizedLeft,     270);

      final double latitudeMin_Resized       = bottomLeft_BottomPos.latitude;
      final double latitudeMax_Resized       = topRight_TopPos.latitude;
      final double longitudeMin_Resized      = bottomLeft_LeftPos.longitude;
      final double longitudeMax_Resized      = topRight_RightPos.longitude;

      final int latitudeMinE6_Resized        = (int) (latitudeMin_Resized * 1E6);
      final int latitudeMaxE6_Resized        = (int) (latitudeMax_Resized * 1E6);
      final int longitudeMinE6_Resized       = (int) (longitudeMin_Resized * 1E6);
      final int longitudeMaxE6_Resized       = (int) (longitudeMax_Resized * 1E6);

      int latitudeMinE6_Resized_Normalized   = latitudeMinE6_Resized + 90_000_000;
      int latitudeMaxE6_Resized_Normalized   = latitudeMaxE6_Resized + 90_000_000;
      int longitudeMinE6_Resized_Normalized  = longitudeMinE6_Resized + 180_000_000;
      int longitudeMaxE6_Resized_Normalized  = longitudeMaxE6_Resized + 180_000_000;

      final double bboxWidth           = MtMath.distanceVincenty(latitudeMin, longitudeMin, latitudeMin, longitudeMax) / UI.UNIT_VALUE_DISTANCE_SMALL;
      final double bboxHeight          = MtMath.distanceVincenty(latitudeMin, longitudeMin, latitudeMax, longitudeMin) / UI.UNIT_VALUE_DISTANCE_SMALL;

      final double bboxWidth_Resized   = MtMath.distanceVincenty(latitudeMin_Resized, longitudeMin_Resized, latitudeMin_Resized, longitudeMax_Resized) / UI.UNIT_VALUE_DISTANCE_SMALL;
      final double bboxHeight_Resized  = MtMath.distanceVincenty(latitudeMin_Resized, longitudeMin_Resized, latitudeMax_Resized, longitudeMin_Resized) / UI.UNIT_VALUE_DISTANCE_SMALL;

// SET_FORMATTING_ON

      // ensure that min < max
      if (latitudeMinE6_Resized_Normalized > latitudeMaxE6_Resized_Normalized) {

         final int swapValue = latitudeMinE6_Resized_Normalized;

         latitudeMinE6_Resized_Normalized = latitudeMaxE6_Resized_Normalized;
         latitudeMaxE6_Resized_Normalized = swapValue;
      }

      if (longitudeMinE6_Resized_Normalized > longitudeMaxE6_Resized_Normalized) {

         final int swapValue = longitudeMinE6_Resized_Normalized;

         longitudeMinE6_Resized_Normalized = longitudeMaxE6_Resized_Normalized;
         longitudeMaxE6_Resized_Normalized = swapValue;
      }

      /*
       * Update model
       */

      _tourLocation.latitudeMin_Resized = latitudeMin_Resized;
      _tourLocation.latitudeMax_Resized = latitudeMax_Resized;
      _tourLocation.longitudeMin_Resized = longitudeMin_Resized;
      _tourLocation.longitudeMax_Resized = longitudeMax_Resized;

      _tourLocation.latitudeMinE6_Resized_Normalized = latitudeMinE6_Resized_Normalized;
      _tourLocation.latitudeMaxE6_Resized_Normalized = latitudeMaxE6_Resized_Normalized;
      _tourLocation.longitudeMinE6_Resized_Normalized = longitudeMinE6_Resized_Normalized;
      _tourLocation.longitudeMaxE6_Resized_Normalized = longitudeMaxE6_Resized_Normalized;

      TourLocationManager.setResizedBoundingBox(_tourLocation.getLocationId(),

            latitudeMinE6_Resized_Normalized,
            latitudeMaxE6_Resized_Normalized,
            longitudeMinE6_Resized_Normalized,
            longitudeMaxE6_Resized_Normalized);

      /*
       * Update UI
       */

      _lblBBoxDefaultSize.setText(BOUNDING_BOX_SIZE.formatted(

            FormatManager.formatNumber_0(bboxWidth),
            FormatManager.formatNumber_0(bboxHeight),

            UI.UNIT_LABEL_DISTANCE_M_OR_YD));

      _lblBBoxResizedSize.setText(BOUNDING_BOX_SIZE.formatted(

            FormatManager.formatNumber_0(bboxWidth_Resized),
            FormatManager.formatNumber_0(bboxHeight_Resized),

            UI.UNIT_LABEL_DISTANCE_M_OR_YD));

      _tourLocationView.updateUI(_tourLocation);
   }

   private void updateModelFromUI_AndFireEvent() {

      updateModelFromUI();

      enableControls();

      fireModifyEvent();
   }

   /**
    * Update UI from {@link #_tourLocation}
    */
   private void updateUIfromModel() {

// SET_FORMATTING_OFF

      final double latitudeMin               = _tourLocation.latitudeMin;
      final double latitudeMax               = _tourLocation.latitudeMax;
      final double longitudeMin              = _tourLocation.longitudeMin;
      final double longitudeMax              = _tourLocation.longitudeMax;

      final double latitudeMin_Resized       = _tourLocation.latitudeMin_Resized;
      final double latitudeMax_Resized       = _tourLocation.latitudeMax_Resized;
      final double longitudeMin_Resized      = _tourLocation.longitudeMin_Resized;
      final double longitudeMax_Resized      = _tourLocation.longitudeMax_Resized;

      final int signumLatMin           = latitudeMin  <= latitudeMin_Resized  ? -1 :  1;
      final int signumLatMax           = latitudeMax  <= latitudeMax_Resized  ?  1 : -1;
      final int signumLonMin           = longitudeMin <= longitudeMin_Resized ? -1 :  1;
      final int signumLonMax           = longitudeMax <= longitudeMax_Resized ?  1 : -1;

      final double bboxWidth           = MtMath.distanceVincenty(latitudeMin, longitudeMin, latitudeMin, longitudeMax) / UI.UNIT_VALUE_DISTANCE_SMALL;
      final double bboxHeight          = MtMath.distanceVincenty(latitudeMin, longitudeMin, latitudeMax, longitudeMin) / UI.UNIT_VALUE_DISTANCE_SMALL;

      final double bboxWidth_Resized   = MtMath.distanceVincenty(latitudeMin_Resized, longitudeMin_Resized, latitudeMin_Resized, longitudeMax_Resized) / UI.UNIT_VALUE_DISTANCE_SMALL;
      final double bboxHeight_Resized  = MtMath.distanceVincenty(latitudeMin_Resized, longitudeMin_Resized, latitudeMax_Resized, longitudeMin_Resized) / UI.UNIT_VALUE_DISTANCE_SMALL;

      double bboxTopDiff      = MtMath.distanceVincenty(latitudeMax, longitudeMax, latitudeMax_Resized,  longitudeMax)           / UI.UNIT_VALUE_DISTANCE_SMALL;
      double bboxBottomDiff   = MtMath.distanceVincenty(latitudeMin, longitudeMin, latitudeMin_Resized,  longitudeMin)           / UI.UNIT_VALUE_DISTANCE_SMALL;
      double bboxLeftDiff     = MtMath.distanceVincenty(latitudeMin, longitudeMin, latitudeMin,          longitudeMin_Resized)   / UI.UNIT_VALUE_DISTANCE_SMALL;
      double bboxRightDiff    = MtMath.distanceVincenty(latitudeMin, longitudeMax, latitudeMin,          longitudeMax_Resized)   / UI.UNIT_VALUE_DISTANCE_SMALL;

      bboxTopDiff    *= signumLatMax;
      bboxBottomDiff *= signumLatMin;
      bboxLeftDiff   *= signumLonMin;
      bboxRightDiff  *= signumLonMax;

// SET_FORMATTING_ON

      /*
       * Update UI
       */
      _lblBBoxDefaultSize.setText(BOUNDING_BOX_SIZE.formatted(

            FormatManager.formatNumber_0(bboxWidth),
            FormatManager.formatNumber_0(bboxHeight),

            UI.UNIT_LABEL_DISTANCE_M_OR_YD));

      _lblBBoxResizedSize.setText(BOUNDING_BOX_SIZE.formatted(

            FormatManager.formatNumber_0(bboxWidth_Resized),
            FormatManager.formatNumber_0(bboxHeight_Resized),

            UI.UNIT_LABEL_DISTANCE_M_OR_YD));

      _spinnerDistance_Top.setSelection((int) Math.round(bboxTopDiff));
      _spinnerDistance_Bottom.setSelection((int) Math.round(bboxBottomDiff));
      _spinnerDistance_Left.setSelection((int) (Math.round(bboxLeftDiff)));
      _spinnerDistance_Right.setSelection((int) (Math.round(bboxRightDiff)));
   }

}
