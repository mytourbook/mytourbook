/*******************************************************************************
 * Copyright (C) 2024 Frédéric Bard
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
package net.tourbook.ui.views.nutrition;

import static org.eclipse.swt.events.KeyListener.keyPressedAdapter;
import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.OtherMessages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
import net.tourbook.common.UI;
import net.tourbook.common.formatter.FormatManager;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnDefinitionFor1stVisibleAlignmentColumn;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.ColumnProfile;
import net.tourbook.common.util.IContextMenuProvider;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.TableColumnDefinition;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourBeverageContainer;
import net.tourbook.data.TourData;
import net.tourbook.data.TourNutritionProduct;
import net.tourbook.database.TourDatabase;
import net.tourbook.nutrition.DialogCustomTourNutritionProduct;
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

import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
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
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

import cop.swt.widgets.viewers.table.celleditors.RangeContent;
import cop.swt.widgets.viewers.table.celleditors.SpinnerCellEditor;

public class TourNutritionView extends ViewPart implements ITourViewer {

   public static final String            ID                             = "net.tourbook.ui.views.nutrition.TourNutritionView"; //$NON-NLS-1$
   private static final String           STATE_PRODUCT_SEARCHES_HISTORY = "products.searchesHistory";                          //$NON-NLS-1$
   private static final String           STATE_SECTION_PRODUCTS_LIST    = "STATE_SECTION_PRODUCTS_LIST";                       //$NON-NLS-1$
   private static final String           STATE_SECTION_SUMMARY          = "STATE_SECTION_SUMMARY";                             //$NON-NLS-1$

   private static final String           COLUMN_CONSUMED_QUANTITY       = "ConsumedQuantity";                                  //$NON-NLS-1$
   private static final String           COLUMN_QUANTITY_TYPE           = "QuantityType";                                      //$NON-NLS-1$
   private static final String           COLUMN_NAME                    = "Name";                                              //$NON-NLS-1$
   private static final String           COLUMN_CALORIES                = "Calories";                                          //$NON-NLS-1$
   private static final String           COLUMN_SODIUM                  = "Sodium";                                            //$NON-NLS-1$
   private static final String           COLUMN_ISBEVERAGE              = "IsBeverage";                                        //$NON-NLS-1$
   private static final String           COLUMN_BEVERAGE_QUANTITY       = "BeverageQuantity";                                  //$NON-NLS-1$
   private static final String           COLUMN_BEVERAGE_CONTAINER      = "BeverageContainer";                                 //$NON-NLS-1$
   private static final String           COLUMN_CONSUMED_CONTAINERS     = "ConsumedContainers";                                //$NON-NLS-1$

   private static final IPreferenceStore _prefStore                     = TourbookPlugin.getPrefStore();

   private static final int              HINT_TEXT_COLUMN_WIDTH         = UI.IS_OSX ? 100 : 50;

   private final NumberFormat            _nf2                           = NumberFormat.getNumberInstance();
   {
      _nf2.setMinimumFractionDigits(0);
      _nf2.setMaximumFractionDigits(2);
   }

   private final IDialogSettings          _state                            = TourbookPlugin.getState(ID);
   private TourData                       _tourData;

   private PixelConverter                 _pc;

   private TableViewer                    _productsViewer;
   private ColumnManager                  _columnManager;
   private MenuManager                    _viewerMenuManager;
   private IContextMenuProvider           _tableViewerContextMenuProvider   = new TableContextMenuProvider();

   private TableColumnDefinition          _colDef_ConsumedQuantity;
   private TableColumnDefinition          _colDef_QuantityType;
   private TableColumnDefinition          _colDef_Name;
   private TableColumnDefinition          _colDef_Calories;
   private TableColumnDefinition          _colDef_Sodium;
   private TableColumnDefinition          _colDef_IsBeverage;
   /**
    * Index of the column with the image, index can be changed when the columns are reordered with
    * the mouse or the column manager
    */
   private int                            _columnIndex_ForColumn_IsBeverage = -1;
   private int                            _columnWidth_ForColumn_IsBeverage;

   private TableColumnDefinition          _colDef_BeverageContainer;
   private TableColumnDefinition          _colDef_ConsumedBeverageContainers;

   private TourNutritionProductComparator _tourNutritionProductComparator   = new TourNutritionProductComparator();
   private List<TourBeverageContainer>    _tourBeverageContainers           = new ArrayList<>();

   private List<String>                   _searchHistory                    = new ArrayList<>();
   private IPropertyChangeListener        _prefChangeListener;
   private SelectionListener              _columnSortListener;

   private ISelectionListener             _postSelectionListener;
   private ITourEventListener             _tourEventListener;
   private IPartListener2                 _partListener;
   private PostSelectionProvider          _postSelectionProvider;

   private final RangeContent             _quantityRange                    = new RangeContent(0.25, 10.0, 0.25, 100);

   /*
    * UI controls
    */
   private Image                     _imageAdd     = TourbookPlugin.getImageDescriptor(Images.App_Add).createImage();
   private Image                     _imageSearch  = TourbookPlugin.getImageDescriptor(Images.SearchTours).createImage();

   private Image                     _imageCheck   = TourbookPlugin.getImageDescriptor(Images.Checkbox_Checked).createImage();
   private Image                     _imageUncheck = TourbookPlugin.getImageDescriptor(Images.Checkbox_Uncheck).createImage();
   private Image                     _imageYes     = CommonActivator.getImageDescriptor(CommonImages.App_Yes).createImage();

   private PageBook                  _pageBook;
   private Composite                 _pageNoData;
   private Composite                 _pageContent;
   private Composite                 _viewerContainer;

   private boolean                   _isInUpdate;
   private Label                     _lblCalories_Average;
   private Label                     _lblCalories_Total;
   private Label                     _lblFluid_Average;
   private Label                     _lblFluid_Total;
   private Label                     _lblSodium_Average;
   private Label                     _lblSodium_Total;
   private Section                   _sectionProductsList;
   private Section                   _sectionSummary;
   private FormToolkit               _tk;
   private Menu                      _tableContextMenu;

   private ActionDeleteProducts      _actionDeleteProducts;
   private ActionOpenProductsWebsite _actionOpenProductsWebsite;

   private class ActionDeleteProducts extends Action {

      public ActionDeleteProducts() {

         super(Messages.Tour_Nutrition_Button_DeleteProduct, AS_PUSH_BUTTON);
         setToolTipText(Messages.Tour_Nutrition_Button_DeleteProduct_Tooltip);
      }

      @Override
      public void run() {
         onDeleteProducts();
      }
   }

   private final class ConsumedBeverageContainersEditingSupport extends EditingSupport {

      private SpinnerCellEditor spinnerCellEditor;

      private ConsumedBeverageContainersEditingSupport() {

         super(_productsViewer);

         spinnerCellEditor = new SpinnerCellEditor(_productsViewer.getTable(),
               _nf2,
               _quantityRange,
               SWT.NONE);

         final Spinner spinner = spinnerCellEditor.getControl();
         spinner.setMinimum(25);
         spinner.setMaximum(10000);
         spinner.setIncrement(25);
         spinner.setPageIncrement(100);
         spinner.addMouseWheelListener(mouseEvent -> UI.adjustSpinnerValueOnMouseScroll(mouseEvent, 24));
      }

      @Override
      protected boolean canEdit(final Object element) {

         final TourNutritionProduct tourNutritionProduct = (TourNutritionProduct) element;
         return tourNutritionProduct.getTourBeverageContainer() != null;
      }

      @Override
      protected CellEditor getCellEditor(final Object element) {
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
   }

   private final class ConsumedQuantityEditingSupport extends EditingSupport {

      private SpinnerCellEditor spinnerCellEditor;

      private ConsumedQuantityEditingSupport() {

         super(_productsViewer);

         spinnerCellEditor = new SpinnerCellEditor(_productsViewer.getTable(),
               _nf2,
               new RangeContent(0, 0, 0),
               SWT.NONE);

         final Spinner spinner = spinnerCellEditor.getControl();
         spinner.setMinimum(25);
         spinner.setMaximum(10000);
         spinner.setIncrement(25);
         spinner.setPageIncrement(100);
         spinner.addMouseWheelListener(mouseEvent -> UI.adjustSpinnerValueOnMouseScroll(mouseEvent, 24));
      }

      @Override
      protected boolean canEdit(final Object element) {
         return true;
      }

      @Override
      protected CellEditor getCellEditor(final Object element) {
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
   }

   private final class NoEditingSupport extends EditingSupport {

      private NoEditingSupport() {
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

   private class TableContextMenuProvider implements IContextMenuProvider {

      @Override
      public void disposeContextMenu() {

         if (_tableContextMenu != null) {
            _tableContextMenu.dispose();
         }
      }

      @Override
      public Menu getContextMenu() {

         return _productsViewer.getTable().getSelectionCount() > 0
               ? _tableContextMenu : null;
      }

      @Override
      public Menu recreateContextMenu() {

         disposeContextMenu();

         _tableContextMenu = createUI_32_CreateViewerContextMenu();

         return _tableContextMenu;
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
            rc = tnp1.getName().compareToIgnoreCase(tnp2.getName());
            break;

         }

         if (rc == 0) {

            // subsort 1 by category
            rc = tnp1.getName().compareToIgnoreCase(tnp2.getName());
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
      public Object[] getElements(final Object parent) {

         if (_tourData == null || _tourData.isMultipleTours()) {
            return new Object[0];
         }

         return _tourData.getTourNutritionProducts().toArray();
      }
   }

   private void addPartListener() {

      _partListener = new IPartListener2() {};
      getViewSite().getPage().addPartListener(_partListener);
   }

   private void addPrefListener() {

      _prefChangeListener = propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

            _productsViewer.getTable().setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));
            _productsViewer.refresh();

         } else if (property.equals(ITourbookPreferences.NUTRITION_BEVERAGECONTAINERS_HAVE_CHANGED)) {

            _tourBeverageContainers = TourDatabase.getTourBeverageContainers();
            refreshTourData(TourManager.getTour(_tourData.getTourId()));

            recreateViewer(getViewer());

         } else if (property.equals(ITourbookPreferences.NUTRITION_IGNORE_FIRST_HOUR)) {

            recreateViewer(getViewer());
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

         handleTourEvent(tourEventId, eventData);
      };

      TourManager.getInstance().addTourEventListener(_tourEventListener);
   }

   private void clearView() {

      _tourData = null;

      updateUI_ProductViewer();

      _postSelectionProvider.clearSelection();
   }

   private void createActions() {

      _actionDeleteProducts = new ActionDeleteProducts();
      _actionOpenProductsWebsite = new ActionOpenProductsWebsite();
   }

   private void createMenuManager() {

      _viewerMenuManager = new MenuManager("#PopupMenu"); //$NON-NLS-1$
      _viewerMenuManager.setRemoveAllWhenShown(true);
      _viewerMenuManager.addMenuListener(manager -> fillContextMenu(manager));
   }

   @Override
   public void createPartControl(final Composite parent) {

      initUI(parent);

      // define all columns for the viewer
      _columnManager = new ColumnManager(this, _state);
      defineAllColumns();

      createActions();
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
                                 final String title,
                                 final boolean isGrabVertical) {

      final Section section = tk.createSection(parent, ExpandableComposite.TWISTIE | ExpandableComposite.TITLE_BAR);

      section.setText(title);
      GridDataFactory.fillDefaults().grab(true, isGrabVertical).applyTo(section);

      final Composite sectionContainer = tk.createComposite(section);
      section.setClient(sectionContainer);

      return section;
   }

   private void createUI(final Composite parent) {

      _pageBook = new PageBook(parent, SWT.NONE);

      _tourBeverageContainers = TourDatabase.getTourBeverageContainers();

      _pageNoData = net.tourbook.common.UI.createUI_PageNoData(_pageBook, Messages.UI_Label_no_chart_is_selected);

      _tk = new FormToolkit(_pageBook.getDisplay());

      _pageContent = new Composite(_pageBook, SWT.NONE);
      _pageContent.setLayout(new GridLayout());
      {
         createUI_Section_10_Summary(_pageContent);
         createUI_Section_20_ProductsList(_pageContent);
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
            Label label = UI.createLabel(container);
            GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).applyTo(label);

            label = UI.createLabel(container, Messages.Tour_Nutrition_Label_Calories);
            GridDataFactory.fillDefaults().span(2, 1).align(SWT.CENTER, SWT.FILL).applyTo(label);

            label = UI.createLabel(container, Messages.Tour_Nutrition_Label_Fluids);
            GridDataFactory.fillDefaults().span(2, 1).align(SWT.CENTER, SWT.FILL).applyTo(label);

            label = UI.createLabel(container, Messages.Tour_Nutrition_Label_Sodium);
            GridDataFactory.fillDefaults().span(2, 1).align(SWT.CENTER, SWT.FILL).applyTo(label);
         }

         /*
          * Totals
          */
         {
            final Label label = UI.createLabel(container, Messages.Tour_Nutrition_Label_Totals, Messages.Tour_Nutrition_Label_Totals_Tooltip);
            GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.CENTER).applyTo(label);

            _lblCalories_Total = _tk.createLabel(container, UI.EMPTY_STRING, SWT.TRAIL);
            GridDataFactory.fillDefaults()
                  .hint(HINT_TEXT_COLUMN_WIDTH, SWT.DEFAULT)
                  .align(SWT.END, SWT.FILL)
                  .applyTo(_lblCalories_Total);

            // Unit: kcal
            UI.createLabel(container, OtherMessages.VALUE_UNIT_K_CALORIES);

            _lblFluid_Total = _tk.createLabel(container, UI.EMPTY_STRING, SWT.TRAIL);
            GridDataFactory.fillDefaults()
                  .hint(HINT_TEXT_COLUMN_WIDTH, SWT.DEFAULT)
                  .align(SWT.END, SWT.FILL)
                  .applyTo(_lblFluid_Total);

            // Unit: L
            UI.createLabel(container, UI.UNIT_FLUIDS_L);

            _lblSodium_Total = _tk.createLabel(container, UI.EMPTY_STRING, SWT.TRAIL);
            GridDataFactory.fillDefaults()
                  .hint(HINT_TEXT_COLUMN_WIDTH, SWT.DEFAULT)
                  .align(SWT.END, SWT.FILL)
                  .applyTo(_lblSodium_Total);

            // Unit: mg
            UI.createLabel(container, UI.UNIT_WEIGHT_MG);
         }

         /*
          * Averages
          */
         {
            Label label = UI.createLabel(container, Messages.Tour_Nutrition_Label_Averages, Messages.Tour_Nutrition_Label_Averages_Tooltip);
            GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.CENTER).applyTo(label);

            _lblCalories_Average = _tk.createLabel(container, UI.EMPTY_STRING, SWT.TRAIL);
            GridDataFactory.fillDefaults().hint(HINT_TEXT_COLUMN_WIDTH, SWT.DEFAULT).align(SWT.END, SWT.FILL)
                  .applyTo(_lblCalories_Average);

            // Unit: kcal/h
            UI.createLabel(container, OtherMessages.VALUE_UNIT_K_CALORIES + UI.SLASH + UI.UNIT_LABEL_TIME);

            _lblFluid_Average = _tk.createLabel(container, UI.EMPTY_STRING, SWT.TRAIL);
            GridDataFactory.fillDefaults().hint(HINT_TEXT_COLUMN_WIDTH, SWT.DEFAULT).align(SWT.END, SWT.FILL)
                  .applyTo(_lblFluid_Average);

            // Unit: L/h
            UI.createLabel(container, UI.UNIT_FLUIDS_L + UI.SLASH + UI.UNIT_LABEL_TIME);

            _lblSodium_Average = _tk.createLabel(container, UI.EMPTY_STRING, SWT.TRAIL);
            _lblSodium_Average.setToolTipText(Messages.Tour_Nutrition_Text_AverageSodium_Tooltip);
            GridDataFactory.fillDefaults().hint(HINT_TEXT_COLUMN_WIDTH, SWT.DEFAULT).align(SWT.END, SWT.FILL)
                  .applyTo(_lblSodium_Average);

            // Unit: mg/L
            label = UI.createLabel(container, UI.UNIT_WEIGHT_MG + UI.SLASH + UI.UNIT_FLUIDS_L);
            label.setToolTipText(Messages.Tour_Nutrition_Text_AverageSodium_Tooltip);
         }
      }
   }

   private void createUI_210_Actions(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {

         /*
          * Search product button
          */
         final Button btnSearchProduct = new Button(container, SWT.NONE);
         btnSearchProduct.setText(Messages.Tour_Nutrition_Button_SearchProduct);
         btnSearchProduct.setToolTipText(Messages.Tour_Nutrition_Button_SearchProduct_Tooltip);
         btnSearchProduct.addSelectionListener(widgetSelectedAdapter(selectionEvent -> new DialogSearchProduct(Display.getCurrent().getActiveShell(),
               _tourData.getTourId()).open()));
         btnSearchProduct.setImage(_imageSearch);
         GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(btnSearchProduct);

         /*
          * Add product button
          */
         final Button btnAddCustomProduct = new Button(container, SWT.NONE);
         btnAddCustomProduct.setText(Messages.Tour_Nutrition_Button_AddCustomProduct);
         btnAddCustomProduct.setToolTipText(Messages.Tour_Nutrition_Button_AddCustomProduct_Tooltip);
         btnAddCustomProduct.addSelectionListener(widgetSelectedAdapter(selectionEvent -> {

            final DialogCustomTourNutritionProduct dialogTourNutritionProduct = new DialogCustomTourNutritionProduct(Display.getCurrent()
                  .getActiveShell());

            if (dialogTourNutritionProduct.open() != Window.OK) {
               return;
            }

            final Set<TourNutritionProduct> tourNutritionProducts = _tourData.getTourNutritionProducts();
            tourNutritionProducts.add(dialogTourNutritionProduct.getTourNutritionProduct(_tourData));
            _tourData.setTourNutritionProducts(tourNutritionProducts);
            _tourData = TourManager.saveModifiedTour(_tourData);
            _tourData.setTourNutritionProducts(_tourData.getTourNutritionProducts());

         }));
         btnAddCustomProduct.setImage(_imageAdd);
         GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(btnAddCustomProduct);
      }
   }

   private void createUI_220_Viewer(final Composite parent) {

      /*
       * table viewer: products
       */
      final Table productsTable = new Table(parent, SWT.MULTI | SWT.FULL_SELECTION);
      GridLayoutFactory.fillDefaults().applyTo(productsTable);
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

      final Listener doubleClickListener = event -> {

         if (event.type == SWT.MouseDoubleClick) {

            final Point point = new Point(event.x, event.y);
            final ViewerCell cell = _productsViewer.getCell(point);
            if (cell == null || cell.getColumnIndex() != 2) {
               return;
            }

            onOpenProductsWebsite();
         }
      };
      _productsViewer.getTable().addListener(SWT.MouseDoubleClick, doubleClickListener);

      _productsViewer.getTable().addKeyListener(keyPressedAdapter(keyEvent -> {

         if (keyEvent.keyCode == SWT.DEL) {
            onDeleteProducts();
         }
      }));

      createUI_230_ColumnImages(productsTable);

      createUI_30_ContextMenu();
   }

   private void createUI_230_ColumnImages(final Table table) {

      boolean isColumnVisible = false;
      final ControlListener controlResizedAdapter = ControlListener.controlResizedAdapter(controlEvent -> setWidth_ForColumn_IsBeverage());

      // update column index which is needed for repainting
      final ColumnProfile activeProfile = _columnManager.getActiveProfile();
      _columnIndex_ForColumn_IsBeverage = activeProfile.getColumnIndex(_colDef_IsBeverage.getColumnId());

      final int numColumns = table.getColumns().length;

      // add column resize listener
      if (_columnIndex_ForColumn_IsBeverage >= 0 && _columnIndex_ForColumn_IsBeverage < numColumns) {

         isColumnVisible = true;
         table.getColumn(_columnIndex_ForColumn_IsBeverage).addControlListener(controlResizedAdapter);
      }

      // add table listener
      if (isColumnVisible) {

         final Listener paintListener = event -> {

            // paint images for the correct column
            if (event.index != _columnIndex_ForColumn_IsBeverage ||
                  event.type != SWT.PaintItem) {
               return;
            }

            onPaint_Viewer_IsBeverageImage(event);
         };

         table.addControlListener(controlResizedAdapter);
         table.addListener(SWT.PaintItem, paintListener);
      }
   }

   /**
    * create the views context menu
    */
   private void createUI_30_ContextMenu() {

      _tableContextMenu = createUI_32_CreateViewerContextMenu();

      final Table table = (Table) _productsViewer.getControl();

      _columnManager.createHeaderContextMenu(table, _tableViewerContextMenuProvider);
   }

   private Menu createUI_32_CreateViewerContextMenu() {

      final Table table = (Table) _productsViewer.getControl();
      final Menu tableContextMenu = _viewerMenuManager.createContextMenu(table);

      return tableContextMenu;
   }

   private void createUI_Section_10_Summary(final Composite parent) {

      _sectionSummary = createSection(parent, _tk, Messages.Tour_Nutrition_Section_Summary, false);
      _sectionSummary.setToolTipText(Messages.Tour_Nutrition_Section_Summary_Tooltip);
      final Composite container = (Composite) _sectionSummary.getClient();
      GridLayoutFactory.fillDefaults().applyTo(container);
      {
         createUI_110_Report(container);

         /*
          * Empty label to put empty space between this section and the next one
          */
         UI.createLabel(container);
      }
   }

   private void createUI_Section_20_ProductsList(final Composite parent) {

      _sectionProductsList = createSection(parent, _tk, Messages.Tour_Nutrition_Section_ProductsList, true);
      _sectionProductsList.setToolTipText(Messages.Tour_Nutrition_Section_ProductsList_Tooltip);
      final Composite container = (Composite) _sectionProductsList.getClient();
      GridDataFactory.fillDefaults().applyTo(container);
      GridLayoutFactory.fillDefaults().applyTo(container);
      {
         createUI_210_Actions(container);

         _viewerContainer = new Composite(container, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, true).applyTo(_viewerContainer);
         GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
         {
            createUI_220_Viewer(_viewerContainer);
         }
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

      new ColumnDefinitionFor1stVisibleAlignmentColumn(_columnManager);
   }

   private void defineColumn_10_ConsumedQuantity() {

      _colDef_ConsumedQuantity = new TableColumnDefinition(_columnManager, COLUMN_CONSUMED_QUANTITY, SWT.TRAIL);

      _colDef_ConsumedQuantity.setColumnLabel(Messages.Tour_Nutrition_Column_ConsumedQuantity);
      _colDef_ConsumedQuantity.setColumnHeaderText(Messages.Tour_Nutrition_Column_ConsumedQuantity);
      _colDef_ConsumedQuantity.setColumnHeaderToolTipText(Messages.Tour_Nutrition_Column_ConsumedQuantity_Tooltip);

      _colDef_ConsumedQuantity.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(15));

      _colDef_ConsumedQuantity.setIsDefaultColumn();
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

      _colDef_QuantityType = new TableColumnDefinition(_columnManager, COLUMN_QUANTITY_TYPE, SWT.TRAIL);

      _colDef_QuantityType.setColumnLabel(Messages.Tour_Nutrition_Column_QuantityType);
      _colDef_QuantityType.setColumnHeaderText(Messages.Tour_Nutrition_Column_QuantityType);
      _colDef_QuantityType.setColumnHeaderToolTipText(Messages.Tour_Nutrition_Column_QuantityType_Tooltip);

      _colDef_QuantityType.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(17));

      _colDef_QuantityType.setIsDefaultColumn();

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

      _colDef_Name = new TableColumnDefinition(_columnManager, COLUMN_NAME, SWT.TRAIL);

      _colDef_Name.setColumnLabel(Messages.Tour_Nutrition_Column_Name);
      _colDef_Name.setColumnHeaderText(Messages.Tour_Nutrition_Column_Name);

      _colDef_Name.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(50));

      _colDef_Name.setIsDefaultColumn();
      _colDef_Name.setColumnSelectionListener(_columnSortListener);

      _colDef_Name.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourNutritionProduct tourNutritionProduct = (TourNutritionProduct) cell.getElement();

            cell.setText(tourNutritionProduct.getName());
         }
      });
   }

   private void defineColumn_40_Calories() {

      _colDef_Calories = new TableColumnDefinition(_columnManager, COLUMN_CALORIES, SWT.TRAIL);

      _colDef_Calories.setColumnLabel(Messages.Tour_Nutrition_Label_Calories);
      _colDef_Calories.setColumnHeaderText(Messages.Tour_Nutrition_Label_Calories);

      _colDef_Calories.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(12));

      _colDef_Calories.setIsDefaultColumn();

      _colDef_Calories.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourNutritionProduct tourNutritionProduct = (TourNutritionProduct) cell.getElement();

            final int caloriesValue = tourNutritionProduct.getQuantityType() == QuantityType.Products
                  ? tourNutritionProduct.getCalories()
                  : tourNutritionProduct.getCalories_Serving();

            final String text = caloriesValue == 0
                  ? UI.EMPTY_STRING
                  : String.valueOf(caloriesValue);
            cell.setText(text);
         }
      });
   }

   private void defineColumn_50_Sodium() {

      _colDef_Sodium = new TableColumnDefinition(_columnManager, COLUMN_SODIUM, SWT.TRAIL);

      _colDef_Sodium.setColumnLabel(Messages.Tour_Nutrition_Label_Sodium);
      _colDef_Sodium.setColumnHeaderText(Messages.Tour_Nutrition_Label_Sodium);

      _colDef_Sodium.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(12));

      _colDef_Sodium.setIsDefaultColumn();

      _colDef_Sodium.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourNutritionProduct tourNutritionProduct = (TourNutritionProduct) cell.getElement();

            final int sodiumValue = tourNutritionProduct.getQuantityType() == QuantityType.Products
                  ? tourNutritionProduct.getSodium()
                  : tourNutritionProduct.getSodium_Serving();

            final String text = sodiumValue == 0
                  ? UI.EMPTY_STRING
                  : String.valueOf(sodiumValue);
            cell.setText(text);
         }
      });
   }

   private void defineColumn_60_IsBeverage() {

      _colDef_IsBeverage = new TableColumnDefinition(_columnManager, COLUMN_ISBEVERAGE, SWT.CENTER);

      _colDef_IsBeverage.setColumnLabel(Messages.Tour_Nutrition_Column_IsBeverage);
      _colDef_IsBeverage.setColumnHeaderText(Messages.Tour_Nutrition_Column_IsBeverage);
      _colDef_IsBeverage.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(15));

      _colDef_IsBeverage.setIsDefaultColumn();

      _colDef_IsBeverage.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            // !!! When using cell.setImage() then it is not centered !!!
            // !!! Set dummy label provider, otherwise an error occurs !!!
         }
      });
   }

   private void defineColumn_70_BeverageQuantity() {

      final TableColumnDefinition colDef_BeverageQuantity = new TableColumnDefinition(_columnManager, COLUMN_BEVERAGE_QUANTITY, SWT.TRAIL);

      colDef_BeverageQuantity.setColumnLabel(Messages.Tour_Nutrition_Column_BeverageQuantity);
      colDef_BeverageQuantity.setColumnHeaderText(Messages.Tour_Nutrition_Column_BeverageQuantity);
      colDef_BeverageQuantity.setColumnHeaderToolTipText(Messages.Tour_Nutrition_Column_BeverageQuantity_Tooltip);

      colDef_BeverageQuantity.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(22));

      colDef_BeverageQuantity.setIsDefaultColumn();

      colDef_BeverageQuantity.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourNutritionProduct tourNutritionProduct = (TourNutritionProduct) cell.getElement();

            final int beverageQuantityValue = tourNutritionProduct.getQuantityType() == QuantityType.Products
                  ? tourNutritionProduct.getBeverageQuantity()
                  : tourNutritionProduct.getBeverageQuantity_Serving();
            final String cellText = tourNutritionProduct.isBeverage() && beverageQuantityValue != 0
                  ? String.valueOf(beverageQuantityValue)
                  : UI.EMPTY_STRING;

            cell.setText(cellText);
         }
      });
   }

   private void defineColumn_80_BeverageContainer() {

      _colDef_BeverageContainer = new TableColumnDefinition(_columnManager, COLUMN_BEVERAGE_CONTAINER, SWT.TRAIL);

      _colDef_BeverageContainer.setColumnLabel(Messages.Tour_Nutrition_Column_BeverageContainer);
      _colDef_BeverageContainer.setColumnHeaderText(Messages.Tour_Nutrition_Column_BeverageContainer);

      _colDef_BeverageContainer.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(25));

      _colDef_BeverageContainer.setIsDefaultColumn();

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

      _colDef_ConsumedBeverageContainers = new TableColumnDefinition(_columnManager, COLUMN_CONSUMED_CONTAINERS, SWT.TRAIL);

      _colDef_ConsumedBeverageContainers.setColumnLabel(Messages.Tour_Nutrition_Column_ConsumedContainers);
      _colDef_ConsumedBeverageContainers.setColumnHeaderText(Messages.Tour_Nutrition_Column_ConsumedContainers);
      _colDef_ConsumedBeverageContainers.setColumnHeaderToolTipText(Messages.Tour_Nutrition_Column_ConsumedContainers_Tooltip);

      _colDef_ConsumedBeverageContainers.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(27));

      _colDef_ConsumedBeverageContainers.setIsDefaultColumn();

      _colDef_ConsumedBeverageContainers.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourNutritionProduct tourNutritionProduct = (TourNutritionProduct) cell.getElement();

            final String text = tourNutritionProduct.getTourBeverageContainer() == null
                  ? UI.EMPTY_STRING
                  : String.valueOf(tourNutritionProduct.getContainersConsumed());

            cell.setText(text);
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
      UI.disposeResource(_imageCheck);
      UI.disposeResource(_imageUncheck);
      UI.disposeResource(_imageYes);

      super.dispose();
   }

   private void enableActions() {

      final List<String> selectedProductsCodes = getSelectedProductsCodes();

      _actionOpenProductsWebsite.setEnabled(selectedProductsCodes.size() > 0);

   }

   private void fillContextMenu(final IMenuManager menuMgr) {

      _actionOpenProductsWebsite.setTourNutritionProducts(getSelectedProductsCodes());
      menuMgr.add(_actionOpenProductsWebsite);

      menuMgr.add(_actionDeleteProducts);

      enableActions();
   }

   @Override
   public ColumnManager getColumnManager() {
      return _columnManager;
   }

   private List<TourNutritionProduct> getSelectedProducts() {

      final StructuredSelection selection = (StructuredSelection) _productsViewer.getSelection();

      final List<TourNutritionProduct> selectedTourNutritionProducts = new ArrayList<>();

      for (final Object object : selection.toList()) {

         if (object instanceof final TourNutritionProduct tourNutritionProduct) {
            selectedTourNutritionProducts.add(tourNutritionProduct);
         }
      }

      return selectedTourNutritionProducts;
   }

   private List<String> getSelectedProductsCodes() {

      final List<TourNutritionProduct> selectedTourNutritionProducts = getSelectedProducts();

      final List<String> selectedTourNutritionProductsCodes = new ArrayList<>();

      for (final TourNutritionProduct tourNutritionProduct : selectedTourNutritionProducts) {

         if (tourNutritionProduct.isCustomProduct()) {
            continue;
         }
         selectedTourNutritionProductsCodes.add(tourNutritionProduct.getProductCode());
      }

      return selectedTourNutritionProductsCodes;
   }

   @Override
   public ColumnViewer getViewer() {

      return _productsViewer;
   }

   private void handleTourEvent(final TourEventId tourEventId, final Object eventData) {

      if (tourEventId == TourEventId.TOUR_SELECTION && eventData instanceof final ISelection eventDataSelection) {

         onSelectionChanged(eventDataSelection);

      } else {

         if (_tourData == null) {
            return;
         }

         if (tourEventId == TourEventId.TOUR_CHANGED && eventData instanceof final TourEvent tourEventData) {

            final ArrayList<TourData> modifiedTours = tourEventData.getModifiedTours();
            if (modifiedTours != null) {

               // update modified tour

               updateModifiedTour(modifiedTours);
            }
         } else if (tourEventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

            clearView();
         }
      }
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      createMenuManager();

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

   private void onOpenProductsWebsite() {

      final Object selectedItem = ((IStructuredSelection) _productsViewer.getSelection()).getFirstElement();
      if (selectedItem == null) {
         return;
      }

      final StructuredSelection selection = (StructuredSelection) _productsViewer.getSelection();

      for (final Object object : selection.toList()) {

         if (object instanceof final TourNutritionProduct tourNutritionProduct) {
            final String productCode = tourNutritionProduct.getProductCode();
            NutritionUtils.openProductWebPage(productCode);
         }
      }
   }

   private void onPaint_Viewer_IsBeverageImage(final Event event) {

      final TourNutritionProduct tourNutritionProduct = (TourNutritionProduct) event.item.getData();

      final Image image;
      if (tourNutritionProduct.isBeverage()) {

         image = UI.IS_DARK_THEME ? _imageYes : _imageCheck;

      } else {

         image = UI.IS_DARK_THEME ? null : _imageUncheck;

      }

      if (image != null) {

         final int alignment = _colDef_IsBeverage.getColumnStyle();

         UI.paintImage(event, image, _columnWidth_ForColumn_IsBeverage, alignment);
      }
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

      } else if (selection instanceof SelectionTourIds) {

         showInvalidPage();
         return;

      } else if (selection instanceof SelectionDeletedTours) {

         clearView();
         return;
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

      _columnManager.saveState(_state);
   }

   private void setColumnsEditingSupport() {

      _colDef_ConsumedQuantity.setEditingSupport(new ConsumedQuantityEditingSupport());

      final String[] quantityTypeItems = new String[] { Messages.Tour_Nutrition_Label_QuantityType_Servings,
            Messages.Tour_Nutrition_Label_QuantityType_Products };

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

      _colDef_Name.setEditingSupport(new NoEditingSupport());
      _colDef_Calories.setEditingSupport(new NoEditingSupport());
      _colDef_Sodium.setEditingSupport(new NoEditingSupport());

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
         protected void setValue(final Object element, final Object value) {
            // Nothing to do
         }
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

            final TourNutritionProduct tourNutritionProduct = (TourNutritionProduct) element;

            final TourBeverageContainer tourBeverageContainer = tourNutritionProduct.getTourBeverageContainer();

            return tourBeverageContainer == null
                  ? 0
                  : _tourBeverageContainers.indexOf(tourBeverageContainer) + 1;
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

      _colDef_ConsumedBeverageContainers.setEditingSupport(new ConsumedBeverageContainersEditingSupport());
   }

   @Override
   public void setFocus() {
      _productsViewer.getTable().setFocus();
   }

   private void setWidth_ForColumn_IsBeverage() {

      if (_colDef_IsBeverage == null) {
         return;
      }

      final TableColumn tableColumn = _colDef_IsBeverage.getTableColumn();

      if (tableColumn != null && tableColumn.isDisposed() == false) {
         _columnWidth_ForColumn_IsBeverage = tableColumn.getWidth();
      }
   }

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
   public void updateColumnHeader(final ColumnDefinition colDef) {
      // Nothing to do
   }

   private void updateModifiedTour(final ArrayList<TourData> modifiedTours) {

      final long viewTourId = _tourData.getTourId();

      if (_tourData.isMultipleTours()) {

         showInvalidPage();

      } else {

         // The view contains a single tour
         for (final TourData tourData : modifiedTours) {

            if (tourData.getTourId() == viewTourId) {

               refreshTourData(tourData);

               // nothing more to do, the view contains only one tour
               break;
            }
         }
      }
   }

   private void updateUI_ProductViewer() {

      if (_tourData == null) {

         _pageBook.showPage(_pageNoData);

      } else {

         _isInUpdate = true;
         {
            _productsViewer.setInput(new Object[0]);
         }
         _isInUpdate = false;

         _pageBook.showPage(_pageContent);

         updateUI_SummaryFromModel();
      }
   }

   private void updateUI_SummaryFromModel() {

      final Set<TourNutritionProduct> tourNutritionProducts = _tourData.getTourNutritionProducts();

      final int totalCalories = NutritionUtils.getTotalCalories(tourNutritionProducts);
      final String totalCaloriesFormatted = FormatManager.formatNumber_0(totalCalories);
      _lblCalories_Total.setText(totalCaloriesFormatted);

      final float totalFluid = NutritionUtils.getTotalFluids(tourNutritionProducts) * 100 / 100;
      final String totalFluidFormatted = _nf2.format(totalFluid);
      _lblFluid_Total.setText(totalFluidFormatted);

      final int totalSodium = (int) NutritionUtils.getTotalSodium(tourNutritionProducts);
      final String totalSodiumFormatted = FormatManager.formatNumber_0(totalSodium);
      _lblSodium_Total.setText(totalSodiumFormatted);

      final String averageCaloriesPerHour = NutritionUtils.computeAverageCaloriesPerHour(_tourData);
      _lblCalories_Average.setText(averageCaloriesPerHour);

      final String averageFluidsPerHour = NutritionUtils.computeAverageFluidsPerHour(_tourData);
      _lblFluid_Average.setText(averageFluidsPerHour);

      final String averageSodiumPerLiter = NutritionUtils.computeAverageSodiumPerLiter(_tourData);
      _lblSodium_Average.setText(averageSodiumPerLiter);
   }

}
