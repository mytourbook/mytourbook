/*******************************************************************************
 * Copyright (C) 2024, 2025 Wolfgang Schramm and Contributors
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

import net.tourbook.OtherMessages;
import net.tourbook.common.UI;
import net.tourbook.common.action.IActionResetToDefault;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.IToolTipProvider;
import net.tourbook.common.util.Util;
import net.tourbook.preferences.PrefPageAppearanceDisplayFormat;
import net.tourbook.ui.Messages;
import net.tourbook.ui.action.Action_ToolTip_EditPreferences;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Slideout for the tour info
 */
public class SlideoutTourInfoOptions extends ToolbarSlideout implements IColorSelectorListener, IActionResetToDefault {

   static final String                    STATE_IS_SHOW_BODY_VALUES         = "STATE_IS_SHOW_BODY_VALUES";         //$NON-NLS-1$
   static final String                    STATE_IS_SHOW_DAYVSNIGHT_TIMES    = "STATE_IS_SHOW_DAYVSNIGHT_TIMES";    //$NON-NLS-1$
   static final String                    STATE_IS_SHOW_CUSTOM_VALUES       = "STATE_IS_SHOW_CUSTOM_VALUES";       //$NON-NLS-1$
   static final String                    STATE_IS_SHOW_HEART_RATE_ZONES    = "STATE_IS_SHOW_HEART_RATE_ZONES";    //$NON-NLS-1$
   static final String                    STATE_IS_SHOW_RUNNING_DYNAMICS    = "STATE_IS_SHOW_RUNNING_DYNAMICS";    //$NON-NLS-1$
   static final String                    STATE_IS_SHOW_SENSOR_VALUES       = "STATE_IS_SHOW_SENSOR_VALUES";       //$NON-NLS-1$
   static final String                    STATE_IS_SHOW_START_END_LOCATION  = "STATE_IS_SHOW_START_END_LOCATION";  //$NON-NLS-1$
   static final String                    STATE_IS_SHOW_VERTICAL_SPEED      = "STATE_IS_SHOW_VERTICAL_SPEED";      //$NON-NLS-1$
   static final String                    STATE_IS_SHOW_WEATHER_DESCRIPTION = "STATE_IS_SHOW_WEATHER_DESCRIPTION"; //$NON-NLS-1$
   static final String                    STATE_IS_SHOW_WEATHER_VALUES      = "STATE_IS_SHOW_WEATHER_VALUES";      //$NON-NLS-1$

   static final String                    STATE_UI_WIDTH_SIZE_INDEX         = "STATE_UI_WIDTH_SIZE_INDEX";         //$NON-NLS-1$
   static final String                    STATE_UI_WIDTH_SMALL              = "STATE_UI_WIDTH_SMALL";              //$NON-NLS-1$
   static final String                    STATE_UI_WIDTH_MEDIUM             = "STATE_UI_WIDTH_MEDIUM";             //$NON-NLS-1$
   static final String                    STATE_UI_WIDTH_LARGE              = "STATE_UI_WIDTH_LARGE";              //$NON-NLS-1$
   static final int                       STATE_UI_WIDTH_SMALL_DEFAULT      = 600;
   static final int                       STATE_UI_WIDTH_MEDIUM_DEFAULT     = 800;
   static final int                       STATE_UI_WIDTH_LARGE_DEFAULT      = 1000;

   static final int                       STATE_UI_WIDTH_MIN                = 100;
   static final int                       STATE_UI_WIDTH_MAX                = 3000;

   private static IDialogSettings         _state;

   private TourInfoUI                     _tourInfoUI;

   private SelectionListener              _defaultSelectionListener;
   private FocusListener                  _keepOpenListener;

   private Action_ToolTip_EditPreferences _actionPrefDialog;

   private boolean                        _isInTooltip;
   private boolean                        _isShowCustomValues;

   /*
    * UI controls
    */
   private Composite        _shellContainer;

   private Button           _chkShowBodyValues;
   private Button           _chkShowDayVsNightTimes;
   private Button           _chkShowHRZones;
   private Button           _chkShowRunDyn;
   private Button           _chkShowSensorValues;
   private Button           _chkShowStartEndLocation;
   private Button           _chkShowVerticalSpeed;
   private Button           _chkShowWeatherDescription;
   private Button           _chkShowWeatherValues;

   private Button           _rdoShowAllValues;
   private Button           _rdoShowCustomValues;

   private Combo            _comboUIWidth_Size;

   private Label            _lblTooltipUIWidth;

   private Spinner          _spinnerUIWidth_Pixel;
   private IToolTipProvider _tourToolTipProvider;

   public SlideoutTourInfoOptions(final Control ownerControl,
                                  final ToolBar toolBar,
                                  final TourInfoUI tourInfoUI,
                                  final IToolTipProvider tourToolTipProvider,
                                  final IDialogSettings state) {

      super(ownerControl, toolBar);

      _tourInfoUI = tourInfoUI;
      _tourToolTipProvider = tourToolTipProvider;

      _state = state;

      final boolean isUIEmbedded = _tourInfoUI.isUIEmbedded();
      _isInTooltip = isUIEmbedded == false;

   }

   @Override
   protected void beforeHideToolTip() {

      // set state that this tooltip is now hidden

      _tourInfoUI.setUICanBeClosed();
   }

   @Override
   public void colorDialogOpened(final boolean isDialogOpened) {

      setIsAnotherDialogOpened(isDialogOpened);
   }

   private void createActions() {

      _actionPrefDialog = new Action_ToolTip_EditPreferences(_tourToolTipProvider,
            Messages.Tour_Tooltip_Action_EditFormatPreferences,
            PrefPageAppearanceDisplayFormat.ID,
            Integer.valueOf(0) // selected tab folder
      );
   }

   @Override
   protected Composite createToolTipContentArea(final Composite parent) {

      initUI();

      createActions();

      final Composite ui = createUI(parent);

      fillUI();

      restoreState();

      enableControls();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      _shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.swtDefaults().applyTo(_shellContainer);
      {
         final Composite container = new Composite(_shellContainer, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
         {
            createUI_10_Title(container);
            createUI_20_Options(container);
         }
      }

      return _shellContainer;
   }

   private void createUI_10_Title(final Composite parent) {

      final int numSpanColumns = _isInTooltip

            // show a 2nd column for the action
            ? 1

            : 2;

      /*
       * Label: Slideout title
       */
      final Label label = new Label(parent, SWT.NONE);
      label.setText(Messages.Tour_TooltipOptions_Title_TourInfoOptions);
      MTFont.setBannerFont(label);
      GridDataFactory.fillDefaults().span(numSpanColumns, 1).applyTo(label);

      if (_isInTooltip) {

         // Edit Value &Formats...
         final ToolBar toolbar = UI.createToolbarAction(parent, _actionPrefDialog);

         GridDataFactory.fillDefaults()
               .grab(true, false)
               .align(SWT.END, SWT.FILL)
               .applyTo(toolbar);

      }
   }

   private void createUI_20_Options(final Composite parent) {

      final GridDataFactory gdSpan2 = GridDataFactory.fillDefaults().span(2, 1);

      {
         /*
          * Show all values
          */
         _rdoShowAllValues = new Button(parent, SWT.RADIO);
         _rdoShowAllValues.setText(Messages.Tour_TooltipOptions_Radio_ShowAllValues);
         _rdoShowAllValues.addSelectionListener(_defaultSelectionListener);
         gdSpan2.applyTo(_rdoShowAllValues);
      }
      {
         /*
          * Show custom values
          */
         _rdoShowCustomValues = new Button(parent, SWT.RADIO);
         _rdoShowCustomValues.setText(Messages.Tour_TooltipOptions_Radio_CustomizeValues);
         _rdoShowCustomValues.addSelectionListener(_defaultSelectionListener);
         gdSpan2.applyTo(_rdoShowCustomValues);
      }

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .span(2, 1)
            .indent(16, 0)
            .applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      {
         {
            /*
             * Show body values
             */
            _chkShowBodyValues = new Button(container, SWT.CHECK);
            _chkShowBodyValues.setText(Messages.Tour_TooltipOptions_Checkbox_ShowBodyValues);
            _chkShowBodyValues.addSelectionListener(_defaultSelectionListener);
            gdSpan2.applyTo(_chkShowBodyValues);
         }
         {
            /*
             * Show day vs night times values
             */
            _chkShowDayVsNightTimes = new Button(container, SWT.CHECK);
            _chkShowDayVsNightTimes.setText(Messages.Tour_TooltipOptions_Checkbox_ShowDayVsNightTimes);
            _chkShowDayVsNightTimes.addSelectionListener(_defaultSelectionListener);
            gdSpan2.applyTo(_chkShowDayVsNightTimes);
         }
         {
            /*
             * Show heartrate zones
             */
            _chkShowHRZones = new Button(container, SWT.CHECK);
            _chkShowHRZones.setText(Messages.Tour_TooltipOptions_Checkbox_ShowHeartrateZones);
            _chkShowHRZones.addSelectionListener(_defaultSelectionListener);
            gdSpan2.applyTo(_chkShowHRZones);
         }
         {
            /*
             * Show running dynamics
             */
            _chkShowRunDyn = new Button(container, SWT.CHECK);
            _chkShowRunDyn.setText(Messages.Tour_TooltipOptions_Checkbox_ShowRunningDynamics);
            _chkShowRunDyn.addSelectionListener(_defaultSelectionListener);
            gdSpan2.applyTo(_chkShowRunDyn);
         }
         {
            /*
             * Show vertical speed
             */
            _chkShowVerticalSpeed = new Button(container, SWT.CHECK);
            _chkShowVerticalSpeed.setText(Messages.Tour_TooltipOptions_Checkbox_ShowVerticalSpeed);
            _chkShowVerticalSpeed.addSelectionListener(_defaultSelectionListener);
            gdSpan2.applyTo(_chkShowVerticalSpeed);
         }
         {
            /*
             * Show sensor values
             */
            _chkShowSensorValues = new Button(container, SWT.CHECK);
            _chkShowSensorValues.setText(Messages.Tour_TooltipOptions_Checkbox_ShowSensorValues);
            _chkShowSensorValues.addSelectionListener(_defaultSelectionListener);
            gdSpan2.applyTo(_chkShowSensorValues);
         }
         {
            /*
             * Show weather values
             */
            _chkShowWeatherValues = new Button(container, SWT.CHECK);
            _chkShowWeatherValues.setText(Messages.Tour_TooltipOptions_Checkbox_ShowWeatherValues);
            _chkShowWeatherValues.addSelectionListener(_defaultSelectionListener);
            gdSpan2.applyTo(_chkShowWeatherValues);
         }
         {
            /*
             * Show weather description
             */
            _chkShowWeatherDescription = new Button(container, SWT.CHECK);
            _chkShowWeatherDescription.setText(Messages.Tour_TooltipOptions_Checkbox_ShowWeatherDescription);
            _chkShowWeatherDescription.addSelectionListener(_defaultSelectionListener);
            gdSpan2.applyTo(_chkShowWeatherDescription);
         }
         {
            /*
             * Show start/end location
             */
            _chkShowStartEndLocation = new Button(container, SWT.CHECK);
            _chkShowStartEndLocation.setText(Messages.Tour_TooltipOptions_Checkbox_ShowStartEndLocation);
            _chkShowStartEndLocation.addSelectionListener(_defaultSelectionListener);
            gdSpan2.applyTo(_chkShowStartEndLocation);
         }
      }
      {
         /*
          * Tooltip UI width
          */
         {
            _lblTooltipUIWidth = new Label(parent, SWT.NONE);
            _lblTooltipUIWidth.setText(Messages.Tour_TooltipOptions_Label_TooltipWidth);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_lblTooltipUIWidth);
         }
         {
            final Composite widthContainer = new Composite(parent, SWT.NONE);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(widthContainer);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(widthContainer);
            {
               {
                  /*
                   * Text width in pixel
                   */
                  _spinnerUIWidth_Pixel = new Spinner(widthContainer, SWT.BORDER);
                  _spinnerUIWidth_Pixel.setMinimum(STATE_UI_WIDTH_MIN);
                  _spinnerUIWidth_Pixel.setMaximum(STATE_UI_WIDTH_MAX);
                  _spinnerUIWidth_Pixel.setIncrement(10);
                  _spinnerUIWidth_Pixel.setPageIncrement(50);
                  _spinnerUIWidth_Pixel.setToolTipText(Messages.Tour_Tooltip_Spinner_TextWidth_Tooltip);

                  _spinnerUIWidth_Pixel.addSelectionListener(SelectionListener.widgetSelectedAdapter(
                        selectionEvent -> onSelect_UIWidth_2_Value()));

                  _spinnerUIWidth_Pixel.addMouseWheelListener(mouseEvent -> {

                     UI.adjustSpinnerValueOnMouseScroll(mouseEvent, 10);
                     onSelect_UIWidth_2_Value();
                  });

                  GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_spinnerUIWidth_Pixel);
               }
               {
                  // Combo: Mouse wheel incrementer
                  _comboUIWidth_Size = new Combo(widthContainer, SWT.READ_ONLY | SWT.BORDER);
                  _comboUIWidth_Size.setVisibleItemCount(10);
                  _comboUIWidth_Size.setToolTipText(Messages.Tour_Tooltip_Combo_UIWidthSize_Tooltip);
                  _comboUIWidth_Size.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onSelect_UIWidth_1_Size()));
                  _comboUIWidth_Size.addFocusListener(_keepOpenListener);

                  GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_comboUIWidth_Size);
               }
            }
         }
      }
   }

   private void enableControls() {

// SET_FORMATTING_OFF

      _chkShowBodyValues         .setEnabled(_isShowCustomValues);
      _chkShowDayVsNightTimes    .setEnabled(_isShowCustomValues);
      _chkShowHRZones            .setEnabled(_isShowCustomValues);
      _chkShowRunDyn             .setEnabled(_isShowCustomValues);
      _chkShowSensorValues       .setEnabled(_isShowCustomValues);
      _chkShowStartEndLocation   .setEnabled(_isShowCustomValues);
      _chkShowVerticalSpeed      .setEnabled(_isShowCustomValues);
      _chkShowWeatherDescription .setEnabled(_isShowCustomValues);
      _chkShowWeatherValues      .setEnabled(_isShowCustomValues);

      _lblTooltipUIWidth         .setEnabled(_isInTooltip);
      _comboUIWidth_Size         .setEnabled(_isInTooltip);
      _spinnerUIWidth_Pixel      .setEnabled(_isInTooltip);

// SET_FORMATTING_ON
   }

   private void fillUI() {

      if (_comboUIWidth_Size != null && _comboUIWidth_Size.isDisposed() == false) {

         _comboUIWidth_Size.add(OtherMessages.APP_SIZE_SMALL_SHORTCUT);
         _comboUIWidth_Size.add(OtherMessages.APP_SIZE_MEDIUM_SHORTCUT);
         _comboUIWidth_Size.add(OtherMessages.APP_SIZE_LARGE_SHORTCUT);
      }
   }

   private int getSelectedUIWidthSizeIndex() {

      final int selectionIndex = _comboUIWidth_Size.getSelectionIndex();

      return selectionIndex < 0
            ? 0
            : selectionIndex;
   }

   private void initUI() {

      _defaultSelectionListener = SelectionListener.widgetSelectedAdapter(selectionEvent -> onModified());

      _keepOpenListener = new FocusListener() {

         @Override
         public void focusGained(final FocusEvent e) {

            setIsAnotherDialogOpened(true);
         }

         @Override
         public void focusLost(final FocusEvent e) {

            setIsAnotherDialogOpened(false);
         }
      };
   }

   private void onModified() {

      _isShowCustomValues = _rdoShowCustomValues.getSelection();

      saveState();

      enableControls();

      _tourInfoUI.restoreState_UIOptions();
      _tourInfoUI.updateUI_FromUIOptions();
   }

   private void onSelect_UIWidth_1_Size() {

      final int selectedUIWidthSizeIndex = getSelectedUIWidthSizeIndex();

      // save selected size
      _state.put(STATE_UI_WIDTH_SIZE_INDEX, selectedUIWidthSizeIndex);

      int uiWidth_Pixel;

      // set size from state
      switch (getSelectedUIWidthSizeIndex()) {
      case 1  -> uiWidth_Pixel = Util.getStateInt(_state, STATE_UI_WIDTH_MEDIUM, STATE_UI_WIDTH_MEDIUM_DEFAULT);
      case 2  -> uiWidth_Pixel = Util.getStateInt(_state, STATE_UI_WIDTH_LARGE, STATE_UI_WIDTH_LARGE_DEFAULT);
      default -> uiWidth_Pixel = Util.getStateInt(_state, STATE_UI_WIDTH_SMALL, STATE_UI_WIDTH_SMALL_DEFAULT);
      }

      // update model
      _tourInfoUI.setUIWidth_Pixel(uiWidth_Pixel);

      // update UI
      _spinnerUIWidth_Pixel.setSelection(uiWidth_Pixel);

      _tourInfoUI.updateUI_FromUIOptions();
   }

   private void onSelect_UIWidth_2_Value() {

      // get width
      final int uiWidth_Pixel = _spinnerUIWidth_Pixel.getSelection();

      // save state for the selected size
      switch (getSelectedUIWidthSizeIndex()) {
      case 1  -> _state.put(STATE_UI_WIDTH_MEDIUM, uiWidth_Pixel);
      case 2  -> _state.put(STATE_UI_WIDTH_LARGE, uiWidth_Pixel);
      default -> _state.put(STATE_UI_WIDTH_SMALL, uiWidth_Pixel);
      }

      // update model
      _tourInfoUI.setUIWidth_Pixel(uiWidth_Pixel);

      // update UI
      _tourInfoUI.updateUI_FromUIOptions();
   }

   @Override
   public void resetToDefaults() {}

   private void restoreState() {

// SET_FORMATTING_OFF

      _isShowCustomValues                    = Util.getStateBoolean(_state, STATE_IS_SHOW_CUSTOM_VALUES,          true);

      final boolean isShowBodyValues         = Util.getStateBoolean(_state, STATE_IS_SHOW_BODY_VALUES,            true);
      final boolean isShowDayVsNightTimes    = Util.getStateBoolean(_state, STATE_IS_SHOW_DAYVSNIGHT_TIMES,       true);
      final boolean isShowHRZones            = Util.getStateBoolean(_state, STATE_IS_SHOW_HEART_RATE_ZONES,       true);
      final boolean isShowRunDyn             = Util.getStateBoolean(_state, STATE_IS_SHOW_RUNNING_DYNAMICS,       true);
      final boolean isShowSensorValues       = Util.getStateBoolean(_state, STATE_IS_SHOW_SENSOR_VALUES,          true);
      final boolean isShowStartEndLocation   = Util.getStateBoolean(_state, STATE_IS_SHOW_START_END_LOCATION,     true);
      final boolean isShowVerticalSpeed      = Util.getStateBoolean(_state, STATE_IS_SHOW_VERTICAL_SPEED,         true);
      final boolean isShowWeatherDescription = Util.getStateBoolean(_state, STATE_IS_SHOW_WEATHER_DESCRIPTION,    true);
      final boolean isShowWeatherValues      = Util.getStateBoolean(_state, STATE_IS_SHOW_WEATHER_VALUES,         true);

      final int uiWidth_SizeIndex            = Util.getStateInt(_state, SlideoutTourInfoOptions.STATE_UI_WIDTH_SIZE_INDEX, 0);

      _chkShowBodyValues         .setSelection(isShowBodyValues);
      _chkShowDayVsNightTimes    .setSelection(isShowDayVsNightTimes);
      _chkShowHRZones            .setSelection(isShowHRZones);
      _chkShowRunDyn             .setSelection(isShowRunDyn);
      _chkShowSensorValues       .setSelection(isShowSensorValues);
      _chkShowStartEndLocation   .setSelection(isShowStartEndLocation);
      _chkShowVerticalSpeed      .setSelection(isShowVerticalSpeed);
      _chkShowWeatherDescription .setSelection(isShowWeatherDescription);
      _chkShowWeatherValues      .setSelection(isShowWeatherValues);

      _rdoShowAllValues          .setSelection(_isShowCustomValues == false);
      _rdoShowCustomValues       .setSelection(_isShowCustomValues);

      _spinnerUIWidth_Pixel      .setSelection(_tourInfoUI.getUIWidth_Pixel());

      _comboUIWidth_Size         .select(uiWidth_SizeIndex);

// SET_FORMATTING_ON

   }

   private void saveState() {

// SET_FORMATTING_OFF

      _state.put(STATE_IS_SHOW_CUSTOM_VALUES,         _rdoShowCustomValues       .getSelection());

      _state.put(STATE_IS_SHOW_BODY_VALUES,           _chkShowBodyValues         .getSelection());
      _state.put(STATE_IS_SHOW_DAYVSNIGHT_TIMES,      _chkShowDayVsNightTimes    .getSelection());
      _state.put(STATE_IS_SHOW_HEART_RATE_ZONES,      _chkShowHRZones            .getSelection());
      _state.put(STATE_IS_SHOW_RUNNING_DYNAMICS,      _chkShowRunDyn             .getSelection());
      _state.put(STATE_IS_SHOW_SENSOR_VALUES,         _chkShowSensorValues       .getSelection());
      _state.put(STATE_IS_SHOW_START_END_LOCATION,    _chkShowStartEndLocation   .getSelection());
      _state.put(STATE_IS_SHOW_VERTICAL_SPEED,        _chkShowVerticalSpeed      .getSelection());
      _state.put(STATE_IS_SHOW_WEATHER_DESCRIPTION,   _chkShowWeatherDescription .getSelection());
      _state.put(STATE_IS_SHOW_WEATHER_VALUES,        _chkShowWeatherValues      .getSelection());

// SET_FORMATTING_ON

   }
}
