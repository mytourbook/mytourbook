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

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourNutritionProduct;
import net.tourbook.database.TourDatabase;
import net.tourbook.nutrition.NutritionQuery;
import net.tourbook.nutrition.NutritionUtils;
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
import net.tourbook.ui.views.referenceTour.SelectionReferenceTourView;
import net.tourbook.ui.views.referenceTour.TVIElevationCompareResult_ComparedTour;
import net.tourbook.ui.views.referenceTour.TVIRefTour_ComparedTour;
import net.tourbook.ui.views.referenceTour.TVIRefTour_RefTourItem;

import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

import cop.swt.widgets.viewers.table.celleditors.RangeContent;
import cop.swt.widgets.viewers.table.celleditors.SpinnerCellEditor;

public class TourNutritionView extends ViewPart implements PropertyChangeListener {

   //add the possibility to add custom products (i.e: water...)
   public static final String  ID                              = "net.tourbook.ui.views.TourNutritionView"; //$NON-NLS-1$
   private static final String STATE_SEARCHED_NUTRITIONQUERIES = "searched.nutritionQueries";               //$NON-NLS-1$
   private static final String STATE_SECTION_PRODUCTS_LIST     = "STATE_SECTION_PRODUCTS_LIST";             //$NON-NLS-1$

//https://world.openfoodfacts.org/data

   // in the pref page, add a button to "Refresh product's information"
   // this will retrieve the updated (if any), info for each product)
   // display the previous total calories vs new total calories

   private static final IPreferenceStore _prefStore           = TourbookPlugin.getPrefStore();

   private static final int              _hintTextColumnWidth = UI.IS_OSX ? 100 : 50;
   private final IDialogSettings         _state               = TourbookPlugin.getState(ID);

   private TourData                      _tourData;
   private TableViewer                   _productsViewer;

   private ColumnManager                 _columnManager;
   private List<String>                  _searchHistory       = new ArrayList<>();

   private IPropertyChangeListener       _prefChangeListener;
   final NutritionQuery                  _nutritionQuery      = new NutritionQuery();

   private ISelectionListener            _postSelectionListener;
   private ITourEventListener            _tourEventListener;
   private PostSelectionProvider         _postSelectionProvider;
   private final RangeContent            _opacityRange        = new RangeContent(0.25, 10.0, 0.25, 100);

   private final NumberFormat            _nf2                 = NumberFormat.getNumberInstance();

   {
      _nf2.setMinimumFractionDigits(2);
      _nf2.setMaximumFractionDigits(2);
   }

   /*
    * UI controls
    */
   private Image       _imageAdd;
   private Image       _imageDelete;

   private Button      _btnAddProduct;

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
   private Combo       _cboSearchQuery;
   private Section     _sectionProductsList;
   private FormToolkit _tk;

   class TourNutritionProductCellModifier implements ICellModifier {

      private Viewer viewer;

      public TourNutritionProductCellModifier(final Viewer viewer) {
         this.viewer = viewer;
      }

      @Override
      public boolean canModify(final Object element, final String property) {
         return true;
      }

      @Override
      public Object getValue(final Object element, final String property) {

         final TourNutritionProduct task = (TourNutritionProduct) element;

         final String[] tableColumns = { "Servings", "Name", "Calories", "Sodium", "Beverage", "Beverage Container", "Consumed Containers" };
         if (property.equals("Servings")) {
            return task.getServingsConsumed();
         }
         if (property.equals("Consumed Containers")) {
            return 1;
         }
         if (property.equals("Sodium")) {
            return task.getSodium();
         }
         if (property.equals("Beverage")) {
            return Boolean.valueOf(task.isBeverage());
         }
         if (property.equals("Beverage Container")) {
            return 1;// task.getTourBeverageContainerName();
         }
         return UI.EMPTY_STRING;
      }

      @Override
      public void modify(Object element, final String property, final Object value) {

         if (element instanceof final Item elementItem) {
            element = (elementItem).getData();
         }
         final TourNutritionProduct tourNutritionProduct = (TourNutritionProduct) element;

         if (property.equals("Servings")) {
            tourNutritionProduct.setServingsConsumed(((Double) value).floatValue());
         }
         if (property.equals("Consumed Containers")) {
            tourNutritionProduct.getContainersConsumed();
         }
         if (property.equals("Beverage")) {

            final boolean booleanValue = ((Boolean) value).booleanValue();
            tourNutritionProduct.setIsBeverage(booleanValue);
         }

         if (property.equals("Beverage Container")) {
            final var toto = TourDatabase.getTourBeverageContainers();

            tourNutritionProduct.setTourBeverageContainer(toto.get(0));
         }

         _tourData = TourManager.saveModifiedTour(_tourData);

         _productsViewer.refresh();
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

   class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {

      @Override
      public Image getColumnImage(final Object obj, final int index) {

         final TourNutritionProduct tourNutritionProduct = (TourNutritionProduct) obj;

         switch (index) {
         case 4:

            final String imageDescriptorPath = tourNutritionProduct.isBeverage()
                  ? Images.Checkbox_Checked
                  : Images.Checkbox_Uncheck;

            return TourbookPlugin.getImageDescriptor(imageDescriptorPath).createImage();

         default:
            return null;
         }
      }

      @Override
      public String getColumnText(final Object obj, final int index) {

         final TourNutritionProduct tourNutritionProduct = (TourNutritionProduct) obj;

         switch (index) {
         case 0:
            return String.valueOf(tourNutritionProduct.getServingsConsumed());

         case 1:
            return tourNutritionProduct.getName();

         case 2:
            return String.valueOf(tourNutritionProduct.getCalories());

         case 3:
            return String.valueOf(tourNutritionProduct.getSodium());

         case 5:
            return tourNutritionProduct.getTourBeverageContainerName();

         case 4:
         default:
            return UI.EMPTY_STRING;
         }
      }

      @Override
      public Image getImage(final Object obj) {

         return null;
      }
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

         if (/* _isInUpdate || */ workbenchPart == TourNutritionView.this) {
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

                     final List<Long> tourIds = new ArrayList<>();

                     modifiedTours.forEach(tour -> tourIds.add(tour.getTourId()));
                     _tourData = TourManager.createJoinedTourData(tourIds);

                     updateUI_ProductViewer();

                     // removed old tour data from the selection provider
                     _postSelectionProvider.clearSelection();

                  } else {

                     // The view contains a single tour
                     for (final TourData tourData : modifiedTours) {
                        if (tourData.getTourId() == viewTourId) {

                           // get modified tour
                           _tourData = tourData;

                           updateUI_ProductViewer();

                           // removed old tour data from the selection provider
                           _postSelectionProvider.clearSelection();

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
      }

      return total * 60 / (tourDeviceTime_Recorded / 60);
   }

   private void createColumns(final Table productsTable, final String[] tableColumns) {

      int index = 0;

      // Column: Quantity
      final TableColumn columnServings = new TableColumn(productsTable, SWT.LEFT);
      columnServings.setText(tableColumns[index++]);
      columnServings.setWidth(75);

      // Column: name
      final TableColumn columnName = new TableColumn(productsTable, SWT.LEFT);
      columnName.setText(tableColumns[index++]);
      columnName.setWidth(150);

      // Column: Calories
      final TableColumn columnCalories = new TableColumn(productsTable, SWT.LEFT);
      columnCalories.setText(tableColumns[index++]);
      columnCalories.setWidth(75);

      // Column: Sodium
      final TableColumn columnSodium = new TableColumn(productsTable, SWT.LEFT);
      columnSodium.setText(tableColumns[index++]);
      columnSodium.setWidth(75);

      // Column: Beverage
      final TableColumn columnFluid = new TableColumn(productsTable, SWT.LEFT);
      columnFluid.setText(tableColumns[index++]);
      columnFluid.setWidth(75);

      // Column: Beverage Container
      final TableColumn columnFluidContainerName = new TableColumn(productsTable, SWT.LEFT);
      columnFluidContainerName.setText(tableColumns[index++]);
      columnFluidContainerName.setWidth(75);

      // Column: Consumed Containers
      final TableColumn columnFluidContainersConsumed = new TableColumn(productsTable, SWT.LEFT);
      columnFluidContainersConsumed.setText(tableColumns[index++]);
      columnFluidContainersConsumed.setWidth(75);
   }

   @Override
   public void createPartControl(final Composite parent) {

      createUI(parent);

      addSelectionListener();
      addTourEventListener();

      addPrefListener();

      // this part is a selection provider
      _postSelectionProvider = new PostSelectionProvider(ID);
      getSite().setSelectionProvider(_postSelectionProvider);

      // show default page
      _pageBook.showPage(_pageNoData);

      enableControls();
      restoreState();
   }

   private Section createSection(final Composite parent,
                                 final FormToolkit tk,
                                 final String title,
                                 final boolean isExpandable) {

      final int style = isExpandable
            ? Section.TWISTIE | Section.TITLE_BAR
            : Section.TITLE_BAR;

      final Section section = tk.createSection(parent, style);

      section.setText(title);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(section);

      final Composite sectionContainer = tk.createComposite(section);
      section.setClient(sectionContainer);

      return section;
   }

   private void createUI(final Composite parent) {

      _pageBook = new PageBook(parent, SWT.NONE);

      _pageNoData = net.tourbook.common.UI.createUI_PageNoData(_pageBook, Messages.UI_Label_no_chart_is_selected);

      _tk = new FormToolkit(parent.getDisplay());

      _imageAdd = TourbookPlugin.getImageDescriptor(Images.App_Add).createImage();
      _imageDelete = TourbookPlugin.getImageDescriptor(Images.App_Trash).createImage();

      _viewerContainer = new Composite(_pageBook, SWT.NONE);
      GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
      {
         createUI_Section_10_Summary(_viewerContainer);
         createUI_Section_20_ProductsList(_viewerContainer);
      }
   }

   private void createUI_210_Actions(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().span(2, 1).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         /*
          * Add product button
          */
         _btnAddProduct = new Button(container, SWT.NONE);
         _btnAddProduct.setText(Messages.Tour_Nutrition_Label_AddProduct);
         //_btnCleanup.setToolTipText(Messages.PrefPage_CloudConnectivity_Label_Cleanup_Tooltip);
         _btnAddProduct.addSelectionListener(widgetSelectedAdapter(selectionEvent -> {
            new DialogSearchProduct(Display.getCurrent().getActiveShell(), _tourData.getTourId()).open();
         }));
         _btnAddProduct.setImage(_imageAdd);
         GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).grab(true, false).applyTo(_btnAddProduct);

         /*
          * Delete product button
          */
         _btnDeleteProduct = new Button(container, SWT.NONE);
         _btnDeleteProduct.setText("&Delete");
         _btnDeleteProduct.setEnabled(false);
         //_btnCleanup.setToolTipText(Messages.PrefPage_CloudConnectivity_Label_Cleanup_Tooltip);
         _btnDeleteProduct.addSelectionListener(widgetSelectedAdapter(selectionEvent -> {
            //TODO FB
            // also only enable it if a product is selected in the table
         }));
         _btnDeleteProduct.setImage(_imageDelete);
         GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).grab(true, false).applyTo(_btnDeleteProduct);
      }
   }

   private void createUI_220_Viewer(final Composite parent) {

      /*
       * table viewer: products
       */
      final Table productsTable = new Table(parent, /* SWT.BORDER | */SWT.SINGLE | SWT.FULL_SELECTION);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(productsTable);
      productsTable.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));
      productsTable.setHeaderVisible(true);

      final String[] tableColumns = { "Servings", "Name", "Calories", "Sodium", "Beverage", "Beverage Container", "Consumed Containers" };
      createColumns(productsTable, tableColumns);

      _productsViewer = new TableViewer(productsTable);
      _productsViewer.setColumnProperties(tableColumns);

      // Create the cell editors
      final CellEditor[] editors = new CellEditor[tableColumns.length];
      // Servings
      editors[0] = new SpinnerCellEditor(productsTable, _nf2, _opacityRange, SWT.NONE);
      // Is Beverage
      editors[4] = new CheckboxCellEditor(productsTable);
      // Column: Consumed Containers
      editors[6] = new SpinnerCellEditor(productsTable, _nf2, _opacityRange, SWT.NONE);

      final var toto = TourDatabase.getTourBeverageContainers();
      final String[] items = new String[toto.size()];
      for (int index2 = 0; index2 < toto.size(); ++index2) {
         items[index2] = toto.get(index2).getName();
      }
      editors[5] = new ComboBoxCellEditor(productsTable, items, SWT.READ_ONLY);
      // example
      // Flask (0.5L)
      // bladder (1.5L)
      // not the append of the capacity

      // Assign the cell editors to the viewer
      _productsViewer.setCellEditors(editors);
      _productsViewer.setCellModifier(new TourNutritionProductCellModifier(_productsViewer));

      _productsViewer.setContentProvider(new ViewContentProvider());
      _productsViewer.setLabelProvider(new ViewLabelProvider());

      _productsViewer.addSelectionChangedListener(selectionChangedEvent -> onTableSelectionChanged(selectionChangedEvent));
   }

   private void createUI_Section_10_Summary(final Composite parent) {

      final Composite container = new Composite(parent, SWT.BORDER);
      GridLayoutFactory.fillDefaults().numColumns(4).applyTo(container);
      {
         /*
          * Title: Summary report card
          */
         {
            final Label label = UI.createLabel(container, Messages.Tour_Nutrition_Label_ReportCard);
            label.setToolTipText(Messages.Tour_Nutrition_Label_ReportCard_Tooltip);
            GridDataFactory.fillDefaults().span(4, 1).align(SWT.BEGINNING, SWT.FILL).applyTo(label);
         }

         /*
          * Columns headers
          */
         {
            Label label = UI.createLabel(container, UI.EMPTY_STRING);
            GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL).applyTo(label);

            label = UI.createLabel(container, "Calories");
            label.setToolTipText("Messages.Poi_View_Label_POI_Tooltip");
            GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL).applyTo(label);

            label = UI.createLabel(container, "Fluid");
            label.setToolTipText("Messages.Poi_View_Label_POI_Tooltip");
            GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL).applyTo(label);

            label = UI.createLabel(container, "Sodium");
            label.setToolTipText("Messages.Poi_View_Label_POI_Tooltip");
            GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL).applyTo(label);
         }

         /*
          * Totals
          */
         {
            final Label label = UI.createLabel(container, "Totals");
            label.setToolTipText("Messages.Poi_View_Label_POI_Tooltip");
            GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL).applyTo(label);

            _txtCalories_Total = new Text(container, SWT.READ_ONLY | SWT.TRAIL | SWT.BORDER);
            GridDataFactory.fillDefaults()//
                  .align(SWT.END, SWT.FILL)
                  .applyTo(_txtCalories_Total);

            _txtFluid_Total = new Text(container, SWT.READ_ONLY | SWT.TRAIL | SWT.BORDER);
            GridDataFactory.fillDefaults()//
                  .align(SWT.END, SWT.FILL)
                  .applyTo(_txtFluid_Total);

            _txtSodium_Total = new Text(container, SWT.READ_ONLY | SWT.TRAIL | SWT.BORDER);
            GridDataFactory.fillDefaults()//
                  .hint(_hintTextColumnWidth, SWT.DEFAULT)
                  .align(SWT.END, SWT.FILL)
                  .applyTo(_txtSodium_Total);
         }

         /*
          * Averages
          */
         {
            final Label label = UI.createLabel(container, "Averages");
            label.setToolTipText("Messages.Poi_View_Label_POI_Tooltip");
            GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL).applyTo(label);

            _txtCalories_Average = new Text(container, SWT.READ_ONLY | SWT.TRAIL | SWT.BORDER);
            GridDataFactory.fillDefaults()//
                  .align(SWT.END, SWT.FILL)
                  .applyTo(_txtCalories_Average);

            _txtFluid_Average = new Text(container, SWT.READ_ONLY | SWT.TRAIL | SWT.BORDER);
            GridDataFactory.fillDefaults()//
                  .align(SWT.END, SWT.FILL)
                  .applyTo(_txtFluid_Average);

            _txtSodium_Average = new Text(container, SWT.READ_ONLY | SWT.TRAIL | SWT.BORDER);
            GridDataFactory.fillDefaults()//
                  .align(SWT.END, SWT.FILL)
                  .applyTo(_txtSodium_Average);
         }

         /*
          * Grades
          */
         {
            /*
             * Label: Grade
             */
            final Label label = UI.createLabel(container, "Grade");
            label.setToolTipText("Messages.Poi_View_Label_POI_Tooltip");
            GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL).applyTo(label);
         }
      }
   }

   private void createUI_Section_20_ProductsList(final Composite parent) {

      _sectionProductsList = createSection(parent, _tk, "Products List" /*
                                                                         * Messages.
                                                                         * tour_editor_section_characteristics
                                                                         */, true);
      final Composite container = (Composite) _sectionProductsList.getClient();
      GridLayoutFactory.fillDefaults().applyTo(container);
      {
         // todo fb create buttons (remove item, add recent item, define container (flask...)
         createUI_210_Actions(container);
         createUI_220_Viewer(container);
      }
   }

   @Override
   public void dispose() {

      getSite().getPage().removeSelectionListener(_postSelectionListener);
      TourManager.getInstance().removeTourEventListener(_tourEventListener);
      _prefStore.removePropertyChangeListener(_prefChangeListener);
      _nutritionQuery.removePropertyChangeListener(this);

      UI.disposeResource(_imageAdd);
      UI.disposeResource(_imageDelete);

      super.dispose();
   }

   private void enableControls() {

      final boolean isTourSelected = _tourData != null;

      //_actionOpenSearchProduct.setEnabled(isTourSelected);
   }

   private void onSelectionChanged(final ISelection selection) {

      if (selection == null) {
         return;
      }

      long tourId = TourDatabase.ENTITY_IS_NOT_SAVED;
      TourData tourData = null;

      if (selection instanceof SelectionTourData) {

         // a tour was selected, get the chart and update the nutrition viewer

         final SelectionTourData tourDataSelection = (SelectionTourData) selection;
         tourData = tourDataSelection.getTourData();

      } else if (selection instanceof SelectionTourId) {

         tourId = ((SelectionTourId) selection).getTourId();

      } else if (selection instanceof SelectionTourIds) {

         final ArrayList<Long> tourIds = ((SelectionTourIds) selection).getTourIds();

         if (tourIds != null && tourIds.size() > 0) {

            if (tourIds.size() == 1) {
               tourId = tourIds.get(0);
            } else {
               tourData = TourManager.createJoinedTourData(tourIds);
            }
         }

      } else if (selection instanceof final SelectionReferenceTourView tourCatalogSelection) {

         final TVIRefTour_RefTourItem refItem = tourCatalogSelection.getRefItem();
         if (refItem != null) {
            tourId = refItem.getTourId();
         }

      } else if (selection instanceof StructuredSelection) {

         final Object firstElement = ((StructuredSelection) selection).getFirstElement();
         if (firstElement instanceof TVIRefTour_ComparedTour) {
            tourId = ((TVIRefTour_ComparedTour) firstElement).getTourId();
         } else if (firstElement instanceof TVIElevationCompareResult_ComparedTour) {
            tourId = ((TVIElevationCompareResult_ComparedTour) firstElement).getTourId();
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
      _btnDeleteProduct.setEnabled(true);
   }

   @Override
   public void propertyChange(final PropertyChangeEvent propertyChangeEvent) {

      final List<String> searchResult = (List<String>) propertyChangeEvent.getNewValue();

      Display.getDefault().asyncExec(() -> {

         // check if view is closed
//         if (_btnSearch.isDisposed()) {
//            return;
//         }

         // refresh viewer
         //todo fb fix java.lang.IllegalStateException: Need an underlying widget to be able to set the input.(Has the widget been disposed?)

         _productsViewer.setInput(new Object());

         _cboSearchQuery.setEnabled(true);
      });

   }

   private void restoreState() {

      // restore old used queries
      final String[] stateSearchedQueries = _state.getArray(STATE_SEARCHED_NUTRITIONQUERIES);
      if (stateSearchedQueries != null) {
         Stream.of(stateSearchedQueries).forEach(query -> _searchHistory.add(query));
      }

      _sectionProductsList.setExpanded(Util.getStateBoolean(_state, STATE_SECTION_PRODUCTS_LIST, true));
   }

   @PersistState
   private void saveState() {

      _state.put(STATE_SEARCHED_NUTRITIONQUERIES, _searchHistory.toArray(new String[_searchHistory.size()]));
      _state.put(STATE_SECTION_PRODUCTS_LIST, _sectionProductsList.isExpanded());
   }

   @Override
   public void setFocus() {

   }

   //TODO FB
   //https://fdc.nal.usda.gov/api-guide.html

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

      enableControls();
   }

   private void updateUI_SummaryFromModel() {

      final int totalCalories = NutritionUtils.getTotalCalories(_tourData.getTourNutritionProducts());
      _txtCalories_Total.setText(String.valueOf(totalCalories));

      final int totalFluid = (int) Math.round(NutritionUtils.getTotalFluids(_tourData.getTourNutritionProducts()));
      _txtFluid_Total.setText(String.valueOf(totalFluid));

      final int totalSodium = (int) NutritionUtils.getTotalSodium(_tourData.getTourNutritionProducts());
      _txtSodium_Total.setText(String.valueOf(totalSodium));

      final long averageCaloriesPerHour = computeAveragePerHour(totalCalories);
      _txtCalories_Average.setText(String.valueOf(averageCaloriesPerHour));

      final long averageSodiumPerHour = computeAveragePerHour(totalSodium);
      _txtSodium_Average.setText(String.valueOf(averageSodiumPerHour));

   }

}
