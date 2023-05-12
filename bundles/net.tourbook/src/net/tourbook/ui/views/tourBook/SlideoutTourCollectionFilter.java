/*******************************************************************************
 * Copyright (C) 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourBook;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.ui.views.tourBook.TourBookView.TourCollectionFilter;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;

public class SlideoutTourCollectionFilter extends ToolbarSlideout {

   private TourBookView      _tourBookView;

   private SelectionListener _defaultSelectionListener;

   private boolean           _isFilterActive;

   /*
    * UI controls
    */
   private Composite _shellContainer;

   private Button    _rdoShowAllTours;
   private Button    _rdoShowCollectedTours;
   private Button    _rdoShowNotCollectedTours;

   private Label     _lblNumAllTours;
   private Label     _lblNumAllTours_Value;
   private Label     _lblNumCollectedTours;
   private Label     _lblNumCollectedTours_Value;

   /**
    * @param ownerControl
    * @param toolBar
    * @param tourBookView
    * @param state
    * @param gridPrefPrefix
    */
   public SlideoutTourCollectionFilter(final Control ownerControl,
                                       final ToolBar toolBar,
                                       final TourBookView tourBookView) {

      super(ownerControl, toolBar);

      _tourBookView = tourBookView;
   }

   @Override
   protected Composite createToolTipContentArea(final Composite parent) {

      initUI();

      final Composite ui = createUI(parent);

      restoreState();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      _shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.swtDefaults().applyTo(_shellContainer);
      {
         final Composite container = new Composite(_shellContainer, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         GridLayoutFactory.fillDefaults()
//					.numColumns(2)
               .applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
         {
            createUI_10_Title(container);
            createUI_20_Filter(container);
         }
      }

      return _shellContainer;
   }

   private void createUI_10_Title(final Composite parent) {

      /*
       * Label: Slideout title
       */
      final Label label = new Label(parent, SWT.NONE);
      GridDataFactory.fillDefaults().applyTo(label);
      label.setText(Messages.Slideout_TourCollectionFilter_Label_Title);
      label.setFont(JFaceResources.getBannerFont());

      MTFont.setBannerFont(label);
   }

   private void createUI_20_Filter(final Composite parent) {

      if (_isFilterActive) {

         createUI_22_FilterOptions(parent);

      } else {

         /*
          * Show filter hint, 2 weeks after initial development, I didn't knew how to enable the
          * options, so I created this hint
          */

         UI.createLabel(parent, Messages.Slideout_TourCollectionFilter_Label_FilterHint);
      }

      createUI_24_FilterInfo(parent);
   }

   private void createUI_22_FilterOptions(final Composite parent) {

      {
         /*
          * Radio: Show all tours
          */
         _rdoShowAllTours = new Button(parent, SWT.RADIO);
         _rdoShowAllTours.setText(Messages.Slideout_TourCollectionFilter_Radio_ShowAllTours);
         _rdoShowAllTours.setToolTipText(Messages.Slideout_TourCollectionFilter_Radio_ShowAllTours_Tooltip);
         _rdoShowAllTours.addSelectionListener(_defaultSelectionListener);

         GridDataFactory.fillDefaults().span(2, 1).applyTo(_rdoShowAllTours);
      }
      {
         /*
          * Radio: Show only selected tours
          */
         _rdoShowCollectedTours = new Button(parent, SWT.RADIO);
         _rdoShowCollectedTours.setText(Messages.Slideout_TourCollectionFilter_Radio_ShowCollectedTours);
         _rdoShowCollectedTours.addSelectionListener(_defaultSelectionListener);

         GridDataFactory.fillDefaults().span(2, 1).applyTo(_rdoShowCollectedTours);

      }
      {
         /*
          * Radio: Show only not selected tours
          */
         _rdoShowNotCollectedTours = new Button(parent, SWT.RADIO);
         _rdoShowNotCollectedTours.setText(Messages.Slideout_TourCollectionFilter_Radio_ShowNotCollectedTours);
         _rdoShowNotCollectedTours.addSelectionListener(_defaultSelectionListener);

         GridDataFactory.fillDefaults().span(2, 1).applyTo(_rdoShowNotCollectedTours);
      }
   }

   private void createUI_24_FilterInfo(final Composite parent) {

      final String showAllTours_Tooltip = Messages.Slideout_TourCollectionFilter_Radio_ShowAllTours_Tooltip;

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//      container.setBackground(UI.SYS_COLOR_BLUE);
      {
         {
            /*
             * Label: Number of all tours
             */
            _lblNumAllTours = new Label(container, SWT.NONE);
            _lblNumAllTours.setText(Messages.Slideout_TourCollectionFilter_Label_NumberOfAllTours);
            _lblNumAllTours.setToolTipText(showAllTours_Tooltip);
            GridDataFactory.fillDefaults().applyTo(_lblNumAllTours);

            _lblNumAllTours_Value = new Label(container, SWT.NONE);
            _lblNumAllTours_Value.setText(UI.SPACE8);
            _lblNumAllTours_Value.setToolTipText(showAllTours_Tooltip);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblNumAllTours_Value);
         }
         {
            /*
             * Label: Number of selected tours
             */
            _lblNumCollectedTours = new Label(container, SWT.NONE);
            _lblNumCollectedTours.setText(Messages.Slideout_TourCollectionFilter_Label_NumberOfCollectedTours);
            GridDataFactory.fillDefaults().applyTo(_lblNumCollectedTours);

            _lblNumCollectedTours_Value = new Label(container, SWT.NONE);
            _lblNumCollectedTours_Value.setText(UI.SPACE8);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblNumCollectedTours_Value);
         }
      }
   }

   private void initUI() {

      _defaultSelectionListener = SelectionListener.widgetSelectedAdapter(selectionEvent -> onChangeUI());

      _isFilterActive = _tourBookView.getActionTourCollectionFilter().getSelection();
   }

   private void onChangeUI() {

      TourCollectionFilter selectionFilter;

      if (_rdoShowCollectedTours.getSelection()) {

         selectionFilter = TourCollectionFilter.COLLECTED_TOURS;

      } else if (_rdoShowNotCollectedTours.getSelection()) {

         selectionFilter = TourCollectionFilter.NOT_COLLECTED_TOURS;

      } else {

         selectionFilter = TourCollectionFilter.ALL_TOURS;
      }

      _tourBookView.updateTourSelectionFilter(selectionFilter, true);
   }

   private void restoreState() {

      if (_isFilterActive) {

         switch (_tourBookView.getSlideoutData_TourCollectionFilter()) {

         case NOT_COLLECTED_TOURS:

            _rdoShowNotCollectedTours.setSelection(true);

            break;

         case COLLECTED_TOURS:

            _rdoShowCollectedTours.setSelection(true);

            break;

         case ALL_TOURS:
         default:

            _rdoShowAllTours.setSelection(true);
            break;
         }
      }

      updateUI();
   }

   void updateUI() {

      if (_lblNumAllTours_Value != null && _lblNumAllTours_Value.isDisposed()) {
         return;
      }

      _lblNumAllTours_Value.setText(_tourBookView.getSlideoutData_NumberOfAllTours());
      _lblNumCollectedTours_Value.setText(_tourBookView.getSlideoutData_NumberOfSelectedTours());

      // resize slideout that numbers are fully displayed
      final Shell shell = _shellContainer.getShell();
      shell.pack(true);
   }
}
