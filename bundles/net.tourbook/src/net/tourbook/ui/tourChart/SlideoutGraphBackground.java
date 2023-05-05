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

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.OtherMessages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.action.ActionResetToDefaults;
import net.tourbook.common.action.IActionResetToDefault;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.data.TourPerson;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageAppearanceColors;
import net.tourbook.preferences.PrefPageAppearanceTourChart;
import net.tourbook.preferences.PrefPagePeople;
import net.tourbook.preferences.PrefPagePeopleData;
import net.tourbook.preferences.PrefPage_Appearance_Swimming;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Tour chart properties slideout
 */
public class SlideoutGraphBackground extends ToolbarSlideout implements IActionResetToDefault {

   private static final IPreferenceStore _prefStore                   = TourbookPlugin.getPrefStore();

   /**
    * Contains all {@link GraphBgSourceType}s which can be displayed for the current tour
    */
   private ArrayList<GraphBgSourceType>  _availableGraphBgSourceTypes = new ArrayList<>();

   private ActionResetToDefaults         _actionRestoreDefaults;
   private ActionOpenPrefDialog          _actionPrefDialog;

   private SelectionListener             _defaultSelectionListener;
   private MouseWheelListener            _defaultMouseWheelListener;
   private FocusListener                 _keepOpenListener;

   /*
    * UI controls
    */
   private TourChart _tourChart;

   private Combo     _comboGraphBgStyle;
   private Combo     _comboGraphBgSource;

   private Label     _lblGraphBgStyle;

   private Spinner   _spinnerGraphFillingTransparency_Dark;
   private Spinner   _spinnerGraphFillingTransparency_Light;

   public SlideoutGraphBackground(final Control ownerControl,
                                  final ToolBar toolBar,
                                  final TourChart tourChart) {

      super(ownerControl, toolBar);

      _tourChart = tourChart;
   }

   private void createActions() {

      _actionRestoreDefaults = new ActionResetToDefaults(this);

      _actionPrefDialog = new ActionOpenPrefDialog(
            Messages.Slideout_TourChartGraphBackground_Action_Colors_Tooltip,
            PrefPageAppearanceTourChart.ID);

      _actionPrefDialog.closeThisTooltip(this);
      _actionPrefDialog.setShell(_tourChart.getShell());
   }

   @Override
   protected Composite createToolTipContentArea(final Composite parent) {

      initUI();

      createActions();

      final Composite ui = createUI(parent);

      fillUI();
      restoreState();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.swtDefaults().applyTo(shellContainer);
      {
         final Composite container = new Composite(shellContainer, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         GridLayoutFactory.fillDefaults()
               .numColumns(2)
               .applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
         {
            createUI_10_Title(container);
            createUI_12_Actions(container);
         }

         final Composite containerGraph = new Composite(shellContainer, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(containerGraph);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerGraph);
         {
            createUI_20_Graphs(containerGraph);
            createUI_30_Filling(containerGraph);
         }
      }

      return shellContainer;
   }

   private void createUI_10_Title(final Composite parent) {

      /*
       * Label: Slideout title
       */
      final Label label = new Label(parent, SWT.NONE);
      GridDataFactory.fillDefaults().applyTo(label);
      label.setText(Messages.Slideout_TourChartGraphBackground_Label_Title);
      MTFont.setBannerFont(label);
   }

   private void createUI_12_Actions(final Composite parent) {

      final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);
      GridDataFactory
            .fillDefaults()//
            .grab(true, false)
            .align(SWT.END, SWT.BEGINNING)
            .applyTo(toolbar);

      final ToolBarManager tbm = new ToolBarManager(toolbar);
      tbm.add(_actionRestoreDefaults);
      tbm.update(true);
   }

   private void createUI_20_Graphs(final Composite parent) {

      {
         /*
          * Combo: Graph source
          */
         final Label label = new Label(parent, SWT.NONE);
         label.setText(Messages.Slideout_TourChartGraphBackground_Label_BackgroundSource);
         GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);

         final Composite bgSourcecontainer = new Composite(parent, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(bgSourcecontainer);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(bgSourcecontainer);
         {
            {
               _comboGraphBgSource = new Combo(bgSourcecontainer, SWT.DROP_DOWN | SWT.READ_ONLY);
               _comboGraphBgSource.setVisibleItemCount(20);
               _comboGraphBgSource.setToolTipText(Messages.Slideout_TourChartGraphBackground_Combo_BackgroundSource_Tooltip);
               _comboGraphBgSource.addSelectionListener(_defaultSelectionListener);
               _comboGraphBgSource.addFocusListener(_keepOpenListener);
            }
            {
               final ToolBar toolbar = new ToolBar(bgSourcecontainer, SWT.FLAT);
               GridDataFactory.fillDefaults()
                     .grab(true, false)
                     .align(SWT.BEGINNING, SWT.CENTER)
                     .applyTo(toolbar);

               final ToolBarManager tbm = new ToolBarManager(toolbar);
               tbm.add(_actionPrefDialog);
               tbm.update(true);
            }
         }
      }
      {
         /*
          * Combo: Background style
          */
         _lblGraphBgStyle = new Label(parent, SWT.NONE);
         _lblGraphBgStyle.setText(Messages.Slideout_TourChartGraphBackground_Label_BackgroundStyle);
         GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_lblGraphBgStyle);

         _comboGraphBgStyle = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
         _comboGraphBgStyle.setVisibleItemCount(20);
         _comboGraphBgStyle.addSelectionListener(_defaultSelectionListener);
         _comboGraphBgStyle.addFocusListener(_keepOpenListener);
      }
   }

   private void createUI_30_Filling(final Composite parent) {

      {
         /*
          * Label: Graph filling transparency
          */
         final String tooltipText = NLS.bind(
               Messages.Pref_Graphs_Label_GraphTransparency_Tooltip,
               UI.TRANSFORM_OPACITY_MAX);

         final Label label = new Label(parent, SWT.NONE);
         label.setText(Messages.Pref_Graphs_Label_GraphTransparency);
         label.setToolTipText(tooltipText);
         GridDataFactory.fillDefaults()
               .align(SWT.FILL, SWT.CENTER)
               .applyTo(label);

         final Composite container = new Composite(parent, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
         {
            /*
             * Graph filling transparency: light
             */
            _spinnerGraphFillingTransparency_Light = new Spinner(container, SWT.BORDER);
            _spinnerGraphFillingTransparency_Light.setMinimum(0);
            _spinnerGraphFillingTransparency_Light.setMaximum(UI.TRANSFORM_OPACITY_MAX);
            _spinnerGraphFillingTransparency_Light.setIncrement(1);
            _spinnerGraphFillingTransparency_Light.setPageIncrement(10);
            _spinnerGraphFillingTransparency_Light.setToolTipText(OtherMessages.APP_THEME_VALUE_FOR_LIGHT_TOOLTIP);
            _spinnerGraphFillingTransparency_Light.addMouseWheelListener(_defaultMouseWheelListener);
            _spinnerGraphFillingTransparency_Light.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults()
                  .align(SWT.BEGINNING, SWT.FILL)
                  .applyTo(_spinnerGraphFillingTransparency_Light);

            /*
             * Graph filling transparency: dark
             */
            _spinnerGraphFillingTransparency_Dark = new Spinner(container, SWT.BORDER);
            _spinnerGraphFillingTransparency_Dark.setMinimum(0);
            _spinnerGraphFillingTransparency_Dark.setMaximum(UI.TRANSFORM_OPACITY_MAX);
            _spinnerGraphFillingTransparency_Dark.setIncrement(1);
            _spinnerGraphFillingTransparency_Dark.setPageIncrement(10);
            _spinnerGraphFillingTransparency_Dark.setToolTipText(OtherMessages.APP_THEME_VALUE_FOR_DARK_TOOLTIP);
            _spinnerGraphFillingTransparency_Dark.addMouseWheelListener(_defaultMouseWheelListener);
            _spinnerGraphFillingTransparency_Dark.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults()
                  .align(SWT.BEGINNING, SWT.FILL)
                  .applyTo(_spinnerGraphFillingTransparency_Dark);
         }
      }
   }

   private void enableControls() {

      final TourChartConfiguration tcc = _tourChart.getTourChartConfig();

      final boolean canUseBgStyle = tcc.isBackgroundStyle_HrZone() || tcc.isBackgroundStyle_SwimmingStyle();

      _lblGraphBgStyle.setEnabled(canUseBgStyle);
      _comboGraphBgStyle.setEnabled(canUseBgStyle);
   }

   private void fillUI() {

      _availableGraphBgSourceTypes.clear();

      final TourChartConfiguration tcc = _tourChart.getTourChartConfig();

      for (final GraphBgSourceType bgSourceType : TourChartConfiguration.GRAPH_BACKGROUND_SOURCE_TYPE) {

         if (bgSourceType.graphBgSource == GraphBackgroundSource.DEFAULT || tcc == null) {

            // default is always added
            _availableGraphBgSourceTypes.add(bgSourceType);
            _comboGraphBgSource.add(bgSourceType.label);

         } else if (bgSourceType.graphBgSource == GraphBackgroundSource.HR_ZONE) {

            if (tcc.canShowBackground_HrZones) {
               _availableGraphBgSourceTypes.add(bgSourceType);
               _comboGraphBgSource.add(bgSourceType.label);
            }

         } else if (bgSourceType.graphBgSource == GraphBackgroundSource.SWIMMING_STYLE) {

            if (tcc.canShowBackground_SwimStyle) {
               _availableGraphBgSourceTypes.add(bgSourceType);
               _comboGraphBgSource.add(bgSourceType.label);
            }
         }

      }

      for (final GraphBgStyleType bgStyleType : TourChartConfiguration.GRAPH_BACKGROUND_STYLE_TYPE) {
         _comboGraphBgStyle.add(bgStyleType.label);
      }

   }

   private void initUI() {

      _defaultSelectionListener = widgetSelectedAdapter(selectionEvent -> onChangeUI());

      _defaultMouseWheelListener = mouseEvent -> {
         UI.adjustSpinnerValueOnMouseScroll(mouseEvent);
         onChangeUI();
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

   private void onChangeUI() {

      saveState();
      enableControls();

      updateUI();
      updateGraphUI();
   }

   @Override
   protected void onDispose() {

   }

   @Override
   public void resetToDefaults() {

      final int graphFillingTransparency_Dark = _prefStore.getDefaultInt(ITourbookPreferences.GRAPH_TRANSPARENCY_FILLING_DARK);
      final int graphFillingTransparency_Light = _prefStore.getDefaultInt(ITourbookPreferences.GRAPH_TRANSPARENCY_FILLING);

      final int graphFillingTransparency_Dark_Transformed = UI.transformOpacity_WhenRestored(graphFillingTransparency_Dark);
      final int graphFillingTransparency_Light_Transformed = UI.transformOpacity_WhenRestored(graphFillingTransparency_Light);

      _spinnerGraphFillingTransparency_Light.setSelection(graphFillingTransparency_Light_Transformed);
      _spinnerGraphFillingTransparency_Dark.setSelection(graphFillingTransparency_Dark_Transformed);

      select_GraphBgSource(TourChartConfiguration.GRAPH_BACKGROUND_SOURCE_DEFAULT);
      select_GraphBgStyle(TourChartConfiguration.GRAPH_BACKGROUND_STYLE_DEFAULT);

      saveState();
      enableControls();

      updateUI();
      updateGraphUI();
   }

   private void restoreState() {

      final TourChartConfiguration tcc = _tourChart.getTourChartConfig();

      if (tcc == null) {

         // this happened when a tour was not displayed in the tour chart

         return;
      }

      final int graphFillingTransparency_Dark = _prefStore.getInt(ITourbookPreferences.GRAPH_TRANSPARENCY_FILLING_DARK);
      final int graphFillingTransparency_Light = _prefStore.getInt(ITourbookPreferences.GRAPH_TRANSPARENCY_FILLING);

      final int graphFillingTransparency_Dark_Transformed = UI.transformOpacity_WhenRestored(graphFillingTransparency_Dark);
      final int graphFillingTransparency_Light_Transformed = UI.transformOpacity_WhenRestored(graphFillingTransparency_Light);

      _spinnerGraphFillingTransparency_Light.setSelection(graphFillingTransparency_Light_Transformed);
      _spinnerGraphFillingTransparency_Dark.setSelection(graphFillingTransparency_Dark_Transformed);

      // graph background
      select_GraphBgSource(tcc.graphBackground_Source);
      select_GraphBgStyle(tcc.graphBackground_Style);

      updateUI();
      enableControls();
   }

   private void saveState() {

// SET_FORMATTING_OFF

      /*
       * Update pref store
       */
      final int graphFillingOpacity_Dark              = _spinnerGraphFillingTransparency_Dark.getSelection();
      final int graphFillingOpacity_Light             = _spinnerGraphFillingTransparency_Light.getSelection();

      final int graphFillingOpacity_Transformed_Dark  = UI.transformOpacity_WhenSaved(graphFillingOpacity_Dark);
      final int graphFillingOpacity_Transformed_Light = UI.transformOpacity_WhenSaved(graphFillingOpacity_Light);

      _prefStore.setValue(ITourbookPreferences.GRAPH_TRANSPARENCY_FILLING_DARK,  graphFillingOpacity_Transformed_Dark);
      _prefStore.setValue(ITourbookPreferences.GRAPH_TRANSPARENCY_FILLING,       graphFillingOpacity_Transformed_Light);

		final GraphBgSourceType graphBgSourceType 	= _availableGraphBgSourceTypes.get(_comboGraphBgSource.getSelectionIndex());
		final GraphBgStyleType graphBgStyleType 		= TourChartConfiguration.GRAPH_BACKGROUND_STYLE_TYPE[_comboGraphBgStyle.getSelectionIndex()];

		final GraphBackgroundSource graphBgSource 	= graphBgSourceType.graphBgSource;
		final GraphBackgroundStyle graphBgStyle 		= graphBgStyleType.graphBgStyle;

		_prefStore.setValue(ITourbookPreferences.GRAPH_BACKGROUND_SOURCE,  graphBgSource.name());
		_prefStore.setValue(ITourbookPreferences.GRAPH_BACKGROUND_STYLE,   graphBgStyle.name());

		/*
		 * Update chart config
		 */
		final TourChartConfiguration tcc = _tourChart.getTourChartConfig();

		tcc.graphBackground_Source 	= graphBgSource;
		tcc.graphBackground_Style 		= graphBgStyle;

// SET_FORMATTING_ON
   }

   private void select_GraphBgSource(final GraphBackgroundSource graphBgSource) {

      int itemIndex = 0;

      for (final GraphBgSourceType graphBgSourceType : _availableGraphBgSourceTypes) {

         if (graphBgSourceType.graphBgSource.equals(graphBgSource)) {
            _comboGraphBgSource.select(itemIndex);
            return;
         }

         itemIndex++;
      }

      // set default
      _comboGraphBgSource.select(0);
   }

   private void select_GraphBgStyle(final GraphBackgroundStyle graphBgStyle) {

      final GraphBgStyleType[] graphBgStyleType = TourChartConfiguration.GRAPH_BACKGROUND_STYLE_TYPE;
      for (int itemIndex = 0; itemIndex < graphBgStyleType.length; itemIndex++) {

         final GraphBgStyleType bgSource = graphBgStyleType[itemIndex];

         if (bgSource.graphBgStyle.equals(graphBgStyle)) {
            _comboGraphBgStyle.select(itemIndex);
            return;
         }
      }

      // set default
      _comboGraphBgSource.select(0);
   }

   private void updateGraphUI() {

      _tourChart.updateTourChart();
   }

   private void updateUI() {

      /*
       * Setup pref page which should be opened with the action
       */

      String pageId = null;
      Object data = null;

      final GraphBgSourceType graphBgSourceType = _availableGraphBgSourceTypes.get(_comboGraphBgSource.getSelectionIndex());

      switch (graphBgSourceType.graphBgSource) {

      case HR_ZONE:

         final TourPerson person = _tourChart.getTourData().getDataPerson();

         pageId = PrefPagePeople.ID;
         data = new PrefPagePeopleData(PrefPagePeople.PREF_DATA_SELECT_HR_ZONES, person);

         break;

      case SWIMMING_STYLE:
         pageId = PrefPage_Appearance_Swimming.ID;
         break;

      case DEFAULT:
      default:
         pageId = PrefPageAppearanceColors.ID;
         break;
      }

      _actionPrefDialog.setPrefData(pageId, data);
   }
}
