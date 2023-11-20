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

import de.byteholder.geoclipse.poi.PostSelectionProvider;
import de.byteholder.gpx.PointOfInterest;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import net.tourbook.Images;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.nutrition.NutritionQuery;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ActionOpenSearchProduct;

import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
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
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.ViewPart;

public class TourNutritionView extends ViewPart implements PropertyChangeListener {

   public static final String            ID                              = "net.tourbook.ui.views.TourNutritionView"; //$NON-NLS-1$

   private static final String           STATE_SEARCHED_NUTRITIONQUERIES = "searched.nutritionQueries";               //$NON-NLS-1$

   private static final String           IMG_KEY_ANCHOR                  = "anchor";                                  //$NON-NLS-1$
   private static final String           IMG_KEY_CAR                     = "car";                                     //$NON-NLS-1$
   private static final String           IMG_KEY_CART                    = "cart";                                    //$NON-NLS-1$
   private static final String           IMG_KEY_FLAG                    = "flag";                                    //$NON-NLS-1$
   private static final String           IMG_KEY_HOUSE                   = "house";                                   //$NON-NLS-1$
   private static final String           IMG_KEY_SOCCER                  = "soccer";                                  //$NON-NLS-1$
   private static final String           IMG_KEY_STAR                    = "star";                                    //$NON-NLS-1$

   private static final IPreferenceStore _prefStore                      = TourbookPlugin.getPrefStore();
   private final IDialogSettings         _state                          = TourbookPlugin.getState(ID);

   private TableViewer                   _productsViewer;
   private List<String>                  _pois;
   private List<String>                  _searchHistory                  = new ArrayList<>();

   private IPropertyChangeListener       _prefChangeListener;
   final NutritionQuery                  _nutritionQuery                 = new NutritionQuery();
   private PostSelectionProvider         _postSelectionProvider;

   /*
    * UI controls
    */
   private Button                        _btnSearch;

   private Combo                         _cboSearchQuery;

   private Section                       _sectionProductsList;
   private FormToolkit                    _tk;
   private ActionOpenSearchProduct       _actionOpenSearchProduct;

   public class SearchContentProvider implements IStructuredContentProvider {

      @Override
      public void dispose() {}

      @Override
      public Object[] getElements(final Object inputElement) {
         return _searchHistory.toArray(new String[_searchHistory.size()]);
      }

      @Override
      public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
   }
   class ViewContentProvider implements IStructuredContentProvider {

      @Override
      public void dispose() {}

      @Override
      public Object[] getElements(final Object parent) {
         if (_pois == null) {
            return new String[] {};
         } else {
            return _pois.toArray();
         }
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

         if (obj instanceof PointOfInterest) {

            Image img;
            final PointOfInterest poi = (PointOfInterest) obj;

            // TODO find/make better matching icons

            final ImageRegistry imageRegistry = TourbookPlugin.getDefault().getImageRegistry();
            final String poiCategory = poi.getCategory();

            if (poiCategory.equals("highway")) { //$NON-NLS-1$
               img = imageRegistry.get(IMG_KEY_CAR);
            } else if (poiCategory.equals("place")) { //$NON-NLS-1$
               img = imageRegistry.get(IMG_KEY_HOUSE);
            } else if (poiCategory.equals("waterway")) { //$NON-NLS-1$
               img = imageRegistry.get(IMG_KEY_ANCHOR);
            } else if (poiCategory.equals("amenity")) { //$NON-NLS-1$
               img = imageRegistry.get(IMG_KEY_CART);
            } else if (poiCategory.equals("leisure")) { //$NON-NLS-1$
               img = imageRegistry.get(IMG_KEY_STAR);
            } else if (poiCategory.equals("sport")) { //$NON-NLS-1$
               img = imageRegistry.get(IMG_KEY_SOCCER);
            } else {
               img = imageRegistry.get(IMG_KEY_FLAG);
            }

            return img;
         } else {
            return null;
         }
      }
   }

   public TourNutritionView() {}

   public TourNutritionView(final List<String> pois) {
      _pois = pois;
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

   private void createActions() {


      _actionOpenSearchProduct = new ActionOpenSearchProduct();

   }

   @Override
   public void createPartControl(final Composite parent) {

      initImageRegistry();

      createUI(parent);

      createActions();
      fillToolbar();

      addPrefListener();

      // this part is a selection provider
      //getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider());

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

      final Display display = parent.getDisplay();

      _tk = new FormToolkit(display);

      final Composite container = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().spacing(0, 0).numColumns(1).applyTo(container);
      {
         createUI_Section_10_Summary(container);
         createUI_Section_20_ProductsList(container);
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

      _productsViewer.addPostSelectionChangedListener(selectionChangedEvent -> {

         final ISelection selection = selectionChangedEvent.getSelection();
         final Object firstElement = ((IStructuredSelection) selection).getFirstElement();
         final PointOfInterest selectedPoi = (PointOfInterest) firstElement;

         _postSelectionProvider.setSelection(selectedPoi);
      });
   }

   private void createUI_Section_10_Summary(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         /*
          * Label: Calories
          */
         final Label label = UI.createLabel(parent, "Summary/Report card");
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

   private void fillToolbar() {

      /*
       * fill view toolbar
       */
      final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

      tbm.add(new Separator());
      tbm.add(_actionOpenSearchProduct);

      tbm.update(true);
   }

   private void initImageRegistry() {

      final TourbookPlugin activator = TourbookPlugin.getDefault();
      final ImageRegistry imageRegistry = activator.getImageRegistry();

      if (imageRegistry.get(Images.POI_Anchor) == null) {

         imageRegistry.put(IMG_KEY_ANCHOR, TourbookPlugin.getImageDescriptor(Images.POI_Anchor));
         imageRegistry.put(IMG_KEY_CAR, TourbookPlugin.getImageDescriptor(Images.POI_Car));
         imageRegistry.put(IMG_KEY_CART, TourbookPlugin.getImageDescriptor(Images.POI_Cart));
         imageRegistry.put(IMG_KEY_FLAG, TourbookPlugin.getImageDescriptor(Images.POI_Flag));
         imageRegistry.put(IMG_KEY_HOUSE, TourbookPlugin.getImageDescriptor(Images.POI_House));
         imageRegistry.put(IMG_KEY_SOCCER, TourbookPlugin.getImageDescriptor(Images.POI_Soccer));
         imageRegistry.put(IMG_KEY_STAR, TourbookPlugin.getImageDescriptor(Images.POI_Star));
      }
   }

   @Override
   public void propertyChange(final PropertyChangeEvent evt) {

      final List<String> searchResult = (List<String>) evt.getNewValue();

      if (searchResult != null) {
         _pois = searchResult;
      }

      Display.getDefault().asyncExec(() -> {

         // check if view is closed
         if (_btnSearch.isDisposed()) {
            return;
         }

         // refresh viewer
         _productsViewer.setInput(new Object());

         // select first entry, if there is one
         final Table poiTable = _productsViewer.getTable();
         if (poiTable.getItemCount() > 0) {

            final Object firstData = poiTable.getItem(0).getData();
            if (firstData instanceof PointOfInterest) {

               _productsViewer.setSelection(new StructuredSelection(firstData));
               setViewerFocus();
            }
         }

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

   /**
    * set focus to selected item, selection and focus are not the same !!!
    */
   private void setViewerFocus() {

      final Table table = _productsViewer.getTable();

      table.setSelection(table.getSelectionIndex());
      table.setFocus();
   }

}
