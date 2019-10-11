/*******************************************************************************
 * Copyright (C) 2005, 2019 Frederic Bard and Contributors
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

import gnu.trove.list.array.TIntArrayList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.data.TourData;
import net.tourbook.data.TourPerson;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.statistic.ActionSynchChartScale;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.SQLFilter;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.UI;

import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

public class PerformanceChartView extends ViewPart implements ITourProvider {

   public static final String           ID                       = "net.tourbook.ui.views.performanceChart.PerformanceChartView"; //$NON-NLS-1$

   private static final String          COMBO_MINIMUM_WIDTH      = "1234567890";                                  //$NON-NLS-1$
   private static final String          COMBO_MAXIMUM_WIDTH      = "123456789012345678901234567890";              //$NON-NLS-1$

   private static final char            NL                       = net.tourbook.common.UI.NEW_LINE;

   private static final boolean         IS_OSX                   = net.tourbook.common.UI.IS_OSX;
   private static final boolean         IS_LINUX                 = net.tourbook.common.UI.IS_LINUX;

   private static boolean               _isInUpdateUI;

   private final IPreferenceStore       _prefStore               = TourbookPlugin.getPrefStore();
   private final IPreferenceStore       _prefStoreCommon         = CommonActivator.getPrefStore();
   private final IDialogSettings        _state                   = TourbookPlugin.getState("PerformanceChartView");               //$NON-NLS-1$

   private IPartListener2               _partListener;
   private IPropertyChangeListener      _prefChangeListener;
   private IPropertyChangeListener      _prefChangeListenerCommon;
   private ITourEventListener           _tourEventListener;
   private ISelectionListener           _postSelectionListener;

   private TourPerson                   _activePerson;
   private TourTypeFilter               _activeTourTypeFilter;

   private int                          _selectedYear            = -1;

   /**
    * Contains all years which have tours for the selected tour type and person.
    */
   private TIntArrayList                _availableYears;

   /**
    * contains the statistics in the same sort order as the statistic combo box
    */
   private ActionSynchChartScale        _actionSynchChartScale;

   private boolean                      _isSynchScaleEnabled;
   private boolean                      _isVerticalOrderDisabled;

   private int                          _minimumComboWidth;
   private int                          _maximumComboWidth;

   private PixelConverter               _pc;

   /*
    * UI controls
    */
   private Combo                    _comboYear;
   private Combo                    _comboNumberOfYears;
   private Combo                    _comboBarVerticalOrder;

   private Composite                _statContainer;

   public static boolean isInUpdateUI() {
      return _isInUpdateUI;
   }

   void actionSynchScale(final boolean isEnabled) {

      _isSynchScaleEnabled = isEnabled;
   }

   private void addPartListener() {

      // set the part listener
      _partListener = new IPartListener2() {

         @Override
         public void partActivated(final IWorkbenchPartReference partRef) {

//          if (partRef.getPart(false) == TourStatisticsView.this) {
//
//             int a = 0;
//             a++;
//          }
         }

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
      };

      // register the part listener
      getSite().getPage().addPartListener(_partListener);
   }

   private void addPrefListener() {

      _prefChangeListener = new IPropertyChangeListener() {

         @Override
         public void propertyChange(final PropertyChangeEvent event) {

            final String property = event.getProperty();

            /*
             * set a new chart configuration when the preferences has changed
             */

            if (property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)
                  || property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)
                  || property.equals(ITourbookPreferences.TOUR_PERSON_LIST_IS_MODIFIED)) {

               _activePerson = TourbookPlugin.getActivePerson();
               _activeTourTypeFilter = TourbookPlugin.getActiveTourTypeFilter();


            } else if (property.equals(ITourbookPreferences.STATISTICS_STATISTIC_PROVIDER_IDS)) {


            } else if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

               // measurement system has changed

               UI.updateUnits();

            }
         }
      };

      // register the listener
      _prefStore.addPropertyChangeListener(_prefChangeListener);

      /*
       * Common preferences
       */
      _prefChangeListenerCommon = new IPropertyChangeListener() {
         @Override
         public void propertyChange(final PropertyChangeEvent event) {

            final String property = event.getProperty();

            if (property.equals(ICommonPreferences.TIME_ZONE_LOCAL_ID)) {

            }
         }
      };

      // register the listener
      _prefStoreCommon.addPropertyChangeListener(_prefChangeListenerCommon);
   }

   private void addSelectionListener() {

      // this view part is a selection listener
      _postSelectionListener = new ISelectionListener() {

         @Override
         public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

            if (part == PerformanceChartView.this) {
               return;
            }

            if (selection instanceof SelectionDeletedTours) {
            }
         }
      };

      // register selection listener in the page
      getSite().getPage().addPostSelectionListener(_postSelectionListener);
   }

   private void addTourEventListener() {

      _tourEventListener = new ITourEventListener() {
         @Override
         public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object propertyData) {

            if (eventId == TourEventId.TOUR_CHANGED && propertyData instanceof TourEvent) {

               if (part == PerformanceChartView.this) {
                  return;
               }

               if (((TourEvent) propertyData).isTourModified) {
                  /*
                   * ignore edit changes because the statistics show data only from saved data
                   */
                  return;
               }

               _isInUpdateUI = true;

               // update statistics

               _isInUpdateUI = false;

            } else if (eventId == TourEventId.UPDATE_UI || //
                  eventId == TourEventId.ALL_TOURS_ARE_MODIFIED) {

            }
         }
      };
      TourManager.getInstance().addTourEventListener(_tourEventListener);
   }

   public boolean canFireEvents() {

      return _isInUpdateUI == false;
   }

   private void createActions() {

      //_actionSynchChartScale = new ActionSynchChartScale(this);
   }

   @Override
   public void createPartControl(final Composite parent) {

      initUI(parent);

      createUI(parent);

      createActions();

      addPartListener();
      addPrefListener();
      addSelectionListener();
      addTourEventListener();

      /*
       * Start async that the workspace is fully initialized with all data filters
       */
      parent.getDisplay().asyncExec(new Runnable() {
         @Override
         public void run() {

            _activePerson = TourbookPlugin.getActivePerson();
            _activeTourTypeFilter = TourbookPlugin.getActiveTourTypeFilter();

            restoreState();
         }
      });
   }

   private void createUI(final Composite parent) {

      _statContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(_statContainer);
      {
         createUI_10_Toolbar(_statContainer);

      }
   }

   private void createUI_10_Toolbar(final Composite parent) {

      final int widgetSpacing = 15;

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory
            .fillDefaults()//
            .grab(true, false)
            .align(SWT.BEGINNING, SWT.FILL)
            .applyTo(container);
      GridLayoutFactory
            .fillDefaults()//
            .numColumns(6)
            .margins(3, 3)
            .applyTo(container);
      {
         {
            /*
             * combo: statistics
             */

         }

         {
            /*
             * combo: year
             */

            _comboYear = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
            _comboYear.setToolTipText(Messages.Tour_Book_Combo_year_tooltip);
            _comboYear.setVisibleItemCount(50);

            GridDataFactory
                  .fillDefaults()//
                  .indent(widgetSpacing, 0)
                  .hint(_pc.convertWidthInCharsToPixels(IS_OSX ? 12 : IS_LINUX ? 12 : 5), SWT.DEFAULT)
                  .applyTo(_comboYear);

            _comboYear.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onSelectYear();
               }
            });
         }

         {
            /*
             * combo: year numbers
             */

            _comboNumberOfYears = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
            _comboNumberOfYears.setToolTipText(Messages.tour_statistic_number_of_years);
            _comboNumberOfYears.setVisibleItemCount(50);

            GridDataFactory
                  .fillDefaults()//
                  .indent(2, 0)
                  .hint(_pc.convertWidthInCharsToPixels(IS_OSX ? 8 : IS_LINUX ? 8 : 4), SWT.DEFAULT)
                  .applyTo(_comboNumberOfYears);

            _comboNumberOfYears.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onSelectYear();
               }
            });
         }

         {
            /*
             * combo: sequence for stacked charts
             */

            _comboBarVerticalOrder = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
            _comboBarVerticalOrder.setToolTipText(Messages.Tour_Statistic_Combo_BarVOrder_Tooltip);
            _comboBarVerticalOrder.setVisibleItemCount(50);
            _comboBarVerticalOrder.setVisible(false);

            GridDataFactory
                  .fillDefaults()//
                  .indent(widgetSpacing, 0)
                  //                .hint(defaultTextSize.x, SWT.DEFAULT)
                  .applyTo(_comboBarVerticalOrder);

            _comboBarVerticalOrder.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
               }
            });
         }
      }
   }

   @Override
   public void dispose() {

      // dispose all statistic resources
      getViewSite().getPage().removePartListener(_partListener);
      getSite().getPage().removePostSelectionListener(_postSelectionListener);
      TourManager.getInstance().removeTourEventListener(_tourEventListener);

      _prefStore.removePropertyChangeListener(_prefChangeListener);
      _prefStoreCommon.removePropertyChangeListener(_prefChangeListenerCommon);

      super.dispose();
   }

   /**
    * @param defaultYear
    * @return Returns the index for the active year or <code>-1</code> when there are no years
    *         available
    */
   private int getActiveYearComboboxIndex(final int defaultYear) {

      int selectedYearIndex = -1;

      if (_availableYears == null) {
         return selectedYearIndex;
      }

      /*
       * try to get the year index for the default year
       */
      if (defaultYear != -1) {

         int yearIndex = 0;
         for (final int year : _availableYears.toArray()) {

            if (year == defaultYear) {

               _selectedYear = defaultYear;

               return yearIndex;
            }
            yearIndex++;
         }
      }

      /*
       * try to get year index of the selected year
       */
      int yearIndex = 0;
      for (final int year : _availableYears.toArray()) {
         if (year == _selectedYear) {
            selectedYearIndex = yearIndex;
            break;
         }
         yearIndex++;
      }

      return selectedYearIndex;
   }


   /**
    * @return Returns number of years which are selected in the combobox
    */
   private int getNumberOfYears() {

      int numberOfYears = 1;
      final int selectedIndex = _comboNumberOfYears.getSelectionIndex();

      if (selectedIndex != -1) {
         numberOfYears = selectedIndex + 1;
      }

      return numberOfYears;
   }

   @Override
   public ArrayList<TourData> getSelectedTours() {
      return new ArrayList<>();
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      final GC gc = new GC(parent);
      {
         _minimumComboWidth = gc.textExtent(COMBO_MINIMUM_WIDTH).x;
         _maximumComboWidth = gc.textExtent(COMBO_MAXIMUM_WIDTH).x;
      }
      gc.dispose();
   }

   private void onSelectStatistic() {

   }

   private void onSelectYear() {

      final int selectedItem = _comboYear.getSelectionIndex();
      if (selectedItem != -1) {

         _selectedYear = Integer.parseInt(_comboYear.getItem(selectedItem));

      }
   }

   /**
    * create the year list for all tours and fill the year combobox with the available years
    */
   private void refreshYearCombobox() {

      final SQLFilter filter = new SQLFilter(SQLFilter.TAG_FILTER);

      String fromTourData;

      final SQLFilter sqlFilter = new SQLFilter(SQLFilter.TAG_FILTER);
      if (sqlFilter.isTagFilterActive()) {

         // with tag filter

         fromTourData = NL

               + "FROM (         " + NL //$NON-NLS-1$

               + " SELECT        " + NL //$NON-NLS-1$

               + "  StartYear    " + NL //$NON-NLS-1$

               + ("  FROM " + TourDatabase.TABLE_TOUR_DATA) + NL//$NON-NLS-1$

               // get tag id's
               + "  LEFT OUTER JOIN " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " jTdataTtag" + NL //$NON-NLS-1$ //$NON-NLS-2$
               + "  ON tourID = jTdataTtag.TourData_tourId  " + NL //$NON-NLS-1$

               + "  WHERE 1=1    " + NL //$NON-NLS-1$
               + sqlFilter.getWhereClause()

               + ") td           " + NL//$NON-NLS-1$
         ;

      } else {

         // without tag filter

         fromTourData = NL

               + " FROM " + TourDatabase.TABLE_TOUR_DATA + NL //$NON-NLS-1$

               + " WHERE 1=1        " + NL //$NON-NLS-1$
               + sqlFilter.getWhereClause() + NL;
      }

      final String sqlString = NL +

            "SELECT                 " + NL //$NON-NLS-1$

            + " StartYear           " + NL //$NON-NLS-1$

            + fromTourData

            + " GROUP BY STARTYEAR     " + NL //$NON-NLS-1$
            + " ORDER BY STARTYEAR     " + NL//       //$NON-NLS-1$
      ;
      _availableYears = new TIntArrayList();

      try {
         final Connection conn = TourDatabase.getInstance().getConnection();
         final PreparedStatement statement = conn.prepareStatement(sqlString);
         filter.setParameters(statement, 1);

         final ResultSet result = statement.executeQuery();

         while (result.next()) {
            _availableYears.add(result.getInt(1));
         }

         conn.close();

      } catch (final SQLException e) {
         UI.showSQLException(e);
      }

      _comboYear.removeAll();

      /*
       * add all years of the tours and the current year
       */
      final int thisYear = LocalDate.now().getYear();

      boolean isThisYearSet = false;

      for (final int year : _availableYears.toArray()) {

         if (year == thisYear) {
            isThisYearSet = true;
         }

         _comboYear.add(Integer.toString(year));
      }

      // add currenty year if not set
      if (isThisYearSet == false) {
         _availableYears.add(thisYear);
         _comboYear.add(Integer.toString(thisYear));
      }
   }

   /**
    * Restore selected statistic
    */
   void restoreState() {

   }

   @PersistState
   private void saveState() {
   }

   private void selectYear(final int defaultYear) {

      int selectedYearIndex = getActiveYearComboboxIndex(defaultYear);
      if (selectedYearIndex == -1) {

         /*
          * the active year was not found in the combo box, it's possible that the combo box needs
          * to be update
          */

         refreshYearCombobox();
         selectedYearIndex = getActiveYearComboboxIndex(defaultYear);

         if (selectedYearIndex == -1) {

            // year is still not selected
            final int yearCount = _comboYear.getItemCount();

            // reselect the youngest year if years are available
            if (yearCount > 0) {
               selectedYearIndex = yearCount - 1;
               _selectedYear = Integer.parseInt(_comboYear.getItem(yearCount - 1));
            }
         }
      }

      _comboYear.select(selectedYearIndex);
   }

   @Override
   public void setFocus() {

   }

}
