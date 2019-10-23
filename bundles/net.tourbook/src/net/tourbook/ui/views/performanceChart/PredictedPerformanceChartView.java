/*******************************************************************************
 * Copyright (C) 2019 Frédéric Bard
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
package net.tourbook.ui.views.performanceChart;

import java.text.NumberFormat;
import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartType;
import net.tourbook.chart.IBarSelectionListener;
import net.tourbook.chart.MinMaxKeeper_YData;
import net.tourbook.common.tooltip.ActionToolbarSlideout;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.Util;
import net.tourbook.data.HrZoneContext;
import net.tourbook.data.TourData;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourPersonHRZone;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPagePeople;
import net.tourbook.preferences.PrefPagePeopleData;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.training.TrainingManager;
import net.tourbook.ui.UI;
import net.tourbook.ui.views.tourCatalog.SelectionTourCatalogView;
import net.tourbook.ui.views.tourCatalog.TVICatalogComparedTour;
import net.tourbook.ui.views.tourCatalog.TVICatalogRefTourItem;
import net.tourbook.ui.views.tourCatalog.TVICompareResultComparedTour;

import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

public class PredictedPerformanceChartView extends ViewPart {

   public static final String  ID                                    = "net.tourbook.ui.views.performanceChart.PredictedPerformanceChartView"; //$NON-NLS-1$

   private static final int    HR_LEFT_MIN_BORDER                    = 0;
   private static final int    HR_RIGHT_MAX_BORDER                   = 230;

   private static final String STATE_HR_CHART_LEFT_BORDER            = "HrLeftChartBorder";                                                    //$NON-NLS-1$
   private static final String STATE_HR_CHART_RIGHT_BORDER           = "HrRightChartBorder";                                                   //$NON-NLS-1$
   private static final String STATE_IS_SHOW_ALL_STRESS_SCORE_VALUES = "IsShowAllStressScoreValues";                                           //$NON-NLS-1$
   private static final String STATE_IS_SYNC_VERTICAL_CHART_SCALING  = "IsSyncVerticalChartScaling";                                           //$NON-NLS-1$

   private static final String GRID_PREF_PREFIX                      = "GRID_TRAINING__";                                                      //$NON-NLS-1$

// SET_FORMATTING_OFF

   private static final String         GRID_IS_SHOW_VERTICAL_GRIDLINES     = (GRID_PREF_PREFIX + ITourbookPreferences.CHART_GRID_IS_SHOW_VERTICAL_GRIDLINES);
   private static final String         GRID_IS_SHOW_HORIZONTAL_GRIDLINES   = (GRID_PREF_PREFIX + ITourbookPreferences.CHART_GRID_IS_SHOW_HORIZONTAL_GRIDLINES);
   private static final String         GRID_VERTICAL_DISTANCE              = (GRID_PREF_PREFIX + ITourbookPreferences.CHART_GRID_VERTICAL_DISTANCE);
   private static final String         GRID_HORIZONTAL_DISTANCE            = (GRID_PREF_PREFIX + ITourbookPreferences.CHART_GRID_HORIZONTAL_DISTANCE);

// SET_FORMATTING_ON

   private final IPreferenceStore         _prefStore     = TourbookPlugin.getPrefStore();
   private final IDialogSettings          _state         = TourbookPlugin.getState(ID);

   private IPartListener2                 _partListener;
   private ISelectionListener             _postSelectionListener;
   private IPropertyChangeListener        _prefChangeListener;
   private ITourEventListener             _tourEventListener;

   private ModifyListener                 _defaultSpinnerModifyListener;
   private SelectionAdapter               _defaultSpinnerSelectionListener;
   private MouseWheelListener             _defaultSpinnerMouseWheelListener;

   private TourPerson                     _currentPerson;
   private TourData                       _tourData;

   private boolean                        _isUpdateUI;
   private boolean                        _isShowAllValues;
   private boolean                        _isSynchChartVerticalValues;

   private ToolBarManager                 _headerToolbarManager;

   private ActionShowAllStressScoreValues _actionShowAllStressScoreValues;
   private ActionSynchChartScale          _actionSynchVerticalChartScaling;
   private ActionTrainingOptions          _actionTrainingOptions;

   private double[]                       _xSeriePulse;

   private ArrayList<TourPersonHRZone>    _personHrZones = new ArrayList<>();
   private final MinMaxKeeper_YData       _minMaxKeeper  = new MinMaxKeeper_YData();

   private final NumberFormat             _nf1           = NumberFormat.getNumberInstance();
   {
      _nf1.setMinimumFractionDigits(1);
      _nf1.setMaximumFractionDigits(1);
   }

   /*
    * UI controls/resources
    */
   private FormToolkit _tk;

   /*
    * Pagebook for the training view
    */
   private PageBook  _pageBook;
   private Composite _page_HrZones;
   private Composite _page_NoTour;
   private Composite _page_NoPerson;

   private Composite _toolbar;
   private Chart     _chartHrTime;

   private Spinner   _spinnerHrLeft;
   private Spinner   _spinnerHrRight;
   private Label     _lblHrMin;
   private Label     _lblHrMax;

   private Composite _hrZoneDataContainerContent;

   private Label[]   _lblTourMinMaxValue;
   private Label[]   _lblTourMinMaxHours;
   private Label[]   _lblHRZoneName;
   private Label[]   _lblHRZoneColor;
   private Label[]   _lblHrZonePercent;

   /*
    * none UI
    */

   private class ActionTrainingOptions extends ActionToolbarSlideout {

      @Override
      protected ToolbarSlideout createSlideout(final ToolBar toolbar) {

         return new SlideoutTrainingOptions(_pageBook, toolbar, GRID_PREF_PREFIX, PredictedPerformanceChartView.this);
      }
   }

   public PredictedPerformanceChartView() {}

   void actionEditHrZones() {

      final TourPerson person = _currentPerson != null ? _currentPerson : TourbookPlugin.getActivePerson();

      PreferencesUtil.createPreferenceDialogOn(
            _pageBook.getShell(),
            PrefPagePeople.ID,
            null,
            new PrefPagePeopleData(PrefPagePeople.PREF_DATA_SELECT_HR_ZONES, person)//
      )
            .open();
   }

   void actionShowAllStressScoreValues() {

      _isShowAllValues = _actionShowAllStressScoreValues.isChecked();

      updateUI_30_HrZonesFromModel();
   }

   void actionSynchChartScale() {

      _isSynchChartVerticalValues = _actionSynchVerticalChartScaling.isChecked();

      if (_isSynchChartVerticalValues == false) {
         _minMaxKeeper.resetMinMax();
      }

      updateUI_30_HrZonesFromModel();
   }

   private void addPartListener() {

      getViewSite().getPage().addPartListener(_partListener = new IPartListener2() {

         @Override
         public void partActivated(final IWorkbenchPartReference partRef) {}

         @Override
         public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

         @Override
         public void partClosed(final IWorkbenchPartReference partRef) {}

         @Override
         public void partDeactivated(final IWorkbenchPartReference partRef) {}

         @Override
         public void partHidden(final IWorkbenchPartReference partRef) {}

         @Override
         public void partInputChanged(final IWorkbenchPartReference partRef) {}

         @Override
         public void partOpened(final IWorkbenchPartReference partRef) {}

         @Override
         public void partVisible(final IWorkbenchPartReference partRef) {}
      });
   }

   private void addPrefListener() {

      _prefChangeListener = new IPropertyChangeListener() {
         @Override
         public void propertyChange(final PropertyChangeEvent event) {

            final String property = event.getProperty();

            /*
             * set a new chart configuration when the preferences has changed
             */
            if (property.equals(ITourbookPreferences.TOUR_PERSON_LIST_IS_MODIFIED)
                  || property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)) {

               onModifyPerson();

            } else if (property.equals(GRID_HORIZONTAL_DISTANCE)
                  || property.equals(GRID_VERTICAL_DISTANCE)
                  || property.equals(GRID_IS_SHOW_HORIZONTAL_GRIDLINES)
                  || property.equals(GRID_IS_SHOW_VERTICAL_GRIDLINES)
            //
            ) {

               setChartProperties();

               // grid has changed, update chart
               updateUI_30_HrZonesFromModel();
            }
         }
      };

      _prefStore.addPropertyChangeListener(_prefChangeListener);
   }

   /**
    * listen for events when a tour is selected
    */
   private void addSelectionListener() {

      _postSelectionListener = new ISelectionListener() {
         @Override
         public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

            if (part == PredictedPerformanceChartView.this) {
               return;
            }

            onSelectionChanged(selection);
         }
      };
      getSite().getPage().addPostSelectionListener(_postSelectionListener);
   }

   private void addTourEventListener() {

      _tourEventListener = new ITourEventListener() {

         @Override
         public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

            if (part == PredictedPerformanceChartView.this) {
               return;
            }

            if (eventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

               clearView();

            } else if (eventId == TourEventId.TOUR_SELECTION && eventData instanceof ISelection) {

               onSelectionChanged((ISelection) eventData);

            } else if ((eventId == TourEventId.TOUR_CHANGED) && (eventData instanceof TourEvent)) {

               final ArrayList<TourData> modifiedTours = ((TourEvent) eventData).getModifiedTours();

               if ((modifiedTours != null) && (modifiedTours.size() > 0)) {
                  updateUI_20(modifiedTours.get(0));
               }
            } else if (eventId == TourEventId.TOUR_CHART_PROPERTY_IS_MODIFIED) {

               if (_tourData != null) {

                  _tourData.clearComputedSeries();

                  updateUI_20(_tourData);
               }
            }
         }
      };

      TourManager.getInstance().addTourEventListener(_tourEventListener);
   }

   private void clearView() {

      _tourData = null;
      _currentPerson = null;

//      _tourChart.updateChart(null, false);

      _pageBook.showPage(_page_NoTour);
      enableControls();
   }

   private void createActions() {

      _actionShowAllStressScoreValues = new ActionShowAllStressScoreValues(this);
      _actionSynchVerticalChartScaling = new ActionSynchChartScale(this);
      _actionTrainingOptions = new ActionTrainingOptions();
   }

   @Override
   public void createPartControl(final Composite parent) {

      createUI(parent);

      createActions();
      fillToolbar();

      // show default page
      _pageBook.showPage(_page_NoTour);

      addSelectionListener();
      addPrefListener();
      addTourEventListener();
      addPartListener();

      restoreState();

      showTour();
   }

   private void createUI(final Composite parent) {

      initUI(parent);

      final Composite container = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults()//
            .spacing(0, 0)
            .applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
      {
         createUI_10_Toolbar(container);
         createUI_12_PageBook(container);
      }
   }

   private void createUI_10_Toolbar(final Composite parent) {

      _toolbar = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()//
            .grab(true, false)
            .align(SWT.BEGINNING, SWT.FILL)
            .applyTo(_toolbar);
      GridLayoutFactory.fillDefaults()//
            .numColumns(9)
            .margins(3, 3)
            .applyTo(_toolbar);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
      {
         /*
          * label: hr min
          */
         _lblHrMin = new Label(_toolbar, SWT.NONE);
         GridDataFactory.fillDefaults()//
//               .indent(5, 0)
               .align(SWT.BEGINNING, SWT.CENTER)
               .applyTo(_lblHrMin);
         _lblHrMin.setText(Messages.Training_View_Label_LeftChartBorder);
         _lblHrMin.setToolTipText(Messages.Training_View_Label_LeftChartBorder_Tooltip);

         /*
          * spinner: hr min
          */
         _spinnerHrLeft = new Spinner(_toolbar, SWT.BORDER);
         _spinnerHrLeft.setMinimum(HR_LEFT_MIN_BORDER);
         _spinnerHrLeft.setMaximum(HR_RIGHT_MAX_BORDER);
         _spinnerHrLeft.addModifyListener(_defaultSpinnerModifyListener);
         _spinnerHrLeft.addSelectionListener(_defaultSpinnerSelectionListener);
         _spinnerHrLeft.addMouseWheelListener(_defaultSpinnerMouseWheelListener);

         /*
          * label: hr max
          */
         _lblHrMax = new Label(_toolbar, SWT.NONE);
         GridDataFactory.fillDefaults()//
               .align(SWT.BEGINNING, SWT.CENTER)
               .applyTo(_lblHrMax);
         _lblHrMax.setText(Messages.Training_View_Label_RightChartBorder);
         _lblHrMax.setToolTipText(Messages.Training_View_Label_RightChartBorder_Tooltip);

         /*
          * spinner: hr max
          */
         _spinnerHrRight = new Spinner(_toolbar, SWT.BORDER);
         _spinnerHrRight.setMinimum(HR_LEFT_MIN_BORDER);
         _spinnerHrRight.setMaximum(HR_RIGHT_MAX_BORDER);
         _spinnerHrRight.addModifyListener(_defaultSpinnerModifyListener);
         _spinnerHrRight.addSelectionListener(_defaultSpinnerSelectionListener);
         _spinnerHrRight.addMouseWheelListener(_defaultSpinnerMouseWheelListener);

         // label: vertical separator
         final Label label = new Label(_toolbar, SWT.SEPARATOR | SWT.VERTICAL);
         label.setText(UI.EMPTY_STRING);

         /*
          * toolbar actions
          */
         final ToolBar toolbar = new ToolBar(_toolbar, SWT.FLAT);
         GridDataFactory.fillDefaults()//
               .align(SWT.BEGINNING, SWT.CENTER)
               .applyTo(toolbar);
         _headerToolbarManager = new ToolBarManager(toolbar);

      }


   }

   private void createUI_12_PageBook(final Composite parent) {

      _pageBook = new PageBook(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_pageBook);

      _page_HrZones = createUI_20_PageHrZones(_pageBook);

      _page_NoPerson = UI.createPage(_tk, _pageBook, Messages.UI_Label_PersonIsRequired);
      _page_NoTour = UI.createPage(_tk, _pageBook, Messages.UI_Label_no_chart_is_selected);
   }

   private Composite createUI_20_PageHrZones(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()//
//            .grab(true, true)
            .applyTo(container);
      GridLayoutFactory.fillDefaults()//
            .numColumns(3)
            .spacing(0, 0)
            .applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
      container.setBackground(_tk.getColors().getBackground());
      {
         // feature request to show the zone image first https://sourceforge.net/p/mytourbook/feature-requests/132/
         createUI_30_HrZoneChart(container);
      }

      return container;
   }

   private void createUI_30_HrZoneChart(final Composite parent) {

      /*
       * chart
       */
      _chartHrTime = new Chart(parent, SWT.FLAT);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_chartHrTime);

      setChartProperties();

      _chartHrTime.addBarSelectionListener(new IBarSelectionListener() {
         @Override
         public void selectionChanged(final int serieIndex, final int valueIndex) {

//               _postSelectionProvider.setSelection(selection);
         }
      });
   }
   @Override
   public void dispose() {

      if (_tk != null) {
         _tk.dispose();
      }

      getSite().getPage().removePostSelectionListener(_postSelectionListener);

      // an NPE occured when the part could not be created
      if (_partListener != null) {
         getViewSite().getPage().removePartListener(_partListener);
      }

      TourManager.getInstance().removeTourEventListener(_tourEventListener);

      _prefStore.removePropertyChangeListener(_prefChangeListener);

      super.dispose();
   }

   private void enableControls() {

      final boolean isHrZoneAvailable = TrainingManager.isRequiredHrZoneDataAvailable(_tourData);
      final boolean isCustomScaling = isHrZoneAvailable && _isShowAllValues == false;

//      _comboTrainingChart.setEnabled(canShowHrZones);

      _spinnerHrLeft.setEnabled(isCustomScaling);
      _spinnerHrRight.setEnabled(isCustomScaling);
      _lblHrMin.setEnabled(isCustomScaling);
      _lblHrMax.setEnabled(isCustomScaling);

      _actionSynchVerticalChartScaling.setEnabled(isCustomScaling);
      _actionShowAllStressScoreValues.setEnabled(isHrZoneAvailable);
      _actionTrainingOptions.setEnabled(isHrZoneAvailable);
   }

   private void fillToolbar() {

      /*
       * View toolbar
       */
      /*
       * final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();
       * tbm.add(_actionTrainingOptions);
       * // update toolbar which creates the slideout
       * tbm.update(true);
       */
      /*
       * Header toolbar
       */
      _headerToolbarManager.add(_actionShowAllStressScoreValues);
      _headerToolbarManager.add(_actionSynchVerticalChartScaling);

      _headerToolbarManager.update(true);
   }

   private void initUI(final Composite parent) {

      _tk = new FormToolkit(parent.getDisplay());

      _defaultSpinnerModifyListener = new ModifyListener() {
         @Override
         public void modifyText(final ModifyEvent e) {
            if (_isUpdateUI) {
               return;
            }
            onModifyHrBorder();
         }
      };

      _defaultSpinnerSelectionListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            if (_isUpdateUI) {
               return;
            }
            onModifyHrBorder();
         }
      };

      _defaultSpinnerMouseWheelListener = new MouseWheelListener() {
         @Override
         public void mouseScrolled(final MouseEvent event) {
            Util.adjustSpinnerValueOnMouseScroll(event);
            if (_isUpdateUI) {
               return;
            }
            onModifyHrBorder();
         }
      };

   }

   private void onModifyHrBorder() {

      int left = _spinnerHrLeft.getSelection();
      int right = _spinnerHrRight.getSelection();

      _isUpdateUI = true;
      {
         if (left == HR_LEFT_MIN_BORDER && right == HR_LEFT_MIN_BORDER) {

            right++;
            _spinnerHrRight.setSelection(right);

         } else if (left == HR_RIGHT_MAX_BORDER && right == HR_RIGHT_MAX_BORDER) {

            left--;
            _spinnerHrLeft.setSelection(left);

         } else if (left >= right) {

            left = right - 1;
            _spinnerHrLeft.setSelection(left);
         }
      }
      _isUpdateUI = false;

      updateUI_30_HrZonesFromModel();
   }

   /**
    * Person and/or hr zones are modified
    */
   private void onModifyPerson() {

      clearView();
      showTour();
   }

   private void onSelectionChanged(final ISelection selection) {

      if (selection instanceof SelectionTourData) {

         final TourData selectionTourData = ((SelectionTourData) selection).getTourData();
         if (selectionTourData != null) {

            // prevent loading the same tour
            if (_tourData != null && _tourData.equals(selectionTourData)) {
               return;
            }

            updateUI_20(selectionTourData);
         }

      } else if (selection instanceof SelectionTourIds) {

         final SelectionTourIds selectionTourId = (SelectionTourIds) selection;
         final ArrayList<Long> tourIds = selectionTourId.getTourIds();
         if (tourIds != null && tourIds.size() > 0) {
            updateUI(tourIds.get(0));
         }

      } else if (selection instanceof SelectionTourId) {

         final SelectionTourId selectionTourId = (SelectionTourId) selection;
         final Long tourId = selectionTourId.getTourId();

         updateUI(tourId);

      } else if (selection instanceof StructuredSelection) {

         final Object firstElement = ((StructuredSelection) selection).getFirstElement();
         if (firstElement instanceof TVICatalogComparedTour) {

            updateUI(((TVICatalogComparedTour) firstElement).getTourId());

         } else if (firstElement instanceof TVICompareResultComparedTour) {

            final TVICompareResultComparedTour compareResultItem = (TVICompareResultComparedTour) firstElement;
            final TourData tourData = TourManager.getInstance().getTourData(compareResultItem.getComparedTourData().getTourId());
            updateUI_20(tourData);
         }

      } else if (selection instanceof SelectionTourCatalogView) {

         final SelectionTourCatalogView tourCatalogSelection = (SelectionTourCatalogView) selection;

         final TVICatalogRefTourItem refItem = tourCatalogSelection.getRefItem();
         if (refItem != null) {
            updateUI(refItem.getTourId());
         }

      } else if (selection instanceof SelectionDeletedTours) {

         clearView();
      }
   }

   private void restoreState() {

      _isShowAllValues = Util.getStateBoolean(_state, STATE_IS_SHOW_ALL_STRESS_SCORE_VALUES, false);
      _actionShowAllStressScoreValues.setChecked(_isShowAllValues);

      _isSynchChartVerticalValues = Util.getStateBoolean(_state, STATE_IS_SYNC_VERTICAL_CHART_SCALING, false);
      _actionSynchVerticalChartScaling.setChecked(_isSynchChartVerticalValues);

      _isUpdateUI = true;
      {
         _spinnerHrLeft.setSelection(Util.getStateInt(_state, STATE_HR_CHART_LEFT_BORDER, 60));
         _spinnerHrRight.setSelection(Util.getStateInt(_state, STATE_HR_CHART_RIGHT_BORDER, 200));
      }
      _isUpdateUI = false;
   }

   @PersistState
   private void saveState() {

      _state.put(STATE_HR_CHART_LEFT_BORDER, _spinnerHrLeft.getSelection());
      _state.put(STATE_HR_CHART_RIGHT_BORDER, _spinnerHrRight.getSelection());

      _state.put(STATE_IS_SHOW_ALL_STRESS_SCORE_VALUES, _actionShowAllStressScoreValues.isChecked());
      _state.put(STATE_IS_SYNC_VERTICAL_CHART_SCALING, _actionSynchVerticalChartScaling.isChecked());
   }

   private void setChartProperties() {

      UI.updateChartProperties(_chartHrTime, GRID_PREF_PREFIX);

      // show title
      _chartHrTime.getChartTitleSegmentConfig().isShowSegmentTitle = true;
   }

   @Override
   public void setFocus() {

   }

   private void showTour() {

      onSelectionChanged(getSite().getWorkbenchWindow().getSelectionService().getSelection());

      if (_tourData == null) {
         showTourFromTourProvider();
      }

      enableControls();
   }

   private void showTourFromTourProvider() {

      // a tour is not displayed, find a tour provider which provides a tour
      Display.getCurrent().asyncExec(new Runnable() {
         @Override
         public void run() {

            // validate widget
            if (_pageBook.isDisposed()) {
               return;
            }

            /*
             * check if tour was set from a selection provider
             */
            if (_tourData != null) {
               return;
            }

            final ArrayList<TourData> selectedTours = TourManager.getSelectedTours();
            if (selectedTours != null && selectedTours.size() > 0) {
               updateUI_20(selectedTours.get(0));
            }
         }
      });
   }

   private void updateUI(final long tourId) {

      if (_tourData != null && _tourData.getTourId() == tourId) {
         // optimize
         return;
      }

      //TODO Get all tours for current user for which are the types that the stress score was selected
      //==> create SQL statement ? find another location in the code that does the same
      updateUI_20(TourManager.getInstance().getTourData(tourId));
   }

   private void updateUI_20(final TourData tourData) {

      if (tourData == null) {
         // nothing to do
         return;
      }

      _tourData = tourData;

      final TourPerson tourPerson = tourData.getTourPerson();
      if (tourPerson != _currentPerson) {

         /*
          * another person is contained in the tour, dispose resources which depends on the current
          * person
          */
         if (_hrZoneDataContainerContent != null) {
            _hrZoneDataContainerContent.dispose();
         }

         _currentPerson = tourPerson;
      }

      updateUI_30_HrZonesFromModel();
   }

   /**
    * Displays training data when a tour is available
    */
   private void updateUI_30_HrZonesFromModel() {

      enableControls();

      /*
       * check person
       */
      if (_currentPerson == null) {

         // selected tour do not contain a person
         _pageBook.showPage(_page_NoPerson);

         return;
      }

      /*
       * required data are available
       */

      // display page for the selected chart
      _pageBook.showPage(_page_HrZones);

      final HrZoneContext zoneMinMaxBpm = _currentPerson.getHrZoneContext(
            _currentPerson.getHrMaxFormula(),
            _currentPerson.getMaxPulse(),
            _currentPerson.getBirthDayWithDefault(),
            _tourData.getTourStartTime());

      updateUI_40_HrZoneChart(zoneMinMaxBpm);
      updateUI_42_HrZoneData(zoneMinMaxBpm);

   }

   private void updateUI_40_HrZoneChart(final HrZoneContext zoneMinMaxBpm) {

      final float[] pulseSerie = new float[_tourData.timeSerie.length];
      _tourData.getPulseSmoothedSerie();
      final int[] timeSerie = _tourData.timeSerie;
      final boolean[] breakTimeSerie = _tourData.getBreakTimeSerie();
      final int timeSerieSize = timeSerie.length;

      final ArrayList<TourPersonHRZone> hrSortedZones = _currentPerson.getHrZonesSorted();
      final int zoneSize = hrSortedZones.size();

      final RGB[] rgbBright = new RGB[zoneSize];
      final RGB[] rgbDark = new RGB[zoneSize];
      final RGB[] rgbLine = new RGB[zoneSize];

      int zoneIndex = 0;

      for (final TourPersonHRZone hrZone : hrSortedZones) {

         rgbDark[zoneIndex] = hrZone.getColor();
         rgbBright[zoneIndex] = hrZone.getColorBright();
         rgbLine[zoneIndex] = hrZone.getColorDark();

         zoneIndex++;
      }

      /*
       * Get min/max values
       */
      float pulseMin;
      float pulseMax;

      if (_isShowAllValues) {

         pulseMin = pulseMax = pulseSerie[0];

         for (final float pulse : pulseSerie) {
            pulseMin = _tourData.getGovss();
         }

      } else {
         pulseMin = _tourData.getGovss();
         pulseMax = _tourData.getGovss();
      }

      /*
       * create x-data series
       */
      final int pulseRange = 360;

      _xSeriePulse = new double[pulseRange];
      final float[] ySeriePulseDuration = new float[pulseRange];

      final int[] colorIndex = new int[pulseRange];

      final float[] zoneMinBpm = new float[zoneSize];
      zoneMinBpm[0] = 0;//zoneMinMaxBpm.zoneMinBpm;
      final float[] zoneMaxBpm = new float[zoneSize];//zoneMinMaxBpm.zoneMaxBpm;
      zoneMaxBpm[0] = 200;

      for (int pulseIndex = 0; pulseIndex < pulseRange; pulseIndex++) {

         _xSeriePulse[pulseIndex] = pulseIndex;

         // set color index for each pulse value
         for (zoneIndex = 0; zoneIndex < zoneSize; zoneIndex++) {

            final float minValue = zoneMinBpm[zoneIndex];
            final float maxValue = zoneMaxBpm[zoneIndex];

            final double pulse = pulseMin + pulseIndex;

            if (pulse >= minValue && pulse <= maxValue) {

               // pulse is in the current zone

               colorIndex[pulseIndex] = zoneIndex;

               break;
            }
         }
      }

      int prevTime = 0;

      /*
       * create y-data serie: get time/color for each pulse value
       */
      for (int serieIndex = 0; serieIndex < timeSerieSize; serieIndex++) {

         // get time for each pulse value
         final int currentTime = timeSerie[serieIndex];
         final int timeDiff = currentTime - prevTime;
         prevTime = currentTime;

         // check if time is in a break
         if (breakTimeSerie != null) {

            /*
             * break time requires distance data, so it's possible that break time data are not
             * available
             */

            if (breakTimeSerie[serieIndex] == true) {
               // pulse time is not set within a break
               continue;
            }
         }

         final float pulse = pulseSerie[serieIndex];
         final int pulseIndex = (int) (pulse - pulseMin);

         // check array bounds
         if (pulseIndex >= 0 && pulseIndex < pulseRange) {
            ySeriePulseDuration[pulseIndex] += timeDiff;
         }
      }

      final ChartDataModel chartDataModel = new ChartDataModel(ChartType.BAR);

//      chartDataModel.setTitle(TourManager.getTourDateTimeFull(_tourData));

      /*
       * x-axis: pulse
       */
      final ChartDataXSerie xData = new ChartDataXSerie(_xSeriePulse);
      xData.setAxisUnit(ChartDataXSerie.X_AXIS_UNIT_NUMBER_CENTER);
      xData.setUnitLabel("date");//net.tourbook.common.Messages.Graph_Label_Heartbeat_Unit);
      xData.setUnitStartValue(pulseMin);

      chartDataModel.setXData(xData);

      /*
       * y-axis: time
       */
      final float[][] yvalues = new float[pulseRange][1];
      for (int toto = 0; toto < pulseRange; ++toto)
      {
         for (int titi = 0; titi < 1; ++titi)
         {
            yvalues[toto][titi] = 20;
         }
      }

      final ChartDataYSerie yData = new ChartDataYSerie(
            ChartType.LINE,
            yvalues);

      yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
      yData.setYTitle("GOVSS");//Messages.App_Label_H_MM);

      yData.setColorIndex(new int[][] { colorIndex });
      yData.setRgbLine(rgbLine);
      yData.setRgbBright(rgbBright);
      yData.setRgbDark(rgbDark);
      yData.setDefaultRGB(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY).getRGB());

      chartDataModel.addYData(yData);

      /*
       * if (_isSynchChartVerticalValues && _isShowAllValues == false) {
       * _minMaxKeeper.setMinMaxValues(chartDataModel);
       * }
       */
      // show the new data data model in the chart
      _chartHrTime.updateChart(chartDataModel, false);
   }

   /**
    * @param zoneContext
    *           Contains age and HR max values.
    */
   private void updateUI_42_HrZoneData(final HrZoneContext zoneContext) {

      final int personZoneSize = _personHrZones.size();
      final int[] tourHrZoneTimes = _tourData.getHrZones();
      final long drivingTime = _tourData.getTourDrivingTime();

      final int tourHrZoneSize = 0;//tourHrZoneTimes.length;

      for (int tourZoneIndex = 0; tourZoneIndex < tourHrZoneSize; tourZoneIndex++) {

         if (tourZoneIndex >= personZoneSize) {

            /*
             * when zone data in the tours are not consistent, number of zones in the tour and in
             * the person can be different
             */
            break;
         }

         final double zoneTime = tourHrZoneTimes[tourZoneIndex];
         final double zoneTimePercent = drivingTime == 0 //
               ? 0
               : zoneTime * 100.0 / drivingTime;

         if (zoneTime == -1) {
            // this zone and following zones are not available
            break;
         }

         final TourPersonHRZone hrZone = _personHrZones.get(tourZoneIndex);

         final int zoneMaxValue = hrZone.getZoneMaxValue();
         final String zoneMaxValueText = zoneMaxValue == Integer.MAX_VALUE //
               ? Messages.App_Label_max
               : Integer.toString(zoneMaxValue);

         final float zoneMinBpm = zoneContext.zoneMinBpm[tourZoneIndex];
         final float zoneMaxBmp = zoneContext.zoneMaxBpm[tourZoneIndex];

         final String zoneMaxBpmText = zoneMaxBmp == Integer.MAX_VALUE //
               ? Messages.App_Label_max
               : Integer.toString((int) zoneMaxBmp);

         final int ageYears = zoneContext.age;
         final String ageText = UI.SPACE + ageYears + UI.SPACE2 + Messages.Pref_People_Label_Years;

         final String hrZoneTooltip =
               //
               hrZone.getNameLongShortcutFirst()
                     //
                     + UI.NEW_LINE
                     + UI.NEW_LINE
                     //
                     + hrZone.getZoneMinValue()
                     + UI.DASH
                     + zoneMaxValueText
                     + UI.SPACE
                     + UI.SYMBOL_PERCENTAGE
                     //
                     + UI.SPACE
                     + UI.SYMBOL_EQUAL
                     + UI.SPACE
                     //
                     + Integer.toString((int) zoneMinBpm)
                     + UI.DASH
                     + zoneMaxBpmText
                     + UI.SPACE
                     + net.tourbook.common.Messages.Graph_Label_Heartbeat_Unit
                     //
                     + UI.NEW_LINE
                     + UI.NEW_LINE
                     //
                     + Messages.Pref_People_Label_Age
                     + ageText
                     //
                     + UI.DASH_WITH_DOUBLE_SPACE
                     //
                     + Messages.HRMax_Label
                     + UI.SPACE
                     + zoneContext.hrMax
                     + net.tourbook.common.Messages.Graph_Label_Heartbeat_Unit
         //
         ;

         // % values
         _lblHrZonePercent[tourZoneIndex].setText(_nf1.format(zoneTimePercent));
         _lblHrZonePercent[tourZoneIndex].setToolTipText(hrZoneTooltip);

         // bpm values
         _lblTourMinMaxValue[tourZoneIndex].setText(((int) zoneMinBpm) + UI.DASH + zoneMaxBpmText);
         _lblTourMinMaxValue[tourZoneIndex].setToolTipText(hrZoneTooltip);

         _lblTourMinMaxHours[tourZoneIndex].setText(net.tourbook.common.UI
               .format_hh_mm((long) (zoneTime + 30))
               .toString());
         _lblTourMinMaxHours[tourZoneIndex].setToolTipText(hrZoneTooltip);

         _lblHRZoneName[tourZoneIndex].setToolTipText(hrZoneTooltip);
         _lblHRZoneColor[tourZoneIndex].setToolTipText(hrZoneTooltip);
      }
   }

}
