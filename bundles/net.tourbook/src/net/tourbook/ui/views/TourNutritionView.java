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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.nutrition.NutritionQuery;
import net.tourbook.nutrition.NutritionUtils;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ActionOpenSearchProduct;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.views.referenceTour.SelectionReferenceTourView;
import net.tourbook.ui.views.referenceTour.TVIElevationCompareResult_ComparedTour;
import net.tourbook.ui.views.referenceTour.TVIRefTour_ComparedTour;
import net.tourbook.ui.views.referenceTour.TVIRefTour_RefTourItem;

import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

public class TourNutritionView extends ViewPart implements PropertyChangeListener {

   //TODO FB
   //https://fdc.nal.usda.gov/api-guide.html

//https://world.openfoodfacts.org/data
   public static final String            ID                              = "net.tourbook.ui.views.TourNutritionView"; //$NON-NLS-1$

   private static final String           STATE_SEARCHED_NUTRITIONQUERIES = "searched.nutritionQueries";               //$NON-NLS-1$

   private static final IPreferenceStore _prefStore                      = TourbookPlugin.getPrefStore();
   private final IDialogSettings         _state                          = TourbookPlugin.getState(ID);

   private TourData                      _tourData;

   private TableViewer                   _productsViewer;
   private List<String>                  _searchHistory                  = new ArrayList<>();

   private IPropertyChangeListener       _prefChangeListener;
   final NutritionQuery                  _nutritionQuery                 = new NutritionQuery();
   private ISelectionListener            _postSelectionListener;
   private ITourEventListener            _tourEventListener;

   private PostSelectionProvider         _postSelectionProvider;

   /*
    * UI controls
    */
   private PageBook                _pageBook;
   private Composite               _pageNoData;
   private Composite               _viewerContainer;
   private boolean                 _isInUpdate;

   private Text                    _txtCalories;


   private Button                  _btnSearch;

   private Combo                   _cboSearchQuery;

   private Section                 _sectionProductsList;
   private FormToolkit             _tk;
   private ActionOpenSearchProduct _actionOpenSearchProduct;


   private class ViewContentProvider implements IStructuredContentProvider {

      @Override
      public void dispose() {}

      @Override
      public Object[] getElements(final Object parent) {

         if (_tourData == null || _tourData.isMultipleTours()) {
            return new Object[0];
         }

         final Object[] tourFuelProducts = _tourData.getTourFuelProducts().toArray();

//            System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//                  + ("\n\t_tourData: " + _tourData));
//            // TODO remove SYSTEM.OUT.PRINTLN

         return tourFuelProducts;
      }

      @Override
      public void inputChanged(final Viewer v, final Object oldInput, final Object newInput) {}
   }

   class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {

      @Override
      public Image getColumnImage(final Object obj, final int index) {
         switch (index) {
         case 0:
            return getImage(obj);

         default:
            return null;
         }
      }

      @Override
      public String getColumnText(final Object obj, final int index) {

         return obj.toString();
//         final PointOfInterest poi = (PointOfInterest) obj;
//
//         switch (index) {
//         case 0:
//            return poi.getCategory();
//         case 1:
//
//            final StringBuilder sb = new StringBuilder(poi.getName());
//
//            final List<? extends Waypoint> nearestPlaces = poi.getNearestPlaces();
//            if (nearestPlaces != null && nearestPlaces.size() > 0) {
//
//               // create a string with all nearest waypoints
//               boolean isFirstPoi = true;
//               for (final Waypoint waypoint : nearestPlaces) {
//
//                  if (isFirstPoi) {
//                     isFirstPoi = false;
//                     sb.append("Messages.Poi_View_Label_NearestPlacesPart1");
//                     sb.append("Messages.Poi_View_Label_Near");
//                  } else {
//                     sb.append("Messages.Poi_View_Label_NearestPlacesPart2");
//                  }
//
//                  sb.append("Messages.Poi_View_Label_NearestPlacesPart3");
//                  sb.append(waypoint.getName());
//               }
//               sb.append("Messages.Poi_View_Label_NearestPlacesPart4");
//            }
//            return sb.toString();
//         default:
//            return getText(obj);
//         }
      }

      @Override
      public Image getImage(final Object obj) {

         return null;
      }
   }

   public TourNutritionView() {}

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

         if (tourEventId == TourEventId.TOUR_SELECTION && eventData instanceof ISelection) {

            onSelectionChanged((ISelection) eventData);

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

   private void createActions() {

      _actionOpenSearchProduct = new ActionOpenSearchProduct();

   }

   @Override
   public void createPartControl(final Composite parent) {

      createUI(parent);

      addSelectionListener();
      addTourEventListener();

      createActions();
      fillToolbar();

      addPrefListener();

      // this part is a selection provider
      _postSelectionProvider = new PostSelectionProvider(ID);
      getSite().setSelectionProvider(_postSelectionProvider);

      // show default page
      _pageBook.showPage(_pageNoData);

      restoreState();
   }

   private Section createSection(final Composite parent,
                                 final FormToolkit tk,
                                 final String title,
                                 final boolean isGrabVertical,
                                 final boolean isExpandable) {

      final int style = isExpandable
            ? Section.TWISTIE | Section.TITLE_BAR
            : Section.TITLE_BAR;

      final Section section = tk.createSection(parent, style);

      section.setText(title);
      GridDataFactory.fillDefaults().grab(true, isGrabVertical).applyTo(section);

      final Composite sectionContainer = tk.createComposite(section);
      section.setClient(sectionContainer);

      return section;
   }

   private void createUI(final Composite parent) {

      _pageBook = new PageBook(parent, SWT.NONE);

      _pageNoData = net.tourbook.common.UI.createUI_PageNoData(_pageBook, Messages.UI_Label_no_chart_is_selected);

      _tk = new FormToolkit(parent.getDisplay());

      _viewerContainer = new Composite(_pageBook, SWT.NONE);
      GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
      {
         createUI_Section_10_Summary(_viewerContainer);
         createUI_Section_20_ProductsList(_viewerContainer);
      }
   }

   private void createUI_210_Viewer(final Composite parent) {

      /*
       * table viewer: poi items
       */
      final Table poiTable = new Table(parent, /* SWT.BORDER | */SWT.SINGLE | SWT.FULL_SELECTION);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(poiTable);
      poiTable.setLinesVisible(true);
      poiTable.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));
      poiTable.setHeaderVisible(true);

      // column: category
      final TableColumn columnCategory = new TableColumn(poiTable, SWT.LEFT);
      columnCategory.setText("Category"); //$NON-NLS-1$
      columnCategory.setWidth(75);

      // column: name
      final TableColumn columnName = new TableColumn(poiTable, SWT.LEFT);
      columnName.setText("Name"); //$NON-NLS-1$
      columnName.setWidth(300);

      _productsViewer = new TableViewer(poiTable);

      _productsViewer.setContentProvider(new ViewContentProvider());
      _productsViewer.setLabelProvider(new ViewLabelProvider());
   }

   private void createUI_Section_10_Summary(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         /*
          * Label: Calories
          */
         Label label = UI.createLabel(container, "Summary/Report card");
         label.setToolTipText("Messages.Poi_View_Label_POI_Tooltip");
         GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL).applyTo(label);

         _txtCalories = new Text(container, SWT.READ_ONLY | SWT.TRAIL | SWT.BORDER);
         GridDataFactory.fillDefaults()//
               .align(SWT.END, SWT.FILL)
               .applyTo(_txtCalories);

         /*
          * Label: Carbohydrates
          */
         label = UI.createLabel(container, "Carbohydrates");
         label.setToolTipText("Messages.Poi_View_Label_POI_Tooltip");
         GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL).applyTo(label);
      }
   }

   private void createUI_Section_20_ProductsList(final Composite parent) {

      //put a link with "Not finding the product you used ? You can create it here"
      //https://world.openfoodfacts.org/cgi/product.pl

      _sectionProductsList = createSection(parent, _tk, "Products List" /*
                                                                         * Messages.
                                                                         * tour_editor_section_characteristics
                                                                         */, false, true);
      final Composite container = (Composite) _sectionProductsList.getClient();
      GridLayoutFactory.fillDefaults().numColumns(4).applyTo(container);
      {
         createUI_210_Viewer(container);
      }
   }

   @Override
   public void dispose() {

      _prefStore.removePropertyChangeListener(_prefChangeListener);
      _nutritionQuery.removePropertyChangeListener(this);

      super.dispose();
   }

   private void enableActions() {
      // TODO Auto-generated method stub

   }

   private void fillToolbar() {

      /*
       * fill view toolbar
       */
      final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

      tbm.add(new Separator());
      tbm.add(_actionOpenSearchProduct);

      tbm.update(true);
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
      _actionOpenSearchProduct.setTourData(_tourData);

      updateUI_ProductViewer();
   }

   @Override
   public void propertyChange(final PropertyChangeEvent propertyChangeEvent) {

      final List<String> searchResult = (List<String>) propertyChangeEvent.getNewValue();

      Display.getDefault().asyncExec(() -> {

         // check if view is closed
         if (_btnSearch.isDisposed()) {
            return;
         }

         // refresh viewer
         _productsViewer.setInput(new Object());

         _cboSearchQuery.setEnabled(true);
         _btnSearch.setEnabled(true);
      });

   }

   private void restoreState() {

      // restore old used queries
      final String[] stateSearchedQueries = _state.getArray(STATE_SEARCHED_NUTRITIONQUERIES);
      if (stateSearchedQueries != null) {
         Stream.of(stateSearchedQueries).forEach(query -> _searchHistory.add(query));
      }
   }

   @PersistState
   private void saveState() {

      _state.put(STATE_SEARCHED_NUTRITIONQUERIES, _searchHistory.toArray(new String[_searchHistory.size()]));
   }

   @Override
   public void setFocus() {

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

         _pageBook.showPage(_viewerContainer);
      }

      updateUI_SummaryFromModel();

      enableActions();
   }

   private void updateUI_SummaryFromModel() {

      _txtCalories.setText(NutritionUtils.getTotalCalories(_tourData.getTourFuelProducts()));

   }

}
