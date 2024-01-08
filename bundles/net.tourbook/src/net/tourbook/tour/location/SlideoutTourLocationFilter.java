/*******************************************************************************
 * Copyright (C) 2024 Wolfgang Schramm and Contributors
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
package net.tourbook.tour.location;

import java.util.List;

import net.tourbook.Messages;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.Util;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;

public class SlideoutTourLocationFilter extends ToolbarSlideout {

   private static int       _comboAllCountryOffset;

   private TourLocationView _tourLocationView;

   private FocusListener    _keepOpenListener;

   private List<String>     _allCountries;

   /*
    * UI controls
    */
   private Composite _shellContainer;

   private Combo     _comboCountry;

   private Label     _lblCountry;

   /**
    * @param ownerControl
    * @param toolBar
    * @param tourLocationView
    */
   public SlideoutTourLocationFilter(final Control ownerControl,
                                     final ToolBar toolBar,
                                     final TourLocationView tourLocationView) {

      super(ownerControl, toolBar);

      _tourLocationView = tourLocationView;
   }

   @Override
   protected Composite createToolTipContentArea(final Composite parent) {

      initUI();

      final Composite ui = createUI(parent);

      fillUI();

      restoreState();

      enableControls();

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
      label.setText(Messages.Slideout_TourLocationFilter_Label_Title);
      label.setFont(JFaceResources.getBannerFont());

      MTFont.setBannerFont(label);
   }

   private void createUI_20_Filter(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {

         _lblCountry = new Label(container, SWT.NONE);
         _lblCountry.setText(Messages.Slideout_TourLocationFilter_Label_Country);
         GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_lblCountry);

         _comboCountry = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
         _comboCountry.setVisibleItemCount(150);
         _comboCountry.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onChangeUI()));
         _comboCountry.addFocusListener(_keepOpenListener);
      }
   }

   private void enableControls() {

      final boolean isFilterActive = _tourLocationView.isLocationFilterActive();

      _lblCountry.setEnabled(isFilterActive);
      _comboCountry.setEnabled(isFilterActive);
   }

   private void fillUI() {

      _comboAllCountryOffset = 1;
      _comboCountry.add(Messages.Slideout_TourLocationFilter_Filter_AllCountries);

      _allCountries = _tourLocationView.getCountries();

      for (final String country : _allCountries) {
         _comboCountry.add(country);
      }
   }

   private void initUI() {

      _keepOpenListener = new FocusListener() {

         @Override
         public void focusGained(final FocusEvent focusEvent) {

            /*
             * This will fix the problem that when the list of a combobox is displayed, then the
             * slideout will disappear :-(((
             */
            setIsAnotherDialogOpened(true);
         }

         @Override
         public void focusLost(final FocusEvent focusEvent) {

            setIsAnotherDialogOpened(false);
         }
      };
   }

   private void onChangeUI() {

      final int selectionIndex = _comboCountry.getSelectionIndex();

      final String selectedCountry = selectionIndex < _comboAllCountryOffset

            // all countries are selected
            ? null

            // get selected country
            : _allCountries.get(selectionIndex - _comboAllCountryOffset);

      _tourLocationView.updateTourLocationFilter(selectedCountry);
   }

   private void restoreState() {

      final String selectedCountry = _tourLocationView.getLocationFilter_Country();

      if (selectedCountry != null) {

         Util.selectTextInCombo(

               _comboCountry,
               _allCountries,
               selectedCountry,

               // offset, country names are starting by the second combo item
               _comboAllCountryOffset);

      } else {

         // filter is not active

         _comboCountry.select(0);
      }
   }

}
