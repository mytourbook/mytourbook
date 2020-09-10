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

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;

import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

public class StatisticValuesView extends ViewPart {

   public static final String            ID         = "net.tourbook.statistic.StatisticValuesView"; //$NON-NLS-1$

   private static final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

   private IPropertyChangeListener       _prefChangeListener;
   private ITourEventListener            _tourEventListener;

   /*
    * UI controls
    */
   private PageBook                  _pageBook;

   private Composite                 _pageNoData;
   private Composite                 _pageContent;

   /**
    * With a label, the content can easily be scrolled but cannot be selected
    */
//   private Label     _txtAllFields;
   private Text                      _txtAllFields;

   private Selection_StatisticValues _selection_StatisticValues;

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
//               _txtAllFields.pack(true);

            } else if (property.equals(ITourbookPreferences.GRAPH_MARKER_IS_MODIFIED)) {

               updateUI();
            }
         }
      };
      _prefStore.addPropertyChangeListener(_prefChangeListener);
   }

   private void addTourEventListener() {

      _tourEventListener = new ITourEventListener() {
         @Override
         public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

            if (part == StatisticValuesView.this) {
               return;
            }

            if (eventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

               clearView();

            } else if ((eventId == TourEventId.STATISTIC_VALUES) && eventData instanceof Selection_StatisticValues) {

               onSelectionChanged((Selection_StatisticValues) eventData);
            }
         }
      };

      TourManager.getInstance().addTourEventListener(_tourEventListener);
   }

   private void clearView() {

      showInvalidPage();
   }

   private void createActions() {

   }

   @Override
   public void createPartControl(final Composite parent) {

      initUI();

      createUI(parent);
      createActions();

      addTourEventListener();
      addPrefListener();

      showInvalidPage();
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

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().applyTo(container);
      {
         _txtAllFields = new Text(container, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
         _txtAllFields.setFont(net.tourbook.ui.UI.getLogFont());
         GridDataFactory.fillDefaults().grab(true, true).applyTo(_txtAllFields);
      }
   }

   @Override
   public void dispose() {

      TourManager.getInstance().removeTourEventListener(_tourEventListener);

      _prefStore.removePropertyChangeListener(_prefChangeListener);

      super.dispose();
   }

   private void initUI() {

   }

   private void onSelectionChanged(final Selection_StatisticValues selection) {

      _selection_StatisticValues = selection;

      updateUI();
   }

   @PersistState
   private void saveState() {

   }

   @Override
   public void setFocus() {

      _txtAllFields.setFocus();
   }

   private void showInvalidPage() {

      _pageBook.showPage(_pageNoData);
   }

   void updateUI() {

      if (_selection_StatisticValues == null || _selection_StatisticValues.statisticValues == null) {

         showInvalidPage();
         return;
      }

      _pageBook.showPage(_pageContent);

      _txtAllFields.setText(_selection_StatisticValues.statisticValues);

   }

}
