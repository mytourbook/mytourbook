/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.tourChart;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import net.tourbook.Messages;
import net.tourbook.OtherMessages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionResetToDefaults;
import net.tourbook.common.action.IActionResetToDefault;
import net.tourbook.common.color.ColorSelectorExtended;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.data.TourMarker;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Tour chart marker properties slideout.
 */
public class SlideoutTourChartMarker extends ToolbarSlideout implements IColorSelectorListener, IActionResetToDefault {

   private final IPreferenceStore  _prefStore = TourbookPlugin.getPrefStore();

   private SelectionListener       _defaultSelectionListener;
   private MouseWheelListener      _defaultMouseWheelListener;
   private IPropertyChangeListener _defaultPropertyChangeListener;
   private FocusListener           _keepOpenListener;

   {
      _defaultSelectionListener = widgetSelectedAdapter(selectionEvent -> onChangeUI());

      _defaultMouseWheelListener = mouseEvent -> {
         UI.adjustSpinnerValueOnMouseScroll(mouseEvent);
         onChangeUI();
      };

      _defaultPropertyChangeListener = propertyChangeEvent -> onChangeUI();

      _keepOpenListener = new FocusListener() {

         @Override
         public void focusGained(final FocusEvent e) {

            /*
             * This will fix the problem that when the list of a combobox is displayed, then the
             * slideout will disappear :-(((
             */
            setIsAnotherDialogOpened(true);
         }

         @Override
         public void focusLost(final FocusEvent e) {
            setIsAnotherDialogOpened(false);
         }
      };
   }

   private PixelConverter        _pc;

   private ActionResetToDefaults _actionRestoreDefaults;

   /*
    * UI controls
    */
   private TourChart             _tourChart;

   private Button                _chkDrawMarkerWithDefaultColor;
   private Button                _chkShowAbsoluteValues;
   private Button                _chkShowHiddenMarker;
   private Button                _chkShowLabelTempPosition;
   private Button                _chkShowMarkerLabel;
   private Button                _chkShowMarkerPoint;
   private Button                _chkShowMarkerTooltip;
   private Button                _chkTooltipData_Elevation;
   private Button                _chkTooltipData_Distance;
   private Button                _chkTooltipData_Duration;
   private Button                _chkTooltipData_ElevationGainDifference;
   private Button                _chkTooltipData_DistanceDifference;
   private Button                _chkTooltipData_DurationDifference;
   private Button                _chkShowOnlyWithDescription;

   /**
    * Label temporary position, this position is not saved in the marker.
    */
   private Combo                 _comboLabelTempPosition;
   private Combo                 _comboTooltipPosition;

   private ColorSelectorExtended _colorDefaultMarker_Light;
   private ColorSelectorExtended _colorDefaultMarker_Dark;
   private ColorSelectorExtended _colorDeviceMarker_Light;
   private ColorSelectorExtended _colorDeviceMarker_Dark;
   private ColorSelectorExtended _colorHiddenMarker_Light;
   private ColorSelectorExtended _colorHiddenMarker_Dark;

   private Label                 _lblLabelOffset;
   private Label                 _lblMarkerPointSize;

   private Spinner               _spinHoverSize;
   private Spinner               _spinLabelOffset;
   private Spinner               _spinMarkerPointSize;

   public SlideoutTourChartMarker(final Control ownerControl,
                                  final ToolBar toolBar,
                                  final TourChart tourChart) {

      super(ownerControl, toolBar);

      _tourChart = tourChart;
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

      fillUI();
      restoreState();
      enableControls();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.swtDefaults().applyTo(shellContainer);
      {
         final Composite container = new Composite(shellContainer, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         GridLayoutFactory.fillDefaults().applyTo(container);
//         container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
         {
            createUI_10_Header(container);
            createUI_20_Properties(container);
            createUI_50_TempPosition(container);
            createUI_70_TooltipData(container);
            createUI_90_Bottom(container);
         }
      }

      return shellContainer;
   }

   private void createUI_10_Header(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         {
            /*
             * Label: Slideout title
             */
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Slideout_ChartMarkerOptions_Label_Title);
            GridDataFactory.fillDefaults().applyTo(label);
            MTFont.setBannerFont(label);
         }
         {
            final ToolBar toolbar = new ToolBar(container, SWT.FLAT);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .align(SWT.END, SWT.BEGINNING)
                  .applyTo(toolbar);

            final ToolBarManager tbm = new ToolBarManager(toolbar);

            tbm.add(_actionRestoreDefaults);

            tbm.update(true);
         }
      }
   }

   private void createUI_20_Properties(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
         {
            final Composite ttContainer = new Composite(container, SWT.NONE);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .span(2, 1)
                  .applyTo(ttContainer);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(ttContainer);
            {
               {
                  /*
                   * Show marker tooltip
                   */
                  _chkShowMarkerTooltip = new Button(ttContainer, SWT.CHECK);
                  _chkShowMarkerTooltip.setText(Messages.Slideout_ChartMarkerOptions_Checkbox_IsShowMarkerTooltip);
                  _chkShowMarkerTooltip.addSelectionListener(_defaultSelectionListener);
                  GridDataFactory.fillDefaults()
                        .align(SWT.FILL, SWT.CENTER)
                        .applyTo(_chkShowMarkerTooltip);
               }
               {
                  /*
                   * Combo: tooltip position
                   */
                  _comboTooltipPosition = new Combo(ttContainer, SWT.DROP_DOWN | SWT.READ_ONLY);
                  _comboTooltipPosition.setVisibleItemCount(20);
                  _comboTooltipPosition.setToolTipText(Messages.Slideout_ChartMarkerOptions_Combo_TooltipPosition_Tooltip);
                  _comboTooltipPosition.addSelectionListener(_defaultSelectionListener);
                  _comboTooltipPosition.addFocusListener(_keepOpenListener);
                  GridDataFactory.fillDefaults()
                        .grab(true, false)
                        .align(SWT.END, SWT.FILL)
                        .applyTo(_comboTooltipPosition);
               }
               {
                  /*
                   * Show relative/absolute values
                   */
                  _chkShowAbsoluteValues = new Button(ttContainer, SWT.CHECK);
                  _chkShowAbsoluteValues.setText(Messages.Slideout_ChartMarkerOptions_Checkbox_IsShowAbsoluteValues);
                  _chkShowAbsoluteValues.setToolTipText(Messages.Slideout_ChartMarkerOptions_Checkbox_IsShowAbsoluteValues_Tooltip);
                  _chkShowAbsoluteValues.addSelectionListener(_defaultSelectionListener);
                  GridDataFactory.fillDefaults()
                        .span(2, 1)
                        .indent(_pc.convertWidthInCharsToPixels(3), 0)
                        .applyTo(_chkShowAbsoluteValues);
               }
            }
         }

         {
            /*
             * Show labels
             */
            // show label
            _chkShowMarkerLabel = new Button(container, SWT.CHECK);
            _chkShowMarkerLabel.setText(Messages.Slideout_ChartMarkerOptions_Checkbox_IsShowMarker);
            _chkShowMarkerLabel.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults()
                  .span(2, 1)
                  .applyTo(_chkShowMarkerLabel);
         }

         {
            /*
             * Show marker point
             */
            _chkShowMarkerPoint = new Button(container, SWT.CHECK);
            _chkShowMarkerPoint.setText(Messages.Slideout_ChartMarkerOptions_Checkbox_IsShowMarkerPoint);
            _chkShowMarkerPoint.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults()
                  .span(2, 1)
                  .applyTo(_chkShowMarkerPoint);
         }

         {
            /*
             * Show hidden marker
             */
            _chkShowHiddenMarker = new Button(container, SWT.CHECK);
            _chkShowHiddenMarker.setText(Messages.Slideout_ChartMarkerOptions_Checkbox_IsShowHiddenMarker);
            _chkShowHiddenMarker.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults()
                  .span(2, 1)
                  .applyTo(_chkShowHiddenMarker);
         }

         {
            /*
             * Show hidden marker
             */
            _chkShowOnlyWithDescription = new Button(container, SWT.CHECK);
            _chkShowOnlyWithDescription.setText(Messages.Slideout_ChartMarkerOptions_Checkbox_IsShowOnlyWithDescription);
            _chkShowOnlyWithDescription.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults()
                  .span(2, 1)
                  .applyTo(_chkShowOnlyWithDescription);
         }

         {
            /*
             * Draw marker with default color
             */
            _chkDrawMarkerWithDefaultColor = new Button(container, SWT.CHECK);
            _chkDrawMarkerWithDefaultColor.setText(Messages.Slideout_ChartMarkerOptions_Checkbox_IsShowMarkerWithDefaultColor);
            _chkDrawMarkerWithDefaultColor.setToolTipText(Messages.Slideout_ChartMarkerOptions_Checkbox_IsShowMarkerWithDefaultColor_Tooltip);
            _chkDrawMarkerWithDefaultColor.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults()
                  .span(2, 1)
                  .applyTo(_chkDrawMarkerWithDefaultColor);
         }
      }
   }

   private void createUI_50_TempPosition(final Composite parent) {

      {
         /*
          * Temp position
          */
         final Composite tempContainer = new Composite(parent, SWT.NONE);
         GridDataFactory
               .fillDefaults()//
               .grab(true, false)
               .span(2, 1)
               .applyTo(tempContainer);
         GridLayoutFactory.fillDefaults().numColumns(1).applyTo(tempContainer);
//         tempContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
         {
            // show temp position
            _chkShowLabelTempPosition = new Button(tempContainer, SWT.CHECK);
            _chkShowLabelTempPosition.setText(Messages.Slideout_ChartMarkerOptions_Checkbox_IsShowTempPosition);
            _chkShowLabelTempPosition.setToolTipText(Messages.Slideout_ChartMarkerOptions_Checkbox_IsShowTempPosition_Tooltip);
            _chkShowLabelTempPosition.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults().applyTo(_chkShowLabelTempPosition);

            {
               /*
                * Combo: temp position
                */
               _comboLabelTempPosition = new Combo(tempContainer, SWT.DROP_DOWN | SWT.READ_ONLY);
               _comboLabelTempPosition.setVisibleItemCount(20);
               _comboLabelTempPosition.addSelectionListener(_defaultSelectionListener);
               _comboLabelTempPosition.addFocusListener(_keepOpenListener);
               GridDataFactory.fillDefaults()
                     .indent(_pc.convertWidthInCharsToPixels(3), 0)
                     .applyTo(_comboLabelTempPosition);
            }
         }
      }
   }

   private void createUI_70_TooltipData(final Composite container) {

      final Group groupData = new Group(container, SWT.NONE);
      groupData.setText(Messages.Slideout_ChartMarkerOptions_Group_TooltipData);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(groupData);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(groupData);
      {
         _chkTooltipData_Elevation = new Button(groupData, SWT.CHECK);
         _chkTooltipData_Elevation.setText(OtherMessages.GRAPH_LABEL_ALTITUDE);
         _chkTooltipData_Elevation.addSelectionListener(_defaultSelectionListener);

         _chkTooltipData_ElevationGainDifference = new Button(groupData, SWT.CHECK);
         _chkTooltipData_ElevationGainDifference.setText(UI.SYMBOL_DIFFERENCE_WITH_SPACE + OtherMessages.GRAPH_LABEL_ELEVATION_GAIN);
         _chkTooltipData_ElevationGainDifference.addSelectionListener(_defaultSelectionListener);

         _chkTooltipData_Distance = new Button(groupData, SWT.CHECK);
         _chkTooltipData_Distance.setText(OtherMessages.GRAPH_LABEL_DISTANCE);
         _chkTooltipData_Distance.addSelectionListener(_defaultSelectionListener);

         _chkTooltipData_DistanceDifference = new Button(groupData, SWT.CHECK);
         _chkTooltipData_DistanceDifference.setText(UI.SYMBOL_DIFFERENCE_WITH_SPACE + OtherMessages.GRAPH_LABEL_DISTANCE);
         _chkTooltipData_DistanceDifference.addSelectionListener(_defaultSelectionListener);

         _chkTooltipData_Duration = new Button(groupData, SWT.CHECK);
         _chkTooltipData_Duration.setText(OtherMessages.GRAPH_LABEL_TIME);
         _chkTooltipData_Duration.addSelectionListener(_defaultSelectionListener);

         _chkTooltipData_DurationDifference = new Button(groupData, SWT.CHECK);
         _chkTooltipData_DurationDifference.setText(UI.SYMBOL_DIFFERENCE_WITH_SPACE + OtherMessages.GRAPH_LABEL_TIME);
         _chkTooltipData_DurationDifference.addSelectionListener(_defaultSelectionListener);
      }
   }

   private void createUI_90_Bottom(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .applyTo(container);
      GridLayoutFactory.fillDefaults()
            .numColumns(2)
            .spacing(_pc.convertWidthInCharsToPixels(2), 0)
            .applyTo(container);
      {
         createUI_92_Sizes(container);
         createUI_94_Colors(container);
      }
   }

   private void createUI_92_Sizes(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         {
            /*
             * Label offset
             */

            // Label
            _lblLabelOffset = new Label(container, SWT.NONE);
            _lblLabelOffset.setText(Messages.Slideout_ChartMarkerOptions_Label_Offset);
            _lblLabelOffset.setToolTipText(Messages.Slideout_ChartMarkerOptions_Label_Offset_Tooltip);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .applyTo(_lblLabelOffset);

            // Spinner
            _spinLabelOffset = new Spinner(container, SWT.BORDER);
            _spinLabelOffset.setMinimum(-99);
            _spinLabelOffset.setMaximum(100);
            _spinLabelOffset.setPageIncrement(5);
            _spinLabelOffset.addSelectionListener(_defaultSelectionListener);
            _spinLabelOffset.addMouseWheelListener(_defaultMouseWheelListener);
         }

         {
            /*
             * Marker point size
             */

            // Label
            _lblMarkerPointSize = new Label(container, SWT.NONE);
            _lblMarkerPointSize.setText(Messages.Slideout_ChartMarkerOptions_Label_MarkerSize);
            _lblMarkerPointSize.setToolTipText(Messages.Slideout_ChartMarkerOptions_Label_MarkerSize_Tooltip);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .applyTo(_lblMarkerPointSize);

            // Spinner
            _spinMarkerPointSize = new Spinner(container, SWT.BORDER);
            _spinMarkerPointSize.setMinimum(0);
            _spinMarkerPointSize.setMaximum(100);
            _spinMarkerPointSize.setPageIncrement(5);
            _spinMarkerPointSize.addSelectionListener(_defaultSelectionListener);
            _spinMarkerPointSize.addMouseWheelListener(_defaultMouseWheelListener);
         }

         {
            /*
             * Hover size
             */

            // Label
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Slideout_ChartMarkerOptions_Label_HoverSize);
            label.setToolTipText(Messages.Slideout_ChartMarkerOptions_Label_HoverSize_Tooltip);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .applyTo(label);

            // Spinner
            _spinHoverSize = new Spinner(container, SWT.BORDER);
            _spinHoverSize.setMinimum(0);
            _spinHoverSize.setMaximum(100);
            _spinHoverSize.setPageIncrement(5);
            _spinHoverSize.addSelectionListener(_defaultSelectionListener);
            _spinHoverSize.addMouseWheelListener(_defaultMouseWheelListener);
         }
      }
   }

   private void createUI_94_Colors(final Composite parent) {

      final GridDataFactory colorLayout = GridDataFactory.swtDefaults()
            .grab(false, true)
            .align(SWT.BEGINNING, SWT.BEGINNING);

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .align(SWT.FILL, SWT.BEGINNING)
            .applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
      {
         /*
          * Default color
          */
         {
            // Label
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Slideout_ChartMarkerOptions_Label_MarkerColor);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .applyTo(label);

            // Color selector: light
            _colorDefaultMarker_Light = new ColorSelectorExtended(container);
            _colorDefaultMarker_Light.addOpenListener(this);
            _colorDefaultMarker_Light.addListener(_defaultPropertyChangeListener);
            _colorDefaultMarker_Light.getButton().setToolTipText(OtherMessages.APP_THEME_FOREGROUND_COLOR_LIGHT_TOOLTIP);
            colorLayout.applyTo(_colorDefaultMarker_Light.getButton());

            // Color selector: dark
            _colorDefaultMarker_Dark = new ColorSelectorExtended(container);
            _colorDefaultMarker_Dark.addOpenListener(this);
            _colorDefaultMarker_Dark.addListener(_defaultPropertyChangeListener);
            _colorDefaultMarker_Dark.getButton().setToolTipText(OtherMessages.APP_THEME_FOREGROUND_COLOR_DARK_TOOLTIP);
            colorLayout.applyTo(_colorDefaultMarker_Dark.getButton());
         }

         /*
          * Device marker color
          */
         {
            // Label
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Slideout_ChartMarkerOptions_Label_DeviceMarkerColor);
            label.setToolTipText(Messages.Slideout_ChartMarkerOptions_Label_DeviceMarkerColor_Tooltip);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .applyTo(label);

            // Color selector: light
            _colorDeviceMarker_Light = new ColorSelectorExtended(container);
            _colorDeviceMarker_Light.addOpenListener(this);
            _colorDeviceMarker_Light.addListener(_defaultPropertyChangeListener);
            _colorDeviceMarker_Light.getButton().setToolTipText(OtherMessages.APP_THEME_FOREGROUND_COLOR_LIGHT_TOOLTIP);
            colorLayout.applyTo(_colorDeviceMarker_Light.getButton());

            // Color selector: light
            _colorDeviceMarker_Dark = new ColorSelectorExtended(container);
            _colorDeviceMarker_Dark.addOpenListener(this);
            _colorDeviceMarker_Dark.addListener(_defaultPropertyChangeListener);
            _colorDeviceMarker_Dark.getButton().setToolTipText(OtherMessages.APP_THEME_FOREGROUND_COLOR_DARK_TOOLTIP);
            colorLayout.applyTo(_colorDeviceMarker_Dark.getButton());
         }

         /*
          * Hidden marker color
          */
         {
            // Label
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Slideout_ChartMarkerOptions_Label_HiddenMarkerColor);
            label.setToolTipText(Messages.Slideout_ChartMarkerOptions_Label_HiddenMarkerColor_Tooltip);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .applyTo(label);

            // Color selector: light
            _colorHiddenMarker_Light = new ColorSelectorExtended(container);
            _colorHiddenMarker_Light.addOpenListener(this);
            _colorHiddenMarker_Light.addListener(_defaultPropertyChangeListener);
            _colorHiddenMarker_Light.getButton().setToolTipText(OtherMessages.APP_THEME_FOREGROUND_COLOR_LIGHT_TOOLTIP);
            colorLayout.applyTo(_colorHiddenMarker_Light.getButton());

            // Color selector: light
            _colorHiddenMarker_Dark = new ColorSelectorExtended(container);
            _colorHiddenMarker_Dark.addOpenListener(this);
            _colorHiddenMarker_Dark.addListener(_defaultPropertyChangeListener);
            _colorHiddenMarker_Dark.getButton().setToolTipText(OtherMessages.APP_THEME_FOREGROUND_COLOR_DARK_TOOLTIP);
            colorLayout.applyTo(_colorHiddenMarker_Dark.getButton());
         }
      }
   }

   private void enableControls() {

      final boolean isShowTempPosition = _chkShowLabelTempPosition.getSelection();
      final boolean isLabelVisible = _chkShowMarkerLabel.getSelection();
      final boolean isMarkerPointVisible = _chkShowMarkerPoint.getSelection();
      final boolean isTooltipVisible = _chkShowMarkerTooltip.getSelection();

      final boolean isMarkerVisible = isLabelVisible || isMarkerPointVisible;// || isImageVisible;
      final boolean isMultipleTours = _tourChart.getTourData().isMultipleTours();

      _comboTooltipPosition.setEnabled(isTooltipVisible);
      _chkShowAbsoluteValues.setEnabled(isTooltipVisible && isMultipleTours);

      _chkShowLabelTempPosition.setEnabled(isLabelVisible);
      _comboLabelTempPosition.setEnabled(isLabelVisible && isShowTempPosition);
      _chkTooltipData_Elevation.setEnabled(isTooltipVisible);
      _chkTooltipData_Distance.setEnabled(isTooltipVisible);
      _chkTooltipData_Duration.setEnabled(isTooltipVisible);
      _chkTooltipData_ElevationGainDifference.setEnabled(isTooltipVisible);
      _chkTooltipData_DistanceDifference.setEnabled(isTooltipVisible);
      _chkTooltipData_DurationDifference.setEnabled(isTooltipVisible);

      _chkShowHiddenMarker.setEnabled(isMarkerVisible);
      _chkShowOnlyWithDescription.setEnabled(isMarkerVisible);
      _chkDrawMarkerWithDefaultColor.setEnabled(isMarkerVisible);

      _lblLabelOffset.setEnabled(isLabelVisible);
      _spinLabelOffset.setEnabled(isLabelVisible);

      _lblMarkerPointSize.setEnabled(isMarkerPointVisible);
      _spinMarkerPointSize.setEnabled(isMarkerPointVisible);
   }

   private void fillUI() {

      /*
       * Fill position combos
       */
      for (final String position : TourMarker.LABEL_POSITIONS) {
         _comboLabelTempPosition.add(position);
      }

      for (final String position : ChartMarkerToolTip.TOOLTIP_POSITIONS) {
         _comboTooltipPosition.add(position);
      }
   }

   private void onChangeUI() {

// SET_FORMATTING_OFF

      final boolean isDrawMarkerWithDefaultColor               = _chkDrawMarkerWithDefaultColor.getSelection();
      final boolean isShowAbsoluteValues                       = _chkShowAbsoluteValues.getSelection();
      final boolean isShowHiddenMarker                         = _chkShowHiddenMarker.getSelection();
      final boolean isShowLabelTempPos                         = _chkShowLabelTempPosition.getSelection();
      final boolean isShowMarkerLabel                          = _chkShowMarkerLabel.getSelection();
      final boolean isShowMarkerPoint                          = _chkShowMarkerPoint.getSelection();
      final boolean isShowMarkerTooltip                        = _chkShowMarkerTooltip.getSelection();
      final boolean isShowOnlyWithDescription                  = _chkShowOnlyWithDescription.getSelection();
      final boolean isShowTooltipData_Elevation                = _chkTooltipData_Elevation.getSelection();
      final boolean isShowTooltipData_Distance                 = _chkTooltipData_Distance.getSelection();
      final boolean isShowTooltipData_Duration                 = _chkTooltipData_Duration.getSelection();
      final boolean isShowTooltipData_ElevationGainDifference  = _chkTooltipData_ElevationGainDifference.getSelection();
      final boolean isShowTooltipData_DistanceDifference       = _chkTooltipData_DistanceDifference.getSelection();
      final boolean isShowTooltipData_DurationDifference       = _chkTooltipData_DurationDifference.getSelection();

      final int hoverSize           = _spinHoverSize.getSelection();
      final int labelOffset         = _spinLabelOffset.getSelection();
      final int markerPointSize     = _spinMarkerPointSize.getSelection();
      final int tempPosition        = _comboLabelTempPosition.getSelectionIndex();
      final int ttPosition          = _comboTooltipPosition.getSelectionIndex();

      final RGB defaultColor_Light  = _colorDefaultMarker_Light.getColorValue();
      final RGB defaultColor_Dark   = _colorDefaultMarker_Dark.getColorValue();
      final RGB deviceColor_Light   = _colorDeviceMarker_Light.getColorValue();
      final RGB deviceColor_Dark    = _colorDeviceMarker_Dark.getColorValue();
      final RGB hiddenColor_Light   = _colorHiddenMarker_Light.getColorValue();
      final RGB hiddenColor_Dark    = _colorHiddenMarker_Dark.getColorValue();

      /*
       * Update pref store
       */
      _prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_HOVER_SIZE,             hoverSize);
      _prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_LABEL_OFFSET,           labelOffset);
      _prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_LABEL_TEMP_POSITION,    tempPosition);
      _prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_POINT_SIZE,             markerPointSize);
      _prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_TOOLTIP_POSITION,       ttPosition);

      _prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_IS_DRAW_WITH_DEFAULT_COLOR,               isDrawMarkerWithDefaultColor);
      _prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_ABSOLUTE_VALUES,                  isShowAbsoluteValues);
      _prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_HIDDEN_MARKER,                    isShowHiddenMarker);
      _prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_LABEL_TEMP_POSITION,              isShowLabelTempPos);
      _prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_MARKER_LABEL,                     isShowMarkerLabel);
      _prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_MARKER_POINT,                     isShowMarkerPoint);
      _prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_MARKER_TOOLTIP,                   isShowMarkerTooltip);
      _prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_TOOLTIP_DATA_ELEVATION,           isShowTooltipData_Elevation);
      _prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_TOOLTIP_DATA_DISTANCE,            isShowTooltipData_Distance);
      _prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_TOOLTIP_DATA_DURATION,            isShowTooltipData_Duration);
      _prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_TOOLTIP_DATA_ELEVATIONGAIN_DIFFERENCE, isShowTooltipData_ElevationGainDifference);
      _prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_TOOLTIP_DATA_DISTANCE_DIFFERENCE, isShowTooltipData_DistanceDifference);
      _prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_TOOLTIP_DATA_DURATION_DIFFERENCE, isShowTooltipData_DurationDifference);
      _prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_ONLY_WITH_DESCRIPTION,            isShowOnlyWithDescription);

      PreferenceConverter.setValue(_prefStore, ITourbookPreferences.GRAPH_MARKER_COLOR_DEFAULT,       defaultColor_Light);
      PreferenceConverter.setValue(_prefStore, ITourbookPreferences.GRAPH_MARKER_COLOR_DEFAULT_DARK,  defaultColor_Dark);
      PreferenceConverter.setValue(_prefStore, ITourbookPreferences.GRAPH_MARKER_COLOR_DEVICE,        deviceColor_Light);
      PreferenceConverter.setValue(_prefStore, ITourbookPreferences.GRAPH_MARKER_COLOR_DEVICE_DARK,   deviceColor_Dark);
      PreferenceConverter.setValue(_prefStore, ITourbookPreferences.GRAPH_MARKER_COLOR_HIDDEN,        hiddenColor_Light);
      PreferenceConverter.setValue(_prefStore, ITourbookPreferences.GRAPH_MARKER_COLOR_HIDDEN_DARK,   hiddenColor_Dark);

      /*
       * Update chart config
       */
      final TourChartConfiguration tcc = _tourChart.getTourChartConfig();

      tcc.isDrawMarkerWithDefaultColor                = isDrawMarkerWithDefaultColor;
      tcc.isShowAbsoluteValues                        = isShowAbsoluteValues;
      tcc.isShowHiddenMarker                          = isShowHiddenMarker;
      tcc.isShowLabelTempPos                          = isShowLabelTempPos;
      tcc.isShowMarkerLabel                           = isShowMarkerLabel;
      tcc.isShowMarkerPoint                           = isShowMarkerPoint;
      tcc.isShowMarkerTooltip                         = isShowMarkerTooltip;
      tcc.isShowTooltipData_Elevation                 = isShowTooltipData_Elevation;
      tcc.isShowTooltipData_Distance                  = isShowTooltipData_Distance;
      tcc.isShowTooltipData_Duration                  = isShowTooltipData_Duration;
      tcc.isShowTooltipData_ElevationGainDifference   = isShowTooltipData_ElevationGainDifference;
      tcc.isShowTooltipData_DistanceDifference        = isShowTooltipData_DistanceDifference;
      tcc.isShowTooltipData_DurationDifference        = isShowTooltipData_DurationDifference;
      tcc.isShowOnlyWithDescription                   = isShowOnlyWithDescription;

      tcc.markerHoverSize           = hoverSize;
      tcc.markerLabelOffset         = labelOffset;
      tcc.markerLabelTempPos        = tempPosition;
      tcc.markerPointSize           = markerPointSize;
      tcc.markerTooltipPosition     = ttPosition;

      tcc.markerColorDefault_Light  = defaultColor_Light;
      tcc.markerColorDefault_Dark   = defaultColor_Dark;
      tcc.markerColorDevice_Light   = deviceColor_Light;
      tcc.markerColorDevice_Dark    = deviceColor_Dark;
      tcc.markerColorHidden_Light   = hiddenColor_Light;
      tcc.markerColorHidden_Dark    = hiddenColor_Dark;

// SET_FORMATTING_ON

      // update chart with new settings
      _tourChart.updateUI_MarkerLayer();

      enableControls();

      // notify pref listener
      TourbookPlugin.getPrefStore().setValue(ITourbookPreferences.GRAPH_MARKER_IS_MODIFIED, Math.random());
   }

   @Override
   public void resetToDefaults() {

      /*
       * Update UI with defaults from pref store
       */

// SET_FORMATTING_OFF

      _chkDrawMarkerWithDefaultColor.setSelection(          _prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_MARKER_IS_DRAW_WITH_DEFAULT_COLOR));
      _chkShowAbsoluteValues.setSelection(                  _prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_ABSOLUTE_VALUES));
      _chkShowHiddenMarker.setSelection(                    _prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_HIDDEN_MARKER));
      _chkShowLabelTempPosition.setSelection(               _prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_LABEL_TEMP_POSITION));
      _chkShowMarkerLabel.setSelection(                     _prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_MARKER_LABEL));
      _chkShowMarkerPoint.setSelection(                     _prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_MARKER_POINT));
      _chkShowMarkerTooltip.setSelection(                   _prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_MARKER_TOOLTIP));
      _chkShowOnlyWithDescription.setSelection(             _prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_ONLY_WITH_DESCRIPTION));
      _chkTooltipData_Elevation.setSelection(               _prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_TOOLTIP_DATA_ELEVATION));
      _chkTooltipData_Distance.setSelection(                _prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_TOOLTIP_DATA_DISTANCE));
      _chkTooltipData_Duration.setSelection(                _prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_TOOLTIP_DATA_DURATION));
      _chkTooltipData_ElevationGainDifference.setSelection( _prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_TOOLTIP_DATA_ELEVATIONGAIN_DIFFERENCE));
      _chkTooltipData_DistanceDifference.setSelection(      _prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_TOOLTIP_DATA_DISTANCE_DIFFERENCE));
      _chkTooltipData_DurationDifference.setSelection(      _prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_TOOLTIP_DATA_DURATION_DIFFERENCE));

      _comboLabelTempPosition.select(        _prefStore.getDefaultInt(ITourbookPreferences.GRAPH_MARKER_LABEL_TEMP_POSITION));
      _comboTooltipPosition.select(          _prefStore.getDefaultInt(ITourbookPreferences.GRAPH_MARKER_TOOLTIP_POSITION));

      _spinHoverSize.setSelection(           _prefStore.getDefaultInt(ITourbookPreferences.GRAPH_MARKER_HOVER_SIZE));
      _spinMarkerPointSize.setSelection(     _prefStore.getDefaultInt(ITourbookPreferences.GRAPH_MARKER_POINT_SIZE));
      _spinLabelOffset.setSelection(         _prefStore.getDefaultInt(ITourbookPreferences.GRAPH_MARKER_LABEL_OFFSET));

      _colorDefaultMarker_Light.setColorValue(  PreferenceConverter.getDefaultColor(_prefStore, ITourbookPreferences.GRAPH_MARKER_COLOR_DEFAULT));
      _colorDefaultMarker_Dark.setColorValue(   PreferenceConverter.getDefaultColor(_prefStore, ITourbookPreferences.GRAPH_MARKER_COLOR_DEFAULT_DARK));
      _colorDeviceMarker_Light.setColorValue(   PreferenceConverter.getDefaultColor(_prefStore, ITourbookPreferences.GRAPH_MARKER_COLOR_DEVICE));
      _colorDeviceMarker_Dark.setColorValue(    PreferenceConverter.getDefaultColor(_prefStore, ITourbookPreferences.GRAPH_MARKER_COLOR_DEVICE_DARK));
      _colorHiddenMarker_Light.setColorValue(   PreferenceConverter.getDefaultColor(_prefStore, ITourbookPreferences.GRAPH_MARKER_COLOR_HIDDEN));
      _colorHiddenMarker_Dark.setColorValue(    PreferenceConverter.getDefaultColor(_prefStore, ITourbookPreferences.GRAPH_MARKER_COLOR_HIDDEN_DARK));

// SET_FORMATTING_ON

      onChangeUI();
   }

   private void restoreState() {

      final TourChartConfiguration tcc = _tourChart.getTourChartConfig();

      final int markerTooltipPosition = tcc.markerTooltipPosition < 0
            ? ChartMarkerToolTip.DEFAULT_TOOLTIP_POSITION
            : tcc.markerTooltipPosition;

// SET_FORMATTING_OFF

      _chkDrawMarkerWithDefaultColor.setSelection(          tcc.isDrawMarkerWithDefaultColor);
      _chkShowAbsoluteValues.setSelection(                  tcc.isShowAbsoluteValues);

      _chkShowHiddenMarker.setSelection(                    tcc.isShowHiddenMarker);
      _chkShowLabelTempPosition.setSelection(               tcc.isShowLabelTempPos);
      _chkShowMarkerLabel.setSelection(                     tcc.isShowMarkerLabel);
      _chkShowMarkerPoint.setSelection(                     tcc.isShowMarkerPoint);
      _chkShowMarkerTooltip.setSelection(                   tcc.isShowMarkerTooltip);
      _chkTooltipData_Elevation.setSelection(               tcc.isShowTooltipData_Elevation);
      _chkTooltipData_Distance.setSelection(                tcc.isShowTooltipData_Distance);
      _chkTooltipData_Duration.setSelection(                tcc.isShowTooltipData_Duration);
      _chkTooltipData_ElevationGainDifference.setSelection( tcc.isShowTooltipData_ElevationGainDifference);
      _chkTooltipData_DistanceDifference.setSelection(      tcc.isShowTooltipData_DistanceDifference);
      _chkTooltipData_DurationDifference.setSelection(      tcc.isShowTooltipData_DurationDifference);
      _chkShowOnlyWithDescription.setSelection(             tcc.isShowOnlyWithDescription);

      _comboLabelTempPosition.select(  tcc.markerLabelTempPos);
      _comboTooltipPosition.select(    markerTooltipPosition);

      _colorDefaultMarker_Light.setColorValue(              tcc.markerColorDefault_Light);
      _colorDefaultMarker_Dark.setColorValue(               tcc.markerColorDefault_Dark);
      _colorDeviceMarker_Light.setColorValue(               tcc.markerColorDevice_Light);
      _colorDeviceMarker_Dark.setColorValue(                tcc.markerColorDevice_Dark);
      _colorHiddenMarker_Light.setColorValue(               tcc.markerColorHidden_Light);
      _colorHiddenMarker_Dark.setColorValue(                tcc.markerColorHidden_Dark);

      _spinHoverSize.setSelection(        tcc.markerHoverSize);
      _spinLabelOffset.setSelection(      tcc.markerLabelOffset);
      _spinMarkerPointSize.setSelection(  tcc.markerPointSize);

// SET_FORMATTING_ON
   }

}
