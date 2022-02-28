/*******************************************************************************
 * Copyright (C) 2022 Frédéric Bard
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

import java.text.NumberFormat;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.data.TourData;
import net.tourbook.database.IComputeTourValues;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.WeatherProvider;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.PageBook;

public class WeatherProvidersUI {

   private static final WeatherProvider[] WEATHER_PROVIDER = {

         new WeatherProvider(
               IWeatherProvider.SMOOTHING_ALGORITHM_INITIAL,
               Messages.TourChart_Smoothing_Algorithm_Initial),

         new WeatherProvider(
               IWeatherProvider.SMOOTHING_ALGORITHM_JAMET,
               Messages.TourChart_Smoothing_Algorithm_Jamet),

         new WeatherProvider(
               IWeatherProvider.SMOOTHING_ALGORITHM_NO_SMOOTHING,
               Messages.TourChart_Smoothing_Algorithm_NoSmoothing),
   };

   private final IPreferenceStore            _prefStore          = TourbookPlugin.getPrefStore();

   private ITourEventListener                _tourEventListener;

   private FocusListener                     _keepOpenListener;
   {
      _keepOpenListener = new FocusListener() {

         @Override
         public void focusGained(final FocusEvent e) {

            if (_slideout != null) {

               /*
                * This will fix the problem that when the list of a combobox is displayed, then
                * the slideout will disappear :-(((
                */
               _slideout.setIsAnotherDialogOpened(true);
            }
         }

         @Override
         public void focusLost(final FocusEvent e) {

            if (_slideout != null) {
               _slideout.setIsAnotherDialogOpened(false);
            }

         }
      };
   }

   private boolean             _isUpdateUI;

   private ISmoothingAlgorithm _smoothing_Initial     = new SmoothingUI_Initial();
   private ISmoothingAlgorithm _smoothing_Jamet       = new SmoothingUI_Jamet();
   private ISmoothingAlgorithm _smoothing_NoSmoothing = new SmoothingUI_NoSmoothing();

   private NumberFormat        _nf0                   = NumberFormat.getNumberInstance();

   {
      _nf0.setMinimumFractionDigits(0);
      _nf0.setMaximumFractionDigits(0);
   }

   /*
    * UI controls
    */
   private FormToolkit     _tk;

   private Composite       _uiContainer;

   private Combo           _comboAlgorithm;

   private PageBook        _pagebookSmoothingAlgo;

   private Composite       _pageInitialUI;
   private Composite       _pageJametUI;
   private Composite       _pageNoSmoothingUI;

   private ToolbarSlideout _slideout;

   public WeatherProvidersUI() {}

   /**
    * @param tk
    *           This toolkit will be disposed when the UI is disposed;
    * @param toolbarSlideout
    */
   public WeatherProvidersUI(final FormToolkit tk, final ToolbarSlideout slideout) {

      _tk = tk;
      _slideout = slideout;
   }

   private void addTourEventListener() {

      _tourEventListener = new ITourEventListener() {
         @Override
         public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

            // don't listen to the own events
            if (part == WeatherProvidersUI.this) {
               return;
            }

            if (eventId == TourEventId.TOUR_CHART_PROPERTY_IS_MODIFIED) {
               updateUI_FromPropertyEvent();
            }
         }
      };

      TourManager.getInstance().addTourEventListener(_tourEventListener);
   }

   public void computeSmoothingForAllTours() {

      // get smoothing algorithm
      final String prefAlgoId = _prefStore.getString(ITourbookPreferences.GRAPH_SMOOTHING_SMOOTHING_ALGORITHM);
      int prefAlgoIndex = -1;
      for (int algoIndex = 0; algoIndex < SMOOTHING_ALGORITHM.length; algoIndex++) {
         if (SMOOTHING_ALGORITHM[algoIndex].algorithmId.equals(prefAlgoId)) {
            prefAlgoIndex = algoIndex;
            break;
         }
      }
      if (prefAlgoIndex == -1) {
         // this case should not happen
         prefAlgoIndex = 0;
      }

      if (MessageDialog.openConfirm(
            Display.getCurrent().getActiveShell(),
            Messages.TourChart_Smoothing_Dialog_SmoothAllTours_Title,
            NLS.bind(
                  Messages.TourChart_Smoothing_Dialog_SmoothAllTours_Message,
                  SMOOTHING_ALGORITHM[prefAlgoIndex].uiText)) == false) {
         return;
      }

      final IComputeTourValues computeTourValueConfig = new IComputeTourValues() {

         @Override
         public boolean computeTourValues(final TourData oldTourData) {

            oldTourData.computeComputedValues();

            return true;
         }

         @Override
         public String getResultText() {
            return null;
         }

         @Override
         public String getSubTaskText(final TourData savedTourData) {
            return null;
         }
      };

      TourDatabase.computeAnyValues_ForAllTours(computeTourValueConfig, null);

      fireTourModifyEvent();
   }

   public void createUI(final Composite parent,
                        final boolean isShowDescription,
                        final boolean isShowAdditionalActions) {

      initUI(parent);

      createUI_10(parent, isShowDescription, isShowAdditionalActions);

      setupUI();

      restoreState();
      updateUI();

      addTourEventListener();
   }

   private void createUI_10(final Composite parent,
                            final boolean isShowDescription,
                            final boolean isShowAdditionalActions) {

      _uiContainer = _tk.createComposite(parent);
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .applyTo(_uiContainer);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(_uiContainer);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
      {
         createUI_10_SmoothingAlgorithm(_uiContainer);
         createUI_20_SmoothingPagebook(_uiContainer, isShowDescription, isShowAdditionalActions);
      }
   }

   private void createUI_10_SmoothingAlgorithm(final Composite parent) {

      final Composite container = _tk.createComposite(parent);
      GridDataFactory.fillDefaults().grab(false, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).extendedMargins(0, 0, 0, 5).applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
      {
         /*
          * label: smoothing algorithm
          */
         final Label label = _tk.createLabel(container, Messages.TourChart_Smoothing_Label_SmoothingAlgorithm);
         GridDataFactory.fillDefaults()
               .align(SWT.FILL, SWT.CENTER)
               .applyTo(label);

         /*
          * combo: smoothing algorithm
          */
         _comboAlgorithm = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
         _comboAlgorithm.setVisibleItemCount(10);
         _comboAlgorithm.addFocusListener(_keepOpenListener);
         _comboAlgorithm.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               if (_isUpdateUI) {
                  return;
               }
               onSelectSmoothingAlgo();
            }
         });
         GridDataFactory.fillDefaults()
//               .align(SWT.END, SWT.FILL)
               .indent(20, 0)
               .applyTo(_comboAlgorithm);
         _tk.adapt(_comboAlgorithm, true, true);
      }
   }

   private void createUI_20_SmoothingPagebook(final Composite parent,
                                              final boolean isShowDescription,
                                              final boolean isShowAdditionalActions) {
      /*
       * pagebook: smoothing algorithm
       */
      _pagebookSmoothingAlgo = new PageBook(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .span(2, 1)
            .applyTo(_pagebookSmoothingAlgo);
      {
         _pageInitialUI = _smoothing_Initial.createUI(
               this,
               _pagebookSmoothingAlgo,
               _tk,
               isShowDescription,
               isShowAdditionalActions);

         _pageJametUI = _smoothing_Jamet.createUI(
               this,
               _pagebookSmoothingAlgo,
               _tk,
               isShowDescription,
               isShowAdditionalActions);

         _pageNoSmoothingUI = _smoothing_NoSmoothing.createUI(
               this,
               _pagebookSmoothingAlgo,
               _tk,
               isShowDescription,
               isShowAdditionalActions);
      }
   }

   public void dispose() {

      TourManager.getInstance().removeTourEventListener(_tourEventListener);

      _smoothing_Initial.dispose();
      _smoothing_Jamet.dispose();
      _smoothing_NoSmoothing.dispose();

      _tk.dispose();
   }

   private void fireTourModifyEvent() {

      TourManager.getInstance().removeAllToursFromCache();
      TourManager.fireEvent(TourEventId.CLEAR_DISPLAYED_TOUR);

      // fire unique event for all changes
      TourManager.fireEvent(TourEventId.ALL_TOURS_ARE_MODIFIED);
   }

   private SmoothingAlgorithm getSelectedAlgorithm() {
      return SMOOTHING_ALGORITHM[_comboAlgorithm.getSelectionIndex()];
   }

   private void initUI(final Composite parent) {

      if (_tk == null) {

         // it could be already created
         _tk = new FormToolkit(parent.getDisplay());
      }
   }

   protected void onModifySmoothingAlgo() {}

   private void onSelectSmoothingAlgo() {

      updateUI();

      // update pref store
      saveState();

      // force tours to be recomputed
      TourManager.getInstance().removeAllToursFromCache();

      // fire unique event for all changes
      TourManager.fireEvent(TourEventId.TOUR_CHART_PROPERTY_IS_MODIFIED);
   }

   public void performDefaults() {

      final String defaultSmoothingId = _prefStore.getDefaultString(//
            ITourbookPreferences.GRAPH_SMOOTHING_SMOOTHING_ALGORITHM);

      _prefStore.setValue(ITourbookPreferences.GRAPH_SMOOTHING_SMOOTHING_ALGORITHM, defaultSmoothingId);

      selectSmoothingAlgo(defaultSmoothingId);

      updateUI();

      final String algorithmId = getSelectedAlgorithm().algorithmId;

      final boolean isInitialSelected = algorithmId.equals(ISmoothingAlgorithm.SMOOTHING_ALGORITHM_INITIAL);
      _smoothing_Initial.performDefaults(isInitialSelected);

      final boolean isJametSelected = algorithmId.equals(ISmoothingAlgorithm.SMOOTHING_ALGORITHM_JAMET);
      _smoothing_Jamet.performDefaults(isJametSelected);

      final boolean isNoSmoothingSelected = algorithmId.equals(ISmoothingAlgorithm.SMOOTHING_ALGORITHM_NO_SMOOTHING);
      _smoothing_NoSmoothing.performDefaults(isNoSmoothingSelected);
   }

   private void restoreState() {

      _isUpdateUI = true;
      {
         // smoothing algorithm
         selectSmoothingAlgo(_prefStore.getString(ITourbookPreferences.GRAPH_SMOOTHING_SMOOTHING_ALGORITHM));
      }
      _isUpdateUI = false;
   }

   /**
    * Update new values in the pref store
    */
   private void saveState() {

      // smoothing algorithm
      _prefStore.setValue(//
            ITourbookPreferences.GRAPH_SMOOTHING_SMOOTHING_ALGORITHM,
            getSelectedAlgorithm().algorithmId);
   }

   private void selectSmoothingAlgo(final String prefAlgoId) {

      int prefAlgoIndex = -1;
      for (int algoIndex = 0; algoIndex < SMOOTHING_ALGORITHM.length; algoIndex++) {
         if (SMOOTHING_ALGORITHM[algoIndex].algorithmId.equals(prefAlgoId)) {
            prefAlgoIndex = algoIndex;
            break;
         }
      }
      if (prefAlgoIndex == -1) {
         prefAlgoIndex = 0;
      }
      _comboAlgorithm.select(prefAlgoIndex);
   }

   private void setupUI() {

      _isUpdateUI = true;
      {
         /*
          * fillup algorithm combo
          */
         for (final SmoothingAlgorithm algo : SMOOTHING_ALGORITHM) {
            _comboAlgorithm.add(algo.uiText);
         }
         _comboAlgorithm.select(0);
      }
      _isUpdateUI = false;
   }

   private void updateUI() {

      final String selectedSmoothingAlgo = getSelectedAlgorithm().algorithmId;

      // select smoothing page
      if (selectedSmoothingAlgo.equals(ISmoothingAlgorithm.SMOOTHING_ALGORITHM_INITIAL)) {

         _pagebookSmoothingAlgo.showPage(_pageInitialUI);

      } else if (selectedSmoothingAlgo.equals(ISmoothingAlgorithm.SMOOTHING_ALGORITHM_JAMET)) {

         _pagebookSmoothingAlgo.showPage(_pageJametUI);

      } else if (selectedSmoothingAlgo.equals(ISmoothingAlgorithm.SMOOTHING_ALGORITHM_NO_SMOOTHING)) {

         _pagebookSmoothingAlgo.showPage(_pageNoSmoothingUI);
      }

      UI.updateScrolledContent(_uiContainer);

      // fire event to pack the UI, this is needed when the UI is in a slideout
      onModifySmoothingAlgo();
   }

   private void updateUI_FromPropertyEvent() {

      _smoothing_Initial.updateUIFromPrefStore();
      _smoothing_Jamet.updateUIFromPrefStore();
      _smoothing_NoSmoothing.updateUIFromPrefStore();

      restoreState();

      updateUI();
   }

}
