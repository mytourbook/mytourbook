/*******************************************************************************
 * Copyright (C) 2020 Wolfgang Schramm and Contributors
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
package net.tourbook.statistic;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.SelectionTourMarker;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.views.tourCatalog.SelectionTourCatalogView;
import net.tourbook.ui.views.tourCatalog.TVICatalogComparedTour;
import net.tourbook.ui.views.tourCatalog.TVICatalogRefTourItem;
import net.tourbook.ui.views.tourCatalog.TVICompareResultComparedTour;

import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

public class StatisticValuesView extends ViewPart {

   public static final String            ID                         = "net.tourbook.statistic.StatisticValuesView"; //$NON-NLS-1$


   private static final IPreferenceStore _prefStore                 = TourbookPlugin.getPrefStore();
   private static final IDialogSettings  _state                     = TourbookPlugin.getState(ID);

   private static final String           STATE_VIEW_SCROLL_POSITION = "STATE_VIEW_SCROLL_POSITION";                //$NON-NLS-1$

   private PostSelectionProvider         _postSelectionProvider;
   private ISelectionListener            _postSelectionListener;
   private IPropertyChangeListener       _prefChangeListener;
   private ITourEventListener            _tourEventListener;

   private boolean                       _isUIRestored;

   private TourData                      _tourData;

   /*
    * UI controls
    */
   private PageBook  _pageBook;

   private Composite _pageNoData;
   private Composite _pageContent;

   /**
    * With a label, the content can easily be scrolled but cannot be selected
    */
   private Label     _txtAllFields;
//   private Text    _txtAllFields;

   private ScrolledComposite _uiContainer;

   private Composite         _infoContainer;

   private void addPrefListener() {

      _prefChangeListener = new IPropertyChangeListener() {
         @Override
         public void propertyChange(final PropertyChangeEvent event) {

            final String property = event.getProperty();

            if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

               // measurement system has changed

            } else if (property.equals(ITourbookPreferences.FONT_LOGGING_IS_MODIFIED)) {

               // update font

               _txtAllFields.setFont(net.tourbook.ui.UI.getLogFont());

               // relayout UI
               _txtAllFields.pack(true);
               onResize();

            } else if (property.equals(ITourbookPreferences.GRAPH_MARKER_IS_MODIFIED)) {

               updateUI();
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
            if (part == StatisticValuesView.this) {
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

            if (part == StatisticValuesView.this) {
               return;
            }

            if ((eventId == TourEventId.TOUR_CHANGED) && (eventData instanceof TourEvent)) {

               final ArrayList<TourData> modifiedTours = ((TourEvent) eventData).getModifiedTours();
               if (modifiedTours != null) {

                  // update modified tour

                  final long viewTourId = _tourData.getTourId();

                  for (final TourData tourData : modifiedTours) {
                     if (tourData.getTourId() == viewTourId) {

                        // get modified tour
                        _tourData = tourData;

                        // removed old tour data from the selection provider
                        _postSelectionProvider.clearSelection();

                        updateUI();

                        // nothing more to do, the view contains only one tour
                        return;
                     }
                  }
               }

            } else if (eventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

               clearView();

            } else if ((eventId == TourEventId.TOUR_SELECTION) && eventData instanceof ISelection) {

               onSelectionChanged((ISelection) eventData);

            } else if (eventId == TourEventId.MARKER_SELECTION) {

               if (eventData instanceof SelectionTourMarker) {

                  final TourData tourData = ((SelectionTourMarker) eventData).getTourData();

                  if (tourData != _tourData) {

                     _tourData = tourData;

                     updateUI();
                  }
               }
            }
         }
      };

      TourManager.getInstance().addTourEventListener(_tourEventListener);
   }

   private void clearView() {

      _tourData = null;

      // removed old tour data from the selection provider
      _postSelectionProvider.clearSelection();

      showInvalidPage();
   }

   private void createActions() {

   }

   @Override
   public void createPartControl(final Composite parent) {

      initUI();

      createUI(parent);
      createActions();

      addSelectionListener();
      addTourEventListener();
      addPrefListener();

      showInvalidPage();

      // this part is a selection provider
      getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider(ID));

      // show markers from last selection
      onSelectionChanged(getSite().getWorkbenchWindow().getSelectionService().getSelection());

      if (_tourData == null) {
         showTourFromTourProvider();
      }
   }

   private void createUI(final Composite parent) {

      _pageBook = new PageBook(parent, SWT.NONE);

      _pageNoData = UI.createUI_PageNoData(_pageBook, Messages.UI_Label_no_chart_is_selected);

      _pageContent = new Composite(_pageBook, SWT.NONE);
      _pageContent.setLayout(new FillLayout());
      {
         createUI_10_Container(_pageContent);
      }
   }

   private void createUI_10_Container(final Composite parent) {

      /*
       * scrolled container
       */
      _uiContainer = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
      _uiContainer.setExpandVertical(true);
      _uiContainer.setExpandHorizontal(true);
      _uiContainer.addControlListener(new ControlAdapter() {
         @Override
         public void controlResized(final ControlEvent e) {
            onResize();
         }
      });
      {
         _infoContainer = new Composite(_uiContainer, SWT.NONE);
         _infoContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
         GridDataFactory.fillDefaults().grab(true, true).applyTo(_infoContainer);
         GridLayoutFactory.swtDefaults().applyTo(_infoContainer);
         {
            createUI_30_AllFields(_infoContainer);
         }
      }

      // set content for scrolled composite
      _uiContainer.setContent(_infoContainer);
   }

   private void createUI_30_AllFields(final Composite parent) {

//      _txtAllFields = new Text(parent,
//            SWT.MULTI
//                  | SWT.BORDER //
//                  | SWT.READ_ONLY);

      _txtAllFields = new Label(parent,
            SWT.READ_ONLY
//                  | SWT.BORDER
//                  | SWT.WRAP
      );

      _txtAllFields.setFont(net.tourbook.ui.UI.getLogFont());

      GridDataFactory.fillDefaults()
            .grab(true, true)
            .applyTo(_txtAllFields);

      _txtAllFields.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

   }

   @Override
   public void dispose() {

      TourManager.getInstance().removeTourEventListener(_tourEventListener);

      getSite().getPage().removePostSelectionListener(_postSelectionListener);

      _prefStore.removePropertyChangeListener(_prefChangeListener);

      super.dispose();
   }

   private void initUI() {

   }

   private void onResize() {

      // horizontal scroll bar ishidden, only the vertical scrollbar can be displayed
      int infoContainerWidth = _uiContainer.getBounds().width;
      final ScrollBar vertBar = _uiContainer.getVerticalBar();

      if (vertBar != null) {

         // vertical bar is displayed
         infoContainerWidth -= vertBar.getSize().x;
      }

      final Point minSize = _infoContainer.computeSize(infoContainerWidth, SWT.DEFAULT);

      _uiContainer.setMinSize(minSize);
   }

   private void onSelectionChanged(final ISelection selection) {

      long tourId = TourDatabase.ENTITY_IS_NOT_SAVED;

      if (selection instanceof SelectionTourData) {

         // a tour is selected

         final SelectionTourData tourDataSelection = (SelectionTourData) selection;
         _tourData = tourDataSelection.getTourData();

         if (_tourData != null) {
            tourId = _tourData.getTourId();
         }

      } else if (selection instanceof SelectionTourId) {

         tourId = ((SelectionTourId) selection).getTourId();

      } else if (selection instanceof SelectionTourIds) {

         final ArrayList<Long> tourIds = ((SelectionTourIds) selection).getTourIds();
         if ((tourIds != null) && (tourIds.size() > 0)) {
            tourId = tourIds.get(0);
         }

      } else if (selection instanceof SelectionTourCatalogView) {

         final SelectionTourCatalogView tourCatalogSelection = (SelectionTourCatalogView) selection;

         final TVICatalogRefTourItem refItem = tourCatalogSelection.getRefItem();
         if (refItem != null) {
            tourId = refItem.getTourId();
         }

      } else if (selection instanceof StructuredSelection) {

         final Object firstElement = ((StructuredSelection) selection).getFirstElement();
         if (firstElement instanceof TVICatalogComparedTour) {
            tourId = ((TVICatalogComparedTour) firstElement).getTourId();
         } else if (firstElement instanceof TVICompareResultComparedTour) {
            tourId = ((TVICompareResultComparedTour) firstElement).getComparedTourData().getTourId();
         }

      } else if (selection instanceof SelectionDeletedTours) {

         clearView();
      }

      if (tourId >= TourDatabase.ENTITY_IS_NOT_SAVED) {

         final TourData tourData = TourManager.getInstance().getTourData(tourId);
         if (tourData != null) {
            _tourData = tourData;
         }
      }

      final boolean isTourAvailable = (tourId >= 0) && (_tourData != null);
      if (isTourAvailable) {
         updateUI();
      }
   }

   private void restoreState_UI() {

      if (_isUIRestored) {
         return;
      }

      _isUIRestored = true;

      final int scrollPos = Util.getStateInt(_state, STATE_VIEW_SCROLL_POSITION, -1);
      if (scrollPos != -1) {

         // scroll to the previous position

         _uiContainer.setOrigin(0, scrollPos);
      }
   }

   @PersistState
   private void saveState() {

      _state.put(STATE_VIEW_SCROLL_POSITION, _uiContainer.getVerticalBar().getSelection());
   }

   @Override
   public void setFocus() {

//      _pageBook.setFocus();
   }

   private void showInvalidPage() {

      _pageBook.showPage(_pageNoData);
   }

   private void showTourFromTourProvider() {

      showInvalidPage();

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

            if ((selectedTours != null) && (selectedTours.size() > 0)) {
               onSelectionChanged(new SelectionTourData(selectedTours.get(0)));
            }
         }
      });
   }

   /**
    * Update the UI from {@link #_tourData}.
    */
   void updateUI() {

      if (_tourData == null) {
         return;
      }

      _pageBook.showPage(_pageContent);

      /*
       * All Fields
       */
//      _txtAllFields.setText(getAllFieldsContent(_tourData));
      _txtAllFields.pack(true);
//      _txtAllFields.getParent().layout(true, true);

      /*
       * layout container to resize the labels
       */
      onResize();

      restoreState_UI();
   }

}
