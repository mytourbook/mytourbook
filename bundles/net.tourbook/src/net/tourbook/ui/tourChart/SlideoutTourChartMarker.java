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
package net.tourbook.ui.tourChart;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.color.ColorSelectorExtended;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.data.TourMarker;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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
public class SlideoutTourChartMarker extends ToolbarSlideout implements IColorSelectorListener {

   private static final String     GRAPH_LABEL_ALTITUDE      = net.tourbook.common.Messages.Graph_Label_Altitude;
   private static final String     GRAPH_LABEL_TIME          = net.tourbook.common.Messages.Graph_Label_Time;
   private static final String     GRAPH_LABEL_DISTANCE      = net.tourbook.common.Messages.Graph_Label_Distance;
   private static final String     GRAPH_LABEL_ELEVATIONGAIN = net.tourbook.common.Messages.Graph_Label_ElevationGain;

   private final IPreferenceStore  _prefStore                = TourbookPlugin.getPrefStore();

   private SelectionAdapter        _defaultSelectionAdapter;
   private MouseWheelListener      _defaultMouseWheelListener;
   private IPropertyChangeListener _defaultPropertyChangeListener;
   private FocusListener           _keepOpenListener;

   {
      _defaultSelectionAdapter = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            onChangeUI();
         }
      };

      _defaultMouseWheelListener = new MouseWheelListener() {
         @Override
         public void mouseScrolled(final MouseEvent event) {
            UI.adjustSpinnerValueOnMouseScroll(event);
            onChangeUI();
         }
      };

      _defaultPropertyChangeListener = new IPropertyChangeListener() {
         @Override
         public void propertyChange(final PropertyChangeEvent event) {
            onChangeUI();
         }
      };

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

   private PixelConverter _pc;

   private Action         _actionRestoreDefaults;

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
   private Button                _chkMarkerTooltip_Elevation;
   private Button                _chkMarkerTooltip_Distance;
   private Button                _chkMarkerTooltip_Duration;
   private Button                _chkMarkerTooltip_ElevationGainDifference;
   private Button                _chkMarkerTooltip_DistanceDifference;
   private Button                _chkMarkerTooltip_DurationDifference;
   private Button                _chkShowOnlyWithDescription;

   /**
    * Label temporary position, this position is not saved in the marker.
    */
   private Combo                 _comboLabelTempPosition;
   private Combo                 _comboTooltipPosition;

   private ColorSelectorExtended _colorDefaultMarker;
   private ColorSelectorExtended _colorDeviceMarker;
   private ColorSelectorExtended _colorHiddenMarker;

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

      /*
       * Action: Restore default
       */
      _actionRestoreDefaults = new Action() {
         @Override
         public void run() {
            resetToDefaults();
         }
      };

      _actionRestoreDefaults.setImageDescriptor(//
            TourbookPlugin.getImageDescriptor(Messages.Image__App_RestoreDefault));
      _actionRestoreDefaults.setToolTipText(Messages.App_Action_RestoreDefault_Tooltip);
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
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
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
            GridDataFactory.fillDefaults().applyTo(label);
            label.setText(Messages.Slideout_ChartMarkerOptions_Label_Title);
            MTFont.setBannerFont(label);
         }
         {
            final ToolBar toolbar = new ToolBar(container, SWT.FLAT);
            GridDataFactory
                  .fillDefaults()//
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
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
         {
            final Composite ttContainer = new Composite(container, SWT.NONE);
            GridDataFactory
                  .fillDefaults()//
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
                  GridDataFactory
                        .fillDefaults()//
                        .align(SWT.FILL, SWT.CENTER)
                        .applyTo(_chkShowMarkerTooltip);
                  _chkShowMarkerTooltip.setText(//
                        Messages.Slideout_ChartMarkerOptions_Checkbox_IsShowMarkerTooltip);
                  _chkShowMarkerTooltip.addSelectionListener(_defaultSelectionAdapter);
               }
               {
                  /*
                   * Combo: tooltip position
                   */
                  _comboTooltipPosition = new Combo(ttContainer, SWT.DROP_DOWN | SWT.READ_ONLY);
                  GridDataFactory
                        .fillDefaults()//
                        .grab(true, false)
                        .align(SWT.END, SWT.FILL)
                        .applyTo(_comboTooltipPosition);
                  _comboTooltipPosition.setVisibleItemCount(20);
                  _comboTooltipPosition.setToolTipText(//
                        Messages.Slideout_ChartMarkerOptions_Combo_TooltipPosition_Tooltip);
                  _comboTooltipPosition.addSelectionListener(_defaultSelectionAdapter);
                  _comboTooltipPosition.addFocusListener(_keepOpenListener);
               }
               {
                  /*
                   * Show relative/absolute values
                   */
                  _chkShowAbsoluteValues = new Button(ttContainer, SWT.CHECK);
                  GridDataFactory
                        .fillDefaults()//
                        .span(2, 1)
                        .indent(_pc.convertWidthInCharsToPixels(3), 0)
                        .applyTo(_chkShowAbsoluteValues);
                  _chkShowAbsoluteValues.setText(//
                        Messages.Slideout_ChartMarkerOptions_Checkbox_IsShowAbsoluteValues);
                  _chkShowAbsoluteValues.setToolTipText(//
                        Messages.Slideout_ChartMarkerOptions_Checkbox_IsShowAbsoluteValues_Tooltip);
                  _chkShowAbsoluteValues.addSelectionListener(_defaultSelectionAdapter);
               }
            }
         }

         {
            /*
             * Show labels
             */
            // show label
            _chkShowMarkerLabel = new Button(container, SWT.CHECK);
            GridDataFactory
                  .fillDefaults()//
                  .span(2, 1)
                  .applyTo(_chkShowMarkerLabel);
            _chkShowMarkerLabel.setText(Messages.Slideout_ChartMarkerOptions_Checkbox_IsShowMarker);
            _chkShowMarkerLabel.addSelectionListener(_defaultSelectionAdapter);
         }

         {
            /*
             * Show marker point
             */
            _chkShowMarkerPoint = new Button(container, SWT.CHECK);
            GridDataFactory
                  .fillDefaults()//
                  .span(2, 1)
                  .applyTo(_chkShowMarkerPoint);
            _chkShowMarkerPoint.setText(Messages.Slideout_ChartMarkerOptions_Checkbox_IsShowMarkerPoint);
            _chkShowMarkerPoint.addSelectionListener(_defaultSelectionAdapter);
         }

         {
            /*
             * Show hidden marker
             */
            _chkShowHiddenMarker = new Button(container, SWT.CHECK);
            GridDataFactory
                  .fillDefaults()//
                  .span(2, 1)
                  .applyTo(_chkShowHiddenMarker);
            _chkShowHiddenMarker.setText(Messages.Slideout_ChartMarkerOptions_Checkbox_IsShowHiddenMarker);
            _chkShowHiddenMarker.addSelectionListener(_defaultSelectionAdapter);
         }

         {
            /*
             * Show hidden marker
             */
            _chkShowOnlyWithDescription = new Button(container, SWT.CHECK);
            GridDataFactory
                  .fillDefaults()//
                  .span(2, 1)
                  .applyTo(_chkShowOnlyWithDescription);
            _chkShowOnlyWithDescription
                  .setText(Messages.Slideout_ChartMarkerOptions_Checkbox_IsShowOnlyWithDescription);
            _chkShowOnlyWithDescription.addSelectionListener(_defaultSelectionAdapter);
         }

         {
            /*
             * Draw marker with default color
             */
            _chkDrawMarkerWithDefaultColor = new Button(container, SWT.CHECK);
            GridDataFactory
                  .fillDefaults()//
                  .span(2, 1)
                  .applyTo(_chkDrawMarkerWithDefaultColor);
            _chkDrawMarkerWithDefaultColor.setText(//
                  Messages.Slideout_ChartMarkerOptions_Checkbox_IsShowMarkerWithDefaultColor);
            _chkDrawMarkerWithDefaultColor.setToolTipText(//
                  Messages.Slideout_ChartMarkerOptions_Checkbox_IsShowMarkerWithDefaultColor_Tooltip);
            _chkDrawMarkerWithDefaultColor.addSelectionListener(_defaultSelectionAdapter);
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
//			tempContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
         {
            // show temp position
            _chkShowLabelTempPosition = new Button(tempContainer, SWT.CHECK);
            GridDataFactory
                  .fillDefaults()//
                  .applyTo(_chkShowLabelTempPosition);
            _chkShowLabelTempPosition.setText(Messages.Slideout_ChartMarkerOptions_Checkbox_IsShowTempPosition);
            _chkShowLabelTempPosition.setToolTipText(//
                  Messages.Slideout_ChartMarkerOptions_Checkbox_IsShowTempPosition_Tooltip);
            _chkShowLabelTempPosition.addSelectionListener(_defaultSelectionAdapter);

            {
               /*
                * Combo: temp position
                */
               _comboLabelTempPosition = new Combo(tempContainer, SWT.DROP_DOWN | SWT.READ_ONLY);
               GridDataFactory
                     .fillDefaults()//
                     .indent(_pc.convertWidthInCharsToPixels(3), 0)
                     .applyTo(_comboLabelTempPosition);
               _comboLabelTempPosition.setVisibleItemCount(20);
               _comboLabelTempPosition.addSelectionListener(_defaultSelectionAdapter);
               _comboLabelTempPosition.addFocusListener(_keepOpenListener);
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
         _chkMarkerTooltip_Elevation = new Button(groupData, SWT.CHECK);
         _chkMarkerTooltip_Elevation.setText(GRAPH_LABEL_ALTITUDE);
         _chkMarkerTooltip_Elevation.addSelectionListener(_defaultSelectionAdapter);

         _chkMarkerTooltip_Distance = new Button(groupData, SWT.CHECK);
         _chkMarkerTooltip_Distance.setText(GRAPH_LABEL_DISTANCE);
         _chkMarkerTooltip_Distance.addSelectionListener(_defaultSelectionAdapter);

         _chkMarkerTooltip_Duration = new Button(groupData, SWT.CHECK);
         _chkMarkerTooltip_Duration.setText(GRAPH_LABEL_TIME);
         _chkMarkerTooltip_Duration.addSelectionListener(_defaultSelectionAdapter);

         _chkMarkerTooltip_ElevationGainDifference = new Button(groupData, SWT.CHECK);
         _chkMarkerTooltip_ElevationGainDifference.setText(UI.SYMBOL_DIFFERENCE_WITH_SPACE + GRAPH_LABEL_ELEVATIONGAIN);
         _chkMarkerTooltip_ElevationGainDifference.addSelectionListener(_defaultSelectionAdapter);

         _chkMarkerTooltip_DistanceDifference = new Button(groupData, SWT.CHECK);
         _chkMarkerTooltip_DistanceDifference.setText(UI.SYMBOL_DIFFERENCE_WITH_SPACE + GRAPH_LABEL_DISTANCE);
         _chkMarkerTooltip_DistanceDifference.addSelectionListener(_defaultSelectionAdapter);

         _chkMarkerTooltip_DurationDifference = new Button(groupData, SWT.CHECK);
         _chkMarkerTooltip_DurationDifference.setText(UI.SYMBOL_DIFFERENCE_WITH_SPACE + GRAPH_LABEL_TIME);
         _chkMarkerTooltip_DurationDifference.addSelectionListener(_defaultSelectionAdapter);
      }
   }

   private void createUI_90_Bottom(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory
            .fillDefaults()//
            .grab(true, false)
            .applyTo(container);
      GridLayoutFactory
            .fillDefaults()//
            .numColumns(2)
            .spacing(_pc.convertWidthInCharsToPixels(4), 0)
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
            GridDataFactory
                  .fillDefaults()//
                  .align(SWT.FILL, SWT.CENTER)
                  .applyTo(_lblLabelOffset);
            _lblLabelOffset.setText(Messages.Slideout_ChartMarkerOptions_Label_Offset);
            _lblLabelOffset.setToolTipText(Messages.Slideout_ChartMarkerOptions_Label_Offset_Tooltip);

            // Spinner
            _spinLabelOffset = new Spinner(container, SWT.BORDER);
            _spinLabelOffset.setMinimum(-99);
            _spinLabelOffset.setMaximum(100);
            _spinLabelOffset.setPageIncrement(5);
            _spinLabelOffset.addSelectionListener(_defaultSelectionAdapter);
            _spinLabelOffset.addMouseWheelListener(_defaultMouseWheelListener);
         }

         {
            /*
             * Marker point size
             */

            // Label
            _lblMarkerPointSize = new Label(container, SWT.NONE);
            GridDataFactory
                  .fillDefaults()//
                  .align(SWT.FILL, SWT.CENTER)
                  .applyTo(_lblMarkerPointSize);
            _lblMarkerPointSize.setText(Messages.Slideout_ChartMarkerOptions_Label_MarkerSize);
            _lblMarkerPointSize.setToolTipText(Messages.Slideout_ChartMarkerOptions_Label_MarkerSize_Tooltip);

            // Spinner
            _spinMarkerPointSize = new Spinner(container, SWT.BORDER);
            _spinMarkerPointSize.setMinimum(0);
            _spinMarkerPointSize.setMaximum(100);
            _spinMarkerPointSize.setPageIncrement(5);
            _spinMarkerPointSize.addSelectionListener(_defaultSelectionAdapter);
            _spinMarkerPointSize.addMouseWheelListener(_defaultMouseWheelListener);
         }

         {
            /*
             * Hover size
             */

            // Label
            final Label label = new Label(container, SWT.NONE);
            GridDataFactory
                  .fillDefaults()//
                  .align(SWT.FILL, SWT.CENTER)
                  .applyTo(label);
            label.setText(Messages.Slideout_ChartMarkerOptions_Label_HoverSize);
            label.setToolTipText(Messages.Slideout_ChartMarkerOptions_Label_HoverSize_Tooltip);

            // Spinner
            _spinHoverSize = new Spinner(container, SWT.BORDER);
            _spinHoverSize.setMinimum(0);
            _spinHoverSize.setMaximum(100);
            _spinHoverSize.setPageIncrement(5);
            _spinHoverSize.addSelectionListener(_defaultSelectionAdapter);
            _spinHoverSize.addMouseWheelListener(_defaultMouseWheelListener);
         }
      }
   }

   private void createUI_94_Colors(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory
            .fillDefaults()//
            .grab(true, false)
            .align(SWT.FILL, SWT.BEGINNING)
            .applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
      {
         /*
          * Default color
          */
         {
            // Label
            final Label label = new Label(container, SWT.NONE);
            GridDataFactory
                  .fillDefaults()//
                  .align(SWT.FILL, SWT.CENTER)
                  .applyTo(label);
            label.setText(Messages.Slideout_ChartMarkerOptions_Label_MarkerColor);

            // Color selector
            _colorDefaultMarker = new ColorSelectorExtended(container);
            GridDataFactory
                  .swtDefaults()//
                  .grab(false, true)
                  .align(SWT.BEGINNING, SWT.BEGINNING)
                  .applyTo(_colorDefaultMarker.getButton());

            _colorDefaultMarker.addOpenListener(this);
            _colorDefaultMarker.addListener(_defaultPropertyChangeListener);
         }

         /*
          * Device marker color
          */
         {
            // Label
            final Label label = new Label(container, SWT.NONE);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .applyTo(label);
            label.setText(Messages.Slideout_ChartMarkerOptions_Label_DeviceMarkerColor);
            label.setToolTipText(Messages.Slideout_ChartMarkerOptions_Label_DeviceMarkerColor_Tooltip);

            // Color selector
            _colorDeviceMarker = new ColorSelectorExtended(container);
            GridDataFactory.swtDefaults()
                  .grab(false, true)
                  .align(SWT.BEGINNING, SWT.BEGINNING)
                  .applyTo(_colorDeviceMarker.getButton());

            _colorDeviceMarker.addOpenListener(this);
            _colorDeviceMarker.addListener(_defaultPropertyChangeListener);
         }

         /*
          * Hidden marker color
          */
         {
            // Label
            final Label label = new Label(container, SWT.NONE);
            GridDataFactory
                  .fillDefaults()//
                  .align(SWT.FILL, SWT.CENTER)
                  .applyTo(label);
            label.setText(Messages.Slideout_ChartMarkerOptions_Label_HiddenMarkerColor);
            label.setToolTipText(Messages.Slideout_ChartMarkerOptions_Label_HiddenMarkerColor_Tooltip);

            // Color selector
            _colorHiddenMarker = new ColorSelectorExtended(container);
            GridDataFactory
                  .swtDefaults()//
                  .grab(false, true)
                  .align(SWT.BEGINNING, SWT.BEGINNING)
                  .applyTo(_colorHiddenMarker.getButton());

            _colorHiddenMarker.addOpenListener(this);
            _colorHiddenMarker.addListener(_defaultPropertyChangeListener);
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
      _chkMarkerTooltip_Elevation.setEnabled(isTooltipVisible);
      _chkMarkerTooltip_Distance.setEnabled(isTooltipVisible);
      _chkMarkerTooltip_Duration.setEnabled(isTooltipVisible);
      _chkMarkerTooltip_ElevationGainDifference.setEnabled(isTooltipVisible);
      _chkMarkerTooltip_DistanceDifference.setEnabled(isTooltipVisible);
      _chkMarkerTooltip_DurationDifference.setEnabled(isTooltipVisible);

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

      final TourChartConfiguration tcc = _tourChart.getTourChartConfig();

      final boolean isDrawMarkerWithDefaultColor = _chkDrawMarkerWithDefaultColor.getSelection();
      final boolean isShowAbsoluteValues = _chkShowAbsoluteValues.getSelection();
      final boolean isShowHiddenMarker = _chkShowHiddenMarker.getSelection();
      final boolean isShowLabelTempPos = _chkShowLabelTempPosition.getSelection();
      final boolean isShowMarkerLabel = _chkShowMarkerLabel.getSelection();
      final boolean isShowMarkerPoint = _chkShowMarkerPoint.getSelection();
      final boolean isShowMarkerTooltip = _chkShowMarkerTooltip.getSelection();
      final boolean isShowMarkerTooltip_Elevation = _chkMarkerTooltip_Elevation.getSelection();
      final boolean isShowMarkerTooltip_Distance = _chkMarkerTooltip_Distance.getSelection();
      final boolean isShowMarkerTooltip_Duration = _chkMarkerTooltip_Duration.getSelection();
      final boolean isShowMarkerTooltip_ElevationGainDifference = _chkMarkerTooltip_ElevationGainDifference.getSelection();
      final boolean isShowMarkerTooltip_DistanceDifference = _chkMarkerTooltip_DistanceDifference.getSelection();
      final boolean isShowMarkerTooltip_DurationDifference = _chkMarkerTooltip_DurationDifference.getSelection();
      final boolean isShowOnlyWithDescription = _chkShowOnlyWithDescription.getSelection();

      final int hoverSize = _spinHoverSize.getSelection();
      final int labelOffset = _spinLabelOffset.getSelection();
      final int markerPointSize = _spinMarkerPointSize.getSelection();
      final int tempPosition = _comboLabelTempPosition.getSelectionIndex();
      final int ttPosition = _comboTooltipPosition.getSelectionIndex();

      final RGB defaultColor = _colorDefaultMarker.getColorValue();
      final RGB deviceColor = _colorDeviceMarker.getColorValue();
      final RGB hiddenColor = _colorHiddenMarker.getColorValue();

      /*
       * Update pref store
       */
      _prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_HOVER_SIZE, hoverSize);
      _prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_LABEL_OFFSET, labelOffset);
      _prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_LABEL_TEMP_POSITION, tempPosition);
      _prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_POINT_SIZE, markerPointSize);
      _prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_TOOLTIP_POSITION, ttPosition);

      _prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_IS_DRAW_WITH_DEFAULT_COLOR, isDrawMarkerWithDefaultColor);
      _prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_ABSOLUTE_VALUES, isShowAbsoluteValues);
      _prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_HIDDEN_MARKER, isShowHiddenMarker);
      _prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_LABEL_TEMP_POSITION, isShowLabelTempPos);
      _prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_MARKER_LABEL, isShowMarkerLabel);
      _prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_MARKER_POINT, isShowMarkerPoint);
      _prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_MARKER_TOOLTIP, isShowMarkerTooltip);
      _prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_MARKER_TOOLTIP_ELEVATION, isShowMarkerTooltip_Elevation);
      _prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_MARKER_TOOLTIP_DISTANCE, isShowMarkerTooltip_Distance);
      _prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_MARKER_TOOLTIP_DURATION, isShowMarkerTooltip_Duration);
      _prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_MARKER_TOOLTIP_ELEVATIONGAIN_DIFFERENCE,
            isShowMarkerTooltip_ElevationGainDifference);
      _prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_MARKER_TOOLTIP_DISTANCE_DIFFERENCE, isShowMarkerTooltip_DistanceDifference);
      _prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_MARKER_TOOLTIP_DURATION_DIFFERENCE, isShowMarkerTooltip_DurationDifference);
      _prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_ONLY_WITH_DESCRIPTION, isShowOnlyWithDescription);

      PreferenceConverter.setValue(_prefStore, ITourbookPreferences.GRAPH_MARKER_COLOR_DEFAULT, defaultColor);
      PreferenceConverter.setValue(_prefStore, ITourbookPreferences.GRAPH_MARKER_COLOR_DEVICE, deviceColor);
      PreferenceConverter.setValue(_prefStore, ITourbookPreferences.GRAPH_MARKER_COLOR_HIDDEN, hiddenColor);

      /*
       * Update chart config
       */
      tcc.isDrawMarkerWithDefaultColor = isDrawMarkerWithDefaultColor;
      tcc.isShowAbsoluteValues = isShowAbsoluteValues;
      tcc.isShowHiddenMarker = isShowHiddenMarker;
      tcc.isShowLabelTempPos = isShowLabelTempPos;
      tcc.isShowMarkerLabel = isShowMarkerLabel;
      tcc.isShowMarkerPoint = isShowMarkerPoint;
      tcc.isShowMarkerTooltip = isShowMarkerTooltip;
      tcc.isShowMarkerTooltip_Elevation = isShowMarkerTooltip_Elevation;
      tcc.isShowMarkerTooltip_Distance = isShowMarkerTooltip_Distance;
      tcc.isShowMarkerTooltip_Duration = isShowMarkerTooltip_Duration;
      tcc.isShowMarkerTooltip_ElevationGainDifference = isShowMarkerTooltip_ElevationGainDifference;
      tcc.isShowMarkerTooltip_DistanceDifference = isShowMarkerTooltip_DistanceDifference;
      tcc.isShowMarkerTooltip_DurationDifference = isShowMarkerTooltip_DurationDifference;
      tcc.isShowOnlyWithDescription = isShowOnlyWithDescription;

      tcc.markerHoverSize = hoverSize;
      tcc.markerLabelOffset = labelOffset;
      tcc.markerLabelTempPos = tempPosition;
      tcc.markerPointSize = markerPointSize;
      tcc.markerTooltipPosition = ttPosition;

      tcc.markerColorDefault = defaultColor;
      tcc.markerColorDevice = deviceColor;
      tcc.markerColorHidden = hiddenColor;

      // update chart with new settings
      _tourChart.updateUI_MarkerLayer();

      enableControls();

      // notify pref listener
      TourbookPlugin.getPrefStore().setValue(ITourbookPreferences.GRAPH_MARKER_IS_MODIFIED, Math.random());
   }

   private void resetToDefaults() {

// SET_FORMATTING_OFF
		/*
		 * Update UI with defaults from pref store
		 */

		_chkDrawMarkerWithDefaultColor.setSelection(	_prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_MARKER_IS_DRAW_WITH_DEFAULT_COLOR));
		_chkShowAbsoluteValues.setSelection(			_prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_ABSOLUTE_VALUES));
		_chkShowHiddenMarker.setSelection(				_prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_HIDDEN_MARKER));
		_chkShowLabelTempPosition.setSelection(		_prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_LABEL_TEMP_POSITION));
		_chkShowMarkerLabel.setSelection(				_prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_MARKER_LABEL));
		_chkShowMarkerPoint.setSelection(				_prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_MARKER_POINT));
		_chkShowMarkerTooltip.setSelection(				_prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_MARKER_TOOLTIP));
		_chkMarkerTooltip_Elevation.setSelection(				_prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_MARKER_TOOLTIP_ELEVATION));
		_chkMarkerTooltip_Distance.setSelection(				_prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_MARKER_TOOLTIP_DISTANCE));
		_chkMarkerTooltip_Duration.setSelection(				_prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_MARKER_TOOLTIP_DURATION));
		_chkMarkerTooltip_ElevationGainDifference	.setSelection(				_prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_MARKER_TOOLTIP_ELEVATIONGAIN_DIFFERENCE));
		_chkMarkerTooltip_DistanceDifference.setSelection(				_prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_MARKER_TOOLTIP_DISTANCE_DIFFERENCE));
		_chkMarkerTooltip_DurationDifference	.setSelection(				_prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_MARKER_TOOLTIP_DURATION_DIFFERENCE));
		_chkShowOnlyWithDescription.setSelection(		_prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_ONLY_WITH_DESCRIPTION));

		_comboLabelTempPosition.select(					_prefStore.getDefaultInt(ITourbookPreferences.GRAPH_MARKER_LABEL_TEMP_POSITION));
		_comboTooltipPosition.select(						_prefStore.getDefaultInt(ITourbookPreferences.GRAPH_MARKER_TOOLTIP_POSITION));

		_spinHoverSize.setSelection(						_prefStore.getDefaultInt(ITourbookPreferences.GRAPH_MARKER_HOVER_SIZE));
		_spinMarkerPointSize.setSelection(				_prefStore.getDefaultInt(ITourbookPreferences.GRAPH_MARKER_POINT_SIZE));
		_spinLabelOffset.setSelection(					_prefStore.getDefaultInt(ITourbookPreferences.GRAPH_MARKER_LABEL_OFFSET));

		_colorDefaultMarker.setColorValue(	PreferenceConverter.getColor(_prefStore, 			ITourbookPreferences.GRAPH_MARKER_COLOR_DEFAULT));
		_colorDeviceMarker.setColorValue(	PreferenceConverter.getDefaultColor(_prefStore, ITourbookPreferences.GRAPH_MARKER_COLOR_DEVICE));
		_colorHiddenMarker.setColorValue(	PreferenceConverter.getDefaultColor(_prefStore, ITourbookPreferences.GRAPH_MARKER_COLOR_HIDDEN));

// SET_FORMATTING_ON

      onChangeUI();
   }

   private void restoreState() {

      final TourChartConfiguration tcc = _tourChart.getTourChartConfig();

      final int markerTooltipPosition = tcc.markerTooltipPosition < 0
            ? ChartMarkerToolTip.DEFAULT_TOOLTIP_POSITION
            : tcc.markerTooltipPosition;

      _chkDrawMarkerWithDefaultColor.setSelection(tcc.isDrawMarkerWithDefaultColor);
      _chkShowAbsoluteValues.setSelection(tcc.isShowAbsoluteValues);

      _chkShowHiddenMarker.setSelection(tcc.isShowHiddenMarker);
      _chkShowLabelTempPosition.setSelection(tcc.isShowLabelTempPos);
      _chkShowMarkerLabel.setSelection(tcc.isShowMarkerLabel);
      _chkShowMarkerPoint.setSelection(tcc.isShowMarkerPoint);
      _chkShowMarkerTooltip.setSelection(tcc.isShowMarkerTooltip);
      _chkMarkerTooltip_Elevation.setSelection(tcc.isShowMarkerTooltip_Elevation);
      _chkMarkerTooltip_Distance.setSelection(tcc.isShowMarkerTooltip_Distance);
      _chkMarkerTooltip_Duration.setSelection(tcc.isShowMarkerTooltip_Duration);
      _chkMarkerTooltip_ElevationGainDifference.setSelection(tcc.isShowMarkerTooltip_ElevationGainDifference);
      _chkMarkerTooltip_DistanceDifference.setSelection(tcc.isShowMarkerTooltip_DistanceDifference);
      _chkMarkerTooltip_DurationDifference.setSelection(tcc.isShowMarkerTooltip_DurationDifference);
      _chkShowOnlyWithDescription.setSelection(tcc.isShowOnlyWithDescription);

      _comboLabelTempPosition.select(tcc.markerLabelTempPos);
      _comboTooltipPosition.select(markerTooltipPosition);

      _colorDefaultMarker.setColorValue(tcc.markerColorDefault);
      _colorDeviceMarker.setColorValue(tcc.markerColorDevice);
      _colorHiddenMarker.setColorValue(tcc.markerColorHidden);

      _spinHoverSize.setSelection(tcc.markerHoverSize);
      _spinLabelOffset.setSelection(tcc.markerLabelOffset);
      _spinMarkerPointSize.setSelection(tcc.markerPointSize);
   }

}
