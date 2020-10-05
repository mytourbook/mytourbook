/*******************************************************************************
 * Copyright (C) 2005, 2020 Wolfgang Schramm and Contributors
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
package net.tourbook.preferences;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.form.FormTools;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.database.IComputeNoDataserieValues;
import net.tourbook.database.IComputeTourValues;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.BreakTimeMethod;
import net.tourbook.tour.BreakTimeTool;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;
import net.tourbook.ui.views.SmoothingUI;
import net.tourbook.web.WEB;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.PreferenceLinkArea;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

public class PrefPageComputedValues extends PreferencePage implements IWorkbenchPreferencePage {

   private static final String GRAPH_LABEL_CADENCE_UNIT          = net.tourbook.common.Messages.Graph_Label_Cadence_Unit;

   public static final String  ID                                = "net.tourbook.preferences.PrefPageComputedValues";    //$NON-NLS-1$

   public static final String  URL_DOUGLAS_PEUCKER_ALGORITHM     =
         "https://en.wikipedia.org/wiki/Ramer%E2%80%93Douglas%E2%80%93Peucker_algorithm";                                //$NON-NLS-1$

   private static final String STATE_COMPUTED_VALUE_SELECTED_TAB = "computedValue.selectedTab";                          //$NON-NLS-1$

   /*
    * contains the tab folder index
    */
   public static final int    TAB_FOLDER_SMOOTHING     = 0;
   public static final int    TAB_FOLDER_BREAK_TIME    = 1;
   public static final int    TAB_FOLDER_ELEVATION     = 2;
   public static final int    TAB_FOLDER_CADENCE_ZONES = 4;

   private static final float SPEED_DIGIT_VALUE        = 10.0f;

   /**
    * 100 km/h is very high but it supports air planes which are slow on the ground
    */
   public static final int    BREAK_MAX_SPEED_KM_H     = 1000;                            // 100.0 km/h

   private int                DEFAULT_DESCRIPTION_WIDTH;
   private int                DEFAULT_V_DISTANCE_PARAGRAPH;
   private boolean            INITIAL_UNIT_IS_METRIC;

   private IPreferenceStore   _prefStore               = TourbookPlugin.getPrefStore();

   private NumberFormat       _nf0                     = NumberFormat.getNumberInstance();
   private NumberFormat       _nf1                     = NumberFormat.getNumberInstance();
   {
      _nf0.setMinimumFractionDigits(0);
      _nf0.setMaximumFractionDigits(0);
      _nf1.setMinimumFractionDigits(1);
      _nf1.setMaximumFractionDigits(1);
   }

   private boolean                  _isUpdateUI;
   private SelectionAdapter         _selectionListener;
   private MouseWheelListener       _spinnerMouseWheelListener;

   /**
    * contains the controls which are displayed in the first column, these controls are used to get
    * the maximum width and set the first column within the different section to the same width
    */
   private final ArrayList<Control> _firstColBreakTime = new ArrayList<>();

   private PixelConverter           _pc;

   /*
    * UI controls
    */
   private TabFolder         _tabFolder;

   private Combo             _comboBreakMethod;

   private PageBook          _pagebookBreakTime;

   private Composite         _pageBreakByAvgSliceSpeed;
   private Composite         _pageBreakByAvgSpeed;
   private Composite         _pageBreakBySliceSpeed;
   private Composite         _pageBreakByTimeDistance;

   private Label             _lblBreakDistanceUnit;

   private Spinner           _spinnerBreakShortestTime;
   private Spinner           _spinnerBreakMaxDistance;
   private Spinner           _spinnerBreakMinSliceSpeed;
   private Spinner           _spinnerBreakMinAvgSpeed;
   private Spinner           _spinnerBreakSliceDiff;
   private Spinner           _spinnerBreakMinAvgSpeedAS;
   private Spinner           _spinnerBreakMinSliceSpeedAS;
   private Spinner           _spinnerBreakMinSliceTimeAS;
   private Spinner           _spinnerCadenceDelimiter;
   private Spinner           _spinnerDPTolerance;

   private ScrolledComposite _smoothingScrolledContainer;
   private Composite         _smoothingScrolledContent;
   private SmoothingUI       _smoothingUI;

   private FormToolkit       _tk;

   @Override
   public void applyData(final Object data) {

      // data contains the folder index, this is set when the pref page is opened from a link

      if (data instanceof Integer) {
         _tabFolder.setSelection((Integer) data);
      }
   }

   @Override
   protected Control createContents(final Composite parent) {

      initUI(parent);

      final Composite container = createUI(parent);

      restoreState();

      return container;
   }

   private Composite createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory
            .fillDefaults()//
            .grab(true, true)
            .applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 15).applyTo(container);
      {
         /*
          * label: info
          */
         final Label label = new Label(container, SWT.WRAP);
         GridDataFactory.fillDefaults().hint(DEFAULT_DESCRIPTION_WIDTH, SWT.DEFAULT).applyTo(label);
         label.setText(Messages.Compute_Values_Label_Info);

         /*
          * tab folder: computed values
          */
         _tabFolder = new TabFolder(container, SWT.TOP);
         GridDataFactory
               .fillDefaults()//
               .grab(true, true)
               .applyTo(_tabFolder);
         {

            final TabItem tabSmoothing = new TabItem(_tabFolder, SWT.NONE);
            tabSmoothing.setControl(createUI_10_Smoothing(_tabFolder));
            tabSmoothing.setText(Messages.Compute_Values_Group_Smoothing);

            final TabItem tabBreakTime = new TabItem(_tabFolder, SWT.NONE);
            tabBreakTime.setControl(createUI_50_BreakTime(_tabFolder));
            tabBreakTime.setText(Messages.Compute_BreakTime_Group_BreakTime);

            final TabItem tabElevation = new TabItem(_tabFolder, SWT.NONE);
            tabElevation.setControl(createUI_20_ElevationGain(_tabFolder));
            tabElevation.setText(Messages.compute_tourValueElevation_group_computeTourAltitude);

            final TabItem tabHrZone = new TabItem(_tabFolder, SWT.NONE);
            tabHrZone.setControl(createUI_60_HrZone(_tabFolder));
            tabHrZone.setText(Messages.Compute_HrZone_Group);

            // tab: cadence zones
            final TabItem tabItemCadenceZones = new TabItem(_tabFolder, SWT.NONE);
            tabItemCadenceZones.setControl(createUI_70_CadenceZones(_tabFolder));
            tabItemCadenceZones.setText(Messages.Compute_CadenceZonesTimes_Group);

            /**
             * 4.8.2009 week no/year is currently disabled because a new field in the db is
             * required which holds the year of the week<br>
             * <br>
             * all plugins must be adjusted which set's the week number of a tour
             */
//				createUIWeek(container);
         }
      }

      return _tabFolder;
   }

   private Control createUI_10_Smoothing(final Composite parent) {

      _tk = new FormToolkit(parent.getDisplay());
      _smoothingUI = new SmoothingUI();

      _smoothingScrolledContainer = new ScrolledComposite(parent, SWT.V_SCROLL);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_smoothingScrolledContainer);
      {
         _smoothingScrolledContent = _tk.createComposite(_smoothingScrolledContainer);
         GridDataFactory
               .fillDefaults()//
               .grab(true, true)
               .hint(DEFAULT_DESCRIPTION_WIDTH, SWT.DEFAULT)
               .applyTo(_smoothingScrolledContent);
         GridLayoutFactory
               .swtDefaults() //
               .extendedMargins(5, 5, 10, 5)
               .numColumns(1)
               .applyTo(_smoothingScrolledContent);
//			_smoothingScrolledContent.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
         {
            _smoothingUI.createUI(_smoothingScrolledContent, true, true);
         }

         // setup scrolled container
         _smoothingScrolledContainer.setExpandVertical(true);
         _smoothingScrolledContainer.setExpandHorizontal(true);
         _smoothingScrolledContainer.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(final ControlEvent e) {
               _smoothingScrolledContainer.setMinSize(//
                     _smoothingScrolledContent.computeSize(SWT.DEFAULT, SWT.DEFAULT));
            }
         });

         _smoothingScrolledContainer.setContent(_smoothingScrolledContent);
      }

      return _smoothingScrolledContainer;
   }

   private Control createUI_20_ElevationGain(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.swtDefaults().extendedMargins(5, 5, 10, 5).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
         final Composite dpContainer = new Composite(container, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(dpContainer);
         GridLayoutFactory.fillDefaults().numColumns(3).applyTo(dpContainer);
         {
            // label: DP Tolerance
            final Link linkDP = new Link(dpContainer, SWT.NONE);
            linkDP.setText(Messages.Compute_TourValue_ElevationGain_Link_DBTolerance);
            linkDP.setToolTipText(Messages.Tour_Segmenter_Label_DPTolerance_Tooltip);
            linkDP.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  WEB.openUrl(PrefPageComputedValues.URL_DOUGLAS_PEUCKER_ALGORITHM);
               }
            });

            // spinner: minimum altitude
            _spinnerDPTolerance = new Spinner(dpContainer, SWT.BORDER);
            GridDataFactory.fillDefaults().applyTo(_spinnerDPTolerance);
            _spinnerDPTolerance.setMinimum(1); // 0.1
            _spinnerDPTolerance.setMaximum(10000); // 1000
            _spinnerDPTolerance.setDigits(1);
            _spinnerDPTolerance.addSelectionListener(_selectionListener);
            _spinnerDPTolerance.addMouseWheelListener(_spinnerMouseWheelListener);

            // label: unit
            final Label label = new Label(dpContainer, SWT.NONE);
            label.setText(net.tourbook.common.UI.UNIT_LABEL_ALTITUDE);
         }

         {
            // label: description
            Label label = new Label(container, SWT.WRAP);
            GridDataFactory
                  .fillDefaults()//
                  .indent(0, DEFAULT_V_DISTANCE_PARAGRAPH)
                  .hint(DEFAULT_DESCRIPTION_WIDTH, SWT.DEFAULT)
                  .grab(true, false)
                  .applyTo(label);
            label.setText(Messages.Compute_TourValue_ElevationGain_Label_Description);

            // label: tip
            final Composite tipContainer = new Composite(container, SWT.NONE);
            GridDataFactory
                  .fillDefaults()//
                  .indent(0, DEFAULT_V_DISTANCE_PARAGRAPH)
                  .grab(true, false)
                  .applyTo(tipContainer);
            GridLayoutFactory.fillDefaults().margins(5, 5).applyTo(tipContainer);
            tipContainer.setBackground(tipContainer.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
            {
               // label: description
               label = new Label(tipContainer, SWT.WRAP);
               GridDataFactory
                     .fillDefaults()//
                     .hint(DEFAULT_DESCRIPTION_WIDTH, SWT.DEFAULT)
                     .grab(true, false)
                     .applyTo(label);
               label.setText(Messages.compute_tourValueElevation_label_description_Hints);
            }
         }

         final Composite btnContainer = new Composite(container, SWT.NONE);
         GridDataFactory.fillDefaults().applyTo(btnContainer);
         GridLayoutFactory.fillDefaults().applyTo(btnContainer);
         {
            // button: compute computed values
            final Button btnComputValues = new Button(btnContainer, SWT.NONE);
            GridDataFactory
                  .fillDefaults()//
                  .indent(0, DEFAULT_V_DISTANCE_PARAGRAPH)
                  .applyTo(btnComputValues);
            btnComputValues.setText(Messages.compute_tourValueElevation_button_computeValues);
            btnComputValues.setToolTipText(Messages.Compute_TourValue_ElevationGain_Button_ComputeValues_Tooltip);
            btnComputValues.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onComputeElevationGainValues();
               }
            });
         }
      }
      return container;
   }

   private Composite createUI_50_BreakTime(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.swtDefaults().extendedMargins(5, 5, 10, 5).numColumns(2).applyTo(container);

//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
      {
         final Composite containerTitle = new Composite(container, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(containerTitle);
         GridLayoutFactory.fillDefaults().extendedMargins(0, 0, 0, 10).numColumns(1).applyTo(containerTitle);
         {
            /*
             * label: compute break time by
             */
            final Label label = new Label(containerTitle, SWT.NONE);
            label.setText(Messages.Compute_BreakTime_Label_Title);
         }

         /*
          * label: compute break time by
          */
         Label label = new Label(container, SWT.NONE);
         GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);
         label.setText(Messages.Compute_BreakTime_Label_ComputeBreakTimeBy);
         _firstColBreakTime.add(label);

         // combo: break method
         _comboBreakMethod = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
         _comboBreakMethod.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               updateUIShowSelectedBreakTimeMethod();
               onModifyBreakTime();
            }
         });

         // fill combo
         for (final BreakTimeMethod breakMethod : BreakTimeTool.BREAK_TIME_METHODS) {
            _comboBreakMethod.add(breakMethod.uiText);
         }

         /*
          * pagebook: break method
          */
         _pagebookBreakTime = new PageBook(container, SWT.NONE);
         GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(_pagebookBreakTime);
         {
            _pageBreakByAvgSliceSpeed = createUI_51_BreakByAvgSliceSpeed(_pagebookBreakTime);
            _pageBreakByAvgSpeed = createUI_52_BreakByAvgSpeed(_pagebookBreakTime);
            _pageBreakBySliceSpeed = createUI_53_BreakBySliceSpeed(_pagebookBreakTime);
            _pageBreakByTimeDistance = createUI_54_BreakByTimeDistance(_pagebookBreakTime);
         }

         /*
          * label: description part II
          */
         label = new Label(container, SWT.WRAP);
         GridDataFactory
               .fillDefaults()//
               .span(2, 1)
               .indent(0, DEFAULT_V_DISTANCE_PARAGRAPH)
               .hint(DEFAULT_DESCRIPTION_WIDTH, SWT.DEFAULT)
               .grab(true, false)
               .applyTo(label);
         label.setText(Messages.Compute_BreakTime_Label_Description);

         /*
          * hints
          */
         FormTools.createBullets(
               container, //
               Messages.Compute_BreakTime_Label_Hints,
               1,
               2,
               DEFAULT_DESCRIPTION_WIDTH,
               null);

         /*
          * compute speed values for all tours
          */
         final Composite btnContainer = new Composite(container, SWT.NONE);
         GridDataFactory.fillDefaults().span(3, 1).applyTo(btnContainer);
         GridLayoutFactory.fillDefaults().applyTo(btnContainer);
         {
            // button: compute computed values
            final Button btnComputValues = new Button(btnContainer, SWT.NONE);
            GridDataFactory.fillDefaults().indent(0, DEFAULT_V_DISTANCE_PARAGRAPH).applyTo(btnComputValues);
            btnComputValues.setText(Messages.Compute_BreakTime_Button_ComputeAllTours);
            btnComputValues.setToolTipText(Messages.Compute_BreakTime_Button_ComputeAllTours_Tooltip);
            btnComputValues.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onComputeBreakTimeValues();
               }
            });
         }
      }

      /*
       * force pages to be displayed otherwise they are hidden or the hint is not computed for the
       * first column until a resize is done
       */
      _pagebookBreakTime.showPage(_pageBreakBySliceSpeed);

      container.layout(true, true);
      net.tourbook.common.UI.setEqualizeColumWidths(_firstColBreakTime);

      return container;
   }

   private Composite createUI_51_BreakByAvgSliceSpeed(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
      {
         /*
          * minimum average speed
          */
         {
            // label: minimum speed
            Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Compute_BreakTime_Label_MinimumAvgSpeed);
            _firstColBreakTime.add(label);

            // spinner: minimum speed
            _spinnerBreakMinAvgSpeedAS = new Spinner(container, SWT.BORDER);
            GridDataFactory
                  .fillDefaults()//
                  .applyTo(_spinnerBreakMinAvgSpeedAS);
            _spinnerBreakMinAvgSpeedAS.setMinimum(0); // 0.0 km/h
            _spinnerBreakMinAvgSpeedAS.setMaximum(BREAK_MAX_SPEED_KM_H); // 10.0 km/h
            _spinnerBreakMinAvgSpeedAS.setDigits(1);
            _spinnerBreakMinAvgSpeedAS.addSelectionListener(_selectionListener);
            _spinnerBreakMinAvgSpeedAS.addMouseWheelListener(_spinnerMouseWheelListener);

            // label: km/h
            label = new Label(container, SWT.NONE);
            label.setText(net.tourbook.common.UI.UNIT_LABEL_SPEED);
         }

         /*
          * minimum slice speed
          */
         {
            // label: minimum speed
            Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Compute_BreakTime_Label_MinimumSliceSpeed);
            _firstColBreakTime.add(label);

            // spinner: minimum speed
            _spinnerBreakMinSliceSpeedAS = new Spinner(container, SWT.BORDER);
            GridDataFactory
                  .fillDefaults()//
                  .applyTo(_spinnerBreakMinSliceSpeedAS);
            _spinnerBreakMinSliceSpeedAS.setMinimum(0); // 0.0 km/h
            _spinnerBreakMinSliceSpeedAS.setMaximum(BREAK_MAX_SPEED_KM_H); // 10.0 km/h
            _spinnerBreakMinSliceSpeedAS.setDigits(1);
            _spinnerBreakMinSliceSpeedAS.addSelectionListener(_selectionListener);
            _spinnerBreakMinSliceSpeedAS.addMouseWheelListener(_spinnerMouseWheelListener);

            // label: km/h
            label = new Label(container, SWT.NONE);
            label.setText(net.tourbook.common.UI.UNIT_LABEL_SPEED);
         }

         /*
          * minimum slice time
          */
         {
            // label: minimum slice time
            Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Compute_BreakTime_Label_MinimumSliceTime);
            _firstColBreakTime.add(label);

            // spinner: minimum slice time
            _spinnerBreakMinSliceTimeAS = new Spinner(container, SWT.BORDER);
            GridDataFactory
                  .fillDefaults()//
                  .applyTo(_spinnerBreakMinSliceTimeAS);
            _spinnerBreakMinSliceTimeAS.setMinimum(0); // 0 sec
            _spinnerBreakMinSliceTimeAS.setMaximum(10); // 10 sec
            _spinnerBreakMinSliceTimeAS.addSelectionListener(_selectionListener);
            _spinnerBreakMinSliceTimeAS.addMouseWheelListener(_spinnerMouseWheelListener);

            // label: seconds
            label = new Label(container, SWT.NONE);
            label.setText(Messages.app_unit_seconds);
         }

         /*
          * label: description
          */
         final Label label = new Label(container, SWT.WRAP);
         GridDataFactory
               .fillDefaults()//
               .span(3, 1)
               .indent(0, DEFAULT_V_DISTANCE_PARAGRAPH)
               .hint(DEFAULT_DESCRIPTION_WIDTH, SWT.DEFAULT)
               .grab(true, false)
               .applyTo(label);
         label.setText(Messages.Compute_BreakTime_Label_Description_ComputeByAvgSliceSpeed);
      }

      return container;
   }

   private Composite createUI_52_BreakByAvgSpeed(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
      {
         /*
          * minimum average speed
          */

         // label: minimum speed
         Label label = new Label(container, SWT.NONE);
         label.setText(Messages.Compute_BreakTime_Label_MinimumAvgSpeed);
         _firstColBreakTime.add(label);

         // spinner: minimum speed
         _spinnerBreakMinAvgSpeed = new Spinner(container, SWT.BORDER);
         GridDataFactory
               .fillDefaults()//
               .applyTo(_spinnerBreakMinAvgSpeed);
         _spinnerBreakMinAvgSpeed.setMinimum(0); // 0.0 km/h
         _spinnerBreakMinAvgSpeed.setMaximum(BREAK_MAX_SPEED_KM_H); // 10.0 km/h
         _spinnerBreakMinAvgSpeed.setDigits(1);
         _spinnerBreakMinAvgSpeed.addSelectionListener(_selectionListener);
         _spinnerBreakMinAvgSpeed.addMouseWheelListener(_spinnerMouseWheelListener);

         // label: km/h
         label = new Label(container, SWT.NONE);
         label.setText(net.tourbook.common.UI.UNIT_LABEL_SPEED);

         /*
          * label: description
          */
         label = new Label(container, SWT.WRAP);
         GridDataFactory
               .fillDefaults()//
               .span(3, 1)
               .indent(0, DEFAULT_V_DISTANCE_PARAGRAPH)
               .hint(DEFAULT_DESCRIPTION_WIDTH, SWT.DEFAULT)
               .grab(true, false)
               .applyTo(label);
         label.setText(Messages.Compute_BreakTime_Label_Description_ComputeByAvgSpeed);
      }

      return container;
   }

   private Composite createUI_53_BreakBySliceSpeed(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
         /*
          * minimum slice speed
          */

         // label: minimum speed
         Label label = new Label(container, SWT.NONE);
         label.setText(Messages.Compute_BreakTime_Label_MinimumSliceSpeed);
         _firstColBreakTime.add(label);

         // spinner: minimum speed
         _spinnerBreakMinSliceSpeed = new Spinner(container, SWT.BORDER);
         GridDataFactory
               .fillDefaults()//
               .applyTo(_spinnerBreakMinSliceSpeed);
         _spinnerBreakMinSliceSpeed.setMinimum(0); // 0.0 km/h
         _spinnerBreakMinSliceSpeed.setMaximum(BREAK_MAX_SPEED_KM_H); // 10.0 km/h
         _spinnerBreakMinSliceSpeed.setDigits(1);
         _spinnerBreakMinSliceSpeed.addSelectionListener(_selectionListener);
         _spinnerBreakMinSliceSpeed.addMouseWheelListener(_spinnerMouseWheelListener);

         // label: km/h
         label = new Label(container, SWT.NONE);
         label.setText(net.tourbook.common.UI.UNIT_LABEL_SPEED);

         /*
          * label: description
          */
         label = new Label(container, SWT.WRAP);
         GridDataFactory
               .fillDefaults()//
               .span(3, 1)
               .indent(0, DEFAULT_V_DISTANCE_PARAGRAPH)
               .hint(DEFAULT_DESCRIPTION_WIDTH, SWT.DEFAULT)
               .grab(true, false)
               .applyTo(label);
         label.setText(Messages.Compute_BreakTime_Label_Description_ComputeBySliceSpeed);
      }

      return container;
   }

   private Composite createUI_54_BreakByTimeDistance(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
      {
         /*
          * shortest break time
          */
         {
            // label: break min time
            Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Compute_BreakTime_Label_MinimumTime);
            _firstColBreakTime.add(label);

            // spinner: break minimum time
            _spinnerBreakShortestTime = new Spinner(container, SWT.BORDER);
            GridDataFactory
                  .fillDefaults()//
                  .applyTo(_spinnerBreakShortestTime);
            _spinnerBreakShortestTime.setMinimum(1);
            _spinnerBreakShortestTime.setMaximum(120); // 120 seconds
            _spinnerBreakShortestTime.addSelectionListener(_selectionListener);
            _spinnerBreakShortestTime.addMouseWheelListener(_spinnerMouseWheelListener);

            // label: unit
            label = new Label(container, SWT.NONE);
            label.setText(Messages.App_Unit_Seconds_Small);
         }

         /*
          * recording distance
          */
         {
            // label: break min distance
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Compute_BreakTime_Label_MinimumDistance);
            _firstColBreakTime.add(label);

            // spinner: break minimum time
            _spinnerBreakMaxDistance = new Spinner(container, SWT.BORDER);
            GridDataFactory
                  .fillDefaults()//
                  .applyTo(_spinnerBreakMaxDistance);
            _spinnerBreakMaxDistance.setMinimum(1);
            _spinnerBreakMaxDistance.setMaximum(1000); // 1000 m/yards
            _spinnerBreakMaxDistance.addSelectionListener(_selectionListener);
            _spinnerBreakMaxDistance.addMouseWheelListener(_spinnerMouseWheelListener);

            // label: unit
            _lblBreakDistanceUnit = new Label(container, SWT.NONE);
            _lblBreakDistanceUnit.setText(net.tourbook.common.UI.UNIT_LABEL_DISTANCE_M_OR_YD);
            GridDataFactory
                  .fillDefaults()//
//						.span(2, 1)
                  .align(SWT.FILL, SWT.CENTER)
                  .applyTo(_lblBreakDistanceUnit);
         }

         /*
          * slice diff break
          */
         {
            // label: break slice diff
            Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Compute_BreakTime_Label_SliceDiffBreak);
            label.setToolTipText(Messages.Compute_BreakTime_Label_SliceDiffBreak_Tooltip);
            _firstColBreakTime.add(label);

            // spinner: slice diff break time
            _spinnerBreakSliceDiff = new Spinner(container, SWT.BORDER);
            GridDataFactory
                  .fillDefaults()//
                  .applyTo(_spinnerBreakSliceDiff);
            _spinnerBreakSliceDiff.setMinimum(0);
            _spinnerBreakSliceDiff.setMaximum(60); // minutes
            _spinnerBreakSliceDiff.addSelectionListener(_selectionListener);
            _spinnerBreakSliceDiff.addMouseWheelListener(_spinnerMouseWheelListener);

            // label: unit
            label = new Label(container, SWT.NONE);
            label.setText(Messages.App_Unit_Minute);
         }

         /*
          * label: description
          */
         final Label label = new Label(container, SWT.WRAP);
         GridDataFactory
               .fillDefaults()//
               .span(3, 1)
               .indent(0, DEFAULT_V_DISTANCE_PARAGRAPH)
               .hint(DEFAULT_DESCRIPTION_WIDTH, SWT.DEFAULT)
               .grab(true, false)
               .applyTo(label);
         label.setText(Messages.Compute_BreakTime_Label_Description_ComputeByTime);
      }

      return container;
   }

   private Control createUI_60_HrZone(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.swtDefaults().extendedMargins(5, 5, 10, 5).numColumns(1).applyTo(container);
      {
         final PreferenceLinkArea prefLink = new PreferenceLinkArea(
               container,
               SWT.NONE,
               PrefPagePeople.ID,
               Messages.Compute_HrZone_Link,
               (IWorkbenchPreferenceContainer) getContainer(),
               new PrefPagePeopleData(PrefPagePeople.PREF_DATA_SELECT_HR_ZONES, null));

         GridDataFactory
               .fillDefaults()//
               .grab(true, false)
               .hint(DEFAULT_DESCRIPTION_WIDTH, SWT.DEFAULT)
               .applyTo(prefLink.getControl());
      }

      return container;
   }

   /**
    * UI for the selection of the cadence differentiating slow cadence (hiking) from
    * fast cadence (running).
    */
   private Control createUI_70_CadenceZones(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridLayoutFactory.swtDefaults().numColumns(3).extendedMargins(0, 0, 7, 0).applyTo(container);
      {
         Label label = new Label(container, SWT.NONE);
         label.setText(Messages.Compute_CadenceZonesTimes_Label_CadenceZonesDelimiter);

         // spinner: cadence zone delimiter
         _spinnerCadenceDelimiter = new Spinner(container, SWT.BORDER);
         GridDataFactory.fillDefaults() //
               .applyTo(_spinnerCadenceDelimiter);
         _spinnerCadenceDelimiter.setMinimum(0);
         _spinnerCadenceDelimiter.setMaximum(200);
         _spinnerCadenceDelimiter.addSelectionListener(_selectionListener);
         _spinnerCadenceDelimiter.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseScrolled(final MouseEvent event) {
               if (_isUpdateUI) {
                  return;
               }
               Util.adjustSpinnerValueOnMouseScroll(event);
               onModifyCadenceZonesDelimiter();
            }
         });

         // label: unit
         label = new Label(container, SWT.NONE);
         label.setText(GRAPH_LABEL_CADENCE_UNIT);
      }

      GridDataFactory.fillDefaults().applyTo(container);
      {
         // label: Text explaining the meaning of the two zones
         final Label label = new Label(container, SWT.WRAP);
         GridDataFactory.fillDefaults()//
               .grab(true, false)
               .span(3, 0)
               .hint(net.tourbook.common.UI.DEFAULT_DESCRIPTION_WIDTH, SWT.DEFAULT)
               .applyTo(label);
         label.setText(Messages.Compute_CadenceZonesTimes_Label_Description_CadenceZonesDelimiter);
      }

      // button: compute time values
      final Button buttonComputeTimes = new Button(container, SWT.NONE);
      GridDataFactory.fillDefaults().span(3, 0).indent(0, 50).align(SWT.BEGINNING, SWT.FILL).applyTo(buttonComputeTimes);
      buttonComputeTimes.setText(Messages.Compute_CadenceZonesTimes_ComputeAllTours);
      buttonComputeTimes.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            onComputeCadenceZonesTimeValues();
         }
      });

      return container;
   }

   @Override
   public void dispose() {

      _smoothingUI.dispose();

      super.dispose();
   }

   private void fireTourModifyEvent() {

      TourManager.getInstance().removeAllToursFromCache();
      TourManager.fireEvent(TourEventId.CLEAR_DISPLAYED_TOUR);

      // fire unique event for all changes
      TourManager.fireEvent(TourEventId.ALL_TOURS_ARE_MODIFIED);
   }

   private BreakTimeMethod getSelectedBreakMethod() {

      int selectedIndex = _comboBreakMethod.getSelectionIndex();

      if (selectedIndex == -1) {
         selectedIndex = 0;
      }

      return BreakTimeTool.BREAK_TIME_METHODS[selectedIndex];
   }

   @Override
   public void init(final IWorkbench workbench) {}

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      DEFAULT_DESCRIPTION_WIDTH = _pc.convertWidthInCharsToPixels(80);
      DEFAULT_V_DISTANCE_PARAGRAPH = _pc.convertVerticalDLUsToPixels(4);
      INITIAL_UNIT_IS_METRIC = net.tourbook.common.UI.UNIT_IS_METRIC;

      _selectionListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            if (_isUpdateUI) {
               return;
            }
            onModifyBreakTime();
         }
      };

      _spinnerMouseWheelListener = new MouseWheelListener() {
         @Override
         public void mouseScrolled(final MouseEvent event) {
            if (_isUpdateUI) {
               return;
            }
            Util.adjustSpinnerValueOnMouseScroll(event);
            onModifyBreakTime();
         }
      };
   }

   @Override
   public boolean okToLeave() {
      saveUIState();
      return super.okToLeave();
   }

   private void onComputeBreakTimeValues() {

      if (MessageDialog.openConfirm(
            Display.getCurrent().getActiveShell(),
            Messages.Compute_BreakTime_Dialog_ComputeForAllTours_Title,
            Messages.Compute_BreakTime_Dialog_ComputeForAllTours_Message) == false) {
         return;
      }

      saveState();

      final int[] oldBreakTime = { 0 };
      final int[] newBreakTime = { 0 };

      final IComputeTourValues computeTourValueConfig = new IComputeTourValues() {

         @Override
         public boolean computeTourValues(final TourData oldTourData) {

            final int tourElapsedTime = (int) oldTourData.getTourDeviceTime_Elapsed();

            // get old break time
            final int tourMovingTime = (int) oldTourData.getTourComputedTime_Moving();
            oldBreakTime[0] += tourElapsedTime - tourMovingTime;

            // force the break time to be recomputed with the current values which are already store in the pref store
            oldTourData.setBreakTimeSerie(null);

            // recompute break time
            oldTourData.computeTourMovingTime();

            return true;
         }

         @Override
         public String getResultText() {

            return NLS.bind(
                  Messages.Compute_BreakTime_ForAllTour_Job_Result, //
                  new Object[] {
                        net.tourbook.common.UI.format_hh_mm_ss(oldBreakTime[0]),
                        net.tourbook.common.UI.format_hh_mm_ss(newBreakTime[0]), });
         }

         @Override
         public String getSubTaskText(final TourData savedTourData) {

            String subTaskText = null;

            if (savedTourData != null) {

               // get new value
               final int tourElapsedTime = (int) savedTourData.getTourDeviceTime_Elapsed();

               // get old break time
               final int tourMovingTime = (int) savedTourData.getTourComputedTime_Moving();
               newBreakTime[0] += tourElapsedTime - tourMovingTime;

               subTaskText = NLS.bind(
                     Messages.Compute_BreakTime_ForAllTour_Job_SubTask, //
                     new Object[] {
                           net.tourbook.common.UI.format_hh_mm_ss(oldBreakTime[0]),
                           net.tourbook.common.UI.format_hh_mm_ss(newBreakTime[0]), });
            }

            return subTaskText;
         }
      };

      TourDatabase.computeAnyValues_ForAllTours(computeTourValueConfig, null);

      fireTourModifyEvent();
   }

   private void onComputeCadenceZonesTimeValues() {

      if (MessageDialog.openConfirm(
            Display.getCurrent().getActiveShell(),
            Messages.Compute_CadenceZonesTimes_Dialog_ComputeForAllTours_Title,
            Messages.Compute_CadenceZonesTimes_Dialog_ComputeForAllTours_Message) == false) {
         return;
      }

      saveState();

      final int[] old_CadenceZone_SlowTime = { 0 };
      final int[] old_CadenceZone_FastTime = { 0 };

      final int[] total_Old_CadenceZone_SlowTime = { 0 };
      final int[] total_New_CadenceZone_SlowTime = { 0 };
      final int[] total_Old_CadenceZone_FastTime = { 0 };
      final int[] total_New_CadenceZone_FastTime = { 0 };

      final IComputeNoDataserieValues computeTourValueConfig = new IComputeNoDataserieValues() {

         @Override
         public boolean computeTourValues(final TourData originalTourData, final PreparedStatement sqlUpdateStatement) throws SQLException {

            // keep old values
            old_CadenceZone_SlowTime[0] = originalTourData.getCadenceZone_SlowTime();
            total_Old_CadenceZone_SlowTime[0] += old_CadenceZone_SlowTime[0];
            old_CadenceZone_FastTime[0] = originalTourData.getCadenceZone_FastTime();
            total_Old_CadenceZone_FastTime[0] += old_CadenceZone_FastTime[0];

            if (originalTourData.computeCadenceZonesTimes() == false) {
               // cadence zones times could not be computed
               return false;
            }

            // update total new values
            total_New_CadenceZone_SlowTime[0] += originalTourData.getCadenceZone_SlowTime();
            total_New_CadenceZone_FastTime[0] += originalTourData.getCadenceZone_FastTime();

            // update cadence zones times in the database
            sqlUpdateStatement.setInt(1, originalTourData.getCadenceZone_SlowTime());
            sqlUpdateStatement.setInt(2, originalTourData.getCadenceZone_FastTime());
            sqlUpdateStatement.setInt(3, originalTourData.getCadenceZones_DelimiterValue());
            sqlUpdateStatement.setLong(4, originalTourData.getTourId());

            return true;
         }

         @Override
         public String getResultText() {

            return net.tourbook.common.UI.NEW_LINE + NLS.bind(
                  Messages.Compute_CadenceZonesTimes_ComputeForAllTours_Job_Result,
                  new Object[] {
                        net.tourbook.common.UI.format_hh_mm_ss(total_Old_CadenceZone_SlowTime[0]),
                        net.tourbook.common.UI.format_hh_mm_ss(total_New_CadenceZone_SlowTime[0]),
                        net.tourbook.common.UI.format_hh_mm_ss(total_Old_CadenceZone_FastTime[0]),
                        net.tourbook.common.UI.format_hh_mm_ss(total_New_CadenceZone_FastTime[0]), });
         }

         @Override
         public String getSQLUpdateStatement() {

            return TourManager.cadenceZonesTimes_StatementUpdate;
         }

      };

      TourDatabase.computeNoDataserieValues_ForAllTours(computeTourValueConfig, null);

      fireTourModifyEvent();
   }

   private void onComputeElevationGainValues() {

      final float prefDPTolerance = _spinnerDPTolerance.getSelection() / 10.0f;

      final String dpToleranceWithUnit = _nf1.format(prefDPTolerance) + net.tourbook.common.UI.SPACE1
            + net.tourbook.common.UI.UNIT_LABEL_ALTITUDE;

      if (MessageDialog.openConfirm(
            Display.getCurrent().getActiveShell(),
            Messages.compute_tourValueElevation_dlg_computeValues_title,
            NLS.bind(Messages.Compute_TourValue_ElevationGain_Dlg_ComputeValues_Message, dpToleranceWithUnit)) == false) {
         return;
      }

      saveState();

      final int[] elevation = new int[] { 0, 0 };

      final IComputeTourValues computeTourValueConfig = new IComputeTourValues() {

         @Override
         public boolean computeTourValues(final TourData oldTourData) {

            // keep old value
            elevation[0] += oldTourData.getTourAltUp();

            return oldTourData.computeAltitudeUpDown();
         }

         private String getElevationDifferenceString(final int elevationDifference) {

            final StringBuilder differenceResult = new StringBuilder();
            if (elevationDifference > 0) {
               differenceResult.append(net.tourbook.common.UI.SYMBOL_PLUS);
            }

            differenceResult.append(_nf0.format((elevationDifference) / UI.UNIT_VALUE_ALTITUDE));
            return differenceResult.toString();
         }

         @Override
         public String getResultText() {

            final int elevationDifference = elevation[1] - elevation[0];
            final String differenceResult = getElevationDifferenceString(elevationDifference);

            return NLS.bind(
                  Messages.Compute_TourValue_ElevationGain_ResultText,
                  new Object[] {
                        dpToleranceWithUnit,
                        differenceResult,
                        net.tourbook.common.UI.UNIT_LABEL_ALTITUDE
                  });
         }

         @Override
         public String getSubTaskText(final TourData savedTourData) {

            String subTaskText = null;

            if (savedTourData != null) {

               // summarize new values
               elevation[1] += savedTourData.getTourAltUp();

               final int elevationDifference = elevation[1] - elevation[0];
               final String differenceResult = getElevationDifferenceString(elevationDifference);

               subTaskText = NLS.bind(
                     Messages.compute_tourValueElevation_subTaskText, //
                     new Object[] {
                           differenceResult,
                           net.tourbook.common.UI.UNIT_LABEL_ALTITUDE //
                     });
            }

            return subTaskText;
         }
      };

      TourDatabase.computeAnyValues_ForAllTours(computeTourValueConfig, null);

      fireTourModifyEvent();
   }

   private void onModifyBreakTime() {

      saveState();

      TourManager.getInstance().removeAllToursFromCache();

      // fire unique event for all changes
      TourManager.fireEvent(TourEventId.TOUR_CHART_PROPERTY_IS_MODIFIED);
   }

   private void onModifyCadenceZonesDelimiter() {

      saveState();

      TourManager.getInstance().removeAllToursFromCache();

      // fire unique event for all changes
      TourManager.fireEvent(TourEventId.TOUR_CHART_PROPERTY_IS_MODIFIED);
   }

   @Override
   public boolean performCancel() {
      saveUIState();
      return super.performCancel();
   }

   @Override
   protected void performDefaults() {

      final int selectedTab = _tabFolder.getSelectionIndex();

      if (selectedTab == TAB_FOLDER_ELEVATION) {

         /*
          * compute altitude
          */
         final float prefDPTolerance = _prefStore.getDefaultFloat(//
               ITourbookPreferences.COMPUTED_ALTITUDE_DP_TOLERANCE) * 10 / UI.UNIT_VALUE_ALTITUDE;
         _spinnerDPTolerance.setSelection((int) prefDPTolerance);

      } else if (selectedTab == TAB_FOLDER_SMOOTHING) {

         /*
          * compute smoothing
          */
         _smoothingUI.performDefaults();

      } else if (selectedTab == TAB_FOLDER_BREAK_TIME) {

         _isUpdateUI = true;
         {
            /*
             * break method
             */
            final String prefBreakTimeMethod = _prefStore.getDefaultString(ITourbookPreferences.BREAK_TIME_METHOD2);
            selectBreakMethod(prefBreakTimeMethod);

            /*
             * break by avg+slice speed
             */
            final float prefMinAvgSpeedAS = _prefStore.getDefaultFloat(//
                  ITourbookPreferences.BREAK_TIME_MIN_AVG_SPEED_AS);
            final float prefMinSliceSpeedAS = _prefStore.getDefaultFloat(//
                  ITourbookPreferences.BREAK_TIME_MIN_SLICE_SPEED_AS);
            final int prefMinSliceTimeAS = _prefStore.getDefaultInt(//
                  ITourbookPreferences.BREAK_TIME_MIN_SLICE_TIME_AS);

            _spinnerBreakMinAvgSpeedAS.setSelection(//
                  (int) (prefMinAvgSpeedAS * SPEED_DIGIT_VALUE / UI.UNIT_VALUE_DISTANCE));
            _spinnerBreakMinSliceSpeedAS.setSelection(//
                  (int) (prefMinSliceSpeedAS * SPEED_DIGIT_VALUE / UI.UNIT_VALUE_DISTANCE));
            _spinnerBreakMinSliceTimeAS.setSelection(prefMinSliceTimeAS);

            /*
             * break by speed
             */
            final float prefMinSliceSpeed = _prefStore
                  .getDefaultFloat(ITourbookPreferences.BREAK_TIME_MIN_SLICE_SPEED);
            final float prefMinAvgSpeed = _prefStore.getDefaultFloat(ITourbookPreferences.BREAK_TIME_MIN_AVG_SPEED);

            _spinnerBreakMinSliceSpeed.setSelection(//
                  (int) (prefMinSliceSpeed * SPEED_DIGIT_VALUE * UI.UNIT_VALUE_DISTANCE));
            _spinnerBreakMinAvgSpeed.setSelection(//
                  (int) (prefMinAvgSpeed * SPEED_DIGIT_VALUE * UI.UNIT_VALUE_DISTANCE));

            /*
             * break time by time/distance
             */
            final int prefShortestTime = _prefStore.getDefaultInt(ITourbookPreferences.BREAK_TIME_SHORTEST_TIME);
            final float prefMaxDistance = _prefStore.getDefaultFloat(ITourbookPreferences.BREAK_TIME_MAX_DISTANCE);
            final int prefSliceDiff = _prefStore.getDefaultInt(ITourbookPreferences.BREAK_TIME_SLICE_DIFF);
            final float breakDistance = prefMaxDistance / UI.UNIT_VALUE_DISTANCE_SMALL;

            _spinnerBreakShortestTime.setSelection(prefShortestTime);
            _spinnerBreakMaxDistance.setSelection((int) (breakDistance + 0.5));
            _spinnerBreakSliceDiff.setSelection(prefSliceDiff);

            updateUIShowSelectedBreakTimeMethod();
         }
         _isUpdateUI = false;

         // keep state and fire event
         onModifyBreakTime();
      } else if (selectedTab == TAB_FOLDER_CADENCE_ZONES) {

         final int cadenceZonesDelimiterValue = _prefStore.getDefaultInt(ITourbookPreferences.CADENCE_ZONES_DELIMITER);
         _spinnerCadenceDelimiter.setSelection(cadenceZonesDelimiterValue);

      }

      super.performDefaults();
   }

   @Override
   public boolean performOk() {

      saveState();

      return super.performOk();
   }

   private void restoreState() {

      _isUpdateUI = true;
      {
         /*
          * DP tolerance
          */
         final float prefDPTolerance = _prefStore.getFloat(ITourbookPreferences.COMPUTED_ALTITUDE_DP_TOLERANCE) * 10 / UI.UNIT_VALUE_ALTITUDE;
         _spinnerDPTolerance.setSelection((int) prefDPTolerance);

         /*
          * break method
          */
         final String prefBreakTimeMethod = _prefStore.getString(ITourbookPreferences.BREAK_TIME_METHOD2);
         selectBreakMethod(prefBreakTimeMethod);

         /*
          * break by avg+slice speed
          */
         final float prefMinAvgSpeedAS = _prefStore.getFloat(ITourbookPreferences.BREAK_TIME_MIN_AVG_SPEED_AS);
         final float prefMinSliceSpeedAS = _prefStore.getFloat(ITourbookPreferences.BREAK_TIME_MIN_SLICE_SPEED_AS);
         final int prefMinSliceTimeAS = _prefStore.getInt(ITourbookPreferences.BREAK_TIME_MIN_SLICE_TIME_AS);
         _spinnerBreakMinAvgSpeedAS.setSelection(//
               (int) (prefMinAvgSpeedAS * SPEED_DIGIT_VALUE / UI.UNIT_VALUE_DISTANCE));
         _spinnerBreakMinSliceSpeedAS.setSelection(//
               (int) (prefMinSliceSpeedAS * SPEED_DIGIT_VALUE / UI.UNIT_VALUE_DISTANCE));
         _spinnerBreakMinSliceTimeAS.setSelection(prefMinSliceTimeAS);

         /*
          * break by speed
          */
         final float prefMinSliceSpeed = _prefStore.getFloat(ITourbookPreferences.BREAK_TIME_MIN_SLICE_SPEED);
         final float prefMinAvgSpeed = _prefStore.getFloat(ITourbookPreferences.BREAK_TIME_MIN_AVG_SPEED);
         _spinnerBreakMinSliceSpeed.setSelection(//
               (int) (prefMinSliceSpeed * SPEED_DIGIT_VALUE * UI.UNIT_VALUE_DISTANCE));
         _spinnerBreakMinAvgSpeed.setSelection(//
               (int) (prefMinAvgSpeed * SPEED_DIGIT_VALUE * UI.UNIT_VALUE_DISTANCE));

         /*
          * break time by time/distance
          */
         final int prefShortestTime = _prefStore.getInt(ITourbookPreferences.BREAK_TIME_SHORTEST_TIME);
         final float prefMaxDistance = _prefStore.getFloat(ITourbookPreferences.BREAK_TIME_MAX_DISTANCE);
         final int prefSliceDiff = _prefStore.getInt(ITourbookPreferences.BREAK_TIME_SLICE_DIFF);
         final float breakDistance = prefMaxDistance / UI.UNIT_VALUE_DISTANCE_SMALL;
         _spinnerBreakShortestTime.setSelection(prefShortestTime);
         _spinnerBreakMaxDistance.setSelection((int) (breakDistance + 0.5));
         _spinnerBreakSliceDiff.setSelection(prefSliceDiff);

         /*
          * folder
          */
         _tabFolder.setSelection(_prefStore.getInt(STATE_COMPUTED_VALUE_SELECTED_TAB));

         /*
          * Cadence zones delimiter
          */
         _spinnerCadenceDelimiter.setSelection(_prefStore.getInt(ITourbookPreferences.CADENCE_ZONES_DELIMITER));

         updateUIShowSelectedBreakTimeMethod();
      }

      _isUpdateUI = false;
   }

   /**
    * save values in the pref store
    */
   private void saveState() {

      //If the saveState() was triggered by the change of measurement system,
      //we don't save the values as they were already saved and it would convert
      //those values by error
      if (INITIAL_UNIT_IS_METRIC != net.tourbook.common.UI.UNIT_IS_METRIC) {
         return;
      }

      // DP tolerance when computing altitude up/down
      _prefStore.setValue(
            ITourbookPreferences.COMPUTED_ALTITUDE_DP_TOLERANCE,
            _spinnerDPTolerance.getSelection() / 10.0f * UI.UNIT_VALUE_ALTITUDE);

      /*
       * break time method
       */
      _prefStore.setValue(ITourbookPreferences.BREAK_TIME_METHOD2, getSelectedBreakMethod().methodId);

      /*
       * break by average+slice speed
       */
      final float breakMinAvgSpeedAS = _spinnerBreakMinAvgSpeedAS.getSelection()
            / SPEED_DIGIT_VALUE
            * UI.UNIT_VALUE_DISTANCE;
      final float breakMinSliceSpeedAS = _spinnerBreakMinSliceSpeedAS.getSelection()
            / SPEED_DIGIT_VALUE
            * UI.UNIT_VALUE_DISTANCE;
      final int breakMinSliceTimeAS = _spinnerBreakMinSliceTimeAS.getSelection();

      _prefStore.setValue(ITourbookPreferences.BREAK_TIME_MIN_AVG_SPEED_AS, breakMinAvgSpeedAS);
      _prefStore.setValue(ITourbookPreferences.BREAK_TIME_MIN_SLICE_SPEED_AS, breakMinSliceSpeedAS);
      _prefStore.setValue(ITourbookPreferences.BREAK_TIME_MIN_SLICE_TIME_AS, breakMinSliceTimeAS);

      /*
       * break by slice speed
       */
      final float breakMinSliceSpeed = _spinnerBreakMinSliceSpeed.getSelection()
            / SPEED_DIGIT_VALUE
            / UI.UNIT_VALUE_DISTANCE;
      _prefStore.setValue(ITourbookPreferences.BREAK_TIME_MIN_SLICE_SPEED, breakMinSliceSpeed);

      /*
       * break by avg speed
       */
      final float breakMinAvgSpeed = _spinnerBreakMinAvgSpeed.getSelection()
            / SPEED_DIGIT_VALUE
            / UI.UNIT_VALUE_DISTANCE;
      _prefStore.setValue(ITourbookPreferences.BREAK_TIME_MIN_AVG_SPEED, breakMinAvgSpeed);

      /*
       * break by time/distance
       */
      _prefStore.setValue(ITourbookPreferences.BREAK_TIME_SHORTEST_TIME, _spinnerBreakShortestTime.getSelection());

      final float breakDistance = _spinnerBreakMaxDistance.getSelection() * UI.UNIT_VALUE_DISTANCE_SMALL;
      _prefStore.setValue(ITourbookPreferences.BREAK_TIME_MAX_DISTANCE, breakDistance);

      _prefStore.setValue(ITourbookPreferences.BREAK_TIME_SLICE_DIFF, _spinnerBreakSliceDiff.getSelection());

      /*
       * notify break time listener
       */
      _prefStore.setValue(ITourbookPreferences.BREAK_TIME_IS_MODIFIED, Math.random());

      /*
       * Cadence delimiter value
       */
      _prefStore.setValue(ITourbookPreferences.CADENCE_ZONES_DELIMITER, _spinnerCadenceDelimiter.getSelection());
   }

   private void saveUIState() {

      if (_tabFolder == null || _tabFolder.isDisposed()) {
         return;
      }

      _prefStore.setValue(STATE_COMPUTED_VALUE_SELECTED_TAB, _tabFolder.getSelectionIndex());
   }

   private void selectBreakMethod(final String methodId) {

      final BreakTimeMethod[] breakMethods = BreakTimeTool.BREAK_TIME_METHODS;

      int selectionIndex = -1;

      for (int methodIndex = 0; methodIndex < breakMethods.length; methodIndex++) {
         if (breakMethods[methodIndex].methodId.equals(methodId)) {
            selectionIndex = methodIndex;
            break;
         }
      }

      if (selectionIndex == -1) {
         selectionIndex = 0;
      }

      _comboBreakMethod.select(selectionIndex);
   }

   private void updateUIShowSelectedBreakTimeMethod() {

      final BreakTimeMethod selectedBreakMethod = getSelectedBreakMethod();

      if (selectedBreakMethod.methodId.equals(BreakTimeTool.BREAK_TIME_METHOD_BY_AVG_SPEED)) {

         _pagebookBreakTime.showPage(_pageBreakByAvgSpeed);

      } else if (selectedBreakMethod.methodId.equals(BreakTimeTool.BREAK_TIME_METHOD_BY_SLICE_SPEED)) {

         _pagebookBreakTime.showPage(_pageBreakBySliceSpeed);

      } else if (selectedBreakMethod.methodId.equals(BreakTimeTool.BREAK_TIME_METHOD_BY_AVG_SLICE_SPEED)) {

         _pagebookBreakTime.showPage(_pageBreakByAvgSliceSpeed);

      } else if (selectedBreakMethod.methodId.equals(BreakTimeTool.BREAK_TIME_METHOD_BY_TIME_DISTANCE)) {

         _pagebookBreakTime.showPage(_pageBreakByTimeDistance);
      }

      // break method pages have different heights, enforce layout of the whole view part
      _tabFolder.layout(true, true);

      net.tourbook.common.UI.updateScrolledContent(_tabFolder);
   }

}
