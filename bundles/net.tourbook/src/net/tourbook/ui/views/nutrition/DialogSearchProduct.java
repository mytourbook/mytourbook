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

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.autocomplete.AutoComplete_ComboInputMT;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnDefinitionFor1stVisibleAlignmentColumn;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.IContextMenuProvider;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.TableColumnDefinition;
import net.tourbook.data.TourData;
import net.tourbook.data.TourNutritionProduct;
import net.tourbook.nutrition.NutritionQuery;
import net.tourbook.nutrition.NutritionUtils;
import net.tourbook.nutrition.ProductSearchType;
import net.tourbook.nutrition.openfoodfacts.Product;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourManager;
import net.tourbook.web.WEB;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Widget;

public class DialogSearchProduct extends TitleAreaDialog implements ITourViewer, PropertyChangeListener {

   private static final String           STATE_AUTOCOMPLETE_POPUP_HEIGHT_SEARCH_HISTORY = "STATE_AUTOCOMPLETE_POPUP_HEIGHT_SEARCH_HISTORY";      //$NON-NLS-1$

   private static final String           ID                                             = "net.tourbook.ui.views.nutrition.DialogSearchProduct"; //$NON-NLS-1$

   private static final IPreferenceStore _prefStore                                     = TourbookPlugin.getPrefStore();
   private static final IDialogSettings  _state                                         = TourbookPlugin.getState(ID);
   private static final String           STATE_SEARCHED_QUERIES                         = "searched.queries";                                    //$NON-NLS-1$
   private static final String           STATE_SEARCH_TYPE                              = "STATE_SEARCH_TYPE";                                   //$NON-NLS-1$
   private static final String           COLUMN_CODE                                    = "Code";                                                //$NON-NLS-1$
   private static final String           COLUMN_NAME                                    = "Name";                                                //$NON-NLS-1$
   private static final String           COLUMN_QUANTITY                                = "Quantity";                                            //$NON-NLS-1$

   private static final String           HTTPS_OPENFOODFACTS_PRODUCTS                   = "https://world.openfoodfacts.org/cgi/product.pl";      //$NON-NLS-1$

   private TableViewer                   _productsViewer;
   private List<Product>                 _products;
   private ProductComparator             _productComparator                             = new ProductComparator();

   private long                          _tourId;

   private boolean                       _isInUIInit;

   /*
    * none UI
    */
   private PixelConverter            _pc;
   private List<String>              _searchHistory                  = new ArrayList<>();
   private final NutritionQuery      _nutritionQuery                 = new NutritionQuery();
   private PostSelectionProvider     _postSelectionProvider;

   private IPropertyChangeListener   _prefChangeListener;
   private SelectionListener         _columnSortListener;

   private AutoComplete_ComboInputMT _autocompleteProductSearchHistory;

   private ControlDecoration         _decorator_InvalidBarCode;

   private ComboViewer               _queryViewer;

   private ColumnManager             _columnManager;
   private MenuManager               _viewerMenuManager;
   private IContextMenuProvider      _tableViewerContextMenuProvider = new TableContextMenuProvider();

   private ActionAddProduct          _actionAddProduct;
   private ActionOpenProductsWebsite _actionOpenProductWebsite;

   /*
    * UI controls
    */
   private Button    _btnAdd;
   private Button    _btnSearch;

   private Label     _lblKeywords;
   private Label     _lblSearchType;

   private Combo     _comboSearchQuery;
   private Combo     _comboSearchType;

   private Composite _viewerContainer;

   private Menu      _tableContextMenu;

   private Image     _imageDialog = TourbookPlugin.getImageDescriptor(Images.TourNutrition).createImage();

   private class ActionAddProduct extends Action {

      public ActionAddProduct() {

         super(Messages.Dialog_SearchProduct_Button_Add, AS_PUSH_BUTTON);
         setToolTipText(Messages.Dialog_SearchProduct_Button_Add_Tooltip);
      }

      @Override
      public void run() {
         onAddProduct();
      }
   }

   private class ProductComparator extends ViewerComparator {

      private static final int ASCENDING        = 0;
      private static final int DESCENDING       = 1;

      private String           __sortColumnName = COLUMN_NAME;
      private int              __sortDirection  = ASCENDING;

      @Override
      public int compare(final Viewer viewer, final Object e1, final Object e2) {

         final boolean isDescending = __sortDirection == DESCENDING;

         final Product p1 = (Product) e1;
         final Product p2 = (Product) e2;

         long rc = 0;

         // Determine which column and do the appropriate sort
         switch (__sortColumnName) {

         case COLUMN_CODE:
            rc = StringUtils.compareIgnoreCase(p1.code, p2.code);
            break;

         case COLUMN_QUANTITY:
            final String quantity1 = net.tourbook.common.util.StringUtils.isNullOrEmpty(p1.quantity)
                  ? UI.EMPTY_STRING : p1.quantity;
            final String quantity2 = net.tourbook.common.util.StringUtils.isNullOrEmpty(p2.quantity)
                  ? UI.EMPTY_STRING : p2.quantity;
            rc = StringUtils.compareIgnoreCase(quantity1, quantity2);
            break;

         case COLUMN_NAME:
         default:
            rc = StringUtils.compareIgnoreCase(p1.productName, p2.productName);
            break;

         }

         if (rc == 0) {

            // subsort 1 by name
            rc = StringUtils.compareIgnoreCase(p1.productName, p2.productName);
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
       * @param sortColumnName
       *
       * @return Returns the column widget by its column name, when column name
       *         is not found then the first column is returned.
       */
      private TableColumn getSortColumn(final String sortColumnName) {

         final TableColumn[] allColumns = _productsViewer.getTable().getColumns();

         for (final TableColumn column : allColumns) {

            final String columnName = column.getText();

            if (columnName != null && columnName.equals(sortColumnName)) {
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

         final TableColumn tableColumn = (TableColumn) widget;

         if (tableColumn.getText().equals(__sortColumnName)) {

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

            __sortColumnName = tableColumn.getText();
            __sortDirection = ASCENDING;
         }

         updateUI_SetSortDirection(__sortColumnName, __sortDirection);
      }

      /**
       * Set the sort column direction indicator for a column
       *
       * @param sortColumnName
       * @param isAscendingSort
       */
      private void updateUI_SetSortDirection(final String sortColumnName, final int sortDirection) {

         final int direction =
               sortDirection == ProductComparator.ASCENDING ? SWT.UP
                     : sortDirection == ProductComparator.DESCENDING ? SWT.DOWN
                           : SWT.NONE;

         final Table table = _productsViewer.getTable();
         final TableColumn tc = getSortColumn(sortColumnName);

         table.setSortColumn(tc);
         table.setSortDirection(direction);
      }
   }

   private class SearchContentProvider implements IStructuredContentProvider {

      @Override
      public Object[] getElements(final Object inputElement) {
         return _searchHistory.toArray(new String[_searchHistory.size()]);
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

   private class ViewContentProvider implements IStructuredContentProvider {

      @Override
      public Object[] getElements(final Object parent) {

         return _products == null ? new String[] {} : _products.toArray();
      }
   }

   public DialogSearchProduct(final Shell parentShell, final long tourId) {

      super(parentShell);

      // make dialog resizable and display maximize button
      setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);

      // set icon for the window
      setDefaultImage(_imageDialog);

      _tourId = tourId;
   }

   private void addPrefListener() {

      _prefChangeListener = propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

            _productsViewer.getTable().setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));
            _productsViewer.refresh();
         }
      };

      _prefStore.addPropertyChangeListener(_prefChangeListener);

      _nutritionQuery.addPropertyChangeListener(this);
   }

   @Override
   public boolean close() {

      saveState();

      return super.close();
   }

   @Override
   protected void configureShell(final Shell shell) {

      super.configureShell(shell);

      shell.setText(Messages.Dialog_SearchProduct_Title);

      shell.addDisposeListener(disposeEvent -> onDispose());
   }

   @Override
   public void create() {

      addPrefListener();

      // create UI widgets
      super.create();

      setTitle(Messages.Dialog_SearchProduct_Title);

      _isInUIInit = true;
      {
         restoreState();
      }
      _isInUIInit = false;

      validateFields();
   }

   private void createActions() {

      _actionAddProduct = new ActionAddProduct();
      _actionOpenProductWebsite = new ActionOpenProductsWebsite();
   }

   @Override
   protected final void createButtonsForButtonBar(final Composite parent) {

      super.createButtonsForButtonBar(parent);
      getButton(IDialogConstants.OK_ID).setVisible(false);
      getButton(IDialogConstants.CANCEL_ID).setVisible(false);

      ((GridLayout) parent.getLayout()).marginHeight = 0;
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      initUI(parent);

      // define all columns for the viewer
      _columnManager = new ColumnManager(this, _state);
      defineAllColumns();

      _isInUIInit = true;
      {
         createActions();
         createUI(parent);
      }
      _isInUIInit = false;

      fillUI();

      // this part is a selection provider
      _postSelectionProvider = new PostSelectionProvider(ID);

      restoreState_WithUI();

      return _viewerContainer;
   }

   private void createMenuManager() {

      _viewerMenuManager = new MenuManager("#PopupMenu"); //$NON-NLS-1$
      _viewerMenuManager.setRemoveAllWhenShown(true);
      _viewerMenuManager.addMenuListener(manager -> fillContextMenu(manager));
   }

   private void createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().margins(10, 5).applyTo(container);
      {
         createUI_10_Header(container);

         _viewerContainer = new Composite(container, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, true).applyTo(_viewerContainer);
         GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
         {
            createUI_20_Viewer(_viewerContainer);
         }

         /*
          * Link/Info: How to add a product in the database
          */
         final Link link = new Link(container, SWT.NONE);
         link.setText(NLS.bind(Messages.Dialog_SearchProduct_Link_ProductCreationRequest,
               HTTPS_OPENFOODFACTS_PRODUCTS));
         link.setToolTipText(HTTPS_OPENFOODFACTS_PRODUCTS);
         link.addSelectionListener(widgetSelectedAdapter(selectionEvent -> WEB.openUrl(HTTPS_OPENFOODFACTS_PRODUCTS)));
         GridDataFactory.fillDefaults()
               .align(SWT.LEFT, SWT.CENTER)
               .grab(true, false)
               .applyTo(link);
      }
   }

   private void createUI_10_Header(final Composite parent) {

      final Composite headerContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(headerContainer);
      GridLayoutFactory.fillDefaults().numColumns(4).applyTo(headerContainer);
      {
         {
            /*
             * Label: Keywords
             */
            _lblKeywords = UI.createLabel(headerContainer, Messages.Dialog_SearchProduct_Label_Keywords);
         }
         {
            /*
             * combo: Text search
             */
            _comboSearchQuery = new Combo(headerContainer, SWT.NONE);
            _comboSearchQuery.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSearchProduct()));
            _comboSearchQuery.addModifyListener(event -> {

               validateFields();

               _btnSearch.getShell().setDefaultButton(_btnSearch);
            });
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .grab(true, false)
                  .applyTo(_comboSearchQuery);

            _decorator_InvalidBarCode = new ControlDecoration(_comboSearchQuery, SWT.TOP | SWT.LEFT);
            _decorator_InvalidBarCode.setDescriptionText(Messages.Dialog_SearchProduct_Tooltip_InvalidBarcode);
            final Image image = FieldDecorationRegistry
                  .getDefault()
                  .getFieldDecoration(FieldDecorationRegistry.DEC_ERROR)
                  .getImage();
            _decorator_InvalidBarCode.setImage(image);
         }
         {
            /*
             * button: Search
             */
            _btnSearch = new Button(headerContainer, SWT.PUSH);
            _btnSearch.setText(Messages.Dialog_SearchProduct_Button_Search);
            _btnSearch.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSearchProduct()));
            setButtonLayoutData(_btnSearch);
         }
         {
            /*
             * Button: Add
             */
            _btnAdd = new Button(headerContainer, SWT.PUSH);
            _btnAdd.setText(Messages.Dialog_SearchProduct_Button_Add);
            _btnAdd.setToolTipText(Messages.Dialog_SearchProduct_Button_Add_Tooltip);
            _btnAdd.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onAddProduct()));
            setButtonLayoutData(_btnAdd);
         }
         {
            /*
             * Label: Search Type
             */
            _lblSearchType = UI.createLabel(headerContainer, Messages.Dialog_SearchProduct_Label_SearchType);
            GridDataFactory.fillDefaults()
                  .align(SWT.LEFT, SWT.CENTER)
                  .applyTo(_lblSearchType);
         }
         {
            /*
             * combo: Search type
             */
            _comboSearchType = new Combo(headerContainer, SWT.READ_ONLY);
            _comboSearchType.setVisibleItemCount(2);
            _comboSearchType.addSelectionListener(widgetSelectedAdapter(selectionEvent -> validateFields()));
            GridDataFactory.fillDefaults()
                  .align(SWT.LEFT, SWT.CENTER)
                  .grab(true, false)
                  .applyTo(_comboSearchType);
         }
      }

      _queryViewer = new ComboViewer(_comboSearchQuery);
      _queryViewer.setContentProvider(new SearchContentProvider());
      _queryViewer.setComparator(new ViewerComparator());
   }

   private void createUI_20_Viewer(final Composite parent) {

      /*
       * table viewer: products
       */
      final Table productsTable = new Table(parent, SWT.SINGLE | SWT.FULL_SELECTION);
      GridLayoutFactory.fillDefaults().margins(2, 2).applyTo(productsTable);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(productsTable);
      productsTable.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));
      productsTable.setHeaderVisible(true);

      final MenuManager menuManager = new MenuManager();
      menuManager.add(_actionAddProduct);
      final Menu contextMenu = menuManager.createContextMenu(productsTable);
      productsTable.setMenu(contextMenu);

      final GridData gdDescription = (GridData) productsTable.getLayoutData();
      gdDescription.heightHint = _pc.convertHeightInCharsToPixels(15);

      _productsViewer = new TableViewer(productsTable);

      _columnManager.createColumns(_productsViewer);

      _productsViewer.setContentProvider(new ViewContentProvider());
      _productsViewer.setComparator(_productComparator);

      final Listener doubleClickListener = event -> {

         if (event.type == SWT.MouseDoubleClick) {

            final Object selectedItem = ((IStructuredSelection) _productsViewer.getSelection()).getFirstElement();
            if (selectedItem == null) {
               return;
            }

            final Product product = (Product) selectedItem;

            NutritionUtils.openProductWebPage(product.code);
         }
      };
      _productsViewer.getTable().addListener(SWT.MouseDoubleClick, doubleClickListener);

      _productsViewer.addPostSelectionChangedListener(selectionChangedEvent -> {

         final ISelection selection = selectionChangedEvent.getSelection();
         final Object firstElement = ((IStructuredSelection) selection).getFirstElement();
         final Product selectedProduct = (Product) firstElement;

         _postSelectionProvider.setSelection(selectedProduct);
         validateFields();
      });

      createUI_30_ContextMenu();
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
      return _viewerMenuManager.createContextMenu(table);
   }

   private void defineAllColumns() {

      defineColumn_10_Barcode();
      defineColumn_20_Name();
      defineColumn_30_Quantity();

      new ColumnDefinitionFor1stVisibleAlignmentColumn(_columnManager);
   }

   private void defineColumn_10_Barcode() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_CODE, SWT.TRAIL);

      colDef.setColumnLabel(Messages.Tour_Nutrition_Column_Code);
      colDef.setColumnHeaderText(Messages.Tour_Nutrition_Column_Code);

      colDef.setIsDefaultColumn();
      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(20));

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Product product = (Product) cell.getElement();

            cell.setText(product.code);
         }
      });
   }

   private void defineColumn_20_Name() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_NAME, SWT.TRAIL);

      colDef.setColumnLabel(Messages.Tour_Nutrition_Column_Name);
      colDef.setColumnHeaderText(Messages.Tour_Nutrition_Column_Name);

      colDef.setIsDefaultColumn();
      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(55));

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Product product = (Product) cell.getElement();

            cell.setText(product.productName);
         }
      });
   }

   private void defineColumn_30_Quantity() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_QUANTITY, SWT.TRAIL);

      colDef.setColumnLabel(Messages.Tour_Nutrition_Column_Quantity);
      colDef.setColumnHeaderText(Messages.Tour_Nutrition_Column_Quantity);

      colDef.setIsDefaultColumn();
      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(15));

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Product product = (Product) cell.getElement();

            cell.setText(product.quantity);
         }
      });
   }

   private void enableControls(final boolean isFocusSearchQuery) {

      _lblKeywords.setEnabled(true);
      _comboSearchQuery.setEnabled(true);
      if (isFocusSearchQuery) {
         _comboSearchQuery.setFocus();
      }
      _btnSearch.setEnabled(true);
      _comboSearchType.setEnabled(true);
      _lblSearchType.setEnabled(true);
   }

   private void fillContextMenu(final IMenuManager menuMgr) {

      menuMgr.add(_actionAddProduct);

      _actionOpenProductWebsite.setTourNutritionProducts(getSelectedProducts());
      menuMgr.add(_actionOpenProductWebsite);
   }

   private void fillUI() {

      /*
       * Fill search type combo
       */
      _comboSearchType.add(Messages.Dialog_SearchProduct_Combo_SearchType_ByName);
      _comboSearchType.add(Messages.Dialog_SearchProduct_Combo_SearchType_ByCode);
      _comboSearchType.select(0);

      _autocompleteProductSearchHistory = new AutoComplete_ComboInputMT(_comboSearchQuery);
   }

   @Override
   public ColumnManager getColumnManager() {
      return _columnManager;
   }

   @Override
   protected IDialogSettings getDialogBoundsSettings() {

      // keep window size and position
      return _state;
   }

   private ProductSearchType getProductSearchType() {

      switch (_comboSearchType.getSelectionIndex()) {
      case 1:
         return ProductSearchType.ByCode;

      case 0:
      default:
         return ProductSearchType.ByName;
      }
   }

   private List<String> getSelectedProducts() {

      final StructuredSelection selection = (StructuredSelection) _productsViewer.getSelection();

      final List<String> productCodes = new ArrayList<>();

      for (final Object object : selection.toList()) {

         if (object instanceof final Product product) {
            productCodes.add(product.code);
         }
      }

      return productCodes;
   }

   @Override
   public ColumnViewer getViewer() {
      return _productsViewer;
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      createMenuManager();

      _columnSortListener = SelectionListener.widgetSelectedAdapter(selectionEvent -> onSelect_SortColumn(selectionEvent));
   }

   private void onAddProduct() {

      BusyIndicator.showWhile(Display.getCurrent(), () -> {

         final ISelection selection = _productsViewer.getSelection();
         final Object firstElement = ((IStructuredSelection) selection).getFirstElement();
         final Product selectedProduct = (Product) firstElement;

         final TourData tourData = TourManager.getTour(_tourId);

         final Set<TourNutritionProduct> tourNutritionProducts = tourData.getTourNutritionProducts();

         // Before adding the selected product, we need to check if it doesn't already exist
         if (tourNutritionProducts.stream().anyMatch(tourNutritionProduct -> tourNutritionProduct.getProductCode().equals(selectedProduct.code))) {

            setErrorMessage(Messages.Dialog_SearchProduct_Label_AlreadyExists);
            return;
         }

         setErrorMessage(null);

         final TourNutritionProduct tourNutritionProduct = new TourNutritionProduct(tourData, selectedProduct);
         tourData.addNutritionProduct(tourNutritionProduct);

         TourManager.saveModifiedTour(tourData);
      });
   }

   private void onDispose() {

      UI.disposeResource(_imageDialog);

      _prefStore.removePropertyChangeListener(_prefChangeListener);

      _nutritionQuery.removePropertyChangeListener(this);
   }

   private void onSearchProduct() {

      if (_btnSearch.isEnabled() == false) {
         return;
      }

      _productsViewer.getTable().removeAll();
      // disable search controls
      _lblKeywords.setEnabled(false);
      _comboSearchQuery.setEnabled(false);
      _btnSearch.setEnabled(false);
      _btnAdd.setEnabled(false);
      _comboSearchType.setEnabled(false);
      _lblSearchType.setEnabled(false);

      String searchText = _comboSearchQuery.getText().trim();

      // remove same search text
      if (_searchHistory.contains(searchText) == false) {

         // update model
         _searchHistory.add(searchText);

         // update viewer
         _queryViewer.add(searchText);
      }

      // start product search
      final ProductSearchType productSearchType = getProductSearchType();
      if (productSearchType == ProductSearchType.ByCode &&
            searchText.length() < 12) {
         searchText = StringUtils.leftPad(searchText, 12, '0');
      }
      _nutritionQuery.asyncFind(searchText, productSearchType);
   }

   private void onSelect_SortColumn(final SelectionEvent selectionEvent) {

      _viewerContainer.setRedraw(false);
      {
         // keep selection
         final ISelection selectionBackup = _productsViewer.getSelection();

         // toggle sorting
         _productComparator.setSortColumn(selectionEvent.widget);
         _productsViewer.refresh();

         // reselect selection
         _productsViewer.setSelection(selectionBackup, true);
         _productsViewer.getTable().showSelection();
      }
      _viewerContainer.setRedraw(true);
   }

   @Override
   public void propertyChange(final PropertyChangeEvent propertyChangeEvent) {

      @SuppressWarnings("unchecked")
      final List<Product> searchResults = (List<Product>) propertyChangeEvent.getNewValue();

      Display.getDefault().asyncExec(() -> {

         String errorMessage = null;
         String message = null;
         if (searchResults == null || searchResults.isEmpty()) {

            errorMessage = String.format(Messages.Dialog_SearchProduct_Label_NotFound, _comboSearchQuery.getText());

         } else {

            message = String.format(Messages.Dialog_SearchProduct_Message, searchResults.size());

            _products = searchResults;

            // check if view is closed
            if (_btnSearch.isDisposed()) {
               return;
            }

            // refresh viewer
            _productsViewer.setInput(new Object());

            // select first entry, if there is one
            final Table productsTable = _productsViewer.getTable();
            if (productsTable.getItemCount() > 0) {

               final Object firstData = productsTable.getItem(0).getData();
               if (firstData instanceof Product) {

                  _productsViewer.setSelection(new StructuredSelection(firstData));
                  setViewerFocus();
               }
            }
         }

         setErrorMessage(errorMessage);
         setMessage(message);

         final boolean isFocusSearchQuery = net.tourbook.common.util.StringUtils.hasContent(errorMessage);
         enableControls(isFocusSearchQuery);
      });

   }

   @Override
   public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

      _viewerContainer.setRedraw(false);
      {
         _productsViewer.getTable().dispose();

         createUI_20_Viewer(_viewerContainer);
         _viewerContainer.layout();

         // update the viewer
         reloadViewer();
      }
      _viewerContainer.setRedraw(true);

      return _productsViewer;
   }

   @Override
   public void reloadViewer() {
      _productsViewer.setInput(new Object[0]);
   }

   private void restoreState() {

      // restore old used queries
      final String[] stateSearchedQueries = _state.getArray(STATE_SEARCHED_QUERIES);
      if (stateSearchedQueries != null) {
         Stream.of(stateSearchedQueries).forEach(query -> _searchHistory.add(query));
      }

      int stateSearchType;
      try {

         stateSearchType = _state.getInt(STATE_SEARCH_TYPE);

      } catch (final NumberFormatException e) {
         stateSearchType = 0;
      }
      _comboSearchType.select(stateSearchType);

      // update content in the comboviewer
      _queryViewer.setInput(new Object());
   }

   private void restoreState_WithUI() {

      _autocompleteProductSearchHistory.restoreState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_SEARCH_HISTORY);
   }

   private void saveState() {

      _state.put(STATE_SEARCHED_QUERIES, _searchHistory.toArray(new String[_searchHistory.size()]));
      _state.put(STATE_SEARCH_TYPE, _comboSearchType.getSelectionIndex());

      _columnManager.saveState(_state);

      _autocompleteProductSearchHistory.saveState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_SEARCH_HISTORY);
   }

   /**
    * set focus to selected item, selection and focus are not the same !!!
    */
   private void setViewerFocus() {

      final Table table = _productsViewer.getTable();

      table.setSelection(table.getSelectionIndex());
      table.setFocus();
   }

   @Override
   public void updateColumnHeader(final ColumnDefinition colDef) {}

   private void validateFields() {

      if (_isInUIInit) {
         return;
      }

      final ISelection selection = _productsViewer.getSelection();
      _btnAdd.setEnabled(!selection.isEmpty());
      final String searchQueryText = _comboSearchQuery.getText().trim();

      if (net.tourbook.common.util.StringUtils.isNullOrEmpty(searchQueryText)) {

         _btnSearch.setEnabled(false);
         _decorator_InvalidBarCode.hide();
         return;
      }

      if (_comboSearchType.getSelectionIndex() == 1) {

         // Search by product code is selected
         final boolean isSearchQueryNumeric = StringUtils.isNumeric(searchQueryText);
         _btnSearch.setEnabled(isSearchQueryNumeric);
         if (!isSearchQueryNumeric) {
            _decorator_InvalidBarCode.show();
            _decorator_InvalidBarCode.showHoverText(Messages.Dialog_SearchProduct_Tooltip_InvalidBarcode);
         } else {
            _decorator_InvalidBarCode.hide();
         }
      } else {

         _btnSearch.setEnabled(true);
         _decorator_InvalidBarCode.hide();
      }

   }
}
