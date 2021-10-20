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
package net.tourbook.ui.views.geoCompare;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionResetToDefaults;
import net.tourbook.common.action.IActionResetToDefault;
import net.tourbook.common.color.ColorSelectorExtended;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.formatter.FormatManager;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.MtMath;
import net.tourbook.common.util.Util;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPage_Map2_Appearance;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Slideout for the tour tag filter
 */
public class SlideoutGeoCompareOptions extends ToolbarSlideout implements IColorSelectorListener, IActionResetToDefault {

   private static final String      VALUE_FORMAT_1_0              = "%1.0f %s";                   //$NON-NLS-1$
   private static final String      VALUE_FORMAT_1_1              = "%1.1f %s";                   //$NON-NLS-1$
   private static final String      VALUE_FORMAT_1_2              = "%1.2f %s";                   //$NON-NLS-1$

   final static IPreferenceStore    _prefStore                    = TourbookPlugin.getPrefStore();
   private static IDialogSettings   _state;

   private SelectionAdapter         _compareSelectionListener;
   private MouseWheelListener       _compareMouseWheelListener;
   private MouseWheelListener       _mapOptions_MouseWheelListener;
   private IPropertyChangeListener  _mapOptions_PropertyListener;
   private SelectionAdapter         _mapOptions_SelectionListener;

   private ActionResetToDefaults    _actionRestoreDefaults;

   private GeoCompareView           _geoCompareView;

   private int                      _geoAccuracy;

   /**
    * contains the controls which are displayed in the first column, these controls are used to get
    * the maximum width and set the first column within the different section to the same width
    */
   private final ArrayList<Control> _firstColumnContainerControls = new ArrayList<>();
   private final ArrayList<Control> _firstColumnControls          = new ArrayList<>();
   private final ArrayList<Control> _secondColumnControls         = new ArrayList<>();

   /*
    * UI controls
    */
   private Composite             _parent;

   private Button                _chkGeo_RelativeDifferences_Filter;
   private Button                _chkMapOption_TrackOpacity;

   private ColorSelectorExtended _colorMapOption_RefTour;
   private ColorSelectorExtended _colorMapOption_ComparedTourPart;

   private Label                 _lblGeo_Accuracy;
   private Label                 _lblGeo_Accuracy_Value;
   private Label                 _lblGeo_DistanceInterval;
   private Label                 _lblGeo_DistanceInterval_Unit;
   private Label                 _lblGeo_NormalizedDistance;
   private Label                 _lblGeo_NormalizedDistance_Unit;
   private Label                 _lblGeo_NormalizedDistance_Value;
   private Label                 _lblGeo_RelativeDifferences_Filter_Value;
   private Label                 _lblMapOption_ComparedTourPart;
   private Label                 _lblMapOption_LineWidth;
   private Label                 _lblMapOption_RefTour;

   private Spinner               _spinnerGeo_Accuracy;
   private Spinner               _spinnerGeo_DistanceInterval;
   private Spinner               _spinnerGeo_RelativeDifferences_Filter;
   private Spinner               _spinnerMapOption_LineWidth;
   private Spinner               _spinnerMapOption_TrackOpacity;

   /**
    * @param ownerControl
    * @param toolbar
    * @param state
    * @param geoCompareView
    */
   public SlideoutGeoCompareOptions(final Composite ownerControl,
                                    final ToolBar toolbar,
                                    final IDialogSettings state,
                                    final GeoCompareView geoCompareView) {

      super(ownerControl, toolbar);

      _state = state;
      _geoCompareView = geoCompareView;
   }

   @Override
   public void colorDialogOpened(final boolean isAnotherDialogOpened) {

      setIsAnotherDialogOpened(isAnotherDialogOpened);
   }

   private void createActions() {

      _actionRestoreDefaults = new ActionResetToDefaults(this);
   }

   @Override
   protected Composite createToolTipContentArea(final Composite parent) {

      _parent = parent;

      initUI(parent);

      createActions();

      final Composite ui = createUI(parent);

      restoreState();
      updateUI_GeoAccuracy();
      updateUI_StateValues(_geoCompareView.getSlideoutState());

      enableControls();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.swtDefaults().applyTo(shellContainer);
      {
         final Composite container = new Composite(shellContainer, SWT.NONE);
         GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
         {
            createUI_10_Header(container);
            createUI_20_CompareOptions(container);
         }

//         // compute width for all controls and equalize column width for the different sections
//         container.layout(true, true);
//         UI.setEqualizeColumWidths(_firstColumnControls, 5);
//         UI.setEqualizeColumWidths(_secondColumnControls);
//
//         container.layout(true, true);
//         UI.setEqualizeColumWidths(_firstColumnContainerControls);
      }

      return shellContainer;
   }

   private void createUI_10_Header(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//         container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
         {
            /*
             * Slideout title
             */
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Slideout_GeoCompareOptions_Label_Title);
            MTFont.setBannerFont(label);
            GridDataFactory.fillDefaults()
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(label);
         }
         {
            /*
             * Actionbar
             */
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

   private void createUI_20_CompareOptions(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      {
         createUI_40_Options(container);
         createUI_50_MapOptions(container);
      }
   }

   private void createUI_40_Options(final Composite parent) {

      final GridDataFactory gridData_Grab_FillCenter = GridDataFactory
            .fillDefaults()
            .grab(true, false)
            .align(SWT.FILL, SWT.CENTER);

      final GridDataFactory gridData_EndFill = GridDataFactory
            .fillDefaults()
            .align(SWT.END, SWT.FILL);

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);

      _firstColumnContainerControls.add(container);

      {
         {
            /*
             * Distance
             */
            {
               _lblGeo_NormalizedDistance = new Label(container, SWT.NONE);
               _lblGeo_NormalizedDistance.setText(Messages.Slideout_GeoCompareOptions_Label_NormalizedDistance);

               _firstColumnControls.add(_lblGeo_NormalizedDistance);
            }
            {
               _lblGeo_NormalizedDistance_Value = new Label(container, SWT.TRAIL);
               _lblGeo_NormalizedDistance_Value.setText(UI.EMPTY_STRING);
               gridData_EndFill.applyTo(_lblGeo_NormalizedDistance_Value);

               _secondColumnControls.add(_lblGeo_NormalizedDistance_Value);
            }
            {
               // Label: Distance unit
               _lblGeo_NormalizedDistance_Unit = new Label(container, SWT.NONE);
               _lblGeo_NormalizedDistance_Unit.setText(UI.EMPTY_STRING);
               GridDataFactory
                     .fillDefaults()
                     .grab(true, false)
                     .applyTo(_lblGeo_NormalizedDistance_Unit);
            }
         }
         {
            /*
             * Distance interval
             */
            {
               // Label
               _lblGeo_DistanceInterval = new Label(container, SWT.NONE);
               _lblGeo_DistanceInterval.setText(Messages.Slideout_GeoCompareOptions_Label_DistanceInterval);

               _firstColumnControls.add(_lblGeo_DistanceInterval);
            }
            {
               // Spinner
               _spinnerGeo_DistanceInterval = new Spinner(container, SWT.BORDER);
               _spinnerGeo_DistanceInterval.setMinimum(10);
               _spinnerGeo_DistanceInterval.setMaximum(1_000);
               _spinnerGeo_DistanceInterval.setPageIncrement(10);
               _spinnerGeo_DistanceInterval.addSelectionListener(_compareSelectionListener);
               _spinnerGeo_DistanceInterval.addMouseWheelListener(_compareMouseWheelListener);
               gridData_EndFill.applyTo(_spinnerGeo_DistanceInterval);

               _secondColumnControls.add(_spinnerGeo_DistanceInterval);
            }
            {
               // Label: Distance unit
               _lblGeo_DistanceInterval_Unit = new Label(container, SWT.NONE);
               _lblGeo_DistanceInterval_Unit.setText(UI.UNIT_LABEL_DISTANCE_M_OR_YD);
               gridData_Grab_FillCenter.applyTo(_lblGeo_DistanceInterval_Unit);
            }
         }
         {
            /*
             * Normalized geo data factor
             */
            {
               // Label
               _lblGeo_Accuracy = new Label(container, SWT.NONE);
               _lblGeo_Accuracy.setText(Messages.Slideout_GeoCompareOptions_Label_GeoAccuracy);

               _firstColumnControls.add(_lblGeo_Accuracy);
            }
            {
               // Spinner
               _spinnerGeo_Accuracy = new Spinner(container, SWT.BORDER);
               _spinnerGeo_Accuracy.setMinimum(100);
               _spinnerGeo_Accuracy.setMaximum(100_000);
               _spinnerGeo_Accuracy.setPageIncrement(100);
               _spinnerGeo_Accuracy.addSelectionListener(_compareSelectionListener);
               _spinnerGeo_Accuracy.addMouseWheelListener(_compareMouseWheelListener);

               _secondColumnControls.add(_spinnerGeo_Accuracy);
            }
            {
               // geo distance
               _lblGeo_Accuracy_Value = new Label(container, SWT.NONE);
               gridData_Grab_FillCenter.applyTo(_lblGeo_Accuracy_Value);
            }
         }
         {
            /*
             * Relative geographic differences filter
             */
            {
               // Checkbox
               _chkGeo_RelativeDifferences_Filter = new Button(container, SWT.CHECK);
               _chkGeo_RelativeDifferences_Filter.setText(Messages.Slideout_GeoCompareOptions_Label_GeoRelativeDifferences_Filter);
               _chkGeo_RelativeDifferences_Filter.setToolTipText(Messages.Slideout_GeoCompareOptions_Label_GeoRelativeDifferences_Filter_Tooltip);
               _chkGeo_RelativeDifferences_Filter.addSelectionListener(_compareSelectionListener);

               _firstColumnControls.add(_chkGeo_RelativeDifferences_Filter);
            }
            {
               // Spinner
               _spinnerGeo_RelativeDifferences_Filter = new Spinner(container, SWT.BORDER);
               _spinnerGeo_RelativeDifferences_Filter.setMinimum(1);
               _spinnerGeo_RelativeDifferences_Filter.setMaximum(100);
               _spinnerGeo_RelativeDifferences_Filter.setPageIncrement(10);
               _spinnerGeo_RelativeDifferences_Filter.addSelectionListener(_compareSelectionListener);
               _spinnerGeo_RelativeDifferences_Filter.addMouseWheelListener(_compareMouseWheelListener);
               gridData_EndFill.applyTo(_spinnerGeo_RelativeDifferences_Filter);

               _secondColumnControls.add(_spinnerGeo_RelativeDifferences_Filter);
            }
            {
               // geo differences percentage value
               _lblGeo_RelativeDifferences_Filter_Value = new Label(container, SWT.NONE);
               _lblGeo_RelativeDifferences_Filter_Value.setText(UI.UNIT_PERCENT);
               gridData_Grab_FillCenter.applyTo(_lblGeo_RelativeDifferences_Filter_Value);
            }
         }
      }
   }

   private void createUI_50_MapOptions(final Composite parent) {

      final Group group = new Group(parent, SWT.NONE);
      group.setText(Messages.Slideout_GeoCompareOptions_Group_MapOptions);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);

      _firstColumnContainerControls.add(group);

      {
         {
            /*
             * Ref tour line width
             */
            {
               // label
               _lblMapOption_LineWidth = new Label(group, SWT.NONE);
               _lblMapOption_LineWidth.setText(Messages.Slideout_GeoCompareOptions_Label_LineWidth);

               _firstColumnControls.add(_lblMapOption_LineWidth);
            }
            {
               // spinner
               _spinnerMapOption_LineWidth = new Spinner(group, SWT.BORDER);
               _spinnerMapOption_LineWidth.setMinimum(1);
               _spinnerMapOption_LineWidth.setMaximum(100);
               _spinnerMapOption_LineWidth.setPageIncrement(5);
               _spinnerMapOption_LineWidth.addSelectionListener(_mapOptions_SelectionListener);
               _spinnerMapOption_LineWidth.addMouseWheelListener(_mapOptions_MouseWheelListener);

               _secondColumnControls.add(_spinnerMapOption_LineWidth);
            }
         }
         {
            /*
             * Ref tour in map
             */
            {
               // label
               _lblMapOption_RefTour = new Label(group, SWT.NONE);
               _lblMapOption_RefTour.setText(Messages.Slideout_GeoCompareOptions_Label_ReferenceTour);

               _firstColumnControls.add(_lblMapOption_RefTour);
            }
            {
               // color
               _colorMapOption_RefTour = new ColorSelectorExtended(group);
               _colorMapOption_RefTour.addListener(_mapOptions_PropertyListener);
               _colorMapOption_RefTour.addOpenListener(this);
            }
         }
         {
            /*
             * Compared tour part in map
             */
            {
               // label
               _lblMapOption_ComparedTourPart = new Label(group, SWT.NONE);
               _lblMapOption_ComparedTourPart.setText(Messages.Slideout_GeoCompareOptions_Label_ComparedTourPart);

               _firstColumnControls.add(_lblMapOption_ComparedTourPart);
            }
            {
               // color
               _colorMapOption_ComparedTourPart = new ColorSelectorExtended(group);
               _colorMapOption_ComparedTourPart.addListener(_mapOptions_PropertyListener);
               _colorMapOption_ComparedTourPart.addOpenListener(this);
            }
         }
         {
            /*
             * Tour track opacity
             */
            {
               // checkbox
               _chkMapOption_TrackOpacity = new Button(group, SWT.CHECK);
               _chkMapOption_TrackOpacity.setText(Messages.Slideout_Map_Options_Checkbox_TrackOpacity);
               _chkMapOption_TrackOpacity.setToolTipText(Messages.Slideout_Map_Options_Checkbox_TrackOpacity_Tooltip);
               _chkMapOption_TrackOpacity.addSelectionListener(_mapOptions_SelectionListener);

               _firstColumnControls.add(_chkMapOption_TrackOpacity);
            }
            {
               // spinner
               _spinnerMapOption_TrackOpacity = new Spinner(group, SWT.BORDER);
               _spinnerMapOption_TrackOpacity.setMinimum(PrefPage_Map2_Appearance.MAP_OPACITY_MINIMUM);
               _spinnerMapOption_TrackOpacity.setMaximum(100);
               _spinnerMapOption_TrackOpacity.setIncrement(1);
               _spinnerMapOption_TrackOpacity.setPageIncrement(10);
               _spinnerMapOption_TrackOpacity.addSelectionListener(_mapOptions_SelectionListener);
               _spinnerMapOption_TrackOpacity.addMouseWheelListener(_mapOptions_MouseWheelListener);
            }
         }
      }
   }

   private void enableControls() {

      final boolean isGeoCompareActive = GeoCompareManager.isGeoComparing();

      final boolean isTrackOpacity = _chkMapOption_TrackOpacity.getSelection();
      final boolean isGeoFilter = _chkGeo_RelativeDifferences_Filter.getSelection();

      _spinnerMapOption_TrackOpacity.setEnabled(isTrackOpacity);

      _chkGeo_RelativeDifferences_Filter.setEnabled(isGeoCompareActive);

      _lblGeo_DistanceInterval.setEnabled(isGeoCompareActive);
      _lblGeo_DistanceInterval_Unit.setEnabled(isGeoCompareActive);
      _lblGeo_Accuracy.setEnabled(isGeoCompareActive);
      _lblGeo_Accuracy_Value.setEnabled(isGeoCompareActive);
      _lblGeo_RelativeDifferences_Filter_Value.setEnabled(isGeoCompareActive);
      _lblGeo_NormalizedDistance.setEnabled(isGeoCompareActive);
      _lblGeo_NormalizedDistance_Value.setEnabled(isGeoCompareActive);
      _lblGeo_NormalizedDistance_Unit.setEnabled(isGeoCompareActive);

      _spinnerGeo_DistanceInterval.setEnabled(isGeoCompareActive);
      _spinnerGeo_Accuracy.setEnabled(isGeoCompareActive);
      _spinnerGeo_RelativeDifferences_Filter.setEnabled(isGeoCompareActive && isGeoFilter);
   }

   private void initUI(final Composite parent) {

      parent.addDisposeListener(new DisposeListener() {
         @Override
         public void widgetDisposed(final DisposeEvent e) {
            onDisposeSlideout();
         }
      });

      _compareSelectionListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            onChange_CompareParameter();
         }
      };

      _compareMouseWheelListener = new MouseWheelListener() {
         @Override
         public void mouseScrolled(final MouseEvent event) {
            UI.adjustSpinnerValueOnMouseScroll(event);
            onChange_CompareParameter();
         }
      };

      _mapOptions_SelectionListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            onChange_MapOptions();
         }
      };

      _mapOptions_MouseWheelListener = new MouseWheelListener() {
         @Override
         public void mouseScrolled(final MouseEvent event) {
            UI.adjustSpinnerValueOnMouseScroll(event);
            onChange_MapOptions();
         }
      };

      _mapOptions_PropertyListener = new IPropertyChangeListener() {
         @Override
         public void propertyChange(final PropertyChangeEvent event) {
            onChange_MapOptions();
         }
      };
   }

   private boolean isUICreated() {

      return _parent == null || _parent.isDisposed() ? false : true;
   }

   private void onChange_CompareParameter() {

      saveState_Slideout();

      updateUI_GeoAccuracy();

      _geoCompareView.onChange_CompareParameter();

      enableControls();
   }

   private void onChange_MapOptions() {

      saveState_MapOption();

      // force repainting
      TourbookPlugin.getPrefStore().setValue(ITourbookPreferences.GRAPH_COLORS_HAS_CHANGED, Math.random());

      enableControls();
   }

   private void onDisposeSlideout() {

      saveState_Slideout();

      _firstColumnControls.clear();
      _secondColumnControls.clear();
   }

   @Override
   public void resetToDefaults() {

// SET_FORMATTING_OFF

      _spinnerGeo_DistanceInterval                    .setSelection(GeoCompareView.DEFAULT_DISTANCE_INTERVAL);
      _spinnerGeo_Accuracy                            .setSelection(GeoCompareView.DEFAULT_GEO_ACCURACY);

      _chkGeo_RelativeDifferences_Filter              .setSelection(GeoCompareView.DEFAULT_IS_GEO_RELATIVE_DIFFERENCES_FILTER);
      _spinnerGeo_RelativeDifferences_Filter          .setSelection(GeoCompareView.DEFAULT_GEO_RELATIVE_DIFFERENCES_FILTER);

      /*
       * Map options
       */
      _chkMapOption_TrackOpacity.setSelection(        _prefStore.getDefaultBoolean(ITourbookPreferences.MAP2_LAYOUT_IS_TOUR_TRACK_OPACITY));
      _spinnerMapOption_TrackOpacity.setSelection(    _prefStore.getDefaultInt(ITourbookPreferences.MAP2_LAYOUT_TOUR_TRACK_OPACITY));
      _spinnerMapOption_LineWidth.setSelection(       _prefStore.getDefaultInt(ITourbookPreferences.GEO_COMPARE_REF_TOUR_LINE_WIDTH));

      _colorMapOption_RefTour.setColorValue(          PreferenceConverter.getDefaultColor(_prefStore,ITourbookPreferences.GEO_COMPARE_REF_TOUR_RGB));
      _colorMapOption_ComparedTourPart.setColorValue( PreferenceConverter.getDefaultColor(_prefStore,ITourbookPreferences.GEO_COMPARE_COMPARED_TOUR_PART_RGB));

// SET_FORMATTING_ON

      onChange_CompareParameter();
      onChange_MapOptions();
   }

   void restoreState() {

      // check if UI is created
      if (isUICreated() == false) {
         return;
      }

// SET_FORMATTING_OFF

      _geoAccuracy = Util.getStateInt(_state, GeoCompareView.STATE_GEO_ACCURACY, GeoCompareView.DEFAULT_GEO_ACCURACY);

      _spinnerGeo_Accuracy                   .setSelection(_geoAccuracy);
      _spinnerGeo_DistanceInterval           .setSelection(Util.getStateInt(_state,       GeoCompareView.STATE_DISTANCE_INTERVAL,                  GeoCompareView.DEFAULT_DISTANCE_INTERVAL));

      _chkGeo_RelativeDifferences_Filter     .setSelection(Util.getStateBoolean(_state,   GeoCompareView.STATE_IS_GEO_RELATIVE_DIFFERENCES_FILTER, GeoCompareView.DEFAULT_IS_GEO_RELATIVE_DIFFERENCES_FILTER));
      _spinnerGeo_RelativeDifferences_Filter .setSelection(Util.getStateInt(_state,       GeoCompareView.STATE_GEO_RELATIVE_DIFFERENCES_FILTER,    GeoCompareView.DEFAULT_GEO_RELATIVE_DIFFERENCES_FILTER));

      /*
       * Map options
       */
      _spinnerMapOption_LineWidth         .setSelection(_prefStore.getInt(ITourbookPreferences.GEO_COMPARE_REF_TOUR_LINE_WIDTH));

      _colorMapOption_ComparedTourPart    .setColorValue(PreferenceConverter.getColor(_prefStore, ITourbookPreferences.GEO_COMPARE_COMPARED_TOUR_PART_RGB));
      _colorMapOption_RefTour             .setColorValue(PreferenceConverter.getColor(_prefStore, ITourbookPreferences.GEO_COMPARE_REF_TOUR_RGB));

      _chkMapOption_TrackOpacity          .setSelection(_prefStore.getBoolean(ITourbookPreferences.MAP2_LAYOUT_IS_TOUR_TRACK_OPACITY));
      _spinnerMapOption_TrackOpacity      .setSelection(_prefStore.getInt(ITourbookPreferences.MAP2_LAYOUT_TOUR_TRACK_OPACITY));

// SET_FORMATTING_ON
   }

   private void saveState_MapOption() {

// SET_FORMATTING_OFF

      _prefStore.setValue(ITourbookPreferences.GEO_COMPARE_REF_TOUR_LINE_WIDTH,     _spinnerMapOption_LineWidth.getSelection());
      _prefStore.setValue(ITourbookPreferences.MAP2_LAYOUT_IS_TOUR_TRACK_OPACITY,   _chkMapOption_TrackOpacity.getSelection());
      _prefStore.setValue(ITourbookPreferences.MAP2_LAYOUT_TOUR_TRACK_OPACITY,      _spinnerMapOption_TrackOpacity.getSelection());

      PreferenceConverter.setValue(_prefStore,   ITourbookPreferences.GEO_COMPARE_COMPARED_TOUR_PART_RGB,   _colorMapOption_ComparedTourPart.getColorValue());
      PreferenceConverter.setValue(_prefStore,   ITourbookPreferences.GEO_COMPARE_REF_TOUR_RGB,             _colorMapOption_RefTour.getColorValue());

// SET_FORMATTING_ON
   }

   private void saveState_Slideout() {

      _geoAccuracy = _spinnerGeo_Accuracy.getSelection();

// SET_FORMATTING_OFF

      _state.put(GeoCompareView.STATE_GEO_ACCURACY,                        _geoAccuracy);
      _state.put(GeoCompareView.STATE_DISTANCE_INTERVAL,                   _spinnerGeo_DistanceInterval.getSelection());

      _state.put(GeoCompareView.STATE_IS_GEO_RELATIVE_DIFFERENCES_FILTER,  _chkGeo_RelativeDifferences_Filter.getSelection());
      _state.put(GeoCompareView.STATE_GEO_RELATIVE_DIFFERENCES_FILTER,     _spinnerGeo_RelativeDifferences_Filter.getSelection());

// SET_FORMATTING_ON
   }

   private void updateUI_GeoAccuracy() {

      final double latStart = 0;
      final double latEnd = 1.0 / _geoAccuracy;

      final double lonStart = 0;
      final double lonEnd = 1.0 / _geoAccuracy;

      final double distDiff = MtMath.distanceVincenty(latStart, lonStart, latEnd, lonEnd);

      final double distValue = distDiff / UI.UNIT_VALUE_DISTANCE_SMALL;

      final String valueFormatting = distValue > 100
            ? VALUE_FORMAT_1_0
            : distValue > 10
                  ? VALUE_FORMAT_1_1
                  : VALUE_FORMAT_1_2;

      final String geoDistance = String.format(valueFormatting, distValue, UI.UNIT_LABEL_DISTANCE_M_OR_YD);

      _lblGeo_Accuracy_Value.setText(geoDistance);
   }

   void updateUI_StateValues(final GeoCompareState slideoutState) {

      if (isUICreated() == false) {
         return;
      }

      if (slideoutState == null) {
         return;
      }

      if (slideoutState.isReset) {

         _lblGeo_NormalizedDistance_Value.setText(UI.EMPTY_STRING);
         _lblGeo_NormalizedDistance_Unit.setText(UI.EMPTY_STRING);

      } else {

         final float distance = slideoutState.normalizedDistance / UI.UNIT_VALUE_DISTANCE;

         _lblGeo_NormalizedDistance_Value.setText(FormatManager.formatDistance(distance / 1000.0));
         _lblGeo_NormalizedDistance_Unit.setText(UI.UNIT_LABEL_DISTANCE);
      }

      enableControls();
   }

}
