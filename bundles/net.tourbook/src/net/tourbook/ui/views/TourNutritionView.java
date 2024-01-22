/*******************************************************************************
 * Copyright (C) 2023 Frédéric Bard
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
package net.tourbook.ui.views;

import static org.eclipse.swt.events.KeyListener.keyPressedAdapter;
import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.TableColumnDefinition;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourBeverageContainer;
import net.tourbook.data.TourData;
import net.tourbook.data.TourNutritionProduct;
import net.tourbook.database.TourDatabase;
import net.tourbook.nutrition.DialogTourNutritionProduct;
import net.tourbook.nutrition.NutritionUtils;
import net.tourbook.nutrition.QuantityType;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.views.nutrition.DialogSearchProduct;
import net.tourbook.web.WEB;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

import cop.swt.widgets.viewers.table.celleditors.RangeContent;
import cop.swt.widgets.viewers.table.celleditors.SpinnerCellEditor;

public class TourNutritionView extends ViewPart implements ITourViewer {

   //todo fb add the possibility to add custom products (i.e: water...)
// => Manually with a dialog that asks the name, calories, sodium, is beverage (if yes, ungray the beverage qtty)

   //todo fb clear the view when mltiple tours are selected

   //todo fb on linux, there are a lot of space between the 2 sections even when NOT expanded ?
   //ask wolfgang when i do the pr
   static final String                    ID                              = "net.tourbook.ui.views.TourNutritionView";  //$NON-NLS-1$
   private static final String            STATE_PRODUCT_SEARCHES_HISTORY  = "products.searchesHistory";                 //$NON-NLS-1$
   private static final String            STATE_SECTION_PRODUCTS_LIST     = "STATE_SECTION_PRODUCTS_LIST";              //$NON-NLS-1$
   private static final String            STATE_SECTION_SUMMARY           = "STATE_SECTION_SUMMARY";                    //$NON-NLS-1$

   private static final String            COLUMN_CONSUMED_QUANTITY        = "ConsumedQuantity";                         //$NON-NLS-1$
   private static final String            COLUMN_QUANTITY_TYPE            = "QuantityType";                             //$NON-NLS-1$
   private static final String            COLUMN_NAME                     = "Name";                                     //$NON-NLS-1$
   private static final String            COLUMN_CALORIES                 = "Calories";                                 //$NON-NLS-1$
   private static final String            COLUMN_SODIUM                   = "Sodium";                                   //$NON-NLS-1$
   private static final String            COLUMN_ISBEVERAGE               = "IsBeverage";                               //$NON-NLS-1$
   private static final String            COLUMN_BEVERAGE_QUANTITY        = "BeverageQuantity";                         //$NON-NLS-1$
   private static final String            COLUMN_BEVERAGE_CONTAINER       = "BeverageContainer";                        //$NON-NLS-1$
   private static final String            COLUMN_CONSUMED_CONTAINERS      = "ConsumedContainers";                       //$NON-NLS-1$

   public static final String             OPENFOODFACTS_BASEPATH          = "https://world.openfoodfacts.org/product/"; //$NON-NLS-1$

   private static final IPreferenceStore  _prefStore                      = TourbookPlugin.getPrefStore();

   private static final int               _hintTextColumnWidth            = UI.IS_OSX ? 100 : 50;

   private final IDialogSettings          _state                          = TourbookPlugin.getState(ID);
   private TourData                       _tourData;

   private PixelConverter                 _pc;

   private TableViewer                    _productsViewer;
   private ColumnManager                  _columnManager;

   private TableColumnDefinition          _colDef_ConsumedQuantity;
   private TableColumnDefinition          _colDef_QuantityType;
   private TableColumnDefinition          _colDef_Name;
   private TableColumnDefinition          _colDef_Calories;
   private TableColumnDefinition          _colDef_Sodium;
   private TableColumnDefinition          _colDef_IsBeverage;
   private TableColumnDefinition          _colDef_BeverageQuantity;
   private TableColumnDefinition          _colDef_BeverageContainer;
   private TableColumnDefinition          _colDef_ConsumedBeverageContainers;

   private TourNutritionProductComparator _tourNutritionProductComparator = new TourNutritionProductComparator();
   private List<TourBeverageContainer>    _tourBeverageContainers         = new ArrayList<>();

   private List<String>                   _searchHistory                  = new ArrayList<>();
   private IPropertyChangeListener        _prefChangeListener;
   private SelectionListener              _columnSortListener;

   private ISelectionListener             _postSelectionListener;
   private ITourEventListener             _tourEventListener;
   private IPartListener2                 _partListener;
   private PostSelectionProvider          _postSelectionProvider;

   private final RangeContent             _quantityRange                  = new RangeContent(0.25, 10.0, 0.25, 100);

   private final NumberFormat             _nf2                            = NumberFormat.getNumberInstance();

   {
      _nf2.setMinimumFractionDigits(2);
      _nf2.setMaximumFractionDigits(2);
   }

   /*
    * UI controls
    */
   private Image       _imageAdd;
   private Image       _imageSearch;
   private Image       _imageDelete;

   private Button      _btnAddProduct;
   private Button      _btnSearchProduct;
   private Button      _btnDeleteProduct;

   private PageBook    _pageBook;
   private Composite   _pageNoData;
   private Composite   _viewerContainer;

   private boolean     _isInUpdate;
   private Text        _txtCalories_Average;
   private Text        _txtCalories_Total;
   private Text        _txtFluid_Average;
   private Text        _txtFluid_Total;
   private Text        _txtSodium_Average;
   private Text        _txtSodium_Total;
   private Section     _sectionProductsList;
   private Section     _sectionSummary;
   private FormToolkit _tk;

   private final class No_EditingSupport extends EditingSupport {

      private No_EditingSupport() {
         super(_productsViewer);
      }

      @Override
      protected boolean canEdit(final Object element) {
         return false;
      }

      @Override
      protected CellEditor getCellEditor(final Object element) {
         return null;
      }

      @Override
      protected Object getValue(final Object element) {
         return null;
      }

      @Override
      protected void setValue(final Object element, final Object value) {
         //Nothing to do
      }
   }

   private class TourNutritionProductComparator extends ViewerComparator {

      private static final int ASCENDING       = 0;
      private static final int DESCENDING      = 1;

      private String           __sortColumnId  = COLUMN_NAME;
      private int              __sortDirection = ASCENDING;

      @Override
      public int compare(final Viewer viewer, final Object e1, final Object e2) {

         final boolean isDescending = __sortDirection == DESCENDING;

         final TourNutritionProduct tnp1 = (TourNutritionProduct) e1;
         final TourNutritionProduct tnp2 = (TourNutritionProduct) e2;

         long rc = 0;

         // Determine which column and do the appropriate sort
         switch (__sortColumnId) {

         case COLUMN_CONSUMED_QUANTITY:
            rc = Math.round(tnp1.getConsumedQuantity() - tnp2.getConsumedQuantity());
            break;

         case COLUMN_NAME:
         default:
            rc = tnp1.getName().compareTo(tnp2.getName());
            break;

         }

         if (rc == 0) {

            // subsort 1 by category
            rc = tnp1.getName().compareTo(tnp2.getName());
         }

         // if descending order, flip the direction
         if (isDescending) {
            rc = -rc;
         }

         /*
          * MUST return 1 or -1 otherwise long values are not sorted correctly.
          */
         return rc > 0 //
               ? 1
               : rc < 0 //
                     ? -1
                     : 0;

      }

      /**
       * @param sortColumnId
       *
       * @return Returns the column widget by its column id, when column id is not found then the
       *         first column is returned.
       */
      private TableColumn getSortColumn(final String sortColumnId) {

         final TableColumn[] allColumns = _productsViewer.getTable().getColumns();

         for (final TableColumn column : allColumns) {

            final String columnId = ((ColumnDefinition) column.getData()).getColumnId();

            if (columnId != null && columnId.equals(sortColumnId)) {
               return column;
            }
         }

         return allColumns[0];
      }

      @Override
      public boolean isSorterProperty(final Object element, final String property) {

         // force resorting when a name is renamed
         return true;
      }

      public void setSortColumn(final Widget widget) {

         final ColumnDefinition columnDefinition = (ColumnDefinition) widget.getData();
         final String columnId = columnDefinition.getColumnId();

         if (columnId == null) {
            return;
         }

         if (columnId.equals(__sortColumnId)) {

            // Same column as last sort -> select next sorting

            switch (__sortDirection) {
            case ASCENDING:
               __sortDirection = DESCENDING;
               break;

            case DESCENDING:
            default:
               __sortDirection = ASCENDING;
               break;
            }

         } else {

            // New column; do an ascent sorting

            __sortColumnId = columnId;
            __sortDirection = ASCENDING;
         }

         updateUI_SetSortDirection(__sortColumnId, __sortDirection);
      }

      /**
       * Set the sort column direction indicator for a column
       *
       * @param sortColumnId
       * @param isAscendingSort
       */
      private void updateUI_SetSortDirection(final String sortColumnId, final int sortDirection) {

         final int direction =
               sortDirection == TourNutritionProductComparator.ASCENDING ? SWT.UP
                     : sortDirection == TourNutritionProductComparator.DESCENDING ? SWT.DOWN
                           : SWT.NONE;

         final Table table = _productsViewer.getTable();
         final TableColumn tc = getSortColumn(sortColumnId);

         table.setSortColumn(tc);
         table.setSortDirection(direction);
      }
   }

   private class ViewContentProvider implements IStructuredContentProvider {

      @Override
      public void dispose() {}

      @Override
      public Object[] getElements(final Object parent) {

         if (_tourData == null || _tourData.isMultipleTours()) {
            return new Object[0];
         }

         return _tourData.getTourNutritionProducts().toArray();
      }

      @Override
      public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
         // Nothing to do
      }
   }

   private void addPartListener() {

      _partListener = new IPartListener2() {

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
      };
      getViewSite().getPage().addPartListener(_partListener);
   }

   private void addPrefListener() {

      _prefChangeListener = propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

//      //todo fb recreate the when the preferences are changed and a container is added or removed or modified
         if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

            _productsViewer.getTable().setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));
            _productsViewer.refresh();

         } else if (property.equals(ITourbookPreferences.PREF_BEVERAGECONTAINERS_HAS_CHANGED)) {

            //todo fb does it reload the tour data (aka, the ones that had containers assigned to them?)
            _tourBeverageContainers = TourDatabase.getTourBeverageContainers();
            // todo fb necessary ? recreateViewer(getViewer());

            refreshTourData(_tourData = TourManager.getTour(_tourData.getTourId()));
         }
      };

      _prefStore.addPropertyChangeListener(_prefChangeListener);
   }

   /**
    * listen for events when a tour is selected
    */
   private void addSelectionListener() {

      _postSelectionListener = (workbenchPart, selection) -> {

         if (workbenchPart == TourNutritionView.this) {
            return;
         }

         onSelectionChanged(selection);
      };
      getSite().getPage().addPostSelectionListener(_postSelectionListener);
   }

   private void addTourEventListener() {

      _tourEventListener = (workbenchPart, tourEventId, eventData) -> {

         if (_isInUpdate || workbenchPart == TourNutritionView.this) {
            return;
         }

         if (tourEventId == TourEventId.TOUR_SELECTION && eventData instanceof final ISelection eventDataSelection) {

            onSelectionChanged(eventDataSelection);

         } else {

            if (_tourData == null) {
               return;
            }

            if ((tourEventId == TourEventId.TOUR_CHANGED) && (eventData instanceof final TourEvent tourEventData)) {

               final ArrayList<TourData> modifiedTours = tourEventData.getModifiedTours();
               if (modifiedTours != null) {

                  // update modified tour

                  final long viewTourId = _tourData.getTourId();

                  // The view contains multiple tours
                  if (_tourData.isMultipleTours()) {

                     showInvalidPage();
                  } else {

                     // The view contains a single tour
                     for (final TourData tourData : modifiedTours) {
                        if (tourData.getTourId() == viewTourId) {

                           refreshTourData(tourData);

                           // nothing more to do, the view contains only one tour
                           return;
                        }
                     }
                  }
               }
            } else if (tourEventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

               clearView();
            }
         }
      };

      TourManager.getInstance().addTourEventListener(_tourEventListener);
   }

   private void clearView() {

      _tourData = null;

      updateUI_ProductViewer();

      _postSelectionProvider.clearSelection();
   }

   private long computeAveragePerHour(final int total) {

      long tourDeviceTime_Recorded = _tourData.getTourDeviceTime_Recorded();
      if (tourDeviceTime_Recorded > 3600 /* && preference to remove first hour */) {
         tourDeviceTime_Recorded -= 3600;
      } else if (tourDeviceTime_Recorded <= 3600 /* && preference to remove first hour */) {
         return total;
      }

      return total * 60 / (tourDeviceTime_Recorded / 60);
   }

   @Override
   public void createPartControl(final Composite parent) {

      initUI(parent);

      // define all columns for the viewer
      _columnManager = new ColumnManager(this, _state);
      defineAllColumns();

      createUI(parent);

      addSelectionListener();
      addTourEventListener();
      addPrefListener();
      addPartListener();

      // this part is a selection provider
      _postSelectionProvider = new PostSelectionProvider(ID);
      getSite().setSelectionProvider(_postSelectionProvider);

      // show default page
      showInvalidPage();

      // show nutrition info from last selection
      onSelectionChanged(getSite().getWorkbenchWindow().getSelectionService().getSelection());

      restoreState();

      if (_tourData == null) {
         showTourFromTourProvider();
      }
   }

   private Section createSection(final Composite parent,
                                 final FormToolkit tk,
                                 final String title) {

      final Section section = tk.createSection(parent, ExpandableComposite.TWISTIE | ExpandableComposite.TITLE_BAR);

      section.setText(title);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(section);

      final Composite sectionContainer = tk.createComposite(section);
      section.setClient(sectionContainer);

      return section;
   }

   private void createUI(final Composite parent) {

      _pageBook = new PageBook(parent, SWT.NONE);

      _tourBeverageContainers = TourDatabase.getTourBeverageContainers();

      _pageNoData = net.tourbook.common.UI.createUI_PageNoData(_pageBook, Messages.UI_Label_no_chart_is_selected);

      _tk = new FormToolkit(parent.getDisplay());

      _imageAdd = TourbookPlugin.getImageDescriptor(Images.App_Add).createImage();
      _imageSearch = TourbookPlugin.getImageDescriptor(Images.SearchExternal).createImage();
      _imageDelete = TourbookPlugin.getImageDescriptor(Images.App_Trash).createImage();

      _viewerContainer = new Composite(_pageBook, SWT.NONE);
      GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
      {
         createUI_Section_10_Summary(_viewerContainer);
         createUI_Section_20_ProductsList(_viewerContainer);
      }
   }

   private void createUI_110_Report(final Composite parent) {

      final Composite container = _tk.createComposite(parent);
      GridLayoutFactory.fillDefaults().numColumns(7).applyTo(container);
      {
         /*
          * Columns headers
          */
         {
            Label label = UI.createLabel(container, UI.EMPTY_STRING);
            GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).applyTo(label);

            label = UI.createLabel(container, Messages.Tour_Nutrition_Column_Calories);
            GridDataFactory.fillDefaults().span(2, 1).align(SWT.CENTER, SWT.FILL).applyTo(label);

            label = UI.createLabel(container, Messages.Tour_Nutrition_Label_Fluids);
            GridDataFactory.fillDefaults().span(2, 1).align(SWT.CENTER, SWT.FILL).applyTo(label);

            label = UI.createLabel(container, Messages.Tour_Nutrition_Column_Sodium);
            GridDataFactory.fillDefaults().span(2, 1).align(SWT.CENTER, SWT.FILL).applyTo(label);
         }

         /*
          * Totals
          */
         {
            final Label label = UI.createLabel(container, Messages.Tour_Nutrition_Label_Totals);
            label.setToolTipText(Messages.Tour_Nutrition_Label_Totals_Tooltip);
            GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.CENTER).applyTo(label);

            _txtCalories_Total = _tk.createText(container, UI.EMPTY_STRING, SWT.TRAIL);
            _txtCalories_Total.setEnabled(false);
            GridDataFactory.fillDefaults()
                  .hint(_hintTextColumnWidth, SWT.DEFAULT)
                  .align(SWT.END, SWT.FILL)
                  .applyTo(_txtCalories_Total);

            // Unit: kcal
            UI.createLabel(container, net.tourbook.ui.Messages.Value_Unit_KCalories);

            _txtFluid_Total = _tk.createText(container, UI.EMPTY_STRING, SWT.TRAIL);
            _txtFluid_Total.setEnabled(false);
            GridDataFactory.fillDefaults()
                  .hint(_hintTextColumnWidth, SWT.DEFAULT)
                  .align(SWT.END, SWT.FILL)
                  .applyTo(_txtFluid_Total);

            // Unit: L
            UI.createLabel(container, UI.UNIT_FLUIDS_L);

            _txtSodium_Total = _tk.createText(container, UI.EMPTY_STRING, SWT.TRAIL);
            _txtSodium_Total.setEnabled(false);
            GridDataFactory.fillDefaults()
                  .hint(_hintTextColumnWidth, SWT.DEFAULT)
                  .align(SWT.END, SWT.FILL)
                  .applyTo(_txtSodium_Total);

            // Unit: mg
            UI.createLabel(container, UI.UNIT_WEIGHT_MG);
         }

         /*
          * Averages
          */
         {
            final Label label = UI.createLabel(container, Messages.Tour_Nutrition_Label_Averages);
            label.setToolTipText(Messages.Tour_Nutrition_Label_Averages_Tooltip);
            GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.CENTER).applyTo(label);

            _txtCalories_Average = _tk.createText(container, UI.EMPTY_STRING, SWT.TRAIL);
            _txtCalories_Average.setEnabled(false);
            GridDataFactory.fillDefaults().hint(_hintTextColumnWidth, SWT.DEFAULT).align(SWT.END, SWT.FILL)
                  .applyTo(_txtCalories_Average);

            // Unit: kcal/h
            UI.createLabel(container, net.tourbook.ui.Messages.Value_Unit_KCalories + UI.SLASH + UI.UNIT_LABEL_TIME);

            _txtFluid_Average = _tk.createText(container, UI.EMPTY_STRING, SWT.TRAIL);
            _txtFluid_Average.setEnabled(false);
            GridDataFactory.fillDefaults().hint(_hintTextColumnWidth, SWT.DEFAULT).align(SWT.END, SWT.FILL)
                  .applyTo(_txtFluid_Average);

            // Unit: L/h
            UI.createLabel(container, UI.UNIT_FLUIDS_L + UI.SLASH + UI.UNIT_LABEL_TIME);

            _txtSodium_Average = _tk.createText(container, UI.EMPTY_STRING, SWT.TRAIL);
            _txtSodium_Average.setEnabled(false);
            GridDataFactory.fillDefaults().hint(_hintTextColumnWidth, SWT.DEFAULT).align(SWT.END, SWT.FILL)
                  .applyTo(_txtSodium_Average);

            // Unit: mg/h
            UI.createLabel(container, UI.UNIT_WEIGHT_MG + UI.SLASH + UI.UNIT_LABEL_TIME);
         }
      }
   }

   private void createUI_210_Actions(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().span(3, 1).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(6).applyTo(container);
      {
         /*
          * Search product button
          */
         _btnSearchProduct = new Button(container, SWT.NONE);
         _btnSearchProduct.setText(Messages.Tour_Nutrition_Button_SearchProduct);
         _btnSearchProduct.setToolTipText(Messages.Tour_Nutrition_Button_SearchProduct_Tooltip);
         _btnSearchProduct.addSelectionListener(widgetSelectedAdapter(selectionEvent -> new DialogSearchProduct(Display.getCurrent().getActiveShell(),
               _tourData.getTourId()).open()));
         _btnSearchProduct.setImage(_imageSearch);
         GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).grab(true, false).applyTo(_btnSearchProduct);

         /*
          * Add product button
          */
         _btnAddProduct = new Button(container, SWT.NONE);
         _btnAddProduct.setText(Messages.Tour_Nutrition_Button_AddProduct);
         _btnAddProduct.setToolTipText(Messages.Tour_Nutrition_Button_AddProduct_Tooltip);
         _btnAddProduct.addSelectionListener(widgetSelectedAdapter(selectionEvent -> {

            final DialogTourNutritionProduct dialogTourNutritionProduct = new DialogTourNutritionProduct(Display.getCurrent().getActiveShell());

            if (dialogTourNutritionProduct.open() != Window.OK) {
               return;
            }

            final Set<TourNutritionProduct> tourNutritionProducts = _tourData.getTourNutritionProducts();
            tourNutritionProducts.add(dialogTourNutritionProduct.getTourNutritionProduct(_tourData));
            _tourData.setTourNutritionProducts(tourNutritionProducts);
            _tourData = TourManager.saveModifiedTour(_tourData);
            _tourData.setTourNutritionProducts(_tourData.getTourNutritionProducts());

         }));
         _btnAddProduct.setImage(_imageAdd);
         GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).grab(true, false).applyTo(_btnAddProduct);

         /*
          * Delete product button
          */
         _btnDeleteProduct = new Button(container, SWT.NONE);
         _btnDeleteProduct.setText(Messages.Tour_Nutrition_Button_DeleteProduct);
         _btnDeleteProduct.setToolTipText(Messages.Tour_Nutrition_Button_DeleteProduct_Tooltip);
         _btnDeleteProduct.setEnabled(false);
         _btnDeleteProduct.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onDeleteProducts()));
         _btnDeleteProduct.setImage(_imageDelete);
         GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).grab(true, false).applyTo(_btnDeleteProduct);
      }
   }

   private void createUI_220_Viewer(final Composite parent) {

      /*
       * table viewer: products
       */
      final Table productsTable = new Table(parent, SWT.MULTI | SWT.FULL_SELECTION);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(productsTable);
      productsTable.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));
      productsTable.setHeaderVisible(true);

      _productsViewer = new TableViewer(productsTable);

      // very important: the editing support must be set BEFORE the columns are created
      setColumnsEditingSupport();

      _columnManager.createColumns(_productsViewer);

      final String[] columnProperties = new String[_columnManager.getVisibleAndSortedColumns().size()];
      for (int index = 0; index < columnProperties.length; index++) {
         columnProperties[index] = _columnManager.getVisibleAndSortedColumns().get(index).getColumnHeaderText(_columnManager);
      }
      _productsViewer.setColumnProperties(columnProperties);

      _productsViewer.setContentProvider(new ViewContentProvider());
      _productsViewer.setComparator(_tourNutritionProductComparator);

      _productsViewer.addDoubleClickListener(event -> {

         final Object selectedItem = ((IStructuredSelection) _productsViewer.getSelection()).getFirstElement();
         if (selectedItem == null) {
            return;
         }

         final TourNutritionProduct tourNutritionProduct = (TourNutritionProduct) selectedItem;

         final String productCode = tourNutritionProduct.getProductCode();
         if (StringUtils.isNumeric(productCode)) {
            WEB.openUrl(OPENFOODFACTS_BASEPATH + productCode);
         }
      });

      _productsViewer.addSelectionChangedListener(selectionChangedEvent -> onTableSelectionChanged(selectionChangedEvent));

      _productsViewer.getTable().addKeyListener(keyPressedAdapter(keyEvent -> {

         if (keyEvent.keyCode == SWT.DEL && _btnDeleteProduct.isEnabled()) {
            onDeleteProducts();
         }
      }));
   }

   private void createUI_Section_10_Summary(final Composite parent) {

      _sectionSummary = createSection(parent, _tk, Messages.Tour_Nutrition_Section_Summary);
      _sectionSummary.setToolTipText(Messages.Tour_Nutrition_Section_Summary_Tooltip);
      final Composite container = (Composite) _sectionSummary.getClient();
      GridLayoutFactory.fillDefaults().applyTo(container);
      {
         createUI_110_Report(container);
      }
   }

   private void createUI_Section_20_ProductsList(final Composite parent) {

      _sectionProductsList = createSection(parent, _tk, Messages.Tour_Nutrition_Section_ProductsList);
      _sectionProductsList.setToolTipText(Messages.Tour_Nutrition_Section_ProductsList_Tooltip);
      final Composite container = (Composite) _sectionProductsList.getClient();
      GridLayoutFactory.fillDefaults().applyTo(container);
      {
         createUI_210_Actions(container);
         createUI_220_Viewer(container);
      }
   }

   private void defineAllColumns() {

      defineColumn_10_ConsumedQuantity();
      defineColumn_20_QuantityType();
      defineColumn_30_Name();
      defineColumn_40_Calories();
      defineColumn_50_Sodium();
      defineColumn_60_IsBeverage();
      defineColumn_70_BeverageQuantity();
      defineColumn_80_BeverageContainer();
      defineColumn_90_ConsumedBeverageContainers();
   }

   private void defineColumn_10_ConsumedQuantity() {

      _colDef_ConsumedQuantity = new TableColumnDefinition(_columnManager, COLUMN_CONSUMED_QUANTITY, SWT.LEAD);

      _colDef_ConsumedQuantity.setColumnLabel(COLUMN_CONSUMED_QUANTITY);
      _colDef_ConsumedQuantity.setColumnHeaderText(Messages.Tour_Nutrition_Column_ConsumedQuantity);
      _colDef_ConsumedQuantity.setColumnHeaderToolTipText(Messages.Tour_Nutrition_Column_ConsumedQuantity_Tooltip);

      _colDef_ConsumedQuantity.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(15));

      _colDef_ConsumedQuantity.setIsDefaultColumn();
      _colDef_ConsumedQuantity.setCanModifyVisibility(false);
      _colDef_ConsumedQuantity.setColumnSelectionListener(_columnSortListener);

      _colDef_ConsumedQuantity.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourNutritionProduct tourNutritionProduct = (TourNutritionProduct) cell.getElement();

            cell.setText(String.valueOf(tourNutritionProduct.getConsumedQuantity()));
         }
      });
   }

   private void defineColumn_20_QuantityType() {

      _colDef_QuantityType = new TableColumnDefinition(_columnManager, COLUMN_QUANTITY_TYPE, SWT.LEAD);

      _colDef_QuantityType.setColumnLabel(COLUMN_QUANTITY_TYPE);
      _colDef_QuantityType.setColumnHeaderText(Messages.Tour_Nutrition_Column_QuantityType);
      _colDef_QuantityType.setColumnHeaderToolTipText(Messages.Tour_Nutrition_Column_QuantityType_Tooltip);

      _colDef_QuantityType.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(17));

      _colDef_QuantityType.setIsDefaultColumn();
      _colDef_QuantityType.setCanModifyVisibility(false);

      _colDef_QuantityType.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourNutritionProduct tourNutritionProduct = (TourNutritionProduct) cell.getElement();

            final QuantityType quantityType = tourNutritionProduct.getQuantityType();
            cell.setText(quantityType == null ? UI.EMPTY_STRING : quantityType.name());
         }
      });
   }

   private void defineColumn_30_Name() {

      _colDef_Name = new TableColumnDefinition(_columnManager, COLUMN_NAME, SWT.LEAD);

      _colDef_Name.setColumnLabel(COLUMN_NAME);
      _colDef_Name.setColumnHeaderText(Messages.Tour_Nutrition_Column_Name);

      _colDef_Name.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(50));

      _colDef_Name.setIsDefaultColumn();
      _colDef_Name.setCanModifyVisibility(false);
      _colDef_Name.setColumnSelectionListener(_columnSortListener);

      _colDef_Name.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourNutritionProduct tourNutritionProduct = (TourNutritionProduct) cell.getElement();

            cell.setText(NutritionUtils.getProductFullName(tourNutritionProduct.getBrand(), tourNutritionProduct.getName()));
         }
      });
   }

   private void defineColumn_40_Calories() {

      _colDef_Calories = new TableColumnDefinition(_columnManager, COLUMN_CALORIES, SWT.LEAD);

      _colDef_Calories.setColumnLabel(COLUMN_CALORIES);
      _colDef_Calories.setColumnHeaderText(Messages.Tour_Nutrition_Column_Calories);

      _colDef_Calories.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(12));

      _colDef_Calories.setIsDefaultColumn();
      _colDef_Calories.setCanModifyVisibility(false);

      _colDef_Calories.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourNutritionProduct tourNutritionProduct = (TourNutritionProduct) cell.getElement();

            final int caloriesValue = tourNutritionProduct.getQuantityType() == QuantityType.Products
                  ? tourNutritionProduct.getCalories()
                  : tourNutritionProduct.getCalories_Serving();
            cell.setText(String.valueOf(caloriesValue));
         }
      });
   }

   private void defineColumn_50_Sodium() {

      _colDef_Sodium = new TableColumnDefinition(_columnManager, COLUMN_SODIUM, SWT.LEAD);

      _colDef_Sodium.setColumnLabel(COLUMN_SODIUM);
      _colDef_Sodium.setColumnHeaderText(Messages.Tour_Nutrition_Column_Sodium);

      _colDef_Sodium.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(12));

      _colDef_Sodium.setIsDefaultColumn();
      _colDef_Sodium.setCanModifyVisibility(false);

      _colDef_Sodium.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourNutritionProduct tourNutritionProduct = (TourNutritionProduct) cell.getElement();

            final int sodiumValue = tourNutritionProduct.getQuantityType() == QuantityType.Products
                  ? tourNutritionProduct.getSodium()
                  : tourNutritionProduct.getSodium_Serving();
            cell.setText(String.valueOf(sodiumValue));
         }
      });
   }

   private void defineColumn_60_IsBeverage() {

      _colDef_IsBeverage = new TableColumnDefinition(_columnManager, COLUMN_ISBEVERAGE, SWT.LEAD);

      _colDef_IsBeverage.setColumnLabel(COLUMN_ISBEVERAGE);
      _colDef_IsBeverage.setColumnHeaderText(Messages.Tour_Nutrition_Column_IsBeverage);

      _colDef_IsBeverage.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(15));

      _colDef_IsBeverage.setIsDefaultColumn();
      _colDef_IsBeverage.setCanModifyVisibility(false);

      _colDef_IsBeverage.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourNutritionProduct tourNutritionProduct = (TourNutritionProduct) cell.getElement();

            final String imageDescriptorPath = tourNutritionProduct.isBeverage()
                  ? Images.Checkbox_Checked
                  : Images.Checkbox_Uncheck;

            final var imagetodispose = TourbookPlugin.getImageDescriptor(imageDescriptorPath).createImage();
            cell.setImage(imagetodispose);
         }
      });
   }

   private void defineColumn_70_BeverageQuantity() {

      _colDef_BeverageQuantity = new TableColumnDefinition(_columnManager, COLUMN_BEVERAGE_QUANTITY, SWT.LEAD);

      _colDef_BeverageQuantity.setColumnLabel(COLUMN_BEVERAGE_QUANTITY);
      _colDef_BeverageQuantity.setColumnHeaderText(Messages.Tour_Nutrition_Column_BeverageQuantity);
      _colDef_BeverageQuantity.setColumnHeaderToolTipText(Messages.Tour_Nutrition_Column_BeverageQuantity_Tooltip);

      _colDef_BeverageQuantity.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(22));

      _colDef_BeverageQuantity.setIsDefaultColumn();
      _colDef_BeverageQuantity.setCanModifyVisibility(false);

      _colDef_BeverageQuantity.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourNutritionProduct tourNutritionProduct = (TourNutritionProduct) cell.getElement();

            final int beverageQuantityValue = tourNutritionProduct.getQuantityType() == QuantityType.Products
                  ? tourNutritionProduct.getBeverageQuantity()
                  : tourNutritionProduct.getBeverageQuantity_Serving();
            final String cellText = tourNutritionProduct.isBeverage()
                  ? String.valueOf(beverageQuantityValue)
                  : UI.EMPTY_STRING;

            cell.setText(cellText);
         }
      });
   }

   private void defineColumn_80_BeverageContainer() {

      _colDef_BeverageContainer = new TableColumnDefinition(_columnManager, COLUMN_BEVERAGE_CONTAINER, SWT.LEAD);

      _colDef_BeverageContainer.setColumnLabel(COLUMN_BEVERAGE_CONTAINER);
      _colDef_BeverageContainer.setColumnHeaderText(Messages.Tour_Nutrition_Column_BeverageContainer);

      _colDef_BeverageContainer.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(25));

      _colDef_BeverageContainer.setIsDefaultColumn();
      _colDef_BeverageContainer.setCanModifyVisibility(false);

      _colDef_BeverageContainer.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourNutritionProduct tourNutritionProduct = (TourNutritionProduct) cell.getElement();

            final String cellText = tourNutritionProduct.getTourBeverageContainer() == null
                  ? UI.EMPTY_STRING
                  : NutritionUtils.buildTourBeverageContainerName(tourNutritionProduct.getTourBeverageContainer());

            cell.setText(cellText);
         }

      });
   }

   private void defineColumn_90_ConsumedBeverageContainers() {

      _colDef_ConsumedBeverageContainers = new TableColumnDefinition(_columnManager, COLUMN_CONSUMED_CONTAINERS, SWT.LEAD);

      _colDef_ConsumedBeverageContainers.setColumnLabel(COLUMN_CONSUMED_CONTAINERS);
      _colDef_ConsumedBeverageContainers.setColumnHeaderText(Messages.Tour_Nutrition_Column_ConsumedContainers);
      _colDef_ConsumedBeverageContainers.setColumnHeaderToolTipText(Messages.Tour_Nutrition_Column_ConsumedContainers_Tooltip);

      _colDef_ConsumedBeverageContainers.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(27));

      _colDef_ConsumedBeverageContainers.setIsDefaultColumn();
      _colDef_ConsumedBeverageContainers.setCanModifyVisibility(false);

      _colDef_ConsumedBeverageContainers.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourNutritionProduct tourNutritionProduct = (TourNutritionProduct) cell.getElement();

            final TourBeverageContainer tourBeverageContainer = tourNutritionProduct.getTourBeverageContainer();
            if (tourBeverageContainer == null) {
               return;
            }

            cell.setText(String.valueOf(tourNutritionProduct.getContainersConsumed()));
         }
      });
   }

   @Override
   public void dispose() {

      TourManager.getInstance().removeTourEventListener(_tourEventListener);

      getSite().getPage().removePostSelectionListener(_postSelectionListener);
      getViewSite().getPage().removePartListener(_partListener);

      _prefStore.removePropertyChangeListener(_prefChangeListener);

      UI.disposeResource(_imageAdd);
      UI.disposeResource(_imageSearch);
      UI.disposeResource(_imageDelete);

      super.dispose();
   }

   @Override
   public ColumnManager getColumnManager() {
      return _columnManager;
   }

   private List<TourNutritionProduct> getSelectedProducts() {

      final StructuredSelection selection = (StructuredSelection) _productsViewer.getSelection();

      final List<TourNutritionProduct> tourNutritionProducts = new ArrayList<>();

      for (final Object object : selection.toList()) {

         if (object instanceof final TourNutritionProduct tourNutritionProduct) {
            tourNutritionProducts.add(tourNutritionProduct);
         }
      }

      return tourNutritionProducts;
   }

   @Override
   public ColumnViewer getViewer() {

      return _productsViewer;
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      _columnSortListener = widgetSelectedAdapter(selectionEvent -> onSelect_SortColumn(selectionEvent));
   }

   private void onDeleteProducts() {

      final List<TourNutritionProduct> selectedProducts = getSelectedProducts();

      if (selectedProducts.isEmpty()) {
         return;
      }

      final Set<TourNutritionProduct> tourNutritionProducts = _tourData.getTourNutritionProducts();
      tourNutritionProducts.removeIf(tourNutritionProduct -> selectedProducts.stream().anyMatch(selectedProduct -> selectedProduct
            .getProductCode().equals(tourNutritionProduct.getProductCode())));

      _tourData.setTourNutritionProducts(tourNutritionProducts);
      _tourData = TourManager.saveModifiedTour(_tourData);
   }

   private void onSelect_SortColumn(final SelectionEvent e) {

      _viewerContainer.setRedraw(false);
      {
         // keep selection
         final ISelection selectionBackup = _productsViewer.getSelection();

         // toggle sorting
         _tourNutritionProductComparator.setSortColumn(e.widget);
         _productsViewer.refresh();

         // reselect selection
         _productsViewer.setSelection(selectionBackup, true);
         _productsViewer.getTable().showSelection();
      }
      _viewerContainer.setRedraw(true);
   }

   private void onSelectionChanged(final ISelection selection) {

      if (selection == null) {
         return;
      }

      long tourId = TourDatabase.ENTITY_IS_NOT_SAVED;
      TourData tourData = null;

      if (selection instanceof final SelectionTourData tourDataSelection) {

         // a tour was selected, get the chart and update the nutrition viewer
         tourData = tourDataSelection.getTourData();

      } else if (selection instanceof final SelectionTourId selectionTourId) {

         tourId = selectionTourId.getTourId();

      } else if (selection instanceof final SelectionTourIds selectionTourIds) {

         final ArrayList<Long> tourIds = selectionTourIds.getTourIds();

         if (tourIds != null && tourIds.size() > 0) {

            if (tourIds.size() == 1) {
               tourId = tourIds.get(0);
            } else {
               tourData = TourManager.createJoinedTourData(tourIds);
            }
         }

      } else if (selection instanceof SelectionDeletedTours) {

         clearView();
      }

      if (tourData == null) {

         if (tourId >= TourDatabase.ENTITY_IS_NOT_SAVED) {

            tourData = TourManager.getInstance().getTourData(tourId);
            if (tourData != null) {
               _tourData = tourData;
            }
         }
      } else {

         _tourData = tourData;
      }

      updateUI_ProductViewer();
   }

   private void onTableSelectionChanged(final SelectionChangedEvent event) {

      final StructuredSelection selection = (StructuredSelection) _productsViewer.getSelection();

      _btnDeleteProduct.setEnabled(selection.size() > 0);
   }

   @Override
   public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

      _viewerContainer.setRedraw(false);
      {
         _productsViewer.getTable().dispose();

         createUI_220_Viewer(_viewerContainer);
         _viewerContainer.layout();

         // update the viewer
         reloadViewer();
      }
      _viewerContainer.setRedraw(true);

      return _productsViewer;
   }

   private void refreshTourData(final TourData tourData) {
      // get modified tour
      _tourData = tourData;

      updateUI_ProductViewer();

      // removed old tour data from the selection provider
      _postSelectionProvider.clearSelection();
   }

   @Override
   public void reloadViewer() {
      updateUI_ProductViewer();
   }

   private void restoreState() {

      // restore old used queries
      final String[] stateSearchedQueries = _state.getArray(STATE_PRODUCT_SEARCHES_HISTORY);
      if (stateSearchedQueries != null) {
         Stream.of(stateSearchedQueries).forEach(query -> _searchHistory.add(query));
      }

      _sectionSummary.setExpanded(Util.getStateBoolean(_state, STATE_SECTION_SUMMARY, true));
      _sectionProductsList.setExpanded(Util.getStateBoolean(_state, STATE_SECTION_PRODUCTS_LIST, true));
   }

   @PersistState
   private void saveState() {

      _state.put(STATE_PRODUCT_SEARCHES_HISTORY, _searchHistory.toArray(new String[_searchHistory.size()]));
      _state.put(STATE_SECTION_SUMMARY, _sectionSummary.isExpanded());
      _state.put(STATE_SECTION_PRODUCTS_LIST, _sectionProductsList.isExpanded());
   }

   private void setColumnsEditingSupport() {

      _colDef_ConsumedQuantity.setEditingSupport(new EditingSupport(_productsViewer) {

         private SpinnerCellEditor spinnerCellEditor = new SpinnerCellEditor(_productsViewer.getTable(),
               _nf2,
               new RangeContent(0, 0, 0),
               SWT.NONE);

         @Override
         protected boolean canEdit(final Object element) {
            return true;
         }

         @Override
         protected CellEditor getCellEditor(final Object element) {

            //todo fb
            //it works but it's when i use the mousewheel that it doesn't!?!? at least on windows, to test on linux !?
            //wow, it's all over the place as there are 3 possible ways
            //- mouse wheel on the number
            //- mouse wheel on the -/+ signs
            //- mouse wheel CLICK on the -/+ signs
            //ask wolfgang in the PR and show this line where I configured the increment etc...
            final Spinner spinner = spinnerCellEditor.getSpinner();
            spinner.setMinimum(25);
            spinner.setMaximum(10000);
            spinner.setIncrement(25);
            spinner.addMouseWheelListener(mouseEvent -> Util.adjustSpinnerValueOnMouseScroll(mouseEvent));

            return spinnerCellEditor;
         }

         @Override
         protected Object getValue(final Object element) {
            return ((TourNutritionProduct) element).getConsumedQuantity();
         }

         @Override
         protected void setValue(final Object element, final Object value) {

            final float consumedAmount = ((Double) value).floatValue();

            ((TourNutritionProduct) element).setConsumedQuantity(consumedAmount);
            _tourData = TourManager.saveModifiedTour(_tourData);
         }
      });

      final String[] quantityTypeItems = new String[2];
      quantityTypeItems[0] = Messages.Tour_Nutrition_Label_QuantityType_Servings;
      quantityTypeItems[1] = Messages.Tour_Nutrition_Label_QuantityType_Products;
      _colDef_QuantityType.setEditingSupport(new EditingSupport(_productsViewer) {

         private ComboBoxCellEditor comboBoxCellEditor = new ComboBoxCellEditor(_productsViewer.getTable(), quantityTypeItems, SWT.READ_ONLY);

         @Override
         protected boolean canEdit(final Object element) {
            return true;
         }

         @Override
         protected CellEditor getCellEditor(final Object element) {
            return comboBoxCellEditor;
         }

         @Override
         protected Object getValue(final Object element) {

            final TourNutritionProduct tourNutritionProduct = (TourNutritionProduct) element;
            final QuantityType quantityType = tourNutritionProduct.getQuantityType();
            return quantityType == null ? 0 : QuantityType.valueOf(String.valueOf(quantityType)).ordinal();
         }

         @Override
         protected void setValue(final Object element, final Object value) {

            final int quantityTypeIndex = (int) value;

            final TourNutritionProduct tourNutritionProduct = (TourNutritionProduct) element;
            final int currentQuantityTypeIndex = QuantityType.valueOf(String.valueOf(tourNutritionProduct.getQuantityType())).ordinal();

            if (currentQuantityTypeIndex == quantityTypeIndex) {
               //No need to update the tour since the new value is the same as the current one.
               return;
            }

            tourNutritionProduct.setQuantityType(QuantityType.values()[quantityTypeIndex]);

            //Trigger the update of the calories and sodium values
            _colDef_Calories.setColumnLabel(UI.EMPTY_STRING);
            _colDef_Sodium.setColumnLabel(UI.EMPTY_STRING);
            _tourData = TourManager.saveModifiedTour(_tourData);
         }
      });

      _colDef_Name.setEditingSupport(new No_EditingSupport());
      _colDef_Calories.setEditingSupport(new No_EditingSupport());
      _colDef_Sodium.setEditingSupport(new No_EditingSupport());

      _colDef_IsBeverage.setEditingSupport(new EditingSupport(_productsViewer) {

         private CheckboxCellEditor checkboxCellEditor = new CheckboxCellEditor(_productsViewer.getTable());

         @Override
         protected boolean canEdit(final Object element) {
            return false;
         }

         @Override
         protected CellEditor getCellEditor(final Object element) {
            return checkboxCellEditor;
         }

         @Override
         protected Object getValue(final Object element) {
            return ((TourNutritionProduct) element).isBeverage();
         }

         @Override
         protected void setValue(final Object element, final Object value) {}
      });

      final String[] tourBeverageContainersItems = new String[_tourBeverageContainers.size() + 1];
      tourBeverageContainersItems[0] = UI.EMPTY_STRING;
      for (int index = 0; index < _tourBeverageContainers.size(); ++index) {

         tourBeverageContainersItems[index + 1] = NutritionUtils.buildTourBeverageContainerName(_tourBeverageContainers.get(index));
      }
      _colDef_BeverageContainer.setEditingSupport(new EditingSupport(_productsViewer) {

         private ComboBoxCellEditor comboBoxCellEditor = new ComboBoxCellEditor(_productsViewer.getTable(),
               tourBeverageContainersItems,
               SWT.READ_ONLY);

         @Override
         protected boolean canEdit(final Object element) {
            return _tourBeverageContainers.size() > 0;
         }

         @Override
         protected CellEditor getCellEditor(final Object element) {
            return comboBoxCellEditor;
         }

         @Override
         protected Object getValue(final Object element) {

            //todo fb
            return 0;// task.getTourBeverageContainerName();
         }

         @Override
         protected void setValue(final Object element, final Object value) {

            final int beverageContainerIndex = (int) value;

            final TourBeverageContainer selectedTourBeverageContainer = beverageContainerIndex == 0
                  ? null
                  : _tourBeverageContainers.get(beverageContainerIndex - 1);

            final TourNutritionProduct tourNutritionProduct = (TourNutritionProduct) element;
            tourNutritionProduct.setTourBeverageContainer(selectedTourBeverageContainer);
            tourNutritionProduct.setContainersConsumed(1);
            _tourData = TourManager.saveModifiedTour(_tourData);
         }
      });

      _colDef_ConsumedBeverageContainers.setEditingSupport(new EditingSupport(_productsViewer) {

         private SpinnerCellEditor spinnerCellEditor = new SpinnerCellEditor(_productsViewer.getTable(), _nf2, _quantityRange, SWT.NONE);

         @Override
         protected boolean canEdit(final Object element) {
            return _tourBeverageContainers.size() > 0;
         }

         @Override
         protected CellEditor getCellEditor(final Object element) {
            //       spinnerCellEditor.getSpinner().setMinimum(columnAdvisor.getMinCount(element, columnIndex));
            return spinnerCellEditor;
         }

         @Override
         protected Object getValue(final Object element) {
            return ((TourNutritionProduct) element).getContainersConsumed();
         }

         @Override
         protected void setValue(final Object element, final Object value) {

            final float consumedAmount = ((Double) value).floatValue();

            ((TourNutritionProduct) element).setContainersConsumed(consumedAmount);
            _tourData = TourManager.saveModifiedTour(_tourData);
         }
      });
   }

   @Override
   public void setFocus() {}

   private void showInvalidPage() {

      _pageBook.showPage(_pageNoData);
   }

   private void showTourFromTourProvider() {

      _pageBook.showPage(_pageNoData);

      // a tour is not displayed, find a tour provider which provides a tour
      Display.getCurrent().asyncExec(() -> {

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

         onSelectionChanged(TourManager.getSelectedToursSelection());
      });
   }

   @Override
   public void updateColumnHeader(final ColumnDefinition colDef) {}

   private void updateUI_ProductViewer() {

      if (_tourData == null) {

         _pageBook.showPage(_pageNoData);

      } else {

         _isInUpdate = true;
         {
            _productsViewer.setInput(new Object[0]);
         }
         _isInUpdate = false;

         _pageBook.showPage(_viewerContainer);

         updateUI_SummaryFromModel();
      }
   }

   private void updateUI_SummaryFromModel() {

      final Set<TourNutritionProduct> tourNutritionProducts = _tourData.getTourNutritionProducts();

      final int totalCalories = NutritionUtils.getTotalCalories(tourNutritionProducts);
      _txtCalories_Total.setText(String.valueOf(totalCalories));

      final int totalFluid = Math.round(NutritionUtils.getTotalFluids(tourNutritionProducts));
      _txtFluid_Total.setText(String.valueOf(totalFluid));

      final int totalSodium = (int) NutritionUtils.getTotalSodium(tourNutritionProducts);
      _txtSodium_Total.setText(String.valueOf(totalSodium));

      final long averageCaloriesPerHour = computeAveragePerHour(totalCalories);
      _txtCalories_Average.setText(String.valueOf(averageCaloriesPerHour));

      final long averageFluidPerHour = computeAveragePerHour(totalFluid);
      _txtFluid_Average.setText(String.valueOf(averageFluidPerHour));

      final long averageSodiumPerHour = computeAveragePerHour(totalSodium);
      _txtSodium_Average.setText(String.valueOf(averageSodiumPerHour));
   }

}
