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
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import net.sf.swtaddons.autocomplete.combo.AutocompleteComboInput;
import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.PostSelectionProvider;
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
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.ToolTip;

public class DialogSearchProduct extends TitleAreaDialog implements PropertyChangeListener {

   private static final String           STATE_AUTOCOMPLETE_POPUP_HEIGHT_SEARCH_HISTORY = "STATE_AUTOCOMPLETE_POPUP_HEIGHT_SEARCH_HISTORY";      //$NON-NLS-1$

   public static final String            ID                                             = "net.tourbook.ui.views.nutrition.DialogSearchProduct"; //$NON-NLS-1$

   private static final IPreferenceStore _prefStore                                     = TourbookPlugin.getPrefStore();
   private static final IDialogSettings  _state                                         = TourbookPlugin.getState(ID);
   private static final String           STATE_SEARCHED_QUERIES                         = "searched.queries";                                    //$NON-NLS-1$
   private static final String           STATE_SEARCH_TYPE                              = "STATE_SEARCH_TYPE";                                   //$NON-NLS-1$

   private static final String           HTTPS_OPENFOODFACTS_PRODUCTS                   = "https://world.openfoodfacts.org/cgi/product.pl";      //$NON-NLS-1$

   private TableViewer                   _productsViewer;
   private List<Product>                 _products;

   private long                          _tourId;

   private boolean                       _isInUIInit;

   /*
    * none UI
    */
   private PixelConverter _pc;

   /*
    * UI controls
    */
   private Button                        _btnAdd;
   private Button                        _btnSearch;
   private List<String>                  _searchHistory  = new ArrayList<>();

   private Label                         _lblSearchType;

   private Combo                         _comboSearchQuery;
   private Combo                         _comboSearchType;
   private ComboViewer                   _queryViewer;
   private final NutritionQuery          _nutritionQuery = new NutritionQuery();

   private PostSelectionProvider         _postSelectionProvider;

   private final Image                   _iconPlaceholder;
   private final HashMap<Integer, Image> _graphImages    = new HashMap<>();

   private IPropertyChangeListener       _prefChangeListener;

   //todo fb can we reduce the size of it ? lots of blank space !?ask wolfgang when i do the pr
   private AutocompleteComboInput _autocompleteProductSearchHistory;

   private ToolTip                _tooltipInvalidBarCode;

   private class SearchContentProvider implements IStructuredContentProvider {

      @Override
      public Object[] getElements(final Object inputElement) {
         return _searchHistory.toArray(new String[_searchHistory.size()]);
      }
   }

   private class ViewContentProvider implements IStructuredContentProvider {

      @Override
      public Object[] getElements(final Object parent) {

         return _products == null ? new String[] {} : _products.toArray();
      }
   }

   private class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {

      @Override
      public Image getColumnImage(final Object obj, final int index) {
         return null;
      }

      @Override
      public String getColumnText(final Object obj, final int index) {

         final Product product = (Product) obj;

         switch (index) {
         case 0:
            return product.code;

         case 1:
            return NutritionUtils.getProductFullName(product.brands, product.productName);

         default:
            return getText(obj);
         }
      }
   }

   public DialogSearchProduct(final Shell parentShell, final long tourId) {

      super(parentShell);

      // make dialog resizable and display maximize button
      setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);

      // set icon for the window
      setDefaultImage(TourbookPlugin.getImageDescriptor(Images.TourNutrition).createImage());

      _tourId = tourId;
      _iconPlaceholder = TourbookPlugin.getImageDescriptor(Images.App_EmptyIcon_Placeholder).createImage();

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

      _isInUIInit = true;
      {
         restoreState();
      }
      _isInUIInit = false;

      validateFields();
   }

   @Override
   protected final void createButtonsForButtonBar(final Composite parent) {

      super.createButtonsForButtonBar(parent);
      getButton(IDialogConstants.OK_ID).setVisible(false);
      getButton(IDialogConstants.CANCEL_ID).setVisible(false);
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      _pc = new PixelConverter(parent);

      final Composite dlgContainer = (Composite) super.createDialogArea(parent);

      _isInUIInit = true;
      {
         createUI(dlgContainer);
      }
      _isInUIInit = false;

      fillUI();

      // this part is a selection provider
      _postSelectionProvider = new PostSelectionProvider(ID);

      restoreState_WithUI();

      return dlgContainer;
   }

   private void createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().applyTo(container);
      {
         createUI_10_Header(container);
         createUI_20_Header_Options(container);
         createUI_30_Viewer(container);

         /*
          * Link/Info: How to add a product in the database
          */
         final Link link = new Link(container, SWT.NONE);
         link.setText(NLS.bind(Messages.Dialog_SearchProduct_Link_ProductCreationRequest,
               HTTPS_OPENFOODFACTS_PRODUCTS));
         link.setToolTipText(HTTPS_OPENFOODFACTS_PRODUCTS);
         link.addSelectionListener(widgetSelectedAdapter(selectionEvent -> WEB.openUrl(HTTPS_OPENFOODFACTS_PRODUCTS)));
      }
   }

   private void createUI_10_Header(final Composite parent) {

      final Composite queryContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(queryContainer);
      GridLayoutFactory.fillDefaults()
            .extendedMargins(5, 5, 2, 2)
            .spacing(5, 0)
            .numColumns(3)
            .applyTo(queryContainer);
      {
         {
            /*
             * combo: Text search
             */
            _comboSearchQuery = new Combo(queryContainer, SWT.NONE);
            _comboSearchQuery.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSearchProduct()));
            _comboSearchQuery.addModifyListener(event -> {
               validateFields();
               _btnSearch.getShell().setDefaultButton(_btnSearch);
            });
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .grab(true, false)
                  .applyTo(_comboSearchQuery);

            _tooltipInvalidBarCode = UI.createBalloonTooltip(queryContainer.getShell(), Messages.Dialog_SearchProduct_Tooltip_InvalidBarcode);
         }
         {
            /*
             * button: Search
             */
            _btnSearch = new Button(queryContainer, SWT.PUSH);
            _btnSearch.setText(Messages.Dialog_SearchProduct_Button_Search);
            _btnSearch.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSearchProduct()));
         }
         {
            /*
             * Button: Add
             */
            _btnAdd = new Button(queryContainer, SWT.PUSH);
            _btnAdd.setText(Messages.Dialog_SearchProduct_Button_Add);
            _btnAdd.setToolTipText(Messages.Dialog_SearchProduct_Button_Add_Tooltip);
            _btnAdd.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onAddProduct()));
         }
      }

      _queryViewer = new ComboViewer(_comboSearchQuery);
      _queryViewer.setContentProvider(new SearchContentProvider());
      _queryViewer.setComparator(new ViewerComparator());
   }

   private void createUI_20_Header_Options(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         {
            /*
             * Label: Search Type
             */
            _lblSearchType = UI.createLabel(container, Messages.Dialog_SearchProduct_Label_SearchType);
         }
         {
            /*
             * combo: Search type
             */
            _comboSearchType = new Combo(container, SWT.READ_ONLY);
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

   private void createUI_30_Viewer(final Composite parent) {

      /*
       * table viewer: products
       */
      final Table productsTable = new Table(parent, SWT.SINGLE | SWT.FULL_SELECTION);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(productsTable);
      productsTable.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));
      productsTable.setHeaderVisible(true);

      final GridData gdDescription = (GridData) productsTable.getLayoutData();
      gdDescription.heightHint = _pc.convertHeightInCharsToPixels(15);

      // column: Barcode
      final TableColumn columnCategory = new TableColumn(productsTable, SWT.LEFT);
      columnCategory.setText(Messages.Dialog_SearchProduct_TableHeader_Code);
      columnCategory.setWidth(_pc.convertWidthInCharsToPixels(20));

      // column: Name
      final TableColumn columnName = new TableColumn(productsTable, SWT.LEFT);
      columnName.setText(Messages.Dialog_SearchProduct_TableHeader_Name);
      columnName.setWidth(_pc.convertWidthInCharsToPixels(75));

      _productsViewer = new TableViewer(productsTable);

      _productsViewer.setContentProvider(new ViewContentProvider());
      _productsViewer.setLabelProvider(new ViewLabelProvider());

      _productsViewer.addPostSelectionChangedListener(selectionChangedEvent -> {

         final ISelection selection = selectionChangedEvent.getSelection();
         final Object firstElement = ((IStructuredSelection) selection).getFirstElement();
         final Product selectedProduct = (Product) firstElement;

         _postSelectionProvider.setSelection(selectedProduct);
         validateFields();
      });
   }

   private void fillUI() {

      /*
       * Fill search type combo
       */
      _comboSearchType.add(Messages.Dialog_SearchProduct_Combo_SearchType_ByName);
      _comboSearchType.add(Messages.Dialog_SearchProduct_Combo_SearchType_ByCode);
      _comboSearchType.select(0);

      _autocompleteProductSearchHistory = new AutocompleteComboInput(_comboSearchQuery);
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

      _iconPlaceholder.dispose();

      _graphImages.values().forEach(image -> image.dispose());

      _prefStore.removePropertyChangeListener(_prefChangeListener);

      _nutritionQuery.removePropertyChangeListener(this);
   }

   private void onSearchProduct() {

      _productsViewer.getTable().removeAll();
      // disable search controls
      _comboSearchQuery.setEnabled(false);
      _btnSearch.setEnabled(false);
      _btnAdd.setEnabled(false);
      _comboSearchType.setEnabled(false);
      _lblSearchType.setEnabled(false);

      String searchText = _comboSearchQuery.getText();

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

   @Override
   public void propertyChange(final PropertyChangeEvent propertyChangeEvent) {

      @SuppressWarnings("unchecked")
      final List<Product> searchResults = (List<Product>) propertyChangeEvent.getNewValue();

      Display.getDefault().asyncExec(() -> {

         if (searchResults == null) {

            setErrorMessage(String.format(Messages.Dialog_SearchProduct_Label_NotFound, _comboSearchQuery.getText()));
         } else {

            setErrorMessage(null);
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

         _comboSearchQuery.setEnabled(true);
         _btnSearch.setEnabled(true);
         _comboSearchType.setEnabled(true);
         _lblSearchType.setEnabled(true);
      });

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

   private void validateFields() {

      if (_isInUIInit) {
         return;
      }

      final ISelection selection = _productsViewer.getSelection();
      _btnAdd.setEnabled(!selection.isEmpty());
      final String searchQueryText = _comboSearchQuery.getText();

      if (net.tourbook.common.util.StringUtils.isNullOrEmpty(searchQueryText)) {
         _btnSearch.setEnabled(false);
         _tooltipInvalidBarCode.setVisible(false);
         return;
      }

      if (_comboSearchType.getSelectionIndex() == 1) {

         // Search by product code is selected
         final boolean isSearchQueryNumeric = StringUtils.isNumeric(searchQueryText);
         _btnSearch.setEnabled(isSearchQueryNumeric);
         _tooltipInvalidBarCode.setVisible(!isSearchQueryNumeric);

      } else {
         _btnSearch.setEnabled(true);
         _tooltipInvalidBarCode.setVisible(false);
      }

   }
}
