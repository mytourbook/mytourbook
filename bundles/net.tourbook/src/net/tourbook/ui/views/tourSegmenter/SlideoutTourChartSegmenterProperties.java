/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourSegmenter;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.ColorCache;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionResetToDefaults;
import net.tourbook.common.action.IActionResetToDefault;
import net.tourbook.common.color.ColorSelectorExtended;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.font.FontFieldEditorExtended;
import net.tourbook.common.font.IFontDialogListener;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.AnimatedToolTipShell;
import net.tourbook.common.util.Util;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Tour chart marker properties slideout.
 */
public class SlideoutTourChartSegmenterProperties extends AnimatedToolTipShell
      implements IFontDialogListener, IColorSelectorListener, IActionResetToDefault {

// SET_FORMATTING_OFF

   private static final String   APP_THEME_BACKGROUND_COLOR_DARK_TOOLTIP   = net.tourbook.common.Messages.App_Theme_BackgroundColor_Dark_Tooltip;
   private static final String   APP_THEME_BACKGROUND_COLOR_LIGHT_TOOLTIP  = net.tourbook.common.Messages.App_Theme_BackgroundColor_Light_Tooltip;

// SET_FORMATTING_ON

   private static final IPreferenceStore _prefStore                               = TourbookPlugin.getPrefStore();
   private static final IDialogSettings  _segmenterState                          = TourSegmenterView.getState();

   // initialize with default values which are (should) never be used
   private Rectangle               _toolTipItemBounds = new Rectangle(0, 0, 50, 50);

   private final WaitTimer         _waitTimer         = new WaitTimer();

   private boolean                 _isWaitTimerStarted;
   private boolean                 _canOpenToolTip;

   private IPropertyChangeListener _segmenterChangePropertyListener;
   private IPropertyChangeListener _fontChangePropertyListener;
   private SelectionListener       _defaultSelectionListener;
   private MouseWheelListener      _defaultMouseWheelListener;

   {
      _defaultSelectionListener = widgetSelectedAdapter(selectionEvent -> onChangeUI());
      _defaultMouseWheelListener = event -> {
         UI.adjustSpinnerValueOnMouseScroll(event);
         onChangeUI();
      };

      _segmenterChangePropertyListener = event -> onChangeUI_Segmenter();
      _fontChangePropertyListener = event -> onChangeFontInEditor();
   }

   private PixelConverter        _pc;
   private int                   _firstColumnIndent;

   private TourSegmenterView     _tourSegmenterView;

   private ActionResetToDefaults _actionRestoreDefaults;

   /*
    * UI controls
    */
   private Composite               _shellContainer;
   private Composite               _fontContainer;

   private Button                  _chkHideSmallValues;
   private Button                  _chkShowDecimalPlaces;
   private Button                  _chkShowSegmentLine;
   private Button                  _chkShowSegmentMarker;
   private Button                  _chkShowSegmentTooltip;
   private Button                  _chkShowSegmentValue;

   private FontFieldEditorExtended _valueFontEditor;

   private ColorSelectorExtended   _colorSegmenterElevationUp_Light;
   private ColorSelectorExtended   _colorSegmenterElevationUp_Dark;
   private ColorSelectorExtended   _colorSegmenterElevationDown_Light;
   private ColorSelectorExtended   _colorSegmenterElevationDown_Dark;
   private ColorSelectorExtended   _colorSegmenterTotals_Light;
   private ColorSelectorExtended   _colorSegmenterTotals_Dark;

   private Label                   _lblGraphOpacity;
   private Label                   _lblHideSmallValuesUnit;
   private Label                   _lblLineOpacity;
   private Label                   _lblVisibleStackedValues;

   private Spinner                 _spinGraphOpacity;
   private Spinner                 _spinHideSmallValues;
   private Spinner                 _spinLineOpacity;
   private Spinner                 _spinVisibleValuesStacked;

   private final class WaitTimer implements Runnable {
      @Override
      public void run() {
         open_Runnable();
      }
   }

   public SlideoutTourChartSegmenterProperties(final Control ownerControl,
                                               final ToolBar toolBar,
                                               final TourSegmenterView tourSegmenterView) {

      super(ownerControl);

      _tourSegmenterView = tourSegmenterView;

      addListener(ownerControl, toolBar);

      setToolTipCreateStyle(AnimatedToolTipShell.TOOLTIP_STYLE_KEEP_CONTENT);
      setBehaviourOnMouseOver(AnimatedToolTipShell.MOUSE_OVER_BEHAVIOUR_IGNORE_OWNER);
      setIsKeepShellOpenWhenMoved(false);

      setFadeInSteps(1);
      setFadeOutSteps(10);
      setFadeOutDelaySteps(1);
   }

   private void addListener(final Control ownerControl, final ToolBar toolBar) {

      toolBar.addMouseTrackListener(new MouseTrackAdapter() {
         @Override
         public void mouseExit(final MouseEvent e) {

            // prevent to open the tooltip
            _canOpenToolTip = false;
         }
      });
   }

   @Override
   protected boolean canShowToolTip() {
      return true;
   }

   @Override
   public void colorDialogOpened(final boolean isDialogOpened) {
      setIsAnotherDialogOpened(isDialogOpened);
   }

   private void createActions() {

      _actionRestoreDefaults = new ActionResetToDefaults(this);
   }

   @Override
   protected Composite createToolTipContentArea(final Composite parent) {

      createActions();

      final Composite ui = createUI(parent);

      restoreState();
      enableControls();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      _pc = new PixelConverter(parent);
      _firstColumnIndent = _pc.convertWidthInCharsToPixels(3);

      _shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.swtDefaults().applyTo(_shellContainer);
      {
         final Composite container = new Composite(_shellContainer, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         GridLayoutFactory.fillDefaults()
               .numColumns(4)
               .spacing(20, 5)
               .applyTo(container);
//         container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
         {
            createUI_10_Title(container);
            createUI_12_Actions(container);
            createUI_20_Options(container);
            createUI_30_SegmentValues(container);

            createUI_50_SegmenterTitle(container);
            createUI_60_SegmenterColors(container);
         }
      }

      return _shellContainer;
   }

   private void createUI_10_Title(final Composite parent) {
      {
         /*
          * Label: Slideout title
          */
         final Label label = new Label(parent, SWT.NONE);
         label.setText(Messages.Slideout_SegmenterChartOptions_Label_Title);
         MTFont.setBannerFont(label);
         GridDataFactory.fillDefaults()
               .span(3, 1)
               .applyTo(label);
      }
   }

   private void createUI_12_Actions(final Composite parent) {

      final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .align(SWT.END, SWT.BEGINNING)
            .applyTo(toolbar);

      final ToolBarManager tbm = new ToolBarManager(toolbar);

      tbm.add(_actionRestoreDefaults);

      tbm.update(true);
   }

   private void createUI_20_Options(final Composite parent) {

      {
         /*
          * Checkbox: Show segment line
          */
         _chkShowSegmentLine = new Button(parent, SWT.CHECK);
         _chkShowSegmentLine.setText(Messages.Slideout_SegmenterChartOptions_Checkbox_IsShowSegmentLine);
         _chkShowSegmentLine.addSelectionListener(_defaultSelectionListener);
         GridDataFactory.fillDefaults()
               .span(2, 1)
               .applyTo(_chkShowSegmentLine);
      }
      {
         /*
          * Line opacity
          */

         // Label
         _lblLineOpacity = new Label(parent, SWT.NONE);
         _lblLineOpacity.setText(Messages.Slideout_SegmenterChartOptions_Label_LineOpacity);
         _lblLineOpacity.setToolTipText(Messages.Slideout_SegmenterChartOptions_Label_Opacity_Tooltip);
         GridDataFactory.fillDefaults()
               .align(SWT.FILL, SWT.CENTER)
               .applyTo(_lblLineOpacity);

         // Spinner:
         _spinLineOpacity = new Spinner(parent, SWT.BORDER);
         _spinLineOpacity.setMinimum(0);
         _spinLineOpacity.setMaximum(100);
         _spinLineOpacity.setPageIncrement(10);
         _spinLineOpacity.addSelectionListener(_defaultSelectionListener);
         _spinLineOpacity.addMouseWheelListener(_defaultMouseWheelListener);
      }
      {
         /*
          * Checkbox: Show segment marker
          */
         _chkShowSegmentMarker = new Button(parent, SWT.CHECK);
         _chkShowSegmentMarker.setText(Messages.Slideout_SegmenterChartOptions_Checkbox_IsShowSegmentMarker);
         _chkShowSegmentMarker.addSelectionListener(_defaultSelectionListener);
         GridDataFactory.fillDefaults()
               .span(2, 1)
               .applyTo(_chkShowSegmentMarker);
      }
      {
         /*
          * Graph opacity
          */

         // Label
         _lblGraphOpacity = new Label(parent, SWT.NONE);
         _lblGraphOpacity.setText(Messages.Slideout_SegmenterChartOptions_Label_GraphOpacity);
         _lblGraphOpacity.setToolTipText(Messages.Slideout_SegmenterChartOptions_Label_Opacity_Tooltip);
         GridDataFactory.fillDefaults()
               .align(SWT.FILL, SWT.CENTER)
               .applyTo(_lblGraphOpacity);

         // Spinner:
         _spinGraphOpacity = new Spinner(parent, SWT.BORDER);
         _spinGraphOpacity.setMinimum(0);
         _spinGraphOpacity.setMaximum(100);
         _spinGraphOpacity.setPageIncrement(10);
         _spinGraphOpacity.addSelectionListener(_defaultSelectionListener);
         _spinGraphOpacity.addMouseWheelListener(_defaultMouseWheelListener);
      }
      {
         /*
          * Checkbox: Show segment tooltip
          */
         _chkShowSegmentTooltip = new Button(parent, SWT.CHECK);
         _chkShowSegmentTooltip.setText(Messages.Slideout_SegmenterChartOptions_Checkbox_IsShowSegmentTooltip);
         _chkShowSegmentTooltip.addSelectionListener(_defaultSelectionListener);
         GridDataFactory.fillDefaults()
               .span(2, 1)
               .applyTo(_chkShowSegmentTooltip);
      }
   }

   private void createUI_30_SegmentValues(final Composite parent) {

      {
         /*
          * Checkbox: Show segment value
          */
         _chkShowSegmentValue = new Button(parent, SWT.CHECK);
         _chkShowSegmentValue.setText(Messages.Slideout_SegmenterChartOptions_Checkbox_IsShowSegmentValue);
         _chkShowSegmentValue.addSelectionListener(_defaultSelectionListener);
         GridDataFactory.fillDefaults()
               .span(4, 1)
               .applyTo(_chkShowSegmentValue);
      }
      {
         /*
          * Checkbox: Show decimal places
          */
         _chkShowDecimalPlaces = new Button(parent, SWT.CHECK);
         _chkShowDecimalPlaces.setText(Messages.Slideout_SegmenterChartOptions_Checkbox_IsShowDecimalPlaces);
         _chkShowDecimalPlaces.addSelectionListener(_defaultSelectionListener);
         GridDataFactory.fillDefaults()
               .span(2, 1)
               .indent(_firstColumnIndent, 0)
               .applyTo(_chkShowDecimalPlaces);
      }
      {
         /*
          * Number of stacked values
          */
         // Label
         _lblVisibleStackedValues = new Label(parent, SWT.NONE);
         _lblVisibleStackedValues.setText(Messages.Slideout_SegmenterChartOptions_Label_StackedValues);
         _lblVisibleStackedValues.setToolTipText(Messages.Slideout_SegmenterChartOptions_Label_StackedValues_Tooltip);
         GridDataFactory.fillDefaults()
               .align(SWT.FILL, SWT.CENTER)
               .applyTo(_lblVisibleStackedValues);

         // Spinner:
         _spinVisibleValuesStacked = new Spinner(parent, SWT.BORDER);
         _spinVisibleValuesStacked.setMinimum(0);
         _spinVisibleValuesStacked.setMaximum(20);
         _spinVisibleValuesStacked.setPageIncrement(5);
         _spinVisibleValuesStacked.addSelectionListener(_defaultSelectionListener);
         _spinVisibleValuesStacked.addMouseWheelListener(_defaultMouseWheelListener);
      }
      {
         /*
          * Hide small values
          */

         final Composite containerSmall = new Composite(parent, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false)
               .span(4, 1)
               .applyTo(containerSmall);
         GridLayoutFactory.fillDefaults().numColumns(3).applyTo(containerSmall);
//			containerSmall.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
         {
            // Checkbox
            _chkHideSmallValues = new Button(containerSmall, SWT.CHECK);
            _chkHideSmallValues.setText(Messages.Slideout_SegmenterChartOptions_Checkbox_HideSmallValues);
            _chkHideSmallValues.setToolTipText(Messages.Slideout_SegmenterChartOptions_Checkbox_HideSmallValues_Tooltip);
            _chkHideSmallValues.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .indent(_firstColumnIndent, 0)
                  .applyTo(_chkHideSmallValues);

            // Spinner
            _spinHideSmallValues = new Spinner(containerSmall, SWT.BORDER);
            _spinHideSmallValues.setMinimum(0);
            _spinHideSmallValues.setMaximum(90);
            _spinHideSmallValues.setPageIncrement(10);
            _spinHideSmallValues.addSelectionListener(_defaultSelectionListener);
            _spinHideSmallValues.addMouseWheelListener(_defaultMouseWheelListener);

            // Label %
            _lblHideSmallValuesUnit = new Label(containerSmall, SWT.NONE);
            _lblHideSmallValuesUnit.setText(UI.SYMBOL_PERCENTAGE);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .applyTo(_lblHideSmallValuesUnit);
         }
      }
      {
         /*
          * Font editor
          */
         _fontContainer = new Composite(parent, SWT.NONE);
         GridDataFactory.fillDefaults()
               .grab(true, false)
               .span(4, 1)
               .applyTo(_fontContainer);
         GridLayoutFactory.fillDefaults().numColumns(1).applyTo(_fontContainer);
//         _fontContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
         {
            _valueFontEditor = new FontFieldEditorExtended(ITourbookPreferences.TOUR_SEGMENTER_CHART_VALUE_FONT,
                  Messages.Slideout_SegmenterChartOptions_Label_ValueFont,
                  Messages.Slideout_SegmenterChartOptions_Label_ValueFont_Example,
                  _fontContainer);

            _valueFontEditor.setPropertyChangeListener(_fontChangePropertyListener);
            _valueFontEditor.addOpenListener(this);
            _valueFontEditor.setFirstColumnIndent(_firstColumnIndent, 0);
         }
      }
   }

   private void createUI_50_SegmenterTitle(final Composite parent) {

      {
         /*
          * Label: Segmenter title
          */
         final Label label = new Label(parent, SWT.NONE);
         label.setText(Messages.Slideout_SegmenterOptions_Label_Title);
         MTFont.setBannerFont(label);
         GridDataFactory.fillDefaults()
               .span(4, 1)
               .applyTo(label);
      }
   }

   private void createUI_60_SegmenterColors(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .span(4, 1)
            .applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      {
         final Composite containerLeft = new Composite(container, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(containerLeft);
         GridLayoutFactory.fillDefaults().numColumns(3).applyTo(containerLeft);
         {
            {
               /*
                * Segmenter elevation up
                */

               // Label
               final Label label = new Label(containerLeft, SWT.NONE);
               label.setText(Messages.Slideout_SegmenterOptions_Label_AltitudeUp);
               GridDataFactory.fillDefaults()
                     .align(SWT.BEGINNING, SWT.CENTER)
                     .applyTo(label);

               // Color light
               _colorSegmenterElevationUp_Light = new ColorSelectorExtended(containerLeft);
               _colorSegmenterElevationUp_Light.getButton().setToolTipText(APP_THEME_BACKGROUND_COLOR_LIGHT_TOOLTIP);
               _colorSegmenterElevationUp_Light.addListener(_segmenterChangePropertyListener);
               _colorSegmenterElevationUp_Light.addOpenListener(this);

               // Color dark
               _colorSegmenterElevationUp_Dark = new ColorSelectorExtended(containerLeft);
               _colorSegmenterElevationUp_Dark.getButton().setToolTipText(APP_THEME_BACKGROUND_COLOR_DARK_TOOLTIP);
               _colorSegmenterElevationUp_Dark.addListener(_segmenterChangePropertyListener);
               _colorSegmenterElevationUp_Dark.addOpenListener(this);
            }
            {
               /*
                * Segmenter elevation down
                */

               // Label
               final Label label = new Label(containerLeft, SWT.NONE);
               label.setText(Messages.Slideout_SegmenterOptions_Label_AltitudeDown);
               GridDataFactory.fillDefaults()
                     .align(SWT.BEGINNING, SWT.CENTER)
                     .applyTo(label);

               // Color light
               _colorSegmenterElevationDown_Light = new ColorSelectorExtended(containerLeft);
               _colorSegmenterElevationDown_Light.getButton().setToolTipText(APP_THEME_BACKGROUND_COLOR_LIGHT_TOOLTIP);
               _colorSegmenterElevationDown_Light.addListener(_segmenterChangePropertyListener);
               _colorSegmenterElevationDown_Light.addOpenListener(this);

               // Color dark
               _colorSegmenterElevationDown_Dark = new ColorSelectorExtended(containerLeft);
               _colorSegmenterElevationDown_Dark.getButton().setToolTipText(APP_THEME_BACKGROUND_COLOR_DARK_TOOLTIP);
               _colorSegmenterElevationDown_Dark.addListener(_segmenterChangePropertyListener);
               _colorSegmenterElevationDown_Dark.addOpenListener(this);
            }
            {
               /*
                * Segmenter totals
                */

               // Label
               final Label label = new Label(containerLeft, SWT.NONE);
               label.setText(Messages.Slideout_SegmenterOptions_Label_Totals);
               GridDataFactory.fillDefaults()
                     .align(SWT.BEGINNING, SWT.CENTER)
                     .applyTo(label);

               // Color: Segmenter totals light
               _colorSegmenterTotals_Light = new ColorSelectorExtended(containerLeft);
               _colorSegmenterTotals_Light.getButton().setToolTipText(APP_THEME_BACKGROUND_COLOR_LIGHT_TOOLTIP);
               _colorSegmenterTotals_Light.addListener(_segmenterChangePropertyListener);
               _colorSegmenterTotals_Light.addOpenListener(this);

               // Color: Segmenter totals dark
               _colorSegmenterTotals_Dark = new ColorSelectorExtended(containerLeft);
               _colorSegmenterTotals_Dark.getButton().setToolTipText(APP_THEME_BACKGROUND_COLOR_DARK_TOOLTIP);
               _colorSegmenterTotals_Dark.addListener(_segmenterChangePropertyListener);
               _colorSegmenterTotals_Dark.addOpenListener(this);
            }
         }
      }
   }

   private void enableControls() {

      final boolean isShowValues = _chkShowSegmentValue.getSelection();
      final boolean isHideSmallValues = _chkHideSmallValues.getSelection();
      final boolean isShowSegmentLine = _chkShowSegmentLine.getSelection();

      _lblLineOpacity.setEnabled(isShowSegmentLine);
      _spinLineOpacity.setEnabled(isShowSegmentLine);

      _chkShowDecimalPlaces.setEnabled(isShowValues);

      _chkHideSmallValues.setEnabled(isShowValues);
      _lblHideSmallValuesUnit.setEnabled(isShowValues && isHideSmallValues);
      _spinHideSmallValues.setEnabled(isShowValues && isHideSmallValues);

      _lblVisibleStackedValues.setEnabled(isShowValues);
      _spinVisibleValuesStacked.setEnabled(isShowValues);

      _valueFontEditor.setEnabled(isShowValues, _fontContainer);
   }

   @Override
   public void fontDialogOpened(final boolean isDialogOpened) {
      setIsAnotherDialogOpened(isDialogOpened);
   }

   @Override
   public Point getToolTipLocation(final Point tipSize) {

      final int tipWidth = tipSize.x;
      final int tipHeight = tipSize.y;

      final int itemWidth = _toolTipItemBounds.width;
      final int itemHeight = _toolTipItemBounds.height;

      // center horizontally
      final int devX = _toolTipItemBounds.x + itemWidth / 2 - tipWidth / 2;
      int devY = _toolTipItemBounds.y + itemHeight + 0;

      final Rectangle displayBounds = _shellContainer.getShell().getDisplay().getBounds();

      if (devY + tipHeight > displayBounds.height) {

         // slideout is below bottom, show it above the action button

         devY = _toolTipItemBounds.y - tipHeight;
      }

      return new Point(devX, devY);
   }

   @Override
   protected Rectangle noHideOnMouseMove() {

      return _toolTipItemBounds;
   }

   private void onChangeFontInEditor() {

      enableControls();

      // resize slideout
      final Shell shell = _shellContainer.getShell();
      shell.pack(true);

      /*
       * Update state
       */
      _valueFontEditor.store();

      /*
       * Update UI
       */
      _tourSegmenterView.fireSegmentLayerChanged();
   }

   private void onChangeUI() {

      enableControls();

      saveState();

      /*
       * Update UI
       */
      _tourSegmenterView.fireSegmentLayerChanged();
   }

   private void onChangeUI_Segmenter() {

      saveState_Segmenter();

      _tourSegmenterView.reloadViewer();
   }

   /**
    * @param toolTipItemBounds
    * @param isOpenDelayed
    */
   public void open(final Rectangle toolTipItemBounds, final boolean isOpenDelayed) {

      if (isToolTipVisible()) {
         return;
      }

      if (isOpenDelayed == false) {

         if (toolTipItemBounds != null) {

            _toolTipItemBounds = toolTipItemBounds;

            showToolTip();
         }

      } else {

         if (toolTipItemBounds == null) {

            // item is not hovered any more

            _canOpenToolTip = false;

            return;
         }

         _toolTipItemBounds = toolTipItemBounds;
         _canOpenToolTip = true;

         if (_isWaitTimerStarted == false) {

            _isWaitTimerStarted = true;

            Display.getCurrent().timerExec(50, _waitTimer);
         }
      }
   }

   private void open_Runnable() {

      _isWaitTimerStarted = false;

      if (_canOpenToolTip) {
         showToolTip();
      }
   }

   @Override
   public void resetToDefaults() {

      // set font editor default values
      _valueFontEditor.loadDefault();
      _valueFontEditor.store();

      // hide small values
      _segmenterState.put(
            TourSegmenterView.STATE_IS_HIDE_SMALL_VALUES,
            TourSegmenterView.STATE_IS_HIDE_SMALL_VALUES_DEFAULT);
      _segmenterState.put(
            TourSegmenterView.STATE_SMALL_VALUE_SIZE,
            TourSegmenterView.STATE_SMALL_VALUE_SIZE_DEFAULT);

      // show segment lines
      _segmenterState.put(
            TourSegmenterView.STATE_IS_SHOW_SEGMENTER_LINE,
            TourSegmenterView.STATE_IS_SHOW_SEGMENTER_LINE_DEFAULT);
      _segmenterState.put(
            TourSegmenterView.STATE_LINE_OPACITY,
            TourSegmenterView.STATE_LINE_OPACITY_DEFAULT);

      _segmenterState.put(
            TourSegmenterView.STATE_IS_SHOW_SEGMENTER_DECIMAL_PLACES,
            TourSegmenterView.STATE_IS_SHOW_SEGMENTER_DECIMAL_PLACES_DEFAULT);
      _segmenterState.put(
            TourSegmenterView.STATE_IS_SHOW_SEGMENTER_MARKER,
            TourSegmenterView.STATE_IS_SHOW_SEGMENTER_MARKER_DEFAULT);
      _segmenterState.put(
            TourSegmenterView.STATE_IS_SHOW_SEGMENTER_TOOLTIP,
            TourSegmenterView.STATE_IS_SHOW_SEGMENTER_TOOLTIP_DEFAULT);
      _segmenterState.put(
            TourSegmenterView.STATE_IS_SHOW_SEGMENTER_VALUE,
            TourSegmenterView.STATE_IS_SHOW_SEGMENTER_VALUE_DEFAULT);

      _segmenterState.put(
            TourSegmenterView.STATE_GRAPH_OPACITY,
            TourSegmenterView.STATE_GRAPH_OPACITY_DEFAULT);
      _segmenterState.put(
            TourSegmenterView.STATE_STACKED_VISIBLE_VALUES,
            TourSegmenterView.STATE_STACKED_VISIBLE_VALUES_DEFAULT);

      resetToDefaults_Segmenter();

      // update UI with the default values from the state/pref store
      restoreState();

      enableControls();

      /*
       * Update UI
       */
      _tourSegmenterView.fireSegmentLayerChanged();
      _tourSegmenterView.reloadViewer();
   }

   private void resetToDefaults_Segmenter() {

      /*
       * Colors
       */

// SET_FORMATTING_OFF

      final RGB rgbAltitudeDownDefault_Light    = TourSegmenterView.STATE_COLOR_ALTITUDE_DOWN_DEFAULT;
      final RGB rgbAltitudeDownDefault_Dark     = TourSegmenterView.STATE_COLOR_ALTITUDE_DOWN_DEFAULT_DARK;
      final RGB rgbAltitudeUpDefault_Light      = TourSegmenterView.STATE_COLOR_ALTITUDE_UP_DEFAULT;
      final RGB rgbAltitudeUpDefault_Dark       = TourSegmenterView.STATE_COLOR_ALTITUDE_UP_DEFAULT_DARK;
      final RGB rgbTotalsDefault_Light          = TourSegmenterView.STATE_COLOR_TOTALS_DEFAULT;
      final RGB rgbTotalsDefault_Dark           = TourSegmenterView.STATE_COLOR_TOTALS_DEFAULT_DARK;

      Util.setState(_segmenterState, TourSegmenterView.STATE_COLOR_ALTITUDE_DOWN,      rgbAltitudeDownDefault_Light);
      Util.setState(_segmenterState, TourSegmenterView.STATE_COLOR_ALTITUDE_DOWN_DARK, rgbAltitudeDownDefault_Dark);
      Util.setState(_segmenterState, TourSegmenterView.STATE_COLOR_ALTITUDE_UP,        rgbAltitudeUpDefault_Light);
      Util.setState(_segmenterState, TourSegmenterView.STATE_COLOR_ALTITUDE_UP_DARK,   rgbAltitudeUpDefault_Dark);
      Util.setState(_segmenterState, TourSegmenterView.STATE_COLOR_TOTALS,             rgbTotalsDefault_Light);
      Util.setState(_segmenterState, TourSegmenterView.STATE_COLOR_TOTALS_DARK,        rgbTotalsDefault_Dark);

      final ColorCache colorCache = _tourSegmenterView.getColorCache();

      colorCache.replaceColor(TourSegmenterView.STATE_COLOR_ALTITUDE_DOWN,       rgbAltitudeDownDefault_Light);
      colorCache.replaceColor(TourSegmenterView.STATE_COLOR_ALTITUDE_DOWN_DARK,  rgbAltitudeDownDefault_Dark);
      colorCache.replaceColor(TourSegmenterView.STATE_COLOR_ALTITUDE_UP,         rgbAltitudeUpDefault_Light);
      colorCache.replaceColor(TourSegmenterView.STATE_COLOR_ALTITUDE_UP_DARK,    rgbAltitudeUpDefault_Dark);
      colorCache.replaceColor(TourSegmenterView.STATE_COLOR_TOTALS,              rgbTotalsDefault_Light);
      colorCache.replaceColor(TourSegmenterView.STATE_COLOR_TOTALS_DARK,         rgbTotalsDefault_Dark);

// SET_FORMATTING_ON
   }

   private void restoreState() {

      // hide small values
      _chkHideSmallValues.setSelection(Util.getStateBoolean(
            _segmenterState,
            TourSegmenterView.STATE_IS_HIDE_SMALL_VALUES,
            TourSegmenterView.STATE_IS_HIDE_SMALL_VALUES_DEFAULT));
      _spinHideSmallValues.setSelection(Util.getStateInt(
            _segmenterState,
            TourSegmenterView.STATE_SMALL_VALUE_SIZE,
            TourSegmenterView.STATE_SMALL_VALUE_SIZE_DEFAULT));

      // show segment lines
      _chkShowSegmentLine.setSelection(Util.getStateBoolean(_segmenterState,
            TourSegmenterView.STATE_IS_SHOW_SEGMENTER_LINE,
            TourSegmenterView.STATE_IS_SHOW_SEGMENTER_LINE_DEFAULT));
      _spinLineOpacity.setSelection(Util.getStateInt(_segmenterState,
            TourSegmenterView.STATE_LINE_OPACITY,
            TourSegmenterView.STATE_LINE_OPACITY_DEFAULT));

      _chkShowDecimalPlaces.setSelection(Util.getStateBoolean(_segmenterState,
            TourSegmenterView.STATE_IS_SHOW_SEGMENTER_DECIMAL_PLACES,
            TourSegmenterView.STATE_IS_SHOW_SEGMENTER_DECIMAL_PLACES_DEFAULT));

      _chkShowSegmentMarker.setSelection(Util.getStateBoolean(_segmenterState,
            TourSegmenterView.STATE_IS_SHOW_SEGMENTER_MARKER,
            TourSegmenterView.STATE_IS_SHOW_SEGMENTER_MARKER_DEFAULT));

      _chkShowSegmentTooltip.setSelection(Util.getStateBoolean(_segmenterState,
            TourSegmenterView.STATE_IS_SHOW_SEGMENTER_TOOLTIP,
            TourSegmenterView.STATE_IS_SHOW_SEGMENTER_TOOLTIP_DEFAULT));

      _chkShowSegmentValue.setSelection(Util.getStateBoolean(_segmenterState,
            TourSegmenterView.STATE_IS_SHOW_SEGMENTER_VALUE,
            TourSegmenterView.STATE_IS_SHOW_SEGMENTER_VALUE_DEFAULT));

      _spinGraphOpacity.setSelection(Util.getStateInt(_segmenterState,
            TourSegmenterView.STATE_GRAPH_OPACITY,
            TourSegmenterView.STATE_GRAPH_OPACITY_DEFAULT));

      _spinVisibleValuesStacked.setSelection(Util.getStateInt(_segmenterState,
            TourSegmenterView.STATE_STACKED_VISIBLE_VALUES,
            TourSegmenterView.STATE_STACKED_VISIBLE_VALUES_DEFAULT));

      restoreState_Font();
      restoreState_Segmenter();
   }

   private void restoreState_Font() {

      _valueFontEditor.setPreferenceStore(_prefStore);
      _valueFontEditor.load();
   }

   private void restoreState_Segmenter() {

      /*
       * Colors
       */
      _colorSegmenterElevationDown_Light.setColorValue(Util.getStateRGB(_segmenterState,
            TourSegmenterView.STATE_COLOR_ALTITUDE_DOWN,
            TourSegmenterView.STATE_COLOR_ALTITUDE_DOWN_DEFAULT));
      _colorSegmenterElevationDown_Dark.setColorValue(Util.getStateRGB(_segmenterState,
            TourSegmenterView.STATE_COLOR_ALTITUDE_DOWN_DARK,
            TourSegmenterView.STATE_COLOR_ALTITUDE_DOWN_DEFAULT_DARK));

      _colorSegmenterElevationUp_Light.setColorValue(Util.getStateRGB(_segmenterState,
            TourSegmenterView.STATE_COLOR_ALTITUDE_UP,
            TourSegmenterView.STATE_COLOR_ALTITUDE_UP_DEFAULT));
      _colorSegmenterElevationUp_Dark.setColorValue(Util.getStateRGB(_segmenterState,
            TourSegmenterView.STATE_COLOR_ALTITUDE_UP_DARK,
            TourSegmenterView.STATE_COLOR_ALTITUDE_UP_DEFAULT_DARK));

      _colorSegmenterTotals_Light.setColorValue(Util.getStateRGB(_segmenterState,
            TourSegmenterView.STATE_COLOR_TOTALS,
            TourSegmenterView.STATE_COLOR_TOTALS_DEFAULT));
      _colorSegmenterTotals_Dark.setColorValue(Util.getStateRGB(_segmenterState,
            TourSegmenterView.STATE_COLOR_TOTALS_DARK,
            TourSegmenterView.STATE_COLOR_TOTALS_DEFAULT_DARK));
   }

   private void saveState() {

      // !!! font editor saves it's values automatically !!!

// SET_FORMATTING_OFF

		// hide small values
		_segmenterState.put(TourSegmenterView.STATE_IS_HIDE_SMALL_VALUES,     _chkHideSmallValues.getSelection());
		_segmenterState.put(TourSegmenterView.STATE_SMALL_VALUE_SIZE,         _spinHideSmallValues.getSelection());

		// show segment lines
		_segmenterState.put(TourSegmenterView.STATE_IS_SHOW_SEGMENTER_LINE,   _chkShowSegmentLine.getSelection());
		_segmenterState.put(TourSegmenterView.STATE_LINE_OPACITY,             _spinLineOpacity.getSelection());

		_segmenterState.put(TourSegmenterView.STATE_IS_SHOW_SEGMENTER_DECIMAL_PLACES,  _chkShowDecimalPlaces.getSelection());
		_segmenterState.put(TourSegmenterView.STATE_IS_SHOW_SEGMENTER_MARKER,          _chkShowSegmentMarker.getSelection());
		_segmenterState.put(TourSegmenterView.STATE_IS_SHOW_SEGMENTER_TOOLTIP,         _chkShowSegmentTooltip.getSelection());
		_segmenterState.put(TourSegmenterView.STATE_IS_SHOW_SEGMENTER_VALUE,           _chkShowSegmentValue.getSelection());

		_segmenterState.put(TourSegmenterView.STATE_GRAPH_OPACITY,            _spinGraphOpacity.getSelection());
		_segmenterState.put(TourSegmenterView.STATE_STACKED_VISIBLE_VALUES,   _spinVisibleValuesStacked.getSelection());

// SET_FORMATTING_ON
   }

   private void saveState_Segmenter() {

      /*
       * Color values
       */

// SET_FORMATTING_OFF

      final RGB rgbElevationUp_Light      = _colorSegmenterElevationUp_Light.getColorValue();
      final RGB rgbElevationUp_Dark       = _colorSegmenterElevationUp_Dark.getColorValue();
      final RGB rgbElevationDown_Light    = _colorSegmenterElevationDown_Light.getColorValue();
      final RGB rgbElevationDown_Dark     = _colorSegmenterElevationDown_Dark.getColorValue();
      final RGB rgbTotals_Light           = _colorSegmenterTotals_Light.getColorValue();
      final RGB rgbTotals_Dark            = _colorSegmenterTotals_Dark.getColorValue();

      Util.setState(_segmenterState, TourSegmenterView.STATE_COLOR_ALTITUDE_DOWN,      rgbElevationDown_Light);
      Util.setState(_segmenterState, TourSegmenterView.STATE_COLOR_ALTITUDE_DOWN_DARK, rgbElevationDown_Dark);
      Util.setState(_segmenterState, TourSegmenterView.STATE_COLOR_ALTITUDE_UP,        rgbElevationUp_Light);
      Util.setState(_segmenterState, TourSegmenterView.STATE_COLOR_ALTITUDE_UP_DARK,   rgbElevationUp_Dark);
      Util.setState(_segmenterState, TourSegmenterView.STATE_COLOR_TOTALS,             rgbTotals_Light);
      Util.setState(_segmenterState, TourSegmenterView.STATE_COLOR_TOTALS_DARK,        rgbTotals_Dark);

      final ColorCache colorCache = _tourSegmenterView.getColorCache();

      colorCache.replaceColor(TourSegmenterView.STATE_COLOR_ALTITUDE_DOWN,       rgbElevationDown_Light);
      colorCache.replaceColor(TourSegmenterView.STATE_COLOR_ALTITUDE_DOWN_DARK,  rgbElevationDown_Dark);
      colorCache.replaceColor(TourSegmenterView.STATE_COLOR_ALTITUDE_UP,         rgbElevationUp_Light);
      colorCache.replaceColor(TourSegmenterView.STATE_COLOR_ALTITUDE_UP_DARK,    rgbElevationUp_Dark);
      colorCache.replaceColor(TourSegmenterView.STATE_COLOR_TOTALS,              rgbTotals_Light);
      colorCache.replaceColor(TourSegmenterView.STATE_COLOR_TOTALS_DARK,         rgbTotals_Dark);

// SET_FORMATTING_ON
   }

}
